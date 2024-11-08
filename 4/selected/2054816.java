package org.ibex.js;

import java.io.IOException;
import java.io.InputStream;
import org.ibex.util.*;

/**
 *  A partial RPC-style SOAP 1.1 client. Implemented from the SOAP 1.1
 *  Spec and Dave Winer's "SOAP for Busy Developers". This class
 *  extends XMLRPC in order to share some networking logic.
 *
 *  Currently unsupported features/hacks:
 *  <ul><li> Multi-ref data and circular references
 *      <li> 'Document Style'
 *      <li> WSDL support
 *  </ul>
 */
public class SOAP extends XMLRPC {

    /** the desired content of the SOAPAction header */
    String action = null;

    /** the namespace to use */
    String nameSpace = null;

    /** When you get a property from an SOAP, it just returns another SOAP with the property name tacked onto methodname. */
    public Object get(Object name) {
        return new SOAP(url, (method.equals("") ? "" : method + ".") + name.toString(), this, action, nameSpace);
    }

    public void startElement(String name, String[] keys, Object[] vals, int line, int col) {
        content.reset();
        if (name.equals("SOAP-ENV:Envelope")) return;
        if (name.equals("SOAP-ENV:Body")) return;
        if (name.equals("SOAP-ENV:Fault")) fault = true;
        objects.push(new JS.Obj());
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String value = vals[i].toString();
            if (key.endsWith("ype")) {
                if (value.endsWith("boolean")) {
                    objects.pop();
                    objects.push(JSU.B(true));
                } else if (value.endsWith("int")) {
                    objects.pop();
                    objects.push(NC_0);
                } else if (value.endsWith("double")) {
                    objects.pop();
                    objects.push(NC_0_0);
                } else if (value.endsWith("string")) {
                    objects.pop();
                    objects.push(SC_);
                } else if (value.endsWith("base64")) {
                    objects.pop();
                    objects.push(new byte[] {});
                } else if (value.endsWith("null")) {
                    objects.pop();
                    objects.push(null);
                } else if (value.endsWith("arrayType") || value.endsWith("JSArray") || key.endsWith("arrayType")) {
                    objects.pop();
                    objects.push(new JSArray());
                }
            }
        }
    }

    public void endElement(String name, int line, int col) {
        if (name.equals("SOAP-ENV:Envelope")) return;
        if (name.equals("SOAP-ENV:Body")) return;
        if (content.size() > 0 && content.toString().trim().length() > 0) {
            Object me = objects.get(objects.size() - 1);
            if (fault || me instanceof String) {
                objects.pop();
                objects.push(new String(content.getBuf(), 0, content.size()));
                content.reset();
            } else if (me instanceof byte[]) {
                objects.pop();
                objects.push(new Fountain.ByteArray(Encode.fromBase64(new String(content.getBuf(), 0, content.size()))));
                content.reset();
            } else if (me instanceof Integer) {
                objects.pop();
                objects.push(new Integer(new String(content.getBuf(), 0, content.size())));
                content.reset();
            } else if (me instanceof Boolean) {
                objects.pop();
                String s = new String(content.getBuf(), 0, content.size()).trim();
                if (s.equals("1") || s.equals("true")) objects.push(Boolean.TRUE); else objects.push(Boolean.FALSE);
                content.reset();
            } else if (me instanceof Double) {
                objects.pop();
                objects.push(new Double(new String(content.getBuf(), 0, content.size())));
                content.reset();
            } else {
                String s = new String(content.getBuf(), 0, content.size()).trim();
                boolean hasdot = false;
                for (int i = 0; i < s.length(); i++) {
                    if (s.charAt(i) == '.') hasdot = true;
                    if (!Character.isDigit(s.charAt(i))) {
                        objects.pop();
                        objects.push(s);
                        return;
                    }
                }
                if (hasdot) {
                    objects.pop();
                    objects.push(new Double(s));
                } else {
                    objects.pop();
                    objects.push(new Integer(s));
                }
                content.reset();
            }
        }
        JS me = (JS) objects.get(objects.size() - 1);
        JS parent = objects.size() > 1 ? (JS) objects.get(objects.size() - 2) : (JS) null;
        if (objects.size() < 2) return;
        if (parent != null && parent instanceof JSArray) {
            objects.pop();
            ((JSArray) parent).push(me);
        } else if (parent != null && parent instanceof JS) {
            objects.pop();
            try {
                parent.put(JSU.S(name), me);
            } catch (JSExn e) {
                throw new Error("this should never happen");
            }
        }
    }

    /** Appends the SOAP representation of <code>o</code> to <code>sb</code> */
    void appendObject(String name, Object o, StringBuffer sb) throws JSExn {
        if (o instanceof Number) {
            if ((double) ((Number) o).intValue() == ((Number) o).doubleValue()) {
                sb.append("                <" + name + " xsi:type=\"xsd:int\">");
                sb.append(((Number) o).intValue());
                sb.append("</" + name + ">\r\n");
            } else {
                sb.append("                <" + name + " xsi:type=\"xsd:double\">");
                sb.append(o);
                sb.append("</" + name + ">\r\n");
            }
        } else if (o instanceof Boolean) {
            sb.append("                <" + name + " xsi:type=\"xsd:boolean\">");
            sb.append(((Boolean) o).booleanValue() ? "true" : "false");
            sb.append("</" + name + ">\r\n");
        } else if (o instanceof Fountain) {
            try {
                sb.append("                <" + name + " xsi:type=\"SOAP-ENC:base64\">\r\n");
                InputStream is = ((Fountain) o).getInputStream();
                byte[] buf = new byte[54];
                while (true) {
                    int numread = is.read(buf, 0, 54);
                    if (numread == -1) break;
                    byte[] writebuf = buf;
                    if (numread < buf.length) {
                        writebuf = new byte[numread];
                        System.arraycopy(buf, 0, writebuf, 0, numread);
                    }
                    sb.append("              ");
                    sb.append(new String(Encode.toBase64(writebuf)));
                    sb.append("\r\n");
                }
                sb.append(((Boolean) o).booleanValue() ? "1" : "0");
                sb.append("</" + name + ">\r\n");
            } catch (IOException e) {
                logger.info(this, "caught IOException while attempting to send a Fountain via SOAP");
                logger.info(this, e);
                throw new JSExn("caught IOException while attempting to send a Fountain via SOAP");
            }
        } else if (o instanceof String) {
            sb.append("                <" + name + " xsi:type=\"xsd:string\">");
            String s = (String) o;
            if (s.indexOf('<') == -1 && s.indexOf('&') == -1) {
                sb.append(s);
            } else {
                char[] cbuf = s.toCharArray();
                while (true) {
                    int oldi = 0, i = 0;
                    while (i < cbuf.length && cbuf[i] != '<' && cbuf[i] != '&') i++;
                    sb.append(cbuf, oldi, i);
                    if (i == cbuf.length) break;
                    if (cbuf[i] == '<') sb.append("&lt;"); else if (cbuf[i] == '&') sb.append("&amp;");
                    i = oldi = i + 1;
                }
            }
            sb.append("</" + name + ">\r\n");
        } else if (o instanceof JSArray) {
            JSArray a = (JSArray) o;
            sb.append("                <" + name + " SOAP-ENC:arrayType=\"xsd:ur-type[" + a.size() + "]\">");
            for (int i = 0; i < a.size(); i++) appendObject("item", a.get(i), sb);
            sb.append("</" + name + ">\r\n");
        } else if (o instanceof JS) {
            JS j = (JS) o;
            sb.append("                <" + name + ">");
            JS.Enumeration e = j.keys().iterator();
            while (e.hasNext()) {
                Object key = e.next();
                appendObject((String) key, j.get((JS) key), sb);
            }
            sb.append("</" + name + ">\r\n");
        }
    }

    protected String buildRequest(JSArray args) throws JSExn, IOException {
        StringBuffer content = new StringBuffer();
        content.append("SOAPAction: " + action + "\r\n\r\n");
        content.append("<?xml version=\"1.0\"?>\r\n");
        content.append("<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"\r\n");
        content.append("                   xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"\r\n");
        content.append("                   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\r\n");
        content.append("                   xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\"\r\n");
        content.append("                   xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\">\r\n");
        content.append("<SOAP-ENV:Body>\r\n");
        content.append("    <");
        content.append(method);
        content.append(nameSpace != null ? " xmlns=\"" + nameSpace + "\"" : "");
        content.append(">\r\n");
        if (args.size() > 0) {
            JS.Enumeration e = ((JS) args.get(0)).keys().iterator();
            while (e.hasNext()) {
                JS key = e.next();
                appendObject(((JSString) key).coerceToString(), ((JS) args.get(0)).get(key), content);
            }
        }
        content.append("    </" + method + "></SOAP-ENV:Body></SOAP-ENV:Envelope>\r\n");
        return content.toString();
    }

    public SOAP(Logger logger, String url, String methodname, String action, String nameSpace) throws JSExn {
        super(logger, url, methodname);
        this.action = action;
        this.nameSpace = nameSpace;
    }

    public SOAP(String url, String methodname, SOAP httpSource, String action, String nameSpace) {
        super(url, methodname, httpSource);
        this.action = action;
        this.nameSpace = nameSpace;
    }
}
