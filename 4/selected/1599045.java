package org.lc.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 
 * @author LC ��̬��ҳ��
 */
public class WriteFile {

    public WriteFile() {
    }

    /**
	 * ͨ��urlд��path��ɾ�̬html
	 * 
	 * @param url
	 * @param path
	 * @throws Exception
	 */
    public static void writeHtml(String url, String path) throws Exception {
        String st = openUrl(url);
        boolean b = writeFile(path, st);
        if (!b) {
            throw new Exception("write Html error");
        }
    }

    /**
	 * ��pathд�ļ� 
	 * 
	 * @param pathfile
	 * @param s2
	 * @return
	 */
    public static boolean writeFile(String pathfile, String s2) {
        try {
            String strPath = pathfile.substring(0, pathfile.lastIndexOf(File.separator));
            File path = new File(strPath);
            path.mkdir();
            File file = new File(pathfile);
            byte[] src = s2.getBytes("UTF-8");
            int length = src.length;
            FileOutputStream fis = new FileOutputStream(file);
            FileChannel fc = fis.getChannel();
            ByteBuffer bb = ByteBuffer.allocate(length);
            bb.put(src);
            bb.flip();
            fc.write(bb);
            bb.clear();
            fc.close();
            fis.close();
            return true;
        } catch (Exception exception) {
            System.out.println(exception);
            return false;
        }
    }

    /**
	 * ͨ��urlȡ�þ�̬ҳ�������
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
    public static String openUrl(String url) throws Exception {
        InputStream in = null;
        HttpURLConnection c = null;
        URL uRL = null;
        StringBuffer sb = new StringBuffer();
        if (url == null || url.length() < 1) return "";
        try {
            uRL = new URL(url);
            c = (HttpURLConnection) uRL.openConnection();
            c.connect();
            in = c.getInputStream();
            BufferedReader l_reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = l_reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (Exception e) {
            System.out.println("openUrl error:" + e.toString());
            throw e;
        } finally {
            try {
                if (in != null) in.close();
                if (c != null) c.disconnect();
                uRL = null;
            } catch (Exception ce) {
            }
        }
        return sb.toString();
    }

    public static void createFile(String filename, String html) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedOutputStream bufferedoutputstream = new BufferedOutputStream(new FileOutputStream(filename));
            OutputStreamWriter outputstreamwriter = new OutputStreamWriter(bufferedoutputstream, "UTF-8");
            int n = html.length();
            outputstreamwriter.write(html, 0, n);
            outputstreamwriter.close();
            bufferedoutputstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * д�ļ�
	 * 
	 * @param toSave
	 * @param filename
	 * @return
	 * @throws FileNotFoundException
	 * @throws PassportException
	 */
    public static synchronized int writeFile(String toSave, String filename, boolean append) throws FileNotFoundException {
        int returnValue = -1;
        try {
            String path = filename.substring(0, filename.lastIndexOf("/"));
            File dir = new File(path + "/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f = new File(filename);
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fos;
            fos = new FileOutputStream(filename, append);
            OutputStreamWriter osw;
            osw = new OutputStreamWriter(fos, "GBK");
            osw.write(toSave);
            osw.close();
            fos.close();
            returnValue = 1;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("д��־���ִ���,�Ҳ����ļ���");
        } catch (UnsupportedEncodingException e) {
            throw new FileNotFoundException("д��־���ִ���");
        } catch (IOException e) {
            throw new FileNotFoundException("д��־���ִ���");
        }
        return returnValue;
    }
}
