package com.google.code.gronono.gps.model;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.google.code.gronono.commons.exif.ExifData;
import com.google.code.gronono.commons.exif.ExifUtils;
import com.google.code.gronono.commons.i18n.BundleKey;
import com.google.code.gronono.commons.i18n.BundleName;
import com.google.code.gronono.commons.io.file.FileUtils;
import com.google.code.gronono.commons.io.file.jpeg.JpegFile;
import com.google.code.gronono.commons.observable.AbstractObservable;
import com.google.code.gronono.gps.model.enums.ProcessAction;
import com.google.code.gronono.gps.pattern.JsPattern;
import com.google.code.gronono.gps.pattern.KeywordsPattern;

/**
 * Service de l'application.
 * <br/>Implémente les méthodes de tri.
 */
@BundleName(value = "com.google.code.gronono.gps.gui")
public class GPSService extends AbstractObservable {

    /** Logger. */
    private static final Logger logger = Logger.getLogger(GPSService.class);

    /** Message d'erreur dans le cas où le dossier source n'existe pas. */
    @BundleKey(value = "service.process.src.dir.not.found.err.msg")
    private static String SRC_DIR_NOT_FOUND_ERR_MSG;

    /** Message d'erreur dans le cas où le dossier source n'est pas un répertoire. */
    @BundleKey(value = "service.process.src.dir.not.dir.err.msg")
    private static String SRC_DIR_NOT_DIR_ERR_MSG;

    /** Message indiquant que le traitement est en cours. */
    @BundleKey(value = "service.process.work.in.progress")
    private static String WORK_IN_PROGRESS;

    /** Message indiquant que le traitement est en mode aperçu. */
    @BundleKey(value = "service.process.preview.mode")
    private static String PREVIEW_MODE;

    /** Message d'erreur dans le cas où le dossier n'est pas un répertoire. */
    @BundleKey(value = "service.process.dir.not.dir.err.msg")
    private static String DIR_NOT_DIR_ERR_MSG;

    /** Message d'erreur dans le cas où le dossier est vide. */
    @BundleKey(value = "service.process.dir.empty.err.msg")
    private static String DIR_EMPTY_ERR_MSG;

    /** Message d'erreur dans le cas où le dossier est ignoré (mode non-récursif). */
    @BundleKey(value = "service.process.dir.ignored.err.msg")
    private static String DIR_IGNORED_ERR_MSG;

    /** Message d'information sur le traitement du fichier. */
    @BundleKey(value = "service.process.file")
    private static String FILE;

    /** Message d'erreur sur la détermination du fichier cible. */
    @BundleKey(value = "service.process.file.get.dst.file.err.msg")
    private static String FILE_GET_DST_FILE_ERR_MSG;

    /** Message d'erreur sur la détermination du fichier cible. */
    @BundleKey(value = "service.process.file.dst.file.exists.err.msg")
    private static String FILE_DST_FILE_EXISTS_ERR_MSG;

    /** Message d'erreur sur la détermination du fichier cible. */
    @BundleKey(value = "service.process.file.dst.file.exists.in.results.err.msg")
    private static String FILE_DST_FILE_EXISTS_IN_RESULTS_ERR_MSG;

    /** Message d'erreur sur les droits en écriture du répertoire cible. */
    @BundleKey(value = "service.process.file.dst.dir.not.writable.err.msg")
    private static String FILE_DST_DIR_NOT_WRITABLE_ERR_MSG;

    /** Message d'erreur pour le cas ou le fichier source est filtré. */
    @BundleKey(value = "service.process.file.src.file.filtered.err.msg")
    private static String FILE_SRC_FILE_FILTERED_ERR_MSG;

    /** Message d'erreur sur le traitement du fichier source. */
    @BundleKey(value = "service.process.file.err.msg")
    private static String FILE_ERR_MSG;

    /** Message d'erreur dans le cas où le fichier n'est pas un fichier. */
    @BundleKey(value = "service.process.file.not.file.err.msg")
    private static String FILE_NOT_FILE_ERR_MSG;

    /** Message d'erreur sur le filtrage par extension du fichier source. */
    @BundleKey(value = "service.process.filter.file.filtered.by.extension.err.msg")
    private static String FILTERED_BY_EXTENSION_ERR_MSG;

