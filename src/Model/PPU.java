package Model;

public class PPU {

    /*
    FF40 - LCDC (R/W)
    bit7: LCD/PPU enable 0=off 1=on
    bit6: Window tile map area 0=9800-9BFF 1=9C00-9FFF
    bit5: Window enable 0=off 1=on
    bit4: BG and WIndow tile data area 0=8800-97FF 1=8000-8FFF
    bit3: BG tile map area 0=9800-9BFF, 1=9C00-9FFF
    bit2: OBJ size 0=8x8 1=8x16
    bit1: OBJ enable 0=off 1=on
    bit0: BG and Window enable/priority 0=off 1=on
     */
    byte lcdControlRegister = (byte)0b1000_0000; // @ 0xFF40
    byte lcdStatusRegister = (byte)0b1000_0000;

    public PPU() {

    }

    public void read() {

    }

    public void write() {

    }
}
