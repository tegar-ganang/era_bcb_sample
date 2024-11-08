package org.primordion.xholon.service;

import org.primordion.xholon.base.IMessage;
import org.primordion.xholon.base.ISignal;
import org.primordion.xholon.base.IXholon;
import org.primordion.xholon.base.IXholonClass;
import org.primordion.xholon.base.Message;
import org.primordion.xholon.exception.XholonConfigurationException;
import org.primordion.xholon.io.console.IXholonConsole;
import org.primordion.xholon.io.webbrowser.IWebBrowser;
import org.primordion.xholon.io.xml.IXholon2Xml;
import org.primordion.xholon.io.xml.IXml2XholonClass;
import org.primordion.xholon.io.xmldb.IXmlDb;
import org.primordion.xholon.io.xmldb.XmlDbFactory;
import org.primordion.xholon.io.xquerygui.IXQueryGui;
import org.primordion.xholon.service.xholonhelper.IClipboard;
import org.primordion.xholon.service.xholonhelper.ClipboardFactory;
import org.primordion.xholon.service.xholonhelper.ICutCopyPaste;
import org.primordion.xholon.service.xholonhelper.IFindChildSibWith;

/**
 * This service provides additional methods for IXholon instances.
 * Most of these methods were originally part of the IXholon interface and Xholon class themselves,
 * but were moved here when IXholon and Xholon became too bloated.
 * <ul>
 * <li>Copy/Cut/Paste to/from Clipboard</li>
 * <li>XQuery thru Clipboard</li>
 * <li></li>
 * </ul>
 * The service methods can be invoked in either of two ways.
 * If invoked from some other thread, such as when invoked by the XholonGui,
 * then it should be done in a thread-safe manner by sending a synchronous message.
 * This will cause the request to be placed on the system queue to be actioned next timestep.
 * For example:
 * <pre>xholonHelperService.sendSyncMessage(
 * ISignal.ACTION_COPY_TOCLIPBOARD, someNode, this);</pre>
 * If invoked from the same thread, then it's ok to directly call the public methods, for example:
 * <pre>xholonHelperService.doCopyToClipboard(someNode);</pre>
 * <p>TODO the class should implement one or more interfaces</p>
 * <p>TODO move the signals out of ISignal</p>
 * <p>TODO copyStringToClipboard() vs. writeStringToClipboard() ?</p>
 * @author <a href="mailto:ken@primordion.com">Ken Webb</a>
 * @see <a href="http://www.primordion.com/Xholon">Xholon Project website</a>
 * @since 0.8 (Created on September 18, 2009)
 */
public class XholonHelperService extends AbstractXholonService implements ICutCopyPaste, IFindChildSibWith {

    public static final int SIG_XHOLON_HELPER_RESP = ISignal.SIGNAL_MIN_XHOLON_SERVICE + 102;

    /**
	 * Name of the default Java class that implements ICutCopyPaste.
	 */
    private static final String DEFAULT_CUTCOPYPASTE_IMPL_NAME = "org.primordion.xholon.service.xholonhelper.CutCopyPaste";

    /**
	 * The Java insatance that implements ICutCopyPaste.
	 */
    private ICutCopyPaste cutCopyPasteInstance = null;

    /**
	 * Name of the Java class that implements ICutCopyPaste.
	 */
    private String cutCopyPasteImplName = null;

