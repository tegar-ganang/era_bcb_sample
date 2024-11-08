package org.slasoi.orcmockup.pac.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.slasoi.common.messaging.pointtopoint.Message;
import org.slasoi.common.messaging.pointtopoint.Messaging;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.MessageListener;
import org.slasoi.common.messaging.pubsub.PubSubManager;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import org.slasoi.common.slautil.Evaluator;
import org.slasoi.common.slautil.Violation;
import org.slasoi.gslam.commons.plan.Plan;
import org.slasoi.gslam.commons.plan.Task;
import org.slasoi.gslam.commons.plan.TaskNotFoundException;
import org.slasoi.gslam.core.context.SLAManagerContext;
import org.slasoi.gslam.core.context.SLAManagerContext.SLAManagerContextException;
import org.slasoi.gslam.core.negotiation.SLATemplateRegistry;
import org.slasoi.gslam.core.negotiation.SLARegistry.IQuery;
import org.slasoi.gslam.core.negotiation.SLARegistry.InvalidUUIDException;
import org.slasoi.gslam.core.pac.ProvisioningAdjustment.Status;
import org.slasoi.gslam.pac.TaskStatus;
import org.slasoi.orcmockup.core.TemplateRegistry;
import org.slasoi.orcmockup.core.XMPP;
import org.slasoi.slamodel.core.FunctionalExpr;
import org.slasoi.slamodel.primitives.CONST;
import org.slasoi.slamodel.primitives.ID;
import org.slasoi.slamodel.primitives.STND;
import org.slasoi.slamodel.primitives.UUID;
import org.slasoi.slamodel.sla.SLA;
import org.slasoi.slamodel.sla.SLATemplate;
import org.slasoi.slamodel.vocab.common;
import org.slasoi.slamodel.vocab.units;

public class TaskExecutor implements Runnable {

    private static Logger logger = Logger.getLogger(ProvisioningAdjustmentImpl.class.getName());

    private final String MONITORING_PATH = "MONITORING_PATH";

    private SLAManagerContext context;

    private IQuery query;

    private XMPP xmpp = XMPP.getInstance();

    SLATemplateRegistry registry;

    Messaging messaging;

    PubSubManager manager;

    Plan plan;

    Status planStatus;

    SLATemplate s_template;

    SLATemplate i_template;

    WorkloadMessageListener messageListener;

    JSONObject properties;

    JSONArray fromVM;

    JSONObject fromMONITORING;

    int id;

    public SLATemplate getBusinessSLAT() {
        return s_template;
    }

    public JSONArray getVMOutput() {
        return fromVM;
    }

    public JSONObject getMonitoringOutput() {
        return fromMONITORING;
    }

    public TaskExecutor(SLAManagerContext context, Plan plan, JSONObject properties, Messaging messaging, PubSubManager manager) {
        this.plan = plan;
        this.properties = properties;
        this.messaging = messaging;
        this.manager = manager;
        this.planStatus = Status.CREATED;
        this.context = context;
    }

    public void run() {
        this.planStatus = Status.PROVISIONING;
        Task rootTask = plan.getRootTask();
        Set<Task> tasks = new HashSet<Task>();
        tasks.add(rootTask);
        try {
            query = context.getSLARegistry().getIQuery();
            registry = context.getSLATemplateRegistry();
            boolean alldone = false;
            while (alldone == false) {
                Set<Task> newTasks = new HashSet<Task>();
                alldone = true;
                for (Task task : tasks) {
                    if (task.getStatus().equals(TaskStatus.CREATED.toString())) {
                        alldone = false;
                        Set<Task> parents = plan.getParents(task);
                        if (allDone(parents)) {
                            executeTask(task);
                            newTasks.addAll(plan.getChildren(task));
                        }
                    } else if (task.getStatus().equals(TaskStatus.PROVISIONING.toString())) {
                        alldone = false;
                    } else if (task.getStatus().equals(TaskStatus.PROVISIONING_FAILED.toString())) {
                        this.planStatus = Status.PROVISION_FAILED;
                        break;
                    }
                }
                if (this.planStatus == Status.PROVISION_FAILED) {
                    break;
                }
                tasks.addAll(newTasks);
                newTasks.clear();
            }
            if (this.planStatus == Status.PROVISIONING) {
                this.planStatus = Status.PROVISIONED;
            }
        } catch (TaskNotFoundException e) {
            this.planStatus = Status.PROVISION_FAILED;
            logger.error(e);
        } catch (SLAManagerContextException e) {
            this.planStatus = Status.PROVISION_FAILED;
            logger.error(e);
        }
        logger.info("PLAN STATUS: " + this.planStatus);
        xmpp.info("SPAC", "provisioning finished (" + s_template.getUuid().getValue() + ")", new Date());
    }

