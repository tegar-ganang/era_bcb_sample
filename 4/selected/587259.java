package egovframework.com.sym.sym.bak.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import egovframework.com.utl.sim.service.EgovFileTool;

/**
 * 백업작업을 실행하는 Quartz Job 클래스를 정의한다.
 *
 * @author 김진만
 * @see
 * <pre>
 * == 개정이력(Modification Information) ==
 *
 *   수정일       수정자           수정내용
 *  -------     --------    ---------------------------
 *  2010.09.06   김진만     최초 생성
 * </pre>
 */
public class BackupJob implements Job {

    /**
	 * logger
	 */
    private final Logger log = Logger.getLogger(this.getClass());

    /**
     * (non-Javadoc)
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        boolean result = false;
        JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
        if (log.isDebugEnabled()) {
            log.debug("job[" + jobContext.getJobDetail().getName() + "] " + "Trigger이름 : " + jobContext.getTrigger().getName());
            log.debug("job[" + jobContext.getJobDetail().getName() + "] " + "BackupOpert ID : " + dataMap.getString("backupOpertId"));
            log.debug("job[" + jobContext.getJobDetail().getName() + "] " + "백업원본디렉토리 : " + dataMap.getString("backupOrginlDrctry"));
            log.debug("job[" + jobContext.getJobDetail().getName() + "] " + "백업저장디렉토리 : " + dataMap.getString("backupStreDrctry"));
            log.debug("job[" + jobContext.getJobDetail().getName() + "] " + "압축구분 : " + dataMap.getString("cmprsSe"));
        }
        String backupOpertId = dataMap.getString("backupOpertId");
        String backupOrginlDrctry = dataMap.getString("backupOrginlDrctry");
        String backupStreDrctry = dataMap.getString("backupStreDrctry");
        String cmprsSe = dataMap.getString("cmprsSe");
        String backupFileNm = null;
        if ("01".equals(cmprsSe)) {
            backupFileNm = backupStreDrctry + File.separator + generateBackupFileNm(backupOpertId) + "." + "tar";
        } else if ("02".equals(cmprsSe)) {
            backupFileNm = backupStreDrctry + File.separator + generateBackupFileNm(backupOpertId) + "." + "zip";
        } else {
            log.error("압축구분값[" + cmprsSe + "]이 잘못지정되었습니다.");
            throw new JobExecutionException("압축구분값[" + cmprsSe + "]이 잘못지정되었습니다.");
        }
        log.debug("백업화일명 : " + backupFileNm);
        dataMap.put("backupFile", backupFileNm);
        if ("01".equals(cmprsSe)) {
            result = excuteBackup(backupOrginlDrctry, backupFileNm, ArchiveStreamFactory.TAR);
        } else {
            result = excuteBackup(backupOrginlDrctry, backupFileNm, ArchiveStreamFactory.ZIP);
        }
        jobContext.setResult(result);
    }

    /**
     * 백업화일명을 생성한다.
     * 백업화일명 : 백업작업ID_현재시각()
	 * @param  backupOpertId 백업작업ID
     * @return 백업화일명.
    */
    private String generateBackupFileNm(String backupOpertId) {
        String backupFileNm = null;
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        backupFileNm = backupOpertId + "_" + formatter.format(currentTime);
        return backupFileNm;
    }

    /**
	 * 디렉토리를 백업화일(tar,zip)으로 백업하는 기능
	 * @param backupOrginlDrctry 백업원본디렉토리명
	 * @param targetFileNm 백업파일명
	 * @param archiveFormat 저장포맷 (tar, zip)
	 * @return  result 백업성공여부 True / False
	*/
    public boolean excuteBackup(String backupOrginlDrctry, String targetFileNm, String archiveFormat) throws JobExecutionException {
        File targetFile = new File(targetFileNm);
        File srcFile = new File(backupOrginlDrctry);
        if (!srcFile.exists()) {
            log.error("백업원본디렉토리[" + srcFile.getAbsolutePath() + "]가 존재하지 않습니다.");
            throw new JobExecutionException("백업원본디렉토리[" + srcFile.getAbsolutePath() + "]가 존재하지 않습니다.");
        }
        if (srcFile.isFile()) {
            log.error("백업원본디렉토리[" + srcFile.getAbsolutePath() + "]가 파일입니다. 디렉토리명을 지정해야 합니다. ");
            throw new JobExecutionException("백업원본디렉토리[" + srcFile.getAbsolutePath() + "]가 파일입니다. 디렉토리명을 지정해야 합니다. ");
        }
        boolean result = false;
        FileInputStream finput = null;
        FileOutputStream fosOutput = null;
        ArchiveOutputStream aosOutput = null;
        ArchiveEntry entry = null;
        try {
            log.debug("charter set : " + Charset.defaultCharset().name());
            fosOutput = new FileOutputStream(targetFile);
            aosOutput = new ArchiveStreamFactory().createArchiveOutputStream(archiveFormat, fosOutput);
            if (ArchiveStreamFactory.TAR.equals(archiveFormat)) {
                ((TarArchiveOutputStream) aosOutput).setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            }
            File[] fileArr = srcFile.listFiles();
            ArrayList list = EgovFileTool.getSubFilesByAll(fileArr);
            for (int i = 0; i < list.size(); i++) {
                File sfile = new File((String) list.get(i));
                finput = new FileInputStream(sfile);
                if (ArchiveStreamFactory.TAR.equals(archiveFormat)) {
                    entry = new TarArchiveEntry(sfile, new String(sfile.getAbsolutePath().getBytes(Charset.defaultCharset().name()), "8859_1"));
                    ((TarArchiveEntry) entry).setSize(sfile.length());
                } else {
                    entry = new ZipArchiveEntry(sfile.getAbsolutePath());
                    ((ZipArchiveEntry) entry).setSize(sfile.length());
                }
                aosOutput.putArchiveEntry(entry);
                IOUtils.copy(finput, aosOutput);
                aosOutput.closeArchiveEntry();
                finput.close();
                result = true;
            }
            aosOutput.close();
        } catch (Exception e) {
            log.error("백업화일생성중 에러가 발생했습니다. 에러 : " + e.getMessage());
            log.debug(e);
            result = false;
            throw new JobExecutionException("백업화일생성중 에러가 발생했습니다.", e);
        } finally {
            try {
                if (finput != null) finput.close();
            } catch (Exception e2) {
                log.error("IGNORE:", e2);
            }
            try {
                if (aosOutput != null) aosOutput.close();
            } catch (Exception e2) {
                log.error("IGNORE:", e2);
            }
            try {
                if (fosOutput != null) fosOutput.close();
            } catch (Exception e2) {
                log.error("IGNORE:", e2);
            }
            try {
                if (result == false) targetFile.delete();
            } catch (Exception e2) {
                log.error("IGNORE:", e2);
            }
        }
        return result;
    }
}
