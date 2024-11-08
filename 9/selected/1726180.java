package fr.insa.rennes.pelias.pcreator.editors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import fr.insa.rennes.pelias.framework.Chain;
import fr.insa.rennes.pelias.framework.ChainComponent;
import fr.insa.rennes.pelias.framework.InputType;
import fr.insa.rennes.pelias.framework.Service;
import fr.insa.rennes.pelias.framework.ServiceCall;
import fr.insa.rennes.pelias.framework.ServiceInput;
import fr.insa.rennes.pelias.framework.ServiceOutput;
import fr.insa.rennes.pelias.pcreator.Application;
import fr.insa.rennes.pelias.pcreator.WorkbenchPreferencePage;
import fr.insa.rennes.pelias.pcreator.views.ServiceNavigator;
import fr.insa.rennes.pelias.pcreator.wizards.NewServiceWizard;
import fr.insa.rennes.pelias.platform.IRepository;
import fr.insa.rennes.pelias.platform.ISxSRepository;
import fr.insa.rennes.pelias.platform.PObjectNotFoundException;
import fr.insa.rennes.pelias.platform.PObjectReference;
import fr.insa.rennes.pelias.platform.PSxSObjectReference;
import fr.insa.rennes.pelias.platform.Version;

/**
 * 
 * @author Kévin Le Corre
 *
 */
public class ServiceEditor extends EditorPart {

    public static String ID = "fr.insa.rennes.pelias.editor.serviceeditor";

    private FormToolkit toolkit;

    private ScrolledForm form;

    private Service service;

    private ISxSRepository<Service> repository;

    private ISxSRepository<Chain> chainRepository;

    private boolean minor;

    private boolean major;

    private boolean modification;

    private Button keepVersion;

    private Text nomService;

    private static int id_item;

    private static int selection_erreur;

    public ServiceEditor() {
        minor = false;
        major = false;
        modification = false;
        repository = Application.getCurrentServiceRepository();
        chainRepository = Application.getCurrentChainRepository();
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        PObjectReference reference = ((ServiceEditorInput) input).getInput();
        if (reference instanceof PSxSObjectReference) {
            service = ((PSxSObjectReference) reference).resolve(repository, false, false, false);
        } else {
            service = reference.resolve((IRepository<Service>) repository, false, false);
        }
        setPartName(service.getLabel() + " " + service.getVersion());
    }

    public void doSave(IProgressMonitor monitor) {
        if (!service.getPlatform().equals("") && !Application.getListePlatform().contains(service.getPlatform())) {
            Application.getListePlatform().add(service.getPlatform());
        }
        for (int i = 0; i < service.getInputs().size(); i++) {
            String platform = service.getInputs().get(i).getMIMEType();
            if (!platform.equals("") && !Application.getListeMIME().contains(platform)) {
                Application.getListeMIME().add(platform);
            }
        }
        for (int i = 0; i < service.getOutputs().size(); i++) {
            String platform = service.getOutputs().get(i).getMIMEType();
            if (!platform.equals("") && !Application.getListeMIME().contains(platform)) {
                Application.getListeMIME().add(platform);
            }
        }
        Application.savePreferences();
        if (modification) {
            setPartName(service.getLabel() + " " + service.getVersion());
            IWorkbenchPage page = getEditorSite().getPage();
            IEditorReference[] tabEditorsRefs = page.getEditorReferences();
            for (IEditorReference refEditor : tabEditorsRefs) {
                IEditorPart editor = refEditor.getEditor(false);
                if (editor instanceof ServiceEditor && editor != this) {
                    Service s = ((ServiceEditor) editor).getService();
                    if (s.getId().equals(service.getId())) {
                        ((ServiceEditor) editor).renommage(service.getLabel());
                    }
                }
            }
        }
        if (!keepVersion.getSelection() && (major || minor)) {
            Service oldService = new Service(service.getId(), service.getLabel(), service.getVersion());
            Service newService = doSaveWithNewVersion();
            verifierDependances(oldService, newService);
            actualiseEditeursChaines(newService);
            minor = false;
            major = false;
            modification = false;
            firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        } else {
            if ((modification && !major && !minor) || (keepVersion.getSelection() && (major || minor))) {
                repository.putObject(service, true, false);
                System.out.println("PCREATOR - Sauvegarde du service \"" + service.getLabel() + " " + service.getVersion() + "\"");
                actualiseEditeursChaines(service);
                minor = false;
                major = false;
                modification = false;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
            }
        }
        ((ServiceNavigator) getSite().getWorkbenchWindow().getActivePage().findView(ServiceNavigator.ID)).getTreeViewer().refresh();
    }

