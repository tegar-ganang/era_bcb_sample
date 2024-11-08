package net.sourceforge.coberclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import net.sourceforge.coberclipse.actions.CoberturaNatureActionDelegate;
import net.sourceforge.coberclipse.builders.ClassInstrumentor;
import net.sourceforge.coberclipse.builders.CoverageCollector;
import net.sourceforge.coberclipse.model.CoverageManager;
import net.sourceforge.coberclipse.model.ICoverageItem;
import net.sourceforge.coberclipse.util.FileUtils;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.ProjectData;
import net.sourceforge.cobertura.reporting.ComplexityCalculator;
import net.sourceforge.cobertura.reporting.html.HTMLReport;
import net.sourceforge.cobertura.util.FileFinder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.junit.model.ITestSuiteElement;
import org.eclipse.ui.PlatformUI;

/**
 * A test run listener that generates the coverage report 
 * once the unit test is ended.
 * 
 * @author Guang Zhou
 * 
 */
public class CoverageTestListener extends TestRunListener {

    public void sessionFinished(ITestRunSession session) {
        if (session.getChildren() == null || session.getChildren().length == 0) {
            return;
        }
        ITestSuiteElement tse = (ITestSuiteElement) session.getChildren()[0];
        String testSuiteClass = getTestSuiteClassName(tse);
        if (testSuiteClass == null) {
            return;
        }
        testSuiteClass = testSuiteClass.replace(".", File.separator) + ".java";
        IProject project = findProject(testSuiteClass);
        if (project != null) {
            report(project);
        }
    }

    /**
     * Return the the test suite class which is running.
     * 
     * @param elem
     * @return
     */
    private String getTestSuiteClassName(ITestElement elem) {
        String className = null;
        if (elem == null) {
            return className;
        }
        if (elem instanceof ITestSuiteElement) {
            className = getTestSuiteClassName(((ITestSuiteElement) elem).getChildren()[0]);
        } else if (elem instanceof ITestCaseElement) {
            className = ((ITestSuiteElement) elem.getParentContainer()).getSuiteTypeName();
        }
        return className;
    }

    /**
     * Find the project which the executing test case belongs to.
     * 
     * @param testClass
     * @return
     */
    private IProject findProject(String testClass) {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            IJavaProject javaProject = JavaCore.create(project);
            IClasspathEntry[] entries = new IClasspathEntry[0];
            try {
                entries = javaProject.getRawClasspath();
            } catch (JavaModelException e) {
                CoberclipseLog.error(e);
            }
            Collection<IPath> srcPaths = new ArrayList<IPath>();
            for (IClasspathEntry entry : entries) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    srcPaths.add(entry.getPath().removeFirstSegments(1));
                    IResource srcDir = ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath() + File.separator + testClass);
                    if (srcDir != null) {
                        try {
                            if (project.isOpen() && project.hasNature(CoberturaNatureActionDelegate.NATURE_ID)) {
                                return project;
                            }
                        } catch (CoreException e) {
                            CoberclipseLog.error(e);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Generate test coverage report for the project.
     * 
     * @param p
     */
    private void report(IProject p) {
        File destinationDir = getReportingDirectory(p);
        FileFinder finder = new FileFinder();
        File dataFile = new File(p.getLocation().toString() + File.separator + ClassInstrumentor.DATA_FILE);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            CoberclipseLog.error(e);
        }
        ProjectData projectData = CoverageDataFileHandler.loadCoverageData(dataFile);
        Collection<IPath> srcPaths = getSourcePaths(p, finder);
        ComplexityCalculator complexity = new ComplexityCalculator(finder);
        CoverageCollector cc = new CoverageCollector(projectData, complexity, p, srcPaths);
        final Collection<ICoverageItem> items = cc.getResult();
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            public void run() {
                CoverageManager.getInstance().updateCoverageItems(items);
            }
        });
        try {
            new HTMLReport(projectData, destinationDir, finder, complexity);
            if (!p.getFolder(ClassInstrumentor.REPORTING_PATH).exists()) {
                p.refreshLocal(IProject.DEPTH_ONE, null);
            }
            FileUtils.copyFile(new File(p.getLocation().toString() + File.separator + ClassInstrumentor.ORIGINAL_DATA_FILE), dataFile);
        } catch (IOException e) {
            CoberclipseLog.error(e);
        } catch (Exception e) {
            CoberclipseLog.error(e);
        }
    }

    /**
     * Return the report directory. Create it if it doesn't exist.
     * 
     * @param p
     * @return
     */
    private File getReportingDirectory(IProject p) {
        String destination = p.getLocation().toString() + File.separator + ClassInstrumentor.REPORTING_PATH;
        File destinationDir = new File(destination);
        if (!destinationDir.exists()) {
            destinationDir.mkdir();
        }
        return destinationDir;
    }

    /**
     * Return all the source paths of the Project.
     * 
     * @param p
     * @param finder
     * @return
     */
    private Collection<IPath> getSourcePaths(IProject p, FileFinder finder) {
        Collection<IPath> srcPaths = new ArrayList<IPath>();
        IJavaProject javaProject = JavaCore.create(p);
        IClasspathEntry[] entries = new IClasspathEntry[0];
        try {
            entries = javaProject.getRawClasspath();
        } catch (JavaModelException e) {
            CoberclipseLog.error(e);
        }
        for (IClasspathEntry entry : entries) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                srcPaths.add(entry.getPath().removeFirstSegments(1));
                IResource srcDir = ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath());
                if (srcDir != null) {
                    finder.addSourceDirectory(srcDir.getLocation().toString());
                }
            }
        }
        return srcPaths;
    }
}
