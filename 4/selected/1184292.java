package org.rdv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.rdv.datapanel.DataPanel;
import org.rdv.rbnb.Channel;
import org.rdv.rbnb.RBNBController;
import org.rdv.ui.DataPanelContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A class to manage all the data panels.
 * 
 * @author  Jason P. Hanley
 * @author  Wei Deng
 */
public class DataPanelManager {

    /**
	 * The logger for this class.
	 */
    static Log log = org.rdv.LogFactory.getLog(DataPanelManager.class.getName());

    /** the single instance of this class */
    private static DataPanelManager instance;

    /**
	 * A reference to the data panel container for the data panels
	 * to add their ui component too.
	 */
    private DataPanelContainer dataPanelContainer;

    /**
	 * A list of all the data panels.
	 */
    private List<DataPanel> dataPanels;

    /**
	 * A list of registered extensions
	 */
    private List<Extension> extensions;

    /**
	 * The name of the extensions configuration file
	 */
    private static String extensionsConfigFileName = "config/extensions.xml";

    /**
   * Gets the single instance of this class.
   * 
   * @return  the single instance of this class
   */
    public static DataPanelManager getInstance() {
        if (instance == null) {
            instance = new DataPanelManager();
        }
        return instance;
    }

    /** 
	 * The constructor for the data panel manager. This initializes the
	 * data panel container and the list of registered data panels.
	 */
    private DataPanelManager() {
        dataPanelContainer = new DataPanelContainer();
        dataPanels = new ArrayList<DataPanel>();
        extensions = new ArrayList<Extension>();
        loadExtenionManifest();
    }

