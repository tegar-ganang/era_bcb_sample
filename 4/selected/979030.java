package com.whitebearsolutions.imagine.wbsagnitio.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.whitebearsolutions.http.HTTPClient;
import com.whitebearsolutions.imagine.wbsagnitio.NetworkManager;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.HAConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.TomcatConfiguration;
import com.whitebearsolutions.imagine.wbsagnitio.configuration.WBSAgnitioConfiguration;
import com.whitebearsolutions.util.Configuration;

public class HACommClient {

    private Configuration _c;

    private NetworkManager _nm;

    public HACommClient(Configuration conf) throws Exception {
        this._c = conf;
        this._nm = new NetworkManager(this._c);
    }

    public void request(String[] virtualAddress, String remoteAddress) throws Exception {
        if (this._c.getProperty("directory.request") != null) {
            throw new Exception("an active request already exists");
        }
        if (!this._nm.isNetworkAddress(this._nm.getInterface(0), NetworkManager.toAddress(remoteAddress))) {
            throw new Exception("remote address is not in the network range");
        }
        if (!this._nm.isNetworkAddress(this._nm.getInterface(0), virtualAddress)) {
            throw new Exception("virtual address is not in the network range");
        }
        StringBuilder xml_content = new StringBuilder();
        xml_content.append("<ha><address.real>");
        xml_content.append(this._nm.getStringAddress(this._nm.getInterface(0)));
        xml_content.append("</address.real><address.virtual>");
        xml_content.append(virtualAddress[0] + "." + virtualAddress[1] + "." + virtualAddress[2] + "." + virtualAddress[3]);
        xml_content.append("</address.virtual>");
        xml_content.append("<ldap.basedn>" + this._c.getProperty("ldap.basedn"));
        xml_content.append("</ldap.basedn></ha>");
        HTTPClient _hc = new HTTPClient(remoteAddress);
        if (TomcatConfiguration.checkHTTPS()) {
            _hc.setSecure(true);
        }
        HashMap<String, String> _parameters = new HashMap<String, String>();
        HashMap<String, byte[]> _files = new HashMap<String, byte[]>();
        _parameters.put("type", String.valueOf(CommResponse.TYPE_HA));
        _parameters.put("command", String.valueOf(CommResponse.COMMAND_REQUEST));
        _files.put("ha-request.xml", xml_content.toString().getBytes());
        if (new File(WBSAgnitioConfiguration.getSchemaObjectFile()).exists()) {
            ByteArrayOutputStream _baos = new ByteArrayOutputStream();
            FileInputStream _fis = new FileInputStream(WBSAgnitioConfiguration.getSchemaObjectFile());
            while (_fis.available() > 0) {
                _baos.write(_fis.read());
            }
            _fis.close();
            _files.put("schema_objects.xml", _baos.toByteArray());
            _baos.close();
        }
        if (new File(WBSAgnitioConfiguration.getOptionalSchemaFile()).exists()) {
            ByteArrayOutputStream _baos = new ByteArrayOutputStream();
            FileInputStream _fis = new FileInputStream(WBSAgnitioConfiguration.getOptionalSchemaFile());
            while (_fis.available() > 0) {
                _baos.write(_fis.read());
            }
            _fis.close();
            _files.put("optional.schema", _baos.toByteArray());
            _baos.close();
        }
        _hc.multipartLoad("/admin/Comm", _parameters, _files);
        String _reply = new String(_hc.getContent());
        if (_reply.isEmpty()) {
            throw new Exception("remote product has not sent any reply");
        } else if (_reply.indexOf("done") == -1) {
            throw new Exception(_reply);
        }
        this._c.setProperty("directory.remote", remoteAddress);
        this._c.setProperty("directory.request", "request");
        this._c.store();
    }

