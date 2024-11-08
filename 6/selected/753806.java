package ces.research.oa.document.mail.util;

import java.util.List;
import java.util.TimerTask;
import javax.servlet.ServletContext;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.context.ApplicationContext;
import ces.arch.bo.IBo;
import ces.research.oa.document.mail.upload.UploadConfig;
import ces.research.oa.document.util.ApplicationContextFactory;
import ces.research.oa.document.util.DateUtil;
import ces.research.oa.entity.MailAffixPojo;

public class MailAffixCleanTask extends TimerTask {

    private UploadConfig config;

    public MailAffixCleanTask() {
        config = new UploadConfig();
    }

    ApplicationContext appContext = ApplicationContextFactory.getContext();

    IBo bo = (IBo) (appContext.getBean("DefaultBo"));

    String date = null;

    int size = 1024 * 1024 * 600;

    public void run() {
        date = DateUtil.addMonth(-1);
        List list = bo.getDao().getHibernateTemplate().find("from MailAffixPojo where upload_time <'" + date + "' and to_number(sized) >" + size);
        if (null != list && list.size() > 0) {
            try {
                FTPClient ftp = new FTPClient();
                ftp.connect(config.getHostUrl(), config.getFtpPort());
                ftp.login(config.getUname(), config.getUpass());
                int replyCode = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(replyCode)) {
                    ftp.disconnect();
                    return;
                }
                for (int i = 0; i < list.size(); i++) {
                    MailAffixPojo pojo = (MailAffixPojo) list.get(i);
                    ftp.changeWorkingDirectory(pojo.getUploadTime().substring(0, 7));
                    ftp.deleteFile(pojo.getAffixSaveName());
                    ftp.changeToParentDirectory();
                    bo.delete(MailAffixPojo.class, new Long(pojo.getId()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
