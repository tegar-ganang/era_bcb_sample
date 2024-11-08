package org.tranche.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import org.tranche.ConfigureTranche;
import org.tranche.commons.TextUtil;
import org.tranche.exceptions.TodoException;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.scripts.ScriptsUtil.ChunkType;
import org.tranche.util.IOUtil;
import org.tranche.time.TimeUtil;
import org.tranche.users.UserZipFile;

/**
 * <p>Injects data from a data drive to Tranche network.</p>
 * @author Bryan E. Smith
 */
public class InjectDataIntoTrancheNetwork {

    /**
     * Size of batch to handle at a time
     */
    static final int hashBatchSize = 500;

    /**
     * Keep track of progress for handling chunks
     */
    static int dataCount = 0, metaCount = 0;

    /**
     * Keep some over all stats for user's knowledge
     */
    static int noReplications = 0, totalChunksInjected = 0;

    /**
     * Calculate totals so progress information can be converted to percentages
     */
    static int dataChunkTotal = 0, metaChunkTotal = 0;

    /**
     * Queue up chunks without sufficient replications
     */
    static final ArrayBlockingQueue<ChunkToInject> chunksToInjectQueue = new ArrayBlockingQueue(10000);

    /**
     * Producer
     */
    static final ChunkCheckThread[] dataChunkCheckThreads = new ChunkCheckThread[3];

    /**
     * Producer
     */
    static final ChunkCheckThread[] metaChunkCheckThreads = new ChunkCheckThread[3];

    /**
     * Consumer
     */
    static final ChunkInjectionThread[] injectionThreads = new ChunkInjectionThread[3];

    /**
     * List of servers to use
     */
    static final List<String> serversToUse = new ArrayList<String>();

    /**
     * Only allow one to run per JVM at a time
     */
    private static boolean isRunning = false;