    /**
	 * Load the the  extension manifest file describing all the
	 * extensions and their properties.
	 */
    private void loadExtenionManifest() {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xmlDocument = documentBuilder.parse(DataViewer.getResourceAsStream(extensionsConfigFileName));
            NodeList extensionNodes = xmlDocument.getElementsByTagName("extension");
            for (int i = 0; i < extensionNodes.getLength(); i++) {
                Node extensionNode = extensionNodes.item(i);
                String extensionID = null;
                String extensionName = null;
                List<String> mimeTypes = new ArrayList<String>();
                NodeList parameters = extensionNode.getChildNodes();
                for (int j = 0; j < parameters.getLength(); j++) {
                    Node parameterNode = parameters.item(j);
                    String parameterName = parameterNode.getNodeName();
                    if (parameterName.equals("id")) {
                        extensionID = parameterNode.getFirstChild().getNodeValue();
                    } else if (parameterName.equals("name")) {
                        extensionName = parameterNode.getFirstChild().getNodeValue();
                    } else if (parameterName.equals("mimeTypes")) {
                        NodeList mimeTypeNodes = parameterNode.getChildNodes();
                        for (int k = 0; k < mimeTypeNodes.getLength(); k++) {
                            Node mimeTypeNode = mimeTypeNodes.item(k);
                            if (mimeTypeNode.getNodeName().equals("mimeType")) {
                                mimeTypes.add(mimeTypeNode.getFirstChild().getNodeValue());
                            }
                        }
                    }
                }
                Extension extension = new Extension(extensionID, extensionName, mimeTypes);
                extensions.add(extension);
                log.info("Registered extension " + extensionName + " (" + extensionID + ").");
            }
        } catch (Exception e) {
            log.error("Failed to fully load the extension manifest.");
            e.printStackTrace();
        }
    }

    /**
	 * Return the registered extension specified by the given class.
	 * 
	 * @param extensionClass  the class of the desired extension
	 * @return                the extension, or null if the class wasn't found
	 */
    public Extension getExtension(Class extensionClass) {
        for (int i = 0; i < extensions.size(); i++) {
            Extension extension = extensions.get(i);
            if (extension.getID().equals(extensionClass.getName())) {
                return extension;
            }
        }
        return null;
    }

    /**
   * Return the registered extension specified by the given class name.
   * 
   * @param extensionID              the class name of the desired extension
   * @return                         the extension, or null if the class wasn't found
   * @throws ClassNotFoundException
   */
    public Extension getExtension(String extensionID) throws ClassNotFoundException {
        Class extensionClass = Class.forName(extensionID);
        return getExtension(extensionClass);
    }

    /**
	 * Return a list of registered extensions.
	 * 
	 * @return  an array list of extensions
	 * @see     Extension
	 */
    public List<Extension> getExtensions() {
        return extensions;
    }

    /**
   * Return a list of extension that support the channel.
   * 
   * @param channelName  the channel to support
   * @return             a list of supported extensions
   */
    public List<Extension> getExtensions(String channelName) {
        Channel channel = RBNBController.getInstance().getChannel(channelName);
        String mime = null;
        if (channel != null) {
            mime = channel.getMetadata("mime");
        }
        List<Extension> usefulExtensions = new ArrayList<Extension>();
        for (int i = 0; i < extensions.size(); i++) {
            Extension extension = (Extension) extensions.get(i);
            List<String> mimeTypes = extension.getMimeTypes();
            for (int j = 0; j < mimeTypes.size(); j++) {
                String mimeType = mimeTypes.get(j);
                if (mimeType.equals(mime)) {
                    usefulExtensions.add(extension);
                }
            }
        }
        return usefulExtensions;
    }

    /**
   * Returns a list of extensions that support the channels. An extension must
   * support each channel to be included on this list.
   * 
   * @param channels  the channels to support
   * @return          a list of supported extensions
   */
    public List<Extension> getExtensions(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return Collections.emptyList();
        }
        List<Extension> extensions = getExtensions(channels.get(0));
        for (int i = 1; i < channels.size(); i++) {
            String channel = channels.get(i);
            List<Extension> channelExtensions = getExtensions(channel);
            for (int j = extensions.size() - 1; j >= 0; j--) {
                Extension extension = extensions.get(j);
                if (!channelExtensions.contains(extension)) {
                    extensions.remove(extension);
                }
            }
        }
        return extensions;
    }

    /**
   * Return the default extension for the channel or null if there is none.
   * 
   * @param channelName  the channel to use 
   * @return             the default extension or null if there is none
   */
    public Extension getDefaultExtension(String channelName) {
        Channel channel = RBNBController.getInstance().getChannel(channelName);
        String mime = null;
        if (channel != null) {
            mime = channel.getMetadata("mime");
        }
        return findExtension(mime);
    }

    /**
	 * Returns the data panel container where data panels can
	 * add their ui components too.
	 * 
	 * @return  the data panel container
	 */
    public DataPanelContainer getDataPanelContainer() {
        return dataPanelContainer;
    }

    /**
	 * Calls the closePanel method on the specified data panel.
	 * 
	 * After this method has been called this data panel instance has
	 * reached the end of its life cycle.
	 * 
	 * @param dataPanel  the data panel to be closed
	 * @see              DataPanel#closePanel()
	 */
    public void closeDataPanel(DataPanel dataPanel) {
        dataPanel.closePanel();
        dataPanels.remove(dataPanel);
    }

    /**
	 * Calls the <code>closePanel</code> method on each registered data panel.
	 * 
	 * @see    DataPanel#closePanel()
	 */
    public void closeAllDataPanels() {
        DataPanel dataPanel;
        for (int i = dataPanels.size() - 1; i >= 0; i--) {
            dataPanel = (DataPanel) dataPanels.get(i);
            closeDataPanel(dataPanel);
        }
    }

    /**
	 * View the channel specified by the given channel name. This will
	 * cause the data panel manager to look for the default viewer for
	 * the mime type of the viewer.
	 * 
	 * If the channel has no mime type, it is assumed to be numeric data
	 * unless the channel ends with .jpg, in which case it is assumed to
	 * be image or video data.
	 * 
	 * @param channelName  the name of the channel to view
	 */
    public void viewChannel(String channelName) {
        viewChannel(channelName, getDefaultExtension(channelName));
    }

    /**
   * View the channel given by the channel name with the specified extension.
   * 
   * @param channelName  the name of the channel to view
   * @param extension    the extension to view the channel with
   */
    public void viewChannel(String channelName, Extension extension) {
        if (extension != null) {
            log.info("Creating data panel for channel " + channelName + " with extension " + extension.getName() + ".");
            try {
                DataPanel dataPanel = createDataPanel(extension);
                if (!dataPanel.setChannel(channelName)) {
                    log.error("Failed to add channel " + channelName + ".");
                    closeDataPanel(dataPanel);
                }
            } catch (Exception e) {
                log.error("Failed to create data panel and add channel.");
                e.printStackTrace();
                return;
            }
        } else {
            log.warn("Failed to find data panel extension for channel " + channelName + ".");
        }
    }

    /**
   * View the list of channels with the specified extension. If the channel
   * supports multiple channels, all the channels will be viewed in the same
   * instance. Otherwise a new instance will be created for each channel.
   * 
   * If adding a channel to the same instance of an extension fails, a new
   * instance of the extension will be created and the channel will be added
   * this this.
   * 
   * @param channels   the list of channels to view
   * @param extension  the extension to view these channels with
   */
    public void viewChannels(List<String> channels, Extension extension) {
        DataPanel dataPanel = null;
        try {
            dataPanel = createDataPanel(extension);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        boolean supportsMultipleChannels = dataPanel.supportsMultipleChannels();
        for (String channel : channels) {
            if (supportsMultipleChannels) {
                boolean subscribed = dataPanel.addChannel(channel);
                if (!subscribed) {
                    try {
                        dataPanel = createDataPanel(extension);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    dataPanel.addChannel(channel);
                }
            } else {
                if (dataPanel == null) {
                    try {
                        dataPanel = createDataPanel(extension);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
                dataPanel.setChannel(channel);
                dataPanel = null;
            }
        }
    }

    /**
	 * Return an extension that is capable of viewing the
	 * provided mime type.
	 * 
	 * @param mime  the mime type that the extension must support
	 * @return      an extension to view this mime type, or null if none is found
	 */
    private Extension findExtension(String mime) {
        for (int i = 0; i < extensions.size(); i++) {
            Extension extension = (Extension) extensions.get(i);
            List<String> mimeTypes = extension.getMimeTypes();
            for (int j = 0; j < mimeTypes.size(); j++) {
                String mimeType = mimeTypes.get(j);
                if (mimeType.equals(mime)) {
                    return extension;
                }
            }
        }
        return null;
    }

    /**
   * Creates the data panel referenced by the supplied extension ID. This first
   * finds the extension onbject then calls createDataPanel with this.
   * 
   * @param extensionID  the ID of the extension to create
   * @throws Exception
   * @return             the newly created data panel
   */
    public DataPanel createDataPanel(String extensionID) throws Exception {
        return createDataPanel(getExtension(extensionID));
    }

    /**
	 * Creates the data panel referenced by the supplied extension
	 * ID. This ID is by convention, the name of the class implementing
	 * this extension.
	 * 
	 * @param extension   the extension to create
	 * @throws Exception  
	 * @return            the newly created data panel
	 */
    public DataPanel createDataPanel(Extension extension) throws Exception {
        Class dataPanelClass;
        try {
            dataPanelClass = Class.forName(extension.getID());
        } catch (ClassNotFoundException e) {
            throw new Exception("Unable to find extension " + extension.getName());
        }
        DataPanel dataPanel = (DataPanel) dataPanelClass.newInstance();
        dataPanel.openPanel(this);
        dataPanels.add(dataPanel);
        return dataPanel;
    }

    /**
   * See if any data panel are subscribed to at least one of these channels.
   * 
   * @param channels  the names of the channels to check
   * @return             true if any data panel is subscribed to the channels,
   *                     false otherwise
   */
    public boolean isAnyChannelSubscribed(List<String> channels) {
        for (DataPanel dataPanel : dataPanels) {
            for (String channel : channels) {
                if (dataPanel.isChannelSubscribed(channel)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
   * Unsubscribe all data panels from these channels. Data panels will be
   * closed if they are subscribed to no additional channels.
   * 
   * @param channel   the channel to unsubscribe
   */
    public void unsubscribeChannels(List<String> channels) {
        for (DataPanel dataPanel : dataPanels) {
            for (String channel : channels) {
                dataPanel.removeChannel(channel);
            }
        }
        closeEmptyDataPanels();
    }

    /**
   * Close data panels that aren't subscribed to any channels.
   */
    private void closeEmptyDataPanels() {
        for (int i = dataPanels.size() - 1; i >= 0; i--) {
            DataPanel dp = (DataPanel) dataPanels.get(i);
            if (dp.subscribedChannelCount() == 0) {
                closeDataPanel(dp);
            }
        }
    }

    /**
   * Return a list of data panels.
   * 
   * @return  a list of data panels 
   */
    public List<DataPanel> getDataPanels() {
        return dataPanels;
    }
}
