package model.channels;

import model.Probability;
import model.datapath.AbstractDataPathElement;

/**
 * A binary symmetric channel. This channel has a single channel bit error,
 * which is the probability of any given input bit to be inverted in the output.
 * 
 * @author gijs
 */
public class BSC extends AbstractDataPathElement<Boolean, Boolean> {

    private static final Probability DEFAULT_ERROR = new Probability(0.1);

    private Probability channelBitError;

    /**
	 * Constructs a new BSC with a default channel bit error (0.1).
	 */
    public BSC() {
        this(DEFAULT_ERROR);
    }

    /**
	 * Constructs a new BSC with a given channel bit error.
	 * 
	 * @param channelBitError
	 *            the channel bit error
	 * @precondition channelBitError!=null
	 */
    public BSC(Probability channelBitError) {
        assert channelBitError != null;
        this.channelBitError = channelBitError;
    }

    /**
	 * Returns the channel bit error.
	 * @return the channel bit error
	 */
    public Probability getChannelBitError() {
        return channelBitError;
    }

    public void write(Boolean data) {
        Boolean b;
        if (channelBitError.occurs()) {
            b = !data;
        } else {
            b = data;
        }
        getNext().write(b);
    }
}
