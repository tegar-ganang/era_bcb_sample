package com.sxi.cometd.pages;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.push.ChannelEvent;
import org.wicketstuff.push.IChannelService;
import com.sxi.cometd.pages.utils.LoggedUsers;
import com.sxi.cometd.pages.utils.OverrideConstants;
import com.sxi.cometd.pages.utils.OverrideEntryParams;
import com.sxi.cometd.pages.utils.OverrideGenerator;
import com.sxi.cometd.utils.CometdUtils;
import com.sxi.cometd.utils.OverrideTypes;
import com.sxi.cometd.utils.RemoteConstants;
import com.sxi.override.digibanker.exception.OverrideException;
import com.sxi.override.digibanker.model.BaseModel;
import com.sxi.override.digibanker.model.TestTransaction;
import com.sxi.override.digibanker.model.ovrd.OverrideBean;
import com.sxi.override.digibanker.model.ovrd.OverrideHeader;
import com.sxi.override.digibanker.model.ovrd.OverrideModel;
import com.sxi.override.digibanker.service.log.OverrideTrackingService;
import com.sxi.override.digibanker.service.ovrd.OverrideService;

/**
 * 
 * @author Emmanuel Nollase - emanux
 * created 2009 7 20 - 14:36:14
 */
public class DigibankerOverridePage extends DigiBasePage {

    private static final Log log = LogFactory.getLog(DigibankerOverridePage.class);

    @SpringBean
    private OverrideService overrideService;

    @SpringBean
    private OverrideTrackingService trackingService;

