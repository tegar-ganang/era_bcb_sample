package com.raelity.jvi;

import com.raelity.jvi.ColonCommands.ColonEvent;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import com.raelity.text.TextUtil.MySegment;
import com.raelity.jvi.Option.ColorOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.raelity.jvi.Constants.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Option handling from external sources.
 * <br>
 * Should there be a vi command to set the options to persistent storage,
 * this is useful if want to save after several set commands.
 * <br>
 *
 */
public final class Options {

    private static Logger LOG = Logger.getLogger(Options.class.getName());

    private Options() {
    }

    private static Options options;

    private static PropertyChangeSupport pcs = new PropertyChangeSupport(getOptions());

    public static interface EditOptionsControl {

        void clear();

        void cancel();
    }

    static Options getOptions() {
        if (options == null) {
            options = new Options();
        }
        return options;
    }

    public enum Category {

        PLATFORM, GENERAL, MODIFY, SEARCH, CURSOR_WRAP, PROCESS, DEBUG
    }

    public static final String commandEntryFrame = "viCommandEntryFrameOption";

    public static final String redoTrack = "viRedoTrack";

    public static final String pcmarkTrack = "viPCMarkTrack";

    public static final String autoPopupFN = "viAutoPopupFN";

    public static final String coordSkip = "viCoordSkip";

    public static final String platformPreferences = "viPlatformPreferences";

    public static final String platformTab = "viPlatformTab";

    public static final String backspaceWrapPrevious = "viBackspaceWrapPrevious";

    public static final String hWrapPrevious = "viHWrapPrevious";

    public static final String leftWrapPrevious = "viLeftWrapPrevious";

    public static final String spaceWrapNext = "viSpaceWrapNext";

    public static final String lWrapNext = "viLWrapNext";

    public static final String rightWrapNext = "viRightWrapNext";

    public static final String tildeWrapNext = "viTildeWrapNext";

    public static final String insertLeftWrapPrevious = "viInsertLeftWrapPrevious";

    public static final String insertRightWrapNext = "viInsertRightWrapNext";

    public static final String unnamedClipboard = "viUnnamedClipboard";

    public static final String joinSpaces = "viJoinSpaces";

    public static final String shiftRound = "viShiftRound";

    public static final String notStartOfLine = "viNotStartOfLine";

    public static final String changeWordBlanks = "viChangeWordBlanks";

    public static final String tildeOperator = "viTildeOperator";

    public static final String searchFromEnd = "viSearchFromEnd";

    public static final String endOfSentence = "viEndOfSentence";

    public static final String wrapScan = "viWrapScan";

    public static final String metaEquals = "viMetaEquals";

    public static final String metaEscape = "viMetaEscape";

    public static final String incrSearch = "viIncrSearch";

    public static final String highlightSearch = "viHighlightSearch";

    public static final String ignoreCase = "viIgnoreCase";

    public static final String platformBraceMatch = "viPlatformBraceMatch";

    public static final String expandTabs = "viExpandTabs";

    public static final String report = "viReport";

    public static final String backspace = "viBackspace";

    public static final String scrollOff = "viScrollOff";

    public static final String shiftWidth = "viShiftWidth";

    public static final String tabStop = "viTabStop";

    public static final String softTabStop = "viSoftTabStop";

    public static final String textWidth = "viTextWidth";

    public static final String showMode = "viShowMode";

    public static final String showCommand = "viShowCommand";

    public static final String nrFormats = "viNrFormats";

    public static final String matchPairs = "viMatchPairs";

    public static final String quoteEscape = "viQuoteEscape";

    public static final String modeline = "viModeline";

    public static final String modelines = "viModelines";

    public static final String selection = "viSelection";

    public static final String selectMode = "viSelectMode";

    public static final String selectColor = "viSelectColor";

    public static final String selectFgColor = "viSelectFgColor";

    public static final String searchColor = "viSearchColor";

    public static final String searchFgColor = "viSearchFgColor";

    public static final String equalProgram = "viEqualProgram";

    public static final String formatProgram = "viFormatProgram";

    public static final String shell = "viShell";

    public static final String shellCmdFlag = "viShellCmdFlag";

    public static final String shellXQuote = "viShellXQuote";

    public static final String shellSlash = "viShellSlash";

    public static final String persistedBufMarks = "viPersistedBufMarks";

    public static final String readOnlyHack = "viReadOnlyHack";

    public static final String classicUndoOption = "viClassicUndo";

    public static final String hideVersionOption = "viHideVersion";

    public static final String dbgRedo = "viDbgRedo";

    public static final String dbgKeyStrokes = "viDbgKeyStrokes";

    public static final String dbgCache = "viDbgCache";

    public static final String dbgEditorActivation = "viDbgEditorActivation";

    public static final String dbgBang = "viDbgBang";

    public static final String dbgBangData = "viDbgBangData";

    public static final String dbgMouse = "viDbgMouse";

    public static final String dbgCompletion = "viDbgCompletion";

    public static final String dbgCoordSkip = "viDbgCoordSkip";

    public static final String twMagic = "#TEXT-WIDTH#";

    private static Map<String, Option> optionsMap = new HashMap<String, Option>();

    static List<String> platformList = new ArrayList<String>();

    static List<String> generalList = new ArrayList<String>();

    static List<String> modifyList = new ArrayList<String>();

    static List<String> searchList = new ArrayList<String>();

    static List<String> cursorWrapList = new ArrayList<String>();

    static List<String> processList = new ArrayList<String>();

    static List<String> debugList = new ArrayList<String>();

    static Preferences prefs;

