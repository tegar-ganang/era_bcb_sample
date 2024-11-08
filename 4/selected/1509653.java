package LayerD.CodeDOM;

import LayerD.CodeDOM.XplParser.ParseException;
import java.io.*;
import java.util.*;
import java.text.*;

public class XplDeclarator extends XplNode {

    String p_name;

    String p_internalname;

    String p_externalname;

    boolean p_donotrender;

    int p_storage;

    String p_doc;

    String p_helpURL;

    String p_ldsrc;

    boolean p_iny;

    String p_inydata;

    String p_inyby;

    String p_lddata;

    String p_address;

    boolean p_atomicwrite;

    boolean p_atomicread;

    XplType p_type;

    XplNode p_aliasref;

    XplInitializerList p_i;

    public XplDeclarator() {
        p_name = "";
        p_internalname = "";
        p_externalname = "";
        p_donotrender = false;
        p_storage = XplVarstorage_enum.AUTO;
        p_doc = "";
        p_helpURL = "";
        p_ldsrc = "";
        p_iny = false;
        p_inydata = "";
        p_inyby = "";
        p_lddata = "";
        p_address = "";
        p_atomicwrite = false;
        p_atomicread = false;
        p_type = null;
        p_aliasref = null;
        p_i = null;
    }

    public XplDeclarator(String n_name, String n_internalname, String n_externalname, boolean n_donotrender, int n_storage, boolean n_atomicwrite, boolean n_atomicread) {
        p_name = "";
        p_internalname = "";
        p_externalname = "";
        p_donotrender = false;
        p_storage = XplVarstorage_enum.AUTO;
        p_doc = "";
        p_helpURL = "";
        p_ldsrc = "";
        p_iny = false;
        p_inydata = "";
        p_inyby = "";
        p_lddata = "";
        p_address = "";
        p_atomicwrite = false;
        p_atomicread = false;
        set_name(n_name);
        set_internalname(n_internalname);
        set_externalname(n_externalname);
        set_donotrender(n_donotrender);
        set_storage(n_storage);
        set_atomicwrite(n_atomicwrite);
        set_atomicread(n_atomicread);
        p_type = null;
        p_aliasref = null;
        p_i = null;
    }

    public XplDeclarator(String n_name, String n_internalname, String n_externalname, boolean n_donotrender, int n_storage, String n_doc, String n_helpURL, String n_ldsrc, boolean n_iny, String n_inydata, String n_inyby, String n_lddata, String n_address, boolean n_atomicwrite, boolean n_atomicread) {
        set_name(n_name);
        set_internalname(n_internalname);
        set_externalname(n_externalname);
        set_donotrender(n_donotrender);
        set_storage(n_storage);
        set_doc(n_doc);
        set_helpURL(n_helpURL);
        set_ldsrc(n_ldsrc);
        set_iny(n_iny);
        set_inydata(n_inydata);
        set_inyby(n_inyby);
        set_lddata(n_lddata);
        set_address(n_address);
        set_atomicwrite(n_atomicwrite);
        set_atomicread(n_atomicread);
        p_type = null;
        p_aliasref = null;
        p_i = null;
    }

    public XplDeclarator(XplType n_type, XplNode n_aliasref, XplInitializerList n_i) {
        p_name = "";
        p_internalname = "";
        p_externalname = "";
        p_donotrender = false;
        p_storage = XplVarstorage_enum.AUTO;
        p_doc = "";
        p_helpURL = "";
        p_ldsrc = "";
        p_iny = false;
        p_inydata = "";
        p_inyby = "";
        p_lddata = "";
        p_address = "";
        p_atomicwrite = false;
        p_atomicread = false;
        p_type = null;
        p_aliasref = null;
        p_i = null;
        set_type(n_type);
        set_aliasref(n_aliasref);
        set_i(n_i);
    }

