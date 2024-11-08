package net.sf.solarnetwork.node.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.sf.solarnetwork.node.AbstractTransactionalTests;
import net.sf.solarnetwork.node.ConversationalDataCollector;
import net.sf.solarnetwork.node.PowerDatum;
import net.sf.solarnetwork.node.dao.SettingDao;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * Test case for the {@link SMASerialGenerationDataSource} class.
 * 
 * @author matt
 * @version $Revision: 366 $ $Date: 2009-09-17 20:42:21 -0400 (Thu, 17 Sep 2009) $
 */
public class SMASerialGenerationDataSourceTest extends AbstractTransactionalTests {

    private static final class MockSMACollector implements ConversationalDataCollector<PowerDatum> {

        private List<byte[]> responses;

        private int responseIdx = -1;

        private List<byte[]> spoken = new ArrayList<byte[]>();

        private MockSMACollector(List<byte[]> responses) {
            this.responses = responses;
        }

        private void reset() {
            responseIdx = -1;
            spoken.clear();
        }

        private int getSpokenCount() {
            return spoken.size();
        }

        private String getSpokenHex(int index) {
            if (index < 0 || index > (spoken.size() - 1)) {
                return null;
            }
            return String.valueOf(Hex.encodeHex(spoken.get(index))).toUpperCase();
        }

        public PowerDatum collectData(Moderator<PowerDatum> moderator) {
            return moderator.conductConversation(this);
        }

        public void speakAndListen(byte[] data) {
            spoken.add(data);
            responseIdx++;
        }

        public void speak(byte[] data) {
            spoken.add(data);
        }

        public int bytesRead() {
            return 0;
        }

        public void collectData() {
            throw new UnsupportedOperationException();
        }

        public byte[] getCollectedData() {
            return responses.get(responseIdx);
        }

        public String getCollectedDataAsString() {
            throw new UnsupportedOperationException();
        }

        public void stopCollecting() {
        }
    }

    private static final String NET_START_HEX = "AAAA6800006800000000800006860016";

    private static final String GET_CHANNEL_INFO_HEX = "AAAA68000068000006000000090F0016";

    private static final String SYN_ONLINE_HEX = "AAAA680404680000000080000A";

    private static final String GET_DATA_VPV = "AAAA680303680000060000000B0109011C0016";

    private static final String GET_DATA_IPV = "AAAA680303680000060000000B0109112C0016";

    private static final String GET_DATA_ETOTAL = "AAAA680303680000060000000B0409011F0016";

    @Autowired
    private SettingDao settingDao;

    /**
	 * Setup the test.
	 */
    @Before
    public void setup() {
        settingDao.deleteSetting(SMASerialGenerationDataSource.SETTING_LAST_KNOWN_DAY);
    }

    /**
	 * Test conducting a conversation to read some data.
	 * 
	 * @throws Exception
	 */
    @Test
    public void conductConversation() throws Exception {
        MockSMACollector collector = newMockCollectorInstance();
        SMASerialGenerationDataSource ds = new SMASerialGenerationDataSource();
        ds.setChannelNamesToOffsetDaily(null);
        PowerDatum d = ds.conductConversation(collector);
        assertEquals(89, collector.getSpokenCount());
        assertEquals(NET_START_HEX, collector.getSpokenHex(0));
        assertEquals(GET_CHANNEL_INFO_HEX, collector.getSpokenHex(1));
        assertTrue(collector.getSpokenHex(85).startsWith(SYN_ONLINE_HEX));
        int getDataIndex = 86;
        assertEquals(GET_DATA_VPV, collector.getSpokenHex(getDataIndex++));
        assertEquals(GET_DATA_IPV, collector.getSpokenHex(getDataIndex++));
        assertEquals(GET_DATA_ETOTAL, collector.getSpokenHex(getDataIndex++));
        assertNotNull(d.getPvVolts());
        assertEquals(362.0, d.getPvVolts(), 0.1);
        assertNotNull(d.getPvAmps());
        assertEquals(6.935, d.getPvAmps(), 0.001);
        assertEquals(3373.92578125, d.getKWattHoursToday(), 0.001);
    }

