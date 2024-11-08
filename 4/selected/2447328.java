package net.charabia.jsmoothgen.pe;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 *
 * @author  Rodrigo Reyes
 */
public class PEHeader implements Cloneable {

    public int Machine;

    public int NumberOfSections;

    public long TimeDateStamp;

    public long PointerToSymbolTable;

    public long NumberOfSymbols;

    public int SizeOfOptionalHeader;

    public int Characteristics;

    public int Magic;

    public short MajorLinkerVersion;

    public short MinorLinkerVersion;

    public long SizeOfCode;

    public long SizeOfInitializedData;

    public long SizeOfUninitializedData;

    public long AddressOfEntryPoint;

    public long BaseOfCode;

    public long BaseOfData;

    public long ImageBase;

    public long SectionAlignment;

    public long FileAlignment;

    public int MajorOperatingSystemVersion;

    public int MinorOperatingSystemVersion;

    public int MajorImageVersion;

    public int MinorImageVersion;

    public int MajorSubsystemVersion;

    public int MinorSubsystemVersion;

    public long Reserved1;

    public long SizeOfImage;

    public long SizeOfHeaders;

    public long CheckSum;

    public int Subsystem;

    public int DllCharacteristics;

    public long SizeOfStackReserve;

    public long SizeOfStackCommit;

    public long SizeOfHeapReserve;

    public long SizeOfHeapCommit;

    public long LoaderFlags;

    public long NumberOfRvaAndSizes;

    public long ExportDirectory_VA;

    public long ExportDirectory_Size;

    public long ImportDirectory_VA;

    public long ImportDirectory_Size;

    public long ResourceDirectory_VA;

    public long ResourceDirectory_Size;

    public long ExceptionDirectory_VA;

    public long ExceptionDirectory_Size;

    public long SecurityDirectory_VA;

    public long SecurityDirectory_Size;

    public long BaseRelocationTable_VA;

    public long BaseRelocationTable_Size;

    public long DebugDirectory_VA;

    public long DebugDirectory_Size;

    public long ArchitectureSpecificData_VA;

    public long ArchitectureSpecificData_Size;

    public long RVAofGP_VA;

    public long RVAofGP_Size;

    public long TLSDirectory_VA;

    public long TLSDirectory_Size;

    public long LoadConfigurationDirectory_VA;

    public long LoadConfigurationDirectory_Size;

    public long BoundImportDirectoryinheaders_VA;

    public long BoundImportDirectoryinheaders_Size;

    public long ImportAddressTable_VA;

    public long ImportAddressTable_Size;

    public long DelayLoadImportDescriptors_VA;

    public long DelayLoadImportDescriptors_Size;

    public long COMRuntimedescriptor_VA;

    public long COMRuntimedescriptor_Size;

    private long m_baseoffset;

    private PEFile m_pe;

