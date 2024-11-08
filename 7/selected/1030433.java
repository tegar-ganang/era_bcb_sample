package jenes.problems.menulayout.ga;

import jenes.problems.menulayout.common.MenuNode;
import java.util.ArrayList;
import java.util.List;
import jenes.Random;
import jenes.chromosome.Chromosome;
import jenes.problems.menulayout.MenuProblem.Menu;
import jenes.problems.menulayout.MenuProblem.MenuItem;
import jenes.problems.menulayout.common.MenuObject;
import jenes.problems.menulayout.MenuProblem.MenuRoot;

public class MenuChromosome implements Chromosome<MenuChromosome> {

    public static class Path implements Cloneable {

        private List<MenuObject> path;

        public Path() {
            this.path = null;
        }

        public Path(List<MenuObject> path) {
            this.path = new ArrayList<MenuObject>(path);
        }

        public Path(MenuObject... items) {
            this.path = new ArrayList<MenuObject>();
            for (MenuObject i : items) {
                this.path.add(i);
            }
        }

        public Path(Path p) {
            if (p.path != null) {
                this.path = new ArrayList<MenuObject>(p.path);
            }
        }

        public List<MenuObject> getPath() {
            return path;
        }

        public boolean isNull() {
            return this.path == null;
        }

        public boolean isEmpty() {
            return this.path != null && this.path.size() == 0;
        }

        public int length() {
            return path == null ? -1 : path.size();
        }

        public String toString() {
            String p = "";
            if (path != null) {
                for (MenuObject i : path) {
                    p += i.toString() + ":";
                }
            }
            return p;
        }

        public Path clone() {
            Path p = new Path();
            if (this.path != null) {
                p.path = new ArrayList<MenuObject>();
                for (MenuObject i : this.path) {
                    p.path.add(i);
                }
            }
            return p;
        }

        public boolean equals(Path p) {
            int s0 = this.path.size();
            int s1 = p.path.size();
            if (s0 != s1) {
                return false;
            }
            for (int i = 0; i < s0; ++i) {
                if (this.path.get(i) != p.path.get(i)) {
                    return false;
                }
            }
            return true;
        }

        public void randomize() {
            Random rand = Random.getInstance();
            int op = rand.nextInt(3);
            switch(op) {
                case 0:
                    if (this.length() > 0) {
                        int p = rand.nextInt(this.path.size());
                        this.path.remove(p);
                    }
                    break;
                case 1:
                    if (this.length() > 0) {
                        Menu[] menus = Menu.values();
                        int p = rand.nextInt(this.path.size());
                        int i = rand.nextInt(menus.length);
                        Menu it = menus[i];
                        this.path.set(p, it);
                    }
                    break;
                case 2:
                    if (this.length() > -1) {
                        Menu[] menus = Menu.values();
                        int p = rand.nextInt(this.path.size() + 1);
                        int i = rand.nextInt(menus.length);
                        Menu it = menus[i];
                        this.path.add(p, it);
                    }
                    break;
            }
        }

        public static Path randomPath(int maxsize, boolean repeat) {
            Path p = new Path();
            Menu[] menus = Menu.values();
            Random rand = Random.getInstance();
            int ms = menus.length;
            int k = rand.nextInt(maxsize + 1) - 1;
            if (k > -1) {
                p.path = new ArrayList<MenuObject>();
                if (repeat) {
                    for (int i = 0; i < k; ++i) {
                        int h = rand.nextInt(ms);
                        Menu it = menus[h];
                        p.path.add(it);
                    }
                } else {
                    for (int i = 0; i < k; ++i) {
                        int h = rand.nextInt(ms - i);
                        Menu it = menus[h];
                        p.path.add(it);
                        menus[h] = menus[ms - 1 - i];
                        menus[ms - 1 - i] = it;
                    }
                }
            }
            return p;
        }
    }

    public static final Path NULL_PATH = new Path();

    public static final Path ROOT_PATH = new Path(new ArrayList<MenuObject>());