    /** Message d'erreur sur le filtrage par expression régulière du fichier source. */
    @BundleKey(value = "service.process.filter.file.filtered.by.expression.err.msg")
    private static String FILTERED_BY_EXPRESSION_ERR_MSG;

    /** Message d'erreur sur le filtrage par date du fichier source. */
    @BundleKey(value = "service.process.filter.file.filter.by.date.exif.err.msg")
    private static String FILTER_BY_DATE_EXIF;

    /** Message d'erreur sur le filtrage par date du fichier source. */
    @BundleKey(value = "service.process.filter.file.filter.by.date.lastmod.err.msg")
    private static String FILTER_BY_DATE_LASTMOD;

    /** Message d'erreur sur le filtrage par date du fichier source. */
    @BundleKey(value = "service.process.filter.file.filtered.by.date.before.date.min.err.msg")
    private static String FILTERED_BY_DATE_BEFORE_DATE_MIN_ERR_MSG;

    /** Message d'erreur sur le filtrage par date du fichier source. */
    @BundleKey(value = "service.process.filter.file.filter.by.date.date.min.parse.err.msg")
    private static String FILTER_BY_DATE_MIN_PARSE_ERR_MSG;

    /** Message d'erreur sur le filtrage par date du fichier source. */
    @BundleKey(value = "service.process.filter.file.filtered.by.date.after.date.max.err.msg")
    private static String FILTERED_BY_DATE_AFTER_DATE_MAX_ERR_MSG;

    /** Message d'erreur sur le filtrage par date du fichier source. */
    @BundleKey(value = "service.process.filter.file.filter.by.date.date.max.parse.err.msg")
    private static String FILTER_BY_DATE_MAX_PARSE_ERR_MSG;

    /** Message sur le rejet des fichiers source. */
    @BundleKey(value = "service.process.reject.file.copy")
    private static String REJECT_FILE_COPY;

    /** Message sur le rejet des fichiers source. */
    @BundleKey(value = "service.process.reject.file.move")
    private static String REJECT_FILE_MOVE;

    /** Message d'erreur sur le rejet des fichiers source. */
    @BundleKey(value = "service.process.reject.file.err.msg")
    private static String REJECT_FILE_ERR_MSG;

    /** Message d'erreur sur le rejet des fichiers source. */
    @BundleKey(value = "service.process.reject.file.exists.err.msg")
    private static String REJECT_FILE_EXISTS_ERR_MSG;

    /** Message d'erreur sur le rejet des fichiers source. */
    @BundleKey(value = "service.process.reject.file.is.preview.err.msg")
    private static String REJECT_FILE_IS_PREVIEW_ERR_MSG;

    /** Message d'erreur sur le rejet des fichiers source. */
    @BundleKey(value = "service.process.reject.file.src.file.not.file.err.msg")
    private static String REJECT_FILE_SRC_FILE_NOT_FILE_ERR_MSG;

    /** Message d'erreur sur le rejet des fichiers source. */
    @BundleKey(value = "service.process.reject.file.deactivated.err.msg")
    private static String REJECT_FILE_DEACTIVATED_ERR_MSG;

    /** Exception action non-supportée. */
    @BundleKey(value = "service.process.unsupported.process.action")
    private static String UNSUPPORTED_PROCESS_ACTION;

    /** Exception type de filtre non-supporté. */
    @BundleKey(value = "service.process.unsupported.filter.type")
    private static String UNSUPPORTED_FILTER_TYPE;

    /** Exception type de filtre de date non-supporté. */
    @BundleKey(value = "service.process.unsupported.datefilter.type")
    private static String UNSUPPORTED_DATEFILTER_TYPE;

    /** Exception type de pattern non-supporté. */
    @BundleKey(value = "service.process.unsupported.pattern.type")
    private static String UNSUPPORTED_PATTERN_TYPE;

    /** Clé de propriété représentant le fichier en cours de traitement. */
    public static final String CURRENT_FILE_PROPERTY = "currentFile";

    /** La configuration de l'application. */
    private final Configuration configuration;

    /** Fichier en cours de traitement. */
    private File currentFile;

    /** Filtre (par expression régulière) sur les fichiers d'entrée. */
    private FileFilter regexFileFilter;

    /** Le pattern par mots-clés. */
    private KeywordsPattern keyWordsPattern;

    /** Le pattern javascript. */
    private JsPattern jsPattern;

    /** Flag indiquant qu'une demande d'annulation est faite. */
    private boolean cancel = false;

