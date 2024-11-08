package net.sf.buildbox.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import net.sf.buildbox.installer.bean.*;
import net.sf.buildbox.installer.platform.AbstractPlatform;
import net.sf.buildbox.strictlogging.api.*;
import org.apache.xmlbeans.XmlException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.URLInputStreamFacade;

public class BuildboxInstaller {

    private static final Catalog CAT = StrictCatalogFactory.getCatalog(Catalog.class);

    public static final String CONTENT_SUFFIX = "content.zip";

    public static final String PLATFORM_CONTENT_SUFFIX;

    private DescriptorBean descriptor;

    private final File where;

    private final File mavenRepo;

    private List<File> localRepositories = new ArrayList<File>();

    private final StrictLogger logger;

    private final Logger plexusLogger;

    private File contentFile;

    private File platformContentFile;

    static {
        final AbstractPlatform platform = AbstractPlatform.PLATFORM_SWITCH.getCurrentPlatformHandler();
        PLATFORM_CONTENT_SUFFIX = platform.CLASSIFIER + "." + platform.COMPRESSED_TYPE;
    }

    public BuildboxInstaller(StrictLogger logger, File where) {
        this.logger = logger;
        this.plexusLogger = new PlexusStrictLogger(logger);
        this.where = where;
        mavenRepo = new File(where, "maven-repo");
        localRepositories.add(new File(System.getProperty("user.home"), ".m2/repository"));
        localRepositories.add(new File("/usr/share/maven-repo"));
    }

    public void open(File descriptorFile) throws XmlException, IOException {
        final DescriptorDocument doc = DescriptorDocument.Factory.parse(descriptorFile);
        descriptor = doc.getDescriptor();
        logger.log(CAT.packageid(descriptor.getGroupId(), descriptor.getArtifactId(), descriptor.getVersion(), descriptorFile.getAbsolutePath()));
        final String prefix = descriptorFile.getParentFile().getAbsolutePath() + "/" + descriptor.getArtifactId() + "-" + descriptor.getVersion() + "-";
        logger.debug("prefix = %s", prefix);
        contentFile = new File(prefix + CONTENT_SUFFIX);
        platformContentFile = new File(prefix + PLATFORM_CONTENT_SUFFIX);
    }

    public void open(URL descriptorUrl) throws XmlException, IOException {
        final DescriptorDocument doc = DescriptorDocument.Factory.parse(descriptorUrl);
        descriptor = doc.getDescriptor();
        final String descStr = descriptorUrl.toString();
        logger.log(CAT.packageid(descriptor.getGroupId(), descriptor.getArtifactId(), descriptor.getVersion(), descStr));
        final int n = descStr.lastIndexOf('/');
        final String prefix = descStr.substring(0, n + 1);
        logger.debug("prefix = %s", prefix);
        final String av = descriptor.getArtifactId() + "-" + descriptor.getVersion();
        contentFile = File.createTempFile(av + ".", CONTENT_SUFFIX);
        contentFile.deleteOnExit();
        final URL contentUrl = new URL(prefix + CONTENT_SUFFIX);
        logger.debug("content: %s", contentUrl);
        FileUtils.copyStreamToFile(new URLInputStreamFacade(contentUrl), contentFile);
        platformContentFile = File.createTempFile(av + ".", "." + PLATFORM_CONTENT_SUFFIX);
        platformContentFile.deleteOnExit();
        final URL platformContentUrl = new URL(prefix + PLATFORM_CONTENT_SUFFIX);
        logger.debug("platform content: %s", platformContentUrl);
        FileUtils.copyStreamToFile(new URLInputStreamFacade(platformContentUrl), platformContentFile);
    }

    public void install() throws ArchiverException, IOException {
        if (descriptor == null) {
            throw new IllegalStateException("No descriptor is open");
        }
        logger.log(CAT.where(where));
        unpack(contentFile, where);
        unpack(platformContentFile, where);
        preload();
    }

    private void unpack(File sourceFile, File destDir) throws ArchiverException {
        final String name = sourceFile.getName();
        logger.log(CAT.unpacking(sourceFile, destDir));
        final UnArchiver ua;
        if (name.endsWith(".zip")) {
            final ZipUnArchiver unArchiver = new ZipUnArchiver();
            unArchiver.enableLogging(plexusLogger);
            ua = unArchiver;
        } else if (name.endsWith(".tar.gz")) {
            final TarGZipUnArchiver unArchiver = new TarGZipUnArchiver();
            unArchiver.enableLogging(plexusLogger);
            ua = unArchiver;
        } else {
            throw new ArchiverException("No archiver supported for file " + name);
        }
        ua.setSourceFile(sourceFile);
        destDir.mkdirs();
        ua.setDestDirectory(destDir);
        System.out.println("ua.getDestDirectory() = " + ua.getDestDirectory());
        ua.extract();
    }

