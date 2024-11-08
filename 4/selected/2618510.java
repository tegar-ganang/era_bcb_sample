package fneimg2pdf;

import java.io.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.*;
import javax.swing.*;

public class Converter {

    public static void convertDir(File srcDir, File destDir) throws Exception {
        if (!(srcDir.exists() && srcDir.isDirectory())) errorMessage("Source Directory [" + srcDir.getAbsolutePath() + "]\n is either not a directory or it does not exist.");
        destDir.mkdirs();
        File images[] = srcDir.listFiles(new ImageFileFilter());
        if (images == null) return;
        for (int i = 0; i < images.length; i++) {
            convertImage(images[i], destDir);
        }
    }

    private static void convertImage(File imageFile, File destDir) throws Exception {
        File pdfFile = getDestinationPDFFile(imageFile, destDir);
        if (pdfFile.exists() && pdfFile.isFile() && (pdfFile.lastModified() > imageFile.lastModified())) return;
        byte imageData[] = loadFile(imageFile);
        ImageIcon ii = new ImageIcon(imageData);
        Document doc = new Document();
        doc.setMargins(0, 0, 0, 0);
        Rectangle rec = new Rectangle(ii.getIconWidth(), ii.getIconHeight());
        doc.setPageSize(rec);
        PdfWriter.getInstance(doc, new FileOutputStream(pdfFile));
        doc.open();
        doc.add(Image.getInstance(imageData));
        doc.close();
    }

    private static byte[] loadFile(File f) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream in = new FileInputStream(f);
        int i;
        byte b[] = new byte[1024 * 1024];
        while ((i = in.read(b)) != -1) baos.write(b, 0, i);
        in.close();
        baos.close();
        return baos.toByteArray();
    }

    private static File getDestinationPDFFile(File image, File destDir) {
        String name = image.getName();
        name = name.substring(0, name.lastIndexOf(".")) + ".pdf";
        return new File(destDir, name);
    }

    public static void errorMessage(String msg) throws Exception {
        throw new ConversionError(msg);
    }
}
