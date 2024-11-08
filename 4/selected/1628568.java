package com.tysanclan.site.projectewok.entities.dao.hibernate;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.jeroensteenbeeke.hyperion.data.SearchFilter;
import com.tysanclan.site.projectewok.dataaccess.EwokHibernateDAO;
import com.tysanclan.site.projectewok.entities.BattleNetChannel;

/**
 * @author Jeroen Steenbeeke
 */
@Component("channelDAO")
@Scope("request")
class BattleNetChannelDAOImpl extends EwokHibernateDAO<BattleNetChannel> implements com.tysanclan.site.projectewok.entities.dao.BattleNetChannelDAO {

    @Override
    protected Criteria createCriteria(SearchFilter<BattleNetChannel> filter) {
        Criteria criteria = getSession().createCriteria(BattleNetChannel.class);
        return criteria;
    }

    /**
	 * @see com.tysanclan.site.projectewok.entities.dao.BattleNetChannelDAO#getPasswordByUID(java.lang.String)
	 */
    @Override
    public String getPasswordByUID(String identifier) {
        Criteria criteria = getSession().createCriteria(BattleNetChannel.class);
        criteria.add(Restrictions.eq("webServiceUserId", identifier));
        criteria.setProjection(Projections.property("webServicePassword"));
        return (String) criteria.uniqueResult();
    }

    @Override
    public BattleNetChannel getChannelByUID(String identifier) {
        Criteria criteria = getSession().createCriteria(BattleNetChannel.class);
        criteria.add(Restrictions.eq("webServiceUserId", identifier));
        return (BattleNetChannel) criteria.uniqueResult();
    }
}
