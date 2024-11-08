package jhomenet.ui.tree;

import java.awt.event.MouseEvent;
import java.util.logging.Level;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.hw.HomenetHardware;
import jhomenet.commons.hw.mngt.HardwareManagerException;
import jhomenet.ui.DesktopView;
import jhomenet.ui.factories.ImageFactory;
import jhomenet.ui.window.factories.*;
import jhomenet.ui.wrapper.InternalFrameWindowWrapper;

/**
 * A tree node representing a hardware object.
 * 
 * @author David Irwin (jhomenet at gmail dot com)
 */
public class RegisteredNode extends AbstractHardwareNode<RegisteredHardware> {

    /**
     * Serial version hardwareID information - used for the serialization process.
     */
    private static final long serialVersionUID = 00001;

    /**
     * Define a logging mechanism
     */
    private static Logger logger = Logger.getLogger(RegisteredNode.class.getName());

    /**
     * 
     */
    private final DesktopView desktopView;

    /**
     * 
     */
    private JPopupMenu registeredPopupMenu;

    /**
     * 
     */
    private static final String defaultIconFilename = "green.png";

    /**
     * 
     * @param hardware
     * @param treeView
     * @param desktopView
     * @param serverContext
     * @param config
     */
    public RegisteredNode(RegisteredHardware hardware, TreeView treeView, DesktopView desktopView, GeneralApplicationContext serverContext) {
        super(hardware, true, treeView, serverContext);
        this.desktopView = desktopView;
        initializeChannels();
        initializePopupMenu();
    }

    /**
     * Initialize the input channels.
     */
    private void initializeChannels() {
        RegisteredHardware hardware = (RegisteredHardware) getUserObject();
        if (hardware instanceof HomenetHardware) {
            for (int channel = 0; channel < ((HomenetHardware) hardware).getNumChannels(); channel++) {
                add(new ChannelNode(((HomenetHardware) hardware).getChannel(channel), treeView, serverContext));
            }
        }
    }

    /**
     * 
     */
    private void initializePopupMenu() {
        registeredPopupMenu = new JPopupMenu();
        JMenuItem view_mi = new JMenuItem("View");
        view_mi.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                CustomMutableTreeNode selectedNode = treeView.getSelectedNode();
                logger.debug("Selected node classname: " + selectedNode.getUserObject().getClass().getName());
                if (selectedNode instanceof RegisteredNode) {
                    InternalFrameWindowWrapper window = (InternalFrameWindowWrapper) HardwareWindowFactory.buildHardwareWindow(((RegisteredNode) selectedNode).getHardware(), DefaultWindowFactory.WindowType.INTERNALFRAME, serverContext);
                    desktopView.addWindowToDesktop(window);
                }
            }
        });
        JMenuItem unregister_mi = new JMenuItem("Unregister");
        unregister_mi.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                CustomMutableTreeNode selectedNode = treeView.getSelectedNode();
                logger.debug("Selected node classname: " + selectedNode.getUserObject().getClass().getName());
                if (selectedNode instanceof RegisteredNode) {
                    int userResponse = JOptionPane.showConfirmDialog(null, "Are you sure you want to unregister\r\nhardware " + ((RegisteredNode) selectedNode).getHardwareAddr() + "?", "Unregistering hardware", JOptionPane.YES_NO_OPTION);
                    if (userResponse == JOptionPane.YES_OPTION) {
                        try {
                            serverContext.getHardwareManager().unregisterHardware(((RegisteredNode) selectedNode).getHardware());
                        } catch (HardwareManagerException hme) {
                            logger.error("Hardware manager exception while attempting to unregister hardware: " + hme.getMessage());
                            String msg = "Hardware manager exception while attempting to unregister hardware!";
                            String details = "<html>There was a hardware manager exception while attempting to unregister a " + "registered hardware object.</html>";
                            ErrorInfo errorInfo = new ErrorInfo("Hardware manager exception", msg, details, "hardware-manager", hme, Level.WARNING, null);
                            JXErrorPane.showFrame(null, errorInfo);
                        }
                    }
                }
            }
        });
        registeredPopupMenu.add(view_mi);
        registeredPopupMenu.add(unregister_mi);
    }

    /**
     * @see jhomenet.ui.tree.CustomMutableTreeNode#getIconFilename()
     */
    @Override
    protected String getIconFilename() {
        String iconFilename = ImageFactory.getIconFilename(this.getHardware().getHardwareClassname()).trim();
        if (!(iconFilename != null && iconFilename.length() > 0)) return defaultIconFilename; else return iconFilename;
    }

    /**
     * @see jhomenet.ui.tree.CustomMutableTreeNode#getCustomToolTipText()
     */
    @Override
    public String getCustomToolTipText() {
        RegisteredHardware hardware = (RegisteredHardware) getUserObject();
        StringBuffer buf = new StringBuffer();
        buf.append("<html>");
        buf.append("Hardware address: " + hardware.getHardwareAddr());
        buf.append("<br>Hardware classname: " + hardware.getHardwareClassname());
        buf.append("<br>Hardware description: " + hardware.getAppHardwareDescription());
        if (hardware instanceof HomenetHardware) {
            buf.append("<br>Number of I/O channels: " + ((HomenetHardware) hardware).getNumChannels().toString());
        }
        buf.append("<br>Hardware setup: " + hardware.getHardwareSetupDescription());
        buf.append("</html>");
        return buf.toString();
    }

    /**
     * Get a reference to the node's registered hardware object.
     *
     * @return
     */
    public RegisteredHardware getHardware() {
        return (RegisteredHardware) getUserObject();
    }

    /**
     * @see jhomenet.ui.tree.CustomMutableTreeNode#rightMouseButtonClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void rightMouseButtonClicked(MouseEvent evt) {
        if (this.getEnabled()) registeredPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY()); else this.disabledPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        final RegisteredHardware thisHardware = (RegisteredHardware) this.getUserObject();
        final int prime = 31;
        int result = 1;
        result = prime * result + ((thisHardware == null) ? 0 : thisHardware.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final RegisteredNode other = (RegisteredNode) obj;
        final RegisteredHardware thisRegisteredHardware = (RegisteredHardware) this.getUserObject();
        final RegisteredHardware otherRegisteredHardware = (RegisteredHardware) other.getUserObject();
        if (thisRegisteredHardware == null) {
            if (otherRegisteredHardware != null) return false;
        } else if (!thisRegisteredHardware.equals(otherRegisteredHardware)) {
            return false;
        }
        return true;
    }
}
