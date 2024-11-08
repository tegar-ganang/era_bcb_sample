package eu.vph.predict.vre.in_silico.util.vfs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.FileTypeSelector;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.commons.vfs.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs.provider.gridftp.cogjglobus.GridFtpFileSystem;
import eu.vph.predict.vre.base.exception.MessageKeys;
import eu.vph.predict.vre.base.exception.VREBusinessException;
import eu.vph.predict.vre.base.exception.VRESystemException;
import eu.vph.predict.vre.base.util.io.IOUtil;
import eu.vph.predict.vre.in_silico.exception.InSilicoMessageKeys;
import eu.vph.predict.vre.in_silico.exception.simulation.VFSRetrievalException;
import eu.vph.predict.vre.in_silico.value.authentication.AbstractAuthenticationToken;
import eu.vph.predict.vre.in_silico.value.authentication.MyProxyAuthenticationToken;
import eu.vph.predict.vre.in_silico.value.configuration.AbstractConfiguration;
import eu.vph.predict.vre.in_silico.value.configuration.MdasEnvConfiguration;

/**
 * Commons-VFS utility class.
 *
 * @author Geoff Williams
 * @author David Meredith
 */
public class VFSUtil {

    private static final Map<FileType, String> FILE_TYPE_INFO = new HashMap<FileType, String>(4);

    private static final String GRIDFTP_SCHEME = "gsiftp";

    private static final Log log = LogFactory.getLog(VFSUtil.class);

    static {
        FILE_TYPE_INFO.put(FileType.FILE, InSilicoMessageKeys.MESSAGE_ISGENERAL_FILE);
        FILE_TYPE_INFO.put(FileType.FILE_OR_FOLDER, InSilicoMessageKeys.MESSAGE_ISGENERAL_FILEORFOLDER);
        FILE_TYPE_INFO.put(FileType.IMAGINARY, InSilicoMessageKeys.MESSAGE_ISGENERAL_IMAGINARY);
        FILE_TYPE_INFO.put(FileType.FOLDER, InSilicoMessageKeys.MESSAGE_ISGENERAL_FOLDER);
    }

    /**
   * Retrieve a thread-safe FileSystemManager (because <tt>VFS.getManager()</tt> invocation 
   *   retrieves a singleton which is no good).
   * 
   * @return Thread-safe FileSystemManager.
   */
    protected static FileSystemManager getFileSystemManager() {
        log.debug("~vfs.getFileSystemManager() : Retrieving thread-safe FileSystemManager");
        return fileSystemManager.get();
    }

    private static final ThreadLocal<FileSystemManager> fileSystemManager = new ThreadLocal<FileSystemManager>() {

        @Override
        protected FileSystemManager initialValue() {
            final StandardFileSystemManager fsManager = new StandardFileSystemManager();
            fsManager.setLogger(LogFactory.getLog(VFS.class));
            try {
                fsManager.init();
                final String[] schemes = fsManager.getSchemes();
                if (schemes == null) {
                    log.warn("~vfs.VFSUtil : No schemes defined");
                } else {
                    final StringBuffer schemeList = new StringBuffer();
                    for (final String scheme : schemes) schemeList.append(scheme).append(" ");
                    log.debug("~vfs.VFSUtil : Available schemes [" + schemeList.toString().trim() + "]");
                }
            } catch (FileSystemException fsException) {
                log.error("~vfs.VFSUtil : Failed to initialise the FileSystemManager [" + fsException.getMessage() + "]");
                fsException.printStackTrace();
            }
            return fsManager;
        }
    };

