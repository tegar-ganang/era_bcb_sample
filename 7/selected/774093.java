package bg.obs.internal.jnetplayer.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import bg.obs.internal.jnetplayer.message.BaseMessage;
import bg.obs.internal.jnetplayer.message.Message;

/**
 * @author hmasrafchi
 * 
 */
public class PlainTextProtocol extends AbstractJNPProtocol {

    private static final Log log = LogFactory.getLog(PlainTextProtocol.class);

    /**
     * Default constructor.
     */
    public PlainTextProtocol() {
    }

    protected void resolveMessage() {
        String[] lines = message.split(LINE_SEPARATOR);
        header = lines[0];
        if (lines.length > 1) {
            bodyEntities = new String[lines.length - 1];
            for (int i = 0; i < lines.length - 1; i++) {
                bodyEntities[i] = lines[i + 1];
            }
        }
    }

    public Message parseMessage(String receivedString) {
        setMessage(receivedString);
        return new BaseMessage(MessageHeader.findValue(header), bodyEntities);
    }

    public boolean validateMessage(Message message) {
        return message != null && message.getHeader() != null;
    }

    public String wrapMessage(Message message) {
        if (validateMessage(message)) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(message.getHeader());
            buffer.append(JNPProtocol.LINE_SEPARATOR);
            if (message.getBody() != null) {
                for (String s : message.getBody()) {
                    buffer.append(s).append(JNPProtocol.LINE_SEPARATOR);
                }
            }
            return buffer.toString();
        } else {
            return null;
        }
    }

    public Message readMessage(BufferedReader in) throws IOException {
        Message message = null;
        MessageHeader header = null;
        List<String> list = new ArrayList<String>();
        boolean messageEnd = false;
        while ((!messageEnd)) {
            String line = in.readLine();
            if (line != null && line.trim().length() > 0) {
                log.debug("BYTES RECEIVED: " + line);
                if (header == null) {
                    header = MessageHeader.findValue(line.trim());
                    if (header != null) {
                        log.debug("MESSAGE HEADER FOUND: " + header.getValue());
                    }
                } else if (header != null) {
                    MessageHeader tmpHeader = MessageHeader.findValue(line.trim());
                    if (tmpHeader == null) {
                        log.debug("MESSAGE CONTENT LINE: " + line);
                        list.add(line.trim());
                    } else {
                        String[] messageBody = list.toArray(new String[0]);
                        message = new BaseMessage(header, messageBody);
                        header = tmpHeader;
                        return message;
                    }
                }
            } else {
                messageEnd = true;
            }
        }
        if (header != null) {
            String[] messageBody = list.toArray(new String[0]);
            message = new BaseMessage(header, messageBody);
        }
        return message;
    }
}
