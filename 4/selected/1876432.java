package org.openconcerto.erp.modules;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.utils.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * Package a module from a project and launch it. The system property {@link #MODULE_DIR_PROP} must
 * be defined.
 * 
 * @author Sylvain CUAZ
 * @see ModulePackager
 */
public class ModuleLauncher {

    /**
     * Required system property, it must point to a directory with module classes in bin/, this
     * class will put the packaged module in the dist/ subdirectory.
     */
    public static final String MODULE_DIR_PROP = "module.dir";

    /**
     * System property to use if the module properties files isn't "module.properties" (this
     * property is evaluated relative to {@link #MODULE_DIR_PROP}).
     */
    public static final String MODULE_PROPS_FILE_PROP = "module.propsFile";

    public static void main(String[] args) throws IOException {
        final File moduleDir = new File(System.getProperty(MODULE_DIR_PROP));
        final File propsFile = new File(moduleDir, System.getProperty(MODULE_PROPS_FILE_PROP, "module.properties"));
        final boolean launchFromPackage = !Boolean.getBoolean("module.fromProject");
        final File classes = new File(moduleDir, "bin");
        final File distDir = new File(moduleDir, "dist");
        FileUtils.mkdir_p(distDir);
        final File jar = new ModulePackager(propsFile, classes).writeToDir(distDir);
        FileUtils.mkdir_p(Gestion.MODULES_DIR);
        FileUtils.copyFile(jar, new File(Gestion.MODULES_DIR, jar.getName()));
        final ModuleFactory factory;
        if (launchFromPackage) {
            factory = new JarModuleFactory(jar);
        } else {
            factory = new RuntimeModuleFactory(propsFile);
            try {
                Class.forName(factory.getMainClass());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Module classes are not in the classpath (they should be in " + classes + ")", e);
            }
        }
        Gestion.main(args);
        ModuleManager.getInstance().addFactoryAndStart(factory, false);
    }
}
