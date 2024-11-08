package com.patientis.business.output;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.w3c.dom.Document;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.query.JRXPathQueryExecuterFactory;
import net.sf.jasperreports.engine.util.JRXmlUtils;
import com.patientis.business.controllers.DefaultCustomController;
import com.patientis.business.controllers.IControllerMethod;
import com.patientis.business.reference.AddressUtility;
import com.patientis.data.common.BaseData;
import com.patientis.data.hibernate.HibernateUtil;
import com.patientis.ejb.common.ChainStore;
import com.patientis.ejb.common.IBeanMethod;
import com.patientis.ejb.common.IChainStore;
import com.patientis.ejb.reports.ReportNotFoundException;
import com.patientis.ejb.reports.ReportReturnNoOutputException;
import com.patientis.framework.api.services.ClinicalServer;
import com.patientis.framework.api.services.PatientServer;
import com.patientis.framework.api.services.ReferenceServer;
import com.patientis.framework.api.services.ReportServer;
import com.patientis.framework.api.services.SecurityServer;
import com.patientis.framework.api.standard.StandardFormTypeReference;
import com.patientis.framework.api.standard.StandardRecordItemReference;
import com.patientis.framework.itext.PDFUtility;
import com.patientis.framework.locale.ImageUtil;
import com.patientis.framework.locale.SystemUtil;
import com.patientis.framework.logging.Log;
import com.patientis.framework.scripting.References;
import com.patientis.framework.utility.FileSystemUtil;
import com.patientis.framework.utility.XsltUtil;
import com.patientis.model.clinical.FormModel;
import com.patientis.model.clinical.FormRecordModel;
import com.patientis.model.clinical.FormTypeModel;
import com.patientis.model.common.ByteWrapper;
import com.patientis.model.common.Converter;
import com.patientis.model.common.DateTimeModel;
import com.patientis.model.common.IdentifierModel;
import com.patientis.model.common.LocationModel;
import com.patientis.model.common.ModelReference;
import com.patientis.model.common.ServiceCall;
import com.patientis.model.patient.PatientModel;
import com.patientis.model.reference.ControlTypeReference;
import com.patientis.model.reference.IdentifierSourceReference;
import com.patientis.model.reference.ValueDataTypeReference;
import com.patientis.model.reports.ReportModel;
import com.patientis.model.reports.ReportXml;
import com.patientis.model.reports.ReportXmlLine;
import com.patientis.model.security.ApplicationControlModel;
import com.patientis.model.security.ApplicationViewModel;

/**
 * @author gcaulton
 *
 */
