package org.systemsbiology.PIPE2.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.systemsbiology.PIPE2.client.IDMapperServices;
import org.systemsbiology.PIPE2.domain.Namelist;
import org.systemsbiology.PIPE2.domain.Spreadsheet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.io.*;

public class IDMapperServicesImpl extends RemoteServiceServlet implements IDMapperServices {

    static String IDMAPPER_PIPELET_RESOURCE_DIR = "PIPEletResourceDir" + File.separator + "IDMapperPipelet" + File.separator;

    static String UPLOAD_DIR = IDMAPPER_PIPELET_RESOURCE_DIR + "uploads" + File.separator;

    static String DOWNLOAD_DIR = IDMAPPER_PIPELET_RESOURCE_DIR + "downloads" + File.separator;

    private HashMap<String, ArrayList<String>> ipiToEntrezIdMapper;

    public Namelist[] mapIDs(Namelist ids, String sourceType, String[] targetType, String[] filenames) throws Exception {
        Namelist[] retval = new Namelist[targetType.length];
        if (sourceType.equals("IPI Number")) if (ipiToEntrezIdMapper == null) ipiToEntrezIdMapper = getMapper(getMapping("IPI Number", "Entrez Gene ID"));
        for (int i = 0; i < targetType.length; i++) {
            Mapping mapping;
            if (!filenames[i].equals("")) {
                mapping = new Mapping();
                mapping.sourceIDtype = sourceType;
                mapping.destinationIDtype = targetType[i];
                mapping.filename = this.getServletContext().getRealPath("/") + UPLOAD_DIR + filenames[i];
                mapping.columnOfDestinationID = 1;
                mapping.columnOfSourceID = 0;
                retval[i] = mapIDsUsingMapper(ids, getMapper(mapping));
                retval[i].setName(mapping.destinationIDtype);
            } else if (sourceType.equals("IPI Number")) {
                if (targetType[i].equals("Entrez Gene ID")) {
                    retval[i] = mapIDsUsingMapper(ids, ipiToEntrezIdMapper);
                    retval[i].setName(targetType[i]);
                } else if (targetType[i].equals("Predicted TMH")) {
                    mapping = getMapping(sourceType, targetType[i]);
                    retval[i] = mapIDsUsingMapper(ids, getMapper(getMapping("IPI Number", "Predicted TMH")));
                    retval[i].setName(targetType[i]);
                } else {
                    mapping = getMapping("Entrez Gene ID", targetType[i]);
                    retval[i] = mapIDsUsingMapper(mapIDsUsingMapper(ids, ipiToEntrezIdMapper), getMapper(mapping));
                    retval[i].setName(targetType[i]);
                }
            } else {
                mapping = getMapping(sourceType, targetType[i]);
                if (mapping == null) throw new Exception("No mapping file found for " + sourceType + " to " + targetType[i] + " mappings.");
                retval[i] = mapIDsUsingMapper(ids, getMapper(mapping));
                retval[i].setName(targetType[i]);
            }
        }
        return retval;
    }

    private Namelist mapIDsUsingMapper(Namelist ids, HashMap<String, ArrayList<String>> mapper) {
        Namelist retval = new Namelist();
        retval.setSpecies(ids.getSpecies());
        retval.setNames(new String[ids.getNames().length]);
        for (int j = 0; j < retval.getNames().length; j++) {
            String[] curIDs_outer = ids.getNames()[j].split("; ");
            retval.getNames()[j] = "";
            for (String curID_outer : curIDs_outer) {
                if (mapper.containsKey(curID_outer.toLowerCase())) if (retval.getNames()[j].equals("")) retval.getNames()[j] = join("; ", mapper.get(curID_outer.toLowerCase())); else retval.getNames()[j] += "; " + join("; ", mapper.get(curID_outer.toLowerCase()));
            }
        }
        return retval;
    }

