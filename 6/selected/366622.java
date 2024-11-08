package org.amiwall.delivery;

import org.amiwall.plugin.AbstractPlugin;
import org.apache.log4j.Logger;
import org.amiwall.user.User;
import org.apache.commons.net.io.Util;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SimpleSMTPHeader;
import java.io.Writer;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import org.amiwall.user.BasicUser;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.jdom.Element;
import java.util.Iterator;

/**
 *  Description of the Class
 *
 *@author    Nick Cunnah
 */
public class EmailDelivery extends AbstractPlugin implements Delivery {

    private static Logger log = Logger.getLogger("org.amiwall.delivery.EmailDelivery");

    /**
     *  Description of the Field
     */
    protected String server = null;

    /**
     *  Description of the Field
     */
    protected String from = null;

    /**
     *  Description of the Field
     */
    protected Set ccList = new HashSet();

    /**
     *  Gets the name attribute of the EmailDelivery object
     *
     *@return    The name value
     */
    public String getName() {
        return "EmailDelivery";
    }

    /**
     *  Description of the Method
     *
     *@param  digester  Description of the Parameter
     *@param  root      Description of the Parameter
     */
    public void digest(Element root) {
        setServer(root.getChildTextTrim("server"));
        setFrom(root.getChildTextTrim("from"));
        for (Iterator i = root.getChildren("cc").iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            addCc(child.getTextTrim());
        }
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    public void activate() throws Exception {
        super.activate();
        if (from == null) {
            throw new NullPointerException("from is NULL, this needs to be configured");
        }
        if (server == null) {
            throw new NullPointerException("server is NULL, this needs to be configured");
        }
    }

    /**
     *  Sets the from attribute of the EmailDelivery object
     *
     *@param  from  The new from value
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     *  Sets the server attribute of the EmailDelivery object
     *
     *@param  server  The new server value
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     *  Adds a feature to the Cc attribute of the EmailDelivery object
     *
     *@param  cc  The feature to be added to the Cc attribute
     */
    public void addCc(String cc) {
        ccList.add(cc);
    }

    /**
     *  Description of the Method
     *
     *@param  user     Description of the Parameter
     *@param  subject  Description of the Parameter
     *@param  message  Description of the Parameter
     */
    public void deliver(User user, String subject, String message) {
        if (user instanceof BasicUser) {
            BasicUser basicUser = (BasicUser) user;
            String recipient = basicUser.getEmail();
            if (recipient != null) {
                if (log.isDebugEnabled()) log.debug("delivery To:" + basicUser.getName() + " Sub:" + subject);
                try {
                    SimpleSMTPHeader header = getHeader(recipient, subject);
                    SMTPClient client = new SMTPClient();
                    client.connect(server);
                    if (!SMTPReply.isPositiveCompletion(client.getReplyCode())) {
                        client.disconnect();
                        System.err.println("SMTP server refused connection.");
                        System.exit(1);
                    }
                    client.login();
                    setClientHeaders(client, recipient);
                    writeMessage(client, header, message);
                    client.logout();
                    client.disconnect();
                    if (log.isDebugEnabled()) log.debug("Sent sucessfully");
                } catch (IOException e) {
                    log.error("Failed to send email to " + user.getName(), e);
                }
            } else {
                log.error("Cant send email to " + basicUser.getName() + " - dont have an email address");
            }
        } else {
            log.error("Cant send email to " + user.getName() + " - cant work out his email address.");
        }
    }

    /**
     *  Gets the header attribute of the EmailDelivery object
     *
     *@param  user     Description of the Parameter
     *@param  subject  Description of the Parameter
     *@return          The header value
     */
    protected SimpleSMTPHeader getHeader(String recipient, String subject) {
        SimpleSMTPHeader header = new SimpleSMTPHeader(from, recipient, subject);
        for (Iterator i = ccList.iterator(); i.hasNext(); ) {
            String cc = (String) i.next();
            header.addCC(cc);
        }
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        String date = df.format(Calendar.getInstance().getTime());
        header.addHeaderField("Date", date);
        return header;
    }

    /**
     *  Gets the client attribute of the EmailDelivery object
     *
     *@param  client  The new headersClient value
     */
    protected void setClientHeaders(SMTPClient client, String recipient) throws IOException {
        client.setSender(from);
        client.addRecipient(recipient);
        for (Iterator i = ccList.iterator(); i.hasNext(); ) {
            String cc = (String) i.next();
            client.addRecipient(cc);
        }
    }

    /**
     *  Description of the Method
     *
     *@param  client   Description of the Parameter
     *@param  header   Description of the Parameter
     *@param  message  Description of the Parameter
     */
    protected void writeMessage(SMTPClient client, SimpleSMTPHeader header, String message) throws IOException {
        Writer writer = client.sendMessageData();
        if (writer != null) {
            writer.write(header.toString());
            writer.write(message);
            writer.close();
            client.completePendingCommand();
        }
    }
}
