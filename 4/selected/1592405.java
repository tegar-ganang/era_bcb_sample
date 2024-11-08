package fiswidgets.fistemplate;

import java.io.*;
import fiswidgets.fisgui.*;
import fiswidgets.fisutils.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.table.*;
import javax.swing.table.AbstractTableModel;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class FisTemplateWriter extends FisFrame implements ActionListener {

    private final String DTD_document = "fiscmd.dtd";

    private final String DTD_publicID = "-//Fiswidgets//XML Template Document 1.0//EN";

    private Document document;

    private JTabbedPane tab_pane;

    private CmdPanel cmd_panel;

    private StringPanel string_panel;

    private static final int WIDTH = 500;

    private static final Dimension SCROLL_SIZE = new Dimension(WIDTH, 300);

    private TemplateTable table = new TemplateTable(this);

    private JMenuBar menubar;

    private static final byte ACTION_LOAD = 0;

    private static final byte ACTION_SAVE = 1;

    private static final byte ACTION_EXIT = 2;

    private static final byte ACTION_REMOVE = 3;

    private static final byte ACTION_CLEAR = 4;

    private static final byte ACTION_CLEAR_ALL = 5;

    private static final byte ACTION_HELP = 6;

    private static final byte ACTION_ABOUT = 7;

    public FisTemplateWriter() {
        setTitle("FisTemplateWriter");
        menubar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem load = new JMenuItem("Load template");
        load.addActionListener(this);
        load.setActionCommand(Byte.toString(ACTION_LOAD));
        fileMenu.add(load);
        JMenuItem save = new JMenuItem("Save template");
        save.addActionListener(this);
        save.setActionCommand(Byte.toString(ACTION_SAVE));
        fileMenu.add(save);
        JMenuItem clear_all = new JMenuItem("Clear all");
        clear_all.addActionListener(this);
        clear_all.setActionCommand(Byte.toString(ACTION_CLEAR_ALL));
        fileMenu.add(clear_all);
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(this);
        exit.setActionCommand(Byte.toString(ACTION_EXIT));
        fileMenu.add(exit);
        menubar.add(fileMenu);
        JMenu help = new JMenu("Help");
        JMenuItem help_item = new JMenuItem("Help");
        help_item.addActionListener(this);
        help_item.setActionCommand(Byte.toString(ACTION_HELP));
        help.add(help_item);
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(this);
        about.setActionCommand(Byte.toString(ACTION_ABOUT));
        help.add(about);
        menubar.add(Box.createHorizontalGlue());
        menubar.add(help);
        setJMenuBar(menubar);
        setJMenuBar(menubar);
        FisPanel option_panel = new FisPanel();
        FisPanel button_panel = new FisPanel();
        FisLabel table_label = new FisLabel("Command line options table", option_panel);
        option_panel.newLine();
        JScrollPane scrollPane1 = new JScrollPane(table.option_table);
        scrollPane1.setPreferredSize(SCROLL_SIZE);
        FisComponent.addToPanel(scrollPane1, option_panel);
        option_panel.newLine();
        FisButton remove_button = new FisButton("Remove option", button_panel);
        remove_button.addActionListener(this);
        remove_button.setActionCommand(Byte.toString(ACTION_REMOVE));
        FisButton clear_button = new FisButton("Clear options table", button_panel);
        clear_button.addActionListener(this);
        clear_button.setActionCommand(Byte.toString(ACTION_CLEAR));
        option_panel.addFisPanel(button_panel, 2, 1);
        addFisPanel(option_panel, 1, 1);
        cmd_panel = new CmdPanel();
        string_panel = new StringPanel(table);
        FisPanel editor_panel = new FisPanel();
        tab_pane = new JTabbedPane(JTabbedPane.TOP);
        tab_pane.add("Executable", cmd_panel);
        tab_pane.add("Command line options", string_panel);
        FisComponent.addToPanel(tab_pane, editor_panel);
        addFisPanel(editor_panel, 1, 1);
    }

    public void actionPerformed(ActionEvent e) {
        switch(Byte.parseByte(e.getActionCommand())) {
            case ACTION_LOAD:
                loadFile();
                break;
            case ACTION_SAVE:
                saveFile();
                break;
            case ACTION_EXIT:
                System.exit(0);
                break;
            case ACTION_REMOVE:
                table.removeSelected();
                break;
            case ACTION_CLEAR:
                table.clear();
                break;
            case ACTION_CLEAR_ALL:
                table.clear();
                cmd_panel.clear();
                string_panel.clear();
                break;
            case ACTION_HELP:
                {
                    FisProperties props = new FisProperties();
                    try {
                        props.loadProperties();
                    } catch (Exception ex) {
                        return;
                    }
                    if (!props.hasProperty("BROWSER") || !props.hasProperty("FISDOC_PATH")) return;
                    String browser = System.getProperty("BROWSER");
                    String docpath = System.getProperty("FISDOC_PATH");
                    try {
                        Runtime.getRuntime().exec(browser + " " + docpath + "/FisTemplateWriter.html");
                    } catch (Exception ex) {
                        return;
                    }
                }
                break;
            case ACTION_ABOUT:
                {
                    Dialogs.ShowMessageDialog(this, "FisTemplateWriter\nVersion 0.2\nwritten by Daniel Cunningham\n\nUniversity of Pittsburgh Medical Center (UPMC)", "About");
                }
                break;
        }
    }

    private void saveFile() {
        if (checkOutput()) {
            String dir = System.getProperty("user.dir");
            JFileChooser chooser = new JFileChooser(dir);
            int r = chooser.showSaveDialog(this);
            if (r == JFileChooser.APPROVE_OPTION) {
                String t = chooser.getSelectedFile().getAbsolutePath();
                boolean overwrite = true;
                File output_file = new File(t);
                if (output_file.exists()) {
                    int ans = JOptionPane.showConfirmDialog(this, "The file already exists, overwrite?", "Are you sure?", JOptionPane.YES_NO_OPTION);
                    overwrite = (ans == JOptionPane.YES_OPTION);
                }
                if (overwrite) {
                    try {
                        document = buildDocument();
                        TransformerFactory tFactory = TransformerFactory.newInstance();
                        Transformer transformer = tFactory.newTransformer();
                        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, DTD_publicID);
                        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DTD_document);
                        formatXML(document.getDocumentElement(), "    ");
                        document.getDocumentElement().normalize();
                        DOMSource source = new DOMSource(document);
                        StreamResult result = new StreamResult(output_file);
                        transformer.transform(source, result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void formatXML(Element root, String tab) {
        NodeList children = root.getChildNodes();
        Node[] nodes = new Node[children.getLength()];
        for (int i = 0; i < children.getLength(); i++) nodes[i] = children.item(i);
        for (int i = 0; i < nodes.length; i++) {
            root.insertBefore(document.createTextNode("\n" + tab), nodes[i]);
            if (nodes[i] instanceof Element) formatXML((Element) nodes[i], "  " + tab);
        }
        root.appendChild(document.createTextNode("\n" + tab.substring(0, tab.length() - 2)));
    }

    private Document buildDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        CmdInfo cmd_info = cmd_panel.getInfo();
        cmd_info.addElement(document);
        table.saveFile(document);
        return document;
    }

    private void loadFile() {
        String dir = System.getProperty("user.dir");
        JFileChooser chooser = new JFileChooser(dir);
        int r = chooser.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File template_file = chooser.getSelectedFile();
            try {
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                docBuilder.setErrorHandler(new XmlErrorHandler());
                docBuilder.setEntityResolver(new XmlTemplateEntityResolver());
                Document doc = docBuilder.parse(template_file);
                doc.getDocumentElement().normalize();
                Element root = doc.getDocumentElement();
                CmdInfo cmd_info = new CmdInfo();
                cmd_info.loadElement(root);
                cmd_panel.setPanel(cmd_info);
                table.loadFile(root);
            } catch (Exception e) {
                Dialogs.ShowErrorDialog(this, "could not load template " + template_file.getAbsolutePath() + ".\n" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean checkOutput() {
        return true;
    }

    public static void main(String[] args) {
        FisTemplateWriter wt = new FisTemplateWriter();
        wt.pack();
        wt.setVisible(true);
    }
}

class CmdPanel extends FisPanel implements ActionListener {

    private FisTextField name;

    private FisTextArea synopsis;

    private FisTextArea usage;

    private FisTextField title;

    private FisTextField helpdoc;

    private FisTextField logfile;

    private FisTextField author;

    private FisRadioButton use_cmd_dir;

    private FisRadioButton use_fisprop;

    private FisTextField cmd_dir;

    private FisTextField fisprop;

    private FisFileBrowser browse;

    private static final byte ACTION_DEFAULT = 0;

    private static final byte ACTION_PROPERTY = 1;

    private static final byte ACTION_DIR = 2;

    public CmdPanel() {
        super();
        FisPanel name_panel = new FisPanel();
        FisPanel opts_panel = new FisPanel();
        FisPanel cmd_path_panel = new FisPanel("Executable directory");
        name = new FisTextField("Executable name", "", name_panel);
        name.setMinimumSize(name.getPreferredSize());
        addFisPanel(name_panel, 2, 1);
        newLine();
        FisLabel area_label2 = new FisLabel("Synopsis", this);
        newLine();
        synopsis = new FisTextArea(7, 40, this);
        synopsis.setMinimumSize(synopsis.getPreferredSize());
        newLine();
        FisLabel area_label = new FisLabel("Usage", this);
        newLine();
        usage = new FisTextArea(7, 40, this);
        usage.setMinimumSize(usage.getPreferredSize());
        newLine();
        title = new FisTextField("Application title", "", opts_panel);
        opts_panel.newLine();
        helpdoc = new FisTextField("Html help document", "", opts_panel);
        opts_panel.newLine();
        logfile = new FisTextField("Default logfile name", "", opts_panel);
        addFisPanel(opts_panel, 2, 1);
        newLine();
        FisButton default_button = new FisButton("Use defaults", this);
        default_button.addActionListener(this);
        default_button.setActionCommand(Byte.toString(ACTION_DEFAULT));
        newLine();
        FisPanel author_panel = new FisPanel();
        author = new FisTextField("Author", "", author_panel);
        addFisPanel(author_panel, 1, 1);
        newLine();
        FisRadioButtonGroup cmd_group = new FisRadioButtonGroup();
        use_cmd_dir = new FisRadioButton("Explicitly specify directory", false, cmd_path_panel);
        use_cmd_dir.addActionListener(this);
        use_cmd_dir.setActionCommand(Byte.toString(ACTION_DIR));
        cmd_group.add(use_cmd_dir);
        cmd_dir = new FisTextField("", cmd_path_panel);
        cmd_dir.setMinimumSize(cmd_dir.getPreferredSize());
        browse = new FisFileBrowser(cmd_path_panel);
        browse.attachTo(cmd_dir);
        cmd_path_panel.newLine();
        use_fisprop = new FisRadioButton("Use property from .fisproperties", true, cmd_path_panel);
        use_fisprop.addActionListener(this);
        use_fisprop.setActionCommand(Byte.toString(ACTION_PROPERTY));
        cmd_group.add(use_fisprop);
        fisprop = new FisTextField("", cmd_path_panel);
        setProperty(use_fisprop.isSelected());
        addFisPanel(cmd_path_panel, 2, 1);
    }

    public void setPanel(CmdInfo cmd_info) {
        name.setText(cmd_info.name);
        synopsis.setText(cmd_info.synopsis);
        usage.setText(cmd_info.usage);
        title.setText(cmd_info.title);
        helpdoc.setText(cmd_info.helpdoc);
        logfile.setText(cmd_info.logfile);
        if (cmd_info.use_property) {
            fisprop.setText(cmd_info.path);
            cmd_dir.setText("");
            use_fisprop.doClick();
        } else {
            fisprop.setText("");
            cmd_dir.setText(cmd_info.path);
            use_cmd_dir.doClick();
        }
        author.setText(cmd_info.author);
    }

    public CmdInfo getInfo() {
        CmdInfo info = new CmdInfo();
        info.name = name.getText().trim();
        info.synopsis = synopsis.getText().trim();
        info.usage = usage.getText().trim();
        info.title = title.getText().trim();
        info.helpdoc = helpdoc.getText().trim();
        info.logfile = logfile.getText().trim();
        info.use_property = use_fisprop.isSelected();
        if (info.use_property) {
            info.path = fisprop.getText().trim();
        } else {
            info.path = cmd_dir.getText().trim();
        }
        info.author = author.getText().trim();
        return info;
    }

    private void setProperty(boolean use_property) {
        fisprop.setEditable(use_property);
        cmd_dir.setEditable(!use_property);
        browse.setEnabled(!use_property);
    }

    public void actionPerformed(ActionEvent e) {
        switch(Byte.parseByte(e.getActionCommand())) {
            case ACTION_DEFAULT:
                setDefaults();
                break;
            case ACTION_PROPERTY:
            case ACTION_DIR:
                setProperty(use_fisprop.isSelected());
                break;
        }
    }

    public void clear() {
        name.setText("");
        synopsis.setText("");
        usage.setText("");
        title.setText("");
        helpdoc.setText("");
        logfile.setText("");
        cmd_dir.setText("");
        fisprop.setText("");
        author.setText("");
    }

    private void setDefaults() {
        if (!name.getText().trim().equals("")) {
            title.setText(name.getText());
            helpdoc.setText(Utils.stripSuffixes(name.getText()) + ".html");
            logfile.setText(Utils.stripSuffixes(name.getText()) + ".log");
        }
    }
}

class StringPanel extends FisPanel implements ActionListener {

    private FisTextField name;

    private FisComboBox required;

    private FisTextArea description;

    private FisTextArea default_value;

    private FisComboBox param_types;

    private FisComboBox semantics;

    private FisTextField param_name;

    private TemplateTable table;

    private ParameterTable parameter_table;

    private static final Dimension SCROLL_SIZE = new Dimension(300, 200);

    private static final String[] REQURIRED_CHOICES = { "Yes", "No", "Sometimes" };

    private static final String[] SEMANTICS = { "Input", "Output", "Option", "Stdin", "Stdout", "Stderr" };

    private static final byte ACTION_ADD = 0;

    private static final byte ACTION_EDIT = 1;

    private static final byte ACTION_CLEAR = 2;

    public StringPanel(TemplateTable table) {
        super();
        parameter_table = new ParameterTable();
        this.table = table;
        MouseListener mouseListener2 = new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setPanel();
                }
            }
        };
        table.option_table.addMouseListener(mouseListener2);
        FisPanel grand_panel = new FisPanel();
        FisPanel flag_panel = new FisPanel("Option flag");
        FisPanel description_panel = new FisPanel("Option settings");
        FisPanel opts_panel = new FisPanel();
        FisPanel button_panel = new FisPanel();
        FisPanel params_panel = new FisPanel("Option parameters");
        name = new FisTextField("Name", "", flag_panel);
        FisLabel area_label = new FisLabel("Description", description_panel);
        description_panel.newLine();
        description = new FisTextArea(7, 40, description_panel);
        description_panel.newLine();
        required = new FisComboBox("Required", REQURIRED_CHOICES, opts_panel);
        opts_panel.newLine();
        semantics = new FisComboBox("Semantic type", SEMANTICS, opts_panel);
        description_panel.addFisPanel(opts_panel, 2, 1);
        JScrollPane scrollPane1 = new JScrollPane(parameter_table.param_table);
        scrollPane1.setPreferredSize(SCROLL_SIZE);
        FisComponent.addToPanel(scrollPane1, params_panel);
        FisButton params_clear_button = new FisButton("Clear", params_panel);
        params_clear_button.addActionListener(this);
        params_clear_button.setActionCommand(Byte.toString(ACTION_CLEAR));
        FisButton add_button = new FisButton("Add option", button_panel);
        add_button.addActionListener(this);
        add_button.setActionCommand(Byte.toString(ACTION_ADD));
        FisButton edit_button = new FisButton("Update option", button_panel);
        edit_button.addActionListener(this);
        edit_button.setActionCommand(Byte.toString(ACTION_EDIT));
        addFisPanel(flag_panel, 1, 1);
        newLine();
        addFisPanel(params_panel, 1, 1);
        newLine();
        addFisPanel(description_panel, 1, 1);
        newLine();
        addFisPanel(button_panel, 1, 1);
    }

    public void setPanel() {
        ParameterInfo info = table.getSelectedInfo();
        name.setText(info.name);
        description.setText(info.description);
        required.setSelectedIndex(info.required);
        semantics.setSelectedIndex(info.semantic_type);
        parameter_table.setData(info.options);
    }

    public ParameterInfo getInfo() {
        ParameterInfo info = new ParameterInfo();
        info.name = name.getText().trim();
        info.description = description.getText().trim();
        info.description_lines = description.getLineCount();
        info.required = required.getSelectedIndex();
        info.semantic_type = semantics.getSelectedIndex();
        info.options = parameter_table.getData();
        info.setParamInfo();
        return info;
    }

    public void actionPerformed(ActionEvent e) {
        switch(Byte.parseByte(e.getActionCommand())) {
            case ACTION_ADD:
                table.addEntry(getInfo());
                break;
            case ACTION_EDIT:
                table.updateSelected(getInfo());
                break;
            case ACTION_CLEAR:
                parameter_table.reset();
                break;
        }
    }

    public void clear() {
        name.setText("");
        description.setText("");
        parameter_table.reset();
    }
}

class CmdInfo {

    public String name;

    public String synopsis;

    public String usage;

    public String title;

    public String helpdoc;

    public String path;

    public boolean use_property;

    public String logfile;

    public String author;

    private static final String NAME_ATTR = "name";

    private static final String HELPDOC_ATTR = "helpdoc";

    private static final String TITLE_ATTR = "title";

    private static final String PATHTYPE_ATTR = "pathtype";

    private static final String PATH_ATTR = "path";

    private static final String LOGFILE_ATTR = "logfile";

    private static final String USAGE_ATTR = "usage";

    private static final String SYNOPSIS_ATTR = "synopsis";

    private static final String AUTHOR_ATTR = "author";

    public void addElement(Document doc) {
        Element element = doc.createElement("Command");
        element.setAttribute(NAME_ATTR, name);
        element.setAttribute(HELPDOC_ATTR, helpdoc);
        element.setAttribute(TITLE_ATTR, title);
        element.setAttribute(PATHTYPE_ATTR, (use_property) ? "property" : "path");
        element.setAttribute(PATH_ATTR, path);
        element.setAttribute(LOGFILE_ATTR, logfile);
        element.setAttribute(USAGE_ATTR, usage);
        element.setAttribute(SYNOPSIS_ATTR, synopsis);
        element.setAttribute(AUTHOR_ATTR, author);
        doc.appendChild(element);
    }

    public void loadElement(Element element) {
        name = element.getAttribute(NAME_ATTR);
        helpdoc = element.getAttribute(HELPDOC_ATTR);
        title = element.getAttribute(TITLE_ATTR);
        use_property = element.getAttribute(PATHTYPE_ATTR).equals("property");
        path = element.getAttribute(PATH_ATTR);
        logfile = element.getAttribute(LOGFILE_ATTR);
        usage = element.getAttribute(USAGE_ATTR);
        synopsis = element.getAttribute(SYNOPSIS_ATTR);
        author = element.getAttribute(AUTHOR_ATTR);
    }
}
