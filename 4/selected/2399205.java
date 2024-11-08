package org.gridtrust.trs.impl.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Date;
import org.gridtrust.trs.Action;
import org.gridtrust.trs.AddServiceInVbe;
import org.gridtrust.trs.AddServiceInVbeResponse;
import org.gridtrust.trs.AddUserInVbe;
import org.gridtrust.trs.AddUserInVbeResponse;
import org.gridtrust.trs.AddVo;
import org.gridtrust.trs.AddVoResponse;
import org.gridtrust.trs.ObtainUserReputation;
import org.gridtrust.trs.ObtainUserReputationResponse;
import org.gridtrust.trs.RateUser;
import org.gridtrust.trs.RateUserResponse;
import org.gridtrust.trs.RegisterServiceInVo;
import org.gridtrust.trs.RegisterServiceInVoResponse;
import org.gridtrust.trs.RegisterUserInVo;
import org.gridtrust.trs.RegisterUserInVoResponse;
import org.gridtrust.trs.Service;
import org.gridtrust.trs.User;
import org.gridtrust.trs.test.AbstractTrsTestCase;
import com.sun.corba.se.pept.transport.Acceptor;

public class Simulation extends AbstractTrsTestCase {

    private static String trsDemoSimulationfile = "";

    private static String trsDemoInitVosfile = "";

    private static String trsDemoInitUsersfile = "";

    private static String trsDemoInitServicesfile = "";

    private static String trsDemoInitVoUsersfile = "";

    private static String trsDemoInitVoServicesfile = "";

    protected File outFile = null;

    public Simulation(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        trsDemoSimulationfile = trsConfig.getProperty(org.gridtrust.trs.util.Constants.TRS_DEMO_SIMULATION_FILE);
        trsDemoInitVosfile = trsConfig.getProperty(org.gridtrust.trs.util.Constants.TRS_DEMO_INIT_VOS_FILE);
        trsDemoInitUsersfile = trsConfig.getProperty(org.gridtrust.trs.util.Constants.TRS_DEMO_INIT_USERS_FILE);
        trsDemoInitServicesfile = trsConfig.getProperty(org.gridtrust.trs.util.Constants.TRS_DEMO_INIT_SERVICES_FILE);
        trsDemoInitVoUsersfile = trsConfig.getProperty(org.gridtrust.trs.util.Constants.TRS_DEMO_INIT_VO_USERS_FILE);
        trsDemoInitVoServicesfile = trsConfig.getProperty(org.gridtrust.trs.util.Constants.TRS_DEMO_INIT_VO_SERVICES_FILE);
        outFile = new File(trsConfig.getProperty(org.gridtrust.trs.util.Constants.TRS_LOAD_TEST_OUTFOLDER) + File.separator + Thread.currentThread().getId() + "-" + (new Date().getTime()) + ".sim");
        if (!outFile.exists()) {
            outFile.createNewFile();
        }
    }

    public void runSimulation() {
        try {
            simulate();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error during the simulation");
        }
    }

