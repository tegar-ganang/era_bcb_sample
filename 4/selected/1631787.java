package org.archive.crawler.admin.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.settings.ComplexType;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.ListType;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.ModuleAttributeInfo;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.crawler.settings.XMLSettingsHandler;
import org.archive.crawler.settings.refinements.Refinement;
import org.archive.util.IoUtils;

/**
 * Utility methods used configuring jobs in the admin UI.
 * 
 * Methods are mostly called by the admin UI jsp.
 * 
 * @author stack
 * @version $Date: 2006-07-31 19:39:04 +0000 (Mon, 31 Jul 2006) $, $Revision: 4401 $
 */
public class JobConfigureUtils {

    private static Logger logger = Logger.getLogger(JobConfigureUtils.class.getName());

    public static final String ACTION = "action";

    public static final String SUBACTION = "subaction";

    public static final String FILTERS = "filters";

    private static final String MAP = "map";

    private static final String FILTER = "filter";

    private static final Object ADD = "add";

    private static final Object MOVEUP = "moveup";

    private static final Object MOVEDOWN = "movedown";

    private static final Object REMOVE = "remove";

    private static final Object GOTO = "goto";

    private static final Object DONE = "done";

    private static final Object CONTINUE = "continue";

    /**
     * Check passed crawljob CrawlJob setting. Call this method at start of
     * page.
     * 
     * @param job
     *            Current CrawlJobHandler.
     * @param request
     *            Http request.
     * @param response
     *            Http response.
     * @return Crawljob.
     */
    protected static CrawlJob getAndCheckJob(CrawlJob job, HttpServletRequest request, HttpServletResponse response) {
        return job;
    }

    /**
     * This methods updates a ComplexType with information passed to it by a
     * HttpServletRequest. It assumes that for every 'simple' type there is a
     * corresponding parameter in the request. A recursive call will be made for
     * any nested ComplexTypes. For each attribute it will check if the relevant
     * override is set (name.override parameter equals 'true'). If so the
     * attribute setting on the specified domain level (settings) will be
     * rewritten. If it is not we well ensure that it isn't being overridden.
     * 
     * @param mbean
     *            The ComplexType to update
     * @param settings
     *            CrawlerSettings for the domain to override setting for. null
     *            denotes the global settings.
     * @param request
     *            The HttpServletRequest to use to update the ComplexType
     * @param expert
     *            if true expert settings will be updated, otherwise they will
     *            be ignored.
     */
    public static void writeNewOrderFile(ComplexType mbean, CrawlerSettings settings, HttpServletRequest request, boolean expert) {
        if (mbean.isTransient() || (mbean.isExpertSetting() && expert == false)) {
            return;
        }
        MBeanAttributeInfo a[] = mbean.getMBeanInfo(settings).getAttributes();
        for (int n = 0; n < a.length; n++) {
            checkAttribute((ModuleAttributeInfo) a[n], mbean, settings, request, expert);
        }
    }

