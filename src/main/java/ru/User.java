package ru;

/**
 * Created by Innokentiy on 20.02.2019.
 */
public class User{
    enum Type {ADMIN,WORKER,GUEST}
    Type userType;
    org.telegram.telegrambots.api.objects.User TGuser;
    public User (org.telegram.telegrambots.api.objects.User user, Type type){
        userType = type;
        this.TGuser = user;

    }

    public Type getUserType() {
        return userType;
    }

    public void setUserType(Type userType) {
        this.userType = userType;
    }

    public Integer getId() {
        return this.TGuser.getId();
    }

    public String getFirstName() {
        return this.TGuser.getFirstName();
    }

    public String getLastName() {
        return this.TGuser.getLastName();
    }

    public String getUserName() {
        return this.TGuser.getUserName();
    }

    public String getLanguageCode() {
        return this.TGuser.getLanguageCode();
    }

    public Boolean getBot() {
        return this.TGuser.getBot();
    }

    public String toString() {
        return "User{id=" + this.getId() + ", firstName='" + this.getFirstName() + '\'' + ", isBot=" + this.getBot() + ", lastName='" + this.getLastName() + '\'' + ", userName='" + this.getUserName() + '\'' + ", languageCode='" + this.getLanguageCode() + '\'' + '}';
    }

}
