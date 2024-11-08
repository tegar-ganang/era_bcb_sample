package artofillusion.teddy;

import java.awt.Point;
import java.io.PrintStream;
import java.util.Enumeration;

class Skeleton {

    Skeleton() {
    }

    private static void cos_sin_init() {
        for (int i = -steps; i <= steps; i++) {
            cos[i + steps] = Math.cos((3.1415926535897931D * (double) i) / (double) steps / 2D);
            sin[i + steps] = Math.sin((3.1415926535897931D * (double) i) / (double) steps / 2D);
        }
        cos[0] = 0.0D;
        sin[0] = -1D;
        cos[steps] = 1.0D;
        sin[steps] = 0.0D;
        cos[steps * 2] = 0.0D;
        sin[steps * 2] = 1.0D;
    }

    public static void create_J_triangle(LinkedList linkedlist, LinkedList linkedlist1, SkEdge2D skedge2d) {
        LinkedList linkedlist2 = new LinkedList();
        SkEdge2D skedge2d1 = skedge2d;
        Object obj = null;
        do {
            skedge2d1.type = 2;
            linkedlist2.append(skedge2d1);
            SkVertex2D skvertex2d = (SkVertex2D) ((Edge2D) (skedge2d1)).start;
            if (skvertex2d != ((Edge2D) (skedge2d)).end) {
                LinkedList linkedlist3 = skvertex2d.get_not_shared_edges();
                SkEdge2D skedge2d2;
                if (linkedlist3.size() > 1) skedge2d2 = find_appropriate_edge(linkedlist3, skedge2d1, skvertex2d); else skedge2d2 = (SkEdge2D) linkedlist3.head();
                skedge2d1 = skedge2d2;
            } else {
                System.out.println("J" + linkedlist2.size());
                linkedlist.append(new SkPolygon2D(linkedlist2, 2));
                return;
            }
        } while (true);
    }

    public static void create_S_triangle(LinkedList linkedlist, LinkedList linkedlist1, SkVertex2D skvertex2d, SkVertex2D skvertex2d1, SkVertex2D skvertex2d2) {
        SkEdge2D skedge2d = new SkEdge2D(skvertex2d, skvertex2d1, 0);
        linkedlist1.append(skedge2d);
        SkEdge2D skedge2d1 = get_edge(linkedlist1, skvertex2d1, skvertex2d2);
        SkEdge2D skedge2d2 = get_edge(linkedlist1, skvertex2d2, skvertex2d);
        linkedlist.append(new SkPolygon2D(skedge2d, skedge2d1, skedge2d2, 1));
    }

    public static void create_T_triangle(LinkedList linkedlist, LinkedList linkedlist1, SkVertex2D skvertex2d, SkVertex2D skvertex2d1, SkVertex2D skvertex2d2) {
        SkEdge2D skedge2d = new SkEdge2D(skvertex2d, skvertex2d1, 0);
        linkedlist1.append(skedge2d);
        SkEdge2D skedge2d1 = new SkEdge2D(skvertex2d1, skvertex2d2, 0);
        linkedlist1.append(skedge2d1);
        SkEdge2D skedge2d2 = get_edge(linkedlist1, skvertex2d2, skvertex2d);
        linkedlist.append(new SkPolygon2D(skedge2d, skedge2d1, skedge2d2, 0));
    }

    private static void create_polygons_external(NewPolyhedron newpolyhedron, double d, double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        if (d3 == d && d5 == d2) System.out.println("clashed internal");
        for (int i = -steps; i < steps; i++) {
            double d17 = cos[i + steps];
            double d19 = d7 * sin[i + steps];
            double d18 = cos[i + 1 + steps];
            double d20 = d7 * sin[i + 1 + steps];
            double d9 = d6 + (d - d6) * d17;
            double d10 = d8 + (d2 - d8) * d17;
            double d11 = d6 + (d3 - d6) * d17;
            double d12 = d8 + (d5 - d8) * d17;
            double d13 = d6 + (d3 - d6) * d18;
            double d14 = d8 + (d5 - d8) * d18;
            double d15 = d6 + (d - d6) * d18;
            double d16 = d8 + (d2 - d8) * d18;
            if (i == -1) {
                d13 = d3;
                d14 = d5;
                d15 = d;
                d16 = d2;
                d20 = 0.0D;
            } else if (i == 0) {
                d9 = d;
                d10 = d2;
                d11 = d3;
                d12 = d5;
                d19 = 0.0D;
            }
            if (i == -steps) newpolyhedron.add_temp_polygon(d6, d19, d8, d15, d20, d16, d13, d20, d14); else if (i == steps - 1) newpolyhedron.add_temp_polygon(d11, d19, d12, d9, d19, d10, d6, d20, d8); else if (Vector2.distance(d9, d10, d13, d14) > Vector2.distance(d11, d12, d15, d16)) {
                newpolyhedron.add_temp_polygon(d15, d20, d16, d13, d20, d14, d11, d19, d12);
                newpolyhedron.add_temp_polygon(d15, d20, d16, d11, d19, d12, d9, d19, d10);
            } else {
                newpolyhedron.add_temp_polygon(d15, d20, d16, d13, d20, d14, d9, d19, d10);
                newpolyhedron.add_temp_polygon(d13, d20, d14, d11, d19, d12, d9, d19, d10);
            }
        }
    }

