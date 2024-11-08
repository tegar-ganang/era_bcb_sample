package org.sosy_lab.ccvisu.ui.controlpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.sosy_lab.ccvisu.graph.GraphData;
import org.sosy_lab.ccvisu.graph.GraphVertex;
import org.sosy_lab.ccvisu.graph.GraphVertex.Shape;
import org.sosy_lab.ccvisu.graph.Group;
import org.sosy_lab.ccvisu.graph.NameVisibility;
import org.sosy_lab.ccvisu.readers.ReaderData;
import org.sosy_lab.ccvisu.readers.ReaderWriterGroup;
import org.sosy_lab.ccvisu.ui.DialogEditGroup;
import org.sosy_lab.ccvisu.ui.helper.ColorComboBox;
import org.sosy_lab.ccvisu.ui.helper.ShapeComboBox;
import org.sosy_lab.ccvisu.writers.WriterDataLayoutDISP;
import org.sosy_lab.util.Colors;

/**
 * a GUI to manage the groups define new, remove, …
 */
public class ToolPanelGroups extends JPanel {

    private static final long serialVersionUID = 1L;

    private GraphData graph;

    /** where the groups are stocked */
    private WriterDataLayoutDISP writer;

    private JButton saveButton;

    private JButton loadButton;

    private JButton upButton;

    private JButton downButton;

    private JButton deleteButton;

    private JButton editButton;

    private JButton newButton;

    private JButton hideLabelsButton;

    private JButton showLabelsButton;

    private JCheckBox showGroupfreeNodesCheckBox;

    private JCheckBox showGroupCheckBox;

    private JCheckBox showGraphicInfoCheckBox;

    @SuppressWarnings("rawtypes")
    private ColorComboBox colorComboBox;

    @SuppressWarnings("rawtypes")
    private ShapeComboBox shapeComboBox;

    @SuppressWarnings("rawtypes")
    private JList groupsList;

    private JLabel numberOfNodesLabel;

    private JLabel avgRadiusLabel;

    private JLabel barycenterXLabel;

    private JLabel barycenterYLabel;

    private JLabel barycenterZLabel;

    /**
   * Create the panel.
   */
    public ToolPanelGroups(WriterDataLayoutDISP writer, GraphData graph) {
        this.graph = graph;
        this.writer = writer;
        setLayout(new GridLayout(0, 2, 0, 0));
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(panel);
        panel.setLayout(new BorderLayout(0, 0));
        groupsList = new JList(graph.getGroups().toArray());
        groupsList.setBorder(null);
        panel.add(new JScrollPane(groupsList), BorderLayout.CENTER);
        JPanel panel_2 = new JPanel();
        panel.add(panel_2, BorderLayout.SOUTH);
        panel_2.setLayout(new BorderLayout(0, 0));
        JToolBar editToolBar = new JToolBar();
        editToolBar.setFloatable(false);
        panel_2.add(editToolBar);
        newButton = new JButton("New");
        editToolBar.add(newButton);
        editButton = new JButton("Edit");
        editToolBar.add(editButton);
        deleteButton = new JButton("Delete");
        editToolBar.add(deleteButton);
        JToolBar sortToolBar = new JToolBar();
        sortToolBar.setFloatable(false);
        panel_2.add(sortToolBar, BorderLayout.EAST);
        upButton = new JButton("Up");
        sortToolBar.add(upButton);
        upButton.setHorizontalAlignment(SwingConstants.RIGHT);
        downButton = new JButton("Down");
        sortToolBar.add(downButton);
        downButton.setHorizontalAlignment(SwingConstants.RIGHT);
        JToolBar storeToolBar = new JToolBar();
        storeToolBar.setFloatable(false);
        panel.add(storeToolBar, BorderLayout.NORTH);
        loadButton = new JButton("Load...");
        storeToolBar.add(loadButton);
        saveButton = new JButton("Save...");
        storeToolBar.add(saveButton);
        JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        createTabGroupPanel(tabbedPane);
        createTabMeasuresPanel(tabbedPane);
        add(tabbedPane);
        assignListeners();
    }

