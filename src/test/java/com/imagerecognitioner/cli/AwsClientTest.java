package com.imagerecognitioner.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.imagerecognitioner.model.image.ImageMetadata;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AwsClientTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    private AwsClient awsClient;
    private AwsProperties properties;

    @BeforeEach
    void setUp() {
        awsClient = new AwsClient();
        properties = new AwsProperties();
        properties.setRegion("eu-north-1");
        properties.getDynamoDB().setTableName("image-metadata");
    }

    @Test
    void dynamoDbClient_configuresRegionAndOptionalEndpoint() {
        Map<String, AwsTestCase> testCases = Map.of(
            "without endpoint", () -> assertNull(awsClient.dynamoDbClient(properties, "")
                .serviceClientConfiguration().endpointOverride().orElse(null)),
            "with endpoint", () -> assertEquals(URI.create("http://localhost:8000"),
                awsClient.dynamoDbClient(properties, "http://localhost:8000")
                    .serviceClientConfiguration().endpointOverride().orElse(null)));
        runTestCases(testCases);
    }

    @Test
    void dynamoDbEnhancedClient_wrapsProvidedClient() {
        DynamoDbClient client = DynamoDbClient.builder().region(software.amazon.awssdk.regions.Region.EU_NORTH_1).build();
        org.junit.jupiter.api.Assertions.assertNotNull(awsClient.dynamoDbEnhancedClient(client));
    }

    @Test
    void imageMetadataTable_usesConfiguredNameAndImageMetadataSchema() {
        @SuppressWarnings("unchecked")
        DynamoDbTable<ImageMetadata> table = org.mockito.Mockito.mock(DynamoDbTable.class);
        org.mockito.Mockito.doReturn(table).when(enhancedClient).table(eq("image-metadata"), any());

        assertEquals(table, awsClient.imageMetadataTable(enhancedClient, properties));
        verify(enhancedClient).table(eq("image-metadata"), any());
    }

    @Test
    void s3Client_configuresRegionAndOptionalEndpoint() {
        Map<String, AwsTestCase> testCases = Map.of(
            "without endpoint", () -> assertNull(awsClient.s3Client(properties, "")
                .serviceClientConfiguration().endpointOverride().orElse(null)),
            "with endpoint", () -> assertEquals(URI.create("http://localhost:9000"),
                awsClient.s3Client(properties, "http://localhost:9000")
                    .serviceClientConfiguration().endpointOverride().orElse(null)));
        runTestCases(testCases);
    }

    @Test
    void s3Presigner_configuresRegionAndOptionalEndpoint() {
        Map<String, AwsTestCase> testCases = Map.of(
            "without endpoint", () -> assertNotNull(awsClient.s3Presigner(properties, "", "")),
            "with endpoint", () -> assertNotNull(awsClient.s3Presigner(properties, "http://localhost:9000", "")));
        runTestCases(testCases);
    }

    private void runTestCases(Map<String, AwsTestCase> testCases) {
        for (Map.Entry<String, AwsTestCase> testCase : testCases.entrySet()) {
            reset(enhancedClient);
            try {
                testCase.getValue().run();
            } catch (AssertionError assertionError) {
                throw new AssertionError("Test case failed: " + testCase.getKey(), assertionError);
            }
        }
    }

    @FunctionalInterface
    private interface AwsTestCase {
        void run();
    }
}