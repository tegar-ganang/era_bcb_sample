package org.scribble.validation;

import org.scribble.extensions.*;
import org.scribble.model.*;
import org.scribble.model.admin.ModelIssue;
import org.scribble.model.admin.ModelListener;

/**
 * This class implements the validation rule for the
 * interaction.
 */
@RegistryInfo(extension = ValidationRule.class)
public class InteractionValidationRule extends AbstractValidationRule {

    /**
	 * This is the default constructor.
	 */
    public InteractionValidationRule() {
    }

    /**
	 * This method determines whether the rule is appropriate for
	 * the supplied model object.
	 * 
	 * @param obj The model object
	 * @return Whether the rule is appropriate
	 */
    public boolean isSupported(ModelObject obj) {
        return (obj instanceof Interaction);
    }

    /**
	 * This method validates the supplied model object.
	 * 
	 * @param obj The model object
	 * @param context The context
	 * @param l The listener
	 */
    public void validate(ModelObject obj, ValidationContext context, ModelListener l) {
        Interaction interaction = (Interaction) obj;
        if (interaction.getMessageSignature() != null) {
            context.validate(interaction.getMessageSignature(), l);
        }
        if (interaction.getFromRole() == null && (interaction.getChannel() == null || interaction.getChannel().getFromRole() == null)) {
            if (context.getLocatedRole() == null || interaction.getToRole() == null || context.getLocatedRole().equals(interaction.getToRole().getName())) {
                l.error(new ModelIssue(obj, org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.validation.Messages"), "_INTERACTION_ROLE", new String[] { "from" })));
            }
        }
        if (interaction.getToRole() == null && (interaction.getChannel() == null || interaction.getChannel().getToRole() == null)) {
            if (context.getLocatedRole() == null || interaction.getFromRole() == null || context.getLocatedRole().equals(interaction.getFromRole().getName())) {
                l.error(new ModelIssue(obj, org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.validation.Messages"), "_INTERACTION_ROLE", new String[] { "to" })));
            }
        }
    }
}
