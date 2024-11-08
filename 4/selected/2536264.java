package Tot_PSE_Com;

import java.lang.Exception;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.jar.*;
import java.util.*;
import javax.swing.*;

public class Jar {

    public void be(String neve, String[] filek, String strmainfest, String akt_konyvtar) throws Exception {
        byte buffer3[] = new byte[1024];
        int k = 0, k1 = 0;
        FileOutputStream stream = new FileOutputStream(neve);
        JarOutputStream out;
        if (!"".equals(strmainfest)) {
            strmainfest = strmainfest.substring(akt_konyvtar.length(), strmainfest.length() - 6);
            Manifest manifest;
            StringBuffer StringBuffer2 = new StringBuffer();
            StringBuffer2.append("Manifest-Version: 1.0\n");
            StringBuffer2.append("Main-Class: " + strmainfest.replace("/", ".").replace("\\", ".") + "\n");
            InputStream is1 = new ByteArrayInputStream(StringBuffer2.toString().getBytes("UTF-8"));
            manifest = new Manifest(is1);
            out = new JarOutputStream(stream, manifest);
        } else {
            out = new JarOutputStream(stream);
        }
        for (k = 0; k < filek.length; k++) {
            String utvonal;
            utvonal = filek[k].substring(akt_konyvtar.length(), filek[k].length());
            JarEntry jarAdd = new JarEntry(utvonal);
            out.putNextEntry(jarAdd);
            FileInputStream in = new FileInputStream(filek[k]);
            while (true) {
                int nRead = in.read(buffer3, 0, buffer3.length);
                if (nRead <= 0) break;
                out.write(buffer3, 0, nRead);
            }
            in.close();
        }
        out.close();
        stream.close();
    }

    public void ki(String mit, String hova) throws Exception {
        JarFile jar = new JarFile(mit);
        Enumeration eenum = jar.entries();
        while (eenum.hasMoreElements()) {
            JarEntry file = (JarEntry) eenum.nextElement();
            File unjarDestinationDirectory = new File(hova);
            File f = new File(unjarDestinationDirectory, file.getName());
            File destinationParent = f.getParentFile();
            destinationParent.mkdirs();
            if (file.isDirectory()) {
                f.mkdir();
                continue;
            }
            InputStream is = jar.getInputStream(file);
            FileOutputStream fos = new FileOutputStream(f);
            while (is.available() > 0) {
                fos.write(is.read());
            }
            fos.close();
            is.close();
        }
    }
}
