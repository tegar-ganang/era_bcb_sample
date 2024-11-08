package com.jeecms.common.developer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ģ�������
 * 
 * <p>
 * �������JEEģ�顣
 * <p>
 * ����JAVA�ࣺaction,dao,dao.impl,manager,manager.impl��
 * �����ļ���action����,spring���ã�ftlҳ�棺list.html,add.html,edit.html��
 * �����֤�ļ���Act-Com_save
 * -validation.xml,Act-Com_edit-validation.xml,Act-Com_update-validation.xml
 * <p>
 * �����õĲ����У�ģ��ʵ������java����ַ�������ļ���ַ��ftlҳ���ַ��
 * 
 * @author liufang
 * 
 */
public class ModuleGenerator {

    private static final Logger log = LoggerFactory.getLogger(ModuleGenerator.class);

    public static final String SPT = File.separator;

    private Properties prop = new Properties();

    private String packName;

    private String fileName;

    private File daoImplFile;

    private File daoFile;

    private File managerFile;

    private File managerImplFile;

    private File actionFile;

    private File springFile;

    private File strutsFile;

    private File pageListFile;

    private File pageEditFile;

    private File pageAddFile;

    private File vldSaveFile;

    private File vldEditFile;

    private File vldUpdateFile;

    private File daoImplTpl;

    private File daoTpl;

    private File managerTpl;

    private File managerImplTpl;

    private File actionTpl;

    private File springTpl;

    private File strutsTpl;

    private File pageListTpl;

    private File pageEditTpl;

    private File pageAddTpl;

    private File vldSaveTpl;

    private File vldEditTpl;

    private File vldUpdateTpl;

    public ModuleGenerator(String packName, String fileName) {
        this.packName = packName;
        this.fileName = fileName;
    }

