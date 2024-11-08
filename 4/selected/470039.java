package org.jboke.util;

import java.io.*;
import java.util.*;

/**
 * Creates Java-sourcecode for EJBs
 *
 * @author Kurt Huwig
 * @version $Id: EJBCreator.java,v 1.6 2001/07/16 10:12:57 kurti Exp $
 *
 * This file is part of the JBoKe-Project, see http://www.jboke.org/
 * (c) 2001 iKu Netzwerkl&ouml;sungen
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
public class EJBCreator {

    private static final String CLASS_START = "public class ", METHOD_START = "public ", FIELD_START = "public ", EXTENDS_START = "extends ", JAWS_START = "// JAWS: ", TRANSACTION_START = "// TRANSACTION: ", TYPE_START = "// TYPE: ", TYPE_SESSION = "Session", TYPE_ENTITY = "Entity";

    private File fileDataContainer, fileEnterpriseBean, fileHomeInterface, fileRemoteInterface, fileEJBFieldXML, fileEJBMethodXML, fileJawsXML;

    private FileWriter fwDataContainer, fwEnterpriseBean, fwHomeInterface, fwRemoteInterface, fwEJBFieldXML, fwEJBMethodXML, fwJawsXML;

    private static final int SESSION_BEAN = 0, ENTITY_BEAN = 1;

    private int iEJBType;

    private String sBaseName;

    public EJBCreator(String sTargetDir, String sPackage, String sFilename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(sFilename));
        String sLine;
        StringBuffer sbInitialPart = new StringBuffer();
        sLine = br.readLine();
        if (!sLine.startsWith(TYPE_START)) {
            System.err.println("Bean type missing!");
            System.exit(1);
        }
        if (sLine.indexOf(TYPE_SESSION) != -1) {
            iEJBType = SESSION_BEAN;
        } else if (sLine.indexOf(TYPE_ENTITY) != -1) {
            iEJBType = ENTITY_BEAN;
        } else {
            System.err.println("Unknown bean type!");
            System.exit(1);
        }
        while (!(sLine = br.readLine()).startsWith(CLASS_START)) {
            sbInitialPart.append(sLine);
            sbInitialPart.append('\n');
        }
        sLine = sLine.substring(CLASS_START.length());
        String sClassName = sLine.substring(0, sLine.indexOf(' '));
        sLine = sLine.substring(sLine.indexOf(EXTENDS_START));
        sLine = sLine.substring(EXTENDS_START.length());
        sBaseName = sLine.substring(0, sLine.indexOf(' '));
        createFiles(sTargetDir, sClassName);
        writeHeader(sPackage, sClassName, sBaseName, sbInitialPart);
        boolean bCreateDataContainer = parseBody(br, sClassName, sBaseName);
        writeFooter();
        closeAll();
        if (!bCreateDataContainer && iEJBType == ENTITY_BEAN) {
            fileDataContainer.delete();
        }
    }

    protected void createFiles(String sTargetDir, String sClassName) throws IOException {
        if (iEJBType == ENTITY_BEAN) {
            fileDataContainer = new File(sTargetDir, sClassName + "Data.java");
            fileJawsXML = new File(sTargetDir, sClassName + "-jaws.xml");
        }
        fileEnterpriseBean = new File(sTargetDir, sClassName + "EJB.java");
        fileHomeInterface = new File(sTargetDir, sClassName + "Home.java");
        fileRemoteInterface = new File(sTargetDir, sClassName + ".java");
        fileEJBFieldXML = new File(sTargetDir, sClassName + "-field-ejb-jar.xml");
        fileEJBMethodXML = new File(sTargetDir, sClassName + "-method-ejb-jar.xml");
        if (iEJBType == ENTITY_BEAN) {
            fileDataContainer.delete();
            fileJawsXML.delete();
        }
        fileEnterpriseBean.delete();
        fileHomeInterface.delete();
        fileRemoteInterface.delete();
        fileEJBFieldXML.delete();
        fileEJBMethodXML.delete();
        if (iEJBType == ENTITY_BEAN) {
            fwDataContainer = new FileWriter(fileDataContainer);
            fwJawsXML = new FileWriter(fileJawsXML);
        }
        fwEnterpriseBean = new FileWriter(fileEnterpriseBean);
        fwHomeInterface = new FileWriter(fileHomeInterface);
        fwRemoteInterface = new FileWriter(fileRemoteInterface);
        fwEJBFieldXML = new FileWriter(fileEJBFieldXML);
        fwEJBMethodXML = new FileWriter(fileEJBMethodXML);
    }

    /**
   * writes a String into all java-files
   */
    protected void writeAllJava(String s) throws IOException {
        if (iEJBType == ENTITY_BEAN) {
            fwDataContainer.write(s);
        }
        fwEnterpriseBean.write(s);
        fwHomeInterface.write(s);
        fwRemoteInterface.write(s);
    }

    /**
   * writes the header
   */
    protected void writeHeader(String sPackage, String sClassName, String sBaseName, StringBuffer sbInitialPart) throws IOException {
        writeAllJava("// This file is auto-generated; changes are futile\n\n");
        writeAllJava("package " + sPackage + ";\n\n");
        fwEnterpriseBean.write("import javax.ejb.CreateException;\n");
        fwHomeInterface.write("import java.rmi.RemoteException;\n" + "import javax.ejb.*;\n");
        fwRemoteInterface.write("import java.rmi.RemoteException;\n");
        writeAllJava(sbInitialPart.toString());
        if (iEJBType == ENTITY_BEAN) {
            fwDataContainer.write("public class " + sClassName + "Data extends " + sBaseName + "Data {\n");
        }
        fwEnterpriseBean.write("public class " + sClassName + "EJB extends " + sBaseName);
        switch(iEJBType) {
            case ENTITY_BEAN:
                fwEnterpriseBean.write("EntityBean implements " + sBaseName + "Business {\n");
                break;
            case SESSION_BEAN:
                fwEnterpriseBean.write("Adapter {\n");
                break;
        }
        fwHomeInterface.write("public interface " + sClassName + "Home extends " + sBaseName + "Home {\n");
        fwRemoteInterface.write("public interface " + sClassName + " extends ");
        switch(iEJBType) {
            case ENTITY_BEAN:
                fwRemoteInterface.write(sBaseName + "Business, javax.ejb.EJBObject {\n");
                break;
            case SESSION_BEAN:
                fwRemoteInterface.write("javax.ejb.EJBObject {\n");
                break;
        }
        switch(iEJBType) {
            case ENTITY_BEAN:
                fwEJBFieldXML.write("    <entity>\n");
                break;
            case SESSION_BEAN:
                fwEJBFieldXML.write("    <session>\n");
                break;
        }
        fwEJBFieldXML.write("      <description>" + sClassName + "</description>\n" + "      <display-name>" + sClassName + "</display-name>\n" + "      <ejb-name>" + sClassName + "</ejb-name>\n" + "      <home>" + sPackage + "." + sClassName + "Home</home>\n" + "      <remote>" + sPackage + "." + sClassName + "</remote>\n" + "      <ejb-class>" + sPackage + "." + sClassName + "EJB</ejb-class>\n");
        switch(iEJBType) {
            case ENTITY_BEAN:
                if (sBaseName.endsWith("Node")) {
                    fwEJBFieldXML.write("      <persistence-type>Container</persistence-type>\n" + "      <prim-key-class>java.lang.Integer</prim-key-class>\n" + "      <reentrant>false</reentrant>\n");
                } else {
                    fwEJBFieldXML.write("      <persistence-type>Container</persistence-type>\n" + "      <prim-key-class>org.jboke.framework.ejb.NodeBusiness</prim-key-class>\n" + "      <reentrant>false</reentrant>\n");
                }
                copyResourceInFile(("/" + sBaseName + "-field-ejb-jar").replace('.', '/') + ".xml", fwEJBFieldXML);
                fwJawsXML.write("    <entity>\n" + "      <ejb-name>" + sClassName + "</ejb-name>\n" + "      <create-table>true</create-table>\n" + "      <remove-table>false</remove-table>\n" + "      <tuned-updates>true</tuned-updates>\n" + "      <read-only>false</read-only>\n" + "      <timeout>false</timeout>\n");
                copyResourceInFile(("/" + sBaseName + "-jaws").replace('.', '/') + ".xml", fwJawsXML);
                break;
            case SESSION_BEAN:
                fwEJBFieldXML.write("      <session-type>Stateful</session-type>\n" + "      <transaction-type>Container</transaction-type>\n");
                break;
        }
    }

    /**
   * parses the body
   *
   * @return if a data-container should be created
   */
    protected boolean parseBody(BufferedReader br, String sClassName, String sBaseName) throws IOException {
        String sLine;
        final int NOP = 0, CONSTANTS = 1, FIELDS = 2, METHODS = 3, EJBMETHODS = 4, FINDERS = 5, END = 6;
        boolean bCreateDataContainer = false;
        int iState = NOP;
        LinkedList listFields = new LinkedList(), listMethods = new LinkedList(), listJaws = new LinkedList();
        while (iState != END) {
            sLine = br.readLine();
            if (sLine == null) {
                System.err.println("Unexpected EOF");
                System.exit(1);
            }
            if (sLine.indexOf("JBOKE-CONSTANTS") != -1) {
                iState = CONSTANTS;
            } else if (sLine.indexOf("JBOKE-FIELDS") != -1) {
                iState = FIELDS;
            } else if (sLine.indexOf("JBOKE-METHODS") != -1) {
                iState = METHODS;
            } else if (sLine.indexOf("JBOKE-EJBMETHODS") != -1) {
                iState = EJBMETHODS;
            } else if (sLine.indexOf("JBOKE-CREATEDATACONTAINER") != -1) {
                bCreateDataContainer = true;
                iState = NOP;
            } else if (sLine.indexOf("JBOKE-FINDERS") != -1) {
                iState = FINDERS;
            } else if (sLine.indexOf("JBOKE-END") != -1) {
                iState = END;
            }
            sLine += "\n";
            String sTrimmedLine;
            switch(iState) {
                case CONSTANTS:
                    fwRemoteInterface.write(sLine);
                    break;
                case FIELDS:
                    fwDataContainer.write(sLine);
                    fwEnterpriseBean.write(sLine);
                    sTrimmedLine = sLine.trim();
                    if (sTrimmedLine.startsWith(FIELD_START)) {
                        String sField = sTrimmedLine.substring(FIELD_START.length(), sTrimmedLine.indexOf(';'));
                        listFields.add(sField);
                        if (sTrimmedLine.indexOf(JAWS_START) != -1) {
                            listJaws.add(sField + "|" + sTrimmedLine.substring(sTrimmedLine.indexOf(JAWS_START) + JAWS_START.length()));
                        }
                    }
                    break;
                case METHODS:
                    fwEnterpriseBean.write(sLine);
                    sTrimmedLine = sLine.trim();
                    if (sTrimmedLine.startsWith(METHOD_START)) {
                        fwRemoteInterface.write("  " + sTrimmedLine.substring(METHOD_START.length(), sTrimmedLine.indexOf('{')) + "throws RemoteException;\n");
                        extractTransaction(sTrimmedLine, listMethods);
                    }
                    break;
                case EJBMETHODS:
                    if (sLine.indexOf(METHOD_START) != -1) {
                        extractTransaction(sLine, listMethods);
                    }
                    fwEnterpriseBean.write(sLine);
                    break;
                case FINDERS:
                    if (sLine.indexOf(METHOD_START) != -1) {
                        extractTransaction(sLine, listMethods);
                    }
                    fwHomeInterface.write(sLine);
                    break;
            }
        }
        if (bCreateDataContainer) {
            fwRemoteInterface.write("  " + sClassName + "Data getData() throws RemoteException;\n");
            fwEnterpriseBean.write("  public " + sClassName + "Data getData() {\n" + "    " + sClassName + "Data data = new " + sClassName + "Data();\n" + "    storeFields( data );\n");
        }
        if (iEJBType == ENTITY_BEAN) {
            while (!listFields.isEmpty()) {
                String sField = (String) listFields.removeFirst();
                String sFieldName = sField.substring(sField.indexOf(' ') + 1);
                if (bCreateDataContainer) {
                    fwEnterpriseBean.write("    data." + sFieldName + " = " + sFieldName + ";\n");
                }
                fwEJBFieldXML.write("      <cmp-field>\n" + "        <field-name>" + sFieldName + "</field-name>\n" + "      </cmp-field>\n");
            }
            while (!listJaws.isEmpty()) {
                String sJaws = (String) listJaws.removeFirst();
                StringTokenizer st = new StringTokenizer(sJaws, "|");
                String sFieldName = st.nextToken();
                sFieldName = sFieldName.substring(sFieldName.indexOf(' ') + 1);
                fwJawsXML.write("      <cmp-field>\n" + "        <field-name>" + sFieldName + "</field-name>\n" + "        <column-name>" + sFieldName + "</column-name>\n" + "        <sql-type>" + st.nextToken() + "</sql-type>\n" + "        <jdbc-type>" + st.nextToken() + "</jdbc-type>\n" + "      </cmp-field>\n");
            }
            if (bCreateDataContainer) {
                fwEnterpriseBean.write("    return data;\n" + "  }\n");
            }
        }
        while (!listMethods.isEmpty()) {
            String sMethod = (String) listMethods.removeFirst();
            StringTokenizer st = new StringTokenizer(sMethod, "|");
            fwEJBMethodXML.write("      <container-transaction>\n" + "        <method>\n" + "          <ejb-name>" + sClassName + "</ejb-name>\n" + "          <method-name>" + st.nextToken() + "</method-name>\n" + "        </method>\n" + "        <trans-attribute>" + st.nextToken() + "</trans-attribute>\n" + "      </container-transaction>\n");
        }
        return bCreateDataContainer;
    }

    /**
   * writes the footer
   */
    protected void writeFooter() throws IOException {
        writeAllJava("}\n");
        switch(iEJBType) {
            case ENTITY_BEAN:
                if (sBaseName.endsWith("Node")) {
                    fwEJBFieldXML.write("      <primkey-field>id</primkey-field>\n" + "    </entity>\n");
                } else {
                    fwEJBFieldXML.write("      <primkey-field>node</primkey-field>\n" + "    </entity>\n");
                }
                fwJawsXML.write("    </entity>\n");
                break;
            case SESSION_BEAN:
                fwEJBFieldXML.write("    </session>\n");
                break;
        }
    }

    /**
   * closes all files
   */
    protected void closeAll() throws IOException {
        if (iEJBType == ENTITY_BEAN) {
            fwDataContainer.close();
            fwJawsXML.close();
        }
        fwEnterpriseBean.close();
        fwHomeInterface.close();
        fwRemoteInterface.close();
        fwEJBMethodXML.close();
        fwEJBFieldXML.close();
    }

    protected void copyResourceInFile(String sResource, Writer writer) throws IOException {
        Reader reader = new InputStreamReader(getClass().getResourceAsStream(sResource));
        int iRead;
        char[] acBuffer = new char[4096];
        while ((iRead = reader.read(acBuffer, 0, acBuffer.length)) != -1) {
            writer.write(acBuffer, 0, iRead);
        }
    }

    protected void extractTransaction(String sLine, LinkedList list) {
        int iPos;
        if ((iPos = sLine.indexOf(TRANSACTION_START)) != -1) {
            StringTokenizer st = new StringTokenizer(sLine);
            st.nextToken();
            st.nextToken();
            String sMethodName = st.nextToken();
            sMethodName = sMethodName.substring(0, sMethodName.indexOf('('));
            list.add(sMethodName + "|" + sLine.substring(iPos + TRANSACTION_START.length()));
        }
    }

    public static void main(String[] asParams) {
        if (asParams.length < 3) {
            System.err.println("Usage: org.jboke.util.EJBCreator <TargetDir> <Package> " + "[ <JBoKeEJB>... ]");
            System.exit(1);
        }
        for (int i = 2; i < asParams.length; i++) {
            try {
                new EJBCreator(asParams[0], asParams[1], asParams[i]);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
