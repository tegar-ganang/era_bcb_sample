package mx.unam.fciencias.balpox.gui.main;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.DesertBluer;
import mx.unam.fciencias.balpox.conf.Configurator;
import mx.unam.fciencias.balpox.core.Estadistiquero;
import mx.unam.fciencias.balpox.core.SerialWrapper;
import mx.unam.fciencias.balpox.core.ThreadEvent;
import mx.unam.fciencias.balpox.core.ThreadEventManager;
import mx.unam.fciencias.balpox.core.ThreadListener;
import mx.unam.fciencias.balpox.expression.ExpresionTemporal;
import mx.unam.fciencias.balpox.gui.BuscarRemplazar;
import mx.unam.fciencias.balpox.gui.Configuracion;
import mx.unam.fciencias.balpox.gui.EstadisticasPanel;
import mx.unam.fciencias.balpox.gui.InformationDialog;
import mx.unam.fciencias.balpox.gui.TabType;
import mx.unam.fciencias.balpox.gui.core.CaretToCoordinates;
import mx.unam.fciencias.balpox.gui.core.ColourKeywordTextPane;
import mx.unam.fciencias.balpox.gui.core.FileAndTextTransferHandler;
import mx.unam.fciencias.balpox.gui.core.FileAndTextTransferable;
import mx.unam.fciencias.balpox.gui.core.InsertOverwriteTextPane;
import mx.unam.fciencias.balpox.gui.core.LineHighlightHandler;
import mx.unam.fciencias.balpox.gui.core.LineNumberPanel;
import mx.unam.fciencias.balpox.gui.core.NoWrapEditorKit;
import mx.unam.fciencias.balpox.gui.core.SplashScreenFrame;
import mx.unam.fciencias.balpox.gui.core.StyledInsertOverwriteTextPane;
import mx.unam.fciencias.balpox.gui.core.event.ReglaEvent;
import mx.unam.fciencias.balpox.gui.core.event.ReglaEventManager;
import mx.unam.fciencias.balpox.gui.core.event.ReglaListener;
import mx.unam.fciencias.balpox.gui.core.style.Styler;
import mx.unam.fciencias.balpox.gui.util.GUIUtil;
import mx.unam.fciencias.balpox.parser.LexerExpresionTemporal;
import mx.unam.fciencias.balpox.parser.Trimmer;
import mx.unam.fciencias.balpox.parser.core.exception.ScannerError;
import mx.unam.fciencias.balpox.ui.SimpleMain;
import mx.unam.fciencias.balpox.util.HistoryManager;
import mx.unam.fciencias.balpox.util.Opener;
import mx.unam.fciencias.balpox.util.PrintManager;
import mx.unam.fciencias.balpox.util.PropertyManager;
import mx.unam.fciencias.balpox.util.Saver;
import mx.unam.fciencias.balpox.util.Util;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * <br><br>Fecha: 19-Jun-2006&nbsp;&nbsp;&nbsp;&nbsp;Hora:17:30:52
 *
 * @author <a href="mailto:balpo@gmx.net?subject=mx.unam.fciencias.balpox.gui.Main">Rodrigo Poblanno Balp</a>
 */
public final class Main extends JFrame implements ActionListener, MouseListener, PropertyChangeListener, ChangeListener, ThreadListener, CaretListener, ReglaListener, Serializable, FileAndTextTransferable {

    /**
	 * Etiqueta para la línea en la barra de estado.
	 */
    private JLabel lineLabel;

    /**
	 * Etiqueta que contiene la línea actual en la barra de estado.
	 */
    private JLabel lineNumberLabel;

    /**
	 * Etiqueta para la columna actual en la barra de estado.
	 */
    private JLabel columnLabel;

    /**
	 * Etiqueta que contiene la columna actual en la barra de estado.
	 */
    private JLabel columnNumberLabel;

    /**
	 * Etiqueta de la barra de estado que contiene el indicador de si se está en modo overwrite o no.
	 */
    private JLabel insertOverwriteLabel;

    /**
	 * Barra de progreso contenida en la barra de estado.
	 */
    private JProgressBar progressBar;

    /**
	 * Contenedor de las viñetas.
	 */
    private JTabbedPane tabs;

    /**
	 * Panel que contiene todo.
	 */
    private JPanel contentPane;

    /**
	 * Panel de status (barra de estado).
	 */
    private JPanel statusPanel;

    /**
	 * Etiqueta para el estatus.
	 */
    private JLabel statusLabel;

    /**
	 * Barra de herramientas.
	 */
    private JToolBar toolBar;

    /**
	 * Botón de analizar en análisis rápido.
	 */
    private JButton analizarButton;

    /**
	 * Botón de borrado en análisis rápido.
	 */
    private JButton borrarButton;

    /**
	 * Panel de análisis rápido.
	 */
    private JPanel analisisPanel;

    /**
	 * Componente de texto del análisis rápido.
	 */
    private JTextPane rapidoTA;

    /**
	 * Divisor entre los componentes de texto y el componente de análisis rápido.
	 */
    private JSplitPane split;

    /**
	 * Barra de menú.
	 */
    private JMenuBar menuBar;

    /**
	 * Archivo original.
	 */
    private File originalFile;

    /**
	 * Panel para el buscador y reemplazador de textos.
	 */
    private BuscarRemplazar buscarRemplazarPanel;

    /**
	 * Se encarga de detectar los cambios realizados al documento original.
	 */
    private OriginalDocumentListener originalDocumentListener = new OriginalDocumentListener();

    /**
	 * Logger.
	 */
    private static transient Logger log = Logger.getLogger(Main.class);

    /**
	 * Indica si el archivo original se ha modificado hasta este punto en el tiempo.
	 */
    transient boolean originalModified = false;

    /**
	 * Escucha eventos en el panel de análisis rápido.
	 */
    private AnalisisRapidoListener arl = new AnalisisRapidoListener();

    /**
	 * Componente de texto que contiene el archivo original.
	 */
    private InsertOverwriteTextPane originalTextPane;

    /**
	 * Componente de texto que contiene el archivo marcado.
	 */
    private StyledInsertOverwriteTextPane aetTextPane;

    /**
	 * Componente de texto que aplica colores y estilos a la gramática.
	 */
    private ColourKeywordTextPane gramaticaTextPane;

    /**
	 * Maneja los eventos de <i>undo</i>.
	 */
    private UndoManager undo = new UndoManager();

    /**
	 * Determina la acción <i>undo</i>.
	 */
    private UndoAction undoAction = new UndoAction();

    /**
	 * Determina la acción <i>redo</i>.
	 */
    private RedoAction redoAction = new RedoAction();

    /**
	 * Maneja la acción de salida del programa.
	 */
    private SalirAction salirAction;

    /**
	 * Reconoce las expresiones temporales de un archivo.
	 */
    transient LexerExpresionTemporal lexer;

    /**
	 * Despliega las estadísticas de las expresiones temporales econtradas.
	 */
    private EstadisticasPanel estadisticasPanel;

    /**
	 * Diferencia cada tipo de tab según su contenido.
	 */
    TabType selectedTabType;

    /**
	 * Índice en los tabs del archivo original.
	 */
    int originalIndex = -1;

    /**
	 * Índice en los tabs del archivo con las expresiones temporales marcadas.
	 */
    int aetIndex = -1;

    /**
	 * Índice en los tabs del componente de texto que despliega la gramática.
	 */
    int grammarIndex = -1;

    /**
	 * Índice en los tabs que contiene el componente que despliega las estadísticas.
	 */
    int estadisticasIndex = -1;

    /**
	 * Indica si el componente de las estadísticas fue abierto.
	 */
    private boolean estadisticasOn;

    /**
	 * Determina si el componente que despliega la gramática fue abierto.
	 */
    private boolean grammarOn;

    /**
	 * Determina si el componente que despliega las expresiones temporales fue abierto.
	 */
    private boolean aetOn;

    /**
	 * Determina si el componente de texto que despliega el archivo original ya fue abierto.
	 */
    private boolean originalOn;

    /**
	 * Determina si el archivo con las expresiones temporales marcadas ya fue guardado.
	 */
    boolean aetSaved = false;

