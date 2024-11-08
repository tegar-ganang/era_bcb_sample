package edu.unm.casaa.main;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;
import edu.unm.casaa.globals.GlobalCode;
import edu.unm.casaa.globals.GlobalTemplateUiService;
import edu.unm.casaa.globals.GlobalTemplateView;
import edu.unm.casaa.misc.MiscCode;
import edu.unm.casaa.misc.MiscDataItem;
import edu.unm.casaa.misc.MiscTemplateUiService;
import edu.unm.casaa.misc.MiscTemplateView;
import edu.unm.casaa.utterance.ParserTemplateUiService;
import edu.unm.casaa.utterance.ParserTemplateView;
import edu.unm.casaa.utterance.Utterance;
import edu.unm.casaa.utterance.UtteranceList;

public class MainController implements BasicPlayerListener {

    enum Mode {

        PLAYBACK, PARSE, CODE, GLOBALS
    }

    ;

    public static MainController instance = null;

    private ActionTable actionTable = null;

    private OptionsWindow optionsWindow = null;

    private PlayerView playerView = null;

    private JPanel templateView = null;

    private TemplateUiService templateUI = null;

    private String filenameParse = null;

    private String filenameMisc = null;

    private String filenameGlobals = null;

    private String globalsLabel = "Global Ratings";

    private BasicPlayer player = new BasicPlayer();

    private String playerStatus = "";

    private int bytesPerSecond = 0;

    private String filenameAudio = null;

    private UtteranceList utteranceList = null;

    private int currentUtterance = 0;

    private boolean pauseOnUncoded = true;

    private boolean waitingForCode = false;

    private int numSaves = 0;

    private int numUninterruptedUnparses = 0;

    private boolean progressReported = false;

    private boolean endOfMediaReported = false;

    private int endOfMediaPosition = 0;

