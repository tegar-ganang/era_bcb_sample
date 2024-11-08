package org.fudaa.ctulu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import com.memoire.fu.Fu;
import com.memoire.fu.FuLib;
import com.memoire.fu.FuLog;
import java.io.BufferedOutputStream;
import java.net.URI;
import java.util.LinkedList;

/**
 * Classe utilitaire pour les fichiers.
 * 
 * @author Fred Deniger
 * @version $Id: CtuluLibFile.java,v 1.31 2007-06-20 12:20:45 deniger Exp $
 */
public final class CtuluLibFile {

    public static File createTempDir() throws IOException {
        return createTempDir("fudaaTmp", null);
    }

    public static File createTempDirSafe() {
        try {
            return createTempDir("fudaaTmp", null);
        } catch (final IOException _evt) {
            FuLog.error(_evt);
        }
        return null;
    }

    /**
   * @param _init Le fichier initiale
   * @param _newExt la nouvelle extension sans point
   * @return le fichier avec la bonne extension
   */
    public static File changeExtension(final File _init, final String _newExt) {
        if (_init == null || _newExt == null) {
            return null;
        }
        if (_init.getParentFile() == null) {
            return new File(CtuluLibFile.getFileName(CtuluLibFile.getSansExtension(_init.getName()), _newExt));
        }
        return new File(_init.getParentFile(), CtuluLibFile.getFileName(CtuluLibFile.getSansExtension(_init.getName()), _newExt));
    }

    public static String changeExtension(final String _fileName, final String _newExt) {
        if (_fileName == null || _newExt == null) {
            return null;
        }
        return getFileName(getSansExtension(_fileName), _newExt);
    }

