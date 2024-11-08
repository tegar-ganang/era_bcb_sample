package net.sf.jvdr.data.ejb;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class VdrSmartSearch implements Serializable {

    public static final long serialVersionUID = 1;

    public static enum Key {

        id, vdruser_id, limitToChannel, channel, suche, epgStart, limitRange, rangeDown, rangeUp, aktiv
    }

    ;

    private int id, channel;

    private VdrUser vdruser;

    private boolean limitToChannel, limitRange, aktiv;

    private String suche;

    private Date epgStart;

    private int rangeDown, rangeUp;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne
    public VdrUser getVdruser() {
        return vdruser;
    }

    public void setVdruser(VdrUser vdruser) {
        this.vdruser = vdruser;
    }

    public boolean isLimitToChannel() {
        return limitToChannel;
    }

    public void setLimitToChannel(boolean limitToChannel) {
        this.limitToChannel = limitToChannel;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getSuche() {
        return suche;
    }

    public void setSuche(String suche) {
        this.suche = suche;
    }

    public Date getEpgStart() {
        return epgStart;
    }

    public void setEpgStart(Date epgStart) {
        this.epgStart = epgStart;
    }

    public boolean isLimitRange() {
        return limitRange;
    }

    public void setLimitRange(boolean limitRange) {
        this.limitRange = limitRange;
    }

    public int getRangeDown() {
        return rangeDown;
    }

    public void setRangeDown(int rangeDown) {
        this.rangeDown = rangeDown;
    }

    public int getRangeUp() {
        return rangeUp;
    }

    public void setRangeUp(int rangeUp) {
        this.rangeUp = rangeUp;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(id);
        sb.append(" " + vdruser.getId());
        sb.append(" lChn?" + limitToChannel);
        sb.append(" aktiv?" + aktiv);
        sb.append(" lRng?" + limitRange);
        sb.append(" " + channel);
        sb.append(" (" + suche + ")");
        sb.append(" " + epgStart + "");
        sb.append(" [" + rangeDown + ";" + rangeUp + "]");
        return sb.toString();
    }
}
