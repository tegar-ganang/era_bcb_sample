package com.laoer.bbscs.fio.imp;

import com.laoer.bbscs.fio.ForumUploadFile;
import com.laoer.bbscs.bean.Forum;
import com.laoer.bbscs.fio.UploadFile;
import java.io.*;
import java.util.List;
import com.laoer.bbscs.service.config.ForumConfig;
import com.laoer.bbscs.service.config.SysConfig;
import org.apache.commons.io.*;
import com.laoer.bbscs.comm.*;

/**
 * <p>
 * Title: TianyiBBS
 * </p>
 *
 * <p>
 * Description: BBSCS
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2006
 * </p>
 *
 * <p>
 * Company: Laoer.com
 * </p>
 *
 * @author Laoer
 * @version 7.0
 */
public class ForumUploadFileImp implements ForumUploadFile {

    private ForumConfig forumConfig;

    public ForumConfig getForumConfig() {
        return forumConfig;
    }

    public void setForumConfig(ForumConfig forumConfig) {
        this.forumConfig = forumConfig;
    }

    public ForumUploadFileImp() {
    }

    /**
	 *
	 * @param toFileName
	 *            String
	 * @param uploadFile
	 *            UploadFile
	 * @param sysConfig
	 *            SysConfig
	 * @throws IOException
	 * @todo Implement this com.laoer.bbscs.fio.ForumUploadFile method
	 */
    public void saveUploadFile(String toFileName, UploadFile uploadFile, SysConfig sysConfig) throws IOException {
        OutputStream bos = new FileOutputStream(toFileName);
        IOUtils.copy(uploadFile.getInputStream(), bos);
        if (sysConfig.isAttachImg(uploadFile.getFileName()) && sysConfig.getReduceAttachImg() == 1) {
            ImgUtil.reduceImg(toFileName, toFileName + Constant.IMG_SMALL_FILEPREFIX, sysConfig.getReduceAttachImgSize(), sysConfig.getReduceAttachImgSize(), 1);
        }
    }

    /**
	 *
	 * @param fileName
	 *            String
	 * @throws IOException
	 * @todo Implement this com.laoer.bbscs.fio.ForumUploadFile method
	 */
    public void delUploadFile(Forum f) throws IOException {
        if (f.getHaveAttachFile() == 1 && f.getAttachFileName() != null && f.getAttachFileName().size() > 0) {
            List fl = f.getAttachFileName();
            String filePath = BBSCSUtil.getUpFilePath(f.getBoardID(), f.getPostTime());
            for (int i = 0; i < fl.size(); i++) {
                File attachFile = new File(filePath + (String) fl.get(i));
                if (attachFile.exists()) {
                    FileUtils.forceDelete(attachFile);
                }
                File attachFileSmall = new File(filePath + (String) fl.get(i) + Constant.IMG_SMALL_FILEPREFIX);
                if (attachFileSmall.exists()) {
                    FileUtils.forceDelete(attachFileSmall);
                }
            }
        }
    }

    /**
	 *
	 * @param f
	 *            Forum
	 * @param fileName
	 *            String
	 * @throws IOException
	 * @todo Implement this com.laoer.bbscs.fio.ForumUploadFile method
	 */
    public List delUploadFile(Forum f, List fileNames) throws IOException {
        List fl = f.getAttachFileName();
        String filePath = BBSCSUtil.getUpFilePath(f.getBoardID(), f.getPostTime());
        for (int i = 0; i < fileNames.size(); i++) {
            String fn = (String) fileNames.get(i);
            File attachFile = new File(filePath + fn);
            if (attachFile.exists()) {
                FileUtils.forceDelete(attachFile);
            }
            File attachFileSmall = new File(filePath + fn + Constant.IMG_SMALL_FILEPREFIX);
            if (attachFileSmall.exists()) {
                FileUtils.forceDelete(attachFileSmall);
            }
            fl.remove(fn);
        }
        return fl;
    }

    public void moveUploadFile(Forum forum, long tobid) throws IOException {
        List fileList = forum.getAttachFileName();
        for (int i = 0; i < fileList.size(); i++) {
            String filename = (String) fileList.get(i);
            File fromFile = new File(BBSCSUtil.getUpFilePath(forum.getBoardID(), forum.getPostTime()) + filename);
            File toFile = new File(BBSCSUtil.getUpFilePath(tobid, forum.getPostTime()) + filename);
            FileUtils.copyFile(fromFile, toFile);
            FileUtils.forceDelete(fromFile);
            File fromFileSmall = new File(BBSCSUtil.getUpFilePath(forum.getBoardID(), forum.getPostTime()) + filename + Constant.IMG_SMALL_FILEPREFIX);
            if (fromFileSmall.exists()) {
                File toFileSmall = new File(BBSCSUtil.getUpFilePath(tobid, forum.getPostTime()) + filename + Constant.IMG_SMALL_FILEPREFIX);
                FileUtils.copyFile(fromFileSmall, toFileSmall);
                FileUtils.forceDelete(fromFileSmall);
            }
        }
    }

    public void delDetailFile(Forum forum) throws IOException {
        File postFile = new File(this.getForumConfig().getForumPath(forum.getBoardID(), forum.getPostTime()) + forum.getDetail());
        if (postFile.exists()) {
            FileUtils.forceDelete(postFile);
        }
    }
}
