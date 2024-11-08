import java.io.*;
import java.util.*;

/**
 * Performs tests upon the Files class.
 */
class FilesTests {

    FilesTests() throws IOException {
        String[] sets = { "US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16" };
        for (int i = 0; i < sets.length; i++) {
            Log.log(sets[i] + " write/read");
            testCharacterSet(sets[i]);
        }
    }

    /**
    * All tests repeated for various character sets.
    */
    private void testCharacterSet(String charset) throws IOException {
        String string;
        if (charset.compareTo("UTF-16") == 0) {
            string = "AæE";
            byte[] byteUTF16 = { -2, -1, 0, 65, 0, -26, 0, 69 };
            testWriteRead(byteUTF16, string, charset);
            return;
        }
        string = "A-z09!)";
        byte[] bytes = { 65, 45, 122, 48, 57, 33, 41 };
        testWriteRead(bytes, string, charset);
        string = "";
        byte[] byte0 = {};
        testWriteRead(byte0, string, charset);
        byte[] byte4096 = new byte[4096];
        Arrays.fill(byte4096, (byte) 67);
        string = new String(byte4096);
        testWriteRead(byte4096, string, charset);
        byte[] byte5000 = new byte[5000];
        Arrays.fill(byte5000, (byte) 67);
        string = new String(byte5000);
        testWriteRead(byte5000, string, charset);
        if (charset.compareTo("ISO-8859-1") == 0) {
            string = "AæE";
            byte[] byte8859 = { 65, (byte) 230, 69 };
            testWriteRead(byte8859, string, charset);
        }
        if (charset.compareTo("UTF-8") == 0) {
            string = "AæE";
            byte[] byteUTF8 = { 65, -61, -90, 69 };
            testWriteRead(byteUTF8, string, charset);
        }
    }

    /**
    * All write/read tests repeated for various data sets.
    */
    private void testWriteRead(byte[] byteData, String stringData, String charset) throws IOException {
        String byteFile = "bytefile.log";
        String stringFile = "stringfile.log";
        byte[] bresult;
        String sresult;
        Files.writeBytes(null, byteFile, byteData, false, false);
        Files.write(null, stringFile, stringData, charset, false, false);
        bresult = Files.readBytes(byteFile);
        if (Arrays.equals(byteData, bresult) == false) Log.error("byte write/byte read " + byteData.length);
        sresult = Files.read(byteFile, charset);
        if (sresult.compareTo(stringData) != 0) Log.error("byte write/string read " + byteData.length + "  " + sresult);
        bresult = Files.readBytes(stringFile);
        if (Arrays.equals(byteData, bresult) == false) Log.error("string write/byte read " + byteData.length);
        sresult = Files.read(stringFile, charset);
        if (sresult.compareTo(stringData) != 0) Log.error("string write/string read " + byteData.length);
    }
}
