package org.ascape.runtime.swing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.ascape.model.Scape;
import org.ascape.model.event.DefaultScapeListener;
import org.ascape.model.event.ScapeEvent;
import org.ascape.runtime.Runner;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Manages model runs in a Swing UI environment.
 * 
 * @author Miles Parker
 * @since June 14, 2002
 * @version 3.0
 * @history June 14 first in
 */
public class SwingRunner extends BasicSwingRunner {

    /**
     * 
     */
    private static final long serialVersionUID = -2041589422471681672L;

    public SwingRunner() {
        super(new DesktopEnvironment());
    }

    /**
     * Creates, initializes and runs the model specified in the argument. To allow the running of a model directly from
     * the command line, you should subclass this method as shown below:
     * 
     * <pre><code><BR>
     *     public MyModel extends Model {
     *         public static void main(String[] args) {
     *             (open(&quot;mypath.MyModel&quot;)).start();
     *         }
     *     }
     * <BR>
     * </pre>
     * 
     * </code> Otherwise, assuming your classpath is set up correctly, to invoke a model from the command line type:
     * 
     * <pre><code><BR>
     *     java org.ascape.model.Scape mypath.myModel
     * </pre>
     * 
     * </code>
     * 
     * @param args
     *        at index 0; the name of the subclass of this class to run
     */
    public static void main(String[] args) {
        Runner runner = new SwingRunner();
        try {
            runner.launch(args);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Exception attempting to load model.", "Error", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void openImplementation(String[] args, boolean block) {
        Runner.assignEnvironmentParameters(args);
        if (isDisplayGraphics() && !(environment instanceof DesktopEnvironment)) {
            environment = new DesktopEnvironment();
        }
        super.openImplementation(args, block);
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
                    getRootScape().getRunner().resume();
                }
            } else {
                overwrite = true;
            }
        }
        if (chooser.getSelectedFile() != null) {
            try {
                getRootScape().save(chooser.getSelectedFile());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Sorry, couldn't save model because an input/output exception occured:\n" + e, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "You must enter a file name or cancel.", "Message", JOptionPane.INFORMATION_MESSAGE);
            saveChoose();
        }
    }

    /**
     * Requests the scape to open a saved run, closing the existing one. Will not occur until the current iteration is
     * complete; use static forms to open concurrently. Always called on root.
     */
    public void closeAndOpenSavedFinally(Scape oldScape) {
        boolean exit = false;
        if (oldScape != null) {
            if (!oldScape.isPaused()) {
                oldScape.getRunner().pause();
            }
        }
        while (!exit) {
            JFileChooser chooser = new JFileChooser();
            int option = chooser.showOpenDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                if (chooser.getSelectedFile() != null) {
                    try {
                        final Scape newScape = openSavedRun(chooser.getSelectedFile());
                        if (newScape != null && oldScape != null) {
                            oldScape.addView(new DefaultScapeListener() {

                                /**
                                 * 
                                 */
                                private static final long serialVersionUID = 3300275064817945877L;

                                public void scapeClosing(ScapeEvent scapeEvent) {
                                    newScape.getRunner().setEnvironment(environment);
                                    newScape.getRunner().start();
                                }
                            });
                            oldScape.getRunner().close();
                            exit = true;
                        }
                    } catch (FileNotFoundException e) {
                        JOptionPane.showMessageDialog(null, "Sorry, could not find the file you specified:\n" + chooser.getSelectedFile(), "Error", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException e) {
                        String msg = "Sorry, couldn't open model because a file exception occured:";
                        System.err.println(msg);
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, msg + "\n" + e, "Error", JOptionPane.ERROR_MESSAGE);
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

    private IOException defaultWriteException;

    public void write(final java.io.ObjectOutputStream out) throws IOException {
        if (SwingUtilities.isEventDispatchThread()) {
            out.defaultWriteObject();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        try {
                            defaultWriteException = null;
                            out.defaultWriteObject();
                        } catch (IOException ex) {
                            defaultWriteException = ex;
                        }
                    }
                });
                if (defaultWriteException != null) {
                    throw defaultWriteException;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (InvocationTargetException ex) {
                Throwable target = ex.getTargetException();
                if (target instanceof RuntimeException) {
                    throw (RuntimeException) target;
                } else if (target instanceof Error) {
                    throw (Error) target;
                }
                ex.printStackTrace();
                throw new RuntimeException(ex.toString());
            }
        }
    }
}
