package org.example.client;

import org.example.model.RootSearch;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@FeignClient(value = "wikiClient", url = "https://ru.wikipedia.org/w/api.php")
public interface WikiClient {

    //@Headers("User-Agent: ai-agent/0.0 (arylkov70@mail.ru) generic-library/0.0")
    @RequestMapping(method = RequestMethod.GET, value = "?action=query&list=search&srsearch={search}&format=json&prop=extracts&exintro=true&explaintext=true&srwhat=text&srlimit=3",
    headers = "User-Agent=ai-agent/0.1 (arylkov70@mail.ru) generic-library/0.0")
    ResponseEntity<RootSearch> search(@PathVariable("search") String search);

    //@Headers("User-Agent: ai-agent/0.0 (arylkov70@mail.ru) generic-library/0.0")
    @RequestMapping(method = RequestMethod.GET, value = "?action=query&format=json&pageids={pageids}&prop=extracts&explaintext=true",
            headers = "User-Agent=ai-agent/0.1 (arylkov70@mail.ru) generic-library/0.0")
    ResponseEntity<Map<String, ?>> getPage(@PathVariable("pageids") Integer pageIds);

}
