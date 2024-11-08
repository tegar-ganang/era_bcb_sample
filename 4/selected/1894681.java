package com.google.code.fuzzops.webfuzzer.commands;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.util.ArrayList;
import com.google.code.fuzzops.webfuzzer.applet.CommunicationBean;
import com.google.code.fuzzops.webfuzzer.applet.UtilBean;
import com.google.code.fuzzops.webfuzzer.controller.FuzzCommandInterface;

public class DeleteCommand implements FuzzCommandInterface {

    FilePermission perm;

    @SuppressWarnings("unchecked")
    public void execute(ArrayList params, UtilBean utils) {
        perm = new java.io.FilePermission(utils.getOutputFile().getAbsolutePath(), "write,delete,read");
        String fileNameToDelete = (String) params.get(0);
        File fileToDelete = new File(utils.getOutputFile().getAbsolutePath() + "\\" + fileNameToDelete);
        utils.monitor.log("File to delete: " + utils.getOutputFile().getAbsolutePath() + "\\" + fileNameToDelete);
        ArrayList returnVal = new ArrayList();
        try {
            boolean result = fileToDelete.delete();
            returnVal.add(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            utils.getOutput().writeObject(new CommunicationBean("result", returnVal));
            utils.monitor.log("Sending deletion response message");
        } catch (IOException e) {
            utils.monitor.log("Failed to send deletion complete");
            e.printStackTrace();
        }
    }
}
