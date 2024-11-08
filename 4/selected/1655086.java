package com.whitebearsolutions.imagine.wbsairback.net;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.whitebearsolutions.imagine.wbsairback.configuration.HAConfiguration;
import com.whitebearsolutions.imagine.wbsairback.configuration.WBSAirbackConfiguration;
import com.whitebearsolutions.util.Configuration;

public class HACommServer {

    private Configuration _c;

    protected HACommServer(Configuration conf) {
        this._c = conf;
    }

    protected String request(byte[] request) throws Exception {
        if (this._c.checkProperty("cluster.status", "master") || this._c.checkProperty("cluster.status", "slave")) {
            return "remote product is already grouped";
        }
        if (this._c.checkProperty("cluster.request", "request")) {
            return "remote product has another active request";
        }
        ByteArrayInputStream _bais = new ByteArrayInputStream(request);
        FileOutputStream _fos = new FileOutputStream(WBSAirbackConfiguration.getHARequestFile());
        while (_bais.available() > 0) {
            _fos.write(_bais.read());
        }
        _fos.close();
        _bais.close();
        HashMap<String, String> values = getValues(WBSAirbackConfiguration.getHARequestFile());
        this._c.setProperty("cluster.request", "request");
        this._c.setProperty("cluster.remote", values.get("address.real"));
        this._c.store();
        return "done";
    }

    protected String requestConfirmStage1(String virtualAddress, String remoteAddress) {
        if (!this._c.checkProperty("cluster.request", "request")) {
            return "remote product has no active request";
        }
        if (!this._c.checkProperty("cluster.remote", remoteAddress)) {
            return "confirmation is expected from another network address";
        }
        if (virtualAddress == null || virtualAddress.isEmpty()) {
            return "virtual address has not provided";
        }
        try {
            HAConfiguration.setMasterStage1(virtualAddress, remoteAddress);
            return "done";
        } catch (Exception _ex) {
            return _ex.getMessage();
        }
    }

    protected String requestConfirmStage2(String virtualAddress, String remoteAddress) {
        if (!this._c.checkProperty("cluster.request", "request")) {
            return "remote product has no active request";
        }
        if (!this._c.checkProperty("cluster.remote", remoteAddress)) {
            return "confirmation is expected from another network address";
        }
        if (virtualAddress == null || virtualAddress.isEmpty()) {
            return "virtual address has not provided";
        }
        try {
            HAConfiguration.setMasterStage2(virtualAddress, remoteAddress);
            this._c.removeProperty("cluster.request");
            this._c.setProperty("cluster.virtual", virtualAddress);
            this._c.setProperty("cluster.status", "master");
            this._c.store();
            return "done";
        } catch (Exception _ex) {
            return _ex.getMessage();
        }
    }

    protected String requestReject(String remoteAddress) {
        if (!this._c.checkProperty("cluster.request", "request")) {
            return "remote product has no active request";
        }
        if (!this._c.checkProperty("cluster.remote", remoteAddress)) {
            return "reject is expected from another network address";
        }
        try {
            this._c.removeProperty("cluster.request");
            this._c.removeProperty("cluster.remote");
            this._c.store();
        } catch (Exception _ex) {
            return _ex.getMessage();
        }
        new File(WBSAirbackConfiguration.getHARequestFile()).delete();
        return "done";
    }

    protected String breakRequest(String remoteAddress, byte[] request) throws Exception {
        if (!this._c.checkProperty("cluster.status", "master") && !this._c.checkProperty("cluster.status", "slave")) {
            return "remote product is not currently grouped";
        }
        if (!this._c.checkProperty("cluster.remote", remoteAddress)) {
            return "network address is not equivalent to the grouped product";
        }
        ByteArrayInputStream _bais = new ByteArrayInputStream(request);
        FileOutputStream _fos = new FileOutputStream(WBSAirbackConfiguration.getHABreakFile());
        while (_bais.available() > 0) {
            _fos.write(_bais.read());
        }
        _fos.close();
        _bais.close();
        this._c.setProperty("cluster.request", "break");
        this._c.store();
        return "done";
    }

    protected String breakRequestConfirm(String remoteAddress) {
        if (!this._c.checkProperty("cluster.request", "break")) {
            return "remote product has no active break request";
        }
        if (!this._c.checkProperty("cluster.remote", remoteAddress)) {
            return "confirmation is expected from another network address";
        }
        try {
            HAConfiguration.setStandalone();
            this._c.removeProperty("cluster.request");
            this._c.removeProperty("cluster.status");
            this._c.removeProperty("cluster.virtual");
            this._c.removeProperty("cluster.remote");
            this._c.store();
            return "done";
        } catch (Exception _ex) {
            return _ex.getMessage();
        }
    }

    protected String breakRequestReject(String remoteAddress) {
        if (!this._c.checkProperty("cluster.request", "break")) {
            return "remote product has no active break request";
        }
        if (!this._c.checkProperty("cluster.remote", remoteAddress)) {
            return "reject is expected from another network address";
        }
        try {
            this._c.removeProperty("cluster.request");
            this._c.store();
        } catch (Exception _ex) {
            return _ex.getMessage();
        }
        new File(WBSAirbackConfiguration.getHABreakFile()).delete();
        return "done";
    }

    private HashMap<String, String> getValues(String file) throws Exception {
        HashMap<String, String> values = new HashMap<String, String>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document _doc = db.parse(new File(file));
        NodeList nl = _doc.getDocumentElement().getChildNodes();
        for (int i = nl.getLength(); --i >= 0; ) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) nl.item(i);
                values.put(e.getNodeName(), e.getTextContent());
            }
        }
        return values;
    }
}
