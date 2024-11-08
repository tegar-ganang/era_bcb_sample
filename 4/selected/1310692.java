package hu.sztaki.lpds.pgportal.portlets.compiler;

import hu.sztaki.lpds.pgportal.services.compiler.Compiler;
import hu.sztaki.lpds.pgportal.services.compiler.CompilerService;
import hu.sztaki.lpds.pgportal.services.compiler.CompilerOption;
import org.gridsphere.portlet.service.PortletServiceException;
import org.gridsphere.provider.portlet.jsr.ActionPortlet;
import org.gridsphere.provider.event.jsr.RenderFormEvent;
import org.gridsphere.provider.event.jsr.ActionFormEvent;
import org.gridsphere.provider.portletui.beans.*;
import org.apache.oro.text.perl.Perl5Util;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.portlet.*;
import javax.activation.MimetypesFileTypeMap;
import org.apache.commons.lang.StringEscapeUtils;
import hu.sztaki.lpds.pgportal.services.utils.PropertyLoader;

/**
 *
 * @author based on <a href="mailto:tkucz@icis.pcz.pl">Tomasz Kuczynski</a>'s work
 * @author <a href="mailto:laszlo.radics@gmail.com">Radics Laszlo</a>
 * @version 2007-07-23
 *
 */
public class CompilerPortlet extends ActionPortlet {

    public static final String VIEW_JSP = "compiler/explorer.jsp";

    public static final String MYEDIT_JSP = "compiler/editfile.jsp";

    public static final String CONFIGURE_JSP = "compiler/configure.jsp";

    public static final String HELP_JSP = "compiler/help.jsp";

    public static String rootDir = "/tmp/compiler";

    public static Map<String, Compiler> compilers = new HashMap<String, Compiler>();

    private Perl5Util util = new Perl5Util();

    private CompilerService compilerService = null;

    public void init(javax.portlet.PortletConfig portletConfig) throws javax.portlet.PortletException {
        super.init(portletConfig);
        log.debug("Init called");
        DEFAULT_VIEW_PAGE = "ExplorerView";
        DEFAULT_EDIT_PAGE = "configureView";
        DEFAULT_HELP_PAGE = HELP_JSP;
        try {
            rootDir = PropertyLoader.getPrefixDir() + "/compiler";
        } catch (Exception e) {
        }
        try {
            compilerService = (CompilerService) createPortletService(CompilerService.class);
            compilers = compilerService.getCompilers();
        } catch (PortletServiceException e) {
            log.error("Unable to initialize CompilerService: " + e);
        }
        MyFile.populateMimeTypes();
        for (Compiler cc : compilers.values()) {
            String[] exts = cc.getExtensions().split(";");
            String mime = "text/" + cc.getName() + " ";
            for (int i = 0; i < exts.length; i++) {
                mime += " " + exts[i].substring(1);
            }
            MyFile.mt.addMimeTypes(mime);
            log.debug("add mime:" + mime);
        }
    }

    /********************** EXPLORER VIEW ***********************************/
    public void ExplorerView(RenderFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getRenderRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getRenderRequest().getPortletSession().getAttribute("userData");
        event.getRenderRequest().setAttribute("errorMessage", event.getRenderRequest().getParameter("errorMessage"));
        if (userData == null) {
            userData = new UserData();
            userData.setChrootDir(rootDir + File.separator + userName + File.separator);
            readDirectories(userName, userData);
            event.getRenderRequest().getPortletSession().setAttribute("userData", userData);
        }
        event.getRenderRequest().setAttribute("userData", userData);
        if (userData.getState().equals("explore")) {
            setNextState(event.getRenderRequest(), VIEW_JSP);
        } else {
            editView(event);
        }
    }

    public void changeDir(ActionFormEvent event) throws PortletException {
        String newDirParam = event.getAction().getParameter("newDir");
        String sideParam = event.getAction().getParameter("side");
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        String newDir = userData.getPath(sideParam);
        if (newDirParam.equals("..")) {
            newDir = util.substitute("s!/[^/]+/$!/!", newDir);
        } else {
            newDir += newDirParam + "/";
        }
        userData.setPath(sideParam, newDir);
        readDirectories(userName, userData);
    }

    public void uploadFile(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        try {
            HiddenFieldBean hf = event.getHiddenFieldBean("param");
            FileInputBean fi;
            String path;
            if (hf.getValue().equalsIgnoreCase("left")) {
                fi = event.getFileInputBean("userfileLeft");
                path = userData.getPath("left");
            } else {
                fi = event.getFileInputBean("userfileRight");
                path = userData.getPath("right");
            }
            String filename = fi.getFileName();
            filename = util.substitute("s!\\\\!/!g", filename);
            if (util.match("m!/([^/]+)$!", filename)) {
                filename = util.group(1);
            }
            File f = new File(userData.getChrootDir());
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            fi.saveFile(userData.getChrootDir() + path + filename);
            readDirectories(userName, userData);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            event.getActionResponse().setRenderParameter("errorMessage", "Upload_was_not_successful");
        }
    }

