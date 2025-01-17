package org.easyweb.jdocx;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.w3c.dom.Document;

public class DOCXTransformer {

    @SuppressWarnings("unchecked")
    public String transformDOCX(String srcFileName, Map map, String destFileName) {
        File file = new File(srcFileName);
        String fileName = file.getName();
        int pos = fileName.lastIndexOf(".");
        String ext = fileName.substring(pos + 1);
        if (ext.equalsIgnoreCase("xml")) {
            String templateStr = readDocXmlTemplate(srcFileName);
            String s = replaceContentWidthMap(templateStr, map);
            return s;
        } else if (ext.equalsIgnoreCase("docx")) {
            String templateStr = readDocxTemplate(srcFileName);
            String s = replaceContentWidthMap(templateStr, map);
            generateDocFile(srcFileName, s, destFileName);
        } else {
            System.out.println("error!template must be xml document or .docx word file!");
        }
        return null;
    }

    private String readDocXmlTemplate(String srcFileName) {
        String s = "";
        try {
            File file = new File(srcFileName);
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(reader);
            String str = null;
            while ((str = br.readLine()) != null) {
                s = s + str;
            }
            reader.close();
            br.close();
        } catch (FileNotFoundException e) {
            System.out.println("没有找到模板文件！");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            System.out.println("读取模板：不支持的字符编码！");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("模板IO错误！");
            e.printStackTrace();
        }
        return s;
    }

    private String readDocxTemplate(String srcFileName) {
        StringBuilder sb = new StringBuilder();
        try {
            ZipFile docxFile = new ZipFile(new File(srcFileName));
            ZipEntry documentXML = docxFile.getEntry("word/document.xml");
            InputStream documentXMLIS = docxFile.getInputStream(documentXML);
            InputStreamReader reader = new InputStreamReader(documentXMLIS, "UTF-8");
            BufferedReader br = new BufferedReader(reader);
            String str = null;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            documentXMLIS.close();
            reader.close();
            br.close();
        } catch (ZipException e) {
            System.out.println("zip错误！");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            System.out.println("不支持的编码！");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("读取docx模板文件错误！");
            e.printStackTrace();
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String replaceContentWidthMap(String templateStr, Map map) {
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Entry entry = (Entry) it.next();
            Object object = (Object) entry.getValue();
            Class<?> c = object.getClass();
            templateStr = replaceContentWithMap(entry, c, templateStr);
            if (c.getGenericSuperclass() != null) {
                Class<?> superClass = c.getSuperclass();
                templateStr = replaceContentWithMap(entry, superClass, templateStr);
            }
        }
        return templateStr;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private String replaceContentWithMap(Entry entry, Class<?> c, String templateStr) {
        Object object = (Object) entry.getValue();
        Method method[] = c.getDeclaredMethods();
        for (int i = 0; i < method.length; i++) {
            String methodName = method[i].getName();
            if (methodName.startsWith("get")) {
                String filedName = methodName.substring(3);
                String desStr = entry.getKey().toString() + "." + filedName.substring(0, 1).toLowerCase() + filedName.substring(1);
                Object o;
                String replaceStr = "";
                try {
                    o = method[i].invoke(object, new Object[] {});
                    if (o != null) {
                        replaceStr = o.toString();
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("反射执行方法，参数错误！");
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    System.out.println("反射执行方法，非法接入！");
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    System.out.println("反射执行方法错误！");
                    e.printStackTrace();
                }
                if (filedName.equals("ReplaceImageName")) {
                    templateStr = replaceImage(templateStr, replaceStr);
                } else {
                    templateStr = templateStr.replaceAll(desStr, replaceStr);
                }
            }
        }
        return templateStr;
    }

    private String replaceImage(String templateStr, String replaceStr) {
        org.dom4j.Document doc = null;
        try {
            doc = DocumentHelper.parseText(templateStr);
            Element root = doc.getRootElement();
            getAllElement(root, replaceStr);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return doc.asXML();
    }

    private void getAllElement(Element ele, String replaceStr) {
        List<Element> list = ele.elements();
        for (Element element : list) {
            if (element.getName().equals("binData")) {
                element.setText(replaceStr);
            }
            List<Element> list2 = element.elements();
            if (list2.size() > 0) {
                getAllElement(element, replaceStr);
            }
        }
    }

    private void generateDocFile(String srcFileName, String s, String destFileName) {
        try {
            ZipFile docxFile = new ZipFile(new File(srcFileName));
            ZipEntry documentXML = docxFile.getEntry("word/document.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            InputStream documentXMLIS1 = docxFile.getInputStream(documentXML);
            Document doc = dbf.newDocumentBuilder().parse(documentXMLIS1);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.transform(new DOMSource(doc), new StreamResult(baos));
            ZipOutputStream docxOutFile = new ZipOutputStream(new FileOutputStream(destFileName));
            Enumeration<ZipEntry> entriesIter = (Enumeration<ZipEntry>) docxFile.entries();
            while (entriesIter.hasMoreElements()) {
                ZipEntry entry = entriesIter.nextElement();
                if (entry.getName().equals("word/document.xml")) {
                    docxOutFile.putNextEntry(new ZipEntry(entry.getName()));
                    byte[] datas = s.getBytes("UTF-8");
                    docxOutFile.write(datas, 0, datas.length);
                    docxOutFile.closeEntry();
                } else if (entry.getName().equals("word/media/image1.png")) {
                    InputStream incoming = new FileInputStream("c:/aaa.jpg");
                    byte[] data = new byte[incoming.available()];
                    int readCount = incoming.read(data, 0, data.length);
                    docxOutFile.putNextEntry(new ZipEntry(entry.getName()));
                    docxOutFile.write(data, 0, readCount);
                    docxOutFile.closeEntry();
                } else {
                    InputStream incoming = docxFile.getInputStream(entry);
                    byte[] data = new byte[incoming.available()];
                    int readCount = incoming.read(data, 0, data.length);
                    docxOutFile.putNextEntry(new ZipEntry(entry.getName()));
                    docxOutFile.write(data, 0, readCount);
                    docxOutFile.closeEntry();
                }
            }
            docxOutFile.close();
        } catch (Exception e) {
        }
    }
}
