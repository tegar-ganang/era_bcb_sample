package org.rhq.plugins.mobicents.servlet.sip.jboss5;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.content.version.PackageVersions;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.rhq.plugins.mobicents.servlet.sip.jboss5.util.DeploymentUtils;

/**
 * @author Ian Springer
 */
public class StandaloneManagedDeploymentComponent extends AbstractManagedDeploymentComponent implements MeasurementFacet, ContentFacet, DeleteResourceFacet {

    public static final String RESOURCE_TYPE_EAR = "Enterprise Application (EAR)";

    public static final String RESOURCE_TYPE_WAR = "Web Application (WAR)";

    public static final String RESOURCE_TYPE_CONVERGED_WAR = "Converged SIP/Web Application (WAR)";

    public static final String RESOURCE_TYPE_RAR = "Resource Adaptor (RAR)";

    private static final String CUSTOM_PATH_TRAIT = "custom.path";

    private static final String CUSTOM_EXPLODED_TRAIT = "custom.exploded";

    /**
     * Name of the backing package type that will be used when discovering packages. This corresponds to the name
     * of the package type defined in the plugin descriptor. For simplicity, the package type for both EARs and
     * WARs is simply called "file". This is still unique within the context of the parent resource type and lets
     * this class use the same package type name in both cases.
     */
    private static final String PKG_TYPE_FILE = "file";

    /**
     * Architecture string used in describing discovered packages.
     */
    private static final String ARCHITECTURE = "noarch";

    private static final String BACKUP_FILE_EXTENSION = ".rej";

    private final Log log = LogFactory.getLog(this.getClass());

