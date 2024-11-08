package LayerD.CodeDOM;

import LayerD.CodeDOM.XplParser.ParseException;
import java.io.*;
import java.util.*;
import java.text.*;

public class XplTargetPlatform extends XplNode {

    String p_uniqueName;

    String p_simpleName;

    String p_minVersion;

    String p_maxVersion;

    String p_description;

    int p_supportLevel;

    String p_operatingsystem;

    String p_aplication;

    String p_multitask;

    int p_memorymodel;

    int p_defaultbitorder;

    int p_defaultbyteorder;

    int p_addresswidth;

    int p_databus;

    int p_commonregisterssize;

    int p_segments;

    int p_segmentsize;

    String p_threading;

    public XplTargetPlatform() {
        p_uniqueName = "";
        p_simpleName = "";
        p_minVersion = "";
        p_maxVersion = "";
        p_description = "";
        p_supportLevel = XplPlatformsupportlevel_enum.COMPLETE;
        p_operatingsystem = "";
        p_aplication = "";
        p_multitask = "Preemtive";
        p_memorymodel = XplMemorymodel_enum.LINEAL;
        p_defaultbitorder = XplBitorder_enum.IGNORE;
        p_defaultbyteorder = XplBitorder_enum.IGNORE;
        p_addresswidth = 32;
        p_databus = 32;
        p_commonregisterssize = 32;
        p_segments = 0;
        p_segmentsize = 0;
        p_threading = "";
    }

    public XplTargetPlatform(String n_uniqueName, String n_simpleName, int n_supportLevel, String n_multitask, int n_memorymodel, int n_defaultbitorder, int n_defaultbyteorder, int n_addresswidth, int n_databus, int n_commonregisterssize) {
        p_uniqueName = "";
        p_simpleName = "";
        p_minVersion = "";
        p_maxVersion = "";
        p_description = "";
        p_supportLevel = XplPlatformsupportlevel_enum.COMPLETE;
        p_operatingsystem = "";
        p_aplication = "";
        p_multitask = "Preemtive";
        p_memorymodel = XplMemorymodel_enum.LINEAL;
        p_defaultbitorder = XplBitorder_enum.IGNORE;
        p_defaultbyteorder = XplBitorder_enum.IGNORE;
        p_addresswidth = 32;
        p_databus = 32;
        p_commonregisterssize = 32;
        p_segments = 0;
        p_segmentsize = 0;
        p_threading = "";
        set_uniqueName(n_uniqueName);
        set_simpleName(n_simpleName);
        set_supportLevel(n_supportLevel);
        set_multitask(n_multitask);
        set_memorymodel(n_memorymodel);
        set_defaultbitorder(n_defaultbitorder);
        set_defaultbyteorder(n_defaultbyteorder);
        set_addresswidth(n_addresswidth);
        set_databus(n_databus);
        set_commonregisterssize(n_commonregisterssize);
    }

    public XplTargetPlatform(String n_uniqueName, String n_simpleName, String n_minVersion, String n_maxVersion, String n_description, int n_supportLevel, String n_operatingsystem, String n_aplication, String n_multitask, int n_memorymodel, int n_defaultbitorder, int n_defaultbyteorder, int n_addresswidth, int n_databus, int n_commonregisterssize, int n_segments, int n_segmentsize, String n_threading) {
        set_uniqueName(n_uniqueName);
        set_simpleName(n_simpleName);
        set_minVersion(n_minVersion);
        set_maxVersion(n_maxVersion);
        set_description(n_description);
        set_supportLevel(n_supportLevel);
        set_operatingsystem(n_operatingsystem);
        set_aplication(n_aplication);
        set_multitask(n_multitask);
        set_memorymodel(n_memorymodel);
        set_defaultbitorder(n_defaultbitorder);
        set_defaultbyteorder(n_defaultbyteorder);
        set_addresswidth(n_addresswidth);
        set_databus(n_databus);
        set_commonregisterssize(n_commonregisterssize);
        set_segments(n_segments);
        set_segmentsize(n_segmentsize);
        set_threading(n_threading);
    }

