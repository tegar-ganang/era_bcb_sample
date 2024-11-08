package fr.insa_rennes.pcreator.editiongraphique;

import java.awt.Composite;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.CreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import fr.insa_rennes.pcreator.Application;
import fr.insa_rennes.pcreator.PCreatorConstantes;
import fr.insa_rennes.pcreator.editiongraphique.model.Chaine;
import fr.insa_rennes.pcreator.editiongraphique.model.ContraintePrecedence;
import fr.insa_rennes.pcreator.editiongraphique.model.Element;
import fr.insa_rennes.pcreator.editiongraphique.model.Entree;
import fr.insa_rennes.pcreator.editiongraphique.model.Pause;
import fr.insa_rennes.pcreator.editiongraphique.model.Service;
import fr.insa_rennes.pcreator.editiongraphique.model.Sortie;
import fr.insa_rennes.pcreator.editiongraphique.model.Tube;
import fr.insa_rennes.pcreator.editiongraphique.part.AppEditPartFactory;
import fr.insa_rennes.pcreator.editiongraphique.part.ChainePart;

public class ChaineGraphicalEditor extends GraphicalEditorWithPalette implements ISelectionProvider, PropertyChangeListener, ISaveablePart {

    public static final String ID = "fr.insa_rennes.pcreator.editiongraphique.mygraphicaleditor";

    /**
	 * En gros, permet de savoir s'il y a des modifs non enregistrées.
	 */
    private boolean isDirty;

