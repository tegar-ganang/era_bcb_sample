package collab.fm.server.bean.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import collab.fm.server.bean.entity.attr.Attribute;
import collab.fm.server.bean.entity.attr.Value;
import collab.fm.server.bean.transfer.Comment2;
import collab.fm.server.bean.transfer.Entity2;
import collab.fm.server.bean.transfer.Feature2;
import collab.fm.server.util.DaoUtil;
import collab.fm.server.util.EntityUtil;
import collab.fm.server.util.exception.EntityPersistenceException;
import collab.fm.server.util.exception.StaleDataException;

public class Feature extends VotableEntity {

    private static Logger logger = Logger.getLogger(Feature.class);

    private Map<String, Attribute> attrs = new HashMap<String, Attribute>();

    private Set<Relationship> rels = new HashSet<Relationship>();

    private Model model;

    private List<Comment> comments = new ArrayList<Comment>();

    public Feature() {
        super();
    }

    public Feature(Long creator) {
        super(creator);
    }

    public void addComment(Comment c) {
        this.getComments().add(c);
    }

    public void addRelationship(Relationship r) {
        this.getRels().add(r);
    }

    public void addAttribute(Attribute a) {
        if (attrs.get(a.getName()) == null) {
            attrs.put(a.getName(), a);
        }
    }

    public boolean voteOrAddValue(String attrName, String val, boolean yes, Long userId) {
        Attribute attr = attrs.get(attrName);
        if (attr == null) {
            return false;
        }
        Value v = new Value(userId);
        v.setStrVal(val);
        return attr.voteOrAddValue(v, yes, userId);
    }

    @Override
    public boolean vote(boolean yes, Long userId) {
        if (!yes) {
            for (Relationship r : rels) {
                try {
                    if (r.vote(false, userId)) {
                        DaoUtil.getRelationshipDao().save(r);
                    } else {
                        DaoUtil.getRelationshipDao().delete(r);
                    }
                } catch (EntityPersistenceException e) {
                    logger.warn("Vote on relationship failed.", e);
                } catch (StaleDataException e) {
                    logger.warn("Vote on relationship failed.", e);
                }
            }
        }
        return super.vote(yes, userId);
    }

    public Map<String, Attribute> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Attribute> attrs) {
        this.attrs = attrs;
    }

    public Set<Relationship> getRels() {
        return rels;
    }

    public void setRels(Set<Relationship> rels) {
        this.rels = rels;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (this == null || o == null) return false;
        if (!(o instanceof Feature)) return false;
        Feature that = (Feature) o;
        return this.value().equals(that.value());
    }

    @Override
    public int hashCode() {
        return this.value().hashCode();
    }

    @Override
    public String value() {
        if (this.getId() != null) {
            return this.getId().toString();
        }
        return String.valueOf(this.getAttrs().hashCode());
    }

    @Override
    protected void removeThis() {
        try {
            DaoUtil.getFeatureDao().delete(this);
        } catch (EntityPersistenceException e) {
            logger.warn("Delete feature failed.", e);
        } catch (StaleDataException e) {
            logger.warn("Delete feature failed.", e);
        }
    }

    public Attribute getAttribute(String attrName) {
        return attrs.get(attrName);
    }

    @Override
    public void transfer(Entity2 f) {
        Feature2 f2 = (Feature2) f;
        super.transfer(f2);
        f2.setModel(this.getModel().getId());
        for (Relationship r : this.getRels()) {
            f2.addRel(r.getId());
        }
        for (Comment c : this.getComments()) {
            Comment2 c2 = new Comment2();
            c.transfer(c2);
            f2.addComment(c2);
        }
        for (Map.Entry<String, Attribute> e : this.getAttrs().entrySet()) {
            f2.addAttr(EntityUtil.transferFromAttr(e.getValue()));
        }
    }
}
