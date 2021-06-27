package io.core.subscribe.schedule;

import io.core.subscribe.services.SubscribeManagerService;
import io.core.subscribe.dto.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscribeDistributionTask {
    private final SubscribeManagerService subscribeManagerService;

    private LocalDateTime lastFetchDatabase = LocalDateTime.MIN;
    private final ConcurrentHashMap<Subscribe, Boolean> concurrentHashMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    @PostConstruct
    public void initExecutor() {
        executor.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();

            Enumeration<Long> subscribeIds = subscribeManagerService.pullSubscribesIds();
            while(subscribeIds.hasMoreElements()) {
                concurrentHashMap.put(subscribeManagerService.pullAndRemoveSubscribeById(subscribeIds.nextElement()), false);
            }

            if(ChronoUnit.MINUTES.between(lastFetchDatabase, now) >= 10) {
                log.info("Fetch SQL Base for Subscribes Distribution: " + now.toString());
                updateSubscribesInConcurrentHashMap();
                lastFetchDatabase = now;
            } else {
                concurrentHashMap.forEach((key, value) -> {
                    if(ChronoUnit.MILLIS.between(key.getNextSendEvent().toLocalDateTime(), now) >= 0 && !value) {
                        if(subscribeManagerService.sendEvent(key)) {
                            subscribeManagerService.updateLastAndNextSendEvent(
                                    key.getSubscribeId(),
                                    key.getNextSendEvent(),
                                    Timestamp.valueOf(key.getNextSendEvent().toLocalDateTime().plusDays(1)));
                            concurrentHashMap.remove(key);
                        }
                    }
                });
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void deInit() {
        executor.shutdownNow();
    }

    @PostConstruct
    public void updateSubscribesInConcurrentHashMap() {
        LocalDateTime now = LocalDateTime.now();
        Timestamp timeFrom = Timestamp.valueOf(now);
        Timestamp timeTo = Timestamp.valueOf(now.plusMinutes(30));
        List<Subscribe> subscribeListForDistribution = subscribeManagerService.getSubscribesByTime(timeFrom, timeTo);

        subscribeListForDistribution.forEach(subscribe -> {
            if(!concurrentHashMap.containsKey(subscribe)) {
                concurrentHashMap.put(subscribe, false);
            }
        });
    }
}
