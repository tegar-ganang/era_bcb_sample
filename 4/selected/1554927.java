package uk.ac.ebi.metabolights.controller;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ebi.bioinvindex.model.AssayGroup;
import uk.ac.ebi.bioinvindex.model.AssayResult;
import uk.ac.ebi.bioinvindex.model.Study;
import uk.ac.ebi.bioinvindex.model.VisibilityStatus;
import uk.ac.ebi.bioinvindex.model.processing.Assay;
import uk.ac.ebi.bioinvindex.model.security.User;
import uk.ac.ebi.bioinvindex.model.term.FactorValue;
import uk.ac.ebi.bioinvindex.model.term.PropertyValue;
import uk.ac.ebi.metabolights.model.MLAssay;
import uk.ac.ebi.metabolights.model.MetabolightsUser;
import uk.ac.ebi.metabolights.properties.PropertyLookup;
import uk.ac.ebi.metabolights.service.SearchService;
import uk.ac.ebi.metabolights.service.StudyService;
import uk.ac.ebi.metabolights.service.TextTaggerService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

/**
 * Controller for entry (=study) details.
 * 
 */
@Controller
public class EntryController extends AbstractController {

    private static Logger logger = Logger.getLogger(EntryController.class);

    private final String DESCRIPTION = "descr";

    @Autowired
    private StudyService studyService;

    @Autowired
    private SearchService searchService;

    @Value("#{appProperties.publicFtpLocation}")
    private String publicFtpDirectory;

    @Value("#{appProperties.privateFtpStageLocation}")
    private String privateFtpDirectory;