    private static boolean confirmVFSFileType(final FileObject fileObject, final FileType expectedFileType) throws FileSystemException {
        assert (fileObject != null) : "Inappropriate attempt to confirm VFS FileType using null file object";
        assert (expectedFileType != null) : "Inappropriate attempt to confirm VFS FileType using null expected file type";
        final FileType providedFileType = fileObject.getType();
        if (providedFileType.equals(expectedFileType)) {
            log.debug("~vfs.confirmVFSFileType(FileObject, FileType) : FileType confirmed");
            return true;
        } else {
            if (providedFileType.equals(FileType.FILE)) {
                log.debug("~vfs.confirmVFSFileType(FileObject, FileType) : FileObject was FILE");
            } else if (providedFileType.equals(FileType.FILE_OR_FOLDER)) {
                log.debug("~vfs.confirmVFSFileType(FileObject, FileType) : FileObject was FILE_OR_FOLDER");
            } else if (providedFileType.equals(FileType.FOLDER)) {
                log.debug("~vfs.confirmVFSFileType(FileObject, FileType) : FileObject was FOLDER");
            } else if (providedFileType.equals(FileType.IMAGINARY)) {
                log.debug("~vfs.confirmVFSFileType(FileObject, FileType) : FileObject was IMAGINARY (doesn't exist)");
            } else {
                log.debug("~vfs.confirmVFSFileType(FileObject, FileType) : FileObject wasn't a known type");
            }
            return false;
        }
    }

    /**
   * Copy the content of srcFo to destFo.
   * <p/>
   * 1) if srcFo is a file and destFo is a dir, then copy srcFo INTO destTo.
   * 2) if srcFo is a file and destFo is a file, then OVERWRITE destFo.
   * 3) if srcFo is a file and destFo is IMAGINARY, then create destFo file.
   * 4) if srcFo is a dir and destFo is a dir, then copy children of srcFo INTO destFo (*)
   * 5) if srcFo is a dir and destFo is IMAGINARY, then create destFo dir.
   * 6) if srcFo is a dir and destFo is a file, then throw IOException
   *
   *   <p>
   *   Copying the children of the srcFo directory into the destFo dir
   *   is preferable to copying the srcFo dir inclusive into the destFo dir.
   *   This is because the user may want to provide an alternative base
   *   name for the destFo dir (e.g. 'dirAcopy' rather than 'dirA').
   *   Do the following:
   *      destFo = obj.resolveFile("newDirName");
   *      if(!destFo.exists()) destFo.createFolder();
   *   </p>
   *
   * @param srcFo the source file to copy from
   * @param destFo the destination file to create/overwrite
   * @param doThirdPartyTransferForTwoGridFtpFileObjects If true and both srcFo
   * and destFo are gsiftp uris, then attempt a gridftp third party file transfer
   * (requires cog.properties), otherwise do byte IO (byte streaming)
   * between srcFo and destFo.
   *
   * @throws IOException if an error occurs on byte transfer
   * @throws org.apache.commons.vfs.FileSystemException if srcFo does not exist,
   * if srcFo is a directory and destFo exists and is a file,
   * if destFo is not writable
   *
   * @author David Meredith
   */
    public static void copy(final FileObject srcFo, final FileObject destFo, boolean doThirdPartyTransferForTwoGridFtpFileObjects) throws IOException, FileSystemException {
        if (!srcFo.exists()) throw new FileSystemException("vfs.provider/copy-missing-file.error", srcFo);
        if (!srcFo.isReadable()) throw new FileSystemException("vfs.provider/read-not-readable.error", srcFo);
        if (destFo.getType() == FileType.IMAGINARY || !destFo.exists()) {
            if (srcFo.getType().equals(FileType.FILE)) {
                destFo.createFile();
            } else if (srcFo.getType().equals(FileType.FOLDER)) {
                destFo.createFolder();
            }
        }
        if (!destFo.isWriteable()) throw new FileSystemException("vfs.provider/copy-read-only.error", destFo);
        if (destFo.getName().getURI().equals(srcFo.getName().getURI())) throw new FileSystemException("vfs.provider/copy-file.error", new Object[] { srcFo, destFo }, null);
        if (doThirdPartyTransferForTwoGridFtpFileObjects && srcFo.getName().getScheme().equalsIgnoreCase("gsiftp") && destFo.getName().getScheme().equalsIgnoreCase("gsiftp")) {
            throw new UnsupportedOperationException();
        }
        if (srcFo.getType().equals(FileType.FILE)) {
            if (destFo.getType().equals(FileType.FOLDER)) {
                log.debug("~vfs.copy(..) : vfs FILE into FOLDER");
                FileObject nestedDestFo = destFo.resolveFile(srcFo.getName().getBaseName());
                nestedDestFo.copyFrom(srcFo, new AllFileSelector());
            } else {
                log.debug("~vfs.copy(..) : vfs FILE to FILE");
                destFo.copyFrom(srcFo, new AllFileSelector());
            }
        } else if (srcFo.getType().equals(FileType.FOLDER)) {
            if (destFo.getType().equals(FileType.FOLDER)) {
                log.debug("~vfs.copy(..) : vfs FOLDER children into FOLDER");
                destFo.copyFrom(srcFo, new AllFileSelector());
            } else {
                throw new IOException("Cannot copy a folder to a destination that is not a folder");
            }
        } else {
            throw new IOException("Cannot copy from path of type " + srcFo.getType() + " to another path of type " + destFo.getType());
        }
    }

