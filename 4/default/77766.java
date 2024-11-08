import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;

public class GuideDataLoader {

    public GuideDataLoader(int type) {
    }

    public Vector getDataFromURL(String loadURL) throws Exception {
        Vector<byte[]> resultData = new Vector<byte[]>();
        System.out.println("Loading XMLTV data from URL : " + loadURL);
        Vector<byte[]> data = new Vector<byte[]>();
        getContentUsingThread(new URL(loadURL), data);
        String pageData = "";
        if (data.size() == 1) {
            pageData = new String((byte[]) data.get(0));
        }
        if (pageData.length() > 0 && pageData.indexOf("<XMLTV_FILE_LIST>") > -1) {
            System.out.println("Loading XMLTV data from file list");
            String[] fileList = extractNames(pageData);
            int lastIndex = loadURL.lastIndexOf("/");
            if (lastIndex > -1) {
                String baseUrl = loadURL.substring(0, lastIndex + 1);
                for (int x = 0; x < fileList.length; x++) {
                    String dataURL = baseUrl + fileList[x];
                    System.out.println("Loading XMLTV data from URL : " + dataURL);
                    Vector<byte[]> pages = new Vector<byte[]>();
                    int result = getContentUsingThread(new URL(dataURL), pages);
                    if (result == 200) {
                        for (int y = 0; y < pages.size(); y++) {
                            byte[] pd = (byte[]) pages.get(y);
                            if (pd.length > 0) resultData.add(pd);
                        }
                    }
                }
            }
        } else if (data.size() > 0) {
            for (int y = 0; y < data.size(); y++) {
                byte[] pd = (byte[]) data.get(y);
                if (pd.length > 0) resultData.add(pd);
            }
        }
        return resultData;
    }

    private String[] extractNames(String nameList) {
        System.out.println("Extracting File list from Listing File");
        String names[] = nameList.split("\n");
        Vector<String> namesOfFiles = new Vector<String>();
        if (names.length > 1) {
            for (int x = 1; x < names.length; x++) {
                if (names[x].trim().length() > 0) {
                    namesOfFiles.add(names[x].trim());
                    System.out.println("Adding : " + names[x].trim());
                }
            }
        }
        return (String[]) namesOfFiles.toArray(new String[0]);
    }

    private int getContentUsingThread(URL location, Vector<byte[]> buffs) {
        Thread loaderThread = null;
        LoaderTread loader = null;
        try {
            loader = new LoaderTread(location);
            loaderThread = new Thread(Thread.currentThread().getThreadGroup(), loader, loader.getClass().getName());
            loaderThread.start();
            while (!loader.isFinished()) {
                if (loader.isTimedOut(30)) {
                    System.out.println("\nTimout reached : Killing loader Thread");
                    loader.kill();
                    loaderThread.interrupt();
                    break;
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        loaderThread.interrupt();
        if (loader.getResponceCode() != 200) return loader.getResponceCode();
        if (location.toString().toUpperCase().indexOf(".ZIP", location.toString().length() - 4) > -1) {
            if (unZip(loader.getDataBytes(), buffs) > 0) return 200; else return 404;
        } else {
            buffs.add(loader.getDataBytes());
            return loader.getResponceCode();
        }
    }

    private int unZip(byte[] data, Vector<byte[]> extractedXML) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = null;
        ZipInputStream zis = new ZipInputStream(bais);
        int count = 0;
        try {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                if (ze.getName() != null && ze.getName().indexOf(".xml") > -1) {
                    baos = new ByteArrayOutputStream();
                    System.out.println("Extracting : " + ze.getName());
                    int len;
                    byte[] buf = new byte[1024];
                    while ((len = zis.read(buf)) > 0) {
                        baos.write(buf, 0, len);
                    }
                    extractedXML.add(baos.toByteArray());
                    count++;
                }
                ze = zis.getNextEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public Vector getDataFromFiles(String loadDir, StringBuffer buff, int format) {
        Vector<byte[]> resultData = new Vector<byte[]>();
        File xmlDir = new File(loadDir);
        String actualFile = "";
        if (xmlDir.isDirectory()) {
            String[] fileList = xmlDir.list();
            for (int x = 0; x < fileList.length; x++) {
                if (fileList[x].indexOf(".xml") > 0) {
                    actualFile = loadDir + File.separator + fileList[x];
                    System.out.println("Loading XMLTV data from " + actualFile);
                    buff.append("Loading XMLTV data from " + actualFile);
                    if (format == 1) buff.append("<br>");
                    buff.append("\n");
                    byte[] data = null;
                    try {
                        data = getFileContents(actualFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (data != null && data.length > 10) resultData.add(data);
                }
            }
        }
        return resultData;
    }

    private byte[] getFileContents(String fileName) throws Exception {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(fileName);
        byte[] data = new byte[1024];
        int read = fis.read(data, 0, 1024);
        while (read > -1) {
            ba.write(data, 0, read);
            read = fis.read(data, 0, 1024);
        }
        fis.close();
        return ba.toByteArray();
    }

    void printHex(byte[] b) {
        for (int i = 0; i < b.length; ++i) {
            if (i % 16 == 0) {
                System.out.print(Integer.toHexString((i & 0xFFFF) | 0x10000).substring(1, 5) + " - ");
            }
            System.out.print(Integer.toHexString((b[i] & 0xFF) | 0x100).substring(1, 3) + " ");
            if (i % 16 == 15 || i == b.length - 1) {
                int j;
                for (j = 16 - i % 16; j > 1; --j) {
                    System.out.print("   ");
                }
                System.out.print(" - ");
                int start = (i / 16) * 16;
                int end = (b.length < i + 1) ? b.length : (i + 1);
                for (j = start; j < end; ++j) {
                    if (b[j] >= 32 && b[j] <= 126) {
                        System.out.print((char) b[j]);
                    } else {
                        System.out.print(".");
                    }
                }
                System.out.println();
            }
        }
        System.out.println();
    }
}
