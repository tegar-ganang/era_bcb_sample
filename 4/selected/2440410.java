package be.djdb.utils;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.*;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import be.djdb.UnConstruct;

/**
* @author Lieven Roegiers
* @copyright 2007
* @from http://code.google.com/p/javamylibs/
*/
public class AResourceFile {

    private static final long serialVersionUID = 1L;

    private String fileName;

    private String pathName;

    private URL fileUrl;

    private InputStream is;

    /**
	 * @param fname
	 * @return the file in a stream
	 * @throws IOException
	 */
    public InputStream takeajarfilestream(String fname) throws IOException {
        return AResourceFile.class.getResourceAsStream(fname);
    }

    public InputStream takealocaltestfilestream(String fname) throws IOException {
        return new FileInputStream("" + fname);
    }

    /**
	 * 
	 * @param src
	 * @param fileName
	 */
    protected void getSourcestream(StreamSource src, String fileName) {
        src = new StreamSource(this.getClass().getClassLoader().getResourceAsStream(fileName));
    }

    public InputStream getInputstream(String fileName) throws IOException {
        URL fileUrl = this.getClass().getClassLoader().getResource(fileName);
        log(Level.WARNING, "GetInputstream of string =>Filename" + fileName);
        return fileUrl.openConnection().getInputStream();
    }

    public InputStream getInputstream(URL location) throws IOException {
        return location.openConnection().getInputStream();
    }

    public InputStreamReader getInputStreamReader() throws IOException {
        this.is = fileUrl.openConnection().getInputStream();
        DataInputStream dis = new DataInputStream(this.is);
        return new InputStreamReader(dis);
    }

    public boolean isFile() {
        return (fileUrl != null);
    }

    @UnConstruct
    public void readftpInput() throws IOException {
    }

    @UnConstruct
    public void saveftpOutput() throws IOException {
    }

    public void printInputStream() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(this.is));
        String line;
        while (null != (line = br.readLine())) {
            System.out.println(line);
        }
    }

    public void printInputStream(InputStream in) throws IOException {
        BufferedInputStream bin = new BufferedInputStream(in);
        int b;
        while ((b = bin.read()) != -1) {
            char c = (char) b;
            System.out.print("" + (char) b);
        }
    }

    public void printInputStreamToout(InputStream teststream, OutputStream out) throws IOException {
        byte buf[] = new byte[1024];
        int len;
        while ((len = teststream.read(buf)) > 0) out.write(buf, 0, len);
        out.close();
    }

    public void printInputStreamAschar(InputStream iss) throws IOException {
        int ch;
        while (((ch) = iss.read()) != -1) {
            String aChar = new Character((char) ch).toString();
            System.out.println(aChar);
        }
    }

    public void printinputstreamAsLine(InputStream iss) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(iss));
        String line = br.readLine();
        while (line != null) {
            System.out.println(line);
            line = br.readLine();
        }
    }

    public void printInputStreamAsChar(InputStream input) throws IOException {
        int data = input.read();
        int charint;
        int i = 0;
        while ((charint = input.read()) != -1) {
            i++;
            data = input.read();
            System.out.print((char) charint);
        }
        if ((charint = input.read()) != -1) {
        } else {
            System.out.println("iets mis met de stream");
        }
    }

    public void printOutputStream(OutputStream out) throws IOException {
        System.out.println(out.toString());
    }

    private static void log(Level level, String msg) {
        String tag = "<>>>>*JAVA_LIBS-�L Roegiers 80072631156*<<<<>Take Resourcefile<>";
        Logger.getLogger(AResourceFile.class.getName()).log(level, tag + msg);
    }
}
