import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

/**
 * S3の特定のパス以下のファイルを、指定ディレクトリにダウンロードします。
 * 
 * @author c9katayama
 * 
 */
public class S3DownloadService {

    private static Log log = LogFactory.getLog(S3DownloadService.class);

    private String downloadFileOutputDir;

    private String bucketName;

    /**
	 * S3上にある、バックアップするファイルのプレフィックス
	 */
    private String prefix;

    private String accessId;

    private String secretKey;

    private S3Service s3;

    private AWSCredentials awsCredentials;

    private List<String> downloadedFileList;

    /**
	 * 出力先のディレクトリ
	 * 
	 * @param outputRootDir
	 */
    public void setDownloadFileOutputDir(String downloadFileOutputDir) {
        this.downloadFileOutputDir = downloadFileOutputDir;
    }

    /**
	 * バケット名
	 * 
	 * @param bucketName
	 */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
	 * バケット以下のパスで、ダウンロード対象にするディレクトリ
	 * 
	 * @param subBacketNameList
	 */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void execute() throws Exception {
        downloadedFileList = new ArrayList<String>();
        try {
            awsCredentials = new AWSCredentials(accessId, secretKey);
            s3 = new RestS3Service(awsCredentials);
            S3Bucket bucket = s3.getBucket(bucketName);
            S3Object[] objects = s3.listObjects(bucket, prefix, null);
            for (S3Object object : objects) {
                doDownload(bucket, object);
            }
        } catch (Exception e) {
            log.error("S3 download fail!", e);
            throw e;
        }
    }

    public List<String> getDownloadedFileList() {
        return downloadedFileList;
    }

    protected String trimPrefix(String key) {
        if (prefix == null) {
            return key;
        } else {
            return key.substring(prefix.length(), key.length());
        }
    }

    protected void doDownload(S3Bucket bucket, S3Object s3object) throws Exception {
        String key = s3object.getKey();
        key = trimPrefix(key);
        String[] path = key.split("/");
        String fileName = path[path.length - 1];
        String dirPath = "";
        for (int i = 0; i < path.length - 1; i++) {
            dirPath += path[i] + "/";
        }
        File outputDir = new File(downloadFileOutputDir + "/" + dirPath);
        if (outputDir.exists() == false) {
            outputDir.mkdirs();
        }
        File outputFile = new File(outputDir, fileName);
        long size = s3object.getContentLength();
        if (outputFile.exists() && outputFile.length() == size) {
            return;
        }
        long startTime = System.currentTimeMillis();
        log.info("Download start.S3 file=" + s3object.getKey() + " local file=" + outputFile.getAbsolutePath());
        FileOutputStream fout = null;
        S3Object dataObject = null;
        try {
            fout = new FileOutputStream(outputFile);
            dataObject = s3.getObject(bucket, s3object.getKey());
            InputStream is = dataObject.getDataInputStream();
            IOUtils.copyStream(is, fout);
            downloadedFileList.add(key);
            long downloadTime = System.currentTimeMillis() - startTime;
            log.info("Download complete.Estimete time=" + downloadTime + "ms " + IOUtils.toBPSText(downloadTime, size));
        } catch (Exception e) {
            log.error("Download fail. s3 file=" + key, e);
            outputFile.delete();
            throw e;
        } finally {
            IOUtils.closeNoException(fout);
            if (dataObject != null) {
                dataObject.closeDataInputStream();
            }
        }
    }
}
