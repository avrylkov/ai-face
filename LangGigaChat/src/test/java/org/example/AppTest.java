package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppTest {

    private static Logger log = LogManager.getLogger(AppTest.class);

   @Test
    public void getUrlTest() throws IOException {

       Set<String> links = findLinks("https://docs.langchain4j.dev/tutorials/rag", 2);

       for (String link : links) {
           System.out.println(link);
       }
   }

    private static Set<String> findLinks(String url, int depth) throws IOException {

        Set<String> links = new HashSet<>();
        if (depth <= 0) {
            return links;
        }

        Document doc = Jsoup.connect(url)
                .data("query", "Java")
                .userAgent("Mozilla")
                .cookie("auth", "token")
                .timeout(3000)
                .get();

        List<Element> href = doc.select("a[href]").stream().filter(element -> element.attr("href").startsWith("https://")).toList();
        for (Element element : href) {
            links.add(element.attr("href"));
        }
        //
        Set<String> links1 = new HashSet<>(links);
        links1.forEach(link -> {
            try {
                links.addAll(findLinks(link, depth - 1));
            } catch (IOException e) {
                log.error("Error {}", link, e);
            }
        });
        return links;

    }

}
