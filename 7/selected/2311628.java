package ExamplesJaCoP;

import java.util.ArrayList;
import JaCoP.constraints.SumWeight;
import JaCoP.constraints.XlteqY;
import JaCoP.core.IntVar;
import JaCoP.core.Var;
import JaCoP.core.IntervalDomain;
import JaCoP.core.Store;
import JaCoP.search.DepthFirstSearch;
import JaCoP.search.Search;
import JaCoP.search.SelectChoicePoint;
import JaCoP.search.SimpleSelect;
import JaCoP.set.constraints.AdisjointB;
import JaCoP.set.constraints.AeqS;
import JaCoP.set.constraints.AintersectBeqC;
import JaCoP.set.constraints.AunionBeqC;
import JaCoP.set.constraints.CardA;
import JaCoP.set.constraints.Match;
import JaCoP.set.core.BoundSetDomain;
import JaCoP.set.core.SetVar;
import JaCoP.set.search.IndomainSetMin;
import JaCoP.set.search.MaxGlbCard;
import JaCoP.set.search.MinLubCard;

/**
 * It is a Social Golfer example based on set variables.
 *
 * @author Krzysztof Kuchcinski
 * @version 3.0
 */
public class SocialGolfer extends Example {

    int weeks = 3;

    int groups = 2;

    int players = 2;

    SetVar[][] golferGroup;

    /**
	 * 
	 * It runs a number of social golfer problems.
	 * 
	 * @param args
	 */
    public static void main(String args[]) {
        SocialGolfer example = new SocialGolfer();
        example.setup(3, 2, 2);
        example.model();
        example.search();
        example.setup(2, 5, 4);
        example.model();
        example.search();
        example.setup(2, 6, 4);
        example.model();
        example.search();
        example.setup(2, 7, 4);
        example.model();
        example.search();
        example.setup(3, 5, 4);
        example.model();
        example.search();
        example.setup(3, 6, 4);
        example.model();
        example.search();
        example.setup(3, 7, 4);
        example.model();
        example.search();
        example.setup(4, 5, 4);
        example.model();
        example.search();
        example.setup(4, 6, 5);
        example.model();
        example.search();
        example.setup(4, 7, 4);
        example.model();
        example.search();
        example.setup(4, 9, 4);
        example.model();
        example.search();
        example.setup(5, 5, 3);
        example.model();
        example.search();
        example.setup(5, 7, 4);
        example.model();
        example.search();
        example.setup(5, 8, 3);
        example.model();
        example.search();
        example.setup(6, 6, 3);
        example.model();
        example.search();
        example.setup(5, 3, 2);
        example.model();
        example.search();
        example.setup(4, 3, 3);
        example.model();
        example.search();
    }

    /**
	 * It sets the parameters for the model creation function. 
	 * 
	 * @param weeks
	 * @param groups
	 * @param players
	 */
    public void setup(int weeks, int groups, int players) {
        this.weeks = weeks;
        this.groups = groups;
        this.players = players;
    }

    public void model() {
        int N = groups * players;
        int[] weights = new int[players];
        int base = Math.max(10, players + 1);
        weights[players - 1] = 1;
        for (int i = players - 2; i >= 0; i--) weights[i] = weights[i + 1] * base;
        System.out.println("Social golfer problem " + weeks + "-" + groups + "-" + players);
        store = new Store();
        golferGroup = new SetVar[weeks][groups];
        vars = new ArrayList<Var>();
        for (int i = 0; i < weeks; i++) for (int j = 0; j < groups; j++) {
            golferGroup[i][j] = new SetVar(store, "g_" + i + "_" + j, new BoundSetDomain(1, N));
            vars.add(golferGroup[i][j]);
            store.impose(new CardA(golferGroup[i][j], players));
        }
        for (int i = 0; i < weeks; i++) for (int j = 0; j < groups; j++) for (int k = j + 1; k < groups; k++) {
            store.impose(new AdisjointB(golferGroup[i][j], golferGroup[i][k]));
        }
        for (int i = 0; i < weeks; i++) {
            SetVar t = golferGroup[i][0];
            for (int j = 1; j < groups; j++) {
                SetVar r = new SetVar(store, "r-" + i + "-" + j, new BoundSetDomain(1, N));
                store.impose(new AunionBeqC(t, golferGroup[i][j], r));
                t = r;
            }
            store.impose(new AeqS(t, new IntervalDomain(1, N)));
        }
        for (int i = 0; i < weeks; i++) for (int j = i + 1; j < weeks; j++) if (i != j) for (int k = 0; k < groups; k++) for (int l = 0; l < groups; l++) {
            SetVar result = new SetVar(store, "res" + i + "-" + j + "-" + k + "-" + l, new BoundSetDomain(1, N));
            store.impose(new AintersectBeqC(golferGroup[i][k], golferGroup[j][l], result));
            store.impose(new CardA(result, 0, 1));
        }
        IntVar[] v = new IntVar[weeks];
        IntVar[][] var = new IntVar[weeks][players];
        for (int i = 0; i < weeks; i++) {
            v[i] = new IntVar(store, "v" + i, 0, 100000000);
            for (int j = 0; j < players; j++) var[i][j] = new IntVar(store, "var" + i + "-" + j, 1, N);
            store.impose(new Match(golferGroup[i][0], var[i]));
            store.impose(new SumWeight(var[i], weights, v[i]));
        }
        for (int i = 0; i < weeks - 1; i++) store.impose(new XlteqY(v[i], v[i + 1]));
    }

    public boolean search() {
        Thread tread = java.lang.Thread.currentThread();
        java.lang.management.ThreadMXBean b = java.lang.management.ManagementFactory.getThreadMXBean();
        long startCPU = b.getThreadCpuTime(tread.getId());
        long startUser = b.getThreadUserTime(tread.getId());
        boolean result = store.consistency();
        System.out.println("*** consistency = " + result);
        Search label = new DepthFirstSearch<SetVar>();
        SelectChoicePoint<SetVar> select = new SimpleSelect<SetVar>(vars.toArray(new SetVar[vars.size()]), new MinLubCard<SetVar>(), new MaxGlbCard<SetVar>(), new IndomainSetMin<SetVar>());
        label.getSolutionListener().searchAll(false);
        label.getSolutionListener().recordSolutions(false);
        result = label.labeling(store, select);
        if (result) {
            System.out.println("*** Yes");
            for (int i = 0; i < weeks; i++) {
                for (int j = 0; j < groups; j++) {
                    System.out.print(golferGroup[i][j].dom() + " ");
                }
                System.out.println();
            }
        } else System.out.println("*** No");
        System.out.println("ThreadCpuTime = " + (b.getThreadCpuTime(tread.getId()) - startCPU) / (long) 1e+6 + "ms");
        System.out.println("ThreadUserTime = " + (b.getThreadUserTime(tread.getId()) - startUser) / (long) 1e+6 + "ms");
        return result;
    }
}
