package coollemon.dataBase;

import java.io.FileReader;
import javax.xml.soap.Node;
import coollemon.kernel.ContactManager;
import net.sf.vcard4j.parser.*;

public class Vcard extends DataFormat {

    public static Vcard vcard = new Vcard();

    private Vcard() {
    }

    ;

    public ContactManager readFile(String filename) {
        ContactManager conM = new ContactManager();
        try {
            FileReader reader = new FileReader(filename);
            DomParser parser = new DomParser();
            org.w3c.dom.Node nodes = parser.parse(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conM;
    }

    public boolean writeFile(ContactManager conM, String filename) {
        try {
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        ContactManager conM = vcard.readFile("./data/Test.vcf");
        vcard.writeFile(conM, "./data/Test_write.vcf");
        conM = vcard.readFile("./data/Test_write.vcf");
        vcard.writeFile(conM, "./data/Test_write1.vcf");
    }
}
