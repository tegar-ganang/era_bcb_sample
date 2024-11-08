package org.matsim.core.network.algorithms;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkReaderTeleatlas;

/**
 * Adds additional speed restrictions to a MATSim {@link Network network} created
 * by {@link NetworkReaderTeleatlas}. The input speed restriction DBF file is based on
 * <strong>Tele Atlas MultiNet Shapefile 4.3.2.1 Format Specifications
 * document version Final v1.0, June 2007</strong>.
 *
 * @author balmermi
 */
public class NetworkTeleatlasAddSpeedRestrictions implements NetworkRunnable {

    private static final Logger log = Logger.getLogger(NetworkTeleatlasAddSpeedRestrictions.class);

    /**
	 * path and name to the Tele Atlas MultiNet speed restriction (sr) DBF file
	 */
    private final String srDbfFileName;

    private static final String SR_ID_NAME = "ID";

    private static final String SR_SPEED_NAME = "SPEED";

    private static final String SR_VALDIR_NAME = "VALDIR";

    private static final String SR_VERIFIED_NAME = "VERIFIED";

    /**
	 * To add speed restrictions to a Tele Atlas MultiNet {@link Network network}.
	 *
	 * @param srDbfFileName Tele Atlas MultiNet speed restriction DBF file
	 */
    public NetworkTeleatlasAddSpeedRestrictions(final String srDbfFileName) {
        log.info("init " + this.getClass().getName() + " module...");
        this.srDbfFileName = srDbfFileName;
        log.info("done.");
    }

