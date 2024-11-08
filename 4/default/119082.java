import java.io.*;
import java.util.*;
import java.text.*;
import java.util.zip.*;

class OOXmlFileHandler extends XmlFileHandler {

    public OOXmlFileHandler() {
        super("OpenOffice", "sxw");
        defineFormatTag("text:a", "a");
        defineFormatTag("text:span", "f");
        defineFormatTag("text:s", "s");
        defineFormatTag("text:s/", "s/");
        defineFormatTag("text:tab-stop", "t");
        defineFormatTag("text:tab-stop/", "t/");
        defineVerbatumTag("text:footnote", "foot");
    }

    public BufferedReader createInputStream(String filename) throws IOException {
        File ifp = new File(filename);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(ifp));
        InputStreamReader isr = new InputStreamReader(zis, "UTF8");
        BufferedReader br = new BufferedReader(isr);
        ZipEntry zit = null;
        while ((zit = zis.getNextEntry()) != null) {
            if (zit.getName().equals("content.xml")) break;
        }
        if (zit == null) return null; else return br;
    }

    public BufferedWriter createOutputStream(String inFile, String outFile) throws IOException {
        int k_blockSize = 1024;
        int byteCount;
        char[] buf = new char[k_blockSize];
        File ofp = new File(outFile);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(ofp));
        zos.setMethod(ZipOutputStream.DEFLATED);
        OutputStreamWriter osw = new OutputStreamWriter(zos, "ISO-8859-1");
        BufferedWriter bw = new BufferedWriter(osw);
        ZipEntry zot = null;
        File ifp = new File(inFile);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(ifp));
        InputStreamReader isr = new InputStreamReader(zis, "ISO-8859-1");
        BufferedReader br = new BufferedReader(isr);
        ZipEntry zit = null;
        while ((zit = zis.getNextEntry()) != null) {
            if (zit.getName().equals("content.xml")) {
                continue;
            }
            zot = new ZipEntry(zit.getName());
            zos.putNextEntry(zot);
            while ((byteCount = br.read(buf, 0, k_blockSize)) >= 0) bw.write(buf, 0, byteCount);
            bw.flush();
            zos.closeEntry();
        }
        zos.putNextEntry(new ZipEntry("content.xml"));
        bw.flush();
        osw = new OutputStreamWriter(zos, "UTF8");
        bw = new BufferedWriter(osw);
        return bw;
    }
}
