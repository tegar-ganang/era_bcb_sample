package rhul.tests;

import java.security.Security;
import com.gieseckedevrient.offcard.gp.card.components.GpApplet;
import com.gieseckedevrient.offcard.gp.card.components.IGpApplet;
import com.gieseckedevrient.offcard.terminal.TerminalException;
import com.gieseckedevrient.offcard.terminal.lite.ChannelConfig;
import com.gieseckedevrient.offcard.terminal.lite.ChannelRegistry;
import com.gieseckedevrient.offcard.terminal.lite.ICardChannel;
import com.gieseckedevrient.offcard.terminal.pcsc.lite.PcscCardChannelFactory;
import com.gieseckedevrient.offcard.terminal.vop.lite.VopCardChannelFactory;
import com.gieseckedevrient.offcard.util.Aid;
import com.gieseckedevrient.offcard.util.Bytes;
import com.gieseckedevrient.offcard.util.CommandApdu;
import com.gieseckedevrient.offcard.util.ResponseApdu;

/**
 * This class is responsible to create a connection to the card or simulator 
 * and offer generic methods as support for any concrete test. All methods in this 
 * class are static.
 * 
 * This is superclass for any implemented JUnit tests.
 *  
 * In the current implementation this class supports connection using Gisecke & Devrient's
 * offcard library as part of JCSStudio v. 3.0
 *  
 * @author gruna
 * @version 0.3 
 */
public class CardTest {

    /**
	 * The reference to the channel to the simulator or card
	 */
    static ICardChannel channel;

    /**
	 * Reference to the applet reference used to communicate with the card 
	 */
    static IGpApplet applet;

    /**
	 * The string used by offcard library for the "simulation" device
	 */
    static final String SIMULATION = "Simulation";

    /**
	 * The identification string used the reader 
	 */
    static final String CARD = "SCM Microsystems Inc. SDI010 Contactless Reader 0";

    /**
	 * Sample RSA public/private keys of different length. the syntax is 
	 * pk/bitlength/ = public key of length /bitlength/ as base64 encoded X.509 encoded byte string
	 * sk/bitlength/ = private key of length /bitlength/ as base64 encoded PKCS#8 encoded byte string
	 */
    static String pk512 = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMRyde7upPEFh3EaofkiunyRwK11DYL+Kq7srzPEgHn9rhaPHQpiwhC4wltr3BVOULxDAzKDFdDDD4/IBEbKZRsCAwEAAQ==";

    static String sk512 = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAxHJ17u6k8QWHcRqh+SK6fJHArXUNgv4qruyvM8SAef2uFo8dCmLCELjCW2vcFU5QvEMDMoMV0MMPj8gERsplGwIDAQABAkARiQ4R5+d9RDsU04XF0V3IJNKIKTythraUqrfw7Qdy3BQSOhLYYKYD9osctMpRj1C6D+vCWpWp4v3Yhk8dmhPBAiEA5yhxewYngEXfv2WAVjL42mK9DNk9EM0xU1CUybXlyssCIQDZjw+riV+mEKpQzMoLg5BwbOy4Xx9VmboukinhNef08QIhAJ5chLIFm1YziH/1J6DmNrpWXrHIuh8ilCJB2E0AQrdVAiBGTMUaf9xpc+8lBteu2sd8nKXtecdNSMxxqgdgX6PhcQIgYPxiEcGZHBmmjz1+CPDDcaW+RuCLbsgnfgtpp1hUESM=";

    static String pk544 = "MGAwDQYJKoZIhvcNAQEBBQADTwAwTAJFAKH6s8t7yOrGYImT9oTgyNEnBuIjSZ/km7gzZz0xPykIsUd/ummlDxwT0oHc6ZtQgsgXI3fPzqP7J5vO84W6PGyT2NpjAgMBAAE=";

    static String sk544 = "MIIBZQIBADANBgkqhkiG9w0BAQEFAASCAU8wggFLAgEAAkUAofqzy3vI6sZgiZP2hODI0ScG4iNJn+SbuDNnPTE/KQixR3+6aaUPHBPSgdzpm1CCyBcjd8/Oo/snm87zhbo8bJPY2mMCAwEAAQJEIxbBHiJ5mspQnV0NOSnMtupCZniIeXe3eAbCpZHmgcgdRuyJNvJO1WZOraU5tBx1bI8jtGfpXzwPH87cYL9HLb599okCIwDUe8klMbf2HDPbX3ygK6t9aHqG4xqS6Q+V6V57KhEBofLPAiMAwycM6pT/eh/mnMVCOhhOVzALDNdtryz4DedgbpZZ9EcULQIiaSq/WsZp68j3ratsLoaOwWpVwq9i9rWdvjAcUE94zVsPpwIiCYvxf9BGJy1EVMr5kGn7+xvojnpDbVxfzh5Rg1drkx2fCQIiXQSZimnA1QOoKtrhuNXlvSO8efaruiGWlBm085CgN2WC/g==";

