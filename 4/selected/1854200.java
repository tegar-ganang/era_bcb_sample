package collab.fm.server.bean.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import collab.fm.server.bean.entity.BinaryRelationship;
import collab.fm.server.bean.entity.Feature;
import collab.fm.server.bean.entity.Model;
import collab.fm.server.bean.entity.Relationship;
import collab.fm.server.bean.entity.attr.Attribute;
import collab.fm.server.bean.transfer.Attribute2;
import collab.fm.server.bean.transfer.BinaryRelation2;
import collab.fm.server.bean.transfer.Feature2;
import collab.fm.server.processor.Processor;
import collab.fm.server.util.DaoUtil;
import collab.fm.server.util.EntityUtil;
import collab.fm.server.util.Resources;
import collab.fm.server.util.exception.EntityPersistenceException;
import collab.fm.server.util.exception.InvalidOperationException;
import collab.fm.server.util.exception.StaleDataException;

public class UpdateRequest extends Request {

    private Long modelId;

    @Override
    protected Processor makeDefaultProcessor() {
        return new UpdateProcessor();
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    private static class UpdateProcessor implements Processor {

        public boolean checkRequest(Request req) {
            if (!(req instanceof UpdateRequest)) return false;
            return ((UpdateRequest) req).getModelId() != null;
        }

        public boolean process(Request req, ResponseGroup rg) throws EntityPersistenceException, StaleDataException, InvalidOperationException {
            if (!checkRequest(req)) {
                throw new InvalidOperationException("Invalid update operation.");
            }
            UpdateRequest r = (UpdateRequest) req;
            List<Feature> allFeatures = DaoUtil.getFeatureDao().getAll(r.getModelId());
            List<Feature2> list1 = new ArrayList<Feature2>();
            if (allFeatures != null) {
                for (Feature f : allFeatures) {
                    Feature2 f2 = new Feature2();
                    f.transfer(f2);
                    list1.add(f2);
                }
            }
            List<Relationship> allRelation = DaoUtil.getRelationshipDao().getAll(r.getModelId());
            List<BinaryRelation2> list2 = new ArrayList<BinaryRelation2>();
            if (allRelation != null) {
                for (Relationship rel : allRelation) {
                    if (isBinary(rel.getType())) {
                        BinaryRelation2 r2 = new BinaryRelation2();
                        ((BinaryRelationship) rel).transfer(r2);
                        list2.add(r2);
                    }
                }
            }
            Model m = DaoUtil.getModelDao().getById(r.getModelId(), false);
            List<Attribute2> list3 = new ArrayList<Attribute2>();
            for (Map.Entry<String, Attribute> e : m.getFeatureAttrs().entrySet()) {
                list3.add(EntityUtil.transferFromAttr(e.getValue()));
            }
            UpdateResponse response = new UpdateResponse(r);
            response.setFeatures(list1);
            response.setBinaries(list2);
            response.setAttrs(list3);
            response.setName(Resources.RSP_SUCCESS);
            rg.setBack(response);
            return true;
        }

        private boolean isBinary(String type) {
            return Resources.BIN_REL_EXCLUDES.equals(type) || Resources.BIN_REL_REFINES.equals(type) || Resources.BIN_REL_REQUIRES.equals(type);
        }
    }

    public static class UpdateResponse extends Response {

        private List<Feature2> features;

        private List<BinaryRelation2> binaries;

        private List<Attribute2> attrs;

        public UpdateResponse(Request r) {
            super(r);
        }

        public List<Feature2> getFeatures() {
            return features;
        }

        public void setFeatures(List<Feature2> features) {
            this.features = features;
        }

        public List<BinaryRelation2> getBinaries() {
            return binaries;
        }

        public void setBinaries(List<BinaryRelation2> binaries) {
            this.binaries = binaries;
        }

        public List<Attribute2> getAttrs() {
            return attrs;
        }

        public void setAttrs(List<Attribute2> attrs) {
            this.attrs = attrs;
        }
    }
}
