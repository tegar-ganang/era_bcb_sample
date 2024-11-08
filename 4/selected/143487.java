package com.doculibre.intelligid.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.hpsf.CustomProperties;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.apache.poi.hpsf.WritingNotSupportedException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xslf.XSLFSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;
import com.doculibre.intelligid.delegate.FGDDelegate;
import com.doculibre.intelligid.entites.ContenuFichierElectronique;
import com.doculibre.intelligid.entites.FicheDocument;
import com.doculibre.intelligid.entites.FicheMetadonnees.TypeFiche;
import com.doculibre.intelligid.entites.FichierElectronique;
import com.doculibre.intelligid.entites.profil.MetadonneeProfilMO;
import com.doculibre.intelligid.entites.profil.ProfilSaisieMO;
import com.doculibre.intelligid.entites.profil.constantes.MetadonneesDossier;
import common.Assert;

public class OfficeDocumentPropertiesUtil {

    public static final String PROPRIETE_ID_IGID;

    static {
        String idOrganisation = FGDSpringUtils.getIdOrganisation();
        if (StringUtils.isEmpty(idOrganisation)) {
            PROPRIETE_ID_IGID = "id_igid";
        } else {
            PROPRIETE_ID_IGID = "id_igid_" + idOrganisation;
        }
    }

    private static final List<String> EXTENSIONS_POUR_ECRITURE_ID_IGID = Arrays.asList(new String[] { "doc", "docx", "ppt", "pptx", "xls", "xlsx", "pdf" });

    private static final String EXCEPTION_NON_COMPATIBLE_AVEC_MAC = "Veuillez vérifier que le document a dernièrement été enregistré " + "sur une version de Microsoft Office supportée (2003 et 2007). " + "Un autre logiciel, tel Microsoft Office sur Mac pourrait ne pas " + "écrire le document de sorte à ce que le système le comprenne.";

    public static boolean canWriteIdIGID(String extension) {
        return EXTENSIONS_POUR_ECRITURE_ID_IGID.contains(extension);
    }

    public static boolean canWriteProprietes(String extension) {
        return EXTENSIONS_POUR_ECRITURE_ID_IGID.contains(extension);
    }

    public static Long getIdIGID(ContenuFichierElectronique contenuFichier) throws IOException, DocumentVideException {
        String extension = FilenameUtils.getExtension(contenuFichier.getNomFichier());
        if (EXTENSIONS_POUR_ECRITURE_ID_IGID.contains(extension)) {
            return getLongProperty(extension, contenuFichier.getInputStream(), PROPRIETE_ID_IGID);
        } else {
            return null;
        }
    }

    public static Long getLongProperty(String ext, InputStream in, String propriete) throws IOException, DocumentVideException {
        if (ext.equals("docx") || ext.equals("pptx") || ext.equals("xlsx")) {
            return getLongPropertyOpenXmlDocument(ext, in, propriete);
        } else if (ext.equals("doc") || ext.equals("ppt") || ext.equals("xls")) {
            return getLongDOLE2DocumentProperty(in, propriete);
        } else if (ext.equals("pdf")) {
            return PDFDocumentPropertiesUtil.getLongProperty(in, PROPRIETE_ID_IGID);
        }
        throw new IllegalArgumentException("Extention non supportée : " + ext);
    }

    private static Long getLongPropertyOpenXmlDocument(String ext, InputStream in, String propriete) throws IOException, DocumentVideException {
        in = new BufferedInputStream(in);
        POIXMLDocument doc;
        if (ext.toLowerCase().equals("docx")) {
            doc = new XWPFDocument(in);
        } else if (ext.toLowerCase().equals("xlsx")) {
            doc = new XSSFWorkbook(in);
        } else if (ext.toLowerCase().equals("pptx")) {
            try {
                OPCPackage opcpPackage = OPCPackage.open(in);
                if (opcpPackage == null) {
                    throw new DocumentVideException();
                }
                doc = new XSLFSlideShow(opcpPackage);
            } catch (InvalidFormatException e) {
                throw new RuntimeException(e);
            } catch (OpenXML4JException e) {
                throw new RuntimeException(e);
            } catch (XmlException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                throw new DocumentVideException();
            }
        } else {
            throw new IllegalArgumentException("Writing properties for a " + ext + " file is not supported");
        }
        org.apache.poi.POIXMLProperties.CustomProperties props = doc.getProperties().getCustomProperties();
        for (CTProperty prop : props.getUnderlyingProperties().getPropertyArray()) {
            if (prop.getName().equals(propriete)) {
                return Long.valueOf(prop.getLpwstr());
            }
        }
        return null;
    }

