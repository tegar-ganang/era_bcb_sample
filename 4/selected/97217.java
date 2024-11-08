package com.redhipps.hips.client.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.redhipps.hips.client.io.RequestBuilderFactory.Type;
import com.redhipps.hips.client.model.Context;
import com.redhipps.hips.client.model.DateTime;
import com.redhipps.hips.client.model.Doctor;
import com.redhipps.hips.client.model.DoctorConstraint;
import com.redhipps.hips.client.model.Institution;
import com.redhipps.hips.client.model.PythonDatastoreKey;
import com.redhipps.hips.client.model.Model;
import com.redhipps.hips.client.model.Schedule;
import com.redhipps.hips.client.model.ScheduleBlock;
import com.redhipps.hips.client.model.ScheduleSolution;
import com.redhipps.hips.client.model.ScheduleSolutionBlock;

public class IOServiceFactory {

    private RequestBuilderFactory requestBuilderFactory;

    public IOServiceFactory(RequestBuilderFactory requestBuilderFactory) {
        this.requestBuilderFactory = requestBuilderFactory;
    }

    private void createReadersAndWriters(DefaultIOService ios) {
        ios.addServiceMapping(Institution.class, "Institution", new InstitutionReader(ios), new InstitutionWriter(ios));
        ios.addServiceMapping(Doctor.class, "Doctor", new DoctorReader(ios), new DoctorWriter(ios));
        ios.addServiceMapping(DoctorConstraint.class, "DoctorConstraint", new DoctorConstraintReader(ios), new DoctorConstraintWriter(ios));
        ios.addServiceMapping(Schedule.class, "Schedule", new ScheduleReader(ios), new ScheduleWriter(ios));
        ios.addServiceMapping(ScheduleBlock.class, "ScheduleBlock", new ScheduleBlockReader(ios), new ScheduleBlockWriter(ios));
        ios.addServiceMapping(ScheduleSolution.class, "ScheduleSolution", new ScheduleSolutionReader(ios), new ScheduleSolutionWriter(ios));
        ios.addServiceMapping(ScheduleSolutionBlock.class, "ScheduleSolutionBlock", new ScheduleSolutionBlockReader(ios), new ScheduleSolutionBlockWriter(ios));
    }

    public IOService ioService() {
        DefaultIOService ios = new DefaultIOService(requestBuilderFactory);
        createReadersAndWriters(ios);
        return ios;
    }

    public static class DefaultIOService implements IOService {

        private static class IOServiceSpec {

            final String modelName;

            final ModelReader<? extends Model> reader;

            final ModelWriter<? extends Model> writer;

            public IOServiceSpec(String modelName, ModelReader<? extends Model> reader, ModelWriter<? extends Model> writer) {
                this.modelName = modelName;
                this.reader = reader;
                this.writer = writer;
            }
        }

        private Map<Class<? extends Model>, IOServiceSpec> classToSpec;

        private Map<String, IOServiceSpec> typenameToSpec;

        private RequestBuilderFactory requestBuilderFactory;

        DefaultIOService(RequestBuilderFactory requestBuilderFactory) {
            classToSpec = new HashMap<Class<? extends Model>, IOServiceSpec>();
            typenameToSpec = new HashMap<String, IOServiceSpec>();
            this.requestBuilderFactory = requestBuilderFactory;
        }

        void addServiceMapping(Class<? extends Model> cls, String typename, ModelReader<? extends Model> reader, ModelWriter<? extends Model> writer) {
            IOServiceSpec spec = new IOServiceSpec(typename, reader, writer);
            classToSpec.put(cls, spec);
            typenameToSpec.put(typename, spec);
        }

        @SuppressWarnings("unchecked")
        public <T extends Model> ModelReader<T> reader(Class<?> cls) {
            return (ModelReader<T>) classToSpec.get(cls).reader;
        }

        @SuppressWarnings("unchecked")
        <T extends Model> ModelReader<T> reader(JSONObject o) {
            String type = o.get("type").isString().stringValue();
            return (ModelReader<T>) typenameToSpec.get(type).reader;
        }

        @SuppressWarnings("unchecked")
        public <T extends Model> ModelWriter<T> writer(Class<?> cls) {
            return (ModelWriter<T>) classToSpec.get(cls).writer;
        }

