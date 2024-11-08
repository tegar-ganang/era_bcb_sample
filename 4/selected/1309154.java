package ports;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

public class FileUploader {

    private static final int ERRORCODE = -1;

    private static final int OKCODE = 0;

    private static final String[] CLDC_MIDPPROPS = { "microedition.profiles", "microedition.configuration", "microedition.locale", "microedition.platform", "microedition.encoding", "microedition.commports", "microedition.hostname", "microedition.jtwi.version" };

    private static final String[] MMAPI_SYSPROPS = { "supports.mixing", "supports.audio.capture", "supports.video.capture", "supports.recording", "audio.encodings", "video.encodings", "video.snapshot.encodings", "streamable.contents" };

    private static final String[] WMA_SYSPROPS = { "wireless.messaging.sms.smsc" };

    private static final String[] OPTIONAL_PACKAGES = { "fileconn.dir.landmarks", "fileconn.dir.landmarks.name", "supports.mediacapabilities", "tuner.modulations", "camera.orientations", "camera.resolutions", "wireless.messaging.sms.smsc", "wireless.messaging.mms.mmsc", "fileconn.dir.photos", "fileconn.dir.videos", "fileconn.dir.tones", "fileconn.dir.memorycard", "fileconn.dir.private", "fileconn.dir.photos.name", "fileconn.dir.videos.name", "fileconn.dir.tones.name", "fileconn.dir.memorycard.name", "bluetooth.l2cap.receiveMTU.max", "bluetooth.connected.devices.max", "bluetooth.connected.inquiry", "bluetooth.connected.page", "bluetooth.connected.inquiry.scan", "bluetooth.connected.page.scan", "bluetooth.master.switch", "bluetooth.sd.trans.max", "bluetooth.sd.attr.retrievable.max" };

    /**
	 * Construct the System Property List.
	 */
    public String getSystemPropertyString() {
        String systemInfo = "GloggerV7.1\n";
        systemInfo += "\nCLDC:\n";
        for (int i = 0; i < CLDC_MIDPPROPS.length; i++) {
            systemInfo += CLDC_MIDPPROPS[i] + "=" + System.getProperty(CLDC_MIDPPROPS[i]) + ";\n";
        }
        systemInfo += "\nMMAPI:\n";
        for (int i = 0; i < MMAPI_SYSPROPS.length; i++) {
            systemInfo += MMAPI_SYSPROPS[i] + "=" + System.getProperty(MMAPI_SYSPROPS[i]) + ";\n";
            ;
        }
        systemInfo += "\nOptional:\n";
        for (int i = 0; i < OPTIONAL_PACKAGES.length; i++) {
            systemInfo += OPTIONAL_PACKAGES[i] + "=" + System.getProperty(OPTIONAL_PACKAGES[i]) + ";\n";
            ;
        }
        return systemInfo;
    }

    public FileUploader() {
    }

    public String UploadURL(String sName, String uName) {
        return "http://" + sName + "/upload/upload.php";
    }

    public String StreamURL(String sName, String uName) {
        return "http://www.glogger.mobi/flv_ray/in/upload.php";
    }

    public String postCommentURL(String sName) {
        return "http://" + sName + "/comment/postComment_cellphone.php";
    }

    public String voteURL(String sName) {
        return "http://" + sName + "/SQL/vote.php";
    }