    private static Long getLongDOLE2DocumentProperty(InputStream in, String propriete) throws IOException {
        Long idIGID;
        in = new BufferedInputStream(in);
        POIFSFileSystem poifs;
        try {
            poifs = new POIFSFileSystem(in);
        } catch (IOException e) {
            throw new RuntimeException(EXCEPTION_NON_COMPATIBLE_AVEC_MAC, e);
        } catch (OfficeXmlFileException e) {
            return null;
        } finally {
            in.close();
        }
        DirectoryEntry dir = poifs.getRoot();
        DocumentSummaryInformation dsi;
        try {
            DocumentEntry dsiEntry = (DocumentEntry) dir.getEntry(DocumentSummaryInformation.DEFAULT_STREAM_NAME);
            DocumentInputStream dis = new DocumentInputStream(dsiEntry);
            PropertySet ps = new PropertySet(dis);
            dis.close();
            dsi = new DocumentSummaryInformation(ps);
        } catch (FileNotFoundException ex) {
            dsi = PropertySetFactory.newDocumentSummaryInformation();
        } catch (UnexpectedPropertySetTypeException e) {
            throw new RuntimeException(e);
        } catch (NoPropertySetStreamException e) {
            throw new RuntimeException(e);
        } catch (MarkUnsupportedException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CustomProperties customProperties = dsi.getCustomProperties();
        if (customProperties != null) {
            String idIGIDStr = (String) customProperties.get(propriete);
            if (idIGIDStr != null) {
                try {
                    idIGID = Long.parseLong(idIGIDStr);
                } catch (NumberFormatException e) {
                    idIGID = null;
                }
            } else {
                idIGID = null;
            }
        } else {
            idIGID = null;
        }
        return idIGID;
    }

    public static void writeProperties(final FichierElectronique fichier, FicheDocument ficheDocument) {
        Assert.verify(fichier != null && ficheDocument != null);
        OutputStreamProvider outputStreamProvider = new OutputStreamProvider() {

            @Override
            public OutputStream getOutputStream() {
                return fichier.getOutputStream();
            }
        };
        String auteurs = asString(ficheDocument.getCreateursDocument());
        ProfilSaisieMO profilMO = new FGDDelegate().getProfilSaisieMO();
        MetadonneeProfilMO metadonneeSociete = profilMO.getMetadonnee(TypeFiche.DOCUMENT, "Societe", true);
        String societe = null;
        if (metadonneeSociete != null) {
            societe = (String) ficheDocument.getMetadonneePersonnalisee(metadonneeSociete);
        }
        String motsCles = getMotsCles(ficheDocument, profilMO);
        Map<String, String> properties = new HashMap<String, String>();
        if (auteurs != null) {
            properties.put(Metadata.AUTHOR, auteurs);
        }
        properties.put(Metadata.TITLE, ficheDocument.getTitre());
        if (societe != null) {
            properties.put(Metadata.COMPANY, societe);
        }
        properties.put(Metadata.COMMENTS, ficheDocument.getResume());
        if (motsCles != null) {
            properties.put(Metadata.KEYWORDS, motsCles);
        }
        try {
            long newLength = writeProperties(fichier.getExtension(), fichier.getInputStream(), outputStreamProvider, properties);
            fichier.setTaille(newLength);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'écrire les métadonnées de la fiche document dans le fichier électronique : " + fichier.getNom(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String getMotsCles(FicheDocument ficheDocument, ProfilSaisieMO profilMO) {
        String motsCles = null;
        for (MetadonneeProfilMO metadonneeDocument : profilMO.getMetadonneesDocumentTransaction()) {
            if (metadonneeDocument.getNomPropriete().equals(MetadonneesDossier.MOTS_CLES)) {
                String typeChamp = metadonneeDocument.getType();
                if (MetadonneeProfilMO.TYPE_LISTE.equals(typeChamp)) {
                    motsCles = asString((List<String>) ficheDocument.getMetadonneePersonnalisee(metadonneeDocument));
                } else {
                    motsCles = (String) ficheDocument.getMetadonneePersonnalisee(metadonneeDocument);
                }
                break;
            }
        }
        return motsCles;
    }

    private static String asString(List<String> list) {
        StringBuffer sb = new StringBuffer();
        if (list != null) {
            for (String string : list) {
                sb.append(string.trim());
                sb.append(";");
            }
        }
        return sb.toString();
    }

    public static long writeProperties(File file, Map<String, String> properties) throws IOException {
        return writeProperties(file, file, properties);
    }

    public static long writeProperties(File src, final File dest, Map<String, String> properties) throws IOException {
        try {
            OutputStreamProvider outputStreamProvider = new OutputStreamProvider() {

                @Override
                public OutputStream getOutputStream() {
                    try {
                        return new FileOutputStream(dest);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            String ext = FilenameUtils.getExtension(src.getName());
            return writeProperties(ext, new FileInputStream(src), outputStreamProvider, properties);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Le document n'est pas accessible.", e);
        }
    }

    private static DocumentSummaryInformation getDocumentSummaryInformation(DirectoryEntry dir) {
        DocumentSummaryInformation dsi;
        try {
            DocumentEntry dsiEntry = (DocumentEntry) dir.getEntry(DocumentSummaryInformation.DEFAULT_STREAM_NAME);
            DocumentInputStream dis = new DocumentInputStream(dsiEntry);
            org.apache.poi.hpsf.PropertySet ps = new org.apache.poi.hpsf.PropertySet(dis);
            dis.close();
            dsi = new DocumentSummaryInformation(ps);
        } catch (FileNotFoundException ex) {
            dsi = PropertySetFactory.newDocumentSummaryInformation();
        } catch (UnexpectedPropertySetTypeException e) {
            throw new RuntimeException(e);
        } catch (NoPropertySetStreamException e) {
            throw new RuntimeException(e);
        } catch (MarkUnsupportedException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dsi;
    }

    private static SummaryInformation getSummaryInformation(DirectoryEntry dir) {
        SummaryInformation si;
        try {
            DocumentEntry dsiEntry = (DocumentEntry) dir.getEntry(SummaryInformation.DEFAULT_STREAM_NAME);
            DocumentInputStream dis = new DocumentInputStream(dsiEntry);
            org.apache.poi.hpsf.PropertySet ps = new org.apache.poi.hpsf.PropertySet(dis);
            dis.close();
            si = new SummaryInformation(ps);
        } catch (FileNotFoundException ex) {
            si = PropertySetFactory.newSummaryInformation();
        } catch (UnexpectedPropertySetTypeException e) {
            throw new RuntimeException(e);
        } catch (NoPropertySetStreamException e) {
            throw new RuntimeException(e);
        } catch (MarkUnsupportedException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return si;
    }

    /**
	 * Écrit les propriétés spécifiées dans le fichier Office
	 * 
	 * @param in
	 *            Le fichier en entrée
	 * @param outProvider
	 *            Le fichier en sortie
	 * @param properties
	 *            Les propriétés
	 * @throws IOException
	 */
    public static long writeProperties(String ext, InputStream in, OutputStreamProvider outProvider, Map<String, String> properties) throws IOException {
        ext = ext.toLowerCase();
        long newLength;
        try {
            if (ext.equals("docx") || ext.equals("pptx") || ext.equals("xlsx")) {
                newLength = writePropertiesInOpenXMLDocument(ext, in, outProvider, properties);
            } else if (ext.equals("doc") || ext.equals("ppt") || ext.equals("xls")) {
                newLength = writePropertiesInOLE2Document(in, outProvider, properties);
            } else if (ext.equals("pdf")) {
                newLength = PDFDocumentPropertiesUtil.writeProperties(in, outProvider, properties);
            } else {
                throw new IllegalArgumentException("Writing properties for a " + ext + " file is not supported");
            }
        } finally {
            in.close();
        }
        return newLength;
    }

    public static long writePropertiesInOpenXMLDocument(String ext, InputStream in, OutputStreamProvider outProvider, Map<String, String> properties) {
        in = new BufferedInputStream(in);
        try {
            File tempPptx = null;
            POIXMLDocument doc;
            if (ext.toLowerCase().equals("docx")) {
                doc = new XWPFDocument(in);
            } else if (ext.toLowerCase().equals("xlsx")) {
                doc = new XSSFWorkbook(in);
            } else if (ext.toLowerCase().equals("pptx")) {
                tempPptx = File.createTempFile("temp", "pptx");
                OutputStream tempPptxOut = new FileOutputStream(tempPptx);
                tempPptxOut = new BufferedOutputStream(tempPptxOut);
                IOUtils.copy(in, tempPptxOut);
                tempPptxOut.close();
                doc = new XSLFSlideShow(tempPptx.getAbsolutePath());
            } else {
                throw new IllegalArgumentException("Writing properties for a " + ext + " file is not supported");
            }
            for (Map.Entry<String, String> property : properties.entrySet()) {
                CoreProperties coreProperties = doc.getProperties().getCoreProperties();
                if (property.getKey().equals(Metadata.TITLE)) {
                    coreProperties.setTitle(property.getValue());
                } else if (property.getKey().equals(Metadata.AUTHOR)) {
                    coreProperties.setCreator(property.getValue());
                } else if (property.getKey().equals(Metadata.KEYWORDS)) {
                    coreProperties.getUnderlyingProperties().setKeywordsProperty(property.getValue());
                } else if (property.getKey().equals(Metadata.COMMENTS)) {
                    coreProperties.setDescription(property.getValue());
                } else if (property.getKey().equals(Metadata.SUBJECT)) {
                    coreProperties.setSubjectProperty(property.getValue());
                } else if (property.getKey().equals(Metadata.COMPANY)) {
                    doc.getProperties().getExtendedProperties().getUnderlyingProperties().setCompany(property.getValue());
                } else {
                    org.apache.poi.POIXMLProperties.CustomProperties customProperties = doc.getProperties().getCustomProperties();
                    if (customProperties.contains(property.getKey())) {
                        int index = 0;
                        for (CTProperty prop : customProperties.getUnderlyingProperties().getPropertyArray()) {
                            if (prop.getName().equals(property.getKey())) {
                                customProperties.getUnderlyingProperties().removeProperty(index);
                                break;
                            }
                            index++;
                        }
                    }
                    customProperties.addProperty(property.getKey(), property.getValue());
                }
            }
            in.close();
            File tempOpenXMLDocumentFile = File.createTempFile("temp", "tmp");
            OutputStream tempOpenXMLDocumentOut = new FileOutputStream(tempOpenXMLDocumentFile);
            tempOpenXMLDocumentOut = new BufferedOutputStream(tempOpenXMLDocumentOut);
            doc.write(tempOpenXMLDocumentOut);
            tempOpenXMLDocumentOut.close();
            long length = tempOpenXMLDocumentFile.length();
            InputStream tempOpenXMLDocumentIn = new FileInputStream(tempOpenXMLDocumentFile);
            tempOpenXMLDocumentIn = new BufferedInputStream(tempOpenXMLDocumentIn);
            OutputStream out = null;
            try {
                out = outProvider.getOutputStream();
                out = new BufferedOutputStream(out);
                IOUtils.copy(tempOpenXMLDocumentIn, out);
                out.flush();
            } finally {
                IOUtils.closeQuietly(out);
            }
            if (!FileUtils.deleteQuietly(tempOpenXMLDocumentFile)) {
                tempOpenXMLDocumentFile.deleteOnExit();
            }
            if (tempPptx != null && !FileUtils.deleteQuietly(tempPptx)) {
                tempPptx.deleteOnExit();
            }
            return length;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        } catch (OpenXML4JException e) {
            throw new RuntimeException(e);
        } catch (XmlException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static long writePropertiesInOLE2Document(InputStream in, OutputStreamProvider outProvider, Map<String, String> properties) throws IOException {
        in = new BufferedInputStream(in);
        POIFSFileSystem poifs;
        try {
            poifs = new POIFSFileSystem(in);
        } catch (IOException e) {
            throw new RuntimeException(EXCEPTION_NON_COMPATIBLE_AVEC_MAC, e);
        } finally {
            in.close();
        }
        DirectoryEntry dir = poifs.getRoot();
        SummaryInformation si = getSummaryInformation(dir);
        DocumentSummaryInformation dsi = getDocumentSummaryInformation(dir);
        if (properties.containsKey(Metadata.TITLE)) {
            String prop = properties.get(Metadata.TITLE);
            si.setTitle(prop == null ? "" : prop);
        }
        if (properties.containsKey(Metadata.AUTHOR)) {
            String prop = properties.get(Metadata.AUTHOR);
            si.setAuthor(prop == null ? "" : prop);
        }
        if (properties.containsKey(Metadata.KEYWORDS)) {
            String prop = properties.get(Metadata.KEYWORDS);
            si.setKeywords(prop == null ? "" : prop);
        }
        if (properties.containsKey(Metadata.COMMENTS)) {
            String prop = properties.get(Metadata.COMMENTS);
            si.setComments(prop == null ? "" : prop);
        }
        if (properties.containsKey(Metadata.SUBJECT)) {
            String prop = properties.get(Metadata.SUBJECT);
            si.setSubject(prop == null ? "" : prop);
        }
        if (properties.containsKey(Metadata.COMPANY)) {
            String prop = properties.get(Metadata.COMPANY);
            dsi.setCompany(prop == null ? "" : prop);
        }
        OutputStream out = outProvider.getOutputStream();
        out = new BufferedOutputStream(out);
        long newLength = -1;
        try {
            si.write(dir, SummaryInformation.DEFAULT_STREAM_NAME);
            dsi.write(dir, DocumentSummaryInformation.DEFAULT_STREAM_NAME);
            poifs.writeFilesystem(out);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            poifs.writeFilesystem(baos);
            newLength = baos.size();
            out.flush();
            baos.close();
        } catch (WritingNotSupportedException e1) {
            throw new RuntimeException(e1);
        } finally {
            out.close();
        }
        return newLength;
    }

    public static long writeIdIGID(File file, String valeur) throws IOException {
        return writeIdIGID(file, file, valeur);
    }

    public static long writeIdIGID(File src, final File dest, String valeur) throws IOException {
        try {
            OutputStreamProvider outputStreamProvider = new OutputStreamProvider() {

                @Override
                public OutputStream getOutputStream() {
                    try {
                        return new FileOutputStream(dest);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            String ext = FilenameUtils.getExtension(src.getName());
            return writeIdIGID(ext, new FileInputStream(src), outputStreamProvider, valeur);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Le document n'est pas accessible.", e);
        }
    }

    public static void writeIdIGID(final FichierElectronique fichier) throws IOException {
        String valeur = "" + fichier.getId();
        OutputStreamProvider outputStreamProvider = new OutputStreamProvider() {

            @Override
            public OutputStream getOutputStream() {
                return fichier.getOutputStream();
            }
        };
        long newLength = writeIdIGID(fichier.getExtension(), fichier.getInputStream(), outputStreamProvider, valeur);
        fichier.setTaille(newLength);
    }

    public static long writeIdIGID(String ext, InputStream in, OutputStreamProvider outProvider, String valeur) throws IOException {
        ext = ext.toLowerCase();
        if ("docx".equals(ext) || "pptx".equals(ext) || "xlsx".equals(ext)) {
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(PROPRIETE_ID_IGID, valeur);
            return writePropertiesInOpenXMLDocument(ext, in, outProvider, properties);
        } else if ("pdf".equals(ext)) {
            return PDFDocumentPropertiesUtil.writeIdIGID(ext, in, outProvider, valeur);
        }
        POIFSFileSystem poifs;
        try {
            poifs = new POIFSFileSystem(in);
        } catch (IOException e) {
            throw new RuntimeException(EXCEPTION_NON_COMPATIBLE_AVEC_MAC, e);
        } finally {
            in.close();
        }
        DirectoryEntry dir = poifs.getRoot();
        DocumentSummaryInformation dsi = getDocumentSummaryInformation(dir);
        CustomProperties customProperties = dsi.getCustomProperties();
        if (customProperties == null) {
            customProperties = new CustomProperties();
        }
        customProperties.put(PROPRIETE_ID_IGID, valeur);
        dsi.setCustomProperties(customProperties);
        OutputStream out = outProvider.getOutputStream();
        long newLength = -1;
        try {
            dsi.write(dir, DocumentSummaryInformation.DEFAULT_STREAM_NAME);
            poifs.writeFilesystem(out);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            poifs.writeFilesystem(baos);
            newLength = baos.size();
            out.flush();
            baos.close();
        } catch (WritingNotSupportedException e1) {
            throw new RuntimeException(e1);
        } finally {
            out.close();
        }
        return newLength;
    }

    public static long removeProperty(String ext, InputStream in, OutputStreamProvider outProvider, String propriete) throws IOException {
        ext = ext.toLowerCase();
        if ("docx".equals(ext) || "pptx".equals(ext) || "xlsx".equals(ext)) {
            return removePropertyInOpenXMLDocument(ext, in, outProvider, propriete);
        } else if ("pdf".equals(ext)) {
            return PDFDocumentPropertiesUtil.removeProperty(in, outProvider, propriete);
        }
        POIFSFileSystem poifs;
        try {
            poifs = new POIFSFileSystem(in);
        } catch (IOException e) {
            throw new RuntimeException(EXCEPTION_NON_COMPATIBLE_AVEC_MAC, e);
        } finally {
            in.close();
        }
        DirectoryEntry dir = poifs.getRoot();
        DocumentSummaryInformation dsi = getDocumentSummaryInformation(dir);
        CustomProperties customProperties = dsi.getCustomProperties();
        if (customProperties == null) {
            customProperties = new CustomProperties();
        }
        customProperties.remove(propriete);
        dsi.setCustomProperties(customProperties);
        OutputStream out = outProvider.getOutputStream();
        long newLength = -1;
        try {
            dsi.write(dir, DocumentSummaryInformation.DEFAULT_STREAM_NAME);
            poifs.writeFilesystem(out);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            poifs.writeFilesystem(baos);
            newLength = baos.size();
            out.flush();
            baos.close();
        } catch (WritingNotSupportedException e1) {
            throw new RuntimeException(e1);
        } finally {
            out.close();
        }
        return newLength;
    }

    public static long removePropertyInOpenXMLDocument(String ext, InputStream in, OutputStreamProvider outProvider, String propriete) {
        in = new BufferedInputStream(in);
        try {
            File tempPptx = null;
            POIXMLDocument doc;
            if (ext.toLowerCase().equals("docx")) {
                doc = new XWPFDocument(in);
            } else if (ext.toLowerCase().equals("xlsx")) {
                doc = new XSSFWorkbook(in);
            } else if (ext.toLowerCase().equals("pptx")) {
                tempPptx = File.createTempFile("temp", "pptx");
                OutputStream tempPptxOut = new FileOutputStream(tempPptx);
                tempPptxOut = new BufferedOutputStream(tempPptxOut);
                IOUtils.copy(in, tempPptxOut);
                tempPptxOut.close();
                doc = new XSLFSlideShow(tempPptx.getAbsolutePath());
            } else {
                throw new IllegalArgumentException("Writing properties for a " + ext + " file is not supported");
            }
            CoreProperties coreProperties = doc.getProperties().getCoreProperties();
            if (propriete.equals(Metadata.TITLE)) {
                coreProperties.setTitle("");
            } else if (propriete.equals(Metadata.AUTHOR)) {
                coreProperties.setCreator("");
            } else if (propriete.equals(Metadata.KEYWORDS)) {
                coreProperties.getUnderlyingProperties().setKeywordsProperty("");
            } else if (propriete.equals(Metadata.COMMENTS)) {
                coreProperties.setDescription("");
            } else if (propriete.equals(Metadata.SUBJECT)) {
                coreProperties.setSubjectProperty("");
            } else if (propriete.equals(Metadata.COMPANY)) {
                doc.getProperties().getExtendedProperties().getUnderlyingProperties().setCompany("");
            } else {
                org.apache.poi.POIXMLProperties.CustomProperties customProperties = doc.getProperties().getCustomProperties();
                if (customProperties.contains(propriete)) {
                    int index = 0;
                    for (CTProperty prop : customProperties.getUnderlyingProperties().getPropertyArray()) {
                        if (prop.getName().equals(propriete)) {
                            customProperties.getUnderlyingProperties().removeProperty(index);
                            break;
                        }
                        index++;
                    }
                }
            }
            in.close();
            File tempOpenXMLDocumentFile = File.createTempFile("temp", "tmp");
            OutputStream tempOpenXMLDocumentOut = new FileOutputStream(tempOpenXMLDocumentFile);
            tempOpenXMLDocumentOut = new BufferedOutputStream(tempOpenXMLDocumentOut);
            doc.write(tempOpenXMLDocumentOut);
            tempOpenXMLDocumentOut.close();
            long length = tempOpenXMLDocumentFile.length();
            InputStream tempOpenXMLDocumentIn = new FileInputStream(tempOpenXMLDocumentFile);
            tempOpenXMLDocumentIn = new BufferedInputStream(tempOpenXMLDocumentIn);
            OutputStream out = null;
            try {
                out = outProvider.getOutputStream();
                out = new BufferedOutputStream(out);
                IOUtils.copy(tempOpenXMLDocumentIn, out);
                out.flush();
            } finally {
                IOUtils.closeQuietly(out);
            }
            if (!FileUtils.deleteQuietly(tempOpenXMLDocumentFile)) {
                tempOpenXMLDocumentFile.deleteOnExit();
            }
            if (tempPptx != null && !FileUtils.deleteQuietly(tempPptx)) {
                tempPptx.deleteOnExit();
            }
            return length;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        } catch (OpenXML4JException e) {
            throw new RuntimeException(e);
        } catch (XmlException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String argv[]) throws Exception {
        String src = "/Users/francisbaril/Skype/intelligid 2011-02-15.docx";
        String dest = "/Users/francisbaril/Skype/intelligid 2011-02-15_new.docx";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(Metadata.AUTHOR, "test-author");
        properties.put(Metadata.TITLE, "test-title");
        properties.put(Metadata.COMMENTS, "test-comments");
        properties.put(Metadata.KEYWORDS, "test-keywords");
        properties.put(Metadata.SUBJECT, "test-subject");
        properties.put(Metadata.COMPANY, "test-company");
        properties.put("a", "4");
        properties.put("id_igid", "5");
        properties.put("v", "6");
        System.out.println("new size is " + writeProperties(new File(src), new File(dest), properties));
        System.out.println("ID is " + getLongProperty("docx", new FileInputStream(dest), "id_igid"));
    }
}
