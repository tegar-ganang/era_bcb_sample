package com.softwarementors.extjs.djn.router.processor.standard.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.softwarementors.extjs.djn.ClassUtils;
import com.softwarementors.extjs.djn.ParallelTask;
import com.softwarementors.extjs.djn.StringUtils;
import com.softwarementors.extjs.djn.Timer;
import com.softwarementors.extjs.djn.UnexpectedException;
import com.softwarementors.extjs.djn.api.RegisteredStandardMethod;
import com.softwarementors.extjs.djn.api.Registry;
import com.softwarementors.extjs.djn.config.GlobalConfiguration;
import com.softwarementors.extjs.djn.gson.JsonException;
import com.softwarementors.extjs.djn.router.dispatcher.Dispatcher;
import com.softwarementors.extjs.djn.router.processor.RequestException;
import com.softwarementors.extjs.djn.router.processor.ResponseData;
import com.softwarementors.extjs.djn.router.processor.standard.StandardErrorResponseData;
import com.softwarementors.extjs.djn.router.processor.standard.StandardRequestProcessorBase;
import com.softwarementors.extjs.djn.router.processor.standard.StandardSuccessResponseData;

public class JsonRequestProcessor extends StandardRequestProcessorBase {

    private static final Logger logger = Logger.getLogger(JsonRequestProcessor.class);

    private static volatile ExecutorService individualRequestsThreadPool;

    private JsonParser parser = new JsonParser();

    protected JsonParser getJsonParser() {
        return this.parser;
    }

    private ExecutorService getIndividualRequestsThreadPool() {
        synchronized (JsonRequestProcessor.class) {
            if (individualRequestsThreadPool == null) {
                individualRequestsThreadPool = createThreadPool();
            }
            return individualRequestsThreadPool;
        }
    }

