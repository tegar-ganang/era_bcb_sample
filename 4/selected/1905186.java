package org.epoline.service.services.dossierimage.dummy;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import org.epo.utils.*;
import org.epo.wad.WADException;
import org.epoline.bsi.bps.shared.BPSControlFileIF;
import org.epoline.bsi.bps.shared.BPSPrintException;
import org.epoline.service.services.dossierimage.DocumentAlreadyLoadedException;
import org.epoline.service.services.dossierimage.ImageArchiveException;
import org.epoline.service.services.dossierimage.ImageNotFoundException;
import org.epoline.service.services.dossierimage.dl.ImageDocument;
import org.epoline.service.services.dossierimage.dl.LoadPackage;

public class DummyImageServer {

    public Hashtable images;

    private File root;

    public DummyImageServer(String location) {
        super();
        images = new Hashtable();
        root = new File(location);
        if (root.isDirectory()) {
            File[] packages = root.listFiles();
            for (int i = 0; i < packages.length; i++) {
                String curpck = Utils.leftAllign(17, packages[i].getName());
                File[] pages = packages[i].listFiles();
                ImageDocument doc = new ImageDocument(true);
                doc.setId(curpck);
                doc.setTotalPages(pages.length);
                images.put(curpck, doc);
            }
        }
    }

    public byte[] getPDF(String PXIId, int startPage, int endPage) throws ImageNotFoundException, ImageArchiveException {
        throw new ImageArchiveException("Not Supporrted in DummyMode");
    }

    public PDF_Document getPDF(String PXIId, int startPage, int endPage, String outLocation) throws ImageNotFoundException, ImageArchiveException {
        throw new ImageArchiveException("Not Supporrted in DummyMode");
    }

    public BLOB_Document getBlob(String PXIId, String outLocation) throws ImageNotFoundException, ImageArchiveException {
        throw new ImageArchiveException("Not Supporrted in DummyMode");
    }

    public WIPO_ST33_Document getST33(String PXIId, int startPage, int endPage, String outLocation) throws ImageNotFoundException, ImageArchiveException {
        throw new ImageArchiveException("Not Supporrted in DummyMode");
    }

    public TIFF_Document getTiff(String PXIId, int startPage, int endPage, String outLocation) throws ImageNotFoundException, ImageArchiveException {
        ImageDocument doc = (ImageDocument) images.get(PXIId);
        if (doc == null) throw new ImageNotFoundException("Images not in Dummy" + PXIId);
        int totalPages = doc.getTotalPages();
        if (endPage > totalPages) throw new ImageNotFoundException("ImagePage(s) not in Dummy" + PXIId + "-" + endPage);
        if (startPage < 1) throw new ImageNotFoundException("ImagePage(s) not in Dummy" + PXIId + "-" + startPage);
        TIFF_Document res = new TIFF_Document();
        try {
            for (int i = startPage; i <= endPage; i++) {
                String sourceFile = root.getAbsolutePath() + File.separator + PXIId.trim() + File.separator + Utils.formatString(i, "000000") + ".TIF";
                String targetFile = outLocation + File.separator + Utils.formatString(i, "000000") + ".TIF";
                Utils.writeBinaryFile(targetFile, Utils.readBinaryFile(sourceFile));
                res.addPage(new TIFF_Page(targetFile));
            }
        } catch (IOException e) {
            throw new ImageArchiveException(e.toString());
        } catch (ImageDataException e) {
            throw new ImageArchiveException(e.toString());
        }
        return res;
    }

    public List getPrintJobsForUser(String aName) throws ImageArchiveException {
        throw new ImageArchiveException("Not Supporrted in DummyMode");
    }

    public ImageDocument[] inquire(String[] imgIDs) throws ImageArchiveException {
        throw new ImageArchiveException("Not Supporrted in DummyMode");
    }

    public void load(LoadPackage aPackage, String wadFileName, String tempDir) throws ImageArchiveException, DocumentAlreadyLoadedException, WADException, ImageDataException {
        throw new ImageArchiveException("Not Supporrted in DummyMode");
    }

    public String loadTempDocument(String aText) throws ImageArchiveException {
        throw new ImageArchiveException("Not Supporrted in DummyMode");
    }

    public String print(BPSControlFileIF aControl, boolean withHeader) throws BPSPrintException {
        throw new BPSPrintException("Not Supporrted in DummyMode");
    }
}