    private static void create_polygons_internal(NewPolyhedron newpolyhedron, double d, double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        if (d == d3 && d2 == d5 && d1 == d4) System.out.println("clashed internal");
        for (int i = -steps; i < steps; i++) {
            double d17 = cos[i + steps];
            double d19 = d4 * sin[i + steps];
            double d20 = d1 * sin[i + steps];
            double d18 = cos[i + 1 + steps];
            double d21 = d4 * sin[i + 1 + steps];
            double d22 = d1 * sin[i + 1 + steps];
            if (i == -steps) {
                d17 = 0.0D;
                d19 = -d4;
                d20 = -d1;
            } else if (i == steps - 1) {
                d18 = 0.0D;
                d21 = d4;
                d22 = d1;
            }
            double d9 = d3 + (d6 - d3) * d17;
            double d10 = d5 + (d8 - d5) * d17;
            double d11 = d + (d6 - d) * d17;
            double d12 = d2 + (d8 - d2) * d17;
            double d13 = d + (d6 - d) * d18;
            double d14 = d2 + (d8 - d2) * d18;
            double d15 = d3 + (d6 - d3) * d18;
            double d16 = d5 + (d8 - d5) * d18;
            if (i == -1) newpolyhedron.add_temp_polygon(d6, 0.0D, d8, d11, d20, d12, d9, d19, d10); else if (i == 0) newpolyhedron.add_temp_polygon(d15, d21, d16, d13, d22, d14, d6, 0.0D, d8); else if (Vector2.distance(d9, d10, d13, d14) > Vector2.distance(d11, d12, d15, d16)) {
                newpolyhedron.add_temp_polygon(d15, d21, d16, d13, d22, d14, d11, d20, d12);
                newpolyhedron.add_temp_polygon(d15, d21, d16, d11, d20, d12, d9, d19, d10);
            } else {
                newpolyhedron.add_temp_polygon(d15, d21, d16, d13, d22, d14, d9, d19, d10);
                newpolyhedron.add_temp_polygon(d13, d22, d14, d11, d20, d12, d9, d19, d10);
            }
        }
    }

    private static void create_polygons_junction(NewPolyhedron newpolyhedron, double d, double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8, double d9, double d10, double d11) {
        double d12 = Vector2.distance(d6, d8, d9, d11);
        if (d12 < Vector2.distance(d6, d8, d, d2) && d12 < Vector2.distance(d6, d8, d3, d5) && d12 < Vector2.distance(d9, d11, d, d2) && d12 < Vector2.distance(d9, d11, d, d2)) {
            create_polygons_junction_simple(newpolyhedron, d6, d7, d8, d9, d10, d11, d, d1, d2);
            create_polygons_junction_simple(newpolyhedron, d9, d10, d11, d6, d7, d8, d3, d4, d5);
        } else {
            create_polygons_junction_complicated(newpolyhedron, d6, d7, d8, d9, d10, d11, d, d1, d2);
            create_polygons_junction_complicated(newpolyhedron, d9, d10, d11, d6, d7, d8, d3, d4, d5);
        }
    }

    private static void create_polygons_junction_complicated(NewPolyhedron newpolyhedron, double d, double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        Vertex avertex[] = new Vertex[steps + 1];
        Vertex avertex1[] = new Vertex[steps + 1];
        Vertex avertex2[] = new Vertex[steps + 1];
        create_polygons_junction_prepare(avertex, d3, d4, d5, d6, 0.0D, d8);
        create_polygons_junction_prepare(avertex1, d, d1, d2, d6, 0.0D, d8);
        create_polygons_junction_prepare(avertex2, d, d1, d2, d3, d4, d5);
        Vertex avertex3[] = new Vertex[steps * 3 + 1];
        for (int i = 0; i < steps; i++) avertex3[i] = avertex[i];
        for (int j = 0; j < steps; j++) avertex3[steps + j] = avertex1[steps - j];
        for (int k = 0; k <= steps; k++) avertex3[steps * 2 + k] = avertex2[k];
        LinkedList linkedlist = delauny(avertex3);
        Vertex avertex4[];
        for (Enumeration enumeration = linkedlist.elements(); enumeration.hasMoreElements(); create_polygons_junction_sub(avertex4[0], avertex4[1], avertex4[2])) avertex4 = (Vertex[]) enumeration.nextElement();
    }

