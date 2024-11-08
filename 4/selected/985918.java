package com.doculibre.intelligid.wicket.pages.mesdossiers.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import wicket.PageParameters;
import wicket.WicketRuntimeException;
import wicket.behavior.SimpleAttributeModifier;
import wicket.extensions.markup.html.repeater.data.GridView;
import wicket.extensions.markup.html.repeater.data.ListDataProvider;
import wicket.extensions.markup.html.repeater.refreshing.Item;
import wicket.markup.html.basic.Label;
import wicket.markup.html.form.Button;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.Radio;
import wicket.markup.html.form.RadioGroup;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.model.Model;
import com.doculibre.intelligid.delegate.FGDDelegate;
import com.doculibre.intelligid.entites.FicheDocument;
import com.doculibre.intelligid.entites.FicheDossier;
import com.doculibre.intelligid.entites.FichierElectronique;
import com.doculibre.intelligid.entites.FichierElectroniqueDefaut;
import com.doculibre.intelligid.entites.SupportDocument;
import com.doculibre.intelligid.entites.ddv.DomaineValeurs;
import com.doculibre.intelligid.entites.ddv.Langue;
import com.doculibre.intelligid.wicket.pages.BaseFGDPage;
import com.doculibre.intelligid.wicket.pages.DefaultFGDPage;
import com.doculibre.intelligid.wicket.pages.mesdossiers.MesDossiersPage;
import com.doculibre.intelligid.wicket.pages.mesdossiers.dossier.ConsulterDossierPage;

/**
 * @author Vincent Cormier
 * 
 * Classe représentant la page intermédiaire permettant de demander à
 * l'utilisateur quel type de document il désire créer : FicheDocumentReference
 * ou FicheDocumentTransaction
 */
@SuppressWarnings("serial")
public class PreparerDocumentFormPage extends DefaultFGDPage {

    private FicheDocument ficheSoumise;

    public static final String ID_RELATION_EST_PARTIE_DE = "idRelationEstPartieDe";

    public PreparerDocumentFormPage() {
        super();
        initComponents(null);
    }

    public PreparerDocumentFormPage(PageParameters parameters) {
        super(parameters);
        initComponents(parameters);
    }

    public PreparerDocumentFormPage(FicheDocument ficheSoumise) {
        super();
        this.ficheSoumise = ficheSoumise;
        initComponents(null);
    }

    private void initComponents(final PageParameters parameters) {
        FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        add(feedbackPanel);
        PreparerDocumentForm form = new PreparerDocumentForm("preparerDocumentForm", parameters);
        add(form);
        List<String[]> elementsTypeDocument = new ArrayList<String[]>();
        elementsTypeDocument.add(new String[] { "reference", "Document de référence" });
        elementsTypeDocument.add(new String[] { "transaction", "Document de transaction" });
        RadioGroup radioGroupTypeDocument = getRadioGroup("typeDocument", elementsTypeDocument, new Model());
        radioGroupTypeDocument.setLabel(new Model("Type de document"));
        radioGroupTypeDocument.setRequired(true);
        form.add(radioGroupTypeDocument);
        Button okButton = new Button("okButton");
        form.add(okButton);
        Button cancelButton = new Button("cancelButton") {

            @Override
            protected void onSubmit() {
                FGDDelegate delegate = new FGDDelegate();
                FicheDossier relationEstPartieDe = null;
                if (parameters != null) {
                    String idRelationEstPartieDe = parameters.getString(ID_RELATION_EST_PARTIE_DE);
                    if (idRelationEstPartieDe != null) {
                        relationEstPartieDe = delegate.getFicheDossier(new Long(idRelationEstPartieDe), getUtilisateurCourant());
                    } else {
                        relationEstPartieDe = null;
                    }
                }
                if (relationEstPartieDe != null) {
                    setResponsePage(new ConsulterDossierPage(new PageParameters("id=" + relationEstPartieDe.getId())));
                } else {
                    setResponsePage(MesDossiersPage.class);
                }
            }
        };
        cancelButton.setDefaultFormProcessing(false);
        form.add(cancelButton);
    }

    private class PreparerDocumentForm extends Form {

        PageParameters parameters;

        public PreparerDocumentForm(String id, PageParameters parameters) {
            super(id);
            this.parameters = parameters;
        }

        @Override
        protected void onSubmit() {
            FGDDelegate delegate = new FGDDelegate();
            FicheDossier relationEstPartieDe = null;
            if (parameters != null) {
                String idRelationEstPartieDe = parameters.getString(ID_RELATION_EST_PARTIE_DE);
                if (idRelationEstPartieDe != null) {
                    relationEstPartieDe = delegate.getFicheDossier(new Long(idRelationEstPartieDe), getUtilisateurCourant());
                } else {
                    relationEstPartieDe = null;
                }
            }
            FicheDocument ficheDocument = new FicheDocument();
            String choix = ((String) this.get("typeDocument").getModelObject());
            if ("transaction".equals(choix)) {
                ficheDocument.setTransaction(true);
            } else {
                ficheDocument.setCourriel(true);
            }
            ficheDocument.setRelationEstPartieDe(relationEstPartieDe);
            DomaineValeurs langues = delegate.getDomaineValeurs(Langue.class);
            Langue francais = (Langue) langues.getElement("francais");
            if (francais != null) {
                ficheDocument.getLangues().add(francais);
            }
            if (ficheSoumise != null) {
                peuplerSelonElementsFicheSoumises(ficheDocument);
            }
            setResponsePage(new DocumentFormPage(ficheDocument, ficheSoumise));
        }
    }