    static String pk550 = "MGAwDQYJKoZIhvcNAQEBBQADTwAwTAJFLWY1bC81fSLv5JhNLrB0SqtikAe/vSapj0dBFhi3QNXKL4q4d6HWsXC49YEk14UI9/1/boN2IjUrkh/YhqDlura6ipP7AgMBAAE=";

    static String sk550 = "MIIBaQIBADANBgkqhkiG9w0BAQEFAASCAVMwggFPAgEAAkUtZjVsLzV9Iu/kmE0usHRKq2KQB7+9JqmPR0EWGLdA1covirh3odaxcLj1gSTXhQj3/X9ug3YiNSuSH9iGoOW6trqKk/sCAwEAAQJFJJD4YoS7vmjPc/2/3OU/JY1ZO80iAIYNeFvZ7qRFpbEMwqdLgSwFHJe02/J9R1Ng112o+VZOZuvlUsAXXzwfdTuNtWmhAiMG5N1zch/X5iuqWGPsnjp9StinOIwZV6G2LxJX8k3CCN8SqQIjBpXX0H1+W9rPx0EH/sQFWkJYHdSD1VSXN/X8pMDxT5V3/AMCIwV5Gqo1VzxyugXBqIrIT0z656dSCD9yR6q4EgsKDVtW6aoJAiMEXpZ1W19lWVm2jzrb23rludG38VOMUHWKBykctH0pXjbfuwIjAlGPc9oIYv5GlBS7UzEQlUBYwY2y5ghNRUGO/XNewe4MgjY=";

    static String pk1024 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCGHG3wQSaQRjEoPTc4f32PaD25m/algqxJL86CtZ5+v7EnhqFaV+7eLI23Ef+ASXwAVAfgxetOu2kd94gnNd6yTAGNdNe2tLZDxFsRO1lUNqOwViG7qHA3YnNApwVKkzah/nqkcRJFy0exhnjFLK2bnl2ZW0fXyeesYVYPNrikFQIDAQAB";

    static String sk1024 = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIYcbfBBJpBGMSg9Nzh/fY9oPbmb9qWCrEkvzoK1nn6/sSeGoVpX7t4sjbcR/4BJfABUB+DF6067aR33iCc13rJMAY1017a0tkPEWxE7WVQ2o7BWIbuocDdic0CnBUqTNqH+eqRxEkXLR7GGeMUsrZueXZlbR9fJ56xhVg82uKQVAgMBAAECgYAfgexeRqAtwx/naUJg9qrkF0RxJ/AxgFrDswScdtjuxFB1jc64r/IBpowA16ykUh9c4GEdcMJQs/0FOlN1itVvy79YgtCmFE0mtZVdW52hummA7Jj4KFPhHBEcEvQRbJx7JuW5vWISIdihLqWKVfR1qVIH+2MfAO6PjbnbSe47fQJBANy1Iqa60IZmElmgRnJK8MUYS8kNbIy/LQ1AUWMrkV2Hix4Fb8RNwbv1v3KmasU2//6BHSZ6mBjuujD6fUd6mPcCQQCbjmMnenE2p+53SBHIBojz06m/15e6cR7RVGKRz3EjfvnS5H5lPjsSBH+U+1rLglHFzu3xJXaO6Wih09jEeFRTAkALPA0vf4Lt1HRKq6XqW3Y2Ei6JZW2JHJImgoeiDK2xnRyvwocV7v9VmgTMyTvWHh6Zzei19LeZTU2dAKMv6EWxAkBzfY6OU/DUJG/+7WoOORPa+24PObCzwqKfxef826fjM7WlZvGUl1Rh9ycF5GwvpgyyMQUlwau5RP/jDhblH3VzAkEAruzmYerYXPg5WAOxtAOA2T8ZLnSKN6Z20Malqhbg2gSH+pSj3K1lWEewmsXNNEc0aHqDdRENS9GTtlEtwaqfeA==";

