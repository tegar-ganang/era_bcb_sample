package com.jeecms.cms.entity;

import java.util.HashSet;
import java.util.Set;
import com.jeecms.cms.entity.base.BaseCmsAdmin;
import com.jeecms.core.entity.Admin;

public class CmsAdmin extends BaseCmsAdmin {

    private static final long serialVersionUID = 1L;

    public void addTochannels(CmsChannel chnl) {
        Set<CmsChannel> set = getChannels();
        if (set == null) {
            set = new HashSet<CmsChannel>();
            setChannels(set);
        }
        chnl.addToAdmins(this);
        set.add(chnl);
    }

    /**
	 * ��session�ı����key��
	 */
    public static final String CMS_ADMIN_KEY = "_cms_admin_key";

    /**
	 * ��õ�¼��
	 * 
	 * @return
	 */
    public String getLoginName() {
        Admin admin = getAdmin();
        if (admin != null) {
            return admin.getLoginName();
        } else {
            return null;
        }
    }

    /**
	 * ����Ա�Ƿ����
	 * 
	 * @return
	 */
    public Boolean getAdminDisabled() {
        return getAdmin().getAdminDisabled();
    }

    public CmsAdmin() {
        super();
    }

    /**
	 * Constructor for primary key
	 */
    public CmsAdmin(java.lang.Long id) {
        super(id);
    }

    /**
	 * Constructor for required fields
	 */
    public CmsAdmin(java.lang.Long id, com.jeecms.core.entity.Website website, java.lang.Integer checkRight, java.lang.Boolean selfOnly) {
        super(id, website, checkRight, selfOnly);
    }
}
