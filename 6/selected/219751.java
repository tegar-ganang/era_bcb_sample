package org.one.stone.soup.wiki.pop3.client;

import java.io.BufferedReader;
import java.io.Reader;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3Command;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.commons.net.pop3.POP3Reply;
import org.one.stone.soup.entity.KeyValuePair;
import org.one.stone.soup.xml.XmlElement;

public class WikiPOP3Client {

    private String hostName;

    private String userName;

    private String password;

    private POP3Client client;

    public static void main(String[] args) {
        try {
            WikiPOP3Client client = new WikiPOP3Client();
            client.init(args[0], args[1], args[2]);
            client.connect();
            XmlElement mailBag = client.checkMail();
            client.disconnect();
            System.out.println(mailBag.toXml());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getVersion() {
        return "build 01";
    }

    public WikiPOP3Client() {
    }

    public void init(String hostName, String userName, String password) {
        this.hostName = hostName;
        this.userName = userName;
        this.password = password;
        client = new POP3Client();
    }

    public void connect() throws Exception {
        client.connect(hostName, 110);
        client.login(userName, password);
    }

    public void disconnect() throws Exception {
        client.logout();
        client.disconnect();
    }

    public XmlElement checkMail() throws Exception {
        POP3MessageInfo[] messagesInfo = client.listMessages();
        if (messagesInfo == null) {
            return null;
        }
        XmlElement mailBag = new XmlElement("MailBag");
        for (int loop = 0; loop < messagesInfo.length; loop++) {
            mailBag.addChild(getMail(messagesInfo[loop].number, true));
        }
        return mailBag;
    }

    public XmlElement getMailItem(XmlElement mailHeader) throws Exception {
        int number = Integer.parseInt(mailHeader.getAttributeValueByName("number"));
        return getMail(number, false);
    }

    public boolean deleteMailItem(XmlElement mailHeader) throws Exception {
        int number = Integer.parseInt(mailHeader.getAttributeValueByName("number"));
        return client.deleteMessage(number);
    }

    private XmlElement getMail(int number, boolean justHeader) throws Exception {
        Reader reader = null;
        POP3MessageInfo info = client.listUniqueIdentifier(number);
        if (justHeader) {
            reader = client.retrieveMessageTop(number, 0);
        } else {
            reader = client.retrieveMessage(number);
        }
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line = bufferedReader.readLine();
        XmlElement mail = new XmlElement("Mail");
        mail.addAttribute("number", "" + number);
        mail.addAttribute("id", info.identifier);
        XmlElement currentElement = null;
        while (line != null && line.length() != 0) {
            if (line.indexOf(": ") != -1) {
                KeyValuePair headerParam = KeyValuePair.parseKeyValuePair(line, ": ");
                currentElement = mail.addChild(headerParam.key);
                currentElement.addValue(headerParam.value);
            } else {
                currentElement.setValue(currentElement.getValue() + "\n\r" + line);
            }
            line = bufferedReader.readLine();
        }
        if (justHeader) {
            return mail;
        }
        if (line != null) {
            XmlElement content = mail.addChild("Content");
            line = bufferedReader.readLine();
            while (line != null && line.length() != 0) {
                if (line.indexOf(": ") != -1) {
                    KeyValuePair headerParam = KeyValuePair.parseKeyValuePair(line, ": ");
                    content.addChild(headerParam.key).addValue(headerParam.value);
                }
                line = bufferedReader.readLine();
            }
            line = bufferedReader.readLine();
            XmlElement data = content.addChild("Data");
            StringBuffer dataBuffer = new StringBuffer();
            while (line != null && line.length() != 0) {
                dataBuffer.append(line);
                line = bufferedReader.readLine();
            }
            data.addValue(dataBuffer.toString());
        }
        return mail;
    }
}