    /**
   * 
   * 
   * @param sourceDirectory
   * @param destinationDirectory
   * @param required
   * @throws VREBusinessException
   */
    public static void copyDirectory(final String sourceDirectory, final String destinationDirectory, final boolean required, final boolean hack) throws VREBusinessException {
        log.debug("~vfs.copyDirectory(..) : Instruction copy [" + sourceDirectory + "] to [" + destinationDirectory + "]");
        final FileSystemManager fileSystemManager = getFileSystemManager();
        try {
            final FileObject sourceFolder = fileSystemManager.resolveFile(sourceDirectory);
            if (confirmVFSFileType(sourceFolder, FileType.FOLDER)) {
                log.debug("~vfs.copyDirectory(..) : About to perform copy process");
                String destination = destinationDirectory;
                if (hack) destination = destinationDirectory.concat("testoutput/");
                final FileObject destinationFolder = fileSystemManager.resolveFile(destination);
                try {
                    copy(sourceFolder, destinationFolder, false);
                } catch (IOException e) {
                    log.warn("~vfs.copyDirectory(..) : Exception copying directories [" + e.getMessage() + "]");
                    e.printStackTrace();
                    throw new VREBusinessException(MessageKeys.DATA_INVALID, new Object[] { e.getMessage() });
                }
            } else {
                if (required) throw new VREBusinessException(InSilicoMessageKeys.VFS_FILE_TYPE_MISMATCH, new Object[] { FILE_TYPE_INFO.get(sourceFolder.getType()), FILE_TYPE_INFO.get(FileType.FOLDER), sourceFolder });
            }
        } catch (FileSystemException fse) {
            log.debug("~vfs.copyDirectory(..) : Exception [" + fse.getMessage() + "]");
            fse.printStackTrace();
            throw new VREBusinessException(MessageKeys.DATA_INVALID, new Object[] { fse.getMessage() });
        }
    }

    /**
   * Create a directory
   * 
   * @param directoryName
   * @param overwrite
   * @return Flag to indicate creation success or otherwise
   */
    public static boolean createDirectory(final String directoryName, final boolean overwrite) {
        assert (directoryName != null) : "Inappropriate attempt to create a null directory";
        log.debug("~vfs.createDirectory(String, boolean) : Instruction to create directory [" + directoryName + "]");
        final File directory = new File(directoryName);
        if (directory.exists()) {
            log.debug("~vfs.createDirectory(String, boolean) : Directory exists");
            if (overwrite) {
                log.debug("~vfs.createDirectory(String, boolean) : Going to overwrite it");
            } else {
                log.debug("~vfs.createDirectory(String, boolean) : Instructed not to overwrite it");
            }
        } else {
            log.debug("~vfs.createDirectory(String, boolean) : Directory doesn't exist");
            final boolean created = directory.mkdirs();
            if (created) {
                log.debug("~vfs.createDirectory(String, boolean) : Directory created");
            } else {
                log.debug("~vfs.createDirectory(String, boolean) : Failed to create directory (although some subdirs may have been created!)");
                return false;
            }
        }
        return true;
    }

