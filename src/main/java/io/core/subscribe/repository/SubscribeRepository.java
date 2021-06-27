package io.core.subscribe.repository;

import io.opportunities.enums.EventParamsEnum;
import io.opportunities.enums.EventsEnum;
import io.core.subscribe.dto.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscribeRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Подписка на определенную рассылку по событию
     * @param chatId - id чата в который нужно отправлять рассылку
     * @param event - событие которое будем отправлять
     * @param eventParams - параметры события
     * @param time - время в которое будет происходить рассылка
     * @return true/false - Подписался/подписан
     */
    @SneakyThrows
    @Nullable
    public Subscribe subscribeOnEventWithParams(
            long chatId, EventsEnum event, EventParamsEnum eventParams, Time time){
        String sql_insert = "INSERT INTO subscribes " +
                "(chat_id, event, event_params, time, last_send_event, next_send_event) VALUES (?, ?, ?, ?, ?, ?)";

        if(existSubscribeByChatIdAndParams(chatId, event, eventParams)){
            throw new RuntimeException("Subscribes exist");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime sendTime = time.toLocalTime();
        LocalDate date = now.toLocalDate();

        if(ChronoUnit.MILLIS.between(time.toLocalTime(), now.toLocalTime()) >= 0) {
            date = date.plusDays(1);
        }

        Timestamp nextSendEvent = Timestamp.valueOf(LocalDateTime.of(date, sendTime));
        Timestamp lastSendEvent = Timestamp.valueOf(now);

        return jdbcTemplate.execute((Connection connection) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql_insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, event.toString());
            preparedStatement.setString(3, eventParams.toString());
            preparedStatement.setTime(4, time);
            preparedStatement.setTimestamp(5, lastSendEvent);
            preparedStatement.setTimestamp(6, nextSendEvent);
            preparedStatement.executeUpdate();

            try(ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if(generatedKeys.next()) {
                    return new Subscribe(
                            generatedKeys.getLong(1),
                            chatId,
                            event.toString(),
                            eventParams.toString(),
                            time,
                            nextSendEvent);
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            return null;
        });
    }

    /**
     * Отписаться от рассылки
     * @param chatId - id чата в который больше не должна приходить рассылка
     * @param event - событие по которому происходит отмена
     * @param eventParams - параметры события
     * @return true/false - Отписался/Уже отписан
     */
    @SneakyThrows
    public Boolean unsubscribeFromEvent(long chatId, EventsEnum event, EventParamsEnum eventParams){
        String sql_delete = "DELETE FROM subscribes " +
                "WHERE chat_id = ? " +
                "AND event = ? " +
                "AND event_params = ?";
        return jdbcTemplate.execute((Connection connection) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql_delete);
            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, event.toString());
            preparedStatement.setString(3, eventParams.toString());
            preparedStatement.executeUpdate();

            return true;
        });
    }

    /**
     * Получение всех подписок данного чата
     * @param chatId - id чата для которого требуется получить подписки
     * @return - Список подписок
     */
    public List<Subscribe> getSubscribesByChatId(long chatId) {
        String sql_select = "SELECT chat_id, event, event_params, time, next_send_event FROM subscribes WHERE chat_id = ?";

        return jdbcTemplate.execute((Connection connection) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql_select);
            preparedStatement.setLong(1, chatId);

            return getSubscribes(preparedStatement);
        });
    }

    /**
     * Метод ищет подписки, которые находятся в заданном интервале времени
     * @param timeFrom - от данного времени
     * @param timeTo - до этого времени
     * @return возвращает список подписок во временном интервале
     */
    public List<Subscribe> getSubscribesByTime(Timestamp timeFrom, Timestamp timeTo) {
        String sql_select = "SELECT chat_id, event, event_params, time, next_send_event FROM subscribes " +
                "WHERE next_send_event between ? and ?";

        return jdbcTemplate.execute((Connection connection) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql_select);
            preparedStatement.setTimestamp(1, timeFrom);
            preparedStatement.setTimestamp(2, timeTo);

            return getSubscribes(preparedStatement);
        });
    }

    public Boolean updateLastAndNextSendEvent(long subscribeId, Timestamp lastSendEvent, Timestamp nextSendEvent) {
        String sql_select = "UPDATE subscribes " +
                "SET last_send_event = ?, next_send_event = ? " +
                "WHERE subscribe_id = ?";
        List<Long> result = jdbcTemplate.execute((Connection connection) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql_select);
            preparedStatement.setTimestamp(1, lastSendEvent);
            preparedStatement.setTimestamp(2, nextSendEvent);
            preparedStatement.setLong(3, subscribeId);

            List<Long> chatIds = new ArrayList<>();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) { // todo
                chatIds.add(resultSet.getLong(1));
            }

            return chatIds;
        });

        return Objects.nonNull(result) && result.size() > 0;
    }

    @NotNull
    private List<Subscribe> getSubscribes(PreparedStatement preparedStatement) throws SQLException {
        List<Subscribe> subscribesList = new ArrayList<>();
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Subscribe subscribe = new Subscribe();
            subscribe.setChatId(resultSet.getLong(1));
            subscribe.setEvent(resultSet.getString(2));
            subscribe.setEventParams(resultSet.getString(3));
            subscribe.setTime(resultSet.getTime(4));
            subscribe.setNextSendEvent(resultSet.getTimestamp(5));

            subscribesList.add(subscribe);
        }

        return subscribesList;
    }

    /**
     * Проверка на существования подписки по параметрам
     * @param chatId - id чата
     * @param event - событие на которое он подписан
     * @param eventParams - параметры события
     * @return true/false - Имеется подписка/не имется
     */
    private Boolean existSubscribeByChatIdAndParams(long chatId, EventsEnum event, EventParamsEnum eventParams) {
        String sql_select = "SELECT chat_id " +
                "FROM subscribes " +
                "WHERE chat_id = ? " +
                "AND event = ?" +
                "AND event_params = ?";

        List<Long> result = jdbcTemplate.execute((Connection connection) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql_select);
            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, event.toString());
            preparedStatement.setString(3, eventParams.toString());

            List<Long> chatIds = new ArrayList<>();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) { // todo
                chatIds.add(resultSet.getLong(1));
            }

            return chatIds;
        });

        return Objects.nonNull(result) && result.size() > 0;
    }
}