    /**
     * <p>Injects data from a data drive to Tranche network. Expects DataBlock-format Tranche data chunks.</p>
     * <p>Will put chunks on random servers. Will report any injections, up to three copies.</p>
     *
     * @param args Expecting following runtime arguments in this order:
     * <ol>
     *   <li>Path to your network configuration file</li>
     *   <li>Path to data directory with chunks to add</li>
     *   <li>Path to user file (zip, encrypted)</li>
     *   <li>Passphrase for user file</li>
     *   <li>Path to output file</li>
     *   <li>Path to file to log upload exceptions</li>
     *   <li>Path to file for replication failures</li>
     * </ol>
     *
     * <p>The following are optional arguments, appearing after the aforementioned required parameters:</p>
     * <ul>
     *  <li>ban:tranche_url To ban a specific server. E.g., ban:tranche://141.214.182.211:443</li>
     *  <li>use:tranche_url To limit use to 1+ servers. E.g., use:tranche://141.214.182.211:443 use:tranche://141.214.182.211:443 will cause the script to only use those two servers.</li>
     *  <li>ddc:/path/to/additional/data/dir To add additional data directories. ddc is a reference to DataDirectoryConfiguration</li>
     * </ul>
     */
    public static void main(String[] args) throws Exception {
        if (isRunning) {
            System.err.println("An instance is already running in this JVM. Bailing.");
            printUsage();
            System.exit(1);
        }
        ConfigureTranche.load(args);
        File dataRoot = null, userFile = null, outputFile = null, chunkInjectEsceptionFile = null, chunkInjectFailureFile = null;
        UserZipFile uzf = null;
        try {
            if (args.length < 7) {
                throw new Exception("Does not contain required parameters. Expecting minimum of 6, found " + args.length);
            }
            isRunning = true;
            dataRoot = new File(args[1]);
            if (dataRoot == null || !dataRoot.exists()) {
                throw new Exception("Problem loading data root, cannot find: " + dataRoot.getAbsolutePath());
            }
            userFile = new File(args[2]);
            if (userFile == null || !userFile.exists()) {
                throw new Exception("Problem loading user file, cannot find: " + userFile.getAbsolutePath());
            }
            String passphrase = args[3].trim();
            outputFile = new File(args[4]);
            outputFile.createNewFile();
            if (outputFile == null || !outputFile.exists()) {
                throw new Exception("Problem loading output file, cannot find: " + outputFile.getAbsolutePath());
            }
            chunkInjectEsceptionFile = new File(args[5]);
            chunkInjectEsceptionFile.createNewFile();
            if (chunkInjectEsceptionFile == null || !chunkInjectEsceptionFile.exists()) {
                throw new Exception("Problem loading output injection chunk exception file, cannot find: " + chunkInjectEsceptionFile.getAbsolutePath());
            }
            chunkInjectFailureFile = new File(args[6]);
            chunkInjectFailureFile.createNewFile();
            if (chunkInjectFailureFile == null || !chunkInjectFailureFile.exists()) {
                throw new Exception("Problem loading output injection chunk failure file, cannot find: " + chunkInjectFailureFile.getAbsolutePath());
            }
            uzf = new UserZipFile(userFile);
            uzf.setPassphrase(passphrase);
            final List<File> additionalDataDirectories = new ArrayList<File>();
            if (args.length > 6) {
                System.out.println("");
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                System.out.println("=-=-=     STARTING OPTIONAL PARAMETERS     =-=-=");
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                System.out.println("");
                for (int i = 0; i < args.length; i++) {
                    String nextArg = args[i].trim();
                    if (nextArg.toLowerCase().startsWith("use:")) {
                        String url = nextArg.substring(4);
                        System.out.println("- ADDING SERVER TO USE: " + url);
                        serversToUse.add(url);
                    }
                    if (nextArg.toLowerCase().startsWith("ddc:")) {
                        String path = nextArg.substring(4);
                        System.out.println("- ADDING ANOTHER DDC PATH: " + path);
                        additionalDataDirectories.add(new File(path));
                    }
                }
            }
            System.out.println("");
            System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
            System.out.println("");
            injectDataToNetwork(dataRoot, uzf, outputFile, chunkInjectEsceptionFile, chunkInjectFailureFile, additionalDataDirectories);
        } catch (Exception ex) {
            System.err.println("Problem starting script: " + ex.getMessage());
            ex.printStackTrace(System.err);
            printUsage();
            System.exit(1);
        } finally {
            isRunning = false;
        }
        System.exit(0);
    }