    public void requestConfirm() throws Exception {
        if (!this._c.checkProperty("directory.request", "request")) {
            throw new Exception("product has no active request");
        }
        if (!new File(WBSAgnitioConfiguration.getHARequestFile()).canWrite()) {
            throw new Exception("cannot remove request from system");
        }
        HashMap<String, String> values = getValues(WBSAgnitioConfiguration.getHARequestFile());
        if (!values.containsKey("address.virtual")) {
            throw new Exception("failed to determine the virtual address");
        }
        if (!values.containsKey("address.real")) {
            throw new Exception("failed to determine the remote address");
        }
        HTTPClient _hc = new HTTPClient(values.get("address.real"));
        if (TomcatConfiguration.checkHTTPS()) {
            _hc.setSecure(true);
        }
        _hc.load("/admin/Comm?type=" + CommResponse.TYPE_HA + "&command=" + CommResponse.COMMAND_REQUEST_CONFIRM + "&virtual=" + values.get("address.virtual"));
        String _reply = new String(_hc.getContent());
        if (_reply.isEmpty()) {
            throw new Exception("remote product has not sent any reply");
        } else if (_reply.indexOf("done") == -1) {
            throw new Exception(_reply);
        }
        HAConfiguration.setSlave(values.get("address.virtual"), values.get("address.real"));
        File _f = new File(WBSAgnitioConfiguration.getOptionalSchemaRequestFile());
        if (_f.exists()) {
            FileOutputStream _fos = new FileOutputStream(WBSAgnitioConfiguration.getOptionalSchemaFile());
            FileInputStream _fis = new FileInputStream(_f);
            while (_fis.available() > 0) {
                _fos.write(_fis.read());
            }
            _fis.close();
            _fos.close();
            _f.delete();
        }
        _f = new File(WBSAgnitioConfiguration.getSchemaObjectRequestFile());
        if (_f.exists()) {
            FileOutputStream _fos = new FileOutputStream(WBSAgnitioConfiguration.getSchemaObjectFile());
            FileInputStream _fis = new FileInputStream(_f);
            while (_fis.available() > 0) {
                _fos.write(_fis.read());
            }
            _fis.close();
            _fos.close();
            _f.delete();
        }
        new File(WBSAgnitioConfiguration.getHARequestFile()).delete();
        this._c.removeProperty("directory.request");
        this._c.setProperty("directory.virtual", values.get("address.virtual"));
        this._c.setProperty("directory.status", "slave");
        this._c.store();
    }

    public void requestReject() throws Exception {
        if (!this._c.checkProperty("directory.request", "request")) {
            throw new Exception("product has no active request");
        }
        if (!new File(WBSAgnitioConfiguration.getHARequestFile()).canWrite()) {
            throw new Exception("cannot remove request from system");
        }
        HashMap<String, String> values = getValues(WBSAgnitioConfiguration.getHARequestFile());
        if (!values.containsKey("address.virtual")) {
            throw new Exception("failed to determine the virtual address");
        }
        if (!values.containsKey("address.real")) {
            throw new Exception("failed to determine the remote address");
        }
        HTTPClient _hc = new HTTPClient(values.get("address.real"));
        if (TomcatConfiguration.checkHTTPS()) {
            _hc.setSecure(true);
        }
        _hc.load("/admin/Comm?type=" + CommResponse.TYPE_HA + "&command=" + CommResponse.COMMAND_REQUEST_REJECT);
        String _reply = new String(_hc.getContent());
        if (_reply.isEmpty()) {
            throw new Exception("remote product has not sent any reply");
        } else if (_reply.indexOf("done") == -1) {
            throw new Exception(_reply);
        }
        if (new File("/opt/imagine/optional.schema.request").exists()) {
            new File("/opt/imagine/optional.schema.request").delete();
        }
        if (new File("/opt/imagine/schema_objects.xml.request").exists()) {
            new File("/opt/imagine/schema_objects.xml.request").delete();
        }
        new File(WBSAgnitioConfiguration.getHARequestFile()).delete();
        this._c.removeProperty("directory.remote");
        this._c.removeProperty("directory.request");
        this._c.store();
    }

