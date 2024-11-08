package com.ivis.xprocess.framework.migratetov3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import com.ivis.xprocess.core.Artifact;
import com.ivis.xprocess.framework.XchangeElementContainer;
import com.ivis.xprocess.framework.properties.PropertyType;
import com.ivis.xprocess.util.FileUtils;
import com.ivis.xprocess.util.UuidUtils;
import com.ivis.xprocess.util.Version;

public class MigrateToV3 {

    protected static final Logger logger = Logger.getLogger(MigrateToV3.class.getName());

    public static final String V3VERSION = "3.0.0_15291";

    private IProgressListener progressListener;

    private ConversionXmlifier conversionXmlifier = new ConversionXmlifier();

    private ConversionDexmlifier conversionDexmlifier = new ConversionDexmlifier();

    private Map<String, String> oldStaticUuidsToNewUuids = new HashMap<String, String>();

    private Map<String, String> containerContainer = new HashMap<String, String>();

    private Map<String, HashSet<String>> containerContents = new HashMap<String, HashSet<String>>();

    private Map<String, HashSet<String>> xchangeElementContents = new HashMap<String, HashSet<String>>();

    private Map<String, String> artifacts = new HashMap<String, String>();

    private String sourceDirectory;

    private String destinationDirectory;

    private File localSrcDir;

    private File localSrc;

    private File dataSourceSrc;

    private File dataSourceDest;

    private File localDestDir;

    private File openDestDir;

    private File openSrcDir;

    private File artifactsSrcDir;

    private File artifactsDestDir;

    Boolean hasXmlBeenUpdated = false;

    private Document document;

    private HashMap<Element, ArrayList<Element>> elementsToRemove;

    private Set<String> newChoicesToCreate;

