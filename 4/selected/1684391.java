package edu.mit.osidimpl.provider.installer.firstcut;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.CheckedOutputStream;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;

/**
 *  <p>
 *  Manages software installations under a root directory providing add,
 *  lookup, and delete methods. Each installation is represented by an
 *  Installation object that contains a set of properties.
 *  </p><p>
 *  CVS $Id: Installer.java,v 1.11 2007/02/17 00:57:34 jeffkahn Exp $
 *  </p>
 *  
 *  @author  Tom Coppeto
 *  @version $Revision: 1.11 $
 */
public class Installer extends Object {

    private final String DATA_FILE = ".InstallationData";

    private File root;

    /**
     *  Constructs a new <code>Installer</code>.
     *
     *  @param root the name of the root installation directory
     *  @throws InstallerException if there is a problem with the root
     *          installation directory
     *  @throws NullPointerException if root is null
     */
    public Installer(String root) throws InstallerException {
        if (root == null) {
            throw new NullPointerException("no root specified");
        }
        this.root = new File(root);
        if (this.root.getPath().length() < 5) {
            throw new InstallerException(root + "  should be a longer path");
        }
        if (this.root.exists() == true) {
            if (!this.root.isDirectory()) {
                throw new InstallerException(root + " is not a directory");
            }
        } else {
            if (this.root.mkdirs() == false) {
                throw new InstallerException("cannot create " + root);
            }
        }
    }

    /**
     *  Gets the installation specified by name & version.
     *
     *  @param name the name of the installation package
     *  @param version the version of the installation package expressed
     *         numerically (1.2.3)
     *  @return Installation the installation package corresponding to the
     *          given name and version
     *  @throws InstallerNotFoundException if there is no package matching the
     *          name and version
     *  @throws InstallerException cannot load installation data file
     *  @throws NullPointerException if name or version is null
     */
    public Installation getInstallation(String name, String version) throws InstallerException {
        File inst = getInstDir(name, version);
        if (inst == null) {
            throw new InstallerNotFoundException(name + " " + version + " not found");
        }
        FileInputStream fis;
        try {
            fis = new FileInputStream(inst + File.separator + DATA_FILE);
        } catch (FileNotFoundException fnfe) {
            throw new InstallerException(name + " " + version + " not found");
        }
        FileChannel channel = fis.getChannel();
        FileLock lock;
        if ((name == null) || (version == null)) {
            throw new NullPointerException("name or version is null");
        }
        try {
            lock = channel.lock(0, inst.length(), true);
        } catch (Exception e) {
            try {
                fis.close();
            } catch (Exception e2) {
            }
            throw new InstallerException(inst.getPath() + " is locked");
        }
        Properties p = new Properties();
        File propFile = new File(inst, DATA_FILE);
        if (propFile.exists() == false) {
            try {
                lock.release();
                fis.close();
            } catch (Exception e) {
            }
            throw new InstallerException(DATA_FILE + "does not exist in " + inst.getPath() + " - try re-installing");
        }
        try {
            p.load(new FileInputStream(propFile));
        } catch (Exception e) {
            try {
                lock.release();
                fis.close();
            } catch (Exception e2) {
            }
            throw new InstallerException("unable to load " + propFile.getPath(), e);
        }
        String id = p.getProperty("providerId");
        if (id == null) {
            throw new InstallerException("no providerId found");
        }
        try {
            lock.release();
            fis.close();
        } catch (Exception e) {
        }
        return (new Installation(name, version, id, inst.getPath(), p));
    }

