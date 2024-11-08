package com.doculibre.intelligid.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import com.doculibre.intelligid.delegate.FGDDelegate;
import com.doculibre.intelligid.entites.ContenuFichierElectronique;
import com.doculibre.intelligid.entites.FicheDocument;
import com.doculibre.intelligid.entites.FicheMetadonnees.TypeFiche;
import com.doculibre.intelligid.entites.FichierElectronique;
import com.doculibre.intelligid.entites.FichierElectroniqueDefaut;
import com.doculibre.intelligid.entites.SupportDocument;
import com.doculibre.intelligid.entites.UtilisateurIFGD;
import com.doculibre.intelligid.entites.ddv.DomaineValeurs;
import com.doculibre.intelligid.entites.ddv.FormatDocument;
import com.doculibre.intelligid.entites.ddv.TypeSupport;
import com.doculibre.intelligid.exceptions.FichierElectroniqueExistantException;
import com.doculibre.intelligid.exceptions.NomFichierElectroniqueTropLong;
import com.doculibre.intelligid.utils.proprietesDocuments.CourrielUtils;
import com.doculibre.intelligid.utils.proprietesDocuments.impl.GestionnaireProprietesMimeMessageParser;

public class FichierElectroniqueUtils {

    public static void setContenu(ContenuFichierElectronique contenuFichier, FichierElectronique fichierElectronique, UtilisateurIFGD utilisateurCourant) throws IOException, DocumentVideException {
        if (contenuFichier != null) {
            SupportDocument support = fichierElectronique.getSupport();
            support.setFichierElectronique(fichierElectronique);
            FicheDocument ficheDocument = support.getFicheDocument();
            String nomFichier = contenuFichier.getNomFichier();
            String extension = FilenameUtils.getExtension(nomFichier);
            if (ficheDocument.getFichierElectronique(nomFichier) != null) {
                FichierElectronique fichierElectroniqueExistant = ficheDocument.getFichierElectronique(nomFichier);
                if (fichierElectroniqueExistant.getId() != null && !fichierElectroniqueExistant.getId().equals(fichierElectronique.getId())) {
                    throw new FichierElectroniqueExistantException(nomFichier, ficheDocument);
                }
            }
            if (fichierElectronique.getId() == null) {
                if (OfficeDocumentPropertiesUtil.canWriteIdIGID(extension)) {
                    Long idIgid = OfficeDocumentPropertiesUtil.getIdIGID(contenuFichier);
                    if (idIgid != null) {
                        throw new FichierElectroniqueExistantException(nomFichier, idIgid, ficheDocument);
                    }
                }
            }
            InputStream inputStream = contenuFichier.getInputStream();
            OutputStream outputStream = fichierElectronique.getOutputStream();
            try {
                IOUtils.copy(inputStream, outputStream);
            } finally {
                try {
                    inputStream.close();
                } finally {
                    outputStream.close();
                }
            }
            String typeMime = contenuFichier.getContentType();
            long tailleFichier = contenuFichier.getTailleFichier();
            Date dateDerniereModification = new Date();
            fichierElectronique.setNom(nomFichier);
            fichierElectronique.setTypeMime(extension);
            creerFormatSiNecessaire(typeMime, extension);
            fichierElectronique.setTaille(tailleFichier);
            fichierElectronique.setDateDerniereModification(dateDerniereModification);
            fichierElectronique.setSoumetteur(utilisateurCourant);
            if (extension.endsWith("msg")) {
                CourrielUtils.peuplerMetadonneesCourriel(fichierElectronique.getNom(), ficheDocument, contenuFichier.getInputStream(), utilisateurCourant);
            } else if (extension.endsWith("eml")) {
                Map<String, Object> properties = new GestionnaireProprietesMimeMessageParser().parseMsg(contenuFichier.getInputStream());
                CourrielUtils.peuplerMetadonneesCourriel(fichierElectronique.getNom(), ficheDocument, properties, utilisateurCourant);
            } else {
                FGDProprietesDocumentUtils.copierMetadonneesProprietes(fichierElectronique, ficheDocument);
            }
        }
    }

    private static void creerFormatSiNecessaire(String typeMime, String extension) {
        FGDDelegate delegate = new FGDDelegate();
        DomaineValeurs formats = delegate.getDomaineValeurs(FormatDocument.class);
        FormatDocument format = (FormatDocument) formats.getElement(typeMime);
        if (format == null) {
            format = new FormatDocument();
            format.setCode(typeMime);
            format.setDescription(extension);
            format.setDomaineValeurs(formats);
            formats.getElements().add(format);
            delegate.sauvegarder(format);
        }
    }

    public static FichierElectronique setContenu(FicheDocument ficheDocument, ContenuFichierElectronique contenuFichier, UtilisateurIFGD utilisateurCourant) throws IOException, DocumentVideException {
        SupportDocument support = new SupportDocument();
        support.setFicheDocument(ficheDocument);
        FichierElectronique fichierElectronique = setContenu(ficheDocument, support, contenuFichier, utilisateurCourant);
        return fichierElectronique;
    }

    public static FichierElectronique setContenu(FicheDocument ficheDocument, SupportDocument support, ContenuFichierElectronique contenuFichier, UtilisateurIFGD utilisateurCourant) throws IOException, DocumentVideException {
        FichierElectronique fichierElectronique;
        if (contenuFichier != null) {
            String nomFichier = contenuFichier.getNomFichier();
            if (ficheDocument.getFichierElectronique(nomFichier) != null) {
                throw new FichierElectroniqueExistantException(nomFichier, ficheDocument);
            }
            int tailleMaximaleNomFichier = FichierElectronique.LONGUEUR_MAXIMALE;
            if (TypeFiche.COURRIEL.equals(ficheDocument.getTypeFiche())) {
                tailleMaximaleNomFichier *= 2;
            }
            if (contenuFichier.getNomFichier().length() > tailleMaximaleNomFichier) {
                throw new NomFichierElectroniqueTropLong(nomFichier);
            }
            fichierElectronique = new FichierElectroniqueDefaut();
            fichierElectronique.setSupport(support);
            support.setFichierElectronique(fichierElectronique);
            support.setFicheDocument(ficheDocument);
            FGDDelegate delegate = new FGDDelegate();
            DomaineValeurs typesSupports = delegate.getDomaineValeurs(TypeSupport.class);
            TypeSupport typeSupport = (TypeSupport) typesSupports.getElement(TypeSupport.CODE_DISQUE_MAGNETIQUE);
            support.setTypeSupport(typeSupport);
            setContenu(contenuFichier, fichierElectronique, utilisateurCourant);
            ficheDocument.getSupports().add(support);
        } else {
            fichierElectronique = null;
        }
        return fichierElectronique;
    }
}
