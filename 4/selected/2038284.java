package csv;

import java.io.File;
import java.io.FileReader;
import au.com.bytecode.opencsv.CSVReader;

public class TestCSV {

    public static final char CARACTERE_SEPARATEUR = ';';

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        File f = new File("test.csv");
        if (!f.exists()) {
            throw new Exception("Pas de fichier !");
        }
        CSVReader reader = new CSVReader(new FileReader(f), CARACTERE_SEPARATEUR);
        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            try {
                System.out.println("Ligne du CSV " + nextLine[0] + nextLine[1] + nextLine[2] + nextLine[3]);
            } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                break;
            }
        }
    }
}
