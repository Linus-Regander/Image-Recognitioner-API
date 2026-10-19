package com.imagerecognitioner.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.imagerecognitioner.model.image.ImageMetadata;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageMetadataRepositoryTest {

    @Mock
    private DynamoDbTable<ImageMetadata> imageMetadataTable;

    @Mock
    private DynamoDbIndex<ImageMetadata> contentHashIndex;

    @Mock
    private SdkIterable<Page<ImageMetadata>> queryResults;

    @Mock
    private Page<ImageMetadata> queryPage;

    @Mock
    private PageIterable<ImageMetadata> pages;

    @Mock
    private SdkIterable<ImageMetadata> items;

    private ImageMetadataRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ImageMetadataRepository(imageMetadataTable);
    }

    @Test
    void save_returnsMetadataAndWritesIt() {
        ImageMetadata metadata = metadata("img-1");
        Map<String, RepositoryTestCase> testCases = Map.of(
            "metadata is saved", () -> {
                assertSame(metadata, repository.save(metadata));
                verify(imageMetadataTable).putItem(metadata);
                org.mockito.Mockito.reset(new Object[] { imageMetadataTable });
            },
            "null metadata is forwarded", () -> {
                assertSame(null, repository.save((ImageMetadata) null));
                verify(imageMetadataTable).putItem((ImageMetadata) null);
                org.mockito.Mockito.reset(new Object[] { imageMetadataTable });
            });

        runTestCases(testCases);
    }

    @Test
    void findAll_returnsEachTableResult() {
        ImageMetadata metadata = metadata("img-1");
        Map<String, RepositoryTestCase> testCases = Map.of(
            "populated table", () -> {
                when(imageMetadataTable.scan()).thenReturn(pages);
                when(pages.items()).thenReturn(items);
                when(items.stream()).thenReturn(Stream.of(metadata));
                assertEquals(List.of(metadata), repository.findAll());
                verify(imageMetadataTable).scan();
                org.mockito.Mockito.reset(new Object[] { imageMetadataTable, pages, items });
            },
            "empty table", () -> {
                when(imageMetadataTable.scan()).thenReturn(pages);
                when(pages.items()).thenReturn(items);
                when(items.stream()).thenReturn(Stream.empty());
                assertTrue(repository.findAll().isEmpty());
                verify(imageMetadataTable).scan();
                org.mockito.Mockito.reset(new Object[] { imageMetadataTable, pages, items });
            });

        runTestCases(testCases);
    }

    @Test
    void findById_returnsPresentOrEmptyMetadata() {
        ImageMetadata metadata = metadata("img-1");
        Map<String, RepositoryTestCase> testCases = Map.of(
            "matching metadata", () -> {
                when(imageMetadataTable.getItem(org.mockito.ArgumentMatchers.<Consumer<GetItemEnhancedRequest.Builder>>any())).thenReturn(metadata);
                assertEquals(Optional.of(metadata), repository.findById("img-1"));
                verifyFindRequest("img-1");
                org.mockito.Mockito.reset(new Object[] { imageMetadataTable });
            },
            "missing metadata", () -> {
                when(imageMetadataTable.getItem(org.mockito.ArgumentMatchers.<Consumer<GetItemEnhancedRequest.Builder>>any())).thenReturn(null);
                assertEquals(Optional.empty(), repository.findById("missing"));
                verifyFindRequest("missing");
                org.mockito.Mockito.reset(new Object[] { imageMetadataTable });
            });

        runTestCases(testCases);
    }

    @Test
    void findByContentHash_returnsFirstMatchingMetadataOrEmpty() {
        ImageMetadata metadata = metadata("img-1");
        Map<String, RepositoryTestCase> testCases = Map.of(
            "matching metadata", () -> {
                when(imageMetadataTable.index("contentHash-index")).thenReturn(contentHashIndex);
                when(contentHashIndex.query(org.mockito.ArgumentMatchers.any(QueryConditional.class))).thenReturn(queryResults);
                when(queryResults.stream()).thenReturn(Stream.of(queryPage));
                when(queryPage.items()).thenReturn(List.of(metadata));

                assertEquals(Optional.of(metadata), repository.findByContentHash("hash-1"));

                org.mockito.Mockito.verify(contentHashIndex).query(org.mockito.ArgumentMatchers.any(QueryConditional.class));
                org.mockito.Mockito.reset(imageMetadataTable, contentHashIndex, queryResults, queryPage);
            },
            "missing metadata", () -> {
                when(imageMetadataTable.index("contentHash-index")).thenReturn(contentHashIndex);
                when(contentHashIndex.query(org.mockito.ArgumentMatchers.any(QueryConditional.class))).thenReturn(queryResults);
                when(queryResults.stream()).thenReturn(Stream.empty());

                assertEquals(Optional.empty(), repository.findByContentHash("missing"));

                org.mockito.Mockito.verify(contentHashIndex).query(org.mockito.ArgumentMatchers.any(QueryConditional.class));
                org.mockito.Mockito.reset(imageMetadataTable, contentHashIndex, queryResults, queryPage);
            });

        runTestCases(testCases);
    }

    @Test
    void deleteById_deletesEachRequestedKey() {
        Map<String, RepositoryTestCase> testCases = Map.of(
            "known image", () -> {
                repository.deleteById("img-1");
                verifyDeleteRequest("img-1");
                org.mockito.Mockito.reset(new Object[] { imageMetadataTable });
            },
            "another image", () -> {
                repository.deleteById("img-2");
                verifyDeleteRequest("img-2");
                org.mockito.Mockito.reset(new Object[] { imageMetadataTable });
            });

        runTestCases(testCases);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void verifyFindRequest(String imageId) {
        ArgumentCaptor<Consumer> request = ArgumentCaptor.forClass(Consumer.class);
        verify(imageMetadataTable).getItem(request.capture());
        GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
        request.getValue().accept(builder);
        assertEquals(imageId, builder.build().key().partitionKeyValue().s());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void verifyDeleteRequest(String imageId) {
        ArgumentCaptor<Consumer> request = ArgumentCaptor.forClass(Consumer.class);
        verify(imageMetadataTable).deleteItem(request.capture());
        DeleteItemEnhancedRequest.Builder builder = DeleteItemEnhancedRequest.builder();
        request.getValue().accept(builder);
        assertEquals(imageId, builder.build().key().partitionKeyValue().s());
    }

    private ImageMetadata metadata(String imageId) {
        ImageMetadata metadata = new ImageMetadata();
        metadata.setImageId(imageId);
        return metadata;
    }

    private void runTestCases(Map<String, RepositoryTestCase> testCases) {
        for (Map.Entry<String, RepositoryTestCase> testCase : testCases.entrySet()) {
            try {
                testCase.getValue().run();
            } catch (AssertionError assertionError) {
                throw new AssertionError("Test case failed: " + testCase.getKey(), assertionError);
            }
        }
    }

    @FunctionalInterface
    private interface RepositoryTestCase {
        void run();
    }
}