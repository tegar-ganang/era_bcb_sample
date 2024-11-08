package org.obe.runtime.tool;

import com.toedter.calendar.JCalendar;
import org.obe.client.api.ClientConfig;
import org.obe.client.api.repository.UpdateProcessAttributesMetaData;
import org.obe.client.api.tool.Parameter;
import org.obe.client.api.tool.ToolInvocation;
import org.obe.util.DateUtilities;
import org.obe.util.SpringUtilities;
import org.obe.xpdl.model.data.Type;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.Date;

/**
 * A tool agent that updates a set of process instance attributes.
 *
 * @author Adrian Price
 */
public class UpdateProcessAttributes extends AbstractToolAgent {

    private static final long serialVersionUID = 5475443176807253400L;

    private static final boolean USE_STDIO = ClientConfig.useSTDIO();

    private static final Dimension BUTTON_SIZE = new Dimension(75, 25);

    private static final Dimension COMPONENT_SIZE = new Dimension(200, 20);

    private static final int GAP = 10;

    private final UpdateProcessAttributesMetaData _metadata;

    private static final int BUFFER_SIZE = 1024;

    protected static final String STATUS = "status";

    private static final class JFileSelector extends JPanel {

        private JTextField _name;

        {
            setLayout(new FlowLayout());
            _name = new JTextField();
            _name.setPreferredSize(COMPONENT_SIZE);
            add(_name);
            JButton browse = new JButton("Browse...");
            browse.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    JFileChooser chooser = new JFileChooser(getFile());
                    int ret = chooser.showOpenDialog(JFileSelector.this);
                    if (ret == JFileChooser.APPROVE_OPTION) {
                        try {
                            setText(chooser.getSelectedFile().getCanonicalPath());
                        } catch (IOException e) {
                        }
                    }
                }
            });
            add(browse);
        }

        JFileSelector() {
        }

        JFileSelector(String filename) {
            setText(filename);
        }

        String getText() {
            return _name.getText();
        }

        void setText(String t) {
            _name.setText(t);
        }

        File getFile() {
            String text = getText();
            File location = text == null ? null : new File(text);
            return location == null || !location.exists() ? null : location;
        }
    }

    private class UpdateProcessAttributesDialog extends JDialog {

        private ToolInvocation _ti;

        private int _exitCode = EXIT_CANCEL;

        private static final int MAX_DIGITS_INTEGER = 19;

        UpdateProcessAttributesDialog(ToolInvocation ti) {
            super((Frame) null, true);
            _ti = ti;
            init();
        }

        public int getExitCode() {
            return _exitCode;
        }

        private void init() {
            if (_metadata.getTitle() != null) setTitle(_metadata.getTitle());
            setSize(_metadata.getWidth(), _metadata.getHeight());
            Container contentPane = getContentPane();
            contentPane.setLayout(new BorderLayout());
            final JComponent componentPane = new JPanel(new SpringLayout());
            if (_metadata.getStatus()) {
                JLabel statusBar = new JLabel();
                statusBar.setName(STATUS);
                contentPane.add(statusBar);
            }
            if (_metadata.getScrollbars()) {
            }
            final int n = _ti.parameters.length;
            for (int i = 0; i < n; i++) {
                Parameter param = _ti.parameters[i];
                Object value = param.getValue();
                String strValue = value == null ? null : value.toString();
                JLabel label = new JLabel(param.getName());
                JComponent component;
                JFormattedTextField textField;
                NumberFormat fmt;
                Type type = param.getDataType().getType().getImpliedType();
                switch(type.value()) {
                    case Type.STRING_TYPE:
                        component = new JTextField(strValue);
                        component.setPreferredSize(COMPONENT_SIZE);
                        break;
                    case Type.FLOAT_TYPE:
                        fmt = NumberFormat.getNumberInstance();
                        fmt.setMaximumFractionDigits(10);
                        fmt.setMaximumFractionDigits(10);
                        component = textField = new JFormattedTextField(fmt);
                        component.setPreferredSize(COMPONENT_SIZE);
                        textField.setText(strValue);
                        break;
                    case Type.INTEGER_TYPE:
                        fmt = NumberFormat.getIntegerInstance();
                        fmt.setMaximumIntegerDigits(MAX_DIGITS_INTEGER);
                        component = textField = new JFormattedTextField(fmt);
                        component.setPreferredSize(COMPONENT_SIZE);
                        textField.setText(strValue);
                        break;
                    case Type.DATETIME_TYPE:
                        component = new JCalendar((Date) value);
                        break;
                    case Type.BOOLEAN_TYPE:
                        component = new JCheckBox((String) null, Boolean.TRUE.equals(value));
                        break;
                    case Type.PERFORMER_TYPE:
                        JComboBox combo = new JComboBox(new Object[] { strValue });
                        combo.setEditable(true);
                        component = combo;
                        component.setPreferredSize(COMPONENT_SIZE);
                        break;
                    case Type.SCHEMA_TYPE:
                        component = new JFileSelector(strValue);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported data type: " + type);
                }
                label.setLabelFor(component);
                componentPane.add(label);
                componentPane.add(component);
            }
            SpringUtilities.makeCompactGrid(componentPane, _ti.parameters.length, 2, GAP, GAP, GAP, GAP >> 1);
            JComponent buttonPane = new JPanel(new FlowLayout());
            JButton ok = new JButton("OK");
            ok.setPreferredSize(BUTTON_SIZE);
            ok.setDefaultCapable(true);
            buttonPane.add(ok);
            JButton cancel = new JButton("Cancel");
            cancel.setPreferredSize(BUTTON_SIZE);
            buttonPane.add(cancel);
            contentPane.add(componentPane, BorderLayout.CENTER);
            contentPane.add(buttonPane, BorderLayout.SOUTH);
            getRootPane().setDefaultButton(ok);
            pack();
            ok.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        Component[] components = componentPane.getComponents();
                        for (int i = 0; i < n; i++) {
                            Component component = components[2 * i + 1];
                            Parameter param = _ti.parameters[i];
                            Object value;
                            int type = param.getDataType().getType().getImpliedType().value();
                            switch(type) {
                                case Type.STRING_TYPE:
                                    value = ((JTextComponent) component).getText();
                                    break;
                                case Type.FLOAT_TYPE:
                                case Type.INTEGER_TYPE:
                                    value = ((JFormattedTextField) component).getValue();
                                    break;
                                case Type.DATETIME_TYPE:
                                    value = ((JCalendar) component).getDate();
                                    break;
                                case Type.BOOLEAN_TYPE:
                                    value = Boolean.valueOf(((AbstractButton) component).isSelected());
                                    break;
                                case Type.PERFORMER_TYPE:
                                    value = ((JComboBox) component).getSelectedItem();
                                    break;
                                case Type.SCHEMA_TYPE:
                                    File file = ((JFileSelector) component).getFile();
                                    value = readFile(file);
                                    break;
                                default:
                                    value = null;
                            }
                            param.setValue(value);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(UpdateProcessAttributesDialog.this, "An exception occurred: " + ex.getClass().getName() + ": " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    _exitCode = EXIT_NORMAL;
                    dispose();
                }
            });
            cancel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            addKeyListener(new KeyAdapter() {

                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) dispose();
                }
            });
        }
    }

    private static byte[] readFile(File file) throws IOException {
        byte[] content = null;
        if (file != null) {
            InputStream in = null;
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in = new FileInputStream(file);
                byte[] buf = new byte[BUFFER_SIZE];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                content = out.toByteArray();
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return content;
    }

    private static void clearSchemaTypeValues(Parameter[] parms) {
        for (int i = 0; i < parms.length; i++) {
            Parameter parm = parms[i];
            if (parm.getDataType().getType().getImpliedType().value() == Type.SCHEMA_TYPE) {
                parm.setValue(null);
            }
        }
    }

    private static void write(Writer writer, String key, int value) throws IOException {
        writer.write(key);
        writer.write('=');
        writer.write(String.valueOf(value));
        writer.write(',');
    }

    private static void write(Writer writer, String key, boolean value) throws IOException {
        writer.write(key);
        writer.write('=');
        writer.write(value ? "yes" : "no");
        writer.write(',');
    }

    public UpdateProcessAttributes(UpdateProcessAttributesMetaData metadata) {
        _metadata = metadata;
    }

    public void renderInvocationScript(ToolInvocation ti, Writer writer) throws IOException {
        clearSchemaTypeValues(ti.parameters);
        int n = ti.parameters.length;
        if (n > 0) {
            writer.write("window.open(\"updateProcessAttributes.jsf");
            writer.write("?updProcAttrs:procInstId=");
            writer.write(ti.procInstId);
            writer.write("&updProcAttrs:workItemId=");
            writer.write(ti.workItemId);
            writer.write("&updProcAttrs:toolIndex=");
            writer.write(String.valueOf(ti.toolIndex));
            writer.write("\", \"");
            if (_metadata.getTitle() != null) writer.write(_metadata.getTitle());
            writer.write("\", \"");
            write(writer, "height", _metadata.getHeight());
            write(writer, "width", _metadata.getWidth());
            write(writer, STATUS, _metadata.getStatus());
            write(writer, "toolbar", _metadata.getToolbar());
            write(writer, "menubar", _metadata.getMenubar());
            write(writer, "location", _metadata.getLocation());
            write(writer, "scrollbars", _metadata.getScrollbars());
            writer.write("\");");
        }
    }

    protected int _invokeApplication(ToolInvocation ti) throws InterruptedException, InvocationTargetException {
        clearSchemaTypeValues(ti.parameters);
        return USE_STDIO ? invokeViaSTDIO(ti) : invokeViaSwing(ti);
    }

    private int invokeViaSTDIO(ToolInvocation ti) throws InvocationTargetException {
        int exitCode = EXIT_CANCEL;
        int n = ti.parameters.length;
        if (n > 0) {
            synchronized (System.in) {
                PrintStream stdout = System.out;
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                String title = _metadata.getTitle();
                stdout.println(title != null ? title : "Update Process Attributes");
                for (int i = 0; i < n; i++) {
                    Parameter param = ti.parameters[i];
                    Type type = param.getDataType().getType().getImpliedType();
                    boolean isDate = type.value() == Type.DATETIME_TYPE;
                    boolean isXML = type.value() == Type.SCHEMA_TYPE;
                    stdout.print("Enter value for '");
                    stdout.print(param.getName());
                    stdout.print("' (");
                    stdout.print(type);
                    if (isXML) stdout.print(" - specify XML file to upload");
                    if (isDate) {
                        stdout.print(" - ");
                        stdout.print(DateUtilities.DEFAULT_DATE_FORMAT);
                    }
                    stdout.print(')');
                    Object value = param.getValue();
                    if (value != null) {
                        if (isDate) {
                            value = DateUtilities.getInstance().format((Date) value);
                        }
                        stdout.print(" [");
                        stdout.print(value.toString());
                        stdout.print("]");
                    }
                    stdout.print(": ");
                    stdout.flush();
                    try {
                        Object newValue = stdin.readLine().trim();
                        boolean empty = newValue.equals("");
                        if (empty) newValue = null;
                        if (isXML) {
                            if (!empty) newValue = readFile(new File((String) newValue));
                        } else if (empty) newValue = value;
                        param.setValue(newValue);
                        exitCode = EXIT_NORMAL;
                    } catch (IOException e) {
                        stdout.print("An exception occurred: " + e.getClass().getName() + ": " + e.getMessage());
                        throw new InvocationTargetException(e);
                    }
                }
                System.in.notify();
            }
        }
        return exitCode;
    }

    private int invokeViaSwing(ToolInvocation ti) {
        UpdateProcessAttributesDialog dlg = new UpdateProcessAttributesDialog(ti);
        dlg.setVisible(true);
        return dlg.getExitCode();
    }
}
