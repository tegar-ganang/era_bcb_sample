package org.damour.base.server.hibernate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.damour.base.client.objects.IHibernateFriendly;
import org.damour.base.client.utils.StringUtils;
import org.damour.base.server.BaseSystem;
import org.damour.base.server.Logger;
import org.damour.base.server.hibernate.helpers.DefaultData;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DOMWriter;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static HibernateUtil instance = null;

    private SessionFactory sessionFactory = null;

    private String username;

    private String password;

    private String connectString;

    private HashMap<Class<?>, Element> classElementMap = new HashMap<Class<?>, Element>();

    private HashMap<Class<?>, Element> idElementMap = new HashMap<Class<?>, Element>();

    private HashMap<Class<?>, Boolean> idElementClearedMap = new HashMap<Class<?>, Boolean>();

    private List<Class<?>> mappedClasses = new ArrayList<Class<?>>();

    private Document mappingDocument = DocumentHelper.createDocument();

    private Element mappingRoot = null;

    private String tablePrefix = "";

    private boolean showSQL = true;

    private String hbm2ddlMode = "update";

    private HibernateUtil(HashMap<String, String> overrides) {
        Logger.log("creating new HibernateUtil()");
        Properties rb = BaseSystem.getSettings();
        rb.putAll(overrides);
        setUsername(rb.getProperty("username"));
        setPassword(rb.getProperty("password"));
        setConnectString(rb.getProperty("connectString"));
        setTablePrefix(rb.getProperty("tablePrefix"));
        setHbm2ddlMode(getResource(rb, "hbm2ddlMode", "" + hbm2ddlMode));
        setShowSQL("true".equalsIgnoreCase(getResource(rb, "showSQL", "" + showSQL)));
        generateHibernateMappings(rb);
    }

    private void bootstrap() {
        try {
            org.hibernate.Session session = HibernateUtil.getInstance().getSession();
            Transaction tx = session.beginTransaction();
            try {
                IDefaultData defaultData = null;
                if (BaseSystem.getSettings().get("DefaultDataOverride") != null) {
                    defaultData = (IDefaultData) Class.forName(BaseSystem.getSettings().getProperty("DefaultDataOverride")).newInstance();
                }
                if (defaultData == null) {
                    defaultData = new DefaultData();
                }
                defaultData.create(session);
                tx.commit();
            } catch (HibernateException he) {
                tx.rollback();
                session.close();
            } finally {
                try {
                    session.close();
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
            BaseSystem.setDomainName("sometests.com");
            Logger.log(t);
        }
    }

    private void generateHibernateMappings(Properties bundle) {
        try {
            Enumeration<?> keys = bundle.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if (key.startsWith("HibernateMapped")) {
                    try {
                        Class<?> clazz = Class.forName(bundle.getProperty(key));
                        generateHibernateMapping(clazz);
                    } catch (Throwable t) {
                        Logger.log(t);
                    }
                }
            }
        } catch (Throwable t) {
            Logger.log(t);
        }
    }

    private String getResource(Properties bundle, String key, String defaultValue) {
        try {
            String property = bundle.getProperty(key);
            if (!StringUtils.isEmpty(property)) {
                return property;
            }
        } catch (Throwable t) {
        }
        return defaultValue;
    }

    public static synchronized HibernateUtil getInstance(HashMap<String, String> overrides) {
        if (instance == null) {
            instance = new HibernateUtil(overrides);
            instance.bootstrap();
            if (Logger.DEBUG) {
                Runnable r = new Runnable() {

                    public void run() {
                        while (true) {
                            System.gc();
                            long total = Runtime.getRuntime().totalMemory();
                            long free = Runtime.getRuntime().freeMemory();
                            Logger.log(DecimalFormat.getNumberInstance().format(total) + " allocated " + DecimalFormat.getNumberInstance().format(total - free) + " used " + DecimalFormat.getNumberInstance().format(free) + " free");
                            try {
                                Thread.sleep(30000);
                            } catch (Exception e) {
                            }
                        }
                    }
                };
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.start();
            }
        }
        return instance;
    }

    public static synchronized HibernateUtil getInstance() {
        return getInstance(new HashMap<String, String>());
    }

    public static void resetHibernate() {
        try {
            instance.sessionFactory.getCurrentSession().close();
        } catch (Throwable t) {
        }
        try {
            instance.sessionFactory.close();
            instance.sessionFactory = null;
        } catch (Throwable t) {
        }
        instance = null;
    }

    public void setSessionFactory(SessionFactory inSessionFactory) {
        sessionFactory = inSessionFactory;
    }

    public SessionFactory getSessionFactory(Document configurationDocument) {
        Configuration cfg;
        try {
            cfg = new Configuration().configure(new DOMWriter().write(configurationDocument));
            sessionFactory = cfg.buildSessionFactory();
        } catch (HibernateException e) {
            Logger.log(e);
        } catch (DocumentException e) {
            Logger.log(e);
        }
        return sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Document document = DocumentHelper.createDocument();
                Element root = document.addElement("hibernate-configuration");
                Element sessionFactoryElement = root.addElement("session-factory");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.connection.driver_class").setText("com.mysql.jdbc.Driver");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.connection.username").setText(getUsername());
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.connection.password").setText(getPassword());
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.connection.url").setText(getConnectString());
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.jdbc.use_get_generated_keys").setText("false");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.jdbc.batch_size").setText("25");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.dialect").setText("org.hibernate.dialect.MySQLInnoDBDialect");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.bytecode.use_reflection_optimizer").setText("false");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.generate_statistics").setText("true");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.cache.use_structured_entries").setText("true");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.cache.use_query_cache").setText("true");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.show_sql").setText("" + isShowSQL());
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.jdbc.use_streams_for_binary").setText("true");
                sessionFactoryElement.addElement("property").addAttribute("name", "cache.provider_class").setText(CacheProvider.class.getName());
                sessionFactoryElement.addElement("property").addAttribute("name", "c3p0.acquire_increment").setText("1");
                sessionFactoryElement.addElement("property").addAttribute("name", "c3p0.idle_test_period").setText("10");
                sessionFactoryElement.addElement("property").addAttribute("name", "c3p0.timeout").setText("5000");
                sessionFactoryElement.addElement("property").addAttribute("name", "c3p0.min_size").setText("1");
                sessionFactoryElement.addElement("property").addAttribute("name", "c3p0.max_size").setText("5");
                sessionFactoryElement.addElement("property").addAttribute("name", "c3p0.max_statements").setText("0");
                sessionFactoryElement.addElement("property").addAttribute("name", "c3p0.preferredTestQuery").setText("select 1+1");
                sessionFactoryElement.addElement("property").addAttribute("name", "hibernate.hbm2ddl.auto").setText(hbm2ddlMode);
                Configuration cfg = new Configuration().configure(new DOMWriter().write(document));
                Logger.log(mappingDocument.asXML());
                cfg.addDocument(new DOMWriter().write(mappingDocument));
                sessionFactory = cfg.buildSessionFactory();
            } catch (Exception e) {
                Logger.log(e);
            }
        }
        return sessionFactory;
    }

    private void addMappedClass(Class<?> clazz) {
        mappedClasses.add(clazz);
    }

    private boolean isClassMapped(Class<?> clazz) {
        return mappedClasses.contains(clazz);
    }

    private Element getMappingElement() {
        if (mappingRoot == null) {
            mappingRoot = mappingDocument.addElement("hibernate-mapping");
        }
        return mappingRoot;
    }

    public synchronized Session getSession() {
        return getSessionFactory().openSession();
    }

    public List<?> executeQuery(Session session, String query, boolean cacheResults, int maxResults) {
        Logger.log(query);
        Query q = session.createQuery(query).setCacheable(cacheResults).setMaxResults(maxResults);
        return q.list();
    }

    @SuppressWarnings("rawtypes")
    public List executeQuery(Session session, String query, boolean cacheResults) {
        Logger.log(query);
        Query q = session.createQuery(query).setCacheable(cacheResults);
        return q.list();
    }

    @SuppressWarnings({ "rawtypes" })
    public List executeQuery(Session session, String query) {
        return executeQuery(session, query, true);
    }

    private org.safehaus.uuid.UUIDGenerator guidGenerator = org.safehaus.uuid.UUIDGenerator.getInstance();

    public String generateGUID() {
        return guidGenerator.generateTimeBasedUUID().toString();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        sessionFactory = null;
        this.password = password;
    }

    public String getConnectString() {
        return connectString;
    }

    public void setConnectString(String connectString) {
        sessionFactory = null;
        this.connectString = connectString;
    }

    public void generateHibernateMapping(Class<?> clazz) {
        if (!isClassMapped(clazz)) {
            Logger.log("Adding mapping for " + clazz.getSimpleName().toLowerCase());
            Element mappingRootElement = getMappingElement();
            Element mappingElement = null;
            if (!clazz.getSuperclass().equals(Object.class)) {
                generateHibernateMapping(clazz.getSuperclass());
                Element parentMappingElement = classElementMap.get(clazz.getSuperclass());
                mappingElement = parentMappingElement.addElement("joined-subclass");
                mappingElement.addAttribute("name", clazz.getName());
                mappingElement.addAttribute("table", getTablePrefix() + clazz.getSimpleName().toLowerCase());
                Element keyElement = mappingElement.addElement("key");
                keyElement.addAttribute("column", "id");
                idElementMap.put(clazz, keyElement);
            } else {
                mappingElement = mappingRootElement.addElement("class");
                mappingElement.addAttribute("name", clazz.getName());
                mappingElement.addAttribute("table", getTablePrefix() + clazz.getSimpleName().toLowerCase());
                Element keyElement = mappingElement.addElement("id");
                keyElement.addAttribute("name", "id");
                keyElement.addAttribute("type", "long");
                keyElement.addAttribute("column", "id");
                Element generatorElement = keyElement.addElement("generator");
                generatorElement.addAttribute("class", "native");
                idElementMap.put(clazz, keyElement);
            }
            classElementMap.put(clazz, mappingElement);
            String sqlUpdate = null;
            String cachePolicy = "none";
            boolean lazy = true;
            if (IHibernateFriendly.class.isAssignableFrom(clazz)) {
                try {
                    Method getSqlUpdate = clazz.getMethod("getSqlUpdate");
                    sqlUpdate = (String) getSqlUpdate.invoke(clazz.newInstance());
                    Method getCachePolicy = clazz.getMethod("getCachePolicy");
                    Method isLazy = clazz.getMethod("isLazy");
                    cachePolicy = (String) getCachePolicy.invoke(clazz.newInstance());
                    lazy = (Boolean) isLazy.invoke(clazz.newInstance());
                } catch (Exception e) {
                }
            }
            if (sqlUpdate != null && !"".equals(sqlUpdate)) {
                mappingElement.addElement("sql-update").setText(sqlUpdate);
            }
            if (cachePolicy != null && !"".equals(cachePolicy) && !"none".equals(cachePolicy)) {
                Element cacheElement = mappingElement.addElement("cache");
                cacheElement.addAttribute("usage", cachePolicy);
            }
            mappingElement.addAttribute("lazy", "" + lazy);
            List<Field> fields = ReflectionCache.getFields(clazz);
            for (Field field : fields) {
                String name = field.getName();
                try {
                    Method isFieldMappedMethod = clazz.getMethod("isFieldMapped", String.class);
                    boolean isFieldMapped = (Boolean) isFieldMappedMethod.invoke(clazz.newInstance(), name);
                    if (!isFieldMapped) {
                        Logger.log("  -" + name + ":" + field.getType().getName());
                        continue;
                    }
                } catch (Throwable t) {
                    Logger.log("Cannot determine if field is hibernated managed:" + field.getName() + " (" + field.getType().getName() + ")");
                }
                boolean skip = false;
                List<Field> parentFields = ReflectionCache.getFields(clazz.getSuperclass());
                for (Field parentField : parentFields) {
                    if (field.equals(parentField)) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }
                Logger.log("  +" + name + ":" + field.getType().getName());
                Boolean isKey = Boolean.FALSE;
                Boolean isUnique = Boolean.FALSE;
                String typeOverride = null;
                int fieldLength = -1;
                if (IHibernateFriendly.class.isAssignableFrom(clazz)) {
                    try {
                        Method isKeyMethod = clazz.getMethod("isFieldKey", String.class);
                        Method isUniqueMethod = clazz.getMethod("isFieldUnique", String.class);
                        Method getFieldTypeMethod = clazz.getMethod("getFieldType", String.class);
                        Method getFieldLengthMethod = clazz.getMethod("getFieldLength", String.class);
                        isKey = (Boolean) isKeyMethod.invoke(clazz.newInstance(), name);
                        isUnique = (Boolean) isUniqueMethod.invoke(clazz.newInstance(), name);
                        typeOverride = (String) getFieldTypeMethod.invoke(clazz.newInstance(), name);
                        fieldLength = (Integer) getFieldLengthMethod.invoke(clazz.newInstance(), name);
                    } catch (Exception e) {
                    }
                }
                if (isKey) {
                    Element keyElement = idElementMap.get(clazz);
                    if (idElementClearedMap.get(clazz) == null || !idElementClearedMap.get(clazz)) {
                        keyElement.detach();
                        keyElement = mappingElement.addElement("composite-id");
                        idElementMap.put(clazz, keyElement);
                        idElementClearedMap.put(clazz, Boolean.TRUE);
                    }
                    if (!isJavaType(field.getType())) {
                        Element keyEntry = keyElement.addElement("key-many-to-one");
                        keyEntry.addAttribute("name", field.getName());
                        keyEntry.addAttribute("class", field.getType().getName());
                        keyEntry.addAttribute("column", field.getName());
                    } else {
                        Element keyEntry = keyElement.addElement("key-property");
                        keyEntry.addAttribute("name", field.getName());
                        keyEntry.addAttribute("column", field.getName());
                    }
                    continue;
                }
                if (!name.equals("id")) {
                    String type = field.getType().getSimpleName().toLowerCase();
                    if (isJavaType(field.getType())) {
                        Element propertyElement = mappingElement.addElement("property");
                        propertyElement.addAttribute("name", name);
                        if (typeOverride != null) {
                            propertyElement.addAttribute("type", typeOverride);
                        } else {
                            propertyElement.addAttribute("type", type);
                        }
                        if (fieldLength > 0) {
                            propertyElement.addAttribute("length", "" + fieldLength);
                        }
                        propertyElement.addAttribute("column", name);
                        if (isUnique) {
                            propertyElement.addAttribute("unique", "true");
                        }
                    } else if (field.getType().isAssignableFrom(Set.class)) {
                        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                        Element setElement = mappingElement.addElement("set");
                        setElement.addAttribute("name", name);
                        setElement.addElement("cache").addAttribute("usage", "nonstrict-read-write");
                        setElement.addAttribute("inverse", "true");
                        setElement.addAttribute("lazy", "false");
                        setElement.addElement("key").addAttribute("column", "id");
                        setElement.addElement("one-to-many").addAttribute("class", ((Class<?>) genericType.getActualTypeArguments()[0]).getName());
                    } else if (byte[].class.equals(field.getType())) {
                        Element propertyElement = mappingElement.addElement("property");
                        propertyElement.addAttribute("name", name);
                        propertyElement.addAttribute("type", "binary");
                        propertyElement.addElement("column").addAttribute("name", name).addAttribute("sql-type", "LONGBLOB");
                        if (isUnique) {
                            propertyElement.addAttribute("unique", "true");
                        }
                    } else {
                        Element manyToOneElement = mappingElement.addElement("many-to-one");
                        manyToOneElement.addAttribute("name", name);
                        manyToOneElement.addAttribute("class", field.getType().getName());
                        manyToOneElement.addAttribute("column", name);
                        manyToOneElement.addAttribute("lazy", "false");
                        if (isUnique) {
                            manyToOneElement.addAttribute("unique", "true");
                        }
                    }
                }
            }
            addMappedClass(clazz);
            Logger.log("Finished mapping for " + clazz.getSimpleName().toLowerCase());
        }
    }

    private boolean isJavaType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return true;
        } else if (Boolean.class.isAssignableFrom(clazz)) {
            return true;
        } else if (Number.class.isAssignableFrom(clazz)) {
            return true;
        } else if (String.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    private void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public String getHbm2ddlMode() {
        return hbm2ddlMode;
    }

    private void setHbm2ddlMode(String hbm2ddlMode) {
        this.hbm2ddlMode = hbm2ddlMode;
    }

    public void printStatistics() {
        Session session = HibernateUtil.getInstance().getSession();
        System.out.println("Query Cache: p" + session.getSessionFactory().getStatistics().getQueryCachePutCount() + " h" + session.getSessionFactory().getStatistics().getQueryCacheHitCount() + " m" + session.getSessionFactory().getStatistics().getQueryCacheMissCount());
        System.out.println("2nd Level Cache: p" + session.getSessionFactory().getStatistics().getSecondLevelCachePutCount() + " h" + session.getSessionFactory().getStatistics().getSecondLevelCacheHitCount() + " m" + session.getSessionFactory().getStatistics().getSecondLevelCacheMissCount());
    }

    public boolean isShowSQL() {
        return showSQL;
    }

    private void setShowSQL(boolean showSQL) {
        this.showSQL = showSQL;
    }
}
