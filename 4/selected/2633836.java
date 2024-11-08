package ag.cas.xi.af.edix.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.LookupManager;
import com.sap.aii.af.lib.ra.cci.XIAdapterException;
import com.sap.aii.af.lib.trace.Trace;

/**
 * <code>ConvertCRLFfromToLF</code> represents a XI Adapter Framework (AF) compliant
 * module that can be called by the XI AF Module Processor (MP). The XI AF MP concept
 * allows to implement customer exits as well as extensions for arbitrary adapters.
 * A conceptual description can be found in the XI AF API documentation, see
 * the NetWeaver Online help.  
 * In general a AF module is a stateless ejb. It is highly recommended to use the
 * local ejb variant only to prevent unnecessary object serialization that impacts
 * the overall performance. AF modules MUST implement the com.sap.aii.af.mp.module.Module
 * and javax.ejb.SessionBean interface.
 **/
public class XMLtoEDIConverterBean implements SessionBean, Module {

    public static final String VERSION_ID = "$Id: //ag/cas/xi/af/edix/module/XMLtoEDIConverterBean.java#1 $";

    private static final Trace TRACE = new Trace(VERSION_ID);

    private SessionContext myContext;

    private StringBuffer sbXMLPayload = new StringBuffer();

    private String sUNA = null;

    private Boolean unaIncluding = false;

    private String[] arXML = null;

    private int xmlPosition = 0;

    private StringBuffer sbEDI = new StringBuffer();

    private String ServiceStringAdvice = "UNA";

    private int ServiceStringAdviceLength = 3;

    private String dataSeparator = null;

    private String componentSeparator = null;

    private String decimalNotation = ".";

    private String releaseIndicator = "?";

    private String reservedForFutureUse = " ";

    private String segmentTerminator = null;

    private static final String DATASEPARATORSUBST = "^DSS^";

    private static final String COMPONENTSEPARATORSUBST = "^CSS^";

    private static final String DECIMALNOTATIONSUBST = "^DNS^";

    private static final String RELEASEINDICATORSUBST = "^RIS^";

    private static final String RESERVEDFORFUTUREUSESUBST = "^RFS^";

    private static final String SEGMENTTERMINATORSUBST = "^STS^";

    private static final int LENGTHOFSUBSTITUTION = 5;

    private static final String CODEPAGE = "UTF-8";

    private String ediType = null;

    /**
	 * @see javax.ejb.SessionBean#ejbRemove()
	 */
    public void ejbRemove() {
    }

    /**
	 * @see javax.ejb.SessionBean#ejbActivate()
	 */
    public void ejbActivate() {
    }

    /**
	 * @see javax.ejb.SessionBean#ejbPassivate()
	 */
    public void ejbPassivate() {
    }

    /**
	 * @see javax.ejb.SessionBean#setSessionContext(javax.ejb.SessionContext)
	 */
    public void setSessionContext(SessionContext context) {
        myContext = context;
    }

    /**
	 * @see javax.ejb.SessionBean#ejbCreate(t)
	 * @throws CreateException
	 */
    public void ejbCreate() throws CreateException {
    }

