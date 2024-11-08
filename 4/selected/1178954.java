package tit.gui;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import tit.configuration.Configurations;
import tit.gui.border.HorizontalLine;
import tit.gui.icon.Icons;
import tit.gui.layout.VerticalLayout;
import tit.gui.widget.BehaviourDialog;
import tit.gui.widget.ObservationInput;
import tit.gui.widget.ScrollDownScrollPane;
import tit.gui.widget.progressbar.ProgressBar;
import tit.gui.widget.progressbar.TimeValueFormatter;
import tit.observation.ObservationTableModel;
import tit.observation.PresentTimeObservation;
import tit.observation.behaviour.Behaviour;
import tit.observation.io.ObservationDOMWriter;
import tit.summary.AttributeNotSetException;
import tit.summary.Summary;
import tit.summary.SuperSummarizer;
import tit.summary.io.SummaryWriter;
import tit.summary.io.SummaryXMLWriter;
import tit.summary.personality.PersonalityDefaultValues;
import tit.summary.personality.PersonalityEditor;
import tit.utility.Dates;
import tit.utility.Extensions;
import tit.utility.Files;

/**
 * An observation-editor.
 * @author Bart Sas
 */
public class NewObservation extends JInternalFrame implements ActionListener, InternalFrameListener, TableModelListener, ChangeListener, MouseListener {

    /**
     * The identifier for the start-panel.
     */
    private static final String START = "START";

    /**
     * The identifier for the stop-panel.
     */
    private static final String STOP = "STOP";

    /**
     * The identifier for the save-panel.
     */
    private static final String SAVE = "SAVE";

    /**
     * The interval of the progress timer.
     */
    private static final int PROGRESS_INTERVAL = 1000;

    /**
     * The observation.
     */
    private ObservationTableModel observation;

    /**
     * The table in which the observation is displayed.
     */
    private JTable table;

    /**
     * The table scrollbars.
     */
    private ScrollDownScrollPane scrollbars;

    /**
     * The personality-editor.
     */
    private PersonalityEditor personality;

    /**
     * The save-button.
     */
    private JButton save;

    /**
     * The save as-button.
     */
    private JButton saveas;

    /**
     * The summarize-button.
     */
    private JButton summarize;

    /**
     * The file we are editing.
     */
    private File file;

    /**
     * The dirty flag.
     */
    private boolean dirty;

    /**
     * The top-panel's layout.
     */
    private CardLayout toplayout;

    /**
     * The top-panel.
     */
    private JPanel top;

    /**
     * The start-button.
     */
    private JButton start;

    /**
     * The stop-button.
     */
    private JButton stop;

    /**
     * The progress bar showing the progress of the observation.
     */
    private ProgressBar progress;

    /**
     * The timer which stops the observation.
     */
    private Timer stoptimer;

    /**
     * A wrapper around the observation which records all actions now.
     */
    private PresentTimeObservation ptobservation;

    /**
     * The observation input-component.
     */
    private ObservationInput observationinput;

    /**
     * The timer which advances the progress bar.
     */
    private Timer progresstimer;

    /**
     * The timer which plays a sound indicating the observation is nearly at an end.
     */
    private Timer soundtimer;

    /**
     * The sound which is played when the observation is at an end.
     */
    private AudioClip sound;

    /**
     * The popup menu.
     */
    private JPopupMenu popup;

    /**
     * The add behaviour-popup menu item.
     */
    private JMenuItem addbehaviour;

    /**
     * The remove behaviour-popup menu item.
     */
    private JMenuItem removebehaviour;

    /**
     * The current time.
     */
    private long time;

