package com.tysanclan.site.projectewok.entities.dao.filters;

import com.jeroensteenbeeke.hyperion.data.SearchFilter;
import com.tysanclan.site.projectewok.entities.GameAccount;

public class GameAccountFilter extends SearchFilter<GameAccount> {

    private static final long serialVersionUID = 1L;

    private String accountName;

    private String channelWebServiceUID;

    private Boolean botLinked;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    /**
	 * @return the channelWebServiceUID
	 */
    public String getChannelWebServiceUID() {
        return channelWebServiceUID;
    }

    /**
	 * @param channelWebServiceUID
	 *            the channelWebServiceUID to set
	 */
    public void setChannelWebServiceUID(String channelWebServiceUID) {
        this.channelWebServiceUID = channelWebServiceUID;
    }

    public Boolean getBotLinked() {
        return botLinked;
    }

    public void setBotLinked(Boolean botLinked) {
        this.botLinked = botLinked;
    }
}