    private static boolean didInit = false;

    public static void init() {
        if (didInit) {
            return;
        }
        didInit = true;
        prefs = ViManager.getViFactory().getPreferences();
        prefs.addPreferenceChangeListener(new PreferenceChangeListener() {

            public void preferenceChange(PreferenceChangeEvent evt) {
                Option opt = optionsMap.get(evt.getKey());
                if (opt != null) {
                    if (evt.getNewValue() != null) {
                        opt.preferenceChange(evt.getNewValue());
                    }
                }
            }
        });
        platformList.add("jViVersion");
        G.redoTrack = createBooleanOption(redoTrack, true);
        setupOptionDesc(platformList, redoTrack, "\".\" magic redo tracking", "Track magic document changes during input" + " mode for the \".\" commnad. These" + " changes are often the result of IDE code completion");
        G.pcmarkTrack = createBooleanOption(pcmarkTrack, true);
        setupOptionDesc(platformList, pcmarkTrack, "\"``\" magic pcmark tracking", "Track magic cursor " + " movments for the \"``\" command. These movement are" + " often the result of IDE actions invoked external" + " to jVi.");
        createColorOption(searchColor, new Color(0xffb442), false);
        setupOptionDesc(platformList, searchColor, "'hl-search' color", "The color used for search highlight.");
        createColorOption(searchFgColor, new Color(0x000000), true);
        setupOptionDesc(platformList, searchFgColor, "'hl-search' foreground color", "The color used for search highlight foreground.");
        setExpertHidden(searchFgColor, false, false);
        createColorOption(selectColor, new Color(0xffe588), false);
        setupOptionDesc(platformList, selectColor, "'hl-visual' color", "The color used for a visual mode selection.");
        createColorOption(selectFgColor, null, true);
        setupOptionDesc(platformList, selectFgColor, "'hl-visual' foreground color", "The color used for a visual mode selection foreground.");
        setExpertHidden(selectFgColor, false, false);
        G.isClassicUndo = createBooleanOption(classicUndoOption, true);
        setupOptionDesc(platformList, classicUndoOption, "classic undo", "When false, undo is done according to the" + " underlying platform; usually tiny chunks.");
        setExpertHidden(classicUndoOption, true, false);
        G.isHideVersion = createBooleanOption(hideVersionOption, false);
        setupOptionDesc(platformList, hideVersionOption, "hide version", "When true, display of initial version information" + " does not bring up output window.");
        setExpertHidden(hideVersionOption, true, false);
        G.useFrame = createBooleanOption(commandEntryFrame, true);
        setupOptionDesc(platformList, commandEntryFrame, "use modal frame", "Use modal frame for command/search entry." + " Change takes affect after restart.");
        setExpertHidden(commandEntryFrame, true, false);
        createBooleanOption(autoPopupFN, false);
        setupOptionDesc(platformList, autoPopupFN, "\":e#\" Auto Popup", "When doing \":\" command line entry, if \"e#\" is" + " entered then automatically popup a file" + " name completion window. NB6 only; post 07/07/22");
        G.isCoordSkip = createBooleanOption(coordSkip, true);
        setupOptionDesc(platformList, coordSkip, "Code Folding Compatible", "When false revert some navigation algorithms, e.g. ^F," + " to pre code folding behavior. A just in case option;" + " if needed, please file a bug report.");
        setExpertHidden(coordSkip, true, false);
        createBooleanOption(platformPreferences, false);
        setupOptionDesc(platformList, platformPreferences, "Store init (\"vimrc\") with Platform", "Store user preferences/options in platform location." + " Change occurs after next application startup." + " For example, on NetBeans store in userdir." + " NOTE: except for the first switch to platform," + " changes made in one area" + " are not propogated to the other.");
        setExpertHidden(platformPreferences, true, true);
        G.usePlatformInsertTab = createBooleanOption(platformTab, false);
        setupOptionDesc(platformList, platformTab, "Use the platform's TAB handling", "When false, jVi processes the TAB character according" + " to the expandtab and softtabstop options. Otherwise" + " the TAB is passed to the platform, e.g. IDE, for handling." + " The only reason to set this true is if a bug is discovered" + " in the jVi tab handling.");
        setExpertHidden(platformTab, true, false);
        G.p_so = createIntegerOption(scrollOff, 0);
        setupOptionDesc(generalList, scrollOff, "'scrolloff' 'so'", "visible context around cursor (scrolloff)" + "	Minimal number of screen lines to keep above and below the" + " cursor. This will make some context visible around where you" + " are working.  If you set it to a very large value (999) the" + " cursor line will always be in the middle of the window" + " (except at the start or end of the file)");
        G.p_smd = createBooleanOption(showMode, true);
        setupOptionDesc(generalList, showMode, "'showmode' 'smd'", "If in Insert or Replace mode display that information.");
        G.p_sc = createBooleanOption(showCommand, true);
        setupOptionDesc(generalList, showCommand, "'showcmd' 'sc'", "Show (partial) command in status line.");
        G.p_report = createIntegerOption(report, 2);
        setupOptionDesc(generalList, report, "'report'", "Threshold for reporting number of lines changed.  When the" + " number of changed lines is more than 'report' a message will" + " be given for most \":\" commands.  If you want it always, set" + " 'report' to 0.  For the \":substitute\" command the number of" + " substitutions is used instead of the number of lines.");
        G.p_ml = createBooleanOption(modeline, true);
        setupOptionDesc(generalList, modeline, "'modeline' 'ml'", "Enable/disable modelines option");
        G.p_mls = createIntegerOption(modelines, 5);
        setupOptionDesc(generalList, modelines, "'modelines' 'mls'", " If 'modeline' is on 'modelines' gives the number of lines" + " that is checked for set commands.  If 'modeline' is off" + " or 'modelines' is zero no lines are checked.");
        G.p_cb = createBooleanOption(unnamedClipboard, false);
        setupOptionDesc(generalList, unnamedClipboard, "'clipboard' 'cb' (unnamed)", "use clipboard for unamed yank, delete and put");
        G.p_notsol = createBooleanOption(notStartOfLine, false);
        setupOptionDesc(generalList, notStartOfLine, "(not)'startofline' (not)'sol'", "After motion try to keep column position." + " NOTE: state is opposite of vim.");
        G.viminfoMaxBuf = createIntegerOption(persistedBufMarks, 25, new IntegerOption.Validator() {

            @Override
            public void validate(int val) throws PropertyVetoException {
                if (val < 0 || val > 100) {
                    throw new PropertyVetoException("Only 0 - 100 allowed." + " Not '" + val + "'.", new PropertyChangeEvent(opt, opt.getName(), opt.getInteger(), val));
                }
            }
        });
        setupOptionDesc(generalList, persistedBufMarks, "max persisted buf-marks", "Maximum number of previously edited files for which the marks" + " are remembered. Set to 0 and no marks are persisted.");
        G.p_sel = createEnumStringOption(selection, "inclusive", new String[] { "old", "inclusive", "exclusive" });
        setupOptionDesc(generalList, selection, "'selection' 'sel'", "This option defines the behavior of the selection." + " It is only used in Visual and Select mode." + "Possible values: 'old', 'inclusive', 'exclusive'");
        setExpertHidden(selection, false, false);
        G.p_slm = createEnumStringOption(selectMode, "", new String[] { "mouse", "key", "cmd" });
        setupOptionDesc(generalList, selectMode, "'selectmode' 'slm'", "This is a comma separated list of words, which specifies when to" + " start Select mode instead of Visual mode, when a selection is" + " started. Possible values: 'mouse', key' or 'cmd'");
        setExpertHidden(selectMode, true, true);
        G.p_to = createBooleanOption(tildeOperator, false);
        setupOptionDesc(modifyList, tildeOperator, "'tildeop' 'top'", "tilde \"~\" acts like an operator, e.g. \"~w\" works");
        G.p_cpo_w = createBooleanOption(changeWordBlanks, true);
        setupOptionDesc(modifyList, changeWordBlanks, "'cpoptions' 'cpo' \"w\"", "\"cw\" affects sequential white space");
        G.p_js = createBooleanOption(joinSpaces, true);
        setupOptionDesc(modifyList, joinSpaces, "'joinspaces' 'js'", "\"J\" inserts two spaces after a \".\", \"?\" or \"!\"");
        G.p_sr = createBooleanOption(shiftRound, false);
        setupOptionDesc(modifyList, shiftRound, "'shiftround' 'sr'", "\"<\" and \">\" round indent to multiple of shiftwidth");
        G.p_bs = createEnumIntegerOption(backspace, 0, new Integer[] { 0, 1, 2 });
        setupOptionDesc(modifyList, backspace, "'backspace' 'bs'", "Influences the working of <BS>, <Del> during insert." + "\n  0 - no special handling." + "\n  1 - allow backspace over <EOL>." + "\n  2 - allow backspace over start of insert.");
        createBooleanOption(expandTabs, false);
        setupOptionDesc(modifyList, expandTabs, "'expandtab' 'et'", "In Insert mode: Use the appropriate number of spaces to" + " insert a <Tab>. Spaces are used in indents with the '>' and" + " '<' commands.");
        createIntegerOption(shiftWidth, 8);
        setupOptionDesc(modifyList, shiftWidth, "'shiftwidth' 'sw'", "Number of spaces to use for each step of indent. Used for '>>'," + " '<<', etc.");
        createIntegerOption(tabStop, 8);
        setupOptionDesc(modifyList, tabStop, "'tabstop' 'ts'", "Number of spaces that a <Tab> in the file counts for.");
        createIntegerOption(softTabStop, 0);
        setupOptionDesc(modifyList, softTabStop, "'softtabstop' 'sts'", "Number of spaces that a <Tab> in the file counts for" + " while performing editing operations," + " like inserting a <Tab> or using <BS>." + " It \"feels\" like <Tab>s are being inserted, while in fact" + " a mix of spaces and <Tab>s is used (<Tabs>s only if" + " 'expandtabs' is false).  When 'sts' is zero, this feature" + " is off. If 'softtabstop' is non-zero, a <BS> will try to" + " delete as much white space to move to the previous" + " 'softtabstop' position.");
        createIntegerOption(textWidth, 79);
        setupOptionDesc(modifyList, textWidth, "'textwidth' 'tw'", "This option currently only used in conjunction with the" + " 'gq' and 'Q' format command. This value is substituted" + " for " + twMagic + " in formatprg option string.");
        createStringOption(nrFormats, "octal,hex");
        setupOptionDesc(modifyList, nrFormats, "'nrformats' 'nf'", "Defines bases considered for numbers with the" + " 'CTRL-A' and 'CTRL-X' commands for adding to and subtracting" + " from a number respectively. Value is comma separated list;" + " 'octal,hex,alpha' is all possible values.");
        G.p_is = createBooleanOption(incrSearch, true);
        setupOptionDesc(searchList, incrSearch, "'incsearch' 'is'", "While typing a search command, show where the pattern, as it was" + " typed so far, matches. If invalid pattern, no match" + " or abort then the screen returns to its original location." + " You still need to finish the search with" + " <ENTER> or abort it with <ESC>.");
        G.p_hls = createBooleanOption(highlightSearch, true);
        setupOptionDesc(searchList, highlightSearch, "'hlsearch' 'hls'", "When there is a previous search pattern, highlight" + " all its matches");
        G.p_ic = createBooleanOption(ignoreCase, false);
        setupOptionDesc(searchList, ignoreCase, "'ignorecase' 'ic'", "Ignore case in search patterns.");
        G.p_ws = createBooleanOption(wrapScan, true);
        setupOptionDesc(searchList, wrapScan, "'wrapscan' 'ws'", "Searches wrap around the end of the file.");
        G.p_cpo_search = createBooleanOption(searchFromEnd, true);
        setupOptionDesc(searchList, searchFromEnd, "'cpoptions' 'cpo' \"c\"", "search continues at end of match");
        G.p_cpo_j = createBooleanOption(endOfSentence, false);
        setupOptionDesc(searchList, endOfSentence, "'cpoptions' 'cpo' \"j\"", "A sentence has to be followed by two spaces after" + " the '.', '!' or '?'.  A <Tab> is not recognized as" + " white space.");
        G.p_pbm = createBooleanOption(platformBraceMatch, true);
        setupOptionDesc(searchList, platformBraceMatch, "Platform Brace Matching", "Use the platform/IDE for brace matching" + " and match highlighting. This may enable additional" + " match characters, words and features.");
        G.p_meta_equals = createBooleanOption(metaEquals, true);
        setupOptionDesc(searchList, metaEquals, "RE Meta Equals", "In a regular expression allow" + " '=', in addition to '?', to indicate an optional atom.");
        setExpertHidden(metaEquals, true, false);
        G.p_meta_escape = createStringOption(metaEscape, G.metaEscapeDefault, new StringOption.Validator() {

            @Override
            public void validate(String val) throws PropertyVetoException {
                for (int i = 0; i < val.length(); i++) {
                    if (G.metaEscapeAll.indexOf(val.charAt(i)) < 0) {
                        throw new PropertyVetoException("Only characters from '" + G.metaEscapeAll + "' are RE metacharacters." + " Not '" + val.substring(i, i + 1) + "'.", new PropertyChangeEvent(opt, opt.getName(), opt.getString(), val));
                    }
                }
            }
        });
        setupOptionDesc(searchList, metaEscape, "RE Meta Escape", "Regular expression metacharacters requiring escape:" + " any of: '(', ')', '|', '+', '?', '{'." + " By default vim requires escape, '\\', for these characters.");
        setExpertHidden(metaEscape, true, false);
        G.p_ww_bs = createBooleanOption(backspaceWrapPrevious, true);
        setupOptionDesc(cursorWrapList, backspaceWrapPrevious, "'whichwrap' 'ww'  b - <BS>", "<backspace> wraps to previous line");
        G.p_ww_h = createBooleanOption(hWrapPrevious, false);
        setupOptionDesc(cursorWrapList, hWrapPrevious, "'whichwrap' 'ww'  h - \"h\"", "\"h\" wraps to previous line (not recommended, see vim doc)");
        G.p_ww_larrow = createBooleanOption(leftWrapPrevious, false);
        setupOptionDesc(cursorWrapList, leftWrapPrevious, "'whichwrap' 'ww'  < - <Left>", "<left> wraps to previous line");
        G.p_ww_sp = createBooleanOption(spaceWrapNext, true);
        setupOptionDesc(cursorWrapList, spaceWrapNext, "'whichwrap' 'ww'  s - <Space>", "<space> wraps to next line");
        G.p_ww_l = createBooleanOption(lWrapNext, false);
        setupOptionDesc(cursorWrapList, lWrapNext, "'whichwrap' 'ww'  l - \"l\"", "\"l\" wraps to next line (not recommended, see vim doc)");
        G.p_ww_rarrow = createBooleanOption(rightWrapNext, false);
        setupOptionDesc(cursorWrapList, rightWrapNext, "'whichwrap' 'ww'  > - <Right>", "<right> wraps to next line");
        G.p_ww_tilde = createBooleanOption(tildeWrapNext, false);
        setupOptionDesc(cursorWrapList, tildeWrapNext, "'whichwrap' 'ww'  ~ - \"~\"", "\"~\" wraps to next line");
        G.p_ww_i_left = createBooleanOption(insertLeftWrapPrevious, false);
        setupOptionDesc(cursorWrapList, insertLeftWrapPrevious, "'whichwrap' 'ww'  [ - <Left>", "in Insert Mode <Left> wraps to previous line");
        G.p_ww_i_right = createBooleanOption(insertRightWrapNext, false);
        setupOptionDesc(cursorWrapList, insertRightWrapNext, "'whichwrap' 'ww'  ] - <Right>", "in Insert Mode <Right> wraps to next line");
        boolean inWindows = ViManager.getOsVersion().isWindows();
        String defaultShell = System.getenv("SHELL");
        String defaultXQuote = "";
        String defaultFlag = null;
        if (defaultShell == null) {
            if (inWindows) defaultShell = "cmd.exe"; else defaultShell = "sh";
        }
        if (defaultShell.contains("sh")) {
            defaultFlag = "-c";
            if (inWindows) defaultXQuote = "\"";
        } else defaultFlag = "/c";
        G.p_sh = createStringOption(shell, defaultShell);
        setupOptionDesc(processList, shell, "'shell' 'sh'", "Name of shell to use for ! and :! commands.  (default $SHELL " + "or \"sh\", MS-DOS and Win32: \"command.com\" or \"cmd.exe\").  " + "When changing also check 'shellcmndflag'.");
        G.p_shcf = createStringOption(shellCmdFlag, defaultFlag);
        setupOptionDesc(processList, shellCmdFlag, "'shellcmdflag' 'shcf'", "Flag passed to shell to execute \"!\" and \":!\" commands; " + "e.g., \"bash.exe -c ls\" or \"command.com /c dir\" (default: " + "\"-c\", MS-DOS and Win32, when 'shell' does not contain \"sh\" " + "somewhere: \"/c\").");
        G.p_sxq = createStringOption(shellXQuote, defaultXQuote);
        setupOptionDesc(processList, shellXQuote, "'shellxquote' 'sxq'", "Quoting character(s), put around the commands passed to the " + "shell, for the \"!\" and \":!\" commands (default: \"\"; for " + "Win32, when 'shell' contains \"sh\" somewhere: \"\\\"\").");
        G.p_ssl = createBooleanOption(shellSlash, false);
        setupOptionDesc(processList, shellSlash, "'shellslash' 'ssl'", "When set, a forward slash is used when expanding file names." + "This is useful when a Unix-like shell is used instead of " + "command.com or cmd.exe.");
        G.p_ep = createStringOption(equalProgram, "");
        setupOptionDesc(processList, equalProgram, "'equalprg' 'ep'", "External program to use for \"=\" command (default \"\").  " + "When this option is empty the internal formatting functions " + "are used.");
        G.p_fp = createStringOption(formatProgram, "");
        setupOptionDesc(processList, formatProgram, "'formatprg' 'fp'", "External program to use for \"qq\" or \"Q\" command (default \"\")." + " When this option is empty the internal formatting functions" + " are used." + "\n\n When specified, the program must take input on stdin and" + " send output to stdout. In Unix, \"fmt\" is such a program." + twMagic + " in the string is" + " substituted by the value of textwidth option. " + "\n\nTypically set to \"fmt -w #TEXT-WIDTH#\" to use external program.");
        G.readOnlyHack = createBooleanOption(readOnlyHack, true);
        setupOptionDesc(debugList, readOnlyHack, "enable read only hack", "A Java implementation issue, restricts the characters that jVi" + " recieves for a read only file. Enabling this, changes the file" + " editor mode to read/write so that the file can be viewed" + " using the Normal Mode vi commands.");
        setExpertHidden(readOnlyHack, true, true);
        G.dbgEditorActivation = createBooleanOption(dbgEditorActivation, false);
        setupOptionDesc(debugList, dbgEditorActivation, "debug activation", "Output info about editor switching between files/windows");
        createBooleanOption(dbgKeyStrokes, false);
        setupOptionDesc(debugList, dbgKeyStrokes, "debug KeyStrokes", "Output info for each keystroke");
        G.dbgRedo = createBooleanOption(dbgRedo, false);
        setupOptionDesc(debugList, dbgRedo, "debug redo buffer", "Output info on magic/tracking changes to redo buffer");
        createBooleanOption(dbgCache, false);
        setupOptionDesc(debugList, dbgCache, "debug cache", "Output info on text/doc cache");
        createBooleanOption(dbgBang, false);
        setupOptionDesc(debugList, dbgBang, "debug \"!\" cmds", "Output info about external processes");
        createBooleanOption(dbgBangData, false);
        setupOptionDesc(debugList, dbgBangData, "debug \"!\" cmds data", "Output data tranfers external processes");
        createBooleanOption(dbgCompletion, false);
        setupOptionDesc(debugList, dbgCompletion, "debug Completion", "Output info on completion, eg FileName.");
        G.dbgMouse = createBooleanOption(dbgMouse, false);
        setupOptionDesc(debugList, dbgMouse, "debug mouse events", "Output info about mouse events");
        G.dbgCoordSkip = createBooleanOption(dbgCoordSkip, false);
        setupOptionDesc(debugList, dbgCoordSkip, "debug coordinate skip", "");
    }

