package org.vexi.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import org.ibex.js.Fountain;
import org.ibex.js.JS;
import org.ibex.js.JSArray;
import org.ibex.js.JSDate;
import org.ibex.js.JSExn;
import org.ibex.util.Encode;
import org.ibex.util.Hash;
import org.ibex.util.Log;
import org.kxml2.io.KXmlSerializer;

/**
 * Generates an XMLRPC request string.
 * 
 * @author tupshin
 */
public class XmlRpcMarshaller {

    /**
	 * @param args
	 *            The Javascript array that contains the request parameters
	 * @param method
	 *            The XMLRPC method that will be called on the server
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 * @throws JSExn
	 *             whenever any non-IOException problem occurs
	 * @throws IOException
	 *             It is assumed that an IOException will be turned into a JSExn
	 *             higher in the chain
	 * @return StringWriter The container for the request string
	 */
    public StringWriter buildRequest(JSArray args, String method, Hash tracker) throws JSExn, IOException {
        KXmlSerializer s = new KXmlSerializer();
        StringWriter xmlWriter = new StringWriter();
        s.setOutput(xmlWriter);
        s.startDocument(null, null);
        s.ignorableWhitespace("\n\n");
        s.startTag("", "methodCall");
        s.startTag("", "methodName");
        s.text(method);
        s.endTag("", "methodName");
        s.startTag("", "params");
        for (int i = 0; i < args.size(); i++) {
            s.startTag("", "param");
            appendObject(args.get(i), s, tracker);
            s.endTag("", "param");
        }
        s.endTag("", "params");
        s.endTag("", "methodCall");
        return xmlWriter;
    }

    /**
	 * Appends the XML-RPC representation of <code>o</code> to
	 * <code>serializer</code>
	 * 
	 * @throws JSExn
	 *             Any error in making a XMLRPC request should be treated as a
	 *             JSExn and handled by the app
	 * @param object
	 *            The object that is to be included in an XMLRPC request
	 * @param serializer
	 *            The KXmlSerializer the XMLRPC request is using
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 */
    private void appendObject(Object object, KXmlSerializer serializer, Hash tracker) throws JSExn {
        if (object == null) {
            throw new JSExn("attempted to send a null value via XML-RPC");
        } else if (object instanceof Number) {
            appendNumber((Number) object, serializer, tracker);
        } else if (object instanceof Boolean) {
            appendBoolean((Boolean) object, serializer, tracker);
        } else if (object instanceof Fountain) {
            appendStream((Fountain) object, serializer, tracker);
        } else if (object instanceof String) {
            appendString((String) object, serializer, tracker);
        } else if (object instanceof JSDate) {
            appendDate((JSDate) object, serializer, tracker);
        } else if (object instanceof JSArray) {
            appendArray((JSArray) object, serializer, tracker);
        } else if (object instanceof JS) {
            appendStruct((JS) object, serializer, tracker);
        } else {
            throw new JSExn("attempt to send object of type " + object.getClass().getName() + " via XML-RPC");
        }
    }

    /**
	 * 
	 * @param struct
	 *            A generic JS object is used to work with XMLRPC structs
	 * @param serializer
	 *            The KXmlSerializer the XMLRPC request is using
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 * @throws JSExn
	 *             Any error in making a XMLRPC request should be treated as a
	 *             JSExn and handled by the app
	 */
    private void appendStruct(JS struct, KXmlSerializer serializer, Hash tracker) throws JSExn {
        if (tracker.get(struct) != null) {
            throw new JSExn("attempted to send multi-ref data structure via XML-RPC");
        }
        tracker.put(struct, Boolean.TRUE);
        try {
            serializer.startTag("", "value");
            serializer.startTag("", "struct");
            JS.Enumeration e = struct.keys().iterator();
            while (e.hasNext()) {
                JS key = e.next();
                serializer.startTag("", "member");
                serializer.startTag("", "name");
                serializer.text(key.toString());
                serializer.endTag("", "name");
                appendObject(struct.get(key), serializer, tracker);
                serializer.endTag("", "member");
            }
            serializer.endTag("", "struct");
            serializer.endTag("", "value");
            tracker.remove(struct);
        } catch (IOException e) {
            throw new JSExn("IOException when trying to append struct to XMLRPC Request: " + e);
        }
    }

