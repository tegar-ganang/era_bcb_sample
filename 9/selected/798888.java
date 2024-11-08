package chequeredflag.gui.track;

import chequeredflag.data.track.Track;
import chequeredflag.gui.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author  barrie
 */
public class TrackGraphicalViewer extends javax.swing.JInternalFrame {

    private static final int PAN_AMOUNT = 100;

    /** Creates new form graphicalEditorWindow */
    public TrackGraphicalViewer() {
        initComponents();
        initActions();
        m_trackPanel = new TrackPanel();
        getContentPane().add(m_trackPanel, java.awt.BorderLayout.CENTER);
    }

    public void setTrack(Track track) {
        m_track = track;
        m_trackPanel.setTrack(track);
    }

    public void redrawTrackMap() {
        m_trackPanel.redrawMap();
    }

    public void selectedSegment(int sectionType, int segmentNo) {
        m_trackPanel.selectSegment(sectionType, segmentNo);
    }

    private Track m_track;

    private TrackPanel m_trackPanel;

    private void initComponents() {
        bottomPanel = new javax.swing.JPanel();
        panLeftButton = new javax.swing.JButton();
        panRightButton = new javax.swing.JButton();
        panUpButton = new javax.swing.JButton();
        panDownButton = new javax.swing.JButton();
        zoomInButton = new javax.swing.JButton();
        zoomOutButton = new javax.swing.JButton();
        layoutModeButton = new javax.swing.JToggleButton();
        mapMenuBar = new javax.swing.JMenuBar();
        zoomMenu = new javax.swing.JMenu();
        zoomInItem = new javax.swing.JMenuItem();
        zoomOutItem = new javax.swing.JMenuItem();
        panMenu = new javax.swing.JMenu();
        panLeftItem = new javax.swing.JMenuItem();
        panRightItem = new javax.swing.JMenuItem();
        panUpItem = new javax.swing.JMenuItem();
        panDownItem = new javax.swing.JMenuItem();
        setTitle("Track Map");
        bottomPanel.setLayout(new java.awt.GridLayout(1, 6));
        panLeftButton.setText("Left");
        panLeftButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panLeftButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(panLeftButton);
        panRightButton.setText("Right");
        panRightButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panRightButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(panRightButton);
        panUpButton.setText("Up");
        panUpButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panUpButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(panUpButton);
        panDownButton.setText("Down");
        panDownButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panDownButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(panDownButton);
        zoomInButton.setText("+");
        zoomInButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(zoomInButton);
        zoomOutButton.setText("-");
        zoomOutButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOutButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(zoomOutButton);
        layoutModeButton.setText("Layout Mode");
        layoutModeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleLayoutMode(evt);
            }
        });
        bottomPanel.add(layoutModeButton);
        getContentPane().add(bottomPanel, java.awt.BorderLayout.SOUTH);
        zoomMenu.setText("Zoom");
        zoomInItem.setText("Zoom In");
        zoomInItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomIn(evt);
            }
        });
        zoomMenu.add(zoomInItem);
        zoomOutItem.setText("Zoom Out");
        zoomOutItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOut(evt);
            }
        });
        zoomMenu.add(zoomOutItem);
        mapMenuBar.add(zoomMenu);
        panMenu.setText("Pan");
        panLeftItem.setText("Pan Left");
        panLeftItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panLeft(evt);
            }
        });
        panMenu.add(panLeftItem);
        panRightItem.setText("Pan Right");
        panRightItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panRight(evt);
            }
        });
        panMenu.add(panRightItem);
        panUpItem.setText("Pan Up");
        panUpItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panUp(evt);
            }
        });
        panMenu.add(panUpItem);
        panDownItem.setText("Pan Down");
        panDownItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panDown(evt);
            }
        });
        panMenu.add(panDownItem);
        mapMenuBar.add(panMenu);
        setJMenuBar(mapMenuBar);
        pack();
    }

    private void toggleLayoutMode(java.awt.event.ActionEvent evt) {
        boolean currentLayoutMode = false;
        if (m_track.getLayoutMode() == false) {
            currentLayoutMode = true;
        }
        m_track.setLayoutMode(currentLayoutMode);
        redrawTrackMap();
    }

    private void zoomOutButtonActionPerformed(java.awt.event.ActionEvent evt) {
        m_trackPanel.zoomOut();
    }

    private void zoomInButtonActionPerformed(java.awt.event.ActionEvent evt) {
        m_trackPanel.zoomIn();
    }

    private void panDownButtonActionPerformed(java.awt.event.ActionEvent evt) {
        m_trackPanel.panY(-PAN_AMOUNT);
    }

    private void panUpButtonActionPerformed(java.awt.event.ActionEvent evt) {
        m_trackPanel.panY(PAN_AMOUNT);
    }

    private void panRightButtonActionPerformed(java.awt.event.ActionEvent evt) {
        m_trackPanel.panX(-PAN_AMOUNT);
    }

    private void panLeftButtonActionPerformed(java.awt.event.ActionEvent evt) {
        m_trackPanel.panX(PAN_AMOUNT);
    }

    private void zoomIn(java.awt.event.ActionEvent evt) {
    }

    private void zoomOut(java.awt.event.ActionEvent evt) {
    }

    private void panLeft(java.awt.event.ActionEvent evt) {
    }

    private void panRight(java.awt.event.ActionEvent evt) {
    }

    private void panUp(java.awt.event.ActionEvent evt) {
    }

    private void panDown(java.awt.event.ActionEvent evt) {
    }

    private void initActions() {
        initAction(new AbstractAction("Zoom In") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.zoomIn();
            }
        }, KeyEvent.VK_ADD, 0, zoomInItem);
        zoomMenu.add(zoomInItem);
        initAction(new AbstractAction("Zoom Out") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.zoomOut();
            }
        }, KeyEvent.VK_SUBTRACT, 0, zoomOutItem);
        zoomMenu.add(zoomOutItem);
        initAction(new AbstractAction("Pan Left") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.panX(PAN_AMOUNT);
            }
        }, KeyEvent.VK_NUMPAD4, 0, panLeftItem);
        panMenu.add(panLeftItem);
        initAction(new AbstractAction("Pan Right") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.panX(-PAN_AMOUNT);
            }
        }, KeyEvent.VK_NUMPAD6, 0, panRightItem);
        panMenu.add(panRightItem);
        initAction(new AbstractAction("Pan Up") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.panY(PAN_AMOUNT);
            }
        }, KeyEvent.VK_NUMPAD8, 0, panUpItem);
        panMenu.add(panUpItem);
        initAction(new AbstractAction("Pan Down") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.panY(-PAN_AMOUNT);
            }
        }, KeyEvent.VK_NUMPAD2, 0, panDownItem);
        panMenu.add(panDownItem);
        initAction(new AbstractAction("Pan Down/Left") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.pan(PAN_AMOUNT, -PAN_AMOUNT);
            }
        }, KeyEvent.VK_NUMPAD1, 0, null);
        initAction(new AbstractAction("Pan Up/Left") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.pan(PAN_AMOUNT, PAN_AMOUNT);
            }
        }, KeyEvent.VK_NUMPAD7, 0, null);
        initAction(new AbstractAction("Pan Up/Right") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.pan(-PAN_AMOUNT, PAN_AMOUNT);
            }
        }, KeyEvent.VK_NUMPAD9, 0, null);
        initAction(new AbstractAction("Pan Down/Right") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.pan(-PAN_AMOUNT, -PAN_AMOUNT);
            }
        }, KeyEvent.VK_NUMPAD3, 0, null);
        initAction(new AbstractAction("Pan Center") {

            public void actionPerformed(final ActionEvent e) {
                m_trackPanel.panCenter();
            }
        }, KeyEvent.VK_NUMPAD5, 0, null);
    }

    /**
	 * Binds an action to a key stroke and optionally to a menu.
	 * @param action
	 * @param keyCode
	 * @param modifiers
	 * @param menuItem may be null
	 */
    private void initAction(final Action action, final int keyCode, final int modifiers, final JMenuItem menuItem) {
        final KeyStroke keyStroke = keyCode != 0 ? KeyStroke.getKeyStroke(keyCode, modifiers) : null;
        if (menuItem != null) {
            menuItem.setAction(action);
            if (keyStroke != null) menuItem.setAccelerator(keyStroke);
        } else {
            getActionMap().put(action.getValue(Action.NAME), action);
            if (keyStroke != null) getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, action.getValue(Action.NAME));
        }
    }

    private javax.swing.JPanel bottomPanel;

    private javax.swing.JToggleButton layoutModeButton;

    private javax.swing.JMenuBar mapMenuBar;

    private javax.swing.JButton panDownButton;

    private javax.swing.JMenuItem panDownItem;

    private javax.swing.JButton panLeftButton;

    private javax.swing.JMenuItem panLeftItem;

    private javax.swing.JMenu panMenu;

    private javax.swing.JButton panRightButton;

    private javax.swing.JMenuItem panRightItem;

    private javax.swing.JButton panUpButton;

    private javax.swing.JMenuItem panUpItem;

    private javax.swing.JButton zoomInButton;

    private javax.swing.JMenuItem zoomInItem;

    private javax.swing.JMenu zoomMenu;

    private javax.swing.JButton zoomOutButton;

    private javax.swing.JMenuItem zoomOutItem;
}
