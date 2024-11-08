package org.wsmostudio.ui;

import java.util.*;
import java.util.List;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.editparts.*;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.omwg.ontology.Concept;
import org.omwg.ontology.Instance;
import org.sbpm.bpmo.ProcessFragment;
import org.sbpm.upo.factory.Factory;
import org.sbpm.upo.factory.UPOFactory;
import org.wsmo.common.IRI;
import org.wsmostudio.bpmo.model.*;
import org.wsmostudio.bpmo.model.io.BPMOImporter;
import org.wsmostudio.bpmo.ui.editor.editpart.GraphicalPartFactory;
import org.wsmostudio.bpmo.ui.editor.layout.BPMOModelLayouter;
import org.wsmostudio.runtime.*;
import org.wsmostudio.ui.editors.common.IWSMOSelectionValidator;
import org.wsmostudio.ui.editors.common.WSMOChooser;
import org.wsmostudio.ui.views.navigator.WSMOContentProvider;

public class FragmentsResultDialog {

    private Shell shell;

    private List<ProcessFragment> results;

    private CheckboxTableViewer fragmentsTable;

    private Composite previewArea;

    private ScrollingGraphicalViewer viewer;

    private Label selectionMonitorLab;

    private Button selectBpgButton, clearBpgButton, okButton;

    private Text bpgIRI;

    private String customPreviewScale = null;

    private UPOFactory upoFactory = null;

    private Map<ProcessFragment, WorkflowEntityNode> uiModelsCache = new HashMap<ProcessFragment, WorkflowEntityNode>();

