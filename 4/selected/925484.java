package org.opencube.oms;

import info.fingo.util.TemplateParser;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import org.opencube.data.BoundVariable;
import org.opencube.data.CallResult;
import org.opencube.data.DataSource;
import org.opencube.data.Record;
import org.opencube.data.RecordSet;
import org.opencube.oms.meta.ComplexAttribute;
import org.opencube.oms.meta.DatatypeEnumeration;
import org.opencube.oms.meta.OMSMetaData;
import org.opencube.oms.meta.Scheme;
import org.opencube.oms.meta.SimpleAttribute;
import org.opencube.util.Constants;

/**
 * @author <a href="mailto:maciek@fingo.pl">FINGO - Maciej Mroczko</a>
 * TODO: comment
 */
class DBHelper {

    static final String RIGHT_GRANTED = "1";

    static final String RIGHT_DENIED = "0";

    /** 
	 * The token start 
	 */
    static final String TOKEN_START = "[";

    /** 
	 * The token end 
	 */
    static final String TOKEN_END = "]";

    /**
	 * The common statement token constant: where string = 'whereStr'
	 */
    static final String TOKEN_WHERE = "whereStr";

    /**
	 * The resource name constant: 'getOMSStructureByWhere'
	 */
    static final String RESOURCE_OMS_STRUCTURE_BY_WHERE = "getOMSStructureByWhere";

    /**
	 * The resource name constant: 'meta.getFullMetaData'
	 */
    static final String RESOURCE_FULL_META_DATA = "meta.getFullMetaData";

    /**
	 * The resource name constant: 'element.createElement'
	 */
    static final String RESOURCE_CREATE_ELEMENT = "element.createElement";

    /**
	 * The resource name constant: 'element.updateElement'
	 */
    static final String RESOURCE_UPDATE_ELEMENT = "element.updateElement";

    /**
	 * The resource name constant: 'element.removeElement'
	 */
    static final String RESOURCE_REMOVE_ELEMENT = "element.removeElement";

    /**
	 * The resource name constant: 'relation.appendComposition'
	 */
    static final String RESOURCE_APPEND_COMPOSITION = "relation.appendComposition";

    /**
	 * The resource name constant: 'relation.appendComposition'
	 */
    static final String RESOURCE_APPEND_ASSOCIATION = "relation.appendAssociation";

    /**
	 * The resource name constant: 'relation.moveCompositionAppend'
	 */
    static final String RESOURCE_MOVE_COMPOSITION_APPEND = "relation.moveCompositionAppend";

    /**
	 * The resource name constant: 'relation.moveCompositionBefore'
	 */
    static final String RESOURCE_MOVE_COMPOSITION_BEFORE = "relation.moveCompositionBefore";

    /**
	 * The resource name constant: 'relation.moveAssociationBefore'
	 */
    static final String RESOURCE_MOVE_ASSOCIATION_BEFORE = "relation.moveAssociationBefore";

    /**
	 * The resource name constant: 'relation.insertCompositionBefore'
	 */
    static final String RESOURCE_INSERT_COMPOSITION_BEFORE = "relation.insertCompositionBefore";

    /**
	 * The resource name constant: 'relation.insertAssociationBefore'
	 */
    static final String RESOURCE_INSERT_ASSOCIATION_BEFORE = "relation.insertAssociationBefore";

    /**
	 * The resource name constant: 'relation.moveAssociationAppend'
	 */
    static final String RESOURCE_MOVE_ASSOCIATION_APPEND = "relation.moveAssociationAppend";

    /**
	 * The resource name constant: 'relation.removeComposition'
	 */
    static final String RESOURCE_REMOVE_COMPOSITION = "relation.removeComposition";

    /**
	 * The resource name constant: 'relation.removeAssociation'
	 */
    static final String RESOURCE_REMOVE_ASSOCIATION = "relation.removeAssociation";

    /**
	 * The resource name constant: 'element.createSimpleValue'
	 */
    static final String RESOURCE_CREATE_SIMPLE_VALUE = "element.createSimpleValue";

    /**
	 * The resource name constant: 'element.createSimpleValue'
	 */
    static final String RESOURCE_CREATE_UNIQUE_VALUE = "element.createUniqueValue";

    /**
	 * The resource name constant: 'element.createBlobValue'
	 */
    static final String RESOURCE_CREATE_BLOB_VALUE = "element.createBlobValue";

    /**
	 * The resource name constant: 'element.createClobValue'
	 */
    static final String RESOURCE_CREATE_CLOB_VALUE = "element.createClobValue";

    /**
	 * The resource name constant: 'element.updateSimpleValue'
	 */
    static final String RESOURCE_UPDATE_SIMPLE_VALUE = "element.updateSimpleValue";

    /**
	 * The resource name constant: 'element.updateSimpleValue'
	 */
    static final String RESOURCE_UPDATE_UNIQUE_VALUE = "element.updateUniqueValue";

    /**
	 * The resource name constant: 'element.updateClobValue'
	 */
    static final String RESOURCE_UPDATE_CLOB_VALUE = "element.updateClobValue";

    /**
	 * The resource name constant: 'element.updateBlobValue'
	 */
    static final String RESOURCE_UPDATE_BLOB_VALUE = "element.updateBlobValue";

    /**
	 * The resource name constant: 'element.removeSimpleValue'
	 */
    static final String RESOURCE_REMOVE_SIMPLE_VALUE = "element.removeSimpleValue";

    /**
	 * The resource name constant: 'element.removeSimpleValue'
	 */
    static final String RESOURCE_REMOVE_UNIQUE_VALUE = "element.removeUnquieValue";

    /**
	 * The resource name constant: 'element.removeClobValue'
	 */
    static final String RESOURCE_REMOVE_CLOB_VALUE = "element.removeClobValue";

    /**
	 * The resource name constant: 'element.removeBlobValue'
	 */
    static final String RESOURCE_REMOVE_BLOB_VALUE = "element.removeBlobValue";

    /**
	 * The resource name constant: 'element.removeElementRecursive'
	 */
    static final String RESOURCE_REMOVE_ELEMENT_RECURSIVE = "element.removeElementRecursive";

    /**
	 * The resource name constant: 'element.cloneElementAppend'
	 */
    static final String RESOURCE_CLONE_ELEMENT_APPEND = "element.cloneElementAppend";

    /**
	 * The resource name constant: 'element.cloneElementAppend'
	 */
    static final String RESOURCE_CLONE_ELEMENT_APPEND_EXCLUDED = "element.cloneElementAppendExclude";

    /**
	 * The resource name constant: 'element.cloneElementBefore'
	 */
    static final String RESOURCE_CLONE_ELEMENT_BEFORE = "element.cloneElementBefore";

    /**
	 * The resource name constant: 'locking.getLockerKey'
	 */
    static final String RESOURCE_GET_LOCKER_KEY = "locking.getLockerKey";

    /**
	 * The resource name constant: 'locking.createLock'
	 */
    static final String RESOURCE_CREATE_LOCK = "locking.createLock";

    /**
     * The resource name constant: 'locking.createLock'
     */
    static final String RESOURCE_UPDATE_LOCKED_ELEMENT = "locking.updateLockTimeById";

    /**
	 * The resource name constant: 'locking.removeLockByKey'
	 */
    static final String RESOURCE_REMOVE_LOCK_BY_KEY = "locking.removeLockByKey";

    /**
	 * The resource name constant: 'locking.removeLockById'
	 */
    static final String RESOURCE_REMOVE_LOCK_BY_ID = "locking.removeLockById";

    /**
	 * The resource name constant: 'locking.isLocked'
	 */
    static final String RESOURCE_IS_LOCKED = "locking.isLocked";

