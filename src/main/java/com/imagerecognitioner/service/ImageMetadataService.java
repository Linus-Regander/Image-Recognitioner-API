package com.imagerecognitioner.service;

import com.imagerecognitioner.exception.ImageExceptions;
import com.imagerecognitioner.model.image.ImageMetadata;
import com.imagerecognitioner.repository.ImageMetadataRepositoryInterface;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Service for managing ImageMetadata objects.
 * ImageMetadataService
 */
@Service
public class ImageMetadataService {
    private final ImageMetadataRepositoryInterface imageMetadataRepository;

    public ImageMetadataService(ImageMetadataRepositoryInterface imageMetadataRepository) {
        this.imageMetadataRepository = imageMetadataRepository;
    }

    /*
     * Creates a new ImageMetadata object.
     * @param imageMetadata the ImageMetadata object to create
     * @return the created ImageMetadata object
     */
    public ImageMetadata create(ImageMetadata imageMetadata) {
        Instant now = Instant.now();

        imageMetadata.setImageId(UUID.randomUUID().toString());
        imageMetadata.setCreatedAt(now);
        imageMetadata.setUpdatedAt(now);

        return imageMetadataRepository.save(imageMetadata);
    }

    /*
     * Updates an existing ImageMetadata object.
     * @param imageId the ID of the ImageMetadata object to update
     * @param imageMetadata the updated ImageMetadata object
     * @return the updated ImageMetadata object
     */
    public ImageMetadata update(String imageId, ImageMetadata imageMetadata) {
        return update(imageId, imageMetadata, false);
    }

    /**
     * Updates image fields produced by replacing the stored image, including its content hash.
     * @param imageId the ID of the metadata to update
     * @param imageMetadata the replacement image metadata
     * @return the updated metadata
     */
    public ImageMetadata updateImageContent(String imageId, ImageMetadata imageMetadata) {
        return update(imageId, imageMetadata, true);
    }

    private ImageMetadata update(String imageId, ImageMetadata imageMetadata, boolean updateContentHash) {
        ImageMetadata existing = imageMetadataRepository.findById(imageId)
                .orElseThrow(() -> new ImageExceptions.ImageMetadataNotFoundException(imageId));

        if (imageMetadata.getFileName() != null) {
            existing.setFileName(imageMetadata.getFileName());
        }

        if (imageMetadata.getContentType() != null) {
            existing.setContentType(imageMetadata.getContentType());
        }
        
        if (imageMetadata.getSizeBytes() != null) {
            existing.setSizeBytes(imageMetadata.getSizeBytes());
        }

        if (updateContentHash && imageMetadata.getContentHash() != null) {
            existing.setContentHash(imageMetadata.getContentHash());
        }
        
        existing.setUpdatedAt(Instant.now());

        return imageMetadataRepository.save(existing);
    }

    /**
     * Retrieves all ImageMetadata objects.
     * @return a list of all ImageMetadata objects
     */
    public List<ImageMetadata> findAll() {
        return imageMetadataRepository.findAll();
    }

    /**
     * Retrieves an ImageMetadata object by its imageId.
     * @param imageId the ID of the ImageMetadata object to retrieve
     * @return the ImageMetadata object
     * @throws ImageMetadataNotFoundException if no ImageMetadata exists with the given imageId
     */
    public ImageMetadata findById(String imageId) {
        return imageMetadataRepository.findById(imageId)
                .orElseThrow(() -> new ImageExceptions.ImageMetadataNotFoundException(imageId));
    }

    /**
     * Retrieves metadata for an image with the specified content hash.
     * @param contentHash the content hash to look up
     * @return matching metadata, if present
     */
    public java.util.Optional<ImageMetadata> findByContentHash(String contentHash) {
        return imageMetadataRepository.findByContentHash(contentHash);
    }

    /**
     * Deletes an ImageMetadata object by its imageId.
     * @param imageId the ID of the ImageMetadata object to delete
     * @throws ImageMetadataNotFoundException if no ImageMetadata exists with the given imageId
     */
    public void deleteById(String imageId) {
        if (imageMetadataRepository.findById(imageId).isEmpty()) {
            throw new ImageExceptions.ImageMetadataNotFoundException(imageId);
        }

        imageMetadataRepository.deleteById(imageId);
    }
}