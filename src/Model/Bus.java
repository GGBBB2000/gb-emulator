package Model;

import Model.MBCs.Cartridge;

class Bus {
    Cartridge cartridge;
    final WRam wram;
    final VRam vram;
    final Ppu ppu;
    final JoyPad joyPad;
    final DividerRegister dividerRegister;
    final Timer timer;
    final InterruptRegister interruptRegister;
    final byte[] extVram; // 8KiB
    final byte[] attributeTable;
    final byte[] hRam;

    public Bus(VRam vRam, WRam wRam, Ppu ppu, JoyPad joyPad, DividerRegister dividerRegister, Timer timer, InterruptRegister interruptRegister) {
        this.wram = wRam;
        this.vram = vRam;
        this.ppu = ppu;
        this.dividerRegister = dividerRegister;
        this.timer = timer;
        this.interruptRegister = interruptRegister;
        this.extVram = new byte[8 * 1024]; // 16KiB
        this.attributeTable = new byte[0xA0]; // 0xFE00..0xFE9F分
        this.hRam = new byte[0x7F];
        this.joyPad = joyPad;
    }

    public void connectCartridge(final Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    public void write(final int address, final byte data) {
        if (address < 0x4000) {         // ROM bank 0
            cartridge.write(address, data);
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
            if (address == 0xFF00) {
                this.joyPad.write(address, data);
            } else if (address == 0xFF01) {
                System.out.println((char) data);
            } else if (address == 0xFF04) {
                this.dividerRegister.write(address, data);
            } else if (address <= 0xFF07) {
                this.timer.write(address, data);
            } else if (address == 0xFF0F) {
                this.interruptRegister.setInterruptRequestFlag(data);
            } else if (0xFF40 <= address && address <= 0xFF4B) {
                this.ppu.write(address, data);
            }
        } else if (address < 0xFFFF) {  // High RAM (HRAM)
            this.hRam[address - 0xFF80] = data;
        } else if (address == 0xFFFF) { // Interrupt Enable register
            this.interruptRegister.setInterruptEnable(data);
        }
    }

    public byte read(final int address) {
        byte returnVal = 0;
        if (address < 0x4000) {         // ROM bank 00
            returnVal = this.cartridge.read(address);
        } else if (address < 0x8000) {  // ROM bank 01~NN
            returnVal = this.cartridge.read(address);
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
            if (address == 0xFF00) {
                returnVal = this.joyPad.read(address);
            } else if (address == 0xFF04) {
                returnVal = this.dividerRegister.read(address);
            } else if (address <= 0xFF07) {
                returnVal = this.timer.read(address);
            } else if (address == 0xFF0F) {
                returnVal = this.interruptRegister.getInterruptRequestFlag();
            } else if (address == 0xFF46) {

            } else if (0xFF40 <= address && address <= 0xFF4A) {
                returnVal = this.ppu.read(address);
            }
        } else if (address < 0xFFFF) {  // High RAM (HRAM)
            returnVal = this.hRam[address - 0xFF80];
        } else if (address == 0xFFFF) { // Interrupt Enable register
            returnVal = this.interruptRegister.getInterruptEnable();
        }
        return returnVal;
    }
}
