package com.tysanclan.site.projectewok.entities.dao.filters;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import com.jeroensteenbeeke.hyperion.data.ModelMaker;
import com.jeroensteenbeeke.hyperion.data.SearchFilter;
import com.tysanclan.site.projectewok.entities.BattleNetChannel;
import com.tysanclan.site.projectewok.entities.BattleNetUserPresence;

/**
 * @author Jeroen Steenbeeke
 */
public class BattleNetUserPresenceFilter extends SearchFilter<BattleNetUserPresence> {

    private static final long serialVersionUID = 1L;

    private IModel<BattleNetChannel> channel = new Model<BattleNetChannel>();

    private String accountName;

    public BattleNetUserPresenceFilter(BattleNetChannel channel) {
        this.channel = ModelMaker.wrap(channel);
    }

    /**
	 * @return the accountName
	 */
    public String getAccountName() {
        return accountName;
    }

    /**
	 * @param accountName
	 *            the accountName to set
	 */
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    /**
	 * @return the channel
	 */
    public BattleNetChannel getChannel() {
        return channel.getObject();
    }

    @Override
    public void detach() {
        super.detach();
        if (channel != null) channel.detach();
    }
}
