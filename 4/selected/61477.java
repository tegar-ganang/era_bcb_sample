package galaxiia.ui.starter;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import javax.swing.*;
import galaxiia.ui.Galaxiia;
import galaxiia.ui.UI;
import org.jul.i18n.I18n;
import org.jul.ui.GBCMaker;

public class Console extends WindowAdapter implements KeyListener, ActionListener {

    private class ProcessStdInThread implements Runnable {

        public void run() {
            try {
                while (running) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    while (System.in.available() > 0) {
                        processStdIn.write(System.in.read());
                        processStdIn.flush();
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    private class ProcessStdOutThread implements Runnable {

        public void run() {
            try {
                String line;
                while ((line = processStdOut.readLine()) != null) {
                    stdout(line);
                }
            } catch (IOException e) {
            }
        }
    }

    private class ProcessStdErrThread implements Runnable {

        public void run() {
            try {
                String line;
                while ((line = processStdErr.readLine()) != null) {
                    stderr(line);
                }
            } catch (IOException e) {
            }
        }
    }

    private static final String BUTTON_KILL = "BUTTON_KILL";

    private static final String BUTTON_CLOSE = "BUTTON_CLOSE";

    private static final long serialVersionUID = 1L;

    private final I18n i18n = UI.getI18nInstance(getClass());

    private final Process process;

    private final boolean graphicMode;

    private final BufferedReader processStdOut;

    private final BufferedReader processStdErr;

    private final PrintWriter processStdIn;

    private JFrame frame = null;

    private JTextArea console = null;

    private JScrollBar scrollBar = null;

    private JTextField input;

    private JButton killButton;

    private JCheckBox scrollLock;

    private JCheckBox showOnStdOut;

    private JCheckBox showOnStdErr;

    private boolean running = false;

    private int returnCode;

    private void graphicalConsoleOut(String line) {
        console.append(line);
        console.append("\n");
        if (scrollLock.isSelected()) {
            scrollBar.setValue(scrollBar.getMaximum());
        }
    }

    private void graphicalConsoleStdOut(final String line) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                graphicalConsoleOut(line);
                if (showOnStdOut.isSelected()) {
                    ensureVisibleFrame();
                }
            }
        });
    }

    private void graphicalConsoleStdErr(final String line) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                graphicalConsoleOut(line);
                if (showOnStdErr.isSelected()) {
                    ensureVisibleFrame();
                }
            }
        });
    }

    private void stdout(String line) {
        if (graphicMode) {
            graphicalConsoleStdOut(line);
        } else {
            System.out.println(line);
        }
    }

    private void stderr(String line) {
        if (graphicMode) {
            graphicalConsoleStdErr(line);
        } else {
            System.err.println(line);
        }
    }

    private void sendInput() {
        processStdIn.println(input.getText());
        processStdIn.flush();
        input.setText(null);
    }

    private void ensureVisibleFrame() {
        frame.setVisible(true);
        frame.toFront();
        input.requestFocusInWindow();
    }

    private void close() {
        if (running && (showOnStdOut.isSelected() || showOnStdErr.isSelected())) {
            frame.setVisible(false);
        } else {
            frame.dispose();
        }
    }

    private void initFrame() {
        frame = new JFrame(Galaxiia.NOM + " - " + i18n.get("console"));
        JPanel panel = new JPanel(new GridBagLayout());
        console = new JTextArea(25, 60);
        console.setFont(Font.getFont("Monospaced"));
        console.setEditable(false);
        JPanel consolePanel = new JPanel(new GridBagLayout());
        consolePanel.setBorder(BorderFactory.createTitledBorder(i18n.get("console")));
        JScrollPane scrollPane = new JScrollPane(console);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollBar = scrollPane.getVerticalScrollBar();
        consolePanel.add(scrollPane, new GBCMaker(1, 1, 1, 1, GridBagConstraints.BOTH).setWeight(1, 1).setInsets(4));
        input = new JTextField();
        input.addKeyListener(this);
        consolePanel.add(input, new GBCMaker(1, 2, 1, 1, GridBagConstraints.HORIZONTAL).setWeightX(1).setInsets(4));
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder(i18n.get("control")));
        killButton = new JButton(i18n.get("kill-process"));
        killButton.setActionCommand(BUTTON_KILL);
        killButton.addActionListener(this);
        JButton closeButton = new JButton(i18n.get("close"));
        closeButton.setActionCommand(BUTTON_CLOSE);
        closeButton.addActionListener(this);
        showOnStdOut = new JCheckBox(i18n.get("show-std-out"), true);
        showOnStdErr = new JCheckBox(i18n.get("show-std-err"), true);
        scrollLock = new JCheckBox(i18n.get("scroll-lock"), true);
        controlPanel.add(showOnStdOut, new GBCMaker(1, 1, 1, 1, GridBagConstraints.HORIZONTAL).setWeightX(1));
        controlPanel.add(showOnStdErr, new GBCMaker(1, 2, 1, 1, GridBagConstraints.HORIZONTAL).setWeightX(1));
        controlPanel.add(scrollLock, new GBCMaker(1, 3, 1, 1, GridBagConstraints.HORIZONTAL).setWeightX(1));
        controlPanel.add(killButton, new GBCMaker(2, 1, 1, 1, GridBagConstraints.HORIZONTAL).setAnchor(GridBagConstraints.LINE_START));
        controlPanel.add(closeButton, new GBCMaker(2, 2, 1, 1, GridBagConstraints.HORIZONTAL).setAnchor(GridBagConstraints.LINE_START));
        panel.add(consolePanel, new GBCMaker(1, 1, 1, 1, GridBagConstraints.BOTH).setWeight(1, 1));
        panel.add(controlPanel, new GBCMaker(1, 2, 1, 1, GridBagConstraints.HORIZONTAL).setWeightX(1));
        frame.addWindowListener(this);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void actionPerformed(ActionEvent arg0) {
        if (BUTTON_CLOSE.equals(arg0.getActionCommand())) {
            close();
        } else if (BUTTON_KILL.equals(arg0.getActionCommand())) {
            if (JOptionPane.showConfirmDialog(frame, i18n.get("kill-confirm"), i18n.get("confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                process.destroy();
            }
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        close();
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            sendInput();
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    private Console(Process process, boolean graphicMode) throws InterruptedException {
        this.process = process;
        this.graphicMode = graphicMode && !GraphicsEnvironment.isHeadless();
        this.processStdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        this.processStdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.processStdIn = new PrintWriter(process.getOutputStream());
        if (graphicMode) {
            initFrame();
        } else {
            new Thread(new ProcessStdInThread()).start();
        }
        new Thread(new ProcessStdOutThread()).start();
        new Thread(new ProcessStdErrThread()).start();
        this.returnCode = process.waitFor();
        this.running = false;
        if (graphicMode) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    killButton.setEnabled(false);
                    input.setEnabled(false);
                    scrollLock.setEnabled(false);
                    showOnStdOut.setEnabled(false);
                    showOnStdErr.setEnabled(false);
                }
            });
        }
        stdout("");
        stdout(i18n.format("terminated", returnCode, new Date()));
    }

    static int initConsole(Process process, boolean graphicMode) throws InterruptedException {
        Console console = new Console(process, graphicMode);
        return console.returnCode;
    }
}
