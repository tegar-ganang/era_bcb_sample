package at.fhj.itm.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import at.fhj.itm.beans.SearchTrip.RouteInfo;
import at.fhj.itm.business.ServiceAssembler;
import at.fhj.itm.business.ServiceTrip;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

public class PDFUtil {

    private final Logger logger = Logger.getLogger(PDFUtil.class);

    private ServiceTrip tripService = ServiceAssembler.getInstance().createServiceTrip();

    private static final Color colorBlue = new Color(255, 0, 0);

    private static final Color colorFH = new Color(15, 202, 165);

    private List<RouteInfo> trip;

    private String forUser;

    protected FileInputStream fis;

    Document doc = new Document(PageSize.A4, 50, 50, 50, 50);

    public PDFUtil(List<RouteInfo> trip, String forUser) {
        logger.info("Creating PDFUtil instanze");
        setTrip(trip);
        setForUser(forUser);
    }

    public FileInputStream execute() {
        FacesContext faces = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) faces.getExternalContext().getResponse();
        String pdfPath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/pdf");
        try {
            FileOutputStream outputStream = new FileOutputStream(pdfPath + "/driveTogether.pdf");
            PdfWriter writer = PdfWriter.getInstance(doc, outputStream);
            doc.open();
            String pfad = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/pdf/template.pdf");
            logger.info("Loading PDF-Template: " + pfad);
            PdfReader reader = new PdfReader(pfad);
            PdfImportedPage page = writer.getImportedPage(reader, 1);
            PdfContentByte cb = writer.getDirectContent();
            cb.addTemplate(page, 0, 0);
            doHeader();
            doParagraph(trip, forUser);
            doc.close();
            fis = new FileInputStream(pdfPath + "/driveTogether.pdf");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fis;
    }

    private void doHeader() {
        Paragraph heading = new Paragraph("\n\n\nDriveTogether Statistik", FontFactory.getFont(FontFactory.COURIER_BOLD, 18, new BaseColor(colorFH)));
        Paragraph subHeading = new Paragraph("Routes for: " + getForUser(), FontFactory.getFont(FontFactory.COURIER, 16, new BaseColor(colorBlue)));
        try {
            doc.add(heading);
            doc.add(subHeading);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private void doParagraph(List<RouteInfo> trip, String forUser) {
        for (RouteInfo t : trip) {
            Paragraph someText = new Paragraph("\n########## " + t.getFrom() + " - " + t.getTo() + " ##########" + "\nDepature: " + t.getDepartureDate() + " - " + t.getDepartureTime() + " @ " + t.getArrivalDate() + " - " + t.getArrivalTime() + "\nSeats: " + t.getTrip().getSeats() + "\n\nDriver Info:" + "\nName: " + t.getDriverFullName() + "\nPhone: " + t.getDriverPhone() + "\n########## RouteInfo END ##########");
            try {
                doc.add(someText);
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }
    }

    protected void setTrip(List<RouteInfo> trip) {
        this.trip = trip;
    }

    protected List<RouteInfo> getTrip() {
        return trip;
    }

    protected String getForUser() {
        return forUser;
    }

    protected void setForUser(String user) {
        this.forUser = user;
    }

    protected void setTripService(ServiceTrip tripService) {
        this.tripService = tripService;
    }

    protected ServiceTrip getTripService() {
        return tripService;
    }

    private static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }
}