    /**
	 * The resource name constant: 'rights.loadAll'
	 */
    static final String RESOURCE_RIGHTS_LOAD_ALL = "rights.loadAll";

    /**
	 * The resource name constant: 'rights.loadSingle'
	 */
    static final String RESOURCE_RIGHTS_LOAD_SINGLE = "rights.loadSingle";

    /**
	 * The resource name constant: 'rights.loadArrayAll'
	 */
    static final String RESOURCE_RIGHTS_LOAD_ARRAY_ALL = "rights.loadArrayAll";

    /**
	 * The resource name constant: 'rights.loadArraySome'
	 */
    static final String RESOURCE_RIGHTS_LOAD_ARRAY_SOME = "rights.loadArraySome";

    /**
	 * The resource name constant: 'authorisation.createAuthorisation'
	 */
    static final String RESOURCE_CREATE_AUTHORISATION = "authorisation.createAuthorisation";

    /**
	 * The resource name constant: 'authorisation.updateAuthorisation'
	 */
    static final String RESOURCE_UPDATE_AUTHORISATION = "authorisation.updateAuthorisation";

    /**
	 * The resource name constant: 'authorisation.removeAuthorisation'
	 */
    static final String RESOURCE_REMOVE_AUTHORISATION = "authorisation.removeAuthorisation";

    /**
	 * The resource name constant: 'rights.getOwner'
	 */
    static final String RESOURCE_RIGHTS_GET_OWNER = "rights.getOwner";

    /**
	 * The resource name constant: 'rights.getArrayOwner'
	 */
    static final String RESOURCE_RIGHTS_GET_ARRAY_OWNER = "rights.getArrayOwner";

    /**
     * The resource name constant: 'relation.setCompositionKey'
     */
    static final String RESOURCE_SET_COMPOSITION_KEY = "relation.setCompositionKey";

    /**
     * The resource name constant: 'relation.setCompositionKey'
     */
    static final String RESOURCE_SET_ASSOCIATION_KEY = "relation.setAssociationKey";

    /**
     * The resource name constant: 'rights.loadArrayAll'
     */
    static final String RESOURCE_GET_DATES = "element.getDates";

    /**
	 * Creates the new element in db
	 * 
	 * @param element - element to create in the db
	 * 
	 * @return String - the id of the newly created element
	 */
    private static String createElement(OMSMetaData metaData, DataSource dataSource, OMSElement element, int userId) throws Exception {
        try {
            if (element.getDisplayName() == null) {
                element.setDisplayName(element.getBookmark());
            }
            Integer id = null;
            Scheme s = metaData.getScheme(element.getNamespacePath(), element.getSchemeName());
            if (s == null) {
                throw new Exception("no scheme for: " + element.getNamespacePath() + "/" + element.getSchemeName());
            }
            BoundVariable[] vars = new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_string_display_name", element.getDisplayName()), new BoundVariable("in_string_bookmark", element.getBookmark()), new BoundVariable("in_int_schema_id", s.getId()), new BoundVariable("in_int_creater_id", new Integer(userId)) };
            CallResult result = executeOnResource(dataSource, null, vars, RESOURCE_CREATE_ELEMENT);
            element.setId(result.getBoundVariable("out_int_id").getValue().toString());
            setDatesForDB(dataSource, element);
            return element.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while creating element: " + getElementDescription(element) + " : " + t.getMessage(), t);
        }
    }

    private static void setDatesForDB(DataSource dataSource, OMSElement element) throws Exception {
        RecordSet recordSet = DBHelper.getDates(dataSource, element);
        if (recordSet.getRecordsCount() > 0) {
            Record[] records = recordSet.getRecords();
            for (int i = 0; i < recordSet.getRecordsCount(); i++) {
                Date creatingDate = RecordsetConverter.convertObjectToDate(records[i].getValues()[0]);
                Date modifyDate = RecordsetConverter.convertObjectToDate(records[i].getValues()[1]);
                element.modifyingDate = modifyDate;
                element.creatingDate = creatingDate;
            }
        }
    }

