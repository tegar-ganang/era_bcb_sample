package de.htwg.flowchartgenerator.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import de.htwg.flowchartgenerator.ast.model.INode;
import de.htwg.flowchartgenerator.controller.Controller;

/**
 * The node normalizer. Persists the model into the repository.
 * 
 * @author Aldi Alimucaj
 * 
 */
public class NodeSerializer {

    private INode anode;

    private IWorkbenchPart wb;

    private String path;

    private IMethod method;

    public NodeSerializer(INode anode, IWorkbenchPart wb, String fPath, IMethod meth) {
        super();
        this.anode = anode;
        this.wb = wb;
        this.path = fPath;
        this.method = meth;
    }

    /**
	 * Writes the data into the file. Named after the classname_methodname.ff3 convention.
	 */
    public void doWrite() {
        System.out.print("\nSerializing...");
        try {
            IFile ifile = null;
            File file = null;
            if (null != method.getResource()) {
                ifile = method.getJavaProject().getResource().getProject().getFile(Statics.CFG_DIR + Statics.SEPARATOR + path.substring(path.lastIndexOf(Statics.SEPARATOR)));
            }
            file = new File(path);
            if (file.exists()) {
                boolean ans = MessageDialog.openQuestion(wb.getSite().getShell(), "Flow Plug-in", "File already exists. Do you want to overwrite it?");
                if (ans) {
                    file.delete();
                }
            }
            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(path);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                BufferedOutputStream bo = new BufferedOutputStream(oos);
                oos.writeObject(anode);
                oos.flush();
                oos.close();
                InputStream is = new FileInputStream(path);
                if (null != ifile) {
                    if (ifile.exists()) ifile.delete(true, null);
                    ifile.create(is, IResource.NONE, null);
                }
                Path fullpath = new Path(path);
                IDE.openEditorOnFileStore(wb.getSite().getPage(), EFS.getLocalFileSystem().getStore(fullpath));
                method.getResource().refreshLocal(10, null);
                System.out.println("Serializing ...Done!");
            }
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}
