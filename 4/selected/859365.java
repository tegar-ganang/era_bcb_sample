package com.dalsemi.onewire.application.tag;

import org.xml.sax.DocumentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.AttributeList;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.utils.OWPath;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.application.tag.*;
import java.util.Vector;
import java.util.Stack;
import java.util.EmptyStackException;

/**
 * SAX parser handler that handles XML 1-wire tags.
 */
class TAGHandler implements ErrorHandler, DocumentHandler {

    /**
    * Method setDocumentLocator
    *
    *
    * @param locator
    *
    */
    public void setDocumentLocator(Locator locator) {
    }

    /**
    * Method startDocument
    *
    *
    * @throws SAXException
    *
    */
    public void startDocument() throws SAXException {
        deviceList = new Vector();
        clusterStack = new Stack();
        branchStack = new Stack();
        branchVector = new Vector();
        branchVectors = new Vector();
        branchPaths = new Vector();
    }

    /**
    * Method endDocument
    *
    *
    * @throws SAXException
    *
    */
    public void endDocument() throws SAXException {
        TaggedDevice device;
        OWPath branchPath;
        Vector singleBranchVector;
        for (int i = 0; i < deviceList.size(); i++) {
            device = (TaggedDevice) deviceList.elementAt(i);
            device.setOWPath(adapter, device.getBranches());
        }
        for (int i = 0; i < branchVectors.size(); i++) {
            singleBranchVector = (Vector) branchVectors.elementAt(i);
            branchPath = new OWPath(adapter);
            for (int j = 0; j < singleBranchVector.size(); j++) {
                device = (TaggedDevice) singleBranchVector.elementAt(i);
                branchPath.add(device.getDeviceContainer(), device.getChannel());
            }
            branchPaths.addElement(branchPath);
        }
    }

    /**
    * Method startElement
    *
    *
    * @param name
    * @param atts
    *
    * @throws SAXException
    *
    */
    public void startElement(String name, AttributeList atts) throws SAXException {
        currentElement = name;
        String attributeAddr = "null";
        String attributeType = "null";
        String className;
        int i = 0;
        if (name.toUpperCase().equals("CLUSTER")) {
            for (i = 0; i < atts.getLength(); i++) {
                if (atts.getName(i).toUpperCase().equals("NAME")) {
                    clusterStack.push(atts.getValue(i));
                }
            }
        }
        if (name.toUpperCase().equals("SENSOR") || name.toUpperCase().equals("ACTUATOR") || name.toUpperCase().equals("BRANCH")) {
            for (i = 0; i < atts.getLength(); i++) {
                String attName = atts.getName(i);
                if (attName.toUpperCase().equals("ADDR")) {
                    attributeAddr = atts.getValue(i);
                }
                if (attName.toUpperCase().equals("TYPE")) {
                    attributeType = atts.getValue(i);
                }
            }
            if (name.toUpperCase().equals("BRANCH")) {
                attributeType = "branch";
                currentDevice = new TaggedDevice();
            } else {
                if (attributeType.indexOf(".") > 0) {
                    className = attributeType;
                } else className = "com.dalsemi.onewire.application.tag." + attributeType;
                try {
                    Class genericClass = Class.forName(className);
                    currentDevice = (TaggedDevice) genericClass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Can't load 1-Wire Tag Type class (" + className + "): " + e.getMessage());
                }
            }
            currentDevice.setDeviceContainer(adapter, attributeAddr);
            currentDevice.setDeviceType(attributeType);
            currentDevice.setClusterName(getClusterStackAsString(clusterStack, "/"));
            currentDevice.setBranches((Vector) branchStack.clone());
            if (name.equals("branch")) {
                branchStack.push(currentDevice);
                branchVector.addElement(currentDevice);
                deviceList.addElement(currentDevice);
            }
        }
    }

    /**
    * Method endElement
    *
    *
    * @param name
    *
    * @throws SAXException
    *
    */
    public void endElement(String name) throws SAXException {
        if (name.toUpperCase().equals("SENSOR") || name.toUpperCase().equals("ACTUATOR")) {
            deviceList.addElement(currentDevice);
            currentDevice = null;
        }
        if (name.toUpperCase().equals("BRANCH")) {
            branchVectors.addElement(branchStack.clone());
            branchStack.pop();
            currentDevice = null;
        }
        if (name.toUpperCase().equals("CLUSTER")) {
            clusterStack.pop();
        }
    }

