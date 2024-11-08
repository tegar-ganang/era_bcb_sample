package j2meonrails;

import java.io.*;
import java.util.*;
import org.kxml2.io.*;
import org.kxml2.kdom.*;
import org.xmlpull.v1.*;
import javax.microedition.io.*;

public class J2meResource {

    /** See setResponseType. */
    public static final int RESPONSETYPE_XML = 1001;

    /** See setResponseType. */
    public static final int RESPONSETYPE_JSON = 1002;

    private static String _siteURL;

    private static String _user;

    private static String _password;

    private static int _responseType;

    private String _className;

    private String _ID;

    private boolean _isNewObject;

    private Hashtable _properties;

    /** Configuration: set server/site/host URL e.g. "http://localhost:3000" */
    public static void setSite(String siteURL) {
        _siteURL = siteURL;
    }

    /** Configuration: set HTTP username e.g. "myuser". Currently not used! */
    public static void setUser(String user) {
    }

    /** Configuration: set HTTP password e.g. "mypassword". Currently not used! */
    public static void setPassword(String password) {
    }

    /** Configuration: set response type, either J2meResource.RESPONSETYPE_XML or J2meResource.RESPONSETYPE_JSON. Currently not used! */
    public static void setResponseType(int responseType) {
    }

    /** Data manipulation: find a specific ActiveRecord object. */
    public static J2meResource findRemote(String className, String ID) throws IOException {
        J2meResource remoteObj = new J2meResource(className);
        remoteObj._isNewObject = false;
        remoteObj._ID = ID;
        remoteObj._properties = (Hashtable) doHttpRequest(remoteObj.getObjectURL(), "GET", null);
        return remoteObj;
    }

    /** Data manipulation: Get a list of ActiveRecord objects, returns Vector of J2meResource. */
    public static Vector findAllRemote(String className, String criteria) throws IOException {
        J2meResource remoteObj = new J2meResource(className);
        Vector hashVector = (Vector) doHttpRequest(remoteObj.getClassURL() + "?" + criteria, "GET", null);
        Vector returnVector = new Vector();
        for (int i = 0; i < hashVector.size(); i++) {
            returnVector.addElement(new J2meResource(className, (Hashtable) hashVector.elementAt(i)));
        }
        return returnVector;
    }

    /** Do a HTTP request, return a Hashtable of the XML results. Example: doRemoteRequest("mycontroller.xml", "name=Joe&age=32", "POST", "<xml>my xml</xml>"); */
    public static Hashtable doRemoteRequest(String controllerName, String parameters, String httpMethod, String postValue) throws IOException {
        return (Hashtable) doHttpRequest(_siteURL + "/" + controllerName + "?" + parameters, httpMethod, postValue);
    }

    /** Construct the J2meResource, e.g. J2meResource user = new J2meResource("User", myHash); */
    public J2meResource(String className, Hashtable properties) {
        _className = capitalize(className);
        _isNewObject = true;
        _properties = properties;
    }

    /** Construct the J2meResource, e.g. J2meResource user = new J2meResource("User"); */
    public J2meResource(String className) {
        this(className, new Hashtable());
    }

    /** Data manipulation: save/update the ActiveRecord object. */
    public boolean saveRemote() throws IOException {
        if (_isNewObject) {
            _properties = (Hashtable) doHttpRequest(getClassURL(), "POST", this.toXML());
        } else {
            _properties = (Hashtable) doHttpRequest(getObjectURL(), "PUT", this.toXML());
        }
        return true;
    }

    /** Data manipulation: update the ActiveRecord object. */
    public boolean updateRemote() throws IOException {
        return saveRemote();
    }

    /** Data manipulation: delete the ActiveRecord object. */
    public boolean destroyRemote() throws IOException {
        doHttpRequest(getObjectURL(), "DELETE", null);
        return true;
    }

    /** Data manipulation: get a value from the ActiveRecord object with a given name. All values are returned as Strings or Hashtables. */
    public Object getProperty(String propertyName) {
        return _properties.get(propertyName);
    }

    /** Convenience method - see getProperty for more details. */
    public String getPropertyAsString(String propertyName) {
        return (String) getProperty(propertyName);
    }

    /** Convenience method - see getProperty for more details. */
    public int getPropertyAsInt(String propertyName) {
        return Integer.parseInt(getPropertyAsString(propertyName));
    }

    /** Convenience method - see getProperty for more details. */
    public boolean getPropertyAsBoolean(String propertyName) {
        return getPropertyAsString(propertyName).equals("true");
    }

    /** Data manipulation: set a value from the ActiveRecord object with a given name. All values are stated as Strings or Hashtables. */
    public void setProperty(String propertyName, Object value) {
        _properties.put(propertyName, value);
    }

    /** Convenience method - see setProperty(String propertyName, Object value) for more details. */
    public void setProperty(String propertyName, long value) {
        _properties.put(propertyName, value + "");
    }

    /** Data manipulation: replace all properties/values. */
    public void setPropertiesHash(Hashtable propertiesHash) {
        _properties = propertiesHash;
    }

    /** toString */
    public String toString() {
        return "{ " + getClassReference() + ": " + _properties.toString() + " }";
    }

    /** Format the ActiveRecord object as XML: e.g. "<user><name>Tom</name></user>" */
    public String toXML() {
        String returnStr = encloseTag(getClassReference(), hashtableToXML(_properties));
        return returnStr;
    }

