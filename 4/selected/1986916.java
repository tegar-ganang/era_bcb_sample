package com.smb.MMUtil.handler.createORM;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.smb.MMUtil.pojo.CreateORMMappingFiles;
import com.smb.MMUtil.pojo.MySQLShowColumns;

public class CreateHibernateHelper {

    private static Log logger = LogFactory.getLog(CreateHibernateHelper.class);

    private static String hibernate_cfg_xml_Path = CreateHibernateHelper.class.getResource("hibernate/hibernate.cfg.xml").getFile();

    private static String spring_hbernate_Path = CreateHibernateHelper.class.getResource("hibernate/Spring_Hibernate.xml").getFile();

    private static String hibernate_hbm_xml_Path = CreateHibernateHelper.class.getResource("hibernate/hibernate.hbm.xml").getFile();

    private static CreateBaseHandler baseHandler = new CreateBaseHandler();

    private static String mapping_resource = "<mapping resource=\"#PojoMapping.xml#\"/>";

    private static String hibernate_hbm_xml_content = baseHandler.getFiletoString(hibernate_hbm_xml_Path);

    private static String hibernate_cfg_xml_content = baseHandler.getFiletoString(hibernate_cfg_xml_Path);

    private static String spring_hbernate_xml = baseHandler.getFiletoString(spring_hbernate_Path);

    public String HibernateSpringFile(String host, String dbName, String user, String pswd, String tabNames[], String packName) throws Exception {
        logger.info("HibernateSpringFile .....................");
        StringBuffer SpringHibernateFile = new StringBuffer();
        spring_hbernate_xml = spring_hbernate_xml.replaceAll(CreateBaseHandler.username, user).replaceAll(CreateBaseHandler.password, pswd).replaceAll(CreateBaseHandler.dbhost, host).replaceAll(CreateBaseHandler.dbname, dbName);
        String getFMTpackName = baseHandler.getFMTpackName(packName);
        StringBuffer valueBuffer = new StringBuffer();
        valueBuffer.append("<list>\n");
        for (int i = 0; i < tabNames.length; i++) {
            valueBuffer.append("			     <value>").append(getFMTpackName + "/" + tabNames[i]).append(".hbm.xml</value>\n");
        }
        valueBuffer.append("			</list>");
        SpringHibernateFile.append(spring_hbernate_xml.split("<list>")[0]).append(valueBuffer).append(spring_hbernate_xml.split("</list>")[1]);
        return SpringHibernateFile.toString();
    }

    public String HibernateCFGFile(String host, String dbName, String user, String pswd, String tabNames[], String packName) throws Exception {
        logger.info("HibernateCFGFile .....................");
        StringBuffer HibernateCFGFile = new StringBuffer();
        try {
            hibernate_cfg_xml_content.replaceAll(CreateBaseHandler.username, user).replaceAll(CreateBaseHandler.password, pswd).replaceAll(CreateBaseHandler.dbhost, host).replaceAll(CreateBaseHandler.dbname, dbName);
            HibernateCFGFile.append(hibernate_cfg_xml_content.split(mapping_resource)[0]);
            String getFMTpackName = baseHandler.getFMTpackName(packName);
            for (int i = 0; i < tabNames.length; i++) {
                HibernateCFGFile.append("<mapping resource=\"" + getFMTpackName + "/" + tabNames[i] + ".hbm.xml\"/>");
                HibernateCFGFile.append("\n		");
            }
            HibernateCFGFile.append(hibernate_cfg_xml_content.split(mapping_resource)[1]);
            return HibernateCFGFile.toString();
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public CreateORMMappingFiles HibernateHBMFile(String tabName, String packName, List<MySQLShowColumns> list) throws Exception {
        CreateORMMappingFiles ORMMappingFiles = new CreateORMMappingFiles();
        logger.info("HibernateHBMFile .....................");
        StringBuffer HibernateHBMFile = new StringBuffer();
        HibernateHBMFile.append(hibernate_hbm_xml_content.split("<class name=")[0]);
        String s1 = "      <class name=\"" + packName + "." + tabName + "\" table=\"" + tabName + "\" dynamic-insert=\"true\" dynamic-update=\"true\" lazy=\"false\">";
        HibernateHBMFile.append(s1).append("\n      <meta attribute=\"implement-equals\">true</meta>\n<cache usage=\"read-write\" />\n");
        String pojoIdName = "";
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getKey().equals("PRI")) {
                pojoIdName = list.get(i).getField();
                String ID = "\n      <id name=\"" + pojoIdName + "\" column=\"" + pojoIdName + "\" type=\"java.lang.Integer\" unsaved-value=\"-1\">\n            <generator class=\"identity\" />\n      </id>\n";
                HibernateHBMFile.append(ID);
            } else {
                if (list.get(i).getType().equals("datetime")) {
                    String dd = "      <property name=\"" + list.get(i).getField() + "\" column=\"" + list.get(i).getField() + "\" type=\"java.util.Date\" />";
                    HibernateHBMFile.append(dd).append("\n 	");
                } else {
                    String dd = "      <property name=\"" + list.get(i).getField() + "\" column=\"" + list.get(i).getField() + "\"  />";
                    HibernateHBMFile.append(dd).append("\n	");
                }
            }
        }
        HibernateHBMFile.append("\n      </class>\n</hibernate-mapping>");
        ORMMappingFiles.setFileContext(HibernateHBMFile.toString());
        ORMMappingFiles.setFileName(tabName + ".hbm.xml");
        return ORMMappingFiles;
    }
}
