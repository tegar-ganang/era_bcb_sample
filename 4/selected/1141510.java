package ch.epfl.arni.jtossim;

import ch.epfl.arni.jtossim.gui.MainFrame;
import ch.epfl.arni.jtossim.events.SimulationEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.epfl.arni.jtossim.events.PowerOnAllEvent;
import ch.epfl.arni.jtossim.events.StopSimulationEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.beanutils.BeanUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class SimulationProject implements Serializable {

    protected String Platform = "micaz";

    public static final String PROP_PLATFORM = "Platform";

    /**
     * Get the value of Platform
     *
     * @return the value of Platform
     */
    public String getPlatform() {
        return Platform;
    }

    /**
     * Set the value of Platform
     *
     * @param Platform new value of Platform
     */
    public void setPlatform(String Platform) {
        String oldPlatform = this.Platform;
        this.Platform = Platform;
        propertyChangeSupport.firePropertyChange(PROP_PLATFORM, oldPlatform, Platform);
    }

    protected String sourcePath;

    public static final String PROP_SOURCEPATH = "sourcePath";

    /**
     * Get the value of sourcePath
     *
     * @return the value of sourcePath
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Set the value of sourcePath
     *
     * @param sourcePath new value of sourcePath
     */
    public void setSourcePath(String sourcePath) {
        String oldSourcePath = this.sourcePath;
        this.sourcePath = sourcePath;
        propertyChangeSupport.firePropertyChange(PROP_SOURCEPATH, oldSourcePath, sourcePath);
        reloadCompilationParameters();
    }

    protected String noisePath = "";

    public static final String PROP_NOISEPATH = "noisePath";

    /**
     * Get the value of noisePath
     *
     * @return the value of noisePath
     */
    public String getNoisePath() {
        return noisePath;
    }

    /**
     * Set the value of noisePath
     *
     * @param noisePath new value of noisePath
     */
    public void setNoisePath(String noisePath) {
        String oldNoisePath = this.noisePath;
        this.noisePath = noisePath;
        propertyChangeSupport.firePropertyChange(PROP_NOISEPATH, oldNoisePath, noisePath);
    }

    protected int numNoiseSamples = 200;

    public static final String PROP_NUMNOISESAMPLES = "numNoiseSamples";

    /**
     * Get the value of numNoiseSamples
     *
     * @return the value of numNoiseSamples
     */
    public int getNumNoiseSamples() {
        return numNoiseSamples;
    }

    /**
     * Set the value of numNoiseSamples
     *
     * @param numNoiseSamples new value of numNoiseSamples
     */
    public void setNumNoiseSamples(int numNoiseSamples) {
        int oldNumNoiseSamples = this.numNoiseSamples;
        this.numNoiseSamples = numNoiseSamples;
        propertyChangeSupport.firePropertyChange(PROP_NUMNOISESAMPLES, oldNumNoiseSamples, numNoiseSamples);
    }

    protected LinkLayerModelParameters linkLayerModelParameters = new LinkLayerModelParameters();

    public static final String PROP_LINKLAYERMODELPARAMETERS = "linkLayerModelParameters";

    /**
     * Get the value of linkLayerModelParameters
     *
     * @return the value of linkLayerModelParameters
     */
    public LinkLayerModelParameters getLinkLayerModelParameters() {
        return linkLayerModelParameters;
    }

    /**
     * Set the value of linkLayerModelParameters
     *
     * @param linkLayerModelParameters new value of linkLayerModelParameters
     */
    public void setLinkLayerModelParameters(LinkLayerModelParameters linkLayerModelParameters) {
        LinkLayerModelParameters oldLinkLayerModelParameters = this.linkLayerModelParameters;
        this.linkLayerModelParameters = linkLayerModelParameters;
        propertyChangeSupport.firePropertyChange(PROP_LINKLAYERMODELPARAMETERS, oldLinkLayerModelParameters, linkLayerModelParameters);
    }

    protected Vector<String> channels = new Vector<String>();

    public static final String PROP_CHANNELS = "channels";

    /**
     * Get the value of channels
     *
     * @return the value of channels
     */
    public Vector<String> getChannels() {
        return channels;
    }

    /**
     * Set the value of channels
     *
     * @param channels new value of channels
     */
    public void setChannels(Vector<String> channels) {
        Vector<String> oldChannels = this.channels;
        this.channels = channels;
        propertyChangeSupport.firePropertyChange(PROP_CHANNELS, oldChannels, channels);
    }

    public void addChannel(String channel) {
        Vector<String> newChannels = new Vector<String>(getChannels());
        newChannels.add(channel);
        setChannels(newChannels);
    }

    public void removeChannel(String channel) {
        if (getChannels().contains(channel)) {
            Vector<String> newChannels = new Vector<String>(getChannels());
            newChannels.remove(channel);
            setChannels(newChannels);
        }
    }

    public Vector<CompilationParameter> compilationParameters = new Vector<CompilationParameter>();

    public static final String PROP_COMPILATIONPARAMETERS = "compilationParameters";

    public Vector<CompilationParameter> getCompilationParameters() {
        return compilationParameters;
    }

    private void setCompilationParameters(Vector<CompilationParameter> compilationParameters) {
        Vector<CompilationParameter> oldCompilationParameters = this.compilationParameters;
        this.compilationParameters = compilationParameters;
        propertyChangeSupport.firePropertyChange(PROP_COMPILATIONPARAMETERS, oldCompilationParameters, compilationParameters);
    }

    public void reloadCompilationParameters() {
        Vector<CompilationParameter> oldParams = getCompilationParameters();
        Vector<CompilationParameter> newParams = new Vector<CompilationParameter>();
        try {
            FileReader reader = new FileReader(sourcePath + File.separator + "flags.desc");
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(":");
                if (fields.length < 4) continue;
                CompilationParameter param = new CompilationParameter(fields[0], (fields[1].equals("true") || fields[1].equals("yes")), fields[2], fields[3]);
                for (CompilationParameter oldParam : oldParams) {
                    if (oldParam.getName().equals(param.getName())) {
                        param.setValue(oldParam.getValue());
                        param.setEnabled(oldParam.isEnabled());
                    }
                }
                newParams.add(param);
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        setCompilationParameters(newParams);
    }

    protected ApplicationLayerModelParameters applicationLayerModelParameters = new ApplicationLayerModelParameters();

    public static final String PROP_APPLICATIONLAYERMODELPARAMETERS = "applicationLayerModelParameters";

    /**
     * Get the value of applicationLayerModelParameters
     *
     * @return the value of applicationLayerModelParameters
     */
    public ApplicationLayerModelParameters getApplicationLayerModelParameters() {
        return applicationLayerModelParameters;
    }

    /**
     * Set the value of applicationLayerModelParameters
     *
     * @param applicationLayerModelParameters new value of applicationLayerModelParameters
     */
    public void setApplicationLayerModelParameters(ApplicationLayerModelParameters applicationLayerModelParameters) {
        ApplicationLayerModelParameters oldApplicationLayerModelParameters = this.applicationLayerModelParameters;
        this.applicationLayerModelParameters = applicationLayerModelParameters;
        propertyChangeSupport.firePropertyChange(PROP_APPLICATIONLAYERMODELPARAMETERS, oldApplicationLayerModelParameters, applicationLayerModelParameters);
    }

    private Vector<SimulationEvent> events = new Vector<SimulationEvent>();

    public Vector<SimulationEvent> getEvents() {
        return events;
    }

    public void setEvents(Vector<SimulationEvent> events) {
        Vector<SimulationEvent> old = this.events;
        this.events = events;
        propertyChangeSupport.firePropertyChange("events", old, events);
    }

    public void addEvent(SimulationEvent event) {
        Vector<SimulationEvent> newEvents = new Vector<SimulationEvent>(events);
        newEvents.add(event);
        setEvents(newEvents);
    }

    public void removeEvent(SimulationEvent event) {
        Vector<SimulationEvent> newEvents = new Vector<SimulationEvent>(events);
        newEvents.remove(event);
        setEvents(newEvents);
    }

    private PropertyChangeSupport propertyChangeSupport;

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    double[][] loadNoise() throws FileNotFoundException, IOException {
        Vector<Double> noise = new Vector<Double>();
        FileReader reader = new FileReader(getNoisePath());
        BufferedReader bur = new BufferedReader(reader);
        int maxSamples = getNumNoiseSamples();
        String line = null;
        while ((line = bur.readLine()) != null) {
            if (!line.equals("")) noise.add(Double.parseDouble(line));
        }
        bur.close();
        double[][] ret = new double[2][Math.min(noise.size(), maxSamples)];
        for (int i = 0; i < noise.size() && i < maxSamples; i++) {
            ret[0][i] = (double) i;
            ret[1][i] = noise.get(i);
        }
        return ret;
    }

    public Vector<String> getAvailableDebugChannels() {
        HashSet<String> channelsInApp = new HashSet<String>();
        if (getSourcePath() != null && new File(getSourcePath()).exists()) {
            File appc = new File(getSourcePath() + File.separator + "simbuild" + File.separator + getPlatform() + "/app.c");
            if (appc.exists()) {
                FileReader reader = null;
                try {
                    reader = new FileReader(appc);
                    BufferedReader rdr = new BufferedReader(reader);
                    Pattern p = Pattern.compile(".*sim_log_debug\\([0-9]+U, \"([^\"]+)\".*");
                    String line;
                    while ((line = rdr.readLine()) != null) {
                        Matcher m = p.matcher(line);
                        if (m.matches()) {
                            String[] channelsOfThisLine = m.group(1).split(",");
                            for (String s : channelsOfThisLine) {
                                channelsInApp.add(s);
                            }
                        }
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SimulationProject.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(SimulationProject.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        Logger.getLogger(SimulationProject.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        Vector<String> results = new Vector<String>(channelsInApp);
        Collections.sort(results);
        return results;
    }

    public static SimulationProject fromXML(Element e) {
        SimulationProject project = new SimulationProject();
        project.setPlatform(e.getChildText("platform"));
        Vector<String> channels = new Vector<String>();
        for (Object channel : e.getChild("channels").getChildren("channel")) {
            channels.add(((Element) channel).getText());
        }
        project.setChannels(channels);
        if (e.getChild("events") != null) {
            Vector<SimulationEvent> events = new Vector<SimulationEvent>();
            for (Object event : e.getChild("events").getChildren("event")) {
                events.add(SimulationEvent.fromXML((Element) event));
            }
            project.setEvents(events);
        }
        project.setNoisePath(e.getChildText("noise-path"));
        if (e.getChild("compilation-parameters") != null) {
            Vector<CompilationParameter> parameters = new Vector<CompilationParameter>();
            for (Object param : e.getChild("compilation-parameters").getChildren("compilation-parameter")) {
                parameters.add(CompilationParameter.fromXML((Element) param));
            }
            project.setCompilationParameters(parameters);
        }
        try {
            project.setNumNoiseSamples(Integer.parseInt(e.getChildText("num-noise-samples")));
        } catch (NumberFormatException ex) {
        }
        project.setSourcePath(e.getChildText("source-path"));
        project.setApplicationLayerModelParameters(ApplicationLayerModelParameters.fromXML(e.getChild("application-layer-model-parameters")));
        project.setLinkLayerModelParameters(LinkLayerModelParameters.fromXML(e.getChild("link-layer-model-parameters")));
        return project;
    }

    public Element toXML() {
        Element root = new Element("project");
        Element e = new Element("platform");
        e.addContent(Platform);
        root.addContent(e);
        e = new Element("channels");
        for (String channel : channels) {
            Element e1 = new Element("channel");
            e1.addContent(channel);
            e.addContent(e1);
        }
        root.addContent(e);
        e = new Element("events");
        for (SimulationEvent event : events) {
            e.addContent(event.toXML());
        }
        root.addContent(e);
        e = new Element("noise-path");
        e.addContent(noisePath);
        root.addContent(e);
        e = new Element("num-noise-samples");
        e.addContent(numNoiseSamples + "");
        root.addContent(e);
        e = new Element("source-path");
        e.addContent(sourcePath);
        root.addContent(e);
        e = new Element("compilation-parameters");
        for (CompilationParameter p : compilationParameters) {
            e.addContent(p.toXML());
        }
        root.addContent(e);
        root.addContent(applicationLayerModelParameters.toXML());
        root.addContent(linkLayerModelParameters.toXML());
        return root;
    }

    public void saveAs(String file) {
        Document d = new Document(toXML());
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            XMLOutputter outputter = new XMLOutputter();
            outputter.output(d, out);
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void load(String file) {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File(file));
            SimulationProject loadedProject = fromXML(doc.getRootElement());
            BeanUtils.copyProperties(this, loadedProject);
            this.setCompilationParameters(loadedProject.getCompilationParameters());
            BeanUtils.copyProperties(getLinkLayerModelParameters(), loadedProject.getLinkLayerModelParameters());
            BeanUtils.copyProperties(getLinkLayerModelParameters().getModel(), loadedProject.getLinkLayerModelParameters().getModel());
            BeanUtils.copyProperties(getApplicationLayerModelParameters(), loadedProject.getApplicationLayerModelParameters());
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SimulationProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SimulationProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JDOMException e) {
            Logger.getLogger(SimulationProject.class.getName()).log(Level.SEVERE, null, e);
        } catch (NullPointerException e) {
            Logger.getLogger(SimulationProject.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(SimulationProject.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public SimulationProject() {
        propertyChangeSupport = new PropertyChangeSupport(this);
        addEvent(new PowerOnAllEvent());
        StopSimulationEvent ev = new StopSimulationEvent();
        ev.setTime(7000);
        addEvent(ev);
    }
}
