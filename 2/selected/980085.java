package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.util.logging.Level;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.IFileAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import neembuuuploader.utils.NeembuuUploaderProperties;
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
public class IFile extends AbstractUploader implements UploaderAccountNecessary {

    IFileAccount iFileAccount = (IFileAccount) AccountsManager.getAccount("IFile.it");

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String postURL;

    private String uploadresponse;

    private String file_ukey;

    private String downloadlink;

    public IFile(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "IFile.it";
        if (iFileAccount.loginsuccessful) {
            host = iFileAccount.username + " | IFile.it";
        }
    }

    @Override
    public void run() {
        try {
            if (iFileAccount.loginsuccessful) {
                host = iFileAccount.username + " | IFile.it";
            } else {
                host = "IFile.it";
                uploadFailed();
                return;
            }
            if (file.length() > 1090519040) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>1040MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            NULogger.getLogger().info("Getting upload url from ifile.......");
            u = new URL("http://ifile.it/api-fetch_upload_url.api");
            uc = (HttpURLConnection) u.openConnection();
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String temp = "", k = "";
            while ((temp = br.readLine()) != null) {
                NULogger.getLogger().info(temp);
                k += temp;
            }
            uc.disconnect();
            postURL = CommonUploaderTasks.parseResponse(k, "upload_url\":\"", "\"");
            postURL = postURL.replaceAll("\\\\", "");
            NULogger.getLogger().log(Level.INFO, "Post URL : {0}", postURL);
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(postURL);
            MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            mpEntity.addPart("akey", new StringBody(NeembuuUploaderProperties.getEncryptedProperty("ifile_api_key")));
            mpEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
            httppost.setEntity(mpEntity);
            NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
            NULogger.getLogger().info("Now uploading your file into ifile.it");
            status = UploadStatus.UPLOADING;
            HttpResponse response = httpclient.execute(httppost);
            status = UploadStatus.GETTINGLINK;
            NULogger.getLogger().info("Now getting downloading link.........");
            HttpEntity resEntity = response.getEntity();
            httpclient.getConnectionManager().shutdown();
            NULogger.getLogger().info(response.getStatusLine().toString());
            if (resEntity != null) {
                uploadresponse = EntityUtils.toString(resEntity);
                NULogger.getLogger().log(Level.INFO, "Upload response : {0}", uploadresponse);
            } else {
                throw new Exception("There might be a problem with your internet connection or server error. Please try after some time :(");
            }
            if (uploadresponse.contains("\"status\":\"ok\"")) {
                NULogger.getLogger().info("File uploaded successfully :)");
                file_ukey = CommonUploaderTasks.parseResponse(uploadresponse, "\"ukey\":\"", "\"");
                NULogger.getLogger().log(Level.INFO, "File ukey : {0}", file_ukey);
                downloadlink = "http://www.ifile.it/" + file_ukey + "/" + file.getName();
                downURL = downloadlink;
                NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
            } else {
                throw new Exception("There might be a problem with your internet connection or server error. Please try after some time :(");
            }
            NULogger.getLogger().log(Level.INFO, "Download Link: {0}", downURL);
            uploadFinished();
        } catch (Exception ex) {
            ex.printStackTrace();
            NULogger.getLogger().severe(ex.toString());
            uploadFailed();
        }
    }
}
