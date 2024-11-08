package org.openscience.nmrshiftdb.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;
import javax.vecmath.Point2d;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.log4j.Category;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;
import org.apache.turbine.om.NumberKey;
import org.apache.turbine.services.db.TurbineDB;
import org.apache.turbine.services.rundata.DefaultTurbineRunData;
import org.apache.turbine.util.Log;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.ServletUtils;
import org.apache.turbine.util.db.Criteria;
import org.apache.turbine.util.db.pool.DBConnection;
import org.apache.turbine.util.mail.MailMessage;
import org.apache.turbine.util.upload.FileItem;
import org.jcamp.parser.JCAMPException;
import org.jcamp.parser.JCAMPReader;
import org.jcamp.spectrum.MinHeightPeakPicking;
import org.jcamp.spectrum.NMRSpectrum;
import org.jcamp.spectrum.Peak;
import org.jcamp.spectrum.Spectrum;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.HueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IChemSequence;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IRing;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.io.MDLWriter;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainerCreator;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.renderer.Renderer2D;
import org.openscience.cdk.renderer.Renderer2DModel;
import org.openscience.cdk.ringsearch.AllRingsFinder;
import org.openscience.cdk.ringsearch.SSSRFinder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.BremserOneSphereHOSECodePredictor;
import org.openscience.cdk.tools.DeAromatizationTool;
import org.openscience.cdk.tools.HOSECodeGenerator;
import org.openscience.cdk.tools.HydrogenAdder;
import org.openscience.cdk.tools.SaturationChecker;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
import org.openscience.nmrshiftdb.om.DBAtom;
import org.openscience.nmrshiftdb.om.DBIsotope;
import org.openscience.nmrshiftdb.om.DBIsotopePeer;
import org.openscience.nmrshiftdb.om.DBMolecule;
import org.openscience.nmrshiftdb.om.DBMoleculePeer;
import org.openscience.nmrshiftdb.om.DBReviewGroup;
import org.openscience.nmrshiftdb.om.DBShift;
import org.openscience.nmrshiftdb.om.DBShiftPeer;
import org.openscience.nmrshiftdb.om.DBSignal;
import org.openscience.nmrshiftdb.om.DBSignalDBAtomPeer;
import org.openscience.nmrshiftdb.om.DBSignalPeer;
import org.openscience.nmrshiftdb.om.DBSpectrumPeer;
import org.openscience.nmrshiftdb.om.DBSpectrumType;
import org.openscience.nmrshiftdb.om.DBSpectrumTypePeer;
import org.openscience.nmrshiftdb.om.NmrshiftdbUser;
import org.openscience.nmrshiftdb.om.NmrshiftdbUserPeer;
import org.openscience.nmrshiftdb.util.exception.NmrshiftdbException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import JSX.ObjIn;
import JSX.ObjOut;
import com.workingdogs.village.Record;

/**
 *Contains some utils not fitting in somewhere else
 *
 * @author     shk3
 * @created    23. April 2002
 */
public class GeneralUtils {

    /**
   *  Description of the Field
   */
    public static Properties nmrprops = null;

    /**
   *  Maximum number of hits to display
   */
    public static final int MAX_HITS_TO_DISPLAY = 300;

    static Category sqlLogger = null;

    static Properties jetspeedprops = null;

    static String emailSignature = null;

    static Vector atomsWithHoseCode = null;

    static HashMap mb3d = new HashMap();

    static GeneralUtils instance = new GeneralUtils();

    public static Vector alreadyAttached = new Vector();

    /**
   *Constructor for the GeneralUtils object (private since only static methods)
   */
    private GeneralUtils() {
    }

    public static GeneralUtils getInstance() {
        return instance;
    }

    /**
   *  Gets the adminEmail from TurbineRessources.properties
   *
   * @param  runData          The RunData object
   * @return                  The adminEmail value
   * @exception  IOException  Error reading file
   */
    public static String getAdminEmail(RunData runData) throws IOException {
        if (runData == null) {
            return (jetspeedprops.getProperty("mail.support"));
        } else {
            return (getAdminEmail(runData.getServletConfig()));
        }
    }

    public static String getShort(String input) {
        if (input.length() < 30) {
            return input;
        } else {
            return "<a href=\"javascript:alert('" + input + "')\">" + input.substring(0, 30) + "... (truncated)</a>";
        }
    }

    /**
   *  Gets a Vector containing all element symbols from isotope table except H.
   *
   * @return                The atomsWithHoseCode value
   * @exception  Exception  Description of Exception
   */
    public static Vector getAtomsWithHoseCode() throws Exception {
        if (atomsWithHoseCode == null) {
            Vector v = DBIsotopePeer.doSelect(new Criteria());
            atomsWithHoseCode = new Vector();
            for (int i = 0; i < v.size(); i++) {
                if (!((DBIsotope) v.get(i)).getElementSymbol().equals("H")) {
                    atomsWithHoseCode.add(((DBIsotope) v.get(i)).getElementSymbol());
                }
            }
        }
        return (atomsWithHoseCode);
    }

    /**
   *Gets the adminEmail from TurbineRessources.properties
   *
   * @param  config           Teh servletConfig object
   * @return                  The adminEmail value
   * @exception  IOException  Error reading file
   */
    public static String getAdminEmail(ServletConfig config) throws IOException {
        if (jetspeedprops == null) {
            String jetspeedpropsFile = ServletUtils.expandRelative(config, "/WEB-INF/conf/JetspeedResources.properties");
            jetspeedprops = new Properties();
            jetspeedprops.load(new FileInputStream(jetspeedpropsFile));
        }
        return (jetspeedprops.getProperty("mail.support"));
    }

    /**
   *  Gets the smtpServer from TurbineRessources.properties
   *
   * @param  runData          The RunData object
   * @return                  The smtpServer value
   * @exception  IOException  Error reading file
   */
    public static String getSmtpServer(RunData runData) throws IOException {
        if (runData == null) {
            return (jetspeedprops.getProperty("mail.server"));
        } else {
            return (getSmtpServer(runData.getServletConfig()));
        }
    }

    /**
   *  Gets the smtpServer from TurbineRessources.properties
   *
   * @param  config           Description of Parameter
   * @return                  The smtpServer value
   * @exception  IOException  Error reading file
   */
    public static String getSmtpServer(ServletConfig config) throws IOException {
        if (jetspeedprops == null) {
            String jetspeedpropsFile = ServletUtils.expandRelative(config, "/WEB-INF/conf/JetspeedResources.properties");
            jetspeedprops = new Properties();
            jetspeedprops.load(new FileInputStream(jetspeedpropsFile));
        }
        return (jetspeedprops.getProperty("mail.server"));
    }

    /**
   *  Gets an VectorWithSessionBinding attribute from the session
   *
   * @param  session    The current session
   * @param  attribute  Description of Parameter
   * @return            The StructuresHistory value
   */
    public static Vector getVectorWithSessionBindingAttributeFromSession(HttpSession session, String attribute) {
        if (session.getAttribute(attribute) != null) {
            return (VectorWithSessionBinding) session.getAttribute(attribute);
        } else {
            VectorWithSessionBinding v = null;
            if (DefaultTurbineRunData.getUserFromSession(session) == null) {
                v = new VectorWithSessionBinding(null);
            } else {
                v = new VectorWithSessionBinding(DefaultTurbineRunData.getUserFromSession(session).getUserName());
            }
            session.setAttribute(attribute, v);
            return (v);
        }
    }

    /**
   *  Gets a property from the NMRSHiftDB.properties file
   *
   * @param  property                   The property to get
   * @param  servcon                    The current ServletConfig object
   * @return                            The nmrshiftdbProperty value
   * @exception  FileNotFoundException  Properties file not found
   * @exception  IOException            Could not read properties file
   */
    public static String getNmrshiftdbProperty(String property, ServletConfig servcon) throws FileNotFoundException, IOException {
        if (nmrprops == null) {
            String nmrpropsFile = ServletUtils.expandRelative(servcon, "/WEB-INF/conf/NMRShiftDB.properties");
            nmrprops = new Properties();
            nmrprops.load(new FileInputStream(nmrpropsFile));
        }
        return (nmrprops.getProperty(property));
    }

    public static void initProps(ServletConfig servcon) throws FileNotFoundException, IOException {
        if (nmrprops == null) {
            String nmrpropsFile = ServletUtils.expandRelative(servcon, "/WEB-INF/conf/NMRShiftDB.properties");
            nmrprops = new Properties();
            nmrprops.load(new FileInputStream(nmrpropsFile));
        }
        if (jetspeedprops == null) {
            String jetspeedpropsFile = ServletUtils.expandRelative(servcon, "/WEB-INF/conf/JetspeedResources.properties");
            jetspeedprops = new Properties();
            jetspeedprops.load(new FileInputStream(jetspeedpropsFile));
        }
    }

    /**
   *  Gets a property from the NMRSHiftDB.properties file (the nmrprops argument needs to be set before!).
   *
   * @param  property                   The property to get
   * @return                            The nmrshiftdbProperty value
   * @exception  FileNotFoundException  Properties file not found
   * @exception  IOException            Could not read properties file
   */
    public static String getNmrshiftdbProperty(String property) throws FileNotFoundException, IOException {
        if (nmrprops == null) {
            return (null);
        } else {
            return (nmrprops.getProperty(property));
        }
    }

    /**
   *  Gets a property from the NMRSHiftDB.properties file
   *
   * @param  property                   The property to get
   * @param  runData                    The current RunData object
   * @return                            The nmrshiftdbProperty value
   * @exception  FileNotFoundException  Properties file not found
   * @exception  IOException            Could not read properties file
   */
    public static String getNmrshiftdbProperty(String property, RunData runData) throws FileNotFoundException, IOException {
        if (runData == null) {
            return (getNmrshiftdbProperty(property));
        } else {
            return (getNmrshiftdbProperty(property, runData.getServletConfig()));
        }
    }

