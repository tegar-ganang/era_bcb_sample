package zaphod.toy.gef.japanexample.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import zaphod.toy.gef.japanexample.model.ContentsModel;
import zaphod.toy.gef.japanexample.model.HelloModel;
import zaphod.toy.gef.japanexample.model.MyEditPartFactory;

public class HelloWorldEditor extends GraphicalEditor {

    public HelloWorldEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        ContentsModel parent = new ContentsModel();
        HelloModel child;
        child = new HelloModel();
        child.setConstraint(new Rectangle(30, 30, -1, -1));
        parent.addChild(child);
        child = new HelloModel();
        child.setConstraint(new Rectangle(0, 0, -1, -1));
        parent.addChild(child);
        child = new HelloModel();
        child.setConstraint(new Rectangle(10, 80, 80, 50));
        parent.addChild(child);
        viewer.setContents(parent);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new MyEditPartFactory());
    }
}
