package saadadb.admintool.panels.editors;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import saadadb.admintool.AdminTool;
import saadadb.admintool.components.AdminComponent;
import saadadb.admintool.components.LoaderConfigChooser;
import saadadb.admintool.components.RenameButton;
import saadadb.admintool.components.SaveButton;
import saadadb.admintool.components.ToolBarPanel;
import saadadb.admintool.components.XMLButton;
import saadadb.admintool.components.mapper.ClassMapperPanel;
import saadadb.admintool.components.mapper.CoordSysMapperPanel;
import saadadb.admintool.components.mapper.DispersionMapperPanel;
import saadadb.admintool.components.mapper.ExtAttMapperPanel;
import saadadb.admintool.components.mapper.ExtensionTextFieldPanel;
import saadadb.admintool.components.mapper.MappingTextfieldPanel;
import saadadb.admintool.components.mapper.PositionErrorMapperPanel;
import saadadb.admintool.components.mapper.PositionMapperPanel;
import saadadb.admintool.dialogs.DialogConfName;
import saadadb.admintool.dialogs.DialogConfigFileChooser;
import saadadb.admintool.dialogs.DialogFileChooser;
import saadadb.admintool.panels.EditPanel;
import saadadb.admintool.panels.tasks.DataLoaderPanel;
import saadadb.admintool.tree.VoDataProductTree;
import saadadb.admintool.utils.HelpDesk;
import saadadb.admintool.windows.TextSaver;
import saadadb.api.SaadaDB;
import saadadb.collection.Category;
import saadadb.command.ArgsParser;
import saadadb.database.Database;
import saadadb.exceptions.FatalException;
import saadadb.exceptions.QueryException;
import saadadb.exceptions.SaadaException;
import saadadb.sqltable.SQLTable;
import saadadb.sqltable.Table_Saada_VO_Capabilities;
import saadadb.util.Messenger;
import saadadb.util.RegExp;
import saadadb.vo.registry.Capability;
import saadadb.vo.registry.Record;
import saadadb.vo.tap.TapServiceManager;

/**
 * TODO : specialize this class by inheritance
 * @author michel
 * @version $Id$
 *
 */
public class MappingKWPanel extends EditPanel {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private int category;

    private String confName = "Default";

    private JPanel category_panel;

    private JPanel entry_panel;

    private GridBagConstraints globalGridConstraint;

    private GridBagConstraints e_globalGridConstraint;

    private String last_saved = "";

    private JPanel editorPanel;

    private JPanel e_editorPanel;

    private MappingTextfieldPanel nameMapper;

    private MappingTextfieldPanel e_nameMapper;

    private MappingTextfieldPanel ignoredMapper;

    private MappingTextfieldPanel e_ignoredMapper;

    private ExtensionTextFieldPanel extensionMapper;

    private DispersionMapperPanel dispersionMapper;

    private ClassMapperPanel classMapper;

    private ExtAttMapperPanel extAttMapper;

    private ExtAttMapperPanel e_extAttMapper;

    private CoordSysMapperPanel cooSysMapper;

    private PositionMapperPanel positionMapper;

    private PositionMapperPanel e_positionMapper;

    private PositionErrorMapperPanel positionErrorMapper;

    private PositionErrorMapperPanel e_positionErrorMapper;

    protected LoaderConfigChooser configChooser;

    /**
	 * @param rootFrame
	 * @param title
	 * @param category
	 * @param ancestor
	 */
    public MappingKWPanel(AdminTool rootFrame, String title, int category, String ancestor) {
        super(rootFrame, title, null, ancestor);
        this.category = category;
    }

