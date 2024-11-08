package fiswidgets.fisgui;

import fiswidgets.fisutils.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

class FisSaveButton implements ActionListener, Serializable, PropertyChangeListener {

    protected FisBase g;

    protected JButton save;

    private JTextArea preview;

    private final String XML_desc = "XML serialized file (*.xml)";

    private final String OLD_desc = "Java serialized file (*.gui)";

    private final String XML_suff = ".xml";

    private final String OLD_suff = ".gui";

    private static final String defaultDescription = "Type your description here ~ ";

    public FisSaveButton(FisBase g) {
        this.g = g;
        save = new JButton("Save");
        save.addActionListener(this);
        save.setFont(new Font(g.font, g.fontstyle, g.fontsize));
        save.setToolTipText("Saves the current configuration to a file");
    }

    public void actionPerformed(ActionEvent e) {
        try {
            FisProperties.loadProperties();
        } catch (Exception ex) {
        }
        Properties props = System.getProperties();
        String dir = props.getProperty("GUI_DEFAULTS", "none");
        if (dir.equals("none")) dir = props.getProperty("user.dir");
        JFileChooser chooser = new JFileChooser(dir);
        chooser.addChoosableFileFilter(new FisFileFilter(OLD_suff, OLD_desc));
        chooser.addChoosableFileFilter(new FisFileFilter(XML_suff, XML_desc));
        chooser.setAccessory(buildPreview());
        chooser.addPropertyChangeListener(this);
        int r = chooser.showSaveDialog(g);
        if (r == JFileChooser.APPROVE_OPTION) {
            String t = chooser.getSelectedFile().getAbsolutePath();
            boolean overwrite = true;
            if (!chooser.getFileFilter().getDescription().equals(OLD_desc)) {
                if (!t.endsWith(XML_suff)) t = t + XML_suff;
                if ((new File(t)).exists()) {
                    File file = new File(t);
                    if (file != null) {
                        Document document;
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        try {
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            builder.setErrorHandler(new XmlErrorHandler());
                            builder.setEntityResolver(new XmlEntityResolver());
                            document = builder.parse(file);
                            Element element = document.getDocumentElement();
                            String name = appName(g);
                            if (!(name.equals(element.getAttribute("name").trim()))) {
                                JOptionPane.showMessageDialog(g, "This XML file is not from " + name, "Warning", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (Exception ex) {
                        }
                        document = null;
                        factory = null;
                        int ans = JOptionPane.showConfirmDialog(g, "The file already exists, overwrite?", "Are you sure?", JOptionPane.YES_NO_OPTION);
                        if (ans == JOptionPane.YES_OPTION) {
                            overwrite = true;
                            try {
                                copyFile(t, t + "~");
                            } catch (IOException ex) {
                                int err_ans = JOptionPane.showConfirmDialog(g, "Can't create backup file " + t + "~" + "\nDo you wish to continue anyways?", "Warning", JOptionPane.YES_NO_OPTION);
                                if (err_ans != JOptionPane.YES_OPTION) {
                                    overwrite = false;
                                }
                            }
                        } else overwrite = false;
                    }
                }
                if (overwrite) {
                    if (!preview.getText().equals(defaultDescription)) {
                        g.getAppInfo().description = getComment(preview.getText());
                    }
                    XmlSerializationFactory sf = new XmlSerializationFactory(g, t);
                    try {
                        sf.saveApp();
                    } catch (Exception ex) {
                        Dialogs.ShowErrorDialog(g, "Error saving file!\n" + ex.getMessage());
                    }
                }
            } else {
                if (!t.endsWith(OLD_suff)) t = t + OLD_suff;
                if ((new File(t)).exists()) {
                    int ans = JOptionPane.showConfirmDialog(g, "The file already exists, overwrite?", "Are you sure?", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.YES_OPTION) overwrite = true; else overwrite = false;
                }
                if (overwrite) {
                    SerializationFactory sf = new SerializationFactory(g.componentPanel, t);
                    try {
                        sf.save();
                    } catch (Exception ex) {
                        Dialogs.ShowErrorDialog(g, "Error saving file!");
                    }
                }
            }
        }
    }

    /**
    *  This is the preview panel for the Desktop
    */
    private JPanel buildPreview() {
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.setAlignmentX(Container.CENTER_ALIGNMENT);
        pane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        preview = new JTextArea(9, 12);
        preview.setLineWrap(true);
        preview.setFont(new Font(save.getFont().getName(), Font.PLAIN, save.getFont().getSize() - 1));
        pane.add(preview, BorderLayout.CENTER);
        preview.setText(defaultDescription);
        preview.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                preview.setText(getComment(preview.getText()));
            }
        });
        return pane;
    }

    private String getComment(String comment) {
        StringTokenizer tok = new StringTokenizer(comment, "~");
        if (tok.countTokens() < 2) {
            return comment;
        }
        tok.nextToken();
        String text;
        if (tok.hasMoreTokens()) text = tok.nextToken().trim(); else text = "";
        return text;
    }

    /**
  * Property listener for the JFileChooser save/load preview
  */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            File file = (File) e.getNewValue();
            if (file != null) {
                Document document;
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    builder.setErrorHandler(new XmlErrorHandler());
                    builder.setEntityResolver(new XmlEntityResolver());
                    document = builder.parse(file);
                    preview.setText("");
                    Element element = document.getDocumentElement();
                    preview.append(element.getAttribute("name") + "\n");
                    preview.append(element.getAttribute("date") + "\n");
                    preview.append(element.getAttribute("user") + "\n");
                    preview.append(element.getAttribute("platform") + "\n~\n");
                    preview.append(element.getAttribute("postit"));
                } catch (Exception ex) {
                }
                document = null;
                factory = null;
            }
        }
    }

    /**
   *  Get name from the app
   */
    private String appName(FisBase app) {
        String name = app.getAppName();
        if (name == null) name = app.getClass().getName();
        return name;
    }

    /**
   *  Copy a file from one file to another
   */
    private void copyFile(String input_file, String output_file) throws IOException {
        BufferedReader lr = new BufferedReader(new FileReader(input_file));
        BufferedWriter lw = new BufferedWriter(new FileWriter(output_file));
        String line;
        while ((line = lr.readLine()) != null) {
            lw.write(line, 0, line.length());
            lw.newLine();
            System.out.println(line + "\n");
        }
        lr.close();
        lw.flush();
        lw.close();
    }
}
