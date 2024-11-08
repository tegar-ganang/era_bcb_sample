package com.manydesigns.portofino.base;

import com.manydesigns.portofino.base.cache.Query;
import com.manydesigns.portofino.base.calculations.DependenceGraph2;
import com.manydesigns.portofino.base.operations.MDClassOperation;
import com.manydesigns.portofino.base.operations.MDClassOperationVisitor;
import com.manydesigns.portofino.base.operations.MDObjectOperation;
import com.manydesigns.portofino.base.operations.MDObjectSetOperation;
import com.manydesigns.portofino.base.path.MDPathElement;
import com.manydesigns.portofino.base.portal.MDPortlet;
import com.manydesigns.portofino.base.users.MDMetaUserGroup;
import com.manydesigns.portofino.base.users.User;
import com.manydesigns.portofino.base.util.MDAttributeComparator;
import com.manydesigns.portofino.base.util.MDRelAttributeComparator;
import com.manydesigns.portofino.base.util.SelfTestVisitor;
import com.manydesigns.portofino.base.workflow.MDClassWfTransitionVisitor;
import com.manydesigns.portofino.base.workflow.MDWfState;
import com.manydesigns.portofino.base.workflow.MDWfTransition;
import com.manydesigns.portofino.util.Defs;
import com.manydesigns.portofino.util.Escape;
import com.manydesigns.portofino.util.RuleSet;
import com.manydesigns.portofino.util.Util;
import com.manydesigns.portofino.database.DatabaseAbstraction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author Paolo Predonzani - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo      - angelo.lupo@manydesigns.com
 */
public class MDClass {

    /**
     * Copyright (c) 2005-2009, ManyDesigns srl
     */
    public static final String copyright = "Copyright (c) 2005-2009, ManyDesigns srl";

    private static final boolean RANDOMIZE_SEQUENCES = false;

    private static Random randomizer = new Random();

    private final MDConfig config;

    private final int id;

    private final String name;

    private final String escapedName;

    private final String prettyName;

    private final String prettyPlural;

    private final boolean isAbstract;

    private final boolean isTab;

    private final Integer order;

    private final boolean relationship;

    private final boolean dontLink;

    private volatile int sequence;

    private MDClass parent;

    private MDWfAttribute myWorkflow;

    private MDRelAttribute myContext;

    private boolean permFree;

    private boolean immutable;

    private final Collection<MDClass> ownChildren;

    private final Set<MDAttribute> ownAttributes;

    private final Collection<MDRelAttribute> ownRelAttributes;

    private final Collection<MDMetaUserGroup> ownVisibility;

    private final Collection<MDActor> ownActors;

    private final Collection<MDPathElement> ownPathElements;

    private final Collection<MDPortlet> ownPortlets;

    private final Collection<MDJasperReports> ownJasperReports;

    private final TreeSet<MDAttribute> allAttributes;

    private final TreeSet<MDRelAttribute> allRelAttributes;

    private final Collection<MDPathElement> allPathElements;

    private final Collection<MDPortlet> allPortlets;

    private final Collection<MDJasperReports> allJasperReports;

    private String objectActualClassQuery;

    private String actualClassQuery;

    private final String loadObjectQuery;

    private String nameQueryFragment;

    private final Hashtable<Integer, MDClass> refCache;

    private final Collection<MDClassListener> listeners;

    private final Log log = LogFactory.getLog(MDClass.class);

    private int inNameAttributeCount;

    private int inSummaryAttributeCount;

    private final MDThreadLocals threadLocals;

    private final String schema1;

    private final Locale locale;

    public MDClass(MDConfig config, int id, String name, boolean isAbstract, boolean isTab, String prettyName, String prettyPlural, Integer order, boolean relationship, boolean dontLink, Connection conn, MDThreadLocals threadLocals, String schema1, Locale locale) throws Exception {
        this.config = config;
        this.id = id;
        this.name = name;
        this.prettyName = prettyName;
        this.prettyPlural = prettyPlural;
        this.relationship = relationship;
        this.dontLink = dontLink;
        this.escapedName = Escape.dbSchemaEscape(name);
        this.isAbstract = isAbstract;
        this.isTab = isTab;
        this.order = order;
        this.threadLocals = threadLocals;
        this.schema1 = schema1;
        this.locale = locale;
        parent = null;
        myWorkflow = null;
        permFree = true;
        immutable = true;
        ownChildren = new ArrayList<MDClass>();
        ownRelAttributes = new ArrayList<MDRelAttribute>();
        ownVisibility = new ArrayList<MDMetaUserGroup>();
        ownActors = new ArrayList<MDActor>();
        ownAttributes = new TreeSet<MDAttribute>(new MDAttributeComparator());
        ownPathElements = new ArrayList<MDPathElement>();
        ownPortlets = new ArrayList<MDPortlet>();
        ownJasperReports = new ArrayList<MDJasperReports>();
        allAttributes = new TreeSet<MDAttribute>(new MDAttributeComparator());
        allRelAttributes = new TreeSet<MDRelAttribute>(new MDRelAttributeComparator());
        allPathElements = new ArrayList<MDPathElement>();
        allPortlets = new ArrayList<MDPortlet>();
        allJasperReports = new ArrayList<MDJasperReports>();
        refCache = new Hashtable<Integer, MDClass>();
        listeners = new HashSet<MDClassListener>();
        try {
            computeSequence(conn);
        } catch (Exception e) {
            String dbProductName = conn.getMetaData().getDatabaseProductName();
            if ("DB2/LINUX".equals(dbProductName) || "DB2/NT".equals(dbProductName)) {
                PreparedStatement st = null;
                String query = MessageFormat.format("SET INTEGRITY FOR \"{0}\".\"{1}\" IMMEDIATE CHECKED", schema1, getEscapedName());
                try {
                    st = conn.prepareStatement(query);
                    st.execute();
                    computeSequence(conn);
                } finally {
                    try {
                        if (st != null) {
                            st.close();
                        }
                    } catch (Exception ex) {
                    }
                }
            } else {
                throw e;
            }
        }
        loadObjectQuery = MessageFormat.format("SELECT * FROM \"{0}\".\"{1}_view\" c WHERE c.\"id\" = ?", schema1, getEscapedName());
    }