    private PackageVersions versions;

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            if (metricName.equals(CUSTOM_PATH_TRAIT)) {
                MeasurementDataTrait trait = new MeasurementDataTrait(request, this.deploymentFile.getPath());
                report.addData(trait);
            } else if (metricName.equals(CUSTOM_EXPLODED_TRAIT)) {
                boolean exploded = this.deploymentFile.isDirectory();
                MeasurementDataTrait trait = new MeasurementDataTrait(request, (exploded) ? "yes" : "no");
                report.addData(trait);
            }
        }
    }

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        File packageFile = new File(packageDetails.getName());
        File fileToSend;
        try {
            if (packageFile.isDirectory()) {
                fileToSend = File.createTempFile("rhq", ".zip");
                ZipUtil.zipFileOrDirectory(packageFile, fileToSend);
            } else fileToSend = packageFile;
            return new BufferedInputStream(new FileInputStream(fileToSend));
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve package bits for " + packageDetails, e);
        }
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType packageType) {
        if (!this.deploymentFile.exists()) throw new IllegalStateException("Deployment file '" + this.deploymentFile + "' for " + getResourceDescription() + " does not exist.");
        String fileName = this.deploymentFile.getName();
        PackageVersions packageVersions = loadPackageVersions();
        String version = packageVersions.getVersion(fileName);
        if (version == null) {
            version = "1.0";
            packageVersions.putVersion(fileName, version);
            packageVersions.saveToDisk();
        }
        PackageDetailsKey key = new PackageDetailsKey(fileName, version, PKG_TYPE_FILE, ARCHITECTURE);
        ResourcePackageDetails packageDetails = new ResourcePackageDetails(key);
        packageDetails.setFileName(fileName);
        packageDetails.setLocation(this.deploymentFile.getPath());
        if (!this.deploymentFile.isDirectory()) packageDetails.setFileSize(this.deploymentFile.length());
        packageDetails.setFileCreatedDate(null);
        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();
        packages.add(packageDetails);
        return packages;
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        throw new UnsupportedOperationException("Cannot remove the package backing an EAR/WAR resource.");
    }

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        if (packages.size() != 1) {
            log.warn("Request to update an EAR/WAR file contained multiple packages: " + packages);
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("When updating an EAR/WAR, only one EAR/WAR can be updated at a time.");
            return response;
        }
        ResourcePackageDetails packageDetails = packages.iterator().next();
        log.debug("Updating EAR/WAR file '" + this.deploymentFile + "' using [" + packageDetails + "]...");
        if (!this.deploymentFile.exists()) {
            return failApplicationDeployment("Could not find application to update at location: " + this.deploymentFile, packageDetails);
        }
        log.debug("Writing new EAR/WAR bits to temporary file...");
        File tempFile;
        try {
            tempFile = writeNewAppBitsToTempFile(contentServices, packageDetails);
        } catch (Exception e) {
            return failApplicationDeployment("Error writing new application bits to temporary file - cause: " + e, packageDetails);
        }
        log.debug("Wrote new EAR/WAR bits to temporary file '" + tempFile + "'.");
        boolean deployExploded = this.deploymentFile.isDirectory();
        File backupOfOriginalFile = new File(this.deploymentFile.getPath() + BACKUP_FILE_EXTENSION);
        log.debug("Backing up existing EAR/WAR '" + this.deploymentFile + "' to '" + backupOfOriginalFile + "'...");
        try {
            if (backupOfOriginalFile.exists()) FileUtils.forceDelete(backupOfOriginalFile);
            if (this.deploymentFile.isDirectory()) FileUtils.copyDirectory(this.deploymentFile, backupOfOriginalFile, true); else FileUtils.copyFile(this.deploymentFile, backupOfOriginalFile, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to backup existing EAR/WAR '" + this.deploymentFile + "' to '" + backupOfOriginalFile + "'.");
        }
        try {
            DeploymentManager deploymentManager = ProfileServiceFactory.getDeploymentManager();
            DeploymentProgress progress = deploymentManager.stop(this.deploymentName);
            DeploymentUtils.run(progress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop deployment [" + this.deploymentName + "].", e);
        }
        try {
            DeploymentManager deploymentManager = ProfileServiceFactory.getDeploymentManager();
            DeploymentProgress progress = deploymentManager.remove(this.deploymentName);
            DeploymentUtils.run(progress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove deployment [" + this.deploymentName + "].", e);
        }
        log.debug("Deploying '" + tempFile + "'...");
        File deployDir = this.deploymentFile.getParentFile();
        try {
            DeploymentUtils.deployArchive(tempFile, deployDir, deployExploded);
        } catch (Exception e) {
            log.debug("Redeploy failed - rolling back to original archive...", e);
            String errorMessage = ThrowableUtil.getAllMessages(e);
            try {
                FileUtils.forceDelete(this.deploymentFile);
                DeploymentUtils.deployArchive(backupOfOriginalFile, deployDir, deployExploded);
                errorMessage += " ***** ROLLED BACK TO ORIGINAL APPLICATION FILE. *****";
            } catch (Exception e1) {
                log.debug("Rollback failed!", e1);
                errorMessage += " ***** FAILED TO ROLLBACK TO ORIGINAL APPLICATION FILE. *****: " + ThrowableUtil.getAllMessages(e1);
            }
            log.info("Failed to update EAR/WAR file '" + this.deploymentFile + "' using [" + packageDetails + "].");
            return failApplicationDeployment(errorMessage, packageDetails);
        }
        deleteBackupOfOriginalFile(backupOfOriginalFile);
        persistApplicationVersion(packageDetails, this.deploymentFile);
        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.SUCCESS);
        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.SUCCESS);
        response.addPackageResponse(packageResponse);
        log.debug("Updated EAR/WAR file '" + this.deploymentFile + "' successfully - returning response [" + response + "]...");
        return response;
    }

    public void deleteResource() throws Exception {
        DeploymentManager deploymentManager = ProfileServiceFactory.getDeploymentManager();
        log.debug("Stopping deployment [" + this.deploymentName + "]...");
        DeploymentProgress progress = deploymentManager.stop(deploymentName);
        DeploymentUtils.run(progress);
        log.debug("Removing deployment [" + this.deploymentName + "]...");
        progress = deploymentManager.remove(deploymentName);
        DeploymentUtils.run(progress);
    }

    @Override
    protected Log getLog() {
        return this.log;
    }

    /**
     * Creates the necessary transfer objects to report a failed application deployment (update).
     *
     * @param errorMessage   reason the deploy failed
     * @param packageDetails describes the update being made
     *
     * @return response populated to reflect a failure
     */
    private DeployPackagesResponse failApplicationDeployment(String errorMessage, ResourcePackageDetails packageDetails) {
        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.FAILURE);
        packageResponse.setErrorMessage(errorMessage);
        response.addPackageResponse(packageResponse);
        return response;
    }

    private void persistApplicationVersion(ResourcePackageDetails packageDetails, File appFile) {
        String packageName = appFile.getName();
        log.debug("Persisting application version '" + packageDetails.getVersion() + "' for package '" + packageName + "'");
        PackageVersions versions = loadPackageVersions();
        versions.putVersion(packageName, packageDetails.getVersion());
    }

    private void deleteBackupOfOriginalFile(File backupOfOriginalFile) {
        log.debug("Deleting backup of original file '" + backupOfOriginalFile + "'...");
        try {
            FileUtils.forceDelete(backupOfOriginalFile);
        } catch (Exception e) {
            log.warn("Failed to delete backup of original file: " + backupOfOriginalFile);
        }
    }

    private File writeNewAppBitsToTempFile(ContentServices contentServices, ResourcePackageDetails packageDetails) throws Exception {
        File tempDir = getResourceContext().getTemporaryDirectory();
        File tempFile = new File(tempDir, this.deploymentFile.getName());
        OutputStream tempOutputStream = null;
        try {
            tempOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            long bytesWritten = contentServices.downloadPackageBits(getResourceContext().getContentContext(), packageDetails.getKey(), tempOutputStream, true);
            log.debug("Wrote " + bytesWritten + " bytes to '" + tempFile + "'.");
        } catch (IOException e) {
            log.error("Error writing updated application bits to temporary location: " + tempFile, e);
            throw e;
        } finally {
            if (tempOutputStream != null) {
                try {
                    tempOutputStream.close();
                } catch (IOException e) {
                    log.error("Error closing temporary output stream", e);
                }
            }
        }
        if (!tempFile.exists()) {
            log.error("Temporary file for application update not written to: " + tempFile);
            throw new Exception();
        }
        return tempFile;
    }

    /**
     * Returns an instantiated and loaded versions store access point.
     *
     * @return will not be <code>null</code>
     */
    private PackageVersions loadPackageVersions() {
        if (this.versions == null) {
            ResourceType resourceType = getResourceContext().getResourceType();
            String pluginName = resourceType.getPlugin();
            File dataDirectoryFile = getResourceContext().getDataDirectory();
            dataDirectoryFile.mkdirs();
            String dataDirectory = dataDirectoryFile.getAbsolutePath();
            log.trace("Creating application versions store with plugin name [" + pluginName + "] and data directory [" + dataDirectory + "]");
            this.versions = new PackageVersions(pluginName, dataDirectory);
            this.versions.loadFromDisk();
        }
        return this.versions;
    }
}
