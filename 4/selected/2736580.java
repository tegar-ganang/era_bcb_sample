package de.nava.informa.impl.basic.atom;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import de.nava.informa.core.atom.AtomItemIF;
import de.nava.informa.core.atom.LinkIF;
import de.nava.informa.core.atom.PersonIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.Item;
import de.nava.informa.impl.basic.IdGenerator;

/**
 * @author Jean-Guy Avelin
 *
 */
public class AtomItem extends Item implements AtomItemIF {

    String content = null;

    Collection linksList = null;

    Date issued, modified, created = null;

    String id = null;

    PersonIF author = null;

    Collection contributorsList = null;

    public AtomItem(ItemIF src) {
        this.setId(IdGenerator.getInstance().getId());
        this.setChannel(src.getChannel());
        this.setTitle(src.getTitle());
        this.setDescription(src.getDescription());
        this.setLink(src.getLink());
        this.setCreator(src.getCreator());
        this.setSubject(src.getSubject());
        this.setDate(src.getDate());
        this.setFound(src.getFound());
        this.setUnRead(src.getUnRead());
        this.setCategories(src.getCategories());
    }

    public Collection getLinks() {
        return linksList;
    }

    public void setLinks(Collection list) {
        linksList = list;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public Date getIssued() {
        return this.issued;
    }

    public void setIssued(Date date) {
        this.issued = date;
    }

    public Date getModified() {
        return this.issued;
    }

    public void setModified(Date date) {
        this.modified = date;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(Date date) {
        this.created = date;
    }

    public void setAtomID(String id) {
        this.id = id;
    }

    public String getAtomID() {
        return id;
    }

    public PersonIF getAuthor() {
        return author;
    }

    public void setAuthor(PersonIF author) {
        this.author = author;
    }

    public Collection getContributors() {
        return contributorsList;
    }

    public void setContributors(Collection contributorsList) {
        this.contributorsList = contributorsList;
    }

    public LinkIF getCetegoryURI() {
        return findLink(LinkIF.CATEGORY);
    }

    public LinkIF getEditURI() {
        return findLink(LinkIF.EDIT);
    }

    public LinkIF getFeedURI() {
        return findLink(LinkIF.FEED);
    }

    public LinkIF getPermalink() {
        return findLink(LinkIF.PERMALINK);
    }

    public LinkIF getPostURI() {
        return findLink(LinkIF.POST);
    }

    public LinkIF getRelated() {
        return findLink(LinkIF.RELATED);
    }

    public LinkIF getUploadURI() {
        return findLink(LinkIF.UPLOAD);
    }

    public LinkIF getVia() {
        return findLink(LinkIF.VIA);
    }

    private LinkIF findLink(String relType) {
        Iterator it = getLinks().iterator();
        while (it.hasNext()) {
            LinkIF tmpLink = (LinkIF) it.next();
            if (relType.equalsIgnoreCase(tmpLink.getRel())) {
                return tmpLink;
            }
        }
        return null;
    }
}
