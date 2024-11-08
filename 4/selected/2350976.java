package com.orientechnologies.odbms.tools;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import org.apache.crimson.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.orientechnologies.jdo.oConstants;
import com.orientechnologies.jdo.oDynaObject;
import com.orientechnologies.jdo.oOID;
import com.orientechnologies.jdo.oPersistenceManager;
import com.orientechnologies.jdo.oPersistenceManagerFactory;
import com.orientechnologies.jdo.oUtility;
import com.orientechnologies.jdo.interfaces.oInterface;
import com.orientechnologies.jdo.interfaces.oProperty;
import com.orientechnologies.jdo.types.oBinary;
import com.orientechnologies.jdo.types.oTypes;
import com.orientechnologies.jdo.utils.oFormatOutput;

/**
 * Imports database from XML Data file. XML Data files may be generated through
 * DbExport tool. Copyright (c) 2001-2004 Orient Technologies
 * (www.orientechnologies.com)
 * 
 * @author Orient Staff (staff@orientechnologies.com)
 * @version 2.3
 */
public class DbImport extends GenericTool {

    /**
	 * Constructor for interactive execution
	 */
    public DbImport() {
        parameters.put("force", "true");
    }

    public void start(String[] iArgs) throws Exception {
        loadArgs(iArgs);
        translate();
    }

    private void translate() {
        String database = (String) parameters.get("database");
        String finput = (String) parameters.get("input");
        boolean silent = parameters.containsKey("silent") ? parameters.get("silent").equals("true") : false;
        boolean verbose = parameters.containsKey("verbose") ? parameters.get("verbose").equals("true") : false;
        boolean continueOnErrors = parameters.containsKey("continueOnErrors") ? parameters.get("continueOnErrors").equals("true") : false;
        if (verbose && silent) syntaxError("Cannot specify 'silent' and 'verbose' together");
        oPersistenceManagerFactory factory = new oPersistenceManagerFactory();
        try {
            mgr = DbUtils.openDatabase(factory, database, this);
            makeImport(mgr, finput, silent, verbose, continueOnErrors, null);
        } finally {
            if (mgr != null) mgr.close();
            if (factory != null) factory.close();
        }
    }

