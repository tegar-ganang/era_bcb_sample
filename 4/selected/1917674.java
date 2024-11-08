package whisper;

import gui.TextStyle;
import javax.management.modelmbean.XMLParseException;
import models.responses.Response;
import models.responses.Responses;
import cc.slx.java.xml.XMLDocument;

/**
 * This class is responsible for parsing a response.
 * 
 * @author Thomas Pedley
 */
public class ResponseParser {

    /** The chat client containing the connection to send responses over. */
    private Whisper chat;

    /**
	 * Constructor.
	 * 
	 * @param chat The chat client containing the connection to send responses over.
	 */
    public ResponseParser(Whisper chat) {
        this.chat = chat;
    }

    /**
	 * Parse the incoming response.
	 * 
	 * @param response The incoming response.
	 */
    public void parse(String response) {
        if (Whisper.isDebugging()) {
            TextStyle.addInformationalText(response, Whisper.getClient().getChannel(Whisper.TAB_RAW));
        }
        XMLDocument xDoc = null;
        try {
            xDoc = XMLDocument.parse(response);
        } catch (XMLParseException ex) {
            if (Whisper.isDebugging()) {
                ex.printStackTrace();
            }
        }
        if (xDoc != null) {
            String reply = xDoc.getRootNode().getChild("Reply").getValue();
            if (reply != null) {
                Response r = Responses.InstantiateResponse(reply);
                if (r != null) {
                    r.action(xDoc, chat.getConnection());
                }
            }
        }
    }
}