    @SuppressWarnings("unchecked")
    private void loadProperties() {
        try {
            log.debug("packName=" + packName);
            log.debug("fileName=" + fileName);
            FileInputStream fileInput = new FileInputStream(getFilePath(packName, fileName));
            prop.load(fileInput);
            String entityUp = prop.getProperty("Entity");
            log.debug("entityUp:" + entityUp);
            if (entityUp == null || entityUp.trim().equals("")) {
                log.warn("Entity not specified, exit!");
                return;
            }
            String entityLow = entityUp.substring(0, 1).toLowerCase() + entityUp.substring(1);
            log.debug("entityLow:" + entityLow);
            prop.put("entity", entityLow);
            if (log.isDebugEnabled()) {
                Set ps = prop.keySet();
                for (Object o : ps) {
                    log.debug(o + "=" + prop.get(o));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareFile() {
        String daoImplFilePath = getFilePath(prop.getProperty("dao_impl_p"), prop.getProperty("Entity") + "DaoImpl.java");
        daoImplFile = new File(daoImplFilePath);
        log.debug("daoImplFile:" + daoImplFile.getAbsolutePath());
        String daoFilePath = getFilePath(prop.getProperty("dao_p"), prop.getProperty("Entity") + "Dao.java");
        daoFile = new File(daoFilePath);
        log.debug("daoFile:" + daoFile.getAbsolutePath());
        String managerFilePath = getFilePath(prop.getProperty("manager_p"), prop.getProperty("Entity") + "Mng.java");
        managerFile = new File(managerFilePath);
        log.debug("managerFile:" + managerFile.getAbsolutePath());
        String managerImplFilePath = getFilePath(prop.getProperty("manager_impl_p"), prop.getProperty("Entity") + "MngImpl.java");
        managerImplFile = new File(managerImplFilePath);
        log.debug("managerImplFile:" + managerImplFile.getAbsolutePath());
        String isActionAbstract = prop.getProperty("is_action_abstract");
        String abs = "";
        if (isActionAbstract != null && isActionAbstract.equals("true")) {
            abs = "Abstract";
        }
        String actionFilePath = getFilePath(prop.getProperty("action_p"), prop.getProperty("Entity") + abs + "Act.java");
        actionFile = new File(actionFilePath);
        log.debug("actionFile:" + actionFile.getAbsolutePath());
        String vldSaveFilePath = getFilePath(prop.getProperty("action_p"), prop.getProperty("Entity") + "Act-Com_save-validation.xml");
        vldSaveFile = new File(vldSaveFilePath);
        log.debug("vldSaveFile:" + vldSaveFile.getAbsolutePath());
        String vldEditFilePath = getFilePath(prop.getProperty("action_p"), prop.getProperty("Entity") + "Act-Com_edit-validation.xml");
        vldEditFile = new File(vldEditFilePath);
        log.debug("vldEditFile:" + vldEditFile.getAbsolutePath());
        String vldUpdateFilePath = getFilePath(prop.getProperty("action_p"), prop.getProperty("Entity") + "Act-Com_update-validation.xml");
        vldUpdateFile = new File(vldUpdateFilePath);
        log.debug("vldUpdateFile:" + vldUpdateFile.getAbsolutePath());
        String springFilePath = "src/" + prop.getProperty("spring_file");
        springFile = new File(springFilePath);
        log.debug("springFile:" + springFile.getAbsolutePath());
        String strutsFilePath = "src/" + prop.getProperty("struts_file");
        strutsFile = new File(strutsFilePath);
        log.debug("strutsFile:" + strutsFile.getAbsolutePath());
        if (isActionAbstract != null && isActionAbstract.equals("true")) {
            abs = "/abstract";
        }
        String pagePath = "WebContent/WEB-INF/" + prop.getProperty("config_sys") + abs + "/" + prop.getProperty("config_entity") + "/";
        pageListFile = new File(pagePath + "list.html");
        log.debug("pageListFile:" + pageListFile.getAbsolutePath());
        pageEditFile = new File(pagePath + "edit.html");
        log.debug("pageEditFile:" + pageEditFile.getAbsolutePath());
        pageAddFile = new File(pagePath + "add.html");
        log.debug("pageAddFile:" + pageAddFile.getAbsolutePath());
    }

    private void prepareTemplate() {
        String tplPack = prop.getProperty("template_dir");
        log.debug("tplPack:" + tplPack);
        daoImplTpl = new File(getFilePath(tplPack, "dao_impl.txt"));
        daoTpl = new File(getFilePath(tplPack, "dao.txt"));
        managerImplTpl = new File(getFilePath(tplPack, "manager_impl.txt"));
        managerTpl = new File(getFilePath(tplPack, "manager.txt"));
        actionTpl = new File(getFilePath(tplPack, "action.txt"));
        springTpl = new File(getFilePath(tplPack, "xml_spring_config.txt"));
        strutsTpl = new File(getFilePath(tplPack, "xml_struts_config.txt"));
        pageListTpl = new File(getFilePath(tplPack, "page_list.txt"));
        pageAddTpl = new File(getFilePath(tplPack, "page_add.txt"));
        pageEditTpl = new File(getFilePath(tplPack, "page_edit.txt"));
        vldSaveTpl = new File(getFilePath(tplPack, "validation_save.xml"));
        vldEditTpl = new File(getFilePath(tplPack, "validation_edit.xml"));
        vldUpdateTpl = new File(getFilePath(tplPack, "validation_update.xml"));
    }

    private void writeFile() {
        try {
            if (prop.getProperty("is_dao").equals("true")) {
                FileUtils.writeStringToFile(daoImplFile, readTpl(daoImplTpl));
                FileUtils.writeStringToFile(daoFile, readTpl(daoTpl));
            }
            if (prop.getProperty("is_manager").equals("true")) {
                FileUtils.writeStringToFile(managerImplFile, readTpl(managerImplTpl));
                FileUtils.writeStringToFile(managerFile, readTpl(managerTpl));
            }
            if (prop.getProperty("is_action").equals("true")) {
                FileUtils.writeStringToFile(actionFile, readTpl(actionTpl));
            }
            if (prop.getProperty("is_page").equals("true")) {
                FileUtils.writeStringToFile(pageListFile, readTpl(pageListTpl));
                FileUtils.writeStringToFile(pageAddFile, readTpl(pageAddTpl));
                FileUtils.writeStringToFile(pageEditFile, readTpl(pageEditTpl));
            }
            if (prop.getProperty("is_spring").equals("true")) {
                String springTplStr = readTpl(springTpl);
                String origSpring = FileUtils.readFileToString(springFile, "UTF-8");
                if (origSpring.indexOf(springTplStr) == -1) {
                    String newSpring = origSpring.replaceAll("</beans>", springTplStr + "</beans>");
                    FileUtils.writeStringToFile(springFile, newSpring, "UTF-8");
                }
            }
            if (prop.getProperty("is_struts").equals("true")) {
                String strutsTplStr = readTpl(strutsTpl);
                String origStruts = FileUtils.readFileToString(strutsFile, "UTF-8");
                if (origStruts.indexOf(strutsTplStr) == -1) {
                    String newStruts = origStruts.replaceAll("</struts>", strutsTplStr + "</struts>");
                    FileUtils.writeStringToFile(strutsFile, newStruts, "UTF-8");
                }
            }
            if (!"false".equals(prop.getProperty("is_validate"))) {
                FileUtils.writeStringToFile(vldSaveFile, readTpl(vldSaveTpl, "UTF-8"), "UTF-8");
                FileUtils.writeStringToFile(vldEditFile, readTpl(vldEditTpl, "UTF-8"), "UTF-8");
                FileUtils.writeStringToFile(vldUpdateFile, readTpl(vldUpdateTpl, "UTF-8"), "UTF-8");
            }
        } catch (IOException e) {
            log.warn("write file faild! " + e.getMessage());
        }
    }

    private String readTpl(File tpl) {
        return readTpl(tpl, "GBK");
    }

    private String readTpl(File tpl, String charset) {
        String content = null;
        try {
            content = FileUtils.readFileToString(tpl, charset);
            Set<Object> ps = prop.keySet();
            for (Object o : ps) {
                String key = (String) o;
                String value = prop.getProperty(key);
                content = content.replaceAll("\\#\\{" + key + "\\}", value);
            }
        } catch (IOException e) {
            log.warn("read file faild. " + e.getMessage());
        }
        return content;
    }

    private String getFilePath(String packageName, String name) {
        log.debug("replace:" + packageName);
        String path = packageName.replaceAll("\\.", "/");
        log.debug("after relpace:" + path);
        return "src/" + path + "/" + name;
    }

    public void generate() {
        loadProperties();
        prepareFile();
        prepareTemplate();
        writeFile();
    }

    public static void main(String[] args) {
        String packName = "com.ponyjava.common.developer.template";
        String fileName = "template.properties";
        new ModuleGenerator(packName, fileName).generate();
    }
}