    private void simulate() throws Exception {
        BufferedWriter out = null;
        out = new BufferedWriter(new FileWriter(outFile));
        out.write("#Thread\tReputation\tAction\n");
        out.flush();
        System.out.println("Simulate...");
        File file = new File(trsDemoSimulationfile);
        ObtainUserReputation obtainUserReputationRequest = new ObtainUserReputation();
        ObtainUserReputationResponse obtainUserReputationResponse;
        RateUser rateUserRequest;
        RateUserResponse rateUserResponse;
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        String call = br.readLine();
        while (call != null) {
            rateUserRequest = generateRateUserRequest(call);
            try {
                rateUserResponse = trsPort.rateUser(rateUserRequest);
                System.out.println("----------------R A T I N G-------------------");
                System.out.println("VBE: " + rateUserRequest.getVbeId());
                System.out.println("VO: " + rateUserRequest.getVoId());
                System.out.println("USER: " + rateUserRequest.getUserId());
                System.out.println("SERVICE: " + rateUserRequest.getServiceId());
                System.out.println("ACTION: " + rateUserRequest.getActionId());
                System.out.println("OUTCOME: " + rateUserResponse.isOutcome());
                System.out.println("----------------------------------------------");
                assertEquals("The outcome field of the rateUser should be true: MESSAGE=" + rateUserResponse.getMessage(), true, rateUserResponse.isOutcome());
            } catch (RemoteException e) {
                fail(e.getMessage());
            }
            obtainUserReputationRequest.setIoi(null);
            obtainUserReputationRequest.setServiceId(null);
            obtainUserReputationRequest.setUserId(rateUserRequest.getUserId());
            obtainUserReputationRequest.setVbeId(rateUserRequest.getVbeId());
            obtainUserReputationRequest.setVoId(null);
            try {
                obtainUserReputationResponse = trsPort.obtainUserReputation(obtainUserReputationRequest);
                System.out.println("-----------R E P U T A T I O N----------------");
                System.out.println("VBE: " + obtainUserReputationRequest.getVbeId());
                System.out.println("VO: " + obtainUserReputationRequest.getVoId());
                System.out.println("USER: " + obtainUserReputationRequest.getUserId());
                System.out.println("SERVICE: " + obtainUserReputationRequest.getServiceId());
                System.out.println("IOI: " + obtainUserReputationRequest.getIoi());
                System.out.println("REPUTATION: " + obtainUserReputationResponse.getReputation());
                System.out.println("----------------------------------------------");
                assertEquals("The outcome field of the obtainUserReputation should be true: MESSAGE=" + obtainUserReputationResponse.getMessage(), true, obtainUserReputationResponse.isOutcome());
                assertEquals(0.0, obtainUserReputationResponse.getReputation(), 1.0);
            } catch (RemoteException e) {
                fail(e.getMessage());
            }
            obtainUserReputationRequest.setIoi(null);
            obtainUserReputationRequest.setServiceId(null);
            obtainUserReputationRequest.setUserId(rateUserRequest.getUserId());
            obtainUserReputationRequest.setVbeId(rateUserRequest.getVbeId());
            obtainUserReputationRequest.setVoId(rateUserRequest.getVoId());
            try {
                obtainUserReputationResponse = trsPort.obtainUserReputation(obtainUserReputationRequest);
                System.out.println("-----------R E P U T A T I O N----------------");
                System.out.println("VBE: " + obtainUserReputationRequest.getVbeId());
                System.out.println("VO: " + obtainUserReputationRequest.getVoId());
                System.out.println("USER: " + obtainUserReputationRequest.getUserId());
                System.out.println("SERVICE: " + obtainUserReputationRequest.getServiceId());
                System.out.println("IOI: " + obtainUserReputationRequest.getIoi());
                System.out.println("REPUTATION: " + obtainUserReputationResponse.getReputation());
                System.out.println("----------------------------------------------");
                assertEquals("The outcome field of the obtainUserReputation should be true: MESSAGE=" + obtainUserReputationResponse.getMessage(), true, obtainUserReputationResponse.isOutcome());
                assertEquals(0.0, obtainUserReputationResponse.getReputation(), 1.0);
            } catch (RemoteException e) {
                fail(e.getMessage());
            }
            call = br.readLine();
        }
        fis.close();
        br.close();
        out.flush();
        out.close();
    }

    public void initVos() throws Exception {
        System.out.println("Init Vos...");
        File file = new File(trsDemoInitVosfile);
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        String call = br.readLine();
        AddVoResponse response;
        while (call != null) {
            AddVo request = genereateAddVoRequest(call);
            response = trsPort.addVo(request);
            if (response.isOutcome()) {
                System.out.println(request.getVoId() + " Added.");
            } else {
                System.out.println("Error adding " + request.getVoId());
            }
            call = br.readLine();
        }
        fis.close();
        br.close();
    }

    private void initUsers() throws Exception {
        System.out.println("Init Users...");
        File file = new File(trsDemoInitUsersfile);
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        String call = br.readLine();
        AddUserInVbeResponse addUserInVbeResponse;
        while (call != null) {
            AddUserInVbe request = generateAddUserInVbeRequest(call);
            addUserInVbeResponse = trsPort.addUserInVbe(request);
            if (addUserInVbeResponse.isOutcome()) {
                System.out.println(request.getUser().getUserId() + " Added.");
            } else {
                System.out.println("Error adding " + request.getUser().getUserId() + ". " + addUserInVbeResponse.getMessage());
            }
            call = br.readLine();
        }
        fis.close();
        br.close();
        RegisterUserInVoResponse registerUserInVoResponse;
        file = new File(trsDemoInitVoUsersfile);
        fis = new FileInputStream(file);
        br = new BufferedReader(new InputStreamReader(fis));
        call = br.readLine();
        while (call != null) {
            RegisterUserInVo request = generateRegisterUserInVoRequest(call);
            registerUserInVoResponse = trsPort.registerUserInVo(request);
            if (registerUserInVoResponse.isOutcome()) {
                System.out.println(request.getUserId() + " Added.");
            } else {
                System.out.println("Error adding " + request.getUserId() + ". " + registerUserInVoResponse.getMessage());
            }
            call = br.readLine();
        }
        fis.close();
        br.close();
    }

