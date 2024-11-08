import java.io.*;
import java.util.regex.*;
import java.nio.channels.*;
import java.nio.*;
import java.nio.charset.*;

/**
 * @class Finder
 * 
 * @author Noid
 */
public class Finder implements Runnable {

    public Finder(MainDialog mainDialog) {
        m_State = STATE_STOPPED;
        m_MainDialog = mainDialog;
    }

    public void setRequest(FinderRequest request) {
        m_Request = request;
    }

    public void run() {
        m_State = STATE_RUNNING;
        m_MainDialog.onFinderStarted();
        File startingDir = new File(m_Request.directory());
        if (!startingDir.exists()) {
            m_State = STATE_STOPPED;
            sendEvent(FinderEvent.Code.DIR_NOT_EXISTS, m_Request.directory());
            return;
        }
        if (!startingDir.isDirectory()) {
            m_State = STATE_STOPPED;
            sendEvent(FinderEvent.Code.NOT_A_DIRECTORY, m_Request.directory());
            return;
        }
        findFilesReqursively(startingDir);
        m_State = STATE_STOPPED;
        sendEvent(FinderEvent.Code.COMPLETED);
    }

    private void sendEvent(FinderEvent.Code code) {
        m_MainDialog.onFinderStopped(new FinderEvent(code));
    }

    private void sendEvent(FinderEvent.Code code, String param) {
        m_MainDialog.onFinderStopped(new FinderEvent(code, param));
    }

    private void findFilesReqursively(File currentDir) {
        File[] files = m_Request.filePattern().isEmpty() ? currentDir.listFiles(new RegularFileFilter()) : currentDir.listFiles(new RegexFileFilter(m_Request.filePattern()));
        if (files != null) for (File file : files) {
            if (isStopping()) break;
            if (!Config.instance().isFollowSymLinks() && isLink(file)) continue;
            if (m_Request.text().isEmpty()) m_MainDialog.onFileFound(file.getPath()); else grepFile(file);
        }
        File[] dirs = currentDir.listFiles(new DirectoryFileFilter());
        if (dirs != null) for (File dir : dirs) {
            if (isStopping()) break;
            if (!Config.instance().isFollowSymLinks() && isLink(dir)) continue;
            findFilesReqursively(dir);
        }
    }

    private boolean isLink(File file) {
        try {
            return !file.getAbsolutePath().equals(file.getCanonicalPath());
        } catch (IOException ex) {
            return false;
        }
    }

    private void grepFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            int sz = (int) fc.size();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
            Charset charset = Charset.forName("ISO-8859-15");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer cb = decoder.decode(bb);
            Pattern linePattern = Pattern.compile(".*\r?\n");
            Pattern pattern = Pattern.compile(m_Request.text());
            Matcher lm = linePattern.matcher(cb);
            Matcher pm = null;
            int nLine = 0;
            while (lm.find()) {
                nLine++;
                CharSequence cs = lm.group();
                if (pm == null) pm = pattern.matcher(cs); else pm.reset(cs);
                if (pm.find()) {
                    TextPoint textElement = new TextPoint(file.getPath(), nLine, cs.toString());
                    m_MainDialog.onTextElementFound(textElement);
                }
                if (lm.end() == cb.limit()) break;
            }
            fis.close();
        } catch (IOException ex) {
            return;
        }
    }

    public synchronized void requestThreadStop() {
        m_State = STATE_STOPPING;
    }

    /**
	 * Allows to check if the Finder thread was entered in the
	 * Stopping state. In this state thread will inherently try
	 * to brake its execution as soon as possible.
	 * 
	 * For internal use.
	 */
    private synchronized boolean isStopping() {
        return m_State == STATE_STOPPING;
    }

    /**
	 * Allows to check if the Finder thread was completely stopped
	 * its execution process.
	 */
    public synchronized boolean isStopped() {
        return m_State == STATE_STOPPED;
    }

    public synchronized boolean isRunning() {
        return !isStopped();
    }

    public static int STATE_RUNNING = 1;

    public static int STATE_STOPPING = 2;

    public static int STATE_STOPPED = 3;

    private int m_State;

    FinderRequest m_Request;

    MainDialog m_MainDialog;
}