    /**
	 * Determina si el componente del análisis rápido se está mostrando o ya se mostró..
	 */
    private boolean analisisRapidoShown = false;

    /**
	 * Determina si la ventana principal ya se mostró.
	 */
    private boolean windowShown = false;

    /**
	 * Maneja la historia de archivos abiertos por el usuario.
	 */
    protected HistoryManager hm = new HistoryManager(PropertyManager.readProperty("history.dir").concat(PropertyManager.readProperty("recent.history.file")));

    /**
	 *Configura el logger.
	 */
    static {
        setUpLog();
    }

    /**
	 * <p>Ejecuta el programa</p>
	 * <p>Verifica si la aplicación ya está en ejecución; si se debe mostrar el <i>splash screen</i> y si hay agrumentos en la línea de comandos</p>
	 *
	 * @param args Los argumentos en la línea de comandos.
	 *
	 * @see mx.unam.fciencias.balpox.ui.SimpleMain
	 */
    public static void main(String[] args) {
        if (!GUIUtil.isAppLocked()) {
            GUIUtil.lockApp();
            String showSplash = System.getProperty(PropertyManager.ConstantProperties.SHOW_SPLASH);
            if (showSplash == null) {
                showSplash = "true";
            }
            if (!showSplash.equalsIgnoreCase("false")) {
                new SplashScreenFrame(PropertyManager.readProperty("splash.img"));
            }
            if (args.length > 0) {
                new Main(args);
            } else {
                new Main();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Ya hay una instancia de la aplicación corriendo");
        }
    }

    /**
	 * Constructor principal para la versión GUI.
	 */
    public Main() {
        setUp();
    }

    /**
	 * Metedo para cuando  el usuario desea utilizar el programa sólo
	 * desde la línea de comandos.<br>
	 * Se reciben dos conjuntos de parámetros:
	 * <ol>
	 * <li>Para este caso la salida será desplegada en la pantalla y <b>se guardará el archivo</b> .
	 * <ul>
	 * <li><code>-in=[archivo]</code> que representa el archivo que se va a leer</li>
	 * <li><code>-out=[archivo]</code> que representa el archivo que se va a escribir</li>
	 * <p/>
	 * </ul>
	 * </li>
	 * <li>Para este caso, la salida será desplegada en la pantalla y se guardará el archivo, pero la entrada será <b>desde el teclado</b>.
	 * <ul>
	 * <li><code>-input</code> que determina que el texto se tecleará en la terminal</li>
	 * <li><code>-out=[archivo]</code> que representa el archivo que se va a escribir</li>
	 * <p/>
	 * </ul>
	 * </li>
	 * </ol>
	 *
	 * @param args Los argumentos recibidos desde la línea de comandos.
	 */
    public Main(String[] args) {
        setUpLog();
        new SimpleMain(args);
    }

    /**
	 * Configura todo.
	 */
    private void setUp() {
        super.setTitle(PropertyManager.readProperty("main.title"));
        super.setIconImage(Toolkit.getDefaultToolkit().getImage(PropertyManager.readProperty("main.icon")));
        add(contentPane, BorderLayout.CENTER);
        configurator();
        setUpProperties();
        setUpTabs();
        setUpStatusBar();
        crearMenu();
        crearToolBar();
        crearStatusBar();
        setUpUndoManager();
        setUpListeners();
        setUpTransferHandler();
        setUpAnalisisRrapido();
        toggleComenzar();
        toggleGuardar();
        toggleGuardarComo();
        toggleEstadisticas();
        toggleBuscar();
        waitAndHideSplitPane();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        pack();
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setLocation(GUIUtil.centreComponent(this));
        setVisible(true);
    }

    /**
	 * Crea el menú.
	 *
	 * @see mx.unam.fciencias.balpox.util.PropertyManager.MenuAndToolBarCreator
	 */
    private void crearMenu() {
        log.debug("Creando menú");
        salirAction = new SalirAction(PropertyManager.readProperty("salir.menu"), new ImageIcon(PropertyManager.readProperty("salir.menu.img")), this);
        menuBar = PropertyManager.MenuAndToolBarCreator.createMenuBar(this, this, redoAction, undoAction, salirAction, hm);
        setJMenuBar(menuBar);
    }

    /**
	 * Crea la barra de herramientas.
	 *
	 * @see mx.unam.fciencias.balpox.util.PropertyManager.MenuAndToolBarCreator
	 */
    private void crearToolBar() {
        toolBar = PropertyManager.MenuAndToolBarCreator.createToolBar(this, this, true, true, true, redoAction, undoAction);
        getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    /**
	 * Crea la barra de estado.
	 */
    private void crearStatusBar() {
        lineLabel.setText(PropertyManager.readProperty("status.bar.line.display"));
        columnLabel.setText(PropertyManager.readProperty("status.bar.column.display"));
        getContentPane().add(statusPanel, BorderLayout.SOUTH);
    }

    /**
	 * Verifica la presencia de los archivos de configuración.
	 */
    private void configurator() {
        Configurator.doAll();
    }

    /**
	 * Configura el logger.
	 */
    private static void setUpLog() {
        try {
            File salida = new File(PropertyManager.readProperty("log.dir"), PropertyManager.readProperty("log.file"));
            System.setOut(new PrintStream(new FileOutputStream(salida, true)));
            System.setErr(new PrintStream(new FileOutputStream(salida, true)));
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            DOMConfigurator.configure(new File(PropertyManager.readProperty("log.config.dir"), PropertyManager.readProperty("log.config.file")).toString());
        }
    }

    /**
	 * Establece las propiedades usadas por el programa en el siguiente orden:
	 * <ol>
	 * <li>Locale = es_MX</li>
	 * <li>user.languaje = es</li>
	 * <li>user.country = mx</li>
	 * <li>user.home = <code>System.getProperty("user.dir")</code></li>
	 * <li>exte.dir = <code>System.getProperty("user.dir")</code></li>
	 * <li>ScrollBar.is3DEnabled = <code>Boolean.TRUE</code></li>
	 * </ol>
	 */
    private void setUpProperties() {
        setLocale(new Locale("es", "mx"));
        System.setProperty("user.language", "es");
        System.setProperty("user.country", "mx");
        System.setProperty("user.home", System.getProperty("user.dir"));
        System.setProperty("exte.dir", System.getProperty("user.dir"));
        try {
            UIManager.put("ScrollBar.is3DEnabled", Boolean.TRUE);
            PlasticLookAndFeel.setTabStyle(PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
            Options.setPopupDropShadowEnabled(true);
            LookUtils.setLookAndTheme(new PlasticXPLookAndFeel(), new DesertBluer());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
	 * Configura y establece los objetos encargados de escuchar eventos.
	 */
    private void setUpListeners() {
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                salirAction.actionPerformed(null);
            }

            public void windowOpened(WindowEvent e) {
                waitAndHideSplitPane();
            }
        });
        ThreadEventManager.addThreadEventListener(this);
        ReglaEventManager.addReglaEventListener(this);
        getContentPane().setFocusable(true);
        contentPane.setFocusable(true);
        getRootPane().registerKeyboardAction(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeTab(getSelectedTabType());
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK, true), JComponent.WHEN_IN_FOCUSED_WINDOW);
        undoAction.addPropertyChangeListener(this);
        insertOverwriteLabel.addMouseListener(new MouseInputAdapter() {

            public void mouseClicked(MouseEvent e) {
                TabType stt = getSelectedTabType();
                if (stt != null) {
                    switch(stt) {
                        case ORIGINAL:
                            originalTextPane.setTypingMode(originalTextPane.getTypingMode() == InsertOverwriteTextPane.TypingMode.INSERT ? InsertOverwriteTextPane.TypingMode.OVERWRITE : InsertOverwriteTextPane.TypingMode.INSERT);
                            break;
                        case AET:
                            aetTextPane.setTypingMode(aetTextPane.getTypingMode() == InsertOverwriteTextPane.TypingMode.INSERT ? InsertOverwriteTextPane.TypingMode.OVERWRITE : InsertOverwriteTextPane.TypingMode.INSERT);
                            break;
                    }
                }
            }
        });
    }

    /**
	 * Configura y establece el componente que transferirá archivos y texto.
	 */
    private void setUpTransferHandler() {
        getRootPane().setTransferHandler(new FileAndTextTransferHandler(this));
    }

    /**
	 * Configura el panel de análisis rápido.
	 */
    private void setUpAnalisisRrapido() {
        analizarButton.addActionListener(arl);
        borrarButton.addActionListener(arl);
        analizarButton.setActionCommand(PropertyManager.readProperty("analisisrapido.button.ok.name"));
        analizarButton.setName(analizarButton.getActionCommand());
        analizarButton.setText(PropertyManager.readProperty("analisisrapido.button.ok.display"));
        borrarButton.setActionCommand(PropertyManager.readProperty("analisisrapido.button.cancell.name"));
        borrarButton.setName(borrarButton.getActionCommand());
        borrarButton.setText(PropertyManager.readProperty("analisisrapido.button.cancell.display"));
        analisisPanel.setBorder(BorderFactory.createTitledBorder(PropertyManager.readProperty("analisisrapido.title")));
        rapidoTA.addFocusListener(new FocusAdapter() {

            public void focusGained(FocusEvent e) {
                rapidoTA.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), new Object());
            }
        });
    }

    /**
	 * Configura el manejador de las acciones <i>undo</i>.
	 */
    private void setUpUndoManager() {
        undoAction.setRedoAction(redoAction);
        undoAction.setUndo(undo);
        redoAction.setUndoAction(undoAction);
        redoAction.setUndo(undo);
    }

    /**
	 * Configura los tabs.
	 */
    private void setUpTabs() {
        split.getRightComponent().setMinimumSize(new Dimension(0, 0));
        split.getRightComponent().setMaximumSize(new Dimension(245, 610));
        split.getRightComponent().setPreferredSize(new Dimension(245, 610));
        split.setDividerLocation(760);
        split.setResizeWeight(1);
        tabs.addChangeListener(this);
        tabs.addMouseListener(new TabMouseListener(this));
    }

    /**
	 * Configura la barra de estado.
	 */
    private void setUpStatusBar() {
        setWriteMode(InsertOverwriteTextPane.TypingMode.INSERT);
    }

    /**
	 * Agrega un tab según su tipo.
	 *
	 * @param type El tipo de tab que se agregará.
	 */
    public void addTab(TabType type) {
        switch(type) {
            case ORIGINAL:
                addOriginalTab();
                toggleComenzar();
                toggleBuscar();
                break;
            case AET:
                addAETTab();
                toggleEstadisticas();
                toggleBuscar();
                break;
            case ESTADISTICAS:
                addEstadisticasTab();
                toggleBuscar();
                break;
            case GRAMATICA:
                addGramaticaTab();
                toggleBuscar();
                break;
            default:
                break;
        }
    }

    /**
	 * Elimina un tab según su tipo.
	 *
	 * @param type El tipo del tab a eliminar.
	 */
    public void removeTab(TabType type) {
        switch(type) {
            case ORIGINAL:
                originalOn = false;
                tabs.remove(originalIndex);
                originalIndex = -1;
                fixIndices();
                break;
            case AET:
                if (!aetSaved) {
                    int resp = JOptionPane.showConfirmDialog(getMe(), "<html>El archivo marcado no estí guardado<br>íDeseas guardarlo?", "íGuardar?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.question.img")));
                    if (resp == JOptionPane.NO_OPTION) {
                        aetOn = false;
                        tabs.remove(aetIndex);
                        aetIndex = -1;
                        aetSaved = false;
                    } else {
                        guardarArchivoAction();
                    }
                } else {
                    aetOn = false;
                    tabs.remove(aetIndex);
                    aetIndex = -1;
                    aetSaved = false;
                }
                fixIndices();
                break;
            case ESTADISTICAS:
                estadisticasOn = false;
                tabs.remove(estadisticasIndex);
                estadisticasIndex = -1;
                fixIndices();
                break;
            case GRAMATICA:
                grammarOn = false;
                tabs.remove(grammarIndex);
                grammarIndex = -1;
                fixIndices();
                break;
        }
    }

    /**
	 * Arregla el desplazamiento de los índices de los tabs.<br>
	 * Cuando un tab es removido, los índices deben actualizarse para reflejar
	 * cuíl es el que estí seleccionado. El movimiento de los índices de los tabs estí
	 * definido y alterado cada vez que se agrega un tab {@link #addTab(mx.unam.fciencias.balpox.gui.TabType)}
	 * o cada vez que se quita uno {@link #removeTab(mx.unam.fciencias.balpox.gui.TabType)}.
	 *
	 * @see #originalIndex
	 * @see #aetIndex
	 * @see #grammarIndex
	 * @see #estadisticasIndex
	 */
    private void fixIndices() {
        int cont = tabs.getTabCount();
        for (int i = 0; i < cont; i++) {
            String tit = tabs.getTitleAt(i);
            if (tit.equalsIgnoreCase(PropertyManager.readProperty("tab.original.name"))) {
                originalIndex = i;
            } else if (tit.equalsIgnoreCase(PropertyManager.readProperty("tab.aet.name"))) {
                aetIndex = i;
            } else if (tit.equalsIgnoreCase(PropertyManager.readProperty("tab.grammar.name"))) {
                grammarIndex = i;
            } else if (tit.equalsIgnoreCase(PropertyManager.readProperty("tab.estadisticas.name"))) {
                estadisticasIndex = i;
            }
        }
        setSelectedTabType(tabs.getSelectedIndex());
    }

    /**
	 * Agrega el tab que despliega el archivo original.
	 *
	 * @see mx.unam.fciencias.balpox.gui.TabType#ORIGINAL
	 */
    private void addOriginalTab() {
        log.debug("Agregando archivo original");
        if (!originalOn) {
            originalIndex = tabs.getTabCount();
        }
        originalTextPane = new InsertOverwriteTextPane();
        boolean wrap = Boolean.parseBoolean(PropertyManager.readProperty("configuration.trimmed.wrap"));
        if (!wrap) {
            originalTextPane.setEditorKit(new NoWrapEditorKit());
        }
        boolean highlight = Boolean.parseBoolean(PropertyManager.readProperty("configuration.trimmed.highlight"));
        if (highlight) {
            LineHighlightHandler lineHH = new LineHighlightHandler(Styler.loadColour("configuration.trimmed.highlight.colour"));
            lineHH.setTextComponent(originalTextPane);
            lineHH.addHighlight(0);
        }
        JScrollPane scrollPane = new JScrollPane(originalTextPane);
        boolean lineNumbers = Boolean.parseBoolean(PropertyManager.readProperty("configuration.trimmed.linenumber"));
        if (lineNumbers) {
            LineNumberPanel view = new LineNumberPanel(originalTextPane);
            view.setFontColour(Styler.loadColour("configuration.trimmed.linenumber.colour"));
            scrollPane.setRowHeaderView(view);
        }
        originalTextPane.addPropertyChangeListener(InsertOverwriteTextPane.TYPING_MODE_CHANGED_PROPERTY, this);
        originalTextPane.setText(originalTextPane.readFile(originalFile));
        originalTextPane.applyStyles();
        originalTextPane.getDocument().addDocumentListener(originalDocumentListener);
        originalTextPane.getDocument().addUndoableEditListener(new UndoableDocumentLister());
        originalTextPane.addCaretListener(this);
        buscarRemplazarPanel = new BuscarRemplazar(originalTextPane);
        if (!originalOn) {
            tabs.add(scrollPane, originalIndex);
            tabs.setTitleAt(originalIndex, PropertyManager.readProperty("tab.original.name"));
        } else {
            tabs.setComponentAt(originalIndex, scrollPane);
        }
        Timer tim = new Timer();
        tim.schedule(new TimerTask() {

            public void run() {
                ((JScrollPane) tabs.getComponentAt(originalIndex)).getViewport().getView().requestFocusInWindow();
                try {
                    originalTextPane.setCaretPosition(1);
                } catch (Exception e) {
                }
                originalTextPane.setCaretPosition(0);
            }
        }, new Date(System.currentTimeMillis() + 1500));
        originalOn = true;
    }

    /**
	 * Agrega el tab que contiene el componente que despliega el archivo marcado.
	 *
	 * @see mx.unam.fciencias.balpox.gui.TabType#AET
	 */
    private void addAETTab() {
        log.debug("Agregando archivo aet");
        if (!aetOn) {
            aetIndex = tabs.getTabCount();
        }
        aetTextPane = new StyledInsertOverwriteTextPane(new DefaultStyledDocument());
        boolean wrap = Boolean.parseBoolean(PropertyManager.readProperty("configuration.aet.wrap"));
        if (!wrap) {
            aetTextPane.setEditorKit(new NoWrapEditorKit());
        }
        aetTextPane.addCaretListener(this);
        final boolean highlight = Boolean.parseBoolean(PropertyManager.readProperty("configuration.aet.highlight"));
        final LineHighlightHandler lineHH = new LineHighlightHandler(Styler.loadColour("configuration.aet.highlight.colour"));
        if (highlight) {
            lineHH.setTextComponent(aetTextPane);
        }
        JScrollPane scrollPane = new JScrollPane(aetTextPane);
        boolean lineNumbers = Boolean.parseBoolean(PropertyManager.readProperty("configuration.aet.linenumber"));
        if (lineNumbers) {
            LineNumberPanel view = new LineNumberPanel(aetTextPane);
            view.setFontColour(Styler.loadColour("configuration.aet.linenumber.colour"));
            scrollPane.setRowHeaderView(view);
        }
        aetTextPane.addPropertyChangeListener(InsertOverwriteTextPane.TYPING_MODE_CHANGED_PROPERTY, this);
        aetTextPane.setText(aetTextPane.readFile(originalFile));
        aetTextPane.setExps(lexer.getExpresionesTemporales());
        aetTextPane.applyStyles();
        if (!aetOn) {
            tabs.add(scrollPane, aetIndex);
            tabs.setTitleAt(aetIndex, PropertyManager.readProperty("tab.aet.name"));
        } else {
            tabs.setComponentAt(aetIndex, scrollPane);
        }
        aetTextPane.setEditable(false);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tabs.setSelectedIndex(aetIndex);
                if (highlight) {
                    lineHH.addHighlight(0);
                }
                aetTextPane.repaint();
                guardarArchivoAction();
            }
        });
        aetOn = true;
    }

    /**
	 * Agrega el tab de tipo {@link mx.unam.fciencias.balpox.gui.TabType#AET} con las expresiones temporales marcadas.
	 *
	 * @param exps Las expresiones temporales que {@link mx.unam.fciencias.balpox.parser.LexerExpresionTemporal} marcó.
	 * @param text El texto no marcado.
	 */
    private void addAETTab(Vector<ExpresionTemporal> exps, String text) {
        log.debug("Agregando archivo aet");
        if (!aetOn) {
            aetIndex = tabs.getTabCount();
        }
        aetTextPane = new StyledInsertOverwriteTextPane(new DefaultStyledDocument());
        buscarRemplazarPanel = new BuscarRemplazar(aetTextPane);
        boolean wrap = Boolean.parseBoolean(PropertyManager.readProperty("configuration.aet.wrap"));
        if (!wrap) {
            aetTextPane.setEditorKit(new NoWrapEditorKit());
        }
        aetTextPane.addCaretListener(this);
        final boolean highlight = Boolean.parseBoolean(PropertyManager.readProperty("configuration.aet.highlight"));
        final LineHighlightHandler lineHH = new LineHighlightHandler(Styler.loadColour("configuration.aet.highlight.colour"));
        if (highlight) {
            lineHH.setTextComponent(aetTextPane);
        }
        JScrollPane scrollPane = new JScrollPane(aetTextPane);
        boolean lineNumbers = Boolean.parseBoolean(PropertyManager.readProperty("configuration.aet.linenumber"));
        if (lineNumbers) {
            LineNumberPanel lineNumberPanel = new LineNumberPanel(aetTextPane);
            lineNumberPanel.setFontColour(Styler.loadColour("configuration.aet.linenumber.colour"));
            scrollPane.setRowHeaderView(lineNumberPanel);
        }
        aetTextPane.addPropertyChangeListener(InsertOverwriteTextPane.TYPING_MODE_CHANGED_PROPERTY, this);
        aetTextPane.setText(text);
        aetTextPane.setExps(exps);
        aetTextPane.applyStyles();
        if (!aetOn) {
            tabs.add(scrollPane, aetIndex);
            tabs.setTitleAt(aetIndex, PropertyManager.readProperty("tab.aet.name"));
        } else {
            tabs.setComponentAt(aetIndex, scrollPane);
        }
        aetTextPane.setEditable(false);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tabs.setSelectedIndex(aetIndex);
                selectedTabType = TabType.AET;
                if (highlight) {
                    lineHH.addHighlight(0);
                }
                aetTextPane.setCaretPosition(0);
                aetTextPane.repaint();
            }
        });
        aetOn = true;
        aetSaved = false;
    }

    /**
	 * Agrega el panel de estadísticas.
	 * <p/>
	 * El agregado de este panel depende de {@link #estadisticasOn}, ya que si su valor es <code>true</code>,
	 * esto quiere decir que ya existe este panel.
	 * Las expresiones temporales originadas por {@link #lexer} son cargadas cuando íste termina su ejecuciín y dicho
	 * evento es manipulado por {@link #threadStopped(mx.unam.fciencias.balpox.core.ThreadEvent)}.
	 *
	 * @see EstadisticasPanel
	 * @see Estadistiquero
	 */
    private void addEstadisticasTab() {
        if (!estadisticasOn) {
            estadisticasIndex = tabs.getTabCount();
        }
        estadisticasPanel = new EstadisticasPanel();
        if (!estadisticasOn) {
            tabs.add(estadisticasPanel, estadisticasIndex);
            tabs.setTitleAt(estadisticasIndex, PropertyManager.readProperty("tab.estadisticas.name"));
        } else {
            tabs.setComponentAt(estadisticasIndex, estadisticasPanel);
        }
        estadisticasOn = true;
    }

    /**
	 * Agrega el tab que contiene el componente de texto que despliega la gramática.
	 */
    private void addGramaticaTab() {
        if (!grammarOn) {
            grammarIndex = tabs.getTabCount();
        }
        gramaticaTextPane = new ColourKeywordTextPane();
        gramaticaTextPane.setEditable(false);
        gramaticaTextPane.setText(gramaticaTextPane.readFile(new File(PropertyManager.readProperty("grammar.file"))));
        JScrollPane scroll = new JScrollPane(gramaticaTextPane);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        LineNumberPanel view = new LineNumberPanel(gramaticaTextPane);
        view.setFontColour(new Color(128, 0, 0));
        scroll.setRowHeaderView(view);
        buscarRemplazarPanel = new BuscarRemplazar(gramaticaTextPane);
        gramaticaTextPane.addCaretListener(this);
        if (!grammarOn) {
            tabs.add(scroll, grammarIndex);
            tabs.setTitleAt(grammarIndex, PropertyManager.readProperty("tab.grammar.name"));
        } else {
            tabs.setComponentAt(grammarIndex, scroll);
        }
        scroll.updateUI();
        grammarOn = true;
    }

    /**
	 * Alterna el estado del componente <i>comenzar</i> en la barra de harramientas y en la barra de menú.
	 */
    private void toggleComenzar() {
        toolBar.getComponent(9).setEnabled(originalFile != null);
        ((JMenu) menuBar.getComponent(3)).getItem(0).setEnabled(originalFile != null);
    }

    /**
	 * Activa/Desactiva el botín de guardado.
	 * <p/>
	 * El botín de guardado estarí activo cuando el archivo original estí modificado y cuando {@link #selectedTabType} sea de tipo {@link TabType#ORIGINAL}.
	 */
    private void toggleGuardar() {
        toolBar.getComponent(1).setEnabled(originalModified || !aetSaved);
        ((JMenu) menuBar.getComponent(0)).getItem(4).setEnabled(originalModified || !aetSaved);
    }

    /**
	 * Alterna el estado del componente <i>guardar como</i> en la barra de harramientas y en la barra de menú.
	 */
    private void toggleGuardarComo() {
        toolBar.getComponent(1).setEnabled(originalFile != null || !aetSaved);
        ((JMenu) menuBar.getComponent(0)).getItem(5).setEnabled(originalFile != null || !aetSaved);
    }

    /**
	 * Alterna el estado del componente <i>estadísticas</i> en la barra de harramientas y en la barra de menú.
	 */
    private void toggleEstadisticas() {
        toolBar.getComponent(10).setEnabled(aetOn);
        ((JMenu) menuBar.getComponent(3)).getItem(1).setEnabled(aetOn);
    }

    /**
	 * Alterna el estado del componente <i>buscar</i> en la barra de harramientas y en la barra de menú.
	 */
    private void toggleBuscar() {
        boolean on = (originalOn || aetOn || grammarOn) && getSelectedTabType() != TabType.ESTADISTICAS;
        toolBar.getComponent(7).setEnabled(on);
        toolBar.getComponent(8).setEnabled(on);
        ((JMenu) menuBar.getComponent(1)).getItem(0).setEnabled(on);
        ((JMenu) menuBar.getComponent(1)).getItem(1).setEnabled(on);
        ((JMenu) menuBar.getComponent(1)).getItem(2).setEnabled(on);
    }

    /**
	 * Después de <i>3</i> segundos, oculta el panel de análisis rápido si el usuario no ha posicionado el mouse sobre panel.
	 */
    private void waitAndHideSplitPane() {
        if (!windowShown) {
            int time = 3000;
            Date timeToRun = new Date(System.currentTimeMillis() + time);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                public void run() {
                    Point mouse = getMousePosition(true);
                    mouse = new Point((int) mouse.getX() - 760, (int) mouse.getY());
                    if (!rapidoTA.contains(mouse)) {
                        analisisRapidoShown = false;
                        split.setDividerLocation(split.getMaximumDividerLocation());
                    }
                }
            }, timeToRun);
            windowShown = true;
        }
    }

    /**
	 * Escribe texto a la etiqueta del estado.
	 *
	 * @param status El texto a desplegar.
	 */
    private void writeStatusBar(String status) {
        statusLabel.setText(status);
    }

    /**
	 * Alterna el modo de escritura entre <i>insert</i> y <i>overwrite</i> en el componente de texto actual.
	 *
	 * @param mode El tipo de escritura.
	 */
    private void setWriteMode(InsertOverwriteTextPane.TypingMode mode) {
        if (mode == InsertOverwriteTextPane.TypingMode.INSERT) {
            insertOverwriteLabel.setText(PropertyManager.readProperty("insert.label.ins"));
        } else {
            insertOverwriteLabel.setText(PropertyManager.readProperty("insert.label.ovr"));
        }
    }

    /**
	 * Establece las coordenadas del cursor en el componente de texto.
	 *
	 * @param line La línea actual.
	 * @param col  La columna actual.
	 *
	 * @see mx.unam.fciencias.balpox.gui.main.Main#columnNumberLabel
	 * @see mx.unam.fciencias.balpox.gui.main.Main#lineNumberLabel
	 */
    private void setCursorCoordinates(int line, int col) {
        lineNumberLabel.setText(String.valueOf(line));
        columnNumberLabel.setText(String.valueOf(col));
    }

    /**
	 * Hace que la barra de progreso comience.
	 */
    private void startProgress() {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressBar.setIndeterminate(true);
    }

    /**
	 * Hace que la barra de progreso termine.
	 */
    private void stopProgress() {
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        progressBar.setIndeterminate(false);
    }

    /**
	 * Termina la aplicación.
	 */
    public void exit() {
        System.exit(0);
    }

    /**
	 * Obtiene los tabs.
	 *
	 * @return El componente que despleiga los tabs.
	 */
    public JTabbedPane tabs() {
        return tabs;
    }

    /**
	 * Determina si el archivo original fue modificado.
	 *
	 * @return <code>true</code> si {@link #originalModified} es <code>true</code>.
	 */
    protected boolean isOriginalModified() {
        return originalModified;
    }

    /**
	 * Acción tomada al abrir un archivo.
	 */
    private void abrirArchivoAction() {
        JFileChooser jfc = new JFileChooser(System.getProperty("exte.dir"));
        jfc.setMultiSelectionEnabled(false);
        FileFilter filter = new FileFilter() {

            public boolean accept(File f) {
                String filename = f.getAbsolutePath();
                int punto;
                punto = filename.indexOf('.');
                String extension = "";
                try {
                    extension = filename.substring(punto).toLowerCase();
                } catch (StringIndexOutOfBoundsException sioobe) {
                    punto = -1;
                }
                return punto == -1 || extension.equals(Saver.SaveExtension.HTM.extension()) || extension.equals(Saver.SaveExtension.HTML.extension()) || extension.equals(Saver.SaveExtension.SHTML.extension()) || extension.equals(Saver.SaveExtension.TEXT.extension()) || extension.equals(Saver.SaveExtension.TXT.extension()) || extension.equals(Saver.SaveExtension.TRIMMED.extension()) || extension.equals(Saver.SaveExtension.XHTML.extension());
            }

            public String getDescription() {
                return "Arhivos de texto (*.txt, *.html, *.htm, *.shtml, *.xhtml, *.trimmed)";
            }
        };
        jfc.addChoosableFileFilter(filter);
        int op = jfc.showOpenDialog(this);
        if (op == JFileChooser.APPROVE_OPTION && filter.accept(jfc.getSelectedFile())) {
            abrirORIGINAL(jfc.getSelectedFile());
        }
    }

    /**
	 * Abre un archivo que esté listado como <i>reciente</i>.
	 *
	 * @param what El archivo a abrir.
	 */
    @SuppressWarnings("unchecked")
    private void openRecentAction(String what) {
        what = what.replaceAll(PropertyManager.readProperty("recientes.submenu.open.name"), "");
        if (what.indexOf("aet") == -1) {
            abrirORIGINAL(new File(what));
        } else {
            SerialWrapper sw = Opener.openWrapped(what);
            if (sw == null) {
                JOptionPane.showMessageDialog(this, PropertyManager.readProperty("dialog.archivonoencontrado.text").concat(what), PropertyManager.readProperty("dialog.archivonoencontrado.title"), JOptionPane.WARNING_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.warning.img")));
            } else {
                addAETTab(Vector.class.cast(sw.getComponentByClass(Vector.class)), String.class.cast(sw.getComponentByClass(String.class)));
            }
        }
    }

    /**
	 * Abre un archivo como <i>archivo original</i>.
	 *
	 * @param archivoIn El archivo a abrir.
	 */
    private void abrirORIGINAL(File archivoIn) {
        JOptionPane.showMessageDialog(this, PropertyManager.readProperty("dialog.convertir.text"), PropertyManager.readProperty("dialog.convertir.title"), JOptionPane.INFORMATION_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.info.img")));
        Trimmer trim;
        try {
            File archivoOut = new File(PropertyManager.readProperty("output.dir"), archivoIn.getName());
            trim = new Trimmer(archivoIn.getAbsolutePath(), archivoOut.getAbsolutePath());
            originalFile = trim.getOutputFile();
            trim.start();
            addToHistory(archivoIn);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        }
    }

    /**
	 * Guarda el archivo (actual).
	 */
    public void guardarArchivoAction() {
        log.debug("El archivo que se debe guardar es el que esta seleccionado: " + selectedTabType);
        try {
            switch(selectedTabType) {
                case ORIGINAL:
                    Saver.save(originalFile, originalTextPane.getDocument().getText(0, originalTextPane.getDocument().getLength()));
                    tabs.setIconAt(originalIndex, null);
                    originalModified = false;
                    toggleGuardar();
                    addToHistory(originalFile);
                    break;
                case AET:
                    String nom;
                    File file;
                    if (originalFile == null) {
                        nom = preguntarNombreAET();
                        if (nom == null) {
                            return;
                        }
                        file = new File(nom);
                    } else {
                        nom = originalFile.getName();
                        nom = nom.substring(0, nom.indexOf('.')).concat(PropertyManager.readProperty("file.aet"));
                        file = new File(PropertyManager.readProperty("output.dir"), nom);
                    }
                    SerialWrapper sw = new SerialWrapper();
                    sw.addComponent(lexer.getExpresionesTemporales());
                    sw.addComponent(aetTextPane.getText());
                    Saver.saveSerialize(file, sw);
                    aetSaved = true;
                    addToHistory(file);
                    break;
                case ESTADISTICAS:
                    break;
            }
        } catch (BadLocationException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    /**
	 * Agrega un archivo abierto recientemente a la historia de archivos recientes.
	 *
	 * @param file El archivo a agregar a la historia.
	 */
    private void addToHistory(File file) {
        hm.writeHistoryEvent(new HistoryManager.HistoryEvent(file.getAbsolutePath()));
        hm.actualizar();
        JMenuItem item = ((JMenu) menuBar.getComponent(0)).getItem(2);
        item.removeAll();
        PropertyManager.MenuAndToolBarCreator.updateArchivosRecientes(item, hm, this, this);
    }

    /**
	 * Si se va a terminar la aplicación y el archivo ha sido modificado, pregunta al usuario el nombre
	 * por el que lo desea guardar.
	 *
	 * @return El nombre del archivo al que se guardará.
	 */
    private String preguntarNombreAET() {
        JFileChooser jfc = new JFileChooser(PropertyManager.readProperty("output.dir"));
        jfc.setMultiSelectionEnabled(false);
        jfc.addChoosableFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return !f.isDirectory();
            }

            public String getDescription() {
                return "Arhivos de texto (*.aet)";
            }
        });
        int op = jfc.showSaveDialog(this);
        File copia;
        if (op == JFileChooser.APPROVE_OPTION) {
            copia = jfc.getSelectedFile();
            if (copia.exists()) {
                int resp = JOptionPane.showConfirmDialog(this, PropertyManager.readProperty("dialog.sobreescribir.text"), PropertyManager.readProperty("dialog.sobreescribir.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.question.img")));
                if (resp != JOptionPane.YES_OPTION) {
                    return null;
                }
            }
            String nombre = copia.getAbsolutePath();
            if (!nombre.endsWith(".aet")) {
                return nombre.concat(".aet");
            } else {
                return nombre;
            }
        } else {
            return null;
        }
    }

    /**
	 * Crea el diálogo para <i>guardar como</i>
	 */
    private void guardarComoArchivoAction() {
        log.debug("El archivo que se debe guardar como es el que esta seleccionado: " + (tabs.getSelectedIndex() + 1));
        JFileChooser jfc = new JFileChooser(PropertyManager.readProperty("output.dir"));
        jfc.setMultiSelectionEnabled(false);
        jfc.addChoosableFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return !f.isDirectory();
            }

            public String getDescription() {
                return "Arhivos de texto (*.txt, *.html, *.htm, *.aet)";
            }
        });
        int op = jfc.showSaveDialog(this);
        File copia;
        if (op == JFileChooser.APPROVE_OPTION) {
            copia = jfc.getSelectedFile();
            if (copia.exists()) {
                int resp = JOptionPane.showConfirmDialog(this, PropertyManager.readProperty("dialog.sobreescribir.text"), PropertyManager.readProperty("dialog.sobreescribir.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.question.img")));
                if (resp != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            int sel = tabs.getSelectedIndex();
            if (sel == originalIndex) {
                try {
                    boolean ok = Saver.saveAs(copia, Saver.SaveExtension.AET, originalTextPane.getDocument().getText(0, originalTextPane.getDocument().getLength()));
                    if (!ok) {
                        JOptionPane.showMessageDialog(this, PropertyManager.readProperty("dialog.guardarcomo.error"), PropertyManager.readProperty("dialog.guardarcomo.error.title"), JOptionPane.WARNING_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.warning.img")));
                    }
                    undo.discardAllEdits();
                    undo.end();
                    undoAction.setEnabled(false);
                    tabs.setIconAt(originalIndex, null);
                    originalModified = false;
                    toggleGuardar();
                } catch (BadLocationException e) {
                    log.error(e.getMessage());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
	 * Acción tomada al presionar el botón <i>configuración</i>.
	 */
    private void configuracionAction() {
        Configuracion conf = new Configuracion(getMe());
        conf.setLocation(GUIUtil.centreComponent(conf));
        conf.setVisible(true);
    }

    /**
	 * Alterna el estado del componente <i>análisis rápido</i> en la barra de harramientas y en la barra de menú.
	 */
    private void toggleAnalisisRapidoAction() {
        if (!analisisRapidoShown) {
            split.setDividerLocation(760);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    rapidoTA.requestFocusInWindow();
                }
            });
        } else {
            split.setDividerLocation(split.getMaximumDividerLocation());
        }
        analisisRapidoShown = !analisisRapidoShown;
    }

    /**
	 * Alterna el estado del componente <i>borrar lista</i> en el menú principal.
	 */
    private void toggleBorrarLista() {
        ((JMenu) menuBar.getComponent(0)).getItem(2).setEnabled(!((JMenu) menuBar.getComponent(0)).getItem(2).isEnabled());
    }

    /**
	 * Cambia de tab.
	 *
	 * @param cual El tab que se desplegará.
	 */
    private void switchTextTabs(int cual) {
        switch(cual) {
            case 1:
                if (originalOn) {
                    tabs.setSelectedIndex(originalIndex);
                }
                break;
            case 2:
                if (aetOn) {
                    tabs.setSelectedIndex(aetIndex);
                }
                break;
        }
    }

    /**
	 * Acción tomada al presionar el botón <i>comenzar</i>.
	 */
    private void comenzarAction() {
        try {
            lexer = new LexerExpresionTemporal(new FileReader(originalFile.getAbsolutePath()));
            lexer.setName(LexerExpresionTemporal.name);
            lexer.setArchivo(originalFile.getAbsoluteFile());
            lexer.start();
        } catch (ScannerError scannerError) {
            log.error(scannerError.getMessage());
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
	 * Acción tomada al presionar el botón <i>estadísticas</i>.
	 */
    private void estadisticasAction() {
        addTab(TabType.ESTADISTICAS);
        tabs.setSelectedIndex(estadisticasIndex);
    }

    /**
	 * Acción tomada al presionar el botón <i>gramática</i>.
	 */
    private void grammarAction() {
        addTab(TabType.GRAMATICA);
        tabs.setSelectedIndex(grammarIndex);
    }

    /**
	 * Acción tomada al presionar el botón <i>buscar</i>.
	 */
    private void buscarAction() {
        int sel = tabs.getSelectedIndex();
        if (sel == originalIndex) {
            buscarRemplazarPanel = new BuscarRemplazar(originalTextPane);
            BuscarRemplazar.showDialog(buscarRemplazarPanel, true, this);
        } else if (sel == aetIndex) {
            buscarRemplazarPanel = new BuscarRemplazar(aetTextPane);
            BuscarRemplazar.showDialog(buscarRemplazarPanel, false, this);
        } else if (sel == grammarIndex) {
            buscarRemplazarPanel = new BuscarRemplazar(gramaticaTextPane);
            BuscarRemplazar.showDialog(buscarRemplazarPanel, false, this);
        }
    }

    /**
	 * Acción tomada al presionar el botón <i>buscar siguiente</i>.
	 */
    private void buscarSiguienteAction() {
        if (!buscarRemplazarPanel.buscar()) {
            JOptionPane.showMessageDialog(buscarRemplazarPanel, PropertyManager.readProperty("buscar.dialog.notfound.msg"), PropertyManager.readProperty("buscar.dialog.notfound.msg"), JOptionPane.WARNING_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.warning.img")));
        }
    }

    /**
	 * Acción tomada al presionar el botón <i>remplazar</i>.
	 */
    private void remplazarAction() {
        int sel = tabs.getSelectedIndex();
        if (sel == originalIndex) {
            BuscarRemplazar.showDialog(buscarRemplazarPanel = new BuscarRemplazar(originalTextPane), true, this);
        } else if (sel == aetIndex) {
            BuscarRemplazar.showDialog(buscarRemplazarPanel = new BuscarRemplazar(aetTextPane), false, this);
        } else if (sel == grammarIndex) {
            BuscarRemplazar.showDialog(buscarRemplazarPanel = new BuscarRemplazar(gramaticaTextPane), false, this);
        }
    }

    /**
	 * Acción tomada al presionar el botón <i>imprimir</i>.
	 */
    private void imprimirAction() {
        print();
    }

    /**
	 * Acción tomada al presionar el botón <i>ayuda</i>.
	 */
    private void ayudaAction() {
        int result = JOptionPane.showConfirmDialog(getMe(), PropertyManager.readProperty("dialog.vermanual.text"), PropertyManager.readProperty("dialog.vermanual.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.question.img")));
        if (result == JOptionPane.YES_OPTION) {
            File man = new File(PropertyManager.readProperty("manual.path"));
            Util.displayURL(man.getAbsolutePath());
        }
    }

    /**
	 * Acción tomada al presionar el botón <i>información</i>.
	 */
    private void infoAction() {
        InformationDialog.showDialog();
    }

    /**
	 * Acción tomada al presionar el botón <i>borrar lista</i>.
	 */
    private void borrarListaAction() {
        JMenuItem recientes = ((JMenu) menuBar.getComponent(0)).getItem(2);
        MenuElement[] subElements = recientes.getSubElements();
        for (MenuElement subElement : subElements) {
            MenuElement[] subElements2 = subElement.getSubElements();
            int cuantos = subElements2.length;
            if (cuantos >= 2) {
                for (MenuElement menuElement : subElements2) {
                    recientes.remove(0);
                }
                hm.borrar();
                hm.actualizar();
            }
            toggleBorrarLista();
        }
    }

    /**
	 * Acción tomada al presionar el botón <i>análisis rápido</i>.
	 */
    private void analisisRapidoAction() {
        String rap = rapidoTA.getText();
        if (rap == null || rap.length() == 0) {
            return;
        }
        rap = rap.replaceAll("\\s+", " ");
        try {
            lexer = new LexerExpresionTemporal(new StringReader(rap));
        } catch (ScannerError scannerError) {
            log.error(scannerError.getMessage());
        }
        lexer.setName(LexerExpresionTemporal.nameSimple);
        lexer.start();
    }

    /**
	 * Acción tomada al presionar el botón <i>comenzar</i>.
	 */
    private void analisisRapidoActionContinue() {
        Vector<ExpresionTemporal> exps = lexer.getExpresionesTemporales();
        addAETTab(exps, rapidoTA.getText());
        aetSaved = false;
    }

    /**
	 * Obtiene la referencia al componente general de este programa.
	 *
	 * @return Este componente.
	 */
    private Main getMe() {
        return this;
    }

    /**
	 * Obtiene el tipo de tab seleccionado.
	 *
	 * @return El tipo de tab seleccionado.
	 */
    public TabType getSelectedTabType() {
        return selectedTabType;
    }

    /**
	 * Imprime el componente de texto seleccionado.
	 */
    public void print() {
        TabType sel = getSelectedTabType();
        PrintManager pm;
        switch(sel) {
            case AET:
                pm = new PrintManager(aetTextPane, lexer.getArchivo().getAbsolutePath(), "", PrintManager.PrintOrientation.VERTICAL);
                pm.run();
                break;
            case ESTADISTICAS:
                pm = new PrintManager(estadisticasPanel.getEtByRegla(), "", "", PrintManager.PrintOrientation.VERTICAL);
                pm.run();
                pm = new PrintManager(estadisticasPanel.getEtByTipo(), "", "", PrintManager.PrintOrientation.VERTICAL);
                pm.run();
                break;
            case GRAMATICA:
                pm = new PrintManager(gramaticaTextPane, "Gramítica", "", PrintManager.PrintOrientation.VERTICAL);
                pm.run();
                break;
            case ORIGINAL:
                pm = new PrintManager(originalTextPane, originalFile.getAbsolutePath(), "", PrintManager.PrintOrientation.VERTICAL);
                pm.run();
                break;
        }
    }

    /**
	 * Invoked when an action occurs.
	 */
    public void actionPerformed(ActionEvent e) {
        String ac = e.getActionCommand();
        if (ac == null || ac.length() == 0) {
            return;
        }
        if (PropertyManager.readProperty("abrir.menu.name").equalsIgnoreCase(ac)) {
            abrirArchivoAction();
        } else if (PropertyManager.readProperty("guardar.menu.name").equalsIgnoreCase(ac)) {
            guardarArchivoAction();
        } else if (PropertyManager.readProperty("guardarcomo.menu.name").equalsIgnoreCase(ac)) {
            guardarComoArchivoAction();
        } else if (PropertyManager.readProperty("imprimir.menu.name").equalsIgnoreCase(ac)) {
            imprimirAction();
        } else if (PropertyManager.readProperty("buscar.menu.name").equalsIgnoreCase(ac)) {
            buscarAction();
        } else if (PropertyManager.readProperty("buscarsiguiente.menu.name").equalsIgnoreCase(ac)) {
            buscarSiguienteAction();
        } else if (PropertyManager.readProperty("remplazar.menu.name").equalsIgnoreCase(ac)) {
            remplazarAction();
        } else if (PropertyManager.readProperty("original.menu.name").equalsIgnoreCase(ac)) {
            log.debug("ORIGINAL");
            switchTextTabs(1);
        } else if (PropertyManager.readProperty("estilizado.menu.name").equalsIgnoreCase(ac)) {
            log.debug("ESTILIZADO");
            switchTextTabs(2);
        } else if (PropertyManager.readProperty("comenzar.menu.name").equalsIgnoreCase(ac)) {
            comenzarAction();
        } else if (PropertyManager.readProperty("estadisticas.menu.name").equalsIgnoreCase(ac)) {
            estadisticasAction();
        } else if (PropertyManager.readProperty("grammar.menu.name").equalsIgnoreCase(ac)) {
            grammarAction();
        } else if (PropertyManager.readProperty("configuracion.menu.name").equalsIgnoreCase(ac)) {
            configuracionAction();
        } else if (PropertyManager.readProperty("analisis.rapido.menu.name").equalsIgnoreCase(ac)) {
            toggleAnalisisRapidoAction();
        } else if (PropertyManager.readProperty("ayuda.menu.sub.name").equalsIgnoreCase(ac)) {
            ayudaAction();
        } else if (PropertyManager.readProperty("info.menu.name").equalsIgnoreCase(ac)) {
            infoAction();
        } else if (ac.startsWith(PropertyManager.readProperty("recientes.submenu.open.name"))) {
            openRecentAction(ac);
        } else if (ac.equalsIgnoreCase(PropertyManager.readProperty("recientes.submenu.clearlist.name"))) {
            borrarListaAction();
        }
    }

    /**
	 * Invoked when the mouse button has been clicked (pressed
	 * and released) on a component.
	 */
    public void mouseClicked(MouseEvent e) {
    }

    /**
	 * Invoked when a mouse button has been pressed on a component.
	 */
    public void mousePressed(MouseEvent e) {
    }

    /**
	 * Invoked when a mouse button has been released on a component.
	 */
    public void mouseReleased(MouseEvent e) {
    }

    /**
	 * Invoked when the mouse enters a component.
	 */
    public void mouseEntered(MouseEvent e) {
        JComponent comp = (JComponent) e.getSource();
        writeStatusBar(" " + comp.getToolTipText());
    }

    /**
	 * Invoked when the mouse exits a component.
	 */
    public void mouseExited(MouseEvent e) {
        writeStatusBar(" ");
    }

    /**
	 * Evento de escucha para cuando un thread comienza.
	 *
	 * @param te El evento que describe el inicio de este thread.
	 */
    public void threadStarted(ThreadEvent te) {
        startProgress();
    }

    /**
	 * Evento de escucha para cuando un thread termina.
	 *
	 * @param te El evento que describe el fin de este thread.
	 */
    public void threadStopped(ThreadEvent te) {
        stopProgress();
        String tn = te.getThreadName();
        if (tn.equals(Trimmer.name)) {
            addTab(TabType.ORIGINAL);
        } else if (tn.equals(LexerExpresionTemporal.name)) {
            Estadistiquero.setExpresionesTemporales(lexer.getExpresionesTemporales());
            addTab(TabType.AET);
        } else if (tn.equals(LexerExpresionTemporal.nameSimple)) {
            analisisRapidoActionContinue();
        } else if (tn.equals(Saver.name)) {
            JOptionPane.showMessageDialog(getMe(), "<html>".concat(PropertyManager.readProperty("dialog.guardado.ok") + "<br>" + Saver.lastSave()), PropertyManager.readProperty("dialog.guardado.ok.title"), JOptionPane.INFORMATION_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.info.img")));
        }
    }

    /**
	 * Detecta los cambios en una propiedad de cierto componente.<br><br>
	 * Para este caso se hacen sílo 2 distinciones, si el nombre de la propiedad
	 * que cambií es {@link InsertOverwriteTextPane#TYPING_MODE_CHANGED_PROPERTY} se
	 * detecta el modo de escritura<br>
	 * si es <code>enabled</code> de la clase {@link UndoAction}, se habilitan
	 * los botones del undo y la "modificaciín" del archivo. Esto es, si el botín que representa
	 * la acciín "undo" estí deshabilitado, entonces ya no hay cambios que deshacer, por lo tanto
	 * el archivo ya no estí modificado.
	 *
	 * @param evt El evento que define la propiedad que cambií
	 */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equalsIgnoreCase("enabled")) {
            originalDocumentListener.modify((Boolean) evt.getNewValue());
        } else if (evt.getPropertyName().equalsIgnoreCase(InsertOverwriteTextPane.TYPING_MODE_CHANGED_PROPERTY)) {
            setWriteMode((InsertOverwriteTextPane.TypingMode) evt.getNewValue());
        }
    }

    /**
	 * Establece el tab seleccionado.
	 *
	 * @param index El índice del tab seleccionado.
	 */
    private void setSelectedTabType(int index) {
        if (index == originalIndex) {
            selectedTabType = TabType.ORIGINAL;
            buscarRemplazarPanel.updateTextComponent(originalTextPane);
        } else if (index == aetIndex) {
            selectedTabType = TabType.AET;
            buscarRemplazarPanel.updateTextComponent(aetTextPane);
        } else if (index == grammarIndex) {
            selectedTabType = TabType.GRAMATICA;
            buscarRemplazarPanel.updateTextComponent(gramaticaTextPane);
        } else if (index == estadisticasIndex) {
            selectedTabType = TabType.ESTADISTICAS;
        }
        toggleGuardar();
        toggleBuscar();
    }

    /**
	 * Invoked when the target of the listener has changed its state.
	 *
	 * @param e a ChangeEvent object
	 */
    public void stateChanged(ChangeEvent e) {
        setSelectedTabType(tabs.getSelectedIndex());
    }

    /**
	 * Called when the caret position is updated.
	 *
	 * @param e the caret event
	 */
    public void caretUpdate(CaretEvent e) {
        int index = tabs.getSelectedIndex();
        if (index == -1) {
            setCursorCoordinates(0, 0);
        } else if (index == originalIndex) {
            setCursorCoordinates(CaretToCoordinates.getCaretLinePosition(originalTextPane), CaretToCoordinates.getCaretColumnPosition(originalTextPane));
        } else if (index == aetIndex) {
            setCursorCoordinates(CaretToCoordinates.getCaretLinePosition(aetTextPane), CaretToCoordinates.getCaretColumnPosition(aetTextPane));
        } else if (index == grammarIndex) {
            setCursorCoordinates(CaretToCoordinates.getCaretLinePosition(gramaticaTextPane), CaretToCoordinates.getCaretColumnPosition(gramaticaTextPane));
        }
    }

    /**
	 * Muestra la regla correspondiente.
	 *
	 * @param ev El evento que define cuíl es la regla que se mostrarí.
	 */
    public void showRegla(final ReglaEvent ev) {
        addTab(TabType.GRAMATICA);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tabs.setSelectedIndex(grammarIndex);
                gramaticaTextPane.grabFocus();
                gramaticaTextPane.select(ev.getRegla().tag());
            }
        });
    }

    /**
	 * Agrega un archivo como <i>original</i> si se arrastra (<i>drag 'n drop</i>).
	 *
	 * @param filename El archivo agregado.
	 */
    @SuppressWarnings("unchecked")
    public void addTransferableFile(String filename) {
        int punto = filename.indexOf('.');
        String extension = filename.substring(punto).toLowerCase();
        if (punto == -1 || extension.equals(Saver.SaveExtension.HTM.extension()) || extension.equals(Saver.SaveExtension.HTML.extension()) || extension.equals(Saver.SaveExtension.SHTML.extension()) || extension.equals(Saver.SaveExtension.TEXT.extension()) || extension.equals(Saver.SaveExtension.TXT.extension()) || extension.equals(Saver.SaveExtension.TRIMMED.extension()) || extension.equals(Saver.SaveExtension.XHTML.extension())) {
            abrirORIGINAL(new File(filename));
        } else if (filename.endsWith(".aet")) {
            SerialWrapper sw = Opener.openWrapped(filename);
            addAETTab(Vector.class.cast(sw.getComponentByClass(Vector.class)), String.class.cast(sw.getComponentByClass(String.class)));
        } else {
            JOptionPane.showMessageDialog(this, PropertyManager.readProperty("dialog.unknown.file.format"), PropertyManager.readProperty("dialog.unknown.file.format.title"), JOptionPane.INFORMATION_MESSAGE, new ImageIcon(PropertyManager.readProperty("dialog.info.img")));
        }
    }

    /**
	 * Clase que escucha cuándo un documento original cambió.
	 */
    class OriginalDocumentListener implements DocumentListener, Serializable {

        /**
		 * Gives notification that there was an insert into the document.  The
		 * range given by the DocumentEvent bounds the freshly inserted region.
		 *
		 * @param e the document event
		 */
        public void insertUpdate(DocumentEvent e) {
            modify(true);
        }

        /**
		 * Gives notification that a portion of the document has been
		 * removed.  The range is given in terms of what the view last
		 * saw (that is, before updating sticky positions).
		 *
		 * @param e the document event
		 */
        public void removeUpdate(DocumentEvent e) {
            modify(true);
        }

        /**
		 * Gives notification that an attribute or set of attributes changed.
		 *
		 * @param e the document event
		 */
        public void changedUpdate(DocumentEvent e) {
            modify(true);
        }

        /**
		 * En caso de ser modificado, cambia el icono de <i>modificado</i>.
		 *
		 * @param isModified Si el archivo se modificó.
		 */
        public void modify(boolean isModified) {
            if (isModified) {
                tabs.setIconAt(originalIndex, new ImageIcon(PropertyManager.readProperty("tab.modified.icon")));
                originalModified = true;
                toggleGuardar();
            } else {
                tabs.setIconAt(originalIndex, null);
                originalModified = false;
                toggleGuardar();
            }
        }
    }

    /**
	 * Clase que esucha eventos de tipo <i>undo</i> en un documento.
	 */
    class UndoableDocumentLister implements UndoableEditListener, Serializable {

        /**
		 * An undoable edit happened
		 */
        public void undoableEditHappened(UndoableEditEvent e) {
            undo.addEdit(e.getEdit());
            undoAction.updateUndoState();
            redoAction.updateRedoState();
        }
    }

    /**
	 * Escucha eventos en el panel de análisis rápido.
	 */
    private class AnalisisRapidoListener implements ActionListener {

        /**
		 * Invoked when an action occurs.
		 */
        public void actionPerformed(ActionEvent e) {
            String ac = e.getActionCommand();
            if (PropertyManager.readProperty("analisisrapido.button.ok.name").equals(ac)) {
                analisisRapidoAction();
            } else if (PropertyManager.readProperty("analisisrapido.button.cancell.name").equals(ac)) {
                rapidoTA.setText(null);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        rapidoTA.requestFocus();
                    }
                });
            }
        }
    }
}
