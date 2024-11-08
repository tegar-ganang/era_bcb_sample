package com.pallas.unicore.connection;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.net.ConnectException;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import org.apache.oro.io.GlobFilenameFilter;
import org.unicore.User;
import org.unicore.ajo.AbstractJob;
import org.unicore.ajo.DeclarePortfolio;
import org.unicore.ajo.Portfolio;
import org.unicore.outcome.PutPortfolio_Outcome;
import org.unicore.upl.ConsignJob;
import org.unicore.upl.ConsignJobReply;
import org.unicore.upl.Reply;
import org.unicore.upl.Request;
import org.unicore.upl.RetrieveOutcomeReply;
import org.unicore.upl.UnicoreResponse;
import org.unicore.utility.ConsignForm;
import org.unicore.utility.PacketisedInputStream;
import org.unicore.utility.PacketisedOutputStream;
import com.pallas.unicore.client.UserDefaults;
import com.pallas.unicore.container.TaskContainer;
import com.pallas.unicore.extensions.FileExport;
import com.pallas.unicore.extensions.FileImport;
import com.pallas.unicore.extensions.FileStorage;
import com.pallas.unicore.resourcemanager.OutcomeEntry;
import com.pallas.unicore.resourcemanager.OutcomeManager;
import com.pallas.unicore.resourcemanager.ResourceManager;

/**
 *  Class that manages connections between client and NJS
 *
 *@author     Thomas Kentemich
 *@author     Ralf Ratering
 *@version    $Id: Connection.java,v 1.3 2005/04/20 08:08:56 bschuller Exp $
 */
public class Connection {

    protected static int globalCount = 0;

    protected static Logger logger = Logger.getLogger("com.pallas.unicore.connection");

    protected static String sessionDir;

    private static final String UC_ITERATION_COUNTS_REGEX = "\\$UC_ITERATION_COUNTS";

    private int count = globalCount++;

    protected FileExport directOutput;

    private int errorcode;

    private ObjectInputStream inputStream;

    protected boolean isAscii;

    private long lifeTime = 3600000;

    private ObjectOutputStream outputStream;

    private static final int NOT_READY = 0, READY = 1, WRITTEN = 2, READ = 3, CLOSED = 4;

    private Reply reply;

    private UnicoreResponse response;

    private Socket socket;

    private UnicoreSSLSocketFactory socketFactory;

    private int state;

    private String target;

    private User user;

    /**
	 * get session directory
	 * 
	 * @return session directory
	 */
    public static String getSessionDir() {
        return sessionDir;
    }

    /**
	 * Default constructor
	 *  
	 */
    public Connection() {
    }

    /**
	 * Constructor
	 * 
	 * @param target
	 *            Address of the target Gateway
	 * @param user
	 *            User object
	 * @throws IOException
	 * @throws StreamCorruptedException
	 * @throws ConnectException
	 * @throws SSLPeerUnverifiedException
	 */
    public Connection(String target, User user) throws IOException, StreamCorruptedException, ConnectException, SSLPeerUnverifiedException {
        logger.info(" [" + count + "] " + "Open connection: " + target);
        if (ResourceManager.getUserDefaults().isSocksEnabled()) {
            String portString = new Integer(ResourceManager.getUserDefaults().getSocksPort()).toString();
            System.setProperty("socksProxyHost", ResourceManager.getUserDefaults().getSocksHost());
            System.setProperty("socksProxyPort", portString);
        } else {
            System.setProperty("socksProxyHost", "");
            System.setProperty("socksProxyPort", "");
        }
        this.user = user;
        socketFactory = new UnicoreSSLSocketFactory(user);
        this.target = target;
        this.socket = socketFactory.createSocket(target);
        this.socket.setSoTimeout(ResourceManager.getUserDefaults().getSocketTimeout());
        Certificate[] serverCert = ((SSLSocket) socket).getSession().getPeerCertificates();
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        state = READY;
        if (sessionDir == null) {
            UserDefaults userDefaults = ResourceManager.getUserDefaults();
            sessionDir = new String("Session" + ResourceManager.getNextObjectIdentifier());
            String scratchDir = userDefaults.getTmpDirectory();
            if (scratchDir == null || scratchDir.equals("")) {
                scratchDir = userDefaults.getUnicoreDir();
            }
            sessionDir = scratchDir + File.separator + sessionDir;
        }
    }

