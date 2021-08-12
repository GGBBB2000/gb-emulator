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

    PPU_MODE mode;
    VRam vRam;
    Lcd lcd;

    int cycleSum;
    int lines;

    enum PPU_MODE {
        OAM_SCAN, // MODE2
        DRAWING, // MODE3
        H_BLANK, // MODE0
        V_BLANK; // MODE1
    }

    private record Pixcel(byte color, int palette, int priority, int bgPriority) {}

    private class BGPixelFetcher {
        final Ppu ppu;
        FetchMode mode;
        int tilePositionCounter;
        int tileNum;
        byte tileData;
        int x;
        int y;

        enum FetchMode {
            GET_TILE,
            GET_DATA_LOW,
            GET_DATA_HIGH,
            SLEEP,
            PUSH,
        }

        private BGPixelFetcher(Ppu ppu) {
            this.ppu = ppu;
            this.mode = FetchMode.GET_TILE;
            this.tilePositionCounter = 0;
            this.x = 0;
            this.y = 0;
            this.tileNum = 0;
        }

        public void step() {
            switch (this.mode) {
                case GET_TILE -> this.getTile();
                case GET_DATA_LOW -> this.getTileLow();
                case GET_DATA_HIGH -> this.getTileHigh();
                case PUSH -> this.pushPixelToFIFO();
                case SLEEP -> {}
            }
        }

        private void getTile() {
            this.mode = FetchMode.GET_DATA_LOW;
        }

        private void getTileLow() {
            this.mode = FetchMode.GET_DATA_HIGH;
        }

        private void getTileHigh() {
            this.mode = FetchMode.GET_DATA_HIGH;
        }

        private void pushPixelToFIFO() {
            this.mode = FetchMode.SLEEP;
        }
    }

    public Ppu(VRam vRam, Lcd lcd) {
        this.vRam = vRam;
        this.lcd = lcd;
        this.mode = PPU_MODE.OAM_SCAN;
        this.lcdControlRegister = (byte)0b1000_0000;
        this.lcdStatusRegister = (byte)0b1000_0000;
        this.scy = 0;
        this.scx = 0;
        this.ly = 0;
        this.lyc = 0;
        this.bgp = 0;
        this.obp0 = 0;
        this.obp1 = 0;
        this.wy = 0;
        this.wx = 0;
        this.cycleSum = 0;
        this.lines = 0;
    }

    public void run(int cycle) {
        this.cycleSum += cycle;
        while (cycle > 0) {
            switch (this.mode) {
                case OAM_SCAN -> { // OAM_SCAN takes 80 T-cycles for 40 sprites
                    cycle -= 2;
                    // TODO: read OAM
                    if (this.cycleSum % 456 >= 80) {
                        this.mode = PPU_MODE.DRAWING;
                    }
                }
                case DRAWING -> { // DRAWING takes 172~289 T-cycles

                }
                case H_BLANK -> {

                }
            }
        }
        this.lines = this.cycleSum / 456;
        //System.out.printf("PPU: MODE:%s cycle: %d line: %d\n", mode.toString(), this.cycle, this.line);
    }

    public void read() {
    }

    public void write() {

    }
}
