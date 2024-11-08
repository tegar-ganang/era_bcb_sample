package edu.unibi.agbi.dawismd.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.commons.io.FileUtils;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import au.com.bytecode.opencsv.CSVWriter;
import edu.unibi.agbi.dawismd.config.Config;
import edu.unibi.agbi.dawismd.config.logging.Log;
import edu.unibi.agbi.trabi.pojos.PSSMPojo;
import edu.unibi.agbi.trabi.sequencelogo.SequenceLogoFactory;

/**
 * @author Klaus Hippe
 * @version 1.0 11.08.2008
 */
public class WriteFile {

    private final String LINE_SEPARATOR = System.getProperty("line.separator");

    private String FILE;

    public WriteFile() {
        FILE = new String();
    }

    /**
	 * @param pssm_pojo
	 * @param file_id
	 */
    public void sequenceLogo(PSSMPojo pssm_pojo, String file_id) {
        File seq_logo = new File(Config.getTmpPath() + file_id + FileFormat.PNG.getFileType());
        try {
            ImageIO.write(SequenceLogoFactory.createLogo(pssm_pojo, true), FileFormat.PNG.name(), seq_logo);
        } catch (Exception e) {
            Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
        }
        FILE = Config.getWebTmpPath() + file_id + FileFormat.PNG.getFileType();
    }

    /**
	 * @param file_id
	 * @param entry
	 */
    public void matrix(String file_id, String entry) {
        create(Config.getTmpPath() + file_id + FileFormat.TXT.getFileType(), entry);
        FILE = Config.getWebTmpPath() + file_id + FileFormat.TXT.getFileType();
    }

    /**
	 * @param file_id
	 * @param id
	 * @param entry
	 */
    public void sequence(String file_id, String id, String entry) {
        String input = entry.replaceAll("\\s", "").replaceAll("\\p{Blank}", "").trim();
        String head = ">" + id + "|" + input.length() + LINE_SEPARATOR;
        String fasta = new String();
        int j = 0;
        for (int i = 0; i < input.length(); i++) {
            if (j == 80) {
                fasta = fasta + LINE_SEPARATOR;
                j = -1;
            } else {
                fasta = fasta + input.charAt(i);
            }
            j++;
        }
        fasta = head + fasta;
        create(Config.getTmpPath() + file_id + FileFormat.FASTA.getFileType(), fasta);
        FILE = Config.getWebTmpPath() + file_id + FileFormat.FASTA.getFileType();
    }