    /**
	 * Constructor
	 * 
	 * @param target
	 *            Address of the target Gateway
	 * @param user
	 *            User object
	 * @param directOutput
	 *            directly write one export to the destionation dir
	 * @param isAscii
	 *            direct export is ascii or not
	 * @throws IOException
	 * @throws StreamCorruptedException
	 * @throws ConnectException
	 * @throws SSLPeerUnverifiedException
	 */
    public Connection(String target, User user, FileExport directOutput, boolean isAscii) throws IOException, StreamCorruptedException, ConnectException, SSLPeerUnverifiedException {
        this(target, user);
        this.directOutput = directOutput;
        this.isAscii = isAscii;
    }

    /**
	 * Close the connection.
	 * 
	 * @exception IOException
	 *                Description of Exception
	 */
    public void close() throws IOException {
        if (state != READY) {
            return;
        }
        logger.fine(" [" + count + "] " + "Close connection: " + target);
        if (outputStream != null) {
            outputStream.close();
        }
        if (inputStream != null) {
            inputStream.close();
        }
        state = CLOSED;
    }

    private void copyFile(ZipInputStream zis) throws FileNotFoundException, IOException {
        Vector targetFiles = directOutput.getTargetFiles();
        String targetName = directOutput.getDestinationName();
        logger.fine("Direct copy to: " + targetName + " PF: " + directOutput.getSpoolPortfolio().getUPLDirectoryName());
        ZipEntry entry = zis.getNextEntry();
        String zipName = null;
        String realName = null;
        String uplName = null;
        PutPortfolio_Outcome ppo = null;
        String iteration = "";
        Vector spoolOutcomes = OutcomeManager.getOutcomeEntries(directOutput.getSpoolTask());
        if (spoolOutcomes.size() <= 0) {
            logger.severe("Cannot find outcome of spool task for file: " + directOutput.getSourceName());
            return;
        }
        while (entry != null) {
            zipName = entry.getName().replace('/', File.separatorChar);
            logger.fine("Receiving streamed file (direct output)" + zipName);
            for (int i = 0; i < spoolOutcomes.size(); i++) {
                ppo = (PutPortfolio_Outcome) ((OutcomeEntry) spoolOutcomes.elementAt(i)).getOutcome();
                uplName = (new Portfolio("", ppo.getPortfolio().getValue())).getUPLDirectoryName();
                if (zipName.lastIndexOf(uplName) > 0) {
                    iteration = ((OutcomeEntry) spoolOutcomes.elementAt(i)).getIteration();
                    break;
                }
            }
            realName = zipName.substring(zipName.lastIndexOf(uplName));
            realName = realName.substring(realName.indexOf(File.separator) + 1);
            targetName = directOutput.getDestinationName().replaceAll(UC_ITERATION_COUNTS_REGEX, iteration);
            File destination = new File(targetName);
            if (destination.exists()) {
                if (destination.isDirectory()) {
                    if (!targetName.endsWith(File.separator)) {
                        targetName = targetName + File.separator;
                    }
                    realName = targetName + realName;
                    File destDir = new File(realName.substring(0, realName.lastIndexOf(File.separator)));
                    destDir.mkdirs();
                } else {
                    realName = targetName;
                }
            } else {
                if (realName.indexOf(File.separator) > 0) {
                    if (!targetName.endsWith(File.separator)) {
                        targetName = targetName + File.separator;
                    }
                    logger.fine("non exixting  target:" + realName);
                    String sourceDir = realName.substring(0, realName.indexOf(File.separator));
                    realName = targetName + realName.substring(realName.indexOf(File.separator) + 1);
                    File destDir = new File(realName.substring(0, realName.lastIndexOf(File.separator)));
                    destDir.mkdirs();
                    if (this.directOutput.getTargetFilesFlag() == FileExport.CHOOSE_OVERWRITTEN_FILES) {
                        if (targetFiles.contains(realName)) {
                            copyFileFromZip(realName, zis);
                        }
                    } else {
                        copyFileFromZip(realName, zis);
                    }
                    entry = zis.getNextEntry();
                    while (entry != null) {
                        zipName = entry.getName().replace('/', File.separatorChar);
                        logger.fine("Receiving streamed file (direct output)" + zipName);
                        realName = zipName.substring(zipName.indexOf(directOutput.getSpoolPortfolio().getUPLDirectoryName()));
                        realName = realName.substring(realName.indexOf(File.separator) + 1);
                        if (!realName.startsWith(sourceDir)) {
                            logger.severe("Multiple directories in zip stream are not allowed");
                            throw new IOException("Multiple directorie in zip stream are not allowed");
                        }
                        realName = targetName + realName.substring(realName.indexOf(File.separator) + 1);
                        destDir = new File(realName.substring(0, realName.lastIndexOf(File.separator)));
                        destDir.mkdirs();
                        if (this.directOutput.getTargetFilesFlag() == FileExport.CHOOSE_OVERWRITTEN_FILES) {
                            if (targetFiles.contains(realName)) {
                                copyFileFromZip(realName, zis);
                            }
                        } else {
                            copyFileFromZip(realName, zis);
                        }
                        entry = zis.getNextEntry();
                    }
                    this.directOutput.resetTargetFilesFlag();
                    return;
                } else {
                    realName = targetName;
                }
            }
            if (this.directOutput.getTargetFilesFlag() == FileExport.CHOOSE_OVERWRITTEN_FILES) {
                boolean match = false;
                for (int i = 0; i < targetFiles.size(); i++) {
                    String next = (String) targetFiles.elementAt(i);
                    if (realName.indexOf(next) >= 0) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    copyFileFromZip(realName, zis);
                }
            } else {
                copyFileFromZip(realName, zis);
            }
            entry = zis.getNextEntry();
        }
        this.directOutput.resetTargetFilesFlag();
    }