    /**
   * Create a directory via GridFTP.
   * 
   * @param directoryName
   * @param uri
   * @param overwrite
   * @param authenticationToken
   * @return
   */
    public static boolean createDirectory(final String directoryName, final URI uri, final boolean overwrite, final AbstractAuthenticationToken authenticationToken) {
        MyProxyAuthenticationToken myProxyAuthenticationToken = null;
        if (authenticationToken instanceof MyProxyAuthenticationToken) {
            myProxyAuthenticationToken = (MyProxyAuthenticationToken) authenticationToken;
            log.debug("~vfs.createDirectory(..) : There's a MyProxy [" + myProxyAuthenticationToken + "]");
        }
        final String processingDirectoryName = retrieveProcessingDirectory(directoryName, uri, myProxyAuthenticationToken);
        final FileSystemOptions fileSystemOptions = FileSystemOptionsUtil.retrieveGridFTPFileSystemOptions(myProxyAuthenticationToken);
        final FileSystemManager fileSystemManager = getFileSystemManager();
        log.debug("~vfs.createDirectory(..) : Creating directory [" + processingDirectoryName + "]");
        FileObject processingDirectory = null;
        try {
            processingDirectory = fileSystemManager.resolveFile(processingDirectoryName, fileSystemOptions);
            if (processingDirectory.exists()) {
                log.debug("~vfs.createDirectory(..) : Directory exists");
                if (!overwrite) {
                    log.debug("~vfs.createDirectory(..) : Instructed not to overwrite - aborting");
                    return false;
                }
            } else {
                log.debug("~vfs.createDirectory(..) : Creating processing folder");
                processingDirectory.createFolder();
                log.debug("~vfs.createDirectory(..) : Processing folder created");
            }
        } catch (FileSystemException fse) {
            fse.printStackTrace();
            log.debug("~vfs.createDirectory(..) : Couldn't resolve processing directory");
            return false;
        }
        return true;
    }

    public static String hackPerformRetrieveFile(final String source, final AbstractAuthenticationToken authenticationToken, final AbstractConfiguration configuration, final boolean required) throws VFSRetrievalException {
        log.debug("~vfs.hackPerformRetrieveFile(..) : Going to try to retrieve file");
        MyProxyAuthenticationToken myProxyAuthenticationToken = null;
        if (authenticationToken instanceof MyProxyAuthenticationToken) {
            myProxyAuthenticationToken = (MyProxyAuthenticationToken) authenticationToken;
            log.debug("~vfs.hackPerformRetrieveFile(..) : There's a MyProxy [" + myProxyAuthenticationToken.toString() + "]");
        }
        MdasEnvConfiguration mdasEnvConfiguration = null;
        if (configuration instanceof MdasEnvConfiguration) {
            mdasEnvConfiguration = (MdasEnvConfiguration) configuration;
            log.debug("~vfs.hackPerformRetrieveFile(..) : There's an MdasEnv [" + mdasEnvConfiguration.toString() + "]");
        }
        if (myProxyAuthenticationToken != null && mdasEnvConfiguration != null) {
            log.debug("~vfs.hackPerformRetrieveFile(..) : Going to try to retrieve file from SRB");
            final FileSystemOptions fileSystemOptions = FileSystemOptionsUtil.retrieveSRBFileSystemOptions(myProxyAuthenticationToken, mdasEnvConfiguration);
            final FileSystemManager fileSystemManager = getFileSystemManager();
            try {
                final FileObject sourceFile = fileSystemManager.resolveFile(source, fileSystemOptions);
                if (sourceFile == null) {
                    log.debug("~vfs.hackPerformRetrieveFile(..) : Resolved file was null");
                    return null;
                }
                if (confirmVFSFileType(sourceFile, FileType.FILE)) {
                    final FileContent fileContent = sourceFile.getContent();
                    final String retrievedString = IOUtil.convertInputStreamToString(fileContent.getInputStream(), null);
                    log.debug("~vfs.hackPerformRetrieveFile(..) : Retrieved from SRB [" + retrievedString + "]");
                    return retrievedString;
                } else {
                    log.debug("~vfs.hackPerformRetrieveFile(..) : Source [" + source + "] wasn't a file, it was [" + sourceFile.getType() + "]!");
                }
            } catch (FileSystemException fse) {
                log.warn("~vfs.hackPerformRetrieveFile(..) : FileSystemException [" + fse.getMessage() + "]!");
                fse.printStackTrace();
            }
        } else {
            log.debug("~vfs.hackPerformRetrieveFile(..) : Combination unrecognised");
        }
        return null;
    }

