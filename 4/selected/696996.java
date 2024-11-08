package org.j2eebuilder.util;

import java.util.*;
import java.lang.reflect.*;
import java.rmi.RemoteException;
import org.j2eebuilder.*;
import org.j2eebuilder.view.Request;
import org.j2eebuilder.util.UtilityBean;
import java.beans.Expression;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.sql.RowSet;
import org.j2eebuilder.model.ManagedTransientObject;

/**
 * The PrimaryKeyFactory class provides helper methods such as build primary key from the
 * requestHelperBean, javaBean, etc.
 * @version 1.3
 */
public final class PrimaryKeyFactory implements java.io.Serializable {

    private static transient LogManager log = new LogManager(PrimaryKeyFactory.class);

    /**
     *	@return primarykey object
     *	PrimaryKey class must be a serialized & custom class
     * 	(Integer, etc. would not work)
     *	map key->attribute name of the being located component
     *	map value->attribute name of the req param holding the value
     */
    public static Object getPrimaryKey(PrimaryKeyDefinition def, Request req, Map attributeNameMap) throws PrimaryKeyDefinitionException, PrimaryKeyFactoryException {
        if (def == null) {
            throw new PrimaryKeyFactoryException("getPrimaryKey(): PrimaryKeyDefinition is null.");
        }
        log.debug("getPrimaryKey(): trying to build pk [" + def);
        if (req == null) {
            throw new PrimaryKeyFactoryException("getPrimaryKey(): Unable to build a primary key from PrimaryKeyDefinition [" + def + "]. Request is null.");
        }
        try {
            Object pkObject = def.getPrimaryKeyClass().newInstance();
            String name = null;
            Class type = null;
            Field[] fields = pkObject.getClass().getFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                name = f.getName();
                type = f.getType();
                try {
                    String[] params = null;
                    try {
                        params = req.getStringParameter(name);
                    } catch (org.j2eebuilder.util.RequestParameterException e) {
                        if (attributeNameMap != null && attributeNameMap.size() > 0) {
                            String mappedName = (String) attributeNameMap.get(name);
                            if (mappedName != null) {
                                name = mappedName;
                            }
                            params = req.getStringParameter(name);
                        }
                    }
                    if (params == null || params.length == 0) {
                        throw new PrimaryKeyFactoryException("PrimaryKey attribute [" + name + "] value is null.");
                    }
                    Object valueObject = WrapperFactory.buildObject(params[0], type);
                    f.set(pkObject, valueObject);
                } catch (UnsupportedWrapperTypeException e) {
                    throw new PrimaryKeyFactoryException("UnsupportedWrapperTypeException occurred while constructing PrimaryKey [" + def.getClassName() + "] attribute [" + name + "]:" + e.toString());
                } catch (WrapperFactoryException e) {
                    throw new PrimaryKeyFactoryException("WrapperFactoryException occurred while constructing PrimaryKey [" + def.getClassName() + "] attribute [" + name + "]:" + e.toString());
                } catch (RequestParameterException e) {
                    throw new PrimaryKeyFactoryException("RequestParameterException occurred while constructing PrimaryKey [" + def.getClassName() + "] attribute [" + name + "]:" + e.toString());
                }
            }
            return pkObject;
        } catch (DefinitionException e) {
            throw new org.j2eebuilder.PrimaryKeyDefinitionException(e.getCause());
        } catch (InstantiationException e) {
            throw new org.j2eebuilder.PrimaryKeyDefinitionException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new org.j2eebuilder.PrimaryKeyDefinitionException(e.getCause());
        }
    }

    /**
     *	@return created primarykey object
     *	PrimaryKey class must be a serialized & custom class
     * 	(Integer, etc. would not work)
     *	PrimaryKey class variables must be public
     *	fromObject must be a JavaBean with ReadMethods
     *   if fromObject attributes are named diff than toObject attributes (relation components)
     *     use the passed in Map to find the corresponding (diff) name of the fromObject attribute
     *	map key->attribute name of the being located component
     *	map value->attribute name of the fromObject value object holding the value
     */
    public static Object getPrimaryKey(PrimaryKeyDefinition def, ManagedTransientObject fromObject, Map attributeNameMap) throws PrimaryKeyFactoryException {
        String className = null;
        if (def == null) {
            throw new PrimaryKeyFactoryException("getPrimaryKey(PrimaryKeyDefinition[" + def + "], ManagedTransientObject[" + fromObject + "], Map[" + attributeNameMap + "]): Definition [null] is invalid.");
        }
        try {
            Object toObject = def.getPrimaryKeyClass().newInstance();
            className = toObject.getClass().getName();
            String name = null;
            Class type = null;
            Field[] fields = toObject.getClass().getFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                name = f.getName();
                type = f.getType();
                if (attributeNameMap != null && attributeNameMap.size() > 0) {
                    String mappedName = (String) attributeNameMap.get(name);
                    if (mappedName != null) {
                        name = mappedName;
                    }
                }
                try {
                    String readMethod = UtilityBean.getReadMethod(name);
                    Expression expression = new Expression(fromObject, readMethod, null);
                    Object rObj = expression.getValue();
                    Object valueObject = WrapperFactory.buildObject(String.valueOf(rObj), type);
                    f.set(toObject, valueObject);
                } catch (IllegalArgumentException e) {
                    throw new PrimaryKeyFactoryException("IllegalArgumentException occurred while constructing a PrimaryKey [" + className + "] attribute [" + name + "]:" + "Possibly the number of actual parameters supplied (by readMethod of fromObject) via args[" + name + "] is different from the number of formal parameters required by the underlying method (of toObject's writeMethod).[" + name + "]" + e.toString());
                } catch (java.lang.NoSuchMethodException e) {
                    log.log("getPrimaryKey(PrimaryKeyDefinition[" + def + "], ManagedTransientObject[" + fromObject + "], Map[" + attributeNameMap + "]): " + "NoSuchMethodException occurred while a PrimaryKey [" + className + "] attribute [" + name + "]:", e, log.ERROR);
                    throw new PrimaryKeyFactoryException("getPrimaryKey(PrimaryKeyDefinition[" + def + "], ManagedTransientObject[" + fromObject + "], Map[" + attributeNameMap + "]): " + "NoSuchMethodException occurred while a PrimaryKey [" + className + "] attribute [" + name + "]:" + e.toString());
                } catch (Exception e) {
                    throw new PrimaryKeyFactoryException("getPrimaryKey(PrimaryKeyDefinition[" + def + "], ManagedTransientObject[" + fromObject + "], Map[" + attributeNameMap + "]): " + "Error occurred while constructing a PrimaryKey [" + className + "] attribute [" + name + "]:" + e.toString());
                }
            }
            return toObject;
        } catch (Exception e) {
            throw new PrimaryKeyFactoryException("getPrimaryKey(PrimaryKeyDefinition[" + def + "], ManagedTransientObject[" + fromObject + "], Map[" + attributeNameMap + "]): " + e.toString());
        }
    }

    /**
     *	@return a set of primary key objects
     *	PrimaryKey class must be a serialized & custom class
     * 	(Integer, etc. would not work)
     *	PrimaryKey class variables must be public
     *	fromRowSet object implements javax.sql.RowSet interface
     */
    public static Set getPrimaryKey(PrimaryKeyDefinition def, RowSet fromRowSet) throws PrimaryKeyFactoryException {
        if (fromRowSet == null || def == null) {
            throw new PrimaryKeyFactoryException("getPrimaryKey(PrimaryKeyDefinition[" + def + "], RowSet[" + fromRowSet + "]): Mandatory parameter is null.");
        }
        Set pkSet = new HashSet();
        String className = null;
        try {
            Class pkClass = def.getPrimaryKeyClass();
            Field[] fields = pkClass.getFields();
            String name = null;
            Class type = null;
            while (fromRowSet.next()) {
                Object toObject = pkClass.newInstance();
                for (int i = 0; i < fields.length; i++) {
                    Field f = fields[i];
                    name = f.getName();
                    type = f.getType();
                    Object rObj = fromRowSet.getObject(name);
                    Object typedObj = WrapperFactory.buildObject(String.valueOf(rObj), type);
                    f.set(toObject, typedObj);
                }
                pkSet.add(toObject);
            }
        } catch (Exception e) {
            throw new PrimaryKeyFactoryException("getPrimaryKey(PrimaryKeyDefinition[" + def + "], RowSet[" + fromRowSet + "]): " + e.toString());
        }
        return pkSet;
    }
}
