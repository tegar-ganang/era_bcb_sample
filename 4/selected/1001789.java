package com.doculibre.intelligid.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.tika.metadata.Metadata;
import com.doculibre.intelligid.entites.ContenuFichierElectronique;
import com.doculibre.intelligid.entites.FichierElectronique;

public class PDFDocumentPropertiesUtil {

    private static Logger LOGGER = Logger.getLogger(OfficeDocumentPropertiesUtil.class);

    public static final String PROPRIETE_ID_IGID;

    static {
        String idOrganisation = FGDSpringUtils.getIdOrganisation();
        if (StringUtils.isEmpty(idOrganisation)) {
            PROPRIETE_ID_IGID = "id_igid";
        } else {
            PROPRIETE_ID_IGID = "id_igid_" + idOrganisation;
        }
    }

    private static final List<String> EXTENSIONS_POUR_ECRITURE_ID_IGID = Arrays.asList(new String[] { "pdf" });

    public static boolean canWriteIdIGID(String extension) {
        return EXTENSIONS_POUR_ECRITURE_ID_IGID.contains(extension);
    }

    public static boolean canWriteProprietes(String extension) {
        return EXTENSIONS_POUR_ECRITURE_ID_IGID.contains(extension);
    }

    public static Long getIdIGID(ContenuFichierElectronique contenuFichier) throws IOException {
        return getLongProperty(contenuFichier.getInputStream(), PROPRIETE_ID_IGID);
    }

    public static Long getLongProperty(InputStream in, String property) throws IOException {
        PDDocument document = null;
        try {
            document = getPDFDocument(in);
            PDDocumentInformation documentInfo = document.getDocumentInformation();
            String value = documentInfo.getCustomMetadataValue(property);
            return value == null ? null : Long.valueOf(value);
        } catch (CryptographyException e) {
            throw new RuntimeException(e);
        } finally {
            if (document != null) {
                document.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }

    public static long writeProperties(InputStream in, OutputStreamProvider outProvider, Map<String, String> properties) throws IOException {
        in = new BufferedInputStream(in);
        PDDocument document = null;
        try {
            document = getPDFDocument(in);
            PDDocumentInformation documentInfo = document.getDocumentInformation();
            for (Map.Entry<String, String> prop : properties.entrySet()) {
                if (prop.getKey().equals(Metadata.TITLE)) {
                    documentInfo.setTitle(prop.getValue() == null ? "" : prop.getValue());
                } else if (prop.getKey().equals(Metadata.AUTHOR)) {
                    documentInfo.setAuthor(prop.getValue() == null ? "" : prop.getValue());
                } else if (prop.getKey().equals(Metadata.KEYWORDS)) {
                    documentInfo.setKeywords(prop.getValue() == null ? "" : prop.getValue());
                } else if (prop.getKey().equals(Metadata.SUBJECT)) {
                    documentInfo.setSubject(prop.getValue() == null ? "" : prop.getValue());
                } else if (prop.getKey().equals(Metadata.COMPANY)) {
                    documentInfo.setProducer(prop.getValue() == null ? "" : prop.getValue());
                } else {
                    documentInfo.setCustomMetadataValue(prop.getKey(), prop.getValue() == null ? "" : prop.getValue());
                }
            }
            return saveAndClosePDFDocument(document, outProvider);
        } catch (CryptographyException e) {
            throw new RuntimeException(e);
        } catch (COSVisitorException e) {
            throw new RuntimeException(e);
        } finally {
            if (document != null) {
                document.close();
            }
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
        }
    }

    public static long removeProperty(InputStream in, OutputStreamProvider outProvider, String propriete) throws IOException {
        in = new BufferedInputStream(in);
        PDDocument document = null;
        try {
            document = getPDFDocument(in);
            PDDocumentInformation documentInfo = document.getDocumentInformation();
            if (propriete.equals(Metadata.TITLE)) {
                documentInfo.setTitle("");
            } else if (propriete.equals(Metadata.AUTHOR)) {
                documentInfo.setAuthor("");
            } else if (propriete.equals(Metadata.KEYWORDS)) {
                documentInfo.setKeywords("");
            } else if (propriete.equals(Metadata.SUBJECT)) {
                documentInfo.setSubject("");
            } else if (propriete.equals(Metadata.COMPANY)) {
                documentInfo.setProducer("");
            } else {
                documentInfo.setCustomMetadataValue(propriete, "");
            }
            return saveAndClosePDFDocument(document, outProvider);
        } catch (CryptographyException e) {
            throw new RuntimeException(e);
        } catch (COSVisitorException e) {
            throw new RuntimeException(e);
        } finally {
            if (document != null) {
                document.close();
            }
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
        }
    }

    private static long saveAndClosePDFDocument(PDDocument document, OutputStreamProvider outProvider) throws IOException, COSVisitorException {
        File tempFile = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            tempFile = File.createTempFile("temp", "pdf");
            OutputStream tempFileOut = new FileOutputStream(tempFile);
            tempFileOut = new BufferedOutputStream(tempFileOut);
            document.save(tempFileOut);
            document.close();
            tempFileOut.close();
            long length = tempFile.length();
            in = new BufferedInputStream(new FileInputStream(tempFile));
            out = new BufferedOutputStream(outProvider.getOutputStream());
            IOUtils.copy(in, out);
            return length;
        } finally {
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
            if (out != null) {
                IOUtils.closeQuietly(out);
            }
            if (tempFile != null && !FileUtils.deleteQuietly(tempFile)) {
                tempFile.deleteOnExit();
            }
        }
    }

    private static PDDocument getPDFDocument(InputStream in) throws IOException, CryptographyException {
        PDDocument document;
        try {
            in = new BufferedInputStream(in);
            PDFParser parser = new PDFParser(in);
            parser.parse();
            document = parser.getPDDocument();
            if (document.isEncrypted()) {
                try {
                    document.decrypt("");
                } catch (InvalidPasswordException e) {
                    LOGGER.warn("Ce fichier PDF est verrouillé (nécessite un mot de passe). Impossible de le lire ou de le modifier");
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
        return document;
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
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PROPRIETE_ID_IGID, valeur);
        return writeProperties(in, outProvider, properties);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filePath = "/Users/francisbaril/Downloads/test-1.pdf";
        String testFilePath = "/Users/francisbaril/Desktop/testpdfbox/test.pdf";
        File file = new File(filePath);
        final File testFile = new File(testFilePath);
        if (testFile.exists()) {
            testFile.delete();
        }
        IOUtils.copy(new FileInputStream(file), new FileOutputStream(testFile));
        System.out.println(getLongProperty(new FileInputStream(testFile), PROPRIETE_ID_IGID));
    }
}
