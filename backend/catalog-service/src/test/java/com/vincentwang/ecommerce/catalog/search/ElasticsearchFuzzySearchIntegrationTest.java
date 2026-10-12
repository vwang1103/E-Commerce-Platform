package com.vincentwang.ecommerce.catalog.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.vincentwang.ecommerce.catalog.model.ProductSearchDocument;
import com.vincentwang.ecommerce.catalog.model.ProductStatus;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
class ElasticsearchFuzzySearchIntegrationTest {

    private static final String INDEX_NAME = "products-fuzzy-test";

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> ELASTICSEARCH = new GenericContainer<>(
                    "docker.elastic.co/elasticsearch/elasticsearch:9.4.5")
            .withExposedPorts(9200)
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .waitingFor(Wait.forHttp("/").forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(2));

    @Test
    void shortPrefixRanksProductsWhoseNamesStartWithTheQueryFirst() throws Exception {
        String baseUrl = "http://" + ELASTICSEARCH.getHost() + ":" + ELASTICSEARCH.getMappedPort(9200);
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        ElasticsearchProductSearchRepository searchClient =
                new ElasticsearchProductSearchRepository(restClient, new ObjectMapper(), INDEX_NAME);
        searchClient.initializeIndex();
        searchClient.index(new ProductSearchDocument(
                41,
                "0337c503-6233-385f-ab7b-0a4c48e29513",
                600,
                "Skyline 1000-Piece Puzzle",
                "A one thousand piece jigsaw puzzle of a twilight city skyline.",
                2_200,
                "USD",
                ProductStatus.ACTIVE,
                0));
        searchClient.index(new ProductSearchDocument(
                42,
                "0337c503-6233-385f-ab7b-0a4c48e29513",
                100,
                "Sketch Smart Stylus",
                "A precise stylus for drawing and writing.",
                6_900,
                "USD",
                ProductStatus.ACTIVE,
                0));
        searchClient.index(new ProductSearchDocument(
                43,
                "0337c503-6233-385f-ab7b-0a4c48e29513",
                100,
                "Haven Smart Speaker",
                "A desk speaker with useful controls.",
                7_900,
                "USD",
                ProductStatus.ACTIVE,
                0));
        restClient.post().uri("/{index}/_refresh", INDEX_NAME).retrieve().toBodilessEntity();

        var result = searchClient.search("sk", 0, 10);

        assertThat(result.items().stream().limit(2))
                .extracting((ProductSearchDocument document) -> document.name())
                .containsExactlyInAnyOrder("Skyline 1000-Piece Puzzle", "Sketch Smart Stylus");
    }
}