    private void copyFileFromZip(String realName, ZipInputStream zis) throws FileNotFoundException, IOException {
        logger.fine("Real target:" + realName);
        byte[] buffer = new byte[4096];
        int read = 0;
        File destFile = new File(realName);
        if (!isAscii) {
            File parentFile = destFile.getParentFile();
            if (!parentFile.isDirectory()) {
                parentFile.mkdir();
            }
            FileOutputStream fos = new FileOutputStream(destFile);
            while ((read = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
            fos.close();
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(zis));
            PrintWriter bw = new PrintWriter(new FileOutputStream(destFile));
            String line;
            while ((line = br.readLine()) != null) {
                bw.println(line);
            }
            bw.close();
        }
    }

    /**
	 * when object dies, close socket
	 */
    public void finalize() {
        try {
            close();
        } catch (IOException e) {
            logger.fine("Connection already closed: " + e.getMessage());
        }
    }

    /**
	 * flush output stream
	 * 
	 * @exception IOException
	 *                thrown by flush method
	 */
    public void flush() throws IOException {
        if (state == CLOSED) {
            return;
        }
        outputStream.flush();
        logger.fine(" [" + count + "] " + "Flush connection: " + target);
        state = READY;
    }

    /**
	 * returns the lifetime of AJO's (in ms) for this connection Can be used to
	 * limit the validity of "short" service requests Shold not be used with
	 * user defined jobs
	 * 
	 * @return The ajoLifetime value
	 */
    public long getAjoLifetime() {
        return lifeTime;
    }

    /**
	 * Gets the InputStream of the Connection
	 * 
	 * @return socket input stream
	 * @exception IOException
	 *                thrown by socket
	 */
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    /**
	 * Gets the OutputStream of the Connection
	 * 
	 * @return sopcket output stream
	 * @exception IOException
	 *                thrown by socket
	 */
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    /**
	 * get njs reply
	 * 
	 * @return reply
	 */
    public Reply getReply() {
        state = READY;
        return this.reply;
    }

    /**
	 * Get User object
	 * 
	 * @return user object
	 */
    public User getUser() {
        return user;
    }

    /**
	 * Read object from stream
	 * 
	 * @return object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public Object read() throws IOException, ClassNotFoundException {
        Object returnedObject = null;
        if (state == WRITTEN) {
            state = READY;
            logger.fine(" [" + count + "] " + "Reading NJS reply: ");
            returnedObject = inputStream.readObject();
            return returnedObject;
        } else {
            state = READY;
            return returnedObject;
        }
    }

    /**
	 * Read the NJS's reply after sending an AJO. Retrieve all streamed files
	 * possibly following the reply.
	 * 
	 * @return error code as sent by njs
	 * @exception IOException
	 * @exception ClassNotFoundException
	 */
    public int readReply() throws IOException, ClassNotFoundException {
        if (state == WRITTEN) {
            try {
                reply = (Reply) read();
            } catch (EOFException eofx) {
                logger.warning("Problems reading reply (EOF): ignored");
                response = new UnicoreResponse(0, "Problems reading reply (EOF): ignored");
                reply = new ConsignJobReply();
                reply.addTraceEntry(response);
            }
            response = reply.getLastEntry();
            errorcode = response.getReturnCode();
            state = NOT_READY;
            if (errorcode != 0) {
                return errorcode;
            }
            if (reply instanceof ConsignJobReply) {
            } else if (reply instanceof RetrieveOutcomeReply) {
                RetrieveOutcomeReply ror = (RetrieveOutcomeReply) reply;
                if (ror.hasStreamed()) {
                    readStreamedOutcome();
                }
            }
            state = READY;
        }
        return errorcode;
    }

    /**
	 * Receive file(s) streamed together whit the reply. As optimization a
	 * *single* file can be directly stored to the provided FileOutputStream.
	 * All other files are temporarily stored in the Sessiondir and must be
	 * retrieved form ther later. This holds especially for the stdout/stderr
	 * files of the UserTasks.
	 * 
	 * @exception FileNotFoundException
	 * @exception IOException
	 */
    private void readStreamedOutcome() throws FileNotFoundException, IOException {
        PacketisedInputStream pis = new PacketisedInputStream(socket.getInputStream());
        ZipInputStream zis = new ZipInputStream(pis);
        int avail;
        int count = 0;
        logger.fine("Reading streamed outcome.");
        if ((avail = zis.available()) != 0) {
            if (directOutput == null) {
                FileOutputStream fos = null;
                File streamedDir = null;
                ZipEntry entry = zis.getNextEntry();
                byte[] buffer = new byte[4096];
                int read = 0;
                OutcomeManager.clearCache();
                while (entry != null) {
                    String zipName = entry.getName();
                    int lastSep = zipName.lastIndexOf("/");
                    String filename = zipName.substring(lastSep + 1);
                    String dir = sessionDir + File.separator + zipName.substring(0, lastSep);
                    logger.fine("Receiving streamed file: " + zipName);
                    OutcomeManager.addStreamedFile(zipName);
                    streamedDir = new File(dir);
                    streamedDir.deleteOnExit();
                    File streamedFile = new File(dir, filename);
                    streamedFile.deleteOnExit();
                    streamedDir.mkdirs();
                    fos = new FileOutputStream(streamedFile);
                    while ((read = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                    }
                    fos.close();
                    zis.closeEntry();
                    File parent = streamedDir;
                    while (parent.getParentFile() != null) {
                        parent.deleteOnExit();
                        parent = parent.getParentFile();
                    }
                    parent = parent.getParentFile();
                    entry = zis.getNextEntry();
                }
            } else {
                logger.fine("Direct output files into destination directory.");
                copyFile(zis);
            }
            long skipped = pis.skip(Long.MAX_VALUE);
        }
    }

    /**
	 * Send a request with a Vector of files streamed behind
	 * 
	 * @deprecated Only for backwards compatibility; only Request is allowed as
	 *             type
	 * @param what
	 *            Request to be sent
	 * @param streamedFiles
	 *            Vector of files that should be streamed with the request
	 * @return NJS Reply
	 * @throws Exception
	 *             if anything went wrong
	 */
    public synchronized Reply sendMessage(Object what, Vector streamedFiles) throws Exception {
        return (sendMessage((Request) what, streamedFiles));
    }

    /**
	 * Send a request with a Vector of files streamed behind
	 * 
	 * @param what
	 *            Request to be sent
	 * @param streamedFiles
	 *            Vector of files that should be streamed with the request
	 * @return NJS Reply
	 * @throws Exception
	 *             if anything went wrong
	 */
    public synchronized Reply sendMessage(Request what, Vector streamedFiles) throws Exception {
        if (what instanceof ConsignJob) {
            ConsignForm.AJO cfa = new ConsignForm.AJO((ConsignJob) what);
            AbstractJob job = cfa.getAJO();
            logger.fine("Endorser is: " + job.getEndorser().getCertificate().getSubjectDN());
        }
        logger.info("Socket may have been closed by Gateway.");
        logger.info("Reconnecting...");
        reconnect();
        write(what);
        if (streamedFiles != null && !streamedFiles.isEmpty()) {
            writeStreamedFiles(streamedFiles);
        }
        readReply();
        return getReply();
    }

    public void reconnect() throws Exception {
        socket.close();
        socket = socketFactory.createSocket(target);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        state = READY;
    }

    private boolean socketClosedByGateway() {
        try {
            OutputStream o = socket.getOutputStream();
        } catch (IOException iox) {
            return true;
        }
        return false;
    }

    /**
	 * Set the lifetime (in ms) of AJO's for this Connection
	 * 
	 * @param lifeTime
	 *            ajo lifetime
	 */
    public void setAjoLifetime(long lifeTime) {
        this.lifeTime = lifeTime;
    }

    /**
	 * Set direct file export
	 * 
	 * @param directOutput
	 *            FileExport object
	 */
    public synchronized void setDirectOutput(FileExport directOutput) {
        this.directOutput = directOutput;
    }

    /**
	 * Is direct file export in ascii format?
	 * 
	 * @param isAscii
	 *            true, if yes
	 */
    public void setIsAscii(boolean isAscii) {
        this.isAscii = isAscii;
    }

    /**
	 * Send files that have to be transfered from the local file system to the
	 * Uspace by appending them as a ZIPStream after the serialized AJO.
	 *  
	 */
    private long streamFile(FileImport fileImport, String uplDirectory, org.unicore.utility.ZipOutputStream zos) throws FileNotFoundException, IOException {
        return writeImportToZip(fileImport.getSourceName(), fileImport.getHiddenDestination(), uplDirectory, fileImport.getFileMode(), zos);
    }

    /**
	 * Send files that have to be transfered from the local file system to the
	 * Uspace by appending them as a ZIPStream after the serialized AJO.
	 * Wildcard sensitive variant.
	 *  
	 */
    private long streamWildcardFiles(FileImport wildImport, String uplDirectory, org.unicore.utility.ZipOutputStream zos) throws FileNotFoundException, IOException {
        long total = 0;
        String filename = wildImport.getSourceName();
        File file = new File(filename);
        String directoryPath = file.getParent();
        String pattern = file.getName();
        File directory = new File(directoryPath);
        GlobFilenameFilter fileFilter = new GlobFilenameFilter(pattern);
        File[] files = directory.listFiles((FilenameFilter) fileFilter);
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                FileImport fileImport = new FileImport(FileStorage.NSPACE_STRING, files[i].getAbsolutePath(), wildImport.getDestinationName());
                fileImport.setHiddenDestination(wildImport.getHiddenDestination() + fileImport.getDestinationFileSeparator() + fileImport.getSourceCanonicalBasename());
                total += streamFile(fileImport, uplDirectory, zos);
            }
        }
        return total;
    }

