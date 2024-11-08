package fr.insa.rennes.pelias.pcreator.editors;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.CreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.tools.CreationTool;
import org.eclipse.gef.tools.MarqueeSelectionTool;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.EditorPart;
import fr.insa.rennes.pelias.framework.Chain;
import fr.insa.rennes.pelias.framework.ChainCall;
import fr.insa.rennes.pelias.framework.ChainComponent;
import fr.insa.rennes.pelias.framework.ChainInput;
import fr.insa.rennes.pelias.framework.ChainOutput;
import fr.insa.rennes.pelias.framework.FlowManager;
import fr.insa.rennes.pelias.framework.Pipe;
import fr.insa.rennes.pelias.framework.Service;
import fr.insa.rennes.pelias.framework.ServiceCall;
import fr.insa.rennes.pelias.pcreator.Application;
import fr.insa.rennes.pelias.pcreator.editors.chains.ChainTransferDropTargetListener;
import fr.insa.rennes.pelias.pcreator.editors.chains.SavedComponent;
import fr.insa.rennes.pelias.pcreator.editors.chains.editparts.PCreatorEditPartFactory;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.ChainComponentInputModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.ChainComponentModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.ChainComponentOutputModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.ChainInputModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.ChainModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.ChainOutputModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.CommentModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.Element;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.EndpointModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.PipeModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.PrecedenceModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.ServiceModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.SubChainModel;
import fr.insa.rennes.pelias.pcreator.editors.chains.models.SelectionModel;
import fr.insa.rennes.pelias.pcreator.views.ChainNavigator;
import fr.insa.rennes.pelias.platform.IRepository;
import fr.insa.rennes.pelias.platform.ISxSRepository;
import fr.insa.rennes.pelias.platform.PObjectNotFoundException;
import fr.insa.rennes.pelias.platform.PObjectReference;
import fr.insa.rennes.pelias.platform.PSxSObjectReference;
import fr.insa.rennes.pelias.platform.Version;

/**
 * Editeur graphique de Chaînes
 * @author Julien
 * Modifiée par Kevin pour l'intégration avec les Repository
 */
public class ChainEditor extends GraphicalEditorWithPalette {

    public static String ID = "fr.insa.rennes.pelias.editor.chaineditor";

    private ISxSRepository<Chain> chainRepository;

    private ISxSRepository<Service> serviceRepository;

    private Chain chain;

    private Chain chaineModifie;

    private ChainModel chainModel;

    public static UUID chainAttachmentID = UUID.fromString("76D3A248-1251-49CE-86D5-839793DCF7C6");

    private boolean keepVersion = true;

    public ChainEditor() {
        setEditDomain(new DefaultEditDomain(this));
        chainRepository = Application.getCurrentChainRepository();
        serviceRepository = Application.getCurrentServiceRepository();
    }

    public GraphicalViewer getMonViewer() {
        return getGraphicalViewer();
    }

    /**
	 * Une chaine de test
	 * @return
	 */
    private ChainModel createChainExemple() {
        ChainModel cm = new ChainModel(chain);
        for (PObjectReference p : serviceRepository.enumerateObjects()) {
            Service s = (Service) p.resolve(serviceRepository, false, false);
            ServiceModel model = new ServiceModel(s);
            model.setLayout(new Rectangle(100, 30, 80, 80));
            cm.addChild(model);
        }
        for (PObjectReference p : serviceRepository.enumerateObjects()) {
            Service s = (Service) p.resolve(serviceRepository, false, false);
            ServiceModel model = new ServiceModel(s);
            model.setLayout(new Rectangle(100, 30, 80, 80));
            cm.addChild(model);
        }
        return cm;
    }

