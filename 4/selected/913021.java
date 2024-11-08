package org.wsmostudio.repository.ui;

import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.Workbench;
import org.omwg.ontology.Ontology;
import org.wsmo.common.*;
import org.wsmo.datastore.WsmoRepository;
import org.wsmo.factory.WsmoFactory;
import org.wsmo.mediator.Mediator;
import org.wsmo.service.Goal;
import org.wsmo.service.WebService;
import org.wsmostudio.repository.ExtensionManager;
import org.wsmostudio.repository.Registry;
import org.wsmostudio.repository.ui.wizards.ExportEntitiesWizard;
import org.wsmostudio.repository.ui.wizards.SaveEntityWizard;
import org.wsmostudio.runtime.*;
import org.wsmostudio.ui.GUIHelper;
import org.wsmostudio.ui.WsmoUIPlugin;
import org.wsmostudio.ui.editors.WSMOEditorInput;
import org.wsmostudio.ui.editors.model.ObservableModel;

/**
 * A helper UI component which appears as a part of the <i>RepositoryEditor</i>
 * component. Its task is to visualize a list of all available entities of certain
 * type within a repository instance. The basic operations are addition, retrieval
 * and removal of entities.
 *
 * @author not attributable
 * @version $Revision: 1224 $ $Date: 2007-07-19 08:54:57 -0400 (Thu, 19 Jul 2007) $
 */
public class WSMOEntityContainer {

    public static final byte ONTOLOGY_CONTENT = 1;

    public static final byte MEDIATOR_CONTENT = 2;

    public static final byte GOAL_CONTENT = 3;

    public static final byte SERVICE_CONTENT = 4;

    private Table entitiesList;

    private WsmoRepository repository;

    private byte entityType;

    public WSMOEntityContainer(TabFolder parentFolder, RepositoryEditor editor, WsmoRepository repo, String panelTitle, byte contentType) {
        this.entityType = contentType;
        this.repository = repo;
        TabItem mainItem = new TabItem(parentFolder, SWT.BORDER);
        parentFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        mainItem.setText(panelTitle);
        parentFolder.setLayout(new GridLayout(1, false));
        createTable(parentFolder);
        createContextMenu();
        initDNDTarget(editor);
        mainItem.setControl(entitiesList);
    }

    public void reloadContent() {
        List<IRI> newIRIs = getContent();
        TableItem[] allItems = entitiesList.getItems();
        for (int i = 0; i < allItems.length; i++) {
            Identifier id = (Identifier) allItems[i].getData();
            if (newIRIs.contains(id)) {
                newIRIs.remove(id);
            } else {
                allItems[i].dispose();
            }
        }
        for (Identifier newID : newIRIs) {
            addEntry(newID);
        }
        entitiesList.redraw();
    }

