package be.fedict.eid.applet.service.signer.asic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ASiC signature output stream.
 * <p/>
 * This output stream implementation injects an ASiC signature into an ASiC
 * file.
 * 
 * @author Frank Cornelis.
 * 
 */
public class ASiCSignatureOutputStream extends ByteArrayOutputStream {

    private static final Log LOG = LogFactory.getLog(ASiCSignatureOutputStream.class);

    private final File originalZipFile;

    private final OutputStream targetOutputStream;

    /**
	 * Main constructor.
	 * 
	 * @param originalZipFile
	 *            the original ASiC document.
	 * @param targetOutputStream
	 *            the output stream in which to copy the original ASiC document,
	 *            together with the new ASiC signature.
	 */
    public ASiCSignatureOutputStream(File originalZipFile, OutputStream targetOutputStream) {
        this.originalZipFile = originalZipFile;
        this.targetOutputStream = targetOutputStream;
    }

    @Override
    public void close() throws IOException {
        super.close();
        byte[] signatureData = toByteArray();
        ZipOutputStream zipOutputStream = new ZipOutputStream(this.targetOutputStream);
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(this.originalZipFile));
        ZipEntry zipEntry;
        while (null != (zipEntry = zipInputStream.getNextEntry())) {
            if (!zipEntry.getName().equals(ASiCUtil.SIGNATURE_FILE)) {
                ZipEntry newZipEntry = new ZipEntry(zipEntry.getName());
                zipOutputStream.putNextEntry(newZipEntry);
                LOG.debug("copying " + zipEntry.getName());
                IOUtils.copy(zipInputStream, zipOutputStream);
            }
        }
        zipInputStream.close();
        zipEntry = new ZipEntry(ASiCUtil.SIGNATURE_FILE);
        LOG.debug("writing " + zipEntry.getName());
        zipOutputStream.putNextEntry(zipEntry);
        IOUtils.write(signatureData, zipOutputStream);
        zipOutputStream.close();
    }
}
