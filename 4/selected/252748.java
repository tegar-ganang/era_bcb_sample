package net.alcuria.ball;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.content.res.AssetManager;

public class Map {

    String tileset;

    int sheetWidth;

    int tileWidth;

    int width;

    int height;

    int[][] lowerMap;

    int[][] upperMap;

    int[][] collisionMap;

    public Map(String filename, AssetManager assets) {
        width = 0;
        height = 0;
        int mapIndex = 0;
        InputStream inputStream = null;
        String text;
        try {
            inputStream = assets.open(filename);
            text = loadTextFile(inputStream);
            String[] lines = text.split("\\r?\\n");
            sheetWidth = Integer.parseInt(lines[0]);
            tileWidth = Integer.parseInt(lines[1]);
            width = Integer.parseInt(lines[2]);
            height = Integer.parseInt(lines[3]);
            mapIndex = 0;
            lowerMap = new int[width][height];
            String[] lineData = lines[4].split("\\s");
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < width; k++) {
                    lowerMap[k][j] = Integer.parseInt(lineData[mapIndex]);
                    mapIndex++;
                }
            }
            mapIndex = 0;
            upperMap = new int[width][height];
            lineData = lines[5].split("\\s");
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < width; k++) {
                    upperMap[k][j] = Integer.parseInt(lineData[mapIndex]);
                    mapIndex++;
                }
            }
            mapIndex = 0;
            collisionMap = new int[width][height];
            lineData = lines[6].split("\\s");
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < width; k++) {
                    collisionMap[k][j] = Integer.parseInt(lineData[mapIndex]);
                    mapIndex++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load map: " + filename);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public String loadTextFile(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[4096];
        int len = 0;
        while ((len = inputStream.read(bytes)) > 0) byteStream.write(bytes, 0, len);
        return new String(byteStream.toByteArray(), "UTF8");
    }
}
