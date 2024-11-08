package uk.ac.ebi.metabolights.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.isatools.tablib.utils.logging.TabLoggingEventWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ebi.bioinvindex.model.Study;
import uk.ac.ebi.bioinvindex.model.VisibilityStatus;
import uk.ac.ebi.metabolights.metabolightsuploader.IsaTabUploader;
import uk.ac.ebi.metabolights.model.MetabolightsUser;
import uk.ac.ebi.metabolights.properties.PropertyLookup;
import uk.ac.ebi.metabolights.search.LuceneSearchResult;
import uk.ac.ebi.metabolights.service.SearchService;
import uk.ac.ebi.metabolights.service.StudyService;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Make a study public. THis implies to change the status in the database, reindex, and move the zip file to the public ftp.
 * 
 * @author conesa
 */
@Controller
public class UpdateStudyController extends AbstractController {

    private static Logger logger = Logger.getLogger(UpdateStudyController.class);

    @Value("#{appProperties.publicFtpStageLocation}")
    private String publicFtpLocation;

    @Value("#{appProperties.privateFtpStageLocation}")
    private String privateFtpLocation;

    @Value("#{appProperties.uploadDirectory}")
    private String uploadDirectory;

    @Autowired
    private SearchService searchService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private EntryController entryController;

    @Autowired
    private BIISubmissionController submissionController;

    /**
	 * Receives the study that is going to be published and shows the updateStudy Page to let the user to set the public release date.
	 * 
	 * @param study
	 * @param request
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value = { "/updatepublicreleasedateform" })
    public ModelAndView updatePublicReleaseDate(@RequestParam(required = true, value = "study") String study, HttpServletRequest request) throws Exception {
        return getModelAndView(study, false);
    }

    /**
	 * Receives the study that is going to be updated and shows the updateStudy Page to let the user to set the public release date and upload the new file.
	 * 
	 * @param study
	 * @param request
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value = { "/updatestudyform" })
    public ModelAndView updateStudy(@RequestParam(required = true, value = "study") String study, HttpServletRequest request) throws Exception {
        return getModelAndView(study, true);
    }

    /**
	 * Return the model and view ready to be rendered in the jsp that share 2 modes. Update and MakeStudyPublic
	 * @param study
	 * @param isUpdateMode
	 * @return
	 * @throws Exception 
	 */
    private ModelAndView getModelAndView(String study, boolean isUpdateMode) throws Exception {
        LuceneSearchResult luceneStudy = getStudy(study);
        ModelAndView mav = new ModelAndView("updateStudyForm");
        mav.addObject("searchResult", luceneStudy);
        mav.addObject("isUpdateMode", isUpdateMode);
        mav.addObject("study", study);
        String title = "", msg = "", action = "", submitText = "";
        String studyShortTitle = luceneStudy.getTitle();
        if (studyShortTitle.length() > 47) studyShortTitle = (studyShortTitle.substring(0, 47) + "...");
        if (isUpdateMode) {
            title = PropertyLookup.getMessage("msg.updatestudy.title", study, studyShortTitle);
            msg = PropertyLookup.getMessage("msg.updatestudy.msg");
            submitText = PropertyLookup.getMessage("label.updatestudy");
            action = "updatestudy";
            String ftpLocation = entryController.getDownloadLink(luceneStudy.getAccStudy(), luceneStudy.getIsPublic() ? VisibilityStatus.PUBLIC : VisibilityStatus.PRIVATE);
            mav.addObject("ftpLocation", ftpLocation);
        } else {
            title = PropertyLookup.getMessage("msg.makestudypublic.title", study, studyShortTitle);
            msg = PropertyLookup.getMessage("msg.makestudypublic.msg");
            submitText = PropertyLookup.getMessage("label.makestudypublic");
            action = "updatepublicreleasedate";
        }
        mav.addObject("title", title);
        mav.addObject("message", msg);
        mav.addObject("action", action);
        mav.addObject("submitText", submitText);
        return mav;
    }

    private ModelAndView validateParameters(RequestParameters params) throws Exception {
        params.validate();
        if (!params.validationmsg.isEmpty()) {
            ModelAndView validation = getModelAndView(params.studyId, params.isUpdateStudyMode);
            validation.addObject("validationmsg", params.validationmsg);
            return validation;
        }
        params.calculateStatusAndDate();
        return null;
    }