    /** Liste représentant le résultat du traitement. */
    private List<SortedFile> sortedFiles;

    /**
	 * Constructeur.
	 * @param configuration La configuration de l'application.
	 */
    public GPSService(final Configuration configuration) {
        this.configuration = configuration;
        sortedFiles = new ArrayList<SortedFile>();
    }

    /**
	 * Réinitialisation de la liste représentant le résultat du traitement.
	 */
    public void reset() {
        logger.info("reset");
        sortedFiles.clear();
        if (keyWordsPattern != null) keyWordsPattern.reset();
    }

    /**
	 * Méthode implémentant le traitement de tri.
	 * @param preview <code>true</code> indique qu'on fonctionne en mode aperçu,
	 *  <code>false</code> que le traitement doit être physiquement effectif.
	 */
    public void process(boolean preview) {
        if (!configuration.getSrcDir().exists()) throw new IllegalStateException(SRC_DIR_NOT_FOUND_ERR_MSG);
        if (!configuration.getSrcDir().isDirectory()) throw new IllegalStateException(SRC_DIR_NOT_DIR_ERR_MSG);
        long t = 0;
        if (logger.isInfoEnabled()) {
            t = System.currentTimeMillis();
            String msg = null;
            if (preview) msg = MessageFormat.format(WORK_IN_PROGRESS, PREVIEW_MODE); else MessageFormat.format(WORK_IN_PROGRESS, "");
            logger.info(msg);
        }
        if (sortedFiles.isEmpty()) {
            regexFileFilter = new RegexFileFilter(this.configuration.getFilterExpression());
            keyWordsPattern = new KeywordsPattern();
            jsPattern = new JsPattern(this.configuration.getPatternJsExpression());
            sortedFiles = new ArrayList<SortedFile>();
            processDir(configuration.getSrcDir());
        }
        if (!preview) {
            for (final SortedFile sortedFile : sortedFiles) {
                if (cancel) break;
                final File srcFile = sortedFile.getSrcFile();
                final File dstFile = sortedFile.getDstFile();
                if (StringUtils.isNotBlank(sortedFile.getErrorMsg())) {
                    rejectFile(sortedFile, false);
                } else if (dstFile != null) {
                    try {
                        setCurrentFile(srcFile);
                        boolean isRotated = false;
                        if (configuration.isRotate()) {
                            if ((configuration.getProcessAction() == ProcessAction.COPY) || (configuration.getProcessAction() == ProcessAction.MOVE)) {
                                final JpegFile jpegFile = new JpegFile(srcFile);
                                final ExifData exifData = jpegFile.getExifData();
                                if ((exifData != null) && ExifUtils.needsRotation(exifData.getOrientation())) {
                                    if (logger.isDebugEnabled()) logger.debug("Rotation : " + srcFile + " -> " + dstFile);
                                    final byte[] srcFileData = FileUtils.readFileToByteArray(srcFile);
                                    dstFile.getParentFile().mkdirs();
                                    final FileOutputStream fos = new FileOutputStream(dstFile.getAbsolutePath());
                                    fos.write(ExifUtils.getRotatedImage(srcFileData, exifData.getOrientation()));
                                    fos.close();
                                    if (configuration.getProcessAction() == ProcessAction.MOVE) {
                                        if (logger.isDebugEnabled()) logger.debug(" + Move => FileUtils.deleteQuietly : " + srcFile);
                                        FileUtils.deleteQuietly(srcFile);
                                    }
                                    isRotated = true;
                                }
                            }
                        }
                        if (!isRotated) {
                            switch(configuration.getProcessAction()) {
                                case COPY:
                                    if (logger.isDebugEnabled()) logger.debug("FileUtils.copyFile : " + srcFile + " -> " + dstFile);
                                    FileUtils.copyFile(srcFile, dstFile);
                                    break;
                                case MOVE:
                                    if (logger.isDebugEnabled()) logger.debug("FileUtils.moveFile : " + srcFile + " -> " + dstFile);
                                    FileUtils.deleteQuietly(dstFile);
                                    FileUtils.moveFile(srcFile, dstFile);
                                    break;
                                default:
                                    final String msg = MessageFormat.format(UNSUPPORTED_PROCESS_ACTION, configuration.getProcessAction());
                                    sortedFile.setErrorMsg(msg);
                                    logger.error(msg);
                                    throw new IllegalArgumentException(msg);
                            }
                        }
                    } catch (final IOException e) {
                        final StringBuilder msg = new StringBuilder("IOException : ").append(e.getMessage());
                        if (e.getCause() != null) msg.append(e.getCause());
                        logger.error(msg.toString(), e);
                        sortedFile.setErrorMsg(e.getMessage());
                        if (e.getCause() != null) sortedFile.setErrorCause(e.getCause().getMessage());
                        rejectFile(sortedFile, false);
                    }
                }
            }
        }
        if (logger.isInfoEnabled()) logger.info("Traitement en " + (System.currentTimeMillis() - t) + " ms");
    }

