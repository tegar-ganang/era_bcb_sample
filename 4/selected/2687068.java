package org.gridtrust.trs.impl.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import org.gridtrust.trs.ObtainUserReputation;
import org.gridtrust.trs.ObtainUserReputationResponse;
import org.gridtrust.trs.RateUser;
import org.gridtrust.trs.RateUserResponse;
import org.gridtrust.trs.test.AbstractTrsTestCase;
import com.clarkware.junitperf.ConstantTimer;
import com.clarkware.junitperf.RandomTimer;
import com.clarkware.junitperf.Timer;

public class TestLoadTrustAndReputationService extends AbstractTrsTestCase {

    protected Timer rndTtimer = new RandomTimer(5000, 2000);

    protected Timer cnsTimer = new ConstantTimer(1000);

    protected int cnt = 0;

    protected int iterations = 100;

    protected static Date timeZero = null;

    protected File outFile = null;

    public static synchronized void setZero() {
        if (timeZero == null) {
            timeZero = new Date();
        }
    }

    public TestLoadTrustAndReputationService(String name) {
        super(name);
    }

    protected long getConstantMillisecondsToWait() {
        return cnsTimer.getDelay();
    }

    protected long getRandomMillisecondsToWait() {
        return rndTtimer.getDelay();
    }

    public void setUp() throws Exception {
        super.setUp();
        setZero();
        outFile = new File(trsConfig.getProperty(org.gridtrust.trs.util.Constants.TRS_LOAD_TEST_OUTFOLDER) + File.separator + Thread.currentThread().getId() + "-" + (new Date().getTime()) + ".perf");
        if (!outFile.exists()) {
            outFile.createNewFile();
        }
    }

    public void httpVsHttps() {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(outFile));
            out.write("#Thread\tRequest Time\tResponse Time (ms)\n");
            out.flush();
            Date start = null;
            Date end = null;
            for (int i = 0; i < iterations; i++) {
                Thread.sleep(getConstantMillisecondsToWait());
                ObtainUserReputation obtainUserReputationRequest = new ObtainUserReputation();
                ObtainUserReputationResponse obtainUserReputationResponse;
                obtainUserReputationRequest.setIoi(null);
                obtainUserReputationRequest.setServiceId(null);
                obtainUserReputationRequest.setUserId("u0001");
                obtainUserReputationRequest.setVbeId("1");
                obtainUserReputationRequest.setVoId("vo1");
                start = new Date();
                obtainUserReputationResponse = trsPort.obtainUserReputation(obtainUserReputationRequest);
                end = new Date();
                out.write(Thread.currentThread().getId() + "\t" + cnt + "\t" + (end.getTime() - start.getTime()) + "\n");
                out.flush();
                cnt++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void load() {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(outFile));
            out.write("#Thread\tRequest Time\tResponse Time (ms)\n");
            out.flush();
            for (int i = 0; i < iterations; i++) {
                Thread.sleep(getRandomMillisecondsToWait());
                ObtainUserReputation obtainUserReputationRequest = new ObtainUserReputation();
                ObtainUserReputationResponse obtainUserReputationResponse;
                obtainUserReputationRequest.setIoi(null);
                obtainUserReputationRequest.setServiceId(null);
                obtainUserReputationRequest.setUserId(org.gridtrust.trs.test.Constants.USER_ID_1);
                obtainUserReputationRequest.setVbeId(org.gridtrust.trs.test.Constants.VBE_ID_1);
                obtainUserReputationRequest.setVoId(org.gridtrust.trs.test.Constants.VO_ID_1);
                Date start = new Date();
                obtainUserReputationResponse = trsPort.obtainUserReputation(obtainUserReputationRequest);
                Date end = new Date();
                out.write(Thread.currentThread().getId() + "\t" + (start.getTime() - timeZero.getTime()) + "\t" + (end.getTime() - start.getTime()) + "\n");
                out.flush();
                Thread.sleep(getRandomMillisecondsToWait());
                RateUser rateUserRequest = new RateUser();
                RateUserResponse rateUserRequestResponse;
                rateUserRequest.setActionId(org.gridtrust.trs.test.Constants.ACTION_GOOD_CONTENT);
                rateUserRequest.setServiceId(org.gridtrust.trs.test.Constants.SERVICE_ID_1);
                rateUserRequest.setUserId(org.gridtrust.trs.test.Constants.USER_ID_1);
                rateUserRequest.setVbeId(org.gridtrust.trs.test.Constants.VBE_ID_1);
                rateUserRequest.setVoId(org.gridtrust.trs.test.Constants.VO_ID_1);
                start = new Date();
                rateUserRequestResponse = trsPort.rateUser(rateUserRequest);
                end = new Date();
                out.write(Thread.currentThread().getId() + "\t" + (start.getTime() - timeZero.getTime()) + "\t" + (end.getTime() - start.getTime()) + "\n");
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
