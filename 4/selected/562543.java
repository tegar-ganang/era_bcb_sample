package be.lassi.ui.show;

import be.lassi.ui.util.ValidationSupport;
import be.lassi.ui.util.Validator;

/**
 * Validates the parameters for creating a new show.
 */
public class NewShowParametersValidator extends Validator {

    private final ShowParameters parameters;

    /**
     * Constructs a new instance.
     *
     * @param parameters the parameters to be validated
     */
    public NewShowParametersValidator(final ShowParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(final ValidationSupport support) {
        new ChannelCountValidator().validate(support, parameters.getChannelCount());
        new SubmasterCountValidator().validate(support, parameters.getSubmasterCount());
        new CueCountValidator().validate(support, parameters.getCueCount());
    }
}
