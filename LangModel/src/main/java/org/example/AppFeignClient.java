package org.example;


import org.example.client.WikiClient;
import org.example.model.RootSearch;
import org.example.service.tool.WikiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@SpringBootApplication(scanBasePackages = "org.example.service")
@EnableFeignClients(basePackages = "org.example.client")
@Configuration
public class AppFeignClient implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppFeignClient.class);

    private final WikiClient wikiClient;

    private final WikiService wikiService;

    public AppFeignClient(WikiClient wikiClient, WikiService wikiService) {
        this.wikiClient = wikiClient;
        this.wikiService = wikiService;
    }

    public static void main(String[] args) {
        SpringApplication.run(AppFeignClient.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //testWikiClient();
        testWikiService();
    }

    private void testWikiService() {
        String query = "село Новосильское Воронежской области";
        String text = wikiService.getWiki(query);
        log.info("Text: {}", text);
        String embedding = wikiService.getEmbedding(query, text);
        log.info("Embedding: {}", embedding);
    }

    private void testWikiClient() {
        ResponseEntity<RootSearch> search = wikiClient.search("село Новосильское Воронежской области");
        if (search.getStatusCode().is2xxSuccessful()) {
            RootSearch body = search.getBody();
            log.info(body.toString());
        } else {
            log.info(search.getStatusCode().toString());
        }
        //
        ResponseEntity<Map<String,?>> pageEntity = wikiClient.getPage(364638);
        if (pageEntity.getStatusCode().is2xxSuccessful()) {
            Map query = (Map) pageEntity.getBody().get("query");
            Map pages = (Map) query.get("pages");
            Map page = (Map) pages.get("364638");
            String extract = page.get("extract").toString();
            log.info(extract);
        }
    }


}
