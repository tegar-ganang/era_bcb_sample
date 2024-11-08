package ag.cas.xi.af.edix.module;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.LookupManager;
import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.aii.af.lib.ra.cci.XIAdapterException;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.exception.*;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.TextPayload;
import com.sap.engine.interfaces.messaging.api.XMLPayload;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.aii.af.lib.trace.*;

/**
 * <code>EDItoXMLConverterBean</code> represents a XI Adapter Framework (AF) compliant
 * module that can be called by the XI AF Module Processor (MP). The XI AF MP concept
 * allows to implement customer exits as well as extensions for arbitrary adapters.
 * A conceptual description can be found in the XI AF API documentation, see
 * the NetWeaver Online help.  
 * In general a AF module is a stateless ejb. It is highly recommended to use the
 * local ejb variant only to prevent unnecessary object serialization that impacts
 * the overall performance. AF modules MUST implement the com.sap.aii.af.mp.module.Module
 * and javax.ejb.SessionBean interface.
 **/
public class EDItoXMLConverterBean implements SessionBean, Module {

    public static final String VERSION_ID = "$Id: //ag/cas/xi/af/module/EDItoXMLConverter.java#1 $";

    private static final Trace TRACE = new Trace(VERSION_ID);

    private SessionContext myContext;

    private StringBuffer sbXMLPayload = new StringBuffer();

