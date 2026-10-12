package com.vincentwang.ecommerce.catalog.infrastructure.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vincentwang.ecommerce.catalog.application.model.ProductSearchDocument;
import com.vincentwang.ecommerce.catalog.domain.model.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class ElasticsearchProductSearchClientTest {

    private MockRestServiceServer server;
    private ElasticsearchProductSearchClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ElasticsearchProductSearchClient(builder.build(), new ObjectMapper(), "products-v2");
    }

    @Test
    void searchesActiveProductsWithStableTieBreaking() {
        server.expect(requestTo("http://localhost:9200/products-v2/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"productId\":\"asc\"")))
                .andRespond(withSuccess(searchResponse(), MediaType.APPLICATION_JSON));

        var result = client.search("developer laptop", 0, 20);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.tookMillis()).isEqualTo(7);
        assertThat(result.items()).singleElement().satisfies(product -> {
            assertThat(product.productId()).isEqualTo(42);
            assertThat(product.status()).isEqualTo(ProductStatus.ACTIVE);
        });
        server.verify();
    }

    @Test
    void usesInfixFieldsAndOneEditFuzzinessForShortQueries() {
        server.expect(requestTo("http://localhost:9200/products-v2/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name.keyword\":{\"value\":\"sk\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"case_insensitive\":true")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"fuzziness\":\"AUTO:1,6\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name.partial^4\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"minimum_should_match\":1")))
                .andRespond(withSuccess(skylineSearchResponse(), MediaType.APPLICATION_JSON));

        var result = client.search("sk", 0, 5);

        assertThat(result.items()).singleElement().satisfies(product -> {
            assertThat(product.name()).isEqualTo("Skyline 1000-Piece Puzzle");
        });
        server.verify();
    }

    @Test
    void acknowledgesAnOlderReplayedProductEvent() {
        server.expect(requestTo("http://localhost:9200/products-v2/_doc/42?version=1&version_type=external_gte"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        client.index(new ProductSearchDocument(
                42,
                "0337c503-6233-385f-ab7b-0a4c48e29513",
                100,
                "Older name",
                "Older description",
                129_900,
                "USD",
                ProductStatus.ACTIVE,
                0));

        server.verify();
    }

    private static String searchResponse() {
        return """
                {
                  "took": 7,
                  "hits": {
                    "total": {"value": 1, "relation": "eq"},
                    "hits": [{
                      "_source": {
                        "productId": 42,
                        "userId": "0337c503-6233-385f-ab7b-0a4c48e29513",
                        "categoryId": 100,
                        "name": "Developer Laptop",
                        "description": "Fast laptop",
                        "priceMinor": 129900,
                        "currency": "USD",
                        "status": "ACTIVE",
                        "version": 1
                      }
                    }]
                  }
                }
                """;
    }

    private static String skylineSearchResponse() {
        return """
                {
                  "took": 3,
                  "hits": {
                    "total": {"value": 1, "relation": "eq"},
                    "hits": [{
                      "_source": {
                        "productId": 41,
                        "userId": "0337c503-6233-385f-ab7b-0a4c48e29513",
                        "categoryId": 600,
                        "name": "Skyline 1000-Piece Puzzle",
                        "description": "A one thousand piece jigsaw puzzle of a twilight city skyline.",
                        "priceMinor": 2200,
                        "currency": "USD",
                        "status": "ACTIVE",
                        "version": 0
                      }
                    }]
                  }
                }
                """;
    }
}
