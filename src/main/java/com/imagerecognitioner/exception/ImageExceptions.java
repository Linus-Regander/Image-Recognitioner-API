package com.imagerecognitioner.exception;

/**
 * Custom exceptions that are not caused by AWS or other internal processes.
 * ImageExceptions
 */
public final class ImageExceptions {
    private ImageExceptions() {}

    public static class ImageMetadataNotFoundException extends RuntimeException {
        public ImageMetadataNotFoundException(String imageId) {
            super("ImageMetadata not found with id: " + imageId);
        }
    }

    public static class DuplicateImageException extends RuntimeException {
        public DuplicateImageException(String contentHash) {
            super("An image with this content hash already exists: " + contentHash);
        }
    }

    public static class InvalidImageException extends RuntimeException {
        public InvalidImageException(String message) {
            super(message);
        }
    }

    public static class InvalidModerationConfidenceException extends RuntimeException {
        public InvalidModerationConfidenceException(float confidence) {
            super("Invalid moderation confidence value: " + confidence + ". It must be between 0 and 100.");
        }
    }

    public static class ImageModerationException extends RuntimeException {
        public ImageModerationException(java.util.List<com.imagerecognitioner.model.image.ImageLabel> labels) {
            super("Image moderation failed. Detected inappropriate content with labels: " + labels);
        }
    }
}