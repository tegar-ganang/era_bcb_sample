package eccs.main;

import java.util.*;
import javax.swing.tree.*;
import java.io.File;
import java.sql.*;

/**
 * The DatabaseInterface class handles all interactions between the database and the
 * application side of the ECCS system.  In addition to handling many SQL queries itself,
 * it also accepts SQL queries in the form of strings and passes them to the database.
 * @author Daniel Fulton
 * @version 1.31.2007
 */
public class DatabaseInterface {

    /**
	 * id associated with main template.	  
	 */
    public static final int MAIN_TEMPLATE = 1;

    public static final int MAIN_FIELD = 1;

    public static final int TODO_TEMPLATE_ID = 2;

    public static final int TODO_DATEDUE_ID = 2;

    public static final int TODO_PRIORITY_ID = 3;

    public static final int TODO_COMPLETED_ID = 4;

    public static final int EVENT_TEMPLATE_ID = 3;

    public static final int EVENT_LOCATION_ID = 7;

    public static final int EVENT_START_ID = 5;

    public static final int EVENT_END_ID = 6;

    /**
	 * Reference to the main app
	 */
    private MainApp main = null;

    /**
	 * Connection to the note database
	 */
    private Connection conn;

    /**
	 * Constructor for DatabaseInterface
	 * @param server the server address
	 * @param user the username
	 * @param password the users password
	 * @param main reference to the main app
	 */
    public DatabaseInterface(String server, String user, String password, MainApp main, boolean embedded) throws Exception {
        this.main = main;
        conn = null;
        if (embedded) {
            System.out.println("Embedded");
            try {
                File path = new File("database");
                File lib = new File("database/lib");
                File data = new File("database/data");
                String url = "jdbc:mysql-embedded/eccs";
                Properties props = new Properties();
                props.put("library.path", lib.getCanonicalPath());
                props.put("--basedir", path.getCanonicalPath());
                props.put("--datadir", data.getCanonicalPath());
                props.put("--default-character-set", "utf8");
                props.put("--default-collation", "utf_general_ci");
                DriverManager.registerDriver(new com.mysql.embedded.jdbc.MySqlEmbeddedDriver());
                conn = DriverManager.getConnection(url, props);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getMessage() + "\nCannot connect to database server");
                System.err.println("server = " + server + ", user = " + user + ", password = " + password);
            }
        } else {
            System.out.println("Not Embedded");
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                Properties props = new Properties();
                props.put("useUnicode", "true");
                props.put("user", user);
                props.put("password", password);
                conn = DriverManager.getConnection(server, props);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getMessage() + "\nCannot connect to database server");
                System.err.println("server = " + server + ", user = " + user + ", password = " + password);
                throw e;
            }
        }
    }

    /**
	 * This method takes a note ID and querys the database for the info
	 * creates a NoteData object, then returns that NoteData.
	 * Logic: get the template, title, and keywords from the notes table
	 * then get the fields that template has
	 * for each field, determine its type, create a field object, and add it to a linkedlist
	 * then get the assocations for that note
	 * Create the note and return it.
	 * @return NoteData the requested note
	 */
    public NoteData openNote(int id) {
        try {
            String query;
            Statement stmt = conn.createStatement();
            query = "SELECT templateid, title, keywords FROM notes WHERE noteid = " + id;
            ResultSet rset = stmt.executeQuery(query);
            int templateid = -1;
            String title = "", keywords = "";
            if (rset.next()) {
                title = rset.getString("title");
                templateid = rset.getInt("templateid");
                keywords = rset.getString("keywords");
            } else {
                System.err.println("No resultset returned for main note select statement in DatabaseInterface.openNote(" + id + ")");
                return null;
            }
            Template t = main.getTemplate(templateid);
            if (t == null) {
                System.err.println("DatabaseInterface.openNote() - Unable to find template in memory, templateid = " + templateid);
                return null;
            }
            String data;
            LinkedList<FieldData> fields = new LinkedList<FieldData>();
            query = "SELECT fielddata.fieldid AS fieldid, data, fieldtype, name FROM (fielddata INNER JOIN templatefields ON fielddata.fieldid = templatefields.fieldid) WHERE noteid = " + id;
            rset = stmt.executeQuery(query);
            FieldData f = null;
            while (rset.next()) {
                switch(rset.getInt("fieldType")) {
                    case Field.TEXT:
                        data = new String(rset.getBytes("data"));
                        f = new FieldDataText(data);
                        break;
                    case Field.IMAGE:
                        f = new FieldDataImage(rset.getBytes("data"));
                        break;
                    case Field.TIME:
                        data = new String(rset.getBytes("data"));
                        f = new FieldDataTime(data);
                        break;
                    case Field.YESNO:
                        data = new String(rset.getBytes("data"));
                        f = new FieldDataYesNo(data);
                        break;
                    case Field.INTEGER:
                        data = new String(rset.getBytes("data"));
                        f = new FieldDataInteger(data);
                        break;
                    default:
                        System.err.println("Database corrupted, unrecognized fieldtype = " + rset.getInt("fieldtype"));
                        f = null;
                        break;
                }
                f.setName(rset.getString("name"));
                f.setID(rset.getInt("fieldid"));
                fields.add(f);
            }
            Vector<Link> assoc = new Vector<Link>();
            query = "SELECT note1id, note2id, title1, title2 " + "FROM (SELECT * FROM (SELECT * FROM links) L " + "INNER JOIN (SELECT noteid, title as title1 FROM notes) N1 " + "ON N1.noteid = L.note1id) R " + "INNER JOIN (Select noteid, title as title2 FROM notes) N2 " + "ON N2.noteid = R.note2id " + "WHERE note1id = " + id + " OR note2id = " + id;
            rset = stmt.executeQuery(query);
            while (rset.next()) {
                if (rset.getInt("note1id") == id) assoc.add(new Link(rset.getInt("note2id"), rset.getString("title2"))); else assoc.add(new Link(rset.getInt("note1id"), rset.getString("title1")));
            }
            NoteData note = NoteData.construct(title, t, fields, keywords, assoc, id);
            note.setDirty(false);
            return note;
        } catch (SQLException e) {
            System.err.println("Open Note failed: " + e.getMessage());
            return null;
        }
    }

    public NoteDataEvent[] openAllEvents() {
        try {
            Statement stmt = null;
            ResultSet rset = null;
            stmt = conn.createStatement();
            String query = "";
            LinkedList<NoteDataEvent> events = new LinkedList<NoteDataEvent>();
            LinkedList<Integer> queue = new LinkedList<Integer>();
            queue.add(new Integer(DatabaseInterface.EVENT_TEMPLATE_ID));
            while (queue.size() > 0) {
                int tid = queue.removeFirst().intValue();
                query = "SELECT noteid FROM notes WHERE templateid=" + tid;
                rset = stmt.executeQuery(query);
                while (rset.next()) {
                    events.add(new NoteDataEvent(openNote(rset.getInt("noteid"))));
                }
                rset.close();
                rset = stmt.executeQuery("SELECT templateid FROM templates WHERE parentid=" + tid);
                while (rset.next()) {
                    queue.add(new Integer(rset.getInt("templateid")));
                }
            }
            NoteDataEvent r[] = new NoteDataEvent[events.size()];
            r = events.toArray(r);
            return r;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public NoteDataTodo[] openAllTodos() {
        try {
            Statement stmt = null;
            ResultSet rset = null;
            stmt = conn.createStatement();
            String query = "";
            LinkedList<NoteDataTodo> todos = new LinkedList<NoteDataTodo>();
            LinkedList<Integer> queue = new LinkedList<Integer>();
            queue.add(new Integer(DatabaseInterface.TODO_TEMPLATE_ID));
            while (queue.size() > 0) {
                int tid = queue.removeFirst().intValue();
                query = "SELECT noteid FROM notes WHERE templateid=" + tid;
                rset = stmt.executeQuery(query);
                while (rset.next()) {
                    todos.add(new NoteDataTodo(openNote(rset.getInt("noteid"))));
                }
                rset.close();
                rset = stmt.executeQuery("SELECT templateid FROM templates WHERE parentid=" + tid);
                while (rset.next()) {
                    queue.add(new Integer(rset.getInt("templateid")));
                }
            }
            NoteDataTodo r[] = new NoteDataTodo[todos.size()];
            r = todos.toArray(r);
            return r;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 *
	 * @param n the note to be saved
	 * @return boolean indicates a successful save to database
	 */
    public boolean saveNote(NoteData n) {
        String query;
        try {
            conn.setAutoCommit(false);
            Statement stmt = null;
            ResultSet rset = null;
            stmt = conn.createStatement();
            query = "select * from notes where noteid = " + n.getID();
            rset = stmt.executeQuery(query);
            if (rset.next()) {
                query = "UPDATE notes SET title = '" + escapeCharacters(n.getTitle()) + "', keywords = '" + escapeCharacters(n.getKeywords()) + "' WHERE noteid = " + n.getID();
                try {
                    stmt.executeUpdate(query);
                } catch (SQLException e) {
                    e.printStackTrace();
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return false;
                }
                LinkedList<FieldData> fields = n.getFields();
                ListIterator<FieldData> iter = fields.listIterator(0);
                FieldData f = null;
                PreparedStatement pstmt = conn.prepareStatement("UPDATE fielddata SET data = ? WHERE noteid = ? AND fieldid = ?");
                try {
                    while (iter.hasNext()) {
                        f = iter.next();
                        if (f instanceof FieldDataImage) {
                            System.out.println("field is an image.");
                            pstmt.setBytes(1, ((FieldDataImage) f).getDataBytes());
                        } else {
                            System.out.println("field is not an image");
                            pstmt.setString(1, f.getData());
                        }
                        pstmt.setInt(2, n.getID());
                        pstmt.setInt(3, f.getID());
                        pstmt.execute();
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    e.printStackTrace();
                    return false;
                }
                query = "DELETE FROM links WHERE (note1id = " + n.getID() + " OR note2id = " + n.getID() + ")";
                try {
                    stmt.execute(query);
                } catch (SQLException e) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    e.printStackTrace();
                    return false;
                }
                Vector<Link> associations = n.getAssociations();
                ListIterator<Link> itr = associations.listIterator();
                Link association = null;
                pstmt = conn.prepareStatement("INSERT INTO links (note1id, note2id) VALUES (?, ?)");
                try {
                    while (itr.hasNext()) {
                        association = itr.next();
                        pstmt.setInt(1, n.getID());
                        pstmt.setInt(2, association.getID());
                        pstmt.execute();
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    e.printStackTrace();
                    return false;
                }
            } else {
                query = "INSERT INTO notes (templateid, title, keywords) VALUES (" + n.getTemplate().getID() + ", '" + escapeCharacters(n.getTitle()) + "', '" + escapeCharacters(n.getKeywords()) + "')";
                try {
                    stmt.executeUpdate(query);
                } catch (SQLException e) {
                    e.printStackTrace();
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return false;
                }
                LinkedList<FieldData> fields = n.getFields();
                ListIterator<FieldData> iter = fields.listIterator(0);
                FieldData f = null;
                n.setID(Integer.parseInt(executeMySQLGet("SELECT LAST_INSERT_ID()")));
                PreparedStatement pstmt;
                try {
                    pstmt = conn.prepareStatement("INSERT INTO fielddata (noteid, fieldid, data) VALUES (?,?,?)");
                    while (iter.hasNext()) {
                        f = iter.next();
                        if (f instanceof FieldDataImage) {
                            System.out.println("field is an image.");
                            pstmt.setBytes(3, ((FieldDataImage) f).getDataBytes());
                        } else {
                            System.out.println("field is not an image");
                            pstmt.setString(3, f.getData());
                        }
                        pstmt.setInt(1, n.getID());
                        pstmt.setInt(2, f.getID());
                        pstmt.execute();
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    e.printStackTrace();
                    return false;
                }
                Vector<Link> assoc = n.getAssociations();
                Iterator<Link> itr = assoc.listIterator();
                Link l = null;
                pstmt = conn.prepareStatement("INSERT INTO links (note1id, note2id) VALUES (?,?)");
                try {
                    while (itr.hasNext()) {
                        l = itr.next();
                        pstmt.setInt(1, n.getID());
                        pstmt.setInt(2, l.getID());
                        pstmt.execute();
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    e.printStackTrace();
                    return false;
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 *
	 * @param id unique identification number of the note
	 * @return boolean returns true on successful deletion
	 */
    public boolean deleteNote(int id) {
        if (id < 0) {
            return false;
        }
        try {
            conn.setAutoCommit(false);
            String query = "";
            Statement stmt = conn.createStatement();
            try {
                query = "DELETE FROM links WHERE (note1id = " + id + " OR note2id = " + id + ")";
                stmt.execute(query);
            } catch (SQLException ex) {
                conn.rollback();
                conn.setAutoCommit(true);
                return false;
            }
            try {
                query = "DELETE FROM fielddata WHERE noteid = " + id;
                stmt.execute(query);
            } catch (SQLException ex) {
                conn.rollback();
                conn.setAutoCommit(true);
                return false;
            }
            try {
                query = "DELETE FROM notes WHERE noteid = " + id;
                stmt.execute(query);
            } catch (SQLException ex) {
                conn.rollback();
                conn.setAutoCommit(true);
                return false;
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }

    /**
	 * opens all templates in database, stores them in
	 * a tree map the key of id being associated the
	 * Template object as the value
	 * @return TreeMap of templates
	 */
    public TreeMap<Integer, Template> openAllTemplates() {
        TreeMap<Integer, Template> templates = new TreeMap<Integer, Template>();
        ArrayList<Template> tList = new ArrayList<Template>();
        try {
            String name;
            LinkedList<Field> fields;
            Template parent;
            int pid;
            int id;
            String query = "SELECT * FROM templates;";
            Statement s = conn.createStatement();
            ResultSet rs1 = s.executeQuery(query);
            while (rs1.next()) {
                id = rs1.getInt("templateid");
                name = rs1.getString("name");
                fields = new LinkedList<Field>();
                parent = null;
                pid = rs1.getInt("parentid");
                Template temptemp = new Template(name, fields, parent, pid, id);
                tList.add(temptemp);
            }
            ResultSet rs2;
            Field fieldy;
            Iterator<Template> it = tList.iterator();
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM templatefields WHERE templateid = ?");
            while (it.hasNext()) {
                Template t = it.next();
                pstmt.setInt(1, t.getID());
                rs2 = pstmt.executeQuery();
                FieldData fd;
                while (rs2.next()) {
                    switch(rs2.getInt("fieldType")) {
                        case Field.TEXT:
                            fd = new FieldDataText(new String(rs2.getBytes("defaultvalue")));
                            break;
                        case Field.IMAGE:
                            fd = new FieldDataImage(rs2.getBytes("defaultvalue"));
                            break;
                        case Field.TIME:
                            System.out.println(new String(rs2.getBytes("defaultvalue")));
                            fd = new FieldDataTime(new String(rs2.getBytes("defaultvalue")));
                            break;
                        case Field.YESNO:
                            fd = new FieldDataYesNo(new String(rs2.getBytes("defaultvalue")));
                            break;
                        case Field.INTEGER:
                            fd = new FieldDataInteger(new String(rs2.getBytes("defaultvalue")));
                            break;
                        default:
                            System.err.println("Database corrupted, unrecognized fieldtype = " + rs2.getInt("fieldtype"));
                            fd = null;
                            break;
                    }
                    fieldy = new Field(rs2.getString("name"), rs2.getInt("fieldid"), rs2.getInt("fieldtype"), fd, t.getName());
                    t.addField(fieldy);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            templates = null;
        }
        Iterator<Template> itr1 = tList.iterator();
        while (itr1.hasNext()) {
            Template t1 = itr1.next();
            Iterator<Template> itr2 = tList.iterator();
            while (itr2.hasNext()) {
                Template t2 = itr2.next();
                if (t1.getParentID() == t2.getID()) {
                    t1.setParent(t2);
                    break;
                }
            }
        }
        Iterator it = tList.iterator();
        while (it.hasNext()) {
            Template t = (Template) it.next();
            templates.put(Integer.valueOf(t.getID()), t);
        }
        return templates;
    }

    /**
	 *
	 * @param t the Template to be saved
	 * @return boolean returns true on successful save
	 */
    public boolean saveTemplate(Template t) {
        try {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            String query;
            ResultSet rset;
            if (Integer.parseInt(executeMySQLGet("SELECT COUNT(*) FROM templates WHERE name='" + escapeCharacters(t.getName()) + "'")) != 0) return false;
            query = "select * from templates where templateid = " + t.getID();
            rset = stmt.executeQuery(query);
            if (rset.next()) {
                System.err.println("Updating already saved template is not supported!!!!!!");
                return false;
            } else {
                query = "INSERT INTO templates (name, parentid) VALUES ('" + escapeCharacters(t.getName()) + "', " + t.getParentID() + ")";
                try {
                    stmt.executeUpdate(query);
                } catch (SQLException e) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    e.printStackTrace();
                    return false;
                }
                int templateid = Integer.parseInt(executeMySQLGet("SELECT LAST_INSERT_ID()"));
                t.setID(templateid);
                LinkedList<Field> fields = t.getFields();
                ListIterator<Field> iter = fields.listIterator();
                Field f = null;
                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO templatefields(fieldtype, name, templateid, defaultvalue)" + "VALUES (?,?,?,?)");
                try {
                    while (iter.hasNext()) {
                        f = iter.next();
                        if (f.getType() == Field.IMAGE) {
                            System.out.println("field is an image.");
                            byte data[] = ((FieldDataImage) f.getDefault()).getDataBytes();
                            pstmt.setBytes(4, data);
                        } else {
                            System.out.println("field is not an image");
                            String deflt = (f.getDefault()).getData();
                            pstmt.setString(4, deflt);
                        }
                        pstmt.setInt(1, f.getType());
                        pstmt.setString(2, f.getName());
                        pstmt.setInt(3, t.getID());
                        pstmt.execute();
                        f.setID(Integer.parseInt(executeMySQLGet("SELECT LAST_INSERT_ID()")));
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    e.printStackTrace();
                    return false;
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException ex) {
            System.err.println("Error saving the Template");
            return false;
        }
        return true;
    }

    /**
	 * Removes the specified template and any children templates.
	 * Allows option of what to do with the notes that instance
	 * the templates. 
	 * @param id the unique identification number of the template
	 * @param keepNotes if true this means the notes of the specified template
	 * 			and any child templates' notes, will be kept and merged into
	 * 			the next template above, if false then all notes will be deleted	 
	 * @return boolean true indicates a successful deletion
	 *  @param notes LinkedList<Integer> of all ids of notes that were affected
	 * 			calling function should pass an empty LinkedList<Integer> and it
	 * 			will be populated
	 * @param templates LinkedList<Integer> of all ids of templates that were affected
	 * 			calling function should pass an empty LinkedList<Integer> and it
	 * 			will be populated
	 */
    public boolean deleteTemplate(int id, boolean keepNotes, LinkedList<Integer> notes, LinkedList<Integer> templates) {
        if ((id != DatabaseInterface.MAIN_TEMPLATE) && (id != DatabaseInterface.EVENT_TEMPLATE_ID) && (id != DatabaseInterface.TODO_TEMPLATE_ID)) {
            try {
                try {
                    conn.setAutoCommit(false);
                    Statement stmt = conn.createStatement();
                    Template cur = main.getTemplate(id);
                    Template newT = cur.getParent();
                    notes.addAll(deleteTemplate_notes(cur, newT, keepNotes, stmt));
                    templates.addAll(deleteTemplate_templates(id, stmt));
                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                    conn.rollback();
                }
                conn.setAutoCommit(true);
            } catch (SQLException eSQL) {
                eSQL.printStackTrace();
                System.err.println("Something terrible happened in delete template that required the second error catcher to rescue the first");
            }
        }
        return false;
    }

    /**
	 * returns a linkedlist of the ids of the notes
	 * that were changed or deleted	
	 * @param cur
	 * @param newT
	 * @param keepNotes
	 * @param stmt
	 * @return
	 * @throws Exception
	 */
    private LinkedList<Integer> deleteTemplate_notes(Template cur, Template newT, boolean keepNotes, Statement stmt) throws SQLException {
        ResultSet rs;
        LinkedList<Integer> ret = new LinkedList<Integer>();
        rs = stmt.executeQuery("SELECT templateid FROM templates WHERE parentid=" + cur.getID());
        LinkedList<Integer> list = new LinkedList<Integer>();
        while (rs.next()) {
            int id = rs.getInt("templateid");
            list.add(new Integer(id));
        }
        Iterator<Integer> itrT = list.iterator();
        while (itrT.hasNext()) {
            ret.addAll(deleteTemplate_notes(main.getTemplate(itrT.next().intValue()), newT, keepNotes, stmt));
        }
        LinkedList<Field> fieldsToRemove = new LinkedList<Field>();
        fieldsToRemove.addAll(cur.getAllFields());
        Iterator<Field> itr = newT.getAllFields().iterator();
        while (itr.hasNext()) {
            Iterator<Field> itr1 = fieldsToRemove.iterator();
            int id = itr.next().getID();
            while (itr1.hasNext()) {
                if (itr1.next().getID() == id) {
                    itr1.remove();
                    break;
                }
            }
        }
        rs = stmt.executeQuery("SELECT noteid FROM notes WHERE templateid=" + cur.getID());
        LinkedList<Integer> notelist = new LinkedList<Integer>();
        while (rs.next()) {
            notelist.add(new Integer(rs.getInt("noteid")));
        }
        Iterator<Integer> itrN = notelist.iterator();
        while (itrN.hasNext()) {
            int noteid = itrN.next().intValue();
            ret.add(new Integer(noteid));
            if (keepNotes) {
                String concat = "";
                Iterator<Field> itrF = fieldsToRemove.iterator();
                while (itrF.hasNext()) {
                    Field field = itrF.next();
                    ResultSet rs_field = stmt.executeQuery("SELECT data FROM fielddata WHERE (noteid=" + noteid + ") AND (fieldid=" + field.getID() + ")");
                    rs_field.next();
                    String d = new String(rs_field.getBytes("data"));
                    concat += "\n" + field.getName() + ": " + d;
                }
                ResultSet rs_desc = stmt.executeQuery("SELECT data FROM fielddata WHERE (noteid=" + noteid + ") AND (fieldid=" + DatabaseInterface.MAIN_FIELD + ")");
                rs_desc.next();
                String newData = new String(rs_desc.getBytes("data")) + concat;
                stmt.execute("UPDATE fielddata SET data='" + escapeCharacters(newData) + "' WHERE (noteid=" + noteid + ") AND (fieldid=" + DatabaseInterface.MAIN_FIELD + ")");
                stmt.execute("UPDATE notes SET templateid=" + newT.getID() + " WHERE noteid=" + noteid);
            } else {
                stmt.execute("DELETE FROM notes WHERE noteid=" + noteid);
                stmt.execute("DELETE FROM links WHERE note1id=" + noteid + " OR note2id = " + noteid);
                stmt.execute("DELETE FROM fielddata WHERE noteid=" + noteid);
            }
        }
        return ret;
    }

    private LinkedList<Integer> deleteTemplate_templates(int templateid, Statement stmt) throws SQLException {
        LinkedList<Integer> ret = new LinkedList<Integer>();
        ResultSet rs = stmt.executeQuery("SELECT templateid FROM templates WHERE parentid=" + templateid);
        LinkedList<Integer> list = new LinkedList<Integer>();
        while (rs.next()) {
            int id = rs.getInt("templateid");
            list.add(new Integer(id));
        }
        Iterator<Integer> itrT = list.iterator();
        while (itrT.hasNext()) {
            ret.addAll(deleteTemplate_templates(itrT.next().intValue(), stmt));
        }
        stmt.execute("DELETE FROM templatefields WHERE templateid=" + templateid);
        stmt.execute("DELETE FROM templates WHERE templateid=" + templateid);
        ret.add(new Integer(templateid));
        return ret;
    }

    /**
	 *
	 * @return NoteList the list of to-dos
	 */
    public NoteList getTodoList() {
        return new NoteList(0);
    }

    /**
	 *
	 * @return NoteList the list of events
	 */
    public NoteList getEventList() {
        return new NoteList(0);
    }

    /**
	 *
	 * @return TreeModel the BrowseTree
	 */
    public MutableTreeNode getBrowseTree() {
        TreeMap<Integer, NoteTreeNode> tempNodes = new TreeMap<Integer, NoteTreeNode>();
        Template[] temps = main.getTemplates();
        for (int i = 0; i < temps.length; i++) {
            NoteTreeNode t = new NoteTreeNode(temps[i].getID(), temps[i].getName(), true);
            tempNodes.put(new Integer(t.getID()), t);
        }
        try {
            String query = "SELECT noteid, title, templateid FROM notes ORDER BY title";
            Statement stmt = conn.createStatement();
            ResultSet rslts = stmt.executeQuery(query);
            while (rslts.next()) {
                NoteTreeNode note = new NoteTreeNode(rslts.getInt("noteid"), rslts.getString("title"), false);
                NoteTreeNode template = tempNodes.get(new Integer(rslts.getInt("templateid")));
                template.add(note);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createFullTree(tempNodes);
    }

    private MutableTreeNode createFullTree(TreeMap<Integer, NoteTreeNode> tempNodes) {
        Collection<NoteTreeNode> c = tempNodes.values();
        Iterator<NoteTreeNode> itr = c.iterator();
        while (itr.hasNext()) {
            NoteTreeNode nxt = itr.next();
            int prntID = main.getTemplate(nxt.getID()).getParentID();
            if (prntID != -1) {
                NoteTreeNode prnt = tempNodes.get(new Integer(prntID));
                prnt.add(nxt);
            }
        }
        return tempNodes.get(new Integer(MAIN_TEMPLATE));
    }

    /**
	 * Basic query return
	 * @param query sql query
	 * @return 2-d array of strings to represent the returned table
	 */
    public String[][] executeMySQL(String query) {
        return new String[0][0];
    }

    public String executeMySQLGet(String query) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getObject(1).toString();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * This method searches through the database. No longer searches twice.
	 * @param query sql query
	 * @return notelist of notes found in search query
	 */
    public NoteList searchDatabase(String query, int max, boolean hasMain, boolean hasNot, boolean hasAll, boolean hasExact, boolean fieldSearch) {
        try {
            System.out.println(query);
            Statement statement = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet results = statement.executeQuery(query);
            TreeSet<Integer> badIds = new TreeSet<Integer>();
            NoteList list = new NoteList(max);
            while (results.next()) {
                if (hasNot && (results.getBoolean("notDP") || results.getBoolean("notTP") || results.getBoolean("notKP"))) badIds.add(new Integer(results.getInt("noteid")));
            }
            results = statement.executeQuery(query);
            while (results.next() && list.getNumNotes() <= max) {
                Integer temp = new Integer(results.getInt("noteid"));
                if (!badIds.contains(temp) && (!hasAll || (results.getBoolean("allDP") && fieldSearch) || (results.getBoolean("allDP") || results.getBoolean("allTP") || results.getBoolean("allKP"))) && (!hasExact || (results.getBoolean("exactDP") && fieldSearch) || (results.getBoolean("exactDP") || results.getBoolean("exactTP") || results.getBoolean("exactKP"))) && (!hasMain || (results.getBoolean("mainDP") && fieldSearch) || (results.getBoolean("mainDP") || results.getBoolean("mainTP") || results.getBoolean("mainKP")))) {
                    list.addNote(results.getString("title"), new String(results.getBytes("mainData")), results.getInt("noteid"));
                    badIds.add(temp);
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new NoteList(0);
    }

    public String escapeCharacters(String oldString) {
        StringTokenizer t = new StringTokenizer(oldString, "'\\", true);
        String escapedString = "";
        while (t.hasMoreTokens()) {
            String next = t.nextToken();
            if (next.equals("'")) {
                next = "\\'";
            }
            if (next.equals("\\")) next = "\\\\";
            escapedString = escapedString + next;
        }
        return escapedString;
    }

    /**
	 * closes the interfaces connection to the database.
	 *
	 */
    public void closeConnection() {
        try {
            conn.close();
            com.mysql.embedded.jdbc.MySqlEmbeddedDriver.shutdown();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