    /**
     * Process passed attribute. Check if needs to be written and if so, write
     * it.
     * 
     * @param att
     *            Attribute to process.
     * @param mbean
     *            The ComplexType to update
     * @param settings
     *            CrawlerSettings for the domain to override setting for. null
     *            denotes the global settings.
     * @param request
     *            The HttpServletRequest to use to update the ComplexType
     * @param expert
     *            if true expert settings will be updated, otherwise they will
     *            be ignored.
     */
    protected static void checkAttribute(ModuleAttributeInfo att, ComplexType mbean, CrawlerSettings settings, HttpServletRequest request, boolean expert) {
        Object currentAttribute = null;
        try {
            currentAttribute = mbean.getAttribute(settings, att.getName());
        } catch (Exception e) {
            logger.severe("Failed getting " + mbean.getAbsoluteName() + " attribute " + att.getName() + ": " + e.getMessage());
            return;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("MBEAN: " + mbean.getAbsoluteName() + " " + att.getName() + " TRANSIENT " + att.isTransient() + " " + att.isExpertSetting() + " " + expert);
        }
        if (att.isTransient() == false && (att.isExpertSetting() == false || expert)) {
            if (currentAttribute instanceof ComplexType) {
                writeNewOrderFile((ComplexType) currentAttribute, settings, request, expert);
            } else {
                String attName = att.getName();
                String attAbsoluteName = mbean.getAbsoluteName() + "/" + attName;
                boolean override = (request.getParameter(attAbsoluteName + ".override") != null) && (request.getParameter(attAbsoluteName + ".override").equals("true"));
                if (settings == null || override) {
                    if (currentAttribute instanceof ListType) {
                        try {
                            ListType list = (ListType) currentAttribute;
                            Class cls = list.getClass();
                            Constructor constructor = cls.getConstructor(String.class, String.class);
                            list = (ListType) constructor.newInstance(list.getName(), list.getDescription());
                            String[] elems = request.getParameterValues(attAbsoluteName);
                            for (int i = 0; elems != null && i < elems.length; i++) {
                                list.add(elems[i]);
                            }
                            writeAttribute(attName, attAbsoluteName, mbean, settings, list);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.severe("Setting new list values on " + attAbsoluteName + ": " + e.getMessage());
                            return;
                        }
                    } else {
                        writeAttribute(attName, attAbsoluteName, mbean, settings, request.getParameter(attAbsoluteName));
                    }
                } else if (settings != null && override == false) {
                    try {
                        mbean.unsetAttribute(settings, attName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.severe("Unsetting attribute on " + attAbsoluteName + ": " + e.getMessage());
                        return;
                    }
                }
            }
        }
    }

    /**
     * Write out attribute.
     * 
     * @param attName
     *            Attribute short name.
     * @param attAbsoluteName
     *            Attribute full name.
     * @param mbean
     *            The ComplexType to update
     * @param settings
     *            CrawlerSettings for the domain to override setting for. null
     *            denotes the global settings.
     * @param value
     *            Value to set into the attribute.
     */
    protected static void writeAttribute(String attName, String attAbsoluteName, ComplexType mbean, CrawlerSettings settings, Object value) {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("MBEAN SET: " + attAbsoluteName + " " + value);
            }
            mbean.setAttribute(settings, new Attribute(attName, value));
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Setting attribute value " + value + " on " + attAbsoluteName + ": " + e.getMessage());
            return;
        }
    }

    /**
     * Check passed job is not null and not readonly.
     * @param job Job to check.
     * @param response Http response.
     * @param redirectBasePath Full path for where to go next if an error.
     * @param currDomain May be null.
     * E.g. "/admin/jobs/per/overview.jsp".
     * @return A job else we've redirected if no job or readonly.
     * @throws IOException
     */
    public static CrawlJob checkCrawlJob(CrawlJob job, HttpServletResponse response, String redirectBasePath, String currDomain) throws IOException {
        if (job == null) {
            response.sendRedirect(redirectBasePath + "?message=No job selected");
        } else if (job.isReadOnly()) {
            response.sendRedirect(redirectBasePath + "?job=" + job.getUID() + ((currDomain != null && currDomain.length() > 0) ? "&currDomain=" + currDomain : "") + "&message=Can't edit a read only job");
        }
        return job;
    }

    /**
     * Handle job action.
     * @param handler CrawlJobHandler to operate on.
     * @param request Http request.
     * @param response Http response.
     * @param redirectBasePath Full path for where to go next if an error.
     * E.g. "/admin/jobs/per/overview.jsp".
     * @param currDomain Current domain.  Pass null for global domain.
     * @param reference 
     * @return The crawljob configured.
     * @throws IOException
     * @throws AttributeNotFoundException
     * @throws InvocationTargetException
     * @throws InvalidAttributeValueException
     */
    public static CrawlJob handleJobAction(CrawlJobHandler handler, HttpServletRequest request, HttpServletResponse response, String redirectBasePath, String currDomain, String reference) throws IOException, AttributeNotFoundException, InvocationTargetException, InvalidAttributeValueException {
        CrawlJob theJob = checkCrawlJob(handler.getJob(request.getParameter("job")), response, redirectBasePath, currDomain);
        XMLSettingsHandler settingsHandler = theJob.getSettingsHandler();
        CrawlerSettings settings = settingsHandler.getSettingsObject(currDomain);
        if (reference != null) {
            Refinement refinement = settings.getRefinement(reference);
            settings = refinement.getSettings();
        }
        if (request.getParameter(ACTION) != null) {
            String action = request.getParameter(ACTION);
            String subaction = request.getParameter(SUBACTION);
            if (action.equals(FILTERS)) {
                String map = request.getParameter(MAP);
                if (map != null && map.length() > 0) {
                    String filter = request.getParameter(FILTER);
                    MapType filterMap = (MapType) settingsHandler.getComplexTypeByAbsoluteName(settings, map);
                    if (subaction.equals(ADD)) {
                        String className = request.getParameter(map + ".class");
                        String typeName = request.getParameter(map + ".name");
                        if (typeName != null && typeName.length() > 0 && className != null && className.length() > 0) {
                            ModuleType tmp = SettingsHandler.instantiateModuleTypeFromClassName(typeName, className);
                            filterMap.addElement(settings, tmp);
                        }
                    } else if (subaction.equals(MOVEUP)) {
                        if (filter != null && filter.length() > 0) {
                            filterMap.moveElementUp(settings, filter);
                        }
                    } else if (subaction.equals(MOVEDOWN)) {
                        if (filter != null && filter.length() > 0) {
                            filterMap.moveElementDown(settings, filter);
                        }
                    } else if (subaction.equals(REMOVE)) {
                        if (filter != null && filter.length() > 0) {
                            filterMap.removeElement(settings, filter);
                        }
                    }
                }
                settingsHandler.writeSettingsObject(settings);
            } else if (action.equals(DONE)) {
                if (subaction.equals(CONTINUE)) {
                    if (theJob.isRunning()) {
                        handler.kickUpdate();
                    }
                    String overParam = ((currDomain != null && currDomain.length() > 0) ? "&currDomain=" + currDomain : "");
                    String refParam = ((reference != null && reference.length() > 0) ? "&reference=" + reference : "");
                    String messageParam = (refParam.length() > 0) ? "&message=Refinement changes saved" : "&message=Override changes saved";
                    response.sendRedirect(redirectBasePath + "?job=" + theJob.getUID() + overParam + refParam + messageParam);
                } else {
                    if (theJob.isNew()) {
                        handler.addJob(theJob);
                        response.sendRedirect(redirectBasePath + "?message=Job created");
                    } else {
                        if (theJob.isRunning()) {
                            handler.kickUpdate();
                        }
                        if (theJob.isProfile()) {
                            response.sendRedirect(redirectBasePath + "?message=Profile modified");
                        } else {
                            response.sendRedirect(redirectBasePath + "?message=Job modified");
                        }
                    }
                }
            } else if (action.equals(GOTO)) {
                String overParam = ((currDomain != null && currDomain.length() > 0) ? "&currDomain=" + currDomain : "");
                String refParam = ((reference != null && reference.length() > 0) ? "&reference=" + reference : "");
                response.sendRedirect(request.getParameter(SUBACTION) + overParam + refParam);
            }
        }
        return theJob;
    }

    /**
     * Print complete seeds list on passed in PrintWriter.
     * @param hndlr Current handler.
     * @param payload What to write out.
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     * @throws IOException
     * @throws IOException
     */
    public static void printOutSeeds(SettingsHandler hndlr, String payload) throws AttributeNotFoundException, MBeanException, ReflectionException, IOException {
        File seedfile = getSeedFile(hndlr);
        writeReader(new StringReader(payload), new BufferedWriter(new FileWriter(seedfile)));
    }

    /**
     * Print complete seeds list on passed in PrintWriter.
     * @param hndlr Current handler.
     * @param out Writer to write out all seeds to.
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     * @throws IOException
     */
    public static void printOutSeeds(SettingsHandler hndlr, Writer out) throws AttributeNotFoundException, MBeanException, ReflectionException, IOException {
        InputStream is = getSeedStream(hndlr);
        writeReader(new BufferedReader(new InputStreamReader(is)), out);
    }

    /**
     * Test whether seeds file is of a size that's reasonable
     * to edit in an HTML textarea. 
     * @param h current settingsHandler
     * @return true if seeds size is manageable, false otherwise
     * @throws AttributeNotFoundException 
     * @throws MBeanException 
     * @throws ReflectionException 
     * 
     */
    public static boolean seedsEdittableSize(SettingsHandler h) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return getSeedFile(h).length() <= (32 * 1024);
    }

    /**
     * @param hndlr Settings handler.
     * @return Seeds file.
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     */
    protected static File getSeedFile(SettingsHandler hndlr) throws AttributeNotFoundException, MBeanException, ReflectionException {
        String seedsFileStr = (String) ((ComplexType) hndlr.getOrder().getAttribute("scope")).getAttribute("seedsfile");
        return hndlr.getPathRelativeToWorkingDirectory(seedsFileStr);
    }

    /**
     * Return seeds as a stream.
     * This method will work for case where seeds are on disk or on classpath.
     * @param hndlr SettingsHandler.  Used to find seeds.txt file.
     * @return InputStream on current seeds file.
     * @throws IOException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws AttributeNotFoundException
     */
    protected static InputStream getSeedStream(SettingsHandler hndlr) throws IOException, AttributeNotFoundException, MBeanException, ReflectionException {
        InputStream is = null;
        File seedFile = getSeedFile(hndlr);
        if (!seedFile.exists()) {
            is = SettingsHandler.class.getResourceAsStream(IoUtils.getClasspathPath(seedFile));
        } else if (seedFile.canRead()) {
            is = new FileInputStream(seedFile);
        }
        if (is == null) {
            throw new IOException(seedFile + " does not" + " exist -- neither on disk nor on CLASSPATH -- or is not" + " readable.");
        }
        return is;
    }

    /**
     * Print complete seeds list on passed in PrintWriter.
     * @param reader File to read seeds from.
     * @param out Writer to write out all seeds to.
     * @throws IOException
     */
    protected static void writeReader(Reader reader, Writer out) throws IOException {
        final int bufferSize = 1024 * 4;
        char[] buffer = new char[bufferSize];
        int read = -1;
        while ((read = reader.read(buffer, 0, bufferSize)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }
}
