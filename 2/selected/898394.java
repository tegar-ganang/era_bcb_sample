package org.ofbiz.product.product;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProductsExportToGoogle {

    private static final String resource = "ProductUiLabels";

    private static final String module = ProductsExportToGoogle.class.getName();

    public static Map exportToGoogle(DispatchContext dctx, Map context) {
        Locale locale = (Locale) context.get("locale");
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map result = null;
        try {
            String configString = "productsExport.properties";
            String developerKey = UtilProperties.getPropertyValue(configString, "productsExport.google.developerKey");
            String authenticationUrl = UtilProperties.getPropertyValue(configString, "productsExport.google.authenticationUrl");
            String accountEmail = UtilProperties.getPropertyValue(configString, "productsExport.google.accountEmail");
            String accountPassword = UtilProperties.getPropertyValue(configString, "productsExport.google.accountPassword");
            String postItemsUrl = UtilProperties.getPropertyValue(configString, "productsExport.google.postItemsUrl");
            StringBuffer dataItemsXml = new StringBuffer();
            result = buildDataItemsXml(dctx, context, dataItemsXml);
            if (!ServiceUtil.isFailure(result)) {
                String token = authenticate(authenticationUrl, accountEmail, accountPassword);
                if (token != null) {
                    result = postItem(token, postItemsUrl, developerKey, dataItemsXml, locale, (String) context.get("testMode"), (List) result.get("newProductsInGoogle"), (List) result.get("productsRemovedFromGoogle"), dispatcher, delegator);
                } else {
                    Debug.logError("Error during authentication to Google Account", module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.errorDuringAuthenticationToGoogle", locale));
                }
            } else {
                return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
            }
        } catch (Exception e) {
            Debug.logError("Exception in exportToGoogle", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionInExportToGoogle", locale));
        }
        return result;
    }

    public static Map exportProductCategoryToGoogle(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String productCategoryId = (String) context.get("productCategoryId");
        String actionType = (String) context.get("actionType");
        String webSiteUrl = (String) context.get("webSiteUrl");
        String imageUrl = (String) context.get("imageUrl");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.cannotRetrieveUserLogin", locale));
        }
        try {
            if (UtilValidate.isNotEmpty(productCategoryId)) {
                List productsList = FastList.newInstance();
                Map result = dispatcher.runSync("getProductCategoryMembers", UtilMisc.toMap("categoryId", productCategoryId));
                if (result.get("categoryMembers") != null) {
                    List productCategoryMembers = (List) result.get("categoryMembers");
                    if (productCategoryMembers != null) {
                        Iterator i = productCategoryMembers.iterator();
                        while (i.hasNext()) {
                            GenericValue prodCatMemb = (GenericValue) i.next();
                            if (prodCatMemb != null) {
                                String productId = prodCatMemb.getString("productId");
                                if (productId != null) {
                                    GenericValue prod = prodCatMemb.getRelatedOne("Product");
                                    Timestamp salesDiscontinuationDate = prod.getTimestamp("salesDiscontinuationDate");
                                    if (salesDiscontinuationDate == null) {
                                        productsList.add(productId);
                                    }
                                }
                            }
                        }
                    }
                }
                if (productsList.size() == 0) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.noProductsAvailableInProductCategory", locale));
                } else {
                    Map paramIn = FastMap.newInstance();
                    paramIn.put("selectResult", productsList);
                    paramIn.put("webSiteUrl", webSiteUrl);
                    paramIn.put("imageUrl", imageUrl);
                    paramIn.put("actionType", actionType);
                    paramIn.put("statusId", "publish");
                    paramIn.put("testMode", "N");
                    paramIn.put("userLogin", userLogin);
                    result = dispatcher.runSync("exportToGoogle", paramIn);
                    if (ServiceUtil.isError(result)) {
                        return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(result));
                    }
                }
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.missingParameterProductCategoryId", locale));
            }
        } catch (Exception e) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionInExportProductCategoryToGoogle", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    private static String authenticate(String authenticationUrl, String accountEmail, String accountPassword) {
        String postOutput = null;
        String token = null;
        try {
            postOutput = makeLoginRequest(authenticationUrl, accountEmail, accountPassword);
        } catch (IOException e) {
            Debug.logError("Could not connect to authentication server: " + e.toString(), module);
            return token;
        }
        StringTokenizer tokenizer = new StringTokenizer(postOutput, "=\n ");
        while (tokenizer.hasMoreElements()) {
            if ("Auth".equals(tokenizer.nextToken())) {
                if (tokenizer.hasMoreElements()) {
                    token = tokenizer.nextToken();
                }
                break;
            }
        }
        if (token == null) {
            Debug.logError("Authentication error. Response from server:\n" + postOutput, module);
        }
        return token;
    }

    private static String makeLoginRequest(String authenticationUrl, String accountEmail, String accountPassword) throws IOException {
        URL url = new URL(authenticationUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        StringBuffer content = new StringBuffer();
        content.append("Email=").append(URLEncoder.encode(accountEmail, "UTF-8"));
        content.append("&Passwd=").append(URLEncoder.encode(accountPassword, "UTF-8"));
        content.append("&source=").append(URLEncoder.encode("Google Base data API for OFBiz", "UTF-8"));
        content.append("&service=").append(URLEncoder.encode("gbase", "UTF-8"));
        OutputStream outputStream = urlConnection.getOutputStream();
        outputStream.write(content.toString().getBytes("UTF-8"));
        outputStream.close();
        int responseCode = urlConnection.getResponseCode();
        InputStream inputStream;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = urlConnection.getInputStream();
        } else {
            inputStream = urlConnection.getErrorStream();
        }
        return toString(inputStream);
    }

    private static String toString(InputStream inputStream) throws IOException {
        String string;
        StringBuffer outputBuilder = new StringBuffer();
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (null != (string = reader.readLine())) {
                outputBuilder.append(string).append('\n');
            }
        }
        return outputBuilder.toString();
    }

    private static Map postItem(String token, String postItemsUrl, String developerKey, StringBuffer dataItems, Locale locale, String testMode, List newProductsInGoogle, List productsRemovedFromGoogle, LocalDispatcher dispatcher, GenericDelegator delegator) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (new URL(postItemsUrl)).openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/atom+xml");
        connection.setRequestProperty("Authorization", "GoogleLogin auth=" + token);
        connection.setRequestProperty("X-Google-Key", "key=" + developerKey);
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(dataItems.toString().getBytes());
        outputStream.close();
        int responseCode = connection.getResponseCode();
        InputStream inputStream;
        Map result = FastMap.newInstance();
        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
            inputStream = connection.getInputStream();
            String response = toString(inputStream);
            if (response != null && response.length() > 0) {
                result = readResponseFromGoogle(response, newProductsInGoogle, productsRemovedFromGoogle, dispatcher, delegator, locale);
                if (ServiceUtil.isError(result)) {
                    result = ServiceUtil.returnFailure((List) result.get(ModelService.ERROR_MESSAGE_LIST));
                } else {
                    return ServiceUtil.returnSuccess();
                }
            }
        } else {
            inputStream = connection.getErrorStream();
            result = ServiceUtil.returnFailure(toString(inputStream));
        }
        return result;
    }

    private static Map buildDataItemsXml(DispatchContext dctx, Map context, StringBuffer dataItemsXml) {
        Locale locale = (Locale) context.get("locale");
        List newProductsInGoogle = FastList.newInstance();
        List productsRemovedFromGoogle = FastList.newInstance();
        try {
            GenericDelegator delegator = dctx.getDelegator();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            List selectResult = (List) context.get("selectResult");
            String webSiteUrl = (String) context.get("webSiteUrl");
            String imageUrl = (String) context.get("imageUrl");
            String actionType = (String) context.get("actionType");
            String statusId = (String) context.get("statusId");
            String trackingCodeId = (String) context.get("trackingCodeId");
            if (!webSiteUrl.startsWith("http://") && !webSiteUrl.startsWith("https://")) {
                webSiteUrl = "http://" + webSiteUrl;
            }
            if (webSiteUrl.endsWith("/")) {
                webSiteUrl = webSiteUrl.substring(0, webSiteUrl.length() - 1);
            }
            List productsList = delegator.findByCondition("Product", new EntityExpr("productId", EntityOperator.IN, selectResult), null, null);
            if (UtilValidate.isEmpty(trackingCodeId) || "_NA_".equals(trackingCodeId)) {
                trackingCodeId = "";
            } else {
                trackingCodeId = "?atc=" + trackingCodeId;
            }
            try {
                Document feedDocument = UtilXml.makeEmptyXmlDocument("feed");
                Element feedElem = feedDocument.getDocumentElement();
                feedElem.setAttribute("xmlns", "http://www.w3.org/2005/Atom");
                feedElem.setAttribute("xmlns:openSearch", "http://a9.com/-/spec/opensearchrss/1.0/");
                feedElem.setAttribute("xmlns:g", "http://base.google.com/ns/1.0");
                feedElem.setAttribute("xmlns:batch", "http://schemas.google.com/gdata/batch");
                feedElem.setAttribute("xmlns:app", "http://purl.org/atom/app#");
                Iterator productsListItr = productsList.iterator();
                int index = 0;
                String itemActionType = null;
                while (productsListItr.hasNext()) {
                    itemActionType = actionType;
                    GenericValue prod = (GenericValue) productsListItr.next();
                    String price = getProductPrice(dispatcher, prod);
                    if (price == null) {
                        Debug.logInfo("Price not found for product [" + prod.getString("productId") + "]; product will not be exported.", module);
                        continue;
                    }
                    String link = webSiteUrl + "/ecommerce/control/product/~product_id=" + prod.getString("productId") + trackingCodeId;
                    String title = UtilFormatOut.encodeXmlValue(prod.getString("productName"));
                    String description = UtilFormatOut.encodeXmlValue(prod.getString("description"));
                    String imageLink = "";
                    if (UtilValidate.isNotEmpty(prod.getString("largeImageUrl"))) {
                        imageLink = webSiteUrl + prod.getString("largeImageUrl");
                    } else if (UtilValidate.isNotEmpty(prod.getString("mediumImageUrl"))) {
                        imageLink = webSiteUrl + prod.getString("mediumImageUrl");
                    } else if (UtilValidate.isNotEmpty(prod.getString("smallImageUrl"))) {
                        imageLink = webSiteUrl + prod.getString("smallImageUrl");
                    }
                    String googleProductId = null;
                    if (!"insert".equals(actionType)) {
                        try {
                            GenericValue googleProduct = delegator.findByPrimaryKey("GoodIdentification", UtilMisc.toMap("productId", prod.getString("productId"), "goodIdentificationTypeId", "GOOGLE_ID"));
                            if (UtilValidate.isNotEmpty(googleProduct)) {
                                googleProductId = googleProduct.getString("idValue");
                            }
                        } catch (GenericEntityException gee) {
                            Debug.logError("Unable to get the Google id for product [" + prod.getString("productId") + "]: " + gee.getMessage(), module);
                        }
                    }
                    if ("update".equals(actionType) && UtilValidate.isEmpty(googleProductId)) {
                        itemActionType = "insert";
                    }
                    Element entryElem = UtilXml.addChildElement(feedElem, "entry", feedDocument);
                    Element batchElem = UtilXml.addChildElement(entryElem, "batch:operation", feedDocument);
                    batchElem.setAttribute("type", itemActionType);
                    if (statusId != null && ("draft".equals(statusId) || "deactivate".equals(statusId))) {
                        Element appControlElem = UtilXml.addChildElement(entryElem, "app:control", feedDocument);
                        UtilXml.addChildElementValue(appControlElem, "app:draft", "yes", feedDocument);
                        if ("deactivate".equals(statusId)) {
                            UtilXml.addChildElement(appControlElem, "gm:disapproved", feedDocument);
                        }
                    }
                    UtilXml.addChildElementValue(entryElem, "title", title, feedDocument);
                    Element contentElem = UtilXml.addChildElementValue(entryElem, "content", description, feedDocument);
                    contentElem.setAttribute("type", "xhtml");
                    if (UtilValidate.isNotEmpty(googleProductId)) {
                        UtilXml.addChildElementValue(entryElem, "id", googleProductId, feedDocument);
                    } else {
                        UtilXml.addChildElementValue(entryElem, "id", link, feedDocument);
                    }
                    Element linkElem = UtilXml.addChildElement(entryElem, "link", feedDocument);
                    linkElem.setAttribute("rel", "alternate");
                    linkElem.setAttribute("type", "text/html");
                    linkElem.setAttribute("href", link);
                    UtilXml.addChildElementValue(entryElem, "g:item_type", "products", feedDocument);
                    UtilXml.addChildElementValue(entryElem, "g:price", price, feedDocument);
                    if (UtilValidate.isNotEmpty(imageLink)) {
                        UtilXml.addChildElementValue(entryElem, "g:image_link", imageLink, feedDocument);
                    }
                    if ("insert".equals(itemActionType)) {
                        newProductsInGoogle.add(prod.getString("productId"));
                        productsRemovedFromGoogle.add(null);
                    } else if ("delete".equals(itemActionType)) {
                        newProductsInGoogle.add(null);
                        productsRemovedFromGoogle.add(prod.getString("productId"));
                    } else {
                        newProductsInGoogle.add(null);
                        productsRemovedFromGoogle.add(null);
                    }
                    index++;
                }
                dataItemsXml.append(UtilXml.writeXmlDocument(feedDocument));
            } catch (Exception e) {
                Debug.logError("Exception during building data items to Google", module);
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionDuringBuildingDataItemsToGoogle", locale));
            }
        } catch (Exception e) {
            Debug.logError("Exception during building data items to Google", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionDuringBuildingDataItemsToGoogle", locale));
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("newProductsInGoogle", newProductsInGoogle);
        result.put("productsRemovedFromGoogle", productsRemovedFromGoogle);
        return result;
    }

    private static String getProductPrice(LocalDispatcher dispatcher, GenericValue product) {
        String priceString = null;
        try {
            Map map = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product));
            boolean validPriceFound = ((Boolean) map.get("validPriceFound")).booleanValue();
            boolean isSale = ((Boolean) map.get("isSale")).booleanValue();
            if (validPriceFound) {
                priceString = UtilFormatOut.formatPrice((Double) map.get("price"));
            }
        } catch (Exception e) {
            Debug.logError("Exception calculating price for product [" + product.getString("productId") + "]", module);
        }
        return priceString;
    }

    private static Map readResponseFromGoogle(String msg, List newProductsInGoogle, List productsRemovedFromGoogle, LocalDispatcher dispatcher, GenericDelegator delegator, Locale locale) {
        List message = FastList.newInstance();
        try {
            Document docResponse = UtilXml.readXmlDocument(msg, true);
            Element elemResponse = docResponse.getDocumentElement();
            List atomEntryList = UtilXml.childElementList(elemResponse, "atom:entry");
            Iterator atomEntryElemIter = atomEntryList.iterator();
            int index = 0;
            while (atomEntryElemIter.hasNext()) {
                Element atomEntryElement = (Element) atomEntryElemIter.next();
                String id = UtilXml.childElementValue(atomEntryElement, "atom:id", "");
                if (UtilValidate.isNotEmpty(id) && newProductsInGoogle.get(index) != null) {
                    String productId = (String) newProductsInGoogle.get(index);
                    try {
                        GenericValue googleProductId = delegator.makeValue("GoodIdentification", null);
                        googleProductId.set("goodIdentificationTypeId", "GOOGLE_ID");
                        googleProductId.set("productId", productId);
                        googleProductId.set("idValue", id);
                        delegator.createOrStore(googleProductId);
                    } catch (GenericEntityException gee) {
                        Debug.logError("Unable to create or update Google id for product [" + productId + "]: " + gee.getMessage(), module);
                    }
                }
                if (UtilValidate.isNotEmpty(id) && productsRemovedFromGoogle.get(index) != null) {
                    String productId = (String) productsRemovedFromGoogle.get(index);
                    try {
                        int count = delegator.removeByAnd("GoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "GOOGLE_ID", "productId", productId));
                    } catch (GenericEntityException gee) {
                        Debug.logError("Unable to remove Google id for product [" + productId + "]: " + gee.getMessage(), module);
                    }
                }
                String title = UtilXml.childElementValue(atomEntryElement, "atom:title", "");
                List batchStatusList = UtilXml.childElementList(atomEntryElement, "batch:status");
                Iterator batchStatusEntryElemIter = batchStatusList.iterator();
                while (batchStatusEntryElemIter.hasNext()) {
                    Element batchStatusEntryElement = (Element) batchStatusEntryElemIter.next();
                    if (UtilValidate.isNotEmpty(batchStatusEntryElement.getAttribute("reason"))) {
                        message.add(title + " " + batchStatusEntryElement.getAttribute("reason"));
                    }
                }
                String errors = UtilXml.childElementValue(atomEntryElement, "batch:status", "");
                if (UtilValidate.isNotEmpty(errors)) {
                    message.add(title + " " + errors);
                }
                index++;
            }
        } catch (Exception e) {
            Debug.logError("Exception reading response from Google: " + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "productsExportToGoogle.exceptionReadingResponseFromGoogle", locale));
        }
        if (message.size() > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "productsExportToGoogle.errorInTheResponseFromGoogle", locale), message);
        }
        return ServiceUtil.returnSuccess();
    }
}
