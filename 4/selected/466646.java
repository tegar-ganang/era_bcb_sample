package uk101.view;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import uk101.io.Stream;
import uk101.io.UK101InputStream;
import uk101.io.UK101OutputStream;
import uk101.machine.TapeRecorder;
import uk101.view.component.CassetteButton;
import uk101.view.component.DisplayText;
import uk101.view.component.TapeFormat;

/**
 * A visual representation of the cassette recorder.
 *
 * @author Baldwin
 */
public class CassetteView extends JInternalFrame implements ActionListener, ItemListener {

    private static final long serialVersionUID = 1L;

    TapeRecorder recorder;

    JLabel name;

    DisplayText format;

    CassetteButton record, play, stop;

    JFileChooser select;

    File tapeFile;

    int inFormat, outFormat;

    public CassetteView(TapeRecorder recorder) {
        super("Cassette Recorder", false, false, false, true);
        this.recorder = recorder;
        name = new JLabel(" ");
        format = new DisplayText(null, TapeFormat.MODE_UNSET, true);
        JPanel tp = new JPanel();
        tp.setLayout(new BoxLayout(tp, BoxLayout.X_AXIS));
        tp.setBorder(BorderFactory.createTitledBorder("Tape file"));
        tp.add(name);
        tp.add(Box.createHorizontalGlue());
        tp.add(format);
        JPanel bp = new JPanel();
        bp.setLayout(new BoxLayout(bp, BoxLayout.X_AXIS));
        bp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        record = new CassetteButton("●", "Rec", this);
        play = new CassetteButton("►", "Play", this);
        stop = new CassetteButton("■", "Stop", this);
        bp.add(record);
        bp.add(Box.createHorizontalStrut(5));
        bp.add(play);
        bp.add(Box.createHorizontalStrut(5));
        bp.add(stop);
        ButtonGroup group = new ButtonGroup();
        group.add(record.button);
        group.add(play.button);
        group.add(stop.button);
        stop.button.setSelected(true);
        select = new JFileChooser(new File("."));
        select.setDialogTitle(getTitle() + " - Select Tape");
        JButton open = new JButton("Open...");
        open.setAlignmentY(BOTTOM_ALIGNMENT);
        open.addActionListener(this);
        bp.add(Box.createHorizontalStrut(25));
        bp.add(open);
        Container content = getContentPane();
        content.add(tp, BorderLayout.NORTH);
        content.add(bp, BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        TapeFormat tf = new TapeFormat();
        select.setAccessory(tf);
        if (select.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            tapeFile = select.getSelectedFile();
            inFormat = tf.getInputFormat();
            outFormat = tf.getOutputFormat();
            loadTape();
        }
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (e.getItem() == record.button) {
                play.button.setEnabled(false);
                if (tapeFile != null) recordTape();
            } else if (e.getItem() == play.button) {
                record.button.setEnabled(false);
                if (tapeFile != null) playTape();
            } else if (e.getItem() == stop.button) {
                play.button.setEnabled(true);
                record.button.setEnabled(true);
                if (tapeFile != null) stopTape();
            }
        }
    }

    void loadTape() {
        name.setText(tapeFile.getName());
        format.setValue(TapeFormat.MODE_UNSET);
    }

    void recordTape() {
        OutputStream out = null;
        if (!tapeFile.exists()) {
            out = Stream.getOutputStream(tapeFile, outFormat);
        } else {
            String[] msg = { "File " + tapeFile.getPath() + " already exists.", "Are you sure you want to overwrite it?" };
            String[] opts = { "No", "Yes" };
            if (JOptionPane.showInternalOptionDialog(this, msg, getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, opts, opts[0]) == JOptionPane.NO_OPTION) {
                out = Stream.getOutputStream(tapeFile, outFormat);
            } else {
                stop.button.doClick();
            }
        }
        if (out != null) {
            format.setValue((out instanceof UK101OutputStream) ? TapeFormat.MODE_ASCII : TapeFormat.MODE_BINARY);
            recorder.setOutputTape(out);
        }
    }

    void playTape() {
        InputStream in = Stream.getInputStream(tapeFile, inFormat);
        if (in != null) {
            format.setValue((in instanceof UK101InputStream) ? TapeFormat.MODE_ASCII : TapeFormat.MODE_BINARY);
            recorder.setInputTape(in);
        }
    }

    void stopTape() {
        format.setValue(TapeFormat.MODE_UNSET);
        recorder.setInputTape(null);
        recorder.setOutputTape(null);
    }
}
