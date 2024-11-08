package net.laubenberger.bogatyr.service.updater;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.bind.JAXBException;
import net.laubenberger.bogatyr.helper.HelperEnvironment;
import net.laubenberger.bogatyr.helper.HelperIO;
import net.laubenberger.bogatyr.helper.HelperLog;
import net.laubenberger.bogatyr.helper.HelperNet;
import net.laubenberger.bogatyr.helper.HelperObject;
import net.laubenberger.bogatyr.helper.HelperXml;
import net.laubenberger.bogatyr.misc.exception.RuntimeExceptionIsEquals;
import net.laubenberger.bogatyr.misc.exception.RuntimeExceptionIsNull;
import net.laubenberger.bogatyr.model.misc.Platform;
import net.laubenberger.bogatyr.model.updater.ModelUpdater;
import net.laubenberger.bogatyr.model.updater.ModelUpdaterImpl;
import net.laubenberger.bogatyr.service.ServiceAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the updater for documents.
 *
 * @author Stefan Laubenberger
 * @version 0.9.4 (20101126)
 * @since 0.6.0
 */
public class UpdaterImpl extends ServiceAbstract implements Updater {

    private static final Logger log = LoggerFactory.getLogger(UpdaterImpl.class);

    public UpdaterImpl() {
        super();
        if (log.isTraceEnabled()) log.trace(HelperLog.constructor());
    }

    @Override
    public ModelUpdater getDocument(final File file) throws JAXBException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(file));
        final ModelUpdater result = HelperXml.deserialize(file, ModelUpdaterImpl.class);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    @Override
    public ModelUpdater getDocument(final InputStream is) throws JAXBException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(is));
        final ModelUpdater result = HelperXml.deserialize(is, ModelUpdaterImpl.class);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit(result));
        return result;
    }

    @Override
    public void update(final ModelUpdater document, final Platform platform, final File dest) throws IOException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(document, platform, dest));
        if (null == document) {
            throw new RuntimeExceptionIsNull("document");
        }
        if (null == platform) {
            throw new RuntimeExceptionIsNull("platform");
        }
        if (null == dest) {
            throw new RuntimeExceptionIsNull("dest");
        }
        URL location = document.getLocation(platform);
        if (null == location && Platform.ANY != platform) {
            location = document.getLocation(Platform.ANY);
        }
        if (null == location) {
            throw new IllegalArgumentException("no valid location found");
        }
        final File source = new File(location.getFile());
        if (source.exists()) {
            if (HelperObject.isEquals(source, dest)) {
                throw new RuntimeExceptionIsEquals("location", "dest");
            }
            HelperIO.copy(source, dest);
        } else {
            HelperIO.writeFile(dest, HelperNet.readUrl(location), false);
        }
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit());
    }

    @Override
    public void update(final ModelUpdater document, final File dest) throws IOException {
        if (log.isDebugEnabled()) log.debug(HelperLog.methodStart(document, dest));
        if (null == document) {
            throw new RuntimeExceptionIsNull("document");
        }
        update(document, null == document.getLocation(HelperEnvironment.getPlatform()) ? Platform.ANY : HelperEnvironment.getPlatform(), dest);
        if (log.isDebugEnabled()) log.debug(HelperLog.methodExit());
    }
}
