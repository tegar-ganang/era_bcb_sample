package org.musicnotation.gef.ui.editors.score;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.util.AdvancedGraphicalEditor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.util.editorinputs.PathEditorInput;
import org.musicnotation.gef.Engraver;
import org.musicnotation.gef.editparts.score.ScoreEditPartFactory;
import org.musicnotation.gef.ui.views.movements.MovementsPage;
import org.musicnotation.gef.ui.views.overview.OverviewPage;
import org.musicnotation.gef.ui.views.parts.PartsPage;
import org.musicnotation.model.Movement;
import org.musicnotation.model.MusicNotationFactory;
import org.musicnotation.model.Score;
import org.musicnotation.model.Staff;
import org.musicnotation.model.presets.Clefs;

public class ScoreEditor extends AdvancedGraphicalEditor {

    public static final String ID = "org.musicnotation.gef.editor.score";

    public static final String FILTER_EXTENSION = "score";

    public static final String FILTER_NAME = Messages.ScoreEditor_1;

    protected PaletteRoot paletteRoot;

    protected OverviewPage overviewPage;

    protected Score score;

    protected Resource resource;

    protected final Map<Object, Object> options = new HashMap<Object, Object>();

    public ScoreEditor() {
        setEditDomain(new DefaultEditDomain(this));
        options.put(XMIResource.OPTION_ZIP, true);
    }

    @Override
    public void createPartControl(Composite parent) {
        getPalettePreferences().setDockLocation(PositionConstants.WEST);
        getPalettePreferences().setPaletteState(FlyoutPaletteComposite.STATE_PINNED_OPEN);
        getPalettePreferences().setPaletteWidth(152);
        super.createPartControl(parent);
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        getGraphicalViewer().setEditPartFactory(new ScoreEditPartFactory());
        double[] zoomLevels = new double[] { 0.375, 0.5, 0.625, 0.75, 1, 1.25, 1.5, 2, 2.5, 3, 4, 5, 6 };
        getZoomManager().setZoomLevels(zoomLevels);
        scaleZoomLevels(Engraver.DISPLAYED_FONT_SIZE / Engraver.FONT_SIZE);
        ContextMenuProvider contextMenuProvider = new ScoreEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(contextMenuProvider);
        getSite().registerContextMenu(contextMenuProvider, viewer);
        KeyHandler keyHandler = new KeyHandler();
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        keyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
        getGraphicalViewer().setKeyHandler(keyHandler);
    }

    @Override
    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new ScoreEditorPaletteViewerProvider(getEditDomain());
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getGraphicalViewer().setContents(score.getPieces().get(0));
    }

    @Override
    public void doSaveAs() {
        FileDialog fileDialog = new FileDialog(getSite().getShell(), SWT.SAVE);
        fileDialog.setText(MessageFormat.format(Messages.ScoreEditor_2, getPartName()));
        fileDialog.setFilterExtensions(new String[] { FILTER_EXTENSION });
        fileDialog.setFilterNames(new String[] { FILTER_NAME });
        String pathName = fileDialog.open();
        if (pathName != null) {
            if (!pathName.endsWith('.' + FILTER_EXTENSION)) {
                pathName += '.' + FILTER_EXTENSION;
            }
            boolean save = true;
            if (new File(pathName).exists()) {
                save = MessageDialog.openQuestion(getSite().getShell(), Messages.ScoreEditor_3, MessageFormat.format(Messages.ScoreEditor_4, pathName));
            }
            if (save) {
                try {
                    init(getEditorSite(), new PathEditorInput(pathName));
                } catch (PartInitException e) {
                    e.printStackTrace();
                }
                doSave(new NullProgressMonitor());
            }
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (resource.getURI() == null) {
            doSave(monitor);
        } else {
            try {
                resource.save(options);
                getCommandStack().markSaveLocation();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        if (input instanceof PathEditorInput) {
            try {
                resource = new ResourceImpl(URI.createFileURI(((PathEditorInput) input).getName()));
                resource.load(options);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            resource = new ResourceImpl();
            score = MusicNotationFactory.eINSTANCE.createScore();
            Movement movement = MusicNotationFactory.eINSTANCE.createMovement();
            score.getPieces().add(movement);
            Staff staff = MusicNotationFactory.eINSTANCE.createStaff();
            staff.setInitialClef(Clefs.getClef(Messages.ScoreEditor_5));
            movement.getParts().add(staff);
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class type) {
        if (type == PartsPage.class) {
            return new PartsPage(getEditDomain(), score.getPieces().get(0), getActionRegistry());
        } else if (type == MovementsPage.class) {
            return new MovementsPage(getEditDomain(), score, getGraphicalViewer());
        } else if (type == OverviewPage.class) {
            return getOverviewPage();
        } else {
            return super.getAdapter(type);
        }
    }

    protected OverviewPage getOverviewPage() {
        if (overviewPage == null) {
            overviewPage = new OverviewPage(getGraphicalViewer());
        }
        return overviewPage;
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = new ScoreEditorPaletteRoot();
        }
        return paletteRoot;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @Override
    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }
}
