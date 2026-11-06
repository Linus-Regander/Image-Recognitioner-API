package com.imagerecognitioner.service;

import com.imagerecognitioner.cli.AwsProperties;
import com.imagerecognitioner.exception.ImageExceptions;
import com.imagerecognitioner.model.moderation.ModerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageRecognitionServiceTest {
    private static final String BUCKET = "images";
    private static final String KEY = "images/alice/image.jpg";

    @Mock
    private RekognitionClient rekognitionClient;

    private ImageRecognitionService imageRecognitionService;

    @BeforeEach
    void setUp() {
        AwsProperties properties = new AwsProperties();
        properties.getRekognition().setModerationEnabled(true);
        properties.getRekognition().setModerationMinConfidence(60.0);
        imageRecognitionService = new ImageRecognitionService(rekognitionClient, properties);
    }

    @Test
    void moderateContent_usesConfiguredDefaultAndMapsLabels() {
        when(rekognitionClient.detectModerationLabels(org.mockito.ArgumentMatchers.any(DetectModerationLabelsRequest.class)))
                .thenReturn(DetectModerationLabelsResponse.builder()
                        .moderationLabels(ModerationLabel.builder().name("Explicit Nudity").confidence(92.5f).build())
                        .build());

        ModerationResult result = imageRecognitionService.moderateContent(BUCKET, KEY, null);

        ArgumentCaptor<DetectModerationLabelsRequest> request = ArgumentCaptor.forClass(DetectModerationLabelsRequest.class);
        verify(rekognitionClient).detectModerationLabels(request.capture());
        assertEquals(BUCKET, request.getValue().image().s3Object().bucket());
        assertEquals(KEY, request.getValue().image().s3Object().name());
        assertEquals(60.0f, request.getValue().minConfidence());
        assertTrue(result.isFlagged());
        assertEquals("Explicit Nudity", result.getLabels().get(0).getLabel());
        assertEquals(92.5f, result.getLabels().get(0).getConfidence());
    }

    @Test
    void moderateContent_usesExplicitConfidenceAndReturnsUnflaggedWhenNoLabels() {
        when(rekognitionClient.detectModerationLabels(org.mockito.ArgumentMatchers.any(DetectModerationLabelsRequest.class)))
                .thenReturn(DetectModerationLabelsResponse.builder().moderationLabels(List.of()).build());

        ModerationResult result = imageRecognitionService.moderateContent(BUCKET, KEY, 81.5f);

        ArgumentCaptor<DetectModerationLabelsRequest> request = ArgumentCaptor.forClass(DetectModerationLabelsRequest.class);
        verify(rekognitionClient).detectModerationLabels(request.capture());
        assertEquals(81.5f, request.getValue().minConfidence());
        assertFalse(result.isFlagged());
        assertTrue(result.getLabels().isEmpty());
    }

    @Test
    void moderateContent_rejectsConfidenceOutsideAwsRangeWithoutCallingAws() {
        assertThrows(ImageExceptions.InvalidModerationConfidenceException.class,
                () -> imageRecognitionService.moderateContent(BUCKET, KEY, -0.1f));
        assertThrows(ImageExceptions.InvalidModerationConfidenceException.class,
                () -> imageRecognitionService.moderateContent(BUCKET, KEY, 100.1f));

        verifyNoInteractions(rekognitionClient);
    }
}
