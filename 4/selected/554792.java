package org.sss.eibs.design.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.sss.module.IModuleManager;
import org.sss.module.eibs.compile.CompileException;
import org.sss.module.eibs.compile.CompileFactory;
import org.sss.module.eibs.compile.ICompile;

/**
 * ZUL编译工具类
 * @author Jason.Hoo (latest modification by $Author: hujianxin78728 $)
 * @version $Revision: 606 $ $Date: 2009-11-17 06:32:21 -0500 (Tue, 17 Nov 2009) $
 */
public class CompileZul {

    private static final String ZUL_PAGE_HOME = "zul";

    private static File getHomePath(IModuleManager manager) {
        return new File(manager.getBuildPath(), ZUL_PAGE_HOME);
    }

    public static void compileAllManager(IModuleManager manager, String[] transactionNames) throws CompileException {
        ICompile compile = CompileFactory.getZulCompile(new HashMap(), getHomePath(manager));
        for (String transactionName : transactionNames) {
            manager.chain(transactionName);
            compile.compileEibs(manager);
        }
        compile.compileEibsEnd();
    }

    public static void processResources(IModuleManager manager, File warPath, File classesPath, File libPath) throws IOException {
        File resourcePath = new File("resource", ZUL_PAGE_HOME);
        if (resourcePath.exists()) FileUtils.copyDirectory(resourcePath, warPath, FileFilterUtils.makeSVNAware(null));
        FileUtils.copyDirectory(manager.getSourcePath(), classesPath, FileFilterUtils.suffixFileFilter("properties"));
        for (File file : (Collection<File>) FileUtils.listFiles(manager.getSourcePath(), FileFilterUtils.suffixFileFilter("_en_US.properties"), null)) {
            String name = file.getName().replaceAll("_en_US", "");
            FileUtils.copyFile(file, new File(classesPath, name));
        }
        FileUtils.copyDirectory(manager.getSourcePath(), classesPath, FileFilterUtils.suffixFileFilter("ctl"));
        for (File file : (Collection<File>) FileUtils.listFiles(manager.getSourcePath(), FileFilterUtils.suffixFileFilter("_en_US.ctl"), null)) {
            String name = file.getName().replaceAll("_en_US", "");
            FileUtils.copyFile(file, new File(classesPath, name));
        }
        File homePath = getHomePath(manager);
        FileUtils.copyFileToDirectory(new File(homePath, "page.properties"), classesPath);
        FileUtils.copyDirectory(getHomePath(manager), warPath, FileFilterUtils.suffixFileFilter("zul"));
    }
}
