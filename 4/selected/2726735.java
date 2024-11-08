package com.isa.jump.plugin;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.*;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import com.vividsolutions.jump.io.*;
import com.vividsolutions.jump.io.datasource.DelegatingCompressedFileHandler;
import com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource;

public class KMLWriter implements JUMPWriter {

    public static String standard_doc = "Document";

    public static String standard_schema = "Schema";

    public static String standard_simplefield = "SimpleField";

    public static String standard_geom = "geometry";

    public static String standard_folder = "Folder";

    public static String standard_name = "name";

    public static String placemarkName = "Placemark";

    private KMLOutputTemplate outputTemplate = null;

    private KMLGeometryWriter geometryWriter = new KMLGeometryWriter();

    /** constructor**/
    public KMLWriter() {
        geometryWriter.setLinePrefix("                ");
    }

    private static class ClassicReaderWriterFileDataSource extends StandardReaderWriterFileDataSource {

        public ClassicReaderWriterFileDataSource(JUMPReader reader, JUMPWriter writer, String[] extensions) {
            super(new DelegatingCompressedFileHandler(reader, toEndings(extensions)), writer, extensions);
            this.extensions = extensions;
        }
    }

    public static class KML extends ClassicReaderWriterFileDataSource {

        public KML() {
            super(new KMLReader(), new KMLWriter(), new String[] { "kml" });
        }
    }

    /**
     * Main entry function - write the KML file.
     * @param featureCollection features to write
     * @param dp specify the 'OuputFile' and 'OuputTemplateFile'
     */
    public void write(FeatureCollection featureCollection, DriverProperties dp) throws IllegalParametersException, Exception {
        String outputFname;
        double centralMeridian = 0.0;
        outputFname = dp.getProperty("File");
        String UTMZone = dp.getProperty("UTM_Zone");
        String centralMeridianStr = dp.getProperty("Central_Meridian");
        if (outputFname == null) {
            outputFname = dp.getProperty("DefaultValue");
        }
        if (outputFname == null) {
            throw new IllegalParametersException("call to KMLWRite.write() has DriverProperties w/o a OutputFile specified");
        }
        if ((UTMZone != null) && (centralMeridianStr != null)) {
            if ((UTMZone.length() > 0) && (centralMeridianStr.length() > 0)) {
                centralMeridian = Double.parseDouble(dp.getProperty("Central_Meridian"));
                geometryWriter.setParameters(UTMZone, centralMeridian);
            }
            outputTemplate = KMLWriter.makeOutputTemplate(featureCollection.getFeatureSchema());
            java.io.Writer w = new java.io.BufferedWriter(new java.io.FileWriter(outputFname));
            this.write(featureCollection, w);
            w.close();
        }
    }

    private void write(FeatureCollection featureCollection, java.io.Writer writer) throws Exception {
        BufferedWriter buffWriter;
        Feature f;
        String pre;
        String token;
        if (outputTemplate == null) {
            throw new Exception("attempt to write KML w/o specifying the output template");
        }
        buffWriter = new BufferedWriter(writer);
        buffWriter.write(outputTemplate.headerText);
        for (Iterator t = featureCollection.iterator(); t.hasNext(); ) {
            f = (Feature) t.next();
            for (int u = 0; u < outputTemplate.featureText.size(); u++) {
                String evaled;
                pre = (String) outputTemplate.featureText.get(u);
                token = (String) outputTemplate.codingText.get(u);
                buffWriter.write(pre);
                evaled = evaluateToken(f, token);
                if (evaled == null) {
                    evaled = "";
                }
                buffWriter.write(evaled);
            }
            buffWriter.write(outputTemplate.featureTextfooter);
            buffWriter.write("\n");
        }
        buffWriter.write(outputTemplate.footerText);
        buffWriter.flush();
    }

    /**
     *Convert an arbitary string into something that will not cause XML to gack.
     * Ie. convert "<" to "&lt;"
     *@param s string to safe-ify
     */
    private static String safeXML(String s) {
        StringBuffer sb = new StringBuffer(s);
        char c;
        for (int t = 0; t < sb.length(); t++) {
            c = sb.charAt(t);
            if (c == '<') {
                sb.replace(t, t + 1, "&lt;");
            }
            if (c == '>') {
                sb.replace(t, t + 1, "&gt;");
            }
            if (c == '&') {
                sb.replace(t, t + 1, "&amp;");
            }
            if (c == '\'') {
                sb.replace(t, t + 1, "&apos;");
            }
            if (c == '"') {
                sb.replace(t, t + 1, "&quot;");
            }
        }
        return sb.toString();
    }

