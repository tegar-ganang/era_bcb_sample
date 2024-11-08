package graphbuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Concatenate {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        File directory = new File(args[0]);
        File[] files = directory.listFiles();
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(args[1])));
            for (int i = 0; i < files.length; i++) {
                BufferedReader reader = new BufferedReader(new FileReader(files[i]));
                while (reader.ready()) writer.println(reader.readLine());
                reader.close();
            }
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