    /**
	 * Traite le dossier spécifié ('calcul' préalable du résultat).
	 * @param dir Le dossier à traiter.
	 */
    private void processDir(final File dir) {
        this.cancel = false;
        if (cancel) return;
        this.setCurrentFile(dir);
        final SortedFile sortedFile = new SortedFile(dir);
        sortedFiles.add(sortedFile);
        try {
            FileUtils.checkCanReadDir(dir);
        } catch (final IOException e) {
            sortedFile.setErrorMsg(e.getMessage());
            return;
        }
        if (configuration.getProcessAction() == ProcessAction.MOVE) {
            try {
                FileUtils.checkCanWriteDir(dir, false);
            } catch (final IOException e) {
                sortedFile.setErrorMsg(e.getMessage());
                return;
            }
        }
        final File[] files = dir.listFiles();
        if (files != null) {
            if (files.length != 0) {
                for (final File file : files) {
                    if (cancel) break;
                    if (file.isDirectory()) {
                        if (configuration.isRecursive()) {
                            processDir(file);
                        } else {
                            final SortedFile ignoredSortedFile = new SortedFile(file);
                            sortedFiles.add(ignoredSortedFile);
                            ignoredSortedFile.setErrorMsg(DIR_IGNORED_ERR_MSG);
                        }
                    } else {
                        processFile(file);
                    }
                }
            } else {
                sortedFile.setErrorMsg(DIR_EMPTY_ERR_MSG);
            }
        } else {
            final String msg = MessageFormat.format(DIR_NOT_DIR_ERR_MSG, dir.getAbsolutePath());
            sortedFile.setErrorMsg(msg);
            logger.warn(msg);
        }
    }

    /**
	 * Traitement du fichier spécifié ('calcul' préalable du résultat).
	 * @param file Le fichier à traiter.
	 */
    private void processFile(final File file) {
        if (cancel) return;
        this.setCurrentFile(file);
        final SortedFile sortedFile = new SortedFile(file);
        boolean hideFilterRejectedFile = false;
        File dstFile = null;
        try {
            FileUtils.checkCanReadFile(file);
            if (file.isFile()) {
                if (filterFile(sortedFile)) {
                    logger.info(MessageFormat.format(FILE, file.getAbsolutePath()));
                    try {
                        dstFile = getDstFile(file);
                        logger.info(" -> " + dstFile);
                    } catch (final Exception e) {
                        final String msg = MessageFormat.format(FILE_GET_DST_FILE_ERR_MSG, e.getMessage());
                        sortedFile.setErrorMsg(msg);
                        if (e.getCause() != null) sortedFile.setErrorCause(e.getCause().getMessage());
                        logger.error(msg, e);
                        if (configuration.isRejectProcess()) {
                            dstFile = rejectFile(sortedFile, true);
                            if (dstFile != null) sortedFile.setDstFile(dstFile);
                        }
                    }
                    if (dstFile != null) {
                        final boolean dstFileExistsInResults = checkDstFileExistsInResults(dstFile);
                        if ((dstFile.exists() && !configuration.isOverwrite()) || dstFileExistsInResults) {
                            String msg = FILE_DST_FILE_EXISTS_ERR_MSG;
                            if (dstFileExistsInResults) msg = FILE_DST_FILE_EXISTS_IN_RESULTS_ERR_MSG;
                            sortedFile.setErrorMsg(msg);
                            logger.warn(msg);
                            if (configuration.isRejectProcess()) {
                                dstFile = rejectFile(sortedFile, true);
                                if (dstFile != null) sortedFile.setDstFile(dstFile);
                            }
                        } else {
                            try {
                                FileUtils.checkCanWriteDir(configuration.getDstDir(), true);
                                try {
                                    FileUtils.checkCanWriteDir(dstFile.getParentFile(), false);
                                } catch (final FileNotFoundException fex) {
                                }
                            } catch (final IOException e) {
                                final String msg = MessageFormat.format(FILE_DST_DIR_NOT_WRITABLE_ERR_MSG, e.getMessage());
                                sortedFile.setErrorMsg(msg);
                                if (e.getCause() != null) sortedFile.setErrorCause(e.getCause().getMessage());
                                logger.warn(msg, e);
                                if (configuration.isRejectProcess()) {
                                    dstFile = rejectFile(sortedFile, true);
                                    if (dstFile != null) sortedFile.setDstFile(dstFile);
                                }
                            } finally {
                                sortedFile.setDstFile(dstFile);
                            }
                        }
                    }
                } else {
                    final String msg = FILE_SRC_FILE_FILTERED_ERR_MSG;
                    sortedFile.setErrorMsg(msg);
                    logger.warn(msg);
                    hideFilterRejectedFile = !configuration.isFilterShowRejects();
                    if (configuration.isFilterShowRejects() && configuration.isRejectProcess()) {
                        dstFile = rejectFile(sortedFile, true);
                        if (dstFile != null) sortedFile.setDstFile(dstFile);
                    }
                }
            } else {
                final String msg = MessageFormat.format(FILE_NOT_FILE_ERR_MSG, file.getAbsolutePath());
                sortedFile.setErrorMsg(msg);
                logger.warn(msg);
            }
        } catch (final Throwable e) {
            final String msg = MessageFormat.format(FILE_ERR_MSG, e.getMessage());
            sortedFile.setErrorMsg(msg);
            if (e.getCause() != null) sortedFile.setErrorCause(e.getCause().getMessage());
            logger.error(msg, e);
            if (configuration.isRejectProcess()) {
                dstFile = rejectFile(sortedFile, true);
                if (dstFile != null) sortedFile.setDstFile(dstFile);
            }
        } finally {
            if (!hideFilterRejectedFile) sortedFiles.add(sortedFile);
        }
    }

