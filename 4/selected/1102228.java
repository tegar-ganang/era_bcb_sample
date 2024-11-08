package org.ascape.model;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.ascape.explorer.ModelApplet;
import org.ascape.explorer.RuntimeEnvironment;
import org.ascape.explorer.UserEnvironment;
import org.ascape.model.event.ControlEvent;
import org.ascape.model.event.DefaultScapeListener;
import org.ascape.model.event.ScapeEvent;
import org.ascape.model.rule.NotifyViews;
import org.ascape.model.space.SpatialTemporalException;
import org.ascape.util.data.DataGroup;

/**
 * A class that delegates model concerns.
 * 
 * @author Miles Parker
 * @since June 14, 2002
 * @version 3.0
 * @history June 14 first in
 */
public class ModelRoot implements Serializable, Runnable {

    private Scape scape;

    /**
     * Data group for all scapes. At some point this may be made non-static.
     */
    private DataGroup dataGroup;

    /**
     * Manages user space UI, settings etc. Only one exists per vm, so its
     * appropriatly static.
     */
    private static transient RuntimeEnvironment environment;

    /**
     * The unit of time each iteration or period represents.
     */
    private String periodName = "Iteration";

    /**
     * A brief descripiton (including credits) of the scape or of the model, if
     * this is root scape. Plaintext.
     */
    private String description;

    /**
     * A brief descripiton (including credits) of the scape or of the model, if
     * this is root scape. Includes HTML style tags as appropriate.
     */
    private String HTMLDescription;

    /**
     * Iteration to start on when restarting, creating new model, etc...
     */
    private int startPeriod = 0;

    /**
     * Should the scape be started automatically upoin openning it? Default
     * true.
     */
    private boolean startOnOpen = true;

    /**
     * Iteration to stop on.
     */
    private int stopPeriod = Integer.MAX_VALUE;

    /**
     * Period to pause on.
     */
    private int pausePeriod = Integer.MAX_VALUE;

    /**
     * The system path in which all files are by default stored to and retrieved
     * from. The value of the system variable ascape home.
     */
    private String home;

    /**
     * The earliest period this scape is expected to be run at.
     */
    private int earliestPeriod;

    /**
     * The latest period this scape is expected to be run at.
     */
    private int latestPeriod = Integer.MAX_VALUE;

    private List restartingViews = new Vector();

    /**
     * The number of iterations since the scape began iterating.
     */
    private int iteration;

    /**
     * The current period.
     */
    private int period;

    /**
     * Is the scape currently paused?
     */
    private boolean paused = false;

    /**
     * Is the scape currently running?
     */
    private boolean running = false;

    /**
     * Has a step been requested?
     */
    private boolean step = false;

    /**
     * Has a restart been requested after the current run stops?
     */
    private boolean closeAndOpenNewRequested = false;

    /**
     * Has loading of a saved run been requested after the current run stops?
     */
    private boolean closeAndOpenSavedRequested = false;

    /**
     * Has a restart been requested after the current run stops?
     */
    private boolean restartRequested = false;

    /**
     * Has a close been requested after the current run stops?
     */
    private boolean closeRequested = false;

    /**
     * Has a quit been requested after the current run stops?
     */
    private boolean quitRequested = false;

    /**
     * Has an open been requested when the current iteration completes?
     */
    private boolean openRequested = false;

    /**
     * Has a save been requested when the current iteration completes?
     */
    private boolean saveRequested = false;

    /**
     * Are we currently in the main control loop?
     */
    private boolean inMainLoop = false;

    private boolean beginningDeserializedRun = false;

    /**
     * Should the scape be restarted automatically after being stopped?
     */
    private boolean autoRestart = true;

    /**
     * Indicates that GUI should be displayed, if false, not GUI under any
     * circumstances.
     */
    private static boolean displayGraphics = true;

    /**
     * Indicates that we are forwarding graphics to a client scape.
     */
    private static boolean serveGraphics = false;

    /**
     * Indicates that we are in a multiwin environment and want simple winsow
     * strucutures.
     */
    private static boolean muiltWinEnvironment;

    public ModelRoot(Scape scape) {
        this.scape = scape;
        dataGroup = new DataGroup();
        dataGroup.setScape(scape);
    }

    protected void initialize() {
        setInternalRunning(false);
        getData().clear();
        scape.reseed();
        scape.execute(new NotifyViews(ScapeEvent.REQUEST_SETUP));
        waitForViewsUpdate();
        setIteration(0);
        setPeriod(getStartPeriod());
        scape.execute(Scape.INITIALIZE_RULE);
        scape.execute(new NotifyViews(ScapeEvent.REPORT_INITIALIZED));
        waitForViewsUpdate();
        scape.execute(Scape.INITIAL_RULES_RULE);
        setInternalRunning(true);
    }