    /**
	 * @param session_id
	 * @param id
	 * @param domain
	 * @param db
	 * @param map
	 */
    public void xml(String session_id, String id, String domain, String db, HashMap<String, ArrayList<String>> map) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = document.createElement("entry");
            root.setAttribute("id", id);
            root.setAttribute("domain", domain);
            root.setAttribute("database", db);
            document.appendChild(root);
            for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
                Element element = document.createElement(entry.getKey());
                root.appendChild(element);
                for (String value : entry.getValue()) {
                    Element element_id = document.createElement("id");
                    element_id.setTextContent(value);
                    element.appendChild(element_id);
                }
            }
            File file = new File(Config.getTmpPath() + session_id + FileFormat.XML.getFileType());
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(file));
            zip(session_id, FileFormat.XML, file);
            FILE = Config.getWebTmpPath() + session_id + FileFormat.XML.getFileType() + FileFormat.ZIP.getFileType();
        } catch (Exception e) {
            Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
        }
    }

    /**
	 * @param session_id
	 * @param id
	 * @param domain
	 * @param db
	 * @param map
	 */
    public void sbml(String session_id, String id, String domain, String db, HashMap<String, ArrayList<String>> map) {
        SBMLDocument doc = new SBMLDocument(2, 4);
        Model model = doc.createModel("default");
        model.setName(session_id);
        History history = new History();
        Creator creator = new Creator("Bioinformatics / Medical Informatics Department", "Faculty of Technology", "Bielefeld University", Config.getEmailAddress());
        history.addCreator(creator);
        model.setHistory(history);
        Compartment compartment = model.createCompartment(domain);
        compartment.setName("unknown");
        compartment.setSize(1d);
        Species root = model.createSpecies("root", compartment);
        root.setName(id);
        root.setValue(1d);
        for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
            Compartment c = model.getCompartment(entry.getKey());
            if (c == null) {
                c = model.createCompartment(entry.getKey());
                c.setName("unknown");
                c.setSize(1d);
            }
            for (int i = 0; i < entry.getValue().size(); i++) {
                Species species = model.createSpecies(entry.getKey() + "_species_id_" + i, c);
                species.setName(entry.getValue().get(i));
                species.setValue(1d);
                Reaction reaction = model.createReaction(entry.getKey() + "_reaction_id_" + i);
                reaction.createReactant(root);
                reaction.createProduct(species);
            }
        }
        try {
            File file = new File(Config.getTmpPath() + session_id + FileFormat.SBML.getFileType());
            SBMLWriter.write(doc, file, "DAWIS-M.D.", "2.0");
            zip(session_id, FileFormat.SBML, file);
            FILE = Config.getWebTmpPath() + session_id + FileFormat.SBML.getFileType() + FileFormat.ZIP.getFileType();
        } catch (Exception e) {
            Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
        }
    }

    /**
	 * @param file_id
	 * @param entry
	 */
    public void txt(String file_id, String entry) {
        create(Config.getTmpPath() + file_id + FileFormat.TXT.getFileType(), entry);
        FILE = Config.getWebTmpPath() + file_id + FileFormat.TXT.getFileType();
    }

    /**
	 * 
	 * @param session_id
	 * @param id
	 * @param domain
	 * @param db
	 * @param map
	 */
    public void csv(String session_id, String id, String domain, String db, HashMap<String, ArrayList<String>> map) {
        try {
            File file = new File(Config.getTmpPath() + session_id + FileFormat.CSV.getFileType());
            CSVWriter writer = new CSVWriter(new FileWriter(file));
            writer.writeNext(new String[] { "ID", "DOMAIN", "DATABASE" });
            writer.writeNext(new String[] { id, domain, db });
            for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
                for (String value : entry.getValue()) {
                    writer.writeNext(new String[] { value, entry.getKey(), "Unknown" });
                }
            }
            writer.close();
            zip(session_id, FileFormat.CSV, file);
            FILE = Config.getWebTmpPath() + session_id + FileFormat.CSV.getFileType() + FileFormat.ZIP.getFileType();
        } catch (Exception e) {
            Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
        }
    }

    /**
	 * @param session_id
	 * @param id
	 * @param domain
	 * @param db
	 * @param map
	 */
    public void excel(String session_id, String id, String domain, String db, HashMap<String, ArrayList<String>> map) {
        try {
            File file = new File(Config.getTmpPath() + session_id + FileFormat.XLS.getFileType());
            WritableWorkbook workbook = Workbook.createWorkbook(file);
            WritableSheet sheet = workbook.createSheet(" ", 0);
            sheet.addCell(new Label(0, 0, "Id"));
            sheet.addCell(new Label(1, 0, "Domain"));
            sheet.addCell(new Label(2, 0, "Database"));
            sheet.addCell(new Label(0, 1, id));
            sheet.addCell(new Label(1, 1, domain));
            sheet.addCell(new Label(2, 1, db));
            int j = 2;
            for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
                for (String value : entry.getValue()) {
                    sheet.addCell(new Label(0, j, value));
                    sheet.addCell(new Label(1, j, entry.getKey()));
                    sheet.addCell(new Label(2, j, "Unknown"));
                    j++;
                }
            }
            workbook.write();
            workbook.close();
            zip(session_id, FileFormat.XLS, file);
            FILE = Config.getWebTmpPath() + session_id + FileFormat.XLS.getFileType() + FileFormat.ZIP.getFileType();
        } catch (Exception e) {
            Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
        }
    }

    /**
	 * @return FILE
	 */
    public String getWebTmpFilePath() {
        return FILE;
    }

    /**
	 * @param session_id
	 * @param file_format
	 * @param file
	 */
    private void zip(String session_id, FileFormat file_format, File file) {
        ZipOutputStream zos = null;
        try {
            File zip = new File(Config.getTmpPath() + session_id + file_format.getFileType() + FileFormat.ZIP.getFileType());
            if (zip.exists()) {
                zip.delete();
            }
            zip.createNewFile();
            zos = new ZipOutputStream(new FileOutputStream(zip));
            zos.putNextEntry(new ZipEntry(session_id + file_format.getFileType()));
            zos.write(FileUtils.readFileToByteArray(file));
            zos.closeEntry();
            zos.flush();
        } catch (Exception e) {
            Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
                }
            }
            file.delete();
        }
    }

    /**
	 * @param path
	 * @param entry
	 */
    private void create(String path, String entry) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(new File(path)));
            bw.write(entry);
            bw.flush();
        } catch (Exception e) {
            Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (Exception e) {
                    Log.writeErrorLog(WriteFile.class, e.getMessage(), e);
                }
            }
        }
    }
}
