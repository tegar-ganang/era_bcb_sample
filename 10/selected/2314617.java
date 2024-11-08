package uk.co.weft.pres.server;

import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import uk.co.weft.dbutil.*;
import uk.co.weft.htform.*;

/**
 * Add or edit an category in the database.
 *
 * @author Simon Brooke
 * @version $Revision: 1.9 $ This revision: $Author: simon_brooke $
 */
public class Category extends TableWrapperForm {

    /** the names of fields in my table: the key field */
    public static final String KEYFN = "Category";

    /** the names of fields in my table: the name field */
    public static final String NAMEFN = "Name";

    /** the names of fields in my table: the parent field */
    public static final String PARENTFN = "Parent";

    /** the name of my table */
    public static final String TABLENAME = "CATEGORY";

    /** the root category */
    public static final int CATEGORYROOT = 0;

    /** the news category */
    public static final int CATEGORYNEWS = 1;

    /** a apecial widget set for editing the root category */
    protected WidgetSet rcws = new RootCategoryWidgetSet();

    /**
	 * set up widgets to edit each of my fields
	 */
    public void init(Context config) throws InitialisationException {
        table = TABLENAME;
        keyField = KEYFN;
        allowLogout = false;
        rcws.connect(this, "root", 50);
        Widget w = new Widget(NAMEFN, "Name", "A short, distinct name for this category");
        w.setMandatory(true);
        w.setSize(32);
        addWidget(w);
        rcws.addWidget(w);
        w = new Widget("Description", "Description", "A longer description for this category");
        w.setMandatory(true);
        w.setSize(128);
        addWidget(w);
        rcws.addWidget(w);
        w = addWidget(new HTMLTextAreaWidget("Introduction", "Introduction", "An introduction for this category"));
        rcws.addWidget(w);
        addWidget(new CategoryParentWidget(PARENTFN, "Parent", "The parent of this category, if any", TABLENAME, KEYFN, NAMEFN, true));
        w = addWidget(new DataMenuWidget("Lead", "Lead", "The lead story of this category, if any", "select Article, Title from ARTICLE"));
        rcws.addWidget(w);
        w = addWidget(new BooleanWidget("Ephemeral", "Ephemeral", "True if articles in this category should time out"));
        rcws.addWidget(w);
        FileMaybeUploadWidget fw = new FileMaybeUploadWidget("RSSTemplate", "List Template", "An XSL template to use for lists of this category.");
        fw.setSize(128);
        fw.valueType = FileMaybeUploadWidget.VALUE_IS_URL;
        addWidget(fw);
        rcws.addWidget(fw);
        fw = new FileMaybeUploadWidget("NITFTemplate", "Story Template", "An XSL template to use for stories in this category.");
        fw.setSize(128);
        fw.valueType = FileMaybeUploadWidget.VALUE_IS_URL;
        addWidget(fw);
        rcws.addWidget(fw);
        w = addWidget(new LinkTableWidget("CATEGORY_FEED", "Syndication feeds", "Syndication feeds you want to associate with this category", false, 8, TABLENAME, Feed.TABLENAME, KEYFN, Feed.KEYFN, Feed.DESCRFN));
        rcws.addWidget(w);
        super.init(config);
        maybeDeleteWidgets.addWidget(new HiddenWidget("Parent"));
        rcws.addWidget(identWidget);
        rcws.addWidget(updateWidget);
        if (allowLogout) {
            rcws.addWidget(logoutWidget);
        }
        try {
            Auxiliary sublist = new Auxiliary("SUBSCRIBED_CATEGORY_VIEW", "Subscriber", TABLENAME, KEYFN, "subscriber", "Users subscribed to this category", "subscriber_name");
            sublist.canAdd = false;
            addAuxiliary(sublist);
        } catch (Exception e) {
        }
    }

