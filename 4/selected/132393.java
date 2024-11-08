package br.gov.demoiselle.eclipse.main.core.editapp;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import br.gov.demoiselle.eclipse.main.IPluginFacade;
import br.gov.demoiselle.eclipse.util.editapp.JUTestHelper;
import br.gov.demoiselle.eclipse.util.utility.CommonsUtil;
import br.gov.demoiselle.eclipse.util.utility.EclipseUtil;
import br.gov.demoiselle.eclipse.util.utility.FileUtil;
import br.gov.demoiselle.eclipse.util.utility.FrameworkUtil;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassRepresentation;
import br.gov.demoiselle.eclipse.util.utility.classwriter.FieldHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.MethodHelper;
import br.gov.demoiselle.eclipse.util.utility.plugin.Configurator;
import br.gov.demoiselle.eclipse.util.utility.xml.reader.XMLReader;

/**
 * Implements read/write JUnit Test classes information
 * Reads JUnit Test from a list in the wizard xml file, locates in selected project
 * Writes Tests configuration and java file
 * see IPluginFacade 
 * @author CETEC/CTCTA
 */
public class JUTestFacade implements IPluginFacade<JUTestHelper> {

    private String xml = null;

    private boolean insert = false;

    /**
	 * 
	 * @return XML file with Demoiselle configuration for Editing Project 
	 */
    public String getXml() {
        return xml;
    }

    /**
	 * Sets Demoiselle configuration for Editing Project
	 * @param xml
	 */
    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
	 * Reads configurations for Tests has already created for project
	 */
    public List<JUTestHelper> read() {
        Configurator reader = new Configurator();
        List<JUTestHelper> testList = reader.readTests(xml);
        if (testList != null && testList.size() > 0) {
            for (JUTestHelper test : testList) {
                test.setReadOnly(true);
            }
        }
        return testList;
    }

    /**
	 * 
	 * @return if has tag for NODE_TESTS elements in xml file
	 * @throws Exception 
	 */
    public boolean hasTest() throws Exception {
        if (XMLReader.hasElement(xml, NODE_TESTS)) {
            return true;
        }
        return false;
    }

