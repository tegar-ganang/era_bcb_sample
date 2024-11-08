package com.genia.toolbox.uml_generator.manager.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.genia.toolbox.basics.bean.impl.ConjunctionPredicate;
import com.genia.toolbox.basics.bean.impl.DefaultDisplayObject;
import com.genia.toolbox.basics.bean.impl.SimpleDelimiter;
import com.genia.toolbox.basics.exception.BundledException;
import com.genia.toolbox.basics.exception.technical.TechnicalIOException;
import com.genia.toolbox.uml_generator.Constants;
import com.genia.toolbox.uml_generator.bean.ModelGeneratorContext;
import com.genia.toolbox.uml_generator.manager.ModelGenerator;
import com.genia.toolbox.uml_generator.manager.impl.ModelDecoratorImpl.GenericObject;
import com.genia.toolbox.uml_generator.model.AbstractClassModel;
import com.genia.toolbox.uml_generator.model.AbstractJavaObject;
import com.genia.toolbox.uml_generator.model.AbstractObject;
import com.genia.toolbox.uml_generator.model.AssociationCardinality;
import com.genia.toolbox.uml_generator.model.AssociationModel;
import com.genia.toolbox.uml_generator.model.AttributeModel;
import com.genia.toolbox.uml_generator.model.DataTypeModel;
import com.genia.toolbox.uml_generator.model.EnumerationModel;
import com.genia.toolbox.uml_generator.model.StereotypeModel;
import com.genia.toolbox.uml_generator.model.TagModel;
import com.genia.toolbox.uml_generator.model.impl.ClassModel;
import com.genia.toolbox.uml_generator.model.impl.InterfaceModel;
import com.genia.toolbox.uml_generator.visitor.ModelVisitor;

/**
 * implementation of {@link ModelGenerator} that generated hibernate mappings.
 */
public class HibernateMappingGeneratorImpl extends AbstractManager implements ModelGenerator {

    /**
   * the set of classes that are collections and that must be mapped in their
   * own way.
   */
    private static final Set<String> COLLECTION_INTERFACES = new HashSet<String>(Arrays.asList(List.class.getName(), Set.class.getName(), Map.class.getName()));

    /**
   * the different type of collection handled by the generator.
   */
    private enum CollectionType {

        /**
     * {@link Map} collection.
     */
        MAP, /**
     * {@link Set} collection.
     */
        SET, /**
     * {@link List} collection.
     */
        LIST
    }

    ;

    /**
   * check if hibernate mapping should be skipped.
   * @param classModel
   *             the {@link AbstractClassModel}.
   * @return true if skipping set, otherwise false
   */
    private boolean isSkippingHibernateMapping(AbstractClassModel classModel) {
        boolean result = false;
        StereotypeModel skipMappingStereotype = classModel.getStereotype(Constants.SKIP_MAPPING_STEREOTYPE);
        if (skipMappingStereotype != null) {
            result = true;
        }
        return result;
    }

    /**
   * the generator.
   * 
   * @param context
   *          the context of the generation
   * @throws BundledException
   *           when an error occurred
   * @see com.genia.toolbox.uml_generator.manager.ModelGenerator#generate(com.genia.toolbox.uml_generator.bean.ModelGeneratorContext)
   */
    public void generate(ModelGeneratorContext context) throws BundledException {
        for (AbstractClassModel classModel : getCollectionManager().filterList(context.getClasses().values(), Constants.ONLY_GENERATABLE_CLASSES)) {
            if (getModelInspector().isPersistable(classModel) && (!isSkippingHibernateMapping(classModel))) {
                generateHibernateMapping(context, classModel);
            }
        }
    }