    static Preferences getPrefs() {
        return prefs;
    }

    public static StringOption createStringOption(String name, String defaultValue) {
        return createStringOption(name, defaultValue, null);
    }

    public static StringOption createStringOption(String name, String defaultValue, StringOption.Validator valid) {
        if (optionsMap.get(name) != null) throw new IllegalArgumentException("Option " + name + "already exists");
        StringOption opt = new StringOption(name, defaultValue, valid);
        optionsMap.put(name, opt);
        return opt;
    }

    public static EnumStringOption createEnumStringOption(String name, String defaultValue, String[] availableValues) {
        return createEnumStringOption(name, defaultValue, null, availableValues);
    }

    public static EnumStringOption createEnumStringOption(String name, String defaultValue, StringOption.Validator valid, String[] availableValues) {
        if (optionsMap.get(name) != null) throw new IllegalArgumentException("Option " + name + "already exists");
        EnumStringOption opt = new EnumStringOption(name, defaultValue, valid, availableValues);
        optionsMap.put(name, opt);
        return opt;
    }

    public static BooleanOption createBooleanOption(String name, boolean defaultValue) {
        if (optionsMap.get(name) != null) throw new IllegalArgumentException("Option " + name + "already exists");
        BooleanOption opt = new BooleanOption(name, defaultValue);
        optionsMap.put(name, opt);
        return opt;
    }

