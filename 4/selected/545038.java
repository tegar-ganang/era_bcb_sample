package net.sf.mavenize.common.cli;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * @see field/method annotation - http://www.infoq.com/articles/spring-2.5-part-1
 * 
 * http://www.opengroup.org/onlinepubs/009695399/basedefs/xbd_chap12.html#tag_12_02
 * http://www.faqs.org/docs/artu/ch10s05.html
 * 
 * @author alessandroe
 */
public class CliExecutor {

    CommandLineParser parser = new PosixParser();

    Options options = new Options();

    static String DEFAULT_SYNTAX = "[OPTION]...";

    Object app;

    public CliExecutor(Object app) {
        this.app = app;
        init();
    }

    protected org.apache.commons.cli.Option toOption(Option option) {
        String description = "".equals(option.description()) ? option.longName() : option.description();
        org.apache.commons.cli.Option opt = new org.apache.commons.cli.Option(option.name(), description);
        opt.setLongOpt(option.longName());
        opt.setRequired(option.required());
        if (!option.argument().equals("")) {
            opt.setArgName(option.argument());
            opt.setArgs(1);
        }
        if (option.separator() != '\0') {
            opt.setValueSeparator(option.separator());
        }
        return opt;
    }

    protected static void addHelpOption(Options opts) {
        org.apache.commons.cli.Option helpOpt = new org.apache.commons.cli.Option("h", "display this help and exit");
        helpOpt.setLongOpt("help");
        opts.addOption(helpOpt);
    }

    protected void init() {
        addHelpOption(options);
        for (Method m : app.getClass().getMethods()) {
            Option option = m.getAnnotation(Option.class);
            if (null != option) {
                org.apache.commons.cli.Option opt = toOption(option);
                options.addOption(opt);
            }
        }
        for (Field f : app.getClass().getDeclaredFields()) {
            Option option = f.getAnnotation(Option.class);
            if (null != option) {
                org.apache.commons.cli.Option opt = toOption(option);
                options.addOption(opt);
            }
        }
    }

    public static class TweakedHelpFormatter extends HelpFormatter {

        public TweakedHelpFormatter() {
            setSyntaxPrefix("Usage: ");
        }

        @Override
        public void printWrapped(PrintWriter pw, int width, int nextLineTabStop, String text) {
            super.printWrapped(pw, width, nextLineTabStop, text);
            pw.println();
        }

        @Override
        public void printOptions(PrintWriter pw, int width, Options options, int leftPad, int descPad) {
            super.printOptions(pw, width, options, leftPad, descPad);
            pw.println();
        }

        @Override
        protected String rtrim(String s) {
            return s;
        }

        @Override
        public void printUsage(PrintWriter pw, int width, String cmdLineSyntax) {
            printWrapped(pw, width, getSyntaxPrefix().length(), getSyntaxPrefix() + cmdLineSyntax);
        }
    }

    protected String getApplicationName() {
        String applicationName = "";
        CliApplication application = app.getClass().getAnnotation(CliApplication.class);
        if (null != application) {
            applicationName = application.name();
        }
        if ("".equals(applicationName)) {
            if (app.getClass().getName().endsWith(".Application")) applicationName = app.getClass().getPackage().getName(); else applicationName = app.getClass().getName();
        }
        return applicationName;
    }

    public void printUsage() {
        printUsage(new PrintWriter(System.out));
    }

    public void printUsage(PrintWriter pw) {
        HelpFormatter formatter = new TweakedHelpFormatter();
        CliApplication application = app.getClass().getAnnotation(CliApplication.class);
        if (null != application) {
            String cmdLineSyntax = application.syntax();
            if ("".equals(cmdLineSyntax)) {
                cmdLineSyntax = DEFAULT_SYNTAX;
            }
            formatter.printHelp(getApplicationName() + " " + cmdLineSyntax, application.header(), options, application.footer());
        } else {
            formatter.printHelp(getApplicationName(), options);
        }
    }

    public void execute(String[] args) {
        try {
            CommandLine cl = parser.parse(options, args, true);
            List<String> argList = cl.getArgList();
            if (cl.hasOption("h")) {
                printUsage();
            } else {
                for (Method m : app.getClass().getMethods()) {
                    Option option = m.getAnnotation(Option.class);
                    if (null != option) {
                        if (cl.hasOption(option.name())) {
                            org.apache.commons.cli.Option opt = options.getOption(option.name());
                            if (opt.hasValueSeparator()) {
                                setOptionValues(app, m, cl.getOptionValues(option.name()));
                            } else {
                                setOption(app, m, cl.getOptionValue(option.name()));
                            }
                        }
                    }
                }
                for (Field f : app.getClass().getDeclaredFields()) {
                    Option option = f.getAnnotation(Option.class);
                    if (null != option) {
                        if (cl.hasOption(option.name())) {
                            org.apache.commons.cli.Option opt = options.getOption(option.name());
                            if (opt.hasValueSeparator()) {
                                setOptionValues(app, f, cl.getOptionValues(option.name()));
                            } else {
                                setOption(app, f, cl.getOptionValue(option.name()));
                            }
                        }
                    }
                }
                runApplication(cl.getArgs());
            }
        } catch (ParseException e) {
            try {
                Options opts = new Options();
                addHelpOption(opts);
                CommandLineParser parser = new PosixParser();
                CommandLine cl = parser.parse(opts, args);
                if (cl.hasOption("help")) {
                    printUsage();
                } else {
                    tryHelp(e);
                }
            } catch (Exception ignore) {
                tryHelp(e);
            }
        }
    }

    protected void tryHelp(Exception e) {
        System.err.println(e.getMessage());
        System.err.println("Try `" + getApplicationName() + " --help' for more information.");
    }

    protected void runApplication(String[] args) throws ParseException {
        Method m;
        try {
            m = app.getClass().getMethod("run", new Class[] {});
        } catch (Exception e) {
            try {
                m = app.getClass().getMethod("run", new Class[] { String[].class });
            } catch (Exception e2) {
                throw new RuntimeException("Could not find applicaiton run method");
            }
            try {
                m.invoke(app, new Object[] { args });
                return;
            } catch (InvocationTargetException invocationTargetException) {
                if (invocationTargetException.getTargetException() instanceof ParseException) throw (ParseException) invocationTargetException.getTargetException(); else throw new RuntimeException(invocationTargetException.getTargetException());
            } catch (Exception e3) {
                throw new RuntimeException(e3);
            }
        }
        try {
            m.invoke(app, new Object[] {});
            return;
        } catch (InvocationTargetException invocationTargetException) {
            throw new RuntimeException(invocationTargetException.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setOption(Object bean, Field f, Object value) {
        try {
            f.set(bean, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setOptionValues(Object bean, Field f, String[] values) {
        try {
            f.set(bean, values);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setOption(Object bean, Method m, Object value) {
        try {
            m.invoke(bean, new Object[] { value });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setOptionValues(Object bean, Method m, String[] values) {
        try {
            m.invoke(bean, new Object[] { values });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
