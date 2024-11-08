import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class JarClassLoader extends ClassLoader {

    public static Hashtable<String, byte[]> s_Resources = new Hashtable<String, byte[]>();

    public static JarClassLoader s_Loader = new JarClassLoader();

    public static Class<?> load(byte[] resource) throws Exception {
        String mainClassName = "";
        ArrayList<String> classNames = new ArrayList<String>();
        ArrayList<byte[]> classBuffers = new ArrayList<byte[]>();
        HashMap<String, String> depends = new HashMap<String, String>();
        JarInputStream jar = new JarInputStream(new ByteArrayInputStream(resource));
        Manifest manifest = jar.getManifest();
        if (manifest != null) {
            mainClassName = manifest.getMainAttributes().getValue("Main-Class");
        }
        JarEntry entry;
        while ((entry = jar.getNextJarEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".class")) {
                String name = entry.getName().substring(0, entry.getName().length() - ".class".length()).replace('/', '.');
                byte[] data = getResourceData(jar);
                classNames.add(name);
                classBuffers.add(data);
            } else {
                if (entry.getName().charAt(0) == '/') {
                    s_Resources.put(entry.getName(), getResourceData(jar));
                } else {
                    s_Resources.put("/" + entry.getName(), getResourceData(jar));
                }
            }
        }
        while (classNames.size() > 0) {
            int n = classNames.size();
            for (int i = classNames.size() - 1; i >= 0; i--) {
                try {
                    s_Loader.defineClass((String) classNames.get(i), (byte[]) classBuffers.get(i), 0, ((byte[]) classBuffers.get(i)).length);
                    String pkName = (String) classNames.get(i);
                    if (pkName.lastIndexOf('.') >= 0) {
                        pkName = pkName.substring(0, pkName.lastIndexOf('.'));
                        if (s_Loader.getPackage(pkName) == null) {
                            s_Loader.definePackage(pkName, null, null, null, null, null, null, null);
                        }
                    }
                    classNames.remove(i);
                    classBuffers.remove(i);
                } catch (NoClassDefFoundError e) {
                    depends.put((String) classNames.get(i), e.getMessage().replaceAll("/", "."));
                } catch (UnsupportedClassVersionError e) {
                    throw new UnsupportedClassVersionError(classNames.get(i) + ", " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + ")");
                }
            }
            if (n == classNames.size()) {
                for (int i = 0; i < classNames.size(); i++) {
                    System.err.println("NoClassDefFoundError:" + classNames.get(i));
                    String className = (String) classNames.get(i);
                    while (depends.containsKey(className)) {
                        className = (String) depends.get(className);
                    }
                }
                break;
            }
        }
        try {
            Thread.currentThread().setContextClassLoader(s_Loader);
            return Class.forName(mainClassName, true, s_Loader);
        } catch (ClassNotFoundException e) {
            String className = mainClassName;
            while (depends.containsKey(className)) {
                className = (String) depends.get(className);
            }
            throw new ClassNotFoundException(className);
        }
    }

    /**
	 * 获得指定路径文件的byte[]形式
	 * @param name
	 * @return
	 */
    public static final byte[] getDataSource(String name) {
        FileInputStream fileInput;
        try {
            fileInput = new FileInputStream(new File(name));
        } catch (FileNotFoundException e) {
            fileInput = null;
        }
        BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
        return getDataSource(bufferedInput);
    }

    /**
	 * 获得指定InputStream的byte[]形式
	 * @param name
	 * @return
	 */
    public static final byte[] getDataSource(InputStream is) {
        if (is == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] arrayByte = null;
        try {
            byte[] bytes = new byte[8192];
            bytes = new byte[is.available()];
            int read;
            while ((read = is.read(bytes)) >= 0) {
                byteArrayOutputStream.write(bytes, 0, read);
            }
            arrayByte = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                    byteArrayOutputStream = null;
                }
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
            }
        }
        return arrayByte;
    }

    /**
	 * 获得指定JarInputStream的byte[]形式
	 * @param jar
	 * @return
	 * @throws IOException
	 */
    private static final byte[] getResourceData(JarInputStream jar) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int size;
        while (jar.available() > 0) {
            size = jar.read(buffer);
            if (size > 0) {
                data.write(buffer, 0, size);
            }
        }
        return data.toByteArray();
    }

    /**
	 * 重载的getResource,检查是否重复包含
	 */
    public URL getResource(String name) {
        if (s_Resources.containsKey("/" + name)) {
            try {
                return new URL("file:///" + name);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return super.getResource(name);
    }

    /**
	 * 执行指定class类
	 * @param clz
	 * @param methodName
	 * @param args
	 */
    public static void callVoidMethod(Class<?> clz, String methodName, String[] args) {
        Class<?>[] arg = new Class<?>[1];
        arg[0] = args.getClass();
        try {
            Method method = clz.getMethod(methodName, arg);
            Object[] inArg = new Object[1];
            inArg[0] = args;
            method.invoke(clz, inArg);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
	 * 重载的getResourceAsStream,检查是否重复包含
	 */
    public InputStream getResourceAsStream(String name) {
        if (name.charAt(0) == '/') {
            name = name.substring(1);
        }
        if (s_Resources.containsKey("/" + name)) {
            return new ByteArrayInputStream((byte[]) s_Resources.get("/" + name));
        }
        return super.getResourceAsStream(name);
    }
}

/**
 * <p>
 * Title: LoonFramework
 * </p>
 * <p>
 * Description:从外部启动jar包
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: LoonFramework
 * </p>
 * 
 * @author chenpeng
 * @email：ceponline@yahoo.com.cn
 * @version 0.1
 */
class JarTest {

    public static void main(String[] args) {
        byte[] resource = JarClassLoader.getDataSource("http://game.lordol.com/lordolyr/server_1_test/lordol.jar");
        try {
            Class clz = JarClassLoader.load(resource);
            JarClassLoader.callVoidMethod(clz, "main", new String[] { "" });
        } catch (Exception e) {
            e.getStackTrace();
        }
    }
}
