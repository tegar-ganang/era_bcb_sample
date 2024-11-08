package flickr.core;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

public class Flickr {

    /**
     * Search for a resource in Flickr.com
     * 
     * @param tags tags for the search.
     * @param path path for saving the result XML file
     * @param resultPerPage no of results per page.
     */
    public static boolean search(String tags, String path, String resultPerPage) {
        String serverPath = "http://www.flickr.com:80/services/rest";
        String method = "/?method=flickr.photos.search";
        String apiKey = "d2f7679944690df360a03f19ea381613";
        String url = serverPath + method + "&api_key=" + apiKey + "&tags=" + tags + "&per_page=" + resultPerPage;
        File downloadPath = new File(path + File.separator + "flickrResults.xml");
        return downloadXML(url, downloadPath);
    }

    public static boolean downloadXML(String url, File downloadPath) {
        try {
            URL locationURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) locationURL.openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                readFileFromStream(connection.getInputStream(), downloadPath, true);
            }
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static void readFileFromStream(InputStream inStream, File outputFile, boolean closeStream) throws IOException {
        ReadableByteChannel srcChannel = Channels.newChannel(inStream);
        FileChannel dstChannel = new FileOutputStream(outputFile).getChannel();
        ByteBuffer byteBuff = ByteBuffer.allocateDirect(2048);
        int s = 0;
        while ((s = srcChannel.read(byteBuff)) != -1) {
            byteBuff.flip();
            dstChannel.write(byteBuff);
            if (byteBuff.hasRemaining()) byteBuff.compact(); else byteBuff.clear();
        }
        dstChannel.force(true);
        dstChannel.close();
        if (closeStream) {
            srcChannel.close();
        }
    }
}
