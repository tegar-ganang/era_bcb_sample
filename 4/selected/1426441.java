package org.web3d.x3d.tools.x3db;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.util.Arrays.sort;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.web3d.x3d.tools.ValidationTool;
import org.web3d.x3d.tools.X3dHeaderChecker;
import org.web3d.x3d.tools.X3dToolsConstants;

/**
 * Converts an X3D scene graph file into X3D canonical form observing most XML
 * canonical rules except where specified in:
 * <a href="http://www.web3d.org/x3d/specifications/ISO-IEC-19776-3-X3DEncodings-CompressedBinary/Part03/concepts.html#X3DCanonicalForm">X3D Canonical Form</a>
 * and validates the final canonicalized against the X3D 3.2 Schema.
 * <p><pre><code>[Usage] java -jar /www.web3d.org/x3d/tools/canonical/dist/lib/X3dC14n.jar &lt;path&gt;/SceneName.x3d</code>
 *     or <code>java -jar /www.web3d.org/x3d/tools/canonical/dist/lib/X3dC14n.jar &lt;path&gt;/SceneName.x3d &lt;path&gt;/SceneNameCanonical.xml</code></pre></p>
 *
 * @author <a href="mailto:brutzman@nps.edu?subject=X3D%20Tools">Don Brutzman</a>
 * @version $Id: X3dCanonicalizer.java 10405 2012-02-12 00:33:55Z brutzman $
 * <p>
 *   <b>Latest Modifications:</b>
 *   <pre><b>
 *     Date:     05 December 2005
 *     Time:     2355
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Relieved Don Brutzman of X3D C14N implementation
 *               2) Capitalized final variables
 *               3) Updated javadoc comments
 *               4) Restructured to eliminate static variables and methods to
 *                  enable running this class on a server
 *               5) Split up main() method to into smaller methods (constructor
 *                  does the work now)
 *
 *     Date:     24 Janurary 2006
 *     Time:     1100
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Enabled sorting each element to frontload those containing
 *                  DEF, USE and containerField attribute-value pairs by using
 *                  a strict regex pattern as the splitting delimeter
 *               2) Fixed bug to compensate for extra whitespace encountered
 *                  while parsing DOCTYPE elements
 *               3) Fixed bug in processCDATASection() where loop was bypassing
 *                  invocation of this method to due to improper substring
 *                  matching of beginning CDATA tag
 *               4) Fixed bug for closing element tags to indent even with the
 *                  element tag opening index (aligned brackets vertically)
 *                  accounting for child elements
 *               5) Introduced log4j logging
 *               6) Introduced use of Java 1.5 generics for collections
 *               7) Corrected bug where scene was getting written to file before
 *                  undergoing c14n
 *               8) Forces scenes to v3.1 DTDs
 *               9) Corrected bug to write singleton tags for elements that had
 *                  no children and an empty closing tag.
 *              10) setInitialScene() calls a class loader that finds and loads
 *                  a user.properties file that contains current directory
 *                  information.  Because netBeans overrides the system user.dir
 *                  property, and, if a project is not located on the root
 *                  directory, then the default directory will be the netBeans
 *                  install directory (where the JVM is invoked for running
 *                  netBeans).  From user.properties we obtain the examples file
 *                  directory to feed into this class (done by Ant).
 *              11) Provided a constructor to take in a scene in String form for
 *                  use in the Xj3D codebase.  Final c14n scene can be retrieved
 *                  via the getFinalC14nScene() method
 *
 *     Date:     15 February 2006
 *     Time:     1020
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Now creating an examples.canonical directory to store c14n
 *                  files to conduct diffs against the orginal scene files to
 *                  validate this canonicalizer
 *               2) Fixed bug in regex expression for determining xml
 *                  declarations other than v1.0 or v1.1.
 *               3) Created support for either XML version 1.0, or 1.1
 *
 *     Date:     10 April 2006
 *     Time:     1206
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) XML header and DOCTYPE checking now done with the
 *                  X3dHeaderChecker (incorporated into the build).  Internal
 *                  DOCTYPE handled as well.  Supports XML v1.0 and v1.1 and
 *                  final DOCTYPEs v3.0 and v3.1.  X3dHeaderChecker is forced to
 *                  provide final DOCTYPEs in this application however.
 *               2) Implemented more strictness in processAttributes() for
 *                  replacing embedded ' in SFString values and MFString arrays
 *                  with the &apos; character entity, especially in Text nodes.
 *               3) Broke out constants into an interface class
 *                  X3dbToolsConstants and replaced most magic numbers with
 *                  appropriate string lengths
 *               4) Corrected a bug in getSceneContent() that caused a system
 *                  lock on the original scene file that sometimes wouldn't
 *                  release in time for DiffDog to perform comparisons with the
 *                  c14n version
 *
 *     Date:     30 June 2006
 *     Time:     0041
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Closed InputStream after loading properties in
 *                  setSceneContent()
 *               2) Renamed the working directory property key to match the
 *                  amended key in the build.properties file to inline with
 *                  www.web3d.org directory structure
 *
 *     Date:     21 July 2006
 *     Time:     1613
 *     Author:   <a href="mailto:brutzman@nps.edu?subject=X3dCanonicalizer">Don Brutzman</a>
 *     Comments: 1) Fixed a bug in processAttributes() that was replacing "
 *                  characters with the &quot character entity in MFString
 *                  value delimiting
 *               2) Now replacing and numeric character references &#34; and
 *                  &#39; and/or character entities &quot; and &apos; with "
 *                  and ' respectively for SF/MFString values delimiting.
 *               3) Placed a debug flag in above feature methods to promote
 *                  quicker diffing resolution
 *               4) Fixed a major bug in processElement() that was calling an
 *                  unnecessary recursion on child elements under a parent node.
 *                  The simple fix was to just set the proper indentation
 *                  on child elements.  The recursive call was a carry-over from
 *                  the original authoring of this Class.
 *
 *     Date:     20 August 2006
 *     Time:     2112
 *     Author:   <a href="mailto:brutzman@nps.edu?subject=X3dCanonicalizer">Don Brutzman</a>
 *     Comments: 1) Incorporated normalization of whitespace in comments.  X3D-
 *                  Edit has a nasty habit of clobbering comment normalization.
 *
 *     Date:     24 September 2006
 *     Time:     1855
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Incorporated call to the X3dHeaderChecker for single .x3d file
 *                  processing promoting command-line usage of application.
 *               2) Refactored setSceneContent() to work with any single file
 *                  argument entered from the command-line by removing hard
 *                  coded results path.  No longer loading a user.properties
 *                  file, yet still retaining a relative .canonical path to
 *                  place results for diffing
 *               3) Created a sceneStringLength int variable to take the place
 *                  of sceneString.length() calls in loops (costly) and also to
 *                  initialize the c14nStringBuilder with.  Refactored local
 *                  *.length() calls in loops to ints as well.
 *               4) Attempted to canonicalize internal DOCTYPE subsets (hack),
 *                  but better than before.
 *
 *     Date:     26 OCT 06
 *     Time:     1043
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Fixed bug in setSceneContent that when invoked from the
 *                  command line, method did not check for Windows file
 *                  seperators.  Thanks Alan Hudson!
 *               2) Modified usage message to reflect command line jar
 *                  invocation
 *
 *     Date:     24 NOV 06
 *     Time:     1700
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Added extra check for command line call to create a .xml
 *                  canonical copy of the .x3d undergoing c14n (for Don's 3D
 *                  model archive building)
 *               2) Updated usage message to reflect this addition
 *
 *     Date:     25 DEC 06
 *     Time:     2237
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Implemented rule for eliminating empty attribute-value pairs
 *               2) Implemented rule for eliminating comma separators in MF-type
 *                  array values.  Does not check for commas in "meta,"
 *                  "Viewpoint" or other text type attributes.
 *
 *     Date:     27 JAN 07
 *     Time:     2001
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Now validates the final canonicalized scene against the v3.1
 *                  X3D Schema.  Validation errors are reported in the format -
 *                  &lt;(file: row, column) error&gt;
 *               2) Added accessor and setter methods for the original scene
 *                  String for XMLUnit testing purposes.
 *               3) Since the X3dHeaderChecker validates the scene first, we now
 *                  call the scene String from it to process in this class.
 *                  Removed retrieveFileContent() subsequently.
 *
 *     Date:     28 FEB 2007
 *     Time:     1650
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Added support to fix Bug 1155 in setSceneContent(String[])
 *               2) Functionality added to comply with Bug 162 to enable server-
 *                  side filtering of non-c14n compliant X3D scenes.  Scene is
 *                  first checked for C14n compliance with the isCanonical()
 *                  method.
 *               3) Refactored exit() method formal parameter to be of type
 *                  RuntimeException with a specific message indicating why it
 *                  was thrown.  The prior call to force a sysexit caused
 *                  undesired JUnit test results.  Test methods can now be
 *                  annotated with the type of Throwable to expect allowing
 *                  JUnit tests to succeed and not cause Ant build failures.
 *               4) Refactored to remove code making a seperate ".canonical"
 *                  directory.  Now placing c14n processed file in same
 *                  directory with name sceneNameCanonical.xml
 *               5) Removed class method isWhiteSpace() and replaced with
 *                  Character.isWhitespace().
 *               6) Localized and fixed bug where whitespace was not resolved
 *                  within attribute values themselves.
 *               7) Reversed first fix for Bug 1155 in
 *                  setSceneContent(String[]).  It effectivly broke the X3D Edit
 *                  3D Model archive build logic.
 *               8) Implemented the correct fix for bug 1155 by fixing bug 1199.
 *                  The problem lied in the file writing method
 *                  writeFinalX14nScene() where two critical calls were missing
 *                  in that FileChannel was not being truncated to the size of
 *                  the new scene and it's pointer was not being set to 0 in
 *                  order to completely overwrite a previous scene.
 *
 *     Date:     18 MAR 2007
 *     Time:     1305
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Consolidated method for sorting a-v pairs with method for
 *                  removal of empty a-v pairs.  This fixed a new undocumented
 *                  bug when doing each seperately caused double appending of
 *                  certain a-v pairs.
 *               2) Added support to resolve line feed numeric character
 *                  references (NCR) in attribute values
 *               3) Fixed Bug 1200 where a comma seperator at the very end of an
 *                  MF-type array value, right before the attribute delimeter,
 *                  caused non-standard behavior.  This in turn uncovered a new
 *                  bug where a single space right before an ending a-v pair
 *                  delimiter was not being resolved.
 *               4) While processing AllVrml97Nodes.x3d as a test, uncovered a
 *                  bug in which adjacent pairs &#34; NCRs resulted in total
 *                  attribute deletion due to algorithm attempting to remove
 *                  empty a-v pairs.  Implemented stricter regex matching to
 *                  only remove true empty a-v pairs and not those containing
 *                  adjacent pairs &#34; or &#39; NCRs.
 *               5) Added support to validate c14n scene as an option (Bug 1216)
 *               6) Added support to keep SFString/MFString character values
 *                  verbatim, except for the &quot; character entity (Bug 1215)
 *
 *     Date:     16 APR 2007
 *     Time:     2128
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Critical bug fixes for 1228, 1229 and 1231.
 *               2) Modified setSceneContent() to guard against supplying a
 *                  relative path to scene file.  Forces naming full path to
 *                  scene.
 *
 *     Date:     17 JUL 2007
 *     Time:     2128Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Critical bug fix for 1324 in resolveLineFeeds()
 *
 *     Date:     31 AUG 2007
 *     Time:     0522Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Critical bug fix for 1366 in removeCommaSeparators().
 *               2) Revisit Bug 1324, fixed slight variation on resolving NCR
 *                  line feeds
 *
 *     Date:     11 FEB 2008
 *     Time:     0319Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Modified to catch and deal with a StackOverflow Exception
 *                  thrown by the F16.x3d.  This occurs because the file
 *                  contains too many Schema Validation errors even after
 *                  setting max heap/stack sizes
 *
 *     Date:     06 JUL 2008
 *     Time:     0303Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Bugfix 1727.  This X3dC14n did not expect raw data between
 *                  elements such as &lt;ds:Signature&gt;data....&lt;/ds:Signature&gt;.
 *                  Now that we are signing and encrypting X3D files, this type
 *                  syntax will be encountered.
 *
 *     Date:     03 JUN 2009
 *     Time:     2047Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=org.web3d.x3d.tools.x3db.X3dCanonicalizer">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Do not remove/strip commas in appinfo, or description
 *                  attribute SFString values
 *   </b></pre>
 * </p>
 */
