package textprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class CountryListProcessor {

    public static void main(String[] args) throws Exception {
        File inputFile = new File(args[0]);
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        StringBuffer stringBuffer = new StringBuffer();
        String readed;
        while ((readed = reader.readLine()) != null) stringBuffer.append(readed + "\r\n");
        readed = stringBuffer.toString();
        reader.close();
        File outputFile = new File(args[0] + ".output");
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        readed = readed.replaceAll("\\t+", "#");
        readed = readed.replaceAll("\\s*#\\s*", "#");
        readed = readed.replaceAll("Latitude,.*", "");
        readed = readed.replaceAll("Flag of", "");
        readed = readed.replaceAll("^\\s+$", "");
        writer.write(readed);
        writer.close();
    }
}
