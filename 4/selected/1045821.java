package com.loribel.commons.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.commons.io.CopyUtils;
import com.loribel.commons.abstraction.ENCODING;
import com.loribel.commons.util.FTools;
import com.loribel.commons.util.GB_FileToolsI;
import com.loribel.commons.util.Log;

/**
 * Tools for File.
 *
 * Somme methods use NIO.
 *
 * @author Gregory Borelli
 */
public class GB_FileToolsImpl implements GB_FileToolsI {

    /**
     * Copy a file to an other file.
     * <p>
     * Use NIO to copy the file.
     *
     * @param a_fileSrc File - the source file
     * @param a_fileDest File - the destination file
     * @param a_append boolean - true to append content
     */
    public void copyFile(File a_fileSrc, File a_fileDest, boolean a_append) throws IOException {
        a_fileDest.getParentFile().mkdirs();
        FileInputStream in = null;
        FileOutputStream out = null;
        FileChannel fcin = null;
        FileChannel fcout = null;
        try {
            in = new FileInputStream(a_fileSrc);
            out = new FileOutputStream(a_fileDest, a_append);
            fcin = in.getChannel();
            fcout = out.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(16 * 1024);
            while (true) {
                buffer.clear();
                int r = fcin.read(buffer);
                if (r == -1) {
                    break;
                }
                buffer.flip();
                fcout.write(buffer);
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (fcin != null) {
                fcin.close();
            }
            if (fcout != null) {
                fcout.close();
            }
        }
    }

    public void copyInputStreamToFile(InputStream a_inputStream, File a_fileDest) throws IOException {
        if (a_inputStream == null) {
            throw new NullPointerException("InputStream cannot be null!");
        }
        a_fileDest.getParentFile().mkdirs();
        InputStream in = a_inputStream;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(a_fileDest, false);
            while (true) {
                int r = in.read();
                if (r == -1) {
                    break;
                }
                out.write(r);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    public void extractResourceToFile(Class a_loader, String a_pathName, File a_file) throws IOException {
        InputStream l_inputStream = null;
        FileOutputStream l_outputStream = null;
        try {
            l_inputStream = a_loader.getResourceAsStream(a_pathName);
            a_file.getParentFile().mkdirs();
            if (l_inputStream == null) {
                throw new FileNotFoundException("Resource " + a_loader.getName() + "." + a_pathName + " not found!");
            }
            l_outputStream = new FileOutputStream(a_file);
            CopyUtils.copy(l_inputStream, l_outputStream);
        } catch (IOException ex) {
            if (l_inputStream != null) {
                l_inputStream.close();
            }
            if (l_outputStream != null) {
                l_outputStream.close();
            }
            throw ex;
        }
    }

    /**
     * Reads a file.
     *
     * @param a_file File - the source file
     *
     * @return String
     */
    public String readFile(File a_file) throws IOException {
        return readFile(a_file, null);
    }

    public String readFile(File a_file, String a_encoding) throws IOException {
        long l_ts = System.currentTimeMillis();
        if (a_encoding == null) {
            a_encoding = ENCODING.DEFAULT;
        }
        Reader l_reader = new InputStreamReader(new FileInputStream(a_file), a_encoding);
        String retour = readReader(l_reader);
        Log.debugTs(this, "Read file (" + a_encoding + ") " + a_file, l_ts);
        return retour;
    }

    public String readReader(Reader a_reader) throws IOException {
        StringBuffer l_stringBuffer = new StringBuffer();
        try {
            int c = a_reader.read();
            while (c != -1) {
                l_stringBuffer.append((char) c);
                c = a_reader.read();
            }
        } finally {
            a_reader.close();
        }
        String retour = l_stringBuffer.toString();
        return retour;
    }

    /**
     * Reads a resource file.
     *
     * Attention, si vous utiliser un .jar, les fileName sont caseSensitive,
     * et vous ne pouvez referencer un fichier avec ../maRessource.txt
     * Si a_loader est null, alors on utilise readFile(new File(a_pathName)).
     *
     * @param a_loader Class -
     * @param a_pathName String -
     *
     * @return String
     */
    public String readResource(Class a_loader, String a_pathName) throws IOException {
        return readResource(a_loader, a_pathName, null);
    }

    /**
     * Reads a resource file.
     *
     * Attention, si vous utiliser un .jar, les fileName sont caseSensitive,
     * et vous ne pouvez referencer un fichier avec ../maRessource.txt
     * Si a_loader est null, alors on utilise readFile(new File(a_pathName)).
     *
     * @param a_loader Class -
     * @param a_pathName String -
     *
     * @return String
     */
    public String readResource(Class a_loader, String a_pathName, String a_encoding) throws IOException {
        long l_ts = System.currentTimeMillis();
        if (a_encoding == null) {
            a_encoding = ENCODING.DEFAULT;
        }
        if (a_loader == null) {
            return readFile(new File(a_pathName), a_encoding);
        }
        InputStream l_inputStream = a_loader.getResourceAsStream(a_pathName);
        if (l_inputStream == null) {
            throw new FileNotFoundException(a_pathName + " not found !");
        }
        Reader l_reader = new InputStreamReader(l_inputStream, a_encoding);
        String retour = readReader(l_reader);
        l_inputStream.close();
        Log.debugTs(this, "Read resource (" + a_encoding + ") " + a_pathName, l_ts);
        return retour;
    }

    /**
     * Renames a file.
     * Create the destination directory if needed.
     * Returns true if rename is made with success.
     * Returns false if nothing is made.
     *
     * TODO rename file if equals case sensitive
     */
    public boolean renameFile(File a_file, File a_newFile) throws IOException {
        String l_path1 = a_file.getAbsolutePath();
        String l_path2 = a_newFile.getAbsolutePath();
        if (l_path1.equals(l_path2)) {
            return false;
        }
        if (l_path1.equalsIgnoreCase(l_path2)) {
            return false;
        }
        File l_dir = a_newFile.getParentFile();
        l_dir.mkdirs();
        a_newFile.delete();
        boolean r = a_file.renameTo(a_newFile);
        System.out.println("renameFile: " + a_file + "->" + a_newFile + " [retour=" + r + "]");
        if (!r) {
            throw new IOException("Cannot rename " + a_file + " to " + a_newFile);
        }
        return true;
    }

    /**
     * Writes a file with a String.
     * Si a_append == true, alors le contenu est ajoute e la fin du fichier,
     * sinon le contenu remplace l'ancien contenu.
     * Remarque : on se charge de la creation du directory si besoin.
     *
     * @param a_file File -
     * @param a_content String -
     * @param a_append boolean - true to append content
     */
    public void writeFile(File a_file, String a_content, boolean a_append) throws IOException {
        writeFile(a_file, a_content, a_append, null);
    }

    /**
     * Writes a file with a String.
     * Si a_append == true, alors le contenu est ajoute ala fin du fichier,
     * sinon le contenu remplace l'ancien contenu.
     * Remarque : on se charge de la creation du directory si besoin.
     *
     * @param a_file File -
     * @param a_content String -
     * @param a_append boolean - true to append content
     */
    public void writeFile(File a_file, String a_content, boolean a_append, String a_encoding) throws IOException {
        if (a_content == null) {
            Log.debug(this, "Delete file (content is null) : " + a_file);
            throw new IOException("writeFile error - content=null : " + a_file.getAbsolutePath());
        }
        long l_ts = System.currentTimeMillis();
        if (a_encoding == null) {
            a_encoding = ENCODING.DEFAULT;
        }
        File l_path = new File(a_file.getParent());
        l_path.mkdirs();
        Writer l_fileWriter = FTools.newFileWriter(a_file, a_encoding, a_append);
        l_fileWriter.write(a_content);
        l_fileWriter.flush();
        l_fileWriter.close();
        Log.debugTs(this, "Write file (" + a_encoding + ") " + a_file, l_ts);
    }
}
