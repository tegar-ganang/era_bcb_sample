package edu.umn.cs.nlp.specviewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.MouseInputListener;
import edu.umn.cs.nlp.sound.RawSoundFile;
import edu.umn.cs.nlp.sound.SoundFile;
import edu.umn.cs.nlp.sound.Spectrum;
import edu.umn.cs.nlp.feature.PitchFeature;

public class JSpecPanel extends JPanel implements KeyListener, ActionListener {

    private static final long serialVersionUID = -6245233617145919587L;

    private static final int MAX_SPEC_WIDTH = 800;

    private static final int BORDER = 2;

    private static final int FRAME_WIDTH_IN_PIXELS = 1;

    private JTextArea gLocationTextBox = null;

    private JTextArea gMarkupTextArea = null;

    private JTextArea gPhoneTextArea = null;

    private JButton gPlayButton = null;

    private JImageComponent gFullComp = null;

    private JImageComponent gSliceComp = null;

    private JImageComponent gSliceScaleComp = null;

    private JComboBox gSpecComboBox = null;

    private JScrollPane gMarkupPane = null;

    private JScrollPane gFullCompPane = null;

    private AudioClipInfo gClipInfo = null;

    private int gCurrentFrame = 0;

    private ArrayList<JImageComponent> gPhoneTrackComps = null;

    private LinkedList<PhoneTrack> gPhoneTracks = new LinkedList<PhoneTrack>();

    private LinkedList<SpecOverlay> gSpecOverlay = null;

    private LinkedList<SpecOverlay> gCepsOverlay = null;

    private HashMap<Character, SpecOverlay> gToggledOverlays = null;

    private HashMap<String, Color> gPhoneColorMap = null;

    private LinkedList<String> gVoicedPhoneList = null;

    private SpecOverlay gFrameOverlay = null;

    private boolean bShowSpectrum = true;

