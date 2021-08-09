package Model;

class Bus {
    Cartridge cartridge;
    final WRam wram;
    final VRam vram;
    final byte[] extVram; // 8KiB
    final byte[] attributeTable;

    public Bus(VRam vRam, WRam wRam) {
        this.wram = wRam;
        this.vram = vRam;
        this.extVram = new byte[8 * 1024]; // 16KiB
        this.attributeTable = new byte[0xA0]; // 0xFE00..0xFE9F分
    }

    public void connectCartridge(final Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    public void write(final int address, final byte data) {
        if (address < 0x4000) {         // ROM bank 0
            //cartridge[address] = data;
        } else if (address < 0x8000) {  // ROM bank 01~NN
            //cartridge[address - 0x4000] = data;
        } else if (address < 0xA000) {  // VRAM
            this.vram.write(address - 0x8000, data);
        } else if (address < 0xC000) {  // External RAM
            extVram[address - 0xA000] = data;
        } else if (address < 0xD000) {  // WRAM
            this.wram.write(address - 0xC000, data);
        } else if (address < 0xE000) {  // WRAM
            this.wram.write(address - 0xC000, data);
        } else if (address < 0xFE00) {  // ECHO RAM
            // prohibited
        } else if (address < 0xFEA0) {  // Sprite attribute table(OAM)
            attributeTable[address - 0xFE00] = data;
        } else if (address < 0xFF00) {  // Not Usable
            // do nothing
        } else if (address < 0xFF80) {  // I/O Registers

        } else if (address < 0xFFFF) {  // High RAM (HRAM)
            // ?
        } else if (address == 0xFFFF) { // Interrupt Enable register

        }
    }

    public byte read(final int address) {
        byte returnVal = 0;
        if (address < 0x4000) {         // ROM bank 00
            returnVal = this.cartridge.read(address);
        } else if (address < 0x8000) {  // ROM bank 01~NN
            returnVal = this.cartridge.read(address - 0x4000);
        } else if (address < 0xA000) {  // VRAM
            returnVal = this.vram.read(address - 0x8000);
        } else if (address < 0xC000) {  // External RAM
            returnVal = extVram[address - 0xA000];
        } else if (address < 0xD000) {  // WRAM
            returnVal = this.wram.read(address - 0xC000);
        } else if (address < 0xE000) {  // WRAM
            returnVal = this.wram.read(address - 0xC000);
        } else if (address < 0xFE00) {  // ECHO RAM
            returnVal = this.wram.read(address - 0x2000 - 0xC000);
        } else if (address < 0xFEA0) {  // Sprite attribute table(OAM)
            returnVal = attributeTable[address - 0xFE00];
        } else if (address < 0xFF00) {  // Not Usable
            // do nothing
        } else if (address < 0xFF80) {  // I/O Registers

        } else if (address < 0xFFFF) {  // High RAM (HRAM)

        } else if (address == 0xFFFF) { // Interrupt Enable register

        }
        return returnVal;
    }

    public byte[] getAttributeTable() {
        return attributeTable;
    }
}
