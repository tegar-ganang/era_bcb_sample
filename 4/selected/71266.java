package sun.print;

import java.io.FilePermission;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.Pageable;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import sun.awt.image.ByteInterleavedRaster;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.PrinterStateReasons;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import sun.print.PageableDoc;
import sun.print.ServiceDialog;
import sun.print.SunPrinterJobService;
import sun.print.SunPageSelection;

/**
 * A class which rasterizes a printer job.
 *
 * @author Richard Blanchard
 */
public abstract class RasterPrinterJob extends PrinterJob {

    protected static final int PRINTER = 0;

    protected static final int FILE = 1;

    protected static final int STREAM = 2;

    /**
     * Maximum amount of memory in bytes to use for the
     * buffered image "band". 4Mb is a compromise between
     * limiting the number of bands on hi-res printers and
     * not using too much of the Java heap or causing paging
     * on systems with little RAM.
     */
    private static final int MAX_BAND_SIZE = (1024 * 1024 * 4);

    private static final float DPI = 72.0f;

    /**
     * Useful mainly for debugging, this system property
     * can be used to force the printing code to print
     * using a particular pipeline. The two currently
     * supported values are FORCE_RASTER and FORCE_PDL.
     */
    private static final String FORCE_PIPE_PROP = "sun.java2d.print.pipeline";

    /**
     * When the system property FORCE_PIPE_PROP has this value
     * then each page of a print job will be rendered through
     * the raster pipeline.
     */
    private static final String FORCE_RASTER = "raster";

    /**
     * When the system property FORCE_PIPE_PROP has this value
     * then each page of a print job will be rendered through
     * the PDL pipeline.
     */
    private static final String FORCE_PDL = "pdl";

    /**
     * When the system property SHAPE_TEXT_PROP has this value
     * then text is always rendered as a shape, and no attempt is made
     * to match the font through GDI
     */
    private static final String SHAPE_TEXT_PROP = "sun.java2d.print.shapetext";

    /**
     * values obtained from System properties in static initialiser block
     */
    public static boolean forcePDL = false;

    public static boolean forceRaster = false;

    public static boolean shapeTextProp = false;