    @Override
    public void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        PObjectReference reference = ((ChainEditorInput) this.getEditorInput()).getInput();
        String attachment = "";
        try {
            if (reference instanceof PSxSObjectReference) {
                chain = ((PSxSObjectReference) reference).resolve(chainRepository, false, false, false);
                if (chain != null && reference.attachmentExists(chainRepository, chainAttachmentID)) {
                    attachment = ((PSxSObjectReference) reference).getAttachment(chainRepository, chainAttachmentID, false);
                }
            } else {
                chain = reference.resolve((IRepository<Chain>) chainRepository, false, false);
                if (chain != null && reference.attachmentExists(chainRepository, chainAttachmentID)) {
                    attachment = reference.getAttachment(chainRepository, chainAttachmentID);
                }
            }
        } catch (PObjectNotFoundException e) {
        }
        chainModel = generateModelDependencies(attachment, false);
        chainModel.resetDirty();
        viewer.addDropTargetListener(new ChainTransferDropTargetListener(viewer));
        viewer.setContents(chainModel);
        setPartName(chainModel.getContent().getLabel() + " " + chainModel.getContent().getVersion());
    }

    /**
	 * Méthode qui génère les modèles de tous les composants de la chaîne
	 * en résolvant toutes les références au répository à partir de 
	 * l'élément Chain passé en paramètre. Le ChainModel en retour est le
	 * modèle de la chaine passé en paramètre.
	 * @param test true si l'on génère une chaine de test
	 * @return modèle de la chaîne dont toutes les références ont été résolues
	 */
    private ChainModel generateModelDependencies(String attachment, boolean test) {
        if (test) return createChainExemple();
        int nbChainInputs = 0;
        int nbChainOutputs = 0;
        Chain toCopy = chain;
        String description = chain.getDescription();
        chain = new Chain(chain.getId(), chain.getLabel(), chain.getVersion());
        chain.setDescription(description);
        ChainModel tmpChainModel = new ChainModel(chain);
        ArrayList<Rectangle> inputs = new ArrayList<Rectangle>();
        ArrayList<Rectangle> outputs = new ArrayList<Rectangle>();
        HashMap<String, Rectangle> services = new HashMap<String, Rectangle>();
        if (attachment != null && !attachment.equals("")) {
            ByteArrayInputStream ss = new ByteArrayInputStream(attachment.getBytes());
            XMLDecoder d = new XMLDecoder(ss);
            try {
                Object o = d.readObject();
                while (o != null) {
                    SavedComponent c = (SavedComponent) o;
                    if (c.getReferencedClass() == ChainInputModel.class) {
                        inputs.add(new Rectangle(c.getX(), c.getY(), c.getWidth(), c.getHeight()));
                    } else if (c.getReferencedClass() == ServiceModel.class || c.getReferencedClass() == SubChainModel.class) {
                        services.put(c.getId(), new Rectangle(c.getX(), c.getY(), c.getWidth(), c.getHeight()));
                    } else if (c.getReferencedClass() == ChainOutputModel.class) {
                        outputs.add(new Rectangle(c.getX(), c.getY(), c.getWidth(), c.getHeight()));
                    } else if (c.getReferencedClass() == CommentModel.class) {
                        CommentModel comment = new CommentModel(c.getProp());
                        comment.setLayout(new Rectangle(c.getX(), c.getY(), c.getWidth(), c.getHeight()));
                        tmpChainModel.addChild(comment);
                    }
                    o = d.readObject();
                }
            } catch (Exception e) {
            } finally {
                d.close();
            }
        }
        HashMap<ChainComponent, Element> justAdded = new HashMap<ChainComponent, Element>();
        HashMap<ChainComponent, Element> alreadyAdded = new HashMap<ChainComponent, Element>();
        Rectangle defaultInputLayout = new Rectangle(50, 50, 20, 20);
        Rectangle defaultServiceLayout = new Rectangle(50, 130, 80, 80);
        Rectangle defaultOutputLayout = new Rectangle(50, 500, 20, 20);
        ArrayList<Pipe> toDelete = new ArrayList<Pipe>();
        for (ChainInput inputTmp : toCopy.getInputs()) {
            ChainInput input = new ChainInput();
            input.setName(inputTmp.getName());
            input.setType(inputTmp.getType());
            ChainInputModel chainInputModel = new ChainInputModel(input);
            if (inputs.size() > nbChainInputs) {
                chainInputModel.setLayout(inputs.get(nbChainInputs++));
            } else {
                chainInputModel.setLayout(new Rectangle(defaultInputLayout));
                defaultInputLayout.x += 40;
            }
            tmpChainModel.addChild(chainInputModel);
            ArrayList<Pipe> tmpPipes = new ArrayList<Pipe>(inputTmp.getPipes());
            for (Pipe p : tmpPipes) {
                ChainComponent comp = p.getDestination();
                ChainComponentModel elt = null;
                if (alreadyAdded.containsKey(comp)) {
                    ChainComponentModel chainComponentModel = (ChainComponentModel) alreadyAdded.get(comp);
                    ChainComponentInputModel chainComponentInputModel = (ChainComponentInputModel) chainComponentModel.getInput(p.getDestinationInputIndex());
                    if (!tmpChainModel.addPipe(chainInputModel, chainComponentInputModel)) toDelete.add(p);
                } else {
                    if (comp instanceof ServiceCall) {
                        ServiceCall serviceCall = (ServiceCall) comp;
                        Service service = serviceCall.getService(serviceRepository);
                        if (service != null) {
                            elt = new ServiceModel(service);
                            ((ServiceModel) elt).setExecutionMode(serviceCall.getExecutionMode());
                        }
                    } else if (comp instanceof ChainCall) {
                        ChainCall chainCall = (ChainCall) comp;
                        Chain c = chainCall.getChain(chainRepository);
                        if (c != null) elt = new SubChainModel(c);
                    }
                    if (elt != null) {
                        if (services.containsKey(comp.getInstanceId())) {
                            elt.setLayout(services.get(comp.getInstanceId()));
                        } else {
                            elt.setLayout(new Rectangle(defaultServiceLayout));
                            defaultServiceLayout.x += 120;
                        }
                        tmpChainModel.addChild(elt);
                        ChainComponentInputModel ccInputModel = (ChainComponentInputModel) elt.getInput(p.getDestinationInputIndex());
                        if (!tmpChainModel.addPipe(chainInputModel, ccInputModel)) toDelete.add(p);
                        justAdded.put(comp, elt);
                        alreadyAdded.put(comp, elt);
                    } else {
                        toDelete.add(p);
                    }
                }
            }
        }
        generateRecDependencies(tmpChainModel, justAdded, alreadyAdded, services, defaultServiceLayout);
        for (ChainOutput output : toCopy.getOutputs()) {
            ChainOutputModel chainOutputModel = new ChainOutputModel(output);
            if (outputs.size() > nbChainOutputs) {
                chainOutputModel.setLayout(outputs.get(nbChainOutputs++));
            } else {
                chainOutputModel.setLayout(new Rectangle(defaultOutputLayout));
                defaultOutputLayout.x += 40;
            }
            tmpChainModel.addChild(chainOutputModel);
            Pipe pipe = output.getPipe();
            ChainComponentModel elt = (ChainComponentModel) alreadyAdded.get(pipe.getSource());
            if (elt != null) {
                ChainComponentOutputModel ccOutput = (ChainComponentOutputModel) elt.getOutput(pipe.getSourceOutputIndex());
                if (!tmpChainModel.addPipe(ccOutput, chainOutputModel)) toDelete.add(pipe);
            } else {
                tmpChainModel.removeChild(chainOutputModel);
                toDelete.add(pipe);
            }
        }
        for (Pipe p : toDelete) {
            boolean found = false;
            for (Element e : tmpChainModel.getChildren()) {
                if (e instanceof PipeModel) {
                    if (((PipeModel) e).getContent() == p) {
                        found = true;
                        ((PipeModel) e).disconnect();
                    }
                }
            }
            if (!found) {
                if (p.getSource() != null) p.getSource().unbind(p);
                if (p.getDestination() != null) p.getDestination().unbind(p);
            }
        }
        return tmpChainModel;
    }

    /**
	 * Génère récursivement les modèles des dépendances de la chaine
	 * @param tmpChainModel le modèle de la chaine auquel on ajoute les éléments
	 * @param toCheck la liste des composants dont les dépendances sont à vérifiées
	 * @param alreadyAdded la liste des composants déjà ajoutés
	 * @param services 
	 * @param defaultServiceLayout 
	 */
    private void generateRecDependencies(ChainModel tmpChainModel, HashMap<ChainComponent, Element> toCheck, HashMap<ChainComponent, Element> alreadyAdded, HashMap<String, Rectangle> services, Rectangle defaultServiceLayout) {
        defaultServiceLayout.x = 50;
        defaultServiceLayout.y += 140;
        if (toCheck.size() == 0) return;
        HashMap<ChainComponent, Element> justAdded = new HashMap<ChainComponent, Element>();
        ArrayList<FlowManager> toDelete = new ArrayList<FlowManager>();
        for (ChainComponent comp : toCheck.keySet()) {
            for (FlowManager flow : comp.getSuccessors()) {
                ChainComponent destination = flow.getDestination();
                Element elt = null;
                Element pipeSrc = null;
                Element pipeTarget = null;
                if (destination != null) {
                    if (alreadyAdded.containsKey(destination)) {
                        elt = alreadyAdded.get(destination);
                    } else {
                        if (destination instanceof ServiceCall) {
                            ServiceCall serviceCall = (ServiceCall) destination;
                            Service service = serviceCall.getService(serviceRepository);
                            if (service != null) {
                                elt = new ServiceModel(service);
                                ((ServiceModel) elt).setExecutionMode(serviceCall.getExecutionMode());
                            }
                        } else if (destination instanceof ChainCall) {
                            ChainCall chainCall = (ChainCall) destination;
                            Chain c = chainCall.getChain(chainRepository);
                            if (c != null) elt = new SubChainModel(c);
                        }
                        if (elt != null) {
                            if (services.containsKey(destination.getInstanceId())) {
                                elt.setLayout(services.get(destination.getInstanceId()));
                            } else {
                                elt.setLayout(new Rectangle(defaultServiceLayout));
                                defaultServiceLayout.x += 120;
                            }
                            tmpChainModel.addChild(elt);
                            justAdded.put(destination, elt);
                            alreadyAdded.put(destination, elt);
                        }
                    }
                    if (elt != null) {
                        if (flow instanceof Pipe) {
                            pipeSrc = ((ChainComponentModel) toCheck.get(comp)).getOutput(((Pipe) flow).getSourceOutputIndex());
                            pipeTarget = ((ChainComponentModel) elt).getInput(((Pipe) flow).getDestinationInputIndex());
                            if (!tmpChainModel.addPipe((EndpointModel) pipeSrc, (EndpointModel) pipeTarget)) toDelete.add(flow);
                        } else {
                            pipeSrc = (ChainComponentModel) toCheck.get(comp);
                            pipeTarget = (ChainComponentModel) elt;
                            if (!tmpChainModel.addPrecedence((ChainComponentModel) pipeSrc, (ChainComponentModel) pipeTarget)) toDelete.add(flow);
                        }
                    } else {
                        toDelete.add(flow);
                    }
                }
            }
        }
        for (FlowManager f : toDelete) {
            if (f.getSource() != null) f.getSource().unbind(f);
            if (f.getDestination() != null) f.getDestination().unbind(f);
        }
        generateRecDependencies(tmpChainModel, justAdded, alreadyAdded, services, defaultServiceLayout);
    }

    private void verifierDependances(final Chain oldChain, final Chain newChain) {
        final List<PObjectReference> choix = new LinkedList<PObjectReference>();
        final List<Button> boutonACocher = new LinkedList<Button>();
        try {
            final List<PObjectReference> chaineAMAJ = chainRepository.getObjectRegisteredConsumers(oldChain.getId(), oldChain.getVersion(), false);
            if (chaineAMAJ.size() > 0) {
                final Shell dialog = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
                RowLayout rowDialog = new RowLayout();
                rowDialog.type = SWT.VERTICAL;
                rowDialog.fill = false;
                rowDialog.spacing = 10;
                Color gris = new Color(null, 240, 240, 240);
                dialog.setLayout(rowDialog);
                dialog.setText("Choix des chaines à impacter");
                dialog.setBackground(gris);
                Label message = new Label(dialog, SWT.NULL);
                message.setBackground(gris);
                message.setText("Le chaîne \"" + oldChain.getLabel() + " " + oldChain.getVersion() + "\" est utilisée dans les chaines suivantes :");
                Composite chaines = new Composite(dialog, SWT.NULL);
                chaines.setBackground(gris);
                chaines.setLayout(new GridLayout(2, false));
                for (PObjectReference ref : chaineAMAJ) {
                    Chain c = ref.resolve(chainRepository, false, false);
                    if (c != null) {
                        Label ch = new Label(chaines, SWT.NULL);
                        ch.setBackground(gris);
                        ch.setText(c.getLabel() + " " + c.getVersion() + "\n");
                        Button cb = new Button(chaines, SWT.CHECK);
                        cb.setBackground(gris);
                        boutonACocher.add(cb);
                    }
                }
                Composite expli = new Composite(dialog, SWT.NULL);
                expli.setBackground(gris);
                FillLayout fillExpli = new FillLayout();
                expli.setLayout(fillExpli);
                Label explication = new Label(expli, SWT.NULL);
                explication.setBackground(gris);
                explication.setText("Veuillez choisir les chaines à mettre à jour vers \"" + newChain.getLabel() + " " + newChain.getVersion() + "\".");
                Composite barreBouton = new Composite(dialog, SWT.NULL);
                barreBouton.setBackground(gris);
                FillLayout fillBouton = new FillLayout();
                fillBouton.marginWidth = 140;
                barreBouton.setLayout(fillBouton);
                Button okChaine = new Button(barreBouton, SWT.PUSH);
                okChaine.setBackground(gris);
                okChaine.setText("Valider");
                okChaine.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent e) {
                        for (int i = 0; i < boutonACocher.size(); i++) {
                            if (boutonACocher.get(i).getSelection()) {
                                choix.add(chaineAMAJ.get(i));
                            }
                        }
                        for (PObjectReference refChaine : choix) {
                            Chain c = refChaine.resolve(chainRepository, false, false);
                            List<ChainComponent> composants = c.getChainComponents();
                            for (ChainComponent comp : composants) {
                                if (comp instanceof ChainCall) {
                                    if (((ChainCall) comp).getChainReference().getId().equals(oldChain.getId()) && ((ChainCall) comp).getChainReference().getVersion().equals(oldChain.getVersion())) {
                                        ((ChainCall) comp).getChainReference().setId(newChain.getId());
                                        ((ChainCall) comp).getChainReference().setVersion(newChain.getVersion());
                                    }
                                }
                            }
                            chainRepository.putObject(c, true, false);
                        }
                        actualiseEditeursChaines(newChain);
                        System.out.println(choix.size() + " chaines ont été mise à jour pour le nouveau service");
                        dialog.close();
                    }
                });
                dialog.setDefaultButton(okChaine);
                dialog.pack();
                dialog.open();
            }
        } catch (PObjectNotFoundException e) {
            System.out.println("Aucune chaine n'utilise ce service");
        }
    }

    public void actualiseEditeursChaines(Chain oldChain) {
        try {
            List<PObjectReference> chaineAMAJ = chainRepository.getObjectRegisteredConsumers(oldChain.getId(), oldChain.getVersion(), false);
            if (chaineAMAJ.size() > 0) {
                IWorkbenchPage page = getEditorSite().getPage();
                IEditorReference[] tabEditorsRefs = page.getEditorReferences();
                for (IEditorReference refEditor : tabEditorsRefs) {
                    IEditorPart editor = refEditor.getEditor(false);
                    if (editor instanceof ChainEditor) {
                        Chain c = ((ChainEditor) editor).getChain();
                        for (PObjectReference ref : chaineAMAJ) {
                            Chain dep = ref.resolve(chainRepository, false, false);
                            if (dep.getId().equals(c.getId()) && dep.getVersion().equals(c.getVersion())) {
                                ((ChainEditor) editor).initializeGraphicalViewer();
                            }
                        }
                    }
                }
            }
        } catch (PObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDirty() {
        if (chainModel == null) return false;
        return chainModel.isDirty();
    }

    public boolean testServiceSeul() {
        chaineModifie = chainModel.getContent();
        int i = 0;
        ServiceModel serviceModel = new ServiceModel(null);
        Iterator<Element> itm = chainModel.getChildren().iterator();
        while (itm.hasNext()) {
            if (itm.next().getClass().equals(serviceModel.getClass())) {
                i++;
            }
        }
        int j = chaineModifie.getChainComponents().size();
        if (j != i) return true; else return false;
    }

    public void doSave(IProgressMonitor monitor) {
        Chain oldChain = new Chain(chaineModifie.getId(), chaineModifie.getLabel(), chaineModifie.getVersion());
        boolean existait = false;
        if (getKeepVersion()) {
            chainRepository.putObject(chaineModifie, true, false);
        } else {
            if (chainModel.getMajor()) chaineModifie.setVersion(new Version(chaineModifie.getVersion().getMajor() + 1, 0)); else if (chainModel.getMinor()) chaineModifie.setVersion(new Version(chaineModifie.getVersion().getMajor(), chaineModifie.getVersion().getMinor() + 1));
            if (chainModel.getMajor() || chainModel.getMinor()) existait = chainRepository.putObject(chaineModifie, false, false); else chainRepository.putObject(chaineModifie, true, false);
        }
        if (existait && !getKeepVersion()) {
            System.out.println("PCREATOR - Version déjà présente");
            MessageBox box = new MessageBox(this.getSite().getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
            box.setMessage("Etes vous sûr de vouloir écraser \"" + chaineModifie.getLabel() + " " + chaineModifie.getVersion() + "\" ?\nSi vous n'écrasez pas, une chaîne dérivée sera créée en version 1.0.");
            box.setText("Confirmation de remplacement de chaîne");
            int res = box.open();
            if (res == SWT.NO) {
                UUID nouvelId = UUID.randomUUID();
                chaineModifie.setId(nouvelId);
                chaineModifie.setVersion(new Version(1, 0));
                chainRepository.putObject(chaineModifie, true, false);
                System.out.println("PCREATOR - Sauvegarde en version dérivée");
                System.out.println("PCREATOR - Sauvegarde de la chaîne \"" + chaineModifie.getLabel() + " " + chaineModifie.getVersion() + "\"");
            } else {
                chainRepository.putObject(chaineModifie, true, false);
                System.out.println("PCREATOR - Sauvegarde en version existante");
                System.out.println("PCREATOR - Sauvegarde de la chaîne \"" + chaineModifie.getLabel() + " " + chaineModifie.getVersion() + "\"");
            }
        } else {
            System.out.println("PCREATOR - Sauvegarde en nouvelle version");
            System.out.println("PCREATOR - Sauvegarde de la chaîne \"" + chaineModifie.getLabel() + " " + chaineModifie.getVersion() + "\"");
        }
        try {
            chainRepository.putObjectAttachment(chaineModifie.getId(), chaineModifie.getVersion(), false, chainAttachmentID, chainModel.getAttachment(), true);
        } catch (PObjectNotFoundException e) {
            System.out.println("Should Not Happen (Impossible de trouver une chaine qui vient tout juste d'être ajoutée avec succès)");
        }
        if (chainModel.getMajor() || chainModel.getMinor()) verifierDependances(oldChain, chaineModifie);
        actualiseEditeursChaines(chaineModifie);
        ((ChainNavigator) getSite().getWorkbenchWindow().getActivePage().findView(ChainNavigator.ID)).getTreeViewer().refresh();
        chainModel.resetDirty();
    }

    public void setFocus() {
        super.setFocus();
        IContextService contextService = (IContextService) getSite().getService(IContextService.class);
        contextService.activateContext("fr.insa.rennes.pelias.pcreator.chainEditorContext");
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new PCreatorEditPartFactory());
    }

    protected PaletteRoot getPaletteRoot() {
        PaletteRoot root = new PaletteRoot();
        PaletteGroup group = new PaletteGroup("Sélection d'éléments");
        SelectionToolEntry sel = new SelectionToolEntry("Sélection simple", "Sélection d'un élément graphique");
        group.add(sel);
        MarqueeToolEntry mar = new MarqueeToolEntry("Sélection multiple", "Sélection multiple d'éléments graphiques");
        mar.setToolProperty(MarqueeSelectionTool.PROPERTY_UNLOAD_WHEN_FINISHED, true);
        group.add(mar);
        root.add(group);
        root.add(new PaletteSeparator());
        PaletteGroup creationGroup = new PaletteGroup("Création d'éléments");
        CreationToolEntry com = new CreationToolEntry("Commentaire", "Creation d'un commentaire", new CreationFactory() {

            public Object getNewObject() {
                return new CommentModel("");
            }

            public Object getObjectType() {
                return CommentModel.class;
            }
        }, null, null);
        com.setToolProperty(CreationTool.PROPERTY_UNLOAD_WHEN_FINISHED, false);
        creationGroup.add(com);
        creationGroup.add(new ConnectionCreationToolEntry("Tube", "Création d'un tube", new CreationFactory() {

            public Object getNewObject() {
                return null;
            }

            public Object getObjectType() {
                return PipeModel.class;
            }
        }, null, null));
        creationGroup.add(new ConnectionCreationToolEntry("Précédence", "Création d'une précédence entre deux services", new CreationFactory() {

            public Object getNewObject() {
                return null;
            }

            public Object getObjectType() {
                return PrecedenceModel.class;
            }
        }, null, null));
        CreationToolEntry input = new CreationToolEntry("Entrée de Chaine", "Création d'une entrée de chaine à partir d'une entrée de service", new CreationFactory() {

            public Object getNewObject() {
                return null;
            }

            public Object getObjectType() {
                return ChainInputModel.class;
            }
        }, null, null);
        input.setToolProperty(CreationTool.PROPERTY_UNLOAD_WHEN_FINISHED, false);
        creationGroup.add(input);
        CreationToolEntry output = new CreationToolEntry("Sortie de Chaine", "Création d'une sortie de chaine à partir d'une sortie de service", new CreationFactory() {

            public Object getNewObject() {
                return null;
            }

            public Object getObjectType() {
                return ChainOutputModel.class;
            }
        }, null, null);
        output.setToolProperty(CreationTool.PROPERTY_UNLOAD_WHEN_FINISHED, false);
        creationGroup.add(output);
        root.add(creationGroup);
        root.setDefaultEntry(sel);
        return root;
    }

    public Chain getChain() {
        return chain;
    }

    public ISxSRepository<Chain> getChainRepository() {
        return chainRepository;
    }

    public ChainModel getChainModel() {
        return chainModel;
    }

    public Chain getChaineModifie() {
        return chaineModifie;
    }

    public void dirtyChanged() {
        firePropertyChange(EditorPart.PROP_DIRTY);
    }

    public void refreshName() {
        setPartName(chainModel.getContent().getLabel() + " " + chainModel.getContent().getVersion());
    }

    public void setKeepVersion(boolean b) {
        keepVersion = b;
        getEditorSite().getSelectionProvider().setSelection(getEditorSite().getSelectionProvider().getSelection());
    }

    public boolean getKeepVersion() {
        return keepVersion;
    }
}
