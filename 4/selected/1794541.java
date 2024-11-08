package com.nhncorp.cubridqa.navigation;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import com.nhncorp.cubridqa.CUBRIDAdvisor;
import com.nhncorp.cubridqa.cases.manager.CaseManagerView;
import com.nhncorp.cubridqa.console.ConsoleAgent;
import com.nhncorp.cubridqa.console.bo.ConsoleBO;
import com.nhncorp.cubridqa.console.util.CommandUtil;
import com.nhncorp.cubridqa.listener.NavigationSelectAdaptor;
import com.nhncorp.cubridqa.listener.NewWizardSelectAdaptor;
import com.nhncorp.cubridqa.model.Case;
import com.nhncorp.cubridqa.utils.Closer;
import com.nhncorp.cubridqa.utils.CubridqaEnvImport;
import com.nhncorp.cubridqa.utils.FileDeleteUtil;
import com.nhncorp.cubridqa.utils.FileUtil;
import com.nhncorp.cubridqa.utils.ITeam;
import com.nhncorp.cubridqa.utils.PropertiesUtil;

/**
 * The composite is used to navigate scenarios.
 * A composite with a navigation tree which is used to navigate
 * categories of scenarios.There are four essential categories of
 * scenarios(SQL,SHELL,SITE and MEDIUM).The tab will analyze
 * qa_repository structure and record it into a XML file.
 * @ClassName: FunctionTab
 * @date 2009-9-2
 * @version V1.0 
 * Copyright (C) www.nhn.com
 */
public class FunctionTab extends Composite {

    private Menu dbsMenu, pdbsMenu;

    private MenuItem chooseDBMenuItem;

    private MenuItem choosePDBMenuItem;

    private Tree tree;

    private String path;

    private static String localPath = PropertiesUtil.getValue("local.path");

    private String xmlFilePath = PropertiesUtil.getValue("local.path") + "scenario.xml";

    private String rootNodeName = PropertiesUtil.getValue("rootnode.name");

    private NavigationView view;

    private MenuItem newMenuItem;

    private MenuItem radiobuttonMenuItem, radiobuttonPMenuItem;

    public static final String DB_SELECTED = "/db__selected__.properties";

    /**
	 * Create the composite
	 * 
	 */
    public FunctionTab(Composite parent, int style, NavigationView view) {
        super(parent, style);
        final GridLayout gridLayout = new GridLayout();
        setLayout(gridLayout);
        this.view = view;
        createFunction(this);
    }

    @Override
    protected void checkSubclass() {
    }