    /**
	 * 
	 */
    private void addCategoryPanel() {
        if (category_panel == null) {
            category_panel = new JPanel(new GridBagLayout());
            category_panel.setBackground(LIGHTBACKGROUND);
            GridBagConstraints ccs = new GridBagConstraints();
            ccs.gridx = 0;
            ccs.gridy = 0;
            ccs.weightx = 0.33;
            ccs.anchor = GridBagConstraints.CENTER;
            JLabel ds = AdminComponent.getPlainLabel("<HTML><A HREF=>Form Reset</A>");
            ds.addMouseListener(new MouseAdapter() {

                public void mouseReleased(MouseEvent e) {
                    reset(false);
                }
            });
            category_panel.add(ds, ccs);
            ccs.gridx++;
            ds = AdminComponent.getPlainLabel("<HTML><A HREF=>Loader Parameters</A>");
            ds.setToolTipText("Show dataloader parameters matching the current configuration.");
            ds.addMouseListener(new MouseAdapter() {

                public void mouseReleased(MouseEvent e) {
                    if (checkParams()) {
                        ArgsParser args_parser = getArgsParser();
                        if (args_parser != null) {
                            String[] args = args_parser.getArgs();
                            String summary = "";
                            for (int i = 0; i < args.length; i++) {
                                summary += args[i] + "\n";
                            }
                            AdminComponent.showCopiableInfo(rootFrame, summary, "Loader Parameters");
                        }
                    }
                }
            });
            category_panel.add(ds, ccs);
            ccs.gridx++;
            ds = AdminComponent.getPlainLabel("<HTML><A HREF=>Data Sample</A> ");
            ds.setToolTipText("Show dataloader parameters matching the current configuration.");
            ds.addMouseListener(new MouseAdapter() {

                public void mouseReleased(MouseEvent e) {
                    DialogFileChooser fcd = new DialogFileChooser();
                    String filename = fcd.open(rootFrame, true);
                    if (filename.length() == 0) {
                        return;
                    }
                    rootFrame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    JFrame window = new JFrame(new File(filename).getName());
                    VoDataProductTree vot;
                    vot = new VoDataProductTree(window, "ext/keywords (drag & drop to the loader configuration panel)", filename);
                    rootFrame.setCursor(Cursor.getDefaultCursor());
                    vot.drawTree(new Dimension(300, 500));
                    vot.setPreferredSize(new Dimension(300, 500));
                    window.add(vot);
                    window.pack();
                    window.setVisible(true);
                }
            });
            category_panel.add(ds, ccs);
            globalGridConstraint.weightx = 1;
            editorPanel.add(category_panel, globalGridConstraint);
        }
    }

    /**
	 * @return
	 */
    public int getCategory() {
        return category;
    }

    /**
	 * @return
	 */
    public String getExtension() {
        if (extensionMapper != null) {
            return extensionMapper.getText();
        }
        return null;
    }

    /**
	 * 
	 */
    private void addSourcePanel() {
        if (entry_panel == null) {
            entry_panel = new JPanel();
            entry_panel.setBackground(LIGHTBACKGROUND);
            entry_panel.setBorder(BorderFactory.createTitledBorder("Table Entry Mapping"));
            entry_panel.setLayout(new GridBagLayout());
            entry_panel.add(AdminComponent.getHelpLabel(HelpDesk.get(HelpDesk.ENTRY_MAPPING)));
            e_globalGridConstraint.gridy++;
            e_nameMapper = new MappingTextfieldPanel(this, "Entry Name mapping", true);
            entry_panel.add(e_nameMapper.container, globalGridConstraint);
            globalGridConstraint.gridy++;
            e_positionMapper = new PositionMapperPanel(this, "Position mapping", true);
            entry_panel.add(e_positionMapper.container, globalGridConstraint);
            globalGridConstraint.gridy++;
            e_positionErrorMapper = new PositionErrorMapperPanel(this, "Position Error mapping", true);
            entry_panel.add(e_positionErrorMapper.container, globalGridConstraint);
            globalGridConstraint.gridy++;
            e_extAttMapper = new ExtAttMapperPanel(this, "Extended Attribute Selector", Category.ENTRY);
            entry_panel.add(e_extAttMapper.container, globalGridConstraint);
            globalGridConstraint.gridy++;
            e_ignoredMapper = new MappingTextfieldPanel(this, "Ignored Kws Selector", true);
            entry_panel.add(e_ignoredMapper.container, globalGridConstraint);
            globalGridConstraint.gridy++;
            editorPanel.add(entry_panel, globalGridConstraint);
            this.updateUI();
            return;
        }
    }

