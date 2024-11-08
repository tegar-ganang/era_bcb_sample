package net.sourceforge.vietpad;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.channels.*;
import java.text.BreakIterator;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;
import net.sourceforge.vietpad.converter.*;
import net.sourceforge.vietpad.inputmethod.VietKeyListener;
import net.sourceforge.vietpad.utilities.VietUtilities;

/**
 *  Implementation of conversion and sort functions
 *
 *@author     Quan Nguyen
 *@author     Gero Herrmann
 *@created    February 11, 2002
 *@version    1.8, 5 May 2007
 */
public class VietPadWithTools extends VietPadWithInputMethod {

    private ConvertDialog convertDlg;

    private SortDialog sortDlg;

    private PreferencesDialog preferencesDlg;

    private boolean diacriticsPosClassicOn, repeatKeyConsumed, spellCheckForeignLangEnabled;

    private Map<String, String> map;

    private long mapLastModified = Long.MIN_VALUE;

    /**
     *  Creates a new instance of VietPadWithTools
     */
    public VietPadWithTools() {
        super();
        diacriticsPosClassicOn = prefs.getBoolean("diacriticsPosClassic", true);
        repeatKeyConsumed = prefs.getBoolean("repeatKeyConsumed", false);
        spellCheckForeignLangEnabled = prefs.getBoolean("spellCheckForeignLangEnabled", false);
        try {
            VietKeyListener.setDiacriticsPosClassic(diacriticsPosClassicOn);
            VietKeyListener.consumeRepeatKey(repeatKeyConsumed);
        } catch (java.lang.NoSuchMethodError e) {
            e.printStackTrace();
        }
        menuBar.add(createToolsMenu(), menuBar.getMenuCount() - 1);
        if (MAC_OS_X) {
            new MacOSXApplication(this);
        }
        setVisible(true);
    }

