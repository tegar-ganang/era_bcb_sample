package source.model;

import source.model.type.GameObjectType;

/**
 * This will represent the ID for all in game objects.
 * @author Sean Larson
 *
 */
public class ObjectID {

    private String id;

    public String getID() {
        return id;
    }

    public ObjectID(String id) {
        this.id = id;
    }

    public boolean equals(ObjectID o) {
        return id.equals(o.getID());
    }

    public int getTypePrefix() {
        String[] temp;
        temp = this.getID().split("\\.");
        int first = Integer.parseInt(temp[0]);
        return first;
    }

    public static ObjectID[] sortDecending(ObjectID[] oids) {
        for (int i = 1; i < oids.length; i++) {
            ObjectID iId = oids[i];
            for (int j = 0; j < oids.length - i; j++) {
                if (oids[j].getTypePrefix() > oids[j + 1].getTypePrefix()) {
                    ObjectID temp = oids[j];
                    oids[j] = oids[j + 1];
                    oids[j + 1] = temp;
                }
            }
        }
        return oids;
    }
}
