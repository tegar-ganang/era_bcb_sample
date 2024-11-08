package net.sourceforge.bulkmailer;

import java.io.*;
import java.util.*;
import java.text.*;

public class ActivityLogWriter {

    private CsvWriter csvWriter;

    DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS z");

    public ActivityLogWriter(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            csvWriter = initializeFile(file);
        } else {
            csvWriter = new CsvWriter(file);
        }
    }

    public void write(Date now, Card card, String title, String description, int threadNum) throws IOException {
        String emailAddress = "";
        String displayName = "";
        if (card != null) {
            emailAddress = card.getEmailAddress();
            displayName = card.getDisplayName();
        }
        csvWriter.writeLine(dateFormatter.format(now), emailAddress, displayName, title, description, String.valueOf(threadNum));
        csvWriter.flush();
    }

    private static CsvWriter initializeFile(File file) throws IOException {
        file.createNewFile();
        CsvWriter csvWriter = new CsvWriter(file);
        csvWriter.writeLine("Date", "Email Address", "Display Name", "Title", "Description", "Thread Number");
        return csvWriter;
    }

    public void close() throws IOException {
        this.csvWriter.close();
    }
}
