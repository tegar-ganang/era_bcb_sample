package gov.sns.apps.mtv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.ca.*;

/**
 * The window representation / view of an xiodiag document
 *
 * @author  jdg
 */
public class MagnetPanel extends JPanel implements ItemListener {

    private MTVDocument theDoc;

    private ArrayList<String> magTypes, selectedMagTypes;

    private ArrayList<JCheckBox> magCheckBoxes;

    protected ArrayList<String> magnetNames;

    protected ArrayList<PVTableCell> B_Sets, B_RBs, B_Trim_Sets, B_Books;

    private JPanel magCheckBoxPanel;

    private JButton updateTableButton;

    private JTable magnetTable;

    protected MagnetTableModel magnetTableModel;

    private JScrollPane tableScrollPane;

    protected javax.swing.Timer timer;

    public ArrayList<String> getSelectedTypes() {
        return selectedMagTypes;
    }

    /** Creates a new instance of MainWindow */
    public MagnetPanel(MTVDocument aDocument) {
        theDoc = aDocument;
        selectedMagTypes = new ArrayList();
        magTypes = new ArrayList();
        magCheckBoxes = new ArrayList();
        magnetNames = new ArrayList();
        B_Sets = new ArrayList();
        B_RBs = new ArrayList();
        B_Trim_Sets = new ArrayList();
        B_Books = new ArrayList();
        magCheckBoxPanel = new JPanel(new FlowLayout());
        updateTableButton = new JButton("Make Table");
        updateTableButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateMagnetTable();
            }
        });
        makeMagnetTable();
        tableScrollPane = new JScrollPane(magnetTable);
        ActionListener taskPerformer = new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                tableTask();
            }
        };
        timer = new javax.swing.Timer(2000, taskPerformer);
        timer.start();
    }

    /** the action to perform when the tables need updating */
    protected void tableTask() {
        magnetTableModel.fireTableRowsUpdated(0, magnetTableModel.getRowCount());
    }

    /** find the magnet types in the selcted accelerator sequence */
    protected void updateMagnetTypes() {
        magTypes.clear();
        selectedMagTypes.clear();
        magCheckBoxes.clear();
        magTypes.add("all");
        JCheckBox magBox = new JCheckBox("magnet");
        magBox.addItemListener(this);
        magCheckBoxes.add(magBox);
        java.util.List<AcceleratorNode> magNodes = theDoc.getSelectedSequence().getNodesOfType("magnet");
        for (AcceleratorNode mag : magNodes) {
            String type = mag.getType();
            if (!magTypes.contains(type)) {
                magTypes.add(type);
                magBox = new JCheckBox(type);
                magBox.setSelected(false);
                magBox.addItemListener(this);
                magCheckBoxes.add(magBox);
                System.out.println(type);
            }
        }
    }

    public void itemStateChanged(ItemEvent e) {
        JCheckBox source = (JCheckBox) e.getItemSelectable();
        String type = source.getText();
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            int index = selectedMagTypes.indexOf(type);
            if (index >= 0) selectedMagTypes.remove(index);
        }
        if (e.getStateChange() == ItemEvent.SELECTED) {
            int index = selectedMagTypes.indexOf(type);
            if (index < 0) selectedMagTypes.add(type);
        }
    }

    /** update the magnet table with type options for the selected sequence */
    protected void updateMagnetPanel() {
        magCheckBoxPanel.removeAll();
        for (JCheckBox box : magCheckBoxes) {
            magCheckBoxPanel.add(box);
        }
        this.add(magCheckBoxPanel);
        this.add(updateTableButton);
        this.add(tableScrollPane);
    }

    /** update the magnet table for the selected types */
    private void makeMagnetTable() {
        magnetTableModel = new MagnetTableModel(this);
        magnetTable = new JTable(magnetTableModel);
        magnetTable.setRowSelectionAllowed(true);
        magnetTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent event) {
                int col = magnetTable.columnAtPoint(event.getPoint());
                int row = magnetTable.getSelectedRow();
                System.out.println("row = " + row + " col = " + col);
                if (col == 1) {
                    Channel chan = B_Sets.get(row).getChannel();
                    theDoc.myWindow().wheelPanel.setChannel(chan);
                }
                if (col == 2) {
                    Channel chan = B_Trim_Sets.get(row).getChannel();
                    theDoc.myWindow().wheelPanel.setChannel(chan);
                }
                if (col == 4) {
                    Channel chan = B_Books.get(row).getChannel();
                    theDoc.myWindow().wheelPanel.setChannel(chan);
                }
            }
        });
    }

    /** update the magnet table based on the selected magnet types */
    protected void updateMagnetTable() {
        magnetNames.clear();
        B_Sets.clear();
        B_RBs.clear();
        B_Trim_Sets.clear();
        java.util.List<AcceleratorNode> nodes = theDoc.getSelectedSequence().getAllNodes();
        for (AcceleratorNode node : nodes) {
            boolean useIt = false;
            for (String type : selectedMagTypes) {
                if (node.isKindOf(type)) {
                    useIt = true;
                    break;
                }
            }
            if (useIt && !node.isKindOf("pmag")) {
                magnetNames.add(node.getId());
                Channel bRB = ((Electromagnet) node).getChannel(Electromagnet.FIELD_RB_HANDLE);
                MagnetMainSupply mms = ((Electromagnet) node).getMainSupply();
                Channel bSet = mms.getChannel(MagnetMainSupply.FIELD_SET_HANDLE);
                PVTableCell pvrb;
                if (bRB != null) pvrb = new PVTableCell(bRB); else pvrb = new PVTableCell();
                B_RBs.add(pvrb);
                PVTableCell pvsp;
                if (bSet != null) pvsp = new PVTableCell(bSet); else pvsp = new PVTableCell();
                B_Sets.add(pvsp);
                PVTableCell pvBook;
                try {
                    Channel bBookSet = mms.getChannel(MagnetMainSupply.FIELD_BOOK_HANDLE);
                    pvBook = new PVTableCell(bBookSet);
                } catch (Exception ex) {
                    pvBook = new PVTableCell();
                }
                B_Books.add(pvBook);
                PVTableCell pvtsp;
                if (node.isKindOf("trimmedquad")) {
                    MagnetTrimSupply mts = ((TrimmedQuadrupole) node).getTrimSupply();
                    Channel btSet = mts.getChannel(MagnetTrimSupply.FIELD_SET_HANDLE);
                    if (btSet != null) pvtsp = new PVTableCell(btSet); else pvtsp = new PVTableCell();
                    B_Trim_Sets.add(pvtsp);
                } else {
                    pvtsp = new PVTableCell();
                    B_Trim_Sets.add(pvtsp);
                }
            }
        }
        magnetTableModel.fireTableDataChanged();
    }
}