    public Namelist[] mapOrganismGeneSymbolsToGeneIDs(Namelist ids) throws Exception {
        Namelist[] retval = new Namelist[1];
        Mapping mapping = new Mapping();
        mapping.destinationIDtype = "Entrez Gene ID";
        mapping.sourceIDtype = "Gene Symbol";
        mapping.columnOfDestinationID = 1;
        mapping.columnOfSourceID = 0;
        mapping.filename = "geneSymbol-geneID_";
        if (ids.getSpecies().toLowerCase().equals("human") || ids.getSpecies().toLowerCase().equals("homo sapiens")) {
            mapping.filename += "human";
        } else if (ids.getSpecies().toLowerCase().equals("mouse") || ids.getSpecies().toLowerCase().equals("mus musculus")) {
            mapping.filename += "mouse";
        } else if (ids.getSpecies().toLowerCase().equals("rat") || ids.getSpecies().toLowerCase().equals("rattus norvegicus")) {
            mapping.filename += "rat";
        } else if (ids.getSpecies().toLowerCase().equals("yeast") || ids.getSpecies().toLowerCase().equals("saccharomyces cerevisiae")) {
            mapping.filename += "yeast";
        }
        mapping.filename += ".tsv";
        mapping.filename = this.getServletContext().getRealPath("/") + IDMAPPER_PIPELET_RESOURCE_DIR + mapping.filename;
        HashMap<String, ArrayList<String>> gene_symbol_id_mapper = getMapper(mapping);
        retval[0] = mapIDsUsingMapper(ids, gene_symbol_id_mapper);
        retval[0].setName(mapping.destinationIDtype);
        return retval;
    }