    /**
	 * Process a system message that was sent in the previous timestep, by self or by another xholon.
	 * @see org.primordion.xholon.base.Xholon#processReceivedMessage(org.primordion.xholon.base.IMessage)
	 */
    public void processReceivedMessage(IMessage msg) {
        switch(msg.getSignal()) {
            case ISignal.ACTION_COPY_TOCLIPBOARD:
                if (msg.getData() instanceof String) {
                    writeStringToClipboard((String) msg.getData());
                } else {
                    writeStringToClipboard(copySelf((IXholon) msg.getData()));
                }
                break;
            case ISignal.ACTION_CUT_TOCLIPBOARD:
                writeStringToClipboard(cutSelf((IXholon) msg.getData()));
                break;
            case ISignal.ACTION_PASTE_LASTCHILD_FROMCLIPBOARD:
                pasteLastChild((IXholon) msg.getData(), readStringFromClipboard());
                break;
            case ISignal.ACTION_PASTE_FIRSTCHILD_FROMCLIPBOARD:
                pasteFirstChild((IXholon) msg.getData(), readStringFromClipboard());
                break;
            case ISignal.ACTION_PASTE_AFTER_FROMCLIPBOARD:
                pasteAfter((IXholon) msg.getData(), readStringFromClipboard());
                break;
            case ISignal.ACTION_PASTE_BEFORE_FROMCLIPBOARD:
                pasteBefore((IXholon) msg.getData(), readStringFromClipboard());
                break;
            case ISignal.ACTION_PASTE_REPLACEMENT_FROMCLIPBOARD:
                pasteReplacement((IXholon) msg.getData(), readStringFromClipboard());
                break;
            case ISignal.ACTION_PASTE_MERGE_FROMCLIPBOARD:
                pasteMerge((IXholon) msg.getData(), readStringFromClipboard());
                break;
            default:
                super.processReceivedMessage(msg);
        }
    }

    /**
	 * Process a synchronous message set by XholonGui, or by any other class running in another thread.
	 * @see org.primordion.xholon.base.Xholon#processReceivedSyncMessage(org.primordion.xholon.base.IMessage)
	 */
    public IMessage processReceivedSyncMessage(IMessage msg) {
        switch(msg.getSignal()) {
            case ISignal.ACTION_COPY_TOCLIPBOARD:
                if (msg.getData() instanceof String) {
                    writeStringToClipboard((String) msg.getData());
                } else {
                    copyToClipboard((IXholon) msg.getData());
                }
                break;
            case ISignal.ACTION_CUT_TOCLIPBOARD:
                cutToClipboard((IXholon) msg.getData());
                break;
            case ISignal.ACTION_PASTE_LASTCHILD_FROMCLIPBOARD:
                pasteLastChildFromClipboard((IXholon) msg.getData());
                break;
            case ISignal.ACTION_PASTE_FIRSTCHILD_FROMCLIPBOARD:
                pasteFirstChildFromClipboard((IXholon) msg.getData());
                break;
            case ISignal.ACTION_PASTE_AFTER_FROMCLIPBOARD:
                pasteAfterFromClipboard((IXholon) msg.getData());
                break;
            case ISignal.ACTION_PASTE_BEFORE_FROMCLIPBOARD:
                pasteBeforeFromClipboard((IXholon) msg.getData());
                break;
            case ISignal.ACTION_PASTE_REPLACEMENT_FROMCLIPBOARD:
                pasteReplacementFromClipboard((IXholon) msg.getData());
                break;
            case ISignal.ACTION_XQUERY_THRUCLIPBOARD:
                writeStringToClipboard(doXQuery((IXholon) msg.getData(), readStringFromClipboard(), IXholon2Xml.XHATTR_TO_NULL, false, false));
                break;
            case ISignal.ACTION_PASTE_MERGE_FROMCLIPBOARD:
                pasteMergeFromClipboard((IXholon) msg.getData());
                break;
            case ISignal.ACTION_START_XQUERY_GUI:
                xqueryGui((IXholon) msg.getData());
                break;
            case ISignal.ACTION_START_XHOLON_CONSOLE:
                xholonConsole((IXholon) msg.getData());
                break;
            case ISignal.ACTION_START_WEB_BROWSER:
                webBrowser((IXholon) msg.getData());
                break;
            case ISignal.ACTION_PASTE_LASTCHILD_FROMSTRING:
                pasteLastChild(msg.getSender(), (String) msg.getData());
                break;
            case ISignal.ACTION_PASTE_FIRSTCHILD_FROMSTRING:
                pasteFirstChild(msg.getSender(), (String) msg.getData());
                break;
            case ISignal.ACTION_PASTE_AFTER_FROMSTRING:
                pasteAfter(msg.getSender(), (String) msg.getData());
                break;
            case ISignal.ACTION_PASTE_BEFORE_FROMSTRING:
                pasteBefore(msg.getSender(), (String) msg.getData());
                break;
            case ISignal.ACTION_PASTE_REPLACEMENT_FROMSTRING:
                pasteReplacement(msg.getSender(), (String) msg.getData());
                break;
            case ISignal.ACTION_PASTE_MERGE_FROMSTRING:
                pasteMerge(msg.getSender(), (String) msg.getData());
                break;
            case ISignal.ACTION_PASTE_LASTCHILD_FROMDROP:
                pasteLastChildFromDrop(msg.getSender(), (String) msg.getData());
                break;
            case ISignal.ACTION_PASTE_FIRSTCHILD_FROMDROP:
                pasteFirstChildFromDrop(msg.getSender(), (String) msg.getData());
                break;
            case ISignal.ACTION_PASTE_AFTER_FROMDROP:
                pasteAfterFromDrop(msg.getSender(), (String) msg.getData());
                break;
            default:
                return super.processReceivedSyncMessage(msg);
        }
        return new Message(SIG_XHOLON_HELPER_RESP, null, this, msg.getSender());
    }

