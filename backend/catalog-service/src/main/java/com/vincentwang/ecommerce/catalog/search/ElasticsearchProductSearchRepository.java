package com.vincentwang.ecommerce.catalog.search;

import com.vincentwang.ecommerce.catalog.model.ProductSearchDocument;
import com.vincentwang.ecommerce.catalog.model.ProductSearchResult;
import com.vincentwang.ecommerce.catalog.repository.IProductSearchRepository;
import com.vincentwang.ecommerce.common.pagination.Pagination;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Implements product indexing and ranked fuzzy search against Elasticsearch. */
@Component
@ConditionalOnProperty(prefix = "catalog.search.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchProductSearchRepository implements IProductSearchRepository {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String indexName;

    public ElasticsearchProductSearchRepository(
            RestClient elasticsearchRestClient,
            ObjectMapper objectMapper,
            @Value("${catalog.search.elasticsearch.index-name}") String indexName) {
        this.restClient = elasticsearchRestClient;
        this.objectMapper = objectMapper;
        this.indexName = indexName;
    }

    public void initializeIndex() throws Exception {
        int status = restClient
                .head()
                .uri("/{index}", indexName)
                .exchange((request, response) -> response.getStatusCode().value());
        if (status == 404) {
            String mapping = StreamUtils.copyToString(
                    new ClassPathResource("search/product-index.json").getInputStream(), StandardCharsets.UTF_8);
            restClient
                    .put()
                    .uri("/{index}", indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapping)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    @Override
    public void index(ProductSearchDocument document) {
        try {
            restClient
                    .put()
                    .uri(builder -> builder.path("/{index}/_doc/{productId}")
                            .queryParam("version", document.version() + 1)
                            .queryParam("version_type", "external_gte")
                            .build(indexName, document.productId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(document)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict staleEvent) {
            // A newer product version is already indexed; acknowledge this replayed event.
        }
    }

    @Override
    public ProductSearchResult search(String query, int page, int size) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("from", Pagination.offsetAsInt(page, size));
        request.put("size", size);
        request.put("track_total_hits", true);
        ObjectNode bool = request.putObject("query").putObject("bool");
        bool.putArray("filter").addObject().putObject("term").put("status", "ACTIVE");
        ArrayNode should = bool.putArray("should");

        // A product whose name begins with the query should outrank an incidental
        // match inside a description, especially for short autocomplete queries.
        ObjectNode namePrefix = should.addObject().putObject("prefix").putObject("name.keyword");
        namePrefix.put("value", query);
        namePrefix.put("case_insensitive", true);
        namePrefix.put("boost", 100);

        ObjectNode wordPrefix =
                should.addObject().putObject("match_phrase_prefix").putObject("name");
        wordPrefix.put("query", query);
        wordPrefix.put("boost", 20);

        ObjectNode exactName = should.addObject().putObject("match_phrase").putObject("name");
        exactName.put("query", query);
        exactName.put("boost", 10);

        ObjectNode fuzzyWords = should.addObject().putObject("multi_match");
        fuzzyWords.put("query", query);
        fuzzyWords.put("type", "best_fields");
        fuzzyWords.put("operator", "and");
        fuzzyWords.put("fuzziness", "AUTO:1,6");
        fuzzyWords.putArray("fields").add("name^6").add("description^2");

        ObjectNode fuzzyPartial = should.addObject().putObject("multi_match");
        fuzzyPartial.put("query", query);
        fuzzyPartial.put("type", "best_fields");
        fuzzyPartial.put("operator", "and");
        fuzzyPartial.put("fuzziness", "AUTO:1,6");
        fuzzyPartial.put("prefix_length", 0);
        fuzzyPartial.put("max_expansions", 50);
        fuzzyPartial.putArray("fields").add("name.partial^4").add("description.partial");
        bool.put("minimum_should_match", 1);
        ArrayNode sort = request.putArray("sort");
        sort.addObject().put("_score", "desc");
        sort.addObject().put("productId", "asc");

        String response = restClient
                .post()
                .uri("/{index}/_search", indexName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
        return parseSearchResponse(response, page, size);
    }

    private ProductSearchResult parseSearchResponse(String response, int page, int size) {
        try {
            JsonNode root = objectMapper.readTree(response);
            long total = root.path("hits").path("total").path("value").asLong();
            long took = root.path("took").asLong();
            List<ProductSearchDocument> documents = new ArrayList<>();
            for (JsonNode hit : root.path("hits").path("hits")) {
                documents.add(objectMapper.treeToValue(hit.path("_source"), ProductSearchDocument.class));
            }
            return new ProductSearchResult(documents, page, size, total, took);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Elasticsearch returned an invalid search response", exception);
        }
    }
}
