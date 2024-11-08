package raptor.swt.chat;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import raptor.Quadrant;
import raptor.Raptor;
import raptor.RaptorWindowItem;
import raptor.alias.RaptorAliasResult;
import raptor.chat.ChatEvent;
import raptor.chat.ChatLogger.ChatEventParseListener;
import raptor.chat.ChatType;
import raptor.chess.Game;
import raptor.connector.Connector;
import raptor.connector.ConnectorListener;
import raptor.international.L10n;
import raptor.pref.PreferenceKeys;
import raptor.pref.RaptorPreferenceStore;
import raptor.script.ParameterScript;
import raptor.service.AliasService;
import raptor.service.ChatService.ChatListener;
import raptor.service.DictionaryService;
import raptor.service.MemoService;
import raptor.service.ScriptService;
import raptor.service.SoundService;
import raptor.service.ThreadService;
import raptor.swt.ItemChangedListener;
import raptor.swt.SWTUtils;
import raptor.swt.chat.controller.ChannelController;
import raptor.swt.chat.controller.MainController;
import raptor.swt.chat.controller.ToolBarItemKey;
import raptor.swt.chess.ChessBoardWindowItem;
import raptor.swt.chess.ChessSquare;
import raptor.swt.chess.controller.PlayingController;
import raptor.util.BrowserUtils;
import raptor.util.OSUtils;
import raptor.util.RaptorLogger;
import raptor.util.RaptorRunnable;
import raptor.util.RaptorStringTokenizer;
import raptor.util.RaptorStringUtils;

public abstract class ChatConsoleController implements PreferenceKeys {

    public static final double CLEAN_PERCENTAGE = .33;

    public static final long SPELL_CHECK_DELAY = 1000;

    private static final RaptorLogger LOG = RaptorLogger.getLog(ChatConsoleController.class);

    public static final int TEXT_CHUNK_SIZE = 1000;

    public static int[] DONT_FORWARD_KEYSTROKES = { SWT.PAGE_UP, SWT.PAGE_DOWN, SWT.HOME, SWT.END };

    public static int[] DONT_FORWARD_KEYMASKS = { SWT.COMMAND, SWT.CONTROL };

    protected static L10n local = L10n.getInstance();

    protected List<ChatEvent> awayList = new ArrayList<ChatEvent>(100);

    protected ChatConsole chatConsole;

    protected Queue<ChatEvent> chatEventQueue = new ConcurrentLinkedQueue<ChatEvent>();

    protected ChatListener chatServiceListener = new ChatListener() {

        public void chatEventOccured(final ChatEvent event) {
            if (!isDisposed && chatConsole != null && !chatConsole.isDisposed()) {
                if (event.getType() == ChatType.CHANNEL_TELL) chatEventQueue.add(event);
                chatConsole.getDisplay().asyncExec(new RaptorRunnable(getConnector()) {

                    @Override
                    public void execute() {
                        onChatEvent(event);
                    }
                });
            } else {
                eventsWhileBeingReparented.add(event);
            }
        }

        public boolean isHandling(final ChatEvent event) {
            if (event.getType() == ChatType.TELL) {
                sourceOfLastTellReceived = event.getSource();
            }
            return isAcceptingChatEvent(event);
        }
    };

    protected Connector connector;

    protected ConnectorListener connectorListener = new ConnectorListener() {

        public void onConnect() {
            fireItemChanged();
        }

        public void onConnecting() {
            fireItemChanged();
        }

        public void onDisconnect() {
            fireItemChanged();
        }
    };

