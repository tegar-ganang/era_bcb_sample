package test.egu.plugin.testSource;

import static org.junit.Assert.*;
import java.io.IOException;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import egu.plugin.testSource.DataTestFileSource;
import egu.plugin.testSource.TestFileSource;
import egu.plugin.testSource.interfaces.IFixture;
import egu.plugin.testSource.interfaces.ITestClassStructure;
import egu.plugin.util.interfaces.IFileBuilder;
import egu.plugin.util.interfaces.IVectorInclude;

@RunWith(JMock.class)
public class TestFileSourceTest {

    private Mockery context;

    @Before
    public void setUp() throws Exception {
        context = new JUnit4Mockery();
    }

    @Test
    public final void writeTheClassWhenTheFileIsToCreate() throws IOException, CoreException, InterruptedException, DOMException {
        final DataTestFileSource dataTestFileSource = new TestFileSourceBuilder().with(context.mock(IVectorInclude.class)).with(context.mock(IFileBuilder.class)).with(context.mock(IFixture.class)).with(context.mock(ITestClassStructure.class)).build();
        final IProgressMonitor monitor = context.mock(IProgressMonitor.class);
        TestFileSource toTest = new TestFileSource(dataTestFileSource);
        context.checking(new Expectations() {

            {
                oneOf(dataTestFileSource.fileClassBuilder).isExist();
                will(returnValue(false));
                oneOf(dataTestFileSource.fileClassBuilder).create();
                oneOf(dataTestFileSource.fileClassBuilder).write(dataTestFileSource.mockClassInclude, monitor);
                oneOf(dataTestFileSource.fileClassBuilder).write(dataTestFileSource.classOfTest, monitor);
                oneOf(dataTestFileSource.fileClassBuilder).write(dataTestFileSource.emptyFixture, monitor);
                exactly(3).of(monitor).worked(1);
            }
        });
        toTest.write(monitor);
    }

    @Test
    public final void writeTheClassWhenTheFileAlreadyExist() throws IOException, CoreException, InterruptedException, DOMException {
        final DataTestFileSource dataTestFileSource = new TestFileSourceBuilder().with(context.mock(IVectorInclude.class)).with(context.mock(IFileBuilder.class)).with(context.mock(IFixture.class)).with(context.mock(ITestClassStructure.class)).build();
        final IProgressMonitor monitor = context.mock(IProgressMonitor.class);
        TestFileSource toTest = new TestFileSource(dataTestFileSource);
        context.checking(new Expectations() {

            {
                oneOf(dataTestFileSource.fileClassBuilder).isExist();
                will(returnValue(true));
                oneOf(dataTestFileSource.fileClassBuilder).write(dataTestFileSource.mockClassInclude, monitor);
                oneOf(dataTestFileSource.fileClassBuilder).write(dataTestFileSource.classOfTest, monitor);
                oneOf(dataTestFileSource.fileClassBuilder).write(dataTestFileSource.emptyFixture, monitor);
                exactly(3).of(monitor).worked(1);
            }
        });
        toTest.write(monitor);
    }
}