    private void preload() throws IOException {
        for (PreloadList preloadList : descriptor.getPreloadArray()) {
            final String repoUrl = preloadList.getRepoUrl();
            preloadFromRepository(repoUrl, preloadList.getArtifactArray());
        }
    }

    private void preloadFromRepository(String repoUrl, PathItemBean[] artifactArray) throws IOException {
        for (PathItemBean a : artifactArray) {
            final String uri = gavUri(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getType());
            final File destFile = new File(mavenRepo, uri);
            File foundLocally = lookupLocally(a);
            if (foundLocally == null) {
                if (repoUrl == null) {
                    throw new FileNotFoundException(destFile.getAbsolutePath());
                }
                final File destTmp = downloadArtifact(repoUrl, a);
                checkFile(destTmp, a.getDigestArray());
                destFile.delete();
                destTmp.renameTo(destFile);
                foundLocally = destFile;
            }
            foundLocally = foundLocally.getCanonicalFile();
            if (!foundLocally.equals(destFile.getCanonicalFile())) {
                logger.log(CAT.copying(foundLocally, destFile));
                FileUtils.copyFile(foundLocally, destFile);
            }
        }
    }

    protected File downloadArtifact(String repoUrl, PathItemBean a) throws IOException {
        return downloadArtifactDirectly(repoUrl, a);
    }

    protected final File downloadArtifactDirectly(String repoUrl, PathItemBean a) throws IOException {
        final String uri = gavUri(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getType());
        final File destFile = new File(mavenRepo, uri);
        destFile.getParentFile().mkdirs();
        final File destTmp = new File(mavenRepo, uri + ".tmp");
        destTmp.delete();
        final URL url = new URL(repoUrl + "/" + uri);
        downloadFile(url, destTmp);
        return destTmp;
    }

    private File lookupLocally(PathItemBean a) throws IOException {
        final String uri = gavUri(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getType());
        final List<File> repos = new ArrayList<File>();
        repos.add(mavenRepo);
        repos.addAll(localRepositories);
        for (File repo : repos) {
            final File f = new File(repo, uri);
            if (f.isFile() && checkFileNoFail(f, a.getDigestArray())) {
                return f;
            }
        }
        return null;
    }

    private void downloadFile(URL url, File dest) throws IOException {
        logger.log(CAT.loading(url, dest));
        FileUtils.copyStreamToFile(new URLInputStreamFacade(url), dest);
    }

    private boolean checkFileNoFail(File destFile, DigestBean[] digests) throws IOException {
        for (DigestBean digest : digests) {
            final String alg = digest.getAlgorithm();
            try {
                final byte[] checksumBytes = Checksum.computeChecksum(destFile, alg);
                final String checksum = Checksum.toString(checksumBytes);
                final String expectedChecksum = digest.getStringValue();
                if (!checksum.equals(expectedChecksum)) {
                    logger.log(CAT.checksumFailed(destFile, alg, expectedChecksum, checksum));
                    return false;
                }
            } catch (NoSuchAlgorithmException e) {
                logger.log(CAT.cantVerify(destFile, alg, e.getClass().getName(), e.getMessage()));
            }
        }
        return true;
    }

    private void checkFile(File destFile, DigestBean[] digests) throws IOException {
        if (!checkFileNoFail(destFile, digests)) {
            throw new IOException(destFile + ": checksum failed");
        }
    }

    public static String gavUri(String groupId, String artifactId, String version, String classifier, String type) {
        return String.format("%s/%s/%s/%2$s-%3$s%s.%s", groupId.replace('.', '/'), artifactId, version, classifier == null ? "" : "-" + classifier, type);
    }

    public void setLocalRepositories(List<File> localRepositories) {
        this.localRepositories = localRepositories;
    }

    private static interface Catalog extends StrictCatalog {

        @StrictCatalogEntry(severity = Severity.INFO, format = "Destination local directory: '%s'")
        LogMessage where(File where);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Unpacking: %s --> %s")
        LogMessage unpacking(File sourceFile, File destDir);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Copying: %s --> %s")
        LogMessage copying(File src, File dest);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Downloading: %s --> %s")
        LogMessage loading(URL url, File dest);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Installing: %s:%s:%s from %s")
        LogMessage packageid(String groupId, String artifactId, String version, String descriptorSource);

        @StrictCatalogEntry(severity = Severity.ERROR, format = "File %s: %s checksum failed - expected '%s' but got '%s'")
        LogMessage checksumFailed(File destFile, String alg, String expectedChecksum, String checksum);

        @StrictCatalogEntry(severity = Severity.WARN, format = "File %s: %s checksum cannot be verified due to %s:%s")
        LogMessage cantVerify(File destFile, String alg, String exceptionClass, String exceptionMessage);
    }
}