    /**
   * Confirm that a resource can be accessed, i.e. exists and can be read.
   * 
   * @param resourceName Resource name
   * @return Flag to indicate access or otherwise
   * @throws VRESystemException If resource cannot be accessed.
   */
    public static boolean isResourceAccessible(final String resourceName) {
        log.debug("~vfs.resourceAccessible(String) : Confirming resource [" + resourceName + "] existence");
        final FileSystemManager fileSystemManager = getFileSystemManager();
        try {
            final FileObject resource = fileSystemManager.resolveFile(resourceName);
            if (!resource.exists()) {
                log.warn("~vfs.resourceAccessible(String) : Resource [" + resourceName + "] does not exist");
                return false;
            }
            if (!resource.isReadable()) {
                log.warn("~vfs.resourceAccessible(String) : Resource [" + resourceName + "] cannot be read");
                return false;
            }
        } catch (FileSystemException fse) {
            log.warn("~vfs.resourceAccessible(String) : Resource [" + resourceName + "] generated [" + fse.getMessage() + "]");
            return false;
        }
        return true;
    }

    /**
   * Retrieve the artifact from whereever the source indicates and return a string representation
   *   of whatever was found.
   * 
   * @param source Source location of the artifact.
   * @param required If the artifact must exist.
   * @return String representation of the source artifact.
   * @throws VFSRetrievalException If input is required but not found.
   */
    public static String performRetrieveFileFromFTP(final String source, final boolean required) throws VFSRetrievalException {
        log.debug("~vfs.performRetrieveFileFromFTP(String, boolean) : Retrieving [" + source + "]");
        final FileSystemManager fileSystemManager = getFileSystemManager();
        log.debug("~vfs.performRetrieveFileFromFTP(String, boolean) : Retrieved FileSystemManager");
        String problemMessage = null;
        String retrievedString = null;
        try {
            final FileSystemOptions fileSystemOptions = new FileSystemOptions();
            FtpFileSystemConfigBuilder.getInstance().setSoTimeout(fileSystemOptions, 10000);
            FtpFileSystemConfigBuilder.getInstance().setSoTimeout(fileSystemOptions, 10000);
            FtpFileSystemConfigBuilder.getInstance().setDataTimeout(fileSystemOptions, 10000);
            final FileObject sourceFile = fileSystemManager.resolveFile(source, fileSystemOptions);
            log.debug("~vfs.performRetrieveFileFromFTP(String, boolean) : File object [" + sourceFile + "] resolved");
            if (confirmVFSFileType(sourceFile, FileType.FILE)) {
                log.debug("~vfs.performRetrieveFileFromFTP(String, boolean) : File object [" + sourceFile + "] is a file");
                final FileContent fileContent = sourceFile.getContent();
                log.debug("~vfs.performRetrieveFileFromFTP(String, boolean) : File object content retrieved");
                retrievedString = IOUtil.convertInputStreamToString(fileContent.getInputStream(), null);
            } else {
                if (sourceFile.getType() == FileType.IMAGINARY) {
                    problemMessage = "Are you sure [" + source + "] really exists? The system doesn't think so!!";
                    log.warn("~vfs.performRetrieveFileFromFTP(String, boolean) : " + problemMessage);
                } else {
                    final String errorMsg = "Invalid source type [" + sourceFile.getType() + "] for [" + source + "]";
                    log.warn("~vfs.performRetrieveFileFromFTP(String, boolean) : " + errorMsg);
                    throw new VRESystemException(MessageKeys.DATA_INVALID, new Object[] { errorMsg });
                }
            }
        } catch (FileSystemException fse) {
            if (fse.getCause() != null) {
                if (fse.getCause() instanceof java.net.ConnectException) {
                    problemMessage = "ConnectException (firewall rejection (failed quickly) / drop (failed slowly) !?) for [" + source + "][" + fse.getMessage() + "]";
                    log.warn("~vfs.performRetrieveFileFromFTP(String, boolean) : " + problemMessage);
                } else if (fse.getCause() instanceof java.net.NoRouteToHostException) {
                    problemMessage = "NoRouteToHostException (host seems valid, but can't talk to it!?) for [" + source + "][" + fse.getMessage() + "]";
                    log.warn("~vfs.performRetrieveFileFromFTP(String, boolean) : " + problemMessage);
                } else if (fse.getCause() instanceof java.net.UnknownHostException) {
                    problemMessage = "UnknownHostException (incorrect hostname!?) for [" + source + "][" + fse.getMessage() + "]";
                    log.warn("~vfs.performRetrieveFileFromFTP(String, boolean) : " + problemMessage);
                } else {
                    problemMessage = "Unknown failure cause for [" + source + "][" + fse.getMessage() + "]";
                    log.error("~vfs.performRetrieveFileFromFTP(String, boolean) : " + problemMessage);
                    fse.printStackTrace();
                }
            } else {
                problemMessage = "Unknown failure + no cause given for [" + source + "]";
                log.error("~vfs.performRetrieveFileFromFTP(String, boolean) : " + problemMessage);
                fse.printStackTrace();
            }
        }
        if (required && (retrievedString == null)) throw new VFSRetrievalException(MessageKeys.DATA_INVALID, new Object[] { problemMessage });
        return retrievedString;
    }

