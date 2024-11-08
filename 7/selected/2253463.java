package foucault;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import foucault.schema.*;

/**
 * JPanel that displays the details of the {@link Project},
 * including a list of all {@link Measurement}s.
 */
public class ProjectPane extends JPanel {

    JPanel m_paramsPane = new JPanel();

    JPanel m_projectnamePane = new JPanel();

    JLabel m_projectnameLabel = new JLabel("Project Title:");

    JTextField m_projectnameValue = new JTextField();

    Font m_projectnameFont = new Font("helvetica", Font.BOLD, 18);

    JPanel m_projectfilePane = new JPanel();

    JLabel m_fileLabel = new JLabel("Project file:");

    JLabel m_filenameLabel = new JLabel();

    JLabel m_descLabel = new JLabel("Description:");

    JTextArea m_descValue = new JTextArea();

    JScrollPane m_descPane = new JScrollPane(m_descValue);

    JLabel m_projectparamsLabel = new JLabel("Mirror parameters:");

    JPanel m_projectparamsPane1 = new JPanel();

    JPanel m_projectparamsPane2 = new JPanel();

    JLabel m_diaLabel = new JLabel("Mirror diameter:");

    JTextField m_diaValue = new JTextField(8);

    JLabel m_rocLabel = new JLabel("ROC:");

    JTextField m_rocValue = new JTextField(8);

    JLabel m_obsLabel = new JLabel("Central obstruction diameter:");

    JTextField m_obsValue = new JTextField(8);

    JLabel m_edgeLabel = new JLabel("Edge mask:");

    JTextField m_edgeValue = new JTextField(8);

    JLabel m_mmLabel1 = new JLabel("millimeters");

    JLabel m_mmLabel2 = new JLabel("millimeters");

    JLabel m_measLabel = new JLabel("Measurements:");

    JPanel m_measurementsPane = new JPanel();

    JTable m_measurementsList = new JTable();

    JPanel m_measopsPane = new JPanel();

    JPanel m_measopsButtonsPane = new JPanel();

    JButton m_measopsButtonNew = new JButton("New");

    JButton m_measopsButtonEdit = new JButton("Edit");

    JButton m_measopsButtonCompare = new JButton("Compare");

    JButton m_measopsButtonRemove = new JButton("Remove");

    JButton m_measopsButtonUp = new JButton("Move Up");

    JButton m_measopsButtonDown = new JButton("Move Down");

    Project m_project;