    /**
	 * Is the category implied by the context its own ancestor?
	 *
	 * @param context the service context
	 *
	 * @return true if it is else false
	 */
    protected boolean isOwnAncestor(Context context) throws DataStoreException {
        boolean result = false;
        Integer category = context.getValueAsInteger(keyField);
        if (category != null) {
            int cat = category.intValue();
            Connection db = null;
            PreparedStatement ps = null;
            try {
                db = context.getConnection();
                ps = db.prepareStatement("select parent from category where category = ?");
                while (cat != CATEGORYROOT) {
                    ps.setInt(1, cat);
                    ResultSet r = ps.executeQuery();
                    if (r.next()) {
                        cat = r.getInt(1);
                    } else {
                        throw new DataFormatException("Serious: category ancestry is corrupt.");
                    }
                    if (cat == category.intValue()) {
                        result = true;
                    }
                }
            } catch (SQLException sex) {
                throw new DataStoreException("Could not read category ancestry");
            } finally {
                try {
                    if (db != null) {
                        context.releaseConnection(db);
                    }
                } catch (Exception e1) {
                }
            }
        }
        return result;
    }

    /**
	 * move any children, articles and events onto my parent before dropping
	 * me
	 */
    protected void drop(Context context) throws DataStoreException, ServletException {
        Integer category = context.getValueAsInteger(KEYFN);
        if (category != null) {
            if (category.intValue() == CATEGORYROOT) {
                throw new DataAuthException("You may not delete the root category");
            } else {
                Stack queries = new Stack();
                Object id = context.getValueAsString(keyField);
                Object parent = context.getValueAsString("parent");
                queries.push("update article set category = " + parent + " where category = " + id);
                queries.push("update event set category = " + parent + " where category = " + id);
                queries.push("update category set parent = " + parent + " where parent = " + id);
                fixupCategoryAncestry(context);
                super.drop(context, id, queries);
            }
        }
    }

    /**
	 * record in the database that this descendent, and, recursively, all it's
	 * descendents, are descendents of this ancestor
	 *
	 * @param ancestor the index of the ancestor category
	 * @param descendent the index of the descendent category
	 * @param db the database connection to use
	 * @param distance the distance between ancestor and descendent
	 */
    protected void fixupCategoryAncestry(Integer ancestor, Integer descendent, Connection db, int distance) throws SQLException {
        if ((ancestor != null) && (descendent != null)) {
            fixupCategoryAncestry(ancestor.intValue(), descendent.intValue(), db, distance);
        }
    }

    /**
	 * record in the database that this descendent, and, recursively, all it's
	 * descendents, are descendents of this ancestor
	 *
	 * @param ancestor the index of the ancestor category
	 * @param descendent the index of the descendent category
	 * @param db the database connection to use
	 * @param distance the distance between ancestor and descendent
	 */
    protected void fixupCategoryAncestry(int ancestor, int descendent, Connection db, int distance) throws SQLException {
        StringBuffer qb = new StringBuffer("insert into category_ancestry (ancestor, descendent, distance) values (");
        qb.append(ancestor).append(", ").append(descendent);
        qb.append(", ").append(distance).append(")");
        Statement s = db.createStatement();
        s.execute(qb.toString());
        qb = new StringBuffer("select * from category where parent = ");
        qb.append(descendent);
        ResultSet r = s.executeQuery(qb.toString());
        if (debug) {
            System.err.println("fixupCategoryAncestry: " + qb.toString() + "; distance: " + distance);
        }
        while (r.next()) {
            int child = r.getInt(KEYFN);
            if (child > 0) {
                fixupCategoryAncestry(ancestor, child, db, (distance + 1));
            }
        }
        s.close();
    }

