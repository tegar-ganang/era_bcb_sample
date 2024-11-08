package org.sedml.jlibsedml.ui.sedmlexpander.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Unzips a SED-ML archive into its component files.
 * @author radams
 *
 */
public class SedMLExpander extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        System.err.println(" Expanding sedx archive...");
        IStructuredSelection iss = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
        if (iss == null) {
            return null;
        }
        IFile selected = (IFile) iss.getFirstElement();
        try {
            byte[] buf = new byte[1024];
            System.err.println(selected.getLocation().toOSString());
            ZipFile zipFile = new ZipFile(selected.getLocation().toOSString());
            Enumeration entries = zipFile.entries();
            InputStream zis = null;
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                {
                    String entryName = entry.getName();
                    System.out.println("entryname " + entryName);
                    int n;
                    FileOutputStream fileoutputstream;
                    File newFile = new File(entryName);
                    String directory = newFile.getParent();
                    if (directory == null) {
                        if (newFile.isDirectory()) break;
                    }
                    fileoutputstream = new FileOutputStream(selected.getParent().getLocation().toOSString() + File.separator + entryName);
                    zis = zipFile.getInputStream(entry);
                    while ((n = zis.read(buf, 0, 1024)) > -1) fileoutputstream.write(buf, 0, n);
                    fileoutputstream.close();
                    zis.close();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            selected.getParent().refreshLocal(IResource.DEPTH_ONE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }
}