    static String pk2048 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjilN+R3LFNrPma5IfzTmUCHw+ht3VkDtSjAM2jip7y+VhSWX/k2S+EZiVlOLZy3BxiGa2NJyogjlKo+clukHxLOPrY7RIncumJonUOUa3yQX8rXuWKvkjcMWImjG0bGHdseXhRinDuudoihPZkrIsXmTYIb+pM+FQEroACpsJPn3VXut0OfYa01JPgEjNLL3tewOGbDRLJ85uL9etCC6n9CrfBTNbkWDEUU6Ohfhm79BV+qY9N8D51fOIl+H1fuS/uPbtCJt1vxDb4yyCPsIfUKiZIp0sesdPW9rZGI/wpYNb7AUnD/cIDDHppQD03A6gpn/kkLjPxAXN3Nhg+6hdQIDAQAB";

    static String sk2048 = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCOKU35HcsU2s+Zrkh/NOZQIfD6G3dWQO1KMAzaOKnvL5WFJZf+TZL4RmJWU4tnLcHGIZrY0nKiCOUqj5yW6QfEs4+tjtEidy6YmidQ5RrfJBfyte5Yq+SNwxYiaMbRsYd2x5eFGKcO652iKE9mSsixeZNghv6kz4VASugAKmwk+fdVe63Q59hrTUk+ASM0sve17A4ZsNEsnzm4v160ILqf0Kt8FM1uRYMRRTo6F+Gbv0FX6pj03wPnV84iX4fV+5L+49u0Im3W/ENvjLII+wh9QqJkinSx6x09b2tkYj/Clg1vsBScP9wgMMemlAPTcDqCmf+SQuM/EBc3c2GD7qF1AgMBAAECggEAYYPty5NnSc/qldWaPz/vOEp//WA31P/GhZw+RLaXws2WN/YYs8VMqmfFbsfyGP2nLRzCFjcNkR6e7DoExPPmc0Rkqz4LMSQl32Hm1DPD4grlLoUjkMmghqmqlkHfF9o6PP0eb5sAhViUEbaq+FUWI79sm8seA7miAv8e83YmJpoW4iNMR7Z2Uf4ehTOGplY7O01C2zG/ecHwUvAN0FlCRDelEeoVW/fj6OHcEa40RN+gYCuKms/pSbVCBuqcQETt7rGImcQSEA4yi7sr4jKuq80i7WNsrE4uoznG6L5Njmhi6iONUUCJSY2iotQaBjkARGvj+ZjGnJLwH6Vt1MVMpQKBgQDGrwa3bjYik4CpOpcNFcYSMPQoTEkLMm+//VGXMhvBdye0sHp/IC5+4yaCxgQURR0P6gtqSNNeNpmJNhyLM1fz09kB1z7E7kctZAEjtg2DMM3tmkrwnJiWsGzcINolthXyMkh+kygYOZYC2G+Ym6NQ14hdyFohzC4qMKl1Gr7TewKBgQC3LAz/7258TtkH10IiUcL4RMU/x6aARdAXPUTUu+7USJ/MwTWSpTuSD98l9CiKxK6WucCqlBdU+kBi4wV8AbntIQ6f9t051GqEj27s//6PRGHOs/3eUjoM87n+JsXVxEgzaoO0DYM94ktRHfzNbMbp4GRdx/3V+FT7IPDayPaTzwKBgHdLZCcDH7IHCruRmFyc9D678f24QCte70ZBnZnA1nWFS+vsAsEN250IWnku2Agrr1V827nHXFI3Slzehqj1/RtD2gqG6QNpZodUgnKkvtxEk4DUoaZzABOKfvJ1L0ZxXB/+HRUS2oIhIXc36VmiKZ9CqnhU8flVFWrzqOvNUUK9AoGBAKgcMcIjCQ/lfaKImRXOXFZnJ3El11YavKTXgniMEuGZ1a+iE30HUOj40CCROTRC1slWqdDkIZXIJ5eaK8pn+y/7CuUdOGR+41POIEw7lnH+nJWM9A47ATQp7CWpiCBtCKbHtyBk1nLRYbaNAxu2HclAPF0l032xcK0aynLBxixZAoGAdst4OrdM7ljBaNC97ijPorXqXCQCAnTxTqdCvqGp6of4foOz87H4LYiWNRmbhv6sEWosk/mOtalAvA0RFr1BJwDT+XL+LYlaX9BVR/AIPeGwdZfvsW5zBk2YMfTSuzT4i4HrZazzYuV/dGo4DTiuHKX3AHraJf/WF9fxIF1wSaw=";

