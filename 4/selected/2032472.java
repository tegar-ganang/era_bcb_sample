package ispyb.client.shipping;

import ispyb.client.BreadCrumbsForm;
import ispyb.client.results.image.FileUtil;
import ispyb.client.sample.AbstractSampleAction;
import ispyb.client.util.ClientLogger;
import ispyb.client.util.DBTools;
import ispyb.client.util.ShippingInformation;
import ispyb.common.util.Constants;
import ispyb.server.data.interfaces.BlsampleFacadeLocal;
import ispyb.server.data.interfaces.BlsampleFacadeUtil;
import ispyb.server.data.interfaces.BlsampleLightValue;
import ispyb.server.data.interfaces.ContainerFacadeLocal;
import ispyb.server.data.interfaces.ContainerFacadeUtil;
import ispyb.server.data.interfaces.ContainerLightValue;
import ispyb.server.data.interfaces.CrystalFacadeLocal;
import ispyb.server.data.interfaces.CrystalFacadeUtil;
import ispyb.server.data.interfaces.CrystalLightValue;
import ispyb.server.data.interfaces.CrystalValue;
import ispyb.server.data.interfaces.DatamatrixInSampleChangerFacadeLocal;
import ispyb.server.data.interfaces.DatamatrixInSampleChangerFacadeUtil;
import ispyb.server.data.interfaces.DatamatrixInSampleChangerValue;
import ispyb.server.data.interfaces.DewarFacadeLocal;
import ispyb.server.data.interfaces.DewarFacadeUtil;
import ispyb.server.data.interfaces.DewarLightValue;
import ispyb.server.data.interfaces.ProposalFacadeLocal;
import ispyb.server.data.interfaces.ProposalFacadeUtil;
import ispyb.server.data.interfaces.ProposalLightValue;
import ispyb.server.data.interfaces.ProteinFacadeLocal;
import ispyb.server.data.interfaces.ProteinFacadeUtil;
import ispyb.server.data.interfaces.ProteinValue;
import ispyb.server.data.interfaces.ShippingFullFacadeLocal;
import ispyb.server.data.interfaces.ShippingFullFacadeUtil;
import ispyb.server.data.interfaces.ShippingFullValue;
import ispyb.server.data.interfaces.ShippingValue;
import ispyb.server.util.ServerLogger;
import ispyb.server.webservice.DBAcess.DBAccess_EJB;
import ispyb.server.webservice.WebService.DiffractionPlan;
import ispyb.server.webservice.WebService.Shipping;
import ispyb.server.webservice.WebService.XtalDetails;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Category;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.upload.FormFile;
import uk.ac.ehtpx.model.CrystalDetailsElement;
import uk.ac.ehtpx.model.CrystalShipping;
import uk.ac.ehtpx.model.DeliveryAgent;
import uk.ac.ehtpx.model.DiffractionPlanElement;
import uk.ac.ehtpx.model.Laboratory;
import uk.ac.ehtpx.model.Person;
import uk.ac.ehtpx.ws.util.CrystalDetailsBuilder;
import uk.ac.ehtpx.ws.util.CrystalShippingBuilder;
import uk.ac.ehtpx.ws.util.DiffractionPlanBuilder;
import uk.ac.ehtpx.ws.util.XlsUploadException;
import uk.ac.ehtpx.xlsparser.ehtpx.eHTPXXLSParser;

/**
 * @struts.action name="uploadForm" path="/user/submitPocketSampleInformationAction"
 *                type="ispyb.client.shipping.SubmitPocketSampleInformationAction" validate="false"
 *                parameter="reqCode" scope="request"
 * 
 * @struts.action-forward name="submitPocketSampleInformationPage"
 *                path="user.shipping.submitPocketSampleInformation.page"
 *                        
 * @struts.action-forward name="error" path="site.default.error.page"
 * 
 * @struts.action-forward name="shippingViewPage"
 *                path="/menuSelected.do?leftMenuId=7&amp;topMenuId=5&amp;targetUrl=%2Fuser%2FviewShippingAction.do%3FreqCode%3Ddisplay"
 *  
 * @struts.action-forward name="uploadFilePage"
 *                path="user.shipping.upload.page"
 */
