package org.sf.pomreader;

import org.apache.maven.bootstrap.download.AbstractArtifactResolver;
import org.apache.maven.bootstrap.download.DownloadFailedException;
import org.apache.maven.bootstrap.download.HttpUtils;
import org.apache.maven.bootstrap.download.RepositoryMetadata;
import org.apache.maven.bootstrap.model.Dependency;
import org.apache.maven.bootstrap.model.Model;
import org.apache.maven.bootstrap.model.Repository;
import org.apache.maven.bootstrap.util.FileUtils;
import org.apache.maven.bootstrap.util.StringUtils;
import java.io.*;
import java.util.*;

public class OnlineArtifactDownloader extends AbstractArtifactResolver {

    public static final String SNAPSHOT_SIGNATURE = "-SNAPSHOT";

    private boolean useTimestamp = true;

    private boolean ignoreErrors = false;

    private String proxyHost;

    private String proxyPort;

    private String proxyUserName;

    private String proxyPassword;

    private Map<String, Dependency> downloadedArtifacts = new HashMap<String, Dependency>();

    private List<Repository> remoteRepositories;

    public OnlineArtifactDownloader(Repository localRepository) throws Exception {
        super(localRepository);
    }

    public void setProxy(String host, String port, String userName, String password) {
        proxyHost = host;
        proxyPort = port;
        proxyUserName = userName;
        proxyPassword = password;
    }

    public void downloadDependencies(Collection dependencies) throws DownloadFailedException {
        for (Object dependency : dependencies) {
            Dependency dep = (Dependency) dependency;
            if (isAlreadyBuilt(dep)) {
                continue;
            }
            String dependencyConflictId = dep.getDependencyConflictId();
            if (!downloadedArtifacts.containsKey(dependencyConflictId)) {
                File destinationFile = getLocalRepository().getArtifactFile(dep);
                File directory = destinationFile.getParentFile();
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                if (!getRemoteArtifact(dep, destinationFile) && !destinationFile.exists()) {
                    throw new DownloadFailedException(dep);
                }
                downloadedArtifacts.put(dependencyConflictId, dep);
            } else {
                Dependency d = downloadedArtifacts.get(dependencyConflictId);
                dep.setResolvedVersion(d.getResolvedVersion());
            }
        }
    }

    public boolean isOnline() {
        return true;
    }

    private static boolean isSnapshot(Dependency dep) {
        if (dep == null || dep.getGroupId().startsWith("org.apache.maven")) {
            return false;
        }
        if (dep.getVersion() == null) {
            return false;
        }
        return dep.getVersion().indexOf(SNAPSHOT_SIGNATURE) >= 0;
    }