public class X3dCanonicalizer implements X3dToolsConstants {

    /** log4j instance */
    static Logger log = Logger.getLogger(X3dCanonicalizer.class);

    private ByteBuffer bb;

    private FileChannel fc;

    private RandomAccessFile raf;

    /**
     * Canonical StringBuilder (jdk1.5) output (faster than StringBuffer, but
     * only for use in a single thread instance)
     */
    private StringBuilder c14nStringBuilder;

    /** Strings to capture and element and name */
    private String elementName, attributeName;

    /** Current scene element indentation */
    private String indent;

    /** String from loading in contents of a ByteBuffer */
    private String sceneString;

    /** Original X3D scene filename */
    private String x3dFileName;

    /** Name of file that underwent C14n */
    private String x3dFileNameCanonical;

    /** Instance of the X3dHeaderChecker */
    private X3dHeaderChecker x3dHeaderChecker;

    /** Flag to denote if scene is already String contained */
    private boolean isString;

    /** Option to validate against a Schema */
    private boolean validate = false;

    /** Current index of sceneString for character parsing */
    private int sceneStringIndex = 0;

    /** Computation of sceneString length */
    private int sceneStringLength;

    /**
     * Creates a new instance of X3dCanonicalizer.  To extract the c14n scene
     * after processing, call getFinalC14nScene().  Assumption is that the input
     * scene HAS a valid DTD and is a valid XML/X3D file.
     *
     * The regex expression '\z' checks for the end of the input sequence at
     * which time the scene will return and be placed into a String[] at index
     * 0.
     *
     * This constructor is intended to be called from a SAI such as in Xj3D, not
     * from the command line.  When utilizing this constructor, make calls in
     * this manner:
     * <pre>X3dCanonicalizer x3dc = new X3dCanonicalizer(sceneString);
     *if(!isDigitallySigned() && !x3dc.isCanonical()) {
     *    // do something with x3dc.getFinalC14nScene();
     *}
     * </pre></p>
     * @param scene the String representation of the original scene to be
     *        canonicalized.  For use in the Xj3D codebase </p>
     */
    public X3dCanonicalizer(String scene) {
        this(scene.split("\\z"));
    }