    public IXholon getService(String serviceName) {
        if (serviceName.equals(getXhcName())) {
            return this;
        } else {
            return null;
        }
    }

    public IXholon findFirstChildWithXhClass(IXholon contextNode, int childXhClassId) {
        IXholon node = contextNode.getFirstChild();
        while (node != null) {
            int foundXhcId = node.getXhcId();
            if (foundXhcId == childXhClassId) {
                return node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    public IXholon findFirstChildWithXhClass(IXholon contextNode, String childXhClassName) {
        if (childXhClassName == null) {
            return null;
        }
        IXholon node = contextNode.getFirstChild();
        while (node != null) {
            String foundXhcName = node.getXhcName();
            if (childXhClassName.equals(foundXhcName)) {
                return node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    public IXholon findFirstChildWithAncestorXhClass(IXholon contextNode, int childXhClassId) {
        IXholon node = contextNode.getFirstChild();
        while (node != null) {
            if (node.getXhc().hasAncestor(childXhClassId)) {
                return node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    public IXholon findFirstChildWithRoleName(IXholon contextNode, String roleName) {
        if (roleName == null) {
            return null;
        }
        IXholon node = contextNode.getFirstChild();
        while (node != null) {
            if (roleName.equals(node.getRoleName())) {
                return node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    public IXholon findNextSiblingWithXhClass(IXholon contextNode, int siblingXhClassId) {
        IXholon node = contextNode.getNextSibling();
        while (node != null) {
            if (node.getXhcId() == siblingXhClassId) {
                return node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    public IXholon findNextSiblingWithAncestorXhClass(IXholon contextNode, int siblingXhClassId) {
        IXholon node = contextNode.getNextSibling();
        while (node != null) {
            if (node.getXhc().hasAncestor(siblingXhClassId)) {
                return node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    /**
	 * Get the instance of ICutCopyPaste.
	 * @return An instance, or null.
	 */
    protected ICutCopyPaste cutCopyPasteInstance() {
        if (cutCopyPasteInstance == null) {
            IXholonClass ccpXholonClass = null;
            if (cutCopyPasteImplName == null) {
                ccpXholonClass = getApp().getClassNode("CutCopyPaste");
                if (ccpXholonClass == null) {
                    cutCopyPasteImplName = DEFAULT_CUTCOPYPASTE_IMPL_NAME;
                    try {
                        ccpXholonClass = getApp().getFactory().getXholonClassNode();
                        ccpXholonClass.setId(getApp().getNextXholonClassId());
                        ccpXholonClass.setApp(getApp());
                        ccpXholonClass.setMechanism("CutCopyPaste", IXml2XholonClass.MECHANISM_DEFAULT_NAMESPACEURI, "", ccpXholonClass.getId());
                        ccpXholonClass.setXhType(IXholonClass.XhtypePureActiveObject);
                        ccpXholonClass.setName("CutCopyPaste");
                        ccpXholonClass.setImplName(DEFAULT_CUTCOPYPASTE_IMPL_NAME);
                    } catch (XholonConfigurationException e) {
                    }
                } else {
                    cutCopyPasteImplName = ccpXholonClass.getImplName();
                }
            }
            if (cutCopyPasteImplName == null) {
                cutCopyPasteImplName = DEFAULT_CUTCOPYPASTE_IMPL_NAME;
            }
            cutCopyPasteInstance = (ICutCopyPaste) createInstance(cutCopyPasteImplName);
            cutCopyPasteInstance.setId(getNextId());
            cutCopyPasteInstance.setXhc(ccpXholonClass);
            cutCopyPasteInstance.appendChild(this);
        }
        return cutCopyPasteInstance;
    }

    public void copyToClipboard(IXholon node) {
        cutCopyPasteInstance().copyToClipboard(node);
    }

    public String copySelf(IXholon node) {
        return cutCopyPasteInstance().copySelf(node);
    }

    public void cutToClipboard(IXholon node) {
        cutCopyPasteInstance().cutToClipboard(node);
    }

    public String cutSelf(IXholon node) {
        return cutCopyPasteInstance().cutSelf(node);
    }

    public void pasteLastChildFromClipboard(IXholon node) {
        cutCopyPasteInstance().pasteLastChildFromClipboard(node);
    }

    public void pasteLastChildFromDrop(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteLastChildFromDrop(node, xmlString);
    }

    public void pasteLastChild(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteLastChild(node, xmlString);
    }

    public void pasteFirstChildFromClipboard(IXholon node) {
        cutCopyPasteInstance().pasteFirstChildFromClipboard(node);
    }

    public void pasteFirstChildFromDrop(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteFirstChildFromDrop(node, xmlString);
    }

    public void pasteFirstChild(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteFirstChild(node, xmlString);
    }

    public void pasteAfterFromClipboard(IXholon node) {
        cutCopyPasteInstance().pasteAfterFromClipboard(node);
    }

    public void pasteAfterFromDrop(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteAfterFromDrop(node, xmlString);
    }

    public void pasteAfter(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteAfter(node, xmlString);
    }

    public void pasteBeforeFromClipboard(IXholon node) {
        cutCopyPasteInstance().pasteBeforeFromClipboard(node);
    }

    public void pasteBefore(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteBefore(node, xmlString);
    }

    public void pasteReplacementFromClipboard(IXholon node) {
        cutCopyPasteInstance().pasteReplacementFromClipboard(node);
    }

    public void pasteReplacement(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteReplacement(node, xmlString);
    }

    public void pasteMergeFromClipboard(IXholon node) {
        cutCopyPasteInstance().pasteMergeFromClipboard(node);
    }

    public void pasteMerge(IXholon node, String xmlString) {
        cutCopyPasteInstance().pasteMerge(node, xmlString);
    }

    /**
	 * Read a String from the clipboard, such as an XML or XQuery String.
	 * @return A String, or null.
	 */
    protected String readStringFromClipboard() {
        IClipboard xholonClipboard = ClipboardFactory.instance();
        if (xholonClipboard != null) {
            return xholonClipboard.readStringFromClipboard();
        }
        return null;
    }

    /**
	 * Write a String to the clipboard.
	 * @param str A String.
	 */
    protected void writeStringToClipboard(String str) {
        IClipboard xholonClipboard = ClipboardFactory.instance();
        if (xholonClipboard != null) {
            xholonClipboard.writeStringToClipboard(str);
        }
    }

    /**
	 * Do an XQuery operation.
	 * @param context The Xholon node that the XQueryGui node is associated with.
	 * @param xqueryString ex: return (count($x//Creature) , count($x//Monster))
	 * @param xhAttrStyle How to transform Xholon/Java attributes into XML.
	 * @param writeXholonId Should the Xholon ID be written out as an XML attribute for each node.
	 * @param writePorts Should ports be written out for each node.
	 * @return Result of the query, as a String.
	 */
    public String doXQuery(IXholon context, String xqueryString, int xhAttrStyle, boolean writeXholonId, boolean writePorts) {
        String returnStr = "";
        if ((xqueryString == null) || (xqueryString.equals(""))) {
            return returnStr;
        }
        if (xqueryString.startsWith("insert ")) {
            return doXufInsert(context, xqueryString);
        } else if (xqueryString.startsWith("delete ")) {
            return doXufDelete(context, xqueryString);
        } else if (xqueryString.startsWith("replace ")) {
            return doXufReplace(context, xqueryString);
        } else if (xqueryString.startsWith("rename ")) {
            return doXufRename(context, xqueryString);
        } else if (xqueryString.startsWith("copy ")) {
            return doXufCopy(context, xqueryString);
        }
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.charAt(2) <= '4') {
            return "Java version must be 1.5 or greater.";
        }
        IXholon2Xml xholon2Xml = getXholon2Xml();
        xholon2Xml.setXhAttrStyle(xhAttrStyle);
        xholon2Xml.setWriteStartDocument(false);
        xholon2Xml.setWriteXholonId(writeXholonId);
        xholon2Xml.setWritePorts(writePorts);
        String xmlString = xholon2Xml.xholon2XmlString(context);
        String xqueryStringComplete = "let $x := " + xmlString + " " + xqueryString;
        IXmlDb xmlDb = XmlDbFactory.getSingleton();
        if (xmlDb != null) {
            returnStr = xmlDb.xqueryXqj(null, null, xqueryStringComplete);
        }
        return returnStr;
    }

    /**
	 * Do XQuery Update Facility insert.
	 * Examples:
	 * <pre>
	 * insert node &lt;HelloWorldSystem/&gt; as last into .
	 * insert node &lt;HelloWorldSystem/&gt; as first into .
	 * insert node &lt;HelloWorldSystem/&gt; after .
	 * insert node &lt;HelloWorldSystem/&gt; before .
	 * </pre>
	 * @param context The Xholon node that the XQueryGui node is associated with.
	 * @param xqueryString An XQuery Update Facility insert expression (simplified for Xholon).
	 * @return An informative result String.
	 */
    protected String doXufInsert(IXholon context, String xqueryString) {
        int sourceExprBeginIndex = xqueryString.indexOf('<');
        if (sourceExprBeginIndex == -1) {
            return "error: no <";
        }
        int sourceExprEndIndex = xqueryString.lastIndexOf('>');
        if (sourceExprEndIndex == -1) {
            return "error: no >";
        }
        sourceExprEndIndex++;
        String sourceExpr = xqueryString.substring(sourceExprBeginIndex, sourceExprEndIndex);
        sourceExprEndIndex++;
        String insertExprTargetChoice = xqueryString.substring(sourceExprEndIndex).trim();
        if (insertExprTargetChoice.startsWith("as last into")) {
            pasteLastChild(context, sourceExpr);
        } else if (insertExprTargetChoice.startsWith("as first into")) {
            pasteFirstChild(context, sourceExpr);
        } else if (insertExprTargetChoice.startsWith("after")) {
            pasteAfter(context, sourceExpr);
        } else if (insertExprTargetChoice.startsWith("before")) {
            pasteBefore(context, sourceExpr);
        }
        return "'insert' completed";
    }

    /**
	 * Do XQuery Update Facility delete.
	 * Examples:
	 * <pre>
	 * delete node .
	 * </pre>
	 * @param xqueryString An XQuery Update Facility delete expression (simplified for Xholon).
	 * @return An informative result String.
	 */
    protected String doXufDelete(IXholon context, String xqueryString) {
        removeChild();
        return "'delete ' completed";
    }

    /**
	 * Do XQuery Update Facility replace.
	 * Examples:
	 * <pre>
	 * replace node . with &lt;HelloWorldSystem/>
	 * replace value of node . with &lt;HelloWorldSystem/>
	 * </pre>
	 * @param xqueryString An XQuery Update Facility replace expression (simplified for Xholon).
	 * @return An informative result String.
	 */
    protected String doXufReplace(IXholon context, String xqueryString) {
        return "'replace ' is not yet implemented";
    }

    /**
	 * Do XQuery Update Facility rename.
	 * Examples:
	 * <pre>
	 * rename node .
	 * </pre>
	 * @param xqueryString An XQuery Update Facility rename expression (simplified for Xholon).
	 * @return An informative result String.
	 */
    protected String doXufRename(IXholon context, String xqueryString) {
        return "'rename ' is not yet implemented";
    }

    /**
	 * Do XQuery Update Facility copy.
	 * Examples:
	 * <pre>
	 * copy node .
	 * </pre>
	 * @param xqueryString An XQuery Update Facility copy expression (simplified for Xholon).
	 * @return An informative result String.
	 */
    protected String doXufCopy(IXholon context, String xqueryString) {
        return "'copy ' is not yet implemented";
    }

    /**
	 * Start the XQuery GUI.
	 * @param context 
	 */
    public void xqueryGui(IXholon context) {
        String className = "org.primordion.xholon.io.xquerygui.XQueryGui";
        IXQueryGui xqueryGui = null;
        try {
            xqueryGui = (IXQueryGui) Class.forName(className).newInstance();
        } catch (InstantiationException e) {
            logger.error("Unable to instantiate optional XQueryGui class {}", e);
        } catch (IllegalAccessException e) {
            logger.error("Unable to load optional XQueryGui class {}", e);
        } catch (ClassNotFoundException e) {
            logger.error("Unable to load optional XQueryGui class " + className);
        } catch (NoClassDefFoundError e) {
            logger.error("Unable to load optional XQueryGui class {}", e);
        }
        if (xqueryGui != null) {
            xqueryGui.setApp(getApp());
            xqueryGui.setContext(context);
            xqueryGui.postConfigure();
        }
    }

    /**
	 * Start the Xholon Console.
	 * @param context 
	 */
    public void xholonConsole(IXholon context) {
        String className = "org.primordion.xholon.io.console.XholonConsole";
        IXholonConsole xholonConsole = null;
        try {
            xholonConsole = (IXholonConsole) Class.forName(className).newInstance();
        } catch (InstantiationException e) {
            logger.error("Unable to instatiate optional XholonConsole class {}", e);
        } catch (IllegalAccessException e) {
            logger.error("Unable to load optional XholonConsole class {}", e);
        } catch (ClassNotFoundException e) {
            logger.error("Unable to load optional XholonConsole class " + className);
        } catch (NoClassDefFoundError e) {
            logger.error("Unable to load optional XholonConsole class {}", e);
        }
        if (xholonConsole != null) {
            xholonConsole.setApp(getApp());
            xholonConsole.setContext(context);
            xholonConsole.postConfigure();
        }
    }

    /**
	 * Start the Xholon Web Browser.
	 * @param context 
	 */
    public void webBrowser(IXholon context) {
        String className = "org.primordion.xholon.io.webbrowser.WebBrowser";
        IWebBrowser webBrowser = null;
        try {
            webBrowser = (IWebBrowser) Class.forName(className).newInstance();
        } catch (InstantiationException e) {
            logger.error("Unable to instatiate optional WebBrowser class {}", e);
        } catch (IllegalAccessException e) {
            logger.error("Unable to load optional WebBrowser class {}", e);
        } catch (ClassNotFoundException e) {
            logger.error("Unable to load optional WebBrowser class " + className);
        } catch (NoClassDefFoundError e) {
            logger.error("Unable to load optional WebBrowser class {}", e);
        }
        if (webBrowser != null) {
            webBrowser.setApp(getApp());
            webBrowser.setContext(context);
            webBrowser.postConfigure();
        }
    }
}