    public static MenuItem[] MAPPING;

    public Path[] genes;

    public MenuNode structure;

    public MenuChromosome() {
        genes = new Path[MAPPING.length];
    }

    public MenuChromosome(MenuChromosome chromosome) {
        genes = new Path[chromosome.genes.length];
        this.setAs(chromosome);
    }

    public void cross(MenuChromosome chromosome, int from, int to) {
        structure = null;
        final int end = to + 1;
        int minlen = this.genes.length;
        if (minlen < chromosome.genes.length) {
            minlen = chromosome.genes.length;
        }
        if (end > minlen) {
            this.cross(chromosome, from);
        } else {
            for (int i = from; i < end; ++i) {
                Path g = this.genes[i];
                this.genes[i] = chromosome.genes[i].clone();
                chromosome.genes[i] = g.clone();
            }
        }
    }

    public void cross(MenuChromosome chromosome, int from) {
        this.cross(chromosome, from, MAPPING.length - 1);
    }

    public boolean equals(MenuChromosome chromosome) {
        int l = this.genes.length;
        for (int i = 0; i < l; ++i) {
            if (this.genes[i].equals(chromosome.genes[i])) {
                return false;
            }
        }
        return true;
    }

    public void leftShift(int from, int to) {
        structure = null;
        Path first = genes[from];
        for (int i = from; i < to; ++i) {
            genes[i] = genes[i + 1];
        }
        genes[to] = first;
    }

    public void rightShift(int from, int to) {
        structure = null;
        Path last = genes[to];
        for (int i = to; i > from; --i) {
            genes[i] = genes[i - 1];
        }
        genes[from] = last;
    }

    public int length() {
        return MAPPING.length;
    }

    public void move(int[] offset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void randomize(int pos) {
        structure = null;
        this.genes[pos].randomize();
    }

    public void randomize() {
        int pos = Random.getInstance().nextInt(MAPPING.length);
        this.randomize(pos);
    }

    public void setAs(MenuChromosome chromosome) {
        this.structure = chromosome.structure;
        int i = 0;
        for (Path p : chromosome.genes) {
            genes[i++] = p.clone();
        }
    }

    public void setDefaultValueAt(int pos) {
        structure = null;
        this.genes[pos] = ROOT_PATH.clone();
    }

    public void swap(int pos1, int pos2) {
        structure = null;
        Path p = this.genes[pos1];
        this.genes[pos1] = this.genes[pos2];
        this.genes[pos2] = p;
    }

    public List<Path> getGenes() {
        List<Path> listpath = new ArrayList<Path>();
        for (int i = 0; i < MAPPING.length; ++i) {
            listpath.add(this.genes[i]);
        }
        return listpath;
    }

    public MenuNode getStructure() {
        if (structure == null) {
            structure = new MenuNode(MenuRoot.R);
            int i = 0;
            for (MenuChromosome.Path p : genes) {
                if (!p.isNull()) {
                    fillStructure(MenuChromosome.MAPPING[i++], p, 0, structure);
                }
            }
        }
        return structure;
    }

    private void fillStructure(MenuItem menuitem, MenuChromosome.Path path, int level, MenuNode node) {
        if (level == path.length()) {
            MenuNode child = node.getChild(menuitem);
            if (child == null) {
                child = new MenuNode(menuitem);
                node.add(child);
            }
        } else {
            MenuObject it = path.getPath().get(level);
            MenuNode child = node.getChild(it);
            if (child == null) {
                child = new MenuNode(it);
                node.add(child);
            }
            fillStructure(menuitem, path, ++level, child);
        }
    }

    public MenuChromosome clone() {
        return new MenuChromosome(this);
    }

    public static MenuChromosome randomGenerate(int depth, boolean repeat) {
        MenuChromosome c = new MenuChromosome();
        for (int i = 0; i < MAPPING.length; ++i) {
            c.genes[i] = Path.randomPath(depth, repeat);
        }
        return c;
    }
}
