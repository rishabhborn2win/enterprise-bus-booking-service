package com.busbooking.configurations;// In: com.bussystem.config.ElasticsearchClientConfig

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import java.time.Duration;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.busbooking.repositories")
public class ElasticsearchClientConfig extends ElasticsearchConfiguration {

    // Inject the values directly from your environment or a different property name
    @Value("${es.cloud.id}")
    private String cloudId;

    @Value("${es.api-key}")
    private String apiKey;

    @Override
    public ClientConfiguration clientConfiguration() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "ApiKey " + apiKey);
        return ClientConfiguration.builder().connectedTo("3eba915db9ca4d72b2943da2da64bb09.us-central1.gcp.cloud.es.io:443")
                .usingSsl()
                .withDefaultHeaders(headers)
                .withConnectTimeout(Duration.ofSeconds(15))
                .withSocketTimeout(Duration.ofSeconds(30))
                .build();
    }
}