    private static void create_polygons_junction_prepare(Vertex avertex[], double d, double d1, double d2, double d3, double d4, double d5) {
        if (d1 > d4) {
            create_polygons_junction_prepare_sub(avertex, d, d1, d2, d3, d4, d5);
        } else {
            create_polygons_junction_prepare_sub(avertex, d3, d4, d5, d, d1, d2);
            Vertex avertex1[] = new Vertex[steps + 1];
            for (int i = 0; i <= steps; i++) avertex1[i] = avertex[steps - i];
            for (int j = 0; j <= steps; j++) avertex[j] = avertex1[j];
        }
    }

    private static void create_polygons_junction_prepare_sub(Vertex avertex[], double d, double d1, double d2, double d3, double d4, double d5) {
        for (int i = 0; i <= steps; i++) {
            double d8 = cos[i + steps];
            double d9 = sin[i + steps];
            double d6 = d + (d3 - d) * d8;
            double d7 = d2 + (d5 - d2) * d8;
            d9 = d4 + (d1 - d4) * d9;
            if (i == 0) {
                d6 = d3;
                d7 = d5;
                d9 = d4;
            }
            if (i == steps) {
                d6 = d;
                d7 = d2;
                d9 = d1;
            }
            avertex[steps - i] = new Vertex(d6, -d9, d7);
        }
        if (d1 > 0.0D && d4 > 0.0D) {
            double d10 = ((Vector3) (avertex[0])).x;
            double d11 = (((Vector3) (avertex[steps])).x - ((Vector3) (avertex[0])).x) / (double) steps;
            double d12 = ((Vector3) (avertex[0])).z;
            double d13 = (((Vector3) (avertex[steps])).z - ((Vector3) (avertex[0])).z) / (double) steps;
            for (int j = 1; j <= steps - 1; j++) {
                avertex[j].x = d10 + d11 * (double) j;
                avertex[j].z = d12 + d13 * (double) j;
            }
        }
    }

    private static void create_polygons_junction_simple(NewPolyhedron newpolyhedron, double d, double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        create_polygons_junction_simple_sub(newpolyhedron, d, d1, d2, d3, d4, d5, d6, 0.0D, d8);
        create_polygons_junction_simple_sub(newpolyhedron, d3, -d4, d5, d, -d1, d2, d6, 0.0D, d8);
    }

    private static void create_polygons_junction_simple_sub(NewPolyhedron newpolyhedron, double d, double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        for (int i = 0; i < steps; i++) {
            double d17 = cos[i + steps];
            double d19 = d7 + (d4 - d7) * sin[i + steps];
            double d20 = d7 + (d1 - d7) * sin[i + steps];
            double d18 = cos[i + 1 + steps];
            double d21 = d7 + (d4 - d7) * sin[i + 1 + steps];
            double d22 = d7 + (d1 - d7) * sin[i + 1 + steps];
            if (i == steps - 1) {
                d18 = 0.0D;
                d21 = d4;
                d22 = d1;
            }
            double d9 = d3 + (d6 - d3) * d17;
            double d10 = d5 + (d8 - d5) * d17;
            double d11 = d + (d6 - d) * d17;
            double d12 = d2 + (d8 - d2) * d17;
            double d13 = d + (d6 - d) * d18;
            double d14 = d2 + (d8 - d2) * d18;
            double d15 = d3 + (d6 - d3) * d18;
            double d16 = d5 + (d8 - d5) * d18;
            if (i == 0) {
                newpolyhedron.add_temp_polygon(d15, d21, d16, d13, d22, d14, d6, d7, d8);
            } else {
                newpolyhedron.add_temp_polygon(d15, d21, d16, d13, d22, d14, d11, d20, d12);
                newpolyhedron.add_temp_polygon(d15, d21, d16, d11, d20, d12, d9, d19, d10);
            }
        }
    }

    private static void create_polygons_junction_sub(Vertex vertex, Vertex vertex1, Vertex vertex2) {
        h.add_temp_polygon(((Vector3) (vertex)).x, ((Vector3) (vertex)).y, ((Vector3) (vertex)).z, ((Vector3) (vertex1)).x, ((Vector3) (vertex1)).y, ((Vector3) (vertex1)).z, ((Vector3) (vertex2)).x, ((Vector3) (vertex2)).y, ((Vector3) (vertex2)).z);
        h.add_temp_polygon(((Vector3) (vertex1)).x, -((Vector3) (vertex1)).y, ((Vector3) (vertex1)).z, ((Vector3) (vertex)).x, -((Vector3) (vertex)).y, ((Vector3) (vertex)).z, ((Vector3) (vertex2)).x, -((Vector3) (vertex2)).y, ((Vector3) (vertex2)).z);
    }