        /**
     * Loads prototypes of the given model type and context.
     * 
     * @param callback Called on completion.
     */
        public <T extends Model> void list(Context context, Class<T> modelClass, final ReaderCallback<T> callback) {
            try {
                final IOServiceSpec spec = classToSpec.get(modelClass);
                RequestBuilder requestBuilder = requestBuilderFactory.createBuilder(Type.MODEL_LIST, context, spec.modelName);
                GWT.log("ModelReader sending request: " + requestBuilder.getUrl(), null);
                requestBuilder.sendRequest(null, new RequestCallback() {

                    public void onError(Request request, Throwable exception) {
                        callback.onFailure(exception);
                    }

                    public void onResponseReceived(Request request, Response response) {
                        try {
                            List<T> result = new ArrayList<T>();
                            JSONArray array = JSONParser.parse(response.getText()).isArray();
                            for (int i = 0; i < array.size(); i++) {
                                JSONObject e = array.get(i).isObject();
                                result.add((T) spec.reader.readPrototype(e));
                            }
                            callback.onSuccess(result);
                        } catch (ModelIOException e) {
                            callback.onFailure(e);
                        }
                    }
                });
            } catch (RequestException e) {
                callback.onFailure(e);
            }
        }

        /**
     * Fully loads one instance of the given model.
     * 
     * @param key PythonDatastoreKey for model to load.
     * @param callback Called on completion.
     */
        public <T extends Model> void read(final Context context, final Class<T> modelClass, final PythonDatastoreKey key, final ReaderCallback<T> callback) {
            try {
                final IOServiceSpec spec = classToSpec.get(modelClass);
                RequestBuilder requestBuilder = requestBuilderFactory.createBuilder(Type.MODEL_READ, context, key, spec.modelName);
                GWT.log("ModelReader sending request: " + requestBuilder.getUrl(), null);
                requestBuilder.sendRequest(null, new RequestCallback() {

                    public void onError(Request request, Throwable exception) {
                        callback.onFailure(exception);
                    }

                    @SuppressWarnings("unchecked")
                    public void onResponseReceived(Request request, Response response) {
                        try {
                            List<T> result = new ArrayList<T>();
                            JSONArray array = JSONParser.parse(response.getText()).isArray();
                            ModelReader<T> reader = (ModelReader<T>) spec.reader;
                            result.add(reader.readFully(array));
                            callback.onSuccess(result);
                        } catch (ModelIOException e) {
                            callback.onFailure(e);
                        }
                    }
                });
            } catch (RequestException e) {
                callback.onFailure(e);
            }
        }

        /**
     * Creates a new model in the datastore using the given prototype and calls the
     * callback with a fully formed model based on the prototype.
     * <p>
     * Potentially creates a model tree with the given root. The descendants of the
     * root are created on the server side to avoid multiple calls to create a complex
     * tree.
     * 
     * @param context
     * @param modelPrototype
     * @param callback
     */
        public <T extends Model> void create(Context context, T modelPrototype, WriterCallback<T> callback) {
            IOServiceSpec spec = classToSpec.get(modelPrototype.getClass());
            RequestBuilder requestBuilder = requestBuilderFactory.createBuilder(Type.MODEL_CREATE, context, spec.modelName);
            write(context, modelPrototype, callback, requestBuilder, true);
        }

        /**
     * Writes an existing model to the datastore. Does a shallow write of the given model only.
     * TODO Bulk updates.
     * 
     * @param context
     * @param model
     * @param callback
     */
        public <T extends Model> void write(Context context, T model, WriterCallback<T> callback) {
            IOServiceSpec spec = classToSpec.get(model.getClass());
            RequestBuilder requestBuilder = requestBuilderFactory.createBuilder(Type.MODEL_WRITE, context, spec.modelName);
            write(context, model, callback, requestBuilder, false);
        }