    private String join(String delimiter, ArrayList<String> strings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(strings.get(i));
        }
        return sb.toString();
    }

    private HashMap<String, ArrayList<String>> getMapper(Mapping mapping) throws Exception {
        HashMap<String, ArrayList<String>> retVal = new HashMap<String, ArrayList<String>>();
        System.out.println(mapping.filename);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(mapping.filename));
        String newLineOfText;
        while ((newLineOfText = bufferedReader.readLine()) != null) {
            if (newLineOfText.trim().equals("")) continue;
            String[] tokens = newLineOfText.split("\t");
            if (mapping.columnOfDestinationID > tokens.length - 1 || mapping.columnOfSourceID > tokens.length - 1) continue;
            String key = tokens[mapping.columnOfSourceID].toLowerCase();
            String val = tokens[mapping.columnOfDestinationID];
            if ((val != null && !val.equals("")) && (key != null && !key.equals(""))) {
                for (String subKey : key.split("; ")) {
                    if (!retVal.containsKey(subKey)) retVal.put(subKey, new ArrayList<String>());
                    retVal.get(subKey).add(val);
                }
            }
        }
        return retVal;
    }

    /**
     * tries to find a Mapping object defined in Mappings.xml
     * 
     * @param source
     * @param target
     * @return
     */
    private Mapping getMapping(String source, String target) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            String baseDir = this.getServletContext().getRealPath("/");
            DocumentBuilder db = dbf.newDocumentBuilder();
            File xmlMappingsFile = new File(baseDir + IDMAPPER_PIPELET_RESOURCE_DIR + "mappings.xml");
            Document dom = db.parse(xmlMappingsFile);
            Element docEle = dom.getDocumentElement();
            NodeList nl = docEle.getElementsByTagName("mapping");
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    if (source.equals(el.getAttribute("source-id-type")) && target.equals(el.getAttribute("destination-id-type"))) return getMappingObj(el);
                }
            }
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    private Mapping getMappingObj(Element element) {
        Mapping retVal = new Mapping();
        retVal.sourceIDtype = element.getAttribute("source-id-type");
        retVal.destinationIDtype = element.getAttribute("destination-id-type");
        retVal.filename = this.getServletContext().getRealPath("/") + IDMAPPER_PIPELET_RESOURCE_DIR + element.getAttribute("file");
        retVal.columnOfDestinationID = Integer.parseInt(element.getAttribute("destination-column-in-file"));
        retVal.columnOfSourceID = Integer.parseInt(element.getAttribute("source-column-in-file"));
        return retVal;
    }

    public String getUploadedFileAsDataTableJSONString(String filename) {
        StringBuffer retVal = new StringBuffer();
        String datasetName = "";
        String species = "";
        int total_number_of_columns = 0;
        BufferedReader br;
        String sCurrentLine;
        Boolean headersSet = false;
        File file = new File(getServletContext().getRealPath(File.separator + UPLOAD_DIR + filename));
        int number_col = -1;
        try {
            br = new BufferedReader(new FileReader(file));
            retVal.append("{");
            while ((sCurrentLine = br.readLine()) != null) {
                if (sCurrentLine.startsWith("#Name: ")) {
                    datasetName = sCurrentLine.substring(7);
                    continue;
                } else if (sCurrentLine.startsWith("#Organism: ")) {
                    species = sCurrentLine.substring(11);
                    continue;
                } else if (sCurrentLine.startsWith("#") || sCurrentLine.trim().equals("")) continue;
                String[] values = sCurrentLine.split("\t");
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                    values[i] = values[i].replace("\"", "");
                }
                if (!headersSet) {
                    retVal.append("cols:[");
                    int i = 0;
                    for (; i < values.length; i++) {
                        if (i > 0) retVal.append(",");
                        if (values[i].equals("Number of Patient")) {
                            retVal.append("{type:'number', label: \"" + values[i] + "\"}");
                            number_col = i;
                        } else retVal.append("{type:'string', label: \"" + values[i] + "\"}");
                    }
                    total_number_of_columns = i;
                    retVal.append("],");
                    retVal.append("rows:[");
                    headersSet = true;
                } else {
                    retVal.append("{c:[");
                    int i = 0;
                    for (; i < values.length; i++) {
                        if (i > 0) retVal.append(",");
                        if (i == number_col) retVal.append("{v:" + values[i] + "}"); else retVal.append("{v:\"" + values[i] + "\"}");
                    }
                    for (; i < total_number_of_columns; i++) {
                        if (i > 0) retVal.append(",");
                        retVal.append("{v:\"\"}");
                    }
                    retVal.append("]},");
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File " + file.getAbsolutePath() + " could not be found on filesystem");
        } catch (IOException ioe) {
            System.out.println("Exception while reading the file" + ioe);
        }
        if (retVal.substring(retVal.length() - 1).equals(",")) retVal.deleteCharAt(retVal.length() - 1);
        retVal.append("]");
        if (!datasetName.equals("")) retVal.append(",name:'" + datasetName + "'");
        if (!species.equals("")) retVal.append(",species:'" + species + "'");
        retVal.append("}");
        return retVal.toString();
    }

    /**
     * write file and return relative path to it
     *
     * @param spreadsheet
     * @return
     * @throws Exception
     */
    public String downloadDataTableAsExcell(Spreadsheet spreadsheet) throws Exception {
        String filename = (spreadsheet.getName() + (new Date()).toString()).replaceAll(" ", "_").replaceAll(":", "-") + ".xls";
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(this.getServletContext().getRealPath("/") + DOWNLOAD_DIR + filename, false));
            out.write("#Name: " + spreadsheet.getName());
            out.newLine();
            out.write("#Organism: " + spreadsheet.getSpecies());
            out.newLine();
            out.write("#Rows: " + spreadsheet.getNumberOfRows());
            out.newLine();
            out.newLine();
            for (int i = 0; i < spreadsheet.getNumberOfColumns(); i++) {
                if (i > 0) out.write("\t");
                out.write(spreadsheet.getColumnLabel(i));
            }
            out.newLine();
            for (int i = 0; i < spreadsheet.getNumberOfRows(); i++) {
                for (int j = 0; j < spreadsheet.getNumberOfColumns(); j++) {
                    if (j > 0) out.write("\t");
                    out.write(spreadsheet.get(i, j));
                }
                out.newLine();
            }
            out.close();
        } catch (IOException e) {
            System.out.println("There was a problem:" + e);
            throw e;
        }
        return DOWNLOAD_DIR.replaceAll("\\\\", "/") + filename;
    }

    private class Mapping {

        protected String sourceIDtype;

        protected String destinationIDtype;

        protected String filename;

        protected int columnOfDestinationID;

        protected int columnOfSourceID;
    }
}
