package Model;

class Ppu {

    /*
    FF40 - LCDC (R/W)
    bit7: LCD/PPU enable 0=off 1=on
    bit6: Window tile map area 0=9800-9BFF 1=9C00-9FFF
    bit5: Window enable 0=off 1=on
    bit4: BG and Window tile data area 0=8800-97FF 1=8000-8FFF
    bit3: BG tile map area 0=9800-9BFF, 1=9C00-9FFF
    bit2: OBJ size 0=8x8 1=8x16
    bit1: OBJ enable 0=off 1=on
    bit0: BG and Window enable/priority 0=off 1=on
     */

    byte lcdControlRegister; // 0xFF40 LCDC (LCD Control)
    byte lcdStatusRegister;  // 0xFF41 STAT
    byte scy;                // 0xFF42 Scroll Y
    byte scx;                // 0xFF43 Scroll X
    byte ly;                 // 0xFF44 LCD Y Coordinate
    byte lyc;                // 0xFF45 LYC LY Compare
    byte bgp;                // 0xFF47 BGP (BG Palette Data) Non CGB Mode Only
    byte obp0;               // 0xFF48 OBP0 (Object Palette 1 Data)
    byte obp1;               // 0xFF49 OBP1 (Object Palette 2 Data)
    byte wy;                 // 0xFF4A WY (Window Y Position)
    byte wx;                 // 0xFF4B WX (Window X Position + 7)
                             // 0xFF68 BCPS/BGPI (Background Color Palette Specification or Background Palette Index)
                             // 0xFF69 BCPS/BGPI (Background Color Palette Data or Background Palette Data)

    public Ppu() {
        lcdControlRegister = (byte)0b1000_0000;
        lcdStatusRegister = (byte)0b1000_0000;
        scy = 0;
        scx = 0;
        ly = 0;
        lyc = 0;
        bgp = 0;
        obp0 = 0;
        obp1 = 0;
        wy = 0;
        wx = 0;
    }

    public void run(int cycle) {

    }

    public void read() {
    }

    public void write() {

    }

    enum FetchMode {
        WINDOW,
        BACKGROUND,
        OBJECT,
    }

    enum PPU_MODE {
        OAM_SCAN, // MODE2
        DRAWING, // MODE3
        H_BLANK, // MODE0
        V_BLANK, // MODE1
    };

    private class BackgroundPixelFetcher {
        int windowLineCounter;
        int xPositionCounter;
    }

    private class ObjectPixelFetcher {

    }
}