    public XplNode Clone() {
        XplTargetPlatform copy = new XplTargetPlatform();
        copy.set_uniqueName(this.p_uniqueName);
        copy.set_simpleName(this.p_simpleName);
        copy.set_minVersion(this.p_minVersion);
        copy.set_maxVersion(this.p_maxVersion);
        copy.set_description(this.p_description);
        copy.set_supportLevel(this.p_supportLevel);
        copy.set_operatingsystem(this.p_operatingsystem);
        copy.set_aplication(this.p_aplication);
        copy.set_multitask(this.p_multitask);
        copy.set_memorymodel(this.p_memorymodel);
        copy.set_defaultbitorder(this.p_defaultbitorder);
        copy.set_defaultbyteorder(this.p_defaultbyteorder);
        copy.set_addresswidth(this.p_addresswidth);
        copy.set_databus(this.p_databus);
        copy.set_commonregisterssize(this.p_commonregisterssize);
        copy.set_segments(this.p_segments);
        copy.set_segmentsize(this.p_segmentsize);
        copy.set_threading(this.p_threading);
        copy.set_Name(this.get_Name());
        return (XplNode) copy;
    }

    public int get_TypeName() {
        return CodeDOMTypes.XplTargetPlatform;
    }

    public boolean Write(XplWriter writer) throws IOException, CodeDOM_Exception {
        boolean result = true;
        writer.WriteStartElement(this.get_Name());
        if (p_uniqueName != "") writer.WriteAttributeString("uniqueName", CodeDOM_Utils.Att_ToString(p_uniqueName));
        if (p_simpleName != "") writer.WriteAttributeString("simpleName", CodeDOM_Utils.Att_ToString(p_simpleName));
        if (p_minVersion != "") writer.WriteAttributeString("minVersion", CodeDOM_Utils.Att_ToString(p_minVersion));
        if (p_maxVersion != "") writer.WriteAttributeString("maxVersion", CodeDOM_Utils.Att_ToString(p_maxVersion));
        if (p_description != "") writer.WriteAttributeString("description", CodeDOM_Utils.Att_ToString(p_description));
        if (p_supportLevel != XplPlatformsupportlevel_enum.COMPLETE) writer.WriteAttributeString("supportLevel", CodeDOM_STV.XPLPLATFORMSUPPORTLEVEL_ENUM[(int) p_supportLevel]);
        if (p_operatingsystem != "") writer.WriteAttributeString("operatingsystem", CodeDOM_Utils.Att_ToString(p_operatingsystem));
        if (p_aplication != "") writer.WriteAttributeString("aplication", CodeDOM_Utils.Att_ToString(p_aplication));
        if (p_multitask != "Preemtive") writer.WriteAttributeString("multitask", CodeDOM_Utils.Att_ToString(p_multitask));
        if (p_memorymodel != XplMemorymodel_enum.LINEAL) writer.WriteAttributeString("memorymodel", CodeDOM_STV.XPLMEMORYMODEL_ENUM[(int) p_memorymodel]);
        if (p_defaultbitorder != XplBitorder_enum.IGNORE) writer.WriteAttributeString("defaultbitorder", CodeDOM_STV.XPLBITORDER_ENUM[(int) p_defaultbitorder]);
        if (p_defaultbyteorder != XplBitorder_enum.IGNORE) writer.WriteAttributeString("defaultbyteorder", CodeDOM_STV.XPLBITORDER_ENUM[(int) p_defaultbyteorder]);
        if (p_addresswidth != 32) writer.WriteAttributeString("addresswidth", CodeDOM_Utils.Att_ToString(p_addresswidth));
        if (p_databus != 32) writer.WriteAttributeString("databus", CodeDOM_Utils.Att_ToString(p_databus));
        if (p_commonregisterssize != 32) writer.WriteAttributeString("commonregisterssize", CodeDOM_Utils.Att_ToString(p_commonregisterssize));
        if (p_segments != 0) writer.WriteAttributeString("segments", CodeDOM_Utils.Att_ToString(p_segments));
        if (p_segmentsize != 0) writer.WriteAttributeString("segmentsize", CodeDOM_Utils.Att_ToString(p_segmentsize));
        if (p_threading != "") writer.WriteAttributeString("threading", CodeDOM_Utils.Att_ToString(p_threading));
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
                if (reader.Name().equals("uniqueName")) {
                    this.set_uniqueName(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("simpleName")) {
                    this.set_simpleName(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("minVersion")) {
                    this.set_minVersion(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("maxVersion")) {
                    this.set_maxVersion(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("description")) {
                    this.set_description(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("supportLevel")) {
                    tmpStr = CodeDOM_Utils.StringAtt_To_STRING(reader.Value());
                    count = 0;
                    flag = false;
                    for (int n = 0; n < CodeDOM_STV.XPLPLATFORMSUPPORTLEVEL_ENUM.length; n++) {
                        String str = CodeDOM_STV.XPLPLATFORMSUPPORTLEVEL_ENUM[n];
                        if (str.equals(tmpStr)) {
                            this.set_supportLevel(count);
                            flag = true;
                            break;
                        }
                        count++;
                    }
                } else if (reader.Name().equals("operatingsystem")) {
                    this.set_operatingsystem(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("aplication")) {
                    this.set_aplication(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("multitask")) {
                    this.set_multitask(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else if (reader.Name().equals("memorymodel")) {
                    tmpStr = CodeDOM_Utils.StringAtt_To_STRING(reader.Value());
                    count = 0;
                    flag = false;
                    for (int n = 0; n < CodeDOM_STV.XPLMEMORYMODEL_ENUM.length; n++) {
                        String str = CodeDOM_STV.XPLMEMORYMODEL_ENUM[n];
                        if (str.equals(tmpStr)) {
                            this.set_memorymodel(count);
                            flag = true;
                            break;
                        }
                        count++;
                    }
                } else if (reader.Name().equals("defaultbitorder")) {
                    tmpStr = CodeDOM_Utils.StringAtt_To_STRING(reader.Value());
                    count = 0;
                    flag = false;
                    for (int n = 0; n < CodeDOM_STV.XPLBITORDER_ENUM.length; n++) {
                        String str = CodeDOM_STV.XPLBITORDER_ENUM[n];
                        if (str.equals(tmpStr)) {
                            this.set_defaultbitorder(count);
                            flag = true;
                            break;
                        }
                        count++;
                    }
                } else if (reader.Name().equals("defaultbyteorder")) {
                    tmpStr = CodeDOM_Utils.StringAtt_To_STRING(reader.Value());
                    count = 0;
                    flag = false;
                    for (int n = 0; n < CodeDOM_STV.XPLBITORDER_ENUM.length; n++) {
                        String str = CodeDOM_STV.XPLBITORDER_ENUM[n];
                        if (str.equals(tmpStr)) {
                            this.set_defaultbyteorder(count);
                            flag = true;
                            break;
                        }
                        count++;
                    }
                } else if (reader.Name().equals("addresswidth")) {
                    this.set_addresswidth(CodeDOM_Utils.StringAtt_To_INT(reader.Value()));
                } else if (reader.Name().equals("databus")) {
                    this.set_databus(CodeDOM_Utils.StringAtt_To_INT(reader.Value()));
                } else if (reader.Name().equals("commonregisterssize")) {
                    this.set_commonregisterssize(CodeDOM_Utils.StringAtt_To_INT(reader.Value()));
                } else if (reader.Name().equals("segments")) {
                    this.set_segments(CodeDOM_Utils.StringAtt_To_INT(reader.Value()));
                } else if (reader.Name().equals("segmentsize")) {
                    this.set_segmentsize(CodeDOM_Utils.StringAtt_To_INT(reader.Value()));
                } else if (reader.Name().equals("threading")) {
                    this.set_threading(CodeDOM_Utils.StringAtt_To_STRING(reader.Value()));
                } else {
                    throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".Atributo '" + reader.Name() + "' invalido en elemento '" + this.get_Name() + "'.");
                }
            }
            reader.MoveToElement();
        }
        if (!reader.IsEmptyElement()) {
            reader.Read();
            while (reader.NodeType() != XmlNodeType.ENDELEMENT) {
                XplNode tempNode = null;
                switch(reader.NodeType()) {
                    case XmlNodeType.ELEMENT:
                    case XmlNodeType.TEXT:
                        throw new CodeDOM_Exception("Linea: " + reader.LineNumber() + ".No se esperaba texto o elemento hijo en este nodo.");
                    case XmlNodeType.ENDELEMENT:
                        break;
                    default:
                        break;
                }
                reader.Read();
            }
        }
        return this;
    }

    public String get_uniqueName() {
        return p_uniqueName;
    }

    public String get_simpleName() {
        return p_simpleName;
    }

    public String get_minVersion() {
        return p_minVersion;
    }

    public String get_maxVersion() {
        return p_maxVersion;
    }

    public String get_description() {
        return p_description;
    }

    public int get_supportLevel() {
        return p_supportLevel;
    }

    public String get_operatingsystem() {
        return p_operatingsystem;
    }

    public String get_aplication() {
        return p_aplication;
    }

    public String get_multitask() {
        return p_multitask;
    }

    public int get_memorymodel() {
        return p_memorymodel;
    }

    public int get_defaultbitorder() {
        return p_defaultbitorder;
    }

    public int get_defaultbyteorder() {
        return p_defaultbyteorder;
    }

    public int get_addresswidth() {
        return p_addresswidth;
    }

    public int get_databus() {
        return p_databus;
    }

    public int get_commonregisterssize() {
        return p_commonregisterssize;
    }

    public int get_segments() {
        return p_segments;
    }

    public int get_segmentsize() {
        return p_segmentsize;
    }

    public String get_threading() {
        return p_threading;
    }

    public String set_uniqueName(String new_uniqueName) {
        String back_uniqueName = p_uniqueName;
        p_uniqueName = new_uniqueName;
        return back_uniqueName;
    }

    public String set_simpleName(String new_simpleName) {
        String back_simpleName = p_simpleName;
        p_simpleName = new_simpleName;
        return back_simpleName;
    }

    public String set_minVersion(String new_minVersion) {
        String back_minVersion = p_minVersion;
        p_minVersion = new_minVersion;
        return back_minVersion;
    }

    public String set_maxVersion(String new_maxVersion) {
        String back_maxVersion = p_maxVersion;
        p_maxVersion = new_maxVersion;
        return back_maxVersion;
    }

    public String set_description(String new_description) {
        String back_description = p_description;
        p_description = new_description;
        return back_description;
    }

    public int set_supportLevel(int new_supportLevel) {
        int back_supportLevel = p_supportLevel;
        p_supportLevel = new_supportLevel;
        return back_supportLevel;
    }

    public String set_operatingsystem(String new_operatingsystem) {
        String back_operatingsystem = p_operatingsystem;
        p_operatingsystem = new_operatingsystem;
        return back_operatingsystem;
    }

    public String set_aplication(String new_aplication) {
        String back_aplication = p_aplication;
        p_aplication = new_aplication;
        return back_aplication;
    }

    public String set_multitask(String new_multitask) {
        String back_multitask = p_multitask;
        p_multitask = new_multitask;
        return back_multitask;
    }

    public int set_memorymodel(int new_memorymodel) {
        int back_memorymodel = p_memorymodel;
        p_memorymodel = new_memorymodel;
        return back_memorymodel;
    }

    public int set_defaultbitorder(int new_defaultbitorder) {
        int back_defaultbitorder = p_defaultbitorder;
        p_defaultbitorder = new_defaultbitorder;
        return back_defaultbitorder;
    }

    public int set_defaultbyteorder(int new_defaultbyteorder) {
        int back_defaultbyteorder = p_defaultbyteorder;
        p_defaultbyteorder = new_defaultbyteorder;
        return back_defaultbyteorder;
    }

    public int set_addresswidth(int new_addresswidth) {
        int back_addresswidth = p_addresswidth;
        p_addresswidth = new_addresswidth;
        return back_addresswidth;
    }

    public int set_databus(int new_databus) {
        int back_databus = p_databus;
        p_databus = new_databus;
        return back_databus;
    }

    public int set_commonregisterssize(int new_commonregisterssize) {
        int back_commonregisterssize = p_commonregisterssize;
        p_commonregisterssize = new_commonregisterssize;
        return back_commonregisterssize;
    }

    public int set_segments(int new_segments) {
        int back_segments = p_segments;
        p_segments = new_segments;
        return back_segments;
    }

    public int set_segmentsize(int new_segmentsize) {
        int back_segmentsize = p_segmentsize;
        p_segmentsize = new_segmentsize;
        return back_segmentsize;
    }

    public String set_threading(String new_threading) {
        String back_threading = p_threading;
        p_threading = new_threading;
        return back_threading;
    }
}
