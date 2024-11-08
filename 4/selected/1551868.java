package org.scribble.validation;

import org.scribble.extensions.*;
import org.scribble.model.*;
import org.scribble.model.admin.ModelListener;

/**
 * This class implements the validation rule for the
 * ChannelList construct.
 */
@RegistryInfo(extension = ValidationRule.class)
public class ChannelListValidationRule extends AbstractValidationRule {

    /**
	 * This is the default constructor.
	 */
    public ChannelListValidationRule() {
    }

    /**
	 * This method determines whether the rule is appropriate for
	 * the supplied model object.
	 * 
	 * @param obj The model object
	 * @return Whether the rule is appropriate
	 */
    public boolean isSupported(ModelObject obj) {
        return (obj instanceof ChannelList);
    }

    /**
	 * This method validates the supplied model object.
	 * 
	 * @param obj The model object
	 * @param context The context
	 * @param l The listener
	 */
    public void validate(ModelObject obj, ValidationContext context, ModelListener l) {
        ChannelList elem = (ChannelList) obj;
        for (int i = 0; i < elem.getChannels().size(); i++) {
            context.validate(elem.getChannels().get(i), l);
        }
    }
}
