package org.freebxml.omar.client.ui.web.server;

import java.io.InputStream;
import java.io.PrintWriter;
import javax.activation.DataHandler;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.registry.Connection;
import javax.xml.registry.JAXRException;
import javax.xml.registry.infomodel.ExtrinsicObject;
import javax.xml.registry.infomodel.RegistryObject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freebxml.omar.client.ui.web.jaxr.JAXRCallback;
import org.freebxml.omar.client.ui.web.jaxr.JAXRTemplate;
import org.freebxml.omar.client.ui.web.util.XmlInputStreamReader;
import org.freebxml.omar.client.ui.web.util.XmlStreamTextWriter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class GetRepositoryItemController extends AbstractController {

    private static final Log log = LogFactory.getLog(GetRepositoryItemController.class);

    private JAXRTemplate jaxrTemplate;

    public void setJaxrTemplate(JAXRTemplate jaxrTemplate) {
        this.jaxrTemplate = jaxrTemplate;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final String id = request.getParameter("id");
        if (id == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        try {
            jaxrTemplate.execute(new JAXRCallback<Object>() {

                public Object execute(Connection connection) throws JAXRException {
                    RegistryObject registryObject = connection.getRegistryService().getBusinessQueryManager().getRegistryObject(id);
                    if (registryObject instanceof ExtrinsicObject) {
                        ExtrinsicObject extrinsicObject = (ExtrinsicObject) registryObject;
                        DataHandler dataHandler = extrinsicObject.getRepositoryItem();
                        if (dataHandler != null) {
                            response.setContentType("text/html");
                            try {
                                PrintWriter out = response.getWriter();
                                InputStream is = dataHandler.getInputStream();
                                try {
                                    final XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
                                    xmlStreamWriter.writeStartDocument();
                                    xmlStreamWriter.writeStartElement("div");
                                    xmlStreamWriter.writeStartElement("textarea");
                                    xmlStreamWriter.writeAttribute("name", "repositoryItem");
                                    xmlStreamWriter.writeAttribute("class", "xml");
                                    xmlStreamWriter.writeAttribute("style", "display:none");
                                    IOUtils.copy(new XmlInputStreamReader(is), new XmlStreamTextWriter(xmlStreamWriter));
                                    xmlStreamWriter.writeEndElement();
                                    xmlStreamWriter.writeStartElement("script");
                                    xmlStreamWriter.writeAttribute("class", "javascript");
                                    xmlStreamWriter.writeCharacters("dp.SyntaxHighlighter.HighlightAll('repositoryItem');");
                                    xmlStreamWriter.writeEndElement();
                                    xmlStreamWriter.writeEndElement();
                                    xmlStreamWriter.writeEndDocument();
                                    xmlStreamWriter.flush();
                                } finally {
                                    is.close();
                                }
                            } catch (Throwable ex) {
                                log.error("Error while trying to format repository item " + id, ex);
                            }
                        } else {
                        }
                    } else {
                    }
                    return null;
                }
            });
        } catch (JAXRException ex) {
            throw new ServletException(ex);
        }
        return null;
    }
}
