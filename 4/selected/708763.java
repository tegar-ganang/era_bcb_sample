package com.farukcankaya.simplemodel.gramar;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.Tree;
import com.farukcankaya.simplemodel.generator.GeneratorContext;

public class SimplemodelTreeAdaptor extends CommonTreeAdaptor {

    public abstract static class SimpleModelTree extends CommonTree {

        public SimpleModelTree(Token token) {
            super(token);
            this.token = token;
        }

        public <T> T extractSingle(Class<T> type, boolean safe) {
            for (int i = 0; i < getChildCount(); i++) {
                final Tree child = getChild(i);
                if (type.isInstance(child)) return type.cast(child);
            }
            if (safe) return null; else throw new IllegalStateException();
        }

        public <T> List<T> extractAll(Class<T> type) {
            List<T> result = new LinkedList<T>();
            for (int i = 0; i < getChildCount(); i++) {
                final Tree child = getChild(i);
                if (type.isInstance(child)) result.add(type.cast(child));
            }
            return result;
        }
    }

    public static enum SMCIAType {

        SMClass, SMAbstract, SMInterface
    }

    public static class SMDocument extends SimpleModelTree {

        public SMDocument(Token token) {
            super(token);
        }

        public SMPackageTree getPackage() {
            return extractSingle(SMPackageTree.class, false);
        }
    }

    public static class SMPackageTree extends SimpleModelTree {

        public SMPackageTree(Token token) {
            super(token);
        }

        public List<SMCIATree> getCIAs() {
            return extractAll(SMCIATree.class);
        }

        public SMOptionsTree getPackageOptions() {
            return extractSingle(SMOptionsTree.class, true);
        }

        public List<SMEnumTree> getEnums() {
            return extractAll(SMEnumTree.class);
        }

        public String getPackageName() {
            return getChild(0).getText();
        }

        public Options getOptions() {
            return new Options() {

                public String readText(int type) {
                    if (getPackageOptions() == null) return null;
                    Tree tree = getPackageOptions().getFirstChildWithType(type);
                    if (tree == null) return null;
                    tree = tree.getChild(0);
                    if (tree == null) return null;
                    return tree.getText();
                }

                @Override
                public boolean hasVetoableChangeSupport() {
                    String changeSupport = readText(SimplemodelParser.CHANGE_SUPPORT_TYPE);
                    return "both".equals(changeSupport) || "vetoable".equals(changeSupport);
                }

                @Override
                public boolean hasPropertyChangeSupport() {
                    String changeSupport = readText(SimplemodelParser.CHANGE_SUPPORT_TYPE);
                    return "both".equals(changeSupport) || "property".equals(changeSupport);
                }

                @Override
                public LockType getLockType() {
                    String lockType = readText(SimplemodelParser.LOCK_TYPE);
                    if ("reentrant".equals(lockType)) {
                        return LockType.Reentrant;
                    }
                    if ("synchronized".equals(lockType)) {
                        return LockType.Synchronized;
                    }
                    if ("readwrite".equals(lockType)) {
                        return LockType.ReentrantReadWrite;
                    }
                    return LockType.None;
                }

                @Override
                public String getBaseClassName() {
                    return SimplemodelTreeAdaptor.SMPackageTree.this.getPackageName() + "Base" + (getModelType() == ModelType.Interface ? "Impl" : "");
                }

                @Override
                public String getBaseInterfaceName() {
                    return SimplemodelTreeAdaptor.SMPackageTree.this.getPackageName() + "Base";
                }

                @Override
                public String getBaseInterfaceFull() {
                    return (getIPackage().equals("") ? "" : (getIPackage() + ".")) + getBaseInterfaceName();
                }

                @Override
                public String getCPackage() {
                    final String cpackage = readText(SimplemodelParser.CPACKAGE);
                    return cpackage == null ? "" : cpackage;
                }

                @Override
                public String getIPackage() {
                    final String ipackage = readText(SimplemodelParser.IPACKAGE);
                    return ipackage == null ? "" : ipackage;
                }

                @Override
                public ModelType getModelType() {
                    final String modelType = readText(SimplemodelParser.MODEL_TYPE);
                    return "interface".equals(modelType) ? ModelType.Interface : ModelType.Class;
                }

                @Override
                public boolean useOverrideAnnotations() {
                    return getModelType() == ModelType.Interface;
                }

                @Override
                public String getCFactory() {
                    return getPackageName() + "FactoryImpl";
                }

                @Override
                public String getIFactory() {
                    return getPackageName() + "Factory";
                }

                @Override
                public String getIFactoryFull() {
                    return (getIPackage().equals("") ? "" : (getIPackage() + ".")) + getIFactory();
                }

                @Override
                public String getCFactoryFull() {
                    return (getCPackage().equals("") ? "" : (getCPackage() + ".")) + getCFactory();
                }
            };
        }

