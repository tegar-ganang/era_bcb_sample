package immf;

import java.security.MessageDigest;
import org.apache.commons.lang.StringUtils;

public class SenderAttachment {

    private String contentType;

    private boolean inline;

    private String filename;

    private byte[] data;

    private String hash;

    private String contentId;

    private String docomoFileId;

    public String getDocomoFileId() {
        return docomoFileId;
    }

    public void setDocomoFileId(String docomoFileId) {
        this.docomoFileId = docomoFileId;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentTypeWithoutParameter() {
        String r = contentType.split("\\r?\\n")[0];
        return r.replaceAll("\\s*;\\s*", "");
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        try {
            byte[] digest;
            MessageDigest md = MessageDigest.getInstance("MD5");
            digest = md.digest(data);
            this.hash = "";
            for (int i = 0; i < digest.length; i++) {
                this.hash += Integer.toHexString((int) digest[i] & 0xff);
            }
        } catch (Exception e) {
            this.hash = null;
        }
    }

    public String getHash() {
        return hash;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentIdWithoutBracket() {
        if (StringUtils.isBlank(contentId)) {
            return null;
        }
        return contentId.replaceAll("^\\s*<", "").replaceAll(">\\s*", "");
    }

    public String loggingString() {
        return "ContentType:" + contentType + ", IsInline:" + inline + ", FileName:" + filename + ", Size:" + data.length + ", ContentId:" + contentId;
    }
}
