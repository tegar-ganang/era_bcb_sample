package com.farukcankaya.simplemodel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import com.farukcankaya.simplemodel.builder.SimplemodelNature;

@SuppressWarnings("restriction")
public class TestUtil {

    private TestUtil() {
    }

    public static class ASTCompare extends NaiveASTFlattener {

        @Override
        public void endVisit(Javadoc node) {
        }

        @Override
        public boolean visit(Javadoc node) {
            return false;
        }
    }

    public static String getAstString(IFile file) throws CoreException, IOException {
        InputStreamReader reader = new InputStreamReader(file.getContents(), file.getCharset());
        try {
            StringWriter sw = new StringWriter();
            while (true) {
                int read = reader.read();
                if (read < 0) break;
                sw.write(read);
            }
            return getAstString(sw.toString());
        } finally {
            reader.close();
        }
    }

    public static String getAstString(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(source.toCharArray());
        CompilationUnit result = (CompilationUnit) parser.createAST(null);
        final ASTCompare visitor = new ASTCompare();
        result.accept(visitor);
        return visitor.getResult();
    }

    public static void deleteProject(IProject project) throws CoreException {
        project.close(null);
        project.delete(true, true, null);
    }

    public static IProject createSimplemodelEnabledJavaProject() throws CoreException {
        IWorkspaceDescription desc = ResourcesPlugin.getWorkspace().getDescription();
        desc.setAutoBuilding(false);
        ResourcesPlugin.getWorkspace().setDescription(desc);
        String name = "TestProject";
        for (int i = 0; i < 1000; i++) {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name + i);
            if (project.exists()) continue;
            project.create(null);
            project.open(null);
            IProjectDescription description = project.getDescription();
            String[] natures = description.getNatureIds();
            String[] newNatures = new String[natures.length + 2];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = JavaCore.NATURE_ID;
            newNatures[natures.length + 1] = SimplemodelNature.NATURE_ID;
            description.setNatureIds(newNatures);
            project.setDescription(description, null);
            IJavaProject javaProject = JavaCore.create(project);
            Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
            IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
            Path containerPath = new Path(JavaRuntime.JRE_CONTAINER);
            IPath vmPath = containerPath.append(vmInstall.getVMInstallType().getId()).append(vmInstall.getName());
            entries.add(JavaCore.newContainerEntry(vmPath));
            LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
            for (LibraryLocation element : locations) {
                entries.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null));
            }
            final Path srcPath = new Path("src");
            final IFolder src = project.getFolder(srcPath);
            final Path binPath = new Path("bin");
            final IFolder bin = project.getFolder(binPath);
            src.create(true, true, null);
            bin.create(true, true, null);
            entries.add(JavaCore.newSourceEntry(project.getFullPath().append(srcPath)));
            javaProject.setOutputLocation(project.getFullPath().append(binPath), null);
            javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
            return project;
        }
        throw new RuntimeException("Failed");
    }
}
