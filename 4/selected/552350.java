package palindrume.wiki;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.xml.sax.SAXException;
import palindrume.bd.BDBase;
import com.sun.org.apache.xerces.internal.parsers.SAXParser;

/**
 * Des essais
 * 
 */
public class WikiDumpFileParser {

    /**
	 * Utilisation de Sax pour parcourir l'xml
	 * 
	 */
    public static void create_db(BDBase dico_file_path, String input_file_path) {
        DatabaseWriter dw = new DatabaseWriter(dico_file_path);
        dw.cleanTable();
        SAXParser parser = new SAXParser();
        parser.setContentHandler(new WikiSaxHandler(dw));
        try {
            parser.parse(input_file_path);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Découpe le méga fichier xml en fichiers de BUFFER_SIZE, plus faciles à
	 * ouvrir...
	 * 
	 */
    public static void decoupe(String input_file_path) {
        final int BUFFER_SIZE = 2000000;
        try {
            FileInputStream fr = new FileInputStream(input_file_path);
            byte[] cbuf = new byte[BUFFER_SIZE];
            int n_read = 0;
            int i = 0;
            boolean bContinue = true;
            while (bContinue) {
                n_read = fr.read(cbuf, 0, BUFFER_SIZE);
                if (n_read == -1) break;
                FileOutputStream fo = new FileOutputStream("f_" + ++i);
                fo.write(cbuf, 0, n_read);
                fo.close();
            }
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
    }
}
