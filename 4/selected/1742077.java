package ispyb.client.results;

import fr.improve.struts.taglib.layout.util.FormUtils;
import fr.improve.struts.taglib.layout.util.TabsUtil;
import ispyb.client.BreadCrumbsForm;
import ispyb.client.results.image.FileUtil;
import ispyb.client.util.ClientLogger;
import ispyb.client.util.Confidentiality;
import ispyb.common.util.Constants;
import ispyb.common.util.PropertyLoader;
import ispyb.common.util.StringUtils;
import ispyb.server.data.interfaces.BeamLineSetupFacadeLocal;
import ispyb.server.data.interfaces.BeamLineSetupFacadeUtil;
import ispyb.server.data.interfaces.BeamLineSetupLightValue;
import ispyb.server.data.interfaces.BlsampleFacadeLocal;
import ispyb.server.data.interfaces.BlsampleFacadeUtil;
import ispyb.server.data.interfaces.BlsampleLightValue;
import ispyb.server.data.interfaces.CrystalFacadeLocal;
import ispyb.server.data.interfaces.CrystalFacadeUtil;
import ispyb.server.data.interfaces.CrystalLightValue;
import ispyb.server.data.interfaces.DataCollectionFacadeLocal;
import ispyb.server.data.interfaces.DataCollectionFacadeUtil;
import ispyb.server.data.interfaces.DataCollectionLightValue;
import ispyb.server.data.interfaces.DataCollectionValue;
import ispyb.server.data.interfaces.ImageFacadeLocal;
import ispyb.server.data.interfaces.ImageFacadeUtil;
import ispyb.server.data.interfaces.ImageLightValue;
import ispyb.server.data.interfaces.ImageValue;
import ispyb.server.data.interfaces.ProteinFacadeLocal;
import ispyb.server.data.interfaces.ProteinFacadeUtil;
import ispyb.server.data.interfaces.ProteinLightValue;
import ispyb.server.data.interfaces.ScalingLightValue;
import ispyb.server.data.interfaces.ScreeningFacadeLocal;
import ispyb.server.data.interfaces.ScreeningFacadeUtil;
import ispyb.server.data.interfaces.ScreeningLightValue;
import ispyb.server.data.interfaces.ScreeningOutputFacadeLocal;
import ispyb.server.data.interfaces.ScreeningOutputFacadeUtil;
import ispyb.server.data.interfaces.ScreeningOutputLatticeLightValue;
import ispyb.server.data.interfaces.ScreeningOutputLightValue;
import ispyb.server.data.interfaces.ScreeningOutputValue;
import ispyb.server.data.interfaces.ScreeningRankLightValue;
import ispyb.server.data.interfaces.ScreeningRankSetFacadeLocal;
import ispyb.server.data.interfaces.ScreeningRankSetFacadeUtil;
import ispyb.server.data.interfaces.ScreeningRankSetValue;
import ispyb.server.data.interfaces.ScreeningStrategyFacadeLocal;
import ispyb.server.data.interfaces.ScreeningStrategyFacadeUtil;
import ispyb.server.data.interfaces.ScreeningStrategyLightValue;
import ispyb.server.data.interfaces.ScreeningValue;
import ispyb.server.data.interfaces.SessionFacadeLocal;
import ispyb.server.data.interfaces.SessionFacadeUtil;
import ispyb.server.data.interfaces.SessionLightValue;
import ispyb.server.util.PathUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

/**
 * Clss to handle results from beamline datacollections
 * 
 * @struts.action name="viewResultsForm" path="/user/viewResults"
 *                input="user.collection.viewDataCollection.page"
 *                validate="false" parameter="reqCode" scope="request"
 * 
 * @struts.action-forward name="error" path="site.default.error.page"
 * @struts.action-forward name="success" path="user.results.view.page"
 * @struts.action-forward name="viewImageApplet"
 *                        path="user.results.viewImageApplet.page"
 * @struts.action-forward name="viewDNAImages"
 *                        path="user.results.DNAImages.page"
 * @struts.action-forward name="viewDNAFiles"
 *                        path="user.results.DNAFiles.page"
 * @struts.action-forward name="viewOtherDNAFiles"
 *                        path="user.results.otherDNAFiles.page"
 * @struts.action-forward name="viewDNATextFiles"
 *                        path="user.results.DNATextFiles.page"
 * @struts.action-forward name="viewDenzoImages"
 *                        path="user.results.DenzoImages.page"
 * @struts.action-forward name="viewJpegImage"
 *                        path="user.results.viewJpegImage.page"
 * @struts.action-forward name="viewImageThumbnails"
 *                        path="user.results.viewImageThumbnails.page"
 * 
 * @web.ejb-local-ref name="ejb/ImageFacade" type="Stateless"
 *                    home="ispyb.server.data.interfaces.ImageFacadeLocalHome"
 *                    local="ispyb.server.interfaces.ImageFacadeLocal"
 *                    link="ImageFacade"
 * 
 * @jboss.ejb-local-ref jndi-name="ispyb/ImageFacadeLocalHome"
 *                      ref-name="ImageFacade"
 */
public class ViewResultsAction extends DispatchAction {

    Properties mProp = PropertyLoader.loadProperties("ISPyB");

    private String dataCollectionIdst;

    private String screeningRankSetIdst;

    ActionMessages errors = new ActionMessages();

    private static final String DENZO_HTML_INDEX = "index.html";

    private static final int SNAPSHOT_EXPECTED_NUMBER = 4;

