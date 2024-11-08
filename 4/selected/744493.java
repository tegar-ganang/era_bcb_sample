package org.tolven.assembler.javamodule;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.tolven.plugin.TolvenCommandPlugin;

/**
 * This plugin assembles java modules for an ear file
 * 
 * @author Joseph Isaac
 *
 */
public class JavaModuleAssembler extends TolvenCommandPlugin {

    public static final String EXTENSIONPOINT_COMPONENT = "component";

    private Logger logger = Logger.getLogger(JavaModuleAssembler.class);

    @Override
    protected void doStart() throws Exception {
        logger.debug("*** start ***");
    }

    @Override
    public void execute(String[] args) throws Exception {
        logger.debug("*** execute ***");
        ExtensionPoint javaModuleExtensionPoint = getDescriptor().getExtensionPoint(EXTENSIONPOINT_COMPONENT);
        for (Extension extension : javaModuleExtensionPoint.getConnectedExtensions()) {
            File sourceJarFile = getFilePath(extension.getDeclaringPluginDescriptor(), extension.getParameter("jar").valueAsString());
            File myPluginDataDir = getPluginTmpDir(extension.getDeclaringPluginDescriptor());
            File destinationJarFile = new File(myPluginDataDir, sourceJarFile.getName());
            destinationJarFile.getParentFile().mkdirs();
            logger.debug("Copy " + sourceJarFile.getPath() + " to " + destinationJarFile);
            FileUtils.copyFile(sourceJarFile, destinationJarFile);
        }
    }

    @Override
    protected void doStop() throws Exception {
        logger.debug("*** stop ***");
    }
}
