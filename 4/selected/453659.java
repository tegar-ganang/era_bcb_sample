package org.swemas.rendering.trasmitting.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathExpressionException;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.kernel.IKernel;
import org.swemas.core.messaging.ILocaleProvidingChannel;
import org.swemas.data.xml.XmlException;
import org.swemas.rendering.ErrorCode;
import org.swemas.rendering.RenderingException;
import org.swemas.rendering.composing.SwXmlComposingModule;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Alexey Chernov
 * 
 */
public class SwFileTransmittingChannel extends SwXmlComposingModule implements IFileTransmittingChannel {

    /**
	 * @param kernel
	 * @throws InvocationTargetException
	 */
    public SwFileTransmittingChannel(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        ns().setNamespace("tp", "swemas/topology");
    }

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response) throws RenderingException {
        Node pnode = render(null, request).get(0);
        if (pnode != null) {
            String path = pnode.getNodeValue();
            File file = new File(path);
            try {
                byte[] buf = new byte[(int) file.length()];
                FileInputStream fsm = new FileInputStream(file);
                fsm.read(buf);
                response.getOutputStream().write(buf);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }
    }

    @Override
    public List<Node> render(Node node, HttpServletRequest request) throws RenderingException {
        try {
            String sc = "";
            sc = "xr_template_div_injective.xml";
            Document tpg = ixml().open(kernel().getPath("/site/pages/" + sc), kernel().getPath("/schema/topology.xsd"));
            List<Node> out = new ArrayList<Node>();
            Node root = ixml().evaluate(tpg, "/tp:element", ns()).item(0);
            if (root != null) {
                Map<String, Map<String, List<Node>>> flow = parseElement(root, request);
                out.add(flow.get("").get("").get(0));
                return out;
            } else {
                try {
                    throw new RenderingException(null, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResourcesObtainingError.getCode());
                } catch (ModuleNotFoundException m) {
                    throw new RenderingException(null, name(new Locale("en", "US")), ErrorCode.ResourcesObtainingError.getCode());
                }
            }
        } catch (XmlException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        } catch (XPathExpressionException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        } catch (DOMException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        }
    }
}