    /**
     * Creates a new instance of X3dCanonicalizer and acts for the main() method
     * if invoked from the command line.  When utilizing this constructor, make
     * calls in this manner:
     * <pre>X3dCanonicalizer x3dc = new X3dCanonicalizer(stringArrayArguments);
     *if(!isDigitallySigned() && !x3dc.isCanonical()) {
     *    // do something with x3dc.getFinalC14nScene();
     *}
     *\/\/ Must call close so the *Canonical.x* gets written to file
     *x3dc.close();
     * @param args arguments passed in from the command line naming the path of
     *        the original file to be canonicalized, or scene already in String
     *        form
     */
    public X3dCanonicalizer(String[] args) {
        if (args.length == 0 || args[0].length() == 0) {
            log.warn(C14N_USAGE_MESSAGE);
            exit(new NullPointerException("arguments are null"));
        } else {
            loadSceneFile(args);
        }
    }

    /** Enabler method to check for c14n compliance and must be called
     * @return true iif scene is already c14n compliant
     */
    public boolean isCanonical() {
        processHeaderAndDOCTYPE();
        processNodes(EMPTY_STRING);
        boolean ret = getSceneString().equals(getFinalC14nScene());
        if (ret) {
            log.info(C14N_COMPLIANT);
        } else {
            log.info(C14N_NON_COMPLIANT);
        }
        return ret;
    }

    /** @return true iif a &lt;ds:Signature&gt; element is found.  Basically,
     * leave this file alone and assume it was made canonical before signing
     */
    public boolean isDigitallySigned() {
        String regex = "<ds:Signature>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(sceneString);
        boolean found = matcher.find();
        if (found) {
            log.info(DIGITAL_SIGNATURE_FOUND);
        }
        return found;
    }

    /** Initiate closing of this process. Must be called if the original
     * scene was not c14n compliant in order to write out a compliant scene to
     * file. </p>
     */
    public void close() {
        if (!isString && !isDigitallySigned()) {
            writeFinalC14nScene(getFinalC14nScene());
            if (validate) {
                ValidationTool validator = x3dHeaderChecker.getValidator();
                validator.setX3dFile(x3dFileNameCanonical);
                if (validator.isWellFormedX3D()) {
                    log.info(x3dFileNameCanonical + " is well-formed");
                } else {
                    log.error("Validation errors encountered for: " + x3dFileNameCanonical + "\nCheck /www.web3d.org/x3d/tools/canonical/ValidationErrors.log for details");
                }
            }
        }
        exit(null);
    }

    /**
     * Retrieves the final canonicalized scene in normalized String form.  This
     * scene will not undergo XML Validation here.  For use in the Xj3D
     * codebase.</p>
     * @return the final canonicalized scene in normalized String form
     */
    public String getFinalC14nScene() {
        return c14nStringBuilder.toString().trim();
    }

    protected void nullFinalC14nScene() {
        c14nStringBuilder = null;
    }

    /** @return the original scene as a String for XMLUnit testing */
    protected String getSceneString() {
        return sceneString;
    }

    /** @param s the entire scene string to set */
    protected void setSceneString(String s) {
        sceneString = s;
    }

    private void loadSceneFile(String[] localArgs) {
        log.debug(localArgs[0].charAt(0));
        if (isOpeningTag(localArgs[0].charAt(0))) {
            setSceneString(localArgs[0]);
            isString = true;
            log.debug(localArgs[0]);
            log.debug("Scene is in string form");
            localArgs[0] = null;
        } else {
            setSceneContent(localArgs);
            isString = false;
        }
        sceneStringLength = getSceneString().length();
        c14nStringBuilder = new StringBuilder(sceneStringLength);
    }

