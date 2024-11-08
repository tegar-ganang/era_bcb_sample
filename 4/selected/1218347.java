package org.mypomodoro.gui.todo;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import org.mypomodoro.Main;
import org.mypomodoro.gui.ControlPanel;
import org.mypomodoro.gui.ImageIcons;
import org.mypomodoro.gui.MyPomodoroView;
import org.mypomodoro.model.Activity;
import org.mypomodoro.util.ColorUtil;
import org.mypomodoro.util.Labels;

/**
 * This class keeps the logic for setting a timer for a pomodoro and the
 * breaks after that.
 * 
 * @author nikolavp 
 * @author Phil Karoo
 * 
 */
public class Pomodoro {

    private final int SECOND = 1000;

    private final int MINUTE = 60 * SECOND;

    private final long POMODORO_LENGTH = ControlPanel.preferences.getPomodoroLength() * MINUTE;

    private final long POMODORO_BREAK_LENGTH = ControlPanel.preferences.getShortBreakLength() * MINUTE;

    private final long POMODORO_LONG_LENGTH = ControlPanel.preferences.getLongBreakLength() * MINUTE;

    private final SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");

    private final Timer pomodoroTimer;

    private long pomodoroLength = POMODORO_LENGTH;

    private long tmpPomodoroLength = POMODORO_LENGTH;

    private long shortBreakLength = POMODORO_BREAK_LENGTH;

    private long longBreakLength = POMODORO_LONG_LENGTH;

    private final JLabel pomodoroTime;

    private final ToDoListPanel panel;

    private TimerPanel timerPanel;

    private Activity currentToDo;

    private long time = pomodoroLength;

    private boolean inpomodoro = false;

    private Clip clip;

    private boolean isMute = false;

    public Pomodoro(ToDoListPanel panel) {
        pomodoroTime = panel.getPomodoroTime();
        pomodoroTime.setText(sdf.format(pomodoroLength));
        pomodoroTimer = new Timer(SECOND, new UpdateAction());
        this.panel = panel;
    }

    public void start() {
        pomodoroTimer.start();
        if (ControlPanel.preferences.getTicking() && !isMute) {
            tick();
        }
        if (isSystemTray()) {
            if (isSystemTrayMessage()) {
                MyPomodoroView.trayIcon.displayMessage("", Labels.getString("ToDoListPanel.Started"), TrayIcon.MessageType.NONE);
            }
            MyPomodoroView.trayIcon.setToolTip(Labels.getString("ToDoListPanel.Started"));
        }
        inpomodoro = true;
        panel.refreshIconLabels();
    }

    public void stop() {
        pomodoroTimer.stop();
        time = pomodoroLength;
        tmpPomodoroLength = pomodoroLength;
        pomodoroTime.setText(sdf.format(pomodoroLength));
        stopSound();
        if (inPomodoro() && isSystemTray()) {
            if (isSystemTrayMessage()) {
                MyPomodoroView.trayIcon.displayMessage("", Labels.getString("ToDoListPanel.Stopped"), TrayIcon.MessageType.NONE);
            }
            MyPomodoroView.trayIcon.setToolTip(Labels.getString("ToDoListPanel.Stopped"));
            MyPomodoroView.trayIcon.setImage(ImageIcons.MAIN_ICON.getImage());
        }
        inpomodoro = false;
        panel.refreshIconLabels();
    }

    public boolean stopWithWarning() {
        if (inpomodoro) {
            JFrame window = new JFrame();
            String title = Labels.getString("ToDoListPanel.Void pomodoro");
            String message = Labels.getString("ToDoListPanel.Are you sure to void this pomodoro?");
            message += "\n(" + Labels.getString("ToDoListPanel.please create an unplanned activity in order to record this interruption") + ")";
            int reply = JOptionPane.showConfirmDialog(window, message, title, JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                stop();
            }
        } else {
            stop();
        }
        return !inpomodoro;
    }

    class UpdateAction implements ActionListener {

        int pomSetNumber = 0;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (time >= 1) {
                time -= SECOND;
                refreshTime();
            } else {
                stopSound();
                if (ControlPanel.preferences.getRinging() && !isMute) {
                    ring();
                }
                if (inPomodoro()) {
                    currentToDo.incrementPoms();
                    currentToDo.setDate(new Date());
                    currentToDo.databaseUpdate();
                    PomodorosRemainingLabel.showRemainPomodoros(panel.getPomodorosRemainingLabel(), panel.getToDoList());
                    pomSetNumber++;
                    if (pomSetNumber == ControlPanel.preferences.getNbPomPerSet()) {
                        goInLongBreak();
                        pomSetNumber = 0;
                        if (isSystemTray()) {
                            if (isSystemTrayMessage()) {
                                MyPomodoroView.trayIcon.displayMessage("", Labels.getString("ToDoListPanel.Long break"), TrayIcon.MessageType.NONE);
                            }
                            MyPomodoroView.trayIcon.setToolTip(Labels.getString("ToDoListPanel.Long break"));
                        }
                    } else {
                        goInShortBreak();
                        if (isSystemTray()) {
                            if (isSystemTrayMessage()) {
                                MyPomodoroView.trayIcon.displayMessage("", Labels.getString("ToDoListPanel.Short break"), TrayIcon.MessageType.NONE);
                            }
                            MyPomodoroView.trayIcon.setToolTip(Labels.getString("ToDoListPanel.Short break"));
                        }
                    }
                    timerPanel.setStartColor(ColorUtil.BLACK);
                    panel.getToDoJList().init();
                    inpomodoro = false;
                } else {
                    if (currentToDo.isFinished()) {
                        stop();
                        timerPanel.setStart();
                        if (isSystemTray()) {
                            if (isSystemTrayMessage()) {
                                MyPomodoroView.trayIcon.displayMessage("", Labels.getString("ToDoListPanel.Finished"), TrayIcon.MessageType.NONE);
                            }
                            MyPomodoroView.trayIcon.setToolTip(Labels.getString("ToDoListPanel.Finished"));
                        }
                    } else {
                        if (ControlPanel.preferences.getTicking() && !isMute) {
                            tick();
                        }
                        timerPanel.setStartColor(ColorUtil.RED);
                        panel.getToDoJList().init();
                        inpomodoro = true;
                        if (isSystemTray()) {
                            if (isSystemTrayMessage()) {
                                MyPomodoroView.trayIcon.displayMessage("", Labels.getString("ToDoListPanel.Started"), TrayIcon.MessageType.NONE);
                            }
                            MyPomodoroView.trayIcon.setToolTip(Labels.getString("ToDoListPanel.Started"));
                        }
                        goInPomodoro();
                    }
                }
                panel.refreshIconLabels();
            }
        }

