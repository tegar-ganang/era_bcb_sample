package org.openuss.wiki;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.openuss.documents.DocumentApplicationException;
import org.openuss.documents.FileInfo;
import org.openuss.documents.FolderEntryInfo;
import org.openuss.documents.FolderInfo;
import org.openuss.lecture.CourseInfo;
import org.openuss.lecture.CourseMemberInfo;
import org.openuss.lecture.InstituteInfo;
import org.openuss.lecture.InstituteMember;
import org.openuss.security.Roles;
import org.openuss.security.User;
import org.openuss.security.UserInfo;
import org.openuss.security.acl.LectureAclEntry;

/**
 * @author  Projektseminar WS 07/08, Team Collaboration
 * @see org.openuss.wiki.WikiService
 * 
 */
public class WikiServiceImpl extends org.openuss.wiki.WikiServiceBase {

    private static final Logger LOGGER = Logger.getLogger(WikiServiceImpl.class);

    private static final String WIKI_STARTSITE_NAME = "index";

    private static final String IMPORT_IMAGE_TEMPLATE_SEARCH = "<img(.+)src=\"(.*)%s\\?fileid=%s\"(.*)/>";

    private static final String IMPORT_IMAGE_TEMPLATE_REPLACE = "<img$1src=\"$2%s\\?fileid=%s\"$3/>";

    private static final String DEFAULT_WIKI_INDEX_SITE_TEXT = "<h1>Default Wiki Index Site</h1>";

    @Override
    protected void handleDeleteWikiSite(Long wikiSiteId) {
        Validate.notNull(wikiSiteId, "Parameter wikiSiteId must not be null!");
        getWikiSiteDao().remove(wikiSiteId);
    }

    @Override
    protected void handleDeleteWikiSiteVersion(Long wikiSiteVersionId) {
        final WikiSiteVersion version = getWikiSiteVersionDao().load(wikiSiteVersionId);
        final WikiSite site = version.getWikiSite();
        getWikiSiteVersionDao().remove(version);
        site.getWikiSiteVersions().remove(version);
        if (site.getWikiSiteVersions().isEmpty()) {
            getWikiSiteDao().remove(site);
        }
    }

    @Override
    protected WikiSiteContentInfo handleFindWikiSiteContentByDomainObjectAndName(Long domainId, String siteName) {
        Validate.notNull(domainId, "Parameter domainId must not be null!");
        Validate.notNull(siteName, "Parameter siteName must not be null!");
        final WikiSiteInfo wikiSite = (WikiSiteInfo) getWikiSiteDao().findByDomainIdAndName(WikiSiteDao.TRANSFORM_WIKISITEINFO, domainId, siteName);
        WikiSiteContentInfo wikiSiteContent = null;
        if (wikiSite != null) {
            wikiSiteContent = handleGetNewestWikiSiteContent(wikiSite.getWikiSiteId());
        }
        if ((wikiSiteContent == null) && (WIKI_STARTSITE_NAME.equals(siteName))) {
            wikiSiteContent = createInfoIndexSite(domainId);
        }
        return wikiSiteContent;
    }

