package net.sf.timeslottracker.data.xml;

import java.util.logging.Logger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import net.sf.timeslottracker.core.TimeSlotTracker;

/**
 * File utils
 *
 * @version File version: $Revision: 1.2 $,  $Date: 2007-06-30 04:11:58 $
 * @author Last change: $Author: cnitsa $
 */
public class FileUtils {

    private static final Logger LOG = Logger.getLogger("net.sf.timeslottracker.data.xml");

    /**
   * Copies one file to another one.
   * <p>
   * If the destination file already exists it is deleted first.
   * 
   * @param source source file name with path
   * @param destination destination file name with path
   * @param timeSlotTracker
  */
    public static void copyFile(String source, String destination, TimeSlotTracker timeSlotTracker) {
        LOG.info("copying [" + source + "] to [" + destination + "]");
        BufferedInputStream sourceStream = null;
        BufferedOutputStream destStream = null;
        try {
            File destinationFile = new File(destination);
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
            sourceStream = new BufferedInputStream(new FileInputStream(source));
            destStream = new BufferedOutputStream(new FileOutputStream(destinationFile));
            int readByte;
            while ((readByte = sourceStream.read()) > 0) {
                destStream.write(readByte);
            }
            Object[] arg = { destinationFile.getName() };
            String msg = timeSlotTracker.getString("datasource.xml.copyFile.copied", arg);
            LOG.fine(msg);
        } catch (Exception e) {
            Object[] expArgs = { e.getMessage() };
            String expMsg = timeSlotTracker.getString("datasource.xml.copyFile.exception", expArgs);
            timeSlotTracker.errorLog(expMsg);
            timeSlotTracker.errorLog(e);
        } finally {
            try {
                if (destStream != null) {
                    destStream.close();
                }
                if (sourceStream != null) {
                    sourceStream.close();
                }
            } catch (Exception e) {
                Object[] expArgs = { e.getMessage() };
                String expMsg = timeSlotTracker.getString("datasource.xml.copyFile.exception", expArgs);
                timeSlotTracker.errorLog(expMsg);
                timeSlotTracker.errorLog(e);
            }
        }
    }
}