    /**
	 * Write all JUnit Tests of the parameter List that is not marked with readonly attribute
	 * 
	 * @param List<JUTestHelper> Test list to be written
	 * @throws Exception 
	 */
    public void write(List<JUTestHelper> tests) throws Exception {
        if (tests != null) {
            try {
                this.setInsert(!this.hasTest());
                for (JUTestHelper test : tests) {
                    if (!test.isReadOnly()) {
                        ClassHelper clazzImpl = generateImplementation(test);
                        FileUtil.writeClassFile(test.getAbsolutePath(), clazzImpl, true, true, true);
                    }
                }
                Configurator reader = new Configurator();
                if (!reader.writeTests(tests, xml, insert)) {
                    throw new Exception("erro ao gravar em arquivo de configuração do Demoiselle!");
                }
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    /**
	 * Generate a Class ClassHelper object with informations of the JUnit Test passed by parameter
	 * This object will be used to create the .java file of the Test Implementation Class
	 * 
	 * @param test - JUTestHelper
	 * @return ClassHelper 
	 * @throws Exception 
	 * @throws MalformedURLException 
	 */
    private static ClassHelper generateImplementation(JUTestHelper test) throws MalformedURLException, Exception {
        ClassHelper clazzImpl = new ClassHelper();
        clazzImpl.setName(test.getName());
        clazzImpl.setPackageName(test.getPackageName());
        ArrayList<ClassRepresentation> imports = new ArrayList<ClassRepresentation>();
        imports.add(new ClassRepresentation(Test.class.getName()));
        imports.add(new ClassRepresentation(Before.class.getName()));
        imports.add(new ClassRepresentation(After.class.getName()));
        imports.add(new ClassRepresentation("static org.junit.Assert.*"));
        imports.add(new ClassRepresentation("br.gov.framework.demoiselle.web.transaction.WebTransactionContext"));
        imports.add(FrameworkUtil.getInjection());
        clazzImpl.setImports(imports);
        clazzImpl.setInterfaces(new ArrayList<ClassRepresentation>());
        clazzImpl.getInterfaces().add(FrameworkUtil.getIFacade());
        List<FieldHelper> fields = new ArrayList<FieldHelper>();
        if (test.getTargetClass() != null) {
            FieldHelper iTargetClass = new FieldHelper();
            iTargetClass.setAnnotation(FrameworkUtil.getInjectionAnnotation());
            iTargetClass.setType(test.getTargetClass());
            iTargetClass.setHasGetMethod(false);
            iTargetClass.setHasSetMethod(false);
            iTargetClass.setName(FrameworkUtil.getIVarName(test.getTargetClass()));
            fields.add(iTargetClass);
        }
        clazzImpl.setFields(fields);
        List<MethodHelper> methods = new ArrayList<MethodHelper>();
        MethodHelper newMethod = new MethodHelper();
        newMethod.setAnnotation("@Before");
        newMethod.setModifier(1);
        newMethod.setName("setUp");
        newMethod.setBody("/* //TODO insert necessary code here! e.g.*/" + "WebTransactionContext.getInstance().init();");
        methods.add(newMethod);
        newMethod = new MethodHelper();
        newMethod.setAnnotation("@After");
        newMethod.setModifier(1);
        newMethod.setName("tearDown");
        newMethod.setBody("/* //TODO insert necessary code here! e.g. */" + "WebTransactionContext.getInstance().end();");
        methods.add(newMethod);
        if (test.getTargetClass() != null) {
            Method[] methodList = null;
            if (test.getOriginalInterfaceName() != null) {
                methodList = EclipseUtil.getClass(test.getTargetClass().getFullName()).getMethods();
            } else {
                methodList = EclipseUtil.getDeclaredMethodsFromClass(test.getTargetClass().getFullName());
            }
            try {
                for (Method method : methodList) {
                    newMethod = new MethodHelper();
                    newMethod.setAnnotation("@Test");
                    newMethod.setModifier(1);
                    if (method.getReturnType().getName() != "void") {
                        String methodMidleName = CommonsUtil.toUpperCaseFirstLetter(method.getName());
                        String methodLastName = CommonsUtil.toUpperCaseFirstLetter(method.getReturnType().getSimpleName());
                        newMethod.setName("test" + methodMidleName + methodLastName);
                    } else {
                        String methodLastName = CommonsUtil.toUpperCaseFirstLetter(method.getName());
                        newMethod.setName("test" + methodLastName);
                    }
                    newMethod.setBody("/* //TODO insert your code here, this test is set to fails*/ assertTrue(false);");
                    methods.add(newMethod);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        clazzImpl.setMethods(methods);
        return clazzImpl;
    }

    public boolean isInsert() {
        return insert;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }

    public void searchTests() throws CoreException {
        String varPackageName = null;
        List<String> results = null;
        List<JUTestHelper> JUTestHelperList = null;
        varPackageName = EclipseUtil.getPackageFullName("persistence");
        results = FileUtil.findJavaTestFiles(varPackageName);
        if (results != null) {
            for (String test : results) {
                JUTestHelper testHelper = new JUTestHelper();
                test = test.replace("/", ".");
                test = test.substring(0, test.lastIndexOf(".java"));
                testHelper.setTargetClass(new ClassRepresentation(test));
                varPackageName = varPackageName + "dao";
                testHelper.setPackageName(varPackageName);
                testHelper.setAbsolutePath(EclipseUtil.getTestSourceLocation() + "/" + varPackageName.replace(".", "/") + "/");
                testHelper.setReadOnly(false);
                if (JUTestHelperList == null) {
                    JUTestHelperList = new ArrayList<JUTestHelper>();
                }
                JUTestHelperList.add(testHelper);
            }
        }
        varPackageName = EclipseUtil.getPackageFullName("business");
        results = FileUtil.findJavaTestFiles(varPackageName);
        if (results != null) {
            for (String test : results) {
                JUTestHelper testHelper = new JUTestHelper();
                test = test.replace("/", ".");
                test = test.substring(0, test.lastIndexOf(".java"));
                testHelper.setTargetClass(new ClassRepresentation(test));
                testHelper.setPackageName(varPackageName);
                testHelper.setAbsolutePath(EclipseUtil.getTestSourceLocation() + "/" + varPackageName.replace(".", "/") + "/");
                testHelper.setReadOnly(false);
                if (JUTestHelperList == null) {
                    JUTestHelperList = new ArrayList<JUTestHelper>();
                }
                JUTestHelperList.add(testHelper);
            }
        }
        varPackageName = EclipseUtil.getPackageFullName("managedbean");
        results = FileUtil.findJavaTestFiles(varPackageName);
        if (results != null) {
            for (String test : results) {
                JUTestHelper testHelper = new JUTestHelper();
                test = test.replace("/", ".");
                test = test.substring(0, test.lastIndexOf(".java"));
                testHelper.setTargetClass(new ClassRepresentation(test));
                testHelper.setPackageName(varPackageName);
                testHelper.setAbsolutePath(EclipseUtil.getTestSourceLocation() + "/" + varPackageName.replace(".", "/") + "/");
                testHelper.setReadOnly(false);
                if (JUTestHelperList == null) {
                    JUTestHelperList = new ArrayList<JUTestHelper>();
                }
                JUTestHelperList.add(testHelper);
            }
        }
        if (JUTestHelperList != null && JUTestHelperList.size() > 0) {
            Configurator reader = new Configurator();
            reader.writeTests(JUTestHelperList, getXml(), false);
        }
    }
}
