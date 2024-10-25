package com.study.firedetection.entity;

public class ShareItem {
    private String userUID;
    private String userID;

    public ShareItem(String userUID, String userID) {
        this.userUID = userUID;
        this.userID = userID;
    }

    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }
}