    static {
        String forceStr = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction(FORCE_PIPE_PROP));
        if (forceStr != null) {
            if (forceStr.equalsIgnoreCase(FORCE_PDL)) {
                forcePDL = true;
            } else if (forceStr.equalsIgnoreCase(FORCE_RASTER)) {
                forceRaster = true;
            }
        }
        String shapeTextStr = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction(SHAPE_TEXT_PROP));
        if (shapeTextStr != null) {
            shapeTextProp = true;
        }
    }

    /**
     * Used to minimise GC & reallocation of band when printing
     */
    private int cachedBandWidth = 0;

    private int cachedBandHeight = 0;

    private BufferedImage cachedBand = null;

    /**
     * The number of book copies to be printed.
     */
    private int mNumCopies = 1;

    /**
     * Collation effects the order of the pages printed
     * when multiple copies are requested. For two copies
     * of a three page document the page order is:
     *  mCollate true: 1, 2, 3, 1, 2, 3
     *  mCollate false: 1, 1, 2, 2, 3, 3
     */
    private boolean mCollate = false;

    /**
     * The zero based indices of the first and last
     * pages to be printed. If 'mFirstPage' is
     * UNDEFINED_PAGE_NUM then the first page to
     * be printed is page 0. If 'mLastPage' is
     * UNDEFINED_PAGE_NUM then the last page to
     * be printed is the last one in the book.
     */
    private int mFirstPage = Pageable.UNKNOWN_NUMBER_OF_PAGES;

    private int mLastPage = Pageable.UNKNOWN_NUMBER_OF_PAGES;

    /**
     * The previous print stream Paper
     * Used to check if the paper size has changed such that the
     * implementation needs to emit the new paper size information
     * into the print stream.
     * Since we do our own rotation, and the margins aren't relevant,
     * Its strictly the dimensions of the paper that we will check.
     */
    private Paper previousPaper;

    /**
     * The document to be printed. It is initialized to an
     * empty (zero pages) book.
     */
    private Pageable mDocument = new Book();

    /**
     * The name of the job being printed.
     */
    private String mDocName = "Java Printing";

    /**
     * Printing cancellation flags
     */
    private boolean performingPrinting = false;

    private boolean userCancelled = false;

    /**
    * Print to file permission variables.
    */
    private FilePermission printToFilePermission;

    /**
     * List of areas & the graphics state for redrawing
     */
    private ArrayList redrawList = new ArrayList();

    private int copiesAttr;

    private String jobNameAttr;

    private String userNameAttr;

    private PageRanges pageRangesAttr;

    protected Sides sidesAttr;

    protected String destinationAttr;

    protected boolean noJobSheet = false;

    protected int mDestType = RasterPrinterJob.FILE;

    protected String mDestination = "";

    protected boolean collateAttReq = false;

    /**
     * Device rotation flag, if it support 270, this is set to true;
     */
    protected boolean landscapeRotates270 = false;

    /**
     * attributes used by no-args page and print dialog and print method to
     * communicate state
     */
    protected PrintRequestAttributeSet attributes = null;

    /**
     * Class to keep state information for redrawing areas
     * "region" is an area at as a high a resolution as possible.
     * The redrawing code needs to look at sx, sy to calculate the scale
     * to device resolution.
     */
    private class GraphicsState {

        Rectangle2D region;

        Shape theClip;

        AffineTransform theTransform;

        double sx;

        double sy;
    }

    /**
     * Service for this job
     */
    protected PrintService myService;

    public RasterPrinterJob() {
    }

    /**
     * Returns the resolution in dots per inch across the width
     * of the page.
     */
    protected abstract double getXRes();

    /**
     * Returns the resolution in dots per inch down the height
     * of the page.
     */
    protected abstract double getYRes();

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    protected abstract double getPhysicalPrintableX(Paper p);

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    protected abstract double getPhysicalPrintableY(Paper p);

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    protected abstract double getPhysicalPrintableWidth(Paper p);

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    protected abstract double getPhysicalPrintableHeight(Paper p);

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    protected abstract double getPhysicalPageWidth(Paper p);

    /**
     * Must be obtained from the current printer.
     * Value is in device pixels.
     * Not adjusted for orientation of the paper.
     */
    protected abstract double getPhysicalPageHeight(Paper p);

    /**
     * Begin a new page.
     */
    protected abstract void startPage(PageFormat format, Printable painter, int index, boolean paperChanged) throws PrinterException;

    /**
     * End a page.
     */
    protected abstract void endPage(PageFormat format, Printable painter, int index) throws PrinterException;

    /**
     * Prints the contents of the array of ints, 'data'
     * to the current page. The band is placed at the
     * location (x, y) in device coordinates on the
     * page. The width and height of the band is
     * specified by the caller.
     */
    protected abstract void printBand(byte[] data, int x, int y, int width, int height) throws PrinterException;

    /**
      * save graphics state of a PathGraphics for later redrawing
      * of part of page represented by the region in that state
      */
    public void saveState(AffineTransform at, Shape clip, Rectangle2D region, double sx, double sy) {
        GraphicsState gstate = new GraphicsState();
        gstate.theTransform = at;
        gstate.theClip = clip;
        gstate.region = region;
        gstate.sx = sx;
        gstate.sy = sy;
        redrawList.add(gstate);
    }

    protected static PrintService lookupDefaultPrintService() {
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        if (service != null && service.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PAGEABLE) && service.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
            return service;
        } else {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
            if (services.length > 0) {
                return services[0];
            }
        }
        return null;
    }

    /**
     * Returns the service (printer) for this printer job.
     * Implementations of this class which do not support print services
     * may return null;
     * @return the service for this printer job.
     *
     */
    public PrintService getPrintService() {
        if (myService == null) {
            PrintService svc = PrintServiceLookup.lookupDefaultPrintService();
            if (svc != null && svc.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PAGEABLE)) {
                try {
                    setPrintService(svc);
                    myService = svc;
                } catch (PrinterException e) {
                }
            }
            if (myService == null) {
                PrintService[] svcs = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
                if (svcs.length > 0) {
                    try {
                        setPrintService(svcs[0]);
                        myService = svcs[0];
                    } catch (PrinterException e) {
                    }
                }
            }
        }
        return myService;
    }

    /**
     * Associate this PrinterJob with a new PrintService.
     *
     * Throws <code>PrinterException</code> if the specified service
     * cannot support the <code>Pageable</code> and
     * <code>Printable</code> interfaces necessary to support 2D printing.
     * @param a print service which supports 2D printing.
     *
     * @throws PrinterException if the specified service does not support
     * 2D printing or no longer available.
     */
    public void setPrintService(PrintService service) throws PrinterException {
        if (service == null) {
            throw new PrinterException("Service cannot be null");
        } else if (!(service instanceof StreamPrintService) && service.getName() == null) {
            throw new PrinterException("Null PrintService name.");
        } else {
            PrinterState prnState = (PrinterState) service.getAttribute(PrinterState.class);
            if (prnState == PrinterState.STOPPED) {
                PrinterStateReasons prnStateReasons = (PrinterStateReasons) service.getAttribute(PrinterStateReasons.class);
                if ((prnStateReasons != null) && (prnStateReasons.containsKey(PrinterStateReason.SHUTDOWN))) {
                    throw new PrinterException("PrintService is no longer available.");
                }
            }
            if (service.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PAGEABLE) && service.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
                myService = service;
            } else {
                throw new PrinterException("Not a 2D print service: " + service);
            }
        }
    }

    protected void updatePageAttributes(PrintService service, PageFormat page) {
        if (service == null || page == null) {
            return;
        }
        float x = (float) Math.rint((page.getPaper().getWidth() * Size2DSyntax.INCH) / (72.0)) / (float) Size2DSyntax.INCH;
        float y = (float) Math.rint((page.getPaper().getHeight() * Size2DSyntax.INCH) / (72.0)) / (float) Size2DSyntax.INCH;
        Media[] mediaList = (Media[]) service.getSupportedAttributeValues(Media.class, null, null);
        Media media = null;
        try {
            media = CustomMediaSizeName.findMedia(mediaList, x, y, Size2DSyntax.INCH);
        } catch (IllegalArgumentException iae) {
        }
        if ((media == null) || !(service.isAttributeValueSupported(media, null, null))) {
            media = (Media) service.getDefaultAttributeValue(Media.class);
        }
        OrientationRequested orient;
        switch(page.getOrientation()) {
            case PageFormat.LANDSCAPE:
                orient = OrientationRequested.LANDSCAPE;
                break;
            case PageFormat.REVERSE_LANDSCAPE:
                orient = OrientationRequested.REVERSE_LANDSCAPE;
                break;
            default:
                orient = OrientationRequested.PORTRAIT;
        }
        if (attributes == null) {
            attributes = new HashPrintRequestAttributeSet();
        }
        if (media != null) {
            attributes.add(media);
        }
        attributes.add(orient);
        float ix = (float) (page.getPaper().getImageableX() / DPI);
        float iw = (float) (page.getPaper().getImageableWidth() / DPI);
        float iy = (float) (page.getPaper().getImageableY() / DPI);
        float ih = (float) (page.getPaper().getImageableHeight() / DPI);
        if (ix < 0) ix = 0f;
        if (iy < 0) iy = 0f;
        try {
            attributes.add(new MediaPrintableArea(ix, iy, iw, ih, MediaPrintableArea.INCH));
        } catch (IllegalArgumentException iae) {
        }
    }

    /**
     * Display a dialog to the user allowing the modification of a
     * PageFormat instance.
     * The <code>page</code> argument is used to initialize controls
     * in the page setup dialog.
     * If the user cancels the dialog, then the method returns the
     * original <code>page</code> object unmodified.
     * If the user okays the dialog then the method returns a new
     * PageFormat object with the indicated changes.
     * In either case the original <code>page</code> object will
     * not be modified.
     * @param     page    the default PageFormat presented to the user
     *                    for modification
     * @return    the original <code>page</code> object if the dialog
     *            is cancelled, or a new PageFormat object containing
     *            the format indicated by the user if the dialog is
     *            acknowledged
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @since     1.2
     */
    public PageFormat pageDialog(PageFormat page) throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        PrintService service = (PrintService) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

            public Object run() {
                PrintService service = getPrintService();
                if (service == null) {
                    ServiceDialog.showNoPrintService(gc);
                    return null;
                }
                return service;
            }
        });
        if (service == null) {
            return page;
        }
        updatePageAttributes(service, page);
        PageFormat newPage = pageDialog(attributes);
        if (newPage == null) {
            return page;
        } else {
            return newPage;
        }
    }

    /**
     * return a PageFormat corresponding to the updated attributes,
     * or null if the user cancelled the dialog.
     */
    public PageFormat pageDialog(final PrintRequestAttributeSet attributes) throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        int x = bounds.x + bounds.width / 3;
        int y = bounds.y + bounds.height / 3;
        PrintService service = (PrintService) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

            public Object run() {
                PrintService service = getPrintService();
                if (service == null) {
                    ServiceDialog.showNoPrintService(gc);
                    return null;
                }
                return service;
            }
        });
        if (service == null) {
            return null;
        }
        ServiceDialog pageDialog = new ServiceDialog(gc, x, y, service, DocFlavor.SERVICE_FORMATTED.PAGEABLE, attributes, (Frame) null);
        pageDialog.show();
        if (pageDialog.getStatus() == ServiceDialog.APPROVE) {
            PrintRequestAttributeSet newas = pageDialog.getAttributes();
            Class amCategory = SunAlternateMedia.class;
            if (attributes.containsKey(amCategory) && !newas.containsKey(amCategory)) {
                attributes.remove(amCategory);
            }
            attributes.addAll(newas);
            PageFormat page = defaultPage();
            OrientationRequested orient = (OrientationRequested) attributes.get(OrientationRequested.class);
            int pfOrient = PageFormat.PORTRAIT;
            if (orient != null) {
                if (orient == OrientationRequested.REVERSE_LANDSCAPE) {
                    pfOrient = PageFormat.REVERSE_LANDSCAPE;
                } else if (orient == OrientationRequested.LANDSCAPE) {
                    pfOrient = PageFormat.LANDSCAPE;
                }
            }
            page.setOrientation(pfOrient);
            Media media = (Media) attributes.get(Media.class);
            if (media == null) {
                media = (Media) service.getDefaultAttributeValue(Media.class);
            }
            if (!(media instanceof MediaSizeName)) {
                media = MediaSizeName.NA_LETTER;
            }
            MediaSize size = MediaSize.getMediaSizeForName((MediaSizeName) media);
            if (size == null) {
                size = MediaSize.NA.LETTER;
            }
            Paper paper = new Paper();
            float dim[] = size.getSize(1);
            double w = Math.rint((dim[0] * 72.0) / Size2DSyntax.INCH);
            double h = Math.rint((dim[1] * 72.0) / Size2DSyntax.INCH);
            paper.setSize(w, h);
            MediaPrintableArea area = (MediaPrintableArea) attributes.get(MediaPrintableArea.class);
            double ix, iw, iy, ih;
            if (area != null) {
                ix = Math.rint(area.getX(MediaPrintableArea.INCH) * DPI);
                iy = Math.rint(area.getY(MediaPrintableArea.INCH) * DPI);
                iw = Math.rint(area.getWidth(MediaPrintableArea.INCH) * DPI);
                ih = Math.rint(area.getHeight(MediaPrintableArea.INCH) * DPI);
            } else {
                if (w >= 72.0 * 6.0) {
                    ix = 72.0;
                    iw = w - 2 * 72.0;
                } else {
                    ix = w / 6.0;
                    iw = w * 0.75;
                }
                if (h >= 72.0 * 6.0) {
                    iy = 72.0;
                    ih = h - 2 * 72.0;
                } else {
                    iy = h / 6.0;
                    ih = h * 0.75;
                }
            }
            paper.setImageableArea(ix, iy, iw, ih);
            page.setPaper(paper);
            return page;
        } else {
            return null;
        }
    }

    /**
     * Presents the user a dialog for changing properties of the
     * print job interactively.
     * The services browsable here are determined by the type of
     * service currently installed.
     * If the application installed a StreamPrintService on this
     * PrinterJob, only the available StreamPrintService (factories) are
     * browsable.
     *
     * @param attributes to store changed properties.
     * @return false if the user cancels the dialog and true otherwise.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public boolean printDialog(final PrintRequestAttributeSet attributes) throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        DialogTypeSelection dlg = (DialogTypeSelection) attributes.get(DialogTypeSelection.class);
        if (dlg == DialogTypeSelection.NATIVE) {
            this.attributes = attributes;
            try {
                debug_println("calling setAttributes in printDialog");
                setAttributes(attributes);
            } catch (PrinterException e) {
            }
            boolean ret = printDialog();
            this.attributes = attributes;
            return ret;
        }
        final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        PrintService service = (PrintService) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

            public Object run() {
                PrintService service = getPrintService();
                if (service == null) {
                    ServiceDialog.showNoPrintService(gc);
                    return null;
                }
                return service;
            }
        });
        if (service == null) {
            return false;
        }
        PrintService[] services;
        StreamPrintServiceFactory[] spsFactories = null;
        if (service instanceof StreamPrintService) {
            spsFactories = lookupStreamPrintServices(null);
            services = new StreamPrintService[spsFactories.length];
            for (int i = 0; i < spsFactories.length; i++) {
                services[i] = spsFactories[i].getPrintService(null);
            }
        } else {
            services = (PrintService[]) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

                public Object run() {
                    PrintService[] services = PrinterJob.lookupPrintServices();
                    return services;
                }
            });
            if ((services == null) || (services.length == 0)) {
                services = new PrintService[1];
                services[0] = service;
            }
        }
        Rectangle bounds = gc.getBounds();
        int x = bounds.x + bounds.width / 3;
        int y = bounds.y + bounds.height / 3;
        PrintService newService;
        try {
            newService = ServiceUI.printDialog(gc, x, y, services, service, DocFlavor.SERVICE_FORMATTED.PAGEABLE, attributes);
        } catch (IllegalArgumentException iae) {
            newService = ServiceUI.printDialog(gc, x, y, services, services[0], DocFlavor.SERVICE_FORMATTED.PAGEABLE, attributes);
        }
        if (newService == null) {
            return false;
        }
        if (!service.equals(newService)) {
            try {
                setPrintService(newService);
            } catch (PrinterException e) {
                myService = newService;
            }
        }
        return true;
    }

    /**
     * Presents the user a dialog for changing properties of the
     * print job interactively.
     * @returns false if the user cancels the dialog and
     *          true otherwise.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public boolean printDialog() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new Copies(getCopies()));
        attributes.add(new JobName(getJobName(), null));
        boolean doPrint = printDialog(attributes);
        if (doPrint) {
            JobName jobName = (JobName) attributes.get(JobName.class);
            if (jobName != null) {
                setJobName(jobName.getValue());
            }
            Copies copies = (Copies) attributes.get(Copies.class);
            if (copies != null) {
                setCopies(copies.getValue());
            }
            Destination dest = (Destination) attributes.get(Destination.class);
            if (dest != null) {
                try {
                    mDestType = RasterPrinterJob.FILE;
                    mDestination = (new File(dest.getURI())).getPath();
                } catch (Exception e) {
                    mDestination = "out.prn";
                    PrintService ps = getPrintService();
                    if (ps != null) {
                        Destination defaultDest = (Destination) ps.getDefaultAttributeValue(Destination.class);
                        if (defaultDest != null) {
                            mDestination = (new File(defaultDest.getURI())).getPath();
                        }
                    }
                }
            } else {
                mDestType = RasterPrinterJob.PRINTER;
                PrintService ps = getPrintService();
                if (ps != null) {
                    mDestination = ps.getName();
                }
            }
        }
        return doPrint;
    }

    /**
     * The pages in the document to be printed by this PrinterJob
     * are drawn by the Printable object 'painter'. The PageFormat
     * for each page is the default page format.
     * @param Printable Called to render each page of the document.
     */
    public void setPrintable(Printable painter) {
        setPageable(new OpenBook(defaultPage(new PageFormat()), painter));
    }

    /**
     * The pages in the document to be printed by this PrinterJob
     * are drawn by the Printable object 'painter'. The PageFormat
     * of each page is 'format'.
     * @param Printable Called to render each page of the document.
     * @param PageFormat The size and orientation of each page to
     *                   be printed.
     */
    public void setPrintable(Printable painter, PageFormat format) {
        setPageable(new OpenBook(format, painter));
        updatePageAttributes(getPrintService(), format);
    }

    /**
     * The pages in the document to be printed are held by the
     * Pageable instance 'document'. 'document' will be queried
     * for the number of pages as well as the PageFormat and
     * Printable for each page.
     * @param Pageable The document to be printed. It may not be null.
     * @exception NullPointerException the Pageable passed in was null.
     * @see PageFormat
     * @see Printable
     */
    public void setPageable(Pageable document) throws NullPointerException {
        if (document != null) {
            mDocument = document;
        } else {
            throw new NullPointerException();
        }
    }

    protected void initPrinter() {
        return;
    }

    protected boolean isSupportedValue(Attribute attrval, PrintRequestAttributeSet attrset) {
        PrintService ps = getPrintService();
        return (attrval != null && ps != null && ps.isAttributeValueSupported(attrval, DocFlavor.SERVICE_FORMATTED.PAGEABLE, attrset));
    }

    protected void setAttributes(PrintRequestAttributeSet attributes) throws PrinterException {
        setCollated(false);
        sidesAttr = null;
        pageRangesAttr = null;
        copiesAttr = 0;
        jobNameAttr = null;
        userNameAttr = null;
        destinationAttr = null;
        collateAttReq = false;
        PrintService service = getPrintService();
        if (attributes == null || service == null) {
            return;
        }
        boolean fidelity = false;
        Fidelity attrFidelity = (Fidelity) attributes.get(Fidelity.class);
        if (attrFidelity != null && attrFidelity == Fidelity.FIDELITY_TRUE) {
            fidelity = true;
        }
        if (fidelity == true) {
            AttributeSet unsupported = service.getUnsupportedAttributes(DocFlavor.SERVICE_FORMATTED.PAGEABLE, attributes);
            if (unsupported != null) {
                throw new PrinterException("Fidelity cannot be satisfied");
            }
        }
        SheetCollate collateAttr = (SheetCollate) attributes.get(SheetCollate.class);
        if (isSupportedValue(collateAttr, attributes)) {
            setCollated(collateAttr == SheetCollate.COLLATED);
        }
        sidesAttr = (Sides) attributes.get(Sides.class);
        if (!isSupportedValue(sidesAttr, attributes)) {
            sidesAttr = Sides.ONE_SIDED;
        }
        pageRangesAttr = (PageRanges) attributes.get(PageRanges.class);
        if (!isSupportedValue(pageRangesAttr, attributes)) {
            pageRangesAttr = null;
        } else {
            if ((SunPageSelection) attributes.get(SunPageSelection.class) == SunPageSelection.RANGE) {
                int[][] range = pageRangesAttr.getMembers();
                setPageRange(range[0][0] - 1, range[0][1] - 1);
            } else {
                setPageRange(-1, -1);
            }
        }
        Copies copies = (Copies) attributes.get(Copies.class);
        if (isSupportedValue(copies, attributes) || (!fidelity && copies != null)) {
            copiesAttr = copies.getValue();
            setCopies(copiesAttr);
        } else {
            copiesAttr = getCopies();
        }
        Destination destination = (Destination) attributes.get(Destination.class);
        if (isSupportedValue(destination, attributes)) {
            try {
                destinationAttr = "" + new File(destination.getURI().getSchemeSpecificPart());
            } catch (Exception e) {
                Destination defaultDest = (Destination) service.getDefaultAttributeValue(Destination.class);
                if (defaultDest != null) {
                    destinationAttr = "" + new File(defaultDest.getURI().getSchemeSpecificPart());
                }
            }
        }
        JobSheets jobSheets = (JobSheets) attributes.get(JobSheets.class);
        if (jobSheets != null) {
            noJobSheet = jobSheets == JobSheets.NONE;
        }
        JobName jobName = (JobName) attributes.get(JobName.class);
        if (isSupportedValue(jobName, attributes) || (!fidelity && jobName != null)) {
            jobNameAttr = jobName.getValue();
            setJobName(jobNameAttr);
        } else {
            jobNameAttr = getJobName();
        }
        RequestingUserName userName = (RequestingUserName) attributes.get(RequestingUserName.class);
        if (isSupportedValue(userName, attributes) || (!fidelity && userName != null)) {
            userNameAttr = userName.getValue();
        } else {
            try {
                userNameAttr = getUserName();
            } catch (SecurityException e) {
                userNameAttr = "";
            }
        }
        Media media = (Media) attributes.get(Media.class);
        OrientationRequested orientReq = (OrientationRequested) attributes.get(OrientationRequested.class);
        MediaPrintableArea mpa = (MediaPrintableArea) attributes.get(MediaPrintableArea.class);
        if ((orientReq != null || media != null || mpa != null) && getPageable() instanceof OpenBook) {
            Pageable pageable = getPageable();
            Printable printable = pageable.getPrintable(0);
            PageFormat pf = (PageFormat) pageable.getPageFormat(0).clone();
            Paper paper = pf.getPaper();
            if (mpa == null && media != null && service.isAttributeCategorySupported(MediaPrintableArea.class)) {
                Object mpaVals = service.getSupportedAttributeValues(MediaPrintableArea.class, null, attributes);
                if (mpaVals instanceof MediaPrintableArea[] && ((MediaPrintableArea[]) mpaVals).length > 0) {
                    mpa = ((MediaPrintableArea[]) mpaVals)[0];
                }
            }
            if (isSupportedValue(orientReq, attributes) || (!fidelity && orientReq != null)) {
                int orient;
                if (orientReq.equals(OrientationRequested.REVERSE_LANDSCAPE)) {
                    orient = PageFormat.REVERSE_LANDSCAPE;
                } else if (orientReq.equals(OrientationRequested.LANDSCAPE)) {
                    orient = PageFormat.LANDSCAPE;
                } else {
                    orient = PageFormat.PORTRAIT;
                }
                pf.setOrientation(orient);
            }
            if (isSupportedValue(media, attributes) || (!fidelity && media != null)) {
                if (media instanceof MediaSizeName) {
                    MediaSizeName msn = (MediaSizeName) media;
                    MediaSize msz = MediaSize.getMediaSizeForName(msn);
                    if (msz != null) {
                        float paperWid = msz.getX(MediaSize.INCH) * 72.0f;
                        float paperHgt = msz.getY(MediaSize.INCH) * 72.0f;
                        paper.setSize(paperWid, paperHgt);
                        if (mpa == null) {
                            paper.setImageableArea(72.0, 72.0, paperWid - 144.0, paperHgt - 144.0);
                        }
                    }
                }
            }
            if (isSupportedValue(mpa, attributes) || (!fidelity && mpa != null)) {
                float[] printableArea = mpa.getPrintableArea(MediaPrintableArea.INCH);
                for (int i = 0; i < printableArea.length; i++) {
                    printableArea[i] = printableArea[i] * 72.0f;
                }
                paper.setImageableArea(printableArea[0], printableArea[1], printableArea[2], printableArea[3]);
            }
            pf.setPaper(paper);
            pf = validatePage(pf);
            setPrintable(printable, pf);
        } else {
            this.attributes = attributes;
        }
    }

    private void spoolToService(PrintService psvc, PrintRequestAttributeSet attributes) throws PrinterException {
        if (psvc == null) {
            throw new PrinterException("No print service found.");
        }
        DocPrintJob job = psvc.createPrintJob();
        Doc doc = new PageableDoc(getPageable());
        if (attributes == null) {
            attributes = new HashPrintRequestAttributeSet();
        }
        try {
            job.print(doc, attributes);
        } catch (PrintException e) {
            throw new PrinterException(e.toString());
        }
    }

    /**
     * Prints a set of pages.
     * @exception java.awt.print.PrinterException an error in the print system
     *                                          caused the job to be aborted
     * @see java.awt.print.Book
     * @see java.awt.print.Pageable
     * @see java.awt.print.Printable
     */
    public void print() throws PrinterException {
        print(attributes);
    }

    public static boolean debugPrint = false;

    protected void debug_println(String str) {
        if (debugPrint) {
            System.out.println("RasterPrinterJob " + str + " " + this);
        }
    }

    public void print(PrintRequestAttributeSet attributes) throws PrinterException {
        PrintService psvc = getPrintService();
        debug_println("psvc = " + psvc);
        if (psvc == null) {
            throw new PrinterException("No print service found.");
        }
        PrinterState prnState = (PrinterState) psvc.getAttribute(PrinterState.class);
        if (prnState == PrinterState.STOPPED) {
            PrinterStateReasons prnStateReasons = (PrinterStateReasons) psvc.getAttribute(PrinterStateReasons.class);
            if ((prnStateReasons != null) && (prnStateReasons.containsKey(PrinterStateReason.SHUTDOWN))) {
                throw new PrinterException("PrintService is no longer available.");
            }
        }
        if ((PrinterIsAcceptingJobs) (psvc.getAttribute(PrinterIsAcceptingJobs.class)) == PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS) {
            throw new PrinterException("Printer is not accepting job.");
        }
        if ((psvc instanceof SunPrinterJobService) && ((SunPrinterJobService) psvc).usesClass(getClass())) {
            setAttributes(attributes);
            if (destinationAttr != null) {
                File f = new File(destinationAttr);
                try {
                    if (f.createNewFile()) {
                        f.delete();
                    }
                } catch (IOException ioe) {
                    throw new PrinterException("Cannot write to file:" + destinationAttr);
                } catch (SecurityException se) {
                }
                File pFile = f.getParentFile();
                if ((f.exists() && (!f.isFile() || !f.canWrite())) || ((pFile != null) && (!pFile.exists() || (pFile.exists() && !pFile.canWrite())))) {
                    throw new PrinterException("Cannot write to file:" + destinationAttr);
                }
            }
        } else {
            spoolToService(psvc, attributes);
            return;
        }
        initPrinter();
        int numCollatedCopies = getCollatedCopies();
        int numNonCollatedCopies = getNoncollatedCopies();
        debug_println("getCollatedCopies()  " + numCollatedCopies + " getNoncollatedCopies() " + numNonCollatedCopies);
        int numPages = mDocument.getNumberOfPages();
        if (numPages == 0) {
            return;
        }
        int firstPage = getFirstPage();
        int lastPage = getLastPage();
        if (lastPage == Pageable.UNKNOWN_NUMBER_OF_PAGES) {
            int totalPages = mDocument.getNumberOfPages();
            if (totalPages != Pageable.UNKNOWN_NUMBER_OF_PAGES) {
                lastPage = mDocument.getNumberOfPages() - 1;
            }
        }
        try {
            synchronized (this) {
                performingPrinting = true;
                userCancelled = false;
            }
            startDoc();
            if (isCancelled()) {
                cancelDoc();
            }
            boolean rangeIsSelected = true;
            if (attributes != null) {
                SunPageSelection pages = (SunPageSelection) attributes.get(SunPageSelection.class);
                if ((pages != null) && (pages != SunPageSelection.RANGE)) {
                    rangeIsSelected = false;
                }
            }
            debug_println("after startDoc rangeSelected? " + rangeIsSelected + " numNonCollatedCopies " + numNonCollatedCopies);
            for (int collated = 0; collated < numCollatedCopies; collated++) {
                for (int i = firstPage, pageResult = Printable.PAGE_EXISTS; (i <= lastPage || lastPage == Pageable.UNKNOWN_NUMBER_OF_PAGES) && pageResult == Printable.PAGE_EXISTS; i++) {
                    if ((pageRangesAttr != null) && rangeIsSelected) {
                        int nexti = pageRangesAttr.next(i);
                        if (nexti == -1) {
                            break;
                        } else if (nexti != i + 1) {
                            continue;
                        }
                    }
                    for (int nonCollated = 0; nonCollated < numNonCollatedCopies && pageResult == Printable.PAGE_EXISTS; nonCollated++) {
                        if (isCancelled()) {
                            cancelDoc();
                        }
                        debug_println("printPage " + i);
                        pageResult = printPage(mDocument, i);
                    }
                }
            }
            if (isCancelled()) {
                cancelDoc();
            }
        } finally {
            previousPaper = null;
            synchronized (this) {
                if (performingPrinting) {
                    endDoc();
                }
                performingPrinting = false;
                notify();
            }
        }
    }

    /**
     * updates a Paper object to reflect the current printer's selected
     * paper size and imageable area for that paper size.
     * Default implementation copies settings from the original, applies
     * applies some validity checks, changes them only if they are
     * clearly unreasonable, then sets them into the new Paper.
     * Subclasses are expected to override this method to make more
     * informed decisons.
     */
    protected void validatePaper(Paper origPaper, Paper newPaper) {
        if (origPaper == null || newPaper == null) {
            return;
        } else {
            double wid = origPaper.getWidth();
            double hgt = origPaper.getHeight();
            double ix = origPaper.getImageableX();
            double iy = origPaper.getImageableY();
            double iw = origPaper.getImageableWidth();
            double ih = origPaper.getImageableHeight();
            Paper defaultPaper = new Paper();
            wid = ((wid > 0.0) ? wid : defaultPaper.getWidth());
            hgt = ((hgt > 0.0) ? hgt : defaultPaper.getHeight());
            ix = ((ix > 0.0) ? ix : defaultPaper.getImageableX());
            iy = ((iy > 0.0) ? iy : defaultPaper.getImageableY());
            iw = ((iw > 0.0) ? iw : defaultPaper.getImageableWidth());
            ih = ((ih > 0.0) ? ih : defaultPaper.getImageableHeight());
            if (iw > wid) {
                iw = wid;
            }
            if (ih > hgt) {
                ih = hgt;
            }
            if ((ix + iw) > wid) {
                ix = wid - iw;
            }
            if ((iy + ih) > hgt) {
                iy = hgt - ih;
            }
            newPaper.setSize(wid, hgt);
            newPaper.setImageableArea(ix, iy, iw, ih);
        }
    }

    /**
     * The passed in PageFormat will be copied and altered to describe
     * the default page size and orientation of the PrinterJob's
     * current printer.
     * Platform subclasses which can access the actual default paper size
     * for a printer may override this method.
     */
    public PageFormat defaultPage(PageFormat page) {
        PageFormat newPage = (PageFormat) page.clone();
        newPage.setOrientation(PageFormat.PORTRAIT);
        Paper newPaper = new Paper();
        double ptsPerInch = 72.0;
        double w, h;
        Media media = null;
        PrintService service = getPrintService();
        if (service != null) {
            MediaSize size;
            media = (Media) service.getDefaultAttributeValue(Media.class);
            if (media instanceof MediaSizeName && ((size = MediaSize.getMediaSizeForName((MediaSizeName) media)) != null)) {
                w = size.getX(MediaSize.INCH) * ptsPerInch;
                h = size.getY(MediaSize.INCH) * ptsPerInch;
                newPaper.setSize(w, h);
                newPaper.setImageableArea(ptsPerInch, ptsPerInch, w - 2.0 * ptsPerInch, h - 2.0 * ptsPerInch);
                newPage.setPaper(newPaper);
                return newPage;
            }
        }
        String defaultCountry = Locale.getDefault().getCountry();
        if (!Locale.getDefault().equals(Locale.ENGLISH) && defaultCountry != null && !defaultCountry.equals(Locale.US.getCountry()) && !defaultCountry.equals(Locale.CANADA.getCountry())) {
            double mmPerInch = 25.4;
            w = Math.rint((210.0 * ptsPerInch) / mmPerInch);
            h = Math.rint((297.0 * ptsPerInch) / mmPerInch);
            newPaper.setSize(w, h);
            newPaper.setImageableArea(ptsPerInch, ptsPerInch, w - 2.0 * ptsPerInch, h - 2.0 * ptsPerInch);
        }
        newPage.setPaper(newPaper);
        return newPage;
    }

    /**
     * The passed in PageFormat is cloned and altered to be usable on
     * the PrinterJob's current printer.
     */
    public PageFormat validatePage(PageFormat page) {
        PageFormat newPage = (PageFormat) page.clone();
        Paper newPaper = new Paper();
        validatePaper(newPage.getPaper(), newPaper);
        newPage.setPaper(newPaper);
        return newPage;
    }

    /**
     * Set the number of copies to be printed.
     */
    public void setCopies(int copies) {
        mNumCopies = copies;
    }

    /**
     * Get the number of copies to be printed.
     */
    public int getCopies() {
        return mNumCopies;
    }

    protected int getCopiesInt() {
        return (copiesAttr > 0) ? copiesAttr : getCopies();
    }

    /**
     * Get the name of the printing user.
     * The caller must have security permission to read system properties.
     */
    public String getUserName() {
        return System.getProperty("user.name");
    }

    protected String getUserNameInt() {
        if (userNameAttr != null) {
            return userNameAttr;
        } else {
            try {
                return getUserName();
            } catch (SecurityException e) {
                return "";
            }
        }
    }

    /**
     * Set the name of the document to be printed.
     * The document name can not be null.
     */
    public void setJobName(String jobName) {
        if (jobName != null) {
            mDocName = jobName;
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Get the name of the document to be printed.
     */
    public String getJobName() {
        return mDocName;
    }

    protected String getJobNameInt() {
        return (jobNameAttr != null) ? jobNameAttr : getJobName();
    }

    /**
     * Set the range of pages from a Book to be printed.
     * Both 'firstPage' and 'lastPage' are zero based
     * page indices. If either parameter is less than
     * zero then the page range is set to be from the
     * first page to the last.
     */
    protected void setPageRange(int firstPage, int lastPage) {
        if (firstPage >= 0 && lastPage >= 0) {
            mFirstPage = firstPage;
            mLastPage = lastPage;
            if (mLastPage < mFirstPage) mLastPage = mFirstPage;
        } else {
            mFirstPage = Pageable.UNKNOWN_NUMBER_OF_PAGES;
            mLastPage = Pageable.UNKNOWN_NUMBER_OF_PAGES;
        }
    }

    /**
     * Return the zero based index of the first page to
     * be printed in this job.
     */
    protected int getFirstPage() {
        return mFirstPage == Book.UNKNOWN_NUMBER_OF_PAGES ? 0 : mFirstPage;
    }

    /**
     * Return the zero based index of the last page to
     * be printed in this job.
     */
    protected int getLastPage() {
        return mLastPage;
    }

    /**
     * Set whether copies should be collated or not.
     * Two collated copies of a three page document
     * print in this order: 1, 2, 3, 1, 2, 3 while
     * uncollated copies print in this order:
     * 1, 1, 2, 2, 3, 3.
     * This is set when request is using an attribute set.
     */
    protected void setCollated(boolean collate) {
        mCollate = collate;
        collateAttReq = true;
    }

    /**
     * Return true if collated copies will be printed as determined
     * in an attribute set.
     */
    protected boolean isCollated() {
        return mCollate;
    }

    /**
     * Called by the print() method at the start of
     * a print job.
     */
    protected abstract void startDoc() throws PrinterException;

    /**
     * Called by the print() method at the end of
     * a print job.
     */
    protected abstract void endDoc() throws PrinterException;

    protected abstract void abortDoc();

    private void cancelDoc() throws PrinterAbortException {
        abortDoc();
        synchronized (this) {
            userCancelled = false;
            performingPrinting = false;
            notify();
        }
        throw new PrinterAbortException();
    }

    /**
     * Returns how many times the entire book should
     * be printed by the PrintJob. If the printer
     * itself supports collation then this method
     * should return 1 indicating that the entire
     * book need only be printed once and the copies
     * will be collated and made in the printer.
     */
    protected int getCollatedCopies() {
        return isCollated() ? getCopiesInt() : 1;
    }

    /**
     * Returns how many times each page in the book
     * should be consecutively printed by PrintJob.
     * If the printer makes copies itself then this
     * method should return 1.
     */
    protected int getNoncollatedCopies() {
        return isCollated() ? 1 : getCopiesInt();
    }

    private int deviceWidth, deviceHeight;

    private AffineTransform defaultDeviceTransform;

    private PrinterGraphicsConfig pgConfig;

    synchronized void setGraphicsConfigInfo(AffineTransform at, double pw, double ph) {
        Point2D.Double pt = new Point2D.Double(pw, ph);
        at.transform(pt, pt);
        if (pgConfig == null || defaultDeviceTransform == null || !at.equals(defaultDeviceTransform) || deviceWidth != (int) pt.getX() || deviceHeight != (int) pt.getY()) {
            deviceWidth = (int) pt.getX();
            deviceHeight = (int) pt.getY();
            defaultDeviceTransform = at;
            pgConfig = null;
        }
    }

    synchronized PrinterGraphicsConfig getPrinterGraphicsConfig() {
        if (pgConfig != null) {
            return pgConfig;
        }
        String deviceID = "Printer Device";
        PrintService service = getPrintService();
        if (service != null) {
            deviceID = service.toString();
        }
        pgConfig = new PrinterGraphicsConfig(deviceID, defaultDeviceTransform, deviceWidth, deviceHeight);
        return pgConfig;
    }

    /**
     * Print a page from the provided document.
     * @return int Printable.PAGE_EXISTS if the page existed and was drawn and
     *             Printable.NO_SUCH_PAGE if the page did not exist.
     * @see java.awt.print.Printable
     */
    protected int printPage(Pageable document, int pageIndex) throws PrinterException {
        PageFormat page;
        PageFormat origPage;
        Printable painter;
        try {
            origPage = document.getPageFormat(pageIndex);
            page = (PageFormat) origPage.clone();
            painter = document.getPrintable(pageIndex);
        } catch (Exception e) {
            PrinterException pe = new PrinterException("Error getting page or printable.[ " + e + " ]");
            pe.initCause(e);
            throw pe;
        }
        Paper paper = page.getPaper();
        if (page.getOrientation() != PageFormat.PORTRAIT && landscapeRotates270) {
            double left = paper.getImageableX();
            double top = paper.getImageableY();
            double width = paper.getImageableWidth();
            double height = paper.getImageableHeight();
            paper.setImageableArea(paper.getWidth() - left - width, paper.getHeight() - top - height, width, height);
            page.setPaper(paper);
            if (page.getOrientation() == PageFormat.LANDSCAPE) {
                page.setOrientation(PageFormat.REVERSE_LANDSCAPE);
            } else {
                page.setOrientation(PageFormat.LANDSCAPE);
            }
        }
        double xScale = getXRes() / 72.0;
        double yScale = getYRes() / 72.0;
        Rectangle2D deviceArea = new Rectangle2D.Double(paper.getImageableX() * xScale, paper.getImageableY() * yScale, paper.getImageableWidth() * xScale, paper.getImageableHeight() * yScale);
        AffineTransform uniformTransform = new AffineTransform();
        AffineTransform scaleTransform = new AffineTransform();
        scaleTransform.scale(xScale, yScale);
        int bandWidth = (int) deviceArea.getWidth();
        if (bandWidth % 4 != 0) {
            bandWidth += (4 - (bandWidth % 4));
        }
        if (bandWidth <= 0) {
            throw new PrinterException("Paper's imageable width is too small.");
        }
        int deviceAreaHeight = (int) deviceArea.getHeight();
        if (deviceAreaHeight <= 0) {
            throw new PrinterException("Paper's imageable height is too small.");
        }
        int bandHeight = (int) (MAX_BAND_SIZE / bandWidth / 3);
        int deviceLeft = (int) Math.rint(paper.getImageableX() * xScale);
        int deviceTop = (int) Math.rint(paper.getImageableY() * yScale);
        AffineTransform deviceTransform = new AffineTransform();
        deviceTransform.translate(-deviceLeft, deviceTop);
        deviceTransform.translate(0, bandHeight);
        deviceTransform.scale(1, -1);
        BufferedImage pBand = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        PeekGraphics peekGraphics = createPeekGraphics(pBand.createGraphics(), this);
        Rectangle2D.Double pageFormatArea = new Rectangle2D.Double(page.getImageableX(), page.getImageableY(), page.getImageableWidth(), page.getImageableHeight());
        peekGraphics.transform(scaleTransform);
        peekGraphics.translate(-getPhysicalPrintableX(paper) / xScale, -getPhysicalPrintableY(paper) / yScale);
        peekGraphics.transform(new AffineTransform(page.getMatrix()));
        initPrinterGraphics(peekGraphics, pageFormatArea);
        AffineTransform pgAt = peekGraphics.getTransform();
        setGraphicsConfigInfo(scaleTransform, paper.getWidth(), paper.getHeight());
        int pageResult = painter.print(peekGraphics, origPage, pageIndex);
        debug_println("pageResult " + pageResult);
        if (pageResult == Printable.PAGE_EXISTS) {
            debug_println("startPage " + pageIndex);
            Paper thisPaper = page.getPaper();
            boolean paperChanged = previousPaper == null || thisPaper.getWidth() != previousPaper.getWidth() || thisPaper.getHeight() != previousPaper.getHeight();
            previousPaper = thisPaper;
            startPage(page, painter, pageIndex, paperChanged);
            Graphics2D pathGraphics = createPathGraphics(peekGraphics, this, painter, page, pageIndex);
            if (pathGraphics != null) {
                pathGraphics.transform(scaleTransform);
                pathGraphics.translate(-getPhysicalPrintableX(paper) / xScale, -getPhysicalPrintableY(paper) / yScale);
                pathGraphics.transform(new AffineTransform(page.getMatrix()));
                initPrinterGraphics(pathGraphics, pageFormatArea);
                redrawList.clear();
                AffineTransform initialTx = pathGraphics.getTransform();
                painter.print(pathGraphics, origPage, pageIndex);
                for (int i = 0; i < redrawList.size(); i++) {
                    GraphicsState gstate = (GraphicsState) redrawList.get(i);
                    pathGraphics.setTransform(initialTx);
                    ((PathGraphics) pathGraphics).redrawRegion(gstate.region, gstate.sx, gstate.sy, gstate.theClip, gstate.theTransform);
                }
            } else {
                BufferedImage band = cachedBand;
                if (cachedBand == null || bandWidth != cachedBandWidth || bandHeight != cachedBandHeight) {
                    band = new BufferedImage(bandWidth, bandHeight, BufferedImage.TYPE_3BYTE_BGR);
                    cachedBand = band;
                    cachedBandWidth = bandWidth;
                    cachedBandHeight = bandHeight;
                }
                Graphics2D bandGraphics = band.createGraphics();
                Rectangle2D.Double clipArea = new Rectangle2D.Double(0, 0, bandWidth, bandHeight);
                initPrinterGraphics(bandGraphics, clipArea);
                ProxyGraphics2D painterGraphics = new ProxyGraphics2D(bandGraphics, this);
                Graphics2D clearGraphics = band.createGraphics();
                clearGraphics.setColor(Color.white);
                ByteInterleavedRaster tile = (ByteInterleavedRaster) band.getRaster();
                byte[] data = tile.getDataStorage();
                int deviceBottom = deviceTop + deviceAreaHeight;
                int deviceAddressableX = (int) getPhysicalPrintableX(paper);
                int deviceAddressableY = (int) getPhysicalPrintableY(paper);
                for (int bandTop = 0; bandTop <= deviceAreaHeight; bandTop += bandHeight) {
                    clearGraphics.fillRect(0, 0, bandWidth, bandHeight);
                    bandGraphics.setTransform(uniformTransform);
                    bandGraphics.transform(deviceTransform);
                    deviceTransform.translate(0, -bandHeight);
                    bandGraphics.transform(scaleTransform);
                    bandGraphics.transform(new AffineTransform(page.getMatrix()));
                    Rectangle clip = bandGraphics.getClipBounds();
                    clip = pgAt.createTransformedShape(clip).getBounds();
                    if ((clip == null) || peekGraphics.hitsDrawingArea(clip) && (bandWidth > 0 && bandHeight > 0)) {
                        int bandX = deviceLeft - deviceAddressableX;
                        if (bandX < 0) {
                            bandGraphics.translate(bandX / xScale, 0);
                            bandX = 0;
                        }
                        int bandY = deviceTop + bandTop - deviceAddressableY;
                        if (bandY < 0) {
                            bandGraphics.translate(0, bandY / yScale);
                            bandY = 0;
                        }
                        painterGraphics.setDelegate((Graphics2D) bandGraphics.create());
                        painter.print(painterGraphics, origPage, pageIndex);
                        painterGraphics.dispose();
                        printBand(data, bandX, bandY, bandWidth, bandHeight);
                    }
                }
                clearGraphics.dispose();
                bandGraphics.dispose();
            }
            debug_println("calling endPage " + pageIndex);
            endPage(page, painter, pageIndex);
        }
        return pageResult;
    }

    /**
     * If a print job is in progress, print() has been
     * called but has not returned, then this signals
     * that the job should be cancelled and the next
     * chance. If there is no print job in progress then
     * this call does nothing.
     */
    public void cancel() {
        synchronized (this) {
            if (performingPrinting) {
                userCancelled = true;
            }
            notify();
        }
    }

    /**
     * Returns true is a print job is ongoing but will
     * be cancelled and the next opportunity. false is
     * returned otherwise.
     */
    public boolean isCancelled() {
        boolean cancelled = false;
        synchronized (this) {
            cancelled = (performingPrinting && userCancelled);
            notify();
        }
        return cancelled;
    }

    /**
     * Return the Pageable describing the pages to be printed.
     */
    protected Pageable getPageable() {
        return mDocument;
    }

    /**
     * Examine the metrics captured by the
     * <code>PeekGraphics</code> instance and
     * if capable of directly converting this
     * print job to the printer's control language
     * or the native OS's graphics primitives, then
     * return a <code>PathGraphics</code> to perform
     * that conversion. If there is not an object
     * capable of the conversion then return
     * <code>null</code>. Returning <code>null</code>
     * causes the print job to be rasterized.
     */
    protected Graphics2D createPathGraphics(PeekGraphics graphics, PrinterJob printerJob, Printable painter, PageFormat pageFormat, int pageIndex) {
        return null;
    }

    /**
     * Create and return an object that will
     * gather and hold metrics about the print
     * job. This method is passed a <code>Graphics2D</code>
     * object that can be used as a proxy for the
     * object gathering the print job matrics. The
     * method is also supplied with the instance
     * controlling the print job, <code>printerJob</code>.
     */
    protected PeekGraphics createPeekGraphics(Graphics2D graphics, PrinterJob printerJob) {
        return new PeekGraphics(graphics, printerJob);
    }

    /**
     * Configure the passed in Graphics2D so that
     * is contains the defined initial settings
     * for a print job. These settings are:
     *      color:  black.
     *      clip:   <as passed in>
     */
    void initPrinterGraphics(Graphics2D g, Rectangle2D clip) {
        g.setClip(clip);
        g.setPaint(Color.black);
    }

    /**
    * User dialogs should disable "File" buttons if this returns false.
    *
    */
    public boolean checkAllowedToPrintToFile() {
        try {
            throwPrintToFile();
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Break this out as it may be useful when we allow API to
     * specify printing to a file. In that case its probably right
     * to throw a SecurityException if the permission is not granted
     */
    private void throwPrintToFile() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            if (printToFilePermission == null) {
                printToFilePermission = new FilePermission("<<ALL FILES>>", "read,write");
            }
            security.checkPermission(printToFilePermission);
        }
    }

    protected String removeControlChars(String s) {
        char[] in_chars = s.toCharArray();
        int len = in_chars.length;
        char[] out_chars = new char[len];
        int pos = 0;
        for (int i = 0; i < len; i++) {
            char c = in_chars[i];
            if (c > '\r' || c < '\t' || c == '' || c == '') {
                out_chars[pos++] = c;
            }
        }
        if (pos == len) {
            return s;
        } else {
            return new String(out_chars, 0, pos);
        }
    }
}
