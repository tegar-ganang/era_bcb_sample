package org.tolven.assembler.jboss.tomcatserver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.PluginDescriptor;
import org.tolven.plugin.TolvenCommandPlugin;
import org.tolven.tools.ant.TolvenDependSet;

/**
 * This plugin assmebles the tomcat server.xml by collecting information about the connectors.
 * 
 * @author Joseph Isaac
 *
 */
public class TomcatServerXMLAssembler extends TolvenCommandPlugin {

    private static final String ATTRIBUTE_TEMPLATE_SERVERXML = "template-serverxml";

    public static final String EXTENSIONPOINT_CONNECTOR = "connector";

    public static final String EXTENSIONPOINT_SERVERXML_CONSUMER = "serverxmlConsumer";

    public static final String EXTENSION_TOMCATSERVERXML_ASSEMBLY = "tomcatserverxmlAssembly";

    private Logger logger = Logger.getLogger(TomcatServerXMLAssembler.class);

    @Override
    protected void doStart() throws Exception {
        logger.debug("*** start ***");
    }

    @Override
    public void execute(String[] args) throws Exception {
        logger.debug("*** execute ***");
        File[] tmpFiles = getPluginTmpDir().listFiles();
        if (tmpFiles != null && tmpFiles.length > 0) {
            return;
        }
        Extension extension = getDescriptor().getExtension(EXTENSION_TOMCATSERVERXML_ASSEMBLY);
        String connectorPluginId = extension.getExtendedPluginId();
        PluginDescriptor connectorPluginDescriptor = getManager().getRegistry().getPluginDescriptor(connectorPluginId);
        ExtensionPoint tomcatserverxmlAssemblyExtensionPoint = getManager().getRegistry().getExtensionPoint(connectorPluginDescriptor.getId(), extension.getExtendedPointId());
        String connectorExtensionPointId = tomcatserverxmlAssemblyExtensionPoint.getParameterDefinition("connectorDefinition").getDefaultValue();
        ExtensionPoint connectorExtensionPoint = connectorPluginDescriptor.getExtensionPoint(connectorExtensionPointId);
        assembleServerxml(connectorExtensionPoint);
    }

    protected void assembleServerxml(ExtensionPoint connectorExtensionPoint) throws IOException, XMLStreamException {
        ExtensionPoint serverxmlConsumerExtensionPoint = getDescriptor().getExtensionPoint(EXTENSIONPOINT_SERVERXML_CONSUMER);
        for (Extension serverxmlConsumerExtension : serverxmlConsumerExtensionPoint.getConnectedExtensions()) {
            PluginDescriptor serverxmlConsumerPluginDescriptor = serverxmlConsumerExtension.getDeclaringPluginDescriptor();
            String destinationServerxml = serverxmlConsumerExtension.getParameter("serverxml").valueAsString();
            File destinationXMLFile = new File(getPluginTmpDir(serverxmlConsumerPluginDescriptor), destinationServerxml);
            if (isProcessingRequired(connectorExtensionPoint, destinationXMLFile)) {
                destinationXMLFile.getParentFile().mkdirs();
                String templateFilename = getDescriptor().getAttribute(ATTRIBUTE_TEMPLATE_SERVERXML).getValue();
                File templateFile = getFilePath(templateFilename);
                if (!templateFile.exists()) {
                    throw new RuntimeException("Could not locate: '" + templateFile.getPath() + "' in " + getDescriptor().getId());
                }
                StringBuffer originalXML = new StringBuffer();
                logger.debug("Read " + templateFile.getPath());
                originalXML.append(FileUtils.readFileToString(templateFile));
                String xslt = getXSLT(connectorExtensionPoint);
                File xsltFile = new File(getPluginTmpDir(), "serverxml-xslt.xml");
                logger.debug("Write xslt file " + xsltFile.getPath());
                FileUtils.writeStringToFile(xsltFile, xslt);
                String translatedXMLString = getTranslatedXML(originalXML.toString(), xslt);
                destinationXMLFile.getParentFile().mkdirs();
                logger.debug("Write translated server.xml file to " + destinationXMLFile);
                FileUtils.writeStringToFile(destinationXMLFile, translatedXMLString);
            }
        }
    }

