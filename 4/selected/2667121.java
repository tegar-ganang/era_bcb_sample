package org.grailrtls.legacy.gcs.conversion.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GZIPMessage extends ServerMessage {

    private static final Logger log = LoggerFactory.getLogger(GZIPMessage.class);

    protected byte[] zippedMessage;

    protected byte[] decompressData() {
        if (this.zippedMessage == null || this.zippedMessage.length == 0) {
            log.error("No compressed data to unzip.");
            return null;
        }
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        try {
            GZIPInputStream unzipStream = new GZIPInputStream(new ByteArrayInputStream(this.zippedMessage));
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = unzipStream.read(buffer, 0, buffer.length)) > 0) {
                uncompressed.write(buffer, 0, read);
            }
        } catch (IOException ioe) {
            log.error("Couldn't decompress fingerprint.");
            return null;
        }
        if (uncompressed.size() == 0) {
            log.error("No data decompressed.");
            return null;
        }
        return uncompressed.toByteArray();
    }

    public byte[] getZippedMessage() {
        return zippedMessage;
    }

    public void setZippedMessage(byte[] zippedMessage) {
        this.zippedMessage = zippedMessage;
    }
}
