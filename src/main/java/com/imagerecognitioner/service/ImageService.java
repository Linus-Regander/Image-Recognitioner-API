package com.imagerecognitioner.service;
 
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
 
import com.imagerecognitioner.cli.AwsProperties;
import com.imagerecognitioner.model.image.ImageResponse;
import com.imagerecognitioner.model.image.ImageMetadata;
import com.imagerecognitioner.model.image.ImageStatus;
import com.imagerecognitioner.model.image.ImageLabel;
import com.imagerecognitioner.model.moderation.ModerationResult;
import com.imagerecognitioner.util.hashing.Hash;
import com.imagerecognitioner.exception.ImageExceptions;
 
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
 
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.net.URL;
import java.time.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.NoSuchAlgorithmException;
 
/**
 * Service for managing images, including uploading to S3 and storing metadata in DynamoDB.
 * ImageService
 */
@Service
public class ImageService {
    private final String FAILED_TO_READ_FILE_ERROR = "Failed to read uploaded file";
    private final String UNSUPPORTED_HASH_ALGORITHM_ERROR = "Hash algorithm not available";
    private final ImageStorageService imageStorageService;
    private final ImageMetadataService imageMetadataService;
    private final ImageRecognitionService imageRecognitionService;
    private final String keyPrefix;
    private final String bucketName;
    private final Hash hash;
 
    public ImageService(ImageStorageService imageStorageService, ImageMetadataService imageMetadataService, ImageRecognitionService imageRecognitionService, AwsProperties awsProperties, Hash hash) {
        this.imageStorageService = imageStorageService;
        this.imageMetadataService = imageMetadataService;
        this.imageRecognitionService = imageRecognitionService;
        this.keyPrefix = awsProperties.getS3().getKeyPrefix();
        this.bucketName = awsProperties.getS3().getBucketName();
        this.hash = hash;
    }
    
    //
    // Public methods for image handling.
    //
 
    /**
     * Selects image metadata from DynamoDB.
     * @param imageId the ID of the image metadata to retrieve
     * @return the ImageMetadata object
     */
    public ImageMetadata selectImageMetadata(String imageId) {
        return imageMetadataService.findById(imageId);
    }
 
    /**
     * Selects all image metadata from DynamoDB.
     * @return a list of all ImageMetadata objects
     */
    public List<ImageMetadata> selectAllImageMetadata() {
        return imageMetadataService.findAll();
    }
 
    /**
     * Publishes an image by uploading it to S3 and creating its metadata in DynamoDB.
     * If an identical image has already been moderated, its labels are reused and moderation is skipped.
     * @param file the image file to upload
     * @param owner the owner of the image
     * @return the metadata of the published image
     */
    public ImageMetadata publishImage(MultipartFile file, String owner, Float minConfidence) {
        String id = UUID.randomUUID().toString();
        String key = buildKey(owner, id, file.getOriginalFilename());
        String imageHash = calculateHash(file);
        var found = findByHash(imageHash);
 
        try {
            imageStorageService.upload(key, file);
 
            List<ImageLabel> labels = found.isPresent()
                    ? found.get().getLabels()
                    : moderate(key, minConfidence);
 
            return imageMetadataService.create(extractMetadata(file, owner, id, key, imageHash, labels));
        } catch (AwsServiceException | SdkClientException e) {
            imageStorageService.delete(key);
 
            throw e;
        }
    }
 
