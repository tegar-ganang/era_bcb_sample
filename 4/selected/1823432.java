package org.sss.eibs.design;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.sss.eibs.design.jface.AbstractTableProvider;
import org.sss.eibs.design.report.DataDictionaryUtils;
import org.sss.eibs.design.utils.CompileHibernate;
import org.sss.eibs.design.utils.CompileJsf;
import org.sss.eibs.design.utils.CompileOfbiz;
import org.sss.eibs.design.utils.CompileXul;
import org.sss.eibs.design.utils.CompileZul;
import org.sss.module.IModuleManager;
import org.sss.module.eibs.common.Constants;
import org.sss.module.eibs.util.ModuleConstant;
import org.sss.util.ContainerUtils;

/**
 * 全部编译的Shell
 * @author Guangqiang.Yang (latest modification by $Author: hujianxin78728 $)
 * @version $Revision: 710 $ $Date: 2012-04-22 05:13:08 -0400 (Sun, 22 Apr 2012) $
 */
public class ShellCompile extends AbstractShell {

    protected static final Log log = LogFactory.getLog(ShellCompile.class);

    private static final String defaultPath = File.separator.equals("/") ? "/" : "C:\\";

    private static final HashMap<String, String[]> map = new HashMap<String, String[]>();

    static {
        map.put("MySQL", new String[] { "org.gjt.mm.mysql.Driver", "jdbc:mysql://localhost:3306/eibs?useUnicode=true&characterEncoding=utf8" });
        map.put("PostgreSQL", new String[] { "org.postgresql.Driver", "jdbc:postgresql://localhost:5432/eibs" });
        map.put("Oracle", new String[] { "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@localhost:1521:eibs" });
        map.put("HyperSQL", new String[] { "org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:hsql://localhost/eibs" });
    }

    private boolean packageAsEAR = false;

    private int type1;

    private int type2;

    private IModuleManager manager;

    private String transactionName;

    public ShellCompile(Shell parent, IModuleManager manager, int type1, int type2) {
        super(parent);
        this.type1 = type1;
        this.type2 = type2;
        this.manager = manager;
        this.transactionName = manager.getTransactionName();
        createContents();
        setLayout(new FillLayout());
    }

