package com.nhncorp.usf.core.interceptor.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.nhncorp.lucy.web.WebConstants;
import com.nhncorp.lucy.web.helper.ServletHelper;
import com.nhncorp.lucy.web.interceptor.AbstractInterceptor;
import com.nhncorp.usf.core.config.AttributeInfo;
import com.nhncorp.usf.core.interceptor.impl.multipart.MultiPartRequestWrapper;
import com.nhncorp.usf.core.util.FileUtil;
import com.nhncorp.usf.core.xwork.ActionInvocationHelper;
import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.ValidationAware;

/**
 * @author Web Platform Development Team
 */
public class MultipartInterceptor extends AbstractInterceptor {

    Log log = LogFactory.getLog(MultipartInterceptor.class);

    private static final long serialVersionUID = -5083811735026237256L;

    private static final String UPLOAD_DIR = "uploadDir";

    private static final String MAX_SIZE = "uploadMaxSize";

    /**
     * @param actionInvocation the ActionInvocation
     * @return the interceptor result
     * @throws Exception for Exception
     */
    @SuppressWarnings("unchecked")
    public String intercept(ActionInvocation actionInvocation) throws Exception {
        HttpServletRequest request = ServletHelper.getRequest();
        if (!isMultipart(request)) {
            return actionInvocation.invoke();
        }
        String dir = AttributeInfo.getInstance().getAttribute(UPLOAD_DIR);
        String maxSize = AttributeInfo.getInstance().getAttribute(MAX_SIZE);
        Map actionInvocationDataMap = ActionInvocationHelper.getDataMap(actionInvocation);
        MultiPartRequestWrapper multiWrapper = new MultiPartRequestWrapper(request, dir, Integer.valueOf(maxSize));
        if (multiWrapper.hasErrors()) {
            Collection errors = multiWrapper.getErrors();
            for (Object obj : errors) {
                String result = translateError(obj);
                if (result != null) {
                    return result;
                }
            }
        }
        Enumeration<String> fileParameterNames = multiWrapper.getFileParameterNames();
        while (fileParameterNames != null && fileParameterNames.hasMoreElements()) {
            String inputName = fileParameterNames.nextElement();
            log.debug("file parameter name : " + inputName);
            String[] contentType = multiWrapper.getContentTypes(inputName);
            if (contentType != null && contentType.length > 0) {
                String[] fileNames = multiWrapper.getFileNames(inputName);
                if (isNonEmpty(fileNames)) {
                    File[] files = multiWrapper.getFiles(inputName);
                    if (files != null) {
                        Map[] fileItemArray = new HashMap[files.length];
                        for (int index = 0; index < files.length; index++) {
                            if (acceptFile(files[index], contentType[index], inputName, null, actionInvocation.getInvocationContext().getLocale())) {
                                File uploadedFile = files[index];
                                String saveFileName = saveFile(uploadedFile, dir + File.separatorChar + fileNames[index]);
                                Map<String, Object> fileItem = new HashMap<String, Object>();
                                fileItem.put("fileName", saveFileName);
                                fileItem.put("size", uploadedFile.length());
                                fileItem.put("path", saveFileName);
                                fileItemArray[index] = fileItem;
                            }
                        }
                        if (fileItemArray.length == 1) {
                            actionInvocationDataMap.put(inputName, fileItemArray[0]);
                        } else {
                            actionInvocationDataMap.put(inputName, fileItemArray);
                        }
                    }
                } else {
                    log.error("");
                }
            } else {
                log.error("");
            }
        }
        actionInvocationDataMap.putAll(multiWrapper.getParameterMap());
        return actionInvocation.invoke();
    }

    /**
     * Checks if is multipart.
     *
     * @param request the request
     * @return true, if is multipart
     */
    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.indexOf("multipart/form-data") != -1;
    }

    /**
     * Save file.
     *
     * @param file     the file
     * @param filePath the file path
     * @return the string
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private String saveFile(File file, String filePath) throws IOException {
        File destFile = FileUtil.getUniqueFile(new File(filePath));
        FileUtils.copyFile(file, destFile);
        return FilenameUtils.getName(destFile.getName());
    }

    /**
     * Accept file.
     *
     * @param file        the file
     * @param contentType the content type
     * @param inputName   the input name
     * @param validation  the validation
     * @param locale      the locale
     * @return true, if successful
     */
    protected boolean acceptFile(File file, String contentType, String inputName, ValidationAware validation, Locale locale) {
        return true;
    }

    /**
     * Checks if is non empty.
     *
     * @param objArray the obj array
     * @return true, if is non empty
     */
    private boolean isNonEmpty(Object objArray[]) {
        boolean result = false;
        if (objArray != null) {
            for (int index = 0; index < objArray.length && !result; index++) {
                if (objArray[index] != null) {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Translate error.
     *
     * @param error the error
     * @return the string
     */
    protected String translateError(Object error) {
        if (error instanceof String) {
            return handleErrorString((String) error);
        }
        return WebConstants.RESULT_FILEUPLOAD_FAILED;
    }

    /**
     * Handle error string.
     *
     * @param errorStr the error str
     * @return the string
     */
    protected String handleErrorString(String errorStr) {
        if (errorStr.contains("exceeds")) {
            return WebConstants.RESULT_FILESIZE_LIMIT_EXCEEDED;
        }
        return WebConstants.RESULT_FILEUPLOAD_FAILED;
    }
}
