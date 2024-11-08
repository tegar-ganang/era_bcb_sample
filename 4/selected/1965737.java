package desview.model.entities;

import desview.model.enums.EquipmentStatus;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Class for an equipment entity.
 * @author Luiz Mello.
 * @author Diones Rossetto.
 * @since 27/03/2010.
 * @version 1.0.
 */
@Entity
@Table(name = "Equipment")
public class Equipment implements Serializable {

    private static final long serialVersionUID = -5941L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip", nullable = false, length = 30)
    private String ip;

    @Column(name = "equipment_name", nullable = false, length = 250)
    private String equipmentName;

    @Column(name = "read_community", nullable = true, length = 50)
    private String readCommunity;

    @Column(name = "write_community", nullable = true, length = 50)
    private String writeCommunity;

    @Column(name = "port", nullable = true, length = 5)
    private String porta;

    @Column(name = "timeout", nullable = true, length = 10)
    private String timeout;

    @Column(name = "status", nullable = true, length = 5)
    private EquipmentStatus status;

    @Column(name = "retries", nullable = true, length = 3)
    private String retries;

    /**
     * Default constructor.
     */
    public Equipment() {
    }

    /**
     * Equipment constructor.
     * @param ip equipment ip.
     * @param name equipment name.
     * @param writeCommunity equipment write community.
     * @param readCommunity equipment read community.
     * @param port equipment port.
     * @param timeout equipment timeout.
     * @param status equipment status.
     * @param retries number of retries.
     * @see EquipmentStatus
     */
    public Equipment(String ip, String name, String writeCommunity, String readCommunity, String port, String timeout, EquipmentStatus status, String retries) {
        setIP(ip);
        setName(name);
        setWriteCommunity(writeCommunity);
        setReadCommunity(readCommunity);
        setPort(port);
        setTimeout(timeout);
        setStatus(status);
        setRetries(retries);
    }

    /**
     * Returns equipement id.
     * @return id.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     * @param id new id.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns write community.
     * @return write community.
     */
    public String getWriteCommunity() {
        return writeCommunity;
    }

    /**
     * Sets the write community.
     * <br> If community is null, it will be set to default <i>public</i>.
     * @param writeCommunity new write community.
     */
    public void setWriteCommunity(String writeCommunity) {
        if (writeCommunity == null) {
            this.writeCommunity = "public";
        } else {
            this.writeCommunity = writeCommunity;
        }
    }

    /**
     * Returns equipment read community.
     * @return read community.
     */
    public String getReadCommunity() {
        return readCommunity;
    }

    /**
     * Sets the read community.
     * <br> If community is null, it will be set to default <i>public</i>.
     * @param readCommunity new write community.
     */
    public void setReadCommunity(String readCommunity) {
        if (readCommunity == null) {
            this.readCommunity = "public";
        } else {
            this.readCommunity = readCommunity;
        }
    }

    /**
     * Returns equipment IP.
     * @return equipment IP.
     */
    public String getIP() {
        return ip;
    }

    /**
     * Sets the equipment IP.
     * <br> If IP is null, it will be set to default <i>127.0.0.1</i> (localhost).
     * @param ip new IP.
     */
    public void setIP(String ip) {
        if (ip == null) {
            this.ip = "127.0.0.1";
        } else {
            this.ip = ip;
        }
    }

    /**
     * Returns equipment name.
     * @return equipment name.
     */
    public String getName() {
        return equipmentName;
    }

    /**
     * Sets equipment name.
     * <br> If name is null, it will be set to default <i>Equipment - ip:port</i>.
     * @param name new name.
     */
    public void setName(String name) {
        if (name == null) {
            StringBuffer b = new StringBuffer();
            b.append("Equipment - ").append(getIP()).append(":").append(getPort());
            this.equipmentName = b.toString();
        } else {
            this.equipmentName = name;
        }
    }

    /**
     * Returns equipment port.
     * @return equipment port.
     */
    public String getPort() {
        return porta;
    }

    /**
     * Sets equipment port.
     * <br> If port is null, it will be set to default <i>161</i>.
     * @param port new port.
     */
    public void setPort(String port) {
        if (port == null || port.equals("")) {
            this.porta = "161";
        } else {
            this.porta = port;
        }
    }

    /**
     * Returns equipment retries.
     * @return number of retries.
     */
    public String getRetries() {
        return retries;
    }

    /**
     * Sets equipment retries.
     * <br> If retries is null, it will be set to default <i>2</i>.
     * @param retries the retries.
     */
    public void setRetries(String retries) {
        if (retries == null || retries.equals("")) {
            this.retries = "2";
        } else {
            this.retries = retries;
        }
    }

    /**
     * Returns equipment status.
     * @see EquipmentStatus
     * @return status.
     */
    public EquipmentStatus getStatus() {
        return status;
    }

    /**
     * Sets equipment status.
     * @see EquipmentStatus
     * @param status the current status.
     */
    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    /**
     * Returns timeout.
     * @return timeout.
     */
    public String getTimeout() {
        return timeout;
    }

    /**
     * Sets equipment timeout.
     * <br> If null default is 10000 miliseconds.
     * @param timeout a timeout.
     */
    public void setTimeout(String timeout) {
        if (timeout == null) {
            this.timeout = "10000";
        } else {
            this.timeout = timeout;
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Equipment)) {
            return false;
        }
        Equipment other = (Equipment) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    /**
     * Returns all information about the equipment.
     * @return string with equipment information.
     */
    public String equipmentInformation() {
        StringBuffer s = new StringBuffer();
        s.append("Equipment: [name = ").append(equipmentName);
        s.append(", ip =  ").append(ip);
        if (porta != null) {
            s.append(": ").append(porta);
        }
        if (timeout != null) {
            s.append(", timeout =  ").append(timeout);
        }
        if (writeCommunity != null) {
            s.append(", write community =  ").append(writeCommunity);
        }
        if (readCommunity != null) {
            s.append(", read community =  ").append(readCommunity);
        }
        if (retries != null) {
            s.append(", retries =  ").append(retries);
        }
        if (status != null) {
            s.append(", status =  ").append(status);
        }
        s.append("]");
        return s.toString();
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(equipmentName);
        s.append(" (").append(ip);
        if (porta != null) {
            s.append(": ").append(porta);
        }
        s.append(")");
        return s.toString();
    }
}