    private static void create_polygons_junction_sub(Vertex vertex, Vertex vertex1, Vertex vertex2, Vertex vertex3) {
        if (Vector3.distance(vertex, vertex2) > Vector3.distance(vertex1, vertex3)) {
            Vertex vertex4 = vertex3;
            vertex3 = vertex2;
            vertex2 = vertex1;
            vertex1 = vertex;
            vertex = vertex4;
        }
        h.add_temp_polygon(((Vector3) (vertex)).x, ((Vector3) (vertex)).y, ((Vector3) (vertex)).z, ((Vector3) (vertex1)).x, ((Vector3) (vertex1)).y, ((Vector3) (vertex1)).z, ((Vector3) (vertex2)).x, ((Vector3) (vertex2)).y, ((Vector3) (vertex2)).z);
        h.add_temp_polygon(((Vector3) (vertex)).x, ((Vector3) (vertex)).y, ((Vector3) (vertex)).z, ((Vector3) (vertex2)).x, ((Vector3) (vertex2)).y, ((Vector3) (vertex2)).z, ((Vector3) (vertex3)).x, ((Vector3) (vertex3)).y, ((Vector3) (vertex3)).z);
        h.add_temp_polygon(((Vector3) (vertex1)).x, -((Vector3) (vertex1)).y, ((Vector3) (vertex1)).z, ((Vector3) (vertex)).x, -((Vector3) (vertex)).y, ((Vector3) (vertex)).z, ((Vector3) (vertex2)).x, -((Vector3) (vertex2)).y, ((Vector3) (vertex2)).z);
        h.add_temp_polygon(((Vector3) (vertex2)).x, -((Vector3) (vertex2)).y, ((Vector3) (vertex2)).z, ((Vector3) (vertex)).x, -((Vector3) (vertex)).y, ((Vector3) (vertex)).z, ((Vector3) (vertex3)).x, -((Vector3) (vertex3)).y, ((Vector3) (vertex3)).z);
    }

    private static void create_polygons_square(NewPolyhedron newpolyhedron, double d, double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8, double d9, double d10, double d11) {
        for (int i = -steps; i < steps; i++) {
            double d20 = cos[i + steps];
            double d22 = d10 * sin[i + steps];
            double d23 = d7 * sin[i + steps];
            double d21 = cos[i + 1 + steps];
            double d24 = d10 * sin[i + 1 + steps];
            double d25 = d7 * sin[i + 1 + steps];
            if (i == -steps) {
                d20 = 0.0D;
                d22 = -d10;
                d23 = -d7;
            } else if (i == steps - 1) {
                d21 = 0.0D;
                d24 = d10;
                d25 = d7;
            }
            double d12 = d9 + (d - d9) * d20;
            double d13 = d11 + (d2 - d11) * d20;
            double d14 = d6 + (d3 - d6) * d20;
            double d15 = d8 + (d5 - d8) * d20;
            double d16 = d6 + (d3 - d6) * d21;
            double d17 = d8 + (d5 - d8) * d21;
            double d18 = d9 + (d - d9) * d21;
            double d19 = d11 + (d2 - d11) * d21;
            if (i == -1) {
                d16 = d3;
                d17 = d5;
                d18 = d;
                d19 = d2;
                d25 = 0.0D;
            } else if (i == 0) {
                d12 = d;
                d13 = d2;
                d14 = d3;
                d15 = d5;
                d23 = 0.0D;
            }
            if (Vector2.distance(d12, d13, d16, d17) > Vector2.distance(d14, d15, d18, d19)) {
                newpolyhedron.add_temp_polygon(d18, d24, d19, d16, d25, d17, d14, d23, d15);
                newpolyhedron.add_temp_polygon(d18, d24, d19, d14, d23, d15, d12, d22, d13);
            } else {
                newpolyhedron.add_temp_polygon(d18, d24, d19, d16, d25, d17, d12, d22, d13);
                newpolyhedron.add_temp_polygon(d16, d25, d17, d14, d23, d15, d12, d22, d13);
            }
        }
    }

    private static LinkedList delauny(Vertex avertex[]) {
        LinkedList linkedlist = new LinkedList();
        for (int i = 0; i < steps * 3; i++) {
            Vertex avertex1[] = new Vertex[3];
            avertex1[0] = avertex[i];
            avertex1[1] = avertex[i + 1];
            avertex1[2] = delauny_sub(avertex, avertex[i], avertex[i + 1]);
            if ((i <= 0 || !avertex1[2].same_position(avertex[i - 1])) && (i != 0 || !avertex1[2].same_position(avertex[steps * 3 - 1]))) linkedlist.append(avertex1);
        }
        if (linkedlist.size() == 10) return linkedlist;
        Enumeration enumeration = linkedlist.elements();
        Vertex avertex5[] = new Vertex[3];
        while (enumeration.hasMoreElements()) {
            Vertex avertex2[] = (Vertex[]) enumeration.nextElement();
            Vertex vertex = avertex2[2];
            if (get_origin(avertex, vertex) == 1) {
                avertex5[0] = avertex2[0];
                break;
            }
        }
        while (enumeration.hasMoreElements()) {
            Vertex avertex3[] = (Vertex[]) enumeration.nextElement();
            Vertex vertex1 = avertex3[2];
            if (get_origin(avertex, vertex1) == 2) {
                avertex5[1] = avertex3[0];
                break;
            }
        }
        while (enumeration.hasMoreElements()) {
            Vertex avertex4[] = (Vertex[]) enumeration.nextElement();
            Vertex vertex2 = avertex4[2];
            if (get_origin(avertex, vertex2) == 0) {
                avertex5[2] = avertex4[0];
                linkedlist.append(avertex5);
                break;
            }
        }
        return linkedlist;
    }

