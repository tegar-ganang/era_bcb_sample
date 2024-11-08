package coopnetclient.frames;

import coopnetclient.Globals;
import coopnetclient.frames.clientframetabs.TabOrganizer;
import coopnetclient.protocol.out.Protocol;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class JoinRoomPasswordFrame extends javax.swing.JFrame {

    private String host_name = null;

    private String channel = null;

    private String ID = null;

    /** Creates new form RoomJoinPasswordFrame */
    public JoinRoomPasswordFrame(String host_name, String channel) {
        initComponents();
        hideWrongPasswordNotification();
        this.host_name = host_name;
        this.channel = channel;
        this.getRootPane().setDefaultButton(btn_join);
        AbstractAction act = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                btn_cancel.doClick();
            }
        };
        getRootPane().getActionMap().put("close", act);
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

    public JoinRoomPasswordFrame(String ID) {
        initComponents();
        hideWrongPasswordNotification();
        this.ID = ID;
        this.getRootPane().setDefaultButton(btn_join);
        AbstractAction act = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                btn_cancel.doClick();
            }
        };
        getRootPane().getActionMap().put("close", act);
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

    public void showWrongPasswordNotification() {
        lbl_errormsg.setForeground(Color.red);
        lbl_errormsg.setText("Wrong Password!");
        btn_join.setEnabled(true);
    }

    public void hideWrongPasswordNotification() {
        lbl_errormsg.setText(" ");
    }

    private void initComponents() {
        btn_join = new javax.swing.JButton();
        btn_cancel = new javax.swing.JButton();
        pnl_input = new javax.swing.JPanel();
        lbl_roomPassword = new javax.swing.JLabel();
        pf_roomPassword = new javax.swing.JPasswordField();
        lbl_errormsg = new javax.swing.JLabel();
        setTitle("Enter password");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        btn_join.setText("Join");
        btn_join.setNextFocusableComponent(btn_cancel);
        btn_join.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                join(evt);
            }
        });
        btn_cancel.setText("Cancel");
        btn_cancel.setNextFocusableComponent(pf_roomPassword);
        btn_cancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cancelActionPerformed(evt);
            }
        });
        pnl_input.setBorder(javax.swing.BorderFactory.createTitledBorder("Enter password"));
        lbl_roomPassword.setText("<html>This room is password protected,<br> please enter the correct password:");
        pf_roomPassword.setNextFocusableComponent(btn_join);
        lbl_errormsg.setForeground(new java.awt.Color(255, 0, 0));
        lbl_errormsg.setText("Wrong Password!");
        javax.swing.GroupLayout pnl_inputLayout = new javax.swing.GroupLayout(pnl_input);
        pnl_input.setLayout(pnl_inputLayout);
        pnl_inputLayout.setHorizontalGroup(pnl_inputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnl_inputLayout.createSequentialGroup().addContainerGap().addGroup(pnl_inputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pf_roomPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE).addComponent(lbl_roomPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE).addComponent(lbl_errormsg, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE)).addContainerGap()));
        pnl_inputLayout.setVerticalGroup(pnl_inputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnl_inputLayout.createSequentialGroup().addComponent(lbl_roomPassword).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pf_roomPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbl_errormsg)));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pnl_input, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(btn_join).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btn_cancel).addContainerGap(310, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(pnl_input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btn_join).addComponent(btn_cancel)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { btn_cancel, btn_join });
        pack();
    }

    private void join(java.awt.event.ActionEvent evt) {
        String passw = new String(pf_roomPassword.getPassword());
        if (host_name != null) {
            Protocol.joinRoom(host_name, passw);
        } else if (ID != null) {
            Protocol.joinRoomByID(ID, passw);
        }
        btn_join.setEnabled(false);
        hideWrongPasswordNotification();
    }

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt) {
        FrameOrganizer.closeRoomCreationFrame();
        TabOrganizer.getChannelPanel(channel).enableButtons();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        FrameOrganizer.closeRoomCreationFrame();
    }

    private javax.swing.JButton btn_cancel;

    private javax.swing.JButton btn_join;

    private javax.swing.JLabel lbl_errormsg;

    private javax.swing.JLabel lbl_roomPassword;

    private javax.swing.JPasswordField pf_roomPassword;

    private javax.swing.JPanel pnl_input;
}
