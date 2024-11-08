package com.entelience.esis;

import com.entelience.util.Logs;
import com.entelience.util.Arrays;
import com.entelience.util.BaseXMLParser;
import com.entelience.sql.Db;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Iterator;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.apache.xerces.xni.parser.XMLParseException;

public class SqlUpgradeManager {

    private SqlUpgradeManager() {
    }

    private static Logger _logger = Logs.getLogger();

    public static void main(Db db, String argsIn[]) throws Exception {
        if (argsIn.length == 0) {
            usage();
            return;
        }
        String command = Arrays.head(argsIn);
        String args[] = Arrays.shift(argsIn);
        if ("generate".equals(command)) {
            if (args.length == 5) {
                generateSQLUpgradeFile(args[0], args[1], args[2], args[3], args[4]);
            } else {
                usage();
                return;
            }
        } else {
            usage();
            return;
        }
    }

    private static void generateSQLUpgradeFile(String milestoneDefFileName, String sqlDirectoryName, String fromMilestone, String destMilestone, String destFileName) throws Exception {
        File milestoneDefFile = new File(milestoneDefFileName);
        if (!milestoneDefFile.exists()) {
            throw new IllegalArgumentException("Cannot read [" + milestoneDefFileName + "] : does not exists");
        }
        if (!milestoneDefFile.isFile()) {
            throw new IllegalArgumentException("Cannot read [" + milestoneDefFileName + "] : not a file");
        }
        if (!milestoneDefFile.canRead()) {
            throw new IllegalArgumentException("Cannot read [" + milestoneDefFileName + "] : not readable");
        }
        File sqlDirectory = new File(sqlDirectoryName);
        if (!sqlDirectory.exists()) {
            throw new IllegalArgumentException("Cannot read [" + sqlDirectoryName + "] : does not exists");
        }
        if (!sqlDirectory.isDirectory()) {
            throw new IllegalArgumentException("Cannot read [" + sqlDirectoryName + "] : not a directory");
        }
        if (!sqlDirectory.canRead()) {
            throw new IllegalArgumentException("Cannot read [" + sqlDirectoryName + "] : not readable");
        }
        File destFile = new File(destFileName);
        if (destFile.exists()) {
            throw new IllegalArgumentException("Cannot write to [" + destFileName + "] : already exists");
        }
        destFile.createNewFile();
        SqlUpgradeXmlParser parser = SqlUpgradeXmlParser.newParser();
        FileInputStream milestoneDefFIS = null;
        try {
            milestoneDefFIS = new FileInputStream(milestoneDefFile);
            parser.parse(milestoneDefFIS);
        } catch (XMLParseException e) {
            printXmlError(e, milestoneDefFileName);
        } finally {
            try {
                if (milestoneDefFIS != null) milestoneDefFIS.close();
            } catch (Exception e) {
                _logger.warn("Exception caught when closing inputstream ", e);
            }
        }
        int fromMilestoneIdx = parser.getMilestoneIndex(fromMilestone);
        int toMilestoneIdx = parser.getMilestoneIndex(destMilestone);
        if (fromMilestoneIdx < 0) {
            throw new IllegalArgumentException("Cannot upgrade from milestone [" + fromMilestone + "] : does not exist . (Exisiting milestones " + parser.listMilestones() + ")");
        }
        if (toMilestoneIdx < 0) {
            throw new IllegalArgumentException("Cannot upgrade to milestone [" + destMilestone + "] : does not exist . (Exisiting milestones " + parser.listMilestones() + ")");
        }
        if (fromMilestoneIdx == toMilestoneIdx) {
            throw new IllegalArgumentException("Cannot upgrade to the same milestone");
        }
        List<String> files = parser.getFilesForMilestones(fromMilestoneIdx, toMilestoneIdx);
        List<String> views = parser.getViewsForMilestones(fromMilestoneIdx, toMilestoneIdx);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destFile);
            for (Iterator<String> it = files.iterator(); it.hasNext(); ) {
                appendFileToOutputStream(fos, sqlDirectory, it.next());
            }
            for (Iterator<String> it = views.iterator(); it.hasNext(); ) {
                appendFileToOutputStream(fos, sqlDirectory, it.next());
            }
        } catch (Exception e) {
            fos.close();
            destFile.delete();
            throw e;
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (Exception e) {
                _logger.warn("Exception caught when closing outputstream ", e);
            }
        }
        System.out.println("An SQL update file has been generated to " + destFileName + ".");
    }

    private static void appendFileToOutputStream(OutputStream fos, File sqlDirectory, String fileName) throws Exception {
        byte buffer[] = new byte[com.entelience.util.StaticConfig.ioBufferSize];
        File toRead = new File(sqlDirectory, fileName);
        if (!toRead.exists()) {
            throw new IllegalArgumentException("Cannot read [" + toRead + "] : does not exists");
        }
        if (!toRead.isFile()) {
            throw new IllegalArgumentException("Cannot read [" + toRead + "] : not a file");
        }
        if (!toRead.canRead()) {
            throw new IllegalArgumentException("Cannot read [" + toRead + "] : not readable");
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(toRead);
            int i = 0;
            while ((i = fis.read(buffer, 0, buffer.length)) != -1) {
                if (i > 0) fos.write(buffer, 0, i);
            }
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (Exception e) {
                _logger.warn("Exception caught when closing outputstream ", e);
            }
        }
    }

    private static void printXmlError(XMLParseException ex, String fileName) {
        StringBuffer msg = new StringBuffer();
        msg.append(" Error in XML file [").append(fileName).append("] ");
        msg.append(BaseXMLParser.formatXMLError(ex));
        System.out.println(msg);
    }

    /**
     * show usage information 
     */
    public static void usage() {
        System.out.println("Usage");
        System.out.println("");
        System.out.println("generate <milestone definition file> <SQL files base directory> <Origin milestone> <Destination milestone> <Generated SQL file>");
    }
}