    /**
   * Retrieve a file and place it in a directory
   * 
   * @param source The full path to the remote file
   * @param destination Where to place the file (including file name).
   * @param required Where required or not
   * @return
   * @throws VREBusinessException
   */
    public static Object retrieveFileToDirectory(final String source, final String destination, final boolean required) throws VREBusinessException {
        log.debug("~vfs.retrieveFileToDirectory(..) : Instruction retrieve [" + source + "] to [" + destination + "]");
        final FileSystemManager fileSystemManager = getFileSystemManager();
        try {
            final FileObject sourceFile = fileSystemManager.resolveFile(source);
            if (confirmVFSFileType(sourceFile, FileType.FILE)) {
                log.debug("~vfs.retrieveFileToDirectory(..) : About to perform copy process");
                final FileObject destinationFile = fileSystemManager.resolveFile(destination);
                destinationFile.copyFrom(sourceFile, new FileTypeSelector(FileType.FILE));
                log.debug("~vfs.retrieveFileToDirectory(..) : Copied");
            } else {
                if (required) throw new VREBusinessException(InSilicoMessageKeys.VFS_FILE_TYPE_MISMATCH, new Object[] { FILE_TYPE_INFO.get(sourceFile.getType()), FILE_TYPE_INFO.get(FileType.FILE), source });
            }
        } catch (FileSystemException fse) {
            log.warn("~vfs.retrieveFileToDirectory(..) : Exception [" + fse.getMessage() + "]");
            fse.printStackTrace();
            if (required) throw new VREBusinessException(MessageKeys.DATA_INVALID, new Object[] { source + "->" + destination });
        }
        return null;
    }

