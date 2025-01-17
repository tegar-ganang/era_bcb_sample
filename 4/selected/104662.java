package org.waveprotocol.box.search;

public final class SearchProto {

    private SearchProto() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    }

    public static final class SearchRequest extends com.google.protobuf.GeneratedMessage {

        private SearchRequest() {
            initFields();
        }

        private SearchRequest(boolean noInit) {
        }

        private static final SearchRequest defaultInstance;

        public static SearchRequest getDefaultInstance() {
            return defaultInstance;
        }

        public SearchRequest getDefaultInstanceForType() {
            return defaultInstance;
        }

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return org.waveprotocol.box.search.SearchProto.internal_static_search_SearchRequest_descriptor;
        }

        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return org.waveprotocol.box.search.SearchProto.internal_static_search_SearchRequest_fieldAccessorTable;
        }

        public static final int QUERY_FIELD_NUMBER = 1;

        private boolean hasQuery;

        private java.lang.String query_ = "";

        public boolean hasQuery() {
            return hasQuery;
        }

        public java.lang.String getQuery() {
            return query_;
        }

        public static final int INDEX_FIELD_NUMBER = 2;

        private boolean hasIndex;

        private int index_ = 0;

        public boolean hasIndex() {
            return hasIndex;
        }

        public int getIndex() {
            return index_;
        }

        public static final int NUMRESULTS_FIELD_NUMBER = 3;

        private boolean hasNumResults;

        private int numResults_ = 0;

        public boolean hasNumResults() {
            return hasNumResults;
        }

        public int getNumResults() {
            return numResults_;
        }

        private void initFields() {
        }

        public final boolean isInitialized() {
            if (!hasQuery) return false;
            if (!hasIndex) return false;
            if (!hasNumResults) return false;
            return true;
        }

        public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
            getSerializedSize();
            if (hasQuery()) {
                output.writeString(1, getQuery());
            }
            if (hasIndex()) {
                output.writeInt32(2, getIndex());
            }
            if (hasNumResults()) {
                output.writeInt32(3, getNumResults());
            }
            getUnknownFields().writeTo(output);
        }

        private int memoizedSerializedSize = -1;

        public int getSerializedSize() {
            int size = memoizedSerializedSize;
            if (size != -1) return size;
            size = 0;
            if (hasQuery()) {
                size += com.google.protobuf.CodedOutputStream.computeStringSize(1, getQuery());
            }
            if (hasIndex()) {
                size += com.google.protobuf.CodedOutputStream.computeInt32Size(2, getIndex());
            }
            if (hasNumResults()) {
                size += com.google.protobuf.CodedOutputStream.computeInt32Size(3, getNumResults());
            }
            size += getUnknownFields().getSerializedSize();
            memoizedSerializedSize = size;
            return size;
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
            Builder builder = newBuilder();
            if (builder.mergeDelimitedFrom(input)) {
                return builder.buildParsed();
            } else {
                return null;
            }
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            Builder builder = newBuilder();
            if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
                return builder.buildParsed();
            } else {
                return null;
            }
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchRequest parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static Builder newBuilder() {
            return Builder.create();
        }

        public Builder newBuilderForType() {
            return newBuilder();
        }

        public static Builder newBuilder(org.waveprotocol.box.search.SearchProto.SearchRequest prototype) {
            return newBuilder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return newBuilder(this);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessage.Builder<Builder> {

            private org.waveprotocol.box.search.SearchProto.SearchRequest result;

            private Builder() {
            }

            private static Builder create() {
                Builder builder = new Builder();
                builder.result = new org.waveprotocol.box.search.SearchProto.SearchRequest();
                return builder;
            }

            protected org.waveprotocol.box.search.SearchProto.SearchRequest internalGetResult() {
                return result;
            }

            public Builder clear() {
                if (result == null) {
                    throw new IllegalStateException("Cannot call clear() after build().");
                }
                result = new org.waveprotocol.box.search.SearchProto.SearchRequest();
                return this;
            }

            public Builder clone() {
                return create().mergeFrom(result);
            }

            public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
                return org.waveprotocol.box.search.SearchProto.SearchRequest.getDescriptor();
            }

            public org.waveprotocol.box.search.SearchProto.SearchRequest getDefaultInstanceForType() {
                return org.waveprotocol.box.search.SearchProto.SearchRequest.getDefaultInstance();
            }

            public boolean isInitialized() {
                return result.isInitialized();
            }

            public org.waveprotocol.box.search.SearchProto.SearchRequest build() {
                if (result != null && !isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return buildPartial();
            }

            private org.waveprotocol.box.search.SearchProto.SearchRequest buildParsed() throws com.google.protobuf.InvalidProtocolBufferException {
                if (!isInitialized()) {
                    throw newUninitializedMessageException(result).asInvalidProtocolBufferException();
                }
                return buildPartial();
            }

            public org.waveprotocol.box.search.SearchProto.SearchRequest buildPartial() {
                if (result == null) {
                    throw new IllegalStateException("build() has already been called on this Builder.");
                }
                org.waveprotocol.box.search.SearchProto.SearchRequest returnMe = result;
                result = null;
                return returnMe;
            }

            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof org.waveprotocol.box.search.SearchProto.SearchRequest) {
                    return mergeFrom((org.waveprotocol.box.search.SearchProto.SearchRequest) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(org.waveprotocol.box.search.SearchProto.SearchRequest other) {
                if (other == org.waveprotocol.box.search.SearchProto.SearchRequest.getDefaultInstance()) return this;
                if (other.hasQuery()) {
                    setQuery(other.getQuery());
                }
                if (other.hasIndex()) {
                    setIndex(other.getIndex());
                }
                if (other.hasNumResults()) {
                    setNumResults(other.getNumResults());
                }
                this.mergeUnknownFields(other.getUnknownFields());
                return this;
            }

            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
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
                        case 10:
                            {
                                setQuery(input.readString());
                                break;
                            }
                        case 16:
                            {
                                setIndex(input.readInt32());
                                break;
                            }
                        case 24:
                            {
                                setNumResults(input.readInt32());
                                break;
                            }
                    }
                }
            }

            public boolean hasQuery() {
                return result.hasQuery();
            }

            public java.lang.String getQuery() {
                return result.getQuery();
            }

            public Builder setQuery(java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                result.hasQuery = true;
                result.query_ = value;
                return this;
            }

            public Builder clearQuery() {
                result.hasQuery = false;
                result.query_ = getDefaultInstance().getQuery();
                return this;
            }

            public boolean hasIndex() {
                return result.hasIndex();
            }

            public int getIndex() {
                return result.getIndex();
            }

            public Builder setIndex(int value) {
                result.hasIndex = true;
                result.index_ = value;
                return this;
            }

            public Builder clearIndex() {
                result.hasIndex = false;
                result.index_ = 0;
                return this;
            }

            public boolean hasNumResults() {
                return result.hasNumResults();
            }

            public int getNumResults() {
                return result.getNumResults();
            }

            public Builder setNumResults(int value) {
                result.hasNumResults = true;
                result.numResults_ = value;
                return this;
            }

            public Builder clearNumResults() {
                result.hasNumResults = false;
                result.numResults_ = 0;
                return this;
            }
        }

        static {
            defaultInstance = new SearchRequest(true);
            org.waveprotocol.box.search.SearchProto.internalForceInit();
            defaultInstance.initFields();
        }
    }

    public static final class SearchResponse extends com.google.protobuf.GeneratedMessage {

        private SearchResponse() {
            initFields();
        }

        private SearchResponse(boolean noInit) {
        }

        private static final SearchResponse defaultInstance;

        public static SearchResponse getDefaultInstance() {
            return defaultInstance;
        }

        public SearchResponse getDefaultInstanceForType() {
            return defaultInstance;
        }

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return org.waveprotocol.box.search.SearchProto.internal_static_search_SearchResponse_descriptor;
        }

        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return org.waveprotocol.box.search.SearchProto.internal_static_search_SearchResponse_fieldAccessorTable;
        }

        public static final class Digest extends com.google.protobuf.GeneratedMessage {

            private Digest() {
                initFields();
            }

            private Digest(boolean noInit) {
            }

            private static final Digest defaultInstance;

            public static Digest getDefaultInstance() {
                return defaultInstance;
            }

            public Digest getDefaultInstanceForType() {
                return defaultInstance;
            }

            public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
                return org.waveprotocol.box.search.SearchProto.internal_static_search_SearchResponse_Digest_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
                return org.waveprotocol.box.search.SearchProto.internal_static_search_SearchResponse_Digest_fieldAccessorTable;
            }

            public static final int TITLE_FIELD_NUMBER = 1;

            private boolean hasTitle;

            private java.lang.String title_ = "";

            public boolean hasTitle() {
                return hasTitle;
            }

            public java.lang.String getTitle() {
                return title_;
            }

            public static final int SNIPPET_FIELD_NUMBER = 2;

            private boolean hasSnippet;

            private java.lang.String snippet_ = "";

            public boolean hasSnippet() {
                return hasSnippet;
            }

            public java.lang.String getSnippet() {
                return snippet_;
            }

            public static final int WAVEID_FIELD_NUMBER = 3;

            private boolean hasWaveId;

            private java.lang.String waveId_ = "";

            public boolean hasWaveId() {
                return hasWaveId;
            }

            public java.lang.String getWaveId() {
                return waveId_;
            }

            public static final int LASTMODIFIED_FIELD_NUMBER = 4;

            private boolean hasLastModified;

            private long lastModified_ = 0L;

            public boolean hasLastModified() {
                return hasLastModified;
            }

            public long getLastModified() {
                return lastModified_;
            }

            public static final int UNREADCOUNT_FIELD_NUMBER = 5;

            private boolean hasUnreadCount;

            private int unreadCount_ = 0;

            public boolean hasUnreadCount() {
                return hasUnreadCount;
            }

            public int getUnreadCount() {
                return unreadCount_;
            }

            public static final int BLIPCOUNT_FIELD_NUMBER = 6;

            private boolean hasBlipCount;

            private int blipCount_ = 0;

            public boolean hasBlipCount() {
                return hasBlipCount;
            }

            public int getBlipCount() {
                return blipCount_;
            }

            public static final int PARTICIPANTS_FIELD_NUMBER = 7;

            private java.util.List<java.lang.String> participants_ = java.util.Collections.emptyList();

            public java.util.List<java.lang.String> getParticipantsList() {
                return participants_;
            }

            public int getParticipantsCount() {
                return participants_.size();
            }

            public java.lang.String getParticipants(int index) {
                return participants_.get(index);
            }

            public static final int AUTHOR_FIELD_NUMBER = 8;

            private boolean hasAuthor;

            private java.lang.String author_ = "";

            public boolean hasAuthor() {
                return hasAuthor;
            }

            public java.lang.String getAuthor() {
                return author_;
            }

            private void initFields() {
            }

            public final boolean isInitialized() {
                if (!hasTitle) return false;
                if (!hasSnippet) return false;
                if (!hasWaveId) return false;
                if (!hasLastModified) return false;
                if (!hasUnreadCount) return false;
                if (!hasBlipCount) return false;
                if (!hasAuthor) return false;
                return true;
            }

            public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
                getSerializedSize();
                if (hasTitle()) {
                    output.writeString(1, getTitle());
                }
                if (hasSnippet()) {
                    output.writeString(2, getSnippet());
                }
                if (hasWaveId()) {
                    output.writeString(3, getWaveId());
                }
                if (hasLastModified()) {
                    output.writeInt64(4, getLastModified());
                }
                if (hasUnreadCount()) {
                    output.writeInt32(5, getUnreadCount());
                }
                if (hasBlipCount()) {
                    output.writeInt32(6, getBlipCount());
                }
                for (java.lang.String element : getParticipantsList()) {
                    output.writeString(7, element);
                }
                if (hasAuthor()) {
                    output.writeString(8, getAuthor());
                }
                getUnknownFields().writeTo(output);
            }

            private int memoizedSerializedSize = -1;

            public int getSerializedSize() {
                int size = memoizedSerializedSize;
                if (size != -1) return size;
                size = 0;
                if (hasTitle()) {
                    size += com.google.protobuf.CodedOutputStream.computeStringSize(1, getTitle());
                }
                if (hasSnippet()) {
                    size += com.google.protobuf.CodedOutputStream.computeStringSize(2, getSnippet());
                }
                if (hasWaveId()) {
                    size += com.google.protobuf.CodedOutputStream.computeStringSize(3, getWaveId());
                }
                if (hasLastModified()) {
                    size += com.google.protobuf.CodedOutputStream.computeInt64Size(4, getLastModified());
                }
                if (hasUnreadCount()) {
                    size += com.google.protobuf.CodedOutputStream.computeInt32Size(5, getUnreadCount());
                }
                if (hasBlipCount()) {
                    size += com.google.protobuf.CodedOutputStream.computeInt32Size(6, getBlipCount());
                }
                {
                    int dataSize = 0;
                    for (java.lang.String element : getParticipantsList()) {
                        dataSize += com.google.protobuf.CodedOutputStream.computeStringSizeNoTag(element);
                    }
                    size += dataSize;
                    size += 1 * getParticipantsList().size();
                }
                if (hasAuthor()) {
                    size += com.google.protobuf.CodedOutputStream.computeStringSize(8, getAuthor());
                }
                size += getUnknownFields().getSerializedSize();
                memoizedSerializedSize = size;
                return size;
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
                return newBuilder().mergeFrom(data).buildParsed();
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
                return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
                return newBuilder().mergeFrom(data).buildParsed();
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
                return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseFrom(java.io.InputStream input) throws java.io.IOException {
                return newBuilder().mergeFrom(input).buildParsed();
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
                return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
                Builder builder = newBuilder();
                if (builder.mergeDelimitedFrom(input)) {
                    return builder.buildParsed();
                } else {
                    return null;
                }
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
                Builder builder = newBuilder();
                if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
                    return builder.buildParsed();
                } else {
                    return null;
                }
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
                return newBuilder().mergeFrom(input).buildParsed();
            }

            public static org.waveprotocol.box.search.SearchProto.SearchResponse.Digest parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
                return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
            }

            public static Builder newBuilder() {
                return Builder.create();
            }

            public Builder newBuilderForType() {
                return newBuilder();
            }

            public static Builder newBuilder(org.waveprotocol.box.search.SearchProto.SearchResponse.Digest prototype) {
                return newBuilder().mergeFrom(prototype);
            }

            public Builder toBuilder() {
                return newBuilder(this);
            }

            public static final class Builder extends com.google.protobuf.GeneratedMessage.Builder<Builder> {

                private org.waveprotocol.box.search.SearchProto.SearchResponse.Digest result;

                private Builder() {
                }

                private static Builder create() {
                    Builder builder = new Builder();
                    builder.result = new org.waveprotocol.box.search.SearchProto.SearchResponse.Digest();
                    return builder;
                }

                protected org.waveprotocol.box.search.SearchProto.SearchResponse.Digest internalGetResult() {
                    return result;
                }

                public Builder clear() {
                    if (result == null) {
                        throw new IllegalStateException("Cannot call clear() after build().");
                    }
                    result = new org.waveprotocol.box.search.SearchProto.SearchResponse.Digest();
                    return this;
                }

                public Builder clone() {
                    return create().mergeFrom(result);
                }

                public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
                    return org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.getDescriptor();
                }

                public org.waveprotocol.box.search.SearchProto.SearchResponse.Digest getDefaultInstanceForType() {
                    return org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.getDefaultInstance();
                }

                public boolean isInitialized() {
                    return result.isInitialized();
                }

                public org.waveprotocol.box.search.SearchProto.SearchResponse.Digest build() {
                    if (result != null && !isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return buildPartial();
                }

                private org.waveprotocol.box.search.SearchProto.SearchResponse.Digest buildParsed() throws com.google.protobuf.InvalidProtocolBufferException {
                    if (!isInitialized()) {
                        throw newUninitializedMessageException(result).asInvalidProtocolBufferException();
                    }
                    return buildPartial();
                }

                public org.waveprotocol.box.search.SearchProto.SearchResponse.Digest buildPartial() {
                    if (result == null) {
                        throw new IllegalStateException("build() has already been called on this Builder.");
                    }
                    if (result.participants_ != java.util.Collections.EMPTY_LIST) {
                        result.participants_ = java.util.Collections.unmodifiableList(result.participants_);
                    }
                    org.waveprotocol.box.search.SearchProto.SearchResponse.Digest returnMe = result;
                    result = null;
                    return returnMe;
                }

                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof org.waveprotocol.box.search.SearchProto.SearchResponse.Digest) {
                        return mergeFrom((org.waveprotocol.box.search.SearchProto.SearchResponse.Digest) other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(org.waveprotocol.box.search.SearchProto.SearchResponse.Digest other) {
                    if (other == org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.getDefaultInstance()) return this;
                    if (other.hasTitle()) {
                        setTitle(other.getTitle());
                    }
                    if (other.hasSnippet()) {
                        setSnippet(other.getSnippet());
                    }
                    if (other.hasWaveId()) {
                        setWaveId(other.getWaveId());
                    }
                    if (other.hasLastModified()) {
                        setLastModified(other.getLastModified());
                    }
                    if (other.hasUnreadCount()) {
                        setUnreadCount(other.getUnreadCount());
                    }
                    if (other.hasBlipCount()) {
                        setBlipCount(other.getBlipCount());
                    }
                    if (!other.participants_.isEmpty()) {
                        if (result.participants_.isEmpty()) {
                            result.participants_ = new java.util.ArrayList<java.lang.String>();
                        }
                        result.participants_.addAll(other.participants_);
                    }
                    if (other.hasAuthor()) {
                        setAuthor(other.getAuthor());
                    }
                    this.mergeUnknownFields(other.getUnknownFields());
                    return this;
                }

                public Builder mergeFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
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
                            case 10:
                                {
                                    setTitle(input.readString());
                                    break;
                                }
                            case 18:
                                {
                                    setSnippet(input.readString());
                                    break;
                                }
                            case 26:
                                {
                                    setWaveId(input.readString());
                                    break;
                                }
                            case 32:
                                {
                                    setLastModified(input.readInt64());
                                    break;
                                }
                            case 40:
                                {
                                    setUnreadCount(input.readInt32());
                                    break;
                                }
                            case 48:
                                {
                                    setBlipCount(input.readInt32());
                                    break;
                                }
                            case 58:
                                {
                                    addParticipants(input.readString());
                                    break;
                                }
                            case 66:
                                {
                                    setAuthor(input.readString());
                                    break;
                                }
                        }
                    }
                }

                public boolean hasTitle() {
                    return result.hasTitle();
                }

                public java.lang.String getTitle() {
                    return result.getTitle();
                }

                public Builder setTitle(java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    result.hasTitle = true;
                    result.title_ = value;
                    return this;
                }

                public Builder clearTitle() {
                    result.hasTitle = false;
                    result.title_ = getDefaultInstance().getTitle();
                    return this;
                }

                public boolean hasSnippet() {
                    return result.hasSnippet();
                }

                public java.lang.String getSnippet() {
                    return result.getSnippet();
                }

                public Builder setSnippet(java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    result.hasSnippet = true;
                    result.snippet_ = value;
                    return this;
                }

                public Builder clearSnippet() {
                    result.hasSnippet = false;
                    result.snippet_ = getDefaultInstance().getSnippet();
                    return this;
                }

                public boolean hasWaveId() {
                    return result.hasWaveId();
                }

                public java.lang.String getWaveId() {
                    return result.getWaveId();
                }

                public Builder setWaveId(java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    result.hasWaveId = true;
                    result.waveId_ = value;
                    return this;
                }

                public Builder clearWaveId() {
                    result.hasWaveId = false;
                    result.waveId_ = getDefaultInstance().getWaveId();
                    return this;
                }

                public boolean hasLastModified() {
                    return result.hasLastModified();
                }

                public long getLastModified() {
                    return result.getLastModified();
                }

                public Builder setLastModified(long value) {
                    result.hasLastModified = true;
                    result.lastModified_ = value;
                    return this;
                }

                public Builder clearLastModified() {
                    result.hasLastModified = false;
                    result.lastModified_ = 0L;
                    return this;
                }

                public boolean hasUnreadCount() {
                    return result.hasUnreadCount();
                }

                public int getUnreadCount() {
                    return result.getUnreadCount();
                }

                public Builder setUnreadCount(int value) {
                    result.hasUnreadCount = true;
                    result.unreadCount_ = value;
                    return this;
                }

                public Builder clearUnreadCount() {
                    result.hasUnreadCount = false;
                    result.unreadCount_ = 0;
                    return this;
                }

                public boolean hasBlipCount() {
                    return result.hasBlipCount();
                }

                public int getBlipCount() {
                    return result.getBlipCount();
                }

                public Builder setBlipCount(int value) {
                    result.hasBlipCount = true;
                    result.blipCount_ = value;
                    return this;
                }

                public Builder clearBlipCount() {
                    result.hasBlipCount = false;
                    result.blipCount_ = 0;
                    return this;
                }

                public java.util.List<java.lang.String> getParticipantsList() {
                    return java.util.Collections.unmodifiableList(result.participants_);
                }

                public int getParticipantsCount() {
                    return result.getParticipantsCount();
                }

                public java.lang.String getParticipants(int index) {
                    return result.getParticipants(index);
                }

                public Builder setParticipants(int index, java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    result.participants_.set(index, value);
                    return this;
                }

                public Builder addParticipants(java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    if (result.participants_.isEmpty()) {
                        result.participants_ = new java.util.ArrayList<java.lang.String>();
                    }
                    result.participants_.add(value);
                    return this;
                }

                public Builder addAllParticipants(java.lang.Iterable<? extends java.lang.String> values) {
                    if (result.participants_.isEmpty()) {
                        result.participants_ = new java.util.ArrayList<java.lang.String>();
                    }
                    super.addAll(values, result.participants_);
                    return this;
                }

                public Builder clearParticipants() {
                    result.participants_ = java.util.Collections.emptyList();
                    return this;
                }

                public boolean hasAuthor() {
                    return result.hasAuthor();
                }

                public java.lang.String getAuthor() {
                    return result.getAuthor();
                }

                public Builder setAuthor(java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    result.hasAuthor = true;
                    result.author_ = value;
                    return this;
                }

                public Builder clearAuthor() {
                    result.hasAuthor = false;
                    result.author_ = getDefaultInstance().getAuthor();
                    return this;
                }
            }

            static {
                defaultInstance = new Digest(true);
                org.waveprotocol.box.search.SearchProto.internalForceInit();
                defaultInstance.initFields();
            }
        }

        public static final int QUERY_FIELD_NUMBER = 1;

        private boolean hasQuery;

        private java.lang.String query_ = "";

        public boolean hasQuery() {
            return hasQuery;
        }

        public java.lang.String getQuery() {
            return query_;
        }

        public static final int TOTALRESULTS_FIELD_NUMBER = 2;

        private boolean hasTotalResults;

        private int totalResults_ = 0;

        public boolean hasTotalResults() {
            return hasTotalResults;
        }

        public int getTotalResults() {
            return totalResults_;
        }

        public static final int DIGESTS_FIELD_NUMBER = 3;

        private java.util.List<org.waveprotocol.box.search.SearchProto.SearchResponse.Digest> digests_ = java.util.Collections.emptyList();

        public java.util.List<org.waveprotocol.box.search.SearchProto.SearchResponse.Digest> getDigestsList() {
            return digests_;
        }

        public int getDigestsCount() {
            return digests_.size();
        }

        public org.waveprotocol.box.search.SearchProto.SearchResponse.Digest getDigests(int index) {
            return digests_.get(index);
        }

        private void initFields() {
        }

        public final boolean isInitialized() {
            if (!hasQuery) return false;
            if (!hasTotalResults) return false;
            for (org.waveprotocol.box.search.SearchProto.SearchResponse.Digest element : getDigestsList()) {
                if (!element.isInitialized()) return false;
            }
            return true;
        }

        public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
            getSerializedSize();
            if (hasQuery()) {
                output.writeString(1, getQuery());
            }
            if (hasTotalResults()) {
                output.writeInt32(2, getTotalResults());
            }
            for (org.waveprotocol.box.search.SearchProto.SearchResponse.Digest element : getDigestsList()) {
                output.writeMessage(3, element);
            }
            getUnknownFields().writeTo(output);
        }

        private int memoizedSerializedSize = -1;

        public int getSerializedSize() {
            int size = memoizedSerializedSize;
            if (size != -1) return size;
            size = 0;
            if (hasQuery()) {
                size += com.google.protobuf.CodedOutputStream.computeStringSize(1, getQuery());
            }
            if (hasTotalResults()) {
                size += com.google.protobuf.CodedOutputStream.computeInt32Size(2, getTotalResults());
            }
            for (org.waveprotocol.box.search.SearchProto.SearchResponse.Digest element : getDigestsList()) {
                size += com.google.protobuf.CodedOutputStream.computeMessageSize(3, element);
            }
            size += getUnknownFields().getSerializedSize();
            memoizedSerializedSize = size;
            return size;
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return newBuilder().mergeFrom(data, extensionRegistry).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseFrom(java.io.InputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
            Builder builder = newBuilder();
            if (builder.mergeDelimitedFrom(input)) {
                return builder.buildParsed();
            } else {
                return null;
            }
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            Builder builder = newBuilder();
            if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
                return builder.buildParsed();
            } else {
                return null;
            }
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
            return newBuilder().mergeFrom(input).buildParsed();
        }

        public static org.waveprotocol.box.search.SearchProto.SearchResponse parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            return newBuilder().mergeFrom(input, extensionRegistry).buildParsed();
        }

        public static Builder newBuilder() {
            return Builder.create();
        }

        public Builder newBuilderForType() {
            return newBuilder();
        }

        public static Builder newBuilder(org.waveprotocol.box.search.SearchProto.SearchResponse prototype) {
            return newBuilder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return newBuilder(this);
        }

        public static final class Builder extends com.google.protobuf.GeneratedMessage.Builder<Builder> {

            private org.waveprotocol.box.search.SearchProto.SearchResponse result;

            private Builder() {
            }

            private static Builder create() {
                Builder builder = new Builder();
                builder.result = new org.waveprotocol.box.search.SearchProto.SearchResponse();
                return builder;
            }

            protected org.waveprotocol.box.search.SearchProto.SearchResponse internalGetResult() {
                return result;
            }

            public Builder clear() {
                if (result == null) {
                    throw new IllegalStateException("Cannot call clear() after build().");
                }
                result = new org.waveprotocol.box.search.SearchProto.SearchResponse();
                return this;
            }

            public Builder clone() {
                return create().mergeFrom(result);
            }

            public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
                return org.waveprotocol.box.search.SearchProto.SearchResponse.getDescriptor();
            }

            public org.waveprotocol.box.search.SearchProto.SearchResponse getDefaultInstanceForType() {
                return org.waveprotocol.box.search.SearchProto.SearchResponse.getDefaultInstance();
            }

            public boolean isInitialized() {
                return result.isInitialized();
            }

            public org.waveprotocol.box.search.SearchProto.SearchResponse build() {
                if (result != null && !isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return buildPartial();
            }

            private org.waveprotocol.box.search.SearchProto.SearchResponse buildParsed() throws com.google.protobuf.InvalidProtocolBufferException {
                if (!isInitialized()) {
                    throw newUninitializedMessageException(result).asInvalidProtocolBufferException();
                }
                return buildPartial();
            }

            public org.waveprotocol.box.search.SearchProto.SearchResponse buildPartial() {
                if (result == null) {
                    throw new IllegalStateException("build() has already been called on this Builder.");
                }
                if (result.digests_ != java.util.Collections.EMPTY_LIST) {
                    result.digests_ = java.util.Collections.unmodifiableList(result.digests_);
                }
                org.waveprotocol.box.search.SearchProto.SearchResponse returnMe = result;
                result = null;
                return returnMe;
            }

            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof org.waveprotocol.box.search.SearchProto.SearchResponse) {
                    return mergeFrom((org.waveprotocol.box.search.SearchProto.SearchResponse) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(org.waveprotocol.box.search.SearchProto.SearchResponse other) {
                if (other == org.waveprotocol.box.search.SearchProto.SearchResponse.getDefaultInstance()) return this;
                if (other.hasQuery()) {
                    setQuery(other.getQuery());
                }
                if (other.hasTotalResults()) {
                    setTotalResults(other.getTotalResults());
                }
                if (!other.digests_.isEmpty()) {
                    if (result.digests_.isEmpty()) {
                        result.digests_ = new java.util.ArrayList<org.waveprotocol.box.search.SearchProto.SearchResponse.Digest>();
                    }
                    result.digests_.addAll(other.digests_);
                }
                this.mergeUnknownFields(other.getUnknownFields());
                return this;
            }

            public Builder mergeFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
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
                        case 10:
                            {
                                setQuery(input.readString());
                                break;
                            }
                        case 16:
                            {
                                setTotalResults(input.readInt32());
                                break;
                            }
                        case 26:
                            {
                                org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.Builder subBuilder = org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.newBuilder();
                                input.readMessage(subBuilder, extensionRegistry);
                                addDigests(subBuilder.buildPartial());
                                break;
                            }
                    }
                }
            }

            public boolean hasQuery() {
                return result.hasQuery();
            }

            public java.lang.String getQuery() {
                return result.getQuery();
            }

            public Builder setQuery(java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                result.hasQuery = true;
                result.query_ = value;
                return this;
            }

            public Builder clearQuery() {
                result.hasQuery = false;
                result.query_ = getDefaultInstance().getQuery();
                return this;
            }

            public boolean hasTotalResults() {
                return result.hasTotalResults();
            }

            public int getTotalResults() {
                return result.getTotalResults();
            }

            public Builder setTotalResults(int value) {
                result.hasTotalResults = true;
                result.totalResults_ = value;
                return this;
            }

            public Builder clearTotalResults() {
                result.hasTotalResults = false;
                result.totalResults_ = 0;
                return this;
            }

            public java.util.List<org.waveprotocol.box.search.SearchProto.SearchResponse.Digest> getDigestsList() {
                return java.util.Collections.unmodifiableList(result.digests_);
            }

            public int getDigestsCount() {
                return result.getDigestsCount();
            }

            public org.waveprotocol.box.search.SearchProto.SearchResponse.Digest getDigests(int index) {
                return result.getDigests(index);
            }

            public Builder setDigests(int index, org.waveprotocol.box.search.SearchProto.SearchResponse.Digest value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                result.digests_.set(index, value);
                return this;
            }

            public Builder setDigests(int index, org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.Builder builderForValue) {
                result.digests_.set(index, builderForValue.build());
                return this;
            }

            public Builder addDigests(org.waveprotocol.box.search.SearchProto.SearchResponse.Digest value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                if (result.digests_.isEmpty()) {
                    result.digests_ = new java.util.ArrayList<org.waveprotocol.box.search.SearchProto.SearchResponse.Digest>();
                }
                result.digests_.add(value);
                return this;
            }

            public Builder addDigests(org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.Builder builderForValue) {
                if (result.digests_.isEmpty()) {
                    result.digests_ = new java.util.ArrayList<org.waveprotocol.box.search.SearchProto.SearchResponse.Digest>();
                }
                result.digests_.add(builderForValue.build());
                return this;
            }

            public Builder addAllDigests(java.lang.Iterable<? extends org.waveprotocol.box.search.SearchProto.SearchResponse.Digest> values) {
                if (result.digests_.isEmpty()) {
                    result.digests_ = new java.util.ArrayList<org.waveprotocol.box.search.SearchProto.SearchResponse.Digest>();
                }
                super.addAll(values, result.digests_);
                return this;
            }

            public Builder clearDigests() {
                result.digests_ = java.util.Collections.emptyList();
                return this;
            }
        }

        static {
            defaultInstance = new SearchResponse(true);
            org.waveprotocol.box.search.SearchProto.internalForceInit();
            defaultInstance.initFields();
        }
    }

    private static com.google.protobuf.Descriptors.Descriptor internal_static_search_SearchRequest_descriptor;

    private static com.google.protobuf.GeneratedMessage.FieldAccessorTable internal_static_search_SearchRequest_fieldAccessorTable;

    private static com.google.protobuf.Descriptors.Descriptor internal_static_search_SearchResponse_descriptor;

    private static com.google.protobuf.GeneratedMessage.FieldAccessorTable internal_static_search_SearchResponse_fieldAccessorTable;

    private static com.google.protobuf.Descriptors.Descriptor internal_static_search_SearchResponse_Digest_descriptor;

    private static com.google.protobuf.GeneratedMessage.FieldAccessorTable internal_static_search_SearchResponse_Digest_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        java.lang.String[] descriptorData = { "\n(org/waveprotocol/box/search/search.pro" + "to\022\006search\"A\n\rSearchRequest\022\r\n\005query\030\001 \002" + "(\t\022\r\n\005index\030\002 \002(\005\022\022\n\nnumResults\030\003 \002(\005\"\204\002" + "\n\016SearchResponse\022\r\n\005query\030\001 \002(\t\022\024\n\014total" + "Results\030\002 \002(\005\022.\n\007digests\030\003 \003(\0132\035.search." + "SearchResponse.Digest\032\234\001\n\006Digest\022\r\n\005titl" + "e\030\001 \002(\t\022\017\n\007snippet\030\002 \002(\t\022\016\n\006waveId\030\003 \002(\t" + "\022\024\n\014lastModified\030\004 \002(\003\022\023\n\013unreadCount\030\005 " + "\002(\005\022\021\n\tblipCount\030\006 \002(\005\022\024\n\014participants\030\007" + " \003(\t\022\016\n\006author\030\010 \002(\tB*\n\033org.waveprotocol", ".box.searchB\013SearchProto" };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {

            public com.google.protobuf.ExtensionRegistry assignDescriptors(com.google.protobuf.Descriptors.FileDescriptor root) {
                descriptor = root;
                internal_static_search_SearchRequest_descriptor = getDescriptor().getMessageTypes().get(0);
                internal_static_search_SearchRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessage.FieldAccessorTable(internal_static_search_SearchRequest_descriptor, new java.lang.String[] { "Query", "Index", "NumResults" }, org.waveprotocol.box.search.SearchProto.SearchRequest.class, org.waveprotocol.box.search.SearchProto.SearchRequest.Builder.class);
                internal_static_search_SearchResponse_descriptor = getDescriptor().getMessageTypes().get(1);
                internal_static_search_SearchResponse_fieldAccessorTable = new com.google.protobuf.GeneratedMessage.FieldAccessorTable(internal_static_search_SearchResponse_descriptor, new java.lang.String[] { "Query", "TotalResults", "Digests" }, org.waveprotocol.box.search.SearchProto.SearchResponse.class, org.waveprotocol.box.search.SearchProto.SearchResponse.Builder.class);
                internal_static_search_SearchResponse_Digest_descriptor = internal_static_search_SearchResponse_descriptor.getNestedTypes().get(0);
                internal_static_search_SearchResponse_Digest_fieldAccessorTable = new com.google.protobuf.GeneratedMessage.FieldAccessorTable(internal_static_search_SearchResponse_Digest_descriptor, new java.lang.String[] { "Title", "Snippet", "WaveId", "LastModified", "UnreadCount", "BlipCount", "Participants", "Author" }, org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.class, org.waveprotocol.box.search.SearchProto.SearchResponse.Digest.Builder.class);
                return null;
            }
        };
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {}, assigner);
    }

    public static void internalForceInit() {
    }
}
