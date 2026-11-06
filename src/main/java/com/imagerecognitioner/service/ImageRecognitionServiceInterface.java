package com.imagerecognitioner.service;

import com.imagerecognitioner.model.moderation.ModerationResult;

public interface ImageRecognitionServiceInterface {
    /**
     * Moderates an image stored in S3 to detect inappropriate content.
     * @param bucket The S3 bucket name.
     * @param key The S3 object key.
     * @param minConfidence The minimum confidence level for moderation labels, or null for the default.
     * @return The result of the moderation operation.
     */
    ModerationResult moderateContent(String bucket, String key, Float minConfidence);
}