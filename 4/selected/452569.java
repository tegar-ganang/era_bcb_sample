package net.sourceforge.processdash;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.FileSet;

public class PackageLaunchProfile extends Task {

    private String profilename;

    private String profileid;

    private String profileversion;

    private String requireserver;

    private File destfile;

    private String signingprefix;

    private List<FileSet> filesets = new ArrayList<FileSet>();

    public void setProfilename(String profilename) {
        this.profilename = profilename;
    }

    public void setProfileid(String profileid) {
        this.profileid = profileid;
    }

    public void setProfileversion(String profileversion) {
        this.profileversion = profileversion;
    }

    public void setRequireserver(String requireserver) {
        this.requireserver = requireserver;
    }

    public void setDestfile(File destfile) {
        this.destfile = destfile;
    }

    public void setSigningprefix(String signingprefix) {
        this.signingprefix = signingprefix;
    }

    public void addFileset(FileSet fs) {
        filesets.add(fs);
    }

    @Override
    public void execute() throws BuildException {
        validate();
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("packageLaunchProfile", ".tmp");
            createProfile(tmpFile);
        } catch (BuildException be) {
            throw be;
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            deleteTempFile(tmpFile);
        }
    }

    private void validate() throws BuildException {
        if (!hasValue(profilename)) throw new BuildException("profilename not specified");
        if (!hasValue(profileid)) throw new BuildException("profileid not specified");
        if (!hasValue(profileversion)) throw new BuildException("profileversion not specified");
        if (destfile == null) throw new BuildException("destfile not specified");
    }

    private boolean hasValue(String s) {
        return (s != null && s.length() != 0);
    }

    private void createProfile(File tmpFile) throws Exception {
        createResourceJar(tmpFile);
        signResourceJar(tmpFile);
        packageDestZip(tmpFile);
    }

    private void createResourceJar(File tmpFile) throws IOException, ManifestException {
        String contentToken = calculateContentToken();
        Manifest mf = buildManifest(contentToken);
        Jar jar = new Jar();
        jar.bindToOwner(this);
        jar.addConfiguredManifest(mf);
        for (FileSet fs : filesets) jar.addFileset(fs);
        tmpFile.delete();
        jar.setDestFile(tmpFile);
        jar.execute();
    }

    private String calculateContentToken() throws IOException {
        List<File> files = new ArrayList<File>();
        for (FileSet fs : filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            for (String name : ds.getIncludedFiles()) files.add(new File(ds.getBasedir(), name));
        }
        if (files.isEmpty()) throw new BuildException("You must designate at least one file " + "to include in the launch profile.");
        Collections.sort(files, FILENAME_SORTER);
        Checksum ck = new Adler32();
        for (File f : files) calcChecksum(f, ck);
        return Long.toString(Math.abs(ck.getValue()), Character.MAX_RADIX);
    }

    private Manifest buildManifest(String contentToken) throws ManifestException {
        Manifest mf = Manifest.getDefaultManifest();
        addAttribute(mf, DISTR_FORMAT_ATTR, "1.0");
        addAttribute(mf, DISTR_NAME_ATTR, profilename);
        addAttribute(mf, DISTR_ID_ATTR, profileid);
        addAttribute(mf, DISTR_VERSION_ATTR, profileversion);
        addAttribute(mf, DISTR_TOKEN_ATTR, contentToken);
        if (hasValue(requireserver)) addAttribute(mf, DISTR_REQUIRES_ATTR, requireserver);
        return mf;
    }

    private void addAttribute(Manifest mf, String name, String value) throws ManifestException {
        if (value != null) mf.addConfiguredAttribute(new Manifest.Attribute(name, value));
    }

    private void signResourceJar(File tmpFile) {
        MaybeSign sign = new MaybeSign();
        sign.bindToOwner(this);
        sign.setPrefix(signingprefix);
        sign.setJar(tmpFile);
        sign.execute();
    }

    private void packageDestZip(File tmpFile) throws FileNotFoundException, IOException {
        log("Creating launch profile package " + destfile);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destfile)));
        ZipEntry e = new ZipEntry(RESOURCE_JAR_FILENAME);
        e.setMethod(ZipEntry.STORED);
        e.setSize(tmpFile.length());
        e.setCompressedSize(tmpFile.length());
        e.setCrc(calcChecksum(tmpFile, new CRC32()));
        out.putNextEntry(e);
        InputStream in = new BufferedInputStream(new FileInputStream(tmpFile));
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.closeEntry();
        out.finish();
        out.close();
    }

    private long calcChecksum(File f, Checksum cksum) throws IOException {
        CheckedInputStream in = new CheckedInputStream(new BufferedInputStream(new FileInputStream(f)), cksum);
        while (in.read() != -1) ;
        in.close();
        return cksum.getValue();
    }

    private void deleteTempFile(File tmpFile) {
        if (tmpFile == null) return;
        try {
            FileOutputStream out = new FileOutputStream(tmpFile);
            out.write(1);
            out.close();
        } catch (IOException ioe) {
        }
        tmpFile.delete();
    }

    private static class FilenameSorter implements Comparator<File> {

        public int compare(File f1, File f2) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    private static final FilenameSorter FILENAME_SORTER = new FilenameSorter();

    private static final String DISTR_PREFIX = "Dash-Launch-Profile-";

    private static final String DISTR_NAME_ATTR = DISTR_PREFIX + "Name";

    private static final String DISTR_ID_ATTR = DISTR_PREFIX + "ID";

    private static final String DISTR_VERSION_ATTR = DISTR_PREFIX + "Version";

    private static final String DISTR_TOKEN_ATTR = DISTR_PREFIX + "Content-Token";

    private static final String DISTR_FORMAT_ATTR = DISTR_PREFIX + "Format";

    private static final String DISTR_REQUIRES_ATTR = DISTR_PREFIX + "Requires-Server";

    private static final String RESOURCE_JAR_FILENAME = "pdash-launch-resources.jar";
}