    /**
   * 
   * 
   * @param source
   * @param directoryName
   * @param uri
   * @param authenticationToken
   * @param overwrite
   * @return
   */
    public static boolean retrieveFileToDirectory(final String source, final String directoryName, final URI uri, final AbstractAuthenticationToken authenticationToken, final boolean overwrite) {
        MyProxyAuthenticationToken myProxyAuthenticationToken = null;
        if (authenticationToken instanceof MyProxyAuthenticationToken) {
            myProxyAuthenticationToken = (MyProxyAuthenticationToken) authenticationToken;
            log.debug("~vfs.retrieveFileToDirectory(..) : There's a MyProxy [" + myProxyAuthenticationToken + "]");
        }
        final String processingDirectoryName = retrieveProcessingDirectory(directoryName, uri, myProxyAuthenticationToken);
        final FileSystemOptions fileSystemOptions = FileSystemOptionsUtil.retrieveGridFTPFileSystemOptions(myProxyAuthenticationToken);
        final FileSystemManager fileSystemManager = getFileSystemManager();
        final String processingFileName = processingDirectoryName.concat("/").concat("Chaste");
        try {
            log.debug("~vfs.retrieveFileToDirectory(..) : About to resolve [" + source + "]");
            final FileObject sourceFile = fileSystemManager.resolveFile(source);
            log.debug("~vfs.retrieveFileToDirectory(..) : About to resolve [" + processingFileName + "]");
            final FileObject destinationFile = fileSystemManager.resolveFile(processingFileName, fileSystemOptions);
            log.debug("~vfs.retrieveFileToDirectory(..) : About to copy file");
            destinationFile.copyFrom(sourceFile, new FileTypeSelector(FileType.FILE));
            log.debug("~vfs.retrieveFileToDirectory(..) : File copied");
        } catch (FileSystemException fse) {
            log.warn("~vfs.retrieveFileToDirectory(..) : Exception [" + fse.getMessage() + "]");
            fse.printStackTrace();
        }
        return true;
    }

    /**
   * 
   * 
   * @param directoryName
   * @param uri
   * @param myProxyAuthenticationToken
   * @return
   */
    protected static String retrieveProcessingDirectory(final String directoryName, final URI uri, final MyProxyAuthenticationToken myProxyAuthenticationToken) {
        final FileSystemOptions fileSystemOptions = FileSystemOptionsUtil.retrieveGridFTPFileSystemOptions(myProxyAuthenticationToken);
        final FileSystemManager fileSystemManager = getFileSystemManager();
        FileObject fileObject = null;
        try {
            fileObject = fileSystemManager.resolveFile(uri.toString(), fileSystemOptions);
        } catch (FileSystemException fse) {
            fse.printStackTrace();
            log.debug("~vfs.createDirectory(..) : Exception resolving URI path [" + uri.toString() + "]");
            return null;
        }
        String homeDirectoryName = null;
        if (uri.getScheme().equals(GRIDFTP_SCHEME)) {
            try {
                homeDirectoryName = (String) fileObject.getFileSystem().getAttribute(GridFtpFileSystem.HOME_DIRECTORY);
                log.debug("~vfs.createDirectory(..) : Home directory name [" + homeDirectoryName + "]");
            } catch (FileSystemException fse) {
                fse.printStackTrace();
                log.debug("~vfs.createDirectory(..) : Exception retrieving home directory name");
                return null;
            }
        }
        return uri.toString().concat(homeDirectoryName).concat("/").concat(directoryName);
    }

    private static OutputStream retrieveOutputStream(final String fileName, final StringBuffer problemMessage) {
        log.debug("~retrieveOutputStream(String) : Retrieving output stream for [" + fileName + "]");
        OutputStream outputStream = null;
        final FileSystemManager fileSystemManager = getFileSystemManager();
        try {
            log.debug("~vfs.writeStringToFile(..) : Creating the destination file");
            final FileObject destinationFile = fileSystemManager.resolveFile(fileName);
            destinationFile.createFile();
            outputStream = destinationFile.getContent().getOutputStream();
            log.debug("~vfs.writeStringToFile(..) : Destination file ready!");
        } catch (FileSystemException fse) {
            fse.printStackTrace();
            if (fse.getCause() != null) {
                log.warn("~vfs.writeStringToFile(..) : Could not create local directory file");
                problemMessage.append(fse.getMessage());
            }
        }
        return outputStream;
    }