    /**
	 * @return
	 */
    public ArgsParser getArgsParser() {
        ArrayList<String> params = new ArrayList<String>();
        if (extensionMapper != null) {
            if (extensionMapper.getText().length() > 0) {
                params.add("-extension=" + extensionMapper.getText());
            }
        }
        if (classMapper != null && classMapper.hasMapping()) {
            if (classMapper.getParam().length() > 0) {
                params.add(classMapper.getParam());
            } else {
                showInputError(rootFrame, "A class mapping mode is selected but there is no class name given: ignored");
            }
        }
        if (ignoredMapper != null) {
            if (ignoredMapper.getText().length() > 0) {
                params.add("-ignore=" + ignoredMapper.getText());
            }
        }
        switch(this.category) {
            case Category.MISC:
                params.add("-category=misc");
                break;
            case Category.IMAGE:
                params.add("-category=image");
                break;
            case Category.SPECTRUM:
                params.add("-category=spectrum");
                break;
            case Category.TABLE:
                params.add("-category=table");
                break;
            case Category.FLATFILE:
                params.add("-category=flatfile");
                break;
            default:
                break;
        }
        if (nameMapper != null && nameMapper.getText().length() > 0) {
            params.add("-name=" + nameMapper.getText());
        }
        if (extAttMapper != null) {
            params.addAll(extAttMapper.getParams());
        }
        if (cooSysMapper != null) {
            params.addAll(cooSysMapper.getParams());
        }
        if (positionMapper != null) {
            params.addAll(positionMapper.getParams());
        }
        if (positionErrorMapper != null) {
            params.addAll(positionErrorMapper.getParams());
        }
        if (dispersionMapper != null) {
            params.addAll(dispersionMapper.getParams());
        }
        if (e_nameMapper != null && e_nameMapper.getText().length() > 0) {
            params.add("-ename=" + e_nameMapper.getText());
        }
        if (e_extAttMapper != null) {
            params.addAll(e_extAttMapper.getParams());
        }
        if (e_positionErrorMapper != null) {
            params.addAll(e_positionErrorMapper.getParams());
        }
        if (e_positionMapper != null) {
            params.addAll(e_positionMapper.getParams());
        }
        if (e_ignoredMapper != null) {
            if (e_ignoredMapper.getText().length() > 0) {
                params.add("-eignore=" + e_ignoredMapper.getText());
            }
        }
        try {
            ArgsParser retour;
            retour = new ArgsParser((String[]) (params.toArray(new String[0])));
            retour.setName(confName);
            return retour;
        } catch (FatalException e) {
            Messenger.trapFatalException(e);
        }
        return null;
    }