    /**
	 * Appends the given composition to db
	 * 
	 * @param composition - composition to append to db
	 * 
	 * @return String - the id of the newly created composition
	 */
    private static String appendComposition(OMSMetaData metaData, DataSource dataSource, Relation composition, int userId) throws Exception {
        try {
            ComplexAttribute cat = metaData.getComplexAttributeFromHierarchy(metaData.getScheme(composition.getSourceElement().getNamespacePath(), composition.getSourceElement().getSchemeName()), composition.getComplexAttribute());
            Integer id = null;
            BoundVariable[] vars = new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_source_id", composition.getSourceElementId()), new BoundVariable("in_int_cat_id", cat.getId()), new BoundVariable("in_int_target_id", composition.getTargetElement().getId()), new BoundVariable("in_string_key", composition.getName()), new BoundVariable("in_int_creater_id", new Integer(userId)) };
            CallResult result = executeOnResource(dataSource, null, vars, RESOURCE_APPEND_COMPOSITION);
            composition.setId(result.getBoundVariable("out_int_id").getValue().toString());
            composition.setComplexAttributeId(cat.getId());
            return composition.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while appending composition: " + getCompositionDescription(composition, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Inserts the given composition to db
	 * 
	 * @param composition - composition to insert to db
	 * 
	 * @return String - the id of the newly inserted composition
	 */
    private static String insertCompositionBefore(OMSMetaData metaData, DataSource dataSource, Relation composition, int userId) throws Exception {
        try {
            Integer id = null;
            BoundVariable[] vars = new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_elm_id", composition.getTargetElement().getId()), new BoundVariable("in_int_before_cmp_id", composition.getNextRelation().getId()), new BoundVariable("in_string_key", composition.getName()), new BoundVariable("in_int_creater_id", new Integer(userId)) };
            CallResult result = executeOnResource(dataSource, null, vars, RESOURCE_INSERT_COMPOSITION_BEFORE);
            composition.setId(result.getBoundVariable("out_int_id").getValue().toString());
            composition.setComplexAttributeId(composition.getNextRelation().getComplexAttributeId());
            return composition.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while inserting composition before: " + getCompositionDescription(composition, metaData) + "; next composition: " + getCompositionDescription(composition.getNextRelation(), metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Appends the given association to db
	 * 
	 * @param association - association to append to db
	 * 
	 * @return String - the id of the newly created association
	 */
    private static String appendAssociation(OMSMetaData metaData, DataSource dataSource, Relation association, int userId) throws Exception {
        try {
            ComplexAttribute cat = metaData.getComplexAttributeFromHierarchy(metaData.getScheme(association.getSourceElement().getNamespacePath(), association.getSourceElement().getSchemeName()), association.getComplexAttribute());
            Integer id = null;
            BoundVariable[] vars = new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_source_id", association.getSourceElementId()), new BoundVariable("in_int_cat_id", cat.getId()), new BoundVariable("in_int_target_id", association.getTargetElement().getId()), new BoundVariable("in_string_key", association.getName()), new BoundVariable("in_int_creater_id", new Integer(userId)) };
            CallResult result = executeOnResource(dataSource, null, vars, RESOURCE_APPEND_ASSOCIATION);
            association.setId(result.getBoundVariable("out_int_id").getValue().toString());
            association.setComplexAttributeId(cat.getId());
            return association.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while appending association: " + getAssociationDescription(association, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Inserts the given association to db
	 * 
	 * @param association - association to insert to db
	 * 
	 * @return String - the id of the newly inserted association
	 */
    private static String insertAssociationBefore(OMSMetaData metaData, DataSource dataSource, Relation association, int userId) throws Exception {
        try {
            Integer id = null;
            BoundVariable[] vars = new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_elm_id", association.getTargetElement().getId()), new BoundVariable("in_int_before_ass_id", association.getNextRelation().getId()), new BoundVariable("in_string_key", association.getName()), new BoundVariable("in_int_creater_id", new Integer(userId)) };
            CallResult result = executeOnResource(dataSource, null, vars, RESOURCE_INSERT_ASSOCIATION_BEFORE);
            association.setId(result.getBoundVariable("out_int_id").getValue().toString());
            association.setComplexAttributeId(association.getNextRelation().getComplexAttributeId());
            return association.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while inserting association before: " + getAssociationDescription(association, metaData) + "; next relation: " + getAssociationDescription(association.getNextRelation(), metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Creates the new simple value in db
	 * 
	 * @param simpleValue - simple value to create in the db
	 * 
	 * @return String - the id of the newly created simple value
	 */
    private static String createSimpleValue(OMSMetaData metaData, DataSource dataSource, Value value, int userId) throws Exception {
        try {
            Integer id = null;
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_string_content", value.getContentAsString()), new BoundVariable("in_int_elm_id", new Integer(value.getElement().getId())), new BoundVariable("in_int_sat_id", new Integer(metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getId())), new BoundVariable("in_int_dov_id", new Integer(metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getDatatype().getDatatypeEnumerationByName(value.getDatatypeEnumeration()).getId())), new BoundVariable("in_int_creater_id", new Integer(userId)) }, RESOURCE_CREATE_SIMPLE_VALUE);
            value.setId(result.getBoundVariable("out_int_id").getValue().toString());
            return value.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while creating simple value: " + getValueDescription(value, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Creates the new unique value in db
	 * 
	 * @param uniqueValue - simple value to create in the db
	 * 
	 * @return String - the id of the newly created simple value
	 */
    private static String createUniqueValue(OMSMetaData metaData, DataSource dataSource, Value value, int userId) throws Exception {
        try {
            Integer id = null;
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_string_content", value.getContentAsString()), new BoundVariable("in_int_elm_id", new Integer(value.getElement().getId())), new BoundVariable("in_int_sat_id", new Integer(metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getId())), new BoundVariable("in_int_dov_id", new Integer(metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getDatatype().getDatatypeEnumerationByName(value.getDatatypeEnumeration()).getId())), new BoundVariable("in_int_creater_id", new Integer(userId)) }, RESOURCE_CREATE_UNIQUE_VALUE);
            value.setId(result.getBoundVariable("out_int_id").getValue().toString());
            return value.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while creating unique value: " + getValueDescription(value, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Creates the new clob value in db
	 * 
	 * @param clobValue - clob value to create in the db
	 * 
	 * @return String - the id of the newly created clob value
	 */
    private static String createClobValue(OMSMetaData metaData, DataSource dataSource, Value value, int userId) throws Exception {
        try {
            Integer id = null;
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_clob_content", value.getContentAsString()), new BoundVariable("in_int_elm_id", new Integer(value.getElement().getId())), new BoundVariable("in_int_sat_id", new Integer(metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getId())), new BoundVariable("in_int_dov_id", new Integer(metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getDatatype().getDatatypeEnumerationByName(value.getDatatypeEnumeration()).getId())), new BoundVariable("in_int_creater_id", new Integer(userId)) }, RESOURCE_CREATE_CLOB_VALUE);
            value.setId(result.getBoundVariable("out_int_id").getValue().toString());
            return value.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while creating clob value: " + getValueDescription(value, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Creates the new blob value in db
	 * 
	 * @param blobValue - blob value to create in the db
	 * 
	 * @return String - the id of the newly created blob value
	 */
    private static String createBlobValue(OMSMetaData metaData, DataSource dataSource, Value value, int userId) throws Exception {
        try {
            Integer id = null;
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_blob_content", value.getContentAsByteArray()), new BoundVariable("in_int_elm_id", new Integer(value.getElement().getId())), new BoundVariable("in_int_sat_id", new Integer(metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getId())), new BoundVariable("in_int_dov_id", new Integer(metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getDatatype().getDatatypeEnumerationByName(value.getDatatypeEnumeration()).getId())), new BoundVariable("in_int_creater_id", new Integer(userId)) }, RESOURCE_CREATE_BLOB_VALUE);
            value.setId(result.getBoundVariable("out_int_id").getValue().toString());
            return value.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while creating blob value: " + getValueDescription(value, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Moves the composition in db
	 * 
	 * @param composition - composition to move into db
	 */
    private static void moveCompositionAppend(OMSMetaData metaData, DataSource dataSource, Relation composition, int userId) throws Exception {
        try {
            ComplexAttribute cat = metaData.getComplexAttributeFromHierarchy(metaData.getScheme(composition.getSourceElement().getNamespacePath(), composition.getSourceElement().getSchemeName()), composition.getComplexAttribute());
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("in_int_cmp_id", new Integer(composition.getId())), new BoundVariable("in_int_elm_id", new Integer(composition.getSourceElementId())), new BoundVariable("in_int_cat_id", new Integer(cat.getId())), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_MOVE_COMPOSITION_APPEND);
            composition.setComplexAttributeId(cat.getId());
        } catch (Throwable t) {
            throw new Exception("Exception while moving composition append: " + getAssociationDescription(composition, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Moves the composition in db
	 * 
	 * @param composition - composition to move into db
	 */
    private static void moveCompositionBefore(OMSMetaData metaData, DataSource dataSource, Relation composition, int userId) throws Exception {
        try {
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("in_int_cmp_id", new Integer(composition.getId())), new BoundVariable("in_int_before_cmp_id", new Integer(composition.getNextRelation().getId())), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_MOVE_COMPOSITION_BEFORE);
            composition.setComplexAttributeId(composition.getNextRelation().getComplexAttributeId());
        } catch (Throwable t) {
            throw new Exception("Exception while moving composition before: " + getCompositionDescription(composition, metaData) + "; next composition: " + getCompositionDescription(composition.getNextRelation(), metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Moves the association in db
	 * 
	 * @param composition - composition to move into db
	 */
    private static void moveAssociationAppend(OMSMetaData metaData, DataSource dataSource, Relation association, int userId) throws Exception {
        try {
            ComplexAttribute cat = metaData.getComplexAttributeFromHierarchy(metaData.getScheme(association.getSourceElement().getNamespacePath(), association.getSourceElement().getSchemeName()), association.getComplexAttribute());
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("in_int_ass_id", new Integer(association.getId())), new BoundVariable("in_int_elm_id", new Integer(association.getSourceElementId())), new BoundVariable("in_int_cat_id", new Integer(cat.getId())), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_MOVE_ASSOCIATION_APPEND);
            association.setComplexAttributeId(cat.getId());
        } catch (Throwable t) {
            throw new Exception("Exception while moving association append: " + getAssociationDescription(association, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Moves the association in db
	 * 
	 * @param composition - composition to move into db
	 */
    private static void moveAssociationBefore(OMSMetaData metaData, DataSource dataSource, Relation association, int userId) throws Exception {
        try {
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("in_int_ass_id", new Integer(association.getId())), new BoundVariable("in_int_before_ass_id", new Integer(association.getNextRelation().getId())), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_MOVE_ASSOCIATION_BEFORE);
            association.setComplexAttributeId(association.getNextRelation().getComplexAttributeId());
        } catch (Throwable t) {
            throw new Exception("Exception while moving association before: " + getAssociationDescription(association, metaData) + "; next association: " + getAssociationDescription(association.getNextRelation(), metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Updates the simple value in db
	 * 
	 * @param simpleValue - simple value to update in the db
	 * 
	 * @return String - the id of the updated simple value
	 */
    private static String updateSimpleValue(OMSMetaData metaData, DataSource dataSource, Value simpleValue, int userId) throws Exception {
        try {
            Integer id = null;
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_id", new Integer(simpleValue.getId())), new BoundVariable("in_string_content", simpleValue.getContentAsString()), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_UPDATE_SIMPLE_VALUE);
            return simpleValue.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while updating simple value: " + getValueDescription(simpleValue, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Updates the simple value in db
	 * 
	 * @param simpleValue - simple value to update in the db
	 * 
	 * @return String - the id of the updated simple value
	 */
    private static String updateUniqueValue(OMSMetaData metaData, DataSource dataSource, Value uniqueValue, int userId) throws Exception {
        try {
            Integer id = null;
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_id", new Integer(uniqueValue.getId())), new BoundVariable("in_string_content", uniqueValue.getContentAsString()), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_UPDATE_UNIQUE_VALUE);
            return uniqueValue.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while updating unique value: " + getValueDescription(uniqueValue, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Updates the clob value in db
	 * 
	 * @param clobValue - clob value to update in the db
	 * 
	 * @return String - the id of the updated clob value
	 */
    private static String updateClobValue(OMSMetaData metaData, DataSource dataSource, Value clobValue, int userId) throws Exception {
        try {
            Integer id = null;
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_id", new Integer(clobValue.getId())), new BoundVariable("in_clob_content", clobValue.getContent()), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_UPDATE_CLOB_VALUE);
            return clobValue.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while updating clob value: " + getValueDescription(clobValue, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Updates the blob value in db
	 * 
	 * @param blobValue - blob value to update in the db
	 * 
	 * @return String - the id of the updated blob value
	 */
    private static String updateBlobValue(OMSMetaData metaData, DataSource dataSource, Value blobValue, int userId) throws Exception {
        try {
            Integer id = null;
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_id", new Integer(blobValue.getId())), new BoundVariable("in_blob_content", blobValue.getContent()), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_UPDATE_BLOB_VALUE);
            return blobValue.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while updating blob value: " + getValueDescription(blobValue, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Removes the simple value from db
	 * 
	 * @param simpleValue - simple value to delete from db
	 * 
	 * @return String - the id of the deleted simple value
	 */
    private static String removeSimpleValue(OMSMetaData metaData, DataSource dataSource, Value simpleValue, int userId) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_id", new Integer(simpleValue.getId())) }, RESOURCE_REMOVE_SIMPLE_VALUE);
            return ((Integer) result.getBoundVariable("out_int_id").getValue()).toString();
        } catch (Throwable t) {
            throw new Exception("Exception while removing simple value: " + getValueDescription(simpleValue, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Removes the simple value from db
	 * 
	 * @param uniqueValue - simple value to delete from db
	 * 
	 * @return String - the id of the deleted simple value
	 */
    private static String removeUniqueValue(OMSMetaData metaData, DataSource dataSource, Value uniqueValue, int userId) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_id", new Integer(uniqueValue.getId())) }, RESOURCE_REMOVE_UNIQUE_VALUE);
            return ((Integer) result.getBoundVariable("out_int_id").getValue()).toString();
        } catch (Throwable t) {
            throw new Exception("Exception while removing unique value: " + getValueDescription(uniqueValue, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Removes the clob value from db
	 * 
	 * @param clobValue - clob value to delete from db
	 * 
	 * @return String - the id of the deleted clob value
	 */
    private static String removeClobValue(OMSMetaData metaData, DataSource dataSource, Value clobValue, int userId) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_id", new Integer(clobValue.getId())) }, RESOURCE_REMOVE_CLOB_VALUE);
            return ((Integer) result.getBoundVariable("out_int_id").getValue()).toString();
        } catch (Throwable t) {
            throw new Exception("Exception while removing clob value: " + getValueDescription(clobValue, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Removes the blob value from db
	 * 
	 * @param blobValue - blob value to delete from db
	 * 
	 * @return String - the id of the deleted blob value
	 */
    private static String removeBlobValue(OMSMetaData metaData, DataSource dataSource, Value blobValue, int userId) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_id", new Integer(blobValue.getId())) }, RESOURCE_REMOVE_BLOB_VALUE);
            return ((Integer) result.getBoundVariable("out_int_id").getValue()).toString();
        } catch (Throwable t) {
            throw new Exception("Exception while removing blob value: " + getValueDescription(blobValue, metaData) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Updates the given element in db
	 * 
	 * @param element - element to update in the db
	 * 
	 * @return String - the id of the updated element
	 */
    private static String updateElement(DataSource dataSource, OMSElement element, int userId) throws Exception {
        try {
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_id", new Integer(element.getId())), new BoundVariable("in_string_display_name", element.getDisplayName()), new BoundVariable("in_string_bookmark", element.getBookmark()), new BoundVariable("in_int_modifier_id", new Integer(userId)) }, RESOURCE_UPDATE_ELEMENT);
            setDatesForDB(dataSource, element);
            return element.getId();
        } catch (Throwable t) {
            throw new Exception("Exception while updating element: " + getElementDescription(element) + " : " + t.getMessage(), t);
        }
    }

    /**
     * Set new Key for Relation
     * @param dataSource
     * @param relation
     * @param newKey
     * @throws Exception
	 */
    private static void setKey(DataSource dataSource, Relation relation) throws Exception {
        String resource = relation.isComposition() ? RESOURCE_SET_COMPOSITION_KEY : RESOURCE_SET_ASSOCIATION_KEY;
        try {
            executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("in_int_rel_id", new Integer(relation.getId())), new BoundVariable("in_string_new_key", relation.getName()) }, resource);
        } catch (Throwable t) {
            throw new Exception("Exception while setRelationKey: " + relation.id + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Removes the given element from db
	 * 
	 * @param element - element to remove from the db
	 * 
	 * @return String - the id of the removed element
	 */
    private static String removeElement(DataSource dataSource, OMSElement element) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_id", new Integer(element.getId())) }, RESOURCE_REMOVE_ELEMENT);
            return ((Integer) result.getBoundVariable("out_int_id").getValue()).toString();
        } catch (Throwable t) {
            throw new Exception("Exception while removing element " + getElementDescription(element) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Removes the given composition from db
	 * 
	 * @param relation - composition to remove from the db
	 * 
	 * @return String - the id of the removed composition
	 */
    private static String removeComposition(OMSMetaData metaData, DataSource dataSource, Relation relation) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_id", new Integer(relation.getId())) }, RESOURCE_REMOVE_COMPOSITION);
            return ((Integer) result.getBoundVariable("out_int_id").getValue()).toString();
        } catch (Throwable t) {
            throw new Exception("Exception while removing composition: " + getAssociationDescription(relation, metaData) + " : " + t.getMessage(), t);
        }
    }

    static RecordSet selectFullMetaData(DataSource dataSource) throws Exception {
        return executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_cursor_meta_data", null) }, RESOURCE_FULL_META_DATA).getRecordSets()[0];
    }

    static RecordSet selectTreeByWhere(DataSource dataSource, String whereStr) throws Exception {
        HashMap placeHolders = new HashMap();
        HashMap bindVariables = new HashMap();
        placeHolders.put(TOKEN_WHERE, whereStr);
        return executeOnResource(dataSource, placeHolders, new BoundVariable[] { new BoundVariable("out_cursor_tree", null) }, RESOURCE_OMS_STRUCTURE_BY_WHERE).getRecordSets()[0];
    }

    /**
	 * Removes the given association from db
	 * 
	 * @param relation - association to remove from the db
	 * 
	 * @return String - the id of the removed association
	 */
    private static String removeAssociation(OMSMetaData metaData, DataSource dataSource, Relation association) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_id", new Integer(association.getId())) }, RESOURCE_REMOVE_ASSOCIATION);
            return ((Integer) result.getBoundVariable("out_int_id").getValue()).toString();
        } catch (Throwable t) {
            throw new Exception("Exception while removing association: " + getAssociationDescription(association, metaData) + " : " + t.getMessage(), t);
        }
    }

    static CallResult executeOnApplicationResource(DataSource dataSource, HashMap placeHolders, BoundVariable[] bindVariables, String path, String resource) throws Exception {
        try {
            return executeStatement(dataSource, placeHolders, bindVariables, ResourceHelper.loadApplicationResource(path, resource), path, resource);
        } catch (FileNotFoundException e) {
            throw new OMSResourceException("Resource '" + resource + "' not found in '" + path + "'", OMSResourceException.CODE_FILE_NOT_FOUND, e);
        } catch (IOException e) {
            throw new OMSResourceException("Exception while reading resource '" + resource + "' not found in '" + path + "'", OMSResourceException.CODE_IO, e);
        }
    }

    static CallResult executeOnResource(DataSource dataSource, HashMap placeHolders, BoundVariable[] bindVariables, String resource) throws Exception, OMSResourceException {
        return executeStatement(dataSource, placeHolders, bindVariables, ResourceHelper.loadResource(resource, dataSource.getType()), ResourceHelper.getNamespace(resource, dataSource.getType()), ResourceHelper.getResourceFileName(resource));
    }

    static CallResult executeStatement(DataSource dataSource, HashMap placeHolders, BoundVariable[] bindVariables, String statement, String namespace, String resourceKey) throws Exception {
        try {
            if (placeHolders != null && placeHolders.size() > 0) {
                TemplateParser parser = new TemplateParser(new StringBuffer(statement));
                Iterator it = placeHolders.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String value = (String) placeHolders.get(key);
                    parser.addToken(key, new String[] { value }, TOKEN_START, TOKEN_END);
                }
                statement = parser.parse();
            }
            return dataSource.executeCall(statement, bindVariables, namespace, resourceKey);
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos);
            e.printStackTrace(new PrintWriter(baos));
            pw.close();
            e.printStackTrace();
            throw new Exception("Exception thrown while executing statement: '" + statement + "': " + e.getMessage(), e);
        }
    }

    /**
	 * Saves the association into dataSource
	 * 
	 * @param association - association to save
	 * @param ids - the map for new ids
	 * 
	 * @throws SQLException
	 * @throws OMSResourceException
	 */
    static void saveAssociation(OMSMetaData metaData, DataSource connection, Relation association, int userId) throws Exception {
        if (association.getTargetElement().isNew() && association.getSourceElement().getOMSStructure() != association.getTargetElement().getOMSStructure()) {
            throw new OMSStructureException("Exception while saving association: " + getAssociationDescription(association, metaData) + " : Not saved association target element from external oms structure");
        }
        if (association.getSourceElement().isNew()) {
            createElement(metaData, connection, association.getSourceElement(), userId);
        }
        if (association.getTargetElement().isNew()) {
            createElement(metaData, connection, association.getTargetElement(), userId);
        }
        if (association.isNew()) {
            if (association.isOrdered() && association.getNextRelation() != null) {
                if (association.getNextRelation().isNew() || association.getNextRelation().isUpdated()) {
                    saveAssociation(metaData, connection, association.getNextRelation(), userId);
                }
                insertAssociationBefore(metaData, connection, association, userId);
            } else {
                appendAssociation(metaData, connection, association, userId);
            }
        } else if (association.isUpdated()) {
            if (association.isOrdered() && association.getNextRelation() != null) {
                if (association.getNextRelation().isNew() || association.getNextRelation().isUpdated()) {
                    saveAssociation(metaData, connection, association.getNextRelation(), userId);
                }
                moveAssociationBefore(metaData, connection, association, userId);
            } else {
                moveAssociationAppend(metaData, connection, association, userId);
            }
        } else if (association.isRemoved()) {
            removeAssociation(metaData, connection, association);
        }
        if (!association.isRemoved()) {
            association.setState(OMSNode.STATE_NORMAL);
        }
    }

    /**
	 * Saves the given rights into the database
	 * 
	 * @param connection 
	 * @param rights
	 * @param owner
	 * @param userId
	 * 
	 * @throws Exception
	 */
    static void saveRights(DataSource connection, OMSRights rights, OMSElement grantedFor, int userId) throws Exception {
        if ((rights.isNew())) {
            DBHelper.createAuthorisation(connection, rights, grantedFor, userId);
        } else if (rights.isUpdated()) {
            DBHelper.updateAuthorisation(connection, rights, userId);
        } else if (rights.isRemoved()) {
            DBHelper.removeAuthorisation(connection, rights);
        }
        if (!rights.isRemoved()) {
            rights.setState(OMSNode.STATE_NORMAL);
        }
    }

    private static boolean isBlob(Value value, OMSMetaData metaData) throws Exception {
        Scheme scheme = metaData.getNamespaceByPath(value.getElement().getNamespacePath()).getSchemeByName(value.getElement().getSchemeName());
        SimpleAttribute sat = metaData.getSimpleAttributeFromHierarchy(scheme, value.getSimpleAttribute());
        return sat.getDatatype().isBlob();
    }

    private static boolean isClob(Value value, OMSMetaData metaData) throws Exception {
        Scheme scheme = metaData.getNamespaceByPath(value.getElement().getNamespacePath()).getSchemeByName(value.getElement().getSchemeName());
        SimpleAttribute sat = metaData.getSimpleAttributeFromHierarchy(scheme, value.getSimpleAttribute());
        return sat.getDatatype().isClob();
    }

    private static boolean isUnique(Value value, OMSMetaData metaData) throws Exception {
        Scheme scheme = metaData.getNamespaceByPath(value.getElement().getNamespacePath()).getSchemeByName(value.getElement().getSchemeName());
        SimpleAttribute sat = metaData.getSimpleAttributeFromHierarchy(scheme, value.getSimpleAttribute());
        return sat.getDatatype().getIsUnique();
    }

    /**
	 * Saves the composition into dataSource
	 * 
	 * @param composition - composition to save
	 * 
	 * @throws SQLException
	 * @throws OMSResourceException
	 */
    static void saveComposition(OMSMetaData metaData, DataSource connection, Relation composition, int userId) throws Exception {
        if (composition.getSourceElement().isNew()) {
            createElement(metaData, connection, composition.getSourceElement(), userId);
        }
        if (composition.getTargetElement().isNew()) {
            createElement(metaData, connection, composition.getTargetElement(), userId);
        }
        if (composition.isNew()) {
            if (composition.isOrdered() && composition.getNextRelation() != null) {
                if (composition.getNextRelation().isNew() || composition.getNextRelation().isUpdated()) {
                    saveComposition(metaData, connection, composition.getNextRelation(), userId);
                }
                insertCompositionBefore(metaData, connection, composition, userId);
            } else {
                appendComposition(metaData, connection, composition, userId);
            }
        } else if (composition.isUpdated()) {
            if (composition.isOrdered() && composition.getNextRelation() != null) {
                if (composition.getNextRelation().isNew() || composition.getNextRelation().isUpdated()) {
                    saveComposition(metaData, connection, composition.getNextRelation(), userId);
                }
                moveCompositionBefore(metaData, connection, composition, userId);
            } else {
                moveCompositionAppend(metaData, connection, composition, userId);
            }
        } else if (composition.isRemoved()) {
            DBHelper.removeComposition(metaData, connection, composition);
        }
        if (!composition.isRemoved()) {
            composition.setState(OMSNode.STATE_NORMAL);
        }
    }

    static String cloneElementAppend(DataSource dataSource, OMSMetaData metaData, OMSElement elementToClone, OMSElement targetElement, String targetAttribute, String key, boolean recursive, boolean withAssociations, int userId) throws Exception {
        try {
            Integer id = null;
            BoundVariable[] vars = new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_elm_id", elementToClone.getId()), new BoundVariable("in_int_target_elm_id", targetElement.getId()), new BoundVariable("in_int_target_cat_id", metaData.getComplexAttributeFromHierarchy(metaData.getScheme(targetElement.getNamespacePath(), targetElement.getSchemeName()), targetAttribute).getId()), new BoundVariable("in_string_target_key", key), new BoundVariable("in_int_recursive", new Integer(recursive ? 1 : 0)), new BoundVariable("in_int_clone_associations", new Integer(withAssociations ? 1 : 0)), new BoundVariable("in_int_creater_id", new Integer(userId)) };
            CallResult result = executeOnResource(dataSource, null, vars, RESOURCE_CLONE_ELEMENT_APPEND);
            return result.getBoundVariable("out_int_id").getValue().toString();
        } catch (Throwable t) {
            throw new Exception("Exception while cloning element: " + getElementDescription(elementToClone) + " : " + t.getMessage(), t);
        }
    }

    static String cloneElementAppend(DataSource dataSource, OMSMetaData metaData, OMSElement elementToClone, OMSElement targetElement, String targetAttribute, String key, boolean recursive, boolean withAssociations, int userId, String excludedComplexAttributeId) throws Exception {
        try {
            Integer id = null;
            BoundVariable[] vars = new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_elm_id", elementToClone.getId()), new BoundVariable("in_int_target_elm_id", targetElement.getId()), new BoundVariable("in_int_target_cat_id", metaData.getComplexAttributeFromHierarchy(metaData.getScheme(targetElement.getNamespacePath(), targetElement.getSchemeName()), targetAttribute).getId()), new BoundVariable("in_string_target_key", key), new BoundVariable("in_int_recursive", new Integer(recursive ? 1 : 0)), new BoundVariable("in_int_clone_associations", new Integer(withAssociations ? 1 : 0)), new BoundVariable("in_int_excludeCat_id", excludedComplexAttributeId), new BoundVariable("in_int_creater_id", new Integer(userId)) };
            CallResult result = executeOnResource(dataSource, null, vars, RESOURCE_CLONE_ELEMENT_APPEND_EXCLUDED);
            return result.getBoundVariable("out_int_id").getValue().toString();
        } catch (Throwable t) {
            throw new Exception("Exception while cloning element: " + getElementDescription(elementToClone) + " : " + t.getMessage(), t);
        }
    }

    static String cloneElementBefore(DataSource dataSource, OMSMetaData metaData, OMSElement elementToClone, OMSElement targetElementBefore, String key, boolean recursive, boolean withAssociations, int userId) throws Exception {
        try {
            Integer id = null;
            BoundVariable[] vars = new BoundVariable[] { new BoundVariable("out_int_id", id), new BoundVariable("in_int_elm_id", elementToClone.getId()), new BoundVariable("in_int_target_before_elm_id", targetElementBefore.getId()), new BoundVariable("in_string_target_key", key), new BoundVariable("in_int_recursive", new Integer(recursive ? 1 : 0)), new BoundVariable("in_int_clone_associations", new Integer(withAssociations ? 1 : 0)), new BoundVariable("in_int_creater_id", new Integer(userId)) };
            CallResult result = executeOnResource(dataSource, null, vars, RESOURCE_CLONE_ELEMENT_BEFORE);
            return result.getBoundVariable("out_int_id").getValue().toString();
        } catch (Throwable t) {
            throw new Exception("Exception while cloning element: " + getElementDescription(elementToClone) + " : " + t.getMessage(), t);
        }
    }

    /**
	 * Performs the recursive remove of the given element in the datatabase
	 * 
	 * @param dataSource
	 * @param element
	 * @param includeElement
	 * @param removeAssociations
	 * 
	 * @return The remove result
	 * 
	 * @throws Exception
	 */
    static Integer removeElementRecursive(DataSource dataSource, OMSElement element, boolean includeElement, boolean removeAssociations) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_result", null), new BoundVariable("in_int_elm_id", new Integer(element.getId())), new BoundVariable("in_int_include_self", new Integer(includeElement ? 1 : 0)), new BoundVariable("in_remove_associations", new Integer(removeAssociations ? 1 : 0)) }, RESOURCE_REMOVE_ELEMENT_RECURSIVE);
            return (Integer) result.getBoundVariable("out_int_result").getValue();
        } catch (Throwable t) {
            throw new Exception("Exception while recursive removing element " + getElementDescription(element) + " : " + t.getMessage(), t);
        }
    }

    static RecordSet loadRights(DataSource dataSource, OMSElement grantedFor, OMSElement grantedTo) throws Exception {
        BoundVariable[] bindVariables = null;
        String resource = null;
        if (grantedTo == null) {
            bindVariables = new BoundVariable[] { new BoundVariable("out_cursor_rights", null), new BoundVariable("in_int_elm_id", new Integer(grantedFor.getId())) };
            resource = RESOURCE_RIGHTS_LOAD_ALL;
        } else {
            bindVariables = new BoundVariable[] { new BoundVariable("out_cursor_rights", null), new BoundVariable("in_int_elm_id", new Integer(grantedFor.getId())), new BoundVariable("in_int_owner_id", new Integer(grantedTo.getId())) };
            resource = RESOURCE_RIGHTS_LOAD_SINGLE;
        }
        return executeOnResource(dataSource, null, bindVariables, resource).getRecordSets()[0];
    }

    static RecordSet loadRights(DataSource dataSource, OMSElement[] grantedFor, OMSElement[] grantedTo) throws Exception {
        BoundVariable[] bindVariables = null;
        String resource = null;
        HashMap placeHolders = new HashMap();
        StringBuffer sbIdsFor = new StringBuffer();
        for (int i = 0; i < grantedFor.length; i++) {
            if (i > 0) {
                sbIdsFor.append(',');
            }
            sbIdsFor.append(grantedFor[i].getId());
        }
        bindVariables = new BoundVariable[] { new BoundVariable("out_cursor_rights", null) };
        placeHolders.put("in_int_elm_id_list", sbIdsFor.toString());
        if (grantedTo == null || grantedTo.length == 0) {
            resource = RESOURCE_RIGHTS_LOAD_ARRAY_ALL;
        } else {
            StringBuffer sbIdsTo = new StringBuffer();
            for (int i = 0; i < grantedTo.length; i++) {
                if (i > 0) {
                    sbIdsTo.append(',');
                }
                sbIdsTo.append(grantedTo[i].getId());
            }
            placeHolders.put("in_int_owner_id_list", sbIdsTo.toString());
            resource = RESOURCE_RIGHTS_LOAD_ARRAY_SOME;
        }
        return executeOnResource(dataSource, placeHolders, bindVariables, resource).getRecordSets()[0];
    }

    static RecordSet getDates(DataSource dataSource, OMSElement element) throws Exception {
        BoundVariable[] bindVariables = null;
        String resource = null;
        bindVariables = new BoundVariable[] { new BoundVariable("out_cursor_result", null), new BoundVariable("in_int_elm_id", element.getId()) };
        resource = RESOURCE_GET_DATES;
        return executeOnResource(dataSource, null, bindVariables, resource).getRecordSets()[0];
    }

    static RecordSet selectRightsOwners(DataSource dataSource, OMSElement rightsTo) throws Exception {
        HashMap bindVariables = new HashMap();
        return executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_cursor_tree", null), new BoundVariable("in_int_elm_id", rightsTo.getId()) }, RESOURCE_RIGHTS_GET_OWNER).getRecordSets()[0];
    }

    static RecordSet selectRightsOwners(DataSource dataSource, OMSElement[] rightsTo) throws Exception {
        HashMap bindVariables = new HashMap();
        HashMap placeHolders = new HashMap();
        StringBuffer sbIds = new StringBuffer();
        for (int i = 0; i < rightsTo.length; i++) {
            if (i > 0) {
                sbIds.append(',');
            }
            sbIds.append(rightsTo[i].getId());
        }
        placeHolders.put("in_int_elm_id_list", sbIds.toString());
        return executeOnResource(dataSource, placeHolders, new BoundVariable[] { new BoundVariable("out_cursor_tree", null) }, RESOURCE_RIGHTS_GET_ARRAY_OWNER).getRecordSets()[0];
    }

    /**
	 * Converts given object (BigDecimal expected) into string
	 *  
	 * @param o - object to convert
	 * 
	 * @return String
	 */
    static String bigDecimalAsString(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        String result = null;
        if (o != null) {
            BigDecimal bd = (BigDecimal) o;
            result = bd.intValue() + Constants.STR_EMPTY;
        }
        return result;
    }

    /**
	 * Converts given object (BigDecimal expected) into int
	 *   
	 * @param o - object to convert
	 * 
	 * @return int
	 */
    static int bigDecimalAsInt(Object o) {
        int result = -1;
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        if (o != null) {
            BigDecimal bd = (BigDecimal) o;
            result = bd.intValue();
        }
        return result;
    }

    private static String getElementDescription(OMSElement element) {
        return "id = '" + element.getId() + ",displayName='" + element.getDisplayName() + "', bookmark = '" + element.getBookmark() + "', ', scheme = '" + element.getNamespacePath() + Constants.STR_SLASH + element.getSchemeName() + "'";
    }

    static String getAssociationDescription(Relation association, OMSMetaData metaData) {
        return "id = '" + association.getId() + "', key = '" + association.getName() + "'; source element: " + getElementDescription(association.getSourceElement()) + "; target element: " + getElementDescription(association.getTargetElement()) + "; attribute: " + getComplexAttributeDescription(association, metaData);
    }

    private static String getCompositionDescription(Relation composition, OMSMetaData metaData) {
        return "id = '" + composition.getId() + "', key = '" + composition.getName() + "'; source element: " + getElementDescription(composition.getSourceElement()) + "; target element: " + getElementDescription(composition.getTargetElement()) + "; attribute: " + getComplexAttributeDescription(composition, metaData);
    }

    private static String getComplexAttributeDescription(Relation relation, OMSMetaData metaData) {
        try {
            ComplexAttribute cat = metaData.getComplexAttributeFromHierarchy(metaData.getScheme(relation.getSourceElement().getNamespacePath(), relation.getSourceElement().getSchemeName()), relation.getComplexAttribute());
            return "id = '" + cat.getId() + "', type = '" + cat.getType() + "', name = '" + cat.getSourceScheme().getNamespace().getPath() + Constants.STR_SLASH + cat.getSourceScheme().getName() + Constants.STR_DOT + cat.getName() + "'";
        } catch (Exception e) {
            return "Couldn't get description for the complex attribute: " + e.getMessage();
        }
    }

    private static String getValueDescription(Value value, OMSMetaData metaData) {
        return "id = '" + value.getId() + "', content = '" + value.getContent() + "'; element: " + getElementDescription(value.getElement()) + "; attribute: " + getSimpleAttributeDescription(value, metaData) + "; datatype enumeration: " + getDatatypeEnumerationDescription(value, metaData);
    }

    private static String getSimpleAttributeDescription(Value value, OMSMetaData metaData) {
        try {
            SimpleAttribute sat = metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute());
            return "id = '" + sat.getId() + "', type_name = '" + sat.getDatatype().getTypeName() + "', type = '" + sat.getDatatype().getType() + "' name = '" + sat.getScheme().getNamespace().getPath() + Constants.STR_SLASH + sat.getScheme().getName() + Constants.STR_DOT + sat.getName() + "'";
        } catch (Exception e) {
            return "Couldn't get description for the simple attribute: " + e.getMessage();
        }
    }

    private static String getDatatypeEnumerationDescription(Value value, OMSMetaData metaData) {
        try {
            DatatypeEnumeration den = metaData.getSimpleAttributeFromHierarchy(metaData.getScheme(value.getElement().getNamespacePath(), value.getElement().getSchemeName()), value.getSimpleAttribute()).getDatatype().getDatatypeEnumerationByName(value.getDatatypeEnumeration());
            return "id = '" + den.getId() + "', use = '" + den.getUse() + "', name = '" + den.getDatatype().getNamespace().getPath() + Constants.STR_SLASH + den.getDatatype().getName() + Constants.STR_DOT + den.getName() + "'";
        } catch (Exception e) {
            return "Couldn't get description for the datatype enumeration: " + e.getMessage();
        }
    }

    private static String getRightsDescription(OMSRights rights) {
        try {
            return "id = '" + rights.getId() + "', granted to = '" + getElementDescription(rights.getGrantedTo()) + "', can associate = '" + rights.canAssociate() + "', can compose = '" + rights.canCompose() + "', can delete = '" + rights.canDelete() + "', can read = '" + rights.canRead() + "', can write = '" + rights.canWrite() + "', ";
        } catch (Exception e) {
            return "Couldn't get description for the rights: " + e.getMessage();
        }
    }

    static boolean isElementLocked(DataSource dataSource, OMSElement element) throws Exception {
        try {
            CallResult callResult = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_result", null), new BoundVariable("in_int_elm_id", new Integer(element.getId())) }, RESOURCE_IS_LOCKED);
            Object value = callResult.getBoundVariable("out_int_result").getValue();
            int result = getIntOfCharOrInteger(value);
            return result > 0;
        } catch (Throwable t) {
            throw new Exception("Error while testing lock on element " + getElementDescription(element) + ": " + t.getMessage(), t);
        }
    }

    private static int getIntOfCharOrInteger(Object value) {
        int intValue;
        if (!(value instanceof Integer)) {
            String aa = (String) value;
            intValue = Integer.parseInt(aa);
        } else {
            intValue = ((Integer) value).intValue();
        }
        return intValue;
    }

    static void removeLockByKey(DataSource dataSource, String key) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("in_int_key", new Integer(key)) }, RESOURCE_REMOVE_LOCK_BY_KEY);
        } catch (Throwable t) {
            throw new Exception("Error while removing lock by key '" + key + "': " + t.getMessage(), t);
        }
    }

    static void removeLockById(DataSource dataSource, String id) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("in_int_id", new Integer(id)) }, RESOURCE_REMOVE_LOCK_BY_ID);
        } catch (Throwable t) {
            throw new Exception("Error while removing lock by id '" + id + "': " + t.getMessage(), t);
        }
    }

    static String getLockerKey(DataSource dataSource) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_string_key", null) }, RESOURCE_GET_LOCKER_KEY);
            return result.getBoundVariable("out_string_key").getValue().toString();
        } catch (Throwable t) {
            throw new Exception("Error while getting locker key: " + t.getMessage(), t);
        }
    }

    static void updateLock(DataSource dataSource, String lckId) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("in_int_id", lckId) }, RESOURCE_UPDATE_LOCKED_ELEMENT);
        } catch (Throwable t) {
            throw new Exception("Error while creating lock on element " + lckId + ": " + t.getMessage(), t);
        }
    }

    static String lockElement(DataSource dataSource, OMSElement element, String key, String ip, int interval, int userId) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_locker_elm_id", new Integer(userId)), new BoundVariable("in_int_locked_elm_id", new Integer(element.getId())), new BoundVariable("in_int_key", key), new BoundVariable("in_string_locker_ip", ip), new BoundVariable("in_int_interval", new Integer(interval)) }, RESOURCE_CREATE_LOCK);
            return result.getBoundVariable("out_int_id").getValue().toString();
        } catch (Throwable t) {
            throw new Exception("Error while creating lock on element " + getElementDescription(element) + ": " + t.getMessage(), t);
        }
    }

    static void lockStructure(DataSource dataSource, OMSStructure tree, String key, String ip, int interval, int userId) throws Exception {
        try {
            OMSElement[] elements = tree.getElements();
            for (int i = 0; i < elements.length; i++) {
                lockElement(dataSource, elements[i], key, ip, interval, userId);
            }
        } catch (Throwable t) {
            throw new Exception("Error while creating lock on oms structure: " + t.getMessage(), t);
        }
    }

    private static String createAuthorisation(DataSource dataSource, OMSRights rights, OMSElement grantedFor, int userId) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_source_elm_id", new Integer(rights.getGrantedTo().getId())), new BoundVariable("in_int_target_elm_id", new Integer(grantedFor.getId())), new BoundVariable("in_string_read", getRight(rights.canRead())), new BoundVariable("in_string_write", getRight(rights.canWrite())), new BoundVariable("in_string_compose", getRight(rights.canCompose())), new BoundVariable("in_string_associate", getRight(rights.canAssociate())), new BoundVariable("in_string_delete", getRight(rights.canDelete())), new BoundVariable("in_int_creating_elm_id", new Integer(userId)) }, RESOURCE_CREATE_AUTHORISATION);
            String id = result.getBoundVariable("out_int_id").getValue().toString();
            rights.setId(id);
            return id;
        } catch (Throwable t) {
            throw new Exception("Error while creating rights " + getRightsDescription(rights) + ": " + t.getMessage(), t);
        }
    }

    private static String updateAuthorisation(DataSource dataSource, OMSRights rights, int userId) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_id", null), new BoundVariable("in_int_aut_id", new Integer(rights.getId())), new BoundVariable("in_string_read", getRight(rights.canRead())), new BoundVariable("in_string_write", getRight(rights.canWrite())), new BoundVariable("in_string_compose", getRight(rights.canCompose())), new BoundVariable("in_string_associate", getRight(rights.canAssociate())), new BoundVariable("in_string_delete", getRight(rights.canDelete())), new BoundVariable("in_int_modifying_elm_id", new Integer(userId)) }, RESOURCE_UPDATE_AUTHORISATION);
            return result.getBoundVariable("out_int_id").getValue().toString();
        } catch (Throwable t) {
            throw new Exception("Error while creating rights " + getRightsDescription(rights) + ": " + t.getMessage(), t);
        }
    }

    private static String removeAuthorisation(DataSource dataSource, OMSRights rights) throws Exception {
        try {
            CallResult result = executeOnResource(dataSource, null, new BoundVariable[] { new BoundVariable("out_int_result", null), new BoundVariable("in_int_aut_id", new Integer(rights.getId())) }, RESOURCE_REMOVE_AUTHORISATION);
            return result.getBoundVariable("out_int_result").getValue().toString();
        } catch (Throwable t) {
            throw new Exception("Error while removing rights " + getRightsDescription(rights) + ": " + t.getMessage(), t);
        }
    }

    private static String getRight(boolean right) {
        return right ? RIGHT_GRANTED : RIGHT_DENIED;
    }

    static void resetState(OMSNode[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setState(OMSNode.STATE_NORMAL);
        }
    }

    int createElements(OMSMetaData metaData, DataSource dataSource, OMSElement[] elements, int userId) throws Exception {
        for (int i = 0; i < elements.length; i++) {
            createElement(metaData, dataSource, elements[i], userId);
        }
        resetState(elements);
        return elements.length;
    }

    int createSimpleValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            createSimpleValue(metaData, dataSource, values[i], userId);
        }
        resetState(values);
        return values.length;
    }

    int createClobValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            createClobValue(metaData, dataSource, values[i], userId);
        }
        resetState(values);
        return values.length;
    }

    int createBlobValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            createBlobValue(metaData, dataSource, values[i], userId);
        }
        resetState(values);
        return values.length;
    }

    int createUniqueValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            createUniqueValue(metaData, dataSource, values[i], userId);
        }
        resetState(values);
        return values.length;
    }

    int createAssociations(OMSMetaData metaData, DataSource dataSource, Relation[] relations, int userId) throws Exception {
        for (int i = 0; i < relations.length; i++) {
            saveAssociation(metaData, dataSource, relations[i], userId);
        }
        resetState(relations);
        return relations.length;
    }

    int createCompositions(OMSMetaData metaData, DataSource dataSource, Relation[] relations, int userId) throws Exception {
        for (int i = 0; i < relations.length; i++) {
            saveComposition(metaData, dataSource, relations[i], userId);
        }
        resetState(relations);
        return relations.length;
    }

    int removeElements(OMSMetaData metaData, DataSource dataSource, OMSElement[] elements, int userId) throws Exception {
        for (int i = 0; i < elements.length; i++) {
            removeElement(dataSource, elements[i]);
        }
        return elements.length;
    }

    int removeSimpleValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            removeSimpleValue(metaData, dataSource, values[i], userId);
        }
        return values.length;
    }

    int removeClobValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            removeClobValue(metaData, dataSource, values[i], userId);
        }
        return values.length;
    }

    int removeBlobValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            removeBlobValue(metaData, dataSource, values[i], userId);
        }
        return values.length;
    }

    int removeUniqueValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            removeUniqueValue(metaData, dataSource, values[i], userId);
        }
        return values.length;
    }

