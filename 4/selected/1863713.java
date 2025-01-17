package gov.ca.dsm2.input.model.diff;

import gov.ca.dsm2.input.model.BoundaryInputs;
import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.Channels;
import gov.ca.dsm2.input.model.DSM2Model;
import gov.ca.dsm2.input.model.Gates;
import gov.ca.dsm2.input.model.Nodes;
import gov.ca.dsm2.input.model.Outputs;
import gov.ca.dsm2.input.model.Reservoirs;
import java.util.List;

/**
 * This object contains the result of differencing two DSM2Models. In other
 * words model2-model1 is something like this model (element refers to top level
 * elements only such as channels, nodes, reservoirs, gates, inputs, outputs,
 * etc not xsections or xsection layers that cannot exist independently of say
 * channels)
 * 
 * If an element in model2 is identical in model 1 then it is not present in the
 * diff
 * 
 * If an element in model2 is not identical in model 1, the it is present here
 * as the original element in model 1 and the new element in model 2
 * 
 * If an element in model 2 is not present in model 1, it is considered a pure
 * add and the element in model 1 is null
 * 
 * If an element in model 1 is not present in model 2, it is considered a pure
 * delete and the element in model 2 is null
 * 
 * @author nsandhu
 * 
 */
public class DSM2ModelDiff {

    private DSM2Model addToBase;

    private DSM2Model deleteFromBase;

    private DSM2Model changeToBase;

    public DSM2ModelDiff(DSM2Model modelBase, DSM2Model modelChanged) {
        addToBase = new DSM2Model();
        deleteFromBase = new DSM2Model();
        changeToBase = new DSM2Model();
        diff(modelBase.getChannels(), modelChanged.getChannels());
        diff(modelBase.getNodes(), modelChanged.getNodes());
        diff(modelBase.getReservoirs(), modelChanged.getReservoirs());
        diff(modelBase.getGates(), modelChanged.getGates());
        diff(modelBase.getInputs(), modelChanged.getInputs());
        diff(modelBase.getOutputs(), modelChanged.getOutputs());
    }

    private void diff(Outputs outputs, Outputs outputs2) {
    }

    private void diff(BoundaryInputs inputs, BoundaryInputs inputs2) {
    }

    private void diff(Gates gates, Gates gates2) {
    }

    private void diff(Reservoirs reservoirs, Reservoirs reservoirs2) {
    }

    private void diff(Nodes nodes, Nodes nodes2) {
    }

    public void diff(Channels channelsBase, Channels channelsChanged) {
        List<Channel> channels = channelsBase.getChannels();
        for (Channel channel : channels) {
            Channel channel2 = channelsChanged.getChannel(channel.getId());
            if (channel2 == null) {
                deleteFromBase.getChannels().addChannel(channel);
            } else {
                diff(channel, channel2);
            }
        }
        for (Channel channel : channelsChanged.getChannels()) {
            Channel channel2 = channelsBase.getChannel(channel.getId());
            if (channel2 == null) {
                addToBase.getChannels().addChannel(channel);
            }
        }
    }

    public void diff(Channel channelBase, Channel channelChanged) {
        if (DiffUtils.isApproxEqual(channelBase.getLength(), channelBase.getLength())) {
        }
    }
}
