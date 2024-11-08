package com.cameocontrol.cameo.gui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JTextField;
import com.cameocontrol.cameo.action.ACTSettingsShowGenralMod;
import com.cameocontrol.cameo.action.ActionInterpreter;
import com.cameocontrol.cameo.control.ConsoleInquiry;
import com.cameocontrol.cameo.control.ConsoleSettings;
import com.cameocontrol.cameo.control.ConsoleSettings.RecordMode;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsShowGeneralGUI extends JPanel implements ActionListener {

    private ConsoleInquiry _console;

    private ActionInterpreter _actInt;

    private String[] _rModes;

    private JComboBox _recordMode;

    private JTextField _numChannels;

    private JTextField _numDimmers;

    private JTextField _upTime;

    private JTextField _downTime;

    private JTextField _gotoCueTime;

    private JTextField _title;

    private JTextField _comment;

    private JButton _apply;

    private JButton _cancel;

    private int _initRecordMode;

    private int _initChannels;

    private int _initDimmers;

    private float _initUpTime;

    private float _initDownTime;

    private float _initGotoCueTime;

    private String _initTitle;

    private String _initComment;

    public SettingsShowGeneralGUI(ConsoleInquiry c, ActionInterpreter ai) {
        _console = c;
        _actInt = ai;
        ConsoleSettings cs = _console.getSettings();
        _rModes = new String[2];
        _rModes[0] = "Tracking";
        _rModes[1] = "Cue Only";
        setLayout(new GridBagLayout());
        GridBagConstraints cons = new GridBagConstraints();
        cons.fill = GridBagConstraints.BOTH;
        cons.gridx = 0;
        cons.gridy = 0;
        add(new JLabel("Record Mode"), cons);
        cons.gridx = 1;
        add(_recordMode = new JComboBox(_rModes), cons);
        selectRecordMode(cs);
        cons.gridx = 0;
        cons.gridy = 1;
        add(new JLabel("Number of Channels"), cons);
        cons.gridx = 1;
        add(_numChannels = new JTextField(Integer.toString(_initChannels = cs.getChannels())), cons);
        cons.gridx = 0;
        cons.gridy = 2;
        add(new JLabel("Number of Dimmers"), cons);
        cons.gridx = 1;
        add(_numDimmers = new JTextField(Integer.toString(_initDimmers = cs.getDimmers())), cons);
        cons.gridx = 0;
        cons.gridy = 3;
        add(new JLabel("Default Up Time"), cons);
        cons.gridx = 1;
        add(_upTime = new JTextField(Float.toString(_initUpTime = (cs.getDefaultUpTime() / (float) 1000))), cons);
        cons.gridx = 0;
        cons.gridy = 4;
        add(new JLabel("Default Down Time"), cons);
        cons.gridx = 1;
        add(_downTime = new JTextField(Float.toString(_initDownTime = (cs.getDefaultDownTime() / (float) 1000))), cons);
        cons.gridx = 0;
        cons.gridy = 5;
        add(new JLabel("Default Goto Cue Time"), cons);
        cons.gridx = 1;
        add(_gotoCueTime = new JTextField(Float.toString(_initGotoCueTime = (cs.getDefaultGotoCueTime() / (float) 1000))), cons);
        cons.gridx = 0;
        cons.gridy = 6;
        add(new JLabel("Show Title"), cons);
        cons.gridx = 1;
        add(_title = new JTextField(_initTitle = cs.getTitle()), cons);
        _title.setPreferredSize(new Dimension(300, 20));
        cons.gridx = 0;
        cons.gridy = 7;
        add(new JLabel("Show Comment"), cons);
        cons.gridx = 1;
        _comment = new JTextField(_initComment = cs.getComment());
        _comment.setPreferredSize(new Dimension(300, 20));
        add(_comment, cons);
        cons.gridx = 0;
        cons.gridy = 8;
        cons.gridx = 1;
        add(_apply = new JButton("Apply"), cons);
        _apply.addActionListener(this);
    }

    private void selectRecordMode(ConsoleSettings cs) {
        if (cs.getRecordMode() == RecordMode.TRACKING) _recordMode.setSelectedIndex(0); else if (cs.getRecordMode() == RecordMode.CUE_ONLY) _recordMode.setSelectedIndex(1);
        _initRecordMode = _recordMode.getSelectedIndex();
    }

    public void actionPerformed(ActionEvent event) {
        ConsoleSettings cs = _console.getSettings();
        if (event.getSource().equals(_apply)) {
            int channels = Integer.parseInt(_numChannels.getText());
            int dimmers = Integer.parseInt(_numDimmers.getText());
            float upTime = Float.parseFloat(_upTime.getText());
            float downTime = Float.parseFloat(_downTime.getText());
            float gotoCueTime = Float.parseFloat(_gotoCueTime.getText());
            ACTSettingsShowGenralMod mod = new ACTSettingsShowGenralMod();
            if (_initRecordMode != _recordMode.getSelectedIndex()) {
                if (_rModes[_recordMode.getSelectedIndex()].equals("Tracking")) {
                    mod.setRecordMode(RecordMode.TRACKING);
                } else if (_rModes[_recordMode.getSelectedIndex()].equals("Cue Only")) {
                    mod.setRecordMode(RecordMode.CUE_ONLY);
                }
            }
            if (channels != _initChannels) mod.setChannels(channels);
            if (dimmers != _initDimmers) mod.setDimmers(dimmers);
            if (upTime != _initUpTime) mod.setUpTime((int) (upTime * 1000));
            if (downTime != _initDownTime) mod.setDownTime((int) (downTime * 1000));
            if (gotoCueTime != _initGotoCueTime) mod.setGotoCueTime((int) (gotoCueTime * 1000));
            if (!_title.getText().equals(_initTitle)) mod.setTitle(_title.getText());
            if (!_comment.getText().equals(_initComment)) mod.setComment(_comment.getText());
            _actInt.interprete(mod);
            cs = _console.getSettings();
            selectRecordMode(cs);
            _numChannels.setText(Integer.toString(_initChannels = cs.getChannels()));
            _numDimmers.setText(Integer.toString(_initDimmers = cs.getDimmers()));
            _upTime.setText(Float.toString(_initUpTime = (cs.getDefaultUpTime() / (float) 1000)));
            _downTime.setText(Float.toString(_initDownTime = (cs.getDefaultDownTime() / (float) 1000)));
            _gotoCueTime.setText(Float.toString(_initGotoCueTime = (cs.getDefaultGotoCueTime() / (float) 1000)));
            _title.setText(_initTitle = cs.getTitle());
            _comment.setText(_initComment = cs.getComment());
        }
    }
}
