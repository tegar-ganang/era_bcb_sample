package org.gnf.seqtracs.assembly;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import com.borland.jbcl.layout.*;
import org.gnf.seqtracs.utilities.*;
import org.gnf.seqtracs.dbinterface.SeqDbInterface;
import org.gnf.seqtracs.seqretrival.*;
import java.awt.event.*;
import javax.swing.event.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
public class AssemblyProjectFrame extends JFrame {

    public static final String CONTACT = "czmasek@gnf.org";

    public static final String OPTIONS_DEFAULT = "", WORK_DIR_DEFAULT = "", WORK_DIR_DEFAULT_FOR_ACTUAL_ASSEMBLY = "/nfs/dm3/homedir1/czmasek/ASSEMBLY_WORKDIR1", ASSEMB_IN_PROGRESS_STATUS = "ASSEMBLY IN PROGRESS", ASSEMB_FAILED_STATUS = "ASSEMBLY FAILED", ASSEMB_SUCCESSFULL_STATUS = "ASSEMBLY SUCCESSFULL", TRACEFILEPANEL_NAME = "Tracefiles", ARTIFICIALPANEL_NAME = "Artificial", PRIMERSPANEL_NAME = "Primers", VECTORSPANEL_NAME = "Vectors", CONTIGSPANEL_NAME = "Resulting Contigs";

    private AssemblyProject _ap;

    private String _previous_options, _previous_directory;

    private SeqDbInterface _sdbi = null;

    private RawSeqsTableModel _raw_seqs_model;

    private GeneralSeqsTableModel _contigs_model, _art_seqs_model, _primer_seqs_model, _vector_seqs_model;

    private JTable _raw_seqs_table, _contigs_table, _art_seqs_table, _primer_seqs_table, _vector_seqs_table;

    private String _selectedpanel = TRACEFILEPANEL_NAME;

    private int _current_row;

    JCheckBox jCheckBoxWriteContigToDb = new JCheckBox();

    JMenuBar jMenuBar1 = new JMenuBar();

    JMenu jMenu1 = new JMenu();

    JMenuItem jMenuItem1 = new JMenuItem();

    JMenu jMenu2 = new JMenu();

    JMenuItem jMenuItem3 = new JMenuItem();

    JMenuItem jMenuItem4 = new JMenuItem();

    JPanel jPanelnorth = new JPanel();

    JButton assembleButton = new JButton();

    JPanel jPanelAssemblyProject = new JPanel();

    JButton removeButton = new JButton();

    JButton addButton = new JButton();

    JPanel jPanelwest = new JPanel();

    JTextPane ass_cmnt = new JTextPane();

    JTextField jTextField6_LastModified = new JTextField();

    JTextPane user_cmnt = new JTextPane();

    JTextField jTextField5_CreatedOn = new JTextField();

    JTextField jTextField4_WorkDir = new JTextField();

    GridLayout gridLayout2 = new GridLayout();

    JTextField jTextField3_Options = new JTextField();

    GridLayout gridLayout1 = new GridLayout();

    JLabel jLabel_ass_cmnt = new JLabel();

    JTextField jTextField1_Name = new JTextField();

    JLabel jLabel_user_cmnt = new JLabel();

    JLabel LastModLabel = new JLabel();

    JLabel CreatedOnLabel = new JLabel();

    JLabel OptionsLabel = new JLabel();

    JPanel jPanelcenter = new JPanel();

    JLabel WorkDirLabel = new JLabel();

    JLabel NameLabel = new JLabel();

    JLabel jLabelAssembStatus = new JLabel();

    JPanel jPanelsouth = new JPanel();

    BorderLayout borderLayout1 = new BorderLayout();

    VerticalFlowLayout verticalFlowLayout1 = new VerticalFlowLayout();

    JTabbedPane jTabbedPane1 = new JTabbedPane();

    JPanel TracefilesPanel = new JPanel();

    JPanel ArtificialPanel = new JPanel();

    JPanel PrimersPanel = new JPanel();

    JPanel VectorsPanel = new JPanel();

    JPanel ContigsPanel = new JPanel();

    JPanel jPanel6 = new JPanel();