    /**
	 * Walk the whole category ancestry from the root, fixing up all links.
	 */
    protected void fixupCategoryAncestry(Context context) throws DataStoreException {
        Connection db = null;
        Statement s = null;
        try {
            db = context.getConnection();
            db.setAutoCommit(false);
            s = db.createStatement();
            s.executeUpdate("delete from category_ancestry");
            walkTreeFixing(db, CATEGORYROOT);
            db.commit();
            context.put(Form.ACTIONEXECUTEDTOKEN, "Category Ancestry regenerated");
        } catch (SQLException sex) {
            try {
                db.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            throw new DataStoreException("Failed to refresh category ancestry");
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (db != null) {
                context.releaseConnection(db);
            }
        }
    }

    /**
	 * perform any processing of the contents of the context which has been
	 * passed back by the application, prior to passing it on to the user.
	 * Specialise this method to handle any postprocessing which is common to
	 * all widget sets; specialise WidgetSet.postProcess for any
	 * per-WidgetSet postprocessing and, of course, specialise
	 * Widget.postProcess for any per-widget postprocessing. Default does
	 * nothing.
	 *
	 * @param context the context in which this response is being generated
	 *
	 * @exception doesn 't really throw any exceptions but things which
	 * 			  specialise it may want to.
	 */
    protected void postProcess(Context context) throws DataStoreException, ServletException {
        Context menus = (Context) context.get(Form.CONTEXTMENUMAGICTOKEN);
        Integer cat = context.getValueAsInteger(keyField);
        if (cat != null) {
            menus.put("Lead", "select Article, Title from ARTICLE where Category = " + cat.toString() + " order by Title");
        }
        super.postProcess(context);
    }

    /**
	 * You can't store a category if it becomes its own ancestor (i.e. no
	 * cycles in the category hierarchy); once stored, fix up the hierarchy.
	 */
    protected boolean store(Context context) throws DataStoreException, ServletException {
        boolean result = false;
        Integer category = context.getValueAsInteger(KEYFN);
        if ((category != null) && (category.intValue() == CATEGORYROOT)) {
            context.put(Category.PARENTFN, null);
        }
        if (isOwnAncestor(context)) {
            throw new DataFormatException("You cannot make a category its own ancestor");
        } else {
            result = super.store(context);
            if (result) {
                fixupCategoryAncestry(context);
            }
        }
        return result;
    }

    /**
	 * walk the category ancestry from this category down, fixing up all the
	 * links
	 */
    private void walkTreeFixing(Connection db, int category) throws SQLException {
        StringBuffer qb = new StringBuffer("select category from category where parent = ");
        qb.append(category);
        fixupCategoryAncestry(category, category, db, 0);
        Statement s = db.createStatement();
        ResultSet r = s.executeQuery(qb.toString());
        while (r.next()) {
            int child = r.getInt(KEYFN);
            if (child > 0) {
                walkTreeFixing(db, child);
            }
        }
    }

    class CategoryParentWidget extends SimpleDataMenuWidget {

        /**
		 * as above but with an 'unset' option.
		 */
        public CategoryParentWidget(String name, String prompt, String help, String table, String idCol, String textCol, boolean allowUnset) {
            super(name, prompt, help, table, idCol, textCol, allowUnset);
        }

        /**
		 * Prevent circularities in the category tree -- ensure that this
		 * category is not it's own ancestor.
		 */
        protected void preProcess(Context context) throws DataStoreException, ServletException {
            super.preProcess(context);
            try {
                Connection db = context.getConnection();
                RSContexts result;
                Context row = context;
                Statement s = db.createStatement();
                Integer parent = context.getValueAsInteger(KEYFN);
                String qbase = "select * from category where category = ";
                Stack ancestors = new Stack();
                HttpServletRequest request = (HttpServletRequest) context.get(uk.co.weft.htform.Servlet.REQUESTMAGICTOKEN);
                while ((parent != null) && (request != null) && "POST".equals(request.getMethod())) {
                    result = new RSContexts(s.executeQuery(qbase + parent.toString()));
                    if (!result.isEmpty()) {
                        row = (Context) result.elementAt(0);
                        parent = row.getValueAsInteger("parent");
                        if ((parent != null) && (ancestors.indexOf(parent) != -1)) {
                            throw new DataFormatException("Category " + row.get(NAMEFN) + " is its own ancestor");
                        } else {
                            ancestors.push(parent);
                        }
                    } else {
                        throw new DataStoreException("Shouldn't happen: " + " parent of category " + row.get(NAMEFN) + " could not be found");
                    }
                }
                s.close();
                context.releaseConnection(db);
            } catch (SQLException sex) {
                throw new DataStoreException(sex.getMessage());
            }
        }
    }

    class RootCategoryWidgetSet extends WidgetSet {

        /**
		 * specialisation: select me if this is the root category
		 */
        protected int claim(Context context, Context whinges) throws Exception {
            int result = 0;
            Integer category = context.getValueAsInteger(KEYFN);
            if ((category != null) && (category.intValue() == CATEGORYROOT)) {
                result = precedence;
            }
            return result;
        }
    }
}
