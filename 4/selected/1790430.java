package cn.ekuma.epos.symmetricds.bean;

import cn.ekuma.data.dao.bean.I_BaseBean;

public class Channel extends org.jumpmind.symmetric.model.Channel implements I_BaseBean<String> {

    @Override
    public String getKey() {
        return getChannelId();
    }
}
