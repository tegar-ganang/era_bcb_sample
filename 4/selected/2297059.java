package org.gmoting.exif;

import static com.drew.imaging.jpeg.JpegMetadataReader.readMetadata;
import static org.gmoting.util.Util.EXT_JPG_MAY;
import static org.gmoting.util.Util.EXT_JPG_MIN;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;

public class Main {

    private static final String fichero = Messages.getString("main.fichero.entrada");

    private static final String directorio = Messages.getString("main.directorio.entrada");

    private static final String directorioSalida = Messages.getString("main.fichero.salida");

    private static final FileFilter fileFilter = new FileFilter() {

        public boolean accept(File file) {
            return file.getName().endsWith(EXT_JPG_MAY) || file.getName().endsWith(EXT_JPG_MIN);
        }
    };

    public static void main(String[] args) {
        try {
            BufferedWriter ficheroSalida = new BufferedWriter(new FileWriter(Messages.getString("main.fichero.salida")));
            File dir = new File(directorio);
            File files[] = dir.listFiles(fileFilter);
            for (File file : files) {
                if (file.isFile()) {
                    ficheroSalida.write(readEXIFMetadaExtractor(file).toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Leer EXIF con Metadata Extractor
	 * 
	 * @param filename
	 *            Ruta de la imagen en formato JPG
	 * @return Etiquetas exif
	 * @throws IOException
	 * @throws JpegProcessingException
	 * @throws MetadataException
	 */
    @SuppressWarnings("unchecked")
    public static StringBuilder readEXIFMetadaExtractor(File file) throws IOException, JpegProcessingException, MetadataException {
        StringBuilder imageTags = new StringBuilder();
        imageTags.append(file.getAbsolutePath() + "\n");
        Metadata metadata = readMetadata(file);
        Iterator directories = metadata.getDirectoryIterator();
        while (directories.hasNext()) {
            Directory directory = (Directory) directories.next();
            Iterator tags = directory.getTagIterator();
            while (tags.hasNext()) {
                Tag tag = (Tag) tags.next();
                imageTags.append(tag + "\n");
                System.out.println(tag);
            }
        }
        imageTags.append("\n\n");
        return imageTags;
    }
}
