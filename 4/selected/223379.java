package corpodatrecordsbackend;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author luis
 */
@Entity
@Table(name = "permissions")
@NamedQueries({ @NamedQuery(name = "Permission.findAll", query = "SELECT p FROM Permission p"), @NamedQuery(name = "Permission.findById", query = "SELECT p FROM Permission p WHERE p.id = :id"), @NamedQuery(name = "Permission.findByName", query = "SELECT p FROM Permission p WHERE p.name = :name"), @NamedQuery(name = "Permission.findByDescription", query = "SELECT p FROM Permission p WHERE p.description = :description"), @NamedQuery(name = "Permission.findByModuleId", query = "SELECT p FROM Permission p WHERE p.moduleId = :moduleId"), @NamedQuery(name = "Permission.findByRead", query = "SELECT p FROM Permission p WHERE p.read = :read"), @NamedQuery(name = "Permission.findByWrite", query = "SELECT p FROM Permission p WHERE p.write = :write") })
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Basic(optional = false)
    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Basic(optional = false)
    @Column(name = "module_id")
    private int moduleId;

    @Basic(optional = false)
    @Column(name = "readable")
    private boolean read;

    @Basic(optional = false)
    @Column(name = "writeable")
    private boolean write;

    public Permission() {
    }

    public Permission(Integer id) {
        this.id = id;
    }

    public Permission(Integer id, String name, int moduleId, boolean read, boolean write) {
        this.id = id;
        this.name = name;
        this.moduleId = moduleId;
        this.read = read;
        this.write = write;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getModuleId() {
        return moduleId;
    }

    public void setModuleId(int moduleId) {
        this.moduleId = moduleId;
    }

    public boolean getRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean getWrite() {
        return write;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Permission)) {
            return false;
        }
        Permission other = (Permission) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "corpodatrecordsbackend.Permission[id=" + id + "]";
    }
}