    /**
	 * 
	 */
    public void buildMiscPanel() {
        classMapper = new ClassMapperPanel(this, "Class Mapping", false);
        classMapper.setCollapsed(false);
        editorPanel.add(classMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extensionMapper = new ExtensionTextFieldPanel(this, "Extension to Load", false);
        editorPanel.add(extensionMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        nameMapper = new MappingTextfieldPanel(this, "Instance Name Mapping", false);
        editorPanel.add(nameMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        ignoredMapper = new MappingTextfieldPanel(this, "Ignored Kws Selector", false);
        editorPanel.add(ignoredMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extAttMapper = new ExtAttMapperPanel(this, "Extended Attribute Selector", category);
        editorPanel.add(extAttMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        this.updateUI();
    }

    /**
	 * 
	 */
    public void buildFlatfilePanel() {
        nameMapper = new MappingTextfieldPanel(this, "Instance Name mapping", false);
        editorPanel.add(nameMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extAttMapper = new ExtAttMapperPanel(this, "Extended Attribute Selector", category);
        editorPanel.add(extAttMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        this.updateUI();
    }

    /**
	 * 
	 */
    public void buildImagePanel() {
        classMapper = new ClassMapperPanel(this, "Class Mapping", false);
        classMapper.setCollapsed(false);
        editorPanel.add(classMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extensionMapper = new ExtensionTextFieldPanel(this, "Extension to Load", false);
        editorPanel.add(extensionMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        nameMapper = new MappingTextfieldPanel(this, "Instance Name Mapping", false);
        editorPanel.add(nameMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        cooSysMapper = new CoordSysMapperPanel(this, "Coordinate System Mapping", false);
        editorPanel.add(cooSysMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        positionMapper = new PositionMapperPanel(this, "Position Mapping", false);
        editorPanel.add(positionMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        positionErrorMapper = new PositionErrorMapperPanel(this, "Position Error Mapping", false);
        editorPanel.add(positionErrorMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extAttMapper = new ExtAttMapperPanel(this, "Extended Attribute Selector", category);
        editorPanel.add(extAttMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        ignoredMapper = new MappingTextfieldPanel(this, "Ignored Kws Selector", false);
        editorPanel.add(ignoredMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        this.updateUI();
    }

    /**
	 * 
	 */
    public void buildSpectraPanel() {
        classMapper = new ClassMapperPanel(this, "Class Mapping", false);
        classMapper.setCollapsed(false);
        editorPanel.add(classMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extensionMapper = new ExtensionTextFieldPanel(this, "Extension to Load", false);
        editorPanel.add(extensionMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        nameMapper = new MappingTextfieldPanel(this, "Instance Name Mapping", false);
        editorPanel.add(nameMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        cooSysMapper = new CoordSysMapperPanel(this, "Coordinate System Mapping", false);
        editorPanel.add(cooSysMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        positionMapper = new PositionMapperPanel(this, "Position Mapper", false);
        editorPanel.add(positionMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        positionErrorMapper = new PositionErrorMapperPanel(this, "Position Error Mapping", false);
        editorPanel.add(positionErrorMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        dispersionMapper = new DispersionMapperPanel(this, "Spectral Range Mapping", false);
        editorPanel.add(dispersionMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extAttMapper = new ExtAttMapperPanel(this, "Extended Attribute Selector", category);
        editorPanel.add(extAttMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        ignoredMapper = new MappingTextfieldPanel(this, "Ignored Kws Selector", false);
        editorPanel.add(ignoredMapper.container, globalGridConstraint);
        this.updateUI();
    }

    /**
	 * 
	 */
    public void buildTablePanel() {
        classMapper = new ClassMapperPanel(this, "Class Mapping", false);
        classMapper.setCollapsed(false);
        editorPanel.add(classMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extensionMapper = new ExtensionTextFieldPanel(this, "Extension to Load", false);
        editorPanel.add(extensionMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        nameMapper = new MappingTextfieldPanel(this, "Instance Name Mapping", false);
        editorPanel.add(nameMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        positionErrorMapper = new PositionErrorMapperPanel(this, "Position Error Mapping", false);
        editorPanel.add(positionErrorMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        extAttMapper = new ExtAttMapperPanel(this, "Extended Attribute Selector", category);
        editorPanel.add(extAttMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        ignoredMapper = new MappingTextfieldPanel(this, "Ignored Kws Selector", false);
        editorPanel.add(ignoredMapper.container, globalGridConstraint);
        globalGridConstraint.gridy++;
        this.addSourcePanel();
        this.updateUI();
    }

    /**
	 * @param parser
	 */
    public void loadConfig(ArgsParser parser) {
        try {
            if (parser != null) {
                this.last_saved = parser.toString();
                setConfName(parser.getName());
                String param = null;
                if (classMapper != null) {
                    classMapper.setText(parser.getMappingType(), parser.getClassName());
                }
                if (extensionMapper != null && (param = parser.getExtension()) != null) {
                    this.extensionMapper.setText(param);
                }
                if (ignoredMapper != null) {
                    ignoredMapper.setParams(parser.getIgnoredAttributes());
                }
                if (e_ignoredMapper != null) {
                    e_ignoredMapper.setParams(parser.getEntryIgnoredAttributes());
                }
                if (nameMapper != null) {
                    nameMapper.setParams(parser.getNameComponents());
                }
                if (e_nameMapper != null) {
                    e_nameMapper.setParams(parser.getEntryNameComponents());
                }
                if (extAttMapper != null) {
                    extAttMapper.setParams(parser);
                }
                if (e_extAttMapper != null) {
                    e_extAttMapper.setParams(parser);
                }
                if (cooSysMapper != null) {
                    cooSysMapper.setParams(parser);
                }
                if (positionMapper != null) {
                    positionMapper.setParams(parser);
                }
                if (positionErrorMapper != null) {
                    positionErrorMapper.setParams(parser);
                }
                if (dispersionMapper != null) {
                    dispersionMapper.setParams(parser);
                }
            } else {
                setConfName(null);
            }
        } catch (Exception e) {
            Messenger.printStackTrace(e);
            JOptionPane.showMessageDialog(rootFrame, e.toString(), "Error while loading configuration", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * @param name
	 */
    protected void setConfName(String name) {
        confName = name;
        setSelectedResource(confName, null);
    }

    /**
	 * Clear all widgets
	 */
    public void reset(boolean keep_ext) {
        this.last_saved = "";
        this.confName = null;
        if (nameMapper != null) nameMapper.reset();
        ;
        if (e_nameMapper != null) e_nameMapper.reset();
        ;
        if (ignoredMapper != null) ignoredMapper.reset();
        if (e_ignoredMapper != null) e_ignoredMapper.reset();
        if (extAttMapper != null) extAttMapper.reset();
        if (e_extAttMapper != null) e_extAttMapper.reset();
        if (cooSysMapper != null) cooSysMapper.reset();
        if (positionMapper != null) positionMapper.reset();
        if (positionErrorMapper != null) positionErrorMapper.reset();
        if (dispersionMapper != null) dispersionMapper.reset();
        if (!keep_ext && extensionMapper != null) extensionMapper.setText("");
        if (classMapper != null) classMapper.reset();
    }

    /**
	 * 
	 */
    public void rename() {
        try {
            ArgsParser ap = this.getArgsParser();
            if (checkParams()) {
                FileOutputStream fos = null;
                ObjectOutputStream out = null;
                String name = ap.getClassName();
                if (name == null || name.length() == 0 || name.equalsIgnoreCase("null")) {
                    name = "NewConfig";
                }
                while (true) {
                    DialogConfName dial = new DialogConfName(rootFrame, "Configuration Name", name);
                    dial.pack();
                    dial.setLocationRelativeTo(rootFrame);
                    dial.setVisible(true);
                    String prefix = null;
                    prefix = Category.explain(this.category);
                    name = dial.getTyped_name();
                    if (name == null) {
                        return;
                    } else if (name.equalsIgnoreCase("null")) {
                        AdminComponent.showFatalError(rootFrame, "Wrong config name.");
                    } else {
                        String filename = SaadaDB.getRoot_dir() + Database.getSepar() + "config" + Database.getSepar() + prefix + "." + name + ".config";
                        if ((new File(filename)).exists() && AdminComponent.showConfirmDialog(rootFrame, "Loader configuration <" + name + "> for \"" + prefix + "\" already exists.\nOverwrite it?") == false) {
                            ap.setName(name);
                        } else {
                            ap.setName(name);
                            this.setConfName(name);
                            fos = new FileOutputStream(SaadaDB.getRoot_dir() + Database.getSepar() + "config" + Database.getSepar() + prefix + "." + name + ".config");
                            out = new ObjectOutputStream(fos);
                            out.writeObject(ap);
                            out.close();
                            if (this.ancestor.equals(DATA_LOADER)) {
                                rootFrame.activePanel(DATA_LOADER);
                                ((DataLoaderPanel) (rootFrame.getActivePanel())).setConfig(confName);
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Messenger.printStackTrace(ex);
            AdminComponent.showFatalError(this, ex);
            return;
        }
    }

    /**
	 * @param conf_path
	 */
    public void loadConfFile(String conf_path) {
        ArgsParser ap = null;
        if (conf_path.length() != 0) {
            try {
                FileInputStream fis = new FileInputStream(conf_path);
                ObjectInputStream in = new ObjectInputStream(fis);
                ap = (ArgsParser) in.readObject();
                in.close();
                loadConfig(ap);
            } catch (Exception ex) {
                Messenger.printStackTrace(ex);
                showFatalError(rootFrame, ex);
                return;
            }
        }
    }

    /**
	 * 
	 */
    public void save() {
        if ("Default".equals(confName) || confName.equals("") || confName.equalsIgnoreCase("null")) {
            this.rename();
        } else if (!this.hasChanged()) {
            if (this.ancestor.equals(DATA_LOADER)) {
                rootFrame.activePanel(DATA_LOADER);
                ((DataLoaderPanel) (rootFrame.getActivePanel())).setConfig(confName);
            }
            return;
        } else if (checkParams()) {
            ArgsParser ap = this.getArgsParser();
            FileOutputStream fos = null;
            ObjectOutputStream out = null;
            try {
                String prefix = Category.explain(this.category);
                fos = new FileOutputStream(SaadaDB.getRoot_dir() + Database.getSepar() + "config" + Database.getSepar() + prefix + "." + confName + ".config");
                out = new ObjectOutputStream(fos);
                out.writeObject(ap);
                out.close();
                this.last_saved = ap.toString();
                if (this.ancestor.equals(DATA_LOADER)) {
                    rootFrame.activePanel(DATA_LOADER);
                    ((DataLoaderPanel) (rootFrame.getActivePanel())).setConfig(confName);
                }
            } catch (Exception ex) {
                Messenger.printStackTrace(ex);
                AdminComponent.showFatalError(this, ex);
                return;
            }
        }
    }

    /**
	 * @return
	 */
    public boolean hasChanged() {
        return !last_saved.equals(this.getArgsParser().toString());
    }

    /**
	 * Does a basic param checking. Basically, parameter subject to a mapping must not be empty
	 * @return
	 */
    public boolean checkParams() {
        String msg = "";
        if (classMapper != null) {
            classMapper.setOnError(false);
            if (classMapper.hasMapping()) {
                if (classMapper.getText().length() == 0) {
                    classMapper.setOnError(true);
                    msg += "<LI>Empty class name not allowed in this classification mode</LI>";
                } else if (!classMapper.getText().matches(RegExp.CLASSNAME)) {
                    msg += "<LI>Bad class name</LI>";
                }
            }
        }
        if (cooSysMapper != null) {
            cooSysMapper.setOnError(false);
            if (!cooSysMapper.isNo() && cooSysMapper.getText().length() == 0) {
                cooSysMapper.setOnError(true);
                msg += "<LI>Empty coord. system  not allowed in this mapping mode</LI>";
            }
            if (!cooSysMapper.valid()) {
                cooSysMapper.setOnError(true);
                msg += "<LI>Coord System badly formed</LI>";
            }
        }
        if (positionMapper != null) {
            positionMapper.setOnError(false);
            if (!positionMapper.isNo() && positionMapper.getText().length() == 0) {
                cooSysMapper.setOnError(true);
                msg += "<LI>Empty coordinates not allowed in this mapping mode</LI>";
            }
            if (!positionMapper.valid()) {
                positionMapper.setOnError(true);
                msg += "<LI>Coord mapping badly formed</LI>";
            }
        }
        if (e_positionMapper != null) {
            e_positionMapper.setOnError(false);
            if (!e_positionMapper.isNo() && e_positionMapper.getText().length() == 0) {
                e_positionMapper.setOnError(true);
                msg += "<LI>Empty coordinates not allowed in this mapping mode</LI>";
            }
            if (!e_positionMapper.valid()) {
                e_positionMapper.setOnError(true);
                msg += "<LI>Coord mapping badly formed</LI>";
            }
        }
        if (positionErrorMapper != null) {
            positionErrorMapper.setOnError(false);
            if (!positionErrorMapper.isNo() && positionErrorMapper.getText().length() == 0) {
                positionErrorMapper.setOnError(true);
                msg += "<LI>Empty coordinates error not allowed in this mapping mode</LI>";
            }
            if (!positionErrorMapper.valid()) {
                positionErrorMapper.setOnError(true);
                msg += "<LI>Coord error mapping badly formed</LI>";
            }
        }
        if (e_positionErrorMapper != null) {
            e_positionErrorMapper.setOnError(false);
            if (!e_positionErrorMapper.isNo() && e_positionErrorMapper.getText().length() == 0) {
                e_positionErrorMapper.setOnError(true);
                msg += "<LI>Empty coordinate error not allowed in this mapping mode</LI>";
            }
            if (!e_positionErrorMapper.valid()) {
                e_positionErrorMapper.setOnError(true);
                msg += "<LI>Coord error mapping badly formed</LI>";
            }
        }
        if (dispersionMapper != null) {
            dispersionMapper.setOnError(false);
            if (!dispersionMapper.isNo() && dispersionMapper.getText().length() == 0) {
                dispersionMapper.setOnError(true);
                msg += "<LI>Empty spectral dispersion not allowed in this mapping mode</LI>";
            }
            if (!dispersionMapper.valid()) {
                dispersionMapper.setOnError(true);
                msg += "<LI>Spectral dispersion badly formed</LI>";
            }
        }
        if (msg.length() > 0) {
            AdminComponent.showInputError(rootFrame, "<HTML><UL>" + msg);
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void setToolBar() {
        this.initTreePathLabel();
        this.initSelectResourceLabel();
        this.add(new ToolBarPanel(this, true, true, false));
    }

    @Override
    protected void setActivePanel() {
        globalGridConstraint = new GridBagConstraints();
        globalGridConstraint.weightx = 1;
        globalGridConstraint.fill = GridBagConstraints.HORIZONTAL;
        globalGridConstraint.anchor = GridBagConstraints.PAGE_START;
        globalGridConstraint.gridx = 0;
        globalGridConstraint.gridy = 0;
        e_globalGridConstraint = new GridBagConstraints();
        e_globalGridConstraint.weightx = 1;
        e_globalGridConstraint.fill = GridBagConstraints.HORIZONTAL;
        e_globalGridConstraint.anchor = GridBagConstraints.PAGE_START;
        e_globalGridConstraint.gridx = 0;
        e_globalGridConstraint.gridy = 0;
        GridBagConstraints localGridConstraint = new GridBagConstraints();
        localGridConstraint.weightx = 1;
        localGridConstraint.weighty = 1;
        localGridConstraint.fill = GridBagConstraints.BOTH;
        localGridConstraint.anchor = GridBagConstraints.NORTH;
        localGridConstraint.gridx = 0;
        localGridConstraint.gridy = 0;
        JPanel tPanel = this.addSubPanel("Filter Editor");
        editorPanel = new JPanel();
        editorPanel.setBackground(LIGHTBACKGROUND);
        editorPanel.setLayout(new GridBagLayout());
        e_editorPanel = new JPanel();
        e_editorPanel.setBackground(LIGHTBACKGROUND);
        e_editorPanel.setLayout(new GridBagLayout());
        this.addCategoryPanel();
        globalGridConstraint.gridy++;
        if (this.title.equals(MISC_MAPPER)) {
            category = Category.MISC;
        }
        if (this.title.equals(FLATFILE_MAPPER)) {
            category = Category.FLATFILE;
        }
        if (this.title.equals(IMAGE_MAPPER)) {
            category = Category.IMAGE;
        }
        if (this.title.equals(SPECTRUM_MAPPER)) {
            category = Category.SPECTRUM;
        }
        if (this.title.equals(TABLE_MAPPER)) {
            category = Category.TABLE;
        }
        switch(this.category) {
            case Category.MISC:
                this.buildMiscPanel();
                break;
            case Category.IMAGE:
                this.buildImagePanel();
                break;
            case Category.SPECTRUM:
                this.buildSpectraPanel();
                break;
            case Category.TABLE:
                this.buildTablePanel();
                break;
            case Category.FLATFILE:
                this.buildFlatfilePanel();
                break;
        }
        tPanel.add(new JScrollPane(editorPanel), localGridConstraint);
        this.setActionBar();
        this.setConfName("Default");
    }

    protected void setActionBar() {
        this.saveButton = new SaveButton(this);
        this.saveButton.setEnabled(true);
        this.saveAsButton = new RenameButton(this);
        this.saveAsButton.setEnabled(true);
        JPanel tPanel = new JPanel();
        tPanel.setLayout(new GridBagLayout());
        tPanel.setBackground(LIGHTBACKGROUND);
        tPanel.setPreferredSize(new Dimension(1000, 48));
        tPanel.setMaximumSize(new Dimension(1000, 48));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.anchor = GridBagConstraints.PAGE_END;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        tPanel.add(saveButton, c);
        c.gridx++;
        tPanel.add(saveAsButton, c);
        c.gridx++;
        c.weightx = 1;
        tPanel.add(new JLabel(" "), c);
        this.add(tPanel);
    }

    @Override
    public void active() {
    }
}