    /** @return the name of our X3D Scene file */
    private String getX3dFileName() {
        return x3dFileName;
    }

    /**
     * Determines correct command line usage, sets the initial scene file to be
     * canonicalized as a String and creates a *Canonical.x* result file
     * @param args command line arguments
     */
    private void setSceneContent(String[] args) {
        String sceneName, sceneNamePrefix, workDir;
        for (String arg : args) {
            if (arg.contains(".x3d")) {
                x3dFileName = arg.replaceAll("\\\\", "/");
                workDir = null;
                if (getX3dFileName().contains("/")) {
                    workDir = getX3dFileName().substring(0, getX3dFileName().lastIndexOf("/"));
                } else {
                    log.fatal(C14N_USAGE_MESSAGE);
                    x3dFileName = null;
                    exit(new IllegalArgumentException("Unrecognized command-line options"));
                }
                sceneName = getX3dFileName().substring(getX3dFileName().lastIndexOf("/"));
                sceneNamePrefix = sceneName.substring(0, sceneName.lastIndexOf("."));
                x3dFileNameCanonical = workDir + sceneNamePrefix + "Canonical.x3d";
            } else if (arg.contains("Canonical.xml")) {
                x3dFileNameCanonical = arg;
            } else if (arg.contains("-v")) {
                validate = true;
            } else {
                log.error(C14N_USAGE_MESSAGE);
                exit(new IllegalArgumentException("Unrecognized command-line options"));
            }
        }
        try {
            if (validate) {
                x3dHeaderChecker = new X3dHeaderChecker(new String[] { FINAL_DTD, "-v", getX3dFileName() });
            } else {
                x3dHeaderChecker = new X3dHeaderChecker(new String[] { FINAL_DTD, getX3dFileName() });
            }
            setSceneString(x3dHeaderChecker.getActiveScene());
        } catch (Throwable thr) {
            log.error(thr);
            exit(new RuntimeException(thr));
        }
    }

    /**
     * Parses for and replaces whitespace between end of XML header and X3D node
     */
    private void processHeaderAndDOCTYPE() {
        while (sceneString.charAt(sceneStringIndex) != TAG_CLOSING) {
            appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
        }
        closeTagAndAppendNewline();
        while (sceneString.substring(sceneStringIndex, sceneStringIndex + COMMENT_OPENING.length()).equals(COMMENT_OPENING)) {
            processComment();
        }
        String regex = "dtd\"\\s*\\[";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(sceneString);
        if (matcher.find()) {
            setIndentation(DEFAULT_INDENTATION);
            while (sceneString.charAt(sceneStringIndex) != DOCTYPE_INTERNAL_CLOSING) {
                if (sceneString.charAt(sceneStringIndex) == DOCTYPE_INTERNAL_OPENING) {
                    appendC14nStringBuilderAndNormalize(DOCTYPE_INTERNAL_OPENING);
                    c14nStringBuilder.append(NEWLINE);
                    c14nStringBuilder.append(getIndentation());
                } else if (sceneString.substring(sceneStringIndex, (sceneStringIndex + COMMENT_OPENING.length())).equals(COMMENT_OPENING)) {
                    processComment();
                    if (sceneString.charAt(sceneStringIndex) != DOCTYPE_INTERNAL_CLOSING) {
                        c14nStringBuilder.append(getIndentation());
                    }
                } else if (sceneString.charAt(sceneStringIndex) == TAG_CLOSING) {
                    appendC14nStringBuilderAndNormalize(TAG_CLOSING);
                    c14nStringBuilder.append(NEWLINE);
                    if (sceneString.charAt(sceneStringIndex) != DOCTYPE_INTERNAL_CLOSING) {
                        c14nStringBuilder.append(getIndentation());
                    }
                } else {
                    appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
                }
            }
            appendC14nStringBuilder(DOCTYPE_INTERNAL_CLOSING);
        } else {
            while (sceneString.charAt(sceneStringIndex) != TAG_CLOSING) {
                appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
            }
        }
        closeTagAndAppendNewline();
    }

    /**
     * Appends the Canonical StringBuilder with character content and advances
     * the sceneString index accordingly
     * @param contents the character to append to the StringBuilder
     */
    private void appendC14nStringBuilder(char contents) {
        c14nStringBuilder.append(contents);
        sceneStringIndex++;
    }

    /**
     * Appends the Canonical StringBuilder with character content and advances
     * the sceneString index accordingly
     * @param contents the String to append to the StringBuilder
     */
    private void appendC14nStringBuilder(String contents) {
        c14nStringBuilder.append(contents);
        sceneStringIndex += contents.length();
    }

    /** Closes a tag and begins a newline */
    private void closeTagAndAppendNewline() {
        appendC14nStringBuilder(TAG_CLOSING);
        appendNewline();
    }

    /** Append a newline and cleanup whitespace afterwards */
    private void appendNewline() {
        c14nStringBuilder.append(NEWLINE);
        normalizeWhitespace();
    }

    /** Parses over extraneous whitespace characters */
    private void normalizeWhitespace() {
        while ((sceneStringIndex < sceneStringLength) && Character.isWhitespace(sceneString.charAt(sceneStringIndex))) {
            sceneStringIndex++;
        }
    }

    /** Append character and clean up whitespace afterwards
     * @param contents the character to append to the StringBuilder
     */
    private void appendC14nStringBuilderAndNormalize(char contents) {
        appendC14nStringBuilder(contents);
        normalizeWhitespace();
    }