    public void makeImport(oPersistenceManager iManager, String finput, boolean iSilent, boolean iVerbose, boolean iContinueOnErrors, MonitorableTool iProgressEvent) {
        mgr = iManager;
        monitor = iProgressEvent;
        if (iSilent) setOutput(null);
        printRealTitle();
        try {
            DocumentBuilderFactoryImpl docBuilderFactory = new DocumentBuilderFactoryImpl();
            docBuilderFactory.setIgnoringComments(true);
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilderFactory.setCoalescing(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            writeOutput("\nStart importing from XML input stream...");
            Document doc = null;
            if (finput != null && finput.length() > 0) doc = docBuilder.parse(finput); else doc = docBuilder.parse(input);
            Node radix = doc.getDocumentElement();
            if (!radix.getNodeName().equals("database")) throw new ToolException("'database' node not found as root");
            objectsImported = new HashMap();
            propertiesNotFound = new HashMap();
            importNodes(radix, mgr, iVerbose, iContinueOnErrors);
        } catch (Exception e) {
            writeOutput("\nException: " + e.toString());
        }
    }

    /**
	 * Import an entire object set
	 */
    private boolean importNodes(Node iSetNode, oPersistenceManager iManager, boolean iVerbose, boolean iContinueOnErrors) throws Exception {
        Node currNode;
        String destOID;
        int objCount;
        int errCount = 0;
        int totNodes = 0;
        int effectiveTotNodes = 0;
        warnCount = 0;
        NodeList nodes = iSetNode.getChildNodes();
        StringBuffer reportMsg = new StringBuffer();
        try {
            totNodes = nodes.getLength();
            writeOutput("ok, found " + totNodes + " XML nodes.");
            if (monitor != null) monitor.notifyStatus("Starting importing of objects...", 1);
            StringBuffer msg = new StringBuffer();
            for (objCount = 0; objCount < totNodes; ++objCount) {
                if (monitor != null && monitor.isCanceled()) throw new InterruptedException("Import canceled by user");
                if (monitor != null) {
                    msg.setLength(0);
                    msg.append("Load object ");
                    msg.append(objCount);
                    msg.append(" of ");
                    msg.append(totNodes);
                    monitor.notifyStatus(msg.toString(), objCount * 100 / totNodes);
                }
                try {
                    outInsideNode = false;
                    if (iVerbose) writeOutput("\n- XML node #" + oFormatOutput.print(String.valueOf(objCount + 1), 6, oFormatOutput.ALIGN_LEFT, '.') + ": ");
                    currNode = nodes.item(objCount);
                    getObjectRef(nodes.item(objCount), iManager, iVerbose, false);
                } catch (Exception e) {
                    ++errCount;
                    writeOutput("\nException: " + e);
                    if (!iContinueOnErrors) {
                        writeOutput("\nExecution broken.");
                        return false;
                    }
                }
            }
            reportMsg.append("Import of objects completed.");
        } catch (InterruptedException e) {
            reportMsg.append("Aborted by user.");
        } finally {
            reportMsg.append("\n\nTotal object imported: " + objectsImported.size() + " of " + totNodes + " XML nodes found.");
            reportMsg.append("\nTotal warning found..: " + warnCount + ".");
            reportMsg.append("\nTotal error found....: " + errCount + ".");
            writeOutput(reportMsg.toString());
            if (monitor != null) monitor.notifyStatus(reportMsg.toString(), 100);
        }
        return true;
    }

    private oOID getObjectRef(Node iObjNode, oPersistenceManager iManager, boolean iVerbose, boolean iEmbedded) throws ToolException {
        oOID destOID = null;
        String sourceOID = getSourceOID(iObjNode);
        if (sourceOID == null) sourceOID = "[no oid]"; else {
            destOID = (oOID) objectsImported.get(sourceOID);
            if (destOID != null) {
                if (iVerbose) writeObjectMessage("Warning: " + sourceOID + " already imported as " + destOID);
                ++warnCount;
                return destOID;
            }
        }
        oDynaObject obj = importNode(iObjNode, iManager, iVerbose, iEmbedded);
        if (obj != null) destOID = obj.getOid();
        return destOID;
    }

    private oDynaObject importNode(Node iObjNode, oPersistenceManager iManager, boolean iVerbose, boolean iEmbedded) throws ToolException {
        if (iObjNode.getNodeName().equals("#text")) {
            if (iVerbose) writeOutput("(Warning: object not found, maybe comments. Ignore it)");
            ++warnCount;
            return null;
        }
        String className = iObjNode.getNodeName();
        String sourceOID = getSourceOID(iObjNode);
        if (sourceOID == null) sourceOID = "[no oid]";
        oInterface ifc = mgr.getDomain().getInterface(className);
        if (ifc == null) throw new ToolException("Class '" + className + "' not found in database schema");
        oDynaObject obj = new oDynaObject(iManager, className);
        oProperty prop;
        String typeName;
        Node currNode;
        String attrName;
        String currStringValue;
        Object currValue;
        NodeList nodes = iObjNode.getChildNodes();
        NodeList embeddedNodes;
        for (int attrCount = 0; attrCount < nodes.getLength(); ++attrCount) {
            currNode = nodes.item(attrCount);
            attrName = currNode.getNodeName();
            prop = ifc.getProperty(attrName);
            if (prop == null) {
                if (iVerbose && !propertiesNotFound.containsKey(className + "." + attrName)) {
                    propertiesNotFound.put(className + "." + attrName, "0");
                    writeObjectMessage("Warning: property '" + className + "." + attrName + "' not found in current database schema: ignore it");
                    ++warnCount;
                }
                continue;
            }
            if (currNode.getFirstChild() == null) continue;
            currStringValue = currNode.getFirstChild().getNodeValue();
            switch(prop.type) {
                case oTypes.OSHORT:
                case oTypes.OUSHORT:
                    currValue = new java.lang.Short(currStringValue);
                    break;
                case oTypes.OLONG:
                case oTypes.OULONG:
                    currValue = new java.lang.Integer(currStringValue);
                    break;
                case oTypes.OBIGLONG:
                    currValue = new java.lang.Long(currStringValue);
                    break;
                case oTypes.OFLOAT:
                    currValue = new java.lang.Float(currStringValue);
                    break;
                case oTypes.ODOUBLE:
                    currValue = new java.lang.Double(currStringValue);
                    break;
                case oTypes.OCHAR:
                    currValue = new java.lang.Character(currStringValue.charAt(0));
                    break;
                case oTypes.OBYTE:
                    currValue = new java.lang.Byte(currStringValue);
                    break;
                case oTypes.OBOOLEAN:
                    currValue = new java.lang.Boolean(currStringValue);
                    break;
                case oTypes.OSTRING:
                    currValue = currStringValue;
                    break;
                case oTypes.ODATE:
                case oTypes.OTIME:
                    try {
                        currValue = iManager.getDateFormat().parse(currStringValue);
                    } catch (Exception e) {
                        currValue = null;
                        if (iVerbose) writeObjectMessage("Exception on parsing date '" + currStringValue + "': " + e + "");
                    }
                    break;
                case oTypes.OINTERVAL:
                case oTypes.OTIMESTAMP:
                    currValue = new java.sql.Timestamp(Long.parseLong(currStringValue));
                    break;
                case oTypes.OREF:
                    currValue = getObjectRef(currNode.getChildNodes().item(0), iManager, iVerbose, false);
                    break;
                case oTypes.OARRAY:
                case oTypes.OVECTOR:
                    currValue = null;
                    break;
                case oTypes.OBINARY:
                    currValue = new oBinary(currStringValue.getBytes());
                    break;
                case oTypes.OEMBEDDED:
                    {
                        if (prop.embedded.startsWith(iManager.getDatabaseName() + "::")) currValue = importNode(currNode.getChildNodes().item(0), iManager, iVerbose, true); else {
                            Collection coll = null;
                            if (prop.embedded.equals("System::d_Varray") || prop.embedded.equals("System::d_Set")) coll = new Vector();
                            if (coll != null) {
                                embeddedNodes = currNode.getChildNodes();
                                if (prop.relationship) {
                                    for (int i = 0; i < embeddedNodes.getLength(); ++i) coll.add(getObjectRef(embeddedNodes.item(i), iManager, iVerbose, false));
                                } else {
                                    for (int i = 0; i < embeddedNodes.getLength(); ++i) coll.add(importNode(embeddedNodes.item(i), iManager, iVerbose, true));
                                }
                                currValue = coll;
                            } else currValue = null;
                        }
                        break;
                    }
                default:
                    currValue = null;
            }
            typeName = oUtility.getClassFromId(iManager, prop.type);
            obj.setValue(attrName, currValue);
        }
        if (iEmbedded) {
            if (iVerbose) writeObjectMessage(obj.getClassName() + " as embedded");
        } else {
            obj.store();
            oOID destOID = obj.getOid();
            if (iVerbose) writeObjectMessage(obj.getClassName() + " " + sourceOID + " -> " + destOID);
            objectsImported.put(sourceOID, destOID);
        }
        return obj;
    }

    private String getSourceOID(Node iNode) {
        try {
            return iNode.getAttributes().getNamedItem("oid").getNodeValue();
        } catch (Exception e) {
            return null;
        }
    }

    private void loadArgs(String[] iArgs) {
        if (iArgs == null) syntaxError("Missed <database> parameter");
        parameters.put("database", iArgs[0]);
        for (int i = 1; i < iArgs.length; ++i) {
            if (iArgs[i].startsWith("-s")) parameters.put("silent", "true"); else if (iArgs[i].startsWith("-v")) parameters.put("verbose", "true"); else if (iArgs[i].startsWith("-c")) parameters.put("continueOnErrors", "true"); else if (iArgs[i].startsWith("-i")) parameters.put("input", iArgs[i].substring(2));
        }
    }

    protected void printRealTitle() {
        writeOutput("\nOrient ODBMS oDbImport v. " + oConstants.PRODUCT_VERSION + " - " + oConstants.PRODUCT_COPYRIGHTS + " (" + oConstants.PRODUCT_WWW + ")\n");
    }

    protected void printTitle() {
    }

    protected void printFormat() {
        writeOutput("\nFormat: oDbImport <database> -s [-i<input_file>]");
        writeOutput("\n where: <database>   = database name");
        writeOutput("\n        -s           = silent mode (no output)");
        writeOutput("\n        -v           = verbose mode");
        writeOutput("\n        -c           = continue on errors");
        writeOutput("\n        <input_file> = optional input file name to import information;");
        writeOutput("\n                       default inout is STDIN\n");
    }

    private void writeObjectMessage(String iMessage) {
        if (outInsideNode) writeOutput("\n                    "); else outInsideNode = true;
        writeOutput(iMessage);
    }

    private int warnCount;

    private boolean outInsideNode;

    private HashMap objectsImported = null;

    private HashMap propertiesNotFound = null;

    private oPersistenceManager mgr = null;

    private StringBuffer buffer = null;

    private MonitorableTool monitor = null;

    private InputStream input = System.in;
}
