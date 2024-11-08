package org.gocha.gui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import org.gocha.collection.map.ReadOnlyMap;
import org.gocha.common.ExitListener;
import org.gocha.common.ExitEvent;
import org.gocha.common.ScriptUtil;
import org.gocha.common.SupportInitialize;
import org.gocha.files.FileUtil;
import org.gocha.text.TextUtil;
import org.gocha.types.TypesUtil;
import org.gocha.types.ValueController;

/**
 * Глобальный класс GUI приложения.
 * <p>
 * Данный класс предназначен как единственный экземпляр приложения,
 * который отмечает начало и завершение работы приложения.<br/>
 * К прослушиванию события выхода можно подцепить любой объект реализующий <b>ExitListener.</b>
 * Начало работы приложения отмечается при помощи метода <b>start()</b>, завершение при помощи <b>fireExit()</b>
 * </p><p>
 * Также можно подципить объекты к <b>AutoClose</b>, которые должны быть автоматически закрыти при получении
 * сообщения выхода (<i>fireExit()</i>)
 * </p><p>
 * Для каждого отдельного прилоежния подразумевает свой контекст,
 * который связан с соответствующей пользовательской директорией.<br/>
 * Контекст прдставляется как имя класса с указанием пакета. Например: <b>myorg.app.ver</b>.
 * При запросе рабочего каталога приложения
 * Соответственно в каталоге пользователя будут создан каталог myorg/app/ver (если его еще нет)
 * </p><p>
 * При вызове метода <i>start()</i> можно указать параметры:
 * <table border="1">
 *   <tr>
 *     <td><b>Параметр</b></td>
 *     <td><b>Описание</b></td>
 *     <td><b>Примеры</b></td>
 *   </tr>
 *   <tr>
 *     <td><b>-appdir <i>путь_к_каталогу</i></b></td>
 *     <td>Указывает контекст приложения - путь к каталогу.</td>
 *     <td>
 *       Пример unix:<br/>
 *       <b>-appdir "/home/user/applicationDataDir"</b><br/><br/>
 *       Пример windows:<br/>
 *       <b>-appdir "C:\\Documents and settings\\user\\applicationDataDir"</b>
 *     </td>
 *   </tr>
 *   <tr>
 *     <td><b>-webapp <i>on|off|true|false</i></b></td>
 *     <td>
 *       При значении <i>on</i> или <i>true</i> -
 *       указывает на необходимоть работы в режиме java webstart.
 *       В этом режиме метод <i>getLocalApplicationDirectory()</i> - Возвращает пустую ссылку
 *     </td>
 *     <td>
 *       Пример 1:<br/>
 *       <b>-webapp "on"</b>
 *       <br/><br/>
 *       Пример 2:<br/>
 *       <b>-webapp "false"</b>
 *     </td>
 *   </tr>
 *   <tr>
 *     <td><b>-auto <i>Класс{;Класс}</i></b></td>
 *     <td>Указывает на список классов которые необходимо создать после вызова start()</td>
 *     <td>
 * Пример: <br>
 * 1. <b>-auto "org.app.pluginA"</b> - Создаст при вызове start() класс org.app.pluginA<br/>
 * 2. <b>-auto "org.app.PluginA;org.app.PluginB"</b>
 * - Создаст при вызове start() классы org.app.PluginA затем org.app.PluginB
 *    </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       <b>-argsFile <i>Имя файла</i></b>
 *     </td>
 *     <td>
 *       Указывает файл с дополнительными параметрами, которые не поместились в коммандную строку.<br />
 *       Файл должен быть в формате XML:<br/>
 *<code>
 *&lt;start&gt;<br/>
 *&nbsp;&nbsp;&nbsp;&nbsp;&lt;arg name="имя параметра без тире" value="значение" /&gt;<br/>
 *&lt;/start&gt;
 *</code>
 *     </td>
 *     <td>
 *       -argsFile "some_file.xml"
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       <b>-argsFileCS <i>Кодировка файла</i></b>
 *     </td>
 *     <td>
 *       Указывает кодировку файла с дополнительными параметрами.
 *       По умолчанию испольуется UTF-8
 *     </td>
 *     <td>
 *       -argsFileCS "UTF-8"
 *     </td>
 *   </tr>
 * </table>
 * </p>
 * @see org.gocha.gui.ApplicationGlobal#start(java.lang.String[])
 * <b>start()</b> - Отмечает стар приложения <br/>
 *
 * @see org.gocha.gui.ApplicationGlobal#fireExit(java.lang.Object)
 * <b>fireExit()</b> - Отмечает завершение работы приложения <br/>
 *
 * @see org.gocha.gui.ApplicationGlobal#getLocalApplicationDirectory() 
 * <b>getLocalApplicationDirectory()</b> - Возвращает каталог (контекст) приложения <br/>
 *
 * @see org.gocha.common.ExitListener
 * <b>ExitListener</b> - Слушает выход из приложения <br/>
 *
 * @see org.gocha.gui.AutoClose
 * <b>AutoClose</b> -
 * Класс/Сиглетон слушает сообщения выхода приложения
 * и закрывает при выходе указанные объекты.
 * Своебразный адаптер для закрытия объектов<br/>
 * @author gocha
 */
