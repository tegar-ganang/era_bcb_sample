package libsecondlife;

import java.io.*;
import java.util.*;

public class ProtocolManager {

    public Hashtable TypeSizes;

    public HashMapInt KeywordPositions;

    public MapPacketMap LowMaps;

    public MapPacketMap MediumMaps;

    public MapPacketMap HighMaps;

    private SecondLife Client;

    private int i = 0;

    public ProtocolManager(String mapFile, SecondLife client) throws Exception {
        Client = client;
        LowMaps = new MapPacketMap(256);
        MediumMaps = new MapPacketMap(256);
        HighMaps = new MapPacketMap(256);
        TypeSizes = new Hashtable();
        TypeSizes.put(FieldType.U8, 1);
        TypeSizes.put(FieldType.U16, 2);
        TypeSizes.put(FieldType.U32, 4);
        TypeSizes.put(FieldType.U64, 8);
        TypeSizes.put(FieldType.S8, 1);
        TypeSizes.put(FieldType.S16, 2);
        TypeSizes.put(FieldType.S32, 4);
        TypeSizes.put(FieldType.S64, 8);
        TypeSizes.put(FieldType.F32, 4);
        TypeSizes.put(FieldType.F64, 8);
        TypeSizes.put(FieldType.LLUUID, 16);
        TypeSizes.put(FieldType.BOOL, 1);
        TypeSizes.put(FieldType.LLVector3, 12);
        TypeSizes.put(FieldType.LLVector3d, 24);
        TypeSizes.put(FieldType.LLVector4, 16);
        TypeSizes.put(FieldType.LLQuaternion, 16);
        TypeSizes.put(FieldType.IPADDR, 4);
        TypeSizes.put(FieldType.IPPORT, 2);
        TypeSizes.put(FieldType.Variable, -1);
        TypeSizes.put(FieldType.Fixed, -2);
        KeywordPositions = new HashMapInt();
        LoadMapFile(mapFile);
    }

    public MapPacket Command(String command) throws Exception {
        MapPacket map = HighMaps.getMapPacketByName(command);
        if (map == null) {
            map = MediumMaps.getMapPacketByName(command);
            if (map == null) map = LowMaps.getMapPacketByName(command); else throw new Exception("Cannot find map for command \"" + command + "\"");
        }
        return map;
    }

    public MapPacket Command(byte[] data) throws Exception {
        int command;
        if (data.length < 5) {
            return null;
        }
        if (data[4] == (byte) 0xFF) {
            if ((byte) data[5] == (byte) 0xFF) {
                command = (int) (data[6] * 256 + data[7]);
                return Command(command, PacketFrequency.Low);
            } else {
                command = (int) data[5];
                return Command(command, PacketFrequency.Medium);
            }
        } else {
            command = (int) data[4];
            return Command(command, PacketFrequency.High);
        }
    }

    public MapPacket Command(int command, int frequency) throws Exception {
        switch(frequency) {
            case PacketFrequency.High:
                return HighMaps.getMapPacketByCommand(command);
            case PacketFrequency.Medium:
                return MediumMaps.getMapPacketByCommand(command);
            case PacketFrequency.Low:
                return LowMaps.getMapPacketByCommand(command);
        }
        throw new Exception("Cannot find map for command \"" + command + "\" with frequency \"" + frequency + "\"");
    }

    public void PrintMap() {
        PrintOneMap(LowMaps, "Low   ");
        PrintOneMap(MediumMaps, "Medium");
        PrintOneMap(HighMaps, "High  ");
    }

    private void PrintOneMap(MapPacketMap map, String frequency) {
        int i;
        for (i = 0; i < map.mapPackets.size(); ++i) {
            MapPacket map_packet = (MapPacket) map.mapPackets.elementAt(i);
            if (map_packet != null) {
                for (int j = 0; j < map_packet.Blocks.size(); j++) {
                    MapBlock block = (MapBlock) map_packet.Blocks.get(j);
                    if (block.Count == -1) {
                    } else {
                    }
                    for (int k = 0; k < block.Fields.size(); k++) {
                        MapField field = (MapField) block.Fields.elementAt(k);
                    }
                }
            }
        }
    }