    /**
	 * Filtre le fichier spécifié.
	 * @param sortedFile Le fichier à filtrer.
	 * @return <code>true</code> si le fichier correspond au filtre, <code>false</code> sinon.
	 */
    private boolean filterFile(final SortedFile sortedFile) {
        boolean isFileValid = false;
        final File file = sortedFile.getSrcFile();
        switch(configuration.getFilterType()) {
            case EXTENSION:
                final String fileExtension = StringUtils.right(file.getName(), StringUtils.length(file.getName()) - StringUtils.lastIndexOf(file.getName(), '.') - 1);
                final String extensionsList = StringUtils.replace(configuration.getFilterExtension(), ".", "");
                if (StringUtils.isNotBlank(extensionsList)) {
                    if ("*".equals(extensionsList)) isFileValid = true; else {
                        final StringTokenizer st = new StringTokenizer(extensionsList, ",");
                        while (st.hasMoreElements()) {
                            if (fileExtension.equalsIgnoreCase(StringUtils.trimToEmpty(st.nextToken()))) {
                                isFileValid = true;
                                break;
                            }
                        }
                    }
                } else isFileValid = true;
                if (!isFileValid) sortedFile.setErrorCause(FILTERED_BY_EXTENSION_ERR_MSG);
                break;
            case REGEX:
                isFileValid = regexFileFilter.accept(file);
                if (!isFileValid) sortedFile.setErrorCause(FILTERED_BY_EXPRESSION_ERR_MSG);
                break;
            default:
                final String msg = MessageFormat.format(UNSUPPORTED_FILTER_TYPE, configuration.getFilterType());
                logger.error(msg);
                throw new IllegalArgumentException(msg);
        }
        if (isFileValid && configuration.isFilterByDate() && StringUtils.isNotBlank(configuration.getFilterDateFormat())) {
            final SimpleDateFormat df = new SimpleDateFormat(configuration.getFilterDateFormat());
            switch(configuration.getFilterDateType()) {
                case EXIF:
                    final JpegFile jpegFile = new JpegFile(file);
                    final ExifData exifData = jpegFile.getExifData();
                    if (exifData != null) {
                        final Date exifDate = exifData.getDate();
                        if (exifDate != null) {
                            if (StringUtils.isNotBlank(configuration.getFilterDateMin())) {
                                try {
                                    final Date dateMin = df.parse(configuration.getFilterDateMin());
                                    if (exifDate.after(dateMin)) isFileValid = true; else {
                                        isFileValid = false;
                                        sortedFile.setErrorCause(MessageFormat.format(FILTERED_BY_DATE_BEFORE_DATE_MIN_ERR_MSG, FILTER_BY_DATE_EXIF));
                                    }
                                } catch (final ParseException e) {
                                    isFileValid = false;
                                    sortedFile.setErrorCause(MessageFormat.format(FILTER_BY_DATE_MIN_PARSE_ERR_MSG, e.getMessage()));
                                }
                            }
                            if (isFileValid && StringUtils.isNotBlank(configuration.getFilterDateMax())) {
                                try {
                                    final Date dateMax = df.parse(configuration.getFilterDateMax());
                                    if (exifDate.before(dateMax)) isFileValid = true; else {
                                        isFileValid = false;
                                        sortedFile.setErrorCause(MessageFormat.format(FILTERED_BY_DATE_AFTER_DATE_MAX_ERR_MSG, FILTER_BY_DATE_EXIF));
                                    }
                                } catch (final ParseException e) {
                                    isFileValid = false;
                                    sortedFile.setErrorCause(MessageFormat.format(FILTER_BY_DATE_MAX_PARSE_ERR_MSG, e.getMessage()));
                                }
                            }
                        }
                    }
                    break;
                case LAST_MOD:
                    final Date lastMod = new Date(file.lastModified());
                    if (StringUtils.isNotBlank(configuration.getFilterDateMin())) {
                        try {
                            final Date dateMin = df.parse(configuration.getFilterDateMin());
                            if (lastMod.after(dateMin)) isFileValid = true; else {
                                isFileValid = false;
                                sortedFile.setErrorCause(MessageFormat.format(FILTERED_BY_DATE_BEFORE_DATE_MIN_ERR_MSG, FILTER_BY_DATE_LASTMOD));
                            }
                        } catch (final ParseException e) {
                            isFileValid = false;
                            sortedFile.setErrorCause(MessageFormat.format(FILTER_BY_DATE_MIN_PARSE_ERR_MSG, e.getMessage()));
                        }
                    }
                    if (isFileValid && StringUtils.isNotBlank(configuration.getFilterDateMax())) {
                        try {
                            final Date dateMax = df.parse(configuration.getFilterDateMax());
                            if (lastMod.before(dateMax)) isFileValid = true; else {
                                isFileValid = false;
                                sortedFile.setErrorCause(MessageFormat.format(FILTERED_BY_DATE_AFTER_DATE_MAX_ERR_MSG, FILTER_BY_DATE_LASTMOD));
                            }
                        } catch (final ParseException e) {
                            isFileValid = false;
                            sortedFile.setErrorCause(MessageFormat.format(FILTER_BY_DATE_MAX_PARSE_ERR_MSG, e.getMessage()));
                        }
                    }
                    break;
                default:
                    final String msg = MessageFormat.format(UNSUPPORTED_DATEFILTER_TYPE, configuration.getFilterDateType());
                    logger.error(msg);
                    throw new IllegalArgumentException(msg);
            }
        }
        return isFileValid;
    }

