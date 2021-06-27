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
@Slf4j
@RequiredArgsConstructor
public class NewsService {
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    private final static String NEWS_API = "https://newsapi.org/v2/top-headlines?country=ru&apiKey=59507a96a70c4a419f7503a70d12e313";
    private final static String ERROR = "Нет доступа к сервису с новостями!";

    public String getNews() {

        Request request = new Request.Builder().url(NEWS_API).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseString = Objects.requireNonNull(response.body()).string();
            JsonNode jsonNode = objectMapper.readTree(responseString);

            StringBuilder result = new StringBuilder();

            JsonNode count = jsonNode.get("totalResults");
            if(count.intValue() < 7) {
                return ERROR;
            }
            for (int i = 1; i<=7 ; i++) {
                JsonNode title = jsonNode.get("articles").get(i).get("title");
                result.append(title);
                JsonNode url = jsonNode.get("articles").get(i).get("url");
                result.append("\nДоступ: ").append(url).append("\n\n");
            }

            return result.toString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return ERROR;
    }
}
