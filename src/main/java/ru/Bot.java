package ru;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.telegram.telegrambots.ApiContext;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Innokentiy on 15.02.2019.
 */
public class Bot extends TelegramLongPollingBot {
    private static DefaultBotOptions botOptions;
    private static HttpHost httpHost;
    private static RequestConfig requestConfig;
    private static AppContext appContext;
    public static Bot create(AppContext context){
        ApiContextInitializer.init();
        appContext = context;
        botOptions = ApiContext.getInstance(DefaultBotOptions.class);
        if(appContext.propertiesManager.getProperty("proxy").equals("yes")){
            httpHost = new HttpHost("51.15.68.179",3128);
            requestConfig = RequestConfig.custom().setProxy(httpHost).setAuthenticationEnabled(false).build();
            botOptions.setRequestConfig(requestConfig);
        }

        Bot bot = new Bot(botOptions);
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
        return bot;
    }
    public Bot(DefaultBotOptions defaultBotOptions){
        super(defaultBotOptions);

    }
    public void onUpdateReceived(Update update) {
//        System.out.println("From "+update.getMessage().getFrom().getId()+": '" + update.getMessage().getText()+"'");
        /*List<SendMessage> messages = */appContext.messageManager.build(update).send();

        /*sendMsg(messages);*/
    }
    public synchronized void sendMsg(List<SendMessage> messages){


        for (int i = 0; i < messages.size(); i++) {
            try {
                execute(messages.get(i));
            } catch (TelegramApiException e) {

            }
        }

    }

    public String getBotUsername() {
        return "Restqqqbot";
    }

    public String getBotToken() {
        return "666231685:AAGKQhsNyUg_0xz996jRc2KE3wdB6epeHec";
    }
    public String getBotPaymentToken(){
        return "401643678:TEST:620391e8-e7f6-4546-89b6-5940c65f8170";
    }
}