    /**
	 * Détermine le fichier cible correspondant au fichier source spécifié.
	 * @param file Le fichier source.
	 * @return Le fichier cible.
	 * @throws Exception En cas d'erreur d'interprétation des patterns.
	 */
    private File getDstFile(final File file) throws Exception {
        final String dstDir = configuration.getDstDir().getAbsolutePath();
        final StringBuilder dstPath = new StringBuilder(dstDir).append(File.separator);
        final JpegFile jpegFile = new JpegFile(file);
        switch(configuration.getPatternType()) {
            case KEYWORDS:
                final ExifData exifData = jpegFile.getExifData();
                final String targetPath = keyWordsPattern.applyPattern(jpegFile, configuration.getPatternKeywordsExpression(), exifData);
                dstPath.append(targetPath);
                break;
            case JS:
                final String targetPath2 = jsPattern.applyPattern(jpegFile);
                dstPath.append(targetPath2);
                break;
            default:
                final String msg = MessageFormat.format(UNSUPPORTED_PATTERN_TYPE, configuration.getPatternType());
                logger.error(msg);
                throw new IllegalArgumentException(msg);
        }
        return new File(dstPath.toString());
    }

    /**
	 * Détermine si le fichier cible existe déjà dans la liste de résultats.
	 *
	 * @param dstFile Le fichier cible.
	 * @return <code>true</code> si le fichier est considéré comme déjà existant.
	 */
    private boolean checkDstFileExistsInResults(final File dstFile) {
        boolean dstFileExists = false;
        if (dstFile == null) return false;
        for (final SortedFile sortedFile : sortedFiles) {
            if ((sortedFile.getDstFile() != null) && dstFile.getAbsolutePath().equals(sortedFile.getDstFile().getAbsolutePath())) {
                dstFileExists = true;
                break;
            }
        }
        return dstFileExists;
    }

