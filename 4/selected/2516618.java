package org.jtestcase.core.mapping;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jtestcase.core.converter.ComplexTypeConverter;
import org.jtestcase.core.converter.TypeConversionException;
import org.jtestcase.core.digester.DigesterException;
import org.jtestcase.core.type.ParamType;
import org.jtestcase.core.type.TemplateType;
import org.jtestcase.core.util.FileFinder;
import org.jtestcase.core.util.JDOMDocumentBuilder;

public class ContainerEvaluator {

    private static Logger log = Logger.getLogger(ContainerEvaluator.class);

    public ContainerEvaluator() {
        super();
    }

    /**
	 * puts a parameter value in the container hashmap with key :
	 * param.getName()
	 * 
	 * @param key
	 * @param value
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws TypeConversionException
	 */
    public static HashMap putParameterInContainer(HashMap container, ParamHashMapBuilder builder, ComplexTypeConverter typeConverter, ParamType paramInstance) throws TypeConversionException, InstantiationException, IllegalAccessException {
        log.debug("examining template refereces ...");
        if (isParamTypeTemplate(paramInstance)) {
            log.debug("found template, putting into context");
            putTemplate(builder, typeConverter, paramInstance, container);
        }
        Object value = typeConverter._convertType(paramInstance);
        container.put(paramInstance.getName(), value);
        return container;
    }

    /**
	 * puts a parameter value in the container hashmap with key :
	 * param.getName()
	 * 
	 * @param key
	 * @param value
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws TypeConversionException
	 */
    public static HashMap putGroupParameterInContainer(HashMap container, ParamHashMapBuilder builder, ComplexTypeConverter typeConverter, ParamType paramInstance, String path) throws TypeConversionException, InstantiationException, IllegalAccessException {
        Object value = typeConverter._convertType(paramInstance);
        container.put(path + "/" + paramInstance.getName(), value);
        return container;
    }

    private static HashMap putTemplate(ParamHashMapBuilder builder, ComplexTypeConverter typeConverter, ParamType paramInstance, HashMap container) throws TypeConversionException, InstantiationException, IllegalAccessException {
        log.debug("template ref : " + paramInstance.getTemplate_ref());
        VelocityContext vcontext = null;
        if (container.containsKey("$%$VELOCITY$%$CONTEXT::" + paramInstance.getTemplate_ref())) {
            vcontext = (VelocityContext) container.get("$%$VELOCITY$%$CONTEXT::" + paramInstance.getTemplate_ref());
            log.debug("template already existing, recall from context");
        } else {
            vcontext = new VelocityContext();
            container.put("$%$VELOCITY$%$CONTEXT::" + paramInstance.getTemplate_ref(), vcontext);
            log.debug("creating new template in context : " + paramInstance.getTemplate_ref());
            log.debug("updating template list ...");
            if (container.get("$%$VELOCITY$%$TEMPLATES$%$LIST") != null) {
                container.put("$%$VELOCITY$%$TEMPLATES$%$LIST", paramInstance.getTemplate_ref() + "," + container.get("$%$VELOCITY$%$TEMPLATES$%$LIST"));
                log.debug("add template to template list, total is " + container.get("$%$VELOCITY$%$TEMPLATES$%$LIST"));
            } else {
                container.put("$%$VELOCITY$%$TEMPLATES$%$LIST", paramInstance.getTemplate_ref());
                log.debug("creating new template list : " + paramInstance.getTemplate_ref());
            }
        }
        log.debug("evaluating parameter in local context ...");
        Object value = typeConverter._convertType(paramInstance);
        vcontext.put(paramInstance.getName(), value);
        return container;
    }

    private static boolean isParamTypeTemplate(ParamType paramInstance) {
        if (paramInstance.getTemplate_ref() != null && paramInstance.getTemplate_ref().trim().length() > 0) {
            log.debug("template reference : " + paramInstance.getTemplate_ref());
            return true;
        }
        log.debug("no template reference found");
        return false;
    }

    public static HashMap evaluteTemplates(HashMap container, ParamHashMapBuilder builder, ComplexTypeConverter typeConverter) throws TypeConversionException, InstantiationException, IllegalAccessException, DigesterException {
        String templateList = (String) container.get("$%$VELOCITY$%$TEMPLATES$%$LIST");
        log.debug("templates list : " + templateList);
        if (templateList == null) {
            log.debug("no templates found in context");
            return container;
        }
        StringTokenizer templateListTokens = new StringTokenizer(templateList, ",");
        while (templateListTokens.hasMoreTokens()) {
            container = evaluateTemplate(templateListTokens.nextToken(), container, typeConverter, builder);
        }
        return container;
    }

    private static HashMap evaluateTemplate(String template_name, HashMap container, ComplexTypeConverter typeConverter, ParamHashMapBuilder builder) throws TypeConversionException, InstantiationException, IllegalAccessException, DigesterException {
        log.debug("evaluating template : " + template_name);
        TemplateType templateParamInst = (TemplateType) builder.getDigester().getTemplatesHashMap().get(template_name);
        VelocityStandardEngine vengine = new VelocityStandardEngine();
        StringWriter sw = new StringWriter();
        VelocityContext vcontext = (VelocityContext) container.get("$%$VELOCITY$%$CONTEXT::" + template_name);
        if (templateParamInst.getFromFile() != null && templateParamInst.getFromFile().trim().length() > 0) {
            FileFinder ff = new FileFinder();
            InputStream is = null;
            try {
                is = ff.getInputStream((templateParamInst.getFromFile()));
            } catch (FileNotFoundException e) {
                throw new TypeConversionException(e);
            }
            InputStreamReader isreader = new InputStreamReader(is);
            StringWriter writer = new StringWriter();
            int c;
            try {
                while ((c = isreader.read()) != -1) writer.write(c);
            } catch (IOException e) {
                throw new TypeConversionException(e);
            }
            templateParamInst.setContent(writer.getBuffer().toString());
        }
        String template_content = templateParamInst.getContent();
        try {
            vengine.applyTemplate(vcontext, sw, template_content);
        } catch (ParseErrorException e) {
            e.printStackTrace();
        } catch (MethodInvocationException e) {
            e.printStackTrace();
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fava = sw.toString();
        templateParamInst.setContent(sw.toString());
        if (templateParamInst.getJice() != null) {
            log.debug("template has a jice element, refitting to template values");
            Document doc = null;
            try {
                doc = JDOMDocumentBuilder.build(sw.toString());
            } catch (JDOMException e) {
                throw new TypeConversionException(e);
            } catch (IOException e) {
                throw new TypeConversionException(e);
            }
            templateParamInst.setJice(doc.getRootElement());
        }
        Object value = typeConverter._convertType(templateParamInst);
        container.put(template_name, value);
        return container;
    }
}
