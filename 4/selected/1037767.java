package unibg.overencrypt.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;

/**
 * Allows to simplify download and upload process from / to webdav server.
 *
 * @author Flavio Giovarruscio & Riccardo Tribbia
 * @version 1.0
 */
public class SardineWebDAVClient implements WebDAVClient {

    Sardine client;

    public SardineWebDAVClient() {
        client = SardineFactory.begin();
    }

    public SardineWebDAVClient(String username, String password) {
        client = SardineFactory.begin(username, password);
    }

    @Override
    public void download(String remoteFilePath, String localFilePath) {
        InputStream remoteStream = null;
        try {
            remoteStream = client.get(remoteFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        OutputStream localStream = null;
        try {
            localStream = new FileOutputStream(new File(localFilePath));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        try {
            IOUtils.copy(remoteStream, localStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void upload(String localFilePath, String remoteFilePath) {
        InputStream localStream = null;
        try {
            localStream = new FileInputStream(new File(localFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            client.put(remoteFilePath, localStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void put(String content, String remoteFilePath) {
        try {
            client.put(remoteFilePath, content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream get(String remoteFilePath) {
        InputStream stream = null;
        System.out.println(remoteFilePath);
        try {
            stream = client.get(remoteFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream;
    }

    @Override
    public void delete(String remoteFilePath) {
        try {
            client.delete(remoteFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