    private StringBuffer sbXML = new StringBuffer();

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
            ModuleException me = new ModuleException("EDItoXML Error: " + e.getMessage(), e);
            TRACE.throwing(SIGNATURE, me);
            throw me;
        }
        String cid = null;
        Channel channel = null;
        String validateFlag = null;
        String basicXSDFolderPath = null;
        String defaultXSDFolder = null;
        String exceptionFlag = null;
        String originalMessageFlag = null;
        String encoding = null;
        validateFlag = (String) moduleContext.getContextData("validateFlag");
        if (validateFlag == null) {
            TRACE.debugT(SIGNATURE, "No Configuration parameter validateFlag is set!");
            ModuleException me = new ModuleException("No Configuration parameter validateFlag is set!");
            TRACE.throwing(SIGNATURE, me);
            throw me;
        }
        if (validateFlag.equals("true")) {
            try {
                basicXSDFolderPath = (String) moduleContext.getContextData("basicXSDFolderPath");
                defaultXSDFolder = (String) moduleContext.getContextData("defaultXSDFolder");
                exceptionFlag = (String) moduleContext.getContextData("exceptionFlag");
                originalMessageFlag = (String) moduleContext.getContextData("originalMessageFlag");
                encoding = (String) moduleContext.getContextData("encoding");
                cid = moduleContext.getChannelID();
                channel = (Channel) LookupManager.getInstance().getCPAObject(CPAObjectType.CHANNEL, cid);
                if (basicXSDFolderPath == null) {
                    TRACE.debugT(SIGNATURE, "XSD folder path parameter is not set. Switch to '' as default.");
                    basicXSDFolderPath = "";
                    ModuleException me = new ModuleException("EDIValidate Error: No basic xsd folder path set!");
                    TRACE.throwing(SIGNATURE, me);
                    throw me;
                }
                TRACE.debugT(SIGNATURE, "The XSD folder path is set to {0}", new Object[] { basicXSDFolderPath });
                if (defaultXSDFolder == null) {
                    TRACE.debugT(SIGNATURE, "Default xsd folder is not set. Switch to '' as default.");
                    defaultXSDFolder = "";
                    ModuleException me = new ModuleException("EDIValidate Error: No default xsd folder set!");
                    TRACE.throwing(SIGNATURE, me);
                    throw me;
                }
                TRACE.debugT(SIGNATURE, "The default xsd folder is set to {0}", new Object[] { defaultXSDFolder });
                if (exceptionFlag == null) {
                    TRACE.debugT(SIGNATURE, "Exception flag parameter is not set.");
                    exceptionFlag = "";
                    ModuleException me = new ModuleException("EDIValidate Error: No exception flag is set!");
                    TRACE.throwing(SIGNATURE, me);
                    throw me;
                }
                TRACE.debugT(SIGNATURE, "The XSD folder path is set to {0}", new Object[] { basicXSDFolderPath });
                if (originalMessageFlag == null) {
                    TRACE.debugT(SIGNATURE, "Original Message Flag parameter is not set.");
                    originalMessageFlag = "";
                    ModuleException me = new ModuleException("EDIValidate Error: No Original Message Flag is set!");
                    TRACE.throwing(SIGNATURE, me);
                    throw me;
                }
                TRACE.debugT(SIGNATURE, "The XSD folder path is set to {0}", new Object[] { basicXSDFolderPath });
                if (encoding == null) {
                    encoding = "";
                }
            } catch (Exception e) {
                TRACE.catching(SIGNATURE, e);
                TRACE.errorT(SIGNATURE, "Cannot read the module context and configuration data");
                ModuleException me = new ModuleException("EDIValidate process() Error: " + e.getMessage(), e);
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
        } else {
            try {
                basicXSDFolderPath = (String) moduleContext.getContextData("basicXSDFolderPath");
                defaultXSDFolder = (String) moduleContext.getContextData("defaultXSDFolder");
                exceptionFlag = (String) moduleContext.getContextData("exceptionFlag");
                originalMessageFlag = (String) moduleContext.getContextData("originalMessageFlag");
                encoding = (String) moduleContext.getContextData("encoding");
                cid = moduleContext.getChannelID();
                channel = (Channel) LookupManager.getInstance().getCPAObject(CPAObjectType.CHANNEL, cid);
                if (basicXSDFolderPath == null) {
                    TRACE.debugT(SIGNATURE, "Mode parameter is not set. Switch to '' as default.");
                    basicXSDFolderPath = "";
                    ModuleException me = new ModuleException("EDItoXML Error: No schema path!");
                    TRACE.throwing(SIGNATURE, me);
                    throw me;
                }
                TRACE.debugT(SIGNATURE, "Mode is set to {0}", new Object[] { basicXSDFolderPath });
                if (defaultXSDFolder == null) {
                    TRACE.debugT(SIGNATURE, "EDItoXML Error: No Default Folder!");
                    basicXSDFolderPath = "";
                    ModuleException me = new ModuleException("EDItoXML Error: No Default Folder!");
                    TRACE.throwing(SIGNATURE, me);
                    throw me;
                }
                TRACE.debugT(SIGNATURE, "Mode is set to {0}", new Object[] { defaultXSDFolder });
                if (exceptionFlag == null) {
                    TRACE.debugT(SIGNATURE, "Exception flag parameter is not set.");
                    exceptionFlag = "";
                    ModuleException me = new ModuleException("EDIValidate Error: No exception flag is set!");
                    TRACE.throwing(SIGNATURE, me);
                    throw me;
                }
                TRACE.debugT(SIGNATURE, "The XSD folder path is set to {0}", new Object[] { basicXSDFolderPath });
                if (originalMessageFlag == null) {
                    TRACE.debugT(SIGNATURE, "Original Message Flag parameter is not set.");
                    originalMessageFlag = "";
                    ModuleException me = new ModuleException("EDIValidate Error: No Original Message Flag is set!");
                    TRACE.throwing(SIGNATURE, me);
                    throw me;
                }
                TRACE.debugT(SIGNATURE, "The XSD folder path is set to {0}", new Object[] { basicXSDFolderPath });
                if (encoding == null) {
                    encoding = "";
                }
            } catch (Exception e) {
                TRACE.catching(SIGNATURE, e);
                TRACE.errorT(SIGNATURE, "Cannot read the module context and configuration data");
                ModuleException me = new ModuleException("EDItoXML process() Error: " + e.getMessage(), e);
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
        }
        sbXMLPayload = new StringBuffer();
        if (basicXSDFolderPath.compareToIgnoreCase("noXSDFileNameSpecified") == 0) {
            TRACE.debugT(SIGNATURE, "Bypass EDI to XML conversion since 'schemaFileName' parameter was set to 'noXSDFileNameSpecified'.");
        } else {
            try {
                XMLPayload xmlpayload = msg.getDocument();
                if (xmlpayload != null) {
                    sbXMLPayload.append(new String(xmlpayload.getContent(), "ISO-8859-1"));
                    sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("\r\n", ""));
                    sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("\r", ""));
                    sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&apos;", "'"));
                    sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&quot;", "\""));
                    sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&gt;", ">"));
                    sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&lt;", "<"));
                    sbXMLPayload = new StringBuffer(sbXMLPayload.toString().replaceAll("&amp;", "&"));
                    if (validateFlag.equals("true")) {
                        try {
                            DataProcessor converter = new DataProcessor(sbXMLPayload, basicXSDFolderPath, defaultXSDFolder, exceptionFlag, originalMessageFlag, encoding, new XITrace(TRACE));
                            xmlpayload.setContent(converter.convertAndValidate().toString().getBytes("ISO-8859-1"));
                        } catch (EDIException e) {
                            throw new ModuleException(e.getMessage(), e.getCause());
                        }
                    } else {
                        try {
                            DataProcessor converter = new DataProcessor(sbXMLPayload, basicXSDFolderPath, defaultXSDFolder, exceptionFlag, originalMessageFlag, encoding, new XITrace(TRACE));
                            xmlpayload.setContent(converter.convert().toString().getBytes("ISO-8859-1"));
                        } catch (EDIException e) {
                            throw new ModuleException(e.getMessage(), e.getCause());
                        }
                    }
                }
                inputModuleData.setPrincipalData(msg);
                TRACE.debugT(SIGNATURE, "EDIValidate conversion finished sucessfully.");
            } catch (Exception e) {
                TRACE.catching(SIGNATURE, e);
                TRACE.errorT(SIGNATURE, "Cannot convert one of the payloads. Reason: {0}", new Object[] { e.getMessage() });
                ModuleException me = new ModuleException(new ModuleException("MessageKey: " + mk.toString() + " EDIValidate Error: " + e.getMessage()));
                TRACE.throwing(SIGNATURE, me);
                throw me;
            }
        }
        TRACE.exiting(SIGNATURE);
        return inputModuleData;
    }
}