    /**
     * Process each scene node
     * @param indent current indentation index
     */
    private void processNodes(String indent) {
        setIndentation(indent);
        while (sceneStringIndex < sceneStringLength) {
            if (sceneString.substring(sceneStringIndex, (sceneStringIndex + COMMENT_OPENING.length())).equals(COMMENT_OPENING)) {
                log.debug("Inside comment sceneStringIndex " + sceneStringIndex + " is " + sceneString.charAt(sceneStringIndex));
                c14nStringBuilder.append(getIndentation());
                processComment();
            } else if (sceneString.substring(sceneStringIndex, (sceneStringIndex + ELEMENT_WITH_CHILD_CLOSING.length())).equals(ELEMENT_WITH_CHILD_CLOSING)) {
                log.debug("String between " + sceneStringIndex + " and " + (sceneStringIndex + ELEMENT_WITH_CHILD_CLOSING.length()) + " is " + sceneString.substring(sceneStringIndex, (sceneStringIndex + ELEMENT_WITH_CHILD_CLOSING.length())));
                String indentTa = getIndentation();
                int iLen = Math.max(indentTa.length() - DEFAULT_INDENTATION.length(), 0);
                setIndentation(getIndentation().substring(0, iLen));
                c14nStringBuilder.append(getIndentation());
                terminateElementWithChildren();
            } else if (sceneString.substring(sceneStringIndex, (sceneStringIndex + CDATA_OPENING.length())).equals(CDATA_OPENING)) {
                c14nStringBuilder.append(getIndentation());
                processCDATASection();
            } else {
                c14nStringBuilder.append(getIndentation());
                processElement(getIndentation());
            }
        }
        resolveLineFeeds();
    }

    /**
     * Retrieves the current indentation of an element beginning, or ending
     * @return the current indentation of an element beginning, or ending
     */
    private String getIndentation() {
        return indent;
    }

    /**
     * Sets the current element indentation for the canonicalized scene
     * @param currentIndent the current element indentation
     */
    private void setIndentation(String currentIndent) {
        indent = currentIndent;
    }

    /** Processes scene comments canonically */
    private void processComment() {
        appendC14nStringBuilder(COMMENT_OPENING);
        if (Character.isWhitespace(sceneString.charAt(sceneStringIndex))) {
            appendC14nStringBuilderAndNormalize(NORMAL_SPACING);
        } else {
            c14nStringBuilder.append(NORMAL_SPACING);
        }
        int commentClosingLength = COMMENT_CLOSING.length();
        while (!sceneString.substring(sceneStringIndex - commentClosingLength, sceneStringIndex).equals(COMMENT_CLOSING)) {
            if (Character.isWhitespace(sceneString.charAt(sceneStringIndex))) {
                appendC14nStringBuilderAndNormalize(NORMAL_SPACING);
            } else {
                appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
                if ((sceneString.substring(sceneStringIndex, (sceneStringIndex + COMMENT_CLOSING.length())).equals(COMMENT_CLOSING))) {
                    c14nStringBuilder.append(NORMAL_SPACING);
                }
            }
        }
        appendNewline();
    }

    /** Processes an element with children closing tag */
    private void terminateElementWithChildren() {
        appendC14nStringBuilder(ELEMENT_WITH_CHILD_CLOSING);
        normalizeWhitespace();
        writeElementName();
        closeTagAndAppendNewline();
    }

    /** Processes CDATA sections */
    private void processCDATASection() {
        log.debug("process CDATA section...");
        appendC14nStringBuilder(CDATA_OPENING);
        int cdataClosingLength = CDATA_CLOSING.length();
        while (!sceneString.substring(sceneStringIndex - cdataClosingLength, sceneStringIndex).equals(CDATA_CLOSING)) {
            appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
        }
        appendNewline();
    }

    /**
     * Processes each scene element converting elements to singletons in case of
     * <elementName attribute=""/></elementName>
     * @param indent the current indentation of this element
     */
    private void processElement(String indent) {
        appendC14nStringBuilderAndNormalize(TAG_OPENING);
        int index = c14nStringBuilder.length();
        writeElementName();
        log.debug("Before processAttributes() sceneStringIndex " + sceneStringIndex + " is " + sceneString.charAt(sceneStringIndex) + " for element " + getElementName());
        if (!isTerminator(sceneString.charAt(sceneStringIndex))) {
            processAttributes(index);
        }
        if (sceneString.substring(sceneStringIndex, sceneStringIndex + ELEMENT_SINGLETON_CLOSING.length()).equals(ELEMENT_SINGLETON_CLOSING)) {
            terminateElementSingleton();
        } else if (sceneString.charAt(sceneStringIndex) == TAG_CLOSING) {
            while (Character.isWhitespace(sceneString.charAt(sceneStringIndex + 1))) {
                sceneStringIndex++;
            }
            if (sceneString.substring((sceneStringIndex + 1), (sceneStringIndex + (ELEMENT_WITH_CHILD_CLOSING.length() + 1) + getElementName().length())).contains(ELEMENT_WITH_CHILD_CLOSING + getElementName())) {
                c14nStringBuilder.append(ELEMENT_SINGLETON_CLOSING);
                sceneStringIndex++;
                sceneStringIndex += (ELEMENT_WITH_CHILD_CLOSING.length() + getElementName().length() + 1);
                appendNewline();
                return;
            } else {
                closeTagAndAppendNewline();
            }
            if (!sceneString.substring(sceneStringIndex, (sceneStringIndex + ELEMENT_WITH_CHILD_CLOSING.length())).equals(ELEMENT_WITH_CHILD_CLOSING)) {
                setIndentation(indent + DEFAULT_INDENTATION);
            }
        }
    }

    /** @return the sceneStringIndex of the loaded scene */
    private int getSceneStringIndex() {
        return sceneStringIndex;
    }

    /**
     * Writes the element's name in either an opening or closing tag to the
     * main StringBuilder and captures that name in another for later reference
     */
    private void writeElementName() {
        StringBuilder tempsb = new StringBuilder();
        while (!Character.isWhitespace(sceneString.charAt(sceneStringIndex)) && !isTerminator(sceneString.charAt(sceneStringIndex))) {
            tempsb.append(sceneString.charAt(sceneStringIndex));
            appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
        }
        setElementName(tempsb.toString());
        normalizeWhitespace();
        log.debug("Element name: " + getElementName());
    }

    private String getElementName() {
        return elementName;
    }

    private void setElementName(String name) {
        elementName = name;
    }