    private boolean getRemoteArtifact(Dependency dep, File destinationFile) {
        boolean fileFound = false;
        List<Object> repositories = new ArrayList<Object>();
        repositories.addAll(getRemoteRepositories());
        repositories.addAll(dep.getRepositories());
        for (Object o : dep.getChain()) {
            repositories.addAll(((Model) o).getRepositories());
        }
        for (Object repository : repositories) {
            Repository remoteRepo = (Repository) repository;
            boolean snapshot = isSnapshot(dep);
            if (snapshot && !remoteRepo.isSnapshots()) {
                continue;
            }
            if (!snapshot && !remoteRepo.isReleases()) {
                continue;
            }
            String url = remoteRepo.getBasedir() + "/" + remoteRepo.getArtifactPath(dep);
            try {
                String version = dep.getVersion();
                if (snapshot) {
                    String filename = "maven-metadata-" + remoteRepo.getId() + ".xml";
                    File localFile = getLocalRepository().getMetadataFile(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType(), "maven-metadata-local.xml");
                    File remoteFile = getLocalRepository().getMetadataFile(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType(), filename);
                    String metadataPath = remoteRepo.getMetadataPath(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType(), "maven-metadata.xml");
                    String metaUrl = remoteRepo.getBasedir() + "/" + metadataPath;
                    log("Downloading " + metaUrl);
                    if (metaUrl.startsWith("file://")) {
                        loadFileLocally(metaUrl, remoteFile);
                    } else {
                        try {
                            HttpUtils.getFile(metaUrl, remoteFile, ignoreErrors, true, proxyHost, proxyPort, proxyUserName, proxyPassword, false);
                        } catch (IOException e) {
                            log("WARNING: remote metadata version not found, using local: " + e.getMessage());
                            remoteFile.delete();
                        }
                    }
                    File file = localFile;
                    if (remoteFile.exists()) {
                        if (!localFile.exists()) {
                            file = remoteFile;
                        } else {
                            RepositoryMetadata localMetadata = RepositoryMetadata.read(localFile);
                            RepositoryMetadata remoteMetadata = RepositoryMetadata.read(remoteFile);
                            if (remoteMetadata.getLastUpdatedUtc() > localMetadata.getLastUpdatedUtc()) {
                                file = remoteFile;
                            } else {
                                file = localFile;
                            }
                        }
                    }
                    if (file.exists()) {
                        log("Using metadata: " + file);
                        RepositoryMetadata metadata = RepositoryMetadata.read(file);
                        if (!file.equals(localFile)) {
                            version = metadata.constructVersion(version);
                        }
                        log("Resolved version: " + version);
                        dep.setResolvedVersion(version);
                        if (!version.endsWith("SNAPSHOT")) {
                            String ver = version.substring(version.lastIndexOf("-", version.lastIndexOf("-") - 1) + 1);
                            String extension = url.substring(url.length() - 4);
                            url = getSnapshotMetadataFile(url, ver + extension);
                        } else if (destinationFile.exists()) {
                            return true;
                        }
                    }
                }
                if (!"pom".equals(dep.getType())) {
                    String name = dep.getArtifactId() + "-" + dep.getResolvedVersion() + ".pom";
                    File file = getLocalRepository().getMetadataFile(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType(), name);
                    file.getParentFile().mkdirs();
                    if (!file.exists() || version.indexOf("SNAPSHOT") >= 0) {
                        String filename = dep.getArtifactId() + "-" + version + ".pom";
                        String metadataPath = remoteRepo.getMetadataPath(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType(), filename);
                        String metaUrl = remoteRepo.getBasedir() + "/" + metadataPath;
                        log("Downloading " + metaUrl);
                        if (metaUrl.startsWith("file://")) {
                            loadFileLocally(metaUrl, file);
                        } else {
                            try {
                                HttpUtils.getFile(metaUrl, file, ignoreErrors, false, proxyHost, proxyPort, proxyUserName, proxyPassword, false);
                            } catch (IOException e) {
                                log("Couldn't find POM - ignoring: " + e.getMessage());
                            }
                        }
                    }
                }
                destinationFile = getLocalRepository().getArtifactFile(dep);
                if (!destinationFile.exists()) {
                    log("Downloading " + url);
                    if (url.startsWith("file://")) {
                        loadFileLocally(url, destinationFile);
                    } else {
                        HttpUtils.getFile(url, destinationFile, ignoreErrors, useTimestamp, proxyHost, proxyPort, proxyUserName, proxyPassword, true);
                        if (dep.getVersion().indexOf("SNAPSHOT") >= 0) {
                            String name = StringUtils.replace(destinationFile.getName(), version, dep.getVersion());
                            FileUtils.copyFile(destinationFile, new File(destinationFile.getParentFile(), name));
                        }
                    }
                }
                fileFound = true;
            } catch (FileNotFoundException e) {
                log("Artifact not found at [" + url + "]");
            } catch (Exception e) {
                log("Error retrieving artifact from [" + url + "]: " + e);
            }
        }
        return fileFound;
    }

    private void loadFileLocally(String url, File destinationFile) throws IOException {
        InputStream is = new FileInputStream(url.substring(7));
        FileOutputStream fos = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[100 * 1024];
        int length;
        while ((length = is.read(buffer)) >= 0) {
            fos.write(buffer, 0, length);
            System.out.print(".");
        }
        System.out.println();
        fos.close();
        is.close();
    }

    private static String getSnapshotMetadataFile(String filename, String s) {
        int index = filename.lastIndexOf("SNAPSHOT");
        return filename.substring(0, index) + s;
    }

    private void log(String message) {
        System.out.println(message);
    }

    public List<Repository> getRemoteRepositories() {
        if (remoteRepositories == null) {
            remoteRepositories = new ArrayList<Repository>();
        }
        if (remoteRepositories.isEmpty()) {
            remoteRepositories.add(new Repository("scriptlandia0-repo", "file://" + System.getProperty("maven.repo.local") + "-accelerator", Repository.LAYOUT_DEFAULT, false, true));
            remoteRepositories.add(new Repository("mergere", "http://repo.mergere.com/maven2", Repository.LAYOUT_DEFAULT, false, true));
            remoteRepositories.add(new Repository("apache.snapshots", "http://cvs.apache.org/maven-snapshot-repository", Repository.LAYOUT_DEFAULT, true, false));
            remoteRepositories.add(new Repository("central2", "http://repo1.maven.org/maven2", Repository.LAYOUT_DEFAULT, false, true));
            remoteRepositories.add(new Repository("central3", "http://repo1.maven.org/maven-spring", Repository.LAYOUT_DEFAULT, false, true));
            remoteRepositories.add(new Repository("scriptlandia-languages-repo", "http://scriptlandia-repository.googlecode.com/svn/trunk/languages", Repository.LAYOUT_DEFAULT, false, true));
        }
        return remoteRepositories;
    }

    public void setRemoteRepositories(List<Repository> remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }
}