    private int statusChangeEventIndex = 0;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        SplashWindow splash = new SplashWindow();
        Date date = new Date();
        long startTime = date.getTime();
        splash.setVisible(true);
        MainController.instance = new MainController();
        MainController.instance.init();
        long elapsed = date.getTime() - startTime;
        try {
            Thread.sleep(1000 - elapsed);
        } catch (Exception e) {
        }
        splash.setVisible(false);
        MainController.instance.show();
        MainController.instance.run();
    }

    public MainController() {
    }

    public void init() {
        PlayerView.setLookAndFeel();
        parseUserConfig();
        playerView = new PlayerView();
        player.addBasicPlayerListener(this);
        registerPlayerViewListeners();
        playerView.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });
        setMode(Mode.PLAYBACK);
    }

    public void show() {
        playerView.setVisible(true);
    }

    public void run() {
        while (true) {
            try {
                if (progressReported) applyPlayerProgress();
                if (endOfMediaReported) applyEOM();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleAction(String action) {
        if ("parseStart".equals(action)) {
            parseStart();
        } else if ("parseEnd".equals(action)) {
            parseEnd();
        } else if ("play".equals(action)) {
            handleActionPlay();
        } else if ("replay".equals(action)) {
            handleActionReplay();
        } else if ("unparse".equals(action)) {
            handleActionUnparse();
        } else if ("uncode".equals(action)) {
            handleActionUncode();
        } else if ("unparseAndReplay".equals(action)) {
            handleActionUnparseAndReplay();
        } else if ("rewind5s".equals(action)) {
            handleActionRewind5s();
        }
    }

    public void globalDataChanged() {
        assert (templateView instanceof GlobalTemplateView);
        saveSession();
    }

    public synchronized void setPauseOnUncoded(boolean value) {
        pauseOnUncoded = value;
        waitingForCode = false;
    }

    public ActionTable getActionTable() {
        if (actionTable == null) {
            actionTable = new ActionTable();
            mapAction("Start", "parseStart");
            mapAction("End", "parseEnd");
            mapAction("Play/Pause", "play");
            mapAction("Replay", "replay");
            mapAction("Unparse", "unparse");
            mapAction("Unparse & Replay", "unparseAndReplay");
            mapAction("Uncode", "uncode");
            mapAction("Rewind 5s", "rewind5s");
        }
        return actionTable;
    }

    public int numUtterances() {
        return getUtteranceList().size();
    }

    public Utterance utterance(int index) {
        return getUtteranceList().get(index);
    }

    private void utteranceListChanged() {
        playerView.getTimeline().repaint();
    }

    public int getBytesPerSecond() {
        return bytesPerSecond;
    }

    public int getAudioLength() {
        return player.getEncodedLength();
    }

    public synchronized Utterance getCurrentUtterance() {
        assert (currentUtterance >= 0);
        if (currentUtterance < getUtteranceList().size()) return getUtteranceList().get(currentUtterance); else return null;
    }

    public void handleUserCodesParseException(File file, SAXParseException e) {
        JOptionPane.showMessageDialog(playerView, "Parse error in " + file.getAbsolutePath() + " (line " + e.getLineNumber() + "):\n" + e.getMessage(), "Failed to load user codes", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }

    public void handleUserCodesGenericException(File file, Exception e) {
        JOptionPane.showMessageDialog(playerView, "Unknown error parsing file: " + file.getAbsolutePath() + "\n" + e.toString(), "Failed to load user codes", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }

    public void handleUserCodesError(File file, String message) {
        JOptionPane.showMessageDialog(playerView, "Error loading file: " + file.getAbsolutePath() + "\n" + message, "Failed to load user codes", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }

    public void handleUserCodesMissing(File file) {
        JOptionPane.showMessageDialog(playerView, "Failed to find required file." + file.getAbsolutePath(), "Failed to load user codes", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }

    public void showWarning(String title, String message) {
        JOptionPane.showMessageDialog(playerView, message, title, JOptionPane.WARNING_MESSAGE);
    }

    public String getGlobalsLabel() {
        return globalsLabel;
    }

    private void actionExit() {
        saveIfNeeded();
        System.exit(0);
    }

    private void mapAction(String text, String command) {
        actionTable.put(command, new MainControllerAction(text, command));
    }

    private void display(String msg) {
        System.out.println(msg);
    }

    private void displayPlayerException(BasicPlayerException e) {
        display("BasicPlayerException: " + e.getMessage());
        e.printStackTrace();
    }

    private void parseUserConfig() {
        File file = new File("userConfiguration.xml");
        if (file.exists()) {
            try {
                DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = fact.newDocumentBuilder();
                Document doc = builder.parse(file.getCanonicalFile());
                Node root = doc.getDocumentElement();
                for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                    if (node.getNodeName().equalsIgnoreCase("codes")) parseUserCodes(file, node); else if (node.getNodeName().equalsIgnoreCase("globals")) parseUserGlobals(file, node); else if (node.getNodeName().equalsIgnoreCase("globalsBorder")) parseUserGlobalsBorder(file, node);
                }
            } catch (SAXParseException e) {
                handleUserCodesParseException(file, e);
            } catch (Exception e) {
                handleUserCodesGenericException(file, e);
            }
        } else {
            handleUserCodesMissing(file);
        }
    }

    private void parseUserCodes(File file, Node codes) {
        for (Node n = codes.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeName().equalsIgnoreCase("code")) {
                NamedNodeMap map = n.getAttributes();
                Node nodeValue = map.getNamedItem("value");
                int value = Integer.parseInt(nodeValue.getTextContent());
                String name = map.getNamedItem("name").getTextContent();
                if (!MiscCode.addCode(new MiscCode(value, name))) handleUserCodesError(file, "Failed to add code.");
            }
        }
    }

    private void parseUserGlobals(File file, Node globals) {
        for (Node n = globals.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeName().equalsIgnoreCase("global")) {
                NamedNodeMap map = n.getAttributes();
                Node nodeValue = map.getNamedItem("value");
                int value = Integer.parseInt(nodeValue.getTextContent());
                Node nodeDefaultRating = map.getNamedItem("defaultRating");
                Node nodeMinRating = map.getNamedItem("minRating");
                Node nodeMaxRating = map.getNamedItem("maxRating");
                String name = map.getNamedItem("name").getTextContent();
                String label = map.getNamedItem("label").getTextContent();
                GlobalCode code = new GlobalCode(value, name, label);
                if (nodeDefaultRating != null) code.defaultRating = Integer.parseInt(nodeDefaultRating.getTextContent());
                if (nodeMinRating != null) code.minRating = Integer.parseInt(nodeMinRating.getTextContent());
                if (nodeMaxRating != null) code.maxRating = Integer.parseInt(nodeMaxRating.getTextContent());
                if (code.defaultRating < code.minRating || code.defaultRating > code.maxRating || code.maxRating < code.minRating) {
                    handleUserCodesError(file, "Invalid range for global code: " + code.name + ", minRating: " + code.minRating + ", maxRating: " + code.maxRating + ", defaultRating: " + code.defaultRating);
                }
                if (!GlobalCode.addCode(code)) handleUserCodesError(file, "Failed to add global code.");
            }
        }
    }

    private void parseUserGlobalsBorder(File file, Node node) {
        NamedNodeMap map = node.getAttributes();
        globalsLabel = map.getNamedItem("label").getTextContent();
    }

    private synchronized Utterance getLastUtterance() {
        if (getUtteranceList().size() > 0) {
            return getUtteranceList().get(getUtteranceList().size() - 1);
        } else {
            return null;
        }
    }

    private synchronized Utterance getNextUtterance() {
        if (currentUtterance + 1 < getUtteranceList().size()) {
            return getUtteranceList().get(currentUtterance + 1);
        } else {
            return null;
        }
    }

    private synchronized Utterance getPreviousUtterance() {
        if (currentUtterance > 0) {
            return getUtteranceList().get(currentUtterance - 1);
        } else {
            return null;
        }
    }

    private synchronized boolean hasPreviousUtterance() {
        return currentUtterance > 0;
    }

    private synchronized void gotoPreviousUtterance() {
        assert (hasPreviousUtterance());
        currentUtterance--;
    }

    private synchronized boolean isParsingUtterance() {
        Utterance current = getCurrentUtterance();
        if (current == null) return false;
        return !current.isParsed();
    }

    private synchronized void playerSeek(int bytes) {
        try {
            player.seek(bytes);
        } catch (BasicPlayerException e) {
            showAudioFileNotSeekableDialog();
            displayPlayerException(e);
        }
        getOptionsWindow().applyAudioOptions();
        updateTimeDisplay();
        updateSeekSliderDisplay();
    }

    private synchronized void playerSeekToSlider() {
        if (player.getStatus() == BasicPlayer.UNKNOWN) {
            return;
        }
        double t = playerView.getSliderSeek().getValue() / (double) PlayerView.SEEK_MAX_VAL;
        long bytes = (long) (t * player.getEncodedLength());
        try {
            player.stop();
            player.seek(bytes);
        } catch (BasicPlayerException e) {
            displayPlayerException(e);
        }
        getOptionsWindow().applyAudioOptions();
        updateTimeDisplay();
        playbackPositionChanged();
    }

    private synchronized void playerPause() {
        try {
            player.pause();
        } catch (BasicPlayerException e) {
            displayPlayerException(e);
        }
    }

    private synchronized void playerResume() {
        try {
            player.resume();
        } catch (BasicPlayerException e) {
            displayPlayerException(e);
        }
        getOptionsWindow().applyAudioOptions();
    }

    private synchronized void playerPlay() {
        try {
            player.play();
        } catch (BasicPlayerException e) {
            displayPlayerException(e);
        }
        getOptionsWindow().applyAudioOptions();
    }

    private void cleanupMode() {
        utteranceList = null;
        currentUtterance = 0;
        waitingForCode = false;
        resetUnparseCount();
    }

    private void setMode(Mode mode) {
        setTemplateView(mode);
        playerView.getSliderSeek().setEnabled(filenameAudio != null);
        playerView.getButtonPlay().setEnabled(filenameAudio != null);
        playerView.getButtonReplay().setVisible(mode == Mode.PARSE || mode == Mode.CODE);
        playerView.getButtonUnparse().setVisible(mode == Mode.PARSE);
        playerView.getButtonUnparseAndReplay().setVisible(mode == Mode.PARSE);
        playerView.getButtonUncode().setVisible(mode == Mode.CODE);
        playerView.getButtonRewind5s().setEnabled(filenameAudio != null);
        playerView.getTimeline().setVisible(mode == Mode.PARSE || mode == Mode.CODE);
        playerView.pack();
        if (mode == Mode.GLOBALS) globalDataChanged();
    }

    private synchronized void applyPlayerProgress() {
        updateTimeDisplay();
        updateSeekSliderDisplay();
        if (templateView instanceof MiscTemplateView) {
            Utterance current = getCurrentUtterance();
            Utterance next = getNextUtterance();
            int bytes = player.getEncodedStreamPosition();
            if ((current != null && bytes > current.getEndBytes()) || (next != null && bytes >= next.getStartBytes())) {
                assert (current != null);
                if (pauseOnUncoded && !current.isCoded() && (player.getStatus() == BasicPlayer.PLAYING)) {
                    playerPause();
                    waitingForCode = true;
                }
                if (current.isCoded() || !pauseOnUncoded) {
                    currentUtterance++;
                }
            }
        }
        updateUtteranceDisplays();
        progressReported = false;
    }

    private synchronized void applyEOM() {
        if (isParsingUtterance()) {
            parseEnd(endOfMediaPosition);
        }
        endOfMediaReported = false;
    }

    private boolean selectAndLoadAudioFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Audio File");
        chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio only for coding", "wav"));
        if (chooser.showOpenDialog(playerView) == JFileChooser.APPROVE_OPTION) {
            return loadAudioFile(chooser.getSelectedFile().getAbsolutePath());
        } else {
            return false;
        }
    }

    private boolean loadAudioFile(String filename) {
        filenameAudio = filename;
        bytesPerSecond = 0;
        try {
            player.open(new File(filenameAudio));
            bytesPerSecond = player.getBytesPerSecond();
            updateTimeDisplay();
            updateSeekSliderDisplay();
            return true;
        } catch (BasicPlayerException e) {
            showAudioFileNotFoundDialog();
            displayPlayerException(e);
            return false;
        }
    }

    private void registerPlayerViewListeners() {
        playerView.getSliderSeek().addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                if (playerView.getSliderSeek().getValueIsAdjusting()) {
                    playerSeekToSlider();
                }
            }
        });
        playerView.getMenuItemLoadAudio().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (selectAndLoadAudioFile()) {
                    setMode(Mode.PLAYBACK);
                }
            }
        });
        playerView.getMenuItemOptions().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getOptionsWindow().setVisible(true);
            }
        });
        playerView.getMenuItemExit().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        playerView.getMenuItemNewParse().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleNewParseFile();
            }
        });
        playerView.getMenuItemLoadParse().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleLoadParseFile();
            }
        });
        playerView.getMenuItemNewCode().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleNewCodeFile();
            }
        });
        playerView.getMenuItemLoadCode().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleLoadCodeFile();
            }
        });
        playerView.getMenuItemCodeGlobals().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleNewGlobalRatings();
            }
        });
        playerView.getMenuItemHelp().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showHelpDialog();
            }
        });
        playerView.getMenuItemAbout().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleAboutWindow();
            }
        });
    }

    private synchronized void handleActionPlay() {
        if (waitingForCode) return;
        if (player.getStatus() == BasicPlayer.PLAYING) {
            playerPause();
        } else if (player.getStatus() == BasicPlayer.PAUSED) {
            playerResume();
        } else if (player.getStatus() == BasicPlayer.STOPPED || player.getStatus() == BasicPlayer.OPENED) {
            playerPlay();
        }
    }

    private synchronized void handleActionReplay() {
        if (templateView instanceof ParserTemplateView || templateView instanceof MiscTemplateView) {
            Utterance utterance = getCurrentUtterance();
            int pos = 0;
            if (utterance != null) {
                pos = utterance.getStartBytes();
                pos -= bytesPerSecond;
                pos = Math.max(pos, 0);
            }
            playerSeek(pos);
            playbackPositionChanged();
        } else {
            showParsingErrorDialog();
        }
        updateTimeDisplay();
        updateSeekSliderDisplay();
    }

    private synchronized void handleActionUnparseAndReplay() {
        handleActionUnparse();
        handleActionReplay();
    }

    private synchronized void handleActionUnparse() {
        if (templateView instanceof ParserTemplateView) {
            if (getUtteranceList().isEmpty()) return;
            removeLastUtterance(false);
            incrementUnparseCount();
        } else {
            showParsingErrorDialog();
        }
        saveSession();
    }

    private synchronized void handleActionUncode() {
        if (templateView instanceof MiscTemplateView) {
            removeLastCode();
        } else {
            showParsingErrorDialog();
        }
        saveSession();
    }

    private synchronized void handleActionRewind5s() {
        assert (bytesPerSecond > 0);
        int pos = streamPosition();
        pos -= 5 * bytesPerSecond;
        pos = Math.max(pos, 0);
        playerSeek(pos);
        updateUtteranceDisplays();
        updateTimeDisplay();
        updateSeekSliderDisplay();
        playbackPositionChanged();
    }

    private synchronized void incrementUnparseCount() {
        numUninterruptedUnparses++;
        if (numUninterruptedUnparses >= 4) {
            showWarning("Unparse Warning", "You have unparsed 4 times in a row.");
            numUninterruptedUnparses = 0;
        }
    }

    private synchronized void resetUnparseCount() {
        numUninterruptedUnparses = 0;
    }

    private synchronized void updateSeekSliderDisplay() {
        if (playerView.getSliderSeek().getValueIsAdjusting()) return;
        int position = player.getEncodedStreamPosition();
        int length = player.getEncodedLength();
        double t = (length > 0) ? (position / (double) length) : 0;
        if (t >= 1.0) {
            playerView.setSliderSeek(PlayerView.SEEK_MAX_VAL);
        } else if (t == 0) {
            playerView.setSliderSeek(0);
        } else {
            playerView.setSliderSeek((int) (t * PlayerView.SEEK_MAX_VAL));
        }
    }

    public synchronized void handleSliderPan(JSlider slider) {
        if (player.hasPanControl()) {
            try {
                player.setPan(slider.getValue() / 10.0);
            } catch (BasicPlayerException e) {
                displayPlayerException(e);
            }
        }
    }

    public synchronized void handleSliderGain(JSlider slider) {
        if (player.hasGainControl()) {
            try {
                player.setGain(slider.getValue() / 100.0);
            } catch (BasicPlayerException e) {
                displayPlayerException(e);
            }
        }
    }

    private synchronized void playbackPositionChanged() {
        waitingForCode = false;
    }

    private synchronized OptionsWindow getOptionsWindow() {
        if (optionsWindow == null) optionsWindow = new OptionsWindow();
        return optionsWindow;
    }

    private synchronized boolean confirmOverwrite(String filename) {
        int option = JOptionPane.showConfirmDialog(playerView, "File '" + filename + "' already exists.  Overwrite?", "File Exists", JOptionPane.OK_CANCEL_OPTION);
        return option == JOptionPane.OK_OPTION;
    }

    private synchronized void handleNewParseFile() {
        if (player.getStatus() == BasicPlayer.PLAYING) playerPause();
        saveIfNeeded();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Name New Parse File");
        chooser.setFileFilter(new FileNameExtensionFilter("PARSE files", "parse"));
        if (chooser.showSaveDialog(playerView) != JFileChooser.APPROVE_OPTION) return;
        String newFilename = chooser.getSelectedFile().getAbsolutePath();
        newFilename = correctTextFileType(".parse", newFilename);
        if (new File(newFilename).exists() && !confirmOverwrite(newFilename)) return;
        if (selectAndLoadAudioFile()) {
            cleanupMode();
            filenameParse = newFilename;
            utteranceListChanged();
            setMode(Mode.PARSE);
        }
    }

    private synchronized void handleNewCodeFile() {
        if (player.getStatus() == BasicPlayer.PLAYING) playerPause();
        saveIfNeeded();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a Parse File to Code");
        chooser.setFileFilter(new FileNameExtensionFilter("PARSE files", "parse"));
        if (chooser.showOpenDialog(playerView) != JFileChooser.APPROVE_OPTION) return;
        String newFilenameParse = chooser.getSelectedFile().getAbsolutePath();
        String newFilenameMisc = correctTextFileType(".casaa", newFilenameParse);
        chooser.setDialogTitle("Select a Name for the New Code File");
        chooser.setFileFilter(new FileNameExtensionFilter("CASAA files", "casaa"));
        chooser.setSelectedFile(new File(newFilenameMisc));
        if (chooser.showSaveDialog(playerView) != JFileChooser.APPROVE_OPTION) return;
        newFilenameMisc = chooser.getSelectedFile().getAbsolutePath();
        newFilenameMisc = correctTextFileType(".casaa", newFilenameMisc);
        if (new File(newFilenameMisc).exists() && !confirmOverwrite(newFilenameMisc)) return;
        cleanupMode();
        filenameParse = newFilenameParse;
        filenameMisc = newFilenameMisc;
        try {
            copyParseFileToCodeFile();
        } catch (IOException e) {
            e.printStackTrace();
            showFileNotCreatedDialog();
        }
        filenameAudio = getUtteranceList().loadFromFile(new File(filenameMisc));
        utteranceListChanged();
        loadAudioFile(filenameAudio);
        setMode(Mode.CODE);
        postLoad();
    }

    private synchronized void handleNewGlobalRatings() {
        if (player.getStatus() == BasicPlayer.PLAYING) playerPause();
        saveIfNeeded();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Name New Globals File");
        chooser.setFileFilter(new FileNameExtensionFilter("GLOBALS files", "global"));
        if (chooser.showSaveDialog(playerView) != JFileChooser.APPROVE_OPTION) return;
        String newFilename = chooser.getSelectedFile().getAbsolutePath();
        newFilename = correctTextFileType(".global", newFilename);
        if (new File(newFilename).exists() && !confirmOverwrite(newFilename)) return;
        if (selectAndLoadAudioFile()) {
            cleanupMode();
            filenameGlobals = newFilename;
            setMode(Mode.GLOBALS);
        }
    }

    private synchronized void handleLoadParseFile() {
        if (player.getStatus() == BasicPlayer.PLAYING) playerPause();
        saveIfNeeded();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Parse File");
        chooser.setFileFilter(new FileNameExtensionFilter("PARSE files", "parse"));
        if (chooser.showOpenDialog(playerView) == JFileChooser.APPROVE_OPTION) {
            cleanupMode();
            filenameParse = chooser.getSelectedFile().getAbsolutePath();
            filenameAudio = getUtteranceList().loadFromFile(new File(filenameParse));
            utteranceListChanged();
            if (filenameAudio.equalsIgnoreCase("ERROR: No Audio File Listed")) {
                showAudioFileNotFoundDialog();
                return;
            }
            loadAudioFile(filenameAudio);
            setMode(Mode.PARSE);
            postLoad();
        }
    }

    private synchronized void handleLoadCodeFile() {
        if (player.getStatus() == BasicPlayer.PLAYING) playerPause();
        saveIfNeeded();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load CASAA File");
        chooser.setFileFilter(new FileNameExtensionFilter("CASAA code files", "casaa"));
        if (chooser.showOpenDialog(playerView) == JFileChooser.APPROVE_OPTION) {
            cleanupMode();
            filenameMisc = chooser.getSelectedFile().getAbsolutePath();
            filenameAudio = getUtteranceList().loadFromFile(new File(filenameMisc));
            utteranceListChanged();
            loadAudioFile(filenameAudio);
            setMode(Mode.CODE);
            postLoad();
        }
    }

    private String correctTextFileType(String fileType, String filename) {
        if (filename.endsWith(fileType)) {
            return filename;
        } else if (fileType.equalsIgnoreCase(".parse")) {
            return filename.concat(fileType);
        } else if (filename.endsWith(".parse") && fileType.equalsIgnoreCase(".casaa")) {
            return filename.substring(0, (filename.length() - ".parse".length())).concat(fileType);
        } else {
            return filename.concat(fileType);
        }
    }

    private synchronized void saveIfNeeded() {
        if (isParsingUtterance()) {
            parseEnd();
        }
    }

    private synchronized void saveSession() {
        saveCurrentTextFile(false);
        if (numSaves % 10 == 0) {
            saveCurrentTextFile(true);
        }
        numSaves++;
    }

    private synchronized void saveCurrentTextFile(boolean asBackup) {
        if (templateView instanceof ParserTemplateView && filenameParse != null) {
            String filename = filenameParse;
            if (asBackup) filename += ".backup";
            getUtteranceList().writeToFile(new File(filename), filenameAudio);
        } else if (templateView instanceof MiscTemplateView && filenameMisc != null) {
            String filename = filenameMisc;
            if (asBackup) filename += ".backup";
            getUtteranceList().writeToFile(new File(filename), filenameAudio);
        } else if (templateView instanceof GlobalTemplateView) {
            String filename = filenameGlobals;
            if (asBackup) filename += ".backup";
            ((GlobalTemplateUiService) templateUI).writeGlobalsToFile(new File(filename), filenameAudio);
        }
    }

    private void handleAboutWindow() {
        AboutWindowView aboutWindow = new AboutWindowView();
        aboutWindow.setFocusable(true);
    }

    private void showHelpDialog() {
        JOptionPane.showMessageDialog(playerView, "Please visit http://casaa.unm.edu for the latest reference manual.", "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAudioFileNotFoundDialog() {
        JOptionPane.showMessageDialog(playerView, "The audio file:\n" + filenameAudio + "\nassociated with this project cannot be located.\n" + "Please verify that this file exists, and that it is named correctly.", "Audio File Not Found Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showAudioFileNotSeekableDialog() {
        JOptionPane.showMessageDialog(playerView, "The audio file:\n" + filenameAudio + "\nfailed when setting the play position in the file.\n" + "Please try to reload the file.", "Audio File Seek Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showFileNotCreatedDialog() {
        JOptionPane.showMessageDialog(playerView, "The file:\n" + filenameMisc + "\nfailed to be modified or created.\n" + "Please try to rename or reload the file manually.", "File Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showQueueNotLoadedDialog() {
        JOptionPane.showMessageDialog(playerView, "The Data Queue failed to load.\n" + "Please verify the text file is properly formatted.", "Data Queue Loading Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showTemplateNotFoundDialog() {
        JOptionPane.showMessageDialog(playerView, "The Coding Template Failed to Load.\n", "Coding Template Loading Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showParsingErrorDialog() {
        JOptionPane.showMessageDialog(playerView, "An error occurred while parsing this utterance.\n", "Utterance Parsing Error", JOptionPane.ERROR_MESSAGE);
    }

    private void updateTimeDisplay() {
        playerView.getTimeline().repaint();
        if (bytesPerSecond != 0) {
            int bytes = player.getEncodedStreamPosition();
            int seconds = bytes / bytesPerSecond;
            playerView.setLabelTime("Time:  " + TimeCode.toString(seconds));
        } else {
        }
    }

    private void copyParseFileToCodeFile() throws IOException {
        InputStream in = new FileInputStream(new File(filenameParse));
        OutputStream out = new FileOutputStream(new File(filenameMisc));
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) out.write(buffer, 0, length);
        in.close();
        out.close();
    }

    private void setTemplateView(Mode mode) {
        templateView = null;
        templateUI = null;
        System.gc();
        switch(mode) {
            case PLAYBACK:
                break;
            case PARSE:
                templateUI = new ParserTemplateUiService(actionTable);
                templateView = templateUI.getTemplateView();
                break;
            case CODE:
                templateUI = new MiscTemplateUiService();
                templateView = templateUI.getTemplateView();
                break;
            case GLOBALS:
                templateUI = new GlobalTemplateUiService();
                templateView = templateUI.getTemplateView();
                break;
            default:
                assert false : "Mode unrecognized: " + mode.toString();
                break;
        }
        playerView.setPanelTemplate(templateView);
    }

    public int streamPosition() {
        int position = player.getEncodedStreamPosition();
        if (position < 0) {
            int length = player.getEncodedLength() - 1;
            position = (length > 0) ? (length - 1) : 0;
        }
        return position;
    }

    public synchronized void parseStart() {
        int position = streamPosition();
        Utterance last = getLastUtterance();
        if (last != null && position < last.getStartBytes()) return;
        if (isParsingUtterance()) {
            parseEnd(position);
        } else {
            if (last != null && position < last.getEndBytes()) {
                return;
            }
        }
        assert (bytesPerSecond > 0);
        String startString = TimeCode.toString(position / bytesPerSecond);
        int order = getUtteranceList().size();
        Utterance data = new MiscDataItem(order, startString, position);
        getUtteranceList().add(data);
        currentUtterance = order;
        resetUnparseCount();
        updateUtteranceDisplays();
    }

    public synchronized void parseEnd() {
        parseEnd(streamPosition());
    }

    public synchronized void parseEnd(int endBytes) {
        assert (endBytes >= 0);
        Utterance last = getLastUtterance();
        if (last != null && endBytes < last.getStartBytes()) return;
        Utterance current = getCurrentUtterance();
        if (current != null) {
            assert (bytesPerSecond > 0);
            String endString = TimeCode.toString(endBytes / bytesPerSecond);
            current.setEndTime(endString);
            current.setEndBytes(endBytes);
            saveSession();
        }
        resetUnparseCount();
        updateUtteranceDisplays();
    }

    public synchronized void handleButtonMiscCode(MiscCode miscCode) {
        assert (miscCode.isValid());
        Utterance utterance = getCurrentUtterance();
        if (utterance == null) return;
        int playbackPosition = streamPosition();
        if (playbackPosition < utterance.getStartBytes()) return;
        utterance.setMiscCode(miscCode);
        saveSession();
        if (waitingForCode) {
            currentUtterance++;
            waitingForCode = false;
            playerResume();
        }
        updateUtteranceDisplays();
    }

    private synchronized void removeLastUtterance(boolean seek) {
        getUtteranceList().removeLast();
        if (getUtteranceList().size() > 0) currentUtterance = getUtteranceList().size() - 1; else currentUtterance = 0;
        Utterance utterance = getCurrentUtterance();
        if (seek) playerSeek(utterance == null ? 0 : utterance.getStartBytes());
        if (utterance != null) utterance.stripEndData();
        updateUtteranceDisplays();
    }

    private synchronized void removeLastCode() {
        Utterance utterance = getCurrentUtterance();
        if (utterance != null) utterance.setMiscCode(MiscCode.INVALID_CODE);
        if (hasPreviousUtterance()) {
            gotoPreviousUtterance();
            utterance = getCurrentUtterance();
            assert utterance != null;
            utterance.setMiscCode(MiscCode.INVALID_CODE);
            playerSeek(utterance.getStartBytes());
        } else {
            playerSeek(0);
        }
        waitingForCode = false;
        updateUtteranceDisplays();
    }

    private synchronized void updateUtteranceDisplays() {
        playerView.getTimeline().repaint();
        if (templateView instanceof ParserTemplateView) {
            ParserTemplateView view = (ParserTemplateView) templateView;
            Utterance current = getCurrentUtterance();
            Utterance prev = getPreviousUtterance();
            if (current == null) {
                view.setTextFieldOrder("");
                view.setTextFieldStartTime("");
                view.setTextFieldEndTime("");
            } else {
                view.setTextFieldOrder("" + current.getEnum());
                view.setTextFieldStartTime(current.getStartTime());
                view.setTextFieldEndTime(current.getEndTime());
            }
            if (prev == null) view.setTextFieldPrev(""); else view.setTextFieldPrev(prev.toString());
        } else if (templateView instanceof MiscTemplateView) {
            MiscTemplateView view = (MiscTemplateView) templateView;
            Utterance current = getCurrentUtterance();
            Utterance next = getNextUtterance();
            Utterance prev = getPreviousUtterance();
            if (next == null) view.setTextFieldNext(""); else view.setTextFieldNext(next.toString());
            if (prev == null) view.setTextFieldPrev(""); else view.setTextFieldPrev(prev.toString());
            if (current == null) {
                view.setTextFieldOrder("");
                view.setTextFieldCode("");
                view.setTextFieldStartTime("");
                view.setTextFieldEndTime("");
            } else {
                view.setTextFieldOrder("" + current.getEnum());
                if (current.getMiscCode().value == MiscCode.INVALID) view.setTextFieldCode(""); else view.setTextFieldCode(current.getMiscCode().name);
                view.setTextFieldStartTime(current.getStartTime());
                view.setTextFieldEndTime(current.getEndTime());
                if (streamPosition() < current.getStartBytes()) view.setTextFieldStartTimeColor(Color.RED); else view.setTextFieldStartTimeColor(Color.BLACK);
            }
        }
    }

    private synchronized void postLoad() {
        currentUtterance = 0;
        if (templateView instanceof ParserTemplateView) {
            if (getUtteranceList().size() > 0) {
                currentUtterance = getUtteranceList().size() - 1;
            }
            if (getCurrentUtterance() == null) {
                playerSeek(0);
            } else {
                playerSeek(getCurrentUtterance().getEndBytes());
            }
        } else if (templateView instanceof MiscTemplateView) {
            currentUtterance = getUtteranceList().getLastCodedUtterance();
            currentUtterance++;
            if (getCurrentUtterance() == null) {
                if (getUtteranceList().isEmpty()) playerSeek(0); else playerSeek(getLastUtterance().getEndBytes());
            } else {
                if (currentUtterance == 0) playerSeek(0); else playerSeek(getCurrentUtterance().getStartBytes());
            }
        } else if (templateView == null) {
            showTemplateNotFoundDialog();
        } else {
            showQueueNotLoadedDialog();
        }
        updateUtteranceDisplays();
        numSaves = 0;
    }

    private synchronized UtteranceList getUtteranceList() {
        if (utteranceList == null) utteranceList = new UtteranceList();
        return utteranceList;
    }

    public void opened(Object stream, Map<Object, Object> properties) {
    }

    public void setController(BasicController controller) {
    }

    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map<Object, Object> properties) {
        progressReported = true;
    }

    public void stateUpdated(BasicPlayerEvent event) {
        synchronized (this) {
            String oldStatus = new String(playerStatus);
            switch(event.getCode()) {
                case 0:
                    playerStatus = "OPENING";
                    break;
                case 1:
                    playerStatus = "OPENED";
                    break;
                case 2:
                    playerStatus = "PLAYING";
                    break;
                case 3:
                    playerStatus = "STOPPED";
                    break;
                case 4:
                    playerStatus = "PAUSED";
                    break;
                case 5:
                    playerStatus = "PLAYING";
                    break;
                case 6:
                    break;
                case 7:
                    break;
                case 8:
                    endOfMediaPosition = event.getPosition();
                    endOfMediaReported = true;
                    break;
                case 9:
                    break;
                case 10:
                    break;
                default:
                    playerStatus = "UNKNOWN";
            }
            if (!playerStatus.equals(oldStatus)) {
                if (event.getIndex() >= statusChangeEventIndex) {
                    statusChangeEventIndex = event.getIndex();
                    File file = new File(filenameAudio);
                    String str = playerStatus.concat(":  " + file.getName() + "  |  Total Time = " + TimeCode.toString(player.getSecondsPerFile()));
                    playerView.setLabelPlayerStatus(str);
                }
            }
        }
    }
}