public class SubmitPocketSampleInformationAction extends AbstractSampleAction {

    static Category Log = Category.getInstance(SubmitPocketSampleInformationAction.class.getName());

    private boolean populateDMCodes = false;

    private String mFileType = Constants.TEMPLATE_FILE_TYPE_EXPORT_SHIPPING;

    /**
	 * main
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            SubmitPocketSampleInformationAction s = new SubmitPocketSampleInformationAction();
            s.uploadFile(null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * execute
	 * @param args
	 */
    public ActionForward execute(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse in_response) {
        String reqCode = (String) request.getParameter("reqCode");
        if (reqCode.equals("display")) return this.display(mapping, actForm, request, in_response);
        if (reqCode.equals("displayAfterDewarTracking")) return this.displayAfterDewarTracking(mapping, actForm, request, in_response);
        if (reqCode.equals("uploadFile")) return this.uploadFile(mapping, actForm, request, in_response);
        if (reqCode.equals("downloadFile")) return this.DownloadFile(mapping, actForm, request, in_response);
        if (reqCode.equals("exportShipping")) return this.exportShipping(mapping, actForm, request, in_response);
        return this.display(mapping, actForm, request, in_response);
    }

    /**
	 * displayAfterDewarTracking
	 * @param mapping
	 * @param actForm
	 * @param request
	 * @param in_reponse
	 * @return
	 */
    public ActionForward displayAfterDewarTracking(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse in_reponse) {
        UploadForm form = (UploadForm) actForm;
        Integer shippingId = Integer.decode(request.getParameter(Constants.SHIPPING_ID));
        if (!Constants.SITE_IS_DLS()) {
            String populatedTemplatePath = PopulateTemplate(request, false, false, false, null, null, false, 0, true, shippingId);
            form.setPopulatedForShipmentTemplageURL(populatedTemplatePath);
        }
        form.setCurrentShippingId(shippingId.intValue());
        try {
            ShippingValue selectedShipping = DBTools.getSelectedShipping(shippingId);
            BreadCrumbsForm.getIt(request).setSelectedShipping(selectedShipping);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.display(mapping, form, request, in_reponse);
    }

    /**
	 * display
	 * @param mapping
	 * @param actForm
	 * @param request
	 * @param in_reponse
	 * @return
	 */
    public ActionForward display(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse in_reponse) {
        try {
            String shippingId = request.getParameter(Constants.SHIPPING_ID);
            if (shippingId != null && !shippingId.equals("")) {
                ShippingValue selectedShipping = DBTools.getSelectedShipping(Integer.decode(shippingId));
                BreadCrumbsForm.getIt(request).setSelectedShipping(selectedShipping);
            } else BreadCrumbsForm.getIt(request).setSelectedShipping(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (BreadCrumbsForm.getIt(request).getSelectedShipping() == null) BreadCrumbsForm.setIt(request, BreadCrumbsForm.getItClean(request)); else BreadCrumbsForm.getIt(request).setSelectedDewar(null);
        UploadForm form = (UploadForm) actForm;
        String selectedBeamlineName = form.getSelectedBeamline();
        String populatedTemplatePath = PopulateTemplate(request, false, false, false, null, null, false, 0, false, 0);
        if (selectedBeamlineName != null) {
            String populatedTemplateAdvancedPath = PopulateTemplate(request, false, false, true, selectedBeamlineName, null, false, 0, false, 0);
        }
        form.setPopulatedTemplateURL(populatedTemplatePath);
        try {
            String proposalCode = (String) request.getSession().getAttribute(Constants.PROPOSAL_CODE);
            String proposalNumber = String.valueOf(request.getSession().getAttribute(Constants.PROPOSAL_NUMBER));
            ProposalLightValue proposal = DBTools.getProposal(proposalCode, new Integer(proposalNumber));
            List listBeamlines = new ArrayList();
            DatamatrixInSampleChangerFacadeLocal dmInSC = DatamatrixInSampleChangerFacadeUtil.getLocalHome().create();
            listBeamlines = (ArrayList) dmInSC.findBLNamesByProposalId(proposal.getProposalId());
            form.setListBeamlines(listBeamlines);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapping.findForward("submitPocketSampleInformationPage");
    }

    /**
	 * PopulateTemplate
	 * @param request
	 * @param getTemplateFullPathOnly
	 * @param getTemplateFilenameOnly
	 * @param populateDMCodes
	 * @param selectedBeamlineName
	 * @param hashDMCodesForBeamline
	 * @param populateForExport
	 * @param nbContainersToExport
	 * @param populateForShipment
	 * @param shippingId
	 * @return
	 */
    public static String PopulateTemplate(HttpServletRequest request, boolean getTemplateFullPathOnly, boolean getTemplateFilenameOnly, boolean populateDMCodes, String selectedBeamlineName, List hashDMCodesForBeamline, boolean populateForExport, int nbContainersToExport, boolean populateForShipment, int shippingId) {
        String populatedTemplatePath = "";
        try {
            String xlsPath;
            String proposalCode;
            String proposalNumber;
            String populatedTemplateFileName;
            GregorianCalendar calendar = new GregorianCalendar();
            String today = ".xls";
            if (request != null) {
                xlsPath = Constants.TEMPLATE_POPULATED_RELATIVE_PATH;
                if (populateForShipment) xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FROM_SHIPMENT; else if (populateForExport) {
                    switch(nbContainersToExport) {
                        case 1:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME1;
                            break;
                        case 2:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME2;
                            break;
                        case 3:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME3;
                            break;
                        case 4:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME4;
                            break;
                        case 5:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME5;
                            break;
                        case 6:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME6;
                            break;
                        case 7:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME7;
                            break;
                        case 8:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME8;
                            break;
                        case 9:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME9;
                            break;
                        case 10:
                            xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME10;
                            break;
                    }
                }
                proposalCode = (String) request.getSession().getAttribute(Constants.PROPOSAL_CODE);
                proposalNumber = String.valueOf(request.getSession().getAttribute(Constants.PROPOSAL_NUMBER));
                if (populateForShipment) populatedTemplateFileName = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + proposalCode + proposalNumber + "_shipment_" + shippingId + today; else populatedTemplateFileName = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + proposalCode + proposalNumber + ((populateDMCodes) ? "_#" : "") + today;
                populatedTemplatePath = request.getContextPath() + populatedTemplateFileName;
                if (getTemplateFilenameOnly && populateForShipment) return proposalCode + proposalNumber + "_shipment_" + shippingId + today;
                if (getTemplateFilenameOnly && !populateForShipment) return proposalCode + proposalNumber + ((populateDMCodes) ? "_#" : "") + today;
                xlsPath = request.getRealPath(xlsPath);
                populatedTemplateFileName = request.getRealPath(populatedTemplateFileName);
            } else {
                xlsPath = "C:/" + Constants.TEMPLATE_POPULATED_RELATIVE_PATH;
                proposalCode = "ehtpx";
                proposalNumber = "1";
                populatedTemplateFileName = "C:/" + Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + proposalCode + proposalNumber + today;
            }
            if (getTemplateFullPathOnly) return populatedTemplateFileName;
            String beamlineName = selectedBeamlineName;
            String[][] dmCodesinSC = null;
            if (populateDMCodes) {
                dmCodesinSC = new String[Constants.SC_BASKET_CAPACITY + 1][Constants.BASKET_SAMPLE_CAPACITY + 1];
                DatamatrixInSampleChangerFacadeLocal dms = DatamatrixInSampleChangerFacadeUtil.getLocalHome().create();
                ProposalLightValue prop = DBTools.getProposal(proposalCode, new Integer(proposalNumber));
                if (prop != null) {
                    Integer proposalId = prop.getProposalId();
                    List lstDMCodes = (List) dms.findByProposalIdAndBeamlineName(proposalId, beamlineName);
                    for (int i = 0; i < lstDMCodes.size(); i++) {
                        DatamatrixInSampleChangerValue dmInSC = (DatamatrixInSampleChangerValue) lstDMCodes.get(i);
                        Integer basketLocation = dmInSC.getContainerLocationInSc();
                        Integer sampleLocation = dmInSC.getLocationInContainer();
                        String dmCode = dmInSC.getDatamatrixCode();
                        if (basketLocation <= Constants.SC_BASKET_CAPACITY && sampleLocation <= Constants.BASKET_SAMPLE_CAPACITY) {
                            dmCodesinSC[basketLocation][sampleLocation] = dmCode;
                        }
                    }
                }
            }
            File originalTemplate = new File(xlsPath);
            File populatedTemplate = new File(populatedTemplateFileName);
            FileUtils.copyFile(originalTemplate, populatedTemplate);
            eHTPXXLSParser parser = new eHTPXXLSParser();
            File xlsTemplate = new File(xlsPath);
            File xlsPopulatedTemplate = new File(populatedTemplateFileName);
            FileUtils.copyFile(xlsTemplate, xlsPopulatedTemplate);
            ProposalFacadeLocal _proposal = ProposalFacadeUtil.getLocalHome().create();
            List proposals = (List) _proposal.findByCodeAndNumber(proposalCode, new Integer(proposalNumber));
            ProposalLightValue proposalLight = (ProposalLightValue) proposals.get(0);
            ProteinFacadeLocal _protein = ProteinFacadeUtil.getLocalHome().create();
            List listProteins = (List) _protein.findByProposalId(proposalLight.getPrimaryKey());
            parser.populate(xlsPath, populatedTemplateFileName, listProteins, dmCodesinSC);
            if (populateForShipment) parser.populateExistingShipment(populatedTemplateFileName, populatedTemplateFileName, shippingId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return populatedTemplatePath;
    }

    /**
	 * exportShipping
	 * @param mapping
	 * @param actForm
	 * @param request
	 * @param in_reponse
	 * @return
	 */
    public ActionForward exportShipping(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages messages = new ActionMessages();
        String returnPage = "shippingViewPage";
        try {
            eHTPXXLSParser parser = new eHTPXXLSParser();
            BreadCrumbsForm.setIt(request, BreadCrumbsForm.getItClean(request));
            String shippingId = request.getParameter(Constants.SHIPPING_ID);
            ShippingInformation shippingInformation = DBTools.getShippingInformation(new Integer(shippingId));
            int nbSheetsInInfo = DBTools.GetNumberOfContainers(shippingInformation) - 1;
            String populatedTemplatePath = PopulateTemplate(request, false, false, false, null, null, true, nbSheetsInInfo, false, 0);
            populatedTemplatePath = PopulateTemplate(request, true, false, false, null, null, true, nbSheetsInInfo, false, 0);
            String shippingName = shippingInformation.getShipping().getShippingName();
            if (shippingName.indexOf(".xls") == -1) shippingName += ".xls";
            String fileName = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + shippingName;
            parser.export(fileName, populatedTemplatePath, shippingInformation);
            this.mFileType = Constants.TEMPLATE_FILE_TYPE_EXPORT_SHIPPING;
            this.DownloadFile(mapping, actForm, request, response);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
	 * uploadFile
	 * @param mapping
	 * @param actForm
	 * @param request
	 * @param in_response
	 * @return
	 */
    public ActionForward uploadFile(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse in_response) {
        ActionMessages errors = new ActionMessages();
        ActionMessages messages = new ActionMessages();
        String returnPage = "submitPocketSampleInformationPage";
        UploadForm form = (UploadForm) actForm;
        Integer shippingId = null;
        try {
            eHTPXXLSParser parser = new eHTPXXLSParser();
            String proposalCode;
            String proposalNumber;
            String proposalName;
            String uploadedFileName;
            String realXLSPath;
            if (request != null) {
                proposalCode = (String) request.getSession().getAttribute(Constants.PROPOSAL_CODE);
                proposalNumber = String.valueOf(request.getSession().getAttribute(Constants.PROPOSAL_NUMBER));
                proposalName = proposalCode + proposalNumber.toString();
                uploadedFileName = form.getRequestFile().getFileName();
                String fileName = proposalName + "_" + uploadedFileName;
                realXLSPath = request.getRealPath("\\tmp\\") + "\\" + fileName;
                FormFile f = form.getRequestFile();
                InputStream in = f.getInputStream();
                File outputFile = new File(realXLSPath);
                if (outputFile.exists()) outputFile.delete();
                FileOutputStream out = new FileOutputStream(outputFile);
                while (in.available() != 0) {
                    out.write(in.read());
                    out.flush();
                }
                out.flush();
                out.close();
            } else {
                proposalCode = "ehtpx";
                proposalNumber = "1";
                proposalName = proposalCode + proposalNumber.toString();
                uploadedFileName = "ispyb-template41.xls";
                realXLSPath = "D:\\" + uploadedFileName;
            }
            FileInputStream inFile = new FileInputStream(realXLSPath);
            parser.retrieveShippingId(realXLSPath);
            shippingId = parser.getShippingId();
            String requestShippingId = form.getShippingId();
            if (requestShippingId != null && !requestShippingId.equals("")) {
                shippingId = new Integer(requestShippingId);
            }
            ClientLogger.getInstance().debug("uploadFile for shippingId " + shippingId);
            if (shippingId != null) {
                Log.debug(" ---[uploadFile] Upload for Existing Shipment (DewarTRacking): Deleting Samples from Shipment :");
                double nbSamplesContainers = DBAccess_EJB.DeleteAllSamplesAndContainersForShipping(shippingId);
                if (nbSamplesContainers > 0) parser.getValidationWarnings().add(new XlsUploadException("Shipment contained Samples and/or Containers", "Previous Samples and/or Containers have been deleted and replaced by new ones.")); else parser.getValidationWarnings().add(new XlsUploadException("Shipment contained no Samples and no Containers", "Samples and Containers have been added."));
            }
            Hashtable<String, Hashtable<String, Integer>> listProteinAcronym_SampleName = new Hashtable<String, Hashtable<String, Integer>>();
            ProposalFacadeLocal proposal = ProposalFacadeUtil.getLocalHome().create();
            ProteinFacadeLocal protein = ProteinFacadeUtil.getLocalHome().create();
            CrystalFacadeLocal crystal = CrystalFacadeUtil.getLocalHome().create();
            ProposalLightValue targetProposal = (ProposalLightValue) (((ArrayList) proposal.findByCodeAndNumber(proposalCode, new Integer(proposalNumber))).get(0));
            ArrayList listProteins = (ArrayList) protein.findByProposalId(targetProposal.getProposalId());
            for (int p = 0; p < listProteins.size(); p++) {
                ProteinValue prot = (ProteinValue) listProteins.get(p);
                Hashtable<String, Integer> listSampleName = new Hashtable<String, Integer>();
                CrystalLightValue listCrystals[] = prot.getCrystals();
                for (int c = 0; c < listCrystals.length; c++) {
                    CrystalLightValue _xtal = (CrystalLightValue) listCrystals[c];
                    CrystalValue xtal = crystal.findByPrimaryKey(_xtal.getPrimaryKey());
                    BlsampleLightValue listSamples[] = xtal.getBlsamples();
                    for (int s = 0; s < listSamples.length; s++) {
                        BlsampleLightValue sample = listSamples[s];
                        listSampleName.put(sample.getName(), sample.getBlSampleId());
                    }
                }
                listProteinAcronym_SampleName.put(prot.getAcronym(), listSampleName);
            }
            parser.validate(inFile, listProteinAcronym_SampleName, targetProposal.getProposalId());
            List listErrors = parser.getValidationErrors();
            List listWarnings = parser.getValidationWarnings();
            if (listErrors.size() == 0) {
                parser.open(realXLSPath);
                if (parser.getCrystals().size() == 0) {
                    parser.getValidationErrors().add(new XlsUploadException("No crystals have been found", "Empty shipment"));
                }
            }
            Iterator errIt = listErrors.iterator();
            while (errIt.hasNext()) {
                XlsUploadException xlsEx = (XlsUploadException) errIt.next();
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.free", xlsEx.getMessage() + " ---> " + xlsEx.getSuggestedFix()));
            }
            try {
                saveErrors(request, errors);
            } catch (Exception e) {
            }
            Iterator warnIt = listWarnings.iterator();
            while (warnIt.hasNext()) {
                XlsUploadException xlsEx = (XlsUploadException) warnIt.next();
                messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.free", xlsEx.getMessage() + " ---> " + xlsEx.getSuggestedFix()));
            }
            try {
                saveMessages(request, messages);
            } catch (Exception e) {
            }
            if (listErrors.size() > 0) {
                resetCounts(shippingId);
                return mapping.findForward("submitPocketSampleInformationPage");
            }
            if (listWarnings.size() > 0) returnPage = "submitPocketSampleInformationPage";
            String crystalDetailsXML;
            XtalDetails xtalDetailsWebService = new XtalDetails();
            CrystalDetailsBuilder cDE = new CrystalDetailsBuilder();
            CrystalDetailsElement cd = cDE.createCrystalDetailsElement(proposalName, parser.getCrystals());
            cDE.validateJAXBObject(cd);
            crystalDetailsXML = cDE.marshallJaxBObjToString(cd);
            xtalDetailsWebService.submitCrystalDetails(crystalDetailsXML);
            String diffractionPlan;
            DiffractionPlan diffractionPlanWebService = new DiffractionPlan();
            DiffractionPlanBuilder dPB = new DiffractionPlanBuilder();
            Iterator it = parser.getDiffractionPlans().iterator();
            while (it.hasNext()) {
                DiffractionPlanElement dpe = (DiffractionPlanElement) it.next();
                dpe.setProjectUUID(proposalName);
                diffractionPlan = dPB.marshallJaxBObjToString(dpe);
                diffractionPlanWebService.submitDiffractionPlan(diffractionPlan);
            }
            String crystalShipping;
            Shipping shippingWebService = new Shipping();
            CrystalShippingBuilder cSB = new CrystalShippingBuilder();
            Person person = cSB.createPerson("XLS Upload", null, "ISPyB", null, null, "ISPyB", null, "ispyb@esrf.fr", "0000", "0000", null, null);
            Laboratory laboratory = cSB.createLaboratory("Generic Laboratory", "ISPyB Lab", "Sandwich", "Somewhere", "UK", "ISPyB", "ispyb.esrf.fr", person);
            DeliveryAgent deliveryAgent = parser.getDeliveryAgent();
            CrystalShipping cs = cSB.createCrystalShipping(proposalName, laboratory, deliveryAgent, parser.getDewars());
            String shippingName;
            shippingName = uploadedFileName.substring(0, ((uploadedFileName.toLowerCase().lastIndexOf(".xls")) > 0) ? uploadedFileName.toLowerCase().lastIndexOf(".xls") : 0);
            if (shippingName.equalsIgnoreCase("")) shippingName = uploadedFileName.substring(0, ((uploadedFileName.toLowerCase().lastIndexOf(".xlt")) > 0) ? uploadedFileName.toLowerCase().lastIndexOf(".xlt") : 0);
            cs.setName(shippingName);
            crystalShipping = cSB.marshallJaxBObjToString(cs);
            shippingWebService.submitCrystalShipping(crystalShipping, (ArrayList) parser.getDiffractionPlans(), shippingId);
            ServerLogger.Log4Stat("XLS_UPLOAD", proposalName, uploadedFileName);
        } catch (XlsUploadException e) {
            resetCounts(shippingId);
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.getMessage()));
            ClientLogger.getInstance().error(e.toString());
            saveErrors(request, errors);
            return mapping.findForward("error");
        } catch (Exception e) {
            resetCounts(shippingId);
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            ClientLogger.getInstance().error(e.toString());
            e.printStackTrace();
            saveErrors(request, errors);
            return mapping.findForward("error");
        }
        setCounts(shippingId);
        return mapping.findForward(returnPage);
    }

    /**
	 * Reset dewar and sample counts
	 * @param shippingId
	 */
    private void resetCounts(int shippingId) {
        try {
            ShippingFullFacadeLocal _shippings = ShippingFullFacadeUtil.getLocalHome().create();
            ShippingFullValue shipping = _shippings.findByPrimaryKey(shippingId);
            shipping.setSamplesNumber(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Set dewar and sample count
	 * @param shippingId
	 */
    private void setCounts(int shippingId) {
        try {
            int nbDewars = 0;
            int nbSamples = 0;
            ShippingFullFacadeLocal _shippings = ShippingFullFacadeUtil.getLocalHome().create();
            ContainerFacadeLocal _containers = ContainerFacadeUtil.getLocalHome().create();
            BlsampleFacadeLocal _samples = BlsampleFacadeUtil.getLocalHome().create();
            DewarFacadeLocal _dewars = DewarFacadeUtil.getLocalHome().create();
            Collection<DewarLightValue> dewarList = _dewars.findByShippingId(shippingId);
            for (DewarLightValue dewar : dewarList) {
                nbDewars++;
                Collection<ContainerLightValue> containerList = _containers.findByDewarId(dewar.getDewarId());
                for (ContainerLightValue container : containerList) {
                    Collection<BlsampleLightValue> BlsampleList = _samples.findByContainerId(container.getContainerId());
                    nbSamples += BlsampleList.size();
                }
            }
            ShippingFullValue shipping = _shippings.findByPrimaryKey(shippingId);
            shipping.setParcelsNumber(nbDewars);
            shipping.setSamplesNumber(nbSamples);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * DownloadFile
	 * @param mapping
	 * @param actForm
	 * @param request
	 * @param response
	 * @return
	 */
    public ActionForward DownloadFile(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        ActionMessages messages = new ActionMessages();
        String fileType = request.getParameter(Constants.TEMPLATE_FILE_TYPE);
        if (fileType == null) fileType = this.mFileType;
        try {
            String targetUpload = new String("");
            String attachmentFilename = new String("");
            if (fileType.equals(Constants.TEMPLATE_FILE_TYPE_TEMPLATE)) {
                targetUpload = Constants.TEMPLATE_RELATIVE_PATH;
                targetUpload = request.getRealPath(targetUpload);
                attachmentFilename = Constants.TEMPLATE_XLS_FILENAME;
            }
            if (fileType.equals(Constants.TEMPLATE_FILE_TYPE_POPULATED_TEMPLATE)) {
                targetUpload = this.PopulateTemplate(request, true, false, false, null, null, false, 0, false, 0);
                attachmentFilename = this.PopulateTemplate(request, false, true, false, null, null, false, 0, false, 0);
            }
            if (fileType.equals(Constants.TEMPLATE_FILE_TYPE_POPULATED_TEMPLATE_ADVANCED)) {
                targetUpload = this.PopulateTemplate(request, true, false, true, null, null, false, 0, false, 0);
                attachmentFilename = this.PopulateTemplate(request, false, true, true, null, null, false, 0, false, 0);
            }
            if (fileType.equals(Constants.TEMPLATE_FILE_TYPE_EXPORT_SHIPPING)) {
                targetUpload = PopulateTemplate(request, true, false, false, null, null, false, 0, false, 0);
                attachmentFilename = PopulateTemplate(request, false, true, false, null, null, false, 0, false, 0);
            }
            if (fileType.equals(Constants.TEMPLATE_FILE_TYPE_POPULATED_TEMPLATE_FROM_SHIPMENT)) {
                Integer _shippingId = Integer.decode(request.getParameter(Constants.SHIPPING_ID));
                int shippingId = (_shippingId != null) ? _shippingId.intValue() : 0;
                targetUpload = this.PopulateTemplate(request, true, false, false, null, null, false, 0, true, shippingId);
                attachmentFilename = this.PopulateTemplate(request, false, true, false, null, null, false, 0, true, shippingId);
            }
            try {
                byte[] imageBytes = FileUtil.readBytes(targetUpload);
                response.setContentLength(imageBytes.length);
                ServletOutputStream out = response.getOutputStream();
                response.setHeader("Pragma", "public");
                response.setHeader("Cache-Control", "max-age=0");
                response.setContentType("application/vnd.ms-excel");
                response.setHeader("Content-Disposition", "attachment; filename=" + attachmentFilename);
                out.write(imageBytes);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            ClientLogger.getInstance().error(e.toString());
            saveErrors(request, errors);
            return mapping.findForward("error");
        }
        return null;
    }
}
