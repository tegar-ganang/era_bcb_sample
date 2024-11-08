package org.ode4j.ode.internal;

import org.cpp4j.java.Ref;
import org.ode4j.ode.internal.joints.OdeJointsFactoryImpl;
import org.ode4j.ode.internal.joints.DxJoint;
import org.ode4j.ode.internal.joints.DxJointNode;
import static org.cpp4j.Cstdio.*;
import static org.ode4j.ode.OdeMath.*;

/**
 * this source file is mostly concerned with the data structures, not the
 * numerics.
 */
public class OdeFactoryImpl extends OdeJointsFactoryImpl {

    static <T extends DObject> boolean listHasLoops(Ref<T> first) {
        if (first.get() == null || first.get().getNext() == null) return false;
        DObject a = first.get(), b = first.get().getNext();
        int skip = 0;
        while (b != null) {
            if (a == b) return true;
            b = b.getNext();
            if (skip != 0) a = a.getNext();
            skip ^= 1;
        }
        return false;
    }

    static int g_world_check_tag_generator = 0;

    static int generateWorldCheckTag() {
        return ++g_world_check_tag_generator;
    }

    static void checkWorld(DxWorld w) {
        DxBody b;
        DxJoint j;
        if (listHasLoops(w.firstbody)) dDebug(0, "body list has loops");
        if (listHasLoops(w.firstjoint)) dDebug(0, "joint list has loops");
        if (true) throw new UnsupportedOperationException();
        int nn = 0;
        for (b = w.firstbody.get(); b != null; b = (DxBody) b.getNext()) nn++;
        if (w.nb != nn) dDebug(0, "body count incorrect");
        nn = 0;
        for (j = w.firstjoint.get(); j != null; j = (DxJoint) j.getNext()) nn++;
        if (w.nj != nn) dDebug(0, "joint count incorrect");
        int count = generateWorldCheckTag();
        for (b = w.firstbody.get(); b != null; b = (DxBody) b.getNext()) b.tag = count;
        for (j = w.firstjoint.get(); j != null; j = (DxJoint) j.getNext()) j.tag = count;
        for (b = w.firstbody.get(); b != null; b = (DxBody) b.getNext()) if (b.world != w) dDebug(0, "bad world pointer in body list");
        for (j = w.firstjoint.get(); j != null; j = (DxJoint) j.getNext()) if (j.world != w) dDebug(0, "bad world pointer in joint list");
        for (j = w.firstjoint.get(); j != null; j = (DxJoint) j.getNext()) {
            for (int i = 0; i < 2; i++) {
                if (j.node[i].body != null) {
                    int ok = 0;
                    for (DxJointNode n = j.node[i].body.firstjoint.get(); n != null; n = n.next) {
                        if (n.joint == j) ok = 1;
                    }
                    if (ok == 0) dDebug(0, "joint not in joint list of attached body");
                }
            }
        }
        for (b = w.firstbody.get(); b != null; b = (DxBody) b.getNext()) {
            for (DxJointNode n = b.firstjoint.get(); n != null; n = n.next) {
                if (n.joint.node[0] == n) {
                    if (n.joint.node[1].body != b) dDebug(0, "bad body pointer in joint node of body list (1)");
                } else {
                    if (n.joint.node[0].body != b) dDebug(0, "bad body pointer in joint node of body list (2)");
                }
                if (n.joint.tag != count) dDebug(0, "bad joint node pointer in body");
            }
        }
        for (j = w.firstjoint.get(); j != null; j = (DxJoint) j.getNext()) {
            if (j.node[0].body != null && (j.node[0].body == j.node[1].body)) dDebug(0, "non-distinct body pointers in joint");
            if ((j.node[0].body != null && j.node[0].body.tag != count) || (j.node[1].body != null && j.node[1].body.tag != count)) dDebug(0, "bad body pointer in joint");
        }
    }

    void dWorldCheck(DxWorld w) {
        checkWorld(w);
    }

    private static final int NUM = 100;

