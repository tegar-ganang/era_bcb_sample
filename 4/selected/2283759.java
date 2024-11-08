package jirc;

import java.util.HashMap;
import javax.swing.JOptionPane;
import jirc.MainGUI.ServerWithWnd;

/**
 *
 * @author David
 */
public class ServerWindow extends javax.swing.JInternalFrame {

    private IRCServer server;

    private HashMap<String, ChannelWindow> channelWindows = new HashMap<String, ChannelWindow>();

    private String allMsgs = "";

    /** Creates new form ServerWindow */
    public ServerWindow(IRCServer server, String title) {
        super("Server :: " + title);
        this.server = server;
        initComponents();
    }

    public HashMap<String, ChannelWindow> getChannelWindows() {
        return channelWindows;
    }

    public void dropMessage(String msg) {
        allMsgs += msg + "<br />";
        messages.setText("<html>" + allMsgs + "</html>");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        messages = new javax.swing.JTextPane();
        commandTxt = new javax.swing.JTextField();
        setClosable(true);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        addInternalFrameListener(new javax.swing.event.InternalFrameListener() {

            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
                closing(evt);
            }

            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameIconified(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
            }
        });
        messages.setContentType("text/html");
        messages.setEditable(false);
        jScrollPane1.setViewportView(messages);
        commandTxt.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processCmd(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(commandTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 536, Short.MAX_VALUE).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 536, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(commandTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
        pack();
    }

    private void processCmd(java.awt.event.ActionEvent evt) {
        final String cmd = this.commandTxt.getText().trim();
        String[] cmdSplit = cmd.split(" ");
        if (cmdSplit[0].equalsIgnoreCase("/j") || cmdSplit[0].equalsIgnoreCase("/j")) {
            if (cmdSplit.length > 1) {
                ChannelWindow wnd = channelWindows.get(cmdSplit[1]);
                if (wnd == null) {
                    String channel = cmdSplit[1];
                    if (!channel.startsWith("#")) {
                        channel = "#" + channel;
                    }
                    ChannelWindow cWind = new ChannelWindow(server, channel);
                    cWind.setVisible(true);
                    channelWindows.put(cmdSplit[1], cWind);
                    Core.mainApp.getContentPanel().add(cWind);
                    Core.mainApp.reloadServerList();
                } else {
                    try {
                        if (!wnd.isVisible()) {
                            wnd.setVisible(true);
                        }
                        wnd.setSelected(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        commandTxt.setText("");
    }

    private void closing(javax.swing.event.InternalFrameEvent evt) {
        int opt = JOptionPane.showConfirmDialog(this, "Do you want to disconnect from the server?", "Confirmination", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            server.disconnect();
            for (ServerWithWnd sww : Core.mainApp.getServerList()) {
                if (sww.server == server) {
                    Core.mainApp.getServerList().remove(sww);
                }
            }
            Core.mainApp.reloadServerList();
        }
        this.dispose();
    }

    private javax.swing.JTextField commandTxt;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTextPane messages;
}
