package fido.db;

import java.sql.*;
import java.util.*;
import fido.util.BooleanTree;
import fido.util.BooleanTreeNode;
import fido.util.BooleanNode;
import fido.util.EmptyBooleanNode;
import fido.util.BooleanSyntaxException;
import fido.util.FidoDataSource;

public class ObjectTable {

    public ObjectTable() {
    }

    private int getParent(int objectId) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            ObjectLinkTable objectLinkList = new ObjectLinkTable();
            Collection links = objectLinkList.getLinkTo(objectId, "isa");
            if (links.size() == 0) {
                links = objectLinkList.getLinkTo(objectId, "instance");
                if (links.size() == 0) throw new ObjectNotFoundException(objectId);
            }
            Iterator it = links.iterator();
            return ((Integer) it.next()).intValue();
        } catch (ClassLinkTypeNotFoundException e) {
            return -1;
        }
    }

    /**
	 * Looks for a class named <i>parent</i> as any parent class
	 * of <i>objectId</i>, until the root class <i>Object</i>
	 * is reached.
	 * @param objectId starting class name
	 * @param parent name of the class to look for as a parent
	 * @exception ObjectNotFoundException thrown if <i>objectId</i>
	 *            is not the name of a valid ClassObject.
	 * @exception SQLException thrown if any IO Exception occurs
	 *            while reading classes.
	 */
    public boolean isSubclassOf(int objectId, int parent) throws FidoDatabaseException, ObjectNotFoundException {
        while (true) {
            if (objectId == parent) return true;
            if (objectId == 1) return false;
            objectId = getParent(objectId);
        }
    }

    private boolean contains(Statement stmt, int id) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select count(1) from Objects where ObjectId = " + id;
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new SQLException("No results returned from count(1) query");
            int count = rs.getInt(1);
            if (count == 0) return false;
            return true;
        } finally {
            if (rs != null) rs.close();
        }
    }

    public boolean contains(int id) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = FidoDataSource.getConnection();
                stmt = conn.createStatement();
                return contains(stmt, id);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Creates a new ClassObject cloning <i>objectId</i> with
	 * a LinkName Isa pointing to the parent class.  Also, the
	 * reverse link field of Isa will be added to the parent
	 * class <i>objectId</i> adding the new class as a child.
	 * The name of the new object will be a unique generated
	 * name, which will be returned as a String from the call.
	 * @param objectId name of the parent class to clone
	 * @exception ObjectNotFoundException thrown if the class
	 *            object <i>objectId</i> is not found
	 * @exception SQLException thrown if any IO Exception occurs
	 *            while reading parent class or writting the new
	 *            class.
	 * @return name of the new ClassObject
	 */
    public int subclass(int objectId, String description) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "insert into Objects (Description) " + "values ('" + description + "')";
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                if (contains(stmt, objectId) == false) throw new ObjectNotFoundException(objectId);
                stmt.executeUpdate(sql);
                int id;
                sql = "select currval('objects_objectid_seq')";
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new SQLException("No rows returned from select currval() query"); else id = rs.getInt(1);
                ObjectLinkTable objectLinkList = new ObjectLinkTable();
                objectLinkList.linkObjects(stmt, id, "isa", objectId);
                conn.commit();
                return id;
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private void internalDeleteClass(Statement stmt, int objectId, ObjectLinkTable objectLinkList, ObjectAttributeTable objectAttributeList) throws FidoDatabaseException {
        try {
            objectLinkList.deleteObject(stmt, objectId);
            objectAttributeList.deleteObject(stmt, objectId);
            stmt.executeUpdate("delete from Objects where ObjectId = " + objectId);
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private void recursiveDeleteTree(Statement stmt, int objectId, ObjectLinkTable objectLinkList, ObjectAttributeTable objectAttributeList) throws FidoDatabaseException, ObjectNotFoundException, ClassLinkTypeNotFoundException {
        Collection kids = objectLinkList.getLinkFrom(objectId, "isa");
        for (Iterator it = kids.iterator(); it.hasNext(); ) {
            int kid = ((Integer) it.next()).intValue();
            recursiveDeleteTree(stmt, kid, objectLinkList, objectAttributeList);
        }
        internalDeleteClass(stmt, objectId, objectLinkList, objectAttributeList);
    }

    /**
	 * Removes the Class name <i>objectId</i> from the Hierarchy.  
	 * To maintain hierarchial integrity, the parameter <i>tree</i>
	 * specifies what should happen to the children ClassObjects
	 * of the object:
	 * <UL>
	 * <LI>if tree is true, all of the children are removed
	 * <LI>if tree is false, all of the children become children
	 *     of the parent of the ClassObject being removed.
	 * </UL>
	 * Any InstanceObject of the object will be removed.
	 * @param objectId name of the class to remove
	 * @param tree true if all subclasses should be removed, false if
	 *             all children should become children of the parent
	 * @exception ObjectNotFoundException thrown if the class
	 *            object <i>objectId</i> is not found
	 * @exception SQLException thrown if any IO Exception occurs
	 *            while reading parent class or deleting the class.
	 * @exception CannotDeleteRootObjectException thrown when attempting
	 *            to remove the root object named <i>Object</i>
	 * @exception LinkTypeNotFoundException System LinkType named Isa
	 *            was not found
	 */
    public void deleteClass(int objectId, boolean tree) throws ObjectNotFoundException, FidoDatabaseException, CannotDeleteRootObjectException, ClassLinkTypeNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                if (objectId == 1) throw new CannotDeleteRootObjectException();
                ObjectLinkTable objectLinkList = new ObjectLinkTable();
                ObjectAttributeTable objectAttributeList = new ObjectAttributeTable();
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                if (contains(stmt, objectId) == false) throw new ObjectNotFoundException(objectId);
                if (tree == true) recursiveDeleteTree(stmt, objectId, objectLinkList, objectAttributeList); else {
                    int parent = getParent(objectId);
                    Collection kids = objectLinkList.getLinkFrom(objectId, "isa");
                    for (Iterator it = kids.iterator(); it.hasNext(); ) {
                        int kid = ((Integer) it.next()).intValue();
                        objectLinkList.unlinkObjects(kid, "isa", objectId);
                        objectLinkList.linkObjects(kid, "isa", parent);
                    }
                    internalDeleteClass(stmt, objectId, objectLinkList, objectAttributeList);
                }
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } catch (FidoDatabaseException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Creates a new InstanceObject from the ClassObject <i>objectId</i>
	 * The new object will duplicate all of the links of the parent
	 * ClassObject.  The LinkType Instance will be used to link the
	 * new Instance with the parent ClassObject.  The name of the new
	 * object will be a unique generated name, which will be returned
	 * as a String from the call.
	 * @param objectId name of the class to create an instance from
	 * @exception ObjectNotFoundException thrown if the class
	 *            object <i>objectId</i> is not found
	 * @exception LinkTypeNotFoundException thrown if the link type
	 *            <i>Instance</i> is not found
	 * @exception SQLException thrown if any IO Exception occurs
	 *            while reading parent class or writting the new
	 *            class.
	 * @return name of the new InstanceObject
	 */
    public int instantiate(int objectId, String description) throws FidoDatabaseException, ObjectNotFoundException, ClassLinkTypeNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "insert into Objects (Description) " + "values ('" + description + "')";
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                if (contains(stmt, objectId) == false) throw new ObjectNotFoundException(objectId);
                stmt.executeUpdate(sql);
                int id;
                sql = "select currval('objects_objectid_seq')";
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new SQLException("No rows returned from select currval() query"); else id = rs.getInt(1);
                ObjectLinkTable objectLinkList = new ObjectLinkTable();
                objectLinkList.linkObjects(stmt, id, "instance", objectId);
                conn.commit();
                return id;
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Deletes an InstanceObject.
	 * @param instanceName name of the object to remove
	 * @exception ObjectNotFoundException thrown if the class
	 *            object <i>instanceName</i> is not found
	 * @exception SQLException thrown if any IO Exception occurs
	 *            while removing the object
	 */
    public void deleteInstance(int instanceId) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                if (contains(stmt, instanceId) == false) throw new ObjectNotFoundException(instanceId);
                ObjectLinkTable objectLinkList = new ObjectLinkTable();
                ObjectAttributeTable objectAttributeList = new ObjectAttributeTable();
                objectLinkList.deleteObject(stmt, instanceId);
                objectAttributeList.deleteObject(stmt, instanceId);
                stmt.executeUpdate("delete from Objects where ObjectId = " + instanceId);
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    public String buildInList(int objectId) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                if (contains(stmt, objectId) == false) throw new ObjectNotFoundException(objectId);
                StringBuffer sb = new StringBuffer("(");
                boolean first = true;
                while (true) {
                    if (first == true) {
                        sb.append(objectId);
                        first = false;
                    } else sb.append(", " + objectId);
                    if (objectId == 1) break;
                    objectId = getParent(objectId);
                }
                sb.append(")");
                return sb.toString();
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Deletes an InstanceObject.
	 * @param instanceName name of the object to remove
	 * @exception ObjectNotFoundException thrown if the class
	 *            object <i>instanceName</i> is not found
	 * @exception SQLException thrown if any IO Exception occurs
	 *            while removing the object
	 */
    public Collection pathToObject(int objectId) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                if (contains(stmt, objectId) == false) throw new ObjectNotFoundException(objectId);
                Vector path = new Vector();
                while (true) {
                    Integer id = new Integer(objectId);
                    path.add(id);
                    if (objectId == 1) break;
                    objectId = getParent(objectId);
                }
                return path;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    public void modify(int id, String description) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                if (contains(stmt, id) == false) throw new ObjectNotFoundException(id);
                String sql = "update Objects " + "   set Description = '" + description + "' " + "where ObjectId = " + id;
                stmt.executeUpdate(sql);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private String getObjectDescription(int objectId) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Description from Objects where ObjectId = " + objectId;
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new ObjectNotFoundException(objectId); else return rs.getString(1);
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private Collection findObjectSubtree(int objectId, int previousId, Collection previousSubtree) throws FidoDatabaseException, ObjectNotFoundException {
        Vector list = new Vector();
        try {
            ObjectLinkTable objectLinkList = new ObjectLinkTable();
            Collection kids = objectLinkList.getLinkFrom(objectId, "instance");
            for (Iterator it = kids.iterator(); it.hasNext(); ) {
                int kid = ((Integer) it.next()).intValue();
                ObjectTreeElement element = new ObjectTreeElement(kid, getObjectDescription(kid), false, false, true);
                list.add(element);
            }
            kids = objectLinkList.getLinkFrom(objectId, "isa");
            for (Iterator it = kids.iterator(); it.hasNext(); ) {
                int kid = ((Integer) it.next()).intValue();
                Collection grandkids = objectLinkList.getLinkFrom(kid, "isa");
                Collection grandinstances = objectLinkList.getLinkFrom(kid, "instance");
                boolean hasChildren;
                if ((grandkids.size() == 0) && (grandinstances.size() == 0)) hasChildren = false; else hasChildren = true;
                boolean elementOpen;
                if ((previousId == -1) || (previousId != kid)) elementOpen = false; else elementOpen = true;
                ObjectTreeElement element = new ObjectTreeElement(kid, getObjectDescription(kid), hasChildren, elementOpen, false);
                list.add(element);
                if (previousId == kid) list.add(previousSubtree);
            }
        } catch (ClassLinkTypeNotFoundException e) {
        }
        return list;
    }

    public Collection findObject(int objectId) throws FidoDatabaseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                if (contains(stmt, objectId) == false) throw new ObjectNotFoundException(objectId);
                int previousId = -1;
                Collection subtree = null;
                while (true) {
                    subtree = findObjectSubtree(objectId, previousId, subtree);
                    if (objectId == 1) break;
                    previousId = objectId;
                    objectId = getParent(objectId);
                }
                Vector wholeTree = new Vector();
                ObjectTreeElement rootElement = new ObjectTreeElement(1, getObjectDescription(1), true, true, false);
                wholeTree.add(rootElement);
                wholeTree.add(subtree);
                return wholeTree;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private void recursiveBuildFindAttr(int objectId, BooleanTreeNode node, StringBuffer sb) {
        if (node instanceof AttributeBooleanNode) {
            AttributeBooleanNode attr = (AttributeBooleanNode) node;
            sb.insert(0, "select ObjectId from ObjectAttributes " + "where AttributeName = '" + attr.getAttribute() + "' and ObjectId in (");
            sb.append(")");
        } else if (node instanceof BooleanNode) {
            BooleanNode bn = (BooleanNode) node;
            recursiveBuildFindAttr(objectId, bn.getLeftBranch(), sb);
            recursiveBuildFindAttr(objectId, bn.getRightBranch(), sb);
        }
    }

    private void recursiveBuildFindPrep(int objectId, BooleanTreeNode node, StringBuffer sb) {
        if (node instanceof PrepositionBooleanNode) {
            PrepositionBooleanNode prep = (PrepositionBooleanNode) node;
            sb.insert(0, "select ObjectId from ObjectLinks " + "where LinkName = '" + prep.getLink() + "' and LinkToObject = " + prep.getObject() + "and ObjectId in (");
            sb.append(")");
        } else if (node instanceof BooleanNode) {
            BooleanNode bn = (BooleanNode) node;
            recursiveBuildFindPrep(objectId, bn.getLeftBranch(), sb);
            recursiveBuildFindPrep(objectId, bn.getRightBranch(), sb);
        }
    }

    private boolean buildFindInList(int objectId, StringBuffer sb, boolean first) throws FidoDatabaseException, ObjectNotFoundException, ClassLinkTypeNotFoundException {
        ObjectLinkTable objectLinkList = new ObjectLinkTable();
        Collection instances = objectLinkList.getLinkFrom(objectId, "instance");
        for (Iterator it = instances.iterator(); it.hasNext(); ) {
            int inst = ((Integer) it.next()).intValue();
            if (first == true) {
                sb.append("(" + inst);
                first = false;
            } else sb.append(", " + inst);
        }
        Collection kids = objectLinkList.getLinkFrom(objectId, "isa");
        for (Iterator it = kids.iterator(); it.hasNext(); ) {
            int kid = ((Integer) it.next()).intValue();
            first = buildFindInList(kid, sb, first);
        }
        return first;
    }

    public Collection findInstance(int subtreeRoot, BooleanTree attrTree, BooleanTree prepTree) throws FidoDatabaseException, BooleanSyntaxException, ObjectNotFoundException, ClassLinkTypeNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                Vector matches = new Vector();
                StringBuffer sb = new StringBuffer("select ObjectId from Objects where ObjectId in ");
                boolean empty = buildFindInList(subtreeRoot, sb, true);
                if (empty == true) return matches; else sb.append(") ");
                BooleanTreeNode node = prepTree.getRootNode();
                recursiveBuildFindPrep(subtreeRoot, node, sb);
                node = attrTree.getRootNode();
                recursiveBuildFindAttr(subtreeRoot, node, sb);
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sb.toString());
                while (rs.next() == true) {
                    Integer id = new Integer(rs.getInt(1));
                    matches.add(id);
                }
                return matches;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    public int hashCode(String id) throws FidoDatabaseException, ObjectNotFoundException {
        return hashCode(Integer.parseInt(id));
    }

    public int hashCode(int id) throws FidoDatabaseException, ObjectNotFoundException {
        String description = getObjectDescription(id);
        Vector list = new Vector();
        list.add(description);
        return list.hashCode();
    }
}
