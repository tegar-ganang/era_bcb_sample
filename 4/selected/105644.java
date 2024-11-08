package ch.laoe.clip;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.ImageIcon;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GPersistence;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.GToolkit;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			AClipHistory
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	history of old states of this clip.  

History:
Date:			Description:									Autor:
31.08.00		erster Entwurf									oli4
07.11.00		attach to AClip								oli4
20.07.01		unlimited file-based history				oli4
08.12.01		file-modified handling (title and star)	oli4

***********************************************************/
public class AClipHistory {

    /**
	*	constructor
	*/
    public AClipHistory(AClip c) {
        clip = c;
        history = new ArrayList<HistoryElement>();
        lastSaveIndex = 0;
    }

    /**
    * copy-constructor
    */
    public AClipHistory(AClipHistory ch, AClip c) {
        this(c);
        for (int i = 0; i < ch.history.size(); i++) {
            this.history.add(ch.history.get(i));
        }
    }

    private static final boolean historyEnable = GPersistence.createPersistance().getBoolean("history.enable");

    public static boolean isEnabled() {
        return historyEnable;
    }

    private static String historyPath = GToolkit.getLaoeUserHomePath() + "history/";

    private static String historyExtension = ".laoe.tmp";

    private static class HistoryFileFilter implements FileFilter {

        public boolean accept(File file) {
            return file.getName().endsWith(historyExtension);
        }
    }

    static {
        File garbage[] = new File(historyPath).listFiles(new HistoryFileFilter());
        if (garbage != null) {
            for (int i = 0; i < garbage.length; i++) {
                garbage[i].delete();
            }
        }
    }

