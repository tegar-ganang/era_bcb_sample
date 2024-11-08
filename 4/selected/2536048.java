package jeeves.utils;

import jeeves.constants.ConfigFile;
import java.io.*;
import sun.misc.*;
import org.jdom.*;

/** class to encode/decode binary files to base64 strings
  */
public class BinaryFile {

    private static final int BUF_SIZE = 8192;

    public static Element encode(int responseCode, String path, String name, boolean remove) {
        Element response = new Element("response");
        response.setAttribute("responseCode", responseCode + "");
        response.setAttribute("path", path);
        response.setAttribute("name", name);
        response.setAttribute("remove", remove ? "y" : "n");
        return response;
    }

    public static Element encode(int responseCode, String path) {
        Element response = new Element("response");
        response.setAttribute("responseCode", responseCode + "");
        response.setAttribute("path", path);
        return response;
    }

    public static String getContentType(Element response) {
        String path = response.getAttributeValue("path");
        return getContentType(path);
    }

    public static String getContentLength(Element response) {
        String path = response.getAttributeValue("path");
        File f = new File(path);
        return f.length() + "";
    }

    public static void removeIfTheCase(Element response) {
        boolean remove = "y".equals(response.getAttributeValue("remove"));
        if (remove) {
            String path = response.getAttributeValue("path");
            new File(path).delete();
        }
    }

    public static String getContentDisposition(Element response) {
        String name = response.getAttributeValue("name");
        if (name == null) {
            name = response.getAttributeValue("path");
            name = new File(name).getName();
        }
        return "attachment;filename=" + name;
    }

    public static int getResponseCode(Element response) {
        return Integer.parseInt(response.getAttributeValue("responseCode"));
    }

    public static void write(Element response, OutputStream output) throws IOException {
        String path = response.getAttributeValue("path");
        File f = new File(path);
        InputStream input = new FileInputStream(f);
        copy(input, output, true, false);
    }

    public static void copy(InputStream in, OutputStream out, boolean closeInput, boolean closeOutput) throws IOException {
        BufferedInputStream input = new BufferedInputStream(in);
        try {
            byte buffer[] = new byte[BUF_SIZE];
            int nRead;
            while ((nRead = input.read(buffer)) > 0) out.write(buffer, 0, nRead);
        } finally {
            if (closeInput) in.close();
            if (closeOutput) out.close();
        }
    }

    private static String getContentType(String fName) {
        if (fName.endsWith(".gif")) return "image/gif"; else if (fName.endsWith(".jpg") || fName.endsWith(".jpeg")) return "image/jpeg"; else if (fName.endsWith(".png")) return "application/png"; else if (fName.endsWith(".bmp")) return "application/bmp"; else if (fName.endsWith(".zip")) return "application/zip"; else if (fName.endsWith(".pdf")) return "application/pdf"; else if (fName.endsWith(".eps")) return "application/eps"; else if (fName.endsWith(".ai")) return "application/ai"; else if (fName.endsWith(".pmf")) return "application/pmf"; else if (fName.endsWith(".e00")) return "application/e00"; else return ("application/binary");
    }
}