        public SMCIATree findCia(String ciaName) {
            for (SMCIATree cia : extractAll(SMCIATree.class)) {
                if (cia.getCIAName().equals(ciaName)) return cia;
            }
            throw new RuntimeException(ciaName + " not found!");
        }

        public boolean hasAnyIndexedAggregation() {
            for (SMCIATree cia : getCIAs()) {
                for (Property property : cia.getProperties()) {
                    if (property.getIndexProperty() != null) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static enum LockType {

        None, Synchronized, Reentrant, ReentrantReadWrite
    }

    public static enum ModelType {

        Interface, Class
    }

    public static interface Options {

        boolean hasPropertyChangeSupport();

        String getBaseInterfaceFull();

        boolean hasVetoableChangeSupport();

        LockType getLockType();

        String getCFactory();

        String getIFactory();

        String getIFactoryFull();

        String getCPackage();

        String getIPackage();

        String getBaseClassName();

        String getBaseInterfaceName();

        ModelType getModelType();

        boolean useOverrideAnnotations();

        String getCFactoryFull();
    }

    public static class SMEnumTree extends SimpleModelTree {

        public SMEnumTree(Token token) {
            super(token);
        }

        public String getName() {
            return getToken().getText();
        }

        public List<String> getLiterals() {
            List<String> literals = new LinkedList<String>();
            for (int i = 0; i < getChildCount(); i++) {
                literals.add(((CommonTree) getChild(i)).getToken().getText());
            }
            return literals;
        }

        public SMPackageTree getParent() {
            return (SMPackageTree) super.getParent();
        }
    }

    public static class SMTypeTree extends SimpleModelTree {

        public SMTypeTree(Token token) {
            super(token);
        }

        public String getJavaType() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < getChildCount(); i++) {
                sb.append(getChild(i).getText());
            }
            return sb.toString();
        }
    }

    public static class SMSourceMultiplicationTree extends SimpleModelTree {

        public SMSourceMultiplicationTree(Token token) {
            super(token);
        }

        public List<String> getIndexes() {
            List<String> list = new LinkedList<String>();
            for (int i = 0; i < getChildCount(); i++) {
                list.add(getChild(i).getText());
            }
            return list;
        }
    }

    public static class SMRelationTree extends SimpleModelTree {

        public SMRelationTree(Token token) {
            super(token);
        }

        public String getPropertyName() {
            return getChild(0).getText();
        }

        public boolean isAggregation() {
            return getToken().getType() == SimplemodelParser.AGGREGATION;
        }

        public boolean isComposition() {
            return getToken().getType() == SimplemodelParser.COMPOSITION;
        }

        public String getInversePropertyName() {
            return isAggregation() ? getChild(2).getText() : null;
        }

        public SMCIATree getPropertyType() {
            return getParent().getParent().findCia(getChild(1).getText());
        }

        public SMCIATree getParent() {
            return (SMCIATree) super.getParent();
        }

        public String getAllValidations(GeneratorContext gc) {
            return "";
        }
    }

    public static interface Property {

        String getPropertyName();

        Property getIndexProperty();

        boolean isMultiple();

        String getAllValidations(GeneratorContext gc);

        void appendAllValidations(StringBuilder sb, GeneratorContext gc);

        String getPropertyType();

        boolean isInitializedByConstructor();

        Property getInverse();

        boolean hasGet();

        boolean hasSet();

        boolean hasAdd();

        boolean hasRemove();

        SMCIATree getPropertyTypeCIA();
    }

    private static class PropertyProperty implements Property {

        private final SMPropertyTree p;

        public PropertyProperty(SMPropertyTree p) {
            this.p = p;
        }

        @Override
        public String getPropertyName() {
            return p.getPropertyName();
        }

        @Override
        public String getPropertyType() {
            return p.getPropertyType();
        }

        @Override
        public void appendAllValidations(StringBuilder sb, GeneratorContext gc) {
            p.appendAllValidations(sb, gc);
        }

        @Override
        public String getAllValidations(GeneratorContext gc) {
            return p.getAllValidations(gc);
        }

        @Override
        public boolean isInitializedByConstructor() {
            return p.getFirstChildWithType(SimplemodelParser.PROPERTY_INITIALIZER) != null;
        }

        @Override
        public Property getInverse() {
            return null;
        }

        @Override
        public boolean hasAdd() {
            return false;
        }

        @Override
        public boolean hasGet() {
            return true;
        }

        @Override
        public boolean hasRemove() {
            return false;
        }

        @Override
        public boolean hasSet() {
            return true;
        }

        @Override
        public boolean isMultiple() {
            return false;
        }

        @Override
        public Property getIndexProperty() {
            return null;
        }

        @Override
        public SMCIATree getPropertyTypeCIA() {
            return null;
        }
    }

    private static class RelationProperty implements Property {

        private final SMRelationTree p;

        public RelationProperty(SMRelationTree p) {
            this.p = p;
        }

        public boolean isMultiple() {
            return p.getFirstChildWithType(SimplemodelParser.SOURCE_MULTIPLICATION) != null;
        }

        @Override
        public String getPropertyName() {
            return p.getPropertyName();
        }

        @Override
        public String getPropertyType() {
            return p.getPropertyType().getFullInterfaceName();
        }

        @Override
        public void appendAllValidations(StringBuilder sb, GeneratorContext gc) {
        }

        @Override
        public boolean isInitializedByConstructor() {
            return false;
        }

        @Override
        public String getAllValidations(GeneratorContext gc) {
            return "";
        }

        @Override
        public Property getInverse() {
            if (p.getInversePropertyName() == null) return null; else return new Property() {

                @Override
                public void appendAllValidations(StringBuilder sb, GeneratorContext gc) {
                }

                @Override
                public String getAllValidations(GeneratorContext gc) {
                    return "";
                }

                @Override
                public Property getInverse() {
                    return RelationProperty.this;
                }

                @Override
                public String getPropertyName() {
                    return p.getInversePropertyName();
                }

                @Override
                public String getPropertyType() {
                    return p.getParent().getFullInterfaceName();
                }

                @Override
                public boolean isInitializedByConstructor() {
                    return false;
                }

                @Override
                public boolean hasAdd() {
                    return false;
                }

                @Override
                public boolean hasGet() {
                    return true;
                }

                @Override
                public boolean hasRemove() {
                    return false;
                }

                @Override
                public boolean hasSet() {
                    return true;
                }

                @Override
                public boolean isMultiple() {
                    return false;
                }

                @Override
                public Property getIndexProperty() {
                    return null;
                }

                @Override
                public SMCIATree getPropertyTypeCIA() {
                    return p.getParent();
                }
            };
        }

        @Override
        public boolean hasAdd() {
            return isMultiple();
        }

        @Override
        public boolean hasGet() {
            return true;
        }

        @Override
        public boolean hasRemove() {
            return isMultiple();
        }

        @Override
        public boolean hasSet() {
            return !isMultiple();
        }

        @Override
        public Property getIndexProperty() {
            CommonTree sourceMultiplication = (CommonTree) p.getFirstChildWithType(SimplemodelParser.SOURCE_MULTIPLICATION);
            if (sourceMultiplication != null && sourceMultiplication.getChildCount() > 0) return getPropertyTypeCIA().getProperty(sourceMultiplication.getChild(0).getText());
            return null;
        }

        @Override
        public SMCIATree getPropertyTypeCIA() {
            return p.getPropertyType();
        }
    }

    public static class SMPropertyTree extends SimpleModelTree {

        public SMPropertyTree(Token token) {
            super(token);
        }

        public String getPropertyName() {
            return ((CommonTree) getChild(0)).getText();
        }

        public String getPropertyType() {
            return ((SMTypeTree) getChild(1)).getJavaType();
        }

        public boolean isReadable() {
            for (int i = 2; i < getChildCount(); i++) {
                if (((CommonTree) getChild(i)).getToken().getType() == SimplemodelParser.WRITEONLY) return false;
            }
            return true;
        }

        public boolean isWriteable() {
            for (int i = 2; i < getChildCount(); i++) {
                if (((CommonTree) getChild(i)).getToken().getType() == SimplemodelParser.READONLY) return false;
            }
            return true;
        }

        public SMCIATree getParent() {
            return (SMCIATree) super.getParent();
        }

        public String getAllValidations(GeneratorContext gc) {
            StringBuilder sb = new StringBuilder();
            appendAllValidations(sb, gc);
            return sb.toString();
        }

        public void appendAllValidations(StringBuilder sb, GeneratorContext gc) {
            CommonTree propertyBody = (CommonTree) getFirstChildWithType(SimplemodelParser.PROPERTY_BODY);
            if (propertyBody == null) return;
            CommonTree validator = (CommonTree) propertyBody.getFirstChildWithType(SimplemodelParser.VALIDATOR);
            if (validator == null) return;
            for (int i = 0; i < validator.getChildCount(); i++) {
                CommonTree child = (CommonTree) validator.getChild(i);
                if (child.getToken().getType() == SimplemodelParser.VALIDATE) {
                    String validationText;
                    CommonTree text = (CommonTree) child.getChild(0);
                    if (text == null) {
                        validationText = "";
                    } else {
                        validationText = text.getText();
                    }
                    sb.append("if (").append(gc.getVariableName(new PropertyProperty(this))).append(" == null) throw new IllegalArgumentException(").append(validationText).append(");").append(gc.NL);
                }
            }
        }
    }

    public static class SMCIATree extends SimpleModelTree {

        private final SMCIAType ciaType;

        public boolean hasMultipleProperty() {
            for (Property property : getProperties()) {
                if (property.isMultiple()) return true;
            }
            return false;
        }

        public SMCIAType getCiaType() {
            return ciaType;
        }

        public SMCIATree(Token token, SMCIAType ciaType) {
            super(token);
            this.ciaType = ciaType;
        }

        public String getClassName() {
            if (getParent().getOptions().getModelType() == ModelType.Interface) return getCIAName() + "Impl"; else return getCIAName();
        }

        public String getInterfaceName() {
            return getCIAName();
        }

        public String getCIAName() {
            return getChild(0).getText();
        }

        public List<Property> getProperties() {
            List<Property> list = new LinkedList<Property>();
            for (final SMPropertyTree p : extractAll(SMPropertyTree.class)) {
                list.add(new PropertyProperty(p));
            }
            for (final SMRelationTree p : extractAll(SMRelationTree.class)) {
                list.add(new RelationProperty(p));
            }
            for (SMCIATree cia : getParent().getCIAs()) {
                for (SMRelationTree relation : cia.extractAll(SMRelationTree.class)) {
                    if (relation.getPropertyType() == this) {
                        RelationProperty relationProperty = new RelationProperty(relation);
                        Property inverse = relationProperty.getInverse();
                        if (inverse != null) list.add(inverse);
                    }
                }
            }
            return list;
        }

        public SMPackageTree getParent() {
            return (SMPackageTree) super.getParent();
        }

        public List<SMCIATree> getSuper() {
            final Tree extendsTree = getFirstChildWithType(SimplemodelParser.EXTENDS);
            if (extendsTree == null) return Collections.emptyList();
            List<SMCIATree> result = new LinkedList<SMCIATree>();
            for (int i = 0; i < extendsTree.getChildCount(); i++) {
                String ciaName = extendsTree.getChild(i).getText();
                result.add(getParent().findCia(ciaName));
            }
            return result;
        }

        public String getFullInterfaceName() {
            return (getParent().getOptions().getIPackage().equals("") ? "" : (getParent().getOptions().getIPackage() + ".")) + getInterfaceName();
        }

        public String getFullClassName() {
            return (getParent().getOptions().getCPackage().equals("") ? "" : (getParent().getOptions().getCPackage() + ".")) + getClassName();
        }

        public String getAllValidations(GeneratorContext gc) {
            StringBuilder sb = new StringBuilder();
            appendAllValidations(sb, gc);
            return sb.toString();
        }

        public void appendAllValidations(StringBuilder sb, GeneratorContext gc) {
            for (Property property : getProperties()) {
                property.appendAllValidations(sb, gc);
            }
        }

        public Property getProperty(String indexProperty) {
            for (Property property : getProperties()) {
                if (indexProperty.equals(property.getPropertyName())) {
                    return property;
                }
            }
            throw new IllegalArgumentException("Not found!");
        }

        public List<Property> getConstructorInitializedPropertiesSuper() {
            if (getSuper().isEmpty()) return Collections.emptyList(); else return getSuper().get(0).getConstructorInitializedProperties();
        }

        public List<Property> getConstructorInitializedPropertiesSelf() {
            List<Property> properties = getProperties();
            for (Iterator<Property> iterator = properties.iterator(); iterator.hasNext(); ) {
                Property property = iterator.next();
                if (!property.isInitializedByConstructor()) iterator.remove();
            }
            return properties;
        }

        public List<Property> getConstructorInitializedProperties() {
            List<Property> result = new LinkedList<Property>(getConstructorInitializedPropertiesSuper());
            result.addAll(getConstructorInitializedPropertiesSelf());
            return result;
        }
    }

    public static class SMOptionsTree extends SimpleModelTree {

        public SMOptionsTree(Token token) {
            super(token);
        }

        public SMPackageTree getParent() {
            return (SMPackageTree) super.getParent();
        }
    }

    @Override
    public Object create(Token payload) {
        if (payload == null) return super.create(payload);
        switch(payload.getType()) {
            case SimplemodelParser.DOCUMENT:
                return new SMDocument(payload);
            case SimplemodelParser.CLASS:
                return new SMCIATree(payload, SMCIAType.SMClass);
            case SimplemodelParser.ABSTRACT:
                return new SMCIATree(payload, SMCIAType.SMAbstract);
            case SimplemodelParser.INTERFACE:
                return new SMCIATree(payload, SMCIAType.SMInterface);
            case SimplemodelParser.ENUM:
                return new SMEnumTree(payload);
            case SimplemodelParser.PACKAGE:
                return new SMPackageTree(payload);
            case SimplemodelParser.PROPERTY:
                return new SMPropertyTree(payload);
            case SimplemodelParser.TYPE:
                return new SMTypeTree(payload);
            case SimplemodelParser.OPTIONS:
                return new SMOptionsTree(payload);
            case SimplemodelParser.AGGREGATION:
            case SimplemodelParser.COMPOSITION:
                return new SMRelationTree(payload);
            default:
                return (CommonTree) super.create(payload);
        }
    }

    @Override
    public Object dupNode(Object t) {
        if (t == null) {
            return null;
        }
        return create(((CommonTree) t).getToken());
    }
}