    /** Format the ActiveRecord object as URL: e.g. "name=John&password=Doe" */
    public String toURL() {
        String name;
        Object value;
        String returnStr = "";
        Enumeration e = _properties.keys();
        while (e.hasMoreElements()) {
            name = (String) e.nextElement();
            value = (String) _properties.get(name);
            if (returnStr.length() > 0) returnStr += "&";
            returnStr += name + "=" + value;
        }
        return returnStr;
    }

    /** Format a Enumeration object as XML: e.g. "<user><name>Tom</name></user>" */
    private static String vectorToXML(String name, Vector vector) {
        Object value;
        String returnStr = "";
        for (int i = 0; i < vector.size(); i++) {
            value = vector.elementAt(i);
            returnStr += encloseTag(name, elementToXML(name, value));
        }
        return returnStr;
    }

    /** Format a Hashtable object as XML: e.g. "<user><name>Tom</name></user>" */
    private static String hashtableToXML(Hashtable hashtable) {
        String name;
        Object value;
        String returnStr = "";
        Enumeration e = hashtable.keys();
        while (e.hasMoreElements()) {
            name = (String) e.nextElement();
            value = hashtable.get(name);
            returnStr += encloseTag(name, elementToXML(name, value));
        }
        return returnStr;
    }

    /** */
    private static String encloseTag(String name, String value) {
        String returnStr = "";
        returnStr += "<" + name + ">";
        returnStr += value;
        returnStr += "</" + name + ">";
        return returnStr;
    }

    /** Decide what kind of Object to use. */
    private static String elementToXML(String name, Object value) {
        if (value.getClass().getName().equals("java.lang.String")) {
            return (String) value;
        }
        if (value.getClass().getName().equals("java.util.Vector")) {
            String singularName = name.substring(0, name.length() - 1);
            return vectorToXML(singularName, (Vector) value);
        }
        if (value.getClass().getName().equals("java.util.Hashtable")) {
            return hashtableToXML((Hashtable) value);
        }
        return null;
    }

    /** "hello" -> "Hello" */
    private static String capitalize(String inputString) {
        return inputString.substring(0, 1).toUpperCase() + inputString.substring(1, inputString.length()).toLowerCase();
    }

    private static String pluralize(String inputString) {
        return inputString + "s";
    }

    /** HTTP: Do a HTTP request, return a Hashtable or Vector of the XML results. */
    private static Object doHttpRequest(String URL, String httpMethod, String postValue) throws IOException {
        System.out.println("doHttpRequest: " + URL + " " + httpMethod + " " + postValue);
        HttpConnection connection = null;
        DataInputStream input = null;
        DataOutputStream output = null;
        if (httpMethod.equals(HttpConnection.GET) || httpMethod.equals("DELETE")) {
            System.out.println("  " + httpMethod + " = readonly");
            connection = (HttpConnection) Connector.open(URL);
            connection.setRequestMethod(httpMethod);
        } else {
            System.out.println("  " + httpMethod + " = read+write");
            connection = (HttpConnection) Connector.open(URL, Connector.READ_WRITE);
            connection.setRequestMethod(httpMethod);
            connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8");
            if (postValue != null) {
                output = connection.openDataOutputStream();
                byte[] request_body = postValue.getBytes();
                for (int i = 0; i < request_body.length; i++) {
                    output.writeByte(request_body[i]);
                }
            }
        }
        input = new DataInputStream(connection.openInputStream());
        Object returnObject;
        returnObject = processXmlDom(input);
        try {
            if (connection != null) connection.close();
            if (input != null) input.close();
            if (output != null) output.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return returnObject;
    }

    /** Returns Hashtable or Vector. */
    public static Object processXmlDom(InputStream inputStream) throws IOException {
        try {
            KXmlParser parser = new KXmlParser();
            parser.setInput(inputStream, "UTF-8");
            Document dom = new Document();
            dom.parse(parser);
            Object returnObject = traverse(dom.getRootElement());
            return returnObject;
        } catch (XmlPullParserException e) {
            throw new IOException(e.toString());
        }
    }

    /** Recursive function - parse XML with kXML, return Hashtable or String. */
    private static Object traverse(Element parentElement) {
        Hashtable returnHash = new Hashtable();
        Vector childVector = null;
        for (int childIndex = 0; childIndex < parentElement.getChildCount(); childIndex++) {
            switch(parentElement.getType(childIndex)) {
                case Node.TEXT:
                    String textValue = parentElement.getText(childIndex).trim();
                    if (!textValue.equals("")) {
                        String nodeName = parentElement.getName();
                        return textValue;
                    }
                    break;
                case Node.ELEMENT:
                    Element childElement = parentElement.getElement(childIndex);
                    if (parentElement.getName().equals(pluralize(childElement.getName()))) {
                        if (childVector == null) childVector = new Vector();
                        childVector.addElement(traverse(childElement));
                    } else {
                        String nodeName = childElement.getName();
                        returnHash.put(nodeName, traverse(childElement));
                    }
                    break;
            }
        }
        if (childVector != null) return childVector;
        return returnHash;
    }

    /** Rails object, e.g. "User" */
    public String getClassName() {
        return _className;
    }

    /** Rails object, e.g. "user" */
    private String getClassReference() {
        return _className.toLowerCase();
    }

    /** Rails object */
    private String getClassReferencePluralized() {
        return pluralize(getClassReference());
    }

    /** getClassURL: e.g. "http://www2.zyked.com/users.xml" */
    private String getClassURL() {
        return _siteURL + "/" + getClassReferencePluralized() + ".xml";
    }

    /** getObjectURL: e.g. "http://www2.zyked.com/users/Tom.xml" */
    private String getObjectURL() {
        return _siteURL + "/" + getClassReferencePluralized() + "/" + _ID + ".xml";
    }
}