    /**
   * Write a string representation to an output.
   * 
   * @param fileName
   * @param fileContent
   * @throws VFSRetrievalException
   */
    public static void writeStringToFile(final String fileName, final String fileContent) throws VFSRetrievalException {
        assert (fileName != null) : "Inappropriate attempt to write a null file name to local directory";
        log.debug("~vfs.writeStringToFile(..) : Instructed to write [" + fileName + "]");
        final StringBuffer problemMessage = new StringBuffer();
        final OutputStream outputStream = retrieveOutputStream(fileName, problemMessage);
        if (outputStream != null) {
            try {
                log.debug("~vfs.writeStringToFile(..) : Preparing the write process");
                outputStream.write(fileContent.getBytes());
                outputStream.flush();
                outputStream.close();
                log.debug("~vfs.writeStringToFile(..) : Content written");
            } catch (IOException ioe) {
                ioe.printStackTrace();
                problemMessage.append(problemMessage.length() > 0 ? "\n" : "").append(ioe.getMessage());
                log.warn("~vfs.writeStringToFile(..) : Could not write to local directory file");
            }
        }
        if (problemMessage.length() > 0) throw new VFSRetrievalException(MessageKeys.DATA_INVALID, new Object[] { problemMessage.toString() });
    }

    /**
   * Write a string representation to an output.
    VFSUtil.writeStringToFile(directoryName, uri,
                              InputDeterminatorChaste.CHASTE_PARAMETERS_XML_NAME,
                              chasteParametersXML);   * 
                              
                              final String directoryName, final URI uri, 
                                        final boolean overwrite, 
                                        final AbstractAuthenticationToken authenticationToken
   * @param fileName
   * @param fileContent
   * @throws VFSRetrievalException
   */
    public static void writeStringToFile(final String directoryName, final URI uri, final String fileName, final String fileContent, final AbstractAuthenticationToken authenticationToken) throws VFSRetrievalException {
        assert (fileName != null) : "Inappropriate attempt to write a null file name to local directory";
        MyProxyAuthenticationToken myProxyAuthenticationToken = null;
        if (authenticationToken instanceof MyProxyAuthenticationToken) {
            myProxyAuthenticationToken = (MyProxyAuthenticationToken) authenticationToken;
            log.debug("~vfs.writeStringToFile(..) : There's a MyProxy [" + myProxyAuthenticationToken + "]");
        }
        final String processingDirectoryName = retrieveProcessingDirectory(directoryName, uri, myProxyAuthenticationToken);
        final FileSystemOptions fileSystemOptions = FileSystemOptionsUtil.retrieveGridFTPFileSystemOptions(myProxyAuthenticationToken);
        final FileSystemManager fileSystemManager = getFileSystemManager();
        final String processingFileName = processingDirectoryName.concat("/").concat(fileName);
        log.debug("~vfs.writeStringToFile(..) : Instructed to write [" + processingFileName + "]");
        String problemMessage = null;
        OutputStream outputStream = null;
        try {
            log.debug("~vfs.writeStringToFile(..) : Creating the destination file");
            final FileObject destinationFile = fileSystemManager.resolveFile(processingFileName, fileSystemOptions);
            destinationFile.createFile();
            outputStream = destinationFile.getContent().getOutputStream();
            log.debug("~vfs.writeStringToFile(..) : Destination file ready!");
        } catch (FileSystemException fse) {
            fse.printStackTrace();
            if (fse.getCause() != null) {
                problemMessage = fse.getCause().getMessage();
                log.warn("~vfs.writeStringToFile(..) : Could not create local directory file");
            }
        }
        if (outputStream != null) {
            try {
                log.debug("~vfs.writeStringToFile(..) : Preparing the write process");
                outputStream.write(fileContent.getBytes());
                outputStream.flush();
                outputStream.close();
                log.debug("~vfs.writeStringToFile(..) : Content written");
            } catch (IOException ioe) {
                ioe.printStackTrace();
                problemMessage = ioe.getMessage();
                log.warn("~vfs.writeStringToFile(..) : Could not write to local directory file");
            }
        }
        if (problemMessage != null) throw new VFSRetrievalException(MessageKeys.DATA_INVALID, new Object[] { problemMessage });
    }
}
