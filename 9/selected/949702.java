package org.eclipse.smd.gef.editor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.CopyTemplateAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.smd.gef.command.SMDCommand;
import org.eclipse.smd.gef.directedit.StatusLineValidationMessageHandler;
import org.eclipse.smd.gef.dnd.DataEditDropTargetListener;
import org.eclipse.smd.gef.part.StatesMachinesDiagramPart;
import org.eclipse.smd.gef.part.factory.StatesMachinesEditPartFactory;
import org.eclipse.smd.java.action.JavaGenerateAction;
import org.eclipse.smd.model.StatesMachines;
import org.eclipse.smd.rcp.ICommandIds;
import org.eclipse.smd.rcp.action.ExportAsSMCAction;
import org.eclipse.smd.rcp.action.SMCAction;
import org.eclipse.smd.rcp.action.SMDLayoutAction;
import org.eclipse.smd.rcp.action.SMDXMLAction;
import org.eclipse.smd.rcp.action.ValidateRulesAction;
import org.eclipse.smd.rcp.conf.lang.Language;
import org.eclipse.smd.rcp.editorInput.SMDEditorInput;
import org.eclipse.smd.rcp.view.IHierarchyView;
import org.eclipse.smd.rcp.view.SMDHierarchyPage;
import org.eclipse.smd.rcp.view.SMDOutlinePage;
import org.eclipse.smd.smc.ISMParser;
import org.eclipse.smd.smc.ISMWritter;
import org.eclipse.smd.smc.SMParser;
import org.eclipse.smd.smc.SMParserException;
import org.eclipse.smd.smc.SMWritter;
import org.eclipse.smd.xml.editor.XMLEditor;
import org.eclipse.smd.xml.model.IXMLSerializeContributor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Editeur RCP bas� sur GEF. Permet l'�dition graphique de diagramme � �tats-transitions.
 * @author Pierrick HYMBERT (phymbert [at] users.sourceforge.net) 
 */
public class SMDEditor extends GraphicalEditorWithFlyoutPalette {

    /**
     * Les langages support�s par SMC.
     */
    public static String[] SMC_SUPPORTED_LANGUAGES = { "java", "perl", "ruby", "php", "python", "c", "c++" };

    /**
     * L'identifiant de l'�diteur => doit correspondre � celui sp�cifi� dans le plugin.xml
     */
    public static final String EDITOR_ID = "org.eclipse.smd.gef.editor.SMDEditor";

    /**
     * Le mod�le associ� � l'�diteur.
     */
    private StatesMachines statesMachines;

    /**
     * La vue representant le diagramme en cours d'�dition avec draw 2D.
     */
    private SMDOutlinePage smdOutlinePage;

    /**
     * Le logger associ� � l'�diteur.
     */
    private Logger logger = Logger.getLogger(getClass());

    /**
     * La vue hierarchie.
     */
    private SMDHierarchyPage smdHierarchyPage;

    /**
     * L'�diteur XML.
     */
    private XMLEditor xmlEditor = new XMLEditor(this);

    /**
     * Le convertisseur SM.
     */
    private ISMWritter writter = new SMWritter();

    /**
     * Le parseur SM.
     */
    private ISMParser parser = new SMParser();

    /**
     * No-arg constructor
     */
    public SMDEditor() {
        super();
        setEditDomain(new DefaultEditDomain(this));
        logger.debug("Construction d'un nouvel editeur SMD.");
    }

    /**
     * @see org.eclipse.gef.commands.CommandStackListener#commandStackChanged(java.util.EventObject)
     */
    public void commandStackChanged(EventObject event) {
        super.commandStackChanged(event);
        logger.debug("La pile des commandes a �t� modifi�e.");
        firePropertyChange(IEditorPart.PROP_DIRTY);
        Command cmd = getCommandStack().getUndoCommand();
        if (cmd == null) cmd = getCommandStack().getRedoCommand();
        firePropertyChange(cmd);
    }

    /**
     * Appel� par l'�diteur
     */
    public void xmlChange() {
        List<String> actionIds = new ArrayList<String>();
        actionIds.add(ICommandIds.CMD_UPDATE_FROM_XML);
        updateActions(actionIds);
    }

    /**
     * Recherche l'identifiant de la commande modifi�e et pr�vient les listeners.
     * @param cmd
     *            La commande execut�e
     */
    private void firePropertyChange(Command cmd) {
        int propId = -1;
        if (cmd instanceof SMDCommand) {
            SMDCommand smdCmd = (SMDCommand) cmd;
            propId = smdCmd.getId();
            firePropertyChange(propId);
        } else {
            firePropertyChange(propId);
        }
    }

