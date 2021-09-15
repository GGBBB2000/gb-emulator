package Model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

class Ppu implements IODevice {
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
    final InterruptRegister interruptRegister;
    final BG_Fetcher bgFetcher;
    final SpriteFetcher spriteFetcher;
    final PixelFIFO pixelFIFO;
    final ObjectAttributeTable oamTable;
    final SpriteBuffer spriteBuffer;

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

    public Ppu(VRam vRam, Lcd lcd, InterruptRegister interruptRegister) {
        this.vRam = vRam;
        this.lcd = lcd;
        this.mode = PPU_MODE.OAM_SCAN;
        this.lcdControl = new LcdControl();
        this.lcdStat = new LcdStat();
        this.pixelFIFO = new PixelFIFO(this.lcd);
        this.bgFetcher = new BG_Fetcher();
        this.oamTable = new ObjectAttributeTable();
        this.spriteBuffer = new SpriteBuffer(this.oamTable);
        this.spriteFetcher = new SpriteFetcher(this.spriteBuffer);
        this.scy = 0;
        this.scx = 0;
        this.ly = 0;
        this.lyc = 0;
        this.bgp = 0;
        this.obp0 = 0;
        this.obp1 = 0;
        this.wy = 0;
        this.wx = 0;
        this.interruptRegister = interruptRegister;
        this.cycleSum = 0;
    }

    public void run(int cycle) {
        final var lineCycle = 456; // sum of cycle to render a line
        this.cycleSum += cycle;
        while (cycle > 0) {
            switch (this.mode) {
                case OAM_SCAN -> { // OAM_SCAN takes 20 T-cycles for 40 sprites
                    cycle -= 2;
                    final var ly = Byte.toUnsignedInt(this.ly);
                    final var bigSpriteMode = !this.lcdControl.isOBJ_Square();
                    this.spriteBuffer.loadSprite(ly, bigSpriteMode);
                    if ((this.cycleSum % lineCycle) - cycle >= 80) {
                        this.pixelFIFO.setScrollCounter(Byte.toUnsignedInt(this.scx));
                        this.mode = PPU_MODE.DRAWING;
                    }
                }
                case DRAWING -> { // DRAWING takes 43-72 T-cycles
                    cycle -= 2;
                    final var pixelCount = this.pixelFIFO.getPixelCount();
                    if (this.lcdControl.isOBJEnable() && this.spriteBuffer.hasSprite() && this.spriteBuffer.hasSpriteAt(pixelCount)) {
                        this.spriteFetcher.step();
                    } else {
                        this.bgFetcher.step();
                        this.pixelFIFO.pushPixelsToLCD();
                        this.pixelFIFO.pushPixelsToLCD();
                        final var wx = Byte.toUnsignedInt(this.wx);
                        final var wy = Byte.toUnsignedInt(this.wy);
                        final var ly = Byte.toUnsignedInt(this.ly);
                        if (this.lcdControl.getWindowEnable() && (wx - 7) <= pixelCount && wy <= ly && bgFetcher.isFetchingBG()) {
                            this.pixelFIFO.clear();
                            this.bgFetcher.reset();
                            this.bgFetcher.setWindowFetchingMode(true);
                        }
                    }
                    if (pixelCount >= 160) {
                        this.pixelFIFO.reset();
                        this.bgFetcher.reset();
                        this.spriteBuffer.reset();
                        this.mode = PPU_MODE.H_BLANK;
                        if (this.lcdControl.isLCDEnable() && this.lcdStat.isHBLANK_InterruptMode()) {
                            this.interruptRegister.setLCD_STAT_Interrupt(true);
                        }
                    }
                }
                case H_BLANK -> {
                    final int currentLine = Byte.toUnsignedInt(this.ly);
                    if (this.cycleSum / lineCycle > currentLine) {
                        if (currentLine == 143) {
                            this.bgFetcher.resetWindowLineCounter();
                            this.interruptRegister.setVBlankInterrupt(true);
                            this.mode = PPU_MODE.V_BLANK;
                            if (this.lcdControl.isLCDEnable() && this.lcdStat.isVBLANK_InterruptMode()) {
                                this.interruptRegister.setLCD_STAT_Interrupt(true);
                            }
                        } else {
                            this.bgFetcher.incrementWindowLineCounter();
                            this.mode = PPU_MODE.OAM_SCAN;
                            if (this.lcdControl.isLCDEnable() && this.lcdStat.isOAM_InterruptMode()) {
                                this.interruptRegister.setLCD_STAT_Interrupt(true);
                            }
                        }
                        this.bgFetcher.setWindowFetchingMode(false);
                        this.ly++;
                        this.lcdStat.setEqualLyLyc(this.ly == this.lyc);
                        if (this.lcdControl.isLCDEnable() && this.lcdStat.isLYC_InterruptMode() && this.lcdStat.isEqualLyLyc()) {
                            this.interruptRegister.setLCD_STAT_Interrupt(true);
                        }
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
                        if (this.lcdStat.isOAM_InterruptMode()) {
                            this.interruptRegister.setLCD_STAT_Interrupt(true);
                        }
                    }
                }
            }
            this.lcdStat.setMode(this.mode);
        }
        //System.out.printf("PPU: MODE:%s \n", mode.toString());
    }

