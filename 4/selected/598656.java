package skribler.ast;

import org.antlr.runtime.CommonToken;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

public class TreeNodeSerializer {

    private TreeNode tree;

    private static final StringTemplateGroup TEMPLATES = new StringTemplateGroup("ossa");

    public TreeNodeSerializer(TreeNode tree) {
        assert (tree != null);
        this.tree = tree;
    }

    public String serialize() {
        StringTemplate template = TEMPLATES.getInstanceOf("skribler/ast/ast");
        template.setAttribute("content", visit(tree));
        return template.toString();
    }

    private String visit(CommonToken token) {
        if (token == null) {
            return "<nil />";
        }
        StringTemplate template = TEMPLATES.getInstanceOf("skribler/ast/token");
        template.setAttribute("type", token.getType());
        template.setAttribute("line", token.getLine());
        template.setAttribute("charPositionInLine", token.getCharPositionInLine());
        template.setAttribute("channel", token.getChannel());
        template.setAttribute("index", token.getTokenIndex());
        template.setAttribute("start", token.getStartIndex());
        template.setAttribute("stop", token.getStopIndex());
        template.setAttribute("text", token.getText());
        return template.toString();
    }

    private String visit(TreeNode tree) {
        StringTemplate template = TEMPLATES.getInstanceOf("skribler/ast/tree");
        final int count = tree.getChildCount();
        template.setAttribute("tokenStart", tree.getTokenStartIndex());
        template.setAttribute("tokenStop", tree.getTokenStopIndex());
        template.setAttribute("childIndex", tree.getChildIndex());
        template.setAttribute("token", visit((CommonToken) tree.getToken()));
        for (int i = 0; i < count; i++) {
            final TreeNode child = (TreeNode) tree.getChild(i);
            template.setAttribute("child", visit(child));
        }
        return template.toString();
    }
}
