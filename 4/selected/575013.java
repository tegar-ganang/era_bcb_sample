package de.renier.vdr.channel.editor;

import java.awt.BorderLayout;
import java.util.Enumeration;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import de.renier.vdr.channel.editor.container.ChannelListRenderer;
import de.renier.vdr.channel.editor.container.DNDList;

/**
 * ChannelParkingPanel
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class ChannelParkingPanel extends JPanel {

    private static final long serialVersionUID = -8188519081240735880L;

    private JLabel jLabel = null;

    private JScrollPane jScrollPane = null;

    private JList jList = null;

    private JPanel jPanel = null;

    private JButton jButton = null;

    /**
   * This is the default constructor
   */
    public ChannelParkingPanel() {
        super();
        initialize();
    }

    /**
   * This method initializes this
   * 
   * @return void
   */
    private void initialize() {
        jLabel = new JLabel();
        this.setLayout(new BorderLayout());
        this.setSize(300, 200);
        jLabel.setText(Messages.getString("ChannelParkingPanel.0"));
        this.add(jLabel, java.awt.BorderLayout.NORTH);
        this.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
        this.add(getJPanel(), java.awt.BorderLayout.SOUTH);
    }

    /**
   * This method initializes jScrollPane
   * 
   * @return javax.swing.JScrollPane
   */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getJList());
        }
        return jScrollPane;
    }

    /**
   * This method initializes jList
   * 
   * @return javax.swing.JList
   */
    private JList getJList() {
        if (jList == null) {
            jList = new DNDList();
            ChannelListRenderer channelRenderer = new ChannelListRenderer();
            jList.setCellRenderer(channelRenderer);
        }
        return jList;
    }

    public int getListSize() {
        return jList.getModel().getSize();
    }

    public void addElement(DefaultMutableTreeNode node) {
        ((DefaultListModel) jList.getModel()).addElement(node);
    }

    public void removeElement(DefaultMutableTreeNode node) {
        ((DefaultListModel) jList.getModel()).removeElement(node);
    }

    public Object[] getSelectedOrAllElements() {
        Object[] result = jList.getSelectedValues();
        if (result == null || result.length == 0) {
            result = new Object[((DefaultListModel) jList.getModel()).getSize()];
            Enumeration enumer = ((DefaultListModel) jList.getModel()).elements();
            for (int i = 0; enumer.hasMoreElements(); i++) {
                result[i] = enumer.nextElement();
            }
        }
        return result;
    }

    /**
   * This method initializes jPanel
   * 
   * @return javax.swing.JPanel
   */
    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.add(getJButton(), null);
        }
        return jPanel;
    }

    /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setIcon(new ImageIcon(getClass().getResource("/org/javalobby/icons/20x20/Delete.gif")));
            jButton.setText(Messages.getString("ChannelParkingPanel.2"));
            jButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    DefaultListModel listModel = (DefaultListModel) jList.getModel();
                    Enumeration enumer = listModel.elements();
                    while (enumer.hasMoreElements()) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumer.nextElement();
                        ChannelEditor.application.getChannelDeletedPanel().addElement(node);
                    }
                    listModel.removeAllElements();
                }
            });
        }
        return jButton;
    }
}
