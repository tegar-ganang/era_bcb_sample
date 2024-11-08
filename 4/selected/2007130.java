package com.ivis.xprocess.core.impl;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ivis.xprocess.core.Artifact;
import com.ivis.xprocess.core.ArtifactContainer;
import com.ivis.xprocess.core.FormType;
import com.ivis.xprocess.framework.XchangeElementContainer;
import com.ivis.xprocess.framework.impl.XchangeElementContainerImpl;
import com.ivis.xprocess.framework.vcs.VcsProvider;
import com.ivis.xprocess.framework.xml.IPersistenceHelper;
import com.ivis.xprocess.util.FileUtils;
import com.ivis.xprocess.util.LogicalLink;
import com.ivis.xprocess.util.UuidUtils;

public class ArtifactContainerImpl implements ArtifactContainer {

    private static final String THIS_CLASS_NAME = "ArtifactContainerImpl";

    private static final Logger logger = Logger.getLogger(ArtifactContainerImpl.class.getName());

    private XchangeElementContainerImpl owner;

    public ArtifactContainerImpl(XchangeElementContainerImpl owner) {
        this.owner = owner;
    }

    public void buildLinks() {
    }

    public void unbuildLinks() {
    }

    public Artifact createForm(String name, FormType formType) {
        if (formType != null) {
            Artifact instance = this.createInstanceIn(name, formType, null, null);
            return instance;
        }
        return null;
    }

    public Artifact createManagedFile(String name, String prototypePath) {
        Artifact instance = this.createInstanceIn(name, null, prototypePath, null);
        return instance;
    }

    public Artifact createArtifact(String name, Artifact prototype) {
        FormTypeImpl formType = (FormTypeImpl) prototype.getFormType();
        Artifact artifact = null;
        String resolveArtifactPath = null;
        LogicalLink logicalLink = null;
        if (formType == null) {
            logicalLink = prototype.getLinkUrl();
            if (logicalLink != null) {
                artifact = this.createInstanceIn(name, null, null, logicalLink);
                return artifact;
            } else {
                try {
                    resolveArtifactPath = prototype.resolveArtifactPath();
                    artifact = this.createInstanceIn(name, null, resolveArtifactPath, null);
                    return artifact;
                } catch (Exception e) {
                    IllegalArgumentException iae = new IllegalArgumentException("A valid prototype artifact is required.", e);
                    logger.throwing(this.getClass().getName(), "createArtifact", iae);
                    throw iae;
                }
            }
        } else {
            artifact = this.createInstanceIn(name, formType, null, null);
            return artifact;
        }
    }

    public Artifact createLogicalLink(String name, FormType formType, LogicalLink link) {
        Artifact instance = this.createInstanceIn(name, null, "", link);
        return instance;
    }

    /**
     * Creates new artifact in owner (the owner of this ArtifactContainerImpl)
     *
     * One of formType, prototypePath or link must be not null...
     *
     * @param name
     * @param formType
     * @param prototypePath
     * @param link
     * @return
     */
    private Artifact createInstanceIn(String name, FormType formType, String prototypePath, LogicalLink link) {
        Artifact artifact = owner.getPersistenceHelper().createElement(Artifact.class, owner);
        artifact.setName(name);
        if (formType != null) {
            artifact.setFormType(formType);
            if (formType.getSchema() != null) {
                artifact.setSchema(formType.getSchema());
            }
        } else if (link != null) {
            if ((link.getAddress() == null) || link.getAddress().equalsIgnoreCase("")) {
                IllegalArgumentException iae = new IllegalArgumentException("Link has bad address: " + link);
                logger.throwing(this.getClass().getName(), "createInstanceIn", iae);
                throw iae;
            } else {
                artifact.setLinkUrl(link);
            }
        } else if (prototypePath != null) {
            artifact.setManagedPath(ArtifactContainerImpl.acquireExternalArtifact(prototypePath, owner));
        }
        ((ArtifactImpl) artifact).buildLinks();
        return artifact;
    }

    public Set<Artifact> getArtifacts() {
        return owner.getContentsByType(Artifact.class);
    }

    public Set<Artifact> getAllArtifacts() {
        return getArtifacts();
    }

    /**
     * @param fromPath
     *            String, the path to the artifact prototype
     * @param container
     *            XchangeElement, the container into which the artifact be
     *            imported
     * @return String, path relative to the root portfolio artifacts dir
     */
    public static String acquireExternalArtifact(String fromPath, XchangeElementContainer container) {
        fromPath = FileUtils.fixPathToBackSlash(fromPath);
        if ((fromPath == null) || fromPath.equals("")) {
            IllegalArgumentException iae = new IllegalArgumentException("A valid from path is required.");
            logger.throwing(THIS_CLASS_NAME, "acquireExternalArtifact", iae);
            throw iae;
        }
        IPersistenceHelper ph = container.getPersistenceHelper();
        File fromFile = new File(fromPath);
        if (!fromFile.exists() || fromFile.isDirectory()) {
            RuntimeException re = new RuntimeException("Prototype artifact must exist and can't be a directory");
            logger.throwing(THIS_CLASS_NAME, "acquireExternalArtifact", re);
            throw re;
        }
        String artifactsDir = ph.getDataLayout().getArtifactsDir();
        if ((artifactsDir == null) || artifactsDir.equals("")) {
            RuntimeException re = new RuntimeException("Artifact dir is not defined in DataLayout.xml");
            logger.throwing(THIS_CLASS_NAME, "acquireExternalArtifact", re);
            throw re;
        }
        String relDir = File.separator + container.getId() + File.separator;
        String extension = "";
        int lastSlashPos = fromPath.lastIndexOf(File.separator);
        int dotPos = fromPath.lastIndexOf(".");
        if (lastSlashPos < dotPos) {
            extension = fromPath.substring(fromPath.lastIndexOf("."));
        }
        String uuid = UuidUtils.getUUID();
        String relativeArtifactPath = relDir + uuid + extension;
        String artifactDir = artifactsDir + relDir;
        File artifactDirFile = new File(artifactDir);
        String absoluteArtifactPath = artifactsDir + relativeArtifactPath;
        File artifactFile;
        artifactFile = new File(absoluteArtifactPath);
        VcsProvider vcs = container.getPersistenceHelper().getDataSource().getVcsProvider();
        if (vcs != null) {
            String artifactsLimboDir = ph.getDataLayout().getArtifactsLimboDir();
            artifactDirFile = new File(artifactsLimboDir + File.separator + relDir);
            if (!artifactDirFile.exists()) {
                artifactDirFile.mkdirs();
            }
            artifactFile = new File(artifactsLimboDir + File.separator + relativeArtifactPath);
        } else {
            if (!artifactDirFile.exists()) {
                artifactDirFile.mkdirs();
            }
        }
        try {
            FileUtils.copyFile(fromFile, artifactFile);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Problem copying artifact", e);
        }
        return relativeArtifactPath;
    }
}
