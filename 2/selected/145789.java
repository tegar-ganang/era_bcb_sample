package org.mbari.vars.annotation.io;

import org.mbari.movie.Timecode;
import org.mbari.vars.annotation.model.Association;
import org.mbari.vars.annotation.model.CameraData;
import vars.annotation.ISimpleConcept;
import org.mbari.vars.annotation.model.Observation;
import org.mbari.vars.annotation.model.VideoArchive;
import org.mbari.vars.annotation.model.VideoArchiveSet;
import org.mbari.vars.annotation.model.VideoFrame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This is used for reading VIF files generated by VICKI. To input these, you
 * will need need the vif file and if possible the camera log file. If the camera
 * log is not present the camera data can be loaded later.</p>
 *
 * <p>Use as:
 * <pre>
 * VifInput vi = new VifInput(new URL("http://some.url.com/myfile.vif"));
 * vi.read();
 * </pre>
 * </p>
 *
 * <h2><u>License</u></h2>
 * <p><font size="-1" color="#336699"><a href="http://www.mbari.org">
 * The Monterey Bay Aquarium Research Institute (MBARI)</a> provides this
 * documentation and code &quot;as is&quot;, with no warranty, express or
 * implied, of its quality or consistency. It is provided without support and
 * without obligation on the part of MBARI to assist in its use, correction,
 * modification, or enhancement. This information should not be published or
 * distributed to third parties without specific written permission from
 * MBARI.</font></p>
 *
 * <p><font size="-1" color="#336699">Copyright 2003 MBARI.
 * MBARI Proprietary Information. All rights reserved.</font></p>
 *
 *@author     <a href="http://www.mbari.org">MBARI</a>
 *@created    June 14, 2004
 *@version    $Id: VifInput.java,v 1.3 2006/01/10 00:54:18 brian Exp $
 */
public class VifInput {

    private static final Logger log = LoggerFactory.getLogger(VifInput.class);

    /**
     * Concepts that require special handling. key = conceptName (String);
     * value = ConceptAction
     */
    private static Map specialConcepts = new HashMap();

    /**
     * Properties that require special handiling. They are tagged here by
     * linkName. key = linkName (String); value=ConceptAction
     */
    private static Map specialProperties = new HashMap();