public class ApplicationGlobal {

    /**
     * Конструктор закрыт для непосредственного вызова.
     * Вместо этого предлогается использовать статичный метод instance()
     * @see org.gocha.gui.ApplicationGlobal#instance()
     */
    protected ApplicationGlobal() {
        this.addExitListener(createAutoClose());
    }

    /**
     * Создает/Возвращает объект который закрывает обекты при выходе
     * @return Автоматический закрыватель
     */
    protected AutoClose createAutoClose() {
        return AutoClose.instance();
    }

    protected static GuiFactory factory = null;

    /**
     * Возвращает фабрику создающую объект ApplicationGlobal
     * @return Фабрика классов
     */
    public static GuiFactory getFactory() {
        if (factory == null) factory = new DefaultFactory();
        return factory;
    }

    /**
     * Устанавливает фабрику создающую объект ApplicationGlobal
     * @param factory Фабрика классов
     */
    public static void setFactory(GuiFactory factory) {
        ApplicationGlobal.factory = factory;
    }

    /**
     * Создает экземпляр ApplicationGlobal
     * @return экземпляр ApplicationGlobal
     */
    public static ApplicationGlobal create() {
        return new ApplicationGlobal();
    }

    protected static ApplicationGlobal inst = null;

    /**
     * Возвращает экземпляр ApplicationGlobal.
     * Доавбляет слушателя AutoClose.instance() на завершение работыю
     * @return экземпляр ApplicationGlobal
     */
    public static ApplicationGlobal instance() {
        if (inst == null) {
            inst = getFactory().createGlobal();
            if (inst != null) inst.addExitListener(AutoClose.instance());
        }
        return inst;
    }

    protected Collection exitListeners = new HashSet();

    protected Map<String, String> programArgs = new HashMap<String, String>();

    /**
     * Возвращает параметры указанные при запуске программы.
     * <p>
     * Данные параметры доступны только для чтения.
     * Параметры разделяются на ключ/значение.
     * Признаком ключа является символ тире "<b>-</b>".
     * </p><p>
     * В данной коллекции ключ указан без тире.
     * </p>
     * @return параметры указанные при запуске программы
     */
    public Map<String, String> getProgramAruments() {
        return programArgs;
    }

    protected Boolean webstartApp = null;

    /**
     * Указывает что программа была запущена в режиме Java web start.
     * <p>
     * На данный признак влияет параметр коммандой строки <b>-webapp "on"</b> либо <b>-webapp "true"</b>
     * </p>
     * @return Указывает что программа была запущена в режиме Java web start.
     */
    public boolean isWebStartApplication() {
        if (webstartApp == null) {
            if (getProgramAruments().containsKey(WEBSTART_APPLICATION)) {
                String v = getProgramAruments().get(WEBSTART_APPLICATION);
                webstartApp = v.equalsIgnoreCase("on") || v.equalsIgnoreCase("true");
            } else {
                webstartApp = false;
            }
        }
        return webstartApp;
    }

    protected File localApplicationDirectory = null;

    private static String[] contextToNames(String ctx) {
        if (ctx == null) return new String[] { "" };
        if (ctx.startsWith(".") && ctx.length() > 1) {
            ctx = ctx.substring(1);
        }
        String[] ctxDirNames = ctx.split("\\.");
        if (ctx.startsWith(".") && ctxDirNames.length > 0 && !ctxDirNames[0].startsWith(".")) {
            ctxDirNames[0] = "." + ctxDirNames[0];
        }
        return ctxDirNames;
    }

