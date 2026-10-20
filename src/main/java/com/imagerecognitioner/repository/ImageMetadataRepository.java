package com.imagerecognitioner.repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.imagerecognitioner.model.image.ImageMetadata;

/**
 * Repository implementation for managing ImageMetadata objects in DynamoDB.
 * ImageMetadataRepository
 */
@Repository 
public class ImageMetadataRepository implements ImageMetadataRepositoryInterface {
    private static final String CONTENT_HASH_INDEX = "contentHash-index";
    private final DynamoDbTable<ImageMetadata> imageMetadataTable;

    public ImageMetadataRepository(DynamoDbTable<ImageMetadata> imageMetadataTable) {
        this.imageMetadataTable = imageMetadataTable;
    }

    /**
     * Saves an ImageMetadata object to the DynamoDB table.
     * @param imageMetadata the ImageMetadata object to save
     * @return the saved ImageMetadata object
     */
    @Override
    public ImageMetadata save(ImageMetadata imageMetadata) {
        imageMetadataTable.putItem(imageMetadata);

        return imageMetadata;
    }

    /**
     * Retrieves all ImageMetadata objects from the DynamoDB table.
     * @return a list of all ImageMetadata objects
     */
    @Override
    public List<ImageMetadata> findAll() {
        return imageMetadataTable.scan().items().stream().toList();
    }

    /**
     * Retrieves an ImageMetadata object by its imageId.
     * @param imageId the ID of the ImageMetadata object to retrieve
     * @return an Optional containing the ImageMetadata object if found, or empty if not found
     */
    @Override
    public Optional<ImageMetadata> findById(String imageId) {
        return Optional.ofNullable(imageMetadataTable.getItem(r -> r.key(k -> k.partitionValue(imageId))));
    }

    @Override
    public Optional<ImageMetadata> findByContentHash(String contentHash) {
        return imageMetadataTable.index(CONTENT_HASH_INDEX)
                .query(QueryConditional.keyEqualTo(key -> key.partitionValue(contentHash)))
                .stream()
                .flatMap(page -> page.items().stream())
                .findFirst();
    }

    /**
     * Deletes an ImageMetadata object by its imageId.
     * @param imageId the ID of the ImageMetadata object to delete
     */
    @Override
    public void deleteById(String imageId) {
        imageMetadataTable.deleteItem(r -> r.key(k -> k.partitionValue(imageId)));
    }
}
