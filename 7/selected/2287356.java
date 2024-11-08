package com.genia.toolbox.uml_generator.transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import com.genia.toolbox.basics.manager.XmlManager;
import com.genia.toolbox.uml_generator.bean.ModelGeneratorContext;
import com.genia.toolbox.uml_generator.model.AbstractClassModel;
import com.genia.toolbox.uml_generator.model.AbstractDelegatableObject;
import com.genia.toolbox.uml_generator.model.AbstractJavaObject;
import com.genia.toolbox.uml_generator.model.AbstractObject;
import com.genia.toolbox.uml_generator.model.AttributeModel;
import com.genia.toolbox.uml_generator.model.OperationModel;
import com.genia.toolbox.uml_generator.model.StereotypeModel;
import com.genia.toolbox.uml_generator.model.TagModel;
import com.genia.toolbox.uml_generator.model.impl.AttributeModelImpl;
import com.genia.toolbox.uml_generator.model.impl.OperationModelImpl;
import com.genia.toolbox.uml_generator.model.impl.StereotypeModelImpl;
import com.genia.toolbox.uml_generator.model.impl.TagModelImpl;

/**
 * base implementation of {@link InputHandler}.
 * 
 * @param <ObjectType>
 *          the type of the parent of the current {@link Element}
 */
public abstract class AbstractInputHandler<ObjectType> implements InputHandler<ObjectType> {

    /**
   * the {@link XmlManager} to use.
   */
    private XmlManager xmlManager;

    /**
   * fill the common value of an {@link AbstractJavaObject} from an
   * {@link Element}.
   * 
   * @param context
   *          the generation context
   * @param element
   *          to {@link Element} to retrieve the values for
   * @param model
   *          the object to fill
   */
    public void fillCommonValue(final ModelGeneratorContext context, final Element element, final AbstractJavaObject model) {
        model.setPackageName(getQualifiedName(element));
        fillCommonValue(context, element, (AbstractObject) model);
    }

    /**
   * fill the common value of an {@link AbstractObject} from an {@link Element}.
   * 
   * @param context
   *          the generation context
   * @param element
   *          to {@link Element} to retrieve the values for
   * @param model
   *          the object to fill
   */
    public void fillCommonValue(final ModelGeneratorContext context, final Element element, final AbstractObject model) {
        model.setUmlId(element.getAttribute("xmi.id"));
        model.setName(element.getAttribute("name"));
        String comment = getComment(context, element);
        if (comment != null) {
            model.setComment(comment);
        }
        for (StereotypeModel documentationModel : getStereotypes(context, element)) {
            model.addStereotype(documentationModel);
        }
    }

    /**
   * returns the {@link List} of {@link Element} situated in the given path from
   * the root.
   * 
   * @param context
   *          the generation context
   * @param root
   *          the root {@link Element}
   * @param path
   *          the list of sub-element to follow
   * @return the {@link List} of {@link Element} situated in the given path from
   *         the root
   */
    public List<Element> getElementsByPath(final ModelGeneratorContext context, final Element root, final String... path) {
        if (path.length <= 0) {
            return Arrays.asList(context.getRealElement(root));
        }
        List<Element> res = new ArrayList<Element>();
        String[] nextPath = new String[path.length - 1];
        for (int i = 0; i < nextPath.length; i++) {
            nextPath[i] = path[i + 1];
        }
        for (Element child : getXmlManager().getElements(path[0], context.getRealElement(root).getChildNodes())) {
            res.addAll(getElementsByPath(context, child, nextPath));
        }
        return res;
    }

    /**
   * returns the {@link Element} situated in the given path from the root.
   * 
   * @param context
   *          the generation context
   * @param root
   *          the root {@link Element}
   * @param path
   *          the list of sub-element to follow
   * @return the {@link Element} situated in the given path from the root
   */
    public Element getUniqueElementByPath(final ModelGeneratorContext context, final Element root, final String... path) {
        List<Element> elements = getElementsByPath(context, root, path);
        if (elements.size() != 1) {
            String error = "Element " + root.getTagName() + " should only have one element with relative path: ";
            for (String pathElement : path) {
                error += (pathElement + "/");
            }
            context.getErrors().add(error);
        }
        return elements.get(0);
    }

    /**
   * getter for the xmlManager property.
   * 
   * @return the xmlManager
   */
    public XmlManager getXmlManager() {
        return xmlManager;
    }

