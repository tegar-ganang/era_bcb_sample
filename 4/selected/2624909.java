package org.jmantis.server;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * 启动服务器
 * @author zuoge85@gmail.com
 *
 */
public class App {
	private static final Logger log = LoggerFactory.getLogger(App.class);
	
	public static final String PACK_FILE_SUFFIX=".jar";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File apps=new File("apps");
		File[] fs=apps.listFiles(new FileFilter() {
			//@Override
			public boolean accept(File p) {
				return p.isFile()&&p.getName().toLowerCase().endsWith(PACK_FILE_SUFFIX);
			}
		});
		for(File f:fs){
			if(isDecompress(apps,f)){
				//解压
				decompress(apps,f);
			}
		}
		File[] appsDirs=apps.listFiles(new FileFilter() {
			//@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		});
		for(File dir:appsDirs){
			try {
				startApp(dir);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(Arrays.toString(fs));
	}
	public static URL[] getClassPath(File dir) throws MalformedURLException{
		List<URL> list=Lists.newArrayList();
		list.add(new File(dir,"classes").toURI().toURL());
		File libDir=new File(dir,"lib");
		File[] libs=libDir.listFiles();
		for(File lib:libs){
			if(lib.isFile()&&lib.getName().endsWith(PACK_FILE_SUFFIX)){
				list.add(lib.toURI().toURL());
				log.debug("需要包:{}",lib.getAbsolutePath());
			}
		}
		return list.toArray(new URL[list.size()]);
	}
	public static void startApp(File dir) throws MalformedURLException, ClassNotFoundException{
		log.info("准备开始启动应用:"+dir.getName());
		final Runtime runtime = Runtime.getRuntime();
		long freeMem = runtime.freeMemory();
		log.info("开始启动:[JVM freeMem:{}MB ; JVM maxMemory:{}MB ; JVM totalMemory:{}MB ]", 
				new Object[]{freeMem/1024f/1024f,runtime.maxMemory()/1024f/1024f,runtime.totalMemory()/1024f/1024f});
		URLClassLoader loader=new URLClassLoader(getClassPath(dir)){
			@Override
			protected Class<?> findClass(String name)
					throws ClassNotFoundException {
				Class<?> cls=super.findClass(name);
				//System.out.println(name+":"+cls.getResource("").getPath());
				return cls;
			}
		};
		Class<?> cls=loader.loadClass("vlan.webgame.manage.Bootstrap");
		System.out.println(cls.getResource(""));
		
		try {
			Object main=cls.newInstance();
			MethodUtils.invokeMethod(main, "main", new Object[]{new String[]{}});
			System.out.println("实例类"+main);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("开始准备关闭classloader:[JVM freeMem:{}MB ; JVM maxMemory:{}MB ; JVM totalMemory:{}MB ]", 
				new Object[]{freeMem/1024f/1024f,runtime.maxMemory()/1024f/1024f,runtime.totalMemory()/1024f/1024f});
		runtime.gc();
		/*** java6及以前版本不能关闭classloader
		try {
			loader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*****/
		log.info("成功关闭classloader:[JVM freeMem:{}MB ; JVM maxMemory:{}MB ; JVM totalMemory:{}MB ]", 
				new Object[]{freeMem/1024f/1024f,runtime.maxMemory()/1024f/1024f,runtime.totalMemory()/1024f/1024f});
		
	}
	public static void decompress(File apps,File f) throws IOException{
		String filename=f.getName();
		filename=filename.substring(0,filename.length()-PACK_FILE_SUFFIX.length());
		File dir=new File(apps,filename);
		if(!dir.exists()){
			dir.mkdirs();
		}
		if(dir.isDirectory()){
			JarFile jar=new JarFile(f);
			Enumeration<JarEntry> files=jar.entries();
			while(files.hasMoreElements()){
				JarEntry je=files.nextElement();
				if(je.isDirectory()){
					File item=new File(dir,je.getName());
					item.mkdirs();
				}else{
					File item=new File(dir,je.getName());
					item.getParentFile().mkdirs();
					InputStream input=jar.getInputStream(je);
					FileOutputStream out=new FileOutputStream(item);
					IOUtils.copy(input, out);
					input.close();
					out.close();
				}
				//System.out.println(je.isDirectory() + je.getName());
			}
		}
	}
	/**
	 * 是否需要解压
	 * @param f
	 * @return
	 */
	public static boolean isDecompress(File apps,File f){
		String filename=f.getName();
		filename=filename.substring(0,filename.length()-PACK_FILE_SUFFIX.length());
		File dir=new File(apps,filename);
		if(dir.isDirectory()){
			return false;
		}else{
			return true;
		}
	}
}
