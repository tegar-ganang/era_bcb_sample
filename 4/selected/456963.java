package eln.editors;

import java.util.Vector;
import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.Line2D;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.text.*;
import java.net.*;
import java.lang.reflect.*;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import javax.sound.sampled.*;
import eln.nob.*;
import eln.editors.util.*;
import emsl.JavaShare.ImageLoad;

public class SoundEditor extends JFrame implements INBEditor {

    private static final String kIconPath = "icons";

    private static final String kEditorIcon = "EDsound.gif";

    private INBClient mElnClient;

    private NObNode mNob;

    private boolean mReadonly;

    AudioInputStream mAudioInputStream;

    SamplingGraph mSamplingGraph;

    Capture mCapture;

    Playback mPlayback;

    double mDuration;

    double mSeconds;

    JButton mPlayButton;

    JButton mExitButton;

    JButton mPauseButton;

    JButton mSaveButton;

    JButton mRecordButton;

    /**
     * Constructor
     */
    public SoundEditor() {
        super("Sound Editor");
        mPlayback = new Playback();
        mCapture = new Capture();
        mSamplingGraph = new SamplingGraph();
        AudioInputStream mAudioInputStream = null;
        mDuration = 0.0;
        mElnClient = null;
        mNob = null;
    }

    /**
     * Returns the default icon for this editor
    */
    public Image getIcon() {
        Image classImage = ImageLoad.getImage(this, this, kIconPath, kEditorIcon);
        return classImage;
    }

    /**
     * Returns a string with the editor name and version number
     */
    public String getLabel() {
        return "Sound Editor (v1.0)";
    }

    public void setReadOnly(boolean readonly) {
        mReadonly = readonly;
        if (mRecordButton != null && mSaveButton != null) {
            boolean enabled = !readonly;
            mRecordButton.setEnabled(enabled);
            mSaveButton.setEnabled(enabled);
        }
    }

    /**
     * This method stores a reference to the notebook client
     * @param aClient The notebook client
     */
    public void setClient(INBClient aClient) {
        mElnClient = aClient;
    }

    /**
     * This method starts up the editor.
     *
     * @param aNOb Sound annotation NOb from the ELN server.
     */
    public void Launch(NObNode aNOb) {
        Launch(aNOb, "");
    }

    public void Launch(NObNode aNOb, String langCode) {
        createDisplay();
        mNob = aNOb;
        if (mNob != null) {
            String mimetype = (String) mNob.get("dataType");
            byte[] buf = (byte[]) mNob.get("data");
            if (!"audio/x-wav".equals(mimetype)) {
                showErrorMessage("File type: " + mimetype + " not supported!");
            } else {
                if (buf != null) {
                    try {
                        createAudioInputStream(buf, null);
                    } catch (Exception e) {
                        showErrorMessage("Error loading sound file: " + e.toString(), e);
                    }
                    mPlayButton.setEnabled(true);
                } else {
                    showErrorMessage("Audio input is null", null);
                }
            }
        } else {
        }
        setVisible(true);
    }

    /**
     * main method to run standalone
     */
    public static void main(String args[]) {
        SoundEditor editor = new SoundEditor();
        editor.Launch(null);
    }