    /**
	 * 
	 * @Title: createFunction
	 * @Description:Create navigation tree, add select listener and context
	 *                     menu.There are six actions in context menu,create
	 *                     scenario or category,delete category,import QA
	 *                     repository,refresh tree,choose database for function
	 *                     test and choose database for performance test.
	 * @param @param container
	 */
    private void createFunction(Composite container) {
        tree = new Tree(container, SWT.CHECK);
        GridData layout = new GridData(SWT.FILL, SWT.FILL, true, true);
        tree.setLayoutData(layout);
        final Menu menu = new Menu(tree);
        tree.setMenu(menu);
        tree.setFocus();
        newMenuItem = new MenuItem(menu, SWT.NONE);
        newMenuItem.setText("New");
        NewWizardSelectAdaptor newWizardSelectAdaptor = new NewWizardSelectAdaptor();
        newWizardSelectAdaptor.setTree(tree);
        newWizardSelectAdaptor.setView(view);
        newMenuItem.addSelectionListener(newWizardSelectAdaptor);
        final MenuItem importMenuItem = new MenuItem(menu, SWT.NONE);
        importMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                DirectoryDialog dia = new DirectoryDialog(getShell());
                dia.setMessage("Please select the root folder");
                dia.setText("Select folder");
                dia.setFilterPath(localPath);
                path = dia.open();
                if (path != null) {
                    importFunction();
                }
            }
        });
        importMenuItem.setText("Import...");
        if (xmlFilePath != null && !xmlFilePath.equals("")) {
            buildTree();
        }
        if (ITeam.USE_TEAM) {
            buildTeamMenu(menu);
        }
        final MenuItem refreshMenuItem = new MenuItem(menu, SWT.NONE);
        refreshMenuItem.setText("Refresh");
        refreshMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                path = localPath;
                importFunction(false);
            }
        });
        final MenuItem deleteMenuItem = new MenuItem(menu, SWT.NONE);
        deleteMenuItem.setText("Delete");
        deleteMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                TreeItem item = tree.getSelection()[0];
                final File file = FileUtil.getScenarioFile(item);
                if (file.getAbsolutePath().replaceAll("\\\\", "/").equals(localPath + "/scenario/sql") || file.getAbsolutePath().replaceAll("\\\\", "/").equals(localPath + "/scenario/shell") || file.getAbsolutePath().replaceAll("\\\\", "/").equals(localPath + "/scenario/site") || file.getAbsolutePath().replaceAll("\\\\", "/").equals(localPath + "/scenario/medium")) {
                    MessageDialog.openError(getShell(), "Delete error", "Can not delete root node");
                } else {
                    boolean openConfirm = MessageDialog.openConfirm(getShell(), "Are you sure", "Do you really want to delete it?");
                    if (openConfirm) {
                        Thread thread = new Thread(new Runnable() {

                            @SuppressWarnings("static-access")
                            public void run() {
                                Thread deleteThread = new Thread(new Runnable() {

                                    public void run() {
                                        FileDeleteUtil.deleteFileAndFolder(file.getAbsolutePath());
                                        FileUtil.createXml();
                                    }
                                });
                                deleteThread.start();
                                try {
                                    deleteThread.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                view.getSite().getShell().getDisplay().getDefault().asyncExec(new Runnable() {

                                    public void run() {
                                        buildTree();
                                        ConsoleAgent.addMessage("delete " + file.getAbsolutePath() + " finished");
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                }
            }
        });
        if (tree.getItemCount() == 0) {
            newMenuItem.setEnabled(false);
        }
        new MenuItem(menu, SWT.SEPARATOR);
        chooseDBMenuItem = new MenuItem(menu, SWT.CASCADE);
        chooseDBMenuItem.setEnabled(false);
        chooseDBMenuItem.setText("Choose Function DB");
        choosePDBMenuItem = new MenuItem(menu, SWT.CASCADE);
        choosePDBMenuItem.setEnabled(false);
        choosePDBMenuItem.setText("Choose Performance DB");
        resetDatabase();
        resetPDatabase();
        NavigationSelectAdaptor navigationSelectAdaptor = new NavigationSelectAdaptor();
        tree.addSelectionListener(navigationSelectAdaptor);
    }

    public static void setDbDefault(String key, String value, String filePath) {
        String fileName = filePath + "/" + DB_SELECTED;
        System.out.println("fileName=" + fileName);
        PropertiesUtil.setValue(key, value, fileName);
    }

    public static void setDbDefault(String key, String value, TreeItem ti) {
        TreeItem cu = ti;
        if (cu == null) {
            return;
        }
        File file = new File(FileUtil.getScenarioFile(cu).getAbsolutePath() + "/" + DB_SELECTED);
        String fileName = file.getAbsolutePath() + "/" + DB_SELECTED;
        PropertiesUtil.setValue(key, value, fileName);
    }

    /**
	 * 
	 * @Title: getDbDefault
	 * @Description:Get the test database name by the tree item which has been
	 *                  selected.Include function test database and performance
	 *                  test database.
	 * @param @param TreeItem
	 * @param @param isUpstairs
	 * @param @param dbType
	 * @return database name which will be used to test
	 */
    public static String getDbDefault(TreeItem ti, boolean isUpstairs, String dbType) {
        TreeItem cu = ti;
        do {
            if (cu == null) break;
            File file = new File(FileUtil.getScenarioFile(cu).getAbsolutePath() + "/" + DB_SELECTED);
            String value = null;
            if (file.exists()) {
                value = PropertiesUtil.getValue("db_selected", file.getAbsolutePath());
                if ("p".equals(dbType)) {
                    value = PropertiesUtil.getValue("pdb_selected", file.getAbsolutePath());
                }
                if (value != null) {
                    return value;
                }
            }
            cu = cu.getParentItem();
        } while (isUpstairs);
        return null;
    }

    /**
	 * 
	 * @Title: getDbDefault
	 * @Description:Get the test database name by the absolute file path of
	 *                  category. Include function test database and performance
	 *                  test database.
	 * @param @param filePath
	 * @param @param isUpstairs
	 * @param @param dbType
	 * @return database name which will be used to test
	 * @throws
	 */
    public static String getDbDefault(String filePath, boolean isUpstairs, String dbType) {
        String string = filePath;
        string = string.replaceAll("\\\\", "/");
        if (new File(string).isFile()) {
            string = string.substring(0, string.lastIndexOf("/"));
        }
        do {
            if (string == null || string.equals("")) break;
            File file = new File(string + "/" + DB_SELECTED);
            if (file.exists()) {
                String value = PropertiesUtil.getValue("db_selected", file.getAbsolutePath());
                if ("p".equals(dbType)) {
                    value = PropertiesUtil.getValue("pdb_selected", file.getAbsolutePath());
                }
                if (value != null) {
                    return value;
                }
            }
            if (string.indexOf("/") >= 0) {
                string = string.substring(0, string.lastIndexOf("/"));
            } else {
                string = "";
            }
        } while (isUpstairs);
        return null;
    }

    /**
	 * 
	 * @Title: getDbs
	 * @Description:Get all function test database.
	 * @return List<File>
	 */
    public static List<File> getDbs() {
        List<File> rs = new ArrayList<File>();
        String path = localPath + "configuration/Function_Db";
        File dbDir = new File(path);
        if (dbDir.exists() && dbDir.isDirectory()) {
            File[] xmls = dbDir.listFiles();
            for (File f : xmls) {
                if (f.isFile() && f.getAbsolutePath().endsWith(".xml")) {
                    rs.add(f);
                }
            }
        }
        return rs;
    }

    /**
	 * 
	 * @Title: getPDbs
	 * @Description:Get all performance test database.
	 * @return List<File>
	 */
    public static List<File> getPDbs() {
        List<File> rs = new ArrayList<File>();
        String path = localPath + "configuration/Performance_Db";
        File dbDir = new File(path);
        if (dbDir.exists() && dbDir.isDirectory()) {
            File[] xmls = dbDir.listFiles();
            for (File f : xmls) {
                if (f.isFile() && f.getAbsolutePath().endsWith(".xml")) {
                    rs.add(f);
                }
            }
        }
        return rs;
    }

    private void resetDatabase() {
        if (dbsMenu != null) {
            dbsMenu.dispose();
        }
        dbsMenu = new Menu(chooseDBMenuItem);
        chooseDBMenuItem.setMenu(dbsMenu);
        radiobuttonMenuItem = new MenuItem(dbsMenu, SWT.RADIO);
        radiobuttonMenuItem.setText("NO DB selected");
        radiobuttonMenuItem.setSelection(true);
        radiobuttonMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                final TreeItem ti = tree.getSelection()[0];
                File scenarioFile = FileUtil.getScenarioFile(ti);
                deleteDbSelected(ti, null);
                String filePath = scenarioFile.getAbsolutePath().replaceAll("\\\\", "/");
                if (!filePath.endsWith("/")) {
                    filePath = filePath + "/";
                }
                TreeItem parentItem = ti.getParentItem();
                if (parentItem == null) {
                    parentItem = ti;
                }
                String defaultPDB = getDbDefault(parentItem, true, "p");
                if (defaultPDB == null) {
                    defaultPDB = "No db selected";
                }
                CaseManagerView caseManagerView = (CaseManagerView) CUBRIDAdvisor.getView(CaseManagerView.ID);
                caseManagerView.getEc().setText("description   Function Db:No db selected  Performance Db:" + defaultPDB + "   total:" + ConsoleAgent.checkAnswers(filePath).get("caseCount"));
                caseManagerView.getEc().layout(true, true);
            }
        });
        new MenuItem(dbsMenu, SWT.SEPARATOR);
    }

    private void resetPDatabase() {
        if (pdbsMenu != null) {
            pdbsMenu.dispose();
        }
        pdbsMenu = new Menu(choosePDBMenuItem);
        choosePDBMenuItem.setMenu(pdbsMenu);
        radiobuttonPMenuItem = new MenuItem(pdbsMenu, SWT.RADIO);
        radiobuttonPMenuItem.setText("NO DB selected");
        radiobuttonPMenuItem.setSelection(true);
        radiobuttonPMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                final TreeItem ti = tree.getSelection()[0];
                File scenarioFile = FileUtil.getScenarioFile(ti);
                FileDeleteUtil.deleteFile(new File(scenarioFile.getAbsolutePath() + "/" + DB_SELECTED));
                String filePath = scenarioFile.getAbsolutePath().replaceAll("\\\\", "/");
                if (!filePath.endsWith("/")) {
                    filePath = filePath + "/";
                }
                TreeItem parentItem = ti.getParentItem();
                if (parentItem == null) {
                    parentItem = ti;
                }
                String defaultDB = getDbDefault(parentItem, true, null);
                if (defaultDB == null) {
                    defaultDB = "No db selected";
                }
                CaseManagerView caseManagerView = (CaseManagerView) CUBRIDAdvisor.getView(CaseManagerView.ID);
                caseManagerView.getEc().setText("description   Function Db:" + defaultDB + "  Performance Db:No db selected" + "   total:" + ConsoleAgent.checkAnswers(filePath).get("caseCount"));
                caseManagerView.getEc().layout(true, true);
            }
        });
        new MenuItem(pdbsMenu, SWT.SEPARATOR);
    }

    @Deprecated
    private void buildTeamMenu(final Menu menu) {
        new MenuItem(menu, SWT.SEPARATOR);
        MenuItem teamMenuItem = new MenuItem(menu, SWT.CASCADE);
        teamMenuItem.setText("Team");
        Menu teamMenu = new Menu(getShell(), SWT.DROP_DOWN);
        teamMenuItem.setMenu(teamMenu);
        MenuItem commitMenuItem = new MenuItem(teamMenu, SWT.NONE);
        MenuItem updateMenuItem = new MenuItem(teamMenu, SWT.NONE);
        commitMenuItem.setText("Commit");
        updateMenuItem.setText("Update");
        commitMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                localPath = PropertiesUtil.getValue("local.path");
                final File rootFile = FileUtil.getScenarioFile(tree.getSelection()[0]);
                Thread thread = new Thread(new Runnable() {

                    public void run() {
                        Thread commitThread = new Thread(new Runnable() {

                            public void run() {
                                CommandUtil.execute("svn add " + rootFile.getAbsolutePath().replaceAll("\\\\", "/"), new ConsoleBO());
                                CommandUtil.execute("svn commit -m \"\" " + rootFile.getAbsolutePath().replaceAll("\\\\", "/") + " --non-interactive --username " + PropertiesUtil.getValue("svnuser") + " --password " + PropertiesUtil.getValue("svnpassword"), new ConsoleBO());
                            }
                        });
                        commitThread.start();
                        try {
                            commitThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        });
        updateMenuItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                localPath = PropertiesUtil.getValue("local.path");
                final String absolutePath = FileUtil.getScenarioFile(tree.getSelection()[0]).getAbsolutePath();
                Thread thread = new Thread(new Runnable() {

                    @SuppressWarnings("static-access")
                    public void run() {
                        Thread updateThread = new Thread(new Runnable() {

                            public void run() {
                                CommandUtil.execute("svn up -r HEAD " + absolutePath.replaceAll("\\\\", "/") + " --non-interactive --username " + PropertiesUtil.getValue("svnuser") + " --password " + PropertiesUtil.getValue("svnpassword"), new ConsoleBO());
                            }
                        });
                        updateThread.start();
                        try {
                            updateThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        view.getSite().getShell().getDisplay().getDefault().asyncExec(new Runnable() {

                            public void run() {
                                path = localPath;
                                importFunction(false);
                            }
                        });
                    }
                });
                thread.start();
            }
        });
    }

    /**
	 * 
	 * @Title: importFunction
	 * @Description:Import QA repository.
	 * @return void
	 */
    public void importFunction() {
        view.getSite().getShell().getDisplay().getDefault().asyncExec(new Runnable() {

            public void run() {
                importFunction(true);
            }
        });
    }

    /**
	 * 
	 * @Title: importFunction
	 * @Description:Create a description XML file for QA repository
	 *                     structure.The description file will be created in the
	 *                     QA repository folder and it's name is 'scenario.xml'.
	 *                     Record absolute file path of QA repository into the
	 *                     profile(a properties file) of QA tool.
	 * @param @param importVar(Does need to import)
	 * @return void
	 */
    public void importFunction(boolean importVar) {
        String shellPath = path + File.separator + "scenario" + File.separator + "shell";
        File shellFile = new File(shellPath);
        if (shellFile.exists() && shellFile.isDirectory()) {
            if (FileUtil.isLinux()) {
                shellFile.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        if (pathname.isDirectory() && pathname.getName().endsWith("_win")) {
                            FileDeleteUtil.deleteFileAndFolder(pathname.getAbsolutePath());
                        }
                        return false;
                    }
                });
            } else {
                shellFile.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        if (pathname.isDirectory() && !pathname.getName().endsWith("_win")) {
                            FileDeleteUtil.deleteFileAndFolder(pathname.getAbsolutePath());
                        }
                        return false;
                    }
                });
            }
        }
        File f = new File(path);
        Element e = FileUtil.createDesc(f);
        Document d = DocumentHelper.createDocument();
        d.add(e);
        FileUtil.addProcedure(d);
        File descFile = new File(FilenameUtils.concat(path, "scenario.xml"));
        xmlFilePath = descFile.getPath();
        OutputStream os = null;
        try {
            os = new FileOutputStream(descFile);
            String str = d.asXML();
            os.write(str.getBytes("utf8"));
        } catch (Exception e1) {
            System.out.println(e1.getStackTrace());
        } finally {
            Closer.close(os);
        }
        if (importVar) {
            if (true == CubridqaEnvImport.importVar(path)) {
                String proppath = "";
                if (path.endsWith("/")) {
                    proppath = path + "qatool_bin/qamanager/properties/local.properties";
                } else {
                    proppath = path + "/qatool_bin/qamanager/properties/local.properties";
                }
                if (path.endsWith("/")) {
                    PropertiesUtil.setValue("local.path", path.replaceAll("\\\\", "/").replaceAll("\\:", ":"), proppath);
                } else {
                    PropertiesUtil.setValue("local.path", path.replaceAll("\\\\", "/").replaceAll("\\:", ":") + "/", proppath);
                }
                String[] strings = path.replaceAll("\\\\", "/").split("/");
                PropertiesUtil.setValue("rootnode.name", strings[strings.length - 1], proppath);
                String message = "Import Environment Variables OK!\r\n";
                message += "Please restart your qatool";
                MessageDialog.openInformation(null, "Import Env Var OK!", message);
            }
            ;
        } else {
            buildTree();
            if (tree.getItemCount() > 0) {
                newMenuItem.setEnabled(true);
            }
        }
        File configurationTemplate = new File(FilenameUtils.concat(path, "configuration_template"));
        File configuration = new File(FilenameUtils.concat(path, "configuration"));
        if (!configuration.exists()) {
            configuration.mkdir();
        }
        File[] configurationFiles = configuration.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return !".svn".equalsIgnoreCase(name);
            }
        });
        if (configurationFiles.length == 0) {
            File[] configurations = configurationTemplate.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return !".svn".equalsIgnoreCase(name);
                }
            });
            for (File file : configurations) {
                try {
                    if (file.isFile()) {
                        FileUtils.copyFileToDirectory(file, configuration);
                    } else {
                        FileUtils.copyDirectoryToDirectory(file, configuration);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        String propertiesPath = path;
        if (!propertiesPath.endsWith("/")) {
            propertiesPath += "/";
        }
        propertiesPath += "qatool_bin/qamanager/properties/local.properties";
        File propFile = new File(propertiesPath);
        if (!propFile.exists()) {
            MessageDialog.openError(null, "PROPERTIES_PATH  ERROR!", "PROPERTIES_PATH environment variable error, not found local.properties, please set it again!");
        }
    }

    /**
	 * 
	 * @Title: buildTree
	 * @Description:Read description file of QA repository and create navigation
	 *                   tree.
	 * @return void
	 */
    public void buildTree() {
        try {
            SAXReader reader = new SAXReader();
            final TreeItem functionSqlItem, functionShellItem, functionSiteItem, functionMediumItem;
            tree.removeAll();
            Document document = reader.read(new File(xmlFilePath));
            functionSqlItem = new TreeItem(tree, SWT.NONE);
            functionSqlItem.setText("SQL");
            functionShellItem = new TreeItem(tree, SWT.NONE);
            functionShellItem.setText("SHELL");
            functionSiteItem = new TreeItem(tree, SWT.NONE);
            functionSiteItem.setText("SITE");
            functionMediumItem = new TreeItem(tree, SWT.NONE);
            functionMediumItem.setText("MEDIUM");
            Node functionSqlNode = document.selectSingleNode("/" + rootNodeName + "/scenario/sql");
            xmlWalk((Element) functionSqlNode, functionSqlItem);
            Node functionShellNode = document.selectSingleNode("/" + rootNodeName + "/scenario/shell");
            xmlWalk((Element) functionShellNode, functionShellItem);
            Node functionSiteNode = document.selectSingleNode("/" + rootNodeName + "/scenario/site");
            xmlWalk((Element) functionSiteNode, functionSiteItem);
            Node functionMediumNode = document.selectSingleNode("/" + rootNodeName + "/scenario/medium");
            xmlWalk((Element) functionMediumNode, functionMediumItem);
            if (tree.getItems().length > 0) {
                tree.addMenuDetectListener(new MenuDetectListener() {

                    public void menuDetected(MenuDetectEvent e) {
                        final TreeItem ti = tree.getSelection()[0];
                        TreeItem parentItem = ti.getParentItem();
                        if (parentItem == null) {
                            chooseDBMenuItem.setEnabled(true);
                            choosePDBMenuItem.setEnabled(true);
                            buildDbMenu(ti);
                        } else if (Case.categoryList.contains(parentItem.getText().toLowerCase())) {
                            String defaultDB = getDbDefault(parentItem, true, null);
                            String pdefaultDB = getDbDefault(parentItem, true, "p");
                            if (defaultDB != null) {
                                chooseDBMenuItem.setEnabled(false);
                            } else {
                                chooseDBMenuItem.setEnabled(true);
                            }
                            if (pdefaultDB != null) {
                                choosePDBMenuItem.setEnabled(false);
                            } else {
                                choosePDBMenuItem.setEnabled(true);
                            }
                            buildDbMenu(ti);
                        } else {
                            chooseDBMenuItem.setEnabled(false);
                            choosePDBMenuItem.setEnabled(false);
                        }
                    }

                    /**
					 * 
					 * @Title: buildDbMenu
					 * @Description:Create choose database menu.Only in level 1
					 *                     and level 2 categories can choose
					 *                     database.If select test database in
					 *                     level 1 category can overwrite
					 *                     the previous selection in level 2
					 *                     category.
					 * @param @param Choose database tree item
					 * @return void
					 */
                    private void buildDbMenu(final TreeItem ti) {
                        List<File> fs = getDbs();
                        List<File> pfs = getPDbs();
                        if (fs.size() == 0) {
                            resetDatabase();
                        }
                        if (pfs.size() == 0) {
                            resetPDatabase();
                        }
                        resetDatabase();
                        resetPDatabase();
                        String value = getDbDefault(ti, true, null);
                        for (File f : fs) {
                            final MenuItem menuItem = new MenuItem(dbsMenu, SWT.RADIO);
                            String dbName = "";
                            String name = f.getName();
                            if (f.isFile() && name.endsWith(".xml") && !name.endsWith("consolescheduledefaultdb2008.xml")) {
                                dbName = f.getName().substring(0, f.getName().lastIndexOf(".xml"));
                                menuItem.setText(dbName);
                                if (dbName.equalsIgnoreCase(value)) {
                                    menuItem.setSelection(true);
                                    radiobuttonMenuItem.setSelection(false);
                                }
                                menuItem.addSelectionListener(new SelectionAdapter() {

                                    @Override
                                    public void widgetSelected(SelectionEvent e) {
                                        TreeItem parentItem = ti.getParentItem();
                                        if (parentItem == null) {
                                            TreeItem[] children = ti.getItems();
                                            for (TreeItem t : children) {
                                                if (getDbDefault(t, false, null) != null) {
                                                    if (MessageDialog.openConfirm(getShell(), "Are you sure?", "Default DB is selected in sub categories, your selection will override it, are you sure to continue?")) {
                                                        for (TreeItem t2 : children) {
                                                            if (getDbDefault(t2, false, null) != null) {
                                                                deleteDbSelected(t2, null);
                                                            }
                                                        }
                                                    } else {
                                                        return;
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        File scenarioFile = FileUtil.getScenarioFile(ti);
                                        MenuItem selectedDb = (MenuItem) e.widget;
                                        setDbDefault("db_selected", selectedDb.getText(), scenarioFile.getAbsolutePath());
                                        String filePath = scenarioFile.getAbsolutePath().replaceAll("\\\\", "/");
                                        if (!filePath.endsWith("/")) {
                                            filePath = filePath + "/";
                                        }
                                        CaseManagerView caseManagerView = (CaseManagerView) CUBRIDAdvisor.getView(CaseManagerView.ID);
                                        String dbPDefault = getDbDefault(ti, true, "p");
                                        if (dbPDefault == null) {
                                            dbPDefault = "NO DB selected";
                                        }
                                        caseManagerView.getEc().setText("description   Function Db:" + getDbDefault(ti, true, null) + "  Peformance Db:" + dbPDefault + "   total:" + ConsoleAgent.checkAnswers(filePath).get("caseCount"));
                                        caseManagerView.getEc().layout(true, true);
                                    }
                                });
                            }
                        }
                        value = getDbDefault(ti, true, "p");
                        for (File f : pfs) {
                            final MenuItem menuItem = new MenuItem(pdbsMenu, SWT.RADIO);
                            String dbName = "";
                            String name = f.getName();
                            if (f.isFile() && name.endsWith(".xml") && !name.endsWith("consolescheduledefaultdb2008.xml")) {
                                dbName = f.getName().substring(0, f.getName().lastIndexOf(".xml"));
                                menuItem.setText(dbName);
                                if (dbName.equalsIgnoreCase(value)) {
                                    menuItem.setSelection(true);
                                    radiobuttonPMenuItem.setSelection(false);
                                }
                                menuItem.addSelectionListener(new SelectionAdapter() {

                                    @Override
                                    public void widgetSelected(SelectionEvent e) {
                                        TreeItem parentItem = ti.getParentItem();
                                        if (parentItem == null) {
                                            TreeItem[] children = ti.getItems();
                                            for (TreeItem t : children) {
                                                if (getDbDefault(t, false, "p") != null) {
                                                    if (MessageDialog.openConfirm(getShell(), "Are you sure?", "Default DB is selected in sub categories, your selection will override it, are you sure to continue?")) {
                                                        for (TreeItem t2 : children) {
                                                            if (getDbDefault(t2, false, "p") != null) {
                                                                deleteDbSelected(t2, "p");
                                                            }
                                                        }
                                                    } else {
                                                        return;
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        File scenarioFile = FileUtil.getScenarioFile(ti);
                                        MenuItem selectedDb = (MenuItem) e.widget;
                                        setDbDefault("pdb_selected", selectedDb.getText(), scenarioFile.getAbsolutePath());
                                        String filePath = scenarioFile.getAbsolutePath().replaceAll("\\\\", "/");
                                        if (!filePath.endsWith("/")) {
                                            filePath = filePath + "/";
                                        }
                                        CaseManagerView caseManagerView = (CaseManagerView) CUBRIDAdvisor.getView(CaseManagerView.ID);
                                        String dbPDefault = getDbDefault(ti, true, "p");
                                        if (dbPDefault == null) {
                                            dbPDefault = "NO DB selected";
                                        }
                                        caseManagerView.getEc().setText("description   Function Db:" + getDbDefault(ti, true, null) + "  Peformance Db:" + dbPDefault + "   total:" + ConsoleAgent.checkAnswers(filePath).get("caseCount"));
                                        caseManagerView.getEc().layout(true, true);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            if (new File(xmlFilePath).exists()) {
                MessageDialog.openError(null, "FILE FORMAT ERROR!", "Please check follow step:" + System.getProperty("line.separator") + "1. Please make sure you set a correct local.path in local.properties file" + System.getProperty("line.separator") + "2. Please make sure all of your directory name can be xml node name(ex: can not use nunmber as the first charactor and can not include blank)" + System.getProperty("line.separator") + "3. Please make sure there are 'sql','shell','medium','site' directory in your scenario folder");
            }
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * @Title: deleteDbSelected
	 * @Description:Remove selected database profile
	 * @param @param selected tree item
	 * @param @param dbType
	 * @return void
	 * @throws
	 */
    private void deleteDbSelected(TreeItem t2, String dbType) {
        File file = new File(FileUtil.getScenarioFile(t2).getAbsolutePath() + "/" + DB_SELECTED);
        if (file.exists()) {
            String key = "db_selected";
            if ("p".equals(dbType)) {
                key = "pdb_selected";
            }
            PropertiesUtil.removeProperty(file.getAbsolutePath(), key);
        }
    }

    private void xmlWalk(Element element, TreeItem item) {
        for (int i = 0; i < element.nodeCount(); i++) {
            Node node = element.node(i);
            TreeItem treeItem = new TreeItem(item, SWT.NONE);
            treeItem.setText(node.getName());
            if (node instanceof Element && !endWalk((Element) node)) {
                xmlWalk((Element) node, treeItem);
            }
        }
    }

    private boolean endWalk(Element element) {
        boolean end = false;
        for (int i = 0; i < element.nodeCount(); i++) {
            Node node = element.node(i);
            if (node.getName().toLowerCase().indexOf("cases") >= 0) {
                end = true;
            }
        }
        return end;
    }

    /**
	 * 
	 * @Title: freshTree
	 * @Description:Refresh navigation tree
	 * @return void
	 */
    public void freshTree(Case ca) {
        TreeItem[] items = tree.getItems();
        for (TreeItem ti : items) {
            selectNode(ti, ca);
        }
    }

    private boolean selectNode(TreeItem ti, Case ca) {
        String fileName = ca.getFileName();
        String text = ti.getText();
        if (text != null && text.equals(fileName)) {
            ti.setExpanded(true);
            return true;
        }
        TreeItem[] items = ti.getItems();
        for (TreeItem ch : items) {
            if (selectNode(ch, ca)) {
                return true;
            }
        }
        return false;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Tree getTree() {
        return tree;
    }

    public MenuItem getNewMenuItem() {
        return newMenuItem;
    }
}