    /**
     * To display all the parameters linked to a dataCollectionId.
     * 
     * @param mapping
     * @param actForm
     * @param request
     * @param in_reponse
     * @return
     */
    public ActionForward display(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse in_reponse) {
        ActionMessages errors = new ActionMessages();
        boolean redirectToError = true;
        boolean displayOutputParam = false;
        try {
            boolean DNAContentPresent = false;
            boolean DNARankingProjectFilePresent = false;
            boolean DNARankingSummaryFilePresent = false;
            boolean DenzonContentPresent = false;
            boolean displayDenzoContent = false;
            boolean dna_logContentPresent = false;
            boolean mosflm_triclintContentPresent = false;
            boolean scala_logContentPresent = false;
            boolean pointlessContentPresent = false;
            dataCollectionIdst = request.getParameter(Constants.DATA_COLLECTION_ID);
            Integer dataCollectionId = new Integer(dataCollectionIdst);
            Integer screeningId;
            String rankingProjectFileName = null;
            String rankingSummaryFileName = null;
            ViewResultsForm form = (ViewResultsForm) actForm;
            DataCollectionFacadeLocal dataCollection = DataCollectionFacadeUtil.getLocalHome().create();
            DataCollectionLightValue dclv = dataCollection.findByPrimaryKeyLight(dataCollectionId);
            DataCollectionValue dcv = dataCollection.findByPrimaryKey(dataCollectionId);
            if (dclv.getImagePrefix().startsWith("ref-")) {
                displayOutputParam = true;
            }
            ScreeningLightValue[] screeningList = null;
            Integer sessionId = new Integer(dclv.getSessionId());
            SessionFacadeLocal session = SessionFacadeUtil.getLocalHome().create();
            SessionLightValue sessionlv = session.findByPrimaryKeyLight(sessionId);
            form.setSession(sessionlv);
            if (!Confidentiality.isAccessAllowed(request, sessionlv.getProposalId())) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", "Access denied"));
                saveErrors(request, errors);
                return (mapping.findForward("error"));
            }
            if (sessionlv.getBeamLineSetupId() != null && sessionlv.getBeamLineSetupId().intValue() != 0) {
                Integer beamLineId = new Integer(sessionlv.getBeamLineSetupId());
                BeamLineSetupFacadeLocal beamLine = BeamLineSetupFacadeUtil.getLocalHome().create();
                BeamLineSetupLightValue beamLinelv = beamLine.findByPrimaryKey(beamLineId);
                form.setBeamLine(beamLinelv);
                form.setUndulatorTypes(beamLinelv.getUndulatorType1(), beamLinelv.getUndulatorType2(), beamLinelv.getUndulatorType3());
            }
            ScreeningLightValue[] screenings = dcv.getScreenings();
            if (screenings.length > 0) {
                displayOutputParam = true;
                int length = screenings.length;
                screeningList = screenings;
                screeningId = screeningList[length - 1].getScreeningId();
                ScreeningRankLightValue srlv = new ScreeningRankLightValue();
                ScreeningOutputLightValue solv = new ScreeningOutputLightValue();
                ScreeningOutputValue sov = new ScreeningOutputValue();
                ScreeningRankSetValue srsv = new ScreeningRankSetValue();
                ScreeningOutputLatticeLightValue solav = new ScreeningOutputLatticeLightValue();
                ScreeningStrategyLightValue[] screeningStrategyList;
                List screeningInfoList = new ArrayList();
                ScreeningFacadeLocal screening = ScreeningFacadeUtil.getLocalHome().create();
                ScreeningOutputFacadeLocal screeningOutput = ScreeningOutputFacadeUtil.getLocalHome().create();
                ScreeningRankSetFacadeLocal screeningRankSet = ScreeningRankSetFacadeUtil.getLocalHome().create();
                ScreeningValue sv = screening.findByPrimaryKey(screeningId);
                ScreeningRankLightValue[] screeningRanks = sv.getScreeningRanks();
                if (screeningRanks.length > 0) {
                    srlv = screeningRanks[0];
                    srsv = screeningRankSet.findByPrimaryKey(srlv.getScreeningRankSetId());
                    rankingProjectFileName = srsv.getRankingProjectFileName();
                    rankingSummaryFileName = srsv.getRankingSummaryFileName();
                }
                ScreeningOutputLightValue[] screeningOutputs = sv.getScreeningOutputs();
                if (screeningOutputs.length > 0) {
                    solv = screeningOutputs[0];
                    sov = screeningOutput.findByPrimaryKey(solv.getPrimaryKey());
                }
                ScreeningOutputLatticeLightValue[] screeningOutputLattices = sov.getScreeningOutputLattices();
                if (screeningOutputLattices.length > 0) {
                    solav = screeningOutputLattices[0];
                    form.setScreeningOutputLattice(solav);
                }
                ScreeningStrategyLightValue[] screeningStrategys = sov.getScreeningStrategys();
                if (screeningStrategys.length > 0) {
                    screeningStrategyList = screeningStrategys;
                    for (int j = 0; j < screeningStrategyList.length; j++) {
                        ScreeningStrategyFacadeLocal screeningStrategy = ScreeningStrategyFacadeUtil.getLocalHome().create();
                        ScreeningStrategyValueInfo ssvi = new ScreeningStrategyValueInfo(screeningStrategy.findByPrimaryKey(screeningStrategyList[j].getPrimaryKey()));
                        ssvi.setProgramLog(dataCollectionId);
                        screeningInfoList.add(ssvi);
                    }
                    form.setScreeningStrategyList(screeningStrategyList);
                    form.setListStrategiesInfo(screeningInfoList);
                }
                ScalingLightValue scaling = dcv.getScaling();
                form.setScreeningRank(srlv);
                form.setScreeningRankSet(srsv);
                form.setScreeningOutput(solv);
                form.setScaling(scaling);
            }
            if (dclv.getBlSampleId() != null && dclv.getBlSampleId().intValue() != 0) {
                BlsampleFacadeLocal sample = BlsampleFacadeUtil.getLocalHome().create();
                BlsampleLightValue bslv = sample.findByPrimaryKeyLight(dclv.getBlSampleId());
                BreadCrumbsForm.getIt(request).setSelectedSample(bslv);
                CrystalFacadeLocal crystal = CrystalFacadeUtil.getLocalHome().create();
                CrystalLightValue clv = crystal.findByPrimaryKeyLight(bslv.getCrystalId());
                ProteinFacadeLocal protein = ProteinFacadeUtil.getLocalHome().create();
                ProteinLightValue plv = protein.findByPrimaryKeyLight(clv.getProteinId());
                form.setCrystal(clv);
                form.setProtein(plv);
                form.setSample(bslv);
            } else {
                BreadCrumbsForm.getIt(request).setSelectedSample(null);
            }
            List imageList = this.GetImageList(dataCollectionId, null, null, null, null);
            Collections.reverse(imageList);
            String fullDNAPath = PathUtils.GetFullDNAPath(dataCollectionId);
            DNAContentPresent = (new File(fullDNAPath + Constants.DNA_FILES_INDEX_FILE)).exists();
            String fullDNARankingPath = PathUtils.GetFullDNARankingPath(dataCollectionId);
            DNARankingProjectFilePresent = (new File(fullDNARankingPath + rankingProjectFileName).exists());
            DNARankingSummaryFilePresent = (new File(fullDNARankingPath + rankingSummaryFileName).exists());
            String fullLogPath = PathUtils.GetFullLogPath(dataCollectionId);
            dna_logContentPresent = (new File(fullLogPath + Constants.DNA_FILES_LOG_FILE)).exists();
            mosflm_triclintContentPresent = (new File(fullLogPath + Constants.DNA_FILES_MOSFLM_TRICLINT_FILE)).exists();
            scala_logContentPresent = (new File(fullLogPath + Constants.DNA_FILES_SCALA_FILE)).exists();
            pointlessContentPresent = (new File(fullLogPath + Constants.DNA_FILES_POINTLESS_FILE)).exists();
            List listSnapshots = GetFullSnapshotPath(dataCollectionId);
            String expectedSnapshotPath = ((SnapshotInfo) listSnapshots.get(SNAPSHOT_EXPECTED_NUMBER)).getFileLocation();
            String fullDenzoPath = null;
            if (Constants.DENZO_ENABLED) {
                fullDenzoPath = GetFullDenzoPath(dataCollectionId);
                DenzonContentPresent = (new File(fullDenzoPath)).exists();
                displayDenzoContent = DisplayDenzoContent(dataCollectionId);
                if (DenzonContentPresent) {
                    File denzoIndex = new File(fullDenzoPath + DENZO_HTML_INDEX);
                    if (!denzoIndex.exists()) {
                        redirectToError = false;
                        errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", "File does not exist " + denzoIndex));
                        saveErrors(request, errors);
                        DenzonContentPresent = false;
                    }
                }
            }
            form.setDisplayOutputParam(displayOutputParam);
            form.setDNAContentPresent(DNAContentPresent);
            form.setScala_logContentPresent(scala_logContentPresent);
            form.setDna_logContentPresent(dna_logContentPresent);
            form.setMosflm_triclintContentPresent(mosflm_triclintContentPresent);
            form.setPointlessContentPresent(pointlessContentPresent);
            form.setDNARankingProjectFilePresent(DNARankingProjectFilePresent);
            form.setDNARankingSummaryFilePresent(DNARankingSummaryFilePresent);
            form.setDenzonContentPresent(DenzonContentPresent);
            form.setExpectedDenzoPath(fullDenzoPath);
            form.setDisplayDenzoContent(displayDenzoContent);
            form.setDataCollectionId(dataCollectionId);
            form.setDataCollection(dclv);
            form.setEnergy(dclv.getWavelength());
            form.setUndulatorGaps(dclv.getUndulatorGap1(), dclv.getUndulatorGap2(), dclv.getUndulatorGap3());
            form.setAxisStartLabel(dclv.getRotationAxis());
            form.setScreeningList(screeningList);
            form.setListSnapshots(listSnapshots);
            form.setExpectedSnapshotPath(expectedSnapshotPath);
            form.setListInfo(imageList);
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
            BreadCrumbsForm.getIt(request).setSelectedImage(null);
            BreadCrumbsForm.getIt(request).setSelectedDataCollection(dclv);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.view"));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        String tabKeyResult = (String) request.getParameter("tabKeyResult");
        if (tabKeyResult != null) TabsUtil.setCurrentTab("tabKeyResult", tabKeyResult, request, in_reponse);
        if (!errors.isEmpty() && redirectToError) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("success");
    }