    public void executeTask(Task task) {
        boolean succed = false;
        task.setStatus(TaskStatus.PROVISIONING.toString());
        logger.info(task);
        if (task.getTaskId().equalsIgnoreCase("START")) {
            succed = START_TASK(task);
        } else if (task.getTaskId().equalsIgnoreCase("VM")) {
            succed = VM_Task(task);
        } else if (task.getTaskId().equalsIgnoreCase("MONITORING")) {
            succed = MONITORING_Task(task);
        } else if (task.getTaskId().equalsIgnoreCase("WORKLOAD")) {
            succed = WORKLOAD_Task(task);
        }
        logger.info(task.toString() + ": " + Boolean.toString(succed));
        if (succed) {
            task.setStatus(TaskStatus.PROVISIONED_OK.toString());
        } else {
            task.setStatus(TaskStatus.PROVISIONING_FAILED.toString());
        }
    }

    public boolean START_TASK(Task task) {
        try {
            SLA[] slas = query.getSLA(new UUID[] { new UUID(task.getSlaId()) });
            if (slas.length > 0) {
                s_template = slas[0];
                return true;
            }
        } catch (InvalidUUIDException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean VM_Task(Task task) {
        xmpp.info("SPAC", "provisioning started (" + s_template.getUuid().getValue() + ")", new Date());
        final String RESOURCES = "resources";
        PPMessageListener msgListener = new PPMessageListener();
        try {
            i_template = TemplateRegistry.getInstance().get(task.getSlaId());
            id = random(100);
            messaging.addMessageListener(msgListener);
            properties.put("hostname", String.format("orctest_vm_%d", id));
            String requestString = String.format("%s [%s]", "createmanycompute", properties);
            messaging.sendMessage(new Message("tashi@slasoi-testbed.xlab.si", requestString));
            org.slasoi.common.messaging.Message message = msgListener.getMessage();
            String response = message.getPayload();
            if (response != null) {
                JSONObject obj = new JSONObject(response);
                logger.info("RESPONSE FROM TASHI: " + response);
                if (obj.get("status").toString().equalsIgnoreCase("OK")) {
                    fromVM = obj.getJSONArray(RESOURCES);
                } else {
                    logger.error("ERROR STATUS RETURNED FROM TASHI: " + obj.get("message").toString());
                    return false;
                }
            } else {
                logger.error("NO RESPONSE FROM TASHI!");
                return false;
            }
        } catch (JSONException e) {
            logger.error(e);
            return false;
        } finally {
            messaging.removeMessageListener(msgListener);
        }
        return true;
    }

    private int random(int max) {
        return (int) (Math.random() * max);
    }

    private void reportMonitoring(JSONObject request, JSONObject response) {
        if (request == null || response == null) {
            return;
        }
        JSONObject result = new JSONObject();
        try {
            result.put("type", "monitoringRequest");
            result.put("timestamp", XMPP.dateToString(new Date()));
            result.put("slaId", s_template.getUuid().getValue());
            result.put("request", request);
            result.put("response", response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        xmpp.log(result.toString());
    }

    public boolean MONITORING_Task(Task task) {
        final String VM_LIST = "vmList";
        final String SERVICE_NAME = "serviceName";
        final String SERVICE_URL = "serviceUrl";
        final String FQDN = "fqdn";
        final String MESSAGE_ID = "messageId";
        UUID uuid = new UUID(java.util.UUID.randomUUID().toString());
        File monitoringFile = new File(System.getenv(MONITORING_PATH));
        if (fromVM == null || fromVM.length() == 0) {
            return false;
        }
        try {
            String resource = fromVM.getString(0);
            if (monitoringFile.isFile()) {
                byte[] buffer = new byte[(int) monitoringFile.length()];
                BufferedInputStream f = new BufferedInputStream(new FileInputStream(monitoringFile));
                f.read(buffer);
                JSONObject o = new JSONObject(new String(buffer));
                f.close();
                o.put(SERVICE_NAME, "orctest-" + Integer.toString(id));
                o.put(SERVICE_URL, "slasoi://xlab.si/Service/orctest-" + Integer.toString(id));
                o.put(MESSAGE_ID, uuid.toString());
                JSONArray vms = o.getJSONArray(VM_LIST);
                for (int i = 0; i < vms.length(); i++) {
                    JSONObject item = vms.getJSONObject(i);
                    item.put(FQDN, resource);
                }
                String request = o.toString();
                PubSubMessageListener pubSubListener = new PubSubMessageListener(new MessageFilter(uuid));
                manager.addMessageListener(pubSubListener, new String[] { XMPP.MONITORING_CHANNEL_NAME });
                PubSubMessage message = new PubSubMessage(XMPP.MONITORING_CHANNEL_NAME, request);
                manager.publish(message);
                xmpp.info("ISM", "monitoring request sent (" + s_template.getUuid().getValue() + ")", new Date());
                org.slasoi.common.messaging.Message response = pubSubListener.getMessage();
                if (response != null) {
                    String payload = response.getPayload();
                    logger.info("RESPONSE FROM MONITORING: " + payload);
                    fromMONITORING = new JSONObject(payload);
                    reportMonitoring(o, fromMONITORING);
                    if (fromMONITORING.get("responseType").toString().equalsIgnoreCase("Exception")) {
                        logger.error("ERROR STATUS RETURNED FROM MONITORING: " + new JSONObject(payload).get("exceptionMessage").toString());
                        return false;
                    }
                } else {
                    logger.error("NO RESPONSE FROM MONITORING!");
                    return false;
                }
                manager.removeMessageListener(pubSubListener);
            }
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
        return true;
    }

    public boolean WORKLOAD_Task(Task task) {
        boolean result = true;
        try {
            messageListener = new WorkloadMessageListener(this);
            manager.addMessageListener(messageListener);
            xmpp.info("SSM", "software monitoring engaged (" + task.getSlaId() + ")", new Date());
        } catch (Exception e) {
            result = false;
            logger.error(e);
        }
        return result;
    }

    private boolean allDone(Set<Task> tasks) {
        if (tasks == null || tasks.size() == 0) return true;
        boolean result = true;
        for (Task task : tasks) {
            if (!task.getStatus().equals(TaskStatus.PROVISIONED_OK.toString())) {
                logger.info(String.format("WATING ON TASK: %s", task.toString()));
                result = false;
            }
        }
        return result;
    }
}

interface IMessageFilter {

    boolean isAcceptable(org.slasoi.common.messaging.Message message);
}

class WorkloadMessageListener implements MessageListener {

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

    private static Logger logger = Logger.getLogger(ProvisioningAdjustmentImpl.class.getName());

    public WorkloadMessageListener(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        this.evaluator = new Evaluator(taskExecutor.s_template);
        this.uuid = taskExecutor.s_template.getUuid().getValue();
        loadConstraints();
        try {
            loadConstraints();
            taskExecutor.manager.publish(new PubSubMessage(XMPP.FRAMEWORK_LOGGING_CHANNEL, buildOnCreateMessage().toString()));
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
                try {
                    JSONObject json = buildViolationMessage(violation);
                    if (json != null) {
                        PubSubMessage response = new PubSubMessage(XMPP.FRAMEWORK_LOGGING_CHANNEL, json.toString());
                        taskExecutor.manager.publish(response);
                    }
                } catch (MessagingException e) {
                    logger.error(e);
                }
            }
        }
    }

    public void react(Violation violation) {
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

class DemoWorkflow {

    private HashSet<STND> operators;

    private Map<STND, Map<FunctionalExpr, List<Violation>>> violations;

    private Map<String, String> slaToDemo;

    public DemoWorkflow() {
        operators = new HashSet<STND>(Arrays.asList(new STND[] { common.arrival_rate, common.completion_time }));
        violations = new HashMap<STND, Map<FunctionalExpr, List<Violation>>>();
        slaToDemo = new HashMap<String, String>();
        slaToDemo.put("getProductDetails", "productDetails");
        slaToDemo.put("PaymentServiceOperation", "payment");
        slaToDemo.put("bookSale", "bookSale");
    }

    public String extractFunction(FunctionalExpr expr) {
        STND operator = expr.getOperator();
        if (operators.contains(operator)) {
            String function = ((ID) expr.getParameters()[0]).getValue().split("/")[1];
            return (slaToDemo.containsKey(function)) ? slaToDemo.get(function) : null;
        }
        return null;
    }

    public Map<FunctionalExpr, List<Violation>> processViolation(Violation violation) {
        String function = extractFunction(violation.getFunctionalExpr());
        if (function == null) {
            return null;
        }
        FunctionalExpr expr = violation.getFunctionalExpr();
        STND role = new STND(violation.getPartyID().getValue());
        Map<FunctionalExpr, List<Violation>> roleViolations;
        if (violations.containsKey(role)) {
            roleViolations = violations.get(role);
        } else {
            roleViolations = new HashMap<FunctionalExpr, List<Violation>>();
        }
        List<Violation> funcViolations;
        if (roleViolations.containsKey(expr)) {
            funcViolations = roleViolations.get(expr);
        } else {
            funcViolations = new ArrayList<Violation>();
        }
        funcViolations.add(violation);
        roleViolations.put(expr, funcViolations);
        violations.put(role, roleViolations);
        return roleViolations;
    }
}

class MessageFilter implements IMessageFilter {

    final String INREPLAYTO = "inReplyTo";

    UUID uuid;

    public MessageFilter(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isAcceptable(org.slasoi.common.messaging.Message message) {
        try {
            String payload = message.getPayload();
            JSONObject object = new JSONObject(payload);
            if (object.has(INREPLAYTO)) {
                String str = object.getString(INREPLAYTO);
                if (uuid.toString().equalsIgnoreCase(str)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
