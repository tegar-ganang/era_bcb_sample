package com.dgtalize.netc.system;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author DGtalize
 */
public class NetCConfig {

    private static NetCConfig netCConfig = null;

    private String filePath = "config.xml";

    private Document configDoc;

    private String ipBroadcast = "";

    private int portBroadcast = 0;

    private int portPrivateChat = 0;

    private int portFileTransfer = 0;

    private String nickname = "";

    private Boolean trayMsgUserLogged = true;

    private Boolean trayMsgUserLoggedOff = false;

    private Boolean trayMsgPrivateMsg = true;

    private Boolean useNativeUI = true;

    private Boolean startMinimized = true;

    private String receivedFilesPath = "";

    private int fileTransfPartSize = 1024;

    private int fileTransfPartUntilAck = 1;

    public NetCConfig() {
    }

    public static NetCConfig getInstance() {
        if (netCConfig == null) {
            netCConfig = new NetCConfig();
        }
        return netCConfig;
    }

    private Document getConfigDocument() throws Exception {
        if (configDoc == null) {
            File cfgFile = new File(filePath);
            if (!cfgFile.exists()) {
                cfgFile.createNewFile();
                try {
                    InputStream fis = getClass().getResourceAsStream("/com/dgtalize/netc/business/config.xml");
                    FileOutputStream fos = new FileOutputStream(cfgFile);
                    byte[] buf = new byte[1024];
                    int readCant = 0;
                    while ((readCant = fis.read(buf)) != -1) {
                        fos.write(buf, 0, readCant);
                    }
                    fis.close();
                    fos.close();
                } catch (Exception ex) {
                    cfgFile.delete();
                    throw ex;
                }
            }
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            configDoc = db.parse(filePath);
            configDoc.getDocumentElement().normalize();
        }
        return configDoc;
    }

