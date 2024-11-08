package net.sf.genomeview.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.sf.genomeview.core.Configuration;
import net.sf.genomeview.gui.CrashHandler;
import net.sf.genomeview.gui.dialog.Hider;
import net.sf.jannot.source.DataSource;
import net.sf.jannot.source.DataSourceFactory;
import net.sf.jannot.source.Locator;
import be.abeel.io.LineIterator;
import be.abeel.net.URIFactory;

/**
 * 
 * @author Thomas Abeel
 * 
 */
public class Session {

    public static void loadSession(Model model, String in) throws IOException {
        if (in.startsWith("http://") || in.startsWith("https://")) {
            try {
                loadSession(model, URIFactory.url(in));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            loadSession(model, new File(in));
        }
    }

    public static void loadSession(Model model, File selectedFile) throws FileNotFoundException {
        loadSession(model, new FileInputStream(selectedFile));
    }

    public static void loadSession(Model model, URL url) throws IOException {
        loadSession(model, url.openStream());
    }

    private static Logger log = Logger.getLogger(Session.class.getCanonicalName());

    private static void loadSession(final Model model, final InputStream is) {
        model.messageModel().setStatusBarMessage("Preparing to load session, retrieving session file.");
        new Thread(new Runnable() {

            @Override
            public void run() {
                LineIterator it = new LineIterator(is, false, true);
                try {
                    String key = it.next();
                    if (!key.startsWith("##GenomeView session")) {
                        JOptionPane.showMessageDialog(model.getGUIManager().getParent(), "The selected file is not a GenomeView session");
                    } else {
                        model.clearEntries();
                        for (String line : it) {
                            char c = line.charAt(0);
                            line = line.substring(2);
                            model.messageModel().setStatusBarMessage("Loading session, current file: " + line + "...");
                            switch(c) {
                                case 'U':
                                case 'F':
                                    try {
                                        DataSourceHelper.load(model, new Locator(line));
                                    } catch (RuntimeException re) {
                                        log.log(Level.SEVERE, "Something went wrong while loading line: " + line + "\n\tfrom the session file.\n\tTo recover GenomeView skipped this file.", re);
                                    }
                                    break;
                                case 'C':
                                    Configuration.loadExtra(URIFactory.url(line).openStream());
                                default:
                                    log.info("Could not load session line: " + line);
                                    break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    CrashHandler.crash(Level.SEVERE, "Could not load session", ex);
                }
                it.close();
                model.messageModel().setStatusBarMessage(null);
            }
        }).start();
    }
}
