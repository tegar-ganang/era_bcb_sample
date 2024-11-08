package com.controltier.ctl.tasks;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.filters.ChainableReader;
import org.apache.tools.ant.types.FilterChain;
import org.apache.log4j.Logger;
import com.controltier.ctl.types.HandlerActions;
import com.controltier.ctl.types.Watch;
import com.controltier.ctl.types.Action;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.nio.channels.FileChannel;

/**
 * <p>FileMonitor task performs an ant equivalent of "tail -f | grep",
 * but also allows multiple actions to be defined that occur for
 * multiple possible matches against the input.</p>
 * <p>The &lt;filemonitor&gt; tag
 * is used like this:</p>
 * <p><pre><code>
 * &lt;filemonitor file="${test.file}" timeout="4000" timeoutaction="timeout"&gt;
     * &lt;handleractions&gt;
         * &lt;action name="timeout"&gt;
               &lt;!-- the action tag is a taskcontainer --!&gt;
             * &lt;echo level="info"&gt;timeout occurred&lt;/echo&gt;
         * &lt;/action&gt;
         * &lt;action name="success"&gt;
             * &lt;echo level="info"&gt;success occurred&lt;/echo&gt;
         * &lt;/action&gt;
         * &lt;action name="failure"&gt;
             * &lt;echo level="info"&gt;failure occurred&lt;/echo&gt;
         * &lt;/action&gt;
     * &lt;/handleractions&gt;

     * &lt;watch action="success"&gt;
 *         &lt;!-- watch can contain a single filterchain --&gt;
         * &lt;filterchain&gt;
             * &lt;linecontains&gt;
                * &lt;contains value="Server Started up"/&gt;
             * &lt;/linecontains&gt;
         * &lt;/filterchain&gt;
     * &lt;/watch&gt;
     * &lt;watch action="failure"&gt;
         * &lt;filterchain&gt;
             * &lt;linecontains&gt;
                * &lt;contains value="Exception occurred"/&gt;
             * &lt;/linecontains&gt;
         * &lt;/filterchain&gt;
     * &lt;/watch&gt;
 * &lt;/filemonitor&gt;
 * </code></pre></p>
 * <p>
 * The "handleractions" elements can contain multiple "action" elements.  Each action has a name
 * and contains one or more ant tasks.  When a match occurs on the input, the action
 * with the appropriate name is executed.
 * </p>
 * <p>
 * The filemonitor also contains one or more "watch" elements.  Each watch element
 * defines what action is going to occurr if it matches, via the "action" attribute,
 * as well as what causes it to match, via a "filterchain" element.
 * </p>
 * <p>
 * The file specified by the "file" attribute of Filemonitor is opened, and the
 * input is positioned at the end of the file.  As more data is written to the file,
 * each line is processed by filemonitor.  Each Watch element is tested in turn against
 * the input, and if any one matches, its action declaration is used to execute one of
 * the handleractions.  If no Watch element matches by the time the timeout period is up,
 * then the action named in the "timeoutaction" attribute is used, if any.  If no timeoutaction
 * is defined, then the filemonitor finishes executing without further action.
 * </p>
 *
 * @author Greg Schueler <a href="mailto:greg@controltier.com">greg@controltier.com</a>
 * @version $Revision: 1079 $
 * @ant.task name="filemonitor"
 */
public class FileMonitor extends Task {

    public static final Logger log = Logger.getLogger(FileMonitor.class);

    private HandlerActions handlerActions;

    private File file;

    private long timeout = 0;

    ArrayList watches = new ArrayList();

    private String timeoutaction;

    public void addWatch(Watch watch) {
        watches.add(watch);
    }

    public void addHandlerActions(HandlerActions actions) {
        if (null != this.handlerActions) {
            throw new BuildException("Only one handleractions child allowed");
        }
        this.handlerActions = actions;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Uses the ChainableReaders from the FilterChain, and the starting
     * Reader, to create a new Reader with all of the ChainableReaders
     * chained together.
     * @param chain
     * @param start
     * @return
     */
    private Reader chainReaders(FilterChain chain, Reader start) {
        Vector readers = chain.getFilterReaders();
        Reader result = start;
        for (Iterator j = readers.iterator(); j.hasNext(); ) {
            ChainableReader fr = (ChainableReader) j.next();
            result = fr.chain(result);
        }
        return result;
    }

    /**
     * Execute the FileMonitor action.
     * @throws BuildException
     */
    public void execute() throws BuildException {
        validate();
        long starttime = System.currentTimeMillis();
        String actionToPerform = getTimeoutaction();
        try {
            FileInputStream fis = new FileInputStream(file);
            FileChannel channel = fis.getChannel();
            long size = channel.size();
            channel.position(size);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            boolean done = false;
            while (!done && System.currentTimeMillis() < starttime + getTimeout()) {
                while (br.ready() && !done) {
                    String line = br.readLine();
                    for (Iterator i = watches.iterator(); i.hasNext(); ) {
                        Watch watch = (Watch) i.next();
                        FilterChain chain = watch.getFilterChain();
                        StringReader sr = new StringReader(line);
                        Reader chained = chainReaders(chain, sr);
                        int read = chained.read();
                        if (-1 != read) {
                            log.debug("Watch matched, final action will be: " + watch.getAction());
                            actionToPerform = watch.getAction();
                            done = true;
                            break;
                        }
                    }
                }
                try {
                    Thread.currentThread().sleep(100);
                } catch (InterruptedException e) {
                }
            }
            fis.close();
        } catch (IOException e) {
            throw new BuildException("File not found: " + e.getMessage(), e);
        }
        if (null != actionToPerform) {
            log.debug("Calling action: " + actionToPerform);
            Action act = (Action) getHandlerActions().getActions().get(actionToPerform);
            if (null != act) {
                for (Iterator i = act.getTasks().iterator(); i.hasNext(); ) {
                    Task task = (Task) i.next();
                    task.perform();
                }
            }
        } else {
            log.info("FileMonitor task finishing without calling any action.");
        }
    }

    private void validate() {
        getHandlerActions().validate();
        if ((null != getTimeoutaction() && !"".equals(getTimeoutaction().trim()))) {
            if (null == getHandlerActions().getActions().get(getTimeoutaction())) {
                throw new BuildException("timeoutaction does not name a valid action: " + getHandlerActions().getActions().keySet());
            }
        }
        if (!file.exists() || !file.canRead()) {
            throw new BuildException("specified file does not exist or is not readable: " + file);
        }
        if (timeout < 1) {
            throw new BuildException("timeout value must be greater than 0");
        }
        for (Iterator iterator = watches.iterator(); iterator.hasNext(); ) {
            Watch watch = (Watch) iterator.next();
            watch.validate();
            if (null == getHandlerActions().getActions().get(watch.getAction())) {
                throw new BuildException("Action handler not found for Watch tag, name: " + watch.getAction());
            }
        }
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getTimeoutaction() {
        return timeoutaction;
    }

    public void setTimeoutaction(String timeoutaction) {
        this.timeoutaction = timeoutaction;
    }

    public HandlerActions getHandlerActions() {
        return handlerActions;
    }

    public void setHandlerActions(HandlerActions handlerActions) {
        this.handlerActions = handlerActions;
    }
}
