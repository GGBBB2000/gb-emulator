package Model;

import java.util.ArrayDeque;

class Ppu {
    final LcdControl lcdControl;
    final LcdStat lcdStat;  // 0xFF41 STAT
    byte scy;                // 0xFF42 Scroll Y
    byte scx;                // 0xFF43 Scroll X
    byte ly;                 // 0xFF44 LCD Y Coordinate
    byte lyc;                // 0xFF45 LYC LY Compare
    byte bgp;                // 0xFF47 BGP (BG Palette Data) Non CGB Mode Only (0:White 1: Light gray 2: DarkGray 4: Black)
    byte obp0;               // 0xFF48 OBP0 (Object Palette 1 Data)
    byte obp1;               // 0xFF49 OBP1 (Object Palette 2 Data)
    byte wy;                 // 0xFF4A WY (Window Y Position)
    byte wx;                 // 0xFF4B WX (Window X Position + 7)
    // 0xFF68 BCPS/BGPI (Background Color Palette Specification or Background Palette Index)
    // 0xFF69 BCPS/BGPI (Background Color Palette Data or Background Palette Data)
    final PixelFetcher pixelFetcher;
    final PixelFIFO pixelFIFO;

    PPU_MODE mode;
    VRam vRam;
    Lcd lcd;

    int cycleSum;

    enum PPU_MODE {
        OAM_SCAN((byte) 2), // MODE2
        DRAWING((byte) 3), // MODE3
        H_BLANK((byte) 0), // MODE0
        V_BLANK((byte) 1); // MODE1

        final byte modeIndex;
        PPU_MODE(byte i) {
            this.modeIndex = i;
        }
    }

    public Ppu(VRam vRam, Lcd lcd) {
        this.vRam = vRam;
        this.lcd = lcd;
        this.mode = PPU_MODE.OAM_SCAN;
        this.pixelFIFO = new PixelFIFO(this.lcd);
        this.pixelFetcher = new PixelFetcher(this.pixelFIFO);
        this.lcdControl = new LcdControl();
        this.lcdStat = new LcdStat();
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
    }

    public void run(int cycle) {
        final var lineCycle = 114; // sum of cycle to render a line
        this.cycleSum += cycle;
        while (cycle > 0) {
            switch (this.mode) {
                case OAM_SCAN -> { // OAM_SCAN takes 20 T-cycles for 40 sprites
                    cycle -= 2;
                    // TODO: read OAM
                    if ((this.cycleSum % lineCycle) - cycle >= 20) {
                        this.mode = PPU_MODE.DRAWING;
                    }
                }
                case DRAWING -> { // DRAWING takes 43-72 T-cycles
                    cycle -= 2;
                    this.pixelFetcher.step();
                    this.pixelFIFO.pushPixelsToLCD();
                    this.pixelFIFO.pushPixelsToLCD();
                    if (this.pixelFIFO.getPixelCounter() >= 160) {
                        this.pixelFIFO.clear();
                        this.mode = PPU_MODE.H_BLANK;
                    }
                }
                case H_BLANK -> {
                    cycle = lineCycle - ((this.cycleSum % lineCycle) - cycle);
                    if (Byte.toUnsignedInt(this.ly) == 143) {
                        this.mode = PPU_MODE.V_BLANK;
                    } else {
                        this.ly++;
                        this.mode = PPU_MODE.OAM_SCAN;
                    }
                }
                case V_BLANK -> {
                    if (this.cycleSum >= 1140) {
                        this.cycleSum = 0;
                        this.ly = 0;
                        this.mode = PPU_MODE.OAM_SCAN;
                    }
                }
            }
        }
        this.ly = (byte)(this.cycleSum / lineCycle);
        //System.out.printf("PPU: MODE:%s cycle: %d line: %d\n", mode.toString(), this.cycle, this.line);
    }

    public byte read(final int address) {
        return switch (address) {
            case 0xFF40 -> this.lcdControl.getRegister();
            case 0xFF41 -> this.lcdStat.getRegister();
            case 0xFF42 -> this.scy;
            case 0xFF43 -> this.scx;
            case 0xFF44 -> this.ly;
            case 0xFF45 -> this.lyc;
            case 0xFF46 -> this.bgp;
            case 0xFF47 -> this.obp0;
            case 0xFF48 -> this.obp1;
            case 0xFF49 -> this.wy;
            case 0xFF4A -> this.wx;
            default -> throw new IllegalArgumentException("");
        };
    }

    public void write(final int address, final byte data) {
        switch (address) {
            case 0xFF40 -> this.lcdControl.setRegister(data);
            case 0xFF41 -> this.lcdStat.setRegister(data);
            case 0xFF42 -> this.scy = data;
            case 0xFF43 -> this.scx = data;
            case 0xFF44 -> this.ly = data;
            case 0xFF45 -> this.lyc = data;
            case 0xFF46 -> this.bgp = data;
            case 0xFF47 -> this.obp0 = data;
            case 0xFF48 -> this.obp1 = data;
            case 0xFF49 -> this.wy = data;
            case 0xFF4A -> this.wx = data;
            default -> throw new IllegalArgumentException("");
        }
    }