    private void createTabGroupPanel(JTabbedPane tabbedPane) {
        JPanel groupPanel = new JPanel();
        tabbedPane.addTab("Group(s)", null, groupPanel, null);
        GridBagLayout groupPanelGridBagLayout = new GridBagLayout();
        groupPanelGridBagLayout.columnWidths = new int[] { -81, 0, 16, 42, 52, 0 };
        groupPanelGridBagLayout.rowHeights = new int[] { 23, 23, 23, 29, 0, 29, 27, 0 };
        groupPanelGridBagLayout.columnWeights = new double[] { 1.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        groupPanelGridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        groupPanel.setLayout(groupPanelGridBagLayout);
        showGroupfreeNodesCheckBox = new JCheckBox("Show group-free nodes");
        GridBagConstraints gbc_chckbxShowGroupfreeNodes = createGridBagConstraint(0, 0, GridBagConstraints.NORTHWEST, new Insets(0, 0, 5, 0));
        gbc_chckbxShowGroupfreeNodes.gridwidth = 6;
        groupPanel.add(showGroupfreeNodesCheckBox, gbc_chckbxShowGroupfreeNodes);
        showGroupCheckBox = new JCheckBox("Show group");
        GridBagConstraints gbc_chckbxShowGroup = createGridBagConstraint(0, 1, GridBagConstraints.NORTHWEST, new Insets(0, 0, 5, 0));
        gbc_chckbxShowGroup.gridwidth = 5;
        groupPanel.add(showGroupCheckBox, gbc_chckbxShowGroup);
        showGraphicInfoCheckBox = new JCheckBox("Graphic informations");
        GridBagConstraints gbc_chckbxGraphicInformations = createGridBagConstraint(0, 2, GridBagConstraints.NORTHWEST, new Insets(0, 0, 5, 0));
        gbc_chckbxGraphicInformations.gridwidth = 6;
        groupPanel.add(showGraphicInfoCheckBox, gbc_chckbxGraphicInformations);
        shapeComboBox = new ShapeComboBox(Shape.DISC);
        GridBagConstraints gbc_shapeComboBox = createGridBagConstraint(1, 3);
        gbc_shapeComboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_shapeComboBox.gridwidth = 4;
        gbc_shapeComboBox.insets = new Insets(0, 0, 5, 0);
        groupPanel.add(shapeComboBox, gbc_shapeComboBox);
        JLabel lblShape = new JLabel("Shape:");
        lblShape.setHorizontalAlignment(SwingConstants.RIGHT);
        GridBagConstraints gbc_lblShape = createGridBagConstraint(0, 3);
        gbc_lblShape.fill = GridBagConstraints.HORIZONTAL;
        gbc_lblShape.insets = new Insets(0, 0, 5, 5);
        groupPanel.add(lblShape, gbc_lblShape);
        lblShape.setLabelFor(shapeComboBox);
        colorComboBox = new ColorComboBox(Colors.RED.get());
        GridBagConstraints gbc_colorComboBox = createGridBagConstraint(1, 4, GridBagConstraints.NORTH, new Insets(0, 0, 5, 0));
        gbc_colorComboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_colorComboBox.gridwidth = 4;
        groupPanel.add(colorComboBox, gbc_colorComboBox);
        JLabel lblColor = new JLabel("Color:");
        lblColor.setHorizontalAlignment(SwingConstants.RIGHT);
        GridBagConstraints gbc_lblColor = createGridBagConstraint(0, 4);
        gbc_lblColor.fill = GridBagConstraints.HORIZONTAL;
        gbc_lblColor.insets = new Insets(0, 0, 5, 5);
        groupPanel.add(lblColor, gbc_lblColor);
        lblColor.setLabelFor(colorComboBox);
        showLabelsButton = new JButton("Show labels");
        GridBagConstraints gbc_btnShowLabels = createGridBagConstraint(0, 5, GridBagConstraints.NORTHWEST, new Insets(0, 0, 5, 0));
        gbc_btnShowLabels.gridwidth = 5;
        groupPanel.add(showLabelsButton, gbc_btnShowLabels);
        hideLabelsButton = new JButton("Hide labels");
        GridBagConstraints gbc_btnHideLabels = createGridBagConstraint(0, 6);
        gbc_btnHideLabels.anchor = GridBagConstraints.NORTHWEST;
        gbc_btnHideLabels.gridwidth = 5;
        groupPanel.add(hideLabelsButton, gbc_btnHideLabels);
    }

    private void createTabMeasuresPanel(JTabbedPane tabbedPane) {
        JPanel measuresPanel = new JPanel();
        tabbedPane.addTab("Measures", null, measuresPanel, null);
        GridBagLayout measuresPanelGridBagLayout = new GridBagLayout();
        measuresPanelGridBagLayout.columnWidths = new int[] { 137, 42, 0 };
        measuresPanelGridBagLayout.rowHeights = new int[] { 127, 0, 0, 0, 0, 0, 127, 0 };
        measuresPanelGridBagLayout.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
        measuresPanelGridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        measuresPanel.setLayout(measuresPanelGridBagLayout);
        JLabel lblNewLabel = new JLabel("Number of vertices:");
        GridBagConstraints gbc_lblNewLabel = createGridBagConstraint(0, 1, GridBagConstraints.EAST, new Insets(0, 0, 5, 5));
        gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
        measuresPanel.add(lblNewLabel, gbc_lblNewLabel);
        numberOfNodesLabel = new JLabel("?");
        GridBagConstraints gbc_lblNewLabel_2_1_1 = createGridBagConstraint(1, 1, GridBagConstraints.WEST, new Insets(0, 0, 5, 0));
        measuresPanel.add(numberOfNodesLabel, gbc_lblNewLabel_2_1_1);
        JLabel lblNewLabel_1 = new JLabel("Average radius:");
        GridBagConstraints gbc_lblNewLabel_1 = createGridBagConstraint(0, 2, GridBagConstraints.EAST, new Insets(0, 0, 5, 5));
        gbc_lblNewLabel_1.fill = GridBagConstraints.VERTICAL;
        measuresPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
        avgRadiusLabel = new JLabel("?");
        GridBagConstraints gbc_lblNewLabel_3 = createGridBagConstraint(1, 2, GridBagConstraints.WEST, new Insets(0, 0, 5, 0));
        measuresPanel.add(avgRadiusLabel, gbc_lblNewLabel_3);
        JLabel lblBarycenterX = new JLabel("Barycenter X:");
        GridBagConstraints gbc_lblBarycenterX = createGridBagConstraint(0, 3, GridBagConstraints.EAST, new Insets(0, 0, 5, 5));
        measuresPanel.add(lblBarycenterX, gbc_lblBarycenterX);
        barycenterXLabel = new JLabel("?");
        GridBagConstraints gbc_barycenterXLabel = createGridBagConstraint(1, 3, GridBagConstraints.WEST, new Insets(0, 0, 5, 0));
        measuresPanel.add(barycenterXLabel, gbc_barycenterXLabel);
        JLabel lblBarycenterY = new JLabel("Barycenter Y:");
        GridBagConstraints gbc_lblBarycenterY = createGridBagConstraint(0, 4, GridBagConstraints.EAST, new Insets(0, 0, 5, 5));
        measuresPanel.add(lblBarycenterY, gbc_lblBarycenterY);
        barycenterYLabel = new JLabel("?");
        GridBagConstraints gbc_barycenterYLabel = createGridBagConstraint(1, 4, GridBagConstraints.WEST, new Insets(0, 0, 5, 0));
        measuresPanel.add(barycenterYLabel, gbc_barycenterYLabel);
        JLabel lblBarycenterZ = new JLabel("Barycenter Z:");
        GridBagConstraints gbc_lblBarycenterZ = createGridBagConstraint(0, 5, GridBagConstraints.EAST, new Insets(0, 0, 5, 5));
        measuresPanel.add(lblBarycenterZ, gbc_lblBarycenterZ);
        barycenterZLabel = new JLabel("?");
        GridBagConstraints gbc_barycenterZLabel = createGridBagConstraint(1, 5, GridBagConstraints.WEST, new Insets(0, 0, 5, 0));
        measuresPanel.add(barycenterZLabel, gbc_barycenterZLabel);
    }

    private GridBagConstraints createGridBagConstraint(int gridx, int gridy, int anchor, Insets insets) {
        GridBagConstraints gridBagConstraints = createGridBagConstraint(gridx, gridy);
        gridBagConstraints.anchor = anchor;
        gridBagConstraints.insets = insets;
        return gridBagConstraints;
    }

    private GridBagConstraints createGridBagConstraint(int gridx, int gridy) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx;
        gridBagConstraints.gridy = gridy;
        return gridBagConstraints;
    }

