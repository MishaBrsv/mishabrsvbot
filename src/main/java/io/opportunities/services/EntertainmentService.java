package io.opportunities.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;


@Service
public class EntertainmentService {

    private final static String DEFAULT_FACT = "Мы теряем 100 000 клеток мозга ежедневно.";

    public String getFact() {
        try {
            Document doc = Jsoup.connect("https://randstuff.ru/fact/").get();
            Elements fact = doc.select("table.text > tbody > tr > td");
            return fact.text();
        } catch (Exception exception) {
            return DEFAULT_FACT;
        }
    }
}
