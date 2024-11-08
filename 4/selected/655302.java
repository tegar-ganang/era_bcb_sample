package com.enerjy.analyzer.java.rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.enerjy.analyzer.java.IJavaRule;
import com.enerjy.common.EnerjyException;
import com.enerjy.common.util.ClassUtils;
import com.enerjy.common.util.FileUtils;
import com.enerjy.common.util.FolderIterator;
import com.enerjy.common.util.JUnitUtils;
import com.enerjy.common.util.StreamUtils;

/**
 * Test suite for running the Analyzer P&F tests in JUnit. If -Dinteractive is specified, the suite will prompt for a rule filter
 * and only run P&F tests that contain the filter string.
 */
public class JavaRulesSuite {

    /**
     * @return A test suite with all (optionally filtered) Analyzer test cases to run.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        String ruleFilter = "";
        if (null != System.getProperty("interactive")) {
            String filter = "";
            File lastRun = new File("ruleFilter.cfg");
            if (lastRun.exists()) {
                try {
                    filter = new String(FileUtils.readContents(lastRun, null));
                    filter = filter.trim();
                } catch (IOException e) {
                }
            }
            System.out.print("Rule filter [" + filter + "]: ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                ruleFilter = reader.readLine();
            } catch (IOException e) {
            }
            if ((null == ruleFilter) || (0 == ruleFilter.trim().length())) {
                ruleFilter = filter;
            }
            try {
                FileUtils.writeContents(lastRun, ruleFilter);
            } catch (IOException e) {
            }
        }
        File srcRoot = JUnitUtils.findSourceRoot("Analyzer/core");
        T0002 t0002 = new T0002();
        if ((0 == ruleFilter.length()) || (-1 != ruleFilter.indexOf("T0002"))) {
            if (shouldCheckGui(t0002)) {
                suite.addTest(new CheckRuleName(t0002));
                suite.addTest(new CheckRuleGui(t0002));
            }
            suite.addTest(new JavaRuleWrapper(t0002, new File(srcRoot, "PTestT0002_01.java")));
            suite.addTest(new JavaRuleWrapper(t0002, new File(srcRoot, "biz/PTestT0002_02.java")));
            suite.addTest(new JavaRuleWrapper(t0002, new File(srcRoot, "con/FTestT0002_01.java")));
            suite.addTest(new JavaRuleWrapper(t0002, new File(srcRoot, "sv/deeper/PTestT0002_03.java")));
        }
        Set<String> rules = new HashSet<String>();
        File testRoot = new File(srcRoot, "com/enerjy/analyzer/java/rules/testfiles");
        FolderIterator folders = new FolderIterator(testRoot.getAbsolutePath());
        for (File file : folders) {
            String name = file.getName();
            if ((!name.endsWith(".java") && !name.endsWith(".java_")) || (!name.startsWith("FTest") && !name.startsWith("PTest"))) {
                continue;
            }
            if (-1 == file.getAbsolutePath().indexOf(ruleFilter)) {
                continue;
            }
            String rule = file.getParentFile().getName();
            String ruleClass = "com.enerjy.analyzer.java.rules." + rule;
            File propsFile = new File(file.getParent(), "default.properties");
            if (propsFile.exists()) {
                Properties props = loadProperties(propsFile);
                ruleClass = props.getProperty("ruleClass", ruleClass);
            }
            IJavaRule ruleInstance = ClassUtils.newInstance(IJavaRule.class, ruleClass);
            if (null != ruleInstance) {
                if (!rules.contains(ruleClass)) {
                    suite.addTest(new CheckRuleName(ruleInstance));
                    if (shouldCheckGui(ruleInstance)) {
                        suite.addTest(new CheckRuleGui(ruleInstance));
                    }
                    rules.add(ruleClass);
                }
                suite.addTest(new JavaRuleWrapper(ruleInstance, file));
            }
        }
        suite.setName("Analyzer Rule Suite");
        return suite;
    }

    private static Properties loadProperties(File file) {
        try {
            return StreamUtils.loadProperties(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new EnerjyException("Error loading properties from: " + file);
        }
    }

    private static boolean shouldCheckGui(IJavaRule rule) {
        for (Method check : rule.getClass().getMethods()) {
            String method = check.getName();
            if (method.equals("readProperties") || method.equals("writeProperties") || method.equals("createPreferencePanel")) {
                if (!check.getDeclaringClass().getSimpleName().equals("RuleBase")) {
                    return true;
                }
            }
        }
        return false;
    }

    private JavaRulesSuite() {
    }
}
