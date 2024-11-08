package pl.softech.engine3d.math3d;

public class Polygon3D implements Transformable {

    private static Vector3D temp1 = new Vector3D();

    private static Vector3D temp2 = new Vector3D();

    private Vector3D[] v;

    private int numVertices;

    private Vector3D normal;

    public Polygon3D() {
        numVertices = 0;
        v = new Vector3D[0];
        normal = new Vector3D();
    }

    public Polygon3D(Vector3D v0, Vector3D v1, Vector3D v2) {
        this(new Vector3D[] { v0, v1, v2 });
    }

    public Polygon3D(Vector3D v0, Vector3D v1, Vector3D v2, Vector3D v3) {
        this(new Vector3D[] { v0, v1, v2, v3 });
    }

    public Polygon3D(Vector3D[] vertices) {
        this.v = vertices;
        numVertices = vertices.length;
        calcNormal();
    }

    public void setTo(Polygon3D polygon) {
        numVertices = polygon.numVertices;
        normal.setTo(polygon.normal);
        ensureCapacity(numVertices);
        for (int i = 0; i < numVertices; i++) {
            v[i].setTo(polygon.v[i]);
        }
    }

    protected void ensureCapacity(int length) {
        if (v.length < length) {
            Vector3D[] newV = new Vector3D[length];
            System.arraycopy(v, 0, newV, 0, v.length);
            for (int i = v.length; i < newV.length; i++) {
                newV[i] = new Vector3D();
            }
            v = newV;
        }
    }

    public int getNumVertices() {
        return numVertices;
    }

    public Vector3D getVertex(int index) {
        return v[index];
    }

    public void project(ViewWindow view) {
        for (int i = 0; i < numVertices; i++) {
            view.project(v[i]);
        }
    }

    public void add(Vector3D u) {
        for (int i = 0; i < numVertices; i++) {
            v[i].add(u);
        }
    }

    public void subtract(Vector3D u) {
        for (int i = 0; i < numVertices; i++) {
            v[i].subtract(u);
        }
    }

    public void add(Transform3D xform) {
        addRotation(xform);
        add(xform.getLocation());
    }

    public void subtract(Transform3D xform) {
        subtract(xform.getLocation());
        subtractRotation(xform);
    }

    public void addRotation(Transform3D xform) {
        for (int i = 0; i < numVertices; i++) {
            v[i].addRotation(xform);
        }
        normal.addRotation(xform);
    }

    public void subtractRotation(Transform3D xform) {
        for (int i = 0; i < numVertices; i++) {
            v[i].subtractRotation(xform);
        }
        normal.subtractRotation(xform);
    }

    public Vector3D calcNormal() {
        if (normal == null) {
            normal = new Vector3D();
        }
        temp1.setTo(v[2]);
        temp1.subtract(v[1]);
        temp2.setTo(v[0]);
        temp2.subtract(v[1]);
        normal.setToCrossProduct(temp1, temp2);
        normal.normalize();
        return normal;
    }

    public Vector3D getNormal() {
        return normal;
    }

    public void setNormal(Vector3D n) {
        if (normal == null) {
            normal = new Vector3D(n);
        } else {
            normal.setTo(n);
        }
    }

    public boolean isFacing(Vector3D u) {
        temp1.setTo(u);
        temp1.subtract(v[0]);
        return (normal.getDotProduct(temp1) >= 0);
    }

    public boolean clip(float clipZ) {
        ensureCapacity(numVertices * 3);
        boolean isCompletelyHidden = true;
        for (int i = 0; i < numVertices; i++) {
            int next = (i + 1) % numVertices;
            Vector3D v1 = v[i];
            Vector3D v2 = v[next];
            if (v1.z < clipZ) {
                isCompletelyHidden = false;
            }
            if (v1.z > v2.z) {
                Vector3D temp = v1;
                v1 = v2;
                v2 = temp;
            }
            if (v1.z < clipZ && v2.z > clipZ) {
                float scale = (clipZ - v1.z) / (v2.z - v1.z);
                insertVertex(next, v1.x + scale * (v2.x - v1.x), v1.y + scale * (v2.y - v1.y), clipZ);
                i++;
            }
        }
        if (isCompletelyHidden) {
            return false;
        }
        for (int i = numVertices - 1; i >= 0; i--) {
            if (v[i].z > clipZ) {
                deleteVertex(i);
            }
        }
        return (numVertices >= 3);
    }

    protected void insertVertex(int index, float x, float y, float z) {
        Vector3D newVertex = v[v.length - 1];
        newVertex.x = x;
        newVertex.y = y;
        newVertex.z = z;
        for (int i = v.length - 1; i > index; i--) {
            v[i] = v[i - 1];
        }
        v[index] = newVertex;
        numVertices++;
    }

    protected void deleteVertex(int index) {
        Vector3D deleted = v[index];
        for (int i = index; i < v.length - 1; i++) {
            v[i] = v[i + 1];
        }
        v[v.length - 1] = deleted;
        numVertices--;
    }

    public void insertVertex(int index, Vector3D vertex) {
        Vector3D[] newV = new Vector3D[numVertices + 1];
        System.arraycopy(v, 0, newV, 0, index);
        newV[index] = vertex;
        System.arraycopy(v, index, newV, index + 1, numVertices - index);
        v = newV;
        numVertices++;
    }
}
