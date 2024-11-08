package net.sf.jabref.export;

import java.io.*;
import java.net.URL;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;

/**
 * @author alver
 */
public class OpenDocumentSpreadsheetCreator extends ExportFormat {

    /**
     * Creates a new instance of OpenOfficeDocumentCreator
     */
    public OpenDocumentSpreadsheetCreator() {
        super(Globals.lang("OpenDocument Spreadsheet"), "ods", null, null, ".ods");
    }

    public void performExport(final BibtexDatabase database, final MetaData metaData, final String file, final String encoding, Set<String> keySet) throws Exception {
        exportOpenDocumentSpreadsheet(new File(file), database, keySet);
    }

    public static void storeOpenDocumentSpreadsheetFile(File file, InputStream source) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        try {
            ZipEntry ze = new ZipEntry("mimetype");
            String mime = "application/vnd.oasis.opendocument.spreadsheet";
            ze.setMethod(ZipEntry.STORED);
            ze.setSize(mime.length());
            CRC32 crc = new CRC32();
            crc.update(mime.getBytes());
            ze.setCrc(crc.getValue());
            out.putNextEntry(ze);
            for (int i = 0; i < mime.length(); i++) {
                out.write(mime.charAt(i));
            }
            out.closeEntry();
            ZipEntry zipEntry = new ZipEntry("content.xml");
            out.putNextEntry(zipEntry);
            int c = -1;
            while ((c = source.read()) >= 0) {
                out.write(c);
            }
            out.closeEntry();
            addResourceFile("meta.xml", "/resource/ods/meta.xml", out);
            addResourceFile("META-INF/manifest.xml", "/resource/ods/manifest.xml", out);
        } finally {
            out.close();
        }
    }

    public static void exportOpenDocumentSpreadsheet(File file, BibtexDatabase database, Set<String> keySet) throws Exception {
        File tmpFile = File.createTempFile("opendocument", null);
        exportOpenDocumentSpreadsheetXML(tmpFile, database, keySet);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile));
        storeOpenDocumentSpreadsheetFile(file, in);
        tmpFile.delete();
    }

    public static void exportOpenDocumentSpreadsheetXML(File tmpFile, BibtexDatabase database, Set<String> keySet) {
        OpenDocumentRepresentation od = new OpenDocumentRepresentation(database, keySet);
        try {
            Writer ps = new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF8");
            try {
                DOMSource source = new DOMSource(od.getDOMrepresentation());
                StreamResult result = new StreamResult(ps);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.transform(source, result);
            } finally {
                ps.close();
            }
        } catch (Exception e) {
            throw new Error(e);
        }
        return;
    }

    private static void addResourceFile(String name, String resource, ZipOutputStream out) throws IOException {
        ZipEntry zipEntry = new ZipEntry(name);
        out.putNextEntry(zipEntry);
        addFromResource(resource, out);
        out.closeEntry();
    }

    private static void addFromResource(String resource, OutputStream out) {
        URL url = OpenDocumentSpreadsheetCreator.class.getResource(resource);
        try {
            InputStream in = url.openStream();
            byte[] buffer = new byte[256];
            synchronized (in) {
                synchronized (out) {
                    while (true) {
                        int bytesRead = in.read(buffer);
                        if (bytesRead == -1) break;
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