    /**
     * <p>Helper method to inject data to the Tranche network.</p>
     * @param directory
     * @param user
     * @param outputFile
     * @param chunkInjectExceptionFile
     * @param chunkInjectFailureFile
     * @param additionalDataDirectories
     * @throws java.lang.Exception
     */
    private static void injectDataToNetwork(File directory, UserZipFile user, File outputFile, File chunkInjectExceptionFile, File chunkInjectFailureFile, List<File> additionalDataDirectories) throws Exception {
        System.out.println("injectDataToNetwork(" + directory.getAbsolutePath() + "," + user.getFile().getAbsolutePath() + ")");
        final long start = TimeUtil.getTrancheTimestamp();
        FlatFileTrancheServer ffts = null;
        BufferedWriter outputFileWriter = null, chunkExceptionFileWriter = null, chunkFailureFileWriter = null;
        try {
            outputFileWriter = new BufferedWriter(new FileWriter(outputFile, true));
            chunkExceptionFileWriter = new BufferedWriter(new FileWriter(chunkInjectExceptionFile, false));
            chunkExceptionFileWriter.write("\"Data or meta\",\"Required replications count\",\"Found replications count\",\"URL of server for failed replication\",\"Date\",\"Exception message\",\"Chunk hash\"");
            chunkExceptionFileWriter.newLine();
            chunkExceptionFileWriter.flush();
            chunkFailureFileWriter = new BufferedWriter(new FileWriter(chunkInjectFailureFile, false));
            chunkFailureFileWriter.write("\"Expected replications\", \"Found replications\", \"Data or meta\", \"Chunk hash\"");
            chunkFailureFileWriter.newLine();
            chunkFailureFileWriter.flush();
            ffts = new FlatFileTrancheServer(directory.getAbsolutePath());
            user.setFlags(UserZipFile.CAN_GET_CONFIGURATION | UserZipFile.CAN_SET_CONFIGURATION | UserZipFile.CAN_SET_DATA | UserZipFile.CAN_SET_META_DATA);
            ffts.getConfiguration().getUsers().add(user);
            if (additionalDataDirectories.size() > 0) {
                for (File additionalDataDirectory : additionalDataDirectories) {
                    DataDirectoryConfiguration nextDDC = new DataDirectoryConfiguration(additionalDataDirectory.getAbsolutePath(), Long.MAX_VALUE);
                    ffts.getConfiguration().getDataDirectories().add(nextDDC);
                }
                IOUtil.setConfiguration(ffts, ffts.getConfiguration(), user.getCertificate(), user.getPrivateKey());
                ffts.close();
                ffts = new FlatFileTrancheServer(directory.getAbsolutePath());
            }
            final long startupStart = TimeUtil.getTrancheTimestamp();
            System.out.println(">>> Loading servers.");
            System.out.println(">>> Loading data from disk.");
            ffts.waitToLoadExistingDataBlocks();
            System.out.println("Finished loading servers and data, took " + TextUtil.formatTimeLength(TimeUtil.getTrancheTimestamp() - startupStart));
            System.out.println();
            System.out.println("Writing output to file: " + outputFile.getAbsolutePath());
            {
                System.out.println(">>> Getting data chunk count...");
                BigInteger offset = BigInteger.ZERO;
                BigInteger limit = BigInteger.valueOf(hashBatchSize);
                dataChunkTotal = ffts.getDataBlockUtil().dataHashes.size();
                System.out.println("... total of " + dataChunkTotal + " chunks loaded.");
            }
            {
                BigInteger offset = BigInteger.ZERO;
                BigInteger limit = BigInteger.valueOf(hashBatchSize);
                System.out.println(">>> Getting meta data chunk count...");
                metaChunkTotal = ffts.getDataBlockUtil().metaDataHashes.size();
                System.out.println("... total of " + metaChunkTotal + " meta data chunks loaded.");
            }
            for (int i = 0; i < dataChunkCheckThreads.length; i++) {
                dataChunkCheckThreads[i] = new ChunkCheckThread(ffts, user, false, outputFileWriter, i, dataChunkCheckThreads.length);
                dataChunkCheckThreads[i].start();
            }
            for (int i = 0; i < metaChunkCheckThreads.length; i++) {
                metaChunkCheckThreads[i] = new ChunkCheckThread(ffts, user, true, outputFileWriter, i, metaChunkCheckThreads.length);
                metaChunkCheckThreads[i].start();
            }
            for (int i = 0; i < injectionThreads.length; i++) {
                injectionThreads[i] = new ChunkInjectionThread(ffts, user, chunksToInjectQueue, chunkExceptionFileWriter, chunkFailureFileWriter);
                injectionThreads[i].start();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
            }
            for (int i = 0; i < dataChunkCheckThreads.length; i++) {
                while (dataChunkCheckThreads[i].isAlive()) {
                    dataChunkCheckThreads[i].join(1000);
                }
            }
            for (int i = 0; i < metaChunkCheckThreads.length; i++) {
                while (metaChunkCheckThreads[i].isAlive()) {
                    metaChunkCheckThreads[i].join(1000);
                }
            }
            System.out.println("Waiting for injection threads to complete chunk uploads at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            for (int i = 0; i < injectionThreads.length; i++) {
                injectionThreads[i].setStopped(true);
            }
            for (int i = 0; i < injectionThreads.length; i++) {
                while (injectionThreads[i].isAlive()) {
                    injectionThreads[i].join(1000);
                }
            }
        } finally {
            if (ffts != null) {
                IOUtil.safeClose(ffts);
            }
            if (outputFileWriter != null) {
                IOUtil.safeClose(outputFileWriter);
            }
            System.out.println("\n\n=============== SUMMARY ===============");
            System.out.println("* Tool ran for: " + TextUtil.formatTimeLength(TimeUtil.getTrancheTimestamp() - start));
            System.out.println("* Found total " + dataCount + " data chunks and " + metaCount + " meta chunks on disk");
            System.out.println("* Found " + noReplications + " chunks on disk with no replications on network");
            System.out.println("* Injected total of " + totalChunksInjected + " chunk replications to network (each chunks could account for up to three replications)");
            System.out.println();
            System.out.println("Output saved to file: " + outputFile.getAbsolutePath());
            System.out.println();
            reset();
        }
    }

