package LayerD.CodeDOM;

import LayerD.CodeDOM.XplParser.ParseException;
import java.io.*;
import java.util.*;
import java.text.*;

public class XplExpression extends XplNode {

    String p_typeStr;

    String p_valueStr;

    String p_doc;

    String p_helpURL;

    String p_ldsrc;

    boolean p_iny;

    String p_inydata;

    String p_inyby;

    String p_lddata;

    XplNode p_texpression;

    public XplExpression() {
        p_typeStr = "";
        p_valueStr = "";
        p_doc = "";
        p_helpURL = "";
        p_ldsrc = "";
        p_iny = false;
        p_inydata = "";
        p_inyby = "";
        p_lddata = "";
        p_texpression = null;
    }

    public XplExpression(String n_typeStr, String n_valueStr, String n_doc, String n_helpURL, String n_ldsrc, boolean n_iny, String n_inydata, String n_inyby, String n_lddata) {
        set_typeStr(n_typeStr);
        set_valueStr(n_valueStr);
        set_doc(n_doc);
        set_helpURL(n_helpURL);
        set_ldsrc(n_ldsrc);
        set_iny(n_iny);
        set_inydata(n_inydata);
        set_inyby(n_inyby);
        set_lddata(n_lddata);
        p_texpression = null;
    }

    public XplExpression(XplNode n_texpression) {
        p_typeStr = "";
        p_valueStr = "";
        p_doc = "";
        p_helpURL = "";
        p_ldsrc = "";
        p_iny = false;
        p_inydata = "";
        p_inyby = "";
        p_lddata = "";
        p_texpression = null;
        set_texpression(n_texpression);
    }

    public XplExpression(String n_typeStr, String n_valueStr, String n_doc, String n_helpURL, String n_ldsrc, boolean n_iny, String n_inydata, String n_inyby, String n_lddata, XplNode n_texpression) {
        set_typeStr(n_typeStr);
        set_valueStr(n_valueStr);
        set_doc(n_doc);
        set_helpURL(n_helpURL);
        set_ldsrc(n_ldsrc);
        set_iny(n_iny);
        set_inydata(n_inydata);
        set_inyby(n_inyby);
        set_lddata(n_lddata);
        p_texpression = null;
        set_texpression(n_texpression);
    }

    public XplNode Clone() {
        XplExpression copy = new XplExpression();
        copy.set_typeStr(this.p_typeStr);
        copy.set_valueStr(this.p_valueStr);
        copy.set_doc(this.p_doc);
        copy.set_helpURL(this.p_helpURL);
        copy.set_ldsrc(this.p_ldsrc);
        copy.set_iny(this.p_iny);
        copy.set_inydata(this.p_inydata);
        copy.set_inyby(this.p_inyby);
        copy.set_lddata(this.p_lddata);
        if (p_texpression != null) copy.set_texpression(p_texpression.Clone());
        copy.set_Name(this.get_Name());
        return (XplNode) copy;
    }

    public int get_TypeName() {
        return CodeDOMTypes.XplExpression;
    }

    public boolean Write(XplWriter writer) throws IOException, CodeDOM_Exception {
        boolean result = true;
        writer.WriteStartElement(this.get_Name());
        if (p_typeStr != "") writer.WriteAttributeString("typeStr", CodeDOM_Utils.Att_ToString(p_typeStr));
        if (p_valueStr != "") writer.WriteAttributeString("valueStr", CodeDOM_Utils.Att_ToString(p_valueStr));
        if (p_doc != "") writer.WriteAttributeString("doc", CodeDOM_Utils.Att_ToString(p_doc));
        if (p_helpURL != "") writer.WriteAttributeString("helpURL", CodeDOM_Utils.Att_ToString(p_helpURL));
        if (p_ldsrc != "") writer.WriteAttributeString("ldsrc", CodeDOM_Utils.Att_ToString(p_ldsrc));
        if (p_iny != false) writer.WriteAttributeString("iny", CodeDOM_Utils.Att_ToString(p_iny));
        if (p_inydata != "") writer.WriteAttributeString("inydata", CodeDOM_Utils.Att_ToString(p_inydata));
        if (p_inyby != "") writer.WriteAttributeString("inyby", CodeDOM_Utils.Att_ToString(p_inyby));
        if (p_lddata != "") writer.WriteAttributeString("lddata", CodeDOM_Utils.Att_ToString(p_lddata));
        if (p_texpression != null) if (!p_texpression.Write(writer)) result = false;
        writer.WriteEndElement();
        return result;
    }

