package io.opportunities.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyRateApiService {
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final static String API_URL = "https://www.freeforexapi.com/api/live?pairs=";
    private final static String ERROR = "Нет доступа к сервису с курсом валют";


    /**
     * Метод возвращает нужный нам курс валюты
     * @param direction - направление в курсе. Например: "USDRUB", "EURRUB"
     * @return - возвращение курса на данный момент
     */
    public String getRate(String direction) {
        String url = API_URL + direction;
        Request request = new Request.Builder().url(url).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseString = Objects.requireNonNull(response.body()).string();
            JsonNode jsonNode = objectMapper.readTree(responseString);

            JsonNode result = jsonNode.get("rates").get(direction).get("rate");

            return objectMapper.readValue(result.toString(), String.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return ERROR;
    }
    public String getEurRub() {
        return String.valueOf(Double.parseDouble(getRate("EURUSD")) * Double.parseDouble(getRate("USDRUB")));
    }
}