    /**
     * Создает временную директорию
     * @param nameTmpl Шаблон имени ("{0}") временной директории
     * @return Временная директория или null;
     */
    private static File createTempDir(String nameTmpl) {
        String sTmpDir = System.getProperty("java.io.tmpdir");
        if (sTmpDir == null) return null;
        File tmpDir = new File(sTmpDir);
        File res = null;
        Random rnd = new Random(new Date().getTime());
        while (true) {
            res = new File(tmpDir, TextUtil.template(nameTmpl, rnd.nextInt()));
            if (res.exists()) continue;
            if (!res.mkdirs()) return null;
            res.deleteOnExit();
            break;
        }
        return res;
    }

    private File getAppDirFromHome() {
        File appd = null;
        String userHome = System.getProperty("user.home", null);
        if (userHome == null) {
            return null;
        } else {
            String[] ctxDirNames = contextToNames(context);
            File appDir = new File(userHome);
            for (String ctxD : ctxDirNames) {
                appDir = new File(appDir, ctxD);
            }
            appd = appDir;
        }
        return appd;
    }

    /**
     * Создает временную директорию согласно контексту.
     * @return Временная директория или null;
     */
    private File createTempAppDir() {
        String ctx = context;
        if (ctx == null) ctx = "appdir.{0}.tmp"; else {
            if (ctx.startsWith(".") && ctx.length() > 1) ctx = ctx.substring(1);
            if (ctx.contains("{")) ctx = ctx.replace("{", "{{");
            ctx = ctx + ".{0}.tmp";
        }
        File f = createTempDir(ctx);
        if (f == null) return null;
        return f;
    }

    /**
     * Директория данных программы.
     * <p>
     * На данное значение влияет параметр ком. строки LOCAL_APPLICATION_DIRECTORY (-appdir).<br/>
     * <ul>
     * <li>Если он не указан, то 
     * <ul>
     *      <li>Если указан -argsFile, то используется каталог -argsFile.</li>
     *      <li>Иначе используется домашняя директория пользователя + контекст.</li>
     *      <li>Либо временная директория.</li>
     * </ul>
     * </li>
     * <li>Если запрещен доступ к локальным данным (режим WEB), то создает временную директорию.</li>
     * </ul>
     * </p>
     * @return Директория данных программы или null
     * @see #getContext()
     */
    public File getLocalApplicationDirectory() {
        if (localApplicationDirectory != null) return localApplicationDirectory;
        if (isWebStartApplication()) {
            localApplicationDirectory = createTempAppDir();
            return localApplicationDirectory;
        }
        boolean appDirSetted = false;
        for (String appDirKey : appDir) {
            if (getProgramAruments().containsKey(appDirKey)) {
                File appDirFile = new File(getProgramAruments().get(appDirKey));
                localApplicationDirectory = appDirFile;
                appDirSetted = true;
                break;
            }
        }
        if (!appDirSetted && getProgramAruments().containsKey(ARGUMENT_FILE)) {
            File fArg = new File(getProgramAruments().get(ARGUMENT_FILE));
            localApplicationDirectory = fArg.getParentFile();
            if (localApplicationDirectory == null) localApplicationDirectory = getAppDirFromHome();
        } else if (!appDirSetted && getProgramAruments().containsKey(INIT_JS_TMPL)) {
            File fArg = new File(getProgramAruments().get(INIT_JS_TMPL));
            localApplicationDirectory = fArg.getParentFile();
            if (localApplicationDirectory == null) localApplicationDirectory = getAppDirFromHome();
        } else {
            localApplicationDirectory = getAppDirFromHome();
        }
        if (localApplicationDirectory == null) {
            localApplicationDirectory = createTempAppDir();
            if (localApplicationDirectory == null) return null;
        }
        if (!localApplicationDirectory.exists()) {
            if (!localApplicationDirectory.mkdirs()) {
                Logger.getLogger(ApplicationGlobal.class.getName()).log(Level.SEVERE, "can't create directory {0}", localApplicationDirectory);
                localApplicationDirectory = null;
                return null;
            }
        } else if (!localApplicationDirectory.isDirectory() || !localApplicationDirectory.canRead() || !localApplicationDirectory.canWrite()) {
            Logger.getLogger(ApplicationGlobal.class.getName()).log(Level.SEVERE, "can't read/write directory {0}", localApplicationDirectory);
            localApplicationDirectory = null;
            return null;
        }
        return localApplicationDirectory;
    }