    protected void assignListeners() {
        groupsList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent event) {
                refreshInfo();
            }
        });
        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileDialog = new JFileChooser(".");
                fileDialog.setFileFilter(ReaderData.mkExtensionFileFilter(".grp", "Group Files"));
                int outcome = fileDialog.showSaveDialog((Frame) null);
                if (outcome == JFileChooser.APPROVE_OPTION) {
                    assert (fileDialog.getCurrentDirectory() != null);
                    assert (fileDialog.getSelectedFile() != null);
                    String fileName = fileDialog.getCurrentDirectory().toString() + File.separator + fileDialog.getSelectedFile().getName();
                    try {
                        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
                        ReaderWriterGroup.write(out, writer);
                        System.err.println("Wrote groups informations to output '" + fileName + "'.");
                        out.close();
                    } catch (IOException e) {
                        System.err.println("error while writing (GroupManager.saveClt):");
                        e.printStackTrace();
                    }
                } else if (outcome == JFileChooser.CANCEL_OPTION) {
                }
            }
        });
        loadButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                JFileChooser fileDialog = new JFileChooser(".");
                fileDialog.setFileFilter(ReaderData.mkExtensionFileFilter(".grp", "Group Files"));
                int outcome = fileDialog.showOpenDialog((Frame) null);
                if (outcome == JFileChooser.APPROVE_OPTION) {
                    assert (fileDialog.getCurrentDirectory() != null);
                    assert (fileDialog.getSelectedFile() != null);
                    String fileName = fileDialog.getCurrentDirectory().toString() + File.separator + fileDialog.getSelectedFile().getName();
                    BufferedReader fileReader = null;
                    try {
                        fileReader = new BufferedReader(new FileReader(fileName));
                        ReaderWriterGroup.read(fileReader, writer);
                        fileReader.close();
                    } catch (Exception e) {
                        System.err.println("Exception while reading from file '" + fileName + "'.");
                        System.err.println(e);
                    }
                } else if (outcome == JFileChooser.CANCEL_OPTION) {
                }
            }
        });
        ItemListener propItemListener = new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent event) {
                int[] indices = groupsList.getSelectedIndices();
                for (int index : indices) {
                    Group group = getGroupFromListIndex(index);
                    if (group != null) {
                        if (event.getSource() instanceof JComboBox) {
                            JComboBox eventSource = (JComboBox) event.getSource();
                            if (eventSource == colorComboBox) {
                                Color color = colorComboBox.getSelectedColor();
                                assert (color != null);
                                group.setColor(color);
                                shapeComboBox.setColor(color);
                            } else if (eventSource == shapeComboBox) {
                                Shape shape = shapeComboBox.getSelectedShape();
                                assert (shape != null);
                                group.setShape(shape);
                            }
                        } else if (event.getSource() instanceof JCheckBox) {
                            JCheckBox eventSource = (JCheckBox) event.getSource();
                            if (eventSource == showGroupCheckBox) {
                                group.visible = showGroupCheckBox.isSelected();
                            } else if (eventSource == showGraphicInfoCheckBox) {
                                group.info = showGraphicInfoCheckBox.isSelected();
                            }
                        }
                    }
                }
                graph.notifyAboutGroupsChange(null);
            }
        };
        colorComboBox.addItemListener(propItemListener);
        shapeComboBox.addItemListener(propItemListener);
        showGroupCheckBox.addItemListener(propItemListener);
        showGraphicInfoCheckBox.addItemListener(propItemListener);
        showGroupfreeNodesCheckBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent event) {
                graph.getGroup(0).visible = showGroupfreeNodesCheckBox.isSelected();
                graph.notifyAboutGroupsChange(null);
            }
        });
        ActionListener propActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                JButton botton = (JButton) event.getSource();
                Group group = getGroupFromListIndex(groupsList.getSelectedIndex());
                if (group != null) {
                    for (GraphVertex graphVertex : group) {
                        if (botton == showLabelsButton) {
                            graphVertex.setShowName(NameVisibility.Priority.GROUPS, true);
                        } else if (botton == hideLabelsButton) {
                            graphVertex.setShowName(NameVisibility.Priority.GROUPS, false);
                        }
                    }
                    graph.notifyAboutGroupsChange(null);
                }
            }
        };
        showLabelsButton.addActionListener(propActionListener);
        hideLabelsButton.addActionListener(propActionListener);
        newButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                String newGroupName = JOptionPane.showInputDialog(null, "Enter a name", "Name of the new group", JOptionPane.QUESTION_MESSAGE);
                if (newGroupName != null) {
                    if (graph.getGroup(newGroupName) == null) {
                        graph.addGroup(new Group(newGroupName, graph));
                    }
                }
            }
        });
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                Group group = getGroupFromListIndex(groupsList.getSelectedIndex());
                if (group != null) {
                    DialogEditGroup dialog = new DialogEditGroup(graph, group);
                    dialog.setModal(true);
                    dialog.setVisible(true);
                }
            }
        });
        deleteButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                int index = groupsList.getSelectedIndex();
                if (index > 0 && index < graph.getNumberOfGroups() - 1) {
                    graph.removeGroup(index);
                }
            }
        });
        upButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                int index = groupsList.getSelectedIndex();
                if (index < graph.getNumberOfGroups() - 1) {
                    graph.moveGroupUp(index);
                    groupsList.setSelectedIndex(index - 1);
                }
            }
        });
        downButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                int index = groupsList.getSelectedIndex();
                if (index < graph.getNumberOfGroups() - 1) {
                    graph.moveGroupDown(index);
                    groupsList.setSelectedIndex(index + 1);
                }
            }
        });
    }

    /**
   * return the group at index
   *
   * @param index the index of the selected group
   * @return the selected group
   */
    private Group getGroupFromListIndex(int index) {
        if (index < graph.getNumberOfGroups() - 1) {
            return graph.getGroup(index);
        } else {
            return null;
        }
    }

    /**
   * refresh the information about the selected group
   */
    protected void refreshInfo() {
        int index = groupsList.getSelectedIndex();
        Group group = graph.getGroup(index);
        if (group != null) {
            avgRadiusLabel.setText(Float.toString(group.getAverageRadius()));
            numberOfNodesLabel.setText(Integer.toString(group.getNodes().size()));
            showGroupCheckBox.setSelected(group.visible);
            showGraphicInfoCheckBox.setSelected(group.info);
            barycenterXLabel.setText(Float.toString(group.getX()));
            barycenterYLabel.setText(Float.toString(group.getY()));
            barycenterZLabel.setText(Float.toString(group.getZ()));
            String colorString = Colors.toString(group.getColor());
            if (colorString != null) {
                colorComboBox.setSelectedItem(Colors.valueOfUpper(colorString));
            }
            shapeComboBox.setSelectedItem(group.getShape());
            shapeComboBox.validate();
        }
    }
}
