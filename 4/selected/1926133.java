package lt.ku.ik.recon.presentation.gui.widgets;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import lt.ku.ik.recon.logic.service.LocaleService;
import lt.ku.ik.recon.logic.service.SerialPortService;
import lt.ku.ik.recon.logic.service.SettingsService;

/**
 *
 * @author linas
 */
public class MainWindowFrame extends MainWindowSketch {

    private LocaleService localeService;

    private SettingsService settingsService;

    private SerialPortService serialPortService;

    private SerialPort serialPort;

    private EmotionSelectorPanel emotionSelector = new EmotionSelectorPanel();

    private InputStream inputStream;

    private DataInputStream dataInputStream;

    private int[] buffer1;

    private int[] buffer2;

    private int buffer1Fill;

    private int buffer2Fill;

    private ArrayList<DrawingPanel> drawingPanelList = new ArrayList<DrawingPanel>();

    FileWriter fileWriter;

    BufferedWriter bufferedWriter;

    boolean record = false;

    public MainWindowFrame() {
        setPreferredSize(new Dimension(600, 800));
    }

    public void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSerialPortService(SerialPortService serialPortService) {
        this.serialPortService = serialPortService;
    }

    public void setupPanels() {
        for (int i = 0; i < settingsService.getSettings().getChannels(); i++) {
            drawingPanelList.add(new DrawingPanel());
            drawingPanelList.get(i).setMinimumSize(new Dimension(8000, 10));
            drawingPanelList.get(i).setPreferredSize(new Dimension(8000, 100));
            drawingPanelList.get(i).setSettingsService(settingsService);
            jPanelTop.add(drawingPanelList.get(i));
        }
        jPanelBottom.add(emotionSelector);
        jSplitPane.setDividerLocation(400);
    }