    public void readConfigFile() throws Exception {
        NodeList nodeParamLst = getConfigDocument().getElementsByTagName("param");
        for (int s = 0; s < nodeParamLst.getLength(); s++) {
            Node paramNode = nodeParamLst.item(s);
            if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
                Element paramElmnt = (Element) paramNode;
                Element paramNameElmnt = (Element) paramElmnt.getElementsByTagName("name").item(0);
                Element paramValueElmnt = (Element) paramElmnt.getElementsByTagName("value").item(0);
                String paramName = paramNameElmnt.getChildNodes().item(0).getNodeValue();
                if (paramName.equalsIgnoreCase("ipBroadcast")) {
                    ipBroadcast = paramValueElmnt.getChildNodes().item(0).getNodeValue();
                } else if (paramName.equalsIgnoreCase("portBroadcast")) {
                    portBroadcast = Integer.parseInt(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("nickname")) {
                    if (paramValueElmnt.getChildNodes().item(0) != null) {
                        nickname = paramValueElmnt.getChildNodes().item(0).getNodeValue();
                    } else {
                        nickname = "";
                    }
                } else if (paramName.equalsIgnoreCase("portPrivateChat")) {
                    portPrivateChat = Integer.parseInt(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("portFileTransfer")) {
                    portFileTransfer = Integer.parseInt(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("trayMsgUserLogged")) {
                    trayMsgUserLogged = Boolean.parseBoolean(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("trayMsgPrivateMsg")) {
                    trayMsgPrivateMsg = Boolean.parseBoolean(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("trayMsgUserLoggedOff")) {
                    trayMsgUserLoggedOff = Boolean.parseBoolean(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("useNativeUI")) {
                    useNativeUI = Boolean.parseBoolean(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("startMinimized")) {
                    startMinimized = Boolean.parseBoolean(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("receivedFilesPath")) {
                    if (paramValueElmnt.getChildNodes().item(0) != null) {
                        receivedFilesPath = paramValueElmnt.getChildNodes().item(0).getNodeValue();
                    } else {
                        receivedFilesPath = "";
                    }
                } else if (paramName.equalsIgnoreCase("fileTransfPartSize")) {
                    fileTransfPartSize = Integer.parseInt(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                } else if (paramName.equalsIgnoreCase("fileTransfPartUntilAck")) {
                    fileTransfPartUntilAck = Integer.parseInt(paramValueElmnt.getChildNodes().item(0).getNodeValue());
                }
            }
        }
    }

    public void saveConfigFile() throws Exception {
        Document confDoc = getConfigDocument();
        NodeList nodeParamLst = confDoc.getElementsByTagName("param");
        for (int s = 0; s < nodeParamLst.getLength(); s++) {
            Node paramNode = nodeParamLst.item(s);
            if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
                Element paramElmnt = (Element) paramNode;
                Element paramNameElmnt = (Element) paramElmnt.getElementsByTagName("name").item(0);
                Element paramValueElmnt = (Element) paramElmnt.getElementsByTagName("value").item(0);
                String paramName = paramNameElmnt.getChildNodes().item(0).getNodeValue();
                if (paramName.equalsIgnoreCase("ipBroadcast")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(ipBroadcast);
                } else if (paramName.equalsIgnoreCase("portBroadcast")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(Integer.toString(portBroadcast));
                } else if (paramName.equalsIgnoreCase("nickname")) {
                    if (paramValueElmnt.getChildNodes().item(0) != null) {
                        paramValueElmnt.getChildNodes().item(0).setNodeValue(nickname);
                    } else {
                        Node nickValue = confDoc.createTextNode(nickname);
                        paramValueElmnt.appendChild(nickValue);
                    }
                } else if (paramName.equalsIgnoreCase("portPrivateChat")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(Integer.toString(portPrivateChat));
                } else if (paramName.equalsIgnoreCase("portFileTransfer")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(Integer.toString(portFileTransfer));
                } else if (paramName.equalsIgnoreCase("trayMsgUserLogged")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(trayMsgUserLogged.toString());
                } else if (paramName.equalsIgnoreCase("trayMsgPrivateMsg")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(trayMsgPrivateMsg.toString());
                } else if (paramName.equalsIgnoreCase("trayMsgUserLoggedOff")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(trayMsgUserLoggedOff.toString());
                } else if (paramName.equalsIgnoreCase("useNativeUI")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(useNativeUI.toString());
                } else if (paramName.equalsIgnoreCase("startMinimized")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(startMinimized.toString());
                } else if (paramName.equalsIgnoreCase("receivedFilesPath")) {
                    if (paramValueElmnt.getChildNodes().item(0) != null) {
                        paramValueElmnt.getChildNodes().item(0).setNodeValue(receivedFilesPath);
                    } else {
                        Node nickValue = confDoc.createTextNode(receivedFilesPath);
                        paramValueElmnt.appendChild(nickValue);
                    }
                } else if (paramName.equalsIgnoreCase("fileTransfPartSize")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(Integer.toString(fileTransfPartSize));
                } else if (paramName.equalsIgnoreCase("fileTransfPartUntilAck")) {
                    paramValueElmnt.getChildNodes().item(0).setNodeValue(Integer.toString(fileTransfPartUntilAck));
                }
            }
        }
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        DOMSource source = new DOMSource(confDoc);
        File configFile = new File(filePath);
        StreamResult result = new StreamResult(configFile);
        transformer.transform(source, result);
    }

    public String getIpBroadcast() {
        return ipBroadcast;
    }

    public void setIpBroadcast(String ipBroadcast) {
        this.ipBroadcast = ipBroadcast;
    }

    public int getPortBroadcast() {
        return portBroadcast;
    }

    public void setPortBroadcast(int portBroadcast) {
        this.portBroadcast = portBroadcast;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getPortPrivateChat() {
        return portPrivateChat;
    }

    public void setPortPrivateChat(int portPrivateChat) {
        this.portPrivateChat = portPrivateChat;
    }

    public Boolean getTrayMsgPrivateMsg() {
        return trayMsgPrivateMsg;
    }

    public void setTrayMsgPrivateMsg(Boolean trayMsgPrivateMsg) {
        this.trayMsgPrivateMsg = trayMsgPrivateMsg;
    }

    public Boolean getTrayMsgUserLogged() {
        return trayMsgUserLogged;
    }

    public void setTrayMsgUserLogged(Boolean trayMsgUserLogged) {
        this.trayMsgUserLogged = trayMsgUserLogged;
    }

    public Boolean getTrayMsgUserLoggedOff() {
        return trayMsgUserLoggedOff;
    }

    public void setTrayMsgUserLoggedOff(Boolean trayMsgUserLoggedOff) {
        this.trayMsgUserLoggedOff = trayMsgUserLoggedOff;
    }

    public Boolean getUseNativeUI() {
        return useNativeUI;
    }

    public void setUseNativeUI(Boolean useNativeUI) {
        this.useNativeUI = useNativeUI;
    }

    public Boolean getStartMinimized() {
        return startMinimized;
    }

    public void setStartMinimized(Boolean startMinimized) {
        this.startMinimized = startMinimized;
    }

    public int getPortFileTransfer() {
        return portFileTransfer;
    }

    public void setPortFileTransfer(int portFileTransfer) {
        this.portFileTransfer = portFileTransfer;
    }

    public String getReceivedFilesPath() {
        String path = receivedFilesPath.trim();
        if (!path.isEmpty()) {
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
        }
        return path;
    }

    public void setReceivedFilesPath(String receivedFilesPath) {
        this.receivedFilesPath = receivedFilesPath;
    }

    public int getFileTransfPartSize() {
        return fileTransfPartSize;
    }

    public void setFileTransfPartSize(int fileTransfPartSeize) {
        this.fileTransfPartSize = fileTransfPartSeize;
    }

    public int getFileTransfPartUntilAck() {
        return fileTransfPartUntilAck;
    }

    public void setFileTransfPartUntilAck(int fileTransfPartUntilAck) {
        this.fileTransfPartUntilAck = fileTransfPartUntilAck;
    }
}
