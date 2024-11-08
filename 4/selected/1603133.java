package org.omegat.filters2.xml.openoffice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.omegat.filters2.Instance;
import org.omegat.filters2.xml.XMLAbstractFilter;
import org.omegat.util.AntiCRReader;
import org.omegat.util.EncodingAwareReader;
import org.omegat.util.UTF8Writer;

/**
 * Filter to natively handle OpenOffice XML file format.
 * This format is used by OO Writer, OO Spreadsheet etc
 *
 * @author Keith Godfrey
 */
public class OOFileHandler extends XMLAbstractFilter {

    public String getFileFormatName() {
        return "OpenOffice files";
    }

    public boolean isSourceEncodingVariable() {
        return false;
    }

    public boolean isTargetEncodingVariable() {
        return false;
    }

    public Instance[] getDefaultInstances() {
        return new Instance[] { new Instance("*.sxw", ENCODING_AUTO, ENCODING_AUTO), new Instance("*.sxc", ENCODING_AUTO, ENCODING_AUTO), new Instance("*.sxg", ENCODING_AUTO, ENCODING_AUTO), new Instance("*.sxm", ENCODING_AUTO, ENCODING_AUTO), new Instance("*.sxd", ENCODING_AUTO, ENCODING_AUTO), new Instance("*.sxi", ENCODING_AUTO, ENCODING_AUTO) };
    }

    /** holds the input file */
    private File infile;

    public Reader createReader(File infile, String encoding) throws UnsupportedEncodingException, IOException {
        this.infile = infile;
        ZipInputStream zis = new ZipInputStream(new FileInputStream(infile));
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
            if (zipEntry.getName().equals("content.xml")) break;
        }
        if (zipEntry == null) throw new IOException("ERROR: Illegal OpenOffice file, no contents found."); else return new InputStreamReader(zis, "UTF-8");
    }

    /**
	 * Writing a zipfile with several components in it.
	 * First copy all unchanged components (i.e. everything but content.xml)
	 * then set the stream for the changed file to be written directly
	 */
    public Writer createWriter(File outfile, String encoding) throws UnsupportedEncodingException, IOException {
        int k_blockSize = 1024;
        int byteCount;
        char[] buf = new char[k_blockSize];
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outfile));
        zos.setMethod(ZipOutputStream.DEFLATED);
        OutputStreamWriter osw = new OutputStreamWriter(zos, "ISO-8859-1");
        BufferedWriter bw = new BufferedWriter(osw);
        ZipEntry zot;
        ZipInputStream zis = new ZipInputStream(new FileInputStream(infile));
        InputStreamReader isr = new InputStreamReader(zis, "ISO-8859-1");
        BufferedReader br = new BufferedReader(isr);
        ZipEntry zit;
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
        return new OutputStreamWriter(zos, "UTF-8");
    }

    public OOFileHandler() {
        defineFormatTag("text:a", "a");
        defineFormatTag("text:span", "f");
        defineFormatTag("text:s", "s");
        defineFormatTag("text:s/", "s/");
        defineFormatTag("text:tab-stop", "t");
        defineFormatTag("text:tab-stop/", "t/");
        defineVerbatumTag("text:footnote", "foot");
    }
}
