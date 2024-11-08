package org.jimcat.services.jobs;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;

/**
 * This class encapsulates methods for operations on files.
 * 
 * $Id$
 * 
 * @author Michael
 */
public class JobUtils {

    /**
	 * this methode will try to find an unused filename within the destination
	 * directory.
	 * 
	 * @param destination
	 * @param file
	 * @param includeParent -
	 *            if the parent directory should be included too
	 * @return the next free file name in the destination directory
	 */
    public static File getNextFreeCopyFileName(File destination, File file, boolean includeParent) {
        String parent = file.getParentFile().getName();
        String name = file.getName();
        int dotIndex = name.indexOf('.');
        String extension = "";
        if (dotIndex != -1) {
            extension = name.substring(dotIndex);
            name = name.substring(0, dotIndex);
        }
        int num = 1;
        String testName = "";
        if (includeParent) {
            testName = testName + parent + "/";
        }
        testName = testName + name + extension;
        File dest = new File(destination, testName);
        while (dest.exists()) {
            num++;
            testName = "";
            if (includeParent) {
                testName = testName + parent + "/";
            }
            testName = testName + name + "(" + num + ")" + extension;
            dest = new File(destination, testName);
        }
        return dest;
    }

    /**
	 * This methode will copy the source file to the given destination.
	 * 
	 * If the destination file already exists, it will be overriden. If an error
	 * occures, a JobFailure - Handling request will be generated and send to
	 * the job. The result will be used to continue work.
	 * 
	 * If the result is cancel or rollback (only if supported by job), then the
	 * jobstate will be changed. The last IOException making the final problem
	 * will be thrown. Therefore, check jobstate after recieving an Exception.
	 * 
	 * @param source -
	 *            the source file
	 * @param dest -
	 *            the destination file
	 * @param job -
	 *            the calling job
	 * @return - true if successfull, false if an error occured, but user
	 *         ignored it
	 * @throws IOException -
	 *             will be thrown if user selectes ignore, cancel or rollback
	 *             afer an error - check job state!
	 */
    public static boolean copyFile(File source, File dest, Job job) throws IOException {
        boolean done = false;
        while (!done) {
            try {
                FileUtils.copyFile(source, dest);
                done = true;
            } catch (IOException ioe) {
                JobFailureDescription desc = new JobFailureDescription();
                desc.setCause(ioe);
                desc.setDescription("An error occured during file copy. \n" + "Please make sure the source file and destination directory is accessable and not full.");
                List<JobFailureOption> options = new LinkedList<JobFailureOption>();
                options.add(JobFailureOption.Retry);
                if (canRollback(job)) {
                    options.add(JobFailureOption.Rollback);
                }
                options.add(JobFailureOption.Cancel);
                desc.setOptions(options);
                desc.setRespond(JobFailureOption.Retry);
                job.requestFailureHandling(desc);
                switch(desc.getRespond()) {
                    case Retry:
                        done = false;
                        break;
                    case Rollback:
                        job.rollback();
                        throw ioe;
                    case Cancel:
                        job.cancel();
                        throw ioe;
                    case Ignore:
                    case IgnoreAll:
                        return false;
                }
            }
        }
        return true;
    }

    /**
	 * This methode will delete a file.
	 * 
	 * If file can't be deleted, a JobFailerDescription will be generated and
	 * handed over to the given job. So the user can decide what to do.
	 * 
	 * Therefore, it is possible that the job state is changing during
	 * execution. If it is caused by the user, this methode will throw an
	 * IOException to indecate something went wrong.
	 * 
	 * @param victem -
	 *            the file to delete
	 * @param job -
	 *            the job requesting the operation
	 * @throws IOException -
	 *             if something went wrong and the user decided to cancel /
	 *             rollback this job
	 */
    public static void deleteFile(File victem, Job job) throws IOException {
        JobFailureDescription desc = new JobFailureDescription();
        desc.setDescription("Error deleting file " + victem.getName() + ".\n" + "Please make sure the file is accessable.");
        List<JobFailureOption> options = new LinkedList<JobFailureOption>();
        options.add(JobFailureOption.Retry);
        options.add(JobFailureOption.Ignore);
        if (canRollback(job)) {
            options.add(JobFailureOption.Rollback);
        }
        options.add(JobFailureOption.Cancel);
        desc.setOptions(options);
        desc.setRespond(JobFailureOption.Retry);
        if (victem.exists()) {
            boolean ignored = false;
            while (!victem.delete() && !ignored) {
                job.requestFailureHandling(desc);
                switch(desc.getRespond()) {
                    case Retry:
                        ignored = false;
                        break;
                    case Ignore:
                        ignored = true;
                        break;
                    case Rollback:
                        job.rollback();
                        throw new IOException("error deleting file " + victem.getName());
                    case Cancel:
                        job.cancel();
                        throw new IOException("error deleting file " + victem.getName());
                    case IgnoreAll:
                        break;
                }
            }
        }
    }

    /**
	 * check if the given job can be still rollbacked.
	 * 
	 * @param job
	 * @return a boolean indicating if the job supports rollback at the moment
	 */
    private static boolean canRollback(Job job) {
        return (job.supportsRollback() && !job.getState().isUndoingState() && !job.getState().isFinal());
    }
}