    /**
   * parse an element that represents an attribute.
   * 
   * @param context
   *          the generation context
   * @param root
   *          the element to inspect
   * @return the {@link AttributeModel} representing the attribute
   */
    public AttributeModel parseAttributeModel(final ModelGeneratorContext context, final Element root) {
        AttributeModel attributeModel = new AttributeModelImpl();
        fillCommonValue(context, root, attributeModel);
        for (Element tmpTypeElement : getXmlManager().getElements("UML:StructuralFeature.type", root.getChildNodes())) {
            for (Element typeElement : getXmlManager().getElements(tmpTypeElement.getChildNodes())) {
                attributeModel.setType(context.getObject(typeElement.getAttribute("xmi.idref")));
            }
        }
        for (Element initialElement : getElementsByPath(context, root, "UML:Attribute.initialValue", "UML:Expression")) {
            String initialValue = initialElement.getAttribute("body").trim();
            if ("".equals(initialValue)) {
                initialValue = null;
            }
            attributeModel.setInitialValue(initialValue);
        }
        return attributeModel;
    }

    /**
   * parse an element that represents a parameter.
   * 
   * @param context
   *          the generation context
   * @param root
   *          the element to inspect
   * @return the {@link AttributeModel} representing the parameter
   */
    public AttributeModel parseParameter(final ModelGeneratorContext context, final Element root) {
        AttributeModel attributeModel = new AttributeModelImpl();
        fillCommonValue(context, root, attributeModel);
        for (Element tmpTypeElement : getXmlManager().getElements("UML:Parameter.type", root.getChildNodes())) {
            for (Element typeElement : getXmlManager().getElements(tmpTypeElement.getChildNodes())) {
                attributeModel.setType(context.getObject(typeElement.getAttribute("xmi.idref")));
            }
        }
        return attributeModel;
    }

    /**
   * setter for the xmlManager property.
   * 
   * @param xmlManager
   *          the xmlManager to set
   */
    public void setXmlManager(XmlManager xmlManager) {
        this.xmlManager = xmlManager;
    }

    /**
   * returns the comment of the element given in parameter.
   * 
   * @param context
   *          the generation context
   * @param element
   *          to {@link Element} to retrieve the comments for
   * @return the comment associated to the given {@link Element}.
   */
    private String getComment(final ModelGeneratorContext context, final Element element) {
        for (Element taggedValue : getElementsByPath(context, element, "UML:ModelElement.taggedValue", "UML:TaggedValue")) {
            Element type = getUniqueElementByPath(context, taggedValue, "UML:TaggedValue.type", "UML:TagDefinition");
            if (!"documentation".equals(type.getAttribute("name"))) {
                continue;
            }
            for (Element data : getXmlManager().getElements("UML:TaggedValue.dataValue", taggedValue.getChildNodes())) {
                return getTextContent(data);
            }
        }
        return null;
    }

    /**
   * returns the qualified name of a the object represented by a {@link Element}
   * .
   * 
   * @param element
   *          the element to inspect
   * @return the qualified name of a the object represented by a {@link Element}
   */
    private String getQualifiedName(Element element) {
        if (element.getParentNode() == null || !(element.getParentNode() instanceof Element)) {
            return "";
        }
        String packageName = getQualifiedName((Element) element.getParentNode());
        if (!element.getTagName().equals("UML:Package")) {
            return packageName;
        }
        if (!"".equals(packageName)) {
            packageName += ".";
        }
        return packageName + element.getAttribute("name").trim();
    }

    /**
   * returns the tags of an {@link Element}.
   * 
   * @param context
   *          the generation context
   * @param element
   *          to {@link Element} to retrieve the values for
   * @return the {@link List} of tags of the {@link Element}
   */
    private List<StereotypeModel> getStereotypes(final ModelGeneratorContext context, final Element element) {
        List<StereotypeModel> res = new ArrayList<StereotypeModel>();
        for (Element taggedValue : getElementsByPath(context, element, "UML:ModelElement.comment", "UML:Comment")) {
            StereotypeModelImpl stereotype = new StereotypeModelImpl();
            stereotype.setName(taggedValue.getAttribute("name"));
            if (!Boolean.parseBoolean(taggedValue.getAttribute("body"))) {
                context.getErrors().add("An element " + taggedValue.getAttribute("name") + " is tagged with an old UML:Comment with a body not equals to <true>. Bailing out as this is no longer specified. Change the model to use only Stereotype.");
            } else {
                res.add(stereotype);
            }
        }
        for (Element stereotypeDefinitionElement : getElementsByPath(context, element, "UML:ModelElement.stereotype", "UML:Stereotype")) {
            StereotypeModel stereotype = new StereotypeModelImpl();
            stereotype.setName(stereotypeDefinitionElement.getAttribute("name"));
            res.add(stereotype);
            for (Element tagDefinition : getElementsByPath(context, stereotypeDefinitionElement, "UML:Stereotype.definedTag", "UML:TagDefinition")) {
                TagModel tagModel = new TagModelImpl();
                tagModel.setName(tagDefinition.getAttribute("name"));
                for (Element taggedValueElement : getElementsByPath(context, element, "UML:ModelElement.taggedValue", "UML:TaggedValue")) {
                    if (getElementsByPath(context, taggedValueElement, "UML:TaggedValue.type", "UML:TagDefinition").contains(tagDefinition)) {
                        tagModel.getValues().add(getTextContent(getUniqueElementByPath(context, taggedValueElement, "UML:TaggedValue.dataValue")));
                    }
                }
                if (tagModel.getValues() != null && !tagModel.getValues().isEmpty()) {
                    stereotype.addTag(tagModel);
                }
            }
        }
        return res;
    }