    public static IntegerOption createIntegerOption(String name, int defaultValue) {
        return createIntegerOption(name, defaultValue, null);
    }

    public static IntegerOption createIntegerOption(String name, int defaultValue, IntegerOption.Validator valid) {
        if (optionsMap.get(name) != null) throw new IllegalArgumentException("Option " + name + "already exists");
        IntegerOption opt = new IntegerOption(name, defaultValue, valid);
        optionsMap.put(name, opt);
        return opt;
    }

    public static EnumIntegerOption createEnumIntegerOption(String name, int defaultValue, Integer[] availableValues) {
        return createEnumIntegerOption(name, defaultValue, null, availableValues);
    }

    public static EnumIntegerOption createEnumIntegerOption(String name, int defaultValue, IntegerOption.Validator valid, Integer[] availableValues) {
        if (optionsMap.get(name) != null) throw new IllegalArgumentException("Option " + name + "already exists");
        EnumIntegerOption opt = new EnumIntegerOption(name, defaultValue, valid, availableValues);
        optionsMap.put(name, opt);
        return opt;
    }

    public static ColorOption createColorOption(String name, Color defaultValue, boolean permitNull) {
        return createColorOption(name, defaultValue, permitNull, null);
    }

    public static ColorOption createColorOption(String name, Color defaultValue, boolean permitNull, ColorOption.Validator valid) {
        if (optionsMap.get(name) != null) throw new IllegalArgumentException("Option " + name + "already exists");
        ColorOption opt = new ColorOption(name, defaultValue, permitNull, valid);
        optionsMap.put(name, opt);
        return opt;
    }