    private static Vertex delauny_sub(Vertex avertex[], Vertex vertex, Vertex vertex1) {
        Vertex2D vertex2d = new Vertex2D(((Vector3) (vertex)).x, ((Vector3) (vertex)).z);
        Vertex2D vertex2d1 = new Vertex2D(((Vector3) (vertex1)).x, ((Vector3) (vertex1)).z);
        int i = steps * 3 + 1;
        double d = 1.0D;
        Vertex vertex2 = null;
        byte byte0 = -100;
        for (int k = 0; k < i - 1; k++) if (avertex[k] != vertex && avertex[k] != vertex1) {
            Vertex2D vertex2d2 = new Vertex2D(((Vector3) (avertex[k])).x, ((Vector3) (avertex[k])).z);
            if (!Geometry2D.left_side(vertex2d, vertex2d1, vertex2d2)) {
                double d1 = (new Vector2(vertex2d2, vertex2d)).get_cos(new Vector2(vertex2d2, vertex2d1));
                if (d1 < d) {
                    d = d1;
                    vertex2 = avertex[k];
                    int j = k;
                }
            }
        }
        return vertex2;
    }

    private static SkEdge2D find_appropriate_edge(LinkedList linkedlist, SkEdge2D skedge2d, SkVertex2D skvertex2d) {
        Vector2 vector2 = skedge2d.vector2();
        if (((Edge2D) (skedge2d)).start != skvertex2d) vector2.multiple_self(-1D);
        SkEdge2D skedge2d1 = null;
        double d = 360D;
        for (Enumeration enumeration = linkedlist.elements(); enumeration.hasMoreElements(); ) {
            SkEdge2D skedge2d2 = (SkEdge2D) enumeration.nextElement();
            Vector2 vector2_1 = skedge2d2.vector2();
            if (((Edge2D) (skedge2d2)).start != skvertex2d) vector2_1.multiple_self(-1D);
            double d1 = vector2.get_angle(vector2_1);
            if (d1 < d) {
                d = d1;
                skedge2d1 = skedge2d2;
            }
        }
        return skedge2d1;
    }