    /**
	 * Détermine le fichier de rejet pour le fichier source spécifié.
	 * @param sortedFile Le fichier source.
	 * @param preview <code>true</code> si on est mode aperçu, <code>false</code> sinon.
	 * @return Le fichier de rejet.
	 */
    private File rejectFile(final SortedFile sortedFile, boolean preview) {
        final File srcFile = sortedFile.getSrcFile();
        File rejectDstFile = null;
        if (configuration.isRejectProcess()) {
            if (srcFile.isFile()) {
                if (logger.isDebugEnabled()) logger.debug("rejectFile : " + srcFile.getAbsolutePath());
                try {
                    final JpegFile jpegFile = new JpegFile(srcFile);
                    final ExifData exifData = jpegFile.getExifData();
                    final String targetFileName = keyWordsPattern.applyPattern(srcFile, configuration.getRejectPatternKeywordsExpression(), exifData);
                    final StringBuilder targetPath = new StringBuilder(configuration.getRejectDir().getAbsolutePath()).append(File.separator).append(targetFileName);
                    rejectDstFile = new File(targetPath.toString());
                    if (logger.isDebugEnabled()) logger.debug(" -> " + rejectDstFile.getAbsolutePath());
                    if (!preview) {
                        if (!(rejectDstFile.exists() && !configuration.isOverwrite())) {
                            switch(configuration.getProcessAction()) {
                                case COPY:
                                    logger.info(MessageFormat.format(REJECT_FILE_COPY, srcFile.getAbsolutePath(), rejectDstFile.getAbsolutePath()));
                                    FileUtils.copyFile(srcFile, rejectDstFile);
                                    break;
                                case MOVE:
                                    logger.info(MessageFormat.format(REJECT_FILE_MOVE, srcFile.getAbsolutePath(), rejectDstFile.getAbsolutePath()));
                                    FileUtils.deleteQuietly(rejectDstFile);
                                    FileUtils.moveFile(srcFile, rejectDstFile);
                                    break;
                                default:
                                    final String msg = MessageFormat.format(UNSUPPORTED_PROCESS_ACTION, configuration.getProcessAction());
                                    logger.error(msg);
                                    throw new IllegalArgumentException(msg);
                            }
                        } else {
                            logger.info(REJECT_FILE_EXISTS_ERR_MSG);
                        }
                    } else {
                        logger.debug(REJECT_FILE_IS_PREVIEW_ERR_MSG);
                    }
                } catch (final Exception ex) {
                    final String msg = MessageFormat.format(REJECT_FILE_ERR_MSG, ex.getMessage());
                    logger.error(msg, ex);
                }
            } else {
                logger.debug(REJECT_FILE_SRC_FILE_NOT_FILE_ERR_MSG);
            }
        } else {
            logger.trace(REJECT_FILE_DEACTIVATED_ERR_MSG);
        }
        return rejectDstFile;
    }

    /**
	 * Positionne une demande d'annulation du traitement.
	 */
    public void cancel() {
        logger.info("cancel");
        this.cancel = true;
    }

    /**
	 * Modifie le fichier source courant (utilisé pour notifier un listener par ex.).
	 * @param currentFile Le fichier source courant à positionner.
	 */
    private void setCurrentFile(final File currentFile) {
        final File oldValue = this.currentFile;
        this.currentFile = currentFile;
        firePropertyChange(CURRENT_FILE_PROPERTY, oldValue, currentFile);
    }

    /**
	 * Récupère la liste représentant le résultat de traitement sous forme de tableau.
	 * @return La liste représentant le résultat de traitement.
	 */
    public SortedFile[] getProcessFiles() {
        return sortedFiles.toArray(new SortedFile[sortedFiles.size()]);
    }
}