    public void newDirectory(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String sideParam = event.getActionRequest().getParameter("side");
        TextFieldBean textFieldBean = event.getTextFieldBean("resourceName" + sideParam);
        String resourceName = textFieldBean.getValue();
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        if (resourceName != null && !resourceName.equals("")) {
            String path = userData.getPath(sideParam);
            File f = new File(userData.getChrootDir() + path + resourceName);
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            textFieldBean.setValue("");
            readDirectories(userName, userData);
        }
    }

    public void newFile(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String sideParam = event.getActionRequest().getParameter("side");
        TextFieldBean textFieldBean = event.getTextFieldBean("resourceName" + sideParam);
        String resourceName = textFieldBean.getValue();
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        if (resourceName != null && !resourceName.equals("")) {
            try {
                String path = userData.getPath(sideParam);
                File f = new File(userData.getChrootDir() + path);
                if (!f.isDirectory()) {
                    f.mkdirs();
                }
                File newFile = new File(userData.getChrootDir() + path + resourceName);
                newFile.createNewFile();
                textFieldBean.setValue("");
                readDirectories(userName, userData);
            } catch (IOException e) {
                log.error("Unable to create new file", e);
                event.getActionResponse().setRenderParameter("errorMessage", "Unable to create newfile");
            }
        }
    }

    public void gotoRootDirLeft(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        userData.setPath("left", "/");
        readDirectories(userName, userData);
    }

    public void gotoRootDirRight(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        userData.setPath("right", "/");
        readDirectories(userName, userData);
    }

    public void copy(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String sideParam = event.getActionRequest().getParameter("side");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        if (userData.getPath("left").equals(userData.getPath("right"))) {
            event.getActionResponse().setRenderParameter("errorMessage", "Source and the destination is the same!");
            return;
        }
        String sourcePath = userData.getPath(sideParam);
        String destinationPath = (sideParam.equals("left") ? userData.getPath("right") : userData.getPath("left"));
        ArrayList<MyFile> resources = (sideParam.equals("left") ? userData.getLeftFileList() : userData.getRightFileList());
        try {
            Enumeration params = event.getActionRequest().getParameterNames();
            while (params.hasMoreElements()) {
                String param = (String) params.nextElement();
                if (util.match("m!^" + sideParam + "_(\\d)+$!", param)) {
                    File in = new File(userData.getChrootDir() + sourcePath + resources.get(Integer.parseInt(util.group(1))).getName());
                    File out = new File(userData.getChrootDir() + destinationPath + resources.get(Integer.parseInt(util.group(1))).getName());
                    FileInputStream fis = new FileInputStream(in);
                    FileOutputStream fos = new FileOutputStream(out);
                    byte[] buf = new byte[1024];
                    int i = 0;
                    while ((i = fis.read(buf)) != -1) {
                        fos.write(buf, 0, i);
                    }
                    fis.close();
                    fos.close();
                }
            }
            readDirectories(userName, userData);
        } catch (IOException e) {
            log.error("Unable to copy file(s)", e);
            event.getActionResponse().setRenderParameter("errorMessage", "Unable to copy file(s)");
        }
    }

    public void move(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String sideParam = event.getActionRequest().getParameter("side");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        if (userData.getPath("left").equals(userData.getPath("right"))) {
            event.getActionResponse().setRenderParameter("errorMessage", "Source and the destination is the same!");
            return;
        }
        String sourcePath = userData.getPath(sideParam);
        String destinationPath = (sideParam.equals("left") ? userData.getPath("right") : userData.getPath("left"));
        ArrayList<MyFile> resources = (sideParam.equals("left") ? userData.getLeftFileList() : userData.getRightFileList());
        Enumeration params = event.getActionRequest().getParameterNames();
        while (params.hasMoreElements()) {
            String param = (String) params.nextElement();
            if (util.match("m!^" + sideParam + "_(\\d)+$!", param)) {
                File f = new File(userData.getChrootDir() + sourcePath + resources.get(Integer.parseInt(util.group(1))).getName());
                f.renameTo(new File(userData.getChrootDir() + destinationPath + resources.get(Integer.parseInt(util.group(1))).getName()));
            }
        }
        readDirectories(userName, userData);
    }

    public void delete(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String sideParam = event.getActionRequest().getParameter("side");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        String path = userData.getPath(sideParam);
        ArrayList<MyFile> resources = (sideParam.equals("left") ? userData.getLeftFileList() : userData.getRightFileList());
        Enumeration params = event.getActionRequest().getParameterNames();
        while (params.hasMoreElements()) {
            String param = (String) params.nextElement();
            if (util.match("m!^" + sideParam + "_(\\d)+$!", param)) {
                File f = new File(userData.getChrootDir() + path + resources.get(Integer.parseInt(util.group(1))).getName());
                f.delete();
            }
        }
        readDirectories(userName, userData);
    }