    private MockSMACollector newMockCollectorInstance() throws IOException, DecoderException {
        final List<byte[]> mockResponses = new ArrayList<byte[]>();
        ClassPathResource getChannelResponse = new ClassPathResource("sma-getchannellist-response.txt", SMASerialGenerationDataSourceTest.class);
        BufferedReader r = new BufferedReader(new InputStreamReader(getChannelResponse.getInputStream()));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.length() < 1 || line.charAt(0) == '#') {
                continue;
            }
            byte[] data = Hex.decodeHex(line.toCharArray());
            mockResponses.add(data);
        }
        MockSMACollector collector = new MockSMACollector(mockResponses);
        return collector;
    }

    /**
	 * Test the same conversation, this time with kWh "daily offset" logic enabled.
	 * @throws Exception
	 */
    @Test
    public void testDayOffset() throws Exception {
        MockSMACollector collector = newMockCollectorInstance();
        SMASerialGenerationDataSource ds = new SMASerialGenerationDataSource();
        ds.setSettingDao(settingDao);
        PowerDatum d = ds.conductConversation(collector);
        assertEquals(GET_DATA_ETOTAL, collector.getSpokenHex(collector.getSpokenCount() - 1));
        assertEquals(0.0, d.getKWattHoursToday(), 0.001);
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.D");
        String expectedDayStartValue = sdf.format(now.getTime());
        String dayStartValue = settingDao.getSetting(SMASerialGenerationDataSource.SETTING_LAST_KNOWN_DAY);
        assertNotNull(dayStartValue);
        assertEquals(expectedDayStartValue, dayStartValue);
        String eTotalDayStartKey = SMASerialGenerationDataSource.SETTING_DAY_START_VALUE_PREFIX + "E-Total";
        String eTotalDayStartValue = settingDao.getSetting(eTotalDayStartKey);
        assertNotNull(eTotalDayStartValue);
        Integer value = Integer.valueOf(eTotalDayStartValue);
        assertEquals(202435504, value.intValue());
        settingDao.storeSetting(eTotalDayStartKey, String.valueOf(202430504));
        collector.reset();
        ds = new SMASerialGenerationDataSource();
        ds.setSettingDao(settingDao);
        d = ds.conductConversation(collector);
        assertEquals(GET_DATA_ETOTAL, collector.getSpokenHex(collector.getSpokenCount() - 1));
        assertEquals(.083, d.getKWattHoursToday(), 0.001);
        now.add(Calendar.DATE, -1);
        settingDao.storeSetting(SMASerialGenerationDataSource.SETTING_LAST_KNOWN_DAY, sdf.format(now.getTime()));
        String eTotalLastValueKey = SMASerialGenerationDataSource.SETTING_LAST_KNOWN_VALUE_PREFIX + "E-Total";
        settingDao.storeSetting(eTotalLastValueKey, String.valueOf(202432504));
        collector.reset();
        ds = new SMASerialGenerationDataSource();
        ds.setSettingDao(settingDao);
        d = ds.conductConversation(collector);
        dayStartValue = settingDao.getSetting(SMASerialGenerationDataSource.SETTING_LAST_KNOWN_DAY);
        assertNotNull(dayStartValue);
        assertEquals(expectedDayStartValue, dayStartValue);
        eTotalDayStartValue = settingDao.getSetting(eTotalDayStartKey);
        assertNotNull(eTotalDayStartValue);
        value = Integer.valueOf(eTotalDayStartValue);
        assertEquals(202432504, value.intValue());
        assertEquals(GET_DATA_ETOTAL, collector.getSpokenHex(collector.getSpokenCount() - 1));
        assertEquals(0.05, d.getKWattHoursToday(), 0.001);
    }
}