public class DefaultJasperSQLOutputController extends DefaultCustomController {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
    }

    /**
	 * 
	 * @param reportRefId
	 * @param settingsForm
	 * @param call
	 * @return
	 * @throws Exception
	 */
    public ByteWrapper getPdfReportForSettings(final ReportModel report, final FormModel settingsForm, final ServiceCall call) throws Exception {
        IControllerMethod method = new IControllerMethod() {

            public Object execute() throws Exception {
                JasperPrint print = getSQLReportOutputByReport(report, settingsForm, call);
                File file = SystemUtil.getTemporaryFile("pdf");
                JasperExportManager.exportReportToPdfFile(print, file.getAbsolutePath());
                System.out.println("created " + file.getAbsolutePath() + " " + file.length() + " bytes");
                ByteWrapper bytes = new ByteWrapper(FileSystemUtil.getBinaryContents(file));
                return bytes;
            }
        };
        return (ByteWrapper) call(method, call);
    }

    /**
	 * 
	 * @param report
	 * @param formType
	 * @param form
	 * @param call
	 * @return
	 * @throws Exception
	 */
    public ByteWrapper getPdfReport(final ReportModel report, final FormTypeModel formType, final FormModel form, final ServiceCall call) throws Exception {
        throw new Exception("not supported");
    }

    /**
	 * @see com.patientis.business.output.DefaultFormOutputController#getHtmlReportForSettings(com.patientis.model.reports.ReportModel, com.patientis.model.clinical.FormModel, com.patientis.model.common.ServiceCall)
	 */
    @Override
    public String getHtmlReportForSettings(final ReportModel report, final FormModel settingsForm, final ServiceCall call) throws Exception {
        IControllerMethod method = new IControllerMethod() {

            public Object execute() throws Exception {
                JasperPrint print = getSQLReportOutputByReport(report, settingsForm, call);
                File file = SystemUtil.getTemporaryFile("html");
                JasperExportManager.exportReportToHtmlFile(print, file.getAbsolutePath());
                String html = FileSystemUtil.getTextContents(file);
                return html;
            }
        };
        return (String) call(method, call);
    }

    /**
	 * 
	 * @param reportRefId
	 * @param settingsForm
	 * @param call
	 * @return
	 * @throws Exception
	 */
    public String getHtmlReport(final ReportModel report, final FormModel form, final ServiceCall call) throws Exception {
        throw new Exception("not supported");
    }

    /**
	 * 
	 * @param report
	 * @param xmlFile
	 * @param call
	 * @return
	 * @throws Exception
	 */
    public JasperPrint getSQLReportOutputByReport(final ReportModel report, final FormModel settingsForm, final ServiceCall call) throws Exception {
        IControllerMethod method = new IControllerMethod() {

            public Object execute() throws Exception {
                Session session = HibernateUtil.getNewSession();
                session.setFlushMode(org.hibernate.FlushMode.NEVER);
                try {
                    HashMap params = new HashMap();
                    for (FormRecordModel record : settingsForm.getSortedRecords()) {
                        if (Converter.isNotEmpty(record.getTermText()) && record.getValue() != null) {
                            String paramName = record.getTermText().trim();
                            if (record.getValue() instanceof DateTimeModel) {
                                DateTimeModel dateTime = record.getValueDate();
                                java.sql.Timestamp timestamp = new java.sql.Timestamp(dateTime.getTime().getTime());
                                params.put(paramName, timestamp);
                            } else {
                                params.put(paramName, record.getValue());
                            }
                        }
                    }
                    params.put("user_ref_id", call.getUserRefId());
                    params.put("userRefId", call.getUserRefId());
                    params.put("StandardUserName", ReferenceServer.getDisplayValue(call.getUserRefId()));
                    params.put("StandardUserNameShort", ReferenceServer.getDisplayModel(call.getUserRefId()).getShortDisplay());
                    params.put("StandardFacilityName", ReferenceServer.getDisplayValue(call.getFacilityLocationRefId()));
                    params.put("StandardFacilityNameShort", ReferenceServer.getDisplayModel(call.getFacilityLocationRefId()).getShortDisplay());
                    LocationModel location = ReferenceServer.getLocationByRef(call.getFacilityLocationRefId());
                    FormModel locationForm = ClinicalServer.getFormFromCache(location.getLocationFormId());
                    ApplicationViewModel view = SecurityServer.getApplicationViewByFormTypeRefId(locationForm.getFormTypeRefId());
                    for (ApplicationControlModel acm : view.getApplicationControls()) {
                        Object value = locationForm.getActiveFormRecordSequenceTerm(acm.getRecordItemRefId(), 0, acm.getTermId()).getValue();
                        params.put("StandardFacility" + acm.getHtmlLabel(), Converter.convertDisplayString(value));
                    }
                    if (settingsForm.getVisitId() > 0L) {
                        params.put("visit_id", settingsForm.getVisitId());
                    }
                    if (settingsForm.getPatientId() > 0L) {
                        params.put("patient_id", settingsForm.getPatientId());
                        PatientModel patient = null;
                        if (settingsForm.getVisitId() > 0L) {
                            patient = PatientServer.getPatientForVisitId(settingsForm.getVisitId());
                        } else {
                            patient = PatientServer.getPatient(settingsForm.getPatientId());
                        }
                        params.put("patientName", patient.getPatientName());
                        params.put("firstName", patient.getFirstName());
                        params.put("lastName", patient.getLastName());
                        params.put("mrn", patient.getMedicalRecordNumber());
                        params.put("patientSSN", IdentifierModel.getIIdentifierList(patient.getIdentifiers(), IdentifierSourceReference.SOCIALSECURITYNUMBER));
                        params.put("dob", patient.getBirthDt().toString(DateTimeModel.getDefaultShortDateFormat()));
                        params.put("patientDOB", patient.getBirthDt().toString(DateTimeModel.getDefaultShortDateFormat()));
                        params.put("patientHomeAddressLine1", ClinicalServer.getRecordValueForFormRecordItem(settingsForm.getPatientId(), 0, StandardRecordItemReference.PatientHomeStreetAddressLine1, StandardFormTypeReference.SystemPatientHomeAddressSection, call));
                        params.put("patientHomeAddressLine2", ClinicalServer.getRecordValueForFormRecordItem(settingsForm.getPatientId(), 0, StandardRecordItemReference.PatientHomeStreetAddressLine2, StandardFormTypeReference.SystemPatientHomeAddressSection, call));
                        params.put("patientHomeCity", ClinicalServer.getRecordValueForFormRecordItem(settingsForm.getPatientId(), 0, StandardRecordItemReference.PatientHomeCity, StandardFormTypeReference.SystemPatientHomeAddressSection, call));
                        params.put("patientHomeState", ClinicalServer.getRecordValueForFormRecordItem(settingsForm.getPatientId(), 0, StandardRecordItemReference.PatientHomeStateorProvince, StandardFormTypeReference.SystemPatientHomeAddressSection, call));
                        params.put("patientHomeStateAbbreviation", AddressUtility.getStateAbbreviation(Converter.convertDisplayString(ClinicalServer.getRecordValueForFormRecordItem(settingsForm.getPatientId(), 0, StandardRecordItemReference.PatientHomeStateorProvince, StandardFormTypeReference.SystemPatientHomeAddressSection, call))));
                        params.put("patientHomeZip", ClinicalServer.getRecordValueForFormRecordItem(settingsForm.getPatientId(), 0, StandardRecordItemReference.PatientHomeZiporPostalCode, StandardFormTypeReference.SystemPatientHomeAddressSection, call));
                    }
                    String path = generateFilePathForReport(report, call);
                    JasperPrint jasperPrint = JasperFillManager.fillReport(path, params, session.connection());
                    return jasperPrint;
                } catch (Exception ex) {
                    Log.exception(ex);
                    throw ex;
                } finally {
                    try {
                        session.close();
                    } catch (Exception ex) {
                        Log.exception(ex);
                    }
                }
            }
        };
        return (JasperPrint) call(method, call);
    }

    /**
	 * 
	 * @param params
	 * @param report
	 * @param xml
	 * @param call
	 * @return
	 * @throws Exception
	 */
    private String generateFilePathForReport(final ReportModel report, final ServiceCall call) throws Exception {
        long jasperFileId = report.getJasperFileId();
        String filePath = BaseData.getSystem().getFile(jasperFileId, call).getFilePath();
        if (!new File(filePath).exists()) {
            ByteWrapper wrapper = BaseData.getSystem().getFileContents(jasperFileId, call);
            if (wrapper != null) {
                File tempFile = SystemUtil.getTemporaryFile();
                FileSystemUtil.createBinaryFile(tempFile, wrapper.getBytes());
                Log.warn("Copying report file to temp");
                return tempFile.getAbsolutePath();
            } else {
                throw new Exception("No jasper file found for file id " + jasperFileId);
            }
        } else {
            return filePath;
        }
    }

    /**
	 * 
	 */
    public byte[] getImageReportForSettings(final ReportModel report, final FormModel settingsForm, final ServiceCall call) {
        try {
            System.out.println("Report for " + report.getReportRef().getDisplay() + " patient " + settingsForm.getPatientId() + " " + settingsForm.getVisitId());
            ByteWrapper byteWrapper = ReportServer.getPdfReportForSettings(report.getReportRef().getId(), settingsForm, call);
            if (byteWrapper != null) {
                File pdfFile = SystemUtil.getTemporaryFile("pdf");
                FileSystemUtil.createBinaryFile(pdfFile, byteWrapper.getBytes());
                FileUtils.copyFile(pdfFile, new File("C:\\temp\\test.pdf"));
                BufferedImage image = PDFUtility.createImageFromPDF(pdfFile, 1);
                if (image != null) {
                    File tempFile = SystemUtil.getTemporaryFile("png");
                    ImageUtil.savePNG(image, tempFile);
                    byte[] bytes = FileSystemUtil.getBinaryContents(tempFile);
                    FileSystemUtil.createBinaryFile(new File("C:\\temp\\test.png"), bytes);
                    return bytes;
                } else {
                    System.err.println("null image");
                    return null;
                }
            } else {
                System.err.println("null byte wrapper");
                return null;
            }
        } catch (Exception ex) {
            Log.exception(ex);
            return null;
        }
    }
}
