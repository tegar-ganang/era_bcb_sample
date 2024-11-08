package es.rvp.java.simpletag.core.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.google.common.base.Predicate;
import es.rvp.java.simpletag.core.filetypes.MusicFile;

/**
 * Utilidades para el tratamiento de ficheros.
 *
 * @author Rodrigo Villamil Perez
 */
public class UtilFiles {

    protected static final Logger LOGGER = Logger.getLogger(UtilFiles.class);

    private static class SingletonHolder {

        private static final UtilFiles INSTANCE = new UtilFiles();
    }

    /**
	 * Retorna la unica instancia del objeto.
	 */
    public static UtilFiles getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private UtilFiles() {
    }

    /**
	 * Aplica un predicado a todos los ficheros filtrados de un directorio.
	 *
	 * @param dir Directorio a procesar
	 * @param filter filtro aplicable a los ficheros.
	 * @param predicate Predicado a aplicar a cada fichero encontrado que cumple
	 * el filtro anterio.
	 *
	 * @see listFilesRecursive
	 *
	 * @return numero de ficheros que cumplen el filtro.
	 */
    public int listFiles(final File dir, final FileFilter filter, final Predicate<File> predicate) {
        int totalOccurs = 0;
        final File[] listFiles = dir.listFiles(filter);
        if (listFiles == null) {
            LOGGER.warn("[MusicFileReader - readRecursive]" + " Cannot access to directory " + dir.getAbsolutePath());
        } else {
            for (final File file : listFiles) {
                if (file.isFile()) {
                    predicate.apply(file);
                    totalOccurs++;
                }
            }
        }
        return totalOccurs;
    }

    /**
	 * Aplica recursivamente un predicado a todos los ficheros filtrados de
	 * un directorio.
	 *
	 * @param dir Directorio a procesar
	 * @param filter filtro aplicable a los ficheros.
	 * @param predicate Predicado a aplicar a cada fichero encontrado que cumple
	 * el filtro anterio.
	 *
	 * @see listFiles
	 *
	 * @return numero de ficheros que cumplen el filtro.
	 */
    public int listFilesRecursive(final File dir, final FileFilter filter, final Predicate<File> predicate) {
        int totalOccurs = 0;
        final File[] listFiles = dir.listFiles(filter);
        if (listFiles == null) {
            LOGGER.warn("[MusicFileReader - readRecursive]" + " Cannot access to directory " + dir.getAbsolutePath());
        } else {
            for (final File file : listFiles) {
                if (file.isDirectory()) {
                    totalOccurs += this.listFilesRecursive(file, filter, predicate);
                } else {
                    LOGGER.info("[UtilFiles - listFilesRecursive]" + " found file '" + file + "'");
                    predicate.apply(file);
                    totalOccurs++;
                }
            }
        }
        return totalOccurs;
    }

    /**
	 * Crea en el directorio temporal, un directorio con el formato:
	 *
	 * UNIX --> /tmp/data_milisegundos/ WINDOWS --> Temporal de
	 * Windows\data_milisegundos
	 *
	 * @return EL nuevo directorio creado o null si no se ha podido generar.
	 */
    public File makeTempDirectory(final String data) {
        final String pathTmp = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + data + "_" + System.currentTimeMillis() + System.getProperty("file.separator");
        final File nuevoDirectorio = new File(pathTmp);
        nuevoDirectorio.mkdir();
        return nuevoDirectorio;
    }

    /**
	 * Obtiene la extension de un fichero en minusculas a partir de su ruta.
	 */
    public String getExtension(final String path) {
        String ext = null;
        final String s = path;
        final int i = s.lastIndexOf(UtilStrings.EXT_SEPARATOR);
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
	 * Obtiene la extension de un fichero en minusculas.
	 */
    public String getExtension(final File f) {
        return this.getExtension(f.getName());
    }

    /**
	 * Copia un fichero a un directorio
	 */
    public void copyFileToDirectory(final File srcFile, final File destDir) throws IOException {
        FileUtils.copyFileToDirectory(srcFile, destDir);
    }

    /**
	 * Renombra un fichero musical
	 *
	 * @throws IOException
	 */
    public void renameFile(final MusicFile musicFile, final String newName) throws IOException {
        final String destPath = musicFile.getFile().getParent() + System.getProperty("file.separator") + newName;
        FileUtils.moveFile(musicFile.getFile(), new File(destPath));
    }
}
