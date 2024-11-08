package net.sf.joafip.store.service.export_import.out;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.StorableAccess;
import net.sf.joafip.kvstore.record.entity.DataRecordIdentifier;
import net.sf.joafip.kvstore.service.HeapException;
import net.sf.joafip.logger.JoafipLogger;
import net.sf.joafip.reflect.DoPrivilegedSetFieldAccessible;
import net.sf.joafip.reflect.HelperReflect;
import net.sf.joafip.reflect.ReflectException;
import net.sf.joafip.service.Version;
import net.sf.joafip.store.entity.EnumKey;
import net.sf.joafip.store.entity.StoreRoot4;
import net.sf.joafip.store.entity.classinfo.ClassInfo;
import net.sf.joafip.store.entity.classinfo.FieldInfo;
import net.sf.joafip.store.entity.objectio.ObjectAndItsClassInfo;
import net.sf.joafip.store.service.IStore;
import net.sf.joafip.store.service.Store;
import net.sf.joafip.store.service.StoreClassNotFoundException;
import net.sf.joafip.store.service.StoreDataCorruptedException;
import net.sf.joafip.store.service.StoreException;
import net.sf.joafip.store.service.StoreInvalidClassException;
import net.sf.joafip.store.service.StoreNotSerializableException;
import net.sf.joafip.store.service.StoreTooBigForSerializationException;
import net.sf.joafip.store.service.classinfo.ClassInfoException;