    /**
   *  Tells if a molecule is already in the DB
   *
   * @param  molWithHCount          The mol without the Hs
   * @param  molWithH               With Hs
   * @param  isDoubleBondSpecified  The vector indicating if double bond orientations are specified as needed for SmilesGenerator, null if none is specified
   * @param  runData                The runData object
   * @param  molecule               The mdl file
   * @param  ssb                    Description of Parameter
   * @return                        The existing molecule if there is one, null if none
   * @exception  IOException        Problems reading properties files
   * @exception  Exception          Database problems
   */
    public static DBMolecule getExistingMolecule(IMolecule molWithHCount, IMolecule molWithH, boolean[] isDoubleBondSpecified, RunData runData, String molecule, StringAndStringAndBoolean ssb) throws IOException, Exception {
        boolean isChiral = false;
        SmilesGenerator sg = new SmilesGenerator(DefaultChemObjectBuilder.getInstance());
        String smilesNonChiral = sg.createSMILES(molWithHCount);
        String smilesChiral = "";
        try {
            if (isDoubleBondSpecified != null) {
                smilesChiral = sg.createSMILES(molWithH, true, isDoubleBondSpecified);
            } else {
                smilesChiral = sg.createSMILES(molWithH, true, new boolean[molWithH.getBondCount()]);
            }
        } catch (Exception ex) {
            smilesChiral = smilesNonChiral;
            MailMessage email = new org.apache.turbine.util.mail.MailMessage(getSmtpServer(runData), "stefan.kuhn@uni-koeln.de", getAdminEmail(runData), "Chiral SMILES error", "Chiral SMILES failed for " + molecule);
            GeneralUtils.logError(ex, "smiles", null, true);
            if (email.send() == false) {
                Log.error("Sending email to failed for error creating smiles " + molecule);
            }
        }
        if (smilesChiral.indexOf("/") != -1 || smilesChiral.indexOf("\\") != -1 || smilesChiral.indexOf("@") != -1) {
            isChiral = true;
        }
        DBConnection dbconn = TurbineDB.getConnection();
        ResultSet rs = null;
        if (isChiral) {
            PreparedStatement checksmiles = dbconn.prepareStatement("select MOLECULE.MOLECULE_ID from MOLECULE where SMILES_STRING= ? and SMILES_STRING_CHIRAL = ?");
            checksmiles.setString(1, smilesNonChiral);
            checksmiles.setString(2, smilesChiral);
            rs = checksmiles.executeQuery();
        } else {
            String sql = "select MOLECULE.MOLECULE_ID from MOLECULE where SMILES_STRING= '" + smilesNonChiral + "' and SMILES_STRING_CHIRAL is null";
            rs = dbconn.createStatement().executeQuery(sql);
        }
        Vector v = new Vector();
        while (rs.next()) {
            v.add(DBMoleculePeer.retrieveByPK(new NumberKey(rs.getString(1))));
        }
        if (ssb != null) {
            ssb.string1 = smilesNonChiral;
            if (isChiral) {
                ssb.string2 = smilesChiral;
            }
            ssb.bool = isChiral;
        }
        if (v.size() > 0) {
            return ((DBMolecule) v.get(0));
        } else {
            return (null);
        }
    }

    /**
   *  Gets a standard email signature.
   *
   * @param  runData          Ther runData object.
   * @return                  The emailSignature value.
   * @exception  IOException  Problems reading properties.
   */
    public static String getEmailSignature(RunData runData) throws IOException {
        if (emailSignature == null) {
            if (runData == null) {
                return ("");
            }
            String url = javax.servlet.http.HttpUtils.getRequestURL(runData.getRequest()).toString();
            StringTokenizer st = new StringTokenizer(url, "/");
            st.nextToken();
            emailSignature = "\n\r-----------------------------\n\rThis is an email automatically generated by the nmrshiftdb system on " + st.nextToken() + ". It is sent to you because you used a service from nmrshiftdb. For any questions, including exclusion of further emails, mail the administrator (email: " + GeneralUtils.getAdminEmail(runData) + ").";
        }
        return (emailSignature);
    }

    /**
   *  Gets an object as an JSX XML String.
   *
   * @param  obj                      The object to serialize.
   * @return                          The xml string.
   * @exception  java.io.IOException  IO problems.
   */
    public static String getAsXMLString(Object obj) throws java.io.IOException {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ObjOut out = new ObjOut(printWriter);
        out.writeObject(obj);
        printWriter.flush();
        writer.flush();
        return writer.toString();
    }

    /**
   *  Gets an object from an JSX xml string.
   *
   * @param  xmlString                             The xml string.
   * @return                                       The object.
   * @exception  java.io.IOException               IO problem.
   * @exception  java.lang.ClassNotFoundException  The class to create must be found.
   */
    public static Object getFromXMLString(String xmlString) throws java.io.IOException, java.lang.ClassNotFoundException {
        StringReader reader = new StringReader(xmlString);
        ObjIn in = new ObjIn(reader);
        Object obj = in.readObject();
        return obj;
    }

    /**
   *  Returns a letter multiplicity from an int.
   *
   * @param  multiplicity  The mult to translate.
   * @param  totalname     true=long name, false=one letter name.
   * @return               The multiplicity value.
   */
    public static String getMultiplicity(int multiplicity, boolean totalname) {
        int protonCount = multiplicity;
        if (protonCount == 0) {
            if (totalname) {
                return ("Singlet");
            } else {
                return ("S");
            }
        } else if (protonCount == 1) {
            if (totalname) {
                return ("Duplet");
            } else {
                return ("D");
            }
        } else if (protonCount == 2) {
            if (totalname) {
                return ("Triplet");
            } else {
                return ("T");
            }
        } else if (protonCount == 3) {
            if (totalname) {
                return ("Quadruplet");
            } else {
                return ("Q");
            }
        } else {
            return ("");
        }
    }

    /**
   *  Tells how many digits after the comma (or .) a float has got.
   *
   * @param  value  The float to axamine.
   * @return        Number of digits.
   */
    public static int getDigitsAfterComma(float value) {
        int numberOfDigitsAfterComma = 0;
        String str = new String(value + "");
        if (str.length() < 1) {
            return (0);
        }
        int k = 0;
        for (int i = str.length() - 1; i > -1; i--) {
            if (str.substring(i, i + 1).equals(".")) {
                numberOfDigitsAfterComma = k;
                break;
            }
            k++;
        }
        return (numberOfDigitsAfterComma);
    }

    /**
   *  Gets the modelBuilder attribute of the GeneralUtils class
   *
   * @param  ff                Description of Parameter
   * @return                   The modelBuilder value
   * @exception  CDKException  Description of Exception
   */
    public static ModelBuilder3D getModelBuilder(String ff) throws CDKException {
        if (mb3d.get(ff) == null) {
            ModelBuilder3D mb3dlocal = new ModelBuilder3D();
            mb3dlocal.setTemplateHandler();
            mb3dlocal.setForceField(ff);
            mb3d.put(ff, mb3dlocal);
        }
        return (ModelBuilder3D) mb3d.get(ff);
    }

    /**
   *  Removes the last character from a string buffer if it is a , or ;.
   *
   * @param  s  The string buffer to deal with.
   * @return    Description of the Returned Value
   */
    public static StringBuffer removeLastComma(StringBuffer s) {
        if (s.length() > 0 && (s.charAt(s.length() - 1) == ' ')) {
            s.deleteCharAt(s.length() - 1);
        }
        if (s.length() > 0 && (s.charAt(s.length() - 1) == ';' || s.charAt(s.length() - 1) == ',')) {
            s.deleteCharAt(s.length() - 1);
        }
        return (s);
    }

    /**
   *Logs an error
   *
   * @param  ex       The thrown excpetion
   * @param  message  Some individual message
   * @param  data     The current RunData
   * @return          A text to display
   */
    public static String logError(Throwable ex, String message, RunData data, boolean critical) {
        Log.error("There was an error, described as " + message + ", the Exception message is: " + ex.getMessage());
        StringWriter strWr = new StringWriter();
        PrintWriter prWr = new PrintWriter(strWr);
        ex.printStackTrace(prWr);
        Log.error(strWr.toString());
        String x = "";
        if (data != null && data.getUser().hasLoggedIn()) {
            try {
                x = new String("<img src=\"images/bug.png\">An error occured. This error has been reported to the <a href=\"mailto:" + getAdminEmail(data) + "\">adminstrator</a>! He/she might contact you for details.");
            } catch (Exception e) {
                x = new String("<img src=\"images/bug.png\">An error occured. This error has been reported to the adminstrator! He/she might contact you for details.");
            }
        } else {
            try {
                x = new String("<img src=\"images/bug.png\">An error occured. Please report this error to the <a href=\"mailto:" + getAdminEmail(data) + "\">adminstrator</a>!");
            } catch (Exception e) {
                x = new String("<img src=\"images/bug.png\">An error occured. Please report this error to the adminstrator!");
            }
        }
        if (data != null) {
            try {
                sendEmailToEventReceivers("Exception occured in nmrshiftdb", "There was an exception " + ex.getMessage() + " thrown on " + data.getServerName() + " at " + new Date() + ". The logged in user was " + (data.getUser().hasLoggedIn() ? data.getUser().getUserName() : "nobody") + ".", data, critical ? 2 : 1);
            } catch (Exception e) {
                Log.error("sending email in logError failed");
            }
        }
        return (x);
    }