    /**
    * Method characters
    *
    *
    * @param ch
    * @param start
    * @param length
    *
    * @throws SAXException
    *
    */
    public void characters(char ch[], int start, int length) throws SAXException {
        if (currentElement.toUpperCase().equals("LABEL")) {
            if (currentDevice == null) {
                try {
                    currentDevice = (TaggedDevice) branchStack.peek();
                    currentDevice.setLabel(new String(ch, start, length));
                    currentDevice = null;
                } catch (EmptyStackException ese) {
                }
            } else {
                currentDevice.setLabel(new String(ch, start, length));
            }
        }
        if (currentElement.toUpperCase().equals("CHANNEL")) {
            if (currentDevice == null) {
                try {
                    currentDevice = (TaggedDevice) branchStack.peek();
                    currentDevice.setChannelFromString(new String(ch, start, length));
                    currentDevice = null;
                } catch (EmptyStackException ese) {
                }
            } else {
                currentDevice.setChannelFromString(new String(ch, start, length));
            }
        }
        if (currentElement.toUpperCase().equals("MAX")) {
            currentDevice.max = new String(ch, start, length);
        }
        if (currentElement.toUpperCase().equals("MIN")) {
            currentDevice.min = new String(ch, start, length);
        }
        if (currentElement.toUpperCase().equals("INIT")) {
            currentDevice.setInit(new String(ch, start, length));
        }
    }

    /**
    * Method ignorableWhitespace
    *
    *
    * @param ch
    * @param start
    * @param length
    *
    * @throws SAXException
    *
    */
    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
    }

    /**
    * Method processingInstruction
    *
    *
    * @param target
    * @param data
    *
    * @throws SAXException
    *
    */
    public void processingInstruction(String target, String data) throws SAXException {
    }

    /**
    * Method getTaggedDeviceList
    *
    *
    * @return
    *
    */
    public Vector getTaggedDeviceList() {
        return deviceList;
    }

    /**
    * Method setAdapter
    *
    *
    * @param adapter
    *
    * @throws com.dalsemi.onewire.OneWireException
    *
    */
    public void setAdapter(DSPortAdapter adapter) throws com.dalsemi.onewire.OneWireException {
        this.adapter = adapter;
    }

    /**
    * Method fatalError
    *
    *
    * @param exception
    *
    * @throws SAXParseException
    *
    */
    public void fatalError(SAXParseException exception) throws SAXParseException {
        System.err.println(exception);
        throw exception;
    }

    /**
    * Method error
    *
    *
    * @param exception
    *
    * @throws SAXParseException
    *
    */
    public void error(SAXParseException exception) throws SAXParseException {
        System.err.println(exception);
        throw exception;
    }

    /**
    * Method warning
    *
    *
    * @param exception
    *
    */
    public void warning(SAXParseException exception) {
        System.err.println(exception);
    }

    /**
    * Method getAllBranches
    *
    *
    * @param no parameters
    *
    * @return Vector of all TaggedDevices of type "branch".
    *
    */
    public Vector getAllBranches() {
        return branchVector;
    }

    /**
    * Method getAllBranchPaths
    *
    *
    * @param no parameters
    *
    * @return Vector of all possible OWPaths.
    *
    */
    public Vector getAllBranchPaths() {
        return branchPaths;
    }

    /**
    * Method getClusterStackAsString
    *
    *
    * @param clusters
    * @param separator
    *
    * @return
    *
    */
    private String getClusterStackAsString(Stack clusters, String separator) {
        String returnString = "";
        for (int j = 0; j < clusters.size(); j++) {
            returnString = returnString + separator + (String) clusters.elementAt(j);
        }
        return returnString;
    }

    /** Field adapter           */
    private DSPortAdapter adapter;

    /** Field currentElement           */
    private String currentElement;

    /** Field currentDevice           */
    private TaggedDevice currentDevice;

    /** Field deviceList           */
    private Vector deviceList;

    /** Field clusterStack           */
    private Stack clusterStack;

    /** Field branchStack           */
    private Stack branchStack;

    /** Field branchVector           */
    private Vector branchVector;

    /** Field branchVectors          */
    private Vector branchVectors;

    /** Field branchPaths            */
    private Vector branchPaths;
}
