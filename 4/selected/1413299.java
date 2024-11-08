package net.sf.xdc.processing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import net.sf.xdc.util.IOUtils;
import net.sf.xdc.util.Logging;
import org.apache.log4j.Logger;

/**
 * This class represents a "package" of sources. This is essentially a
 * directory containing one or more XML files to be processed by XDC.
 *
 * @author Jens Vo�
 * @since 0.5
 * @version 0.5
 */
public class XdcPackage {

    private static final Logger LOG = Logging.getLogger();

    private String name;

    private String summaryText;

    private SortedMap xdcSources = new TreeMap();

    /**
   * Public constructor.
   *
   * @param name The name of the package. This amounts to the last part of
   *         the source directory path.
   */
    public XdcPackage(String name) {
        this.name = name;
    }

    /**
   * Getter method for this <code>XdcPackage</code>'s name.
   *
   * @return The name of this <code>XdcPackage</code>
   */
    public String getName() {
        return name;
    }

    /**
   * This method is used to register an <code>XdcSource</code> with this
   * <code>XdcPackage</code>.
   *
   * @param xdcSource An <code>XdcSource</code> object to be registered with
   *         this <code>XdcPackage</code>
   */
    public void addSource(XdcSource xdcSource) {
        xdcSources.put(xdcSource.getFile().getName(), xdcSource);
    }

    /**
   * This method is used to unregister an <code>XdcSource</code> from this
   * <code>XdcPackage</code>.
   *
   * @param xdcSource An <code>XdcSource</code> object to be unregistered from
   *         this <code>XdcPackage</code>
   */
    public void removeSource(XdcSource xdcSource) {
        xdcSources.remove(xdcSource);
    }

    /**
   * This method retrieves all registered <code>XdcSource</code> objects.
   *
   * @return An array of all <code>XdcSource</code> objects registered with
   *          this <code>XdcPackage</code>
   */
    public XdcSource[] getXdcSources() {
        return (XdcSource[]) xdcSources.values().toArray(new XdcSource[xdcSources.size()]);
    }

    /**
   * This method retrieves a particular <code>XdcSource</code> object
   * from this <code>XdcPackage</code> by its (non-qualified) name.
   *
   * @param sourceName The name of the <code>XdcSource</code> to be
   *        retrieved
   * @return The <code>XdcSource</code> of this <code>XdcPackage</code>
   *         with the name <code>sourceName</code> 
   */
    public XdcSource getXdcSource(String sourceName) {
        return (XdcSource) xdcSources.get(sourceName);
    }

    /**
   * This method retrieves the relevant package summary file text. This text is
   * the portion between the opening and the closing &lt;body&gt; tag of
   * a file named <em>xdc-package.html</em> which is placed in this
   * <code>XdcPackage</code>'s source directory.
   *
   * @return The relevant text from the package summary file. Note that this
   *          text is returned "raw", i.e. with XDC tags not yet resolved.
   */
    public String getSummaryText() {
        if (summaryText == null) {
            for (Iterator iter = xdcSources.values().iterator(); iter.hasNext(); ) {
                XdcSource source = (XdcSource) iter.next();
                File packageFile = new File(source.getFile().getParentFile(), "xdc-package.html");
                if (packageFile.exists()) {
                    Reader in = null;
                    try {
                        in = new FileReader(packageFile);
                        StringWriter out = new StringWriter();
                        IOUtils.copy(in, out);
                        StringBuffer buf = out.getBuffer();
                        int pos1 = buf.indexOf("<body>");
                        int pos2 = buf.lastIndexOf("</body>");
                        if (pos1 >= 0 && pos1 < pos2) {
                            summaryText = buf.substring(pos1 + 6, pos2);
                        } else {
                            summaryText = "";
                        }
                    } catch (FileNotFoundException e) {
                        LOG.error(e.getMessage(), e);
                        summaryText = "";
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                        summaryText = "";
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                    }
                    break;
                } else {
                    summaryText = "";
                }
            }
        }
        return summaryText;
    }
}
