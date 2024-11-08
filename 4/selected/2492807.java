package jpcsp.Debugger;

import jpcsp.Emulator;
import jpcsp.Resource;
import jpcsp.settings.Settings;

public class ElfHeaderInfo extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;

    public static String PbpInfo;

    public static String ElfInfo;

    public static String ProgInfo;

    public static String SectInfo;

    /** Creates new form ElfHeaderInfo */
    public ElfHeaderInfo() {
        initComponents();
        ELFInfoArea.append(PbpInfo);
        ELFInfoArea.append(ElfInfo);
        ELFInfoArea.append(ProgInfo);
        ELFInfoArea.append(SectInfo);
    }

    public void RefreshWindow() {
        ELFInfoArea.setText("");
        ELFInfoArea.append(PbpInfo);
        ELFInfoArea.append(ElfInfo);
        ELFInfoArea.append(ProgInfo);
        ELFInfoArea.append(SectInfo);
    }

    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        ELFInfoArea = new javax.swing.JTextArea();
        setTitle(Resource.get("elfheaderinfo"));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });
        ELFInfoArea.setColumns(20);
        ELFInfoArea.setEditable(false);
        ELFInfoArea.setFont(new java.awt.Font("Courier New", 0, 12));
        ELFInfoArea.setLineWrap(true);
        ELFInfoArea.setRows(5);
        jScrollPane1.setViewportView(ELFInfoArea);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE));
        pack();
    }

    private void formWindowDeactivated(java.awt.event.WindowEvent evt) {
        if (Settings.getInstance().readBool("gui.saveWindowPos")) Settings.getInstance().writeWindowPos("elfheader", getLocation());
    }

    @Override
    public void dispose() {
        Emulator.getMainGUI().endWindowDialog();
        super.dispose();
    }

    private javax.swing.JTextArea ELFInfoArea;

    private javax.swing.JScrollPane jScrollPane1;
}
