package onepoint.persistence.hibernate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpConstrainedMember;
import onepoint.persistence.OpField;
import onepoint.persistence.OpMember;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpPersistenceException;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpRelationship;
import onepoint.persistence.OpType;
import onepoint.persistence.OpTypeManager;
import onepoint.persistence.OpUserType;
import onepoint.persistence.OpUserType.FieldDescription;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.modules.customers.OpCustomer;
import onepoint.project.modules.documents.OpDocumentNode;
import onepoint.project.modules.documents.OpFolder;
import onepoint.project.modules.product.OpProduct;
import onepoint.project.modules.product.OpRelease;
import onepoint.project.modules.product.OpRequirement;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpAttachmentVersion;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.report.OpReportData;
import onepoint.project.modules.report.OpReportJRType;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.risks.OpRiskPlan;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.modules.work.OpTimeRecord;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XResourceBroker;
import org.hibernate.Hibernate;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.BooleanType;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.NullableType;
import org.hibernate.type.Type;
import org.hibernate.usertype.UserType;

/**
 * This class is responsible for generation of Hibernate mapping file for all loaded prototypes.
 *
 * @author calin.pavel
 */
public class OpMappingsGenerator {

    /**
    *
    */
    public static final String TYPE_SUFFIX = "_TYPE";

    public static final String ID_SUFFIX = "_ID";

    private static final XLog logger = XLogFactory.getLogger(OpMappingsGenerator.class);

    private static final String INDEX_NAME_PREFIX = "op_";

    private static final String INDEX_NAME_POSTFIX = "_i";

    private static final String COLUMN_NAME_PREFIX = "op_";

    private static final String COLUMN_NAME_POSTFIX = "";

    private static final String TABLE_NAME_PREFIX = "op_";

    private static final String TABLE_NAME_POSTFIX = "";

    private static final String JOIN_NAME_SEPARATOR = "_";

    private static final List<String> PROTOTYPE_PREFIXES = Arrays.asList("X", "Op");

    private static final int IDENTIFIER_NAME_LENGTH = 30;

    private static final int INDEX_NAME_LENGTH = 60;

    private static final int IBM_DB_2_INDEX_NAME_LENGTH = 18;

    private static final int ORACLE_INDEX_NAME_LENGTH = 30;

    private static final String NEW_LINE = "\n";

    private static final char SPACE_CHAR = ' ';

    private static final boolean USE_DYNAMIC = false;

    private static final String ID_GENERATOR_CLASS = "onepoint.persistence.hibernate.OpSynchronizedHiLoGenerator";

    private static final Set<String> UNIQUE_KEY_EXTENSIONS = new HashSet<String>(Arrays.asList("Site"));

    private final int databaseType;

    protected List<OpPrototype> rootTypes;

    protected Set<OpHibernateFilter> filters;

    private static Map<Integer, String> HIBERNATE_TYPE_NAME_MAP = new HashMap<Integer, String>();

    private static Map<Integer, NullableType> HIBERNATE_TYPE_MAP = new HashMap<Integer, NullableType>();

    private static Map<Integer, UserType> HIBERNATE_USER_TYPE_MAP = new HashMap<Integer, UserType>();

