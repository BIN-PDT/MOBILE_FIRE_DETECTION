package com.study.firedetection.entity;

import java.util.List;

public class HistoryItem {
    private String timestamp;
    private List<String> listCaptureUrl;

    public HistoryItem(String timestamp, List<String> listCaptureUrl) {
        this.timestamp = timestamp;
        this.listCaptureUrl = listCaptureUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getListCaptureUrl() {
        return listCaptureUrl;
    }

    public void setListCaptureUrl(List<String> listCaptureUrl) {
        this.listCaptureUrl = listCaptureUrl;
    }
}