    public FragmentsResultDialog(Shell parent, List<ProcessFragment> fragments) {
        this.results = fragments;
        this.shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.SYSTEM_MODAL);
        Image icon = JFaceResources.getImageRegistry().get(WsmoImageRegistry.DEFAULT_WINDOW_ICON);
        shell.setImage(icon);
        shell.setText("Fragmenter Results");
        shell.setSize(800, 550);
        shell.setLayout(new GridLayout(1, false));
        constructDialogContent(shell);
        createControlArea(shell);
        Point pLocation = parent.getLocation();
        Point pSize = parent.getSize();
        shell.setLocation(pLocation.x + pSize.x / 2 - shell.getSize().x / 2, pLocation.y + pSize.y / 2 - shell.getSize().y / 2);
    }

    public void open() {
        this.shell.open();
        Display display = this.shell.getDisplay();
        while (!this.shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private void constructDialogContent(Composite parent) {
        SashForm splitter = new SashForm(parent, SWT.HORIZONTAL);
        splitter.setLayoutData(new GridData(GridData.FILL_BOTH));
        Group fragmentsGroup = new Group(splitter, SWT.NONE);
        fragmentsGroup.setLayout(new GridLayout(1, false));
        fragmentsGroup.setText("Result Fragments");
        fragmentsTable = CheckboxTableViewer.newCheckList(fragmentsGroup, SWT.NONE);
        fragmentsTable.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        fragmentsTable.setContentProvider(new ArrayContentProvider());
        fragmentsTable.setLabelProvider(new LabelProvider() {

            public Image getImage(Object element) {
                return JFaceResources.getImage(WsmoImageRegistry.INSTANCE_ICON);
            }

            public String getText(Object element) {
                if (element == null) {
                    return "";
                }
                if (element instanceof ProcessFragment) {
                    ProcessFragment frag = (ProcessFragment) element;
                    String label = (frag.getName() == null) ? " <no-fragment-name>" : frag.getName();
                    if (frag.getDescription() != null) {
                        label += "  [" + Utils.normalizeSpaces(frag.getDescription()) + ']';
                    }
                    return label;
                }
                return element.toString();
            }
        });
        fragmentsTable.setInput(results.toArray());
        final MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager mgr) {
                if (false == GUIHelper.containsCursor(fragmentsTable.getTable())) {
                    return;
                }
                if (false == fragmentsTable.getSelection().isEmpty()) {
                    menuMgr.add(new Action("Edit Name") {

                        public void run() {
                            doEditName();
                        }
                    });
                    menuMgr.add(new Action("Edit Description") {

                        public void run() {
                            doEditDescription();
                        }
                    });
                    menuMgr.add(new Separator());
                }
                menuMgr.add(new Action("Select All") {

                    public void run() {
                        fragmentsTable.setAllChecked(true);
                        updateSelectionMonitor();
                    }
                });
                menuMgr.add(new Separator());
                menuMgr.add(new Action("Unselect All") {

                    public void run() {
                        fragmentsTable.setAllChecked(false);
                        updateSelectionMonitor();
                    }
                });
            }
        });
        fragmentsTable.getTable().setMenu(menuMgr.createContextMenu(fragmentsTable.getTable()));
        fragmentsTable.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                updatePreviewPanel((IStructuredSelection) event.getSelection());
            }
        });
        new FragmentsToolTipProvider(this.fragmentsTable.getTable());
        Group previewGroup = new Group(splitter, SWT.NONE);
        previewGroup.setLayout(new GridLayout(1, false));
        previewGroup.setText("Fragment Preview");
        createZoomToolbar(previewGroup);
        previewArea = new Composite(previewGroup, SWT.BORDER);
        previewArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        previewArea.setLayout(new GridLayout(1, false));
        viewer = new ScrollingGraphicalViewer();
        viewer.createControl(previewArea);
        ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        viewer.setEditPartFactory(new GraphicalPartFactory());
        viewer.getControl().setBackground(ColorConstants.listBackground);
        viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        ZoomManager zoomManager = rootEditPart.getZoomManager();
        ArrayList<String> zoomContributions = new ArrayList<String>();
        zoomContributions.add(ZoomManager.FIT_ALL);
        zoomContributions.add(ZoomManager.FIT_HEIGHT);
        zoomContributions.add(ZoomManager.FIT_WIDTH);
        zoomManager.setZoomLevelContributions(zoomContributions);
        zoomManager.setZoomLevels(new double[] { 0.25, 0.33, 0.5, 0.75, 1.0 });
        zoomManager.setZoom(1.0);
        Composite businessGoalPanel = new Composite(previewGroup, SWT.NONE);
        businessGoalPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        businessGoalPanel.setLayout(new GridLayout(4, false));
        Label lab = new Label(businessGoalPanel, SWT.NONE);
        lab.setText("Process goal:");
        bpgIRI = new Text(businessGoalPanel, SWT.BORDER | SWT.READ_ONLY);
        bpgIRI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        selectBpgButton = new Button(businessGoalPanel, SWT.NONE);
        selectBpgButton.setText("Select");
        selectBpgButton.setEnabled(false);
        selectBpgButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent s) {
                doSelectProcessGoal();
            }
        });
        clearBpgButton = new Button(businessGoalPanel, SWT.NONE);
        clearBpgButton.setText("Clear");
        clearBpgButton.setEnabled(false);
        clearBpgButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent s) {
                IStructuredSelection sel = (IStructuredSelection) fragmentsTable.getSelection();
                if (sel.isEmpty() || false == sel.getFirstElement() instanceof ProcessFragment) {
                    return;
                }
                ((ProcessFragment) sel.getFirstElement()).setBusinessProcessGoal(null);
                updatePreviewPanel(sel);
            }
        });
        splitter.setWeights(new int[] { 1, 2 });
    }

    private void createZoomToolbar(Composite parent) {
        Composite toolbar = new Composite(parent, SWT.NONE);
        toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolbar.setLayout(new GridLayout(2, false));
        new Label(toolbar, SWT.NONE).setText("Select zoom level : ");
        final Combo combo = new Combo(toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setItems(new String[] { "25%", "33%", "50%", "75%", "100%", "Page", "Height ", "Width " });
        combo.select(4);
        combo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int i = combo.getSelectionIndex();
                ZoomManager zoomManager = ((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getZoomManager();
                customPreviewScale = null;
                switch(i) {
                    case 0:
                        zoomManager.setZoom(0.25);
                        break;
                    case 1:
                        zoomManager.setZoom(0.33);
                        break;
                    case 2:
                        zoomManager.setZoom(0.50);
                        break;
                    case 3:
                        zoomManager.setZoom(0.75);
                        break;
                    case 4:
                        zoomManager.setZoom(1.0);
                        break;
                    case 5:
                        zoomManager.setZoomAsText(ZoomManager.FIT_ALL);
                        customPreviewScale = ZoomManager.FIT_ALL;
                        break;
                    case 6:
                        zoomManager.setZoomAsText(ZoomManager.FIT_HEIGHT);
                        customPreviewScale = ZoomManager.FIT_HEIGHT;
                        break;
                    case 7:
                        zoomManager.setZoomAsText(ZoomManager.FIT_WIDTH);
                        customPreviewScale = ZoomManager.FIT_WIDTH;
                }
            }
        });
    }

    private void createControlArea(Composite mainContainer) {
        Composite buttons = new Composite(mainContainer, SWT.NONE);
        buttons.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        buttons.setLayout(new GridLayout(3, false));
        selectionMonitorLab = new Label(buttons, SWT.NONE);
        selectionMonitorLab.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
        selectionMonitorLab.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        okButton = new Button(buttons, SWT.PUSH);
        okButton.setText("Send to Library");
        okButton.setEnabled(false);
        okButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                MessageDialog.openWarning(shell, "Unsupported Operation", "Integration with BPL not implemented!");
            }
        });
        shell.setDefaultButton(okButton);
        Button noButton = new Button(buttons, SWT.PUSH);
        noButton.setText("   Close   ");
        noButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });
        this.fragmentsTable.addCheckStateListener(new ICheckStateListener() {

            public void checkStateChanged(CheckStateChangedEvent event) {
                updateSelectionMonitor();
            }
        });
        updateSelectionMonitor();
    }

    private void updatePreviewPanel(IStructuredSelection sel) {
        ProcessFragment fr = null;
        if (sel.isEmpty() == false && sel.getFirstElement() instanceof ProcessFragment) {
            fr = (ProcessFragment) sel.getFirstElement();
        }
        selectBpgButton.setEnabled(fr != null);
        bpgIRI.setEnabled(fr != null);
        clearBpgButton.setEnabled(fr != null && fr.getBusinessProcessGoal() != null);
        updateGraphicalViewer(fr);
        if (fr != null) {
            bpgIRI.setText((fr.getBusinessProcessGoal() == null) ? "" : fr.getBusinessProcessGoal().getInstanceIdentity().toString());
        } else {
            bpgIRI.setText("");
        }
    }

    private void updateGraphicalViewer(ProcessFragment selected) {
        WorkflowEntityNode model = uiModelsCache.get(selected);
        if (model == null) {
            if (selected != null) {
                Set<WorkflowEntityNode> guiNodes = null;
                try {
                    guiNodes = new BPMOImporter().importProcessFragment(selected);
                } catch (Exception ex) {
                    guiNodes = new HashSet<WorkflowEntityNode>();
                    LogManager.logError(ex);
                }
                if (guiNodes.size() > 1) {
                    model = new ProcessNode("");
                    for (WorkflowEntityNode node : guiNodes) {
                        ((ProcessNode) model).addWorkflow(node);
                    }
                } else if (guiNodes.size() == 1) {
                    model = guiNodes.iterator().next();
                } else {
                    model = new BpmoModel();
                }
            } else {
                model = new BpmoModel();
            }
            uiModelsCache.put(selected, model);
            if (model instanceof WorkflowEntitiesContainer) {
                BPMOModelLayouter.doLayout((WorkflowEntitiesContainer) model);
            }
        }
        viewer.setContents(model);
        if (customPreviewScale != null) {
            ((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getZoomManager().setZoomAsText(customPreviewScale);
        }
        previewArea.layout();
    }

    private void updateSelectionMonitor() {
        selectionMonitorLab.setText("Selected " + fragmentsTable.getCheckedElements().length + " of " + results.size() + " process fragment(s)");
        okButton.setEnabled(fragmentsTable.getCheckedElements().length > 0);
    }

    private void doSelectProcessGoal() {
        IStructuredSelection sel = (IStructuredSelection) this.fragmentsTable.getSelection();
        if (sel.isEmpty() || false == sel.getFirstElement() instanceof ProcessFragment) {
            return;
        }
        ProcessFragment fr = (ProcessFragment) sel.getFirstElement();
        WSMOChooser chooser = new WSMOChooser(Display.getCurrent().getActiveShell(), SWT.SINGLE, WSMORuntime.getRuntime(), new WSMOContentProvider(WSMOContentProvider.INSTANCE_MASK));
        chooser.setFilter(new IWSMOSelectionValidator() {

            public String isValid(Object selection) {
                if (false == selection instanceof Instance) {
                    return "Please select a Instance";
                }
                Instance selected = (Instance) selection;
                Set<Concept> toDo = selected.listConcepts();
                while (toDo.size() > 0) {
                    Set<Concept> newToDo = new HashSet<Concept>();
                    for (Concept cnt : toDo) {
                        if (cnt.getIdentifier().equals(WorkflowPropertyUtils.BUSINESS_PROCESS_GOAL_ID)) {
                            return null;
                        }
                        newToDo.addAll(cnt.listSuperConcepts());
                    }
                    toDo = newToDo;
                }
                return "Instance not of proper type";
            }
        });
        chooser.setDialogTitle("Select Busuness Process Goal Instance");
        Instance instVal = (Instance) chooser.open();
        if (instVal != null) {
            if (this.upoFactory == null) {
                this.upoFactory = Factory.createFactory(null);
            }
            fr.setBusinessProcessGoal(this.upoFactory.getBusinessProcessGoal((IRI) instVal.getIdentifier()));
        }
        updatePreviewPanel(sel);
    }

    private void doEditDescription() {
        ProcessFragment frag = (ProcessFragment) ((IStructuredSelection) fragmentsTable.getSelection()).getFirstElement();
        InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Process Fragment Description", "Description text : ", frag.getDescription(), null);
        if (dialog.open() != Window.OK) {
            return;
        }
        String newValue = dialog.getValue();
        if (newValue != null && newValue.trim().length() == 0) {
            newValue = null;
        }
        frag.setDescription(newValue);
        fragmentsTable.refresh(frag, true);
    }

    private void doEditName() {
        ProcessFragment frag = (ProcessFragment) ((IStructuredSelection) fragmentsTable.getSelection()).getFirstElement();
        InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Process Fragment Name", "Name : ", frag.getName(), null);
        if (dialog.open() != Window.OK) {
            return;
        }
        String newValue = dialog.getValue();
        if (newValue != null && newValue.trim().length() == 0) {
            newValue = null;
        }
        frag.setName(newValue);
        fragmentsTable.refresh(frag, true);
    }
}