    private void initServices() throws Exception {
        System.out.println("Init Services...");
        File file = new File(trsDemoInitServicesfile);
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        String call = br.readLine();
        AddServiceInVbeResponse addServiceInVbeResponse;
        while (call != null) {
            AddServiceInVbe request = generateAddServiceInVbeRequest(call);
            addServiceInVbeResponse = trsPort.addServiceInVbe(request);
            if (addServiceInVbeResponse.isOutcome()) {
                System.out.println(request.getService().getServiceId() + " Added.");
            } else {
                System.out.println("Error adding " + request.getService().getServiceId() + ". " + addServiceInVbeResponse.getMessage());
            }
            call = br.readLine();
        }
        fis.close();
        br.close();
        file = new File(trsDemoInitVoServicesfile);
        fis = new FileInputStream(file);
        br = new BufferedReader(new InputStreamReader(fis));
        call = br.readLine();
        RegisterServiceInVoResponse registerServiceInVoResponse;
        while (call != null) {
            RegisterServiceInVo request = generateRegisterServiceInVoRequest(call);
            registerServiceInVoResponse = trsPort.registerServiceInVo(request);
            if (registerServiceInVoResponse.isOutcome()) {
                System.out.println(request.getServiceId() + " Added.");
            } else {
                System.out.println("Error adding " + request.getServiceId() + ". " + registerServiceInVoResponse.getMessage());
            }
            call = br.readLine();
        }
        fis.close();
        br.close();
    }

    private RateUser generateRateUserRequest(String call) {
        RateUser rateUserRequest = new RateUser();
        String[] fields = call.split(";");
        rateUserRequest.setVbeId(fields[0]);
        rateUserRequest.setVoId(fields[1]);
        rateUserRequest.setUserId(fields[2]);
        rateUserRequest.setServiceId(fields[3]);
        rateUserRequest.setActionId(fields[4]);
        return rateUserRequest;
    }

    private AddUserInVbe generateAddUserInVbeRequest(String call) {
        AddUserInVbe addUserInVbeRequest = new AddUserInVbe();
        String[] fields = call.split(";");
        addUserInVbeRequest.setVbeId(fields[0]);
        User user = new User();
        user.setUserId(fields[1]);
        addUserInVbeRequest.setUser(user);
        return addUserInVbeRequest;
    }

    private AddServiceInVbe generateAddServiceInVbeRequest(String call) {
        AddServiceInVbe addServiceInVbeRequest = new AddServiceInVbe();
        String[] fields = call.split(";");
        addServiceInVbeRequest.setVbeId(fields[0]);
        Service service = new Service();
        service.setServiceId(fields[1]);
        Action actionOk = new Action();
        actionOk.setActionId("OK");
        actionOk.setIoi("ServiceUsage");
        actionOk.setRate(1);
        Action actionViolation = new Action();
        actionViolation.setActionId("VIOLATION");
        actionViolation.setIoi("ServiceUsage");
        actionViolation.setRate(-1);
        Action[] actions = new Action[2];
        actions[0] = actionOk;
        actions[1] = actionViolation;
        service.setActions(actions);
        addServiceInVbeRequest.setService(service);
        return addServiceInVbeRequest;
    }

    private RegisterServiceInVo generateRegisterServiceInVoRequest(String call) {
        RegisterServiceInVo registerServiceInVoRequest = new RegisterServiceInVo();
        String[] fields = call.split(";");
        registerServiceInVoRequest.setVbeId(fields[0]);
        registerServiceInVoRequest.setVoId(fields[1]);
        registerServiceInVoRequest.setServiceId(fields[2]);
        return registerServiceInVoRequest;
    }

    private RegisterUserInVo generateRegisterUserInVoRequest(String call) {
        RegisterUserInVo registerUserInVoRequest = new RegisterUserInVo();
        String[] fields = call.split(";");
        registerUserInVoRequest.setVbeId(fields[0]);
        registerUserInVoRequest.setVoId(fields[1]);
        registerUserInVoRequest.setUserId(fields[2]);
        return registerUserInVoRequest;
    }

    private AddVo genereateAddVoRequest(String call) {
        AddVo addVoRequest = new AddVo();
        String[] fields = call.split(";");
        addVoRequest.setVbeId(fields[0]);
        addVoRequest.setVoId(fields[1]);
        return addVoRequest;
    }
}
