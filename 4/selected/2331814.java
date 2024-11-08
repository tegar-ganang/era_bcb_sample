package com.bjSoft.regressionTestTool.codeCoverage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import com.bjSoft.regressionTestTool.Activator;

public class CodeCoverageFacade {

    public CodeCoverageFacade(String instrumentToolLocation, String executionToolLocation, String reportToolLocation) {
    }

    public void runCoverage(String className, IPath path, IFolder projectFolder) {
        if (className.contains("covCase")) {
            runEmma(className, path, projectFolder);
        } else {
            return;
        }
    }

    private void runEmma(String className, IPath path, IFolder folder) {
        try {
            File f = new File(Activator.eclipseLocation + "\\coverage.xml");
            if (f.exists()) {
                f.delete();
            }
            f = new File(Activator.eclipseLocation + "\\coverage.ec");
            if (f.exists()) {
                f.delete();
            }
            Process executionProcess = Runtime.getRuntime().exec(Activator.executionToolLocation + " \"" + path.toOSString() + "\" " + " \"" + folder.getLocation().toOSString() + "\" " + className);
            executionProcess.waitFor();
            Process reportProcess = Runtime.getRuntime().exec(Activator.reportToolLocation);
            reportProcess.waitFor();
            File report = new File("D:\\Programas\\eclipse\\coverage.xml");
            File coverFolder = new File(folder.getLocation().toOSString() + "\\coverageData\\");
            if (!coverFolder.exists()) {
                coverFolder.mkdir();
            }
            if (coverFolder.exists()) {
                File f2 = new File(folder.getLocation().toOSString() + "\\coverageData\\" + className + ".xml");
                if (f2.exists()) {
                    f2.delete();
                }
                if (f2.createNewFile()) {
                    copyFile(report, f2);
                } else {
                    throw new CoverageException("Error in the creation of the file: " + f2.getName());
                }
            } else {
            }
            executionProcess.destroy();
            reportProcess.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void copyFile(File in, File out) throws Exception {
        FileChannel ic = new FileInputStream(in).getChannel();
        FileChannel oc = new FileOutputStream(out).getChannel();
        ic.transferTo(0, ic.size(), oc);
        ic.close();
        oc.close();
    }

    public void instrument(IPath path, IFolder folder) {
        File instFolder = folder.getLocation().toFile();
        try {
            folder.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e1) {
            e1.printStackTrace();
        }
        if (instFolder.exists()) {
            instFolder.delete();
            if (folder.exists()) {
                try {
                    folder.delete(true, null);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
            instFolder.mkdir();
        } else {
            instFolder.mkdir();
        }
        File f = new File(Activator.eclipseLocation + "\\coverage.em");
        if (f.exists()) {
            f.delete();
        }
        try {
            Process instrumentProcess = Runtime.getRuntime().exec(Activator.instrumentToolLocation + " \"" + path.toOSString() + "\\bin\" " + " \"" + folder.getLocation().toOSString() + "\"");
            instrumentProcess.waitFor();
            instrumentProcess.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
