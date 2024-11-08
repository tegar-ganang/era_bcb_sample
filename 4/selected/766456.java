package gui.clientsbar;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import engine.server.Server;
import org.apache.log4j.Logger;
import util.StringPadder;

public class pnlClientsBar extends javax.swing.JPanel {

    private static Logger log = Logger.getLogger(pnlClientsBar.class.getName());

    private javax.swing.Timer oneSecondTimer = null;

    private final Dimension clientButtonDimension = new Dimension(198, 23);

    private JPopupMenu popup = null;

    private JMenuItem mi = null;

    private String popUpInvoker = "";

    private BackgroundPanel pnlClients = null;

    public pnlClientsBar() {
        initComponents();
        if (!java.beans.Beans.isDesignTime()) {
            pnlClients = new BackgroundPanel();
            pnlClients.setLayout(new javax.swing.BoxLayout(pnlClients, javax.swing.BoxLayout.Y_AXIS));
            jScrollPane3.setViewportView(pnlClients);
            oneSecondTimer = new javax.swing.Timer(1000, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    clearDisconnectedClients(clientsButtonGroup);
                    Integer selectedID = getSelectedClient();
                    Set set = Server.getChannelsArray().entrySet();
                    Iterator it = set.iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (!isClientAlreadyConnected(clientsButtonGroup, entry.getKey().toString())) {
                            JToggleButton cmdClient01 = createClientButton(entry.getKey().toString(), entry.getValue().toString());
                            clientsButtonGroup.add(cmdClient01);
                            pnlClients.add(cmdClient01);
                        }
                    }
                    if (selectedID == -1 && pnlClients.getComponentCount() > 0) {
                        ((JToggleButton) pnlClients.getComponent(0)).setSelected(true);
                        fireClientSelected(((JToggleButton) pnlClients.getComponent(0)).getName());
                    }
                    pnlClients.revalidate();
                    pnlClients.repaint();
                    fireOneSecondTimerTicked();
                }
            });
            popup = new JPopupMenu();
            mi = new JMenuItem("Disconnect");
            mi.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals("disconnect")) {
                        fireDisconnectionRequested(popUpInvoker);
                    }
                }
            });
            mi.setActionCommand("disconnect");
            popup.add(mi);
            popup.setOpaque(true);
            popup.setLightWeightPopupEnabled(true);
        }
    }

    public int getSelectedClient() {
        int clientsNumber = pnlClients.getComponentCount();
        for (int i = 0; i < clientsNumber; i++) {
            if (pnlClients.getComponent(i) instanceof javax.swing.JToggleButton) {
                JToggleButton el = (JToggleButton) pnlClients.getComponent(i);
                if (el.isSelected() == true) {
                    try {
                        return Integer.parseInt(el.getName());
                    } catch (NumberFormatException ex) {
                        log.error(ex.getMessage());
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    public void clearAll() {
        pnlClients.removeAll();
        pnlClients.revalidate();
        pnlClients.repaint();
    }

    private JToggleButton createClientButton(String id, String address) {
        JToggleButton cmdClient01 = new JToggleButton();
        cmdClient01.setName(id);
        cmdClient01.setFont(new java.awt.Font("Monospaced", 0, 12));
        cmdClient01.setMargin(new java.awt.Insets(2, 4, 2, 4));
        cmdClient01.setText("<html>ID: " + StringPadder.leftPad(id, "0", 9) + "<br/>" + StringPadder.leftPad(address, " ", 22));
        cmdClient01.setMinimumSize(clientButtonDimension);
        cmdClient01.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        cmdClient01.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.getButton() == 3) {
                    popUpInvoker = ((JToggleButton) evt.getSource()).getName();
                    popup.show((JComponent) evt.getSource(), evt.getX(), evt.getY());
                }
                if (evt.getButton() == 1 && !((JToggleButton) evt.getSource()).isSelected()) {
                    fireClientSelected(((JToggleButton) evt.getSource()).getName());
                }
            }
        });
        return cmdClient01;
    }

    private void clearDisconnectedClients(ButtonGroup gr) {
        int clientsNumber = pnlClients.getComponentCount();
        for (int i = 0; i < clientsNumber; i++) {
            try {
                if (pnlClients.getComponent(i) instanceof javax.swing.JToggleButton) {
                    JToggleButton el = (JToggleButton) pnlClients.getComponent(i);
                    Set set = Server.getChannelsArray().entrySet();
                    Iterator it = set.iterator();
                    boolean flag = false;
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (el.getName().equalsIgnoreCase(entry.getKey().toString())) {
                            flag = true;
                        }
                    }
                    if (!flag) {
                        pnlClients.remove(el);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                log.error(ex.getMessage());
            }
        }
        if (pnlClients.getComponentCount() == 1 && ((JToggleButton) pnlClients.getComponent(0)).isSelected() == false) {
            ((JToggleButton) pnlClients.getComponent(0)).setSelected(true);
            fireClientSelected(((JToggleButton) pnlClients.getComponent(0)).getName());
        }
    }

    private boolean isClientAlreadyConnected(ButtonGroup gr, String name) {
        int clientsNumber = pnlClients.getComponentCount();
        for (int i = 0; i < clientsNumber; i++) {
            if (pnlClients.getComponent(i) instanceof javax.swing.JToggleButton) {
                JToggleButton el = (JToggleButton) pnlClients.getComponent(i);
                if (el.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        clientsButtonGroup = new javax.swing.ButtonGroup();
        pnlRight = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        setPreferredSize(new java.awt.Dimension(200, 100));
        setLayout(new java.awt.BorderLayout());
        pnlRight.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 0, 0, new java.awt.Color(153, 153, 153)), "Clients connected"));
        pnlRight.setMinimumSize(new java.awt.Dimension(200, 14));
        pnlRight.setPreferredSize(new java.awt.Dimension(200, 100));
        pnlRight.setLayout(new java.awt.BorderLayout());
        jScrollPane3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        pnlRight.add(jScrollPane3, java.awt.BorderLayout.CENTER);
        jLabel1.setText("<html><b>Hint:</b> Perform right click on a client<br/>to content menu call");
        pnlRight.add(jLabel1, java.awt.BorderLayout.PAGE_END);
        add(pnlRight, java.awt.BorderLayout.CENTER);
    }

    private javax.swing.ButtonGroup clientsButtonGroup;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JPanel pnlRight;

    protected javax.swing.event.EventListenerList CustomEventsListenerList = new javax.swing.event.EventListenerList();

    public void addEventsListener(ButtonBarListenerIF listener) {
        CustomEventsListenerList.add(ButtonBarListenerIF.class, listener);
    }

    public void removeEventsListener(ButtonBarListenerIF listener) {
        CustomEventsListenerList.remove(ButtonBarListenerIF.class, listener);
    }

    public void fireClientSelected(String ClientId) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ButtonBarListenerIF.class) {
                ((ButtonBarListenerIF) listeners[i + 1]).ClientSelected(ClientId);
            }
        }
    }

    public void fireOneSecondTimerTicked() {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ButtonBarListenerIF.class) {
                ((ButtonBarListenerIF) listeners[i + 1]).OneSecondTimerTicked();
            }
        }
    }

    public void fireDisconnectionRequested(String ClientId) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ButtonBarListenerIF.class) {
                ((ButtonBarListenerIF) listeners[i + 1]).DisconnectionRequested(ClientId);
            }
        }
    }

    public javax.swing.Timer getOneSecondTimer() {
        return oneSecondTimer;
    }
}
