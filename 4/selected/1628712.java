package org.slasoi.orcsample.pac.messagelisteners;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.MessageListener;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import org.slasoi.common.slautil.Evaluator;
import org.slasoi.common.slautil.SlaUtil;
import org.slasoi.common.slautil.Violation;
import org.slasoi.gslam.core.context.SLAManagerContext.SLAManagerContextException;
import org.slasoi.gslam.core.pac.ProvisioningAdjustment;
import org.slasoi.gslam.core.pac.ProvisioningAdjustment.PlanNotFoundException;
import org.slasoi.orcsample.core.services.XMPP;
import org.slasoi.orcsample.pac.taskexecutor.DemoWorkflow;
import org.slasoi.orcsample.pac.taskexecutor.TaskExecutor;
import org.slasoi.slamodel.core.FunctionalExpr;
import org.slasoi.slamodel.primitives.CONST;
import org.slasoi.slamodel.primitives.ID;
import org.slasoi.slamodel.primitives.STND;
import org.slasoi.slamodel.vocab.common;
import org.slasoi.slamodel.vocab.units;

public class WorkloadMessageListener implements MessageListener {

    private final String SLA_ID = "slaId";

    private final String TYPE = "type";

    private final String PAYLOAD = "payload";

    private final String PARTY = "party";

    private final String PENALTY = "value";

    private final String PENALTY_SUM = "penalty";

    private final String DATE = "timestamp";

    private final String ARRIVAL_RATE = "arrival_rate";

    private final String ORC_ENDPOINT = "orcEndpoint";

    private TaskExecutor taskExecutor;

    private Evaluator evaluator;

    private String uuid;

    Set<FunctionalExpr> functionalExpressions = new HashSet<FunctionalExpr>();

    Map<FunctionalExpr, CONST> constraints_arrival_rate;

    Map<FunctionalExpr, CONST> constraints_completion_time;

    private DemoWorkflow demoWorkflow = new DemoWorkflow();

    private static Logger logger = Logger.getLogger(WorkloadMessageListener.class.getName());

    private XMPP xmpp = XMPP.getInstance();

    public WorkloadMessageListener(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        this.evaluator = new Evaluator(taskExecutor.getBusinessSLAT());
        this.uuid = taskExecutor.getBusinessSLAT().getUuid().getValue();
        loadConstraints();
        try {
            loadConstraints();
            xmpp.info("MM", "software monitoring configured", new Date());
            taskExecutor.getPubSubManager().publish(new PubSubMessage(XMPP.FRAMEWORK_LOGGING_CHANNEL, buildOnCreateMessage().toString()));
        } catch (MessagingException e1) {
            logger.error(e1);
        }
    }

    public void addNotify(JSONObject object) {
        try {
            STND op = (ARRIVAL_RATE.equalsIgnoreCase(object.getString(TYPE))) ? common.arrival_rate : common.completion_time;
            String[] params = object.getString(PAYLOAD).split(":");
            if (op.equals(common.arrival_rate)) {
                CONST value = new CONST(params[2], units.tx_per_s);
                for (Entry<FunctionalExpr, CONST> entry : constraints_arrival_rate.entrySet()) {
                    evaluator.addNotify(entry.getKey(), value);
                }
            } else if (op.equals(common.completion_time)) {
                CONST value = new CONST(params[3], units.ms);
                FunctionalExpr expr = matchFunctionalExpr(op, new ID(params[2]));
                evaluator.addNotify(expr, value);
            }
        } catch (JSONException e) {
            logger.error(e);
        }
    }

    private FunctionalExpr matchFunctionalExpr(STND op, ID id) {
        for (FunctionalExpr expr : functionalExpressions) {
            if (expr.getOperator().equals(op) && expr.getParameters()[0].equals(id)) {
                return expr;
            }
        }
        return null;
    }

