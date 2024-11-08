package org.web3d.x3d.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * Verify correct X3D final or transitional DTDs, convert one to the other if
 * desired
 * <p>[Usage] java -cp /www.web3d.org/x3d/tools/canonical/dist/lib/X3dC14n.jar org.web3d.x3d.tools.X3dHeaderChecker [-f | -t] [optional -v] &lt;path&gt;/SceneName.x3d</code></p>
 *
 * @author <a href="mailto:brutzman@nps.edu">Don Brutzman</a>
 * @version $Id: X3dHeaderChecker.java 10140 2012-01-04 01:42:00Z brutzman $
 * <p>
 *   <b>Latest Modifications:</b>
 *   <pre><b>
 *     Date:     19 April 2006
 *     Time:     1302
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Ensured that even if a Final DTD DOCTYPE is matched it will
 *                  be replaced with a canonical DOCTYPE declaration
 *               2) Corrected regex pattern for Warning recognition
 *               3) Created a constructor so that this class can be launched
 *                  from within another class making this class fully extensible
 *               4) Added log4j logging capability replacing Sysout statements
 *               5) Closed the FileInputStream in getSceneContent().  Originally
 *                  set to null which doesn't release is file lock until the
 *                  JVM invokes the gc()
 *               6) Fixed a bug in that if there is no matched XML declaration,
 *                  exit the X3dHeaderChecker.  Stripped out code that dealt with
 *                  no DTD as that issue is handled elsewhere
 *               7) Instered a log statement to identify which scene file had no
 *                  XML declaration and/or DTD, or multiple DTDs
 *
 *     Date:     19 July 2006
 *     Time:     2245
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Implemented X3dToolsConstants.  Moved some final Strings to
 *                  X3dToolsConstants.
 *               2) Removed static references to variables and methods
 *               3) Broke up constructor function into small class methods
 *               4) Added an EXIT_ABNORMAL if no DTD was found to cause throwing
 *                  an Ant BuildException.  Purpose: DOCTYPE not found will
 *                  cause the X3dCanonicalizer to fail.
 *
 *     Date:     22 August 2006
 *     Time:     1232
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Revised checking of arguments from the command line.  Now
 *                  checking for final/transitional switch before parsing the
 *                  scene file to validate the DTD
 *
 *     Date:     20 September 2006
 *     Time:     2235
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Implemented a check for a canonical 3.0, or 3.1 final
 *                  DTD.  If found, does not modify scene file preventing
 *                  triggering of unnecessary Ant tasks for Savage Model Archive
 *                  building
 *               2) Major refactoring by implementing enumerated case checking
 *                  via a switch statement in determineAndProcessDOCTYPE()
 *
 *     Date:     27 JAN 07
 *     Time:     2001
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Implemented JAXP Schema validation for input scene.  Must be
 *                  online for validation to occur.
 *               2) Validation errors are reported in the format -
 *                  <(file: row, column) error>
 *
 *     Date:     27 FEB 2007
 *     Time:     2316
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Refactored to better accomodate handling a read-only file.
 *                  Will not System.exit(-1) as before.  X3dCanonicalizer can
 *                  process a read-only file as long as XML header and DTD are
 *                  valid.
 *               2) Provided an accessor method to the active scene for the
 *                  X3dCanonicalizer due to consolidation of scene retrieval
 *                  methods between this class and the X3dCanonicalizer.
 *               3) Criteria to be online for validation now removed.  X3D scene
 *                  will be resolved against the local DTD and validated against
 *                  the local Schema.
 *               4) Refactored exit() method formal parameter to be of type
 *                  RuntimeException with a specific message indicating why it
 *                  was thrown.  The prior call to force a sysexit caused
 *                  undesired JUnit test results.  Test methods can now be
 *                  annotated with the type of Throwable to expect allowing
 *                  JUnit tests to succeed and not cause Ant build failures.
 *               5) Refactored DTD found type messages to be more verbose
 *
 *     Date:     15 MAR 2007
 *     Time:     1629
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Added optional Schema validation switch.  Updated usage 
 *                  message to reflect.
 *               2) Refactored setSceneContent() for cleaner parsing of command-
 *                  line arguments
 *               3) Strengthened tests for null arguments
 * 
 *     Date:     02 DEC 2007
 *     Time:     0124Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Added support for X3D 3.2
 *     
 *     Date:     25 MAR 2008
 *     Time:     1957Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.X3dHeaderChecker">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Renamed to X3dHeaderChecker
 * 
 *     Date:     28 AUG 2011
 *     Author:   Don Brutzman
 *     Comments: 1) Added support for X3D 3.3
 *   </b></pre>
 * </p>
 */
