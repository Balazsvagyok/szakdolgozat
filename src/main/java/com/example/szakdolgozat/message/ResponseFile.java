package com.example.szakdolgozat.message;

import com.example.szakdolgozat.model.User;

public class ResponseFile {

    private int id;
    private String name;
    private String url;
    private String type;
    private long size;
    private String description;
    private Double price;
    private User uploader;
    private boolean deleted;

    public ResponseFile(int id, String name, String url, String type, long size, String description, Double price, User uploader, boolean deleted) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.type = type;
        this.size = size;
        this.description = description;
        this.price = price;
        this.uploader = uploader;
        this.deleted = deleted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
