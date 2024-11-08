package com.loribel.commons.office;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import com.loribel.commons.util.FTools;
import com.loribel.commons.util.GB_DebugTools;
import com.loribel.commons.util.GB_MapTools;
import com.loribel.commons.util.STools;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Tools for PDF.
 * 
 * TODO Classe en test
 * 
 * @author Gregory Borelli
 */
public abstract class GB_PdfTools {

    public static interface NAME extends GB_PdfMetaInfo.NAME {
    }

    ;

    public static void addMeta(File a_fileSrc, File a_fileDest, String a_title, String a_subject, String a_keywords, String a_author, String a_creator) throws DocumentException, IOException {
        Document document = new Document();
        File l_inputFile = a_fileSrc;
        String l_inputPath = l_inputFile.getAbsolutePath();
        File l_outputFile = a_fileDest;
        if (a_fileDest == null) {
            l_outputFile = FTools.replaceIntoPath(a_fileSrc, ".pdf", "-temp.pdf");
        }
        l_outputFile.delete();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(l_outputFile));
        document.open();
        document.addAuthor(a_author);
        document.addCreator(a_creator);
        document.addKeywords(a_keywords);
        document.addTitle(a_title);
        document.addSubject(a_subject);
        PdfReader reader = new PdfReader(l_inputPath);
        int n = reader.getNumberOfPages();
        PdfImportedPage page;
        for (int i = 1; i <= n; i++) {
            page = writer.getImportedPage(reader, i);
            Image instance = Image.getInstance(page);
            document.add(instance);
        }
        document.close();
        reader.close();
        writer.close();
        if (a_fileDest == null) {
            FTools.rename(l_outputFile, a_fileSrc);
        }
    }

    public static String getContent(File a_pdf) throws IOException {
        GB_PdfTextParser l_parsor = new GB_PdfTextParser();
        return l_parsor.pdfToText(a_pdf);
    }

    /**
     * Retourne le meta d'un fichier PDF.
     * 
     * @param a_pdf
     * @param a_url optionnel permet de l'ajouter au meta si pr�sent
     */
    public static GB_PdfMetaInfo getMeta(File a_pdf, String a_url) throws IOException {
        GB_PdfMetaInfoImpl retour = newMetaInner(a_pdf, a_url);
        PdfReader l_reader;
        Map l_map = retour.getMap();
        try {
            l_reader = new PdfReader(a_pdf.getAbsolutePath());
            Map l_mapInfo = l_reader.getInfo();
            l_map.putAll(l_mapInfo);
            l_map.put(NAME.NB_PAGES, new Integer(l_reader.getNumberOfPages()));
            l_map.put(NAME.SIZE, "" + a_pdf.length());
        } catch (Exception ex) {
            retour.setError("Error PDF reader : " + ex.getMessage());
        }
        return retour;
    }

    /**
     * Retourne une String avec key=valeur
     */
    public static String getMetaProperties(GB_PdfMetaInfo a_metaInfo) {
        Map l_map = a_metaInfo.getMap();
        return GB_MapTools.toStringProperties(l_map, true);
    }

    public static void main(String[] args) throws DocumentException, IOException {
        File l_dir = new File("B:/gb-data/usana-data/pdf/test");
        File l_outputFile = null;
        File l_inputFile = new File(l_dir, "100.pdf");
        String l_data = "Test...";
        String l_title = l_data;
        String l_subject = l_data;
        String l_keywords = l_data;
        String l_author = l_data;
        String l_creator = l_data;
        addMeta(l_inputFile, l_outputFile, l_title, l_subject, l_keywords, l_author, l_creator);
    }

    public static void main2(String[] args) {
        File l_dir = new File("C:/MyDocuments/usana/press");
        File pdf = new File(l_dir, "2010-01-20.pdf");
        File sortie = new File(l_dir, "xxx.txt");
        PdfReader pdfr = null;
        int mod = 1;
        if (sortie.isFile()) {
            sortie.delete();
        }
        if (!sortie.isFile()) {
            try {
                sortie.createNewFile();
            } catch (IOException e) {
                test_ecrire(sortie, e.toString(), mod);
            }
        }
        if (pdf.isFile()) {
            try {
                pdfr = new PdfReader(pdf.getAbsolutePath());
            } catch (IOException e) {
                test_ecrire(sortie, "impossible de lire le fichier pdf; erreur : " + e.toString(), mod);
            }
            HashMap infos = pdfr.getInfo();
            test_ecrire(sortie, "nb meta=" + infos.size(), mod);
            test_ecrire(sortie, "nb page=" + pdfr.getNumberOfPages(), mod);
            String result = infos.toString();
            String[] result2 = result.split(",");
            int i;
            for (i = 0; i < result2.length; i++) {
                test_ecrire(sortie, result2[i], mod);
            }
        } else {
            test_ecrire(sortie, "le fichier pdf n'existe pas", mod);
        }
    }

    public static void main3(String[] args) {
        try {
            File l_dir = new File("C:/MyDocuments/usana/press");
            File l_pdf = new File(l_dir, "2010-01-20.pdf");
            GB_PdfMetaInfo l_meta = getMeta(l_pdf, null);
            Map l_map = l_meta.getMap();
            GB_DebugTools.debugMap(GB_PdfTools.class, "Meta", l_map, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main4(String[] args) throws DocumentException, IOException {
        Document document = new Document();
        File l_dir = new File("B:/gb-data/usana-data/pdf/test");
        File l_outputFile = new File(l_dir, "100b.pdf");
        File l_inputFile = new File(l_dir, "100.pdf");
        String l_inputPath = l_inputFile.getAbsolutePath();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(l_outputFile));
        document.open();
        document.addAuthor("USANA Canada - info@AskforYourHealth.com");
        document.addCreator("USANA Canada - info@AskforYourHealth.com");
        document.addKeywords("aa,bb,cc");
        document.addTitle("HealthPak 100");
        document.addSubject("Sujet HealthPak 100");
        PdfReader reader = new PdfReader(l_inputPath);
        int n = reader.getNumberOfPages();
        PdfImportedPage page;
        for (int i = 1; i <= n; i++) {
            page = writer.getImportedPage(reader, i);
            Image instance = Image.getInstance(page);
            document.add(instance);
        }
        document.close();
    }

    /**
     * Creation simple d'une instance de GB_PdfMetaInfo.
     */
    public static GB_PdfMetaInfo newMeta(File a_pdf, String a_url) {
        return newMetaInner(a_pdf, a_url);
    }

    public static GB_PdfMetaInfo newMetaError(File a_pdf, String a_url, String a_error) {
        GB_PdfMetaInfoImpl retour = newMetaInner(a_pdf, a_url);
        retour.setError(a_error);
        return retour;
    }

    /**
     * Creation simple d'une instance de GB_PdfMetaInfo.
     */
    private static GB_PdfMetaInfoImpl newMetaInner(File a_pdf, String a_url) {
        Map l_map = new HashMap();
        if (STools.isNotNull(a_url)) {
            l_map.put(NAME.URL, a_url);
            String l_name = FTools.getName(a_url);
            l_map.put(NAME.NAME, l_name);
        } else {
            String l_name = FTools.getName(a_pdf, false);
            l_map.put(NAME.NAME, l_name);
        }
        GB_PdfMetaInfoImpl retour = new GB_PdfMetaInfoImpl(l_map);
        return retour;
    }

    public static void test_ecrire(File f, String m, int mode) {
        PrintWriter fic = null;
        System.out.println(m);
        FileWriter n = null;
        if (mode == 1) {
            try {
                n = new FileWriter(f, true);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            fic = new PrintWriter(n, true);
            fic.println(m);
            fic.flush();
            fic.close();
        }
    }

    public static String writeContentTxt(File a_filePdf, File a_filePdfTxt, String a_encoding) throws IOException {
        return writeContentTxt(a_filePdf, a_filePdfTxt, null, a_encoding);
    }

    /**
     * Transforme un pdf en text.
     * 
     * @param a_filePdf
     * @param a_filePdfTxt
     * @param a_url si non null, ajoute au fichier Txt : "URL : http://...."
     * @param a_encoding
     * @throws IOException
     */
    public static String writeContentTxt(File a_filePdf, File a_filePdfTxt, String a_url, String a_encoding) throws IOException {
        String l_txt = getContent(a_filePdf);
        l_txt = STools.replaceSpeciaux(l_txt);
        l_txt = STools.replaceSpeciauxHtml(l_txt);
        if (STools.isNull(l_txt)) {
            a_filePdfTxt.delete();
        } else {
            if (!STools.isNull(a_url)) {
                l_txt = "URL=" + a_url + AA.SL + AA.SL + l_txt;
            }
            FTools.writeFile(a_filePdfTxt, l_txt, false, a_encoding);
        }
        return l_txt;
    }
}
