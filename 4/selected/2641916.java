package org.nees.calculate.expression;

public class ChannelListVisitor implements ExpressionVisitor {

    private boolean[] list = new boolean[0];

    public boolean[] getChannelList() {
        return list;
    }

    private void setChannel(int index) {
        if (index >= list.length) {
            boolean[] more = new boolean[index + 1];
            for (int i = 0; i < list.length; i++) more[i] = list[i];
            for (int i = list.length; i < more.length; i++) more[i] = false;
            list = more;
        }
        list[index] = true;
    }

    private Object process(SimpleNode node, Object data) {
        return node.childrenAccept(this, data);
    }

    public Object visit(SimpleNode node, Object data) {
        return process(node, data);
    }

    public Object visit(MyStart node, Object data) {
        return process(node, data);
    }

    public Object visit(MyAdd node, Object data) {
        return process(node, data);
    }

    public Object visit(MySub node, Object data) {
        return process(node, data);
    }

    public Object visit(MyMul node, Object data) {
        return process(node, data);
    }

    public Object visit(MyDiv node, Object data) {
        return process(node, data);
    }

    public Object visit(MyMinus node, Object data) {
        return process(node, data);
    }

    public Object visit(MyFunction0 node, Object data) {
        return process(node, data);
    }

    public Object visit(MyFunction1 node, Object data) {
        return process(node, data);
    }

    public Object visit(MyFunction2 node, Object data) {
        return process(node, data);
    }

    public Object visit(MySpecial node, Object data) {
        return data;
    }

    public Object visit(MyLit node, Object data) {
        return data;
    }

    public Object visit(MyChannel node, Object data) {
        String index_st = ((String) node.val).substring(1);
        int index = Integer.parseInt(index_st);
        setChannel(index);
        return data;
    }

    public Object visit(MyConditional node, Object data) {
        return process(node, data);
    }

    public Object visit(MyLogicalValue node, Object data) {
        return data;
    }

    public Object visit(MyOr node, Object data) {
        return process(node, data);
    }

    public Object visit(MyAnd node, Object data) {
        return process(node, data);
    }

    public Object visit(MyNegation node, Object data) {
        return process(node, data);
    }

    public Object visit(MyLT node, Object data) {
        return process(node, data);
    }

    public Object visit(MyLE node, Object data) {
        return process(node, data);
    }

    public Object visit(MyGT node, Object data) {
        return process(node, data);
    }

    public Object visit(MyGE node, Object data) {
        return process(node, data);
    }

    public Object visit(MyEQ node, Object data) {
        return process(node, data);
    }

    public Object visit(MyNE node, Object data) {
        return process(node, data);
    }
}
