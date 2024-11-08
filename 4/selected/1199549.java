package cn.ekuma.epos.linkshop.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import cn.ekuma.epos.symmetricds.bean.Channel;
import cn.ekuma.epos.symmetricds.bean.Router;
import cn.ekuma.epos.symmetricds.bean.Trigger;
import cn.ekuma.epos.symmetricds.bean.TriggerRouter;

public class SyncUtil {

    public static List<Trigger> createTriggerForTables(String[] tables, Channel c) {
        List<Trigger> ret = new ArrayList<Trigger>();
        for (String t : tables) {
            Trigger obj = new Trigger();
            obj.setTriggerId(t);
            obj.setSourceTableName(t);
            obj.setChannelId(c.getChannelId());
            obj.setCreateTime(new Date());
            obj.setLastUpdateTime(new Date());
            ret.add(obj);
        }
        return ret;
    }

    public static List<TriggerRouter> createTriggerRouters(List<Trigger> triggers, Router router, int base_initial_load_order) {
        List<TriggerRouter> ret = new ArrayList<TriggerRouter>();
        for (Trigger t : triggers) {
            TriggerRouter obj = new TriggerRouter();
            obj.setTrigerId(t.getTriggerId());
            obj.setRouterId(router.getRouterId());
            obj.setInitialLoadOrder(base_initial_load_order++);
            obj.setCreateTime(new Date());
            obj.setLastUpdateTime(new Date());
        }
        return ret;
    }
}
