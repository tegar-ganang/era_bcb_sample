package com.doculibre.intelligid.izpack;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import com.izforge.izpack.util.AbstractUIProcessHandler;

/**
 * Cette classe sera utilisée par le ProcessPanel (izPack) pour appliquer les
 * options choisies par l'utilisateur
 */
public class ConfigureIntelligid {

    private static final String GEST_AUTH_DB = "gestionnaireAuthentificationHibernate";

    private static final String GEST_AUTH_LDAP = "gestionnaireAuthentificationLDAP";

    public static void run(AbstractUIProcessHandler handler, String[] args) {
        if (args.length != 16) {
            System.out.println("Not the same number of parameters as declared in ProcessPanel.Spec.xml");
            return;
        }
        int paramIndex = 0;
        File webContentDir = new File(args[paramIndex++]);
        File webInfDir = new File(webContentDir, "WEB-INF");
        File classesDir = new File(webInfDir, "classes");
        File applicationContext = new File(classesDir, "applicationContext.xml");
        String dbChoice = args[paramIndex++];
        String dbHost = args[paramIndex++];
        String dbPortEmbedded = args[paramIndex++];
        String dbPortMysql = args[paramIndex++];
        String dbName = args[paramIndex++];
        String dbUsername = args[paramIndex++];
        String dbPassword = args[paramIndex++];
        String authChoice = args[paramIndex++];
        String activeDirectoryURL = args[paramIndex++];
        List<String> activeDirectoryDomains = Arrays.asList(args[paramIndex++].split("[:]"));
        String activeDirectoryAdminUser = args[paramIndex++];
        String serverHost = args[paramIndex++];
        String serverPort = args[paramIndex++];
        String organizationID = args[paramIndex++];
        String initialDataChoice = args[paramIndex++];
        try {
            Document doc = new SAXBuilder().build(applicationContext);
            bean(doc, "idOrganisation").firstConstructorArg().setValue(organizationID);
            bean(doc, "serverHost").firstConstructorArg().setValue(serverHost);
            bean(doc, "serverPort").firstConstructorArg().setValue(serverPort);
            String dbPort;
            if (dbChoice == null || dbChoice.equals("Embedded")) {
                dbHost = "localhost";
                dbPort = dbPortEmbedded;
                dbName = "intelligid";
                dbUsername = "alice";
                dbPassword = "q93uti0opwhkd";
                bean(doc, "databasePort").firstConstructorArg().setValue(dbPort);
            } else {
                dbPort = dbPortMysql;
                bean(doc, "databasePort").firstConstructorArg().setValue("0");
            }
            bean(doc, "dataSource").property("url").setValue("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?createDatabaseIfNotExist=true");
            bean(doc, "dataSource").property("username").setValue(dbUsername);
            bean(doc, "dataSource").property("password").setValue(dbPassword);
            boolean authDB = authChoice.equals("Database");
            String gestionnaire = authDB ? GEST_AUTH_DB : GEST_AUTH_LDAP;
            bean(doc, "gestionnaireAuthentification").property("target").element("ref").setAttribute("local", gestionnaire);
            if (!authDB) {
                SpringElement bean = bean(doc, GEST_AUTH_LDAP);
                bean.property("url").setValue(activeDirectoryURL);
                bean.property("utilisateurAdmin").setValue(activeDirectoryAdminUser);
                bean.property("domaines").setValues(activeDirectoryDomains);
            }
            Writer w = new BufferedWriter(new FileWriter(applicationContext));
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat());
            outputter.output(doc, w);
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (JDOMException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        copierScriptChargement(webInfDir, initialDataChoice);
    }

    private static void copierScriptChargement(File webInfDir, String initialDataChoice) {
        File chargementInitialDir = new File(webInfDir, "chargementInitial");
        File fichierChargement = new File(chargementInitialDir, "ScriptChargementInitial.sql");
        File fichierChargementAll = new File(chargementInitialDir, "ScriptChargementInitial-All.sql");
        File fichierChargementTypesDocument = new File(chargementInitialDir, "ScriptChargementInitial-TypesDocument.sql");
        File fichierChargementVide = new File(chargementInitialDir, "ScriptChargementInitial-Vide.sql");
        if (fichierChargement.exists()) {
            fichierChargement.delete();
        }
        File fichierUtilise = null;
        if ("all".equals(initialDataChoice)) {
            fichierUtilise = fichierChargementAll;
        } else if ("typesDocument".equals(initialDataChoice)) {
            fichierUtilise = fichierChargementTypesDocument;
        } else if ("empty".equals(initialDataChoice)) {
            fichierUtilise = fichierChargementVide;
        }
        if (fichierUtilise != null && fichierUtilise.exists()) {
            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(fichierUtilise).getChannel();
                destination = new FileOutputStream(fichierChargement).getChannel();
                destination.transferFrom(source, 0, source.size());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (source != null) {
                    try {
                        source.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (destination != null) {
                    try {
                        destination.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static SpringElement bean(Document document, String beanId) {
        Element beans = document.getRootElement();
        for (Element bean : (List<Element>) beans.getChildren("bean")) {
            if (beanId.equals(bean.getAttributeValue("id"))) {
                return new SpringElement(bean, false);
            }
        }
        return null;
    }

    public static class SpringElement {

        private Element jdomElement;

        private boolean valueInAttribute;

        public SpringElement(Element jdomElement, boolean valueInAttribute) {
            super();
            this.jdomElement = jdomElement;
            this.valueInAttribute = valueInAttribute;
        }

        private SpringElement firstConstructorArg() {
            return constructorArg(0);
        }

        private SpringElement constructorArg(int index) {
            return element("constructor-arg", index, true);
        }

        public Element get() {
            return jdomElement;
        }

        public void setAttribute(String attribute, String value) {
            jdomElement.setAttribute(attribute, value);
        }

        @SuppressWarnings("unchecked")
        public SpringElement property(String propertyName) {
            for (Element bean : (List<Element>) jdomElement.getChildren("property")) {
                if (propertyName.equals(bean.getAttributeValue("name"))) {
                    return new SpringElement(bean, false);
                }
            }
            return null;
        }

        public void setValue(String value) {
            if (valueInAttribute) {
                setAttribute("value", value);
            } else {
                jdomElement.removeChildren("value");
                Element valueElement = new Element("value");
                jdomElement.addContent(valueElement);
                valueElement.setText(value);
            }
        }

        public void setValues(List<String> values) {
            jdomElement.removeChildren("list");
            Element list = new Element("list");
            jdomElement.addContent(list);
            for (String value : values) {
                Element valueElement = new Element("value");
                list.addContent(valueElement);
                valueElement.setText(value);
            }
        }

        public SpringElement element(String tag) {
            return element(tag, 0);
        }

        public SpringElement element(String tag, int index) {
            return element(tag, index, false);
        }

        public SpringElement element(String tag, int index, boolean valueInAttribute) {
            return new SpringElement((Element) jdomElement.getChildren(tag).get(index), valueInAttribute);
        }
    }

    public static void main(String[] args) {
        int index = 0;
        String[] arguments = new String[15];
        arguments[index++] = "/Applications/Intelligid/intelligid/bin/";
        arguments[index++] = "Embedded";
        arguments[index++] = "localhost1";
        arguments[index++] = "12345";
        arguments[index++] = "intelligid3";
        arguments[index++] = "root4";
        arguments[index++] = "root5";
        arguments[index++] = "ActiveDirectory";
        arguments[index++] = "ldap://myserver:1236";
        arguments[index++] = "/path/first/domain:/path/second/domain7";
        arguments[index++] = "SexyMac.local8";
        arguments[index++] = "90909";
        arguments[index++] = "myOrganisation10";
        arguments[index++] = "test11";
        arguments[index++] = "Empty";
        run(null, arguments);
    }
}