    public static Polyhedron generate_polyhedron(LinkedList linkedlist) {
        System.out.println("Skeleton start ------");
        DrawPanel.special_segments = new LinkedList();
        int i = linkedlist.size();
        vertices = new SkVertex2D[linkedlist.size()];
        int j = 0;
        for (Enumeration enumeration = linkedlist.elements(); enumeration.hasMoreElements(); ) vertices[j++] = new SkVertex2D((Point) enumeration.nextElement());
        vertices[i - 1] = vertices[0];
        n_vertices = i - 1;
        edges = new LinkedList();
        triangles = new LinkedList();
        for (int k = 0; k < i - 1; k++) {
            SkVertex2D skvertex2d = vertices[k];
            SkVertex2D skvertex2d1 = vertices[k + 1];
            double d = 1.0D;
            SkVertex2D skvertex2d3 = null;
            int i1 = -100;
            for (int j1 = 0; j1 < i - 1; j1++) if (j1 != k && j1 != k + 1 && (k != i - 2 || j1 != 0) && Geometry2D.left_side(skvertex2d, skvertex2d1, vertices[j1]) && Geometry2D.left_side(get_vertex(j1 - 1), get_vertex(j1), get_vertex(j1 + 1), skvertex2d) && Geometry2D.left_side(get_vertex(j1 - 1), get_vertex(j1), get_vertex(j1 + 1), skvertex2d1)) {
                double d3 = (new Vector2(vertices[j1], skvertex2d)).get_cos(new Vector2(vertices[j1], skvertex2d1));
                if (d3 < d) {
                    d = d3;
                    skvertex2d3 = vertices[j1];
                    i1 = j1;
                }
            }
            if (i1 == k + 2 || k == i - 2 && i1 == 1 || k == i - 3 && i1 == 0) create_T_triangle(triangles, edges, skvertex2d, skvertex2d1, skvertex2d3); else if (i1 != k - 1 && (k != 0 || i1 != i - 2)) create_S_triangle(triangles, edges, skvertex2d, skvertex2d1, skvertex2d3);
        }
        for (Enumeration enumeration1 = edges.elements(); enumeration1.hasMoreElements(); ) {
            SkEdge2D skedge2d = (SkEdge2D) enumeration1.nextElement();
            if (skedge2d.type == 1) create_J_triangle(triangles, edges, skedge2d);
        }
        for (Enumeration enumeration2 = triangles.elements(); enumeration2.hasMoreElements(); ) {
            SkPolygon2D skpolygon2d = (SkPolygon2D) enumeration2.nextElement();
            if (skpolygon2d.type == 1) {
                LinkedList linkedlist2 = skpolygon2d.get_internal_edges();
                double d1 = ((SkEdge2D) linkedlist2.head()).length();
                double d4 = ((SkEdge2D) linkedlist2.tail()).length();
                skpolygon2d.height = ((d1 + d4) / 4D) * Def.GENERATE_HEIGHT;
            } else if (skpolygon2d.type == 2) {
                SkEdge2D skedge2d1 = skpolygon2d.get_longest_edge();
                skpolygon2d.height = (skedge2d1.length() / 2D) * Def.GENERATE_HEIGHT;
            } else if (skpolygon2d.type == 0) {
                SkEdge2D skedge2d2 = skpolygon2d.get_internal_edge();
                LinkedList linkedlist3 = skpolygon2d.get_external_edges();
                SkEdge2D skedge2d4 = (SkEdge2D) linkedlist3.head();
                SkEdge2D skedge2d6 = (SkEdge2D) linkedlist3.tail();
                Vertex2D vertex2d2 = skedge2d2.mid_point();
                double d7 = skedge2d4.distance(vertex2d2);
                double d9 = skedge2d6.distance(vertex2d2);
                skpolygon2d.height = Math.min(d7, d9) * Def.GENERATE_HEIGHT;
            }
        }
        for (Enumeration enumeration3 = triangles.elements(); enumeration3.hasMoreElements(); ) {
            SkPolygon2D skpolygon2d1 = (SkPolygon2D) enumeration3.nextElement();
            for (Enumeration enumeration8 = skpolygon2d1.edges.elements(); enumeration8.hasMoreElements(); ((SkEdge2D) enumeration8.nextElement()).set_height()) ;
            if (skpolygon2d1.type == 2) if (skpolygon2d1.center_edge == null) skpolygon2d1.center.height = skpolygon2d1.height; else skpolygon2d1.center.height = skpolygon2d1.center_edge.height;
        }
        LinkedList linkedlist1 = new LinkedList();
        for (Enumeration enumeration4 = triangles.elements(); enumeration4.hasMoreElements(); ) {
            SkPolygon2D skpolygon2d2 = (SkPolygon2D) enumeration4.nextElement();
            if (skpolygon2d2.type == 0) propagate_terminal(skpolygon2d2, linkedlist1);
        }
        for (Enumeration enumeration5 = triangles.elements(); enumeration5.hasMoreElements(); ) {
            SkPolygon2D skpolygon2d3 = (SkPolygon2D) enumeration5.nextElement();
            if (skpolygon2d3.type == 2 && !skpolygon2d3.marked) propagate_junction_final(skpolygon2d3, linkedlist1);
        }
        cos_sin_init();
        h = new NewPolyhedron();
        double d2;
        Vertex2D vertex2d1;
        Vertex2D vertex2d3;
        Vertex2D vertex2d4;
        for (Enumeration enumeration6 = linkedlist1.elements(); enumeration6.hasMoreElements(); create_polygons_external(h, ((Vector2) (vertex2d3)).x, 0.0D, ((Vector2) (vertex2d3)).y, ((Vector2) (vertex2d4)).x, 0.0D, ((Vector2) (vertex2d4)).y, ((Vector2) (vertex2d1)).x, d2, ((Vector2) (vertex2d1)).y)) {
            SkPolygon2D skpolygon2d4 = (SkPolygon2D) enumeration6.nextElement();
            d2 = reverse_convert(skpolygon2d4.center.height);
            vertex2d1 = reverse_convert(skpolygon2d4.center);
            vertex2d3 = reverse_convert(skpolygon2d4.get_vertex(0));
            vertex2d4 = reverse_convert(skpolygon2d4.get_vertex(1));
        }
        for (Enumeration enumeration7 = triangles.elements(); enumeration7.hasMoreElements(); ) {
            SkPolygon2D skpolygon2d5 = (SkPolygon2D) enumeration7.nextElement();
            if (!skpolygon2d5.marked && skpolygon2d5.type == 1) {
                SkEdge2D skedge2d3 = (SkEdge2D) skpolygon2d5.get_internal_edges().head();
                SkEdge2D skedge2d5 = (SkEdge2D) skpolygon2d5.get_internal_edges().tail();
                double d5 = reverse_convert(skedge2d3.height);
                double d8 = reverse_convert(skedge2d5.height);
                Vertex2D vertex2d6 = reverse_convert(skpolygon2d5.get_vertex(0));
                Vertex2D vertex2d8 = reverse_convert(skpolygon2d5.get_vertex(1));
                Vertex2D vertex2d10 = reverse_convert(skpolygon2d5.get_vertex(2));
                Vertex2D vertex2d11 = reverse_convert(skedge2d3.mid_point());
                Vertex2D vertex2d12 = reverse_convert(skedge2d5.mid_point());
                create_polygons_square(h, ((Vector2) (vertex2d6)).x, 0.0D, ((Vector2) (vertex2d6)).y, ((Vector2) (vertex2d8)).x, 0.0D, ((Vector2) (vertex2d8)).y, ((Vector2) (vertex2d11)).x, d5, ((Vector2) (vertex2d11)).y, ((Vector2) (vertex2d12)).x, d8, ((Vector2) (vertex2d12)).y);
                create_polygons_internal(h, ((Vector2) (vertex2d12)).x, d8, ((Vector2) (vertex2d12)).y, ((Vector2) (vertex2d11)).x, d5, ((Vector2) (vertex2d11)).y, ((Vector2) (vertex2d10)).x, 0.0D, ((Vector2) (vertex2d10)).y);
            } else if (!skpolygon2d5.marked && skpolygon2d5.type == 2) {
                SkVertex2D skvertex2d2 = skpolygon2d5.center;
                Vertex2D vertex2d = reverse_convert(skvertex2d2);
                double d6 = reverse_convert(skvertex2d2.height);
                for (int l = 0; l < skpolygon2d5.edges.size(); l++) if (skpolygon2d5.trimData[l] == null && skpolygon2d5.get_edge(l) != skpolygon2d5.center_edge) {
                    Vertex2D vertex2d5 = reverse_convert(skpolygon2d5.get_vertex(l));
                    Vertex2D vertex2d7 = reverse_convert(skpolygon2d5.get_vertex(l + 1));
                    Vertex2D vertex2d9 = reverse_convert(skpolygon2d5.get_edge(l).mid_point());
                    double d10 = reverse_convert(skpolygon2d5.get_edge(l).height);
                    create_polygons_junction(h, ((Vector2) (vertex2d5)).x, 0.0D, ((Vector2) (vertex2d5)).y, ((Vector2) (vertex2d7)).x, 0.0D, ((Vector2) (vertex2d7)).y, ((Vector2) (vertex2d9)).x, d10, ((Vector2) (vertex2d9)).y, ((Vector2) (vertex2d)).x, d6, ((Vector2) (vertex2d)).y);
                }
            }
        }
        h.postprocess_main();
        Tessellation.remove_thin_polygons(h);
        ReTessellation.smooth5(h);
        h.set_parameters();
        return h;
    }