    public static void setOptionValue(String name, String value) {
        Option option = getOption(name);
        option.setValue(value);
    }

    public static Option getOption(String name) {
        return optionsMap.get(name);
    }

    /**
   * Only used by JBuilder, should convert....
   * @return the String key names of the options.
   */
    public static List<String> getOptionNamesList() {
        List<String> l = new ArrayList<String>();
        l.addAll(generalList);
        l.addAll(modifyList);
        l.addAll(cursorWrapList);
        l.addAll(debugList);
        return Collections.unmodifiableList(l);
    }

    public static List<String> getOptionList(Category category) {
        List<String> catList = null;
        switch(category) {
            case PLATFORM:
                catList = platformList;
                break;
            case GENERAL:
                catList = generalList;
                break;
            case MODIFY:
                catList = modifyList;
                break;
            case SEARCH:
                catList = searchList;
                break;
            case CURSOR_WRAP:
                catList = cursorWrapList;
                break;
            case PROCESS:
                catList = processList;
                break;
            case DEBUG:
                catList = debugList;
                break;
        }
        return Collections.unmodifiableList(catList);
    }

    public static void setupOptionDesc(Category category, String name, String displayName, String desc) {
        List<String> catList = getOptionList(category);
        setupOptionDesc(catList, name, displayName, desc);
    }

