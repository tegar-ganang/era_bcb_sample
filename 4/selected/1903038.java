package org.openremote.controller.protocol.virtual;

import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.openremote.controller.command.StatusCommand;
import org.openremote.controller.command.ExecutableCommand;
import org.openremote.controller.command.CommandBuilder;
import org.openremote.controller.command.Command;
import org.openremote.controller.component.EnumSensorType;
import org.jdom.Element;

/**
 /**
  * Test 'range' sensor state reads and writes on OpenRemote virtual room/device protocol.
  *
  * @see org.openremote.controller.protocol.virtual.VirtualCommand
  *
  * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
  */
public class RangeStatusTest {

    /**
   * Reference to the command builder we can use to build command instances.
   */
    private VirtualCommandBuilder builder = null;

    @Before
    public void setUp() {
        builder = new VirtualCommandBuilder();
    }

    /**
   * Tests protocol read command behavior for 'range' sensor type when no explict command to
   * set state has been sent yet. Expecting a 'range' sensor to return '0' in such a case.
   */
    @Test
    public void testStatusDefaultValue() {
        StatusCommand cmd = getReadCommand("test range default value");
        String value = cmd.read(EnumSensorType.RANGE, new HashMap<String, String>());
        Assert.assertTrue(value.equals("0"));
    }

    /**
   * Tests 'range' sensor read/write behavior.
   */
    @Test
    public void testRangeState() {
        final String ADDRESS = "range read/write tests";
        StatusCommand readCmd = getReadCommand(ADDRESS);
        String value = readCmd.read(EnumSensorType.RANGE, new HashMap<String, String>());
        Assert.assertTrue(value.equalsIgnoreCase("0"));
        ExecutableCommand writeLevel1 = getWriteCommand(ADDRESS, "any command", 1);
        writeLevel1.send();
        value = readCmd.read(EnumSensorType.RANGE, new HashMap<String, String>());
        Assert.assertTrue(value.equals("1"));
        ExecutableCommand writeLevel10001 = getWriteCommand(ADDRESS, "any command", 10001);
        writeLevel10001.send();
        value = readCmd.read(EnumSensorType.RANGE, new HashMap<String, String>());
        Assert.assertTrue("Was expecting '10001', got '" + value + "'.", value.equals("10001"));
    }

    /**
   * Returns a read ('status') command.
   *
   * @param address   arbitrary string address
   *
   * @return  status command instance for the given address
   */
    private StatusCommand getReadCommand(String address) {
        Element ele = new Element("command");
        ele.setAttribute("id", "test");
        ele.setAttribute(CommandBuilder.PROTOCOL_ATTRIBUTE_NAME, "virtual");
        Element propAddr = new Element(CommandBuilder.XML_ELEMENT_PROPERTY);
        propAddr.setAttribute(CommandBuilder.XML_ATTRIBUTENAME_NAME, "address");
        propAddr.setAttribute(CommandBuilder.XML_ATTRIBUTENAME_VALUE, address);
        ele.addContent(propAddr);
        Element propAddr2 = new Element(CommandBuilder.XML_ELEMENT_PROPERTY);
        propAddr2.setAttribute(CommandBuilder.XML_ATTRIBUTENAME_NAME, "command");
        propAddr2.setAttribute(CommandBuilder.XML_ATTRIBUTENAME_VALUE, "status");
        ele.addContent(propAddr2);
        Command cmd = builder.build(ele);
        if (!(cmd instanceof StatusCommand)) {
            Assert.fail("Was expecting a read command (StatusCommand) type, got " + cmd.getClass());
            return null;
        } else {
            return (StatusCommand) cmd;
        }
    }

    /**
   * Creates a write command with given command value hacked into an XML element attribute.
   *
   * @param address   arbitrary address string
   * @param cmd       arbitrary command name
   * @param value     command value
   *
   * @return  write command instance with given parameters
   */
    private ExecutableCommand getWriteCommand(String address, String cmd, int value) {
        Element ele = new Element("command");
        ele.setAttribute("id", "test");
        ele.setAttribute(CommandBuilder.PROTOCOL_ATTRIBUTE_NAME, "virtual");
        ele.setAttribute(Command.DYNAMIC_VALUE_ATTR_NAME, Integer.toString(value));
        Element propAddr = new Element(CommandBuilder.XML_ELEMENT_PROPERTY);
        propAddr.setAttribute(CommandBuilder.XML_ATTRIBUTENAME_NAME, "address");
        propAddr.setAttribute(CommandBuilder.XML_ATTRIBUTENAME_VALUE, address);
        ele.addContent(propAddr);
        Element propAddr2 = new Element(CommandBuilder.XML_ELEMENT_PROPERTY);
        propAddr2.setAttribute(CommandBuilder.XML_ATTRIBUTENAME_NAME, "command");
        propAddr2.setAttribute(CommandBuilder.XML_ATTRIBUTENAME_VALUE, cmd);
        ele.addContent(propAddr2);
        Command command = builder.build(ele);
        if (!(command instanceof ExecutableCommand)) {
            Assert.fail("Was expecting a write command (ExecutableCommand) type, got " + command.getClass());
            return null;
        } else {
            return (ExecutableCommand) command;
        }
    }
}