    public static SkEdge2D get_edge(LinkedList linkedlist, SkVertex2D skvertex2d, SkVertex2D skvertex2d1) {
        for (Enumeration enumeration = linkedlist.elements(); enumeration.hasMoreElements(); ) {
            SkEdge2D skedge2d = (SkEdge2D) enumeration.nextElement();
            if (skedge2d.equals(skvertex2d, skvertex2d1)) {
                skvertex2d.add_owner(skedge2d);
                skvertex2d1.add_owner(skedge2d);
                if (skedge2d.type == 2) System.out.println("edge shared by more than 2 polygons (SKeleton)");
                skedge2d.type = 2;
                return skedge2d;
            }
        }
        SkEdge2D skedge2d1 = new SkEdge2D(skvertex2d, skvertex2d1, 1);
        linkedlist.append(skedge2d1);
        return skedge2d1;
    }

    private static int get_origin(Vertex avertex[], Vertex vertex) {
        int i = 0;
        do {
            if (avertex[i] == vertex) return i / steps;
            i++;
        } while (true);
    }

    public static LinkedList get_skeleton_edges(LinkedList linkedlist) {
        return new LinkedList();
    }

    public static SkVertex2D get_vertex(int i) {
        if (i >= n_vertices) i -= (i / n_vertices) * n_vertices;
        if (i < 0) i += (-i / n_vertices + 1) * n_vertices;
        return vertices[i];
    }

