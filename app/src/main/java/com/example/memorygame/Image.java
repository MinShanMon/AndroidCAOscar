package com.example.memorygame;

public class Image {
    private int id;
    private String filePath;


    public int getId(){ return id;}

    public String getFilePath(){return filePath;}

    public Image(int id, String filePath){
        this.id = id;
        this.filePath = filePath;
    }
}
