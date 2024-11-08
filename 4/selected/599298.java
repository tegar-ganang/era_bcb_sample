package com.rbnb.inds.exec.commands;

import com.rbnb.inds.exec.Port;
import java.io.File;
import org.xml.sax.Attributes;

/**
  * Root class of Demultiplexing commands.
  */
public abstract class Demux extends JavaCommand {

    /**
	  * @param demuxClass  The fully qualified demux Java class.
	  */
    public Demux(String demuxClass, Attributes attr) throws java.io.IOException {
        super(attr);
        File jarFile = new File(getCommandProperties().get("executableDirectory") + "/" + demuxClass.toLowerCase() + ".jar");
        addArgument("-jar");
        addArgument(jarFile.getCanonicalPath());
        String silentMode = attr.getValue("silentMode"), chanNameFromID = attr.getValue("chanNameFromID"), xmlFileStr = attr.getValue("xmlFile");
        String useEmbeddedTimestamp = attr.getValue("useEmbeddedTimestamp");
        if ("true".equals(silentMode)) addArgument("-S");
        if ("true".equals(chanNameFromID)) addArgument("-I");
        if (xmlFileStr != null) {
            addArgument("-x");
            addArgument(xmlFileStr);
            xmlFile = new File(getInitialDirectory() + '/' + xmlFileStr);
        } else xmlFile = null;
        if ("true".equals(useEmbeddedTimestamp)) addArgument("-t");
    }

    protected boolean doExecute() throws java.io.IOException {
        if (!getInputs().isEmpty()) {
            Port.RbnbPort rbnbPort = (Port.RbnbPort) getInputs().get(0);
            addArgument("-a");
            addArgument(rbnbPort.getPort());
            addArgument("-i");
            addArgument(rbnbPort.getChannel());
        }
        if (!getOutputs().isEmpty()) {
            Port.RbnbPort rbnbPort = (Port.RbnbPort) getOutputs().get(0);
            addArguments("-A", rbnbPort.getPort());
            if (rbnbPort.getName() != null) addArguments("-o", rbnbPort.getName());
            if (rbnbPort.getCacheFrames() > 0) addArguments("-c", String.valueOf(rbnbPort.getCacheFrames()));
            if (rbnbPort.getArchiveFrames() > 0) {
                if ("create".equals(rbnbPort.getArchiveMode())) addArguments("-K", String.valueOf(rbnbPort.getArchiveFrames())); else addArguments("-k", String.valueOf(rbnbPort.getArchiveFrames()));
            }
        }
        return super.doExecute();
    }

    public String getChildConfiguration() {
        if (xmlFile == null || !xmlFile.canRead()) return super.getChildConfiguration();
        return file2string(xmlFile);
    }

    public String getPrettyName() {
        String pretty = getClass().getSimpleName();
        if (!getOutputs().isEmpty()) pretty += " (" + getOutputs().get(0).getName() + ')';
        return pretty;
    }

    private final File xmlFile;
}