    /**
     * Processes each element's attributes, ensuring that DEF, USE and (non-
     * default) containerField pairs appear before other attributes.  Sorts
     * lexicographically the remaining attributes and removes empty attribute-
     * value (a-v) pairs. </p>
     * @param elementNameBeginIndex the beginning index of the current element
     *        name which will be seperated from its attributes in order to
     *        process a-v pairs
     */
    private void processAttributes(int elementNameBeginIndex) {
        do {
            writeAttributeName();
            appendC14nStringBuilderAndNormalize(EQUALS_SIGN);
            if (isQuote(sceneString.charAt(sceneStringIndex))) {
                appendC14nStringBuilderAndNormalize(DEFAULT_ATTRIBUTE_DELIMITER);
                processAttributeValueInQuot();
            } else if (isApostrophe(sceneString.charAt(sceneStringIndex))) {
                appendC14nStringBuilderAndNormalize(DEFAULT_ATTRIBUTE_DELIMITER);
                processAttributeValueInApos();
            }
            resolveWhitespaceBeforeDelimiter();
            appendC14nStringBuilderAndNormalize(DEFAULT_ATTRIBUTE_DELIMITER);
        } while (!isTerminator(sceneString.charAt(sceneStringIndex)));
        resolveWhitespaceBeforeEndingDelimiter();
        int elementNameEndIndex = c14nStringBuilder.length();
        char[] currentAttributes = new char[elementNameEndIndex - elementNameBeginIndex];
        String elementContents;
        String tempAttValPairs;
        StringBuilder sb = new StringBuilder();
        c14nStringBuilder.getChars(elementNameBeginIndex, elementNameEndIndex, currentAttributes, 0);
        elementContents = new String(currentAttributes);
        tempAttValPairs = elementContents.substring(elementContents.indexOf(NORMAL_SPACING) + 1);
        sortAttributes(elementContents, tempAttValPairs, sb, elementNameBeginIndex, elementNameEndIndex);
    }

    /**
     * Writes the attribute's name to the main StringBuilder and captures it in
     * another for later reference
     */
    private void writeAttributeName() {
        StringBuilder tempsb = new StringBuilder();
        c14nStringBuilder.append(NORMAL_SPACING);
        while (!Character.isWhitespace(sceneString.charAt(sceneStringIndex)) && !isEqualsSign(sceneString.charAt(sceneStringIndex))) {
            tempsb.append(sceneString.charAt(sceneStringIndex));
            appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
        }
        setAttributeName(tempsb.toString());
        normalizeWhitespace();
        log.debug("Attribute name: " + getAttributeName());
    }

    private String getAttributeName() {
        return attributeName;
    }

    private void setAttributeName(String name) {
        attributeName = name;
    }

    /** Process all attribute values delimited by quotation marks */
    private void processAttributeValueInQuot() {
        while (!isQuote(sceneString.charAt(sceneStringIndex))) {
            if (isApostrophe(sceneString.charAt(sceneStringIndex))) {
                appendAposCharacterEntity();
            } else {
                processCommonAVCharacters();
            }
            removeCommaSeparators();
        }
    }

    /** Replace ' characters with the &apos; character entity */
    private void appendAposCharacterEntity() {
        c14nStringBuilder.append(CHARACTER_ENTITY_APOS);
        sceneStringIndex++;
    }

    /** Appends commonly encountered attribute value pair characters and ensures
     * normalized whitespace.
     */
    private void processCommonAVCharacters() {
        if (sceneString.substring(sceneStringIndex, (sceneStringIndex + CHARACTER_ENTITY_QUOTE.length())).equals(CHARACTER_ENTITY_QUOTE) && NO_DEBUG) {
            c14nStringBuilder.append(QUOTE_CHAR);
            sceneStringIndex += CHARACTER_ENTITY_QUOTE.length();
        } else if (sceneString.substring(sceneStringIndex, (sceneStringIndex + NUMERIC_CHARACTER_REFERENCE_QUOTE.length())).equals(NUMERIC_CHARACTER_REFERENCE_QUOTE) && NO_DEBUG) {
            c14nStringBuilder.append(QUOTE_CHAR);
            sceneStringIndex += NUMERIC_CHARACTER_REFERENCE_QUOTE.length();
        } else if (sceneString.substring(sceneStringIndex, (sceneStringIndex + NUMERIC_CHARACTER_REFERENCE_APOS.length())).equals(NUMERIC_CHARACTER_REFERENCE_APOS) && NO_DEBUG) {
            c14nStringBuilder.append(CHARACTER_ENTITY_APOS);
            sceneStringIndex += NUMERIC_CHARACTER_REFERENCE_APOS.length();
        } else if (Character.isWhitespace(sceneString.charAt(sceneStringIndex))) {
            if (getAttributeName().equals("string")) {
                log.debug("Attribute name: " + getAttributeName());
                appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
                return;
            } else {
                appendC14nStringBuilderAndNormalize(NORMAL_SPACING);
            }
        } else {
            appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
        }
    }

    /** Removes comma seperators in MF-type array values, except in selected
     * textual type elements.  Whitespace may have been placed after comma
     * seperators by this c14n's normalization process and has to be dealt with
     * here.</p>
     */
    private void removeCommaSeparators() {
        if (getElementName().equals("meta")) {
            return;
        } else if (getElementName().equals("MetadataString")) {
            return;
        } else if (getElementName().equals("Text")) {
            return;
        } else if (getElementName().equals("WorldInfo")) {
            return;
        } else if (getAttributeName().equals("appinfo")) {
            return;
        } else if (getAttributeName().equals("description")) {
            return;
        } else if (getAttributeName().equals("documentation")) {
            return;
        } else if (isComma(sceneString.charAt(sceneStringIndex))) {
            if (Character.isWhitespace(sceneString.charAt(sceneStringIndex - 1))) {
                c14nStringBuilder.deleteCharAt(c14nStringBuilder.length() - 1);
                log.debug("1. whitespace before comma: " + getAttributeName());
            }
            if (Character.isWhitespace(sceneString.charAt(sceneStringIndex + 1))) {
                sceneStringIndex++;
                log.debug("2. whitespace after comma: " + getAttributeName());
                return;
            }
            if ((Character.getNumericValue(sceneString.charAt(sceneStringIndex + 1)) == -1) || (Character.isLetterOrDigit(sceneString.charAt(sceneStringIndex + 1)))) {
                appendC14nStringBuilderAndNormalize(NORMAL_SPACING);
                log.debug("3. comma followed by hyphens, or no whitespace: " + getAttributeName());
            }
        }
    }