/**
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
public abstract class AbstractExporter implements IExporter {

    protected final JoafipLogger logger = JoafipLogger.getLogger(getClass());

    private static final DoPrivilegedSetFieldAccessible SET_FIELD_ACCESSIBLE = DoPrivilegedSetFieldAccessible.getInstance();

    private static final HelperReflect helperReflect = HelperReflect.getInstance();

    private transient File classDirectory;

    private transient Set<Class<?>> classSet;

    /** true if export persisted class byte code */
    private transient boolean exportPersistedClassByteCode;

    /** identifier of not persisted object map */
    private transient Map<Object, Integer> idOfObjectMap;

    private transient Map<Integer, Object> objectByIdMap;

    private transient Deque<Integer> objectIdQue;

    /** next not persisted object identifier */
    private transient int nextObjectId;

    private IExporterListener listener;

    private ExportStoreQue exportStoreQue;

    private final StoreAccessForExport storeAccessForExport;

    private transient int numberExported;

    public AbstractExporter(final IStore store) throws StoreException {
        super();
        storeAccessForExport = new StoreAccessForExport(store);
    }

    @Override
    public void setListener(final IExporterListener listener) {
        this.listener = listener;
    }

    @Override
    public void export(final String directoryName, final String temporaryDirectoryName, final boolean exportPersistedClassByteCode) throws StoreException, StoreClassNotFoundException, StoreInvalidClassException, StoreDataCorruptedException, StoreNotSerializableException, StoreTooBigForSerializationException {
        exportInitialization(directoryName, temporaryDirectoryName, exportPersistedClassByteCode);
        try {
            exportVisit();
        } catch (final StoreException exception) {
            closeWriterAfterError();
            throw exception;
        } catch (final StoreClassNotFoundException exception) {
            closeWriterAfterError();
            throw exception;
        } catch (final StoreInvalidClassException exception) {
            closeWriterAfterError();
            throw exception;
        } catch (final StoreDataCorruptedException exception) {
            closeWriterAfterError();
            throw exception;
        } catch (final StoreNotSerializableException exception) {
            closeWriterAfterError();
            throw exception;
        } catch (final StoreTooBigForSerializationException exception) {
            closeWriterAfterError();
            throw exception;
        } finally {
            exportEnd();
        }
    }

    @Override
    public void export(final String directoryName, final String temporaryDirectoryName, final Object objectToExport) throws StoreException, StoreClassNotFoundException, StoreInvalidClassException, StoreDataCorruptedException, StoreNotSerializableException {
        exportInitialization(directoryName, temporaryDirectoryName, false);
        idOfObjectMap = new IdentityHashMap<Object, Integer>();
        objectByIdMap = new TreeMap<Integer, Object>();
        objectIdQue = new LinkedList<Integer>();
        try {
            exportVisitOfObject(objectToExport);
        } catch (final StoreException exception) {
            closeWriterAfterError();
            throw exception;
        } finally {
            exportEnd();
            idOfObjectMap = null;
            objectByIdMap = null;
        }
    }

    private void exportInitialization(final String directoryName, final String temporaryDirectoryName, final boolean exportPersistedClassByteCode) throws StoreException, StoreClassNotFoundException {
        this.exportPersistedClassByteCode = exportPersistedClassByteCode;
        final File directory = new File(directoryName);
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new StoreException(directory + " exists and is not a directory");
            }
        } else if (!directory.mkdirs()) {
            throw new StoreException("failed create directory " + directory);
        }
        if (exportPersistedClassByteCode) {
            classDirectory = new File(directory, "class");
            if (classDirectory.exists()) {
                if (classDirectory.isDirectory()) {
                    final File[] files = classDirectory.listFiles();
                    for (final File file : files) {
                        if (file.getName().endsWith(".class") && !file.delete()) {
                            throw new StoreException("failed delete " + file);
                        }
                    }
                } else if (!classDirectory.delete()) {
                    throw new StoreException("failed delete " + classDirectory);
                }
            } else if (!classDirectory.mkdirs()) {
                throw new StoreException("failed create directory " + classDirectory);
            }
            classSet = new HashSet<Class<?>>();
        }
        final File temporaryDirectory = new File(temporaryDirectoryName);
        if (temporaryDirectory.exists()) {
            if (!temporaryDirectory.isDirectory()) {
                throw new StoreException(temporaryDirectory + " exists and is not a directory");
            }
        } else if (!temporaryDirectory.mkdirs()) {
            throw new StoreException("failed create directory " + temporaryDirectory);
        }
        try {
            exportStoreQue = new ExportStoreQue(temporaryDirectoryName);
        } catch (HeapException exception) {
            throw new StoreException(exception);
        }
        open(directory);
    }

    private void exportEnd() throws StoreException {
        try {
            exportStoreQue.close();
        } catch (final Exception exception2) {
            logger.error("closing store for que", exception2);
        }
        close();
        classSet = null;
    }

    /**
	 * opening for export
	 * 
	 * @param directory
	 * @throws StoreException
	 */
    protected abstract void open(final File directory) throws StoreException;

    /**
	 * close export
	 * 
	 * @throws StoreException
	 */
    protected abstract void close() throws StoreException;

    /**
	 * close export after an error
	 */
    protected abstract void closeWriterAfterError();

    /**
	 * visit object for export
	 * 
	 * @throws StoreException
	 * @throws StoreClassNotFoundException
	 * @throws StoreInvalidClassException
	 * @throws StoreNotSerializableException
	 * @throws StoreDataCorruptedException
	 * @throws StoreTooBigForSerializationException
	 */
    private void exportVisit() throws StoreException, StoreClassNotFoundException, StoreInvalidClassException, StoreNotSerializableException, StoreDataCorruptedException, StoreTooBigForSerializationException {
        try {
            final int batchSize;
            if (listener == null) {
                batchSize = 0;
            } else {
                batchSize = listener.getBatchSize();
            }
            DataRecordIdentifier dataRecordIdentifier = storeAccessForExport.getNextFreeDataRecordIdentifier();
            ObjectAndItsClassInfo objectAndItsClassInfo = storeAccessForExport.createGenericObjectReadingInStoreNotLazy(Store.IDENTIFIER_FOR_ROOT);
            final StoreRoot4 storeRoot = (StoreRoot4) objectAndItsClassInfo.getObject();
            final int dataModelIdentifier = dataModelIdentifier(storeRoot);
            beginExportVisit(Version.JOAFIP_RELEASE, dataModelIdentifier, dataRecordIdentifier.value);
            exportStoreQue.initialize();
            exportRoot(storeRoot);
            storeAccessForExport.clearMemory();
            int batchCount = 0;
            numberExported = 0;
            dataRecordIdentifier = exportStoreQue.pollFirst();
            while (dataRecordIdentifier != null) {
                objectAndItsClassInfo = storeAccessForExport.createGenericObjectReadingInStoreNotLazy(dataRecordIdentifier);
                final ClassInfo objectClassInfo = objectAndItsClassInfo.objectClassInfo;
                if (exportPersistedClassByteCode) {
                    exportClass(objectClassInfo);
                }
                final Object object = objectAndItsClassInfo.getObject();
                if (objectClassInfo.isStringType()) {
                    exportString(dataRecordIdentifier, (String) object);
                } else if (objectClassInfo.isArrayType()) {
                    exportArray(dataRecordIdentifier, object, objectClassInfo);
                } else if (objectClassInfo.isEnumType()) {
                    FieldInfo[] fieldInfos;
                    fieldInfos = objectClassInfo.allDeclaredFieldsWithTransientWithoutStatic();
                    exportEnum(dataRecordIdentifier, object, objectClassInfo, fieldInfos);
                } else {
                    FieldInfo[] fieldInfos;
                    fieldInfos = objectClassInfo.allDeclaredFieldsWithTransientWithoutStatic();
                    exportGeneric(dataRecordIdentifier, object, objectClassInfo, fieldInfos);
                }
                storeAccessForExport.clearMemory();
                numberExported = exportStoreQue.getNumberExported();
                dataRecordIdentifier = exportStoreQue.pollFirst();
                if (listener != null && ++batchCount == batchSize) {
                    batchCount = 0;
                    listener.numberOfExported(numberExported);
                }
            }
            if (listener != null) {
                listener.numberOfExported(numberExported);
            }
            endExportVisit();
        } catch (final ClassInfoException exception) {
            throw new StoreException(exception);
        } catch (HeapException exception) {
            throw new StoreException(exception);
        }
    }

    @Override
    public int getNumberOfObjectExported() {
        return numberExported;
    }

    private void exportVisitOfObject(final Object objectToVisit) throws StoreException {
        beginExportVisit(Version.JOAFIP_RELEASE, -1, -1);
        exportVisitNotPersisted(objectToVisit);
        endExportVisit();
    }

    @StorableAccess
    private Set<Long> dataRecordIdOfpersistedStaticSet(final StoreRoot4 storeRoot) {
        final Set<Long> dataRecordIdOfpersistedStaticSet = storeRoot.getDataRecordIdOfpersistedStaticSet();
        final TreeSet<Long> resultSet = new TreeSet<Long>();
        if (dataRecordIdOfpersistedStaticSet != null) {
            resultSet.addAll(dataRecordIdOfpersistedStaticSet);
        }
        return resultSet;
    }

    @StorableAccess
    private int dataModelIdentifier(final StoreRoot4 storeRoot) {
        return storeRoot.getDataModelIdentifier();
    }

    /**
	 * export of not persisted object
	 * 
	 * @param xobject
	 *            the object to export
	 * @throws StoreException
	 */
    private void exportVisitNotPersisted(final Object xobject) throws StoreException {
        addObjectToQue(xobject);
        Integer objectIdentifier = objectIdQue.pollFirst();
        while (objectIdentifier != null) {
            final Object object = objectByIdMap.get(objectIdentifier);
            final Class<?> objectClass = object.getClass();
            if (exportPersistedClassByteCode) {
                exportClass(objectClass);
            }
            final ClassInfo objectClassInfo = storeAccessForExport.getNoProxyClassInfo(objectClass);
            if (objectClassInfo.isStringType()) {
                exportString(objectIdentifier, (String) object);
            } else if (objectClassInfo.isArrayType()) {
                exportArray(objectIdentifier, object, objectClassInfo);
            } else if (objectClassInfo.isEnumType()) {
                FieldInfo[] fieldInfos;
                try {
                    fieldInfos = objectClassInfo.allDeclaredFieldsWithTransientWithoutStatic();
                } catch (final ClassInfoException exception) {
                    throw new StoreException(exception);
                }
                exportEnum(objectIdentifier, object, objectClassInfo, fieldInfos);
            } else {
                FieldInfo[] fieldInfos;
                try {
                    fieldInfos = objectClassInfo.allDeclaredFieldsWithTransientWithoutStatic();
                } catch (final ClassInfoException exception) {
                    throw new StoreException(exception);
                }
                exportGeneric(objectIdentifier, object, objectClassInfo, fieldInfos);
            }
            objectIdentifier = objectIdQue.pollFirst();
        }
    }

    private void exportNotPersistedReferenceByObject(final Object object) throws StoreException {
        addObjectToQue(object);
        final Integer objectIdentifier = idOfObjectMap.get(object);
        exportNotPersistedReference(objectIdentifier.intValue());
    }

    /**
	 * export reference to not persisted object
	 * 
	 * @param objectIdentifier
	 *            the not persisted object identifier
	 * @throws StoreException
	 */
    protected abstract void exportNotPersistedReference(int objectIdentifier) throws StoreException;

    /**
	 * add object to queue (update map)
	 * 
	 * @param object
	 * @return true if added
	 */
    private boolean addObjectToQue(final Object object) {
        final boolean added;
        Integer objectIdentifier = idOfObjectMap.get(object);
        if (objectIdentifier == null) {
            objectIdentifier = nextObjectId++;
            idOfObjectMap.put(object, objectIdentifier);
            objectByIdMap.put(objectIdentifier, object);
            objectIdQue.add(objectIdentifier);
            added = true;
        } else {
            added = false;
        }
        return added;
    }

    /**
	 * begin of the visit for export
	 * 
	 * @param joafipRelease
	 * @param dataModelIdentifier
	 * @param lastRecordId
	 * @throws StoreException
	 */
    protected abstract void beginExportVisit(String joafipRelease, int dataModelIdentifier, long lastRecordId) throws StoreException;

    /**
	 * end visit for export
	 * 
	 * @throws StoreException
	 */
    protected abstract void endExportVisit() throws StoreException;

    /**
	 * root object export
	 * 
	 * @param storeRoot
	 * @throws StoreException
	 * @throws StoreClassNotFoundException
	 * @throws StoreInvalidClassException
	 * @throws StoreNotSerializableException
	 * @throws StoreDataCorruptedException
	 */
    private void exportRoot(final StoreRoot4 storeRoot) throws StoreException, StoreClassNotFoundException, StoreInvalidClassException, StoreNotSerializableException, StoreDataCorruptedException {
        beginExportRoot();
        final String storeRootClassName = StoreRoot4.class.getName();
        beginExportField(Object.class.getName(), "rootObject", storeRootClassName, false, false);
        final Object object;
        object = rootObject(storeRoot);
        final Map<EnumKey, Enum<?>> enumMap = storedEnumMap(storeRoot);
        final Set<Long> set = dataRecordIdOfpersistedStaticSet(storeRoot);
        exportReference(object);
        endExportField();
        beginExportField(Map.class.getName(), "storedEnumMap", storeRootClassName, false, false);
        exportReference(enumMap);
        endExportField();
        beginExportStaticField();
        if (set != null) {
            for (final long dataRecordIdentifierValue : set) {
                final DataRecordIdentifier dataRecordIdentifier = new DataRecordIdentifier(dataRecordIdentifierValue);
                exportReference(dataRecordIdentifier);
                try {
                    exportStoreQue.addDataRecordIdentifier(dataRecordIdentifier);
                } catch (HeapException exception) {
                    throw new StoreException(exception);
                }
            }
        }
        endExportStaticField();
        endExportRoot();
    }

    @StorableAccess
    private Map<EnumKey, Enum<?>> storedEnumMap(final StoreRoot4 storeRoot) {
        return storeRoot.getStoredEnumMap();
    }

    @StorableAccess
    private Object rootObject(final StoreRoot4 storeRoot) {
        return storeRoot.getRootObject();
    }

    /**
	 * begin of root export
	 * 
	 * @throws StoreException
	 */
    protected abstract void beginExportRoot() throws StoreException;

    /**
	 * end of root export
	 * 
	 * @throws StoreException
	 */
    protected abstract void endExportRoot() throws StoreException;

    protected abstract void beginExportStaticField() throws StoreException;

    protected abstract void endExportStaticField() throws StoreException;

    /**
	 * export string
	 * 
	 * @param dataRecordIdentifier
	 * @param string
	 *            the string instance
	 * @throws StoreException
	 */
    protected abstract void exportString(final DataRecordIdentifier dataRecordIdentifier, final String string) throws StoreException;

    /**
	 * export not persisted string
	 * 
	 * @param objectIdentifier
	 * @param string
	 *            the string instance
	 * @throws StoreException
	 */
    protected abstract void exportString(final int objectIdentifier, final String string) throws StoreException;

    @SuppressWarnings("unused")
    private void exportStatic(final ClassInfo objectClassInfo, final DataRecordIdentifier objectDataRecordIdentifier) throws StoreException {
        try {
            beginExportStatic(objectDataRecordIdentifier, objectClassInfo);
            final FieldInfo[] fieldInfos = objectClassInfo.getAllDeclaredStaticFields();
            for (final FieldInfo fieldInfo : fieldInfos) {
                if (fieldInfo.isPersisted()) {
                    final Field field = fieldInfo.getField();
                    SET_FIELD_ACCESSIBLE.set(field);
                    final Object fieldValue;
                    fieldValue = field.get(null);
                    final ClassInfo fieldClassInfo = fieldInfo.getFieldTypeInfo();
                    final String fieldName = fieldInfo.getFieldName();
                    final String fieldClassName = fieldClassInfo.getName();
                    final String fieldDeclaringClassName = fieldInfo.getDeclaringClassName();
                    final boolean staticField = fieldInfo.isStaticField();
                    final boolean transientField = fieldInfo.isTransientField();
                    beginExportField(fieldClassName, fieldName, fieldDeclaringClassName, staticField, transientField);
                    if (fieldClassInfo.isBasicType()) {
                        exportBasicValue(fieldValue);
                    } else {
                        exportReference(fieldValue);
                    }
                    endExportField();
                }
            }
            endExportStatic();
        } catch (final IllegalArgumentException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final IllegalAccessException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final ClassInfoException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        }
    }

    /**
	 * export generic object
	 * 
	 * @param objectDataRecordIdentifier
	 *            the object data record identifier
	 * @param object
	 *            the object
	 * @param objectClassInfo
	 *            the object class information
	 * @param fieldInfos
	 *            the object field's information
	 * @throws StoreException
	 */
    private void exportGeneric(final DataRecordIdentifier objectDataRecordIdentifier, final Object object, final ClassInfo objectClassInfo, final FieldInfo[] fieldInfos) throws StoreException {
        try {
            beginExportGeneric(objectDataRecordIdentifier, objectClassInfo);
            for (final FieldInfo fieldInfo : fieldInfos) {
                if (fieldInfo.isPersisted()) {
                    final Field field = fieldInfo.getField();
                    SET_FIELD_ACCESSIBLE.set(field);
                    final Object fieldValue;
                    fieldValue = field.get(object);
                    final ClassInfo fieldClassInfo = fieldInfo.getFieldTypeInfo();
                    final String fieldName = fieldInfo.getFieldName();
                    final String fieldClassName = fieldClassInfo.getName();
                    final String fieldDeclaringClassName = fieldInfo.getDeclaringClassName();
                    final boolean staticField = fieldInfo.isStaticField();
                    final boolean transientField = fieldInfo.isTransientField();
                    beginExportField(fieldClassName, fieldName, fieldDeclaringClassName, staticField, transientField);
                    if (fieldClassInfo.isBasicType()) {
                        exportBasicValue(fieldValue);
                    } else {
                        exportReference(fieldValue);
                    }
                    endExportField();
                }
            }
            endExportGeneric();
        } catch (final IllegalArgumentException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final IllegalAccessException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final ClassInfoException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        }
    }

    /**
	 * export not persisted generic object
	 * 
	 * @param objectIdentifier
	 *            the object instance identifier
	 * @param object
	 *            the object
	 * @param objectClassInfo
	 *            the object class information
	 * @param fieldInfos
	 *            the object field's information
	 * @throws StoreException
	 */
    private void exportGeneric(final int objectIdentifier, final Object object, final ClassInfo objectClassInfo, final FieldInfo[] fieldInfos) throws StoreException {
        try {
            beginExportNotPersistedGeneric(objectIdentifier, objectClassInfo);
            for (final FieldInfo fieldInfo : fieldInfos) {
                if (fieldInfo.isPersisted()) {
                    final Field field = fieldInfo.getField();
                    SET_FIELD_ACCESSIBLE.set(field);
                    final Object fieldValue;
                    fieldValue = field.get(object);
                    final ClassInfo fieldClassInfo = fieldInfo.getFieldTypeInfo();
                    final String fieldName = fieldInfo.getFieldName();
                    final String fieldClassName = fieldClassInfo.getName();
                    final String fieldDeclaringClassName = fieldInfo.getDeclaringClassName();
                    final boolean staticField = fieldInfo.isStaticField();
                    final boolean transientField = fieldInfo.isTransientField();
                    beginExportField(fieldClassName, fieldName, fieldDeclaringClassName, staticField, transientField);
                    if (fieldClassInfo.isBasicType()) {
                        exportBasicValue(fieldValue);
                    } else {
                        exportReferenceInNotPersisted(fieldValue);
                    }
                    endExportField();
                }
            }
            endExportNotPersistedGeneric();
        } catch (final IllegalArgumentException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final IllegalAccessException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final ClassInfoException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        }
    }

    protected abstract void beginExportStatic(final DataRecordIdentifier objectDataRecordIdentifier, final ClassInfo objectClassInfo) throws StoreException;

    protected abstract void endExportStatic() throws StoreException;

    /**
	 * begin generic object export
	 * 
	 * @param objectDataRecordIdentifier
	 *            the object data record identifier
	 * @param objectClassInfo
	 *            the object class information
	 * @throws StoreException
	 */
    protected abstract void beginExportGeneric(final DataRecordIdentifier objectDataRecordIdentifier, final ClassInfo objectClassInfo) throws StoreException;

    /**
	 * begin generic not persisted object export
	 * 
	 * @param objectIdentifier
	 *            the object data record identifier
	 * @param objectClassInfo
	 *            the object class information
	 * @throws StoreException
	 */
    protected abstract void beginExportNotPersistedGeneric(final int objectIdentifier, final ClassInfo objectClassInfo) throws StoreException;

    /**
	 * end of generic object export
	 * 
	 * @throws StoreException
	 */
    protected abstract void endExportGeneric() throws StoreException;

    /**
	 * end of generic not persisted object export
	 * 
	 * @throws StoreException
	 */
    protected abstract void endExportNotPersistedGeneric() throws StoreException;

    /**
	 * begin of an object field export
	 * 
	 * @param fieldClassName
	 *            the field class name
	 * @param fieldName
	 *            the field name
	 * @param fieldDeclaringClassName
	 *            the field declaring class name
	 * @param staticField
	 * @param transientField
	 * @throws StoreException
	 */
    protected abstract void beginExportField(String fieldClassName, String fieldName, String fieldDeclaringClassName, boolean staticField, boolean transientField) throws StoreException;

    /**
	 * end of object field export
	 * 
	 * @throws StoreException
	 */
    protected abstract void endExportField() throws StoreException;

    /**
	 * export a basic value
	 * 
	 * @param value
	 *            the value to export
	 * @throws StoreException
	 */
    protected abstract void exportBasicValue(Object value) throws StoreException;

    /**
	 * export reference
	 * 
	 * @param object
	 * @throws StoreException
	 */
    private void exportReference(final Object object) throws StoreException {
        if (object == null) {
            exportNullReference();
        } else {
            exportNotNullReference(object);
        }
    }

    private void exportReferenceInNotPersisted(final Object object) throws StoreException {
        if (object == null) {
            exportNullReference();
        } else {
            exportNotNullReferenceInNotPersisted(object);
        }
    }

    /**
	 * export not null reference
	 * 
	 * @param object
	 * @throws StoreException
	 */
    private void exportNotNullReference(final Object object) throws StoreException {
        final DataRecordIdentifier dataRecordIdentifier = storeAccessForExport.dataRecordIdentifierOfObject(object);
        if (dataRecordIdentifier == null) {
            idOfObjectMap = new IdentityHashMap<Object, Integer>();
            objectByIdMap = new TreeMap<Integer, Object>();
            objectIdQue = new LinkedList<Integer>();
            nextObjectId = 0;
            exportVisitNotPersisted(object);
            idOfObjectMap = null;
            objectByIdMap = null;
        } else {
            exportReference(dataRecordIdentifier);
            try {
                exportStoreQue.addDataRecordIdentifier(dataRecordIdentifier);
            } catch (HeapException exception) {
                throw new StoreException(exception);
            }
        }
    }

    private void exportNotNullReferenceInNotPersisted(final Object object) throws StoreException {
        final DataRecordIdentifier dataRecordIdentifier = storeAccessForExport.dataRecordIdentifierOfObject(object);
        if (dataRecordIdentifier == null) {
            exportNotPersistedReferenceByObject(object);
        } else {
            exportReference(dataRecordIdentifier);
            try {
                exportStoreQue.addDataRecordIdentifier(dataRecordIdentifier);
            } catch (HeapException exception) {
                throw new StoreException(exception);
            }
        }
    }

    /**
	 * export null reference
	 * 
	 * @throws StoreException
	 */
    protected abstract void exportNullReference() throws StoreException;

    /**
	 * export not null reference
	 * 
	 * @param dataRecordIdentifier
	 *            the data record identifier as reference
	 * @throws StoreException
	 */
    protected abstract void exportReference(DataRecordIdentifier dataRecordIdentifier) throws StoreException;

    /**
	 * export array
	 * 
	 * @param arrayDataRecordIdentifier
	 *            the array instance data record identifier
	 * @param array
	 *            the array
	 * @param arrayComponentClassInfo
	 *            the array component class information
	 * @throws StoreException
	 */
    private void exportArray(final DataRecordIdentifier arrayDataRecordIdentifier, final Object array, final ClassInfo arrayComponentClassInfo) throws StoreException {
        try {
            final int arrayLength = helperReflect.arrayLength(array);
            beginExportArray(arrayDataRecordIdentifier, arrayLength, arrayComponentClassInfo);
            if (arrayComponentClassInfo.getComponentType().isBasicType()) {
                for (int index = 0; index < arrayLength; index++) {
                    final Object elementValue = helperReflect.getArrayElement(array, index);
                    exportBasicValue(elementValue);
                }
            } else {
                for (int index = 0; index < arrayLength; index++) {
                    final Object elementValue = helperReflect.getArrayElement(array, index);
                    exportReference(elementValue);
                }
            }
            endExportArray();
        } catch (final ReflectException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        }
    }

    /**
	 * export not persisted array
	 * 
	 * @param arrayIdentifier
	 *            the array instance identifier
	 * @param array
	 *            the array
	 * @param arrayComponentClassInfo
	 *            the array component class information
	 * @throws StoreException
	 */
    private void exportArray(final int arrayIdentifier, final Object array, final ClassInfo arrayComponentClassInfo) throws StoreException {
        try {
            final int arrayLength = helperReflect.arrayLength(array);
            beginExportArrayNotPersisted(arrayIdentifier, arrayLength, arrayComponentClassInfo);
            if (arrayComponentClassInfo.getComponentType().isBasicType()) {
                for (int index = 0; index < arrayLength; index++) {
                    final Object elementValue = helperReflect.getArrayElement(array, index);
                    exportBasicValue(elementValue);
                }
            } else {
                for (int index = 0; index < arrayLength; index++) {
                    final Object elementValue = helperReflect.getArrayElement(array, index);
                    exportReferenceInNotPersisted(elementValue);
                }
            }
            endExportArrayNotPersisted();
        } catch (final ReflectException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        }
    }

    /**
	 * begin of array export
	 * 
	 * @param arrayDataRecordIdentifier
	 *            the array instance data record identifier
	 * @param arrayLength
	 *            the array length
	 * @param arrayComponentClassInfo
	 *            the array component class information
	 * @throws StoreException
	 */
    protected abstract void beginExportArray(final DataRecordIdentifier arrayDataRecordIdentifier, final int arrayLength, final ClassInfo arrayComponentClassInfo) throws StoreException;

    protected abstract void endExportArrayNotPersisted() throws StoreException;

    /**
	 * end of array export
	 * 
	 * @throws StoreException
	 */
    protected abstract void endExportArray() throws StoreException;

    protected abstract void beginExportArrayNotPersisted(int arrayIdentifier, int arrayLength, ClassInfo arrayComponentClassInfo) throws StoreException;

    /**
	 * export enum
	 * 
	 * @param enumDataRecordIdentifier
	 *            the enum instance data record identifier
	 * @param enumObject
	 *            the enum
	 * @param enumClassInfo
	 *            the enum class information
	 * @param fieldInfos
	 *            the enum field information
	 * @throws StoreException
	 */
    private void exportEnum(final DataRecordIdentifier enumDataRecordIdentifier, final Object enumObject, final ClassInfo enumClassInfo, final FieldInfo[] fieldInfos) throws StoreException {
        try {
            beginExportEnum(enumDataRecordIdentifier, enumClassInfo, (Enum<?>) enumObject);
            for (final FieldInfo fieldInfo : fieldInfos) {
                if (fieldInfo.isPersisted()) {
                    final Field field = fieldInfo.getField();
                    SET_FIELD_ACCESSIBLE.set(field);
                    final Object fieldValue;
                    fieldValue = field.get(enumObject);
                    final ClassInfo fieldClassInfo = fieldInfo.getFieldTypeInfo();
                    final String fieldName = fieldInfo.getFieldName();
                    final String fieldClassName = fieldClassInfo.getName();
                    final String fieldDeclaringClassName = fieldInfo.getDeclaringClassName();
                    final boolean staticField = fieldInfo.isStaticField();
                    final boolean transientField = fieldInfo.isTransientField();
                    beginExportField(fieldClassName, fieldName, fieldDeclaringClassName, staticField, transientField);
                    if (fieldClassInfo.isBasicType()) {
                        exportBasicValue(fieldValue);
                    } else {
                        exportReference(fieldValue);
                    }
                    endExportField();
                }
            }
            endExportEnum();
        } catch (final IllegalArgumentException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final IllegalAccessException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final ClassInfoException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        }
    }

    /**
	 * export not persisted enum
	 * 
	 * @param enumIdentifier
	 *            the enum instance identifier
	 * @param enumObject
	 *            the enum
	 * @param enumClassInfo
	 *            the enum class information
	 * @param fieldInfos
	 *            the enum field information
	 * @throws StoreException
	 */
    private void exportEnum(final int enumIdentifier, final Object enumObject, final ClassInfo enumClassInfo, final FieldInfo[] fieldInfos) throws StoreException {
        try {
            beginExportEnumNotPersisted(enumIdentifier, enumClassInfo, (Enum<?>) enumObject);
            for (final FieldInfo fieldInfo : fieldInfos) {
                if (fieldInfo.isPersisted()) {
                    final Field field = fieldInfo.getField();
                    SET_FIELD_ACCESSIBLE.set(field);
                    final Object fieldValue;
                    fieldValue = field.get(enumObject);
                    final ClassInfo fieldClassInfo = fieldInfo.getFieldTypeInfo();
                    final String fieldName = fieldInfo.getFieldName();
                    final String fieldClassName = fieldClassInfo.getName();
                    final String fieldDeclaringClassName = fieldInfo.getDeclaringClassName();
                    final boolean staticField = fieldInfo.isStaticField();
                    final boolean transientField = fieldInfo.isTransientField();
                    beginExportField(fieldClassName, fieldName, fieldDeclaringClassName, staticField, transientField);
                    if (fieldClassInfo.isBasicType()) {
                        exportBasicValue(fieldValue);
                    } else {
                        exportReferenceInNotPersisted(fieldValue);
                    }
                    endExportField();
                }
            }
            endExportEnumNotPersisted();
        } catch (final IllegalArgumentException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final IllegalAccessException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        } catch (final ClassInfoException exception) {
            closeWriterAfterError();
            throw new StoreException(exception);
        }
    }

    /**
	 * begin enum export
	 * 
	 * @param enumDataRecordIdentifier
	 *            the enum instance data record identifier
	 * @param enumClassInfo
	 *            the enum class information
	 * @param enumInstance
	 *            the enum
	 * @throws StoreException
	 */
    protected abstract void beginExportEnum(final DataRecordIdentifier enumDataRecordIdentifier, final ClassInfo enumClassInfo, final Enum<?> enumInstance) throws StoreException;

    protected abstract void beginExportEnumNotPersisted(final int enumIdentifier, final ClassInfo enumClassInfo, final Enum<?> enumInstance) throws StoreException;

    /**
	 * end enum export
	 * 
	 * @throws StoreException
	 */
    protected abstract void endExportEnum() throws StoreException;

    protected abstract void endExportEnumNotPersisted() throws StoreException;

    /**
	 * convert basic value to string ( primitives and correspond java.lang class
	 * )
	 * 
	 * @param basicValue
	 * @return value in string form
	 * @throws StoreException
	 */
    protected String basicValueToString(final Object basicValue) throws StoreException {
        final String toString;
        if (basicValue == null) {
            toString = "null";
        } else {
            final Class<? extends Object> basicValueClass = basicValue.getClass();
            if (Byte.class.equals(basicValueClass)) {
                toString = Integer.toString((Byte) basicValue);
            } else if (Short.class.equals(basicValueClass)) {
                toString = Short.toString((Short) basicValue);
            } else if (Integer.class.equals(basicValueClass)) {
                toString = Integer.toString((Integer) basicValue);
            } else if (Long.class.equals(basicValueClass)) {
                toString = Long.toString((Long) basicValue);
            } else if (Character.class.equals(basicValueClass)) {
                toString = Integer.toString((Character) basicValue);
            } else if (Float.class.equals(basicValueClass)) {
                toString = Float.toString((Float) basicValue);
            } else if (Double.class.equals(basicValueClass)) {
                toString = Double.toString((Double) basicValue);
            } else if (Boolean.class.equals(basicValueClass)) {
                toString = Boolean.toString((Boolean) basicValue);
            } else {
                throw new StoreException("not a basic type " + basicValueClass);
            }
        }
        return toString;
    }

    /**
	 * export class and all super class byte code
	 * 
	 * @param objectClassInfo
	 * @throws StoreException
	 */
    private void exportClass(final ClassInfo objectClassInfo) throws StoreException {
        if (!objectClassInfo.isArrayType()) {
            final Class<?> objectClass = objectClassInfo.getObjectClass();
            exportClass(objectClass);
        }
    }

    private void exportClass(final Class<?> objectClass) throws StoreException {
        Class<?> currentClass = objectClass;
        while (!Object.class.equals(currentClass)) {
            if (classSet.add(currentClass)) {
                exportOneClass(currentClass);
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
	 * export class byte code
	 * 
	 * @param objectClass
	 * @throws StoreException
	 */
    private void exportOneClass(final Class<?> objectClass) throws StoreException {
        final ClassLoader classLoader = storeAccessForExport.getClassLoader();
        final String className = objectClass.getName();
        final String classResourceName = className.replace('.', '/') + ".class";
        final InputStream input = new BufferedInputStream(classLoader.getResourceAsStream(classResourceName));
        final File outFile = new File(classDirectory, className + ".class");
        try {
            final OutputStream output = new BufferedOutputStream(new FileOutputStream(outFile));
            int read;
            while ((read = input.read()) != -1) {
                output.write(read);
            }
            output.flush();
            output.close();
            input.close();
        } catch (final IOException exception) {
            throw new StoreException(exception);
        }
    }
}
