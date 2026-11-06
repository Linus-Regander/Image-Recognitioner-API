package com.imagerecognitioner.controller;

import com.imagerecognitioner.model.image.ImageResponse;
import com.imagerecognitioner.model.image.ImageMetadata;
import com.imagerecognitioner.service.ImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;
import java.time.Duration;

/**
 * Controller for managing Image objects via RESTful API endpoints.
 * ImageController
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Upload, retrieve, update, and delete images and their metadata")
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @Operation(summary = "Get image metadata", description = "Retrieves metadata for one image from DynamoDB.")
    @ApiResponse(responseCode = "200", description = "Image metadata returned")
    @ApiResponse(responseCode = "404", description = "Image metadata not found")
    @GetMapping("/{imageId}/metadata")
    public ResponseEntity<ImageMetadata> getImageMetadata(
            @Parameter(description = "The image identifier", required = true, in = ParameterIn.PATH)
            @PathVariable String imageId) {
        return ResponseEntity.ok(imageService.selectImageMetadata(imageId));
    }

    @Operation(summary = "List image metadata", description = "Retrieves metadata for all images from DynamoDB.")
    @ApiResponse(responseCode = "200", description = "Image metadata returned")
    @GetMapping("/metadata")
    public ResponseEntity<List<ImageMetadata>> getAllImageMetadata() {
        return ResponseEntity.ok(imageService.selectAllImageMetadata());
    }

    @Operation(summary = "Upload an image", description = "Uploads an image to S3, moderates it with Amazon Rekognition, and stores its metadata in DynamoDB.")
    @ApiResponse(responseCode = "201", description = "Image uploaded and metadata created")
    @ApiResponse(responseCode = "400", description = "Invalid image or moderation confidence")
    @ApiResponse(responseCode = "422", description = "Image moderation rejected the image")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageMetadata> publishImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam("owner") String owner,
            @RequestParam(value = "minConfidence", required = false) Float minConfidence) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageService.publishImage(file, owner, minConfidence));
    }

    @Operation(summary = "Replace an image", description = "Replaces an existing image in S3 and updates its metadata in DynamoDB.")
    @ApiResponse(responseCode = "200", description = "Image replaced and metadata updated")
    @ApiResponse(responseCode = "404", description = "Image metadata not found")
    @ApiResponse(responseCode = "422", description = "Image moderation rejected the image")
    @PutMapping(value = "/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageMetadata> updateImage(
            @RequestPart("file") MultipartFile file,
            @PathVariable String imageId,
            @RequestParam(value = "minConfidence", required = false) Float minConfidence) {
        return ResponseEntity.status(HttpStatus.OK).body(imageService.replaceImage(file, imageId, minConfidence));
    }

    @Operation(summary = "Update image metadata", description = "Updates metadata for an existing image.")
    @ApiResponse(responseCode = "200", description = "Image metadata updated")
    @ApiResponse(responseCode = "404", description = "Image metadata not found")
    @PatchMapping("/{imageId}/metadata")
    public ResponseEntity<ImageMetadata> updateImageMetadata(
            @Parameter(description = "The image identifier", required = true, in = ParameterIn.PATH)
            @PathVariable String imageId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated image metadata", required = true)
            @RequestBody ImageMetadata imageMetadata) {
        return ResponseEntity.ok(imageService.updateMetadata(imageId, imageMetadata));
    }

    @Operation(summary = "Get an image", description = "Retrieves image metadata and a presigned S3 URL.")
    @ApiResponse(responseCode = "200", description = "Image metadata and presigned URL returned")
    @ApiResponse(responseCode = "404", description = "Image metadata not found")
    @GetMapping("/{imageId}")
    public ResponseEntity<ImageResponse> selectImage(
            @Parameter(description = "The image identifier", required = true, in = ParameterIn.PATH)
            @PathVariable String imageId,
            @Parameter(description = "Presigned URL validity in seconds")
            @RequestParam(value = "expiry", required = false) Long expirySeconds) {
        Duration expiry = expirySeconds != null ? Duration.ofSeconds(expirySeconds) : null;

        return ResponseEntity.ok(imageService.selectImage(imageId, expiry));
    }

    @Operation(summary = "Delete an image", description = "Deletes an image from S3 and its metadata from DynamoDB.")
    @ApiResponse(responseCode = "204", description = "Image deleted")
    @ApiResponse(responseCode = "404", description = "Image metadata not found")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "The image identifier", required = true, in = ParameterIn.PATH)
            @PathVariable String imageId) {
        imageService.deleteImage(imageId);

        return ResponseEntity.noContent().build();
    }
}