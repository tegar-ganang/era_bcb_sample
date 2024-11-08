package org.dengues.core.export;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf Qiang.Zhang.Adolf@gmail.com 2008-3-26 qiang.zhang $
 * 
 */
public class FileSystemExporterFullPath {

    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    public void write(String resource, String destinationPath) throws IOException, CoreException {
        OutputStream output = null;
        InputStream contentStream = null;
        try {
            contentStream = new BufferedInputStream(new FileInputStream(resource));
            output = new BufferedOutputStream(new FileOutputStream(destinationPath));
            int available = contentStream.available();
            available = available <= 0 ? DEFAULT_BUFFER_SIZE : available;
            int chunkSize = Math.min(DEFAULT_BUFFER_SIZE, available);
            byte[] readBuffer = new byte[chunkSize];
            int n = contentStream.read(readBuffer);
            while (n > 0) {
                output.write(readBuffer, 0, n);
                n = contentStream.read(readBuffer);
            }
        } finally {
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException e) {
                    IDEWorkbenchPlugin.log("Error closing input stream for file: " + resource, e);
                }
            }
            if (output != null) {
                output.close();
            }
        }
    }
}
