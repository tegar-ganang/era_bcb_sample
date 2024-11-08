package netgest.bo.def.v2;

import java.util.Vector;
import netgest.bo.def.boDefOPL;
import netgest.bo.def.v2.boDefHandlerImpl;
import netgest.utils.ngtXMLHandler;
import org.w3c.dom.Node;

public final class boDefOPLImpl extends ngtXMLHandler implements boDefOPL {

    private static final String[] EMPTY_KEYS = new String[0];

    private String[] p_attributesReadKey;

    private String[] p_attributesWriteKey;

    private String[] p_attributesDeleteKey;

    private String[] p_attributesFullControlKey;

    private String[] p_methodsExecuteKey;

    private String[] p_eventsExecuteKey;

    private boDefHandlerImpl p_defhandler;

    private String[] p_classKeys = null;

    private boolean[] p_classKeysActive = null;

    public boDefOPLImpl(boDefHandlerImpl bodef, Node x) {
        super(x);
        p_defhandler = bodef;
        this.parseO();
    }

    private void parseO() {
        ngtXMLHandler xnodeAtrKey = null;
        ngtXMLHandler[] xAttributes = null;
        ngtXMLHandler node = super.getChildNode("classKeys");
        if (node != null) {
            ngtXMLHandler[] xnodes = node.getChildNodes();
            if (xnodes != null) {
                p_classKeys = new String[xnodes.length];
                p_classKeysActive = new boolean[xnodes.length];
                for (int i = 0; i < xnodes.length; i++) {
                    p_classKeysActive[i] = GenericParseUtils.parseBoolean(xnodes[i].getAttribute("active"));
                    p_classKeys[i] = xnodes[i].getAttribute("name");
                }
            }
        }
        node = super.getChildNode("attributeKeys");
        if (node != null) {
            String[] ktypes = { "read", "write", "delete", "fullControl" };
            for (int i = 0; i < ktypes.length; i++) {
                xnodeAtrKey = node.getChildNode(ktypes[i]);
                Vector atts = new Vector();
                if (xnodeAtrKey != null) {
                    xAttributes = xnodeAtrKey.getChildNodes();
                    for (int k = 0; k < xAttributes.length; k++) {
                        boolean active = GenericParseUtils.parseBoolean(xAttributes[k].getAttribute("active", "true"));
                        if (active) {
                            atts.add(xAttributes[k].getText());
                        }
                    }
                }
                switch(i) {
                    case 0:
                        p_attributesReadKey = (String[]) atts.toArray(new String[atts.size()]);
                        break;
                    case 1:
                        p_attributesWriteKey = (String[]) atts.toArray(new String[atts.size()]);
                        break;
                    case 2:
                        p_attributesDeleteKey = (String[]) atts.toArray(new String[atts.size()]);
                        break;
                    case 3:
                        p_attributesFullControlKey = (String[]) atts.toArray(new String[atts.size()]);
                        break;
                }
            }
        }
    }

    public String[] getReadKeyAttributes() {
        return p_attributesReadKey;
    }

    public String[] getWriteKeyAttributes() {
        return p_attributesWriteKey;
    }

    public String[] getDeleteKeyAttributes() {
        return p_attributesDeleteKey;
    }

    public String[] getFullControlKeyAttributes() {
        return p_attributesFullControlKey;
    }

    public String[] getMethodsExecuteKeys() {
        return this.p_methodsExecuteKey;
    }

    public String[] getEventsExecuteKeys() {
        return this.p_eventsExecuteKey;
    }

    public String[] getClassKeys() {
        if (p_classKeys == null) return new String[0];
        return p_classKeys;
    }
}
