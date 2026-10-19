package com.imagerecognitioner.service;

import com.imagerecognitioner.cli.AwsProperties;
import com.imagerecognitioner.exception.ImageExceptions;
import com.imagerecognitioner.model.image.ImageResponse;
import com.imagerecognitioner.model.image.ImageMetadata;
import com.imagerecognitioner.model.image.ImageStatus;
import com.imagerecognitioner.model.image.ImageLabel;
import com.imagerecognitioner.model.moderation.ModerationResult;
import com.imagerecognitioner.util.hashing.CryptoType;
import com.imagerecognitioner.util.hashing.Hash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private ImageMetadataService imageMetadataService;

    @Mock
    private ImageRecognitionService imageRecognitionService;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        AwsProperties properties = new AwsProperties();
        properties.getS3().setKeyPrefix("images/");
        properties.getS3().setBucketName("image-bucket");
        imageService = new ImageService(imageStorageService, imageMetadataService, imageRecognitionService, properties,
                new Hash(CryptoType.SHA256, null));
        lenient().when(imageRecognitionService.moderateContent(anyString(), anyString(), nullable(Float.class)))
            .thenReturn(new ModerationResult(false, List.of()));
    }

    @Test
    void selectImageMetadata_delegatesEachImageId() {
        ImageMetadata metadata = metadata("img-1", "images/img-1.jpg", "owner");
        Map<String, ServiceTestCase> testCases = Map.of(
            "known image", () -> selectMetadata("img-1", metadata),
            "missing result", () -> selectMetadata("missing", null));

        runTestCases(testCases);
    }

    private void selectMetadata(String imageId, ImageMetadata result) {
        when(imageMetadataService.findById(imageId)).thenReturn(result);
        assertSame(result, imageService.selectImageMetadata(imageId));
        verify(imageMetadataService).findById(imageId);
        reset(imageMetadataService);
    }

    @Test
    void selectAllImageMetadata_delegatesPopulatedAndEmptyResults() {
        Map<String, ServiceTestCase> testCases = Map.of(
            "populated result", () -> {
                List<ImageMetadata> result = List.of(metadata("img-1", "key", "owner"));
                when(imageMetadataService.findAll()).thenReturn(result);
                assertSame(result, imageService.selectAllImageMetadata());
                verify(imageMetadataService).findAll();
                reset(imageMetadataService);
            },
            "empty result", () -> {
                when(imageMetadataService.findAll()).thenReturn(List.of());
                assertEquals(List.of(), imageService.selectAllImageMetadata());
                verify(imageMetadataService).findAll();
                reset(imageMetadataService);
            });

        runTestCases(testCases);
    }

    @Test
    void publishImage_uploadsAndMapsMetadata() {
        Map<String, ServiceTestCase> testCases = Map.of(
            "jpeg includes extension", () -> publishSuccess("photo.jpg", "alice", ".jpg"),
            "filename without extension", () -> publishSuccess("photo", "bob", ""),
            "null filename", () -> publishSuccess(null, "carol", ""));

        runTestCases(testCases);
    }

    private void publishSuccess(String filename, String owner, String extension) {
        MultipartFile file = file(filename, "image/jpeg", "bytes");
        ImageMetadata created = metadata("created", "created-key", owner);
        when(imageMetadataService.create(any(ImageMetadata.class))).thenReturn(created);

        assertSame(created, imageService.publishImage(file, owner, null));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(imageStorageService).upload(key.capture(), argThat(actual -> actual == file));
        ArgumentCaptor<ImageMetadata> captured = ArgumentCaptor.forClass(ImageMetadata.class);
        verify(imageMetadataService).create(captured.capture());
        String actualKey = key.getValue();
        assertEquals("images/" + owner + "/", actualKey.substring(0, ("images/" + owner + "/").length()));
        assertEquals(extension, extensionOf(actualKey));
        ImageMetadata actual = captured.getValue();
        assertEquals(owner, actual.getOwner());
        assertEquals(file.getOriginalFilename(), actual.getFileName());
        assertEquals("image/jpeg", actual.getContentType());
        assertEquals(5L, actual.getSizeBytes());
        assertEquals(actualKey, actual.getS3Key());
        assertEquals("277089d91c0bdf4f2e6862ba7e4a07605119431f5d13f726dd352b06f1b206a9",
                actual.getContentHash());
        assertEquals(ImageStatus.PENDING, actual.getImageStatus());
        reset(imageStorageService, imageMetadataService);
    }

    @Test
    void publishImage_deletesUploadedObjectForAwsFailures() {
        Map<String, ServiceTestCase> testCases = Map.of(
            "AWS service failure", () -> publishFailure(AwsServiceException.builder().message("DynamoDB failed").statusCode(500).build()),
            "AWS client failure", () -> publishFailure(SdkClientException.create("DynamoDB unavailable")));

        runTestCases(testCases);
    }

    @Test
    void publishImage_deletesAndRejectsFlaggedContent() {
        MultipartFile file = file("photo.jpg", "image/jpeg", "bytes");
        List<ImageLabel> labels = List.of(new ImageLabel("Explicit Nudity", 95.0f));
        when(imageRecognitionService.moderateContent(anyString(), anyString(), nullable(Float.class)))
                .thenReturn(new ModerationResult(true, labels));

        assertThrows(ImageExceptions.ImageModerationException.class,
                () -> imageService.publishImage(file, "alice", null));

        verify(imageStorageService).delete(argThat(key -> key.startsWith("images/alice/")));
        verify(imageMetadataService, org.mockito.Mockito.never()).create(any());
    }

    @Test
    void publishImage_rejectsDuplicateContentBeforeUploading() {
        MultipartFile file = file("photo.jpg", "image/jpeg", "bytes");
        ImageMetadata duplicate = metadata("existing-image", "images/alice/existing.jpg", "alice");
        when(imageMetadataService.findByContentHash(anyString())).thenReturn(java.util.Optional.of(duplicate));

        assertThrows(ImageExceptions.DuplicateImageException.class,
                () -> imageService.publishImage(file, "alice", null));

        verify(imageStorageService, org.mockito.Mockito.never()).upload(anyString(), any());
        verify(imageMetadataService, org.mockito.Mockito.never()).create(any());
    }

    private void publishFailure(RuntimeException failure) {
        MultipartFile file = file("photo.png", "image/png", "bytes");
        when(imageMetadataService.create(any(ImageMetadata.class))).thenThrow(failure);

        assertThrows(failure.getClass(), () -> imageService.publishImage(file, "alice", null));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(imageStorageService).upload(key.capture(), argThat(actual -> actual == file));
        verify(imageStorageService).delete(key.getValue());
        reset(imageStorageService, imageMetadataService);
    }

    @Test
    void replaceImage_replacesObjectAndUpdatesMetadata() {
        ImageMetadata existing = metadata("img-1", "images/alice/img-1.jpg", "alice");
        MultipartFile file = file("new.png", "image/png", "new-bytes");
        ImageMetadata updated = metadata("img-1", existing.getS3Key(), "alice");
        when(imageMetadataService.findById("img-1")).thenReturn(existing);
        when(imageMetadataService.updateImageContent(eq("img-1"), any(ImageMetadata.class))).thenReturn(updated);

        assertSame(updated, imageService.replaceImage(file, "img-1", null));

        verify(imageStorageService).replace(existing.getS3Key(), file);
        ArgumentCaptor<ImageMetadata> replacement = ArgumentCaptor.forClass(ImageMetadata.class);
        verify(imageMetadataService).updateImageContent(eq("img-1"), replacement.capture());
        assertEquals(existing.getOwner(), replacement.getValue().getOwner());
        assertEquals(existing.getS3Key(), replacement.getValue().getS3Key());
        assertEquals("new.png", replacement.getValue().getFileName());
        org.junit.jupiter.api.Assertions.assertNotNull(replacement.getValue().getContentHash());
    }

    @Test
    void selectImage_generatesUrlAndMapsMetadata() throws Exception {
        ImageMetadata metadata = metadata("img-1", "images/alice/img-1.jpg", "alice");
        Duration expiry = Duration.ofMinutes(5);
        URL url = new URL("https://example.test/img-1");
        when(imageMetadataService.findById("img-1")).thenReturn(metadata);
        when(imageStorageService.getPresignedUrl(metadata.getS3Key(), expiry)).thenReturn(url);

        ImageResponse result = imageService.selectImage("img-1", expiry);

        assertSame(metadata, result.getImageMetadata());
        assertEquals(url, result.getPresignedUrl());
        verify(imageStorageService).getPresignedUrl(metadata.getS3Key(), expiry);
    }

    @Test
    void updateMetadata_delegatesUpdate() {
        ImageMetadata patch = metadata(null, null, null);
        ImageMetadata updated = metadata("img-1", "key", "owner");
        when(imageMetadataService.update("img-1", patch)).thenReturn(updated);

        assertSame(updated, imageService.updateMetadata("img-1", patch));
        verify(imageMetadataService).update("img-1", patch);
    }

    @Test
    void deleteImage_deletesStorageBeforeMetadata() {
        ImageMetadata existing = metadata("img-1", "images/alice/img-1.jpg", "alice");
        when(imageMetadataService.findById("img-1")).thenReturn(existing);

        imageService.deleteImage("img-1");

        verify(imageStorageService).delete(existing.getS3Key());
        verify(imageMetadataService).deleteById("img-1");
    }

    private MultipartFile file(String filename, String contentType, String content) {
        return new MockMultipartFile("file", filename, contentType, content.getBytes());
    }

    private String extensionOf(String key) {
        int slash = key.lastIndexOf('/');
        int dot = key.lastIndexOf('.');
        return dot > slash ? key.substring(dot) : "";
    }

    private ImageMetadata metadata(String imageId, String key, String owner) {
        ImageMetadata metadata = new ImageMetadata();
        metadata.setImageId(imageId);
        metadata.setS3Key(key);
        metadata.setOwner(owner);
        return metadata;
    }

    private void runTestCases(Map<String, ServiceTestCase> testCases) {
        for (Map.Entry<String, ServiceTestCase> testCase : testCases.entrySet()) {
            try {
                testCase.getValue().run();
            } catch (AssertionError assertionError) {
                throw new AssertionError("Test case failed: " + testCase.getKey(), assertionError);
            } catch (Exception exception) {
                throw new AssertionError("Test case failed: " + testCase.getKey(), exception);
            }
        }
    }

    @FunctionalInterface
    private interface ServiceTestCase {
        void run() throws Exception;
    }
}