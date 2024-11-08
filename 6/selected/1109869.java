package net.sourceforge.ondex.ws_tester.parser;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPMessageCollector;
import com.enterprisedt.net.ftp.FTPTransferType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sourceforge.ondex.webservice.client.WebserviceException_Exception;
import net.sourceforge.ondex.webservice.client.parser.ArrayOfString;
import net.sourceforge.ondex.ws_tester.inputs.ParserArrayOfString;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

/**
 * Program for creating plugin wrappers.
 *
 * Designed only to run when Christian Server is running.
 * 
 * @author BrennincC based on stuff by taubertj
 */
public class AracycTester extends WS_Tester_Parser {

    public AracycTester() throws MalformedURLException {
        super("Aracyc");
    }

    public void play() throws FileNotFoundException, IOException, NoSuchAlgorithmException, FTPException {
        final int BUFFER = 2048;
        String host = "ftp.genome.jp";
        String username = "anonymous";
        String password = "";
        FTPClient ftp = null;
        ftp = new FTPClient();
        ftp.setRemoteHost(host);
        FTPMessageCollector listener = new FTPMessageCollector();
        ftp.setMessageListener(listener);
        System.out.println("Connecting");
        ftp.connect();
        System.out.println("Logging in");
        ftp.login(username, password);
        System.out.println("Setting up passive, ASCII transfers");
        ftp.setConnectMode(FTPConnectMode.PASV);
        ftp.setType(FTPTransferType.ASCII);
        System.out.println("Directory before put:");
        String[] files = ftp.dir(".", true);
        for (int i = 0; i < files.length; i++) System.out.println(files[i]);
        System.out.println("Quitting client");
        ftp.quit();
        String messages = listener.getLog();
        System.out.println("Listener log:");
        System.out.println(messages);
        System.out.println("Test complete");
    }

    public void play2() throws FileNotFoundException, IOException {
        final int BUFFER = 2048;
        String fileName = INPUT_DATA + "aracyc.zip";
        System.out.println("reading " + fileName);
        FileInputStream fis = new FileInputStream(fileName);
        System.out.println(fis.available());
        BufferedOutputStream dest = null;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            System.out.println("Extracting: " + entry);
            int count;
            byte data[] = new byte[BUFFER];
            String tempName = RESULT_DATA + entry.getName();
            if (tempName.endsWith("/")) {
                File output = new File(tempName);
                System.out.println("new directory " + output.getAbsoluteFile());
                output.mkdirs();
            } else {
                System.out.println("ouput file is " + tempName);
                FileOutputStream fos = new FileOutputStream(tempName);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        }
        zis.close();
    }

    public void testShort() throws MalformedURLException, net.sourceforge.ondex.webservice.client.parser.WebserviceException_Exception, WebserviceException_Exception, IOException {
        testName = "Festa with short.fasta";
        long graphId = api_service.createMemoryGraph("Test");
        String InputFileString = INPUT_DATA + "short.fasta";
        byte[] InputFileByteArray = null;
        java.lang.String FastaFileType = "simple";
        java.lang.String TaxId = "40559";
        java.lang.String CC = "Protein";
        java.lang.String CV = null;
        ArrayOfString POS_TO_ACCESSION = null;
        java.lang.String SeqType = "AA";
        java.lang.String Separator = null;
        java.lang.String AccessionRegEx = null;
        String parseResult = parser_service.fastaParser(InputFileString, InputFileByteArray, FastaFileType, TaxId, CC, CV, POS_TO_ACCESSION, SeqType, Separator, AccessionRegEx, graphId);
        System.out.println(parseResult);
        graphInfo(graphId, false);
        writeOXL(graphId, "fasta.oxl");
        api_service.deleteGraph(graphId);
        System.out.println("Done Fasta Test");
    }

    public static void main(String[] args) throws MalformedURLException, WebserviceException_Exception, net.sourceforge.ondex.webservice.client.parser.WebserviceException_Exception, IOException, FileNotFoundException, NoSuchAlgorithmException, FTPException {
        AracycTester testMain = new AracycTester();
        testMain.play();
    }
}
