<div align="center">

# Image Recognition API

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?logo=springboot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Build](https://github.com/Linus-Regander/java-aws-api/actions/workflows/ci.yml/badge.svg)
[![Coverage](https://codecov.io/gh/Linus-Regander/java-aws-api/branch/main/graph/badge.svg)](https://codecov.io/gh/Linus-Regander/java-aws-api)

</div>

## Description
API for handling images and using **Amazon Rekognition** for moderating images and preventing upload of inappropriate content, for storage of non-GDPR images in **Amazon S3**. (*This is currently done in a local setting*).

Image metadata is stored in **Amazon DynamoDB**.

Uploaded image content is identified by its exact file bytes with SHA-256 by default (`app.hash.crypto-type` in `application.yml`); SHA-512 is also supported. A custom Spring bean implementing `HashFunc` can provide a different hashing strategy, such as perceptual hashing. The hash is stored in image metadata and indexed to reject duplicate content before upload.

Docker images are stored in Docker Hub, with CI/CD integration via GitHub Actions.

## Versioning

**Current version:** 0.8.0
- Added OpenAPI documentation and Swagger UI for the REST API.
- Documented image upload, retrieval, metadata, replacement, and deletion endpoints.

**Previous version:** 0.7.0
- Added Amazon Rekognition integration with client.
- Added image moderation for tagging and preventing upload of inappropriate images.
- Updated Terraform and configuration for Rekognition support.

**Next version:** Authentication and authorization in API.

## API Documentation

When the application is running on the default port, interactive API documentation is available at:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI specification: http://localhost:8080/v3/api-docs

### Image endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/images` | Upload and moderate an image |
| `GET` | `/api/images/{imageId}` | Retrieve an image and presigned URL |
| `PUT` | `/api/images/{imageId}` | Replace an image |
| `DELETE` | `/api/images/{imageId}` | Delete an image |
| `GET` | `/api/images/metadata` | List all image metadata |
| `GET` | `/api/images/{imageId}/metadata` | Retrieve image metadata |
| `PATCH` | `/api/images/{imageId}/metadata` | Update image metadata |

## Tech Stack
| Category | Tools |
|---|---|
| Language | **Java 17** (Eclipse Temurin) |
| Framework | **Spring Boot** |
| Build | Gradle, **Terraform** |
| Containers | **Docker**, Docker Compose |
| CI/CD (GitHub) | **Actions**, CoPilot Agent, Workflows |
| Cloud (AWS) | **DynamoDB**, **S3**, **Rekognition**, Localstack |

---

<div align="center">

**Developed by** Linus Regander
**Latest update:** 2026-09-30

</div>