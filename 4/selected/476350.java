package com.byterefinery.rmbench.util;

import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import com.byterefinery.rmbench.exceptions.SystemException;
import com.byterefinery.rmbench.model.Model;
import com.byterefinery.rmbench.util.xml.ModelReader;
import com.byterefinery.rmbench.util.xml.ModelWriter;

/**
 * a model storage that writes to an file resource, using the eclipse resource API
 *  
 * @author cse
 */
public class FileResourceModelStorage implements IModelStorage {

    private final IFile file;

    private Model model;

    private IProgressMonitor progressMonitor;

    public FileResourceModelStorage(IFile file) {
        this.file = file;
    }

    public FileResourceModelStorage(Model model) {
        this.model = model;
        this.file = null;
    }

    public FileResourceModelStorage(IFile file, Model model) {
        this.file = file;
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public IFile getFile() {
        return file;
    }

    public IProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    public void setProgressMonitor(IProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    public boolean isNew() {
        return file == null;
    }

    public void store() throws SystemException {
        if (model == null || file == null) {
            throw new IllegalStateException("model and file must be set");
        }
        if (progressMonitor == null) {
            progressMonitor = new NullProgressMonitor();
        }
        final PipedOutputStream outStream = new PipedOutputStream();
        WriteThread writeThread = new WriteThread(model, outStream);
        try {
            PipedInputStream inStream = new PipedInputStream(outStream);
            writeThread.start();
            if (file.exists()) {
                file.setContents(inStream, IResource.FORCE, progressMonitor);
            } else {
                file.create(inStream, IResource.FORCE, progressMonitor);
            }
        } catch (Exception x) {
            throw new SystemException(x);
        }
        if (writeThread.error != null) {
            throw writeThread.error;
        }
    }

    public void load(LoadListener listener) throws SystemException {
        if (file == null) throw new IllegalStateException("file must be set");
        if (model != null) throw new IllegalStateException("model already set");
        try {
            model = ModelReader.read(file.getContents(), file.getLocation().toOSString(), listener);
        } catch (CoreException e) {
            throw new SystemException(e);
        }
    }

    private static class WriteThread extends Thread {

        private final Model model;

        private final OutputStream outStream;

        SystemException error;

        public WriteThread(Model model, OutputStream outStream) {
            this.model = model;
            this.outStream = outStream;
        }

        public void run() {
            try {
                ModelWriter.write(model, outStream);
            } catch (SystemException e) {
                error = e;
            }
        }
    }
}
