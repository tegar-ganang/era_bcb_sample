import java.util.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import org.biomage.QuantitationType.*;
import org.biomage.BioAssay.Channel;
import org.biomage.Interface.*;
import org.biomage.Common.MAGEJava;
import org.biomage.tools.xmlutils.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Column Configure Frame
 * 
 */
public class ColumnConfigFrame extends MageFrame implements MageGuiErrors {

    private ArrayList panels;

    JPanel basePanel;

    GridBagLayout baseGbl;

    private FieldConfigModel fcModel;

    File xmlFile;

    /**
     * Constructs ColumnConfigFrame
     *
     * @param int colNum
     *
     */
    public ColumnConfigFrame(int colNum) {
        panels = new ArrayList(colNum);
        fcModel = new FieldConfigModel();
        FieldConfigPanel.setMaxId(colNum);
        setTitle("Column Configuration");
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();
        setLocation(screenSize.width / 8 + getFrameCounter() * 10, screenSize.height / 8 + getFrameCounter() * 20);
        Container contentPane = getContentPane();
        basePanel = new JPanel();
        baseGbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        basePanel.setLayout(baseGbl);
        basePanel.setBackground(bgColor);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(10, 10, 0, 0);
        JLabel blankLabel = new JLabel();
        baseGbl.setConstraints(blankLabel, gbc);
        basePanel.add(blankLabel);
        buildMenu();
        gbc.insets = new Insets(0, 10, 10, 0);
        for (int i = 0; i < colNum; i++) {
            FieldConfigPanel panel = new FieldConfigPanel(i + 1);
            panels.add(i, panel);
            gbc.gridy = i + 1;
            baseGbl.setConstraints(panel, gbc);
            basePanel.add(panel);
        }
        JScrollPane scrollPane = new JScrollPane(basePanel);
        contentPane.add(scrollPane);
        pack();
        Dimension dim = getPreferredSize();
        if (dim.getHeight() > screenSize.height * 0.8) {
            setSize((int) (dim.getWidth()), (int) (screenSize.height * 0.8));
        }
    }

    /**
     * Overriding dispose. If Mage GUI doesn't have any more frames, exit.
     * 
     */
    public void dispose() {
        super.dispose();
        if (getFrameCounter() == 0) {
            System.exit(0);
        }
    }