    /** Prevent whitespace right before an a-v pair delimiter in the current
     * element
     */
    private void resolveWhitespaceBeforeDelimiter() {
        if (Character.isWhitespace(c14nStringBuilder.charAt(c14nStringBuilder.length() - 1))) {
            c14nStringBuilder.deleteCharAt(c14nStringBuilder.length() - 1);
        }
    }

    /** Prevent whitespace right before the ending a-v pair delimiter in the
     * current element
     */
    private void resolveWhitespaceBeforeEndingDelimiter() {
        if (Character.isWhitespace(c14nStringBuilder.charAt(c14nStringBuilder.length() - 2))) {
            c14nStringBuilder.deleteCharAt(c14nStringBuilder.length() - 2);
        }
    }

    /** Process all attribute values delimited by apostrophes */
    private void processAttributeValueInApos() {
        String regex = "'\\w";
        Pattern pattern = Pattern.compile(regex);
        String tempString;
        Matcher matcher;
        while (!isApostrophe(sceneString.charAt(sceneStringIndex))) {
            tempString = sceneString.substring((sceneStringIndex + 1), (sceneStringIndex + 3));
            log.debug("tempString: " + tempString);
            matcher = pattern.matcher(tempString);
            if (matcher.find()) {
                appendC14nStringBuilder(sceneString.charAt(sceneStringIndex));
                appendAposCharacterEntity();
            } else {
                processCommonAVCharacters();
            }
            removeCommaSeparators();
        }
    }

    /**
     * Alphabetically sorts the contents of the current Element's
     * Attribute-value pairs as DEF, USE and containerField first (if these are
     * present), then alphabetically sorts the remaining a-v pairs.  This
     * method also removes empty a-v pairs. </p>
     * @param elementContents the entire contents of the element being processed
     * @param tempAttValPairs the attribute-value pairs to split into an []
     * @param sb a StringBuilder used to hold processed a-v pairs
     * @param elementNameBeginIndex an element name beginning index
     * @param elementNameEndIndex and element name ending index
     */
    private void sortAttributes(String elementContents, String tempAttValPairs, StringBuilder sb, int elementNameBeginIndex, int elementNameEndIndex) {
        log.debug("Element contents before sorting: " + elementContents);
        String regex = "(?<=[^=]')\\s";
        String[] attributeValuePairs = tempAttValPairs.split(regex);
        sort(attributeValuePairs);
        for (String attributeValuePair : attributeValuePairs) {
            if (attributeValuePair.matches("\\w+=" + EMPTY_APOSTROPHES) || attributeValuePair.matches("\\w+=" + EMPTY_QUOTES)) {
                sb.append(EMPTY_STRING);
            } else if (isDefaultContainerField(elementName, attributeValuePair)) {
                sb.append(EMPTY_STRING);
            } else {
                sb.append(NORMAL_SPACING).append(attributeValuePair.trim());
            }
        }
        log.debug("Element contents after sorting: " + getElementName() + sb.toString());
        c14nStringBuilder.replace((elementNameBeginIndex + elementContents.indexOf(NORMAL_SPACING)), elementNameEndIndex, sb.toString());
    }

    private boolean isDefaultContainerField(String elementName, String attributeValuePair) {
        if (!attributeValuePair.trim().startsWith("containerField")) return false;
        if ((attributeValuePair.contains("appearance") && (elementName.equals("Appearance"))) || (attributeValuePair.contains("material") && (elementName.equals("Material") || elementName.equals("TwoSidedMaterial"))) || (attributeValuePair.contains("geometry") && (elementName.equals("Box") || elementName.equals("Cone") || elementName.equals("Cylinder") || elementName.equals("Sphere") || elementName.equals("Text") || elementName.equals("ElevationGrid") || elementName.equals("Extrusion") || elementName.equals("LineSet") || elementName.equals("IndexedLineSet") || elementName.equals("IndexedFaceSet") || elementName.equals("PointSet") || elementName.equals("TriangleSet") || elementName.equals("TriangleFanSet") || elementName.equals("TriangleStripSet") || elementName.equals("QuadSet") || elementName.equals("IndexedTriangleSet") || elementName.equals("IndexedTriangleFanSet") || elementName.equals("IndexedTriangleStripSet") || elementName.equals("IndexedQuadSet") || elementName.equals("NurbsCurve") || elementName.equals("NurbsPatchSurface") || elementName.equals("NurbsSweptSurface") || elementName.equals("NurbsSwungSurface") || elementName.equals("NurbsTrimmedSurface") || elementName.equals("Arc2D") || elementName.equals("ArcClose2D") || elementName.equals("Circle2D") || elementName.equals("Disk2D") || elementName.equals("Polyline2D") || elementName.equals("PolyPoint2D") || elementName.equals("Rectangle2D") || elementName.equals("TriangleSet2D"))) || (attributeValuePair.contains("color") && (elementName.equals("Color") || elementName.equals("ColorRGBA"))) || (attributeValuePair.contains("coord") && (elementName.equals("Coordinate") || elementName.equals("CoordinateDouble"))) || (attributeValuePair.contains("texture") && (elementName.equals("ImageTexture") || elementName.equals("MovieTexture") || elementName.equals("MultiTexture") || elementName.equals("PixelTexture"))) || (attributeValuePair.contains("texCoord") && (elementName.equals("TextureCoordinate") || elementName.equals("MultiTextureCoordinate") || elementName.equals("NurbsTextureCoordinate") || elementName.equals("TextureCoordinateGenerator"))) || (attributeValuePair.contains("metadata") && (elementName.startsWith("Metadata"))) || (attributeValuePair.contains("source") && (elementName.equals("AudioClip"))) || (attributeValuePair.contains("fillProperties") && (elementName.equals("FillProperties"))) || (attributeValuePair.contains("lineProperties") && (elementName.equals("LineProperties"))) || (attributeValuePair.contains("normal") && (elementName.equals("Normal"))) || (attributeValuePair.contains("textureProperties") && (elementName.equals("TextureProperties"))) || (attributeValuePair.contains("textureTransform") && (elementName.equals("TextureTransform") || elementName.equals("MultiTextureTransform"))) || (attributeValuePair.contains("trimmingContour") && (elementName.equals("Contour2D"))) || (attributeValuePair.contains("children"))) return true; else return false;
    }

