package org.ikasan.testing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.Assert;
import org.ikasan.attributes.AttributeResolver;
import org.ikasan.attributes.FirstStrikeAttributeResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Base test class for testing exception XML files against AttributeResolver configuration
 * 
 * @author Ikasan Development Team
 *
 */
public class BaseAttributeResolverConfigTest implements BeanFactoryAware {

    private FirstStrikeAttributeResolver firstStrikeAttributeResolver;

    private DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    private DocumentBuilder documentBuilder;

    public BaseAttributeResolverConfigTest() {
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            Assert.fail(e.getMessage());
        }
    }

    public Map<String, Object> resolveAttributesForErrorFile(String errorFileName) {
        byte[] bytes = null;
        try {
            bytes = loadFile(errorFileName);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Document errorOccurrenceDocument = null;
        try {
            errorOccurrenceDocument = documentBuilder.parse(new ByteArrayInputStream(bytes));
        } catch (SAXException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Element errorOccurrenceElement = (Element) errorOccurrenceDocument.getFirstChild();
        Map<String, Object> resolvedAttributes = firstStrikeAttributeResolver.resolveAttributes(errorOccurrenceElement);
        return resolvedAttributes;
    }

    protected byte[] loadFile(String fileName) throws IOException {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (resourceAsStream == null) {
            throw new IOException("File not found:" + fileName);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read = 0;
        while (read > -1) {
            read = resourceAsStream.read();
            if (read != -1) {
                byteArrayOutputStream.write(read);
            }
        }
        byte[] content = byteArrayOutputStream.toByteArray();
        return content;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        ListableBeanFactory listableBeanFactory = (ListableBeanFactory) beanFactory;
        Map beanNamesForType = listableBeanFactory.getBeansOfType(AttributeResolver.class);
        List<AttributeResolver> resolvers = new ArrayList<AttributeResolver>();
        resolvers.addAll(beanNamesForType.values());
        firstStrikeAttributeResolver = new FirstStrikeAttributeResolver(resolvers);
    }
}
