package coopnetclient.frames;

import coopnetclient.ErrorHandler;
import coopnetclient.Globals;
import coopnetclient.enums.LogTypes;
import coopnetclient.enums.MapLoaderTypes;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.frames.clientframetabs.TabOrganizer;
import coopnetclient.utils.ui.Colorizer;
import coopnetclient.utils.Logger;
import coopnetclient.utils.RoomData;
import coopnetclient.utils.gamedatabase.GameDatabase;
import coopnetclient.utils.gamedatabase.GameSetting;
import coopnetclient.utils.launcher.Launcher;
import coopnetclient.utils.launcher.TempGameSettings;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public class GameSettingsFrame extends javax.swing.JFrame {

    private ArrayList<JLabel> labels = new ArrayList<JLabel>();

    private ArrayList<Component> inputfields = new ArrayList<Component>();

    private boolean[] enabledInputfieldsBeforeDisable;

    private boolean mapsEnabledBeforeDisable;

    private boolean lastEnableAction = true;

    private RoomData roomData;

    /** Creates new form GameSettingsPanel */
    public GameSettingsFrame(RoomData roomData) {
        initComponents();
        this.roomData = roomData;
        lbl_map.setVisible(false);
        cb_map.setVisible(false);
        cb_map.setEnabled(false);
        customize();
        this.getRootPane().setDefaultButton(btn_save);
        AbstractAction act = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                btn_close.doClick();
            }
        };
        getRootPane().getActionMap().put("close", act);
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        this.setLocationRelativeTo(null);
        Colorizer.colorize(this);
        this.pack();
        if (roomData.isInstant()) {
            btn_save.setText("Launch");
            btn_close.setText("Cancel");
        }
        decideVisibility();
    }

    private void decideVisibility() {
        if (roomData.isHost() && GameDatabase.getLocalSettingCount(roomData.getChannel(), roomData.getModName()) + GameDatabase.getServerSettingCount(roomData.getChannel(), roomData.getModName()) > 0) {
            if (!roomData.isInstant()) {
                btn_close.setEnabled(false);
            }
            setVisible(true);
        } else if (!roomData.isHost() && GameDatabase.getLocalSettingCount(roomData.getChannel(), roomData.getModName()) > 0) {
            if (!roomData.isInstant()) {
                btn_close.setEnabled(false);
            }
            setVisible(true);
        } else {
            if (!roomData.isInstant()) {
                TabOrganizer.getRoomPanel().initDone();
            } else {
                setVisible(true);
            }
        }
    }

    public void setEnabledOfGameSettingsFrameSettings(boolean enabled) {
        if (!enabled && lastEnableAction) {
            enabledInputfieldsBeforeDisable = new boolean[inputfields.size()];
            for (int i = 0; i < enabledInputfieldsBeforeDisable.length; i++) {
                enabledInputfieldsBeforeDisable[i] = inputfields.get(i).isEnabled() && inputfields.get(i).isVisible();
                inputfields.get(i).setEnabled(false);
            }
            mapsEnabledBeforeDisable = cb_map.isEnabled() && cb_map.isVisible();
            cb_map.setEnabled(false);
        } else if (enabled && !lastEnableAction) {
            for (int i = 0; i < enabledInputfieldsBeforeDisable.length; i++) {
                if (enabledInputfieldsBeforeDisable[i]) {
                    inputfields.get(i).setEnabled(true);
                }
            }
            if (mapsEnabledBeforeDisable) {
                cb_map.setEnabled(true);
            }
        } else {
            throw new IllegalStateException("setEnabledOfGameSettingsFrameSettings should only support toggle!");
        }
        lastEnableAction = enabled;
    }

    private void customize() {
        if (GameDatabase.getMapExtension(roomData.getChannel(), roomData.getModName()) != null) {
            lbl_map.setVisible(true);
            cb_map.setVisible(true);
            if (GameDatabase.getMapLoaderType(roomData.getChannel(), roomData.getModName()) == MapLoaderTypes.FILE) {
                cb_map.setModel(new DefaultComboBoxModel(loadFileMaps()));
            } else if (GameDatabase.getMapLoaderType(roomData.getChannel(), roomData.getModName()) == MapLoaderTypes.PK3) {
                cb_map.setModel(new DefaultComboBoxModel(loadPK3Maps()));
            }
            cb_map.setSelectedItem(TempGameSettings.getMap());
            if (cb_map.getSelectedItem() == null && cb_map.getItemCount() > 0) {
                cb_map.setSelectedIndex(0);
            }
            cb_map.setEnabled(roomData.isHost());
        }
        GridBagConstraints firstcolumn = new GridBagConstraints();
        GridBagConstraints secondcolumn = new GridBagConstraints();
        int serverrowindex = 1;
        int localrowindex = 0;
        int localcount = 0;
        ArrayList<GameSetting> settings = GameDatabase.getGameSettings(roomData.getChannel(), roomData.getModName());
        firstcolumn.gridwidth = 1;
        firstcolumn.gridheight = 1;
        firstcolumn.fill = GridBagConstraints.HORIZONTAL;
        firstcolumn.ipadx = 40;
        firstcolumn.anchor = GridBagConstraints.EAST;
        firstcolumn.weightx = 0;
        firstcolumn.weighty = 0;
        firstcolumn.insets = new Insets(5, 5, 5, 5);
        firstcolumn.gridx = 0;
        secondcolumn.gridwidth = 1;
        secondcolumn.gridheight = 1;
        secondcolumn.fill = GridBagConstraints.HORIZONTAL;
        secondcolumn.ipadx = 0;
        secondcolumn.anchor = GridBagConstraints.CENTER;
        secondcolumn.weightx = 1.0;
        secondcolumn.weighty = 0;
        secondcolumn.insets = new Insets(5, 5, 5, 5);
        secondcolumn.gridx = 1;
        for (GameSetting gs : settings) {
            if (gs.isLocal()) {
                localcount++;
                firstcolumn.gridy = localrowindex;
                secondcolumn.gridy = localrowindex;
            } else {
                firstcolumn.gridy = serverrowindex;
                secondcolumn.gridy = serverrowindex;
            }
            JLabel label = new JLabel(gs.getName());
            label.setHorizontalAlignment(JLabel.RIGHT);
            Component input = null;
            switch(gs.getType()) {
                case TEXT:
                    {
                        input = new JTextField(gs.getDefaultValue());
                        String currentValue = TempGameSettings.getGameSettingValue(gs.getName());
                        if (currentValue != null && currentValue.length() > 0) {
                            ((JTextField) input).setText(currentValue);
                        }
                        if (!roomData.isHost() && !gs.isLocal()) {
                            input.setEnabled(false);
                        }
                        break;
                    }
                case NUMBER:
                    {
                        int def = 0;
                        def = Integer.valueOf((gs.getDefaultValue() == null || gs.getDefaultValue().length() == 0) ? "0" : gs.getDefaultValue());
                        int min = Integer.valueOf(gs.getMinValue());
                        int max = Integer.valueOf(gs.getMaxValue());
                        if (min <= max && min <= def && def <= max) {
                            input = new JSpinner(new SpinnerNumberModel(def, min, max, 1));
                        } else {
                            input = new JSpinner();
                        }
                        String currentValue = TempGameSettings.getGameSettingValue(gs.getName());
                        if (currentValue != null && currentValue.length() > 0) {
                            ((JSpinner) input).setValue(Integer.valueOf(currentValue));
                        }
                        if (!roomData.isHost() && !gs.isLocal()) {
                            input.setEnabled(false);
                        }
                        break;
                    }
                case CHOICE:
                    {
                        if (!roomData.isHost() && !gs.isLocal()) {
                            input = new JTextField(gs.getDefaultValue());
                            String currentValue = TempGameSettings.getGameSettingValue(gs.getName());
                            if (gs.getDefaultValue() != null && gs.getDefaultValue().length() > 0) {
                                ((JTextField) input).setText(gs.getDefaultValue());
                            }
                            if (currentValue != null && currentValue.length() > 0) {
                                ((JTextField) input).setText(currentValue);
                            }
                            input.setEnabled(false);
                        } else {
                            input = new JComboBox(gs.getComboboxSelectNames().toArray());
                            if (gs.getDefaultValue() != null && gs.getDefaultValue().length() > 0) {
                                int idx = -1;
                                idx = gs.getComboboxSelectNames().indexOf(gs.getDefaultValue());
                                if (idx > -1) {
                                    ((JComboBox) input).setSelectedIndex(idx);
                                }
                            }
                            String currentValue = TempGameSettings.getGameSettingValue(gs.getName());
                            if (currentValue != null && currentValue.length() > 0) {
                                ((JComboBox) input).setSelectedItem(currentValue);
                            }
                        }
                        break;
                    }
            }
            labels.add(label);
            inputfields.add(input);
            if (inputfields.size() == 1) {
            } else {
            }
            if (gs.isLocal()) {
                pnl_localSettings.add(label, firstcolumn);
                pnl_localSettings.add(input, secondcolumn);
                localrowindex++;
            } else {
                pnl_serverSettings.add(label, firstcolumn);
                pnl_serverSettings.add(input, secondcolumn);
                serverrowindex++;
            }
        }
        if (localcount == 0) {
            pnl_localSettings.setVisible(false);
        }
    }

    private String[] loadFileMaps() {
        String extension = GameDatabase.getMapExtension(roomData.getChannel(), roomData.getModName());
        String path = GameDatabase.getFullMapPath(roomData.getChannel(), roomData.getModName());
        Logger.log(LogTypes.LOG, "Loading maps from: " + path);
        if (path.endsWith("\\") || path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        File mapdir = new File(path);
        if (!mapdir.isDirectory()) {
            return new String[0];
        }
        File[] files = mapdir.listFiles();
        Vector<String> names = new Vector<String>();
        for (File f : files) {
            if (f.isFile()) {
                String tmp = f.getName();
                if (tmp.toLowerCase().endsWith(extension.toLowerCase())) {
                    names.add(tmp.substring(0, tmp.length() - (extension.length() + 1)));
                }
            }
        }
        return names.toArray(new String[names.size()]);
    }

    private ArrayList<File> getPK3Files(ArrayList<File> list, File baseDir) {
        for (File f : baseDir.listFiles()) {
            if (f.isDirectory()) {
                getPK3Files(list, f);
            } else {
                if (f.getName().toLowerCase().endsWith("pk3") || f.getName().endsWith("pk4")) {
                    list.add(f);
                }
            }
        }
        return list;
    }

    private String getMapNameFromEntry(String entry) {
        Pattern pattern = Pattern.compile(GameDatabase.getMapPath(roomData.getChannel(), roomData.getModName()).replace('\\', '/') + "([\\p{Alnum}\\p{Punct}&&[^/\\\\]]+)\\." + GameDatabase.getMapExtension(roomData.getChannel(), roomData.getModName()).toLowerCase());
        Matcher matcher = pattern.matcher(entry.toLowerCase());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private String[] loadPK3Maps() {
        String pk3FindPath = GameDatabase.getInstallPath(roomData.getChannel()) + GameDatabase.getPK3FindPath(roomData.getChannel(), roomData.getModName());
        Vector<String> names = new Vector<String>();
        ArrayList<File> pk3Files = new ArrayList<File>();
        getPK3Files(pk3Files, new File(pk3FindPath));
        for (File pk3File : pk3Files) {
            try {
                ZipFile zf = new ZipFile(pk3File);
                for (Enumeration entries = zf.entries(); entries.hasMoreElements(); ) {
                    String zipEntryName = ((ZipEntry) entries.nextElement()).getName();
                    String mapFileName = getMapNameFromEntry(zipEntryName);
                    if (mapFileName != null) {
                        names.add(mapFileName);
                    }
                }
            } catch (IOException e) {
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        btn_save = new javax.swing.JButton();
        pnl_serverSettings = new javax.swing.JPanel();
        lbl_map = new javax.swing.JLabel();
        cb_map = new javax.swing.JComboBox();
        pnl_localSettings = new javax.swing.JPanel();
        btn_close = new javax.swing.JButton();
        setTitle("Game settings");
        setFocusTraversalPolicyProvider(true);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        btn_save.setText("Save");
        btn_save.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_saveActionPerformed(evt);
            }
        });
        pnl_serverSettings.setBorder(javax.swing.BorderFactory.createTitledBorder("Server settings"));
        pnl_serverSettings.setLayout(new java.awt.GridBagLayout());
        lbl_map.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lbl_map.setText("Map:");
        lbl_map.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 40;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        pnl_serverSettings.add(lbl_map, gridBagConstraints);
        cb_map.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        pnl_serverSettings.add(cb_map, gridBagConstraints);
        pnl_localSettings.setBorder(javax.swing.BorderFactory.createTitledBorder("Local settings"));
        pnl_localSettings.setLayout(new java.awt.GridBagLayout());
        btn_close.setText("Close");
        btn_close.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_closeActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(btn_save).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btn_close).addGap(318, 318, 318)).addComponent(pnl_serverSettings, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE).addComponent(pnl_localSettings, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE));
        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { btn_close, btn_save });
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(pnl_serverSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pnl_localSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btn_save).addComponent(btn_close)).addContainerGap()));
        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { btn_close, btn_save });
        pack();
    }

    private void btn_saveActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (cb_map.isVisible()) {
                if (cb_map.getSelectedItem() != null && cb_map.isEnabled()) {
                    TempGameSettings.setMap(cb_map.getSelectedItem().toString());
                    if (roomData.isHost() && !roomData.isInstant()) {
                        Protocol.sendSetting("map", cb_map.getSelectedItem().toString());
                    }
                }
            }
            String name;
            String value = "save-error";
            for (int i = 0; i < labels.size(); i++) {
                name = labels.get(i).getText();
                Component input = inputfields.get(i);
                if (input instanceof JTextField) {
                    value = ((JTextField) input).getText();
                } else if (input instanceof JSpinner) {
                    value = ((JSpinner) input).getValue() + "";
                } else if (input instanceof JComboBox) {
                    value = ((JComboBox) input).getSelectedItem().toString();
                }
                if (input.isEnabled()) {
                    TempGameSettings.setGameSetting(name, value, (roomData.isHost() && !roomData.isInstant()));
                }
            }
            if (!roomData.isInstant() && !btn_close.isEnabled()) {
                TabOrganizer.getRoomPanel().initDone();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (roomData.isInstant()) {
            FrameOrganizer.closeGameSettingsFrame();
            new Thread() {

                @Override
                public void run() {
                    try {
                        Launcher.instantLaunch(true);
                    } catch (Exception e) {
                        ErrorHandler.handle(e);
                    }
                }
            }.start();
        } else {
            this.setVisible(false);
        }
        btn_close.setEnabled(true);
    }

    public void updateValues() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                cb_map.setSelectedItem(TempGameSettings.getMap());
                for (int i = 0; i < labels.size(); i++) {
                    String name = labels.get(i).getText();
                    Component input = inputfields.get(i);
                    String value = TempGameSettings.getGameSettingValue(name);
                    if (value != null && value.length() > 0) {
                        if (input instanceof JTextField) {
                            ((JTextField) input).setText(value);
                        } else if (input instanceof JSpinner) {
                            int j;
                            try {
                                j = Integer.valueOf(value);
                                ((JSpinner) input).setValue(j);
                            } catch (NumberFormatException nfe) {
                            }
                        } else if (input instanceof JComboBox) {
                            ((JComboBox) input).setSelectedItem(value);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        boolean foundEnabledField = cb_map.isEnabled() && cb_map.isVisible();
        for (int i = 0; i < inputfields.size() && !foundEnabledField; i++) {
            foundEnabledField = inputfields.get(i).isEnabled();
        }
        btn_save.setEnabled(foundEnabledField || roomData.isInstant());
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        if (!btn_close.isEnabled()) {
            btn_save.doClick();
        } else {
            if (roomData.isInstant()) {
                FrameOrganizer.closeGameSettingsFrame();
            } else {
                this.setVisible(false);
            }
        }
    }

    private void btn_closeActionPerformed(java.awt.event.ActionEvent evt) {
        if (roomData.isInstant()) {
            FrameOrganizer.closeGameSettingsFrame();
            Launcher.deInitialize();
        } else {
            this.setVisible(false);
        }
    }

    private javax.swing.JButton btn_close;

    private javax.swing.JButton btn_save;

    private javax.swing.JComboBox cb_map;

    private javax.swing.JLabel lbl_map;

    private javax.swing.JPanel pnl_localSettings;

    private javax.swing.JPanel pnl_serverSettings;
}