    @RequestMapping(value = { "/updatepublicreleasedate" })
    public ModelAndView changePublicReleaseDate(@RequestParam(required = true, value = "study") String study, @RequestParam(required = true, value = "pickdate") String publicReleaseDateS, HttpServletRequest request) throws Exception {
        RequestParameters params = new RequestParameters(publicReleaseDateS, study);
        logger.info("Updating the public release date of the study " + study + " owned by " + params.user.getUserName());
        ModelAndView validation = validateParameters(params);
        if (validation != null) {
            return validation;
        }
        String unzipFolder = uploadDirectory + params.user.getUserId() + "/" + study;
        IsaTabUploader itu = new IsaTabUploader();
        itu.setCopyToPrivateFolder(privateFtpLocation);
        itu.setCopyToPublicFolder(publicFtpLocation);
        ModelAndView mav = new ModelAndView("updateStudyForm");
        try {
            File zipFile = new File(itu.getStudyFilePath(study, VisibilityStatus.PUBLIC));
            if (!zipFile.exists()) {
                zipFile = new File(itu.getStudyFilePath(study, VisibilityStatus.PRIVATE));
                if (!zipFile.exists()) {
                    throw new FileNotFoundException(PropertyLookup.getMessage("msg.makestudypublic.nofilefound", study));
                }
            }
            Study biiStudy = studyService.getBiiStudy(study, false);
            biiStudy.setReleaseDate(params.publicReleaseDate);
            biiStudy.setStatus(params.status);
            logger.info("Updating study (database)");
            studyService.update(biiStudy);
            String configPath = UpdateStudyController.class.getClassLoader().getResource("").getPath();
            itu.setDBConfigPath(configPath);
            itu.reindexStudies(study);
            itu.setUnzipFolder(unzipFolder);
            HashMap<String, String> replacementHash = new HashMap<String, String>();
            replacementHash.put("Study Public Release Date", new SimpleDateFormat("yyyy-MM-dd").format(params.publicReleaseDate));
            logger.info("Replacing Study Public Release Date in zip file. with " + params.publicReleaseDate);
            itu.changeStudyFields(study, replacementHash);
            if (params.status == VisibilityStatus.PUBLIC) {
                itu.moveFile(study, VisibilityStatus.PRIVATE);
                String studyPath = itu.getStudyFilePath(study, VisibilityStatus.PUBLIC);
                itu.changeFilePermissions(studyPath, VisibilityStatus.PUBLIC);
            }
            mav.addObject("title", PropertyLookup.getMessage("msg.makestudypublic.ok.title"));
            mav.addObject("message", PropertyLookup.getMessage("msg.makestudypublic.ok.msg"));
            mav.addObject("searchResult", getStudy(study));
            mav.addObject("updated", true);
        } catch (Exception e) {
            String message = "There's been a problem while changing the Public Release date of the study " + study + "\n" + e.getMessage();
            logger.error(message);
            throw new Exception(message);
        }
        return mav;
    }

