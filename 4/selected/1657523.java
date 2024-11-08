package am.ik.protobuf;

public final class CalcIOProtos {

    private CalcIOProtos() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    }

    public static final class CalcInput extends com.google.protobuf.GeneratedMessage {

        private CalcInput() {
        }

        private static final CalcInput defaultInstance = new CalcInput();

        public static CalcInput getDefaultInstance() {
            return defaultInstance;
        }

        public CalcInput getDefaultInstanceForType() {
            return defaultInstance;
        }

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return am.ik.protobuf.CalcIOProtos.internal_static_am_ik_protobuf_CalcInput_descriptor;
        }

        @Override
        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return am.ik.protobuf.CalcIOProtos.internal_static_am_ik_protobuf_CalcInput_fieldAccessorTable;
        }

        public static final int X_FIELD_NUMBER = 1;

        private boolean hasX;

        private int x_ = 0;

        public boolean hasX() {
            return hasX;
        }

        public int getX() {
            return x_;
        }

        public static final int Y_FIELD_NUMBER = 2;

        private boolean hasY;

        private int y_ = 0;

        public boolean hasY() {
            return hasY;
        }

        public int getY() {
            return y_;
        }

        @Override
        public final boolean isInitialized() {
            if (!hasX) return false;
            if (!hasY) return false;
            return true;
        }

        @Override
        public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
            if (hasX()) {
                output.writeInt32(1, getX());
            }
            if (hasY()) {
                output.writeInt32(2, getY());
            }
            getUnknownFields().writeTo(output);
        }

        private int memoizedSerializedSize = -1;

        @Override
        public int getSerializedSize() {
            int size = memoizedSerializedSize;
            if (size != -1) return size;
            size = 0;
            if (hasX()) {
                size += com.google.protobuf.CodedOutputStream.computeInt32Size(1, getX());
            }
            if (hasY()) {
                size += com.google.protobuf.CodedOutputStream.computeInt32Size(2, getY());
            }
            size += getUnknownFields().getSerializedSize();
            memoizedSerializedSize = size;
            return size;
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistry extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseFrom(byte[] data, com.google.protobuf.ExtensionRegistry extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeDelimitedFrom(input).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeDelimitedFrom(input, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcInput parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder newBuilderForType() {
            return new Builder();
        }

        public static Builder newBuilder(am.ik.protobuf.CalcIOProtos.CalcInput prototype) {
            return new Builder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return newBuilder(this);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessage.Builder<Builder> {

            private Builder() {
            }

            am.ik.protobuf.CalcIOProtos.CalcInput result = new am.ik.protobuf.CalcIOProtos.CalcInput();

            @Override
            protected am.ik.protobuf.CalcIOProtos.CalcInput internalGetResult() {
                return result;
            }

            @Override
            public Builder clear() {
                result = new am.ik.protobuf.CalcIOProtos.CalcInput();
                return this;
            }

            @Override
            public Builder clone() {
                return new Builder().mergeFrom(result);
            }

            @Override
            public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
                return am.ik.protobuf.CalcIOProtos.CalcInput.getDescriptor();
            }

            public am.ik.protobuf.CalcIOProtos.CalcInput getDefaultInstanceForType() {
                return am.ik.protobuf.CalcIOProtos.CalcInput.getDefaultInstance();
            }

            public am.ik.protobuf.CalcIOProtos.CalcInput build() {
                if (result != null && !isInitialized()) {
                    throw new com.google.protobuf.UninitializedMessageException(result);
                }
                return buildPartial();
            }

            private am.ik.protobuf.CalcIOProtos.CalcInput buildParsed() throws com.google.protobuf.InvalidProtocolBufferException {
                if (!isInitialized()) {
                    throw new com.google.protobuf.UninitializedMessageException(result).asInvalidProtocolBufferException();
                }
                return buildPartial();
            }

            public am.ik.protobuf.CalcIOProtos.CalcInput buildPartial() {
                if (result == null) {
                    throw new IllegalStateException("build() has already been called on this Builder.");
                }
                am.ik.protobuf.CalcIOProtos.CalcInput returnMe = result;
                result = null;
                return returnMe;
            }

            @Override
            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof am.ik.protobuf.CalcIOProtos.CalcInput) {
                    return mergeFrom((am.ik.protobuf.CalcIOProtos.CalcInput) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(am.ik.protobuf.CalcIOProtos.CalcInput other) {
                if (other == am.ik.protobuf.CalcIOProtos.CalcInput.getDefaultInstance()) return this;
                if (other.hasX()) {
                    setX(other.getX());
                }
                if (other.hasY()) {
                    setY(other.getY());
                }
                this.mergeUnknownFields(other.getUnknownFields());
                return this;
            }

            @Override
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
                return mergeFrom(input, com.google.protobuf.ExtensionRegistry.getEmptyRegistry());
            }

            @Override
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
                com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder(this.getUnknownFields());
                while (true) {
                    int tag = input.readTag();
                    switch(tag) {
                        case 0:
                            this.setUnknownFields(unknownFields.build());
                            return this;
                        default:
                            {
                                if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                                    this.setUnknownFields(unknownFields.build());
                                    return this;
                                }
                                break;
                            }
                        case 8:
                            {
                                setX(input.readInt32());
                                break;
                            }
                        case 16:
                            {
                                setY(input.readInt32());
                                break;
                            }
                    }
                }
            }

            public boolean hasX() {
                return result.hasX();
            }

            public int getX() {
                return result.getX();
            }

            public Builder setX(int value) {
                result.hasX = true;
                result.x_ = value;
                return this;
            }

            public Builder clearX() {
                result.hasX = false;
                result.x_ = 0;
                return this;
            }

            public boolean hasY() {
                return result.hasY();
            }

            public int getY() {
                return result.getY();
            }

            public Builder setY(int value) {
                result.hasY = true;
                result.y_ = value;
                return this;
            }

            public Builder clearY() {
                result.hasY = false;
                result.y_ = 0;
                return this;
            }
        }

        static {
            am.ik.protobuf.CalcIOProtos.getDescriptor();
        }
    }

    public static final class CalcOutput extends com.google.protobuf.GeneratedMessage {

        private CalcOutput() {
        }

        private static final CalcOutput defaultInstance = new CalcOutput();

        public static CalcOutput getDefaultInstance() {
            return defaultInstance;
        }

        public CalcOutput getDefaultInstanceForType() {
            return defaultInstance;
        }

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return am.ik.protobuf.CalcIOProtos.internal_static_am_ik_protobuf_CalcOutput_descriptor;
        }

        @Override
        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return am.ik.protobuf.CalcIOProtos.internal_static_am_ik_protobuf_CalcOutput_fieldAccessorTable;
        }

        public static final int RESULT_FIELD_NUMBER = 1;

        private boolean hasResult;

        private int result_ = 0;

        public boolean hasResult() {
            return hasResult;
        }

        public int getResult() {
            return result_;
        }

        @Override
        public final boolean isInitialized() {
            if (!hasResult) return false;
            return true;
        }

        @Override
        public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
            if (hasResult()) {
                output.writeInt32(1, getResult());
            }
            getUnknownFields().writeTo(output);
        }

        private int memoizedSerializedSize = -1;

        @Override
        public int getSerializedSize() {
            int size = memoizedSerializedSize;
            if (size != -1) return size;
            size = 0;
            if (hasResult()) {
                size += com.google.protobuf.CodedOutputStream.computeInt32Size(1, getResult());
            }
            size += getUnknownFields().getSerializedSize();
            memoizedSerializedSize = size;
            return size;
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistry extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseFrom(byte[] data, com.google.protobuf.ExtensionRegistry extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeDelimitedFrom(input).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeDelimitedFrom(input, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static am.ik.protobuf.CalcIOProtos.CalcOutput parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder newBuilderForType() {
            return new Builder();
        }

        public static Builder newBuilder(am.ik.protobuf.CalcIOProtos.CalcOutput prototype) {
            return new Builder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return newBuilder(this);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessage.Builder<Builder> {

            private Builder() {
            }

            am.ik.protobuf.CalcIOProtos.CalcOutput result = new am.ik.protobuf.CalcIOProtos.CalcOutput();

            @Override
            protected am.ik.protobuf.CalcIOProtos.CalcOutput internalGetResult() {
                return result;
            }

            @Override
            public Builder clear() {
                result = new am.ik.protobuf.CalcIOProtos.CalcOutput();
                return this;
            }

            @Override
            public Builder clone() {
                return new Builder().mergeFrom(result);
            }

            @Override
            public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
                return am.ik.protobuf.CalcIOProtos.CalcOutput.getDescriptor();
            }

            public am.ik.protobuf.CalcIOProtos.CalcOutput getDefaultInstanceForType() {
                return am.ik.protobuf.CalcIOProtos.CalcOutput.getDefaultInstance();
            }

            public am.ik.protobuf.CalcIOProtos.CalcOutput build() {
                if (result != null && !isInitialized()) {
                    throw new com.google.protobuf.UninitializedMessageException(result);
                }
                return buildPartial();
            }

            private am.ik.protobuf.CalcIOProtos.CalcOutput buildParsed() throws com.google.protobuf.InvalidProtocolBufferException {
                if (!isInitialized()) {
                    throw new com.google.protobuf.UninitializedMessageException(result).asInvalidProtocolBufferException();
                }
                return buildPartial();
            }

            public am.ik.protobuf.CalcIOProtos.CalcOutput buildPartial() {
                if (result == null) {
                    throw new IllegalStateException("build() has already been called on this Builder.");
                }
                am.ik.protobuf.CalcIOProtos.CalcOutput returnMe = result;
                result = null;
                return returnMe;
            }

            @Override
            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof am.ik.protobuf.CalcIOProtos.CalcOutput) {
                    return mergeFrom((am.ik.protobuf.CalcIOProtos.CalcOutput) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(am.ik.protobuf.CalcIOProtos.CalcOutput other) {
                if (other == am.ik.protobuf.CalcIOProtos.CalcOutput.getDefaultInstance()) return this;
                if (other.hasResult()) {
                    setResult(other.getResult());
                }
                this.mergeUnknownFields(other.getUnknownFields());
                return this;
            }

            @Override
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
                return mergeFrom(input, com.google.protobuf.ExtensionRegistry.getEmptyRegistry());
            }

            @Override
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
                com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder(this.getUnknownFields());
                while (true) {
                    int tag = input.readTag();
                    switch(tag) {
                        case 0:
                            this.setUnknownFields(unknownFields.build());
                            return this;
                        default:
                            {
                                if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                                    this.setUnknownFields(unknownFields.build());
                                    return this;
                                }
                                break;
                            }
                        case 8:
                            {
                                setResult(input.readInt32());
                                break;
                            }
                    }
                }
            }

            public boolean hasResult() {
                return result.hasResult();
            }

            public int getResult() {
                return result.getResult();
            }

            public Builder setResult(int value) {
                result.hasResult = true;
                result.result_ = value;
                return this;
            }

            public Builder clearResult() {
                result.hasResult = false;
                result.result_ = 0;
                return this;
            }
        }

        static {
            am.ik.protobuf.CalcIOProtos.getDescriptor();
        }
    }

    public abstract static class CalcService implements com.google.protobuf.Service {

        protected CalcService() {
        }

        public interface Interface {

            public abstract void execute(com.google.protobuf.RpcController controller, am.ik.protobuf.CalcIOProtos.CalcInput request, com.google.protobuf.RpcCallback<am.ik.protobuf.CalcIOProtos.CalcOutput> done);
        }

        public static com.google.protobuf.Service newReflectiveService(final Interface impl) {
            return new CalcService() {

                @Override
                public void execute(com.google.protobuf.RpcController controller, am.ik.protobuf.CalcIOProtos.CalcInput request, com.google.protobuf.RpcCallback<am.ik.protobuf.CalcIOProtos.CalcOutput> done) {
                    impl.execute(controller, request, done);
                }
            };
        }

        public static com.google.protobuf.BlockingService newReflectiveBlockingService(final BlockingInterface impl) {
            return new com.google.protobuf.BlockingService() {

                public final com.google.protobuf.Descriptors.ServiceDescriptor getDescriptorForType() {
                    return getDescriptor();
                }

                public final com.google.protobuf.Message callBlockingMethod(com.google.protobuf.Descriptors.MethodDescriptor method, com.google.protobuf.RpcController controller, com.google.protobuf.Message request) throws com.google.protobuf.ServiceException {
                    if (method.getService() != getDescriptor()) {
                        throw new java.lang.IllegalArgumentException("Service.callBlockingMethod() given method descriptor for " + "wrong service type.");
                    }
                    switch(method.getIndex()) {
                        case 0:
                            return impl.execute(controller, (am.ik.protobuf.CalcIOProtos.CalcInput) request);
                        default:
                            throw new java.lang.AssertionError("Can't get here.");
                    }
                }

                public final com.google.protobuf.Message getRequestPrototype(com.google.protobuf.Descriptors.MethodDescriptor method) {
                    if (method.getService() != getDescriptor()) {
                        throw new java.lang.IllegalArgumentException("Service.getRequestPrototype() given method " + "descriptor for wrong service type.");
                    }
                    switch(method.getIndex()) {
                        case 0:
                            return am.ik.protobuf.CalcIOProtos.CalcInput.getDefaultInstance();
                        default:
                            throw new java.lang.AssertionError("Can't get here.");
                    }
                }

                public final com.google.protobuf.Message getResponsePrototype(com.google.protobuf.Descriptors.MethodDescriptor method) {
                    if (method.getService() != getDescriptor()) {
                        throw new java.lang.IllegalArgumentException("Service.getResponsePrototype() given method " + "descriptor for wrong service type.");
                    }
                    switch(method.getIndex()) {
                        case 0:
                            return am.ik.protobuf.CalcIOProtos.CalcOutput.getDefaultInstance();
                        default:
                            throw new java.lang.AssertionError("Can't get here.");
                    }
                }
            };
        }

        public abstract void execute(com.google.protobuf.RpcController controller, am.ik.protobuf.CalcIOProtos.CalcInput request, com.google.protobuf.RpcCallback<am.ik.protobuf.CalcIOProtos.CalcOutput> done);

        public static final com.google.protobuf.Descriptors.ServiceDescriptor getDescriptor() {
            return am.ik.protobuf.CalcIOProtos.getDescriptor().getServices().get(0);
        }

        public final com.google.protobuf.Descriptors.ServiceDescriptor getDescriptorForType() {
            return getDescriptor();
        }

        public final void callMethod(com.google.protobuf.Descriptors.MethodDescriptor method, com.google.protobuf.RpcController controller, com.google.protobuf.Message request, com.google.protobuf.RpcCallback<com.google.protobuf.Message> done) {
            if (method.getService() != getDescriptor()) {
                throw new java.lang.IllegalArgumentException("Service.callMethod() given method descriptor for wrong " + "service type.");
            }
            switch(method.getIndex()) {
                case 0:
                    this.execute(controller, (am.ik.protobuf.CalcIOProtos.CalcInput) request, com.google.protobuf.RpcUtil.<am.ik.protobuf.CalcIOProtos.CalcOutput>specializeCallback(done));
                    return;
                default:
                    throw new java.lang.AssertionError("Can't get here.");
            }
        }

        public final com.google.protobuf.Message getRequestPrototype(com.google.protobuf.Descriptors.MethodDescriptor method) {
            if (method.getService() != getDescriptor()) {
                throw new java.lang.IllegalArgumentException("Service.getRequestPrototype() given method " + "descriptor for wrong service type.");
            }
            switch(method.getIndex()) {
                case 0:
                    return am.ik.protobuf.CalcIOProtos.CalcInput.getDefaultInstance();
                default:
                    throw new java.lang.AssertionError("Can't get here.");
            }
        }

        public final com.google.protobuf.Message getResponsePrototype(com.google.protobuf.Descriptors.MethodDescriptor method) {
            if (method.getService() != getDescriptor()) {
                throw new java.lang.IllegalArgumentException("Service.getResponsePrototype() given method " + "descriptor for wrong service type.");
            }
            switch(method.getIndex()) {
                case 0:
                    return am.ik.protobuf.CalcIOProtos.CalcOutput.getDefaultInstance();
                default:
                    throw new java.lang.AssertionError("Can't get here.");
            }
        }

        public static Stub newStub(com.google.protobuf.RpcChannel channel) {
            return new Stub(channel);
        }

        public static final class Stub extends am.ik.protobuf.CalcIOProtos.CalcService implements Interface {

            private Stub(com.google.protobuf.RpcChannel channel) {
                this.channel = channel;
            }

            private final com.google.protobuf.RpcChannel channel;

            public com.google.protobuf.RpcChannel getChannel() {
                return channel;
            }

            public void execute(com.google.protobuf.RpcController controller, am.ik.protobuf.CalcIOProtos.CalcInput request, com.google.protobuf.RpcCallback<am.ik.protobuf.CalcIOProtos.CalcOutput> done) {
                channel.callMethod(getDescriptor().getMethods().get(0), controller, request, am.ik.protobuf.CalcIOProtos.CalcOutput.getDefaultInstance(), com.google.protobuf.RpcUtil.generalizeCallback(done, am.ik.protobuf.CalcIOProtos.CalcOutput.class, am.ik.protobuf.CalcIOProtos.CalcOutput.getDefaultInstance()));
            }
        }

        public static BlockingInterface newBlockingStub(com.google.protobuf.BlockingRpcChannel channel) {
            return new BlockingStub(channel);
        }

        public interface BlockingInterface {

            public am.ik.protobuf.CalcIOProtos.CalcOutput execute(com.google.protobuf.RpcController controller, am.ik.protobuf.CalcIOProtos.CalcInput request) throws com.google.protobuf.ServiceException;
        }

        private static final class BlockingStub implements BlockingInterface {

            private BlockingStub(com.google.protobuf.BlockingRpcChannel channel) {
                this.channel = channel;
            }

            private final com.google.protobuf.BlockingRpcChannel channel;

            public am.ik.protobuf.CalcIOProtos.CalcOutput execute(com.google.protobuf.RpcController controller, am.ik.protobuf.CalcIOProtos.CalcInput request) throws com.google.protobuf.ServiceException {
                return (am.ik.protobuf.CalcIOProtos.CalcOutput) channel.callBlockingMethod(getDescriptor().getMethods().get(0), controller, request, am.ik.protobuf.CalcIOProtos.CalcOutput.getDefaultInstance());
            }
        }
    }

    private static com.google.protobuf.Descriptors.Descriptor internal_static_am_ik_protobuf_CalcInput_descriptor;

    private static com.google.protobuf.GeneratedMessage.FieldAccessorTable internal_static_am_ik_protobuf_CalcInput_fieldAccessorTable;

    private static com.google.protobuf.Descriptors.Descriptor internal_static_am_ik_protobuf_CalcOutput_descriptor;

    private static com.google.protobuf.GeneratedMessage.FieldAccessorTable internal_static_am_ik_protobuf_CalcOutput_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        java.lang.String descriptorData = "\n\rcalc_io.proto\022\016am.ik.protobuf\"!\n\tCalcI" + "nput\022\t\n\001x\030\001 \002(\005\022\t\n\001y\030\002 \002(\005\"\034\n\nCalcOutput" + "\022\016\n\006result\030\001 \002(\0052O\n\013CalcService\022@\n\007execu" + "te\022\031.am.ik.protobuf.CalcInput\032\032.am.ik.pr" + "otobuf.CalcOutputB\016B\014CalcIOProtos";
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {

            public com.google.protobuf.ExtensionRegistry assignDescriptors(com.google.protobuf.Descriptors.FileDescriptor root) {
                descriptor = root;
                internal_static_am_ik_protobuf_CalcInput_descriptor = getDescriptor().getMessageTypes().get(0);
                internal_static_am_ik_protobuf_CalcInput_fieldAccessorTable = new com.google.protobuf.GeneratedMessage.FieldAccessorTable(internal_static_am_ik_protobuf_CalcInput_descriptor, new java.lang.String[] { "X", "Y" }, am.ik.protobuf.CalcIOProtos.CalcInput.class, am.ik.protobuf.CalcIOProtos.CalcInput.Builder.class);
                internal_static_am_ik_protobuf_CalcOutput_descriptor = getDescriptor().getMessageTypes().get(1);
                internal_static_am_ik_protobuf_CalcOutput_fieldAccessorTable = new com.google.protobuf.GeneratedMessage.FieldAccessorTable(internal_static_am_ik_protobuf_CalcOutput_descriptor, new java.lang.String[] { "Result" }, am.ik.protobuf.CalcIOProtos.CalcOutput.class, am.ik.protobuf.CalcIOProtos.CalcOutput.Builder.class);
                return null;
            }
        };
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {}, assigner);
    }
}