    PaneLayout paneLayout1 = new PaneLayout();

    JButton saveButton = new JButton();

    JScrollPane jScrollPane1 = new JScrollPane();

    JScrollPane jScrollPane2 = new JScrollPane();

    JScrollPane jScrollPane3 = new JScrollPane();

    JScrollPane jScrollPane4 = new JScrollPane();

    JScrollPane jScrollPane5 = new JScrollPane();

    JScrollPane jScrollPane6 = new JScrollPane();

    JButton deleteContigsButton = new JButton();

    BorderLayout borderLayout2 = new BorderLayout();

    BorderLayout borderLayout3 = new BorderLayout();

    BorderLayout borderLayout4 = new BorderLayout();

    BorderLayout borderLayout5 = new BorderLayout();

    BorderLayout borderLayout6 = new BorderLayout();

    BorderLayout borderLayout7 = new BorderLayout();

    JMenuItem menuItem1 = new JMenuItem("Show Sequence");

    JMenuItem menuItem2 = new JMenuItem("Show Traces");

    JMenuItem menuItem3 = new JMenuItem("Show Quality Value Histogram");

    JPopupMenu popupMenu = new JPopupMenu();

    public AssemblyProjectFrame() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        ass_cmnt.setEditable(false);
        jMenuItem1.setText("Close");
        jMenuItem3.setText("About");
        jMenuItem4.setText("Help");
        jMenu2.setText("Help");
        jMenu1.setText("File");
        jPanelnorth.setLayout(gridLayout1);
        assembleButton.setText("Assemble");
        assembleButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                assembleButton_actionPerformed(e);
            }
        });
        assembleButton.setToolTipText("This assembles this project in the given working directory on ~~~~");
        assembleButton.setActionCommand("Assemble");
        jPanelAssemblyProject.setLayout(borderLayout1);
        removeButton.setText("Remove Selected From Project");
        removeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeButton_actionPerformed(e);
            }
        });
        removeButton.setFont(new java.awt.Font("Dialog", 0, 10));
        removeButton.setToolTipText("This allows to remove tracefiles/primers/artificial sequences from " + "this project");
        addButton.setToolTipText("This allows to add tracefiles/primers/artificial sequences to be " + "added to this project");
        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addButton_actionPerformed(e);
            }
        });
        jPanelwest.setLayout(verticalFlowLayout1);
        ass_cmnt.setText("");
        jTextField6_LastModified.setText("");
        jTextField6_LastModified.setEditable(false);
        user_cmnt.setText("");
        jTextField5_CreatedOn.setText("");
        jTextField5_CreatedOn.setEditable(false);
        jTextField4_WorkDir.setText(WORK_DIR_DEFAULT);
        jTextField3_Options.setText(OPTIONS_DEFAULT);
        gridLayout1.setRows(3);
        gridLayout1.setColumns(4);
        jLabel_ass_cmnt.setText("Assembly Comment");
        jTextField1_Name.setText("");
        jTextField1_Name.setToolTipText("");
        jLabel_user_cmnt.setText("User Comment");
        LastModLabel.setText("Last modified");
        CreatedOnLabel.setText("Created on");
        OptionsLabel.setToolTipText("The options for phrad/phrap -- it is recommended to work with the " + "defaults, i.e. leave this blank");
        OptionsLabel.setText("Options");
        WorkDirLabel.setText("Working Directory");
        WorkDirLabel.setToolTipText("The complete path to a directory on CUB where you have read/write " + "access to");
        NameLabel.setToolTipText("");
        NameLabel.setText("Project Name");
        jPanelsouth.setLayout(gridLayout2);
        jPanelcenter.setLayout(paneLayout1);
        saveButton.setToolTipText("This saves this assembly project to the database");
        saveButton.setText("Save Project To DB");
        saveButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveButton_actionPerformed(e);
            }
        });
        deleteContigsButton.setEnabled(false);
        deleteContigsButton.setFont(new java.awt.Font("Dialog", 0, 10));
        deleteContigsButton.setToolTipText("This deletes from the database");
        deleteContigsButton.setText("Delete Selected From DB");
        deleteContigsButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deleteContigsButton_actionPerformed(e);
            }
        });
        TracefilesPanel.setLayout(borderLayout2);
        ArtificialPanel.setLayout(borderLayout3);
        PrimersPanel.setLayout(borderLayout4);
        VectorsPanel.setLayout(borderLayout5);
        ContigsPanel.setLayout(borderLayout6);
        jPanel6.setLayout(borderLayout7);
        jCheckBoxWriteContigToDb.setToolTipText("This allows to write the resulting contigs  to the database");
        jCheckBoxWriteContigToDb.setSelected(true);
        jCheckBoxWriteContigToDb.setText("Write Contig to DB");
        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                jTabbedPane1_stateChanged(e);
            }
        });
        jMenuBar1.add(jMenu1);
        jMenuBar1.add(jMenu2);
        jMenu1.add(jMenuItem1);
        jMenu2.add(jMenuItem3);
        jMenu2.add(jMenuItem4);
        jPanelsouth.add(jLabel_user_cmnt, null);
        jPanelsouth.add(user_cmnt, null);
        jPanelsouth.add(jLabel_ass_cmnt, null);
        jPanelsouth.add(ass_cmnt, null);
        jPanelAssemblyProject.add(jPanelwest, BorderLayout.EAST);
        jPanelAssemblyProject.add(jPanelsouth, BorderLayout.SOUTH);
        jPanelwest.add(assembleButton, null);
        jPanelwest.add(saveButton, null);
        jPanelwest.add(addButton, null);
        jPanelwest.add(removeButton, null);
        jPanelwest.add(deleteContigsButton, null);
        jPanelAssemblyProject.add(jPanelcenter, BorderLayout.CENTER);
        jPanelcenter.add(jTabbedPane1, new PaneConstraints("jTabbedPane1", "jTabbedPane1", PaneConstraints.ROOT, 1.0f));
        jTabbedPane1.add(TracefilesPanel, TRACEFILEPANEL_NAME);
        TracefilesPanel.add(jScrollPane1, BorderLayout.CENTER);
        jTabbedPane1.add(ArtificialPanel, ARTIFICIALPANEL_NAME);
        ArtificialPanel.add(jScrollPane2, BorderLayout.NORTH);
        jTabbedPane1.add(PrimersPanel, PRIMERSPANEL_NAME);
        PrimersPanel.add(jScrollPane3, BorderLayout.NORTH);
        jTabbedPane1.add(VectorsPanel, VECTORSPANEL_NAME);
        VectorsPanel.add(jScrollPane4, BorderLayout.NORTH);
        jTabbedPane1.add(ContigsPanel, CONTIGSPANEL_NAME);
        ContigsPanel.add(jScrollPane5, BorderLayout.NORTH);
        jPanel6.add(jScrollPane6, BorderLayout.NORTH);
        jPanelAssemblyProject.add(jPanelnorth, BorderLayout.NORTH);
        jPanelnorth.add(NameLabel, null);
        jPanelnorth.add(jTextField1_Name, null);
        jPanelnorth.add(CreatedOnLabel, null);
        jPanelnorth.add(jTextField5_CreatedOn, null);
        jPanelnorth.add(OptionsLabel, null);
        jPanelnorth.add(jTextField3_Options, null);
        jPanelnorth.add(LastModLabel, null);
        jPanelnorth.add(jTextField6_LastModified, null);
        jPanelnorth.add(WorkDirLabel, null);
        jPanelnorth.add(jTextField4_WorkDir, null);
        getContentPane().add(jPanelAssemblyProject, BorderLayout.CENTER);
        jPanelwest.add(jCheckBoxWriteContigToDb, null);
        jPanelwest.add(jLabelAssembStatus, null);
        menuItem1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        menuItem2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        menuItem3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        popupMenu.add(menuItem1);
        popupMenu.add(menuItem2);
        popupMenu.add(menuItem3);
        addTables();
        updateDisplay();
    }

    public void setAssemblyProject(AssemblyProject ap) {
        _ap = ap;
        _sdbi = null;
        updateDisplay();
    }

    public AssemblyProject getAssemblyProject() {
        return _ap;
    }

    void setPreviousOptions(String previous_options) {
        _previous_options = previous_options;
    }

    String getPreviousOptions() {
        return _previous_options;
    }

    void setPreviousDirectory(String previous_directory) {
        _previous_directory = previous_directory;
    }

    String getPreviousDirectory() {
        return _previous_directory;
    }

    private void addTables() {
        _raw_seqs_model = new RawSeqsTableModel();
        _raw_seqs_table = new JTable(_raw_seqs_model);
        _contigs_model = new GeneralSeqsTableModel();
        _contigs_table = new JTable(_contigs_model);
        _art_seqs_model = new GeneralSeqsTableModel();
        _art_seqs_table = new JTable(_art_seqs_model);
        _primer_seqs_model = new GeneralSeqsTableModel();
        _primer_seqs_table = new JTable(_primer_seqs_model);
        _vector_seqs_model = new GeneralSeqsTableModel();
        _vector_seqs_table = new JTable(_vector_seqs_model);
        jScrollPane1.getViewport().add(_raw_seqs_table, null);
        jScrollPane2.getViewport().add(_art_seqs_table, null);
        jScrollPane3.getViewport().add(_primer_seqs_table, null);
        jScrollPane4.getViewport().add(_vector_seqs_table, null);
        jScrollPane5.getViewport().add(_contigs_table, null);
    }

    /**
     *
     * This reads in an assembly project's auxilary data (such as raw sequences,
     * primers, ...) from the database and displays all the information
     * in the GUI.
     *
     */
    private void updateDisplay() {
        if (getAssemblyProject() != null) {
            if (getAssemblyProject().getDbConnection() == null) {
                try {
                    getAssemblyProject().createDbConnection();
                } catch (Exception e) {
                    Utilities.unexpectedException(e, this, CONTACT);
                }
            }
            if (_sdbi == null) {
                try {
                    _sdbi = new SeqDbInterface(getAssemblyProject().getDbConnection());
                } catch (Exception e) {
                    Utilities.unexpectedException(e, this, CONTACT);
                }
            }
            if (getAssemblyProject().getLastModificationDate() != null) {
                jTextField6_LastModified.setText(getAssemblyProject().getLastModificationDate().toString());
            }
            if (getAssemblyProject().getInitialDate() != null) {
                jTextField5_CreatedOn.setText(getAssemblyProject().getInitialDate().toString());
            }
            if (getAssemblyProject().getWorkDir() != null) {
                jTextField4_WorkDir.setText(getAssemblyProject().getWorkDir().toString());
            } else {
                jTextField4_WorkDir.setText(WORK_DIR_DEFAULT);
            }
            if (getAssemblyProject().getOptions() != null) {
                jTextField3_Options.setText(getAssemblyProject().getOptions());
            } else {
                jTextField4_WorkDir.setText(OPTIONS_DEFAULT);
            }
            setPreviousDirectory(jTextField4_WorkDir.getText().trim());
            setPreviousOptions(jTextField3_Options.getText().trim());
            jTextField3_Options.setText(getAssemblyProject().getOptions());
            jTextField1_Name.setText(getAssemblyProject().getName());
            ass_cmnt.setText(getAssemblyProject().getTechnicalComment());
            user_cmnt.setText(getAssemblyProject().getUserComment());
            RawSequence[] raw_seqs = null;
            GeneralSequence[] contigs = null, art_seqs = null, primer_seqs = null, vector_seqs = null;
            try {
                raw_seqs = _sdbi.retrieveBioSequencesAsRawSequences(getAssemblyProject().getRawSeqOids());
                contigs = _sdbi.retrieveBioSequencesAsGeneralSequences(getAssemblyProject().getContigOids());
                art_seqs = _sdbi.retrieveBioSequencesAsGeneralSequences(getAssemblyProject().getAdditionalSeqsOids());
                primer_seqs = _sdbi.retrieveBioSequencesAsGeneralSequences(getAssemblyProject().getPrimerOids());
                vector_seqs = _sdbi.retrieveBioSequencesAsGeneralSequences(getAssemblyProject().getVectorOids());
            } catch (Exception e) {
                Utilities.unexpectedException(e, this, CONTACT);
            }
            loadTableData(raw_seqs, _raw_seqs_model, _raw_seqs_table);
            loadTableData(contigs, _contigs_model, _contigs_table);
            loadTableData(art_seqs, _art_seqs_model, _art_seqs_table);
            loadTableData(primer_seqs, _primer_seqs_model, _primer_seqs_table);
            loadTableData(vector_seqs, _vector_seqs_model, _vector_seqs_table);
            addSimpleTableSortDecorator(_raw_seqs_model, _raw_seqs_table);
            addSimpleTableSortDecorator(_contigs_model, _contigs_table);
            addSimpleTableSortDecorator(_art_seqs_model, _art_seqs_table);
            addSimpleTableSortDecorator(_primer_seqs_model, _primer_seqs_table);
            addSimpleTableSortDecorator(_vector_seqs_model, _vector_seqs_table);
            validate();
            repaint();
            _raw_seqs_table.updateUI();
            _contigs_table.updateUI();
            _art_seqs_table.updateUI();
            _primer_seqs_table.updateUI();
            _vector_seqs_table.updateUI();
        }
        validate();
        repaint();
    }

    private void addSimpleTableSortDecorator(final SimpleTableModel model, final JTable table) {
        final SimpleTableSortDecorator sort_decorator = new SimpleTableSortDecorator(model);
        table.setModel(sort_decorator);
        JTableHeader hdr = (JTableHeader) table.getTableHeader();
        hdr.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                TableColumnModel tcm = table.getColumnModel();
                int vc = tcm.getColumnIndexAtX(e.getX());
                int mc = table.convertColumnIndexToModel(vc);
                if (mc != table.getColumn(model.NO_COLUMN_NAME).getModelIndex()) {
                    sort_decorator.sort(mc);
                }
            }
        });
    }

    /**
     *
     * This stores the editable data from the GUI into
     * the assembly project and writes the assembly project
     * to the database.
     *
     */
    private void updateAssemblyProject() {
        if (getAssemblyProject() != null) {
            if (getAssemblyProject().getLastModificationDate() != null) {
                jTextField6_LastModified.setText(getAssemblyProject().getLastModificationDate().toString());
            }
            if (jTextField4_WorkDir.getText().trim().length() > 0) {
                getAssemblyProject().setWorkDir(new File(jTextField4_WorkDir.getText().trim()));
            }
            getAssemblyProject().setOptions(jTextField3_Options.getText().trim());
            getAssemblyProject().setUserComment(user_cmnt.getText().trim());
            try {
                _sdbi.submitAssemblyProject(getAssemblyProject());
            } catch (Exception ex) {
                ex.printStackTrace();
                Utilities.unexpectedException(ex, this, CONTACT);
            }
        }
    }

    private void loadTableData(final RawSequence[] seqs, final RawSeqsTableModel model, final JTable table) {
        for (int row = 0; row < seqs.length; ++row) {
            loadTableRowData(seqs, model, table, row);
        }
        model.setIsColumnEditable(table.getColumn(model.CHECK_COLUMN_NAME).getModelIndex(), true);
        table.getColumn(model.CHECK_COLUMN_NAME).setMaxWidth(model.CHECK_COLUMN_WIDTH);
        table.getColumn(model.CHECK_COLUMN_NAME).setPreferredWidth(model.CHECK_COLUMN_WIDTH);
        table.getColumn(model.NO_COLUMN_NAME).setMaxWidth(model.NO_COLUMN_WIDTH);
        table.getColumn(model.NO_COLUMN_NAME).setPreferredWidth(model.NO_COLUMN_WIDTH);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBackground(new Color(230, 230, 230));
        table.getColumn(model.NO_COLUMN_NAME).setCellRenderer(r);
    }

    private void loadTableRowData(final RawSequence[] seqs, final RawSeqsTableModel model, final JTable table, final int row) {
        model.setValueAt(new Integer(row), row, table.getColumn(model.NO_COLUMN_NAME).getModelIndex());
        model.setValueAt(seqs[row].getTemplate(), row, table.getColumn(model.TEMPLATE_COLUMN_NAME).getModelIndex());
        model.setValueAt(seqs[row].getPrimer(), row, table.getColumn(model.PRIMER_COLUMN_NAME).getModelIndex());
        model.setValueAt(seqs[row].getTraceFileName(), row, table.getColumn(model.TRACE_COLUMN_NAME).getModelIndex());
        model.setValueAt(new Integer(seqs[row].getLength()), row, table.getColumn(model.LENGTH_COLUMN_NAME).getModelIndex());
        model.setValueAt(new Integer(seqs[row].getHighQualityLength()), row, table.getColumn(model.HQ_LENGTH_COLUMN_NAME).getModelIndex());
        model.setValueAt(seqs[row].getDate(), row, table.getColumn(model.DATE_COLUMN_NAME).getModelIndex());
        model.setValueAt(seqs[row].getName(), row, table.getColumn(model.NAME_COLUMN_NAME).getModelIndex());
        model.setValueAt(seqs[row].getProjectName(), row, table.getColumn(model.PROJECT_NAME_COLUMN_NAME).getModelIndex());
        model.setValueAt(Boolean.FALSE, row, table.getColumn(model.CHECK_COLUMN_NAME).getModelIndex());
        model.setValueAt(new Integer(seqs[row].getOid()), row, table.getColumn(model.OID_COLUMN_NAME).getModelIndex());
    }

    private void loadTableData(final GeneralSequence[] seqs, final GeneralSeqsTableModel model, final JTable table) {
        for (int row = 0; row < seqs.length; ++row) {
            loadTableRowData(seqs, model, table, row);
        }
        model.setIsColumnEditable(table.getColumn(model.CHECK_COLUMN_NAME).getModelIndex(), true);
        table.getColumn(model.CHECK_COLUMN_NAME).setMaxWidth(model.CHECK_COLUMN_WIDTH);
        table.getColumn(model.NO_COLUMN_NAME).setMaxWidth(model.NO_COLUMN_WIDTH);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBackground(new Color(230, 230, 230));
        table.getColumn(model.NO_COLUMN_NAME).setCellRenderer(r);
    }

    private void loadTableRowData(final GeneralSequence[] seqs, final GeneralSeqsTableModel model, final JTable table, final int row) {
        model.setValueAt(new Integer(row), row, table.getColumn(model.NO_COLUMN_NAME).getModelIndex());
        if (seqs[row].getLength() > 0) {
            model.setValueAt(new Integer(seqs[row].getLength()), row, table.getColumn(model.LENGTH_COLUMN_NAME).getModelIndex());
        }
        model.setValueAt(seqs[row].getDate(), row, table.getColumn(model.DATE_COLUMN_NAME).getModelIndex());
        model.setValueAt(seqs[row].getName(), row, table.getColumn(model.NAME_COLUMN_NAME).getModelIndex());
        model.setValueAt(Boolean.FALSE, row, table.getColumn(model.CHECK_COLUMN_NAME).getModelIndex());
        model.setValueAt(new Integer(seqs[row].getOid()), row, table.getColumn(model.OID_COLUMN_NAME).getModelIndex());
    }

    /**
     * This writes the assembly project to the database.
     *
     *
     */
    private void saveProject() {
        if (getAssemblyProject() != null) {
            updateAssemblyProject();
            updateDisplay();
        }
    }

    /**
     * This assembles a project on a server.
     * All the relevant data is extracted from
     * the assembly project on which this
     * frame is based.
     *
     *
     */
    private void assembleProject() {
        assemblyStatusChanged(Color.BLACK, "");
        if (getAssemblyProject() != null) {
            if (!getPreviousDirectory().equals(jTextField4_WorkDir.getText().trim())) {
                System.out.println("changed dir. OK?");
            }
            if (!getPreviousOptions().equals(jTextField3_Options.getText().trim())) {
                System.out.println("changed options. OK?");
            }
            if (jTextField4_WorkDir.getText().trim().equals(WORK_DIR_DEFAULT)) {
                getAssemblyProject().setWorkDir(new File(WORK_DIR_DEFAULT_FOR_ACTUAL_ASSEMBLY));
            }
            getAssemblyProject().setAssemblySuccessful(false);
            getAssemblyProject().setWriteResultingConsensusToDb(true);
            assemblyStatusChanged(Color.WHITE, ASSEMB_IN_PROGRESS_STATUS);
            updateAssemblyProject();
            AssemblyClient ac = new AssemblyClient();
            System.out.println("Sending AssemblyProject");
            AssemblyProject ap_returned = null;
            try {
                ap_returned = ac.assembleOnServer(getAssemblyProject());
            } catch (Exception ex) {
                ex.printStackTrace();
                Utilities.unexpectedException(ex, this, CONTACT);
                assemblyStatusChanged(Color.RED, ASSEMB_FAILED_STATUS);
            }
            try {
                getAssemblyProject().removeDbConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
                Utilities.unexpectedException(ex, this, CONTACT);
            }
            setAssemblyProject(ap_returned);
            if (getAssemblyProject().isAssemblySuccessful()) {
                assemblyStatusChanged(Color.BLUE, ASSEMB_SUCCESSFULL_STATUS);
            } else {
                assemblyStatusChanged(Color.RED, ASSEMB_FAILED_STATUS);
            }
            updateDisplay();
            System.out.println("Received AssemblyProject");
            System.out.println("technical comment:\n" + getAssemblyProject().getTechnicalComment());
        }
    }

    /**
     * This calls method saveProject() and
     * therefore saves a assembly project ot the database
     *
     *
     * @param e
     */
    void saveButton_actionPerformed(ActionEvent e) {
        saveProject();
    }

    void addButton_actionPerformed(ActionEvent e) {
    }

    /**
     *
     * This deletes sequences from one table (determined
     * by which panel is active). It does not affect the
     * data in the database.
     *
     *
     * @param e
     */
    void removeButton_actionPerformed(ActionEvent e) {
        if (_selectedpanel.equals(TRACEFILEPANEL_NAME)) {
            ArrayList oids = getOidsInSelectedRows(_raw_seqs_model, _raw_seqs_table);
            for (int i = 0; i < oids.size(); ++i) {
                getAssemblyProject().removeRawSeqOid(((Integer) (oids.get(i))).intValue());
            }
            deleteRows(_raw_seqs_model, getIndexesOfSelectedRows(_raw_seqs_model, _raw_seqs_table));
            _raw_seqs_table.updateUI();
        } else if (_selectedpanel.equals(ARTIFICIALPANEL_NAME)) {
            ArrayList oids = getOidsInSelectedRows(_art_seqs_model, _art_seqs_table);
            for (int i = 0; i < oids.size(); ++i) {
                getAssemblyProject().removeAdditionalSeqsOid(((Integer) (oids.get(i))).intValue());
            }
            deleteRows(_art_seqs_model, getIndexesOfSelectedRows(_art_seqs_model, _art_seqs_table));
            _art_seqs_table.updateUI();
        } else if (_selectedpanel.equals(PRIMERSPANEL_NAME)) {
            ArrayList oids = getOidsInSelectedRows(_primer_seqs_model, _primer_seqs_table);
            for (int i = 0; i < oids.size(); ++i) {
                getAssemblyProject().removePrimerOid(((Integer) (oids.get(i))).intValue());
            }
            deleteRows(_primer_seqs_model, getIndexesOfSelectedRows(_primer_seqs_model, _primer_seqs_table));
            _primer_seqs_table.updateUI();
        } else if (_selectedpanel.equals(VECTORSPANEL_NAME)) {
            ArrayList oids = getOidsInSelectedRows(_vector_seqs_model, _vector_seqs_table);
            for (int i = 0; i < oids.size(); ++i) {
                getAssemblyProject().removeVectorOid(((Integer) (oids.get(i))).intValue());
            }
            deleteRows(_vector_seqs_model, getIndexesOfSelectedRows(_vector_seqs_model, _vector_seqs_table));
            _vector_seqs_table.updateUI();
        } else if (_selectedpanel.equals(CONTIGSPANEL_NAME)) {
            ArrayList oids = getOidsInSelectedRows(_contigs_model, _contigs_table);
            for (int i = 0; i < oids.size(); ++i) {
                getAssemblyProject().removeContigOid(((Integer) (oids.get(i))).intValue());
            }
            deleteRows(_contigs_model, getIndexesOfSelectedRows(_contigs_model, _contigs_table));
            _contigs_table.updateUI();
        }
    }

    void deleteContigsButton_actionPerformed(ActionEvent e) {
    }

    /**
      *
      * This deletes rows from a table model
      *
      *
      * @param model the model to delete from
      * @param row_indexes the table indexes of the rows to delete
      */
    private void deleteRows(final SimpleTableModel model, final ArrayList row_indexes) {
        model.deleteRows(row_indexes);
    }

    /**
     * This return the table indexes of the checked (=selected) rows
     * in a table (as ArrayList of Integers).
     *
     *
     * @param model the model of the table
     * @param table the table itself
     * @return ArrayList of Integers
     */
    private ArrayList getIndexesOfSelectedRows(final SimpleTableModel model, final JTable table) {
        int check_col = table.getColumn(model.CHECK_COLUMN_NAME).getModelIndex();
        ArrayList row_indexes = new ArrayList();
        for (int row = 0; row < model.getRowCount(); ++row) {
            if (((Boolean) model.getValueAt(row, check_col)).booleanValue()) {
                row_indexes.add(new Integer(row));
            }
        }
        return row_indexes;
    }

    /**
     * This return the oids of the checked (=selected) rows
     * in a table (as ArrayList of Integers).
     *
     *
     * @param model the model of the table
     * @param table the table itself
     * @return ArrayList of Integers
     */
    private ArrayList getOidsInSelectedRows(final SimpleTableModel model, final JTable table) {
        int check_col = table.getColumn(model.CHECK_COLUMN_NAME).getModelIndex(), oid_col = table.getColumn(model.OID_COLUMN_NAME).getModelIndex();
        ArrayList oids = new ArrayList();
        for (int row = 0; row < model.getRowCount(); ++row) {
            if (((Boolean) model.getValueAt(row, check_col)).booleanValue()) {
                oids.add((Integer) model.getValueAt(row, oid_col));
            }
        }
        return oids;
    }

    /**
     * This returns the names of the selected rows in a table and
     * its underlying model
     *
     * @param model
     * @param table
     * @return an ArrayLisy of names
     */
    private ArrayList getNamesOfSelectedRows(final SimpleTableModel model, final JTable table) {
        int check_col = table.getColumn(model.CHECK_COLUMN_NAME).getModelIndex(), name_col = table.getColumn(model.NAME_COLUMN_NAME).getModelIndex();
        ArrayList names = new ArrayList();
        for (int row = 0; row < model.getRowCount(); ++row) {
            if (((Boolean) model.getValueAt(row, check_col)).booleanValue()) {
                System.out.println("name=" + model.getValueAt(row, name_col));
                names.add(model.getValueAt(row, name_col) + "");
            }
        }
        return names;
    }

    /**
     * This is called when the assembleButton is pressed.
     * It calls method assembleProject().
     *
     *
     * @param e
     */
    void assembleButton_actionPerformed(ActionEvent e) {
        assembleProject();
    }

    /**
     * This writes the status of the assembly to the GUI.
     *
     *
     * @param foreground the color to use
     * @param text the text to write
     */
    private void assemblyStatusChanged(final Color foreground, final String text) {
        jLabelAssembStatus.setText(text);
        jLabelAssembStatus.setForeground(foreground);
        validate();
        repaint();
        update(getGraphics());
        getToolkit().sync();
    }

    /**
     *
     * This is called when a differed tabbed panel becomes
     * active. It sets variable  _selectedpaned to the index
     * of the selected pane
     * and
     * it only enables if the pane showing the
     * contigs is active.
     *
     *
     * @param e
     */
    void jTabbedPane1_stateChanged(ChangeEvent e) {
        JTabbedPane tp = (JTabbedPane) e.getSource();
        _selectedpanel = tp.getTitleAt(tp.getSelectedIndex());
        if (_selectedpanel.equals(CONTIGSPANEL_NAME)) {
            deleteContigsButton.setEnabled(true);
        } else {
            deleteContigsButton.setEnabled(false);
        }
    }
}
