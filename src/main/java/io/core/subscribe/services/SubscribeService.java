package io.core.subscribe.services;

import io.core.subscribe.repository.SubscribeRepository;
import io.opportunities.enums.EventParamsEnum;
import io.opportunities.enums.EventsEnum;
import io.core.subscribe.dto.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscribeService {
    private final SubscribeRepository subscribeRepository;
    private final ConcurrentHashMap<Long, Subscribe> concurrentHashMap = new ConcurrentHashMap<>();

    @SneakyThrows
    @Nullable
    public Subscribe subscribeOnEventWithParams(
            long chatId, EventsEnum event, EventParamsEnum eventParams, Time time) {

        Subscribe subscribe = subscribeRepository.subscribeOnEventWithParams(chatId, event, eventParams, time);

        LocalTime now = LocalTime.now();
        if(Math.abs(ChronoUnit.MINUTES.between(now, time.toLocalTime())) <= 10 && Objects.nonNull(subscribe)) {
            concurrentHashMap.put(subscribe.getSubscribeId(), subscribe);
        }
        return subscribe;
    }

    public Enumeration<Long> pullSubscribesIds() {
        return concurrentHashMap.keys();
    }

    public Subscribe pullAndRemoveSubscribeById(Long subscribeId) {
        return concurrentHashMap.remove(subscribeId);
    }

    @SneakyThrows
    public Boolean unsubscribeFromEvent(long chatId, EventsEnum event, EventParamsEnum eventParams) {
        return subscribeRepository.unsubscribeFromEvent(chatId, event, eventParams);
    }

    public List<Subscribe> getSubscribesByChatId(long chatId) {
        return subscribeRepository.getSubscribesByChatId(chatId);
    }

    public List<Subscribe> getSubscribesByTime(Timestamp timeFrom, Timestamp timeTo) {
        return subscribeRepository.getSubscribesByTime(timeFrom, timeTo);
    }

    public Boolean updateLastAndNextSendEvent(long subscribeId, Timestamp lastSendEvent, Timestamp nextSendEvent) {
        return subscribeRepository.updateLastAndNextSendEvent(subscribeId, lastSendEvent, nextSendEvent);
    }
}
