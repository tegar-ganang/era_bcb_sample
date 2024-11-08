package com.quikj.application.web.talk.messaging;

import java.util.Enumeration;
import net.n3.nanoxml.IXMLElement;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SetupRequestMessage implements TalkMessageInterface {

    public static final String MESSAGE_TYPE = "setup_request";

    private CallingNameElement calling = null;

    private CalledNameElement called = null;

    private long sessionId = -1;

    private String transferId = null;

    private String transferFrom = null;

    private String errorMessage = "";

    private String encryptedKey = null;

    private MediaElements media = null;

    public SetupRequestMessage() {
    }

    public String format() {
        StringBuffer buffer = new StringBuffer("<?xml version=\"1.0\" encoding=\"us-ascii\"?>" + '\n' + "<setup");
        if (sessionId != -1) {
            buffer.append(" session=\"" + sessionId + "\"");
        }
        if (transferId != null) {
            buffer.append(" transfer=\"" + TalkMessageParser.encodeXMLString(transferId) + "\"");
        }
        if (transferFrom != null) {
            buffer.append(" transfer-from=\"" + TalkMessageParser.encodeXMLString(transferFrom) + "\"");
        }
        if (encryptedKey != null) {
            buffer.append(" key=\"" + TalkMessageParser.encodeXMLString(encryptedKey) + "\"");
        }
        buffer.append(">\n");
        if (calling != null) {
            buffer.append(calling.format());
        }
        if (called != null) {
            buffer.append(called.format());
        }
        if (media != null) {
            buffer.append("<media>\n" + media.format() + "</media>\n");
        }
        buffer.append("</setup>\n");
        return buffer.toString();
    }

    public CalledNameElement getCalledNameElement() {
        return called;
    }

    public CallingNameElement getCallingNameElement() {
        return calling;
    }

    /**
	 * Getter for property encryptedKey.
	 * 
	 * @return Value of property encryptedKey.
	 */
    public java.lang.String getEncryptedKey() {
        return encryptedKey;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
	 * Getter for property media.
	 * 
	 * @return Value of property media.
	 * 
	 */
    public com.quikj.application.web.talk.messaging.MediaElements getMedia() {
        return media;
    }

    public long getSessionId() {
        return sessionId;
    }

    /**
	 * Getter for property transferFrom.
	 * 
	 * @return Value of property transferFrom.
	 * 
	 */
    public java.lang.String getTransferFrom() {
        return transferFrom;
    }

    /**
	 * Getter for property transferId.
	 * 
	 * @return Value of property transferId.
	 */
    public java.lang.String getTransferId() {
        return transferId;
    }

    public String messageType() {
        return MESSAGE_TYPE;
    }

    public boolean parse(Object node) {
        if (TalkMessageParser.getParserType() == TalkMessageParser.DOM_PARSER) {
            return parseDOM((Node) node);
        } else {
            return parseNANO((IXMLElement) node);
        }
    }

    private boolean parseDOM(Node node) {
        NamedNodeMap attribs = node.getAttributes();
        int length = attribs.getLength();
        for (int i = 0; i < length; i++) {
            Node attr = attribs.item(i);
            if (setAttribute(attr.getNodeName(), attr.getNodeValue()) == false) {
                return false;
            }
        }
        Node element = null;
        for (element = node.getFirstChild(); element != null; element = element.getNextSibling()) {
            if (element.getNodeType() == Node.ELEMENT_NODE) {
                String name = element.getNodeName();
                if (setElement(name, element) == false) {
                    return false;
                }
            }
        }
        return validateParameters();
    }

    private boolean parseNANO(IXMLElement node) {
        Enumeration e = node.enumerateAttributeNames();
        while (e.hasMoreElements() == true) {
            String name = (String) e.nextElement();
            String value = node.getAttribute(name, null);
            if (setAttribute(name, value) == false) {
                return false;
            }
        }
        e = node.enumerateChildren();
        while (e.hasMoreElements() == true) {
            IXMLElement child = (IXMLElement) e.nextElement();
            String name = child.getFullName();
            if (name == null) {
                continue;
            }
            if (setElement(name, child) == false) {
                return false;
            }
        }
        return validateParameters();
    }

    private boolean setAttribute(String name, String value) {
        if (name.equals("session") == true) {
            try {
                sessionId = Long.parseLong(value);
            } catch (NumberFormatException ex) {
                errorMessage = "The session id is not an integer";
                return false;
            }
        } else if (name.equals("transfer") == true) {
            transferId = value;
        } else if (name.equals("transfer-from") == true) {
            transferFrom = value;
        } else if (name.equals("key") == true) {
            encryptedKey = value;
        }
        return true;
    }

    public void setCalledNameElement(CalledNameElement called) {
        this.called = called;
    }

    public void setCallingNameElement(CallingNameElement calling) {
        this.calling = calling;
    }

    private boolean setElement(String name, Object element) {
        if (name.equals("calling") == true) {
            CallingNameElement calling_name = new CallingNameElement();
            if (calling_name.parse(element) == true) {
                calling = calling_name;
            } else {
                errorMessage = calling_name.getErrorMessage();
                return false;
            }
        } else if (name.equals("called") == true) {
            CalledNameElement called_name = new CalledNameElement();
            if (called_name.parse(element) == true) {
                called = called_name;
            } else {
                errorMessage = called_name.getErrorMessage();
                return false;
            }
        } else if (name.equals("media") == true) {
            MediaElements r_media = new MediaElements();
            if (r_media.parse(element) == false) {
                errorMessage = r_media.getErrorMessage();
                return false;
            }
            media = r_media;
        }
        return true;
    }

    /**
	 * Setter for property encryptedKey.
	 * 
	 * @param encryptedKey
	 *            New value of property encryptedKey.
	 */
    public void setEncryptedKey(java.lang.String encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    /**
	 * Setter for property media.
	 * 
	 * @param media
	 *            New value of property media.
	 * 
	 */
    public void setMedia(com.quikj.application.web.talk.messaging.MediaElements media) {
        this.media = media;
    }

    public void setSessionId(long id) {
        sessionId = id;
    }

    /**
	 * Setter for property transferFrom.
	 * 
	 * @param transferFrom
	 *            New value of property transferFrom.
	 * 
	 */
    public void setTransferFrom(java.lang.String transferFrom) {
        this.transferFrom = transferFrom;
    }

    /**
	 * Setter for property transferId.
	 * 
	 * @param transferId
	 *            New value of property transferId.
	 */
    public void setTransferId(java.lang.String transferId) {
        this.transferId = transferId;
    }

    private boolean validateParameters() {
        if ((calling == null) && (called == null)) {
            errorMessage = "The calling and/or called name element has not been specified";
            return false;
        }
        return true;
    }
}