    /**
	NEU TEST
	**/
    public static void main(String[] args) {
        String schemaDefaultPath = null;
        schemaDefaultPath = "C:\\Dokumente und Einstellungen\\user\\Desktop\\Default";
        System.out.println("schemaDefaultPath = " + schemaDefaultPath);
        try {
            StringBuffer sbXMLPayload = readPayLoad();
            sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("\r\n", ""));
            sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("\r", ""));
            sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&apos;", "'"));
            sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&quot;", "\""));
            sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&gt;", ">"));
            sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&lt;", "<"));
            sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&amp;", "&"));
            XMLtoEDIConverterBean converter = new XMLtoEDIConverterBean();
            System.out.println("\n" + converter.buildEDIfromXML(sbXMLPayload, schemaDefaultPath).toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR 3");
        }
    }

    private static StringBuffer readPayLoad() {
        StringBuffer ediSB = new StringBuffer();
        String base = "C:\\Dokumente und Einstellungen\\user\\Desktop\\";
        File iFile = new File(base + "receiver_payload.xml");
        long fileLength = iFile.length();
        BufferedReader fr;
        try {
            fr = new BufferedReader(new FileReader(iFile));
            while (true) {
                String line;
                line = fr.readLine();
                if (line == null) break;
                ediSB.append(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("ERROR 1");
        } catch (IOException e1) {
            e1.printStackTrace();
            System.out.println("ERROR 2");
        }
        return ediSB;
    }

    private void analyzeEdi(StringBuffer sbXMLPayload, String schemaDefaultPath) throws ModuleException {
        String SIGNATURE = "analyzeEdi(sbXMLPayload,linkedXsdTree)";
        TRACE.entering(SIGNATURE, new Object[] { sbXMLPayload });
        int ediEdifact = sbXMLPayload.indexOf("S_UNB");
        int ediX12 = sbXMLPayload.indexOf("S_ISA");
        int ediTradacoms = sbXMLPayload.indexOf("S_STX");
        String separator = File.separator;
        String schemaPath = null;
        String schemaFileName = null;
        XSDTree linkedXsdTree = null;
        if (ediEdifact != -1 && ediX12 == -1 && ediTradacoms == -1) {
            componentSeparator = ":";
            segmentTerminator = "'";
            dataSeparator = "+";
            ediType = "edifact";
            String d0065 = null;
            String d0052 = null;
            String d0054 = null;
            String allUNA = null;
            int startUNA = 0;
            int endUNA = 0;
            int i0065 = 0;
            int i0052 = 0;
            int i0054 = 0;
            int i0054end = 0;
            startUNA = sbXMLPayload.indexOf("<S_UNA>");
            if (startUNA >= 0) {
                unaIncluding = true;
                endUNA = sbXMLPayload.indexOf("</S_UNA>");
                allUNA = sbXMLPayload.substring(startUNA, endUNA + 8);
                sbXMLPayload = sbXMLPayload.delete(startUNA, endUNA + 8);
                int iUNA1 = allUNA.indexOf("<D_UNA1>");
                int iUNA2 = allUNA.indexOf("<D_UNA2>");
                int iUNA3 = allUNA.indexOf("<D_UNA3>");
                int iUNA4 = allUNA.indexOf("<D_UNA4>");
                int iUNA5 = allUNA.indexOf("<D_UNA5>");
                int iUNA6 = allUNA.indexOf("<D_UNA6>");
                componentSeparator = allUNA.substring(iUNA1 + 8, iUNA1 + 9);
                dataSeparator = allUNA.substring(iUNA2 + 8, iUNA2 + 9);
                decimalNotation = allUNA.substring(iUNA3 + 8, iUNA3 + 9);
                releaseIndicator = allUNA.substring(iUNA4 + 8, iUNA4 + 9);
                reservedForFutureUse = allUNA.substring(iUNA5 + 8, iUNA5 + 9);
                segmentTerminator = allUNA.substring(iUNA6 + 8, iUNA6 + 9);
                sUNA = "UNA" + componentSeparator + dataSeparator + decimalNotation + releaseIndicator + reservedForFutureUse + segmentTerminator;
                sbEDI.append(sUNA);
            }
            i0065 = sbXMLPayload.indexOf("D_0065");
            if (i0065 <= -1) {
                System.err.println("XMLtoEDI Error: S_UNH Tag not available or not complete!");
                ModuleException me = new ModuleException("XMLtoEDI Error: S_UNH Tag not available or not complete!");
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            i0065 = sbXMLPayload.indexOf(">", i0065);
            d0065 = sbXMLPayload.substring(i0065 + 1, i0065 + 7);
            i0052 = sbXMLPayload.indexOf("D_0052");
            if (i0052 <= -1) {
                System.err.println("XMLtoEDI Error: S_UNH Tag not available or not complete!");
                ModuleException me = new ModuleException("XMLtoEDI Error: S_UNH Tag not available or not complete!");
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            i0052 = sbXMLPayload.indexOf(">", i0052);
            d0052 = sbXMLPayload.substring(i0052 + 1, i0052 + 2);
            i0054 = sbXMLPayload.indexOf("D_0054");
            if (i0054 <= -1) {
                System.err.println("XMLtoEDI Error: S_UNH Tag not available or not complete!");
                ModuleException me = new ModuleException("XMLtoEDI Error: S_UNH Tag not available or not complete!");
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            i0054 = sbXMLPayload.indexOf(">", i0054);
            i0054end = sbXMLPayload.indexOf("<", i0054 + 1);
            d0054 = sbXMLPayload.substring(i0054 + 1, i0054end);
            schemaFileName = d0065 + d0052 + d0054 + ".xsd";
            schemaPath = schemaDefaultPath + separator + schemaFileName;
            TRACE.debugT(SIGNATURE, "schemaPath = " + schemaPath);
            try {
                linkedXsdTree = xsdJAXP(schemaPath);
            } catch (IOException exc) {
                System.err.println("XMLtoEDI Error: EDIX IOException: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX IOException: " + exc.getMessage(), exc);
                TRACE.throwing(SIGNATURE, me);
                throw me;
            } catch (SAXException exc) {
                System.err.println("XMLtoEDI Error: EDIX SAXExceptiond: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX SAXException: " + exc.getMessage(), exc);
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            try {
                if (linkedXsdTree != null) {
                    protectEdiServiceStrings(sbXMLPayload);
                    setXML(sbXMLPayload.toString().replaceAll("\t", "").replaceAll("\n", "").replaceAll("> <", "><").replaceAll("><", ">\r<").split("\r"));
                    buildEDIMessage(linkedXsdTree, schemaFileName);
                    substSubstitutions();
                }
            } catch (Exception exc) {
                System.err.println("XMLtoEDI Error: EDIX processing failed: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX processing failed: " + exc.getMessage());
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
        } else if (ediEdifact == -1 && ediX12 != -1 && ediTradacoms == -1) {
            componentSeparator = "*";
            segmentTerminator = "!";
            dataSeparator = "*";
            ediType = "x12";
            String x12_M = null;
            int ix12_M = 0;
            ix12_M = sbXMLPayload.indexOf("D_143");
            if (ix12_M <= -1) {
                System.err.println("XMLtoEDI Error: M_... Tag not available or not complete!");
                ModuleException me = new ModuleException("XMLtoEDI Error: M_... Tag not available or not complete!");
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            ix12_M = sbXMLPayload.indexOf(">", ix12_M);
            x12_M = sbXMLPayload.substring(ix12_M + 1, ix12_M + 4);
            schemaFileName = "x12_" + x12_M + ".xsd";
            schemaPath = schemaDefaultPath + separator + schemaFileName;
            TRACE.debugT(SIGNATURE, "schemaPath = " + schemaPath);
            try {
                linkedXsdTree = xsdJAXP(schemaPath);
            } catch (IOException exc) {
                System.err.println("XMLtoEDI Error: EDIX IOException: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX IOException: " + exc.getMessage(), exc);
                TRACE.throwing(SIGNATURE, me);
                throw me;
            } catch (SAXException exc) {
                System.err.println("XMLtoEDI Error: EDIX SAXExceptiond: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX SAXException: " + exc.getMessage(), exc);
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            try {
                if (linkedXsdTree != null) {
                    setXML(sbXMLPayload.toString().replaceAll("\t", "").replaceAll("\n", "").replaceAll("> <", "><").replaceAll("><", ">\r<").split("\r"));
                    buildEDIMessage(linkedXsdTree, schemaFileName);
                }
            } catch (Exception exc) {
                System.err.println("XMLtoEDI Error: EDIX processing failed: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX processing failed: " + exc.getMessage());
                throw me;
            }
        } else if (ediEdifact == -1 && ediX12 == -1 && ediTradacoms != -1) {
            componentSeparator = ":";
            segmentTerminator = "'";
            dataSeparator = "+";
            ediType = "tradacoms";
            String type1 = null;
            String type2 = null;
            int itype1 = 0;
            int itype2 = 0;
            itype1 = sbXMLPayload.indexOf("D_TYPE1_2");
            if (itype1 <= -1) {
                System.err.println("XMLtoEDI Error: M_... Tag not available or not complete!");
                ModuleException me = new ModuleException("XMLtoEDI Error: M_... Tag not available or not complete!");
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            itype1 = sbXMLPayload.indexOf(">", itype1);
            type1 = sbXMLPayload.substring(itype1 + 1, itype1 + 4);
            itype2 = sbXMLPayload.indexOf("D_TYPE2_2");
            if (itype2 <= -1) {
                System.err.println("XMLtoEDI Error: M_... Tag not available or not complete!");
                ModuleException me = new ModuleException("XMLtoEDI Error: M_... Tag not available or not complete!");
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            itype2 = sbXMLPayload.indexOf(">", itype2);
            type2 = sbXMLPayload.substring(itype2 + 1, itype2 + 2);
            schemaFileName = "tradacoms_" + type1 + type2 + ".xsd";
            schemaPath = schemaDefaultPath + separator + schemaFileName;
            TRACE.debugT(SIGNATURE, "schemaPath = " + schemaPath);
            try {
                linkedXsdTree = xsdJAXP(schemaPath);
            } catch (IOException exc) {
                System.err.println("XMLtoEDI Error: EDIX IOException: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX IOException: " + exc.getMessage(), exc);
                TRACE.throwing(SIGNATURE, me);
                throw me;
            } catch (SAXException exc) {
                System.err.println("XMLtoEDI Error: EDIX SAXExceptiond: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX SAXException: " + exc.getMessage(), exc);
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            try {
                if (linkedXsdTree != null) {
                    setXML(sbXMLPayload.toString().replaceAll("\t", "").replaceAll("\n", "").replaceAll("> <", "><").replaceAll("><", ">\r<").split("\r"));
                    buildEDIMessage(linkedXsdTree, schemaFileName);
                }
            } catch (Exception exc) {
                System.err.println("XMLtoEDI Error: EDIX processing failed: " + exc);
                ModuleException me = new ModuleException("XMLtoEDI Error: EDIX processing failed: " + exc.getMessage());
                throw me;
            }
        } else {
            System.err.println("XMLtoEDI Error:  No EDI-XML-Message of type EDIFACT, X12 or Tradacoms!");
            ModuleException me = new ModuleException("XMLtoEDI Error:  No EDI-XML-Message of type EDIFACT, X12 or Tradacoms!!");
            throw me;
        }
        TRACE.exiting(SIGNATURE);
    }

    /** 
	 * The main method of AF modules is the <code>process()</code> method. It takes the XI message, changes it
	 * according to some module specific rules and forwards it in the module chain. If this module is the last
	 * module in the chain before the adapter is being called it must ensure that in case of synchronous messages
	 * a response message is sent back in the return <code>ModuleDate</code> parameter.
	 * @param ModuleContext Contains data of the module processor that might be important for the module implementation 
	 * such as current channel ID
	 * @param ModuleData Contains the input XI message as principal data plus eventual set supplement data
	 * @return ModuleData Contains the (changed) output XI message. Might be the response message if the module
	 * is the last in the chain.
	 * @exception ModuleException Describes the cause of the exception and indicates whether an retry is sensible or not.
	 * Meaning of EDI line headers 
	 * UNA = ServiceStringAdvice
	 * UNB = InterchangeHeader
	 * UNG = FunctionalGroupHeader
	 * UNH = MessageHeader
	 * BGM = Beginning of Message
	 * UNE = FunctionalGroupTrailer
	 * UNZ = InterchangeTrailer
	 */
    public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException {
        String SIGNATURE = "process(ModuleContext moduleContext, ModuleData inputModuleData)";
        TRACE.entering(SIGNATURE, new Object[] { moduleContext, inputModuleData });
        Object obj = null;
        Message msg = null;
        MessageKey mk = null;
        try {
            obj = inputModuleData.getPrincipalData();
            msg = (Message) obj;
            mk = new MessageKey(((Message) inputModuleData.getPrincipalData()).getMessageId(), (((Message) inputModuleData.getPrincipalData()).getMessageDirection()));
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            if (obj != null) TRACE.errorT(SIGNATURE, "Input ModuleData does not contain an object that implements the XI message interface. The object class is: {0}", new Object[] { obj.getClass().getName() }); else TRACE.errorT(SIGNATURE, "Input ModuleData contains only null as XI message");
            ModuleException me = new ModuleException("XMLtoEDI Error: " + e.getMessage(), e);
            TRACE.throwing(SIGNATURE, me);
            throw me;
        }
        String cid = null;
        String schemaDefaultPath = null;
        Channel channel = null;
        try {
            schemaDefaultPath = (String) moduleContext.getContextData("schemaDefaultPath");
            cid = moduleContext.getChannelID();
            channel = (Channel) LookupManager.getInstance().getCPAObject(CPAObjectType.CHANNEL, cid);
            if (schemaDefaultPath == null) {
                TRACE.debugT(SIGNATURE, "Mode parameter is not set. Switch to '' as default.");
                schemaDefaultPath = "";
                ModuleException me = new ModuleException("XMLtoEDI Error: No schema default path!");
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
            TRACE.debugT(SIGNATURE, "Mode is set to {0}", new Object[] { schemaDefaultPath });
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, "Cannot read the module context and configuration data");
            ModuleException me = new ModuleException("XMLtoEDI Error: " + e.getMessage(), e);
            TRACE.throwing(SIGNATURE, me);
            throw me;
        }
        xmlPosition = 0;
        arXML = null;
        sbXMLPayload = new StringBuffer();
        sbEDI = new StringBuffer();
        dataSeparator = "+";
        componentSeparator = ":";
        decimalNotation = ".";
        releaseIndicator = "?";
        try {
            XMLPayload xmlpayload = msg.getDocument();
            if (xmlpayload != null) {
                sbXMLPayload.append(xmlpayload.getText().toString());
                sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("\r\n", ""));
                sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("\r", ""));
                sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&apos;", "'"));
                sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&quot;", "\""));
                sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&gt;", ">"));
                sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&lt;", "<"));
                sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&amp;", "&"));
                xmlpayload.setContent(buildEDIfromXML(sbXMLPayload, schemaDefaultPath).toString().getBytes());
            }
            inputModuleData.setPrincipalData(msg);
            TRACE.debugT(SIGNATURE, "XMLtoEDI conversion finished sucessfully.");
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            TRACE.errorT(SIGNATURE, "Cannot convert one of the payloads. Reason: {0}", new Object[] { e.getMessage() });
            ModuleException me = new ModuleException(new ModuleException("MessageKey: " + mk.toString() + " EDIValidate Error: " + e.getMessage()));
            TRACE.throwing(SIGNATURE, me);
            throw me;
        }
        TRACE.exiting(SIGNATURE);
        return inputModuleData;
    }

    /**
	 * Converts content from EDI in payload to XML 
	 * @param src Byte array with text data (UTF-8 is assumed)
	 * @return Converted byte array 
	 */
    private StringBuffer buildEDIfromXML(StringBuffer sbXMLPayload, String schemaDefaultPath) throws ModuleException {
        String SIGNATURE = "convertXMLtoEDI(ediStringBuffer)";
        TRACE.entering(SIGNATURE, new Object[] { sbXMLPayload });
        analyzeEdi(sbXMLPayload, schemaDefaultPath);
        if (ediType.equals("tradacoms")) {
            String[] segments = sbEDI.toString().split("(?<=[" + segmentTerminator + "])");
            StringBuffer tempSbXML = new StringBuffer();
            for (int i = 0; i < segments.length; i++) {
                tempSbXML.append(segments[i].replaceFirst("[+]", "="));
            }
            sbEDI = tempSbXML;
        }
        TRACE.exiting(SIGNATURE);
        return sbEDI;
    }

    private XSDTree xsdJAXP(String xsdURI) throws IOException, SAXException {
        XSDTree xsdTree = new XSDTree();
        XMLReader reader = XMLReaderFactory.createXMLReader();
        ContentHandler xsdHandler = new XSDHandler(xsdTree);
        reader.setContentHandler(xsdHandler);
        InputSource inputSource = new InputSource(xsdURI);
        reader.parse(inputSource);
        XSDTree linkedTree = linkTreeNodes(xsdTree);
        xsdTree = null;
        return linkedTree;
    }

    @SuppressWarnings("unchecked")
    private XSDTree linkTreeNodes(XSDTree xsdTree) {
        XSDTree tmpNode = null;
        XSDTree rootNode = null;
        XSDTree linkedXsdTree = new XSDTree();
        ArrayList<Object> myChildren = xsdTree.getMyChildren();
        boolean found = false;
        for (int i = 0; i < myChildren.size(); i++) {
            rootNode = (XSDTree) myChildren.get(i);
            for (int j = 0; j < myChildren.size(); j++) {
                tmpNode = (XSDTree) myChildren.get(j);
                if (tmpNode.getAttributeList() != null && tmpNode.getAttributeList().contains(rootNode.getNodeName())) {
                    found = true;
                    break;
                }
            }
            if (!found) break;
        }
        if (rootNode != null) {
            rootNode.setNodeType(rootNode.getNodeName().substring(0, 1));
            linkedXsdTree = rootNode;
            linkedXsdTree = xsdTreeLink(xsdTree, linkedXsdTree, rootNode, "");
        } else {
            System.out.println("No root node found!");
        }
        return linkedXsdTree;
    }

    private XSDTree xsdTreeLink(XSDTree initialTree, XSDTree linkedTree, XSDTree parent, String spacer) {
        if (parent.getAttributeList().size() > 0) {
            for (int i = 0; i < parent.getAttributeList().size(); i++) {
                String nodeName = (String) parent.getAttributeList().get(i);
                for (int j = 0; j < initialTree.getMyChildren().size(); j++) {
                    XSDTree treeTemp = (XSDTree) initialTree.getMyChildren().get(j);
                    if (treeTemp.getNodeName().equals(nodeName)) {
                        treeTemp.setNodeType(treeTemp.getNodeName().substring(0, 1));
                        treeTemp.setSeqType(treeTemp.getNodeName().substring(2));
                        treeTemp.setOccurence((Integer) parent.getOccurenceList().get(i));
                        parent.newChild(treeTemp);
                        linkedTree = parent;
                        spacer = spacer + " ";
                        xsdTreeLink(initialTree, linkedTree, treeTemp, spacer);
                        break;
                    }
                }
            }
        }
        return linkedTree;
    }

    private StringBuffer protectEdiServiceStrings(StringBuffer sb) {
        int m = sb.indexOf("\">") + 1;
        int a = -1;
        a = m;
        int i = sb.indexOf(releaseIndicator, a);
        while (a < i) {
            a = i;
            sb.replace(i, i + 1, releaseIndicator + releaseIndicator);
            i = sb.indexOf(releaseIndicator, a + 2);
        }
        a = m;
        i = sb.indexOf(dataSeparator, a);
        while (a < i) {
            a = i;
            sb.replace(i, i + 1, releaseIndicator + dataSeparator);
            i = sb.indexOf(dataSeparator, a + 2);
        }
        a = m;
        i = sb.indexOf(componentSeparator, a);
        while (a < i) {
            a = i;
            sb.replace(i, i + 1, releaseIndicator + componentSeparator);
            i = sb.indexOf(componentSeparator, a + 2);
        }
        a = m;
        i = sb.indexOf(segmentTerminator, a);
        while (a < i) {
            a = i;
            sb.replace(i, i + 1, releaseIndicator + segmentTerminator);
            i = sb.indexOf(segmentTerminator, a + 2);
        }
        return sb;
    }

    /**
	 * @param edi
	 * The edi to set.
	 */
    private void setXML(String[] edi) {
        this.arXML = edi;
    }

    private void buildEDIMessage(XSDTree treeNode, String schemaFileName) throws ModuleException {
        XSDTree tmpNode;
        ArrayList<Object> myChildren = treeNode.getMyChildren();
        String nodeName = "";
        String chopedNodeName = "";
        String SIGNATURE = "buildEDIMessage(XSDTree treeNode, String schemaFileName)";
        try {
            if (treeNode.getNodeType().equals("G")) {
                XSDTree firstChild = (XSDTree) treeNode.getMyChildren().get(0);
                if ((arXML[xmlPosition] != null) && (firstChild.getSeqType().equals(arXML[xmlPosition + 1].substring(3, arXML[xmlPosition + 1].indexOf(">"))))) {
                    if (treeNode.getOccurence().intValue() > 1) {
                        do {
                            xmlPosition++;
                            traverseChildren(myChildren, schemaFileName);
                            xmlPosition++;
                        } while ((arXML[xmlPosition] != null) && (firstChild.getSeqType().equals(arXML[xmlPosition + 1].substring(3, arXML[xmlPosition + 1].indexOf(">")))));
                    } else {
                        xmlPosition++;
                        traverseChildren(myChildren, schemaFileName);
                        xmlPosition++;
                    }
                }
            } else if (treeNode.getNodeType().equals("S")) {
                if ((arXML[xmlPosition] != null) && (treeNode.getSeqType().equals(arXML[xmlPosition].substring(3, arXML[xmlPosition].indexOf(">"))))) {
                    int c = 1;
                    do {
                        int s = treeNode.getSeqType().indexOf("_");
                        if (s != -1) {
                            sbEDI.append(treeNode.getSeqType().substring(0, s));
                        } else {
                            sbEDI.append(treeNode.getSeqType());
                        }
                        xmlPosition++;
                        for (int i = 0; i < myChildren.size(); i++) {
                            c = traverseComponents((XSDTree) myChildren.get(i), c);
                        }
                        c = 1;
                        sbEDI.append(segmentTerminator);
                        xmlPosition++;
                    } while ((arXML.length > xmlPosition) && (treeNode.getSeqType().equals(arXML[xmlPosition].substring(3, arXML[xmlPosition].indexOf(">")))) && treeNode.getOccurence().intValue() > 1);
                }
            } else if ((treeNode.getNodeType().equals("B")) || (treeNode.getNodeType().equals("M"))) {
                XSDTree firstChild = (XSDTree) treeNode.getMyChildren().get(0);
                if ((arXML[xmlPosition] != null) && (firstChild.getSeqType().equals(arXML[xmlPosition + 1].substring(3, arXML[xmlPosition + 1].indexOf(">"))))) {
                    do {
                        xmlPosition++;
                        traverseChildren(myChildren, schemaFileName);
                        xmlPosition++;
                    } while ((arXML[xmlPosition] != null) && (firstChild.getSeqType().equals(arXML[xmlPosition + 1].substring(3, arXML[xmlPosition + 1].indexOf(">")))));
                }
            } else if (treeNode.getNodeType().equals("L")) {
                xmlPosition++;
                xmlPosition++;
                traverseChildren(myChildren, schemaFileName);
                xmlPosition++;
                if (xmlPosition < arXML.length) {
                    System.err.println("XMLtoEDI Error: XML contains lines which are not declareated in XSD");
                    ModuleException me = new ModuleException("XMLtoEDI Error: XML contains lines which are not declareated in XSD");
                    throw me;
                }
            }
        } catch (Exception e) {
            TRACE.catching(SIGNATURE, e);
            ModuleException me = new ModuleException("XMLtoEDI Error: buildEDIMessage(): " + e.getMessage(), e);
            TRACE.throwing(SIGNATURE, me);
            throw me;
        }
    }

    private int traverseComponents(XSDTree node, int c) {
        if (node.getNodeType().equals("C")) {
            if (node.getNodeName().equals(arXML[xmlPosition].substring(1, arXML[xmlPosition].indexOf(">")))) {
                int i = 0;
                int d = 0;
                xmlPosition++;
                for (int j = 0; j < c; j++) {
                    sbEDI.append(dataSeparator);
                }
                c = 1;
                do {
                    d = traverseComponent((XSDTree) node.getMyChildren().get(i), d);
                    i++;
                } while (i < node.getMyChildren().size());
                xmlPosition++;
            } else {
                c++;
            }
        } else if (node.getNodeType().equals("D")) {
            if (node.getNodeName().equals(arXML[xmlPosition].substring(1, arXML[xmlPosition].indexOf(">")))) {
                for (int j = 0; j < c; j++) {
                    sbEDI.append(dataSeparator);
                }
                c = 1;
                if (arXML[xmlPosition].indexOf("</") != -1) {
                    sbEDI.append(arXML[xmlPosition].replaceAll("<" + node.getNodeName() + ">", "").replaceAll("</" + node.getNodeName() + ">", ""));
                } else {
                    xmlPosition++;
                }
                xmlPosition++;
            } else {
                c++;
            }
        }
        return c;
    }

    private int traverseComponent(XSDTree node, int d) {
        if (node.getNodeType().equals("D")) {
            if (node.getNodeName().equals(arXML[xmlPosition].substring(1, arXML[xmlPosition].indexOf(">")))) {
                if (d > 0) {
                    for (int j = 0; j < d; j++) {
                        sbEDI.append(componentSeparator);
                    }
                    d = 1;
                } else {
                    d++;
                }
                if (arXML[xmlPosition].indexOf("</") != -1) {
                    sbEDI.append(arXML[xmlPosition].replaceAll("<" + node.getNodeName() + ">", "").replaceAll("</" + node.getNodeName() + ">", ""));
                } else {
                    xmlPosition++;
                }
                xmlPosition++;
            } else {
                d++;
            }
        }
        return d;
    }

    private void traverseChildren(ArrayList myChildren, String schemaFileName) {
        for (int j = 0; j < myChildren.size(); j++) {
            XSDTree tmpNode = (XSDTree) myChildren.get(j);
            String nodeName = tmpNode.getNodeName();
            try {
                buildEDIMessage(tmpNode, schemaFileName);
            } catch (Exception e) {
                System.err.println("traverse Children" + e);
            }
        }
    }

    private void substSubstitutions() {
    }

    private void printLinkedTree(XSDTree linkedTree) {
        System.out.println("\nStart printing out linkedXsdTree.......");
        XSDTree tmpNode;
        ArrayList myChildren = linkedTree.getMyChildren();
        String spacer = "";
        System.out.println("Name: " + linkedTree.getNodeName());
        for (int j = 0; j < myChildren.size(); j++) {
            tmpNode = (XSDTree) myChildren.get(j);
            System.out.println("Name: " + tmpNode.getNodeName());
            for (int k = 0; k < tmpNode.getAttributeList().size(); k++) {
                System.out.println(spacer + "______" + tmpNode.getAttributeList().get(k));
            }
            spacer = spacer + "    ";
        }
        System.out.println("\nEnd printing out linkedXsdTree.......\n");
    }

    private void checkMetaData() {
        if ("UNA".equals(arXML[xmlPosition].substring(0, 3))) {
            componentSeparator = arXML[xmlPosition].substring(3, 4);
            dataSeparator = arXML[xmlPosition].substring(4, 5);
            decimalNotation = arXML[xmlPosition].substring(5, 6);
            releaseIndicator = arXML[xmlPosition].substring(6, 7);
            xmlPosition++;
        }
    }
}
