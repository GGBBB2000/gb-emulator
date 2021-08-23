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
        this.lcdControl = new LcdControl();
        this.lcdStat = new LcdStat();
        this.pixelFIFO = new PixelFIFO(this.lcd);
        this.pixelFetcher = new PixelFetcher();
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
        if (!this.lcdControl.isLCDEnable()) {
            this.cycleSum = 0;
            this.mode = PPU_MODE.OAM_SCAN;
            this.ly = 0;
            this.lyc = 0;
            this.lcd.reset();
            return;
        }
        final var lineCycle = 456; // sum of cycle to render a line
        this.cycleSum += cycle;
        while (cycle > 0) {
            switch (this.mode) {
                case OAM_SCAN -> { // OAM_SCAN takes 20 T-cycles for 40 sprites
                    cycle -= 2;
                    // TODO: read OAM
                    if ((this.cycleSum % lineCycle) - cycle >= 80) {
                        this.pixelFIFO.setScrollCounter(Byte.toUnsignedInt(this.scx));
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
                        this.pixelFetcher.reset();
                        this.mode = PPU_MODE.H_BLANK;
                    }
                }
                case H_BLANK -> {
                    final int currentLine = Byte.toUnsignedInt(this.ly);
                    if (this.cycleSum / lineCycle > currentLine) {
                        if (currentLine == 143) {
                            this.mode = PPU_MODE.V_BLANK;
                        } else {
                            this.mode = PPU_MODE.OAM_SCAN;
                        }
                        this.ly++;
                    }
                    cycle = 0;
                }
                case V_BLANK -> {
                    cycle = 0;
                    this.ly = (byte)(this.cycleSum / lineCycle);
                    if (this.cycleSum >= 70224) {
                        this.cycleSum = 0;
                        this.ly = 0;
                        this.mode = PPU_MODE.OAM_SCAN;
                    }
                }
            }
        }
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
            case 0xFF46 -> (byte)0; // DMA
            case 0xFF47 -> this.bgp;
            case 0xFF48 -> this.obp0;
            case 0xFF49 -> this.obp1;
            case 0xFF4A -> this.wy;
            case 0xFF4B -> this.wx;
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
            case 0xFF46 -> {} // DMA
            case 0xFF47 -> this.bgp = data;
            case 0xFF48 -> this.obp0 = data;
            case 0xFF49 -> this.obp1 = data;
            case 0xFF4A -> this.wy = data;
            case 0xFF4B -> this.wx = data;
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

        boolean getBGDataAreaFlag() {
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
        private FetchMode mode;
        private int tileNum;
        private byte tileDataLow;
        private byte tileDataHigh;
        private int xPositionCounter;
        private final int VRAM_BASE_ADDER = 0x8000;

        enum FetchMode {
            GET_TILE,
            GET_DATA_LOW,
            GET_DATA_HIGH,
            SLEEP,
            PUSH,
        }

        private PixelFetcher() {
            this.reset();
        }

        private void reset() {
            this.mode = FetchMode.GET_TILE;
            this.xPositionCounter = 0;
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
            int tileMapBaseAdder = 0x9800;
            if (Ppu.this.lcdControl.getBGTileMapAreaFlag() /* && current line is not inside window*/) { // BG Tilemap true: 0x9C00~ false: 0x9800~
                tileMapBaseAdder = 0x9C00;
            }
            // only for rendering bg
            final int scx = Byte.toUnsignedInt(Ppu.this.scx);
            final int scy = Byte.toUnsignedInt(Ppu.this.scy);
            final int ly = Byte.toUnsignedInt(Ppu.this.ly);
            final int xIndex = ((scx) / 8 + this.xPositionCounter) & 0x1F; // this.x should be 0 ~ 31
            final int yIndex = ((scy + ly) & 0xFF) / 8;
            final int tileIndex = 32 * yIndex + xIndex;
            final int tileAdder = tileMapBaseAdder + tileIndex - this.VRAM_BASE_ADDER;
            final boolean bgDataAreaFlag = Ppu.this.lcdControl.getBGDataAreaFlag(); // true: tileNum is unsigned
            final var num = Ppu.this.vRam.read(tileAdder);
            this.tileNum = (bgDataAreaFlag) ? Byte.toUnsignedInt(num) : num;
            this.xPositionCounter++;
            this.mode = FetchMode.GET_DATA_LOW;
        }

        private void getTileLow() {
            // BG and Window data area LCDC.4: 0=8800-97FF 1=8000-8FFF
            final int ly = Byte.toUnsignedInt(Ppu.this.ly);
            final int scy = Byte.toUnsignedInt(Ppu.this.scy);
            final boolean bgDataAreaFlag = Ppu.this.lcdControl.getBGDataAreaFlag();
            final int BASE_ADDER = (bgDataAreaFlag) ? 0x8000 : 0x9000; // if LCDC.4 is 0, base adder will be 9000. otherwise 8000
            final int offset = this.tileNum * 16 + (2 * ((ly + scy) % 8));
            final int address = BASE_ADDER + offset;
            this.tileDataLow = Ppu.this.vRam.read(address - this.VRAM_BASE_ADDER);
            this.mode = FetchMode.GET_DATA_HIGH;
        }

        private void getTileHigh() {
            final int ly = Byte.toUnsignedInt(Ppu.this.ly);
            final int scy = Byte.toUnsignedInt(Ppu.this.scy);
            final boolean bgDataAreaFlag = Ppu.this.lcdControl.getBGDataAreaFlag();
            final int BASE_ADDER = (bgDataAreaFlag) ? 0x8000 : 0x9000; // if LCDC.4 is 0, base adder will be 9000. otherwise 8000
            final int offset = this.tileNum * 16 + (2 * ((ly + scy) % 8));
            final int address = BASE_ADDER + offset + 1;
            this.tileDataHigh = Ppu.this.vRam.read(address - this.VRAM_BASE_ADDER);
            this.mode = FetchMode.PUSH;
        }

        private void pushPixelToFIFO() {
            final var fifo = Ppu.this.pixelFIFO;
            if (fifo.hasEnoughSpace()) {
                for (int i = 7; i >= 0; i--) {
                    final int lowBit = (this.tileDataLow >>> i) & 1;
                    final int highBit = (this.tileDataHigh >>> (i - 1)) & 0b10;
                    final byte colorIndex = (byte)(highBit + lowBit);
                    final byte pixelData = this.mapColorIndexToColor(colorIndex);
                    final Pixcel pixcel = new Pixcel(pixelData, 0, 0, 0);
                    fifo.offerLast(pixcel);
                }
                this.mode = FetchMode.GET_TILE;
            }
        }

        private byte mapColorIndexToColor(byte index) {
            final byte[] colorArray = {(byte)255, (byte)200, (byte)100, 0};
            final int palette = Byte.toUnsignedInt(Ppu.this.bgp);
            final int paletteIndex =  ((palette >> 2 * index) & 0b0011);
            return colorArray[paletteIndex];
        }
    }

    private class PixelFIFO extends ArrayDeque<Pixcel> {
        private final Lcd lcd;
        private int pixelCounter = 0;
        private int scrollCounter = 0;

        private PixelFIFO(final Lcd lcd) {
            super(16); // FIFO has 16 pixel data
            this.lcd = lcd;
        }

        public void setScrollCounter(int scrollCounter) {
            this.scrollCounter = scrollCounter % 8;
        }

        private int getPixelCounter() {
            return pixelCounter;
        }

        private void pushPixelsToLCD() {
            Pixcel pixel = this.poll();
            if (pixel != null && this.pixelCounter < 160) {
                if (this.scrollCounter == 0) {
                    this.lcd.draw(pixel.color);
                    this.pixelCounter++;
                } else {
                    this.scrollCounter--;
                }
            }
        }

        private boolean hasEnoughSpace() {
            return this.size() <= 8;
        }

        @Override
        public void clear() {
            super.clear();
            this.pixelCounter = 0;
        }
    }
}
