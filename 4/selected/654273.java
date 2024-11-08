package jacky.lanlan.song.extension.struts.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.log4j.Logger;

/**
 * General file manipulation utilities.
 * <p>
 * Facilities are provided in the following areas:
 * <ul>
 * <li>writing to a file
 * <li>reading from a file
 * <li>make a directory including parent directories
 * <li>copying files and directories
 * <li>deleting files and directories
 * <li>converting to and from a URL
 * <li>comparing file content
 * <li>file last changed date
 * <li>calculating a checksum
 * </ul>
 * <p>
 * Origin of code: Excalibur, Alexandria, Commons-Utils
 * 
 * @author <a href="mailto:burton@relativity.yi.org">Kevin A. Burton</A>
 * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph.Reck</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jefft@apache.org">Jeff Turner</a>
 * @author Matthew Hawthorne
 * @author <a href="mailto:jeremias@apache.org">Jeremias Maerki</a>
 * @author Stephen Colebourne
 * @author Ian Springer
 * @author Chris Eldredge
 * @author Jim Harrington
 * @author Niall Pemberton
 * @author Sandy McArthur
 * @version $Id: FileUtils.java 507684 2007-02-14 20:38:25Z bayard $
 */
public abstract class FileUtils {

    private static final Logger logger = Logger.getLogger(FileUtils.class);

