package org.rdv.datapanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import org.apache.commons.logging.Log;
import org.rdv.DataPanelManager;
import org.rdv.DataViewer;
import org.rdv.rbnb.Channel;
import org.rdv.rbnb.DataListener;
import org.rdv.rbnb.RBNBController;
import org.rdv.rbnb.StateListener;
import org.rdv.rbnb.TimeListener;
import org.rdv.rbnb.TimeScaleListener;
import org.rdv.ui.ChannelListDataFlavor;
import org.rdv.ui.DataPanelContainer;
import org.rdv.ui.ScrollablePopupMenu;
import org.rdv.ui.ToolBarButton;
import com.jgoodies.uif_lite.panel.SimpleInternalFrame;
import com.rbnb.sapi.ChannelMap;

/**
 * A default implementation of the DataPanel interface. This class manages
 * add and remove channel requests, and handles subscription to the
 * RBNBController for time, data, state, and posting. It also provides a toolbar
 * placed at the top of the UI component to enable the detach and fullscreen
 * features along with a close button.
 * <p>
 * Data panels extending this class must have a no argument constructor and call
 * setDataComponent with their UI component in this constructor. 
 * 
 * @since   1.1
 * @author  Jason P. Hanley
 */
public abstract class AbstractDataPanel implements DataPanel, DataListener, TimeListener, TimeScaleListener, StateListener, DropTargetListener {

    /**
	 * The logger for this class.
	 * 
	 * @since  1.1
	 */
    static Log log = org.rdv.LogFactory.getLog(AbstractDataPanel.class.getName());

    /**
	 * The data panel manager for callbacks to the main application.
	 * 
	 * @since  1.2
	 */
    protected DataPanelManager dataPanelManager;

    /**
	 * The data panel container for docking in the UI.
	 * 
	 * @since  1.2
	 */
    DataPanelContainer dataPanelContainer;

    /**
	 * The RBNB controller for receiving data.
	 * 
	 * @since 1.2
	 */
    protected RBNBController rbnbController;

    /**
   * A list of subscribed channels.
   * 
   * @since 1.1
   */
    protected List<String> channels;

    /**
   * A list of lower threshold values for channels.
   * 
   * @since 1.3
   */
    Hashtable<String, String> lowerThresholds;

    /**
   * A list of upper threshold values for channels.
   * 
   * @since 1.3
   */
    Hashtable<String, String> upperThresholds;

    /**
	 * The last posted time.
	 * 
	 * @since  1.1
	 */
    protected double time;

    /**
	 * The last posted time scale.
	 * 
	 * @since  1.1
	 */
    protected double timeScale;

    /**
   * The last posted state.
   * 
   * @since  1.3
   */
    protected double state;

    /**
	 * The UI component with toolbar.
	 * 
	 * @since  1.1
	 */
    SimpleInternalFrame component;

    /**
   * The attach/detach button.
   * 
   * @since 1.3
   */
    ToolBarButton achButton;

    /**
	 * The subclass UI component.
	 * 
	 * @since  1.1
	 */
    JComponent dataComponent;

    /**
	 * The frame used when the UI component is detached or full screen.
	 * 
	 * @since  1.1
	 */
    JFrame frame;

    /**
	 * Indicating if the UI component is docked.
	 * 
	 * @since  1.1
	 */
    boolean attached;

    /**
	 * Indicating if the UI component is in fullscreen mode.
	 * 
	 * @since  1.1
	 */
    boolean maximized;

    static String attachIconFileName = "icons/attach.gif";

    static String detachIconFileName = "icons/detach.gif";

    static String closeIconFileName = "icons/close.gif";

    /**
 	 * Indicating if the data panel has been paused by the snaphsot button in the
 	 * toolbar.
 	 * 
 	 * @since  1.1
 	 */
    boolean paused;

    /**
 	 * A data channel map from the last post of data.
 	 * 
 	 * @since  1.1
 	 */
    protected ChannelMap channelMap;

    /**
   * A description of the data panel.
   * 
   * @since  1.4
   */
    String description;

    /**
   * Whether to show the channel names in the title;
   */
    boolean showChannelsInTitle;

    /**
   * Properties list for data panel.
   */
    protected Properties properties;

    /**
	 * Initialize the list of channels and units. Set parameters to defaults.
	 * 
	 * @since  1.1
	 */
    public AbstractDataPanel() {
        channels = new CopyOnWriteArrayList<String>();
        lowerThresholds = new Hashtable<String, String>();
        upperThresholds = new Hashtable<String, String>();
        time = 0;
        timeScale = 1;
        state = RBNBController.STATE_DISCONNECTED;
        attached = true;
        maximized = false;
        paused = false;
        showChannelsInTitle = true;
        properties = new Properties();
    }

