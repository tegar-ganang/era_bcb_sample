package beastcalc;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AbstractDocument;

public class GraphFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private int numTableRows;

    private Graph graph;

    private JPanel graphPanel, functionPanel, tempPanel;

    private JTable table;

    private String text;

    private int numFunctions, xPos, yPos, maxFunctions, currentOpenTab, tableIndex;

    private double tableStart, tableStep;

    private DropDownTextField[] functionTextFields, graphSettingsFields;

    private DropDownTextField tableStartField, tableStepField, textField;

    private JTabbedPane tabbedPane;

    private JPopupMenu popupMenu;

    private boolean graphDrag, graphZoom;

    private JDialog dialog;

    private CustomCellRenderer customCellRenderer;

    public GraphFrame(Graph aGraph) {
        super();
        constructDialog();
        graph = aGraph;
        numTableRows = aGraph.getAppletSize() / 20;
        graphDrag = true;
        graphZoom = true;
        tableIndex = 0;
        tableStart = 0;
        tableStep = 1;
        maxFunctions = 20;
        this.setJMenuBar(createMenuBar());
        functionTextFields = new DropDownTextField[maxFunctions];
        numFunctions = 0;
        createJPopupMenu();
        tabbedPane = new JTabbedPane();
        createGraphPanel();
        tabbedPane.add("Graph", graphPanel);
        functionPanel = new JPanel();
        functionPanel.setLayout(new BoxLayout(functionPanel, BoxLayout.Y_AXIS));
        tempPanel = new JPanel(new FlowLayout());
        JButton tempButton = new JButton("Add Function");
        tempButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (numFunctions < maxFunctions) {
                    addFunction();
                    if (numFunctions == maxFunctions - 1) {
                        ((JButton) e.getSource()).setEnabled(false);
                    }
                }
            }
        });
        tempPanel.add(tempButton);
        functionPanel.add(tempPanel);
        tempPanel = new JPanel(new FlowLayout());
        tempPanel.add(new JLabel("(double-click text fields to show pop-up menu)"));
        functionPanel.add(tempPanel);
        JScrollPane scrollPane = new JScrollPane(functionPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {
                for (int x = 0; x < numFunctions; x++) {
                    if (functionTextFields[x].windowIsVisible()) functionTextFields[x].setWindowVisible(false);
                }
            }
        });
        tabbedPane.add("Functions", scrollPane);
        for (int i = 0; i < graph.getNumberOfFunctions(); i++) {
            addFunctionToPanel(graph.getFunction(i), graph.getFunctionEnabled(i), graph.getColor(i));
        }
        customCellRenderer = new CustomCellRenderer();
        table = new JTable(new FunctionTable(graph, tableStart, tableStep, numTableRows));
        table.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (tabbedPane.getSelectedIndex() == 2) {
                    if (table.getSelectedRow() == numTableRows - 1 && e.getKeyCode() == KeyEvent.VK_DOWN) {
                        int[] columnWidths = new int[table.getColumnCount()];
                        for (int i = 0; i < columnWidths.length; i++) {
                            columnWidths[i] = table.getColumnModel().getColumn(i).getWidth();
                        }
                        tableIndex++;
                        table.setModel(new FunctionTable(graph, tableStart + tableIndex * tableStep, tableStep, numTableRows));
                        table.setRowSelectionInterval(numTableRows - 1, numTableRows - 1);
                        for (int i = 0; i < columnWidths.length; i++) {
                            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
                            table.getColumnModel().getColumn(i).setCellRenderer(customCellRenderer);
                        }
                    } else if (table.getSelectedRow() == 0 && e.getKeyCode() == KeyEvent.VK_UP) {
                        int[] columnWidths = new int[table.getColumnCount()];
                        for (int i = 0; i < columnWidths.length; i++) {
                            columnWidths[i] = table.getColumnModel().getColumn(i).getWidth();
                        }
                        tableIndex--;
                        table.setModel(new FunctionTable(graph, tableStart + tableIndex * tableStep, tableStep, numTableRows));
                        for (int i = 0; i < columnWidths.length; i++) {
                            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
                            table.getColumnModel().getColumn(i).setCellRenderer(customCellRenderer);
                        }
                    }
                }
            }
        });
        table.addMouseWheelListener(new MouseWheelListener() {

            public void mouseWheelMoved(MouseWheelEvent e) {
                int[] columnWidths = new int[table.getColumnCount()];
                for (int i = 0; i < columnWidths.length; i++) {
                    columnWidths[i] = table.getColumnModel().getColumn(i).getWidth();
                }
                int notches = e.getWheelRotation();
                tableIndex += notches;
                table.setModel(new FunctionTable(graph, tableStart + tableIndex * tableStep, tableStep, numTableRows));
                if (notches > 0) table.setRowSelectionInterval(numTableRows - 1, numTableRows - 1); else if (notches < 0) table.setRowSelectionInterval(0, 0);
                for (int i = 0; i < columnWidths.length; i++) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
                    table.getColumnModel().getColumn(i).setCellRenderer(customCellRenderer);
                }
            }
        });
        table.setRowHeight(18);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        scrollPane = new JScrollPane(table);
        tabbedPane.add("Table", scrollPane);
        Mouse m = new Mouse(-1);
        tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.PAGE_AXIS));
        JPanel tempFlow = new JPanel(new FlowLayout());
        tempFlow.add(new JLabel("Table Start: "));
        tableStartField = new DropDownTextField(10, -1, -1, 144, 60, false, true);
        JButton button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        tableStartField.addLowerComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        tableStartField.addLowerComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        tableStartField.addLowerComponent(button);
        tableStartField.buildWindow();
        ((AbstractDocument) tableStartField.getDocument()).setDocumentFilter(new UnderscoreFilter() {
        });
        tableStartField.setText(Main.calculator.formatOutput(tableStart));
        tempFlow.add(tableStartField);
        tempPanel.add(tempFlow);
        m = new Mouse(-2);
        tempFlow = new JPanel(new FlowLayout());
        tempFlow.add(new JLabel("Table Step: "));
        tableStepField = new DropDownTextField(10, -1, -1, 144, 60, false, true);
        button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        tableStepField.addLowerComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        tableStepField.addLowerComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        tableStepField.addLowerComponent(button);
        tableStepField.buildWindow();
        ((AbstractDocument) tableStepField.getDocument()).setDocumentFilter(new UnderscoreFilter() {
        });
        tableStepField.setText(Main.calculator.formatOutput(tableStep));
        tempFlow.add(tableStepField);
        tempPanel.add(tempFlow);
        tempFlow = new JPanel(new FlowLayout());
        tempFlow.add(new JLabel("(double-click text fields to show pop-up menu)"));
        tempPanel.add(tempFlow);
        tempPanel.add(Box.createVerticalStrut(graph.getAppletSize() + 60));
        tabbedPane.add("Table Settings", tempPanel);
        graphSettingsFields = new DropDownTextField[6];
        tabbedPane.add("Graph Settings", createGraphSettingsPanel());
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (currentOpenTab != 2) {
                    for (int x = 0; x < numFunctions; x++) {
                        if (functionTextFields[x].windowIsVisible()) functionTextFields[x].setWindowVisible(false);
                    }
                }
                if (currentOpenTab != 4) {
                    if (tableStartField.windowIsVisible()) tableStartField.setWindowVisible(false);
                    if (tableStepField.windowIsVisible()) tableStepField.setWindowVisible(false);
                }
                if (currentOpenTab != 5) {
                    for (int x = 0; x < 6; x++) {
                        if (graphSettingsFields[x].windowIsVisible()) graphSettingsFields[x].setWindowVisible(false);
                    }
                }
                if (currentOpenTab == 0) {
                    synchGraphSettingsFields();
                } else if (currentOpenTab == 1) {
                    takeFunctionInput();
                    table.setModel(new FunctionTable(graph, tableStart + tableIndex * tableStep, tableStep, numTableRows));
                    for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
                        table.getColumnModel().getColumn(i).setCellRenderer(customCellRenderer);
                    }
                } else if (currentOpenTab == 2) {
                    tableStartField.setText(Main.calculator.formatOutput(tableStart + tableIndex * tableStep));
                    tableStepField.setText(Main.calculator.formatOutput(tableStep));
                    tableIndex = 0;
                } else if (currentOpenTab == 3) {
                    try {
                        tableStart = Main.calculator.solve(Main.calculator.replaceVariables(tableStartField.getText()));
                        double newStep = Main.calculator.solve(Main.calculator.replaceVariables(tableStepField.getText()));
                        if (newStep > 0) tableStep = newStep; else throw new Exception();
                        tableIndex = 0;
                        table.setModel(new FunctionTable(graph, tableStart + tableIndex * tableStep, tableStep, numTableRows));
                        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
                            table.getColumnModel().getColumn(i).setCellRenderer(customCellRenderer);
                        }
                    } catch (Exception ex) {
                        tableStartField.setText(Main.calculator.formatOutput(tableStart));
                        tableStepField.setText(Main.calculator.formatOutput(tableStep));
                        tableIndex = 0;
                        tabbedPane.setSelectedIndex(3);
                        JOptionPane.showMessageDialog(null, "Invalid table settings.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else if (currentOpenTab == 4) {
                    try {
                        double[] n = new double[6];
                        for (int i = 0; i < n.length; i++) {
                            n[i] = Main.calculator.solve(Main.calculator.replaceVariables(graphSettingsFields[i].getText()));
                        }
                        graph.resizeGraph(n[0], n[1], n[2], n[3], n[4], n[5]);
                    } catch (Exception ex) {
                        synchGraphSettingsFields();
                        tabbedPane.setSelectedIndex(4);
                        JOptionPane.showMessageDialog(null, "Invalid graph settings.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                currentOpenTab = ((JTabbedPane) e.getSource()).getSelectedIndex();
            }
        });
        synchGraphSettingsFields();
        currentOpenTab = 0;
        if (numFunctions == 0) {
            addFunction();
        }
        this.add(tabbedPane);
        this.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent arg0) {
                dialog.setVisible(false);
                textField.setWindowVisible(false);
            }

            public void focusLost(FocusEvent arg0) {
                dialog.setVisible(false);
            }
        });
        this.addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent arg0) {
                textField.setVisible(false);
            }

            public void componentMoved(ComponentEvent arg0) {
                for (int x = 0; x < numFunctions; x++) {
                    if (functionTextFields[x].windowIsVisible()) functionTextFields[x].moved();
                }
                for (int x = 0; x < 6; x++) {
                    if (graphSettingsFields[x].windowIsVisible()) graphSettingsFields[x].moved();
                }
                if (tableStartField.windowIsVisible()) tableStartField.moved();
                if (tableStepField.windowIsVisible()) tableStepField.moved();
                dialog.setVisible(false);
            }

            public void componentResized(ComponentEvent arg0) {
                for (int x = 0; x < numFunctions; x++) {
                    if (functionTextFields[x].windowIsVisible()) functionTextFields[x].moved();
                }
                for (int x = 0; x < 6; x++) {
                    if (graphSettingsFields[x].windowIsVisible()) graphSettingsFields[x].moved();
                }
                if (tableStartField.windowIsVisible()) tableStartField.moved();
                if (tableStepField.windowIsVisible()) tableStepField.moved();
                dialog.setVisible(false);
            }

            public void componentShown(ComponentEvent arg0) {
            }
        });
        this.addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent arg0) {
                dialog.setVisible(false);
            }

            public void windowClosed(WindowEvent arg0) {
                for (int x = 0; x < numFunctions; x++) {
                    if (functionTextFields[x].windowIsVisible()) functionTextFields[x].setWindowVisible(false);
                }
                for (int x = 0; x < 6; x++) {
                    if (graphSettingsFields[x].windowIsVisible()) graphSettingsFields[x].setWindowVisible(false);
                }
                tableStartField.setWindowVisible(false);
                tableStepField.setWindowVisible(false);
                dialog.setVisible(false);
            }

            public void windowClosing(WindowEvent arg0) {
            }

            public void windowDeactivated(WindowEvent arg0) {
                for (int x = 0; x < numFunctions; x++) {
                    if (functionTextFields[x].windowIsVisible()) functionTextFields[x].setWindowVisible(false);
                }
                for (int x = 0; x < 6; x++) {
                    if (graphSettingsFields[x].windowIsVisible()) graphSettingsFields[x].setWindowVisible(false);
                }
                tableStartField.setWindowVisible(false);
                tableStepField.setWindowVisible(false);
            }

            public void windowDeiconified(WindowEvent arg0) {
            }

            public void windowIconified(WindowEvent arg0) {
                for (int x = 0; x < numFunctions; x++) {
                    if (functionTextFields[x].windowIsVisible()) functionTextFields[x].setWindowVisible(false);
                }
                for (int x = 0; x < 6; x++) {
                    if (graphSettingsFields[x].windowIsVisible()) graphSettingsFields[x].setWindowVisible(false);
                }
                tableStartField.setWindowVisible(false);
                tableStepField.setWindowVisible(false);
                textField.setWindowVisible(false);
                dialog.setVisible(false);
            }

            public void windowOpened(WindowEvent arg0) {
            }
        });
    }

    public void radiansChanged() {
        graph.reevaluateFunctions();
        graphPanel.repaint();
        table.setModel(new FunctionTable(graph, tableStart + tableIndex * tableStep, tableStep, numTableRows));
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(customCellRenderer);
        }
    }

    public void changeSize(int size) {
        int i = Math.min(Main.screenHeight - 120, Main.screenWidth);
        if (size >= 440 && size <= i && size + 11 != this.getWidth()) {
            setSize(size + 11, size + 83);
            graph.resizeWindow(size);
            tempPanel = (JPanel) tabbedPane.getComponent(3);
            tempPanel.remove(tempPanel.getComponent(2));
            tempPanel.add(Box.createVerticalStrut(graph.getAppletSize() + 60));
            numTableRows = graph.getAppletSize() / 19;
            table.setModel(new FunctionTable(graph, tableStart + tableIndex * tableStep, tableStep, numTableRows));
            for (int j = 0; j < table.getColumnModel().getColumnCount(); j++) {
                table.getColumnModel().getColumn(j).setCellRenderer(customCellRenderer);
            }
        }
    }

    private JPanel createGraphSettingsPanel() {
        JPanel tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.Y_AXIS));
        Container tempFlow = new JPanel(new FlowLayout());
        JButton button = new JButton("Rectangular");
        button.setSelected(true);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    graph.resizeGraph(-10, 10, 1, -10, 10, 1);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Invalid Input", "Error", JOptionPane.ERROR_MESSAGE);
                }
                synchGraphSettingsFields();
            }
        });
        tempFlow.add(button);
        button = new JButton("Trig");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    if (Main.calculator.isInRadians()) graph.resizeGraph(-2 * Math.PI, 2 * Math.PI, Math.PI / 2, -4, 4, 1); else graph.resizeGraph(-360, 360, 90, -4, 4, 1);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Fatal System Error", "Error", JOptionPane.ERROR_MESSAGE);
                }
                synchGraphSettingsFields();
            }
        });
        tempFlow.add(button);
        tempPanel.add(tempFlow);
        tempFlow = new JPanel(new FlowLayout());
        tempFlow.add(new JLabel("x min: "));
        Mouse m = new Mouse(-3);
        graphSettingsFields[0] = new DropDownTextField(13, -1, -1, -1, -1, false, true);
        button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        graphSettingsFields[0].addLowerComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        graphSettingsFields[0].addLowerComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        graphSettingsFields[0].addLowerComponent(button);
        graphSettingsFields[0].buildWindow();
        tempFlow.add(graphSettingsFields[0]);
        tempFlow.add(new JLabel(" y min: "));
        m = new Mouse(-6);
        graphSettingsFields[3] = new DropDownTextField(13, -1, -1, -1, -1, false, true);
        button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        graphSettingsFields[3].addLowerComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        graphSettingsFields[3].addLowerComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        graphSettingsFields[3].addLowerComponent(button);
        graphSettingsFields[3].buildWindow();
        tempFlow.add(graphSettingsFields[3]);
        tempPanel.add(tempFlow);
        tempFlow = new JPanel(new FlowLayout());
        tempFlow.add(new JLabel("x max: "));
        m = new Mouse(-4);
        graphSettingsFields[1] = new DropDownTextField(13, -1, -1, -1, -1, false, true);
        button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        graphSettingsFields[1].addLowerComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        graphSettingsFields[1].addLowerComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        graphSettingsFields[1].addLowerComponent(button);
        graphSettingsFields[1].buildWindow();
        tempFlow.add(graphSettingsFields[1]);
        tempFlow.add(new JLabel(" y max: "));
        m = new Mouse(-7);
        graphSettingsFields[4] = new DropDownTextField(13, -1, -1, -1, -1, false, true);
        button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        graphSettingsFields[4].addLowerComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        graphSettingsFields[4].addLowerComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        graphSettingsFields[4].addLowerComponent(button);
        graphSettingsFields[4].buildWindow();
        tempFlow.add(graphSettingsFields[4]);
        tempPanel.add(tempFlow);
        tempFlow = new JPanel(new FlowLayout());
        tempFlow.add(new JLabel("x scale: "));
        m = new Mouse(-5);
        graphSettingsFields[2] = new DropDownTextField(13, -1, -1, -1, -1, false, true);
        button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        graphSettingsFields[2].addLowerComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        graphSettingsFields[2].addLowerComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        graphSettingsFields[2].addLowerComponent(button);
        graphSettingsFields[2].buildWindow();
        tempFlow.add(graphSettingsFields[2]);
        tempFlow.add(new JLabel(" y scale: "));
        m = new Mouse(-8);
        graphSettingsFields[5] = new DropDownTextField(13, -1, -1, -1, -1, false, true);
        button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        graphSettingsFields[5].addLowerComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        graphSettingsFields[5].addLowerComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        graphSettingsFields[5].addLowerComponent(button);
        graphSettingsFields[5].buildWindow();
        tempFlow.add(graphSettingsFields[5]);
        tempPanel.add(tempFlow);
        for (int i = 0; i < graphSettingsFields.length; i++) {
            ((AbstractDocument) graphSettingsFields[i].getDocument()).setDocumentFilter(new UnderscoreFilter() {
            });
        }
        tempFlow = new JPanel(new FlowLayout());
        tempFlow.add(new JLabel("(double-click text fields to show pop-up menu)"));
        tempPanel.add(tempFlow);
        tempFlow = new JPanel(new FlowLayout());
        JCheckBox checkBox = new JCheckBox("Mouse Coordinates");
        checkBox.setSelected(graph.getTrace());
        checkBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                graph.setTrace(((JCheckBox) e.getSource()).isSelected());
            }
        });
        tempFlow.add(checkBox);
        checkBox = new JCheckBox("Axes");
        checkBox.setSelected(graph.getAxis());
        checkBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                graph.setAxis(((JCheckBox) e.getSource()).isSelected());
            }
        });
        tempFlow.add(checkBox);
        tempPanel.add(tempFlow);
        tempFlow = new JPanel(new FlowLayout());
        checkBox = new JCheckBox("Function Coordinates");
        checkBox.setSelected(graph.getFunctionTrace());
        checkBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                graph.setFunctionTrace(((JCheckBox) e.getSource()).isSelected());
            }
        });
        tempFlow.add(checkBox);
        checkBox = new JCheckBox("Grid");
        checkBox.setSelected(graph.getGrid());
        checkBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                graph.setGrid(((JCheckBox) e.getSource()).isSelected());
            }
        });
        tempFlow.add(checkBox);
        tempPanel.add(tempFlow);
        tempFlow = new JPanel(new FlowLayout());
        checkBox = new JCheckBox("Drag");
        checkBox.setSelected(graphDrag);
        checkBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                graphDrag = ((JCheckBox) e.getSource()).isSelected();
            }
        });
        tempFlow.add(checkBox);
        checkBox = new JCheckBox("Zoom");
        checkBox.setSelected(graphZoom);
        checkBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                graphZoom = ((JCheckBox) e.getSource()).isSelected();
                popupMenu.getComponent(0).setEnabled(graphZoom);
                popupMenu.getComponent(1).setEnabled(graphZoom);
                popupMenu.getComponent(2).setEnabled(graphZoom);
            }
        });
        tempFlow.add(checkBox);
        tempPanel.add(tempFlow);
        synchGraphSettingsFields();
        return tempPanel;
    }

    private void createGraphPanel() {
        graphPanel = new JPanel() {

            private static final long serialVersionUID = 1L;

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (graph != null) {
                    graph.drawGraph(g, xPos, yPos);
                }
            }
        };
        graphPanel.addMouseMotionListener(new MouseMotionAdapter() {

            public void mouseMoved(MouseEvent e) {
                if (graph.getFunctionTrace() || graph.getTrace()) {
                    xPos = e.getX();
                    yPos = e.getY();
                    graphPanel.repaint();
                }
            }

            public void mouseDragged(MouseEvent e) {
                if (graphDrag && e.getModifiers() == 16 && e.getX() >= 0 && e.getX() < graph.getAppletSize() && e.getY() >= 0 && e.getY() < graph.getAppletSize()) {
                    int xMov = xPos - e.getX();
                    int yMov = e.getY() - yPos;
                    graph.dragGraph(xMov, yMov);
                    graphPanel.repaint();
                    xPos = e.getX();
                    yPos = e.getY();
                }
            }
        });
        graphPanel.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
                xPos = e.getX();
                yPos = e.getY();
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
                xPos = e.getX();
                yPos = e.getY();
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void createJPopupMenu() {
        popupMenu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Zoom in");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                graph.zoom(true, xPos, yPos, 2);
                graphPanel.repaint();
            }
        });
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("Zoom out");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                graph.zoom(false, xPos, yPos, 2);
                graphPanel.repaint();
            }
        });
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("Custom");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    double zoomScale = Double.parseDouble(JOptionPane.showInputDialog("Input zoom multiplier\n(positive to zoom in, negative to zoom out)"));
                    graph.zoom((zoomScale > 0 ? true : false), xPos, yPos, Math.abs(zoomScale));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Invalid Entry.");
                }
                graphPanel.repaint();
            }
        });
        popupMenu.add(menuItem);
    }

    private void addFunction() {
        addFunctionToPanel("", false, Color.black);
        for (int x = 0; x < numFunctions; x++) {
            if (functionTextFields[x].windowIsVisible()) functionTextFields[x].setWindowVisible(false);
        }
        graph.addFunction("");
    }

    private void addFunctionToPanel(String aFunction, boolean aEnabled, Color aColor) {
        numFunctions++;
        Container flow = new JPanel(new FlowLayout());
        flow.add(new JLabel("Function " + numFunctions + " ="));
        functionTextFields[numFunctions - 1] = new DropDownTextField(10, 114, 68, 108, 69, true, true);
        ((AbstractDocument) functionTextFields[numFunctions - 1].getDocument()).setDocumentFilter(new UnderscoreFilter() {
        });
        functionTextFields[numFunctions - 1].setText(aFunction);
        functionTextFields[numFunctions - 1].setUpperLayout(new GridLayout(4, 3));
        Mouse m = new Mouse(numFunctions - 1);
        JButton button = new JButton(7 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(8 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(9 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(4 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(5 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(6 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(1 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(2 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(3 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(0 + "");
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton(".");
        button.setFont(new Font("Dialog", 1, 10));
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        button = new JButton("" + Main.calculator.NEGATIVE);
        button.addMouseListener(m);
        functionTextFields[numFunctions - 1].addUpperComponent(button);
        boolean[] fonts = new boolean[12];
        fonts[10] = true;
        functionTextFields[numFunctions - 1].setUpperFont(new Font("Default", Font.BOLD, 8), fonts);
        functionTextFields[numFunctions - 1].setLowerLayout(new GridLayout(3, 2));
        JMenu menu = new JMenu("Trig     ");
        JMenuItem jmItem = new JMenuItem("sin");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("cos");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("tan");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("csc");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("sec");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("cot");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);
        menu = new JMenu("Inv. Trig");
        jmItem = new JMenuItem("asin");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("acos");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("atan");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("acsc");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("asec");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("acot");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        menuBar.add(menu);
        functionTextFields[numFunctions - 1].addLowerComponent(menuBar);
        menu = new JMenu("Ops.     ");
        jmItem = new JMenuItem("/");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("*");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("-");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("+");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("%");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("^");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        menuBar = new JMenuBar();
        menuBar.add(menu);
        menu = new JMenu("Exp.     ");
        jmItem = new JMenuItem("" + Main.calculator.SQUARE);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.CUBE);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.FOUR);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.FIVE);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.SIX);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.SEVEN);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.EIGHT);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.NINE);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        menuBar.add(menu);
        functionTextFields[numFunctions - 1].addLowerComponent(menuBar);
        menu = new JMenu("Vars     ");
        jmItem = new JMenuItem("x");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.E);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem(Main.calculator.PI + "");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        menuBar = new JMenuBar();
        menuBar.add(menu);
        menu = new JMenu("Other   ");
        jmItem = new JMenuItem("(");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem(")");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("log");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("ln");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("abs");
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.SQUARE_ROOT);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.CUBE_ROOT);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.FOUR_ROOT);
        jmItem.addMouseListener(m);
        menu.add(jmItem);
        menuBar.add(menu);
        functionTextFields[numFunctions - 1].addLowerComponent(menuBar);
        functionTextFields[numFunctions - 1].buildWindow();
        flow.add(functionTextFields[numFunctions - 1]);
        button = new JButton("" + Main.calculator.BACKSPACE);
        button.addMouseListener(m);
        flow.add(button);
        functionTextFields[numFunctions - 1].setBackground(aColor);
        if (aColor.getRed() + aColor.getGreen() + aColor.getBlue() > 382) {
            functionTextFields[numFunctions - 1].setForeground(new Color(0, 0, 0));
            functionTextFields[numFunctions - 1].setCaretColor(new Color(0, 0, 0));
        } else {
            functionTextFields[numFunctions - 1].setForeground(new Color(255, 255, 255));
            functionTextFields[numFunctions - 1].setCaretColor(new Color(255, 255, 255));
        }
        JCheckBox enable = new JCheckBox("Enable " + numFunctions, aEnabled);
        enable.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                text = ((JCheckBox) e.getSource()).getText();
                text = text.substring(text.indexOf(' ') + 1);
                graph.enableDisableFunction(Integer.parseInt(text) - 1, ((JCheckBox) e.getSource()).isSelected());
            }
        });
        flow.add(enable);
        JButton color = new JButton("Color " + numFunctions);
        color.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                text = ((JButton) e.getSource()).getText();
                text = text.substring(text.indexOf(' ') + 1);
                int pos = Integer.parseInt(text) - 1;
                Color c = JColorChooser.showDialog(GraphFrame.this, "Choose Color", graph.getColor(pos));
                if (c == null) {
                    c = graph.getColor(pos);
                }
                functionTextFields[pos].setBackground(c);
                if (c.getRed() + c.getGreen() + c.getBlue() > 382) {
                    functionTextFields[pos].setForeground(new Color(0, 0, 0));
                    functionTextFields[pos].setCaretColor(new Color(0, 0, 0));
                } else {
                    functionTextFields[pos].setForeground(new Color(255, 255, 255));
                    functionTextFields[pos].setCaretColor(new Color(255, 255, 255));
                }
                graph.setColor(pos, c);
            }
        });
        flow.add(color);
        functionPanel.add(flow);
        super.repaint();
    }

    class Mouse extends MouseAdapter {

        private int number;

        public Mouse(int num) {
            number = num;
        }

        public void mousePressed(MouseEvent e) {
            if (number >= 0 && (e.getSource().getClass().equals(JMenuItem.class) || e.getSource().getClass().equals(JButton.class))) {
                String txt = functionTextFields[number].getText();
                String fcar = txt.substring(0, functionTextFields[number].getCaretPosition());
                String car = txt.substring(functionTextFields[number].getCaretPosition());
                if (e.getSource().getClass().equals(JMenuItem.class)) {
                    text = ((JMenuItem) e.getSource()).getText();
                } else if (e.getSource().getClass().equals(JButton.class)) {
                    text = ((JButton) e.getSource()).getText();
                }
                if (text.equals("" + Main.calculator.BACKSPACE)) {
                    if (fcar.length() > 0) {
                        fcar = fcar.substring(0, fcar.length() - 1);
                        functionTextFields[number].setText(fcar + car);
                        functionTextFields[number].setCaretPosition(functionTextFields[number].getText().length() - car.length());
                    }
                } else if ((text.length() < 2 && (text.charAt(0) < Main.calculator.SQUARE_ROOT || text.charAt(0) > Main.calculator.FOUR_ROOT)) || (text.charAt(0) == Main.calculator.SQUARE || text.charAt(0) == Main.calculator.CUBE) || (text.charAt(0) >= Main.calculator.FOUR && text.charAt(0) <= Main.calculator.NINE)) {
                    functionTextFields[number].setText(fcar + text + car);
                    functionTextFields[number].setCaretPosition(functionTextFields[number].getText().length() - car.length());
                } else {
                    functionTextFields[number].setText(fcar + (((JMenuItem) e.getSource()).getText()) + "()" + car);
                    functionTextFields[number].setCaretPosition(functionTextFields[number].getText().length() - car.length() - 1);
                }
            } else if (number == -1) {
                String txt = tableStartField.getText();
                int cp = tableStartField.getCaretPosition();
                String fcar = txt.substring(0, cp), car = txt.substring(cp);
                tableStartField.setText(fcar + (((JButton) e.getSource()).getText()) + car);
                tableStartField.setCaretPosition(tableStartField.getText().length() - car.length());
            } else if (number == -2) {
                String txt = tableStepField.getText();
                int cp = tableStepField.getCaretPosition();
                String fcar = txt.substring(0, cp), car = txt.substring(cp);
                tableStepField.setText(fcar + (((JButton) e.getSource()).getText()) + car);
                tableStepField.setCaretPosition(tableStepField.getText().length() - car.length());
            } else {
                number += 3;
                number *= -1;
                String txt = graphSettingsFields[number].getText();
                int cp = graphSettingsFields[number].getCaretPosition();
                String fcar = txt.substring(0, cp), car = txt.substring(cp);
                graphSettingsFields[number].setText(fcar + (((JButton) e.getSource()).getText()) + car);
                graphSettingsFields[number].setCaretPosition(graphSettingsFields[number].getText().length() - car.length());
                number *= -1;
                number -= 3;
            }
        }
    }

    public void constructDialog() {
        dialog = new JDialog();
        textField = new DropDownTextField(10, -1, -1, -1, -1, true, false);
        dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.PAGE_AXIS));
        tempPanel = new JPanel();
        tempPanel.add(new JLabel("          Input a value for x.          "));
        dialog.getContentPane().add(tempPanel);
        tempPanel = new JPanel();
        tempPanel.add(new JLabel("(double-click text fields to show pop-up menu)"));
        dialog.getContentPane().add(tempPanel);
        tempPanel = new JPanel();
        tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.LINE_AXIS));
        tempPanel.add(new JLabel("          "));
        class Mouse1 extends MouseAdapter {

            public void mousePressed(MouseEvent e) {
                String txt = textField.getText();
                int cp = textField.getCaretPosition();
                String fcar = txt.substring(0, cp), car = txt.substring(cp);
                textField.setText(fcar + (((JButton) e.getSource()).getText()) + car);
                textField.setCaretPosition(textField.getText().length() - car.length());
            }
        }
        Mouse1 m = new Mouse1();
        JButton button = new JButton(Main.calculator.E + "");
        button.addMouseListener(m);
        textField.addUpperComponent(button);
        button = new JButton(Main.calculator.PI + "");
        button.addMouseListener(m);
        textField.addUpperComponent(button);
        button = new JButton(Main.calculator.NEGATIVE + "");
        button.addMouseListener(m);
        textField.addUpperComponent(button);
        textField.buildWindow();
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new UnderscoreFilter() {
        });
        textField.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String text = textField.getText();
                    try {
                        double x = Main.calculator.solve(Main.calculator.replaceVariables(text));
                        text = "Functional Values for x = " + Main.calculator.formatOutput(x);
                        double[] answers = graph.evaluate(x);
                        for (int i = 0; i < answers.length; i++) {
                            if (graph.getFunctionEnabled(i)) {
                                if (i < 9) text += "\ny" + ((char) (8321 + i)) + " = " + Main.calculator.formatOutput(answers[i]); else text += "\ny" + ((char) 8321) + ((char) (8320 + ((i + 1) % 10))) + " = " + Main.calculator.formatOutput(answers[i]);
                            }
                        }
                        boolean functionTraceState = graph.getFunctionTrace();
                        if (functionTraceState) {
                            graph.setFunctionTrace(false);
                            graph.setEvaluationDots(true, x);
                            graphPanel.repaint();
                        }
                        JOptionPane.showMessageDialog(null, text, "Error", JOptionPane.INFORMATION_MESSAGE);
                        graph.setEvaluationDots(false, x);
                        graph.setFunctionTrace(functionTraceState);
                        dialog.setVisible(false);
                        textField.setText("");
                        textField.setWindowVisible(false);
                    } catch (NumberFormatException ex) {
                        graph.setEvaluationDots(false, 0.0);
                        JOptionPane.showMessageDialog(null, "Invalid input", "Error", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IllegalArgumentException ex) {
                        graph.setEvaluationDots(false, 0.0);
                        JOptionPane.showMessageDialog(null, "Invalid input", "Error", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
        tempPanel.add(textField);
        tempPanel.add(new JLabel("          "));
        dialog.getContentPane().add(tempPanel);
        tempPanel = new JPanel(new FlowLayout());
        JButton temp = new JButton("OK");
        temp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String text = textField.getText();
                try {
                    double x = Main.calculator.solve(Main.calculator.replaceVariables(text));
                    text = "Functional Values for x = " + Main.calculator.formatOutput(x);
                    double[] answers = graph.evaluate(x);
                    for (int i = 0; i < answers.length; i++) {
                        if (graph.getFunctionEnabled(i)) {
                            if (i < 9) text += "\ny" + ((char) (8321 + i)) + " = " + Main.calculator.formatOutput(answers[i]); else text += "\ny" + ((char) 8321) + ((char) (8320 + ((i + 1) % 10))) + " = " + Main.calculator.formatOutput(answers[i]);
                        }
                    }
                    boolean functionTraceState = graph.getFunctionTrace();
                    if (functionTraceState) {
                        graph.setFunctionTrace(false);
                        graph.setEvaluationDots(true, x);
                        graphPanel.repaint();
                    }
                    tabbedPane.setSelectedIndex(0);
                    graph.setEvaluationDots(false, x);
                    graph.setFunctionTrace(functionTraceState);
                    dialog.setVisible(false);
                    dialog.dispose();
                    textField.setText("");
                    textField.setWindowVisible(false);
                    JOptionPane.showMessageDialog(null, text, "FunctionalValues", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException ex) {
                    graph.setEvaluationDots(false, 0.0);
                    JOptionPane.showMessageDialog(null, "Invalid input", "Error", JOptionPane.INFORMATION_MESSAGE);
                } catch (IllegalArgumentException ex) {
                    graph.setEvaluationDots(false, 0.0);
                    JOptionPane.showMessageDialog(null, "Invalid input", "Error", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        tempPanel.add(temp);
        temp = new JButton("Cancel");
        temp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                textField.setText("");
                textField.setWindowVisible(false);
            }
        });
        tempPanel.add(temp);
        dialog.getContentPane().add(tempPanel);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setTitle("Find functional values.");
        dialog.addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent arg0) {
                if (textField != null && textField.windowIsVisible()) {
                    textField.setWindowVisible(false);
                }
            }

            public void componentMoved(ComponentEvent arg0) {
                if (textField != null && textField.windowIsVisible()) {
                    textField.moved();
                }
            }

            public void componentResized(ComponentEvent arg0) {
            }

            public void componentShown(ComponentEvent arg0) {
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuItem jmItem;
        JMenuBar menuBar;
        JMenu currentMenuBar;
        menuBar = new JMenuBar();
        currentMenuBar = new JMenu("File");
        menuBar.add(currentMenuBar);
        jmItem = new JMenuItem("Reset Graph");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                Main.resetGraph();
            }
        });
        currentMenuBar.add(jmItem);
        jmItem = new JMenuItem("Open");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Calculator Files", "dat"));
                if (chooser.showOpenDialog(GraphFrame.this) == JFileChooser.APPROVE_OPTION) {
                    String file = chooser.getSelectedFile().getPath();
                    if (file.substring(file.lastIndexOf(".") + 1).equals("dat")) {
                        try {
                            loadGraph(chooser.getSelectedFile().getPath());
                        } catch (FileNotFoundException e1) {
                        } catch (IOException e1) {
                        } catch (ClassNotFoundException e1) {
                        }
                    }
                }
                repaint();
            }
        });
        currentMenuBar.add(jmItem);
        currentMenuBar.addSeparator();
        jmItem = new JMenuItem("Save As...");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Calculator Files", "dat"));
                if (chooser.showSaveDialog(GraphFrame.this) == JFileChooser.APPROVE_OPTION) {
                    String file = chooser.getSelectedFile().getPath();
                    if (file.lastIndexOf(".") < 0) file += ".dat";
                    if (file.substring(file.lastIndexOf(".") + 1).equals("dat")) {
                        try {
                            File f = new File(file);
                            if (!f.exists() || JOptionPane.showConfirmDialog(GraphFrame.this, "There is already a file with the name " + file.substring(file.lastIndexOf("\\") + 1) + ", are you sure you wish to overwrite it?") == JOptionPane.YES_OPTION) {
                                saveGraph(file);
                            }
                        } catch (HeadlessException e1) {
                        } catch (FileNotFoundException e1) {
                        } catch (IOException e1) {
                        }
                    }
                }
            }
        });
        currentMenuBar.add(jmItem);
        currentMenuBar.addSeparator();
        jmItem = new JMenuItem("Open Calculator");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Main.showCalc();
            }
        });
        currentMenuBar.add(jmItem);
        jmItem = new JMenuItem("Exit");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                dispose();
            }
        });
        currentMenuBar.add(jmItem);
        jmItem = new JMenuItem("Exit All");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                System.exit(0);
            }
        });
        currentMenuBar.add(jmItem);
        currentMenuBar = new JMenu("Tools");
        menuBar.add(currentMenuBar);
        jmItem = new JMenuItem("Find Function Values");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(true);
                dialog.setLocation(GraphFrame.this.getLocationOnScreen().x + (GraphFrame.this.getWidth() / 2) - (dialog.getWidth() / 2), GraphFrame.this.getLocationOnScreen().y + (GraphFrame.this.getHeight() / 2) - (dialog.getHeight() / 2));
            }
        });
        currentMenuBar.add(jmItem);
        currentMenuBar.addSeparator();
        jmItem = new JMenuItem("Settings");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                Main.showSettings();
            }
        });
        currentMenuBar.add(jmItem);
        currentMenuBar = new JMenu("Help");
        menuBar.add(currentMenuBar);
        jmItem = new JMenuItem("Help Contents");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                Main.showHelp();
            }
        });
        currentMenuBar.add(jmItem);
        currentMenuBar.addSeparator();
        jmItem = new JMenuItem("About BeastCalc");
        jmItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                Main.showAbout();
            }
        });
        currentMenuBar.add(jmItem);
        return menuBar;
    }

    private void takeFunctionInput() {
        for (int i = 0; i < numFunctions; i++) {
            if (!functionTextFields[i].getText().equals(graph.getFunction(i))) graph.setFunction(i, functionTextFields[i].getText());
        }
    }

    private void synchGraphSettingsFields() {
        graphSettingsFields[0].setText(Main.calculator.formatOutput(graph.getXMin()));
        graphSettingsFields[1].setText(Main.calculator.formatOutput(graph.getXMax()));
        graphSettingsFields[2].setText(Main.calculator.formatOutput(graph.getXScale()));
        graphSettingsFields[3].setText(Main.calculator.formatOutput(graph.getYMin()));
        graphSettingsFields[4].setText(Main.calculator.formatOutput(graph.getYMax()));
        graphSettingsFields[5].setText(Main.calculator.formatOutput(graph.getYScale()));
    }

    private void saveGraph(String saveName) throws FileNotFoundException, IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(saveName)));
        out.writeObject(graph);
        out.close();
    }

    private void loadGraph(String readName) throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(readName)));
        graph = (Graph) in.readObject();
        in.close();
        Main.loadGraph(graph);
    }

    static class CustomCellRenderer extends DefaultTableCellRenderer {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        public CustomCellRenderer() {
            super();
        }

        public void setValue(Object value) {
            if (value.getClass().equals(Double.class)) {
                setText(Main.calculator.formatOutput((Double) value));
            } else {
                setText(value.toString());
            }
        }
    }
}
