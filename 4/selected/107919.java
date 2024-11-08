package br.gov.component.demoiselle.monitoring.snmp.security;

import org.snmp4j.smi.OctetString;

/**
 * @author SERPRO/CETEC
 */
public class AccessEntry {

    private OctetString context;

    private SecModel model;

    private SecLevel level;

    private MatchType match;

    private OctetString readView;

    private OctetString writeView;

    private OctetString notifyView;

    public AccessEntry(String context, String model, boolean auth, boolean priv, boolean exact) {
        this.context = new OctetString(context);
        this.model = SecModel.parseString(model);
        this.level = SecLevel.parsePair(auth, priv);
        this.match = MatchType.parseBoolean(exact);
    }

    public OctetString getContext() {
        return context;
    }

    public void setContext(OctetString context) {
        this.context = context;
    }

    public SecModel getModel() {
        return model;
    }

    public void setModel(SecModel model) {
        this.model = model;
    }

    public SecLevel getLevel() {
        return level;
    }

    public void setLevel(SecLevel level) {
        this.level = level;
    }

    public OctetString getReadView() {
        return readView;
    }

    public void setReadView(String readView) {
        this.readView = new OctetString(readView);
    }

    public OctetString getWriteView() {
        return writeView;
    }

    public void setWriteView(String writeView) {
        this.writeView = new OctetString(writeView);
    }

    public OctetString getNotifyView() {
        return notifyView;
    }

    public void setNotifyView(String notifyView) {
        this.notifyView = new OctetString(notifyView);
    }

    public MatchType getMatch() {
        return match;
    }

    public void setMatch(MatchType match) {
        this.match = match;
    }

    public String toString() {
        return "AccessEntry [context=" + context + ", level=" + level + ", match=" + match + ", model=" + model + ", notifyView=" + notifyView + ", readView=" + readView + ", writeView=" + writeView + "]";
    }
}
