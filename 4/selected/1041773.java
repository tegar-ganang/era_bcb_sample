package org.qtitools.mathqurate.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.imsglobal.xsd.imsqti_v2p1.AssessmentItemType;
import org.imsglobal.xsd.imsqti_v2p1.BaseTypeType;
import org.imsglobal.xsd.imsqti_v2p1.ChoiceInteractionType;
import org.imsglobal.xsd.imsqti_v2p1.CorrectResponseType;
import org.imsglobal.xsd.imsqti_v2p1.CustomInteractionType;
import org.imsglobal.xsd.imsqti_v2p1.DefaultValueType;
import org.imsglobal.xsd.imsqti_v2p1.EndAttemptInteractionType;
import org.imsglobal.xsd.imsqti_v2p1.FeedbackBlockType;
import org.imsglobal.xsd.imsqti_v2p1.FeedbackInlineType;
import org.imsglobal.xsd.imsqti_v2p1.InlineChoiceInteractionType;
import org.imsglobal.xsd.imsqti_v2p1.InlineChoiceType;
import org.imsglobal.xsd.imsqti_v2p1.ItemBodyType;
import org.imsglobal.xsd.imsqti_v2p1.PrintedVariableType;
import org.imsglobal.xsd.imsqti_v2p1.PromptType;
import org.imsglobal.xsd.imsqti_v2p1.ResponseProcessingType;
import org.imsglobal.xsd.imsqti_v2p1.SimpleChoiceType;
import org.imsglobal.xsd.imsqti_v2p1.TemplateBlockType;
import org.imsglobal.xsd.imsqti_v2p1.TemplateInlineType;
import org.imsglobal.xsd.imsqti_v2p1.TemplateProcessingType;
import org.imsglobal.xsd.imsqti_v2p1.TextEntryInteractionType;
import org.imsglobal.xsd.imsqti_v2p1.ValueType;
import org.qtitools.mathqurate.utilities.CPBuildException;
import org.qtitools.mathqurate.utilities.ZipHelper;
import org.qtitools.mathqurate.view.MQMain;
import org.w3._1998.math.mathml.MathType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A factory for creating Mathqurate objects.
 * Utility class to marshalling and unmarshalling
 * 
 * @author James Annesley <j.annesley@kingston.ac.uk>
 */
public final class MQObjectFactory implements org.xml.sax.ErrorHandler {

    /**
	 * Instantiates a new mQ object factory.
	 */
    public MQObjectFactory() {
    }

    private Object getJAXBElement(String sXML) {
        JAXBElement<?> jaxbe = null;
        try {
            InputSource inputSource = new InputSource(new StringReader(sXML));
            inputSource.setEncoding("UTF-8");
            jaxbe = (JAXBElement<?>) MQModel.qtiCf.unmarshal(MQModel.imsqtiUnmarshaller, inputSource);
        } catch (JAXBException e) {
            MQMain.logger.error("getJAXBElement", e);
        }
        return jaxbe.getValue();
    }

    private Object getJAXBElement(Document dom) {
        JAXBElement<?> jaxbe = null;
        try {
            jaxbe = (JAXBElement<?>) MQModel.imsqtiUnmarshaller.unmarshal(dom);
        } catch (JAXBException e) {
            MQMain.logger.error("getJAXBElement", e);
        }
        return jaxbe.getValue();
    }

    private Object getJAXBElement(Node node) {
        JAXBElement<?> jaxbe = null;
        try {
            jaxbe = (JAXBElement<?>) MQModel.imsqtiUnmarshaller.unmarshal(node);
        } catch (JAXBException e) {
            MQMain.logger.error("getJAXBElement", e);
        }
        return jaxbe.getValue();
    }

