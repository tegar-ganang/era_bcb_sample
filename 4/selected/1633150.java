package com.emeraldjb.generator.io.classes;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import com.emeraldjb.EmdFactory;
import com.emeraldjb.base.EmeraldjbBean;
import com.emeraldjb.base.EmeraldjbException;
import com.emeraldjb.base.Entity;
import com.emeraldjb.base.Member;
import com.emeraldjb.base.PatternSpec;
import com.emeraldjb.base.Section;
import com.emeraldjb.base.xml.EmeraldjbSchemaConst;
import com.emeraldjb.base.xml.EmeraldjbXMLReader;
import com.emeraldjb.base.xml.XMLSectionReader;
import com.emeraldjb.generator.ClassGenerator;
import com.emeraldjb.generator.GeneratorConst;
import com.emeraldjb.generator.MethodGenerator;
import com.emeraldjb.generator.dao.DaoGeneratorUtils;
import com.emeraldjb.generator.dao.classes.DaoValuesGenerator;
import com.emeraldjb.generator.dao.methods.DaoValuesMethodGenerator;
import com.emeraldjb.generator.javatypes.JTypeBase;

/**
 * <p>
 * Generates XML streamer for an object.
 * </p>
 * <p>
 * Copyright (c) 2003, 2004 by Emeraldjb LLC<br>
 * All Rights Reserved.
 * </p>
 */
public class XmlStreamGenerator implements ClassGenerator {

    private MethodGenerator methodGenerator = new DaoValuesMethodGenerator();