    /**
     *  Creates the Tools menu
     *
     *@return    The menu
     */
    private JMenu createToolsMenu() {
        JMenu mTools = new JMenu(myResources.getString("Tools"));
        mTools.setMnemonic('T');
        Action spellAction = new AbstractAction(myResources.getString("Spell_Check") + "...") {

            @Override
            public void actionPerformed(ActionEvent e) {
                undoSupport.beginUpdate();
                VietUtilities.spellCheck(m_editor, myResources.getString("Spell_Check"), supportDir, spellCheckForeignLangEnabled);
                undoSupport.endUpdate();
            }
        };
        JMenuItem item = mTools.add(spellAction);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        mTools.addSeparator();
        Action convertAction = new AbstractAction(myResources.getString("Convert_to_Unicode") + "...") {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (convertDlg == null) {
                    convertDlg = new ConvertDialog(VietPadWithTools.this, false);
                    convertDlg.setSelectedIndex(prefs.getInt("convertIndex", 0));
                    convertDlg.setLocation(prefs.getInt("convertX", convertDlg.getX()), prefs.getInt("convertY", convertDlg.getY()));
                }
                if (m_currentFile != null) {
                    convertDlg.checkHTML(m_currentFile.getName().toLowerCase().matches(".+\\.html?"));
                }
                if (m_editor.getSelectedText() == null) {
                    m_editor.selectAll();
                }
                convertDlg.setVisible(true);
            }
        };
        item = mTools.add(convertAction);
        item.setMnemonic('u');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, MENU_MASK));
        Action viqrAction = new AbstractAction(myResources.getString("Convert_to_VIQR")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (m_editor.getSelectedText() == null) {
                    m_editor.selectAll();
                    if (m_editor.getSelectedText() == null) {
                        return;
                    }
                }
                if (!isUnicode(m_editor.getSelectedText())) {
                    if (JOptionPane.CANCEL_OPTION == JOptionPane.showConfirmDialog(VietPadWithTools.this, myResources.getString("The_text_appears_to_be_already_in_VIQR_format.\nDo_you_still_want_to_proceed?"), myResources.getString("Convert_to_VIQR"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
                        return;
                    }
                }
                String result = VietUtilities.convert(m_editor.getSelectedText(), "Unicode", false);
                undoSupport.beginUpdate();
                int start = m_editor.getSelectionStart();
                m_editor.replaceSelection(result);
                setSelection(start, start + result.length());
                undoSupport.endUpdate();
            }
        };
        item = mTools.add(viqrAction);
        item.setMnemonic('q');
        mTools.addSeparator();
        Action stripAction = new AbstractAction(myResources.getString("Strip_Diacritics")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (m_editor.getSelectedText() == null) {
                    m_editor.selectAll();
                    if (m_editor.getSelectedText() == null) {
                        return;
                    }
                }
                String result = VietUtilities.stripDiacritics(m_editor.getSelectedText());
                undoSupport.beginUpdate();
                int start = m_editor.getSelectionStart();
                m_editor.replaceSelection(result);
                setSelection(start, start + result.length());
                undoSupport.endUpdate();
            }
        };
        item = mTools.add(stripAction);
        item.setMnemonic('d');
        Action addAction = new AbstractAction(myResources.getString("Add_Diacritics")) {

            @Override
            public void actionPerformed(ActionEvent event) {
                if (m_editor.getSelectedText() == null) {
                    m_editor.selectAll();
                    if (m_editor.getSelectedText() == null) {
                        return;
                    }
                }
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                getGlassPane().setVisible(true);
                try {
                    String result = addDiacritics(m_editor.getSelectedText());
                    undoSupport.beginUpdate();
                    int start = m_editor.getSelectionStart();
                    m_editor.replaceSelection(result);
                    setSelection(start, start + result.length());
                    undoSupport.endUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            getGlassPane().setVisible(false);
                        }
                    });
                }
            }
        };
        item = mTools.add(addAction);
        item.setMnemonic('a');
        Action normalizeAction = new AbstractAction(myResources.getString("Normalize_Diacritics")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (m_editor.getSelectedText() == null) {
                    m_editor.selectAll();
                    if (m_editor.getSelectedText() == null) {
                        return;
                    }
                }
                String result = VietUtilities.normalizeDiacritics(m_editor.getSelectedText(), diacriticsPosClassicOn);
                undoSupport.beginUpdate();
                int start = m_editor.getSelectionStart();
                m_editor.replaceSelection(result);
                setSelection(start, start + result.length());
                undoSupport.endUpdate();
            }
        };
        item = mTools.add(normalizeAction);
        item.setMnemonic('n');
        mTools.addSeparator();
        Action sortAction = new AbstractAction(myResources.getString("Sort_Lines") + "...") {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (sortDlg == null) {
                    sortDlg = new SortDialog(VietPadWithTools.this, false);
                    sortDlg.setReverse(prefs.getBoolean("sortReverse", false));
                    sortDlg.setLocation(prefs.getInt("sortX", sortDlg.getX()), prefs.getInt("sortY", sortDlg.getY()));
                }
                if (m_editor.getSelectedText() == null) {
                    m_editor.selectAll();
                }
                sortDlg.setVisible(true);
            }
        };
        item = mTools.add(sortAction);
        item.setMnemonic('s');
        if (!MAC_OS_X) {
            mTools.addSeparator();
            Action optionsAction = new AbstractAction(myResources.getString("Preferences")) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    preferences();
                }
            };
            item = mTools.add(optionsAction);
            item.setMnemonic('r');
        }
        return mTools;
    }

    /**
     *  Adds diacritics to unmarked Viet text
     *
     *@param  source  Plain text to be marked
     *@return         Text with diacritics added
     */
    public String addDiacritics(String source) {
        loadMap();
        StringBuffer strB = new StringBuffer(source.toLowerCase());
        BreakIterator boundary = BreakIterator.getWordInstance();
        boundary.setText(source);
        int length = source.length();
        int start = boundary.first();
        for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
            if (!Character.isLetter(strB.charAt(start))) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                if (end < length) {
                    end = boundary.next();
                } else {
                    break;
                }
                char ch = strB.charAt(end - 1);
                if (!Character.isLetter(ch) && ch != ' ') {
                    break;
                }
            }
            String key = strB.substring(start, end);
            while (!Character.isLetter(key.charAt(key.length() - 1))) {
                end = boundary.previous();
                if (start == end) {
                    break;
                }
                key = strB.substring(start, end);
            }
            if (map.containsKey(key)) {
                strB.replace(start, end, map.get(key));
            } else {
                if (end > start) {
                    end = boundary.previous();
                }
                if (end > start) {
                    end = boundary.previous();
                }
                key = strB.substring(start, end);
                if (map.containsKey(key)) {
                    strB.replace(start, end, map.get(key));
                } else {
                    if (end > start) {
                        end = boundary.previous();
                    }
                    if (end > start) {
                        end = boundary.previous();
                    }
                    key = strB.substring(start, end);
                    if (map.containsKey(key)) {
                        strB.replace(start, end, map.get(key));
                    }
                }
            }
            end = boundary.next();
        }
        for (int i = 0; i < length; i++) {
            if (Character.isUpperCase(source.charAt(i))) {
                strB.setCharAt(i, Character.toUpperCase(strB.charAt(i)));
            }
        }
        return VietUtilities.normalizeDiacritics(strB.toString(), diacriticsPosClassicOn);
    }

    /**
     *  Reads Vietnamese wordlist for Add Diacritics function
     */
    private void loadMap() {
        final String wordList = "vietwordlist.txt";
        try {
            File dataFile = new File(supportDir, wordList);
            if (!dataFile.exists()) {
                final ReadableByteChannel input = Channels.newChannel(ClassLoader.getSystemResourceAsStream("dict/" + dataFile.getName()));
                final FileChannel output = new FileOutputStream(dataFile).getChannel();
                output.transferFrom(input, 0, 1000000L);
                input.close();
                output.close();
            }
            long fileLastModified = dataFile.lastModified();
            if (map == null) {
                map = new HashMap<String, String>();
            } else {
                if (fileLastModified <= mapLastModified) {
                    return;
                }
                map.clear();
            }
            mapLastModified = fileLastModified;
            BufferedReader bs = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
            String accented;
            while ((accented = bs.readLine()) != null) {
                String plain = VietUtilities.stripDiacritics(accented);
                map.put(plain.toLowerCase(), accented);
            }
            bs.close();
        } catch (IOException e) {
            map = null;
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, myResources.getString("Cannot_find_\"") + wordList + myResources.getString("\"_in\n") + supportDir.toString(), VietPad.APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *  Converts to Unicode
     *
     *@param  sourceEncoding  Source encoding
     *@param  html      True for HTML source
     */
    protected void convert(final String sourceEncoding, final boolean html) {
        if (m_editor.getSelectedText() == null) {
            m_editor.selectAll();
            return;
        }
        if (sourceEncoding.equals(VietEncodings.VIQR.toString()) && isUnicode(m_editor.getSelectedText())) {
            if (JOptionPane.CANCEL_OPTION == JOptionPane.showConfirmDialog(VietPadWithTools.this, myResources.getString("The_text_appears_to_be_already_in_Unicode_format.\nDo_you_still_want_to_proceed?"), myResources.getString("Convert_to_Unicode"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
                return;
            }
        }
        String result = VietUtilities.convert(m_editor.getSelectedText(), sourceEncoding, html);
        undoSupport.beginUpdate();
        int start = m_editor.getSelectionStart();
        m_editor.replaceSelection(result);
        setSelection(start, start + result.length());
        undoSupport.endUpdate();
    }

    /**
     *  Sorts lines
     *
     *@param  reverse    True for reverse sorting
     *@param  delimiter  Delimiter for Left-to-Right sorting; empty if not needed
     */
    protected void sort(final boolean reverse, final String delimiter) {
        if (m_editor.getSelectedText() == null) {
            m_editor.selectAll();
            return;
        }
        int start = m_editor.getSelectionStart();
        if (start != 0 && m_editor.getText().charAt(start - 1) != '\n') {
            try {
                int lineStart = m_editor.getLineStartOffset(m_editor.getLineOfOffset(start));
                start = lineStart;
                m_editor.setSelectionStart(start);
            } catch (BadLocationException e) {
                System.err.println(e);
            }
        }
        int end = m_editor.getSelectionEnd();
        if (end != m_editor.getDocument().getLength() && m_editor.getText().charAt(end) != '\n') {
            try {
                int lineEnd = m_editor.getLineEndOffset((m_editor.getLineOfOffset(end)));
                if (m_editor.getDocument().getLength() == lineEnd) {
                    end = lineEnd;
                } else {
                    end = lineEnd - 1;
                }
                m_editor.setSelectionEnd(end);
            } catch (BadLocationException e) {
                System.err.println(e);
            }
        }
        String[] words = m_editor.getSelectedText().split("\n");
        VietUtilities.sort(words, reverse, delimiter);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < words.length; i++) {
            result.append(words[i]).append("\n");
        }
        result.setLength(result.length() - 1);
        undoSupport.beginUpdate();
        m_editor.replaceSelection(result.toString());
        setSelection(start, start + result.length());
        undoSupport.endUpdate();
    }

    /**
     *  Retrieves options values from Preferences Dialog
     */
    protected void retrieveOptions() {
        defaultEOLOn = preferencesDlg.isDefaultEOL();
        diacriticsPosClassicOn = preferencesDlg.isDiacriticsPosClassic();
        repeatKeyConsumed = preferencesDlg.isRepeatKeyConsumed();
        localeVietOn = preferencesDlg.isLocaleVN();
        spellCheckForeignLangEnabled = preferencesDlg.isForeignLangSelected();
        try {
            VietKeyListener.setDiacriticsPosClassic(diacriticsPosClassicOn);
            VietKeyListener.consumeRepeatKey(repeatKeyConsumed);
        } catch (java.lang.NoSuchMethodError e) {
            e.printStackTrace();
        }
    }

    /**
     *  Updates UI component if changes in LAF
     *
     *@param  laf  The look and feel class name
     */
    @Override
    protected void updateLaF(String laf) {
        super.updateLaF(laf);
        if (convertDlg != null) {
            SwingUtilities.updateComponentTreeUI(convertDlg);
            convertDlg.pack();
        }
        if (sortDlg != null) {
            SwingUtilities.updateComponentTreeUI(sortDlg);
            sortDlg.pack();
        }
        if (preferencesDlg != null) {
            SwingUtilities.updateComponentTreeUI(preferencesDlg);
            preferencesDlg.pack();
        }
    }

    /**
     *  Preferences (Options) settings
     */
    public void preferences() {
        if (preferencesDlg == null) {
            preferencesDlg = new PreferencesDialog(VietPadWithTools.this, true);
            preferencesDlg.setDiacriticsPosition(diacriticsPosClassicOn);
            preferencesDlg.setRepeatKeyConsumed(repeatKeyConsumed);
            preferencesDlg.setDefaultEOL(defaultEOLOn);
            preferencesDlg.setLocaleVN(localeVietOn);
            boolean foreignLangDicInstalled = VietUtilities.getInstalledForeignDicFiles(supportDir).length > 0;
            preferencesDlg.setSpellCheckForeignLang(spellCheckForeignLangEnabled, foreignLangDicInstalled);
            preferencesDlg.setLocation(prefs.getInt("optionsX", preferencesDlg.getX()), prefs.getInt("optionsY", preferencesDlg.getY()));
        }
        preferencesDlg.setVisible(true);
    }

    /**
     *  Remembers settings and dialog locations, then quits
     */
    @Override
    protected void quit() {
        if (convertDlg != null) {
            prefs.putInt("convertIndex", convertDlg.getSelectedIndex());
            prefs.putInt("convertX", convertDlg.getX());
            prefs.putInt("convertY", convertDlg.getY());
        }
        if (sortDlg != null) {
            prefs.putBoolean("sortReverse", sortDlg.isReverse());
            prefs.putInt("sortX", sortDlg.getX());
            prefs.putInt("sortY", sortDlg.getY());
        }
        if (preferencesDlg != null) {
            prefs.putBoolean("diacriticsPosClassic", diacriticsPosClassicOn);
            prefs.putBoolean("repeatKeyConsumed", repeatKeyConsumed);
            prefs.putBoolean("localeVN", localeVietOn);
            prefs.putBoolean("spellCheckForeignLangEnabled", spellCheckForeignLangEnabled);
            prefs.putInt("optionsX", preferencesDlg.getX());
            prefs.putInt("optionsY", preferencesDlg.getY());
        }
        super.quit();
    }

    /**
     *  Starts VietPad when called from a Mac OS X application bundle
     *
     *@param  args  The command line arguments
     */
    public static void main(String[] args) {
        new VietPadWithTools();
    }
}