    /**
     * Constructs a new <code>EditObservation</code>.
     */
    public NewObservation() {
        super("New observation", true, true, true, true);
        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(this);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(content);
        toplayout = new CardLayout();
        top = new JPanel(toplayout);
        top.setBorder(new HorizontalLine());
        content.add(top, BorderLayout.NORTH);
        JPanel startpanel = new JPanel(new BorderLayout(2, 2));
        top.add(startpanel, START);
        start = new JButton("Start");
        start.addActionListener(this);
        startpanel.add(start, BorderLayout.WEST);
        JPanel stoppanel = new JPanel(new BorderLayout(2, 2));
        top.add(stoppanel, STOP);
        stop = new JButton("Stop");
        stop.addActionListener(this);
        stoppanel.add(stop, BorderLayout.WEST);
        progress = new ProgressBar();
        progress.setValueFormatter(new TimeValueFormatter(Configurations.getObservationDuration()));
        stoppanel.add(progress, BorderLayout.EAST);
        JPanel savepanel = new JPanel(new BorderLayout(2, 2));
        top.add(savepanel, SAVE);
        JPanel savebuttons = new JPanel(new GridLayout(1, 0, 2, 2));
        savepanel.add(savebuttons, BorderLayout.WEST);
        save = new JButton("Save", Icons.getSaveIcon());
        save.addActionListener(this);
        savebuttons.add(save);
        saveas = new JButton("Save as", Icons.getSaveIcon());
        saveas.addActionListener(this);
        savebuttons.add(saveas);
        summarize = new JButton("Summarize", Icons.getExportIcon());
        summarize.addActionListener(this);
        savebuttons.add(summarize);
        JPanel left = new JPanel(new BorderLayout());
        observation = new ObservationTableModel();
        observation.addTableModelListener(this);
        table = new JTable(observation);
        table.addMouseListener(this);
        scrollbars = new ScrollDownScrollPane(table);
        scrollbars.setAutoScrollDown(false);
        left.add(scrollbars, BorderLayout.CENTER);
        ptobservation = new PresentTimeObservation(observation);
        observationinput = new ObservationInput(ptobservation);
        observationinput.setEnabled(false);
        left.add(observationinput, BorderLayout.SOUTH);
        JPanel right = new JPanel(new VerticalLayout(4));
        personality = new PersonalityEditor(new PersonalityDefaultValues());
        personality.setBorder(new TitledBorder("Personality"));
        personality.addChangeListener(this);
        right.add(personality);
        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        center.setDividerLocation(2.0 / 3.0);
        center.setResizeWeight(2.0 / 3.0);
        content.add(center, BorderLayout.CENTER);
        popup = new JPopupMenu();
        addbehaviour = new JMenuItem("Add behaviour");
        addbehaviour.addActionListener(this);
        popup.add(addbehaviour);
        removebehaviour = new JMenuItem("Remove selected behaviours");
        removebehaviour.addActionListener(this);
        popup.add(removebehaviour);
        stoptimer = new Timer((int) Configurations.getObservationDuration(), this);
        soundtimer = new Timer((int) Configurations.getObservationDuration() - Configurations.getEndOfObservationSoundOffset(), this);
        progresstimer = new Timer(PROGRESS_INTERVAL, this);
        time = 0l;
        try {
            sound = Applet.newAudioClip(Files.searchFileInAllDirectories(Configurations.getEndOfObservationSound()).toURI().toURL());
        } catch (Exception exception) {
            sound = null;
        }
        dirty = false;
        toplayout.show(top, START);
    }

    /**
     * Saves the observation.
     */
    private void saveObservation() {
        if (!personality.issetME()) {
            JOptionPane.showMessageDialog(this, "Please fill in the observer (me)", "No observer specified", JOptionPane.WARNING_MESSAGE);
        } else {
            if (file == null) {
                saveObservationAs();
            } else {
                try {
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document document = builder.newDocument();
                    ObservationDOMWriter domwriter = new ObservationDOMWriter(document, personality);
                    observation.produceObservation(domwriter);
                    Writer output = new FileWriter(file);
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    Source source = new DOMSource(document);
                    Result result = new StreamResult(output);
                    transformer.transform(source, result);
                    dirty = false;
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(this, "An error occurred while saving the file: " + exception.toString());
                }
            }
        }
    }