    public void dTestDataStructures() {
        int i;
        printf("testDynamicsStuff()\n");
        DxBody[] body = new DxBody[NUM];
        int nb = 0;
        DxJoint[] joint = new DxJoint[NUM];
        int nj = 0;
        for (i = 0; i < NUM; i++) body[i] = null;
        for (i = 0; i < NUM; i++) joint[i] = null;
        printf("creating world\n");
        DxWorld w = DxWorld.dWorldCreate();
        checkWorld(w);
        for (; ; ) {
            if (nb < NUM && dRandReal() > 0.5) {
                printf("creating body\n");
                body[nb] = DxBody.dBodyCreate(w);
                printf("\t--> %p\n", body[nb].toString());
                nb++;
                checkWorld(w);
                printf("%d BODIES, %d JOINTS\n", nb, nj);
            }
            if (nj < NUM && nb > 2 && dRandReal() > 0.5) {
                DxBody b1 = (DxBody) body[(int) (dRand() % nb)];
                DxBody b2 = (DxBody) body[(int) (dRand() % nb)];
                if (b1 != b2) {
                    printf("creating joint, attaching to %p,%p\n", b1, b2);
                    joint[nj] = dJointCreateBall(w, null);
                    printf("\t-->%p\n", joint[nj]);
                    checkWorld(w);
                    joint[nj].dJointAttach(b1, b2);
                    nj++;
                    checkWorld(w);
                    printf("%d BODIES, %d JOINTS\n", nb, nj);
                }
            }
            if (nj > 0 && nb > 2 && dRandReal() > 0.5) {
                DxBody b1 = body[(int) (dRand() % nb)];
                DxBody b2 = body[(int) (dRand() % nb)];
                if (b1 != b2) {
                    int k = (int) (dRand() % nj);
                    printf("reattaching joint %p\n", joint[k]);
                    joint[k].dJointAttach(b1, b2);
                    checkWorld(w);
                    printf("%d BODIES, %d JOINTS\n", nb, nj);
                }
            }
            if (nb > 0 && dRandReal() > 0.5) {
                int k = (int) (dRand() % nb);
                printf("destroying body %p\n", body[k]);
                body[k].dBodyDestroy();
                checkWorld(w);
                for (; k < (NUM - 1); k++) body[k] = body[k + 1];
                nb--;
                printf("%d BODIES, %d JOINTS\n", nb, nj);
            }
            if (nj > 0 && dRandReal() > 0.5) {
                int k = (int) (dRand() % nj);
                printf("destroying joint %p\n", joint[k]);
                dJointDestroy(joint[k]);
                checkWorld(w);
                for (; k < (NUM - 1); k++) joint[k] = joint[k + 1];
                nj--;
                printf("%d BODIES, %d JOINTS\n", nb, nj);
            }
        }
    }

    private static void REGISTER_EXTENSION(String s) {
        ode_configuration += s + " ";
    }

    static String ode_configuration = "ODE ";

    static {
        if (dNODEBUG) REGISTER_EXTENSION("ODE_EXT_no_debug");
        if (dUSE_MALLOC_FOR_ALLOCA) REGISTER_EXTENSION("ODE_EXT_malloc_not_alloca");
        if (dTRIMESH_ENABLED) {
            REGISTER_EXTENSION("ODE_EXT_trimesh");
            if (dTRIMESH_OPCODE) {
                REGISTER_EXTENSION("ODE_EXT_opcode");
                if (dTRIMESH_16BIT_INDICES) REGISTER_EXTENSION("ODE_OPC_16bit_indices");
                if (!dTRIMESH_OPCODE_USE_OLD_TRIMESH_TRIMESH_COLLIDER) REGISTER_EXTENSION("ODE_OPC_new_collider");
            }
            if (dTRIMESH_GIMPACT) {
                REGISTER_EXTENSION("ODE_EXT_gimpact");
            }
        }
        if (dTLS_ENABLED) {
            REGISTER_EXTENSION("ODE_EXT_mt_collisions");
        }
        ode_configuration += "ODE_double_precision";
    }

    public String _dGetConfiguration() {
        return ode_configuration;
    }

    public boolean _dCheckConfiguration(final String extension) {
        int start;
        int where;
        int terminator;
        if (extension.indexOf(' ') >= 0 || extension.length() == 0) return true;
        final String config = getConfiguration();
        final int ext_length = extension.length();
        start = 0;
        for (; ; ) {
            where = config.indexOf(extension, start);
            if (where == -1) break;
            terminator = where + ext_length;
            if ((where == start || extension.charAt(where - 1) == ' ') && (extension.charAt(terminator) == ' ' || terminator == extension.length())) {
                return true;
            }
            start = terminator;
        }
        return false;
    }
}