    /**
    * returns the memory-size used for all histories
    */
    public static long getMemorySize() {
        long mem = 0;
        File files[] = new File(historyPath).listFiles(new HistoryFileFilter());
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                mem += files[i].length();
            }
        }
        return mem;
    }

    private AClip clip;

    private ArrayList<HistoryElement> history;

    private int lastSaveIndex;

    private int actualIndex;

    /**
    *	returns true if the actual history-index corresponds to the
    *	last saved version.
    */
    public boolean hasUnsavedModifications() {
        return (lastSaveIndex != actualIndex);
    }

    /**
    *	if a clip is saved, call this method. 
    */
    public void onSave() {
        lastSaveIndex = actualIndex;
    }

    private class HistoryElement {

        private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.GERMAN);

        public HistoryElement(String d) {
            this(null, d);
        }

        public HistoryElement(ImageIcon icon, String d) {
            this.icon = icon;
            time = dateFormat.format(new Date());
            description = d;
            if (historyEnable) {
                clip.markChange();
                file = new File(historyPath + clip.getChangeId() + historyExtension);
                file.deleteOnExit();
                try {
                    LProgressViewer.getInstance().entrySubProgress(0.5, "storeToHistory", false);
                    Debug.println(3, "history save clip " + clip.getName() + " to file " + file.getName());
                    AClipStorage.saveWithoutSamples(clip, file);
                    for (int i = 0; i < clip.getNumberOfLayers(); i++) {
                        if (LProgressViewer.getInstance().setProgress((i + 1) * 100 / clip.getNumberOfLayers())) return;
                        ALayer l = clip.getLayer(i);
                        LProgressViewer.getInstance().entrySubProgress(0.3, "", false);
                        for (int j = 0; j < l.getNumberOfChannels(); j++) {
                            if (LProgressViewer.getInstance().setProgress((j + 1) * 100 / l.getNumberOfChannels())) return;
                            AChannel ch = l.getChannel(j);
                            File chFile = new File(historyPath + ch.getChangeId() + historyExtension);
                            if (!chFile.exists()) {
                                Debug.println(3, "history save channel " + ch.getName() + " to file " + chFile.getName());
                                chFile.deleteOnExit();
                                AClipStorage.saveSamples(ch, chFile);
                            }
                        }
                        LProgressViewer.getInstance().exitSubProgress();
                    }
                    LProgressViewer.getInstance().exitSubProgress();
                } catch (IOException ioe) {
                    Debug.printStackTrace(5, ioe);
                }
            }
        }

        private File file;

        /**
		 * reloads the clip from file
		 * @param c concerned clip, we have to keep this reference
		 * @return the same clip reference, reloaded
		 */
        public AClip reloadClip(AClip c) {
            if (historyEnable && (c != null)) {
                try {
                    LProgressViewer.getInstance().entrySubProgress(0.5, "reloadFromHistory");
                    Map<String, AChannel> chm = new HashMap<String, AChannel>();
                    for (int i = 0; i < c.getNumberOfLayers(); i++) {
                        for (int j = 0; j < c.getLayer(i).getNumberOfChannels(); j++) {
                            chm.put(c.getLayer(i).getChannel(j).getChangeId(), c.getLayer(i).getChannel(j));
                        }
                    }
                    c.removeAll();
                    AClipStorage.load(c, file);
                    Debug.println(3, "history load clip" + clip.getName() + " from file " + file.getName());
                    for (int i = 0; i < c.getNumberOfLayers(); i++) {
                        if (LProgressViewer.getInstance().setProgress((i + 1) * 100 / c.getNumberOfLayers())) return c;
                        ALayer l = c.getLayer(i);
                        LProgressViewer.getInstance().entrySubProgress(0.3);
                        for (int j = 0; j < l.getNumberOfChannels(); j++) {
                            if (LProgressViewer.getInstance().setProgress((j + 1) * 100 / l.getNumberOfChannels())) return c;
                            AChannel ch = l.getChannel(j);
                            if (chm.containsKey(ch.getChangeId())) {
                                Debug.println(13, "history reuse channel, id=" + ch.getChangeId());
                                ch.setSamples(chm.get(ch.getChangeId()).getSamples());
                            } else {
                                Debug.println(13, "history reload channel, id=" + ch.getChangeId());
                                File chFile = new File(historyPath + ch.getChangeId() + historyExtension);
                                if (chFile.exists()) {
                                    ch.setSamples(AClipStorage.loadSamples(chFile));
                                }
                            }
                        }
                        LProgressViewer.getInstance().exitSubProgress();
                    }
                    chm.clear();
                    LProgressViewer.getInstance().exitSubProgress();
                } catch (IOException ioe) {
                    Debug.printStackTrace(5, ioe);
                }
            }
            return c;
        }

        public AClip reloadClip() {
            return reloadClip(new AClip(0, 0));
        }

        private String time;

        private String description;

        public String getTime() {
            return time;
        }

        public String getDescription() {
            return description;
        }

        private ImageIcon icon;

        public ImageIcon getIcon() {
            return icon;
        }
    }

    /**
	*	call after doing an operation which changes the data of
	*	the AClip
	*/
    public void store(String description) {
        history.add(new HistoryElement(description));
        actualIndex = history.size() - 1;
    }

    /**
	*	call after doing an operation which changes the data of
	*	the AClip
	*/
    public void store(ImageIcon icon, String description) {
        history.add(new HistoryElement(icon, description));
        actualIndex = history.size() - 1;
    }

    private int limitIndex(int index) {
        if (index >= history.size()) {
            return history.size() - 1;
        } else if (index < 0) {
            return 0;
        }
        return index;
    }

    /**
	*	reload an old version of this clip, giving the clip
	*/
    public AClip reloadClip(int index, AClip c) {
        int i = limitIndex(index);
        actualIndex = i;
        return history.get(i).reloadClip(c);
    }

    /**
	*	undo one step into the given clip
	*/
    public AClip undo(AClip c) {
        return reloadClip(--actualIndex, c);
    }

    /**
	*	redo one step into the given clip
	*/
    public AClip redo(AClip c) {
        return reloadClip(++actualIndex, c);
    }

    /**
	*	reload an old version of this clip into a new clip
	*/
    public AClip reloadClip(int index) {
        int i = limitIndex(index);
        actualIndex = i;
        return history.get(i).reloadClip();
    }

    /**
	*	get the element at index
	*/
    public String getDescription(int index) {
        if (index < history.size()) {
            return history.get(index).getDescription();
        }
        return null;
    }

    /**
	*	get the element at index
	*/
    public String getTime(int index) {
        if (index < history.size()) {
            return history.get(index).getTime();
        }
        return null;
    }

    /**
	*	get the element at index
	*/
    public ImageIcon getIcon(int index) {
        if (index < history.size()) {
            return history.get(index).getIcon();
        }
        return null;
    }

    /**
	*	get the stack size
	*/
    public int getStackSize() {
        return history.size();
    }
}