    protected boolean isProcessingRequired(ExtensionPoint connectorExtensionPoint, File productFile) {
        if (!productFile.exists()) {
            logger.debug(productFile.getPath() + " does not exist, so it will be created");
            return true;
        }
        Set<File> pluginFiles = new HashSet<File>();
        for (Extension extension : connectorExtensionPoint.getConnectedExtensions()) {
            pluginFiles.add(getPluginZip(extension.getDeclaringPluginDescriptor()));
            pluginFiles.add(getPluginTmpDir(extension.getDeclaringPluginDescriptor()));
        }
        if (!pluginFiles.isEmpty()) {
            TolvenDependSet.process(pluginFiles, productFile);
        }
        if (productFile.exists()) {
            logger.debug(productFile.getPath() + " is more recent than any of its source files");
            return false;
        } else {
            logger.debug(productFile.getPath() + " was removed since its source files are more recent");
            return true;
        }
    }

    protected String getXSLT(ExtensionPoint connectorExtensionPoint) throws XMLStreamException, IOException {
        StringWriter xslt = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlStreamWriter = null;
        try {
            xmlStreamWriter = factory.createXMLStreamWriter(xslt);
            xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
            xmlStreamWriter.writeCharacters("\n");
            xmlStreamWriter.writeStartElement("xsl:stylesheet");
            xmlStreamWriter.writeAttribute("version", "2.0");
            xmlStreamWriter.writeNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
            xmlStreamWriter.writeCharacters("\n");
            xmlStreamWriter.writeStartElement("xsl:output");
            xmlStreamWriter.writeAttribute("method", "xml");
            xmlStreamWriter.writeAttribute("indent", "yes");
            xmlStreamWriter.writeAttribute("encoding", "UTF-8");
            xmlStreamWriter.writeAttribute("omit-xml-declaration", "no");
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeCharacters("\n");
            addMainTemplate(xmlStreamWriter);
            addServiceTemplate(connectorExtensionPoint, xmlStreamWriter);
            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.writeEndDocument();
        } finally {
            if (xmlStreamWriter != null) {
                xmlStreamWriter.close();
            }
        }
        return xslt.toString();
    }

    protected void addMainTemplate(XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("xsl:template");
        xmlStreamWriter.writeAttribute("match", "/ | * | @* | text() | comment()");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:copy");
        xmlStreamWriter.writeAttribute("select", ".");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:apply-templates");
        xmlStreamWriter.writeAttribute("select", "* | @* | text() | comment()");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
    }

    protected void addServiceTemplate(ExtensionPoint connectorExtensionPoint, XMLStreamWriter xmlStreamWriter) throws XMLStreamException, IOException {
        xmlStreamWriter.writeStartElement("xsl:template");
        xmlStreamWriter.writeAttribute("match", "Service");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:element");
        xmlStreamWriter.writeAttribute("name", "{name()}");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:for-each");
        xmlStreamWriter.writeAttribute("select", "@*");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:attribute");
        xmlStreamWriter.writeAttribute("name", "{name()}");
        xmlStreamWriter.writeStartElement("xsl:value-of");
        xmlStreamWriter.writeAttribute("select", ".");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:value-of");
        xmlStreamWriter.writeAttribute("select", "text()");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        Extension extension = getDescriptor().getExtension(EXTENSION_TOMCATSERVERXML_ASSEMBLY);
        String connectorPluginId = extension.getExtendedPluginId();
        PluginDescriptor connectorPluginDescriptor = getManager().getRegistry().getPluginDescriptor(connectorPluginId);
        ExtensionPoint tomcatserverxmlAssemblyExtensionPoint = getManager().getRegistry().getExtensionPoint(connectorPluginDescriptor.getId(), extension.getExtendedPointId());
        String webserverCredentialDirname = (String) evaluate(tomcatserverxmlAssemblyExtensionPoint.getParameterDefinition("webserverCredentialDirectory").getDefaultValue(), tomcatserverxmlAssemblyExtensionPoint.getDeclaringPluginDescriptor());
        File webserverCredentialDir = new File(webserverCredentialDirname);
        if (!isAbsoluteFilePath(webserverCredentialDir)) {
            throw new RuntimeException("The webserver credential must be an absolute path " + webserverCredentialDir.getPath());
        }
        webserverCredentialDir.mkdirs();
        logger.debug("WebServer credential directory will be: " + webserverCredentialDir.getPath());
        String webserverDeploymentDirname = (String) evaluate(tomcatserverxmlAssemblyExtensionPoint.getParameterDefinition("webserverDeploymentDirectory").getDefaultValue(), tomcatserverxmlAssemblyExtensionPoint.getDeclaringPluginDescriptor());
        File webserverDeploymentDir = new File(webserverDeploymentDirname);
        if (!isAbsoluteFilePath(webserverDeploymentDir)) {
            throw new RuntimeException("The webserver deployment directory must be an absolute path " + webserverDeploymentDir.getPath());
        }
        logger.debug("WebServer deployment directory in the server.xml will be: " + webserverDeploymentDir.getPath());
        for (Extension connectorExtension : connectorExtensionPoint.getConnectedExtensions()) {
            addConnectorTemplate(connectorExtension, webserverCredentialDir, webserverDeploymentDir, xmlStreamWriter);
        }
        xmlStreamWriter.writeStartElement("xsl:copy-of");
        xmlStreamWriter.writeAttribute("select", "*");
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeStartElement("xsl:apply-templates");
        xmlStreamWriter.writeAttribute("select", "* | @* | text() | comment()");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
        xmlStreamWriter.writeEndElement();
        xmlStreamWriter.writeCharacters("\n");
    }

