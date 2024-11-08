package com.tomecode.mjprocessor.execute;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.apache.maven.plugin.logging.Log;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultElement;
import org.xml.sax.InputSource;
import com.tomecode.mjprocessor.assembly.Assembly;
import com.tomecode.mjprocessor.assembly.Project;
import com.tomecode.mjprocessor.assembly.action.Action;
import com.tomecode.mjprocessor.assembly.action.ActionType;
import com.tomecode.mjprocessor.assembly.type.ProjectActions;
import com.tomecode.mjprocessor.assembly.type.ProcessType;

/**
 * Execute
 * 
 * @author Frastia Tomas
 */
public final class AssemblyExecutor {

    private final Log log;

    private final Properties properties;

    private final Assembly assembly;

    public AssemblyExecutor(Log log, Assembly assembly, Properties properties) {
        this.log = log;
        this.properties = properties;
        this.assembly = assembly;
    }

    /**
     * execute current processing for current profile
     * 
     * @param groupId
     * @param artifactId
     * @param profile profile arrays
     * @throws MJProcessorException {@link ProjectNotFoundException}
     */
    public final void execute(String groupId, String artifactId, String[] profiles) throws MJProcessorException {
        Project project = findProject(assembly.getProjects(), groupId, artifactId);
        if (profiles != null && profiles.length != 0) {
            for (String profile : profiles) {
                for (ProjectActions basicType : project.getTypes()) {
                    if (basicType.isProfile(profile)) {
                        if (basicType.getType() == ProcessType.XML) {
                            executeXml(basicType);
                        } else if (basicType.getType() == ProcessType.PROPERTIES) {
                            executeProperties(basicType);
                        }
                    }
                }
            }
        } else {
            for (ProjectActions basicType : project.getTypes()) {
                if (basicType.getType() == ProcessType.XML) {
                    executeXml(basicType);
                } else if (basicType.getType() == ProcessType.PROPERTIES) {
                    executeProperties(basicType);
                }
            }
        }
    }

    /**
     * execute actions on properties
     * 
     * @param basicType
     * @throws MJProcessorException
     */
    private final void executeProperties(ProjectActions basicType) throws MJProcessorException {
        copyTargetFileToSourceFile(basicType.getSourceFile(), basicType.getTargetFile());
        Properties properties = loadProperties(basicType.getTargetFile());
        for (Action action : basicType.getActions()) {
            if (action.getActionType() == ActionType.INSERT) {
                properties.put(action.getPropertyName(), resolvePropertyValue(action.getValue()));
            } else if (action.getActionType() == ActionType.REMOVE) {
                properties.remove(action.getPropertyName());
            } else if (action.getActionType() == ActionType.REPLACE) {
                properties.put(action.getPropertyName(), resolvePropertyValue(action.getValue()));
            }
        }
        saveProperties(properties, basicType.getTargetFile());
    }

    /**
     * save properties
     * 
     * @param properties
     * @param targetFile
     * @throws MJProcessorException
     */
    private final void saveProperties(Properties properties, File file) throws MJProcessorException {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            Enumeration<Object> enumeration = properties.keys();
            while (enumeration.hasMoreElements()) {
                String key = enumeration.nextElement().toString();
                String value = properties.getProperty(key);
                bufferedWriter.write(key + "=" + value);
                if (enumeration.hasMoreElements()) {
                    bufferedWriter.newLine();
                }
            }
        } catch (IOException e) {
            throw new MJProcessorException(e.getMessage(), e);
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    throw new MJProcessorException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * load {@link Properties}
     * 
     * @param file contains properties
     * @return
     * @throws MJProcessorException
     */
    private final Properties loadProperties(File file) throws MJProcessorException {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new MJProcessorException(e.getMessage(), e);
        }
        return properties;
    }

    /**
     * find project in {@link Assembly#getProjects()}
     * 
     * @param projects
     * @param groupId
     * @param artifactId
     * @return
     * @throws ProjectNotFoundException if not found project
     */
    private final Project findProject(List<Project> projects, String groupId, String artifactId) throws ProjectNotFoundException {
        for (Project project : projects) {
            if (project.getGroupId().equals(groupId) && project.getArtifactId().equals(artifactId)) {
                return project;
            }
        }
        throw new ProjectNotFoundException(groupId, artifactId);
    }