    public XplDeclarator(String n_name, String n_internalname, String n_externalname, boolean n_donotrender, int n_storage, boolean n_atomicwrite, boolean n_atomicread, XplType n_type, XplNode n_aliasref, XplInitializerList n_i) {
        p_name = "";
        p_internalname = "";
        p_externalname = "";
        p_donotrender = false;
        p_storage = XplVarstorage_enum.AUTO;
        p_doc = "";
        p_helpURL = "";
        p_ldsrc = "";
        p_iny = false;
        p_inydata = "";
        p_inyby = "";
        p_lddata = "";
        p_address = "";
        p_atomicwrite = false;
        p_atomicread = false;
        set_name(n_name);
        set_internalname(n_internalname);
        set_externalname(n_externalname);
        set_donotrender(n_donotrender);
        set_storage(n_storage);
        set_atomicwrite(n_atomicwrite);
        set_atomicread(n_atomicread);
        p_type = null;
        p_aliasref = null;
        p_i = null;
        set_type(n_type);
        set_aliasref(n_aliasref);
        set_i(n_i);
    }

    public XplDeclarator(String n_name, String n_internalname, String n_externalname, boolean n_donotrender, int n_storage, String n_doc, String n_helpURL, String n_ldsrc, boolean n_iny, String n_inydata, String n_inyby, String n_lddata, String n_address, boolean n_atomicwrite, boolean n_atomicread, XplType n_type, XplNode n_aliasref, XplInitializerList n_i) {
        set_name(n_name);
        set_internalname(n_internalname);
        set_externalname(n_externalname);
        set_donotrender(n_donotrender);
        set_storage(n_storage);
        set_doc(n_doc);
        set_helpURL(n_helpURL);
        set_ldsrc(n_ldsrc);
        set_iny(n_iny);
        set_inydata(n_inydata);
        set_inyby(n_inyby);
        set_lddata(n_lddata);
        set_address(n_address);
        set_atomicwrite(n_atomicwrite);
        set_atomicread(n_atomicread);
        p_type = null;
        p_aliasref = null;
        p_i = null;
        set_type(n_type);
        set_aliasref(n_aliasref);
        set_i(n_i);
    }

    public XplNode Clone() {
        XplDeclarator copy = new XplDeclarator();
        copy.set_name(this.p_name);
        copy.set_internalname(this.p_internalname);
        copy.set_externalname(this.p_externalname);
        copy.set_donotrender(this.p_donotrender);
        copy.set_storage(this.p_storage);
        copy.set_doc(this.p_doc);
        copy.set_helpURL(this.p_helpURL);
        copy.set_ldsrc(this.p_ldsrc);
        copy.set_iny(this.p_iny);
        copy.set_inydata(this.p_inydata);
        copy.set_inyby(this.p_inyby);
        copy.set_lddata(this.p_lddata);
        copy.set_address(this.p_address);
        copy.set_atomicwrite(this.p_atomicwrite);
        copy.set_atomicread(this.p_atomicread);
        if (p_type != null) copy.set_type((XplType) p_type.Clone());
        if (p_aliasref != null) copy.set_aliasref(p_aliasref.Clone());
        if (p_i != null) copy.set_i((XplInitializerList) p_i.Clone());
        copy.set_Name(this.get_Name());
        return (XplNode) copy;
    }

    public int get_TypeName() {
        return CodeDOMTypes.XplDeclarator;
    }

