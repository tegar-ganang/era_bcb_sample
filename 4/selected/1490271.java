package org.swemas.data.xml;

import java.lang.reflect.InvocationTargetException;
import org.swemas.core.Module;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.kernel.IKernel;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Alexey Chernov
 * 
 */
public abstract class XmlUsingModule extends Module {

    /**
	 * @param kernel
	 * @throws InvocationTargetException
	 */
    public XmlUsingModule(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        _ns = new NamespaceContext();
        try {
            _ixml = (IXmlChannel) kernel().getChannel(IXmlChannel.class);
        } catch (ModuleNotFoundException e) {
            throw new InvocationTargetException(e);
        }
    }

    protected IXmlChannel ixml() {
        return _ixml;
    }

    ;

    protected NamespaceContext ns() {
        return _ns;
    }

    ;

    protected void appendChild(Node parent, Node child) throws DOMException {
        if (parent != null) {
            if (child != null) {
                if (parent.getNodeType() != Node.DOCUMENT_NODE) if (child.getNodeType() != Node.ATTRIBUTE_NODE) if (child.getOwnerDocument() != parent.getOwnerDocument()) parent.appendChild(parent.getOwnerDocument().importNode(child, true)); else parent.appendChild(child); else if (child.getOwnerDocument() != parent.getOwnerDocument()) parent.getAttributes().setNamedItemNS(parent.getOwnerDocument().importNode(child, true)); else parent.getAttributes().setNamedItemNS(child); else {
                    if (child.getOwnerDocument() != parent) {
                        Document doc = (Document) parent;
                        doc.appendChild(doc.importNode(child, true));
                    } else parent.appendChild(child);
                }
            }
        }
    }

    private NamespaceContext _ns;

    private IXmlChannel _ixml;
}
