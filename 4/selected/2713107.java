package joliex.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

/**
 *
 * @author Fabrizio Montesi
 */
public class ZipUtils extends JavaService {

    private static int BUFFER_SIZE = 1024;

    private static ByteArray inputStreamToByteArray(InputStream istream) throws IOException {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        byte buffer[] = new byte[BUFFER_SIZE];
        int read;
        while ((read = istream.read(buffer)) >= 0) {
            ostream.write(buffer, 0, read);
        }
        return new ByteArray(ostream.toByteArray());
    }

    public Value readEntry(Value request) throws FaultException {
        Value response = Value.create();
        try {
            ZipFile file = new ZipFile(request.getFirstChild("filename").strValue());
            ZipEntry entry = file.getEntry(request.getFirstChild("entry").strValue());
            if (entry != null) {
                response.setValue(inputStreamToByteArray(new BufferedInputStream(file.getInputStream(entry))));
            }
        } catch (IOException e) {
            throw new FaultException(e);
        }
        return response;
    }

    public ByteArray zip(Value request) throws FaultException {
        ByteArrayOutputStream bbstream = new ByteArrayOutputStream();
        try {
            ZipOutputStream zipStream = new ZipOutputStream(bbstream);
            ZipEntry zipEntry;
            byte[] bb;
            for (Entry<String, ValueVector> entry : request.children().entrySet()) {
                zipEntry = new ZipEntry(entry.getKey());
                zipStream.putNextEntry(zipEntry);
                bb = entry.getValue().first().byteArrayValue().getBytes();
                zipStream.write(bb, 0, bb.length);
                zipStream.closeEntry();
            }
            zipStream.close();
        } catch (IOException e) {
            throw new FaultException(e);
        }
        return new ByteArray(bbstream.toByteArray());
    }
}
