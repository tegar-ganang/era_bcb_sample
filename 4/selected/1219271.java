package gov.sns.apps.saddam;

import java.util.*;
import java.net.*;
import java.awt.Toolkit;
import gov.sns.ca.*;
import gov.sns.xal.smf.application.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.xal.smf.impl.qualify.*;
import gov.sns.tools.data.*;

/**
 * This class contains the primary internal working objects of the 
 * settem application. E.g. which parts of the accelerator are being used.
 *
 * @author  jdg
 */
public class SaddamDocument extends AcceleratorDocument {

    /** the list of all types within the selected accelerator sequence */
    protected Vector theRawTypes = new Vector();

    /** the list of types within the selected accelerator sequence 
    * that have matches to the data table*/
    protected Vector theTypes = new Vector();

    /** the new set pointy value to use */
    protected double newSetValue = 0.;

    /** the new selected string to send out */
    protected String newSetValueString = "";

    /** make a copy of the selctedSequence so other classes in this package can use it */
    protected AcceleratorSeq theSequence;

    /** name of the selected device type */
    protected String theSelectedType;

    /** name of the selected signal handle */
    protected String theSelectedSignal;

    /** whether we are setting a preseleted allowbe value, or general user specified double value */
    protected boolean setPreselected = false;

    /** whether channel put action is confirmed to go */
    private boolean confirmed = false;

    /** Object used to do connection management  */
    private ChannelManager channelManager;

    /** the data table holding device-signal-allowed_value records this app will 
   *   deal with */
    protected SaddamData theData;

    /** A list to contain the PV names to set */
    protected Vector thePVList = new Vector();

    /** A map containing the old values in the PVs, before the reset */
    protected HashMap oldValues = new HashMap();

    /** flag to indicate whether to use wrapping to +- 180 deg ranges when setting  this value */
    protected boolean usePhaseWrap = false;

    /** workaround to avoid jca context initialization exception */
    static {
        ChannelFactory.defaultFactory().init();
    }

    /** Create a new empty document */
    public SaddamDocument() {
        theData = new SaddamData();
        channelManager = new ChannelManager(this);
    }

    /** 
     * Create a new document loaded from the URL file 
     * @param url The URL of the file to load into the new document.
     */
    public SaddamDocument(java.net.URL url) {
        this();
        if (url == null) {
            return;
        } else {
            System.out.println("Opening document: " + url.toString());
            setSource(url);
        }
    }

    /**
     * Make a main window by instantiating the my custom window.  Set the text 
     * pane to use the textDocument variable as its document.
     */
    @Override
    public void makeMainWindow() {
        mainWindow = new SaddamWindow(this);
    }

    /**
     * Save the document to the specified URL.
     * @param url The URL to which the document should be saved.
     */
    @Override
    public void saveDocumentAs(URL url) {
        setHasChanges(false);
    }

    /**
     * Convenience method for getting the main window cast to the proper subclass of XalWindow.
     * This allows me to avoid casting the window every time I reference it.
     * @return The main window cast to its dynamic runtime class
     */
    protected SaddamWindow myWindow() {
        return (SaddamWindow) mainWindow;
    }

    /**
     * Handle the accelerator changed event by displaying the elements of the 
     * accelerator in the main window.
     */
    @Override
    public void acceleratorChanged() {
        System.out.println("accelerator path: " + acceleratorFilePath);
    }

    /**
     * Handle the selected sequence changed event by displaying the elements of the 
     * selected sequence in the main window.
     */
    @Override
    public void selectedSequenceChanged() {
        theSequence = selectedSequence;
        setTypes();
        theRawTypes.clear();
        List theNodes = theSequence.getAllInclusiveNodes();
        Iterator itr = theNodes.iterator();
        while (itr.hasNext()) {
            AcceleratorNode node = ((AcceleratorNode) itr.next());
            String type;
            if (node instanceof RfCavity) type = RfCavity.s_strType; else type = node.getType();
            if (!theRawTypes.contains(type)) theRawTypes.add(type);
        }
        myWindow().updateTypeList();
        theSelectedType = "";
        theSelectedSignal = "";
        myWindow().textArea.setText("");
        confirmed = false;
    }

    /** set the types from the selected sequence */
    private void setTypes() {
        theRawTypes.clear();
        List theNodes = theSequence.getAllInclusiveNodes();
        Iterator itr = theNodes.iterator();
        while (itr.hasNext()) {
            AcceleratorNode node = ((AcceleratorNode) itr.next());
            String type;
            if (node instanceof RfCavity) type = RfCavity.s_strType; else type = node.getType();
            if (!theRawTypes.contains(type)) theRawTypes.add(type);
        }
        theTypes.clear();
        Iterator itr2 = theRawTypes.iterator();
        while (itr2.hasNext()) {
            String type = (String) itr2.next();
            Collection matches = theData.theTable.records("device", type);
            if (matches.size() > 0) theTypes.add(type);
        }
    }

    /** this method does the action after a device type is selected */
    protected void typeSelected() {
        Vector signalData = new Vector();
        theSelectedType = (String) myWindow().typeList.getSelectedValue();
        Collection matches = theData.theTable.records("device", theSelectedType);
        System.out.println("the type is " + (String) myWindow().typeList.getSelectedValue());
        Iterator itr = matches.iterator();
        while (itr.hasNext()) {
            GenericRecord rec = (GenericRecord) itr.next();
            String signal = rec.stringValueForKey("signal");
            if (!signalData.contains(signal)) signalData.add(signal);
        }
        myWindow().signalList.setListData(signalData);
        myWindow().typesReset();
        myWindow().textArea.setText("");
        confirmed = false;
    }

