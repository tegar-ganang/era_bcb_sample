package votebox.middle.driver;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.print.PrintService;
import javax.print.attribute.standard.PrinterName;
import auditorium.IAuditoriumParams;
import sexpression.ASExpression;
import sexpression.ListExpression;
import tap.BallotImageHelper;
import votebox.middle.IBallotVars;
import votebox.middle.Properties;
import votebox.middle.ballot.Ballot;
import votebox.middle.ballot.BallotParser;
import votebox.middle.ballot.BallotParserException;
import votebox.middle.ballot.CardException;
import votebox.middle.ballot.IBallotLookupAdapter;
import votebox.middle.ballot.NonCardException;
import votebox.middle.datacollection.evil.EvilObserver;
import votebox.middle.view.IViewFactory;
import votebox.middle.view.ViewManager;

public class Driver {

    private final String _path;

    private final IViewFactory _factory;

    private ViewManager _view;

    private Ballot _ballot;

    private boolean _encryptionEnabled;

    private List<EvilObserver> _pendingRegisterForCastBallot = new ArrayList<EvilObserver>();

    private List<EvilObserver> _pendingRegisterForReview = new ArrayList<EvilObserver>();

    private IAdapter _viewAdapter = new IAdapter() {

        public boolean deselect(String uid) throws UnknownUIDException, DeselectionException {
            return _view.deselect(uid);
        }

        public Properties getProperties() {
            return _view.getCurrentLayout().getProperties();
        }

        public boolean select(String uid) throws UnknownUIDException, SelectionException {
            return _view.select(uid);
        }
    };

    private IAdapter _ballotAdapter = new IAdapter() {

        public boolean deselect(String uid) throws UnknownUIDException, DeselectionException {
            return _ballot.deselect(uid);
        }

        public Properties getProperties() {
            return _ballot.getProperties();
        }

        public boolean select(String uid) throws UnknownUIDException, SelectionException {
            return _ballot.select(uid);
        }
    };

    private IBallotLookupAdapter _ballotLookupAdapter = new IBallotLookupAdapter() {

        public boolean isCard(String UID) throws UnknownUIDException {
            return _ballot.isCard(UID);
        }

        public String selectedElement(String UID) throws NonCardException, UnknownUIDException, CardException {
            return _ballot.selectedElement(UID);
        }

        public boolean exists(String UID) {
            return _ballot.exists(UID);
        }

        public boolean isSelected(String uid) throws UnknownUIDException {
            return _ballot.isSelected(uid);
        }

        public ASExpression getCastBallot() {
            if (!_encryptionEnabled) return _ballot.toASExpression();
            return _ballot.getCastBallot();
        }

        public int numSelections() {
            return _ballot.getNumSelections();
        }

        public List<List<String>> getRaceGroups() {
            return _ballot.getRaceGroups();
        }

        public Map<String, List<ASExpression>> getAffectedRaces(List<String> affectedUIDs) {
            throw new RuntimeException("Not implemented");
        }

        public List<String> getRaceGroupContaining(List<ASExpression> uids) {
            throw new RuntimeException("Not implemented");
        }
    };

    public Driver(String path, IViewFactory factory, boolean encryptionEnabled) {
        _path = path;
        _factory = factory;
        _encryptionEnabled = encryptionEnabled;
    }

    public void run(Observer reviewScreenObserver, Observer castBallotObserver) {
        IBallotVars vars;
        try {
            vars = new GlobalVarsReader(_path).parse();
        } catch (IOException e) {
            System.err.println("The ballot's configuration file could not be found.");
            e.printStackTrace();
            return;
        }
        try {
            _ballot = new BallotParser().getBallot(vars);
        } catch (BallotParserException e) {
            System.err.println("The ballot's XML file was unable to be parsed.");
            e.printStackTrace();
            return;
        }
        _ballot.setViewAdapter(_viewAdapter);
        _view = new ViewManager(_ballotAdapter, _ballotLookupAdapter, vars, _factory);
        if (castBallotObserver != null) _view.registerForCastBallot(castBallotObserver);
        if (reviewScreenObserver != null) _view.registerForReview(reviewScreenObserver);
        for (EvilObserver o : _pendingRegisterForCastBallot) {
            o.setAdapter(_ballotAdapter, _viewAdapter, _ballot);
            _view.registerForCastBallot(o);
        }
        _pendingRegisterForCastBallot.clear();
        for (EvilObserver o : _pendingRegisterForReview) {
            o.setAdapter(_ballotAdapter, _viewAdapter, _ballot);
            _view.registerForReview(o);
        }
        _pendingRegisterForReview.clear();
        _view.run();
    }