    public XplNode Read(XplReader reader) throws ParseException, CodeDOM_Exception, IOException {
        this.set_Name(reader.Name());
        if (reader.HasAttributes()) {
            for (int i = 1; i <= reader.AttributeCount(); i++) {
                reader.MoveToAttribute(i);
                if (reader.Name().equals("typeStr")) {
                    this.set_typeStr(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("valueStr")) {
                    this.set_valueStr(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("doc")) {
                    this.set_doc(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("helpURL")) {
                    this.set_helpURL(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("ldsrc")) {
                    this.set_ldsrc(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("iny")) {
                    this.set_iny(CodeDOM_Utils.StringAtt_To_BOOLEAN(reader.Value()));
                } else if (reader.Name().equals("inydata")) {
                    this.set_inydata(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("inyby")) {
                    this.set_inyby(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("lddata")) {
                    this.set_lddata(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else {
                    throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".Atributo '" + reader.Name() + "' invalido en elemento '" + this.get_Name() + "'.");
                }
            }
            reader.MoveToElement();
        }
        this.p_texpression = null;
        if (!reader.IsEmptyElement()) {
            reader.Read();
            while (reader.NodeType() != XmlNodeType.ENDELEMENT) {
                XplNode tempNode = null;
                switch(reader.NodeType()) {
                    case XmlNodeType.ELEMENT:
                        if (reader.Name().equals("a")) {
                            tempNode = new XplAssing();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("new")) {
                            tempNode = new XplNewExpression();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("bo")) {
                            tempNode = new XplBinaryoperator();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("uo")) {
                            tempNode = new XplUnaryoperator();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("to")) {
                            tempNode = new XplTernaryoperator();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("b")) {
                            tempNode = new XplFunctioncall();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("n")) {
                            tempNode = new XplNode(XplNodeType_enum.STRING);
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("lit")) {
                            tempNode = new XplLiteral();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("fc")) {
                            tempNode = new XplFunctioncall();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("cfc")) {
                            tempNode = new XplComplexfunctioncall();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("cast")) {
                            tempNode = new XplCastexpression();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("delete")) {
                            tempNode = new XplExpression();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("onpointer")) {
                            tempNode = new XplExpression();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("writecode")) {
                            tempNode = new XplWriteCodeBody();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("t")) {
                            tempNode = new XplType();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("toft")) {
                            tempNode = new XplType();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("is")) {
                            tempNode = new XplCastexpression();
                            tempNode.Read(reader);
                        } else if (reader.Name().equals("empty")) {
                            tempNode = new XplNode(XplNodeType_enum.EMPTY);
                            tempNode.Read(reader);
                        } else {
                            throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".Nombre de nodo '" + reader.Name() + "' inesperado como hijo de elemento '" + this.get_Name() + "'.");
                        }
                        break;
                    case XmlNodeType.ENDELEMENT:
                        break;
                    case XmlNodeType.TEXT:
                        throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".No se esperaba texto en este nodo.");
                    default:
                        break;
                }
                if (this.get_texpression() != null && tempNode != null) throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".Nodo '" + reader.Name() + "' incorrecto como hijo de elemento '" + this.get_Name() + "'."); else if (tempNode != null) this.set_texpression(tempNode);
                reader.Read();
            }
        }
        return this;
    }

    public String get_typeStr() {
        return p_typeStr;
    }

    public String get_valueStr() {
        return p_valueStr;
    }

    public String get_doc() {
        return p_doc;
    }

    public String get_helpURL() {
        return p_helpURL;
    }

    public String get_ldsrc() {
        return p_ldsrc;
    }

    public boolean get_iny() {
        return p_iny;
    }

    public String get_inydata() {
        return p_inydata;
    }

    public String get_inyby() {
        return p_inyby;
    }

    public String get_lddata() {
        return p_lddata;
    }

    public XplNode get_texpression() {
        return p_texpression;
    }

    public String set_typeStr(String new_typeStr) {
        String back_typeStr = p_typeStr;
        p_typeStr = new_typeStr;
        return back_typeStr;
    }

    public String set_valueStr(String new_valueStr) {
        String back_valueStr = p_valueStr;
        p_valueStr = new_valueStr;
        return back_valueStr;
    }

    public String set_doc(String new_doc) {
        String back_doc = p_doc;
        p_doc = new_doc;
        return back_doc;
    }

    public String set_helpURL(String new_helpURL) {
        String back_helpURL = p_helpURL;
        p_helpURL = new_helpURL;
        return back_helpURL;
    }

    public String set_ldsrc(String new_ldsrc) {
        String back_ldsrc = p_ldsrc;
        p_ldsrc = new_ldsrc;
        return back_ldsrc;
    }

    public boolean set_iny(boolean new_iny) {
        boolean back_iny = p_iny;
        p_iny = new_iny;
        return back_iny;
    }

    public String set_inydata(String new_inydata) {
        String back_inydata = p_inydata;
        p_inydata = new_inydata;
        return back_inydata;
    }

    public String set_inyby(String new_inyby) {
        String back_inyby = p_inyby;
        p_inyby = new_inyby;
        return back_inyby;
    }

    public String set_lddata(String new_lddata) {
        String back_lddata = p_lddata;
        p_lddata = new_lddata;
        return back_lddata;
    }

    public XplNode set_texpression(XplNode new_texpression) {
        if (new_texpression == null) return p_texpression;
        XplNode back_texpression = p_texpression;
        if (new_texpression.get_Name().equals("a")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplAssing) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("new")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplNewExpression) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("bo")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplBinaryoperator) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("uo")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplUnaryoperator) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("to")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplTernaryoperator) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("b")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplFunctioncall) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("n")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.String) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("lit")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplLiteral) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("fc")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplFunctioncall) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("cfc")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplComplexfunctioncall) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("cast")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplCastexpression) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("delete")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplExpression) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("onpointer")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplExpression) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("writecode")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplWriteCodeBody) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("t")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplType) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("toft")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplType) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("is")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplCastexpression) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        if (new_texpression.get_Name().equals("empty")) {
            if (new_texpression.get_ContentTypeName() != CodeDOMTypes.XplNode) {
                this.set_ErrorString("El elemento de tipo '" + new_texpression.get_ContentTypeName() + "' no es valido como componente de 'texpression'.");
                return null;
            }
            p_texpression = new_texpression;
            p_texpression.set_Parent(this);
            return back_texpression;
        }
        this.set_ErrorString("El elemento de nombre '" + new_texpression.get_Name() + "' no es valido como componente de 'texpression'.");
        return null;
    }

    public static final XplAssing new_a() {
        XplAssing node = new XplAssing();
        node.set_Name("a");
        return node;
    }

    public static final XplNewExpression new_new() {
        XplNewExpression node = new XplNewExpression();
        node.set_Name("new");
        return node;
    }

    public static final XplBinaryoperator new_bo() {
        XplBinaryoperator node = new XplBinaryoperator();
        node.set_Name("bo");
        return node;
    }

    public static final XplUnaryoperator new_uo() {
        XplUnaryoperator node = new XplUnaryoperator();
        node.set_Name("uo");
        return node;
    }

    public static final XplTernaryoperator new_to() {
        XplTernaryoperator node = new XplTernaryoperator();
        node.set_Name("to");
        return node;
    }

    public static final XplFunctioncall new_b() {
        XplFunctioncall node = new XplFunctioncall();
        node.set_Name("b");
        return node;
    }

    public static final XplNode new_n() {
        XplNode node = new XplNode(XplNodeType_enum.STRING);
        node.set_Name("n");
        return node;
    }

    public static final XplLiteral new_lit() {
        XplLiteral node = new XplLiteral();
        node.set_Name("lit");
        return node;
    }

    public static final XplFunctioncall new_fc() {
        XplFunctioncall node = new XplFunctioncall();
        node.set_Name("fc");
        return node;
    }

    public static final XplComplexfunctioncall new_cfc() {
        XplComplexfunctioncall node = new XplComplexfunctioncall();
        node.set_Name("cfc");
        return node;
    }

    public static final XplCastexpression new_cast() {
        XplCastexpression node = new XplCastexpression();
        node.set_Name("cast");
        return node;
    }

    public static final XplExpression new_delete() {
        XplExpression node = new XplExpression();
        node.set_Name("delete");
        return node;
    }

    public static final XplExpression new_onpointer() {
        XplExpression node = new XplExpression();
        node.set_Name("onpointer");
        return node;
    }

    public static final XplWriteCodeBody new_writecode() {
        XplWriteCodeBody node = new XplWriteCodeBody();
        node.set_Name("writecode");
        return node;
    }

    public static final XplType new_t() {
        XplType node = new XplType();
        node.set_Name("t");
        return node;
    }

    public static final XplType new_toft() {
        XplType node = new XplType();
        node.set_Name("toft");
        return node;
    }

    public static final XplCastexpression new_is() {
        XplCastexpression node = new XplCastexpression();
        node.set_Name("is");
        return node;
    }

    public static final XplNode new_empty() {
        XplNode node = new XplNode(XplNodeType_enum.EMPTY);
        node.set_Name("empty");
        return node;
    }
}
