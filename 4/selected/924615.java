package cn.ekuma.epos.symmetricds.bean;

import java.util.Date;
import cn.ekuma.data.dao.bean.CompositeKey;
import cn.ekuma.data.dao.bean.I_BaseBean;

public class NodeChannelCtl extends org.jumpmind.symmetric.model.NodeChannelControl implements I_BaseBean<CompositeKey> {

    @Override
    public CompositeKey getKey() {
        return CompositeKey.instance(getNodeId(), getChannelId());
    }
}