    public static void setupOptionDesc(String name, String displayName, String desc) {
        setupOptionDesc((List<String>) null, name, displayName, desc);
    }

    private static void setupOptionDesc(List<String> optionsGroup, String name, String displayName, String desc) {
        Option opt = optionsMap.get(name);
        if (opt != null) {
            if (optionsGroup != null) {
                optionsGroup.add(name);
            }
            if (opt.desc != null) {
                throw new Error("option: " + name + " already has a description.");
            }
            opt.desc = desc;
            opt.displayName = displayName;
        } else {
            throw new Error("Unknown option: " + name);
        }
    }

    public static void setExpertHidden(String optionName, boolean fExpert, boolean fHidden) {
        Option opt = optionsMap.get(optionName);
        if (opt != null) {
            opt.fExpert = fExpert;
            opt.fHidden = fHidden;
        }
    }

    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public static void addPropertyChangeListener(String p, PropertyChangeListener l) {
        pcs.addPropertyChangeListener(p, l);
    }

    public static void removePropertyChangeListener(String p, PropertyChangeListener l) {
        pcs.removePropertyChangeListener(p, l);
    }

    /** This should only be used from Option and its subclasses */
    static void firePropertyChange(String name, Object oldValue, Object newValue) {
        pcs.firePropertyChange(name, oldValue, newValue);
    }

    /** Implement ":se[t]".
   *
   * Options are either global or indirect, see the P_ XXX below An option
   * must be one or the other. Global options are static, an indirect option
   * is an instance variable in either G.curwin or G.curbuf. When a P_IND
   * variable is set, introspection is used to do the set.
   * <p>
   * In some cases, due to platform limitation, the same variable must be
   * set in all the instances, syncAllInstances(var) does that.
   */
    public static class SetCommand extends ColonCommands.ColonAction {

        private static class SetCommandException extends Exception {

            SetCommandException(String msg) {
                super(msg);
            }
        }

        private static int P_IND = 0x01;

        private static int P_WIN = 0x02;

        private static int P_OPT = 0x04;

        private static class VimOption {

            String fullname;

            String shortname;

            int flags;

            String varName;

            String optName;

            VimOption(String fullname, String shortname, int flags, String varName, String optName) {
                this.fullname = fullname;
                this.shortname = shortname;
                this.flags = flags;
                this.varName = varName;
                this.optName = optName;
            }
        }

        private static VimOption vopts[] = new VimOption[] { new VimOption("expandtab", "et", P_IND, "b_p_et", null), new VimOption("ignorecase", "ic", P_OPT, null, ignoreCase), new VimOption("incsearch", "is", P_OPT, null, incrSearch), new VimOption("hlsearch", "hls", P_OPT, null, highlightSearch), new VimOption("number", "nu", P_IND | P_WIN, "w_p_nu", null), new VimOption("shiftwidth", "sw", P_IND, "b_p_sw", shiftWidth), new VimOption("tabstop", "ts", P_IND, "b_p_ts", tabStop), new VimOption("softtabstop", "sts", P_IND, "b_p_sts", softTabStop), new VimOption("textwidth", "tw", P_IND, "b_p_tw", textWidth) };