public class X3dHeaderChecker implements X3dToolsConstants {

    /** log4j logger instance */
    static Logger log = Logger.getLogger(X3dHeaderChecker.class);

    private boolean readOnlyFile = false;

    private boolean setFinalDTD = false;

    private boolean setTransitionalDTD = false;

    private boolean saveFile = true;

    /** Enumerations of our specified DTD cases */
    private enum DTDCase {

        FINAL_30_DTD, FINAL_CANONICAL_30_DTD, FINAL_31_DTD, FINAL_CANONICAL_31_DTD, FINAL_32_DTD, FINAL_CANONICAL_32_DTD, FINAL_33_DTD, FINAL_CANONICAL_33_DTD, TRANSITIONAL_30_DTD, TRANSITIONAL_31_DTD, NON_STANDARD_DTD, NO_DTD, SCENE_IS_READ_ONLY
    }

    private ByteBuffer bb;

    /** Class instance of our DTDCase enumeration */
    private DTDCase dtdCase;

    private FileInputStream fis;

    private FileChannel fc;

    private RandomAccessFile raf;

    private String x3dFileName;

    private String scene = null;

    private String revisedScene = null;

    /** Schema validator instance */
    private ValidationTool validator;

    /**
     * Creates a new instance of X3dHeaderChecker
     * @param args command line arguments
     */
    public X3dHeaderChecker(String[] args) {
        if (args.length == 0) {
            log.warn(DTD_USAGE_MESSAGE);
            saveFile = false;
            exit(new NullPointerException("arguments are null"));
        }
        setSceneContent(args);
        if (validator != null) {
            if (isWellFormedX3D()) {
                log.info(x3dFileName + " is well-formed");
            } else {
                log.error("Validation errors encountered for: " + x3dFileName + "\nCheck /www.web3d.org/x3d/tools/canonical/ValidationErrors.log for details");
                saveFile = false;
                exit(new RuntimeException("Unrecoverable XML validation errors encountered"));
            }
        }
        processXMLHeader();
        determineAndProcessDOCTYPE();
        exit(null);
    }

    /** Accessor method for the X3dCanonicalizer.  If no modifications
     * to the scene DTD were necessary, then the original scene DTD persists
     * and original scene will be returned. </p>
     * @return either the revised, or unmodified scene to the caller
     */
    public String getActiveScene() {
        return (getRevisedScene() == null) ? getScene() : getRevisedScene();
    }

    /** Accessor method for the Schema ValidationTool.  Once loaded with the
     * schema and subject X3D scene, the X3dCanonicalizer can use it since it
     * has an instance of this class. </p>
     * @return the Schema ValidationTool instance used to pre-validate the
     *         current X3D scene
     */
    public ValidationTool getValidator() {
        return validator;
    }

    protected String getScene() {
        return scene;
    }

    protected void setScene(String s) {
        scene = s;
    }

    protected void setRevisedScene(String s) {
        revisedScene = s;
    }

    protected String getRevisedScene() {
        return revisedScene;
    }

    /**
     * Checks for argument compliance and sets the scene as a String
     * @param args a String[] containing command line arguments
     */
    private void setSceneContent(String[] args) {
        boolean validate = false;
        for (String arg : args) {
            if (arg.contains(FINAL_DTD) || arg.contains("-setFinalDTD")) {
                setFinalDTD = true;
            } else if (arg.contains(TRANSITIONAL_DTD) || arg.contains("-setTransitionalDTD")) {
                setTransitionalDTD = true;
            } else if (arg.contains("-v")) {
                validate = true;
            } else if (arg.contains(".x3d")) {
                x3dFileName = arg;
            } else {
                log.warn(DTD_USAGE_MESSAGE);
                saveFile = false;
                exit(new IllegalArgumentException("arguments not properly set"));
            }
        }
        if (validate) {
            validator = new ValidationTool(x3dFileName, X3D_33_SCHEMA);
        }
        setScene(retrieveFileContent(x3dFileName));
    }