    /**
     *  Gets all the installation packages.
     *
     *  @return array of installation packages
     *  @throws InstallerException another error occurred retrieving 
     *          package
     */
    public Installation[] getInstallations() {
        ArrayList<Installation> al = new ArrayList<Installation>();
        File[] pkgs = this.root.listFiles();
        Arrays.sort(pkgs, new nameComparator());
        for (File pkg : pkgs) {
            if (!pkg.isDirectory() || !pkg.canRead()) {
                continue;
            }
            File[] versions = pkg.listFiles();
            Arrays.sort(versions, new versionComparator());
            for (File version : versions) {
                try {
                    al.add(getInstallation(pkg.getName(), version.getName()));
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return (al.toArray(new Installation[al.size()]));
    }

    /**
     *  Gets all the installation package by providerId..
     *
     *  @return installation packages
     *  @throws InstallerException another error occurred retrieving 
     *          package
     */
    public Installation getInstallationByProviderId(String providerId) {
        Installation[] installations = getInstallations();
        for (Installation installation : installations) {
            if (providerId.equals(installation.getProviderId())) {
                return (installation);
            }
        }
        return (null);
    }

    /**
     *  Gets the latest installation package identified by the given name. The
     *  latest package is the one identified with the highest version number
     *  that is readable.
     *
     *  @param name the name of the installation package
     *  @return the installation package identified with the given name at the
     *          highest version number
     *  @throws InstallerNotFoundException no package with the given name exists
     *  @throws InstallerException another error occurred retrieving 
     *          package
     *  @throws NullPointerException is name is null
     */
    public Installation getLatestInstallation(String name) throws InstallerException {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        File inst = getInstDir(name);
        if (inst == null) {
            throw new InstallerNotFoundException(name + " not found");
        }
        File[] versions = inst.listFiles();
        if (versions.length == 0) {
            throw new InstallerNotFoundException(name + " not found");
        }
        Arrays.sort(versions, new versionComparator());
        for (File version : versions) {
            try {
                return (getInstallation(name, version.getName()));
            } catch (Exception e) {
            }
        }
        throw new InstallerNotFoundException(name + " no valid install found");
    }

    /**
     *  Gets the latest installation packages. The
     *  latest package is the one identified with the highest version number
     *  that is readable.
     *
     *  @return array of the installation packages
     *  @throws InstallerException another error occurred retrieving 
     *          package
     */
    public Installation[] getLatestInstallations() {
        ArrayList<Installation> al = new ArrayList<Installation>();
        File[] pkgs = this.root.listFiles();
        Arrays.sort(pkgs, new nameComparator());
        for (File pkg : pkgs) {
            if (!pkg.isDirectory() || !pkg.canRead()) {
                continue;
            }
            File[] versions = pkg.listFiles();
            Arrays.sort(versions, new versionComparator());
            for (File version : versions) {
                try {
                    al.add(getInstallation(pkg.getName(), version.getName()));
                    break;
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return (al.toArray(new Installation[0]));
    }

    /**
     *  Adds (installs) an installation package.
     *
     *  @param name the name of the new installation package
     *  @param version the version of the new installation package
     *  @param format the format of the installation download stream
     *         expressed as a String. Currently "gzip" or "zip".
     *  @param is the InputStream pointing to the installation download
     *  @param xsum the checksum of the installation download in CRC32. A value
     *         of 0 will not check the checksum.
     *  @param properties a properties map of the installation attributes
     *  @throws InstallerExistsException an installation package already exists
     *          by this name and version number
     *  @throws InstallerException another error occurred installing package
     *  @throws NullPointerException a null argument provided
     */
    public void add(String name, String version, String providerId, String format, InputStream is, long xsum, Properties properties) throws InstallerException {
        if ((name == null) || (version == null) || (is == null) || (properties == null) || (format == null)) {
            throw new NullPointerException("null argument provided");
        }
        if ((format.equals("zip") == false) && (format.equals("gzip") == false)) {
            throw new IllegalArgumentException("unknown format " + format);
        }
        try {
            if (getInstallation(name, version) != null) {
                throw new InstallerExistsException(name + " " + version + " is already installed");
            }
        } catch (Exception e) {
        }
        File tmpDir = new File(this.root, ".downloads");
        tmpDir.deleteOnExit();
        if (tmpDir.exists() == false) {
            if (tmpDir.mkdir() == false) {
                throw new InstallerException("unable to create " + tmpDir.getPath());
            }
            if (tmpDir.isDirectory() != true) {
                throw new InstallerException(tmpDir.getPath() + " is not a directory");
            }
        }
        File tmp;
        try {
            tmp = File.createTempFile("download.", format, tmpDir);
        } catch (java.io.IOException ie) {
            throw new InstallerException("cannot open temp file");
        }
        tmp.deleteOnExit();
        BufferedOutputStream bos;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(tmp));
        } catch (Exception e) {
            throw new InstallerException("cannot open " + tmp.getPath(), e);
        }
        CheckedOutputStream cos = new CheckedOutputStream(bos, new CRC32());
        try {
            copyFile(is, cos);
            if ((xsum > 0) && (xsum != cos.getChecksum().getValue())) {
                cleanup(tmp);
                throw new InstallerException("bad checksum for " + name);
            }
        } catch (Exception e) {
            cleanup(tmp);
            throw new InstallerException("unable to write data ", e);
        } finally {
            try {
                bos.close();
                is.close();
            } catch (Exception e) {
            }
        }
        File pkg = getInstDir(name);
        if (pkg.exists() == false) {
            if (pkg.mkdir() == false) {
                throw new InstallerException("unable to create " + pkg.getPath());
            }
        } else if (pkg.isDirectory() != true) {
            throw new InstallerException(pkg.getPath() + " is not a directory");
        }
        File pkgVers = getInstDir(name, version);
        if (pkgVers.exists() == false) {
            if (pkgVers.mkdir() == false) {
                throw new InstallerException("unable to create " + pkgVers.getPath());
            }
        } else {
            cleanup(pkgVers);
        }
        try {
            if (format.equals("zip")) {
                unZip(tmp, pkgVers);
            } else if (format.equals("gzip")) {
                unGzip(tmp, pkgVers);
            }
        } catch (Exception e) {
            try {
                cleanup(pkgVers);
                cleanup(tmp);
            } catch (Exception e2) {
            }
            throw new InstallerException("unable to decompress " + tmp.getPath() + e.getMessage(), e);
        }
        cleanup(tmp);
        File propFile = new File(pkgVers, DATA_FILE);
        try {
            properties.store(new FileOutputStream(propFile), "This file contains the data associated with this installation.\nDO NOT DELETE\n\n");
        } catch (Exception e) {
            cleanup(pkgVers);
            throw new InstallerException("unable to store properties data");
        }
        return;
    }

    /**
     *  Removes an installation package.
     *
     *  @param name the name of the installation package to delete
     *  @param version the version of the installation package to delete
     *  @throws InstallerNotFoundException if no installation package
     *          by the given name and version exists
     *  @throws InstallerException if an error occured during removal
     *  @throws NullPointerException if name or version is null
     */
    public void remove(String name, String version) throws InstallerException {
        Installation installation = getInstallation(name, version);
        File file = getInstDir(name, version);
        cleanup(file);
        return;
    }

    private void unZip(File file, File dst) throws InstallerException {
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(file);
        } catch (Exception e) {
            throw new InstallerException("unable to open zip file", e);
        }
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                if (entry.getName().equals("..")) {
                    throw new InstallerException("bad directory");
                }
                File dir = new File(dst, entry.getName());
                if (dir.exists() == false) {
                    if (dir.mkdir() == false) {
                        throw new InstallerException("unable to create directory " + dir.getPath());
                    }
                }
                continue;
            }
            try {
                InputStream is = zipFile.getInputStream(entry);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(dst, entry.getName())));
                copyFile(is, bos);
                bos.close();
                is.close();
            } catch (Exception e) {
                try {
                    zipFile.close();
                } catch (Exception e2) {
                }
                throw new InstallerException("unable to unzip " + file.getPath(), e);
            }
        }
        try {
            zipFile.close();
        } catch (Exception e) {
        }
    }

    private void unGzip(File file, File dst) throws InstallerException {
        TarInputStream tin;
        try {
            tin = new TarInputStream(new GZIPInputStream(new FileInputStream(file)));
        } catch (Exception e) {
            throw new InstallerException("unable to open gzip file " + file.getPath());
        }
        try {
            TarEntry entry = tin.getNextEntry();
            while (entry != null) {
                if (entry.getName().equals("..")) {
                    throw new InstallerException("bad directory");
                }
                File nfile = new File(dst, entry.getName());
                if (entry.isDirectory()) {
                    if (nfile.exists() == false) {
                        if (nfile.mkdir() == false) {
                            throw new InstallerException("unable to create directory " + nfile.getPath());
                        }
                    }
                } else {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(nfile));
                    tin.copyEntryContents(bos);
                    try {
                        bos.close();
                    } catch (Exception e) {
                        throw new InstallerException("cannot close output stream", e);
                    }
                }
                entry = tin.getNextEntry();
            }
        } catch (Exception e) {
            throw new InstallerException("error in ungzip", e);
        } finally {
            try {
                tin.close();
            } catch (Exception e) {
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[65536];
        int count;
        while ((count = in.read(buffer)) >= 0) {
            out.write(buffer, 0, count);
        }
    }

    private void cleanup(File dir) throws InstallerException {
        if (!dir.getPath().startsWith(dir.getPath())) {
            throw new InstallerException("trying to remove a directory not under " + dir.getPath());
        }
        if (dir.isDirectory()) {
            String[] subs = dir.list();
            for (String sub : subs) {
                if (sub.equals(DATA_FILE)) {
                    File f = new File(dir.getPath(), sub);
                    f.renameTo(new File(dir.getPath(), sub.substring(1)));
                    f = new File(dir.getPath(), sub.substring(1));
                    cleanup(f);
                } else {
                    cleanup(new File(dir.getPath(), sub));
                }
            }
        }
        if (dir.delete() != true) {
            dir.deleteOnExit();
            System.out.println("unable to delete immediately " + dir.getPath());
        }
        return;
    }

    private File getInstDir(String name) {
        String nameF = name.replace(File.separatorChar, '_').replace('\\', '_');
        return (new File(this.root, nameF));
    }

    private File getInstDir(String name, String version) {
        String nameF = name.replace(File.separatorChar, '_').replace('\\', '_');
        String versionF = version.replace(File.separatorChar, '_').replace('\\', '_');
        File dir = getInstDir(nameF);
        return (new File(dir, versionF));
    }

    private class versionComparator implements java.util.Comparator {

        public int compare(Object a, Object b) {
            File af = (File) a;
            File bf = (File) b;
            return (bf.getName().compareTo(af.getName()));
        }
    }

    private class nameComparator implements java.util.Comparator {

        public int compare(Object a, Object b) {
            File af = (File) a;
            File bf = (File) b;
            return (af.getName().compareTo(bf.getName()));
        }
    }
}
