package com.imagerecognitioner.service;

import com.imagerecognitioner.cli.AwsProperties;
import com.imagerecognitioner.exception.ImageExceptions;
import com.imagerecognitioner.model.image.ImageLabel;
import com.imagerecognitioner.model.moderation.ModerationResult;

import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.S3Object;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing image recognition tasks using AWS Rekognition.
 * ImageRecognitionService
 */
@Service
public class ImageRecognitionService implements ImageRecognitionServiceInterface {
    private final RekognitionClient rekognitionClient;
    private final float defaultModerationMinConfidence;
    private final boolean moderationEnabled;

    public ImageRecognitionService(RekognitionClient rekognitionClient, AwsProperties awsProperties) {
        this.rekognitionClient = rekognitionClient;
        this.defaultModerationMinConfidence = awsProperties.getRekognition().getModerationMinConfidence().floatValue();
        this.moderationEnabled = awsProperties.getRekognition().isModerationEnabled();
    }

    /**
     * Moderates the content of an image using AWS Rekognition, in order to detect inappropriate content.
     * @param bucket The S3 bucket name.
     * @param key The S3 object key.
     * @param minConfidence The minimum confidence level for moderation labels.
     * @return The result of the moderation operation.
     */
    public ModerationResult moderateContent(String bucket, String key, Float minConfidence) {
        if (!moderationEnabled) {
            return new ModerationResult(false, List.of());
        }

        float confidence = effectiveConfidence(minConfidence);
        
        DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
                .image(Image.builder()
                        .s3Object(S3Object.builder().bucket(bucket).name(key).build())
                        .build())
                    .minConfidence(confidence)
                .build();

        List<ImageLabel> labels = rekognitionClient.detectModerationLabels(request)
                .moderationLabels().stream()
                .map(l -> new ImageLabel(l.name(), l.confidence()))
                .toList();

        return new ModerationResult(!labels.isEmpty(), labels);
    }

    /**
     * Validates the confidence level for moderation labels, to make sure that it is within the valid range.
     * @param confidence The confidence level to validate.
     */
    private float effectiveConfidence(Float confidence) {
        float threshold = confidence != null ? confidence : defaultModerationMinConfidence;

        if (threshold < 0.0f || threshold > 100.0f) {
            throw new ImageExceptions.InvalidModerationConfidenceException(threshold);
        }

        return threshold;
    }
}