    /** this method does the action after a device type is selected */
    protected void signalSelected() {
        theSelectedSignal = (String) myWindow().signalList.getSelectedValue();
        Collection valueRecords;
        HashMap bindings2 = new HashMap();
        bindings2.put("device", theSelectedType);
        bindings2.put("signal", theSelectedSignal);
        valueRecords = theData.valueTable.records(bindings2);
        if (valueRecords.size() > 1) {
            Vector actions = new Vector();
            Iterator itr = valueRecords.iterator();
            while (itr.hasNext()) {
                GenericRecord rec = (GenericRecord) itr.next();
                String setVal = rec.stringValueForKey("setValue");
                actions.add(setVal);
            }
            myWindow().valueList.setListData(actions);
            myWindow().valueField.setEnabled(false);
            myWindow().valueList.setEnabled(true);
            setPreselected = true;
        } else {
            myWindow().valueField.setEnabled(true);
            myWindow().valueList.setListData(myWindow().nullValues);
            myWindow().valueList.setEnabled(false);
            setPreselected = false;
        }
        confirmed = false;
    }

    /** method called when a new value is set in the text field and is read to be fired off
    * first we create a list of PVs and check if it's OK */
    protected void confirmIt() {
        String val;
        if (!setPreselected) val = (new Double(myWindow().valueField.getValue())).toString(); else val = (String) myWindow().valueList.getSelectedValue();
        if (!makePVList()) return;
        String PVNames = new String("");
        Iterator itr = thePVList.iterator();
        while (itr.hasNext()) {
            PVNames += itr.next() + "\n";
        }
        String message = "You may now set " + val + "\nfor the following PVs:\n" + PVNames;
        myWindow().textArea.setText(message);
        System.out.println(message);
        confirmed = true;
    }

    /** make a list of channel names from the selected device type + signals 
    * return false if a list is not constructed */
    protected boolean makePVList() {
        thePVList.clear();
        usePhaseWrap = false;
        String channelName, oldNamePart, newNamePart;
        ArrayList selectedRecords;
        HashMap bindings = new HashMap();
        bindings.put("device", theSelectedType);
        bindings.put("signal", theSelectedSignal);
        selectedRecords = new ArrayList(theData.theTable.records(bindings));
        if (selectedRecords.size() == 0) {
            Toolkit.getDefaultToolkit().beep();
            String errText = "No selectons yet!";
            if (myWindow() != null) myWindow().textArea.setText(errText);
            System.err.println(errText);
            return false;
        }
        GenericRecord rec = (GenericRecord) selectedRecords.get(0);
        oldNamePart = rec.stringValueForKey("oldString");
        newNamePart = rec.stringValueForKey("newString");
        Boolean tf = (Boolean) rec.valueForKey("usePhaseWrap");
        if (tf != null) usePhaseWrap = tf.booleanValue(); else usePhaseWrap = false;
        TypeQualifier qualifier = new KindQualifier(theSelectedType);
        List nodes = theSequence.getAllInclusiveNodesWithQualifier(qualifier);
        Iterator itr = nodes.iterator();
        while (itr.hasNext()) {
            AcceleratorNode node = (AcceleratorNode) itr.next();
            String nodeName = node.getId();
            System.out.println(nodeName);
            channelName = nodeName + ":" + theSelectedSignal;
            if (oldNamePart != null && newNamePart != null) channelName = channelName.replaceFirst(oldNamePart, newNamePart);
            if (theData.substitutionMap.containsKey(channelName)) {
                channelName = (String) theData.substitutionMap.get(channelName);
            }
            thePVList.add(channelName);
        }
        return true;
    }

    /** this method fires off the PVs with the new set value and keeps track of connections */
    protected void fireSignals() {
        if (!confirmed) {
            Toolkit.getDefaultToolkit().beep();
            String errText = "Whoa there - confirm action first";
            if (myWindow() != null) myWindow().textArea.setText(errText);
            System.err.println(errText);
            return;
        }
        channelManager.checkConnections(thePVList);
        Iterator itr = thePVList.iterator();
        String action = (String) myWindow().actionChoice.getSelectedItem();
        oldValues.clear();
        while (itr.hasNext()) {
            String name = (String) itr.next();
            Channel chan = ChannelFactory.defaultFactory().getChannel(name);
            if (chan.isConnected()) {
                try {
                    if (!setPreselected) {
                        double newVal = myWindow().valueField.getValue();
                        double oldVal = chan.getValDbl();
                        oldValues.put(name, new Double(oldVal));
                        if (action.startsWith("Incr")) newVal += oldVal;
                        if (action.startsWith("Mul")) newVal *= oldVal;
                        if (usePhaseWrap) newVal = wrapPhase(newVal);
                        chan.putVal(newVal);
                    } else {
                        String newVal = (String) myWindow().valueList.getSelectedValue();
                        byte[] val = chan.getArrByte();
                        oldValues.put(name, new String(val));
                        chan.putVal(newVal.getBytes());
                    }
                } catch (Exception ex) {
                    System.out.println("exception putting channel :" + chan.getId());
                }
            }
        }
    }

    /** wrap the value to between -180 <= val <= +180 */
    private double wrapPhase(double val) {
        double valOut = val;
        if (val > 180) valOut = val - 360.;
        if (val < -180.) valOut = val + 360.;
        return valOut;
    }
}
