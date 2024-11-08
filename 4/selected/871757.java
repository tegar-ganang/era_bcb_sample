package dk.i2m.converge.ejb.facades;

import dk.i2m.converge.core.Notification;
import dk.i2m.converge.core.content.NewsItemMediaAttachment;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.content.catalogue.*;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireItemAttachment;
import dk.i2m.converge.core.plugin.CatalogueEvent;
import dk.i2m.converge.core.plugin.CatalogueEventException;
import dk.i2m.converge.core.plugin.CatalogueHook;
import dk.i2m.converge.core.search.QueueEntryOperation;
import dk.i2m.converge.core.search.QueueEntryType;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.utils.StringUtils;
import dk.i2m.converge.ejb.services.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import org.apache.commons.io.FilenameUtils;

/**
 * Stateless enterprise java bean providing a facade for interacting with
 * {@link Catalogue}s.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class CatalogueFacadeBean implements CatalogueFacadeLocal {

    private static final Logger LOG = Logger.getLogger(CatalogueFacadeBean.class.getName());

    @EJB
    private DaoServiceLocal daoService;

    @EJB
    private SearchEngineLocal searchEngine;

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private PluginContextBeanLocal pluginContext;

    @EJB
    private MetaDataServiceLocal metaDataService;

    @EJB
    private SystemFacadeLocal systemFacade;

    @EJB
    private NotificationServiceLocal notificationService;

    @Resource
    private SessionContext ctx;

    /**
     * Creates a new {@link Catalogue} in the database.
     *
     * @param catalogue {@link Catalogue} to create
     * @return {@link Catalogue} created with auto-generated properties set
     */
    @Override
    public Catalogue create(Catalogue catalogue) {
        return daoService.create(catalogue);
    }

    /**
     * Updates an existing {@link Catalogue} in the database.
     *
     * @param catalogue {@link Catalogue} to update
     * @return Updated {@link Catalogue}
     */
    @Override
    public Catalogue update(Catalogue catalogue) {
        return daoService.update(catalogue);
    }

    /**
     * Deletes an existing {@link Catalogue} from the database.
     *
     * @param id Unique identifier of the {@link Catalogue}
     * @throws DataNotFoundException If the given {@link Catalogue} does not exist
     */
    @Override
    public void deleteCatalogueById(Long id) throws DataNotFoundException {
        daoService.delete(Catalogue.class, id);
    }

    /**
     * Finds all {@link Catalogue}s in the database.
     *
     * @return {@link List} of all {@link Catalogue}s in the database
     */
    @Override
    public List<Catalogue> findAllCatalogues() {
        return daoService.findAll(Catalogue.class);
    }

    /**
     * Finds a {@link List} of all enabled and writable {@link Catalogue}s.
     *
     * @return {@link List} of enabled writable {@link Catalogue}s
     */
    @Override
    public List<Catalogue> findWritableCatalogues() {
        return daoService.findWithNamedQuery(Catalogue.FIND_WRITABLE);
    }

    /**
     * Finds an existing {@link Catalogue} in the database.
     *
     * @param id Unique identifier of the {@link Catalogue}
     * @return {@link Catalogue} matching the given {@code id}
     * @throws DataNotFoundException If no {@link Catalogue} could be matched to the given {@code id}
     */
    @Override
    public Catalogue findCatalogueById(Long id) throws DataNotFoundException {
        return daoService.findById(Catalogue.class, id);
    }

    /**
     * Indexes enabled {@link Catalogue}s.
     *
     * @throws InvalidMediaRepositoryException  If the location of the {@link Catalogue} is not valid
     * @throws MediaRepositoryIndexingException If the location of the {@link Catalogue} could not be indexed
     */
    @Override
    public void indexCatalogues() throws InvalidMediaRepositoryException, MediaRepositoryIndexingException {
        Map<String, Object> parameters = QueryBuilder.with("status", MediaItemStatus.APPROVED).parameters();
        List<MediaItem> items = daoService.findWithNamedQuery(MediaItem.FIND_BY_STATUS, parameters);
        for (MediaItem item : items) {
            searchEngine.addToIndexQueue(QueueEntryType.MEDIA_ITEM, item.getId(), QueueEntryOperation.UPDATE);
        }
    }

    /**
     * Finds a {@link List} of {@link Rendition}s.
     *
     * @return {@link List} of {@link Rendition}s
     */
    @Override
    public List<Rendition> findRenditions() {
        return daoService.findAll(Rendition.class);
    }

    /** {@inheritDoc } */
    @Override
    public Rendition findRenditionById(Long id) throws DataNotFoundException {
        return daoService.findById(Rendition.class, id);
    }

    /**
     * Finds a {@link Rendition} by its name.
     * <p/>
     * @param name Name of the {@link Rendition}
     * @return {@link Rendition} matching the name
     * @throws DataNotFoundException * If the {@link Rendition} could not be found
     */
    @Override
    public Rendition findRenditionByName(String name) throws DataNotFoundException {
        Map<String, Object> params = QueryBuilder.with("name", name).parameters();
        List<Rendition> results = daoService.findWithNamedQuery(Rendition.FIND_BY_NAME, params, 1);
        if (results.isEmpty()) {
            throw new DataNotFoundException();
        }
        return results.iterator().next();
    }

    @Override
    public Rendition create(Rendition rendition) {
        return daoService.create(rendition);
    }

    @Override
    public Rendition update(Rendition rendition) {
        return daoService.update(rendition);
    }

    /** {@inheritDoc } */
    @Override
    public void deleteRendition(Long id) {
        daoService.delete(Rendition.class, id);
    }

    /**
     * Creates a new {@link MediaItemRendition} based on a {@link File} and
     * {@link MediaItem}.
     * <p/>
     * @param file        File representing the {@link MediaItemRendition}
     * @param item        {@link MediaItem} to add the {@link MediaItemRendition}
     * @param rendition   {@link Rendition} of the {@link MediaItemRendition}
     * @param filename    Name of the file
     * @param contentType Content type of the file
     * @return Created {@link MediaItemRendition}
     * @throws IOException If the {@link MediaItemRendition} could not be stored in the {@link Catalogue}
     */
    @Override
    public MediaItemRendition create(File file, MediaItem item, Rendition rendition, String filename, String contentType) throws IOException {
        Catalogue catalogue = item.getCatalogue();
        String originalExtension = FilenameUtils.getExtension(filename);
        StringBuilder realFilename = new StringBuilder();
        realFilename.append(rendition.getId()).append(".");
        realFilename.append(originalExtension);
        MediaItemRendition mediaItemRendition;
        try {
            mediaItemRendition = item.findRendition(rendition);
        } catch (RenditionNotFoundException ex) {
            mediaItemRendition = new MediaItemRendition();
        }
        mediaItemRendition.setMediaItem(item);
        mediaItemRendition.setFilename(realFilename.toString());
        mediaItemRendition.setSize(file.length());
        mediaItemRendition.setContentType(contentType);
        mediaItemRendition.setRendition(rendition);
        String path = archive(file, catalogue, mediaItemRendition);
        fillWithMetadata(mediaItemRendition);
        for (CatalogueHookInstance hookInstance : catalogue.getHooks()) {
            try {
                CatalogueHook hook = hookInstance.getHook();
                CatalogueEvent event = new CatalogueEvent(CatalogueEvent.Event.UploadRendition, mediaItemRendition.getMediaItem(), mediaItemRendition);
                hook.execute(pluginContext, event, hookInstance);
            } catch (CatalogueEventException ex) {
                LOG.log(Level.SEVERE, "Could not execute CatalogueHook", ex);
            }
        }
        if (mediaItemRendition.getId() == null) {
            mediaItemRendition = daoService.create(mediaItemRendition);
        } else {
            mediaItemRendition = daoService.update(mediaItemRendition);
        }
        return mediaItemRendition;
    }

    @Override
    public MediaItemRendition update(MediaItemRendition rendition) {
        return daoService.update(rendition);
    }

    /**
     * Executes a {@link CatalogueHookInstance} on a {@link MediaItem}.
     * <p/>
     * @param mediaItemId    Unique identifier of the {@link MediaItem}
     * @param hookInstanceId Unique identifier of the {@link CatalogueHookInstance}
     * @throws DataNotFoundException If the given {@code mediaItemId} or {@code hookInstanceId} was invalid
     */
    @Override
    public void executeHook(Long mediaItemId, Long hookInstanceId) throws DataNotFoundException {
        MediaItem mediaItem = findMediaItemById(mediaItemId);
        CatalogueHookInstance hookInstance = daoService.findById(CatalogueHookInstance.class, hookInstanceId);
        LOG.log(Level.INFO, "Executing hook {0} for Media Item #{1}", new Object[] { hookInstance.getLabel(), mediaItem.getId() });
        Long innerTaskId = systemFacade.createBackgroundTask("Executing " + hookInstance.getLabel() + " for Media Item #" + mediaItem.getId());
        for (MediaItemRendition mir : mediaItem.getRenditions()) {
            CatalogueEvent event = new CatalogueEvent(CatalogueEvent.Event.UpdateRendition, mediaItem, mir);
            try {
                CatalogueHook hook = hookInstance.getHook();
                hook.execute(pluginContext, event, hookInstance);
            } catch (CatalogueEventException ex) {
                LOG.log(Level.WARNING, ex.getMessage());
                LOG.log(Level.FINE, "Could not execute hook", ex);
            }
        }
        systemFacade.removeBackgroundTask(innerTaskId);
    }

    @Override
    public void executeBatchHook(CatalogueHookInstance hookInstance, Long catalogueId) throws DataNotFoundException {
        LOG.log(Level.INFO, "Executing Batch Hook");
        CatalogueHook hook;
        try {
            hook = hookInstance.getHook();
        } catch (CatalogueEventException ex) {
            LOG.log(Level.SEVERE, "Could not instantiate hook", ex);
            return;
        }
        if (!hook.isSupportBatch()) {
            LOG.log(Level.INFO, "{0} doesn''t support batch execution", hook.getName());
            return;
        }
        Long taskId = systemFacade.createBackgroundTask("Batch hook " + hookInstance.getLabel() + " (" + hook.getName() + ")");
        Catalogue catalogue = findCatalogueById(catalogueId);
        Map<String, Object> params = QueryBuilder.with("catalogue", catalogue).parameters();
        List<MediaItem> mediaItems = daoService.findWithNamedQuery(MediaItem.FIND_BY_CATALOGUE, params);
        for (MediaItem item : mediaItems) {
            LOG.log(Level.INFO, "Executing Batch Hook for Media Item #" + item.getId());
            Long innerTaskId = systemFacade.createBackgroundTask("Executing " + hookInstance.getLabel() + " for Media Item #" + item.getId());
            for (MediaItemRendition mir : item.getRenditions()) {
                CatalogueEvent event = new CatalogueEvent(CatalogueEvent.Event.UpdateRendition, item, mir);
                try {
                    hook.execute(pluginContext, event, hookInstance);
                } catch (CatalogueEventException ex) {
                    LOG.log(Level.WARNING, ex.getMessage());
                    LOG.log(Level.FINE, "Could not execute hook", ex);
                }
            }
            systemFacade.removeBackgroundTask(innerTaskId);
        }
        systemFacade.removeBackgroundTask(taskId);
    }

    @Override
    public MediaItemRendition update(File file, String filename, String contentType, MediaItemRendition mediaItemRendition) throws IOException {
        Catalogue catalogue = mediaItemRendition.getMediaItem().getCatalogue();
        String originalExtension = FilenameUtils.getExtension(filename);
        StringBuilder realFilename = new StringBuilder();
        realFilename.append(mediaItemRendition.getRendition().getId()).append(".");
        realFilename.append(originalExtension);
        mediaItemRendition.setFilename(realFilename.toString());
        mediaItemRendition.setSize(file.length());
        mediaItemRendition.setContentType(contentType);
        String path = archive(file, catalogue, mediaItemRendition);
        fillWithMetadata(mediaItemRendition);
        for (CatalogueHookInstance hookInstance : catalogue.getHooks()) {
            try {
                CatalogueHook hook = hookInstance.getHook();
                CatalogueEvent event = new CatalogueEvent(CatalogueEvent.Event.UpdateRendition, mediaItemRendition.getMediaItem(), mediaItemRendition);
                hook.execute(pluginContext, event, hookInstance);
            } catch (CatalogueEventException ex) {
                LOG.log(Level.SEVERE, "Could not execute CatalogueHook", ex);
            }
        }
        return update(mediaItemRendition);
    }

    private void fillWithMetadata(MediaItemRendition mediaItemRendition) {
        Map<String, String> metaData = metaDataService.extract(mediaItemRendition.getFileLocation());
        for (String key : metaData.keySet()) {
            if (key.equalsIgnoreCase("width")) {
                mediaItemRendition.setWidth(Integer.valueOf(metaData.get(key)));
            } else if (key.equalsIgnoreCase("height")) {
                mediaItemRendition.setHeight(Integer.valueOf(metaData.get(key)));
            } else if (key.equalsIgnoreCase("colourSpace")) {
                mediaItemRendition.setColourSpace(metaData.get(key));
            } else if (key.equalsIgnoreCase("Resolution")) {
                mediaItemRendition.setResolution(Integer.valueOf(metaData.get(key)));
            }
        }
    }

    /**
     * Deletes an existing {@link MediaItemRendition} from a {@link MediaItem}.
     * <p/>
     * @param id Unique identifier of the {@link MediaItemRendition}
     */
    @Override
    public void deleteMediaItemRenditionById(Long id) {
        try {
            MediaItemRendition mir = daoService.findById(MediaItemRendition.class, id);
            File f = new File(mir.getFileLocation());
            if (f.exists()) {
                f.delete();
            } else {
                LOG.log(Level.WARNING, "{0} does not exist and could not be deleted", f.getAbsoluteFile().toString());
            }
            daoService.delete(MediaItemRendition.class, id);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, "MediaItemRendition #{0} does not exist", id);
        }
    }

    /** {@inheritDoc } */
    @Override
    public MediaItem create(MediaItem mediaItem) {
        mediaItem.setCreated(Calendar.getInstance());
        mediaItem.setUpdated(mediaItem.getCreated());
        if (mediaItem.getId() == null) {
            mediaItem = daoService.create(mediaItem);
            if (mediaItem.getStatus() == null || !mediaItem.getStatus().equals(MediaItemStatus.APPROVED)) {
                searchEngine.addToIndexQueue(QueueEntryType.MEDIA_ITEM, mediaItem.getId(), QueueEntryOperation.REMOVE);
            } else {
                searchEngine.addToIndexQueue(QueueEntryType.MEDIA_ITEM, mediaItem.getId(), QueueEntryOperation.UPDATE);
            }
        }
        return mediaItem;
    }

    /**
     * Updates an existing {@link MediaItem} in the database. Upon updating the
     * {@link MediaItem} will be updated and possibly deleted from the search
     * engine.
     *
     * @param mediaItem {@link MediaItem} to update
     * @return Updated {@link MediaItem}
     */
    @Override
    public MediaItem update(MediaItem mediaItem) {
        mediaItem.setUpdated(Calendar.getInstance());
        mediaItem = daoService.update(mediaItem);
        if (mediaItem.getStatus() == null || !mediaItem.getStatus().equals(MediaItemStatus.APPROVED)) {
            searchEngine.addToIndexQueue(QueueEntryType.MEDIA_ITEM, mediaItem.getId(), QueueEntryOperation.REMOVE);
        } else {
            searchEngine.addToIndexQueue(QueueEntryType.MEDIA_ITEM, mediaItem.getId(), QueueEntryOperation.UPDATE);
        }
        if (mediaItem.getStatus() != null) {
            UserAccount user = null;
            try {
                user = userFacade.findById(ctx.getCallerPrincipal().getName());
            } catch (DataNotFoundException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            Notification notification;
            switch(mediaItem.getStatus()) {
                case APPROVED:
                    notification = new Notification("<b>" + mediaItem.getTitle() + "</b> was approved", "MediaItemDetails.xhtml?id=" + mediaItem.getId(), mediaItem.getOwner(), user);
                    notificationService.create(notification);
                    if (!mediaItem.getOwner().equals(user)) {
                        notification = new Notification("Approved <b>" + mediaItem.getTitle() + "</b>", "MediaItemDetails.xhtml?id=" + mediaItem.getId(), user, user);
                        notificationService.create(notification);
                    }
                    break;
                case REJECTED:
                    notification = new Notification("<b>" + mediaItem.getTitle() + "</b> was rejected", "MediaItemDetails.xhtml?id=" + mediaItem.getId(), mediaItem.getOwner(), user);
                    notificationService.create(notification);
                    notification = new Notification("Rejected <b>" + mediaItem.getTitle() + "</b>", "MediaItemDetails.xhtml?id=" + mediaItem.getId(), user, user);
                    notificationService.create(notification);
                    break;
                case SUBMITTED:
                    for (UserAccount editor : mediaItem.getCatalogue().getEditorRole().getUserAccounts()) {
                        notification = new Notification("<b>" + mediaItem.getTitle() + "</b> was submitted for approval", "MediaItemDetails.xhtml?id=" + mediaItem.getId(), editor, mediaItem.getOwner());
                        notificationService.create(notification);
                    }
                    notification = new Notification("Submitted <b>" + mediaItem.getTitle() + "</b> for approval", "MediaItemDetails.xhtml?id=" + mediaItem.getId(), user, user);
                    notificationService.create(notification);
                    break;
                case SELF_UPLOAD:
                    for (UserAccount editor : mediaItem.getCatalogue().getEditorRole().getUserAccounts()) {
                        notification = new Notification("<b>" + mediaItem.getTitle() + "</b> was self-uploaded for approval", "MediaItemDetails.xhtml?id=" + mediaItem.getId(), editor, mediaItem.getOwner());
                        notificationService.create(notification);
                    }
                    notification = new Notification("Submitted <b>" + mediaItem.getTitle() + "</b> for approval", "MediaItemDetails.xhtml?id=" + mediaItem.getId(), user, user);
                    notificationService.create(notification);
                    break;
            }
        }
        return mediaItem;
    }

    /**
     * Creates a {@link MediaItem} based on a {@link NewswireItem}.
     * <p/>
     * @param newswireItem {@link NewswireItem} to base the {@link MediaItem}
     * @param catalogue    {@link Catalogue} to add the {@link MediaItem}
     * @return {@link MediaItem} created
     */
    @Override
    public MediaItem create(NewswireItem newswireItem, Catalogue catalogue) {
        UserAccount user = null;
        try {
            user = userFacade.findById(ctx.getCallerPrincipal().getName());
        } catch (DataNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        MediaItem item = new MediaItem();
        item.setByLine(newswireItem.getAuthor());
        Calendar now = Calendar.getInstance();
        item.setCreated(now);
        item.setUpdated(now);
        StringBuilder description = new StringBuilder();
        if (org.apache.commons.lang.StringUtils.isNotBlank(newswireItem.getSummary())) {
            description.append(newswireItem.getSummary());
        }
        if (org.apache.commons.lang.StringUtils.isNotBlank(newswireItem.getContent())) {
            description.append(StringUtils.stripHtml(newswireItem.getContent()));
        }
        item.setDescription(description.toString());
        item.setTitle(newswireItem.getTitle());
        item.setOwner(user);
        item.setCatalogue(catalogue);
        item.setStatus(MediaItemStatus.APPROVED);
        item = create(item);
        for (NewswireItemAttachment attachment : newswireItem.getAttachments()) {
            if (attachment.isStoredInCatalogue() && attachment.isRenditionSet()) {
                try {
                    MediaItemRendition mir = new MediaItemRendition();
                    mir.setContentType(attachment.getContentType());
                    File mediaFile = new File(attachment.getCatalogueFileLocation());
                    mir.setPath(archive(mediaFile, catalogue, attachment.getFilename()));
                    mir.setFilename(attachment.getFilename());
                    mir.setRendition(attachment.getRendition());
                    mir.setMediaItem(item);
                    mir = update(mediaFile, attachment.getFilename(), attachment.getContentType(), mir);
                    item.getRenditions().add(mir);
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }
        return item;
    }

    /**
     * Deletes an existing {@link MediaItem} from the database. Upon deletion
     * the {@link MediaItem} will also be removed from the search engine.
     *
     * @param id Unique identifier of the {@link MediaItem}
     */
    @Override
    public void deleteMediaItemById(Long id) {
        searchEngine.addToIndexQueue(QueueEntryType.MEDIA_ITEM, id, QueueEntryOperation.REMOVE);
        try {
            MediaItem mi = daoService.findById(MediaItem.class, id);
            String path = null;
            for (MediaItemRendition mir : mi.getRenditions()) {
                path = mir.getPath();
                deleteMediaItemRenditionById(mir.getId());
            }
            if (path != null) {
                File p = new File(mi.getCatalogue().getLocation(), path);
                if (p.exists() && p.isDirectory()) {
                    p.delete();
                } else {
                    LOG.log(Level.WARNING, "Could not remove MediaItem #{0} path ({1})", new Object[] { id, p.getAbsoluteFile().toString() });
                }
            }
            daoService.delete(MediaItem.class, id);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, "MediaItem #{0} does not exist", id);
        }
    }

    @Override
    public MediaItem findMediaItemById(Long id) throws DataNotFoundException {
        return daoService.findById(MediaItem.class, id);
    }

    @Override
    public List<MediaItem> findMediaItemsByStatus(MediaItemStatus status) {
        Map<String, Object> params = QueryBuilder.with("status", status).parameters();
        return daoService.findWithNamedQuery(MediaItem.FIND_BY_STATUS, params);
    }

    @Override
    public List<MediaItem> findMediaItemsByOwner(UserAccount owner) {
        Map<String, Object> params = QueryBuilder.with("owner", owner).parameters();
        return daoService.findWithNamedQuery(MediaItem.FIND_BY_OWNER, params);
    }

    @Override
    public List<MediaItem> findCurrentMediaItems(UserAccount user, Long mediaRepositoryId) {
        try {
            Catalogue mr = daoService.findById(Catalogue.class, mediaRepositoryId);
            Map<String, Object> params = QueryBuilder.with("user", user).and("mediaRepository", mr).parameters();
            List<MediaItem> items = new ArrayList<MediaItem>();
            items.addAll(daoService.findWithNamedQuery(MediaItem.FIND_CURRENT_AS_OWNER, params));
            items.addAll(daoService.findWithNamedQuery(MediaItem.FIND_CURRENT_AS_EDITOR, params));
            Set set = new HashSet(items);
            return new ArrayList(set);
        } catch (DataNotFoundException ex) {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public List<MediaItem> findCurrentMediaItems(UserAccount user, MediaItemStatus status, Long mediaRepositoryId) {
        try {
            Catalogue mr = daoService.findById(Catalogue.class, mediaRepositoryId);
            Map<String, Object> params = QueryBuilder.with("user", user).and("status", status).and("mediaRepository", mr).parameters();
            return daoService.findWithNamedQuery(MediaItem.FIND_BY_OWNER_AND_STATUS, params, 200);
        } catch (DataNotFoundException ex) {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Determines if the given {@link MediaItem} is referenced by a
     * {@link dk.i2m.converge.core.content.NewsItem}.
     *
     * @param id Unique identifier of the {@link MediaItem}
     * @return {@code true} if the {@link MediaItem} is referenced, otherwise {@code false}
     */
    @Override
    public boolean isMediaItemUsed(Long id) {
        try {
            MediaItem mediaItem = daoService.findById(MediaItem.class, id);
            Map<String, Object> params = QueryBuilder.with("mediaItem", mediaItem).parameters();
            List results = daoService.findWithNamedQuery(NewsItemMediaAttachment.FIND_BY_MEDIA_ITEM, params);
            if (results.isEmpty()) {
                return false;
            } else {
                return true;
            }
        } catch (DataNotFoundException ex) {
            return false;
        }
    }

    /**
     * Gets a {@link List} of all placements for a given {@link MediaItem}.
     * <p/>
     * @param id Unique identifier of the {@link MediaItem}
     * @return {@link List} of placements for the given {@link MediaItem}
     * @throws DataNotFoundException * If the given {@link MediaItem} does not exist
     */
    @Override
    public List<MediaItemUsage> getMediaItemUsage(Long id) throws DataNotFoundException {
        MediaItem mediaItem = daoService.findById(MediaItem.class, id);
        Map<String, Object> params = QueryBuilder.with("mediaItem", mediaItem).parameters();
        List<NewsItemMediaAttachment> results = daoService.findWithNamedQuery(NewsItemMediaAttachment.FIND_BY_MEDIA_ITEM, params);
        List<MediaItemUsage> output = new ArrayList<MediaItemUsage>();
        for (NewsItemMediaAttachment attachment : results) {
            if (!attachment.getNewsItem().getPlacements().isEmpty()) {
                for (NewsItemPlacement placement : attachment.getNewsItem().getPlacements()) {
                    MediaItemUsage usage = new MediaItemUsage();
                    usage.setNewsItemId(attachment.getNewsItem().getId());
                    usage.setTitle(attachment.getNewsItem().getTitle());
                    usage.setCaption(attachment.getCaption());
                    usage.setDate(placement.getEdition().getPublicationDate().getTime());
                    usage.setOutlet(placement.getEdition().getOutlet().getTitle());
                    usage.setSection(placement.getSection().getFullName());
                    usage.setStart(placement.getStart());
                    usage.setPosition(placement.getPosition());
                    usage.setPublished(!placement.getEdition().isOpen());
                    output.add(usage);
                }
            } else {
                MediaItemUsage usage = new MediaItemUsage();
                usage.setNewsItemId(attachment.getNewsItem().getId());
                usage.setTitle(attachment.getNewsItem().getTitle());
                usage.setCaption(attachment.getCaption());
                usage.setDate(attachment.getNewsItem().getUpdated().getTime());
                usage.setOutlet("");
                usage.setSection("");
                usage.setStart(0);
                usage.setPosition(0);
                usage.setPublished(false);
                output.add(usage);
            }
        }
        return output;
    }

    /**
     * Scans all the active catalogue drop points for files and processes new files.
     */
    @Override
    public void scanDropPoints() {
    }

    /**
     * Archives a {@link MediaItemRendition} in a {@link Catalogue}.
     * <p/>
     * @param file      File to archive
     * @param catalogue Catalogue used for archiving the file
     * @param rendition Rendition to store
     * @param rendition {@link MediaItemRendition} to archive
     * @return Path where the rendition was stored on the {@link Catalogue}
     * @throws IOException * If the file could not be archived
     */
    public String archive(File file, Catalogue catalogue, MediaItemRendition rendition) throws IOException {
        Calendar now = Calendar.getInstance();
        StringBuilder cataloguePath = new StringBuilder();
        cataloguePath.append(now.get(Calendar.YEAR)).append(File.separator).append(now.get(Calendar.MONTH) + 1).append(File.separator).append(now.get(Calendar.DAY_OF_MONTH)).append(File.separator).append(rendition.getMediaItem().getId());
        StringBuilder catalogueLocation = new StringBuilder(catalogue.getLocation());
        catalogueLocation.append(File.separator).append(cataloguePath.toString());
        File dir = new File(catalogueLocation.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File mediaFile = new File(dir, rendition.getFilename());
        LOG.log(Level.FINE, "Archiving {0} at {1}", new Object[] { file.getAbsolutePath(), mediaFile.getAbsolutePath() });
        copyFile(file, mediaFile);
        rendition.setPath(cataloguePath.toString());
        return cataloguePath.toString();
    }

    /**
     * Archives a {@link File} in a {@link Catalogue}.
     * <p/>
     * @param file      File to archive
     * @param catalogue Catalogue used for archiving the file
     * @param fileName  File name of the file
     * @return Path where the file was stored on the {@link Catalogue}
     * @throws IOException If the file could not be archived
     */
    @Override
    public String archive(File file, Catalogue catalogue, String fileName) throws IOException {
        if (!file.exists()) {
            throw new IOException("Source file does not exist");
        }
        Calendar now = Calendar.getInstance();
        StringBuilder cataloguePath = new StringBuilder();
        cataloguePath.append(now.get(Calendar.YEAR)).append(File.separator).append(now.get(Calendar.MONTH) + 1).append(File.separator).append(now.get(Calendar.DAY_OF_MONTH));
        StringBuilder catalogueLocation = new StringBuilder(catalogue.getLocation());
        catalogueLocation.append(File.separator).append(cataloguePath.toString());
        File dir = new File(catalogueLocation.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File mediaFile = new File(dir, fileName);
        copyFile(file, mediaFile);
        return cataloguePath.toString();
    }

    /**
     * Utility method for copying a file from one
     * location to another.
     * <p/>
     * @param sourceFile Source {@link File}
     * @param destFile   Destination {@link File}
     * @throws IOException * If the {@link File} could not be copied
     */
    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    @Override
    public CatalogueHookInstance createCatalogueAction(CatalogueHookInstance action) {
        return daoService.create(action);
    }

    @Override
    public CatalogueHookInstance updateCatalogueAction(CatalogueHookInstance action) {
        return daoService.update(action);
    }
}
