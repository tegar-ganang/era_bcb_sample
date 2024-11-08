package org.inigma.utopia.parser;

import org.inigma.utopia.Race;
import org.inigma.utopia.Science;
import org.junit.Test;

public class ScienceParserTest extends ParserTestBase {

    @Test
    public void checkSelfScience() {
        ScienceParserData data = ScienceParser.parse(fileToString("/age41/self/dwarf-sage-sos.txt"));
        assertNotNull(data);
        assertNull(data.getProvinceName());
        data.adjustScience(550, Race.Dwarf);
        Science science = data.getScience();
        assertEquals(8068, science.getAlchemy());
        assertEquals(8063, science.getTools());
        assertEquals(7558, science.getHousing());
        assertEquals(3127, science.getFood());
        assertEquals(4056, science.getMilitary());
        assertEquals(5210, science.getCrime());
        assertEquals(5207, science.getChanneling());
    }

    @Test
    public void checkOrcWarrior() {
        ScienceParserData data = ScienceParser.parse(fileToString("/age41/random/orc-warrior-sos.txt"));
        assertNotNull(data);
        assertNotNull(data.getProvinceName());
        assertEquals(13, data.getCoordinate().getKingdom());
        assertEquals(3, data.getCoordinate().getIsland());
        data.adjustScience(311, Race.Orc);
        Science science = data.getScience();
        assertEquals(0, science.getAlchemy());
        assertEquals(0, science.getTools());
        assertEquals(0, science.getHousing());
        assertEquals(0, science.getFood());
        assertEquals(3966, science.getMilitary());
        assertEquals(9811, science.getCrime());
        assertEquals(22734, science.getChanneling());
    }

    @Test
    public void checkOrcAsHuman() {
        ScienceParserData data = ScienceParser.parse(fileToString("/age41/random/orc-warrior-sos.txt"));
        assertNotNull(data);
        assertNotNull(data.getProvinceName());
        assertEquals(13, data.getCoordinate().getKingdom());
        assertEquals(3, data.getCoordinate().getIsland());
        data.adjustScience(311, Race.Human);
        Science science = data.getScience();
        assertEquals(0, science.getAlchemy());
        assertEquals(0, science.getTools());
        assertEquals(0, science.getHousing());
        assertEquals(0, science.getFood());
        assertEquals(2538, science.getMilitary());
        assertEquals(6279, science.getCrime());
        assertEquals(14550, science.getChanneling());
    }

    @Test
    public void parseAngelSelfElf() {
        ScienceParserData data = ScienceParser.parse(fileToString("/age41/angel/self/elf-warhero-sos.txt"));
        assertNotNull(data);
        assertNotNull(data.getProvinceName());
        assertEquals(12, data.getCoordinate().getKingdom());
        assertEquals(3, data.getCoordinate().getIsland());
        data.adjustScience(1017, Race.Human);
        Science science = data.getScience();
        assertFalse(data.isRaw());
        assertEquals(18852, science.getAlchemy());
        assertEquals(18128, science.getTools());
        assertEquals(17759, science.getHousing());
        assertEquals(6439, science.getFood());
        assertEquals(1242, science.getMilitary());
        assertEquals(10318, science.getCrime());
        assertEquals(10429, science.getChanneling());
    }

    @Test
    public void parseAngelHuman() {
        ScienceParserData data = ScienceParser.parse(fileToString("/age41/angel/combo/human-merchant-sos.txt"));
        assertNotNull(data);
        assertNotNull(data.getProvinceName());
        assertEquals(11, data.getCoordinate().getKingdom());
        assertEquals(11, data.getCoordinate().getIsland());
        data.adjustScience(550, Race.Human);
        Science science = data.getScience();
        assertTrue(data.isRaw());
        assertEquals(39871, science.getAlchemy());
        assertEquals(63205, science.getTools());
        assertEquals(68991, science.getHousing());
        assertEquals(7942, science.getFood());
        assertEquals(14225, science.getMilitary());
        assertEquals(5726, science.getCrime());
        assertEquals(2926, science.getChanneling());
    }
}
