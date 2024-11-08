package restdom.dao.impl;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.util.CollectionUtils;
import restdom.dao.GenericDao;
import restdom.dao.annotation.CascadeWrite;
import restdom.dao.annotation.CollectionMode;
import restdom.finder.FinderSupport;

public class GenericDaoIbatisImpl<T, ID extends Serializable> extends SqlMapClientDaoSupport implements GenericDao<T, ID>, FinderSupport<T> {

    protected final Log log = LogFactory.getLog(getClass());

    private Class<T> persistentClass;

    private Method versionGetter;

    private Method versionSetter;

    private boolean useLocking = false;

    private Map<Method, CascadeWrite> cascades = new HashMap<Method, CascadeWrite>();

    public GenericDaoIbatisImpl(Class<T> clazz) {
        this.persistentClass = clazz;
        initCascadeProcessing();
    }

    public GenericDaoIbatisImpl(Class<T> clazz, String versionProperty) {
        this.persistentClass = clazz;
        initLocking(versionProperty);
        initCascadeProcessing();
    }

    protected void initLocking(String versionProperty) {
        try {
            versionSetter = persistentClass.getMethod(getPropertySetter(versionProperty), Integer.class);
            versionGetter = persistentClass.getMethod(getPropertyGetter(versionProperty));
            useLocking = true;
        } catch (SecurityException e) {
            throw new InvalidDataAccessApiUsageException("Could not find version property accessor/mutator", e);
        } catch (NoSuchMethodException e) {
            throw new InvalidDataAccessApiUsageException("Could not find version property accessor/mutator", e);
        }
    }

    protected String getPropertySetter(String versionProperty) {
        return "set" + Character.toUpperCase(versionProperty.charAt(0)) + versionProperty.substring(1);
    }

    protected String getPropertyGetter(String versionProperty) {
        return "get" + Character.toUpperCase(versionProperty.charAt(0)) + versionProperty.substring(1);
    }

    private void initCascadeProcessing() {
        try {
            for (Field field : persistentClass.getDeclaredFields()) {
                CascadeWrite cascadeConfig = field.getAnnotation(CascadeWrite.class);
                if (cascadeConfig == null) {
                    continue;
                }
                Method getter = persistentClass.getMethod(getPropertyGetter(field.getName()));
                if (log.isDebugEnabled()) {
                    log.debug("CascadeWrite on field '" + field.getName() + "' using method '" + getter.getName() + "'");
                }
                cascades.put(getter, cascadeConfig);
            }
        } catch (SecurityException e) {
            throw new InvalidDataAccessApiUsageException("Could not read cascade write configuration", e);
        } catch (NoSuchMethodException e) {
            throw new InvalidDataAccessApiUsageException("Could not read cascade write configuration", e);
        }
    }

    public ID create(T object) {
        if (useLocking) {
            incrementVersion(object);
        }
        @SuppressWarnings("unchecked") ID objectId = (ID) getSqlMapClientTemplate().insert(getCreateStatement(), object);
        try {
            doCascadeOperation(object, new Operation(OperationType.CREATE) {

                @Override
                void execute(T parentObject, Object object) {
                    getSqlMapClientTemplate().insert(getCreateStatement(object.getClass()), buildCascadeMap(parentObject, object));
                }

                @Override
                void executeCollection(T parentObject, Object object) {
                    getSqlMapClientTemplate().insert(getCreateCollectionStatement(object.getClass()), parentObject);
                }
            });
        } catch (IllegalArgumentException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade creation", e);
        } catch (IllegalAccessException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade creation", e);
        } catch (InvocationTargetException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade creation", e);
        }
        return objectId;
    }

    protected void incrementVersion(T object) {
        try {
            Integer currentVersion = (Integer) versionGetter.invoke(object);
            if (currentVersion == null) {
                versionSetter.invoke(object, 1);
            } else {
                versionSetter.invoke(object, currentVersion + 1);
            }
        } catch (IllegalArgumentException e) {
            throw new ObjectOptimisticLockingFailureException("Could not increment version number", e);
        } catch (IllegalAccessException e) {
            throw new ObjectOptimisticLockingFailureException("Could not increment version number", e);
        } catch (InvocationTargetException e) {
            throw new ObjectOptimisticLockingFailureException("Could not increment version number", e);
        }
    }

    public T read(ID id) {
        @SuppressWarnings("unchecked") T object = (T) getSqlMapClientTemplate().queryForObject(getReadStatement(), id);
        if (object == null) {
            throw new ObjectRetrievalFailureException(persistentClass, id);
        }
        return object;
    }

    public void update(T object) {
        if (useLocking) {
            incrementVersion(object);
        }
        getSqlMapClientTemplate().update(getUpdateStatement(), object);
        try {
            doCascadeOperation(object, new Operation(OperationType.UPDATE) {

                @Override
                void execute(T parentObject, Object object) {
                    getSqlMapClientTemplate().update(getUpdateStatement(object.getClass()), buildCascadeMap(parentObject, object));
                }

                @Override
                void executeCollection(T parentObject, Object object) {
                    getSqlMapClientTemplate().update(getUpdateCollectionStatement(object.getClass()), parentObject);
                }
            });
        } catch (IllegalArgumentException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade operation", e);
        } catch (IllegalAccessException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade operation", e);
        } catch (InvocationTargetException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade operation", e);
        }
    }