    /**
 * GetImageList
 * @param dataCollectionId
 * @return
 * @throws Exception
 */
    public static List GetImageList(Integer dataCollectionId, Integer extract_ImageId, Integer extract_ImageNumber, Integer nbImagesHorizontal, Integer nbImagesVertical) throws Exception {
        ImageFacadeLocal image = ImageFacadeUtil.getLocalHome().create();
        List tmp_imageList = (List) image.findByDataCollectionId(dataCollectionId);
        List imageList = new ArrayList(tmp_imageList.size());
        int imgNumberHorizontal = 0;
        int imgNumberVertical = 0;
        for (int i = 0; i < tmp_imageList.size(); i++) {
            imgNumberHorizontal++;
            ImageValue imgValue = (ImageValue) tmp_imageList.get(i);
            ImageValueInfo imageValueInfo = new ImageValueInfo(imgValue);
            Integer currId = imgValue.getImageId();
            Integer prevId = (i > 0) ? ((ImageValue) tmp_imageList.get(i - 1)).getImageId() : currId;
            Integer nextId = (i < (tmp_imageList.size() - 1)) ? ((ImageValue) tmp_imageList.get(i + 1)).getImageId() : currId;
            Integer imageNumber = new Integer(i + 1);
            boolean first = (i == 0) ? true : false;
            boolean last = (i == tmp_imageList.size() - 1) ? true : false;
            imageValueInfo.setCurrentImageId(currId);
            imageValueInfo.setPreviousImageId(prevId);
            imageValueInfo.setNextImageId(nextId);
            imageValueInfo.setImageNumber(imageNumber);
            imageValueInfo.setFirst(first);
            imageValueInfo.setLast(last);
            imageValueInfo.setSynchrotronCurrent(imgValue.getSynchrotronCurrent());
            imageValueInfo.setFormattedData();
            imageValueInfo.setLastImageHorizontal(false);
            imageValueInfo.setLastImageVertical(false);
            if (nbImagesHorizontal != null && imgNumberHorizontal == nbImagesHorizontal && nbImagesHorizontal != 0) {
                imageValueInfo.setLastImageHorizontal(true);
                imgNumberHorizontal = 0;
                imgNumberVertical++;
            }
            if (nbImagesVertical != null && imgNumberVertical == nbImagesVertical && nbImagesVertical != 0) {
                imageValueInfo.setLastImageVertical(true);
                imgNumberVertical = 0;
            }
            if (extract_ImageId != null && extract_ImageId.equals(currId)) {
                imageList.add(0, imageValueInfo);
            } else {
                if (extract_ImageNumber != null && extract_ImageNumber.equals(imageNumber)) imageList.add(0, imageValueInfo);
            }
            imageList.add(imageValueInfo);
        }
        return imageList;
    }