    /**
     * Replaces an existing image in S3 and updates its metadata in DynamoDB.
     * If an identical image has already been moderated, its labels are reused and moderation is skipped.
     * @param file the new image file to upload
     * @param imageId the ID of the existing image to replace
     * @return the updated metadata of the replaced image
     */
    public ImageMetadata replaceImage(MultipartFile file, String imageId, Float minConfidence) {
        ImageMetadata existingMetadata = imageMetadataService.findById(imageId);
        String key = existingMetadata.getS3Key();
        String imageHash = calculateHash(file);
        var found = findByHash(imageHash);
 
        List<ImageLabel> labels;
 
        if (found.isPresent()) {
            labels = found.get().getLabels();
            imageStorageService.replace(key, file);
        } else {
            String temporaryKey = buildKey(existingMetadata.getOwner(), imageId, file.getOriginalFilename());
 
            try {
                imageStorageService.upload(temporaryKey, file);

                labels = moderate(temporaryKey, minConfidence);

                imageStorageService.replace(key, file);
                
                imageStorageService.delete(temporaryKey);
            } catch (AwsServiceException | SdkClientException e) {
                imageStorageService.delete(temporaryKey);
 
                throw e;
            }
        }
 
        return imageMetadataService.updateImageContent(imageId, extractMetadata(file, existingMetadata.getOwner(), imageId, key, imageHash, labels));
    }
 
    /**
     * Selects an image by retrieving its metadata from DynamoDB and generating a presigned URL for access from S3.
     * @param imageId the ID of the image to select
     * @param expiry the duration for which the presigned URL should be valid
     * @return an Image object containing the metadata and presigned URL
     */
    public ImageResponse selectImage(String imageId, Duration expiry) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
 
        String key = metadata.getS3Key();
 
        URL presignedUrl = imageStorageService.getPresignedUrl(key, expiry);
 
        ImageResponse image = new ImageResponse();
 
        image.setPresignedUrl(presignedUrl);
        image.setImageMetadata(metadata);
 
        return image;
    }
 
    /**
     * Updates the metadata of an existing image in DynamoDB.
     * @param imageId the ID of the existing image to update
     * @param imageMetadata the new metadata to update
     * @return the updated metadata of the image
     */
    public ImageMetadata updateMetadata(String imageId, ImageMetadata imageMetadata) {
        return imageMetadataService.update(imageId, imageMetadata);
    }
 
    /**
     * Deletes an image by removing it from S3 and deleting its metadata from DynamoDB.
     * @param imageId the ID of the image to delete
     */
    public void deleteImage(String imageId) {
        ImageMetadata existingMetadata = imageMetadataService.findById(imageId);
        String key = existingMetadata.getS3Key();
 
        imageStorageService.delete(key);
        imageMetadataService.deleteById(imageId);
    }

    //
    // Private helper methods.
    //
 
    private ImageMetadata extractMetadata(MultipartFile file, String owner, String id, String key, String imageHash, List<ImageLabel> labels) {
        ImageMetadata metadata = new ImageMetadata();
 
        metadata.setImageId(id);
        metadata.setS3Key(key);
        metadata.setS3Bucket(bucketName);
        metadata.setContentHash(imageHash);
        metadata.setFileName(file.getOriginalFilename());
        metadata.setContentType(file.getContentType());
        metadata.setSizeBytes(file.getSize());
        metadata.setOwner(owner);
        metadata.setCreatedAt(Instant.now());
        metadata.setImageStatus(ImageStatus.PENDING);
        metadata.setLabels(labels);
 
        return metadata;
    }
 
    private String calculateHash(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return hash.hash(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(this.FAILED_TO_READ_FILE_ERROR, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(this.UNSUPPORTED_HASH_ALGORITHM_ERROR, e);
        }
    }
 
    private Optional<ImageMetadata> findByHash(String imageHash) {
        return imageMetadataService.findByContentHash(imageHash).filter(metadata -> metadata.getLabels() != null);
    }
 
    private List<ImageLabel> moderate(String key, Float minConfidence) {
        ModerationResult result = imageRecognitionService.moderateContent(bucketName, key, minConfidence);
 
        if (result.isFlagged()) {
            // deletes image from bucket, if it is flagged as inappropriate.
            imageStorageService.delete(key);
 
            throw new ImageExceptions.ImageModerationException(result.getLabels());
        }
 
        return result.getLabels();
    }
 
    private String buildKey(String owner, String id, String originalFilename) {
        String extension = "";
 
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
 
        return keyPrefix + owner + "/" + id + extension;
    }
}