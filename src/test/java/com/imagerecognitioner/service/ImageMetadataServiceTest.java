package com.imagerecognitioner.service;

import com.imagerecognitioner.exception.ImageExceptions.ImageMetadataNotFoundException;
import com.imagerecognitioner.model.image.ImageMetadata;
import com.imagerecognitioner.repository.ImageMetadataRepositoryInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageMetadataServiceTest {
    private static final String TENANT_ID = "tenant-1";

    @Mock
    private ImageMetadataRepositoryInterface repository;

    private ImageMetadataService metadataService;

    @BeforeEach
    void setUp() {
        metadataService = new ImageMetadataService(repository);
    }

    @Test
    void create_savesMetadataWithoutChangingTenantOwnership() {
        Map<String, ServiceTestCase> testCases = Map.of(
            "new metadata", () -> {
                ImageMetadata metadata = new ImageMetadata();
                metadata.setTenantId(TENANT_ID);
                when(repository.save(metadata)).thenReturn(metadata);

                assertSame(metadata, metadataService.create(metadata));

                assertEquals(TENANT_ID, metadata.getTenantId());
                verify(repository).save(metadata);
                reset(repository);
            },
            "metadata with existing fields", () -> {
                ImageMetadata metadata = new ImageMetadata();
                metadata.setFileName("photo.jpg");
                metadata.setTenantId(TENANT_ID);
                when(repository.save(metadata)).thenReturn(metadata);

                ImageMetadata result = metadataService.create(metadata);

                assertSame(metadata, result);
                assertEquals("photo.jpg", result.getFileName());
                assertEquals(TENANT_ID, result.getTenantId());
                verify(repository).save(metadata);
                reset(repository);
            });

        runTestCases(testCases);
    }

    @Test
    void update_appliesEachProvidedFieldAndPreservesUnsetFields() {
        Map<String, ServiceTestCase> testCases = Map.of(
            "all mutable fields", () -> {
                ImageMetadata existing = metadata("img-1", "old.jpg", "image/jpeg", 10L);
                ImageMetadata patch = metadata(null, "new.png", "image/png", 20L);
                when(repository.findById("img-1", TENANT_ID)).thenReturn(Optional.of(existing));
                when(repository.save(existing)).thenReturn(existing);

                assertSame(existing, metadataService.update(TENANT_ID, "img-1", patch));

                assertEquals("new.png", existing.getFileName());
                assertEquals("image/png", existing.getContentType());
                assertEquals(20L, existing.getSizeBytes());
                assertNotNull(existing.getUpdatedAt());
                verify(repository).save(existing);
                reset(repository);
            },
            "null patch fields preserve existing values", () -> {
                ImageMetadata existing = metadata("img-2", "old.jpg", "image/jpeg", 10L);
                ImageMetadata patch = new ImageMetadata();
                when(repository.findById("img-2", TENANT_ID)).thenReturn(Optional.of(existing));
                when(repository.save(existing)).thenReturn(existing);

                metadataService.update(TENANT_ID, "img-2", patch);

                assertEquals("old.jpg", existing.getFileName());
                assertEquals("image/jpeg", existing.getContentType());
                assertEquals(10L, existing.getSizeBytes());
                verify(repository).save(existing);
                reset(repository);
            },
            "client metadata update cannot replace content hash", () -> {
                ImageMetadata existing = metadata("img-3", "photo.jpg", "image/jpeg", 10L);
                existing.setContentHash("original-hash");
                ImageMetadata patch = new ImageMetadata();
                patch.setContentHash("new-content-hash");
                when(repository.findById("img-3", TENANT_ID)).thenReturn(Optional.of(existing));
                when(repository.save(existing)).thenReturn(existing);

                metadataService.update(TENANT_ID, "img-3", patch);

                assertEquals("original-hash", existing.getContentHash());
                reset(repository);
            },
            "replacement updates content hash", () -> {
                ImageMetadata existing = metadata("img-4", "photo.jpg", "image/jpeg", 10L);
                existing.setContentHash("original-hash");
                ImageMetadata replacement = new ImageMetadata();
                replacement.setContentHash("replacement-hash");
                when(repository.findById("img-4", TENANT_ID)).thenReturn(Optional.of(existing));
                when(repository.save(existing)).thenReturn(existing);

                metadataService.updateImageContent(TENANT_ID, "img-4", replacement);

                assertEquals("replacement-hash", existing.getContentHash());
                reset(repository);
            },
            "missing metadata", () -> {
                when(repository.findById("missing", TENANT_ID)).thenReturn(Optional.empty());

                assertThrows(ImageMetadataNotFoundException.class,
                    () -> metadataService.update(TENANT_ID, "missing", new ImageMetadata()));

                verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
                reset(repository);
            });

        runTestCases(testCases);
    }

    @Test
    void findAll_returnsEachRepositoryResult() {
        Map<String, ServiceTestCase> testCases = Map.of(
            "populated result", () -> {
                List<ImageMetadata> result = List.of(metadata("img-1", null, null, null));
                when(repository.findAll(TENANT_ID)).thenReturn(result);

                assertSame(result, metadataService.findAll(TENANT_ID));
                verify(repository).findAll(TENANT_ID);
                reset(repository);
            },
            "empty result", () -> {
                when(repository.findAll(TENANT_ID)).thenReturn(List.of());

                assertEquals(List.of(), metadataService.findAll(TENANT_ID));
                verify(repository).findAll(TENANT_ID);
                reset(repository);
            });

        runTestCases(testCases);
    }

    @Test
    void findById_returnsOrThrowsForEachRepositoryResult() {
        Map<String, ServiceTestCase> testCases = Map.of(
            "metadata found", () -> {
                ImageMetadata metadata = metadata("img-1", null, null, null);
                when(repository.findById("img-1", TENANT_ID)).thenReturn(Optional.of(metadata));

                assertSame(metadata, metadataService.findById("img-1", TENANT_ID));
                verify(repository).findById("img-1", TENANT_ID);
                reset(repository);
            },
            "metadata missing", () -> {
                when(repository.findById("missing", TENANT_ID)).thenReturn(Optional.empty());

                assertThrows(ImageMetadataNotFoundException.class,
                    () -> metadataService.findById("missing", TENANT_ID));
                verify(repository).findById("missing", TENANT_ID);
                reset(repository);
            });

        runTestCases(testCases);
    }

    @Test
    void deleteById_deletesOnlyExistingMetadata() {
        Map<String, ServiceTestCase> testCases = Map.of(
            "existing metadata", () -> {
                ImageMetadata metadata = metadata("img-1", null, null, null);
                when(repository.findById("img-1", TENANT_ID)).thenReturn(Optional.of(metadata));

                metadataService.deleteById("img-1", TENANT_ID);

                verify(repository).deleteById("img-1", TENANT_ID);
                reset(repository);
            },
            "missing metadata", () -> {
                when(repository.findById("missing", TENANT_ID)).thenReturn(Optional.empty());

                assertThrows(ImageMetadataNotFoundException.class,
                    () -> metadataService.deleteById("missing", TENANT_ID));
                verify(repository, never()).deleteById("missing", TENANT_ID);
                reset(repository);
            });

        runTestCases(testCases);
    }

    private ImageMetadata metadata(String imageId, String fileName, String contentType, Long sizeBytes) {
        ImageMetadata metadata = new ImageMetadata();
        metadata.setImageId(imageId);
        metadata.setFileName(fileName);
        metadata.setContentType(contentType);
        metadata.setSizeBytes(sizeBytes);
        metadata.setCreatedAt(Instant.now());
        return metadata;
    }

    private void runTestCases(Map<String, ServiceTestCase> testCases) {
        for (Map.Entry<String, ServiceTestCase> testCase : testCases.entrySet()) {
            try {
                testCase.getValue().run();
            } catch (AssertionError assertionError) {
                throw new AssertionError("Test case failed: " + testCase.getKey(), assertionError);
            }
        }
    }

    @FunctionalInterface
    private interface ServiceTestCase {
        void run();
    }
}
