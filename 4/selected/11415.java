package co.edu.unal.ungrid.client.controller;

import java.io.File;
import co.edu.unal.ungrid.client.file.FileReader;
import co.edu.unal.ungrid.client.file.FileWriter;

public class FileManager {

    private FileManager() {
    }

    public static void readFile(final String from, final int fidx) {
        final FileReader reader = new FileReader(from, fidx);
        reader.start();
    }

    public static int readFileIndex() {
        final FileWriter writer = new FileWriter();
        return writer.readFileIndex();
    }

    public static void sendFile(final File fToSend) {
        final FileWriter writer = new FileWriter(fToSend);
        writer.start();
    }
}
