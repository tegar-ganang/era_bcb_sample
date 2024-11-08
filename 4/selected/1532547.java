package com.elitost.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Classe utilitaire de zip/unzip de fichiers. Attention : certaines parties
 * sont en commentaire : zipDir existe en deux exemplaires : le premier permet
 * de zipper un dossier dans un flux zipp�. Le deuxi�me (comment�) permet de
 * zipper un dossier dans un fichier zip (on a pas acc�s au stream)
 * 
 * Cette classe est utilis�e pour le param�trage. Le zip est construit � la
 * main, en pla�ant les dossiers dans le flux zipp� Par contre, sur le client,
 * le zip est d�zipp� de mani�re automatique.
 * 
 * @author sebayle
 * 
 */
public class ZipUtils {

    /**
         * @param currentDir :
         *                r�pertoire courant (� l'int�rieur du zip)
         * @param dir2zip :
         *                r�pertoire r�el � zipper
         * @param zos :
         *                stream � remplir
         * @throws IOException
         * @throws FileNotFoundException
         */
    public static void zipDir(String currentDir, String dir2zip, ZipOutputStream zos) throws IOException, FileNotFoundException {
        File zipDir = new File(dir2zip);
        File f = null;
        String[] dirList = zipDir.list();
        if (dirList == null) {
            zipFichier(zipDir, currentDir, zos);
        } else {
            for (int i = 0; i < dirList.length; i++) {
                f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(currentDir + "/" + f.getName(), filePath, zos);
                    continue;
                }
                zipFichier(f, currentDir, zos);
            }
        }
    }

    /** Taille du buffer pour les lectures/�critures */
    private static final int BUFFER_SIZE = 8 * 1024;

    /**
         * D�compresse un GZIP contenant un fichier unique.
         * 
         * Efface le filedest avant de commencer.
         * 
         * @param gzsource
         *                Fichier GZIP � d�compresser
         * @param filedest
         *                Nom du fichier destination o� sera sauvegard� le
         *                fichier contenu dans le GZIP.
         * @throws FileNotFoundException
         *                 si le fichier GZip n'existe pas
         * @throws IOException
         * @see http://javaalmanac.com/egs/java.util.zip/UncompressFile.html?l=rel
         */
    public static void gunzip(String gzsource, String filedest) throws FileNotFoundException, IOException {
        GZIPInputStream in = new GZIPInputStream(new FileInputStream(gzsource));
        try {
            BufferedInputStream bis = new BufferedInputStream(in);
            try {
                OutputStream out = new FileOutputStream(filedest);
                try {
                    BufferedOutputStream bos = new BufferedOutputStream(out);
                    try {
                        byte[] buf = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = bis.read(buf, 0, BUFFER_SIZE)) != -1) {
                            bos.write(buf, 0, len);
                        }
                        buf = null;
                    } finally {
                        bos.close();
                    }
                } finally {
                    out.close();
                }
            } finally {
                bis.close();
            }
        } finally {
            in.close();
        }
    }

    /**
         * Compresse un fichier dans un GZIP.
         * 
         * Efface le filedest avant de commencer.
         * 
         * @param filesource
         *                Fichier � compresser
         * @param gzdest
         *                Fichier GZIP cible
         * @throws FileNotFoundException
         *                 si le fichier source n'existe pas ou si le GZIP
         *                 n'existe pas apr�s la compression
         * @throws IOException
         * @see http://javaalmanac.com/egs/java.util.zip/CompressFile.html?l=rel
         * @see http://java.developpez.com/livres/penserenjava/?chap=12&page=3
         */
    public static void gzip(String filesource, String gzdest) throws FileNotFoundException, IOException {
        GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzdest));
        try {
            BufferedOutputStream bos = new BufferedOutputStream(out);
            try {
                FileInputStream in = new FileInputStream(filesource);
                try {
                    BufferedInputStream bis = new BufferedInputStream(in);
                    try {
                        byte[] buf = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = bis.read(buf, 0, BUFFER_SIZE)) > 0) {
                            bos.write(buf, 0, len);
                        }
                        buf = null;
                    } finally {
                        bis.close();
                    }
                } finally {
                    in.close();
                }
            } finally {
                bos.close();
            }
        } finally {
            out.close();
        }
        if (!new File(gzdest).exists()) throw new FileNotFoundException("Le fichier " + gzdest + " n'a pas �t� cr��");
    }

    /**
         * D�compresse l'archive Zip dans un r�pertoire. R�utilise les noms des
         * r�pertoires lors de la d�compression.
         * 
         * @param zipsrc
         * @param basedirdest
         * @throws FileNotFoundException
         * @throws IOException
         * @throws SecurityException
         */
    public static long unzipToDir(String zipsrc, String basedirdest) throws FileNotFoundException, IOException, SecurityException {
        return unzipToDir(new FileInputStream(zipsrc), basedirdest);
    }

    /**
         * D�compresse le flux <tt>InputStream</tt> Zip dans un r�pertoire.
         * R�utilise les noms des r�pertoires lors de la d�compression.
         * 
         * @param zipsrc
         * @param basedirdest
         * @throws SecurityException
         * @throws IOException
         */
    public static long unzipToDir(InputStream inzip, String basedirdest) throws IOException, SecurityException {
        long sum = 0;
        File base = new File(basedirdest);
        if (!base.exists()) base.mkdirs();
        try {
            CheckedInputStream checksum = new CheckedInputStream(inzip, new Adler32());
            try {
                BufferedInputStream bis = new BufferedInputStream(checksum);
                try {
                    ZipInputStream zis = new ZipInputStream(bis);
                    try {
                        ZipEntry entry;
                        File f;
                        int count;
                        byte[] buf = new byte[BUFFER_SIZE];
                        BufferedOutputStream bos;
                        FileOutputStream fos;
                        while ((entry = zis.getNextEntry()) != null) {
                            f = new File(basedirdest, entry.getName());
                            if (entry.isDirectory()) f.mkdirs(); else {
                                int l = entry.getName().lastIndexOf('/');
                                if (l != -1) {
                                    new File(basedirdest, entry.getName().substring(0, l)).mkdirs();
                                }
                                fos = new FileOutputStream(f);
                                try {
                                    bos = new BufferedOutputStream(fos, BUFFER_SIZE);
                                    try {
                                        while ((count = zis.read(buf, 0, BUFFER_SIZE)) != -1) {
                                            bos.write(buf, 0, count);
                                        }
                                    } finally {
                                        bos.close();
                                    }
                                } finally {
                                    fos.close();
                                }
                            }
                            if (entry.getTime() != -1) {
                                f.setLastModified(entry.getTime());
                            }
                        }
                    } finally {
                        zis.close();
                    }
                } finally {
                    bis.close();
                }
            } finally {
                checksum.close();
            }
            sum = checksum.getChecksum().getValue();
        } finally {
            inzip.close();
        }
        return sum;
    }

    /**
     * Zippe un fichier pass� en param�tre. Si le fichier est introuvable ou n'est pas un fichier mais un r�pertoire, lance une exception.
     * @param f le fichier � zipper.
     * @param currentDir le r�pertoire o� placer le fichier dans le zip.
     * @param zos le zip.
     * @throws IOException Si le fichier est introuvable ou illisible ou est un r�pertoire.
     */
    private static void zipFichier(File f, String currentDir, ZipOutputStream zos) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        String path;
        if (currentDir.equals("")) path = f.getName(); else path = currentDir + "/" + f.getName();
        ZipEntry anEntry = new ZipEntry(path);
        zos.putNextEntry(anEntry);
        while ((bytesIn = fis.read(readBuffer)) != -1) {
            zos.write(readBuffer, 0, bytesIn);
        }
        fis.close();
    }
}