    public void breakRequest() throws Exception {
        if (!this._c.checkProperty("directory.status", "master") && !this._c.checkProperty("directory.status", "slave")) {
            throw new Exception("this product is not currently grouped");
        }
        StringBuilder xml_content = new StringBuilder();
        xml_content.append("<ha><address.real>");
        xml_content.append(this._nm.getStringAddress(this._nm.getInterface(0)));
        xml_content.append("</address.real></ha>");
        HTTPClient _hc = new HTTPClient(this._c.getProperty("directory.remote"));
        if (TomcatConfiguration.checkHTTPS()) {
            _hc.setSecure(true);
        }
        HashMap<String, String> _parameters = new HashMap<String, String>();
        HashMap<String, byte[]> _files = new HashMap<String, byte[]>();
        _parameters.put("type", String.valueOf(CommResponse.TYPE_HA));
        _parameters.put("command", String.valueOf(CommResponse.COMMAND_BREAK));
        _files.put("ha-break.xml", xml_content.toString().getBytes());
        _hc.multipartLoad("/admin/Comm", _parameters, _files);
        String _reply = new String(_hc.getContent());
        if (_reply.isEmpty()) {
            throw new Exception("remote product has not sent any reply");
        } else if (_reply.indexOf("done") == -1) {
            throw new Exception(_reply);
        }
        this._c.setProperty("directory.request", "break");
        this._c.store();
    }

    public void breakRequestConfirm() throws Exception {
        if (!this._c.checkProperty("directory.request", "break")) {
            throw new Exception("this product has no active break request");
        }
        if (!new File(WBSAgnitioConfiguration.getHABreakFile()).canWrite()) {
            throw new Exception("cannot remove break request from system");
        }
        HashMap<String, String> values = getValues(WBSAgnitioConfiguration.getHABreakFile());
        if (!values.containsKey("address.real")) {
            throw new Exception("failed to determine the remote address");
        }
        HTTPClient _hc = new HTTPClient(values.get("address.real"));
        if (TomcatConfiguration.checkHTTPS()) {
            _hc.setSecure(true);
        }
        _hc.load("/admin/Comm?type=" + CommResponse.TYPE_HA + "&command=" + CommResponse.COMMAND_BREAK_CONFIRM);
        String _reply = new String(_hc.getContent());
        if (_reply.isEmpty()) {
            throw new Exception("remote product has not sent any reply");
        } else if (_reply.indexOf("done") == -1) {
            throw new Exception(_reply);
        }
        HAConfiguration.setStandalone();
        new File(WBSAgnitioConfiguration.getHABreakFile()).delete();
        this._c.removeProperty("directory.request");
        this._c.removeProperty("directory.status");
        this._c.removeProperty("directory.virtual");
        this._c.removeProperty("directory.remote");
        this._c.store();
    }

    public void breakRequestReject() throws Exception {
        if (!this._c.checkProperty("directory.request", "break")) {
            throw new Exception("this product has no active break request");
        }
        if (!new File(WBSAgnitioConfiguration.getHABreakFile()).canWrite()) {
            throw new Exception("cannot remove break request from system");
        }
        HashMap<String, String> values = getValues(WBSAgnitioConfiguration.getHABreakFile());
        if (!values.containsKey("address.real")) {
            throw new Exception("failed to determine the remote address");
        }
        HTTPClient _hc = new HTTPClient(values.get("address.real"));
        if (TomcatConfiguration.checkHTTPS()) {
            _hc.setSecure(true);
        }
        _hc.load("/admin/Comm?type=" + CommResponse.TYPE_HA + "&command=" + CommResponse.COMMAND_BREAK_REJECT);
        String _reply = new String(_hc.getContent());
        if (_reply.isEmpty()) {
            throw new Exception("remote product has not sent any reply");
        } else if (_reply.indexOf("done") == -1) {
            throw new Exception(_reply);
        }
        new File(WBSAgnitioConfiguration.getHABreakFile()).delete();
        this._c.removeProperty("directory.request");
        this._c.store();
    }

    public void forceBreak() throws Exception {
        forgetRequest();
        HAConfiguration.setStandalone();
    }

    public void forgetRequest() throws Exception {
        if (new File(WBSAgnitioConfiguration.getHARequestFile()).canWrite()) {
            new File(WBSAgnitioConfiguration.getHARequestFile()).delete();
        }
        if (new File(WBSAgnitioConfiguration.getHABreakFile()).canWrite()) {
            new File(WBSAgnitioConfiguration.getHABreakFile()).delete();
        }
        this._c.removeProperty("directory.request");
        this._c.removeProperty("directory.status");
        this._c.removeProperty("directory.remote");
        this._c.removeProperty("directory.virtual");
        this._c.store();
    }

    public static HashMap<String, String> getValues(String file) throws Exception {
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
