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

public class BaseClassGenerationTests {

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
        file.create(new ByteArrayInputStream("package Test{}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile baseFile = folder.getFile(new Path("TestBase.java"));
        assertTrue(baseFile.exists());
        assertEquals(TestUtil.getAstString("public class TestBase{}"), TestUtil.getAstString(baseFile));
    }

    @Test
    public void testDefaultPackage() throws CoreException, IOException {
        IFolder srcFolder = project.getFolder(new Path("src"));
        IFolder folder = srcFolder.getFolder(new Path("com"));
        folder.create(true, true, null);
        folder = folder.getFolder(new Path("farukcankaya"));
        folder.create(true, true, null);
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile baseFile = srcFolder.getFile(new Path("TestBase.java"));
        assertTrue(baseFile.exists());
        assertEquals(TestUtil.getAstString("public class TestBase{}"), TestUtil.getAstString(baseFile));
    }

    @Test
    public void testPackage() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test {options{cpackage com.farukcankaya.test;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile baseFile = folder.getFile(new Path("com/farukcankaya/test/TestBase.java"));
        assertTrue(baseFile.exists());
        assertEquals(TestUtil.getAstString("package com.farukcankaya.test;public class TestBase{}"), TestUtil.getAstString(baseFile));
    }

    @Test
    public void testChangeSupport() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        IFile baseFile = folder.getFile(new Path("TestBase.java"));
        file.create(new ByteArrayInputStream(new byte[0]), true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{ changesupport both;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(baseFile.exists());
        StringBuilder expected = new StringBuilder("import java.beans.PropertyChangeSupport;").append("import java.beans.").append("PropertyChangeListener;").append("import java.beans.").append("VetoableChangeSupport;").append("import java.beans.").append("VetoableChangeListener;").append("import java.beans.").append("PropertyVetoException;").append("public class TestBase{").append("private final PropertyChangeSupport ").append("propertyChangeSupport").append(" = new PropertyChangeSupport(this);").append("private final VetoableChangeSupport ").append("vetoableChangeSupport").append(" = new VetoableChangeSupport(this);").append("public final void addPropertyChangeListener").append("(PropertyChangeListener listener){").append("propertyChangeSupport.addPropertyChangeListener(listener);}").append("public final void removePropertyChangeListener").append("(PropertyChangeListener listener){").append("propertyChangeSupport.removePropertyChangeListener(listener);}").append("protected final void firePropertyChange").append("(String propertyName, Object oldValue, Object newValue)").append("{propertyChangeSupport.firePropertyChange").append("(propertyName,oldValue,newValue);}").append("public final void addVetoableChangeListener").append("(VetoableChangeListener listener){").append("vetoableChangeSupport.addVetoableChangeListener(listener);}").append("public final void removeVetoableChangeListener").append("(VetoableChangeListener listener){").append("vetoableChangeSupport.removeVetoableChangeListener(listener);}").append("protected final void fireVetoableChange").append("(String propertyName, Object oldValue, Object newValue)").append("throws PropertyVetoException").append("{vetoableChangeSupport.fireVetoableChange").append("(propertyName,oldValue,newValue);}");
        assertEquals(TestUtil.getAstString(expected.toString()), TestUtil.getAstString(baseFile));
        baseFile.delete(true, null);
    }

    @Test
    public void testLock() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        IFile baseFile = folder.getFile(new Path("TestBase.java"));
        file.create(new ByteArrayInputStream(new byte[0]), true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{ lock readwrite;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(baseFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("import java.util.concurrent.locks.ReadWriteLock;").append("import java.util.concurrent.locks.").append("ReentrantReadWriteLock;").append("public class TestBase{").append("public final ReadWriteLock LOCK = ").append("new ReentrantReadWriteLock();}").toString()), TestUtil.getAstString(baseFile));
        baseFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{ lock reentrant;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(baseFile.exists());
        assertEquals(TestUtil.getAstString(new StringBuilder("import java.util.concurrent.locks.Lock;").append("import java.util.concurrent.locks.ReentrantLock;").append("public class TestBase{").append("public final Lock LOCK = new ReentrantLock();}").toString()), TestUtil.getAstString(baseFile));
        baseFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{ lock synchronized;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(baseFile.exists());
        assertEquals(TestUtil.getAstString("public class TestBase{}"), TestUtil.getAstString(baseFile));
        baseFile.delete(true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{ lock none;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(baseFile.exists());
        assertEquals(TestUtil.getAstString("public class TestBase{}"), TestUtil.getAstString(baseFile));
    }

    @Test
    public void testInterface() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        file.create(new ByteArrayInputStream("package Test{options{model interface;}}".getBytes()), true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        IFile baseInterfaceFile = folder.getFile(new Path("TestBase.java"));
        IFile baseClassFile = folder.getFile(new Path("TestBaseImpl.java"));
        assertTrue(baseInterfaceFile.exists());
        assertTrue(baseClassFile.exists());
        assertEquals(TestUtil.getAstString("public interface TestBase{}"), TestUtil.getAstString(baseInterfaceFile));
        assertEquals(TestUtil.getAstString("public class TestBaseImpl implements TestBase{}"), TestUtil.getAstString(baseClassFile));
    }

    @Test
    public void testInterfacePropertyChangeSupport() throws CoreException, IOException {
        IFolder folder = project.getFolder(new Path("src"));
        IFile file = folder.getFile("test.simplemodel");
        IFile baseInterfaceFile = folder.getFile(new Path("TestBase.java"));
        IFile baseClassFile = folder.getFile(new Path("TestBaseImpl.java"));
        file.create(new ByteArrayInputStream(new byte[0]), true, null);
        file.setContents(new ByteArrayInputStream("package Test{options{model interface; changesupport both;}}".getBytes()), true, true, null);
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
        assertTrue(baseClassFile.exists());
        assertTrue(baseInterfaceFile.exists());
        StringBuilder interfaceExpected = new StringBuilder("import java.beans.PropertyChangeListener;").append("import java.beans.VetoableChangeListener;").append("public interface TestBase{").append("void addPropertyChangeListener").append("(PropertyChangeListener listener);").append("void removePropertyChangeListener").append("(PropertyChangeListener listener);").append("void addVetoableChangeListener").append("(VetoableChangeListener listener);").append("void removeVetoableChangeListener").append("(VetoableChangeListener listener);").append("}");
        StringBuilder classExpected = new StringBuilder("import java.beans.PropertyChangeSupport;").append("import java.beans.").append("PropertyChangeListener;").append("import java.beans.").append("VetoableChangeSupport;").append("import java.beans.").append("VetoableChangeListener;").append("import java.beans.").append("PropertyVetoException;").append("public class TestBaseImpl implements TestBase {").append("private final PropertyChangeSupport ").append("propertyChangeSupport").append(" = new PropertyChangeSupport(this);").append("private final VetoableChangeSupport ").append("vetoableChangeSupport").append(" = new VetoableChangeSupport(this);").append("@Override public final void addPropertyChangeListener").append("(PropertyChangeListener listener){").append("propertyChangeSupport.addPropertyChangeListener(listener);}").append("@Override public final void removePropertyChangeListener").append("(PropertyChangeListener listener){").append("propertyChangeSupport.removePropertyChangeListener(listener);}").append("protected final void firePropertyChange").append("(String propertyName, Object oldValue, Object newValue)").append("{propertyChangeSupport.firePropertyChange").append("(propertyName,oldValue,newValue);}").append("@Override public final void addVetoableChangeListener").append("(VetoableChangeListener listener){").append("vetoableChangeSupport.addVetoableChangeListener(listener);}").append("@Override public final void removeVetoableChangeListener").append("(VetoableChangeListener listener){").append("vetoableChangeSupport.removeVetoableChangeListener(listener);}").append("protected final void fireVetoableChange").append("(String propertyName, Object oldValue, Object newValue)").append("throws PropertyVetoException").append("{vetoableChangeSupport.fireVetoableChange").append("(propertyName,oldValue,newValue);}");
        assertEquals(TestUtil.getAstString(interfaceExpected.toString()), TestUtil.getAstString(baseInterfaceFile));
        assertEquals(TestUtil.getAstString(classExpected.toString()), TestUtil.getAstString(baseClassFile));
    }
}
