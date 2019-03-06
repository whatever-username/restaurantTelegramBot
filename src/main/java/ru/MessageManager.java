package ru;

import org.telegram.telegrambots.api.methods.send.SendInvoice;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiValidationException;
import org.telegram.telegrambots.updateshandlers.SentCallback;

import java.io.File;
import java.io.InputStream;
import java.security.Key;
import java.util.*;

/**
 * Created by Innokentiy on 21.02.2019.
 */
public class MessageManager {
    enum DefaultKeybordMarkup {ADMIN_START, WORKER_START, GUEST_START, ADMIN_PANEL, ADMIN_WORKERS_PANEL, ADMIN_GUESTS_PANEL,GUEST_MENU  }
    AppContext context;
    Map<DefaultKeybordMarkup,ReplyKeyboard> keyboardMarkups;
    Map<ru.User,DefaultKeybordMarkup> prevState;
    Update curUpdate;
    List<SendMessage>messages;
    List<SendPhoto> photos;
    List<DeleteMessage> deleteMessages;
    Map<Integer,List<Integer>> menuIDBuffer;
    LinkedList<String> menuCategories;
    public MessageManager(AppContext context){
        this.context = context;
        deleteMessages = new ArrayList<>();
        messages = new ArrayList<>();
        photos = new ArrayList<>();
        prevState = new HashMap<>();
        menuIDBuffer = new HashMap<>();
        menuCategories = new LinkedList<>();

        menuCategories.addAll(context.dataManager.getItems().keySet());
        keyboardMarkups = context.dataManager.loadKeyboardLayouts();

    }
    public MessageManager build(Update update){
        curUpdate = update;
        User tgUser = update.getMessage().getFrom();
        checkIfInDB(tgUser);
        if (update.hasMessage() && update.getMessage().hasText()) {
            messages = new ArrayList<>();
            photos = new ArrayList<>();
            deleteMessages = new ArrayList<>();

            ru.User user = context.dataManager.getUserWithID(tgUser.getId());

            manage(user, update.getMessage().getText());
        }else if (update.hasCallbackQuery()){
            String callbackData = update.getCallbackQuery().getData();
            System.out.println(callbackData);
            if (callbackData.contains("add:")){

            }else if (callbackData.contains("menu:")){

            }

        }

        return this;
    }
    public void checkIfInDB(User user){
        if (context.dataManager.getUserWithID(user.getId())==null){
            context.dataManager.addUser(new ru.User(user, ru.User.Type.GUEST));
            System.out.println("Пользователь "+user.getId()+" добавлен как гость");
        }
    }
    public void manage(ru.User user, String inputMessage){
        System.out.println(user.getUserType()+" "+user.getId()+" message:'"+inputMessage+"'");

        switch (user.userType) {

            case GUEST:
                if ((inputMessage.equals("/start")||
                        ((inputMessage.equals("Назад"))&&(prevState.get(user)==DefaultKeybordMarkup.GUEST_START)))) {
                    prevState.put(user,null);
                    SendMessage message=new SendMessage();
                    message.setReplyMarkup(keyboardMarkups.get(DefaultKeybordMarkup.GUEST_START));
                    message.setText("Главное меню");
                    messages.add(message); 
                }
                else if(inputMessage.equals("Меню")){
                    messages.add(new SendMessage().setText("Меню").setReplyMarkup(keyboardMarkups.get(DefaultKeybordMarkup.GUEST_MENU)));
                    prevState.put(user,DefaultKeybordMarkup.GUEST_START);
                    String curCategoryName = menuCategories.getFirst();

                    HashMap<String, Item> curCategory = context.dataManager.getItems().get(curCategoryName);
                    Iterator<Item> itemIterator = curCategory.values().iterator();
                    if ((menuIDBuffer.get(user.getId())!=null)&&(!menuIDBuffer.get(user.getId()).isEmpty())){
                        List<Integer> messagesIDToDelete = menuIDBuffer.get(user.getId());
                        for (int i = 0; i < messagesIDToDelete.size(); i++) {
                            deleteMessages.add(new DeleteMessage().setMessageId(messagesIDToDelete.get(i)));
                        }
                        menuIDBuffer.get(user.getId()).clear();

                    }

                    while (itemIterator.hasNext()){
                        Item curItem = itemIterator.next();
                        SendPhoto photo = new SendPhoto();
                        photo.setNewPhoto(curItem.getPhoto());
                        photo.setChatId(curUpdate.getMessage().getChatId());
                        photo.setCaption(curItem.getName()+"\n\n"+curItem.getDescription()+"\n\nЦена: "+curItem.getPrice()+" рублей");
                        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                        InlineKeyboardButton button = new InlineKeyboardButton().setText("Добавить в корзину").setCallbackData("add:"+curItem.getName());
                        List<InlineKeyboardButton> row = new ArrayList<>();
                        row.add(button);
                        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                        keyboard.add(row);
                        List<InlineKeyboardButton> infoRow = new ArrayList<>();
                        infoRow.add(new InlineKeyboardButton().setText("Категория: "+curCategoryName).setCallbackData("null"));
                        keyboard.add(infoRow);

                        if (!itemIterator.hasNext()){
                            System.out.println(curCategoryName);
                            int length = 0;

                            List<InlineKeyboardButton> menuRow = new ArrayList<>();
                            for (int i = 0; i < menuCategories.size(); i++) {
                                if (!menuCategories.get(i).equals(curCategoryName)){
                                    menuRow.add(new InlineKeyboardButton().setText(menuCategories.get(i)).setCallbackData("menu:"+menuCategories.get(i)));
                                    length++;
                                    if (length==2){
                                        keyboard.add(menuRow);
                                        menuRow = new ArrayList<>();
                                        length=0;
                                    }
                                }

                            }
                            keyboard.add(menuRow);
                        }
                        markup.setKeyboard(keyboard);
                        photo.setReplyMarkup(markup);

                        photos.add(photo);
//                        List<LabeledPrice> lp = new ArrayList<>();
//                        lp.add(new LabeledPrice("suka",100000));
//                        SendInvoice invoice = new SendInvoice(
//                                Integer.parseInt(curUpdate.getMessage().getChatId().toString()),
//                                "test",
//                                "test",
//                                "123",
//                                context.bot.getBotPaymentToken(),
//                                "StartParam",
//                                "RUB",
//                                lp
//                        );
//                        try {
//                            context.bot.sendInvoice(invoice);
//                        } catch (TelegramApiException e) {
//                            e.printStackTrace();
//                        }
                    }


                } else if (inputMessage.equals("Заказать стол")) {
                    prevState.put(user, DefaultKeybordMarkup.GUEST_START);
                    SendMessage message=new SendMessage();
                    message.setText("Выберите дату заказа стола");
                    messages.add(message);
                } else if (inputMessage.equals("Помощь")){
                    prevState.put(user, DefaultKeybordMarkup.GUEST_START);
                    SendMessage message=new SendMessage();
                    message.setText("Помощь");
                    messages.add(message);
                } else {
                    SendMessage message=new SendMessage();
                    message.setText("Команда не найдена");
                    messages.add(message);
                }




            break;

            case ADMIN:
                    if (inputMessage.equals("/start")) {
                        SendMessage message=new SendMessage();
                        message.setReplyMarkup(keyboardMarkups.get(DefaultKeybordMarkup.ADMIN_START));
                        message.setText("Начало работы");
                        messages.add(message);
                    }
                    else if (inputMessage.equals("Панель администратора")) {
                        SendMessage message=new SendMessage();
                        prevState.put(user,DefaultKeybordMarkup.ADMIN_START);
                        message.setReplyMarkup(keyboardMarkups.get(DefaultKeybordMarkup.ADMIN_PANEL));
                        message.setText("Панель администратора");
                        messages.add(message);
                    }
                    else if(inputMessage.equals("Список сотрудников")){

                    }
                    else if(inputMessage.equals("Список гостей")){

                    }
                    else if (inputMessage.equals("Назад")){
                        SendMessage message=new SendMessage();
                        message.setReplyMarkup(keyboardMarkups.get(prevState.get(user)));

                        //ЗАМЕНИТЬ. Сделать более рациональное дерево переходов
                        message.setText("Панель администратора");
                        messages.add(message);


                    }
                    else {
                        SendMessage message=new SendMessage();
                        message.setText("Команда не найдена");
                        messages.add(message);
                    }
                    break;


            case WORKER:
                switch (inputMessage) {

                    case "/start":

                    break;
                }

        }

    }
    public InlineKeyboardMarkup getDynamicMarkup(String name, String args){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(new InlineKeyboardButton().setText("suka"));
        keyboard.add(row1);
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
    public synchronized void sendMessages(List<SendMessage> messages){
        if (!messages.isEmpty()){
            for (int i = 0; i < messages.size(); i++) {
                messages.get(i).setChatId(curUpdate.getMessage().getChatId());
                try {
                    context.bot.execute(messages.get(i));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public synchronized void sendPhotos(List<SendPhoto> photos){
        for (int i = 0; i < photos.size(); i++) {
            photos.get(i).setChatId(curUpdate.getMessage().getChatId());
            try {
                Message m = context.bot.sendPhoto(photos.get(i));

                menuIDBuffer.get(curUpdate.getMessage().getFrom().getId()).add(m.getMessageId());

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    public synchronized void send(){
        if (!deleteMessages.isEmpty()){
            for (int i = 0; i < deleteMessages.size(); i++) {
                deleteMessages.get(i).setChatId(curUpdate.getMessage().getChatId().toString());
                try {
                    context.bot.execute(deleteMessages.get(i));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!messages.isEmpty()){
            for (int i = 0; i < messages.size(); i++) {
                messages.get(i).setChatId(curUpdate.getMessage().getChatId());
                try {
                    context.bot.execute(messages.get(i));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!photos.isEmpty()){
            if (!menuIDBuffer.containsKey(curUpdate.getMessage().getFrom().getId())){
                menuIDBuffer.put(curUpdate.getMessage().getFrom().getId(), new ArrayList<Integer>());
            }
            for (int i = 0; i < photos.size(); i++) {
                photos.get(i).setChatId(curUpdate.getMessage().getChatId());
                try {
                    Message m = context.bot.sendPhoto(photos.get(i));

                    menuIDBuffer.get(curUpdate.getMessage().getFrom().getId()).add(m.getMessageId());

                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }



    }


}
