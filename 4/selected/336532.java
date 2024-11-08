package de.renier.vdr.channel.editor;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import de.renier.vdr.channel.Channel;
import de.renier.vdr.channel.editor.util.Utils;

/**
 * CreateChannelDialog
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class CreateChannelDialog extends JDialog {

    private static final long serialVersionUID = -5573593801149984351L;

    public static final int RESULT_CANCEL = 0;

    public static final int RESULT_CREATE = 1;

    private javax.swing.JPanel jContentPane = null;

    private JPanel jPanel = null;

    private JPanel jPanel1 = null;

    private JLabel jLabel = null;

    private ChannelPropertyPanel channelPropertyPanel = null;

    private JButton jButton = null;

    private JButton jButton1 = null;

    private int result = RESULT_CANCEL;

    /**
   * This is the default constructor
   */
    public CreateChannelDialog(Frame frame) {
        super(frame, true);
        initialize();
        if (frame != null) {
            Point p = frame.getLocation();
            Dimension frameDim = frame.getSize();
            Dimension ownDim = this.getSize();
            this.setLocation((int) p.getX() + ((int) (frameDim.getWidth() - ownDim.getWidth()) / 2), (int) p.getY() + ((int) (frameDim.getHeight() - ownDim.getHeight()) / 2));
        }
    }

    /**
   * This method initializes this
   * 
   * @return void
   */
    private void initialize() {
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle(Messages.getString("CreateChannelDialog.0"));
        this.setSize(530, 270);
        this.setContentPane(getJContentPane());
    }

    /**
   * This method initializes jContentPane
   * 
   * @return javax.swing.JPanel
   */
    private javax.swing.JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout(new java.awt.BorderLayout());
            jContentPane.add(getJPanel(), java.awt.BorderLayout.NORTH);
            jContentPane.add(getJPanel1(), java.awt.BorderLayout.SOUTH);
            jContentPane.add(getChannelPropertyPanel(), java.awt.BorderLayout.CENTER);
        }
        return jContentPane;
    }

    /**
   * This method initializes jPanel
   * 
   * @return javax.swing.JPanel
   */
    private JPanel getJPanel() {
        if (jPanel == null) {
            jLabel = new JLabel();
            jPanel = new JPanel();
            jLabel.setText(Messages.getString("CreateChannelDialog.1"));
            jPanel.add(jLabel, null);
        }
        return jPanel;
    }

    /**
   * This method initializes jPanel1
   * 
   * @return javax.swing.JPanel
   */
    private JPanel getJPanel1() {
        if (jPanel1 == null) {
            jPanel1 = new JPanel();
            jPanel1.add(getJButton(), null);
            jPanel1.add(getJButton1(), null);
        }
        return jPanel1;
    }

    /**
   * This method initializes channelPropertyPanel
   * 
   * @return de.renier.vdr.channel.editor.ChannelPropertyPanel
   */
    private ChannelPropertyPanel getChannelPropertyPanel() {
        if (channelPropertyPanel == null) {
            channelPropertyPanel = new ChannelPropertyPanel(true);
        }
        return channelPropertyPanel;
    }

    /**
   * This method initializes jButton
   * 
   * @return javax.swing.JButton
   */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setText(Messages.getString("CreateChannelDialog.2"));
            jButton.setIcon(new ImageIcon(getClass().getResource("/org/javalobby/icons/20x20/New.gif")));
            jButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    Channel channel = getChannelPropertyPanel().getChannel();
                    if (Utils.isEmpty(channel.getName())) {
                        JOptionPane.showMessageDialog(CreateChannelDialog.this, Messages.getString("CreateChannelDialog.4"));
                        return;
                    }
                    result = RESULT_CREATE;
                    setVisible(false);
                    dispose();
                }
            });
        }
        return jButton;
    }

    /**
   * This method initializes jButton1
   * 
   * @return javax.swing.JButton
   */
    private JButton getJButton1() {
        if (jButton1 == null) {
            jButton1 = new JButton();
            jButton1.setText(Messages.getString("CreateChannelDialog.5"));
            jButton1.setIcon(new ImageIcon(getClass().getResource("/org/javalobby/icons/20x20/Stop.gif")));
            jButton1.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    setVisible(false);
                    dispose();
                }
            });
        }
        return jButton1;
    }

    public int showDialog() {
        show();
        return result;
    }

    public Channel getChannel() {
        return getChannelPropertyPanel().getChannel();
    }
}