    private Service doSaveWithNewVersion() {
        if (major) {
            Version majoree = new Version(service.getVersion().getMajor() + 1, 0);
            service.setVersion(majoree);
        } else {
            if (minor) {
                Version minoree = new Version(service.getVersion().getMajor(), service.getVersion().getMinor() + 1);
                service.setVersion(minoree);
            }
        }
        boolean dejaPresent;
        dejaPresent = repository.putObject(service, false, false);
        if (dejaPresent) {
            System.out.println("PCREATOR - Version déjà présente");
            MessageBox box = new MessageBox(this.getSite().getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
            box.setMessage("Etes vous sûr de vouloir écraser \"" + service.getLabel() + " " + service.getVersion() + "\" ?\nSi vous n'écrasez pas, un service dérivé sera créé en version 1.0.");
            box.setText("Confirmation de remplacement de service");
            int res = box.open();
            if (res == SWT.NO) {
                UUID nouvelId = UUID.randomUUID();
                service.setId(nouvelId);
                service.setVersion(new Version(1, 0));
                repository.putObject(service, true, false);
                System.out.println("PCREATOR - Sauvegarde en version dérivée");
                System.out.println("PCREATOR - Sauvegarde du service \"" + service.getLabel() + " " + service.getVersion() + "\"");
            } else {
                repository.putObject(service, true, false);
                System.out.println("PCREATOR - Sauvegarde en version existante");
                System.out.println("PCREATOR - Sauvegarde du service \"" + service.getLabel() + " " + service.getVersion() + "\"");
            }
        } else {
            System.out.println("PCREATOR - Sauvegarde en nouvelle version");
            System.out.println("PCREATOR - Sauvegarde du service \"" + service.getLabel() + " " + service.getVersion() + "\"");
        }
        return service;
    }

    public void doSaveAs() {
    }

    public boolean isDirty() {
        return modification || minor || major;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    private void createSectionIdentification(final ScrolledForm form) {
        Section identification = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.EXPANDED | Section.TITLE_BAR);
        identification.addExpansionListener(new ExpansionAdapter() {

            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(true);
            }
        });
        identification.setText("Identification");
        Composite contentIdent = toolkit.createComposite(identification);
        contentIdent.setLayout(new GridLayout(2, false));
        toolkit.createLabel(contentIdent, "Nom du Service");
        nomService = toolkit.createText(contentIdent, service.getLabel());
        nomService.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nomService.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                modification = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                service.setLabel(((Text) e.widget).getText());
            }
        });
        toolkit.createLabel(contentIdent, "Description du service");
        Text descriptionService = toolkit.createText(contentIdent, service.getDescription());
        descriptionService.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        descriptionService.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                minor = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                service.setDescription(((Text) e.widget).getText());
            }
        });
        toolkit.createLabel(contentIdent, "Version du service");
        Text versionService = toolkit.createText(contentIdent, service.getVersion().toString());
        versionService.setEditable(false);
        versionService.setEnabled(false);
        toolkit.createLabel(contentIdent, "Type de service (Template)");
        Text typeService = toolkit.createText(contentIdent, service.getType());
        typeService.setEditable(false);
        typeService.setEnabled(false);
        toolkit.createLabel(contentIdent, "Genre de service");
        Text kindService = toolkit.createText(contentIdent, service.getKind());
        kindService.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        kindService.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                minor = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                service.setKind(((Text) e.widget).getText());
            }
        });
        identification.setClient(contentIdent);
    }

    private void createSectionExecution(final ScrolledForm form) {
        Section execution = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.EXPANDED | Section.TITLE_BAR);
        execution.addExpansionListener(new ExpansionAdapter() {

            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(true);
            }
        });
        execution.setText("Exécution");
        Composite contentExec = toolkit.createComposite(execution);
        contentExec.setLayout(new GridLayout(2, false));
        toolkit.createLabel(contentExec, "Emplacement");
        final Text textEmplacement = toolkit.createText(contentExec, service.getLocation());
        textEmplacement.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        textEmplacement.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                minor = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                service.setLocation(((Text) e.widget).getText());
            }
        });
        toolkit.createLabel(contentExec, "");
        Button boutonParcourir = new Button(contentExec, SWT.PUSH);
        boutonParcourir.setText("Parcourir...");
        boutonParcourir.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                FileDialog browse = new FileDialog(getSite().getShell());
                IEclipsePreferences preferences = new ConfigurationScope().getNode(Application.PLUGIN_ID);
                String workspace = preferences.get(WorkbenchPreferencePage.WORKSPACE, "");
                browse.setFilterPath(workspace);
                String selectedDirectory = browse.open();
                if (selectedDirectory != null) {
                    minor = true;
                    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                    if (!workspace.equals("")) {
                        String path;
                        if (!workspace.endsWith("\\") && !workspace.endsWith("/")) {
                            if (selectedDirectory.contains("/")) {
                                path = selectedDirectory.replace(workspace + "/", "");
                            } else {
                                path = selectedDirectory.replace(workspace + "\\", "");
                            }
                        } else {
                            path = selectedDirectory.replace(workspace, "");
                        }
                        textEmplacement.setText(path);
                        service.setLocation(path);
                    } else {
                        textEmplacement.setText(selectedDirectory);
                        service.setLocation(selectedDirectory);
                    }
                }
            }
        });
        toolkit.createLabel(contentExec, "Plateforme d'éxécution");
        Combo combo = new Combo(contentExec, SWT.DROP_DOWN);
        for (int i = 0; i < Application.getListePlatform().size(); i++) {
            combo.add(Application.getListePlatform().get(i));
        }
        combo.setText(service.getPlatform());
        combo.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                minor = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                service.setPlatform(((Combo) e.widget).getText());
            }
        });
        combo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                minor = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                service.setPlatform(((Combo) e.widget).getText());
            }
        });
        execution.setClient(contentExec);
    }

    private void createSectionErreur(final ScrolledForm form) {
        Section erreurs = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.EXPANDED | Section.TITLE_BAR);
        erreurs.addExpansionListener(new ExpansionAdapter() {

            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(true);
            }
        });
        erreurs.setText("Erreurs");
        Composite contentErreur = toolkit.createComposite(erreurs);
        contentErreur.setLayout(new GridLayout(2, false));
        toolkit.createLabel(contentErreur, "Table des erreurs");
        final Table tableErreur = new Table(contentErreur, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        final TableColumn colCode = new TableColumn(tableErreur, SWT.LEFT);
        colCode.setText("Code erreur");
        colCode.setWidth(100);
        final TableColumn colDescErreur = new TableColumn(tableErreur, SWT.LEFT);
        colDescErreur.setText("Description");
        colDescErreur.setWidth(400);
        tableErreur.setHeaderVisible(true);
        tableErreur.setLinesVisible(true);
        if (!(service.getErrors().isEmpty())) {
            Set<Entry<Integer, String>> donneesErreur = service.getErrors().entrySet();
            for (Entry<Integer, String> e : donneesErreur) {
                TableItem item = new TableItem(tableErreur, 0, tableErreur.getItemCount());
                item.setText(new String[] { e.getKey().toString(), e.getValue() });
            }
        }
        tableErreur.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                form.setFocus();
            }

            public void focusLost(FocusEvent e) {
            }
        });
        toolkit.createLabel(contentErreur, "");
        Composite listeBoutonErreur = toolkit.createComposite(contentErreur);
        listeBoutonErreur.setLayout(new FillLayout());
        Button boutonAjouterErreur = new Button(listeBoutonErreur, SWT.PUSH);
        boutonAjouterErreur.setText("Ajouter...");
        boutonAjouterErreur.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                final Shell dialog = new Shell(form.getDisplay(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
                dialog.setLayout(new GridLayout(2, false));
                dialog.setText("Ajout d'une Erreur");
                Color gris = new Color(null, 240, 240, 240);
                dialog.setBackground(gris);
                final Pattern patternHexa = Pattern.compile("^0x([0-9A-F]+)$");
                final Pattern patternDeci = Pattern.compile("^[0-9]+$");
                final Pattern patternOcta = Pattern.compile("^x([0-7]+)$");
                final FormToolkit toolkitErreur = new FormToolkit(dialog.getDisplay());
                Label labelCode = toolkitErreur.createLabel(dialog, "Code d'erreur");
                labelCode.setBackground(gris);
                final Text textCodeErreur = toolkitErreur.createText(dialog, "");
                textCodeErreur.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                Label labelDesc = toolkitErreur.createLabel(dialog, "Description");
                labelDesc.setBackground(gris);
                final Text textDescErreur = toolkitErreur.createText(dialog, "");
                textDescErreur.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                toolkit.createLabel(dialog, "");
                Label vide = toolkit.createLabel(dialog, "\t\t\t\t");
                vide.setBackground(gris);
                toolkit.createLabel(dialog, "");
                Composite barreBouton = toolkit.createComposite(dialog);
                barreBouton.setBackground(gris);
                FillLayout fillErreur = new FillLayout();
                barreBouton.setLayout(fillErreur);
                final Button okErreur = toolkit.createButton(barreBouton, "OK", SWT.PUSH);
                okErreur.setBackground(gris);
                okErreur.setEnabled(false);
                okErreur.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent e) {
                        int codeResultat = -1;
                        String code = textCodeErreur.getText();
                        Matcher matcherHexa = patternHexa.matcher(code);
                        Matcher matcherDeci = patternDeci.matcher(code);
                        Matcher matcherOcta = patternOcta.matcher(code);
                        if (matcherDeci.find()) {
                            codeResultat = Integer.parseInt(code);
                        }
                        if (matcherHexa.find()) {
                            codeResultat = Integer.parseInt(matcherHexa.group(1), 16);
                        }
                        if (matcherOcta.find()) {
                            codeResultat = Integer.parseInt(matcherOcta.group(1), 8);
                        }
                        if (service.getErrors().containsKey(codeResultat)) {
                            MessageBox erreurSaisie = new MessageBox(form.getShell(), SWT.YES | SWT.NO | SWT.ICON_QUESTION);
                            erreurSaisie.setText("Code entré déjà existant");
                            erreurSaisie.setMessage("Le code que vous avez tappé est déjà présent dans la liste des codes d'erreurs, voulez-vous remplacer le code existant ?");
                            int resultat = erreurSaisie.open();
                            if (resultat == SWT.YES) {
                                minor = true;
                                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                                service.getErrors().put(codeResultat, textDescErreur.getText());
                                dialog.close();
                                synchroniserErreur(form, tableErreur);
                            }
                            if (resultat == SWT.NO) {
                                dialog.close();
                            }
                        } else {
                            minor = true;
                            firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                            TableItem item = new TableItem(tableErreur, 0, tableErreur.getItemCount());
                            item.setText(new String[] { "" + codeResultat, textDescErreur.getText() });
                            service.getErrors().put(codeResultat, textDescErreur.getText());
                            dialog.close();
                            form.reflow(true);
                        }
                    }
                });
                textCodeErreur.addModifyListener(new ModifyListener() {

                    public void modifyText(ModifyEvent e) {
                        String code = ((Text) e.widget).getText();
                        Matcher matcherHexa = patternHexa.matcher(code);
                        Matcher matcherDeci = patternDeci.matcher(code);
                        Matcher matcherOcta = patternOcta.matcher(code);
                        if (matcherHexa.find() || matcherDeci.find() || matcherOcta.find()) {
                            okErreur.setEnabled(true);
                        } else {
                            okErreur.setEnabled(false);
                        }
                    }
                });
                Button annulerErreur = toolkit.createButton(barreBouton, "Annuler", SWT.PUSH);
                annulerErreur.setBackground(gris);
                annulerErreur.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent e) {
                        dialog.close();
                    }
                });
                dialog.setDefaultButton(okErreur);
                dialog.pack();
                dialog.open();
                form.reflow(true);
            }
        });
        final Button boutonModifier = new Button(listeBoutonErreur, SWT.PUSH);
        boutonModifier.setText("Modifier");
        boutonModifier.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int selection_erreur = tableErreur.getSelectionIndex();
                if (selection_erreur >= 0) {
                    minor = true;
                    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                    TableItem item = tableErreur.getItem(selection_erreur);
                    final Shell dialog = new Shell(form.getDisplay(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
                    dialog.setLayout(new GridLayout(2, false));
                    dialog.setText("Modifier une Erreur");
                    Color gris = new Color(null, 240, 240, 240);
                    dialog.setBackground(gris);
                    final Pattern patternHexa = Pattern.compile("^0x([0-9A-F]+)$");
                    final Pattern patternDeci = Pattern.compile("^[0-9]+$");
                    final Pattern patternOcta = Pattern.compile("^x([0-7]+)$");
                    final FormToolkit toolkitErreur = new FormToolkit(dialog.getDisplay());
                    Label labelCode = toolkitErreur.createLabel(dialog, "Code d'erreur");
                    labelCode.setBackground(gris);
                    final Text textCodeErreur = toolkitErreur.createText(dialog, item.getText(0));
                    textCodeErreur.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    Label labelDesc = toolkitErreur.createLabel(dialog, "Description");
                    labelDesc.setBackground(gris);
                    final Text textDescErreur = toolkitErreur.createText(dialog, item.getText(1));
                    textDescErreur.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    toolkit.createLabel(dialog, "");
                    Label vide = toolkit.createLabel(dialog, "\t\t\t\t");
                    vide.setBackground(gris);
                    String code = item.getText(0);
                    id_item = Integer.parseInt(code);
                    toolkit.createLabel(dialog, "");
                    Composite barreBouton = toolkit.createComposite(dialog);
                    barreBouton.setBackground(gris);
                    FillLayout fillErreur = new FillLayout();
                    barreBouton.setLayout(fillErreur);
                    final Button okErreur = toolkit.createButton(barreBouton, "OK", SWT.PUSH);
                    okErreur.setBackground(gris);
                    okErreur.setEnabled(true);
                    okErreur.addSelectionListener(new SelectionAdapter() {

                        public void widgetSelected(SelectionEvent e) {
                            service.getErrors().remove(ServiceEditor.id_item);
                            tableErreur.remove(ServiceEditor.selection_erreur);
                            int codeResultat = -1;
                            String code = textCodeErreur.getText();
                            Matcher matcherHexa = patternHexa.matcher(code);
                            Matcher matcherDeci = patternDeci.matcher(code);
                            Matcher matcherOcta = patternOcta.matcher(code);
                            if (matcherDeci.find()) {
                                codeResultat = Integer.parseInt(code);
                            }
                            if (matcherHexa.find()) {
                                codeResultat = Integer.parseInt(matcherHexa.group(1), 16);
                            }
                            if (matcherOcta.find()) {
                                codeResultat = Integer.parseInt(matcherOcta.group(1), 8);
                            }
                            if (service.getErrors().containsKey(codeResultat)) {
                                MessageBox erreurSaisie = new MessageBox(form.getShell(), SWT.YES | SWT.NO | SWT.ICON_QUESTION);
                                erreurSaisie.setText("Code entré déjà existant");
                                erreurSaisie.setMessage("Le code que vous avez tappé est déjà présent dans la liste des codes d'erreurs, voulez-vous remplacer le code existant ?");
                                int resultat = erreurSaisie.open();
                                if (resultat == SWT.YES) {
                                    minor = true;
                                    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                                    service.getErrors().put(codeResultat, textDescErreur.getText());
                                    dialog.close();
                                    synchroniserErreur(form, tableErreur);
                                }
                                if (resultat == SWT.NO) {
                                    dialog.close();
                                }
                            } else {
                                minor = true;
                                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                                TableItem item = new TableItem(tableErreur, 0, tableErreur.getItemCount());
                                item.setText(new String[] { "" + codeResultat, textDescErreur.getText() });
                                service.getErrors().put(codeResultat, textDescErreur.getText());
                                dialog.close();
                                form.reflow(true);
                            }
                        }
                    });
                    textCodeErreur.addModifyListener(new ModifyListener() {

                        public void modifyText(ModifyEvent e) {
                            String code = ((Text) e.widget).getText();
                            Matcher matcherHexa = patternHexa.matcher(code);
                            Matcher matcherDeci = patternDeci.matcher(code);
                            Matcher matcherOcta = patternOcta.matcher(code);
                            if (matcherHexa.find() || matcherDeci.find() || matcherOcta.find()) {
                                okErreur.setEnabled(true);
                            } else {
                                okErreur.setEnabled(false);
                            }
                        }
                    });
                    Button annulerErreur = toolkit.createButton(barreBouton, "Annuler", SWT.PUSH);
                    annulerErreur.setBackground(gris);
                    annulerErreur.addSelectionListener(new SelectionAdapter() {

                        public void widgetSelected(SelectionEvent e) {
                            dialog.close();
                        }
                    });
                    dialog.setDefaultButton(okErreur);
                    dialog.pack();
                    dialog.open();
                }
                form.reflow(true);
            }
        });
        Button boutonSupprimerErreur = new Button(listeBoutonErreur, SWT.PUSH);
        boutonSupprimerErreur.setText("Supprimer");
        boutonSupprimerErreur.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int selection = tableErreur.getSelectionIndex();
                if (selection >= 0) {
                    minor = true;
                    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                    TableItem item = tableErreur.getItem(selection);
                    String code = item.getText(0);
                    int i = Integer.parseInt(code);
                    service.getErrors().remove(i);
                    tableErreur.remove(selection);
                }
                form.reflow(true);
            }
        });
        erreurs.setClient(contentErreur);
    }

    private void createSectionEntree(final ScrolledForm form) {
        Section entree = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.EXPANDED | Section.TITLE_BAR);
        entree.addExpansionListener(new ExpansionAdapter() {

            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(true);
            }
        });
        entree.setText("Entrées");
        Composite contentEntree = toolkit.createComposite(entree);
        contentEntree.setLayout(new GridLayout(2, false));
        toolkit.createLabel(contentEntree, "");
        toolkit.createLabel(contentEntree, "Les entrées de type lot ne sont jamais combinées entre elles. Elles sont séquencées dans leur ordre d'apparition en conservant leur positionnement.");
        toolkit.createLabel(contentEntree, "Table des entrées");
        final Table tableEntree = new Table(contentEntree, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        final TableColumn colConstEntree = new TableColumn(tableEntree, SWT.LEFT);
        colConstEntree.setText("Type");
        colConstEntree.setWidth(70);
        final TableColumn colNomEntree = new TableColumn(tableEntree, SWT.LEFT);
        colNomEntree.setText("Nom");
        colNomEntree.setWidth(100);
        final TableColumn colDescEntree = new TableColumn(tableEntree, SWT.LEFT);
        colDescEntree.setText("Description");
        colDescEntree.setWidth(250);
        final TableColumn colMimeEntree = new TableColumn(tableEntree, SWT.LEFT);
        colMimeEntree.setText("Type MIME");
        colMimeEntree.setWidth(100);
        final TableColumn colValeurEntree = new TableColumn(tableEntree, SWT.LEFT);
        colValeurEntree.setText("Valeur par défaut");
        colValeurEntree.setWidth(80);
        final TableColumn colOptionEntree = new TableColumn(tableEntree, SWT.LEFT);
        colOptionEntree.setText("Options");
        colOptionEntree.setWidth(60);
        tableEntree.setHeaderVisible(true);
        tableEntree.setLinesVisible(true);
        if (!(service.getInputs().isEmpty())) {
            List<ServiceInput> donneesEntree = (List<ServiceInput>) service.getInputs();
            for (ServiceInput in : donneesEntree) {
                TableItem item = new TableItem(tableEntree, 0, tableEntree.getItemCount());
                if (in.getType() == InputType.UserParameter) {
                    item.setText(new String[] { "Paramètre", in.getName(), in.getDescription(), "", in.getDefaultValue(), in.getOption() });
                } else {
                    item.setText(new String[] { "Lot", in.getName(), in.getDescription(), in.getMIMEType(), "", in.getOption() });
                }
            }
            tableEntree.pack(true);
        }
        tableEntree.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                form.setFocus();
            }

            public void focusLost(FocusEvent e) {
            }
        });
        toolkit.createLabel(contentEntree, "");
        Composite listeBoutonEntree = toolkit.createComposite(contentEntree);
        listeBoutonEntree.setLayout(new FillLayout());
        toolkit.createLabel(contentEntree, "   Entrée de type");
        toolkit.createLabel(contentEntree, "");
        toolkit.createLabel(contentEntree, "fichier par fichier");
        final Combo fbfInput = new Combo(contentEntree, SWT.READ_ONLY);
        synchroniserCombo(form, fbfInput);
        int index = service.getFileByFileInputIndex();
        if (index != -1) {
            fbfInput.setText(service.getInputs().get(index).getName());
        } else {
            fbfInput.setText("");
        }
        fbfInput.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                minor = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                int indexChoisi = fbfInput.getSelectionIndex();
                List<ServiceInput> listeInput = service.getInputs();
                int i = 0;
                while (indexChoisi != 0) {
                    if (listeInput.get(i).getType() == InputType.Batch) {
                        indexChoisi--;
                    }
                    i++;
                }
                service.setFileByFileInputIndex(i - 1);
            }
        });
        Button boutonAjouterEntree = new Button(listeBoutonEntree, SWT.PUSH);
        boutonAjouterEntree.setText("Ajouter...");
        boutonAjouterEntree.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                afficherFenetreAjoutEntree(tableEntree, fbfInput);
                form.reflow(true);
            }
        });
        boutonAjouterEntree.setEnabled(true);
        final Button boutonModifierEntree = new Button(listeBoutonEntree, SWT.PUSH);
        boutonModifierEntree.setText("Modifier...");
        boutonModifierEntree.setEnabled(false);
        boutonModifierEntree.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                afficherFenetreModificationEntree(tableEntree, fbfInput);
                form.reflow(true);
            }
        });
        tableEntree.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                if (tableEntree.getSelectionIndex() >= 0) {
                    boutonModifierEntree.setEnabled(true);
                } else {
                    boutonModifierEntree.setEnabled(false);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                afficherFenetreModificationEntree(tableEntree, fbfInput);
                form.reflow(true);
            }
        });
        Button boutonSupprimerEntree = new Button(listeBoutonEntree, SWT.PUSH);
        boutonSupprimerEntree.setText("Supprimer");
        boutonSupprimerEntree.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int selection = tableEntree.getSelectionIndex();
                if (selection >= 0) {
                    major = true;
                    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                    List<ServiceInput> listInput = (List<ServiceInput>) service.getInputs();
                    listInput.remove(selection);
                    synchroniserCombo(form, fbfInput);
                    service.setFileByFileInputIndex(-1);
                    tableEntree.remove(selection);
                }
                tableEntree.pack();
                form.reflow(true);
            }
        });
        entree.setClient(contentEntree);
    }

    private void createSectionSortie(final ScrolledForm form) {
        Section sortie = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.EXPANDED | Section.TITLE_BAR);
        sortie.addExpansionListener(new ExpansionAdapter() {

            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(true);
            }
        });
        sortie.setText("Sorties");
        Composite contentSortie = toolkit.createComposite(sortie);
        contentSortie.setLayout(new GridLayout(2, false));
        toolkit.createLabel(contentSortie, "Table des sorties");
        final Table tableSortie = new Table(contentSortie, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        final TableColumn colNomSortie = new TableColumn(tableSortie, SWT.LEFT);
        colNomSortie.setText("Nom");
        colNomSortie.setWidth(100);
        final TableColumn colDescSortie = new TableColumn(tableSortie, SWT.LEFT);
        colDescSortie.setText("Description");
        colDescSortie.setWidth(300);
        final TableColumn colMimeSortie = new TableColumn(tableSortie, SWT.LEFT);
        colMimeSortie.setText("Type MIME");
        colMimeSortie.setWidth(100);
        tableSortie.setHeaderVisible(true);
        tableSortie.setLinesVisible(true);
        if (!(service.getOutputs().isEmpty())) {
            List<ServiceOutput> donneesSortie = (List<ServiceOutput>) service.getOutputs();
            for (ServiceOutput out : donneesSortie) {
                TableItem item = new TableItem(tableSortie, 0, tableSortie.getItemCount());
                item.setText(new String[] { out.getName(), out.getDescription(), out.getMIMEType() });
            }
            tableSortie.pack();
        }
        toolkit.createLabel(contentSortie, "");
        Composite listeBoutonSortie = toolkit.createComposite(contentSortie);
        listeBoutonSortie.setLayout(new FillLayout());
        Button boutonAjouterSortie = new Button(listeBoutonSortie, SWT.PUSH);
        boutonAjouterSortie.setText("Ajouter...");
        boutonAjouterSortie.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                afficherFenetreAjoutSortie(tableSortie);
                form.reflow(true);
            }
        });
        boutonAjouterSortie.setEnabled(true);
        final Button boutonModifierSortie = new Button(listeBoutonSortie, SWT.PUSH);
        boutonModifierSortie.setText("Modifier...");
        boutonModifierSortie.setEnabled(false);
        boutonModifierSortie.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                afficherFenetreModificationSortie(tableSortie);
                form.reflow(true);
            }
        });
        tableSortie.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                if (tableSortie.getSelectionIndex() >= 0) {
                    boutonModifierSortie.setEnabled(true);
                } else {
                    boutonModifierSortie.setEnabled(false);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                afficherFenetreModificationSortie(tableSortie);
                form.reflow(true);
            }
        });
        tableSortie.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                form.setFocus();
            }

            public void focusLost(FocusEvent e) {
            }
        });
        Button boutonSupprimerSortie = new Button(listeBoutonSortie, SWT.PUSH);
        boutonSupprimerSortie.setText("Supprimer");
        boutonSupprimerSortie.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int selection = tableSortie.getSelectionIndex();
                if (selection >= 0) {
                    major = true;
                    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                    List<ServiceOutput> listOutput = (List<ServiceOutput>) service.getOutputs();
                    listOutput.remove(selection);
                    tableSortie.remove(selection);
                    tableSortie.pack();
                    form.reflow(true);
                }
            }
        });
        sortie.setClient(contentSortie);
        form.reflow(true);
    }

    private void createSectionEnregister(final ScrolledForm form) {
        toolkit.createSection(form.getBody(), Section.TITLE_BAR);
        Composite contentEnregistrer = toolkit.createComposite(form.getBody());
        contentEnregistrer.setLayout(new GridLayout(2, false));
        toolkit.createLabel(contentEnregistrer, "\t\t\t\t");
        Composite boutonEnregister = toolkit.createComposite(contentEnregistrer);
        boutonEnregister.setLayout(new FillLayout());
        final Button Enreg = new Button(boutonEnregister, SWT.PUSH);
        Enreg.setSize(10, 10);
        Enreg.setText("Enregistrer");
        Enreg.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
                IEditorPart editeurActif = window.getActivePage().getActiveEditor();
                if (editeurActif instanceof ServiceEditor) {
                    if (((ServiceEditor) editeurActif).isDirty()) {
                        ISxSRepository<Service> oldRepository = ((ServiceEditor) editeurActif).getRepository();
                        boolean major = ((ServiceEditor) editeurActif).isMajor();
                        boolean minor = ((ServiceEditor) editeurActif).isMinor();
                        editeurActif.doSave(null);
                        if (!((ServiceEditor) editeurActif).getKeepVersion().getSelection() && (major || minor)) {
                            try {
                                Service newService = ((ServiceEditor) editeurActif).getService();
                                if (Application.getCurrentServiceRepository() == oldRepository) {
                                    ServiceEditor newEditor = (ServiceEditor) window.getActivePage().openEditor(new ServiceEditorInput(newService.getSelfSxSReference()), ServiceEditor.ID);
                                    window.getActivePage().activate(newEditor);
                                    IWorkbenchPage page = window.getActivePage();
                                    IEditorReference[] tabEditorsRefs = page.getEditorReferences();
                                    for (IEditorReference refEditor : tabEditorsRefs) {
                                        IEditorPart editor = refEditor.getEditor(false);
                                        if ((editor instanceof ServiceEditor) && (!editor.equals(newEditor))) {
                                            Service s = ((ServiceEditor) editor).getService();
                                            if (s.getId().equals(newService.getId()) && s.getVersion().equals(newService.getVersion())) {
                                                page.closeEditor(editor, false);
                                            }
                                        }
                                    }
                                    newEditor.getKeepVersion().setSelection(true);
                                } else {
                                    if (Application.isConnected()) {
                                        System.out.println("PCREATOR - Veuillez vous déconnecter pour ouvrir le nouveau service");
                                        MessageBox messageBox = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OK | SWT.ICON_INFORMATION);
                                        messageBox.setText("Information");
                                        messageBox.setMessage("Nouveau service créé avec succès. Veuillez vous déconnecter pour l'ouvrir.");
                                        messageBox.open();
                                    } else {
                                        System.out.println("PCREATOR - Veuillez vous connecter pour ouvrir le nouveau service");
                                        MessageBox messageBox = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OK | SWT.ICON_INFORMATION);
                                        messageBox.setText("Information");
                                        messageBox.setMessage("Nouveau service créé avec succès. Veuillez vous connecter pour l'ouvrir.");
                                        messageBox.open();
                                    }
                                }
                                window.getActivePage().closeEditor(editeurActif, false);
                            } catch (PartInitException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.out.println("PCREATOR - Aucune modification à sauvegarder");
                    }
                }
            }
        });
        form.reflow(true);
    }

    public void createPartControl(Composite parent) {
        toolkit = new FormToolkit(parent.getDisplay());
        form = toolkit.createScrolledForm(parent);
        form.setText("Editeur de Service");
        RowLayout rowForm = new RowLayout();
        rowForm.type = SWT.VERTICAL;
        rowForm.fill = true;
        form.getBody().setLayout(rowForm);
        toolkit.createLabel(form.getBody(), "");
        keepVersion = toolkit.createButton(form.getBody(), "Sauvegarde des changements sur la version actuelle (\"" + service.getVersion() + "\")", SWT.CHECK);
        toolkit.createLabel(form.getBody(), "");
        createSectionIdentification(form);
        createSectionExecution(form);
        createSectionErreur(form);
        createSectionEntree(form);
        createSectionSortie(form);
        createSectionEnregister(form);
    }

    public void setFocus() {
        form.setFocus();
        form.setEnabled(true);
        IContextService contextService = (IContextService) getSite().getService(IContextService.class);
        contextService.activateContext("fr.insa.rennes.pelias.pcreator.serviceEditorContext");
    }

    private void synchroniserCombo(ScrolledForm f, Combo c) {
        c.removeAll();
        c.add("");
        List<ServiceInput> liste = service.getInputs();
        for (ServiceInput in : liste) {
            if (in.getType() == InputType.Batch) {
                c.add(in.getName());
            }
        }
        c.setText("");
        f.reflow(true);
    }

    private void synchroniserErreur(ScrolledForm f, Table tableErreur) {
        tableErreur.removeAll();
        if (!(service.getErrors().isEmpty())) {
            Set<Entry<Integer, String>> donneesErreur = service.getErrors().entrySet();
            for (Entry<Integer, String> e : donneesErreur) {
                TableItem item = new TableItem(tableErreur, 0, tableErreur.getItemCount());
                item.setText(new String[] { e.getKey().toString(), e.getValue() });
            }
        }
        f.reflow(true);
    }

    public void afficherService() {
        System.out.println("Nom : " + service.getLabel());
        System.out.println("Des : " + service.getDescription());
        System.out.println("Pla : " + service.getPlatform());
        System.out.println("Loc : " + service.getLocation());
        System.out.println("Err : " + service.getErrors());
        List<ServiceInput> list = service.getInputs();
        Iterator<ServiceInput> i = list.iterator();
        System.out.println("Inp : " + service.getInputs());
        while (i.hasNext()) {
            ServiceInput courant = i.next();
            System.out.println(courant.getName());
            System.out.println(courant.getDescription());
            System.out.println(courant.getMIMEType());
            System.out.println(courant.getOption());
            System.out.println(courant.getDefaultValue());
        }
        if (service.getFileByFileInputIndex() != -1) {
            System.out.println("FbF Inp : " + service.getInputs().get(service.getFileByFileInputIndex()).getName());
        } else {
            System.out.println("FbF Inp : Pas défini");
        }
        System.out.println("Out : " + service.getOutputs());
    }

    private void afficherFenetreAjoutEntree(final Table tableEntree, final Combo fbfInput) {
        final Shell dialog = new Shell(form.getDisplay(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        dialog.setLayout(new GridLayout(2, false));
        dialog.setText("Ajout d'une Entrée");
        Color gris = new Color(null, 240, 240, 240);
        dialog.setBackground(gris);
        final FormToolkit toolkitEntree = new FormToolkit(dialog.getDisplay());
        Label labelNom = toolkitEntree.createLabel(dialog, "Nom");
        labelNom.setBackground(gris);
        final Text nomEntree = toolkitEntree.createText(dialog, "");
        nomEntree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label labelDesc = toolkitEntree.createLabel(dialog, "Description");
        labelDesc.setBackground(gris);
        final Text descEntree = toolkitEntree.createText(dialog, "");
        descEntree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label labelMime = toolkit.createLabel(dialog, "Type MIME");
        labelMime.setBackground(gris);
        final Combo mimeEntree = new Combo(dialog, SWT.DROP_DOWN);
        for (int i = 0; i < Application.getListeMIME().size(); i++) {
            mimeEntree.add(Application.getListeMIME().get(i));
        }
        Label labelOption = toolkitEntree.createLabel(dialog, "Option(s)");
        labelOption.setBackground(gris);
        final Text optionsEntree = toolkitEntree.createText(dialog, "");
        optionsEntree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label labelDefaut = toolkitEntree.createLabel(dialog, "Valeur par défaut");
        labelDefaut.setBackground(gris);
        final Text valeurEntree = toolkitEntree.createText(dialog, "");
        valeurEntree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nomEntree.setEnabled(false);
        descEntree.setEnabled(false);
        mimeEntree.setEnabled(false);
        optionsEntree.setEnabled(false);
        valeurEntree.setEnabled(false);
        Label labelType = toolkitEntree.createLabel(dialog, "Type d'entrée");
        labelType.setBackground(gris);
        Composite radios = toolkit.createComposite(dialog);
        radios.setBackground(gris);
        radios.setLayout(new FillLayout());
        final Button lot = toolkitEntree.createButton(radios, "Lot", SWT.RADIO);
        lot.setBackground(gris);
        final Button param = toolkitEntree.createButton(radios, "Paramètre", SWT.RADIO);
        param.setBackground(gris);
        toolkitEntree.createLabel(dialog, "");
        Label labelText = toolkitEntree.createLabel(dialog, "Pensez à rajouter un espace à la fin de l'option si nécessaire");
        labelText.setBackground(gris);
        Label labelApercu = toolkitEntree.createLabel(dialog, "Aperçu");
        labelApercu.setBackground(gris);
        final Text apercu = toolkitEntree.createText(dialog, "");
        apercu.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkitEntree.createLabel(dialog, "");
        toolkitEntree.createLabel(dialog, "");
        apercu.setEditable(false);
        apercu.setEnabled(false);
        optionsEntree.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                String option = ((Text) e.widget).getText();
                if (valeurEntree.isEnabled()) {
                    apercu.setText(option + valeurEntree.getText());
                } else {
                    apercu.setText(option + "fichier1.ext");
                }
            }
        });
        valeurEntree.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                String valeur = ((Text) e.widget).getText();
                if (valeurEntree.isEnabled()) {
                    apercu.setText(optionsEntree.getText() + valeur);
                }
            }
        });
        param.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                nomEntree.setEnabled(true);
                descEntree.setEnabled(true);
                mimeEntree.setEnabled(false);
                optionsEntree.setEnabled(true);
                valeurEntree.setEnabled(true);
                apercu.setText(optionsEntree.getText() + valeurEntree.getText());
            }
        });
        lot.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                nomEntree.setEnabled(true);
                descEntree.setEnabled(true);
                mimeEntree.setEnabled(true);
                optionsEntree.setEnabled(true);
                valeurEntree.setEnabled(false);
                apercu.setText(optionsEntree.getText() + "fichier1.ext");
            }
        });
        toolkit.createLabel(dialog, "");
        Composite barreBouton = toolkit.createComposite(dialog);
        barreBouton.setBackground(gris);
        FillLayout fill = new FillLayout();
        fill.marginWidth = 65;
        barreBouton.setLayout(fill);
        final Button okEntree = toolkit.createButton(barreBouton, "OK", SWT.PUSH);
        okEntree.setBackground(gris);
        okEntree.setEnabled(false);
        okEntree.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                major = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                TableItem item = new TableItem(tableEntree, 0, tableEntree.getItemCount());
                ServiceInput in = new ServiceInput();
                if (valeurEntree.isEnabled()) {
                    item.setText(new String[] { "Paramètre", nomEntree.getText(), descEntree.getText(), "", valeurEntree.getText(), optionsEntree.getText() });
                    in.setType(InputType.UserParameter);
                    in.setName(nomEntree.getText());
                    in.setDescription(descEntree.getText());
                    in.setMIMEType("");
                    in.setDefaultValue(valeurEntree.getText());
                    in.setOption(optionsEntree.getText());
                } else {
                    item.setText(new String[] { "Lot", nomEntree.getText(), descEntree.getText(), mimeEntree.getText(), "", optionsEntree.getText() });
                    in.setType(InputType.Batch);
                    in.setName(nomEntree.getText());
                    in.setDescription(descEntree.getText());
                    in.setMIMEType(mimeEntree.getText());
                    in.setDefaultValue("");
                    in.setOption(optionsEntree.getText());
                }
                service.getInputs().add(in);
                synchroniserCombo(form, fbfInput);
                service.setFileByFileInputIndex(-1);
                tableEntree.pack();
                dialog.close();
                form.reflow(true);
            }
        });
        Button annulerEntree = toolkit.createButton(barreBouton, "Annuler", SWT.PUSH);
        annulerEntree.setBackground(gris);
        annulerEntree.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                dialog.close();
                form.reflow(true);
            }
        });
        nomEntree.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                if (nomEntree.getText().equals("")) {
                    okEntree.setEnabled(false);
                } else {
                    if (!valeurEntree.isEnabled()) {
                        if (mimeEntree.getText().equals("")) {
                            okEntree.setEnabled(false);
                        } else {
                            okEntree.setEnabled(true);
                        }
                    } else {
                        okEntree.setEnabled(true);
                    }
                }
            }
        });
        mimeEntree.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                String texte = ((Combo) e.widget).getText();
                if (texte.equals("")) {
                    okEntree.setEnabled(false);
                } else {
                    if (nomEntree.getText().equals("")) {
                        okEntree.setEnabled(false);
                    } else {
                        okEntree.setEnabled(true);
                    }
                }
            }
        });
        lot.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                if (nomEntree.getText().equals("") || mimeEntree.getText().equals("")) {
                    okEntree.setEnabled(false);
                } else {
                    okEntree.setEnabled(true);
                }
            }
        });
        param.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                if (nomEntree.getText().equals("")) {
                    okEntree.setEnabled(false);
                } else {
                    okEntree.setEnabled(true);
                }
            }
        });
        dialog.setDefaultButton(okEntree);
        nomEntree.setEnabled(true);
        descEntree.setEnabled(true);
        mimeEntree.setEnabled(true);
        optionsEntree.setEnabled(true);
        lot.setSelection(true);
        dialog.pack();
        dialog.open();
    }

    private void afficherFenetreModificationEntree(Table tableEntree, final Combo fbfInput) {
        final int selection = tableEntree.getSelectionIndex();
        if (selection >= 0) {
            final TableItem item = tableEntree.getItem(selection);
            ServiceInput input = service.getInputs().get(selection);
            final Shell dialog = new Shell(form.getDisplay(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
            dialog.setLayout(new GridLayout(2, false));
            dialog.setText("Modification d'une Entrée");
            Color gris = new Color(null, 240, 240, 240);
            dialog.setBackground(gris);
            final FormToolkit toolkitEntree = new FormToolkit(dialog.getDisplay());
            Label labelEntree = toolkitEntree.createLabel(dialog, "Nom");
            labelEntree.setBackground(gris);
            final Text nomEntree = toolkitEntree.createText(dialog, input.getName());
            nomEntree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label labelDesc = toolkitEntree.createLabel(dialog, "Description");
            labelDesc.setBackground(gris);
            final Text descEntree = toolkitEntree.createText(dialog, input.getDescription());
            descEntree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label labelMime = toolkit.createLabel(dialog, "Type MIME");
            labelMime.setBackground(gris);
            final Combo mimeEntree = new Combo(dialog, SWT.DROP_DOWN);
            for (int i = 0; i < Application.getListeMIME().size(); i++) {
                mimeEntree.add(Application.getListeMIME().get(i));
            }
            mimeEntree.setText(input.getMIMEType());
            Label labelOption = toolkitEntree.createLabel(dialog, "Option(s)");
            labelOption.setBackground(gris);
            final Text optionsEntree = toolkitEntree.createText(dialog, input.getOption());
            optionsEntree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label labelValeur = toolkitEntree.createLabel(dialog, "Valeur par défaut");
            labelValeur.setBackground(gris);
            final Text valeurEntree = toolkitEntree.createText(dialog, input.getDefaultValue());
            valeurEntree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            nomEntree.setEnabled(false);
            descEntree.setEnabled(false);
            mimeEntree.setEnabled(false);
            optionsEntree.setEnabled(false);
            valeurEntree.setEnabled(false);
            Label labelType = toolkitEntree.createLabel(dialog, "Type d'entrée");
            labelType.setBackground(gris);
            Composite radios = toolkit.createComposite(dialog);
            radios.setBackground(gris);
            radios.setLayout(new FillLayout());
            final Button lot = toolkitEntree.createButton(radios, "Lot", SWT.RADIO);
            lot.setBackground(gris);
            final Button param = toolkitEntree.createButton(radios, "Paramètre", SWT.RADIO);
            param.setBackground(gris);
            toolkitEntree.createLabel(dialog, "");
            Label labelText = toolkitEntree.createLabel(dialog, "Pensez à rajouter un espace à la fin de l'option si nécessaire");
            labelText.setBackground(gris);
            Label labelApercu = toolkitEntree.createLabel(dialog, "Aperçu");
            labelApercu.setBackground(gris);
            final Text apercu = toolkitEntree.createText(dialog, "");
            apercu.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            toolkitEntree.createLabel(dialog, "");
            toolkitEntree.createLabel(dialog, "");
            apercu.setEditable(false);
            apercu.setEnabled(false);
            if (input.getType() == InputType.UserParameter) {
                param.setSelection(true);
                nomEntree.setEnabled(true);
                descEntree.setEnabled(true);
                mimeEntree.setEnabled(false);
                optionsEntree.setEnabled(true);
                valeurEntree.setEnabled(true);
                apercu.setText(optionsEntree.getText() + valeurEntree.getText());
            }
            if (input.getType() == InputType.Batch) {
                lot.setSelection(true);
                nomEntree.setEnabled(true);
                descEntree.setEnabled(true);
                mimeEntree.setEnabled(true);
                optionsEntree.setEnabled(true);
                valeurEntree.setEnabled(false);
                apercu.setText(optionsEntree.getText() + "fichier1.ext");
            }
            optionsEntree.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    String option = ((Text) e.widget).getText();
                    if (valeurEntree.isEnabled()) {
                        apercu.setText(option + valeurEntree.getText());
                    } else {
                        apercu.setText(option + "fichier1.ext");
                    }
                }
            });
            valeurEntree.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    String valeur = ((Text) e.widget).getText();
                    if (valeurEntree.isEnabled()) {
                        apercu.setText(optionsEntree.getText() + valeur);
                    }
                }
            });
            param.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    nomEntree.setEnabled(true);
                    descEntree.setEnabled(true);
                    mimeEntree.setEnabled(false);
                    optionsEntree.setEnabled(true);
                    valeurEntree.setEnabled(true);
                    apercu.setText(optionsEntree.getText() + valeurEntree.getText());
                }
            });
            lot.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    nomEntree.setEnabled(true);
                    descEntree.setEnabled(true);
                    mimeEntree.setEnabled(true);
                    optionsEntree.setEnabled(true);
                    valeurEntree.setEnabled(false);
                    apercu.setText(optionsEntree.getText() + "fichier1.ext");
                }
            });
            toolkit.createLabel(dialog, "");
            Composite barreBouton = toolkit.createComposite(dialog);
            FillLayout fill = new FillLayout();
            fill.marginWidth = 65;
            barreBouton.setBackground(gris);
            barreBouton.setLayout(fill);
            final Button okEntree = toolkit.createButton(barreBouton, "OK", SWT.PUSH);
            okEntree.setBackground(gris);
            okEntree.setEnabled(true);
            okEntree.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    major = true;
                    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                    ServiceInput in = service.getInputs().get(selection);
                    if (valeurEntree.isEnabled()) {
                        item.setText(new String[] { "Paramètre", nomEntree.getText(), descEntree.getText(), "", valeurEntree.getText(), optionsEntree.getText() });
                        in.setType(InputType.UserParameter);
                        in.setName(nomEntree.getText());
                        in.setDescription(descEntree.getText());
                        in.setMIMEType("");
                        in.setDefaultValue(valeurEntree.getText());
                        in.setOption(optionsEntree.getText());
                    } else {
                        item.setText(new String[] { "Lot", nomEntree.getText(), descEntree.getText(), mimeEntree.getText(), "", optionsEntree.getText() });
                        in.setType(InputType.Batch);
                        in.setName(nomEntree.getText());
                        in.setDescription(descEntree.getText());
                        in.setMIMEType(mimeEntree.getText());
                        in.setDefaultValue("");
                        in.setOption(optionsEntree.getText());
                    }
                    synchroniserCombo(form, fbfInput);
                    service.setFileByFileInputIndex(-1);
                    dialog.close();
                    form.reflow(true);
                }
            });
            Button annulerEntree = toolkit.createButton(barreBouton, "Annuler", SWT.PUSH);
            annulerEntree.setBackground(gris);
            annulerEntree.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    dialog.close();
                    form.reflow(true);
                }
            });
            nomEntree.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    if (nomEntree.getText().equals("")) {
                        okEntree.setEnabled(false);
                    } else {
                        if (!valeurEntree.isEnabled()) {
                            if (mimeEntree.getText().equals("")) {
                                okEntree.setEnabled(false);
                            } else {
                                okEntree.setEnabled(true);
                            }
                        } else {
                            okEntree.setEnabled(true);
                        }
                    }
                }
            });
            mimeEntree.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    String texte = ((Combo) e.widget).getText();
                    if (texte.equals("")) {
                        okEntree.setEnabled(false);
                    } else {
                        if (nomEntree.getText().equals("")) {
                            okEntree.setEnabled(false);
                        } else {
                            okEntree.setEnabled(true);
                        }
                    }
                }
            });
            lot.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent e) {
                }

                public void widgetSelected(SelectionEvent e) {
                    if (nomEntree.getText().equals("") || mimeEntree.getText().equals("")) {
                        okEntree.setEnabled(false);
                    } else {
                        okEntree.setEnabled(true);
                    }
                }
            });
            param.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent e) {
                }

                public void widgetSelected(SelectionEvent e) {
                    if (nomEntree.getText().equals("")) {
                        okEntree.setEnabled(false);
                    } else {
                        okEntree.setEnabled(true);
                    }
                }
            });
            dialog.setDefaultButton(okEntree);
            dialog.pack();
            dialog.open();
        }
    }

    private void afficherFenetreAjoutSortie(final Table tableSortie) {
        final Shell dialog = new Shell(form.getDisplay(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        dialog.setLayout(new GridLayout(2, false));
        dialog.setText("Ajout d'une Sortie");
        Color gris = new Color(null, 240, 240, 240);
        dialog.setBackground(gris);
        FormToolkit toolkit = new FormToolkit(dialog.getDisplay());
        Label labelNom = toolkit.createLabel(dialog, "Nom de la sortie");
        labelNom.setBackground(gris);
        final Text nomSortie = toolkit.createText(dialog, "");
        nomSortie.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label labelDesc = toolkit.createLabel(dialog, "Description de la sortie");
        labelDesc.setBackground(gris);
        final Text descSortie = toolkit.createText(dialog, "");
        descSortie.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label labelMime = toolkit.createLabel(dialog, "Type MIME");
        labelMime.setBackground(gris);
        final Combo combo = new Combo(dialog, SWT.DROP_DOWN);
        for (int i = 0; i < Application.getListeMIME().size(); i++) {
            combo.add(Application.getListeMIME().get(i));
        }
        toolkit.createLabel(dialog, "");
        Label labelVide = toolkit.createLabel(dialog, "\t\t\t\t");
        labelVide.setBackground(gris);
        toolkit.createLabel(dialog, "");
        Composite barreBouton = toolkit.createComposite(dialog);
        barreBouton.setBackground(gris);
        barreBouton.setLayout(new FillLayout());
        final Button okSortie = toolkit.createButton(barreBouton, "OK", SWT.PUSH);
        okSortie.setBackground(gris);
        okSortie.setEnabled(false);
        okSortie.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                major = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                TableItem item1 = new TableItem(tableSortie, 0, tableSortie.getItemCount());
                item1.setText(new String[] { nomSortie.getText(), descSortie.getText(), combo.getText() });
                ServiceOutput out = new ServiceOutput();
                out.setName(nomSortie.getText());
                out.setDescription(descSortie.getText());
                out.setMIMEType(combo.getText());
                service.getOutputs().add(out);
                dialog.close();
                form.reflow(true);
            }
        });
        Button annulerSortie = toolkit.createButton(barreBouton, "Annuler", SWT.PUSH);
        annulerSortie.setBackground(gris);
        annulerSortie.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                dialog.close();
            }
        });
        nomSortie.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                String texte = ((Text) e.widget).getText();
                if (texte.equals("")) {
                    okSortie.setEnabled(false);
                } else {
                    if (combo.getText().equals("")) {
                        okSortie.setEnabled(false);
                    } else {
                        okSortie.setEnabled(true);
                    }
                }
            }
        });
        combo.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                String texte = ((Combo) e.widget).getText();
                if (texte.equals("")) {
                    okSortie.setEnabled(false);
                } else {
                    if (nomSortie.getText().equals("")) {
                        okSortie.setEnabled(false);
                    } else {
                        okSortie.setEnabled(true);
                    }
                }
            }
        });
        dialog.setDefaultButton(okSortie);
        dialog.pack();
        dialog.open();
    }

    private void afficherFenetreModificationSortie(final Table tableSortie) {
        final int selection = tableSortie.getSelectionIndex();
        if (selection >= 0) {
            final TableItem item = tableSortie.getItem(selection);
            ServiceOutput output = service.getOutputs().get(selection);
            final Shell dialog = new Shell(form.getDisplay(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
            dialog.setLayout(new GridLayout(2, false));
            dialog.setText("Modification d'une Sortie");
            Color gris = new Color(null, 240, 240, 240);
            dialog.setBackground(gris);
            FormToolkit toolkit = new FormToolkit(dialog.getDisplay());
            Label labelNom = toolkit.createLabel(dialog, "Nom de la sortie");
            labelNom.setBackground(gris);
            final Text nomSortie = toolkit.createText(dialog, output.getName());
            nomSortie.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label labelDesc = toolkit.createLabel(dialog, "Description de la sortie");
            labelDesc.setBackground(gris);
            final Text descSortie = toolkit.createText(dialog, output.getDescription());
            descSortie.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label labelMime = toolkit.createLabel(dialog, "Type MIME");
            labelMime.setBackground(gris);
            final Combo combo = new Combo(dialog, SWT.DROP_DOWN);
            for (int i = 0; i < Application.getListeMIME().size(); i++) {
                combo.add(Application.getListeMIME().get(i));
            }
            combo.setText(output.getMIMEType());
            toolkit.createLabel(dialog, "");
            Label labelVide = toolkit.createLabel(dialog, "\t\t\t\t");
            labelVide.setBackground(gris);
            toolkit.createLabel(dialog, "");
            Composite barreBouton = toolkit.createComposite(dialog);
            barreBouton.setBackground(gris);
            barreBouton.setLayout(new FillLayout());
            final Button okSortie = toolkit.createButton(barreBouton, "OK", SWT.PUSH);
            okSortie.setBackground(gris);
            okSortie.setEnabled(true);
            okSortie.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    major = true;
                    firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                    item.setText(new String[] { nomSortie.getText(), descSortie.getText(), combo.getText() });
                    ServiceOutput out = service.getOutputs().get(selection);
                    out.setName(nomSortie.getText());
                    out.setDescription(descSortie.getText());
                    out.setMIMEType(combo.getText());
                    tableSortie.pack();
                    dialog.close();
                    form.reflow(true);
                }
            });
            Button annulerSortie = toolkit.createButton(barreBouton, "Annuler", SWT.PUSH);
            annulerSortie.setBackground(gris);
            annulerSortie.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    dialog.close();
                }
            });
            nomSortie.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    String texte = ((Text) e.widget).getText();
                    if (texte.equals("")) {
                        okSortie.setEnabled(false);
                    } else {
                        if (combo.getText().equals("")) {
                            okSortie.setEnabled(false);
                        } else {
                            okSortie.setEnabled(true);
                        }
                    }
                }
            });
            combo.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    String texte = ((Combo) e.widget).getText();
                    if (texte.equals("")) {
                        okSortie.setEnabled(false);
                    } else {
                        if (nomSortie.getText().equals("")) {
                            okSortie.setEnabled(false);
                        } else {
                            okSortie.setEnabled(true);
                        }
                    }
                }
            });
            dialog.setDefaultButton(okSortie);
            dialog.pack();
            dialog.open();
        }
    }

    public Service getService() {
        return service;
    }

    public ISxSRepository<Service> getRepository() {
        return repository;
    }

    public Button getKeepVersion() {
        return keepVersion;
    }

    private void verifierDependances(final Service oldService, final Service newService) {
        final List<PObjectReference> choix = new LinkedList<PObjectReference>();
        final List<Button> boutonACocher = new LinkedList<Button>();
        try {
            final List<PObjectReference> chaineAMAJ = repository.getObjectRegisteredConsumers(oldService.getId(), oldService.getVersion(), false);
            if (chaineAMAJ.size() > 0) {
                final Shell dialog = new Shell(form.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
                RowLayout rowDialog = new RowLayout();
                rowDialog.type = SWT.VERTICAL;
                rowDialog.fill = false;
                rowDialog.spacing = 10;
                dialog.setLayout(rowDialog);
                dialog.setText("Choix des chaines à impacter");
                Color gris = new Color(null, 240, 240, 240);
                dialog.setBackground(gris);
                Label message = new Label(dialog, SWT.NULL);
                message.setBackground(gris);
                message.setText("Le service \"" + oldService.getLabel() + " " + oldService.getVersion() + "\" est utilisé dans les chaines suivantes :");
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
                explication.setText("Veuillez choisir les chaines à mettre à jour vers \"" + newService.getLabel() + " " + newService.getVersion() + "\".");
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
                                if (comp instanceof ServiceCall) {
                                    if (((ServiceCall) comp).getServiceReference().getId().equals(oldService.getId()) && ((ServiceCall) comp).getServiceReference().getVersion().equals(oldService.getVersion())) {
                                        ((ServiceCall) comp).getServiceReference().setId(newService.getId());
                                        ((ServiceCall) comp).getServiceReference().setVersion(newService.getVersion());
                                    }
                                }
                            }
                            chainRepository.putObject(c, true, false);
                        }
                        actualiseEditeursChaines(newService);
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

    public void actualiseEditeursChaines(Service oldService) {
        try {
            List<PObjectReference> chaineAMAJ = repository.getObjectRegisteredConsumers(oldService.getId(), oldService.getVersion(), false);
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

    public boolean isMinor() {
        return minor;
    }

    public boolean isMajor() {
        return major;
    }

    public void renommage(String s) {
        service.setLabel(s);
        nomService.setText(s);
        modification = false;
        firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        setPartName(service.getLabel() + " " + service.getVersion());
    }
}
