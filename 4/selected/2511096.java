package org.openXpertya.print.pdf.text.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Instance of PdfReader in each output document.
 *
 * @author Paulo Soares (psoares@consiste.pt)
 */
class PdfReaderInstance {

    static final PdfLiteral IDENTITYMATRIX = new PdfLiteral("[1 0 0 1 0 0]");

    static final PdfNumber ONE = new PdfNumber(1);

    ArrayList xrefObj;

    ArrayList pages;

    int myXref[];

    PdfReader reader;

    RandomAccessFileOrArray file;

    HashMap importedPages = new HashMap();

    PdfWriter writer;

    HashMap visited = new HashMap();

    ArrayList nextRound = new ArrayList();

    PdfReaderInstance(PdfReader reader, PdfWriter writer, ArrayList xrefObj, ArrayList pages) {
        this.reader = reader;
        this.xrefObj = xrefObj;
        this.pages = pages;
        this.writer = writer;
        file = reader.getSafeFile();
        myXref = new int[xrefObj.size()];
    }

    PdfReader getReader() {
        return reader;
    }

    PdfImportedPage getImportedPage(int pageNumber) {
        if (pageNumber < 1 || pageNumber > pages.size()) throw new IllegalArgumentException("Invalid page number");
        Integer i = new Integer(pageNumber);
        PdfImportedPage pageT = (PdfImportedPage) importedPages.get(i);
        if (pageT == null) {
            pageT = new PdfImportedPage(this, writer, pageNumber);
            importedPages.put(i, pageT);
        }
        return pageT;
    }

    int getNewObjectNumber(int number, int generation) {
        if (myXref[number] == 0) {
            myXref[number] = writer.getIndirectReferenceNumber();
            nextRound.add(new Integer(number));
        }
        return myXref[number];
    }

    RandomAccessFileOrArray getReaderFile() {
        return file;
    }

    PdfObject getResources(int pageNumber) {
        return PdfReader.getPdfObject(((PdfDictionary) pages.get(pageNumber - 1)).get(PdfName.RESOURCES));
    }

    PdfStream getFormXObject(int pageNumber) throws IOException {
        PdfDictionary page = (PdfDictionary) pages.get(pageNumber - 1);
        PdfObject contents = PdfReader.getPdfObject(page.get(PdfName.CONTENTS));
        PdfDictionary dic = new PdfDictionary();
        byte bout[] = null;
        ArrayList filters = null;
        if (contents != null) {
            if (contents.isStream()) dic.putAll((PRStream) contents); else bout = reader.getPageContent(pageNumber, file);
        } else bout = new byte[0];
        dic.put(PdfName.RESOURCES, PdfReader.getPdfObject(page.get(PdfName.RESOURCES)));
        dic.put(PdfName.TYPE, PdfName.XOBJECT);
        dic.put(PdfName.SUBTYPE, PdfName.FORM);
        PdfImportedPage impPage = (PdfImportedPage) importedPages.get(new Integer(pageNumber));
        dic.put(PdfName.BBOX, new PdfRectangle(impPage.getBoundingBox()));
        PdfArray matrix = impPage.getMatrix();
        if (matrix == null) dic.put(PdfName.MATRIX, IDENTITYMATRIX); else dic.put(PdfName.MATRIX, matrix);
        dic.put(PdfName.FORMTYPE, ONE);
        PRStream stream;
        if (bout == null) {
            stream = new PRStream((PRStream) contents, dic);
        } else {
            stream = new PRStream(reader, bout);
            stream.putAll(dic);
        }
        return stream;
    }

    void writeAllVisited() throws IOException {
        while (nextRound.size() > 0) {
            ArrayList vec = nextRound;
            nextRound = new ArrayList();
            for (int k = 0; k < vec.size(); ++k) {
                Integer i = (Integer) vec.get(k);
                if (!visited.containsKey(i)) {
                    visited.put(i, null);
                    int n = i.intValue();
                    writer.addToBody((PdfObject) xrefObj.get(n), myXref[n]);
                }
            }
        }
    }

    void writeAllPages() throws IOException {
        try {
            file.reOpen();
            for (Iterator it = importedPages.values().iterator(); it.hasNext(); ) {
                PdfImportedPage ip = (PdfImportedPage) it.next();
                writer.addToBody(ip.getFormXObject(), ip.getIndirectReference());
            }
            writeAllVisited();
        } finally {
            try {
                file.close();
            } catch (Exception e) {
            }
        }
    }
}
