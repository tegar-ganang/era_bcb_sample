package org.posterita.core.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.posterita.core.exception.IOOperationException;

public class IOUtil {

    public static byte[] getByteArray(InputStream inStream) throws IOOperationException {
        try {
            BufferedInputStream bufferedInStream = new BufferedInputStream(inStream);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            BufferedOutputStream bufferedOutStream = new BufferedOutputStream(outStream);
            byte buffer[] = new byte[1024];
            int read = 0;
            while ((read = bufferedInStream.read(buffer)) != -1) {
                bufferedOutStream.write(buffer, 0, read);
            }
            bufferedOutStream.flush();
            byte retData[] = outStream.toByteArray();
            bufferedOutStream.close();
            bufferedInStream.close();
            outStream.close();
            return retData;
        } catch (IOException ex) {
            throw new IOOperationException("Could not get data from InputStream", ex);
        }
    }

    public static StringBuffer getContent(Reader reader, String newLineChar) throws IOOperationException {
        try {
            StringBuffer retDataBuffer = new StringBuffer(1000);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) retDataBuffer.append(line + newLineChar);
            return retDataBuffer;
        } catch (IOException ex) {
            throw new IOOperationException("Could not read data from reader", ex);
        }
    }
}
