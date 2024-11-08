package org.scribble.projector;

import org.scribble.extensions.RegistryInfo;
import org.scribble.model.*;
import org.scribble.model.admin.ModelListener;

/**
 * This class provides the Interaction implementation of the
 * projector rule.
 */
@RegistryInfo(extension = ProjectorRule.class)
public class InteractionProjectorRule implements ProjectorRule {

    /**
	 * This method determines whether the projection rule is
	 * appropriate for the supplied model object.
	 * 
	 * @param obj The model object to be projected
	 * @return Whether the rule is relevant for the
	 * 				model object
	 */
    public boolean isSupported(ModelObject obj) {
        return (obj.getClass() == Interaction.class);
    }

    /**
	 * This method returns a new instance of the interaction model
	 * object.
	 * 
	 * @return The new interaction
	 */
    protected Interaction createInteraction() {
        return (new Interaction());
    }

    /**
	 * This method projects the supplied model object based on the
	 * specified role.<p>
	 * 
	 * @param model The model object
	 * @param role The role
	 * @param l The model listener
	 * @return The projected model object
	 */
    public ModelObject project(ProjectorContext context, ModelObject model, Role role, ModelListener l) {
        Interaction ret = createInteraction();
        Interaction source = (Interaction) model;
        boolean f_roleFound = false;
        ret.derivedFrom(source);
        if (source.getRequestLabel() != null) {
            ret.setRequestLabel(source.getRequestLabel());
        }
        if (source.getReplyToLabel() != null) {
            ret.setReplyToLabel(source.getReplyToLabel());
        }
        if (source.getFromRole() != null) {
            if (source.getFromRole().equals(role)) {
                f_roleFound = true;
            } else {
                Object state = context.getState(source.getFromRole().getName());
                if (state instanceof Role) {
                    Role r = new Role();
                    r.setName(source.getFromRole().getName());
                    r.derivedFrom(source.getFromRole());
                    ret.setFromRole(r);
                }
            }
        }
        if (source.getToRole() != null) {
            if (source.getToRole().equals(role)) {
                f_roleFound = true;
            } else {
                Object state = context.getState(source.getToRole().getName());
                if (state instanceof Role) {
                    Role r = new Role();
                    r.setName(source.getToRole().getName());
                    r.derivedFrom(source.getToRole());
                    ret.setToRole(r);
                }
            }
        }
        if (source.getChannel() != null) {
            Object state = context.getState(source.getChannel().getName());
            if (state instanceof Channel) {
                Channel c = new Channel();
                c.setName(source.getChannel().getName());
                c.derivedFrom(source.getChannel());
                ret.setChannel(c);
            }
            if (source.getChannel().getFromRole() != null && source.getChannel().getFromRole().equals(role)) {
                f_roleFound = true;
            } else if (source.getChannel().getToRole() != null && source.getChannel().getToRole().equals(role)) {
                f_roleFound = true;
            }
        }
        if (f_roleFound) {
            ret.setMessageSignature((MessageSignature) context.project(source.getMessageSignature(), role, l));
        } else {
            ret = null;
        }
        return (ret);
    }
}