    /**
	 * 
	 * @param array
	 *            The array that is to be included in an XMLRPC request
	 * @param serializer
	 *            The KXmlSerializer the XMLRPC request is using
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 * @throws JSExn
	 *             Any error in making a XMLRPC request should be treated as a
	 *             JSExn and handled by the app
	 */
    private void appendArray(JSArray array, KXmlSerializer serializer, Hash tracker) throws JSExn {
        if (tracker.get(array) != null) {
            throw new JSExn("attempted to send multi-ref data structure via XML-RPC");
        }
        tracker.put(array, Boolean.TRUE);
        try {
            serializer.startTag("", "value");
            serializer.startTag("", "array");
            serializer.startTag("", "data");
            for (int i = 0; i < array.size(); i++) {
                appendObject(array.get(i), serializer, tracker);
            }
            serializer.endTag("", "data");
            serializer.endTag("", "array");
            serializer.endTag("", "value");
            tracker.remove(array);
        } catch (IOException e) {
            throw new JSExn("IOException when trying to append array to XMLRPC Request: " + e);
        }
    }

    /**
	 * 
	 * @param date
	 *            The date object that is to be included in an XMLRPC request
	 * @param serializer
	 *            The KXmlSerializer the XMLRPC request is using
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 * @throws JSExn
	 *             Any error in making a XMLRPC request should be treated as a
	 *             JSExn and handled by the app
	 */
    private void appendDate(JSDate date, KXmlSerializer serializer, Hash tracker) throws JSExn {
        try {
            serializer.startTag("", "value");
            serializer.startTag("", "dateTime.iso8601");
            Calendar cal = Calendar.getInstance();
            Date d = new Date(date.getRawTime());
            cal.setTime(d);
            serializer.text(new Integer(cal.get(Calendar.YEAR)).toString());
            if (cal.get(Calendar.MONTH) + 1 < 10) {
                serializer.text("0");
            }
            serializer.text(new Integer(cal.get(Calendar.MONTH) + 1).toString());
            if (cal.get(Calendar.DAY_OF_MONTH) < 10) {
                serializer.text("0");
            }
            serializer.text(new Integer(cal.get(Calendar.DAY_OF_MONTH)).toString());
            serializer.text("T");
            if (cal.get(Calendar.HOUR_OF_DAY) < 10) {
                serializer.text("0");
            }
            serializer.text(new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
            serializer.text(":");
            if (cal.get(Calendar.MINUTE) < 10) {
                serializer.text("0");
            }
            serializer.text(new Integer(cal.get(Calendar.MINUTE)).toString());
            serializer.text(":");
            if (cal.get(Calendar.SECOND) < 10) {
                serializer.text("0");
            }
            serializer.text(new Integer(cal.get(Calendar.SECOND)).toString());
            serializer.endTag("", "dateTime.iso8601");
            serializer.endTag("", "value");
        } catch (IOException e) {
            throw new JSExn("IOException when trying to append Date to XMLRPC Request: " + e);
        }
    }

    /**
	 * 
	 * @param string
	 *            The string that is to be included in an XMLRPC request
	 * @param serializer
	 *            The KXmlSerializer the XMLRPC request is using
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 * @throws JSExn
	 *             Any error in making a XMLRPC request should be treated as a
	 *             JSExn and handled by the app
	 */
    private void appendString(String string, KXmlSerializer serializer, Hash tracker) throws JSExn {
        try {
            serializer.startTag("", "value");
            serializer.startTag("", "string");
            if (string.indexOf('<') == -1 && string.indexOf('&') == -1) {
                serializer.text(string);
            } else {
                char[] cbuf = string.toCharArray();
                int oldi = 0, i = 0;
                while (true) {
                    while (i < cbuf.length && cbuf[i] != '<' && cbuf[i] != '&') {
                        i++;
                    }
                    serializer.text(cbuf, oldi, i - oldi);
                    if (i >= cbuf.length) {
                        break;
                    }
                    if (cbuf[i] == '<') {
                        serializer.text("<");
                    } else if (cbuf[i] == '&') {
                        serializer.text("&");
                    }
                    i++;
                    oldi = i;
                    if (i >= cbuf.length) {
                        break;
                    }
                }
            }
            serializer.endTag("", "string");
            serializer.endTag("", "value");
        } catch (IOException e) {
            throw new JSExn("IOException when trying to append string to XMLRPC Request: " + e);
        }
    }

    /**
	 * 
	 * @param stream
	 *            The stream object that is to be included in an XMLRPC request
	 * @param serializer
	 *            The KXmlSerializer the XMLRPC request is using
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 * @throws JSExn
	 *             Any error in making a XMLRPC request should be treated as a
	 *             JSExn and handled by the app
	 */
    private void appendStream(Fountain stream, KXmlSerializer serializer, Hash tracker) throws JSExn {
        try {
            serializer.startTag("", "value");
            serializer.startTag("", "base64");
            InputStream is = stream.getInputStream();
            byte[] buf = new byte[54];
            while (true) {
                int numread = is.read(buf, 0, 54);
                if (numread == -1) {
                    break;
                }
                byte[] writebuf = buf;
                if (numread < buf.length) {
                    writebuf = new byte[numread];
                    System.arraycopy(buf, 0, writebuf, 0, numread);
                }
                serializer.text(" " + Encode.toBase64(writebuf));
            }
            serializer.endTag("", "base64");
            serializer.endTag("", "value");
        } catch (IOException e) {
            Log.warn(XmlRpcMarshaller.class, "caught IOException while attempting to send a ByteStream via XML-RPC");
            Log.warn(XmlRpcMarshaller.class, e);
            throw new JSExn("caught IOException while attempting to send a ByteStream via XML-RPC");
        }
    }

    /**
	 * 
	 * @param bool
	 *            The boolean value that is to be included in an XMLRPC request
	 * @param serializer
	 *            The KXmlSerializer the XMLRPC request is using
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 * @throws JSExn
	 *             Any error in making a XMLRPC request should be treated as a
	 *             JSExn and handled by the app

	 */
    private void appendBoolean(Boolean bool, KXmlSerializer serializer, Hash tracker) throws JSExn {
        try {
            serializer.startTag("", "value");
            serializer.startTag("", "boolean");
            serializer.text(bool.booleanValue() ? "1" : "0");
            serializer.endTag("", "boolean");
            serializer.endTag("", "value");
        } catch (IOException e) {
            throw new JSExn("IOException when trying to append boolean to XMLRPC Request: " + e);
        }
    }

    /**
	 * 
	 * @param number
	 *            The numeric value that is to be included in an XMLRPC request
	 * @param serializer
	 *            The KXmlSerializer the XMLRPC request is using
	 * @param tracker
	 *            Tracks which objects have been serialized to prevent loops.
	 *            Explicit duplication is permitted
	 * @throws JSExn
	 *             Any error in making a XMLRPC request should be treated as a
	 *             JSExn and handled by the app

	 */
    private void appendNumber(Number number, KXmlSerializer serializer, Hash tracker) throws JSExn {
        try {
            if (number.intValue() == number.doubleValue()) {
                serializer.startTag("", "value");
                serializer.startTag("", "i4");
                serializer.text(new Integer(number.intValue()).toString());
                serializer.endTag("", "i4");
                serializer.endTag("", "value");
            } else {
                serializer.startTag("", "value");
                serializer.startTag("", "double");
                serializer.text(number.toString());
                serializer.endTag("", "double");
                serializer.endTag("", "value");
            }
        } catch (IOException e) {
            throw new JSExn("IOException when trying to append number to XMLRPC Request: " + e);
        }
    }
}