    private void createTable(Composite parent) {
        entitiesList = new Table(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        entitiesList.setLayoutData(new GridData(GridData.FILL_BOTH));
        entitiesList.setLinesVisible(false);
        for (Iterator it = getContent().iterator(); it.hasNext(); ) {
            Identifier entityID = (Identifier) it.next();
            addEntry(entityID);
        }
    }

    private void initDNDTarget(final RepositoryEditor editor) {
        DropTarget target = new DropTarget(entitiesList, DND.DROP_COPY | DND.DROP_DEFAULT);
        final FileTransfer fileTransfer = FileTransfer.getInstance();
        target.setTransfer(new Transfer[] { fileTransfer });
        target.addDropListener(new DropTargetAdapter() {

            public void dragEnter(DropTargetEvent event) {
                if (event.detail == DND.DROP_DEFAULT) {
                    if (fileTransfer.isSupportedType(event.currentDataType)) {
                        String[] fileNames = (String[]) fileTransfer.nativeToJava(event.currentDataType);
                        for (int i = 0; i < fileNames.length; i++) {
                            if (false == fileNames[i].toLowerCase().endsWith(".wsml")) {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        }
                    }
                    event.detail = DND.DROP_COPY;
                }
            }

            public void drop(DropTargetEvent event) {
                if (fileTransfer.isSupportedType(event.currentDataType)) {
                    String[] names = (String[]) event.data;
                    for (int i = 0; i < names.length; i++) {
                        editor.addResourceFromFile(names[i]);
                    }
                }
            }
        });
    }

    public boolean containsEntry(Identifier entityRef) {
        if (getItemForIdentifier(entityRef) != null) {
            boolean confirm = MessageDialog.openConfirm(PlatformUI.getWorkbench().getDisplay().getActiveShell(), "Overwrite " + getTypeAsText(), getTypeAsText() + " '" + entityRef.toString() + "' already exists in the repository!" + "\nDo you want to overwrite it?");
            if (false == confirm) {
                return true;
            } else {
                removeEntityFromRepository((IRI) entityRef);
                getItemForIdentifier(entityRef).dispose();
            }
        }
        return false;
    }

    public void addEntry(Identifier entityRef) {
        TableItem item = new TableItem(entitiesList, SWT.None);
        item.setData(entityRef);
        item.setImage(getRelevantImage());
        item.setText(entityRef.toString());
    }

    private void createContextMenu() {
        final MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager mgr) {
                fillContextMenu(menuMgr);
            }
        });
        entitiesList.setMenu(menuMgr.createContextMenu(entitiesList));
    }

    private void fillContextMenu(MenuManager menuMgr) {
        if (false == GUIHelper.containsCursor(entitiesList)) {
            return;
        }
        if (entityType != MEDIATOR_CONTENT) {
            menuMgr.add(new Action("Create " + getTypeAsText()) {

                public void run() {
                    doCreateEntity(getText());
                }
            });
        } else {
            menuMgr.add(new Action("Create OO Mediator") {

                public void run() {
                    doCreateEntity(getText());
                }
            });
            menuMgr.add(new Action("Create WW Mediator") {

                public void run() {
                    doCreateEntity(getText());
                }
            });
            menuMgr.add(new Action("Create WG Mediator") {

                public void run() {
                    doCreateEntity(getText());
                }
            });
            menuMgr.add(new Action("Create GG Mediator") {

                public void run() {
                    doCreateEntity(getText());
                }
            });
        }
        menuMgr.add(new Separator());
        menuMgr.add(new Action("Import from Workspace") {

            public void run() {
                doExportEntities();
            }
        });
        TableItem[] sel = entitiesList.getSelection();
        if (sel != null && sel.length > 0 && GUIHelper.containsCursor(sel[0].getBounds(), entitiesList)) {
            menuMgr.add(new Separator());
            menuMgr.add(new Action("View " + getTypeAsText()) {

                public void run() {
                    doOpenEntity();
                }
            });
            menuMgr.add(new Separator());
            menuMgr.add(new Action("Save in Workspace") {

                public void run() {
                    doSaveEntity();
                }
            });
            menuMgr.add(new Separator());
            menuMgr.add(new Action("Remove " + getTypeAsText()) {

                public void run() {
                    doRemoveEntity();
                }
            });
            Registry.getInstance().getContentActionsManager().initActions(menuMgr, ExtensionManager.getRepositoryType(repository), this.entityType, repository, (IRI) sel[0].getData());
        }
    }

    public void dispose() {
        entitiesList.dispose();
    }

    private List<IRI> getContent() {
        switch(entityType) {
            case ONTOLOGY_CONTENT:
                return repository.listOntologies();
            case MEDIATOR_CONTENT:
                return repository.listMediators();
            case GOAL_CONTENT:
                return repository.listGoals();
            case SERVICE_CONTENT:
                return repository.listWebServices();
        }
        return null;
    }

    private String getTypeAsText() {
        switch(entityType) {
            case ONTOLOGY_CONTENT:
                return "Ontology";
            case MEDIATOR_CONTENT:
                return "Mediator";
            case GOAL_CONTENT:
                return "Goal";
            case SERVICE_CONTENT:
                return "WebService";
        }
        return "";
    }

    private Image getRelevantImage() {
        String iconType;
        switch(entityType) {
            case ONTOLOGY_CONTENT:
                iconType = WsmoImageRegistry.ONTOLOGY_ICON;
                break;
            case MEDIATOR_CONTENT:
                iconType = WsmoImageRegistry.OOMEDIATOR_ICON;
                break;
            case GOAL_CONTENT:
                iconType = WsmoImageRegistry.GOAL_ICON;
                break;
            case SERVICE_CONTENT:
                iconType = WsmoImageRegistry.WEBSERVICE_ICON;
                break;
            default:
                return null;
        }
        return JFaceResources.getImageRegistry().get(iconType);
    }

    private TopEntity makeAndPersistEntity(WsmoFactory factory, IRI id, String actionName) {
        switch(entityType) {
            case ONTOLOGY_CONTENT:
                Ontology onto = factory.createOntology(id);
                repository.addOntology(onto);
                return onto;
            case MEDIATOR_CONTENT:
                Mediator medi = null;
                if (actionName.indexOf(" OO ") != -1) {
                    medi = factory.createOOMediator(id);
                } else if (actionName.indexOf(" WW ") != -1) {
                    medi = factory.createWWMediator(id);
                } else if (actionName.indexOf(" WG ") != -1) {
                    medi = factory.createWGMediator(id);
                } else {
                    medi = factory.createGGMediator(id);
                }
                repository.addMediator(medi);
                return medi;
            case GOAL_CONTENT:
                Goal goal = factory.createGoal(id);
                repository.addGoal(goal);
                return goal;
            case SERVICE_CONTENT:
                WebService ws = factory.createWebService(id);
                repository.addWebService(ws);
                return ws;
        }
        return null;
    }

    private void removeEntityFromRepository(IRI id) {
        switch(entityType) {
            case ONTOLOGY_CONTENT:
                repository.deleteOntology(id);
                break;
            case MEDIATOR_CONTENT:
                repository.deleteMediator(id);
                break;
            case GOAL_CONTENT:
                repository.deleteGoal(id);
                break;
            case SERVICE_CONTENT:
                repository.deleteWebService(id);
                break;
        }
    }

    private void doOpenEntity() {
        TableItem[] sel = entitiesList.getSelection();
        TopEntity topEntity = null;
        try {
            topEntity = retrieveTopEntity(repository, (IRI) sel[0].getData(), entityType);
        } catch (Throwable anyError) {
            MessageDialog.openError(entitiesList.getShell(), "Repository Error", anyError.getMessage());
            LogManager.logError(anyError);
            return;
        }
        if (topEntity == null) {
            return;
        }
        try {
            String targetEditorID = WsmoUIPlugin.getDefault().getExtensionManager().locateEditorForEntity(topEntity);
            if (targetEditorID == null) {
                return;
            }
            ObservableModel model = GUIHelper.createEditorModel(topEntity, targetEditorID);
            WSMOEditorInput input = new WSMOEditorInput(topEntity, model);
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, targetEditorID);
            model.setChanged();
        } catch (PartInitException e) {
            LogManager.logError(e);
        }
    }

    private void doSaveEntity() {
        TableItem[] sel = entitiesList.getSelection();
        IRI entityID = (IRI) sel[0].getData();
        IWorkbenchWizard wizard = new SaveEntityWizard("Save " + getTypeAsText(), repository, entityID, entityType);
        wizard.init(Workbench.getInstance(), new StructuredSelection());
        WizardDialog dialog = new WizardDialog(entitiesList.getShell(), wizard);
        dialog.open();
    }

    private void doCreateEntity(String action) {
        InputDialog iDialog = new InputDialog(entitiesList.getShell(), "New " + getTypeAsText(), "Name:", null, null);
        iDialog.open();
        String newEntityName = iDialog.getValue();
        if (newEntityName == null || newEntityName.trim().length() == 0) {
            return;
        }
        WsmoFactory wsmoFactory = WSMORuntime.getRuntime().getWsmoFactory();
        IRI entityRef = null;
        try {
            entityRef = wsmoFactory.createIRI(newEntityName);
        } catch (Exception iae) {
            MessageDialog.openError(entitiesList.getShell(), "Invalid Identifier", iae.getMessage());
            return;
        }
        if (getItemForIdentifier(entityRef) != null) {
            MessageDialog.openError(entitiesList.getShell(), "Name Clash", "Entity name '" + newEntityName + "' already in use!");
            return;
        }
        makeAndPersistEntity(wsmoFactory, entityRef, action);
        addEntry(entityRef);
        entitiesList.redraw();
    }

    private void doRemoveEntity() {
        TableItem[] sel = entitiesList.getSelection();
        IRI entityID = (IRI) sel[0].getData();
        removeEntityFromRepository(entityID);
        sel[0].dispose();
        entitiesList.redraw();
    }

    private void doExportEntities() {
        IWizard wizard = new ExportEntitiesWizard();
        WizardDialog dialog = new WizardDialog(entitiesList.getShell(), wizard);
        dialog.open();
    }

    private TableItem getItemForIdentifier(Identifier id) {
        TableItem[] items = entitiesList.getItems();
        for (int i = 0; i < items.length; i++) {
            if (id.equals(items[i].getData())) {
                return items[i];
            }
        }
        return null;
    }

    public static TopEntity retrieveTopEntity(WsmoRepository repo, IRI id, byte eType) {
        TopEntity result = null;
        switch(eType) {
            case WSMOEntityContainer.ONTOLOGY_CONTENT:
                result = repo.getOntology(id);
                break;
            case WSMOEntityContainer.MEDIATOR_CONTENT:
                result = repo.getMediator(id);
                break;
            case WSMOEntityContainer.GOAL_CONTENT:
                result = repo.getGoal(id);
                break;
            case WSMOEntityContainer.SERVICE_CONTENT:
                result = repo.getWebService(id);
        }
        return result;
    }
}
