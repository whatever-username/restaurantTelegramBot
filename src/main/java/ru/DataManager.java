package ru;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;

/**
 * Created by Innokentiy on 18.02.2019.
 */
public class DataManager {
    private Map<User.Type,List<User>> users;
    private Map<String, HashMap<String, Item>> items;
    public DataManager(){
        users = getUsers();
        try {
            items = loadItems();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    public Map<User.Type,List<User>> getUsers(){
        Gson gson = new Gson();

        ClassLoader urlLoader=this.getClass().getClassLoader();


        File usersFile = new File("data/users.json");



        Map bufMap = null;
        try {
            Scanner scanner = new Scanner(usersFile);

            String json = "";
            while (scanner.hasNextLine()){
                json+=scanner.nextLine();
            }
            Type type = new TypeToken<Map<User.Type, List<User>>>(){}.getType();
             bufMap = gson.fromJson(json, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bufMap==null){
            bufMap = new HashMap();
        }
        if (bufMap.get(User.Type.ADMIN)==null){
            bufMap.put(User.Type.ADMIN, new ArrayList<User>());
        }
        if (bufMap.get(User.Type.WORKER)==null){
            bufMap.put(User.Type.WORKER, new ArrayList<User>());
        }
        if (bufMap.get(User.Type.GUEST)==null){
            bufMap.put(User.Type.GUEST, new ArrayList<User>());
        }
        return bufMap;
    }
    public void addUser(User user){
        if (getUserWithID(user.getId())==null){
            users.get(user.getUserType()).add(user);
        }else{
            System.out.println("Такой пользователь уже существует");
        }
        storeUsers();

    }
    public User getUserWithID(Integer userID){
        Iterator<List<User>> it1 = users.values().iterator();
        while (it1.hasNext()){
            Iterator<User> it2 = it1.next().iterator();
            while (it2.hasNext()){
                User curUser = it2.next();
                if (curUser.getId().equals(userID)){
                    return curUser;
                }
            }
        }
        return null;

    }
    public void setUserType(User user, User.Type type){
        users.get(user.getUserType()).remove(user);
        users.get(type).add(user);
        System.out.println("Пользователь "+user.getId()+" добавлен в категорию " + type );
    }
    public void storeUsers(){
        File usersFile = new File("data/users.json");
//            usersFile = new File(URLDecoder.decode(getClass().getClassLoader().getResource("data/users.json").getFile(),"UTF-8"));


        if (!usersFile.exists()){
            try {
                usersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Gson gson = new Gson();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(usersFile));
            writer.write(gson.toJson(users));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public HashMap<String, HashMap<String,Item>> loadItems() throws UnsupportedEncodingException {
        HashMap<String, HashMap<String,Item>> items= new HashMap<>();
        File inputFile = new File("data/items.xml");
//        File inputFile = new File(URLDecoder.decode(getClass().getClassLoader().getResource("data/items.xml").getFile(),"UTF-8"));

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document doc = null;
        try {
            doc = dBuilder.parse(inputFile);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        doc.getDocumentElement().normalize();
        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

        NodeList nList = doc.getElementsByTagName("item");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE){
                Element eElement = (Element) nNode;
                Item curItem = new Item().
                        setName(eElement.getElementsByTagName("name").item(0).getTextContent()).
                        setCategory(eElement.getElementsByTagName("category").item(0).getTextContent()).
                        setDescription(eElement.getElementsByTagName("description").item(0).getTextContent()).
                        setPrice(eElement.getElementsByTagName("price").item(0).getTextContent()).
                        setPhoto(eElement.getElementsByTagName("photo").item(0).getTextContent());
                if (items.get(curItem.getCategory())==null) {
                    items.put(curItem.getCategory(),new HashMap<String, Item>());
                }
                items.get(curItem.getCategory()).put(curItem.getName(),curItem);

            }

        }
        System.out.println(items.toString());
        return items;
    }
    public HashMap<MessageManager.DefaultKeybordMarkup, ReplyKeyboard> loadKeyboardLayouts(){
        HashMap<MessageManager.DefaultKeybordMarkup, ReplyKeyboard> layouts = new HashMap<>();
        File inputFile = new File("data/keyboardLayouts.xml");
//            inputFile = new File(URLDecoder.decode(getClass().getClassLoader().getResource("data/keyboardLayouts.xml").getFile(),"UTF-8"));




        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document doc = null;
        try {
            doc = dBuilder.parse(inputFile);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        doc.getDocumentElement().normalize();
        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
        NodeList nList = doc.getElementsByTagName("keyboard");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            System.out.println("Current Element :" + nNode.getNodeName());

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                System.out.println("Имя: "+eElement.getAttribute("name"));
                String type = eElement.getAttribute("type");
                if (type.equals("reply")) {
                    System.out.println("Тип: reply");
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                    replyKeyboardMarkup.setSelective(true);
                    replyKeyboardMarkup.setResizeKeyboard(true);
                    replyKeyboardMarkup.setOneTimeKeyboard(false);
                    List<KeyboardRow> keyboard = new ArrayList<>();
                    NodeList rows = eElement.getElementsByTagName("row");
                    for (int i = 0; i < rows.getLength(); i++) {

                        Node curRow = rows.item(i);
                        KeyboardRow keyboardRow = new KeyboardRow();
                        NodeList buttons = curRow.getChildNodes();
                        for (int j = 0; j < buttons.getLength(); j++) {
                            if (buttons.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                Element curButton = (Element) buttons.item(j);
                                keyboardRow.add(new KeyboardButton(curButton.getAttribute("text")));
                                System.out.print(curButton.getAttribute("text") + " | ");
                            }
                        }
                        keyboard.add(keyboardRow);
                        System.out.println();
                    }
                    replyKeyboardMarkup.setKeyboard(keyboard);
                    layouts.put(MessageManager.DefaultKeybordMarkup.valueOf(eElement.getAttribute("name")), replyKeyboardMarkup);
                }else if(type.equals("inline")){
                    System.out.println("Тип: inline");
                    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

                    NodeList rows = eElement.getElementsByTagName("row");
                    for (int i = 0; i < rows.getLength(); i++) {
                        Node curRow = rows.item(i);
                        NodeList buttons = curRow.getChildNodes();
                        List<InlineKeyboardButton> row = new ArrayList<>();
                        for (int j = 0; j < buttons.getLength(); j++) {
                            if (buttons.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                Element curButton = (Element) buttons.item(j);
                                row.add(new InlineKeyboardButton().setText(curButton.getAttribute("text")).setCallbackData(curButton.getAttribute("text")));
                                System.out.print(curButton.getAttribute("text") + " | ");
                            }
                        }
                        keyboard.add(row);

                        System.out.println();
                    }
                    inlineKeyboardMarkup.setKeyboard(keyboard);
                    layouts.put(MessageManager.DefaultKeybordMarkup.valueOf(eElement.getAttribute("name")), inlineKeyboardMarkup);
                }


            }
        }
        return layouts;
    }
    public Map<String,HashMap<String,Item>> getItems(){
        return this.items;
    }

}
