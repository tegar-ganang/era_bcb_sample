package GUI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import org.xml.sax.SAXException;
import basis.Woord;
import basis.WoordenXMLWriter;

/**
 * @author Ghost
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WoordController {

    private WoordenMakenView woordenMakenView;

    private SpelController controller;

    private WoordenXMLWriter wXmlWriter;

    private boolean packFrame = false;

    private File importFile = null;

    public WoordController(SpelController controller) {
        this.controller = controller;
    }

    public void closeApp() {
        controller.closeApp();
    }

    public void setWoordenMakenView(WoordenMakenView woordenMakenView) {
        this.woordenMakenView = woordenMakenView;
    }

    public boolean maakWoordenSet(File file, ArrayList nieuweWoorden, String categorie) {
        wXmlWriter = new WoordenXMLWriter(file.getAbsolutePath());
        try {
            wXmlWriter.nieuweWoordenSet(categorie);
            Iterator nieuweWoordenIterator = nieuweWoorden.iterator();
            while (nieuweWoordenIterator.hasNext()) {
                Woord tempWoord = (Woord) nieuweWoordenIterator.next();
                wXmlWriter.voegWoordToe(tempWoord.getWoord());
            }
            wXmlWriter.schrijfWoordenSet();
        } catch (SAXException s) {
            s.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean voegAanWoordenSetToe(File file, ArrayList nieuweWoorden, String categorie) {
        if (!checkImportFile(file)) {
            return false;
        }
        wXmlWriter = new WoordenXMLWriter(file.getAbsolutePath());
        try {
            System.out.println("File: " + file.getAbsolutePath());
            wXmlWriter.laadBestand(file.getAbsolutePath());
            Iterator nieuweWoordenIterator = nieuweWoorden.iterator();
            while (nieuweWoordenIterator.hasNext()) {
                Woord tempWoord = (Woord) nieuweWoordenIterator.next();
                wXmlWriter.voegWoordToe(tempWoord.getWoord());
            }
            wXmlWriter.schrijfWoordenSet();
        } catch (SAXException s) {
            s.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean checkImportFile(File importFile) {
        this.importFile = importFile;
        if ((importFile.getName()).toLowerCase().endsWith(".xml") && checkFileContents(importFile.getAbsolutePath())) {
            return true;
        }
        return false;
    }

    public boolean importFile() {
        File destFile = createValidFile(importFile);
        System.out.println("returning filename: " + destFile.getAbsolutePath() + " : " + importFile.getAbsolutePath());
        try {
            copyFile(importFile, destFile);
            return true;
        } catch (IOException s) {
            return false;
        }
    }

    private boolean checkFileContents(String inFileName) {
        String line = "";
        try {
            BufferedReader inFile = new BufferedReader(new FileReader(inFileName));
            try {
                line = inFile.readLine();
            } catch (IOException s) {
            }
            try {
                line = inFile.readLine();
            } catch (IOException s) {
            }
            String testLine = line.substring(1, 11);
            if (testLine.equals("woordenset")) {
                return true;
            }
        } catch (FileNotFoundException s) {
            s.printStackTrace();
            return false;
        }
        return false;
    }

    /** Fast & simple file copy. */
    public static void copyFile(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    private File createValidFile(File importFile) {
        String newFileName = importFile.getName();
        File destFile = null;
        if (!importFile.getName().substring(0, 11).equals("woordenset_")) {
            destFile = new File("XML/woordenset_" + newFileName);
        } else {
            destFile = new File("XML/" + newFileName);
        }
        int added_to_filename = 0;
        do {
            added_to_filename++;
            newFileName = destFile.getName().substring(0, destFile.getName().length() - 4) + String.valueOf(added_to_filename) + ".xml";
            destFile = new File("XML/" + newFileName);
        } while (destFile.exists());
        return destFile;
    }
}