    @Override
    public byte read(final int address) {
        if (0xFE00 <= address && address <= 0xFE9F) {
            return this.oamTable.read(address);
        }
        return switch (address) {
            case 0xFF40 -> this.lcdControl.getRegister();
            case 0xFF41 -> this.lcdStat.getRegister();
            case 0xFF42 -> this.scy;
            case 0xFF43 -> this.scx;
            case 0xFF44 -> this.ly;
            case 0xFF45 -> this.lyc;
            case 0xFF47 -> this.bgp;
            case 0xFF48 -> this.obp0;
            case 0xFF49 -> this.obp1;
            case 0xFF4A -> this.wy;
            case 0xFF4B -> this.wx;
            default -> throw new IllegalArgumentException("");
        };
    }

    @Override
    public void write(final int address, final byte data) {
        if (0xFE00 <= address && address <= 0xFE9F) {
            this.oamTable.write(address, data);
            return;
        }
        switch (address) {
            case 0xFF40 -> this.lcdControl.setRegister(data);
            case 0xFF41 -> this.lcdStat.setRegister(data);
            case 0xFF42 -> this.scy = data;
            case 0xFF43 -> this.scx = data;
            case 0xFF44 -> this.ly = data;
            case 0xFF45 -> this.lyc = data;
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

        boolean getWindowTileMapAreaFlag() {
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

        boolean isOBJ_Square() {
            return ((this.register & 0b0000_0100) >> 2) == 0;
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

        boolean isLYC_InterruptMode() {
            return ((this.register & 0b0100_0000) >> 6) == 1;
        }

        boolean isOAM_InterruptMode() {
            return ((this.register & 0b0010_0000) >> 5) == 1;
        }

        boolean isVBLANK_InterruptMode() {
            return ((this.register & 0b0001_0000) >> 4) == 1;
        }

        boolean isHBLANK_InterruptMode() {
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

    /*
     Bit7   BG and Window over OBJ (0=No, 1=BG and Window colors 1-3 over the OBJ)
     Bit6   Y flip          (0=Normal, 1=Vertically mirrored)
     Bit5   X flip          (0=Normal, 1=Horizontally mirrored)
     Bit4   Palette number  **Non CGB Mode Only** (0=OBP0, 1=OBP1)
     Bit3   Tile VRAM-Bank  **CGB Mode Only**     (0=Bank 0, 1=Bank 1)
     Bit2-0 Palette number  **CGB Mode Only**     (OBP0-7)
     */
    private record ObjectAttribute(int y, int x, int tileIndex, boolean bgOverObj, boolean yFlip, boolean xFlip,
                                   int paletteNum, int tileBank, int colorPaletteNum) {
    }

    private class SpriteBuffer {
        final ArrayList<ObjectAttribute> buffer;
        final ObjectAttributeTable table;
        int tableIndex = 0;

        SpriteBuffer(ObjectAttributeTable table) {
            this.buffer = new ArrayList<>();
            this.table = table;
        }

        public void reset() {
            this.tableIndex = 0;
            this.buffer.clear();
        }

        public void loadSprite(int ly, boolean bigSpriteMode) {
            if (this.buffer.size() < 10) { // buffer can hold up to 10 info in one line
                final var spriteInfo = table.at(tableIndex);
                final var x = spriteInfo.x;
                final var y = spriteInfo.y;
                final var spriteHeight = (bigSpriteMode) ? 16 : 8;
                if (x > 0 && ly + 16 >= y && ly + 16 < y + spriteHeight) {
                    this.buffer.add(spriteInfo);
                }
            }
            tableIndex++;
        }

        public boolean hasSprite() {
            return this.buffer.size() > 0;
        }

        public boolean hasSpriteAt(int x) {
            for (final var sprite : this.buffer) {
                final var spriteFound = sprite.x() - 8 <= x;
                if (spriteFound) {
                    return true;
                }
            }
            return false;
        }

        public ObjectAttribute getSpriteAt(int x) {
            for (final var sprite : this.buffer) {
                final var spriteFound = sprite.x() - 8 <= x;
                if (spriteFound) {
                    return sprite;
                }
            }
            return null; // program will not reach here!

        }

        public void removeSprite(ObjectAttribute o) {
            this.buffer.remove(o);
        }
    }

    private class ObjectAttributeTable {
        final byte[] attributes;
        final int BASE_ADDRESS = 0xFE00;

        ObjectAttributeTable() {
            this.attributes = new byte[40 * 4]; // 4 bytes by each sprite
        }

        private void write(int address, byte data) {
            this.attributes[address - BASE_ADDRESS] = data;
        }

        private byte read(int address) {
            return this.attributes[address - BASE_ADDRESS];
        }

        private ObjectAttribute at(int index) {
            index *= 4;
            final int y = Byte.toUnsignedInt(this.attributes[index]);
            final int x = Byte.toUnsignedInt(this.attributes[index + 1]);
            final int tileIndex = Byte.toUnsignedInt(this.attributes[index + 2]);
            final int flags = Byte.toUnsignedInt(this.attributes[index + 3]);
            final boolean bgOverObj = (flags & 0b1000_0000 >> 7) == 1;
            final boolean yFlip = (flags & 0b0100_0000 >> 6) == 1;
            final boolean xFlip = (flags & 0b0010_0000 >> 5) == 1;
            final int paletteNum = flags & 0b0001_0000 >> 4;
            final int tileBank = flags & 0b0000_1000 >> 3;
            final int colorPaletteNum = flags & 0b11;
            return new ObjectAttribute(y, x, tileIndex, bgOverObj, yFlip, xFlip, paletteNum, tileBank, colorPaletteNum);
        }
    }

    private record Pixel(byte color, int palette, int priority, boolean bgOverObj) {
    }

    private abstract class PixelFetcher {
        State state;
        int tileNum;
        byte tileDataLow;
        byte tileDataHigh;
        final int VRAM_BASE_ADDER = 0x8000;

        enum State {
            GET_TILE,
            GET_DATA_LOW,
            GET_DATA_HIGH,
            SLEEP,
            PUSH,
        }

        abstract void reset();

        public void step() {
            switch (this.state) {
                case GET_TILE -> this.getTile();
                case GET_DATA_LOW -> this.getTileLow();
                case GET_DATA_HIGH -> this.getTileHigh();
                case PUSH -> this.pushPixelToFIFO();
                case SLEEP -> {
                }
            }
        }

        abstract void getTile();

        abstract void getTileLow();

        abstract void getTileHigh();

        abstract void pushPixelToFIFO();

        abstract byte mapColorIndexToColor(byte index);
    }

    private class SpriteFetcher extends PixelFetcher {
        final SpriteBuffer spriteBuffer;
        ObjectAttribute attribute;

        SpriteFetcher(SpriteBuffer spriteBuffer) {
            this.spriteBuffer = spriteBuffer;
            reset();
        }

        @Override
        void reset() {
            this.state = State.GET_TILE;
        }

        @Override
        void getTile() {
            final var x = Ppu.this.pixelFIFO.getPixelCount();
            this.attribute = this.spriteBuffer.getSpriteAt(x);
            assert this.attribute != null;
            this.tileNum = this.attribute.tileIndex;
            this.state = State.GET_DATA_LOW;
        }

        @Override
        void getTileLow() {
            final var ram = Ppu.this.vRam;
            final var y = this.attribute.y;
            final var ly = Byte.toUnsignedInt(Ppu.this.ly);
            final var isOBJ_Square = Ppu.this.lcdControl.isOBJ_Square();
            final var yOffset = 16 - Math.abs(y - ly);
            var tileNum = this.tileNum;
            if (isOBJ_Square && yOffset >= 8) {
                tileNum++;
            }
            this.tileDataLow = ram.read(tileNum * 16 + 2 * yOffset);
            this.state = State.GET_DATA_HIGH;
        }

        @Override
        void getTileHigh() {
            final var ram = Ppu.this.vRam;
            final var y = this.attribute.y;
            final var ly = Byte.toUnsignedInt(Ppu.this.ly);
            final var isOBJ_Square = Ppu.this.lcdControl.isOBJ_Square();
            final var yOffset = 16 - Math.abs(y - ly);
            var tileNum = this.tileNum;
            if (isOBJ_Square && yOffset >= 8) {
                tileNum++;
            }
            this.tileDataHigh = ram.read(tileNum * 16 + 2 * yOffset + 1);
            this.state = State.PUSH;
        }

        @Override
        void pushPixelToFIFO() {
            final var fifo = Ppu.this.pixelFIFO;
            final var pixels = new ArrayList<Pixel>();
            for (int i = 7; i >= 0; i--) {
                final int lowBit = (this.tileDataLow >>> i) & 1;
                final int highBit = (this.tileDataHigh >>> i) & 1;
                final byte colorIndex = (byte) ((highBit << 1) | lowBit);
                final byte pixelData = this.mapColorIndexToColor(colorIndex);
                final Pixel pixel = new Pixel(pixelData, 0, 0, attribute.bgOverObj);
                pixels.add(pixel);
            }
            if (this.attribute.xFlip) {
                Collections.reverse(pixels);
            }
            fifo.setSpritePixelBuffer(pixels);
            this.spriteBuffer.removeSprite(this.attribute);
            this.state = State.GET_TILE;
        }

        @Override
        byte mapColorIndexToColor(byte index) {
            final byte[] colorArray = {(byte) 255, (byte) 200, (byte) 100, (byte) 0};
            final byte palette = (this.attribute.paletteNum == 0) ? Ppu.this.obp0 : Ppu.this.obp1;
            final int paletteIndex = ((palette >> 2 * index) & 0b0011);
            return colorArray[paletteIndex];
        }
    }

    private class BG_Fetcher extends PixelFetcher {
        private boolean isFetchingBG;
        private int xPositionCounter;
        private int windowLineCounter;

        private BG_Fetcher() {
            this.reset();
            this.resetWindowLineCounter();
        }

        private boolean isFetchingBG() {
            return this.isFetchingBG;
        }

        @Override
        void reset() {
            this.state = State.GET_TILE;
            this.xPositionCounter = 0;
            this.tileNum = 0;
        }

        private void resetWindowLineCounter() {
            if (Ppu.this.lcdControl.getWindowEnable() && !this.isFetchingBG) {
                this.windowLineCounter = 0;
            }
        }

        private void incrementWindowLineCounter() {
            if (Ppu.this.lcdControl.getWindowEnable() && !this.isFetchingBG) {
                this.windowLineCounter++;
            }
        }

        public void setWindowFetchingMode(boolean isFetchingWin) {
            this.isFetchingBG = !isFetchingWin;
        }

        /* render background or window*/
        @Override
        void getTile() {
            int tileMapBaseAdder = 0x9800;
            if (this.isFetchingBG) {
                if (Ppu.this.lcdControl.getBGTileMapAreaFlag()) { // lcdc: bit6 BG Tilemap true: 0x9C00~ false: 0x9800~
                    tileMapBaseAdder = 0x9C00;
                }
            } else {
                if (Ppu.this.lcdControl.getWindowTileMapAreaFlag()) { // lcdc: bit3
                    tileMapBaseAdder = 0x9C00;
                }
            }
            // only for rendering bg
            final int scx = Byte.toUnsignedInt(Ppu.this.scx);
            final int scy = Byte.toUnsignedInt(Ppu.this.scy);
            final int ly = Byte.toUnsignedInt(Ppu.this.ly);
            final int xIndex = (this.isFetchingBG) ? (((scx) / 8 + this.xPositionCounter) & 0x1F) : (this.xPositionCounter & 0x1F); // this.x should be 0 ~ 31
            final int yIndex = (this.isFetchingBG) ? (((scy + ly) & 0xFF) / 8) : (windowLineCounter / 8);
            final int tileIndex = 32 * yIndex + xIndex;
            final int tileAdder = tileMapBaseAdder + tileIndex;
            final boolean bgDataAreaFlag = Ppu.this.lcdControl.getBGDataAreaFlag(); // true: tileNum is unsigned
            final var num = Ppu.this.vRam.read(tileAdder - this.VRAM_BASE_ADDER);
            this.tileNum = (bgDataAreaFlag) ? Byte.toUnsignedInt(num) : num;
            this.xPositionCounter++;
            this.state = State.GET_DATA_LOW;
        }

        @Override
        void getTileLow() {
            // BG and Window data area LCDC.4: 0=8800-97FF 1=8000-8FFF
            final int ly = Byte.toUnsignedInt(Ppu.this.ly);
            final int scy = Byte.toUnsignedInt(Ppu.this.scy);
            final boolean bgDataAreaFlag = Ppu.this.lcdControl.getBGDataAreaFlag();
            final int BASE_ADDER = (bgDataAreaFlag) ? 0x8000 : 0x9000; // if LCDC.4 is 0, base adder will be 9000. otherwise 8000
            final int offset = this.tileNum * 16 + ((this.isFetchingBG) ? ((2 * ((ly + scy) % 8)))
                    : (2 * (windowLineCounter % 8)));
            final int address = BASE_ADDER + offset;
            this.tileDataLow = Ppu.this.vRam.read(address - this.VRAM_BASE_ADDER);
            this.state = State.GET_DATA_HIGH;
        }

        @Override
        void getTileHigh() {
            final int ly = Byte.toUnsignedInt(Ppu.this.ly);
            final int scy = Byte.toUnsignedInt(Ppu.this.scy);
            final boolean bgDataAreaFlag = Ppu.this.lcdControl.getBGDataAreaFlag();
            final int BASE_ADDER = (bgDataAreaFlag) ? 0x8000 : 0x9000; // if LCDC.4 is 0, base adder will be 9000. otherwise 8000
            final int offset = this.tileNum * 16 + ((this.isFetchingBG) ? ((2 * ((ly + scy) % 8)))
                    : (2 * (windowLineCounter % 8)));
            final int address = BASE_ADDER + offset + 1;
            this.tileDataHigh = Ppu.this.vRam.read(address - this.VRAM_BASE_ADDER);
            this.state = State.PUSH;
        }

        @Override
        void pushPixelToFIFO() {
            final var fifo = Ppu.this.pixelFIFO;
            if (fifo.hasEnoughSpace()) {
                for (int i = 7; i >= 0; i--) {
                    final int lowBit = (this.tileDataLow >>> i) & 1;
                    final int highBit = (this.tileDataHigh >>> i) & 1;
                    final byte colorIndex = (byte) ((highBit << 1) | lowBit);
                    final byte pixelData = this.mapColorIndexToColor(colorIndex);
                    final Pixel pixel = new Pixel(pixelData, 0, 0, false);
                    fifo.offerLast(pixel);
                }
                this.state = State.GET_TILE;
            }
        }

        @Override
        byte mapColorIndexToColor(byte index) {
            final byte[] colorArray = {(byte) 255, (byte) 200, (byte) 100, 0};
            final int palette = Byte.toUnsignedInt(Ppu.this.bgp);
            final int paletteIndex = ((palette >> 2 * index) & 0b0011);
            return colorArray[paletteIndex];
        }
    }

    private class PixelFIFO extends ArrayDeque<Pixel> {
        private final Lcd lcd;
        private int pixelCounter = 0;
        private int scrollCounter = 0;
        private final ArrayList<Pixel> spritePixelBuffer;

        private PixelFIFO(final Lcd lcd) {
            super(16); // FIFO has 16 pixel data
            this.lcd = lcd;
            this.spritePixelBuffer = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                this.spritePixelBuffer.add(null);
            }
        }

        public void setScrollCounter(int scrollCounter) {
            this.scrollCounter = scrollCounter % 8;
        }

        private int getPixelCount() {
            return pixelCounter;
        }

        private void setSpritePixelBuffer(ArrayList<Pixel> spritePixels) {
            assert spritePixels.size() == 8;
            for (int i = 0; i < 8; i++) {
                final var newPixel = spritePixels.get(i);
                final var oldPixel = this.spritePixelBuffer.get(i);
                if (oldPixel == null) {
                    this.spritePixelBuffer.set(i, newPixel);
                } else if (oldPixel.color == 0 && newPixel.color != 0) {
                    this.spritePixelBuffer.set(i, newPixel);
                }
            }
        }

        private void pushPixelsToLCD() {
            Pixel pixel = this.poll();
            if (pixel != null && this.pixelCounter < 160) {
                if (this.scrollCounter == 0) {
                    final var spritePixel = this.spritePixelBuffer.get(0);
                    if (spritePixel != null) {
                        if (spritePixel.bgOverObj) { // スプライトが背景の下にある時
                            if (pixel.color == 0 && spritePixel.color != (byte) 255) {
                                // 背景色が黒で，スプライトが透過色でなければスプライトを描画
                                pixel = spritePixel;
                            }
                        } else {
                            if (spritePixel.color != (byte) 255) {
                                // スプライトが透過色でなければスプライトを描画
                                pixel = spritePixel;
                            }
                        }
                        this.spritePixelBuffer.remove(0);
                        this.spritePixelBuffer.add(null);
                    }
                    if (Ppu.this.lcdControl.isLCDEnable()) {
                        this.lcd.draw(pixel.color);
                    } else { // lcdがオフなら何も表示しない
                        this.lcd.draw((byte) 255);
                    }
                    this.pixelCounter++;
                } else {
                    this.scrollCounter--;
                }
            }
        }

        private boolean hasEnoughSpace() {
            return this.size() <= 8;
        }

        public void reset() {
            this.clear();
            this.pixelCounter = 0;
        }
    }
}
