package am.ik.protobuf;

public final class TakIOProtos {

    private TakIOProtos() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    }

    public static final class TakInput extends com.google.protobuf.GeneratedMessage {

        private TakInput() {
        }

        private static final TakInput defaultInstance = new TakInput();

        public static TakInput getDefaultInstance() {
            return defaultInstance;
        }

        public TakInput getDefaultInstanceForType() {
            return defaultInstance;
        }

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return am.ik.protobuf.TakIOProtos.internal_static_am_ik_protobuf_TakInput_descriptor;
        }

        @Override
        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return am.ik.protobuf.TakIOProtos.internal_static_am_ik_protobuf_TakInput_fieldAccessorTable;
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

        public static final int Z_FIELD_NUMBER = 3;

        private boolean hasZ;

        private int z_ = 0;

        public boolean hasZ() {
            return hasZ;
        }

        public int getZ() {
            return z_;
        }

        @Override
        public final boolean isInitialized() {
            if (!hasX) return false;
            if (!hasY) return false;
            if (!hasZ) return false;
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
            if (hasZ()) {
                output.writeInt32(3, getZ());
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
            if (hasZ()) {
                size += com.google.protobuf.CodedOutputStream.computeInt32Size(3, getZ());
            }
            size += getUnknownFields().getSerializedSize();
            memoizedSerializedSize = size;
            return size;
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistry extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseFrom(byte[] data, com.google.protobuf.ExtensionRegistry extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeDelimitedFrom(input).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeDelimitedFrom(input, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakInput parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder newBuilderForType() {
            return new Builder();
        }

        public static Builder newBuilder(am.ik.protobuf.TakIOProtos.TakInput prototype) {
            return new Builder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return newBuilder(this);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessage.Builder<Builder> {

            private Builder() {
            }

            am.ik.protobuf.TakIOProtos.TakInput result = new am.ik.protobuf.TakIOProtos.TakInput();

            @Override
            protected am.ik.protobuf.TakIOProtos.TakInput internalGetResult() {
                return result;
            }

            @Override
            public Builder clear() {
                result = new am.ik.protobuf.TakIOProtos.TakInput();
                return this;
            }

            @Override
            public Builder clone() {
                return new Builder().mergeFrom(result);
            }

            @Override
            public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
                return am.ik.protobuf.TakIOProtos.TakInput.getDescriptor();
            }

            public am.ik.protobuf.TakIOProtos.TakInput getDefaultInstanceForType() {
                return am.ik.protobuf.TakIOProtos.TakInput.getDefaultInstance();
            }

            public am.ik.protobuf.TakIOProtos.TakInput build() {
                if (result != null && !isInitialized()) {
                    throw new com.google.protobuf.UninitializedMessageException(result);
                }
                return buildPartial();
            }

            private am.ik.protobuf.TakIOProtos.TakInput buildParsed() throws com.google.protobuf.InvalidProtocolBufferException {
                if (!isInitialized()) {
                    throw new com.google.protobuf.UninitializedMessageException(result).asInvalidProtocolBufferException();
                }
                return buildPartial();
            }

            public am.ik.protobuf.TakIOProtos.TakInput buildPartial() {
                if (result == null) {
                    throw new IllegalStateException("build() has already been called on this Builder.");
                }
                am.ik.protobuf.TakIOProtos.TakInput returnMe = result;
                result = null;
                return returnMe;
            }

            @Override
            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof am.ik.protobuf.TakIOProtos.TakInput) {
                    return mergeFrom((am.ik.protobuf.TakIOProtos.TakInput) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(am.ik.protobuf.TakIOProtos.TakInput other) {
                if (other == am.ik.protobuf.TakIOProtos.TakInput.getDefaultInstance()) return this;
                if (other.hasX()) {
                    setX(other.getX());
                }
                if (other.hasY()) {
                    setY(other.getY());
                }
                if (other.hasZ()) {
                    setZ(other.getZ());
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
                        case 24:
                            {
                                setZ(input.readInt32());
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

            public boolean hasZ() {
                return result.hasZ();
            }

            public int getZ() {
                return result.getZ();
            }

            public Builder setZ(int value) {
                result.hasZ = true;
                result.z_ = value;
                return this;
            }

            public Builder clearZ() {
                result.hasZ = false;
                result.z_ = 0;
                return this;
            }
        }

        static {
            am.ik.protobuf.TakIOProtos.getDescriptor();
        }
    }

    public static final class TakOutput extends com.google.protobuf.GeneratedMessage {

        private TakOutput() {
        }

        private static final TakOutput defaultInstance = new TakOutput();

        public static TakOutput getDefaultInstance() {
            return defaultInstance;
        }

        public TakOutput getDefaultInstanceForType() {
            return defaultInstance;
        }

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return am.ik.protobuf.TakIOProtos.internal_static_am_ik_protobuf_TakOutput_descriptor;
        }

        @Override
        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return am.ik.protobuf.TakIOProtos.internal_static_am_ik_protobuf_TakOutput_fieldAccessorTable;
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

        public static am.ik.protobuf.TakIOProtos.TakOutput parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistry extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseFrom(byte[] data, com.google.protobuf.ExtensionRegistry extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeDelimitedFrom(input).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeDelimitedFrom(input, extensionRegistry).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static am.ik.protobuf.TakIOProtos.TakOutput parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistry extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder newBuilderForType() {
            return new Builder();
        }

        public static Builder newBuilder(am.ik.protobuf.TakIOProtos.TakOutput prototype) {
            return new Builder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return newBuilder(this);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessage.Builder<Builder> {

            private Builder() {
            }

            am.ik.protobuf.TakIOProtos.TakOutput result = new am.ik.protobuf.TakIOProtos.TakOutput();

            @Override
            protected am.ik.protobuf.TakIOProtos.TakOutput internalGetResult() {
                return result;
            }

            @Override
            public Builder clear() {
                result = new am.ik.protobuf.TakIOProtos.TakOutput();
                return this;
            }

            @Override
            public Builder clone() {
                return new Builder().mergeFrom(result);
            }

            @Override
            public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
                return am.ik.protobuf.TakIOProtos.TakOutput.getDescriptor();
            }

            public am.ik.protobuf.TakIOProtos.TakOutput getDefaultInstanceForType() {
                return am.ik.protobuf.TakIOProtos.TakOutput.getDefaultInstance();
            }

            public am.ik.protobuf.TakIOProtos.TakOutput build() {
                if (result != null && !isInitialized()) {
                    throw new com.google.protobuf.UninitializedMessageException(result);
                }
                return buildPartial();
            }

            private am.ik.protobuf.TakIOProtos.TakOutput buildParsed() throws com.google.protobuf.InvalidProtocolBufferException {
                if (!isInitialized()) {
                    throw new com.google.protobuf.UninitializedMessageException(result).asInvalidProtocolBufferException();
                }
                return buildPartial();
            }

            public am.ik.protobuf.TakIOProtos.TakOutput buildPartial() {
                if (result == null) {
                    throw new IllegalStateException("build() has already been called on this Builder.");
                }
                am.ik.protobuf.TakIOProtos.TakOutput returnMe = result;
                result = null;
                return returnMe;
            }

            @Override
            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof am.ik.protobuf.TakIOProtos.TakOutput) {
                    return mergeFrom((am.ik.protobuf.TakIOProtos.TakOutput) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(am.ik.protobuf.TakIOProtos.TakOutput other) {
                if (other == am.ik.protobuf.TakIOProtos.TakOutput.getDefaultInstance()) return this;
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
            am.ik.protobuf.TakIOProtos.getDescriptor();
        }
    }

    public abstract static class TakService implements com.google.protobuf.Service {

        protected TakService() {
        }

        public interface Interface {

            public abstract void execute(com.google.protobuf.RpcController controller, am.ik.protobuf.TakIOProtos.TakInput request, com.google.protobuf.RpcCallback<am.ik.protobuf.TakIOProtos.TakOutput> done);
        }

        public static com.google.protobuf.Service newReflectiveService(final Interface impl) {
            return new TakService() {

                @Override
                public void execute(com.google.protobuf.RpcController controller, am.ik.protobuf.TakIOProtos.TakInput request, com.google.protobuf.RpcCallback<am.ik.protobuf.TakIOProtos.TakOutput> done) {
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
                            return impl.execute(controller, (am.ik.protobuf.TakIOProtos.TakInput) request);
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
                            return am.ik.protobuf.TakIOProtos.TakInput.getDefaultInstance();
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
                            return am.ik.protobuf.TakIOProtos.TakOutput.getDefaultInstance();
                        default:
                            throw new java.lang.AssertionError("Can't get here.");
                    }
                }
            };
        }

        public abstract void execute(com.google.protobuf.RpcController controller, am.ik.protobuf.TakIOProtos.TakInput request, com.google.protobuf.RpcCallback<am.ik.protobuf.TakIOProtos.TakOutput> done);

        public static final com.google.protobuf.Descriptors.ServiceDescriptor getDescriptor() {
            return am.ik.protobuf.TakIOProtos.getDescriptor().getServices().get(0);
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
                    this.execute(controller, (am.ik.protobuf.TakIOProtos.TakInput) request, com.google.protobuf.RpcUtil.<am.ik.protobuf.TakIOProtos.TakOutput>specializeCallback(done));
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
                    return am.ik.protobuf.TakIOProtos.TakInput.getDefaultInstance();
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
                    return am.ik.protobuf.TakIOProtos.TakOutput.getDefaultInstance();
                default:
                    throw new java.lang.AssertionError("Can't get here.");
            }
        }

        public static Stub newStub(com.google.protobuf.RpcChannel channel) {
            return new Stub(channel);
        }

        public static final class Stub extends am.ik.protobuf.TakIOProtos.TakService implements Interface {

            private Stub(com.google.protobuf.RpcChannel channel) {
                this.channel = channel;
            }

            private final com.google.protobuf.RpcChannel channel;

            public com.google.protobuf.RpcChannel getChannel() {
                return channel;
            }

            public void execute(com.google.protobuf.RpcController controller, am.ik.protobuf.TakIOProtos.TakInput request, com.google.protobuf.RpcCallback<am.ik.protobuf.TakIOProtos.TakOutput> done) {
                channel.callMethod(getDescriptor().getMethods().get(0), controller, request, am.ik.protobuf.TakIOProtos.TakOutput.getDefaultInstance(), com.google.protobuf.RpcUtil.generalizeCallback(done, am.ik.protobuf.TakIOProtos.TakOutput.class, am.ik.protobuf.TakIOProtos.TakOutput.getDefaultInstance()));
            }
        }

        public static BlockingInterface newBlockingStub(com.google.protobuf.BlockingRpcChannel channel) {
            return new BlockingStub(channel);
        }

        public interface BlockingInterface {

            public am.ik.protobuf.TakIOProtos.TakOutput execute(com.google.protobuf.RpcController controller, am.ik.protobuf.TakIOProtos.TakInput request) throws com.google.protobuf.ServiceException;
        }

        private static final class BlockingStub implements BlockingInterface {

            private BlockingStub(com.google.protobuf.BlockingRpcChannel channel) {
                this.channel = channel;
            }

            private final com.google.protobuf.BlockingRpcChannel channel;

            public am.ik.protobuf.TakIOProtos.TakOutput execute(com.google.protobuf.RpcController controller, am.ik.protobuf.TakIOProtos.TakInput request) throws com.google.protobuf.ServiceException {
                return (am.ik.protobuf.TakIOProtos.TakOutput) channel.callBlockingMethod(getDescriptor().getMethods().get(0), controller, request, am.ik.protobuf.TakIOProtos.TakOutput.getDefaultInstance());
            }
        }
    }

    private static com.google.protobuf.Descriptors.Descriptor internal_static_am_ik_protobuf_TakInput_descriptor;

    private static com.google.protobuf.GeneratedMessage.FieldAccessorTable internal_static_am_ik_protobuf_TakInput_fieldAccessorTable;

    private static com.google.protobuf.Descriptors.Descriptor internal_static_am_ik_protobuf_TakOutput_descriptor;

    private static com.google.protobuf.GeneratedMessage.FieldAccessorTable internal_static_am_ik_protobuf_TakOutput_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        java.lang.String descriptorData = "\n\014tak_io.proto\022\016am.ik.protobuf\"+\n\010TakInp" + "ut\022\t\n\001x\030\001 \002(\005\022\t\n\001y\030\002 \002(\005\022\t\n\001z\030\003 \002(\005\"\033\n\tT" + "akOutput\022\016\n\006result\030\001 \002(\0052L\n\nTakService\022>" + "\n\007execute\022\030.am.ik.protobuf.TakInput\032\031.am" + ".ik.protobuf.TakOutputB\rB\013TakIOProtos";
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {

            public com.google.protobuf.ExtensionRegistry assignDescriptors(com.google.protobuf.Descriptors.FileDescriptor root) {
                descriptor = root;
                internal_static_am_ik_protobuf_TakInput_descriptor = getDescriptor().getMessageTypes().get(0);
                internal_static_am_ik_protobuf_TakInput_fieldAccessorTable = new com.google.protobuf.GeneratedMessage.FieldAccessorTable(internal_static_am_ik_protobuf_TakInput_descriptor, new java.lang.String[] { "X", "Y", "Z" }, am.ik.protobuf.TakIOProtos.TakInput.class, am.ik.protobuf.TakIOProtos.TakInput.Builder.class);
                internal_static_am_ik_protobuf_TakOutput_descriptor = getDescriptor().getMessageTypes().get(1);
                internal_static_am_ik_protobuf_TakOutput_fieldAccessorTable = new com.google.protobuf.GeneratedMessage.FieldAccessorTable(internal_static_am_ik_protobuf_TakOutput_descriptor, new java.lang.String[] { "Result" }, am.ik.protobuf.TakIOProtos.TakOutput.class, am.ik.protobuf.TakIOProtos.TakOutput.Builder.class);
                return null;
            }
        };
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {}, assigner);
    }
}
