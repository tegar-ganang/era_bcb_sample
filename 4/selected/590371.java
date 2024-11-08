package com.farukcankaya.simplemodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClassGenerationTests {

    private IProject project;

    @Before
    public void setUp() throws CoreException {
        project = TestUtil.createSimplemodelEnabledJavaProject();
    }

    @After
    public void tearDown() throws CoreException {
        TestUtil.deleteProject(project);
    }

    @Test
    public void testBasic() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{class Test1{}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile classFile = folder.getFile(new Path("Test1.java"));
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString("public class Test1 extends TestBase{public Test1(){}}"), TestUtil.getAstString(classFile));
    }

    @Test
    public void testAbstract() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{abstract Test1{}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile classFile = folder.getFile(new Path("Test1.java"));
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString("public abstract class Test1 extends TestBase{public Test1(){}}"), TestUtil.getAstString(classFile));
    }

    @Test
    public void testInterface() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{interface Test1{property SomeProperty : String;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile classFile = folder.getFile(new Path("Test1.java"));
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString("public interface Test1{void setSomeProperty(String someProperty);String getSomeProperty();}"), TestUtil.getAstString(classFile));
    }

    @Test
    public void testDefaultPackage() throws CoreException, IOException {
        IFolder srcFolder = project.getFolder(new Path("src"));
        IFolder folder = srcFolder.getFolder(new Path("com"));
        folder.create(true, true, null);
        folder = folder.getFolder(new Path("farukcankaya"));
        folder.create(true, true, null);
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{class Test1{}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile classFile = srcFolder.getFile(new Path("Test1.java"));
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString("public class Test1 extends TestBase{public Test1(){}}"), TestUtil.getAstString(classFile));
    }

    @Test
    public void testPackage() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test {options{cpackage com.farukcankaya.test;} class Test1{}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile classFile = folder.getFile(new Path("com/farukcankaya/test/Test1.java"));
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString("package com.farukcankaya.test;public class Test1 extends TestBase{public Test1(){}}"), TestUtil.getAstString(classFile));
    }

    @Test
    public void testProperty() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{class Test1{property SomeProperty : String;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile classFile = folder.getFile(new Path("Test1.java"));
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty)").append("{this.someProperty=someProperty;}").append("public String getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
    }

    @Test
    public void testImport() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        IFile classFile = folder.getFile(new Path("Test1.java"));
        file.create(new ByteArrayInputStream("package Test{class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty)").append("{this.someProperty=someProperty;}").append("public String getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{class Test1{property SomeProperty : java.util.Date;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("import java.util.Date;").append("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private Date someProperty;").append("public void setSomeProperty(Date someProperty)").append("{this.someProperty=someProperty;}").append("public Date getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{class Test1{property SomeProperty : java.util.Date;property SomeProperty2 : java.sql.Date;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("import java.util.Date;").append("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private Date someProperty;").append("public void setSomeProperty(Date someProperty)").append("{this.someProperty=someProperty;}").append("public Date getSomeProperty(){").append("return someProperty;}").append("private java.sql.Date someProperty2;").append("public void setSomeProperty2(").append("java.sql.Date someProperty2)").append("{this.someProperty2=someProperty2;}").append("public java.sql.Date getSomeProperty2(){").append("return someProperty2;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
    }

    @Test
    public void testLock() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        IFile classFile = folder.getFile(new Path("Test1.java"));
        file.create(new ByteArrayInputStream("package Test{options{lock none;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty)").append("{this.someProperty=someProperty;}").append("public String getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{lock synchronized;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public synchronized void setSomeProperty(String someProperty)").append("{this.someProperty=someProperty;}").append("public synchronized String getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{lock reentrant;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty)").append("{LOCK.lock();try{").append("this.someProperty=someProperty;").append("}finally{LOCK.unlock();}}").append("public String getSomeProperty(){").append("LOCK.lock();try{").append("return someProperty;}finally{").append("LOCK.unlock();}}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{lock readwrite;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty)").append("{LOCK.writeLock().lock();try{").append("this.someProperty=someProperty;").append("}finally{LOCK.writeLock().unlock();}}").append("public String getSomeProperty(){").append("LOCK.readLock().lock();try{").append("return someProperty;}finally{").append("LOCK.readLock().unlock();}}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
    }

    @Test
    public void testChangeSupportWithLock() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        IFile classFile = folder.getFile(new Path("Test1.java"));
        file.create(new ByteArrayInputStream("package Test{options{changesupport both;lock readwrite;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("import java.beans.PropertyVetoException;").append("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty){").append("LOCK.writeLock().lock();try{").append("try{fireVetoableChange(\"someProperty\",").append("this.someProperty,someProperty);").append("}catch(PropertyVetoException ex){").append("throw new RuntimeException(ex);}").append("String old_someProperty = this.someProperty;").append("this.someProperty=someProperty;").append("firePropertyChange(\"someProperty\",").append("old_someProperty,someProperty);}").append("finally{LOCK.writeLock().unlock();}}").append("public String getSomeProperty(){").append("LOCK.readLock().lock();try{").append("return someProperty;}").append("finally{LOCK.readLock().unlock();}}").toString()), TestUtil.getAstString(classFile));
    }

    @Test
    public void testChangeSupport() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        IFile classFile = folder.getFile(new Path("Test1.java"));
        file.create(new ByteArrayInputStream("package Test{options{changesupport none;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty)").append("{this.someProperty=someProperty;}").append("public String getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{changesupport property;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty)").append("{String old_someProperty = this.someProperty;").append("this.someProperty=someProperty;").append("firePropertyChange(\"someProperty\",").append("old_someProperty,someProperty);}").append("public String getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{changesupport vetoable;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("import java.beans.PropertyVetoException;").append("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty){").append("try{fireVetoableChange(\"someProperty\",").append("this.someProperty,someProperty);").append("}catch(PropertyVetoException ex){").append("throw new RuntimeException(ex);}").append("this.someProperty=someProperty;}").append("public String getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{changesupport both;} class Test1{property SomeProperty : java.lang.String;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("import java.beans.PropertyVetoException;").append("public class Test1 ").append("extends TestBase{").append("public Test1(){}").append("private String someProperty;").append("public void setSomeProperty(String someProperty){").append("try{fireVetoableChange(\"someProperty\",").append("this.someProperty,someProperty);").append("}catch(PropertyVetoException ex){").append("throw new RuntimeException(ex);}").append("String old_someProperty = this.someProperty;").append("this.someProperty=someProperty;").append("firePropertyChange(\"someProperty\",").append("old_someProperty,someProperty);}").append("public String getSomeProperty(){").append("return someProperty;}}").toString()), TestUtil.getAstString(classFile));
        classFile.delete(true, null);
    }

    @Test
    public void testInterfaceModelType() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{ options{model interface;} class Test1{property Something : String;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile interfaceFile = folder.getFile(new Path("Test1.java"));
        IFile classFile = folder.getFile(new Path("Test1Impl.java"));
        assertTrue(interfaceFile.exists());
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public interface Test1 extends TestBase{").append("void setSomething(String something);").append("String getSomething();}").toString()), TestUtil.getAstString(interfaceFile));
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1Impl extends ").append("TestBaseImpl implements Test1{").append("public Test1Impl(){}").append("private String something;").append("@Override public void setSomething").append("(String something){this.something=something;}").append("@Override public String getSomething()").append("{return something;}}").toString()), TestUtil.getAstString(classFile));
    }

    @Test
    public void testValidatorTest() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{class Test1{property Something : String{validator{validate value != null : \"Something can not be null!\";}}}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile classFile = folder.getFile(new Path("Test1.java"));
        assertTrue(classFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("public class Test1 extends ").append("TestBase{").append("public Test1(){").append("if (something==null)").append("throw new IllegalArgumentException(").append("\"Something can not be null!\");}").append("private String something;").append("public void setSomething").append("(String something){").append("if (something==null)").append("throw new IllegalArgumentException(").append("\"Something can not be null!\");").append("this.something=something;}").append("public String getSomething()").append("{return something;}}").toString()), TestUtil.getAstString(classFile));
    }
}