    public ProjectPane(Project project) {
        m_project = project;
        try {
            initUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI() throws Exception {
        m_projectnameLabel.setFont(m_projectnameFont);
        m_projectnameValue.setFont(m_projectnameFont);
        m_projectnameValue.addFocusListener(projectnameValueFocusAdapter);
        m_projectnamePane.setLayout(new BorderLayout(5, 0));
        m_projectnamePane.add(m_projectnameLabel, BorderLayout.WEST);
        m_projectnamePane.add(m_projectnameValue);
        m_descPane.setPreferredSize(new Dimension(0, 40));
        m_descValue.addFocusListener(descValueFocusListener);
        m_projectfilePane.setLayout(new BorderLayout(5, 0));
        m_projectfilePane.add(m_fileLabel, BorderLayout.WEST);
        m_projectfilePane.add(m_filenameLabel);
        m_diaValue.setHorizontalAlignment(JTextField.RIGHT);
        m_rocValue.setHorizontalAlignment(JTextField.RIGHT);
        m_obsValue.setHorizontalAlignment(JTextField.RIGHT);
        m_edgeValue.setHorizontalAlignment(JTextField.RIGHT);
        m_projectparamsPane1.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        m_projectparamsPane2.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        m_projectparamsPane1.add(m_diaLabel);
        m_projectparamsPane1.add(m_diaValue);
        m_projectparamsPane1.add(m_rocLabel);
        m_projectparamsPane1.add(m_rocValue);
        m_projectparamsPane1.add(m_mmLabel1);
        m_projectparamsPane2.add(m_obsLabel);
        m_projectparamsPane2.add(m_obsValue);
        m_projectparamsPane2.add(m_edgeLabel);
        m_projectparamsPane2.add(m_edgeValue);
        m_projectparamsPane2.add(m_mmLabel2);
        m_diaValue.addFocusListener(diaValueFocusListener);
        m_rocValue.addFocusListener(rocValueFocusListener);
        m_obsValue.addFocusListener(obsValueFocusListener);
        m_edgeValue.addFocusListener(edgeValueFocusListener);
        m_paramsPane.setLayout(new GridBagLayout());
        GridBagConstraints fullrow = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 4, 2), 0, 0);
        m_paramsPane.add(m_projectnamePane, fullrow);
        m_paramsPane.add(m_projectfilePane, fullrow);
        m_paramsPane.add(m_descLabel, fullrow);
        m_paramsPane.add(m_descPane, fullrow);
        m_paramsPane.add(m_projectparamsLabel, fullrow);
        m_paramsPane.add(m_projectparamsPane1, fullrow);
        m_paramsPane.add(m_projectparamsPane2, fullrow);
        m_paramsPane.add(m_measLabel, fullrow);
        m_measopsButtonNew.setMnemonic('N');
        m_measopsButtonNew.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                measopsButtonNew_actionPerformed(e);
            }
        });
        m_measopsButtonCompare.setMnemonic('C');
        m_measopsButtonRemove.setMnemonic('R');
        m_measopsButtonRemove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                measopsButtonRemove_actionPerformed(e);
            }
        });
        m_measopsButtonEdit.setMnemonic('I');
        m_measopsButtonEdit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                measopsButtonEdit_actionPerformed(e);
            }
        });
        m_measopsButtonUp.setMnemonic('U');
        m_measopsButtonUp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                measopsButtonUp_actionPerformed(e);
            }
        });
        m_measopsButtonDown.setMnemonic('D');
        m_measopsButtonDown.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                measopsButtonDown_actionPerformed(e);
            }
        });
        m_measopsPane.setLayout(new GridLayout(6, 1));
        m_measopsPane.add(m_measopsButtonNew);
        m_measopsPane.add(m_measopsButtonEdit);
        m_measopsPane.add(m_measopsButtonCompare);
        m_measopsPane.add(m_measopsButtonRemove);
        m_measopsPane.add(m_measopsButtonUp);
        m_measopsPane.add(m_measopsButtonDown);
        m_measopsButtonsPane.setLayout(new BorderLayout());
        m_measopsButtonsPane.add(m_measopsPane, BorderLayout.NORTH);
        m_measurementsPane.setLayout(new BorderLayout());
        m_measurementsPane.add(new JScrollPane(m_measurementsList), BorderLayout.CENTER);
        m_measurementsPane.add(m_measopsButtonsPane, BorderLayout.EAST);
        m_measurementsList.addMouseListener(measurementsListMouseListener);
        m_measurementsList.addKeyListener(measurementsListKeyListener);
        setLayout(new BorderLayout());
        add(m_paramsPane, BorderLayout.NORTH);
        add(m_measurementsPane, BorderLayout.CENTER);
    }

    FocusAdapter projectnameValueFocusAdapter = new FocusAdapter() {

        public void focusLost(FocusEvent e) {
            String oldvalue = m_project.getModel().getName();
            String newvalue = m_projectnameValue.getText();
            if (!newvalue.equals(oldvalue)) {
                m_project.getModel().setName(newvalue);
                m_project.setModified();
            }
        }
    };

    FocusListener descValueFocusListener = new FocusAdapter() {

        public void focusLost(FocusEvent e) {
            String oldvalue = m_project.getModel().getDescription().getContent();
            String newvalue = m_descValue.getText();
            if (!newvalue.equals(oldvalue)) {
                m_project.getModel().getDescription().setContent(newvalue);
                m_project.setModified();
            }
        }
    };

    FocusListener diaValueFocusListener = new FocusAdapter() {

        public void focusLost(FocusEvent e) {
            try {
                String entered = m_diaValue.getText();
                if ("".equals(entered)) return;
                float newvalue = Float.parseFloat(entered);
                float oldvalue = m_project.getModel().getDiameter();
                if (newvalue != oldvalue) {
                    m_project.getModel().setDiameter(newvalue);
                    m_project.setModified();
                }
            } catch (NumberFormatException nfe) {
            } finally {
                m_diaValue.setText(Float.toString(m_project.getModel().getDiameter()));
            }
        }
    };

    FocusListener rocValueFocusListener = new FocusAdapter() {

        public void focusLost(FocusEvent e) {
            try {
                String entered = m_rocValue.getText();
                if ("".equals(entered)) return;
                float newvalue = Float.parseFloat(entered);
                float oldvalue = m_project.getModel().getRroc();
                if (newvalue != oldvalue) {
                    m_project.getModel().setRroc(newvalue);
                    m_project.setModified();
                }
            } catch (NumberFormatException nfe) {
            } finally {
                m_rocValue.setText(Float.toString(m_project.getModel().getRroc()));
            }
        }
    };

    FocusListener obsValueFocusListener = new FocusAdapter() {

        public void focusLost(FocusEvent e) {
            try {
                String entered = m_obsValue.getText();
                if ("".equals(entered)) {
                    if (m_project.getModel().hasObstruction()) {
                        m_project.getModel().deleteObstruction();
                        m_project.setModified();
                    }
                    return;
                }
                float newvalue = Float.parseFloat(entered);
                float oldvalue = m_project.getModel().getObstruction();
                if (newvalue != oldvalue) {
                    m_project.getModel().setObstruction(newvalue);
                    m_project.setModified();
                }
            } catch (NumberFormatException nfe) {
            } finally {
                if (m_project.getModel().hasObstruction()) {
                    m_obsValue.setText(Float.toString(m_project.getModel().getObstruction()));
                } else {
                    m_obsValue.setText(null);
                }
            }
        }
    };

    FocusListener edgeValueFocusListener = new FocusAdapter() {

        public void focusLost(FocusEvent e) {
            try {
                String entered = m_edgeValue.getText();
                if ("".equals(entered)) {
                    if (m_project.getModel().hasEdge()) {
                        m_project.getModel().deleteEdge();
                        m_project.setModified();
                    }
                    return;
                }
                float newvalue = Float.parseFloat(entered);
                float oldvalue = m_project.getModel().getEdge();
                if (newvalue != oldvalue) {
                    m_project.getModel().setEdge(newvalue);
                    m_project.setModified();
                }
            } catch (NumberFormatException nfe) {
            } finally {
                if (m_project.getModel().hasEdge()) {
                    m_edgeValue.setText(Float.toString(m_project.getModel().getEdge()));
                } else {
                    m_edgeValue.setText(null);
                }
            }
        }
    };

    MouseListener measurementsListMouseListener = new MouseAdapter() {

        public void mouseClicked(MouseEvent e) {
            if (2 == e.getClickCount()) {
                ((TopFrame) getRootPane().getParent()).switchtomeaspane(m_measurementsList.getSelectedRow());
            } else {
                refreshbuttons();
            }
        }
    };

    KeyListener measurementsListKeyListener = new KeyAdapter() {

        public void keyPressed(KeyEvent e) {
            refreshbuttons();
        }

        public void keyReleased(KeyEvent e) {
            refreshbuttons();
        }

        public void keyTyped(KeyEvent e) {
            if (KeyEvent.VK_ENTER == e.getKeyChar() && 1 == m_measurementsList.getSelectedRowCount()) {
                ((TopFrame) getRootPane().getParent()).switchtomeaspane(m_measurementsList.getSelectedRow());
            }
        }
    };

    private void refreshbuttons() {
        int rows = m_measurementsList.getSelectedRowCount();
        if (0 == rows) {
            m_measopsButtonEdit.setEnabled(false);
            m_measopsButtonCompare.setEnabled(false);
            m_measopsButtonRemove.setEnabled(false);
            m_measopsButtonUp.setEnabled(false);
            m_measopsButtonDown.setEnabled(false);
        } else if (1 == rows) {
            m_measopsButtonEdit.setEnabled(true);
            m_measopsButtonCompare.setEnabled(false);
            m_measopsButtonRemove.setEnabled(true);
            if (0 != m_measurementsList.getSelectedRow()) m_measopsButtonUp.setEnabled(true); else m_measopsButtonUp.setEnabled(false);
            if (m_measurementsList.getRowCount() != m_measurementsList.getSelectedRow() + 1) m_measopsButtonDown.setEnabled(true); else m_measopsButtonDown.setEnabled(false);
        } else {
            m_measopsButtonEdit.setEnabled(false);
            m_measopsButtonCompare.setEnabled(true);
            m_measopsButtonRemove.setEnabled(true);
            m_measopsButtonUp.setEnabled(false);
            m_measopsButtonDown.setEnabled(false);
        }
    }

    void measopsButtonNew_actionPerformed(ActionEvent e) {
        ((TopFrame) getRootPane().getParent()).switchtomeaspane(-1);
        m_measurementsList.setRowSelectionInterval(0, 0);
        refreshbuttons();
    }

    void measopsButtonEdit_actionPerformed(ActionEvent e) {
        ((TopFrame) getRootPane().getParent()).switchtomeaspane(m_measurementsList.getSelectedRow());
    }

    void measopsButtonRemove_actionPerformed(ActionEvent e) {
        int rows[] = m_measurementsList.getSelectedRows();
        for (int i = 1; i <= rows.length; i++) {
            int index = rows[rows.length - i];
            m_project.getModel().removeMeasurement(index);
            m_project.removeAnalysis(index);
        }
        setSelected(-1);
    }

    void measopsButtonUp_actionPerformed(ActionEvent e) {
        int index = m_measurementsList.getSelectedRow();
        Measurement[] m = m_project.getModel().getMeasurement();
        Measurement temp = m[index];
        m[index] = m[index - 1];
        m[index - 1] = temp;
        m_project.getModel().setMeasurement(m);
        m_project.setModified();
        Analysis a = m_project.removeAnalysis(index);
        m_project.insertAnalysis(a, index - 1);
        setSelected(index - 1);
    }

    void measopsButtonDown_actionPerformed(ActionEvent e) {
        int index = m_measurementsList.getSelectedRow();
        Measurement[] m = m_project.getModel().getMeasurement();
        Measurement temp = m[index];
        m[index] = m[index + 1];
        m[index + 1] = temp;
        m_project.getModel().setMeasurement(m);
        m_project.setModified();
        Analysis a = m_project.removeAnalysis(index);
        m_project.insertAnalysis(a, index + 1);
        setSelected(index + 1);
    }

    void setSelected(int index) {
        if (-1 == index) {
            m_measurementsList.clearSelection();
        } else {
            m_measurementsList.setRowSelectionInterval(index, index);
        }
        m_measurementsList.revalidate();
        refreshbuttons();
    }

    void repopulate() {
        if (null != m_project.getFile()) m_filenameLabel.setText(m_project.getFile().getPath()); else m_filenameLabel.setText(null);
        m_projectnameValue.setText(m_project.getModel().getName());
        foucault.schema.Description d = m_project.getModel().getDescription();
        if (null != d) m_descValue.setText(d.getContent());
        m_diaValue.setText(Float.toString(m_project.getModel().getDiameter()));
        m_rocValue.setText(Float.toString(m_project.getModel().getRroc()));
        if (m_project.getModel().hasObstruction()) {
            m_obsValue.setText(Float.toString(m_project.getModel().getObstruction()));
        } else {
            m_obsValue.setText(null);
        }
        if (m_project.getModel().hasEdge()) {
            m_edgeValue.setText(Float.toString(m_project.getModel().getEdge()));
        } else {
            m_edgeValue.setText(null);
        }
        m_measurementsList.setModel(new MeasTableModel(m_project.getModel()));
        refreshbuttons();
    }
}