    /**
     * Print out info about security with applets
     * (this may be useless - see if we should remove)
     */
    public static void showSecurityDialog() {
        final String msg = "When running the Java Sound demo as an applet these\n" + "permissions are necessary in order to load/save files\n" + "and record audio :  \n\n" + "grant { \n" + "  permission java.io.FilePermission \"<<ALL FILES>>\", " + "\"read, write\";\n" + "  permission javax.sound.sampled.AudioPermission \"record\";\n" + "  permission java.util.PropertyPermission \"user.dir\", " + "\"read\";\n" + "}; \n\n" + "The permissions need to be added to the .java.policy file.";
        JOptionPane.showMessageDialog(null, msg, "Applet Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Builds the gui
     */
    protected void createDisplay() {
        mSamplingGraph = new SamplingGraph();
        mPlayButton = new JButton();
        mPauseButton = new JButton();
        mRecordButton = new JButton();
        mSaveButton = new JButton();
        mExitButton = new JButton();
        getContentPane().setLayout(new GridBagLayout());
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent evt) {
                close();
            }
        });
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 370;
        gridBagConstraints.ipady = 160;
        gridBagConstraints.insets = new Insets(13, 40, 0, 41);
        getContentPane().add(mSamplingGraph, gridBagConstraints);
        mPlayButton.setText("Play");
        mPlayButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                playCB(evt);
            }
        });
        mPlayButton.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(20, 120, 0, 43);
        getContentPane().add(mPlayButton, gridBagConstraints);
        mPauseButton.setText("Pause");
        mPauseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                pauseCB(evt);
            }
        });
        mPauseButton.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(20, 190, 0, 201);
        getContentPane().add(mPauseButton, gridBagConstraints);
        mRecordButton.setText("Record");
        mRecordButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                recordCB(evt);
            }
        });
        mRecordButton.setEnabled(!mReadonly);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(20, 48, 0, 115);
        getContentPane().add(mRecordButton, gridBagConstraints);
        mSaveButton.setText("Save");
        mSaveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                saveCB(evt);
            }
        });
        mSaveButton.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(20, 160, 23, 0);
        getContentPane().add(mSaveButton, gridBagConstraints);
        mExitButton.setText("Exit");
        mExitButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                exitCB(evt);
            }
        });
        mExitButton.setEnabled(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(20, 18, 23, 164);
        getContentPane().add(mExitButton, gridBagConstraints);
        pack();
    }

    /**
     * Loads the audio from a byte array.  This array could come from a 
     * file (if it was from NOb contents) OR directly from an input device
     * (i.e., microphone).  If it's from the input device, it won't have
     * file formatting added, so it will be different - therefore we handle
     * these two streams differently
     *
     * @param input ByteArrayInputStream encapsulating audio data
     * @param format If null, then the format is already incorporated into 
     * the data (i.e., it came from file).  
     */
    protected void createAudioInputStream(byte[] audioBytes, AudioFormat format) throws Exception {
        if (audioBytes != null) {
            ByteArrayInputStream input = new ByteArrayInputStream(audioBytes);
            byte[] rawBytes = null;
            long numFrames;
            if (format == null) {
                mAudioInputStream = AudioSystem.getAudioInputStream(input);
                numFrames = mAudioInputStream.getFrameLength();
            } else {
                numFrames = audioBytes.length / format.getFrameSize();
                mAudioInputStream = new AudioInputStream(input, format, numFrames);
                rawBytes = audioBytes;
            }
            float frameRate = mAudioInputStream.getFormat().getFrameRate();
            mDuration = ((long) (numFrames * 1000 / frameRate)) / 1000.0;
            mSamplingGraph.createWaveForm(rawBytes);
            mPlayButton.setEnabled(true);
        }
    }

    /**
     * Takes the raw data from the AudioInputStream and turns it into the
     * a WAVE formatted stream (as a byte array) so it can be saved in the
     * NOb object.
     */
    protected byte[] getAudioInputInWaveFormat() throws Exception {
        mAudioInputStream.reset();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (AudioSystem.write(mAudioInputStream, AudioFileFormat.Type.WAVE, output) == -1) {
            throw new IOException("Problems writing to file");
        }
        return output.toByteArray();
    }

    /**
     * Close the gui and free resources
     */
    protected void close() {
        if (mPlayback.playbackStarted()) {
            mPlayback.stop();
            mSamplingGraph.stop();
        } else if (mCapture.captureStarted()) {
            mCapture.stop();
            mSamplingGraph.stop();
        }
        dispose();
    }

    /**
     * Show an error message in the SamplingGraph area
     */
    protected void showErrorMessage(String msg) {
        showErrorMessage(msg, null);
    }

    protected void showErrorMessage(Exception e) {
        showErrorMessage(null, e);
    }

    protected void showErrorMessage(String msg, Exception e) {
        String message = null;
        if (msg != null && !msg.equals("")) {
            message = msg;
        } else if (e != null) {
            message = e.toString();
        }
        if (message != null) {
            JOptionPane.showMessageDialog(this, message, "SoundEditor Error", JOptionPane.ERROR_MESSAGE);
        }
        if (e != null) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the standard AudioFormat that this editor will always use.
     * (For simplicity, the user isn't allowed to control these settings).
     */
    protected AudioFormat getAudioFormat() {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
    }

    /**
     * Put this out here so it can be shared by the inner classes
     */
    protected void startPlayback() {
        mPlayback.start();
        mSamplingGraph.start();
        mRecordButton.setEnabled(false);
        mPauseButton.setEnabled(true);
        mPlayButton.setText("Stop");
    }

    /**
     * Put this out here so it can be shared by the inner classes
     */
    protected void stopPlayback() {
        mPlayback.stop();
        mSamplingGraph.stop();
        mRecordButton.setEnabled(!mReadonly);
        mPauseButton.setEnabled(false);
        mPlayButton.setText("Play");
    }

    /**
     * Toggles between play and stop
     */
    protected void playCB(ActionEvent evt) {
        if (mPlayButton.getText().startsWith("Play")) {
            startPlayback();
        } else {
            stopPlayback();
        }
    }

    /**
     * Toggles between pause and resume.  Can be used to pause either the
     * playback or the recording.
     */
    protected void pauseCB(ActionEvent evt) {
        if (mPauseButton.getText().startsWith("Pause")) {
            if (mCapture.captureStarted()) {
                mCapture.pauseCapture();
            } else if (mPlayback.playbackStarted()) {
                mPlayback.pausePlayback();
            }
            mPauseButton.setText("Resume");
        } else {
            if (mCapture.captureStarted()) {
                mCapture.resumeCapture();
            } else if (mPlayback.playbackStarted()) {
                mPlayback.resumePlayback();
            }
            mPauseButton.setText("Pause");
        }
    }

    /**
     * Put this out here so it can be shared by the inner classes
     */
    protected void startCapture() {
        mCapture.start();
        mSamplingGraph.reset();
        mSamplingGraph.start();
        mPlayButton.setEnabled(false);
        mPauseButton.setEnabled(true);
        mRecordButton.setText("Stop");
    }

    /**
     * Put this out here so it can be shared by the inner classes
     */
    protected void stopCapture() {
        mCapture.stop();
        mSamplingGraph.stop();
        mPlayButton.setEnabled(true);
        mPauseButton.setEnabled(false);
        mSaveButton.setEnabled(true);
        mRecordButton.setText("Record");
    }

    /**
     * Toggles between record and stop
     */
    protected void recordCB(ActionEvent evt) {
        if (mRecordButton.getText().startsWith("Record")) {
            startCapture();
        } else {
            stopCapture();
        }
    }

    /**
     * Saves the sound file to the Nob object
     */
    protected void saveCB(ActionEvent evt) {
        if (mNob == null) {
            mNob = new NOb();
        }
        try {
            byte[] buf = getAudioInputInWaveFormat();
            if (mElnClient != null) {
                mNob.put("data", buf);
                mNob.put("dataType", "audio/x-wav");
                mNob.put("label", "Audio annotation");
                mNob.put("editor", "eln.editors.SoundEditor");
                mElnClient.Save((NObNode) mNob);
            } else {
                FileOutputStream output = new FileOutputStream("test.wav");
                output.write(buf);
                output.close();
            }
            close();
        } catch (Exception e) {
            showErrorMessage("Unable to save audio file: " + e.toString(), e);
        }
    }

    /**
     * Saves to local file
     */
    protected void exitCB(ActionEvent evt) {
        close();
    }

    /**
     * Plays the current sound file by writing data to the output line in
     * a separate thread (so gui is not blocked).
     */
    public class Playback implements Runnable {

        SourceDataLine mLine = null;

        Thread mThread = null;

        public void start() {
            mThread = new Thread(this, "Playback");
            mThread.start();
        }

        public void stop() {
            mThread = null;
        }

        public boolean lineOpen() {
            return mLine != null && mLine.isOpen();
        }

        public boolean playbackStarted() {
            return (mThread != null);
        }

        public void pausePlayback() {
            mLine.stop();
        }

        public void resumePlayback() {
            mLine.start();
        }

        public long getMillisecondPosition() {
            return (long) (mLine.getMicrosecondPosition() / 1000);
        }

        public void run() {
            if (mAudioInputStream == null) {
                showErrorMessage("No loaded audio file to play!", null);
                stopPlayback();
                return;
            }
            try {
                mAudioInputStream.reset();
            } catch (Exception e) {
                showErrorMessage("Unable to reset the stream!", e);
                stopPlayback();
                return;
            }
            AudioFormat format = mAudioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                showErrorMessage("Line matching " + info + " not supported.", null);
                stopPlayback();
                return;
            }
            try {
                mLine = (SourceDataLine) AudioSystem.getLine(info);
                mLine.open(format);
            } catch (LineUnavailableException ex) {
                showErrorMessage("Unable to open the line:", ex);
                stopPlayback();
                return;
            }
            int lineBufferSize = mLine.getBufferSize();
            byte[] data = new byte[lineBufferSize];
            int numBytesRead = 0;
            mLine.start();
            while (mThread != null) {
                try {
                    if ((numBytesRead = mAudioInputStream.read(data)) < 0) {
                        break;
                    }
                    mLine.write(data, 0, numBytesRead);
                } catch (Exception e) {
                    showErrorMessage("Error during playback:", e);
                    break;
                }
            }
            if (mThread != null) {
                mLine.drain();
                stopPlayback();
            }
            mLine.stop();
            mLine.close();
            mLine = null;
        }
    }

    /** 
     * Reads data from the input device and writes to the audio stream in a
     * separate thread (so gui is not blocked)
     */
    class Capture implements Runnable {

        TargetDataLine mLine = null;

        Thread mThread;

        public void start() {
            mThread = new Thread(this, "Capture");
            mThread.start();
        }

        public void stop() {
            mThread = null;
        }

        public boolean lineActive() {
            return mLine != null && mLine.isActive();
        }

        public boolean captureStarted() {
            return (mThread != null);
        }

        public void pauseCapture() {
            mLine.stop();
        }

        public void resumeCapture() {
            mLine.start();
        }

        public long getMillisecondPosition() {
            return (long) (mLine.getMicrosecondPosition() / 1000);
        }

        public void run() {
            mDuration = 0;
            mAudioInputStream = null;
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                showErrorMessage("Line matching " + info + " not supported.", null);
                stopCapture();
                return;
            }
            try {
                mLine = (TargetDataLine) AudioSystem.getLine(info);
                mLine.open(format);
            } catch (LineUnavailableException ex) {
                showErrorMessage("Unable to open the line: ", ex);
                stopCapture();
                return;
            } catch (SecurityException ex) {
                showErrorMessage(ex);
                stopCapture();
                showSecurityDialog();
                return;
            } catch (Exception ex) {
                showErrorMessage(ex);
                stopCapture();
                return;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int lineBufferSize = mLine.getBufferSize();
            int frameSize = format.getFrameSize();
            int numFrames = lineBufferSize / frameSize;
            int framesToRead = (int) (numFrames / 4);
            int readSize = framesToRead * frameSize;
            byte[] data = new byte[lineBufferSize];
            int numBytesRead;
            mLine.start();
            while (mThread != null) {
                if ((numBytesRead = mLine.read(data, 0, readSize)) == -1) {
                    break;
                }
                out.write(data, 0, numBytesRead);
            }
            mLine.stop();
            mLine.close();
            mLine = null;
            if (mThread != null) {
                stopCapture();
            }
            byte audioBytes[] = out.toByteArray();
            try {
                createAudioInputStream(audioBytes, format);
            } catch (Exception e) {
                showErrorMessage("Could not create audio input stream:", e);
            }
        }
    }

    /**
     * Runs a separate thread that shows the wave form currently playing
     * or being recorded.
     */
    class SamplingGraph extends JPanel implements Runnable {

        Thread mThread;

        Vector mLines = new Vector();

        String mErrorMsg = null;

        double mSeconds;

        Font font10 = new Font("serif", Font.PLAIN, 10);

        Font font12 = new Font("serif", Font.PLAIN, 12);

        Color jfcBlue = new Color(204, 204, 255);

        Color pink = new Color(255, 175, 175);

        public SamplingGraph() {
            setBackground(new Color(20, 20, 20));
        }

        public void showError(String message) {
            mErrorMsg = message;
            repaint();
            mErrorMsg = null;
        }

        /**
         * Reset the lines to clear the wave form drawing
         */
        public void reset() {
            mLines.removeAllElements();
        }

        /**
         * Create the graphics to display the current AudioInputStream
         */
        public void createWaveForm(byte[] audioBytes) {
            mLines.removeAllElements();
            AudioFormat format = mAudioInputStream.getFormat();
            if (audioBytes == null) {
                try {
                    int numFrames = (int) mAudioInputStream.getFrameLength();
                    int frameSize = (int) format.getFrameSize();
                    audioBytes = new byte[numFrames * frameSize];
                    mAudioInputStream.read(audioBytes);
                } catch (Exception ex) {
                    showErrorMessage(ex.toString(), ex);
                    return;
                }
            }
            Dimension d = getSize();
            int w = d.width;
            int h = d.height - 15;
            int[] audioData = null;
            if (format.getSampleSizeInBits() == 16) {
                int nlengthInSamples = audioBytes.length / 2;
                audioData = new int[nlengthInSamples];
                if (format.isBigEndian()) {
                    for (int i = 0; i < nlengthInSamples; i++) {
                        int MSB = (int) audioBytes[2 * i];
                        int LSB = (int) audioBytes[2 * i + 1];
                        audioData[i] = MSB << 8 | (255 & LSB);
                    }
                } else {
                    for (int i = 0; i < nlengthInSamples; i++) {
                        int LSB = (int) audioBytes[2 * i];
                        int MSB = (int) audioBytes[2 * i + 1];
                        audioData[i] = MSB << 8 | (255 & LSB);
                    }
                }
            } else if (format.getSampleSizeInBits() == 8) {
                int nlengthInSamples = audioBytes.length;
                audioData = new int[nlengthInSamples];
                if (format.getEncoding().toString().startsWith("PCM_SIGN")) {
                    for (int i = 0; i < audioBytes.length; i++) {
                        audioData[i] = audioBytes[i];
                    }
                } else {
                    for (int i = 0; i < audioBytes.length; i++) {
                        audioData[i] = audioBytes[i] - 128;
                    }
                }
            }
            int frames_per_pixel = audioBytes.length / format.getFrameSize() / w;
            byte my_byte = 0;
            double y_last = 0;
            int numChannels = format.getChannels();
            for (double x = 0; x < w && audioData != null; x++) {
                int idx = (int) (frames_per_pixel * numChannels * x);
                if (format.getSampleSizeInBits() == 8) {
                    my_byte = (byte) audioData[idx];
                } else {
                    my_byte = (byte) (128 * audioData[idx] / 32768);
                }
                double y_new = (double) (h * (128 - my_byte) / 256);
                mLines.add(new Line2D.Double(x, y_last, x, y_new));
                y_last = y_new;
            }
            repaint();
        }

        public void paint(Graphics g) {
            Dimension d = getSize();
            int w = d.width;
            int h = d.height;
            int INFOPAD = 15;
            Graphics2D g2 = (Graphics2D) g;
            g2.setBackground(getBackground());
            g2.clearRect(0, 0, w, h);
            g2.setColor(Color.white);
            g2.fillRect(0, h - INFOPAD, w, INFOPAD);
            if (mErrorMsg != null) {
                g2.setColor(jfcBlue);
                g2.setFont(new Font("serif", Font.BOLD, 18));
                g2.drawString("ERROR", 5, 20);
                AttributedString as = new AttributedString(mErrorMsg);
                as.addAttribute(TextAttribute.FONT, font12, 0, mErrorMsg.length());
                AttributedCharacterIterator aci = as.getIterator();
                FontRenderContext frc = g2.getFontRenderContext();
                LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
                float x = 5, y = 25;
                lbm.setPosition(0);
                while (lbm.getPosition() < mErrorMsg.length()) {
                    TextLayout tl = lbm.nextLayout(w - x - 5);
                    if (!tl.isLeftToRight()) {
                        x = w - tl.getAdvance();
                    }
                    tl.draw(g2, x, y += tl.getAscent());
                    y += tl.getDescent() + tl.getLeading();
                }
            } else if (mCapture.captureStarted()) {
                g2.setColor(Color.black);
                g2.setFont(font12);
                g2.drawString("Length: " + String.valueOf(mSeconds), 3, h - 4);
            } else {
                g2.setColor(Color.black);
                g2.setFont(font12);
                g2.drawString("Length: " + String.valueOf(mDuration) + "  Position: " + String.valueOf(mSeconds), 3, h - 4);
                if (mAudioInputStream != null) {
                    g2.setColor(jfcBlue);
                    for (int i = 1; i < mLines.size(); i++) {
                        g2.draw((Line2D) mLines.get(i));
                    }
                    if (mSeconds != 0) {
                        double loc = mSeconds / mDuration * w;
                        g2.setColor(pink);
                        g2.setStroke(new BasicStroke(3));
                        g2.draw(new Line2D.Double(loc, 0, loc, h - INFOPAD - 2));
                    }
                }
            }
        }

        public void start() {
            mThread = new Thread(this, "SamplingGraph");
            mThread.start();
            mSeconds = 0;
        }

        public void stop() {
            mThread = null;
        }

        public void run() {
            mSeconds = 0;
            while (mThread != null) {
                if (mPlayback.lineOpen()) {
                    long milliseconds = mPlayback.getMillisecondPosition();
                    mSeconds = milliseconds / 1000.0;
                } else if (mCapture.lineActive()) {
                    long milliseconds = mCapture.getMillisecondPosition();
                    mSeconds = milliseconds / 1000.0;
                }
                try {
                    mThread.sleep(100);
                } catch (Exception e) {
                    break;
                }
                repaint();
                while ((mCapture.captureStarted() && !mCapture.lineActive()) || (mPlayback.playbackStarted() && !mPlayback.lineOpen())) {
                    try {
                        mThread.sleep(10);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            mSeconds = 0;
            repaint();
        }
    }
}
