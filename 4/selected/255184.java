package org.scribble.parser;

import junit.framework.TestCase;
import org.scribble.model.*;
import org.scribble.model.admin.ErrorRecorder;

public class ChannelListParserRuleTest extends TestCase {

    protected void setUp() throws Exception {
        org.scribble.extensions.TestRegistry reg = new org.scribble.extensions.TestRegistry();
        reg.addExtension(GenericKeyWordProvider.class);
        reg.addExtension(GenericParser.class);
        reg.addExtension(org.scribble.parser.ChannelListParserRule.class);
        reg.addExtension(TestModelRepository.class);
        org.scribble.extensions.RegistryFactory.setRegistry(reg);
    }

    protected void tearDown() throws Exception {
        org.scribble.extensions.RegistryFactory.setRegistry(null);
    }

    public void testValidChannelList() {
        String role1Name = "Customer";
        Role role1 = new Role(role1Name);
        String role2Name = "Supplier";
        Role role2 = new Role(role2Name);
        String channel1Name = "Ch1";
        String channel2Name = "Ch2";
        String text = "channel " + channel1Name + " from " + role1Name + " to " + role2Name + ", " + channel2Name + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role1Name, role1);
        context.setState(role2Name, role2);
        Object obj = context.parse(ChannelList.class, l);
        if (obj instanceof ChannelList) {
            ChannelList chlist = (ChannelList) obj;
            if (chlist.getChannels().size() != 2) {
                fail("Two channels expected: " + chlist.getChannels().size());
            }
            if (chlist.getChannels().get(0).getFromRole() == null) {
                fail("First channel From role not set");
            } else if (chlist.getChannels().get(0).getFromRole() != role1) {
                fail("First channel From role not expected: " + chlist.getChannels().get(0).getFromRole());
            }
            if (chlist.getChannels().get(0).getToRole() == null) {
                fail("First channel To role not set");
            } else if (chlist.getChannels().get(0).getFromRole() != role1) {
                fail("First channel To role not expected: " + chlist.getChannels().get(0).getToRole());
            }
            if (chlist.getChannels().get(1).getFromRole() != null) {
                fail("Second channel should not have 'from' role");
            }
            if (chlist.getChannels().get(1).getToRole() != null) {
                fail("Second channel should not have 'to' role");
            }
            Channel ch1 = (Channel) context.getState(channel1Name);
            Channel ch2 = (Channel) context.getState(channel2Name);
            if (ch1 == null) {
                fail("Channel1 not created");
            }
            if (ch2 == null) {
                fail("Channel2 not created");
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

    public void testInvalidChannelListToRole() {
        String role1Name = "Customer";
        Role role1 = new Role(role1Name);
        String role2Name = "Supplier";
        Role role2 = new Role(role2Name);
        String channel1Name = "Ch1";
        String channel2Name = "Ch2";
        String text = "channel " + channel1Name + " from " + role1Name + " to " + role2Name + ", " + channel2Name + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role1Name, role1);
        context.parse(ChannelList.class, l);
        if (l.hasError(org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_UNKNOWN_ROLE", new String[] { role2Name })) == false) {
            fail("Should register 'unknown role' error");
        }
    }

    public void testInvalidChannelListFromRole() {
        String role1Name = "Customer";
        Role role1 = new Role(role1Name);
        String role2Name = "Supplier";
        Role role2 = new Role(role2Name);
        String channel1Name = "Ch1";
        String channel2Name = "Ch2";
        String text = "channel " + channel1Name + " from " + role1Name + " to " + role2Name + ", " + channel2Name + ";";
        ModelReference ref = new ModelReference("");
        TestParserContext context = new TestParserContext(ref, text);
        ErrorRecorder l = new ErrorRecorder();
        context.setState(role2Name, role2);
        context.parse(ChannelList.class, l);
        if (l.hasError(org.scribble.util.MessageUtil.format(java.util.PropertyResourceBundle.getBundle("org.scribble.parser.Messages"), "_UNKNOWN_ROLE", new String[] { role1Name })) == false) {
            fail("Should register 'unknown role' error");
        }
    }
}
