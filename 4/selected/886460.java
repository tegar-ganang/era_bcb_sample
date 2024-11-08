package com.user;

import java.io.IOException;
import com.moon.ReadEnglishWord;
import com.moon.WriteToClass;

public class User {

    private WriteToClass write = new WriteToClass();

    private ReadEnglishWord read = new ReadEnglishWord();

    public static void main(String[] args) {
        User user = new User();
        user.doSth();
    }

    @SuppressWarnings("unchecked")
    public void doSth() {
        write.addClassToCache(read.readWordToCache());
        try {
            write.print();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