    /**
	 * Reading and assigning speed restrictions to the {@link Link links} of a {@link Network network}.
	 *
	 * <p>It uses the following attributes from the Tele Atlas MultiNet speed restriction DBF file:
	 * <ul>
	 *   <li>{@link #SR_ID_NAME} (Feature Identification)</li>
	 *   <li>{@link #SR_SPEED_NAME} (Speed Restriction)</li>
	 *   <li>
	 *     {@link #SR_VALDIR_NAME} (Validity Direction)
	 *     <ul>
	 *       <li>1: Valid in Both Directions</li>
	 *       <li>2: Valid Only in Positive Direction</li>
	 *       <li>3: Valid Only in Negative Direction</li>
	 *     </ul>
	 *   </li>
	 *   <li>
	 *     {@link #SR_VERIFIED_NAME} (Verified)
	 *     <ul>
	 *       <li>0: Not Verified (default)</li>
	 *       <li>1: Verified</li>
	 *     </ul>
	 *   </li>
	 * </ul></p>
	 * <p><b>Conversion rules:</b>
	 * <ul>
	 *   <li>speed restrictions that are not verified will be ignored.</li>
	 *   <li>speed restrictions will be assigned in given directions ({@link #SR_VALDIR_NAME})</li>
	 *   <li>speed restrictions will be ignored if the corresponding {@link Link link} is not found (produces a trace message).</li>
	 *   <li>speed restrictions will not be assigned to the {@link Link link} if it already contains a speed that
	 *   is lower then the one from the speed restrictions file.</li>
	 * </ul></p>
	 *
	 * @param network
	 * @throws RuntimeException with another Exception in it in the case something goes wrong
	 */
    @Override
    public void run(final Network network) {
        try {
            run2(network);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void run2(final Network network) throws Exception {
        log.info("running " + this.getClass().getName() + " module...");
        FileChannel in = new FileInputStream(this.srDbfFileName).getChannel();
        DbaseFileReader r = new DbaseFileReader(in, true);
        int srIdNameIndex = -1;
        int srSpeedNameIndex = -1;
        int srValDirNameIndex = -1;
        int srVerifiedNameIndex = -1;
        for (int i = 0; i < r.getHeader().getNumFields(); i++) {
            if (r.getHeader().getFieldName(i).equals(SR_ID_NAME)) {
                srIdNameIndex = i;
            }
            if (r.getHeader().getFieldName(i).equals(SR_SPEED_NAME)) {
                srSpeedNameIndex = i;
            }
            if (r.getHeader().getFieldName(i).equals(SR_VALDIR_NAME)) {
                srValDirNameIndex = i;
            }
            if (r.getHeader().getFieldName(i).equals(SR_VERIFIED_NAME)) {
                srVerifiedNameIndex = i;
            }
        }
        if (srIdNameIndex < 0) {
            throw new NoSuchFieldException("Field name '" + SR_ID_NAME + "' not found.");
        }
        if (srSpeedNameIndex < 0) {
            throw new NoSuchFieldException("Field name '" + SR_SPEED_NAME + "' not found.");
        }
        if (srValDirNameIndex < 0) {
            throw new NoSuchFieldException("Field name '" + SR_VALDIR_NAME + "' not found.");
        }
        if (srVerifiedNameIndex < 0) {
            throw new NoSuchFieldException("Field name '" + SR_VERIFIED_NAME + "' not found.");
        }
        log.trace("  FieldName-->Index:");
        log.trace("    " + SR_ID_NAME + "-->" + srIdNameIndex);
        log.trace("    " + SR_SPEED_NAME + "-->" + srSpeedNameIndex);
        log.trace("    " + SR_VALDIR_NAME + "-->" + srValDirNameIndex);
        log.trace("    " + SR_VERIFIED_NAME + "-->" + srVerifiedNameIndex);
        int srCnt = 0;
        int srIgnoreCnt = 0;
        while (r.hasNext()) {
            Object[] entries = r.readEntry();
            int verified = Integer.parseInt(entries[srVerifiedNameIndex].toString());
            if (verified == 1) {
                int valdir = Integer.parseInt(entries[srValDirNameIndex].toString());
                String id = entries[srIdNameIndex].toString();
                if (valdir == 1) {
                    Link ftLink = network.getLinks().get(new IdImpl(id + "FT"));
                    Link tfLink = network.getLinks().get(new IdImpl(id + "TF"));
                    if ((ftLink == null) || (tfLink == null)) {
                        log.trace("  linkid=" + id + ", valdir=" + valdir + ": at least one link not found. Ignoring and proceeding anyway...");
                        srIgnoreCnt++;
                    } else {
                        double speed = Double.parseDouble(entries[srSpeedNameIndex].toString()) / 3.6;
                        if (speed < ftLink.getFreespeed()) {
                            ftLink.setFreespeed(speed);
                            srCnt++;
                        } else {
                            srIgnoreCnt++;
                        }
                        if (speed < tfLink.getFreespeed()) {
                            tfLink.setFreespeed(speed);
                            srCnt++;
                        } else {
                            srIgnoreCnt++;
                        }
                    }
                } else if (valdir == 2) {
                    Link ftLink = network.getLinks().get(new IdImpl(id + "FT"));
                    if (ftLink == null) {
                        log.trace("  linkid=" + id + ", valdir=" + valdir + ": link not found. Ignoring and proceeding anyway...");
                        srIgnoreCnt++;
                    } else {
                        double speed = Double.parseDouble(entries[srSpeedNameIndex].toString()) / 3.6;
                        if (speed < ftLink.getFreespeed()) {
                            ftLink.setFreespeed(speed);
                            srCnt++;
                        } else {
                            srIgnoreCnt++;
                        }
                    }
                } else if (valdir == 3) {
                    Link tfLink = network.getLinks().get(new IdImpl(id + "TF"));
                    if (tfLink == null) {
                        log.trace("  linkid=" + id + ", valdir=" + valdir + ": link not found. Ignoring and proceeding anyway...");
                        srIgnoreCnt++;
                    } else {
                        double speed = Double.parseDouble(entries[srSpeedNameIndex].toString()) / 3.6;
                        if (speed < tfLink.getFreespeed()) {
                            tfLink.setFreespeed(speed);
                            srCnt++;
                        } else {
                            srIgnoreCnt++;
                        }
                    }
                } else {
                    throw new IllegalArgumentException("linkid=" + id + ": valdir=" + valdir + " not known.");
                }
            }
        }
        log.info("  " + srCnt + " links with restricted speed assigned.");
        log.info("  " + srIgnoreCnt + " speed restrictions ignored (while verified = 1).");
        log.info("done.");
    }

    /**
	 * prints the variable settings to the STDOUT
	 *
	 * @param prefix a prefix for each line of the STDOUT
	 */
    public final void printInfo(final String prefix) {
        System.out.println(prefix + "configuration of " + this.getClass().getName() + ":");
        System.out.println(prefix + "  speed restrictions:");
        System.out.println(prefix + "    srDbfFileName:    " + this.srDbfFileName);
        System.out.println(prefix + "    SR_ID_NAME:       " + SR_ID_NAME);
        System.out.println(prefix + "    SR_SPEED_NAME:    " + SR_SPEED_NAME);
        System.out.println(prefix + "    SR_VALDIR_NAME:   " + SR_VALDIR_NAME);
        System.out.println(prefix + "    SR_VERIFIED_NAME: " + SR_VERIFIED_NAME);
        System.out.println(prefix + "done.");
    }
}
