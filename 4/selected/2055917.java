package com.patientis.business.billing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import com.patientis.business.controllers.DefaultCustomController;
import com.patientis.business.controllers.IControllerMethod;
import com.patientis.business.interfaces.IProcessorDefinition;
import com.patientis.client.action.BaseAction;
import com.patientis.data.common.BaseData;
import com.patientis.data.common.ISParameter;
import com.patientis.ejb.common.IChainStore;
import com.patientis.framework.api.services.ClinicalServer;
import com.patientis.framework.api.services.ReferenceServer;
import com.patientis.framework.api.services.SystemServer;
import com.patientis.framework.controls.ISControlPanel;
import com.patientis.framework.locale.SystemUtil;
import com.patientis.framework.scripting.IReceiveMessage;
import com.patientis.framework.scripting.ISEvent;
import com.patientis.framework.scripting.ServiceUtility;
import com.patientis.framework.utility.FileSystemUtil;
import com.patientis.framework.utility.XsltUtil;
import com.patientis.model.clinical.FormModel;
import com.patientis.model.clinical.FormTypeModel;
import com.patientis.model.common.Converter;
import com.patientis.model.common.DisplayModel;
import com.patientis.model.common.IBaseModel;
import com.patientis.model.common.ModelReference;
import com.patientis.model.common.ServiceCall;
import com.patientis.model.interfaces.ITransactionModel;
import com.patientis.model.interfaces.InterfaceMessageModel;
import com.patientis.model.interfaces.InterfaceTransactionModel;
import com.patientis.model.patient.PatientModel;
import com.patientis.model.reference.FormGroupReference;
import com.patientis.model.reference.InterfaceTransactionStateReference;
import com.patientis.model.reference.InterfaceTransactionTypeReference;
import com.patientis.model.reference.RecordItemReference;
import com.patientis.model.reference.ReportReference;

/**
 * @author gcaulton
 *
 */
public class DefaultClaimFormController extends DefaultCustomController {

    private FormModel form = null;

    private ISControlPanel controlPanel = null;

    @Override
    public void clientInitializeMonitor(IBaseModel model, ISControlPanel formDetailsControlPanel) throws Exception {
        form = (FormModel) model;
        controlPanel = formDetailsControlPanel;
        formDetailsControlPanel.getMediator().register(getReceiver(), this);
        setMainMediator(formDetailsControlPanel.getMediator());
    }

    /**
	 * 
	 * @return
	 */
    private IReceiveMessage getReceiver() {
        return new IReceiveMessage() {

            @Override
            public boolean receive(ISEvent event, Object value) throws Exception {
                switch(event) {
                    case EXECUTEACTION:
                        BaseAction action = (BaseAction) value;
                        switch(action.getActionReference()) {
                            case OPENFINDPATIENTCONTROLLER:
                                PatientModel patient = ServiceUtility.findVisit(action.getParentFrameOrDialog(getFrameOrDialog()));
                                if (patient.hasVisit()) {
                                    updatePatientForm(patient);
                                }
                                return true;
                        }
                        return false;
                }
                return false;
            }
        };
    }

    /**
	 * 
	 * @param patient
	 * @throws Exception
	 */
    private void updatePatientForm(PatientModel patient) throws Exception {
        form.givePatientQueryModel().copyAllFrom(patient);
        form.givePatientQueryModel().firePropertyChange(ModelReference.BASE, null, "update");
        form.givePatientQueryModel().firePropertyChange(ModelReference.PATIENTIDENTIFIERS_IDVALUE, null, "update");
        form.setPatientId(patient.getId());
        form.setVisitId(patient.getVisitId());
        String sql = "select fr.child_form_id, f.title " + " from form_records fr, forms f " + " where fr.visit_id = :visitId " + " and fr.record_item_ref_id = " + RecordItemReference.INSURANCECOMPANY.getRefId() + " and fr.child_form_id = f.form_id";
        List list = ReferenceServer.sqlQuery(sql, ISParameter.createList(new ISParameter("visitId", patient.getVisitId())));
        List<DisplayModel> displays = new ArrayList<DisplayModel>();
        for (Object o : list) {
            Object[] results = (Object[]) o;
            long formId = Converter.convertLong(results[0]);
            String title = Converter.convertDisplayString(results[1]);
            displays.add(new DisplayModel(formId, title, 0L));
        }
        ServiceUtility.rebuildList(controlPanel, "Primary Insurance", displays);
        ServiceUtility.rebuildList(controlPanel, "Secondary Insurance", displays);
        ServiceUtility.rebuildList(controlPanel, "Tertiary Insurance", displays);
    }

