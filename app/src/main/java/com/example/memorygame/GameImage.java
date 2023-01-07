package com.example.memorygame;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class GameImage {

    // Grab all files from pictures directory
    // Extract file path
    // Create two GameImage objects per file with same id
    // Store all objects into ArrayList
    // Shuffle and return ArrayList
    public static ArrayList<Image> createGameImageList(Context context) {
        ArrayList<Image> gameImages = new ArrayList<>();

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] filesInDir = dir.listFiles();

        int id = 1;

        for (File file : filesInDir) {
            String filePath = file.getAbsolutePath();
            Image gameImage = new Image(id, filePath);
            gameImages.add(gameImage);
            gameImages.add(gameImage);
            id++;
        }

        Collections.shuffle(gameImages);

        return gameImages;
    }
}
