package dsrwebserver;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import ssmith.io.IOFunctions;
import ssmith.lang.Dates;
import ssmith.util.KeyValuePair;

public final class HTTPHeaders extends HashMap<String, StringBuffer> {

    private static final long serialVersionUID = 1L;

    public static int HTTP_OK = 200;

    public static int HTTP_ACCESS_DENIED = 403;

    public static int HTTP_NOT_FOUND = 404;

    public URLRequest request;

    public HashMap<String, StringBuffer> postdata;

    public HashMap<String, String> cookies = new HashMap<String, String>();

    private boolean keep_alive = false;

    public String line1;

    public HashMap<String, UploadFileData> uploaded_files;

    public HTTPHeaders(DataInputStream br) throws IOException {
        super();
        line1 = br.readLine();
        System.out.println(Dates.FormatDate(Calendar.getInstance().getTime(), Dates.UKDATE_FORMAT_WITH_TIME) + " " + line1);
        request = new URLRequest(line1);
        String line = br.readLine();
        String csv[];
        while (line != null) {
            csv = line.split(":");
            StringBuffer str = new StringBuffer();
            for (int i = 1; i < csv.length; i++) {
                str.append(csv[i].trim());
                if (i < csv.length - 1) {
                    str.append(":");
                }
            }
            KeyValuePair kvp = new KeyValuePair(csv[0].trim(), str);
            super.put(kvp.key.toLowerCase().trim(), kvp.value);
            if (kvp.key.equalsIgnoreCase("cookie")) {
                String ckcsv[] = kvp.value.toString().split(";");
                for (int i = 0; i < ckcsv.length; i++) {
                    String ck2[] = ckcsv[i].trim().split("=");
                    if (ck2.length > 1) {
                        String key = ck2[0];
                        String value = ck2[1];
                        cookies.put(key, value);
                    }
                }
            } else if (kvp.key.equalsIgnoreCase("Connection")) {
                this.keep_alive = kvp.value.toString().equalsIgnoreCase("keep-alive");
            }
            line = br.readLine();
            if (line == null) {
                break;
            }
            if (line.length() <= 2) {
                break;
            }
        }
        if (this.containsKey("content-length")) {
            if (this.getContentLength() > 0) {
                if (this.getContentType().equalsIgnoreCase("application/x-www-form-urlencoded")) {
                    StringBuffer data = new StringBuffer();
                    int tot_read = 0;
                    byte tmp[] = new byte[4096];
                    char c[] = new char[this.getContentLength()];
                    while (tot_read < this.getContentLength()) {
                        int read = br.read(tmp, 0, Math.min(tmp.length, c.length - tot_read));
                        tot_read += read;
                        data.append(new String(tmp, 0, read));
                    }
                    postdata = URLRequest.DecodeParams(data);
                } else if (this.getContentType().indexOf("multipart/form-data") >= 0) {
                    uploaded_files = new HashMap<String, UploadFileData>();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int tot_read = 0;
                    byte tmp[] = new byte[4096];
                    while (tot_read < this.getContentLength()) {
                        int read = br.read(tmp);
                        if (read <= 0) {
                            break;
                        }
                        tot_read += read;
                        out.write(tmp, 0, read);
                    }
                    String boundary = null;
                    UploadFileData filedata = new UploadFileData();
                    byte data[] = out.toByteArray();
                    int pos = 0;
                    int prev_pos = 0;
                    boolean reading_text = true;
                    ByteArrayOutputStream file = new ByteArrayOutputStream();
                    while (pos < data.length) {
                        if (reading_text) {
                            if (data[pos] == 13) {
                                String t = new String(data, prev_pos, pos - prev_pos);
                                pos += 2;
                                prev_pos = pos;
                                if (boundary == null) {
                                    boundary = t;
                                } else {
                                    if (filedata.getDisposition() == null) {
                                        filedata.setDisposition(t);
                                        if (filedata.hasFilename() == false) {
                                            pos += 1;
                                            reading_text = false;
                                        }
                                    } else {
                                        filedata.setType(t);
                                        pos += 1;
                                        reading_text = false;
                                    }
                                }
                            }
                        } else {
                            String test = new String(data, pos + 2, boundary.length());
                            if (test.equals(boundary)) {
                                if (filedata.hasFilename()) {
                                    if (file.size() > 0) {
                                        String unique_filename = IOFunctions.GetUniqueFilenameAndPath("./webroot/temp_uploaded_files", filedata.getFileExtention());
                                        filedata.setNewFilename(unique_filename);
                                        FileOutputStream fos = new FileOutputStream(unique_filename);
                                        fos.write(file.toByteArray());
                                        fos.close();
                                        this.uploaded_files.put(filedata.getName(), filedata);
                                    } else {
                                    }
                                } else {
                                    if (postdata == null) {
                                        postdata = new HashMap<String, StringBuffer>();
                                    }
                                    this.postdata.put(filedata.getName(), new StringBuffer(new String(file.toByteArray())));
                                }
                                pos += boundary.length() + 2;
                                file = new ByteArrayOutputStream();
                                filedata = new UploadFileData();
                                reading_text = true;
                                pos += 2;
                                prev_pos = pos;
                            } else {
                                file.write(data[pos]);
                            }
                        }
                        pos++;
                    }
                }
            }
        }
    }

