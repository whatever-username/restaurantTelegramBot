package ru;


import java.io.*;
import java.util.*;

/**
 * Created by Innokentiy on 17.02.2019.
 */
public class PropertiesManager {
    private Properties properties;

    public PropertiesManager(){
        properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("data/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public String getProperty(String key){
        return properties.getProperty(key);
    }



}
