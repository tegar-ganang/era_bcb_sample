package groom.handlers;

import groom.FileTypeHandler;
import groom.GroomException;
import groom.HttpQuery;
import groom.utils.LoggerInterface;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Mathieu Allory
 */
public class BinaryHandler implements FileTypeHandler {

    private String _resource;

    private HttpQuery _query;

    private LoggerInterface _logger;

    public void setResource(HttpQuery iQuery, LoggerInterface iLogger) {
        _query = iQuery;
        _resource = _query.Resource;
        _logger = iLogger;
    }

    public long prefetch() throws GroomException {
        _logger.log(this, 10, "Entered handler to get resource " + _resource);
        File aResourceAsFile = new File(_resource);
        if (!aResourceAsFile.exists()) throw new GroomException(GroomException.FILE_NOT_FOUND);
        if (!aResourceAsFile.isFile()) throw new GroomException(GroomException.INTERNAL_ERROR);
        if (!aResourceAsFile.canRead()) throw new GroomException(GroomException.NOT_AUTHORIZED);
        return aResourceAsFile.length();
    }

    public boolean mustFinalizeHeaders() {
        return true;
    }

    public void writeContent(OutputStream oStream) throws GroomException {
        DataInputStream aDIS = null;
        try {
            aDIS = new DataInputStream(new FileInputStream(_resource));
            while (true) oStream.write(aDIS.readUnsignedByte());
        } catch (EOFException e) {
        } catch (FileNotFoundException e) {
            throw new GroomException(GroomException.FILE_NOT_FOUND);
        } catch (IOException e) {
            throw new GroomException(GroomException.IO_ERROR);
        } finally {
            try {
                if (aDIS != null) {
                    aDIS.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    public String getOverrideMimeType() {
        return null;
    }
}
