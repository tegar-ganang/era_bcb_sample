package be.fedict.eid.dss.document.zip;

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
import be.fedict.eid.applet.service.signer.odf.ODFUtil;

public class ZIPSignatureOutputStream extends ByteArrayOutputStream {

    private static final Log LOG = LogFactory.getLog(ZIPSignatureOutputStream.class);

    private final File originalZipFile;

    private final OutputStream targetOutputStream;

    public ZIPSignatureOutputStream(File originalZipFile, OutputStream targetOutputStream) {
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
            if (!zipEntry.getName().equals(ODFUtil.SIGNATURE_FILE)) {
                ZipEntry newZipEntry = new ZipEntry(zipEntry.getName());
                zipOutputStream.putNextEntry(newZipEntry);
                LOG.debug("copying " + zipEntry.getName());
                IOUtils.copy(zipInputStream, zipOutputStream);
            }
        }
        zipInputStream.close();
        zipEntry = new ZipEntry(ODFUtil.SIGNATURE_FILE);
        LOG.debug("writing " + zipEntry.getName());
        zipOutputStream.putNextEntry(zipEntry);
        IOUtils.write(signatureData, zipOutputStream);
        zipOutputStream.close();
    }
}