    /**
	 * Re-submit process:
		Each step must successfully validate, if not then stop and display an error
		no need to allocate a new MTBLS id during this process
		OK - Check that the logged in user owns the chosen study
		OK - Check that the study is PRIVATE (? what do we think ?).  This could stop submitters from "nullifying" a public study.
		OK - Unzip the new zipfile and check that the study id is matching (MTBLS id)
		OK - Update new zipfile with Public date from the resubmission form
		OK - Unload the old study
		OK - IF SUCCESSFULLY UNLOADED =  DO NOT Remove old study zipfile
		OK - IF ERROR = Reupload the old zipfile, DO NOT Remove old study zipfile
		OK - Upload the new study (includes Lucene re-index)
		OK - IF ERROR = Reupload the old zipfile, DO NOT Remove old study zipfile
		OK - Copy the new zipfile to the correct folder (public or private locations)
		OK - Remove old study zipfile
		Display a success or error page to the submitter.  Email metabolights-help and submitter with results
	 * @param file
	 * @param study
	 * @param publicExp
	 * @param publicReleaseDateS
	 * @param request
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value = { "/updatestudy" })
    public ModelAndView updateStudy(@RequestParam("file") MultipartFile file, @RequestParam(required = true, value = "study") String study, @RequestParam(required = false, value = "pickdate") String publicReleaseDateS, HttpServletRequest request) throws Exception {
        logger.info("Starting Updating study " + study);
        RequestParameters params = new RequestParameters(publicReleaseDateS, study, file);
        ModelAndView validation = validateParameters(params);
        if (validation != null) {
            return validation;
        }
        File backup = new File(uploadDirectory + "backup/" + study + ".zip");
        boolean needRestore = false;
        try {
            File isaTabFile = new File(submissionController.writeFile(file, null));
            IsaTabUploader itu = submissionController.getIsaTabUploader(isaTabFile.getAbsolutePath(), params.status, params.publicReleaseDateS);
            Map<String, String> zipValues = itu.getStudyFields(isaTabFile, new String[] { "Study Identifier" });
            String newStudyId = zipValues.get("Study Identifier");
            if (!study.equals(newStudyId)) {
                validation = getModelAndView(study, true);
                validation.addObject("validationmsg", PropertyLookup.getMessage("msg.validation.studyIdDoNotMatch", newStudyId, study));
                return validation;
            }
            if (backup.exists()) {
                throw new Exception(PropertyLookup.getMessage("msg.validation.backupFileExists", study));
            }
            try {
                itu.validate(itu.getUnzipFolder());
            } catch (Exception e) {
                validation = getModelAndView(study, true);
                validation.addObject("validationmsg", PropertyLookup.getMessage("msg.validation.invalid"));
                List<TabLoggingEventWrapper> isaTabLog = itu.getSimpleManager().getLastLog();
                validation.addObject("isatablog", isaTabLog);
                return validation;
            }
            File currentFile = new File(itu.getStudyFilePath(study, VisibilityStatus.PRIVATE));
            FileUtils.copyFile(currentFile, backup);
            logger.info("Deleting previous study " + study);
            itu.unloadISATabFile(study);
            needRestore = true;
            itu.setIsaTabFile(itu.getUnzipFolder());
            logger.info("Uploading new study");
            itu.UploadWithoutIdReplacement(study);
            needRestore = false;
            backup.delete();
            ModelAndView mav = new ModelAndView("updateStudyForm");
            mav.addObject("title", PropertyLookup.getMessage("msg.updatestudy.ok.title", study));
            mav.addObject("message", PropertyLookup.getMessage("msg.updatestudy.ok.msg", study));
            mav.addObject("searchResult", getStudy(study));
            mav.addObject("updated", true);
            return mav;
        } catch (Exception e) {
            if (needRestore) {
                VisibilityStatus oldStatus = params.study.getIsPublic() ? VisibilityStatus.PUBLIC : VisibilityStatus.PRIVATE;
                IsaTabUploader itu = submissionController.getIsaTabUploader(backup.getAbsolutePath(), oldStatus, null);
                itu.UploadWithoutIdReplacement(study);
                backup.delete();
                throw new Exception("There was an error while updating the study. We have restored the previous experiment. " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    /**
	 * Gets the study that has just been published.
	 * @param study
	 * @return
	 * @throws Exception 
	 */
    public LuceneSearchResult getStudy(String study) throws Exception {
        HashMap<Integer, List<LuceneSearchResult>> searchResultHash = new HashMap<Integer, List<LuceneSearchResult>>();
        String luceneQuery = "acc:" + study;
        logger.info("Searching for " + luceneQuery);
        searchResultHash = searchService.search(luceneQuery);
        LuceneSearchResult result = searchResultHash.values().iterator().next().get(0);
        return result;
    }

    /**
	 * Parameters from the request. It parses and validate the parameters.
	 * @author conesa
	 *
	 */
    public class RequestParameters {

        String publicReleaseDateS;

        Date publicReleaseDate;

        VisibilityStatus status;

        MultipartFile file;

        String studyId;

        String validationmsg;

        Boolean isUpdateStudyMode;

        LuceneSearchResult study;

        MetabolightsUser user;

        /**
		 * 
		 * @param publicReleaseDateS: Should be in a "dd/mm/yyyy" format
		 * @param studyId
		 * @throws Exception
		 */
        public RequestParameters(String publicReleaseDateS, String studyId) throws Exception {
            if (publicReleaseDateS == null) {
                this.publicReleaseDateS = "";
            } else {
                this.publicReleaseDateS = publicReleaseDateS;
            }
            this.studyId = studyId;
            this.study = getStudy(studyId);
            this.isUpdateStudyMode = false;
            this.user = (MetabolightsUser) (SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        }

        public RequestParameters(String publicReleaseDateS, String study, MultipartFile file) throws Exception {
            this(publicReleaseDateS, study);
            this.isUpdateStudyMode = true;
            this.file = file;
        }

        /**
		 * Validate the parameters
		 * @return
		 */
        public String validate() {
            validationmsg = "";
            if (publicReleaseDateS.isEmpty()) {
                validationmsg = PropertyLookup.getMessage("msg.makestudypublic.daterequired");
            }
            if (isUpdateStudyMode && file.isEmpty()) {
                validationmsg = validationmsg + PropertyLookup.getMessage("msg.upload.notValid");
            }
            if (!study.getSubmitter().getUserName().equals(user.getUserName())) {
                validationmsg = validationmsg + PropertyLookup.getMessage("msg.validation.studynotowned");
            }
            if (study.getIsPublic()) {
                validationmsg = validationmsg + PropertyLookup.getMessage("msg.validation.publicstudynoteditable");
            }
            return validationmsg;
        }

        public void calculateStatusAndDate() throws ParseException {
            status = VisibilityStatus.PRIVATE;
            publicReleaseDate = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            if (!publicReleaseDateS.isEmpty()) {
                publicReleaseDate = new SimpleDateFormat("dd-MMM-yyyy").parse(publicReleaseDateS);
                if (publicReleaseDate.before(new Date())) {
                    status = VisibilityStatus.PUBLIC;
                }
            }
        }
    }
}
