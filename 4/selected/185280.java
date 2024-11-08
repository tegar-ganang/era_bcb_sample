package org.openremote.modeler.service.impl;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StreamCorruptedException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.OptionalDataException;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import org.openremote.modeler.configuration.PathConfig;
import org.openremote.modeler.client.Configuration;
import org.openremote.modeler.client.utils.PanelsAndMaxOid;
import org.openremote.modeler.domain.Account;
import org.openremote.modeler.domain.Panel;
import org.openremote.modeler.domain.User;
import org.openremote.modeler.exception.UIRestoreException;
import org.openremote.modeler.exception.NetworkException;
import org.openremote.modeler.exception.ConfigurationException;
import org.openremote.modeler.beehive.Beehive30API;
import org.openremote.modeler.beehive.BeehiveService;
import org.openremote.modeler.cache.LocalFileCache;
import org.openremote.modeler.cache.ResourceCache;
import org.openremote.modeler.cache.CacheOperationException;
import org.openremote.modeler.logging.LogFacade;
import org.openremote.modeler.logging.AdministratorAlert;

/**
 * TODO
 *
 *   This is a temporary implementation. It only exists to support the
 *   existing legacy Java serialization for designer UI state.
 *
 *   Fundamentally the original mechanism for state persistence is
 *   too brittle and unsuitable, and should be replaced. Once that
 *   is complete, this implementation can be mostly or completely
 *   removed.
 *
 *   The issues with current serialization-based state persistence
 *   are many:
 *
 *     - In its current state it is too brittle, using a binary
 *       format that is difficult to debug and correct when issues
 *       arise. A temporary remedy to this is to serialize the
 *       designer state in an alternative XML format which allows
 *       easier manipulation when corrections must be made.
 *
 *     - There's no recovery -- it is purely based on file I/O
 *       with no recovery options. File I/O *does* fail on occasions
 *       (for example in case of out-of-memory errors or forced
 *       shut-downs) so recovery is a necessity.
 *
 *     - It is based on regular file I/O -- it is somewhat redundant
 *       to implement recovery options for file I/O when a database
 *       is available which has lot of the ground-work for reliable
 *       storage on disk. Instead of using regular filesystem, the
 *       data could be moved to a DB with reliable disk management,
 *       reducing the need for recovery handling on regular file I/O
 *       which will always be inferior to the more dedicated disk
 *       management by a DB. This also enables hosting on cloud
 *       services that do not allow direct file system access.
 *
 *     - Current implementation is not restricted to merely persisting
 *       designer UI state. Large parts of the current serialization
 *       graph include data that is redundant and stored more reliably
 *       in the controller.xml and panel.xml documents (but these are
 *       not being used). Some of the data would clearly belong to the
 *       schemas for controller.xml and panel.xml but is not supported
 *       yet. Only a very minority of the data being persisted is
 *       actual UI state data. This makes the persistence a much more
 *       critical part of the implementation (with more urgent need
 *       for features such as recovery) than it would be otherwise.
 *       These very poor implementation choices must be corrected.
 *
 *     - State that is defined within controller.xml and panel.xml
 *       documents should be directly parsed and restored from those
 *       files. The implementation for this will be similar to that
 *       of an account import feature. The same object-to-xml mapping
 *       implementations should be reusable in designer, beehive and
 *       controller applications.
 *
 *   Ultimately a web-front end UI state persistence should be supported
 *   via Beehive REST API. This removes the need for localized
 *   UI state persistence, and enables UI state storage for all client
 *   applications, not merely this designer implementation.
 *
 *   Once the above is complete, this implementation can be removed.
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
class DesignerState {

    private static final LogFacade saveLog = LogFacade.getInstance(LogFacade.Category.STATE_SAVE);

    private static final LogFacade restoreLog = LogFacade.getInstance(LogFacade.Category.STATE_RECOVERY);

    private static final AdministratorAlert admin = AdministratorAlert.getInstance(AdministratorAlert.Type.DESIGNER_STATE);

    private static final Set<Long> haltAccountSave = new HashSet<Long>(0);

    /**
   * TODO : should come through resource cache interface
   *
   * Opens a deserialization stream to the legacy Designer UI state object. Attempts
   * to read the serialization header and initialize the stream ready to deserialize
   * panels from the file.
   *
   * @param legacyPanelsObjFile   file path to the legacy binary panels.obj serialization
   *                              file
   *
   * @return  object input stream positioned to read the first object from the
   *          legacy binary file
   *
   * @throws RestoreFailureException
   *            if opening a stream to the serialization file or verifying the header
   *            fails for any reason
   */
    private static ObjectInputStream openSerializationStream(File legacyPanelsObjFile) throws RestoreFailureException {
        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(new FileInputStream(legacyPanelsObjFile));
        } catch (FileNotFoundException e) {
            throw new RestoreFailureException("Previously checked " + legacyPanelsObjFile.getAbsoluteFile() + " can no longer be found, or was not a proper file : " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new RestoreFailureException("Security manager has denied read access to " + legacyPanelsObjFile.getAbsoluteFile() + ". Read/write access to designer temporary directory is required.", e);
        }
        try {
            return new ObjectInputStream(bis);
        } catch (SecurityException e) {
            throw new RestoreFailureException("Error in creating object input stream : " + e.getMessage(), e);
        } catch (StreamCorruptedException e) {
            throw new RestoreFailureException("Serialization header is corrupted : " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RestoreFailureException("I/O Error when attempting to read serialization header : " + e.getMessage(), e);
        }
    }

    private Long maxOID = 0L;

    private Collection<Panel> panels = new ArrayList<Panel>();

    private User user;

    private Configuration configuration;

    protected DesignerState(Configuration config, User user) {
        this.configuration = config;
        this.user = user;
    }

    @Override
    public String toString() {
        return "Panels : " + panels.size() + ", max OID : " + maxOID;
    }

    /**
   * Restores designer UI state to match the data found from Beehive. <p>
   *
   * Note the various problems with this current implementation described in this class'
   * documentation.  <p>
   *
   *
   * @throws NetworkException
   *            If any errors occur with the network connection when updating the cache
   *            from Beehive server -- the basic assumption here is that network exceptions
   *            are recoverable (within a certain time period) and the method call can
   *            optionally be re-attempted at later time. Do note that the exception class
   *            provides a {@link NetworkException.Severity severity level} which can be used
   *            to indicate the likelyhood that the network error can be recovered from.
   */
    protected void restore() throws NetworkException {
        ExecutionPerformance perf = new ExecutionPerformance(LogFacade.Category.STATE_RECOVERY_PERFORMANCE);
        perf.start();
        try {
            addContextLog();
            restoreLog.info("Attempting to restore designer panel UI state for user {0} (Acct ID : {1})", user.getUsername(), user.getAccount().getOid());
            LocalFileCache cache = new LocalFileCache(configuration, user);
            cache.update();
            PathConfig pathConfig = PathConfig.getInstance(configuration);
            File legacyPanelsObjFile = new File(pathConfig.getSerializedPanelsFile(user.getAccount()));
            boolean hasLegacyDesignerUIState = hasLegacyDesignerUIState(pathConfig, legacyPanelsObjFile);
            boolean hasCachedState = cache.hasState();
            boolean hasXMLUIState = hasXMLUIState();
            if (!hasLegacyDesignerUIState && !hasXMLUIState && !hasCachedState) {
                restoreLog.info("There was no serialized panels.obj file, no serialized XML stream and no local " + "cache backups found in local user cache {0}. No user state was found in Beehive. " + "Assuming a new user with no saved design.", printUserAccountLog(user));
                LogFacade.getInstance(LogFacade.Category.USER).info("NEW USER: {0}", printUserAccountLog(user));
                return;
            }
            if (hasLegacyDesignerUIState) {
                try {
                    restoreLegacyDesignerUIState(legacyPanelsObjFile);
                    restoreLog.info("Restored UI state : {0}", this);
                    return;
                } catch (RestoreFailureException e) {
                    admin.alert("There was a state restoration error from panels.obj file in account {0} ({1}) : {2}", e, user.getAccount().getOid(), printUserAccountLog(user), e.getMessage());
                }
            }
            if (hasXMLUIState) {
            }
            haltAccount(MessageFormat.format("Could not restore account state (Acct ID : {0})", user.getAccount().getOid()), "There has been an error in restoring the designer state from your account data. " + "The system administrators have been notified of this issue. As a precaution, " + "further modifications of your data has been disabled. Do not make changes to " + "your designs or configuration during this period, as these changes may get lost. " + "For further assistance, contact support.");
        } catch (ConfigurationException e) {
            haltAccount(MessageFormat.format("CRITICAL CONFIGURATION ERROR ({0}) : {1}", printUserAccountLog(user), e.getMessage()), MessageFormat.format("There's an issue with the server configuration that has prevented restoring your " + "account data from Beehive. The administrator has been notified of this issue. As a " + "safety precaution, all changes to your account data has been disabled until the " + "issue has been resolved. Do not make changes to your designs or configuration " + "during this time, as the changes may get lost. For further assistance, contact support." + "(Error Message : {0})", e.getMessage()), e);
        } catch (CacheOperationException e) {
            haltAccount(MessageFormat.format("RUNTIME I/O ERROR : {0} ({1}).", e.getMessage(), printUserAccountLog(user)), MessageFormat.format("There has been an I/O error in reading or saving a cached copy of your " + "account data stored in Beehive. The system administrators have been notified of " + "this error. To prevent any potential damage, further modifications of your data " + "has been disabled until the admins have cleared the issue. Do not make changes to " + "your designs or configuration during this period, as these changes may get lost. " + "For further assistance, contact support. (Error Message : {0})", e.getMessage()), e);
        } catch (Throwable t) {
            haltAccount(MessageFormat.format("IMPLEMENTATION ERROR : {0} ({1}).", t.getMessage(), printUserAccountLog(user)), MessageFormat.format("There was an implementation error in Designer while restoring your account data. " + "The system administrators have been notified of this issue. To prevent potential " + "damage to your data, further modifications have been disabled until the admins " + "have cleared the issue. Do not make changes to your designs or configuration " + "during this period, as these changes may get lost. For further assistance, " + "please contact support. (Error Message : {0})", t.getMessage()), t);
        } finally {
            perf.end("Restore time " + printUserAccountLog(user) + " : {1}");
            removeContextLog();
        }
    }

    /**
   * TODO
   *
   * @param panels
   */
    protected void save(Set<Panel> panels) {
        Account acct = user.getAccount();
        if (haltAccountSave.contains(acct.getOid())) {
            saveLog.error("Did not save to Beehive due to earlier restore failure");
            return;
        }
        Set<File> imageFiles = new HashSet<File>();
        if (panels == null) {
            saveLog.warn("getAllImageNames(panels) was called with null argument (Account : {0})", acct.getOid());
        } else {
            for (Panel panel : panels) {
                Set<String> imageNames = Panel.getAllImageNames(panel);
                for (String imageName : imageNames) {
                    imageFiles.add(new File(imageName));
                }
            }
        }
        BeehiveService beehive = new Beehive30API(configuration);
        ResourceCache<File> fileCache = new LocalFileCache(configuration, user);
        try {
            fileCache.markInUseImages(imageFiles);
            beehive.uploadResources(fileCache.openReadStream(), user);
        } catch (NetworkException e) {
            saveLog.error("Save failed due to network error : " + e.getMessage());
            throw new UIRestoreException("Save failed due to network error to Beehive server. You may try again later. " + "If the issue persists, contact support (Error : " + e.getMessage() + ").", e);
        } catch (CacheOperationException e) {
            admin.alert("Can't save account data due to cache error. Account ID : {0}, User : {1}. " + "Error: {2}", e, user.getAccount().getOid(), printUserAccountLog(user), e.getMessage());
            throw new UIRestoreException("Saving your design failed due to a cache error. Administrators have been notified " + "of this issue.");
        } catch (ConfigurationException e) {
            admin.alert("Save failed for account {0} due to configuration error : {1}", e, user.getAccount().getOid(), e.getMessage());
            throw new UIRestoreException("Unable to save your data due to Designer configuration error. " + "Administrators have been notified of this issue. For further assistance, " + "please contact support. (Error : " + e.getMessage() + ")", e);
        } catch (Throwable t) {
            admin.alert("IMPLEMENTATION ERROR : {0}", t, t.getMessage());
            throw new UIRestoreException("Save failed due to Designer implementation error. Administrators have been notified " + "of this issue. For further assistance, please contact support. Error : " + t.getMessage(), t);
        }
    }

    protected PanelsAndMaxOid transformToPanelsAndMaxOid() {
        return new PanelsAndMaxOid(panels, maxOID);
    }

    /**
   * Attempts to deserialize a legacy binary panels.obj designer UI state serialization file.
   *
   * @param legacyPanelsObjFile   file path to legacy panels.obj serialization file
   *
   * @throws RestoreFailureException
   *            if deserialization fails for any reason
   */
    private void restoreLegacyDesignerUIState(File legacyPanelsObjFile) throws RestoreFailureException {
        ObjectInputStream ois = null;
        try {
            ois = openSerializationStream(legacyPanelsObjFile);
            this.panels = deserializePanels(ois);
            this.maxOID = deserializeMaxOID(ois);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                restoreLog.warn("Failed to close input stream to " + legacyPanelsObjFile.getAbsolutePath() + " : " + e.getMessage(), e);
            }
        }
    }

    /**
   * Deserializes a Java collection of panel instances from the legacy designer
   * serialization file.
   *
   * @param ois     object input stream for the file to deserialize -- assumes
   *                the stream is positioned for reading the collection object
   *
   * @return        deserialized instance of a collection with panel objects
   *
   * @throws RestoreFailureException
   *                if deserializing the collection fails for any reason
   */
    @SuppressWarnings("unchecked")
    private Collection<Panel> deserializePanels(ObjectInputStream ois) throws RestoreFailureException {
        try {
            return (Collection<Panel>) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RestoreFailureException("Deployment Error -- Cannot restore panel collection, class not found : " + e.getMessage(), e);
        } catch (InvalidClassException e) {
            throw new RestoreFailureException("Deployment Error -- Cannot restore panel collection, invalid class : " + e.classname, e);
        } catch (StreamCorruptedException e) {
            throw new RestoreFailureException("Corrupt panel collection serialization stream : " + e.getMessage(), e);
        } catch (OptionalDataException e) {
            throw new RestoreFailureException("Optional Data Exception : " + e.getMessage(), e);
        } catch (EOFException e) {
            throw new RestoreFailureException("Corrupted serialization file, reached end-of-file prematurely : " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RestoreFailureException("I/O Exception while reading panel collection serialization stream : " + e.getMessage(), e);
        }
    }

    /**
   * Deserialized a primitive long value from Java serialization stream.
   *
   * @param ois   object input stream for the file to deserialize -- assumes
   *              the stream is positioned for reading a long value.
   *
   * @return      the long value representing the max oid value in designer
   *              legacy serialization format
   *
   * @throws RestoreFailureException
   *              if deserializing the value fails for any reason
   */
    private Long deserializeMaxOID(ObjectInputStream ois) throws RestoreFailureException {
        try {
            return ois.readLong();
        } catch (EOFException e) {
            throw new RestoreFailureException("Attempt to read past end-of-file restoring max_oid value : " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RestoreFailureException("I/O Exception while reading max_oid from serialization stream : " + e.getMessage(), e);
        }
    }

    private void addContextLog() {
        LogFacade.addUserName(user.getUsername());
        LogFacade.addAccountID(user.getAccount().getOid());
    }

    private void removeContextLog() {
        LogFacade.removeUserName();
        LogFacade.removeAccountID();
    }

    private boolean hasXMLUIState() {
        return false;
    }

    private void haltAccount(String adminMessage, String userMessage) {
        haltAccount(adminMessage, userMessage, null);
    }

    private void haltAccount(String adminMessage, String userMessage, Throwable exception) {
        haltAccountSave.add(user.getAccount().getOid());
        if (exception != null) {
            admin.alert(adminMessage, exception);
            throw new UIRestoreException(userMessage, exception);
        } else {
            admin.alert(adminMessage);
            throw new UIRestoreException(userMessage);
        }
    }

    /**
   * TODO : should be part of cache implementation
   *
   * Detects the presence of legacy binary panels.obj designer UI state serialization file.
   *
   * @param pathConfig      Designer path configuration
   * @param panelsObjFile   file path to the legacy binary panels.objs UI state serialization file
   *
   * @return      true if the panels.obj file is present in local beehive archive cache folder,
   *              false otherwise
   *
   * @throws ConfigurationException
   *              if read access to the file system is denied for any reason
   */
    private boolean hasLegacyDesignerUIState(PathConfig pathConfig, File panelsObjFile) throws ConfigurationException {
        try {
            return panelsObjFile.exists();
        } catch (SecurityException e) {
            throw new ConfigurationException("Security manager denied access to " + panelsObjFile.getAbsoluteFile() + ". File read/write access must be enabled to " + pathConfig.tempFolder() + ".", e);
        }
    }

    /**
   * Helper for logging user information.
   *
   * TODO : should be reused via User domain object
   *
   * @param currentUser   current logged in user (as per the http session associated with this
   *                      thread)
   *
   * @return    string with user name, email, role and account id information
   */
    private String printUserAccountLog(User currentUser) {
        return "(User: " + currentUser.getUsername() + ", Email: " + currentUser.getEmail() + ", Roles: " + currentUser.getRole() + ", Account ID: " + currentUser.getAccount().getOid() + ")";
    }

    private static class ExecutionPerformance {

        private long startTime = 0;

        private LogFacade logger;

        private ExecutionPerformance(LogFacade.Category category) {
            this.logger = LogFacade.getInstance(category);
        }

        private void start() {
            startTime = System.currentTimeMillis();
        }

        private void end(String logMessage) {
            float time = (float) (System.currentTimeMillis() - startTime) / 1000;
            logger.info(logMessage, new DecimalFormat("######00.000").format(time) + " seconds.");
        }
    }

    private static class RestoreFailureException extends Exception {

        RestoreFailureException(String msg) {
            super(msg);
        }

        RestoreFailureException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