    /**
	 * Creates a localized default Index Page.
	 * @param domainId Domain ID of the specified courses.
	 * @return Created localized default Index WikiSite.
	 */
    private WikiSiteContentInfo createInfoIndexSite(Long domainId) {
        final UserInfo user = getSecurityService().getCurrentUser();
        final Locale locale = new Locale(user.getLocale());
        final String country = locale.getLanguage();
        InputStream inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("wiki_index_" + country + ".xhtml");
        if (inStream == null) {
            inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("wiki_index.xhtml");
        }
        if (inStream == null) {
            inStream = new ByteArrayInputStream(DEFAULT_WIKI_INDEX_SITE_TEXT.getBytes());
        }
        if (inStream != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                IOUtils.copyLarge(inStream, out);
                return createIndexVersion(domainId, out.toString(), user);
            } catch (IOException exception) {
                LOGGER.error("Error creating info page.", exception);
            } finally {
                try {
                    inStream.close();
                    out.close();
                } catch (IOException exception) {
                    LOGGER.error("Error reading wiki_index.xhtml", exception);
                }
            }
        }
        return null;
    }

    /**
	 * Creates a localized default Index Page Version.
	 * @param domainId Domain ID of the specified courses.
	 * @param text Localized Version Text.
	 * @param user Current User.
	 * @return Created localized default Index WikiSite.
	 */
    private WikiSiteContentInfo createIndexVersion(Long domainId, String text, UserInfo user) {
        final WikiSiteContentInfo siteVersionInfo = new WikiSiteContentInfo();
        siteVersionInfo.setId(null);
        siteVersionInfo.setName(WIKI_STARTSITE_NAME);
        siteVersionInfo.setText(text);
        siteVersionInfo.setCreationDate(new Date());
        siteVersionInfo.setAuthorId(user.getId());
        siteVersionInfo.setDomainId(domainId);
        siteVersionInfo.setDeleted(false);
        siteVersionInfo.setReadOnly(false);
        siteVersionInfo.setStable(false);
        saveWikiSite(siteVersionInfo);
        return siteVersionInfo;
    }

    /** 
	 * @return List of WikiSiteInfo
	 */
    @SuppressWarnings("unchecked")
    @Override
    protected List handleFindWikiSiteVersionsByWikiSite(Long wikiSiteId) {
        Validate.notNull(wikiSiteId, "Parameter wikiSiteId must not be null!");
        final WikiSite wikiSite = getWikiSiteDao().load(wikiSiteId);
        return getWikiSiteVersionDao().findByWikiSite(WikiSiteVersionDao.TRANSFORM_WIKISITEINFO, wikiSite);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List handleFindWikiSitesByDomainObject(Long domainId) {
        Validate.notNull(domainId, "Parameter domainId must not be null!");
        return getWikiSiteDao().findByDomainId(WikiSiteDao.TRANSFORM_WIKISITEINFO, domainId);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected WikiSiteContentInfo handleGetNewestWikiSiteContent(Long wikiSiteId) {
        Validate.notNull(wikiSiteId, "Parameter wikiSiteId must not be null!");
        final WikiSite site = getWikiSiteDao().load(wikiSiteId);
        Validate.notNull(site, "No wikiSite found for wikiSiteId:" + wikiSiteId);
        final String query = "from org.openuss.wiki.WikiSiteVersion as f where f.wikiSite = ? order by f.creationDate desc";
        final List<WikiSiteInfo> list = getWikiSiteVersionDao().findByWikiSite(WikiSiteVersionDao.TRANSFORM_WIKISITEINFO, query, site);
        if (list.isEmpty()) {
            return null;
        } else {
            final WikiSiteInfo wikiSite = list.get(0);
            return (WikiSiteContentInfo) getWikiSiteVersionDao().load(WikiSiteVersionDao.TRANSFORM_WIKISITECONTENTINFO, wikiSite.getId());
        }
    }

    @Override
    protected WikiSiteInfo handleGetWikiSite(Long wikiSiteId) {
        Validate.notNull(wikiSiteId, "Parameter wikiSiteId must not be null!");
        return (WikiSiteInfo) getWikiSiteDao().load(WikiSiteDao.TRANSFORM_WIKISITEINFO, wikiSiteId);
    }

    @Override
    protected WikiSiteContentInfo handleGetWikiSiteContent(Long wikiSiteVersionId) {
        Validate.notNull(wikiSiteVersionId, "Parameter wikiSiteId must not be null!");
        return (WikiSiteContentInfo) getWikiSiteVersionDao().load(WikiSiteVersionDao.TRANSFORM_WIKISITECONTENTINFO, wikiSiteVersionId);
    }

    @Override
    protected void handleSaveWikiSite(WikiSiteInfo wikiSiteInfo) {
        Validate.notNull(wikiSiteInfo, "Parameter wikiSiteInfo cannot be null.");
        Validate.notNull(wikiSiteInfo.getWikiSiteId(), "getWikiSiteId cannot be null.");
        final WikiSite wikiSite = getWikiSiteDao().wikiSiteInfoToEntity(wikiSiteInfo);
        wikiSite.setId(wikiSiteInfo.getWikiSiteId());
        getWikiSiteDao().update(wikiSite);
    }

    @Override
    protected void handleSaveWikiSite(WikiSiteContentInfo wikiSiteContentInfo) {
        Validate.notNull(wikiSiteContentInfo, "Parameter wikiSiteContentInfo cannot be null.");
        Validate.notNull(wikiSiteContentInfo.getDomainId(), "getDomainId cannot be null.");
        if (wikiSiteContentInfo.getId() != null) {
            saveWikiSiteUpdate(wikiSiteContentInfo);
        } else {
            saveWikiSiteCreate(wikiSiteContentInfo);
        }
    }

    /**
	 * Creates a WikiSite.
	 * @param wikiSiteContentInfo Info-Object with the information for the WikiSite
	 */
    private void saveWikiSiteCreate(WikiSiteContentInfo wikiSiteContentInfo) {
        WikiSite wikiSite = getWikiSiteDao().findByDomainIdAndName(wikiSiteContentInfo.getDomainId(), wikiSiteContentInfo.getName());
        if (wikiSite != null) {
            wikiSiteContentInfo.setWikiSiteId(wikiSite.getId());
        }
        if (wikiSiteContentInfo.getWikiSiteId() != null) {
            wikiSite = getWikiSiteDao().load(wikiSiteContentInfo.getWikiSiteId());
            Validate.notNull(wikiSite, "Cannot find wikiSite for id: " + wikiSiteContentInfo.getWikiSiteId());
        } else {
            wikiSite = this.getWikiSiteDao().wikiSiteInfoToEntity(wikiSiteContentInfo);
            Validate.notNull(wikiSite, "Cannot transform wikiSiteInfo to entity.");
            this.getWikiSiteDao().create(wikiSite);
            Validate.notNull(wikiSite, "Id of wikiSite cannot be null.");
            getSecurityService().createObjectIdentity(wikiSite, null);
        }
        final WikiSiteVersion wikiSiteVersion = getWikiSiteVersionDao().wikiSiteContentInfoToEntity(wikiSiteContentInfo);
        final User author = getUserDao().load(getSecurityService().getCurrentUser().getId());
        wikiSiteVersion.setAuthor(author);
        getWikiSiteVersionDao().create(wikiSiteVersion);
        wikiSite.getWikiSiteVersions().add(wikiSiteVersion);
        wikiSiteVersion.setWikiSite(wikiSite);
        getWikiSiteDao().update(wikiSite);
        getWikiSiteVersionDao().update(wikiSiteVersion);
        wikiSiteContentInfo.setId(wikiSiteVersion.getId());
        wikiSiteContentInfo.setWikiSiteId(wikiSite.getId());
        getSecurityService().createObjectIdentity(wikiSiteVersion, wikiSite);
    }

    /**
	 * Updates a WikiSite.
	 * @param wikiSiteContentInfo Info-Object with the information for the WikiSite
	 */
    private void saveWikiSiteUpdate(WikiSiteContentInfo wikiSiteContentInfo) {
        final WikiSiteVersion wikiSiteVersion = getWikiSiteVersionDao().wikiSiteContentInfoToEntity(wikiSiteContentInfo);
        getWikiSiteVersionDao().update(wikiSiteVersion);
    }

    @Override
    protected void handleDeleteImage(Long fileId) throws DocumentApplicationException {
        Validate.notNull(fileId, "Parameter fileId cannot be null.");
        getDocumentService().removeFolderEntry(fileId);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List handleFindImagesByDomainId(Long domainId) {
        Validate.notNull(domainId, "Parameter domainId cannot be null.");
        final WikiSite indexSite = getWikiSiteDao().findByDomainIdAndName(domainId, "index");
        return getDocumentService().getFolderEntries(indexSite, null);
    }

    @Override
    protected void handleSaveImage(final WikiSiteInfo wikiSiteInfo, final FileInfo image) throws DocumentApplicationException {
        Validate.notNull(wikiSiteInfo, "Parameter wikiSiteInfo cannot be null.");
        Validate.notNull(image, "Parameter image cannot be null.");
        this.handleSaveImage(wikiSiteInfo.getDomainId(), image);
    }

    /**
	 * Saves an Image.
	 * @param domainId ID of the Wiki.
	 * @param image Image to be saved.
	 * @throws DocumentApplicationException
	 */
    private void handleSaveImage(Long domainId, FileInfo image) throws DocumentApplicationException {
        final WikiSite indexSite = getWikiSiteDao().findByDomainIdAndName(domainId, "index");
        final FolderInfo folder = getDocumentService().getFolder(indexSite);
        getDocumentService().createFileEntry(image, folder);
        getSecurityService().setPermissions(Roles.ANONYMOUS, image, LectureAclEntry.READ);
        getSecurityService().setPermissions(Roles.USER, image, LectureAclEntry.READ);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected WikiSiteContentInfo handleGetNewestStableWikiSiteContent(Long wikiSiteId) {
        Validate.notNull(wikiSiteId, "Parameter wikiSiteId must not be null!");
        final WikiSite site = getWikiSiteDao().load(wikiSiteId);
        Validate.notNull(site, "No wikiSite found for wikiSiteId:" + wikiSiteId);
        final String query = "from org.openuss.wiki.WikiSiteVersion as f where f.wikiSite = ? and f.stable = true order by f.creationDate desc";
        final List<WikiSiteInfo> list = getWikiSiteVersionDao().findByWikiSite(WikiSiteVersionDao.TRANSFORM_WIKISITEINFO, query, site);
        if (list.isEmpty()) {
            return null;
        } else {
            final WikiSiteInfo wikiSite = list.get(0);
            return (WikiSiteContentInfo) getWikiSiteVersionDao().load(WikiSiteVersionDao.TRANSFORM_WIKISITECONTENTINFO, wikiSite.getId());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleImportWikiSites(Long importDomainId, Long exportDomainId) throws DocumentApplicationException {
        Validate.notNull(importDomainId, "Parameter importDomainId must not be null!");
        Validate.notNull(exportDomainId, "Parameter exportDomainId must not be null!");
        deleteAllWikiSites(importDomainId);
        final List<WikiSiteInfo> exportWikiSites = findWikiSitesByDomainObject(exportDomainId);
        final List<WikiSiteContentInfo> importWikiSites = new LinkedList<WikiSiteContentInfo>();
        for (WikiSiteInfo exportWikiSite : exportWikiSites) {
            final WikiSiteContentInfo newestWikiSiteContent = getNewestWikiSiteContent(exportWikiSite.getWikiSiteId());
            final WikiSiteContentInfo importWikiSiteContent = importWikiSiteContent(importDomainId, newestWikiSiteContent);
            importWikiSites.add(importWikiSiteContent);
        }
        importWikiSiteImages(importDomainId, importWikiSites, exportDomainId);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleImportWikiVersions(Long importDomainId, Long exportDomainId) throws DocumentApplicationException {
        Validate.notNull(importDomainId, "Parameter importDomainId must not be null!");
        Validate.notNull(exportDomainId, "Parameter exportDomainId must not be null!");
        deleteAllWikiSites(importDomainId);
        final List<WikiSiteInfo> exportWikiSites = findWikiSitesByDomainObject(exportDomainId);
        final List<WikiSiteContentInfo> importWikiSites = new LinkedList<WikiSiteContentInfo>();
        for (WikiSiteInfo exportWikiSite : exportWikiSites) {
            final List<WikiSiteInfo> exportWikiSiteVersions = findWikiSiteVersionsByWikiSite(exportWikiSite.getWikiSiteId());
            for (WikiSiteInfo exportWikiSiteVersion : exportWikiSiteVersions) {
                final WikiSiteContentInfo exportWikiSiteVersionContent = getWikiSiteContent(exportWikiSiteVersion.getId());
                final WikiSiteContentInfo importWikiSiteContent = importWikiSiteContent(importDomainId, exportWikiSiteVersionContent);
                importWikiSites.add(importWikiSiteContent);
            }
        }
        importWikiSiteImages(importDomainId, importWikiSites, exportDomainId);
    }

    /**
	 * Deletes all WikiSites and corresponding Images that refer to a specific DomainObject. 
	 * @param deleteDomainId ID of the specific DomainObject.
	 */
    @SuppressWarnings("unchecked")
    private void deleteAllWikiSites(Long deleteDomainId) {
        final List<WikiSiteInfo> oldImportWikiSites = findWikiSitesByDomainObject(deleteDomainId);
        for (WikiSiteInfo oldImportWikiSite : oldImportWikiSites) {
            deleteWikiSite(oldImportWikiSite.getWikiSiteId());
        }
    }

    /**
	 * Clones existing WikiSite and imports it to a specific DomainObject.
	 * @param importDomainId ID of the specific import DomainObject.
	 * @param exportWikiSiteContent Exported WikiSiteContentInfo.
	 */
    private WikiSiteContentInfo importWikiSiteContent(Long importDomainId, WikiSiteContentInfo exportWikiSiteContent) {
        final WikiSiteContentInfo importWikiSiteContent = new WikiSiteContentInfo(exportWikiSiteContent);
        importWikiSiteContent.setId(null);
        importWikiSiteContent.setWikiSiteId(null);
        importWikiSiteContent.setDomainId(importDomainId);
        saveWikiSite(importWikiSiteContent);
        return importWikiSiteContent;
    }

    /**
	 * Clones existing Images and imports it to a specific DomainObject.
	 * @param importDomainId ID of the specific import DomainObject.
	 * @param importWikiSites List of all imported WikiSiteContentInfo objects.
	 * @param exportDomainId ID of the specific export DomainObject.
	 * @throws DocumentApplicationException
	 */
    @SuppressWarnings("unchecked")
    private void importWikiSiteImages(Long importDomainId, List<WikiSiteContentInfo> importWikiSites, Long exportDomainId) throws DocumentApplicationException {
        final List<ImageImportAllocation> imageImportAllocations = new LinkedList<ImageImportAllocation>();
        final List<FolderEntryInfo> imageFolderEntries = findImagesByDomainId(exportDomainId);
        for (FolderEntryInfo imageFolderEntry : imageFolderEntries) {
            final FileInfo imageFile = getDocumentService().getFileEntry(imageFolderEntry.getId(), true);
            final Long exportImageId = imageFile.getId();
            imageFile.setId(null);
            handleSaveImage(importDomainId, imageFile);
            final Long importImageId = imageFile.getId();
            final ImageImportAllocation imageImportAllocation = new ImageImportAllocation(importImageId, exportImageId, imageFile.getFileName());
            imageImportAllocations.add(imageImportAllocation);
        }
        for (WikiSiteContentInfo importWikiSite : importWikiSites) {
            updateWikiSiteImages(importWikiSite, imageImportAllocations);
        }
    }

    /**
	 * Replaces old Image IDs of a imported WikiSiteContentInfo object by the Image IDs of the imported Images.
	 * @param wikiSiteContent Imported WikiSiteContentInfo.
	 * @param imageImportAllocations List of ImageImportAllocations.
	 */
    private void updateWikiSiteImages(WikiSiteContentInfo wikiSiteContent, List<ImageImportAllocation> imageImportAllocations) {
        String newContent = wikiSiteContent.getText();
        for (ImageImportAllocation imageImportAllocation : imageImportAllocations) {
            final String searchString = String.format(IMPORT_IMAGE_TEMPLATE_SEARCH, imageImportAllocation.getFilename(), imageImportAllocation.getExportId());
            final String replaceString = String.format(IMPORT_IMAGE_TEMPLATE_REPLACE, imageImportAllocation.getFilename(), imageImportAllocation.getImportId());
            final Pattern searchPattern = Pattern.compile(searchString);
            final Matcher matcher = searchPattern.matcher(newContent);
            newContent = matcher.replaceAll(replaceString);
        }
        wikiSiteContent.setText(newContent);
        saveWikiSiteUpdate(wikiSiteContent);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected WikiSiteInfo handleGetNewestWikiSite(Long wikiSiteId) {
        Validate.notNull(wikiSiteId, "Parameter wikiSiteId must not be null!");
        final WikiSite site = getWikiSiteDao().load(wikiSiteId);
        Validate.notNull(site, "No wikiSite found for wikiSiteId:" + wikiSiteId);
        String query = "from org.openuss.wiki.WikiSiteVersion as f where f.wikiSite = ? order by f.creationDate desc";
        List<WikiSiteInfo> list = getWikiSiteVersionDao().findByWikiSite(WikiSiteVersionDao.TRANSFORM_WIKISITEINFO, query, site);
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List handleFindAllExportableWikiCoursesByInstituteAndUser(InstituteInfo institute, UserInfo user, CourseInfo importCourse) {
        Validate.notNull(institute, "Parameter institute must not be null!");
        Validate.notNull(institute.getId(), "Parameter institute.getId() must not be null!");
        Validate.notNull(user, "Parameter user must not be null!");
        Validate.notNull(user.getId(), "Parameter user.getId must not be null!");
        Validate.notNull(importCourse, "Parameter importCourse must not be null!");
        Validate.notNull(importCourse.getId(), "Parameter importCourse.getId must not be null!");
        final List<CourseInfo> availableCourses = this.getCourseService().findAllCoursesByInstitute(institute.getId());
        availableCourses.remove(importCourse);
        if (userIsInstituteMember(institute, user)) {
            return removeNoWiki(availableCourses);
        }
        return removeNoWiki(findAssistantCourses(availableCourses, user));
    }

    /**
	 * Checks if a User is Member of an Institute.
	 * @param institute Specified Institute.
	 * @param user Specified User.
	 * @return <code>true</code> if the User is Member of the Institute, otherwise <code>false</code>.
	 */
    private boolean userIsInstituteMember(InstituteInfo institute, UserInfo user) {
        final List<InstituteMember> instituteMembers = getInstituteService().getInstituteSecurity(institute.getId()).getMembers();
        for (InstituteMember instituteMember : instituteMembers) {
            if (user.getId().equals(instituteMember.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Finds all Courses a User is Assistant of.
	 * @param availableCourses List of checkable Courses.
	 * @param user Specified User.
	 * @return List of all Courses the User is Assistant of.
	 */
    @SuppressWarnings("unchecked")
    private List<CourseInfo> findAssistantCourses(List<CourseInfo> availableCourses, UserInfo user) {
        final List<CourseInfo> assistantCourses = new LinkedList<CourseInfo>();
        for (CourseInfo availableCourse : availableCourses) {
            final List<CourseMemberInfo> assistants = getCourseService().getAssistants(availableCourse);
            for (CourseMemberInfo assistant : assistants) {
                if (assistant.getUserId().equals(user.getId())) {
                    assistantCourses.add(availableCourse);
                }
            }
        }
        return assistantCourses;
    }

    /**
	 * Revoves all Courses that do not have a Wiki.
	 * @param availableCourses List of courses to check.
	 * @return List of Courses that have a Wiki.
	 */
    private List<CourseInfo> removeNoWiki(List<CourseInfo> availableCourses) {
        final List<CourseInfo> coursesWithWiki = new LinkedList<CourseInfo>();
        for (CourseInfo availableCourse : availableCourses) {
            if (availableCourse.isWiki()) {
                WikiSiteContentInfo version = this.findWikiSiteContentByDomainObjectAndName(availableCourse.getId(), WIKI_STARTSITE_NAME);
                if (version != null) {
                    coursesWithWiki.add(availableCourse);
                }
            }
        }
        return coursesWithWiki;
    }

    /**
	 * Bean for encapsulation of Information regarding Import of Images and Update of References.
	 * @author  Projektseminar WS 07/08, Team Collaboration
	 * @see org.openuss.wiki.WikiService
	 * 
	 */
    private static class ImageImportAllocation {

        private final Long importId;

        private final Long exportId;

        private final String filename;

        public ImageImportAllocation(final Long importId, final Long exportId, final String filename) {
            this.importId = importId;
            this.exportId = exportId;
            this.filename = filename;
        }

        /**
		 * Returns ID of the imported Image.
		 * @return ID of the imported Image.
		 */
        public Long getImportId() {
            return importId;
        }

        /**
		 * Returns ID of the exported Image.
		 * @return ID of the exported Image.
		 */
        public Long getExportId() {
            return exportId;
        }

        /**
		 * Returns Filename of the imported Image.
		 * @return Filename of the imported Image.
		 */
        public String getFilename() {
            return filename;
        }
    }
}