    /**
     * Loads the content of a file into a String which returns for parsing
     * @param fileName the name of the file to load
     * @return String representing contents of the scene file
     */
    private String retrieveFileContent(String fileName) {
        try {
            fis = new FileInputStream(fileName);
            fis.close();
        } catch (FileNotFoundException fnf) {
            log.warn(DTD_USAGE_MESSAGE);
            saveFile = false;
            exit(new RuntimeException("scene \"" + fileName + "\" not found", fnf));
        } catch (IOException ioe) {
            log.fatal(ioe);
        }
        try {
            raf = new RandomAccessFile(fileName, "rwd");
            fc = raf.getChannel();
            bb = ByteBuffer.allocate((int) fc.size());
            fc.read(bb);
        } catch (IOException ioe) {
            readOnlyFile = true;
        }
        if (raf == null) {
            try {
                raf = new RandomAccessFile(fileName, "r");
                log.warn("Scene is read-only!");
                fc = raf.getChannel();
                bb = ByteBuffer.allocate((int) fc.size());
                fc.read(bb);
            } catch (IOException ioe) {
                saveFile = false;
                exit(new RuntimeException("unable to read scene \"" + fileName + "\"", ioe));
            }
        }
        bb.flip();
        String returnString = new String(bb.array());
        bb = null;
        return returnString;
    }

    /** Employs an EntityResolver to point to a local DTD should a machine be
     * offline while validating X3D scenes.
     * @return indication of well-formedness and valid X3D
     */
    private boolean isWellFormedX3D() {
        return validator.isWellFormedX3D();
    }

    private void processXMLHeader() {
        Pattern patternXmlHeader = Pattern.compile(REGEX_XML_HEADER);
        Matcher matcherXmlHeader = patternXmlHeader.matcher(getScene());
        if (matcherXmlHeader.find()) {
            log.debug("valid XML for X3D declaration found");
        } else {
            saveFile = false;
            exit(new RuntimeException("XML declaration in " + x3dFileName.substring(x3dFileName.lastIndexOf("/") + 1) + " does not comply with X3D specification"));
        }
    }