    public void unzipFile(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String sideParam = event.getAction().getParameter("side");
        String fileNumberParam = event.getAction().getParameter("fileNumber");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        try {
            int fileNumber = Integer.parseInt(fileNumberParam);
            ArrayList<MyFile> resources = (sideParam.equals("left") ? userData.getLeftFileList() : userData.getRightFileList());
            String filename = userData.getChrootDir() + userData.getPath(sideParam) + resources.get(fileNumber).getName();
            FileInputStream fis = new FileInputStream(filename);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            BufferedOutputStream dest = null;
            final int BUFFER = 2048;
            while ((entry = zis.getNextEntry()) != null) {
                log.debug("Extracting: " + entry);
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(userData.getChrootDir() + userData.getPath(sideParam) + File.separator + entry.getName());
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
            readDirectories(userName, userData);
        } catch (NumberFormatException e) {
            log.error("Unable to parse fileNumberParam (" + fileNumberParam + ")", e);
        } catch (java.io.IOException e) {
            log.error("Unzip was unseccessfull");
            event.getActionResponse().setRenderParameter("errorMessage", "Unzip was unseccessfull");
        }
    }

    public void add2zip(String chroot, File file2zip, ZipOutputStream zos) throws IOException {
        final int BUFFER = 2048;
        String[] dirList;
        byte[] readBuffer = new byte[BUFFER];
        int bytesIn = 0;
        if (file2zip.isDirectory()) {
            dirList = file2zip.list();
        } else {
            dirList = new String[1];
            dirList[0] = "";
        }
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(file2zip, dirList[i]);
            if (f.isDirectory()) {
                add2zip(chroot, f, zos);
            } else {
                FileInputStream fis = new FileInputStream(f);
                ZipEntry entry = new ZipEntry(f.getPath().substring(chroot.length()));
                zos.putNextEntry(entry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        }
    }

    public void doDownload(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        if (event.getAction().getParameter("filename") != null) {
            String sideParam = event.getAction().getParameter("side");
            String filename = event.getAction().getParameter("filename");
            String path = (sideParam.equals("left") ? userData.getPath("left") : userData.getPath("right"));
            this.setFileDownloadEvent(event.getActionRequest(), filename, userData.getChrootDir() + path, false);
        } else {
            String sideParam = event.getActionRequest().getParameter("side");
            String filename = event.getActionRequest().getParameter("filename");
            String path = userData.getPath(sideParam);
            ArrayList<MyFile> resources = (sideParam.equals("left") ? userData.getLeftFileList() : userData.getRightFileList());
            Random rand = new Random();
            FileOutputStream fos;
            File d = null;
            try {
                d = File.createTempFile("download", ".zip");
                fos = new FileOutputStream(d);
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
                Enumeration params = event.getActionRequest().getParameterNames();
                while (params.hasMoreElements()) {
                    String param = (String) params.nextElement();
                    if (util.match("m!^" + sideParam + "_(\\d)+$!", param)) {
                        File f = new File(userData.getChrootDir() + path + resources.get(Integer.parseInt(util.group(1))).getName());
                        add2zip(userData.getChrootDir(), f, zos);
                    }
                }
                zos.close();
                this.setFileDownloadEvent(event.getActionRequest(), d.getName(), d.getParent() + File.separator, true);
            } catch (Exception e) {
                log.error("Zip creation failed.", e);
                event.getActionResponse().setRenderParameter("errorMessage", "Zip creation failed.");
                if (d != null) {
                    d.delete();
                }
            }
        }
    }

    private void readDirectories(String userName, UserData userData) {
        File dir = new File(userData.getChrootDir());
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        ArrayList<MyFile> leftFiles = new ArrayList<MyFile>();
        ExtensionFilter filter;
        File[] files;
        String leftPath = userData.getPath("left");
        dir = new File(userData.getChrootDir() + leftPath + File.separator);
        if (dir.isDirectory() && dir.exists()) {
            if (!leftPath.equals("/")) {
                leftFiles.add(new MyFile(".."));
            }
            filter = new ExtensionFilter("");
            files = dir.listFiles(filter);
            for (int i = 0; i < files.length; i++) {
                leftFiles.add(new MyFile(files[i]));
            }
            filter = new ExtensionFilter(null, true);
            files = dir.listFiles(filter);
            for (int i = 0; i < files.length; i++) {
                leftFiles.add(new MyFile(files[i]));
            }
        } else {
            leftFiles = null;
        }
        ArrayList<MyFile> rightFiles = new ArrayList<MyFile>();
        String rightPath = userData.getPath("right");
        dir = new File(userData.getChrootDir() + rightPath + File.separator);
        if (dir.isDirectory() && dir.exists()) {
            filter = new ExtensionFilter("");
            files = dir.listFiles(filter);
            if (!rightPath.equals("/")) {
                rightFiles.add(new MyFile(".."));
            }
            for (int i = 0; i < files.length; i++) {
                rightFiles.add(new MyFile(files[i]));
            }
            filter = new ExtensionFilter(null, true);
            files = dir.listFiles(filter);
            for (int i = 0; i < files.length; i++) {
                rightFiles.add(new MyFile(files[i]));
            }
        } else {
            rightFiles = null;
        }
        userData.setLeftFileList(leftFiles);
        userData.setRightFileList(rightFiles);
    }

    /********************** CONFIGURE VIEW ***********************************/
    public void configureNew(ActionFormEvent event) throws PortletException {
    }

    public void configureEdit(ActionFormEvent event) throws PortletException {
        String selectedLanguage = event.getListBoxBean("language").getSelectedValue();
        if (selectedLanguage != null) if (!selectedLanguage.equals(new String(""))) {
            event.getActionResponse().setRenderParameter("language", selectedLanguage);
        }
    }

    public void configureDelete(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String selectedLanguage = event.getListBoxBean("language").getSelectedValue();
        Compiler c;
        if (selectedLanguage != null && compilers.get(selectedLanguage) != null) {
            c = compilers.get(selectedLanguage);
            compilerService.deleteCompiler(compilers.get(selectedLanguage));
            compilers.remove(selectedLanguage);
            log.info("Compiler DELETED:" + selectedLanguage);
        } else {
            log.error("Compilercouldn't be deleted" + selectedLanguage);
            event.getActionResponse().setRenderParameter("errorMessage", "Compiler couldn't be deleted");
        }
        MyFile.populateMimeTypes();
        for (Compiler cc : compilers.values()) {
            String[] exts = cc.getExtensions().split(";");
            String mime = "text/" + cc.getName() + " ";
            for (int i = 0; i < exts.length; i++) {
                mime += " " + exts[i].substring(1);
            }
            MyFile.mt.addMimeTypes(mime);
            log.debug("add mime:" + mime);
        }
    }

    public void configureSave(ActionFormEvent event) throws PortletException {
        String selectedLanguage = event.getActionRequest().getParameter("language");
        Compiler c = compilers.get(selectedLanguage);
        if (c == null) {
            c = new Compiler();
        }
        log.debug("save compiler:" + selectedLanguage);
        c.setName(event.getActionRequest().getParameter("langName"));
        c.setCompiler(event.getActionRequest().getParameter("runable"));
        c.setExtensions(event.getActionRequest().getParameter("extensions"));
        c.setOutputFilename(event.getActionRequest().getParameter("output_filename"));
        ArrayList<CompilerOption> cos = c.getOptions();
        for (int i = 0; i < cos.size(); i++) {
            CompilerOption co = cos.get(i);
            if ("checked".equals((String) event.getActionRequest().getParameter("optionEnableByDefault_" + String.valueOf(i)))) {
                co.setEnable_by_default(true);
            } else {
                co.setEnable_by_default(false);
            }
            if ("checked".equals(event.getActionRequest().getParameter("optionEnableMultiply_" + String.valueOf(i)))) {
                co.setMultiple(true);
            } else {
                co.setMultiple(false);
            }
            co.setName(event.getActionRequest().getParameter("optionName_" + String.valueOf(i)));
            co.setParam(event.getActionRequest().getParameter("optionParameter_" + String.valueOf(i)));
            co.setType(event.getActionRequest().getParameter("optionType_" + String.valueOf(i)));
            co.setTypeParam(event.getActionRequest().getParameter("optionTypeParameter_" + String.valueOf(i)));
        }
        c.setOptions(cos);
        if (!c.getName().equals(selectedLanguage)) {
            compilers.remove(selectedLanguage);
            compilers.put(c.getName(), c);
        }
        event.getActionResponse().setRenderParameter("language", c.getName());
        compilerService.saveCompiler(c);
        MyFile.populateMimeTypes();
        for (Compiler cc : compilers.values()) {
            String[] exts = cc.getExtensions().split(";");
            String mime = "text/" + cc.getName() + " ";
            for (int i = 0; i < exts.length; i++) {
                if (exts[i].length() > 1) {
                    mime += " " + exts[i].substring(1);
                }
            }
            MyFile.mt.addMimeTypes(mime);
            log.debug("add mime:" + mime);
        }
    }

    public void configureNewOption(ActionFormEvent event) throws PortletException {
        configureSave(event);
        String selectedLanguage = event.getActionRequest().getParameter("langName");
        Compiler c = compilers.get(selectedLanguage);
        if (c != null) {
            CompilerOption co = new CompilerOption();
            c.getOptions().add(co);
        }
    }

    public void configureDeleteOption(ActionFormEvent event) throws PortletException {
        configureSave(event);
        String selectedLanguage = event.getActionRequest().getParameter("language");
        Compiler c = compilers.get(selectedLanguage);
        if (c != null) {
            String[] targets = event.getActionRequest().getParameterValues("selectedOptions");
            if (targets != null) {
                for (int i = targets.length - 1; i >= 0; i--) {
                    try {
                        c.getOptions().remove(Integer.parseInt(targets[i]));
                    } catch (NumberFormatException e) {
                        log.error("Hack warning:", e);
                    }
                }
            }
            compilerService.saveCompiler(c);
        }
    }

    public void configureView(RenderFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getRenderRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getRenderRequest().getPortletSession().getAttribute("userData");
        String selectedLanguage = event.getRenderRequest().getParameter("language");
        Compiler compiler = compilers.get(selectedLanguage);
        if (compiler == null) {
            compiler = new Compiler();
        }
        ListBoxBean lbb = event.getListBoxBean("language");
        lbb.setSize(1);
        lbb.clear();
        lbb.setMultipleSelection(false);
        if (compilers != null && !compilers.isEmpty()) {
            Collection<Compiler> cs = compilers.values();
            int i = -1;
            for (Compiler c : cs) {
                ListBoxItemBean item = new ListBoxItemBean();
                item.setName(StringEscapeUtils.escapeHtml(c.getName()));
                item.setValue(StringEscapeUtils.escapeHtml(c.getName()));
                if (c.getName().equals(selectedLanguage)) {
                    item.setSelected(true);
                }
                lbb.addBean(item);
            }
        }
        event.getRenderRequest().setAttribute("userData", userData);
        event.getRenderRequest().setAttribute("compiler", compiler);
        event.getRenderRequest().setAttribute("compilationWasSuccess", event.getRenderRequest().getParameter("compilationWasSuccess"));
        event.getRenderRequest().setAttribute("errorMessage", event.getRenderRequest().getParameter("errorMessage"));
        setNextState(event.getRenderRequest(), CONFIGURE_JSP);
    }

    /********************** EDITOR VIEW ***********************************/
    public void editFile(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String sideParam = event.getAction().getParameter("side");
        String fileNumberParam = event.getAction().getParameter("fileNumber");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        try {
            int fileNumber = Integer.parseInt(fileNumberParam);
            ArrayList<MyFile> resources = (sideParam.equals("left") ? userData.getLeftFileList() : userData.getRightFileList());
            userData.setEditFile(userData.getPath(sideParam) + resources.get(fileNumber).getName());
            userData.setState("edit");
        } catch (NumberFormatException e) {
            log.error("Unable to parse fileNumberParam (" + fileNumberParam + ")", e);
        }
    }

    public void editView(RenderFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getRenderRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getRenderRequest().getPortletSession().getAttribute("userData");
        String selectedCompiler = (String) event.getRenderRequest().getParameter("language");
        StringBuffer out = new StringBuffer();
        try {
            ArrayList<Pair<String, String>> values = userData.getFilesCompilerProperties().get(userData.getEditFile());
            Compiler compiler = null;
            if (selectedCompiler != null) {
                compiler = compilers.get(selectedCompiler);
            } else if (values == null) {
                int dotPos = userData.getEditFile().lastIndexOf(".");
                String extension = "";
                if (dotPos > -1) {
                    extension = userData.getEditFile().substring(dotPos);
                }
                Collection<Compiler> cs = compilers.values();
                for (Compiler c : cs) {
                    if ((c.getExtensions().indexOf(extension) > -1) && compiler == null) {
                        compiler = c;
                    }
                }
            } else {
                compiler = compilers.get(values.get(0).getSecond());
            }
            if (values == null) {
                values = new ArrayList<Pair<String, String>>();
                values.add(new Pair<String, String>("compiler", compiler.getName()));
                for (CompilerOption co : compiler.getOptions()) {
                    if (co.isEnable_by_default()) {
                        if ("user".equals(co.getType())) {
                            values.add(new Pair<String, String>(co.getName(), co.getTypeParam()));
                        } else {
                            values.add(new Pair<String, String>(co.getName(), ""));
                        }
                    }
                }
                userData.getFilesCompilerProperties().put(userData.getEditFile(), values);
            }
            out.append("<table><tr><td>");
            out.append("<input type=\"hidden\" name=\"oldLanguage\" value=\"" + StringEscapeUtils.escapeHtml(compiler.getName()) + "\">");
            out.append("<select name=\"language\">");
            Collection<Compiler> cs = compilers.values();
            for (Compiler c : cs) {
                String selected = null;
                if (c.getName().equals(compiler.getName())) {
                    selected = "selected";
                } else {
                    selected = "";
                }
                out.append("<option value=\"" + StringEscapeUtils.escapeHtml(c.getName()) + "\" " + selected + ">" + StringEscapeUtils.escapeHtml(c.getName()) + "</option>");
            }
            out.append("</select>");
            out.append("<input class=\"portlet-form-button\" type=\"submit\" name=\"gs_action=editChangeCompiler\" value=\"" + "Change" + "\"><br/>");
            out.append("</td></tr>");
            int i = 1;
            ArrayList<CompilerOption> cos = compiler.getOptions();
            Iterator<CompilerOption> coi = cos.iterator();
            CompilerOption co = null;
            if (coi.hasNext()) co = coi.next();
            while (i < values.size() || co != null) {
                String checked = "";
                String value = null;
                if (i < values.size() && (co.getName().equals(values.get(i).getFirst()))) {
                    checked = "checked";
                    value = values.get(i).getSecond();
                    i++;
                }
                out.append("<tr><td><label><input type=\"checkbox\" name=\"co_" + co.getName() + "\" value=\"checked\" " + checked + ">" + co.getName() + "</label></td>");
                if (co.getType().equals("simple")) {
                } else if (co.getType().equals("file") || co.getType().equals("directory")) {
                    String options = null;
                    ArrayList<String> fileList = new ArrayList<String>();
                    try {
                        File dir = new File(userData.getChrootDir());
                        ExtensionFilter filter = null;
                        if (co.getType().equals("file")) {
                            filter = new ExtensionFilter(co.getTypeParam());
                        } else {
                            filter = new ExtensionFilter("");
                        }
                        LinkedList<File> tmp = new LinkedList<File>();
                        tmp.addAll(Arrays.asList(dir.listFiles(filter)));
                        File f = null;
                        while (tmp.size() > 0) {
                            f = tmp.getFirst();
                            if (f.isDirectory()) {
                                tmp.addAll(Arrays.asList(f.listFiles(filter)));
                                if (co.getType().equals("directory")) {
                                    fileList.add(f.getCanonicalPath().substring(userData.getChrootDir().length()));
                                }
                            } else {
                                fileList.add(f.getCanonicalPath().substring(userData.getChrootDir().length()));
                            }
                            tmp.remove();
                        }
                    } catch (IOException ex) {
                        log.error("Error while processing dirs", ex);
                    }
                    String selected = "";
                    for (String name : fileList) {
                        if (name.equals(value)) {
                            selected = "selected";
                        } else {
                            selected = "";
                        }
                        options += "<option value=\"" + name + "\"" + selected + ">" + name + "</option>";
                    }
                    out.append("<td><select name=\"cov_" + co.getName() + "\">" + options + "</select></td>");
                } else if (co.getType().equals("user")) {
                    if (value == null) {
                        value = "";
                    }
                    out.append("<td><input name=\"cov_" + co.getName() + "\" value=\"" + value + "\"></td>");
                } else if (co.getType().equals("option")) {
                    String options = "";
                    String selected = "";
                    String[] opt = co.getTypeParam().split(";");
                    for (int j = 0; j < opt.length; j++) {
                        String name = opt[j].substring(0, opt[j].indexOf(":"));
                        if (String.valueOf(j).equals(value)) {
                            selected = "selected";
                        } else {
                            selected = "";
                        }
                        options += "<option value=\"" + j + "\" " + selected + ">" + name + "</option>";
                    }
                    out.append("<td><select name=\"cov_" + co.getName() + "\">" + options + "</select></td>");
                } else {
                    out.append("Error occured while processing arguments. No such type:" + co.getType());
                }
                String addMoreButton = (co.isMultiple() ? "<td><input class=\"portlet-form-button\" type=\"submit\" name=\"gs_action=editSave&amp;up=gay&amp;gay_co_addMore=" + co.getName() + "\" value=\"" + "Add more" + "\"></td>" : "");
                out.append(addMoreButton + "</tr>\n");
                if (i >= values.size() || !co.getName().equals(values.get(i).getFirst())) {
                    if (coi.hasNext()) {
                        co = coi.next();
                    } else {
                        co = null;
                    }
                }
            }
            out.append("</table><input class=\"portlet-form-button\" type=\"submit\" name=\"gs_action=editCompile\" value=\"" + "Compile" + "\">");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        String syntax = "c";
        if (userData.getEditFile() != null) {
            String ext = userData.getEditFile().substring(userData.getEditFile().lastIndexOf(".") + 1);
            if (ext != null && ext.length() > 0) {
                syntax = ext;
            }
        }
        event.getRenderRequest().setAttribute("editAreaProperties", "language: \"" + "en" + "\",\n" + "// " + userData.getEditFile() + "\n " + "// " + userData.getEditFile().substring(userData.getEditFile().lastIndexOf(".")) + "\n " + "syntax: \"" + syntax + "\"");
        if (event.getRenderRequest().getParameter("fileData") != null) {
            event.getRenderRequest().setAttribute("fileData", event.getRenderRequest().getParameter("fileData"));
        } else {
            File file = new File(userData.getChrootDir() + userData.getEditFile());
            try {
                FileReader fileReader = new FileReader(file);
                int numRead = 0;
                char[] buf = new char[4096];
                StringBuffer sb = new StringBuffer();
                while (!((numRead = fileReader.read(buf)) < 0)) {
                    sb.append(buf, 0, numRead);
                }
                event.getRenderRequest().setAttribute("fileData", sb.toString());
            } catch (java.io.FileNotFoundException e) {
                event.getRenderRequest().setAttribute("errorMessage", "File not found");
                log.error(e.getMessage(), e);
            } catch (java.io.IOException e) {
                log.error(e.getMessage(), e);
                event.getRenderRequest().setAttribute("errorMessage", "File read error");
            }
        }
        String errorMess = (String) event.getRenderRequest().getParameter("errorMessage");
        String compsucc = (String) event.getRenderRequest().getParameter("compilationWasSuccess");
        String compMess = (String) event.getRenderRequest().getParameter("compilerMessage");
        event.getRenderRequest().setAttribute("errorMessage", errorMess);
        event.getRenderRequest().setAttribute("compilationWasSuccess", compsucc);
        event.getRenderRequest().setAttribute("compilerMessage", compMess);
        event.getRenderRequest().setAttribute("userData", userData);
        event.getRenderRequest().setAttribute("compilers", compilers);
        event.getRenderRequest().setAttribute("compilerSettings", out.toString());
        setNextState(event.getRenderRequest(), MYEDIT_JSP);
    }

    public String replaceMacro(String s, UserData userData) {
        s = s.replaceAll("\\$filename2", (userData.getChrootDir() + userData.getEditFile().substring(0, userData.getEditFile().lastIndexOf("."))));
        s = s.replaceAll("\\$filename", (userData.getChrootDir() + userData.getEditFile()));
        return s;
    }

    public void editDownload(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        ArrayList<Pair<String, String>> ls = userData.getFilesCompilerProperties().get(userData.getEditFile());
        Compiler compiler = compilers.get(ls.get(0).getSecond());
        File f = new File(replaceMacro(compiler.getOutputFilename(), userData));
        System.out.println("file to download : " + f.getName() + "  -- parentdir : " + f.getParent());
        this.setFileDownloadEvent(event.getActionRequest(), f.getName(), f.getParent() + File.separator, false);
    }

    public void editChangeCompiler(ActionFormEvent event) throws PortletException {
    }

    public void editCompile(ActionFormEvent event) throws PortletException {
        editSave(event);
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        ArrayList<Pair<String, String>> ls = userData.getFilesCompilerProperties().get(userData.getEditFile());
        Compiler compiler = null;
        if (ls != null && !ls.isEmpty()) {
            compiler = compilers.get(ls.get(0).getSecond());
        }
        if (compiler == null) {
            log.error("No compiler found");
            event.getActionResponse().setRenderParameter("errorMessage", "No compiler found");
        }
        ArrayList<CompilerOption> cos = compiler.getOptions();
        String[] command = compiler.getCompiler().split(" ");
        ArrayList<String> commandLine = new ArrayList();
        for (int i = 0; i < command.length; i++) {
            commandLine.add(replaceMacro(command[i], userData));
        }
        if (!cos.isEmpty()) {
            Iterator<CompilerOption> coi = cos.iterator();
            CompilerOption co = coi.next();
            for (int i = 1; i < ls.size(); i++) {
                while (!ls.get(i).getFirst().equals(co.getName())) {
                    co = coi.next();
                }
                if (co.getType().equals("simple")) {
                    commandLine.add(replaceMacro(co.getParam(), userData));
                } else if (co.getType().equals("file") || co.getType().equals("directory")) {
                    commandLine.add(co.getParam() + userData.getChrootDir() + ls.get(i).getSecond());
                } else if (co.getType().equals("user")) {
                    commandLine.add(co.getParam() + ls.get(i).getSecond().replaceAll("\"", "\\\""));
                } else if (co.getType().equals("option")) {
                    String[] opt = co.getTypeParam().split(";");
                    String name = null;
                    try {
                        name = opt[Integer.valueOf(ls.get(i).getSecond())];
                    } catch (NumberFormatException e) {
                        name = opt[0];
                    }
                    name = name.substring(name.indexOf(":") + 1);
                    commandLine.add(co.getParam() + name);
                } else {
                    log.error("Error occured while processing arguments. No such type:" + co.getType());
                }
            }
        }
        StringBuffer commandString = new StringBuffer();
        for (int i = 0; i < commandLine.size(); i++) {
            commandString.append(commandLine.get(i));
            commandString.append(" ");
        }
        log.info("Command line=" + commandString.toString());
        String line, output = new String();
        output = "Commandline" + " : " + StringEscapeUtils.escapeHtml(commandString.toString()) + "<br/>\n";
        try {
            Process p = Runtime.getRuntime().exec(commandLine.toArray(new String[commandLine.size()]));
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = input.readLine()) != null) {
                log.debug("Output: " + line);
                output += line + "<br>";
            }
            while ((line = error.readLine()) != null) {
                log.debug("Error: " + line);
                output += "<font color=\"red\">" + StringEscapeUtils.escapeHtml(line) + "</font><br/>";
            }
            input.close();
            error.close();
            p.waitFor();
            log.debug("ReturnValue gcc:" + p.exitValue());
            if (p.exitValue() == 0) {
                output += "Sourcefile compilation successful";
                event.getActionResponse().setRenderParameter("compilationWasSuccess", "true");
                readDirectories(userName, userData);
            } else {
                event.getActionResponse().setRenderParameter("compilationWasSuccess", "false");
                event.getActionResponse().setRenderParameter("errorMessage", "Compilation failed");
            }
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (Exception e) {
            log.error("IOException", e);
            event.getActionResponse().setRenderParameter("errorMessage", "Compilation failed");
            event.getActionResponse().setRenderParameter("compilationWasSuccess", "false");
        }
        output = output.replaceAll(userData.getChrootDir(), "");
        event.getActionResponse().setRenderParameter("compilerMessage", output);
    }

    public void editSave(ActionFormEvent event) throws PortletException {
        String userName = (String) ((Map) event.getActionRequest().getAttribute(PortletRequest.USER_INFO)).get("user.name");
        String fileData = event.getActionRequest().getParameter("fileData");
        String fileName = event.getActionRequest().getParameter("fileName");
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        try {
            File fout = new File(userData.getChrootDir() + userData.getEditFile());
            File dir = new File(fout.getParent());
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
            File fout2 = new File(fout.getParent() + File.separator + fileName);
            FileOutputStream out = new FileOutputStream(fout2);
            out.write(fileData.getBytes());
            readDirectories(userName, userData);
            userData.getFilesCompilerProperties().put(fout2.getAbsolutePath(), userData.getFilesCompilerProperties().get(userData.getEditFile()));
            userData.setEditFile(fout2.getAbsolutePath().substring(userData.getChrootDir().length() - 1));
        } catch (IOException e) {
            log.error("Unable to save thefile", e);
            event.getActionResponse().setRenderParameter("errorMessage", "Unable to save thefile");
        }
        ArrayList<Pair<String, String>> ls = userData.getFilesCompilerProperties().get(userData.getEditFile());
        Compiler compiler;
        if (ls != null) {
            compiler = compilers.get(ls.get(0).getSecond());
        } else {
            compiler = compilers.get(event.getActionRequest().getParameter("language"));
            ls = new ArrayList<Pair<String, String>>();
        }
        if (compiler != null) {
            ArrayList<CompilerOption> cos = compiler.getOptions();
            String addMoreOption = event.getAction().getParameter("co_addMore");
            ls.clear();
            ls.add(new Pair<String, String>("compiler", event.getActionRequest().getParameter("language")));
            if (event.getActionRequest().getParameter("language").equals(event.getActionRequest().getParameter("oldLanguage"))) {
                for (CompilerOption co : cos) {
                    String[] options = event.getActionRequest().getParameterValues("co_" + co.getName());
                    String[] optionsValue = event.getActionRequest().getParameterValues("cov_" + co.getName());
                    if (co.getName().equals(addMoreOption)) {
                        ls.add(new Pair<String, String>(co.getName(), ""));
                    }
                    if (options != null) {
                        for (int i = 0; i < options.length; i++) {
                            if (co.getType().equals("simple")) {
                                ls.add(new Pair<String, String>(co.getName(), ""));
                            } else if (co.getType().equals("file") || co.getType().equals("directory")) {
                                ls.add(new Pair<String, String>(co.getName(), optionsValue[i]));
                            } else if (co.getType().equals("user")) {
                                ls.add(new Pair<String, String>(co.getName(), optionsValue[i]));
                            } else if (co.getType().equals("option")) {
                                ls.add(new Pair<String, String>(co.getName(), optionsValue[i]));
                            } else {
                                log.error("Error occured while processing arguments. No such type:" + co.getType());
                            }
                        }
                    }
                }
                userData.getFilesCompilerProperties().put(userData.getEditFile(), ls);
            }
        }
    }

    public void editCancel(ActionFormEvent event) throws PortletException {
        UserData userData = (UserData) event.getActionRequest().getPortletSession().getAttribute("userData");
        userData.setEditFile(null);
        userData.setState("explore");
    }
}