    protected void createContents() {
        setText(MessageFormat.format("Compile to {0} {1} ...", Constants.types1[type1], Constants.types2[type2]));
        setSize(500, 400);
        final Composite composite = new Composite(this, SWT.NONE);
        final Label labelTarget = new Label(composite, SWT.NONE);
        labelTarget.setText("Target name:");
        labelTarget.setBounds(217, 35, 75, 12);
        final Label labelPath = new Label(composite, SWT.WRAP);
        labelPath.setText(defaultPath);
        labelPath.setBounds(243, 86, 248, 57);
        final Button buttonChangePath = new Button(composite, SWT.NONE);
        buttonChangePath.setText("Change path to deploy:");
        buttonChangePath.setBounds(217, 58, 144, 17);
        buttonChangePath.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setText(labelPath, directory("Select directory to deploy..."));
            }
        });
        final Label labelMessage = new Label(composite, SWT.NONE);
        labelMessage.setText("Select transactions to compile:");
        labelMessage.setBounds(8, 11, 225, 13);
        final CheckboxTableViewer checkboxTableViewer = CheckboxTableViewer.newCheckList(composite, SWT.FULL_SELECTION | SWT.BORDER);
        final Table table = checkboxTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setBounds(2, 30, 201, 343);
        AbstractTableProvider provider = new AbstractTableProvider() {

            @Override
            public Object[] getElements(Object inputElement) {
                return manager.listModule(true).toArray();
            }
        };
        checkboxTableViewer.setContentProvider(provider);
        checkboxTableViewer.setLabelProvider(provider);
        checkboxTableViewer.setInput(Collections.EMPTY_LIST);
        checkboxTableViewer.setChecked(manager.getTransactionName(), true);
        final TableColumn tableColumnTransactionName = new TableColumn(table, SWT.NONE);
        tableColumnTransactionName.setWidth(150);
        tableColumnTransactionName.setText("Names");
        final Button checkPackageAsEAR = new Button(composite, SWT.CHECK);
        checkPackageAsEAR.setText("package as EAR");
        checkPackageAsEAR.setSelection(false);
        checkPackageAsEAR.setBounds(374, 58, 117, 16);
        final Button checkIgnoreWarning = new Button(composite, SWT.CHECK);
        checkIgnoreWarning.setText("Ignore warnings");
        checkIgnoreWarning.setBounds(215, 306, 117, 16);
        checkIgnoreWarning.setSelection(true);
        final Combo comboExportType = new Combo(composite, SWT.READ_ONLY);
        comboExportType.setBounds(385, 270, 53, 20);
        comboExportType.setItems(new String[] { "html", "docx", "xlsx", "pptx", "pdf", "xls", "xml" });
        comboExportType.select(0);
        final Button checkGenerateDataDictionary = new Button(composite, SWT.CHECK);
        checkGenerateDataDictionary.setText("Generate data dictionary");
        checkGenerateDataDictionary.setBounds(215, 275, 171, 16);
        checkGenerateDataDictionary.setSelection(true);
        checkGenerateDataDictionary.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                comboExportType.setVisible(checkGenerateDataDictionary.getSelection());
            }
        });
        final Label labelJndiName = new Label(composite, SWT.NONE);
        labelJndiName.setBounds(215, 150, 36, 13);
        labelJndiName.setText("Jndi:");
        final Text textJndiName = new Text(composite, SWT.BORDER);
        textJndiName.setEnabled(false);
        textJndiName.setBounds(257, 145, 117, 21);
        final Label labelType = new Label(composite, SWT.NONE);
        labelType.setBounds(215, 174, 36, 13);
        labelType.setText("Type:");
        final Text textUrl = new Text(composite, SWT.BORDER);
        textUrl.setBounds(257, 195, 225, 21);
        final Combo comboType = new Combo(composite, SWT.READ_ONLY);
        comboType.setBounds(257, 170, 144, 20);
        comboType.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent event) {
                if (map.containsKey(comboType.getText())) textUrl.setText(map.get(comboType.getText())[1]);
            }
        });
        comboType.setItems(map.keySet().toArray(Constants.EMPTY_STRINGS));
        comboType.select(0);
        final Button checkJndi = new Button(composite, SWT.CHECK);
        checkJndi.setBounds(388, 148, 13, 16);
        checkJndi.setSelection(false);
        checkJndi.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean flag = checkJndi.getSelection();
                textJndiName.setEnabled(flag);
                textUrl.setEnabled(!flag);
                comboType.setEnabled(!flag);
                if (flag) {
                    textUrl.setText("");
                    comboType.setText("");
                } else textJndiName.setText("");
            }
        });
        final Label labelUrl = new Label(composite, SWT.NONE);
        labelUrl.setBounds(215, 198, 36, 13);
        labelUrl.setText("URL:");
        final Label labelUsername = new Label(composite, SWT.NONE);
        labelUsername.setBounds(215, 222, 36, 13);
        labelUsername.setText("User:");
        final Text textUsername = new Text(composite, SWT.BORDER);
        textUsername.setBounds(257, 218, 117, 21);
        final Label labelPassword = new Label(composite, SWT.NONE);
        labelPassword.setBounds(215, 246, 56, 13);
        labelPassword.setText("Password:");
        final Text textPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        textPassword.setBounds(277, 242, 190, 21);
        final Combo comboTarget = new Combo(composite, SWT.NONE);
        comboTarget.setBounds(298, 31, 138, 20);
        comboTarget.setItems(manager.listTargets().toArray(Constants.EMPTY_STRINGS));
        comboTarget.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                Object[] target = manager.getTarget(comboTarget.getText());
                labelPath.setText((String) target[0]);
                checkboxTableViewer.setAllChecked(false);
                for (String name : (List<String>) target[1]) checkboxTableViewer.setChecked(name, true);
                checkPackageAsEAR.setSelection((Boolean) target[2]);
                String[] settings = manager.getDataSourceSetting(comboTarget.getText());
                comboType.setText(settings[ModuleConstant.IDX_TYPE]);
                setText(textUrl, settings[ModuleConstant.IDX_URL]);
                setText(textUsername, settings[ModuleConstant.IDX_USERNAME]);
                setText(textPassword, settings[ModuleConstant.IDX_PASSWORD]);
                if (!ContainerUtils.isEmpty(settings[ModuleConstant.IDX_JNDI])) {
                    setText(textJndiName, settings[ModuleConstant.IDX_JNDI]);
                    checkJndi.setSelection(true);
                }
            }
        });
        comboTarget.select(0);
        if (ContainerUtils.isEmpty(comboTarget.getText())) comboTarget.setText("eIBS");
        final Button buttonApply = new Button(composite, SWT.NONE);
        buttonApply.setSelection(true);
        buttonApply.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
        buttonApply.setText("Compile");
        buttonApply.setBounds(408, 304, 75, 21);
        buttonApply.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                Object[] elements = checkboxTableViewer.getCheckedElements();
                if (elements != null && elements.length > 0) {
                    FileUtils.deleteQuietly(manager.getBuildPath());
                    ArrayList<String> list = new ArrayList();
                    for (Object element : elements) list.add((String) element);
                    manager.saveTarget(comboTarget.getText(), labelPath.getText(), checkPackageAsEAR.getSelection(), list);
                    String[] settings = new String[ModuleConstant.IDX_MAX];
                    settings[ModuleConstant.IDX_TYPE] = comboType.getText();
                    settings[ModuleConstant.IDX_USERNAME] = textUsername.getText();
                    settings[ModuleConstant.IDX_PASSWORD] = textPassword.getText();
                    if (checkJndi.getSelection()) {
                        settings[ModuleConstant.IDX_URL] = "";
                        settings[ModuleConstant.IDX_DRIVER] = "";
                        settings[ModuleConstant.IDX_JNDI] = textJndiName.getText();
                    } else {
                        settings[ModuleConstant.IDX_URL] = textUrl.getText();
                        settings[ModuleConstant.IDX_DRIVER] = map.get(comboType.getText())[0];
                        settings[ModuleConstant.IDX_JNDI] = "";
                    }
                    manager.setDatasource(comboTarget.getText(), settings);
                    ModuleConstant.ignoreWarning = checkIgnoreWarning.getSelection();
                    packageAsEAR = checkPackageAsEAR.getSelection();
                    if (checkGenerateDataDictionary.getSelection()) {
                        File file = new File(manager.getBuildPath(), comboTarget.getText() + "." + comboExportType.getText());
                        file.getParentFile().mkdirs();
                        DataDictionaryUtils.generate(manager, file.getAbsolutePath(), comboExportType.getText());
                    }
                    compileAllManager(list.toArray(Constants.EMPTY_STRINGS), labelPath.getText(), comboTarget.getText());
                }
            }
        });
        final Button buttonReset = new Button(composite, SWT.NONE);
        buttonReset.setText("Reset");
        buttonReset.setBounds(217, 342, 75, 21);
        buttonReset.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                checkboxTableViewer.setAllChecked(false);
                checkboxTableViewer.setChecked(manager.getTransactionName(), true);
            }
        });
        final Button buttonSelectAll = new Button(composite, SWT.NONE);
        buttonSelectAll.setText("Select All");
        buttonSelectAll.setBounds(313, 342, 75, 21);
        buttonSelectAll.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                checkboxTableViewer.setAllChecked(true);
            }
        });
        final Button buttonClose = new Button(composite, SWT.NONE);
        buttonClose.setBounds(408, 342, 75, 21);
        buttonClose.setText("Close");
        buttonClose.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                ShellCompile.this.close();
            }
        });
    }

    protected void copyFiles(File srcDir, File destHome) throws IOException {
        FileUtils.copyDirectory(srcDir, new File(destHome, srcDir.getName()));
    }

    private boolean copyFolder(String targetPath, String name) throws IOException {
        String prefix = String.format("%s.%s.", Constants.names1[type1], Constants.names2[type2]);
        if (type2 == Constants.TYPE_JSF_DOJO || type2 == Constants.TYPE_ZUL) {
            if (targetPath == null || "".equals(targetPath)) return false;
            File cfgPath = new File(Thread.currentThread().getContextClassLoader().getResource("log4j.properties").getFile()).getParentFile();
            File namedCfgPath = new File(cfgPath, name);
            File warPath;
            if (packageAsEAR) {
                File earPath = new File(targetPath, name + ".ear");
                if (earPath.exists()) FileUtils.forceDelete(earPath);
                warPath = new File(earPath, name + ".war");
                File metaInfoPath = new File(earPath, "META-INF");
                copyFile(namedCfgPath, "application.xml", prefix, metaInfoPath);
                copyFileByProperties(namedCfgPath, "package.properties", prefix, earPath);
            } else {
                warPath = new File(targetPath, name);
                if (warPath.exists()) FileUtils.forceDelete(warPath);
            }
            File webInfoPath = new File(warPath, "WEB-INF");
            File classesPath = new File(webInfoPath, "classes");
            File libPath = new File(webInfoPath, "lib");
            if (type1 == Constants.TYPE_HIBERNATE && type2 == Constants.TYPE_ZUL) {
                copyFileByProperties(namedCfgPath, "resource.properties", prefix, warPath);
                copyFile(namedCfgPath, "eIBS.xml", prefix, classesPath);
                copyFile(namedCfgPath, "web.xml", prefix, webInfoPath);
                copyFile(namedCfgPath, "log4j.xml", prefix, classesPath);
            }
            if (type1 == Constants.TYPE_HIBERNATE) CompileHibernate.processResources(manager, warPath, classesPath, libPath);
            if (type2 == Constants.TYPE_ZUL) {
                CompileZul.processResources(manager, warPath, classesPath, libPath);
                copyFileStream(classesPath, "zul.properties");
                copyFileStream(classesPath, "zul_en_US.properties");
                copyFileStream(classesPath, "zul_zh_CN.properties");
            }
            copyFileStream(classesPath, "commons-logging.properties");
            copyFileStream(classesPath, "container.properties");
            copyFileStream(classesPath, "container_en_US.properties");
            copyFileStream(classesPath, "container_zh_CN.properties");
        }
        return true;
    }

    private final void copyFileByProperties(File namedCfgPath, String propertiesFileName, String prefix, File packagePath) throws IOException {
        File resourceFile = copyFile(namedCfgPath, propertiesFileName, prefix, null);
        if (resourceFile.exists()) {
            Properties p = new Properties();
            p.load(new FileInputStream(resourceFile));
            for (Object key : p.keySet()) {
                String targetPath = (String) key;
                String sourcePath = (String) p.get(key);
                File sourceFile = sourcePath.startsWith("$") ? new File(manager.getHomePath(), sourcePath.substring(1)) : new File(sourcePath);
                File targetFile = targetPath.startsWith("\\.") ? packagePath : new File(packagePath, targetPath.split("\\.")[0]);
                if (sourceFile.exists()) {
                    if (sourceFile.isDirectory()) FileUtils.copyDirectory(sourceFile, targetFile, FileFilterUtils.makeSVNAware(null)); else FileUtils.copyFileToDirectory(sourceFile, targetFile);
                }
            }
        }
    }

    private final File copyFile(File namedCfgPath, String name, String prefix, File targetPath) throws IOException {
        File file = new File(namedCfgPath, name);
        if (!file.exists()) copyFileStream(prefix + name, file);
        if (file.exists() && targetPath != null) FileUtils.copyFile(file, new File(targetPath, name));
        return file;
    }

    private final void copyFileStream(File file, String name) throws IOException {
        copyFileStream(name, new File(file, name));
    }

    private final void copyFileStream(String name, File file) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (is != null) ContainerUtils.copy(is, new FileOutputStream(file));
    }

    protected void compileAllManager(String[] elements, String targetPath, String name) {
        try {
            switch(type2) {
                case Constants.TYPE_XUL:
                    CompileXul.compileAllManager(manager, elements);
                    break;
                case Constants.TYPE_JSF_DOJO:
                    CompileJsf.compileAllManager(manager, elements);
                    break;
                case Constants.TYPE_ZUL:
                    CompileZul.compileAllManager(manager, elements);
                    break;
            }
            switch(type1) {
                case Constants.TYPE_OFBIZ:
                    CompileOfbiz.compileAllManager(manager, elements);
                    break;
                case Constants.TYPE_HIBERNATE:
                    CompileHibernate.compileAllManager(manager, elements);
                    break;
            }
            if (copyFolder(targetPath, name)) MessageDialog.openInformation(this, "OK", "Compile completed !!");
            manager.save();
            resetAllChanged();
            ShellCompile.this.close();
        } catch (Exception e) {
            log.info("Compile error.", e);
            error(String.format("%s\r\n%s", e.getClass().getName(), e.getMessage()));
        } finally {
            manager.chain(transactionName);
        }
    }
}
