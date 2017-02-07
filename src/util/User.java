package util;

import java.io.Serializable;

/**
 * Created by wapchief.com on 2017/2/7 0007.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 4841783412248857282L;

    private String userName;

    private String ip;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String toString(){
        return userName+"["+ip+"]";
    }



    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        result = prime * result
                + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (ip == null) {
            if (other.ip != null)
                return false;
        } else if (!ip.equals(other.ip))
            return false;
        if (userName == null) {
            if (other.userName != null)
                return false;
        } else if (!userName.equals(other.userName))
            return false;
        return true;
    }

    public static String formatUser(User user){
        return user.getUserName()+"@"+user.getIp();
    }

    public static User parseUser(String suser){
        User user = new User();
        String[] data = suser.split("@");
        user.setUserName(data[0]);
        user.setIp(data[1]);
        return user;
    }

}

