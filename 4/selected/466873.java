package org.sss.eibs.design.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.sss.module.IModuleManager;
import org.sss.module.eibs.compile.CompileException;
import org.sss.module.eibs.compile.CompileFactory;
import org.sss.module.eibs.compile.ICompile;

/**
 * Hibernate编译工具类
 * @author Guangqiang.Yang (latest modification by $Author: hujianxin $)
 * @version $Revision: 707 $ $Date: 2012-04-08 11:25:57 -0400 (Sun, 08 Apr 2012) $
 */
public class CompileHibernate {

    private static final String HIBERNATE_GENERATE_HOME = "hibernate";

    private static File getHomePath(IModuleManager manager) {
        return new File(manager.getBuildPath(), HIBERNATE_GENERATE_HOME);
    }

    public static void compileAllManager(IModuleManager manager, String[] transactionNames) throws CompileException {
        ICompile compile = CompileFactory.getHibernateCompile(new HashMap(), getHomePath(manager));
        for (String transactionName : transactionNames) {
            manager.chain(transactionName);
            compile.compileEibs(manager);
        }
        compile.compileEibsEnd();
    }

    public static void processResources(IModuleManager manager, File warPath, File classesPath, File libPath) throws IOException {
        File homePath = getHomePath(manager);
        FileUtils.copyDirectory(homePath, classesPath, FileFilterUtils.or(FileFilterUtils.suffixFileFilter("class"), FileFilterUtils.directoryFileFilter()));
        FileUtils.copyFileToDirectory(new File(homePath, "hibernate.eibs.xml"), classesPath);
        FileUtils.copyFileToDirectory(new File(homePath, "oscache.properties"), classesPath);
    }
}