    /**
	 * Write a request
	 *  
	 */
    private void write(Request what) throws IOException {
        if (state == READY) {
            logger.fine(" [" + count + "] " + "Writing AJO: ");
            outputStream.writeObject(what);
            outputStream.flush();
        }
        state = WRITTEN;
    }

    private long writeFileToZip(String filename, String uspaceName, String uplDirectory, boolean isAscii, org.unicore.utility.ZipOutputStream zos) throws FileNotFoundException, IOException {
        String zipName = uplDirectory + "/" + uspaceName;
        logger.info("Writing ZIP entry: " + zipName);
        zos.putNextEntry(new ZipEntry(zipName));
        FileInputStream fis = new FileInputStream(filename);
        long length = new File(filename).length();
        if (!isAscii) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, read);
                logger.fine("next chunk: " + read);
            }
        } else {
            byte read;
            while ((read = (byte) fis.read()) > 0) {
                if (read != 13 && read != 26) {
                    zos.write((int) read);
                }
            }
        }
        zos.closeEntry();
        fis.close();
        return length;
    }

    private long writeImportToZip(String filename, String uspaceName, String uplDir, boolean isAscii, org.unicore.utility.ZipOutputStream zos) throws FileNotFoundException, IOException {
        long total = 0;
        File importObj = new File(filename);
        if (importObj.exists() == true) {
            if (importObj.isDirectory() == true) {
                File[] fileList = importObj.listFiles();
                for (int i = 0; i < fileList.length; i++) {
                    if (fileList[i].isDirectory()) {
                        total += writeImportToZip(fileList[i].getPath(), uspaceName + "/" + fileList[i].getName(), uplDir, isAscii, zos);
                    } else if (fileList[i].isFile()) {
                        total += writeFileToZip(fileList[i].getPath(), uspaceName + "/" + fileList[i].getName(), uplDir, isAscii, zos);
                    }
                }
            } else {
                total += writeFileToZip(importObj.getPath(), uspaceName, uplDir, isAscii, zos);
            }
        }
        return total;
    }

    /**
	 * Write files imported from nspace as zip stream following a ConsignJob
	 * object.
	 * 
	 * @param importTasks
	 *            Vector with ImportContainers that contain import tasks from
	 *            nspace
	 * @return long total bytes transferred
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public long writeStreamedFiles(Vector importTasks) throws FileNotFoundException, IOException {
        if (importTasks.size() <= 0) {
            return 0L;
        }
        long total = 0;
        PacketisedOutputStream pos = new PacketisedOutputStream(socket.getOutputStream());
        org.unicore.utility.ZipOutputStream zos = new org.unicore.utility.ZipOutputStream(pos);
        for (int i = 0; i < importTasks.size(); i++) {
            TaskContainer importContainer = (TaskContainer) importTasks.get(i);
            FileImport[] fileImports = importContainer.getFileImportsFromLocal();
            for (int j = 0; j < fileImports.length; j++) {
                DeclarePortfolio portfolio = fileImports[j].getNspacePortfolio();
                String uplDirectory = portfolio.getPortfolio().getUPLDirectoryName();
                long l;
                if (fileImports[j].hasWildcards()) {
                    logger.fine("Wildcard in Nspace import: " + fileImports[j]);
                    l = streamWildcardFiles(fileImports[j], uplDirectory, zos);
                    total += l;
                } else {
                    l = streamFile(fileImports[j], uplDirectory, zos);
                    total += l;
                }
                fileImports[j].setTotalBytes(l);
            }
        }
        pos.finish();
        return total;
    }

    private class WatchDogThread extends Thread {

        private boolean defused = false;

        private Thread parent;

        private long timeout;

        public WatchDogThread(long timeout, Thread parent) {
            this.timeout = timeout;
            this.parent = parent;
        }

        public void run() {
            logger.fine("WatchDog activated for thread: " + parent);
            setDefused(false);
            try {
                sleep(timeout);
            } catch (InterruptedException ie) {
            }
            if (!defused) {
                logger.fine("Interrupted doomed thread: " + parent);
                parent.interrupt();
            } else {
                logger.fine("WatchDog defused");
            }
            logger.fine("WatchDog terminated for thread: " + parent + "(" + defused + ")");
        }

        public void setDefused(boolean what) {
            this.defused = what;
        }
    }
}
