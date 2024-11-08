package org.regadou.nalasys.system;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.regadou.nalasys.*;

public class Service {

    public Service() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            debug("Syntax: java -jar nalasys.jar language=<lang-code> database=<jdbc-url> server=<server-port> src=<filename-of-text-to-interpret>");
            return;
        }
        Map params = Types.toMap(args);
        params.put("input", new Stream(new InputStreamReader(System.in)));
        params.put("output", new Stream(System.out));
        Context.init(params);
        TcpServer server = null;
        Object port = params.get("server");
        if (port != null && !port.toString().trim().equals("")) {
            try {
                server = new TcpServer(Integer.parseInt(port.toString().trim()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Object src = params.get("src");
        if (src == null) interactive("\n? "); else {
            try {
                write(Types.toString(parse(read(src), null)) + "\n", null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (server != null) server.close();
    }

    public static Object convert(Object obj, Object type) throws Exception {
        try {
            if (type == null) return obj;
            Class cl;
            if (type instanceof Class) cl = (Class) type; else if (type instanceof CharSequence) cl = Class.forName(type.toString()); else if (type instanceof char[]) cl = Class.forName(new String((char[]) type)); else if (type instanceof Number) cl = Data.CLASSES[((Number) type).intValue()]; else return new RuntimeException("Unknown conversion type " + type);
            return Types.convert(obj, cl);
        } catch (Exception e) {
            return e;
        }
    }

    public static boolean copy(Object src, Object dst) {
        return Types.copy(src, dst);
    }

    public static void debug(String txt) {
        debug(txt, null);
    }

    public static void debug(String txt, Throwable t) {
        System.out.println(txt);
        if (t != null) t.printStackTrace();
    }

    public static String escape(String txt, String esc) {
        if (txt == null) return "null"; else if (esc == null) return txt; else esc = esc.trim().toLowerCase();
        for (int e = 0; e < Types.ESCAPES.length; e++) {
            if (esc.equals(Types.ESCAPES[e])) return Types.escapeString(txt, null, e);
        }
        return txt;
    }

    public static Object get(Object obj, Object prop) {
        if (obj == null || prop == null) return null; else return DataFactory.getInstance(obj).getProperty(DataFactory.getInstance(prop));
    }

    public static void interactive(String prompt) {
        if (Context.getCurrent().getInput() == null) {
            try {
                Context.getCurrent().getOutput().write("\nNo input stream for interactive session\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        boolean runLoop = true;
        while (runLoop) {
            try {
                Context.getCurrent().getOutput().write(prompt);
                String txt = Context.getCurrent().getInput().getReader().readLine();
                if (txt != null && !txt.trim().equals("")) {
                    Object val = parse(txt, null);
                    if (val instanceof Data) val = ((Data) val).getValue();
                    Context.getCurrent().getOutput().write("\n= " + print(val, null) + "\n");
                }
            } catch (Exception e) {
                try {
                    Context.getCurrent().getOutput().write("\n" + print(e, null) + "\n");
                } catch (Exception e2) {
                    e2.printStackTrace();
                    runLoop = false;
                }
            }
        }
    }

    public static Object invoke(Object target, String method, Object params) {
        if (target instanceof CharSequence) {
            String txt = target.toString();
            int p = txt.indexOf('[');
            try {
                if (p > 0) {
                    Integer n = new Integer(txt.substring(p + 1).replace("]", ""));
                    Class cl = Class.forName(txt.substring(0, p));
                    params = new Object[] { cl, n };
                    target = Array.class;
                    method = "newInstance";
                } else target = Class.forName(txt);
            } catch (Exception e) {
            }
        }
        try {
            return new java.beans.Expression(target, method, Types.toArray(params)).getValue();
        } catch (Exception e) {
            return e;
        }
    }

    public static String[] languages() {
        return Parser.getLanguages();
    }

    public static Object load(Object src) {
        try {
            if (src instanceof Address) return ((Address) src).getContent();
            Data data = DataFactory.newInstance(src, Address.class);
            if (data instanceof Address) return ((Address) data).getContent(); else throw new RuntimeException(src + " is not a valid address");
        } catch (Exception e) {
            return e;
        }
    }

    public static Object parse(String txt, String lang) {
        if (lang == null || lang.equals("")) {
            lang = Context.getCurrent().getLanguage();
            if (lang == null) lang = Parser.DEFAULT_LANGUAGE;
        }
        LanguageHandler handler = Parser.getLanguageHandler(lang);
        if (handler == null) {
            String msg = "Unknown language " + lang;
            return new RuntimeException(msg);
        }
        return handler.parse(txt);
    }

    public static String print(Object obj, String lang) {
        if (lang == null || lang.equals("")) {
            lang = Context.getCurrent().getLanguage();
            if (lang == null) lang = Parser.DEFAULT_LANGUAGE;
        }
        LanguageHandler handler = Parser.getLanguageHandler(lang);
        if (handler == null) {
            String msg = "Unknown language " + lang;
            return new RuntimeException(msg).toString();
        }
        return handler.print(obj);
    }

    private static Method WEB_PROCESS = null;

    public static void process(Object input, Object output) {
        Stream is;
        if (input == null) is = Context.getCurrent().getInput(); else if (input instanceof Stream) is = (Stream) input; else if (Stream.isStreamable(input)) is = Types.toStream(input); else is = null;
        Stream os;
        if (output == null) os = Context.getCurrent().getOutput(); else if (output instanceof Stream) os = (Stream) output; else if (Stream.isStreamable(output)) os = Types.toStream(output); else os = null;
        if (is != null && os != null) {
            Context.getCurrent().setInput(is);
            Context.getCurrent().setOutput(os);
            interactive("\n? ");
        } else if (input != null && output != null) {
            try {
                if (WEB_PROCESS == null) {
                    Class servlet = Class.forName("org.regadou.nalasys.system.Servlet");
                    Class[] params = new Class[] { Class.forName("javax.servlet.http.HttpServletRequest"), Class.forName("javax.servlet.http.HttpServletResponse") };
                    WEB_PROCESS = servlet.getMethod("process", params);
                }
                WEB_PROCESS.invoke(null, new Object[] { input, output });
            } catch (Exception e) {
                throw new RuntimeException("Exception while executing process(" + input + "," + output + ")", e);
            }
        } else throw new RuntimeException("Cannot execute process(" + input + "," + output + ")");
    }

    public static String read(Object src) {
        Stream s;
        if (src == null || src.equals("")) s = Context.getCurrent().getInput(); else {
            s = Types.toStream(src);
            if (s == null) return null;
        }
        try {
            return s.read();
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static void set(Object obj, Object prop, Object val) {
        if (obj != null && prop != null) DataFactory.getInstance(obj).setProperty(DataFactory.getInstance(prop), DataFactory.getInstance(val));
    }

    public static boolean write(Object obj, Object dst) {
        if (obj == null || obj.equals("")) return false;
        try {
            if (dst instanceof Address) {
                ((Address) dst).setContent(obj);
                return true;
            }
            Data data = DataFactory.newInstance(dst, Address.class);
            if (data instanceof Address) {
                ((Address) data).setContent(obj);
                return true;
            } else return false;
        } catch (Exception e) {
            return false;
        }
    }
}