    /**
   * returns the text content inside an node in a XML tree.
   * 
   * @param element
   *          the node in the tree
   * @return the text content inside an node in a XML tree
   */
    private String getTextContent(Element element) {
        if (element.getChildNodes().getLength() == 0) {
            return null;
        }
        final String textContent = element.getChildNodes().item(0).getTextContent();
        if (textContent == null) {
            return null;
        }
        return textContent.trim();
    }

    /**
   * handle abstraction for object that can implements interfaces.
   * 
   * @param context
   *          the generation context
   * @param element
   *          to {@link Element} to retrieve the abstraction for
   * @param delegatableObject
   *          the object to add the abstractions to
   */
    protected void handleAbstraction(final ModelGeneratorContext context, final Element element, final AbstractDelegatableObject delegatableObject) {
        for (String[] path : Arrays.asList(new String[] { "UML:ModelElement.clientDependency", "UML:Abstraction", "UML:Dependency.supplier" }, new String[] { "UML:GeneralizableElement.generalization", "UML:Generalization", "UML:Generalization.parent" })) {
            for (Element abstraction : getElementsByPath(context, element, path)) {
                for (Element type : getXmlManager().getElements(abstraction.getChildNodes())) {
                    delegatableObject.addImplementsModel(context.getClasses().get(type.getAttribute("xmi.idref")));
                }
            }
        }
    }

    /**
   * handle generalization for classes.
   * 
   * @param context
   *          the generation context
   * @param element
   *          to {@link Element} to retrieve the abstraction for
   * @param classModel
   *          the object to add the generalization to
   */
    protected void handleGeneralization(final ModelGeneratorContext context, final Element element, final AbstractClassModel classModel) {
        for (Element abstraction : getElementsByPath(context, element, "UML:GeneralizableElement.generalization", "UML:Generalization", "UML:Generalization.parent")) {
            for (Element type : getXmlManager().getElements(abstraction.getChildNodes())) {
                AbstractClassModel parent = context.getClasses().get(type.getAttribute("xmi.idref"));
                if (classModel.getImplementationModel() != null) {
                    if (classModel.getImplementationModel().getParent() == null) {
                        classModel.getImplementationModel().setParent(parent.getImplementationModel());
                    } else {
                        context.getErrors().add("Class " + classModel.getImplementationModel().getQualifiedName() + " extends 2 classes: " + parent.getImplementationModel().getQualifiedName() + " and " + classModel.getParent().getQualifiedName() + ".");
                    }
                }
            }
        }
    }

    /**
   * handle operations for object that can have delegates.
   * 
   * @param context
   *          the generation context
   * @param element
   *          to {@link Element} to retrieve the operations for
   * @param delegatableObject
   *          the object to add the abstractions to
   */
    protected void handleOperation(final ModelGeneratorContext context, final Element element, final AbstractDelegatableObject delegatableObject) {
        for (Element operationElement : getElementsByPath(context, element, "UML:Classifier.feature", "UML:Operation")) {
            OperationModel operation = new OperationModelImpl();
            fillCommonValue(context, operationElement, operation);
            if (!StringUtils.hasText(operation.getComment())) {
                context.getErrors().add("Comment of operation " + operation.getName() + " of object " + delegatableObject.getQualifiedName() + " is missing.");
            }
            delegatableObject.addOperationModel(operation);
            for (Element parameterElement : getElementsByPath(context, operationElement, "UML:BehavioralFeature.parameter", "UML:Parameter")) {
                AttributeModel attribute = parseParameter(context, parameterElement);
                if (!(attribute.getType().getQualifiedName().equals("void") && attribute.getName().equals("return"))) {
                    if (!StringUtils.hasText(attribute.getComment())) {
                        context.getErrors().add("Parameter " + attribute.getName() + " of operation " + operation.getName() + " of object " + delegatableObject.getName() + " must have a comment.");
                    }
                }
                if (attribute.getName().equals("return")) {
                    operation.setReturnType(attribute);
                } else {
                    operation.addParameterModels(attribute);
                }
            }
            delegatableObject.addOperationModel(operation);
        }
    }
}
