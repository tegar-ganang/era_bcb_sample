package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class MultiUpload extends AbstractUploader {

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String postURL;

    private String downloadlink;

    private String tmp;

    private String ucookie;

    private String uid;

    private String uploadresponse;

    public MultiUpload(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "MultiUpload.com";
    }

    private void initialize() throws Exception {
        status = UploadStatus.INITIALISING;
        NULogger.getLogger().info("Getting startup cookie from multiupload.com");
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
        NULogger.getLogger().log(Level.INFO, "ucookie : {0}", ucookie);
        NULogger.getLogger().info("Getting multiupload.com dynamic upload link");
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        postURL = CommonUploaderTasks.parseResponse(k, "action=\"", "\"");
        postURL = postURL.replace("progress/?id=", "upload/?UPLOAD_IDENTIFIER=");
        uid = postURL.substring(postURL.indexOf("=") + 1);
        NULogger.getLogger().log(Level.INFO, "Post URL  : {0}", postURL);
        NULogger.getLogger().log(Level.INFO, "UID : {0}", uid);
    }

    private void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        httppost.setHeader("Cookie", ucookie);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart("UPLOAD_IDENTIFIER", new StringBody(uid));
        reqEntity.addPart("u", new StringBody(ucookie));
        reqEntity.addPart("file_0", new MonitoredFileBody(file, uploadProgress));
        reqEntity.addPart("service_1", new StringBody("1"));
        reqEntity.addPart("service_16", new StringBody("1"));
        reqEntity.addPart("service_7", new StringBody("1"));
        reqEntity.addPart("service_17", new StringBody("1"));
        reqEntity.addPart("service_9", new StringBody("1"));
        reqEntity.addPart("service_6", new StringBody("1"));
        reqEntity.addPart("service_15", new StringBody("1"));
        reqEntity.addPart("service_14", new StringBody("1"));
        reqEntity.addPart("service_18", new StringBody("1"));
        reqEntity.addPart("remember_1", new StringBody("1"));
        reqEntity.addPart("remember_16", new StringBody("1"));
        reqEntity.addPart("remember_7", new StringBody("1"));
        reqEntity.addPart("remember_17", new StringBody("1"));
        reqEntity.addPart("remember_9", new StringBody("1"));
        reqEntity.addPart("remember_6", new StringBody("1"));
        reqEntity.addPart("remember_15", new StringBody("1"));
        reqEntity.addPart("remember_14", new StringBody("1"));
        reqEntity.addPart("remember_18", new StringBody("1"));
        httppost.setEntity(reqEntity);
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into multiupload.com. Please wait......................");
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
            uploadresponse = CommonUploaderTasks.parseResponse(uploadresponse, "\"downloadid\":\"", "\"");
            downloadlink = "http://www.multiupload.com/" + uploadresponse;
            NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
            downURL = downloadlink;
        } else {
            throw new Exception("MultiUpload server problem or Internet connectivity problem");
        }
    }

    public void run() {
        if (file.length() > 400 * 1024 * 1024) {
            JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>400MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            uploadFailed();
            return;
        }
        try {
            initialize();
            fileUpload();
            uploadFinished();
        } catch (Exception e) {
            NULogger.getLogger().severe(e.toString());
            uploadFailed();
        }
    }
}
