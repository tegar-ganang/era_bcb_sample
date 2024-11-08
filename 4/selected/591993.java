package org.jcompany.view.jsf.renderer;

import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.trinidad.bean.FacesBean;
import org.apache.myfaces.trinidad.component.core.input.CoreInputFile;
import org.apache.myfaces.trinidad.context.RenderingContext;
import org.apache.myfaces.trinidad.render.CoreRenderer;
import org.apache.myfaces.trinidadinternal.renderkit.core.xhtml.InputFileRenderer;
import org.jcompany.commons.PlcFileEntity;
import org.jcompany.view.jsf.component.PlcFile;

/**
 * Especializa��o do renderer base InputFileRenderer para permitir IoC e DI nos renderes JSF/Trinidad. 
 * 
 */
public class PlcFileRenderer extends InputFileRenderer {

    public static final String RENDERER_TYPE = "com.powerlogic.jsf.Arquivo";

    /**
	 * IoC do jcompany. Se o inputFile cont�m valor, ent�o cria componentes HTML para mostrar a descri��o do arquivo e o bot�o para fazer o download
	 */
    @Override
    protected void delegateRenderer(FacesContext context, RenderingContext arc, UIComponent component, FacesBean bean, CoreRenderer arg4) throws IOException {
        PlcFileEntity value = (PlcFileEntity) bean.getProperty(PlcFile.VALUE_ARQUIVO_KEY);
        if (value != null && value.getId() != null) {
            ResponseWriter rw = context.getResponseWriter();
            rw.startElement("span", component);
            String estilos = (String) bean.getProperty(CoreInputFile.STYLE_CLASS_KEY);
            if (!StringUtils.isBlank(estilos)) rw.writeAttribute("class", estilos, null); else rw.writeAttribute("class", "af_inputText", null);
            estilos = (String) bean.getProperty(CoreInputFile.INLINE_STYLE_KEY);
            if (!StringUtils.isBlank(estilos)) rw.writeAttribute("style", estilos, null); else rw.writeAttribute("style", "float: left;", null);
            rw.startElement("input", component);
            renderId(context, component);
            renderAllAttributes(context, arc, bean, false);
            rw.writeAttribute("type", "text", "type");
            rw.writeAttribute("value", value.getName(), null);
            rw.writeAttribute("readonly", true, null);
            rw.writeAttribute("name", value, "name");
            rw.endElement("input");
            rw.endElement("span");
        } else {
            ResponseWriter rw = context.getResponseWriter();
            rw.startElement("span", component);
            String estilos = (String) bean.getProperty(CoreInputFile.STYLE_CLASS_KEY);
            if (!StringUtils.isBlank(estilos)) rw.writeAttribute("class", estilos, null); else rw.writeAttribute("class", "af_inputText", null);
            estilos = (String) bean.getProperty(CoreInputFile.INLINE_STYLE_KEY);
            if (!StringUtils.isBlank(estilos)) rw.writeAttribute("style", estilos, null); else rw.writeAttribute("style", "float: left;", null);
            super.delegateRenderer(context, arc, component, bean, arg4);
            rw.endElement("span");
        }
    }
}