    int removeAssociations(OMSMetaData metaData, DataSource dataSource, Relation[] relations, int userId) throws Exception {
        for (int i = 0; i < relations.length; i++) {
            removeAssociation(metaData, dataSource, relations[i]);
        }
        return relations.length;
    }

    int removeCompositions(OMSMetaData metaData, DataSource dataSource, Relation[] relations, int userId) throws Exception {
        for (int i = 0; i < relations.length; i++) {
            removeComposition(metaData, dataSource, relations[i]);
        }
        return relations.length;
    }

    int updateElements(OMSMetaData metaData, DataSource dataSource, OMSElement[] elements, int userId) throws Exception {
        for (int i = 0; i < elements.length; i++) {
            updateElement(dataSource, elements[i], userId);
        }
        resetState(elements);
        return elements.length;
    }

    int updateSimpleValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            updateSimpleValue(metaData, dataSource, values[i], userId);
        }
        resetState(values);
        return values.length;
    }

    int updateClobValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            updateClobValue(metaData, dataSource, values[i], userId);
        }
        resetState(values);
        return values.length;
    }

    int updateBlobValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            updateBlobValue(metaData, dataSource, values[i], userId);
        }
        resetState(values);
        return values.length;
    }

    int updateUniqueValues(OMSMetaData metaData, DataSource dataSource, Value[] values, int userId) throws Exception {
        for (int i = 0; i < values.length; i++) {
            updateUniqueValue(metaData, dataSource, values[i], userId);
        }
        resetState(values);
        return values.length;
    }

    int updateAssociations(OMSMetaData metaData, DataSource dataSource, Relation[] relations, int userId) throws Exception {
        for (int i = 0; i < relations.length; i++) {
            saveAssociation(metaData, dataSource, relations[i], userId);
            setKey(dataSource, relations[i]);
        }
        resetState(relations);
        return relations.length;
    }

    int updateCompositions(OMSMetaData metaData, DataSource dataSource, Relation[] relations, int userId) throws Exception {
        for (int i = 0; i < relations.length; i++) {
            saveComposition(metaData, dataSource, relations[i], userId);
            setKey(dataSource, relations[i]);
        }
        resetState(relations);
        return relations.length;
    }
}
