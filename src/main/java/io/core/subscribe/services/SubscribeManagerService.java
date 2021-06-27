package io.core.subscribe.services;

import io.core.MishaBrsvBot;
import io.opportunities.services.CoronaVirusInfoService;
import io.opportunities.services.NewsService;
import io.opportunities.enums.EventParamsEnum;
import io.opportunities.enums.EventsEnum;
import io.core.subscribe.dto.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscribeManagerService {
    private final SubscribeService subscribeService;
    private final MishaBrsvBot mishaBrsvBot;
    private final CoronaVirusInfoService coronaVirusInfoService;
    private final NewsService newsService;

    public Enumeration<Long> pullSubscribesIds() {
        return subscribeService.pullSubscribesIds();
    }

    public Subscribe pullAndRemoveSubscribeById(Long subscribeId) {
        return subscribeService.pullAndRemoveSubscribeById(subscribeId);
    }

    public boolean sendEvent(Subscribe subscribe) {
        EventsEnum event = EventsEnum.valueOf(subscribe.getEvent());
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(subscribe.getChatId());

        switch (event) {
            case CORONA_VIRUS:
                sendMessage.setText(coronaVirusInfoService.getCoronaVirusInfo());
                break;
            case NEWS:
                String news = newsService.getNews();

                sendMessage.setText(news);
                break;
        }

        try{
            mishaBrsvBot.execute(sendMessage);
            return true;
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<Subscribe> getSubscribesByTime(Timestamp timeFrom, Timestamp timeTo) {
        return subscribeService.getSubscribesByTime(timeFrom, timeTo);
    }

    public Boolean updateLastAndNextSendEvent(long subscribeId, Timestamp lastSendEvent, Timestamp nextSendEvent) {
        return subscribeService.updateLastAndNextSendEvent(subscribeId, lastSendEvent, nextSendEvent);
    }
}