        private <T extends Model> void write(Context context, final T model, final WriterCallback<T> callback, final RequestBuilder requestBuilder, final boolean readResponseFully) {
            RequestCallback translationCallback = new RequestCallback() {

                public void onError(Request request, Throwable exception) {
                    callback.onFailure(exception);
                }

                public void onResponseReceived(Request request, Response response) {
                    try {
                        List<T> models = new ArrayList<T>();
                        JSONArray array = JSONParser.parse(response.getText()).isArray();
                        if (readResponseFully) {
                            ModelReader<T> reader = reader(model.getClass());
                            T model = reader.readFully(array);
                            models.add(model);
                        } else {
                            for (int i = 0; i < array.size(); i++) {
                                JSONObject o = array.get(i).isObject();
                                ModelReader<T> reader = reader(o);
                                models.add(reader.readPrototype(o));
                            }
                        }
                        callback.onSuccess(models);
                    } catch (ModelIOException e) {
                        callback.onFailure(e);
                    }
                }
            };
            try {
                ModelWriter<T> writer = writer(model.getClass());
                String s = writer.writePrototype(context, model).toString();
                requestBuilder.sendRequest(s, translationCallback);
            } catch (RequestException e) {
                callback.onFailure(e);
            } catch (ModelIOException e) {
                callback.onFailure(e);
            }
        }
    }

    public abstract static class JsonWriter<T extends Model> implements ModelWriter<T> {

        protected IOService ioService;

        public JsonWriter(IOService ioService) {
            this.ioService = ioService;
        }

        public JSONObject writePrototype(Context context, T model) throws ModelIOException {
            JSONObject value = new JSONObject();
            writeStringAttr(value, "type", modelType());
            writeKeyAttr(value, "key", model.key());
            serializeToJson(context, model, value);
            return value;
        }

        public JSONArray writeFully(Context context, T model) throws ModelIOException {
            throw new UnsupportedOperationException("writeFully is not yet supported");
        }

        protected abstract String modelType();

        protected abstract void serializeToJson(Context context, T model, JSONObject o);

        protected static void writeKeyAttr(JSONObject o, String key, PythonDatastoreKey value) {
            o.put(key, new JSONString(value.toString()));
        }

        protected static void writeStringAttr(JSONObject o, String key, String value) {
            o.put(key, new JSONString(value));
        }

        protected static void writeIntAttr(JSONObject o, String key, int value) {
            o.put(key, new JSONNumber(value));
        }

