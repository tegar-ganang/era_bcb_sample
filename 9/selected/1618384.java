package aurora.ide.meta.gef.editors;

import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public abstract class FlayoutBMGEFEditor extends GraphicalEditorWithFlyoutPalette {

    private FlyoutPaletteComposite splitter;

    private DatasetView datasetView;

    public void createPartControl(Composite parent) {
        SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
        SashForm c = new SashForm(sashForm, SWT.VERTICAL | SWT.BORDER);
        createBMViewer(c);
        createPropertyViewer(c);
        Composite cpt = new Composite(sashForm, SWT.NONE);
        cpt.setLayout(new GridLayout());
        Composite bottom = new Composite(cpt, SWT.NONE);
        bottom.setLayoutData(new GridData(GridData.FILL_BOTH));
        bottom.setLayout(new FillLayout());
        super.createPartControl(bottom);
        sashForm.setWeights(new int[] { 1, 4 });
    }

    protected void initDatasetView() {
    }

    @Override
    protected void createGraphicalViewer(Composite parent) {
        super.createGraphicalViewer(parent);
    }

    public DatasetView getDatasetView() {
        return datasetView;
    }

    protected abstract void createPropertyViewer(Composite c);

    protected abstract void createBMViewer(Composite c);
}
