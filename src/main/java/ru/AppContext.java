package ru;

import javax.xml.crypto.Data;

/**
 * Created by Innokentiy on 20.02.2019.
 */
public class AppContext {
    Bot bot;
    DataManager dataManager;
    PropertiesManager propertiesManager;
    MessageManager messageManager;
    public AppContext(){
        this.propertiesManager=new PropertiesManager();
        this.bot = Bot.create(this);
        this.dataManager=new DataManager();
        this.messageManager = new MessageManager(this);
    }
}