    private void buildMenu() {
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menubar.add(fileMenu);
        buildFileMenuItems(fileMenu);
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        menubar.add(toolsMenu);
        buildToolsMenuItems(toolsMenu);
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menubar.add(helpMenu);
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ImageIcon mageIcon = new ImageIcon("images/om-oi-mini.png");
                JOptionPane.showMessageDialog(ColumnConfigFrame.this, "MAGE Java version", "About", JOptionPane.PLAIN_MESSAGE, mageIcon);
            }
        });
        helpMenu.add(aboutItem);
    }

    private void saveFile() {
        if (xmlFile == null) {
            System.err.println("Error: Save File is null\n");
            return;
        }
        fcModel.clearAll();
        preparePanels();
        Iterator panelIter = panels.iterator();
        while (panelIter.hasNext()) {
            FieldConfigPanel fpanel = (FieldConfigPanel) panelIter.next();
            fcModel.addQuanType(fpanel.getQuanType());
        }
        ArrayList chans = FieldConfigPanel.getChannels();
        Iterator chanIter = chans.iterator();
        while (chanIter.hasNext()) {
            Channel chan = (Channel) chanIter.next();
            fcModel.addChannel(chan);
        }
        try {
            fcModel.writeMAGEML(xmlFile);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void buildFileMenuItems(JMenu fileMenu) {
        final JFileChooser fc = new JFileChooser();
        XmlFileFilter filter = new XmlFileFilter();
        fc.setFileFilter(filter);
        JMenuItem newItem = new JMenuItem("New", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        newItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                IntroFrame frame = new IntroFrame();
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setVisible(true);
            }
        });
        fileMenu.add(newItem);
        JMenuItem loadItem = new JMenuItem("Load...", KeyEvent.VK_L);
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        loadItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int retVal = fc.showOpenDialog(ColumnConfigFrame.this);
                if (retVal == JFileChooser.APPROVE_OPTION) {
                    File file = formatFileName(fc.getSelectedFile());
                    if (file.exists()) {
                        xmlFile = file;
                        setTitle("Column Configuration  - " + xmlFile.getPath());
                        try {
                            XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
                            MAGEContentHandler cHandler = new MAGEContentHandler();
                            parser.setContentHandler(cHandler);
                            String abpath = xmlFile.getAbsolutePath();
                            parser.parse(abpath);
                            MAGEJava mageJava = cHandler.getMAGEJava();
                            if (mageJava != null) {
                                QuantitationType_package.QuantitationType_list quanList = (mageJava.getQuantitationType_package()).getQuantitationType_list();
                                int lim = 0;
                                boolean loadFewer = false;
                                if (FieldConfigPanel.getMaxId() <= quanList.size()) {
                                    lim = FieldConfigPanel.getMaxId();
                                } else {
                                    lim = quanList.size();
                                    loadFewer = true;
                                }
                                for (int i = 0; i < lim; i++) {
                                    QuantitationType qt = (QuantitationType) quanList.get(i);
                                    FieldConfigPanel fp = (FieldConfigPanel) panels.get(i);
                                    fp.copyFrom(qt);
                                }
                                if (loadFewer) {
                                    for (int i = lim; i < FieldConfigPanel.getMaxId(); i++) {
                                        FieldConfigPanel delP = (FieldConfigPanel) panels.get(lim);
                                        panels.remove(delP);
                                        basePanel.remove(delP);
                                    }
                                    FieldConfigPanel.setMaxId(lim);
                                } else {
                                    GridBagConstraints gbc = new GridBagConstraints();
                                    gbc.anchor = GridBagConstraints.NORTHWEST;
                                    gbc.fill = GridBagConstraints.NONE;
                                    gbc.weightx = 1;
                                    gbc.weighty = 1;
                                    gbc.insets = new Insets(0, 10, 10, 0);
                                    for (int i = lim; i < quanList.size(); i++) {
                                        QuantitationType qt = (QuantitationType) quanList.get(i);
                                        FieldConfigPanel panel = new FieldConfigPanel(i + 1);
                                        panel.copyFrom(qt);
                                        panels.add(i, panel);
                                        gbc.gridy = i + 1;
                                        baseGbl.setConstraints(panel, gbc);
                                        basePanel.add(panel);
                                    }
                                    FieldConfigPanel.setMaxId(quanList.size());
                                }
                                Iterator panIter = panels.iterator();
                                while (panIter.hasNext()) {
                                    FieldConfigPanel derefedPanel = (FieldConfigPanel) panIter.next();
                                    QuantitationType qt = derefedPanel.getQuanType();
                                    HasConfidenceIndicators.ConfidenceIndicators_list clist = qt.getConfidenceIndicators();
                                    if (clist.size() > 0) {
                                        Iterator ciIter = clist.iterator();
                                        while (ciIter.hasNext()) {
                                            ConfidenceIndicator confInd = (ConfidenceIndicator) ciIter.next();
                                            String id = confInd.getIdentifier();
                                            for (int i = 0; i < FieldConfigPanel.getMaxId(); i++) {
                                                FieldConfigPanel setPanel = (FieldConfigPanel) panels.get(i);
                                                if (id.equals(setPanel.getQuanType().getIdentifier())) {
                                                    setPanel.setConfIndRef(derefedPanel.getFieldId());
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                ColumnConfigFrame.this.pack();
                            }
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        } catch (SAXException exception) {
                            exception.printStackTrace();
                        }
                    } else {
                        JOptionPane.showMessageDialog(ColumnConfigFrame.this, "No such file, " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        fileMenu.add(loadItem);
        JMenuItem saveItem = new JMenuItem("Save", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                String errMsg = errorCheck();
                if (!errMsg.equals("")) {
                    JOptionPane.showMessageDialog(ColumnConfigFrame.this, errMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    if (xmlFile != null) {
                        saveFile();
                    } else {
                        int retVal = fc.showSaveDialog(ColumnConfigFrame.this);
                        if (retVal == JFileChooser.APPROVE_OPTION) {
                            xmlFile = formatFileName(fc.getSelectedFile());
                            setTitle("Column Configuration  - " + xmlFile.getPath());
                            saveFile();
                        }
                    }
                }
            }
        });
        fileMenu.add(saveItem);
        JMenuItem saveAsItem = new JMenuItem("Save As...", KeyEvent.VK_A);
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        saveAsItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String errMsg = errorCheck();
                if (!errMsg.equals("")) {
                    JOptionPane.showMessageDialog(ColumnConfigFrame.this, errMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    int retVal = fc.showSaveDialog(ColumnConfigFrame.this);
                    if (retVal == JFileChooser.APPROVE_OPTION) {
                        File savingFile = fc.getSelectedFile();
                        xmlFile = formatFileName(savingFile);
                        setTitle("Column Configuration  - " + xmlFile.getPath());
                        saveFile();
                    }
                }
            }
        });
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        JMenuItem closeItem = new JMenuItem("Close", KeyEvent.VK_C);
        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        closeItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        exitItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        fileMenu.add(exitItem);
    }

    void buildToolsMenuItems(JMenu toolsMenu) {
        JMenuItem insertRowItem = new JMenuItem("Insert rows", KeyEvent.VK_R);
        insertRowItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        insertRowItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                InsertRowsDialog insertRowsDiag = new InsertRowsDialog(ColumnConfigFrame.this, "Insert Rows");
                insertRowsDiag.setLocationRelativeTo(ColumnConfigFrame.this);
                insertRowsDiag.setVisible(true);
                if (insertRowsDiag.getNumRows() > 0) {
                    int beforeMax = FieldConfigPanel.getMaxId();
                    int afterMax = beforeMax + insertRowsDiag.getNumRows();
                    FieldConfigPanel.setMaxId(afterMax);
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.anchor = GridBagConstraints.NORTHWEST;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.weightx = 1;
                    gbc.weighty = 1;
                    gbc.insets = new Insets(0, 10, 10, 0);
                    int startLoc = insertRowsDiag.getRowLocation();
                    int addNum = insertRowsDiag.getNumRows();
                    for (int i = startLoc; i < beforeMax; i++) {
                        FieldConfigPanel panel = (FieldConfigPanel) panels.get(i);
                        panel.setFieldId(i + addNum + 1);
                        gbc.gridy = i + addNum + 1;
                        baseGbl.setConstraints(panel, gbc);
                    }
                    for (int i = startLoc; i < startLoc + addNum; i++) {
                        FieldConfigPanel panel = new FieldConfigPanel(i + 1);
                        panels.add(i, panel);
                        gbc.gridy = i + 1;
                        baseGbl.setConstraints(panel, gbc);
                        basePanel.add(panel);
                    }
                    ColumnConfigFrame.this.pack();
                }
            }
        });
        toolsMenu.add(insertRowItem);
        JMenuItem copyRowItem = new JMenuItem("Copy rows");
        copyRowItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                CopyRowsDialog copyRowsDiag = new CopyRowsDialog(ColumnConfigFrame.this, "Copy Row");
                copyRowsDiag.setLocationRelativeTo(ColumnConfigFrame.this);
                copyRowsDiag.setVisible(true);
                if (copyRowsDiag.getRowCopyFrom() > 0) {
                    int fromRow = copyRowsDiag.getRowCopyFrom();
                    ArrayList toRows = copyRowsDiag.getRowsCopyTo();
                    FieldConfigPanel fromPanel = (FieldConfigPanel) panels.get(fromRow - 1);
                    Iterator rowIter = toRows.iterator();
                    while (rowIter.hasNext()) {
                        FieldConfigPanel toPanel = (FieldConfigPanel) panels.get(((Integer) rowIter.next()).intValue() - 1);
                        toPanel.copyFrom(fromPanel);
                    }
                }
            }
        });
        toolsMenu.add(copyRowItem);
        JMenuItem deleteRowItem = new JMenuItem("Delete rows", KeyEvent.VK_D);
        deleteRowItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
        deleteRowItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                DeleteRowsDialog deleteRowsDiag = new DeleteRowsDialog(ColumnConfigFrame.this, "Delete Row(s)");
                deleteRowsDiag.setLocationRelativeTo(ColumnConfigFrame.this);
                deleteRowsDiag.setVisible(true);
                ArrayList delRows = deleteRowsDiag.getDeletingRows();
                if (delRows.size() > 0) {
                    Iterator rowNumIter = delRows.iterator();
                    ArrayList delPanels = new ArrayList(delRows.size());
                    while (rowNumIter.hasNext()) {
                        FieldConfigPanel selPanel = (FieldConfigPanel) panels.get(((Integer) rowNumIter.next()).intValue() - 1);
                        if (selPanel.getFieldId() == 1) {
                            int freeIdx = 2;
                            for (; freeIdx < FieldConfigPanel.getMaxId(); freeIdx++) {
                                if (delRows.contains(new Integer(freeIdx)) == false) {
                                    break;
                                }
                            }
                            freeIdx--;
                            FieldConfigPanel swapPanel = (FieldConfigPanel) panels.get(freeIdx);
                            selPanel.copyFrom(swapPanel);
                            selPanel = swapPanel;
                        }
                        delPanels.add(selPanel);
                    }
                    Iterator panelIter = delPanels.iterator();
                    while (panelIter.hasNext()) {
                        FieldConfigPanel delP = (FieldConfigPanel) panelIter.next();
                        panels.remove(delP);
                        basePanel.remove(delP);
                    }
                    FieldConfigPanel.setMaxId(FieldConfigPanel.getMaxId() - delRows.size());
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.anchor = GridBagConstraints.NORTHWEST;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.weightx = 1;
                    gbc.weighty = 1;
                    gbc.insets = new Insets(0, 10, 10, 0);
                    for (int i = 0; i < FieldConfigPanel.getMaxId(); i++) {
                        FieldConfigPanel fp = (FieldConfigPanel) panels.get(i);
                        fp.setFieldId(i + 1);
                        gbc.gridy = i + 1;
                        baseGbl.setConstraints(fp, gbc);
                    }
                    ColumnConfigFrame.this.pack();
                }
            }
        });
        toolsMenu.add(deleteRowItem);
    }

    private File formatFileName(File f) {
        String fname = f.getName();
        if (!fname.toLowerCase().endsWith(".xml")) {
            f = new File(f.getPath() + ".xml");
        }
        return f;
    }

    private String errorCheck() {
        String msg = new String("");
        Iterator panelIter = panels.iterator();
        while (panelIter.hasNext()) {
            FieldConfigPanel fpanel = (FieldConfigPanel) panelIter.next();
            if (fpanel.checkValid() == INCOMPLETE_FIELD) {
                msg = "Panel " + fpanel.getFieldId() + ": Incomplete field(s).";
                break;
            } else if (fpanel.checkValid() == INVALID_CONF_IND) {
                msg = "Panel " + fpanel.getFieldId() + ": Invalid Confidence Indicator.";
                break;
            }
        }
        return msg;
    }

    private void preparePanels() {
        Iterator panelIter = panels.iterator();
        while (panelIter.hasNext()) {
            FieldConfigPanel fpanel = (FieldConfigPanel) panelIter.next();
            fpanel.makeMageObjects();
        }
        panelIter = panels.iterator();
        while (panelIter.hasNext()) {
            FieldConfigPanel fpanel = (FieldConfigPanel) panelIter.next();
            int confRef = fpanel.getConfIndRef();
            if (confRef != 0) {
                FieldConfigPanel derefCI = (FieldConfigPanel) panels.get(fpanel.getConfIndRef() - 1);
                derefCI.addConfidenceIndicator((ConfidenceIndicator) fpanel.getQuanType());
            }
        }
    }
}
