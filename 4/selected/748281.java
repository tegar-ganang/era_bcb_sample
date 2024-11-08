package pcgen.gui2.tabs.summary;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import pcgen.core.facade.CharacterFacade;
import pcgen.core.facade.CharacterLevelFacade;
import pcgen.core.facade.GameModeFacade;
import pcgen.core.facade.StatFacade;
import pcgen.core.facade.event.ListEvent;
import pcgen.core.facade.event.ListListener;
import pcgen.core.facade.event.ReferenceEvent;
import pcgen.core.facade.event.ReferenceListener;
import pcgen.gui2.util.SwingWorker;
import pcgen.io.ExportHandler;
import pcgen.system.LanguageBundle;
import pcgen.util.Logging;

/**
 * Manages the information pane of the summary tab. This is an output sheet 
 * that is displayed in the summary tab to advise the user of important 
 * stats for their character. The output sheet to be displayed is specified in 
 * the game mode miscinfo.lst file using the INFOSHEET tag.
 * <br/>
 * Last Editor: $Author:  $
 * Last Edited: $Date:  $
 *    
 * @author Connor Petty <cpmeister@users.sourceforge.net>
 * @version $Revision: $
 */
public class InfoPaneHandler implements ReferenceListener<Object>, ListListener<CharacterLevelFacade> {

    private boolean installed = false;

    private Thread refresher = null;

    private ImageCache cache = new ImageCache();

    private JEditorPane htmlPane;

    private String currentInfoTemplateFile;

    private CharacterFacade character;

    /**
	 * Create a new info pane handler instance for a character.
	 * @param character The character the pane is to display information for.
	 */
    public InfoPaneHandler(CharacterFacade character) {
        this.character = character;
        GameModeFacade game = character.getDataSet().getGameMode();
        currentInfoTemplateFile = game.getInfoSheet();
        registerListeners();
    }

    /**
	 * Initialise our display component. Any expected UI behaviour/
	 * configuration is enforced here. Note that this is a utility function for
	 * use by SummaryInfoTab. While there is a handler for each character 
	 * displayed, there is only a single instance of each display component. 
	 * 
	 * @param htmlPane The editor panel that will display the sheet.
	 */
    public static void initializeEditorPane(JEditorPane htmlPane) {
        htmlPane.setOpaque(false);
        htmlPane.setEditable(false);
        htmlPane.setFocusable(false);
        htmlPane.setContentType("text/html");
    }

    /**
	 * Link this handler with our display component and schedule a refresh of 
	 * the contents for the character. 
	 * @param htmlPane The display component
	 */
    public void install(JEditorPane htmlPane) {
        this.htmlPane = htmlPane;
        installed = true;
        scheduleRefresh();
    }

    /**
	 * Register with the things we want to be notified of changes about. 
	 */
    private void registerListeners() {
        character.getRaceRef().addReferenceListener(this);
        character.getGenderRef().addReferenceListener(this);
        character.getAlignmentRef().addReferenceListener(this);
        for (StatFacade stat : character.getDataSet().getStats()) {
            character.getScoreBaseRef(stat).addReferenceListener(this);
        }
        character.getCharacterLevelsFacade().addListListener(this);
        character.getHandedRef().addReferenceListener(this);
        character.getAgeRef().addReferenceListener(this);
    }

    /**
	 * Start an update of the contents of the info pane for this character. The
	 * update will happen in a new thread and will not be started if one is 
	 * already running.  
	 */
    public void scheduleRefresh() {
        if (refresher == null || !refresher.isAlive()) {
            if (currentInfoTemplateFile != null) {
                refresher = new Thread(new TempInfoPaneRefresher());
                refresher.start();
            } else {
                showMisingSheetMsg();
            }
        }
    }

    private void showMisingSheetMsg() {
        htmlPane.setText(LanguageBundle.getFormattedString("in_sumNoInfoSheet", character.getDataSet().getGameMode().getName()));
    }

    /**
	 * Register that we are no longer the active character. 
	 */
    public void uninstall() {
        installed = false;
    }

    public void referenceChanged(ReferenceEvent<Object> e) {
        scheduleRefresh();
    }

    public void elementAdded(ListEvent<CharacterLevelFacade> e) {
        scheduleRefresh();
    }

    public void elementRemoved(ListEvent<CharacterLevelFacade> e) {
        scheduleRefresh();
    }

    public void elementsChanged(ListEvent<CharacterLevelFacade> e) {
        scheduleRefresh();
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

    private class TempInfoPaneRefresher implements Runnable {

        private File templateFile = new File(currentInfoTemplateFile);

        public void run() {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        StringWriter writer = new StringWriter();
                        character.export(new ExportHandler(templateFile), new BufferedWriter(writer));
                        StringReader reader = new StringReader(writer.toString());
                        EditorKit kit = htmlPane.getEditorKit();
                        HTMLDocument doc = new HTMLDocument();
                        try {
                            doc.setBase(templateFile.getParentFile().toURL());
                            doc.putProperty("imageCache", cache);
                            kit.read(reader, doc, 0);
                        } catch (IOException ex) {
                            Logging.errorPrint("Could not get parent of load template file " + "for info panel.", ex);
                        } catch (BadLocationException ex) {
                        }
                        if (installed) {
                            htmlPane.setDocument(doc);
                        }
                    }
                });
            } catch (InterruptedException ex) {
            } catch (InvocationTargetException ex) {
            }
        }
    }

    /**
	 * Refreshes the contents of the info panel by processing the export template sheet
	 * for the character.
	 */
    private class InfoPaneRefresher extends SwingWorker<HTMLDocument> implements Runnable {

        private File templateFile = new File(currentInfoTemplateFile);

        private PipedReader reader = new PipedReader();

        public void run() {
            PipedWriter writer = null;
            try {
                writer = new PipedWriter(reader);
                start();
                character.export(new ExportHandler(templateFile), new BufferedWriter(writer, 1));
            } catch (IOException ex) {
                Logging.errorPrint("Unable to construct piped writer", ex);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException ex) {
                    Logging.errorPrint("Unable to close PipedWriter", ex);
                }
            }
        }

        @Override
        public HTMLDocument construct() {
            EditorKit kit = htmlPane.getEditorKit();
            HTMLDocument doc = new HTMLDocument();
            try {
                doc.setBase(templateFile.getParentFile().toURL());
                doc.putProperty("imageCache", cache);
                kit.read(reader, doc, 0);
            } catch (IOException ex) {
                Logging.errorPrint("Could not get parent of load template file for info panel.", ex);
            } catch (BadLocationException ex) {
            } finally {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Logging.errorPrint("Unable to close PipedReader", ex);
                }
            }
            return doc;
        }

        @Override
        public void finished() {
            if (installed) {
                htmlPane.setDocument(getValue());
            }
        }
    }
}