    public void registerForReview(EvilObserver o) {
        _pendingRegisterForReview.add(o);
    }

    public void registerForCastBallot(EvilObserver o) {
        _pendingRegisterForCastBallot.add(o);
    }

    public void run() {
        run(null, null);
    }

    public void kill() {
        _view.dispose();
    }

    /**
     * Gets this VoteBox instance's view.  Used to allow the caller to register for
     * the cast ballot event in the view manager.
     * @return the view manager
     */
    public ViewManager getView() {
        return _view;
    }

    /**
     * @return a reference to the current BallotLookupAdapter
     */
    public IBallotLookupAdapter getBallotAdapter() {
        return _ballotLookupAdapter;
    }

    /**
     * Prints a statement that the ballot has been accepted by the voter on a VVPAT.
     * 
     * @param constants - parameters to use for printing
     * @param currentBallotFile - ballot file to extract images from
     */
    public static void printBallotAccepted(IAuditoriumParams constants, File currentBallotFile) {
        Map<String, Image> choices = BallotImageHelper.loadImagesForVVPAT(currentBallotFile);
        final Image accept = choices.get("accept");
        Printable toPrint = new Printable() {

            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex != 0) return Printable.NO_SUCH_PAGE;
                graphics.drawImage(accept, (int) pageFormat.getImageableX(), (int) pageFormat.getImageableY(), null);
                return Printable.PAGE_EXISTS;
            }
        };
        printOnVVPAT(constants, toPrint);
    }

    /**
     * Prints a statement that the ballot has been rejected by the voter on a VVPAT.
     * 
     * @param constants - parameters to use for printing
     * @param currentBallotFile - ballot file to extract images from
     */
    public static void printBallotRejected(IAuditoriumParams constants, File currentBallotFile) {
        Map<String, Image> choices = BallotImageHelper.loadImagesForVVPAT(currentBallotFile);
        final Image spoil = choices.get("spoil");
        Printable toPrint = new Printable() {

            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex != 0) return Printable.NO_SUCH_PAGE;
                graphics.drawImage(spoil, (int) pageFormat.getImageableX(), (int) pageFormat.getImageableY(), null);
                return Printable.PAGE_EXISTS;
            }
        };
        printOnVVPAT(constants, toPrint);
    }

    /**
     * Prints a ballot out on a VVPAT.
     * 
     * @param constants - parameters to use for printing
     * @param ballot - ballot in the form ((race-id (race-id (... ))))
     * @param currentBallotFile - ballot file to extract images from
     */
    public static void printCommittedBallot(IAuditoriumParams constants, ListExpression ballot, File currentBallotFile) {
        final Map<String, Image> choiceToImage = BallotImageHelper.loadImagesForVVPAT(currentBallotFile);
        if (choiceToImage == null) {
            return;
        }
        final List<String> choices = new ArrayList<String>();
        for (int i = 0; i < ballot.size(); i++) {
            ListExpression choice = (ListExpression) ballot.get(i);
            if (choice.size() != 2) {
                choices.add(choice.get(0).toString());
                continue;
            }
            if (!(choice.get(1) instanceof ListExpression)) {
                choices.add(choice.get(0).toString());
                continue;
            }
            if (((ListExpression) choice.get(1)).size() < 1) {
                choices.add(choice.get(0).toString());
                continue;
            }
            if (((ListExpression) choice.get(1)).get(0).toString().trim().length() > 0) choices.add(((ListExpression) choice.get(1)).get(0).toString());
        }
        int totalSize = 0;
        for (int i = 0; i < choices.size(); i++) totalSize += choiceToImage.get(choices.get(i)).getHeight(null);
        final int fTotalSize = totalSize;
        final List<String> printedChoices = new ArrayList<String>();
        Printable printedBallot = new Printable() {

            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                int numPages = fTotalSize / (int) pageFormat.getImageableHeight();
                if (fTotalSize % (int) pageFormat.getImageableHeight() != 0) numPages++;
                if (printedChoices.size() == choices.size()) return Printable.NO_SUCH_PAGE;
                int choiceIndex = 0;
                int totalSize = 0;
                while (pageIndex != 0) {
                    totalSize += choiceToImage.get(choices.get(choiceIndex)).getHeight(null);
                    if (totalSize > pageFormat.getImageableHeight()) {
                        totalSize = 0;
                        choiceIndex--;
                        pageIndex--;
                    }
                    choiceIndex++;
                }
                totalSize = 0;
                while (totalSize < pageFormat.getImageableHeight() && choiceIndex < choices.size()) {
                    Image img = choiceToImage.get(choices.get(choiceIndex));
                    if (img.getHeight(null) + totalSize > pageFormat.getImageableHeight()) break;
                    printedChoices.add(choices.get(choiceIndex));
                    int x = (int) pageFormat.getImageableX();
                    int y = (int) pageFormat.getImageableY() + totalSize;
                    graphics.drawImage(img, x, y, null);
                    totalSize += img.getHeight(null);
                    choiceIndex++;
                }
                return Printable.PAGE_EXISTS;
            }
        };
        Driver.printOnVVPAT(constants, printedBallot);
    }

    /**
	 * Prints onto the attached VVPAT printer, if possible.
	 * @param print - the Printable to print.
	 */
    public static void printOnVVPAT(final IAuditoriumParams constants, final Printable toPrint) {
        Thread t = new Thread() {

            public void run() {
                if (constants.getPrinterForVVPAT().equals("")) return;
                PrintService[] printers = PrinterJob.lookupPrintServices();
                PrintService vvpat = null;
                for (PrintService printer : printers) {
                    PrinterName name = printer.getAttribute(PrinterName.class);
                    if (name.getValue().equals(constants.getPrinterForVVPAT())) {
                        vvpat = printer;
                        break;
                    }
                }
                if (vvpat == null) {
                    return;
                }
                PrinterJob job = PrinterJob.getPrinterJob();
                try {
                    job.setPrintService(vvpat);
                } catch (PrinterException e) {
                    return;
                }
                Paper paper = new Paper();
                paper.setSize(constants.getPaperWidthForVVPAT(), constants.getPaperHeightForVVPAT());
                int imageableWidth = constants.getPrintableWidthForVVPAT();
                int imageableHeight = constants.getPrintableHeightForVVPAT();
                int leftInset = (constants.getPaperWidthForVVPAT() - constants.getPrintableWidthForVVPAT()) / 2;
                int topInset = (constants.getPaperHeightForVVPAT() - constants.getPrintableHeightForVVPAT()) / 2;
                paper.setImageableArea(leftInset, topInset, imageableWidth, imageableHeight);
                PageFormat pageFormat = new PageFormat();
                pageFormat.setPaper(paper);
                job.setPrintable(toPrint, pageFormat);
                try {
                    job.print();
                } catch (PrinterException e) {
                    return;
                }
            }
        };
        t.start();
    }

    public static void unzip(String src, String dest) throws IOException {
        if (!(new File(dest)).exists()) {
            (new File(dest)).mkdirs();
        }
        ZipFile zipFile = new ZipFile(src);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        byte[] buf = new byte[1024];
        int len;
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                File newDir = new File(dest, entry.getName().replace('/', File.separatorChar));
                newDir.mkdirs();
            }
        }
        entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            } else {
                InputStream in = zipFile.getInputStream(entry);
                File outFile = new File(dest, entry.getName().replace('/', File.separatorChar));
                OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
                while ((len = in.read(buf)) >= 0) out.write(buf, 0, len);
                in.close();
                out.flush();
                out.close();
            }
        }
        zipFile.close();
    }

    public static void deleteRecursivelyOnExit(String dir) {
        Stack<File> dirStack = new Stack<File>();
        dirStack.add(new File(dir));
        while (!dirStack.isEmpty()) {
            File file = dirStack.pop();
            file.deleteOnExit();
            File[] children = file.listFiles();
            for (File f : children) {
                if (f.isDirectory()) dirStack.add(f); else f.deleteOnExit();
            }
            if (dirStack.size() > 100) return;
        }
    }
}