    /**
     *takes a token and replaces it with its value (ie. geometry or column)
     * @param f feature to take geometry or column value from
     *@token token to evaluate - "column","geometry" or "geometrytype"
     */
    private String evaluateToken(Feature f, String token) throws Exception, ParseException {
        String column;
        String cmd;
        String result;
        int index;
        token = token.trim();
        if (!(token.startsWith("=")) || (token.length() < 7)) {
            throw new ParseException("couldn't understand token '" + token + "' in the output template");
        }
        token = token.substring(1);
        token = token.trim();
        index = token.indexOf(" ");
        if (index == -1) {
            cmd = token;
        } else {
            cmd = token.substring(0, token.indexOf(" "));
        }
        if (cmd.equalsIgnoreCase("column")) {
            column = token.substring(6);
            column = column.trim();
            result = toString(f, column);
            result = safeXML(result);
            return result;
        } else if (cmd.equalsIgnoreCase("geometry")) {
            geometryWriter.setMaximumCoordinatesPerLine(1);
            return geometryWriter.write(f.getGeometry());
        } else if (cmd.equalsIgnoreCase("geometrytype")) {
            return f.getGeometry().getGeometryType();
        } else {
            throw new ParseException("couldn't understand token '" + token + "' in the output template");
        }
    }

    protected String toString(Feature f, String column) {
        if (column.equalsIgnoreCase("FID")) return "" + f.getID();
        Assert.isTrue(f.getSchema().getAttributeType(column) != AttributeType.GEOMETRY);
        Object attribute = f.getAttribute(column);
        if (attribute == null) {
            return "";
        }
        if (attribute instanceof Date) {
            return format((Date) attribute);
        }
        return attribute.toString();
    }

    protected String format(Date date) {
        return dateFormatter.format(date);
    }

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    /** given a FEatureSchema, make an output template
     *  in the JCS  format
     * @param fcmd input featureSchema
     */
    private static KMLOutputTemplate makeOutputTemplate(FeatureSchema fcmd) {
        KMLOutputTemplate result;
        int t;
        String colName;
        String colText = "";
        String colCode = "";
        String colHeader = "";
        result = new KMLOutputTemplate();
        result.setHeaderText("<?xml version='1.0' encoding='UTF-8'?>\n<kml xmlns=\"http://earth.google.com/kml/2.0\" >\n" + "<" + standard_doc + "> \n" + "  <" + standard_name + ">" + "Doc1" + "</" + standard_name + ">\n" + getSchemaHeader(fcmd) + "    <" + standard_folder + ">\n");
        colText = "";
        colHeader = "        <" + placemarkName + "> \n";
        colText = colHeader + "          <" + standard_name + ">\n";
        colCode = "=COLUMN FID";
        colHeader = "\n          </" + standard_name + ">\n";
        result.addItem(colText, colCode);
        for (t = 0; t < fcmd.getAttributeCount(); t++) {
            colName = fcmd.getAttributeName(t);
            colText = "";
            if (t != fcmd.getGeometryIndex()) {
                colText = colHeader + "          <" + colName + ">\n";
                colCode = "=COLUMN " + colName;
                colHeader = "\n          </" + colName + ">\n";
            } else {
                colText = colHeader;
                colCode = "=GEOMETRY";
                colHeader = "\n";
            }
            result.addItem(colText, colCode);
        }
        result.setFeatureFooter(colHeader + "     </" + placemarkName + ">\n");
        result.setFooterText("    </" + standard_folder + ">\n" + "</" + standard_doc + ">\n" + "</kml>\n");
        return result;
    }

    private static String getSchemaHeader(FeatureSchema fcmd) {
        String schemaHeader;
        String fieldLine = "    <" + standard_simplefield + " type=\"wstring\" name=\"";
        schemaHeader = "  <" + standard_schema + " parent=\"Placemark\" name=\"" + placemarkName + "\">\n";
        for (int t = 0; t < fcmd.getAttributeCount(); t++) {
            String colName = fcmd.getAttributeName(t);
            if (!colName.equalsIgnoreCase("Geometry") && (!colName.equalsIgnoreCase("FID"))) {
                schemaHeader = schemaHeader + fieldLine + colName + "\"/>\n";
            }
        }
        schemaHeader = schemaHeader + "  </" + standard_schema + ">\n";
        return schemaHeader;
    }
}
