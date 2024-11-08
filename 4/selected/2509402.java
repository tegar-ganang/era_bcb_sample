package com.cbsgmbh.xi.af.edifact.jca;

import javax.resource.spi.ConnectionRequestInfo;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;

public class SPIConnectionRequestInfo implements ConnectionRequestInfo {

    private static final String VERSION_ID = "$Id://OPI2_EDIFACT_Adapter_Http/com/cbsgmbh/opi2/xi/af/edifact/jca/SPIConnectionRequestInfo.java#1 $";

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(VERSION_ID, TracerCategories.APP_ADAPTER_HTTP);

    private String user;

    private String password;

    private String channelId;

    public SPIConnectionRequestInfo(String user, String password, String channelId) {
        final Tracer tracer = baseTracer.entering("SPIConnectionRequestInfo(String userName, String password, String channelId)");
        this.user = user;
        this.password = password;
        this.channelId = channelId;
        tracer.leaving();
    }

    public String getUserName() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getChannelId() {
        return channelId;
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof SPIConnectionRequestInfo) {
            int hash1 = ((SPIConnectionRequestInfo) obj).hashCode();
            int hash2 = hashCode();
            return hash1 == hash2;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int hash = 0;
        String propset = this.user + this.password + this.channelId;
        hash = propset.hashCode();
        return hash;
    }
}