        protected static void writeIntArrayAttr(JSONObject o, String key, List<Integer> value) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < value.size(); i++) {
                array.set(i, new JSONNumber(value.get(i)));
            }
            o.put(key, array);
        }

        protected static void writeDateAttr(JSONObject o, String key, DateTime value) {
            StringBuilder formattedDate = new StringBuilder();
            formattedDate.append(value.getYear());
            formattedDate.append('-');
            formattedDate.append(value.getMonth());
            formattedDate.append('-');
            formattedDate.append(value.getDay());
            formattedDate.append(' ');
            formattedDate.append(value.getHour());
            formattedDate.append(':');
            formattedDate.append(value.getMinute());
            formattedDate.append(':');
            formattedDate.append(value.getSecond());
            writeStringAttr(o, key, formattedDate.toString());
        }
    }

    public abstract static class JsonReader<T extends Model> implements ModelReader<T> {

        protected IOService ioService;

        public JsonReader(IOService ioService) {
            this.ioService = ioService;
        }

        public abstract T readPrototype(JSONObject o) throws ModelIOException;

        public abstract void readPrototype(JSONObject o, ReaderVisitor visitor) throws ModelIOException;

        public T readFully(JSONArray o) throws ModelIOException {
            return readPrototype(o.get(0).isObject());
        }

        protected static PythonDatastoreKey keyForJson(JSONObject o) {
            return new PythonDatastoreKey(o.get("key"));
        }

        protected static PythonDatastoreKey readKeyAttr(JSONObject o, String key) {
            return new PythonDatastoreKey(o.get(key));
        }

        protected static String readStringAttr(JSONObject o, String key) {
            return o.get(key).isString().stringValue();
        }

        protected static int readIntAttr(JSONObject o, String key) {
            return (int) o.get(key).isNumber().doubleValue();
        }

        protected static List<Integer> readIntArrayAttr(JSONObject o, String key) {
            JSONArray array = o.get(key).isArray();
            List<Integer> values = new ArrayList<Integer>(array.size());
            for (int i = 0; i < array.size(); i++) {
                values.add((int) array.get(i).isNumber().doubleValue());
            }
            return values;
        }

        protected static DateTime readDateAttr(JSONObject o, String key) {
            String formattedDate = readStringAttr(o, "start_time");
            String[] parts = formattedDate.split(" ");
            String[] dateParts = parts[0].split("-");
            String[] timeParts = parts[1].split(":");
            DateTime date = new DateTime(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]), Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]), Integer.parseInt(timeParts[2]));
            return date;
        }
    }

    public static class InstitutionReader extends JsonReader<Institution> {

        public InstitutionReader(IOService ioService) {
            super(ioService);
        }

        @Override
        public Institution readPrototype(JSONObject o) {
            Institution model = new Institution(keyForJson(o));
            model.setName(readStringAttr(o, "name"));
            return model;
        }

        @Override
        public void readPrototype(JSONObject o, ReaderVisitor v) throws ModelIOException {
            v.visit(readPrototype(o));
        }

        @Override
        public Institution readFully(JSONArray o) throws ModelIOException {
            final Institution[] institution = new Institution[1];
            final List<Schedule> schedules = new ArrayList<Schedule>();
            final List<Doctor> doctors = new ArrayList<Doctor>();
            ReaderVisitor visitor = new ReaderVisitor() {

                @Override
                public void visit(Institution model) {
                    institution[0] = model;
                }

                @Override
                public void visit(Doctor model) {
                    doctors.add(model);
                }

                @Override
                public void visit(Schedule model) {
                    schedules.add(model);
                }
            };
            for (int i = 0; i < o.size(); i++) {
                JSONObject e = o.get(i).isObject();
                ModelReader<?> reader = ((DefaultIOService) ioService).reader(e);
                reader.readPrototype(e, visitor);
            }
            institution[0].setDoctors(doctors);
            institution[0].setSchedules(schedules);
            return institution[0];
        }
    }

    public static class InstitutionWriter extends JsonWriter<Institution> {

        public InstitutionWriter(IOService ioService) {
            super(ioService);
        }

        @Override
        protected String modelType() {
            return "Institution";
        }

        @Override
        protected void serializeToJson(Context context, Institution model, JSONObject o) {
            writeStringAttr(o, "name", model.getName());
        }
    }

    public static class DoctorReader extends JsonReader<Doctor> {

        public DoctorReader(IOService ioService) {
            super(ioService);
        }

        @Override
        public Doctor readPrototype(JSONObject o) {
            Doctor model = new Doctor(keyForJson(o));
            model.setLogin(readStringAttr(o, "login"));
            model.setName(readStringAttr(o, "name"));
            model.setMinShifts(readIntAttr(o, "min_shifts"));
            model.setMaxShifts(readIntAttr(o, "max_shifts"));
            model.setMaxNightShifts(readIntAttr(o, "max_night_shifts"));
            model.setMaxConsecutiveDays(readIntAttr(o, "max_consecutive_days"));
            return model;
        }

        @Override
        public void readPrototype(JSONObject o, ReaderVisitor v) throws ModelIOException {
            v.visit(readPrototype(o));
        }
    }

    public static class DoctorWriter extends JsonWriter<Doctor> {

        public DoctorWriter(IOService ioService) {
            super(ioService);
        }

        @Override
        protected String modelType() {
            return "Doctor";
        }

        @Override
        protected void serializeToJson(Context context, Doctor model, JSONObject o) {
            writeKeyAttr(o, "institution", context.institution().key());
            writeStringAttr(o, "login", model.getLogin().getLogin());
            writeStringAttr(o, "name", model.getName());
            writeIntAttr(o, "min_shifts", model.getMinShifts());
            writeIntAttr(o, "max_shifts", model.getMaxShifts());
            writeIntAttr(o, "max_night_shifts", model.getMaxNightShifts());
            writeIntAttr(o, "max_consecutive_days", model.getMaxConsecutiveDays());
        }
    }

    public static class DoctorConstraintReader extends JsonReader<DoctorConstraint> {

        public DoctorConstraintReader(IOService ioService) {
            super(ioService);
        }

        @Override
        public DoctorConstraint readPrototype(JSONObject o) {
            DoctorConstraint model = new DoctorConstraint(keyForJson(o));
            model.setDoctorRef(readKeyAttr(o, "doctor"));
            model.setDaysOff(readIntArrayAttr(o, "days_off"));
            model.setMinShifts(readIntAttr(o, "min_shifts"));
            model.setMaxShifts(readIntAttr(o, "max_shifts"));
            model.setMaxNightShifts(readIntAttr(o, "max_night_shifts"));
            model.setMaxConsecutiveDays(readIntAttr(o, "max_consecutive_days"));
            return model;
        }

        @Override
        public void readPrototype(JSONObject o, ReaderVisitor v) throws ModelIOException {
            v.visit(readPrototype(o));
        }
    }

    public static class DoctorConstraintWriter extends JsonWriter<DoctorConstraint> {

        public DoctorConstraintWriter(IOService ioService) {
            super(ioService);
        }

        @Override
        protected String modelType() {
            return "DoctorConstraint";
        }

        @Override
        protected void serializeToJson(Context context, DoctorConstraint model, JSONObject o) {
            writeKeyAttr(o, "schedule", context.schedule().key());
            writeKeyAttr(o, "doctor", model.getDoctorRef());
            writeIntArrayAttr(o, "days_off", model.getDaysOff());
            writeIntAttr(o, "min_shifts", model.getMinShifts());
            writeIntAttr(o, "max_shifts", model.getMaxShifts());
            writeIntAttr(o, "max_night_shifts", model.getMaxNightShifts());
            writeIntAttr(o, "max_consecutive_days", model.getMaxConsecutiveDays());
        }
    }

    public static class ScheduleReader extends JsonReader<Schedule> {

        public ScheduleReader(IOService ioService) {
            super(ioService);
        }

        @Override
        public Schedule readPrototype(JSONObject o) {
            Schedule model = new Schedule(keyForJson(o));
            model.setStartTime(readDateAttr(o, "start_time"));
            return model;
        }

        @Override
        public void readPrototype(JSONObject o, ReaderVisitor v) throws ModelIOException {
            v.visit(readPrototype(o));
        }

        @Override
        public Schedule readFully(JSONArray o) throws ModelIOException {
            final Schedule[] schedule = new Schedule[1];
            final List<ScheduleBlock> blocks = new ArrayList<ScheduleBlock>();
            final List<DoctorConstraint> constraints = new ArrayList<DoctorConstraint>();
            final List<ScheduleSolution> solutions = new ArrayList<ScheduleSolution>();
            ReaderVisitor visitor = new ReaderVisitor() {

                @Override
                public void visit(Schedule model) {
                    schedule[0] = model;
                }

                @Override
                public void visit(ScheduleBlock model) {
                    blocks.add(model);
                }

                @Override
                public void visit(DoctorConstraint model) {
                    constraints.add(model);
                }

                @Override
                public void visit(ScheduleSolution model) {
                    solutions.add(model);
                }
            };
            for (int i = 0; i < o.size(); i++) {
                JSONObject e = o.get(i).isObject();
                ModelReader<?> reader = ((DefaultIOService) ioService).reader(e);
                reader.readPrototype(e, visitor);
            }
            schedule[0].setScheduleBlocks(blocks);
            schedule[0].setDoctorConstraints(constraints);
            schedule[0].setScheduleSolutions(solutions);
            return schedule[0];
        }
    }

    public static class ScheduleWriter extends JsonWriter<Schedule> {

        public ScheduleWriter(IOService ioService) {
            super(ioService);
        }

        @Override
        protected String modelType() {
            return "Schedule";
        }

        @Override
        protected void serializeToJson(Context context, Schedule model, JSONObject o) {
            writeKeyAttr(o, "institution", context.institution().key());
            writeDateAttr(o, "start_time", model.getStartTime());
        }
    }

    public static class ScheduleBlockReader extends JsonReader<ScheduleBlock> {

        public ScheduleBlockReader(IOService ioService) {
            super(ioService);
        }

        @Override
        public ScheduleBlock readPrototype(JSONObject o) {
            ScheduleBlock model = new ScheduleBlock(keyForJson(o));
            model.setStartTime(readDateAttr(o, "start_time"));
            model.setDurationMinutes(readIntAttr(o, "duration_minutes"));
            return model;
        }

        @Override
        public void readPrototype(JSONObject o, ReaderVisitor v) throws ModelIOException {
            v.visit(readPrototype(o));
        }
    }

    public static class ScheduleBlockWriter extends JsonWriter<ScheduleBlock> {

        public ScheduleBlockWriter(IOService ioService) {
            super(ioService);
        }

        @Override
        protected String modelType() {
            return "ScheduleBlock";
        }

        @Override
        protected void serializeToJson(Context context, ScheduleBlock model, JSONObject o) {
            writeKeyAttr(o, "schedule", context.schedule().key());
            writeDateAttr(o, "start_time", model.getStartTime());
            writeIntAttr(o, "duration_minutes", model.getDurationMinutes());
        }
    }

    public static class ScheduleSolutionReader extends JsonReader<ScheduleSolution> {

        public ScheduleSolutionReader(IOService ioService) {
            super(ioService);
        }

        @Override
        public ScheduleSolution readPrototype(JSONObject o) {
            ScheduleSolution model = new ScheduleSolution(keyForJson(o));
            model.setMessage(readStringAttr(o, "message"));
            return model;
        }

        @Override
        public void readPrototype(JSONObject o, ReaderVisitor v) throws ModelIOException {
            v.visit(readPrototype(o));
        }

        @Override
        public ScheduleSolution readFully(JSONArray o) throws ModelIOException {
            final ScheduleSolution[] solution = new ScheduleSolution[1];
            final List<ScheduleSolutionBlock> blocks = new ArrayList<ScheduleSolutionBlock>();
            ReaderVisitor visitor = new ReaderVisitor() {

                @Override
                public void visit(ScheduleSolution model) {
                    solution[0] = model;
                }

                @Override
                public void visit(ScheduleSolutionBlock model) {
                    blocks.add(model);
                }
            };
            for (int i = 0; i < o.size(); i++) {
                JSONObject e = o.get(i).isObject();
                ModelReader<?> reader = ((DefaultIOService) ioService).reader(e);
                reader.readPrototype(e, visitor);
            }
            solution[0].setScheduleSolutionBlocks(blocks);
            return solution[0];
        }
    }

    public static class ScheduleSolutionWriter extends JsonWriter<ScheduleSolution> {

        public ScheduleSolutionWriter(IOService ioService) {
            super(ioService);
        }

        @Override
        protected String modelType() {
            return "ScheduleSolution";
        }

        @Override
        protected void serializeToJson(Context context, ScheduleSolution model, JSONObject o) {
            writeKeyAttr(o, "schedule", context.schedule().key());
            writeStringAttr(o, "message", model.getMessage());
        }
    }

    public static class ScheduleSolutionBlockReader extends JsonReader<ScheduleSolutionBlock> {

        public ScheduleSolutionBlockReader(IOService ioService) {
            super(ioService);
        }

        @Override
        public ScheduleSolutionBlock readPrototype(JSONObject o) {
            ScheduleSolutionBlock model = new ScheduleSolutionBlock(keyForJson(o));
            model.setDoctorRef(readKeyAttr(o, "doctor"));
            model.setBlockRef(readKeyAttr(o, "block"));
            return model;
        }

        @Override
        public void readPrototype(JSONObject o, ReaderVisitor v) throws ModelIOException {
            v.visit(readPrototype(o));
        }
    }

    public static class ScheduleSolutionBlockWriter extends JsonWriter<ScheduleSolutionBlock> {

        public ScheduleSolutionBlockWriter(IOService ioService) {
            super(ioService);
        }

        @Override
        protected String modelType() {
            return "ScheduleSolutionBlock";
        }

        @Override
        protected void serializeToJson(Context context, ScheduleSolutionBlock model, JSONObject o) {
            writeKeyAttr(o, "solution", context.scheduleSolution().key());
            writeKeyAttr(o, "doctor", model.getDoctorRef());
            writeKeyAttr(o, "block", model.getBlockRef());
        }
    }
}