    private static void propagate(SkPolygon2D skpolygon2d, SkEdge2D skedge2d, TrimData trimdata, LinkedList linkedlist) {
        SkPolygon2D skpolygon2d1 = skpolygon2d;
        SkEdge2D skedge2d1 = skedge2d;
        SkPolygon2D skpolygon2d2;
        do {
            skpolygon2d1.mark();
            skpolygon2d2 = skedge2d1.the_other_polygon(skpolygon2d1);
            if (skpolygon2d2.type != 1) break;
            SkEdge2D skedge2d2 = skpolygon2d2.the_other_internal_edge(skedge2d1);
            if (!trimdata.terminals_are_within_this_circle(skedge2d2)) break;
            trimdata.append_terminal_edge(skpolygon2d2.get_external_edge());
            skedge2d1 = skedge2d2;
            skpolygon2d1 = skpolygon2d2;
        } while (true);
        if (skpolygon2d2.type == 2) {
            SkVertex2D skvertex2d = skpolygon2d2.center;
            skpolygon2d2.setTrimData(skedge2d1, trimdata);
            propagate_junction(skpolygon2d2, linkedlist);
        } else {
            SkVertex2D skvertex2d1 = new SkVertex2D(skedge2d1.mid_point());
            skvertex2d1.height = skedge2d1.height;
            SkEdge2D skedge2d3;
            for (Enumeration enumeration = trimdata.terminal_edges.elements(); enumeration.hasMoreElements(); linkedlist.append(new SkPolygon2D(skedge2d3, skvertex2d1))) skedge2d3 = (SkEdge2D) enumeration.nextElement();
        }
    }

    private static void propagate_junction(SkPolygon2D skpolygon2d, LinkedList linkedlist) {
        SkEdge2D skedge2d = skpolygon2d.get_longest_edge();
        TrimData trimdata = new TrimData();
        boolean flag = false;
        boolean flag1 = false;
        for (int i = 0; i < skpolygon2d.edges.size(); i++) {
            SkEdge2D skedge2d1 = skpolygon2d.get_edge(i);
            if (skedge2d1 != skedge2d) {
                TrimData trimdata1 = skpolygon2d.getTrimData(i);
                if (trimdata1 != null) {
                    if (!trimdata1.terminals_are_within_this_circle(skedge2d1)) flag = true; else trimdata.merge(trimdata1);
                } else {
                    flag1 = true;
                }
            }
        }
        if (skpolygon2d.getTrimData(skedge2d) != null) flag = true;
        if (flag1) return;
        if (flag) {
            return;
        } else {
            propagate(skpolygon2d, skedge2d, trimdata, linkedlist);
            return;
        }
    }

    private static void propagate_junction_final(SkPolygon2D skpolygon2d, LinkedList linkedlist) {
        LinkedList linkedlist1 = new LinkedList();
        for (int i = 0; i < skpolygon2d.edges.size(); i++) {
            TrimData trimdata = skpolygon2d.getTrimData(i);
            if (trimdata != null) linkedlist1.connect(trimdata.terminal_edges);
        }
        SkVertex2D skvertex2d = skpolygon2d.center;
        SkEdge2D skedge2d;
        for (Enumeration enumeration = linkedlist1.elements(); enumeration.hasMoreElements(); linkedlist.append(new SkPolygon2D(skedge2d, skvertex2d))) skedge2d = (SkEdge2D) enumeration.nextElement();
    }

    private static void propagate_terminal(SkPolygon2D skpolygon2d, LinkedList linkedlist) {
        SkVertex2D skvertex2d = skpolygon2d.get_terminal_vertex();
        LinkedList linkedlist1 = skpolygon2d.get_external_edges();
        SkEdge2D skedge2d = skpolygon2d.get_internal_edge();
        TrimData trimdata = new TrimData(skvertex2d, linkedlist1);
        propagate(skpolygon2d, skedge2d, trimdata, linkedlist);
    }

    public static void remove_S_triangle(LinkedList linkedlist, LinkedList linkedlist1, SkPolygon2D skpolygon2d) {
        SkEdge2D skedge2d = skpolygon2d.get_external_edge();
        remove_edge(linkedlist1, skedge2d);
        LinkedList linkedlist2 = skpolygon2d.get_internal_edges();
        ((SkEdge2D) linkedlist2.head()).type = 1;
        ((SkEdge2D) linkedlist2.tail()).type = 1;
    }

    public static void remove_edge(LinkedList linkedlist, SkEdge2D skedge2d) {
        ((SkVertex2D) ((Edge2D) (skedge2d)).start).remove_owner(skedge2d);
        ((SkVertex2D) ((Edge2D) (skedge2d)).end).remove_owner(skedge2d);
        linkedlist.remove(skedge2d);
    }

    public static double reverse_convert(double d) {
        return Draw3DScene.reverse_convertZ(d);
    }

    public static Vertex2D reverse_convert(Vertex2D vertex2d) {
        return new Vertex2D(Draw3DScene.reverse_convertX(((Vector2) (vertex2d)).x), Draw3DScene.reverse_convertY(((Vector2) (vertex2d)).y));
    }

    public static LinkedList triangles;

    public static LinkedList edges;

    public static SkVertex2D vertices[];

    public static int n_vertices;

    public static NewPolyhedron h;

    private static int steps;

    private static double cos[];

    private static double sin[];

    static {
        steps = 4;
        cos = new double[steps * 2 + 1];
        sin = new double[steps * 2 + 1];
    }
}
