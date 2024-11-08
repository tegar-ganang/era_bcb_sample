package org.javadelic.burrow;

import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.FileWriter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.util.Date;
import java.util.Stack;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.SecurityException;
import org.javadelic.burrow.query.Agents;
import org.javadelic.burrow.query.Roster;

/**
 * JabParser provides the SAX parser 
 */
public class JabParser implements ContentHandler, ErrorHandler {

    public static boolean bDebug = false;

    private String logFileName = new String("parser.log");

    private int msgType = JabUtil.UNDEFINED;

    private Stack parsingContext = null;

    private StringBuffer buffer = null;

    private Agents agents;

    /** 
     * Used for JabEvent 
     */
    JabMessage message = null;

    JabPresence presence;

    JabIq iq;

    JabExtension extension;

    String namespace;

    Object queryObj;

    boolean inQuery = false;

    private String parserClass = "org.apache.xerces.parsers.SAXParser";

    private ErrorHandler errorHandler = null;

    private ContentHandler contentHandler = null;

    private Locator locator = null;

    private String packageName;

    private String uri = null;

    private InputSource carIS;

    int warnings = 0;

    int errors = 0;

    int depth = 0;

    BufferedWriter logStream = null;

    public JabParser() {
        packageName = this.getClass().getPackage().getName() + ".query.";
        contentHandler = this;
        errorHandler = this;
        try {
            this.logStream = new BufferedWriter(new FileWriter(logFileName));
        } catch (Exception e) {
            System.out.println("Can not write  to " + logFileName + " e=" + e);
        }
    }

    /**
	  * Close the log file
	  */
    public void close() {
        try {
            logStream.close();
        } catch (Exception e) {
            System.out.println("Problem with close e=" + e);
        }
    }

    /**
	  * Debugging convenience method
	  */
    private void debug(String s) {
        if (bDebug) {
            System.out.println(s);
        }
    }

    /**
	  * Main parsing routine.
	  * The string read from the server is sent to this method.  A Object is returned
	  * of the correct Jabber element (MESSAGE, PRESENCE, IQ).
	  * If the parsing fails, return null.
	  */
    public Object parseMessage(String messageFromServer) {
        Object result = null;
        try {
            int bytes_read = messageFromServer.length();
            char[] ch_buffer = new char[bytes_read + 1];
            for (int i = 0; i < messageFromServer.length(); i++) {
                ch_buffer[i] = messageFromServer.charAt(i);
            }
            CharArrayReader car = new CharArrayReader(ch_buffer, 0, bytes_read);
            carIS = new InputSource(car);
            if (null != logStream) {
                String strDate = "" + (new Date());
                logStream.write(strDate, 0, strDate.length());
                logStream.newLine();
                logStream.write(ch_buffer, 0, bytes_read);
                logStream.newLine();
                logStream.flush();
            }
            XMLReader parser = XMLReaderFactory.createXMLReader(parserClass);
            parser.setContentHandler(contentHandler);
            parser.setErrorHandler(errorHandler);
            parser.setFeature("http://xml.org/sax/features/validation", false);
            debug("Calling .parse! car=" + car);
            parser.parse(carIS);
            debug("Done with .parse");
            switch(msgType) {
                case JabUtil.MESSAGE:
                    result = message;
                    break;
                case JabUtil.PRESENCE:
                    result = presence;
                    break;
                case JabUtil.IQ:
                    result = iq;
                    break;
            }
        } catch (IOException e) {
            System.err.println(e);
        } catch (SAXException e) {
            System.out.println("SAXException: " + e);
            System.out.println("Could not parse messageFromServer=" + messageFromServer);
            int eCol = locator.getColumnNumber();
            System.out.println("endElement locator.getColumnNumber()=" + eCol);
            System.out.println("endElement locator.getLineNumber()=" + locator.getLineNumber());
        } finally {
            return result;
        }
    }

    public void setDocumentLocator(Locator locator) {
        debug("setDocumentLocator");
        this.locator = locator;
    }

    public void startDocument() throws SAXException {
        debug("\n********************\nstartDocument");
        parsingContext = new Stack();
        buffer = new StringBuffer();
        msgType = JabUtil.UNDEFINED;
    }

    public void endDocument() throws SAXException {
        debug("endDocument\n********************\n");
    }

    public void processingInstruction(String target, String data) {
        debug("processingInstruction");
    }

    public void startPrefixMapping(String prefix, String uri) {
        debug("startPrefixMapping");
    }

    public void endPrefixMapping(String prefix) {
        debug("endPrefixMapping");
    }

