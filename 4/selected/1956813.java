package ClasesKinesiologia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;

/**
 *
 * @author Adrian
 */
public class Ficheros {

    File ruta = new File("");

    private String fi;

    public void copyFile(File s, File t) {
        try {
            FileChannel in = (new FileInputStream(s)).getChannel();
            FileChannel out = (new FileOutputStream(t)).getChannel();
            in.transferTo(0, s.length(), out);
            in.close();
            out.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public boolean obs(String nom, Ficha fi) {
        File fic = new File(ruta.getAbsolutePath() + "\\DatosPersonales\\" + nom + "\\" + fi.getnom() + "\\observaciones.doc");
        return fic.exists();
    }

    public void verObs(String nom, Ficha fi) {
        File fic = new File(ruta.getAbsolutePath() + "\\DatosPersonales\\" + nom + "\\" + fi.getnom() + "\\observaciones.doc");
        if (fic.exists() != true) {
            BufferedWriter outfile;
            try {
                outfile = new BufferedWriter(new FileWriter(fic));
                String str = "   ";
                outfile.write(str);
                outfile.close();
            } catch (IOException ex) {
                Logger.getLogger(Ficheros.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Runtime r = Runtime.getRuntime();
        try {
            r.exec("explorer.exe " + fic.getCanonicalPath());
        } catch (IOException ex) {
        }
    }

    public File agregaFormu(Paciente p, File f) {
        File form = new File(ruta.getAbsolutePath() + "\\DatosPersonales\\" + p.getDoc() + "\\Formularios\\" + f.getName());
        this.copyFile(f, form);
        return form;
    }

    public void cargaFormu(DefaultListModel d, String nom, String fi) {
        File fic = new File(ruta.getAbsolutePath() + "\\DatosPersonales\\" + nom + "\\Formularios");
        if (fic.exists() == false) fic.mkdir();
        for (String g : fic.list()) {
            d.addElement(g);
        }
    }
}
