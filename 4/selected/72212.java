package net.oschina.action;

import java.io.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import net.oschina.beans.File;
import net.oschina.beans.User;
import net.oschina.service.StorageService;
import net.oschina.toolbox.LinkTool;
import my.mvc.Annotation;
import my.mvc.RequestContext;
import my.util.Multimedia;

/**
 * 文件库
 * @author Winter Lau
 * @date 2010-7-4 下午01:35:12
 */
public class FileAction {

    public static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    /**
	 * 文件下载
	 * @param ctx
	 * @throws java.io.IOException
	 */
    public void download(RequestContext ctx) throws IOException {
        if (ctx.isRobot()) {
            ctx.forbidden();
            return;
        }
        long id = ctx.id();
        File bean = File.INSTANCE.Get(id);
        if (bean == null) {
            ctx.not_found();
            return;
        }
        String f_ident = ctx.param("fn", "");
        if (id >= 100 && !StringUtils.equals(f_ident, bean.getIdent())) {
            ctx.not_found();
            return;
        }
        if (bean.IsLoginRequired() && ctx.user() == null) {
            StringBuilder login = new StringBuilder(LinkTool.home("home/login?goto_page="));
            ctx.redirect(login.toString());
            return;
        }
        FileInputStream fis = null;
        try {
            java.io.File file = StorageService.FILES.readFile(bean.getPath());
            fis = new FileInputStream(file);
            ctx.response().setContentLength((int) file.length());
            String ext = FilenameUtils.getExtension(bean.getPath());
            String mine_type = Multimedia.mime_types.get(ext);
            if (mine_type != null) ctx.response().setContentType(mine_type);
            String ua = ctx.header("user-agent");
            if (ua != null && ua.indexOf("Firefox") >= 0) ctx.header("Content-Disposition", "attachment; filename*=\"utf8''" + LinkTool.encode_url(bean.getName()) + "." + ext + "\""); else ctx.header("Content-Disposition", "attachment; filename=" + LinkTool.encode_url(bean.getName() + "." + ext));
            IOUtils.copy(fis, ctx.response().getOutputStream());
            bean.IncDownloadCount(1);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    /**
	 * 文件上传
	 * @param ctx
	 * @throws java.io.IOException
	 */
    @Annotation.UserRoleRequired(role = User.ROLE_EDITOR)
    public void upload(RequestContext ctx) throws IOException {
        File form = ctx.form(File.class);
        if (StringUtils.isBlank(form.getName())) throw ctx.error("file_name_empty");
        java.io.File file = ctx.file("file");
        if (file == null) throw ctx.error("file_empty");
        if (!File.IsLegalFile(file.getName())) throw ctx.error("file_illegal");
        String the_path = form.getUrl();
        if (StringUtils.isNotBlank(the_path) && StorageService.FILES.exist(the_path)) throw ctx.error("file_exists", the_path);
        String uri = StringUtils.isBlank(the_path) ? StorageService.FILES.save(file) : StorageService.FILES.save(file, the_path);
        form.setSize(file.length());
        form.setUrl(uri);
        form.setPath(uri);
        form.setUser(ctx.user().getId());
        form.setDl_count(0);
        form.Save();
        throw ctx.error("file_upload_ok", null);
    }

    /**
	 * 文件修改
	 * @param ctx
	 * @throws java.io.IOException
	 */
    @Annotation.UserRoleRequired(role = User.ROLE_EDITOR)
    public void edit(RequestContext ctx) throws IOException {
        File form = ctx.form(File.class);
        if (StringUtils.isBlank(form.getName())) throw ctx.error("file_name_empty");
        File bean = File.INSTANCE.Get(form.getId());
        java.io.File pic = ctx.file("file");
        if (pic != null) {
            if (!File.IsLegalFile(pic.getName())) throw ctx.error("file_illegal");
            bean.setSize(pic.length());
            StorageService.FILES.save(pic, bean.getPath());
        }
        bean.setName(form.getName());
        bean.Update();
        throw ctx.error("file_edit_ok");
    }

    /**
	 * 文件删除
	 * @param ctx
	 * @throws java.io.IOException
	 */
    @Annotation.UserRoleRequired(role = User.ROLE_ADMIN)
    public void delete(RequestContext ctx) throws IOException {
        long id = ctx.id();
        File file = File.INSTANCE.Get(id);
        if (file != null) {
            StorageService.FILES.delete(file.getPath());
            file.Delete();
        }
    }
}