        public void actionPerformed(ActionEvent e) {
            ColonEvent evt = (ColonEvent) e;
            parseSetOptions(evt.getArgs());
        }

        public void parseSetOptions(List<String> eventArgs) {
            if (eventArgs == null || eventArgs.size() == 1 && "all".equals(eventArgs.get(0))) {
                displayAllOptions();
                return;
            }
            LinkedList<String> args = new LinkedList<String>();
            int j = 0;
            for (int i = 0; i < eventArgs.size(); i++) {
                String arg = eventArgs.get(i);
                if (arg.startsWith("=") && args.size() > 0) {
                    arg = args.removeLast() + arg;
                }
                args.addLast(arg);
            }
            for (String arg : args) {
                try {
                    parseSetOption(arg);
                } catch (SetCommandException ex) {
                    return;
                } catch (IllegalAccessException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    return;
                } catch (IllegalArgumentException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    return;
                }
            }
        }

        private static class VimOptionDescriptor {

            Class type;

            Object value;

            Field f;

            ViOptionBag bag;

            Option opt;

            boolean fInv;

            boolean fNo;

            boolean fShow;

            boolean fValue;

            String split[];
        }

        public static void parseSetOption(String arg) throws IllegalAccessException, SetCommandException {
            VimOptionDescriptor voptDesc = new VimOptionDescriptor();
            voptDesc.split = arg.split("[:=]");
            String voptName = voptDesc.split[0];
            if (voptDesc.split.length > 1) {
                voptDesc.fValue = true;
            }
            if (voptName.startsWith("no")) {
                voptDesc.fNo = true;
                voptName = voptName.substring(2);
            } else if (voptName.startsWith("inv")) {
                voptDesc.fInv = true;
                voptName = voptName.substring(3);
            } else if (voptName.endsWith("!")) {
                voptDesc.fInv = true;
                voptName = voptName.substring(0, voptName.length() - 1);
            } else if (voptName.endsWith("?")) {
                voptDesc.fShow = true;
                voptName = voptName.substring(0, voptName.length() - 1);
            }
            VimOption vopt = null;
            for (VimOption v : vopts) {
                if (voptName.equals(v.fullname) || voptName.equals(v.shortname)) {
                    vopt = v;
                    break;
                }
            }
            if (vopt == null) {
                String msg = "Unknown option: " + voptName;
                Msg.emsg(msg);
                throw new SetCommandException(msg);
            }
            if (!determineOptionState(vopt, voptDesc)) {
                String msg = "Internal error: " + arg;
                Msg.emsg(msg);
                throw new SetCommandException(msg);
            }
            Object newValue = newOptionValue(arg, vopt, voptDesc);
            if (voptDesc.fShow) Msg.smsg(formatDisplayValue(vopt, voptDesc.value)); else {
                if (voptDesc.opt != null) {
                    try {
                        voptDesc.opt.validate(newValue);
                    } catch (PropertyVetoException ex) {
                        Msg.emsg(ex.getMessage());
                        throw new SetCommandException(ex.getMessage());
                    }
                }
                if ((vopt.flags & P_IND) != 0) {
                    voptDesc.f.set(voptDesc.bag, newValue);
                    voptDesc.bag.viOptionSet(G.curwin, vopt.varName);
                } else {
                    voptDesc.opt.setValue(newValue.toString());
                }
            }
        }

