package mase.wikipage;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import mase.team.TeamMember;
import mase.util.WikipageService;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.hibernate.annotations.IndexColumn;

/**
 * WikiPage: MASE_EJB3
 * Package: mase.planninggame
 * Class: WikiPage.java
 * @author xueling shu
 *
 * TODO: <Describe Class>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "WikiPage")
@Table(name = "WikiPage")
public class WikiPage implements Serializable {

    private static final long serialVersionUID = 3256728394166188345L;

    protected long id = -1;

    protected String name;

    protected String namespace;

    protected String mainContent;

    protected String rightContent;

    protected Timestamp modified;

    protected Timestamp lastAccessed;

    protected boolean deleted;

    protected String locale;

    protected int pageType;

    protected int readPermission;

    protected int writePermission;

    protected int version;

    protected TeamMember author;

    protected List<Attachment> attatchmentChildren;

    protected WikiPage parent;

    protected boolean latest;

    protected int pageLayout = WikipageService.LAYOUT_SINGLE;

    protected boolean fitTest;

    private static Category log = Category.getInstance(WikiPage.class);

    /**
     * Constructor: WikiPage.java
     * @author xueling shu 
     */
    public WikiPage() {
        this("default wikipage", null, null, 1, null, null, null);
    }

    /**
     * Constructor: WikiPage.java
     * @author xueling shu
     * @param name
     * @param content
     * @param version
     * @param author
     * @param project 
     */
    public WikiPage(String name, String mainContent, String rightContent, int version, TeamMember author, WikiPage parent, String namespace) {
        log.setLevel(Level.OFF);
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        setName(name);
        setMainContent(mainContent);
        setRightContent(rightContent);
        setVersion(version);
        setAuthor(author);
        setModified(ts);
        setLastAccessed(ts);
        setDeleted(false);
        setLocale(Locale.CANADA.toString());
        setFitTest(false);
        setPageType(0);
        setReadPermission(0);
        setWritePermission(1);
        setAuthor(author);
        setLatest(true);
        setParent(parent);
        setNamespace(namespace);
        attatchmentChildren = new Vector<Attachment>();
    }

    public void setFitTest(boolean fitTest) {
        this.fitTest = fitTest;
    }

    public boolean isFitTest() {
        return fitTest;
    }

    /** (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o instanceof WikiPage) {
            WikiPage wiki = (WikiPage) o;
            return wiki.getId() == this.getId();
        } else return false;
    }

    /** (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return (int) getId();
    }

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    public TeamMember getAuthor() {
        return author;
    }

    public void setAuthor(TeamMember author) {
        this.author = author;
    }

    @Column(nullable = true, length = 100000)
    public String getMainContent() {
        return mainContent;
    }

    public void setMainContent(String mainContent) {
        this.mainContent = mainContent;
    }

    @Column(nullable = true, length = 100000)
    public String getRightContent() {
        return rightContent;
    }

    public void setRightContent(String rightContent) {
        this.rightContent = rightContent;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable = true)
    public Timestamp getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(Timestamp lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Column(nullable = true)
    public Timestamp getModified() {
        return modified;
    }

    public void setModified(Timestamp modified) {
        this.modified = modified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPageType() {
        return pageType;
    }

    public void setPageType(int pageType) {
        this.pageType = pageType;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_ID", referencedColumnName = "id", nullable = true)
    public WikiPage getParent() {
        return parent;
    }

    public void setParent(WikiPage parent) {
        this.parent = parent;
    }

    public int getReadPermission() {
        return readPermission;
    }

    public void setReadPermission(int readPermission) {
        this.readPermission = readPermission;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getWritePermission() {
        return writePermission;
    }

    public void setWritePermission(int writePermission) {
        this.writePermission = writePermission;
    }

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @IndexColumn(name = "attChildrenIndex")
    public List<Attachment> getAttatchmentChildren() {
        return attatchmentChildren;
    }

    public void setAttatchmentChildren(List<Attachment> attatchmentChildren) {
        this.attatchmentChildren = attatchmentChildren;
    }

    public void addAttachment(Attachment attach) {
        this.attatchmentChildren.add(attach);
    }

    public void removeAttachment(Attachment attach) {
        this.attatchmentChildren.remove(attach);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        if (namespace != null) this.namespace = namespace; else this.namespace = "";
    }

    public void setFullQualifiedName(String fullName) {
        int cutPnt = fullName.lastIndexOf(".");
        if (cutPnt != -1) {
            this.setName((fullName.substring(cutPnt + 1, fullName.length())).trim());
            this.setNamespace((fullName.substring(0, cutPnt)).trim());
        }
    }

    public String getFullQualifiedName() {
        return namespace + "." + name;
    }

    public int getPageLayout() {
        return pageLayout;
    }

    public void setPageLayout(int pageLayout) {
        this.pageLayout = pageLayout;
    }

    public void update(WikiPage wikipage) {
        if (wikipage == null) {
            this.setId(0);
            this.version = 1;
            this.latest = true;
        } else {
            this.version = wikipage.version + 1;
            this.latest = true;
            wikipage.latest = false;
            this.setId(0);
            this.attatchmentChildren = wikipage.attatchmentChildren;
            wikipage.attatchmentChildren = new Vector<Attachment>();
        }
    }

    public String toString() {
        String retVal = "##### Wikipage ##### /n" + "name: " + name + "/nnamespace: " + namespace + "/nmodified: " + modified.toString() + "/nlastAccessed: " + lastAccessed.toString() + "/ndeleted: " + deleted + "/nlocale: " + locale + "/npageType: " + pageType + "/nreadPermission: " + readPermission + "/nwritePermission: " + writePermission + "/nversion: " + version + "/nlatest: " + latest + "/npageLayout: " + pageLayout + "/nmainContent: " + mainContent + "/nrightContent: " + rightContent;
        return retVal;
    }
}