    /** Processes closing an element singleton */
    private void terminateElementSingleton() {
        appendC14nStringBuilder(ELEMENT_SINGLETON_CLOSING);
        appendNewline();
    }

    /** Removes instances of the numeric character reference &#10; (line feed)
     * and any whitespace that may result
     */
    private void resolveLineFeeds() {
        Pattern patternLineFeed = Pattern.compile("(\\s)*(" + NUMERIC_CHARACTER_REFERENCE_LINE_FEED + ")+(\\s)*");
        Matcher matcherLineFeed = patternLineFeed.matcher(getFinalC14nScene());
        if (matcherLineFeed.find()) {
            int index = matcherLineFeed.end();
            log.debug("Match found for &#10; NCR: " + matcherLineFeed.group() + " at index: " + index + " as: " + matcherLineFeed.toString());
            matcherLineFeed.reset();
            c14nStringBuilder = new StringBuilder(matcherLineFeed.replaceAll(Character.toString(NORMAL_SPACING)));
        }
        matcherLineFeed = null;
        Pattern patternSpaceBeforeDelimiter = Pattern.compile("\\s'(\\s)*(/|>)");
        Matcher matcherSpaceBeforeDelimiter = patternSpaceBeforeDelimiter.matcher(getFinalC14nScene());
        int index = 0;
        String regexReplace = "";
        StringBuffer sb = new StringBuffer();
        while (matcherSpaceBeforeDelimiter.find()) {
            index = matcherSpaceBeforeDelimiter.end();
            log.debug("Match found for Bug 1324 at index: " + index + " as: " + matcherSpaceBeforeDelimiter.toString());
            regexReplace = "'/";
            if (!matcherSpaceBeforeDelimiter.group().contains("/")) {
                regexReplace = "'>";
                log.debug("Output: \n" + getFinalC14nScene().subSequence(index - 15, index + 15));
            }
            matcherSpaceBeforeDelimiter.appendReplacement(sb, regexReplace);
        }
        c14nStringBuilder = new StringBuilder(matcherSpaceBeforeDelimiter.appendTail(sb));
    }

    /**
     * Determine if character is an apostrophe
     * @param value the character to evaluate
     * @return true if evaluated character is an apostrophe
     */
    private boolean isApostrophe(char value) {
        return (value == DEFAULT_ATTRIBUTE_DELIMITER);
    }

    /**
     * Determines if a character is a comma
     * @param value the character to evaluate
     * @return true if a comma
     */
    private boolean isComma(char value) {
        return value == COMMA_CHAR;
    }

    /**
     * Determines if a character is an equals sign
     * @param value the character to evaluate
     * @return true if an equals sign
     */
    private boolean isEqualsSign(char value) {
        return value == EQUALS_SIGN;
    }

    /**
     * Determine if character is an opening tag '<'
     * @param value the character to evaluate
     * @return true if evaluated character is a '<'
     */
    private boolean isOpeningTag(char value) {
        return value == TAG_OPENING;
    }

    /**
     * Determine if character is a quotation sign
     * @param value the character to evaluate
     * @return true if evaluated character is a quote
     */
    private boolean isQuote(char value) {
        return value == QUOTE_CHAR;
    }

    /**
     * Determines XML header, DOCTYPE, or an element terminator
     * @param value the character to evaluate
     * @return true if '/' or '>'
     */
    private boolean isTerminator(char value) {
        return (value == '/') || (value == TAG_CLOSING);
    }

    /**
     * Writes out the final canonicalized scene to file
     * @param finalScene the scene that underwent X3D c14n
     */
    private void writeFinalC14nScene(String finalScene) {
        try {
            long length = finalScene.length();
            raf = new RandomAccessFile(x3dFileNameCanonical, "rwd");
            fc = raf.getChannel();
            bb = ByteBuffer.allocate((int) length);
            bb = ByteBuffer.wrap(finalScene.getBytes());
            fc.truncate(length);
            fc.position(0);
            fc.write(bb);
        } catch (IOException ioe) {
            log.fatal(ioe);
        }
    }

    /**
     * Releases resources before normal JVM exit.
     * @param re Specific RuntimeException causing JVM exit
     * @throws IOException
     *         If the revised .x3d scene file would not close
     * @throws RuntimeException if another type of exiting issue occured
     */
    private void exit(RuntimeException re) {
        if (re == null) {
            try {
                if (!isString && !isDigitallySigned()) {
                    raf.close();
                }
                bb = null;
                setSceneString(null);
                nullFinalC14nScene();
                setIndentation(null);
                x3dHeaderChecker = null;
            } catch (IOException ioe) {
                log.fatal(ioe);
            }
        } else {
            if (getX3dFileName() != null) {
                log.error("File causing error: " + getX3dFileName());
                if (re.getCause() instanceof StackOverflowError) {
                    log.error("There are multiple Schema Validation errors causing a StackOverflowError Exception to be thrown");
                } else {
                    writeFinalC14nScene(getFinalC14nScene());
                }
            }
            throw re;
        }
    }

    public void addLog4jAppender(Appender app) {
        log.addAppender(app);
    }

    public void removeLog4jAppender(Appender app) {
        log.removeAppender(app);
    }

    public Level setLog4jLevel(Level lev) {
        Level orig = log.getLevel();
        log.setLevel(lev);
        return orig;
    }

    /**
     * Command line entry point for the program.  Contains example of X3dC14n
     * usage.
     * @param args the command line arguments (if any)
     */
    public static void main(String[] args) {
        X3dCanonicalizer x3dc = new X3dCanonicalizer(args);
        String temp = "";
        for (String t : args) {
            if (t.contains("x3d")) {
                temp = t;
            }
        }
        if (x3dc.isDigitallySigned()) {
            log.info(DIGITAL_SIGNATURE_FOUND + " for " + temp);
        } else if (x3dc.isCanonical()) {
            log.info(C14N_COMPLIANT + " for " + temp);
        } else {
            log.info(C14N_NON_COMPLIANT + " for " + temp);
        }
        x3dc.close();
    }
}