    /**
     * execute actions in target file
     * 
     * @param basicType
     * @throws MJProcessorException
     */
    private final void executeXml(ProjectActions basicType) throws MJProcessorException {
        copyTargetFileToSourceFile(basicType.getSourceFile(), basicType.getTargetFile());
        Document targetDoc = parseXmlDocument(basicType.getTargetFile());
        for (Action action : basicType.getActions()) {
            if (action.getActionType() == ActionType.REPLACE) {
                DefaultElement node = (DefaultElement) targetDoc.selectSingleNode(action.getXPath());
                replaceNode(node, action.getValue());
            } else if (action.getActionType() == ActionType.INSERT) {
                DefaultElement node = (DefaultElement) targetDoc.selectSingleNode(action.getXPath());
                insertNode(node, action.getValue());
            } else if (action.getActionType() == ActionType.REMOVE) {
                DefaultElement node = (DefaultElement) targetDoc.selectSingleNode(action.getXPath());
                node.getParent().remove(node);
            }
        }
        saveDocument(targetDoc, basicType.getTargetFile());
    }

    /**
     * copy target file to source file
     * 
     * @param sourceFile
     * @param targetFile
     * @throws MJProcessorException
     */
    private final void copyTargetFileToSourceFile(File sourceFile, File targetFile) throws MJProcessorException {
        if (!targetFile.exists()) {
            targetFile.getParentFile().mkdirs();
            try {
                if (!targetFile.exists()) {
                    targetFile.createNewFile();
                }
            } catch (IOException e) {
                throw new MJProcessorException(e.getMessage(), e);
            }
        }
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(sourceFile).getChannel();
            out = new FileOutputStream(targetFile).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                log.error(e);
            }
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    /**
     * replace node value
     * 
     * @param affectedNode
     * @param value
     */
    private final void replaceNode(Element affectedNode, String value) {
        int indexStart = value.indexOf("${");
        int indexEnd = value.indexOf("}");
        if (indexStart != -1 && indexEnd != -1 && indexStart <= indexEnd) {
            affectedNode.setText(resolvePropertyValue(indexStart + 2, indexEnd, value));
        } else if (value.contains("<") || value.contains("/>") || value.contains("</")) {
            Document elementValue = parseTempXml(value);
            if (elementValue == null) {
                affectedNode.setText(value);
            } else {
                Element element = affectedNode.getParent();
                element.remove(affectedNode);
                element.add(elementValue.getRootElement());
                elementValue.getRootElement().setParent(element);
            }
        } else {
            affectedNode.setText(value);
        }
    }

    /**
     * resolve property value
     * 
     * @param indexStart
     * @param indexEnd
     * @param value
     * @return new value
     */
    private final String resolvePropertyValue(int indexStart, int indexEnd, String value) {
        String key = value.substring(indexStart, indexEnd);
        String keyValue = (String) properties.getProperty(key);
        if (keyValue == null) {
            keyValue = "";
        }
        return value.substring(0, indexStart - 2) + keyValue + value.substring(indexEnd, value.length() - 1);
    }

    /**
     * resolve property value
     * 
     * @param value
     * @return
     */
    private final String resolvePropertyValue(String value) {
        int indexStart = value.indexOf("${");
        int indexEnd = value.indexOf("}");
        if (indexStart != -1 && indexEnd != -1 && indexStart <= indexEnd) {
            return resolvePropertyValue(indexStart + 2, indexEnd, value);
        }
        return value;
    }

    /**
     * insert new node
     * 
     * @param affectedNode
     * @param value
     */
    private void insertNode(Element affectedNode, String value) {
        int indexStart = value.indexOf("${");
        int indexEnd = value.indexOf("}");
        if (indexStart != -1 && indexEnd != -1 && indexStart <= indexEnd) {
        } else if (value.contains("<") || value.contains("/>") || value.contains("</")) {
            Document elementValue = parseTempXml(value);
            if (elementValue == null) {
                affectedNode.setText(value);
            } else {
                affectedNode.add(elementValue.getRootElement());
                elementValue.getRootElement().setParent(affectedNode);
            }
        } else {
            affectedNode.setText(value);
        }
    }

    /***
     * save {@link Document} to file
     * 
     * @param document
     * @param file target file
     * @throws MJProcessorException
     */
    private final void saveDocument(Document document, File file) throws MJProcessorException {
        FileOutputStream fos = null;
        XMLWriter writer = null;
        try {
            fos = new FileOutputStream(file);
            writer = new XMLWriter(fos);
            writer.write(document);
            writer.flush();
            writer.close();
            fos.close();
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * parse file to {@link Document}
     * 
     * @param file
     * @return
     * @throws MJProcessorException if failed parse
     */
    private final org.dom4j.Document parseXmlDocument(File file) throws MJProcessorException {
        try {
            SAXReader saxReader = new SAXReader();
            return saxReader.read(file);
        } catch (DocumentException e) {
            throw new MJProcessorException(e.getMessage(), e);
        }
    }

    /**
     * parse string to dom document
     * 
     * @param value
     * @return
     */
    private final Document parseTempXml(String value) {
        try {
            return new SAXReader().read(new InputSource(new StringReader(value.trim())));
        } catch (DocumentException e) {
            log.error(e);
        }
        return null;
    }
}
