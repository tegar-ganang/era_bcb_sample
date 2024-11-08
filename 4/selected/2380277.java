package pseutem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/** Clase que descomprime el archivo ZIP que contiene el compilador javac, necesario para la aplicación.
 * 
 * @author Diego Guerrero <diego.guerrero87@gmail.com>
 */
public class UnZip {

    public static File tempFolder;

    /**
     * Copia el archivo ZIP desde la aplicación hacia la carpeta temporal del OS
     */
    public static void copyZip() {
        InputStream is;
        OutputStream os;
        String javacZip = "";
        try {
            if ("windows".equalsIgnoreCase(Compilador.getSo())) {
                javacZip = "javacWin.zip";
                is = UnZip.class.getResourceAsStream("javacWin.zip");
            } else if ("linux".equalsIgnoreCase(Compilador.getSo())) {
                javacZip = "javacLinux.zip";
                is = UnZip.class.getResourceAsStream("javacLinux.zip");
            }
            is = UnZip.class.getResourceAsStream(javacZip);
            File tempZip = File.createTempFile("tempJavacJTraductor", ".zip");
            tempZip.mkdir();
            tempZip.deleteOnExit();
            os = FileUtils.openOutputStream(tempZip);
            IOUtils.copy(is, os);
            is.close();
            os.close();
            extractZip(tempZip.getPath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(PseutemView.mainPanel, "Error al copiar los archivos temporales necesarios para ejecutar el programa:\n\n" + ex, "Error copiando.", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Descomprime el archivo ZIP dentro de la carpeta temporal del OS
     * @param zip   Archivo ZIP copiado desde copyZip()
     */
    public static void extractZip(String zip) {
        try {
            if ("windows".equalsIgnoreCase(Compilador.getSo())) {
                String dirName = System.getProperty("java.io.tmpdir");
                tempFolder = new File(dirName + "javac");
            } else if ("linux".equalsIgnoreCase(Compilador.getSo())) {
                String dirName = System.getProperty("user.home");
                tempFolder = new File(dirName + Compilador.getFileSeparator() + "javac");
            }
            if (!tempFolder.exists()) {
                tempFolder.mkdir();
            } else {
                UnZip.deleteFile(UnZip.tempFolder);
            }
            Compilador.setDirJavac(tempFolder.getCanonicalPath() + Compilador.getFileSeparator() + "bin" + Compilador.getFileSeparator());
            tempFolder.deleteOnExit();
            ZipFile archive = new ZipFile(zip);
            Enumeration e = archive.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                File file = new File(tempFolder, entry.getName());
                if (entry.isDirectory() && !file.exists()) {
                    file.mkdirs();
                } else {
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    InputStream in = archive.getInputStream(entry);
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    byte[] buffer = new byte[8192];
                    int read;
                    while (-1 != (read = in.read(buffer))) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.close();
                }
            }
            archive.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(PseutemView.mainPanel, "Error al extraer los archivos necesarios para el funcionamiento de la aplicación:\n\n" + ex, "Error Zip.", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Si existe la carpeta temporal javac, borra su contenido recursivamente para dejarla vacia y así se elimine al salir de la aplicación
     * @param path  Ruta de la carpeta javac para borrar su contenido
     */
    public static void deleteFile(File path) {
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteFile(files[i]);
                        files[i].delete();
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
    }
}
