package ru;

import org.telegram.telegrambots.api.methods.send.SendInvoice;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageReplyMarkup;
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
    enum DefaultKeybordMarkup {ADMIN_START, GUEST_BUCKET, WORKER_START, GUEST_START, ADMIN_PANEL, ADMIN_WORKERS_PANEL, ADMIN_GUESTS_PANEL,GUEST_MENU  }
    AppContext context;
    Map<DefaultKeybordMarkup,ReplyKeyboard> keyboardMarkups;
    Map<ru.User,DefaultKeybordMarkup> prevState;
    Update curUpdate;
    List<SendMessage>messages;
    List<SendPhoto> photos;
    List<DeleteMessage> deleteMessages;
    Map<Integer,List<Integer>> menuIDBuffer;
    LinkedList<String> menuCategories;
    HashMap<ru.User,List<Item>> bucket;
    //используется для удаления сообщений по тексту, для удобного обновления текста клавиатур при добавлении в корзину
    HashMap<ru.User,HashSet<String>> lastMessages = new HashMap<>();
    long curChatID;
    public MessageManager(AppContext context){
        this.context = context;
        bucket = new HashMap<>();
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

        if (update.hasMessage() && update.getMessage().hasText()) {
            User tgUser = update.getMessage().getFrom();
            curChatID = update.getMessage().getChatId();
            checkIfInDB(tgUser);
            messages = new ArrayList<>();
            photos = new ArrayList<>();
            deleteMessages = new ArrayList<>();

            ru.User user = context.dataManager.getUserWithID(tgUser.getId());

            manage(user, update.getMessage().getText());
        }else if (update.hasCallbackQuery()){
            curChatID = update.getCallbackQuery().getFrom().getId();
            User tgUser = update.getCallbackQuery().getFrom();
            checkIfInDB(tgUser);
            ru.User user = context.dataManager.getUserWithID(tgUser.getId());

            String callbackData = update.getCallbackQuery().getData();
            boolean isLast = false;
            System.out.println(callbackData);
            if (callbackData.contains("add:")){
                String productName = null;
                if (callbackData.contains("&")){
                    productName  = callbackData.substring(4,callbackData.indexOf("&"));
                    String params = callbackData.substring(9);
                    if (params.contains("last")){
                        isLast = true;
                    }
                }else{
                    productName = callbackData.substring(4);
                }

                if(bucket.get(user)==null){
                    bucket.put(user,new ArrayList<Item>());
                }
                    try{
                        Item curItem = context.dataManager.getItemWithName(productName);
                        int amount = operateAndGetAmountInBucket(curItem,user,"add");
                        EditMessageReplyMarkup markup;
                        if (isLast){
                            markup= new EditMessageReplyMarkup().
                                    setChatId(curChatID).
                                    setMessageId(update.getCallbackQuery().getMessage().getMessageId()).
                                    setReplyMarkup(formKeyboard(curItem,"last&amount="+amount));
                        }else {
                            markup= new EditMessageReplyMarkup().
                                    setChatId(curChatID).
                                    setMessageId(update.getCallbackQuery().getMessage().getMessageId()).
                                    setReplyMarkup(formKeyboard(curItem,"&amount="+amount));
                        }

                        context.bot.execute(markup);



                        System.out.println("В корзину пользователя "+user.getId()+" добавлен '"+productName+"'");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }



            else if (callbackData.contains("menu:")){
                String categoryName = callbackData.substring(5);
                List<Integer> messagesToDelete = menuIDBuffer.get(user.getId());
                System.out.println(messagesToDelete);


                HashMap<String,Item> category = context.dataManager.getItems().get(categoryName);
                photos = formMenuCategoryMessage(user, categoryName);
                sendPhotos(photos);

            }
            else if (callbackData.equals("null")){

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
                    //МОЖЕТ В БУДУЩЕМ ВЫЗЫВАТЬ БАГИ
                    if ((menuIDBuffer.get(user.getId())!=null)&&(!menuIDBuffer.get(user.getId()).isEmpty())){
                        List<Integer> messagesIDToDelete = menuIDBuffer.get(user.getId());
                        for (int i = 0; i < messagesIDToDelete.size(); i++) {
                            deleteMessages.add(new DeleteMessage().setMessageId(messagesIDToDelete.get(i)));
                        }
                        deleteMessages(deleteMessages);
                        menuIDBuffer.get(user.getId()).clear();

                    }
                    messages.add(message);
                    sendMessages(messages);
                }
                else if(inputMessage.equals("Меню")){
                    //вывод сообщения для прикрепления кнопки возврата
                    messages.add(new SendMessage().setText("Меню").setReplyMarkup(keyboardMarkups.get(DefaultKeybordMarkup.GUEST_MENU)));
                    sendMessages(messages);
                    //сохранение точки возврата
                    prevState.put(user,DefaultKeybordMarkup.GUEST_START);
                    String curCategoryName = menuCategories.getFirst();

//                    HashMap<String, Item> curCategory = context.dataManager.getItems().get(curCategoryName);
//                    Iterator<Item> itemIterator = curCategory.values().iterator();
//                    if ((menuIDBuffer.get(user.getId())!=null)&&(!menuIDBuffer.get(user.getId()).isEmpty())){
//                        List<Integer> messagesIDToDelete = menuIDBuffer.get(user.getId());
//                        for (int i = 0; i < messagesIDToDelete.size(); i++) {
//                            deleteMessages.add(new DeleteMessage().setMessageId(messagesIDToDelete.get(i)));
//                        }
//                        deleteMessages(deleteMessages);
//                        menuIDBuffer.get(user.getId()).clear();
//
//                    }
//
//                    while (itemIterator.hasNext()){
//                        Item curItem = itemIterator.next();
//                        SendPhoto photo = new SendPhoto();
//                        photo.setNewPhoto(curItem.getPhoto());
//                        photo.setChatId(curUpdate.getMessage().getChatId());
//                        photo.setCaption(curItem.getName()+"\n\n"+curItem.getDescription()+"\n\nЦена: "+curItem.getPrice()+" рублей");
//                        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
//                        InlineKeyboardButton button = new InlineKeyboardButton().setText("Добавить в корзину").setCallbackData("add:"+curItem.getName());
//                        List<InlineKeyboardButton> row = new ArrayList<>();
//                        row.add(button);
//                        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
//                        keyboard.add(row);
//                        List<InlineKeyboardButton> infoRow = new ArrayList<>();
//                        infoRow.add(new InlineKeyboardButton().setText("Категория: "+curCategoryName).setCallbackData("null"));
//                        keyboard.add(infoRow);
//
//                        if (!itemIterator.hasNext()){
//                            System.out.println(curCategoryName);
//                            int length = 0;
//
//                            List<InlineKeyboardButton> menuRow = new ArrayList<>();
//                            for (int i = 0; i < menuCategories.size(); i++) {
//                                if (!menuCategories.get(i).equals(curCategoryName)){
//                                    menuRow.add(new InlineKeyboardButton().setText(menuCategories.get(i)).setCallbackData("menu:"+menuCategories.get(i)));
//                                    length++;
//                                    if (length==2){
//                                        keyboard.add(menuRow);
//                                        menuRow = new ArrayList<>();
//                                        length=0;
//                                    }
//                                }
//
//                            }
//                            keyboard.add(menuRow);
//                        }
//                        markup.setKeyboard(keyboard);
//                        photo.setReplyMarkup(markup);
//
//                        photos.add(photo);
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

                    photos = formMenuCategoryMessage(user,curCategoryName);
                    sendPhotos(photos);


                }
                else if (inputMessage.equals("Корзина")){
                    prevState.put(user, DefaultKeybordMarkup.GUEST_START);
                    SendMessage message = new SendMessage();
                    message.setText("Корзина");
                    messages.add(message);
                    sendMessages(messages);
                }
                else if (inputMessage.equals("Заказать стол")) {
                    prevState.put(user, DefaultKeybordMarkup.GUEST_START);
                    SendMessage message=new SendMessage();
                    message.setText("Выберите дату заказа стола");
                    messages.add(message);
                    sendMessages(messages);
                } else if (inputMessage.equals("Помощь")){
                    prevState.put(user, DefaultKeybordMarkup.GUEST_START);
                    SendMessage message=new SendMessage();
                    message.setText("Помощь");
                    messages.add(message);
                    sendMessages(messages);
                } else {
                    SendMessage message=new SendMessage();
                    message.setText("Команда не найдена");
                    messages.add(message);
                    sendMessages(messages);
                }




            break;

            case ADMIN:
                    if (inputMessage.equals("/start")) {
                        SendMessage message=new SendMessage();
                        message.setReplyMarkup(keyboardMarkups.get(DefaultKeybordMarkup.ADMIN_START));
                        message.setText("Начало работы");
                        messages.add(message);
                        sendMessages(messages);
                    }
                    else if (inputMessage.equals("Панель администратора")) {
                        SendMessage message=new SendMessage();
                        prevState.put(user,DefaultKeybordMarkup.ADMIN_START);
                        message.setReplyMarkup(keyboardMarkups.get(DefaultKeybordMarkup.ADMIN_PANEL));
                        message.setText("Панель администратора");
                        messages.add(message);
                        sendMessages(messages);
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
                        deleteMessages(deleteMessages);
                        sendMessages(messages);


                    }
                    else {
                        SendMessage message=new SendMessage();
                        message.setText("Команда не найдена");
                        messages.add(message);
                        sendMessages(messages);
                    }
                    break;


            case WORKER:
                switch (inputMessage) {

                    case "/start":

                    break;
                }

        }


    }
    public InlineKeyboardMarkup formKeyboard(Item curItem, String param) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> row = new ArrayList<>();

        System.out.println(param);
        System.out.println(Integer.parseInt(param.substring(param.indexOf("amount=") + 7)));
        int amount = 0;
        if (param.contains("amount=")) {
            amount = Integer.parseInt(param.substring(param.indexOf("amount=") + 7));
        }
        InlineKeyboardButton moreButton = new InlineKeyboardButton().setText("+1").setCallbackData("add:" + curItem.getName());
        InlineKeyboardButton amountButton = new InlineKeyboardButton().setText(amount + "").setCallbackData("null");
        InlineKeyboardButton lessButton = new InlineKeyboardButton().setText("-1").setCallbackData("null");
        row.add(moreButton);
        row.add(amountButton);
        row.add(lessButton);
        if (param.contains("last")) {
            moreButton.setCallbackData(moreButton.getCallbackData() + "&last");
            amountButton.setCallbackData(amountButton.getCallbackData() + "&last");
            lessButton.setCallbackData(lessButton.getCallbackData() + "&last");
        }


        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        if (param.contains("last")) {

            int length = 0;
            List<InlineKeyboardButton> infoRow = new ArrayList<>();
            infoRow.add(new InlineKeyboardButton().setText("Категория: " + curItem.getCategory()).setCallbackData("null"));
            keyboard.add(infoRow);
            List<InlineKeyboardButton> menuRow = new ArrayList<>();
            for (int i = 0; i < menuCategories.size(); i++) {
                if (!menuCategories.get(i).equals(curItem.getCategory())) {
                    menuRow.add(new InlineKeyboardButton().setText(menuCategories.get(i)).setCallbackData("menu:" + menuCategories.get(i)));
                    length++;
                    if (length == 2) {
                        keyboard.add(menuRow);
                        menuRow = new ArrayList<>();
                        length = 0;
                    }
                }

            }

            keyboard.add(menuRow);
        }
        markup.setKeyboard(keyboard);
        return markup;
    }
    public ArrayList<SendPhoto> formMenuCategoryMessage(ru.User user, String categoryName){
        ArrayList<SendPhoto> photos = new ArrayList<>();
        HashMap<String, Item> curCategory = context.dataManager.getItems().get(categoryName);
        Iterator<Item> itemIterator = curCategory.values().iterator();
        if ((menuIDBuffer.get(user.getId())!=null)&&(!menuIDBuffer.get(user.getId()).isEmpty())){
            List<Integer> messagesIDToDelete = menuIDBuffer.get(user.getId());
            for (int i = 0; i < messagesIDToDelete.size(); i++) {
                deleteMessages.add(new DeleteMessage().setMessageId(messagesIDToDelete.get(i)));
            }
            deleteMessages(deleteMessages);
            menuIDBuffer.get(user.getId()).clear();

        }

        while (itemIterator.hasNext()){
            Item curItem = itemIterator.next();
            int amount = operateAndGetAmountInBucket(curItem,user,"");
            SendPhoto photo = new SendPhoto();
            photo.setNewPhoto(curItem.getPhoto());
            photo.setChatId(user.getId().toString());
            photo.setCaption(curItem.getName()+"\n\n"+curItem.getDescription()+"\n\nЦена: "+curItem.getPrice()+" рублей");



            if (itemIterator.hasNext()){
                photo.setReplyMarkup(formKeyboard(curItem,"amount="+amount));
            }else {
                photo.setReplyMarkup(formKeyboard(curItem,"last&amount="+amount));


            }

            photos.add(photo);
        }
        return photos;
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
    public synchronized void deleteMessages(List<DeleteMessage> messages){
        if (!messages.isEmpty()){
            for (int i = 0; i < messages.size(); i++) {
                messages.get(i).setChatId(curChatID+"");
                try {
                    context.bot.execute(messages.get(i));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
        deleteMessages = new ArrayList<>();


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
            messages.clear();
        }
    }
    public synchronized void sendPhotos(List<SendPhoto> photos){
        if (!photos.isEmpty()) {
            if (!menuIDBuffer.containsKey((int)curChatID)){
                menuIDBuffer.put((int)curChatID, new ArrayList<Integer>());
            }

            for (int i = 0; i < photos.size(); i++) {
                photos.get(i).setChatId(curChatID);
                try {
                    Message m = context.bot.sendPhoto(photos.get(i));

                    menuIDBuffer.get((int)curChatID).add(m.getMessageId());

                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            photos = new ArrayList<>();
        }
    }
    public int operateAndGetAmountInBucket(Item item, ru.User user,String operation){
        if (operation.equals("add")) {
            this.bucket.get(user).add(item);

        }
        if (this.bucket.get(user)==null){
            this.bucket.put(user, new ArrayList<Item>());
        }
        Iterator<Item> itemIterator = this.bucket.get(user).iterator();
        int amount = 0;
        while (itemIterator.hasNext()){
            Item curItem = itemIterator.next();
            if (curItem.getName().equals(item.getName())){
                amount++;
            }
        }
        return amount;
    }


}