    /**
     * <p>Reset the resources used by this script.</p>
     */
    private static void reset() {
        dataCount = 0;
        metaCount = 0;
        noReplications = 0;
        totalChunksInjected = 0;
        dataChunkTotal = 0;
        metaChunkTotal = 0;
    }

    /**
     * <p>Print out the usage.</p>
     */
    private static void printUsage() {
        System.err.println("");
        System.err.println("USAGE:");
        System.err.println("  java -jar ThisJar.jar <path to data root> <path to user zip file> <user zip file passphrase> <path to output file> <path to failed chunks output file> [<optional>, ...]");
        System.err.println("");
        System.err.println("Required parameters:");
        System.err.println("  - <path to data root>: The directory containing a Tranche data directory (i.e., the parent of a \"data\" directory.");
        System.err.println("  - <path to user zip file>: Full path to a Tranche user zip file.");
        System.err.println("  - <user zip file passphrase>: The passphrase to the above user zip file.");
        System.err.println("  - <path to output file>: Full path to a file containing replication information about every chunk. This can get quite large -- perhaps hundreds of MB!");
        System.err.println("  - <path to chunks exception output file>: Full path to a new CSV file for any exceptions replications information. Must not already exist.");
        System.err.println("  - <path to chunks failed to meet minimum reps>: Full path to a new CSV file for any chunks that were not replication enough times");
        System.err.println("");
        System.err.println("Optional parameters:");
        System.err.println("  - ban:tranche_url To ban a specific server. E.g., ban:tranche://141.214.182.211:443");
        System.err.println("  - use:tranche_url To limit use to 1+ servers. E.g., use:tranche://141.214.182.211:443 use:tranche://141.214.182.211:443 will cause the script to only use those two servers.");
        System.err.println("  - ddc:/path/to/additional/data/dir To add additional data directories. \"ddc\" is a reference to DataDirectoryConfiguration.");
        System.err.println("");
        System.err.flush();
    }

    /**
     * <p>Thread to check for chunks.</p>
     */
    static class ChunkCheckThread extends Thread {

        private final FlatFileTrancheServer ffts;

        private final UserZipFile user;

        private final boolean isMetaData;

        private final BufferedWriter writer;

        private final boolean isServersToUse;

        private final int requiredReps;

        /**
         * This is the tricky part, so read this carefully:
         * 
         * Want to be able to have multiple threads working on data. So if three threads handling meta data,
         *   they shouldn't step on each others' heels. To do this, assign each thread a number, along with the
         *   total number of other threads doing the same task.
         *
         * This way, can split up the tasks into portion, and each will start off at different places and take a slice
         *   of the remaining duty. 
         *   - Thread one: 1-499, 1500-1999, etc.
         *   - Thread two: 500-999, 2000,2499, etc.
         *   - Thread three: 1000-1499, 2500-2999, etc.
         *
         * So,
         *       startingOffset = 0 + threadNumber * hashBatchSize
         * And next batch,
         *       start with => last offset + totalThreadCount * hashBatchSize
         *
         *   - Thread one:
         *     - First batch: startingOffset = 0 + 0 * 500 = 0
         *     - Second batch: 0 + 3 * 500 = 1500
         *   - Thread two:
         *     - First batch: startingOffset = 0 + 1 * 500 = 500
         *     - Second batch: 500 + 3 * 500 = 2000
         *   - Thread three:
         *     - First batch: startingOffset = 0 + 2 * 500 = 1000
         *     - Second batch: 1000 + 3 * 500 = 2500
         *
         * So on!
         */
        long threadNumber, totalThreadCount, startingOffset;