    public boolean Write(XplWriter writer) throws IOException, CodeDOM_Exception {
        boolean result = true;
        writer.WriteStartElement(this.get_Name());
        if (p_name != "") writer.WriteAttributeString("name", CodeDOM_Utils.Att_ToString(p_name));
        if (p_internalname != "") writer.WriteAttributeString("internalname", CodeDOM_Utils.Att_ToString(p_internalname));
        if (p_externalname != "") writer.WriteAttributeString("externalname", CodeDOM_Utils.Att_ToString(p_externalname));
        if (p_donotrender != false) writer.WriteAttributeString("donotrender", CodeDOM_Utils.Att_ToString(p_donotrender));
        if (p_storage != XplVarstorage_enum.AUTO) writer.WriteAttributeString("storage", CodeDOM_STV.XPLVARSTORAGE_ENUM[(int) p_storage]);
        if (p_doc != "") writer.WriteAttributeString("doc", CodeDOM_Utils.Att_ToString(p_doc));
        if (p_helpURL != "") writer.WriteAttributeString("helpURL", CodeDOM_Utils.Att_ToString(p_helpURL));
        if (p_ldsrc != "") writer.WriteAttributeString("ldsrc", CodeDOM_Utils.Att_ToString(p_ldsrc));
        if (p_iny != false) writer.WriteAttributeString("iny", CodeDOM_Utils.Att_ToString(p_iny));
        if (p_inydata != "") writer.WriteAttributeString("inydata", CodeDOM_Utils.Att_ToString(p_inydata));
        if (p_inyby != "") writer.WriteAttributeString("inyby", CodeDOM_Utils.Att_ToString(p_inyby));
        if (p_lddata != "") writer.WriteAttributeString("lddata", CodeDOM_Utils.Att_ToString(p_lddata));
        if (p_address != "") writer.WriteAttributeString("address", CodeDOM_Utils.Att_ToString(p_address));
        if (p_atomicwrite != false) writer.WriteAttributeString("atomicwrite", CodeDOM_Utils.Att_ToString(p_atomicwrite));
        if (p_atomicread != false) writer.WriteAttributeString("atomicread", CodeDOM_Utils.Att_ToString(p_atomicread));
        if (p_type != null) if (!p_type.Write(writer)) result = false;
        if (p_aliasref != null) if (!p_aliasref.Write(writer)) result = false;
        if (p_i != null) if (!p_i.Write(writer)) result = false;
        writer.WriteEndElement();
        return result;
    }

