package ircam.jmax.fts;

import java.io.*;
import java.util.*;
import ircam.fts.client.*;
import ircam.jmax.*;
import ircam.jmax.editors.project.*;

public class FtsAudioConfig extends FtsObject {

    protected FtsArgs args = new FtsArgs();

    static {
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("sampling_rates"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).setSamplingRates(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("buffer_sizes"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).setBufferSizes(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("sampling_rate"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).setSamplingRate(args.getInt(0));
            }
        });
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("buffer_size"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).setBufferSize(args.getInt(0));
            }
        });
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("inputs"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).setSources(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("outputs"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).setDestinations(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("insert"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).insertLabel(args.getInt(0), args.getInt(1), args.getAtoms(), 2, args.getLength());
            }
        });
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("remove"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).removeLabel(args.getInt(0));
            }
        });
        FtsObject.registerMessageHandler(FtsAudioConfig.class, FtsSymbol.get("clear"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsAudioConfig) obj).clear();
            }
        });
    }

    public FtsAudioConfig(FtsServer server, FtsObject parent, int id) {
        super(server, parent, id);
        labels = new Vector();
    }

    public FtsAudioConfig() throws IOException {
        super(JMaxApplication.getFtsServer(), JMaxApplication.getRootPatcher(), FtsSymbol.get("audio_config"));
        labels = new Vector();
        sources = new String[0];
        sourceChannels = new int[0];
        destinations = new String[0];
        destinationChannels = new int[0];
        bufferSizes = new String[0];
        samplingRates = new String[0];
    }

    public void setListener(ircam.jmax.editors.configuration.AudioConfigPanel listener) {
        this.listener = listener;
        for (Enumeration e = labels.elements(); e.hasMoreElements(); ) ((FtsAudioLabel) e.nextElement()).setListener(listener);
    }

    /*****************************************************/
    public void requestInsertLabel(int index, String label) {
        args.clear();
        args.addInt(index);
        args.addSymbol(FtsSymbol.get(label));
        try {
            send(FtsSymbol.get("insert"), args);
        } catch (IOException e) {
            System.err.println("FtsAudioConfig: I/O Error sending insert Message!");
            e.printStackTrace();
        }
    }

    public void requestRemoveLabel(int index) {
        args.clear();
        args.addInt(index);
        try {
            send(FtsSymbol.get("remove"), args);
        } catch (IOException e) {
            System.err.println("FtsAudioConfig: I/O Error sending remove Message!");
            e.printStackTrace();
        }
    }

    public void requestSetBufferSize(int bufferSize) {
        args.clear();
        args.addInt(bufferSize);
        try {
            send(FtsSymbol.get("buffer_size"), args);
        } catch (IOException e) {
            System.err.println("FtsAudioConfig: I/O Error sending buffer_size Message!");
            e.printStackTrace();
        }
    }

    public void requestSetSamplingRate(int sRate) {
        args.clear();
        args.addInt(sRate);
        try {
            send(FtsSymbol.get("sampling_rate"), args);
        } catch (IOException e) {
            System.err.println("FtsAudioConfig: I/O Error sending sampling_rate Message!");
            e.printStackTrace();
        }
    }

    /*************************************************************/
    void setSamplingRates(int nArgs, FtsAtom[] args) {
        samplingRates = new String[nArgs];
        for (int i = 0; i < nArgs; i++) samplingRates[i] = "" + args[i].intValue;
        if (listener != null) listener.samplingRatesChanged();
    }

    public String[] getSamplingRates() {
        return samplingRates;
    }

    void setSamplingRate(int sr) {
        samplingRate = sr;
        if (listener != null) listener.samplingRateChanged();
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    void setBufferSizes(int nArgs, FtsAtom[] args) {
        bufferSizes = new String[nArgs];
        for (int i = 0; i < nArgs; i++) bufferSizes[i] = "" + args[i].intValue;
        if (listener != null) listener.bufferSizesChanged();
    }

    public String[] getBufferSizes() {
        return bufferSizes;
    }

    void setBufferSize(int bs) {
        bufferSize = bs;
        if (listener != null) listener.bufferSizeChanged();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    void setSources(int nArgs, FtsAtom[] args) {
        sources = new String[nArgs / 2];
        sourceChannels = new int[nArgs / 2];
        int j = 0;
        for (int i = 0; i < nArgs; i += 2) {
            sources[j] = args[i].symbolValue.toString();
            sourceChannels[j] = args[i + 1].intValue;
            j++;
        }
        if (listener != null) listener.sourcesChanged();
    }

    public String[] getSources() {
        return sources;
    }

    public int getInDeviceChannels(String input) {
        int id = 0;
        for (int i = 0; i < sources.length; i++) if (sources[i].equals(input)) {
            id = i;
            break;
        }
        return sourceChannels[id];
    }

    void setDestinations(int nArgs, FtsAtom[] args) {
        destinations = new String[nArgs / 2];
        destinationChannels = new int[nArgs / 2];
        int j = 0;
        for (int i = 0; i < nArgs; i += 2) {
            destinations[j] = args[i].symbolValue.toString();
            destinationChannels[j] = args[i + 1].intValue;
            j++;
        }
        if (listener != null) listener.destinationsChanged();
    }

    public int getOutDeviceChannels(String output) {
        int id = 0;
        for (int i = 0; i < destinations.length; i++) if (destinations[i].equals(output)) {
            id = i;
            break;
        }
        return destinationChannels[id];
    }

    public String[] getDestinations() {
        return destinations;
    }

    void insertLabel(int index, int id, FtsAtom[] args, int offset, int nArgs) {
        FtsAudioLabel label = new FtsAudioLabel(JMaxApplication.getFtsServer(), JMaxApplication.getRootPatcher(), id, args, offset, nArgs);
        labels.insertElementAt(label, index);
        if (listener != null) label.setListener(listener);
        if (listener != null) listener.labelInserted(index, label);
    }

    void removeLabel(int index) {
        if (index >= 0 && index < labels.size()) labels.remove(index);
    }

    public Enumeration getLabels() {
        return labels.elements();
    }

    public FtsAudioLabel getLabelAt(int index) {
        return (FtsAudioLabel) labels.elementAt(index);
    }

    public FtsAudioLabel getLabelByName(String lb) {
        FtsAudioLabel label;
        for (Enumeration e = labels.elements(); e.hasMoreElements(); ) {
            label = (FtsAudioLabel) e.nextElement();
            if (lb.equals(label.getLabel())) return label;
        }
        return null;
    }

    public int getLabelIndex(FtsAudioLabel lb) {
        FtsAudioLabel label;
        int i = 0;
        for (Enumeration e = labels.elements(); e.hasMoreElements(); ) {
            label = (FtsAudioLabel) e.nextElement();
            if (lb.getLabel().equals(label.getLabel())) return i;
            i++;
        }
        return -1;
    }

    public void clear() {
        labels.removeAllElements();
    }

    private String[] sources;

    private int[] sourceChannels;

    private String[] destinations;

    private int[] destinationChannels;

    private String[] bufferSizes;

    private String[] samplingRates;

    private Vector labels;

    private int samplingRate;

    private int bufferSize;

    private ircam.jmax.editors.configuration.AudioConfigPanel listener;
}