    private final class LcdControl { // 0xFF40 LCDC (LCD Control)
        /*
        bit7: LCD/PPU enable 0=off 1=on
        bit6: Window tile map area 0=9800-9BFF 1=9C00-9FFF
        bit5: Window enable 0=off 1=on
        bit4: BG and Window tile data area 0=8800-97FF 1=8000-8FFF
        bit3: BG tile map area 0=9800-9BFF, 1=9C00-9FFF
        bit2: OBJ size 0=8x8 1=8x16
        bit1: OBJ enable 0=off 1=on
        bit0: BG and Window enable/priority 0=off 1=on
        */

        byte register;

        LcdControl() {
            this.register = (byte)0b1000_0000;
        }

        void setRegister(byte register) {
            this.register = register;
        }

        byte getRegister() {
            return register;
        }

        boolean isLCDEnable() {
            return ((this.register & 0b1000_0000) >> 7) == 1;
        }

        boolean getWindowTileMapFlag() {
            return ((this.register & 0b0100_0000) >> 6) == 1;
        }

        boolean getWindowEnable() {
            return ((this.register & 0b0010_0000) >> 5) == 1;
        }

        boolean getBGWindowTileDataAreaFlag() {
            return ((this.register & 0b0001_0000) >> 4) == 1;
        }

        boolean getBGTileMapAreaFlag() {
            return ((this.register & 0b0000_1000) >> 3) == 1;
        }

        boolean isOBJIsSquare() {
            return ((this.register & 0b0000_0100) >> 2) == 1;
        }

        boolean isOBJEnable() {
            return ((register & 0b0000_0010) >> 1) == 1;
        }

        boolean isBGAndWindowEnable() {
            return (this.register & 0b0000_0001) == 1;
        }
    }

    private final class LcdStat {
        /*
        0xFF41 STAT
        bit6: LYC=LY STAT Interrupt source 1=Enable
        bit5: Mode OAM STAT Interrupt source
        bit4: Mode 1 VBlank STAT Interrupt source
        bit3: Mode 0 HBlank STAT Interrupt source
        bit2: LYC=LY Flag
        bit1-0: ModeFlag
        00: In HBlank
        01: In VBlank
        10: Searching OAM
        11: Transferring Data to LCD Controller
        */
        byte register;
        PPU_MODE mode;
        boolean isEqualLyLyc;

        LcdStat() {
            this.register = (byte)0b1000_0000;
            this.mode = Ppu.this.mode;
            this.isEqualLyLyc = Ppu.this.ly == Ppu.this.lyc;
        }

        void setRegister(final byte data) {
            this.register = data;
        }

        byte getRegister() {
            return register;
        }

        boolean getLYC_LY_STAT_Interrupt() {
            return ((this.register & 0b0100_0000) >> 6) == 1;
        }

        boolean getMode2_OAM_STAT_Interrupt() {
            return ((this.register & 0b0010_0000) >> 5) == 1;
        }

        boolean getMode1_VBLANK_STAT_Interrupt() {
            return ((this.register & 0b0001_0000) >> 4) == 1;
        }

        boolean getMode0_HBLANK_STAT_Interrupt() {
            return ((this.register & 0b0000_1000) >> 3) == 1;
        }

        boolean isEqualLyLyc() {
            return ((this.register & 0b0000_0100) >> 2) == 1;
        }

        void setEqualLyLyc(boolean isLyLycEqual) {
            this.isEqualLyLyc = isLyLycEqual;
            this.register &= 0b1111_1011;
            this.register |= (isLyLycEqual) ? 0b0000_0100 : 0;
        }

        void setMode(PPU_MODE mode) {
            this.mode = mode;
            this.register &= 0b1111_1100;
            this.register |= this.mode.modeIndex;
        }
    }

    private record Pixcel(byte color, int palette, int priority, int bgPriority) { }

    private class PixelFetcher {
        private final PixelFIFO fifo;
        private FetchMode mode;
        private int tilePositionCounter;
        private int tileNum;
        private byte tileData;
        private int x;
        private int y;
        private Pixcel pixcel;

        enum FetchMode {
            GET_TILE,
            GET_DATA_LOW,
            GET_DATA_HIGH,
            SLEEP,
            PUSH,
        }

        private PixelFetcher(PixelFIFO fifo) {
            this.fifo = fifo;
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
                case SLEEP -> {
                }
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

        private boolean pixelDataIsReady() {
            return this.pixcel != null;
        }
    }

    private class PixelFIFO extends ArrayDeque<Pixcel> {
        private final Lcd lcd;
        private int pixelCounter = 0;

        private PixelFIFO(final Lcd lcd) {
            super(16); // FIFO has 16 pixel data
            this.lcd = lcd;
        }

        private int getPixelCounter() {
            return pixelCounter;
        }

        private void pushPixelsToLCD() {
            final var pixel = this.poll();
            if (pixel != null) {
                this.lcd.draw(pixel.color);
                this.pixelCounter++;
            }
        }

        private boolean hasEnoughSpace() {
            return this.size() <= 8;
        }
    }
}