    /** Creates a new instance of PEHeader */
    public PEHeader(PEFile pef, long baseoffset) {
        m_pe = pef;
        m_baseoffset = baseoffset;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void read() throws IOException {
        FileChannel ch = m_pe.getChannel();
        ByteBuffer head = ByteBuffer.allocate(255);
        head.order(ByteOrder.LITTLE_ENDIAN);
        ch.position(m_baseoffset);
        ch.read(head);
        head.position(0);
        int pemagic = head.getInt();
        Machine = head.getShort();
        NumberOfSections = head.getShort();
        TimeDateStamp = head.getInt();
        PointerToSymbolTable = head.getInt();
        NumberOfSymbols = head.getInt();
        SizeOfOptionalHeader = head.getShort();
        Characteristics = head.getShort();
        Magic = head.getShort();
        MajorLinkerVersion = head.get();
        MinorLinkerVersion = head.get();
        SizeOfCode = head.getInt();
        SizeOfInitializedData = head.getInt();
        SizeOfUninitializedData = head.getInt();
        AddressOfEntryPoint = head.getInt();
        BaseOfCode = head.getInt();
        BaseOfData = head.getInt();
        ImageBase = head.getInt();
        SectionAlignment = head.getInt();
        FileAlignment = head.getInt();
        MajorOperatingSystemVersion = head.getShort();
        MinorOperatingSystemVersion = head.getShort();
        MajorImageVersion = head.getShort();
        MinorImageVersion = head.getShort();
        MajorSubsystemVersion = head.getShort();
        MinorSubsystemVersion = head.getShort();
        Reserved1 = head.getInt();
        SizeOfImage = head.getInt();
        SizeOfHeaders = head.getInt();
        CheckSum = head.getInt();
        Subsystem = head.getShort();
        DllCharacteristics = head.getShort();
        SizeOfStackReserve = head.getInt();
        SizeOfStackCommit = head.getInt();
        SizeOfHeapReserve = head.getInt();
        SizeOfHeapCommit = head.getInt();
        LoaderFlags = head.getInt();
        NumberOfRvaAndSizes = head.getInt();
        ExportDirectory_VA = head.getInt();
        ExportDirectory_Size = head.getInt();
        ImportDirectory_VA = head.getInt();
        ImportDirectory_Size = head.getInt();
        ResourceDirectory_VA = head.getInt();
        ResourceDirectory_Size = head.getInt();
        ExceptionDirectory_VA = head.getInt();
        ExceptionDirectory_Size = head.getInt();
        SecurityDirectory_VA = head.getInt();
        SecurityDirectory_Size = head.getInt();
        BaseRelocationTable_VA = head.getInt();
        BaseRelocationTable_Size = head.getInt();
        DebugDirectory_VA = head.getInt();
        DebugDirectory_Size = head.getInt();
        ArchitectureSpecificData_VA = head.getInt();
        ArchitectureSpecificData_Size = head.getInt();
        RVAofGP_VA = head.getInt();
        RVAofGP_Size = head.getInt();
        TLSDirectory_VA = head.getInt();
        TLSDirectory_Size = head.getInt();
        LoadConfigurationDirectory_VA = head.getInt();
        LoadConfigurationDirectory_Size = head.getInt();
        BoundImportDirectoryinheaders_VA = head.getInt();
        BoundImportDirectoryinheaders_Size = head.getInt();
        ImportAddressTable_VA = head.getInt();
        ImportAddressTable_Size = head.getInt();
        DelayLoadImportDescriptors_VA = head.getInt();
        DelayLoadImportDescriptors_Size = head.getInt();
        COMRuntimedescriptor_VA = head.getInt();
        COMRuntimedescriptor_Size = head.getInt();
    }

    public void dump(PrintStream out) {
        out.println("HEADER:");
        out.println("int  Machine=" + Machine + " //  4");
        out.println("int  NumberOfSections=" + NumberOfSections + "     //  6");
        out.println("long   TimeDateStamp=" + TimeDateStamp + " //  8");
        out.println("long   PointerToSymbolTable=" + PointerToSymbolTable + "     //  C");
        out.println("long   NumberOfSymbols=" + NumberOfSymbols + " // 10");
        out.println("int  SizeOfOptionalHeader=" + SizeOfOptionalHeader + "     // 14");
        out.println("int  Characteristics=" + Characteristics + " // 16");
        out.println("int    Magic=" + Magic + "     // 18");
        out.println("short   MajorLinkerVersion=" + MajorLinkerVersion + "     // 1a");
        out.println("short   MinorLinkerVersion=" + MinorLinkerVersion + " // 1b");
        out.println("long   SizeOfCode=" + SizeOfCode + "     // 1c");
        out.println("long   SizeOfInitializedData=" + SizeOfInitializedData + " // 20");
        out.println("long   SizeOfUninitializedData=" + SizeOfUninitializedData + "     // 24");
        out.println("long   AddressOfEntryPoint=" + AddressOfEntryPoint + " // 28");
        out.println("long   BaseOfCode=" + BaseOfCode + "     // 2c");
        out.println("long   BaseOfData=" + BaseOfData + "    //    // NT additional fields. // 30");
        out.println("long   ImageBase=" + ImageBase + "     // 34");
        out.println("long   SectionAlignment=" + SectionAlignment + " // 38");
        out.println("long   FileAlignment=" + FileAlignment + "     // 3c");
        out.println("int    MajorOperatingSystemVersion=" + MajorOperatingSystemVersion + " // 40");
        out.println("int    MinorOperatingSystemVersion=" + MinorOperatingSystemVersion + "     // 42");
        out.println("int    MajorImageVersion=" + MajorImageVersion + " // 44");
        out.println("int    MinorImageVersion=" + MinorImageVersion + "     // 46");
        out.println("int    MajorSubsystemVersion=" + MajorSubsystemVersion + " // 48");
        out.println("int    MinorSubsystemVersion=" + MinorSubsystemVersion + "     // 4a");
        out.println("long   Reserved1=" + Reserved1 + "     // 4c");
        out.println("long   SizeOfImage=" + SizeOfImage + " // 50");
        out.println("long   SizeOfHeaders=" + SizeOfHeaders + "     // 54");
        out.println("long   CheckSum=" + CheckSum + "     // 58");
        out.println("int    Subsystem=" + Subsystem + " // 5c");
        out.println("int    DllCharacteristics=" + DllCharacteristics + "     // 5e");
        out.println("long   SizeOfStackReserve=" + SizeOfStackReserve + " // 60");
        out.println("long   SizeOfStackCommit=" + SizeOfStackCommit + "     // 64");
        out.println("long   SizeOfHeapReserve=" + SizeOfHeapReserve + " // 68");
        out.println("long   SizeOfHeapCommit=" + SizeOfHeapCommit + "     // 6c");
        out.println("long   LoaderFlags=" + LoaderFlags + " // 70");
        out.println("long   NumberOfRvaAndSizes=" + NumberOfRvaAndSizes + " // 74");
        out.println("long ExportDirectory_VA=" + ExportDirectory_VA + " // 78");
        out.println("long ExportDirectory_Size=" + ExportDirectory_Size + " // 7c");
        out.println("long ImportDirectory_VA=" + ImportDirectory_VA + " // 80");
        out.println("long ImportDirectory_Size=" + ImportDirectory_Size + " // 84");
        out.println("long ResourceDirectory_VA=" + ResourceDirectory_VA + " // 88");
        out.println("long ResourceDirectory_Size=" + ResourceDirectory_Size + " // 8c");
        out.println("long ExceptionDirectory_VA=" + ExceptionDirectory_VA + " // 90");
        out.println("long ExceptionDirectory_Size=" + ExceptionDirectory_Size + " // 94");
        out.println("long SecurityDirectory_VA=" + SecurityDirectory_VA + " // 98");
        out.println("long SecurityDirectory_Size=" + SecurityDirectory_Size + " // 9c");
        out.println("long BaseRelocationTable_VA=" + BaseRelocationTable_VA + " // a0");
        out.println("long BaseRelocationTable_Size=" + BaseRelocationTable_Size + " // a4");
        out.println("long DebugDirectory_VA=" + DebugDirectory_VA + " // a8");
        out.println("long DebugDirectory_Size=" + DebugDirectory_Size + " // ac");
        out.println("long ArchitectureSpecificData_VA=" + ArchitectureSpecificData_VA + " // b0");
        out.println("long ArchitectureSpecificData_Size=" + ArchitectureSpecificData_Size + " // b4");
        out.println("long RVAofGP_VA=" + RVAofGP_VA + " // b8");
        out.println("long RVAofGP_Size=" + RVAofGP_Size + " // bc");
        out.println("long TLSDirectory_VA=" + TLSDirectory_VA + " // c0");
        out.println("long TLSDirectory_Size=" + TLSDirectory_Size + " // c4");
        out.println("long LoadConfigurationDirectory_VA=" + LoadConfigurationDirectory_VA + " // c8");
        out.println("long LoadConfigurationDirectory_Size=" + LoadConfigurationDirectory_Size + " // cc");
        out.println("long BoundImportDirectoryinheaders_VA=" + BoundImportDirectoryinheaders_VA + " // d0");
        out.println("long BoundImportDirectoryinheaders_Size=" + BoundImportDirectoryinheaders_Size + " // d4");
        out.println("long ImportAddressTable_VA=" + ImportAddressTable_VA + " // d8");
        out.println("long ImportAddressTable_Size=" + ImportAddressTable_Size + " // dc");
        out.println("long DelayLoadImportDescriptors_VA=" + DelayLoadImportDescriptors_VA + " // e0");
        out.println("long DelayLoadImportDescriptors_Size=" + DelayLoadImportDescriptors_Size + " // e4");
        out.println("long COMRuntimedescriptor_VA=" + COMRuntimedescriptor_VA + " // e8");
        out.println("long COMRuntimedescriptor_Size=" + COMRuntimedescriptor_Size + " // ec");
    }

    public ByteBuffer get() {
        ByteBuffer head = ByteBuffer.allocate(16 + this.SizeOfOptionalHeader);
        head.order(ByteOrder.LITTLE_ENDIAN);
        head.position(0);
        head.putInt(17744);
        head.putShort((short) Machine);
        head.putShort((short) NumberOfSections);
        head.putInt((int) TimeDateStamp);
        head.putInt((int) PointerToSymbolTable);
        head.putInt((int) NumberOfSymbols);
        head.putShort((short) SizeOfOptionalHeader);
        head.putShort((short) Characteristics);
        head.putShort((short) Magic);
        head.put((byte) MajorLinkerVersion);
        head.put((byte) MinorLinkerVersion);
        head.putInt((int) SizeOfCode);
        head.putInt((int) SizeOfInitializedData);
        head.putInt((int) SizeOfUninitializedData);
        head.putInt((int) AddressOfEntryPoint);
        head.putInt((int) BaseOfCode);
        head.putInt((int) BaseOfData);
        head.putInt((int) ImageBase);
        head.putInt((int) SectionAlignment);
        head.putInt((int) FileAlignment);
        head.putShort((short) MajorOperatingSystemVersion);
        head.putShort((short) MinorOperatingSystemVersion);
        head.putShort((short) MajorImageVersion);
        head.putShort((short) MinorImageVersion);
        head.putShort((short) MajorSubsystemVersion);
        head.putShort((short) MinorSubsystemVersion);
        head.putInt((int) Reserved1);
        head.putInt((int) SizeOfImage);
        head.putInt((int) SizeOfHeaders);
        head.putInt((int) CheckSum);
        head.putShort((short) Subsystem);
        head.putShort((short) DllCharacteristics);
        head.putInt((int) SizeOfStackReserve);
        head.putInt((int) SizeOfStackCommit);
        head.putInt((int) SizeOfHeapReserve);
        head.putInt((int) SizeOfHeapCommit);
        head.putInt((int) LoaderFlags);
        head.putInt((int) NumberOfRvaAndSizes);
        head.putInt((int) ExportDirectory_VA);
        head.putInt((int) ExportDirectory_Size);
        head.putInt((int) ImportDirectory_VA);
        head.putInt((int) ImportDirectory_Size);
        head.putInt((int) ResourceDirectory_VA);
        head.putInt((int) ResourceDirectory_Size);
        head.putInt((int) ExceptionDirectory_VA);
        head.putInt((int) ExceptionDirectory_Size);
        head.putInt((int) SecurityDirectory_VA);
        head.putInt((int) SecurityDirectory_Size);
        head.putInt((int) BaseRelocationTable_VA);
        head.putInt((int) BaseRelocationTable_Size);
        head.putInt((int) DebugDirectory_VA);
        head.putInt((int) DebugDirectory_Size);
        head.putInt((int) ArchitectureSpecificData_VA);
        head.putInt((int) ArchitectureSpecificData_Size);
        head.putInt((int) RVAofGP_VA);
        head.putInt((int) RVAofGP_Size);
        head.putInt((int) TLSDirectory_VA);
        head.putInt((int) TLSDirectory_Size);
        head.putInt((int) LoadConfigurationDirectory_VA);
        head.putInt((int) LoadConfigurationDirectory_Size);
        head.putInt((int) BoundImportDirectoryinheaders_VA);
        head.putInt((int) BoundImportDirectoryinheaders_Size);
        head.putInt((int) ImportAddressTable_VA);
        head.putInt((int) ImportAddressTable_Size);
        head.putInt((int) DelayLoadImportDescriptors_VA);
        head.putInt((int) DelayLoadImportDescriptors_Size);
        head.putInt((int) COMRuntimedescriptor_VA);
        head.putInt((int) COMRuntimedescriptor_Size);
        head.position(0);
        return head;
    }

    public void updateVAAndSize(Vector oldsections, Vector newsections) {
        long codebase = findNewVA(this.BaseOfCode, oldsections, newsections);
        long codesize = findNewSize(this.BaseOfCode, oldsections, newsections);
        this.BaseOfCode = codebase;
        this.SizeOfCode = codesize;
        this.AddressOfEntryPoint = findNewVA(this.AddressOfEntryPoint, oldsections, newsections);
        long database = findNewVA(this.BaseOfData, oldsections, newsections);
        long datasize = findNewSize(this.BaseOfData, oldsections, newsections);
        this.BaseOfData = database;
        long imagesize = 0;
        for (int i = 0; i < newsections.size(); i++) {
            PESection sect = (PESection) newsections.get(i);
            long curmax = sect.VirtualAddress + sect.VirtualSize;
            if (curmax > imagesize) imagesize = curmax;
        }
        this.SizeOfImage = imagesize;
        ExportDirectory_Size = findNewSize(ExportDirectory_VA, oldsections, newsections);
        ExportDirectory_VA = findNewVA(ExportDirectory_VA, oldsections, newsections);
        ImportDirectory_Size = findNewSize(ImportDirectory_VA, oldsections, newsections);
        ImportDirectory_VA = findNewVA(ImportDirectory_VA, oldsections, newsections);
        ResourceDirectory_Size = findNewSize(ResourceDirectory_VA, oldsections, newsections);
        ResourceDirectory_VA = findNewVA(ResourceDirectory_VA, oldsections, newsections);
        ExceptionDirectory_Size = findNewSize(ExceptionDirectory_VA, oldsections, newsections);
        ExceptionDirectory_VA = findNewVA(ExceptionDirectory_VA, oldsections, newsections);
        SecurityDirectory_Size = findNewSize(SecurityDirectory_VA, oldsections, newsections);
        SecurityDirectory_VA = findNewVA(SecurityDirectory_VA, oldsections, newsections);
        BaseRelocationTable_Size = findNewSize(BaseRelocationTable_VA, oldsections, newsections);
        BaseRelocationTable_VA = findNewVA(BaseRelocationTable_VA, oldsections, newsections);
        DebugDirectory_Size = findNewSize(DebugDirectory_VA, oldsections, newsections);
        DebugDirectory_VA = findNewVA(DebugDirectory_VA, oldsections, newsections);
        ArchitectureSpecificData_Size = findNewSize(ArchitectureSpecificData_VA, oldsections, newsections);
        ArchitectureSpecificData_VA = findNewVA(ArchitectureSpecificData_VA, oldsections, newsections);
        RVAofGP_Size = findNewSize(RVAofGP_VA, oldsections, newsections);
        RVAofGP_VA = findNewVA(RVAofGP_VA, oldsections, newsections);
        TLSDirectory_Size = findNewSize(TLSDirectory_VA, oldsections, newsections);
        TLSDirectory_VA = findNewVA(TLSDirectory_VA, oldsections, newsections);
        LoadConfigurationDirectory_Size = findNewSize(LoadConfigurationDirectory_VA, oldsections, newsections);
        LoadConfigurationDirectory_VA = findNewVA(LoadConfigurationDirectory_VA, oldsections, newsections);
        BoundImportDirectoryinheaders_Size = findNewSize(BoundImportDirectoryinheaders_VA, oldsections, newsections);
        BoundImportDirectoryinheaders_VA = findNewVA(BoundImportDirectoryinheaders_VA, oldsections, newsections);
        ImportAddressTable_Size = findNewSize(ImportAddressTable_VA, oldsections, newsections);
        ImportAddressTable_VA = findNewVA(ImportAddressTable_VA, oldsections, newsections);
        DelayLoadImportDescriptors_Size = findNewSize(DelayLoadImportDescriptors_VA, oldsections, newsections);
        DelayLoadImportDescriptors_VA = findNewVA(DelayLoadImportDescriptors_VA, oldsections, newsections);
        COMRuntimedescriptor_Size = findNewSize(COMRuntimedescriptor_VA, oldsections, newsections);
        COMRuntimedescriptor_VA = findNewVA(COMRuntimedescriptor_VA, oldsections, newsections);
    }

    private long findNewVA(long current, Vector oldsections, Vector newsections) {
        for (int i = 0; i < oldsections.size(); i++) {
            PESection sect = (PESection) oldsections.get(i);
            if (sect.VirtualAddress == current) {
                PESection newsect = (PESection) newsections.get(i);
                return newsect.VirtualAddress;
            } else if ((current > sect.VirtualAddress) && (current < (sect.VirtualAddress + sect.VirtualSize))) {
                long diff = current - sect.VirtualAddress;
                PESection newsect = (PESection) newsections.get(i);
                return newsect.VirtualAddress + diff;
            }
        }
        return 0;
    }

    private long findNewSize(long current, Vector oldsections, Vector newsections) {
        for (int i = 0; i < oldsections.size(); i++) {
            PESection sect = (PESection) oldsections.get(i);
            if (sect.VirtualAddress == current) {
                PESection newsect = (PESection) newsections.get(i);
                return newsect.VirtualSize;
            }
        }
        return 0;
    }
}