    /********************
	 * Constructs a JSpecPanel with from the sound file mFilename and using properties mProps
	 * @param mFilename the sound file
	 * @param mProps a set of properties
	 */
    public JSpecPanel(String mFilename, Properties mProps) {
        super();
        setBackground(Color.LIGHT_GRAY);
        gCepsOverlay = new LinkedList<SpecOverlay>();
        if (mFilename == null) {
            JFileChooser tChooser = new JFileChooser();
            int result = tChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                mFilename = tChooser.getSelectedFile().getAbsolutePath();
            }
        }
        if (mFilename == null) {
            gClipInfo = null;
        } else {
            try {
                gClipInfo = new AudioClipInfo(mFilename, FRAME_WIDTH_IN_PIXELS);
            } catch (FileNotFoundException mFNFE) {
                gClipInfo = null;
            }
        }
        gPhoneTrackComps = new ArrayList<JImageComponent>();
        this.setLayout(null);
        if (gClipInfo == null) {
        } else {
            addComponents();
        }
        setVisible(true);
    }

    private void load() {
        String tFilename = null;
        JFileChooser tChooser = new JFileChooser();
        int result = tChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            tFilename = tChooser.getSelectedFile().getAbsolutePath();
        }
        if (tFilename == null) {
            gClipInfo = null;
        } else {
            try {
                gClipInfo = new AudioClipInfo(tFilename, FRAME_WIDTH_IN_PIXELS);
                this.addComponents();
            } catch (FileNotFoundException mFNFE) {
                gClipInfo = null;
            }
        }
    }

    private void addComponents() {
        gSpecOverlay = getDefaultSpecOverlays();
        gCepsOverlay.add(getFrameOverlay());
        add(getPlayButton());
        add(getLocationTextBox());
        add(getSpecComboBox());
        add(getFullCompPane());
        add(getSliceComp());
        add(getSliceScaleComp());
        add(getPhoneTextArea());
        add(getMarkupPane());
    }

    private LinkedList<SpecOverlay> getDefaultSpecOverlays() {
        LinkedList<SpecOverlay> rList = new LinkedList<SpecOverlay>();
        if (gClipInfo == null) {
            return rList;
        }
        int HIST = 10;
        SpecOverlay tTotalMagOverlay = new SpecOverlay(Color.RED.darker(), "Total Mag");
        SpecOverlay tPitchOverlay = new SpecOverlay(Color.BLUE, "Pitch");
        SpecOverlay tPitchFeatureOverlay = new SpecOverlay(Color.GREEN, "PitchFeat");
        for (int i = 0; i < gClipInfo.getAudioLength(); i++) {
            PitchFeature tPFeat = new PitchFeature(gClipInfo.getFrameHistory(i, HIST));
            tTotalMagOverlay.add(new Point(i, (gClipInfo.getSpecTotalMag(i) > 5120000) ? 0 : Spectrum.NUM_FREQUENCIES - gClipInfo.getSpecTotalMag(i) / 20000));
            tPitchOverlay.add(new Point(i, Spectrum.NUM_FREQUENCIES - gClipInfo.getFrame(i).getCepstrum().getPitch() - 1));
            int temp = Spectrum.NUM_FREQUENCIES - (Integer) tPFeat.getFeature(1) - 1;
            if (temp < 1) {
                temp = 1;
            }
            if (temp > Spectrum.NUM_FREQUENCIES - 1) {
                temp = Spectrum.NUM_FREQUENCIES - 1;
            }
            tPitchFeatureOverlay.add(new Point(i, temp));
        }
        gToggledOverlays = new HashMap<Character, SpecOverlay>();
        gToggledOverlays.put('1', tTotalMagOverlay);
        gToggledOverlays.put('2', tPitchOverlay);
        gToggledOverlays.put('3', tPitchFeatureOverlay);
        return rList;
    }

    private SpecOverlay getFrameOverlay() {
        if (gFrameOverlay == null) gFrameOverlay = new SpecOverlay(Color.YELLOW, "FRAME");
        return gFrameOverlay;
    }

    /******************
	 * gets the phone text area, where current phone info is displayed
	 * @return
	 */
    private JTextArea getPhoneTextArea() {
        if (gPhoneTextArea == null) {
            gPhoneTextArea = new JTextArea();
            gPhoneTextArea.append("Frame: " + gCurrentFrame + '\n');
            for (PhoneTrack pt : gPhoneTracks) gPhoneTextArea.append(pt.getName() + ':' + pt.getPhone(gCurrentFrame) + '\n');
            gPhoneTextArea.setLocation(getSliceComp().getLocation().x + getSliceComp().getWidth() + BORDER, getSliceComp().getLocation().y);
            gPhoneTextArea.setSize(200, (gPhoneTracks.size() + 1) * 16);
            gPhoneTextArea.setEditable(false);
            gPhoneTextArea.setFocusable(false);
            gPhoneTextArea.setVisible(true);
        }
        return gPhoneTextArea;
    }

    /******************
	 * Gets the scroll pane for the overlay info (markup)
	 * @return
	 */
    private JScrollPane getMarkupPane() {
        if (gMarkupPane == null) {
            gMarkupPane = new JScrollPane(getMarkupTextArea());
            gMarkupPane.setLocation(getPhoneTextArea().getLocation().x, getPhoneTextArea().getLocation().y + getPhoneTextArea().getHeight());
            gMarkupPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            gMarkupPane.setSize(200, getSliceComp().getHeight() + getSliceScaleComp().getHeight() - getPhoneTextArea().getHeight());
            gMarkupPane.setVisible(true);
        }
        return gMarkupPane;
    }

    /*********************
	 * Gets the markup text
	 */
    private JTextArea getMarkupTextArea() {
        if (gMarkupTextArea == null) {
            gMarkupTextArea = new JTextArea();
            for (SpecOverlay s : gToggledOverlays.values()) for (int i : s.getFrame(gCurrentFrame)) gMarkupTextArea.append(s.getName() + ':' + (Spectrum.NUM_FREQUENCIES - i) + '\n');
            gMarkupTextArea.setEditable(false);
            gMarkupTextArea.setFocusable(false);
            gMarkupTextArea.setVisible(true);
        }
        return gMarkupTextArea;
    }

    /************************
	 * Gets the drop-down box for Spectrum/Cepstrum selection
	 * @return
	 */
    private JComboBox getSpecComboBox() {
        if (gSpecComboBox == null) {
            Object[] tItems = { "Spectrum", "Cepstrum" };
            gSpecComboBox = new JComboBox(tItems);
            gSpecComboBox.setSize(120, 30);
            gSpecComboBox.setLocation(BORDER * 3 + getPlayButton().getWidth() + getLocationTextBox().getWidth(), BORDER);
            gSpecComboBox.setBackground(this.getBackground());
            gSpecComboBox.setFocusable(false);
            gSpecComboBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent mEvent) {
                    String s = (String) ((JComboBox) mEvent.getSource()).getSelectedItem();
                    if (s.equals("Spectrum")) bShowSpectrum = true; else if (s.equals("Cepstrum")) bShowSpectrum = false;
                    refresh();
                }
            });
            gSpecComboBox.setVisible(true);
        }
        return gSpecComboBox;
    }

    private JScrollPane getFullCompPane() {
        if (gFullCompPane == null) {
            JPanel tFullCompAndPhoneTrackPanel = new JPanel();
            tFullCompAndPhoneTrackPanel.setLayout(new BoxLayout(tFullCompAndPhoneTrackPanel, BoxLayout.PAGE_AXIS));
            tFullCompAndPhoneTrackPanel.add(getFullComp());
            for (JImageComponent tComp : gPhoneTrackComps) {
                tFullCompAndPhoneTrackPanel.add(tComp);
            }
            gFullCompPane = new JScrollPane(tFullCompAndPhoneTrackPanel);
            gFullCompPane.setLocation(BORDER, BORDER + getLocationTextBox().getHeight() + BORDER);
            gFullCompPane.setSize(gClipInfo.getImageWidth() + 4 > MAX_SPEC_WIDTH ? MAX_SPEC_WIDTH : gClipInfo.getImageWidth() + 4, gClipInfo.getHeight() + (gPhoneTrackComps.size() + 2) * PhoneTrack.PHONE_HEIGHT);
            SpecMouseListener tListener = new SpecMouseListener();
            getFullComp().addMouseListener(tListener);
            getFullComp().addMouseMotionListener(tListener);
            gFullCompPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            gFullCompPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }
        return gFullCompPane;
    }

    /*************************
	 * Gets the spectrum component
	 * @return
	 */
    private JImageComponent getFullComp() {
        if (gFullComp == null) {
            gFullComp = new JImageComponent(gClipInfo.getFullSpecImage(gCurrentFrame, gSpecOverlay));
            gFullComp.setAutoscrolls(true);
            gFullComp.setVisible(true);
        }
        return gFullComp;
    }

    /***********************
	 * Gets the slice component
	 * @return
	 */
    private JImageComponent getSliceComp() {
        if (gSliceComp == null) {
            gSliceComp = new JImageComponent(gClipInfo.getSpecSliceImage(gCurrentFrame, gSpecOverlay));
            gSliceComp.setLocation(BORDER, getFullCompPane().getLocation().y + getFullCompPane().getHeight() + HypothPhoneTrack.PHONE_HEIGHT * gPhoneTrackComps.size() + BORDER);
            gSliceComp.setSize(gSliceComp.getPreferredSize());
            gSliceComp.setVisible(true);
        }
        return gSliceComp;
    }

    /*********************
	 * Gets the tiny ImageComponent that holds the slice scale
	 * @return
	 */
    private JImageComponent getSliceScaleComp() {
        if (gSliceScaleComp == null) {
            gSliceScaleComp = new JImageComponent(gClipInfo.getSliceScale());
            gSliceScaleComp.setLocation(getSliceComp().getLocation().x, getSliceComp().getLocation().y + getSliceComp().getHeight());
            gSliceScaleComp.setSize(gSliceScaleComp.getPreferredSize());
            gSliceScaleComp.setVisible(true);
        }
        return gSliceScaleComp;
    }

    /**************************
	 * Gets the play button, which is tied to gClipInfo's play method
	 * @return
	 */
    private JButton getPlayButton() {
        if (gPlayButton == null) {
            gPlayButton = new JButton("Play");
            gPlayButton.setLocation(BORDER, BORDER);
            gPlayButton.setSize(80, 30);
            gPlayButton.addActionListener(new PlayActionListener(gPlayButton));
            gPlayButton.setBackground(this.getBackground());
            gPlayButton.setFocusable(false);
            gPlayButton.setVisible(true);
        }
        return gPlayButton;
    }

    /*******************************
	 * Gets the text area that has the current frame-freq location of the mouse pointer
	 * @return
	 */
    private JTextArea getLocationTextBox() {
        if (gLocationTextBox == null) {
            gLocationTextBox = new JTextArea();
            gLocationTextBox.setLocation(BORDER + getPlayButton().getWidth() + BORDER, BORDER);
            gLocationTextBox.setSize(70, 30);
            gLocationTextBox.setEditable(false);
            gLocationTextBox.setFocusable(false);
            gLocationTextBox.setBackground(this.getBackground());
            gLocationTextBox.setText("Frame: 0\nFreq: 0");
            gLocationTextBox.setVisible(true);
        }
        return gLocationTextBox;
    }

    private void refresh() {
        getMarkupTextArea().setText("");
        for (SpecOverlay s : gToggledOverlays.values()) for (int i : s.getFrame(gCurrentFrame)) gMarkupTextArea.append(s.getName() + ':' + (Spectrum.NUM_FREQUENCIES - i) + '\n');
        getPhoneTextArea().setText("");
        getPhoneTextArea().append("Frame: " + gCurrentFrame + '\n');
        for (PhoneTrack pt : gPhoneTracks) getPhoneTextArea().append(pt.getName() + ':' + pt.getPhone(gCurrentFrame) + '\n');
        getFullComp().setImage(bShowSpectrum ? gClipInfo.getFullSpecImage(gCurrentFrame, gSpecOverlay) : gClipInfo.getFullCepsImage(gCurrentFrame, gCepsOverlay));
        getSliceComp().setImage(bShowSpectrum ? gClipInfo.getSpecSliceImage(gCurrentFrame, gSpecOverlay) : gClipInfo.getCepsSliceImage(gCurrentFrame));
        repaint();
    }

    private void refresh(int mFrame) {
        if (mFrame < 0) gCurrentFrame = 0; else if (mFrame > gClipInfo.getAudioLength() - 1) gCurrentFrame = gClipInfo.getAudioLength() - 1; else gCurrentFrame = mFrame;
        refresh();
    }

    /*************************************
	 * MouseListener inner class
	 */
    private class SpecMouseListener implements MouseInputListener, ActionListener {

        public void mousePressed(MouseEvent mEvent) {
            int tFrame = mEvent.getX() / FRAME_WIDTH_IN_PIXELS;
            refresh(tFrame);
        }

        public void mouseMoved(MouseEvent mEvent) {
            int tFrame = mEvent.getX() / FRAME_WIDTH_IN_PIXELS;
            int tFreq = Spectrum.NUM_FREQUENCIES - mEvent.getY() - 1;
            getLocationTextBox().setText("Frame: " + tFrame + "\nFreq: " + tFreq);
        }

        public void mouseReleased(MouseEvent arg0) {
        }

        public void mouseClicked(MouseEvent mEvent) {
        }

        public void mouseEntered(MouseEvent arg0) {
        }

        public void mouseExited(MouseEvent arg0) {
        }

        public void mouseDragged(MouseEvent mEvent) {
            int tFrame = mEvent.getX() / FRAME_WIDTH_IN_PIXELS;
            getFullComp().scrollRectToVisible(new Rectangle(mEvent.getX(), mEvent.getY(), 1, 1));
            int tFreq = Spectrum.NUM_FREQUENCIES - mEvent.getY() - 1;
            getLocationTextBox().setText("Frame: " + tFrame + "\nFreq: " + tFreq);
            refresh(tFrame);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    /**************************
	 * getFrontEndData -- builds toggled overlays & phone hypoth track 
	 * from frontendbin 
	 * 
	 */
    public void getFrontEndData(String mFilename, Properties mProps) {
        if (gToggledOverlays == null) gToggledOverlays = new HashMap<Character, SpecOverlay>();
        if (mProps.getProperty("recognizercommandline") != null) {
            String tRecCmd = mProps.getProperty("recognizercommandline");
            String tRecBin = tRecCmd.split(" ")[0];
            String tPhoneTag = mProps.getProperty("phonetag");
            HashMap<Integer, Phone> tPhoneMap = new HashMap<Integer, Phone>();
            HashMap<String, Character> tAnnotTagMap = new HashMap<String, Character>();
            HashMap<Character, Color> tAnnotColorMap = new HashMap<Character, Color>();
            for (char c = '0'; c <= '9'; c++) {
                if (mProps.getProperty("annottag" + c) != null) tAnnotTagMap.put(mProps.getProperty("annottag" + c), c);
                if (mProps.getProperty("annotcolor" + c) != null) tAnnotColorMap.put(c, new Color(Integer.parseInt(mProps.getProperty("annotcolor" + c), 16)));
            }
            Scanner tScanner = null;
            Scanner tErrScanner = null;
            BufferedOutputStream tOut = null;
            RawSoundFile tSoundFile = null;
            if (!(new File(tRecBin)).exists()) {
                System.err.println("Error, cannot find binary " + tRecBin);
                return;
            }
            Process tProc = null;
            try {
                System.out.println("Attempting to run: " + tRecCmd);
                tProc = Runtime.getRuntime().exec(tRecCmd);
            } catch (IOException mIOE) {
                System.err.println("Errors running frontend! Attempting to run workaround script...");
                System.err.println("Ack! Can't run workaround! I give up!");
                return;
            }
            tScanner = new Scanner(tProc.getInputStream());
            tErrScanner = new Scanner(tProc.getErrorStream());
            tOut = new BufferedOutputStream(tProc.getOutputStream());
            try {
                tSoundFile = new RawSoundFile(mFilename);
                int b;
                while ((b = tSoundFile.read()) != -1) tOut.write(b);
                tOut.close();
            } catch (IOException mIOE) {
                System.err.println("Error piping sound file to recognizer!");
                System.err.println("Bailing...");
                return;
            }
            Pattern tPattern = Pattern.compile("([A-Z0-9]*) *([0-9]*)> *([\\S&&[^:]]*):? *([0-9-]*)");
            while (tScanner.hasNextLine()) {
                Matcher tMatcher = tPattern.matcher(tScanner.nextLine());
                if (tMatcher.matches()) {
                    if (tMatcher.group(1).equals(tPhoneTag)) {
                        tPhoneMap.put(Integer.parseInt(tMatcher.group(2)), new Phone(tMatcher.group(3), gVoicedPhoneList.contains(tMatcher.group(3)), gPhoneColorMap.get(tMatcher.group(3))));
                    } else {
                        if (tAnnotTagMap.containsKey(tMatcher.group(1))) {
                            if (!gToggledOverlays.containsKey(tAnnotTagMap.get(tMatcher.group(1)))) gToggledOverlays.put(tAnnotTagMap.get(tMatcher.group(1)), new SpecOverlay(tAnnotColorMap.get(tAnnotTagMap.get(tMatcher.group(1))), tMatcher.group(1)));
                            gToggledOverlays.get(tAnnotTagMap.get(tMatcher.group(1))).add(new Point(Integer.parseInt(tMatcher.group(2)), Integer.parseInt(tMatcher.group(3))));
                        }
                    }
                }
            }
            while (tErrScanner.hasNextLine()) System.err.println("Rec stderr: " + tErrScanner.nextLine());
            if (tPhoneMap.size() != 0) gPhoneTracks.add(new HypothPhoneTrack(tPhoneMap, gClipInfo.getAudioLength(), tPhoneTag));
        }
    }

    /*************
	 * KeyListener methods... typed, released & pressed.
	 */
    public void keyTyped(KeyEvent mKey) {
        if (mKey.getKeyChar() >= '0' && mKey.getKeyChar() <= '9') {
            SpecOverlay tOver = gToggledOverlays.get(mKey.getKeyChar());
            if (tOver != null) {
                if (bShowSpectrum) {
                    if (gSpecOverlay.contains(tOver)) gSpecOverlay.remove(tOver); else gSpecOverlay.add(tOver);
                } else {
                    if (gCepsOverlay.contains(tOver)) gCepsOverlay.remove(tOver); else gCepsOverlay.add(tOver);
                }
                refresh();
            }
        }
    }

    public void keyPressed(KeyEvent mKey) {
        if (mKey.getKeyCode() == KeyEvent.VK_RIGHT) {
            gCurrentFrame = Math.min(gCurrentFrame + 1, gClipInfo.getAudioLength() - 1);
            refresh();
        } else if (mKey.getKeyCode() == KeyEvent.VK_LEFT) {
            gCurrentFrame = Math.max(gCurrentFrame - 1, 0);
            refresh();
        }
    }

    public void keyReleased(KeyEvent arg0) {
    }

    public void actionPerformed(ActionEvent e) {
        Object tSrc = e.getSource();
        if (tSrc == getPlayButton()) {
            if (getPlayButton().getText().equals("Play")) {
                gClipInfo.play(gCurrentFrame);
                getPlayButton().setText("Stop");
            } else if (getPlayButton().getText().equals("Stop")) {
                getPlayButton().setText("Play");
            }
        } else if (e.getActionCommand().equals("open")) {
            this.load();
        }
    }

    private class PlayActionListener implements ActionListener {

        private final JButton gButton;

        private PlayerThread gPlayThread;

        public PlayActionListener(JButton mButton) {
            gButton = mButton;
        }

        public void actionPerformed(ActionEvent mEvent) {
            if (gButton == mEvent.getSource()) {
                if (gButton.getText().equals("Play")) {
                    gButton.setText("Stop");
                    gPlayThread = new PlayerThread(gClipInfo.getSoundFile(), gCurrentFrame, gButton);
                    gPlayThread.start();
                } else if (gButton.getText().equals("Stop")) {
                    gPlayThread.halt();
                }
            }
        }
    }

    private class PlayerThread extends Thread {

        private final SoundFile gFile;

        private final int gFrameStart;

        private final JButton gButton;

        private boolean gPlay;

        public PlayerThread(SoundFile mFile, int mStart, JButton mButton) {
            gFile = mFile;
            gFrameStart = mStart;
            gPlay = true;
            gButton = mButton;
        }

        public void halt() {
            gPlay = false;
            gButton.setText("Play");
        }

        public void run() {
            AudioFormat tFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000.0F, 16, 1, 2, 16000.0F, false);
            DataLine.Info tInfo = new DataLine.Info(SourceDataLine.class, tFormat);
            SourceDataLine tLineout = null;
            try {
                tLineout = (SourceDataLine) AudioSystem.getLine(tInfo);
                tLineout.open(tFormat);
            } catch (LineUnavailableException mLUE) {
                System.err.println("Error opening audio, sorry!");
                tLineout.close();
                return;
            }
            tLineout.start();
            try {
                gFile.seek(RawSoundFile.BYTES_PER_SAMPLE * RawSoundFile.FRAME_RATE_IN_SAMPLES * gFrameStart);
                byte[] tBuffer = new byte[RawSoundFile.BYTES_PER_SAMPLE];
                while (gPlay && gFile.read(tBuffer, 0, RawSoundFile.BYTES_PER_SAMPLE) > 0) {
                    tLineout.write(tBuffer, 0, RawSoundFile.BYTES_PER_SAMPLE);
                }
            } catch (IOException mIOE) {
                System.err.println("Error reading from raw file...");
                mIOE.printStackTrace();
            }
            halt();
        }
    }
}
