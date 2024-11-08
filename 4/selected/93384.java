package modele;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectory;

public class PhotoModele extends AbstractModele {

    public File image;

    public String nom;

    public String date;

    public DateFormat sdf = DateFormat.getDateInstance(DateFormat.SHORT);

    public SimpleDateFormat sdf_exif = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

    public String date_exif;

    public String statut;

    public String type;

    public long taille;

    public int hauteur;

    public int largeur;

    public int largeurMini;

    public int hauteurMini;

    public BufferedImage miniature;

    public URL url;

    public Metadata exif;

    private PhotoModele photo;

    private Executor backgroundExec;

    private static final int DEFAULT_ICON_DIM = 320;

    private static final String mimeImageIO[] = ImageIO.getReaderMIMETypes();

    private BufferedImage imageBI;

    public PhotoModele() {
    }

    public PhotoModele(File img) {
        this.image = img;
        this.nom = img.getName();
        this.date = sdf.format(new Date(img.lastModified()));
        this.date_exif = "";
        this.statut = "";
        this.taille = img.length() / 1024;
        try {
            this.exif = ImageMetadataReader.readMetadata(img);
        } catch (ImageProcessingException e) {
            System.out.println("File : " + this.nom + " n'est pas dans un format contenant des données exif.");
        }
        try {
            this.imageBI = ImageIO.read(img);
            this.hauteur = imageBI.getHeight();
            this.largeur = imageBI.getWidth();
        } catch (IOException e) {
            e.printStackTrace();
        }
        creerMiniatures(this);
    }

    private void setExif() {
        if (exif != null) {
            Directory exifDirectory = exif.getDirectory(ExifDirectory.class);
            date_exif = exifDirectory.getString(ExifDirectory.TAG_DATETIME_ORIGINAL);
        }
    }

    private void addPhoto(File f) {
        Boolean isImage = false;
        ImageReader reader = null;
        ImageInputStream iis = null;
        String mimeFile = "";
        try {
            InputStream is = new FileInputStream(f);
            iis = ImageIO.createImageInputStream(is);
            Iterator<?> iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                System.out.println("Impossible de créer une photo : pas une image");
                isImage = false;
            } else {
                reader = (ImageReader) iter.next();
                mimeFile = reader.getFormatName();
            }
            iis.close();
        } catch (IOException e) {
            System.out.println("Impossible de créer une photo : format d'image inconnu.");
            isImage = false;
        }
        mimeFile = mimeFile.toLowerCase();
        for (String mio : mimeImageIO) {
            if (stringOccur(mio, mimeFile) <= 0) {
                isImage = true;
            } else {
                isImage = false;
            }
        }
        if (isImage == true) {
            photo = new PhotoModele(f);
            photo.type = mimeFile;
            photo.setExif();
            notifyObservateur(photo);
            notifyObservateurPreview(photo);
        }
    }

    @Override
    public void creerMiniatures(final PhotoModele photo) {
        Runnable r = new Runnable() {

            public void run() {
                photo.largeurMini = photo.largeur;
                photo.hauteurMini = photo.hauteur;
                if (photo.largeur > photo.hauteur) {
                    if (photo.largeur > DEFAULT_ICON_DIM) {
                        photo.largeurMini = DEFAULT_ICON_DIM;
                        photo.hauteurMini = photo.hauteur * DEFAULT_ICON_DIM / photo.largeur;
                    } else {
                        photo.largeurMini = photo.largeur;
                        photo.hauteurMini = photo.hauteur;
                    }
                } else {
                    if (photo.hauteur > DEFAULT_ICON_DIM) {
                        photo.hauteurMini = DEFAULT_ICON_DIM;
                        photo.largeurMini = photo.largeur * DEFAULT_ICON_DIM / photo.hauteur;
                    } else {
                        photo.largeurMini = photo.largeur;
                        photo.hauteurMini = photo.hauteur;
                    }
                }
                BufferedImage scaledImage = new BufferedImage(photo.largeurMini, photo.hauteurMini, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics2D = scaledImage.createGraphics();
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics2D.drawImage(photo.imageBI, 0, 0, photo.largeurMini, photo.hauteurMini, null);
                graphics2D.dispose();
                photo.miniature = scaledImage;
            }
        };
        backgroundExec = Executors.newCachedThreadPool();
        backgroundExec.execute(r);
    }

    @Override
    public void importerPhotos() {
    }

    @Override
    public void listerPhotos(final File repertoire) {
        Runnable r1 = new Runnable() {

            public void run() {
                lister(repertoire);
            }
        };
        backgroundExec = Executors.newCachedThreadPool();
        backgroundExec.execute(r1);
    }

    private void lister(File repertoire) {
        File[] list = repertoire.listFiles();
        System.out.println("debut : " + repertoire.toString());
        if (repertoire.isDirectory()) {
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].isDirectory()) {
                        System.out.println("isDirectory : " + list[i]);
                        listerPhotos(list[i]);
                    } else {
                        if (list[i].canRead() && list[i].isFile()) {
                            System.out.println("traiter : " + list[i].getAbsolutePath());
                            addPhoto(list[i]);
                        }
                    }
                }
            } else {
                System.out.println(repertoire + " : Erreur de lecture.");
            }
        }
    }

    @Override
    public void copierPhotos(FileInputStream fichierACopier, FileOutputStream fichierDestination) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = fichierACopier.getChannel();
            out = fichierDestination.getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void afficheMetadataExif() {
        if (exif != null) {
            Iterator<?> directories = exif.getDirectoryIterator();
            while (directories.hasNext()) {
                Directory directory = (Directory) directories.next();
                Iterator<?> tags = directory.getTagIterator();
                while (tags.hasNext()) {
                    Tag tag = (Tag) tags.next();
                    System.out.println(tag);
                }
            }
        }
    }

    /**
     * Renvoie le nombre d'occurrences de la sous-chaine de caract�res sp�cifi�e dans la chaine de caract�res sp�cifi�e
     * @param text chaine de caract�res initiale
     * @param string sous-chaine de caract�res dont le nombre d'occurrences doit etre compt�
     * @return le nombre d'occurrences du pattern sp�cifi� dans la chaine de caract�res sp�cifi�e
     */
    public static final int stringOccur(String text, String string) {
        return regexOccur(text, Pattern.quote(string));
    }

    /**
     * Renvoie le nombre d'occurrences du pattern sp�cifi� dans la chaine de caract�res sp�cifi�e
     * @param text chaine de caract�res initiale
     * @param regex expression r�guli�re dont le nombre d'occurrences doit etre compt�
     * @return le nombre d'occurrences du pattern sp�cifi� dans la chaine de caract�res sp�cifi�e
     */
    public static final int regexOccur(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int occur = 0;
        while (matcher.find()) {
            occur++;
        }
        return occur;
    }
}