    static {
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.BOOLEAN), Hibernate.BOOLEAN);
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.INTEGER), Hibernate.INTEGER);
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.LONG), Hibernate.LONG);
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.STRING), Hibernate.STRING);
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.TEXT), Hibernate.STRING);
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.DATE), Hibernate.DATE);
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.BYTE), Hibernate.BYTE);
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.DOUBLE), Hibernate.DOUBLE);
        HIBERNATE_TYPE_MAP.put(Integer.valueOf(OpType.TIMESTAMP), Hibernate.TIMESTAMP);
        HIBERNATE_USER_TYPE_MAP.put(Integer.valueOf(OpType.CONTENT), new OpBlobUserType());
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.BOOLEAN), "boolean");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.INTEGER), "integer");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.LONG), "long");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.STRING), "string");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.TEXT), "string");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.DATE), "java.sql.Date");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.BYTE), "byte");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.DOUBLE), "double");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.TIMESTAMP), "timestamp");
        HIBERNATE_TYPE_NAME_MAP.put(Integer.valueOf(OpType.CONTENT), "onepoint.persistence.hibernate.OpBlobUserType");
    }

    private static OpMappingsGenerator generator;

    private static Map<Class, String> shortNameMap = new HashMap<Class, String>();

    static {
        shortNameMap.put(OpActivity.class, "AC");
        shortNameMap.put(OpActivityVersion.class, "ACV");
        shortNameMap.put(OpAttachment.class, "AT");
        shortNameMap.put(OpAttachmentVersion.class, "ATV");
        shortNameMap.put(OpCostRecord.class, "CR");
        shortNameMap.put(OpCustomer.class, "CU");
        shortNameMap.put(OpDocumentNode.class, "DN");
        shortNameMap.put(OpFolder.class, "FO");
        shortNameMap.put(OpProjectNode.class, "PN");
        shortNameMap.put(OpReport.class, "RP");
        shortNameMap.put(OpReportJRType.class, "RPT");
        shortNameMap.put(OpResource.class, "RE");
        shortNameMap.put(OpResourcePool.class, "REP");
        shortNameMap.put(OpTimeRecord.class, "TR");
        shortNameMap.put(OpProjectPlan.class, "PP");
        shortNameMap.put(OpProjectPlanVersion.class, "PPV");
        shortNameMap.put(OpRiskPlan.class, "RSKP");
        shortNameMap.put(OpReportData.class, "RD");
        shortNameMap.put(OpRelease.class, "REL");
        shortNameMap.put(OpProduct.class, "PROD");
        shortNameMap.put(OpRequirement.class, "REQ");
        shortNameMap.put(OpActivityComment.class, "CMT");
    }

    private static Map<String, Integer> identifierSequenceMap = new HashMap<String, Integer>();

    private static final int POSTFIX_LENGTH = 3;

    private static NumberFormat identifierEnumFormat = null;

    static {
        identifierEnumFormat = NumberFormat.getIntegerInstance();
        identifierEnumFormat.setGroupingUsed(false);
        identifierEnumFormat.setMinimumIntegerDigits(POSTFIX_LENGTH);
    }

    /**
    * Creates a new generator for the provided prototypes.
    *
    * @param databaseType data base type.
    */
    public OpMappingsGenerator(int databaseType) {
        this.databaseType = databaseType;
        initializeFilters();
    }

    protected void initializeFilters() {
    }

    /**
    * Initialize mappings generator with the prototypes for which mappings should be generated.
    *
    * @param initialPrototypes initial prototypes
    */
    public void init(Collection<OpPrototype> initialPrototypes) {
        if (initialPrototypes == null || initialPrototypes.isEmpty()) {
            logger.info("No prototype received for mapping.");
            return;
        }
        this.rootTypes = new ArrayList<OpPrototype>();
        for (OpPrototype prototype : initialPrototypes) {
            OpPrototype superType = prototype.getSuperType();
            if (superType == null) {
                this.rootTypes.add(prototype);
            }
        }
        logger.debug("End processing prototypes.");
    }

    /**
    * This method generates and returns Hibernate mappings XML content.
    */
    public String generateMappings() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version=\"1.0\"?>\n");
        buffer.append("<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n");
        buffer.append("<hibernate-mapping default-access=\"onepoint.persistence.hibernate.OpPropertyAccessor\">").append(NEW_LINE);
        Map<String, OpUserType> userTypeMap = OpTypeManager.getUserTypeMap();
        if (userTypeMap != null) {
            for (Map.Entry<String, OpUserType> userType : userTypeMap.entrySet()) {
                addCompositeTypeDef(OpHibernateCompositeType.class, buffer, userType, 0);
            }
        }
        for (OpPrototype prototype : rootTypes) {
            appendMapping(buffer, prototype, 0);
        }
        if (filters != null && filters.size() > 0) {
            for (OpHibernateFilter filter : filters) {
                addFilterDefinition(buffer, filter, 1);
            }
        }
        buffer.append("</hibernate-mapping>").append(NEW_LINE);
        String mappings = buffer.toString();
        final boolean debug = false;
        if (debug || logger.isLoggable(XLog.DEBUG)) {
            try {
                File fout = new File("HibernateMapping.hbm.xml");
                if (!fout.exists()) {
                    fout.createNewFile();
                }
                logger.info("writing mapping to file: " + fout.getAbsolutePath());
                FileWriter writer = new FileWriter(fout);
                writer.write(mappings);
                writer.close();
                logger.info("... done");
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
        return mappings;
    }

    private void addCompositeTypeDef(Class<?> typeClass, StringBuffer buffer, Entry<String, OpUserType> userType, int level) {
        level++;
        buffer.append(generateIndent(level)).append("<typedef");
        buffer.append(" name=\"").append(userType.getKey()).append("\"");
        buffer.append(" class=\"").append(typeClass.getName()).append("\"");
        buffer.append(">").append(NEW_LINE);
        buffer.append(generateIndent(level + 1)).append("<param");
        buffer.append(" name=\"").append(OpEnhancedTypeBase.USER_TYPE_NAME).append("\">");
        buffer.append(userType.getKey());
        buffer.append("</param>").append(NEW_LINE);
        buffer.append(generateIndent(level)).append("</typedef>").append(NEW_LINE);
    }

    /**
    * @return
    * @pre
    * @post
    */
    private Map<OpPrototype, Set<OpPrototype>> getInterfaceMap() {
        return null;
    }

    /**
    * @param buffer
    * @param prototype
    * @pre
    * @post
    */
    private void appendMapping(StringBuffer buffer, OpPrototype prototype, int level) {
        String[] implementingNames = prototype.getImplementingNames();
        if (prototype.getSuperType() == null) {
            generateRootClassMapping(buffer, prototype);
        } else {
            appendSubTypeMapping(buffer, prototype, level);
        }
    }

    /**
    * This method add to mapping content definition of a Hibernate filter.
    *
    * @param buffer mappings buffer
    * @param filter filter to add
    * @param level  indent level
    */
    private void addFilterDefinition(StringBuffer buffer, OpHibernateFilter filter, int level) {
        buffer.append(generateIndent(level)).append("<filter-def name=\"").append(filter.getName()).append("\">").append(NEW_LINE);
        if (filter.getParameters() != null) {
            for (Map.Entry entry : filter.getParameters().entrySet()) {
                String paramName = (String) entry.getKey();
                String paramType = (String) entry.getValue();
                buffer.append(generateIndent(level + 1)).append("<filter-param name=\"").append(paramName);
                buffer.append("\" type=\"").append(paramType).append("\"/>").append(NEW_LINE);
            }
        }
        buffer.append(generateIndent(level)).append("</filter-def>").append(NEW_LINE);
    }

    /**
    * This method should add if necessary tags for filters usage (class, relationship, ... level).
    *
    * @param buffer buffer where to add content
    * @param level  indent level.
    */
    private void addFiltersUsage(Class<? extends OpObjectIfc> type, StringBuffer buffer, int level) {
        if (filters != null && filters.size() > 0) {
            for (OpHibernateFilter filter : filters) {
                if (filter.getType().isAssignableFrom(type)) {
                    buffer.append(generateIndent(level)).append("<filter name=\"").append(filter.getName());
                    buffer.append("\" condition=\"").append(filter.getCondition()).append("\"/>").append(NEW_LINE);
                }
            }
        }
    }

    /**
    * Generates mapping for root type.
    *
    * @param buffer   buffer where to write mapping content
    * @param rootType root prototype
    */
    private void generateRootClassMapping(StringBuffer buffer, OpPrototype rootType) {
        if (rootType.isInterface()) {
            return;
        }
        int level = 1;
        buffer.append(generateIndent(level)).append("<class name=\"").append(rootType.getInstanceClass().getName());
        buffer.append("\" polymorphism=\"implicit\" ");
        if (USE_DYNAMIC) {
            buffer.append("dynamic-insert=\"true\" ");
            buffer.append("dynamic-update=\"true\" ");
        }
        String tableName = generateTableName(rootType.getName());
        rootType.setTableName(tableName);
        buffer.append("table=\"").append(tableName).append("\">").append(NEW_LINE);
        appendLine(buffer, "<cache usage=\"read-write\"/>", level + 1);
        appendLine(buffer, "<id name=\"id\" column=\"op_id\" type=\"long\" access=\"field\">", level + 1);
        appendLine(buffer, "<generator class=\"" + ID_GENERATOR_CLASS + "\">", level + 2);
        appendLine(buffer, "<param name=\"table\">" + OpHibernateSource.HILO_GENERATOR_TABLE_NAME + "</param>", level + 3);
        appendLine(buffer, "<param name=\"column\">" + OpHibernateSource.HILO_GENERATOR_COLUMN_NAME + "</param>", level + 3);
        appendLine(buffer, "</generator>", level + 2);
        appendLine(buffer, "</id>", level + 1);
        addMembers(buffer, rootType, level + 1);
        Iterator subTypesIt = rootType.subTypes();
        while (subTypesIt.hasNext()) {
            OpPrototype subType = (OpPrototype) subTypesIt.next();
            appendMapping(buffer, subType, level + 1);
        }
        addFiltersUsage(rootType.getInstanceClass(), buffer, level + 1);
        buffer.append(generateIndent(level)).append("</class>").append(NEW_LINE);
    }

    /**
    * @param buffer
    * @param type
    * @param interfaceType
    * @param member
    * @param level
    * @pre
    * @post
    */
    private void appendInterface(StringBuffer buffer, OpPrototype type, OpRelationship member, int level) {
        buffer.append(generateIndent(level)).append("<set name=\"").append(member.getName()).append("\"");
        buffer.append(" table=\"").append(generateTableName(member.getTypeName())).append("\"");
        buffer.append(" where=\"").append(COLUMN_NAME_PREFIX).append(member.getBackRelationship().getName()).append(TYPE_SUFFIX).append(COLUMN_NAME_POSTFIX).append("='").append(getIdForImplementingType(type)).append("'\"");
        buffer.append(">").append(NEW_LINE);
        buffer.append(generateIndent(level + 1)).append("<key foreign-key=\"none\">").append(NEW_LINE);
        buffer.append(generateIndent(level + 2)).append("<column name=\"").append(COLUMN_NAME_PREFIX).append(member.getBackRelationship().getName() + ID_SUFFIX).append(COLUMN_NAME_POSTFIX).append("\"/>").append(NEW_LINE);
        buffer.append(generateIndent(level + 1)).append("</key>").append(NEW_LINE);
        buffer.append(generateIndent(level + 1)).append("<one-to-many class=\"").append(OpTypeManager.getPrototypeByID(member.getTypeID()).getInstanceClass().getName()).append("\"/>").append(NEW_LINE);
        addFiltersUsage(type.getInstanceClass(), buffer, level + 1);
        buffer.append(generateIndent(level)).append("</set>").append(NEW_LINE);
    }

    /**
    * @param type
    * @return
    * @pre
    * @post
    */
    public static String getIdForImplementingType(OpPrototype type) {
        return getIdForImplementingType(type.getInstanceClass());
    }

    /**
    * @param type
    * @return
    * @pre
    * @post
    */
    public static String getIdForImplementingType(Class<?> type) {
        String shortName = shortNameMap.get(type);
        if (shortName == null) {
            shortName = type.getSimpleName() + ":" + type.getPackage().getName();
        }
        return shortName;
    }

    /**
    * Add mappings for prototype members.
    *
    * @param buffer    buffer where to write content.
    * @param prototype prototype for which we'll add members mappings
    * @param level     indent level
    */
    private void addMembers(StringBuffer buffer, OpPrototype prototype, int level) {
        Map<OpConstrainedMember, String> uniqueKeysMap = new HashMap<OpConstrainedMember, String>();
        String ukExtension = "";
        Set<OpMember> ukeMembers = new HashSet<OpMember>();
        Map<OpMember, Boolean> desclaredMembers = prototype.getResolvedDeclaredMembers();
        for (String uniqueKeyExtension : UNIQUE_KEY_EXTENSIONS) {
            OpMember uniquKeyExtensionField = prototype.getMember(uniqueKeyExtension);
            if (uniquKeyExtensionField instanceof OpConstrainedMember) {
                OpConstrainedMember ukecm = (OpConstrainedMember) uniquKeyExtensionField;
                ukExtension += "_" + ukecm.getName();
                ukeMembers.add(ukecm);
                desclaredMembers.put(ukecm, Boolean.FALSE);
            }
        }
        if (ukExtension.length() > 0) {
            Set<String> allUniqueKeys = new HashSet<String>();
            for (OpMember member : desclaredMembers.keySet()) {
                if (ukeMembers.contains(member)) {
                    continue;
                }
                if (member instanceof OpConstrainedMember) {
                    OpConstrainedMember f = (OpConstrainedMember) member;
                    String namedUniqueKey = getNamedUniqueKeyString(f, uniqueKeysMap.get(f));
                    if (f.getUnique()) {
                        String ukName = namedUniqueKey.length() > 0 ? namedUniqueKey : f.getName();
                        ukName += ukExtension;
                        String ukIndexName = generateIndexName(prototype.getName(), ukName);
                        allUniqueKeys.add(ukIndexName);
                        uniqueKeysMap.put(f, ukIndexName);
                    }
                }
            }
            if (!allUniqueKeys.isEmpty()) {
                for (String uniqueKeyExtension : UNIQUE_KEY_EXTENSIONS) {
                    OpMember uniquKeyExtensionField = prototype.getMember(uniqueKeyExtension);
                    if (uniquKeyExtensionField instanceof OpConstrainedMember) {
                        OpConstrainedMember ukecm = (OpConstrainedMember) uniquKeyExtensionField;
                        StringBuffer uks = new StringBuffer();
                        for (String uk : allUniqueKeys) {
                            if (uks.length() > 0) {
                                uks.append(" ");
                            }
                            uks.append(uk);
                        }
                        uniqueKeysMap.put(ukecm, uks.toString());
                    }
                }
            }
        }
        logger.debug("Unique Keys for Prototype: " + prototype.getName() + ": " + uniqueKeysMap);
        int depth = 0;
        OpPrototype t = prototype;
        while (t.getSuperType() != null) {
            depth++;
            t = t.getSuperType();
        }
        for (Entry<OpMember, Boolean> member : desclaredMembers.entrySet()) {
            OpMember m = member.getKey();
            if (m instanceof OpField) {
                addFieldMember(buffer, prototype, (OpField) m, level, uniqueKeysMap.get(m), depth);
            } else if (m instanceof OpRelationship) {
                addRelationMember(buffer, prototype, (OpRelationship) m, level, member.getValue().booleanValue(), uniqueKeysMap.get(m), depth);
            } else if (m instanceof OpUserType) {
                OpUserType c = (OpUserType) m;
                addCompositeMapping(buffer, prototype, c, level, depth);
            } else {
                logger.error("Invalid type for member: " + m.getName());
            }
        }
    }

    private void addCompositeMapping(StringBuffer b, OpPrototype prototype, OpUserType c, int level, int depth) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(generateIndent(level)).append("<property");
        buffer.append(" name=\"").append(c.getName()).append("\"");
        buffer.append(" type=\"").append(c.getUserTypeKey()).append("\"");
        buffer.append(">").append(NEW_LINE);
        Iterator<FieldDescription> fit = c.getFieldIterator();
        while (fit.hasNext()) {
            FieldDescription f = fit.next();
            if (f.getMember() instanceof OpField) {
                OpField field = (OpField) f.getMember();
                buffer.append(generateIndent(level + 1));
                addColumnTag(buffer, prototype, field, null, depth);
                buffer.append(NEW_LINE);
            }
        }
        buffer.append(generateIndent(level)).append("</property>").append(NEW_LINE);
        b.append(buffer);
    }

    /**
    * Add to mapping content prototype fields
    *
    * @param buffer bufffer where to add mapping
    * @param field  field to map
    * @param level  indent level
    */
    private void addFieldMember(StringBuffer buffer, OpPrototype prototype, OpField field, int level, String uniqueKeyName, int depth) {
        buffer.append(generateIndent(level)).append("<property name=\"").append(field.getName()).append("\"");
        buffer.append(" type=\"").append(getHibernateTypeName(field.getTypeID())).append("\"");
        if (field.getUpdate() != null) {
            buffer.append(" update=\"").append(field.getUpdate()).append("\"");
        }
        if (field.getInsert() != null) {
            buffer.append(" insert=\"").append(field.getInsert()).append("\"");
        }
        if (field.getMandatory()) {
            buffer.append(" not-null=\"true\"");
        }
        String namedUniqueKeys = getNamedUniqueKeyString(field, uniqueKeyName);
        boolean hasNamedUniqueKey = namedUniqueKeys.length() > 0;
        if (field.getUnique() && !hasNamedUniqueKey) {
            buffer.append(" unique=\"true\"");
        }
        if (hasNamedUniqueKey) {
            buffer.append(" unique-key=\"").append(namedUniqueKeys).append("\"");
        }
        buffer.append(">");
        addColumnTag(buffer, prototype, field, uniqueKeyName, depth);
        buffer.append("</property>").append(NEW_LINE);
    }

    private void addColumnTag(StringBuffer buffer, OpPrototype prototype, OpField field, String uniqueKeyName, int depth) {
        buffer.append("<column name=\"");
        String columnName = generateColumnName(prototype, field.getName(), field.getColumn(), depth);
        prototype.addMemberColumnName(field, columnName);
        buffer.append(columnName);
        buffer.append('"');
        if ((field.getTypeID() == OpType.CONTENT)) {
            if (databaseType == OpHibernateSource.MYSQL_INNODB) {
                buffer.append(" sql-type=\"longblob\"");
            }
            if (databaseType == OpHibernateSource.POSTGRESQL) {
                buffer.append(" sql-type=\"bytea\"");
            }
            if ((databaseType == OpHibernateSource.IBM_DB2)) {
                buffer.append(" sql-type=\"blob(100M)\"");
            }
            if (databaseType == OpHibernateSource.DERBY) {
                buffer.append(" sql-type=\"blob\"");
            }
        }
        if (field.getMandatory()) {
            buffer.append(" not-null=\"true\"");
        }
        String namedUniqueKeys = getNamedUniqueKeyString(field, uniqueKeyName);
        boolean hasNamedUniqueKey = namedUniqueKeys.length() > 0;
        if (field.getUnique() && !hasNamedUniqueKey) {
            buffer.append(" unique=\"true\"");
        }
        if (hasNamedUniqueKey) {
            buffer.append(" unique-key=\"").append(namedUniqueKeys).append("\"");
        }
        String defaultValue = field.getDefaultValue();
        if (defaultValue != null && defaultValue.trim().length() != 0) {
            if (field.getTypeID() == OpType.BOOLEAN) {
                defaultValue = getDefautBooleanValue(defaultValue, databaseType);
            }
            if (field.getTypeID() == OpType.STRING || field.getTypeID() == OpType.TEXT) {
                defaultValue = "'" + defaultValue + "'";
            }
            buffer.append(" default=\"").append(defaultValue).append("\"");
        }
        if (field.getIndexed() && field.getTypeID() != OpType.TEXT) {
            if (databaseType != OpHibernateSource.ORACLE || !field.getUnique()) {
                buffer.append(" index=\"");
                buffer.append(generateIndexName(prototype.getName(), field.getName()));
                buffer.append('\"');
            }
        }
        if (field.getTypeID() == OpType.TEXT) {
            buffer.append(" length=\"").append(OpTypeManager.getMaxTextLength()).append("\"");
        }
        buffer.append("/>");
    }

    public static String getDefautBooleanValue(String defaultValue, int databaseType) {
        boolean value = false;
        if ("true".equalsIgnoreCase(defaultValue)) {
            value = true;
        } else if ("1".equals(defaultValue)) {
            value = true;
        }
        final BooleanType type = new BooleanType();
        Dialect dialect = OpHibernateConfiguration.getDialect(databaseType);
        try {
            String retval = type.objectToSQLString(value, dialect);
            return retval;
        } catch (Exception e) {
            logger.error(e);
            return defaultValue;
        }
    }

    /**
    * Add to mapping content prototype relations
    *
    * @param buffer       bufffer where to add mapping
    * @param prototype    prototype for which we add relatiojship
    * @param relationship relationship to map
    * @param level        indent level
    * @param isInterface
 * @param depth
    */
    private void addRelationMember(StringBuffer buffer, OpPrototype prototype, OpRelationship relationship, int level, boolean isInterface, String uniqueKeyName, int depth) {
        String cascadeMode = relationship.getCascadeMode();
        String fetch = relationship.getFetch();
        String lazy = relationship.getLazy();
        String orderBy = relationship.getOrderBy();
        String sort = relationship.getSort();
        OpRelationship back_relationship = relationship.getBackRelationship();
        OpPrototype target_prototype = OpTypeManager.getPrototypeByID(relationship.getTypeID());
        String namedUniqueKeys = getNamedUniqueKeyString(relationship, uniqueKeyName);
        boolean hasNamedUniqueKey = namedUniqueKeys.length() > 0;
        if (!OpType.isCollectionType(relationship.getCollectionTypeID())) {
            if (target_prototype.isInterface()) {
                String relName = relationship.getName();
                buffer.append(generateIndent(level)).append("<any name=\"").append(relName).append("\"").append(" meta-type=\"string\"").append(" id-type=\"long\"").append(">").append(NEW_LINE);
                Iterator<OpPrototype> iter = target_prototype.subTypes();
                while (iter.hasNext()) {
                    OpPrototype implementingType = iter.next();
                    buffer.append(generateIndent(level + 1)).append("<meta-value value=\"").append(getIdForImplementingType(implementingType)).append("\" class=\"").append(implementingType.getInstanceClass().getName()).append("\"/>").append(NEW_LINE);
                }
                buffer.append(generateIndent(level + 1));
                String typeColumnName = COLUMN_NAME_PREFIX + relationship.getName() + TYPE_SUFFIX + COLUMN_NAME_POSTFIX;
                String idColumnName = COLUMN_NAME_PREFIX + relationship.getName() + ID_SUFFIX + COLUMN_NAME_POSTFIX;
                buffer.append("<column name=\"").append(typeColumnName).append("\"/>").append(NEW_LINE);
                buffer.append(generateIndent(level + 1)).append("<column name=\"").append(idColumnName).append("\" index=\"").append(generateIndexName(prototype.getName(), relationship.getName())).append("\"/>").append(NEW_LINE);
                buffer.append(generateIndent(level)).append("</any>").append(NEW_LINE);
            } else {
                String columnName = generateColumnName(prototype, relationship.getName(), relationship.getColumn(), depth);
                prototype.addMemberColumnName(relationship, columnName);
                if ((back_relationship != null) && !OpType.isCollectionType(back_relationship.getCollectionTypeID())) {
                    if (relationship.getInverse()) {
                        buffer.append(generateIndent(level)).append("<one-to-one name=\"");
                        buffer.append(relationship.getName());
                        buffer.append("\" property-ref=\"");
                        buffer.append(back_relationship.getName());
                        buffer.append("\"");
                        if (cascadeMode != null) {
                            buffer.append(" cascade=\"").append(cascadeMode).append("\"");
                        }
                        if (fetch != null) {
                            buffer.append(" fetch=\"").append(fetch).append("\"");
                        }
                        buffer.append("/>").append(NEW_LINE);
                    } else {
                        buffer.append(generateIndent(level)).append("<many-to-one name=\"");
                        buffer.append(relationship.getName());
                        buffer.append("\" column=\"");
                        buffer.append(columnName);
                        buffer.append("\" index=\"");
                        buffer.append(generateIndexName(prototype.getName(), relationship.getName()));
                        buffer.append("\" class=\"");
                        buffer.append(target_prototype.getInstanceClass().getName());
                        buffer.append("\"");
                        if (relationship.getUnique() && !hasNamedUniqueKey) {
                            buffer.append(" unique=\"true\"");
                        }
                        if (hasNamedUniqueKey) {
                            buffer.append(" unique-key=\"").append(namedUniqueKeys).append("\"");
                        }
                        if (cascadeMode != null) {
                            buffer.append(" cascade=\"").append(cascadeMode).append("\"");
                        }
                        if (fetch != null) {
                            buffer.append(" fetch=\"").append(fetch).append("\"");
                        }
                        buffer.append("/>").append(NEW_LINE);
                    }
                } else {
                    buffer.append(generateIndent(level)).append("<many-to-one name=\"");
                    buffer.append(relationship.getName());
                    buffer.append("\" column=\"");
                    buffer.append(columnName);
                    buffer.append("\" index=\"");
                    buffer.append(generateIndexName(prototype.getName(), relationship.getName()));
                    buffer.append("\" class=\"");
                    buffer.append(target_prototype.getInstanceClass().getName());
                    buffer.append("\"");
                    if (relationship.getUnique() && !hasNamedUniqueKey) {
                        buffer.append(" unique=\"true\"");
                    }
                    if (hasNamedUniqueKey) {
                        buffer.append(" unique-key=\"").append(namedUniqueKeys).append("\"");
                    }
                    if (cascadeMode != null) {
                        buffer.append(" cascade=\"").append(cascadeMode).append("\"");
                    }
                    if (fetch != null) {
                        buffer.append(" fetch=\"").append(fetch).append("\"");
                    }
                    buffer.append("/>").append(NEW_LINE);
                }
            }
        } else if (back_relationship != null) {
            String collectionType = null;
            switch(relationship.getCollectionTypeID()) {
                case OpType.SET:
                    collectionType = "set";
                    break;
                case OpType.BAG:
                    collectionType = "bag";
                    break;
            }
            if (!OpType.isCollectionType(back_relationship.getCollectionTypeID())) {
                if (isInterface) {
                    appendInterface(buffer, prototype, relationship, level);
                } else {
                    buffer.append(generateIndent(level)).append("<" + collectionType + " name=\"");
                    buffer.append(relationship.getName());
                    if (relationship.getInverse()) {
                        buffer.append("\" inverse=\"true");
                    }
                    if (orderBy != null) {
                        buffer.append("\" order-by=\"").append(generateColumnName(prototype, orderBy, null, depth));
                    }
                    if (sort != null) {
                        buffer.append("\" sort=\"").append(sort);
                    }
                    buffer.append("\" lazy=\"").append(lazy);
                    if (cascadeMode != null) {
                        buffer.append("\" cascade=\"").append(cascadeMode);
                    }
                    if (fetch != null) {
                        buffer.append("\" fetch=\"").append(fetch);
                    }
                    buffer.append("\">").append(NEW_LINE);
                    buffer.append(generateIndent(level + 1)).append("<key column=\"");
                    buffer.append(generateColumnName(prototype, back_relationship.getName(), null, depth));
                    buffer.append("\"/>").append(NEW_LINE);
                    buffer.append(generateIndent(level + 1)).append("<one-to-many class=\"");
                    buffer.append(target_prototype.getInstanceClass().getName());
                    buffer.append("\"/>").append(NEW_LINE);
                    buffer.append(generateIndent(level)).append("</" + collectionType + ">").append(NEW_LINE);
                }
            } else {
                buffer.append(generateIndent(level)).append("<" + collectionType + " name=\"");
                buffer.append(relationship.getName());
                String join_table_name;
                String key_column_name;
                String column_name;
                if (relationship.getInverse()) {
                    buffer.append("\" inverse=\"true");
                    join_table_name = generateJoinTableName(target_prototype.getName(), back_relationship.getName());
                } else {
                    join_table_name = generateJoinTableName(prototype.getName(), relationship.getName());
                }
                key_column_name = generateJoinColumnName(prototype.getName(), relationship.getName());
                column_name = generateJoinColumnName(target_prototype.getName(), back_relationship.getName());
                buffer.append("\" table=\"");
                buffer.append(join_table_name);
                if (orderBy != null) {
                    buffer.append("\" order-by=\"").append(generateColumnName(prototype, orderBy, null, depth));
                }
                buffer.append("\" lazy=\"").append(lazy);
                if (cascadeMode != null) {
                    buffer.append("\" cascade=\"").append(cascadeMode);
                }
                if (fetch != null) {
                    buffer.append("\" fetch=\"").append(fetch);
                }
                buffer.append("\">").append(NEW_LINE);
                buffer.append(generateIndent(level + 1)).append("<key column=\"").append(key_column_name);
                buffer.append("\"/>").append(NEW_LINE);
                buffer.append(generateIndent(level + 1)).append("<many-to-many class=\"");
                buffer.append(target_prototype.getInstanceClass().getName()).append("\" column=\"").append(column_name).append("\">").append(NEW_LINE);
                buffer.append(generateIndent(level + 1)).append("</many-to-many>");
                buffer.append(generateIndent(level)).append("</" + collectionType + ">").append(NEW_LINE);
            }
        } else {
            logger.warn("Warning: To-many relationships not supported for null back-relationship: " + prototype.getName() + "." + relationship.getName());
        }
    }

    private String getNamedUniqueKeyString(OpConstrainedMember member, String uniqueKeyName) {
        StringBuffer namedUniqueKeysBuffer = new StringBuffer(uniqueKeyName != null && uniqueKeyName.trim().length() > 0 ? uniqueKeyName : "");
        namedUniqueKeysBuffer.append(member.getUniqueKey() != null && member.getUniqueKey().trim().length() > 0 ? " " + member.getUniqueKey() : "");
        String namedUniqueKeys = namedUniqueKeysBuffer.toString();
        return namedUniqueKeys;
    }

    /**
    * Add mapping for subtype.
    *
    * @param buffer    buffer where to add mapping
    * @param prototype sub prototype
    * @param level     indent level
    */
    private void appendSubTypeMapping(StringBuffer buffer, OpPrototype prototype, int level) {
        if (prototype.isInterface()) {
            return;
        }
        buffer.append(NEW_LINE);
        buffer.append(generateIndent(level)).append("<joined-subclass name=\"");
        buffer.append(prototype.getInstanceClass().getName());
        buffer.append("\" table=\"");
        String table_name = generateTableName(prototype.getName());
        prototype.setTableName(table_name);
        buffer.append(table_name);
        if (USE_DYNAMIC) {
            buffer.append("\" dynamic-insert=\"true");
            buffer.append("\" dynamic-update=\"true");
        }
        if (prototype.getBatchSize() != null) {
            buffer.append("\" batch-size=\"").append(prototype.getBatchSize());
        }
        buffer.append("\">").append(NEW_LINE);
        buffer.append(generateIndent(level + 1)).append("<key column=\"op_id\"/>").append(NEW_LINE);
        addMembers(buffer, prototype, level + 1);
        Iterator subTypes = prototype.subTypes();
        OpPrototype subType;
        while (subTypes.hasNext()) {
            subType = (OpPrototype) subTypes.next();
            appendMapping(buffer, subType, level + 1);
        }
        buffer.append(generateIndent(level)).append("</joined-subclass>").append(NEW_LINE);
    }

    /**
    * Generates column name
    *
    * @param propertyName property for which we need to generate column name
    * @param string
    * @return column name
    */
    private static String generateColumnName(OpPrototype prototype, String propertyName, String givenColumnName, int depth) {
        String columnName = givenColumnName;
        if (columnName == null) {
            columnName = getDefaultColumnName(propertyName, depth);
        }
        columnName = generateUpperCase(columnName);
        if (columnName.length() > IDENTIFIER_NAME_LENGTH) {
            throw new IllegalArgumentException("Idetified Name to long: " + prototype.getName() + "." + propertyName + (givenColumnName != null ? "(" + givenColumnName + ")" : "") + " -> " + columnName + " - length: " + columnName.length() + " allowed: " + IDENTIFIER_NAME_LENGTH);
        }
        return columnName;
    }

    private static String getDefaultColumnName(String propertyName, int depth) {
        StringBuffer buffer = new StringBuffer(COLUMN_NAME_PREFIX);
        buffer.append(propertyName.toUpperCase());
        buffer.append(COLUMN_NAME_POSTFIX);
        if (depth > 0) {
            for (String ukex : UNIQUE_KEY_EXTENSIONS) {
                if (ukex.equals(propertyName)) {
                    buffer.append('_');
                    buffer.append(Integer.toString(depth));
                }
            }
        }
        return buffer.toString();
    }

    /**
    * Generates table name
    *
    * @param prototype_name name of the prototype for which we need to generate table name
    * @return table name
    */
    public static String generateTableName(String prototype_name) {
        StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
        String prototype = removePrototypeNamePrefixes(prototype_name);
        buffer.append(prototype);
        buffer.append(TABLE_NAME_POSTFIX);
        return generateUpperCase(buffer.toString());
    }

    /**
    * Removes prefixes from the name of a prototype, each prefix at most once.
    *
    * @param prototypeName a <code>String</code> representing the name of a prototype.
    * @return a <code>String</code> representing the name of the prototype without the prefixes.
    */
    private static String removePrototypeNamePrefixes(String prototypeName) {
        for (String prefix : PROTOTYPE_PREFIXES) {
            if (prototypeName.startsWith(prefix)) {
                prototypeName = prototypeName.replaceAll(prefix, "");
            }
        }
        return prototypeName;
    }

    /**
    * Generates JOIN table name
    *
    * @param prototype_name1 name of the prototype for which we need to generate table name
    * @param prototype_name2 name of the prototype for which we need to generate table name
    * @return table name
    */
    private static String generateJoinTableName(String prototype_name1, String prototype_name2) {
        StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
        buffer.append(prototype_name1);
        buffer.append(JOIN_NAME_SEPARATOR);
        buffer.append(prototype_name2);
        buffer.append(TABLE_NAME_POSTFIX);
        return generateUpperCase(buffer.toString());
    }

    /**
    * GEnerates column name for a join.
    *
    * @param prototype_name prototype name
    * @param property_name  join property name
    * @return join column name
    */
    private static String generateJoinColumnName(String prototype_name, String property_name) {
        StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
        buffer.append(prototype_name);
        buffer.append(JOIN_NAME_SEPARATOR);
        buffer.append(property_name);
        buffer.append(TABLE_NAME_POSTFIX);
        return generateUpperCase(buffer.toString());
    }

    /**
    * Generates index name
    *
    * @param prototype_name prototype for which we need to generate index.
    * @param property_name  property for which index must be generated
    * @return index name.
    */
    private String generateIndexName(String prototype_name, String property_name) {
        StringBuffer buffer = new StringBuffer(INDEX_NAME_PREFIX);
        buffer.append(generateTableName(prototype_name));
        buffer.append(JOIN_NAME_SEPARATOR);
        buffer.append(property_name);
        buffer.append(INDEX_NAME_POSTFIX);
        int maxIndexNameLength = INDEX_NAME_LENGTH - 15;
        maxIndexNameLength = databaseType == OpHibernateSource.IBM_DB2 ? IBM_DB_2_INDEX_NAME_LENGTH : maxIndexNameLength;
        maxIndexNameLength = databaseType == OpHibernateSource.ORACLE ? ORACLE_INDEX_NAME_LENGTH : maxIndexNameLength;
        String indexName = buffer.substring(getTableNamePrefix().length());
        if (indexName.length() > maxIndexNameLength) {
            String prototypeNameTruncated = createIndexSequenceKeyFromPrototypeName(prototype_name, INDEX_NAME_PREFIX, JOIN_NAME_SEPARATOR, maxIndexNameLength);
            Integer latestIdx = identifierSequenceMap.get(prototypeNameTruncated);
            latestIdx = Integer.valueOf(latestIdx != null ? latestIdx.intValue() + 1 : 0);
            identifierSequenceMap.put(prototypeNameTruncated, latestIdx);
            String indexNameTruncated = indexName.substring(0, maxIndexNameLength - POSTFIX_LENGTH);
            indexName = indexNameTruncated + identifierEnumFormat.format(latestIdx.longValue());
        }
        return generateUpperCase(indexName);
    }

    private String createIndexSequenceKeyFromPrototypeName(String prototype_name, String prefix, String separator, int maxLength) {
        int maxKeyLength = maxLength - POSTFIX_LENGTH - prefix.length() - separator.length();
        String prototypeNameTruncated = prototype_name.length() > maxKeyLength ? prototype_name.substring(0, maxKeyLength) : prototype_name;
        return prototypeNameTruncated;
    }

    /**
    * Returns the name of the hibernate type associated with the given id of an <code>OpType</code>.
    *
    * @param xTypeId a <code>int</code> representing the id of an OpType.
    * @return a <code>String</code> representing the name of the equivalent hibernate type, or the name of a custom type.
    */
    public static String getHibernateTypeName(int xTypeId) {
        return HIBERNATE_TYPE_NAME_MAP.get(Integer.valueOf(xTypeId));
    }

    public static NullableType getHibernateType(int xTypeId) {
        return HIBERNATE_TYPE_MAP.get(Integer.valueOf(xTypeId));
    }

    public static UserType getUserType(int xTypeId) {
        return HIBERNATE_USER_TYPE_MAP.get(Integer.valueOf(xTypeId));
    }

    /**
    * Add provided content into the buffer with appropriate indent and NEW line char at the end.
    *
    * @param buffer  buffer where to write content/
    * @param content content to write
    * @param level   indent level
    */
    private void appendLine(StringBuffer buffer, String content, int level) {
        buffer.append(generateIndent(level)).append(content).append(NEW_LINE);
    }

    /**
    * Generates indent string.
    *
    * @param level level for which we generate indent.
    * @return indent string
    */
    private static String generateIndent(int level) {
        char[] array = new char[level * 3];
        Arrays.fill(array, SPACE_CHAR);
        return new String(array);
    }

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length < 1) {
            System.out.println("usage java -cp lib OpHibernateSource dbtype <outfile>");
            System.out.println("  dbtype is one of derby, mssql, mysql_innodb, mysql, postgres, oracle, hsql or db2");
            System.out.println("  outfile is the filename to write the mapping to, stdout is used if this argument is not given");
            System.exit(-1);
        }
        int dbType = -1;
        if (args[0].equalsIgnoreCase("derby")) {
            dbType = OpHibernateSource.DERBY;
        } else if (args[0].equalsIgnoreCase("mysql_innodb")) {
            dbType = OpHibernateSource.MYSQL_INNODB;
        } else if (args[0].equalsIgnoreCase("mysql")) {
            dbType = OpHibernateSource.IBM_DB2;
        } else if (args[0].equalsIgnoreCase("postgres")) {
            dbType = OpHibernateSource.POSTGRESQL;
        } else if (args[0].equalsIgnoreCase("oracle")) {
            dbType = OpHibernateSource.ORACLE;
        } else if (args[0].equalsIgnoreCase("hsql")) {
            dbType = OpHibernateSource.HSQLDB;
        } else if (args[0].equalsIgnoreCase("db2")) {
            dbType = OpHibernateSource.IBM_DB2;
        } else if (args[0].equalsIgnoreCase("mssql")) {
            dbType = OpHibernateSource.MSSQL;
        } else {
            System.err.println("ERROR unknown dbtype '" + args[0] + "'");
            System.exit(-1);
        }
        PrintStream out = System.out;
        if (args.length > 1) {
            try {
                File file = new File(args[1]);
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                out = new PrintStream(file);
            } catch (IOException exc) {
                System.err.println("ERROR could not create file '" + args[1] + "', error: " + exc.getLocalizedMessage());
                System.exit(-1);
            }
        }
        OpEnvironmentManager.getInstance().setOnepointHome(new File(System.getProperty("user.dir")));
        XResourceBroker.setResourcePath("onepoint/project");
        OpInitializerFactory factory = OpInitializerFactory.getInstance();
        OpInitializer initializer = factory.getInitializer();
        if (initializer == null) {
            initializer = factory.setInitializer(OpInitializer.class);
            initializer.init(OpProjectConstants.OPEN_EDITION_CODE);
        }
        register(new OpMappingsGenerator(dbType));
        generator.init(OpTypeManager.getPrototypes());
        String mapping = generator.generateMappings();
        out.print(mapping);
    }

    private int getDatabaseType() {
        return databaseType;
    }

    public static String generateUpperCase(String s) {
        boolean toUpper = false;
        if (generator != null) {
            toUpper = generator.getDatabaseType() == OpHibernateSource.ORACLE || generator.getDatabaseType() == OpHibernateSource.DERBY;
        }
        return toUpper ? s.toUpperCase() : s.toLowerCase();
    }

    public static OpMappingsGenerator getInstance(int databaseType) {
        return generator;
    }

    public static void register(OpMappingsGenerator generator) {
        OpMappingsGenerator.generator = generator;
    }

    public static String getTableNamePrefix() {
        return generateUpperCase(TABLE_NAME_PREFIX);
    }

    public static String getColumnNamePrefix() {
        return generateUpperCase(COLUMN_NAME_PREFIX);
    }

    public static Type getHibernateType(OpMember userType) {
        return getTypeWrapper(userType);
    }

    public static Type getTypeWrapper(OpMember composite) {
        if (composite instanceof OpUserType) {
            String userTypeParameter = composite.getTypeName();
            Properties parameters = new Properties();
            parameters.put(OpEnhancedTypeBase.USER_TYPE_NAME, userTypeParameter);
            return new CompositeCustomType(OpHibernateCompositeType.class, parameters);
        }
        throw new OpPersistenceException(composite.getName() + " is not of Type " + OpUserType.class.getName(), OpPersistenceException.PROTOTYPE_ERROR);
    }
}