    /**
	 * Used to log the commands
	 */
    static StringBuffer stb = new StringBuffer();

    /**
     * The maximum size of bytes that can be sent as data in one command APDU.
     */
    private static final int MAX_APDU_SLICE = 0xff;

    /**
	 * Creates a connection to the defined card/simulator and selects the 
	 * chosen /aid/
	 * 
	 */
    public static void setUpBeforeClass(String mode, String aid) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        String ch = mode;
        ChannelRegistry.getInstance().add(new PcscCardChannelFactory(), null);
        ChannelRegistry.getInstance().add(new VopCardChannelFactory(), "Simulation;localhost;2000");
        ChannelConfig[] channelConfigList = ChannelRegistry.getInstance().getChannelList();
        ChannelConfig channelConfig = null;
        for (int i = 0; i < channelConfigList.length; i++) {
            if (ch.equals(channelConfigList[i].getName())) {
                channelConfig = channelConfigList[i];
                break;
            }
        }
        if (channelConfig == null) {
            throw new RuntimeException("Selected channel not found, aborting");
        }
        channelConfig.setOpenTimeout(1000);
        channel = ChannelRegistry.getInstance().openChannel(channelConfig);
        applet = new GpApplet(channel, new Aid(aid));
        applet.select();
    }

    /**
	 * Creates a command APDU with given class, instruction, p1, p2 and data for each 
	 * MAX_APDU_SLICE bytes of data for dataIn. 
	 * 
	 */
    public static ResponseApdu createAndSend(IGpApplet applet, int cls, int ins, int p1, int p2, int le, byte[] dataIn) {
        int bytesLeft = dataIn.length;
        int position = 0;
        int amount;
        CommandApdu outAPDU;
        ResponseApdu resAPDU = null;
        while (bytesLeft > 0) {
            amount = (bytesLeft > MAX_APDU_SLICE) ? MAX_APDU_SLICE : bytesLeft;
            byte[] inputRev = new byte[amount];
            System.arraycopy(dataIn, position, inputRev, 0, amount);
            outAPDU = new CommandApdu(cls, ins, p1, p2, new Bytes(inputRev), le);
            resAPDU = send(applet, "", outAPDU);
            bytesLeft -= MAX_APDU_SLICE;
            position += amount;
        }
        System.out.println("");
        return resAPDU;
    }

    /**
	 * Creates a command APDU with given class, instruction, p1 and p2 for each
	 * MAX_APDU_SLICE bytes requested in le. For the first command ins will be used as
	 * instruction. For any subsequent commands insGet will be used as instruction.
	 * 
	 * @return The combined le bytes received 
	 */
    public static byte[] createAndReceive(IGpApplet applet, int cls, int ins, int insGet, int p1, int p2, int le, byte[] dataIn) {
        int bytesLeft = le;
        int amount;
        int position = 0;
        byte ret[] = new byte[le];
        int eINS = ins;
        CommandApdu outAPDU;
        ResponseApdu resAPDU;
        while (bytesLeft > 0) {
            amount = (bytesLeft > MAX_APDU_SLICE) ? MAX_APDU_SLICE : bytesLeft;
            outAPDU = new CommandApdu(cls, eINS, p1, p2, new Bytes(dataIn), 0);
            resAPDU = send(applet, "", outAPDU);
            bytesLeft -= MAX_APDU_SLICE;
            System.arraycopy(resAPDU.getData().toByteArray(), 0, ret, position, amount);
            position += amount;
            eINS = insGet;
        }
        System.out.println("");
        return ret;
    }

    /**
	 * Creates a command APDU for a given class, instruction, p1, p2, data and le.
	 */
    public static CommandApdu create_apdu(int cls, int ins, int p1, int p2, int le, byte[] dataIn) {
        CommandApdu ret_apdu = new CommandApdu(cls, ins, p1, p2, new Bytes(dataIn), le);
        return ret_apdu;
    }

    /**
	 * Sends a command APDU and prints the time between sending and receiving. 
	 * It also stores the command as string.
	 */
    public static ResponseApdu send(IGpApplet applet, String name, CommandApdu command) {
        stb.append(command.toString() + "\n");
        long start, end;
        start = System.currentTimeMillis();
        try {
            ResponseApdu response = applet.send(command);
            end = System.currentTimeMillis();
            System.out.print((end - start) + "\t");
            return response;
        } catch (TerminalException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Prints the commands sent so far.
	 */
    public static void printCommands() {
        System.out.println(stb);
    }
}