    public void openPanel(final DataPanelManager dataPanelManager) {
        this.dataPanelManager = dataPanelManager;
        this.dataPanelContainer = dataPanelManager.getDataPanelContainer();
        this.rbnbController = RBNBController.getInstance();
        component = new SimpleInternalFrame(getTitleComponent(), createToolBar(), dataComponent);
        dataPanelContainer.addDataPanel(component);
        initDropTarget();
        rbnbController.addTimeListener(this);
        rbnbController.addStateListener(this);
        rbnbController.addTimeScaleListener(this);
    }

    JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        final DataPanel dataPanel = this;
        achButton = new ToolBarButton(detachIconFileName, "Detach data panel");
        achButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                toggleDetach();
            }
        });
        toolBar.add(achButton);
        JButton button = new ToolBarButton(closeIconFileName, "Close data panel");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                dataPanelManager.closeDataPanel(dataPanel);
            }
        });
        toolBar.add(button);
        return toolBar;
    }

    public boolean setChannel(String channelName) {
        Channel channel = rbnbController.getChannel(channelName);
        if (channel == null) {
            return false;
        }
        if (channels.size() == 1 && channels.contains(channelName)) {
            return false;
        }
        removeAllChannels();
        return addChannel(channelName);
    }

    public boolean addChannel(String channelName) {
        Channel channel = rbnbController.getChannel(channelName);
        if (channel == null) {
            return false;
        }
        if (channels.contains(channelName)) {
            return false;
        }
        channels.add(channelName);
        String lowerThreshold = channel.getMetadata("lowerbound");
        String upperThreshold = channel.getMetadata("upperbound");
        if (lowerThreshold != null && !lowerThresholds.containsKey(channelName)) {
            try {
                Double.parseDouble(lowerThreshold);
                lowerThresholds.put(channelName, lowerThreshold);
            } catch (java.lang.NumberFormatException nfe) {
                log.warn("Non-numeric lower threshold in metadata: \"" + lowerThreshold + "\"");
            }
        }
        if (upperThreshold != null && !upperThresholds.containsKey(channelName)) {
            try {
                Double.parseDouble(upperThreshold);
                upperThresholds.put(channelName, upperThreshold);
            } catch (java.lang.NumberFormatException nfe) {
                log.warn("Non-numeric upper threshold in metadata: \"" + upperThreshold + "\"");
            }
        }
        log.debug("&&& LOWER: " + lowerThreshold + " UPPER: " + upperThreshold);
        updateTitle();
        channelAdded(channelName);
        rbnbController.subscribe(channelName, this);
        return true;
    }

    /**
   * For use by subclasses to do any initialization needed when a channel has
   * been added.
   * 
   * @param channelName  the name of the channel being added
   */
    protected void channelAdded(String channelName) {
    }

    public boolean removeChannel(String channelName) {
        if (!channels.contains(channelName)) {
            return false;
        }
        rbnbController.unsubscribe(channelName, this);
        channels.remove(channelName);
        lowerThresholds.remove(channelName);
        upperThresholds.remove(channelName);
        updateTitle();
        channelRemoved(channelName);
        return true;
    }

    /**
   * For use by subclasses to do any cleanup needed when a channel has been
   * removed.
   * 
   * @param channelName  the name of the channel being removed
   */
    protected void channelRemoved(String channelName) {
    }

    /**
	 * Calls removeChannel for each subscribed channel.
	 * 
	 * @see    removeChannel(String)
	 * @since  1.1
	 *
	 */
    void removeAllChannels() {
        Iterator<String> i = channels.iterator();
        while (i.hasNext()) {
            removeChannel(i.next());
        }
    }

    /**
	 * Gets the UI component used for displaying data.
	 * 
	 * @return  the UI component used to display data
	 */
    protected JComponent getDataComponent() {
        return dataComponent;
    }

    /**
	 * Set the UI component to be used for displaying data. This method must be 
	 * called from the constructor of the subclass.
	 * 
	 * @param dataComponent  the UI component
	 * @since                1.1
	 */
    protected void setDataComponent(JComponent dataComponent) {
        this.dataComponent = dataComponent;
    }

    protected String getTitle() {
        if (description != null) {
            return description;
        }
        String titleString = "";
        Iterator<String> i = channels.iterator();
        while (i.hasNext()) {
            titleString += i.next();
            if (i.hasNext()) {
                titleString += ", ";
            }
        }
        return titleString;
    }

    /**
   * Get a component for displaying the title in top bar. This implementation
   * includes a button to remove a specific channel.
   * 
   * Subclasses should overide this method if they don't want the default
   * implementation.
   * 
   * @return  A component for the top bar
   * @since   1.3
   */
    JComponent getTitleComponent() {
        JPanel titleBar = new JPanel();
        titleBar.setOpaque(false);
        titleBar.setLayout(new BorderLayout());
        JPopupMenu popupMenu = new ScrollablePopupMenu();
        final String title;
        if (description != null) {
            title = "Edit description";
        } else {
            title = "Add description";
        }
        JMenuItem addDescriptionMenuItem = new JMenuItem(title);
        addDescriptionMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                String response = (String) JOptionPane.showInputDialog(null, "Enter a description", title, JOptionPane.QUESTION_MESSAGE, null, null, description);
                if (response == null) {
                    return;
                } else if (response.length() == 0) {
                    setDescription(null);
                } else {
                    setDescription(response);
                }
            }
        });
        popupMenu.add(addDescriptionMenuItem);
        if (description != null) {
            JMenuItem removeDescriptionMenuItem = new JMenuItem("Remove description");
            removeDescriptionMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    setDescription(null);
                }
            });
            popupMenu.add(removeDescriptionMenuItem);
        }
        popupMenu.addSeparator();
        final JCheckBoxMenuItem showChannelsInTitleMenuItem = new JCheckBoxMenuItem("Show channels in title", showChannelsInTitle);
        showChannelsInTitleMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                setShowChannelsInTitle(showChannelsInTitleMenuItem.isSelected());
            }
        });
        popupMenu.add(showChannelsInTitleMenuItem);
        if (channels.size() > 0) {
            popupMenu.addSeparator();
            Iterator<String> i = channels.iterator();
            while (i.hasNext()) {
                final String channelName = i.next();
                JMenuItem unsubscribeChannelMenuItem = new JMenuItem("Unsubscribe from " + channelName);
                unsubscribeChannelMenuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        removeChannel(channelName);
                    }
                });
                popupMenu.add(unsubscribeChannelMenuItem);
            }
        }
        titleBar.setComponentPopupMenu(popupMenu);
        titleBar.addMouseListener(new MouseInputAdapter() {
        });
        if (description != null) {
            titleBar.add(getDescriptionComponent(), BorderLayout.WEST);
        }
        JComponent titleComponent = getChannelComponent();
        if (titleComponent != null) {
            titleBar.add(titleComponent, BorderLayout.CENTER);
        }
        return titleBar;
    }

    JComponent getDescriptionComponent() {
        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.BOLD));
        descriptionLabel.setForeground(Color.white);
        return descriptionLabel;
    }

    protected JComponent getChannelComponent() {
        if (channels.size() == 0) {
            return null;
        }
        JPanel channelBar = new JPanel();
        channelBar.setOpaque(false);
        channelBar.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        Iterator<String> i = channels.iterator();
        while (i.hasNext()) {
            String channelName = i.next();
            if (showChannelsInTitle) {
                channelBar.add(new ChannelTitle(channelName));
            }
        }
        return channelBar;
    }

    /**
   * Update the title of the data panel. This includes the text displayed in the
   * header panel and the text displayed in the frame if the data panel is
   * detached.
   */
    protected void updateTitle() {
        component.setTitle(getTitleComponent());
        if (!attached) {
            frame.setTitle(getTitle());
        }
    }

    protected boolean isShowChannelsInTitle() {
        return showChannelsInTitle;
    }

    void setShowChannelsInTitle(boolean showChannelsInTitle) {
        if (this.showChannelsInTitle != showChannelsInTitle) {
            this.showChannelsInTitle = showChannelsInTitle;
            properties.setProperty("showChannelsInTitle", Boolean.toString(showChannelsInTitle));
            updateTitle();
        }
    }

    void setDescription(String description) {
        if (this.description != description) {
            this.description = description;
            if (description != null) {
                properties.setProperty("description", description);
            } else {
                properties.remove("description");
            }
            updateTitle();
        }
    }

    public void postData(ChannelMap channelMap) {
        this.channelMap = channelMap;
    }

    public void postTime(double time) {
        this.time = time;
    }

    public void timeScaleChanged(double timeScale) {
        this.timeScale = timeScale;
    }

    public void postState(int newState, int oldState) {
        state = newState;
    }

    /**
	 * Toggle pausing of the data panel. Pausing is freezing the data display and
	 * stopping listeners for data. When a channel is unpaused it will again
	 * subscribe to data for the subscribed channels.
	 *
	 * @since  1.1
	 */
    void togglePause() {
        Iterator<String> i = channels.iterator();
        while (i.hasNext()) {
            String channelName = i.next();
            if (paused) {
                rbnbController.subscribe(channelName, this);
            } else {
                rbnbController.unsubscribe(channelName, this);
            }
        }
        paused = !paused;
    }

    public void closePanel() {
        removeAllChannels();
        if (maximized) {
            restorePanel(false);
        } else if (!attached) {
            attachPanel(false);
        } else if (attached) {
            dataPanelContainer.removeDataPanel(component);
        }
        rbnbController.removeStateListener(this);
        rbnbController.removeTimeListener(this);
        rbnbController.removeTimeScaleListener(this);
    }

    public int subscribedChannelCount() {
        return channels.size();
    }

    public List<String> subscribedChannels() {
        return channels;
    }

    public boolean isChannelSubscribed(String channelName) {
        return channels.contains(channelName);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperty(String key, String value) {
        if (key.equals("showChannelsInTitle")) {
            setShowChannelsInTitle(Boolean.parseBoolean(value));
        } else if (key.equals("description")) {
            setDescription(value);
        } else if (key.equals("attached") && Boolean.parseBoolean(value) == false) {
            detachPanel();
        } else if (key.equals("bounds")) {
            loadBounds(value);
        }
    }

    /**
	 * Toggle detaching the UI component from the data panel container.
	 * 
	 * @since  1.1
	 */
    void toggleDetach() {
        if (maximized) {
            restorePanel(false);
        }
        if (attached) {
            detachPanel();
        } else {
            attachPanel(true);
        }
    }

    /**
	 * Detach the UI component from the data panel container.
	 * 
	 * @since  1.1
	 */
    void detachPanel() {
        attached = false;
        properties.setProperty("attached", "false");
        dataPanelContainer.removeDataPanel(component);
        achButton.setIcon(attachIconFileName);
        frame = new JFrame(getTitle());
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                closePanel();
            }
        });
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(component);
        String bounds = properties.getProperty("bounds");
        if (bounds != null) {
            loadBounds(bounds);
        } else {
            frame.pack();
        }
        frame.addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                storeBounds();
            }

            public void componentMoved(ComponentEvent e) {
                storeBounds();
            }

            public void componentShown(ComponentEvent e) {
                storeBounds();
            }
        });
        frame.setVisible(true);
    }

    void loadBounds(String bounds) {
        if (bounds != null) {
            properties.setProperty("bounds", bounds);
            if (frame == null) {
                return;
            }
            String[] boundsElements = bounds.split(",");
            int x = Integer.parseInt(boundsElements[0]);
            int y = Integer.parseInt(boundsElements[1]);
            int width = Integer.parseInt(boundsElements[2]);
            int height = Integer.parseInt(boundsElements[3]);
            frame.setBounds(x, y, width, height);
        }
    }

    void storeBounds() {
        if (frame == null) {
            return;
        }
        String bounds = frame.getX() + "," + frame.getY() + "," + frame.getWidth() + "," + frame.getHeight();
        properties.setProperty("bounds", bounds);
    }

    /** Dispose of the frame for the UI component. Dock the UI component if
	 * addToContainer is true.
	 * 
	 * @param addToContainer  whether to dock the UI component
	 * @since                 1.1
	 */
    void attachPanel(boolean addToContainer) {
        if (frame != null) {
            frame.setVisible(false);
            frame.getContentPane().remove(component);
            frame.dispose();
            frame = null;
        }
        if (addToContainer) {
            attached = true;
            properties.remove("attached");
            achButton.setIcon(detachIconFileName);
            dataPanelContainer.addDataPanel(component);
        }
    }

    /**
	 * Toggle maximizing the data panel UI component to fullscreen.
	 * 
	 * @since  1.1
	 */
    void toggleMaximize() {
        if (maximized) {
            restorePanel(attached);
            if (!attached) {
                detachPanel();
            }
        } else {
            if (!attached) {
                attachPanel(false);
            }
            maximizePanel();
        }
    }

    /**
	 * Undock the UI component and display fullscreen.
	 * 
	 * @since  1.1
	 */
    void maximizePanel() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        for (int i = 0; i < devices.length; i++) {
            GraphicsDevice device = devices[i];
            if (device.isFullScreenSupported() && device.getFullScreenWindow() == null) {
                maximized = true;
                dataPanelContainer.removeDataPanel(component);
                frame = new JFrame(getTitle());
                frame.setUndecorated(true);
                frame.getContentPane().add(component);
                try {
                    device.setFullScreenWindow(frame);
                } catch (InternalError e) {
                    log.error("Failed to switch to full screen mode: " + e.getMessage() + ".");
                    restorePanel(true);
                    return;
                }
                frame.setVisible(true);
                frame.requestFocus();
                return;
            }
        }
        log.warn("No screens available or full screen exclusive mode is unsupported on your platform.");
    }

    /**
	 * Leave fullscreen mode and dock the UI component if addToContainer is true.
	 * 
	 * @param addToContainer  whether to dock the UI component
	 * @since                 1.1
	 */
    void restorePanel(boolean addToContainer) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        for (int i = 0; i < devices.length; i++) {
            GraphicsDevice device = devices[i];
            if (device.isFullScreenSupported() && device.getFullScreenWindow() == frame) {
                if (frame != null) {
                    frame.setVisible(false);
                    device.setFullScreenWindow(null);
                    frame.getContentPane().remove(component);
                    frame.dispose();
                    frame = null;
                }
                maximized = false;
                if (addToContainer) {
                    dataPanelContainer.addDataPanel(component);
                }
                break;
            }
        }
    }

    /**
	 * Setup the drop target for channel subscription via drag-and-drop.
	 *
	 * @since  1.2
	 */
    void initDropTarget() {
        new DropTarget(component, DnDConstants.ACTION_LINK, this);
    }

    public void dragEnter(DropTargetDragEvent e) {
    }

    public void dragOver(DropTargetDragEvent e) {
    }

    public void dropActionChanged(DropTargetDragEvent e) {
    }

    public void drop(DropTargetDropEvent e) {
        try {
            int dropAction = e.getDropAction();
            if (dropAction == DnDConstants.ACTION_LINK) {
                DataFlavor channelListDataFlavor = new ChannelListDataFlavor();
                Transferable tr = e.getTransferable();
                if (e.isDataFlavorSupported(channelListDataFlavor)) {
                    e.acceptDrop(DnDConstants.ACTION_LINK);
                    e.dropComplete(true);
                    final List channels = (List) tr.getTransferData(channelListDataFlavor);
                    new Thread() {

                        public void run() {
                            for (int i = 0; i < channels.size(); i++) {
                                String channel = (String) channels.get(i);
                                boolean status;
                                if (supportsMultipleChannels()) {
                                    status = addChannel(channel);
                                } else {
                                    status = setChannel(channel);
                                }
                                if (!status) {
                                }
                            }
                        }
                    }.start();
                } else {
                    e.rejectDrop();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (UnsupportedFlavorException ufe) {
            ufe.printStackTrace();
        }
    }

    public void dragExit(DropTargetEvent e) {
    }

    public class ChannelTitle extends JPanel {

        /** serialization version identifier */
        private static final long serialVersionUID = -7191565876111378704L;

        public ChannelTitle(String channelName) {
            this(channelName, channelName);
        }

        public ChannelTitle(String seriesName, String channelName) {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(0, 0, 0, 5));
            setOpaque(false);
            JLabel text = new JLabel(seriesName);
            text.setForeground(SimpleInternalFrame.getTextForeground(true));
            add(text, BorderLayout.CENTER);
            JButton closeButton = new JButton(DataViewer.getIcon("icons/close_channel.gif"));
            closeButton.setToolTipText("Remove channel");
            closeButton.setBorder(null);
            closeButton.setOpaque(false);
            closeButton.addActionListener(getActionListener(seriesName, channelName));
            add(closeButton, BorderLayout.EAST);
        }

        protected ActionListener getActionListener(final String seriesName, final String channelName) {
            return new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    removeChannel(channelName);
                }
            };
        }
    }

    /**
   * A class to transfer image data.
   * 
   * @since  1.3
   */
    public class ImageSelection implements Transferable {

        /**
     * The image to transfer.
     */
        Image image;

        /**
     * Creates this class to transfer the <code>image</code>.
     * 
     * @param image the image to transfer
     */
        public ImageSelection(Image image) {
            this.image = image;
        }

        /**
     * Returns the transfer data flavors support. This returns an array with one
     * element representing the {@link DataFlavor#imageFlavor}.
     * 
     * @return an array of transfer data flavors supported
     */
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        /**
     * Returns true if the specified data flavor is supported. The only data
     * flavor supported by this class is the {@link DataFlavor#imageFlavor}.
     * 
     * @return true if the transfer data flavor is supported
     */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        /**
     * Returns the {@link Image} object to be transfered if the flavor is
     * {@link DataFlavor#imageFlavor}. Otherwise it throws an
     * {@link UnsupportedFlavorException}.
     * 
     * @return the image object transfered by this class
     * @throws UnsupportedFlavorException
     */
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
