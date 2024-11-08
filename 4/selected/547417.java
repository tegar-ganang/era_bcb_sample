package edu.ucsd.osdt.sink.numeric;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.ChannelTree;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import edu.ucsd.osdt.util.ISOtoRbnbTime;
import edu.sdsc.grid.io.FileFactory;
import edu.sdsc.grid.io.GeneralRandomAccessFile;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.GeneralFileSystem;
import edu.sdsc.grid.io.srb.SRBAccount;
import edu.sdsc.grid.io.srb.SRBFileSystem;

public class RbnbToSrb {

    private SRBAccount srbAccount;

    private GeneralFileSystem srbFileSystem;

    private boolean clearSrb;

    private static Logger logger = Logger.getLogger(RbnbToSrb.class.getName());

    private String rbnbServer = "localhost:3333";

    private double reqStart = Double.MAX_VALUE;

    private double reqDuration = -1;

    public RbnbToSrb() {
        srbAccount = null;
        srbFileSystem = null;
        clearSrb = false;
        if (!initSRB()) {
            logger.severe("Couldn't initialize SRB. Exiting.");
            System.exit(1);
        } else {
            logger.info("Established SRB connection: " + srbAccount.toString() + "\nto filesystem: " + srbFileSystem.toString());
        }
    }

    private boolean initSRB() {
        try {
            srbAccount = new SRBAccount();
            srbFileSystem = new SRBFileSystem(srbAccount);
        } catch (IOException ioe) {
            logger.severe("Connecting to the SRB");
            return false;
        }
        return true;
    }

    private void populateSRB(GeneralFile path, GeneralFile fle) throws IOException {
        path.mkdir();
        fle.createNewFile();
    }

    private void cleanSRB(GeneralFile path, GeneralFile fle) {
        fle.delete();
        path.delete();
    }

    private byte[] readFileContents(GeneralRandomAccessFile fileToRead) throws IOException {
        long fileLen = fileToRead.length();
        byte[] fileContents = new byte[(int) fileLen];
        long filePointer = fileToRead.getFilePointer();
        fileToRead.seek(0);
        fileToRead.readFully(fileContents);
        fileToRead.seek(filePointer);
        return fileContents;
    }

    public boolean writeCmapToSrb(ChannelMap cmap) throws IOException {
        if (clearSrb) {
            logger.info("Clearing out srb.");
        }
        ChannelTree ctree = ChannelTree.createFromChannelMap(cmap);
        Iterator treeIt = ctree.iterator();
        ChannelTree.Node node = null;
        GeneralFile genFilePath = null;
        GeneralFile genFile = null;
        while (treeIt.hasNext()) {
            node = (ChannelTree.Node) (treeIt.next());
            if (node.getType() == ChannelTree.CHANNEL) {
                genFilePath = FileFactory.newFile(srbFileSystem, node.getFullName());
                genFile = FileFactory.newFile(genFilePath, node.getName() + ".dat");
                if (clearSrb) {
                    cleanSRB(genFilePath, genFile);
                } else {
                    populateSRB(genFilePath, genFile);
                    GeneralRandomAccessFile srbFile = FileFactory.newRandomAccessFile(genFile, "rw");
                    StringBuffer toWrite = new StringBuffer();
                    int cmapIndex = cmap.GetIndex(node.getName());
                    double[] cmapData, cmapTimes;
                    if (0 < cmapIndex) {
                        cmapData = cmap.GetDataAsFloat64(cmap.GetIndex(node.getName()));
                        cmapTimes = cmap.GetTimes(cmap.GetIndex(node.getName()));
                        logger.info("Got data array of size: " + cmapData.length);
                    } else {
                        cmapData = new double[5];
                        cmapTimes = new double[cmapData.length];
                        long now = System.currentTimeMillis();
                        for (int i = 0; i < cmapData.length; i++) {
                            cmapData[i] = Math.random() * 1E5;
                            cmapTimes[i] = now + (long) (Math.random() * 1E6);
                        }
                        java.util.Arrays.sort(cmapTimes);
                    }
                    for (int i = 0; i < cmapData.length; i++) {
                        toWrite.append(cmapData[i]);
                        toWrite.append('\t');
                        toWrite.append(ISOtoRbnbTime.formatDate((long) cmapTimes[i]));
                        toWrite.append('\r');
                        toWrite.append('\n');
                    }
                    srbFile.seek(srbFile.length());
                    srbFile.writeBytes(toWrite.toString());
                    logger.info("Wrote file \"" + node.getName() + ".dat\"");
                    srbFile.close();
                }
            }
        }
        return true;
    }