    public DigibankerOverridePage(final BaseModel baseModel, PageParameters parameters) {
        super(parameters);
        final OverrideEntryParams params = new OverrideEntryParams();
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        add(feedback.setOutputMarkupId(true));
        final int reqOvrdCnt = 3;
        final TestTransaction _txn = (TestTransaction) baseModel;
        final Form form = new Form("txnform", new CompoundPropertyModel(_txn));
        add(form);
        form.add(new Label("tranCode"));
        form.add(new Label("tranType"));
        form.add(new Label("tranAmt"));
        form.add(new Label("tranDscp"));
        final String funccd = _txn.getTranCode();
        final String refNo = OverrideGenerator.generateOvrdTransaction(funccd);
        params.setFuncId(funccd);
        params.setReqOvrdCnt(reqOvrdCnt);
        params.setSubmittedBy(getUser());
        final OverrideModel ovrdModel = new OverrideModel();
        final Form ovrdForm = new Form("override", new CompoundPropertyModel(ovrdModel));
        add(ovrdForm);
        final ListView listOverride = new ListView("listOverride", overrideService.numberOfOverrides(reqOvrdCnt)) {

            @Override
            protected void populateItem(final ListItem item) {
                final OverrideBean overmodel = (OverrideBean) item.getModelObject();
                item.setModel(new CompoundPropertyModel(overmodel));
                final RadioGroup overrideTypeRadios = new RadioGroup("overrideType");
                final Radio localOverride = new Radio("localOverride", new Model(OverrideTypes.LOCAL_OVERRIDE));
                overrideTypeRadios.add(localOverride);
                final Radio queuedOverride = new Radio("queuedOverride", new Model(OverrideTypes.QUEUED_OVERRIDE));
                overrideTypeRadios.add(queuedOverride);
                final Radio remoteOverride = new Radio("remoteOverride", new Model(OverrideTypes.REMOTE_OVERRIDE));
                overrideTypeRadios.add(remoteOverride);
                item.add(overrideTypeRadios);
                final DropDownChoice username = new DropDownChoice("supervisor", LoggedUsers.LOGGED_USERS);
                item.add(username);
                final TextField password = new TextField("password");
                password.setOutputMarkupId(true);
                password.setEnabled(false);
                item.add(password);
                overrideTypeRadios.add(new AjaxFormChoiceComponentUpdatingBehavior() {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        final String overridetype = overrideTypeRadios.getModelObjectAsString();
                        if (StringUtils.equals(overridetype, OverrideTypes.LOCAL_OVERRIDE)) {
                            password.setEnabled(true);
                        } else {
                            password.setEnabled(false);
                        }
                        target.addComponent(password);
                    }
                });
                final AjaxButton submit = new AjaxButton("submit", ovrdForm) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        final String overridetype = overrideTypeRadios.getModelObjectAsString();
                        final String toUser = ((OverrideBean) item.getModelObject()).getSupervisor();
                        if (StringUtils.isEmpty(toUser)) {
                            target.appendJavascript("sxicometd.alerts.failed('Please select supervisor name')");
                            return;
                        }
                        final int actualCnt = trackingService.countOverrideEntry(refNo);
                        if (actualCnt >= reqOvrdCnt) {
                            target.appendJavascript("sxicometd.alerts.failed('Required override reach.')");
                            return;
                        }
                        params.setSubmittedTo(toUser);
                        params.setRefNo(refNo);
                        if (StringUtils.equals(overridetype, OverrideTypes.REMOTE_OVERRIDE)) {
                            final OverrideModel remotemodel = new OverrideModel();
                            params.setOvrdkey(OverrideGenerator.generateOvrdKey());
                            remotemodel.setOvrdType(OverrideTypes.REMOTE_OVERRIDE);
                            serializeObject(baseModel, remotemodel);
                            boolean _success = overrideService.save(remotemodel, params);
                            if (_success) {
                                try {
                                    createEntry(params);
                                    final ChannelEvent event = new ChannelEvent(toUser);
                                    event.addData("Ref No.", refNo);
                                    event.addData(RemoteConstants.OUTGOING, "true");
                                    event.addData("tranCode", _txn.getTranCode());
                                    event.addData("tranType", _txn.getTranType());
                                    event.addData("tranAmt", String.valueOf(_txn.getTranAmt()));
                                    event.addData("tranDscp", _txn.getTranDscp());
                                    event.addData("RequestFrom", getUser());
                                    event.addData("RequestTo", toUser);
                                    getChannelService().publish(event);
                                    target.appendJavascript("sxicometd.alerts.success('" + CometdUtils.requestAlertSuccess(event.getData()) + "')");
                                } catch (OverrideException e) {
                                    target.appendJavascript("sxicometd.alerts.failed('" + e.getMessage() + "')");
                                    log.error(e);
                                }
                            } else {
                                target.appendJavascript("sxicometd.alerts.failed('Override request failed')");
                            }
                        }
                        if (StringUtils.equals(overridetype, OverrideTypes.QUEUED_OVERRIDE)) {
                            params.setOvrdkey(OverrideGenerator.generateOvrdKey());
                            final OverrideModel queuedmodel = new OverrideModel();
                            queuedmodel.setOvrdType(OverrideTypes.QUEUED_OVERRIDE);
                            serializeObject(baseModel, queuedmodel);
                            boolean _success = overrideService.save(queuedmodel, params);
                            if (_success) {
                                try {
                                    createEntry(params);
                                    final Map<String, String> alert = new HashMap<String, String>();
                                    alert.put("Ref No.", refNo);
                                    alert.put("tranCode", _txn.getTranCode());
                                    alert.put("tranType", _txn.getTranType());
                                    alert.put("tranAmt", String.valueOf(_txn.getTranAmt()));
                                    alert.put("tranDscp", _txn.getTranDscp());
                                    target.appendJavascript("sxicometd.alerts.success('" + CometdUtils.requestAlertSuccess(alert) + "')");
                                } catch (OverrideException e) {
                                    log.error(e);
                                    target.appendJavascript("sxicometd.alerts.failed('" + e.getMessage() + "')");
                                }
                            } else {
                                target.appendJavascript("sxicometd.alerts.failed('Override request failed')");
                            }
                        }
                        if (StringUtils.equals(overridetype, OverrideTypes.LOCAL_OVERRIDE)) {
                            params.setOvrdkey(OverrideGenerator.generateOvrdKey());
                            params.setOvrdType(OverrideTypes.LOCAL_OVERRIDE);
                            try {
                                final OverrideHeader header = trackingService.findOverrideHeader(refNo);
                                if (header.getOvrdStatus() == OverrideConstants.OVRD_STATUS_REJECTED) {
                                    target.appendJavascript("sxicometd.alerts.failed('Transaction has already been rejected')");
                                    return;
                                }
                                createEntry(params);
                                boolean _check = trackingService.checkLocalOverride(params.getRefNo(), params.getOvrdkey());
                                if (_check) {
                                    final Map<String, String> alert = new HashMap<String, String>();
                                    alert.put("Ref No.", refNo);
                                    alert.put("tranCode", _txn.getTranCode());
                                    alert.put("tranType", _txn.getTranType());
                                    alert.put("tranAmt", String.valueOf(_txn.getTranAmt()));
                                    alert.put("tranDscp", _txn.getTranDscp());
                                    target.appendJavascript("sxicometd.alerts.success('" + CometdUtils.requestAlertSuccess(alert) + "')");
                                } else {
                                    target.appendJavascript("sxicometd.alerts.failed('Override request failed')");
                                }
                            } catch (OverrideException e) {
                                log.error(e);
                                target.appendJavascript("sxicometd.alerts.failed('" + e.getMessage() + "')");
                            }
                        }
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form form) {
                        target.addComponent(feedback);
                        super.onError(target, form);
                    }
                };
                item.add(submit);
                item.add(new AjaxButton("cancel", ovrdForm) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                    }
                }.setDefaultFormProcessing(false));
            }
        };
        ovrdForm.add(listOverride);
    }

    /**
	 * 
	 * @param baseModel
	 * @param overrideModel
	 */
    private void serializeObject(BaseModel baseModel, OverrideModel overrideModel) {
        overrideModel.setHdrMdl(SerializationUtils.serialize(baseModel));
    }

    private void createEntry(OverrideEntryParams params) throws OverrideException {
        try {
            trackingService.createOverrideEntry(params);
        } catch (Exception e) {
            throw new OverrideException(e);
        }
    }
}