    public static void main(String[] args) {
        try {
            XmlStreamGenerator.testXmlStreamGen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testXmlStreamGen() throws Exception {
        XMLSectionReader xsr = new XMLSectionReader();
        Section section = new Section();
        xsr.setSection(section);
        InputStream is = xsr.getInputStream("c:/dev/emeraldjb/src/ipxml/section_emeraldjb_jade.xml", EmeraldjbXMLReader.FILE_STREAM);
        xsr.startParsing(is, xsr, EmeraldjbSchemaConst.ELEM_SECTION);
        Entity entity = (Entity) section.getEntities().iterator().next();
        XmlStreamGenerator dvg = new XmlStreamGenerator();
        String streamerClass = dvg.getClass(entity);
        System.out.println(streamerClass);
    }

    public String getClass(EmeraldjbBean eb) throws EmeraldjbException {
        Entity entity = (Entity) eb;
        StringBuffer sb = new StringBuffer();
        String myPackage = getPackageName(eb);
        sb.append("package " + myPackage + ";\n");
        sb.append("\n");
        DaoValuesGenerator valgen = new DaoValuesGenerator();
        String values_class_name = valgen.getClassName(entity);
        sb.append("\n");
        List importList = new Vector();
        importList.add("java.io.*;");
        importList.add("java.sql.Date;");
        importList.add("com.emeraldjb.runtime.patternXmlObj.*;");
        importList.add("javax.xml.parsers.*;");
        importList.add("java.text.ParseException;");
        importList.add("org.xml.sax.*;");
        importList.add("org.xml.sax.helpers.*;");
        importList.add(valgen.getPackageName(eb) + "." + values_class_name + ";");
        Iterator it = importList.iterator();
        while (it.hasNext()) {
            String importName = (String) it.next();
            sb.append("import " + importName + "\n");
        }
        sb.append("\n");
        String proto_version = entity.getPatternValue(GeneratorConst.PATTERN_STREAM_PROTO_VERSION, "1");
        boolean short_version = entity.getPatternBooleanValue(GeneratorConst.PATTERN_STREAM_XML_SHORT, false);
        StringBuffer preface = new StringBuffer();
        StringBuffer consts = new StringBuffer();
        StringBuffer f_writer = new StringBuffer();
        StringBuffer f_writer_short = new StringBuffer();
        StringBuffer f_reader = new StringBuffer();
        StringBuffer end_elems = new StringBuffer();
        boolean end_elem_needs_catch = false;
        consts.append("\n  public static final String EL_CLASS_TAG=\"" + values_class_name + "\";");
        preface.append("\n    xos.print(\"<!-- This format is optimised for space, below are the column mappings\");");
        boolean has_times = false;
        boolean has_strings = false;
        it = entity.getMembers().iterator();
        int col_num = 0;
        while (it.hasNext()) {
            col_num++;
            Member member = (Member) it.next();
            String nm = member.getName();
            preface.append("\n    xos.print(\"c" + col_num + " = " + nm + "\");");
            String elem_name = nm;
            String elem_name_short = "c" + col_num;
            String el_name = nm.toUpperCase();
            if (member.getColLen() > 0 || !member.isNullAllowed()) {
                end_elem_needs_catch = true;
            }
            String element_const = "EL_" + el_name;
            String element_const_short = "EL_" + el_name + "_SHORT";
            consts.append("\n  public static final String " + element_const + "=\"" + elem_name + "\";" + "\n  public static final String " + element_const_short + "=\"" + elem_name_short + "\";");
            String getter = "obj." + methodGenerator.getMethodName(DaoGeneratorUtils.METHOD_GET, member);
            String setter = "values_." + methodGenerator.getMethodName(DaoGeneratorUtils.METHOD_SET, member);
            String pad = "    ";
            JTypeBase gen_type = EmdFactory.getJTypeFactory().getJavaType(member.getType());
            f_writer.append(gen_type.getToXmlCode(pad, element_const, getter + "()"));
            f_writer_short.append(gen_type.getToXmlCode(pad, element_const_short, getter + "()"));
            end_elems.append(gen_type.getFromXmlCode(pad, element_const, setter));
            end_elems.append("\n    //and also the short version");
            end_elems.append(gen_type.getFromXmlCode(pad, element_const_short, setter));
        }
        preface.append("\n    xos.print(\"-->\");");
        String body_part = f_writer.toString();
        String body_part_short = preface.toString() + f_writer_short.toString();
        String reader_vars = "";
        String streamer_class_name = getClassName(entity);
        sb.append("public class " + streamer_class_name + "  extends DefaultHandler implements TSParser\n");
        sb.append("{" + consts + "\n  public static final int PROTO_VERSION=" + proto_version + ";" + "\n  private transient StringBuffer cdata_=new StringBuffer();" + "\n  private transient String endElement_;" + "\n  private transient TSParser parentParser_;" + "\n  private transient XMLReader theReader_;\n" + "\n  private " + values_class_name + " values_;");
        sb.append("\n\n");
        sb.append("\n  /**" + "\n   * This is really only here as an example.  It is very rare to write a single" + "\n   * object to a file - far more likely to have a collection or object graph.  " + "\n   * in which case you can write something similar - maybe using the writeXmlShort" + "\n   * version instread." + "\n   */" + "\n  public static void writeToFile(String file_nm, " + values_class_name + " obj) throws IOException" + "\n  {" + "\n    if (file_nm==null || file_nm.length()==0) throw new IOException(\"Bad file name (null or zero length)\");" + "\n    if (obj==null) throw new IOException(\"Bad value object parameter, cannot write null object to file\");" + "\n    FileOutputStream fos = new FileOutputStream(file_nm);" + "\n    XmlOutputFilter xos = new XmlOutputFilter(fos);" + "\n    xos.openElement(\"FILE_\"+EL_CLASS_TAG);" + "\n    writeXml(xos, obj);" + "\n    xos.closeElement();" + "\n    fos.close();" + "\n  } // end of writeToFile" + "\n" + "\n  public static void readFromFile(String file_nm, " + values_class_name + " obj) throws IOException, SAXException" + "\n  {" + "\n    if (file_nm==null || file_nm.length()==0) throw new IOException(\"Bad file name (null or zero length)\");" + "\n    if (obj==null) throw new IOException(\"Bad value object parameter, cannot write null object to file\");" + "\n    FileInputStream fis = new FileInputStream(file_nm);" + "\n    DataInputStream dis = new DataInputStream(fis);" + "\n    marshalFromXml(dis, obj);" + "\n    fis.close();" + "\n  } // end of readFromFile" + "\n" + "\n  public static void writeXml(XmlOutputFilter xos, " + values_class_name + " obj) throws IOException" + "\n  {" + "\n    xos.openElement(EL_CLASS_TAG);" + body_part + "\n    xos.closeElement();" + "\n  } // end of writeXml" + "\n" + "\n  public static void writeXmlShort(XmlOutputFilter xos, " + values_class_name + " obj) throws IOException" + "\n  {" + "\n    xos.openElement(EL_CLASS_TAG);" + body_part_short + "\n    xos.closeElement();" + "\n  } // end of writeXml" + "\n" + "\n  public " + streamer_class_name + "(" + values_class_name + " obj) {" + "\n    values_ = obj;" + "\n  } // end of ctor" + "\n");
        String xml_bit = addXmlFunctions(streamer_class_name, values_class_name, end_elem_needs_catch, end_elems, f_reader);
        String close = "\n" + "\n} // end of classs" + "\n\n" + "\n//**************" + "\n// End of file" + "\n//**************";
        return sb.toString() + xml_bit + close;
    }

    /**
  * Adds the cdata, start elem end elem etc functions to the class
  * @param end_element_needs_catch If true put a try/catch arround end elems
  * @param end_elems A section of text which checks the end elements and
  *  sets the correct member variable
  * @param dump_to_xml The guts of the func which can output xml
  * @return the string holding the functions
  */
    public String addXmlFunctions(String class_name, String values_class_name, boolean end_element_needs_catch, StringBuffer end_elems, StringBuffer dump_to_xml) {
        StringBuffer ret = new StringBuffer();
        ret.append("\n  //********************************************************************" + "\n  // These functions enable the class for SAX 2 parsing of streams" + "\n  // Which means its now VERY simple to hand parsing of streams to the" + "\n  // object or to request that it streams out to XML.  These classes" + "\n  // make use of the Tall Software XML utility classes to escape characters" + "\n  // and to code up the TSParser i/f which allows nesting of objects" + "\n  //********************************************************************" + "\n\n  /**" + "\n  * This is called when I am part of a larger TSParser infrastructure" + "\n  * Basically call this to set me as the new parser (ie on start element)" + "\n  * and I will call back to subElementDone() on the XmlParentParser interface" + "\n  */" + "\n  public void setParser(XMLReader reader,TSParser parent, String el_name,  Attributes attribs) {" + "\n    theReader_ = reader;" + "\n    parentParser_ = parent;" + "\n    endElement_ = el_name;" + "\n    " + "\n    reader.setContentHandler((ContentHandler)this);" + "\n  } // end of setParser" + "\n\n  /**" + "\n  * This is called by any nested parser, when its finished then it can" + "\n  * call me back to this function" + "\n  */" + "\n  public void childFinishedParsing(TSParser child, String el_name)" + "\n  {" + "\n    theReader_.setContentHandler(this);" + "\n    // If had dependant classes could record them here" + "\n  }// end of startParsing");
        ret.append("\n\n  /**" + "\n  * This is called by the parser when there is cdata" + "\n  */" + "\n  public void characters(char[] ch," + "\n                         int start," + "\n                         int length)" + "\n                  throws SAXException {" + "\n    cdata_.append(ch, start, length);" + "\n  } // end of charData" + "\n " + "\n\n  /**" + "\n  * called by parser" + "\n  */" + "\npublic void startElement(java.lang.String uri," + "\n                         java.lang.String local_name," + "\n                         java.lang.String raw_ame," + "\n                         Attributes attributes)" + "\n                  throws SAXException {" + "\n    cdata_.setLength(0);" + "\n    // If have dependant classes then do them here " + "\n  } // end of startElement" + "\n " + "\n  /**" + "\n  * called by parser" + "\n  * @param namespaceURI - The Namespace URI, or the empty string if the element has" + "\n  * no Namespace URI or if Namespace processing is not being performed." + "\n  * @param local_name - The local name (without prefix), or the empty string" + "\n  * if Namespace processing is not being performed." + "\n  * @param qName - The qualified XML 1.0 name (with prefix), or the empty" + "\n  * string if qualified names are not available." + "\n  * @exception SAXException - Any SAX exception, possibly wrapping another" + "\n  * exception" + "\n  */" + "\n  public void endElement(java.lang.String namespaceURI," + "\n                       java.lang.String local_name," + "\n                       java.lang.String qName)" + "\n                throws SAXException {" + "\n	   // In jdk1.4 the local name is null and qname is used." + "\n  	 if (local_name==null || local_name.length()==0) local_name=qName;" + "\n" + "\n    if (local_name.equals(endElement_) && parentParser_!=null) {" + "\n      parentParser_.childFinishedParsing(this, local_name);" + "\n      parentParser_=null;" + "\n    }" + "\n");
        if (end_element_needs_catch) {
            ret.append("\n    try {" + end_elems.toString() + "\n    } catch (IllegalArgumentException ex) {" + "\n      throw new SAXException(ex.getMessage());" + "\n    }");
        } else {
            ret.append(end_elems.toString());
        }
        ret.append("\n  } // end of endElement");
        ret.append("\n\n  /**" + "\n  * This unmarshals the class from an input stream" + "\n  * Only use this if the stream holds ONLY this class" + "\n  * @param is The input stream  holding only this class" + "\n  * @exception SAXException if something goes wrong with the read" + "\n  */" + "\n  public static void marshalFromXml(InputStream is," + values_class_name + " obj) throws SAXException {" + "\n    try {" + "\n      // Create a JAXP SAXParserFactory and configure it" + "\n      SAXParserFactory spf = SAXParserFactory.newInstance();" + "\n      spf.setValidating(false);" + "\n" + "\n      XMLReader xml_reader = null;" + "\n      // Create a JAXP SAXParser" + "\n      SAXParser saxParser = spf.newSAXParser();" + "\n" + "\n      // Get the encapsulated SAX XMLReader" + "\n      xml_reader = saxParser.getXMLReader();" + "\n" + "\n      // Set the ContentHandler of the XMLReader" + "\n      " + class_name + " me_parser = new " + class_name + "(obj);" + "\n      me_parser.setParser(xml_reader, null, me_parser.EL_CLASS_TAG, null);" + "\n" + "\n      // Tell the XMLReader to parse the XML document" + "\n      xml_reader.parse(new InputSource(is));" + "\n    } catch (Exception ex) {" + "\n      throw new SAXException(\"Got exception:\"+ex.getClass().getName()+\" with message :\"+ex.getMessage());" + "\n    }" + "\n  } // end of marshalFromXml");
        return ret.toString();
    }

    /**
   * returns the package holding the class
   * @param eb
   * @return
   */
    public String getPackageName(EmeraldjbBean eb) throws EmeraldjbException {
        PatternSpec p = eb.getPattern(GeneratorConst.PATTERN_STREAM_CLASSPATH);
        if (p == null) {
            throw new EmeraldjbException(EmeraldjbException.EMERALDJB_E_NAME_NOT_FOUND, "Schema error: no pattern defined named " + GeneratorConst.PATTERN_STREAM_CLASSPATH + " for entity " + ((Entity) eb).getName());
        }
        return p.getValue();
    }

    public String getClassName(EmeraldjbBean eb) {
        Entity entity = (Entity) eb;
        return DaoGeneratorUtils.genClassName(entity.getName()) + "XmlStreamer";
    }

    public String getInterface(EmeraldjbBean eb) {
        throw new RuntimeException("No interface for XmlStreamers");
    }

    public String getInterfaceName(EmeraldjbBean eb) {
        throw new RuntimeException("No interface for XmlStreamers");
    }

    /**
	 * @return
	 */
    public MethodGenerator getMethodGenerator() {
        return methodGenerator;
    }

    /**
	 * @param generator
	 */
    public void setMethodGenerator(MethodGenerator generator) {
        methodGenerator = generator;
    }

    public boolean hasInterface(EmeraldjbBean eb) {
        return false;
    }

    public boolean hasClassImpl(EmeraldjbBean eb) {
        return true;
    }

    public String getJavadoc(EmeraldjbBean eb) throws EmeraldjbException {
        String javadoc = "";
        javadoc += "/**\n";
        javadoc += "**/\n";
        return javadoc;
    }
}
