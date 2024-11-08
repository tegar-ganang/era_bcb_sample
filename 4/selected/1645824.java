package com.tomczarniecki.s3.gui;

import com.tomczarniecki.s3.Lists;
import com.tomczarniecki.s3.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.List;

class FileDropListener implements FileDrop.Listener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProgressDialog dialog;

    private final FileSize fileSize;

    private final Controller controller;

    private final DropBoxModel model;

    private final Display display;

    private final Worker worker;

    public FileDropListener(Controller controller, DropBoxModel model, Display display, Worker worker) {
        this.dialog = display.createProgressDialog("Upload Progress", worker);
        this.fileSize = new FileSize();
        this.controller = controller;
        this.display = display;
        this.worker = worker;
        this.model = model;
    }

    public void filesDropped(final File[] files) {
        if (controller.isShowingObjects()) {
            uploadFiles(files);
        } else {
            worker.executeOnEventLoop(new Runnable() {

                public void run() {
                    selectBucketAndUploadFiles(files);
                }
            });
        }
    }

    private void selectBucketAndUploadFiles(File[] files) {
        List<String> names = model.getCurrentNames();
        String bucketName = display.selectOption("Select Folder", "Please choose a folder for your files.", names);
        if (bucketName != null) {
            controller.selectBucket(bucketName);
            controller.showObjects();
            uploadFiles(files);
        }
    }

    private void uploadFiles(final File[] files) {
        worker.executeInBackground(new Runnable() {

            public void run() {
                dialog.begin();
                try {
                    uploadFiles(resolveKeys(files));
                    dialog.append("\nDone");
                } finally {
                    dialog.finish();
                }
            }
        });
    }

    private void uploadFiles(List<Pair<String, File>> files) {
        String bucketName = controller.getSelectedBucketName();
        String plural = (files.size() > 1) ? "files" : "file";
        dialog.append("Attempting upload of %d %s to folder %s\n\n", files.size(), plural, bucketName);
        for (Pair<String, File> entry : files) {
            uploadFile(bucketName, entry.getKey(), entry.getValue());
        }
    }

    private void uploadFile(String bucketName, String objectKey, File file) {
        if (canCreateObject(bucketName, objectKey, file)) {
            attemptObjectCreation(bucketName, objectKey, file);
        } else {
            dialog.append("File %s not uploaded\n", file.getAbsolutePath());
        }
    }

    private boolean canCreateObject(String bucketName, String objectKey, File file) {
        if (controller.objectExists(bucketName, objectKey)) {
            String message = "File %s already exists in folder %s.\nDo you want to overwrite?";
            return display.confirmMessage("Oops", String.format(message, file.getName(), bucketName));
        }
        return true;
    }

    private void attemptObjectCreation(String bucketName, String objectKey, File file) {
        dialog.next();
        try {
            dialog.append("File %s (%s) ...", file.getAbsolutePath(), fileSize.format(file.length()));
            controller.createObject(bucketName, objectKey, file, dialog);
            dialog.append(" OK\n");
        } catch (Throwable e) {
            logger.warn("Upload failed for " + file, e);
            dialog.append(" ERROR\n --> %s\n", e.toString());
        }
    }

    private List<Pair<String, File>> resolveKeys(File[] files) {
        List<Pair<String, File>> list = Lists.create();
        resolveFolders(list, files, "");
        return list;
    }

    private void resolveFolders(List<Pair<String, File>> list, File[] folder, String prefix) {
        for (File file : folder) {
            resolveFile(list, file, prefix);
        }
    }

    private void resolveFile(List<Pair<String, File>> list, File file, String prefix) {
        if (file.isFile()) {
            list.add(Pair.create(prefix + file.getName(), file));
        } else if (file.isDirectory()) {
            resolveFolders(list, file.listFiles(), prefix + file.getName() + "/");
        }
    }
}