    /**
     * Параметр ком. строки указывающий директорию данных программы
     */
    public static final String LOCAL_APPLICATION_DIRECTORY = "appDir";

    private static final String[] appDir = new String[] { "appdir", "appDir", "AppDir", "APPDIR", LOCAL_APPLICATION_DIRECTORY };

    /**
     * Параметр ком. строки указывающий режим веб приложения<br/>
     * <b>-webapp "on"</b> либо <b>-webapp "true"</b>
     */
    public static final String WEBSTART_APPLICATION = "webapp";

    protected static String context = null;

    /**
     * Возвращает контекст приложения
     * @return Контекст приложения
     * @see #setContext(java.lang.String)
     */
    public static String getContext() {
        return context;
    }

    /**
     * Устанавливает контекст приложения.<br/><br/>
     * Контекст - Определяет каталог в домашней директории пользователя где храняться настройки,
     * если не указанно явно через коммандную строку <b>-appdir</b>.<br/>
     * Например: контекст указан как: <b>.mydomain.ru.myapplication.ver1</b> 
     * то соответственно каталог будет:<b>~/.mydomain/ru/myapplication/ver1</b>
     * @param context Контекст приложения
     * @see #LOCAL_APPLICATION_DIRECTORY
     */
    public static void setContext(String context) {
        ApplicationGlobal.context = context;
    }

    /**
     * Устанавливает контекст приложения
     * @param context Контекст приложения
     * @see #setContext(java.lang.String)
     */
    public static void setContext(Class<?> context) {
        if (context != null) {
            ApplicationGlobal.context = "." + context.getName();
        }
    }

    /**
     * Параметр ком. строки указывающий на файл с дополнительными параметрами <b>start()</b><br/>
     * Этот файл объединяется с текущими параметрами командной строки.<br/>
     * Для чтения файла используется FileUtil.readProperties(File,Charset)
     * @see org.gocha.gui.ApplicationGlobal#start(java.lang.String[])
     * @see org.gocha.gui.ApplicationGlobal#ARGUMENT_FILE_CS
     * @see org.gocha.files.FileUtil#readProperties(java.io.File, java.nio.charset.Charset) 
     */
    public static final String ARGUMENT_FILE = "argsFile";

    /**
     * Указывает какую кодировку использовать для файла указаного параметром <b>argsFile</b>.
     * По уполчанию используется кодировка <b>UTF8</b>.
     * @see org.gocha.gui.ApplicationGlobal#ARGUMENT_FILE
     */
    public static final String ARGUMENT_FILE_CS = "argsFileCS";

    /**
     * Имя инициализируемого класса, указывается параметром <b>initBean</b>.<br/>
     * <ul>
     * <li>Класс должен содержать конструктор по умолчанию.</li>
     * <li>Свойства класса инициализируются согласно переданым параметрам, 
     * для инициализации используется org.ogcha.types.DefaultTypesConvertors.</li>
     * <li>Класс может реализовывать интерфейс org.gocha.common.SupportInitialize</li>
     * </ul>
     * @see org.gocha.common.SupportInitialize
     * @see org.gocha.types.DefaultTypesConvertors
     */
    public static final String INIT_BEAN = "initBean";