    /**
	 * Post a comment to the glogger website.
	 * 
	 * @param owner
	 * @param prefix
	 * @param comment
	 * @param serverName
	 * @param username
	 * @param password
	 * @return
	 */
    public String postComment(String owner, String prefix, String comment, String serverName, String username, String password) {
        try {
            HttpConnection c = null;
            DataInputStream is = null;
            DataOutputStream dstream = null;
            StringBuffer b = new StringBuffer();
            String sysProp = getSystemPropertyString();
            try {
                c = (HttpConnection) Connector.open(postCommentURL(serverName));
                c.setRequestMethod(HttpConnection.POST);
                c.setRequestProperty("Connection", "Keep-Alive");
                c.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Confirguration/CLDC-1.1");
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=****4353");
                dstream = new DataOutputStream(c.openOutputStream());
                dstream.write("--****4353\r\n".getBytes());
                dstream.write(("Content-Disposition: form-data; name=username" + "\r\n\r\n" + username.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=password" + "\r\n\r\n" + password.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=comment" + "\r\n\r\n" + comment.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=systemProperty" + "\r\n\r\n" + sysProp.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=owner" + "\r\n\r\n" + owner.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=prefix" + "\r\n\r\n" + prefix.trim() + "\r\n" + "--****4353--\r\n\r\n").getBytes());
                is = c.openDataInputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                return b.toString().trim();
            } catch (Exception e) {
                throw new IllegalArgumentException("Not an HTTP URL");
            } finally {
                if (is != null) is.close();
                if (dstream != null) dstream.close();
                if (c != null) c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "dead";
    }

    public int uploadImage(byte[] data, String serverName, String userName, String password, String commentString) {
        int returnCode = FileUploader.OKCODE;
        try {
            HttpConnection c = null;
            DataInputStream is = null;
            DataOutputStream dstream = null;
            StringBuffer b = new StringBuffer();
            String sysProp = getSystemPropertyString();
            try {
                c = (HttpConnection) Connector.open(UploadURL(serverName, userName));
                c.setRequestMethod(HttpConnection.POST);
                c.setRequestProperty("Connection", "Keep-Alive");
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=****4353");
                dstream = new DataOutputStream(c.openOutputStream());
                dstream.write("--****4353\r\n".getBytes());
                dstream.write(("Content-Disposition: form-data; name=\"username\" " + "\r\n\r\n" + userName + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=\"password\" " + "\r\n\r\n" + password + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=\"systemProperty\" " + "\r\n\r\n" + sysProp + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=\"comment\" " + "\r\n\r\n" + commentString + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=\"photo\"; " + "filename=\"" + "whatever.jpg\"" + "\r\nContent-Type: image/jpeg\r\n\r\n").getBytes());
                dstream.write(data, 0, data.length);
                dstream.write("\r\n--****4353--\r\n\r\n".getBytes());
                is = c.openDataInputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                if (b.toString().indexOf('1') != -1) {
                    returnCode = FileUploader.ERRORCODE;
                } else if (b.toString().indexOf('0') != -1) {
                    returnCode = FileUploader.OKCODE;
                } else {
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Not an HTTP URL");
            } finally {
                if (is != null) is.close();
                if (dstream != null) dstream.close();
                if (c != null) c.close();
            }
            return returnCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnCode;
    }

    /**
	 * This will upload the Video to the server through a HTTP POST
	 * 
	 * @param data
	 * @param serverName
	 * @param userName
	 * @param password
	 * @param commentString
	 * @return
	 */
    public String uploadVideoGetName(byte[] data, String serverName, String userName, String password, String commentString) {
        String functionString = "fileprefix";
        try {
            HttpConnection c = null;
            DataInputStream is = null;
            DataOutputStream dstream = null;
            StringBuffer b = new StringBuffer();
            String sysProp = getSystemPropertyString();
            try {
                c = (HttpConnection) Connector.open(UploadURL(serverName, userName));
                c.setRequestMethod(HttpConnection.POST);
                c.setRequestProperty("Connection", "Keep-Alive");
                c.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Confirguration/CLDC-1.1");
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=****4353");
                dstream = new DataOutputStream(c.openOutputStream());
                dstream.write("--****4353\r\n".getBytes());
                dstream.write(("Content-Disposition: form-data; name=username" + "\r\n\r\n" + userName.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=password" + "\r\n\r\n" + password.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=systemProperty" + "\r\n\r\n" + sysProp.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=comment" + "\r\n\r\n" + commentString.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=function" + "\r\n\r\n" + functionString.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=\"uploadedfile\"; " + "filename=\"" + "abc.3gp\"" + "\r\nContent-Type: video/3gpp\r\n\r\n").getBytes());
                int index = 0;
                int size = 1024;
                do {
                    if ((index + size) > data.length) {
                        size = data.length - index;
                    }
                    dstream.write(data, index, size);
                    index += size;
                } while (index < data.length);
                dstream.write("\r\n--****4353--\r\n\r\n".getBytes());
                is = c.openDataInputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                return b.toString();
            } catch (Exception e) {
                throw new IllegalArgumentException("Not an HTTP URL");
            } finally {
                if (is != null) is.close();
                if (dstream != null) dstream.close();
                if (c != null) c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
    }

    /**
	 * This will upload the Video to the server through a HTTP POST
	 * 
	 * @param data
	 * @param serverName
	 * @param userName
	 * @param password
	 * @param commentString
	 * @return
	 */
    public String streamVideoGetName(byte[] data, String serverName, String userName, String password, String commentString) {
        String functionString = "fileprefix";
        try {
            HttpConnection c = null;
            DataInputStream is = null;
            DataOutputStream dstream = null;
            StringBuffer b = new StringBuffer();
            String sysProp = getSystemPropertyString();
            try {
                c = (HttpConnection) Connector.open(StreamURL(serverName, userName));
                c.setRequestMethod(HttpConnection.POST);
                c.setRequestProperty("Connection", "Keep-Alive");
                c.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Confirguration/CLDC-1.1");
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=****4353");
                dstream = new DataOutputStream(c.openOutputStream());
                dstream.write("--****4353\r\n".getBytes());
                dstream.write(("Content-Disposition: form-data; name=username" + "\r\n\r\n" + userName.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=password" + "\r\n\r\n" + password.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=systemProperty" + "\r\n\r\n" + sysProp.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=comment" + "\r\n\r\n" + commentString.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=function" + "\r\n\r\n" + functionString.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=\"uploadedfile\"; " + "filename=\"" + "abc.3gp\"" + "\r\nContent-Type: video/3gpp\r\n\r\n").getBytes());
                int index = 0;
                int size = 1024;
                do {
                    if ((index + size) > data.length) {
                        size = data.length - index;
                    }
                    dstream.write(data, index, size);
                    index += size;
                } while (index < data.length);
                dstream.write("\r\n--****4353--\r\n\r\n".getBytes());
                is = c.openDataInputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                return b.toString();
            } catch (Exception e) {
                throw new IllegalArgumentException("Not an HTTP URL");
            } finally {
                if (is != null) is.close();
                if (dstream != null) dstream.close();
                if (c != null) c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
    }

    private final String CrLf = "\r\n";

    public void postThis(String url, byte[] imgData) {
        HttpConnection conn = null;
        DataOutputStream os = null;
        DataInputStream is = null;
        try {
            System.out.println("url:" + url);
            conn = (HttpConnection) Connector.open(url);
            conn.setRequestMethod(HttpConnection.POST);
            String message1 = "";
            message1 += "-----------------------------4664151417711" + CrLf;
            message1 += "Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"image.3gp\"" + CrLf;
            message1 += "Content-Type: video/3gpp" + CrLf;
            message1 += CrLf;
            String message2 = "";
            message2 += CrLf + "-----------------------------4664151417711--" + CrLf;
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=---------------------------4664151417711");
            System.out.println("open os");
            os = conn.openDataOutputStream();
            System.out.println(message1);
            os.write(message1.getBytes());
            int index = 0;
            int size = 1024;
            do {
                System.out.println("write:" + index);
                if ((index + size) > imgData.length) {
                    size = imgData.length - index;
                }
                os.write(imgData, index, size);
                index += size;
            } while (index < imgData.length);
            System.out.println("written:" + index);
            System.out.println(message2);
            os.write(message2.getBytes());
            os.flush();
            System.out.println("open is");
            is = conn.openDataInputStream();
            char buff = 512;
            int len;
            byte[] data = new byte[buff];
            do {
                System.out.println("READ");
                len = is.read(data);
                if (len > 0) {
                    System.out.println(new String(data, 0, len));
                }
            } while (len > 0);
            System.out.println("DONE");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Close connection");
            try {
                os.close();
            } catch (Exception e) {
            }
            try {
                is.close();
            } catch (Exception e) {
            }
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * This will upload the image to the server through a HTTP POST.
	 * 
	 * @param data
	 * @param serverName
	 * @param userName
	 * @param password
	 * @param commentString
	 * @return
	 */
    public String uploadImageGetName(byte[] data, String serverName, String userName, String password, String commentString) {
        String functionString = "fileprefix";
        String acc = "0.0";
        String dis = "0.0";
        String lat = "0.0";
        String log = "0.0";
        String mph = "0.0";
        String dir = "0.0";
        try {
            HttpConnection c = null;
            DataInputStream is = null;
            DataOutputStream dstream = null;
            StringBuffer b = new StringBuffer();
            String sysProp = getSystemPropertyString();
            try {
                c = (HttpConnection) Connector.open(UploadURL(serverName, userName));
                c.setRequestMethod(HttpConnection.POST);
                c.setRequestProperty("Connection", "Keep-Alive");
                c.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Confirguration/CLDC-1.1");
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=****4353");
                dstream = new DataOutputStream(c.openOutputStream());
                dstream.write("--****4353\r\n".getBytes());
                dstream.write(("Content-Disposition: form-data; name=username" + "\r\n\r\n" + userName.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=password" + "\r\n\r\n" + password.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=systemProperty" + "\r\n\r\n" + sysProp.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=comment" + "\r\n\r\n" + commentString.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=log" + "\r\n\r\n" + log.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=lat" + "\r\n\r\n" + lat.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=dir" + "\r\n\r\n" + dir.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=mph" + "\r\n\r\n" + mph.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=acc" + "\r\n\r\n" + acc.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=dis" + "\r\n\r\n" + dis.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=function" + "\r\n\r\n" + functionString.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=\"uploadedfile\"; " + "filename=\"" + "abc.jpg\"" + "\r\nContent-Type: image/jpeg\r\n\r\n").getBytes());
                int index = 0;
                int size = 1024 * 5;
                do {
                    System.out.println("write:" + index);
                    if ((index + size) > data.length) {
                        size = data.length - index;
                    }
                    dstream.write(data, index, size);
                    index += size;
                } while (index < data.length);
                dstream.write("\r\n--****4353--\r\n\r\n".getBytes());
                is = c.openDataInputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                return b.toString();
            } catch (Exception e) {
                throw new IllegalArgumentException("Not an HTTP URL");
            } finally {
                if (is != null) is.close();
                if (dstream != null) dstream.close();
                if (c != null) c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
    }

    /**
	 * 
	 * Send a Message.
	 * @param recipient 
	 * @param subject 
	 * @param body 
	 * @param serverName 
	 * @param username 
	 * @param password
	 * @return dead if it's dead? ;)
	 */
    public String sendMessage(String recipient, String subject, String body, String serverName, String username, String password) {
        try {
            HttpConnection c = null;
            DataInputStream is = null;
            DataOutputStream dstream = null;
            StringBuffer b = new StringBuffer();
            String sysProp = getSystemPropertyString();
            try {
                c = (HttpConnection) Connector.open(replyMessageURL(serverName));
                c.setRequestMethod(HttpConnection.POST);
                c.setRequestProperty("Connection", "Keep-Alive");
                c.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Confirguration/CLDC-1.1");
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=****4353");
                dstream = new DataOutputStream(c.openOutputStream());
                dstream.write("--****4353\r\n".getBytes());
                dstream.write(("Content-Disposition: form-data; name=username" + "\r\n\r\n" + username.trim().toLowerCase() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=password" + "\r\n\r\n" + password.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=systemProperty" + "\r\n\r\n" + sysProp.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=recipient" + "\r\n\r\n" + recipient.trim().toLowerCase() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=body" + "\r\n\r\n" + body.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=subject" + "\r\n\r\n" + subject.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=sender" + "\r\n\r\n" + username.trim().toLowerCase() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=MessageSent" + "\r\n\r\n" + "TRUE" + "\r\n" + "--****4353--\r\n\r\n").getBytes());
                is = c.openDataInputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                return b.toString().trim();
            } catch (Exception e) {
                throw new IllegalArgumentException("Not an HTTP URL");
            } finally {
                if (is != null) is.close();
                if (dstream != null) dstream.close();
                if (c != null) c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Please Check your Connection Settings";
    }

    /**
	 * Perform all friends related function here.
	 */
    public String friendsFunction(String username, String password, String friend, String message, String operation) {
        try {
            HttpConnection c = null;
            DataInputStream is = null;
            DataOutputStream dstream = null;
            StringBuffer b = new StringBuffer();
            String sysProp = getSystemPropertyString();
            try {
                c = (HttpConnection) Connector.open(friendsURL("www.glogger.mobi"));
                c.setRequestMethod(HttpConnection.POST);
                c.setRequestProperty("Connection", "Keep-Alive");
                c.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Confirguration/CLDC-1.1");
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=****4353");
                dstream = new DataOutputStream(c.openOutputStream());
                dstream.write("--****4353\r\n".getBytes());
                dstream.write(("Content-Disposition: form-data; name=username" + "\r\n\r\n" + username.trim().toLowerCase() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=password" + "\r\n\r\n" + password.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=systemProperty" + "\r\n\r\n" + sysProp.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=friend" + "\r\n\r\n" + friend.trim().toLowerCase() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=message" + "\r\n\r\n" + message + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=operation" + "\r\n\r\n" + operation.trim() + "\r\n" + "--****4353\r\n\r\n").getBytes());
                is = c.openDataInputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                return b.toString().trim();
            } catch (Exception e) {
                throw new IllegalArgumentException("Not an HTTP URL");
            } finally {
                if (is != null) is.close();
                if (dstream != null) dstream.close();
                if (c != null) c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Please check your connection settings";
    }

    public String replyMessageURL(String sName) {
        return "http://" + sName + "/pmsys/sendPhone.php";
    }

    public String friendsURL(String sName) {
        return "http://" + sName + "/friends/friendsFunctionsHook.php";
    }

    /**
	 * Push the Data through OBEX
	 * @param btConnectionURL the bt URL
	 * @param filename - the filename (e.g. myImage)
	 * @param filetype - the filetype (e.g. txt, jpg, gif)
	 * @param data - data to transfer
	 * @return 0 good, 1 errror
	 */
    public int push(String btConnectionURL, String filename, String filetype, byte data[]) {
        Connection connection;
        try {
            connection = Connector.open(btConnectionURL);
            System.out.println("Connecting...");
            ClientSession cs = (ClientSession) connection;
            HeaderSet hs = cs.createHeaderSet();
            cs.connect(hs);
            hs.setHeader(HeaderSet.NAME, filename);
            hs.setHeader(HeaderSet.TYPE, filetype);
            hs.setHeader(HeaderSet.LENGTH, new Long(data.length));
            Operation putOperation = cs.put(hs);
            DataOutputStream outputStream = putOperation.openDataOutputStream();
            outputStream.write(data);
            outputStream.close();
            putOperation.close();
            cs.disconnect(null);
            connection.close();
            System.out.println("File sent.");
            return 0;
        } catch (Exception e) {
            System.out.println("Failed: " + e.toString());
            return 1;
        }
    }

    public String vote(String serverName, String owner, String prefix, int option, String spreadusername) {
        try {
            HttpConnection c = null;
            DataInputStream is = null;
            DataOutputStream dstream = null;
            StringBuffer b = new StringBuffer();
            String sysProp = getSystemPropertyString();
            try {
                c = (HttpConnection) Connector.open(voteURL(serverName));
                c.setRequestMethod(HttpConnection.POST);
                c.setRequestProperty("Connection", "Keep-Alive");
                c.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Confirguration/CLDC-1.1");
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=****4353");
                dstream = new DataOutputStream(c.openOutputStream());
                dstream.write("--****4353\r\n".getBytes());
                dstream.write(("Content-Disposition: form-data; name=option" + "\r\n\r\n" + option + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=systemProperty" + "\r\n\r\n" + sysProp.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=owner" + "\r\n\r\n" + owner.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=spreadusername" + "\r\n\r\n" + spreadusername.trim() + "\r\n" + "--****4353\r\n").getBytes());
                dstream.write(("Content-Disposition: form-data; name=prefix" + "\r\n\r\n" + prefix.trim() + "\r\n" + "--****4353--\r\n\r\n").getBytes());
                is = c.openDataInputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    b.append((char) ch);
                }
                return b.toString().trim();
            } catch (Exception e) {
                throw new IllegalArgumentException("Not an HTTP URL");
            } finally {
                if (is != null) is.close();
                if (dstream != null) dstream.close();
                if (c != null) c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "dead";
    }
}