    private void LoadKeywordFile(String keywordFile) throws Exception {
        String line;
        BufferedReader file;
        KeywordPositions = new HashMapInt();
        try {
            file = new BufferedReader(new FileReader(keywordFile));
        } catch (Exception e) {
            Client.Log("Error opening \"" + keywordFile + "\": " + e.toString(), Helpers.LogLevel.Error);
            throw new Exception("Keyword file error", e);
        }
        while ((line = file.readLine()) != null) {
            KeywordPositions.put(line.trim(), new Integer(i++));
        }
        file.close();
    }

    public static void DecodeMapFile(String mapFile, String outputFile) throws Exception {
        byte magicKey = 0;
        byte[] buffer = new byte[2048];
        int nread;
        InputStream map;
        OutputStream output;
        try {
            map = new FileInputStream(mapFile);
        } catch (Exception e) {
            throw new Exception("Map file error", e);
        }
        try {
            output = new FileOutputStream(outputFile);
        } catch (Exception e) {
            throw new Exception("Map file error", e);
        }
        while ((nread = map.read(buffer, 0, 2048)) != 0) {
            for (int i = 0; i < nread; ++i) {
                buffer[i] ^= magicKey;
                magicKey += 43;
            }
            output.write(buffer, 0, nread);
        }
        map.close();
        output.close();
    }

