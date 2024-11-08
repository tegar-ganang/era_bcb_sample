package org.apache.maven.bootstrap.download;

import org.apache.maven.bootstrap.model.Dependency;
import org.apache.maven.bootstrap.model.Model;
import org.apache.maven.bootstrap.model.Repository;
import org.apache.maven.bootstrap.util.FileUtils;
import org.apache.maven.bootstrap.util.StringUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OnlineArtifactDownloader extends AbstractArtifactResolver {

    public static final String SNAPSHOT_SIGNATURE = "-SNAPSHOT";

    private boolean useTimestamp = true;

    private boolean ignoreErrors = false;

    private String proxyHost;

    private String proxyPort;

    private String proxyUserName;

    private String proxyPassword;

    private static final String REPO_URL = "http://repo1.maven.org/maven2";

    private Map downloadedArtifacts = new HashMap();

    private List remoteRepositories;

    public OnlineArtifactDownloader(Repository localRepository) throws Exception {
        super(localRepository);
    }

    public void setProxy(String host, String port, String userName, String password) {
        proxyHost = host;
        proxyPort = port;
        proxyUserName = userName;
        proxyPassword = password;
        System.out.println("Using the following proxy : " + proxyHost + "/" + proxyPort);
    }

    public void downloadDependencies(Collection dependencies) throws DownloadFailedException {
        for (Iterator j = dependencies.iterator(); j.hasNext(); ) {
            Dependency dep = (Dependency) j.next();
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
                Dependency d = (Dependency) downloadedArtifacts.get(dependencyConflictId);
                dep.setResolvedVersion(d.getResolvedVersion());
            }
        }
    }

    public boolean isOnline() {
        return true;
    }

    private static boolean isSnapshot(Dependency dep) {
        if (dep == null || dep.getGroupId().startsWith("org.apache.maven")) {
            if (!dep.getArtifactId().equals("maven-parent")) {
                return false;
            }
        }
        if (dep.getVersion() == null) {
            return false;
        }
        return dep.getVersion().indexOf(SNAPSHOT_SIGNATURE) >= 0;
    }

    private boolean getRemoteArtifact(Dependency dep, File destinationFile) {
        boolean fileFound = false;
        List repositories = new ArrayList();
        repositories.addAll(getRemoteRepositories());
        repositories.addAll(dep.getRepositories());
        for (Iterator i = dep.getChain().iterator(); i.hasNext(); ) {
            repositories.addAll(((Model) i.next()).getRepositories());
        }
        for (Iterator i = repositories.iterator(); i.hasNext(); ) {
            Repository remoteRepo = (Repository) i.next();
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
                    try {
                        HttpUtils.getFile(metaUrl, remoteFile, ignoreErrors, true, proxyHost, proxyPort, proxyUserName, proxyPassword, false);
                    } catch (IOException e) {
                        log("WARNING: remote metadata version not found, using local: " + e.getMessage());
                        remoteFile.delete();
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
                        try {
                            HttpUtils.getFile(metaUrl, file, ignoreErrors, false, proxyHost, proxyPort, proxyUserName, proxyPassword, false);
                        } catch (IOException e) {
                            log("Couldn't find POM - ignoring: " + e.getMessage());
                        }
                    }
                }
                destinationFile = getLocalRepository().getArtifactFile(dep);
                if (!destinationFile.exists()) {
                    log("Downloading " + url);
                    HttpUtils.getFile(url, destinationFile, ignoreErrors, useTimestamp, proxyHost, proxyPort, proxyUserName, proxyPassword, true);
                    if (dep.getVersion().indexOf("SNAPSHOT") >= 0) {
                        String name = StringUtils.replace(destinationFile.getName(), version, dep.getVersion());
                        FileUtils.copyFile(destinationFile, new File(destinationFile.getParentFile(), name));
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

    private static String getSnapshotMetadataFile(String filename, String s) {
        int index = filename.lastIndexOf("SNAPSHOT");
        return filename.substring(0, index) + s;
    }

    private void log(String message) {
        System.out.println(message);
    }

    public List getRemoteRepositories() {
        if (remoteRepositories == null) {
            remoteRepositories = new ArrayList();
        }
        if (remoteRepositories.isEmpty()) {
            remoteRepositories.add(new Repository("central", REPO_URL, Repository.LAYOUT_DEFAULT, false, true));
            remoteRepositories.add(new Repository("apache.snapshots", "http://people.apache.org/repo/m2-snapshot-repository/", Repository.LAYOUT_DEFAULT, true, false));
        }
        return remoteRepositories;
    }

    public void setRemoteRepositories(List remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }
}