    private ExecutorService createThreadPool() {
        assert getGlobalConfiguration() != null;
        ExecutorService result = new ThreadPoolExecutor(getGlobalConfiguration().getBatchRequestsMinThreadsPoolSize(), getGlobalConfiguration().getBatchRequestsMaxThreadsPoolSize(), getGlobalConfiguration().getBatchRequestsThreadKeepAliveSeconds(), TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        return result;
    }

    public JsonRequestProcessor(Registry registry, Dispatcher dispatcher, GlobalConfiguration globalConfiguration) {
        super(registry, dispatcher, globalConfiguration);
    }

    public String process(Reader reader, Writer writer) throws IOException {
        String requestString = IOUtils.toString(reader);
        if (logger.isDebugEnabled()) {
            logger.debug("Request data (JSON)=>" + requestString);
        }
        JsonRequestData[] requests = getIndividualJsonRequests(requestString);
        final boolean isBatched = requests.length > 1;
        if (isBatched) {
            if (logger.isDebugEnabled()) {
                logger.debug("Batched request: " + requests.length + " individual requests batched");
            }
        }
        Collection<ResponseData> responses = null;
        boolean useMultipleThreadsIfBatched = isBatched && getGlobalConfiguration().getBatchRequestsMultithreadingEnabled();
        if (useMultipleThreadsIfBatched) {
            responses = processIndividualRequestsInMultipleThreads(requests);
        } else {
            responses = processIndividualRequestsInThisThread(requests);
        }
        String result = convertInvididualResponsesToJsonString(responses);
        writer.write(result);
        if (logger.isDebugEnabled()) {
            logger.debug("ResponseData data (JSON)=>" + result);
        }
        return result;
    }

    private Collection<ResponseData> processIndividualRequestsInThisThread(JsonRequestData[] requests) {
        Collection<ResponseData> responses;
        boolean isBatched = requests.length > 1;
        responses = new ArrayList<ResponseData>(requests.length);
        int requestNumber = 1;
        for (JsonRequestData request : requests) {
            ResponseData response = processIndividualRequest(request, isBatched, requestNumber);
            responses.add(response);
            requestNumber++;
        }
        return responses;
    }

    private Collection<ResponseData> processIndividualRequestsInMultipleThreads(JsonRequestData[] requests) {
        assert requests != null;
        int individualRequestNumber = 1;
        Collection<Callable<ResponseData>> tasks = new ArrayList<Callable<ResponseData>>(requests.length);
        for (final JsonRequestData request : requests) {
            JsonRequestProcessorThread thread = createJsonRequestProcessorThread();
            thread.initialize(this, request, individualRequestNumber);
            tasks.add(thread);
            individualRequestNumber++;
        }
        try {
            ParallelTask<ResponseData> task = new ParallelTask<ResponseData>(getIndividualRequestsThreadPool(), tasks, getGlobalConfiguration().getBatchRequestsMaxThreadsPerRequest());
            Collection<ResponseData> responses = task.get();
            return responses;
        } catch (InterruptedException e) {
            List<ResponseData> responses = new ArrayList<ResponseData>(requests.length);
            logger.error("(Controlled) server error cancelled a batch of " + requests.length + " individual requests due to an InterruptedException exception. " + e.getMessage(), e);
            for (final JsonRequestData request : requests) {
                StandardErrorResponseData response = createJsonServerErrorResponse(request, e);
                responses.add(response);
            }
            return responses;
        } catch (ExecutionException e) {
            UnexpectedException ex = UnexpectedException.forExecutionExceptionShouldNotHappenBecauseProcessorHandlesExceptionsAsServerErrorResponses(e);
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    private JsonRequestProcessorThread createJsonRequestProcessorThread() {
        Class<? extends JsonRequestProcessorThread> cls = getGlobalConfiguration().getJsonRequestProcessorThreadClass();
        try {
            return cls.newInstance();
        } catch (InstantiationException e) {
            JsonRequestProcessorThreadConfigurationException ex = JsonRequestProcessorThreadConfigurationException.forUnableToInstantiateJsonRequestProcessorThread(cls, e);
            logger.fatal(ex.getMessage(), ex);
            throw ex;
        } catch (IllegalAccessException e) {
            JsonRequestProcessorThreadConfigurationException ex = JsonRequestProcessorThreadConfigurationException.forUnableToInstantiateJsonRequestProcessorThread(cls, e);
            logger.fatal(ex.getMessage(), ex);
            throw ex;
        }
    }

    private JsonRequestData[] getIndividualJsonRequests(String requestString) {
        assert !StringUtils.isEmpty(requestString);
        JsonObject[] individualJsonRequests = parseIndividualJsonRequests(requestString, getJsonParser());
        JsonRequestData[] individualRequests = new JsonRequestData[individualJsonRequests.length];
        int i = 0;
        for (JsonObject individualRequest : individualJsonRequests) {
            individualRequests[i] = createIndividualJsonRequest(individualRequest);
            i++;
        }
        return individualRequests;
    }

    private String convertInvididualResponsesToJsonString(Collection<ResponseData> responses) {
        assert responses != null;
        assert !responses.isEmpty();
        StringBuilder result = new StringBuilder();
        if (responses.size() > 1) {
            result.append("[\n");
        }
        int j = 0;
        for (ResponseData response : responses) {
            appendIndividualResponseJsonString(response, result);
            boolean isLast = j == responses.size() - 1;
            if (!isLast) {
                result.append(",");
            }
            j++;
        }
        if (responses.size() > 1) {
            result.append("]");
        }
        return result.toString();
    }

    private Object[] getIndividualRequestParameters(JsonRequestData request) {
        assert request != null;
        RegisteredStandardMethod method = getStandardMethod(request.getAction(), request.getMethod());
        assert method != null;
        Object[] parameters;
        if (!method.getHandleParametersAsJsonArray()) {
            checkJsonMethodParameterTypes(request.getJsonData(), method);
            parameters = jsonDataToMethodParameters(method, request.getJsonData(), method.getParameterTypes());
        } else {
            parameters = new Object[] { request.getJsonData() };
        }
        return parameters;
    }

    private Object[] jsonDataToMethodParameters(RegisteredStandardMethod method, JsonArray jsonParametersArray, Class<?>[] parameterTypes) {
        assert method != null;
        assert parameterTypes != null;
        try {
            JsonElement[] jsonParameters = getJsonElements(jsonParametersArray);
            Object[] result = getMethodParameters(parameterTypes, jsonParameters);
            return result;
        } catch (JsonParseException ex) {
            throw JsonException.forFailedConversionFromJsonStringToMethodParameters(method, jsonParametersArray.toString(), parameterTypes, ex);
        }
    }

    private JsonElement[] getJsonElements(JsonArray jsonParameters) {
        if (jsonParameters == null) {
            return new JsonElement[] {};
        }
        JsonElement[] parameters;
        JsonArray dataArray = jsonParameters;
        parameters = new JsonElement[dataArray.size()];
        for (int i = 0; i < dataArray.size(); i++) {
            parameters[i] = dataArray.get(i);
        }
        return parameters;
    }

    private boolean isString(JsonElement element) {
        assert element != null;
        return element.isJsonPrimitive() && ((JsonPrimitive) element).isString();
    }

    private Object[] getMethodParameters(Class<?>[] parameterTypes, JsonElement[] jsonParameters) {
        assert parameterTypes != null;
        assert jsonParameters != null;
        Object[] result = new Object[jsonParameters.length];
        for (int i = 0; i < jsonParameters.length; i++) {
            JsonElement jsonValue = jsonParameters[i];
            Class<?> parameterType = parameterTypes[i];
            Object value = null;
            if (isString(jsonValue)) {
                if (parameterType.equals(String.class)) {
                    value = jsonValue.getAsString();
                } else if (parameterType.equals(char.class) || parameterType.equals(Character.class)) {
                    value = Character.valueOf(jsonValue.getAsString().charAt(0));
                } else {
                    String json = jsonValue.toString();
                    value = getGson().fromJson(json, parameterType);
                }
            } else {
                String json = jsonValue.toString();
                value = getGson().fromJson(json, parameterType);
            }
            result[i] = value;
        }
        return result;
    }

    private JsonElement[] getIndividualRequestJsonParameters(JsonArray jsonParameters) {
        if (jsonParameters == null) {
            return new JsonElement[] {};
        }
        JsonElement[] parameters;
        parameters = new JsonElement[jsonParameters.size()];
        for (int i = 0; i < jsonParameters.size(); i++) {
            parameters[i] = jsonParameters.get(i);
        }
        return parameters;
    }

    private void checkJsonMethodParameterTypes(JsonArray jsonData, RegisteredStandardMethod method) {
        assert method != null;
        JsonElement[] jsonParameters = getIndividualRequestJsonParameters(jsonData);
        Class<?>[] parameterTypes = method.getParameterTypes();
        assert jsonParameters.length == parameterTypes.length;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            JsonElement jsonElement = jsonParameters[i];
            if (!isValidJsonTypeForJavaType(jsonElement, parameterType)) {
                throw new IllegalArgumentException("'" + jsonElement.toString() + "' is not a valid json text for the '" + parameterType.getName() + "' Java type");
            }
        }
    }

    private boolean isValidJsonTypeForJavaType(JsonElement jsonElement, Class<?> parameterType) {
        assert jsonElement != null;
        assert parameterType != null;
        if (jsonElement.isJsonNull()) {
            return !parameterType.isPrimitive();
        }
        if (parameterType.isArray()) {
            return jsonElement.isJsonArray();
        }
        if (parameterType.equals(Boolean.class) || parameterType.equals(boolean.class)) {
            return jsonElement.isJsonPrimitive() && ((JsonPrimitive) jsonElement).isBoolean();
        } else if (parameterType.equals(char.class) || parameterType.equals(Character.class)) {
            if (jsonElement.isJsonPrimitive() && ((JsonPrimitive) jsonElement).isString()) {
                return jsonElement.getAsString().length() == 1;
            }
            return false;
        } else if (parameterType.equals(String.class)) {
            return jsonElement.isJsonPrimitive();
        } else if (ClassUtils.isNumericType(parameterType)) {
            return jsonElement.isJsonPrimitive() && ((JsonPrimitive) jsonElement).isNumber();
        }
        return true;
    }

    ResponseData processIndividualRequest(JsonRequestData request, boolean isBatched, int requestNumber) {
        assert request != null;
        boolean resultReported = false;
        Timer timer = new Timer();
        try {
            if (isBatched) {
                if (logger.isDebugEnabled()) {
                    logger.debug("  - Individual request #" + requestNumber + " request data=>" + getGson().toJson(request));
                }
            }
            Object[] parameters = getIndividualRequestParameters(request);
            String action = request.getAction();
            String method = request.getMethod();
            Object result = dispatchStandardMethod(action, method, parameters);
            StandardSuccessResponseData response = new StandardSuccessResponseData(request.getTid(), action, method);
            response.setResult(result);
            if (isBatched) {
                if (logger.isDebugEnabled()) {
                    timer.stop();
                    timer.logDebugTimeInMilliseconds("  - Individual request #" + requestNumber + " response data=>" + getGson().toJson(response));
                    resultReported = true;
                }
            }
            return response;
        } catch (Throwable t) {
            StandardErrorResponseData response = createJsonServerErrorResponse(request, t);
            logger.error("(Controlled) server error: " + t.getMessage() + " for Method '" + request.getFullMethodName() + "'", t);
            return response;
        } finally {
            if (!resultReported) {
                timer.stop();
                if (isBatched) {
                    if (logger.isDebugEnabled()) {
                        timer.logDebugTimeInMilliseconds("  - Individual request #" + requestNumber + ": " + request.getFullMethodName() + ". Time");
                    }
                }
            }
        }
    }

    private JsonObject[] parseIndividualJsonRequests(String requestString, JsonParser parser) {
        assert !StringUtils.isEmpty(requestString);
        assert parser != null;
        JsonObject[] individualRequests;
        JsonElement root = parser.parse(requestString);
        if (root.isJsonArray()) {
            JsonArray rootArray = (JsonArray) root;
            if (rootArray.size() == 0) {
                RequestException ex = RequestException.forRequestBatchMustHaveAtLeastOneRequest();
                logger.error(ex.getMessage(), ex);
                throw ex;
            }
            individualRequests = new JsonObject[rootArray.size()];
            int i = 0;
            for (JsonElement item : rootArray) {
                if (!item.isJsonObject()) {
                    RequestException ex = RequestException.forRequestBatchItemMustBeAValidJsonObject(i);
                    logger.error(ex.getMessage(), ex);
                    throw ex;
                }
                individualRequests[i] = (JsonObject) item;
                i++;
            }
        } else if (root.isJsonObject()) {
            individualRequests = new JsonObject[] { (JsonObject) root };
        } else {
            RequestException ex = RequestException.forRequestMustBeAValidJsonObjectOrArray();
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
        return individualRequests;
    }

    private JsonRequestData createIndividualJsonRequest(JsonObject element) {
        assert element != null;
        String action = getNonEmptyJsonString(element, JsonRequestData.ACTION_ELEMENT);
        String method = getNonEmptyJsonString(element, JsonRequestData.METHOD_ELEMENT);
        Long tid = getNonEmptyJsonLong(element, JsonRequestData.TID_ELEMENT);
        String type = getNonEmptyJsonString(element, JsonRequestData.TYPE_ELEMENT);
        JsonArray jsonData = getMethodParametersJsonData(element);
        JsonRequestData result = new JsonRequestData(type, action, method, tid, jsonData);
        return result;
    }

    private JsonArray getMethodParametersJsonData(JsonObject object) {
        assert object != null;
        JsonElement data = object.get(JsonRequestData.DATA_ELEMENT);
        if (data == null) {
            RequestException ex = RequestException.forJsonElementMissing(JsonRequestData.DATA_ELEMENT);
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
        if (data.isJsonNull()) {
            return null;
        }
        if (!data.isJsonNull() && !data.isJsonArray()) {
            RequestException ex = RequestException.forJsonElementMustBeAJsonArray(JsonRequestData.DATA_ELEMENT, data.toString());
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
        return (JsonArray) data;
    }

    private <T> T getNonEmptyJsonPrimitiveValue(JsonObject object, String elementName, PrimitiveJsonValueGetter<T> getter) {
        assert object != null;
        assert !StringUtils.isEmpty(elementName);
        try {
            JsonElement element = object.get(elementName);
            if (element == null) {
                RequestException ex = RequestException.forJsonElementMissing(elementName);
                logger.error(ex.getMessage(), ex);
                throw ex;
            }
            T result = null;
            if (element.isJsonPrimitive()) {
                result = getter.checkedGet((JsonPrimitive) element);
            }
            if (result == null) {
                RequestException ex = RequestException.forJsonElementMustBeANonNullOrEmptyValue(elementName, getter.getValueType());
                logger.error(ex.getMessage(), ex);
                throw ex;
            }
            return result;
        } catch (JsonParseException e) {
            String message = "Probably a DirectJNgine BUG: there should not be JSON parse exceptions: we should have check ALL error conditions. " + e.getMessage();
            logger.error(message, e);
            assert false : message;
            throw e;
        }
    }

    interface PrimitiveJsonValueGetter<T> {

        T checkedGet(JsonPrimitive value);

        Class<T> getValueType();
    }

    private static class PrimitiveJsonLongGetter implements PrimitiveJsonValueGetter<Long> {

        @Override
        public Long checkedGet(JsonPrimitive value) {
            assert value != null;
            if (value.isNumber()) {
                String v = value.toString();
                try {
                    return Long.valueOf(Long.parseLong(v));
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public Class<Long> getValueType() {
            return Long.class;
        }
    }

    private static class PrimitiveJsonStringGetter implements PrimitiveJsonValueGetter<String> {

        @Override
        public String checkedGet(JsonPrimitive value) {
            assert value != null;
            if (value.isString()) {
                String result = value.getAsString();
                if (result.equals("")) result = null;
                return result;
            }
            return null;
        }

        @Override
        public Class<String> getValueType() {
            return String.class;
        }
    }

    private Long getNonEmptyJsonLong(JsonObject object, String elementName) {
        assert object != null;
        assert !StringUtils.isEmpty(elementName);
        return getNonEmptyJsonPrimitiveValue(object, elementName, new PrimitiveJsonLongGetter());
    }

    private String getNonEmptyJsonString(JsonObject object, String elementName) {
        assert object != null;
        assert !StringUtils.isEmpty(elementName);
        return getNonEmptyJsonPrimitiveValue(object, elementName, new PrimitiveJsonStringGetter());
    }
}