    public static List<String> getImageJpgThumbList(Integer dataCollectionId) throws Exception {
        ImageFacadeLocal image = ImageFacadeUtil.getLocalHome().create();
        List tmp_imageList = (List) image.findByDataCollectionId(dataCollectionId);
        List<String> imageList = new ArrayList<String>(tmp_imageList.size());
        for (int i = 0; i < tmp_imageList.size(); i++) {
            ImageValue imgValue = (ImageValue) tmp_imageList.get(i);
            String jpgThumbFullPath = imgValue.getJpegThumbnailFileFullPath();
            imageList.add(jpgThumbFullPath);
        }
        return imageList;
    }

    /**
 * forward to the page with dna results
 * 
 * @param mapping
 * @param actForm
 * @param request
 * @param response
 * @return
 */
    public ActionForward displayDNAFiles(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        boolean dataProcessingContentPresent = false;
        boolean integrationContentPresent = false;
        boolean strategyContentPresent = false;
        boolean dna_logContentPresent = false;
        boolean mosflm_triclintContentPresent = false;
        boolean scala_logContentPresent = false;
        boolean pointlessContentPresent = false;
        try {
            ViewResultsForm form = (ViewResultsForm) actForm;
            dataCollectionIdst = request.getParameter(Constants.DATA_COLLECTION_ID);
            Integer dataCollectionId = new Integer(dataCollectionIdst);
            String fullDNAPath = PathUtils.GetFullDNAPath(dataCollectionId);
            String fullDNAIndexPath = fullDNAPath + "index.html";
            String fullLogPath = PathUtils.GetFullLogPath(dataCollectionId);
            String fullDNAurl = request.getContextPath() + "/user/imageDownload.do?reqCode=getImageDNA";
            ClientLogger.getInstance().debug("displayDNAFiles: fullDNAurl= " + fullDNAurl);
            String hrefDNAurl = request.getContextPath() + "/user/viewResults.do?reqCode=viewJpegImageFromFile";
            String fullDNAFileContent = FileUtil.fileToString(fullDNAIndexPath);
            if (fullDNAFileContent == null) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
                return this.display(mapping, actForm, request, response);
            }
            String fullDNAFileContentChanged = StringUtils.formatImageURL(fullDNAFileContent, fullDNAurl, hrefDNAurl, fullDNAPath);
            String fullHtmlFileContentChangedNoLink = StringUtils.deleteIndexLinks(fullDNAFileContentChanged);
            String fullDataProcessingPath = PathUtils.GetFullDataProcessingPath(dataCollectionId) + Constants.DNA_FILES_DATA_PROC_FILE;
            dataProcessingContentPresent = (new File(fullDataProcessingPath)).exists();
            String fullIntegrationPath = PathUtils.GetFullIntegrationPath(dataCollectionId) + Constants.DNA_FILES_INTEGRATION_FILE;
            integrationContentPresent = (new File(fullIntegrationPath)).exists();
            String fullStrategyPath = PathUtils.GetFullStrategyPath(dataCollectionId) + Constants.DNA_FILES_STRATEGY_FILE;
            strategyContentPresent = (new File(fullStrategyPath)).exists();
            dna_logContentPresent = (new File(fullLogPath + Constants.DNA_FILES_LOG_FILE)).exists();
            mosflm_triclintContentPresent = (new File(fullLogPath + Constants.DNA_FILES_MOSFLM_TRICLINT_FILE)).exists();
            scala_logContentPresent = (new File(fullLogPath + Constants.DNA_FILES_SCALA_FILE)).exists();
            pointlessContentPresent = (new File(fullLogPath + Constants.DNA_FILES_POINTLESS_FILE)).exists();
            form.setDNAContent(fullHtmlFileContentChangedNoLink);
            form.setDataProcessingContentPresent(dataProcessingContentPresent);
            form.setIntegrationContentPresent(integrationContentPresent);
            form.setStrategyContentPresent(strategyContentPresent);
            form.setScala_logContentPresent(scala_logContentPresent);
            form.setDna_logContentPresent(dna_logContentPresent);
            form.setMosflm_triclintContentPresent(mosflm_triclintContentPresent);
            form.setPointlessContentPresent(pointlessContentPresent);
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewDNAImages");
    }