    /**
	 * @see com.patientis.business.controllers.DefaultCustomController#serverProcessPreSave(com.patientis.model.clinical.FormModel, com.patientis.model.clinical.FormTypeModel, com.patientis.ejb.common.IChainStore, com.patientis.model.common.ServiceCall)
	 */
    @Override
    public void serverProcessPostSave(final FormModel form, final FormTypeModel formType, final IChainStore chain, final ServiceCall call) throws Exception {
        IControllerMethod method = new IControllerMethod() {

            public Object execute() throws Exception {
                InterfaceTransactionModel transaction = new InterfaceTransactionModel();
                transaction.setTransactionTypeRef(new DisplayModel(InterfaceTransactionTypeReference.DEFAULTCLAIMCHARGES.getRefId()));
                transaction.setForeignKeyId(form.getId());
                transaction.setIdentifier1Id(form.getPatientId());
                BaseData.getInterface().store(transaction, chain, call);
                return null;
            }
        };
        call(method, call);
    }

    /**
	 * @see com.patientis.business.common.ICustomController#serverProcessInterfaceTransaction(com.patientis.model.interfaces.InterfaceTransactionModel, com.patientis.ejb.common.IChainStore, com.patientis.model.common.ServiceCall)
	 */
    @Override
    public boolean serverProcessInterfaceTransaction(final IProcessorDefinition processor, final ITransactionModel transaction, final IChainStore chain, final ServiceCall call) throws Exception {
        IControllerMethod method = new IControllerMethod() {

            public Object execute() throws Exception {
                FormModel claimsForm = BaseData.getClinical().getForm(((InterfaceTransactionModel) transaction).getForeignKeyId(), chain, call);
                List<Long> formTypeRefIds = BaseData.getClinical().getFormTypeRefIdsByFormGroupRefId(FormGroupReference.ACTIVELIST.getRefId(), call);
                List<FormModel> forms = new ArrayList<FormModel>();
                for (Long formTypeRefId : formTypeRefIds) {
                    FormModel form = BaseData.getClinical().getActiveListForm(claimsForm.getPatientId(), claimsForm.getVisitId(), formTypeRefId, call);
                    forms.add(form);
                }
                StringBuffer sb = new StringBuffer(4096 * 64);
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><report>");
                for (FormModel form : forms) {
                    String xml = BaseData.getReport().getReportXml(ReportReference.SYSTEMDEFAULTFORMPRINT.getRefId(), form, call);
                    xml = xml.replace("<report>", "").replace("</report>", "").replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
                    sb.append(xml);
                }
                sb.append("</report>");
                long settingsFormId = processor.getProcessSettingsFormId();
                if (settingsFormId == 0L) {
                    throw new Exception("No settings form on processor");
                }
                FormModel settingsForm = ClinicalServer.getFormFromCache(settingsFormId);
                long dataToClaimsXslFileId = settingsForm.getIntValueForRecordItem(4959753L);
                if (dataToClaimsXslFileId == 0L) {
                    throw new Exception("No data to claims xsl file defined");
                }
                File tempXMLFile = SystemUtil.getTemporaryFile("xml");
                File tempDataFile = SystemUtil.getTemporaryFile("xml");
                File x12File = SystemUtil.getTemporaryFile("xml");
                File dataToClaimsXslFile = SystemUtil.getTemporaryFile("xsl");
                FileSystemUtil.createBinaryFile(dataToClaimsXslFile, SystemServer.getFileContents(dataToClaimsXslFileId).getBytes());
                FileSystemUtil.createFile(tempXMLFile.getAbsolutePath(), sb.toString());
                XsltUtil.createTextFile(tempXMLFile, dataToClaimsXslFile, tempDataFile);
                long xslX12FileId = settingsForm.getIntValueForRecordItem(4959754L);
                if (dataToClaimsXslFileId == 0L) {
                    throw new Exception("No data to claims xsl file defined");
                }
                File xslX12File = SystemUtil.getTemporaryFile("xsl");
                FileSystemUtil.createBinaryFile(xslX12File, SystemServer.getFileContents(xslX12FileId).getBytes());
                XsltUtil.createTextFile(tempDataFile, xslX12File, x12File);
                FileUtils.copyFile(tempXMLFile, new File("C:\\temp\\data.xml"));
                FileUtils.copyFile(tempDataFile, new File("C:\\temp\\claim_data.xml"));
                FileUtils.copyFile(x12File, new File("C:\\temp\\x12.txt"));
                claimsForm.setFormStateRef(new DisplayModel(50003630L));
                BaseData.getClinical().store(claimsForm, chain, call);
                return true;
            }
        };
        return (Boolean) call(method, call);
    }
}
