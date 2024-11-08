package bgu.nlp.context;

import bgu.nlp.context.vo.Context;
import bgu.nlp.utils.FileUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ziv
 *
 * Will treat all context-IDs as files in its root directory
 */
public class ContextPersistanceDir implements ContextPersistance {

    private static final Logger log = Logger.getLogger(ContextPersistanceDir.class.getName());

    private static final XStream MAP_XSTREAM = new XStream();

    private static final int cacheLimit = 40;

    private static final Map<String, AnalyzedContext> cache = new HashMap<String, AnalyzedContext>(cacheLimit);

    private File rootDir = null;

    public void init(final String rootDir) {
        this.rootDir = new File(rootDir);
    }

    public AnalyzedContext getAnalyzedData(final Context context) {
        final String contextId = context.getContextId();
        final AnalyzedContext cahced = cache.get(contextId);
        final AnalyzedContext analyzedData;
        if (cahced == null) {
            analyzedData = getAnalyzedData(context, contextId);
            if (cache.size() > cacheLimit) {
                removeSmart();
            }
            cache.put(contextId, analyzedData);
        } else {
            analyzedData = cahced;
        }
        return analyzedData;
    }

    protected void removeSmart() {
        final Set<Entry<String, AnalyzedContext>> entrySet = cache.entrySet();
        final Iterator<Entry<String, AnalyzedContext>> iterator = entrySet.iterator();
        Entry<String, AnalyzedContext> entry = iterator.next();
        String smallestKey = entry.getKey();
        long smallestSize = entry.getValue().getSize();
        while (smallestSize > 100 && iterator.hasNext()) {
            entry = iterator.next();
            final long size = entry.getValue().getSize();
            if (size < smallestSize) {
                smallestKey = entry.getKey();
                smallestSize = size;
            }
        }
        cache.remove(smallestKey);
    }

    public AnalyzedContext getAnalyzedData(final Context context, final String contextFileName) {
        final File savedConcordance = new File(rootDir, contextFileName);
        final AnalyzedContext analyzed = getAnalyzedData(context, savedConcordance);
        return analyzed;
    }

    public AnalyzedContext getAnalyzedData(final Context context, final File savedConcordance) {
        final AnalyzedContext analyzed;
        if (!savedConcordance.exists()) analyzed = buildAnalyzedNewData(context, savedConcordance); else analyzed = getAnalyzedExistingData2(context, savedConcordance);
        return analyzed;
    }

    public AnalyzedContext getAnalyzedExistingData2(final Context context, final File savedConcordance) {
        log.entering("Persistance", "getAnalyzedExistingData", savedConcordance.getName());
        final Reader reader = FileUtils.buildReader(savedConcordance);
        try {
            final Object mapObj = MAP_XSTREAM.fromXML(reader);
            final Map<String, Integer> map = (Map<String, Integer>) mapObj;
            final AnalyzedContext analyzed = new AnalyzedContextByMap(map);
            log.exiting("Persistance", "getAnalyzedExistingData", "Map of size " + analyzed.getSize());
            return analyzed;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to read from file: " + savedConcordance.getName() + ". Re-write file.", ex);
            try {
                reader.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, null, e);
            }
            final AnalyzedContext analyzed = buildAnalyzedNewData(context, savedConcordance);
            return analyzed;
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
    }

    public AnalyzedContext getAnalyzedExistingData(final Context context, final File savedConcordance) {
        log.entering("Persistance", "getAnalyzedExistingData", savedConcordance.getName());
        AnalyzedContext analyzed = null;
        try {
            analyzed = new AnalyzedContextByXpath(savedConcordance);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to read from file: " + savedConcordance.getName() + ". Re-write file.", e);
            analyzed = buildAnalyzedNewData(context, savedConcordance);
        }
        log.exiting("Persistance", "getAnalyzedExistingData", "Map of size " + analyzed.getSize());
        return analyzed;
    }

    public static void saveAnalyzedData(final Map<String, Integer> analyzed, final File savedConcordance) {
        final Writer writer = FileUtils.buildWriter(savedConcordance);
        try {
            MAP_XSTREAM.toXML(analyzed, writer);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
    }

    public AnalyzedContext buildAnalyzedNewData(final Context context, final File savedConcordance) {
        if (log.isLoggable(Level.FINER)) log.entering("Persistance", "buildAnalyzedNewData", savedConcordance.getName());
        final Collection<Context> children = context.getChildren();
        final AnalyzedContext analyzed;
        if (children == null) analyzed = buildAnalyzedNewDataByIteration(context, savedConcordance); else analyzed = buildAnalyzedNewDataByChildren(context, savedConcordance);
        if (log.isLoggable(Level.FINER)) log.entering("Persistance", "buildAnalyzedNewData", "Map of size " + analyzed.getSize() + " ; children: " + children);
        return analyzed;
    }

    public AnalyzedContext buildAnalyzedNewDataByIteration(final Context context, final File savedConcordance) {
        final Map<String, Integer> analyzed = new HashMap<String, Integer>(100);
        final Iterator<String> iterator = context.getIterator();
        while (iterator.hasNext()) {
            final String token = iterator.next();
            final Integer counter = analyzed.get(token);
            final Integer newCounter = counter == null ? 1 : counter + 1;
            analyzed.put(token, newCounter);
        }
        saveAnalyzedData(analyzed, savedConcordance);
        final AnalyzedContextByMap byMap = new AnalyzedContextByMap(analyzed);
        return byMap;
    }

    public AnalyzedContext buildAnalyzedNewDataByChildren(final Context parentContext, final File savedConcordance) {
        final AggregatedContext parentAnalyzed = new AggregatedContext();
        final Collection<Context> children = parentContext.getChildren();
        for (final Context childContext : children) {
            final AnalyzedContext childAnalyzed = getAnalyzedData(childContext);
            parentAnalyzed.add(childAnalyzed);
        }
        return parentAnalyzed;
    }
}