    private void determineAndProcessDOCTYPE() {
        Pattern patternFinalCanonical30Doctype = Pattern.compile(FINAL_30_DOCTYPE);
        Matcher matcherFinalCanonical30Doctype = patternFinalCanonical30Doctype.matcher(getScene());
        Pattern patternFinal30Doctype = Pattern.compile(REGEX_FINAL_30_DOCTYPE);
        Matcher matcherFinal30Doctype = patternFinal30Doctype.matcher(getScene());
        Pattern patternFinalCanonical31Doctype = Pattern.compile(FINAL_31_DOCTYPE);
        Matcher matcherFinalCanonical31Doctype = patternFinalCanonical31Doctype.matcher(getScene());
        Pattern patternFinal31Doctype = Pattern.compile(REGEX_FINAL_31_DOCTYPE);
        Matcher matcherFinal31Doctype = patternFinal31Doctype.matcher(getScene());
        Pattern patternFinalCanonical32Doctype = Pattern.compile(FINAL_32_DOCTYPE);
        Matcher matcherFinalCanonical32Doctype = patternFinalCanonical32Doctype.matcher(getScene());
        Pattern patternFinal32Doctype = Pattern.compile(REGEX_FINAL_32_DOCTYPE);
        Matcher matcherFinal32Doctype = patternFinal32Doctype.matcher(getScene());
        Pattern patternFinalCanonical33Doctype = Pattern.compile(FINAL_33_DOCTYPE);
        Matcher matcherFinalCanonical33Doctype = patternFinalCanonical33Doctype.matcher(getScene());
        Pattern patternFinal33Doctype = Pattern.compile(REGEX_FINAL_33_DOCTYPE);
        Matcher matcherFinal33Doctype = patternFinal33Doctype.matcher(getScene());
        Pattern patternTransitional30Doctype = Pattern.compile(REGEX_TRANSITIONAL_30_DOCTYPE);
        Matcher matcherTransitional30Doctype = patternTransitional30Doctype.matcher(getScene());
        Pattern patternTransitional31Doctype = Pattern.compile(REGEX_TRANSITIONAL_31_DOCTYPE);
        Matcher matcherTransitional31Doctype = patternTransitional31Doctype.matcher(getScene());
        Pattern patternAnyDoctype = Pattern.compile(REGEX_ANY_DOCTYPE);
        Matcher matcherAnyDoctype = patternAnyDoctype.matcher(getScene());
        if (matcherFinalCanonical30Doctype.find()) {
            dtdCase = DTDCase.FINAL_CANONICAL_30_DTD;
            log.info("final canonical 3.0 DOCTYPE found");
        } else if (matcherFinal30Doctype.find()) {
            dtdCase = DTDCase.FINAL_30_DTD;
            log.info("final non-canonical 3.0 DOCTYPE found");
        } else if (matcherFinalCanonical31Doctype.find()) {
            dtdCase = DTDCase.FINAL_CANONICAL_31_DTD;
            log.info("final canonical 3.1 DOCTYPE found");
        } else if (matcherFinal31Doctype.find()) {
            dtdCase = DTDCase.FINAL_31_DTD;
            log.info("final non-canonical 3.1 DOCTYPE found");
        } else if (matcherFinalCanonical32Doctype.find()) {
            dtdCase = DTDCase.FINAL_CANONICAL_32_DTD;
            log.info("final canonical 3.2 DOCTYPE found");
        } else if (matcherFinal32Doctype.find()) {
            dtdCase = DTDCase.FINAL_32_DTD;
            log.info("final non-canonical 3.2 DOCTYPE found");
        } else if (matcherFinalCanonical33Doctype.find()) {
            dtdCase = DTDCase.FINAL_CANONICAL_33_DTD;
            log.info("final canonical 3.3 DOCTYPE found");
        } else if (matcherFinal33Doctype.find()) {
            dtdCase = DTDCase.FINAL_33_DTD;
            log.info("final non-canonical 3.3 DOCTYPE found");
        } else if (matcherTransitional30Doctype.find()) {
            dtdCase = DTDCase.TRANSITIONAL_30_DTD;
            log.info("transitional 3.0 DOCTYPE found");
        } else if (matcherTransitional31Doctype.find()) {
            dtdCase = DTDCase.TRANSITIONAL_31_DTD;
            log.info("transitional 3.1 DOCTYPE found");
        } else if (matcherAnyDoctype.find()) {
            dtdCase = DTDCase.NON_STANDARD_DTD;
        } else {
            dtdCase = DTDCase.NO_DTD;
        }
        if (readOnlyFile) {
            dtdCase = DTDCase.SCENE_IS_READ_ONLY;
        }
        if (setFinalDTD) {
            log.debug("set for final DTD");
        } else if (setTransitionalDTD) {
            log.debug("set for transitional DTD");
        }
        switch(dtdCase) {
            case FINAL_CANONICAL_30_DTD:
                if (setFinalDTD) {
                    saveFile = false;
                    log.info("-f final DTD was set, no action taken");
                } else if (setTransitionalDTD) {
                    matcherFinalCanonical30Doctype.reset();
                    setRevisedScene(matcherFinalCanonical30Doctype.replaceFirst(WARNING_COMMENT + TRANSITIONAL_30_DOCTYPE));
                    log.info("scene will reset to a transitional DTD");
                    saveFile = true;
                }
                break;
            case FINAL_30_DTD:
                if (setFinalDTD) {
                    matcherFinal30Doctype.reset();
                    revisedScene = matcherFinal30Doctype.replaceFirst(FINAL_30_DOCTYPE);
                    log.info("scene will reset to canonical form");
                    saveFile = true;
                } else if (setTransitionalDTD) {
                    matcherFinal30Doctype.reset();
                    setRevisedScene(matcherFinal30Doctype.replaceFirst(WARNING_COMMENT + TRANSITIONAL_30_DOCTYPE));
                    log.info("scene will reset to a transitional DTD");
                    saveFile = true;
                }
                break;
            case FINAL_CANONICAL_31_DTD:
                if (setFinalDTD) {
                    saveFile = false;
                    log.info("-f final DTD was set, no action taken");
                } else if (setTransitionalDTD) {
                    matcherFinalCanonical31Doctype.reset();
                    setRevisedScene(matcherFinalCanonical31Doctype.replaceFirst(WARNING_COMMENT + TRANSITIONAL_31_DOCTYPE));
                    log.info("scene will reset to a transitional DTD");
                    saveFile = true;
                }
                break;
            case FINAL_31_DTD:
                if (setFinalDTD) {
                    matcherFinal31Doctype.reset();
                    revisedScene = matcherFinal31Doctype.replaceFirst(FINAL_31_DOCTYPE);
                    log.info("scene will reset to canonical form");
                    saveFile = true;
                } else if (setTransitionalDTD) {
                    matcherFinal31Doctype.reset();
                    setRevisedScene(matcherFinal31Doctype.replaceFirst(WARNING_COMMENT + TRANSITIONAL_31_DOCTYPE));
                    log.info("scene will reset to a transitional DTD");
                    saveFile = true;
                }
                break;
            case FINAL_CANONICAL_32_DTD:
                if (setFinalDTD) {
                    saveFile = false;
                    log.info("-f final DTD was set, no action taken");
                }
                break;
            case FINAL_32_DTD:
                if (setFinalDTD) {
                    matcherFinal32Doctype.reset();
                    revisedScene = matcherFinal32Doctype.replaceFirst(FINAL_32_DOCTYPE);
                    log.info("scene will reset to canonical form");
                    saveFile = true;
                }
                break;
            case FINAL_CANONICAL_33_DTD:
                if (setFinalDTD) {
                    saveFile = false;
                    log.info("-f final DTD was set, no action taken");
                }
                break;
            case FINAL_33_DTD:
                if (setFinalDTD) {
                    matcherFinal33Doctype.reset();
                    revisedScene = matcherFinal33Doctype.replaceFirst(FINAL_33_DOCTYPE);
                    log.info("scene will reset to canonical form");
                    saveFile = true;
                }
                break;
            case TRANSITIONAL_30_DTD:
                if (setFinalDTD) {
                    matcherTransitional30Doctype.reset();
                    revisedScene = matcherTransitional30Doctype.replaceFirst(FINAL_30_DOCTYPE);
                    setRevisedScene(revisedScene.replaceAll(WARNING_REGEX, ""));
                    log.info("scene will reset to a final DTD");
                    saveFile = true;
                } else if (setTransitionalDTD) {
                    saveFile = false;
                    log.info("-t transitional DTD was set, no action taken");
                }
                break;
            case TRANSITIONAL_31_DTD:
                if (setFinalDTD) {
                    matcherTransitional31Doctype.reset();
                    revisedScene = matcherTransitional31Doctype.replaceFirst(FINAL_31_DOCTYPE);
                    setRevisedScene(revisedScene.replaceAll(WARNING_REGEX, ""));
                    log.info("scene will reset to a final DTD");
                    saveFile = true;
                } else if (setTransitionalDTD) {
                    saveFile = false;
                    log.info("-t transitional DTD was set, no action taken");
                }
                break;
            case NON_STANDARD_DTD:
                log.warn("no action taken, functionality not yet implemented...");
                log.error("Scene with non-standard DTD: " + x3dFileName);
                saveFile = false;
                exit(new RuntimeException("Nonstandard X3D DOCTYPE found!"));
            case NO_DTD:
                log.warn("no action taken, functionality not yet implemented...");
                log.error("Scene with no DTD: " + x3dFileName);
                saveFile = false;
                exit(new RuntimeException("No X3D DOCTYPE found!"));
            case SCENE_IS_READ_ONLY:
                log.info("Unable to modify read-only");
                saveFile = false;
                break;
            default:
                saveFile = false;
                exit(new RuntimeException("Undetermined DTD case"));
        }
    }

