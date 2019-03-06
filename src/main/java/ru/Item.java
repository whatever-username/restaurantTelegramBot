package ru;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by Innokentiy on 24.02.2019.
 */
public class Item {
    private String name;
    private String description;
    private String category;
    private float price;
    private File photo;
    public Item(){

    }

    public Item setName(String name) {
        this.name = name;
        return this;
    }

    public Item setCategory(String category) {
        this.category = category;
        return this;
    }

    public Item setDescription(String description) {
        this.description = description;
        return this;
    }

    public Item setPrice(float price) {
        this.price = price;
        return this;
    }
    public Item setPrice(String price) {
        this.price = Float.parseFloat(price);
        return this;
    }

    public Item setPhoto(String photo) throws UnsupportedEncodingException {

//        this.photo = new File(URLDecoder.decode(getClass().getClassLoader().getResource("data/photos/"+photo).getFile(),"UTF-8"));
        this.photo = new File("data/photos/"+photo);
        return this;
    }
    public Item setPhoto(File photo) {
        this.photo = photo;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public String getCategory() {
        return category;
    }

    public File getPhoto() {
        return photo;
    }

    public String getPrice() {
        return String.valueOf(price);
    }

    public String getDescription() {
        return description;
    }

}
