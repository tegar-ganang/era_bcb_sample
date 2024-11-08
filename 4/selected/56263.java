package gov.sns.apps.pvbrowser;

import gov.sns.application.Application;
import gov.sns.application.ApplicationAdaptor;
import gov.sns.application.XalDocument;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.ConnectionListener;
import gov.sns.ca.GetException;
import gov.sns.ca.IEventSinkValue;
import gov.sns.tools.database.ConnectionDictionary;
import java.net.URL;

/**
 * Allows the user to search for and examine multiple PVs.
 * 
 * @author Chris Fowlkes
 */
public class Main extends ApplicationAdaptor {

    /**
   * Holds the model for the application used to load data shared by all 
   * windows.
   */
    private PVBrowserModel model;

    /**
   * Creates a new <CODE>Main</CODE>.
   */
    public Main() {
        try {
            URL dbPropertiesURL = getClass().getResource("resources/dbconnection.properties");
            ConnectionDictionary dictionary = new ConnectionDictionary(dbPropertiesURL);
            model = new PVBrowserModel(dictionary);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            Application.displayApplicationError("IO Error", "Unable to open file resources/dbconnection.properties.", ex);
        }
    }

    /**
   * Launches the application.
   * 
   * @param args The application arguments.
   */
    public static void main(String[] args) {
        try {
            System.out.println("Starting application...");
            Application.launch(new Main());
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
            exception.printStackTrace();
            Application.displayApplicationError("Launch Exception", "Launch Exception", exception);
            System.exit(-1);
        }
    }

    /**
   * Returns the name of the application. This name appears in the title bar.
   * 
   * @return The name of the application.
   */
    @Override
    public String applicationName() {
        return "PV Browser";
    }

    /**
   * Creates a new <CODE>PVBrowserDocument</CODE> for the <CODE>URL</CODE>.
   * 
   * @param url The <CODE>URL</CODE> of the document to create.
   * @return The new <CODE>PVBrowserDocument</CODE>.
   */
    @Override
    public XalDocument newDocument(URL url) {
        return new PVBrowserSearchDocument(url, model);
    }

    /**
   * Creates a new empty <CODE>PVBrowserDocument</CODE>.
   * 
   * @return An empty <CODE>PVBrowserDocument</CODE>.
   */
    @Override
    public XalDocument newEmptyDocument() {
        return new PVBrowserSearchDocument(model);
    }

    /**
   * Provides a list of file extensions that the application can read.
   * 
   * @return The extensions of files the application can open.
   */
    @Override
    public String[] readableDocumentTypes() {
        return new String[] { "pvs" };
    }

    /**
   * Provides a list of the file extensions that the application can write.
   * 
   * @return The extensions of files the application can write.
   */
    @Override
    public String[] writableDocumentTypes() {
        return new String[] { "pvs", "db" };
    }

    /**
   * Uses channel access to get current values for a PV.
   * 
   * @param basePV An existing PV from which the PV name and field IDs will be obtained. This PV will not be changed.
   * @return The new values obtained through channel access.
   */
    public static BrowserPV probe(BrowserPV basePV) {
        String pvID = basePV.getID();
        int fieldCount = basePV.getFieldCount();
        BrowserPV newPV = new BrowserPV(pvID, basePV.getType());
        for (int i = 0; i < fieldCount; i++) {
            BrowserPVField field = basePV.getFieldAt(i);
            probe(newPV, field.getName(), field.getType());
        }
        return newPV;
    }

    /**
   * Uses channel access to determine the value for a given field in a PV.
   * 
   * @param pv The PV to which to add the field.
   * @param fieldName The name of the field to probe through channel access.
   * @param fieldType The type of the field to probe.
   */
    public static void probe(final BrowserPV pv, final String fieldName, final String fieldType) {
        pv.setProbeData(true);
        StringBuffer channelName = new StringBuffer(pv.getID());
        channelName.append(".");
        channelName.append(fieldName);
        String channelNameString = channelName.toString();
        final Channel channel = ChannelFactory.defaultFactory().getChannel(channelNameString);
        channel.addConnectionListener(new ConnectionListener() {

            public void connectionMade(Channel channel) {
                try {
                    channel.getValueCallback(new IEventSinkValue() {

                        public void eventValue(ChannelRecord record, Channel chan) {
                            String value = record.stringValue();
                            BrowserPVField newField = new BrowserPVField(fieldName, value, fieldType);
                            pv.addField(newField);
                        }
                    });
                    Channel.flushIO();
                } catch (ConnectionException e) {
                    Application.displayError(e);
                    e.printStackTrace();
                } catch (GetException e) {
                    Application.displayError(e);
                    e.printStackTrace();
                }
            }

            public void connectionDropped(Channel channel) {
            }
        });
        channel.requestConnection();
        Channel.flushIO();
    }
}