    private void computeSequence(Connection conn) throws Exception {
        String query = MessageFormat.format("SELECT MAX(\"id\") AS \"maxId\" FROM \"{0}\".\"{1}\"", schema1, getEscapedName());
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement(query);
            rs = st.executeQuery();
            if (!rs.next()) {
                throw new Exception(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Unable_to_get_the_max_id"));
            }
            if (rs.getObject("maxId") == null) {
                if (RANDOMIZE_SEQUENCES) this.sequence += (randomizer.nextInt() % 10000); else this.sequence = 0;
            } else {
                this.sequence = rs.getInt("maxId");
            }
        } finally {
            try {
                rs.close();
            } catch (Exception ex) {
            }
            try {
                st.close();
            } catch (Exception ex) {
            }
        }
    }

    public void registerParent(MDClass parent) {
        this.parent = parent;
    }

    public void registerChild(MDClass child) {
        ownChildren.add(child);
    }

    public void registerAttribute(MDAttribute attr) throws Exception {
        ownAttributes.add(attr);
    }

    public void registerRelAttribute(MDRelAttribute relAttr) {
        ownRelAttributes.add(relAttr);
    }

    public void registerVisibility(MDMetaUserGroup mug) {
        ownVisibility.add(mug);
    }

    public void registerActor(MDActor actor) {
        ownActors.add(actor);
    }

    public void registerPortlet(MDPortlet portlet) {
        ownPortlets.add(portlet);
    }

    public void registerJasperReports(MDJasperReports jasperReports) {
        ownJasperReports.add(jasperReports);
    }

    protected void postResetSetup() throws Exception {
        fillAllAttributes(allAttributes);
        fillAllRelAttributes(allRelAttributes);
        fillAllPathElements(allPathElements);
        fillAllPortlets(allPortlets);
        fillAllJasperReports(allJasperReports);
        findMyWorkflow();
        findMyContext();
        StringBuffer sb = new StringBuffer();
        sb.append(" ELSE NULL END AS \"actual_class_id\" ");
        doAc(sb, this);
        sb.insert(0, "SELECT c" + getId() + ".\"id\" AS \"id\", CASE");
        actualClassQuery = sb.toString();
        sb.append(" WHERE c" + getId() + ".\"id\" = ?");
        objectActualClassQuery = sb.toString();
        StringBuffer nqfb = new StringBuffer();
        MDClassVisitor nqfv = new NameQueryFragmentVisitor(nqfb);
        visit(nqfv, false);
        nameQueryFragment = nqfb.toString();
        computeAttributeCountsImmutable();
    }

    private void computeAttributeCountsImmutable() {
        inNameAttributeCount = 0;
        inSummaryAttributeCount = 0;
        for (MDAttribute current : allAttributes) {
            if (current.isInName()) {
                inNameAttributeCount++;
            }
            if (current.isInSummary()) {
                inSummaryAttributeCount++;
            }
            immutable = immutable && current.isImmutable();
        }
    }

    public String getNameQueryFragment() {
        return nameQueryFragment;
    }

    public int getInNameAttributeCount() {
        return inNameAttributeCount;
    }

    public int getInSummaryAttributeCount() {
        return inSummaryAttributeCount;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void visit(MDConfigVisitor visitor) {
        visitor.doClassPre(this);
        visitor.doAttributeListPre();
        for (MDAttribute attr : ownAttributes) {
            attr.visit(visitor);
        }
        visitor.doAttributeListPost();
        visitor.doClassVisibilityListPre();
        for (MDMetaUserGroup mug : ownVisibility) {
            visitor.doClassVisibility(mug);
        }
        visitor.doClassVisibilityListPost();
        visitor.doActorListPre();
        for (MDActor actor : ownActors) {
            actor.visit(visitor);
        }
        visitor.doActorListPost();
        visitor.doClassListenerListPre();
        for (MDClassListener listener : listeners) {
            if (listener instanceof MDScriptClassListener) {
                visitor.doScriptClassListener((MDScriptClassListener) listener);
            } else {
                visitor.doJavaClassListener(listener);
            }
        }
        visitor.doClassListenerListPost();
        visitor.doClassPost();
    }

    private class NameQueryFragmentVisitor implements MDClassAttributeVisitor {

        private final StringBuffer sb;

        private final Transaction tx;

        private boolean first;

        private final DatabaseAbstraction mdDataBase;

        public NameQueryFragmentVisitor(StringBuffer sb) throws Exception {
            this.sb = sb;
            tx = threadLocals.getCurrentTransaction();
            first = true;
            mdDataBase = tx.getMDDataBase();
        }

        public void doClassPre(MDClass cls) throws Exception {
        }

        public void doAttributeListPre() throws Exception {
        }

        public void doAttributeGroupPre(String groupName) throws Exception {
        }

        public void doIntegerAttribute(MDIntegerAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!attribute.isInName()) return;
            if (first) {
                first = false;
            } else {
                mdDataBase.addSqlSpace(sb);
            }
            mdDataBase.caseCastIntegerToString(sb, attribute.getEscapedName());
        }

        public void doTextAttribute(MDTextAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!attribute.isInName()) return;
            if (first) {
                first = false;
            } else {
                mdDataBase.addSqlSpace(sb);
            }
            mdDataBase.caseTextToString(sb, attribute.getEscapedName());
        }

