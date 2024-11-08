package org.scribble.parser;

import org.scribble.extensions.*;
import org.scribble.model.Channel;
import org.scribble.model.ChannelList;
import org.scribble.model.Role;
import org.scribble.model.admin.ModelIssue;
import org.scribble.model.admin.ModelListener;

/**
 * This class provides the parser rule for the Channel List
 * declaration.
 */
@RegistryInfo(extension = ParserRule.class)
public class ChannelListParserRule extends org.scribble.parser.AbstractParserRule {

    /**
	 * The default constructor.
	 */
    public ChannelListParserRule() {
        super(ParserRuleType.Line);
    }

    /**
	 * This method determines whether the parser rules is
	 * appropriate to return a model object of the required
	 * type.
	 * 
	 * @param type The required type
	 * @return Whether the parser rule returns a
	 * 				model object of the required type
	 */
    public boolean isSupported(Class<?> type) {
        return (type.isAssignableFrom(ChannelList.class));
    }

    /**
	 * This method parses the description to obtain
	 * a model object of the appropriate type.
	 * 
	 * @param context The context
	 * @param l The listener
	 * @return The model object
	 */
    public Object parse(ParserContext context, ModelListener l) {
        ChannelList ret = null;
        Token t = context.lookahead(0);
        if (t.isToken("channel", TokenType.Keyword)) {
            boolean f_error = false;
            ret = new ChannelList();
            do {
                context.nextToken();
                t = context.lookahead(0);
                if (t.getType() == TokenType.Identifier) {
                    context.nextToken();
                    Channel ch = new Channel();
                    ch.getSource().setStartPosition(t.getSource().getStartPosition());
                    ch.setName(t.getText());
                    ch.getSource().setEndPosition(t.getSource().getEndPosition());
                    ret.getChannels().add(ch);
                    if (context.getState(t.getText()) != null) {
                        l.error(new ModelIssue(t, org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_DUPLICATE_DECLARATION", new String[] { t.getText() })));
                    } else {
                        context.setState(ch.getName(), ch);
                    }
                    t = context.lookahead(0);
                    if (t.isToken("from", TokenType.Keyword)) {
                        context.nextToken();
                        t = context.lookahead(0);
                        if (t.getType() == TokenType.Identifier) {
                            context.nextToken();
                            Object part = context.getState(t.getText());
                            if (part == null) {
                                l.error(new ModelIssue(t, org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_UNKNOWN_ROLE", new String[] { t.getText() })));
                            } else if (part instanceof Role) {
                                ch.setFromRole((Role) part);
                            } else {
                                l.error(new ModelIssue(t, org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_REQUIRED_ROLE", new String[] { t.getText() })));
                            }
                            t = context.lookahead(0);
                        }
                    }
                    if (t.isToken("to", TokenType.Keyword)) {
                        context.nextToken();
                        t = context.lookahead(0);
                        if (t.getType() == TokenType.Identifier) {
                            context.nextToken();
                            Object part = context.getState(t.getText());
                            if (part == null) {
                                l.error(new ModelIssue(t, org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_UNKNOWN_ROLE", new String[] { t.getText() })));
                            } else if (part instanceof Role) {
                                ch.setToRole((Role) part);
                            } else {
                                l.error(new ModelIssue(t, org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_REQUIRED_ROLE", new String[] { t.getText() })));
                            }
                            t = context.lookahead(0);
                        }
                    }
                } else {
                    l.error(new ModelIssue(t, java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages").getString("_EXPECTING_CHANNEL_NAME")));
                    f_error = true;
                }
            } while (f_error == false && t.isToken(ParserConstants.LIST_SEPARATOR, TokenType.Symbol));
        }
        return (ret);
    }
}