    public ActionForward displayDNA_LOG(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ViewResultsForm form = (ViewResultsForm) actForm;
        form.setDNASelectedFile(Constants.DNA_FILES_LOG_FILE);
        return this.displayDNALogFiles(mapping, actForm, request, response);
    }

    public ActionForward displayDNA_MOSFLM_TRI(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ViewResultsForm form = (ViewResultsForm) actForm;
        form.setDNASelectedFile(Constants.DNA_FILES_MOSFLM_TRICLINT_FILE);
        return this.displayDNALogFiles(mapping, actForm, request, response);
    }

    public ActionForward displayDNA_SCALA_LOG(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ViewResultsForm form = (ViewResultsForm) actForm;
        form.setDNASelectedFile(Constants.DNA_FILES_SCALA_FILE);
        return this.displayDNALogFiles(mapping, actForm, request, response);
    }

    public ActionForward displayDNA_POINTLESS_LOG(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ViewResultsForm form = (ViewResultsForm) actForm;
        form.setDNASelectedFile(Constants.DNA_FILES_POINTLESS_FILE);
        return this.displayDNALogFiles(mapping, actForm, request, response);
    }

    /**
 * forward to the page with dna files present in xxx_dnafiles
 * 
 * @param mapping
 * @param actForm
 * @param request
 * @param response
 * @return
 */
    public ActionForward displayDNALogFiles(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        boolean contentPresent = false;
        try {
            ViewResultsForm form = (ViewResultsForm) actForm;
            dataCollectionIdst = request.getParameter(Constants.DATA_COLLECTION_ID);
            Integer dataCollectionId = new Integer(dataCollectionIdst);
            String fileName = form.getDNASelectedFile();
            String fullLogPath = PathUtils.GetFullLogPath(dataCollectionId) + fileName;
            ClientLogger.getInstance().debug("displayDNALogFiles: " + fullLogPath);
            contentPresent = (new File(fullLogPath)).exists();
            if (!contentPresent) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
                return this.display(mapping, actForm, request, response);
            }
            String fullDNAFileContent = FileUtil.fileToString(fullLogPath);
            form.setDNAContent(fullDNAFileContent);
            form.setDNAContentPresent(contentPresent);
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewDNATextFiles");
    }

    public static String GetFullDenzoPath(Integer dataCollectionId) throws Exception {
        DataCollectionFacadeLocal dataCollection = DataCollectionFacadeUtil.getLocalHome().create();
        DataCollectionLightValue dcValue = dataCollection.findByPrimaryKey(dataCollectionId);
        String imgPrefix = dcValue.getImagePrefix();
        while (imgPrefix.startsWith("ref-")) {
            imgPrefix = imgPrefix.substring(4);
        }
        String imageDir = dcValue.getImageDirectory() + "/";
        imageDir = imageDir.trim();
        String fullDenzoPath = imageDir + Constants.IMG_DENZO_URL_PREFIX;
        String archive_fullDenzoPath = PathUtils.GetArchiveEquivalentPath(fullDenzoPath);
        fullDenzoPath = StringUtils.FitPathToOS(fullDenzoPath);
        archive_fullDenzoPath = StringUtils.FitPathToOS(archive_fullDenzoPath);
        File file_archive_fullDenzoPath = new File(archive_fullDenzoPath);
        if (file_archive_fullDenzoPath.exists()) {
            fullDenzoPath = archive_fullDenzoPath;
        } else {
            File originalFilePath = new File(fullDenzoPath);
            File archiveFilePath = new File(archive_fullDenzoPath);
            try {
                FileUtils.copyDirectory(originalFilePath, archiveFilePath);
            } catch (Exception e) {
                ClientLogger.getInstance().debug("SyncAndCopyDenzoFiles : Error when trying to Sync: \n originalFilePath=" + originalFilePath + "\narchiveFilePath=" + archiveFilePath);
            }
        }
        return fullDenzoPath;
    }

    public static boolean DisplayDenzoContent(Integer dataCollectionId) throws Exception {
        boolean displayDenzoContent = false;
        DataCollectionFacadeLocal dataCollection = DataCollectionFacadeUtil.getLocalHome().create();
        DataCollectionLightValue dcValue = dataCollection.findByPrimaryKey(dataCollectionId);
        SessionFacadeLocal _session = SessionFacadeUtil.getLocalHome().create();
        SessionLightValue session = _session.findByPrimaryKey(dcValue.getSessionId());
        try {
            String beamlineName = session.getBeamLineName().toLowerCase();
            if (beamlineName.indexOf(Constants.BEAMLINE_NAME_BM14) != -1) displayDenzoContent = true;
        } catch (Exception e) {
        }
        return displayDenzoContent;
    }