    public String getReferer() {
        if (this.containsKey("referer")) {
            return this.get("referer").toString();
        } else {
            return "";
        }
    }

    public String getHost() {
        if (this.containsKey("host")) {
            return (String) this.get("host").toString();
        } else {
            return "";
        }
    }

    public void blank(String method, String URL) throws IOException {
        this.request = new URLRequest(method + " " + URL + " HTTP/1.1");
        if (postdata != null) {
            this.postdata.clear();
        }
    }

    public boolean isKeepAlive() {
        return this.keep_alive;
    }

    private String getContentType() {
        if (this.containsKey("content-type")) {
            return this.get("content-type").toString();
        } else {
            return "";
        }
    }

    private int getContentLength() {
        try {
            return new Integer(this.get("content-length").toString()).intValue();
        } catch (java.lang.NumberFormatException ex) {
            return 0;
        }
    }

    public String getPostValueAsString(String key) {
        if (postdata != null) {
            if (this.postdata.containsKey(key.toLowerCase())) {
                return this.postdata.get(key.toLowerCase()).toString();
            }
        }
        return "";
    }

    public UploadFileData getUploadedFile(String key) {
        if (this.uploaded_files != null) {
            if (this.uploaded_files.containsKey(key.toLowerCase())) {
                return this.uploaded_files.get(key.toLowerCase());
            }
        }
        return null;
    }

    public String getGetOrPostValueAsString(String key) {
        if (this.getPostValueAsString(key).length() > 0) {
            return this.getPostValueAsString(key);
        } else {
            return this.getGetValueAsString(key);
        }
    }

    public int getGetOrPostValueAsInt(String key) {
        if (this.getPostValueAsString(key).length() > 0) {
            return this.getPostValueAsInt(key);
        } else if (this.getGetValueAsString(key).length() > 0) {
            return this.getGetValueAsInt(key);
        } else {
            return 0;
        }
    }

    public int getPostValueAsInt(String key) {
        if (postdata != null) {
            if (this.postdata.containsKey(key.toLowerCase())) {
                try {
                    return Integer.parseInt(this.postdata.get(key.toLowerCase()).toString());
                } catch (java.lang.NumberFormatException ex) {
                    return 0;
                }
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public float getPostValueAsFloat(String key) {
        if (postdata != null) {
            if (this.postdata.containsKey(key.toLowerCase())) {
                try {
                    return Float.parseFloat(this.postdata.get(key.toLowerCase()).toString());
                } catch (java.lang.NumberFormatException ex) {
                    return 0f;
                }
            } else {
                return 0f;
            }
        } else {
            return 0f;
        }
    }

    public String getGetValueAsString(String key) {
        if (this.request.containsKey(key.toLowerCase())) {
            return (String) this.request.getGetValue(key.toLowerCase());
        } else {
            return "";
        }
    }

    public int getGetValueAsInt(String key) {
        if (this.request.containsKey(key.toLowerCase())) {
            try {
                return new Integer(this.request.getGetValue(key.toLowerCase())).intValue();
            } catch (java.lang.NumberFormatException ex) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public boolean doesPostKeyExists(String key) {
        if (this.postdata != null) {
            return this.postdata.containsKey(key.toLowerCase());
        } else {
            return false;
        }
    }

    public boolean doesGetKeyExist(String key) {
        return this.request.containsKey(key.toLowerCase());
    }

    public static String URLDecodeString(String s) throws UnsupportedEncodingException {
        try {
            return java.net.URLDecoder.decode(s, "ISO-8859-1");
        } catch (IllegalArgumentException ex) {
            return s;
        }
    }

    public String getCookie(String key) {
        return this.cookies.get(key);
    }

    public boolean doesCookieExist(String key) {
        String s = this.cookies.get(key);
        if (s == null) {
            return false;
        } else if (s.length() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public String getXForwardedFor() {
        if (this.containsKey("x-forwarded-for")) {
            return this.get("x-forwarded-for").toString();
        } else {
            return "";
        }
    }

    public String getUserAgent() {
        if (this.containsKey("user-agent")) {
            return this.get("user-agent").toString();
        } else {
            return "";
        }
    }

    public boolean isMobile() {
        try {
            String ua = this.getUserAgent().toLowerCase();
            if (ua.length() >= 4) {
                return ua.matches(".*(android.+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|symbian|treo|up\\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino).*") || ua.substring(0, 4).matches("1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|e\\-|e\\/|\\-[a-w])|libw|lynx|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(di|rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|xda(\\-|2|g)|yas\\-|your|zeto|zte\\-");
            } else {
                return true;
            }
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex);
            try {
                DSRWebServer.SendEmailToAdmin("Invalid user agent", "User agent: " + this.getUserAgent());
            } catch (Exception ex2) {
            }
            return false;
        }
    }

    public String getQueryString() {
        return request.getQueryString();
    }

    public static String URLEncodeString(String s) {
        try {
            return java.net.URLEncoder.encode(s, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return s;
        }
    }
}
