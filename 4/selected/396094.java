package gov.ca.dsm2.input.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Outputs implements Serializable {

    private ArrayList<ChannelOutput> channelOutput;

    private ArrayList<ReservoirOutput> reservoirOutput;

    public Outputs() {
        channelOutput = new ArrayList<ChannelOutput>();
        reservoirOutput = new ArrayList<ReservoirOutput>();
    }

    public void addOutput(ChannelOutput output) {
        channelOutput.add(output);
    }

    public void addReservoirOutput(ReservoirOutput output) {
        reservoirOutput.add(output);
    }

    public List<ChannelOutput> getChannelOutputs() {
        return channelOutput;
    }

    public List<ReservoirOutput> getReservoirOutputs() {
        return reservoirOutput;
    }
}