    @RequestMapping(value = "/{metabolightsId}")
    public ModelAndView showEntry(@PathVariable("metabolightsId") String mtblId, HttpServletRequest request) {
        logger.info("requested entry " + mtblId);
        Study study = null;
        try {
            request.setCharacterEncoding("UTF-8");
            study = studyService.getBiiStudy(mtblId, true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (study.getAcc() == null || study.getAcc().equals("Error") || study.getAcc().equals(VisibilityStatus.PRIVATE.toString())) return new ModelAndView("index", "message", PropertyLookup.getMessage("msg.noStudyFound") + " (" + mtblId + ")");
        Collection<String> organismNames = new TreeSet<String>();
        for (AssayResult assRes : study.getAssayResults()) {
            for (PropertyValue<?> pv : assRes.getCascadedPropertyValues()) {
                if (pv.getType().getValue().equals("organism")) {
                    organismNames.add(pv.getValue());
                }
            }
        }
        String ftpLocation = getDownloadLink(study.getAcc(), study.getStatus());
        ModelAndView mav = new ModelAndView("entry");
        mav.addObject("study", study);
        mav.addObject("organismNames", organismNames);
        mav.addObject("factors", getFactorsSummary(study));
        mav.addObject("assays", getMLAssays(study));
        if (!study.getAcc().equals(VisibilityStatus.PRIVATE.toString())) mav.addObject("ftpLocation", ftpLocation);
        if (study.getDescription() != null) {
            logger.debug("placing study description in session for Ajax highlighting");
            request.getSession().setAttribute(DESCRIPTION, study.getDescription());
        }
        return mav;
    }

    private Collection<MLAssay> getMLAssays(Study study) {
        HashMap<String, MLAssay> mlAssays = new HashMap<String, MLAssay>();
        for (AssayResult ar : study.getAssayResults()) {
            MLAssay mlAssay = null;
            for (Assay assay : ar.getAssays()) {
                String assayName = MLAssay.getAssayNameFromAssay(assay);
                if (!mlAssays.containsKey(assayName)) {
                    mlAssay = new MLAssay(assay);
                    mlAssays.put(assayName, mlAssay);
                } else {
                    mlAssay = mlAssays.get(assayName);
                }
                mlAssay.addAssayLines(assay);
            }
            mlAssay.addAssayResult(ar);
            if (ar.getAssays().size() != 1) logger.info("*****WARNING:Assays collection's size is different from 1, we expect to have a one-to-one relationship!!!: " + ar.getAssays().size());
        }
        for (AssayGroup ag : study.getAssayGroups()) {
            MLAssay mlAssay = mlAssays.get(ag.getFileName());
            if (mlAssay == null) {
                logger.info("Oh! Not MLAssay forund for AssayGroup: " + ag.getFileName());
            } else {
                mlAssay.setAssayGroup(ag);
            }
        }
        return mlAssays.values();
    }

    @RequestMapping(value = "/privatefiles/{file_name}")
    public void getFile(@PathVariable("file_name") String fileName, HttpServletResponse response, Principal principal) {
        try {
            Boolean validUser = false;
            final String currentUser = principal.getName();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!auth.getPrincipal().equals(new String("anonymousUser"))) {
                MetabolightsUser metabolightsUser = (MetabolightsUser) auth.getPrincipal();
                if (metabolightsUser != null && metabolightsUser.isCurator()) validUser = true;
            }
            if (currentUser != null) {
                Study study = studyService.getBiiStudy(fileName, true);
                Collection<User> users = study.getUsers();
                Iterator<User> iter = users.iterator();
                while (iter.hasNext()) {
                    User user = iter.next();
                    if (user.getUserName().equals(currentUser)) {
                        validUser = true;
                        break;
                    }
                }
            }
            if (!validUser) throw new RuntimeException(PropertyLookup.getMessage("Entry.notAuthorised"));
            try {
                InputStream is = new FileInputStream(privateFtpDirectory + fileName + ".zip");
                response.setContentType("application/zip");
                IOUtils.copy(is, response.getOutputStream());
            } catch (Exception e) {
                throw new RuntimeException(PropertyLookup.getMessage("Entry.fileMissing"));
            }
            response.flushBuffer();
        } catch (IOException ex) {
            logger.info("Error writing file to output stream. Filename was '" + fileName + "'");
            throw new RuntimeException("IOError writing file to output stream");
        }
    }

    @Autowired
    private TextTaggerService textTagger;

    /**
     * Intended for an asynchronous call, to tag the description in a study.
     * @param request
     * @return ugly pixie 
     */
    @RequestMapping(value = "/tagText")
    public ModelAndView whatIzItStuff(HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("taggedText");
        String description = (String) request.getSession().getAttribute(DESCRIPTION);
        if (description != null) {
            logger.debug("Calling WhatWhatIzIt");
            String taggedContent = textTagger.tagText(description);
            mav.addObject("taggedContent", taggedContent);
            logger.debug(taggedContent);
        } else mav.addObject("taggedContent", null);
        return mav;
    }

    public String getDownloadLink(String study, VisibilityStatus status) {
        String ftpLocation = null;
        if (status.equals(VisibilityStatus.PRIVATE)) {
            ftpLocation = "privatefiles/" + study;
        } else {
            ftpLocation = publicFtpDirectory.replaceFirst("/ebi/ftp/", "ftp://ftp.ebi.ac.uk/") + study + ".zip";
        }
        return ftpLocation;
    }

    private HashMap<String, ArrayList<String>> getFactorsSummary(Study study) {
        HashMap<String, ArrayList<String>> fvSummary = new HashMap<String, ArrayList<String>>();
        for (AssayResult ar : study.getAssayResults()) {
            for (FactorValue fv : ar.getData().getFactorValues()) {
                if (!fvSummary.containsKey(fv.getType().getValue())) {
                    fvSummary.put(fv.getType().getValue(), new ArrayList<String>());
                }
                ArrayList<String> values = fvSummary.get(fv.getType().getValue());
                String value;
                if (!(fv.getUnit() == null)) {
                    value = fv.getValue() + " " + fv.getUnit().getValue();
                } else {
                    value = fv.getValue();
                }
                if (!values.contains(value)) values.add(value);
            }
        }
        return fvSummary;
    }
}