    /**
     * Cleans up resources upon a normal process.  Executes a RumtimeException
     * should one be encountered during an abnormal process.  This will be
     * highlighted in the Ant build output to identify the problem.
     * </p>
     * @param re the message prepared RuntimeException to throw if not null
     */
    private void exit(RuntimeException re) {
        if (!saveFile) {
            if (x3dFileName != null) {
                log.info("Scene \"" + x3dFileName + "\" was not modified");
            }
            if (re != null) {
                throw re;
            }
        } else if (getRevisedScene() != null) {
            writeFileContent(getRevisedScene());
            log.info("Modifying scene DTD");
        }
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException ioe) {
            log.error(ioe);
            System.exit(EXIT_ABNORMAL);
        } finally {
            x3dFileName = null;
            bb = null;
            raf = null;
        }
    }

    /**
     * Writes out to file any changes of the DTD in scene file if flagged to
     * @param s content being modified
     */
    private void writeFileContent(String s) {
        try {
            bb = ByteBuffer.wrap(s.getBytes());
            fc.truncate(s.length());
            fc.position(0);
            fc.write(bb);
        } catch (IOException ioe) {
            log.error(ioe);
        }
    }

    /**
     * Command line entry point for the program
     * @param args the command line arguments (if any)
     */
    public static void main(String[] args) {
        new X3dHeaderChecker(args);
    }
}