    protected SelectionListener verticalScrollbarListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent e) {
            smartScroll();
        }
    };

    protected Listener consoleInputKeyUpListener = new Listener() {

        public void handleEvent(Event event) {
            processInputTextKeystroke(event, false);
        }
    };

    protected Listener consoleInputKeyDownListener = new Listener() {

        public void handleEvent(Event event) {
            processInputTextKeystroke(event, true);
        }
    };

    protected Listener consoleOutputKeyDownListener = new Listener() {

        public void handleEvent(Event event) {
            processOutputTextKeystroke(event);
        }
    };

    protected List<ChatEvent> eventsWhileBeingReparented = Collections.synchronizedList(new ArrayList<ChatEvent>(100));

    protected boolean hasUnseenText;

    protected boolean ignoreAwayList;

    protected boolean isActive;

    protected long lastCommandLineKeystrokeTime;

    protected String lastSpellCheckLine;

    protected MouseListener inputTextClickListener = new MouseAdapter() {

        @Override
        public void mouseDoubleClick(MouseEvent e) {
            onInputTextDoubleClick(e);
        }

        @Override
        public void mouseUp(MouseEvent e) {
            if (SWTUtils.isRightClick(e)) {
                onInputTextRightClick(e);
            }
        }
    };

    /**
	 * In windows you can only receive mouse wheel events when you have focus.
	 * This method handles wheeling over a chess board in windows.
	 */
    protected MouseWheelListener chessBoardMouseWheelListener = new MouseWheelListener() {

        long lastWheel = System.currentTimeMillis();

        public void mouseScrolled(MouseEvent e) {
            Control cursorControl = Raptor.getInstance().getDisplay().getCursorControl();
            if (cursorControl instanceof ChessSquare) {
                if (System.currentTimeMillis() - lastWheel > 100) {
                    ((ChessSquare) cursorControl).getChessBoard().getController().userMouseWheeled(e.count);
                }
            }
        }
    };

    protected MouseListener outputTextClickListener = new MouseAdapter() {

        @Override
        public void mouseUp(MouseEvent e) {
            if (SWTUtils.isRightClick(e)) {
                onOutputTextRightClick(e);
            }
        }
    };

    protected Runnable spellCheckRunnable = new Runnable() {

        public void run() {
            if (!isDisposed && chatConsole != null && !chatConsole.isDisposed() && isActive && (getPreferences().getBoolean(CHAT_COMMAND_LINE_SPELL_CHECK))) {
                Raptor.getInstance().getDisplay().asyncExec(new RaptorRunnable() {

                    @Override
                    public void execute() {
                        onSpellCheck();
                    }
                });
                ThreadService.getInstance().scheduleOneShot(SPELL_CHECK_DELAY, spellCheckRunnable);
            }
        }
    };

    protected boolean isDisposed;

    protected boolean isDirty;

    protected boolean isSoundDisabled = false;

    protected boolean isAutoScrolling = true;

    protected List<ItemChangedListener> itemChangedListeners = new ArrayList<ItemChangedListener>(5);

    protected List<String> sentText = new ArrayList<String>(50);

    protected int sentTextIndex = 0;

    protected String sourceOfLastTellReceived;

    protected ToolBar toolbar;

    protected Map<ToolBarItemKey, ToolItem> toolItemMap = new HashMap<ToolBarItemKey, ToolItem>();

    public ChatConsoleController(Connector connector) {
        this.connector = connector;
        connector.addConnectorListener(connectorListener);
    }

    protected boolean isSpelledCorrectly(String previousWord, String word) {
        if (previousWord != null && connector.isLikelyCommandPrecedingPersonName(previousWord)) {
            return true;
        } else if (!getPreferences().getBoolean(CHAT_COMMAND_LINE_SPELL_CHECK)) return true; else {
            return connector.isLikelyCommandPrecedingPersonName(word) || connector.isInAutoComplete(word) || DictionaryService.getInstance().isValidWord(word);
        }
    }

    public void onSpellCheck() {
        if (!getPreferences().getBoolean(CHAT_COMMAND_LINE_SPELL_CHECK)) return;
        if (chatConsole.isDisposed() || chatConsole.getOutputText().isDisposed()) {
            return;
        }
        String outputText = chatConsole.getOutputText().getText();
        if (outputText == null || outputText.length() == 0 || (lastSpellCheckLine != null && lastSpellCheckLine.equals(outputText))) {
            return;
        }
        List<int[]> ranges = new ArrayList<int[]>(10);
        StyleRange resetRange = new StyleRange(0, outputText.length(), chatConsole.getOutputText().getForeground(), chatConsole.getOutputText().getBackground());
        chatConsole.getOutputText().setStyleRange(resetRange);
        String firstWord = null;
        int wordStart = -1;
        int wordsCounted = 0;
        for (int i = 0; i < outputText.length(); i++) {
            char currentChar = outputText.charAt(i);
            switch(currentChar) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    {
                        if (wordStart != -1) {
                            String word = outputText.substring(wordStart, i);
                            if (!isSpelledCorrectly(wordsCounted == 1 ? firstWord : null, word)) {
                                ranges.add(new int[] { wordStart, i });
                            }
                            if (firstWord == null) {
                                firstWord = word;
                            }
                            wordsCounted++;
                        }
                        wordStart = -1;
                        continue;
                    }
                default:
                    {
                        if (wordStart == -1) {
                            wordStart = i;
                        }
                    }
            }
        }
        if (wordStart != -1) {
            String word = outputText.substring(wordStart, outputText.length());
            if (!isSpelledCorrectly(wordsCounted == 1 ? firstWord : null, word)) {
                ranges.add(new int[] { wordStart, outputText.length() });
            }
        }
        for (int[] linkRange : ranges) {
            StyleRange range = new StyleRange(linkRange[0], linkRange[1] - linkRange[0], chatConsole.getOutputText().getForeground(), chatConsole.getOutputText().getBackground());
            range.underline = true;
            range.underlineColor = Raptor.getInstance().getDisplay().getSystemColor(SWT.COLOR_RED);
            range.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
            chatConsole.getOutputText().setStyleRange(range);
        }
        lastSpellCheckLine = outputText;
    }

    public void addItemChangedListener(ItemChangedListener listener) {
        itemChangedListeners.add(listener);
    }

    public void addScrollBarListeners() {
        chatConsole.getInputText().getVerticalBar().addSelectionListener(verticalScrollbarListener);
    }

    public void addToolItem(ToolBarItemKey key, ToolItem item) {
        toolItemMap.put(key, item);
    }

    public boolean confirmClose() {
        return true;
    }

    public void dispose() {
        isDisposed = true;
        if (connector != null) {
            connector.getChatService().removeChatServiceListener(chatServiceListener);
            connectorListener = null;
            connector = null;
        }
        if (toolbar != null) {
            toolbar.setVisible(false);
            SWTUtils.clearToolbar(toolbar);
            toolbar = null;
        }
        removeListenersTiedToChatConsole();
        if (itemChangedListeners != null) {
            itemChangedListeners.clear();
            itemChangedListeners = null;
        }
        if (awayList != null) {
            awayList.clear();
            awayList = null;
        }
        if (eventsWhileBeingReparented != null) {
            eventsWhileBeingReparented.clear();
            eventsWhileBeingReparented = null;
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Disposed ChatConsoleController");
        }
    }

    /**
	 * A method that allows subclasses to filter the text before it is appended
	 * to the chatConsole.inputText. The default implementation just returns
	 * what is passed in.
	 * 
	 * @param text
	 *            The incomming text
	 * @return The text to append.
	 */
    public String filterText(String text) {
        return text;
    }

    public ChatConsole getChatConsole() {
        return chatConsole;
    }

    public Connector getConnector() {
        return connector;
    }

    /**
	 * Returns an Image icon that can be used to represent this controller.
	 */
    public Image getIconImage() {
        if (!isActive && hasUnseenText) {
            return Raptor.getInstance().getIcon("chat2");
        } else {
            return null;
        }
    }

    public List<ItemChangedListener> getItemChangedListeners() {
        return itemChangedListeners;
    }

    public abstract String getName();

    public RaptorPreferenceStore getPreferences() {
        return Raptor.getInstance().getPreferences();
    }

    public abstract Quadrant getPreferredQuadrant();

    public String getPrependText(boolean useButtonState) {
        return "";
    }

    public abstract String getPrompt();

    public String getSourceOfLastTellReceived() {
        return sourceOfLastTellReceived;
    }

    /**
	 * Returns the title. THe current format is connector.shortName([CONNECTOR
	 * STATUS IF NOT CONNECTED]getName()).
	 */
    public String getTitle() {
        if (connector == null) {
            return local.getString("chatConsCont1");
        } else if (connector.isConnecting()) {
            return local.getString("chatConsCont2") + getName();
        } else if (connector.isConnected()) {
            return getName();
        } else {
            return local.getString("chatConsCont3") + getName();
        }
    }

    public abstract Control getToolbar(Composite parent);

    public ToolItem getToolItem(ToolBarItemKey key) {
        return toolItemMap.get(key);
    }

    public boolean hasUnseenText() {
        return hasUnseenText;
    }

    public void init() {
        addScrollBarListeners();
        addInputTextKeyListeners();
        addMouseListeners();
        registerForChatEvents();
        adjustAwayButtonEnabled();
        chatConsole.getOutputText().setText(getPrependText(false));
        setCaretToOutputTextEnd();
    }

    public abstract boolean isAcceptingChatEvent(ChatEvent inboundEvent);

    public boolean isAutoScrolling() {
        return isAutoScrolling;
    }

    public abstract boolean isAwayable();

    public abstract boolean isCloseable();

    public boolean isDisposed() {
        return isDisposed;
    }

    public boolean isIgnoringActions() {
        boolean result = false;
        if (isDisposed || chatConsole == null || chatConsole.isDisposed()) {
            LOG.debug("isBeingReparented invoked. The exception is thrown just to debug the stack trace.", new Exception());
            result = true;
        }
        return result;
    }

    public abstract boolean isPrependable();

    public abstract boolean isSearchable();

    public boolean isSoundDisabled() {
        return isSoundDisabled;
    }

    public boolean isToolItemSelected(ToolBarItemKey key) {
        boolean result = false;
        ToolItem item = getToolItem(key);
        if (item != null) {
            return item.getSelection();
        }
        return result;
    }

    public void onActivate() {
        if (!isActive) {
            isActive = true;
            hasUnseenText = false;
            fireItemChanged();
            ThreadService.getInstance().scheduleOneShot(SPELL_CHECK_DELAY, spellCheckRunnable);
        } else {
            if (isAutoScrolling) {
                onForceAutoScroll();
            }
        }
        chatConsole.getDisplay().timerExec(100, new Runnable() {

            public void run() {
                onForceAutoScroll();
                chatConsole.outputText.setFocus();
            }
        });
    }

    public void onAppendChatEventToInputText(ChatEvent event) {
        if (!ignoreAwayList && event.getType() == ChatType.TELL || event.getType() == ChatType.PARTNER_TELL) {
            awayList.add(event);
            adjustAwayButtonEnabled();
        }
        String appendText = null;
        int startIndex = 0;
        synchronized (chatConsole) {
            if (chatConsole.isDisposed()) {
                return;
            }
            if (event.getType() == ChatType.CHANNEL_TELL) event = (chatEventQueue.size() == 0) ? event : chatEventQueue.poll();
            String messageText = filterText(event.getMessage());
            String date = "";
            if (Raptor.getInstance().getPreferences().getBoolean(CHAT_TIMESTAMP_CONSOLE)) {
                SimpleDateFormat format = new SimpleDateFormat(Raptor.getInstance().getPreferences().getString(CHAT_TIMESTAMP_CONSOLE_FORMAT));
                date = format.format(new Date(event.getTime()));
            } else {
                messageText = RaptorStringUtils.removeBeginingNewlines(messageText);
            }
            appendText = (chatConsole.inputText.getCharCount() == 0 ? "" : "\n") + date + messageText;
            chatConsole.inputText.append(appendText);
            startIndex = chatConsole.inputText.getCharCount() - appendText.length();
            if (isAutoScrolling) {
                onForceAutoScroll();
            }
        }
        onDecorateInputText(event, appendText, startIndex);
        reduceInputTextIfNeeded();
    }

    public void onAppendOutputText(String string) {
        chatConsole.outputText.append(string);
    }

    public void onAway() {
        if (isAwayable()) {
            ignoreAwayList = true;
            onAppendChatEventToInputText(new ChatEvent(null, ChatType.OUTBOUND, local.getString("chatConsCont4")));
            for (ChatEvent event : awayList) {
                onAppendChatEventToInputText(event);
            }
            awayList.clear();
            ignoreAwayList = false;
            adjustAwayButtonEnabled();
        }
    }

    public void onChatEvent(ChatEvent event) {
        onAppendChatEventToInputText(event);
        if (!isIgnoringActions()) {
            playSounds(event);
            updateImageIcon(event);
        }
    }

    public void onForceAutoScroll() {
        if (isIgnoringActions()) {
            return;
        }
        int chars = chatConsole.inputText.getCharCount();
        chatConsole.inputText.setCaretOffset(chars);
        chatConsole.inputText.setSelection(chars + 1, chars + 1);
    }

    public void onPassivate() {
        if (isActive) {
            isActive = false;
            hasUnseenText = false;
        }
    }

    public void onSave() {
        if (isIgnoringActions()) {
            return;
        }
        FileDialog fd = new FileDialog(chatConsole.getShell(), SWT.SAVE);
        fd.setText(local.getString("chatConsCont5"));
        fd.setFilterPath("");
        String[] filterExt = { "*.txt", "*.*" };
        fd.setFilterExtensions(filterExt);
        final String selected = fd.open();
        if (selected != null) {
            chatConsole.getDisplay().asyncExec(new RaptorRunnable(getConnector()) {

                public void execute() {
                    FileWriter writer = null;
                    try {
                        writer = new FileWriter(selected);
                        writer.append(local.getString("chatConsCont6")).append(String.valueOf(new Date())).append("\n");
                        int i = 0;
                        while (i < chatConsole.getInputText().getCharCount() - 1) {
                            int endIndex = i + TEXT_CHUNK_SIZE;
                            if (endIndex >= chatConsole.getInputText().getCharCount()) {
                                endIndex = i + chatConsole.getInputText().getCharCount() - i - 1;
                            }
                            String string = chatConsole.getInputText().getText(i, endIndex);
                            writer.append(string);
                            i = endIndex;
                        }
                        writer.flush();
                    } catch (Throwable t) {
                        LOG.error("Error writing file: " + selected, t);
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException ioe) {
                            }
                        }
                    }
                }
            });
        }
    }

    public void onSearch() {
        if (!isIgnoringActions()) {
            chatConsole.getDisplay().asyncExec(new RaptorRunnable(getConnector()) {

                public void execute() {
                    String searchString = chatConsole.outputText.getText();
                    if (StringUtils.isBlank(searchString)) {
                        MessageBox box = new MessageBox(chatConsole.getShell(), SWT.ICON_INFORMATION | SWT.OK);
                        box.setMessage(local.getString("chatConsCont7"));
                        box.setText(local.getString("chatConsCont8"));
                        box.open();
                    } else {
                        boolean foundText = false;
                        String searchStringUpper = searchString.toUpperCase();
                        int start = chatConsole.inputText.getCaretOffset();
                        if (start >= chatConsole.inputText.getCharCount()) {
                            start = chatConsole.inputText.getCharCount() - 1;
                        } else if (start - searchStringUpper.length() + 1 >= 0) {
                            String text = chatConsole.inputText.getText(start - searchStringUpper.length(), start - 1);
                            if (text.equalsIgnoreCase(searchStringUpper)) {
                                start -= searchStringUpper.length();
                            }
                        }
                        while (start > 0) {
                            int charsBack = 0;
                            if (start - TEXT_CHUNK_SIZE > 0) {
                                charsBack = TEXT_CHUNK_SIZE;
                            } else {
                                charsBack = start;
                            }
                            String stringToSearch = chatConsole.inputText.getText(start - charsBack, start).toUpperCase();
                            int index = stringToSearch.lastIndexOf(searchStringUpper);
                            if (index != -1) {
                                int textStart = start - charsBack + index;
                                chatConsole.inputText.setSelection(textStart, textStart + searchStringUpper.length());
                                foundText = true;
                                break;
                            }
                            start -= charsBack;
                        }
                        if (!foundText) {
                            MessageBox box = new MessageBox(chatConsole.getShell(), SWT.ICON_INFORMATION | SWT.OK);
                            box.setMessage(local.getString("chatConsCont9") + searchString + "'.");
                            box.setText(local.getString("chatConsCont8"));
                            box.open();
                        }
                    }
                }
            });
        }
    }

    public void onSendOutputText() {
        String text = chatConsole.outputText.getText();
        RaptorAliasResult alias = AliasService.getInstance().processAlias(this, text);
        if (alias == null) {
            connector.sendMessage(text);
        } else if (alias.getNewText() != null) {
            connector.sendMessage(alias.getNewText());
        }
        if (alias != null && alias.getUserMessage() != null) {
            onAppendChatEventToInputText(new ChatEvent(null, ChatType.INTERNAL, alias.getUserMessage()));
        }
        chatConsole.outputText.setText(getPrependText(true));
        setCaretToOutputTextEnd();
        awayList.clear();
        adjustAwayButtonEnabled();
        smartScroll(true);
    }

    public void processInputTextKeystroke(Event event, boolean isKeyUp) {
        if (isKeyUp) {
            boolean isSmartScrolling = false;
            for (int i = 0; i < DONT_FORWARD_KEYSTROKES.length; i++) {
                if (event.keyCode == DONT_FORWARD_KEYSTROKES[i]) {
                    isSmartScrolling = true;
                    break;
                }
            }
            if (isSmartScrolling) {
                smartScroll();
            }
        } else if (!isKeyUp) {
            if (ChatUtils.processHotkeyActions(this, event)) {
                return;
            }
            boolean dontForwardKeystroke = false;
            for (int i = 0; i < DONT_FORWARD_KEYSTROKES.length; i++) {
                if (event.keyCode == DONT_FORWARD_KEYSTROKES[i]) {
                    dontForwardKeystroke = true;
                    break;
                }
            }
            if (!dontForwardKeystroke) {
                for (int i = 0; i < DONT_FORWARD_KEYMASKS.length; i++) {
                    if ((event.stateMask & DONT_FORWARD_KEYMASKS[i]) != 0) {
                        dontForwardKeystroke = true;
                        break;
                    }
                }
                if (!dontForwardKeystroke) {
                    chatConsole.getOutputText().setFocus();
                    if (event.character == '\b') {
                        if (chatConsole.outputText.getCharCount() > 0) {
                            chatConsole.outputText.setText(chatConsole.outputText.getText().substring(0, chatConsole.outputText.getText().length() - 1));
                            chatConsole.outputText.setSelection(chatConsole.outputText.getText().length());
                        }
                    } else {
                        String textToInsert = String.valueOf(event.character);
                        chatConsole.getOutputText().insert(textToInsert);
                        chatConsole.getOutputText().setCaretOffset(chatConsole.getOutputText().getCaretOffset() + 1);
                    }
                }
            }
        }
    }

    public boolean processOutputTextKeystroke(Event event) {
        lastCommandLineKeystrokeTime = System.currentTimeMillis();
        if (ChatUtils.processHotkeyActions(this, event)) {
            return true;
        }
        if (event.keyCode == SWT.ESC) {
            chatConsole.outputText.setText("");
            RaptorWindowItem[] items = Raptor.getInstance().getWindow().getWindowItems(ChessBoardWindowItem.class);
            for (RaptorWindowItem item : items) {
                ChessBoardWindowItem chessBoardItem = (ChessBoardWindowItem) item;
                if (chessBoardItem.getController() instanceof PlayingController) {
                    ((PlayingController) chessBoardItem.getController()).onClearPremoves();
                }
            }
            return true;
        } else if (event.keyCode == SWT.ARROW_UP) {
            if (sentTextIndex >= 0) {
                if (sentTextIndex > 0) {
                    sentTextIndex--;
                }
                if (!sentText.isEmpty()) {
                    chatConsole.outputText.setText("");
                    chatConsole.outputText.append(sentText.get(sentTextIndex));
                    chatConsole.outputText.setCaretOffset(chatConsole.outputText.getCharCount());
                    onSpellCheck();
                }
            }
            return true;
        } else if (event.keyCode == SWT.ARROW_DOWN) {
            if (sentTextIndex < sentText.size() - 1) {
                sentTextIndex++;
                chatConsole.outputText.setText("");
                chatConsole.outputText.append(sentText.get(sentTextIndex));
                chatConsole.outputText.setCaretOffset(chatConsole.outputText.getCharCount());
                onSpellCheck();
            } else {
                chatConsole.outputText.setText("");
            }
            return true;
        } else if (event.character == '\r') {
            if (sentText.size() > 50) {
                sentText.remove(0);
            }
            sentText.add(chatConsole.outputText.getText().substring(0, chatConsole.outputText.getText().length()));
            sentTextIndex = sentText.size();
            onSendOutputText();
            return true;
        } else if (isAutoCompleteTrigger(event)) {
            int endIndex = chatConsole.getOutputText().getCaretOffset();
            int startIndex = endIndex - 1;
            for (; startIndex >= 0; startIndex--) {
                if (chatConsole.getOutputText().getContent().getTextRange(startIndex, 1).equals(" ")) {
                    startIndex++;
                    break;
                }
            }
            if (startIndex < 0) {
                startIndex = 0;
            }
            String wordToAutoComplete = chatConsole.getOutputText().getContent().getTextRange(startIndex, endIndex - startIndex).trim();
            if (wordToAutoComplete.length() > 0) {
                String[] autoComplete = connector.autoComplete(wordToAutoComplete);
                if (autoComplete.length == 0) {
                    onAppendChatEventToInputText(new ChatEvent(null, ChatType.INTERNAL, local.getString("chatConsCont10")));
                } else if (autoComplete.length == 1) {
                    chatConsole.getOutputText().insert(autoComplete[0].substring(wordToAutoComplete.length()));
                    chatConsole.getOutputText().setCaretOffset(startIndex + autoComplete[0].length());
                } else {
                    StringBuilder matchesBuilder = new StringBuilder(100);
                    matchesBuilder.append(local.getString("chatConsCont11"));
                    int counter = 0;
                    for (int i = 0; i < autoComplete.length; i++) {
                        matchesBuilder.append(StringUtils.rightPad(autoComplete[i], 20));
                        counter++;
                        if (counter == 3) {
                            matchesBuilder.append("\n");
                            counter++;
                        }
                    }
                    onAppendChatEventToInputText(new ChatEvent(null, ChatType.INTERNAL, matchesBuilder.toString()));
                }
            } else {
                Raptor.getInstance().getDisplay().beep();
            }
            event.doit = false;
            return true;
        }
        return false;
    }

    protected boolean isAutoCompleteTrigger(Event keyEvent) {
        boolean isMaskedKey = (keyEvent.stateMask & SWT.CONTROL) != 0 || (keyEvent.stateMask & SWT.ALT) != 0 || (keyEvent.stateMask & SWT.COMMAND) != 0;
        return isMaskedKey && (keyEvent.character == ' ' || keyEvent.keyCode == ' ');
    }

    public void removeItemChangedListener(ItemChangedListener listener) {
        itemChangedListeners.remove(listener);
    }

    public void setAutoScrolling(boolean isAutoScrolling) {
        if (isAutoScrolling != this.isAutoScrolling) {
            this.isAutoScrolling = isAutoScrolling;
            if (isAutoScrolling) {
                ToolItem item = toolItemMap.get(ToolBarItemKey.AUTO_SCROLL_BUTTON);
                if (item != null) {
                    item.setSelection(true);
                    item.setImage(Raptor.getInstance().getIcon("locked"));
                    item.setToolTipText(local.getString("chatConsCont12"));
                }
                onForceAutoScroll();
            } else {
                ToolItem item = toolItemMap.get(ToolBarItemKey.AUTO_SCROLL_BUTTON);
                if (item != null) {
                    item.setSelection(false);
                    item.setImage(Raptor.getInstance().getIcon("unlocked"));
                    item.setToolTipText(local.getString("chatConsCont13"));
                }
            }
        }
    }

    public void setChatConsole(ChatConsole chatConsole) {
        this.chatConsole = chatConsole;
    }

    public void setHasUnseenText(boolean hasUnseenText) {
        this.hasUnseenText = hasUnseenText;
    }

    public void setInputToLastTell() {
        if (sourceOfLastTellReceived != null) {
            chatConsole.outputText.setText(connector.getTellToString(sourceOfLastTellReceived));
            chatConsole.outputText.setSelection(chatConsole.outputText.getCharCount() + 1);
        }
    }

    public void setItemChangedListeners(List<ItemChangedListener> itemChangedListeners) {
        this.itemChangedListeners = itemChangedListeners;
    }

    public void setSoundDisabled(boolean isSoundDisabled) {
        this.isSoundDisabled = isSoundDisabled;
    }

    public void setSourceOfLastTellReceived(String sourceOfLastTellReceived) {
        this.sourceOfLastTellReceived = sourceOfLastTellReceived;
    }

    public void setToolItemEnabled(ToolBarItemKey key, boolean isEnabled) {
        ToolItem item = getToolItem(key);
        if (item != null) {
            item.setEnabled(isEnabled);
        }
    }

    public void setToolItemSelected(ToolBarItemKey key, boolean isSelected) {
        ToolItem item = getToolItem(key);
        if (item != null) {
            item.setSelection(isSelected);
        }
    }

    protected void addChannelMenuItems(Menu menu, String word) {
        if (connector.isLikelyChannel(word)) {
            if (menu.getItemCount() > 0) {
                new MenuItem(menu, SWT.SEPARATOR);
            }
            final String channel = connector.parseChannel(word);
            MenuItem item = null;
            item = new MenuItem(menu, SWT.PUSH);
            item.setText(local.getString("chatConsCont14") + channel);
            item.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    if (!Raptor.getInstance().getWindow().containsChannelItem(connector, channel)) {
                        ChatConsoleWindowItem windowItem = new ChatConsoleWindowItem(new ChannelController(connector, channel));
                        Raptor.getInstance().getWindow().addRaptorWindowItem(windowItem, false);
                        ChatUtils.appendPreviousChatsToController(windowItem.console);
                    }
                }
            });
            final String[][] connectorChannelItems = connector.getChannelActions(channel);
            if (connectorChannelItems != null) {
                MenuItem channelItem = new MenuItem(menu, SWT.CASCADE);
                channelItem.setText(connector.getShortName() + local.getString("chatConsCont15") + channel);
                Menu channelItemMenu = new Menu(menu);
                channelItem.setMenu(channelItemMenu);
                for (int i = 0; i < connectorChannelItems.length; i++) {
                    item = new MenuItem(channelItemMenu, SWT.PUSH);
                    item.setText(connectorChannelItems[i][0]);
                    final int index = i;
                    item.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event e) {
                            connector.sendMessage(connectorChannelItems[index][1]);
                        }
                    });
                }
            }
        }
    }

    protected void addCommandMenuItems(Menu menu, String message, int caretPosition) {
        if (message != null && message.length() <= 200) {
            if (menu.getItemCount() > 0) {
                new MenuItem(menu, SWT.SEPARATOR);
            }
            final Map<String, Object> parameterMap = new HashMap<String, Object>();
            final String line = chatConsole.inputText.getLine(chatConsole.inputText.getLineAtOffset(caretPosition));
            connector.getChatService().getChatLogger().parseFile(new ChatEventParseListener() {

                public boolean onNewEventParsed(ChatEvent event) {
                    if (event.getMessage().contains(line)) {
                        parameterMap.put("chatEvent", event);
                        return false;
                    }
                    return true;
                }

                public void onParseCompleted() {
                }
            });
            parameterMap.put("selection", message);
            if (parameterMap.containsKey("chatEvent")) {
                MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
                menuItem.setText(local.getString("chatConsCont16"));
                menuItem.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        MemoService.getInstance().addMemo((ChatEvent) parameterMap.get("chatEvent"));
                        onAppendChatEventToInputText(new ChatEvent(null, ChatType.INTERNAL, local.getString("chatConsCont17")));
                    }
                });
            }
            if (message.startsWith("http") || message.endsWith(".com") || message.endsWith(".edu") || message.endsWith(".org")) {
                if (!message.startsWith("http")) {
                    message = "http://" + message;
                }
                final String url = message.replace("\"", "");
                MenuItem internalBrowserItem = new MenuItem(menu, SWT.PUSH);
                internalBrowserItem.setText(local.getString("chatConsCont18") + message + "'");
                internalBrowserItem.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        BrowserUtils.openInternalUrl(url);
                    }
                });
                MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
                menuItem.setText(local.getString("chatConsCont18") + message + "'");
                menuItem.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        BrowserUtils.openExternalUrl(url);
                    }
                });
            }
            MenuItem scriptsItem = new MenuItem(menu, SWT.CASCADE);
            scriptsItem.setText(local.getString("chatConsCont19"));
            Menu scriptsMenu = new Menu(menu);
            scriptsItem.setMenu(scriptsMenu);
            ParameterScript[] scripts = ScriptService.getInstance().getParameterScripts(connector.getScriptConnectorType(), ParameterScript.Type.ConsoleRightClickScripts);
            for (final ParameterScript script : scripts) {
                MenuItem menuItem = new MenuItem(scriptsMenu, SWT.PUSH);
                menuItem.setText(script.getName() + ": '" + message + "'");
                menuItem.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        script.execute(connector.getParameterScriptContext(parameterMap));
                    }
                });
            }
        }
    }

    protected void addGameIdMenuItems(Menu menu, String word) {
        if (connector.isLikelyGameId(word)) {
            word = connector.parseGameId(word);
            if (menu.getItemCount() > 0) {
                new MenuItem(menu, SWT.SEPARATOR);
            }
            MenuItem item = null;
            final String gameId = connector.parseGameId(word);
            item = new MenuItem(menu, SWT.PUSH);
            item.setText(local.getString("chatConsCont20") + word);
            item.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    ChatUtils.openGameChatTab(getConnector(), gameId, true);
                }
            });
            final String[][] gameIdItems = connector.getGameIdActions(gameId);
            if (gameIdItems != null) {
                MenuItem gameCommands = new MenuItem(menu, SWT.CASCADE);
                gameCommands.setText(connector.getShortName() + local.getString("chatConsCont21") + word);
                Menu gameCommandsMenu = new Menu(menu);
                gameCommands.setMenu(gameCommandsMenu);
                for (int i = 0; i < gameIdItems.length; i++) {
                    item = new MenuItem(gameCommandsMenu, SWT.PUSH);
                    item.setText(gameIdItems[i][0]);
                    final int index = i;
                    item.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event e) {
                            connector.sendMessage(gameIdItems[index][1]);
                        }
                    });
                }
            }
        }
    }

    protected void addInputTextKeyListeners() {
        if (!isIgnoringActions()) {
            chatConsole.outputText.addListener(SWT.KeyDown, consoleOutputKeyDownListener);
            chatConsole.inputText.addListener(SWT.KeyDown, consoleInputKeyDownListener);
            chatConsole.inputText.addListener(SWT.KeyUp, consoleInputKeyUpListener);
            if (chatConsole.inputText.getVerticalBar() != null) {
                chatConsole.inputText.getVerticalBar().addListener(SWT.KeyUp, consoleInputKeyUpListener);
                chatConsole.inputText.getVerticalBar().addListener(SWT.KeyDown, consoleInputKeyDownListener);
            }
        }
    }

    protected void addMouseListeners() {
        if (!isIgnoringActions()) {
            chatConsole.inputText.addMouseListener(inputTextClickListener);
            chatConsole.outputText.addMouseListener(outputTextClickListener);
            if (OSUtils.isLikelyWindows()) {
                chatConsole.inputText.addMouseWheelListener(chessBoardMouseWheelListener);
                chatConsole.outputText.addMouseWheelListener(chessBoardMouseWheelListener);
            }
        }
    }

    protected void addPersonMenuItems(Menu menu, String word) {
        ChatUtils.addPersonMenuItems(menu, connector, word);
    }

    protected void adjustAwayButtonEnabled() {
        setToolItemEnabled(ToolBarItemKey.AWAY_BUTTON, !awayList.isEmpty());
    }

    protected void decorateBugWhoLinks(ChatEvent event, String message, int textStartPosition) {
        if ((event.getType() == ChatType.BUGWHO_ALL || event.getType() == ChatType.BUGWHO_GAMES) && getPreferences().getBoolean(PreferenceKeys.CHAT_UNDERLINE_COMMANDS)) {
            List<int[]> linkRanges = new ArrayList<int[]>(5);
            int lastNewlineIndex = 0;
            int newLineIndex = 0;
            while ((newLineIndex = message.indexOf('\n', lastNewlineIndex + 1)) != -1) {
                String line = message.substring(lastNewlineIndex + 1, newLineIndex).trim();
                if (StringUtils.isNotBlank(line) && (line.contains("W:") || line.contains("B:"))) {
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        String firstWord = line.substring(0, spaceIndex);
                        if (NumberUtils.isDigits(firstWord)) {
                            linkRanges.add(new int[] { lastNewlineIndex + 1, newLineIndex });
                        }
                    }
                }
                lastNewlineIndex = newLineIndex;
            }
            for (int[] linkRange : linkRanges) {
                StyleRange range = new StyleRange(textStartPosition + linkRange[0], linkRange[1] - linkRange[0], getPreferences().getColor(event), chatConsole.inputText.getBackground());
                range.underline = true;
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    protected void decorateForegroundColor(ChatEvent event, String message, int textStartPosition) {
        Color color = getPreferences().getColor(event);
        if (color == null) {
            color = chatConsole.inputText.getForeground();
        }
        String prompt = connector.getPrompt();
        if (message.endsWith(prompt)) {
            message = message.substring(0, message.length() - prompt.length());
        }
        chatConsole.inputText.setStyleRange(new StyleRange(textStartPosition, message.length(), color, chatConsole.inputText.getBackground()));
    }

    protected void decorateGameNotifyLinks(ChatEvent event, String message, int textStartPosition) {
        if (event.getType() == ChatType.UNKNOWN && (message.startsWith("\nGame notification: ") || message.startsWith("Game notification: ")) && getPreferences().getBoolean(PreferenceKeys.CHAT_UNDERLINE_COMMANDS)) {
            int startIndex = message.indexOf("Game notification:");
            int endIndex = -1;
            for (int i = startIndex; i < message.length(); i++) {
                if (!Character.isWhitespace(message.charAt(i))) {
                    endIndex = i;
                }
            }
            if (endIndex != -1) {
                StyleRange range = new StyleRange(textStartPosition + startIndex, (endIndex - startIndex) + 1, getPreferences().getColor(event), chatConsole.inputText.getBackground());
                range.underline = true;
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    protected void decorateGamesLinks(ChatEvent event, String message, int textStartPosition) {
        if (event.getType() == ChatType.GAMES && getPreferences().getBoolean(PreferenceKeys.CHAT_UNDERLINE_COMMANDS)) {
            List<int[]> linkRanges = new ArrayList<int[]>(5);
            int lastNewlineIndex = 0;
            int newLineIndex = 0;
            while ((newLineIndex = message.indexOf('\n', lastNewlineIndex + 1)) != -1) {
                String line = message.substring(lastNewlineIndex + 1, newLineIndex).trim();
                if (StringUtils.isNotBlank(line) && (line.contains("W:") || line.contains("B:"))) {
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        String firstWord = line.substring(0, spaceIndex);
                        if (NumberUtils.isDigits(firstWord)) {
                            linkRanges.add(new int[] { lastNewlineIndex + 1, newLineIndex });
                        }
                    }
                }
                lastNewlineIndex = newLineIndex;
            }
            for (int[] linkRange : linkRanges) {
                StyleRange range = new StyleRange(textStartPosition + linkRange[0], linkRange[1] - linkRange[0], getPreferences().getColor(event), chatConsole.inputText.getBackground());
                range.underline = true;
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    protected void decorateHistoryLinks(ChatEvent event, String message, int textStartPosition) {
        if (event.getType() == ChatType.HISTORY && getPreferences().getBoolean(PreferenceKeys.CHAT_UNDERLINE_COMMANDS)) {
            List<int[]> linkRanges = new ArrayList<int[]>(5);
            int lastNewlineIndex = 0;
            int newLineIndex = 0;
            while ((newLineIndex = message.indexOf('\n', lastNewlineIndex + 1)) != -1) {
                String line = message.substring(lastNewlineIndex + 1, newLineIndex).trim();
                if (StringUtils.isNotBlank(line)) {
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        String firstWord = line.substring(0, spaceIndex);
                        if (firstWord.endsWith(":")) {
                            firstWord = firstWord.substring(0, firstWord.length() - 1);
                            if (NumberUtils.isDigits(firstWord)) {
                                linkRanges.add(new int[] { lastNewlineIndex + 1, newLineIndex });
                            }
                        }
                    }
                }
                lastNewlineIndex = newLineIndex;
            }
            if (lastNewlineIndex != 0) {
                String line = message.substring(lastNewlineIndex + 1, message.length()).trim();
                if (StringUtils.isNotBlank(line)) {
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        String firstWord = line.substring(0, spaceIndex);
                        if (firstWord.endsWith(":")) {
                            firstWord = firstWord.substring(0, firstWord.length() - 1);
                            if (NumberUtils.isDigits(firstWord)) {
                                linkRanges.add(new int[] { lastNewlineIndex + 1, message.length() });
                            }
                        }
                    }
                }
            }
            for (int[] linkRange : linkRanges) {
                StyleRange range = new StyleRange(textStartPosition + linkRange[0], linkRange[1] - linkRange[0], getPreferences().getColor(event), chatConsole.inputText.getBackground());
                range.underline = true;
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    protected void decorateJournalLinks(ChatEvent event, String message, int textStartPosition) {
        if (event.getType() == ChatType.JOURNAL && getPreferences().getBoolean(PreferenceKeys.CHAT_UNDERLINE_COMMANDS)) {
            List<int[]> linkRanges = new ArrayList<int[]>(5);
            int lastNewlineIndex = 0;
            int newLineIndex = 0;
            while ((newLineIndex = message.indexOf('\n', lastNewlineIndex + 1)) != -1) {
                String line = message.substring(lastNewlineIndex + 1, newLineIndex);
                if (StringUtils.isNotBlank(line)) {
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        String firstWord = line.substring(0, spaceIndex);
                        if (firstWord.endsWith(":")) {
                            firstWord = firstWord.substring(1, firstWord.length() - 1);
                            if (NumberUtils.isDigits(firstWord)) {
                                firstWord = firstWord.substring(0, firstWord.length() - 1);
                                if (NumberUtils.isDigits(firstWord)) {
                                    int lastIndex = newLineIndex;
                                    while (Character.isWhitespace(message.charAt(lastIndex))) {
                                        lastIndex--;
                                    }
                                    linkRanges.add(new int[] { lastNewlineIndex + 1, lastIndex + 1 });
                                }
                            }
                        }
                    }
                }
                lastNewlineIndex = newLineIndex;
            }
            if (lastNewlineIndex != 0) {
                String line = message.substring(lastNewlineIndex + 1, message.length());
                if (StringUtils.isNotBlank(line)) {
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        String firstWord = line.substring(0, spaceIndex);
                        if (firstWord.endsWith(":")) {
                            firstWord = firstWord.substring(1, firstWord.length() - 1);
                            if (NumberUtils.isDigits(firstWord)) {
                                int lastIndex = message.length() - 1;
                                while (Character.isWhitespace(message.charAt(lastIndex))) {
                                    lastIndex--;
                                }
                                linkRanges.add(new int[] { lastNewlineIndex + 1, lastIndex + 1 });
                            }
                        }
                    }
                }
            }
            for (int[] linkRange : linkRanges) {
                StyleRange range = new StyleRange(textStartPosition + linkRange[0], linkRange[1] - linkRange[0], getPreferences().getColor(event), chatConsole.inputText.getBackground());
                range.underline = true;
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    protected void decorateLinks(ChatEvent event, String message, int textStartPosition) {
        if (event.getType() != ChatType.OUTBOUND && getPreferences().getBoolean(CHAT_UNDERLINE_URLS) && event.getType() != ChatType.GAMES && event.getType() != ChatType.BUGWHO_ALL && event.getType() != ChatType.BUGWHO_GAMES && event.getType() != ChatType.BUGWHO_AVAILABLE_TEAMS && event.getType() != ChatType.BUGWHO_UNPARTNERED_BUGGERS && !message.endsWith("(*) indicates system administrator.")) {
            List<int[]> linkRanges = new ArrayList<int[]>(5);
            int startIndex = message.indexOf("http://");
            if (startIndex == -1) {
                startIndex = message.indexOf("https://");
                if (startIndex == -1) {
                    startIndex = message.indexOf("www.");
                }
            }
            while (startIndex != -1 && startIndex < message.length()) {
                int endIndex = startIndex + 1;
                while (endIndex < message.length()) {
                    if (message.charAt(endIndex) == '\n' && message.length() > endIndex + 1 && message.charAt(endIndex + 1) == '\\') {
                        endIndex += 2;
                        while (endIndex < message.length() && Character.isWhitespace(message.charAt(endIndex))) {
                            endIndex++;
                        }
                        continue;
                    } else if (Character.isWhitespace(message.charAt(endIndex))) {
                        break;
                    }
                    endIndex++;
                }
                if (message.charAt(endIndex - 1) == '.') {
                    endIndex--;
                }
                linkRanges.add(new int[] { startIndex, endIndex });
                startIndex = message.indexOf("http://", endIndex + 1);
                if (startIndex == -1) {
                    startIndex = message.indexOf("https://", endIndex + 1);
                    if (startIndex == -1) {
                        startIndex = message.indexOf("www.", endIndex + 1);
                    }
                }
            }
            StringBuilder dom = new StringBuilder();
            int endIndex = ChatUtils.getEndIndexOfUrl(startIndex, linkRanges, message, dom);
            int linkEnd = endIndex + dom.length() - 1;
            if (message.charAt(linkEnd) == '/') {
                for (; linkEnd < message.length() && !Character.isWhitespace(message.charAt(linkEnd)); linkEnd++) ;
            }
            while (endIndex != -1) {
                startIndex = endIndex--;
                while (startIndex >= 0) {
                    if (Character.isWhitespace(message.charAt(startIndex))) {
                        break;
                    }
                    startIndex--;
                }
                int atIndex = message.indexOf('@', startIndex);
                if (atIndex == -1 || atIndex > linkEnd) {
                    linkRanges.add(new int[] { startIndex + 1, linkEnd });
                }
                dom = new StringBuilder();
                endIndex = ChatUtils.getEndIndexOfUrl(startIndex, linkRanges, message, linkEnd + 1, dom);
                linkEnd = endIndex + dom.length() - 1;
            }
            for (int[] linkRange : linkRanges) {
                Color underlineColor = chatConsole.getPreferences().getColor(CHAT_LINK_UNDERLINE_COLOR);
                StyleRange range = new StyleRange(textStartPosition + linkRange[0], linkRange[1] - linkRange[0], underlineColor, chatConsole.inputText.getBackground());
                range.underline = true;
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    protected void decorateNewsLinks(ChatEvent event, String message, int textStartPosition) {
        if (event.getType() == ChatType.UNKNOWN && isPossibleNewsMessage(message) && getPreferences().getBoolean(PreferenceKeys.CHAT_UNDERLINE_COMMANDS)) {
            List<int[]> linkRanges = new ArrayList<int[]>(5);
            int lastNewlineIndex = 0;
            int newLineIndex = 0;
            while ((newLineIndex = message.indexOf('\n', lastNewlineIndex + 1)) != -1) {
                String line = message.substring(lastNewlineIndex + 1, newLineIndex).trim();
                if (StringUtils.isNotBlank(line) && (line.contains("(") || line.contains(")")) && !line.contains("W:") && !line.contains("B:")) {
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        String firstWord = line.substring(0, spaceIndex);
                        if (NumberUtils.isDigits(firstWord)) {
                            linkRanges.add(new int[] { lastNewlineIndex + 1, newLineIndex });
                        }
                    }
                }
                lastNewlineIndex = newLineIndex;
            }
            if (lastNewlineIndex != 0) {
                String line = message.substring(lastNewlineIndex + 1, message.length()).trim();
                if (StringUtils.isNotBlank(line) && (line.contains("(") || line.contains(")")) && !line.contains("W:") && !line.contains("B:")) {
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        String firstWord = line.substring(0, spaceIndex);
                        if (NumberUtils.isDigits(firstWord)) {
                            linkRanges.add(new int[] { lastNewlineIndex + 1, message.length() });
                        }
                    }
                }
            }
            for (int[] linkRange : linkRanges) {
                StyleRange range = new StyleRange(textStartPosition + linkRange[0], linkRange[1] - linkRange[0], getPreferences().getColor(event), chatConsole.inputText.getBackground());
                range.underline = true;
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    protected void decorateQuotes(ChatEvent event, String message, int textStartPosition) {
        if (event.getType() != ChatType.OUTBOUND) {
            boolean isUnderliningSingleQuotes = getPreferences().getBoolean(CHAT_UNDERLINE_SINGLE_QUOTES);
            boolean isUnderliningDoubleQuotes = getPreferences().getBoolean(CHAT_UNDERLINE_QUOTED_TEXT);
            if (!isUnderliningSingleQuotes && !isUnderliningDoubleQuotes) {
                return;
            }
            List<int[]> quotedRanges = new ArrayList<int[]>(5);
            int quoteIndex = !isUnderliningDoubleQuotes ? -1 : message.indexOf('\"');
            if (quoteIndex == -1 && isUnderliningSingleQuotes) {
                quoteIndex = message.indexOf('\'');
            }
            while (quoteIndex != -1) {
                int endQuote = !isUnderliningDoubleQuotes ? -1 : message.indexOf('\"', quoteIndex + 1);
                if (endQuote == -1 && isUnderliningSingleQuotes) {
                    endQuote = message.indexOf('\'', quoteIndex + 1);
                }
                if (endQuote == -1) {
                    break;
                } else {
                    if (quoteIndex + 1 != endQuote) {
                        int newLine = message.indexOf('\n', quoteIndex);
                        boolean isASpaceTwoCharsAfterQuote = message.charAt(quoteIndex + 2) == ' ';
                        boolean doQuotesMatch = message.charAt(quoteIndex) == message.charAt(endQuote);
                        if (!(newLine > quoteIndex && newLine < endQuote) && !isASpaceTwoCharsAfterQuote && doQuotesMatch) {
                            quotedRanges.add(new int[] { quoteIndex + 1, endQuote });
                        }
                    }
                }
                quoteIndex = !isUnderliningDoubleQuotes ? -1 : message.indexOf('\"', endQuote + 1);
                if (quoteIndex == -1 && isUnderliningSingleQuotes) {
                    quoteIndex = message.indexOf('\'', endQuote + 1);
                }
            }
            for (int[] quotedRange : quotedRanges) {
                Color underlineColor = chatConsole.getPreferences().getColor(CHAT_QUOTE_UNDERLINE_COLOR);
                StyleRange range = new StyleRange(textStartPosition + quotedRange[0], quotedRange[1] - quotedRange[0], underlineColor, chatConsole.inputText.getBackground());
                range.underline = true;
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    protected void decoreateNext(ChatEvent event, String message, int textStartPosition) {
        if (event.getType() != ChatType.OUTBOUND && getPreferences().getBoolean(PreferenceKeys.CHAT_UNDERLINE_COMMANDS)) {
            List<int[]> nextRanges = new ArrayList<int[]>(5);
            int nextIndex = message.indexOf("[next]");
            while (nextIndex != -1) {
                nextRanges.add(new int[] { nextIndex, nextIndex + 6 });
                nextIndex = message.indexOf("[next]", nextIndex + 1);
            }
            for (int[] nextRange : nextRanges) {
                Color underlineColor = chatConsole.getPreferences().getColor(CHAT_QUOTE_UNDERLINE_COLOR);
                StyleRange range = new StyleRange(textStartPosition + nextRange[0], nextRange[1] - nextRange[0], underlineColor, chatConsole.inputText.getBackground());
                chatConsole.inputText.setStyleRange(range);
            }
        }
    }

    /**
	 * Should be invoked when the title or closeability changes.
	 */
    protected void fireItemChanged() {
        if (!isDisposed) {
            for (ItemChangedListener listener : itemChangedListeners) {
                listener.itemStateChanged();
            }
        }
    }

    protected boolean isExaminingAGame() {
        Game[] games = connector.getGameService().getAllActiveGames();
        boolean result = false;
        for (Game game : games) {
            if (game.isInState(Game.EXAMINING_STATE) || game.isInState(Game.SETUP_STATE)) {
                result = true;
                break;
            }
        }
        return result;
    }

    protected boolean isPossibleNewsMessage(String message) {
        return message.startsWith("Index of news items ") || message.startsWith("Index of news items ", 1) || message.startsWith("Index of all news items:") || message.startsWith("Index of all news items:", 1) || message.startsWith("Index of the last few news items:") || message.contains("\nIndex of the last few news items:") || message.startsWith("\nIndex of new news items:") || message.startsWith("\nIndex of new news items:", 1) || message.contains("\nIndex of new news items:");
    }

    protected void onDecorateInputText(final ChatEvent event, final String message, final int textStartPosition) {
        decorateForegroundColor(event, message, textStartPosition);
        decoreateNext(event, message, textStartPosition);
        decorateHistoryLinks(event, message, textStartPosition);
        decorateGamesLinks(event, message, textStartPosition);
        decorateJournalLinks(event, message, textStartPosition);
        decorateBugWhoLinks(event, message, textStartPosition);
        decorateNewsLinks(event, message, textStartPosition);
        decorateGameNotifyLinks(event, message, textStartPosition);
        decorateQuotes(event, message, textStartPosition);
        decorateLinks(event, message, textStartPosition);
    }

    protected void onInputTextDoubleClick(MouseEvent e) {
        if (chatConsole.inputText.getSelectionText().equals("next")) {
            connector.sendMessage("next", true);
            return;
        }
        int caretPosition = 0;
        try {
            caretPosition = chatConsole.inputText.getOffsetAtLocation(new Point(e.x, e.y));
        } catch (IllegalArgumentException iae) {
            return;
        }
        int lineIndex = chatConsole.inputText.getContent().getLineAtOffset(caretPosition);
        String line = chatConsole.inputText.getContent().getLine(lineIndex).trim();
        boolean isHandled = false;
        int spaceIndex = line.indexOf(' ');
        if (spaceIndex != -1) {
            String firstWord = line.trim().substring(0, spaceIndex);
            if (firstWord.startsWith("%") && firstWord.endsWith(":")) {
                firstWord = firstWord.substring(1, firstWord.length() - 1);
                if (NumberUtils.isDigits(firstWord)) {
                    int linesBackCounter = 0;
                    lineIndex--;
                    while (lineIndex > 0 && linesBackCounter < 50) {
                        String newLine = chatConsole.inputText.getContent().getLine(lineIndex);
                        if (newLine.startsWith("Journal for ")) {
                            RaptorStringTokenizer tok = new RaptorStringTokenizer(newLine, " :", true);
                            tok.nextToken();
                            tok.nextToken();
                            String user = tok.nextToken();
                            if (user != null) {
                                isHandled = true;
                                if (isExaminingAGame()) {
                                    connector.sendMessage("unexamine", true);
                                }
                                connector.sendMessage("examine " + user + " %" + firstWord, true);
                            }
                            break;
                        }
                        lineIndex--;
                        linesBackCounter++;
                    }
                }
            } else if (firstWord.endsWith(":")) {
                firstWord = firstWord.substring(0, firstWord.length() - 1);
                if (NumberUtils.isDigits(firstWord)) {
                    int linesBackCounter = 0;
                    lineIndex--;
                    while (lineIndex > 0 && linesBackCounter < 12) {
                        String newLine = chatConsole.inputText.getContent().getLine(lineIndex);
                        if (newLine.startsWith("History for ")) {
                            RaptorStringTokenizer tok = new RaptorStringTokenizer(newLine, " :", true);
                            tok.nextToken();
                            tok.nextToken();
                            String user = tok.nextToken();
                            if (user != null) {
                                isHandled = true;
                                if (isExaminingAGame()) {
                                    connector.sendMessage("unexamine", true);
                                }
                                connector.sendMessage("examine " + user + " " + firstWord, true);
                            }
                            break;
                        }
                        lineIndex--;
                        linesBackCounter++;
                    }
                }
            } else if (NumberUtils.isDigits(firstWord) && (line.contains("W:") || line.contains("B:"))) {
                connector.sendMessage("observe " + firstWord, true);
            } else if (line.startsWith("Game notification:")) {
                RaptorStringTokenizer tok = new RaptorStringTokenizer(line, " ", true);
                String lastWord = null;
                while (tok.hasMoreTokens()) {
                    lastWord = tok.nextToken();
                }
                if (lastWord != null && NumberUtils.isDigits(lastWord)) {
                    connector.sendMessage("observe " + lastWord, true);
                }
            } else if (NumberUtils.isDigits(firstWord) && !line.contains("W:") && !line.contains("B:") && line.contains("(") && line.contains(")")) {
                connector.sendMessage("news " + firstWord, true);
            }
        }
        if (!isHandled) {
            String url = ChatUtils.getUrl(chatConsole.inputText, caretPosition);
            if (StringUtils.isNotBlank(url)) {
                BrowserUtils.openUrl(url);
                return;
            }
            String quotedText = ChatUtils.getQuotedText(chatConsole.inputText, caretPosition);
            if (StringUtils.isNotBlank(quotedText)) {
                url = ChatUtils.getUrl(quotedText);
                if (url == null) {
                    RaptorAliasResult alias = AliasService.getInstance().processAlias(this, quotedText);
                    if (alias == null) {
                        connector.sendMessage(quotedText);
                    } else if (alias.getNewText() != null) {
                        connector.sendMessage(alias.getNewText());
                    }
                    if (alias != null && alias.getUserMessage() != null) {
                        onAppendChatEventToInputText(new ChatEvent(null, ChatType.INTERNAL, alias.getUserMessage()));
                    }
                    onForceAutoScroll();
                } else {
                    BrowserUtils.openUrl(url);
                }
            }
        }
    }

    protected void onInputTextRightClick(MouseEvent e) {
        int caretPosition = 0;
        try {
            caretPosition = chatConsole.inputText.getOffsetAtLocation(new Point(e.x, e.y));
        } catch (IllegalArgumentException iae) {
            return;
        }
        String word = chatConsole.inputText.getSelectionText();
        boolean wasSelectedText = true;
        if (StringUtils.isBlank(word)) {
            word = ChatUtils.getWord(chatConsole.inputText, caretPosition);
            wasSelectedText = false;
        } else {
            word = connector.removeLineBreaks(word);
        }
        Menu menu = new Menu(chatConsole.getShell(), SWT.POP_UP);
        if (wasSelectedText) {
            MenuItem copyItem = new MenuItem(menu, SWT.PUSH);
            copyItem.setText(local.getString("chatConsCont22"));
            copyItem.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    chatConsole.inputText.copy();
                }
            });
            MenuItem copyPreserveItem = new MenuItem(menu, SWT.PUSH);
            copyPreserveItem.setText(local.getString("chatConsCont23"));
            copyPreserveItem.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    TextTransfer plainTextTransfer = TextTransfer.getInstance();
                    Object[] data = new Object[] { chatConsole.inputText.getSelectionText() };
                    Transfer[] types = new Transfer[] { plainTextTransfer };
                    Raptor.getInstance().getClipboard().setContents(data, types, DND.CLIPBOARD);
                }
            });
        }
        addCommandMenuItems(menu, word, caretPosition);
        addPersonMenuItems(menu, word);
        addChannelMenuItems(menu, word);
        addGameIdMenuItems(menu, word);
        if (menu.getItemCount() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Showing popup with " + menu.getItemCount() + " items." + chatConsole.inputText.toDisplay(e.x, e.y));
            }
            menu.setLocation(chatConsole.inputText.toDisplay(e.x, e.y));
            menu.setVisible(true);
            while (!menu.isDisposed() && menu.isVisible()) {
                if (!chatConsole.getDisplay().readAndDispatch()) {
                    chatConsole.getDisplay().sleep();
                }
            }
        }
        menu.dispose();
    }

    protected void onOutputTextRightClick(MouseEvent e) {
        int caretPosition = 0;
        try {
            caretPosition = chatConsole.getOutputText().getOffsetAtLocation(new Point(e.x, e.y));
        } catch (IllegalArgumentException iae) {
            return;
        }
        String word = chatConsole.getOutputText().getSelectionText();
        boolean wasSelectedText = true;
        if (StringUtils.isBlank(word)) {
            word = ChatUtils.getWord(chatConsole.getOutputText(), caretPosition);
            wasSelectedText = false;
        } else {
            word = connector.removeLineBreaks(word);
        }
        final String finalWord = word;
        Menu menu = new Menu(chatConsole.getShell(), SWT.POP_UP);
        if (wasSelectedText) {
            MenuItem copyItem = new MenuItem(menu, SWT.PUSH);
            copyItem.setText(local.getString("chatConsCont24"));
            copyItem.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    chatConsole.outputText.copy();
                }
            });
        }
        MenuItem pasteItem = new MenuItem(menu, SWT.PUSH);
        pasteItem.setText(local.getString("chatConsCont31"));
        pasteItem.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                chatConsole.outputText.paste();
            }
        });
        if (getPreferences().getBoolean(CHAT_COMMAND_LINE_SPELL_CHECK)) {
            new MenuItem(menu, SWT.SEPARATOR);
            MenuItem showWordsThatStartWithAction = new MenuItem(menu, SWT.PUSH);
            showWordsThatStartWithAction.setText(local.getString("chatConsCont25") + finalWord);
            showWordsThatStartWithAction.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    BrowserUtils.openUrl("http://www.google.com/search?q=define:+" + finalWord);
                }
            });
        }
        if (getPreferences().getBoolean(CHAT_COMMAND_LINE_SPELL_CHECK)) {
            new MenuItem(menu, SWT.SEPARATOR);
            MenuItem showWordsThatStartWithAction = new MenuItem(menu, SWT.PUSH);
            showWordsThatStartWithAction.setText(local.getString("chatConsCont26") + word + "'");
            showWordsThatStartWithAction.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    String[] words = DictionaryService.getInstance().getWordsThatStartWith(finalWord);
                    StringBuilder output = new StringBuilder(2000);
                    if (words == null || words.length == 0) {
                        output.append(local.getString("chatConsCont27"));
                    } else {
                        output.append(local.getString("chatConsCont28")).append(finalWord).append(":\n");
                        int count = 0;
                        for (int i = 0; i < words.length; i++) {
                            output.append(StringUtils.rightPad(words[i], 20));
                            count++;
                            if (count == 3) {
                                output.append("\n");
                                count = 0;
                            }
                        }
                    }
                    onAppendChatEventToInputText(new ChatEvent(null, ChatType.INTERNAL, output.toString()));
                }
            });
        }
        if (getPreferences().getBoolean(CHAT_COMMAND_LINE_SPELL_CHECK) && word != null && !isSpelledCorrectly(null, word)) {
            MenuItem addWord = new MenuItem(menu, SWT.PUSH);
            addWord.setText(local.getString("chatConsCont29") + word + local.getString("chatConsCont30"));
            addWord.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    DictionaryService.getInstance().addWord(finalWord);
                }
            });
        }
        if (menu.getItemCount() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Showing popup with " + menu.getItemCount() + " items." + chatConsole.inputText.toDisplay(e.x, e.y));
            }
            menu.setLocation(chatConsole.outputText.toDisplay(e.x, e.y));
            menu.setVisible(true);
            while (!menu.isDisposed() && menu.isVisible()) {
                if (!chatConsole.getDisplay().readAndDispatch()) {
                    chatConsole.getDisplay().sleep();
                }
            }
        }
        menu.dispose();
    }

    protected void playSounds(ChatEvent event) {
        if (!isSoundDisabled && !event.hasSoundBeenHandled()) {
            if (event.getType() == ChatType.TELL && getPreferences().getBoolean(PreferenceKeys.CHAT_IS_PLAYING_CHAT_ON_PERSON_TELL)) {
                SoundService.getInstance().playSound("chat");
            } else if (event.getType() == ChatType.PARTNER_TELL && getPreferences().getBoolean(PreferenceKeys.CHAT_IS_PLAYING_CHAT_ON_PTELL)) {
                SoundService.getInstance().playSound("chat");
            } else if (event.getType() == ChatType.CHALLENGE && getPreferences().getBoolean(PreferenceKeys.BOARD_PLAY_CHALLENGE_SOUND)) {
                SoundService.getInstance().playSound("challenge");
            } else if (event.getType() == ChatType.ABORT_REQUEST && getPreferences().getBoolean(PreferenceKeys.BOARD_PLAY_ABORT_REQUEST_SOUND)) {
                SoundService.getInstance().playSound("abortRequested");
            } else if (event.getType() == ChatType.DRAW_REQUEST && getPreferences().getBoolean(PreferenceKeys.BOARD_PLAY_CHALLENGE_SOUND)) {
                SoundService.getInstance().playSound("drawRequested");
            } else if (event.getType() == ChatType.NOTIFICATION_ARRIVAL && getPreferences().getBoolean(PreferenceKeys.CHAT_PLAY_NOTIFICATION_SOUND_ON_ARRIVALS)) {
                SoundService.getInstance().playSound("notificationArrival");
            } else if (event.getType() == ChatType.NOTIFICATION_DEPARTURE && getPreferences().getBoolean(PreferenceKeys.CHAT_PLAY_NOTIFICATION_SOUND_ON_DEPARTURES)) {
                SoundService.getInstance().playSound("notificationDeparture");
            }
        }
    }

    protected void reduceInputTextIfNeeded() {
        int charCount = chatConsole.inputText.getCharCount();
        if (charCount > Raptor.getInstance().getPreferences().getInt(CHAT_MAX_CONSOLE_CHARS)) {
            LOG.info("Cleaning chat console");
            long startTime = System.currentTimeMillis();
            int cleanTo = (int) (charCount * CLEAN_PERCENTAGE);
            int lineNumber = chatConsole.inputText.getContent().getLineAtOffset(cleanTo);
            cleanTo = chatConsole.inputText.getContent().getOffsetAtLine(lineNumber++);
            chatConsole.inputText.replaceTextRange(0, cleanTo, "");
            setCaretToOutputTextEnd();
            onForceAutoScroll();
            LOG.info("Cleaned console in " + (System.currentTimeMillis() - startTime));
        }
    }

    protected void registerForChatEvents() {
        if (this instanceof MainController) {
            connector.getChatService().addMainConsoleListener(chatServiceListener);
        } else {
            connector.getChatService().addChatServiceListener(chatServiceListener);
        }
    }

    protected void removeListenersTiedToChatConsole() {
        if (!chatConsole.isDisposed()) {
            chatConsole.outputText.removeListener(SWT.KeyDown, consoleOutputKeyDownListener);
            chatConsole.outputText.removeMouseListener(outputTextClickListener);
            chatConsole.inputText.removeListener(SWT.KeyUp, consoleInputKeyUpListener);
            chatConsole.inputText.removeListener(SWT.KeyDown, consoleInputKeyDownListener);
            if (OSUtils.isLikelyWindows()) {
                chatConsole.inputText.removeMouseWheelListener(chessBoardMouseWheelListener);
                chatConsole.outputText.removeMouseWheelListener(chessBoardMouseWheelListener);
            }
            if (chatConsole.inputText.getVerticalBar() != null) {
                chatConsole.inputText.getVerticalBar().removeListener(SWT.KeyUp, consoleInputKeyUpListener);
                chatConsole.inputText.getVerticalBar().removeListener(SWT.KeyDown, consoleInputKeyDownListener);
            }
            chatConsole.inputText.removeMouseListener(inputTextClickListener);
            chatConsole.inputText.getVerticalBar().removeSelectionListener(verticalScrollbarListener);
        }
    }

    protected void setCaretToOutputTextEnd() {
        if (!isIgnoringActions()) {
            chatConsole.getOutputText().setSelection(chatConsole.getOutputText().getCharCount());
        }
    }

    protected void smartScroll(boolean force) {
        ScrollBar scrollbar = chatConsole.inputText.getVerticalBar();
        if (scrollbar != null && scrollbar.isVisible() && getPreferences().getBoolean(PreferenceKeys.CHAT_IS_SMART_SCROLL_ENABLED)) {
            if (force) {
                setAutoScrolling(true);
            } else if (scrollbar.getMaximum() == scrollbar.getSelection() + scrollbar.getThumb()) {
                setAutoScrolling(true);
            } else {
                setAutoScrolling(false);
            }
        }
    }

    protected void smartScroll() {
        smartScroll(false);
    }

    protected void updateImageIcon(ChatEvent event) {
        if (!isActive && !hasUnseenText) {
            hasUnseenText = true;
            fireItemChanged();
        }
    }
}
