package com.alianzamedica.servlets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.objectsearch.sqlsearch.ObjectSearch;
import org.objectsearch.web.tools.Converter;
import org.objectsearch.web.tools.FormatDateTool;
import org.w3c.dom.Document;
import com.alianzamedica.businessobject.Doctor;
import com.alianzamedica.businessobject.Drug;
import com.alianzamedica.businessobject.Patient;
import com.alianzamedica.businessobject.Prescription;
import com.alianzamedica.businessobject.PrescriptionDetail;
import com.alianzamedica.connection.ConnectionImpl;
import com.alianzamedica.tools.Enviroment;
import com.alianzamedica.tools.OXMLTest;
import com.alianzamedica.tools.ZipUtility;

/**
 * @author Carlos
 * 
 */
public class DocumentRecetaServlet extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = -8038641916212403906L;

    @SuppressWarnings("unchecked")
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml");
        response.setHeader("Content-disposition", "attachment;filename=receta.docx");
        try {
            Integer id = Converter.string2Integer(request.getParameter("id"));
            String documentResource = this.getServletContext().getRealPath("");
            String resource = documentResource + "/WEB-INF/resource/receta.docx";
            System.out.println("resource: " + resource);
            if (id == null) {
                out.close();
                return;
            }
            ConnectionImpl impl = new ConnectionImpl();
            Enviroment env = Enviroment.getInstance();
            Document doc = env.getDocument();
            ObjectSearch search = new ObjectSearch(doc, impl);
            Prescription tPrescription = new Prescription();
            tPrescription.setId(id);
            Iterator<Prescription> iterator = search.searchObjects(tPrescription).iterator();
            Prescription prescription = null;
            while (iterator.hasNext()) {
                prescription = (Prescription) iterator.next();
            }
            if (prescription != null) {
                OXMLTest oxml = new OXMLTest();
                Doctor doctor = prescription.getDoctor();
                if (doctor == null) {
                    out.close();
                    return;
                }
                String doctorName = doctor.getFullName();
                String speciality = doctor.getSpeciality();
                String cedulaProfesional = doctor.getCell();
                String universidad = doctor.getUniversity();
                Patient patient = prescription.getPatient();
                String nombreDelPaciente = patient.getFullName();
                String date = "";
                try {
                    date = FormatDateTool.formatDate(prescription.getExpedition());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                String edad = "";
                Integer age = patient.getAge();
                edad = (age != null) ? age.toString() : "";
                String direccion = doctor.getLocation();
                String telefono = doctor.getPhoneNo();
                String[] recetas = getRecetas(prescription);
                org.dom4j.Document document = oxml.createElement(doctorName, speciality, cedulaProfesional, universidad, nombreDelPaciente, date, edad, recetas, direccion, telefono);
                ZipUtility zipUtil = new ZipUtility();
                File resourceFile = new File(resource);
                zipUtil.processZipEntries(resourceFile);
                byte[] data = oxml.serializeDocument(document);
                zipUtil.setDocumentEntry("word/document.xml", data);
                File receta2 = new File(documentResource + "/WEB-INF/resource/receta2.docx");
                zipUtil.processOutput(receta2);
                byte[] serialData = readFile(receta2);
                out.write(serialData);
                receta2.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    private byte[] readFile(File receta2) throws IOException {
        byte[] data = null;
        InputStream in = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            in = new FileInputStream(receta2);
            int read = 0;
            while ((read = in.read()) != -1) {
                baos.write((byte) read);
            }
            data = baos.toByteArray();
        } finally {
            try {
                in.close();
            } catch (Exception e2) {
            }
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private String[] getRecetas(Prescription prescription) throws Exception {
        String[] recetas = null;
        ArrayList details = prescription.getDetails();
        int size = details.size();
        recetas = new String[size];
        for (int i = 0; i < size; i++) {
            PrescriptionDetail detail = (PrescriptionDetail) details.get(i);
            Drug drug = detail.getDrug();
            String genericName = "";
            if (drug.getGenericName() != null) {
                genericName = "(" + drug.getGenericName() + ")";
            }
            String receta = "" + drug.getName() + genericName + "," + detail.getDose() + " , cantidad: " + detail.getQuantity() + " , " + detail.getAdministrationForm() + " , " + detail.getDose() + " por un periodo de " + detail.getPeriod();
            recetas[i] = receta;
        }
        return recetas;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.processRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.processRequest(req, resp);
    }
}