    protected void addConnectorTemplate(Extension connectorExtension, File webserverCredentialDir, File webserverDeploymentDir, XMLStreamWriter xmlStreamWriter) throws XMLStreamException, IOException {
        Properties props = getConnectorProperties(connectorExtension, webserverCredentialDir, webserverDeploymentDir);
        if (!props.isEmpty()) {
            List<Object> objs = new ArrayList<Object>();
            objs.addAll(props.keySet());
            Comparator<Object> comparator = new Comparator<Object>() {

                public int compare(Object obj1, Object obj2) {
                    return (obj1.toString().compareTo(obj2.toString()));
                }

                ;
            };
            Collections.sort(objs, comparator);
            xmlStreamWriter.writeStartElement("Connector");
            for (Object obj : objs) {
                String key = (String) obj;
                xmlStreamWriter.writeAttribute(key, props.getProperty(key));
            }
            xmlStreamWriter.writeEndElement();
        }
    }

    protected Properties getConnectorProperties(Extension connectorExtension, File webserverCredentialDir, File webserverDeploymentDir) throws IOException {
        Properties props = new Properties();
        PluginDescriptor pluginDescriptor = connectorExtension.getDeclaringPluginDescriptor();
        String evaluatedProperty = null;
        if (connectorExtension.getParameter("port") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("port").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("port", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("address") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("address").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("address", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("maxThreads") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("maxThreads").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("maxThreads", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("maxHttpHeaderSize") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("maxHttpHeaderSize").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("maxHttpHeaderSize", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("emptySessionPath") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("emptySessionPath").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("emptySessionPath", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("protocol") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("protocol").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("protocol", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("enableLookups") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("enableLookups").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("enableLookups", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("redirectPort") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("redirectPort").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("redirectPort", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("acceptCount") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("acceptCount").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("acceptCount", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("connectionTimeout") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("connectionTimeout").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("connectionTimeout", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("disableUploadTimeout") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("disableUploadTimeout").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("disableUploadTimeout", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("SSLEnabled") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("SSLEnabled").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("SSLEnabled", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("scheme") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("scheme").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("scheme", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("secure") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("secure").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("secure", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("clientAuth") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("clientAuth").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("clientAuth", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("sslProtocol") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("sslProtocol").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("sslProtocol", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("keystoreFile") != null) {
            PluginDescriptor connectorPluginDescriptor = connectorExtension.getDeclaringPluginDescriptor();
            String sourceKeyStoreFilename = (String) evaluate(connectorExtension.getParameter("keystoreFile").valueAsString(), pluginDescriptor);
            if (sourceKeyStoreFilename != null) {
                File sourceKeyStoreFile = null;
                if (new File(sourceKeyStoreFilename).getPath().equals(new File(sourceKeyStoreFilename).getAbsolutePath())) {
                    sourceKeyStoreFile = new File(sourceKeyStoreFilename);
                } else {
                    sourceKeyStoreFile = getFilePath(connectorPluginDescriptor, sourceKeyStoreFilename);
                }
                logger.debug("Copy " + sourceKeyStoreFile.getPath() + " to " + webserverCredentialDir.getPath());
                FileUtils.copyFileToDirectory(sourceKeyStoreFile, webserverCredentialDir);
                File finalDeployedKeyStoreFile = new File(webserverDeploymentDir, sourceKeyStoreFile.getName());
                props.put("keystoreFile", escapeCurlyBraces(finalDeployedKeyStoreFile.getPath().replace("\\", "/")));
            }
        }
        if (connectorExtension.getParameter("keystoreType") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("keystoreType").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("keystoreType", escapeCurlyBraces(evaluatedProperty));
            }
        }
        if (connectorExtension.getParameter("keystorePass") != null) {
            evaluatedProperty = (String) evaluate(connectorExtension.getParameter("keystorePass").valueAsString(), pluginDescriptor);
            if (evaluatedProperty != null) {
                props.put("keystorePass", escapeCurlyBraces(evaluatedProperty));
            }
        }
        return props;
    }

    @Override
    protected void doStop() throws Exception {
        logger.debug("*** stop ***");
    }
}
