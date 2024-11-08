package model.channels;

import model.Probability;
import model.datapath.AbstractDataPathElement;

/**
 * A binary asymmetric channel. This channel has an error type, which defines
 * whether this channel behaves like a normal asymmetric channel (exhibiting
 * 0-errors) or an inverted asymmetric channel (exhibiting 1-errors). The second
 * field (channelBitError) defines the occurance chance for this error.
 * 
 * @author gijs
 */
public class BAC extends AbstractDataPathElement<Boolean, Boolean> {

    public enum ErrorType {

        NORMAL("normal"), INVERTED("inverted");

        String name;

        ErrorType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private ErrorType errorType;

    private Probability channelBitError;

    /**
	 * Constructs a new BAC with a given channel bit error and error type.
	 * 
	 * @param channelBitError
	 *            the channel bit error
	 * @param errorType
	 *            The type of errors this BAC will exhibit. NORMAL stands for
	 *            0-errors (0->1 cross-over errors), and INVERTED for 1-errors.
	 * @precondition channelBitError!=null
	 */
    public BAC(ErrorType errorType, Probability channelBitError) {
        assert channelBitError != null;
        this.channelBitError = channelBitError;
        this.errorType = errorType;
    }

    /**
	 * Returns the channel bit error.
	 * 
	 * @return the channel bit error
	 */
    public Probability getChannelBitError() {
        return channelBitError;
    }

    public void write(Boolean data) {
        Boolean nextData;
        boolean possibleError = false;
        switch(errorType) {
            case NORMAL:
                possibleError = Boolean.FALSE.equals(data);
                break;
            case INVERTED:
                possibleError = Boolean.TRUE.equals(data);
                break;
        }
        if (possibleError && channelBitError.occurs()) {
            nextData = !data;
        } else {
            nextData = data;
        }
        getNext().write(nextData);
    }
}