    private void LoadMapFile(String mapFile) throws Exception {
        FileReader map;
        int low = 1;
        int medium = 1;
        int high = 1;
        try {
            map = new FileReader(mapFile);
        } catch (Exception e) {
            throw new Exception("Map file error", e);
        }
        try {
            BufferedReader r = new BufferedReader(map);
            String newline;
            String trimmedline;
            boolean inPacket = false;
            boolean inBlock = false;
            MapPacket currentPacket = null;
            MapBlock currentBlock = null;
            while (r.ready()) {
                newline = r.readLine();
                trimmedline = newline.trim();
                if (!inPacket) {
                    if (trimmedline.equals("{")) {
                        inPacket = true;
                    }
                } else {
                    if (!inBlock) {
                        if (trimmedline.equals("{")) {
                            inBlock = true;
                        } else if (trimmedline.equals("}")) {
                            Collections.sort(currentPacket.Blocks);
                            inPacket = false;
                        } else {
                            String[] tokens = trimmedline.split("\\s+");
                            if (tokens.length > 3) {
                                if (tokens[1].equals("Fixed")) {
                                    if (tokens[2].substring(0, 2).equals("0x")) {
                                        tokens[2] = tokens[2].substring(2, tokens[2].length());
                                    }
                                    long l_fixedID = Long.parseLong(tokens[2], 16);
                                    int fixedID = (int) (l_fixedID ^ 0xFFFF0000);
                                    currentPacket = new MapPacket();
                                    currentPacket.ID = (int) fixedID;
                                    currentPacket.Frequency = PacketFrequency.Low;
                                    currentPacket.Name = tokens[0];
                                    currentPacket.Trusted = (tokens[3].equals("Trusted"));
                                    currentPacket.Encoded = (tokens[4].equals("Zerocoded"));
                                    currentPacket.Blocks = new ArrayList();
                                    LowMaps.addPacket(fixedID, currentPacket);
                                } else if (tokens[1].equals("Low")) {
                                    currentPacket = new MapPacket();
                                    currentPacket.ID = low;
                                    currentPacket.Frequency = PacketFrequency.Low;
                                    currentPacket.Name = tokens[0];
                                    currentPacket.Trusted = (tokens[2].equals("Trusted"));
                                    currentPacket.Encoded = (tokens[3].equals("Zerocoded"));
                                    currentPacket.Blocks = new ArrayList();
                                    LowMaps.addPacket(low, currentPacket);
                                    low++;
                                } else if (tokens[1].equals("Medium")) {
                                    currentPacket = new MapPacket();
                                    currentPacket.ID = medium;
                                    currentPacket.Frequency = PacketFrequency.Low;
                                    currentPacket.Name = tokens[0];
                                    currentPacket.Trusted = (tokens[2].equals("Trusted"));
                                    currentPacket.Encoded = (tokens[3].equals("Zerocoded"));
                                    currentPacket.Blocks = new ArrayList();
                                    MediumMaps.addPacket(medium, currentPacket);
                                    medium++;
                                } else if (tokens[1].equals("High")) {
                                    currentPacket = new MapPacket();
                                    currentPacket.ID = high;
                                    currentPacket.Frequency = PacketFrequency.Low;
                                    currentPacket.Name = tokens[0];
                                    currentPacket.Trusted = (tokens[2].equals("Trusted"));
                                    currentPacket.Encoded = (tokens[3].equals("Zerocoded"));
                                    currentPacket.Blocks = new ArrayList();
                                    HighMaps.addPacket(high, currentPacket);
                                    high++;
                                } else {
                                    Client.Log("Unknown packet frequency", Helpers.LogLevel.Error);
                                }
                            }
                        }
                    } else {
                        if (trimmedline.length() > 0 && trimmedline.substring(0, 1).equals("{")) {
                            MapField field = new MapField();
                            String[] tokens = trimmedline.split("\\s+");
                            field.Name = tokens[1];
                            field.KeywordPosition = KeywordPosition(field.Name);
                            field.Type = parseFieldType(tokens[2]);
                            if (tokens[3].equals("}") == false) {
                                field.Count = Integer.parseInt(tokens[3]);
                            } else {
                                field.Count = 1;
                            }
                            currentBlock.Fields.addElement(field);
                        } else if (trimmedline.equals("}")) {
                            Collections.sort(currentBlock.Fields);
                            inBlock = false;
                        } else if (trimmedline.length() != 0 && trimmedline.substring(0, 2).equals("//") == false) {
                            currentBlock = new MapBlock();
                            String[] tokens = trimmedline.split("\\s+");
                            currentBlock.Name = tokens[0];
                            currentBlock.KeywordPosition = KeywordPosition(currentBlock.Name);
                            currentBlock.Fields = new Vector();
                            currentPacket.Blocks.add(currentBlock);
                            if (tokens[1].equals("Single")) {
                                currentBlock.Count = 1;
                            } else if (tokens[1].equals("Multiple")) {
                                currentBlock.Count = Integer.parseInt(tokens[2]);
                            } else if (tokens[1].equals("Variable")) {
                                currentBlock.Count = -1;
                            } else {
                                Client.Log("Unknown block frequency", Helpers.LogLevel.Error);
                            }
                        }
                    }
                }
            }
            r.close();
            map.close();
        } catch (Exception e) {
            throw e;
        }
    }

    private int KeywordPosition(String keyword) throws Exception {
        if (KeywordPositions.containsKey(keyword)) {
            return KeywordPositions.get(keyword);
        } else {
            int hash = 0;
            for (int i = 1; i < keyword.length(); i++) {
                hash = (hash + (int) (keyword.charAt(i))) * 2;
            }
            hash *= 2;
            hash &= 0x1FFF;
            int startHash = hash;
            while (KeywordPositions.containsValue(hash)) {
                hash++;
                hash &= 0x1FFF;
                if (hash == startHash) {
                    throw new Exception("All hash values are taken. Failed to add keyword: " + keyword);
                }
            }
            KeywordPositions.put(keyword, hash);
            return hash;
        }
    }

    public static int parseFieldType(String token) {
        int value = 0;
        for (int i = 0; i < FieldType.TypeNames.length; i++) if (FieldType.TypeNames[i].equals(token)) {
            value = i;
            break;
        }
        return value;
    }
}
