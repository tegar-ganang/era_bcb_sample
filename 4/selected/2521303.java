package com.freeture.frmwk.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipManager {

    private static final String ZIP_EXTENSION = ".zip";

    private static final int DEFAULT_LEVEL_COMPRESSION = Deflater.BEST_COMPRESSION;

    /**
     * Remplace l'extension si le fichier cible ne fini pas par '.zip'
     * @param source
     * @param target
     * @return
     * @throws IOException
     */
    private static File getZipTypeFile(final File source, final File target) throws IOException {
        if (target.getName().toLowerCase().endsWith(ZIP_EXTENSION)) return target;
        final String tName = target.isDirectory() ? source.getName() : target.getName();
        final int index = tName.lastIndexOf('.');
        return new File(new StringBuilder(target.isDirectory() ? target.getCanonicalPath() : target.getParentFile().getCanonicalPath()).append(File.separatorChar).append(index < 0 ? tName : tName.substring(0, index)).append(ZIP_EXTENSION).toString());
    }

    /**
     * Compresse un fichier
     * @param out Flux compresser
     * @param parentFolder Dossier/fichier � compresser
     * @param file 
     * @throws IOException
     */
    private static final void compressFile(final ZipOutputStream out, final String parentFolder, final File file) throws IOException {
        final String zipName = new StringBuilder(parentFolder).append(file.getName()).append(file.isDirectory() ? '/' : "").toString();
        final ZipEntry entry = new ZipEntry(zipName);
        entry.setSize(file.length());
        entry.setTime(file.lastModified());
        out.putNextEntry(entry);
        if (file.isDirectory()) {
            for (final File f : file.listFiles()) compressFile(out, zipName.toString(), f);
            return;
        }
        final InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            final byte[] buf = new byte[8192];
            int bytesRead;
            while (-1 != (bytesRead = in.read(buf))) out.write(buf, 0, bytesRead);
        } finally {
            in.close();
        }
    }

    /**
	 * Compresse un fichier � l'adresse point�e par le fichier cible.
	 * Remplace le fichier cible s'il existe d�j�.
	 * @param file Fichier Zip
	 * @param target Repertoire de destination
	 * @param compressionLevel niveau de compression
	 * @throws IOException
	 */
    public static void compress(final File file, final File target, final int compressionLevel) throws IOException {
        final File source = file.getCanonicalFile();
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(getZipTypeFile(source, target.getCanonicalFile())));
        out.setMethod(ZipOutputStream.DEFLATED);
        out.setLevel(compressionLevel);
        compressFile(out, "", source);
        out.close();
    }

    /**
	 * Compresse un fichier � l'adresse point�e par le fichier cible.
	 * Remplace le fichier cible s'il existe d�j�.
	 * @param file Fichier Zip
	 * @param compressionLevel niveau de compression
	 * @throws IOException
	 */
    public static void compress(final File file, final int compressionLevel) throws IOException {
        compress(file, file, compressionLevel);
    }

    /**
     * Compresse un fichier
     * @param fileName Chemin du fichier � compresser
     * @param targetName Reperttoire de destination
     * @throws IOException
     */
    public static void compress(final File file, final File target) throws IOException {
        compress(file, target, DEFAULT_LEVEL_COMPRESSION);
    }

    /**
     * Compresse un fichier
     * @param fileName Chemin du fichier � compresser; le fichier zip portera le meme nom.
     * @throws IOException
     */
    public static void compress(final File file) throws IOException {
        compress(file, file, DEFAULT_LEVEL_COMPRESSION);
    }

    /**
     * Compresse un fichier
     * @param fileName Chemin du fichier � compresser
     * @param targetName Reperttoire de destination
     * @param compressionLevel Niveau de compression
     * @throws IOException
     */
    public static void compress(final String fileName, final String targetName, final int compressionLevel) throws IOException {
        compress(new File(fileName), new File(targetName), compressionLevel);
    }

    /**
     * Compression d'un fichier avec comme fichier zip le meme nom
     * @param fileName Chemin du fichier � compresser
     * @param compressionLevel Niveau de compression
     * @throws IOException
     */
    public static void compress(final String fileName, final int compressionLevel) throws IOException {
        compress(new File(fileName), new File(fileName), compressionLevel);
    }

    /**
     * Compresse un fichier avec le niveau de compression par defaut (Deflater.BEST_COMPRESSION)
     * @param fileName Chemin du fichier � compresser
     * @param targetName Reperttoire de destination
     * @throws IOException
     */
    public static void compress(final String fileName, final String targetName) throws IOException {
        compress(new File(fileName), new File(targetName), DEFAULT_LEVEL_COMPRESSION);
    }

    /**
     * Compresse un fichier avec le niveau de compression par defaut (Deflater.BEST_COMPRESSION) et meme nom pour le Zip
     * @param fileName Chemin du fichier � compresser
     * @throws IOException
     */
    public static void compress(final String fileName) throws IOException {
        compress(new File(fileName), new File(fileName), DEFAULT_LEVEL_COMPRESSION);
    }

    /**
	 * D�compresse un fichier zip dans le dossier sp�cifi�
	 *@param Fichier ZIP
	 *@param Repertoire cible
	 *@param Suprression du fichier � la fin de la decompression 
     * @throws IOException
	 */
    public static void decompress(final File file, final File folder, final boolean deleteZipAfter) throws IOException {
        final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(file.getCanonicalFile())));
        ZipEntry ze;
        try {
            while (null != (ze = zis.getNextEntry())) {
                final File f = new File(folder.getCanonicalPath(), ze.getName());
                if (f.exists()) f.delete();
                if (ze.isDirectory()) {
                    f.mkdirs();
                    continue;
                }
                f.getParentFile().mkdirs();
                final OutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
                try {
                    try {
                        final byte[] buf = new byte[8192];
                        int bytesRead;
                        while (-1 != (bytesRead = zis.read(buf))) fos.write(buf, 0, bytesRead);
                    } finally {
                        fos.close();
                    }
                } catch (final IOException ioe) {
                    f.delete();
                    throw ioe;
                }
            }
        } finally {
            zis.close();
        }
        if (deleteZipAfter) file.delete();
    }

    /**
	 * D�compresse un fichier zip dans le dossier sp�cifi�
	 *@param Fichier ZIP
	 *@param Repertoire cible
	 *@param Suprression du fichier � la fin de la decompression 
     * @throws IOException
	 */
    public static void decompress(final String fileName, final String folderName, final boolean deleteZipAfter) throws IOException {
        decompress(new File(fileName), new File(folderName), deleteZipAfter);
    }

    /**
	 * D�compresse un fichier zip dans le dossier sp�cifi�
	 *@param Fichier ZIP
	 *@param Repertoire cible
     * @throws IOException
	 */
    public static void decompress(final String fileName, final String folderName) throws IOException {
        decompress(new File(fileName), new File(folderName), false);
    }

    /**
	 * D�compresse un fichier zip dans le dossier sp�cifi�
	 *@param Fichier ZIP est decompresser dans le repertoire courrant du zip
	 *@param Suprression du fichier � la fin de la decompression 
     * @throws IOException
	 */
    public static void decompress(final File file, final boolean deleteZipAfter) throws IOException {
        decompress(file, file.getCanonicalFile().getParentFile(), deleteZipAfter);
    }

    /**
	 * D�compresse un fichier zip dans le dossier sp�cifi�
	 *@param Fichier ZIP est decompresser dans le repertoire courrant du zip
	 *@param Suprression du fichier � la fin de la decompression 
     * @throws IOException
	 */
    public static void decompress(final String fileName, final boolean deleteZipAfter) throws IOException {
        decompress(new File(fileName), deleteZipAfter);
    }

    /**
	 * D�compresse un fichier zip dans le dossier sp�cifi�
	 *@param Fichier ZIP est decompresser dans le repertoire courrant du zip
     * @throws IOException
	 */
    public static void decompress(final File file) throws IOException {
        decompress(file, file.getCanonicalFile().getParentFile(), false);
    }

    /**
	 * D�compresse un fichier zip dans le dossier sp�cifi�
	 *@param Fichier ZIP est decompresser dans le repertoire courrant du zip
     * @throws IOException
	 */
    public static void decompress(final String fileName) throws IOException {
        decompress(new File(fileName));
    }

    public static void main(String[] args) throws IOException {
        ZipManager.compress("C:\\test.txt", "C:\\", Deflater.BEST_SPEED);
        ZipManager.decompress("C:\\test.zip", ".", false);
        ZipManager.compress("C:\\test.txt", "C:\\test2.zip", Deflater.BEST_COMPRESSION);
    }
}