    /**
     * Renvoie une instance en fonction de la classe demand�e.
     */
    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
        logger.debug("Recherche d'un adaptateur pour " + adapter);
        if (adapter == IContentOutlinePage.class) return getSMDOutlinePage();
        if (adapter == IHierarchyView.class) return getSMDHierarchyPage();
        return super.getAdapter(adapter);
    }

    /**
     * Retourne la vue hierarchy.
     * @return La vue hierarchie.
     */
    private Object getSMDHierarchyPage() {
        if (null == smdHierarchyPage) {
            logger.debug("Instancie la vue hierarchy de l'�diteur.");
            smdHierarchyPage = new SMDHierarchyPage(this);
        }
        return smdHierarchyPage;
    }

    /**
     * Retourne la vue outline.
     * @return La vue outline.
     */
    private SMDOutlinePage getSMDOutlinePage() {
        if (null == smdOutlinePage && null != getGraphicalViewer()) {
            logger.debug("Instancie la vue outline de l'�diteur.");
            RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
            if (rootEditPart instanceof ScalableFreeformRootEditPart) {
                smdOutlinePage = new SMDOutlinePage((ScalableFreeformRootEditPart) rootEditPart);
            }
        }
        return smdOutlinePage;
    }

    /**
     * Retourne les touches clavier � l'�diteur GEF. ( Suppression(127) et DirectEdit(F2))
     * @return Le gestionnaire des touches clavier.
     */
    protected KeyHandler getCommonKeyHandler() {
        logger.debug("Instancie le gestionnaire des touches claviers");
        KeyHandler sharedKeyHandler = new KeyHandler();
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
        return sharedKeyHandler;
    }

    /**
     * Cr�e les actions propres � l'�diteur.
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
     */
    @SuppressWarnings("unchecked")
    protected void createActions() {
        logger.debug("Cr�e les actions li�es � l'editeur.");
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        IAction action = null;
        for (String language : SMDEditor.SMC_SUPPORTED_LANGUAGES) {
            action = new SMCAction(this, language);
            registry.registerAction(action);
            getStackActions().add(action.getId());
        }
        action = new ExportAsSMCAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new SMDLayoutAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new SMDXMLAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new JavaGenerateAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new ValidateRulesAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new CopyTemplateAction(this);
        registry.registerAction(action);
        action = new MatchWidthAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new MatchHeightAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new DirectEditAction((IWorkbenchPart) this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.LEFT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.RIGHT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.TOP);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.BOTTOM);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.CENTER);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.MIDDLE);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new DirectEditAction((IWorkbenchPart) this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    /**
     * Retourne l'edit part racine de cet editeur.
     * @return L'edit part racine de cet editeur.
     */
    public StatesMachinesDiagramPart getStatesMachinesEditPart() {
        GraphicalViewer viewer = getGraphicalViewer();
        if (viewer == null) return null;
        RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
        if (rootEditPart == null) return null;
        return (StatesMachinesDiagramPart) rootEditPart.getChildren().get(0);
    }

    /**
     * Permet la sauvegarde sous forme binaire du fichier *.smd associ� � l'�diteur graphique.
     * @see EditorPart#doSave
     */
    public void doSave(IProgressMonitor monitor) {
        try {
            logger.debug("Effectue la sauvegarde du mod�le.");
            File file = ((SMDEditorInput) getEditorInput()).getFile();
            if (file == null) {
                doSaveAs();
                return;
            }
            if (file.getName().endsWith("smd")) {
                saveAsSMD(file.getAbsolutePath());
            } else {
                saveAsXMLSMD(file.getAbsolutePath());
            }
            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            logger.error("Imposssible de sauvegarder.", e);
        }
    }

    /**
     * Sauvegarde le mod�le en ouvrant une boite de dialogue pour selectionner un nouveau fichier.
     */
    public void doSaveAs() {
        logger.debug("Effectue la sauvegarde sous du mod�le.");
        FileDialog fileDialog = new FileDialog(getEditorSite().getWorkbenchWindow().getShell(), SWT.SAVE);
        fileDialog.setFileName(getEditorInput().getName() != null ? getEditorInput().getName() : "sans_titre.smd");
        fileDialog.setFilterExtensions(new String[] { "*.smd", "*.smd.xml", "*.sm" });
        fileDialog.setFilterNames(new String[] { "SMD (*.smd)", "XML SMD (*.smd.xml)", "SMC (*.sm)" });
        fileDialog.setText("Save SMD editor");
        String fileName = fileDialog.open();
        if (fileName == null) return;
        try {
            logger.debug("Sauvegarde du mod�le sous " + fileName);
            if (fileName.endsWith("smd")) {
                saveAsSMD(fileName);
                setInput(new SMDEditorInput(fileName));
                getCommandStack().markSaveLocation();
            } else if (fileName.endsWith("smd.xml")) {
                saveAsXMLSMD(fileName);
                setInput(new SMDEditorInput(fileName));
                getCommandStack().markSaveLocation();
            } else if (fileName.endsWith("sm")) {
                saveAsSM(fileName);
            }
        } catch (Exception e) {
            logger.error("Imposssible de sauvegarder.", e);
            e.printStackTrace();
            MessageDialog.openInformation(getSite().getShell(), "Erreur durant la sauveagarde du diagramme", e.getMessage());
        }
    }

    /**
     * Sauvegarde en java.
     * @param fileName
     *            Le nom du fichier.
     * @throws Exception
     *             Si une erreur survient.
     */
    protected void saveAsSMD(String fileName) throws Exception {
        FileOutputStream fo = null;
        ObjectOutputStream out = null;
        try {
            fo = new FileOutputStream(fileName);
            out = new ObjectOutputStream(fo);
            out.writeObject(getStatesMachines());
        } catch (IOException e) {
            logger.error("Erreur durant la sauvegarde du mod�le", e);
            throw e;
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fo != null) try {
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sauvegarde en XML.
     * @param fileName
     *            Le nom du fichier.
     * @throws Exception
     *             Si une erreur survient.
     */
    protected void saveAsXMLSMD(String fileName) throws Exception {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            Document document = getXMLEditor().getDocument();
            ((IXMLSerializeContributor) getStatesMachines().getAdapter(IXMLSerializeContributor.class)).serializeToXML(document, null, null, null);
            StringBuffer xmlNote = new StringBuffer();
            getXMLEditor().parcourArbre(new String(), document.getDocumentElement(), xmlNote, 0, new ArrayList<StyleRange>());
            StringBuffer buffer = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
            buffer.append(xmlNote);
            fw = new FileWriter(fileName);
            bw = new BufferedWriter(fw);
            bw.write(buffer.toString());
        } catch (IOException e) {
            logger.error("Erreur durant la sauvegarde du mod�le", e);
            throw e;
        } finally {
            if (bw != null) try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fw != null) try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sauvegarde en SM.
     * @param fileName
     *            Le nom du fichier.
     * @throws Exception
     *             Si une erreur survient.
     */
    public void saveAsSM(String fileName) throws Exception {
        try {
            getSMWritter().write(getStatesMachines(), fileName);
        } catch (SMParserException e) {
            logger.error("Erreur durant la sauvegarde du mod�le", e);
            throw e;
        }
    }

    /**
     * On autorise le sauvegarde sous.
     */
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Associe l'input avec notre editeur. L'input doit �tre de type SMDInputEditot.
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    protected void setInput(IEditorInput input) {
        logger.debug("L'editeur a un nouvel input : " + input);
        super.setInput(input);
        File file = ((SMDEditorInput) input).getFile();
        setPartName(file != null ? file.getName() : Language.get(Language.SANS_TITRE));
        setTitleToolTip(getPartName());
        if (file == null) {
            setStatesMachines(new StatesMachines());
            getStatesMachines().setName(Language.get(Language.SANS_TITRE));
        } else {
            try {
                logger.debug("Lecture de l'input.");
                if (file.getName().endsWith("smd")) {
                    InputStream is = new FileInputStream(file);
                    ObjectInputStream ois = new ObjectInputStream(is);
                    StatesMachines statesMachines = (StatesMachines) ois.readObject();
                    if (statesMachines.getName() == null) {
                        statesMachines.setName(getPartName());
                    }
                    setStatesMachines(statesMachines);
                    ois.close();
                } else if (file.getName().endsWith("smd.xml")) {
                    getXMLEditor().getParser().parse(new InputSource(new FileInputStream(file)));
                    Document document = getXMLEditor().getParser().getDocument();
                    StatesMachines newStatesMachines = new StatesMachines();
                    ((IXMLSerializeContributor) newStatesMachines.getAdapter(IXMLSerializeContributor.class)).readFromXML(document, document.getDocumentElement(), null, null);
                    setStatesMachines(newStatesMachines);
                } else if (file.getName().endsWith("sm")) {
                    StatesMachines newStatesMachines = getParser().parse(file.getAbsolutePath());
                    setStatesMachines(newStatesMachines);
                } else {
                    throw new Exception("Extension not known: " + file.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e);
                setStatesMachines(new StatesMachines());
                getStatesMachines().setName(Language.get(Language.SANS_TITRE));
                MessageDialog.openError(getSite().getShell(), "Erreur durant l'ouverture du diagramme", e.getMessage());
            }
        }
    }

    /**
     * Retourne le fournisseur de notre palette.
     * @return Le fournisseur de notre palette.
     */
    protected PaletteViewerProvider createPaletteViewerProvider() {
        logger.debug("Instancie le fournisseur de palette.");
        return new SMDPaletteViewerProvider(getEditDomain());
    }

    /**
     * Instancie l'editeur graphique GEF.
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#createGraphicalViewer(Composite)
     */
    protected void createGraphicalViewer(Composite parent) {
        logger.debug("Cr�ation du graphical viewer.");
        StatusLineValidationMessageHandler validationMessageHandler = new StatusLineValidationMessageHandler(getEditorSite());
        GraphicalViewer viewer = new ValidationEnabledGraphicalViewer(validationMessageHandler);
        viewer.createControl(parent);
        setGraphicalViewer(viewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();
    }

    /**
     * Configure l'editeur graphique GEF.
     */
    @SuppressWarnings({ "deprecation", "deprecation" })
    protected void configureGraphicalViewer() {
        logger.debug("Configuration du graphical viewer.");
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();
        List<String> zoomLevels = new ArrayList<String>(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        root.getZoomManager().setZoomLevelContributions(zoomLevels);
        IAction zoomIn = new ZoomInAction(root.getZoomManager());
        IAction zoomOut = new ZoomOutAction(root.getZoomManager());
        getActionRegistry().registerAction(zoomIn);
        getActionRegistry().registerAction(zoomOut);
        getSite().getKeyBindingService().registerAction(zoomIn);
        getSite().getKeyBindingService().registerAction(zoomOut);
        viewer.setRootEditPart(root);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer).setParent(getCommonKeyHandler()));
        viewer.addDropTargetListener(new DataEditDropTargetListener(viewer));
        viewer.setEditPartFactory(new StatesMachinesEditPartFactory());
        viewer.setContents(getStatesMachines());
        getEditDomain().addViewer(getGraphicalViewer());
    }

    /**
     * Retourne les pr�ferences pour la palette.
     * @return Les pr�ferences pour la palette.
     */
    protected FlyoutPreferences getPalettePreferences() {
        logger.debug("Instancie les pr�f�rences pour la palette.");
        return new PaletteFlyoutPreferences();
    }

    /**
     * Retourne la palette racine.
     * @return La palette racine.
     */
    protected PaletteRoot getPaletteRoot() {
        logger.debug("Instancie le cr�ateur de la palette.");
        return new PaletteViewerCreator().createPaletteRoot();
    }

    /**
     * Retourne le mod�le � �tats transitions.
     * @return Le mod�le � �tats transitions.
     */
    public StatesMachines getStatesMachines() {
        return statesMachines;
    }

    /**
     * D�finit le mod�le � �tats transitions.
     * @param statesMachines
     */
    public void setStatesMachines(StatesMachines statesMachines) {
        this.statesMachines = statesMachines;
        if (getGraphicalViewer() != null) {
            getGraphicalViewer().setContents(this.statesMachines);
        }
        logger.debug("L'editeur � un nouveau mod�le : " + statesMachines);
    }

    /**
     * Surcharge la cr�ation des widgets de l'�diteur graphique pour cr�er la partie �diteur XML.
     * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
        Composite gefComposite = createGefComposite(tabFolder);
        Composite xmlComposite = createXmlComposite(tabFolder);
        TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        tabItem.setText("Graphical");
        tabItem.setControl(gefComposite);
        TabItem tabItem1 = new TabItem(tabFolder, SWT.NONE);
        tabItem1.setText("XML");
        tabItem1.setControl(xmlComposite);
        super.createPartControl(gefComposite);
        getXMLEditor().createPartControl(xmlComposite);
    }

    /**
     * This method initializes gefComposite
     */
    private Composite createGefComposite(TabFolder tabFolder) {
        Composite gefComposite = new Composite(tabFolder, SWT.NONE);
        gefComposite.setLayout(new FillLayout());
        return gefComposite;
    }

    /**
     * This method initializes xmlComposite
     */
    private Composite createXmlComposite(TabFolder tabFolder) {
        Composite xmlComposite = new Composite(tabFolder, SWT.NONE);
        xmlComposite.setLayout(new FillLayout());
        return xmlComposite;
    }

    /**
     * Instancie l'editeur XML s'il n'existe pas.
     * @return
     */
    public XMLEditor getXMLEditor() {
        return xmlEditor;
    }

    /**
     * @return the parser
     */
    public ISMWritter getSMWritter() {
        return writter;
    }

    /**
     * @return the parser
     */
    protected ISMParser getParser() {
        return parser;
    }
}