    /**
   *  Sends an email to all admineventreceiver role owners
   *
   * @param  header   The header of the email
   * @param  body     The body of the email
   * @param  runData  The runData object
   * @param  error    Is this an error or an info message (0=info, 1=non-critical error, 2=error)
   * @return          false=sending failed, true=worked
   */
    public static boolean sendEmailToEventReceivers(String header, String body, RunData runData, int error) {
        DBConnection dbconn = null;
        try {
            String sql = "select TURBINE_USER.EMAIL from TURBINE_USER, TURBINE_USER_GROUP_ROLE, TURBINE_ROLE where TURBINE_USER.USER_ID=TURBINE_USER_GROUP_ROLE.USER_ID and TURBINE_USER_GROUP_ROLE.ROLE_ID=TURBINE_ROLE.ROLE_ID AND TURBINE_ROLE.ROLE_NAME='admineventreceiver';";
            dbconn = TurbineDB.getConnection();
            Statement stmt = dbconn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                MailMessage email = new org.apache.turbine.util.mail.MailMessage(GeneralUtils.getSmtpServer(runData), rs.getString(1), GeneralUtils.getAdminEmail(runData), (error > 0 ? (error == 1 ? "[NMRSHIFTDB-ERROR-NON-CRITICAL]" : "[NMRSHIFTDB-ERROR]") : "[NMRSHIFTDB-INFO]") + " " + header, body + GeneralUtils.getEmailSignature(runData));
                if (email.send() == false) {
                    Log.error("Sending email to receiver " + rs.getString(1) + " failed.");
                }
            }
            return (true);
        } catch (Exception ex) {
            GeneralUtils.logError(ex, "in sendEmailToEventReceivers", runData, true);
            return (false);
        } finally {
            try {
                TurbineDB.releaseConnection(dbconn);
            } catch (Exception ex) {
                GeneralUtils.logError(ex, "SubmitPortlet/trying to close connection", runData, true);
            }
        }
    }

    /**
   *  Selects a reviewer
   *
   * @param  user           The user currently logged in
   * @return                The choosen reviewer
   * @exception  Exception  Description of Exception
   */
    public static NmrshiftdbUser selectReviewer(NmrshiftdbUser user) throws Exception {
        Vector v = user.getReviewGroups();
        Vector records = null;
        if (v.size() != 0) {
            String sql = "select TURBINE_USER.USER_ID, count(USER_SPECTRUM.USER_ID) AS REVIEWS from TURBINE_USER left join USER_SPECTRUM on TURBINE_USER.USER_ID = USER_SPECTRUM.USER_ID, TURBINE_USER_GROUP_ROLE, TURBINE_ROLE, REVIEWGROUP_USER where TURBINE_USER.USER_ID=REVIEWGROUP_USER.USER_ID and TURBINE_USER.USER_ID=TURBINE_USER_GROUP_ROLE.USER_ID and TURBINE_USER_GROUP_ROLE.ROLE_ID=TURBINE_ROLE.ROLE_ID AND TURBINE_ROLE.ROLE_NAME='assign_reviews' and REVIEWGROUP_USER.REVIEWGROUP_ID='" + ((DBReviewGroup) v.get(0)).getReviewgroupId() + "' and TURBINE_USER.LOGIN_NAME<>\"" + user.getUserName() + "\" group by USER_ID order by REVIEWS;";
            records = NmrshiftdbUserPeer.executeQuery(sql);
        } else {
            String sql = "select TURBINE_USER.USER_ID, count(USER_SPECTRUM.USER_ID) AS REVIEWS from TURBINE_USER left join USER_SPECTRUM on TURBINE_USER.USER_ID = USER_SPECTRUM.USER_ID, TURBINE_USER_GROUP_ROLE, TURBINE_ROLE left join REVIEWGROUP_USER on REVIEWGROUP_USER.USER_ID=TURBINE_USER.USER_ID where TURBINE_USER.USER_ID=TURBINE_USER_GROUP_ROLE.USER_ID and TURBINE_USER_GROUP_ROLE.ROLE_ID=TURBINE_ROLE.ROLE_ID AND TURBINE_ROLE.ROLE_NAME='assign_reviews' and TURBINE_USER.LOGIN_NAME<>\"" + user.getUserName() + "\" and REVIEWGROUP_USER.REVIEWGROUP_ID is null group by USER_ID order by REVIEWS;";
            records = NmrshiftdbUserPeer.executeQuery(sql);
            if (records.size() == 0) {
                sql = "select TURBINE_USER.USER_ID, count(USER_SPECTRUM.USER_ID) AS REVIEWS from TURBINE_USER left join USER_SPECTRUM on TURBINE_USER.USER_ID = USER_SPECTRUM.USER_ID, TURBINE_USER_GROUP_ROLE, TURBINE_ROLE where TURBINE_USER.USER_ID=TURBINE_USER_GROUP_ROLE.USER_ID and TURBINE_USER_GROUP_ROLE.ROLE_ID=TURBINE_ROLE.ROLE_ID AND TURBINE_ROLE.ROLE_NAME='assign_reviews' and TURBINE_USER.LOGIN_NAME<>\"" + user.getUserName() + "\" group by USER_ID order by REVIEWS;";
                records = NmrshiftdbUserPeer.executeQuery(sql);
            }
        }
        if (records.size() > 0) {
            return NmrshiftdbUserPeer.retrieveByPK(new NumberKey(((Record) records.get(new Random().nextInt(records.size()))).getValue(1).asString()));
        } else {
            return user;
        }
    }

    /**
   *  Adds an mdl file to the StructureHistory
   *
   * @param  mdl      The mdl to be added
   * @param  session  The current session
   */
    public static void addToStructureHistory(String mdl, HttpSession session) {
        Vector v = getVectorWithSessionBindingAttributeFromSession(session, NmrshiftdbConstants.STRUCTURESHISTORY);
        while (v.size() >= 10) {
            v.remove(0);
        }
        v.add(mdl);
    }

    /**
   *  Logs something to the sql log file
   *
   * @param  message        The message to log
   * @param  runData        The runData object
   * @exception  Exception  Description of Exception
   */
    public static void logToSql(String message, RunData runData) throws Exception {
        if (sqlLogger == null) {
            sqlLogger = Category.getInstance("sql");
            sqlLogger.addAppender(new FileAppender(new SimpleLayout(), GeneralUtils.getNmrshiftdbProperty("sql.logfile", runData)));
        }
        sqlLogger.debug(new Date() + " - " + (runData == null ? "" : runData.getUser().getUserName()) + " - " + message);
    }

    /**
   *  Replaces a string in a string with another string
   *
   * @param  str      The string in which replacing has to take place
   * @param  pattern  The pattern to replace
   * @param  replace  The string the pattern has to be replaced with
   * @return          The changed string
   */
    public static String replace(String str, String pattern, String replace) {
        int s = 0;
        int e = 0;
        StringBuffer result = new StringBuffer();
        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    /**
   *  Checks if the values of the C spectra of a certain DBMolecule are inside the CDK prediction range.
   *
   * @param  mol              The CDMolecule to check.
   * @return                  A text either "" or a description of problems.
   * @exception  IOException  Problems with serialisation.
   * @exception  Exception    Database problems.
   */
    public static String checkAgainstCdk(DBMolecule mol) throws IOException, Exception {
        try {
            HOSECodeGenerator hcg = new HOSECodeGenerator();
            BremserOneSphereHOSECodePredictor bremser = new BremserOneSphereHOSECodePredictor();
            StringBuffer returnValue = new StringBuffer();
            IMolecule cdkmolwithh = mol.getAsCDKMolecule(1);
            IMolecule cdkmolwithouth = (IMolecule) AtomContainerManipulator.removeHydrogens(cdkmolwithh);
            Iterator atoms = cdkmolwithh.atoms();
            DBAtom[] dbatoms = mol.getAtomsAsArrayInMdlOrder();
            int i = 0;
            while (atoms.hasNext()) {
                IAtom atom = (IAtom) atoms.next();
                if (atom.getSymbol().equals("C")) {
                    String hc = hcg.getHOSECode(cdkmolwithouth, atom, 1, true);
                    StringTokenizer st = new StringTokenizer(hc, ";");
                    st.nextToken();
                    String simpleHoseCode = st.nextToken();
                    double predict = bremser.predict(simpleHoseCode);
                    double lower = predict - bremser.getConfidenceLimit(simpleHoseCode);
                    double upper = predict + bremser.getConfidenceLimit(simpleHoseCode);
                    Criteria crit = new Criteria();
                    crit.addJoin(DBSpectrumPeer.SPECTRUM_TYPE_ID, DBSpectrumTypePeer.SPECTRUM_TYPE_ID);
                    crit.add(DBSpectrumTypePeer.NAME, "13C");
                    crit.addJoin(DBSpectrumPeer.SPECTRUM_ID, DBSignalPeer.SPECTRUM_ID);
                    crit.addJoin(DBSignalPeer.SIGNAL_ID, DBShiftPeer.SIGNAL_ID);
                    crit.addJoin(DBSignalPeer.SIGNAL_ID, DBSignalDBAtomPeer.SIGNAL_ID);
                    crit.add(DBSignalDBAtomPeer.ATOM_ID, dbatoms[i].getAtomId());
                    Vector v = DBShiftPeer.doSelect(crit);
                    for (int k = 0; k < v.size(); k++) {
                        if (((DBShift) v.get(k)).getValue() < lower || ((DBShift) v.get(k)).getValue() > upper) {
                            returnValue.append("Value from DB is " + ((DBShift) v.get(k)).getValue() + ", the cdk intervall is " + lower + " - " + upper + " for atom " + i + " of Molecule " + mol.getMoleculeId() + ".\n\r");
                        }
                    }
                    i++;
                }
            }
            return (returnValue.toString());
        } catch (CDKException ex) {
            return ("");
        }
    }

    /**
   *  Removes atom numbers from a specfile string.
   *
   * @param  specfile       The original specfile.
   * @param  dropIntensity  true=also drop intensities if they are zero.
   * @return                The new specfile.
   */
    public static String makeSimpleSpecfile(String specfile, boolean dropIntensity) {
        Vector numberSpectrum = ParseUtils.parseSpectrumFromSpecFile(specfile);
        StringBuffer display = new StringBuffer();
        for (int k = 0; k < numberSpectrum.size(); k++) {
            float value1 = ((ValueTriple) numberSpectrum.get(k)).value1;
            if ((int) value1 == value1) {
                display.append((int) value1);
            } else {
                display.append(value1);
            }
            if (((ValueTriple) numberSpectrum.get(k)).value2 != 0 || !dropIntensity) {
                display.append(";");
                float value2 = ((ValueTriple) numberSpectrum.get(k)).value2;
                if ((int) value2 == value2) {
                    display.append((int) value2);
                } else {
                    display.append(value2);
                }
            }
            if (!((ValueTriple) numberSpectrum.get(k)).multiplicityString.equals("")) {
                display.append(((ValueTriple) numberSpectrum.get(k)).multiplicityString);
            }
            display.append("|");
        }
        return (display.toString());
    }

    /**
   *  Prepares a string for sql quersis by escaping " and ' and \
   *
   * @param  text  The text to be escaped
   * @return       The escaped text
   */
    public static String escape(String text) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\'' || text.charAt(i) == '\"' || text.charAt(i) == '\\' || text.charAt(i) == '%' || text.charAt(i) == '\'') {
                buffer.append('\\');
            }
            buffer.append(text.charAt(i));
        }
        String newstr = GeneralUtils.replace(buffer.toString(), "β", "&#946;");
        newstr = GeneralUtils.replace(newstr, "α", "&#945;");
        return (newstr);
    }

    /**
   *  Description of the Method
   *
   * @param  text  Description of Parameter
   * @return       Description of the Returned Value
   */
    public static String escapeForHtml(String text) {
        String s = replace(text, "\"", "");
        return (s);
    }

    /**
   *  Executes searches from the searchHistory.
   *
   * @param  searches                 The searches to execute. Each figure in searches means the search in this position in sarchHistory will be executed, all searches are AND linked.
   * @param  searchHistory            The search history.
   * @param  chiralMessage            Return messages about chiral/non-chiral structure search.
   * @param  similarities             Holds similarites (as Doubles) in case similarity search was done (must be empty vector).
   * @param  calculatedonly           Search only calculated spectra.
   * @param  measuredonly             Search only measured spectra.
   * @param  runData                  Current runData, null if used in client.
   * @param  path                     Description of Parameter
   * @param  not                      Description of Parameter
   * @param  andor                    Description of Parameter
   * @param  otherspecified           Description of Parameter
   * @return                          Vector of molecule ids (as NumberKeys).
   * @exception  Exception            Database problems
   * @exception  NmrshiftdbException  Description of Exception
   */
    public static Vector executeSearch(Vector searchHistory, int[] searches, StringAndInt chiralMessage, Vector similarities, boolean calculatedonly, boolean measuredonly, RunData runData, String path, boolean not, boolean andor, boolean otherspecified, Vector spectrumTypes, boolean logtosql) throws NmrshiftdbException, Exception {
        if (spectrumTypes == null) spectrumTypes = DBSpectrumTypePeer.doSelect(new Criteria());
        String concatenator = " AND ";
        if (!andor) {
            concatenator = " OR ";
        }
        HashMap fields = new HashMap();
        fields.put(NmrshiftdbConstants.LITERATURE_AUTHOR, "AUTHOR.NAME_TOTAL");
        fields.put(NmrshiftdbConstants.LITERATURE_TITLE, "LITERATURE.TITLE_TOTAL");
        fields.put(NmrshiftdbConstants.COMMENT, "SPECTRUM.COMMENT");
        fields.put(NmrshiftdbConstants.CANNAME, "CANONICAL_NAME.NAME");
        fields.put(NmrshiftdbConstants.SPECLINK, "SPECTRUM_HYPERLINK.DESCRIPTION");
        fields.put(NmrshiftdbConstants.MOLLINK, "MOLECULE_HYPERLINK.DESCRIPTION");
        fields.put(NmrshiftdbConstants.CASNUMBER, "MOLECULE.CAS_NUMBER");
        fields.put(NmrshiftdbConstants.CHEMNAME, "CHEMICAL_NAME.NAME");
        fields.put(NmrshiftdbConstants.MOLKEY, "MK.KEYWORD");
        fields.put(NmrshiftdbConstants.SPECKEY, "SK.KEYWORD");
        fields.put(NmrshiftdbConstants.SPECTRUM_NR, "SPECTRUM.NMRSHIFTDB_NUMBER");
        fields.put(NmrshiftdbConstants.MOLECULE_NR, "MOLECULE.NMRSHIFTDB_NUMBER");
        fields.put(NmrshiftdbConstants.HOSECODE, "ATOM.HOSE_CODE");
        fields.put(NmrshiftdbConstants.DBE, "MOLECULE.DBE");
        fields.put(NmrshiftdbConstants.SSSR, "MOLECULE.SSSR");
        Vector v = new Vector();
        boolean isAChiralSearch = false;
        boolean chiral = false;
        for (int i = 0; i < searches.length; i++) {
            if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.TOTALSTRUCTURE)) {
                MDLReader mdlreader = new MDLReader(new StringReader(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange()));
                IMolecule molWithH = (IMolecule) mdlreader.read(new org.openscience.cdk.Molecule());
                for (int k = 0; k < molWithH.getAtomCount(); k++) {
                    if (molWithH.getAtom(k).getPoint2d() == null) molWithH.getAtom(k).setPoint2d(new Point2d(molWithH.getAtom(k).getPoint3d().x, molWithH.getAtom(k).getPoint3d().y));
                }
                AtomUtils.addAndPlaceHydrogens(molWithH);
                HueckelAromaticityDetector.detectAromaticity(molWithH, false);
                String smilesChiral = new SmilesGenerator(DefaultChemObjectBuilder.getInstance()).createSMILES(molWithH, true, new boolean[molWithH.getBondCount()]);
                String smiles = new SmilesGenerator(DefaultChemObjectBuilder.getInstance()).createSMILES(molWithH);
                if (!smiles.equals(smilesChiral)) {
                    isAChiralSearch = true;
                    chiral = true;
                }
            }
        }
        while (true) {
            StringBuffer query = new StringBuffer();
            if (searches.length == 1 && (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSTRUCTURE_SIMILARITY) || ((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSTRUCTURE_EXACT)) && runData != null) {
                MDLReader mdlreader = new MDLReader(new StringReader(((ValuesForVelocityBean) searchHistory.get(searches[0])).getRange()));
                IMolecule molWithH = (IMolecule) mdlreader.read(new org.openscience.cdk.Molecule());
                AtomUtils.normalize(molWithH, path);
                HueckelAromaticityDetector.detectAromaticity(molWithH, false);
                molWithH = (IMolecule) AtomContainerManipulator.removeHydrogens(molWithH);
                for (int m = 0; m < molWithH.getAtomCount(); m++) {
                    molWithH.getAtom(m).setHydrogenCount(0);
                }
                AllRingsFinder arf = new AllRingsFinder();
                arf.setTimeout(50000);
                BitSet bs = new Fingerprinter().getExtendedFingerprint(molWithH, arf.findAllRings(molWithH));
                StringBuffer fingerprintstring = new StringBuffer();
                fingerprintstring.append("(");
                for (int i = 0; i < 16; i++) {
                    fingerprintstring.append("fp" + i + ",");
                }
                for (int i = 0; i < 15; i++) {
                    fingerprintstring.append(AtomUtils.getBigIntegerValue(bs, i) + ",");
                }
                fingerprintstring.append(AtomUtils.getBigIntegerValue(bs, 15) + ")");
                HueckelAromaticityDetector.detectAromaticity(molWithH, false);
                query.append("SELECT DISTINCT FINGERPRINTS.MOLECULE_ID " + (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSTRUCTURE_SIMILARITY) ? ", tanimoto_coefficient" + fingerprintstring + " as tani" : "") + " FROM FINGERPRINTS join SPECTRUM using (MOLECULE_ID) WHERE (");
                for (int i = 0; i < spectrumTypes.size(); i++) {
                    if (((DBSpectrumType) spectrumTypes.get(i)).getChoosen()) query.append("SPECTRUM.SPECTRUM_TYPE_ID=" + ((DBSpectrumType) spectrumTypes.get(i)).getSpectrumTypeId() + " OR ");
                }
                query.append("false) and fingerprint_compare");
                query.append(fingerprintstring + " = 'Y'");
                if (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSTRUCTURE_SIMILARITY)) {
                    query.append(" order by tani desc");
                }
                query.append(";");
            } else if (searches.length == 1 && (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM) || ((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.TOTALSPECTRUM)) && runData != null) {
                long fingerprint = 0;
                if (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM)) {
                    Vector numberSpectrum = ParseUtils.parseSpectrumFromSpecFile(((ValuesForVelocityBean) searchHistory.get(searches[0])).getRange());
                    DBSignal[] searchSpectrum = new DBSignal[numberSpectrum.size()];
                    for (int k = 0; k < numberSpectrum.size(); k++) {
                        DBSignal sig = new DBSignal();
                        DBShift[] shifts = new DBShift[1];
                        DBShift shift = new DBShift();
                        shift.setAxis("" + 1);
                        shift.setValue(((ValueTriple) numberSpectrum.get(k)).value1);
                        shifts[0] = shift;
                        sig.setDBShiftsArray(shifts);
                        sig.addDBShift(shift);
                        sig.setIntensity(((ValueTriple) numberSpectrum.get(k)).value2);
                        searchSpectrum[k] = sig;
                    }
                    fingerprint = SpectrumUtils.makeFingerprint(searchSpectrum, false);
                }
                query.append("SELECT DISTINCT FINGERPRINTS.MOLECULE_ID, spectrumsimilarity(SPECTRUM_FINGERPRINTS.SIMPLE_SPECFILE,\"" + ((ValuesForVelocityBean) searchHistory.get(searches[0])).getRange() + "\",\"");
                if (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM)) {
                    query.append("subspectrum");
                } else {
                    query.append("total");
                }
                query.append("\") FROM SPECTRUM_FINGERPRINTS, FINGERPRINTS WHERE ");
                if (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM)) {
                    query.append("fingerprint_compare64(SPECTRUM_FINGERPRINTS.FINGERPRINT, " + fingerprint + ") = \"Y\" AND ");
                }
                query.append("SPECTRUM_FINGERPRINTS.MOLECULE_ID = FINGERPRINTS.MOLECULE_ID AND (");
                for (int i = 0; i < spectrumTypes.size(); i++) {
                    if (((DBSpectrumType) spectrumTypes.get(i)).getChoosen()) query.append("SPECTRUM_FINGERPRINTS.NAME='" + ((DBSpectrumType) spectrumTypes.get(i)).getName() + "' OR ");
                }
                query.append("false) and spectrumsimilarity(SPECTRUM_FINGERPRINTS.SIMPLE_SPECFILE, \"" + ((ValuesForVelocityBean) searchHistory.get(searches[0])).getRange() + "\", \"");
                if (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM)) {
                    query.append("subspectrum");
                } else {
                    query.append("total");
                }
                query.append("\")  order by spectrumsimilarity(SPECTRUM_FINGERPRINTS.SIMPLE_SPECFILE,\"" + ((ValuesForVelocityBean) searchHistory.get(searches[0])).getRange() + "\", \"");
                if (((ValuesForVelocityBean) searchHistory.get(searches[0])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM)) {
                    query.append("subspectrum");
                } else {
                    query.append("total");
                }
                query.append("\") desc;");
            } else {
                query.append("SELECT DISTINCT MOLECULE.MOLECULE_ID");
                for (int i = 0; i < searches.length; i++) {
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM) && runData != null) {
                        query.append(", spectrumsimilarity(SPECTRUM.SPECFILE,'" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange() + "', 'subspectrum')");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.TOTALSPECTRUM) && runData != null) {
                        query.append(", spectrumsimilarity(SPECTRUM.SPECFILE,'" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange() + "', 'total')");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getNameForElements().equals(NmrshiftdbConstants.FUZZY) & !(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.FORMULA) || ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MULTIPLICITY) || ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.POTMULTIPLICITY))) {
                        query.append(", " + fields.get(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText()));
                    }
                }
                query.append(" FROM SPECTRUM, MOLECULE ");
                Vector simplehistory = new Vector();
                for (int i = 0; i < searches.length; i++) {
                    if (!simplehistory.contains(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText())) {
                        simplehistory.add(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText());
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.CONDITIONS)) {
                        query.append(", SPECTRUM_CONDITION as SPECTRUM_CONDITION_" + i + ", CONDITION as CONDITION_" + i + ", CONDITION_TYPE as CONDITION_TYPE_" + i + " ");
                    }
                }
                for (int i = 0; i < simplehistory.size(); i++) {
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.MULTIPLICITY)) {
                        Map map = AtomUtils.analyzeFuzzyCriteria(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange(), false, true);
                        Iterator it = map.keySet().iterator();
                        while (it.hasNext()) {
                            String key = (String) it.next();
                            if (!((String[]) map.get(key))[0].equals("0") && !((String[]) map.get(key))[1].equals("0")) {
                                query.append(" LEFT JOIN PC AS " + key + "_PC ON");
                                query.append(" SPECTRUM.SPECTRUM_ID = " + key + "_PC.SPECTRUM_ID ");
                            }
                        }
                        query.append(" LEFT JOIN d2 ON SPECTRUM.SPECTRUM_ID = d2.SPECTRUM_ID ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.MYSPECTRA)) {
                        query.append(" left join TURBINE_USER on TURBINE_USER.USER_ID = SPECTRUM.USER_ID ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.LITERATURE_TITLE) || ((String) simplehistory.get(i)).equals(NmrshiftdbConstants.LITERATURE_AUTHOR)) {
                        query.append(" left join SPECTRUM_LITERATURE on SPECTRUM.SPECTRUM_ID = SPECTRUM_LITERATURE.SPECTRUM_ID left join LITERATURE on SPECTRUM_LITERATURE.LITERATURE_ID=LITERATURE.LITERATURE_ID ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.LITERATURE_AUTHOR)) {
                        query.append(" left join IS_AUTHOR on LITERATURE.LITERATURE_ID=IS_AUTHOR.LITERATURE_ID left join AUTHOR on IS_AUTHOR.AUTHOR_ID=AUTHOR.AUTHOR_ID ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.POTMULTIPLICITY)) {
                        Map map = AtomUtils.analyzeFuzzyCriteria(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange(), false, true);
                        Iterator it = map.keySet().iterator();
                        while (it.hasNext()) {
                            String key = (String) it.next();
                            int count = -1;
                            if (key.equals("S") || key.equals("s")) {
                                count = 0;
                            }
                            if (key.equals("D") || key.equals("d")) {
                                count = 1;
                            }
                            if (key.equals("T") || key.equals("t")) {
                                count = 2;
                            }
                            if (key.equals("Q") || key.equals("q")) {
                                count = 3;
                            }
                            if (count == -1) throw new NmrshiftdbException("Multiplicity search only possible with S/D/T/Q symbols");
                            query.append(" LEFT JOIN HCOUNT AS " + count + "_AC ON");
                            query.append(" (MOLECULE.MOLECULE_ID = " + count + "_AC.MOLECULE_ID and " + count + "_AC.H_COUNT=" + count + ") ");
                        }
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.FORMULA)) {
                        Map map = AtomUtils.analyzeFuzzyCriteria(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange(), true, false);
                        Iterator it = map.keySet().iterator();
                        while (it.hasNext()) {
                            String key = (String) it.next();
                            query.append(" LEFT JOIN (select MOLECULE_ID, SYMBOL, count(*) AS COUNT from ATOM group by MOLECULE_ID, SYMBOL) AS " + key + "_AC ON");
                            query.append(" MOLECULE.MOLECULE_ID = " + key + "_AC.MOLECULE_ID ");
                        }
                        query.append(" LEFT JOIN (select MOLECULE_ID , count(Distinct SYMBOL) as c from ATOM group by MOLECULE_ID) as d3 ON MOLECULE.MOLECULE_ID = d3.MOLECULE_ID ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.LITERATURE_TITLE) || ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.HOSECODE)) {
                        query.append(" join ATOM using (MOLECULE_ID) ");
                    }
                    if (((String) simplehistory.get(i)).indexOf(NmrshiftdbConstants.MOLECULEKEYWORDS_TOTAL) != -1) {
                        query.append(", MOLECULE_KEYWORD,KEYWORD AS KWM ");
                    }
                    if (((String) simplehistory.get(i)).indexOf(NmrshiftdbConstants.SPECTRUMKEYWORDS_TOTAL) != -1) {
                        query.append(", SPECTRUM_KEYWORD, KEYWORD AS KWS ");
                    }
                    if (calculatedonly || measuredonly) {
                        query.append(", SPECTRUM_CONDITION, CONDITION, CONDITION_TYPE ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.CHEMNAME)) {
                        query.append(", CHEMICAL_NAME ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.CANNAME)) {
                        query.append(", CANONICAL_NAME ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.MOLKEY)) {
                        query.append(", KEYWORD AS MK, MOLECULE_KEYWORD ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.SPECKEY)) {
                        query.append(", KEYWORD AS SK, SPECTRUM_KEYWORD ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.MOLLINK)) {
                        query.append(", MOLECULE_HYPERLINK ");
                    }
                    if (((String) simplehistory.get(i)).equals(NmrshiftdbConstants.SPECLINK)) {
                        query.append(", SPECTRUM_HYPERLINK ");
                    }
                }
                query.append("WHERE SPECTRUM.MOLECULE_ID = MOLECULE.MOLECULE_ID and (");
                for (int i = 0; i < spectrumTypes.size(); i++) {
                    if (((DBSpectrumType) spectrumTypes.get(i)).getChoosen()) query.append("SPECTRUM.SPECTRUM_TYPE_ID='" + ((DBSpectrumType) spectrumTypes.get(i)).getSpectrumTypeId() + "' OR ");
                }
                query.append("false) and ");
                boolean hasmy = false;
                for (int i = 0; i < searches.length; i++) {
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MOLKEY)) {
                        query.append(" MK.KEYWORD_ID=MOLECULE_KEYWORD.KEYWORD_ID AND MOLECULE.MOLECULE_ID=MOLECULE_KEYWORD.MOLECULE_ID AND ");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SPECKEY)) {
                        query.append(" SK.KEYWORD_ID=SPECTRUM_KEYWORD.KEYWORD_ID AND SPECTRUM.SPECTRUM_ID=SPECTRUM_KEYWORD.SPECTRUM_ID AND ");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MOLLINK)) {
                        query.append(" MOLECULE_HYPERLINK.MOLECULE_ID=MOLECULE.MOLECULE_ID AND ");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SPECLINK)) {
                        query.append(" SPECTRUM_HYPERLINK.SPECTRUM_ID=SPECTRUM.SPECTRUM_ID AND ");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.CHEMNAME)) {
                        query.append(" CHEMICAL_NAME.MOLECULE_ID=MOLECULE.MOLECULE_ID AND ");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.CANNAME)) {
                        query.append(" CANONICAL_NAME.MOLECULE_ID=MOLECULE.MOLECULE_ID AND ");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MYSPECTRA)) {
                        query.append("(TURBINE_USER.LOGIN_NAME='" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange() + "' AND (SPECTRUM.REVIEW_FLAG = '" + NmrshiftdbConstants.FALSE + "' or SPECTRUM.REVIEW_FLAG = '" + NmrshiftdbConstants.REJECTED + "' or SPECTRUM.REVIEW_FLAG = '" + NmrshiftdbConstants.TRUE + "')) and ");
                        hasmy = true;
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.CONDITIONS)) {
                        query.append(" CONDITION_TYPE_" + i + ".CONDITION_NAME='" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getNameForElements() + "' and ");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.CONDITIONS)) {
                        query.append(" SPECTRUM.SPECTRUM_ID = SPECTRUM_CONDITION_" + i + ".SPECTRUM_ID and SPECTRUM_CONDITION_" + i + ".CONDITION_ID = CONDITION_" + i + ".CONDITION_ID and CONDITION_" + i + ".CONDITION_TYPE_ID= CONDITION_TYPE_" + i + ".CONDITION_TYPE_ID and ");
                    }
                }
                if (calculatedonly || measuredonly) {
                    query.append(" SPECTRUM.SPECTRUM_ID = SPECTRUM_CONDITION.SPECTRUM_ID and SPECTRUM_CONDITION.CONDITION_ID = CONDITION.CONDITION_ID and CONDITION.CONDITION_TYPE_ID= CONDITION_TYPE.CONDITION_TYPE_ID and ");
                }
                if (measuredonly || calculatedonly) {
                    query.append(" (");
                    if (measuredonly) {
                        query.append(" CONDITION_TYPE. CONDITION_TYPE='m'");
                    }
                    if (calculatedonly && measuredonly) {
                        query.append(" or");
                    }
                    if (calculatedonly) {
                        query.append(" CONDITION_TYPE. CONDITION_TYPE='c'");
                    }
                    query.append(" ) and ");
                }
                if (!hasmy) {
                    query.append(" SPECTRUM.REVIEW_FLAG = '" + NmrshiftdbConstants.TRUE + "' and ");
                }
                if (not) {
                    query.append("not");
                }
                query.append("(");
                for (int i = 0; i < searches.length; i++) {
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.FORMULA)) {
                        Map map = AtomUtils.analyzeFuzzyCriteria(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange(), true, false);
                        Iterator it = map.keySet().iterator();
                        int k = 0;
                        while (it.hasNext()) {
                            String key = (String) it.next();
                            if (k > 0) {
                                query.append(" AND ");
                            }
                            query.append(key + "_AC.SYMBOL = '" + key + "'");
                            query.append(" AND " + key + "_AC.COUNT >= ");
                            query.append(((String[]) map.get(key))[0]);
                            query.append(" AND " + key + "_AC.COUNT <= ");
                            query.append(((String[]) map.get(key))[1]);
                            k++;
                        }
                        if (otherspecified) {
                            query.append(" AND d3.c = " + map.size());
                        }
                        query.append(concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.POTMULTIPLICITY)) {
                        Map map = AtomUtils.analyzeFuzzyCriteria(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange(), false, true);
                        Iterator it = map.keySet().iterator();
                        int k = 0;
                        while (it.hasNext()) {
                            String key = (String) it.next();
                            int count = 3;
                            if (key.equals("S")) {
                                count = 0;
                            }
                            if (key.equals("D")) {
                                count = 1;
                            }
                            if (key.equals("T")) {
                                count = 2;
                            }
                            if (k > 0) {
                                query.append(" AND ");
                            }
                            if (((String[]) map.get(key))[0].equals("0") && ((String[]) map.get(key))[1].equals("0")) {
                                query.append("(");
                            }
                            query.append("(" + count + "_AC.COUNT >= ");
                            query.append(((String[]) map.get(key))[0]);
                            query.append(" AND " + count + "_AC.COUNT <= ");
                            query.append(((String[]) map.get(key))[1] + ")");
                            if (((String[]) map.get(key))[0].equals("0") && ((String[]) map.get(key))[1].equals("0")) {
                                query.append(" or " + count + "_AC.COUNT is null )");
                            }
                            k++;
                        }
                        query.append(" " + concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MULTIPLICITY)) {
                        Map map = AtomUtils.analyzeFuzzyCriteria(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange(), false, true);
                        Iterator it = map.keySet().iterator();
                        int k = 0;
                        while (it.hasNext()) {
                            String key = (String) it.next();
                            if (!((String[]) map.get(key))[0].equals("0") && !((String[]) map.get(key))[1].equals("0")) {
                                if (k > 0) {
                                    query.append(" AND ");
                                }
                                query.append(key + "_PC.MULTIPLICITY = '" + key + "'");
                                query.append(" AND " + key + "_PC.COUNT >= ");
                                query.append(((String[]) map.get(key))[0]);
                                query.append(" AND " + key + "_PC.COUNT <= ");
                                query.append(((String[]) map.get(key))[1]);
                                k++;
                            }
                        }
                        query.append(" AND d2.c=" + k + "" + concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM) && runData != null) {
                        Vector numberSpectrum = ParseUtils.parseSpectrumFromSpecFile(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange());
                        DBSignal[] searchSpectrum = new DBSignal[numberSpectrum.size()];
                        for (int k = 0; k < numberSpectrum.size(); k++) {
                            DBSignal sig = new DBSignal();
                            DBShift[] shifts = new DBShift[1];
                            DBShift shift = new DBShift();
                            shift.setAxis("" + 1);
                            shift.setValue(((ValueTriple) numberSpectrum.get(k)).value1);
                            shifts[0] = shift;
                            sig.setDBShiftsArray(shifts);
                            sig.addDBShift(shift);
                            sig.setIntensity(((ValueTriple) numberSpectrum.get(k)).value2);
                            searchSpectrum[k] = sig;
                        }
                        long fingerprint = SpectrumUtils.makeFingerprint(searchSpectrum, false);
                        query.append(" fingerprint_compare64(SPECTRUM.FINGERPRINT, " + fingerprint + ") = 'Y' " + concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MOLECULEKEYWORDS_FRAGMENT)) {
                        query.append(" MOLECULE.MOLECULE_ID=MOLECULE_KEYWORD.MOLECULE_ID AND MOLECULE_KEYWORD.KEYWORD_ID=KWM.KEYWORD_ID AND ");
                        String keywords = ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange();
                        StringTokenizer tokenizer = new StringTokenizer(keywords, " |");
                        if (tokenizer.hasMoreTokens()) {
                            query.append("( ");
                        }
                        while (tokenizer.hasMoreTokens()) {
                            String name = tokenizer.nextToken();
                            query.append("KWM.KEYWORD like '%" + name + "%' ");
                            if (tokenizer.hasMoreTokens()) {
                                query.append("OR ");
                            } else {
                                query.append(" ) ");
                            }
                        }
                        query.append(concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MOLECULEKEYWORDS_TOTAL)) {
                        query.append(" MOLECULE.MOLECULE_ID=MOLECULE_KEYWORD.MOLECULE_ID AND MOLECULE_KEYWORD.KEYWORD_ID=KWM.KEYWORD_ID AND ");
                        String keywords = ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange();
                        StringTokenizer tokenizer = new StringTokenizer(keywords, "|");
                        if (tokenizer.hasMoreTokens()) {
                            query.append("( ");
                        }
                        while (tokenizer.hasMoreTokens()) {
                            query.append("KWM.KEYWORD='" + tokenizer.nextToken() + "' ");
                            if (tokenizer.hasMoreTokens()) {
                                query.append("OR ");
                            } else {
                                query.append(" ) ");
                            }
                        }
                        query.append(concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SPECTRUMKEYWORDS_FRAGMENT)) {
                        query.append(" SPECTRUM.SPECTRUM_ID=SPECTRUM_KEYWORD.SPECTRUM_ID AND SPECTRUM_KEYWORD.KEYWORD_ID=KWS.KEYWORD_ID AND ");
                        String keywords = ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange();
                        StringTokenizer tokenizer = new StringTokenizer(keywords, " |");
                        if (tokenizer.hasMoreTokens()) {
                            query.append("( ");
                        }
                        while (tokenizer.hasMoreTokens()) {
                            String name = tokenizer.nextToken();
                            query.append("KWS.KEYWORD like '%" + name + "%' ");
                            if (tokenizer.hasMoreTokens()) {
                                query.append("OR ");
                            } else {
                                query.append(" ) ");
                            }
                        }
                        query.append(concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SPECTRUMKEYWORDS_TOTAL)) {
                        query.append(" SPECTRUM.SPECTRUM_ID=SPECTRUM_KEYWORD.SPECTRUM_ID AND SPECTRUM_KEYWORD.KEYWORD_ID=KWS.KEYWORD_ID AND ");
                        String keywords = ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange();
                        StringTokenizer tokenizer = new StringTokenizer(keywords, "|");
                        if (tokenizer.hasMoreTokens()) {
                            query.append("( ");
                        }
                        while (tokenizer.hasMoreTokens()) {
                            query.append("KWS.KEYWORD = '" + tokenizer.nextToken() + "' ");
                            if (tokenizer.hasMoreTokens()) {
                                query.append("OR ");
                            } else {
                                query.append(" ) ");
                            }
                        }
                        query.append(concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.CONDITIONS)) {
                        query.append(" CONDITION_" + i + ".VALUE='" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange() + "'" + concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.WEIGHT)) {
                        String weights = ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange();
                        StringTokenizer tokenizer = new StringTokenizer(weights, "-");
                        query.append(" MOLECULE.MOLECULAR_WEIGHT >= " + tokenizer.nextToken() + " and MOLECULE.MOLECULAR_WEIGHT <= " + tokenizer.nextToken() + concatenator);
                    }
                    if ((((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSTRUCTURE_SIMILARITY) || ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSTRUCTURE_EXACT)) && runData != null) {
                        MDLReader mdlreader = new MDLReader(new StringReader(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange()));
                        IMolecule molWithH = (IMolecule) mdlreader.read(new org.openscience.cdk.Molecule());
                        AtomUtils.normalize(molWithH, path);
                        HueckelAromaticityDetector.detectAromaticity(molWithH, false);
                        IMolecule molgo = (IMolecule) AtomContainerManipulator.removeHydrogens(molWithH);
                        for (int m = 0; m < molgo.getAtomCount(); m++) {
                            molgo.getAtom(m).setHydrogenCount(0);
                        }
                        AllRingsFinder arf = new AllRingsFinder();
                        arf.setTimeout(50000);
                        BitSet bs = new Fingerprinter().getExtendedFingerprint(molgo, arf.findAllRings(molgo));
                        query.append(" fingerprint_compare(");
                        for (int k = 0; k < 16; k++) {
                            query.append("fp" + k + ",");
                        }
                        for (int k = 0; k < 15; k++) {
                            query.append(AtomUtils.getBigIntegerValue(bs, k) + ",");
                        }
                        query.append(AtomUtils.getBigIntegerValue(bs, 15) + ") = 'Y' " + concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.TOTALSTRUCTURE)) {
                        MDLReader mdlreader = new MDLReader(new StringReader(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange()));
                        IMolecule molWithH = (IMolecule) mdlreader.read(new org.openscience.cdk.Molecule());
                        AtomUtils.normalize(molWithH, path);
                        IRingSet sssr = new SSSRFinder(molWithH).findSSSR();
                        for (int k = 0; k < sssr.getAtomContainerCount(); k++) {
                            DeAromatizationTool.deAromatize((IRing) sssr.getAtomContainer(k));
                        }
                        for (int k = 0; k < molWithH.getAtomCount(); k++) {
                            if (molWithH.getAtom(k).getPoint2d() == null) molWithH.getAtom(k).setPoint2d(new Point2d(molWithH.getAtom(k).getPoint3d().x, molWithH.getAtom(k).getPoint3d().y));
                        }
                        AtomUtils.addAndPlaceHydrogens(molWithH);
                        HueckelAromaticityDetector.detectAromaticity(molWithH, false);
                        IMolecule molWithoutH = (IMolecule) AtomContainerManipulator.removeHydrogens(molWithH);
                        query.append(" MOLECULE.SMILES_STRING" + (chiral ? "_CHIRAL" : "") + " = '" + (chiral ? new SmilesGenerator(DefaultChemObjectBuilder.getInstance()).createSMILES(molWithH, true, new boolean[molWithH.getAtomCount()]) : new SmilesGenerator(DefaultChemObjectBuilder.getInstance()).createSMILES(molWithoutH)) + "' " + concatenator);
                    }
                    if (!((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.WEIGHT) && !((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.FORMULA) && !((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MULTIPLICITY) && !((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.HOSECODE) && !((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.POTMULTIPLICITY)) {
                        if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getNameForElements().equals(NmrshiftdbConstants.EXACT)) {
                            query.append(fields.get(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText()) + "='" + GeneralUtils.escape(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange()) + "' " + concatenator);
                        }
                        if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getNameForElements().equals(NmrshiftdbConstants.FUZZY)) {
                            String searchSoundex = Soundex.soundex(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange(), "9", new StringAndInt());
                            query.append(fields.get(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText()) + "_SOUNDEX regexp '" + searchSoundex + "' " + concatenator);
                        }
                        if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getNameForElements().equals(NmrshiftdbConstants.REGEXP)) {
                            query.append(fields.get(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText()) + " regexp '" + GeneralUtils.escape(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange()) + "' " + concatenator);
                        }
                        if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getNameForElements().equals(NmrshiftdbConstants.FRAGMENT)) {
                            query.append(fields.get(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText()) + " like '%" + GeneralUtils.escape(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange()) + "%' " + concatenator);
                        }
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.HOSECODE)) {
                        query.append("( " + fields.get(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText()) + " like '" + GeneralUtils.escape(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange()) + "%' OR " + fields.get(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText()) + "_WITH_RINGS like '" + GeneralUtils.escape(((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange()) + "%' )" + concatenator);
                        try {
                            int id = Integer.parseInt(((ValuesForVelocityBean) searchHistory.get(searches[i])).getNameForElements());
                            query.append("SPECTRUM.SPECTRUM_TYPE_ID=" + id + "" + concatenator);
                        } catch (NumberFormatException ex) {
                        }
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM) && runData != null) {
                        query.append("spectrumsimilarity(SPECTRUM.SPECFILE,'" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange() + "', 'subspectrum') " + concatenator);
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.TOTALSPECTRUM) && runData != null) {
                        query.append("spectrumsimilarity(SPECTRUM.SPECFILE,'" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange() + "', 'total') " + concatenator);
                    }
                }
                if (!hasmy) {
                    query.setLength(query.length() - 4);
                }
                if (hasmy) {
                    query.append("true");
                }
                query.append(")");
                for (int i = 0; i < searches.length; i++) {
                    if ((((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM) | ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.TOTALSPECTRUM)) && runData != null) {
                        query.append(" order by ");
                        break;
                    }
                }
                for (int i = 0; i < searches.length; i++) {
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM) && runData != null) {
                        query.append("spectrumsimilarity(SPECTRUM.SPECFILE,'" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange() + "', 'subspectrum') desc, ");
                    }
                    if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.TOTALSPECTRUM) && runData != null) {
                        query.append("spectrumsimilarity(SPECTRUM.SPECFILE,'" + ((ValuesForVelocityBean) searchHistory.get(searches[i])).getRange() + "', 'total') desc, ");
                    }
                }
                for (int i = 0; i < searches.length; i++) {
                    if ((((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM) | ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.TOTALSPECTRUM)) && runData != null) {
                        query.append("1 desc");
                        break;
                    }
                }
                query.append(";");
            }
            Log.info(query.toString());
            if (runData != null && logtosql) {
                GeneralUtils.logToSql("Search - " + query, runData);
            }
            DBConnection conn = TurbineDB.getConnection();
            PreparedStatement checksmiles = conn.prepareStatement(query.toString());
            ResultSet rs = checksmiles.executeQuery();
            List l = new ArrayList();
            List l2 = new ArrayList();
            boolean alreadylist = false;
            for (int k = 0; k < searches.length; k++) {
                if (((ValuesForVelocityBean) searchHistory.get(searches[k])).getDisplayText().equals(NmrshiftdbConstants.SUBSTRUCTURE_EXACT)) {
                    alreadylist = true;
                    MDLReader mdlreader = new MDLReader(new StringReader(((ValuesForVelocityBean) searchHistory.get(searches[k])).getRange()));
                    IMolecule molWithH = (IMolecule) mdlreader.read(new org.openscience.cdk.Molecule());
                    AtomUtils.normalize(molWithH, path);
                    boolean queryHasCharge = false;
                    for (int m = 0; m < molWithH.getAtomCount(); m++) {
                        if (molWithH.getAtom(m).getFormalCharge() != 0) {
                            queryHasCharge = true;
                        }
                    }
                    HueckelAromaticityDetector.detectAromaticity(molWithH, false);
                    IAtomContainer mol = (IMolecule) AtomContainerManipulator.removeHydrogens(molWithH);
                    if (queryHasCharge) {
                        mol = QueryAtomContainerCreator.createSymbolAndChargeQueryContainer(mol);
                    }
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        IAtomContainer thisMol = DBMolecule.getAsCDKMolecule(new NumberKey(rs.getString(1)));
                        if (queryHasCharge) {
                            thisMol = QueryAtomContainerCreator.createSymbolAndChargeQueryContainer(thisMol);
                        }
                        if (UniversalIsomorphismTester.isSubgraph(thisMol, mol)) {
                            l.add(new NumberKey(rs.getString(1)));
                            if (rs.getMetaData().getColumnCount() > 1) {
                                l2.add(rs.getString(2));
                            }
                        }
                        if (l.size() > MAX_HITS_TO_DISPLAY) {
                            break;
                        }
                    }
                }
            }
            if (!alreadylist) {
                while (rs.next()) {
                    l.add(new NumberKey(rs.getString(1)));
                    if (rs.getMetaData().getColumnCount() > 1) {
                        l2.add(rs.getString(2));
                    }
                }
            }
            int isFuzzy = -1;
            for (int i = searches.length - 1; i > -1; i--) {
                if (((ValuesForVelocityBean) searchHistory.get(searches[i])).getNameForElements().equals(NmrshiftdbConstants.FUZZY) & !(((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.FORMULA) || ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.MULTIPLICITY) || ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.POTMULTIPLICITY))) {
                    isFuzzy = i;
                }
            }
            if (isFuzzy != -1) {
                String searchSoundex = Soundex.soundex(((ValuesForVelocityBean) searchHistory.get(searches[isFuzzy])).getRange(), "9", new StringAndInt());
                Vector firstResults = new Vector();
                for (int i = 0; i < l2.size(); i++) {
                    String name = (String) l2.get(i);
                    StringAndInt sai = new StringAndInt();
                    sai.myString = (String) l.get(i);
                    Soundex.soundex(name, searchSoundex, sai);
                    if (sai.myInt != 0) {
                        int end = (sai.myInt) + ((ValuesForVelocityBean) searchHistory.get(searches[isFuzzy])).getRange().length() - 1;
                        if (end >= name.length() - 1) {
                            end = name.length() - 1;
                        }
                        String subString = name.substring((sai.myInt) - 1, end);
                        sai.myInt = lDistance.isAlike(((ValuesForVelocityBean) searchHistory.get(searches[isFuzzy])).getRange(), subString);
                        firstResults.add(sai);
                    }
                }
                Collections.sort(firstResults, new StringAndIntComparator());
                for (int k = 0; k < firstResults.size(); k++) {
                    if (k == MAX_HITS_TO_DISPLAY) {
                        break;
                    }
                    v.add(((StringAndInt) firstResults.get(k)).myString);
                }
            } else {
                for (int i = 0; i < l.size(); i++) {
                    if (i == MAX_HITS_TO_DISPLAY) {
                        break;
                    }
                    v.add(l.get(i));
                }
            }
            for (int i = 0; i < searches.length; i++) {
                if ((((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSPECTRUM) | ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.TOTALSPECTRUM) | ((ValuesForVelocityBean) searchHistory.get(searches[i])).getDisplayText().equals(NmrshiftdbConstants.SUBSTRUCTURE_SIMILARITY)) && runData != null) {
                    for (int k = 0; k < l2.size(); k++) {
                        similarities.add(new Double((String) l2.get(k)));
                    }
                    break;
                }
            }
            if (chiral && isAChiralSearch && v.size() == 0) {
                chiral = false;
            } else {
                break;
            }
        }
        if (isAChiralSearch && chiral) {
            chiralMessage.myInt = 2;
        }
        if (isAChiralSearch && !chiral) {
            chiralMessage.myInt = 1;
        }
        return (v);
    }

    /**
   *  Parses a jcmap file to a submittingData object.
   *
   * @param  subData                  The signalstable of this will be filled.
   * @param  jcamp                    The path to the file to read.
   * @exception  IOException          Description of Exception
   * @exception  JCAMPException       Description of Exception
   * @exception  NmrshiftdbException  Description of Exception
   * @exception  Exception            Description of Exception
   */
    public static void parseJcamp(SubmittingData subData, String jcamp) throws IOException, JCAMPException, NmrshiftdbException, Exception {
        Spectrum jcampSpectrum = JCAMPReader.getInstance().createSpectrum(jcamp);
        if (!(jcampSpectrum instanceof NMRSpectrum)) {
            throw new NmrshiftdbException("Spectrum in file is not an NMR spectrum!");
        }
        NMRSpectrum nmrspectrum = (NMRSpectrum) jcampSpectrum;
        ArrayList numberSpectrum = new ArrayList();
        Vector oldSignalstable = null;
        if (subData.getSignalstable() != null) {
            oldSignalstable = subData.getSignalstable();
        }
        if (nmrspectrum.hasPeakTable()) {
            Peak[] peaks = nmrspectrum.getPeakTable();
            for (int i = 0; i < peaks.length; i++) {
                ValueTriple vt = new ValueTriple();
                vt.value1 = (float) peaks[i].getPosition()[0];
                BigDecimal bd = new BigDecimal(vt.value1);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                vt.value1 = bd.floatValue();
                vt.value2 = (float) peaks[i].getHeight();
                bd = new BigDecimal(vt.value2);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                vt.value2 = bd.floatValue();
                numberSpectrum.add(vt);
            }
        } else {
            Vector v = new MinHeightPeakPicking(0, 1).calculate(nmrspectrum);
            for (int i = 0; i < v.size(); i++) {
                ValueTriple vt = new ValueTriple();
                vt.value1 = (float) ((Peak) v.get(i)).getPosition()[0];
                BigDecimal bd = new BigDecimal(vt.value1);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                vt.value1 = bd.floatValue();
                vt.value2 = (float) ((Peak) v.get(i)).getHeight();
                bd = new BigDecimal(vt.value2);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                vt.value2 = bd.floatValue();
                numberSpectrum.add(vt);
            }
        }
        ParseUtils.removeDuplicates(numberSpectrum);
        float maxIntensity = 0;
        for (int i = 0; i < numberSpectrum.size(); i++) {
            if (((ValueTriple) numberSpectrum.get(i)).value2 > maxIntensity) {
                maxIntensity = ((ValueTriple) numberSpectrum.get(i)).value2;
            }
        }
        if (maxIntensity > 1) {
            for (int i = 0; i < numberSpectrum.size(); i++) {
                BigDecimal bd = new BigDecimal(((ValueTriple) numberSpectrum.get(i)).value2 / maxIntensity);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                ((ValueTriple) numberSpectrum.get(i)).value2 = bd.floatValue();
            }
        }
        if (oldSignalstable != null) {
            Vector signalstable = subData.getSignalstable();
            for (int i = 0; i < signalstable.size(); i++) {
                for (int k = 0; k < oldSignalstable.size(); k++) {
                    if (((ValueTriple) signalstable.get(i)).value1 == ((ValueTriple) oldSignalstable.get(k)).value1) {
                        ((ValueTriple) signalstable.get(i)).atoms = ((ValueTriple) oldSignalstable.get(k)).atoms;
                    }
                }
            }
        }
        subData.setKeepAssignmentEntries("");
        for (int i = 0; i < numberSpectrum.size(); i++) {
            subData.addToSignalstable(numberSpectrum.get(i));
        }
    }

    /**
   *  Tries to read a FileItem via cdk to a cdk molecule, which is set to the session with attributename as key.
   *
   * @param  fi             The FileItem to read.
   * @param  messages       Error messages get appended here.
   * @param  data           The current runData
   * @param  attributename  The key for the session.
   */
    public static void processFileItem(FileItem fi, StringBuffer messages, RunData data, String attributename) {
        IChemFile chemFile = null;
        IChemObjectReader cor = null;
        try {
            cor = getChemObjectReader(fi.getString());
        } catch (Exception exc) {
            exc.printStackTrace();
            messages.append("Could not determine file format.<br>");
            return;
        }
        if (cor == null) {
            messages.append("Could not determine file format.<br>");
            return;
        }
        String error = null;
        IChemModel chemModel = null;
        if (cor.accepts(new org.openscience.cdk.ChemFile().getClass())) {
            try {
                chemFile = (IChemFile) cor.read((IChemObject) new org.openscience.cdk.ChemFile());
                if (chemFile != null) {
                    processChemFile(chemFile, messages, data, attributename);
                    return;
                } else {
                }
            } catch (Exception exception) {
                error = "Error while reading file: " + exception.getMessage();
            }
        }
        if (error != null) {
            messages.append(error + "<br>");
            return;
        }
        if (cor.accepts(new org.openscience.cdk.ChemModel().getClass())) {
            try {
                chemModel = (IChemModel) cor.read((IChemObject) new org.openscience.cdk.ChemModel());
                if (chemModel != null) {
                    processChemModel(chemModel, messages, data, attributename);
                    return;
                } else {
                }
                error = null;
            } catch (Exception exception) {
                error = "Error while reading file: " + exception.getMessage();
            }
        }
        if (error != null) {
            messages.append(error + "<br>");
        }
    }

    /**
   *  Generates in inchi for  a mol file via cambridge web service.
   *
   * @param  mdl            The mdl to generate for.
   * @return                The inchi
   * @exception  Exception  Description of Exception
   */
    public static String generateInchi(String mdl) throws Exception {
        String endpoint = "http://wwmm-svc.ch.cam.ac.uk/wwmm/services/InChIServer";
        Service service = new Service();
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress(new java.net.URL(endpoint));
        call.setOperationName("generate");
        String ret = (String) call.invoke(new Object[] { "1", "mol", mdl, "" });
        call.setOperationName("getBasic");
        ret = (String) call.invoke(new Object[] { "1", ret });
        return ret;
    }

    /**
   *  Makes a jpeg out of a mol file.
   *
   * @param  mol            The mol file.
   * @param  filename       The file to write the jpeg to
   * @param  servletConfig  The current servlet configuration.
   * @param  width          Width of jpeg.
   * @param  height         Height of jpeg
   * @param  drawnumbers    Should numbers be contained=
   * @exception  Exception  Description of Exception
   */
    public static void makeJpg(String mol, String filename, ServletConfig servletConfig, int width, int height, boolean drawnumbers) throws Exception {
        Color backColor = Color.LIGHT_GRAY;
        MDLReader mdlreader = new MDLReader(new StringReader(mol));
        IMolecule cdkmol = (IMolecule) mdlreader.read(new org.openscience.cdk.Molecule());
        for (int k = 0; k < cdkmol.getAtomCount(); k++) {
            if (cdkmol.getAtom(k).getPoint2d() == null) cdkmol.getAtom(k).setPoint2d(new Point2d(cdkmol.getAtom(k).getPoint3d().x, cdkmol.getAtom(k).getPoint3d().y));
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        cdkmol = saturate(cdkmol);
        Renderer2DModel r2dm = new Renderer2DModel();
        r2dm.setDrawNumbers(drawnumbers);
        r2dm.setBackColor(backColor);
        r2dm.setShowImplicitHydrogens(false);
        r2dm.setShowEndCarbons(false);
        if (height < 200 || width < 200) {
            r2dm.setIsCompact(true);
            r2dm.setBondDistance(3);
        }
        Renderer2D renderer = new Renderer2D(r2dm);
        r2dm.setBackgroundDimension(new Dimension(width, height));
        GeometryTools.translateAllPositive(cdkmol, r2dm.getRenderingCoordinates());
        GeometryTools.scaleMolecule(cdkmol, new Dimension(width, height), 0.8, r2dm.getRenderingCoordinates());
        GeometryTools.center(cdkmol, new Dimension(width, height), r2dm.getRenderingCoordinates());
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setBackground(backColor);
        svgGenerator.setColor(backColor);
        svgGenerator.fill(new Rectangle(0, 0, width, height));
        renderer.paintMolecule(cdkmol, svgGenerator, false, true);
        boolean useCSS = false;
        baos = new ByteArrayOutputStream();
        Writer outwriter = new OutputStreamWriter(baos, "UTF-8");
        StringBuffer sb = new StringBuffer();
        svgGenerator.stream(outwriter, useCSS);
        StringTokenizer tokenizer = new StringTokenizer(baos.toString(), "\n");
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken();
            if (name.length() > 4 && name.substring(0, 5).equals("<svg ")) {
                sb.append(name.substring(0, name.length() - 1)).append(" width=\"" + width + "\" height=\"" + height + "\">" + "\n\r");
            } else {
                sb.append(name + "\n\r");
            }
        }
        File outputFile = new File(ServletUtils.expandRelative(servletConfig, filename));
        ImageTranscoder it = new JPEGTranscoder();
        it.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(.8));
        TranscoderInput input = new TranscoderInput(new StringReader(sb.toString()));
        OutputStream ostream = new FileOutputStream(outputFile);
        TranscoderOutput output = new TranscoderOutput(ostream);
        it.transcode(input, output);
        ostream.flush();
        ostream.close();
    }

    /**
   *  saturates a molecule with (implicit) protons and bonds.
   *
   * @param  cdkmol         The molecule to saturate.
   * @return                The molecule with implicit protons. original molecule will have explicit protons.
   * @exception  Exception  Description of Exception
   */
    public static IMolecule saturate(IMolecule cdkmol) throws Exception {
        new HydrogenAdder().addExplicitHydrogensToSatisfyValency(cdkmol);
        for (int i = 0; i < cdkmol.getBonds().length; i++) {
            if (cdkmol.getBonds()[i].getFlag(CDKConstants.ISAROMATIC) == true) {
                cdkmol.getBonds()[i].setOrder(1);
            }
        }
        new SaturationChecker().newSaturate(cdkmol.getBonds(), cdkmol);
        for (int i = 0; i < cdkmol.getBonds().length; i++) {
            cdkmol.getBonds()[i].setFlag(CDKConstants.ISAROMATIC, false);
        }
        return (IMolecule) AtomContainerManipulator.removeHydrogens(cdkmol);
    }

    /**
   *  Recursivly zips a directory in an zipoutputstream
   *
   * @param  zipDir           The directory.
   * @param  zos              The stream
   * @exception  IOException  Description of Exception
   */
    public static void zipDir(File zipDir, ZipOutputStream zos, File rawdatadir) throws IOException {
        if (zipDir.isFile()) {
            int bytesIn = 0;
            byte[] readBuffer = new byte[2156];
            if (!alreadyAttached.contains(zipDir.getPath())) {
                FileInputStream fis = new FileInputStream(zipDir);
                ZipEntry anEntry = new ZipEntry(zipDir.getPath());
                alreadyAttached.add(zipDir.getPath());
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } else {
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(new File(filePath), zos, rawdatadir);
                    continue;
                }
                if (!alreadyAttached.contains(getRelativePath(rawdatadir, f))) {
                    FileInputStream fis = new FileInputStream(f);
                    ZipEntry anEntry = new ZipEntry(getRelativePath(rawdatadir, f));
                    alreadyAttached.add(getRelativePath(rawdatadir, f));
                    zos.putNextEntry(anEntry);
                    while ((bytesIn = fis.read(readBuffer)) != -1) {
                        zos.write(readBuffer, 0, bytesIn);
                    }
                    fis.close();
                }
            }
        }
    }

    /**
	 * break a path down into individual elements and add to a list.
	 * example : if a path is /a/b/c/d.txt, the breakdown will be [d.txt,c,b,a]
	 * @param f input file
	 * @return a List collection with the individual elements of the path in reverse order
	 */
    private static List getPathList(File f) {
        List l = new ArrayList();
        File r;
        try {
            r = f.getCanonicalFile();
            while (r != null) {
                l.add(r.getName());
                r = r.getParentFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            l = null;
        }
        return l;
    }

    /**
	 * figure out a string representing the relative path of
	 * 'f' with respect to 'r'
	 * @param r home path
	 * @param f path of file
	 */
    private static String matchPathLists(List r, List f) {
        int i;
        int j;
        String s;
        s = "";
        i = r.size() - 1;
        j = f.size() - 1;
        while ((i >= 0) && (j >= 0) && (r.get(i).equals(f.get(j)))) {
            i--;
            j--;
        }
        for (; i >= 0; i--) {
            s += ".." + File.separator;
        }
        for (; j >= 1; j--) {
            s += f.get(j) + File.separator;
        }
        s += f.get(j);
        return s;
    }

    /**
	 * get relative path of File 'f' with respect to 'home' directory
	 * example : home = /a/b/c
	 *           f    = /a/d/e/x.txt
	 *           s = getRelativePath(home,f) = ../../d/e/x.txt
	 * @param home base path, should be a directory, not a file, or it doesn't make sense
	 * @param f file to generate path for
	 * @return path from home to f as a string
	 */
    public static String getRelativePath(File home, File f) {
        List homelist;
        List filelist;
        String s;
        homelist = getPathList(home);
        filelist = getPathList(f);
        s = matchPathLists(homelist, filelist);
        return s;
    }

    /**
   *  Gets a chemObjectReader for a file.
   *
   * @param  file             The file.
   * @return                  The chemObjectReader.
   * @exception  IOException  Description of Exception
   */
    private static IChemObjectReader getChemObjectReader(String file) throws IOException {
        Reader fileReader = new StringReader(file);
        ReaderFactory factory = new ReaderFactory();
        IChemObjectReader reader = factory.createReader(fileReader);
        return reader;
    }

    /**
   *  Tries to read a chemfile via cdk to a cdk molecule, which is set to the session with attributename as key.
   *
   * @param  chemFile       The chemfile.
   * @param  messages       Error messages get appended here.
   * @param  data           The current runData
   * @param  attributename  The key for the session.
   */
    private static void processChemFile(IChemFile chemFile, StringBuffer messages, RunData data, String attributename) {
        int chemSequenceCount = chemFile.getChemSequenceCount();
        IChemSequence chemSequence = null;
        IChemModel chemModel = null;
        for (int i = 0; i < chemSequenceCount; i++) {
            chemSequence = chemFile.getChemSequence(i);
            int chemModelCount = chemSequence.getChemModelCount();
            for (int j = 0; j < chemModelCount; j++) {
                chemModel = chemSequence.getChemModel(j);
                processChemModel(chemModel, messages, data, attributename);
            }
        }
    }

    /**
   *  Tries to read a chemmodel via cdk to a cdk molecule, which is set to the session with attributename as key.
   *
   * @param  chemModel      The chemModel to read.
   * @param  messages       Error messages get appended here.
   * @param  data           The current runData
   * @param  attributename  The key for the session.
   */
    private static void processChemModel(IChemModel chemModel, StringBuffer messages, RunData data, String attributename) {
        if (ChemModelManipulator.getAllInOneContainer(chemModel).getBondCount() == 0) {
            String error = "Model does not have bonds. Cannot depict contents.";
            messages.append(error + "<br>");
            return;
        }
        if (!(GeometryTools.has2DCoordinates(ChemModelManipulator.getAllInOneContainer(chemModel)))) {
            String error = "Model does not have coordinates. Cannot open file.";
            messages.append(error + "<br>");
            return;
        }
        IMolecule mol = chemModel.getMoleculeSet().getMolecule(0);
        data.getSession().setAttribute(attributename, mol);
    }

    public static String readToMol(String input) throws Exception {
        IChemFile chemFile = null;
        IChemObjectReader cor = null;
        cor = getChemObjectReader(input);
        if (cor == null) {
            throw new NmrshiftdbException("Could not determine file format");
        }
        IChemModel chemModel = null;
        if (cor.accepts(new org.openscience.cdk.ChemFile().getClass())) {
            chemFile = (IChemFile) cor.read((IChemObject) new org.openscience.cdk.ChemFile());
            if (chemFile != null) {
                return processChemFile(chemFile);
            }
        }
        if (cor.accepts(new org.openscience.cdk.ChemModel().getClass())) {
            chemModel = (IChemModel) cor.read((IChemObject) new org.openscience.cdk.ChemModel());
            if (chemModel != null) {
                return processChemModel(chemModel);
            }
        }
        throw new NmrshiftdbException("Problems reading your input");
    }

    private static String processChemFile(IChemFile chemFile) throws Exception {
        int chemSequenceCount = chemFile.getChemSequenceCount();
        IChemSequence chemSequence = null;
        IChemModel chemModel = null;
        for (int i = 0; i < chemSequenceCount; i++) {
            chemSequence = chemFile.getChemSequence(i);
            int chemModelCount = chemSequence.getChemModelCount();
            for (int j = 0; j < chemModelCount; j++) {
                chemModel = chemSequence.getChemModel(j);
                return processChemModel(chemModel);
            }
        }
        throw new NmrshiftdbException("Problems reading your input");
    }

    private static String processChemModel(IChemModel chemModel) throws Exception {
        if (ChemModelManipulator.getAllInOneContainer(chemModel).getBondCount() == 0) {
            throw new NmrshiftdbException("Model does not have bonds. Cannot depict contents");
        }
        if (!(GeometryTools.has2DCoordinates(ChemModelManipulator.getAllInOneContainer(chemModel)))) {
            throw new NmrshiftdbException("Model does not have coordinates. Cannot open file");
        }
        IMolecule mol = chemModel.getMoleculeSet().getMolecule(0);
        StringWriter sw = new StringWriter();
        new MDLWriter(sw).writeMolecule(mol);
        return sw.toString();
    }
}