    /**
	 * Méthode utilisée pour peupler la ficheDocument selon le contenu de la
	 * ficheSoumise si elle existe.
	 */
    @Deprecated
    private void peuplerSelonElementsFicheSoumises(FicheDocument ficheDocument) {
        ficheDocument.setTitre(ficheSoumise.getTitre());
        ficheDocument.setAutresTitres(new ArrayList<String>(ficheSoumise.getAutresTitres()));
        if (!ficheSoumise.getLangues().isEmpty()) {
            ficheDocument.setLangues(ficheSoumise.getLangues());
        }
        ficheDocument.setDateCreation(ficheSoumise.getDateCreation());
        String createur = ficheSoumise.getUtilisateurSoumetteur().getPrenom() + " " + ficheSoumise.getUtilisateurSoumetteur().getNomFamille();
        List<String> createurs = new ArrayList<String>();
        createurs.add(createur);
        ficheDocument.setCreateursDocument(createurs);
        SupportDocument support = ficheSoumise.getSupports().get(0);
        FichierElectronique fichierElectronique = support.getFichierElectronique();
        SupportDocument nouveauSupport = new SupportDocument();
        nouveauSupport.setFicheDocument(ficheDocument);
        nouveauSupport.setTypeSupport(support.getTypeSupport());
        nouveauSupport.setTaille(support.getTaille());
        ficheDocument.getSupports().add(nouveauSupport);
        try {
            FichierElectronique nouveauFichierElectronique = new FichierElectroniqueDefaut();
            copier(fichierElectronique, nouveauFichierElectronique);
            nouveauSupport.setFichierElectronique(nouveauFichierElectronique);
            nouveauFichierElectronique.setSupport(nouveauSupport);
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }
        ficheDocument.setRelationEstPartieDe(ficheSoumise.getRelationEstPartieDe());
    }

    /**
	 * Méthode générique pour construire les sections contenant des boutons
	 * radio.
	 * 
	 * @param id
	 *            le id à utiliser dans le code HTML
	 * @param elements
	 *            la liste des elements à afficher
	 * @param model
	 *            le model à utiliser
	 */
    private RadioGroup getRadioGroup(String id, final List<String[]> elements, Model model) {
        RadioGroup radioGroupRole = new RadioGroup(id, model) {

            @Override
            public boolean isRequired() {
                return true;
            }
        };
        final List<Radio> radios = new ArrayList<Radio>();
        for (String[] element : elements) {
            radios.add(new Radio("radio", new Model(element[0])));
        }
        GridView gridView = new GridView("rows", new ListDataProvider(elements)) {

            @Override
            protected void populateEmptyItem(Item item) {
                String[] element = (String[]) elements.get(item.getIndex());
                Radio radio = radios.get(item.getIndex());
                radio.add(new SimpleAttributeModifier("id", "item" + item.getIndex()));
                radio.setVisible(false);
                item.add(radio);
                Label label = new Label("label", element[1]);
                label.add(new SimpleAttributeModifier("for", "item" + item.getIndex()));
                label.setVisible(false);
                item.add(label);
            }

            @Override
            protected void populateItem(Item item) {
                String[] element = (String[]) elements.get(item.getIndex());
                Radio radio = radios.get(item.getIndex());
                radio.add(new SimpleAttributeModifier("id", "item" + item.getIndex()));
                item.add(radio);
                Label label = new Label("label", element[1]);
                label.add(new SimpleAttributeModifier("for", "item" + item.getIndex()));
                item.add(label);
            }
        };
        gridView.setColumns(1);
        radioGroupRole.add(gridView);
        return radioGroupRole;
    }

    protected Class<? extends BaseFGDPage> getCurrentMenuLinkClass() {
        return MesDossiersPage.class;
    }

    @Override
    protected String getBreadCrumbs() {
        return "Enregistrer un document /";
    }

    @Override
    protected String getTitle() {
        return "Choix du type de document";
    }

    @Override
    public boolean isMultiRequestConversationPage() {
        return true;
    }

    private static void copier(FichierElectronique source, FichierElectronique cible) throws IOException {
        cible.setNom(source.getNom());
        cible.setTaille(source.getTaille());
        cible.setTypeMime(source.getTypeMime());
        cible.setSoumetteur(source.getSoumetteur());
        cible.setDateDerniereModification(source.getDateDerniereModification());
        cible.setEmprunteur(source.getEmprunteur());
        cible.setDateEmprunt(source.getDateEmprunt());
        cible.setNumeroVersion(source.getNumeroVersion());
        InputStream inputStream = source.getInputStream();
        OutputStream outputStream = cible.getOutputStream();
        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            try {
                inputStream.close();
            } finally {
                outputStream.close();
            }
            if (source instanceof FichierElectroniqueDefaut) {
                FichierElectroniqueDefaut fichierElectroniqueTemporaire = (FichierElectroniqueDefaut) source;
                fichierElectroniqueTemporaire.deleteTemp();
            }
        }
    }
}