    private Document getDOM(JAXBElement<?> jaxbe) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setIgnoringElementContentWhitespace(true);
        Document dom = null;
        try {
            dom = dbf.newDocumentBuilder().newDocument();
            MQModel.imsqtiMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            try {
                dom = MQModel.qtiCf.marshal(MQModel.imsqtiMarshaller, jaxbe, dom);
            } catch (Exception e) {
                e.printStackTrace();
            }
            MQModel.imsqtiMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);
        } catch (ParserConfigurationException e) {
            MQMain.logger.error("getDOM", e);
        } catch (PropertyException e) {
            MQMain.logger.error("getDOM", e);
        } catch (JAXBException e) {
            MQMain.logger.error("getDOM", e);
        }
        return dom;
    }

    private Node getNode(Node node, JAXBElement<?> jaxbe) {
        try {
            MQModel.imsqtiMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            MQModel.imsqtiMarshaller.marshal(jaxbe, node);
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            MQModel.imsqtiMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);
        } catch (PropertyException e) {
            MQMain.logger.error("getNode", e);
        } catch (JAXBException e) {
            MQMain.logger.error("getNode", e);
        }
        return node;
    }

    private String getXML(JAXBElement<?> jaxbe) {
        String sXML = "";
        try {
            MQModel.imsqtiMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            sXML = MQModel.qtiCf.marshalToString(MQModel.imsqtiMarshaller, jaxbe);
            MQModel.imsqtiMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);
        } catch (Exception e) {
            MQMain.logger.error("getXML", e);
        }
        return sXML;
    }

    /**
	 * Creates a new FeedbackBlockType object from an DOM document of a feedbackBlock.
	 * 
	 * @param dom the DOM document
	 * 
	 * @return the feedback block type
	 * 
	 */
    public FeedbackBlockType createFeedbackBlockType(Document dom) {
        return (FeedbackBlockType) getJAXBElement(dom);
    }

    /**
	 * Creates a new FeedbackBlockType object from an DOM node of a feedbackBlock.
	 * 
	 * @param node the node
	 * 
	 * @return the feedback block type
	 * 
	 */
    public FeedbackBlockType createFeedbackBlockType(Node node) {
        return (FeedbackBlockType) getJAXBElement(node);
    }

    /**
	 * Creates a new FeedbackBlockType object from an XML string of a feedbackBlock.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the feedback block type
	 * 
	 */
    public FeedbackBlockType createFeedbackBlockType(String sXML) {
        return (FeedbackBlockType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new FeedbackBlockType object from an DOM node of a feedbackBlock.
	 * 
	 * @param node the node
	 * 
	 * @return the feedback block type
	 * 
	 */
    public TemplateBlockType createTemplateBlockType(Node node) {
        return (TemplateBlockType) getJAXBElement(node);
    }

    /**
	 * Creates a new FeedbackBlockType object from an XML string of a feedbackBlock.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the feedback block type
	 * 
	 */
    public TemplateBlockType createTemplateBlockType(String sXML) {
        return (TemplateBlockType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new FeedbackInlineType object from a DOM document of a feedbackInline.
	 * 
	 * @param dom the DOM document
	 * 
	 * @return the feedback inline type
	 * 
	 */
    public FeedbackInlineType createFeedbackInlineType(Document dom) {
        return (FeedbackInlineType) getJAXBElement(dom);
    }

    /**
	 * Creates a new TemplateInlineType object from a DOM document
	 * 
	 * @param dom the DOM document
	 * 
	 * @return the template inline type
	 * 
	 */
    public TemplateInlineType createTemplateInlineType(Document dom) {
        return (TemplateInlineType) getJAXBElement(dom);
    }

    /**
	 * Creates a new FeedbackInlineType object from a DOM node of a feedbackInline.
	 * 
	 * @param node the node
	 * 
	 * @return the feedback inline type
	 * 
	 */
    public FeedbackInlineType createFeedbackInlineType(Node node) {
        return (FeedbackInlineType) getJAXBElement(node);
    }

    /**
	 * Creates a new TemplateInlineType object from a DOM node
	 * 
	 * @param node the node
	 * 
	 * @return the template inline type
	 * 
	 */
    public TemplateInlineType createTemplateInlineType(Node node) {
        return (TemplateInlineType) getJAXBElement(node);
    }

    /**
	 * Creates a new FeedbackInlineType object from an XML string of a feedbackInline.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the feedback inline type
	 * 
	 */
    public FeedbackInlineType createFeedbackInlineType(String sXML) {
        return (FeedbackInlineType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new TemplateInlineType object from an XML string of a templateInline.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the template inline type
	 * 
	 */
    public TemplateInlineType createTemplateInlineType(String sXML) {
        return (TemplateInlineType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new AssessmentItemType object from a XML string of an assessmentItem.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the assessment item type
	 * 	
	 */
    public AssessmentItemType createAssessmentType(String sXML) {
        return (AssessmentItemType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new ItemBodyType object from a DOM of an itemBody.
	 * 
	 * @param dom the DOM document
	 * 
	 * @return the item body type
	 * 
	 */
    public ItemBodyType createItemBodyType(Document dom) {
        return (ItemBodyType) getJAXBElement(dom);
    }

    /**
	 * Creates a new ItemBodyType object from a XML string of an itemBody.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the item body type
	 * 	
	 */
    public ItemBodyType createItemBodyType(String sXML) {
        return (ItemBodyType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new MathType object from an XML string of a math.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the math type
	 * 
	 * @throws JAXBException Signals that an JAXBException has occurred.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public MathType createMathType(String sXML) throws JAXBException, IOException {
        MathType mathType;
        JAXBElement<?> jaxbe = null;
        jaxbe = (JAXBElement<?>) MQModel.mathmlUnmarshaller.unmarshal(new InputSource(new StringReader(sXML)));
        mathType = (MathType) jaxbe.getValue();
        return mathType;
    }

    /**
	 * Creates a new PromptType object from an DOM document of a prompt.
	 * 
	 * @param dom the DOM document
	 * 
	 * @return the prompt type
	 * 
	 */
    public PromptType createPromptType(Document dom) {
        return (PromptType) getJAXBElement(dom);
    }

    /**
	 * Creates a new PromptType object from an DOM node of a prompt.
	 * 
	 * @param node the node
	 * 
	 * @return the prompt type
	 * 
	 */
    public PromptType createPromptType(Node node) {
        return (PromptType) getJAXBElement(node);
    }

    /**
	 * Creates a new PromptType object from an XML string of a prompt.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the prompt type
	 * 
	 */
    public PromptType createPromptType(String sXML) {
        return (PromptType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new ResponseProcessingType object from an XML string of a responseProcessing.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the response processing type
	 * 
	 */
    public ResponseProcessingType createResponseProcessingType(String sXML) {
        return (ResponseProcessingType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new SimpleChoiceType object from a DOM document of a simpleChoice.
	 * 
	 * @param dom the DOM document
	 * 
	 * @return the simple choice type
	 * 
	 */
    public SimpleChoiceType createSimpleChoiceType(Document dom) {
        return (SimpleChoiceType) getJAXBElement(dom);
    }

    /**
	 * Creates a new InlineChoiceType object from a DOM document of a inlineChoice.
	 * 
	 * @param dom the DOM document
	 * 
	 * @return the inline choice type
	 * 
	 */
    public InlineChoiceType createInlineChoiceType(Document dom) {
        return (InlineChoiceType) getJAXBElement(dom);
    }

    /**
	 * Creates a new SimpleChoiceType object from a DOM node of a simpleChoice.
	 * 
	 * @param node the node
	 * 
	 * @return the simple choice type
	 * 
	 */
    public SimpleChoiceType createSimpleChoiceType(Node node) {
        return (SimpleChoiceType) getJAXBElement(node);
    }

    /**
	 * Creates a new SimpleChoiceType object from an XML string of a simpleChoice.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the simple choice type
	 * 	
	 */
    public SimpleChoiceType createSimpleChoiceType(String sXML) {
        return (SimpleChoiceType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new InlineChoiceType object from an XML string of an inlineChoice.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the inline choice type
	 * 	
	 */
    public InlineChoiceType createInlineChoiceType(String sXML) {
        return (InlineChoiceType) getJAXBElement(sXML);
    }

    /**
	 * Creates a new TemplateProcessingType object from an XML string of a templateProcessing.
	 * 
	 * @param sXML the XML string
	 * 
	 * @return the template processing type
	 * 
	 */
    public TemplateProcessingType createTemplateProcessingType(String sXML) {
        return (TemplateProcessingType) getJAXBElement(sXML);
    }

    public void error(SAXParseException exception) throws SAXException {
        exception.printStackTrace();
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        exception.printStackTrace();
    }

    /**
	 * Gets the choice interaction type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the choice interaction type
	 * 
	
	 */
    public ChoiceInteractionType getChoiceInteractionType(Node node) {
        return (ChoiceInteractionType) getJAXBElement(node);
    }

    /**
	 * Gets a correct response from a MQCorrectResponse.
	 * 
	 * @param MQCorrectResponse the correct response Helper
	 * 
	 * @return CorrectResponseType the correct response type
	 */
    public CorrectResponseType getCorrectResponse(MQCorrectResponse correctResponseHelper) {
        CorrectResponseType correctResponseType = null;
        if (correctResponseHelper != null) {
            correctResponseType = MQModel.imsqtiObjectFactory.createCorrectResponseType();
            correctResponseType.setInterpretation(correctResponseHelper.getInterpretation());
            for (MQValue mqvalue : correctResponseHelper.getArrayList()) {
                ValueType valueType = MQModel.imsqtiObjectFactory.createValueType();
                String sFieldIdentifier = mqvalue.getFieldIdentifier();
                if (sFieldIdentifier != null) {
                    sFieldIdentifier = sFieldIdentifier.trim();
                    if (sFieldIdentifier.length() > 0) {
                        valueType.setFieldIdentifier(sFieldIdentifier);
                    }
                }
                String sValue = mqvalue.getValue();
                if (sValue != null) {
                    sValue = sValue.trim();
                    if (sValue.length() == 0) {
                        sValue = "0";
                    }
                    valueType.setValue(sValue);
                }
                String sBaseType = mqvalue.getBaseType();
                if (sBaseType != null) {
                    sBaseType = sBaseType.toUpperCase();
                    boolean found = false;
                    for (BaseTypeType btemp : BaseTypeType.values()) {
                        if (btemp.toString().equals(sBaseType)) {
                            found = true;
                        }
                        if (found) {
                            BaseTypeType bt = BaseTypeType.valueOf(sBaseType);
                            valueType.setBaseType(bt);
                        }
                    }
                }
                correctResponseType.getValue().add(valueType);
            }
        }
        return correctResponseType;
    }

    /**
	 * Gets the custom interaction type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the custom interaction type	
	 */
    public CustomInteractionType getCustomInteractionType(Node node) {
        return (CustomInteractionType) getJAXBElement(node);
    }

    /**
	 * Gets a default value from a MQDefault.
	 * 
	 * @param MQDefaultValue the default Value Helper
	 * 
	 * @return DefaultValueType the default Value type
	 */
    public DefaultValueType getDefaultValue(MQDefaultValue defaultValueHelper) {
        DefaultValueType defaultValueType = null;
        if (defaultValueHelper != null) {
            defaultValueType = MQModel.imsqtiObjectFactory.createDefaultValueType();
            defaultValueType.setInterpretation(defaultValueHelper.getInterpretation());
            for (MQValue mqvalue : defaultValueHelper.getArrayList()) {
                ValueType valueType = MQModel.imsqtiObjectFactory.createValueType();
                String sFieldIdentifier = mqvalue.getFieldIdentifier();
                if (sFieldIdentifier != null) {
                    sFieldIdentifier = sFieldIdentifier.trim();
                    if (sFieldIdentifier.length() > 0) {
                        valueType.setFieldIdentifier(sFieldIdentifier);
                    }
                }
                String sValue = mqvalue.getValue();
                if (sValue != null) {
                    sValue = sValue.trim();
                    if (sValue.length() == 0) {
                        sValue = "0";
                    }
                    valueType.setValue(sValue);
                }
                String sBaseType = mqvalue.getBaseType();
                if (sBaseType != null) {
                    sBaseType = sBaseType.toUpperCase();
                    boolean found = false;
                    for (BaseTypeType btemp : BaseTypeType.values()) {
                        if (btemp.toString().equals(sBaseType)) {
                            found = true;
                        }
                        if (found) {
                            BaseTypeType bt = BaseTypeType.valueOf(sBaseType);
                            valueType.setBaseType(bt);
                        }
                    }
                }
                defaultValueType.getValue().add(valueType);
            }
        }
        return defaultValueType;
    }

    /**
	 * Gets the end attempt interaction type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the end attempt interaction type
	 * 	
	 */
    public EndAttemptInteractionType getEndAttemptInteractionType(Node node) {
        return (EndAttemptInteractionType) getJAXBElement(node);
    }

    /**
	 * Gets the feedback block type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the feedback block type 
	
	 */
    public FeedbackBlockType getFeedbackBlockType(Node node) {
        return (FeedbackBlockType) getJAXBElement(node);
    }

    /**
	 * Gets the template block type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the template block type 
	
	 */
    public TemplateBlockType getTemplateBlockType(Node node) {
        return (TemplateBlockType) getJAXBElement(node);
    }

    /**
	 * Gets the feedback inline type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the feedback inline type
	 * 
	
	 */
    public FeedbackInlineType getFeedbackInlineType(Node node) {
        return (FeedbackInlineType) getJAXBElement(node);
    }

    /**
	 * Gets a templateInlineType from a node
	 * @param node
	 * @return
	 */
    public TemplateInlineType getTemplateInlineType(Node node) {
        return (TemplateInlineType) getJAXBElement(node);
    }

    /**
	 * Gets the math type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the math type
	 * 
	 @throws JAXBException the JAXB exception
	 */
    public MathType getMathType(Node node) throws JAXBException {
        MathType mathml;
        JAXBElement<?> jaxbe = null;
        jaxbe = (JAXBElement<?>) MQModel.imsqtiUnmarshaller.unmarshal(node);
        mathml = (MathType) jaxbe.getValue();
        return mathml;
    }

    /**
	 * Gets the Correct Response Type as a Mathqurate helper correct response.
	 * 
	 * @param type the Correct Value Type
	 * 
	 * @return MQCorrectResponse the Mathqurate correct response
	 * 
	 */
    public MQCorrectResponse getMQCorrectResponse(CorrectResponseType type) {
        MQCorrectResponse correctResponseHelper = null;
        if (type != null) {
            correctResponseHelper = new MQCorrectResponse();
            correctResponseHelper.setInterpretation(type.getInterpretation());
            for (ValueType v : type.getValue()) {
                MQValue value = new MQValue();
                BaseTypeType bt = v.getBaseType();
                if (bt != null) {
                    value.setBaseType(bt.value());
                }
                String fieldid = v.getFieldIdentifier();
                if (fieldid != null) {
                    value.setFieldIdentifier(fieldid);
                }
                String val = v.getValue();
                if (val != null) {
                    value.setValue(val);
                }
                correctResponseHelper.getArrayList().add(value);
            }
        }
        return correctResponseHelper;
    }

    /**
	 * Gets the default value type as a Mathqurate helper default type.
	 * 
	 * @param type the Default Value Type
	 * 
	 * @return MQDefaultValue the Mathqurate default value
	 * 
	 */
    public MQDefaultValue getMQDefaultValue(DefaultValueType type) {
        MQDefaultValue defaultValueHelper = null;
        if (type != null) {
            defaultValueHelper = new MQDefaultValue();
            defaultValueHelper.setInterpretation(type.getInterpretation());
            for (ValueType v : type.getValue()) {
                MQValue value = new MQValue();
                BaseTypeType bt = v.getBaseType();
                if (bt != null) {
                    value.setBaseType(bt.value());
                }
                String fieldid = v.getFieldIdentifier();
                if (fieldid != null) {
                    value.setFieldIdentifier(fieldid);
                }
                String val = v.getValue();
                if (val != null) {
                    value.setValue(val);
                }
                defaultValueHelper.getArrayList().add(value);
            }
        }
        return defaultValueHelper;
    }

    /**
	 * Gets the prompt type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the prompt type
	 * 
	
	 */
    public PromptType getPromptType(Node node) {
        return (PromptType) getJAXBElement(node);
    }

    /**
	 * Gets the simple choice type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the simple choice type
	 * 	
	 */
    public SimpleChoiceType getSimpleChoiceType(Node node) {
        return (SimpleChoiceType) getJAXBElement(node);
    }

    /**
	 * Gets the text entry interaction type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the text entry interaction type
	 * 	
	 */
    public TextEntryInteractionType getTextEntryInteractionType(Node node) {
        return (TextEntryInteractionType) getJAXBElement(node);
    }

    /**
	 * Gets the inline choice interaction type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the inline choice interaction type
	 * 	
	 */
    public InlineChoiceInteractionType getInlineChoiceInteractionType(Node node) {
        return (InlineChoiceInteractionType) getJAXBElement(node);
    }

    /**
	 * Gets the inline choice type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the inline choice type
	 * 	
	 */
    public InlineChoiceType getInlineChoiceType(Node node) {
        return (InlineChoiceType) getJAXBElement(node);
    }

    /**
	 * Gets the printed variable type from a DOM node.
	 * 
	 * @param node the node
	 * 
	 * @return the printed variable type
	 * 
	
	 */
    public PrintedVariableType getPrintedVariableType(Node node) {
        return (PrintedVariableType) getJAXBElement(node);
    }

    /**
	 * 
	 * 
	 * Creates a DOM document of an assessmentItemType
	 * 
	 * @return the type as a DOM document
	 * 
	 */
    public Document getTypeAsDOM(AssessmentItemType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "assessmentItem");
            JAXBElement<AssessmentItemType> jaxbe = new JAXBElement<AssessmentItemType>(qname, AssessmentItemType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets a ChoiceInteractionType as a DOM document.
	 * 
	 * @param type the ChoiceInteractionType
	 * 
	 * @return the type as a DOM document
	 * 
	 */
    public Document getTypeAsDOM(ChoiceInteractionType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "choiceInteraction");
            JAXBElement<ChoiceInteractionType> jaxbe = new JAXBElement<ChoiceInteractionType>(qname, ChoiceInteractionType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets a InlineChoiceInteractionType as a DOM document.
	 * 
	 * @param type the InlineChoiceInteractionType
	 * 
	 * @return the type as a DOM document
	 * 
	 */
    public Document getTypeAsDOM(InlineChoiceInteractionType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "inlineChoiceInteraction");
            JAXBElement<InlineChoiceInteractionType> jaxbe = new JAXBElement<InlineChoiceInteractionType>(qname, InlineChoiceInteractionType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the CustomInteractionType as a DOM document.
	 * 
	 * @param type the CustomInteractionType
	 * 
	 * @return the CustomInteractionType as a DOM document
	 * 
	 */
    public Document getTypeAsDOM(CustomInteractionType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "customInteraction");
            JAXBElement<CustomInteractionType> jaxbe = new JAXBElement<CustomInteractionType>(qname, CustomInteractionType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the EndAttemptInteractionType as a DOM document.
	 * 
	 * @param type the EndAttemptInteractionType
	 * 
	 * @return the type as a DOM document
	 * 
	 */
    public Document getTypeAsDOM(EndAttemptInteractionType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "endAttemptInteraction");
            JAXBElement<EndAttemptInteractionType> jaxbe = new JAXBElement<EndAttemptInteractionType>(qname, EndAttemptInteractionType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the feedbackBlock as a DOM document.
	 * 
	 * @param type the feedbackBlock
	 * 
	 * @return the type as a DOM document
	 */
    public Document getTypeAsDOM(FeedbackBlockType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "feedbackBlock");
            JAXBElement<FeedbackBlockType> jaxbe = new JAXBElement<FeedbackBlockType>(qname, FeedbackBlockType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the templateBlock as a DOM document.
	 * 
	 * @param type the templateBlock
	 * 
	 * @return the type as a DOM document
	 */
    public Document getTypeAsDOM(TemplateBlockType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "templateBlock");
            JAXBElement<TemplateBlockType> jaxbe = new JAXBElement<TemplateBlockType>(qname, TemplateBlockType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the FeedbackInlineType as a DOM document.
	 * 
	 * @param type the FeedbackInlineType
	 * 
	 * @return the type as a DOM document
	 */
    public Document getTypeAsDOM(FeedbackInlineType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "feedbackInline");
            JAXBElement<FeedbackInlineType> jaxbe = new JAXBElement<FeedbackInlineType>(qname, FeedbackInlineType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the TemplateInlineType as a DOM document.
	 * 
	 * @param type the TemplateInlineType
	 * 
	 * @return the type as a DOM document
	 */
    public Document getTypeAsDOM(TemplateInlineType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "templateInline");
            JAXBElement<TemplateInlineType> jaxbe = new JAXBElement<TemplateInlineType>(qname, TemplateInlineType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the ItemBodyType as a DOM document.
	 * 
	 * @param type the ItemBodyType
	 * 
	 * @return the type as a DOM document
	 */
    public Document getTypeAsDOM(ItemBodyType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "itemBody");
            JAXBElement<ItemBodyType> jaxbe = new JAXBElement<ItemBodyType>(qname, ItemBodyType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the MathType as a DOM document.
	 * 
	 * @param type the MathType
	 * 
	 * @return the type as a DOM document
	 * 
	 * @throws JAXBException the JAXB exception
	 * @throws ParserConfigurationException the parser configuration exception
	 */
    public Document getTypeAsDOM(MathType type) throws JAXBException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = null;
        doc = dbf.newDocumentBuilder().newDocument();
        if (type != null) {
            QName qname = new QName("http://www.w3.org/1998/Math/MathML", "math");
            JAXBElement<MathType> jaxbe = new JAXBElement<MathType>(qname, MathType.class, type);
            MQModel.mathmlMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            MQModel.mathmlMarshaller.marshal(jaxbe, doc);
            try {
            } catch (Exception e) {
            }
            MQModel.mathmlMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);
        }
        return doc;
    }

    /**
	 * Gets the PromptType as a DOM document.
	 * 
	 * @param type the PromptType
	 * 
	 * @return the PromptType as a DOM document
	 */
    public Document getTypeAsDOM(PromptType type) {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "prompt");
            JAXBElement<PromptType> jaxbe = new JAXBElement<PromptType>(qname, PromptType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the SimpleChoiceType as a DOM document.
	 * 
	 * @param type the SimpleChoiceType
	 * 
	 * @return the SimpleChoiceType as a DOM document
	 */
    public Document getTypeAsDOM(SimpleChoiceType type) throws ParserConfigurationException, JAXBException {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "simpleChoice");
            JAXBElement<SimpleChoiceType> jaxbe = new JAXBElement<SimpleChoiceType>(qname, SimpleChoiceType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the InlineChoiceType as a DOM document.
	 * 
	 * @param type the InlineChoiceType
	 * 
	 * @return the InlineChoiceType as a DOM document
	 */
    public Document getTypeAsDOM(InlineChoiceType type) throws ParserConfigurationException, JAXBException {
        Document dom = null;
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "inlineChoice");
            JAXBElement<InlineChoiceType> jaxbe = new JAXBElement<InlineChoiceType>(qname, InlineChoiceType.class, type);
            dom = getDOM(jaxbe);
        }
        return dom;
    }

    /**
	 * Gets the ChoiceInteractionType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the ChoiceInteractionType
	 * 
	 * @return the ChoiceInteractionType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, ChoiceInteractionType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "choiceInteraction");
            JAXBElement<ChoiceInteractionType> jaxbe = new JAXBElement<ChoiceInteractionType>(qname, ChoiceInteractionType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the InlineChoiceInteractionType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the InlineChoiceInteractionType
	 * 
	 * @return the InlineChoiceInteractionType as node
	 */
    public Node getTypeAsNode(Node node, InlineChoiceInteractionType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "inlineChoiceInteraction");
            JAXBElement<InlineChoiceInteractionType> jaxbe = new JAXBElement<InlineChoiceInteractionType>(qname, InlineChoiceInteractionType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the CustomInteractionType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the CustomInteractionType
	 * 
	 * @return the CustomInteractionType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, CustomInteractionType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "customInteraction");
            JAXBElement<CustomInteractionType> jaxbe = new JAXBElement<CustomInteractionType>(qname, CustomInteractionType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the EndAttemptInteractionType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the EndAttemptInteractionType
	 * 
	 * @return the EndAttemptInteractionType as node
	 */
    public Node getTypeAsNode(Node node, EndAttemptInteractionType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "endAttemptInteraction");
            JAXBElement<EndAttemptInteractionType> jaxbe = new JAXBElement<EndAttemptInteractionType>(qname, EndAttemptInteractionType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the FeedbackBlockType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the FeedbackBlockType
	 * 
	 * @return the FeedbackBlockType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, FeedbackBlockType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "feedbackBlock");
            JAXBElement<FeedbackBlockType> jaxbe = new JAXBElement<FeedbackBlockType>(qname, FeedbackBlockType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the TemplateBlockType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the FeedbackBlockType
	 * 
	 * @return the FeedbackBlockType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, TemplateBlockType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "templateBlock");
            JAXBElement<TemplateBlockType> jaxbe = new JAXBElement<TemplateBlockType>(qname, TemplateBlockType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the FeedbackInlineType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the FeedbackInlineType
	 * 
	 * @return the type as node
	 * 
	 */
    public Node getTypeAsNode(Node node, FeedbackInlineType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "feedbackInline");
            JAXBElement<FeedbackInlineType> jaxbe = new JAXBElement<FeedbackInlineType>(qname, FeedbackInlineType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the TemplateInlineType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the TemplateInlineType
	 * 
	 * @return the type as node
	 * 
	 */
    public Node getTypeAsNode(Node node, TemplateInlineType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "templateInline");
            JAXBElement<TemplateInlineType> jaxbe = new JAXBElement<TemplateInlineType>(qname, TemplateInlineType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the MathType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the MathType
	 * 
	 * @return the MathType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, MathType type) {
        if (type != null) {
            QName qname = new QName("http://www.w3.org/1998/Math/MathML", "math");
            JAXBElement<MathType> jaxbe = new JAXBElement<MathType>(qname, MathType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the PromptType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the PromptType
	 * 
	 * @return the PromptType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, PromptType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "prompt");
            JAXBElement<PromptType> jaxbe = new JAXBElement<PromptType>(qname, PromptType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the SimpleChoiceType as a DOM 
	 * node.
	 * 
	 * @param node the node
	 * @param type the SimpleChoiceType
	 * 
	 * @return the SimpleChoiceType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, SimpleChoiceType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "simpleChoice");
            JAXBElement<SimpleChoiceType> jaxbe = new JAXBElement<SimpleChoiceType>(qname, SimpleChoiceType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the InlineChoiceType as a DOM 
	 * node.
	 * 
	 * @param node the node
	 * @param type the InlineChoiceType
	 * 
	 * @return the InlineChoiceType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, InlineChoiceType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "inlineChoice");
            JAXBElement<InlineChoiceType> jaxbe = new JAXBElement<InlineChoiceType>(qname, InlineChoiceType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the TextEntryInteractionType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the TextEntryInteractionType
	 * 
	 * @return the TextEntryInteractionType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, TextEntryInteractionType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "textEntryInteraction");
            JAXBElement<TextEntryInteractionType> jaxbe = new JAXBElement<TextEntryInteractionType>(qname, TextEntryInteractionType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the PrintedVariableType as a DOM node.
	 * 
	 * @param node the node
	 * @param type the PrintedVariableType
	 * 
	 * @return the PrintedVariableType as node
	 * 
	 */
    public Node getTypeAsNode(Node node, PrintedVariableType type) {
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "printedVariable");
            JAXBElement<PrintedVariableType> jaxbe = new JAXBElement<PrintedVariableType>(qname, PrintedVariableType.class, type);
            node = getNode(node, jaxbe);
        }
        return node;
    }

    /**
	 * Gets the AssessmentItemType as an XML string.
	 * 
	 * @param assessmentItemType the assessment item type
	 * 
	 * @return the type as an XML string
	 * 
	 */
    public String getTypeAsXML(AssessmentItemType assessmentItemType) {
        QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "assessmentItem");
        JAXBElement<AssessmentItemType> jaxbe = new JAXBElement<AssessmentItemType>(qname, AssessmentItemType.class, assessmentItemType);
        return getXML(jaxbe);
    }

    /**
	 * Gets the ChoiceInteractionType as an XML string.
	 * 
	 * @param type the ChoiceInteractionType
	 * 
	 * @return the type as an XML string
	 * 
	 */
    public String getTypeAsXML(ChoiceInteractionType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "choiceInteraction");
            JAXBElement<ChoiceInteractionType> jaxbe = new JAXBElement<ChoiceInteractionType>(qname, ChoiceInteractionType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the FeedbackBlockType as an XML string.
	 * 
	 * @param type the FeedbackBlockType
	 * 
	 * @return the FeedbackBlockType as an XML string
	 * 
	 */
    public String getTypeAsXML(FeedbackBlockType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "feedbackBlock");
            JAXBElement<FeedbackBlockType> jaxbe = new JAXBElement<FeedbackBlockType>(qname, FeedbackBlockType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the TemplateBlockType as an XML string.
	 * 
	 * @param type the TemplateBlockType
	 * 
	 * @return the TemplateBlockType as an XML string
	 * 
	 */
    public String getTypeAsXML(TemplateBlockType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "templateBlock");
            JAXBElement<TemplateBlockType> jaxbe = new JAXBElement<TemplateBlockType>(qname, TemplateBlockType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the FeedbackInlineType as an XML string.
	 * 
	 * @param type the FeedbackInlineType
	 * 
	 * @return the FeedbackInlineType as an XML string
	 */
    public String getTypeAsXML(FeedbackInlineType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "feedbackInline");
            JAXBElement<FeedbackInlineType> jaxbe = new JAXBElement<FeedbackInlineType>(qname, FeedbackInlineType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the TemplateInlineType as an XML string.
	 * 
	 * @param type the TemplateInlineType
	 * 
	 * @return the TemplateInlineType as an XML string
	 */
    public String getTypeAsXML(TemplateInlineType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "templateInline");
            JAXBElement<TemplateInlineType> jaxbe = new JAXBElement<TemplateInlineType>(qname, TemplateInlineType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the ItemBodyType as an XML string.
	 * 
	 * @param type the ItemBodyType
	 * 
	 * @return the type as an XML string
	 * 
	 */
    public String getTypeAsXML(ItemBodyType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "itemBody");
            JAXBElement<ItemBodyType> jaxbe = new JAXBElement<ItemBodyType>(qname, ItemBodyType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the MathType as an XML string.
	 * 
	 * @param type the MathType
	 * 
	 * @return the MathType as an XML string
	 * 
	 * @throws JAXBException the JAXB exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public String getTypeAsXML(MathType type) throws JAXBException, IOException {
        StringWriter sw = null;
        String sXML = "";
        if (type != null) {
            sw = new StringWriter();
            QName qname = new QName("http://www.w3.org/1998/Math/MathML", "math");
            JAXBElement<MathType> jaxbe = new JAXBElement<MathType>(qname, MathType.class, type);
            MQModel.mathmlMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            MQModel.mathmlMarshaller.marshal(jaxbe, sw);
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            MQModel.mathmlMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, false);
            sXML = sw.toString();
            sw.close();
        }
        return sXML;
    }

    /**
	 * Gets the PromptType as an XML string.
	 * 
	 * @param type the PromptType
	 * 
	 * @return the PromptType as an XML string
	 * 
	 */
    public String getTypeAsXML(PromptType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "prompt");
            JAXBElement<PromptType> jaxbe = new JAXBElement<PromptType>(qname, PromptType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the ResponseProcessingType as an XML string.
	 * 
	 * @param type the ResponseProcessingType
	 * 
	 * @return the ResponseProcessingType as an XML string
	 * 
	 */
    public String getTypeAsXML(ResponseProcessingType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "responseProcessing");
            JAXBElement<ResponseProcessingType> jaxbe = new JAXBElement<ResponseProcessingType>(qname, ResponseProcessingType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the SimpleChoiceType as an XML string.
	 * 
	 * @param type the SimpleChoiceType
	 * 
	 * @return the SimpleChoiceType as an XML string
	 * 
	 */
    public String getTypeAsXML(SimpleChoiceType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "simpleChoice");
            JAXBElement<SimpleChoiceType> jaxbe = new JAXBElement<SimpleChoiceType>(qname, SimpleChoiceType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the InlineChoiceType as an XML string.
	 * 
	 * @param type the InlineChoiceType
	 * 
	 * @return the InlineChoiceType as an XML string
	 * 
	 */
    public String getTypeAsXML(InlineChoiceType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "inlineChoice");
            JAXBElement<InlineChoiceType> jaxbe = new JAXBElement<InlineChoiceType>(qname, InlineChoiceType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the TemplateProcessingType as an XML string.
	 * 
	 * @param type the TemplateProcessingType
	 * 
	 * @return the TemplateProcessingType as an XML string
	 * 
	 */
    public String getTypeAsXML(TemplateProcessingType type) {
        String sXML = "";
        if (type != null) {
            QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "templateProcessing");
            JAXBElement<TemplateProcessingType> jaxbe = new JAXBElement<TemplateProcessingType>(qname, TemplateProcessingType.class, type);
            sXML = getXML(jaxbe);
        }
        return sXML;
    }

    /**
	 * Gets the assessmentItem from a file, in the form of a string filename.
	 * 
	 * @param filename the filename
	 * 
	 * @return the assessment item type
	 *
	 */
    public AssessmentItemType getAssessmentItemType(String filename) {
        if (filename.contains(" ") && (System.getProperty("os.name").contains("Windows"))) {
            File source = new File(filename);
            String tempDir = System.getenv("TEMP");
            File dest = new File(tempDir + "/temp.xml");
            MQMain.logger.info("Importing from " + dest.getAbsolutePath());
            FileChannel in = null, out = null;
            try {
                in = new FileInputStream(source).getChannel();
                out = new FileOutputStream(dest).getChannel();
                long size = in.size();
                MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                out.write(buf);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (out != null) try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                filename = tempDir + "/temp.xml";
            }
        }
        AssessmentItemType assessmentItemType = null;
        JAXBElement<?> jaxbe = null;
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            ChangeNamespace convertfromv2p0tov2p1 = new ChangeNamespace(reader, "http://www.imsglobal.org/xsd/imsqti_v2p0", "http://www.imsglobal.org/xsd/imsqti_v2p1");
            SAXSource source = null;
            try {
                FileInputStream fis = new FileInputStream(filename);
                InputStreamReader isr = null;
                try {
                    isr = new InputStreamReader(fis, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }
                InputSource is = new InputSource(isr);
                source = new SAXSource(convertfromv2p0tov2p1, is);
            } catch (FileNotFoundException e) {
                MQMain.logger.error("SAX/getAssessmentItemType/file not found");
            }
            jaxbe = (JAXBElement<?>) MQModel.qtiCf.unmarshal(MQModel.imsqtiUnmarshaller, source);
            assessmentItemType = (AssessmentItemType) jaxbe.getValue();
        } catch (JAXBException e) {
            MQMain.logger.error("JAX/getAssessmentItemType", e);
        } catch (SAXException e) {
            MQMain.logger.error("SAX/getAssessmentItemType", e);
        }
        return assessmentItemType;
    }

    /**
	 * Saves the AssessmentItemType as a File.
	 * 
	 * @param type the assessment Item Type
	 * @param filename the filename
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JAXBException the JAXB exception
	 */
    public void getTypeAsFile(AssessmentItemType type, String filename) throws IOException, JAXBException {
        QName qname = new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "assessmentItem");
        JAXBElement<AssessmentItemType> jaxbe = new JAXBElement<AssessmentItemType>(qname, AssessmentItemType.class, type);
        try {
            MQModel.qtiCf.marshal(MQModel.imsqtiMarshaller, jaxbe, new File(filename));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sets the PromptType as an XML string.
	 * 
	 * @param type the PromptType
	 * @param sXML the XML string
	 * 
	 */
    public void setTypeXML(PromptType type, String sXML) {
        if (sXML.equals("")) {
            return;
        }
        type = (PromptType) getJAXBElement(sXML);
    }

    /**
	 * Sets the type as an XML string.
	 * 
	 * @param type the type
	 * @param sXML the XML string
	 * 
	 */
    public void setTypeXML(ResponseProcessingType type, String sXML) {
        if (sXML.equals("")) {
            return;
        }
        type = (ResponseProcessingType) getJAXBElement(sXML);
    }

    /**
	 * Sets the type as an XML string.
	 * 
	 * @param type the type
	 * @param sXML the XML string
	 * 
	 */
    public void setTypeXML(TemplateProcessingType type, String sXML) {
        if (sXML.equals("")) {
            return;
        }
        type = (TemplateProcessingType) getJAXBElement(sXML);
    }

    public void warning(SAXParseException e) throws SAXException {
        MQMain.logger.error("MQObjectFactory", e);
    }

    class ChangeNamespace extends XMLFilterImpl {

        /** The old URI, to replace */
        private String oldURI;

        /** The new URI, to replace the old URI with */
        private String newURI;

        public ChangeNamespace(XMLReader reader, String oldURI, String newURI) {
            super(reader);
            this.oldURI = oldURI;
            this.newURI = newURI;
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (uri.equals(oldURI)) {
                super.startPrefixMapping(prefix, newURI);
            } else {
                super.startPrefixMapping(prefix, uri);
            }
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.equals(oldURI)) {
                super.startElement(newURI, localName, qName, attributes);
            } else {
                super.startElement(uri, localName, qName, attributes);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (uri.equals(oldURI)) {
                super.endElement(newURI, localName, qName);
            } else {
                super.endElement(uri, localName, qName);
            }
        }
    }

    public MQContentPackage getCPfromZip(File zipfile) throws CPBuildException {
        String systemTemp = System.getProperty("java.io.tmpdir");
        File unzipLocation = new File(systemTemp + "/" + "tempunzip");
        try {
            ZipHelper.unzip(zipfile, unzipLocation);
        } catch (IOException e) {
            ZipHelper.deleteDirectory(unzipLocation);
            throw new CPBuildException(CPBuildException.ErrorType.IOEXCEPTION);
        }
        File manifest = new File(unzipLocation + "/imsmanifest.xml");
        if (!manifest.exists()) {
            ZipHelper.deleteDirectory(unzipLocation);
            throw new CPBuildException(CPBuildException.ErrorType.NOMANIFEST);
        }
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = null;
        Document doc = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.parse(manifest);
        } catch (Exception e) {
            ZipHelper.deleteDirectory(unzipLocation);
            throw new CPBuildException(CPBuildException.ErrorType.BADMANIFEST);
        }
        Element element = doc.getDocumentElement();
        XPathFactory xpfactory = XPathFactory.newInstance();
        XPath xpath = xpfactory.newXPath();
        NamespaceContext ncImsCp = new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                return ("http://www.imsglobal.org/xsd/imscp_v1p1");
            }

            public String getPrefix(String namespaceURI) {
                return "cp";
            }

            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        };
        xpath.setNamespaceContext(ncImsCp);
        XPathExpression expr = null;
        NodeList resources = null;
        try {
            expr = xpath.compile("//cp:resource");
            resources = (NodeList) expr.evaluate(element, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
        }
        if (resources.getLength() != 1) {
            ZipHelper.deleteDirectory(unzipLocation);
            throw new CPBuildException(CPBuildException.ErrorType.RESNOWRONG);
        }
        LinkedHashMap<String, String> metadata = MQContentPackage.metadataFromFile(unzipLocation + "/imsmanifest.xml");
        String qtiFilename = metadata.get(MQMetadata.FILENAME[0]);
        qtiFilename = unzipLocation + "/" + qtiFilename;
        AssessmentItemType assItem = getAssessmentItemType(qtiFilename);
        MQContentPackage mqCp = new MQContentPackage(assItem);
        mqCp.setMetadataMap(metadata);
        ZipHelper.deleteDirectory(unzipLocation);
        return mqCp;
    }

    public void makeZipFromCP(MQContentPackage cp, String zipFilename) {
        makeZipFromCP(cp, new File(zipFilename));
    }

    /**
	 * Makes a zipfile from the content package
	 * @param cp content package
	 * @param zipfile zipfile
	 */
    public void makeZipFromCP(MQContentPackage cp, File zipfile) {
        String systemTemp = System.getProperty("java.io.tmpdir");
        String zipLocation = systemTemp + "/tempzip";
        new File(zipLocation).mkdir();
        String filename = cp.get(MQMetadata.FILENAME[0]);
        filename = filename.replaceAll("\\\\", "/");
        String[] x = filename.split("/");
        filename = x[x.length - 1];
        filename = filename.trim();
        if (filename.equals("")) filename = "qtiQuestion.xml";
        cp.set(MQMetadata.FILENAME[0], filename);
        try {
            getTypeAsFile(cp.getAssessmentItemType(), zipLocation + "/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        String manifest = cp.metadataToXml();
        FileOutputStream fos = null;
        OutputStreamWriter out = null;
        try {
            fos = new FileOutputStream(zipLocation + "/" + "imsmanifest.xml");
            out = new OutputStreamWriter(fos, "UTF-8");
            out.write(manifest);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
        try {
            ZipHelper.zipDirectory(new File(zipLocation), zipfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ZipHelper.deleteDirectory(zipLocation);
    }
}
