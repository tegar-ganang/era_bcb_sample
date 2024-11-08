package com.sxi.cometd.pages.list;

import java.io.Serializable;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.push.ChannelEvent;
import com.sxi.cometd.pages.DigiBasePage;
import com.sxi.cometd.pages.utils.OverrideConstants;
import com.sxi.cometd.utils.RemoteConstants;
import com.sxi.override.digibanker.model.TestTransaction;
import com.sxi.override.digibanker.model.ovrd.OverrideBean;
import com.sxi.override.digibanker.model.ovrd.OverrideHeader;
import com.sxi.override.digibanker.model.ovrd.OverrideModel;
import com.sxi.override.digibanker.service.log.OverrideTrackingService;
import com.sxi.override.digibanker.service.ovrd.OverrideService;

/**
 * @author Emmanuel Nollase - emanux
 * created 2009 7 22 - 16:27:27
 */
public class ViewOverridePage extends DigiBasePage {

    private static final Log log = LogFactory.getLog(ViewOverridePage.class);

    @SpringBean
    private OverrideTrackingService trackingService;

    @SpringBean
    private OverrideService overrideService;

    public ViewOverridePage(PageParameters parameters, final OverrideModel model) {
        super(parameters);
        final TestTransaction _txn = (TestTransaction) SerializationUtils.deserialize(model.getHdrMdl());
        add(new Label("tranCode", _txn.getTranCode()));
        add(new Label("tranType", _txn.getTranType()));
        add(new Label("tranAmt", _txn.getTranAmt().toString()));
        add(new Label("tranDscp", _txn.getTranDscp()));
        final OverrideBean ovrdbean = new OverrideBean();
        ovrdbean.setSupervisor(getUser());
        final Form form = new Form("supform", new CompoundPropertyModel(ovrdbean));
        add(form);
        form.add(new Label("supervisor"));
        final RequiredTextField pass = new RequiredTextField("password");
        form.add(pass);
        final String requestee = model.getRequestBy();
        final OverrideHeader ovrdHead = trackingService.findOverrideHeader(model.getRefNo());
        final AjaxButton accept = new AjaxButton("accept", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                boolean apprv = trackingService.acceptTransaction(ovrdHead, model.getOvrdKey());
                if (apprv) {
                    final ChannelEvent event = new ChannelEvent(requestee);
                    event.addData(RemoteConstants.ACCEPTED, "true");
                    event.addData("Transaction", model.getReason());
                    event.addData("Transaction Date", model.getTxnDt().toString());
                    event.addData("Reference Number", model.getRefNo());
                    event.addData("Supervisor", getUser());
                    getChannelService().publish(event);
                    model.setStatus(1);
                    overrideService.update(model);
                    setRedirect(true);
                    setResponsePage(ListOverridePage.class);
                } else {
                    target.appendJavascript("sxicometd.alerts.failed('Sorry, override failed.')");
                }
            }
        };
        form.add(accept);
        final AjaxButton reject = new AjaxButton("reject", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                boolean reject = trackingService.rejectTransaction(ovrdHead, model.getOvrdKey());
                if (reject) {
                    final ChannelEvent event = new ChannelEvent(requestee);
                    event.addData(RemoteConstants.ACCEPTED, "false");
                    event.addData("Transaction", _txn.getTranType());
                    event.addData("Refereence Number", model.getRefNo());
                    event.addData("From", getUser());
                    getChannelService().publish(event);
                    model.setStatus(4);
                    overrideService.update(model);
                    setRedirect(true);
                    setResponsePage(ListOverridePage.class);
                } else {
                    target.appendJavascript("sxicometd.alerts.failed('Sorry, override failed.')");
                }
            }
        };
        form.add(reject);
    }
}
