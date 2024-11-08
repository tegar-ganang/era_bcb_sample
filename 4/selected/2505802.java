package org.uefl.ldIntegration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.coppercore.common.MessageList;
import org.coppercore.exceptions.CopperCoreException;
import org.coppercore.parser.IMSCPManifestNode;
import org.coppercore.parser.IMSLDActNode;
import org.coppercore.parser.IMSLDActivityStructureNode;
import org.coppercore.parser.IMSLDConditionsNode;
import org.coppercore.parser.IMSLDEmailDataNode;
import org.coppercore.parser.IMSLDIfNode;
import org.coppercore.parser.IMSLDInitialValueNode;
import org.coppercore.parser.IMSLDItemNode;
import org.coppercore.parser.IMSLDLearningActivityNode;
import org.coppercore.parser.IMSLDLocPersPropertyNode;
import org.coppercore.parser.IMSLDNode;
import org.coppercore.parser.IMSLDNotificationNode;
import org.coppercore.parser.IMSLDOnCompletionNode;
import org.coppercore.parser.IMSLDParser;
import org.coppercore.parser.IMSLDPlayNode;
import org.coppercore.parser.IMSLDRoleNode;
import org.coppercore.parser.IMSLDRolePartNode;
import org.coppercore.validator.IMSCPFactory;
import org.coppercore.validator.IMSPackage;
import org.metaworks.inputter.SelectInput;
import org.uengine.contexts.HtmlFormContext;
import org.uengine.contexts.MappingContext;
import org.uengine.contexts.TextContext;
import org.uengine.formmanager.FormUtil;
import org.uengine.kernel.AllActivity;
import org.uengine.kernel.ComplexActivity;
import org.uengine.kernel.EMailActivity;
import org.uengine.kernel.EventHandler;
import org.uengine.kernel.FormActivity;
import org.uengine.kernel.GlobalContext;
import org.uengine.kernel.HumanActivity;
import org.uengine.kernel.LocalEMailActivity;
import org.uengine.kernel.ParameterContext;
import org.uengine.kernel.ProcessDefinition;
import org.uengine.kernel.ProcessVariable;
import org.uengine.kernel.Role;
import org.uengine.kernel.RolePointingProcessVariable;
import org.uengine.kernel.ScopeActivity;
import org.uengine.kernel.ScriptActivity;
import org.uengine.kernel.SequenceActivity;
import org.uengine.kernel.SwitchActivity;
import org.uengine.processmanager.ProcessManagerFactoryBean;
import org.uengine.processmanager.ProcessManagerRemote;
import org.uengine.util.RecursiveLoop;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class LD2ProcessConverter {

    IMSLDPlayNode playNode = null;

    Vector actNameList = new Vector();

    Vector processVariableNameList = new Vector();

    Vector formActivities = new Vector();

    Vector locPersProperties = new Vector();

    Vector conditions = new Vector();

    Vector ifNode = new Vector();

    HashMap components = new HashMap();

    HashMap processVariableObject = new HashMap();

    HashMap formDefActNameAndAlias = new HashMap();

    HashMap formDefActNameAndVerId = new HashMap();

    String folderId = null;

    String folderName = null;

    String itemRef = null;

    File fileName = null;

    String webRoot = GlobalContext.getPropertyString("web.context.root", "/html/uengine-web");

    String extractedPath = "./data/LDExtract";

    public ProcessDefinition publicate(String packageFileName) throws CopperCoreException {
        String schemaLocation = "./data/schemas";
        String upLoadPath = "./data/upload";
        String extractPath = "./data/LDPackage";
        ProcessManagerFactoryBean prfm = new ProcessManagerFactoryBean();
        ProcessManagerRemote pm = null;
        try {
            pm = prfm.getProcessManagerForReadOnly();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        File uploadFolder = new File(upLoadPath);
        if (!uploadFolder.exists()) {
            uploadFolder.mkdir();
        }
        File extractFolder = new File(extractedPath);
        if (!extractFolder.exists()) {
            extractFolder.mkdir();
        }
        fileName = new File(packageFileName);
        String uploadedFileName = upLoadPath + File.separator + fileName.getName() + File.separator;
        try {
            upLoadFile(fileName, new File(uploadedFileName));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        IMSLDParser parser = null;
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        MessageList messageList = new MessageList();
        IMSPackage pack = IMSCPFactory.getIMSPackage(uploadedFileName, schemaLocation, messageList);
        if (pack.validate()) {
            parser = new IMSLDParser(pack.getManifest(), "", messageList);
            try {
                if (parser.process()) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String folder = extractedPath + File.separator + fileName.getName().replace(".zip", "") + File.separator;
            try {
                pack.storeResources(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        IMSCPManifestNode cpManifest = parser.getCPManifestNode();
        components = cpManifest.components;
        final Vector roleNames = new Vector();
        ArrayList actsNode = new ArrayList();
        RecursiveLoop findPlayNode = new RecursiveLoop() {

            public boolean logic(Object tree) {
                if (tree instanceof IMSLDPlayNode) {
                    playNode = (IMSLDPlayNode) tree;
                }
                if (tree instanceof IMSLDRoleNode) {
                    roleNames.add(((IMSLDNode) ((IMSLDRoleNode) tree)).getTitle());
                }
                if (tree instanceof IMSLDLocPersPropertyNode) {
                    locPersProperties.add((IMSLDLocPersPropertyNode) tree);
                }
                if (tree instanceof IMSLDConditionsNode) {
                    conditions.add((IMSLDConditionsNode) tree);
                }
                if (tree instanceof IMSLDIfNode) {
                    ifNode.add((IMSLDIfNode) tree);
                }
                return false;
            }

            public List getChildren(Object tree) {
                if (tree instanceof IMSLDActivityStructureNode) {
                    return ((IMSLDActivityStructureNode) tree).activities;
                }
                return ((IMSLDNode) tree).children;
            }
        };
        findPlayNode.run(cpManifest);
        ProcessDefinition processDefinition = new ProcessDefinition();
        processDefinition.setName(playNode.getTitle());
        Role role[] = new Role[roleNames.size() + 1];
        for (int roleSize = 0; roleSize < roleNames.size(); roleSize++) {
            role[roleSize] = new Role(((String) roleNames.get(roleSize)));
            role[roleSize].setAskWhenInit(false);
        }
        role[roleNames.size()] = new Role("Initiator");
        role[roleNames.size()].setAskWhenInit(false);
        processDefinition.setRoles(role);
        try {
            folderName = playNode.getTitle() + "_Form";
            folderId = pm.addFolder(folderName, "-1");
            pm.applyChanges();
            pm = prfm.getProcessManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
        actsNode = playNode.acts;
        Role humanRole[] = processDefinition.getRoles();
        HumanActivity humanActivity = new HumanActivity();
        humanActivity.setName("Bind Role");
        ParameterContext[] pc = new ParameterContext[humanRole.length - 1];
        int tempCnt = 0;
        for (int i = 0; i < humanRole.length; i++) {
            if (humanRole[i].getName().equals("Initiator")) {
                humanActivity.setRole(humanRole[i]);
                continue;
            }
            pc[tempCnt] = new ParameterContext();
            RolePointingProcessVariable rpp = new RolePointingProcessVariable();
            rpp.setRole(humanRole[i]);
            pc[tempCnt].setVariable(rpp);
            tempCnt++;
        }
        humanActivity.setParameters(pc);
        processDefinition.addChildActivity(humanActivity);
        ScopeActivity scopeActivity = new ScopeActivity();
        AllActivity mainAllActivity = new AllActivity();
        scopeActivity.addChildActivity(mainAllActivity);
        for (int actNodeCnt = 0; actNodeCnt < actsNode.size(); actNodeCnt++) {
            AllActivity allActivity = new AllActivity();
            IMSLDActNode actNode = (IMSLDActNode) actsNode.get(actNodeCnt);
            allActivity.setName(actNode.getTitle());
            final Vector rolePartNode = new Vector();
            RecursiveLoop findActNodeChild = new RecursiveLoop() {

                public boolean logic(Object tree) {
                    if (tree instanceof IMSLDRolePartNode) {
                        rolePartNode.add((IMSLDRolePartNode) tree);
                    }
                    return false;
                }

                public List getChildren(Object tree) {
                    return ((IMSLDNode) tree).children;
                }
            };
            findActNodeChild.run(actNode);
            for (int rolePartNodeCnt = 0; rolePartNodeCnt < rolePartNode.size(); rolePartNodeCnt++) {
                IMSLDRolePartNode rolePart = (IMSLDRolePartNode) rolePartNode.get(rolePartNodeCnt);
                String roleName = rolePart.getRole().getTitle();
                ComplexActivity complexActivity = new SequenceActivity();
                complexActivity.setName(rolePart.getTitle());
                IMSLDNode tempNode = rolePart.getActivity();
                if (tempNode instanceof IMSLDLearningActivityNode) {
                    IMSLDLearningActivityNode learningActivity = (IMSLDLearningActivityNode) tempNode;
                    FormActivity formActivity = setLearningActivity(learningActivity, roleName);
                    LocalEMailActivity emailActivity = deployEmailActivity(learningActivity, processDefinition, roleName);
                    complexActivity.addChildActivity(formActivity);
                    if (emailActivity != null) {
                        complexActivity.addChildActivity(emailActivity);
                    }
                } else if (tempNode instanceof IMSLDActivityStructureNode) {
                    ComplexActivity activityStructureComplexActivity = null;
                    IMSLDActivityStructureNode activityStructureNode = (IMSLDActivityStructureNode) tempNode;
                    Vector learningActivities = activityStructureNode.activities;
                    if ("selection".equals(activityStructureNode.getStructureType())) {
                        activityStructureComplexActivity = new SequenceActivity();
                    } else {
                        activityStructureComplexActivity = new SequenceActivity();
                    }
                    complexActivity.setName(activityStructureNode.getTitle());
                    for (int activitiesCnt = 0; activitiesCnt < learningActivities.size(); activitiesCnt++) {
                        IMSLDLearningActivityNode learningActivityNode = (IMSLDLearningActivityNode) learningActivities.get(activitiesCnt);
                        FormActivity formActivity = setLearningActivity(learningActivityNode, roleName);
                        LocalEMailActivity emailActivity = deployEmailActivity(learningActivityNode, processDefinition, roleName);
                        activityStructureComplexActivity.addChildActivity(formActivity);
                        if (emailActivity != null) {
                            activityStructureComplexActivity.addChildActivity(emailActivity);
                        }
                    }
                    complexActivity.addChildActivity(activityStructureComplexActivity);
                }
                allActivity.addChildActivity(complexActivity);
            }
            mainAllActivity.addChildActivity(allActivity);
        }
        processDefinition.addChildActivity(scopeActivity);
        ProcessVariable[] pv = new ProcessVariable[processVariableNameList.size() + locPersProperties.size() + 10];
        Vector insertedProcessVariable = new Vector();
        for (int pvCnt = 0; pvCnt < actNameList.size(); pvCnt++) {
            String tempActName = (String) actNameList.get(pvCnt);
            String processVariableName = tempActName + "_FormPV";
            if (!insertedProcessVariable.contains(processVariableName)) {
                pv[pvCnt] = new ProcessVariable();
                pv[pvCnt].setName(processVariableName);
                pv[pvCnt].setType(HtmlFormContext.class);
                HtmlFormContext hfc = new HtmlFormContext();
                hfc.setFormDefId("[" + formDefActNameAndAlias.get(tempActName) + "]@" + formDefActNameAndVerId.get(tempActName));
                pv[pvCnt].setDefaultValue(hfc);
                insertedProcessVariable.add(processVariableName);
                processVariableObject.put(processVariableName, pv[pvCnt]);
            }
        }
        for (int pvCnt = processVariableNameList.size(); pvCnt < (processVariableNameList.size() + locPersProperties.size()); pvCnt++) {
            int procertiesCnt = pvCnt - processVariableNameList.size();
            IMSLDLocPersPropertyNode tempPropertiesNode = (IMSLDLocPersPropertyNode) locPersProperties.get(procertiesCnt);
            IMSLDInitialValueNode initialValue = tempPropertiesNode.initialValueNode;
            ArrayList restriction = tempPropertiesNode.restrictions;
            String locPersPVName = tempPropertiesNode.identifierAttr + "_PV";
            String dataType = tempPropertiesNode.getDatatype();
            Node tempNode = initialValue.node.getFirstChild();
            if (!insertedProcessVariable.contains(locPersPVName)) {
                pv[pvCnt] = new ProcessVariable();
                pv[pvCnt].setName(locPersPVName);
                if ("string".equals(dataType)) {
                    pv[pvCnt].setType(String.class);
                } else if ("boolean".equals(dataType)) {
                    pv[pvCnt].setType(Boolean.class);
                    if ("0".equals(tempNode.getNodeValue())) pv[pvCnt].setDefaultValue(new Boolean(false)); else pv[pvCnt].setDefaultValue(new Boolean(true));
                }
                insertedProcessVariable.add(locPersPVName);
            }
            processVariableObject.put(locPersPVName, pv[pvCnt]);
        }
        int pvCnt = 1;
        for (int addPvCnt = (processVariableNameList.size() + locPersProperties.size()); addPvCnt < (processVariableNameList.size() + locPersProperties.size() + 5); addPvCnt++) {
            pv[addPvCnt] = new ProcessVariable();
            pv[addPvCnt].setName("show_class_Answer" + pvCnt + "_Wrong");
            pv[addPvCnt].setType(Boolean.class);
            pv[addPvCnt].setDefaultValue(new Boolean(false));
            pv[addPvCnt + 5] = new ProcessVariable();
            pv[addPvCnt + 5].setName("show_class_Answer" + pvCnt + "_Right");
            pv[addPvCnt + 5].setType(Boolean.class);
            pv[addPvCnt + 5].setDefaultValue(new Boolean(false));
            pvCnt++;
        }
        processDefinition.setProcessVariables(pv);
        if (ifNode.size() != 0) {
            EventHandler[] eventHandler = new EventHandler[1];
            eventHandler[0] = new EventHandler();
            IfNode2Handler ifToHandler = new IfNode2Handler();
            eventHandler[0] = ifToHandler.makeHandler(eventHandler[0], ifNode, processVariableObject);
            scopeActivity.setEventHandlers(eventHandler);
        }
        for (int formCnt = 0; formCnt < formActivities.size(); formCnt++) {
            FormActivity tempFormActivity = (FormActivity) formActivities.get(formCnt);
            String tempPvName = tempFormActivity.getName() + "_FormPV";
            String tempName = "Answer" + (formCnt + 1) + "_PV";
            MappingContext mc = new MappingContext();
            ProcessVariable tempVariable = null;
            tempVariable = processDefinition.getProcessVariable(tempName);
            if (tempVariable != null) {
                ParameterContext[] parameterContext = new ParameterContext[1];
                parameterContext[0] = new ParameterContext();
                TextContext textContext = new TextContext();
                ProcessVariable mappingVariable = new ProcessVariable();
                mappingVariable.setName(tempPvName + ".Answer" + (formCnt + 1));
                textContext.setText(tempName);
                parameterContext[0].setArgument(textContext);
                parameterContext[0].setVariable(mappingVariable);
                mc.setMappingElements(parameterContext);
            }
            tempFormActivity.setMappingContext(mc);
            tempFormActivity.setVariableForHtmlFormContext((ProcessVariable) processVariableObject.get(tempPvName));
        }
        return processDefinition;
    }

    public static void upLoadFile(File sourceFile, File targetFile) throws IOException {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(sourceFile).getChannel();
            outChannel = new FileOutputStream(targetFile).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            try {
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public FormActivity setLearningActivity(IMSLDLearningActivityNode learningActivityNode, String roleName) {
        String itemHref = null;
        String itemType = null;
        FormActivity formActivity = new FormActivity();
        formActivity.setName(learningActivityNode.getTitle());
        actNameList.add(learningActivityNode.getTitle());
        formActivities.add(formActivity);
        if (!processVariableNameList.contains(learningActivityNode.getTitle())) {
            processVariableNameList.add(learningActivityNode.getTitle());
        }
        Role role = new Role();
        role.setName(roleName);
        formActivity.setRole(role);
        RecursiveLoop findItemNode = new RecursiveLoop() {

            public boolean logic(Object tree) {
                if (tree instanceof IMSLDItemNode) {
                    IMSLDItemNode itemNode = (IMSLDItemNode) tree;
                    NamedNodeMap attribute = itemNode.node.getAttributes();
                    itemRef = attribute.getNamedItem("identifierref").getNodeValue();
                    return true;
                }
                return false;
            }

            public List getChildren(Object tree) {
                return ((IMSLDNode) tree).children;
            }
        };
        findItemNode.run(learningActivityNode);
        IMSLDNode tempNode = (IMSLDNode) components.get(itemRef);
        NamedNodeMap attribute = tempNode.node.getAttributes();
        itemHref = attribute.getNamedItem("href").getNodeValue();
        itemType = attribute.getNamedItem("type").getNodeValue();
        if (!formDefActNameAndAlias.containsKey(learningActivityNode.getTitle())) {
            String defVerId = deployForm(learningActivityNode.getTitle(), itemHref, itemType);
        }
        return formActivity;
    }

    public String deployForm(String actName, String itemHref, String itemType) {
        String defVerId = null;
        String definition = null;
        String source = new String();
        String tempPath = extractedPath + File.separator + fileName.getName().replace(".zip", "") + File.separator;
        String itemPath = extractedPath + File.separator + fileName.getName().replace(".zip", "") + File.separator + itemHref;
        ProcessManagerFactoryBean prfm = new ProcessManagerFactoryBean();
        ProcessManagerRemote pm = null;
        try {
            pm = prfm.getProcessManagerForReadOnly();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        Calendar c = Calendar.getInstance();
        long currentTime = c.getTimeInMillis();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(itemPath));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        if ("imsldcontent".equals(itemType)) {
            HtmlParsing htmlParser = new HtmlParsing();
            try {
                definition = htmlParser.tagParsing(in, locPersProperties, tempPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String s = null;
            try {
                while ((s = in.readLine()) != null) {
                    source += " " + s;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            definition = source;
        }
        String definitionName = actName + "_Form";
        String definitionFormAlias = actName + currentTime + "_alias";
        try {
            in.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            defVerId = FormUtil.deployFormDefinition(pm, definition, "1", definitionName, definitionFormAlias, folderId, "description", "form", definition, "");
            pm.applyChanges();
            pm = prfm.getProcessManager();
            defVerId = defVerId.substring(0, defVerId.lastIndexOf("@"));
            pm.setProcessDefinitionProductionVersion(defVerId);
            pm.applyChanges();
            formDefActNameAndAlias.put(actName, definitionFormAlias);
            formDefActNameAndVerId.put(actName, defVerId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defVerId;
    }

    public LocalEMailActivity deployEmailActivity(IMSLDLearningActivityNode learningActivityNode, ProcessDefinition processDefinition, String roleName) {
        LocalEMailActivity emailActivity = null;
        Vector onCompletion = learningActivityNode.children;
        IMSLDOnCompletionNode completionNode = null;
        for (int childCnt = 0; childCnt < onCompletion.size(); childCnt++) {
            if (onCompletion.get(childCnt) instanceof IMSLDOnCompletionNode) {
                completionNode = (IMSLDOnCompletionNode) onCompletion.get(childCnt);
                break;
            }
        }
        if (completionNode != null) {
            Vector notification = completionNode.children;
            IMSLDNotificationNode notificationNode = null;
            for (int cnt = 0; cnt < notification.size(); cnt++) {
                if (notification.get(cnt) instanceof IMSLDNotificationNode) {
                    notificationNode = (IMSLDNotificationNode) notification.get(cnt);
                    break;
                }
            }
            if (notificationNode != null) {
                String subject = notificationNode.subject.getTitle();
                String title = notificationNode.getTitle();
                Vector notificationChild = notificationNode.children;
                IMSLDEmailDataNode emailDataNode = null;
                for (int i = 0; i < notificationChild.size(); i++) {
                    if (notificationChild.get(i) instanceof IMSLDEmailDataNode) {
                        emailDataNode = (IMSLDEmailDataNode) notificationChild.get(i);
                        break;
                    }
                }
                String toRoleId = emailDataNode.getRoleId();
                String toRole = ((IMSLDRoleNode) components.get(toRoleId)).getTitle();
                Role toRoleVal = processDefinition.getRole(toRole);
                Role fromRoleVal = processDefinition.getRole(roleName);
                emailActivity = new LocalEMailActivity();
                emailActivity.setTitle(subject);
                emailActivity.setToRole(toRoleVal);
                emailActivity.setFromRole(fromRoleVal);
            }
        }
        return emailActivity;
    }
}
