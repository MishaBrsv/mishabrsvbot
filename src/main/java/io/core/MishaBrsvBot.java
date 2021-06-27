package io.core;

import io.configuration.BotConfig;
import io.opportunities.services.*;
import io.core.subscribe.services.SubscribeService;
import io.opportunities.enums.EventParamsEnum;
import io.opportunities.enums.EventsEnum;
import io.core.subscribe.dto.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class MishaBrsvBot extends TelegramLongPollingBot {
    private final SubscribeService subscribeService;
    private final CoronaVirusInfoService coronaVirusInfoService;
    private final EntertainmentService entertainmentService;
    private final NewsService newsService;
    private final CurrencyRateApiService currencyRateApiService;

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        log.info(update.toString());

        Message message = update.getMessage();
        long chatId = message.getChatId();
        User user = message.getFrom();


        String name = user.getFirstName();
        String text = message.getText();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        row1.add("/covid");
        row1.add("/новости");

        row2.add("/факт");
        row2.add("/курс");

        rows.add(row1);
        rows.add(row2);
        replyKeyboardMarkup.setKeyboard(rows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        if(Objects.nonNull(text)) {
            switch (text.toLowerCase()) {
                case "/подписки":
                    List<Subscribe> subscribesList = subscribeService.getSubscribesByChatId(chatId);
                    StringBuilder stringBuilder = new StringBuilder();
                    subscribesList.forEach(subscribe -> {
                        stringBuilder.append(
                                subscribe.getEvent()).append(" ").append(subscribe.getTime()).append("\n\n");
                        sendMessage.setText(stringBuilder.toString());
                    });
                    break;
                case "/подписаться-корона":
                    if(Objects.nonNull(subscribeService.subscribeOnEventWithParams(
                            chatId,
                            EventsEnum.CORONA_VIRUS,
                            EventParamsEnum.NONE,
                            Time.valueOf(LocalTime.of(13, 0, 0))))) {
                        sendMessage.setText("Привет, " + name + "! Вы успешно подписались на информацию по коронавирусу! Теперь она" +
                                " будет приходить вам в 13:00\n\n"
                                + coronaVirusInfoService.getCoronaVirusInfo());
                    } else {
                        sendMessage.setText("Привет, вы уже подписаны!");
                    }
                    break;
                case "/подписаться-новости":
                    if(Objects.nonNull(subscribeService.subscribeOnEventWithParams(
                            chatId,
                            EventsEnum.NEWS,
                            EventParamsEnum.NONE,
                            Time.valueOf(LocalTime.of(10, 0, 0))))) {

                        String news = newsService.getNews();
                        sendMessage.setText("Привет, " + name + "! Вы успешно на новости! Теперь они будут приходить вам" +
                                " в 10:00\n\n"
                                + news);
                    } else {
                        sendMessage.setText("Привет, вы уже подписаны!");
                    }
                    break;
                case "/отписаться-корона":
                    if(subscribeService.unsubscribeFromEvent(chatId, EventsEnum.CORONA_VIRUS, EventParamsEnum.NONE)){
                        sendMessage.setText("Привет, " + name + "! Вы успешно отписались от информации по коронавирусу!");
                    } else {
                        sendMessage.setText("Отписаться не удалось!");
                    }
                    break;
                case "/отписаться-новости":
                    if(subscribeService.unsubscribeFromEvent(chatId, EventsEnum.NEWS, EventParamsEnum.NONE)){
                        sendMessage.setText("Привет, " + name + "! Вы успешно отписались от новостей!");
                    } else {
                        sendMessage.setText("Отписаться не удалось!");
                    }
                    break;
                case "/коронка":
                case "/covid":
                    sendMessage.setText(coronaVirusInfoService.getCoronaVirusInfo());
                    break;
                case "/факт":
                    sendMessage.setText(entertainmentService.getFact());
                    break;
                case "/новости":
                    sendMessage.setText(newsService.getNews());
                    break;
                case "/курс": //todo временный курс доллара и евро
                    sendMessage.setText("Курс USD:" + currencyRateApiService.getRate("USDRUB") +
                            "\nКурс EUR:" + currencyRateApiService.getEurRub());
                    break;
                default:
                    break;
            }
        }

        if(Objects.nonNull(sendMessage.getText())) {
            this.sendApiMethod(sendMessage);
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.MISHABRSVBOT_USER;
    }

    @Override
    public String getBotToken() {
        return BotConfig.MISHABRSVBOT_TOKEN;
    }
}