    /**
     * @param sourceDirectory the original v2.9 Data Source
     * @param destinationDirectory - the destination directory
     */
    public MigrateToV3(String sourceDirectory, String destinationDirectory) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
    }

    private void init() throws DatasourceConversionException {
        localSrcDir = new File(sourceDirectory + File.separator + "local");
        localSrc = new File(localSrcDir + File.separator + "local.xml");
        dataSourceSrc = new File(sourceDirectory + File.separator + "datasource.xml");
        openSrcDir = new File(sourceDirectory + File.separator + "open");
        artifactsSrcDir = new File(sourceDirectory + File.separator + "artifacts");
        if (!isUnshared()) {
            throw new DatasourceConversionException("Aborting Datasource conversion, the Datasource is shared and cannot be converted.");
        }
        localDestDir = new File(destinationDirectory + File.separator + "local");
        dataSourceDest = new File(destinationDirectory + File.separator + "datasource.xml");
        openDestDir = new File(destinationDirectory + File.separator + "open");
        artifactsDestDir = new File(destinationDirectory + File.separator + "artifacts");
    }

    public void migrate() throws DatasourceConversionException {
        init();
        createOldUuidMap();
        parseContainers(openSrcDir);
        parseContainerContents(openSrcDir);
        processArtifacts(artifactsSrcDir);
        updateLogicalContainer(openDestDir);
        updateReferences(openDestDir);
        copyBaseComponents();
        updateMinimumCompatibleVersion();
    }

    private void parseContainers(File dirToParse) throws DatasourceConversionException {
        for (File file : dirToParse.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".xpx")) {
                String fileName = ConversionUtil.stripXPX(file);
                if (fileName.equals(dirToParse.getName())) {
                    createXchangeElementContainer(openDestDir, file);
                }
            } else {
                if (file.isFile()) {
                    logger.log(Level.SEVERE, "Data Source conversion hit an unknown file - " + file.getAbsolutePath());
                    throw new DatasourceConversionException("Found unknown file in the Data Source - " + file.getAbsolutePath());
                }
                parseContainers(file);
                String containerContainerUuid = getUuid(dirToParse.getName());
                String containerUuid = getUuid(file.getName());
                containerContainer.put(containerUuid, containerContainerUuid);
            }
        }
    }

    private void parseContainerContents(File dirToParse) {
        String containerUuid = getUuid(dirToParse.getName());
        for (File file : dirToParse.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".xpx")) {
                String xchangeElementUuid = getUuid(ConversionUtil.stripXPX(file));
                if (!xchangeElementUuid.equals(dirToParse.getName())) {
                    HashSet<String> contents = containerContents.get(containerUuid);
                    contents.add(xchangeElementUuid);
                }
                parseXpxContents(file);
                if (!xchangeElementUuid.equals(containerUuid)) {
                    copyXpxElements(file, containerUuid);
                }
            } else {
                parseContainerContents(file);
            }
        }
    }

    private void copyXpxElements(File xpxFile, String containerUuid) {
        String xchangeElementUuid = getUuid(ConversionUtil.stripXPX(xpxFile));
        File destinationFile = new File(openDestDir + File.separator + containerUuid + File.separator + xchangeElementUuid + ".xpx");
        try {
            FileUtils.copyFile(xpxFile, destinationFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem copying element file: " + xpxFile, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseXpxContents(File xpxFile) {
        Document document = conversionDexmlifier.getDocument(xpxFile.getAbsolutePath());
        Element root = document.getRootElement();
        hasXmlBeenUpdated = false;
        String xchangeElementUuid = null;
        Iterator<Element> children = root.getChildren().iterator();
        Element child = null;
        while (children.hasNext()) {
            child = children.next();
            Attribute uuidAttribute = child.getAttribute("UUID");
            if (uuidAttribute != null) {
                if (xchangeElementUuid == null) {
                    xchangeElementUuid = uuidAttribute.getValue();
                    xchangeElementContents.put(xchangeElementUuid, new HashSet<String>());
                } else {
                    String xelementUuid = uuidAttribute.getValue();
                    HashSet<String> contents = xchangeElementContents.get(xchangeElementUuid);
                    contents.add(xelementUuid);
                }
            }
        }
    }

    private void updateLogicalContainer(File dirToUpdate) {
        for (File file : dirToUpdate.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".xpx")) {
                addLogicalContainer(file);
            }
        }
    }

    /**
     * Handle the container directory and container xpx file
     * @param creationLocation
     * @param sourceFile
     */
    private void createXchangeElementContainer(File creationLocation, File sourceFile) {
        String orignalContainerUuid = ConversionUtil.stripXPX(sourceFile);
        String newContainerUuid = getUuid(orignalContainerUuid);
        sendProgressMessage("Processing XchangeElementContainer - " + newContainerUuid);
        File dir = new File(creationLocation.getAbsoluteFile() + File.separator + newContainerUuid);
        dir.mkdirs();
        try {
            File destinationFile = new File(openDestDir + File.separator + dir.getName() + ".xpx");
            FileUtils.copyFile(sourceFile, destinationFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem creating the container xpx file for: " + orignalContainerUuid, e);
        }
        containerContents.put(newContainerUuid, new HashSet<String>());
    }

    @SuppressWarnings("unchecked")
    private boolean isUnshared() {
        Document document = conversionDexmlifier.getDocument(localSrc.getAbsolutePath());
        Element root = document.getRootElement();
        hasXmlBeenUpdated = false;
        Iterator<Element> children = root.getChildren().iterator();
        Element child = null;
        while (children.hasNext()) {
            child = children.next();
            if (child.getName().equals("repositoryURL")) {
                if ((child.getValue() != null) && (child.getValue().length() > 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getUuid(String originalUuid) {
        if (oldStaticUuidsToNewUuids.containsKey(originalUuid)) {
            return oldStaticUuidsToNewUuids.get(originalUuid);
        }
        return originalUuid;
    }

    /**
     * A map of old uuids to their new counterparts in v3
     */
    private void createOldUuidMap() {
        oldStaticUuidsToNewUuids.put("PSroot", "PS0000000000000001");
        oldStaticUuidsToNewUuids.put("RTadmin", "RT0000000000000003");
        oldStaticUuidsToNewUuids.put("RTparticipant", "RT0000000000000002");
        oldStaticUuidsToNewUuids.put("PEproject", "PE0000000000000004");
        oldStaticUuidsToNewUuids.put("PJproject", "PJ0000000000000005");
        oldStaticUuidsToNewUuids.put("TKproject", "TK0000000000000006");
        oldStaticUuidsToNewUuids.put("PEtask", "PE0000000000000007");
        oldStaticUuidsToNewUuids.put("TKtask", "TK0000000000000008");
        oldStaticUuidsToNewUuids.put("PEoverhead", "PE000000000000000a");
        oldStaticUuidsToNewUuids.put("TKoverhead", "TK0000000000000010");
        oldStaticUuidsToNewUuids.put("PEtimebox", "PE0000000000000011");
        oldStaticUuidsToNewUuids.put("PEtaskfolder", "PE0000000000000012");
        oldStaticUuidsToNewUuids.put("PEpriorityfolders", "PE0000000000000029");
        oldStaticUuidsToNewUuids.put("PEparenttask", "PE0000000000000101");
        oldStaticUuidsToNewUuids.put("TKparenttask", "TK0000000000000102");
        oldStaticUuidsToNewUuids.put("FOtimebox", "FO0000000000000013");
        oldStaticUuidsToNewUuids.put("FOtaskfolder", "FO0000000000000014");
        oldStaticUuidsToNewUuids.put("FOlowfolder", "FO0000000000000032");
        oldStaticUuidsToNewUuids.put("FOmediumfolder", "FO0000000000000031");
        oldStaticUuidsToNewUuids.put("FOhighfolder", "FO0000000000000030");
        oldStaticUuidsToNewUuids.put("ATdoc", "AT0000000000000015");
        oldStaticUuidsToNewUuids.put("SAschema", "SA0000000000000016");
        oldStaticUuidsToNewUuids.put("ATattach", "AT000000000000001a");
        oldStaticUuidsToNewUuids.put("ATmanaged", "AT000000000000001b");
        oldStaticUuidsToNewUuids.put("ATattach", "AT000000000000001c");
        oldStaticUuidsToNewUuids.put("CNexpenses", "CN0000000000000020");
        oldStaticUuidsToNewUuids.put("UAtodayAction", "UA0000000000000021");
        oldStaticUuidsToNewUuids.put("UAearliestDates", "UA0000000000000022");
        oldStaticUuidsToNewUuids.put("UAnextTimeboxName", "UA0000000000000023");
        oldStaticUuidsToNewUuids.put("UAnextTimeboxDate", "UA0000000000000024");
        oldStaticUuidsToNewUuids.put("GTchecklist", "GT0000000000000025");
        oldStaticUuidsToNewUuids.put("CAimportance", "CA0000000000000026");
        oldStaticUuidsToNewUuids.put("CYhigh", "CY0000000000000027");
        oldStaticUuidsToNewUuids.put("CYmedium", "CY0000000000000028");
        oldStaticUuidsToNewUuids.put("CYlow", "CY000000000000002a");
        oldStaticUuidsToNewUuids.put("UArunDiagnostics", "UA0000000000000030");
        oldStaticUuidsToNewUuids.put("UAhelloWorld", "UA0000000000000031");
        oldStaticUuidsToNewUuids.put("UAassignToAccountName", "UA0000000000000032");
        oldStaticUuidsToNewUuids.put("UAsetEstimates", "UA0000000000000033");
        oldStaticUuidsToNewUuids.put("UAsetRequiredRoleType", "UA0000000000000034");
        oldStaticUuidsToNewUuids.put("UAUAsetRequiredRoleType", "UA0000000000000034");
        oldStaticUuidsToNewUuids.put("ACACreferenceArtifact", "AC0000000000000999");
        oldStaticUuidsToNewUuids.put("UAsetImportance", "UA0000000000000035");
        oldStaticUuidsToNewUuids.put("UAsetNumberOfParticipants", "UA0000000000000036");
        oldStaticUuidsToNewUuids.put("UAsetProjectDates", "UA0000000000000037");
        oldStaticUuidsToNewUuids.put("UAsetTargetDates", "UA0000000000000038");
        oldStaticUuidsToNewUuids.put("UAsetDatesAndWeight", "UA000000000000003a");
        oldStaticUuidsToNewUuids.put("UAsetSizeToMatchEffort", "UA000000000000003b");
        oldStaticUuidsToNewUuids.put("EDstandard", "ED0000000000000040");
        oldStaticUuidsToNewUuids.put("EDflexi", "ED0000000000000041");
    }

    private void sendProgressMessage(String message) {
        if (progressListener != null) {
            progressListener.progressMessage(message);
        }
    }

    /**
     * An Object registers itself to receive conversion messages.
     * Only supports one listener.
     *
     * @param progressListener
     */
    public void setProgressListener(IProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void dumpContainerContents() {
        for (String key : containerContents.keySet()) {
            System.out.println("Container - " + key);
            for (String xchangeElementUuuid : containerContents.get(key)) {
                System.out.println("Contains - " + xchangeElementUuuid);
            }
        }
    }

    public void dumpContainerContainers() {
        for (String container : containerContainer.keySet()) {
            System.out.println("Container: " + container + " contained in - " + containerContainer.get(container));
        }
    }

    public void dumpXchangeElementContents() {
        for (String key : xchangeElementContents.keySet()) {
            System.out.println("XchangeElement - " + key);
            for (String xelementUuid : xchangeElementContents.get(key)) {
                System.out.println("Contains - " + xelementUuid);
            }
        }
    }

    public void dumpArtifacts() {
        for (String artifactUuid : artifacts.keySet()) {
            System.out.println("Artifact: - " + artifactUuid + " now stored in - " + artifacts.get(artifactUuid));
        }
    }

    @SuppressWarnings("unchecked")
    private void addLogicalContainer(File file) {
        Document document = conversionDexmlifier.getDocument(file.getAbsolutePath());
        Element root = document.getRootElement();
        hasXmlBeenUpdated = false;
        Iterator<Element> children = root.getChildren().iterator();
        Element child = null;
        while (children.hasNext()) {
            child = children.next();
            Attribute uuidAttribute = child.getAttribute("UUID");
            if (uuidAttribute != null) {
                String xchangeElementUuid = getUuid(uuidAttribute.getValue());
                sendProgressMessage("Add logical container to - " + xchangeElementUuid);
                Element logicalContainerElement = new Element("LOGICAL_CONTAINER");
                logicalContainerElement.setAttribute("type", "REFERENCE");
                String logicalContainerUuid = getContainerUuid(xchangeElementUuid);
                if (logicalContainerUuid != null) {
                    if (!logicalContainerUuid.equals("open")) {
                        logicalContainerElement.setText(logicalContainerUuid);
                        child.addContent(1, logicalContainerElement);
                        hasXmlBeenUpdated = true;
                        break;
                    }
                } else {
                    System.err.println("Cannot locate container uuid for container - " + xchangeElementUuid);
                }
            }
        }
        if (hasXmlBeenUpdated) {
            FileOutputStream elementOutput;
            try {
                elementOutput = new FileOutputStream(file);
                conversionXmlifier.write(document, elementOutput, true);
                elementOutput.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Problem persisting xelement to file : " + file, e);
                return;
            }
        }
    }

    /**
     * Copy across the local dir from the source and the datasource.xml file
     */
    private void copyBaseComponents() {
        try {
            localDestDir.mkdirs();
            FileUtils.copyFile(localSrc, new File(localDestDir + File.separator + "local.xml"));
            FileUtils.copyFile(dataSourceSrc, new File(destinationDirectory + File.separator + "datasource.xml"));
            for (File file : localSrcDir.listFiles()) {
                if (file.isDirectory()) {
                    if (file.getName().equals("limbo")) {
                        System.err.println("Limbo directory found, and has been excluded from the conversion");
                    }
                    if (file.getName().equals("orphans")) {
                        System.err.println("Orphan directory found, and has been excluded from the conversion");
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem copying the base components", e);
        }
    }

    private void updateMinimumCompatibleVersion() {
        boolean hasBeenUpdated = false;
        Document document = conversionDexmlifier.getDocument(dataSourceDest.getAbsolutePath());
        Element root = document.getRootElement();
        Element minimumClientVersionElement = root.getChild("minimumClientVersion");
        if (minimumClientVersionElement != null) {
            minimumClientVersionElement.setText(V3VERSION);
            hasBeenUpdated = true;
        }
        if (hasBeenUpdated) {
            FileOutputStream elementOutput;
            try {
                elementOutput = new FileOutputStream(dataSourceDest);
                conversionXmlifier.write(document, elementOutput, true);
                elementOutput.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Problem writing datasource xml to : " + dataSourceSrc, e);
                return;
            }
        }
    }

    private String getContainerUuid(String containerUuid) {
        return containerContainer.get(containerUuid);
    }

    private void updateReferences(File file) {
        for (File subfile : file.listFiles()) {
            if (subfile.isDirectory()) {
                updateReferences(subfile);
            } else {
                updateReference(subfile);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateReference(File file) {
        elementsToRemove = new HashMap<Element, ArrayList<Element>>();
        newChoicesToCreate = new HashSet<String>();
        document = conversionDexmlifier.getDocument(file.getAbsolutePath());
        Element root = document.getRootElement();
        hasXmlBeenUpdated = false;
        Attribute versionAttribute = root.getAttribute("VERSION");
        if (versionAttribute == null) {
            root.setAttribute("VERSION", "3.0.0_15291");
        } else {
            versionAttribute.setValue("3.0.0_15291");
        }
        hasXmlBeenUpdated = true;
        Iterator<Element> topLevelChildren = root.getChildren().iterator();
        Element topElement = null;
        while (topLevelChildren.hasNext()) {
            topElement = topLevelChildren.next();
            Attribute uuidAttribute = topElement.getAttribute("UUID");
            if (uuidAttribute != null) {
                String uuid = uuidAttribute.getValue();
                uuid = getUuid(uuid);
                if (uuid.startsWith("CC")) {
                    topElement.setName("CO");
                    uuid = uuid.replaceFirst("CC", "CO");
                    uuidAttribute.setValue(uuid);
                    logger.info("Converted CC to " + uuid);
                }
                if (uuid.startsWith("CO")) {
                    Iterator<Element> coChildren = topElement.getChildren().iterator();
                    while (coChildren.hasNext()) {
                        Element coElement = coChildren.next();
                        if (coElement.getName().equals("PATTERN")) {
                            String patternUuid = coElement.getValue();
                            newChoicesToCreate.add(patternUuid);
                            addElementToBeRemoved(topElement, coElement);
                        }
                    }
                }
            }
            String xchangeElementUuid = uuidAttribute.getValue();
            String referenceId = getUuid(xchangeElementUuid);
            if (!xchangeElementUuid.equals(referenceId)) {
                uuidAttribute.setValue(referenceId);
                hasXmlBeenUpdated = true;
            }
            if (uuidAttribute != null) {
                boolean flag = processXML(topElement);
                if (flag) {
                    hasXmlBeenUpdated = true;
                }
            }
        }
        if (newChoicesToCreate.size() > 0) {
            for (String patternUuid : newChoicesToCreate) {
                logger.info("Create new Choice for - " + patternUuid);
                Element newCHElement = new Element("CH");
                newCHElement.setAttribute("UUID", "CH" + UuidUtils.getUUID());
                root.addContent(newCHElement);
                Element newPatternElement = new Element("PATTERN");
                newPatternElement.setAttribute("type", "REFERENCE");
                patternUuid = getUuid(patternUuid);
                newPatternElement.setText(patternUuid);
                newCHElement.addContent(newPatternElement);
                hasXmlBeenUpdated = true;
            }
        }
        if (elementsToRemove.size() > 0) {
            for (Element parentElement : elementsToRemove.keySet()) {
                for (Element childElementToRemove : elementsToRemove.get(parentElement)) {
                    logger.info("Removing blank or redundant reference - " + childElementToRemove.getName() + " from - " + parentElement.getName());
                    parentElement.removeChild(childElementToRemove.getName());
                    hasXmlBeenUpdated = true;
                }
            }
        }
        if (hasXmlBeenUpdated) {
            FileOutputStream elementOutput;
            try {
                elementOutput = new FileOutputStream(file);
                conversionXmlifier.write(document, elementOutput, true);
                elementOutput.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Problem persisting xelement to file : " + file, e);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean processXML(Element childElement) {
        boolean modifiedXML = false;
        Iterator<Element> mainBodyChildren = childElement.getChildren().iterator();
        while (mainBodyChildren.hasNext()) {
            Element element = mainBodyChildren.next();
            if (!element.getName().equals(XchangeElementContainer.LOGICAL_CONTAINER)) {
                if (element.getName().equals(XchangeElementContainer.DEFAULT_EDITOR)) {
                    addElementToBeRemoved(childElement, element);
                } else {
                    Attribute typeAttribute = element.getAttribute("type");
                    if (typeAttribute != null) {
                        String type = typeAttribute.getValue();
                        if (type.equals(PropertyType.REFERENCE.toString())) {
                            String referenceUuid = getUuid(element.getValue());
                            if (referenceUuid.length() == 0) {
                                addElementToBeRemoved(childElement, element);
                            } else {
                                referenceUuid = getReference(referenceUuid);
                                boolean flag = writeReference(referenceUuid, element);
                                if (flag) {
                                    modifiedXML = true;
                                }
                            }
                        }
                        if (type.equals(PropertyType.REFERENCESET.toString())) {
                            Iterator<Element> uuidChildren = element.getChildren().iterator();
                            ArrayList<Element> uuidElements = new ArrayList<Element>();
                            while (uuidChildren.hasNext()) {
                                Element uuidElement = uuidChildren.next();
                                if (uuidElement.getName().equals("UUID")) {
                                    uuidElements.add(uuidElement);
                                }
                            }
                            for (Element uuidelement : uuidElements) {
                                String referenceUuid = getUuid(uuidelement.getValue());
                                referenceUuid = getReference(referenceUuid);
                                boolean flag = writeReference(referenceUuid, uuidelement);
                                if (flag) {
                                    modifiedXML = true;
                                }
                            }
                        }
                    }
                }
                if (element.getName().equals(Artifact.MANAGED_PATH)) {
                    String managedPath = element.getText();
                    String filename = managedPath.substring(managedPath.lastIndexOf("/") + 1);
                    String newPath = artifacts.get(filename);
                    if ((newPath != null) && !newPath.equals(managedPath)) {
                        element.setText(newPath);
                        modifiedXML = true;
                    }
                }
            }
            if (element.getChildren().size() > 0) {
                processXML(element);
            }
        }
        return modifiedXML;
    }

    private void addElementToBeRemoved(Element parentElement, Element elementToBeRemoved) {
        if (elementsToRemove.containsKey(parentElement)) {
            ArrayList<Element> removeList = elementsToRemove.get(parentElement);
            removeList.add(elementToBeRemoved);
        } else {
            ArrayList<Element> removeList = new ArrayList<Element>();
            removeList.add(elementToBeRemoved);
            elementsToRemove.put(parentElement, removeList);
        }
    }

    private String getReference(String referenceUuid) {
        String referenceId = null;
        if (referenceUuid.length() > 0) {
            if (isContainerUuid(referenceUuid)) {
                referenceId = referenceUuid;
            } else {
                referenceId = getReferenceId(referenceUuid);
                if (referenceId == null) {
                    referenceId = referenceUuid;
                }
            }
        }
        return referenceId;
    }

    private boolean writeReference(String referenceId, Element element) {
        if ((referenceId != null) && (referenceId.length() != 0)) {
            element.setText(referenceId);
            return true;
        }
        return false;
    }

    private String getReferenceId(String uuid) {
        for (String xchangeElementUuid : xchangeElementContents.keySet()) {
            for (String xelementUuid : xchangeElementContents.get(xchangeElementUuid)) {
                if (xelementUuid.equals(uuid)) {
                    return uuid + getUuid(xchangeElementUuid);
                }
            }
        }
        for (String containerUuid : containerContents.keySet()) {
            HashSet<String> contents = containerContents.get(containerUuid);
            for (String xchangeElementUuid : contents) {
                if (xchangeElementUuid.equals(uuid)) {
                    return uuid + getUuid(containerUuid);
                }
            }
        }
        return null;
    }

    private boolean isContainerUuid(String uuid) {
        for (String containerUuid : containerContents.keySet()) {
            if (containerUuid.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a flat artifacts directory.
     *
     * @param file
     */
    private void processArtifacts(File file) {
        if (!file.exists()) {
            return;
        }
        if (!artifactsDestDir.exists()) {
            artifactsDestDir.mkdirs();
        }
        for (File artifactFile : file.listFiles()) {
            if (artifactFile.isDirectory()) {
                File artifactFileDir = new File(artifactsDestDir + File.separator + artifactFile.getName());
                artifactFileDir.mkdir();
                processArtifacts(artifactFile);
            } else {
                File destinationFile = new File(artifactsDestDir + File.separator + artifactFile.getParentFile().getName() + File.separator + artifactFile.getName());
                try {
                    FileUtils.copyFile(artifactFile, destinationFile);
                    artifacts.put(artifactFile.getName(), File.separator + artifactFile.getParentFile().getName() + File.separator + artifactFile.getName());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Problem copying artifact: " + destinationFile, e);
                }
            }
        }
    }

    public static boolean needsConvertingToV3(Version datasourceVersion) {
        Version v3Version = new Version("3.0.0_15291");
        if (datasourceVersion.compareTo(v3Version) == -1) {
            Version v25Version = new Version("2.5.0.8810");
            int compareValue = datasourceVersion.compareTo(v25Version);
            if ((compareValue == 0) || (compareValue == 1)) {
                return true;
            }
        }
        if (datasourceVersion.toString().equals("0.0.0_0")) {
            return true;
        }
        return false;
    }
}
