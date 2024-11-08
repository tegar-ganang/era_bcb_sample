package com.asoft.common.util.xml;

import org.apache.log4j.Logger;

/**
 * <p>Description: 实体配置文件</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: asoft</p>
 * @ $Author: amon_lei $
 * @ $Date: 2005/02/15 14:51:27 $
 * @ $Revision: 1.2 $ 
 * @ created in 2005-2-15 9:57
 *
 */
public class ConfigBean {

    static Logger logger = Logger.getLogger(ConfigBean.class);

    private boolean isCachable;

    private String mainEntity;

    private String[] relaEntitys;

    private String[] readOnlyMethods;

    private String[] writeMethods;

    public ConfigBean(boolean isCachable, String mainEntity, String[] relaEntitys, String[] readOnlyMethods, String[] writeMethods) {
        this.isCachable = isCachable;
        this.mainEntity = mainEntity;
        this.relaEntitys = relaEntitys;
        this.readOnlyMethods = readOnlyMethods;
        this.writeMethods = writeMethods;
    }

    public boolean getIsCachable() {
        return this.isCachable;
    }

    public String getMainEntity() {
        return this.mainEntity;
    }

    public String[] getRelaEntitys() {
        return this.relaEntitys;
    }

    public String[] getReadOnlyMethods() {
        return this.readOnlyMethods;
    }

    public String[] writeMethods() {
        return this.writeMethods;
    }

    public ConfigBean getClone() {
        logger.debug("getClone()");
        return new ConfigBean(this.isCachable, this.mainEntity, this.relaEntitys, this.readOnlyMethods, this.writeMethods);
    }

    /**
         * 匹配
         */
    private boolean match(String sStr, String[] toStrs) {
        boolean rs = false;
        for (int i = 0; i < toStrs.length; i++) {
            rs = rs | sStr.startsWith(toStrs[i]);
            logger.debug(toStrs[i] + " " + sStr + ":" + rs);
            if (rs == true) break;
        }
        return rs;
    }

    public boolean relaEntitysMatch(String str) {
        return this.match(str, this.relaEntitys);
    }

    public boolean roMethodsMatch(String str) {
        return this.match(str, this.readOnlyMethods);
    }

    public boolean wrMethodsMatch(String str) {
        return this.match(str, this.writeMethods);
    }
}