    /**
     * The main run loop of a running simulation. Seperated from run so that it
     * can be executed in different runtime modes.
     */
    protected void runMainLoop() {
        inMainLoop = true;
        if (beginningDeserializedRun) {
            beginningDeserializedRun = false;
            saveRequested = false;
            initialize();
            scape.executeOnRoot(new NotifyViews(ScapeEvent.REPORT_DESERIALIZED));
            waitForViewsUpdate();
            scape.reseed();
            System.out.println("\nNew Random Seed: " + scape.getRandomSeed() + "\n");
        } else {
            scape.executeOnRoot(Scape.CLEAR_STATS_RULE);
            initialize();
            scape.executeOnRoot(Scape.COLLECT_STATS_RULE);
            scape.executeOnRoot(new NotifyViews(ScapeEvent.REPORT_START));
            waitForViewsUpdate();
        }
        while (running) {
            if (scape.isListenersAndMembersCurrent() && (!paused || step)) {
                scape.executeOnRoot(Scape.CLEAR_STATS_RULE);
                iteration++;
                period++;
                if (period == getPausePeriod() && !paused) {
                    pause();
                }
                scape.executeOnRoot(Scape.EXECUTE_RULES_RULE);
                scape.executeOnRoot(Scape.COLLECT_STATS_RULE);
                scape.executeOnRoot(new NotifyViews(ScapeEvent.REPORT_ITERATE));
                step = false;
            } else {
                if (paused) {
                    waitForViewsUpdate();
                    scape.executeOnRoot(new NotifyViews(ScapeEvent.TICK));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (period >= getStopPeriod()) {
                waitForViewsUpdate();
                if (!isAutoRestart()) {
                    scape.respondControl(new ControlEvent(this, ControlEvent.REQUEST_STOP));
                } else {
                    scape.respondControl(new ControlEvent(this, ControlEvent.REQUEST_RESTART));
                }
            }
            if (closeAndOpenNewRequested) {
                (new Thread(this, "Ascape Main Execution Loop") {

                    public void run() {
                        closeAndOpenNewFinally(scape);
                    }
                }).start();
                closeAndOpenNewRequested = false;
            }
            if (closeAndOpenSavedRequested) {
                (new Thread(this, "Ascape Main Execution Loop") {

                    public void run() {
                        closeAndOpenSavedFinally(scape);
                    }
                }).start();
                closeAndOpenSavedRequested = false;
            }
            if (saveRequested) {
                waitForViewsUpdate();
                saveChoose();
                saveRequested = false;
            }
            if (openRequested) {
                waitForViewsUpdate();
                openChoose();
                openRequested = false;
            }
        }
        scape.executeOnRoot(new NotifyViews(ScapeEvent.REPORT_STOP));
        waitForViewsUpdate();
        if (restartRequested) {
            restartRequested = false;
            scape.respondControl(new ControlEvent(this, ControlEvent.REQUEST_START));
        }
        if (closeRequested) {
            closeFinally();
            closeRequested = false;
        }
        if (quitRequested) {
            quitFinally();
        }
        inMainLoop = false;
    }

    /**
     * Responds to any control events fired at this scape. Currently reacts to
     * start, stop, pause, resume, step, quit, and restart events, as well as
     * listener update report events. All control events except listener updates
     * are passed up to the root. Any other events trigger an untrapped
     * exception.
     */
    public void respondControl(ControlEvent control) {
        switch(control.getID()) {
            case ControlEvent.REQUEST_CLOSE:
                close();
                break;
            case ControlEvent.REQUEST_OPEN:
                closeAndOpenNew();
                break;
            case ControlEvent.REQUEST_OPEN_SAVED:
                closeAndOpenSaved();
                break;
            case ControlEvent.REQUEST_SAVE:
                save();
                break;
            case ControlEvent.REQUEST_START:
                start();
                break;
            case ControlEvent.REQUEST_STOP:
                stop();
                break;
            case ControlEvent.REQUEST_STEP:
                setStep(true);
                break;
            case ControlEvent.REQUEST_RESTART:
                restart();
                break;
            case ControlEvent.REQUEST_PAUSE:
                pause();
                break;
            case ControlEvent.REQUEST_RESUME:
                resume();
                break;
            case ControlEvent.REQUEST_QUIT:
                quit();
                break;
            default:
                throw new RuntimeException("Unknown control event sent to Agent scape: " + control + " [" + control.getID() + "]");
        }
    }

    /**
     * Blocks until all views of this scape and this scape's members have been
     * updated.
     */
    public void waitForViewsUpdate() {
        while (!scape.isAllViewsUpdated()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Requests the scape to open another model, closing the existing one. Will
     * not occur until the current iteration is complete; use static forms to
     * open concurrently. Always called on root.
     */
    public void closeAndOpenNew() {
        if (running) {
            closeAndOpenNewRequested = true;
        } else {
            closeAndOpenNewFinally(scape);
        }
    }

    /**
     * Requests the scape to open another model, closing the existing one.
     * Always called on root.
     */
    public static void closeAndOpenNewFinally(final Scape oldScape) {
        boolean oldWasPaused = oldScape.isPaused();
        if (!oldWasPaused) {
            oldScape.getModel().pause();
        }
        final String modelName = UserEnvironment.openDialog();
        if (!oldWasPaused) {
            oldScape.getModel().resume();
        }
        if (modelName != null) {
            oldScape.addView(new DefaultScapeListener() {

                public void scapeClosing(ScapeEvent scapeEvent) {
                    open(modelName);
                }
            });
            oldScape.getModel().close();
        }
    }

    /**
     * Requests the scape to open a saved run, closing the existing one. Will
     * not occur until the current iteration is complete; use static forms to
     * open concurrently. Always called on root.
     */
    public void closeAndOpenSaved() {
        if (running) {
            closeAndOpenSavedRequested = true;
        } else {
            (new Thread(this) {

                public void run() {
                    closeAndOpenSavedFinally(scape);
                }
            }).start();
        }
    }

    /**
     * Requests the scape to open a saved run.
     */
    public static void openSavedChoose() {
        closeAndOpenSavedFinally(null);
    }

    /**
     * Requests the scape to open a saved run, closing the existing one. Will
     * not occur until the current iteration is complete; use static forms to
     * open concurrently. Always called on root.
     */
    public static void closeAndOpenSavedFinally(Scape oldScape) {
        boolean exit = false;
        ModelRoot model = null;
        if (oldScape != null) {
            if (!oldScape.isPaused()) {
                oldScape.getModel().pause();
                model = oldScape.getModel();
            }
        }
        while (!exit) {
            JFileChooser chooser = new JFileChooser();
            int option = chooser.showOpenDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                if (chooser.getSelectedFile() != null) {
                    final Scape newScape = openSavedRun(chooser.getSelectedFile());
                    if (newScape != null && oldScape != null) {
                        oldScape.addView(new DefaultScapeListener() {

                            public void scapeClosing(ScapeEvent scapeEvent) {
                                newScape.getModel().start();
                            }
                        });
                        oldScape.getModel().close();
                        exit = true;
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "You must enter a file name or cancel.", "Message", JOptionPane.INFORMATION_MESSAGE);
                    closeAndOpenSavedFinally(oldScape);
                }
            } else {
                exit = true;
            }
        }
    }

    public static Scape openSavedRun(File savedRunFile) {
        Scape newScape = null;
        try {
            InputStream is = new FileInputStream(savedRunFile);
            newScape = openSavedRun(is);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Sorry, could not find the file you specified:\n" + savedRunFile, "Error", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            String msg = "Sorry, couldn't open model because a file exception occured:";
            System.err.println(msg);
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, msg + "\n" + e, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return newScape;
    }

    public static Scape openSavedRun(InputStream is) throws IOException {
        Scape newScape = null;
        GZIPInputStream gis = new GZIPInputStream(is);
        ObjectInputStream ois = new ObjectInputStream(gis);
        try {
            newScape = (Scape) ois.readObject();
            ois.close();
            try {
                newScape.setStartPeriod(newScape.getPeriod() + 1);
            } catch (SpatialTemporalException e) {
                try {
                    newScape.setStartPeriod(newScape.getPeriod());
                } catch (SpatialTemporalException e1) {
                    try {
                        newScape.setStartPeriod(newScape.getPeriod());
                    } catch (SpatialTemporalException e2) {
                        throw new RuntimeException("Internal Error");
                    }
                }
            }
            newScape.getModel().beginningDeserializedRun = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return newScape;
    }

    public static Scape openSavedRun(String fileName, String[] args) {
        Scape newScape = openSavedRun(new File(fileName));
        if (newScape != null) {
            if (args.length > 0) {
                newScape.assignParameters(args, true);
            }
            newScape.getModel().createEnvironment();
            if (newScape.isPaused() && newScape.isStartOnOpen()) {
                newScape.getModel().resume();
            }
            newScape.getModel().runMainLoop();
        }
        return newScape;
    }

    public void createNewModel() {
        createNewModel(null, new String[0]);
    }

    public void createNewModel(ModelApplet applet, String[] args) {
        try {
            if (applet != null) {
                scape.getUserEnvironment().setApplet(applet);
                applet.setScape(scape);
            }
            if (args != null) {
                scape.assignParameters(args, false);
            }
            scape.executeOnRoot(Scape.CREATE_RULE);
            if (args != null) {
                scape.assignParameters(args, true);
            }
            scape.executeOnRoot(Scape.CREATE_VIEW_RULE);
            if (scape.getModel().isStartOnOpen()) {
                start();
            }
        } catch (RuntimeException e) {
            if (scape.getUserEnvironment() != null) {
                scape.getUserEnvironment().showErrorDialog(scape, e);
            } else {
                throw (e);
            }
        }
    }

    /**
     * Open (create) and run the model, just as in the normal open, but block
     * execution. Should be used for testing model run behavior only.
     */
    public void testRun() {
        try {
            scape.executeOnRoot(Scape.CREATE_RULE);
            run();
        } catch (RuntimeException e) {
            if (scape.getUserEnvironment() != null) {
                scape.getUserEnvironment().showErrorDialog(scape, e);
            } else {
                throw (e);
            }
        }
    }

    /**
     * Requests the scape to save itself, providing UI for this purpose. Will
     * not occur until the current iteration is complete. Always called on root.
     */
    public void save() {
        if (scape.isRoot()) {
            saveRequested = true;
        } else {
            save();
        }
    }

    /**
     * The basic execution cycle of a running scape. In normal usage this method
     * is not called directly; use start() instead. You might choose to call
     * this method directly if you want the calling code to block, for instance,
     * in order to test that some behavior occurs. In the current
     * implementation, only the root scape is a running thread; all child scapes
     * are iterated through the root thread. Synchronous, determined,
     * reproducible behavior is expected, let us know if you encounter anything
     * different! The cycle always begins by notifying any observers, giving
     * them a chance to observe initial state. Then, the scape waits for the
     * observers to update. When updated, the simulation iterates the root scape
     * and all child scapes with their rules. Again, the scape waits for the
     * observers to update, and the cycle of iteration and update continues
     * until it is paused or stopped. While paused, tick events will be sent to
     * observers, which typically chose to ignore them.
     */
    public void run() {
        run(false);
    }

    /**
     * The basic execution cycle of a running scape. In normal usage this methos
     * is not called directly; use start() instead. You might choose to call
     * this method directly if you want the calling code to block, for instance,
     * in order to test that some behavior occurs. Also use this version with
     * argument "true" if you want to continue using the same thread for
     * restarts.
     * 
     * @param singlethread
     *            should the run if restarted continue to use the same thread?
     */
    public void run(boolean singlethread) {
        if ((scape.getUserEnvironment() != null) && (scape.getUserEnvironment().getRuntimeMode() == UserEnvironment.RELEASE_RUNTIME_MODE)) {
            try {
                runMainLoop();
            } catch (RuntimeException e) {
                scape.getUserEnvironment().showErrorDialog(scape, e);
            }
        } else {
            runMainLoop();
        }
    }

    /**
     * Requests the scape to start. Note that the scape may not start
     * immeadiatly.
     * 
     * @see #setRunning
     */
    public void start() {
        if (!isRunning()) {
            (new Thread(this, "Ascape Main Execution Loop")).start();
        } else {
            System.out.println("Warning: Tried to start an already running scape.");
        }
    }

    /**
     * Requests the scape to stop. Note that the scape will not actually stop
     * until the current iteration is complete.
     * 
     * @see #setRunning
     */
    public void stop() {
        setInternalRunning(false);
    }

    /**
     * Requests the scape to pause. (Convenience method).
     * 
     * @see #setPaused
     */
    public void pause() {
        setPaused(true);
    }

    /**
     * Requests the scape to resume. (Convenience method).
     * 
     * @see #setPaused
     */
    public void resume() {
        setPaused(false);
    }

    /**
     * Requests the scape to restart.
     */
    public void requestRestart() {
        restartRequested = true;
    }

    /**
     * Stops the scape and requests the scape to restart. (Convenience method).
     * 
     * @see #setRunning
     */
    public void restart() {
        if (running) {
            stop();
            restartRequested = true;
        } else {
            start();
        }
    }

    /**
     * Method neccessary because of amibiguous null values in simpler signature
     * methods.
     */
    private void openImplementation(ModelApplet applet, String[] args, boolean block) {
        try {
            if (applet != null) {
                scape.getUserEnvironment().setApplet(applet);
                applet.setScape(scape);
            }
            if (args != null) {
                scape.assignParameters(args, false);
            }
            createEnvironment();
            scape.executeOnRoot(Scape.CREATE_RULE);
            if (args != null) {
                scape.assignParameters(args, true);
            }
            if (scape.getRuntimeEnvironment() != null) {
                scape.addView(scape.getRuntimeEnvironment());
            }
            scape.executeOnRoot(Scape.CREATE_VIEW_RULE);
            if (scape.getModel().isStartOnOpen()) {
                if (!block) {
                    start();
                } else {
                    run();
                }
            }
        } catch (RuntimeException e) {
            if (scape.getUserEnvironment() != null) {
                scape.getUserEnvironment().showErrorDialog(scape, e);
            } else {
                throw (e);
            }
        }
    }

    /**
     * Creates and runs (if start on open is true) this model scape.
     * 
     * @param applet
     *            the applet if are we in an applet vm context
     * @param args
     *            paramter arguments for the scape
     * @param block
     *            should this call block or run in a new thread?
     */
    public void open(ModelApplet applet, String[] args, boolean block) {
        openImplementation(applet, args, block);
    }

    /**
     * Creates and runs (if start on open is true) this model scape.
     * 
     * @param applet
     *            the applet if are we in an applet vm context
     * @param args
     *            paramter arguments for the scape
     */
    public void open(ModelApplet applet, String[] args) {
        openImplementation(applet, args, false);
    }

    /**
     * Creates and runs (if start on open is true) this model scape.
     * 
     * @param args
     *            paramter arguments for the scape
     * @param block
     *            should this call block or run in a new thread?
     */
    public void open(String[] args, boolean block) {
        openImplementation(null, args, block);
    }

    /**
     * Creates and runs (if start on open is true) this model scape.
     * 
     * @param args
     *            paramter arguments for the scape
     */
    public void open(String[] args) {
        openImplementation(null, args, false);
    }

    /**
     * Creates and runs (if start on open is true) this model scape.
     */
    public void open() {
        openImplementation(null, null, false);
    }

    /**
     * Creates and runs (if start on open is true) this model scape.
     * 
     * @param block
     *            should this call block or run in a new thread?
     */
    public void open(boolean block) {
        openImplementation(null, null, block);
    }

    /**
     * Constructs, creates and runs (if start on open is true) the supplied
     * model.
     * 
     * @param modelName
     *            the fully qualified name of the Java class for the model's
     *            root scape
     * @param applet
     *            the applet if are we in an applet vm context
     * @param args
     *            paramter arguments for the scape
     * @param block
     *            should this call block or run in a new thread?
     */
    public static Scape open(String modelName, ModelApplet applet, String[] args, boolean block) {
        try {
            Class c = Class.forName(modelName);
            try {
                Scape scape = (Scape) c.newInstance();
                scape.setModel(new ModelRoot(scape));
                scape.getModel().open(applet, args, block);
                return scape;
            } catch (InstantiationException e) {
                throw new RuntimeException("An internal Ascape or vm error ocurred: " + e.getMessage());
            } catch (IllegalAccessException e) {
                throw new RuntimeException("An internal Ascape or vm error ocurred: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("The class \"" + modelName + " could not be found.");
        }
    }

    /**
     * Constructs, creates and runs the supplied model.
     * 
     * @param modelName
     *            the fully qualified name of the Java class for the model's
     *            root scape
     * @param args
     *            paramter arguments for the scape
     */
    public static Scape open(String modelName, String[] args) {
        return open(modelName, null, args, false);
    }

    /**
     * Constructs, creates and runs the supplied model.
     * 
     * @param modelName
     *            the fully qualified name of the Java class for the model's
     *            root scape
     * @param args
     *            paramter arguments for the scape
     * @param block
     *            should this call block or run in a new thread?
     */
    public static Scape open(String modelName, String[] args, boolean block) {
        return open(modelName, null, args, block);
    }

    /**
     * Constructs, creates and runs the supplied model.
     * 
     * @param modelName
     *            the fully qualified name of the Java class for the model's
     *            root scape
     * @param applet
     *            the applet if are we in an applet vm context
     */
    public static Scape open(String modelName, ModelApplet applet) {
        return open(modelName, applet, null, false);
    }

    /**
     * Constructs, creates and runs the supplied model.
     * 
     * @param modelName
     *            the fully qualified name of the Java class for the model's
     *            root scape
     */
    public static Scape open(String modelName, boolean block) {
        return open(modelName, null, null, block);
    }

    /**
     * Constructs, creates and runs the supplied model.
     * 
     * @param modelName
     *            the fully qualified name of the Java class for the model's
     *            root scape
     */
    public static Scape open(String modelName) {
        return open(modelName, null, null, false);
    }

    /**
     * Requests the scape to open a model, providing UI for this purpose.
     */
    public static Scape openChoose() {
        return openChoose(null);
    }

    /**
     * Requests the scape to open a model, providing UI for this purpose.
     */
    public static Scape openChoose(String[] args) {
        String modelName = UserEnvironment.openDialog();
        Scape scape = null;
        if (modelName != null) {
            scape = open(modelName, args);
        }
        return scape;
    }

    /**
     * Save the state of the scape to a file.
     */
    public void saveChoose() {
        JFileChooser chooser = null;
        boolean overwrite = false;
        File savedFile;
        while (!overwrite) {
            chooser = new JFileChooser();
            int option = chooser.showSaveDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                savedFile = chooser.getSelectedFile();
            } else {
                return;
            }
            if (savedFile.exists()) {
                int n = JOptionPane.showConfirmDialog(chooser, "Warning - A file already exists by this name!\n" + "Do you want to overwrite it?\n", "Save Confirmation", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    overwrite = true;
                } else if (n == JOptionPane.CANCEL_OPTION) {
                    chooser.cancelSelection();
                    scape.getModel().resume();
                }
            } else {
                overwrite = true;
            }
        }
        if (chooser.getSelectedFile() != null) {
            try {
                scape.save(chooser.getSelectedFile());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Sorry, couldn't save model because an input/output exception occured:\n" + e, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "You must enter a file name or cancel.", "Message", JOptionPane.INFORMATION_MESSAGE);
            saveChoose();
        }
    }

    public void close() {
        if (running) {
            stop();
            closeRequested = true;
        } else {
            closeFinally();
        }
    }

    /**
     * Closes the application; allowing views to close themseleves gracefully.
     * Do not call this method directly unless you want to force close; call
     * <code>close()</code> instead, allowing a running scape to stop
     * gracefully. Override this method if you want to provide any scape related
     * pre-quit finalization or clean-up.
     * 
     * @see #quit
     */
    public void closeFinally() {
        dataGroup = null;
        scape.executeOnRoot(new NotifyViews(ScapeEvent.REQUEST_CLOSE));
        waitForViewsUpdate();
    }

    /**
     * Exits the application; calling stop if running and allowing views to
     * close themseleves gracefully. Override <code>quitFinally</code> if you
     * want to provide any pre-quit finalization or clean-up.
     * 
     * @see #quitFinally
     */
    public void quit() {
        if (inMainLoop) {
            quitRequested = true;
            stop();
        } else if (!quitRequested) {
            quitFinally();
        }
    }

    /**
     * Exits the application; allowing views to close themseleves gracefully. Do
     * not call this method directly unless you want to force quit; call
     * <code>quit()</code> instead, allowing a running scape to stop
     * gracefully. Override this method if you want to provide any scape related
     * pre-quit finalization or clean-up.
     * 
     * @see #quit
     */
    public void quitFinally() {
        closeFinally();
        scape.executeOnRoot(new NotifyViews(ScapeEvent.REQUEST_QUIT));
        waitForViewsUpdate();
        exit();
    }

    /**
     * Final kill. Calls System exit, which appears neccessary for vm even when
     * code has finished.
     */
    public static void exit() {
        try {
            System.exit(0);
        } catch (SecurityException e) {
            System.out.println("Can't quit in this security context. (Scape is probably running in browser or viewer; quit or change that.)");
        }
    }

    /**
     * Creates, initializes and runs the model specified in the argument. To
     * allow the running of a model directly from the command line, you should
     * subclass this method as shown below:
     * 
     * <pre><code><BR>
     *     public MyModel extends Model {
     *         public static void main(String[] args) {
     *             (open(&quot;mypath.MyModel&quot;)).start();
     *         }
     *     }
     * <BR>
     * </pre></code> Otherwise, assuming your classpath is set up correctly, to invoke
     * a model from the command line type:
     * 
     * <pre><code><BR>
     *     java org.ascape.model.Scape mypath.myModel
     * </pre></code>
     * 
     * @param args
     *            at index 0; the name of the subclass of this class to run
     */
    public static void main(String[] args) {
        Scape scape;
        if (args.length > 0 && args[0].indexOf("=") == -1) {
            String[] argsRem = new String[args.length - 1];
            System.arraycopy(args, 1, argsRem, 0, argsRem.length);
            scape = open(args[0], argsRem);
        } else {
            String fileName = null;
            List argsList = new LinkedList(Arrays.asList(args));
            for (ListIterator li = argsList.listIterator(); li.hasNext(); ) {
                String arg = (String) li.next();
                int equalAt = arg.lastIndexOf("=");
                if (equalAt < 1) {
                    System.err.println("Syntax error in command line: " + arg);
                } else {
                    String paramName = arg.substring(0, equalAt);
                    if (paramName.equalsIgnoreCase("SavedRun")) {
                        fileName = arg.substring(equalAt + 1);
                        li.remove();
                    }
                }
            }
            if (fileName != null) {
                scape = openSavedRun(fileName, (String[]) argsList.toArray(new String[0]));
            } else {
                environment = new UserEnvironment();
                UserEnvironment.checkForLicenseAgreement();
                scape = openChoose(args);
                if (scape != null) {
                    scape.getModel().createEnvironment();
                } else {
                    System.exit(0);
                }
            }
        }
    }

    public static void createEnvironment() {
        if (environment == null) {
            environment = UserEnvironment.getDefaultEnvironment();
            if (isDisplayGraphics()) {
                if (!isMultiWinEnvironment()) {
                    if (!(environment instanceof UserEnvironment)) {
                        environment = new UserEnvironment();
                    }
                }
            } else {
                environment = new RuntimeEnvironment();
            }
        }
    }

    public static boolean isDisplayGraphics() {
        try {
            return displayGraphics && !GraphicsEnvironment.isHeadless();
        } catch (HeadlessException e) {
            return false;
        }
    }

    public static void setDisplayGraphics(boolean displayGraphics) {
        ModelRoot.displayGraphics = displayGraphics;
    }

    public static boolean isServeGraphics() {
        return serveGraphics;
    }

    public static void setServeGraphics(boolean serveGraphics) {
        ModelRoot.serveGraphics = serveGraphics;
    }

    public static boolean isMultiWinEnvironment() {
        return muiltWinEnvironment;
    }

    public static void setMultiWinEnvironment(boolean muiltWinEnvironment) {
        ModelRoot.muiltWinEnvironment = muiltWinEnvironment;
    }

    /**
     * Sets the period name for the delegate
     * 
     * @return the periodName
     */
    public String getPeriodName() {
        return periodName;
    }

    /**
     * Sets periodName for the ModelRoot object.
     * 
     * @param periodName
     *            the periodName
     */
    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }

    /**
     * Gets the description for the ModelRoot object.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets description for the ModelRoot object.
     * 
     * @param description
     *            the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns a brief descripiton (including credits) of the scape or of the
     * model, if this is root scape. Plaintext.
     * 
     * @return the description
     */
    public String getHTMLDescription() {
        return HTMLDescription;
    }

    /**
     * Sets an html-formatted description to be used for the model as a whole.
     * 
     * @param HTMLdescription
     *            the description
     */
    public void setHTMLDescription(String HTMLdescription) {
        this.HTMLDescription = HTMLdescription;
    }

    /**
     * Returns the period that this model started.
     * 
     * @return the startPeriod
     */
    public int getStartPeriod() {
        return startPeriod;
    }

    /**
     * Gets the startOnOpen for the ModelRoot object.
     * 
     * @return the startOnOpen
     */
    public boolean isStartOnOpen() {
        return startOnOpen;
    }

    /**
     * Sets startOnOpen for the ModelRoot object.
     * 
     * @param startOnOpen
     *            the startOnOpen
     */
    public void setStartOnOpen(boolean startOnOpen) {
        this.startOnOpen = startOnOpen;
    }

    /**
     * Sets the start period for this scape. The start period is the period this
     * scape is given when a model run is started.
     * 
     * @param startPeriod
     *            the period to begin runs at
     * @throws org.ascape.model.space.SpatialTemporalException
     *             exception
     */
    public void setStartPeriod(int startPeriod) throws SpatialTemporalException {
        if (startPeriod >= earliestPeriod) {
            this.startPeriod = startPeriod;
        } else {
            throw new SpatialTemporalException("Tried to set start period before earliest period");
        }
    }

    /**
     * Returns the period this scape stops running at. By default, the lesser of
     * latest period and integer maximum value (effectively unlimited.)
     * 
     * @return the stopPeriod
     */
    public int getStopPeriod() {
        return stopPeriod;
    }

    /**
     * Sets the stop period for this scape. The stop period is the period that
     * the scape is automatically stopped at. The scape may be automatically set
     * to start agina at start value is the scape is set to restart.
     * 
     * @param stopPeriod
     *            the period the scape will stop at upon reaching
     * @see #setAutoRestart
     * @throws org.ascape.model.space.SpatialTemporalException
     *             exception
     */
    public void setStopPeriod(int stopPeriod) throws SpatialTemporalException {
        if (stopPeriod <= latestPeriod) {
            this.stopPeriod = stopPeriod;
        } else {
            throw new SpatialTemporalException("Tried to set stop period after latest period");
        }
    }

    /**
     * Gets the pausePeriod for the ModelRoot object.
     * 
     * @return the pausePeriod
     */
    public int getPausePeriod() {
        return pausePeriod;
    }

    /**
     * Sets pausePeriod for the ModelRoot object.
     * 
     * @param pausePeriod
     *            the pausePeriod
     */
    public void setPausePeriod(int pausePeriod) {
        this.pausePeriod = pausePeriod;
    }

    /**
     * Gets the earliestPeriod for the ModelRoot object.
     * 
     * @return the earliestPeriod
     */
    public int getEarliestPeriod() {
        return earliestPeriod;
    }

    /**
     * Sets the earliest period this scape is expected to be run at. 0 by
     * default.
     * 
     * @param earliestPeriod
     *            the lowest period value this scape can have
     */
    public void setEarliestPeriod(int earliestPeriod) {
        this.earliestPeriod = earliestPeriod;
        if (startPeriod < earliestPeriod) {
            try {
                setStartPeriod(earliestPeriod);
            } catch (SpatialTemporalException e) {
                throw new RuntimeException("Internal Logic Error");
            }
        }
    }

    /**
     * Gets the latestPeriod for the ModelRoot object.
     * 
     * @return the latestPeriod
     */
    public int getLatestPeriod() {
        return latestPeriod;
    }

    /**
     * Sets the latest period this scape is expected to be run at. Max of
     * integer (effectively unlimited) by default.
     * 
     * @param latestPeriod
     *            the highest period value this scape can have
     */
    public void setLatestPeriod(int latestPeriod) {
        this.latestPeriod = latestPeriod;
        if (stopPeriod > latestPeriod) {
            try {
                setStopPeriod(latestPeriod);
            } catch (SpatialTemporalException e) {
                throw new RuntimeException("Internal Logic Error");
            }
        }
    }

    /**
     * Gets the restartingViews for the ModelRoot object.
     * 
     * @return the restartingViews
     */
    public List getRestartingViews() {
        return restartingViews;
    }

    /**
     * Sets restartingViews for the ModelRoot object.
     * 
     * @param restartingViews
     *            the restartingViews
     */
    public void setRestartingViews(List restartingViews) {
        this.restartingViews = restartingViews;
    }

    /**
     * Gets the AutoRestart for the ModelRoot object.
     * 
     * @return the Restart state
     */
    public boolean isAutoRestart() {
        return autoRestart;
    }

    /**
     * Sets Restart for the ModelRoot object.
     * 
     * @param autoRestart
     *            should the model restart when it ends?
     */
    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }

    /**
     * Is the supplied period a valid period for this scape?
     * 
     * @param period
     *            the period to test
     * @return true if within earliest and latest periods, false otherwise
     */
    public boolean isValidPeriod(int period) {
        return ((period >= earliestPeriod) && (period <= latestPeriod));
    }

    /**
     * Returns the path in which all files should by default be stored to and
     * retrieved from. Nonstatic, so that parameter can automatically be set
     * from command line, but backing variable is static. Default is "./", can
     * be modified by calling setHome or providing an ascape.home java property.
     * (This may change now since it is no longer neccesary.)
     * 
     * @return the home
     */
    public String getHome() {
        if (home == null) {
            home = "./";
            try {
                home = System.getProperty("ascape.home", home);
            } catch (SecurityException e) {
            }
        }
        return home;
    }

    /**
     * Sets the path in which to store all scape related files. Nonstatic, so
     * that parameter can automatically be set from command line, but backing
     * variable is static.
     * 
     * @param home
     *            the home
     */
    public void setHome(String home) {
        this.home = home;
    }

    public static RuntimeEnvironment getEnvironment() {
        return environment;
    }

    public boolean isBeginningDeserializedRun() {
        return beginningDeserializedRun;
    }

    public void setBeginningDeserializedRun(boolean beginningDeserializedRun) {
        this.beginningDeserializedRun = beginningDeserializedRun;
    }

    public boolean isCloseAndOpenNewRequested() {
        return closeAndOpenNewRequested;
    }

    public void setCloseAndOpenNewRequested(boolean closeAndOpenNewRequested) {
        this.closeAndOpenNewRequested = closeAndOpenNewRequested;
    }

    public boolean isCloseAndOpenSavedRequested() {
        return closeAndOpenSavedRequested;
    }

    public void setCloseAndOpenSavedRequested(boolean closeAndOpenSavedRequested) {
        this.closeAndOpenSavedRequested = closeAndOpenSavedRequested;
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    public void setCloseRequested(boolean closeRequested) {
        this.closeRequested = closeRequested;
    }

    public boolean isInMainLoop() {
        return inMainLoop;
    }

    public void setInMainLoop(boolean inMainLoop) {
        this.inMainLoop = inMainLoop;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public boolean isOpenRequested() {
        return openRequested;
    }

    public void setOpenRequested(boolean openRequested) {
        this.openRequested = openRequested;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public boolean isQuitRequested() {
        return quitRequested;
    }

    public void setQuitRequested(boolean quitRequested) {
        this.quitRequested = quitRequested;
    }

    public boolean isRestartRequested() {
        return restartRequested;
    }

    public void setRestartRequested(boolean restartRequested) {
        this.restartRequested = restartRequested;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        if (running) {
            start();
        } else {
            stop();
        }
    }

    public void setInternalRunning(boolean running) {
        this.running = running;
    }

    public boolean isSaveRequested() {
        return saveRequested;
    }

    public void setSaveRequested(boolean saveRequested) {
        this.saveRequested = saveRequested;
    }

    public boolean isStep() {
        return step;
    }

    public void setStep(boolean step) {
        this.step = step;
    }

    public DataGroup getData() {
        return dataGroup;
    }

    public void setScape(Scape scape) {
        this.scape = scape;
    }
}