    public static ArrayList GetFullSnapshotPath(Integer dataCollectionId) throws Exception {
        ArrayList snapshotPath = new ArrayList();
        boolean isWindows = (System.getProperty("os.name").indexOf("Win") != -1) ? true : false;
        DataCollectionFacadeLocal dataCollection = DataCollectionFacadeUtil.getLocalHome().create();
        DataCollectionLightValue dcValue = dataCollection.findByPrimaryKey(dataCollectionId);
        String expectedSnapShotPath = "";
        String fullSnapshotPath = "";
        for (int s = 0; s < SNAPSHOT_EXPECTED_NUMBER; s++) {
            switch(s + 1) {
                case 1:
                    fullSnapshotPath = dcValue.getXtalSnapshotFullPath1();
                    expectedSnapShotPath = fullSnapshotPath;
                    if (expectedSnapShotPath == null) expectedSnapShotPath = "";
                    expectedSnapShotPath = StringUtils.FitPathToOS(expectedSnapShotPath);
                    break;
                case 2:
                    fullSnapshotPath = dcValue.getXtalSnapshotFullPath2();
                    break;
                case 3:
                    fullSnapshotPath = dcValue.getXtalSnapshotFullPath3();
                    break;
                case 4:
                    fullSnapshotPath = dcValue.getXtalSnapshotFullPath4();
                    break;
            }
            if (fullSnapshotPath == null) fullSnapshotPath = "";
            fullSnapshotPath = StringUtils.FitPathToOS(fullSnapshotPath);
            File foundFile = new File(fullSnapshotPath);
            boolean fileExists = (foundFile.exists() && foundFile.isFile());
            SnapshotInfo snapshotInfo = new SnapshotInfo(fullSnapshotPath, fileExists);
            snapshotPath.add(snapshotInfo);
        }
        SnapshotInfo snapshotInfo = new SnapshotInfo(expectedSnapShotPath, false);
        snapshotPath.add(SNAPSHOT_EXPECTED_NUMBER, snapshotInfo);
        return snapshotPath;
    }

    /**
 * Foward to the page with Denzo results
 * 
 * @param mapping
 * @param actForm
 * @param request
 * @param response
 * @return
 */
    public ActionForward displayDenzoFiles(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        try {
            ViewResultsForm form = (ViewResultsForm) actForm;
            dataCollectionIdst = request.getParameter(Constants.DATA_COLLECTION_ID);
            Integer dataCollectionId = new Integer(dataCollectionIdst);
            String fullDenzoPath = GetFullDenzoPath(dataCollectionId);
            String fullDenzoFilePath = fullDenzoPath + DENZO_HTML_INDEX;
            ClientLogger.getInstance().debug("displayDenzoFiles: full Denzo file path = " + fullDenzoFilePath);
            String fullDenzoUrl = request.getContextPath() + "/user/imageDownload.do?reqCode=getImageDNA";
            ClientLogger.getInstance().debug("displayDenzoFiles : full Denzo url = " + fullDenzoUrl);
            String fullDenzoFileContent = FileUtil.fileToString(fullDenzoFilePath);
            if (fullDenzoFileContent == null) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
                return this.display(mapping, actForm, request, response);
            }
            String fullDenzoFileContentChanged = StringUtils.formatImageURL_Denzo(fullDenzoFileContent, fullDenzoUrl, fullDenzoPath);
            form.setDenzoContent(fullDenzoFileContentChanged);
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewDenzoImages");
    }