        private void goInPomodoro() {
            time = pomodoroLength;
            tmpPomodoroLength = pomodoroLength;
        }

        private void goInShortBreak() {
            time = shortBreakLength;
        }

        private void goInLongBreak() {
            time = longBreakLength;
        }
    }

    public void setLongBreak(long longBreakLength) {
        this.longBreakLength = longBreakLength;
    }

    public void setShortBreak(long shortBreak) {
        shortBreakLength = shortBreak;
    }

    public long getPomodoroLength() {
        return pomodoroLength;
    }

    public void setPomodoroLength(long pomodoroLength) {
        this.pomodoroLength = pomodoroLength;
    }

    public long getShortBreakLength() {
        return shortBreakLength;
    }

    public void setShortBreakLength(long shortBreakLength) {
        this.shortBreakLength = shortBreakLength;
    }

    public long getLongBreakLength() {
        return longBreakLength;
    }

    public void setLongBreakLength(long longBreakLength) {
        this.longBreakLength = longBreakLength;
    }

    public boolean inPomodoro() {
        return inpomodoro;
    }

    public void tick() {
        playSound(Main.class.getResourceAsStream("/sounds/ticking.wav"), true);
    }

    public void ring() {
        playSound(Main.class.getResourceAsStream("/sounds/ringing.wav"));
    }

    public void playSound(InputStream is) {
        playSound(is, false);
    }

    public void playSound(InputStream is, boolean continuously) {
        try {
            AudioInputStream ain = AudioSystem.getAudioInputStream(getStreamWithMarkReset(is));
            try {
                DataLine.Info info = new DataLine.Info(Clip.class, ain.getFormat());
                clip = (Clip) AudioSystem.getLine(info);
                clip.addLineListener(new LineListener() {

                    @Override
                    public void update(LineEvent event) {
                        DataLine line = (DataLine) event.getSource();
                        if (event.getType() == LineEvent.Type.STOP) {
                            line.flush();
                            line.close();
                        }
                    }
                });
                clip.open(ain);
                clip.loop(continuously ? Clip.LOOP_CONTINUOUSLY : 0);
                clip.start();
            } finally {
                ain.close();
            }
        } catch (Exception e) {
        }
    }

    public void stopSound() {
        if (clip != null) {
            clip.stop();
            clip = null;
        }
    }

    public void setCurrentToDo(Activity toDo) {
        currentToDo = toDo;
    }

    public Activity getCurrentToDo() {
        return currentToDo;
    }

    public void setTimerPanel(TimerPanel timerPanel) {
        this.timerPanel = timerPanel;
    }

    public TimerPanel getTimerPanel() {
        return timerPanel;
    }

    private InputStream getStreamWithMarkReset(InputStream stream) throws IOException {
        if (stream.markSupported()) {
            return stream;
        }
        ByteArrayOutputStream output = null;
        try {
            output = new ByteArrayOutputStream(stream.available());
            byte[] buf = new byte[2048];
            int read;
            while ((read = stream.read(buf)) > 0) {
                output.write(buf, 0, read);
            }
            return new ByteArrayInputStream(output.toByteArray());
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean isSystemTray() {
        return SystemTray.isSupported() && ControlPanel.preferences.getSystemTray();
    }

    private boolean isSystemTrayMessage() {
        return SystemTray.isSupported() && ControlPanel.preferences.getSystemTrayMessage();
    }

    public void mute() {
        if (inpomodoro) {
            stopSound();
        }
        isMute = true;
    }

    public void unmute() {
        if (inpomodoro) {
            tick();
        }
        isMute = false;
    }

    public void increaseTime() {
        time += MINUTE;
        tmpPomodoroLength += MINUTE;
        refreshTime();
    }

    public void decreaseTime() {
        if (time > MINUTE) {
            time -= MINUTE;
            tmpPomodoroLength -= MINUTE;
            refreshTime();
        }
    }

    private synchronized void refreshTime() {
        pomodoroTime.setText(sdf.format(time));
        if (inPomodoro() && isSystemTray()) {
            MyPomodoroView.trayIcon.setToolTip(sdf.format(time));
            int progressiveTrayIndex = (int) ((double) ((tmpPomodoroLength - time)) / (double) tmpPomodoroLength * 8);
            MyPomodoroView.trayIcon.setImage(ImageIcons.MAIN_ICON_PROGRESSIVE[progressiveTrayIndex].getImage());
        }
    }
}
