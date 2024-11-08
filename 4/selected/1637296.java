package medisnap.gui;

import javax.imageio.*;
import java.io.*;
import java.awt.*;
import medisnap.*;
import medisnap.dblayer.*;
import java.util.*;
import java.awt.image.*;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.*;
import java.util.logging.*;

/**
 *
 * @author jan
 */
public class ImportFileThread extends Thread {

    File file;

    Image img;

    NeuesFoto nf;

    GeneralCallbackInformer gci;

    boolean delete;

    Date photo_date;

    public boolean runningtransaction = false;

    public void freeMemory() {
        if (img != null) {
            img.flush();
            img = null;
        }
        if (nf != null) {
            nf.freeMemory();
            nf = null;
        }
        file = null;
    }

    /** Creates a new instance of ImportFileThread */
    public ImportFileThread(File file, Image img, NeuesFoto nf, boolean delete, Date photo_date) {
        this.file = file;
        this.img = img;
        this.nf = nf;
        this.delete = delete;
        this.photo_date = photo_date;
    }

    public ImportFileThread(File file, Image img, GeneralCallbackInformer gci, boolean delete, Date photo_date) {
        this.file = file;
        this.img = img;
        this.gci = gci;
        this.delete = delete;
        this.photo_date = photo_date;
    }

    @Override
    public void run() {
        try {
            this.runningtransaction = true;
            DBLayer.beginTransaction();
            double width = (double) (img.getWidth(null));
            double height = (double) (img.getHeight(null));
            double thumbscale = FotoHelper.getScaleFactor(width, height, MediSnapOptions.getThumbWidth(), MediSnapOptions.getThumbHeight());
            valueBild vb = new valueBild(new Integer(0), MediSnapOptions.getNewPicturePath(), new String(""), photo_date, MediSnap.activeValueLokalisation.getId(), thumbscale, new String(""));
            valueBild nb = DBLayer.createBild(vb, photo_date);
            DBLayer.commit();
            this.runningtransaction = false;
            MediSnap.activeValueBild = nb;
            int twidth = (int) (width * thumbscale);
            int theight = (int) (height * thumbscale);
            BufferedImage bimg = new BufferedImage(twidth, theight, BufferedImage.TYPE_INT_RGB);
            Image scaled = img.getScaledInstance(twidth, theight, Image.SCALE_DEFAULT);
            String imgid = nb.getId().toString();
            File fscaled = new File(MediSnap.activeValuePatient.getThumbnailPath(nb));
            fscaled.mkdirs();
            Graphics2D bufImageGraphics = bimg.createGraphics();
            bufImageGraphics.drawImage(scaled, 0, 0, null);
            ImageIO.write(bimg, "jpg", fscaled);
            thumbscale = FotoHelper.getScaleFactor(img.getWidth(null), img.getHeight(null), MediSnapOptions.getListWidth(), MediSnapOptions.getListHeight());
            twidth = (int) (width * thumbscale);
            theight = (int) (height * thumbscale);
            bimg = new BufferedImage(twidth, theight, BufferedImage.TYPE_INT_RGB);
            scaled = img.getScaledInstance(twidth, theight, Image.SCALE_DEFAULT);
            imgid = nb.getId().toString();
            fscaled = new File(MediSnap.activeValuePatient.getListPath(nb));
            bufImageGraphics = bimg.createGraphics();
            bufImageGraphics.drawImage(scaled, 0, 0, null);
            ImageIO.write(bimg, "jpg", fscaled);
            File neu = new File(MediSnap.activeValuePatient.getPicturePath(nb));
            copy(file, neu);
            File lfile = new File(MediSnap.activeValuePatient.getListPath(nb));
            ImageIcon limage = new ImageIcon(ImageIO.read(lfile));
            valueBildImage vbi = new valueBildImage(nb, limage, MediSnap.activeValueLokalisation);
            if (nf != null) {
                nf.neue_fotos.add(vbi);
                nf.jlistBilderListe.setListData(nf.neue_fotos);
                nf.jlistBilderListe.setSelectedValue(vbi, true);
            }
            if (gci != null) {
                gci.callback();
            }
            if (delete) {
                file.deleteOnExit();
            }
            if (bimg != null) {
                bimg.flush();
                bimg = null;
            }
            if (scaled != null) {
                scaled.flush();
                scaled = null;
            }
            fscaled = null;
            bufImageGraphics = null;
            neu = null;
        } catch (java.io.IOException e) {
            MediSnap.log.severe("Das Foto konnte nicht gespeichert werden! Bitte überprüfen Sie die Pfad-Einstellungen!" + e);
            JOptionPane.showMessageDialog(null, "Das Foto konnte nicht gespeichert werden! Bitte überprüfen Sie die Pfad-Einstellungen!", "Kritischer Fehler", JOptionPane.ERROR_MESSAGE);
            DBLayer.rollback(null);
        } catch (java.sql.SQLException e) {
            DBLayer.rollback(e);
            MediSnap.log.severe("Speichern des Fotos fehlgeschlagen!" + e);
            javax.swing.JOptionPane.showMessageDialog(null, "Das neue Foto konnten nicht gespeichert werden!\n" + "Sollte diese oder ähnliche Meldungen wiederkehren arbeitet ihre Medisnap Datenbank nicht korrekt." + "Arbeiten mit Medisnap ist dann wahrscheinlich nicht mehr möglich! ", "Datenbankzugriff-Fehler", javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (java.lang.IllegalArgumentException iae) {
            MediSnap.log.severe("Das Foto konnte nicht gespeichert werden! Bitte überprüfen Sie die Pfad-Einstellungen!" + iae);
            JOptionPane.showMessageDialog(null, "Das Foto konnte nicht gespeichert werden! Bitte überprüfen Sie die Pfad-Einstellungen!", "Kritischer Fehler", JOptionPane.ERROR_MESSAGE);
            DBLayer.rollback(null);
        }
        file = null;
        img = null;
        nf = null;
    }

    public static void copy(File source, File dest) throws java.io.IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
