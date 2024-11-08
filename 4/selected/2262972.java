package com.dcivision.framework.taglib.channel;

import com.dcivision.framework.web.AbstractSearchForm;
import com.dcivision.framework.web.ListPersonalHomeForm;
import com.dcivision.workflow.web.ListWorkflowProgressForm;

public class AjaxFormAdapter {

    public static AbstractSearchForm getChannelAdapterForm(String filterName) {
        if (AjaxConstant.TASKINBOXFILTER.equals(filterName)) {
            return new ListWorkflowProgressForm();
        } else {
            return new ListPersonalHomeForm();
        }
    }
}
