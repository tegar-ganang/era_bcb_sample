package net.simpleframework.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;
import net.simpleframework.swing.JActions.ActionCallback;
import net.simpleframework.swing.JTableEx.ExportData;
import net.simpleframework.swing.JTableEx.IRowObject;
import net.simpleframework.swing.JTableExColumn.FilterExpression;
import net.simpleframework.util.BeanUtils;
import net.simpleframework.util.CSVWriter;
import net.simpleframework.util.ConvertUtils;
import net.simpleframework.util.IoUtils;
import net.simpleframework.util.LocaleI18n;
import net.simpleframework.util.StringUtils;
import net.simpleframework.util.TextObject;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXEditorPane;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class JTableExDialogs {

    static class FunctionValueDialog extends EnhancedDialog {

        public FunctionValueDialog(final JComponent table, final String function) {
            super(SwingUtils.findWindow(table), null, table, function);
        }

        @Override
        protected void createUI() {
            size = new Dimension(320, 120);
            final JPanel jp = new JPanel(new BorderLayout());
            jp.setBorder(BorderFactory.createEmptyBorder(5, 4, 0, 4));
            final JEditorPane ep = createEditorPane();
            jp.add(new JScrollPane(ep));
            final String f = (String) elements[1];
            Object v = 0d;
            final JTableEx table = (JTableEx) elements[0];
            final String columnText = table.mTableExColumn.getColumnText();
            if ("sum".equals(f)) {
                setTitle(columnText + " - SUM");
                v = table.function_sum();
            } else if ("avg".equals(f)) {
                setTitle(columnText + " - AVG");
                v = table.function_avg();
            } else if ("max".equals(f)) {
                setTitle(columnText + " - MAX");
                v = table.function_max();
            } else if ("min".equals(f)) {
                setTitle(columnText + " - MIN");
                v = table.function_min();
            }
            final String text = ConvertUtils.toString(v);
            ep.setText(text);
            add(jp);
            final JXButton closeb = new JXButton(LocaleI18n.getMessage("OkCancelDialog.1"));
            closeb.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    FunctionValueDialog.this.dispose();
                }
            });
            final JXButton copyb = new JXButton(LocaleI18n.getMessage("FunctionValueDialog.0"));
            copyb.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    SwingUtils.copySystemClipboard(text);
                }
            });
            final JPanel bp = SwingUtils.createFlowPanel(FlowLayout.RIGHT, 0, copyb, 4, closeb);
            bp.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
            add(bp, BorderLayout.SOUTH);
        }

        private static final long serialVersionUID = -1468006872794534100L;
    }

    static class FilterDialog extends OkCancelDialog {

        private JComboBox relationBox1, relationBox2;

        private JTextField valueField1, valueField2;

        private JRadioButton noneRadio, andRadio, orRadio;

        static String getTitle(final JComponent table) {
            return LocaleI18n.getMessage("FilterDialog.0") + " - " + ((JTableEx) table).columnMenuItem.getText();
        }

        public FilterDialog(final JComponent table) {
            super(SwingUtils.findWindow(table), getTitle(table), table);
        }

        @Override
        protected Component createContentUI() {
            size = new Dimension(400, 160);
            final JPanel jp = new JPanel(new GridBagLayout());
            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.NORTH;
            final JTableEx table = (JTableEx) elements[0];
            final JTableExColumn column = table.mTableExColumn;
            final FilterExpression lfe = column.getLastFilterExpression();
            gbc.insets = new Insets(10, 10, 0, 5);
            jp.add(relationBox1 = new JComboBox(), gbc);
            relationBox1.setModel(new DefaultComboBoxModel(getRelationVector()));
            relationBox1.setPreferredSize(new Dimension(100, 0));
            if (lfe != null) {
                relationBox1.setSelectedIndex(getRelationIndex(lfe.item1.relation));
            }
            gbc.gridx++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            jp.add(createValueComponent(valueField1 = new JTextField()), gbc);
            if (lfe != null) {
                valueField1.setText(table.convertToString(column, lfe.item1.value));
            }
            gbc.gridy++;
            gbc.weightx = 0;
            gbc.insets = new Insets(0, 10, 0, 5);
            final ButtonGroup bg = new ButtonGroup();
            bg.add(noneRadio = new JRadioButton(LocaleI18n.getMessage("FilterDialog.8"), true));
            bg.add(andRadio = new JRadioButton(LocaleI18n.getMessage("FilterDialog.9")));
            bg.add(orRadio = new JRadioButton(LocaleI18n.getMessage("FilterDialog.10")));
            final ActionListener al = new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    relationBox2.setEnabled(!noneRadio.isSelected());
                    valueField2.setEnabled(!noneRadio.isSelected());
                }
            };
            noneRadio.addActionListener(al);
            andRadio.addActionListener(al);
            orRadio.addActionListener(al);
            jp.add(SwingUtils.createFlowPanel(noneRadio, andRadio, orRadio), gbc);
            if (lfe != null) {
                if ("and".equalsIgnoreCase(lfe.ope)) {
                    andRadio.setSelected(true);
                } else if ("or".equalsIgnoreCase(lfe.ope)) {
                    orRadio.setSelected(true);
                }
            }
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weighty = 1.0;
            jp.add(relationBox2 = new JComboBox(), gbc);
            relationBox2.setModel(new DefaultComboBoxModel(getRelationVector()));
            relationBox2.setPreferredSize(new Dimension(100, 0));
            if (lfe != null && lfe.item2 != null) {
                relationBox2.setSelectedIndex(getRelationIndex(lfe.item2.relation));
            }
            gbc.gridx++;
            gbc.weightx = 1.0;
            jp.add(createValueComponent(valueField2 = new JTextField()), gbc);
            if (lfe != null && lfe.item2 != null) {
                valueField2.setText(table.convertToString(column, lfe.item2.value));
            }
            al.actionPerformed(null);
            SwingUtils.initComponentHeight(relationBox1, valueField1, relationBox2, valueField2);
            return jp;
        }

        @Override
        public void ok() {
            super.ok();
            final JTableEx table = (JTableEx) elements[0];
            final JTableExColumn col = table.mTableExColumn;
            final FilterExpression filterExpression = col.createFilterExpression();
            filterExpression.item1 = col.createFilterItem();
            filterExpression.item1.relation = ((TextObject) relationBox1.getSelectedItem()).getName();
            filterExpression.item1.value = table.convertToObject(col, valueField1.getText());
            if (!noneRadio.isSelected()) {
                filterExpression.ope = andRadio.isSelected() ? "and" : "or";
                filterExpression.item2 = col.createFilterItem();
                filterExpression.item2.relation = ((TextObject) relationBox2.getSelectedItem()).getName();
                filterExpression.item2.value = table.convertToObject(col, valueField2.getText());
            }
            JActions.doActionCallback(table, new ActionCallback() {

                @Override
                public void doAction() {
                    table.doFilter(col, filterExpression);
                    table.getTableHeader().updateUI();
                    table.updateLineNo(true);
                    table.updateFixTable();
                }
            });
        }

        private JComponent createValueComponent(final JTextField valueField) {
            final JTableEx table = (JTableEx) elements[0];
            final JTableExColumn col = table.mTableExColumn;
            JComponent component = valueField;
            final Class<?> c = col.getBeanPropertyType();
            if (c != null) {
                if (Date.class.isAssignableFrom(c)) {
                    final JButton dateButton = new JButton();
                    component = SwingUtils.createTextButtonsPanel(valueField, dateButton);
                    dateButton.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            final DateTimeChooser chooser = new DateTimeChooser(FilterDialog.this);
                            valueField.setText(table.convertToString(col, chooser.getDate()));
                        }
                    });
                }
            }
            return component;
        }

        private int getRelationIndex(final String n) {
            int i = -1;
            for (final TextObject to : getRelationVector()) {
                i++;
                if (n != null && n.equals(to.getName())) {
                    return i;
                }
            }
            return i;
        }

        private Vector<TextObject> relationVector = null;

        private Vector<TextObject> getRelationVector() {
            if (relationVector != null) {
                return relationVector;
            }
            final JTableEx table = (JTableEx) elements[0];
            final JTableExColumn col = table.mTableExColumn;
            relationVector = new Vector<TextObject>();
            relationVector.addElement(new TextObject("=", LocaleI18n.getMessage("FilterDialog.1")));
            relationVector.addElement(new TextObject("<>", LocaleI18n.getMessage("FilterDialog.2")));
            final Class<?> c = col.getBeanPropertyType();
            if (c != null) {
                if (Number.class.isAssignableFrom(c) || Date.class.isAssignableFrom(c)) {
                    relationVector.addElement(new TextObject(">", LocaleI18n.getMessage("FilterDialog.3")));
                    relationVector.addElement(new TextObject(">=", LocaleI18n.getMessage("FilterDialog.4")));
                    relationVector.addElement(new TextObject("<", LocaleI18n.getMessage("FilterDialog.5")));
                    relationVector.addElement(new TextObject("<=", LocaleI18n.getMessage("FilterDialog.6")));
                }
                if (String.class.isAssignableFrom(c)) {
                    relationVector.addElement(new TextObject("like", LocaleI18n.getMessage("FilterDialog.7")));
                }
            }
            return relationVector;
        }

        private static final long serialVersionUID = -8330022453357127339L;
    }

    static class ExportCSVDialog extends EnhancedDialog {

        private MultipleLineLabel cl;

        private JLabel pl, tl;

        private JProgressBar pb;

        private JButton sb, ab;

        public ExportCSVDialog(final JComponent table) {
            super(SwingUtils.findWindow(table), "CSV", table);
        }

        @Override
        protected void createUI() {
            setLayout(new BorderLayout());
            size = new Dimension(400, 280);
            add(SwingUtils.createVerticalPanel(createPropertiesPanel(), createProgressPanel()));
        }

        private JPanel createPropertiesPanel() {
            final JButton fileButton = new JXButton(LocaleI18n.getMessage("ExportCSVDialog.4"));
            fileButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    final JFileChooser chooser = SwingUtils.createJFileChooser(new String[] { "csv" }, LocaleI18n.getMessage("ExportCSVDialog.5"));
                    chooser.setSelectedFile(new File("c:\\export.csv"));
                    final int r = chooser.showSaveDialog(ExportCSVDialog.this);
                    if (r == JFileChooser.APPROVE_OPTION) {
                        cl.setText(chooser.getSelectedFile().getAbsolutePath());
                        sb.doClick();
                    }
                }
            });
            cl = new MultipleLineLabel();
            cl.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
            final JTableEx table = (JTableEx) elements[0];
            cl.setForeground(table.getTableHeader().getForeground());
            return SwingUtils.createVerticalPanel(LocaleI18n.getMessage("ExportCSVDialog.0"), fileButton, cl);
        }

        private JPanel createProgressPanel() {
            final JPanel pbp = new JPanel(new BorderLayout());
            pbp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4), BorderFactory.createLineBorder(Color.GRAY)));
            pb = new JProgressBar();
            pb.setStringPainted(true);
            pb.setBorderPainted(false);
            pbp.add(pb);
            sb = new JXButton(LocaleI18n.getMessage("ExportCSVDialog.2"));
            sb.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    String csvFilename = cl.getText();
                    if (!StringUtils.hasText(csvFilename)) {
                        SwingUtils.showError(ExportCSVDialog.this, LocaleI18n.getMessage("ExportCSVDialog.7"));
                        return;
                    }
                    if (!csvFilename.toLowerCase().endsWith(".csv")) {
                        csvFilename += ".csv";
                    }
                    if (new File(csvFilename).exists()) {
                        if (!SwingUtils.confirm(ExportCSVDialog.this, LocaleI18n.getMessage("ExportCSVDialog.6"))) {
                            return;
                        }
                    }
                    doExport(csvFilename);
                }
            });
            ab = new JXButton(LocaleI18n.getMessage("ExportCSVDialog.3"));
            ab.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (sb.isEnabled()) {
                        return;
                    }
                    if (!SwingUtils.confirm(ExportCSVDialog.this, LocaleI18n.getMessage("ExportCSVDialog.8"))) {
                        return;
                    }
                    abort = true;
                }
            });
            final JXButton cb = new JXButton(LocaleI18n.getMessage("ErrorDialog.4"));
            cb.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    ExportCSVDialog.this.dispose();
                }
            });
            final JPanel jp = SwingUtils.createFlowPanel(FlowLayout.RIGHT, 0, sb, 5, ab, 10, cb, 5);
            final JPanel jpl = new JPanel(new BorderLayout());
            jpl.add(pl = new JLabel(" "));
            jpl.add(tl = new JLabel(), BorderLayout.EAST);
            jpl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            final JTableEx table = (JTableEx) elements[0];
            pl.setForeground(table.getTableHeader().getForeground());
            tl.setForeground(pl.getForeground());
            return SwingUtils.createVerticalPanel(LocaleI18n.getMessage("ExportCSVDialog.1"), pbp, jpl, jp);
        }

        private long st;

        private boolean abort;

        private void doExport(final String filename) {
            sb.setEnabled(false);
            pb.setValue(0);
            final JTableEx table = (JTableEx) elements[0];
            new SwingWorker<Object, Object>() {

                @Override
                protected Object doInBackground() {
                    st = System.currentTimeMillis();
                    pl.setText(LocaleI18n.getMessage("ExportCSVDialog.9"));
                    final ExportData qs = table.getCVSExportData();
                    final int count = qs.getCount();
                    pb.setMaximum(count);
                    final ArrayList<JTableExColumn> columns = new ArrayList<JTableExColumn>();
                    final ArrayList<String> columnLine = new ArrayList<String>();
                    table.doRowObjects(-1, new IRowObject() {

                        @Override
                        public void doRow(final JTableExColumn column, final Object o) {
                            columns.add(column);
                            columnLine.add(column.getColumnText());
                        }
                    });
                    try {
                        final CSVWriter writer = new CSVWriter((new FileWriter(IoUtils.createFile(new File(filename)))));
                        writer.writeNext(columnLine.toArray(new String[columnLine.size()]));
                        Object bean;
                        int line = 0;
                        while (qs.hasMoreElements()) {
                            bean = qs.nextElement();
                            if (abort) {
                                abort = false;
                                break;
                            }
                            final String[] nextLine = new String[columns.size()];
                            int i = 0;
                            for (final JTableExColumn column : columns) {
                                nextLine[i++] = table.convertToString(column, BeanUtils.getProperty(bean, column.getColumnName()));
                            }
                            writer.writeNext(nextLine);
                            publish(qs.getCount(), ++line);
                        }
                        writer.close();
                    } catch (final Exception e) {
                        SwingUtils.showError(ExportCSVDialog.this, e);
                    }
                    return count;
                }

                private final String cProcessMessage = LocaleI18n.getMessage("ExportCSVDialog.10");

                private final String cDoneMessage = LocaleI18n.getMessage("ExportCSVDialog.11");

                private final StringBuilder pm = new StringBuilder();

                @Override
                protected void process(final List<Object> chunks) {
                    final Object count = chunks.get(0);
                    final Object line = chunks.get(1);
                    pm.setLength(0);
                    pl.setText(pm.append(cProcessMessage).append(" ( ").append(line).append(" / ").append(count).append(" )").toString());
                    pb.setValue((Integer) line);
                    tl.setText((System.currentTimeMillis() - st) + "ms");
                }

                @Override
                protected void done() {
                    try {
                        sb.setEnabled(true);
                        final Object count = get();
                        pm.setLength(0);
                        pl.setText(pm.append(cDoneMessage).append(" ( ").append(count).append(" / ").append(count).append(" )").toString());
                        pb.setValue((Integer) count);
                        tl.setText((System.currentTimeMillis() - st) + "ms");
                    } catch (final Exception e) {
                        SwingUtils.showError(ExportCSVDialog.this, e);
                    }
                }
            }.execute();
        }

        private static final long serialVersionUID = 2388760450816410686L;
    }

    static class TableStatDialog extends OkCancelDialog {

        public TableStatDialog(final JComponent table) {
            super(SwingUtils.findWindow(table), LocaleI18n.getMessage("TableStatDialog.0"), table);
        }

        @Override
        protected boolean showOk() {
            return false;
        }

        @Override
        protected Component createContentUI() {
            size = new Dimension(400, 240);
            final JPanel j = new JPanel(new BorderLayout());
            final JEditorPane editorPane = createEditorPane();
            editorPane.setText(LocaleI18n.getMessage("TableStatDialog.1"));
            final JTableEx table = (JTableEx) elements[0];
            final String stat = LocaleI18n.replaceI18n(table.getTableStatistics());
            editorPane.setText(stat);
            j.setBorder(BorderFactory.createEmptyBorder(8, 6, 0, 6));
            j.add(new JScrollPane(editorPane));
            return j;
        }

        private static final long serialVersionUID = 3028043299934107029L;
    }

    static Color windowBgColor = Color.decode("#f2f2f2");

    static class ProgressWindow extends JWindow {

        private static final long serialVersionUID = -7721224224205374080L;

        private final JAbstractTableEx table;

        public JProgressBar pb;

        public JXLabel pl, tl;

        public boolean abort = false;

        ProgressWindow(final JAbstractTableEx table) {
            super(SwingUtils.findWindow(table));
            setAlwaysOnTop(true);
            this.table = table;
            final Window owner = getOwner();
            if (owner != null) {
                final ArrayList<ComponentListener> al = new ArrayList<ComponentListener>();
                for (final ComponentListener l : owner.getComponentListeners()) {
                    if (ComponentMoved.class.equals(l.getClass())) {
                        al.add(l);
                    }
                }
                for (final ComponentListener l : al) {
                    owner.removeComponentListener(l);
                }
                owner.addComponentListener(new ComponentMoved());
            }
            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseReleased(final MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        ProgressWindow.this.dispose();
                    }
                }
            });
            createUI();
        }

        void createUI() {
            setSize(360, 62);
            final JPanel pbp = new JPanel(new BorderLayout());
            pbp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4), BorderFactory.createLineBorder(Color.GRAY)));
            pb = new JProgressBar();
            pb.setStringPainted(true);
            pb.setBorderPainted(false);
            pbp.add(pb);
            final JButton deleteBtn = new JButton(LocaleI18n.getMessage("ExportCSVDialog.3"));
            pb.setPreferredSize(new Dimension(0, 20));
            deleteBtn.setPreferredSize(new Dimension(deleteBtn.getPreferredSize().width, 20));
            deleteBtn.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    abort = true;
                }
            });
            final JPanel p1 = new JPanel(new BorderLayout());
            p1.add(pbp);
            p1.add(deleteBtn, BorderLayout.EAST);
            final JPanel p2 = new JPanel(new BorderLayout());
            p2.add(pl = new JXLabel());
            p2.add(tl = new JXLabel(), BorderLayout.EAST);
            pl.setForeground(table.getTableHeader().getForeground());
            tl.setForeground(pl.getForeground());
            final JPanel c = SwingUtils.createVerticalPanel(p1, p2);
            c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), BorderFactory.createEmptyBorder(10, 5, 0, 5)));
            for (final Component comp : new Component[] { pb, pbp, p2, c }) {
                comp.setBackground(windowBgColor);
            }
            getContentPane().add(c);
            setVisible(true);
            pb.setFocusable(true);
            reLocation();
        }

        void reLocation() {
            if (!isVisible()) {
                return;
            }
            try {
                final Point p = table.getLocationOnScreen();
                final Rectangle r = table.getVisibleRect();
                setLocation(new Point(p.x + r.x + 2, p.y + r.y + 2));
            } catch (final Exception ex) {
                SwingUtils.centerWithinScreen(this);
            }
        }

        class ComponentMoved extends ComponentAdapter {

            @Override
            public void componentMoved(final ComponentEvent e) {
                reLocation();
            }
        }
    }

    static class ColumnControlDialog extends OkCancelDialog {

        private static final long serialVersionUID = 739413287063001033L;

        JCheckBoxList list;

        JEditorPane editorPane;

        public ColumnControlDialog(final JComponent table) {
            super(SwingUtils.findWindow(table), LocaleI18n.getMessage("ColumnControlDialog.0"), table);
        }

        @Override
        protected boolean showOk() {
            return false;
        }

        @Override
        protected Component createContentUI() {
            setEnterOk(false);
            size = new Dimension(300, 450);
            final JXMultiSplitPane splitPane = new JXMultiSplitPane();
            splitPane.setDividerSize(4);
            final MultiSplitLayout.Leaf top = new MultiSplitLayout.Leaf("top");
            final MultiSplitLayout.Leaf bottom = new MultiSplitLayout.Leaf("bottom");
            top.setWeight(1);
            final MultiSplitLayout.ColSplit root = new MultiSplitLayout.ColSplit();
            root.setChildren(Arrays.asList(top, new MultiSplitLayout.Divider(), bottom));
            splitPane.getMultiSplitLayout().setModel(root);
            splitPane.add(new JScrollPane(list = new JCheckBoxList()), "top");
            splitPane.add(new JScrollPane(editorPane = createEditorPane()), "bottom");
            editorPane.setPreferredSize(new Dimension(0, 128));
            final JTableEx table = (JTableEx) elements[0];
            final List<JTableExColumn> columns = table.getTableExAllColumns();
            final JPanel jp = new JPanel(new BorderLayout());
            jp.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
            final JLabel lbl = new JLabel(LocaleI18n.getMessage("ColumnControlDialog.1", columns.size()));
            lbl.setForeground(table.getTableHeader().getForeground());
            final JCheckBox cbox = new JCheckBox(LocaleI18n.getMessage("ColumnControlDialog.11"));
            cbox.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    final ListModel lm = list.getModel();
                    for (int i = 0; i < lm.getSize(); i++) {
                        final JCheckBoxList.Entry entry = (JCheckBoxList.Entry) lm.getElementAt(i);
                        entry.setChecked(cbox.isSelected());
                        final JTableExColumn column = ((ColumnWrapper) entry.getValue()).column;
                        column.setVisible(entry.isChecked());
                    }
                    list.updateUI();
                }
            });
            final JPanel tp = new JPanel(new BorderLayout());
            tp.add(lbl);
            tp.add(cbox, BorderLayout.EAST);
            jp.add(tp, BorderLayout.NORTH);
            jp.add(splitPane);
            for (final JTableExColumn column : columns) {
                list.addItem(column.isVisible(), new ColumnWrapper(column));
            }
            final String[] cols = new String[] { "ColumnControlDialog.2", "ColumnControlDialog.3", "ColumnControlDialog.4", "ColumnControlDialog.5", "ColumnControlDialog.6", "ColumnControlDialog.7" };
            list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    final JCheckBoxList.Entry entry = (JCheckBoxList.Entry) list.getSelectedValue();
                    if (entry == null) {
                        return;
                    }
                    final JTableExColumn column = ((ColumnWrapper) entry.getValue()).column;
                    final Class<?> bt = column.getBeanPropertyType();
                    String alignment;
                    if (column.getAlignment() == SwingConstants.LEFT) {
                        alignment = "ColumnControlDialog.8";
                    } else if (column.getAlignment() == SwingConstants.CENTER) {
                        alignment = "ColumnControlDialog.9";
                    } else {
                        alignment = "ColumnControlDialog.10";
                    }
                    String columnText = column.getColumnText();
                    columnText = StringUtils.replace(columnText, "<html>", "");
                    columnText = StringUtils.replace(columnText, "</html>", "");
                    final Object[] values = new Object[] { column.getColumnName(), columnText, bt == null ? "" : bt.getSimpleName(), StringUtils.text(column.getFormat()), LocaleI18n.getMessage(alignment), column.isFreezable() ? JTableExUtils.colorRed("Y") : "N" };
                    final StringBuilder sb = new StringBuilder();
                    sb.append("<table border=\"0\" cellpadding=\"2\" cellspacing=\"0\">");
                    int i = 0;
                    for (final String col : cols) {
                        sb.append("<tr><td style=\"text-align: right;\">");
                        sb.append(LocaleI18n.getMessage(col)).append("</td>");
                        sb.append("<td style=\"padding-left: 4px;\">");
                        sb.append(values[i++]).append("</td></tr>");
                    }
                    sb.append("</table>");
                    editorPane.setText(sb.toString());
                }
            });
            list.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseReleased(final MouseEvent e) {
                    final JCheckBoxList.Entry entry = (JCheckBoxList.Entry) list.getSelectedValue();
                    if (entry == null) {
                        return;
                    }
                    final JTableExColumn column = ((ColumnWrapper) entry.getValue()).column;
                    column.setVisible(entry.isChecked());
                }
            });
            return jp;
        }

        class ColumnWrapper {

            JTableExColumn column;

            ColumnWrapper(final JTableExColumn column) {
                this.column = column;
            }

            @Override
            public String toString() {
                return column.getColumnText();
            }
        }
    }

    static Color editorPaneBackground = Color.decode("#FFF6FA");

    static final String sDIV = "<div style=\"font-family: verdana; color: #116F01; font-size: 12;\">";

    static final String eDIV = "</div>";

    static JEditorPane createEditorPane() {
        final JEditorPane ep = new JXEditorPane() {

            private static final long serialVersionUID = -6461886063482533821L;

            @Override
            public void setText(final String t) {
                super.setText(sDIV + t + eDIV);
                setCaretPosition(0);
            }
        };
        ep.setEditable(false);
        ep.setEditorKit(new HTMLEditorKit());
        ep.setBackground(editorPaneBackground);
        return ep;
    }
}
