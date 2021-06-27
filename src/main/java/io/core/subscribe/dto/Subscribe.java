package io.core.subscribe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Subscribe {
    private long subscribeId;
    private long chatId;
    private String event;
    private String eventParams;
    private Time time;
    private Timestamp nextSendEvent;
}
