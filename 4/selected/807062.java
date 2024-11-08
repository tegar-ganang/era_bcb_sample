package dfdl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import javax.xml.bind.JAXBException;
import dfdl.exception.DFDLException;

/**
 * @author d3m305
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DFDLTestSchema {

    private final int MAXINMEMORYSIZE = 200000;

    public static void main(String[] args) throws DFDLException {
        String dataLocation;
        String resultLocation = null;
        String packageName;
        String xslLocation = null;
        if (args.length >= 2) {
            packageName = args[0];
            dataLocation = args[1];
            if (args.length > 2) {
                resultLocation = args[2];
                if (args.length > 3) {
                    xslLocation = args[3];
                }
            }
            try {
                DFDLTestSchema test = new DFDLTestSchema();
                test.performTest(packageName, dataLocation, resultLocation, xslLocation);
            } catch (IOException e) {
                System.err.println("Error Ocurred while performing test " + e);
                DFDLException de = new DFDLException();
                de.setStackTrace(e.getStackTrace());
                throw de;
            }
        } else {
            System.err.println("USAGE: You must specify package dataLocation resultLocation xsltLocation(optional)");
        }
    }

    public void performTest(String packageName, String dataLocation, String resultLocation, String xslLocation) throws IOException, DFDLException {
        FileInputStream fis = null;
        FileChannel fic = null;
        try {
            try {
                URI uri = new URI(dataLocation);
                File f = new File(uri);
                fis = new FileInputStream(f);
            } catch (Exception e) {
                fis = new FileInputStream(dataLocation);
            }
            if (fis.available() > MAXINMEMORYSIZE) {
                fic = fis.getChannel();
                MappedByteBuffer bb = fic.map(FileChannel.MapMode.READ_ONLY, 0L, fis.available());
                GenericDFDLTest test = new GenericDFDLTest(bb, packageName, xslLocation, resultLocation);
                bb.clear();
                bb = null;
            } else {
                byte[] bb = new byte[fis.available()];
                fis.read(bb);
                GenericDFDLTest test = new GenericDFDLTest(bb, packageName, xslLocation, resultLocation);
            }
        } catch (Throwable e) {
            System.err.println("Exception caught while running defuddle translation.");
            e.printStackTrace();
            DFDLException de = new DFDLException();
            de.setStackTrace(e.getStackTrace());
            throw de;
        }
        try {
            if (fic != null) {
                fic.close();
            }
        } catch (IOException e) {
        }
        try {
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e) {
        }
    }

    public void performTest(String packageName, String dataLocation, OutputStream output, String xslLocation) throws IOException, JAXBException {
        FileInputStream fis = new FileInputStream(dataLocation);
        performTest(packageName, fis, output, xslLocation);
    }

    public void performTest(String packageName, InputStream input, OutputStream output, String xslLocation) throws IOException, JAXBException {
        FileInputStream fis = new FileInputStream(xslLocation);
        performTest(packageName, input, output, fis);
    }

    public void performTest(String packageName, InputStream input, OutputStream output, InputStream xslFile) throws IOException, JAXBException {
        byte[] b = new byte[input.available()];
        input.read(b);
        input.close();
        GenericDFDLTest test = new GenericDFDLTest(b, packageName, xslFile, output);
    }
}
