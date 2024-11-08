package net.sf.mustang.orm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.mustang.InitializingException;
import net.sf.mustang.Restartable;
import net.sf.mustang.cache.Cache;
import net.sf.mustang.cache.CacheManager;
import net.sf.mustang.jdbc.JdbcBlobber;
import net.sf.mustang.jdbc.JdbcManager;
import net.sf.mustang.jdbc.JdbcOutputStreamRowMapper;
import net.sf.mustang.template.TemplateEngine;
import net.sf.mustang.util.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.BindException;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@SuppressWarnings("unchecked")
public class BeanManager implements InitializingBean, Restartable {

    public static final String ID_FILTER = "id";

    public static final int MAX_BLOB_CACHE_LENGHT = 1024 * 1000;

    private JdbcManager jdbcManager;

    private CacheManager cacheManager;

    private Cache blobCache;

    private Map<Class, BeanInfo> beanInfoMap;

    private TemplateEngine templateEngine;

    public BeanManager() throws Exception {
        start();
    }

    public boolean evaluateRule(String rule, Object bean) throws Exception {
        BeanInfo beanInfo = getBeanInfo(bean.getClass());
        TemplateEngine templateEngine = jdbcManager.getTemplateEngine();
        Map<String, Object> context = new HashMap<String, Object>();
        InputStream in = bean.getClass().getResourceAsStream(beanInfo.getBeanName() + "_" + rule + ".validation");
        context.put("this", bean);
        String rules = IOUtils.toString(in);
        return templateEngine.evaluateRuleScript(rules, context);
    }

    public void copyFrom(Bean bean, InputStream inputStream) throws Exception {
        BeanInfo beanInfo = getBeanInfo(bean.getClass());
        validate(bean, beanInfo, "copyFrom");
        JdbcBlobber blobber = beanInfo.getBlobInfo(jdbcManager.getDb()).getBlobber();
        blobber.update(bean, inputStream, beanInfo, jdbcManager);
    }

    public void copyTo(Bean bean, OutputStream out, int offset, int length) throws Exception {
        BeanInfo beanInfo = getBeanInfo(bean.getClass());
        validate(bean, beanInfo, "copyTo");
        if (blobCache != null && length < MAX_BLOB_CACHE_LENGHT) {
            byte[] bytes = null;
            synchronized (this) {
                String key = makeUniqueKey(bean, beanInfo, offset, length);
                if (blobCache.contains(key)) bytes = (byte[]) blobCache.get(key); else blobCache.put(key, bytes = toByteArray(bean, offset, length, beanInfo));
            }
            InputStream in = new ByteArrayInputStream(bytes);
            IOUtils.copy(in, out);
            in.close();
        } else {
            jdbcManager.queryScript(beanInfo.getBlobInfo(jdbcManager.getDb()).getReadScript(), bean, new JdbcOutputStreamRowMapper(out, offset, length));
        }
    }

    private byte[] toByteArray(Bean bean, int offset, int length, BeanInfo beanInfo) throws IOException {
        byte[] bytes;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        jdbcManager.queryScript(beanInfo.getBlobInfo(jdbcManager.getDb()).getReadScript(), bean, new JdbcOutputStreamRowMapper(buffer, offset, length));
        bytes = buffer.toByteArray();
        buffer.close();
        return bytes;
    }

    private String makeUniqueKey(Bean bean, BeanInfo beanInfo, int offset, int length) {
        return bean.getId() + beanInfo.getBeanName() + offset + "-" + length;
    }

    public Serializable read(BeanId id, Class beanClass) throws Exception {
        return read(id, ID_FILTER, beanClass);
    }

    public Serializable read(Number id, Class beanClass) throws Exception {
        return read(new BeanId(id), ID_FILTER, beanClass);
    }

    public Serializable read(Object filter, String filterName, Class beanClass) throws Exception {
        BeanInfo beanInfo = getBeanInfo(beanClass);
        validate(filter, beanInfo, "read-" + filterName);
        Bean bean = null;
        Cache cache = null;
        Serializable key = null;
        if (beanInfo.isCacheEnabled() && filterName.equals(ID_FILTER)) {
            cache = cacheManager.getCache(beanInfo.getCacheName());
            key = makeKey(beanClass, ((BeanId) filter).getId());
            bean = (Bean) cache.get(key);
        }
        if (bean == null) {
            bean = readFromRepository(filter, filterName, beanClass);
            if (bean != null && cache != null) {
                cache.put(key, bean);
            }
        }
        if (bean != null && bean instanceof Bean) {
            ((Bean) bean).setBeanManager(this);
            return bean;
        }
        return null;
    }

    private String makeKey(Class beanClass, Number id) {
        return beanClass.getSimpleName() + id;
    }

    @SuppressWarnings("unchecked")
    private Bean readFromRepository(Object filter, String filterName, Class beanClass) throws Exception {
        Bean bean = null;
        BeanInfo beanInfo = getBeanInfo(beanClass);
        List<Bean> resultList = jdbcManager.queryScript(beanInfo.getReadScript(filterName), filter, beanClass);
        bean = resultList.size() == 1 ? resultList.get(0) : null;
        return bean;
    }

    public Number create(Bean bean) throws Exception {
        Class beanClass = bean.getClass();
        BeanInfo beanInfo = getBeanInfo(beanClass);
        validate(bean, beanInfo, "create");
        Number id = (Number) jdbcManager.execute(new BeanCreator(bean, beanInfo, jdbcManager));
        if (id != null) {
            if (beanInfo.isCacheEnabled()) cacheManager.getCache(beanInfo.getCacheName()).remove(makeKey(beanClass, id));
            return id.longValue();
        } else return null;
    }

