package roboResearch.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import roboResearch.*;
import roboResearch.engine.*;
import roboResearch.interfaces.*;
import simonton.gui.*;
import simonton.utils.*;
import aaronr.utils.*;

/**
 * @author Eric Simonton
 */
public class ThreadItem extends JPanel implements BattleRunner.Listener {

    private static final long serialVersionUID = 1L;

    private static int nextId = 0;

    private JPanel buttonPanel = null;

    private JButton startPauseButton = null;

    private JButton removeKillButton = null;

    private JLabel statusLabel = null;

    private JLabel preferLabel = null;

    private JSpinner preferSpinner = null;

    private final ThreadPanel threadPanel;

    private final ScoreHistory history;

    private final BattleQueue queue;

    private final BotRepository repository;

    private final int id = nextId++;

    private int threadNumber;

    private int curRound;

    private boolean keepRunning;

    private BattleRunner currentRunner;

    public ThreadItem(ThreadPanel threadPanel, ScoreHistory history, BattleQueue queue, BotRepository repository) {
        this.threadPanel = threadPanel;
        this.history = history;
        this.queue = queue;
        this.repository = repository;
        initialize();
        refresh();
    }

    public void setThreadNumber(int number) {
        this.threadNumber = number;
        setBorder(BorderFactory.createTitledBorder("Thread " + number));
    }

    public void pause() {
        keepRunning = false;
        refresh();
    }

    public void start() {
        keepRunning = true;
        maybeStartNewBattle();
    }

    public void kill() {
        keepRunning = false;
        if (currentRunner != null) {
            currentRunner.kill();
        }
    }

    public int getPreferredGroup() {
        return (Integer) preferSpinner.getValue();
    }

    public void setPrefferedGroup(int group) {
        preferSpinner.setValue(group);
    }

    public void battleComplete(final BattleResults results, final BattleRunner source) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    assert currentRunner == source;
                    currentRunner = null;
                    Battle battle = source.getBattle();
                    queue.cancelRunningBattle(battle);
                    if (results == null) {
                        keepRunning = false;
                    } else {
                        history.submitResults(battle, results);
                    }
                    maybeStartNewBattle();
                } catch (final Exception ex) {
                    handleException(ex);
                }
            }
        });
    }

    public void error(String message, Exception ex, BattleRunner source) {
        System.out.format("Thread %d: %s%n", threadNumber, message);
        if (ex != null) {
            ex.printStackTrace();
        }
    }

    public synchronized void lineOutput(final String line, BattleRunner source) {
        if (line.startsWith("Round ")) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    String asText = line.substring(6, line.indexOf(' ', 6));
                    curRound = Integer.parseInt(asText);
                    refresh();
                }
            });
        }
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension size = getPreferredSize();
        size.width = Integer.MAX_VALUE;
        return size;
    }

    private void maybeStartNewBattle() {
        if (!keepRunning || currentRunner != null) {
            refresh();
            return;
        }
        try {
            String group = preferSpinner.getValue().toString();
            Battle battle = queue.getBattleToRun(group, null, null, null);
            if (battle != null) {
                startBattle(battle);
            }
            refresh();
        } catch (Exception ex) {
            handleException(ex);
        }
    }

    private void startBattle(Battle battle) throws IOException, UniversalException {
        File threadDir = new File(Constants.WORKING_DIR, "copy" + id);
        File robotsDir = new File(threadDir, "robots");
        robotsDir.mkdirs();
        File[] existing = robotsDir.listFiles();
        Arrays.sort(existing);
        for (Bot bot : battle.getBots()) {
            File destFile = new File(robotsDir, bot.getJarName());
            if (Arrays.binarySearch(existing, destFile) < 0) {
                OutputStream dest = new FileOutputStream(destFile);
                InputStream source = repository.getBotJar(bot);
                FileUtils.copyFile(source, dest);
                dest.close();
                source.close();
            }
        }
        List<String> extraArgs = new ArrayList<String>();
        extraArgs.add("-nodisplay");
        curRound = -1;
        currentRunner = new BattleRunner(battle, 0, this, threadDir, extraArgs);
        new Thread(currentRunner).start();
    }

    private void refresh() {
        String status;
        if (currentRunner == null) {
            if (keepRunning) {
                status = "No more battles to run!";
                keepRunning = false;
            } else {
                status = "Paused";
            }
        } else {
            Battle battle = currentRunner.getBattle();
            Bot[] bots = battle.getBots();
            status = String.format("%s vs %s:", bots[0].getAlias(), bots[1].getAlias());
            if (curRound <= 0) {
                status += " starting ";
            } else {
                status = String.format("%s round %d of %d", status, curRound, battle.numRounds);
            }
        }
        statusLabel.setText(status);
        if (keepRunning) {
            startPauseButton.setText("Pause");
        } else {
            startPauseButton.setText(currentRunner == null ? "Start" : "Resume");
        }
        if (currentRunner == null) {
            removeKillButton.setText("Remove");
        } else {
            removeKillButton.setText("Kill");
        }
    }

    private void handleException(Exception ex) {
        keepRunning = false;
        refresh();
        ErrorMessageHandler.handle(ex);
    }

    private void initialize() {
        statusLabel = new JLabel("Paused");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        this.setLayout(new BorderLayout(3, 3));
        this.setBorder(BorderFactory.createTitledBorder("Thread"));
        this.add(getButtonPanel(), BorderLayout.SOUTH);
        this.add(statusLabel, BorderLayout.CENTER);
    }

    private JPanel getButtonPanel() {
        if (buttonPanel == null) {
            preferLabel = new JLabel();
            preferLabel.setText("Prefer Group: ");
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(getButtonPanel(), BoxLayout.X_AXIS));
            buttonPanel.add(getStartPauseButton(), null);
            buttonPanel.add(Box.createHorizontalStrut(3));
            buttonPanel.add(getRemoveKillButton(), null);
            buttonPanel.add(Box.createHorizontalStrut(3));
            buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(preferLabel, null);
            buttonPanel.add(getPreferSpinner(), null);
        }
        return buttonPanel;
    }

    private JButton getStartPauseButton() {
        if (startPauseButton == null) {
            startPauseButton = new JButton("Start");
            GUIUtils.sizeToggleButton(startPauseButton, "Pause", "Resume");
            startPauseButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (startPauseButton.getText().equals("Pause")) {
                        pause();
                    } else {
                        start();
                    }
                }
            });
        }
        return startPauseButton;
    }

    private JButton getRemoveKillButton() {
        if (removeKillButton == null) {
            removeKillButton = new JButton("Remove");
            GUIUtils.sizeToggleButton(removeKillButton, "Kill");
            removeKillButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (removeKillButton.getText().equals("Remove")) {
                        threadPanel.removeThread(ThreadItem.this);
                    } else {
                        kill();
                    }
                }
            });
        }
        return removeKillButton;
    }

    private JSpinner getPreferSpinner() {
        if (preferSpinner == null) {
            preferSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9, 1));
            preferSpinner.setMaximumSize(getPreferredSize());
            preferSpinner.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    threadPanel.saveState();
                }
            });
        }
        return preferSpinner;
    }
}
