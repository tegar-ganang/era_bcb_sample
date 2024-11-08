package org.scribble.projector;

import org.scribble.extensions.RegistryInfo;
import org.scribble.model.*;
import org.scribble.model.admin.ModelListener;

/**
 * This class provides the ChannelList implementation of the
 * projector rule.
 */
@RegistryInfo(extension = ProjectorRule.class)
public class ChannelListProjectorRule implements ProjectorRule {

    /**
	 * This method determines whether the projection rule is
	 * appropriate for the supplied model object.
	 * 
	 * @param obj The model object to be projected
	 * @return Whether the rule is relevant for the
	 * 				model object
	 */
    public boolean isSupported(ModelObject obj) {
        return (obj.getClass() == ChannelList.class);
    }

    /**
	 * This method projects the supplied model object based on the
	 * specified role.
	 * 
	 * @param model The model object
	 * @param role The role
	 * @param l The model listener
	 * @return The projected model object
	 */
    public ModelObject project(ProjectorContext context, ModelObject model, Role role, ModelListener l) {
        ChannelList ret = new ChannelList();
        ChannelList source = (ChannelList) model;
        for (int i = 0; i < source.getChannels().size(); i++) {
            if (source.getChannels().get(i).equals(role) == false) {
                Channel ch = new Channel();
                ch.setName(source.getChannels().get(i).getName());
                context.setState(ch.getName(), ch);
                ret.getChannels().add(ch);
            }
        }
        return (ret);
    }
}
