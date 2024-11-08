package com.google.code.fuzzops.webfuzzer.commands;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import com.google.code.fuzzops.webfuzzer.applet.CommunicationBean;
import com.google.code.fuzzops.webfuzzer.applet.FuzzResponseBean;
import com.google.code.fuzzops.webfuzzer.applet.ResultBean;
import com.google.code.fuzzops.webfuzzer.applet.UtilBean;
import com.google.code.fuzzops.webfuzzer.controller.FuzzCommandInterface;

public class RequestCommand implements FuzzCommandInterface {

    FileInputStream fInput;

    ObjectInputStream oInput;

    ArrayList<ResultBean> currentFileContents;

    FilePermission perm;

    ResultBean result;

    @SuppressWarnings("unchecked")
    public void execute(ArrayList params, UtilBean utils) {
        File returnFile = new File(utils.getOutputFile().getAbsolutePath() + "\\" + (String) params.get(0));
        perm = new java.io.FilePermission(utils.getOutputFile().getAbsolutePath(), "write,read");
        utils.monitor.log("Found: " + returnFile.getAbsolutePath());
        ArrayList returnVal = new ArrayList();
        try {
            fInput = new FileInputStream(returnFile);
            oInput = new ObjectInputStream(fInput);
            utils.monitor.log("Sending information about the results...");
            returnVal.add((FuzzResponseBean) oInput.readObject());
            utils.getOutput().writeObject(new CommunicationBean("response", returnVal));
            utils.monitor.log("Sending " + params.get(0) + "...");
            while ((result = (ResultBean) oInput.readObject()) != null) {
                returnVal = new ArrayList();
                returnVal.add(result);
                try {
                    utils.getOutput().writeObject(new CommunicationBean("response", returnVal));
                } catch (IOException e) {
                    utils.monitor.log("Failed: could not send requested file");
                    e.printStackTrace();
                }
            }
        } catch (EOFException ex) {
            utils.monitor.log(params.get(0) + "Sent");
            try {
                oInput.close();
                fInput.close();
            } catch (IOException e) {
            }
            try {
                returnVal = new ArrayList();
                returnVal.add(false);
                utils.getOutput().writeObject(new CommunicationBean("response", returnVal));
            } catch (IOException e) {
                utils.monitor.log("Failed to alert client of EOF");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            e = new Exception("Did not read in file correctly");
        }
    }
}
