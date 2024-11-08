package iLabsTalk.util;

import mobi.ilabs.common.Base64;
import mobi.ilabs.common.Debug;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/**
* Represents the client view of a FileStore Service. 
* It introduces basic remote file store capabilities
* which enables a user application to complete the following use cases: 
* <ul>
* <li>Upload any file and place it in a user defined bucket 
* <li>Create and manage user's account -- NOT IMPLEMENTED YET 
* <li>Manage visibility of files in the FileStore -- NOT IMPLEMENTED YET
* </ul>
* The File Store is organized as an ordinary file tree with the user's ident as root directory.
* All user's buckets are subdirectories within user's root directory.
* And finally; user's files are stored in such buckets.
* <p>
* Whenever the user stores a file in the FileStore, the filename is changed to something unique.
* The new unique filename is reported back to this client.
* <p>
* For more information, see the
* <a href="http://groups.google.com/group/ilabs-mobile-developers-toolbox/web/filestore-lsning-for-demon-vr">
* Utkast til midlertidig hjemmesnekra FileStore protokoll</a>
* <p>
* @author �ystein Myhre
*/
public class FileStore implements Debug.Constants {

    private static final boolean DEBUG = true;

    private static final boolean USE_HTTP_METHOD_OVERRIDE_HEADERS = true;

    private static final String METHOD_OVERRIDE_HEADER = "X-HTTP-Method-Override";

    private String host;

    private String authenticationToken = "TOKEN";

    private String userID;

    private String getSERVICE() {
        return ("http://" + host);
    }

    /**
  * Constructor. Creates a FileStore Client.
  * <p>
  * @param userID the user's ident
  * @param passWord the user's password
  */
    public FileStore(String host, String userID, String passWord) {
        this.host = host;
        this.userID = userID;
        String auth = userID + ':' + passWord;
        if (DEBUG) Debug.println("FileStore.<init>: host=" + host + ", auth=" + auth);
        authenticationToken = "Basic " + new String(Base64.encode(auth.getBytes()));
    }

    /**
  * Uploads a file to the remote FileStore.
  * This method will block until the uploading process is finished or an IOException is thrown. 
  * @param bucket the user's bucket to store file in
  * @param fileName the user's hint of file's name including file extension
  * @param content the content as stream
  * @param length the content length (number of bytes)
  * @return the url to the new file in the remote FileStore.
  * @throws IOException if an I/O error occures
  */
    public String upLoad(String bucket, String fileName, InputStream content, int length) throws IOException {
        String responseFileName = null;
        String resource = "/FileStore/" + bucket + '/' + fileName;
        String url = getSERVICE() + resource;
        HttpConnection httpConnection = null;
        OutputStream outputStream = null;
        if (DEBUG) Debug.printhd("FileStore.upLoad: " + url);
        try {
            httpConnection = (HttpConnection) Connector.open(url);
            if (USE_HTTP_METHOD_OVERRIDE_HEADERS) {
                setRequestMethod(httpConnection, "POST", resource);
                setRequestProperty(httpConnection, METHOD_OVERRIDE_HEADER, "PUT");
            } else setRequestMethod(httpConnection, "PUT", resource);
            if (authenticationToken != null) setRequestProperty(httpConnection, "Authorization", authenticationToken);
            setRequestProperty(httpConnection, "Content-type", "application/octet-stream");
            setRequestProperty(httpConnection, "Content-length", "" + length);
            if (DEBUG) Debug.println("FileStore.upLoad: content=" + content);
            outputStream = httpConnection.openOutputStream();
            copy(content, outputStream, length);
            outputStream.flush();
            int responseCode = httpConnection.getResponseCode();
            if (DEBUG) Debug.println("FileStore.upLoad: responseCode=" + responseCode);
            if ((responseCode / 10) != 20) throw new IOException("Illegal response code: " + responseCode);
            responseFileName = httpConnection.getHeaderField("X-FileName");
            if (responseFileName == null) throw new IOException("Missing response X-FileName");
            if (DEBUG) Debug.println("FileStore.upLoad: responseFileName=" + responseFileName);
        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {
            }
            try {
                if (httpConnection != null) httpConnection.close();
            } catch (Exception ignored) {
            }
        }
        String result = getSERVICE() + "/FileStore/" + userID + '/' + bucket + '/' + responseFileName;
        if (DEBUG) Debug.log("FileStore.upLoad", "FILE(" + fileName + ") UPLOADED TO: " + result);
        return (result);
    }

    public static void setRequestMethod(HttpConnection httpConnection, String name, String resource) throws IOException {
        if (DEBUG) Debug.println("HTTP-REQUEST:   " + name + " " + resource + " HTTP/1.1");
        httpConnection.setRequestMethod(name);
    }

    public static void setRequestProperty(HttpConnection httpConnection, String name, String val) throws IOException {
        if (DEBUG) Debug.println("REQUEST-HEADER: " + name + ": " + val);
        httpConnection.setRequestProperty(name, val);
    }

    private void copy(InputStream in, OutputStream out, int size) throws IOException {
        for (int i = 0; i < size; i++) out.write(in.read());
    }
}