    /**
     * View a simple jpeg image
     * 
     * @param mapping
     * @param actForm
     * @param request
     * @param response
     * @return
     */
    public ActionForward viewJpegImage(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        try {
            ViewResultsForm form = (ViewResultsForm) actForm;
            Integer imageId = (request.getParameter(Constants.IMAGE_ID) != null) ? new Integer(request.getParameter(Constants.IMAGE_ID)) : null;
            Integer targetImageNumber = form.getTargetImageNumber();
            Integer dataCollectionId = BreadCrumbsForm.getIt(request).getSelectedDataCollection().getDataCollectionId();
            ImageFacadeLocal refImages = ImageFacadeUtil.getLocalHome().create();
            ImageLightValue refImage = refImages.findByPrimaryKey(imageId);
            DataCollectionFacadeLocal refDataCollections = DataCollectionFacadeUtil.getLocalHome().create();
            DataCollectionLightValue refDataCollection = refDataCollections.findByPrimaryKey(refImage.getDataCollectionId());
            SessionFacadeLocal refSessions = SessionFacadeUtil.getLocalHome().create();
            SessionLightValue refSession = refSessions.findByPrimaryKeyLight(refDataCollection.getSessionId());
            if (!Confidentiality.isAccessAllowed(request, refSession.getProposalId())) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", "Access denied"));
                saveErrors(request, errors);
                return (mapping.findForward("error"));
            }
            ArrayList imageFetchedList = (ArrayList) this.GetImageList(dataCollectionId, imageId, targetImageNumber, null, null);
            if (imageFetchedList.size() != 0) {
                ImageValueInfo imageToDisplay = (ImageValueInfo) imageFetchedList.get(0);
                form.setImage(imageToDisplay);
                form.setTargetImageNumber(imageToDisplay.getImageNumber());
                form.setTotalImageNumber(new Integer(imageFetchedList.size() - 1));
                BreadCrumbsForm.getIt(request).setSelectedImage((ImageLightValue) imageFetchedList.get(0));
            } else {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewImageId", imageId));
                ClientLogger.getInstance().warn("List fetched has a size != 1!!");
            }
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.general.image"));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewJpegImage");
    }

    /**
     * View a simple jpeg image
     * 
     * @param mapping
     * @param actForm
     * @param request
     * @param response
     * @return
     */
    public ActionForward viewJpegImageFromFile(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        try {
            ViewResultsForm form = (ViewResultsForm) actForm;
            String imageFileName = request.getParameter(Constants.IMG_SNAPSHOT_URL_PARAM);
            if (imageFileName != null) {
                form.setImage(null);
                SnapshotInfo snapshotInfo = new SnapshotInfo(imageFileName);
                form.setSnapshotInfo(snapshotInfo);
            }
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.general.image"));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewJpegImage");
    }

    /**
     * getDataFromFile
     * 
     * @param mapping
     * @param actForm
     * @param request
     * @param response
     * @return
     */
    public ActionForward getDataFromFile(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        boolean isWindows = (System.getProperty("os.name").indexOf("Win") != -1) ? true : false;
        String tmpDirectory = ((isWindows) ? mProp.getProperty("bzip2.outputFilePath.windows") : mProp.getProperty("bzip2.outputFilePath"));
        try {
            Integer proposalId = (Integer) request.getSession().getAttribute(Constants.PROPOSAL_ID);
            Integer imageId = new Integer(request.getParameter(Constants.IMAGE_ID));
            ImageFacadeLocal imgFacade = ImageFacadeUtil.getLocalHome().create();
            ArrayList imageFetchedList = (ArrayList) imgFacade.findByImageIdAndProposalId(imageId, proposalId);
            if (imageFetchedList.size() == 1) {
                ImageLightValue selectedImage = ((ImageLightValue) imageFetchedList.get(0));
                String _sourceFileName = selectedImage.getFileLocation() + "/" + selectedImage.getFileName();
                _sourceFileName = (isWindows) ? "C:" + _sourceFileName : _sourceFileName;
                String _destinationFileName = selectedImage.getFileName();
                String _destinationfullFilename = tmpDirectory + "/" + _destinationFileName;
                String _bz2FileName = _destinationFileName + ".bz2";
                String _bz2FullFileName = _destinationfullFilename + ".bz2";
                File source = new File(_sourceFileName);
                File destination = new File(_destinationfullFilename);
                File bz2File = new File(_bz2FullFileName);
                FileUtils.copyFile(source, destination, false);
                String cmd = ((isWindows) ? mProp.getProperty("bzip2.path.windows") : mProp.getProperty("bzip2.path"));
                String argument = mProp.getProperty("bzip2.arguments");
                argument = " " + argument + " " + _destinationfullFilename;
                cmd = cmd + argument;
                this.CmdExec(cmd, false);
                Date now = new Date();
                long startTime = now.getTime();
                long timeNow = now.getTime();
                long timeOut = 60000;
                boolean filePresent = false;
                while (!filePresent && (timeNow - startTime) < timeOut) {
                    Date d2 = new Date();
                    timeNow = d2.getTime();
                    filePresent = bz2File.exists();
                }
                if (filePresent) Thread.sleep(10000);
                byte[] imageBytes = FileUtil.readBytes(_bz2FullFileName);
                response.setContentLength(imageBytes.length);
                response.setHeader("Content-Disposition", "attachment; filename=" + _bz2FileName);
                response.setContentType("application/x-bzip");
                ServletOutputStream out = response.getOutputStream();
                out.write(imageBytes);
                out.flush();
                out.close();
                FileCleaner fileCleaner = new FileCleaner(60000, _bz2FullFileName);
                fileCleaner.start();
                return null;
            } else {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewImageId", imageId));
                ClientLogger.getInstance().warn("List fetched has a size != 1!!");
            }
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.general.image"));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewJpegImage");
    }

    /**
     * FileCleaner
     * @author launer
     * Delete a file after a certain delay
     */
    private class FileCleaner extends Thread {

        long waitBeforeRun = 5000;

        String fileToDelete = "";

        FileCleaner(long _waitBeforeRun, String _fileToDelete) {
            this.waitBeforeRun = _waitBeforeRun;
            this.fileToDelete = _fileToDelete;
        }

        public void run() {
            try {
                Thread.sleep(this.waitBeforeRun);
                File targetFile = new File(fileToDelete);
                targetFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * CmdExec
     * 
     * @param cmdline
     * @return
     */
    public String CmdExec(String cmdline, boolean captureOutput) {
        String output = new String();
        try {
            String line;
            Process p = Runtime.getRuntime().exec(cmdline);
            if (captureOutput) {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((line = input.readLine()) != null) {
                    output += line;
                }
                input.close();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return output;
    }

    /**
     * foward to the page with dna results
     * 
     * @param mapping
     * @param actForm
     * @param request
     * @param response
     * @return
     */
    public ActionForward displayDNAHtmlFiles(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        try {
            ViewResultsForm form = (ViewResultsForm) actForm;
            dataCollectionIdst = request.getParameter(Constants.DATA_COLLECTION_ID);
            String selectedFile = request.getParameter(Constants.DNA_SELECTED_FILE);
            Integer dataCollectionId = new Integer(dataCollectionIdst);
            String htmlFilePath;
            String htmlFullFilePath;
            if (selectedFile.equals(Constants.DNA_FILES_DATA_PROC)) {
                htmlFilePath = PathUtils.GetFullDataProcessingPath(dataCollectionId);
                htmlFullFilePath = PathUtils.GetFullDataProcessingPath(dataCollectionId) + "dpm_log.html";
            } else if (selectedFile.equals(Constants.DNA_FILES_INTEGRATION)) {
                htmlFilePath = PathUtils.GetFullIntegrationPath(dataCollectionId);
                htmlFullFilePath = PathUtils.GetFullIntegrationPath(dataCollectionId) + "index.html";
            } else if (selectedFile.equals(Constants.DNA_FILES_STRATEGY)) {
                htmlFilePath = PathUtils.GetFullStrategyPath(dataCollectionId);
                htmlFullFilePath = PathUtils.GetFullStrategyPath(dataCollectionId) + "index.html";
            } else {
                htmlFilePath = "";
                htmlFullFilePath = "";
            }
            ClientLogger.getInstance().debug("displayDNAHtmlFiles: htmlFullFilePath= " + htmlFullFilePath);
            String fullDNAUrl = request.getContextPath() + "/user/imageDownload.do?reqCode=getImageDNA";
            ClientLogger.getInstance().debug("displayDNAHtmlFiles: fullDNAUrl= " + fullDNAUrl);
            String hrefDNAurl = request.getContextPath() + "/user/viewResults.do?reqCode=viewJpegImageFromFile";
            String fullHtmlFileContent = FileUtil.fileToString(htmlFullFilePath);
            if (fullHtmlFileContent == null) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
                return this.display(mapping, actForm, request, response);
            }
            String fullHtmlFileContentChanged = StringUtils.formatImageURL(fullHtmlFileContent, fullDNAUrl, hrefDNAurl, htmlFilePath);
            String fullHtmlFileContentChangedNoLink = StringUtils.deleteIndexLinks(fullHtmlFileContentChanged);
            form.setDataCollectionId(dataCollectionId);
            form.setDNAContent(fullHtmlFileContentChangedNoLink);
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA", e.toString()));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewDNAFiles");
    }

    /**
     * foward to the page with dna results
     * 
     * @param mapping
     * @param actForm
     * @param request
     * @param response
     * @return
     */
    public ActionForward displayDNARankingFiles(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        try {
            ViewResultsForm form = (ViewResultsForm) actForm;
            dataCollectionIdst = request.getParameter(Constants.DATA_COLLECTION_ID);
            screeningRankSetIdst = request.getParameter(Constants.SCREENING_RANK_SET_ID);
            String selectedFile = request.getParameter(Constants.DNA_RANKING_SELECTED_FILE);
            Integer dataCollectionId = new Integer(dataCollectionIdst);
            Integer screeningRankSetId = new Integer(screeningRankSetIdst);
            ScreeningRankSetFacadeLocal screeningRankSet = ScreeningRankSetFacadeUtil.getLocalHome().create();
            ScreeningRankSetValue srsv = new ScreeningRankSetValue();
            srsv = screeningRankSet.findByPrimaryKey(screeningRankSetId);
            String htmlFilePath;
            String htmlFullFilePath;
            if (selectedFile.equals(Constants.DNA_FILES_RANKING_SUMMARY)) {
                htmlFilePath = PathUtils.GetFullDNARankingPath(dataCollectionId);
                htmlFullFilePath = htmlFilePath + srsv.getRankingSummaryFileName();
            } else {
                htmlFilePath = "";
                htmlFullFilePath = "";
            }
            ClientLogger.getInstance().debug("displayDNARankingFiles: htmlFullFilePath= " + htmlFullFilePath);
            String fullHtmlFileContent = FileUtil.fileToString(htmlFullFilePath);
            if (fullHtmlFileContent == null) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
                return this.display(mapping, actForm, request, response);
            }
            ClientLogger.getInstance().debug("selectedFile = " + selectedFile);
            ClientLogger.getInstance().debug("fullHtmlFileContent = " + fullHtmlFileContent);
            form.setDNASelectedFile(selectedFile);
            form.setDataCollectionId(dataCollectionId);
            form.setScreeningRankSet(srsv);
            form.setDNAContent(fullHtmlFileContent);
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA", e.toString()));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewDNATextFiles");
    }

    /**
     * foward to the page with dna results
     * 
     * @param mapping
     * @param actForm
     * @param request
     * @param response
     * @return
     */
    public ActionForward displayProgramLogFiles(ActionMapping mapping, ActionForm actForm, HttpServletRequest request, HttpServletResponse response) {
        ActionMessages errors = new ActionMessages();
        try {
            ViewResultsForm form = (ViewResultsForm) actForm;
            dataCollectionIdst = request.getParameter(Constants.DATA_COLLECTION_ID);
            Integer dataCollectionId = new Integer(dataCollectionIdst);
            String programLogPath = PathUtils.GetFullLogPath(dataCollectionId);
            String programLogFilePath = programLogPath + Constants.DNA_FILES_BEST_FILE;
            ClientLogger.getInstance().debug("displayProgramLogFiles: programLogFilePath= " + programLogFilePath);
            String fullFileContent = FileUtil.fileToString(programLogFilePath);
            if (fullFileContent == null) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA"));
                return this.display(mapping, actForm, request, response);
            }
            form.setDNAContent(fullFileContent);
            form.setDNASelectedFile(Constants.DNA_FILES_BEST_FILE);
            FormUtils.setFormDisplayMode(request, actForm, FormUtils.INSPECT_MODE);
        } catch (Exception e) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.user.results.viewDNA", e.toString()));
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.detail", e.toString()));
            e.printStackTrace();
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (mapping.findForward("error"));
        } else return mapping.findForward("viewDNATextFiles");
    }
}
