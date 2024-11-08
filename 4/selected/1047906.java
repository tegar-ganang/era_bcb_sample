package source;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Served {

    public static void main(String[] args) throws Exception {
        Served xml = new Served();
        List<Server> networks = xml.parse("divx_ita.xml");
        for (Server s : networks) {
            System.out.println("network: " + s.getNetwork());
            System.out.println("server: " + s.getServer());
            System.out.println("channels: " + s.getChannels());
        }
    }

    public List parse(String filename) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = reader.read(filename);
        Element root = document.getRootElement();
        List networksList = new ArrayList();
        Iterator networks = root.elementIterator();
        while (networks.hasNext()) {
            Server server = new Server();
            Element network = (Element) networks.next();
            Iterator attributes = network.attributeIterator();
            while (attributes.hasNext()) {
                Attribute attribute = (Attribute) attributes.next();
                if (attribute.getName() == "name") {
                    server.setNetwork(attribute.getText());
                }
            }
            Iterator elements = network.elementIterator();
            while (elements.hasNext()) {
                Element element = (Element) elements.next();
                if (element.getName() == "server") {
                    Iterator serverAttr = element.attributeIterator();
                    while (serverAttr.hasNext()) {
                        Attribute attribute = (Attribute) serverAttr.next();
                        if (attribute.getName() == "name") server.setServer(attribute.getText());
                        if (attribute.getName() == "port") server.setPort(attribute.getText());
                    }
                } else {
                    Iterator channelAttr = element.attributeIterator();
                    while (channelAttr.hasNext()) {
                        Attribute attribute = (Attribute) channelAttr.next();
                        if (attribute.getName() == "name") server.addChannel(attribute.getText());
                    }
                }
            }
            networksList.add(server);
        }
        return networksList;
    }
}