    /**
   * generate the hibernate mapping for an {@link AbstractClassModel}.
   * 
   * @param context
   *          the context of the generation
   * @param classModel
   *          the {@link AbstractClassModel} to consider
   * @throws BundledException
   *           when an error occurred
   */
    private void generateHibernateMapping(ModelGeneratorContext context, AbstractClassModel classModel) throws BundledException {
        File classFile = new File(context.getDirectoryForPackage(context.getConfiguration().getGeneratedResourcesDir(), classModel), classModel.getName() + ".hbm.xml");
        classFile.getParentFile().mkdirs();
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(classFile));
            out.append("<?xml version=\"1.0\"?>\n");
            out.append("<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n");
            out.append("<!-- DO NOT EDIT: This is a generated file -->");
            out.append("<hibernate-mapping>\n");
            if (getModelInspector().isFirstPersistable(classModel)) {
                generateFirstPersistableHibernateMapping(context, out, classModel);
            } else {
                generateNotFirstPersistableHibernateMapping(context, out, classModel);
            }
            out.append("</hibernate-mapping>\n");
            out.close();
        } catch (IOException e) {
            throw getExceptionManager().convertException(e);
        }
    }

    /**
   * generate the mapping file for a {@link AbstractClassModel} that is
   * persistable with a parent not persistable.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    @SuppressWarnings("unchecked")
    private void generateFirstPersistableHibernateMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel) throws BundledException {
        out.append("<class lazy=\"true\" name=\"").append(classModel.getQualifiedName()).append("\" proxy=\"").append(classModel.getUndecoraredObject().getQualifiedName()).append("\" table=\"").append(getDbIdentifier(context, classModel.getUndecoraredObject())).append("\"");
        if (classModel.isAbstractClass()) {
            out.append(" abstract=\"true\"");
        }
        out.append(">\n");
        addCacheIfNecessary(out, classModel);
        generateIdentifierMapping(context, out, classModel);
        for (AttributeModel attribute : getCollectionManager().filterSet(classModel.getAttributes(), new ConjunctionPredicate<AttributeModel>(Constants.REMOVE_IDENTIFIER_PREDICATE, Constants.REMOVE_UN_MAPPABLE_PREDICATE))) {
            if (COLLECTION_INTERFACES.contains(attribute.getType().getHibernateQualifiedName())) {
                generateCollectionAttributeMapping(context, out, classModel, attribute);
            } else {
                generateAttributeMapping(context, out, classModel, attribute);
            }
        }
        for (AssociationModel association : classModel.getAssociations()) {
            generateAssociationMapping(context, out, classModel, association);
        }
        out.append("</class>\n");
    }

    /**
   * generate the identifier mapping.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateIdentifierMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel) throws BundledException {
        AttributeModel identifier = getModelInspector().getIdentifier(classModel);
        out.append("<id name=\"identifier\" column=\"").append(getDbIdentifier(context, identifier)).append("\" type=\"").append(identifier.getType().getHibernateQualifiedName()).append("\" unsaved-value=\"null\">\n");
        out.append("<generator class=\"hilo\">" + "<param name=\"table\">").append(generateHiloTableName(context, classModel)).append("</param></generator>\n");
        out.append("</id>\n");
    }

    /**
   * returns a table name for generating the hilo table of the class given in
   * parameter.
   * 
   * @param context
   *          the context of the generation
   * @param classModel
   *          the {@link AbstractClassModel} to generate the table for
   * @return a table name for generating the hilo table of the class given in
   *         parameter
   */
    private String generateHiloTableName(ModelGeneratorContext context, AbstractClassModel classModel) {
        return "hibernate_unique_key";
    }

    /**
   * generate the mapping for an attribute that is a map, list or set.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} to generate the mapping for
   * @param attribute
   *          the {@link AttributeModel} to generate the mapping for
   */
    private void generateCollectionAttributeMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AttributeModel attribute) {
        GenericObject genericObject = (GenericObject) attribute.getType();
        CollectionType collectionType = CollectionType.LIST;
        if (attribute.getType().getHibernateQualifiedName().equals(Set.class.getName())) {
            collectionType = CollectionType.SET;
        } else if (attribute.getType().getHibernateQualifiedName().equals(Map.class.getName())) {
            collectionType = CollectionType.MAP;
        }
        out.append("<").append(collectionType.name().toLowerCase()).append(" name=\"").append(attribute.getName()).append("\" table=\"").append(getDbIdentifier(context, collectionType.name().toLowerCase(), classModel.getUndecoraredObject(), attribute)).append("\">");
        addCacheIfNecessary(out, classModel);
        out.append("<key column=\"").append(getDbIdentifier(context, classModel.getUndecoraredObject(), "identifier")).append("\"/>");
        AbstractJavaObject genericType = genericObject.getGenericTypes()[0];
        switch(collectionType) {
            case LIST:
                out.append("<list-index column=\"").append(getDbIdentifier(context, "sort_order")).append("\"/>");
                break;
            case MAP:
                out.append("<map-key column=\"").append(getDbIdentifier(context, "index")).append("\" type=\"").append(genericType.getHibernateQualifiedName()).append("\" ");
                if (genericType.getLength() != null) {
                    out.append("length=\"").append(genericType.getLength().toString()).append("\" ");
                }
                out.append("/>");
                genericType = genericObject.getGenericTypes()[1];
                break;
        }
        out.append("<element column=\"").append(getDbIdentifier(context, "value")).append("\" type=\"").append(genericType.getHibernateQualifiedName()).append("\" ");
        if (genericType.getLength() != null) {
            out.append("length=\"").append(genericType.getLength().toString()).append("\" ");
        }
        out.append("/>");
        out.append("</").append(collectionType.name().toLowerCase()).append(">");
    }

    /**
   * generate the mapping for an attribute.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} to generate the mapping for
   * @param attribute
   *          the {@link AttributeModel} to generate the mapping for
   * @throws TechnicalIOException
   *           if an I/O error occured
   */
    private void generateAttributeMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AttributeModel attribute) throws TechnicalIOException {
        out.append("<property name=\"").append(attribute.getName()).append("\" column=\"").append(getDbIdentifier(context, attribute)).append("\" type=\"").append(attribute.getType().getHibernateQualifiedName()).append("\" not-null=\"").append(String.valueOf(attribute.hasStereotype(Constants.NOT_NULL_STEREOTYPE))).append("\" ");
        if (attribute.hasStereotype(Constants.UNIQUE_STEREOTYPE)) {
            StereotypeModel uniqueStereotype = attribute.getStereotype(Constants.UNIQUE_STEREOTYPE);
            TagModel uniqueKeysTag = uniqueStereotype.getTags().get(Constants.UNIQUE_KEY_TAG);
            if (uniqueKeysTag == null || uniqueKeysTag.getValues() == null || uniqueKeysTag.getValues().isEmpty()) {
                out.append("unique=\"true\" ");
            } else {
                out.append("unique-key=\"").append(getCollectionManager().join(uniqueKeysTag.getValues(), new SimpleDelimiter(","), DefaultDisplayObject.INSTANCE)).append("\" ");
            }
        }
        if (attribute.hasStereotype(Constants.INDEXED_STEREOTYPE)) {
            String index;
            StereotypeModel indexStereotype = attribute.getStereotype(Constants.INDEXED_STEREOTYPE);
            TagModel indexNamesTag = indexStereotype.getTags().get(Constants.INDEX_NAME_TAG);
            if (indexNamesTag == null || indexNamesTag.getValues() == null || indexNamesTag.getValues().isEmpty()) {
                index = getDbIdentifier(context, classModel.getUndecoraredObject(), attribute, "index");
            } else {
                index = getCollectionManager().join(indexNamesTag.getValues(), new SimpleDelimiter(","), DefaultDisplayObject.INSTANCE);
            }
            out.append("index=\"").append(index).append("\" ");
        }
        if (attribute.getType().getLength() != null) {
            out.append("length=\"").append(attribute.getType().getLength().toString()).append("\" ");
        }
        out.append("/>\n");
    }

    /**
   * generate the mapping for an association.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateAssociationMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AssociationModel association) throws BundledException {
        if (association.getQualifier() != null) {
            generateQualifierAssociationMapping(context, out, classModel, association);
        } else {
            switch(association.getCurrentCardinality()) {
                case ONE:
                    switch(association.getOtherCardinality()) {
                        case ONE:
                            generateOneToOneAssociationMapping(context, out, classModel, association);
                            break;
                        case MANY:
                            generateOneToManyAssociationMapping(context, out, classModel, association);
                            break;
                    }
                    break;
                case MANY:
                    switch(association.getOtherCardinality()) {
                        case ONE:
                            generateManyToOneAssociationMapping(context, out, classModel, association);
                            break;
                        case MANY:
                            generateManyToManyAssociationMapping(context, out, classModel, association);
                            break;
                    }
                    break;
            }
        }
    }

    /**
   * return the name of the join table for an association.
   * 
   * @param context
   *          the context of the generation
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the association to consider
   * @return the name of the join table for the association
   */
    private String getJoinTable(ModelGeneratorContext context, AbstractClassModel classModel, AssociationModel association) {
        if (getAssociationManager().hasPriority(association)) {
            return getDbIdentifier(context, association.getAssociatedType(), classModel.getUndecoraredObject(), association);
        } else {
            return getJoinTable(context, association.getAssociatedType(), association.getOtherEndAssociation());
        }
    }

    /**
   * add the cache directive for the class and its collections if and only if
   * the {@link AbstractClassModel} is tagged with the right attribute.
   * 
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} to work on
   */
    private void addCacheIfNecessary(PrintWriter out, AbstractClassModel classModel) {
        if (classModel.hasStereotype(Constants.CACHED_STEREOTYPE)) {
            out.append("<cache usage=\"read-write\"/>");
        }
    }

    /**
   * generate the mapping for a many to many association.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateManyToManyAssociationMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AssociationModel association) throws BundledException {
        out.append("<set cascade=\"persist,merge,save-update\" lazy=\"true\" name=\"").append(getAssociationManager().getInternalAttributeForAssociation(association).getName()).append("\" table=\"").append(getJoinTable(context, classModel, association)).append("\" ");
        if (!getAssociationManager().hasPriority(association)) {
            out.append("inverse=\"true\" ");
        }
        out.append(">\n");
        addCacheIfNecessary(out, classModel);
        out.append("<key column=\"").append(getDbIdentifier(context, classModel.getUndecoraredObject())).append("\"/>\n");
        out.append("<many-to-many class=\"").append(getAssociationClassNameForMapping(association.getAssociatedType())).append("\" column=\"").append(getDbIdentifier(context, association.getAssociatedType())).append("\"/>\n");
        out.append("</set>\n");
    }

    /**
   * returns the name of the class to put in the hibernate mapping for an
   * association. It must be the qualified name of the first persistable class
   * of a class hierachie.
   * 
   * @param classModel
   *          the {@link AbstractClassModel} begin associated with
   * @return the name of the class to put in the hibernate mapping for an
   *         association
   */
    public String getAssociationClassNameForMapping(AbstractClassModel classModel) {
        return getModelDecorator().getImplementationObject(classModel.getUndecoraredObject()).getQualifiedName();
    }

    /**
   * generate the mapping for a one to many association. This association is
   * mandatory two-ways. *
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateOneToManyAssociationMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AssociationModel association) throws BundledException {
        out.append("<set cascade=\"persist,merge,save-update\" lazy=\"true\" name=\"").append(getAssociationManager().getInternalAttributeForAssociation(association).getName()).append("\" ");
        if (!getAssociationManager().hasPriority(association)) {
            out.append("inverse=\"true\" ");
        }
        out.append(">\n");
        addCacheIfNecessary(out, classModel);
        out.append("<key column=\"").append(getDbIdentifier(context, association.getOtherEndAssociation())).append("\"/>\n");
        out.append("<one-to-many class=\"").append(getAssociationClassNameForMapping(association.getAssociatedType())).append("\"/>\n");
        out.append("</set>\n");
    }

    /**
   * generate the mapping for a many to one association.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateManyToOneAssociationMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AssociationModel association) throws BundledException {
        out.append("<many-to-one cascade=\"persist,merge,save-update\" name=\"").append(getAssociationManager().getInternalAttributeForAssociation(association).getName()).append("\" class=\"").append(getAssociationClassNameForMapping(association.getAssociatedType())).append("\" column=\"").append(getDbIdentifier(context, association)).append("\" not-null=\"false\" ");
        if (!getAssociationManager().hasPriority(association)) {
            out.append("inverse=\"true\" ");
        }
        out.append("/>\n");
    }

    /**
   * generate the mapping for a one to one association. This association is
   * mandatory two-ways.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateOneToOneAssociationMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AssociationModel association) throws BundledException {
        if (getAssociationManager().hasPriority(association)) {
            out.append("<many-to-one cascade=\"persist,merge,save-update\" class=\"").append(getAssociationClassNameForMapping(association.getAssociatedType())).append("\" name=\"").append(getAssociationManager().getInternalAttributeForAssociation(association).getName()).append("\" column=\"").append(getDbIdentifier(context, association)).append("\" unique=\"true\"/>\n");
        } else {
            out.append("<one-to-one cascade=\"persist,merge,save-update\" class=\"").append(getAssociationClassNameForMapping(association.getAssociatedType())).append("\" name=\"").append(getAssociationManager().getInternalAttributeForAssociation(association).getName()).append("\" property-ref=\"").append(getAssociationManager().getInternalAttributeForAssociation(association.getOtherEndAssociation()).getName()).append("\"/>\n");
        }
    }

    /**
   * generate the mapping for an association with a qualifier.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateQualifierAssociationMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AssociationModel association) throws BundledException {
        if (association.getCurrentCardinality() == AssociationCardinality.ONE) {
            generateOneToManyQualifierAssociationMapping(context, out, classModel, association);
        } else {
            generateManyToManyQualifierAssociationMapping(context, out, classModel, association);
        }
    }

    /**
   * generate the mapping for a many to many association with a qualifier.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateManyToManyQualifierAssociationMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AssociationModel association) throws BundledException {
        out.append("<map cascade=\"persist,merge,save-update\" lazy=\"true\" name=\"").append(getAssociationManager().getInternalAttributeForAssociation(association).getName()).append("\" table=\"").append(getJoinTable(context, classModel, association)).append("\" ");
        if (!getAssociationManager().hasPriority(association)) {
            out.append("inverse=\"true\" ");
        }
        out.append(">\n");
        addCacheIfNecessary(out, classModel);
        out.append("<key column=\"").append(getDbIdentifier(context, classModel.getUndecoraredObject())).append("\"/>\n");
        generateMapKeyProperty(context, out, classModel, association);
        out.append("<many-to-many class=\"").append(getAssociationClassNameForMapping(association.getAssociatedType())).append("\" column=\"").append(getDbIdentifier(context, association.getAssociatedType())).append("\"/>\n");
        out.append("</map>\n");
    }

    /**
   * generate the mapping for a one to many association with a qualifier.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateOneToManyQualifierAssociationMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel, AssociationModel association) throws BundledException {
        out.append("<map cascade=\"persist,merge,save-update\" lazy=\"true\" name=\"").append(getAssociationManager().getInternalAttributeForAssociation(association).getName()).append("\" ");
        if (!getAssociationManager().hasPriority(association)) {
            out.append("inverse=\"true\" ");
        }
        out.append(">\n");
        addCacheIfNecessary(out, classModel);
        out.append("<key column=\"").append(getDbIdentifier(context, association.getOtherEndAssociation())).append("\"/>\n");
        generateMapKeyProperty(context, out, classModel, association);
        out.append("<one-to-many class=\"").append(getAssociationClassNameForMapping(association.getAssociatedType())).append("\"/>\n");
        out.append("</map>\n");
    }

    /**
   * generate the map key property of the mapping for an association with a
   * qualifier.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} the association is from.
   * @param association
   *          the {@link AssociationModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    private void generateMapKeyProperty(final ModelGeneratorContext context, final PrintWriter out, final AbstractClassModel classModel, final AssociationModel association) throws BundledException {
        association.getQualifier().getType().visit(new ModelVisitor() {

            public void visitSimpleType(AbstractJavaObject qualifierType) {
                out.append("<map-key column=\"").append(getDbIdentifier(context, association.getQualifier())).append("\" type=\"").append(association.getQualifier().getType().getHibernateQualifiedName()).append("\"/>");
            }

            public void visitComplexType(AbstractClassModel qualifierType) {
                out.append("<map-key-many-to-many column=\"").append(getDbIdentifier(context, association.getQualifier())).append("\" class=\"").append(getAssociationClassNameForMapping(qualifierType)).append("\"/>");
            }

            public void visitClassModel(ClassModel classModel) {
                visitComplexType(classModel);
            }

            public void visitDataTypeModel(DataTypeModel dataTypeModel) {
                visitSimpleType(dataTypeModel);
            }

            public void visitEnumerationModel(EnumerationModel enumerationModel) {
                visitSimpleType(enumerationModel);
            }

            public void visitInterfaceModel(InterfaceModel interfaceModel) {
                visitComplexType(interfaceModel);
            }
        });
    }

    /**
   * generate the mapping file for a {@link AbstractClassModel} that is
   * persistable with a parent persistable.
   * 
   * @param context
   *          the context of the generation
   * @param out
   *          the {@link PrintWriter} to print to
   * @param classModel
   *          the {@link AbstractClassModel} to generate the mapping for
   * @throws BundledException
   *           when an error occurred
   */
    @SuppressWarnings("unchecked")
    private void generateNotFirstPersistableHibernateMapping(ModelGeneratorContext context, PrintWriter out, AbstractClassModel classModel) throws BundledException {
        out.append("<joined-subclass name=\"").append(classModel.getQualifiedName()).append("\" extends=\"").append(classModel.getParent().getQualifiedName()).append("\" proxy=\"").append(classModel.getUndecoraredObject().getQualifiedName()).append("\" table=\"").append(getDbIdentifier(context, classModel.getUndecoraredObject())).append("\"");
        if (classModel.isAbstractClass()) {
            out.append(" abstract=\"true\"");
        }
        out.append(">\n");
        out.append("<key column=\"").append(getDbIdentifier(context, "identifier")).append("\"/>\n");
        for (AttributeModel attribute : getCollectionManager().filterSet(classModel.getAttributes(), new ConjunctionPredicate<AttributeModel>(Constants.REMOVE_IDENTIFIER_PREDICATE, Constants.REMOVE_UN_MAPPABLE_PREDICATE))) {
            if (!classModel.getParent().getAttributes().contains(attribute)) {
                generateAttributeMapping(context, out, classModel, attribute);
            }
        }
        for (AssociationModel association : classModel.getAssociations()) {
            if (!classModel.getParent().getAssociations().contains(association)) {
                generateAssociationMapping(context, out, classModel, association);
            }
        }
        out.append("</joined-subclass>\n");
    }

    /**
   * returns a database identifier suited to represents the given object.
   * 
   * @param element
   *          the object to generate a database identifier for
   * @return a database identifier suited to represents the given object
   */
    private String getDbIdentifierFromObject(Object element) {
        if (element instanceof AbstractObject) {
            return getNameManager().getLowerUnderlineName((AbstractObject) element);
        }
        return element.toString();
    }

    /**
   * returns the Database identifier associated to the sequence of elements
   * given in parameter.
   * 
   * @param context
   *          the context of the generation
   * @param elements
   *          the sequence of elements to generate the databse identifier with
   * @return the Database identifier associated to the sequence of elements
   *         given in parameter
   */
    private String getDbIdentifier(ModelGeneratorContext context, Object... elements) {
        StringBuilder result = new StringBuilder();
        result.append(context.getConfiguration().getDbElementPrefix());
        result.append(getDbIdentifierFromObject(elements[0]));
        for (int i = 1; i < elements.length; i++) {
            result.append("_");
            result.append(getDbIdentifierFromObject(elements[i]));
        }
        if (result.length() > context.getConfiguration().getMaxDbIdentifierLength()) {
            result.setLength(context.getConfiguration().getMaxDbIdentifierLength());
        }
        return result.toString();
    }
}
