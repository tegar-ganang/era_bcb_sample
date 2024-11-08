package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.EnterUploadAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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
public class EnterUpload extends AbstractUploader implements UploaderAccountNecessary {

    EnterUploadAccount enterUploadAccount = (EnterUploadAccount) AccountsManager.getAccount("EnterUpload.com");

    private HttpURLConnection uc;

    private BufferedReader br;

    private String uploadid;

    private String servertmpurl;

    private String sessid;

    private String postURL;

    private String uploadresponse;

    private String downloadid;

    private URL u;

    private PrintWriter pw;

    private String afteruploadpage;

    private String downloadlink;

    private String deletelink;

    public EnterUpload(File file) {
        super(file);
        host = "EnterUpload.com";
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        if (enterUploadAccount.loginsuccessful) {
            host = enterUploadAccount.username + " | EnterUpload.com";
        }
    }

    private String getData(String myurl) throws Exception {
        URL url = new URL(myurl);
        uc = (HttpURLConnection) url.openConnection();
        if (enterUploadAccount.loginsuccessful) {
            uc.setRequestProperty("Cookie", enterUploadAccount.getLogincookie() + ";" + enterUploadAccount.getXfsscookie());
        }
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        br.close();
        return k;
    }

    public String parseResponse(String response, String stringStart, String stringEnd) throws Exception {
        response = response.substring(response.indexOf(stringStart));
        response = response.replace(stringStart, "");
        response = response.substring(0, response.indexOf(stringEnd));
        return response;
    }

    private void generateEnterUploadRandomID() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            uploadid = String.valueOf(Math.round(Math.random() * 10));
            sb.append(uploadid);
        }
        uploadid = sb.toString();
    }

    private void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("upload_type", new StringBody("file"));
        mpEntity.addPart("sess_id", new StringBody(sessid));
        mpEntity.addPart("srv_tmp_url", new StringBody(servertmpurl));
        mpEntity.addPart("file_0", new MonitoredFileBody(file, uploadProgress));
        mpEntity.addPart("submit_btn", new StringBody(" Upload!"));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        NULogger.getLogger().info("Now uploading your file into enterupload.com");
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        downloadid = parseResponse(uploadresponse, "<textarea name='fn'>", "<");
        httpclient.getConnectionManager().shutdown();
    }

    public void postData(String content, String posturl) throws Exception {
        u = new URL(posturl);
        setHttpHeader(enterUploadAccount.getLogincookie() + ";" + enterUploadAccount.getXfsscookie() + ";");
        writeHttpContent(content);
        u = null;
        uc = null;
    }

    public void setHttpHeader(String cookie) throws Exception {
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", "www.enterupload.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "http://www.enterupload.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        uc.setRequestProperty("Cookie", cookie);
        uc.setRequestMethod("POST");
        uc.setInstanceFollowRedirects(false);
    }

    public void writeHttpContent(String content) throws Exception {
        pw = new PrintWriter(new OutputStreamWriter(uc.getOutputStream()), true);
        pw.print(content);
        pw.flush();
        pw.close();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String tmp = "";
        NULogger.getLogger().info("Getting enter upload page.........");
        while ((tmp = br.readLine()) != null) {
            afteruploadpage += tmp;
        }
    }

    public void run() {
        try {
            if (enterUploadAccount.loginsuccessful) {
                host = enterUploadAccount.username + " | EnterUpload.com";
            } else {
                host = "EnterUpload.com";
                uploadFailed();
                return;
            }
            if (file.length() > 500 * 1024 * 1024) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>500MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            generateEnterUploadRandomID();
            NULogger.getLogger().log(Level.INFO, "upload id : {0}", uploadid);
            String tmp = getData("http://www.enterupload.com/");
            servertmpurl = parseResponse(tmp, "srv_tmp_url='", "'");
            sessid = parseResponse(tmp, "sess_id\" value=\"", "\"");
            NULogger.getLogger().info(servertmpurl);
            NULogger.getLogger().info(sessid);
            postURL = parseResponse(tmp, "action=\"", "\"");
            postURL = postURL + uploadid + "&js_on=1&utype=reg&upload_type=file";
            NULogger.getLogger().log(Level.INFO, "res : {0}", postURL);
            fileUpload();
            status = UploadStatus.GETTINGLINK;
            postData("fn=" + downloadid + "&st=OK&op=upload_result", "http://www.enterupload.com/");
            downloadlink = afteruploadpage.substring(afteruploadpage.indexOf("Download Link"));
            downloadlink = downloadlink.substring(0, downloadlink.indexOf("</textarea>"));
            downloadlink = downloadlink.substring(downloadlink.indexOf("http://"));
            deletelink = afteruploadpage.substring(afteruploadpage.indexOf("Delete Link"));
            deletelink = deletelink.substring(0, deletelink.indexOf("</textarea>"));
            deletelink = deletelink.substring(deletelink.indexOf("http://"));
            NULogger.getLogger().log(Level.INFO, "Download Link : {0}", downloadlink);
            NULogger.getLogger().log(Level.INFO, "Delete Link : {0}", deletelink);
            downURL = downloadlink;
            delURL = deletelink;
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(EnterUpload.class.getName()).log(Level.SEVERE, null, e);
            CommonUploaderTasks.uploadFailed(this);
        }
    }
}