    static {
        specialConcepts.put("aux-camera", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
                CameraData cd = vf.getCameraData();
                if (cd == null) {
                    cd = new CameraData();
                    vf.setCameraData(cd);
                }
                cd.setName(CameraData.NAME_AUX);
            }
        });
        specialConcepts.put("main-camera", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
                CameraData cd = vf.getCameraData();
                if (cd == null) {
                    cd = new CameraData();
                    vf.setCameraData(cd);
                }
                cd.setName(CameraData.NAME_MAIN);
            }
        });
        specialConcepts.put("camera-transport", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
            }
        });
        specialConcepts.put("interpretation", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
            }
        });
    }

    static {
        specialProperties.put("tag", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
                vf.setInSequence(true);
            }
        });
        specialProperties.put("recorded-timecode", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
                StringTokenizer st = new StringTokenizer(p.linkValue, ":");
                String hour = "00";
                if (st.hasMoreTokens()) {
                    hour = st.nextToken();
                }
                String minute = "00";
                if (st.hasMoreTokens()) {
                    minute = st.nextToken();
                }
                String second = "00";
                if (st.hasMoreTokens()) {
                    second = st.nextToken();
                }
                String frame = "00";
                if (st.hasMoreTokens()) {
                    frame = st.nextToken();
                }
                try {
                    Integer hourInt = new Integer(hour);
                    if (hourInt.intValue() < 0) {
                        hour = "00";
                    }
                    if (hour.length() < 2) {
                        hour = "0" + hour;
                    }
                } catch (Exception e) {
                    hour = "00";
                }
                try {
                    Integer minuteInt = new Integer(minute);
                    if (minuteInt.intValue() < 0) {
                        minute = "00";
                    }
                    if (minute.length() < 2) {
                        minute = "0" + minute;
                    }
                } catch (Exception e) {
                    minute = "00";
                }
                try {
                    Integer secondInt = new Integer(second);
                    if (secondInt.intValue() < 0) {
                        second = "00";
                    }
                    if (second.length() < 2) {
                        second = "0" + second;
                    }
                } catch (Exception e) {
                    second = "00";
                }
                try {
                    Integer frameInt = new Integer(frame);
                    if (frameInt.intValue() < 0) {
                        frame = "00";
                    }
                    if (frame.length() < 2) {
                        frame = "0" + frame;
                    }
                } catch (Exception e) {
                    frame = "00";
                }
                vf.setTimeCode(hour + ":" + minute + ":" + second + ":" + frame);
            }
        });
        specialProperties.put("direction", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
                if (cn.equals("camera-transport")) {
                    CameraData cd = vf.getCameraData();
                    if (cd == null) {
                        cd = new CameraData();
                        vf.setCameraData(cd);
                    }
                    cd.setDirection(p.linkValue);
                }
            }
        });
        specialProperties.put("interpreted-time-unix", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
                if (cn.equals("interpretation")) {
                    Collection c = vf.getObservations();
                    synchronized (c) {
                        for (Iterator i = c.iterator(); i.hasNext(); ) {
                            Observation obs = (Observation) i.next();
                            obs.setObservationDate(new Date(Long.parseLong(p.linkValue) * 1000));
                        }
                    }
                }
            }
        });
        specialProperties.put("interpreter-username", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
                if (cn.equals("interpretation")) {
                    Collection c = vf.getObservations();
                    synchronized (c) {
                        for (Iterator i = c.iterator(); i.hasNext(); ) {
                            Observation obs = (Observation) i.next();
                            obs.setObserver(p.linkValue);
                        }
                    }
                }
            }
        });
        specialProperties.put("still-image", new ConceptAction() {

            void doAction(VideoFrame vf, String cn, Property p) {
                final CameraData cd = vf.getCameraData();
                cd.setStillImage(p.linkValue);
            }
        });
    }

    /**
     * These are stored so that we can generate a meaningful name for each
     * archive later on based on dive number and tape number.
     *
     * key = tape number
     * value = VideoArchive corresponding to the tapeNumber.
     */
    private final Map archives = new TreeMap();

    private int lineNumber;

    private final URL url;

    /**
     * Stores the videoFrames. We hang onto them because we don't know exactly
     * which VideoArchive to association them with until we have read the
     * Camera log to.
     */
    private final Collection videoFrames = new ArrayList();

    private VideoArchiveSet videoArchiveSet = new VideoArchiveSet();

    private final Collection orphanedVideoFrames = new ArrayList();

    private VifMetadata vifMetadata;

    /**
     * Constructor for the VifInput object
     *
     * @param  url  Description of the Parameter
     */
    public VifInput(final URL url) {
        this.url = url;
    }

    /**
     * Information about the video archive set gets parsed out of the filename
     * Just for Future reference, storing metadata in the filename is without
     * storing it in the file too is a bad idea.
     *
     *
     * @param  vifName       The name of the vif file in YYYDDDSPn.vif format
     * @return               A VideoArchiveSet populated with metadata parsed from the
     *            filename.
     * @throws  IOException
     */
    private VideoArchiveSet createVideoArchiveSet(String vifName) throws IOException {
        vifMetadata = new VifMetadata(vifName);
        String shipName = null;
        final Collection shipNames = VideoArchiveSet.getShipNames();
        for (Iterator i = shipNames.iterator(); i.hasNext(); ) {
            shipName = (String) i.next();
            final char shipCode = Character.toUpperCase(shipName.charAt(0));
            if (vifMetadata.shipCode == shipCode) {
                break;
            }
            shipName = null;
        }
        if (shipName == null) {
            throw new IOException("Invalid ship code, " + vifMetadata.shipCode + ", in " + url);
        }
        String platformName = null;
        final Collection platformNames = VideoArchiveSet.getCameraPlatforms();
        for (Iterator i = platformNames.iterator(); i.hasNext(); ) {
            platformName = (String) i.next();
            final char platformCode = Character.toUpperCase(platformName.charAt(0));
            if (vifMetadata.platformCode == platformCode) {
                break;
            }
            platformName = null;
        }
        if (platformName == null) {
            throw new IOException("Invalid platform code, " + vifMetadata.platformCode + ", in ");
        }
        final VideoArchiveSet vas = new VideoArchiveSet();
        vas.setTrackingNumber(vifMetadata.trackingNumberString);
        vas.setShipName(shipName);
        vas.setPlatformName(platformName);
        vas.setFormatCode(vifMetadata.formatCode);
        return vas;
    }

    /**
     * @return Returns the VideoArchives as a Map where the key = TapeNumber(Integer)
     * and the value = VideoArchive. The VideoARchvies returned have been stored in
     * the VideoArchiveSet
     */
    public Map getArchives() {
        return archives;
    }

    /**
     * @return Returns the orphanedVideoFrames.
     */
    public final Collection getOrphanedVideoFrames() {
        return Collections.unmodifiableCollection(orphanedVideoFrames);
    }

    /**
     * @return Returns the url.
     */
    public final URL getUrl() {
        return url;
    }

    /**
     * @return Returns the videoArchiveSet.
     */
    public VideoArchiveSet getVideoArchiveSet() {
        return videoArchiveSet;
    }

    /**
     * @return Returns all the VideoFrames found in the vif file. These VideoFrames
     * have been associated with the correct VideoArchive where possible.
     */
    public Collection getVideoFrames() {
        return videoFrames;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    VifMetadata getVifMetadata() {
        return vifMetadata;
    }

    /**
     * The input file name is expected to be of the format YYYYDDDSPX.vif
     * where 'S' is the ship character code, 'P' is the platform character code
     * and 'n' is the sequence code. This method extracts the vif name from
     * a URL
     *
     *
     * @return                  the simple file name, for example if the URL was
     *             http://myhome.org/20040609.vif then 20040609.vif will be
     *         returned.
     * @exception  IOException  Description of the Exception
     */
    private String getVifName() throws IOException {
        final String vif = url.toExternalForm();
        final int idx = vif.lastIndexOf("/") + 1;
        final String vifName = vif.substring(idx);
        if (!vifName.endsWith(".vif")) {
            throw new IOException(vif + " must end have the format of YYYYDDDSPn.vif");
        }
        return vifName;
    }

    /**
     * <p><!-- Method description --></p>
     *
     */
    private void merge() {
        for (Iterator i = videoFrames.iterator(); i.hasNext(); ) {
            final VideoFrame vf = (VideoFrame) i.next();
            final Timecode tc = new Timecode(vf.getTimeCode());
            final VideoArchive va = videoArchiveSet.getVideoArchiveByTimecode(tc);
            if (va == null) {
                log.warn("No VideoArchive was found to store a VideoFrame with a " + "time-code of " + tc + " (source was " + url + ")");
                orphanedVideoFrames.add(vf);
                continue;
            }
            va.addVideoFrame(vf);
        }
    }

    /**
     * Parses a line like 'CONCEPT nanomia-1' to return
     * 'nanomia'
     *
     *
     * @param  line  A String starting with 'CONCEPT'
     * @return       The concept name contained in the line.
     */
    private String parseConceptLine(final String line) {
        final StringTokenizer s = new StringTokenizer(line);
        s.nextToken();
        return trim(s.nextToken());
    }

    /**
     * Most properties are turned into Associations. However, some do not; these
     * require some special processing. Also associations may chain; you can have a
     * nightmarish chain that looks like this in the VIF file:
     * <pre>
     * CONCEPT  nanomia-1
     * PROPERTY euphausia-1 life-stage self dead
     * PROPERTY gastrozooid-1 contains physical-object-1 euphausia-1
     * PROPERTY self contains physical-object-1 gastrozooid-1
     *
     * This turns into
     *
     * Observation (nanomia)
     *     |
     *     V
     * Association (contains | gastrozooid | nil)
     *     |
     *     V
     * Association (contains | euphasia | nil)
     *     |
     *     V
     * Association (life-stage | self | dead)
     * </pre>
     *
     *
     * @param  vf
     * @param  obs
     * @param  conceptName
     * @param  props
     */
    private void processProperties(final VideoFrame vf, final Observation obs, final String conceptName, final Collection props) {
        Map notSelfFromConcepts = null;
        Map notSelfToConcepts = null;
        for (Iterator i = props.iterator(); i.hasNext(); ) {
            Property p = (Property) i.next();
            final ConceptAction ca = (ConceptAction) specialProperties.get(p.linkName);
            if (ca != null) {
                ca.doAction(vf, conceptName, p);
                continue;
            }
            if (p.toConcept.equals("physical-object") && !p.linkValue.equals("nil")) {
                p = new Property(p.fromConcept, p.linkName, p.linkValue, "nil");
            }
            final Association a = new Association();
            a.setLinkName(p.linkName);
            a.setLinkValue(p.linkValue);
            a.setToConcept(p.toConcept);
            if (p.fromConcept.equals("self")) {
                if (obs != null) {
                    obs.addAssociation(a);
                } else {
                    log.warn("Error in " + url + ": " + "There is no observation to add this association ( " + p + ") to");
                }
                continue;
            }
            if (notSelfFromConcepts == null) {
                notSelfFromConcepts = new HashMap();
            }
            Collection c = (Collection) notSelfFromConcepts.get(p.fromConcept);
            if (c == null) {
                c = new ArrayList();
                notSelfFromConcepts.put(p.fromConcept, c);
            }
            c.add(a);
            if (notSelfToConcepts == null) {
                notSelfToConcepts = new HashMap();
            }
            if (!p.toConcept.equals("self")) {
                notSelfToConcepts.put(p.toConcept, a);
            }
        }
        if ((obs != null) && (notSelfFromConcepts != null)) {
            final Collection ass = obs.getAssociations();
            if ((ass != null) && (ass.size() > 0)) {
                synchronized (ass) {
                    for (Iterator j = ass.iterator(); j.hasNext(); ) {
                        final Association aa = (Association) j.next();
                        notSelfToConcepts.put(aa.getToConcept(), aa);
                    }
                }
            }
            for (Iterator j = notSelfFromConcepts.keySet().iterator(); j.hasNext(); ) {
                final String fromConcept = (String) j.next();
                final Collection froms = (Collection) notSelfFromConcepts.get(fromConcept);
                final ISimpleConcept to = (ISimpleConcept) notSelfToConcepts.get(fromConcept);
                if (to == null) {
                    log.warn("Unable to find the concept, " + fromConcept + " to link an Association to. (Around line" + lineNumber + ")");
                    continue;
                }
                for (Iterator k = froms.iterator(); k.hasNext(); ) {
                    final Association sc = (Association) k.next();
                    to.addAssociation(sc);
                }
            }
        }
    }

    /**
     *  Description of the Method
     *
     * @exception  IOException  Description of the Exception
     */
    public void read() throws IOException {
        videoArchiveSet = createVideoArchiveSet(getVifName());
        readArchives(videoArchiveSet);
        readAnnotations();
        merge();
    }

    /**
     * This method is called immediatly after an occurence of 'ANNOTATE'. The
     * input stream should be set to just before the first 'CONCEPT' in the
     * annotation block.
     *
     *
     * @param  in
     * @throws  IOException
     */
    private void readAnnotationBlock(final BufferedReader in) throws IOException {
        in.mark(1024);
        String line = in.readLine();
        final VideoFrame vf = new VideoFrame();
        videoFrames.add(vf);
        while ((line != null) && line.startsWith("CONCEPT")) {
            in.reset();
            readConceptBlock(in, vf);
            in.mark(1024);
            line = in.readLine();
            lineNumber++;
        }
        if (line != null) {
            in.reset();
        }
    }

    /**
     * An annotation block corresonds to a VideoFrame.
     *
     * @throws  IOException
     */
    private void readAnnotations() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                lineNumber++;
                if (line.startsWith("ANNOTATE")) {
                    readAnnotationBlock(in);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            in.close();
        }
    }

    /**
     * Read the archive lines in a vif file and create the corresponding
     * VideoArchive objects.
     *
     * @param  vas
     * @throws  IOException
     */
    private void readArchives(final VideoArchiveSet vas) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = null;
        try {
            while ((line = in.readLine()) != null) {
                if (line.startsWith("ARCHIVE")) {
                    final StringTokenizer s = new StringTokenizer(line);
                    s.nextToken();
                    final Integer tapeNumber = Integer.valueOf(s.nextToken());
                    final Timecode timeCode = new Timecode(s.nextToken());
                    final VideoArchive va = new VideoArchive();
                    va.setTimeCode(timeCode);
                    va.setTapeNumber(tapeNumber);
                    vas.addVideoArchive(va);
                    archives.put(tapeNumber, va);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            in.close();
        }
        if (archives.size() == 0) {
            log.warn("No lines with ARCHIVE were found in the current vif file, will try to look at another vif with same yearday, " + "ship and platform and try to get archives from there:");
            String urlBase = url.getPath().toString().substring(0, url.getPath().toString().lastIndexOf("/"));
            File vifDir = new File(urlBase);
            File[] allYeardayFiles = vifDir.listFiles();
            for (int i = 0; i < allYeardayFiles.length; i++) {
                if (allYeardayFiles[i].toString().endsWith(".vif")) {
                    String filename = allYeardayFiles[i].toString().substring(allYeardayFiles[i].toString().lastIndexOf("/"));
                    String fileLC = filename.toLowerCase();
                    String toLookFor = new String(new Character(vifMetadata.shipCode).toString() + new Character(vifMetadata.platformCode).toString());
                    String toLookForLC = toLookFor.toLowerCase();
                    if (fileLC.indexOf(toLookForLC) >= 0) {
                        log.warn("Will try to read archives from file " + allYeardayFiles[i]);
                        final BufferedReader tempIn = new BufferedReader(new FileReader(allYeardayFiles[i]));
                        String tempLine = null;
                        try {
                            while ((tempLine = tempIn.readLine()) != null) {
                                if (tempLine.startsWith("ARCHIVE")) {
                                    final StringTokenizer s = new StringTokenizer(tempLine);
                                    s.nextToken();
                                    final Integer tapeNumber = Integer.valueOf(s.nextToken());
                                    final Timecode timeCode = new Timecode(s.nextToken());
                                    final VideoArchive va = new VideoArchive();
                                    va.setTimeCode(timeCode);
                                    va.setTapeNumber(tapeNumber);
                                    vas.addVideoArchive(va);
                                    archives.put(tapeNumber, va);
                                }
                            }
                        } catch (IOException e) {
                            throw e;
                        } finally {
                            tempIn.close();
                        }
                    }
                }
                if (archives.size() > 0) {
                    log.warn("Found " + archives.size() + " archives in that vif so will use that");
                    break;
                }
            }
            if (archives.size() == 0) {
                log.warn("Still no archives were found in the file. Unable to process it.");
            }
        }
    }

    /**
     * This method is called when a line that starts with 'CONCEPT' ocurs.
     * THe reader will be set to the start of the 'CONCEPT' line.
     *
     *
     * @param  in
     * @param  vf
     * @throws  IOException
     */
    private void readConceptBlock(final BufferedReader in, final VideoFrame vf) throws IOException {
        String line = in.readLine();
        lineNumber++;
        final String conceptName = parseConceptLine(line);
        Observation obs = null;
        final ConceptAction ca = (ConceptAction) specialConcepts.get(conceptName);
        if (ca != null) {
            ca.doAction(vf, conceptName, null);
        } else {
            obs = new Observation();
            obs.setConceptName(conceptName);
            obs = vf.addObservation(obs);
        }
        final Collection props = readPropertiesBlock(in);
        processProperties(vf, obs, conceptName, props);
    }

    /**
     * This method just reads a block of lines that start with Properties
     *
     * @param  in
     * @return
     * @throws  IOException
     */
    private Collection readPropertiesBlock(final BufferedReader in) throws IOException {
        final Collection out = new ArrayList();
        while (true) {
            in.mark(1024);
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            if (!line.startsWith("PROPERTY")) {
                in.reset();
                break;
            }
            lineNumber++;
            final Property p = new Property(line);
            out.add(p);
        }
        return out;
    }

    /**
     * Trims a concept name
     *
     *
     * @param  conceptName  A conceptName
     * @return              The concept ame with out any -1 on the end.
     */
    private String trim(String conceptName) {
        if (conceptName.endsWith("-1")) {
            conceptName = conceptName.substring(0, conceptName.length() - 2);
        }
        return conceptName;
    }

    private abstract static class ConceptAction {

        /**
         * <p><!-- Method description --></p>
         *
         *
         * @param vf
         * @param cn
         * @param p
         */
        abstract void doAction(VideoFrame vf, String cn, VifInput.Property p);
    }

    private class Property {

        String fromConcept = "";

        String linkName = "";

        String linkValue = "";

        String toConcept = "";

        /**
         * Constructs ...
         *
         *
         * @param line
         */
        Property(final String line) {
            final StringTokenizer s = new StringTokenizer(line);
            if (s.hasMoreTokens()) {
                s.nextToken();
            }
            if (s.hasMoreTokens()) {
                fromConcept = trim(s.nextToken());
            }
            if (s.hasMoreTokens()) {
                linkName = trim(s.nextToken());
            }
            if (s.hasMoreTokens()) {
                toConcept = trim(s.nextToken());
            }
            if (s.hasMoreTokens()) {
                String tempLinkValue = trim(s.nextToken());
                while (s.hasMoreTokens()) {
                    tempLinkValue = tempLinkValue + " " + trim(s.nextToken());
                }
                linkValue = tempLinkValue;
            }
        }

        /**
         * Constructs ...
         *
         *
         * @param fromConcept_
         * @param linkName_
         * @param toConcept_
         * @param linkValue_
         */
        Property(final String fromConcept_, final String linkName_, final String toConcept_, final String linkValue_) {
            fromConcept = fromConcept_;
            linkName = linkName_;
            linkValue = linkValue_;
            toConcept = toConcept_;
        }

        /**
         * <p><!-- Method description --></p>
         *
         *
         * @return
         */
        public String toString() {
            return fromConcept + " | " + linkName + " | " + toConcept + " | " + linkValue;
        }
    }
}
