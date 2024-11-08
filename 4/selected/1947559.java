package br.com.fc.service.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.UUID;

class CreateClient {

    private ResourceBundle bundle = ResourceBundle.getBundle("br.com.fc.service.core.template");

    static final String CLIENT_FILE_NAME = "cliente.zip";

    private Map<Class<?>, String> mapsFiles = new HashMap<Class<?>, String>();

    private List<Class<?>> listClassBean = new ArrayList<Class<?>>();

    private List<Class<?>> listClassException = new ArrayList<Class<?>>();

    private String getString(String key, Object... values) {
        return MessageFormat.format(bundle.getString(key), values);
    }

    private void createServiceClient(String nameService, String url) throws Exception {
        try {
            String saida = "";
            Class<?> classService = Class.forName(nameService);
            saida += getString("package", classService.getPackage().getName());
            saida += getString("import", getString("packageService") + getString("classClient"));
            saida += getString("import", getString("packageService") + getString("classException"));
            saida += "\n";
            saida += getString("class", classService.getSimpleName(), "", "");
            saida += getString("url", url);
            saida += getString("client", getString("classClient"));
            saida += getString("constructorClient", classService.getSimpleName(), getString("classClient"));
            for (Method method : classService.getDeclaredMethods()) {
                if (!method.isAccessible()) {
                    String agrsMethod = "";
                    String agrsCall = "";
                    String agrsType = "";
                    addClassException(method.getExceptionTypes());
                    for (int i = 0; i < method.getGenericParameterTypes().length; i++) {
                        String agrs = "arg" + i;
                        addClassBean(method.getGenericParameterTypes()[i]);
                        agrsMethod += method.getGenericParameterTypes()[i].toString().replace("class ", "");
                        agrsMethod += " " + agrs;
                        agrsCall += agrs;
                        agrsType += method.getParameterTypes()[i].getName().concat(".class");
                        if (i + 1 < method.getGenericParameterTypes().length) {
                            agrsMethod += ", ";
                            agrsCall += ", ";
                            agrsType += ", ";
                        }
                    }
                    String exceptions = "";
                    String catchs = "";
                    for (Class<?> type : method.getExceptionTypes()) {
                        exceptions += type.getName() + ", ";
                        if (Exception.class != type) {
                            catchs += getString("catch", type.getName());
                        }
                    }
                    addClassBean(method.getGenericReturnType());
                    saida += getString("service", method.getGenericReturnType().toString(), method.getName(), agrsMethod, exceptions, classService.getName(), agrsType, agrsCall, catchs);
                }
            }
            saida += "\n}";
            mapsFiles.put(classService, saida);
        } catch (ClassNotFoundException e) {
            throw new Exception("Servi�o " + nameService + " n�o encontrado.");
        }
    }

    private void createServiceBeans() throws Exception {
        for (int i = 0; i < listClassBean.size(); i++) {
            String saida = createServiceBean(listClassBean.get(i));
            mapsFiles.put(listClassBean.get(i), saida);
        }
    }

    private void createServiceExeceptions() throws Exception {
        for (Class<?> clazz : listClassException) {
            String saida = createServiceExeception(clazz);
            mapsFiles.put(clazz, saida);
        }
    }

