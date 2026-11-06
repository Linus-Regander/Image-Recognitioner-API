package com.imagerecognitioner.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws")
public class AwsProperties {
    private String region;
    private DynamoDB dynamoDB = new DynamoDB();
    private S3 s3 = new S3();
    private Rekognition rekognition = new Rekognition();

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public DynamoDB getDynamoDB() {
        return dynamoDB;
    }

    public void setDynamoDB(DynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public Rekognition getRekognition() {
        return rekognition;
    }

    public void setRekognition(Rekognition rekognition) {
        this.rekognition = rekognition;
    }

    public static class DynamoDB {
        private String tableName;

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }

    public static class S3 {
        private String bucketName;
        private String endpoint;
        private String keyPrefix;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class Rekognition {
        private String endpoint;
        private Double moderationMinConfidence = 60.0;
        private boolean moderationEnabled;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Double getModerationMinConfidence() {
            return moderationMinConfidence;
        }

        public void setModerationMinConfidence(Double moderationMinConfidence) {
            this.moderationMinConfidence = moderationMinConfidence;
        }

        public boolean isModerationEnabled() {
            return moderationEnabled;
        }

        public void setModerationEnabled(boolean moderationEnabled) {
            this.moderationEnabled = moderationEnabled;
        }
    }
}
