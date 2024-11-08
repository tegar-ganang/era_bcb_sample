package ch.epfl.arni.jtossim;

import ch.epfl.arni.jtossim.gui.MainFrame;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import ch.epfl.arni.jtossim.events.SimulationEvent;

public class SimulatorBean {

    private SimulationProject project;

    private Thread thread;

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private SimulationResults logMessages = new SimulationResults();

    private DefaultStyledDocument output = new DefaultStyledDocument();

    private long currentTime;

    public static final String PROP_CURRENTTIME = "currentTime";

    public static final String PROP_ISRUNNING = "isRunning";

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public boolean isIsRunning() {
        return thread != null;
    }

    public StyledDocument getOutput() {
        return output;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    Object syncLoading = new Object();

    boolean monitoringFile = false;

    Thread monitorFileThread;

    boolean isMonitoringMessagesFile() {
        return monitoringFile;
    }

    void loadMessagesFile(File selectedFile) {
        stopMonitoringMessagesFile();
        synchronized (syncLoading) {
            try {
                clearResults();
                Thread readInput = createParserThread(new FileInputStream(selectedFile), false);
                readInput.start();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void stopMonitoringMessagesFile() {
        if (monitorFileThread != null && monitoringFile == true && monitorFileThread.isAlive()) monitorFileThread.interrupt();
        monitoringFile = false;
    }

    void startMonitoringMessagesFile(File selectedFile) {
        if (monitoringFile) return;
        synchronized (syncLoading) {
            try {
                clearResults();
                monitoringFile = true;
                monitorFileThread = createParserThread(new FileInputStream(selectedFile), true);
                monitorFileThread.start();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void clearResults() {
        getLogMessages().clear();
        try {
            this.output.remove(0, output.getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String createEventsC() {
        Vector<SimulationEvent> events = new Vector<SimulationEvent>(getProject().getEvents());
        Collections.sort(events, new Comparator<SimulationEvent>() {

            public int compare(SimulationEvent o1, SimulationEvent o2) {
                return (o1.getTime() == o2.getTime() ? 0 : (o1.getTime() < o2.getTime() ? -1 : 1));
            }
        });
        String times = "";
        String eventsD = "";
        for (SimulationEvent event : events) {
            eventsD += (eventsD.length() > 0 ? " ," : "") + event.getCDescription();
            times += (times.length() > 0 ? " ," : "") + event.getTime();
        }
        return "double times[" + events.size() + "] = {" + times + "};\n" + "uint16_t events[" + events.size() + "][2] = {" + eventsD + "};\n #define EVENTS_COUNT " + events.size() + "\n";
    }

    private String createNoiseC() throws IOException {
        String noiseString = "";
        double[][] noise = getProject().loadNoise();
        for (double noiseSample : noise[1]) {
            noiseString += (noiseString.length() > 0 ? ", " : "") + noiseSample;
        }
        return "#define NOISE_LEN " + noise[1].length + "\n" + "double noise [" + noise[1].length + "] = { " + noiseString + "};\n";
    }

    private String createLinkGainsC() throws IOException {
        String linkGainsString = "";
        int size = getProject().getLinkLayerModelParameters().getNumNodes();
        for (int i = 0; i < size; i++) {
            linkGainsString += "{";
            for (int j = 0; j < size; j++) {
                linkGainsString += String.format(Locale.US, "%4f", getProject().getLinkLayerModelParameters().getModel().getLinkGain()[i][j]) + ",";
            }
            linkGainsString += "},\n";
        }
        return "double gains [" + size + "][" + size + "] = { " + linkGainsString + "};\n";
    }

    private String createChannelsC() {
        String channels = "";
        for (String chan : project.getChannels()) {
            channels += "\"" + chan + "\", ";
        }
        return "std::string channels[] = {" + channels + "};\n" + "#define CHANNELS_COUNT " + project.getChannels().size() + "\n";
    }

    public void createCDriver() throws IOException {
        FileWriter os = new FileWriter(getProject().getSourcePath() + File.separator + "driver.c");
        os.write("#include <stdlib.h>\n");
        os.write("#include <string>\n");
        os.write("#define COMPILED_DATE \"" + new Date() + "\"\n");
        os.write("#define SOURCE_PATH \"" + project.getSourcePath() + "\"\n");
        os.write("#define MAX_NODES " + project.getLinkLayerModelParameters().getNumNodes() + "\n");
        os.write(createChannelsC());
        os.write(createEventsC());
        os.write(createLinkGainsC());
        os.write(createNoiseC());
        InputStream is = SimulatorBean.class.getResourceAsStream("resources/driver.c");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            os.write(line + "\n");
        }
        br.close();
        os.close();
    }

    public void compileCDriver(String outputFile) {
        final String command = " make sim " + (String) project.getPlatform() + "; " + " g++ -g -O3 -c -o driver.o driver.c -I$TOSDIR/lib/tossim/ ; " + " g++  -O3 -o driver driver.o simbuild/micaz/tossim.o simbuild/micaz/sim.o simbuild/micaz/c-support.o ;" + " cp driver \"" + outputFile + "\" ;" + " echo 'Created driver'";
        if (thread == null) {
            thread = new Thread(new Runnable() {

                public void run() {
                    try {
                        createCDriver();
                        writeConfig();
                        runCommand(project.getSourcePath(), "bash", new String[] { "-c", command }, "");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    thread = null;
                    propertyChangeSupport.firePropertyChange(PROP_ISRUNNING, true, false);
                }
            });
            propertyChangeSupport.firePropertyChange(PROP_ISRUNNING, false, true);
            thread.start();
        }
    }

    private String createEvents() {
        Vector<SimulationEvent> events = new Vector<SimulationEvent>(getProject().getEvents());
        Collections.sort(events, new Comparator<SimulationEvent>() {

            public int compare(SimulationEvent o1, SimulationEvent o2) {
                return (o1.getTime() == o2.getTime() ? 0 : (o1.getTime() < o2.getTime() ? -1 : 1));
            }
        });
        String times = "";
        String eventsD = "";
        for (SimulationEvent event : events) {
            eventsD += (eventsD.length() > 0 ? " ," : "") + event.getPythonDescription();
            times += (times.length() > 0 ? " ," : "") + event.getTime();
        }
        return "times = [" + times + "]\n" + "events = [" + eventsD + "]\n";
    }

    private void createRunScript() throws IOException {
        FileWriter out3 = new FileWriter(getProject().getSourcePath() + File.separator + "runner.py");
        String script = "from TOSSIM import *\n" + "import sys\n" + "\n" + "maxValuesRead = 1000\n" + "\n" + "# Create simulation, configure output\n" + "t = Tossim([])\n";
        String script3 = "# Create topology\n" + "print \"Creating mote topology...\";\n" + "sys.stdout.flush()\n" + "r = t.radio()\n" + "f = open(\"topology.tmp\", \"r\")\n" + "lines = f.readlines()\n" + "for line in lines:\n" + "  s = line.strip().split(\" \")\n" + "  if (len(s) > 0):\n" + "    r.add(int(s[0]), int(s[1]), float(s[2]))\n" + "print \"Created mote topology.\";\n" + "sys.stdout.flush()\n" + "\n" + "# Add statistical noise model to nodes\n" + "print \"Creating noise models for the motes...\";\n" + "sys.stdout.flush()\n" + "noise = open(\"noise.tmp\", \"r\")\n" + "lines = noise.readlines()\n" + "valuesRead = 0\n" + "for line in lines:\n" + "  st = line.strip()\n" + "  if (st != \"\"):\n" + "    val = int(st)\n" + "    for i in range(0, numNodes):\n" + "      t.getNode(i).addNoiseTraceReading(val)\n" + "    valuesRead += 1\n" + "    if valuesRead >= maxValuesRead:\n" + "      break\n" + "print \"Data read preparing noise model\"\n" + "sys.stdout.flush()\n" + "for i in range(0, numNodes):\n" + "  t.getNode(i).createNoiseModel()\n" + "print \"Created noise models from\", valuesRead, \"trace readings.\";\n" + "sys.stdout.flush()\n" + "\n" + "# Run simulation\n" + "print \"Running the simulation...\";\n" + "t.runNextEvent()\n" + "time = t.time()\n" + "prev = t.time()\n" + "while True:\n" + "  if ( (t.time() - prev) > t.ticksPerSecond() ) :\n" + "        prev = t.time()\n" + "        print  \"TIME\", (t.time() / ( t.ticksPerSecond() / 1000) ) \n" + "        sys.stdout.flush()\n" + "  while times[0] * t.ticksPerSecond() <= t.time():\n" + "        print \"Running event with %f secs of delay\" % (times[0] - t.time() / t.ticksPerSecond() ) \n" + "        if events[0][0] == 0:\n" + "          print(\"DEBUG (\" + str(events[0][1]) + \"): Powering on node\")\n" + "          t.getNode(events[0][1]).turnOn()\n" + "          t.getNode(events[0][1]).bootAtTime(t.time() + 1)\n" + "        elif events[0][0] == 1:\n" + "          print(\"DEBUG (\" + str(events[0][1]) + \"): Powering off node\")\n" + "          t.getNode(events[0][1]).turnOff()\n" + "        elif events[0][0] == 2:\n" + "          print \"DEBUG (0): Stopping simulation\" \n" + "          exit(0)\n" + "        elif events[0][0] == 3:\n" + "          for i in range(0, numNodes):\n" + "              print(\"DEBUG (\" + str(i) + \"): Powering on node\")\n" + "              t.getNode(i).turnOn()\n" + "              t.getNode(i).bootAtTime(t.time() + 1)\n" + "        del times[0]\n" + "        del events[0]\n" + "        sys.stdout.flush()\n" + "  t.runNextEvent()\n" + "print \"Simulation over.\"\n" + "sys.stdout.flush()\n";
        out3.write(script);
        out3.write(createEvents());
        out3.write("numNodes = " + getProject().getLinkLayerModelParameters().getNumNodes() + "\n");
        for (int i = 0; i < getProject().getChannels().size(); i++) {
            out3.write("t.addChannel(\"" + getProject().getChannels().get(i) + "\", sys.stdout)\n");
        }
        out3.write(script3);
        out3.close();
    }

    private void setCurrentTime(long currentTime) {
        long oldCurrentTime = this.currentTime;
        this.currentTime = currentTime;
        propertyChangeSupport.firePropertyChange(PROP_CURRENTTIME, oldCurrentTime, currentTime);
    }

    public SimulationResults getLogMessages() {
        return logMessages;
    }

    public SimulationProject getProject() {
        return project;
    }

    public void setProject(SimulationProject project) {
        this.project = project;
    }

    public void simulate() {
        if (thread == null) {
            thread = new Thread(new Runnable() {

                public void run() {
                    try {
                        clearResults();
                        writeLinkGains();
                        writeNoise();
                        createRunScript();
                        runTossim();
                    } catch (IOException ex) {
                        Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    thread = null;
                    propertyChangeSupport.firePropertyChange(PROP_ISRUNNING, true, false);
                }
            });
            propertyChangeSupport.firePropertyChange(PROP_ISRUNNING, false, true);
            thread.start();
        }
    }

    private void writeConfig() {
        try {
            FileWriter fw = new FileWriter(project.getSourcePath() + File.separator + "config-jtossim.h");
            for (CompilationParameter param : project.getCompilationParameters()) {
                if (param.isEnabled()) {
                    if (param.getValue().equals("")) {
                        fw.write("#define " + param.getName() + "\n");
                    } else {
                        fw.write("#define " + param.getName() + " " + param.getValue() + "\n");
                    }
                }
            }
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void make() {
        if (thread == null) {
            thread = new Thread(new Runnable() {

                public void run() {
                    writeConfig();
                    runCommand(project.getSourcePath(), "make", new String[] { "sim", (String) project.getPlatform() }, "");
                    thread = null;
                    propertyChangeSupport.firePropertyChange(PROP_ISRUNNING, true, false);
                }
            });
            propertyChangeSupport.firePropertyChange(PROP_ISRUNNING, false, true);
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void runCommand(String directory, String command, String[] args, String inputText) {
        runCommand(directory, command, args, null, inputText);
    }

    public void runCommand(String directory, String command, String[] args, String[] env, String inputText) {
        try {
            try {
                this.output.remove(0, output.getLength());
            } catch (BadLocationException ex) {
                Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
            }
            String[] args2 = new String[args.length + 1];
            args2[0] = command;
            for (int i = 0; i < args.length; i++) args2[i + 1] = args[i];
            Process p = Runtime.getRuntime().exec(args2, env, new File(directory));
            final InputStreamReader input = new InputStreamReader(p.getInputStream());
            final BufferedReader bInput = new BufferedReader(input);
            final OutputStream output = p.getOutputStream();
            final InputStreamReader error = new InputStreamReader(p.getErrorStream());
            final BufferedReader bError = new BufferedReader(error);
            Thread readInput = new Thread(new Runnable() {

                public void run() {
                    SimpleAttributeSet set = new SimpleAttributeSet();
                    set.addAttribute(StyleConstants.Foreground, Color.white);
                    set.addAttribute(StyleConstants.FontFamily, Font.MONOSPACED);
                    try {
                        String line;
                        while ((line = bInput.readLine()) != null) {
                            try {
                                SimulatorBean.this.output.insertString(SimulatorBean.this.output.getLength(), line + "\n", set);
                            } catch (BadLocationException ex) {
                                Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } catch (IOException e) {
                    }
                }
            });
            Thread readError = new Thread(new Runnable() {

                public void run() {
                    SimpleAttributeSet set = new SimpleAttributeSet();
                    set.addAttribute(StyleConstants.Foreground, Color.red);
                    set.addAttribute(StyleConstants.FontFamily, Font.MONOSPACED);
                    try {
                        String line;
                        while ((line = bError.readLine()) != null) {
                            try {
                                SimulatorBean.this.output.insertString(SimulatorBean.this.output.getLength(), line + "\n", set);
                            } catch (BadLocationException ex) {
                                Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } catch (IOException e) {
                    }
                }
            });
            readError.start();
            readInput.start();
            output.write(inputText.getBytes());
            try {
                readError.join();
                readInput.join();
                p.waitFor();
            } catch (InterruptedException ex) {
                p.destroy();
                readError.interrupt();
                readInput.interrupt();
            }
            try {
                bError.close();
            } catch (IOException ex) {
            }
            try {
                bInput.close();
            } catch (IOException ex) {
            }
            try {
                output.close();
            } catch (IOException ex) {
            }
            return;
        } catch (IOException e1) {
        }
    }

    private Thread createParserThread(InputStream input, final boolean monitor) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        return new Thread(new Runnable() {

            long time;

            Vector<RunMessage> rows = new Vector<RunMessage>();

            StringBuffer logString = new StringBuffer();

            private void flushResults() {
                setCurrentTime(time);
                final Vector<RunMessage> toBeAdded = rows;
                rows = new Vector<RunMessage>();
                getLogMessages().addAll(toBeAdded);
                try {
                    StringBuffer logStringToBeAdded = logString;
                    logString = new StringBuffer();
                    output.insertString(output.getLength(), logStringToBeAdded.toString(), null);
                } catch (BadLocationException ex) {
                    Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Error err) {
                    Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, err);
                }
            }

            public void run() {
                String line = null;
                TimerTask update = new TimerTask() {

                    @Override
                    public void run() {
                        flushResults();
                    }
                };
                Timer t1 = new Timer();
                t1.schedule(update, 0, 1000);
                do {
                    try {
                        while ((line = reader.readLine()) != null) {
                            if (line.equals("\n")) {
                                continue;
                            }
                            if (line.startsWith("DEBUG")) {
                                final int nodeid = Integer.parseInt(line.substring(line.indexOf("(") + 1, line.indexOf(")")));
                                final String message = line.substring(line.indexOf(":") + 1);
                                final RunMessage log = new RunMessage(nodeid, message);
                                rows.add(log);
                            } else if (line.startsWith("TIME")) {
                                time = Long.parseLong(line.trim().substring(5));
                            } else {
                                logString.append(line + "\n");
                            }
                        }
                    } catch (IOException e) {
                    }
                    if (monitor) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                    }
                } while (monitor && !Thread.interrupted());
                try {
                    reader.close();
                } catch (IOException ex) {
                }
                t1.cancel();
                flushResults();
            }
        });
    }

    public void runTossim() {
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "python", "runner.py" }, null, new File(getProject().getSourcePath()));
            final InputStream input = p.getInputStream();
            final InputStream error = p.getErrorStream();
            final BufferedReader errorReader = new BufferedReader(new InputStreamReader(error));
            Thread readError = new Thread(new Runnable() {

                public void run() {
                    try {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            try {
                                output.insertString(output.getLength(), line, null);
                            } catch (BadLocationException ex) {
                                Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } catch (IOException e) {
                    }
                }
            });
            stopMonitoringMessagesFile();
            Thread readInput = createParserThread(input, false);
            readError.start();
            readInput.start();
            try {
                readError.join();
                readInput.join();
                p.waitFor();
            } catch (InterruptedException ex) {
                p.destroy();
                readError.interrupt();
                readInput.interrupt();
            }
            return;
        } catch (IOException e1) {
        }
    }

    protected int simulationEndTime = 7000;

    public static final String PROP_SIMULATIONENDTIME = "simulationEndTime";

    public int getSimulationEndTime() {
        return simulationEndTime;
    }

    public void setSimulationEndTime(int simulationEndTime) {
        int oldSimulationEndTime = this.simulationEndTime;
        this.simulationEndTime = simulationEndTime;
        propertyChangeSupport.firePropertyChange(PROP_SIMULATIONENDTIME, oldSimulationEndTime, simulationEndTime);
    }

    public void saveOutputAs(String filename) throws IOException {
        FileWriter output = new FileWriter(filename);
        try {
            output.append(this.output.getText(0, this.output.getLength()));
        } catch (BadLocationException ex) {
            Logger.getLogger(SimulatorBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        output.close();
    }

    private void writeLinkGains() throws IOException {
        FileWriter out1 = new FileWriter(getProject().getSourcePath() + File.separator + "topology.tmp");
        for (int i = 0; i < getProject().getLinkLayerModelParameters().getModel().getLinkGain().length; i++) {
            for (int j = 0; j < getProject().getLinkLayerModelParameters().getModel().getLinkGain()[i].length; j++) {
                if (i == j) {
                    continue;
                }
                out1.write(i + " " + j + " " + String.format(Locale.US, "%4f", getProject().getLinkLayerModelParameters().getModel().getLinkGain()[i][j]) + "\n");
            }
        }
        out1.close();
    }

    private void writeNoise() throws IOException {
        FileWriter out2 = new FileWriter(getProject().getSourcePath() + File.separator + "noise.tmp");
        double[][] noise;
        try {
            noise = getProject().loadNoise();
            if (noise[0].length <= 100) {
                throw new Exception("Noise trace should be at least 100 samples long");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.toString(), "Error loading noise trace", JOptionPane.ERROR_MESSAGE);
            throw new IOException("Error loading noise trace");
        }
        for (int i = 0; i < noise[1].length; i++) {
            out2.write((int) noise[1][i] + "\n");
        }
        out2.close();
    }
}
