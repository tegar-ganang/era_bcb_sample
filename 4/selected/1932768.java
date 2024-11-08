package org.orangegears.gl.control.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.orangegears.parser.ODSSpreadSheetParser;
import org.orangegears.parser.SpreadSheetParser;
import org.orangegears.parser.XLSSpreadSheetParser;

public class ReportTemplateService {

    private static final String TYPE_ONE = "ONE";

    private static final String TYPE_TWO = "TWO";

    private static final String ACCOUNT_ENTITY_ID = "accountEntityId";

    public static Map<String, Object> upload(DispatchContext dctx, Map<String, Object> context) {
        String partyId = (String) context.get(ACCOUNT_ENTITY_ID);
        String templateTypeId = "";
        if (context.get("single") != null && context.get("single").equals("Y")) {
            templateTypeId = "ONE";
        } else if (context.get("period") != null && context.get("period").equals("Y")) {
            templateTypeId = "TWO";
        } else return ServiceUtil.returnError("Template type missing");
        String templateName = (String) context.get("templateName");
        String contentType = (String) context.get("_templateFile_contentType");
        GenericDelegator delegator = dctx.getDelegator();
        String seqId = delegator.getNextSeqId("ReportTemplate");
        String fileName = "";
        if (contentType.equals("application/vnd.oasis.opendocument.spreadsheet")) fileName = seqId + ".ods"; else if (contentType.equals("application/vnd.ms-excel")) fileName = seqId + ".xls"; else return ServiceUtil.returnError("File format is not supported");
        List<GenericValue> similarName = null;
        try {
            similarName = delegator.findList("ReportTemplate", new EntityConditionList<EntityExpr>(UtilMisc.<EntityExpr>toList(new EntityExpr("templateName", EntityOperator.EQUALS, templateName), new EntityExpr("partyId", EntityOperator.EQUALS, partyId)), EntityOperator.AND), UtilMisc.toSet("fileName", "templateTypeId", "fileContentType"), null, null, false);
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        if (similarName.size() > 0) {
            String oldFileName = dctx.getClass().getResource("/runtime/upload").getPath();
            oldFileName += "/" + similarName.get(0).get("fileName");
            File templateFile = new File(oldFileName);
            templateFile.delete();
            try {
                delegator.removeByCondition("ReportTemplate", new EntityConditionList<EntityExpr>(UtilMisc.<EntityExpr>toList(new EntityExpr("templateName", EntityOperator.EQUALS, templateName), new EntityExpr("partyId", EntityOperator.EQUALS, partyId)), EntityJoinOperator.AND));
            } catch (GenericEntityException e) {
                e.printStackTrace();
            }
        }
        ByteBuffer bb = (ByteBuffer) context.get("templateFile");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(dctx.getClass().getResource("/runtime/upload").getPath() + "/" + fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fos.write(bb.array());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        GenericValue reportTemplate = delegator.makeValue("ReportTemplate");
        reportTemplate.set("templateId", seqId);
        reportTemplate.set("templateName", templateName);
        reportTemplate.set("partyId", partyId);
        reportTemplate.set("fileName", fileName);
        reportTemplate.set("templateTypeId", templateTypeId);
        reportTemplate.set("fileContentType", contentType);
        try {
            delegator.create(reportTemplate);
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        return ServiceUtil.returnSuccess("Template Upload Complete");
    }

    public static Map<String, Object> download(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result;
        String templateName = (String) context.get("templateName");
        String partyId = (String) context.get(ACCOUNT_ENTITY_ID);
        GenericDelegator delegator = dctx.getDelegator();
        List<GenericValue> reportTemplate = null;
        try {
            reportTemplate = delegator.findList("ReportTemplate", new EntityConditionList<EntityExpr>(UtilMisc.<EntityExpr>toList(new EntityExpr("templateName", EntityOperator.EQUALS, templateName), new EntityExpr("partyId", EntityOperator.EQUALS, partyId)), EntityOperator.AND), UtilMisc.toSet("fileName", "fileContentType"), null, null, false);
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        String fileName = dctx.getClass().getResource("/runtime/upload").getPath();
        fileName += "/" + reportTemplate.get(0).get("fileName");
        File templateFile = new File(fileName);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(templateFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        FileChannel fc = fis.getChannel();
        ByteBuffer bb = null;
        try {
            bb = ByteBuffer.allocate((int) fc.size());
            fc.read(bb);
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        result = ServiceUtil.returnSuccess("Donwload Template Success");
        result.put("templateFile", bb);
        result.put("fileContentType", reportTemplate.get(0).get("fileContentType"));
        return result;
    }

    public static Map<String, Object> processReport(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = null;
        String partyId = (String) context.get(ACCOUNT_ENTITY_ID);
        String templateName = (String) context.get("templateName");
        GenericDelegator delegator = dctx.getDelegator();
        List<GenericValue> reportTemplate = null;
        try {
            reportTemplate = delegator.findList("ReportTemplate", new EntityConditionList<EntityExpr>(UtilMisc.<EntityExpr>toList(new EntityExpr("templateName", EntityOperator.EQUALS, templateName), new EntityExpr("partyId", EntityOperator.EQUALS, partyId)), EntityOperator.AND), UtilMisc.toSet("fileName", "templateTypeId", "fileContentType"), null, null, false);
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        if (reportTemplate == null || reportTemplate.size() == 0) return ServiceUtil.returnError("Can't find template!");
        String fileName = dctx.getClass().getResource("/runtime/upload").getPath();
        fileName += "/" + reportTemplate.get(0).get("fileName");
        File templateFile = new File(fileName);
        String templateType = (String) reportTemplate.get(0).get("templateTypeId");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String glFiscalTypeId = (String) context.get("glFiscalTypeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> resultMap = null;
        Map<String, Object> inputMap = null;
        if (templateType.equals(TYPE_ONE)) {
            try {
                resultMap = dispatcher.runSync("prepareBalanceSheetData", UtilMisc.toMap("organizationPartyId", partyId, "thruDate", context.get("thruDate"), "glFiscalTypeId", glFiscalTypeId, "userLogin", userLogin));
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
            inputMap = (Map<String, Object>) resultMap.get("balancesMap");
        } else if (templateType.equals(TYPE_TWO)) {
            try {
                resultMap = dispatcher.runSync("prepareIncomeStatementData", UtilMisc.toMap("organizationPartyId", partyId, "fromDate", context.get("fromDate"), "thruDate", context.get("thruDate"), "glFiscalTypeId", glFiscalTypeId, "userLogin", userLogin));
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
            inputMap = (Map<String, Object>) resultMap.get("glAccountTotalsMap");
        }
        SpreadSheetParser ssParser = null;
        String contentType = (String) reportTemplate.get(0).get("fileContentType");
        if (contentType.equals("application/vnd.oasis.opendocument.spreadsheet")) ssParser = new ODSSpreadSheetParser(templateFile, inputMap); else if (contentType.equals("application/vnd.ms-excel")) ssParser = new XLSSpreadSheetParser(templateFile, inputMap); else return ServiceUtil.returnError("Unsupport file format");
        File output = ssParser.getParsedFile();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        FileChannel fc = fis.getChannel();
        ByteBuffer bb = null;
        try {
            bb = ByteBuffer.allocate((int) fc.size());
            fc.read(bb);
            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        result = ServiceUtil.returnSuccess("Donwload Template Success");
        result.put("parsedFile", bb);
        result.put("fileContentType", contentType);
        return result;
    }
}