        public void doBooleanAttribute(MDBooleanAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!attribute.isInName()) return;
            if (first) {
                first = false;
            } else {
                mdDataBase.addSqlSpace(sb);
            }
            mdDataBase.caseCastBooleanToString(sb, attribute.getEscapedName(), config.getConfigContainer().getMDDataBase().getDbDataEscape(attribute.formatValue(Boolean.TRUE)), config.getConfigContainer().getMDDataBase().getDbDataEscape(attribute.formatValue(Boolean.FALSE)));
        }

        public void doDateAttribute(MDDateAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!attribute.isInName()) return;
            if (first) {
                first = false;
            } else {
                mdDataBase.addSqlSpace(sb);
            }
            MDConfig config = attribute.getOwnerClass().getConfig();
            String format = config.getLocaleInfo().getDateFormat().toUpperCase();
            mdDataBase.caseCastDateToString(sb, attribute.getEscapedName(), format);
        }

        public void doDecimalAttribute(MDDecimalAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!attribute.isInName()) return;
            if (first) {
                first = false;
            } else {
                mdDataBase.addSqlSpace(sb);
            }
            mdDataBase.caseCastDecimalToString(sb, attribute.getEscapedName());
        }

        public void doWfAttribute(MDWfAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!attribute.isInName()) return;
            if (first) {
                first = false;
            } else {
                mdDataBase.addSqlSpace(sb);
            }
            if (attribute.getWfStates().isEmpty()) {
                sb.append("''");
            } else {
                sb.append("(CASE");
                for (Object o : attribute.getWfStates()) {
                    MDWfState wfs = (MDWfState) o;
                    sb.append(" WHEN \"");
                    sb.append(attribute.getEscapedName());
                    sb.append("\"=");
                    sb.append(wfs.getId());
                    sb.append(" THEN '");
                    sb.append(config.getConfigContainer().getMDDataBase().getDbDataEscape(wfs.getName()));
                    sb.append("'");
                }
                sb.append(" ELSE '' END)");
            }
        }

        public void doRelAttribute(MDRelAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
            if (!attribute.isInName()) return;
            if (first) {
                first = false;
            } else {
                mdDataBase.addSqlSpace(sb);
            }
            mdDataBase.addRelAttributeInName(sb, attribute.getEscapedName(), mdDataBase.getDbDataEscape(attribute.getOppositeEndCls().getName()));
        }

        public void doBlobAttribute(MDBlobAttribute attribute, boolean calculated, boolean canRead, boolean canWrite) throws Exception {
        }

        public void doAttributeGroupPost() throws Exception {
        }

        public void doAttributeListPost() throws Exception {
            if (first) {
                String escapedName = mdDataBase.getDbDataEscape(getName());
                mdDataBase.addNotInNameAttribute(sb, escapedName);
            }
        }

        public void doClassPost() throws Exception {
        }
    }

    private void doAc(StringBuffer sb, MDClass baseCls) {
        sb.insert(0, " WHEN c" + getId() + ".\"id\" IS NOT NULL THEN " + getId());
        if (this == baseCls) {
            sb.append("FROM \"" + schema1 + "\".\"" + getEscapedName() + "\" c" + getId());
        } else {
            sb.append(" LEFT JOIN \"" + schema1 + "\".\"" + getEscapedName() + "\" c" + getId() + " ON c" + getId() + ".\"id\" = c" + baseCls.getId() + ".\"id\"");
        }
        for (Object anOwnChildren : ownChildren) {
            MDClass child = (MDClass) anOwnChildren;
            child.doAc(sb, baseCls);
        }
    }

    private void findMyWorkflow() throws Exception {
        myWorkflow = null;
        for (Object allAttribute : allAttributes) {
            MDAttribute attr = (MDAttribute) allAttribute;
            if (attr instanceof MDWfAttribute) {
                if (myWorkflow != null) {
                    throw new Exception(Util.getLocalizedString(Defs.MDLIBI18N, locale, "More_than_one_workflow_attribute_is_not_allowed"));
                } else {
                    myWorkflow = (MDWfAttribute) attr;
                }
            }
        }
    }

    private void findMyContext() throws Exception {
        myContext = null;
        for (Object allAttribute : allAttributes) {
            MDAttribute attr = (MDAttribute) allAttribute;
            if (attr instanceof MDRelAttribute && ((MDRelAttribute) attr).isContext()) {
                if (myContext != null) {
                    throw new Exception(Util.getLocalizedString(Defs.MDLIBI18N, locale, "More_than_one_context_attribute_is_not_allowed"));
                } else {
                    myContext = (MDRelAttribute) attr;
                }
            }
        }
    }

    public MDConfig getConfig() {
        return config;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEscapedName() {
        return escapedName;
    }

    public String getPrettyName() {
        return prettyName;
    }

    public String getPrettyPlural() {
        return prettyPlural;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isTab() {
        return isTab;
    }

    public Integer getOrder() {
        return order;
    }

    public boolean isRelationship() {
        return relationship;
    }

    public boolean isDontLink() {
        return dontLink;
    }

    public MDClass getParent() {
        return parent;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getSchema1() {
        return schema1;
    }

    public Collection<MDClass> getOwnChildren() {
        return ownChildren;
    }

    public boolean isDescendantOf(MDClass ancestor) {
        if (ancestor == this) return true;
        if (getParent() == null) return false;
        return getParent().isDescendantOf(ancestor);
    }

    public Integer descendantDistanceFrom(MDClass ancestor) {
        if (ancestor == this) {
            return 0;
        }
        if (getParent() == null) {
            return null;
        }
        Integer parentDistance = getParent().descendantDistanceFrom(ancestor);
        if (parentDistance == null) {
            return null;
        } else {
            return parentDistance + 1;
        }
    }

    public int getInheritanceDepth() {
        if (getParent() == null) return 0;
        return getParent().getInheritanceDepth() + 1;
    }

    public synchronized MDObject getMDObject(int oid) throws Exception {
        Transaction tx = threadLocals.getCurrentTransaction();
        return tx.getObject(this, oid);
    }

    public synchronized MDClass getObjectActualClass(int oid) throws Exception {
        Transaction tx = threadLocals.getCurrentTransaction();
        MDClass result;
        if (getParent() == null) {
            result = refCache.get(new Integer(oid));
            if (log.isDebugEnabled()) {
                log.debug(MessageFormat.format("MDClass '{0}': querying refCache " + "with oid = {1}. Result: {2}.", getName(), oid, (result == null ? "null" : result.getName())));
            }
            if (result == null) {
                PreparedStatement st = null;
                ResultSet rs = null;
                try {
                    st = tx.prepareStatement(objectActualClassQuery);
                    st.clearParameters();
                    st.setInt(1, oid);
                    rs = st.executeQuery();
                    if (!rs.next()) {
                        String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Object_not_found"), getName(), oid);
                        throw new MDObjectNotFoundException(msg);
                    }
                    int c_id = rs.getInt("actual_class_id");
                    result = config.getMDClassById(c_id);
                    refCache.put(oid, result);
                    if (log.isDebugEnabled()) {
                        log.debug(MessageFormat.format("MDClass '{0}': inserting " + "into refCache: oid = {1}  result = {2}.", getName(), oid, (result == null ? "null" : result.getName())));
                    }
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (Exception ignore) {
                        }
                    }
                    if (st != null) {
                        try {
                            st.close();
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        } else {
            result = getParent().getObjectActualClass(oid);
        }
        if (!result.isDescendantOf(this)) {
            String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Object_of_incompatible_class"), getName(), oid, result.getName());
            throw new MDObjectNotFoundException(msg);
        }
        return result;
    }

    public boolean canCreate() throws Exception {
        if (isPermissionFree() || threadLocals.getCurrentUser() == null) return true;
        MDObject obj = createNewMDObject(-1);
        return obj.canCreate();
    }

    private void checkVisibility(RuleSet result) throws Exception {
        for (Object anOwnVisibility : ownVisibility) {
            result.incrementRuleCount();
            MDMetaUserGroup mug = (MDMetaUserGroup) anOwnVisibility;
            if (mug.check()) {
                result.setValue(true);
                return;
            }
        }
        if (getParent() != null) {
            getParent().checkVisibility(result);
        }
    }

    public boolean isVisible() throws Exception {
        if (threadLocals.getCurrentUser() == null) return true;
        RuleSet result = new RuleSet();
        checkVisibility(result);
        if (result.getRuleCount() == 0) result.setValue(true);
        return result.getValue();
    }

    public void wrapQueryWithPermissions(Query query) {
        wrapQueryWithPermissions(query, "id");
    }

    public void wrapQueryWithPermissions(Query query, String idColumn) {
        if (threadLocals.getCurrentUser() == null || isPermissionFree()) {
            return;
        }
        query.insert(0, "SELECT q.* FROM (");
        query.append(") q");
        MDWfAttribute wfAttr = getWorkflow();
        if (wfAttr != null) {
            MDClass wfOwnerCls = wfAttr.getOwnerClass();
            if (wfOwnerCls != this) {
                query.append(" JOIN \"" + schema1 + "\".\"" + wfOwnerCls.getEscapedName() + "\" c" + wfOwnerCls.getId() + " ON c" + wfOwnerCls.getId() + ".\"id\" = q.\"" + idColumn + "\"");
            }
        }
        Query wb = new Query();
        wrapOneClass(query, wb, true, idColumn);
        if (wb.isEmpty()) {
            wb.append(" WHERE q.\"" + idColumn + "\" IS NULL");
        } else {
            wb.append(" OR q.\"" + idColumn + "\" IS NULL");
        }
        query.append(wb);
    }

    private void wrapOneClass(Query query, Query wb, boolean doParents, String idColumn) {
        joinForWrap(doParents, query, idColumn);
        MDClass current = this;
        while (current != null) {
            for (Object ownActor : current.ownActors) {
                MDActor actor = (MDActor) ownActor;
                handleActorForWrap(actor, wb);
            }
            current = current.getParent();
        }
        for (Object anOwnChildren : ownChildren) {
            MDClass sub = (MDClass) anOwnChildren;
            sub.wrapOneClass(query, wb, false, idColumn);
        }
    }

    private void handleActorForWrap(MDActor actor, Query wb) {
        Query tb = new Query();
        User currentUser = config.getCurrentUser();
        if (actor instanceof MDGroupActor) {
            MDMetaUserGroup mug = ((MDGroupActor) actor).getMetaUserGroup();
            if (currentUser.isUserInRole(mug)) {
                tb.append("c");
                tb.append(getId());
                tb.append(".\"id\" IS NOT NULL");
            } else {
                tb.append("(0 = 1)");
            }
        } else if (actor instanceof MDPathActor) {
            MDActorHead ah = ((MDPathActor) actor).getActorHead();
            tb.append("c" + getId() + ".\"id\" IN (");
            tb.append(ah.getReverseQuery());
            tb.append(")");
            tb.setInt(currentUser.getUserId());
        } else {
            throw new IllegalStateException(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Actor_type_not_recognized"));
        }
        MDWfAttribute workflow = getWorkflow();
        if (workflow == null) {
            if (actor.canReadSomething(null, this)) {
                if (wb.isEmpty()) {
                    wb.append(" WHERE ");
                } else {
                    wb.append(" OR ");
                }
                wb.append(tb);
            }
        } else {
            String escapedWfName = Escape.dbSchemaEscape(workflow.getName());
            if (wb.isEmpty()) {
                wb.append(" WHERE (");
            } else {
                wb.append(" OR (");
            }
            wb.append(tb);
            wb.append(" AND (");
            boolean first = true;
            if (actor.canReadSomething(null, this)) {
                if (first) {
                    first = false;
                } else {
                    wb.append(" OR ");
                }
                wb.append("c");
                wb.append(workflow.getOwnerClass().getId());
                wb.append(".\"");
                wb.append(escapedWfName);
                wb.append("\" IS NULL");
            }
            for (Object o : workflow.getWfStates()) {
                MDWfState state = (MDWfState) o;
                if (actor.canReadSomething(state, this)) {
                    if (first) {
                        first = false;
                    } else {
                        wb.append(" OR ");
                    }
                    wb.append("c");
                    wb.append(workflow.getOwnerClass().getId());
                    wb.append(".\"");
                    wb.append(escapedWfName);
                    wb.append("\" = ");
                    wb.append(state.getId());
                }
            }
            if (first) {
                wb.append("0 = 1");
            }
            wb.append("))");
        }
    }

    private void joinForWrap(boolean doJoin, Query query, String idColumn) {
        if (doJoin) {
            query.append(" JOIN \"");
        } else {
            query.append(" LEFT JOIN \"");
        }
        query.append(schema1);
        query.append("\".\"");
        query.append(getEscapedName());
        query.append("\" c");
        query.append(getId());
        query.append(" ON c");
        query.append(getId());
        query.append(".\"id\" = q.\"" + idColumn + "\"");
    }

    public MDObject createNewMDObject() throws Exception {
        Transaction tx = threadLocals.getCurrentTransaction();
        return tx.createNewMDObject(this, getNewId());
    }

    public MDObject createNewMDObject(int oid) throws Exception {
        Transaction tx = threadLocals.getCurrentTransaction();
        return tx.createNewMDObject(this, oid);
    }

    public MDObject createNewMDObject(Integer oid) throws Exception {
        Transaction tx = threadLocals.getCurrentTransaction();
        return tx.createNewMDObject(this, oid);
    }

    public synchronized int getNewId() throws Exception {
        int seq_id;
        if (getParent() == null) {
            seq_id = ++sequence;
        } else {
            seq_id = getParent().getNewId();
        }
        return seq_id;
    }

    public String getSearchLink() throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        sb.append("Search?class=");
        sb.append(Escape.urlencode(getName()));
        return sb.toString();
    }

    public void visit(MDClassVisitor visitor, boolean withPerms) throws Exception {
        Set<MDAttribute> attrSet = getAllAttributes();
        Collection<MDWfTransition> wftSet = getAllTransitions();
        Set<MDObjectOperation> objOpSet = getObjectOperationsSortedByOrder();
        Set<MDRelAttribute> relAttrSet = getRelAttributes();
        visitor.doClassPre(this);
        if (visitor instanceof MDClassAttributeVisitor) {
            commonVisitAttributes(this, (MDClassAttributeVisitor) visitor, attrSet, attrSet, attrSet, withPerms);
        }
        if (visitor instanceof MDClassWfTransitionVisitor) {
            commonVisitTransitions(this, (MDClassWfTransitionVisitor) visitor, wftSet, wftSet, wftSet);
        }
        if (visitor instanceof MDClassOperationVisitor) {
            commonVisitOperations(this, (MDClassOperationVisitor) visitor, objOpSet, objOpSet);
        }
        if (visitor instanceof MDClassRelationshipVisitor) {
            commonVisitRelationships((MDClassRelationshipVisitor) visitor, relAttrSet);
        }
        visitor.doClassPost();
    }

    public void visit(MDObject obj, MDClassVisitor visitor) throws Exception {
        MDClass actualCls = obj.getActualClass();
        Set<MDAttribute> attrSet = null;
        Set<MDAttribute> attrReadSet = null;
        Set<MDAttribute> attrWriteSet = null;
        Collection<MDWfTransition> wftSet = null;
        Collection<MDWfTransition> wftRefSet = null;
        Collection<MDWfTransition> wftEnabledSet = null;
        Collection<MDObjectOperation> objOpSet = null;
        Collection<MDObjectOperation> objOpRefSet = null;
        Set<MDRelAttribute> relAttrSet = getRelAttributes();
        if (visitor instanceof MDClassAttributeVisitor) {
            attrSet = getAllAttributes();
            if (threadLocals.getCurrentUser() == null || actualCls.isPermissionFree()) {
                attrReadSet = attrSet;
                attrWriteSet = attrSet;
            } else {
                attrReadSet = new HashSet<MDAttribute>();
                attrWriteSet = new HashSet<MDAttribute>();
                actualCls.fillAttributes(attrReadSet, attrWriteSet, obj);
            }
        }
        if (visitor instanceof MDClassWfTransitionVisitor) {
            if (getWorkflow() == null) {
                wftSet = (Set<MDWfTransition>) Collections.EMPTY_SET;
                wftRefSet = wftSet;
                wftEnabledSet = wftSet;
            } else {
                wftSet = getAllTransitions();
                wftEnabledSet = new HashSet<MDWfTransition>();
                fillGuards(obj, wftSet, wftEnabledSet);
                if (threadLocals.getCurrentUser() == null || actualCls.isPermissionFree()) {
                    wftRefSet = wftSet;
                } else {
                    wftRefSet = new HashSet<MDWfTransition>();
                    actualCls.fillTransitions(wftRefSet, obj);
                }
            }
        }
        if (visitor instanceof MDClassOperationVisitor) {
            objOpSet = getObjectOperationsSortedByOrder();
            if (threadLocals.getCurrentUser() == null || actualCls.isPermissionFree()) {
                objOpRefSet = objOpSet;
            } else {
                objOpRefSet = new HashSet<MDObjectOperation>();
                actualCls.fillOperations(objOpRefSet, obj);
            }
        }
        visitor.doClassPre(this);
        if (visitor instanceof MDClassAttributeVisitor) {
            commonVisitAttributes(actualCls, (MDClassAttributeVisitor) visitor, attrSet, attrReadSet, attrWriteSet, true);
        }
        if (visitor instanceof MDClassWfTransitionVisitor) {
            commonVisitTransitions(actualCls, (MDClassWfTransitionVisitor) visitor, wftSet, wftRefSet, wftEnabledSet);
        }
        if (visitor instanceof MDClassOperationVisitor) {
            commonVisitOperations(actualCls, (MDClassOperationVisitor) visitor, objOpSet, objOpRefSet);
        }
        if (visitor instanceof MDClassRelationshipVisitor) {
            commonVisitRelationships((MDClassRelationshipVisitor) visitor, relAttrSet);
        }
        visitor.doClassPost();
    }

    private void fillAllAttributes(Collection<MDAttribute> collection) {
        collection.addAll(ownAttributes);
        if (getParent() != null) getParent().fillAllAttributes(collection);
    }

    private void fillAllPathElements(Collection<MDPathElement> collection) {
        collection.addAll(ownPathElements);
        if (getParent() != null) getParent().fillAllPathElements(collection);
    }

    private void fillAllPortlets(Collection<MDPortlet> collection) {
        collection.addAll(ownPortlets);
        if (getParent() != null) getParent().fillAllPortlets(collection);
    }

    private void fillAllJasperReports(Collection<MDJasperReports> collection) {
        collection.addAll(ownJasperReports);
        if (getParent() != null) getParent().fillAllJasperReports(collection);
    }

    private void commonVisitAttributes(MDClass refCls, MDClassAttributeVisitor visitor, Set<MDAttribute> attrSet, Set<MDAttribute> attrReadSet, Set<MDAttribute> attrWriteSet, boolean withPerms) throws Exception {
        visitor.doAttributeListPre();
        boolean isFirstAttr = true;
        String currentGroupName = null;
        for (MDAttribute attr : attrSet) {
            if (withPerms && !attr.isVisible()) continue;
            String groupName = attr.getGroupName();
            if (isFirstAttr) {
                currentGroupName = groupName;
                visitor.doAttributeGroupPre(currentGroupName);
                isFirstAttr = false;
            } else {
                if (groupName != null && !groupName.equals(currentGroupName)) {
                    visitor.doAttributeGroupPost();
                    currentGroupName = groupName;
                    visitor.doAttributeGroupPre(currentGroupName);
                }
            }
            boolean calculated = attr.isCalculated();
            boolean canRead = attrReadSet.contains(attr);
            boolean canWrite = attrWriteSet.contains(attr);
            if (attr instanceof MDIntegerAttribute) {
                visitor.doIntegerAttribute((MDIntegerAttribute) attr, calculated, canRead, canWrite);
            } else if (attr instanceof MDTextAttribute) {
                visitor.doTextAttribute((MDTextAttribute) attr, calculated, canRead, canWrite);
            } else if (attr instanceof MDBooleanAttribute) {
                visitor.doBooleanAttribute((MDBooleanAttribute) attr, calculated, canRead, canWrite);
            } else if (attr instanceof MDDateAttribute) {
                visitor.doDateAttribute((MDDateAttribute) attr, calculated, canRead, canWrite);
            } else if (attr instanceof MDDecimalAttribute) {
                visitor.doDecimalAttribute((MDDecimalAttribute) attr, calculated, canRead, canWrite);
            } else if (attr instanceof MDWfAttribute) {
                visitor.doWfAttribute((MDWfAttribute) attr, calculated, canRead, canWrite);
            } else if (attr instanceof MDRelAttribute) {
                visitor.doRelAttribute((MDRelAttribute) attr, calculated, canRead, canWrite);
            } else if (attr instanceof MDBlobAttribute) {
                visitor.doBlobAttribute((MDBlobAttribute) attr, calculated, canRead, canWrite);
            } else {
                throw new Exception("Tipo sconosciuto");
            }
        }
        if (!isFirstAttr) {
            visitor.doAttributeGroupPost();
        }
        visitor.doAttributeListPost();
    }

    public void fillAttributes(Set<MDAttribute> readSet, Set<MDAttribute> writeSet, MDObject obj) throws Exception {
        for (MDActor actor : ownActors) {
            actor.fillAttributes(readSet, writeSet, obj);
        }
        if (getParent() != null) getParent().fillAttributes(readSet, writeSet, obj);
    }

    public void fillTransitions(Collection<MDWfTransition> refSet, MDObject obj) throws Exception {
        for (MDActor actor : ownActors) {
            actor.fillTransitions(refSet, obj);
        }
        if (getParent() != null) getParent().fillTransitions(refSet, obj);
    }

    public void fillOperations(Collection<MDObjectOperation> refSet, MDObject obj) throws Exception {
        for (MDActor actor : ownActors) {
            actor.fillOperations(refSet, obj);
        }
        if (getParent() != null) getParent().fillOperations(refSet, obj);
    }

    private void fillGuards(MDObject obj, Collection<MDWfTransition> set, Collection<MDWfTransition> enabledSet) throws Exception {
        for (MDWfTransition wft : set) {
            if (wft.isEnabled(obj)) enabledSet.add(wft);
        }
    }

    private void commonVisitTransitions(MDClass refCls, MDClassWfTransitionVisitor visitor, Collection<MDWfTransition> wftSet, Collection<MDWfTransition> wftRefSet, Collection<MDWfTransition> wftEnabledSet) throws Exception {
        visitor.doWfTransitionListPre();
        for (MDWfTransition wft : wftSet) {
            boolean canDo = wftRefSet.contains(wft);
            boolean enabled = wftEnabledSet.contains(wft);
            visitor.doWfTransition(wft, canDo, enabled);
        }
        visitor.doWfTransitionListPost();
    }

    private Collection<MDWfTransition> getAllTransitions() {
        if (getWorkflow() == null) {
            return (Set<MDWfTransition>) Collections.EMPTY_SET;
        } else {
            return getWorkflow().getAllWfTransitions();
        }
    }

    private void commonVisitOperations(MDClass refCls, MDClassOperationVisitor visitor, Collection<MDObjectOperation> opSet, Collection<MDObjectOperation> opRefSet) throws Exception {
        visitor.doOperationListPre();
        for (MDObjectOperation op : opSet) {
            boolean canDo = opRefSet.contains(op);
            visitor.doOperation(op, canDo);
        }
        visitor.doOperationListPost();
    }

    public Set<MDObjectOperation> getObjectOperationsSortedByOrder() {
        LinkedHashSet<MDObjectOperation> objectOperationOrder = new LinkedHashSet<MDObjectOperation>();
        Set<MDObjectOperation> objectOperations = config.getAllObjectOperationsSortedByOrder();
        for (MDObjectOperation objOperation : objectOperations) {
            if (objOperation.getCls() == null || this.isDescendantOf(objOperation.getCls())) objectOperationOrder.add(objOperation);
        }
        return objectOperationOrder;
    }

    public Set<MDObjectSetOperation> getObjectSetOperationsSortedByOrder() {
        LinkedHashSet<MDObjectSetOperation> objectOperationOrder = new LinkedHashSet<MDObjectSetOperation>();
        Set<MDObjectSetOperation> objectSetOperations = config.getAllObjectSetOperationsSortedByOrder();
        for (MDObjectSetOperation objectSetOperation : objectSetOperations) {
            if (objectSetOperation.getCls() == null || this.isDescendantOf(objectSetOperation.getCls())) objectOperationOrder.add(objectSetOperation);
        }
        return objectOperationOrder;
    }

    public Set<MDClassOperation> getClassOperationsSortedByOrder() {
        LinkedHashSet<MDClassOperation> clsOperationOrder = new LinkedHashSet<MDClassOperation>();
        Set<MDClassOperation> classOperations = config.getAllClassOperationsSortedByOrder();
        for (MDClassOperation clsOperation : classOperations) {
            if (clsOperation.getCls() == null || this.isDescendantOf(clsOperation.getCls())) clsOperationOrder.add(clsOperation);
        }
        return clsOperationOrder;
    }

    private void fillAllRelAttributes(Collection<MDRelAttribute> collection) {
        collection.addAll(ownRelAttributes);
        if (getParent() != null) getParent().fillAllRelAttributes(collection);
    }

    protected Set<MDRelAttribute> getRelAttributes() {
        return allRelAttributes;
    }

    private void commonVisitRelationships(MDClassRelationshipVisitor visitor, Set<MDRelAttribute> relAttrSet) throws Exception {
        visitor.doRelationshipListPre();
        boolean isFirstRel = true;
        String currentGroupName = null;
        for (MDRelAttribute current : relAttrSet) {
            Integer order = current.getOppositeEndOrder();
            String groupName = null;
            if (order != null) {
                groupName = "" + (order / 100);
            }
            if (isFirstRel) {
                currentGroupName = groupName;
                visitor.doRelationshipGroupPre(currentGroupName);
                isFirstRel = false;
            } else {
                if ((groupName != null && !groupName.equals(currentGroupName)) || (groupName == null && currentGroupName != null)) {
                    visitor.doRelationshipGroupPost();
                    currentGroupName = groupName;
                    visitor.doRelationshipGroupPre(currentGroupName);
                }
            }
            visitor.doRelationship(current);
        }
        if (!isFirstRel) {
            visitor.doRelationshipGroupPost();
        }
        visitor.doRelationshipListPost();
    }

    protected boolean calculatePermFree1() {
        permFree = ownActors.isEmpty();
        for (MDClass child : ownChildren) {
            permFree = permFree && child.calculatePermFree1();
        }
        return permFree;
    }

    protected boolean calculatePermFree2() {
        if (getParent() != null) {
            permFree = permFree && getParent().calculatePermFree2();
        }
        return permFree;
    }

    public boolean isPermissionFree() {
        return permFree;
    }

    public void addListener(MDClassListener listener) {
        listeners.add(listener);
    }

    public void notifyCreateListenersPre(MDObject obj) throws Exception {
        if (getParent() != null) {
            getParent().notifyCreateListenersPre(obj);
        }
        for (MDClassListener listener : listeners) {
            listener.notifyCreatePre(this, obj);
        }
    }

    public void notifyCreateListenersPost(MDObject obj) throws Exception {
        if (getParent() != null) {
            getParent().notifyCreateListenersPost(obj);
        }
        for (MDClassListener listener : listeners) {
            listener.notifyCreatePost(this, obj);
        }
    }

    public void notifyDeleteListenersPre(MDObject obj) throws Exception {
        if (getParent() != null) {
            getParent().notifyDeleteListenersPre(obj);
        }
        for (MDClassListener listener : listeners) {
            listener.notifyDeletePre(this, obj);
        }
    }

    public void notifyDeleteListenersPost(MDObject obj) throws Exception {
        if (getParent() != null) {
            getParent().notifyDeleteListenersPost(obj);
        }
        for (MDClassListener listener : listeners) {
            listener.notifyDeletePost(this, obj);
        }
    }

    public void notifyUpdateListenersPre(MDObject obj) throws Exception {
        if (getParent() != null) {
            getParent().notifyUpdateListenersPre(obj);
        }
        for (MDClassListener listener : listeners) {
            listener.notifyUpdatePre(this, obj);
        }
    }

    public void selfTest(Collection<String> errors) {
        Transaction tx = threadLocals.getCurrentTransaction();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            MDClassVisitor visitor = new SelfTestVisitor(this, errors);
            this.visit(visitor, false);
            String query = MessageFormat.format("SELECT * FROM \"{0}\".\"{1}_view\"", schema1, getName());
            st = tx.prepareStatement(query);
            rs = st.executeQuery();
        } catch (Exception e) {
            errors.add(e.getMessage());
        } finally {
            try {
                rs.close();
            } catch (Exception e) {
            }
            try {
                st.close();
            } catch (Exception e) {
            }
        }
    }

    public void notifyUpdateListenersPost(MDObject obj) throws Exception {
        if (getParent() != null) {
            getParent().notifyUpdateListenersPost(obj);
        }
        for (MDClassListener listener : listeners) {
            listener.notifyUpdatePost(this, obj);
        }
    }

    public void visitObject(MDObject obj, MDObjectVisitor visitor) throws Exception {
        MDClassVisitor mdcav = MDObjectMDClassVisitor.wrap(obj, visitor);
        visit(obj, mdcav);
    }

    protected String getLoadObjectQuery() {
        return loadObjectQuery;
    }

    public MDWfAttribute getWorkflow() {
        return myWorkflow;
    }

    public MDRelAttribute getContext() {
        return myContext;
    }

    public Set<MDAttribute> getAllAttributes() {
        return allAttributes;
    }

    public MDAttribute getAttributeByName(String name) throws Exception {
        for (MDAttribute current : allAttributes) {
            if (current.getName().equals(name)) return current;
        }
        String msg = MessageFormat.format(Util.getLocalizedString(Defs.MDLIBI18N, locale, "Attribute_not_found"), name);
        throw new Exception(msg);
    }

    public Collection<MDPortlet> getAllPortlets() {
        return allPortlets;
    }

    public Collection<MDJasperReports> getAllJasperReports() {
        return allJasperReports;
    }

    public void getPath(boolean advanced, Collection<String> results) throws Exception {
        Collection<MDClass> clsIgnoreList = new ArrayList<MDClass>();
        Collection<MDRelAttribute> relAttrIgnoreList = new ArrayList<MDRelAttribute>();
        if (!advanced) {
            clsIgnoreList.add(this);
        }
        getPath(advanced, clsIgnoreList, relAttrIgnoreList, results, "\"" + this.getName() + "\"", 1);
        if (!advanced) {
            clsIgnoreList.remove(this);
        }
    }

    private void getPath(boolean advanced, Collection<MDClass> clsIgnoreList, Collection<MDRelAttribute> relAttrIgnoreList, Collection<String> results, String path, int stepNumber) throws Exception {
        if (isDescendantOf(config.getMDClassByName(Defs.USER_CLS))) {
            results.add(path);
        }
        if (stepNumber > config.getMaxPathSearchDepth()) return;
        Set<MDRelAttribute> relAttr = getRelAttributes();
        for (Object aRelAttr : relAttr) {
            MDRelAttribute relAttribute = (MDRelAttribute) aRelAttr;
            String newPath = "UpRelSegment(" + path + ",\"" + relAttribute.getOppositeEndName() + "\")";
            MDClass ownerCls = relAttribute.getOwnerClass();
            if (advanced && !relAttrIgnoreList.contains(relAttribute)) {
                relAttrIgnoreList.add(relAttribute);
                ownerCls.getPath(advanced, clsIgnoreList, relAttrIgnoreList, results, newPath, stepNumber + 1);
                relAttrIgnoreList.remove(relAttribute);
            } else if (!advanced && !ownerCls.isContainedWithInheritance(clsIgnoreList)) {
                clsIgnoreList.add(ownerCls);
                ownerCls.getPath(advanced, clsIgnoreList, relAttrIgnoreList, results, newPath, stepNumber + 1);
                clsIgnoreList.remove(ownerCls);
            }
        }
        Set<MDAttribute> attributes = getAllAttributes();
        for (Object attribute1 : attributes) {
            MDAttribute attribute = (MDAttribute) attribute1;
            if (!(attribute instanceof MDRelAttribute)) continue;
            MDRelAttribute relAttribute = (MDRelAttribute) attribute;
            String newPath = "DownRelSegment(" + path + ",\"" + relAttribute.getName() + "\")";
            MDClass oppositeCls = relAttribute.getOppositeEndCls();
            if (advanced && !relAttrIgnoreList.contains(relAttribute)) {
                relAttrIgnoreList.add(relAttribute);
                oppositeCls.getPath(advanced, clsIgnoreList, relAttrIgnoreList, results, newPath, stepNumber + 1);
                relAttrIgnoreList.remove(relAttribute);
            } else if (!advanced && !oppositeCls.isContainedWithInheritance(clsIgnoreList)) {
                clsIgnoreList.add(oppositeCls);
                oppositeCls.getPath(advanced, clsIgnoreList, relAttrIgnoreList, results, newPath, stepNumber + 1);
                clsIgnoreList.remove(oppositeCls);
            }
        }
    }

    protected boolean isContainedWithInheritance(Collection<MDClass> v) {
        if (v.contains(this)) return true;
        MDClass parent = this.getParent();
        if (parent == null) return false;
        return parent.isContainedWithInheritance(v);
    }

    public void propagateDeps(DependenceGraph2 graph, MDObject obj, boolean addLinks) throws Exception {
        for (Object allPathElement : allPathElements) {
            MDPathElement element = (MDPathElement) allPathElement;
            element.propagateDeps(graph, obj, addLinks);
        }
    }

    public void propagateDeps(DependenceGraph2 graph, MDObject obj, MDAttribute attr, boolean addLinks) throws Exception {
        for (Object allPathElement : allPathElements) {
            MDPathElement element = (MDPathElement) allPathElement;
            element.propagateDeps(graph, obj, attr, addLinks);
        }
    }

    public void registerPathElement(MDPathElement element) {
        ownPathElements.add(element);
    }

    public String getActualClassQuery() {
        return actualClassQuery;
    }

    public MDThreadLocals getThreadLocals() {
        return threadLocals;
    }

    public boolean hasBlobAttribute() {
        for (Object allAttribute : allAttributes) {
            if ((MDAttribute) allAttribute instanceof MDBlobAttribute) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }
}
