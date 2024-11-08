package org.jellylab.ojoverse.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jellylab.ojoverse.DataObject;

public class RequestParser {

    public boolean isMultipartRequest(HttpServletRequest request) {
        return ServletFileUpload.isMultipartContent(request);
    }

    public RequestWrapper getParameters(HttpServletRequest request) {
        RequestWrapper params;
        if (isMultipartRequest(request)) {
            params = loadMultipartParameters(request);
        } else {
            params = loadParameters(request);
        }
        return params;
    }

    public RequestWrapper loadParameters(HttpServletRequest request) {
        RequestWrapper params = new RequestWrapper(10);
        Enumeration enume = request.getParameterNames();
        while (enume.hasMoreElements()) {
            String name = (String) enume.nextElement();
            String value = request.getParameter(name);
            params.add(name, value);
        }
        return params;
    }

    public RequestWrapper loadMultipartParameters(HttpServletRequest request) {
        final int _20MB = 20 * 1024 * 1024;
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(_20MB);
        RequestWrapper params = new RequestWrapper(10);
        try {
            List items = upload.parseRequest(request);
            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
                if (item.isFormField()) {
                    String name = item.getFieldName();
                    String value = item.getString();
                    params.add(name, value);
                } else {
                    String PARTIAL_PATH_SOLLECITO_FATTURA = "tmp";
                    String fotoPath = "C:";
                    String fieldName = item.getFieldName();
                    String fileName = item.getName();
                    String contentType = item.getContentType();
                    boolean isInMemory = item.isInMemory();
                    long sizeInBytes = item.getSize();
                    if (sizeInBytes > 0 && fieldName != null) {
                        File uploadedFile = new File(fotoPath + fileName);
                        try {
                            InputStream istream = item.getInputStream();
                            if (uploadedFile.exists()) {
                                uploadedFile.renameTo(new File(fotoPath + "new" + fileName));
                            } else {
                                uploadedFile.createNewFile();
                            }
                            OutputStream out = new FileOutputStream(uploadedFile);
                            int read = 0;
                            byte[] bytes = new byte[1024];
                            while ((read = istream.read(bytes)) != -1) {
                                out.write(bytes, 0, read);
                            }
                            istream.close();
                            out.flush();
                            out.close();
                            item.write(uploadedFile);
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        } finally {
                        }
                    }
                }
            }
        } catch (FileUploadException fuexc) {
            fuexc.printStackTrace();
        }
        return params;
    }

    /**
     * Main method for loading request parameters into a DataObject (if field name equals to parameter name) or RequestMapper.params
     * @param request
     * @param data
     * @return
     */
    public RequestWrapper loadRequestMapper(HttpServletRequest request, DataObject data) {
        RequestWrapper mapper = new RequestWrapper(data);
        Enumeration enume = request.getParameterNames();
        Field[] dataFields = data.getDataFields();
        while (enume.hasMoreElements()) {
            String name = (String) enume.nextElement();
            String value = request.getParameter(name);
            boolean fieldMappingFound = false;
            for (int idf = 0; idf < dataFields.length; idf++) {
                if (dataFields[idf].getName().equals(name)) {
                    try {
                        dataFields[idf].set(data, value);
                        fieldMappingFound = true;
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            if (!fieldMappingFound) {
                mapper.add(name, value);
            }
        }
        return mapper;
    }

    public static DataObject loadDAOFromRequest(HttpServletRequest request, DataObject data) {
        Enumeration enume = request.getParameterNames();
        Field[] dataFields = data.getDataFields();
        int errMaxSize = 10;
        String[][] errors = new String[errMaxSize][2];
        int errCount = 0;
        while (enume.hasMoreElements()) {
            String name = (String) enume.nextElement();
            String value = request.getParameter(name);
            for (int idf = 0; idf < dataFields.length; idf++) {
                Field field = dataFields[idf];
                if (field.getName().equals(name)) {
                    try {
                        field.setAccessible(true);
                        String fieldTypeName = field.getType().getName();
                        if (fieldTypeName.equals(String.class.getName())) {
                            field.set(data, value);
                        } else if (fieldTypeName.equals("int")) {
                            int intValue = new Integer(value).intValue();
                            field.setInt(data, intValue);
                        } else if (fieldTypeName.equals("double")) {
                            double doubleValue = new Double(value).doubleValue();
                            field.setDouble(data, doubleValue);
                        }
                    } catch (Exception exc) {
                        if (errCount < errMaxSize) {
                            errors[errCount][0] = name;
                            errors[errCount][1] = value;
                        }
                        errCount++;
                    }
                }
            }
        }
        if (errCount > 0) {
            request.setAttribute("loadError", errors);
        }
        return data;
    }

    public static String treatLoadErrors(String[][] errors) {
        return treatLoadErrors(errors, "formData");
    }

    public static String treatLoadErrors(String[][] errors, String formName) {
        if (errors == null) {
            return "";
        }
        String ret = "";
        int errCount = 0;
        for (int ider = 0; ider < errors.length; ider++) {
            String name = errors[ider][0];
            String value = errors[ider][1];
            if (name != null && value != null) {
                if (errCount++ > 0) {
                    ret += ", ";
                }
                ret += "'" + name + "':'" + value + "'";
            }
        }
        if (ret.length() > 0) {
            ret = "{" + ret + "}";
        }
        return ret;
    }
}