    /**
     * Парсинг аргументов коммандной строки.
     * Название аргумента должно начинатся с минуса, а за ним следовать само значение:<br/>
     * <code>
     * programme -<i>argumentName</i> <i>value</i>
     * </code>
     * @param args Аргументы коомандной строки.
     * @param readArgFile Читать файл переданый через аргумент ARGUMENT_FILE (argsFile)
     * @param readEnv Читать перменные окружения
     * @return Ключ/Значения
     */
    public static Map<String, String> parseArguments(String[] args, boolean readArgFile, boolean readEnv) {
        Map<String, String> result = new HashMap<String, String>();
        if (args == null) {
            throw new IllegalArgumentException("args == null");
        }
        if (readEnv) {
            result.putAll(System.getenv());
        }
        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            String val = args[i + 1];
            if (arg.startsWith("-") && arg.length() > 1) {
                result.put(arg.substring(1), val);
            }
        }
        if (result.containsKey(ARGUMENT_FILE)) {
            try {
                String csName = result.containsKey(ARGUMENT_FILE_CS) ? result.get(ARGUMENT_FILE_CS) : null;
                Charset cs = csName == null ? FileUtil.UTF8() : Charset.forName(csName);
                Properties props = FileUtil.readProperties(new File(result.get(ARGUMENT_FILE)), cs);
                for (String propName : props.stringPropertyNames()) {
                    result.put(propName, props.getProperty(propName));
                }
            } catch (IOException ex) {
                Logger.getLogger(ApplicationGlobal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }

    /**
     * Отмечает стар приложения
     * @param args Аргумент программы
     * @see org.gocha.gui.ApplicationGlobal#ARGUMENT_FILE
     * @see org.gocha.gui.ApplicationGlobal#ARGUMENT_FILE_CS
     * @see org.gocha.gui.ApplicationGlobal#LOCAL_APPLICATION_DIRECTORY
     * @see org.gocha.gui.ApplicationGlobal#WEBSTART_APPLICATION
     * @see #INIT_BEAN
     */
    public void start(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("args==null");
        }
        programArgs = new ReadOnlyMap<String, String>(parseArguments(args, true, true));
        initBean();
        initJavaScript();
    }

    /**
     * Указывает какой файл выполнить как JavaScript. <br/>
     * Используется как шаблон имени.
     * @see #initJavaScript
     */
    public static final String INIT_JS_TMPL = "initJavaScript";

    /**
     * Указывает кодировку файла JavaScript
     */
    public static final String INIT_JS_CS = "initJavaScriptCS";

    /**
     * Если указан аргумент <b>initJavaScript</b> - то выполняет уазанный файл как JavaScript.<br/>
     * Аргумент <b>initJavaScriptCS</b> - Указывает кодировку файла, по умолчанию использяется та что в ОС.<br/>
     * <br/>
     * <b>initJavaScript</b> - используется как шаблон, в который подставляются аргументы программы: 
     * getProgramAruments() + значение localApplicationDirectory - ключ <b>appdir</b>
     */
    protected void initJavaScript() {
        try {
            Map<String, String> args = getProgramAruments();
            if (!args.containsKey(INIT_JS_TMPL)) return;
            String initJS = args.get(INIT_JS_TMPL);
            Charset cs = null;
            if (args.containsKey(INIT_JS_CS)) cs = Charset.forName(args.get(INIT_JS_CS)); else cs = Charset.defaultCharset();
            HashMap<String, String> tmplV = new HashMap<String, String>();
            tmplV.putAll(args);
            File appDirF = getLocalApplicationDirectory();
            if (appDirF != null) {
                for (String appDirKey : appDir) {
                    tmplV.put(appDirKey, appDirF.getAbsolutePath());
                }
            }
            File file = new File(TextUtil.template(initJS, tmplV));
            ScriptUtil.evalJavaScript(file, cs, tmplV);
        } catch (IOException ex) {
            Logger.getLogger(ApplicationGlobal.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ScriptException ex) {
            Logger.getLogger(ApplicationGlobal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Проверка и если есть то исполнение Init Bean
     * @see #INIT_BEAN
     */
    protected void initBean() {
        try {
            String beanClassName = null;
            if (getProgramAruments().containsKey(INIT_BEAN)) {
                beanClassName = getProgramAruments().get(INIT_BEAN);
            } else {
                return;
            }
            Class cls = Class.forName(beanClassName);
            Object bean = cls.newInstance();
            if (bean instanceof SupportInitialize) ((SupportInitialize) bean).beginInit();
            Iterable<ValueController> props = TypesUtil.Iterators.propertiesOf(bean);
            TypesUtil.textMapToValueControllers(getProgramAruments(), props);
            if (bean instanceof SupportInitialize) ((SupportInitialize) bean).endInit();
        } catch (InstantiationException ex) {
            Logger.getLogger(ApplicationGlobal.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ApplicationGlobal.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ApplicationGlobal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Отмечает завершение работы приложения. Рассылает всем подпищикам о завершении работы.
     * @param source Кто послал сообщение о завершении работы
     */
    public void fireExit(Object source) {
        ExitEvent evnt = new ExitEvent(source);
        for (Object o : exitListeners.toArray()) {
            if (o != null && o instanceof ExitListener) ((ExitListener) o).exitEvent(evnt);
        }
    }

    /**
     * Добавляет подписчика на событие выхода
     * @param listener Подписчик
     */
    public void addExitListener(ExitListener listener) {
        if (listener != null) exitListeners.add(listener);
    }

    /**
     * Удаляет подписчика от события выхода
     * @param listener Подписчик
     */
    public void removeExitListener(ExitListener listener) {
        exitListeners.remove(listener);
    }
}
