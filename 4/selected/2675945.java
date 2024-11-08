package com.quikj.application.web.talk.messaging;

import java.util.Enumeration;
import net.n3.nanoxml.IXMLElement;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DisconnectMessage implements TalkMessageInterface {

    public static final String MESSAGE_TYPE = "disconnect_request";

    private long sessionId = -1;

    private String errorMessage = "";

    private DisconnectReasonElement reason = null;

    private CalledNameElement calledInfo = null;

    private String transferId = null;

    private String transferFrom = null;

    private boolean transcript = false;

    public DisconnectMessage() {
    }

    public String format() {
        StringBuffer buffer = new StringBuffer("<disconnect session=\"" + sessionId + "\"");
        if (transferId != null) {
            buffer.append(" transfer=\"" + TalkMessageParser.encodeXMLString(transferId) + "\"");
        }
        if (transferFrom != null) {
            buffer.append(" transfer-from=\"" + TalkMessageParser.encodeXMLString(transferFrom) + "\"");
        }
        if (transcript == true) {
            buffer.append(" transcript=\"yes\"");
        }
        buffer.append(">\n");
        if (reason != null) {
            buffer.append(reason.format());
        }
        if (calledInfo != null) {
            buffer.append(calledInfo.format());
        }
        buffer.append("</disconnect>" + '\n');
        return buffer.toString();
    }

    public CalledNameElement getCalledInfo() {
        return calledInfo;
    }

    public DisconnectReasonElement getDisconnectReason() {
        return reason;
    }

    public String getErrorMessage() {
        return errorMessage;
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

    /**
	 * Getter for property transcript.
	 * 
	 * @return Value of property transcript.
	 * 
	 */
    public boolean isTranscript() {
        return transcript;
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
        } else if (name.equals("transcript") == true) {
            if (value.equals("yes") == true) {
                transcript = true;
            } else if (value.equals("no") == true) {
                transcript = false;
            } else {
                errorMessage = "The transcript parameter has invalid value " + value;
                return false;
            }
        }
        return true;
    }

    public void setCalledInfo(CalledNameElement element) {
        calledInfo = element;
    }

    public void setDisconnectReason(DisconnectReasonElement reason) {
        this.reason = reason;
    }

    private boolean setElement(String name, Object element) {
        if (name.equals("reason") == true) {
            DisconnectReasonElement r_element = new DisconnectReasonElement();
            if (r_element.parse(element) == false) {
                errorMessage = new String(r_element.getErrorMessage());
                return false;
            }
            reason = r_element;
        } else if (name.equals("called") == true) {
            CalledNameElement cname_element = new CalledNameElement();
            if (cname_element.parse(element) == false) {
                errorMessage = cname_element.getErrorMessage();
                return false;
            }
            calledInfo = cname_element;
        }
        return true;
    }

    public void setSessionId(long id) {
        sessionId = id;
    }

    /**
	 * Setter for property transcript.
	 * 
	 * @param transcript
	 *            New value of property transcript.
	 * 
	 */
    public void setTranscript(boolean transcript) {
        this.transcript = transcript;
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
        if (sessionId == -1) {
            errorMessage = "Session id is not specified";
            return false;
        }
        return true;
    }
}
