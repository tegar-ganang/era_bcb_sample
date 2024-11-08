package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class MultiUploadUploaderPlugin {

    private static URL u;

    private static HttpURLConnection uc;

    private static String tmp;

    private static String ucookie;

    private static BufferedReader br;

    private static String postURL;

    private static String uid;

    private static String uploadresponse;

    private static String downloadlink;

    private static boolean login;

    public static void main(String[] args) throws Exception {
        initialize();
        fileUpload();
    }

    private static void initialize() throws Exception {
        System.out.println("Getting startup cookie from multiupload.com");
        u = new URL("http://www.multiupload.com/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("u=")) {
                    ucookie = tmp;
                }
            }
        }
        ucookie = ucookie.substring(0, ucookie.indexOf(";"));
        System.out.println("ucookie : " + ucookie);
        System.out.println("Getting multiupload.com dynamic upload link");
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        postURL = parseResponse(k, "action=\"", "\"");
        postURL = postURL.replace("progress/?id=", "upload/?UPLOAD_IDENTIFIER=");
        uid = postURL.substring(postURL.indexOf("=") + 1);
        System.out.println("Post URL  : " + postURL);
        System.out.println("UID : " + uid);
    }

    public static String parseResponse(String response, String stringStart, String stringEnd) {
        response = response.substring(response.indexOf(stringStart));
        response = response.replace(stringStart, "");
        response = response.substring(0, response.indexOf(stringEnd));
        return response;
    }

    private static void fileUpload() throws IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        System.out.println(postURL);
        System.out.println(ucookie);
        System.out.println(uid);
        HttpPost httppost = new HttpPost(postURL);
        httppost.setHeader("Cookie", ucookie);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        File f = new File("h:/Rock Lee.jpg");
        reqEntity.addPart("UPLOAD_IDENTIFIER", new StringBody(uid));
        reqEntity.addPart("u", new StringBody(ucookie));
        FileBody bin = new FileBody(f);
        reqEntity.addPart("file_0", bin);
        reqEntity.addPart("service_1", new StringBody("1"));
        reqEntity.addPart("service_16", new StringBody("1"));
        reqEntity.addPart("service_7", new StringBody("1"));
        reqEntity.addPart("service_17", new StringBody("1"));
        reqEntity.addPart("service_9", new StringBody("1"));
        reqEntity.addPart("service_10", new StringBody("1"));
        reqEntity.addPart("remember_1", new StringBody("1"));
        reqEntity.addPart("remember_16", new StringBody("1"));
        reqEntity.addPart("remember_7", new StringBody("1"));
        reqEntity.addPart("remember_17", new StringBody("1"));
        reqEntity.addPart("remember_9", new StringBody("1"));
        reqEntity.addPart("remember_10", new StringBody("1"));
        httppost.setEntity(reqEntity);
        System.out.println("Now uploading your file into multiupload.com. Please wait......................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
            System.out.println("Response :\n" + uploadresponse);
            uploadresponse = parseResponse(uploadresponse, "\"downloadid\":\"", "\"");
            downloadlink = "http://www.multiupload.com/" + uploadresponse;
            System.out.println("Download link : " + downloadlink);
        }
    }
}
