package pcgen.gui2.tabs;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import pcgen.base.lang.UnreachableError;
import pcgen.core.facade.CharacterFacade;
import pcgen.io.ExportHandler;
import pcgen.util.Logging;

/**
 *
 * @author Connor Petty <cpmeister@users.sourceforge.net>
 */
public class HtmlSheetSupport {

    private final CharacterFacade character;

    private final File templateFile;

    private final JEditorPane htmlPane;

    private ImageCache cache = new ImageCache();

    private Future refresher = null;

    private boolean installed = false;

    private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("html-sheet-thread");
            return thread;
        }
    });

    public HtmlSheetSupport(CharacterFacade character, JEditorPane htmlPane, File templateFile) {
        this.character = character;
        this.templateFile = templateFile;
        this.htmlPane = htmlPane;
    }

    public void install() {
        installed = true;
        refresh();
    }

    public void uninstall() {
        installed = false;
    }

    public void refresh() {
        if (refresher != null && !refresher.isDone()) {
            refresher.cancel(false);
        }
        refresher = executor.submit(new Refresher());
    }

    private class Refresher extends FutureTask<HTMLDocument> {

        public Refresher() {
            super(new DocumentBuilder());
        }

        @Override
        protected void done() {
            if (!installed || isCancelled()) {
                return;
            }
            try {
                final HTMLDocument doc = get();
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        htmlPane.setDocument(doc);
                    }
                });
            } catch (InvocationTargetException ex) {
                throw new UnreachableError();
            } catch (InterruptedException ex) {
                Logging.errorPrint(null, ex);
            } catch (ExecutionException ex) {
                Logging.errorPrint(null, ex.getCause());
            }
        }
    }

    private class DocumentBuilder implements Callable<HTMLDocument> {

        public HTMLDocument call() throws Exception {
            StringWriter writer = new StringWriter();
            character.export(new ExportHandler(templateFile), new BufferedWriter(writer));
            StringReader reader = new StringReader(writer.toString());
            EditorKit kit = htmlPane.getEditorKit();
            HTMLDocument doc = new HTMLDocument();
            doc.setBase(templateFile.getParentFile().toURL());
            doc.putProperty("imageCache", cache);
            kit.read(reader, doc, 0);
            return doc;
        }
    }

    /**
	 * A cache for images loaded onto the info pane.
	 */
    private static class ImageCache extends Dictionary<URL, Image> {

        private HashMap<URL, Image> cache = new HashMap<URL, Image>();

        @Override
        public int size() {
            return cache.size();
        }

        @Override
        public boolean isEmpty() {
            return cache.isEmpty();
        }

        @Override
        public Enumeration<URL> keys() {
            return Collections.enumeration(cache.keySet());
        }

        @Override
        public Enumeration<Image> elements() {
            return Collections.enumeration(cache.values());
        }

        @Override
        public Image get(Object key) {
            if (!(key instanceof URL)) {
                return null;
            }
            URL src = (URL) key;
            if (!cache.containsKey(src)) {
                Image newImage = Toolkit.getDefaultToolkit().createImage(src);
                if (newImage != null) {
                    ImageIcon ii = new ImageIcon();
                    ii.setImage(newImage);
                }
                cache.put(src, newImage);
            }
            return cache.get(src);
        }

        @Override
        public Image put(URL key, Image value) {
            return cache.put(key, value);
        }

        @Override
        public Image remove(Object key) {
            return cache.remove(key);
        }
    }
}