        /**
     * Set voptDesc with information about the argument vopt.
     */
        private static boolean determineOptionState(VimOption vopt, VimOptionDescriptor voptDesc) {
            if (vopt.optName != null) voptDesc.opt = getOption(vopt.optName);
            if ((vopt.flags & P_IND) != 0) {
                voptDesc.bag = (vopt.flags & P_WIN) != 0 ? G.curwin : G.curbuf;
                try {
                    voptDesc.f = voptDesc.bag.getClass().getField(vopt.varName);
                } catch (SecurityException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } catch (NoSuchFieldException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                if (voptDesc.f == null) {
                    return false;
                }
                voptDesc.type = voptDesc.f.getType();
                try {
                    voptDesc.value = voptDesc.f.get(voptDesc.bag);
                } catch (IllegalArgumentException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else if ((vopt.flags & P_OPT) != 0) {
                if (voptDesc.opt instanceof BooleanOption) {
                    voptDesc.type = boolean.class;
                    voptDesc.value = voptDesc.opt.getBoolean();
                } else if (voptDesc.opt instanceof IntegerOption) {
                    voptDesc.type = int.class;
                    voptDesc.value = voptDesc.opt.getInteger();
                }
            }
            return true;
        }

        private static Object newOptionValue(String arg, VimOption vopt, VimOptionDescriptor voptDesc) throws NumberFormatException, SetCommandException {
            Object newValue = null;
            if (voptDesc.type == boolean.class) {
                if (voptDesc.fValue) {
                    String msg = "Unknown argument: " + arg;
                    Msg.emsg(msg);
                    throw new SetCommandException(msg);
                }
                if (!voptDesc.fShow) {
                    if (voptDesc.fInv) newValue = !((Boolean) voptDesc.value).booleanValue(); else if (voptDesc.fNo) newValue = false; else newValue = true;
                }
            } else if (voptDesc.type == int.class) {
                if (!voptDesc.fValue) voptDesc.fShow = true;
                if (!voptDesc.fShow) {
                    try {
                        newValue = Integer.parseInt(voptDesc.split[1]);
                    } catch (NumberFormatException ex) {
                        String msg = "Number required after =: " + arg;
                        Msg.emsg(msg);
                        throw new SetCommandException(msg);
                    }
                }
            } else assert false : "Type " + voptDesc.type.getSimpleName() + " not handled";
            return newValue;
        }

        private static String formatDisplayValue(VimOption vopt, Object value) {
            String v = "";
            if (value instanceof Boolean) {
                v = (((Boolean) value).booleanValue() ? "  " : "no") + vopt.fullname;
            } else if (value instanceof Integer) {
                v = vopt.fullname + "=" + value;
            } else assert false : value.getClass().getSimpleName() + " not handled";
            return v;
        }

        private static void displayAllOptions() {
            ViOutputStream osa = ViManager.createOutputStream(null, ViOutputStream.OUTPUT, null);
            for (VimOption vopt : vopts) {
                VimOptionDescriptor voptDesc = new VimOptionDescriptor();
                determineOptionState(vopt, voptDesc);
                osa.println(formatDisplayValue(vopt, voptDesc.value));
            }
            osa.close();
        }

        /** Note the value from the current is used to set any others */
        public static void syncAllInstances(String varName) {
            for (VimOption vopt : vopts) {
                if ((vopt.flags & P_IND) != 0) {
                    if (vopt.varName.equals(varName)) {
                        VimOptionDescriptor voptDesc = new VimOptionDescriptor();
                        determineOptionState(vopt, voptDesc);
                        Set<? extends ViOptionBag> set = (vopt.flags & P_WIN) != 0 ? ViManager.getViFactory().getViTextViewSet() : ViManager.getViFactory().getBufferSet();
                        for (ViOptionBag bag : set) {
                            try {
                                voptDesc.f.set(bag, voptDesc.value);
                            } catch (IllegalArgumentException ex) {
                                LOG.log(Level.SEVERE, null, ex);
                            } catch (IllegalAccessException ex) {
                                LOG.log(Level.SEVERE, null, ex);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private static Pattern mlPat1;

    private static Pattern mlPat2;

    public static void processModelines() {
        if (mlPat1 == null) {
            mlPat1 = Pattern.compile("\\s+(?:vi:|vim:|ex:)\\s*(.*)");
            mlPat2 = Pattern.compile("\\s+(?:vi:|vim:|ex:)\\s*set? ([^:]*):");
        }
        int mls;
        if (!G.p_ml.value || (mls = G.p_mls.value) == 0) return;
        int lnum;
        int lcount = G.curbuf.getLineCount();
        for (lnum = 1; lnum < lcount && lnum <= mls; lnum++) {
            if (checkModeline(lnum)) mls = 0;
        }
        for (lnum = lcount; lnum > 0 && lnum > mls && lnum > lcount - mls; lnum--) {
            if (checkModeline(lnum)) mls = 0;
        }
    }

    /** @return true if parsed a modeline, there may have been errors */
    private static boolean checkModeline(int lnum) {
        MySegment seg = Util.ml_get(lnum);
        if (parseModeline(mlPat2, seg, lnum)) return true;
        if (parseModeline(mlPat1, seg, lnum)) return true;
        return false;
    }

    /** @return true if found and parsed a modeline, there may have been errors */
    private static boolean parseModeline(Pattern p, CharSequence cs, int lnum) {
        Matcher m = p.matcher(cs);
        if (!m.find()) return false;
        boolean parseError = false;
        String mline = m.group(1);
        StringBuilder sb = new StringBuilder();
        try {
            String[] args = mline.split("[:\\s]");
            for (String arg : args) {
                String msg = "";
                try {
                    SetCommand.parseSetOption(arg);
                } catch (SetCommand.SetCommandException ex) {
                    msg = ex.getMessage();
                } catch (IllegalAccessException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                if (sb.length() != 0) sb.append('\n');
                if (!msg.equals("")) {
                    sb.append("Error: ").append(msg);
                    parseError = true;
                } else sb.append("   OK: ").append(arg);
            }
        } finally {
            String fn = G.curbuf.getDisplayFileName();
            ViOutputStream vos = ViManager.createOutputStream(G.curwin, ViOutputStream.OUTPUT, "In " + fn + ":" + lnum + " process modeline: " + mline, parseError ? ViOutputStream.PRI_HIGH : ViOutputStream.PRI_LOW);
            vos.println(sb.toString());
            if (vos != null) vos.close();
        }
        return true;
    }

    static boolean can_bs(char what) {
        switch(G.p_bs.value) {
            case 2:
                return true;
            case 1:
                return what != BS_START;
            case 0:
                return false;
        }
        assert (false) : "can_bs: ; p_bs bad value";
        return false;
    }

    static boolean nohDisableHighlight;

    static {
        addPropertyChangeListener(highlightSearch, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (G.curwin != null) {
                    nohDisableHighlight = false;
                    ViManager.updateHighlightSearchState();
                }
            }
        });
        addPropertyChangeListener(ignoreCase, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (G.curwin != null) {
                    ViManager.updateHighlightSearchState();
                }
            }
        });
    }

    public static boolean doHighlightSearch() {
        return G.p_hls.value && !nohDisableHighlight;
    }

    static void nohCommand() {
        nohDisableHighlight = true;
        ViManager.updateHighlightSearchState();
    }

    static void newSearch() {
        nohDisableHighlight = false;
        ViManager.updateHighlightSearchState();
        Normal.v_updateVisualState();
    }
}