    private ChannelMap getChannelMap() throws SAPIException {
        ChannelMap initMap = new ChannelMap();
        ChannelTree ctree = null;
        Sink rbnbSink = new Sink();
        ArrayList<String> childServers = new ArrayList<String>();
        initMap.Add("*");
        rbnbSink.OpenRBNBConnection(rbnbServer, "SRBsink");
        rbnbSink.RequestRegistration(initMap);
        rbnbSink.Fetch(-1, initMap);
        ctree = ChannelTree.createFromChannelMap(initMap, "*");
        Iterator treeIt = ctree.iterator();
        ChannelTree.Node node = null;
        while (treeIt.hasNext()) {
            node = (ChannelTree.Node) (treeIt.next());
            if (node.getType() == ChannelTree.SERVER) {
                childServers.add(node.getName());
            }
        }
        logger.info("Detected " + childServers.size() + " child rbnb server" + ((1 < childServers.size()) ? "s" : ""));
        Iterator childIterator = childServers.iterator();
        initMap.Clear();
        int childCnt = 0;
        while (childIterator.hasNext()) {
            StringBuffer sbuff = new StringBuffer();
            sbuff.append(childIterator.next());
            sbuff.append("/*/*");
            initMap.Add(sbuff.toString());
            childCnt++;
        }
        logger.info("Loaded " + childCnt + " child server name" + ((1 < childCnt) ? "s" : "") + " into cmap.");
        rbnbSink.RequestRegistration(initMap);
        rbnbSink.Fetch(-1, initMap);
        ChannelMap retval = validateCmapForData(initMap);
        rbnbSink.RequestRegistration(retval);
        rbnbSink.Fetch(-1, retval);
        double reqEnd = reqStart + reqDuration;
        logger.info("Got cmap entry count: " + retval.NumberOfChannels() + " start: " + ISOtoRbnbTime.formatDate((long) (reqStart * 1000)) + " end: " + ISOtoRbnbTime.formatDate((long) (reqEnd * 1000)));
        rbnbSink.Request(retval, 0, 0, "newest");
        rbnbSink.Fetch(1000, retval);
        rbnbSink.CloseRBNBConnection();
        return retval;
    }

    private ChannelMap validateCmapForData(ChannelMap cmap) throws SAPIException {
        ChannelMap retval = new ChannelMap();
        for (int i = 0; i < cmap.NumberOfChannels(); i++) {
            if (0 < cmap.GetTimeDuration(i) && !cmap.GetName(i).matches(".*ChannelListRequest") && !cmap.GetName(i).matches(".*ArchiveDataBytes") && !cmap.GetName(i).matches(".*CacheDataBytes") && !cmap.GetName(i).matches(".*MemoryUsed") && !cmap.GetName(i).matches(".*SocketBytes") && !cmap.GetName(i).matches(".*SocketRate") && !cmap.GetName(i).matches(".*TotalMemory")) {
                retval.Add(cmap.GetName(i));
                if (cmap.GetTimeStart(i) < this.reqStart) {
                    this.reqStart = cmap.GetTimeStart(i);
                }
                if (this.reqDuration < cmap.GetTimeDuration(i)) {
                    this.reqDuration = cmap.GetTimeDuration(i);
                }
            }
        }
        return retval;
    }

    public static void main(String[] args) {
        RbnbToSrb serb = new RbnbToSrb();
        Options opts = new Options();
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;
        opts.addOption("a", false, "about");
        opts.addOption("e", false, "delete the filepath in the srb");
        opts.addOption("h", false, "print usage");
        opts.addOption("s", true, "rbnb server for which to be a data sink");
        HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException pe) {
            logger.severe("Trouble parsing command line: " + pe);
            System.exit(0);
        }
        if (cmd.hasOption("a")) {
            System.out.println("About: this program accepts an rbnb ChannelMap " + "and then forwards the data and metadata to SRB");
            System.exit(0);
        }
        if (cmd.hasOption("e")) {
            serb.clearSrb = true;
        }
        if (cmd.hasOption("h")) {
            formatter.printHelp("RbnbToSrb", opts);
            System.exit(0);
        }
        if (cmd.hasOption("s")) {
            String a = cmd.getOptionValue("s");
            serb.rbnbServer = a;
        }
        try {
            if (serb.writeCmapToSrb(serb.getChannelMap())) {
                logger.info("Wrote to SRB.");
            }
        } catch (SAPIException sae) {
            logger.severe("Cannot get a channelmap: " + sae);
            sae.printStackTrace();
        } catch (IOException ioe) {
            logger.severe("Writing cmap to srb: " + ioe);
        }
    }
}