    public void setupListeners() {
        ActionListener emotionActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setStatusPanel(actionEvent);
            }
        };
        emotionSelector.jButtonAngry.addActionListener(emotionActionListener);
        emotionSelector.jButtonStressed.addActionListener(emotionActionListener);
        emotionSelector.jButtonConfused.addActionListener(emotionActionListener);
        emotionSelector.jButtonSatisfied.addActionListener(emotionActionListener);
        emotionSelector.jButtonHappy.addActionListener(emotionActionListener);
        emotionSelector.jButtonSick.addActionListener(emotionActionListener);
        emotionSelector.jButtonUncomfortable.addActionListener(emotionActionListener);
        emotionSelector.jButtonNeutral.addActionListener(emotionActionListener);
        emotionSelector.jButtonComfortable.addActionListener(emotionActionListener);
        emotionSelector.jButtonConfident.addActionListener(emotionActionListener);
        emotionSelector.jButtonDepressed.addActionListener(emotionActionListener);
        emotionSelector.jButtonTired.addActionListener(emotionActionListener);
        emotionSelector.jButtonIndiferent.addActionListener(emotionActionListener);
        emotionSelector.jButtonCalm.addActionListener(emotionActionListener);
        emotionSelector.jButtonRelaxed.addActionListener(emotionActionListener);
        ActionListener startActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                startDrawing(actionEvent);
            }
        };
        jButtonStart.addActionListener(startActionListener);
        jMenuItemStart.addActionListener(startActionListener);
        ActionListener stopActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stopDrawing(actionEvent);
            }
        };
        jButtonStop.addActionListener(stopActionListener);
        jMenuItemStop.addActionListener(stopActionListener);
        ActionListener recordActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                record(actionEvent);
            }
        };
        jButtonRecord.addActionListener(recordActionListener);
        jMenuItemRecord.addActionListener(recordActionListener);
        ActionListener aboutActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                about(actionEvent);
            }
        };
        jButtonAbout.addActionListener(aboutActionListener);
        jMenuItemAbout.addActionListener(aboutActionListener);
        ActionListener quitActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                quit(actionEvent);
            }
        };
        jButtonQuit.addActionListener(quitActionListener);
        jMenuItemQuit.addActionListener(quitActionListener);
        ActionListener graphActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                graph(actionEvent);
            }
        };
        jButtonGraph.addActionListener(graphActionListener);
        jMenuItemGraph.addActionListener(graphActionListener);
        ActionListener emotionsActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                emotions(actionEvent);
            }
        };
        jButtonEmotions.addActionListener(emotionsActionListener);
        jMenuItemEmotions.addActionListener(emotionsActionListener);
        ActionListener settingsActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                settings(actionEvent);
            }
        };
        jButtonSettings.addActionListener(settingsActionListener);
        jMenuItemSettings.addActionListener(settingsActionListener);
    }

    public void setupSerialListeners() {
        SerialPortEventListener serialEventListener = new SerialPortEventListener() {

            @Override
            public synchronized void serialEvent(SerialPortEvent serialPortEvent) {
                readSerialPort(serialPortEvent);
            }
        };
        serialPort = serialPortService.getSerialPort();
        try {
            inputStream = serialPort.getInputStream();
        } catch (IOException exception) {
            Dialog dialog = new Dialog(exception.toString());
            dialog.error();
        }
        dataInputStream = new DataInputStream(inputStream);
        try {
            serialPort.addEventListener(serialEventListener);
            serialPort.notifyOnDataAvailable(true);
        } catch (Exception exception) {
            Dialog dialog = new Dialog(exception.toString());
            dialog.error();
        }
    }

    public void removeSerialListeners() {
        serialPort.removeEventListener();
    }

    private void setStatusPanel(ActionEvent actionEvent) {
        String actionCommand = actionEvent.getActionCommand();
        statusLabel.setText(actionCommand);
    }

    private void startDrawing(ActionEvent actionEvent) {
        clearBuffer();
        Iterator iterator = drawingPanelList.iterator();
        while (iterator.hasNext()) {
            DrawingPanel drawingPanel = (DrawingPanel) iterator.next();
            drawingPanel.setGraphDrawn(true);
        }
        try {
            serialPortService.connect();
            setupSerialListeners();
            serialPortService.startTransmission();
        } catch (Exception exception) {
            Dialog dialog = new Dialog(exception.toString());
            dialog.error();
        }
        redraw();
    }

    private void stopDrawing(ActionEvent actionEvent) {
        if (serialPort != null) {
            try {
                serialPortService.stopTransmission();
                removeSerialListeners();
                serialPortService.disconnect();
            } catch (Exception exception) {
                Dialog dialog = new Dialog(exception.toString());
                dialog.error();
            }
        }
        redraw();
    }

    private void record(ActionEvent actionEvent) {
        AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
        record = abstractButton.getModel().isSelected();
        if (record) {
            try {
                fileWriter = new FileWriter("log.txt");
            } catch (IOException ex) {
                Logger.getLogger(MainWindowFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            bufferedWriter = new BufferedWriter(fileWriter);
        } else {
            try {
                bufferedWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(MainWindowFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void about(ActionEvent actionEvent) {
        notImplemented();
    }

    private void quit(ActionEvent actionEvent) {
        stopDrawing(actionEvent);
        System.exit(0);
    }

    private void graph(ActionEvent actionEvent) {
        AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        jButtonGraph.setSelected(selected);
        jMenuItemGraph.setSelected(selected);
        jScrollPaneTop.setVisible(selected);
        jSplitPane.setDividerLocation(0.5);
    }

    private void emotions(ActionEvent actionEvent) {
        AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        jButtonEmotions.setSelected(selected);
        jMenuItemEmotions.setSelected(selected);
        jScrollPaneBottom.setVisible(selected);
        jSplitPane.setDividerLocation(0.5);
    }

    private void settings(ActionEvent actionEvent) {
        SettingsWindowFrame settingsWindowFrame = new SettingsWindowFrame();
        settingsWindowFrame.setSettingsService(settingsService);
        settingsWindowFrame.setSerialPortService(serialPortService);
        settingsWindowFrame.setValues();
        settingsWindowFrame.setupListeners();
        settingsWindowFrame.pack();
        settingsWindowFrame.setVisible(true);
    }

    private void notImplemented() {
        Dialog dialog = new Dialog(localeService.getText("TEXT_NOT_IMPLEMENTED"));
        dialog.warning();
    }

    private void redraw() {
        Iterator iterator = drawingPanelList.iterator();
        while (iterator.hasNext()) {
            DrawingPanel drawingPanel = (DrawingPanel) iterator.next();
            drawingPanel.repaint();
        }
    }

    private void clearBuffer() {
        buffer1 = new int[settingsService.getSettings().getResolutionOfGraph()];
        buffer2 = new int[settingsService.getSettings().getResolutionOfGraph()];
        buffer1Fill = 0;
        buffer2Fill = 0;
    }

    private synchronized void readSerialPort(SerialPortEvent serialPortEvent) {
        if ((serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE)) {
            try {
                while (dataInputStream.available() >= 6) {
                    if (buffer1Fill >= settingsService.getSettings().getResolutionOfGraph()) {
                        System.arraycopy(buffer1, 1, buffer1, 0, buffer1.length - 1);
                        System.arraycopy(buffer2, 1, buffer2, 0, buffer2.length - 1);
                        buffer1[settingsService.getSettings().getResolutionOfGraph() - 1] = dataInputStream.readChar();
                        buffer2[settingsService.getSettings().getResolutionOfGraph() - 1] = dataInputStream.readChar();
                        if (record) {
                            bufferedWriter.write(buffer1[settingsService.getSettings().getResolutionOfGraph()]);
                            bufferedWriter.write("\t");
                            bufferedWriter.write(buffer2[settingsService.getSettings().getResolutionOfGraph()]);
                            bufferedWriter.write("\t");
                            bufferedWriter.write(emotionSelector.getSelectedEmotion().toString());
                            bufferedWriter.write("\n");
                        }
                    } else {
                        buffer1[buffer1Fill] = dataInputStream.readChar();
                        buffer2[buffer2Fill] = dataInputStream.readChar();
                        if (record) {
                            bufferedWriter.write(buffer1[buffer1Fill]);
                            bufferedWriter.write("\t");
                            bufferedWriter.write(buffer2[buffer2Fill]);
                            bufferedWriter.write("\t");
                            bufferedWriter.write(emotionSelector.getSelectedEmotion().toString());
                            bufferedWriter.write("\n");
                        }
                        buffer1Fill++;
                        buffer2Fill++;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(MainWindowFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        DrawingPanel drawingPanel1 = drawingPanelList.get(0);
        DrawingPanel drawingPanel2 = drawingPanelList.get(1);
        drawingPanel1.setBuffer(buffer1);
        drawingPanel2.setBuffer(buffer2);
        redraw();
    }
}
