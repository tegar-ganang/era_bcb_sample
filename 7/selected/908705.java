package com.dukesoftware.utils.data;

public class BTree {

    private static final int M = 2;

    private int insertKey;

    private Page insertPage;

    private boolean undersize;

    private class Page {

        int n = 0;

        int[] key = new int[2 * M];

        Page[] branch = new Page[2 * M + 1];

        private boolean searchNode(int searchKey) {
            int i = 0;
            while (i < n && key[i] < searchKey) i++;
            if (i < n && key[i] == searchKey) return true;
            if (branch[i] == null) return false;
            return branch[i].searchNode(searchKey);
        }

        private void insertItem(int i, int newKey, Page newPage) {
            for (int j = n; j > i; j--) {
                branch[j + 1] = branch[j];
                key[j] = key[j - 1];
            }
            key[i] = newKey;
            branch[i + 1] = newPage;
            n++;
        }

        private void split(int i) {
            final int m;
            if (i <= M) m = M; else m = M + 1;
            Page q = new Page();
            for (int j = m + 1; j <= 2 * M; j++) {
                q.key[j - m - 1] = key[j - 1];
                q.branch[j - m] = branch[j];
            }
            q.n = 2 * M - m;
            n = m;
            if (i <= M) insertItem(i, insertKey, insertPage); else q.insertItem(i - m, insertKey, insertPage);
            insertKey = key[n - 1];
            q.branch[0] = branch[n];
            n--;
            insertPage = q;
        }

        private boolean insertNode() {
            int i = 0;
            while (i < n && key[i] < insertKey) i++;
            if (i < n && key[i] == insertKey) {
                message = "Arghhh";
                return true;
            }
            if (branch[i] != null && branch[i].insertNode()) return true;
            if (n < 2 * M) {
                insertItem(i, insertKey, insertPage);
                return true;
            } else {
                split(i);
                return false;
            }
        }

        private void deleteItem(int i) {
            while (++i < n) {
                key[i - 1] = key[i];
                branch[i] = branch[i + 1];
            }
            branch[n] = null;
            undersize = (--n < M);
        }

        private void moveRight(int i) {
            Page left = branch[i - 1], right = branch[i];
            right.insertItem(0, key[i - 1], right.branch[0]);
            key[i - 1] = left.key[left.n - 1];
            right.branch[0] = left.branch[left.n];
            left.n--;
        }

        private void moveLeft(int i) {
            Page left = branch[i - 1], right = branch[i];
            left.insertItem(left.n, key[i - 1], right.branch[0]);
            key[i - 1] = right.key[0];
            right.branch[0] = right.branch[1];
            right.deleteItem(0);
        }

        private void combine(int i) {
            Page left = branch[i - 1], right = branch[i];
            left.insertItem(left.n, key[i - 1], right.branch[0]);
            for (int j = 1; j <= right.n; j++) left.insertItem(left.n, right.key[j - 1], right.branch[j]);
            deleteItem(i - 1);
        }

        private void restore(int i) {
            undersize = false;
            if (i > 0) {
                if (branch[i - 1].n > M) moveRight(i); else combine(i);
            } else {
                if (branch[1].n > M) moveLeft(1); else combine(1);
            }
        }

        private boolean deleteNode(int deleteKey) {
            int i = 0;
            boolean deleted = false;
            while (i < n && key[i] < deleteKey) i++;
            if (i < n && key[i] == deleteKey) {
                deleted = true;
                Page q = branch[i + 1];
                if (q != null) {
                    while (q.branch[0] != null) q = q.branch[0];
                    key[i] = deleteKey = q.key[0];
                    branch[i + 1].deleteNode(deleteKey);
                    if (undersize) restore(i + 1);
                } else deleteItem(i);
            } else {
                if (branch[i] != null) {
                    deleted = branch[i].deleteNode(deleteKey);
                    if (undersize) restore(i);
                }
            }
            return deleted;
        }

        private void print() {
            System.out.print("(");
            for (int i = 0; i <= n; i++) {
                if (branch[i] == null) System.out.print("."); else branch[i].print();
                if (i < n) System.out.print(key[i]);
            }
            System.out.print(")");
        }
    }

    private Page root = new Page();

    static String message = "";

    public void searchNode(int key) {
        if (root.searchNode(key)) message = "A"; else message = "B";
    }

    public void insertNode(int key) {
        message = "";
        insertKey = key;
        insertPage = null;
        if (root != null && root.insertNode()) return;
        Page p = new Page();
        p.branch[0] = root;
        root = p;
        p.insertItem(0, insertKey, insertPage);
    }

    public void deleteNode(int key) {
        undersize = false;
        if (root.deleteNode(key)) {
            if (root.n == 0) {
                root = root.branch[0];
            }
            message = "";
        } else message = "C";
    }
}