    /**
     * Saves the observation.
     */
    private void saveObservationAs() {
        if (!personality.issetME()) {
            JOptionPane.showMessageDialog(this, "Please fill in the obserer (me)", "No observer specified", JOptionPane.WARNING_MESSAGE);
        } else {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(Extensions.XTIT_FILE_FILTER);
                try {
                    StringBuilder titname = new StringBuilder();
                    titname.append(personality.getRN());
                    titname.append("_");
                    titname.append(personality.getSEQ());
                    titname.append("_");
                    titname.append(Dates.SAVE_DATE_FORMAT.format(personality.getDATE()));
                    titname.append(".xtit");
                    chooser.setSelectedFile(new File(titname.toString()));
                } catch (AttributeNotSetException exception) {
                }
                if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File selectedfile = chooser.getSelectedFile();
                    if (!selectedfile.exists() || JOptionPane.showConfirmDialog(this, selectedfile.getName() + " already exists. Overwriter file?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                        Document document = builder.newDocument();
                        ObservationDOMWriter domwriter = new ObservationDOMWriter(document, personality);
                        observation.produceObservation(domwriter);
                        Writer output = new FileWriter(file = selectedfile);
                        Transformer transformer = TransformerFactory.newInstance().newTransformer();
                        Source source = new DOMSource(document);
                        Result result = new StreamResult(output);
                        transformer.transform(source, result);
                        dirty = false;
                    }
                }
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, "An error occurred while saving the file: " + exception.toString());
            }
        }
    }

    /**
     * Summarizes the observation.
     */
    private void saveSummary() {
        if (!personality.issetME()) {
            JOptionPane.showMessageDialog(this, "Please fill in the obserer (me)", "No observer specified", JOptionPane.WARNING_MESSAGE);
        } else {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(Extensions.XSUM_FILE_FILTER);
                try {
                    StringBuilder titname = new StringBuilder();
                    titname.append(personality.getRN());
                    titname.append("_");
                    titname.append(personality.getSEQ());
                    titname.append("_");
                    titname.append(Dates.SAVE_DATE_FORMAT.format(personality.getDATE()));
                    titname.append(".xsum");
                    chooser.setSelectedFile(new File(titname.toString()));
                } catch (AttributeNotSetException exception) {
                }
                if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File selectedfile = chooser.getSelectedFile();
                    if (!selectedfile.exists() || JOptionPane.showConfirmDialog(this, selectedfile.getName() + " already exists. Overwriter file?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        SuperSummarizer summarizer = new SuperSummarizer(Configurations.getSummaryIntervals(), Configurations.getDefaultFlyTime());
                        observation.produceObservation(summarizer);
                        SummaryWriter writer = new SummaryXMLWriter(selectedfile);
                        Summary[] summaries = summarizer.getSummaries();
                        for (int i = 0; i < summaries.length; i++) {
                            writer.writeSummary(summaries[i], personality);
                        }
                        writer.closeWriter();
                    }
                }
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, "An error occurred while summarizing the file: " + exception.toString());
                exception.printStackTrace();
            }
        }
    }

    /**
     * Saves the observation automatically.
     */
    private void autoSave() {
        if (observation.isStopped() && personality.issetME()) {
            try {
                String directory = Configurations.getAutosaveDirectory();
                String separator = System.getProperty("file.separator");
                StringBuilder titname = new StringBuilder();
                titname.append(directory);
                titname.append(directory.endsWith(separator) ? "" : separator);
                titname.append(personality.getRN());
                titname.append("_");
                titname.append(personality.getSEQ());
                titname.append("_");
                titname.append(Dates.SAVE_DATE_FORMAT.format(personality.getDATE()));
                titname.append(".xtit");
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = builder.newDocument();
                ObservationDOMWriter domwriter = new ObservationDOMWriter(document, personality);
                observation.produceObservation(domwriter);
                Writer output = new FileWriter(new File(titname.toString()));
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                Source source = new DOMSource(document);
                Result result = new StreamResult(output);
                transformer.transform(source, result);
                StringBuilder sumname = new StringBuilder();
                sumname.append(directory);
                sumname.append(directory.endsWith(separator) ? "" : separator);
                sumname.append(personality.getRN());
                sumname.append("_");
                sumname.append(personality.getSEQ());
                sumname.append("_");
                sumname.append(Dates.SAVE_DATE_FORMAT.format(personality.getDATE()));
                sumname.append(".xsum");
                SuperSummarizer summarizer = new SuperSummarizer(Configurations.getSummaryIntervals(), Configurations.getDefaultFlyTime());
                observation.produceObservation(summarizer);
                SummaryWriter writer = new SummaryXMLWriter(new File(sumname.toString()));
                Summary[] summaries = summarizer.getSummaries();
                for (int i = 0; i < summaries.length; i++) {
                    writer.writeSummary(summaries[i], personality);
                }
                writer.closeWriter();
                dirty = false;
            } catch (Exception exception) {
            }
        }
    }

    /**
     * Closes this window.
     */
    private void closeWindow() {
        stoptimer.stop();
        soundtimer.stop();
        progresstimer.stop();
        autoSave();
        if (dirty) {
            switch(JOptionPane.showConfirmDialog(this, "The observation has been modified. Save changes?", "Save changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                case JOptionPane.YES_OPTION:
                    saveObservation();
                    if (!dirty) dispose();
                    break;
                case JOptionPane.NO_OPTION:
                    dispose();
                    break;
                case JOptionPane.CANCEL_OPTION:
                default:
                    break;
            }
        } else {
            dispose();
        }
    }

    /**
     * Starts the observation.
     */
    private void startObservation() {
        try {
            observationinput.setEnabled(true);
            observationinput.requestFocusInWindow();
            stoptimer.start();
            soundtimer.start();
            progresstimer.start();
            ptobservation.startObservation();
            toplayout.show(top, STOP);
            scrollbars.setAutoScrollDown(true);
            observationinput.requestFocusInWindow();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Error while starting observation", "Starting failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Stops the observation
     */
    private void stopObservation() {
        try {
            stoptimer.stop();
            progresstimer.stop();
            ptobservation.stopObservation();
            observationinput.setEnabled(false);
            toplayout.show(top, SAVE);
            scrollbars.setAutoScrollDown(false);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Error while stopping observation", "Stopping failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Plays a sound
     */
    private void playSound() {
        soundtimer.stop();
        if (sound != null) {
            sound.play();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Adds a behaviour to the table.
     */
    private void addBehaviour() {
        try {
            Behaviour behaviour = BehaviourDialog.showBehaviourDialog(this);
            if (behaviour != null) {
                observation.addBehaviour(behaviour);
                dirty = true;
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Could not add behaviour: " + exception.toString(), "Operation failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Removes all the selected behaviours in the table.
     */
    private void removeBehaviour() {
        int[] rows = table.getSelectedRows();
        Arrays.sort(rows);
        try {
            for (int i = rows.length - 1; i >= 0; i--) {
                observation.removeBehaviour(rows[i]);
                dirty = true;
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, "Could not remove behaviour: " + exception.toString(), "Operation failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles <code>ActionEvent</code>s.
     * @param event The associated <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == save) {
            saveObservation();
        } else if (event.getSource() == saveas) {
            saveObservationAs();
        } else if (event.getSource() == summarize) {
            saveSummary();
        } else if (event.getSource() == start) {
            startObservation();
        } else if (event.getSource() == stop || event.getSource() == stoptimer) {
            stopObservation();
        } else if (event.getSource() == soundtimer) {
            playSound();
        } else if (event.getSource() == progresstimer) {
            time += PROGRESS_INTERVAL;
            progress.setValue((float) time / (float) Configurations.getObservationDuration());
        } else if (event.getSource() == addbehaviour) {
            addBehaviour();
        } else if (event.getSource() == removebehaviour) {
            removeBehaviour();
        }
    }

    /**
     * Invoked when an internal frame is activated.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameActivated(InternalFrameEvent event) {
    }

    /**
     * Invoked when an internal frame has been closed.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameClosed(InternalFrameEvent event) {
    }

    /**
     * Invoked when an internal frame is in the process of being closed.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameClosing(InternalFrameEvent event) {
        if (event.getSource() == this) {
            closeWindow();
        }
    }

    /**
     * Invoked when an internal frame is de-activated.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameDeactivated(InternalFrameEvent event) {
    }

    /**
     * Invoked when an internal frame is de-iconified.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameDeiconified(InternalFrameEvent event) {
    }

    /**
     * Invoked when an internal frame is iconified.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameIconified(InternalFrameEvent event) {
    }

    /**
     * Invoked when a internal frame has been opened.
     * @param event The associated <code>InternalFrameEvent</code>.
     */
    public void internalFrameOpened(InternalFrameEvent event) {
    }

    /**
     * Invoked when a table model has been changed.
     * @param event The associated <code>TableModelEvent</code>.
     */
    public void tableChanged(TableModelEvent event) {
        if (event.getSource() == observation) {
            dirty = true;
        }
    }

    /**
     * Invoked when the target of the listener has changed its state.
     * @param event The associated <code>ChangeEvent</code>.
     */
    public void stateChanged(ChangeEvent event) {
        if (event.getSource() == personality) {
            dirty = true;
        }
    }

    /**
     * Invoked when the mouse button has been clicked (pressed and released) on a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mouseClicked(MouseEvent event) {
    }

    /**
     * Invoked when the mouse enters a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mouseEntered(MouseEvent event) {
    }

    /**
     * Invoked when the mouse exits a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mouseExited(MouseEvent event) {
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mousePressed(MouseEvent event) {
        if (event.isPopupTrigger() && observation.isStopped()) {
            popup.show(table, event.getX(), event.getY());
        }
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * @param event The associated <code>MouseEvent</code>.
     */
    public void mouseReleased(MouseEvent event) {
        if (event.isPopupTrigger() && observation.isStopped()) {
            popup.show(table, event.getX(), event.getY());
        }
    }
}