    public int update(Bean bean) throws Exception {
        Class beanClass = bean.getClass();
        BeanInfo beanInfo = getBeanInfo(beanClass);
        validate(bean, beanInfo, "update");
        int i = jdbcManager.updateScript(beanInfo.getUpdateScript(), bean);
        if (beanInfo.isCacheEnabled()) cacheManager.getCache(beanInfo.getCacheName()).remove(makeKey(beanClass, bean.getId()));
        return i;
    }

    public int delete(Bean bean) throws Exception {
        Class beanClass = bean.getClass();
        BeanInfo beanInfo = getBeanInfo(beanClass);
        validate(bean, beanInfo, "delete");
        int i = jdbcManager.updateScript(beanInfo.getDeleteScript(), bean);
        if (beanInfo.isCacheEnabled()) cacheManager.getCache(beanInfo.getCacheName()).remove(makeKey(beanClass, bean.getId()));
        return i;
    }

    public BeanInfo getBeanInfo(Class beanClass) throws Exception {
        BeanInfo beanInfo = beanInfoMap.get(beanClass);
        if (beanInfo == null) {
            beanInfo = BeanUtils.beanTobeanInfo(beanClass);
            beanInfoMap.put(beanClass, beanInfo);
        }
        return beanInfo;
    }

    public void afterPropertiesSet() throws Exception {
        if (jdbcManager == null) throw new InitializingException(this, JdbcManager.class);
        if (cacheManager == null) throw new InitializingException(this, CacheManager.class);
        if (templateEngine == null) throw new InitializingException(this, TemplateEngine.class);
    }

    public void setJdbcManager(JdbcManager jdbcManager) {
        this.jdbcManager = jdbcManager;
    }

    public void restart() throws Exception {
        start();
    }

    public void start() throws Exception {
        beanInfoMap = new HashMap<Class, BeanInfo>();
    }

    public void stop() throws Exception {
    }

    public void setCacheManager(CacheManager cacheManager) throws Exception {
        this.cacheManager = cacheManager;
        if (cacheManager.containsCache("blob")) blobCache = cacheManager.getCache("blob");
    }

    public List<Number> readList(Bean bean, String fieldName) throws Exception {
        return readList(bean.getClass(), bean, fieldName);
    }

    @SuppressWarnings("unchecked")
    public int execute(Bean bean, String scriptName) throws Exception {
        return execute(bean.getClass(), bean, scriptName);
    }

    @SuppressWarnings("unchecked")
    public int execute(Class beanClass, String scriptName) throws Exception {
        return execute(beanClass, null, scriptName);
    }

    @SuppressWarnings("unchecked")
    private int execute(Class beanClass, Bean bean, String scriptName) throws Exception {
        BeanInfo beanInfo = getBeanInfo(beanClass);
        if (bean != null) validate(bean, beanInfo, "execute-" + scriptName);
        int i = jdbcManager.updateScript(beanInfo.getExecuteScript(scriptName), bean);
        if (beanInfo.isCacheEnabled()) cacheManager.getCache(beanInfo.getCacheName()).remove(makeKey(beanClass, bean.getId()));
        return i;
    }

    @SuppressWarnings("unchecked")
    public List<Number> readList(Class beanClass, String fieldName) throws Exception {
        BeanInfo beanInfo = getBeanInfo(beanClass);
        return toIdList(jdbcManager.queryScript(beanInfo.getReadListScript(fieldName), null, BeanId.class));
    }

    @SuppressWarnings("unchecked")
    public List exportList(Class beanClass, String fieldName) throws Exception {
        BeanInfo beanInfo = getBeanInfo(beanClass);
        List<Bean> beans = jdbcManager.queryScript(beanInfo.getReadListScript(fieldName), null, beanClass);
        for (Bean bean : beans) bean.setBeanManager(this);
        return beans;
    }

    private List<Number> toIdList(List<BeanId> list) {
        List<Number> idList = new ArrayList<Number>(list.size());
        for (BeanId beanId : list) {
            idList.add(beanId.getId());
        }
        return idList;
    }

    @SuppressWarnings("unchecked")
    public List<Number> readList(Class beanClass, Bean bean, String fieldName) throws Exception {
        BeanInfo beanInfo = getBeanInfo(beanClass);
        validate(bean, beanInfo, "readlist-" + fieldName);
        return toIdList(jdbcManager.queryScript(beanInfo.getReadListScript(fieldName), bean, BeanId.class));
    }

    @SuppressWarnings("unchecked")
    public List exportList(Class beanClass, Bean bean, String fieldName) throws Exception {
        BeanInfo beanInfo = getBeanInfo(beanClass);
        validate(bean, beanInfo, "exportlist-" + fieldName);
        return jdbcManager.queryScript(beanInfo.getReadListScript(fieldName), bean, beanClass);
    }

    public Validator getValidator(BeanInfo beanInfo, String validationGroup) throws Exception {
        return new BeanValidator(beanInfo, templateEngine, validationGroup, this);
    }

    public Validator getValidator(Bean bean, String validationGroup) throws Exception {
        Validator validator = getValidator(getBeanInfo(bean.getClass()), validationGroup);
        if (validator == null) throw new Exception("validator '" + validationGroup + "'not found"); else return validator;
    }

    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    private void validate(Object bean, BeanInfo beanInfo, String validationGroup) throws Exception, BindException {
        if (beanInfo.getValidationInfo() != null) {
            Validator validator = getValidator(beanInfo, validationGroup);
            BindException errors = new BindException(bean, beanInfo.getBeanName());
            ValidationUtils.invokeValidator(validator, bean, errors);
            if (errors.hasErrors()) throw errors;
        }
    }

    public JdbcManager getJdbcManager() {
        return jdbcManager;
    }
}