    /**
   * @param _zip le fichier zip a analyser
   * @return null si pas de fichier ou si exception
   */
    public static String[] getEntries(final File _zip) {
        final ArrayList r = new ArrayList();
        ZipFile zf = null;
        try {
            zf = new ZipFile(_zip);
            for (final Enumeration entries = zf.entries(); entries.hasMoreElements(); ) {
                r.add(((ZipEntry) entries.nextElement()).getName());
            }
        } catch (final IOException e) {
            if (Fu.DEBUG && FuLog.isDebug()) {
                FuLog.error(e);
            }
            return null;
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (final IOException e) {
                    if (Fu.DEBUG && FuLog.isDebug()) {
                        FuLog.error(e);
                    }
                }
            }
        }
        return CtuluLibString.enTableau(r);
    }

    /**
   * @param _filesToZip les fichiers a zipper
   * @param _zipFile le zip cibles: les fichiers seront zippe � plat.
   * @return true si succes.
   */
    public static boolean zip(final File[] _filesToZip, final File _zipFile) {
        if (_filesToZip == null || _zipFile == null || _filesToZip.length == 0 || canWrite(_zipFile) != null) {
            return false;
        }
        ZipOutputStream out = null;
        boolean ok = true;
        try {
            out = new ZipOutputStream(new FileOutputStream(_zipFile));
            for (int i = 0; i < _filesToZip.length; i++) {
                FileInputStream in = null;
                try {
                    in = new FileInputStream(_filesToZip[i]);
                    out.putNextEntry(new ZipEntry(_filesToZip[i].getName()));
                    if (!copyStream(in, out, false, false)) {
                        return false;
                    }
                    out.closeEntry();
                } catch (final IOException e) {
                    _zipFile.delete();
                    if (Fu.DEBUG && FuLog.isDebug()) {
                        FuLog.error(e);
                    }
                    ok = false;
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
        } catch (final IOException e) {
            _zipFile.delete();
            if (Fu.DEBUG && FuLog.isDebug()) {
                FuLog.error(e);
            }
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (final IOException e) {
                _zipFile.delete();
                if (Fu.DEBUG && FuLog.isDebug()) {
                    FuLog.error(e);
                }
                ok = false;
            }
        }
        return ok;
    }

    /**
   * Zippe un r�pertoire et ses fichiers/sous r�pertoires vers un fichier zip.
   * @param _dirToZip Le repertoire contenant les fichiers
   * @param _zipFile Le fichier zip.
   * @param _prog L'interface de progression. Peut etre <tt>null</tt>.
   * @return True si le fichier zip a �t� correctement cr��.
   */
    public static void zip(final File _dirToZip, final File _zipFile, ProgressionInterface _prog) throws IOException {
        ZipOutputStream zos = null;
        try {
            int nbFiles = 0;
            if (_prog != null) {
                _prog.setProgression(0);
                LinkedList<File> list = new LinkedList<File>();
                list.add(_dirToZip);
                while (!list.isEmpty()) {
                    File dir = list.removeLast();
                    nbFiles++;
                    for (File file : dir.listFiles()) {
                        if (file.isDirectory()) {
                            list.add(file);
                        } else {
                            nbFiles++;
                        }
                    }
                }
                _prog.setProgression(10);
            }
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(_zipFile)));
            int indFile = 0;
            zipRecurs(_dirToZip, _dirToZip, zos, _prog, nbFiles, indFile);
        } finally {
            if (zos != null) {
                zos.close();
            }
            if (_prog != null) _prog.setProgression(100);
        }
    }

    /**
   * Zippe un r�pertoire de facon r�cursive.
   */
    private static int zipRecurs(final File _rootDir, final File _dir, final ZipOutputStream _zos, ProgressionInterface _prog, int nbFiles, int indFile) throws IOException {
        URI root = _rootDir.toURI();
        for (File f : _dir.listFiles()) {
            String name = root.relativize(f.toURI()).getPath();
            if (f.isDirectory()) {
                _zos.putNextEntry(new ZipEntry(name.endsWith("/") ? name : name + "/"));
                _zos.closeEntry();
                indFile = zipRecurs(_rootDir, f, _zos, _prog, nbFiles, indFile);
            } else {
                _zos.putNextEntry(new ZipEntry(name));
                FileInputStream finp = new FileInputStream(f);
                copyStream(finp, _zos, true, false);
                _zos.closeEntry();
            }
            if (_prog != null) {
                indFile++;
                _prog.setProgression((int) ((indFile / (double) nbFiles) * 85) + 10);
            }
        }
        return indFile;
    }

    /**
   * Dezippe un fichier zip dans un r�pertoire donn�
   * @param _zipFile Le fichier � d�zipper
   * @param _dirToUnzip Le r�pertoire de d�zippage
   * @param _prog L'interface de progression. Peut etre <tt>null</tt>.
   */
    public static void unzip(final File _zipFile, final File _dirToUnzip, ProgressionInterface _prog) throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(_zipFile);
            int nbFiles = 0;
            int indFile = 0;
            if (_prog != null) {
                _prog.setProgression(0);
                nbFiles = zf.size();
            }
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (entry.isDirectory()) {
                    new File(_dirToUnzip, entry.getName()).mkdirs();
                } else {
                    new File(_dirToUnzip, entry.getName()).getParentFile().mkdirs();
                    FileOutputStream fout = new FileOutputStream(new File(_dirToUnzip, entry.getName()));
                    copyStream(zf.getInputStream(entry), fout, false, true);
                }
                if (_prog != null) {
                    indFile++;
                    _prog.setProgression((int) ((indFile / (double) nbFiles) * 85) + 10);
                }
            }
        } finally {
            if (zf != null) {
                zf.close();
            }
            if (_prog != null) _prog.setProgression(100);
        }
    }

    public static boolean deleteDir(final File _f) {
        if (_f == null) {
            return false;
        }
        final File[] files = _f.listFiles();
        if (files != null) {
            for (int i = files.length - 1; i >= 0; i--) {
                if (files[i].isFile()) {
                    try {
                        files[i].delete();
                    } catch (final RuntimeException _evt) {
                        FuLog.error(_evt);
                    }
                } else {
                    deleteDir(files[i]);
                }
            }
        }
        return _f.delete();
    }

    public static File createTempDir(final String _prefix) throws IOException {
        return createTempDir(_prefix, null);
    }

    public static File createTempDir(final String _prefix, final File _dir) throws IOException {
        final File tempFile = File.createTempFile(_prefix, CtuluLibString.EMPTY_STRING, _dir);
        if (!tempFile.delete()) {
            throw new IOException();
        }
        if (!tempFile.mkdir()) {
            throw new IOException();
        }
        return tempFile;
    }

    /**
   * @param _r le reader a fermer
   * @return l'exception levee lors de la fermeture. ou null si aucune.
   */
    public static IOException close(final Reader _r) {
        if (_r == null) {
            return null;
        }
        try {
            _r.close();
        } catch (final IOException e) {
            return e;
        }
        return null;
    }

    public static IOException close(final OutputStream _r) {
        if (_r == null) {
            return null;
        }
        try {
            _r.close();
        } catch (final IOException e) {
            return e;
        }
        return null;
    }

    public static IOException close(final InputStream _r) {
        if (_r == null) {
            return null;
        }
        try {
            _r.close();
        } catch (final IOException e) {
            return e;
        }
        return null;
    }

    /**
   * @param _r le tableau de lecteurs a fermer
   * @return le tableau des exceptions levee ou null si aucune
   */
    public static IOException[] close(final Reader[] _r) {
        IOException[] envoie = null;
        boolean exceptionLauch = false;
        if (_r != null) {
            final int l = _r.length;
            IOException e;
            envoie = new IOException[l];
            for (int i = 0; i < l; i++) {
                e = close(_r[i]);
                if (e != null) {
                    exceptionLauch = true;
                    envoie[i] = e;
                }
            }
        }
        if (exceptionLauch) {
            return envoie;
        }
        return null;
    }

    /**
   * @param _r le writer a fermer
   * @return l'exception levee lors de la fermeture (ou null si aucune).
   */
    public static IOException close(final Writer _r) {
        if (_r == null) {
            return null;
        }
        try {
            _r.close();
        } catch (final IOException e) {
            return e;
        }
        return null;
    }

    public static boolean copyFileLineByLine(final File _fileFrom, final File _fileTo) {
        return copyFileLineByLine(_fileFrom, _fileTo, false);
    }

    public static boolean copyFileLineByLine(final File _fileFrom, final File _fileTo, final boolean _append) {
        boolean ok = true;
        try {
            final LineNumberReader in = new LineNumberReader(new FileReader(_fileFrom));
            final FileWriter out = new FileWriter(_fileTo, _append);
            try {
                String line = null;
                final String sep = CtuluLibString.LINE_SEP;
                while ((line = in.readLine()) != null) {
                    out.write(line + sep);
                }
                out.flush();
            } finally {
                IOException io = close(in);
                if (io != null) {
                    ok = false;
                }
                io = close(out);
                if (io != null) {
                    ok = false;
                }
            }
        } catch (final IOException e1) {
            ok = false;
        }
        return ok;
    }

    public static boolean copyFileStream(final File _fileFrom, final File _fileTo) {
        return copyFileStream(_fileFrom, _fileTo, false);
    }

    /**
   * Copie avec des FileInputStream,FileOutputStream.
   * 
   * @param _fileFrom le fichier source
   * @param _fileTo le fichier de destination
   */
    public static boolean copyFileStream(final File _fileFrom, final File _fileTo, final boolean _append) {
        boolean r = true;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(_fileFrom);
            out = new FileOutputStream(_fileTo, _append);
            return copyStream(in, out, false, false);
        } catch (final IOException _e1) {
            r = false;
        } finally {
            IOException io = null;
            if (in != null) {
                io = close(in);
                if (io != null) {
                    r = false;
                }
            }
            if (out != null) {
                io = close(out);
                if (io != null) {
                    r = false;
                }
            }
        }
        return r;
    }

    /**
   * Copie le flux _in dans le flux _out et ferme les flux si demand�.
   * 
   * @param _in le flux entrant
   * @param _out le flux sortant
   * @param _closeIn si true le flux d'entree sera ferme
   * @param _closeOut si true le flux de sortie sera ferme
   * @return true si la copie a eu lieu
   */
    public static boolean copyStream(final InputStream _in, final OutputStream _out, final boolean _closeIn, final boolean _closeOut) {
        if (_in == null || _out == null) {
            if (_closeIn && _in != null) {
                close(_in);
            }
            if (_closeOut && _out != null) {
                close(_out);
            }
            return false;
        }
        boolean r = true;
        try {
            try {
                int doneCnt = -1;
                final int bufSize = 32768;
                final byte[] buf = new byte[bufSize];
                while ((doneCnt = _in.read(buf, 0, bufSize)) >= 0) {
                    if (doneCnt == 0) {
                        Thread.yield();
                    } else {
                        _out.write(buf, 0, doneCnt);
                    }
                }
                _out.flush();
            } finally {
                IOException io = _closeIn ? close(_in) : null;
                if (io != null) {
                    r = false;
                }
                io = _closeOut ? close(_out) : null;
                if (io != null) {
                    r = false;
                }
            }
        } catch (final IOException _e1) {
            FuLog.error(_e1);
            return false;
        }
        return r;
    }

    /**
   * Copie les fichiers en chargeant tout en memoire: attention aux fichiers importants.
   * 
   * @param _fileFrom le fichier a copier
   * @param _fileTo le fichier de destination
   * @return si operation r�ussie
   */
    public static boolean copyFileChannel(final File _fileFrom, final File _fileTo) {
        return copyFileChannel(_fileFrom, _fileTo, false);
    }

    public static boolean copyFileChannel(final File _fileFrom, final File _fileTo, final boolean _append) {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            srcChannel = new FileInputStream(_fileFrom).getChannel();
            dstChannel = new FileOutputStream(_fileTo, _append).getChannel();
            if (_append) {
                dstChannel.transferFrom(srcChannel, dstChannel.size(), srcChannel.size());
            } else {
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            }
            srcChannel.close();
            dstChannel.close();
        } catch (final IOException e) {
            return false;
        } finally {
            try {
                if (srcChannel != null) {
                    srcChannel.close();
                }
            } catch (final IOException _evt) {
                FuLog.error(_evt);
            }
            try {
                if (dstChannel != null) {
                    dstChannel.close();
                }
            } catch (final IOException _evt) {
                FuLog.error(_evt);
            }
        }
        return true;
    }

    /**
   * @param _fileFrom le fichier a copier
   * @param _fileTo le fichier de destination
   * @return true si copie reussie
   */
    public static boolean copyFile(final File _fileFrom, final File _fileTo) {
        return copyFile(_fileFrom, _fileTo, false);
    }

    public static boolean appendFile(final File _fileFrom, final File _fileTo) {
        return copyFile(_fileFrom, _fileTo, true);
    }

    public static boolean copyFile(final File _fileFrom, final File _fileTo, final boolean _append) {
        if (_fileFrom.length() > 20971520L) {
            return copyFileStream(_fileFrom, _fileTo, _append);
        }
        return copyFileChannel(_fileFrom, _fileTo, _append);
    }

    /**
   * @param _from le fichier a deplacer
   * @param _to le fichier destination
   * @return true si ok
   */
    public boolean move(final File _from, final File _to) {
        if (_from == null || _to == null || !_from.exists() || canWrite(_to) != null) {
            return false;
        }
        final boolean ok = _from.renameTo(_to);
        if (!ok) {
            final boolean r = copyFile(_from, _to);
            _from.delete();
            return r;
        }
        return true;
    }

    public static Charset getUTF8Charset() {
        return Charset.forName("UTF-8");
    }

    /**
   * Si _path est absolu renvoie new File(_path) sinon renvoie new File(_baseDir,_path).
   * 
   * @return null si un argument est nul.
   * @param _baseDir le repertoire de base
   * @param _path le path a convertir
   */
    public static File getAbsolutePath(final File _baseDir, final String _path) {
        if ((_path == null) || (_baseDir == null)) {
            return null;
        }
        final File f = new File(_path);
        if (f.isAbsolute()) {
            return f;
        }
        return CtuluLibFile.getConanicalPathFile(new File(_baseDir, _path));
    }

    /**
   * Si _path est absolu renvoie new File(_path) sinon renvoie new File(_baseDir,_path).
   * 
   * @return null si un argument est nul.
   * @param _baseDir le repertoire de base
   * @param _path le path a convertir
   */
    public static File getAbsolutePath(final String _baseDir, final String _path) {
        final File f = new File(_path);
        if (f.isAbsolute()) {
            return f;
        }
        return new File(_baseDir, _path);
    }

    /**
   * G�n�re un fichier absolu au chemin de fichier/r�pertoire principal pour le fichier donn�.
   * 
   * @param _main Le fichier/r�pertoire principal.
   * @param _file Le fichier pour lequel on recherche un chemin absolu se basant sur le fichier/r�pertoire principal.
   * @return Le fichier absolu obtenu.
   */
    public static File getAbsolutePathnameTo(final File _main, final File _file) {
        return getAbsolutePath(_main.isDirectory() ? _main : _main.getParentFile(), _file.getPath());
    }

    /**
   * Renvoie le chemin canonique du fichier _f. Si une exception est levee renvoie le chemin absolu.
   * 
   * @param _f le path canonique
   * @return une valeur non nulle
   * @see java.io.File#getCanonicalFile()
   * @see java.io.File#getAbsoluteFile()
   */
    public static String getCanonicalPath(final File _f) {
        String path = null;
        try {
            path = _f.getCanonicalPath();
        } catch (final IOException e) {
            path = _f.getAbsolutePath();
        }
        return path;
    }

    /**
   * @param _f Le Fichier dont on cherche le chemin canonique.
   * @return le fichier avec un chemin canonique
   */
    public static File getConanicalPathFile(final File _f) {
        File path = null;
        try {
            path = _f.getCanonicalFile();
        } catch (final IOException e) {
            path = _f.getAbsoluteFile();
        }
        return path;
    }

    /**
   * Utilise des StringBuffer pour construire le nom du fichier a partir de l'extension.
   * 
   * @param _nameSansExt le nom du fichier sans extension
   * @param _ext l'extension a ajouter
   * @return le nom du fichier complet.
   */
    public static String getFileName(final String _nameSansExt, final String _ext) {
        return _nameSansExt + getDot() + _ext;
    }

    private static String getDot() {
        return CtuluLibString.DOT;
    }

    public static File getFile(final String _nameSansExt, final String _ext) {
        return new File(getFileName(_nameSansExt, _ext));
    }

    /**
   * Perment de d�terminer le chemin relatif de _destFile par rapport a _baseDir. L'entier _nbParentTest permet de
   * limiter les recherches : maximum _nbParentTest repertoire parent teste ( soit maximum _nbParentTest fois "../").
   * 
   * @param _destFile le fichier a traiter
   * @param _baseDir le repertoire de base
   * @param _nbParentTest le nombre de dossier parent a parcourir
   * @return le chemin relatif.
   */
    public static String getRelativeFile(final File _destFile, final File _baseDir, final int _nbParentTest) {
        if (_destFile == null) {
            return null;
        } else if (_destFile.getParentFile().equals(_baseDir)) {
            return _destFile.getName();
        } else if ((_baseDir == null) || (_destFile.getParentFile() == null)) {
            return _destFile.getAbsolutePath();
        }
        final String sFile = _destFile.getAbsolutePath();
        String parentPath = _baseDir.getAbsolutePath();
        if (sFile.startsWith(parentPath)) {
            final String r = sFile.substring(parentPath.length());
            if (r.startsWith(File.separator)) {
                return r.substring(1);
            }
            return r;
        }
        File parent = _baseDir.getParentFile();
        if (parent != null) {
            int nb = 1;
            final int nbToTest = _nbParentTest < 0 ? Integer.MAX_VALUE : _nbParentTest;
            while ((nb <= nbToTest) && (parent != null)) {
                parentPath = parent.getAbsolutePath();
                if (sFile.startsWith(parentPath)) {
                    final StringBuffer r = new StringBuffer();
                    String append = ".." + File.separator;
                    for (int i = nb; i > 0; i--) {
                        r.append(append);
                    }
                    append = sFile.substring(parentPath.length());
                    if (append.startsWith(File.separator)) {
                        append = append.substring(1);
                    }
                    r.append(append);
                    return r.toString();
                }
                nb++;
                parent = parent.getParentFile();
            }
        }
        return _destFile.getAbsolutePath();
    }

    /**
   * G�n�re un chemin relatif au chemin de fichier principal pour le fichier donn�. Si les 2 fichiers sont sur des
   * disques diff�rents (Windows), le chemin relatif est en fait un chemin absolu.
   * 
   * <pre>
   *                                            Exemple : _main : c:\\refonde\\cercle\\rect.prf
   *                                            _file : c:\\users\\dupont\\geom.geo
   *                                            return : ..\\..\\users\\dupont\\geom.geo
   * </pre>
   * 
   * @param _main Le fichier principal.
   * @param _file Le fichier pour lequel on recherche un chemin relatif au fichier principal.
   * @return Le fichier relatif.
   */
    public static File getRelativePathnameTo(final File _main, final File _file) {
        final File main = _main.getAbsoluteFile();
        final File file = _file.getAbsoluteFile();
        final List vdmain = new ArrayList();
        final List vdfile = new ArrayList();
        File ftmp;
        String name;
        ftmp = main;
        while (ftmp.getParent() != null) {
            name = ftmp.getName();
            vdmain.add(0, name);
            ftmp = ftmp.getParentFile();
        }
        name = ftmp.getPath().substring(0, ftmp.getPath().length() - 1);
        vdmain.add(0, name);
        ftmp = file;
        while (ftmp.getParent() != null) {
            name = ftmp.getName();
            vdfile.add(0, name);
            ftmp = ftmp.getParentFile();
        }
        name = ftmp.getPath().substring(0, ftmp.getPath().length() - 1);
        vdfile.add(0, name);
        int i = 0;
        final StringBuffer path = new StringBuffer();
        while (i < vdmain.size() - 1 && i < vdfile.size() - 1 && new File((String) vdmain.get(i)).compareTo(new File((String) vdfile.get(i))) == 0) {
            path.append(((String) vdmain.get(i))).append(File.separator);
            i++;
        }
        final StringBuffer rela = new StringBuffer(50);
        if (i != 0) {
            for (int j = i; j < vdmain.size() - 1; j++) {
                rela.append("..").append(File.separator);
            }
        }
        for (int j = i; j < vdfile.size() - 1; j++) {
            rela.append((String) vdfile.get(j)).append(File.separator);
        }
        rela.append((String) vdfile.get(vdfile.size() - 1));
        return new File(rela.toString());
    }

    /**
   * @param _fName nom du fichier concernr
   * @return la chaine otee de l'extension (soit la chaine avant le dernier point). Si aucun point de trouve, renvoie la
   *         chaine intiale.
   */
    public static String getSansExtension(final String _fName) {
        if (_fName == null) {
            return null;
        }
        final int index = _fName.lastIndexOf('.');
        if (index < 0) {
            return _fName;
        }
        return _fName.substring(0, index);
    }

    /**
   * Renvoie l'extension du fichier.<br>
   * <code>'fichier.jpg'</code> renverra <code>'jpg'</code>.<br>
   * <code>'fichier'</code> renverra <code>null</code>.
   * 
   * @param _fName le nom du fichier
   * @return null si pas d'extension sinon l'extension sans point
   */
    public static String getExtension(final String _fName) {
        if (_fName == null) {
            return null;
        }
        final int index = _fName.lastIndexOf('.');
        if (index < 0 || index == _fName.length() - 1) {
            return null;
        }
        return _fName.substring(index + 1);
    }

    /**
   * Supprime l'extension d'un nom de fichier.
   * @param _f Le fichier
   * @return Le fichier, sans extension si elle existe ou le fichier d'origine si aucune extension.
   */
    public static File getSansExtension(final File _f) {
        return new File(_f.getParent(), getSansExtension(_f.getName()));
    }

    public static boolean containsExtension(final String _fName) {
        return _fName.indexOf('.') >= 0;
    }

    /**
   * @param _file le fichier a tester
   * @return true si non null et existe
   */
    public static boolean exists(final File _file) {
        return _file != null && _file.exists();
    }

    /**
   * Exemple appendExtension(new File(),"txt").
   * 
   * @param _f le fichier
   * @param _ext l'extension sans point a ajouter si necessaire: si le nom du fichier ne contient pas de '.'
   * @return _f ou _f+'.ext'
   */
    public static File appendExtensionIfNeeded(final File _f, final String _ext) {
        if (_f == null || _ext == null) {
            return null;
        }
        if (containsExtension(_f.getName())) {
            return _f;
        }
        return new File(_f.getAbsolutePath() + getDot() + _ext);
    }

    /**
   * @param _f le fichier a tester
   * @param _ext l'extension sans point
   * @return null si _f ou _ext est null. Le fichier finissant par .ext
   */
    public static File appendStrictExtensionIfNeeded(final File _f, final String _ext) {
        if (_f == null || _ext == null) {
            return null;
        }
        final String ext = getDot() + _ext;
        if (_f.getName().endsWith(ext)) {
            return _f;
        }
        return new File(_f.getAbsolutePath() + ext);
    }

    /**
   * @param _ext l'extension a verifier
   * @return _ext sans le point au debut
   */
    public static String getCorrectExtension(final String _ext) {
        if (_ext == null) {
            return null;
        }
        String res = _ext.trim();
        if (res.startsWith(getDot())) {
            res = res.substring(1);
        }
        return res;
    }

    public static String[] getCorrectExtension(final String[] _ext) {
        if (_ext == null) {
            return null;
        }
        final String[] res = new String[_ext.length];
        for (int i = res.length - 1; i >= 0; i--) {
            res[i] = getCorrectExtension(_ext[i]);
        }
        return res;
    }

    /**
   * @see #replaceAndCopyFile(File, File, String, String, String, String, String)
   * @param _fileFrom le fichier source
   * @param _fileTo le fichier de dest
   * @param _oldToken le motif a remplacer
   * @param _newToken le motif qui le remplacera
   * @return true si operation reussie
   */
    public static boolean replaceAndCopyFile(final File _fileFrom, final File _fileTo, final String _oldToken, final String _newToken) {
        return replaceAndCopyFile(_fileFrom, _fileTo, _oldToken, _newToken, null, null, null);
    }

    /**
   * @see #replaceAndCopyFile(File, File, String, String, String, String, String)
   * @param _fileFrom le fichier source
   * @param _fileTo le fichier de dest
   * @param _oldToken le motif a remplacer
   * @param _newToken le motif qui le remplacera
   * @param _encoding l'encodage a utiliser (peut etre null)
   * @return true si operation reussie
   */
    public static boolean replaceAndCopyFile(final File _fileFrom, final File _fileTo, final String _oldToken, final String _newToken, final String _encoding) {
        return replaceAndCopyFile(_fileFrom, _fileTo, _oldToken, _newToken, null, null, _encoding);
    }

    /**
   * @see #replaceAndCopyFile(File, File, String, String, String, String, String)
   * @param _fileFrom le fichier source
   * @param _fileTo le fichier de dest
   * @param _oldToken le motif a remplacer
   * @param _newToken le motif qui le remplacera
   * @param _oldToken1 le motif 1 a remplacer
   * @param _newToken1 le motif 1 qui le remplacera
   * @return true si operation reussie
   */
    public static boolean replaceAndCopyFile(final File _fileFrom, final File _fileTo, final String _oldToken, final String _newToken, final String _oldToken1, final String _newToken1) {
        return replaceAndCopyFile(_fileFrom, _fileTo, _oldToken, _newToken, _oldToken1, _newToken1, null);
    }

    public static boolean replaceAndCopyFile(final File _fileFrom, final File _fileTo, final String _oldToken, final String _newToken, final String _oldToken1, final String _newToken1, final String _encoding) {
        try {
            final Reader r = _encoding == null ? new FileReader(_fileFrom) : new InputStreamReader(new FileInputStream(_fileFrom), _encoding);
            return replaceAndCopyFile(r, _fileTo, _oldToken, _newToken, _oldToken1, _newToken1, _encoding);
        } catch (final FileNotFoundException e) {
            return false;
        } catch (final UnsupportedEncodingException e) {
            return false;
        }
    }

    /**
   * A utiliser pour de petits fichiers: le fichier est copie dans un buffer. Commence par remplacer _oldToken par
   * _newToken puis fait de meme pour 1.
   * 
   * @param _fileFrom le fichier source
   * @param _fileTo le fichier de dest
   * @param _oldToken le motif a remplacer
   * @param _newToken le motif qui le remplacera
   * @param _oldToken1 le motif 1 a remplacer
   * @param _newToken1 le motif 1 qui le remplacera
   * @param _encoding l'encodage du fichier
   * @return true si operation reussie
   */
    public static boolean replaceAndCopyFile(final Reader _fileFrom, final File _fileTo, final String _oldToken, final String _newToken, final String _oldToken1, final String _newToken1, final String _encoding) {
        boolean r = true;
        BufferedReader reader = null;
        Writer writer = null;
        try {
            reader = new BufferedReader(_fileFrom);
            writer = new BufferedWriter(_encoding == null ? new FileWriter(_fileTo) : new OutputStreamWriter(new FileOutputStream(_fileTo), _encoding));
            final StringBuffer b = new StringBuffer(8192);
            int lu;
            while ((lu = reader.read()) != -1) {
                b.append((char) lu);
            }
            String n = FuLib.replace(b.toString(), _oldToken, _newToken);
            if ((_oldToken1 != null) && (_newToken1 != null)) {
                n = FuLib.replace(n, _oldToken1, _newToken1);
            }
            writer.write(n);
            writer.flush();
        } catch (final FileNotFoundException io) {
            FuLog.error("file not found " + io.getMessage());
            r = false;
        } catch (final UnsupportedEncodingException io) {
            FuLog.error("encode not supported " + io.getMessage());
            r = false;
        } catch (final IOException io) {
            FuLog.error("i/o error " + io.getMessage());
            r = false;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (final IOException _io) {
            }
        }
        return r;
    }

    /**
   * A utiliser pour de petits fichiers: le fichier est copie dans un buffer. Commence par remplacer _oldToken par
   * _newToken puis fait de meme pour 1.
   * 
   * @param _fileFrom le fichier source
   * @param _fileTo le fichier de dest
   * @param _oldToken le motif a remplacer
   * @param _newToken le motif qui le remplacera
   * @param _oldToken1 le motif 1 a remplacer
   * @param _newToken1 le motif 1 qui le remplacera
   * @param _encoding l'encodage du fichier
   * @return true si operation reussie
   */
    public static boolean replaceAndCopyFile(final Reader _fileFrom, final File _fileTo, Map<String, String> oldNew, final String _encoding) {
        boolean r = true;
        BufferedReader reader = null;
        Writer writer = null;
        try {
            reader = new BufferedReader(_fileFrom);
            writer = new BufferedWriter(_encoding == null ? new FileWriter(_fileTo) : new OutputStreamWriter(new FileOutputStream(_fileTo), _encoding));
            final StringBuffer b = new StringBuffer(8192);
            int lu;
            while ((lu = reader.read()) != -1) {
                b.append((char) lu);
            }
            String n = b.toString();
            for (Map.Entry<String, String> entry : oldNew.entrySet()) {
                n = FuLib.replace(n, entry.getKey(), entry.getValue());
            }
            writer.write(n);
            writer.flush();
        } catch (final FileNotFoundException io) {
            FuLog.error("file not found " + io.getMessage());
            r = false;
        } catch (final UnsupportedEncodingException io) {
            FuLog.error("encode not supported " + io.getMessage());
            r = false;
        } catch (final IOException io) {
            FuLog.error("i/o error " + io.getMessage());
            r = false;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (final IOException _io) {
            }
        }
        return r;
    }

    /**
   * prive car utilitaire.
   */
    private CtuluLibFile() {
        super();
    }

    /**
   * Teste si on peut ecrire le fichier _f. S'il existe on utilise seulement la methode canWrite.Si le fichier n'existe
   * pas on teste si on peut ecrire dans le repertoire parent.
   * 
   * @param _f le fichier a tester
   * @return null si le fichier peut etre ecrit. Explication sinon _f est null.
   */
    public static String canWrite(final File _f) {
        if (_f == null) {
            return CtuluLib.getS("Le fichier n'est pas correct (null)");
        }
        if (_f.exists()) {
            if (!_f.canWrite()) return CtuluLib.getS("Le fichier '{0}' existe d�j� et est prot�g�", _f.getName());
        } else if (_f.getParentFile() == null) {
            return CtuluLib.getS("Le fichier n'est pas correct (null)");
        } else if (!_f.getParentFile().canWrite()) {
            if (FuLib.isWindows()) {
                try {
                    if (_f.createNewFile() && exists(_f)) {
                        _f.delete();
                        return null;
                    }
                } catch (final IOException e) {
                }
            }
            return CtuluLib.getS("Le r�pertoire parent '{0}' est prot�g� en �criture", _f.getParentFile().getName());
        }
        return null;
    }

    public static boolean isWritable(final File _f) {
        if (_f == null) {
            return false;
        }
        if (_f.exists() && !_f.canWrite()) {
            return false;
        } else if (_f.getParentFile() == null) {
            return false;
        } else if (!_f.getParentFile().canWrite()) {
            if (FuLib.isWindows()) {
                try {
                    final File f = File.createTempFile("testFudaaWindows", ".tmp", _f.getParentFile());
                    if (exists(f)) {
                        f.delete();
                        return true;
                    }
                } catch (final IOException e) {
                }
            }
            return false;
        }
        return true;
    }

    /**
   * Lit un fichier et le place dans un tableau de byte.
   * 
   * @param _fichier Le fichier � lire.
   * @throws IOException Les exceptions lev�es par les instructions FileInputStream(File) skip, read et close.
   * @return byte[] Le tableau d'octet du fichier, peut �tre null en cas de probl�me.
   */
    public static byte[] litFichier(final File _fichier) throws IOException {
        return litFichier(_fichier, _fichier.length(), false);
    }

    /**
   * Lit un fichier texte et le place dans une chaine.
   * 
   * @param _fichier Le fichier � lire.
   * @throws IOException Les exceptions lev�es par les instructions FileInputStream(File) skip, read et close.
   * @return String La chaine de caract�re repr�sentant le contenu du fichier, peut �tre null en cas de probl�me.
   */
    public static String litFichierTexte(final File _fichier) throws IOException {
        return litFichierTexte(_fichier, _fichier.length(), false);
    }

    /**
   * Lit un fichier texte et le place dans une chaine de caract�res.
   * 
   * @param _fichier Le fichier � lire.
   * @param _tailleMax Taille maximum du fichier en Kilo Octet.
   * @param _lectureFin Si vrai, on lit toujours la fin du fichier lorsqu'on d�passe le taille maximum.
   * @param _affiche Si Vrai, affiche quelques messages sur la sortie standard.
   * @throws IOException Les exceptions lev�es par les instructions FileInputStream(File) skip, read et close.
   * @return String La chaine de caract�re repr�sentant le contenu du fichier, peut �tre null en cas de probl�me.
   */
    public static String litFichierTexte(final File _fichier, final double _tailleMax, final boolean _lectureFin, final boolean _affiche) throws IOException {
        final byte[] res = litFichier(_fichier, _tailleMax, _lectureFin, _affiche);
        if (res == null) {
            return null;
        }
        return new String(res);
    }

    /**
   * Lit un fichier texte et le place dans une chaine en affichant quelques messages sur la sortie standard.
   * 
   * @param _fichier Le fichier � lire.
   * @param _tailleMax Taille maximum du fichier en Kilo Octet.
   * @param _lectureFin Si vrai, on lit toujours la fin du fichier lorsqu'on d�passe le taille maximum.
   * @throws IOException Les exceptions lev�es par les instructions FileInputStream(File) skip, read et close.
   * @return String La chaine de caract�re repr�sentant le contenu du fichier, peut �tre null en cas de probl�me.
   */
    public static String litFichierTexte(final File _fichier, final double _tailleMax, final boolean _lectureFin) throws IOException {
        final byte[] res = litFichier(_fichier, _tailleMax, _lectureFin);
        if (res == null) {
            return null;
        }
        return new String(res);
    }

    /**
   * Lit un fichier et le place dans un tableau de byte en affichant quelques messages sur la sortie standard.
   * 
   * @param _fichier Le fichier � lire.
   * @param _tailleMax Taille maximum du fichier en Kilo Octet.
   * @param _lectureFin Si vrai, on lit toujours la fin du fichier lorsqu'on d�passe le taille maximum.
   * @throws IOException Les exceptions lev�es par les instructions FileInputStream(File) skip, read et close.
   * @return byte[] Le tableau d'octet du fichier, peut �tre null en cas de probl�me.
   */
    public static byte[] litFichier(final File _fichier, final double _tailleMax, final boolean _lectureFin) throws IOException {
        return litFichier(_fichier, _tailleMax, _lectureFin, true);
    }

    /**
   * Lit un fichier et le place dans un tableau de byte.
   * 
   * @param _fichier Le fichier � lire.
   * @param _tailleMax Taille maximum du fichier en Kilo Octet.
   * @param _lectureFin Si vrai, on lit toujours la fin du fichier lorsqu'on d�passe le taille maximum.
   * @param _affiche Si Vrai, affiche quelques messages sur la sortie standard.
   * @throws IOException Les exceptions lev�es par les instructions FileInputStream(File) skip, read et close.
   * @return byte[] Le tableau d'octet du fichier, peut �tre null en cas de probl�me.
   */
    public static byte[] litFichier(final File _fichier, final double _tailleMax, final boolean _lectureFin, final boolean _affiche) throws IOException {
        FileInputStream f = null;
        byte[] buffer = null;
        try {
            if (_affiche) {
                FuLog.all("Lecture de " + _fichier.getName());
            }
            f = new FileInputStream(_fichier);
            final long tailleMaxOctet = Math.round(_tailleMax * 1024);
            final long tailleFichier = _fichier.length();
            if (tailleFichier > tailleMaxOctet) {
                if (_lectureFin) {
                    if (_affiche) {
                        FuLog.all("Lecture de la fin de " + _fichier.getName());
                    }
                    buffer = new byte[(int) tailleMaxOctet];
                    f.skip(tailleFichier - tailleMaxOctet);
                    f.read(buffer);
                    f.close();
                    return buffer;
                }
                if (_affiche) {
                    FuLog.all("Fichier " + _fichier.getName() + " trop volumineux : " + (tailleFichier / 1024) + "Ko");
                }
                return null;
            }
            if (_fichier.canRead()) {
                buffer = new byte[(int) _fichier.length()];
                f.read(buffer, 0, (int) _fichier.length());
                return buffer;
            }
        } catch (final FileNotFoundException _evt) {
            FuLog.error(_evt);
        } catch (final IOException _evt) {
            FuLog.error(_evt);
        } finally {
            try {
                if (f != null) {
                    f.close();
                }
            } catch (final IOException _evt) {
                FuLog.error(_evt);
            }
        }
        return null;
    }
}
