package com.sitescape.team.module.extension.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.AbstractAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import com.sitescape.team.ObjectKeys;
import com.sitescape.team.context.request.BaseSessionContext;
import com.sitescape.team.context.request.RequestContext;
import com.sitescape.team.context.request.RequestContextHolder;
import com.sitescape.team.module.definition.DefinitionService;
import com.sitescape.team.module.extension.ExtensionDeployNotifier;
import com.sitescape.team.module.extension.ExtensionDeployer;
import com.sitescape.team.module.template.TemplateService;
import com.sitescape.team.module.zone.ZoneModule;
import com.sitescape.team.util.SZoneConfig;

/**
 * @author dml
 * 
 * Listens for and deploys jar-based extensions.
 * 
 */
public class WarExtensionDeployer<S extends ExtensionDeployNotifier<S>> implements ExtensionDeployer<S> {

    private static final Logger log = LoggerFactory.getLogger(WarExtensionDeployer.class);

    private DefinitionService definitionModule;

    private TemplateService templateService;

    private ZoneModule zoneModule;

    private String configurationFileExtension = "xml";

    private Namespace schemaInstanceNamespace = new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    private String schemaLocationAttribute = "schemaLocation";

    private QName schemaAttribute = new QName(schemaLocationAttribute, schemaInstanceNamespace);

    private String definitionsSchemaNamespace = "http://kablink.org/definition";

    private String templateSchemaNamespace = "http://kablink.org/template";

    private String extensionAttr = ObjectKeys.XTAG_ATTRIBUTE_EXTENSION;

    private String infPrefix = "WEB-INF";

    private File extensionBaseDir;

    private File extensionWebDir;

    public void onNotification(S source, File event) {
        deploy(event);
    }

    public void deploy(final File extension) {
        log.info("Deploying new extension from {}", extension.getPath());
        RequestContextHolder.setRequestContext(new RequestContext(SZoneConfig.getDefaultZoneName(), SZoneConfig.getAdminUserName(SZoneConfig.getDefaultZoneName()), new BaseSessionContext()));
        RequestContextHolder.getRequestContext().resolve();
        JarInputStream warIn;
        try {
            warIn = new JarInputStream(new FileInputStream(extension), true);
        } catch (IOException e) {
            log.warn("Unable to open extension WAR at " + extension.getPath(), e);
            return;
        }
        SAXReader reader = new SAXReader(false);
        reader.setIncludeExternalDTDDeclarations(false);
        String extensionPrefix = extension.getName().substring(0, extension.getName().lastIndexOf("."));
        File extensionDir = new File(extensionBaseDir, extensionPrefix);
        extensionDir.mkdirs();
        File extensionWebDir = new File(this.extensionWebDir, extensionPrefix);
        extensionWebDir.mkdirs();
        try {
            for (JarEntry entry = warIn.getNextJarEntry(); entry != null; entry = warIn.getNextJarEntry()) {
                File inflated = new File(entry.getName().startsWith(infPrefix) ? extensionDir : extensionWebDir, entry.getName());
                if (entry.isDirectory()) {
                    log.debug("Creating directory at {}", inflated.getPath());
                    inflated.mkdirs();
                    continue;
                }
                inflated.getParentFile().mkdirs();
                FileOutputStream entryOut = new FileOutputStream(inflated);
                if (!entry.getName().endsWith(configurationFileExtension)) {
                    log.debug("Inflating file resource to {}", inflated.getPath());
                    IOUtils.copy(warIn, entryOut);
                    entryOut.close();
                    continue;
                }
                try {
                    final Document document = reader.read(new TeeInputStream(new CloseShieldInputStream(warIn), entryOut, true));
                    Attribute schema = document.getRootElement().attribute(schemaAttribute);
                    if (schema == null || StringUtils.isBlank(schema.getText())) {
                        log.debug("Inflating XML with unrecognized schema to {}", inflated.getPath());
                        continue;
                    }
                    if (schema.getText().contains(definitionsSchemaNamespace)) {
                        log.debug("Inflating and registering definition from {}", inflated.getPath());
                        document.getRootElement().add(new AbstractAttribute() {

                            private static final long serialVersionUID = -7880537136055718310L;

                            public QName getQName() {
                                return new QName(extensionAttr, document.getRootElement().getNamespace());
                            }

                            public String getValue() {
                                return extension.getName().substring(0, extension.getName().lastIndexOf("."));
                            }
                        });
                        definitionModule.addDefinition(document, true);
                        continue;
                    }
                    if (schema.getText().contains(templateSchemaNamespace)) {
                        log.debug("Inflating and registering template from {}", inflated.getPath());
                        templateService.addTemplate(document, true, zoneModule.getDefaultZone());
                        continue;
                    }
                } catch (DocumentException e) {
                    log.warn("Malformed XML file in extension war at " + extension.getPath(), e);
                    return;
                }
            }
        } catch (IOException e) {
            log.warn("Malformed extension war at " + extension.getPath(), e);
            return;
        } finally {
            try {
                warIn.close();
            } catch (IOException e) {
                log.warn("Unable to close extension war at " + extension.getPath(), e);
                return;
            }
            RequestContextHolder.clear();
        }
        log.info("Extension deployed successfully from {}", extension.getPath());
    }

    @Autowired
    public void setDefinitionService(DefinitionService definitionModule) {
        this.definitionModule = definitionModule;
    }

    @Autowired
    public void setTemplateModule(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Autowired
    public void setZoneModule(ZoneModule zoneModule) {
        this.zoneModule = zoneModule;
    }

    @Required
    public void setExtensionBaseDir(File extensionBaseDir) {
        this.extensionBaseDir = extensionBaseDir;
    }

    @Required
    public void setExtensionWebDir(File extensionWebDir) {
        this.extensionWebDir = extensionWebDir;
    }

    public void setConfigurationFileExtension(String definitionFileExtension) {
        this.configurationFileExtension = definitionFileExtension;
    }

    public void setSchemaInstanceNamespace(Namespace schemaInstanceNamespace) {
        this.schemaInstanceNamespace = schemaInstanceNamespace;
        this.schemaAttribute = new QName(schemaLocationAttribute, this.schemaInstanceNamespace);
    }

    public void setSchemaLocationAttribute(String schemaLocationAttribute) {
        this.schemaLocationAttribute = schemaLocationAttribute;
        this.schemaAttribute = new QName(schemaLocationAttribute, this.schemaInstanceNamespace);
    }

    public void setDefinitionsSchemaNamespace(String definitionsSchemaNamespace) {
        this.definitionsSchemaNamespace = definitionsSchemaNamespace;
    }

    public void setTemplateSchemaNamespace(String templateSchemaNamespace) {
        this.templateSchemaNamespace = templateSchemaNamespace;
    }

    public void setExtensionAttr(String extensionAttr) {
        this.extensionAttr = extensionAttr;
    }

    public void setInfPrefix(String infPrefix) {
        this.infPrefix = infPrefix;
    }
}
