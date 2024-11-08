package org.scribble.parser;

import junit.framework.TestCase;
import org.scribble.model.*;
import org.scribble.model.admin.ErrorRecorder;

public class InteractionParserRuleTest extends TestCase {

    protected void setUp() throws Exception {
        org.scribble.extensions.TestRegistry reg = new org.scribble.extensions.TestRegistry();
        reg.addExtension(GenericKeyWordProvider.class);
        reg.addExtension(GenericParser.class);
        reg.addExtension(org.scribble.parser.InteractionParserRule.class);
        reg.addExtension(org.scribble.parser.MessageSignatureParserRule.class);
        reg.addExtension(org.scribble.parser.TypeReferenceParserRule.class);
        reg.addExtension(TestModelRepository.class);
        org.scribble.extensions.RegistryFactory.setRegistry(reg);
    }

    protected void tearDown() throws Exception {
        org.scribble.extensions.RegistryFactory.setRegistry(null);
    }

    public void testValidInteraction() {
        String type = "Order";
        String role1Name = "Customer";
        Role role1 = new Role(role1Name);
        String role2Name = "Supplier";
        Role role2 = new Role(role2Name);
        String channelName = "Ch1";
        Channel ch1 = new Channel(channelName);
        String text = type + " from " + role1Name + " to " + role2Name + " via " + channelName + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role1Name, role1);
        context.setState(role2Name, role2);
        context.setState(channelName, ch1);
        Object obj = context.parse(Interaction.class, l);
        if (obj instanceof Interaction) {
            Interaction interaction = (Interaction) obj;
            if (interaction.getMessageSignature() == null) {
                fail("No message signature");
            } else if (interaction.getMessageSignature().getOperation() != null) {
                fail("No operation expected");
            } else if (interaction.getMessageSignature().getTypes().size() != 1) {
                fail("Only one message sig type expected: " + interaction.getMessageSignature().getTypes().size());
            } else if (interaction.getMessageSignature().getTypes().get(0).getAlias().equals(type) == false) {
                fail("Message sig type not expected '" + type + "': " + interaction.getMessageSignature().getTypes().get(0).getAlias());
            }
            if (interaction.getFromRole() == null) {
                fail("From role not set");
            } else if (interaction.getFromRole().equals(role1) == false) {
                fail("From role not expected: " + interaction.getFromRole());
            }
            if (interaction.getToRole() == null) {
                fail("To role not set");
            } else if (interaction.getToRole().equals(role2) == false) {
                fail("To role not expected: " + interaction.getToRole());
            }
            if (interaction.getChannel() == null) {
                fail("Channel not set");
            } else if (interaction.getChannel().equals(ch1) == false) {
                fail("Channel not expected");
            }
            if (l.getErrors().size() > 0) {
                fail("Not expecting " + l.getErrors().size() + " errors");
            } else if (l.getWarnings().size() > 0) {
                fail("Not expecting " + l.getWarnings().size() + " warnings");
            }
        } else if (obj == null) {
            fail("No object returned");
        } else {
            fail("Unexpected object type: " + obj.getClass());
        }
    }

    public void testInvalidInteractionToRole() {
        String type = "Order";
        String role1Name = "Customer";
        Role role1 = new Role(role1Name);
        String role2Name = "Supplier";
        String text = type + " from " + role1Name + " to " + role2Name + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role1Name, role1);
        context.parse(Interaction.class, l);
        if (l.hasError(org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_UNKNOWN_ROLE", new String[] { role2Name })) == false) {
            fail("Should register 'unknown role' error");
        }
    }

    public void testInvalidInteractionToRole2() {
        String type = "Order";
        String role1Name = "Customer";
        Role role1 = new Role(role1Name);
        String role2Name = "Supplier";
        String text = type + " from " + role1Name + " to " + role2Name + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role1Name, role1);
        context.setState(role2Name, "INVALID");
        context.parse(Interaction.class, l);
        if (l.hasError(org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_REQUIRED_ROLE", new String[] { role2Name })) == false) {
            fail("Should register 'required role' error");
        }
    }

    public void testInvalidInteractionFromRole() {
        String type = "Order";
        String role1Name = "Customer";
        String role2Name = "Supplier";
        Role role2 = new Role(role2Name);
        String text = type + " from " + role1Name + " to " + role2Name + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role2Name, role2);
        context.parse(Interaction.class, l);
        if (l.hasError(org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_UNKNOWN_ROLE", new String[] { role1Name })) == false) {
            fail("Should register 'unknown role' error");
        }
    }

    public void testInvalidInteractionFromRole2() {
        String type = "Order";
        String role1Name = "Customer";
        String role2Name = "Supplier";
        Role role2 = new Role(role2Name);
        String text = type + " from " + role1Name + " to " + role2Name + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role1Name, "INVALID");
        context.setState(role2Name, role2);
        context.parse(Interaction.class, l);
        if (l.hasError(org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_REQUIRED_ROLE", new String[] { role1Name })) == false) {
            fail("Should register 'required role' error");
        }
    }

    public void testInvalidInteractionChannel() {
        String type = "Order";
        String role1Name = "Customer";
        Role role1 = new Role(role1Name);
        String role2Name = "Supplier";
        Role role2 = new Role(role2Name);
        String channelName = "Ch1";
        String text = type + " from " + role1Name + " to " + role2Name + " via " + channelName + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role1Name, role1);
        context.setState(role2Name, role2);
        context.parse(Interaction.class, l);
        if (l.hasError(org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_UNKNOWN_CHANNEL", new String[] { channelName })) == false) {
            fail("Should register 'unknown channel' error");
        }
    }

    public void testInvalidInteractionChannel2() {
        String type = "Order";
        String role1Name = "Customer";
        Role role1 = new Role(role1Name);
        String role2Name = "Supplier";
        Role role2 = new Role(role2Name);
        String channelName = "Ch1";
        String text = type + " from " + role1Name + " to " + role2Name + " via " + channelName + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role1Name, role1);
        context.setState(role2Name, role2);
        context.setState(channelName, "INVALAID");
        context.parse(Interaction.class, l);
        if (l.hasError(org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_REQUIRED_CHANNEL", new String[] { channelName })) == false) {
            fail("Should register 'required channel' error");
        }
    }
}
