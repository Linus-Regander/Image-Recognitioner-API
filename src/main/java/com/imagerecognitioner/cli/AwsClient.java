package com.imagerecognitioner.cli;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.imagerecognitioner.model.image.ImageMetadata;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS client configuration for DynamoDB integration.
 */
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsClient {
    /**
     * Creates a DynamoDbClient bean configured with the specified AWS region and default credentials provider.
     * @param awsProperties
     * @return
     */
    @Bean
    public DynamoDbClient dynamoDbClient(AwsProperties awsProperties, @Value("${app.aws.dynamo-db.endpoint:}") String localEndpoint) {
        var builder = DynamoDbClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (!localEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(localEndpoint));
        }

        return builder.build();
    }

    /**
     * Creates a DynamoDbEnhancedClient bean using the provided DynamoDbClient.
     * @param dynamoDbClient
     * @return
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    /**
     * Creates a DynamoDbTable bean for the ImageMetadata table using the provided DynamoDbEnhancedClient and AWS properties.
     * @param enhancedClient
     * @param props
     * @return
     */
    @Bean
    public DynamoDbTable<ImageMetadata> imageMetadataTable(DynamoDbEnhancedClient enhancedClient, AwsProperties props) {
        return enhancedClient.table(props.getDynamoDB().getTableName(), TableSchema.fromBean(ImageMetadata.class));
    }

    /**
     * Creates an S3Client bean configured with the specified AWS region, default credentials provider, and optional S3 endpoint.
     * @param awsProperties
     * @return
     */
    @Bean
    public S3Client s3Client(AwsProperties awsProperties, @Value("${app.aws.s3.endpoint:}") String s3Endpoint) {
        var builder = S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (!s3Endpoint.isBlank()) {
            builder.endpointOverride(URI.create(s3Endpoint))
                    .forcePathStyle(true);
        }

        return builder.build();
    }

    /**
     * Creates an S3Presigner bean configured with the specified AWS region, default credentials provider, and optional S3 endpoint.
     * @param awsProperties
     * @param s3Endpoint
     * @return
     */
    @Bean
    public S3Presigner s3Presigner(AwsProperties awsProperties,@Value("${app.aws.s3.endpoint:}") String s3Endpoint, @Value("${app.aws.s3.public-endpoint:}") String publicEndpoint) {
        var builder = S3Presigner.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        String endpoint = !publicEndpoint.isBlank() ? publicEndpoint : s3Endpoint;

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        return builder.build();
    }

    /**
     * Creates a RekognitionClient bean configured with the specified AWS region, default credentials provider, and optional Rekognition endpoint.
     * @param awsProperties
     * @param endpoint
     * @return
     */
    @Bean
    public RekognitionClient rekognitionClient(AwsProperties awsProperties, @Value("${app.aws.rekognition.endpoint:}") String endpoint) {
        var builder = RekognitionClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
