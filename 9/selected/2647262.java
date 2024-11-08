package zaphod.project1.editparts;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import zaphod.project1.model.internal.Document;

public class Project1Editor extends GraphicalEditor {

    public static final String ID = "zaphod.project1.editor1";

    private Document document = null;

    public Project1Editor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        IEditorInput input = getEditorInput();
        if (input instanceof FileEditorInput) {
            FileEditorInput newInput = (FileEditorInput) input;
            try {
                document = Document.createDocumentFromFile(newInput.getFile());
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        viewer.setContents(document);
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new Project1EditPartFactory());
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }
}