    private String createSerialVersionUID(Class<?> clazz) throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(clazz);
        long svuid = 0L;
        if (osc == null || (svuid = osc.getSerialVersionUID()) == 0) {
            throw new Exception(clazz.getName() + " n�o implementa Serializable");
        }
        return getString("serialVersionUID", String.valueOf(svuid));
    }

    private String createServiceExeception(Class<?> clazz) throws Exception {
        String saida = "";
        saida += getString("package", clazz.getPackage().getName());
        saida += getString("import", getString("packageService") + getString("classException"));
        Class<?> superClass = clazz.getSuperclass();
        saida += getString("class", clazz.getSimpleName(), superClass != null ? "extends " + superClass.getName() : "", "");
        saida += createSerialVersionUID(clazz);
        saida += getString("constructorException", clazz.getSimpleName());
        saida += createFields(clazz) + "}\n";
        return saida;
    }

    private String createServiceBean(Class<?> clazz) throws Exception {
        String saida = "";
        saida += getString("package", clazz.getPackage().getName());
        saida += "\n";
        Class<?> superClass = clazz.getSuperclass();
        saida += getString("class", clazz.getSimpleName(), superClass != null && Object.class != superClass ? "extends " + superClass.getName() : "", "implements java.io.Serializable");
        saida += createSerialVersionUID(clazz);
        return saida + createFields(clazz) + "}\n";
    }

    private String createFields(Class<?> clazz) throws Exception {
        String saida = "";
        String methods = "";
        String fields = "";
        for (Field field : clazz.getDeclaredFields()) {
            if (!"serialVersionUID".equals(field.getName())) {
                if (!field.getDeclaringClass().isPrimitive()) {
                    addClassBean(field.getGenericType());
                }
                fields += getString("field", field.getGenericType().toString().replace("class ", "").trim(), field.getName());
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equalsIgnoreCase("get" + field.getName())) {
                        methods += getString("get", field.getGenericType().toString().replace("class ", "").trim(), method.getName(), field.getName());
                    } else if (method.getName().equalsIgnoreCase("set" + field.getName())) {
                        methods += getString("set", method.getName(), field.getGenericType().toString().replace("class ", "").trim(), field.getName());
                    }
                }
            }
        }
        saida += fields + "\n" + methods + "\n";
        return saida;
    }

    private void addClassException(Class<?>[] types) throws ClassNotFoundException {
        if (types == null) return;
        for (Class<?> clazz : types) {
            String path = clazz.getCanonicalName().replace(".", "/").concat(".class");
            URL urlClass = this.getClass().getClassLoader().getResource(path);
            URL urlSRC = this.getClass().getClassLoader().getResource("");
            if (urlClass.getPath().contains(urlSRC.getPath()) && !listClassException.contains(clazz)) {
                listClassException.add(clazz);
                if (!listClassException.contains(clazz.getGenericSuperclass())) {
                    addClassException(new Class<?>[] { clazz.getSuperclass() });
                }
            }
        }
    }

    private void addClassBean(Type type) throws ClassNotFoundException {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            for (Type typeArgument : typeArguments) {
                addClassBean(typeArgument);
            }
        } else {
            if (!type.toString().contains("class")) {
                return;
            }
            Class<?> clazz = Class.forName(type.toString().replace("class ", "").trim());
            String path = clazz.getCanonicalName().replace(".", "/").concat(".class");
            URL urlClass = this.getClass().getClassLoader().getResource(path);
            clazz.getComponentType();
            URL urlSRC = this.getClass().getClassLoader().getResource("");
            if (urlClass.getPath().contains(urlSRC.getPath()) && !listClassBean.contains(clazz)) {
                listClassBean.add(clazz);
                if (!listClassBean.contains(clazz.getGenericSuperclass())) {
                    addClassBean(clazz.getGenericSuperclass());
                }
            }
        }
    }

    byte[] createClient(String nameService, String url) throws Exception {
        try {
            createServiceClient(nameService, url);
            createServiceExeceptions();
            createServiceBeans();
            String guid = UUID.randomUUID().toString();
            String filePath = System.getProperty("java.io.tmpdir") + File.separator + guid;
            for (Entry<Class<?>, String> entry : mapsFiles.entrySet()) {
                File file = new File(filePath + File.separator + "classes" + File.separator + entry.getKey().getPackage().getName().replace('.', File.separatorChar));
                file.mkdirs();
                FileOutputStream out = new FileOutputStream(file.getPath() + File.separator + entry.getKey().getSimpleName() + ".java");
                PrintStream p = new PrintStream(out);
                p.write(entry.getValue().getBytes());
                p.flush();
                p.close();
                out.flush();
                out.close();
            }
            File file = new File(filePath, CLIENT_FILE_NAME);
            ServiceZipClient.zip(new File(filePath, CLIENT_FILE_NAME), new File(filePath, "classes"));
            byte[] bs = toByte(file);
            return bs;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    private byte[] toByte(File file) throws IOException {
        FileInputStream fin = null;
        FileChannel ch = null;
        try {
            fin = new FileInputStream(file);
            ch = fin.getChannel();
            int size = (int) ch.size();
            MappedByteBuffer buf = ch.map(MapMode.READ_ONLY, 0, size);
            byte[] bytes = new byte[size];
            buf.get(bytes);
            return bytes;
        } finally {
            if (fin != null) fin.close();
            if (ch != null) ch.close();
        }
    }
}