    public void startElement(String namespaceURI, String localName, String rawName, Attributes atts) throws SAXException {
        debug("startElement " + localName);
        debug("  atts.getValue(\"from\")=" + atts.getValue("from"));
        if (inQuery) {
            getQueryAttributes(atts);
        } else {
            if (localName.equals("message")) {
                msgType = JabUtil.MESSAGE;
                message = new JabMessage(atts);
                debug("  msgType = MESSAGE (" + msgType + ")");
            } else if (localName.equals("presence")) {
                msgType = JabUtil.PRESENCE;
                presence = new JabPresence(atts);
                debug("  msgType = PRESENCE (" + msgType + ")");
            } else if (localName.equals("iq")) {
                msgType = JabUtil.IQ;
                iq = new JabIq(atts);
                debug("  msgType = IQ (" + msgType + ")");
            } else if (localName.equals("query")) {
                debug("\n** Processing query element - " + namespaceURI);
                inQuery = true;
                namespace = "";
                queryObj = null;
                int index = namespaceURI.lastIndexOf(":");
                if (index >= 0) {
                    namespace = namespaceURI.substring(index + 1);
                    String className = namespace.substring(0, 1).toUpperCase() + namespace.substring(1);
                    debug(" -- extracted namespace = " + namespace);
                    try {
                        queryObj = Class.forName(packageName + className).newInstance();
                        iq.setQuery(namespace, className, queryObj);
                    } catch (ClassNotFoundException e) {
                        System.out.println("Class not found: " + className);
                    } catch (InstantiationException e) {
                    } catch (IllegalAccessException e) {
                    }
                }
            } else if (localName.equals("x")) {
                debug("\n** Creating extension - " + namespaceURI + "**");
                extension = new JabExtension(namespaceURI);
                for (int i = 0; i < atts.getLength(); i++) {
                    extension.put(atts.getLocalName(i), atts.getValue(i));
                }
                switch(msgType) {
                    case JabUtil.MESSAGE:
                        message.setX(extension);
                        break;
                    case JabUtil.PRESENCE:
                        presence.setX(extension);
                        break;
                }
            }
        }
        buffer.setLength(0);
    }

    public void endElement(String namespaceURI, String localName, String rawName) throws SAXException {
        debug("endElement " + localName);
        debug("  buffer.length()=" + buffer.length());
        if (buffer.length() == 1) {
            debug("  buffer[0]==" + (int) buffer.charAt(0));
        }
        if ((localName.equals("presence")) || (localName.equals("message")) || (localName.equals("iq"))) {
            return;
        }
        if (localName.equals("query")) {
            inQuery = false;
            return;
        }
        if (localName.equals("x")) {
            return;
        }
        String value = new String(buffer);
        buffer.setLength(0);
        switch(msgType) {
            case JabUtil.MESSAGE:
                callSetterFor(message, localName, value);
                break;
            case JabUtil.PRESENCE:
                callSetterFor(presence, localName, value);
                break;
            case JabUtil.IQ:
                callSetterFor(queryObj, localName, value);
                break;
        }
    }

    public void characters(char[] ch, int start, int len) throws SAXException {
        debug("    characters ch=" + ch + " start=" + start + " len=" + len);
        String str = new String(ch, start, len);
        debug("    str=" + str);
        buffer.append(ch, start, len);
    }

    public void ignorableWhitespace(char[] ch, int start, int end) throws SAXException {
        debug("ignorableWhitespace");
    }

    public void skippedEntity(String name) throws SAXException {
        debug("skippedEntity");
    }

    public void warning(SAXParseException exception) throws SAXException {
        debug("WARNING: " + exception.toString());
        warnings++;
    }

    public void error(SAXParseException exception) throws SAXException {
        debug("ERROR: " + exception.toString());
        errors++;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        debug("+----------------------------------------------------------------------------+");
        debug("| FATAL ERROR: " + exception.getMessage() + " at " + locator.getColumnNumber());
        debug("+----------------------------------------------------------------------------+");
        errors++;
    }

    private void getQueryAttributes(Attributes atts) throws SAXException {
        if (atts.getLength() > 0) {
            try {
                Class clazz = queryObj.getClass();
                Class[] params = { Attributes.class };
                Method meth = clazz.getMethod("setAttributes", params);
                Object[] args = { atts };
                meth.invoke(queryObj, args);
            } catch (Exception e) {
                System.out.println("Class " + namespace + " not found");
            }
        }
    }

    private void callSetterFor(Object o, String name, String parameter) {
        String methodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        Class clazz = o.getClass();
        if (parameter.trim().length() == 0) {
            try {
                Class[] c = {};
                Method m = clazz.getMethod(methodName, c);
                Object[] args = {};
                m.invoke(o, args);
            } catch (NoSuchMethodException e) {
            } catch (SecurityException e) {
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            }
        } else {
            try {
                Class[] c = { String.class };
                Method m = clazz.getMethod(methodName, c);
                Object[] args = { parameter };
                m.invoke(o, args);
            } catch (NoSuchMethodException e) {
                System.out.println("No setter method for " + methodName);
            } catch (SecurityException e) {
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            }
        }
    }
}