    public ChaineGraphicalEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void initializeGraphicalViewer() {
        boolean previousIsDirty = false;
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.addDropTargetListener(new FileTransferDropTargetListener(viewer));
        IEditorInput input = getEditorInput();
        Chaine chaine = null;
        if (input instanceof IFileEditorInput) {
            IPath path = ((IFileEditorInput) input).getFile().getLocation();
            try {
                FileInputStream fistream = new FileInputStream(path.toString());
                ObjectInputStream oistream = new ObjectInputStream(fistream);
                Object chaineObj = oistream.readObject();
                if (chaineObj instanceof Chaine) {
                    chaine = (Chaine) chaineObj;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (InvalidClassException e) {
                Application.logger.error("La chaîne a été créée avec une version précédente de PCreator, elle ne peut être ouverte.\n" + "        Une nouvelle chaîne par défaut a été créée. Si vous l'enregistrez, elle écrasera la précédente.");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (chaine == null) {
            chaine = createChaine();
            previousIsDirty = true;
        }
        chaine.addPropertyChangeListener(this);
        viewer.setContents(chaine);
        this.setPartName(chaine.getNom());
        getSite().setSelectionProvider(this);
        chaine.getListeners().firePropertyChange(Element.PROPERTY_MISE_EN_NIVEAUX, null, chaine);
        isDirty = previousIsDirty;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        Chaine chaine = getChaine();
        IEditorInput input = getEditorInput();
        if (input instanceof IFileEditorInput) {
            IPath path = ((IFileEditorInput) input).getFile().getLocation();
            FileOutputStream fostream;
            try {
                fostream = new FileOutputStream(path.toString());
                ObjectOutputStream oostream = new ObjectOutputStream(fostream);
                oostream.writeObject(chaine);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        isDirty = false;
        super.firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        Application.refreshWorkspace(monitor);
    }

    protected void configureGraphicalViewer() {
        double[] zoomLevels;
        ArrayList<String> zoomContributions;
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new AppEditPartFactory());
        ContextMenuProvider provider = new AppContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        ZoomManager manager = rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(manager));
        getActionRegistry().registerAction(new ZoomOutAction(manager));
        zoomLevels = new double[] { 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 10.0, 20.0 };
        manager.setZoomLevels(zoomLevels);
        zoomContributions = new ArrayList<String>();
        zoomContributions.add(ZoomManager.FIT_ALL);
        zoomContributions.add(ZoomManager.FIT_HEIGHT);
        zoomContributions.add(ZoomManager.FIT_WIDTH);
        manager.setZoomLevelContributions(zoomContributions);
        KeyHandler keyHandler = new KeyHandler();
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
        keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
        viewer.setKeyHandler(keyHandler);
        keyHandler = new KeyHandler();
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
        keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
        viewer.setKeyHandler(keyHandler);
        viewer.setContextMenu(provider);
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) {
            return ((ScalableRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        }
        return super.getAdapter(type);
    }

    /**
	 * Crée une nouvelle chaîne avec quelques valeurs par défaut
	 * @return une nouvelle chaîne vide
	 */
    public Chaine createChaine() {
        Chaine chaine = new Chaine();
        chaine.setNom("Nouvelle chaîne");
        chaine.setVersion("1.0");
        chaine.setDescription("[Description par défaut]");
        return chaine;
    }

    /**
	 * Pour le développement seulement, crée une chaîne complète
	 * @return une chaîne entière
	 */
    public Chaine CreateDebugChaine() {
        Chaine chaine = new Chaine();
        chaine.setNom("Nom  De  La  C h a i n e");
        chaine.setVersion("100.gdf.65");
        chaine.setDescription("Da chaine, what else ?");
        Service service1 = chaine.ajouterService("NomService1", 2, 2);
        service1.getInfos().setId_service("un_petit_srvice_svp");
        service1.getInfos().setEmplacement("/Users/romain/Documents/INSA/4INFO/Systeme/tp3_q2/VoitureN.java");
        Service service2 = chaine.ajouterService("NomService2", 2, 2);
        service2.getInfos().setId_service("234");
        service2.getInfos().setSysteme(true);
        Entree entree1 = service1.getEntree(0);
        entree1.setNom("entree1");
        entree1.setDescription("Voic la descfsdflmsflsdrpsrerzp");
        entree1.setValeur_par_defaut("Et ta soeur ?");
        entree1.setValeur("Et ta soeur ?");
        entree1.setType("png");
        entree1.setIdEntree("1");
        Entree entree2 = service1.getEntree(1);
        entree2.setNom("entree2");
        entree2.setType("text/int");
        entree2.setValeur_par_defaut("2");
        entree2.setValeur("val defaut 2");
        entree2.setIdEntree("2");
        Sortie sortie1 = service1.getSortie(0);
        sortie1.setNom("sortie1");
        sortie1.setId_sortie("s1");
        Sortie sortie2 = service1.getSortie(1);
        sortie2.setNom("sortie2");
        sortie2.setId_sortie("s2");
        Entree entree3 = service2.getEntree(0);
        entree3.setNom("entree3");
        entree3.setType(Entree.REPERTOIRE);
        entree3.setValeur_par_defaut(new Double(3.3).toString());
        entree3.setValeur(new Double(3.3).toString());
        entree3.setIdEntree("3");
        entree3.devenirEntreeChaine("desc");
        Entree entree4 = service2.getEntree(1);
        entree4.setNom("entree4");
        entree4.setIdEntree("4");
        entree4.setType(Entree.REPERTOIRE);
        Sortie sortie3 = service2.getSortie(0);
        sortie3.setNom("sortie3");
        sortie3.setId_sortie("s3");
        sortie3.setType("text/int");
        sortie3.setDescription("desc sortie 3");
        sortie3.setType_prod(PCreatorConstantes.TYPE_SORTIE_SIMPLE);
        sortie3.setNom_fichier("elgringo.jpg");
        Sortie sortie4 = service2.getSortie(1);
        sortie4.setNom("sortie4");
        sortie4.setId_sortie("s4");
        sortie4.setType("png");
        sortie4.setDescription("desc sortie 4");
        sortie4.setType_prod(PCreatorConstantes.TYPE_SORTIE_LOT);
        sortie4.setId_entree_associee("4");
        chaine.ajouterTube(sortie3, entree2);
        chaine.ajouterTube(sortie4, entree1);
        Service service3 = chaine.ajouterService("nomService3", 1, 1);
        Service service4 = chaine.ajouterService("nomService4", 1, 1);
        Service service5 = chaine.ajouterService("nomService5", 2, 1);
        Service service6 = chaine.ajouterService("nomService6", 1, 1);
        Service service7 = chaine.ajouterService("nomService7", 2, 1);
        chaine.ajouterTube(service3.getSortie(0), service5.getEntree(0));
        chaine.ajouterTube(service4.getSortie(0), service5.getEntree(1));
        chaine.ajouterTube(service5.getSortie(0), service7.getEntree(0));
        chaine.ajouterTube(service6.getSortie(0), service7.getEntree(1));
        chaine.ajouterContraintePrecedence(service3, service4);
        return chaine;
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        PaletteRoot root = new PaletteRoot();
        PaletteGroup manipGroup = new PaletteGroup("Manipulation d'objets");
        root.add(manipGroup);
        SelectionToolEntry selectionToolEntry = new SelectionToolEntry();
        manipGroup.add(selectionToolEntry);
        MarqueeToolEntry mte = new MarqueeToolEntry();
        mte.setDescription("Outil de sélection multiple - pour les services seulement");
        root.setDefaultEntry(selectionToolEntry);
        PaletteSeparator sep2 = new PaletteSeparator();
        root.add(sep2);
        PaletteGroup toolGroup = new PaletteGroup("Outils");
        ToolEntry tool;
        tool = new ConnectionCreationToolEntry("Tube", "Relier les entrées et les sorties des services", new CreationFactory() {

            public Object getNewObject() {
                return null;
            }

            public Object getObjectType() {
                return Tube.class;
            }
        }, null, null);
        toolGroup.add(tool);
        ToolEntry toolPrecedence;
        toolPrecedence = new ConnectionCreationToolEntry("Précédence", "Définir une contrainte de précédence entre deux services", new CreationFactory() {

            public Object getNewObject() {
                return null;
            }

            public Object getObjectType() {
                return ContraintePrecedence.class;
            }
        }, null, null);
        toolGroup.add(toolPrecedence);
        PaletteSeparator sep3 = new PaletteSeparator();
        toolGroup.add(sep3);
        ToolEntry toolPause;
        toolPause = new CreationToolEntry("Pause", "Imposer une pause sur une connection", new CreationFactory() {

            public Object getNewObject() {
                return null;
            }

            public Object getObjectType() {
                return Pause.class;
            }
        }, null, null);
        toolGroup.add(toolPause);
        root.add(toolGroup);
        return root;
    }

    private KeyHandler keyHandler;

    protected class OutlinePage extends ContentOutlinePage {

        private SashForm sash;

        private ScrollableThumbnail thumbnail;

        private DisposeListener disposeListener;

        public OutlinePage() {
            super(new TreeViewer());
        }

        public void createControl(Composite parent) {
            sash = new SashForm((org.eclipse.swt.widgets.Composite) parent, SWT.VERTICAL);
            getViewer().createControl(sash);
            getViewer().setEditDomain(getEditDomain());
            getSelectionSynchronizer().addViewer(getViewer());
            Canvas canvas = new Canvas(sash, SWT.BORDER);
            LightweightSystem lws = new LightweightSystem(canvas);
            thumbnail = new ScrollableThumbnail((Viewport) ((ScalableRootEditPart) getGraphicalViewer().getRootEditPart()).getFigure());
            thumbnail.setSource(((ScalableRootEditPart) getGraphicalViewer().getRootEditPart()).getLayer(LayerConstants.PRINTABLE_LAYERS));
            lws.setContents(thumbnail);
            disposeListener = new DisposeListener() {

                public void widgetDisposed(DisposeEvent e) {
                    if (thumbnail != null) {
                        thumbnail.deactivate();
                        thumbnail = null;
                    }
                }
            };
            getGraphicalViewer().getControl().addDisposeListener(disposeListener);
        }

        public void init(IPageSite pageSite) {
            super.init(pageSite);
            IActionBars bars = getSite().getActionBars();
            bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), getActionRegistry().getAction(ActionFactory.UNDO.getId()));
            bars.setGlobalActionHandler(ActionFactory.REDO.getId(), getActionRegistry().getAction(ActionFactory.REDO.getId()));
            bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
            bars.updateActionBars();
            getViewer().setKeyHandler(keyHandler);
            ContextMenuProvider provider = new AppContextMenuProvider(getViewer(), getActionRegistry());
            getViewer().setContextMenu(provider);
        }

        public Control getControl() {
            return sash;
        }

        public void dispose() {
            getSelectionSynchronizer().removeViewer(getViewer());
            if (getGraphicalViewer().getControl() != null && !getGraphicalViewer().getControl().isDisposed()) getGraphicalViewer().getControl().removeDisposeListener(disposeListener);
            super.dispose();
        }
    }

    /**
	 * List of selection change listeners (element type:
	 * <code>ISelectionChangedListener</code>).
	 * 
	 * @see #fireSelectionChanged
	 */
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        getGraphicalViewer().addSelectionChangedListener(listener);
    }

    public ISelection getSelection() {
        return getGraphicalViewer().getSelection();
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        getGraphicalViewer().removeSelectionChangedListener(listener);
    }

    public void setSelection(ISelection selection) {
        getGraphicalViewer().setSelection(selection);
    }

    /**
	 * @return the chaine
	 */
    public Chaine getChaine() {
        EditPart part = getGraphicalViewer().getContents();
        if (part instanceof ChainePart) {
            if (((ChainePart) part).getModel() instanceof Chaine) {
                return (Chaine) ((ChainePart) part).getModel();
            }
        }
        return null;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        isDirty = true;
        super.firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        if (evt.getPropertyName() == "NodeRename") {
            this.setPartName(getChaine().getNom());
        }
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }
}