    public JSONObject buildViolationMessage(Violation violation) {
        JSONObject result = new JSONObject();
        Map<FunctionalExpr, List<Violation>> roleViolations = demoWorkflow.processViolation(violation);
        if (roleViolations == null) {
            return null;
        }
        try {
            result.put(TYPE, "violationsReport");
            JSONObject report = new JSONObject();
            report.put(SLA_ID, uuid);
            report.put(PARTY, new STND(violation.getPartyID().getValue()).toString());
            report.put(PENALTY_SUM, Integer.toString(violation.getPenaltySum()));
            for (Entry<FunctionalExpr, List<Violation>> printableFunctions : roleViolations.entrySet()) {
                String id = demoWorkflow.extractFunction(printableFunctions.getKey());
                JSONArray violationArray = new JSONArray();
                for (Violation item : printableFunctions.getValue()) {
                    JSONObject penalty = new JSONObject();
                    penalty.put(PENALTY, item.getPenalty());
                    penalty.put(DATE, XMPP.dateToString(item.getDate()));
                    violationArray.put(penalty);
                }
                report.put(id + "History", violationArray);
            }
            result.put("report", report);
        } catch (JSONException e) {
            logger.error(e);
        }
        return result;
    }

    public JSONObject authorize(PubSubMessage message) {
        JSONObject result = null;
        if (message.getChannelName().equalsIgnoreCase(XMPP.WORKLOAD_CHANNEL_NAME)) {
            try {
                result = new JSONObject(message.getPayload());
                String[] params = result.getString(PAYLOAD).split(":");
                if (params == null || params.length == 0 || !uuid.equals(params[0])) {
                    return null;
                }
            } catch (JSONException e) {
                result = null;
                logger.error(e);
            }
        }
        return result;
    }

    public void processMessage(MessageEvent messageEvent) {
        PubSubMessage message = messageEvent.getMessage();
        JSONObject payload = authorize(message);
        if (payload != null) {
            addNotify(payload);
            while (true) {
                Violation violation = evaluator.getViolation();
                if (violation == null) break;
                react(violation);
                xmpp.info("BM", "violation detected", new Date());
                try {
                    JSONObject json = buildViolationMessage(violation);
                    if (json != null) {
                        PubSubMessage response = new PubSubMessage(XMPP.FRAMEWORK_LOGGING_CHANNEL, json.toString());
                        taskExecutor.getPubSubManager().publish(response);
                    }
                } catch (MessagingException e) {
                    logger.error(e);
                }
            }
        }
    }

    public void react(Violation violation) {
        try {
            if (SlaUtil.isTerminationAction(violation.getAction())) {
                ProvisioningAdjustment pac = taskExecutor.getContext().getProvisioningAdjustment();
                pac.cancelExecution(taskExecutor.getPlan().getPlanId());
            }
        } catch (SLAManagerContextException e) {
            e.printStackTrace();
        } catch (PlanNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ID extractID(FunctionalExpr expr) {
        return (ID) expr.getParameters()[0];
    }

    private void loadConstraints() {
        Map<FunctionalExpr, CONST> constraints = evaluator.getConstraints();
        constraints_arrival_rate = new HashMap<FunctionalExpr, CONST>();
        constraints_completion_time = new HashMap<FunctionalExpr, CONST>();
        for (Entry<FunctionalExpr, CONST> constraint : constraints.entrySet()) {
            functionalExpressions.add(constraint.getKey());
            if (common.arrival_rate.equals(constraint.getKey().getOperator())) {
                constraints_arrival_rate.put(constraint.getKey(), constraint.getValue());
            } else if (common.completion_time.equals(constraint.getKey().getOperator())) {
                constraints_completion_time.put(constraint.getKey(), constraint.getValue());
            }
        }
    }

    private JSONObject buildOnCreateMessage() {
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE, "agreementCreated");
            json.put(SLA_ID, uuid);
            json.put(ORC_ENDPOINT, "http://slasoi.xlab.si:8081/axis/services/");
            JSONObject responseTimeThresholds = new JSONObject();
            for (Entry<FunctionalExpr, CONST> entry : constraints_completion_time.entrySet()) {
                String function = demoWorkflow.extractFunction(entry.getKey());
                if (function != null) {
                    responseTimeThresholds.put(function, entry.getValue().getValue());
                }
            }
            json.put("responseTimeThresholds", responseTimeThresholds);
            JSONObject arrivalRateThresholds = new JSONObject();
            for (Entry<FunctionalExpr, CONST> entry : constraints_arrival_rate.entrySet()) {
                String function = demoWorkflow.extractFunction(entry.getKey());
                if (function != null) {
                    arrivalRateThresholds.put(function, entry.getValue().getValue());
                }
            }
            json.put("arrivalRateThresholds", arrivalRateThresholds);
        } catch (JSONException e) {
            logger.error(e);
        }
        return json;
    }
}
