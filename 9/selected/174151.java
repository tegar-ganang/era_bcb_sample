package gafka.editor;

import gafka.model.Model;
import java.io.IOException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;

/**
 * @author Grill Bal�zs
 *
 */
public class Editor extends GraphicalEditor {

    Model model;

    /**
	 * 
	 */
    public Editor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getGraphicalViewer().setEditPartFactory(new ModelEditPartFactory());
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        if (input instanceof IFileEditorInput) {
            try {
                model = Model.load(((IFileEditorInput) input).getFile().getLocation().toFile());
            } catch (IOException e) {
                throw new PartInitException("Can't open file", e);
            }
        } else throw new PartInitException("This editor must be used on a file");
    }

    @Override
    protected void initializeGraphicalViewer() {
        getGraphicalViewer().setContents(model);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        IEditorInput input = getEditorInput();
        if (input instanceof IFileEditorInput) {
            try {
                model.save(((IFileEditorInput) input).getFile().getLocation().toFile());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                monitor.done();
            }
        }
    }
}
