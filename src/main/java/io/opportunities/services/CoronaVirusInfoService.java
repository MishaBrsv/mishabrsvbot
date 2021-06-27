package io.opportunities.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opportunities.pojo.Country;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoronaVirusInfoService {
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final static String ERROR = "Нет доступа к сервису со статистикой по COVID-19";

    @SneakyThrows
    public String getCoronaVirusInfo() {
        Request request = new Request.Builder().url("https://api.covid19api.com/summary").build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseString = Objects.requireNonNull(response.body()).string();
            JsonNode jsonNode = objectMapper.readTree(responseString);
            JsonNode global = jsonNode.get("Global");
            JsonNode countries = jsonNode.get("Countries");
            List<Country> countriesList = objectMapper.readValue(
                    countries.toString(), objectMapper.getTypeFactory().constructCollectionType(
                            List.class, Country.class));

            Country russia = countriesList.stream()
                    .filter(country -> "Russian Federation".equals(country.getCountry()))
                    .findAny().orElse(null);
            Pair<Long, Long> russiaUpd = getRospotrebnadzorInfo();
            long newRusConfirmed = russiaUpd.getLeft() - russia.getTotalConfirmed();
            if(newRusConfirmed == 0) {
                newRusConfirmed = russia.getNewConfirmed();
            }
            long newRusRecovered = russiaUpd.getRight() - russia.getTotalRecovered();
            if (newRusRecovered == 0) {
                newRusRecovered = russia.getNewRecovered();
            }
            String sendString = "Эй! У меня для вас новая статистика по коронавирусу!\n\n"
                    + "В мире:\n"
                    + "Новых случаев заражения: " + global.get("NewConfirmed").toString() + "\n"
                    + "Всего случаев заражения: " + global.get("TotalConfirmed").toString() + "\n"
                    + "Новых случаев смерти: " + global.get("NewDeaths").toString() + "\n"
                    + "Всего смертей: " + global.get("TotalDeaths").toString() + "\n"
                    + "Новые выздоровевшие: " + global.get("NewRecovered").toString() + "\n"
                    + "Всего выздоровело: " + global.get("TotalRecovered").toString() + "\n\n"
                    + (Objects.isNull(russia) ? "" :
                    "В РФ:\n"
                            + "Новых случаев заражения: " + newRusConfirmed + "\n"
                            + "Всего случаев заражения: " + russiaUpd.getLeft() + "\n"
                            + "Новых случаев смерти: " + russia.getNewDeaths() + "\n"
                            + "Всего смертей: " + russia.getTotalDeaths() + "\n"
                            + "Новые выздоровевшие: " + newRusRecovered + "\n"
                            + "Всего выздоровело: " + russiaUpd.getRight() + "\n\n"
            );
            return sendString;
        } catch (Exception ex) {
            return ERROR;
        }
    }

    private Pair<Long, Long> getRospotrebnadzorInfo() throws IOException {
        String url = "https://www.rospotrebnadzor.ru/about/info/news_time/news_details.php?ELEMENT_ID=13566";

        Document doc = Jsoup.connect(url).get();
        String totalConfirmed = "body > div.content.main > div > " +
                "div > div.news-detail > div:nth-child(1) > " +
                "p:nth-child(5) > font > font:nth-child(1) > b";

        String totalRecovered = "body > div.content.main > div >" +
                " div > div.news-detail > div:nth-child(1) > p:nth-child(6) > font > b";

        Elements totalConf = doc.select(totalConfirmed);
        Elements totalRecov = doc.select(totalRecovered);

        Long totalConfirmedLong = Long.parseLong(
                totalConf.text()
                        .substring(0, totalConf.text().lastIndexOf(" "))
                        .replaceAll(" ", ""));

        Long totalRecoveredLong = Long.parseLong(
                totalRecov.text()
                        .substring(0, totalRecov.text().lastIndexOf(" "))
                        .replaceAll(" ", ""));

        return Pair.of(totalConfirmedLong, totalRecoveredLong);
    }
}