    public void delete(T object) {
        getSqlMapClientTemplate().delete(getDeleteStatement(), object);
        try {
            doCascadeOperation(object, new Operation(OperationType.DELETE) {

                @Override
                void execute(T parentObject, Object object) {
                    getSqlMapClientTemplate().delete(getDeleteStatement(object.getClass()), buildCascadeMap(parentObject, object));
                }

                @Override
                void executeCollection(T parentObject, Object object) {
                    getSqlMapClientTemplate().delete(getDeleteCollectionStatement(object.getClass()), parentObject);
                }
            });
        } catch (IllegalArgumentException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade operation", e);
        } catch (IllegalAccessException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade operation", e);
        } catch (InvocationTargetException e) {
            throw new InvalidDataAccessApiUsageException("Error processing cascade operation", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<T> find(String finder, Object... args) {
        Object param = null;
        if (args.length == 1) {
            param = args[0];
        } else if (args.length > 1) {
            param = buildFinderMap(args);
        }
        return (List<T>) getSqlMapClientTemplate().queryForList(getQueryName(finder), param);
    }

    protected Map buildFinderMap(Object... args) {
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        for (int i = 0; i < args.length; i++) {
            paramsMap.put(String.valueOf(i + 1), args[i]);
        }
        return paramsMap;
    }

    protected String getCreateStatement() {
        return persistentClass.getSimpleName() + ".create";
    }

    protected String getCreateStatement(Class clazz) {
        return persistentClass.getSimpleName() + ".create" + clazz.getSimpleName();
    }

    protected String getCreateCollectionStatement(Class clazz) {
        return persistentClass.getSimpleName() + ".createArrayOf" + clazz.getSimpleName();
    }

    protected String getReadStatement() {
        return persistentClass.getSimpleName() + ".read";
    }

    protected String getUpdateStatement() {
        return persistentClass.getSimpleName() + ".update";
    }

    protected String getUpdateStatement(Class clazz) {
        return persistentClass.getSimpleName() + ".update" + clazz.getSimpleName();
    }

    protected String getUpdateCollectionStatement(Class clazz) {
        return persistentClass.getSimpleName() + ".updateArrayOf" + clazz.getSimpleName();
    }

    protected String getDeleteStatement() {
        return persistentClass.getSimpleName() + ".delete";
    }

    protected String getDeleteStatement(Class clazz) {
        return persistentClass.getSimpleName() + ".delete" + clazz.getSimpleName();
    }

    protected String getDeleteCollectionStatement(Class clazz) {
        return persistentClass.getSimpleName() + ".deleteArrayOf" + clazz.getSimpleName();
    }

    protected enum OperationType {

        CREATE, READ, UPDATE, DELETE
    }

    protected abstract class Operation {

        OperationType type = null;

        Operation(OperationType type) {
            this.type = type;
        }

        abstract void execute(T parentObject, Object object);

        abstract void executeCollection(T parentObject, Object object);
    }

    protected void doCascadeOperation(T parentObject, Operation operation) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        for (Method getter : cascades.keySet()) {
            CascadeWrite cascadeConfig = cascades.get(getter);
            if (log.isDebugEnabled()) {
                log.debug("Cascade " + operation.type + " on " + getter);
            }
            Object object = getter.invoke(parentObject);
            CollectionMode collectionMode = getOperationMode(operation, cascadeConfig);
            if (isCollectionType(object)) {
                if (collectionMode == CollectionMode.ONCE || collectionMode == CollectionMode.BOTH) {
                    operation.executeCollection(parentObject, object);
                }
                if (collectionMode == CollectionMode.ELEMENTS || collectionMode == CollectionMode.BOTH) {
                    for (Object item : getItems(object)) {
                        operation.execute(parentObject, item);
                    }
                }
            } else {
                operation.execute(parentObject, object);
            }
        }
    }

    private CollectionMode getOperationMode(Operation operation, CascadeWrite cascadeConfig) {
        CollectionMode mode = cascadeConfig.value();
        if (mode == CollectionMode.ELEMENTS) {
            if (operation.type == OperationType.CREATE) {
                mode = cascadeConfig.createCollection();
            } else if (operation.type == OperationType.UPDATE) {
                mode = cascadeConfig.updateCollection();
            } else if (operation.type == OperationType.DELETE) {
                mode = cascadeConfig.deleteCollection();
            }
        }
        return mode;
    }

    protected Collection getItems(Object collection) {
        if (collection.getClass().isArray()) {
            return CollectionUtils.arrayToList(collection);
        } else if (Collection.class.isAssignableFrom(collection.getClass())) {
            return (Collection) collection;
        }
        throw new IllegalArgumentException("Not a valid collection type: " + collection.getClass());
    }

    protected boolean isCollectionType(Object object) {
        return object.getClass().isArray() || Collection.class.isAssignableFrom(object.getClass());
    }

    protected Map buildCascadeMap(T parentObject, Object object) {
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put(persistentClass.getSimpleName(), parentObject);
        paramsMap.put(object.getClass().getSimpleName(), object);
        return paramsMap;
    }

    protected String getQueryName(String query) {
        return persistentClass.getSimpleName() + "." + query;
    }
}