        /**
         *
         * @param ffserver
         * @param uzf
         * @param isMetaData
         */
        ChunkCheckThread(FlatFileTrancheServer ffts, UserZipFile user, boolean isMetaData, BufferedWriter writer, long threadNumber, long totalThreadCount) {
            this.ffts = ffts;
            this.user = user;
            this.isMetaData = isMetaData;
            this.writer = writer;
            this.isServersToUse = InjectDataIntoTrancheNetwork.serversToUse.size() > 0;
            if (this.isServersToUse) {
                requiredReps = InjectDataIntoTrancheNetwork.serversToUse.size();
            } else {
                requiredReps = 3;
            }
            this.threadNumber = threadNumber;
            this.totalThreadCount = totalThreadCount;
            this.startingOffset = 0 + this.threadNumber * hashBatchSize;
            String type = null;
            if (isMetaData) {
                type = "meta data";
            } else {
                type = "data";
            }
            System.out.println("ChunkCheckThread #" + this.threadNumber + " for " + type + " starting with " + this.startingOffset);
        }

        @Override()
        public void run() {
            if (true) {
                throw new TodoException();
            }
        }
    }

    /**
     * <p>Thread to inject chunks.</p>
     */
    static class ChunkInjectionThread extends Thread {

        final FlatFileTrancheServer ffts;

        final UserZipFile user;

        final ArrayBlockingQueue<ChunkToInject> queue;

        private boolean stopped = false;

        private final BufferedWriter chunkExceptionFileWriter, chunkFailureFileWriter;

        /**
         *
         * @param user
         * @param isMetaData
         * @param queue
         */
        ChunkInjectionThread(FlatFileTrancheServer ffts, UserZipFile user, ArrayBlockingQueue<ChunkToInject> queue, BufferedWriter chunkErrorFileWriter, BufferedWriter chunkFailureFileWriter) {
            this.ffts = ffts;
            this.user = user;
            this.queue = queue;
            this.chunkExceptionFileWriter = chunkErrorFileWriter;
            this.chunkFailureFileWriter = chunkFailureFileWriter;
        }

        @Override()
        public void run() {
        }

        /**
         * <p>Check if thread has been stopped.</p>
         * @return
         */
        public boolean isStopped() {
            return stopped;
        }

        /**
         * <p>Set the thread to stop.</p>
         * @param stopped
         */
        public void setStopped(boolean stopped) {
            if (stopped) {
                synchronized (queue) {
                    System.out.println("Received signal to stop when queue is empty. Queue has " + queue.size() + " item(s) remaining to inject.");
                }
            }
            this.stopped = stopped;
        }
    }

    /**
     * <p>Encapsulate a single chunk that needs uploaded.</p>
     */
    static class ChunkToInject {

        private BigHash chunkHash;

        private ChunkType type;

        private List<String> serversToInjectTo;

        private final int requiredReps, foundReps;

        ChunkToInject(BigHash chunkHash, ChunkType type, List<String> serversToInjectTo, int requiredReps, int foundReps) {
            this.chunkHash = chunkHash;
            this.type = type;
            this.serversToInjectTo = serversToInjectTo;
            this.requiredReps = requiredReps;
            this.foundReps = foundReps;
        }

        /**
         * <p>Get hash for chunk to inject.</p>
         * @return
         */
        public BigHash getChunkHash() {
            return chunkHash;
        }

        /**
         * <p>Type of the chunk: meta or data.</p>
         * @return
         */
        public ChunkType getType() {
            return type;
        }

        /**
         * <p>List of server URLs to which to inject.</p>
         * @return
         */
        public List<String> getServersToInjectTo() {
            return serversToInjectTo;
        }

        /**
         * <p>Return true if the chunk is meta data.</p>
         * @return
         */
        public boolean isMetaData() {
            return this.type == ChunkType.META;
        }

        /**
         * <p>Get the required number of replications for this chunk.</p>
         * @return
         */
        public int getRequiredReps() {
            return requiredReps;
        }

        /**
         * <p>Get the number of replications found on the network.</p>
         * @return
         */
        public int getFoundReps() {
            return foundReps;
        }
    }
}
