package devices;

import org.jetbrains.annotations.NotNull;

public class Bus {
    Byte[][] romBank; // TODO: impl MBC
    Byte[] wram;
    Byte[] vram;
    Byte[] extVram; // 8KiB
    Byte[] attributeTable;

    public Bus(@NotNull final Cartridge cartridge) {
        romBank = new Byte[2][16 * 1024]; // impl MBC
        wram = new Byte[8 * 1024]; // 8KiB
        vram = new Byte[16 * 1024]; // 16KiB
        extVram = new Byte[8 * 1024]; // 16KiB
        attributeTable = new Byte[0xA0]; // 0xFE00..0xFE9F分
    }

    public void write(final int addr, final Byte data) {
        if (addr < 0x4000) {         // ROM bank 0
            romBank[0][addr] = data;
        } else if (addr < 0x8000) {  // ROM bank 01~NN
            romBank[1][addr - 0x4000] = data;
        } else if (addr < 0xA000) {  // VRAM
            vram[addr - 0x8000] = data;
        } else if (addr < 0xC000) {  // External RAM
            extVram[addr - 0xA000] = data;
        } else if (addr < 0xD000) {  // WRAM
            wram[addr - 0xC000] = data;
        } else if (addr < 0xE000) {  // WRAM
            wram[addr - 0xC000] = data;
        } else if (addr < 0xFE00) {  // ECHO RAM
            // prohibited
        } else if (addr < 0xFEA0) {  // Sprite attribute table(OAM)
            attributeTable[addr] = data;
        } else if (addr < 0xFF00) {  // Not Usable
            // do nothing
        } else if (addr < 0xFF80) {  // I/O Registers

        } else if (addr < 0xFFFF) {  // High RAM (HRAM)
            // ?
        } else if (addr == 0xFFFF) { // Interrupt Enable register

        }
    }

    public Byte read(int addr) {
        Byte returnVal = 0;
        if (addr < 0x4000) {         // ROM bank 00
            returnVal = romBank[0][addr];
        } else if (addr < 0x8000) {  // ROM bank 01~NN
            returnVal = romBank[1][addr - 0x4000];
        } else if (addr < 0xA000) {  // VRAM
            returnVal = vram[addr - 0x8000];
        } else if (addr < 0xC000) {  // External RAM
            returnVal = extVram[addr - 0xA000];
        } else if (addr < 0xD000) {  // WRAM
            returnVal = wram[addr - 0xC000];
        } else if (addr < 0xE000) {  // WRAM
            returnVal = wram[addr - 0xC000];
        } else if (addr < 0xFE00) {  // ECHO RAM
            returnVal = wram[addr - 0x2000 - 0xC000];
        } else if (addr < 0xFEA0) {  // Sprite attribute table(OAM)
            returnVal = attributeTable[addr - 0xFE00];
        } else if (addr < 0xFF00) {  // Not Usable
            // do nothing
        } else if (addr < 0xFF80) {  // I/O Registers

        } else if (addr < 0xFFFF) {  // High RAM (HRAM)

        } else if (addr == 0xFFFF) { // Interrupt Enable register

        }
        return returnVal;
    }
}