    public XplNode Read(XplReader reader) throws ParseException, CodeDOM_Exception, IOException {
        this.set_Name(reader.Name());
        if (reader.HasAttributes()) {
            String tmpStr = "";
            boolean flag = false;
            int count = 0;
            for (int i = 1; i <= reader.AttributeCount(); i++) {
                reader.MoveToAttribute(i);
                if (reader.Name().equals("name")) {
                    this.set_name(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("internalname")) {
                    this.set_internalname(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("externalname")) {
                    this.set_externalname(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("donotrender")) {
                    this.set_donotrender(CodeDOM_Utils.StringAtt_To_BOOLEAN(reader.Value()));
                } else if (reader.Name().equals("storage")) {
                    tmpStr = CodeDOM_Utils.StringAtt_To_STRING(reader.Value());
                    count = 0;
                    flag = false;
                    for (int n = 0; n < CodeDOM_STV.XPLVARSTORAGE_ENUM.length; n++) {
                        String str = CodeDOM_STV.XPLVARSTORAGE_ENUM[n];
                        if (str.equals(tmpStr)) {
                            this.set_storage(count);
                            flag = true;
                            break;
                        }
                        count++;
                    }
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
                } else if (reader.Name().equals("address")) {
                    this.set_address(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("atomicwrite")) {
                    this.set_atomicwrite(CodeDOM_Utils.StringAtt_To_BOOLEAN(reader.Value()));
                } else if (reader.Name().equals("atomicread")) {
                    this.set_atomicread(CodeDOM_Utils.StringAtt_To_BOOLEAN(reader.Value()));
                } else {
                    throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".Atributo '" + reader.Name() + "' invalido en elemento '" + this.get_Name() + "'.");
                }
            }
            reader.MoveToElement();
        }
        this.p_type = null;
        this.p_aliasref = null;
        this.p_i = null;
        if (!reader.IsEmptyElement()) {
            reader.Read();
            while (reader.NodeType() != XmlNodeType.ENDELEMENT) {
                XplNode tempNode = null;
                switch(reader.NodeType()) {
                    case XmlNodeType.ELEMENT:
                        if (reader.Name().equals("type")) {
                            tempNode = new XplType();
                            tempNode.Read(reader);
                            if (this.get_type() != null) throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".Nodo '" + reader.Name() + "' repetido como hijo de elemento '" + this.get_Name() + "'.");
                            this.set_type((XplType) tempNode);
                        } else if (reader.Name().equals("aliasref")) {
                            tempNode = new XplNode(XplNodeType_enum.STRING);
                            tempNode.Read(reader);
                            if (this.get_aliasref() != null) throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".Nodo '" + reader.Name() + "' repetido como hijo de elemento '" + this.get_Name() + "'.");
                            this.set_aliasref(tempNode);
                        } else if (reader.Name().equals("i")) {
                            tempNode = new XplInitializerList();
                            tempNode.Read(reader);
                            if (this.get_i() != null) throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".Nodo '" + reader.Name() + "' repetido como hijo de elemento '" + this.get_Name() + "'.");
                            this.set_i((XplInitializerList) tempNode);
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
                reader.Read();
            }
        }
        return this;
    }

    public String get_name() {
        return p_name;
    }

    public String get_internalname() {
        return p_internalname;
    }

    public String get_externalname() {
        return p_externalname;
    }

    public boolean get_donotrender() {
        return p_donotrender;
    }

    public int get_storage() {
        return p_storage;
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

    public String get_address() {
        return p_address;
    }

    public boolean get_atomicwrite() {
        return p_atomicwrite;
    }

    public boolean get_atomicread() {
        return p_atomicread;
    }

    public XplType get_type() {
        return p_type;
    }

    public XplNode get_aliasref() {
        return p_aliasref;
    }

    public XplInitializerList get_i() {
        return p_i;
    }

    public String set_name(String new_name) {
        String back_name = p_name;
        p_name = new_name;
        return back_name;
    }

    public String set_internalname(String new_internalname) {
        String back_internalname = p_internalname;
        p_internalname = new_internalname;
        return back_internalname;
    }

    public String set_externalname(String new_externalname) {
        String back_externalname = p_externalname;
        p_externalname = new_externalname;
        return back_externalname;
    }

    public boolean set_donotrender(boolean new_donotrender) {
        boolean back_donotrender = p_donotrender;
        p_donotrender = new_donotrender;
        return back_donotrender;
    }

    public int set_storage(int new_storage) {
        int back_storage = p_storage;
        p_storage = new_storage;
        return back_storage;
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

    public String set_address(String new_address) {
        String back_address = p_address;
        p_address = new_address;
        return back_address;
    }

    public boolean set_atomicwrite(boolean new_atomicwrite) {
        boolean back_atomicwrite = p_atomicwrite;
        p_atomicwrite = new_atomicwrite;
        return back_atomicwrite;
    }

    public boolean set_atomicread(boolean new_atomicread) {
        boolean back_atomicread = p_atomicread;
        p_atomicread = new_atomicread;
        return back_atomicread;
    }

    public XplType set_type(XplType new_type) {
        XplType back_type = p_type;
        p_type = new_type;
        if (p_type != null) {
            p_type.set_Name("type");
            p_type.set_Parent(this);
        }
        return back_type;
    }

    public XplNode set_aliasref(XplNode new_aliasref) {
        if (new_aliasref.get_ContentTypeName() != CodeDOMTypes.String) {
            this.set_ErrorString("El nodo que intenta asignar a la propiedad es de un tipo incorrecto.");
            return null;
        }
        XplNode back_aliasref = p_aliasref;
        p_aliasref = new_aliasref;
        if (p_aliasref != null) {
            p_aliasref.set_Parent(this);
            p_aliasref.set_Name("aliasref");
        }
        return back_aliasref;
    }

    public XplInitializerList set_i(XplInitializerList new_i) {
        XplInitializerList back_i = p_i;
        p_i = new_i;
        if (p_i != null) {
            p_i.set_Name("i");
            p_i.set_Parent(this);
        }
        return back_i;
    }

    public static final XplType new_type() {
        XplType node = new XplType();
        node.set_Name("type");
        return node;
    }

    public static final XplNode new_aliasref() {
        XplNode node = new XplNode(XplNodeType_enum.STRING);
        node.set_Name("aliasref");
        return node;
    }

    public static final XplInitializerList new_i() {
        XplInitializerList node = new XplInitializerList();
        node.set_Name("i");
        return node;
    }
}