    /**
   * Reads the contents of a file into a byte array. The file is always closed.
   * 
   * @param file
   *          the file to read, must not be <code>null</code>
   * @return the file contents, never <code>null</code>
   * @throws IOException
   *           in case of an I/O error
   * @since Commons IO 1.1
   */
    public static byte[] readFileToByteArray(File file) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            return IOUtils.toByteArray(in);
        } finally {
            IOUtils.close(in);
        }
    }

    /**
	 * 递归得到指定文件夹下的指定文件，保留文件夹结构。
	 * @param dir 要遍历的文件夹
	 * @param map 匹配文件夹结构的Map，key=文件夹名，value=该文件夹下的文件列表
	 * @param fileSuffix 文件扩展名(格式：.xyz)，如果为null，则代表得到所有的文件
	 */
    public static void listFiles(File dir, Map<File, Collection<File>> map, String fileSuffix) {
        Assert.notNull(map, "文件夹映射Map不能为Null");
        Collection<File> fileSet = new ArrayList<File>();
        map.put(dir, fileSet);
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                listFiles(file, map, fileSuffix);
            } else if (file.isFile()) {
                if (fileSuffix != null && !file.getName().endsWith(fileSuffix)) {
                    continue;
                }
                fileSet.add(file);
            }
        }
    }

    /**
	 * 递归得到指定文件夹下的文件，不保留文件夹结构，仅仅是得到指定文件夹下的文件。
	 * @param dir 要遍历的文件夹
	 * @param files 指定文件夹下的文件列表
	 * @param fileSuffix 文件扩展名，如果为null，则代表得到所有的文件
	 */
    public static void listFiles(File dir, Collection<File> files, String fileSuffix) {
        Assert.notNull(files, "文件列表不能为Null");
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                listFiles(file, files, fileSuffix);
            } else if (file.isFile()) {
                if (fileSuffix != null && !file.getName().endsWith(fileSuffix)) {
                    continue;
                }
                files.add(file);
            }
        }
    }

    private static JarFile makeJar(String jarPath) {
        JarFile jar = null;
        try {
            jar = new JarFile(jarPath);
        } catch (IOException e) {
            logger.error("无法创建jar文件" + jarPath, e);
            throw new RuntimeException("无法创建jar文件" + jarPath, e);
        }
        return jar;
    }

    /**
	 * 遍历指定jar文件内的指定文件。
	 * @param jarPath 要遍历的jar文件路径
	 * @param fileSuffix 文件扩展名(格式：.xyz)，如果为null，则代表得到所有的文件
	 * @return 找到的文件结构映射，key=文件路径，value=该文件的内容
	 */
    public static Map<String, byte[]> listFiles(String jarPath, String fileSuffix) {
        JarFile jar = makeJar(jarPath);
        String fileName = null;
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = entries.nextElement();
            fileName = entry.getName();
            if (fileSuffix != null && !fileName.endsWith(fileSuffix)) {
                continue;
            }
            try {
                byte[] content = new byte[(int) entry.getSize()];
                jar.getInputStream(entry).read(content);
                map.put(entry.getName(), content);
            } catch (Exception e) {
                logger.error("提取 [" + fileName + "] 时出错", e);
            }
        }
        return map;
    }

    /**
	 * 从指定jar文件中装载类。
	 * <p style="color:red">
	 * 这个方法只是尽可能的装载Class，如果某个类无法装载，则可能失败。
	 * @param jarPath 指定的jar文件路径
	 * @param basePackName 要装载的类的包名，该方法将装载该包及其子包下面所有的类
	 * @return 符合条件的Class列表
	 */
    public static List<Class<?>> loadClassFromJar(String jarPath, String basePackName) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        Map<String, byte[]> map = listFiles(jarPath, ".class");
        StringBuilder name = new StringBuilder();
        for (String className : map.keySet()) {
            name.delete(0, name.length());
            name.append(className.replace('/', '.')).delete(name.length() - 6, name.length());
            if (name.toString().startsWith(basePackName)) {
                try {
                    classes.add(Class.forName(name.toString()));
                } catch (Exception e) {
                    logger.error("载入类 [" + name + "] 时出错", e);
                    throw new RuntimeException("载入类 [" + name + "] 时出错", e);
                }
            }
        }
        return classes;
    }

    /**
	 * 递归得到指定文件夹下的子文件夹，不保留文件夹结构，仅仅是得到指定文件夹下的子文件夹。
	 * @param dir 要遍历的文件夹
	 * @param subDirs 指定文件夹下的子文件夹列表
	 */
    public static void listDirs(File dir, Collection<File> subDirs) {
        Assert.notNull(subDirs, "子文件夹列表不能为Null");
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                subDirs.add(file);
                listDirs(file, subDirs);
            }
        }
    }

    /**
	 * 递归得到指定文件夹下的所有文件夹，保留子文件夹结构。
	 * @param dir 要遍历的文件夹
	 * @param subDirs 匹配文件夹结构的Map，key=文件夹，value=该文件夹下的子文件夹列表
	 */
    public static void listDirs(File dir, Map<File, Collection<File>> subDirs) {
        Assert.notNull(subDirs, "子文件夹列表不能为Null");
        List<File> fileSet = new ArrayList<File>();
        subDirs.put(dir, fileSet);
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                fileSet.add(file);
                listDirs(file, subDirs);
            }
        }
    }

    /**
	 * 把数据写入磁盘。写入过程中发生的任何异常都将包装为RuntimeException抛出。
	 * @param data 要写入磁盘的数据
	 * @param path 目标路径
	 * @param name 保存的文件名
	 * @param suffix 文件后缀
	 */
    public static void writeDataToDisk(final byte[] data, String path, String name, String suffix) {
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        final String fullPath = path + name + suffix;
        handleFileWrite(fullPath, new Handler<FileChannel>() {

            public void doWith(FileChannel fc) {
                ByteBuffer bb = ByteBuffer.wrap(data);
                try {
                    fc.write(bb);
                } catch (IOException e) {
                    logger.error("写入 [" + fullPath + "] 时出错:", e);
                    throw new RuntimeException("写入 [" + fullPath + "] 时出错:" + e);
                }
            }
        });
    }

    /**
	 * 从磁盘指定位置读取文件。读取过程中发生的任何异常都将包装为RuntimeException抛出。
	 * @param path 目标文件路径
	 * @param name 目标文件名
	 * @param suffix 目标文件后缀名
	 * @return 文件的二进制内容数组
	 */
    public static byte[] readFileFromDisk(String path, String name, String suffix) {
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        final String fullPath = path + name + suffix;
        byte[] fileContent = null;
        fileContent = handleFileRead(fullPath, new ReturnableHandler<FileChannel, byte[]>() {

            public byte[] doWith(FileChannel fc) {
                try {
                    ByteBuffer bb = ByteBuffer.allocate((int) fc.size() + 1);
                    fc.read(bb);
                    byte[] content = new byte[bb.flip().limit()];
                    bb.get(content);
                    return content;
                } catch (IOException e) {
                    logger.error("读取 [" + fullPath + "] 时出错:", e);
                    throw new RuntimeException("读取 [" + fullPath + "] 时出错:" + e);
                }
            }
        });
        return fileContent;
    }

    /**
	 * 关闭文件通道。
	 */
    public static void closeFileChannel(FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("关闭文件通道时出错", e);
            }
        }
    }

    /**
	 * 使用类似Ruby处理文件的方式来处理文件读操作。
	 * <p>
	 * 调用者只需要提供文件路径和handler即可，方法会自动处理资源释放工作。
	 * @param <T> 需要从文件中得到的数据的类型
	 * @param path 文件路径，支持 file: classpath: 前缀
	 * @param handler 处理文件的闭包，根据传入的文件通道，返回需要的数据
	 * @return 需要的返回值
	 */
    public static <T> T handleFileRead(String path, ReturnableHandler<FileChannel, T> handler) {
        FileInputStream is = null;
        FileChannel channel = null;
        T reValue = null;
        try {
            File file = ResourceUtils.getFile(path);
            Assert.isTrue(file.exists(), "文件不存在");
            is = new FileInputStream(file);
            channel = is.getChannel();
            reValue = handler.doWith(channel);
        } catch (FileNotFoundException e) {
            logger.error("不能打开文件 " + path, e);
            throw new RuntimeException("不能打开文件 " + path, e);
        } finally {
            closeFileChannel(channel);
            IOUtils.close(is);
        }
        return reValue;
    }

    /**
	 * 使用类似Ruby处理文件的方式来处理文件写操作。
	 * <p>
	 * 调用者只需要提供文件路径和handler即可，方法会自动处理资源释放工作。
	 * @param <T> 需要从文件中得到的数据的类型
	 * @param path 文件路径，支持 file: classpath: 前缀
	 * @param handler 处理文件的闭包，根据传入的文件通道，处理文件的写入
	 */
    public static void handleFileWrite(String path, Handler<FileChannel> handler) {
        FileOutputStream os = null;
        FileChannel channel = null;
        try {
            File file = ResourceUtils.getFile(path);
            os = new FileOutputStream(file);
            channel = os.getChannel();
            handler.doWith(channel);
        } catch (FileNotFoundException e) {
            logger.error("不能创建文件 " + path, e);
            throw new RuntimeException("不能创建文件 " + path, e);
        } finally {
            closeFileChannel(channel);
            IOUtils.close(os);
        }
    }
}
