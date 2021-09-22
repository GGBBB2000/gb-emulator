package Model;

class Timer implements IODevice {
    /*
    FF05: TIMA - Timer counter
    FF06: TMA - Timer Modulo
    FF07: TAC - Timer Control
        bit2: Timer Enable
        bit1-0: input clock select
            00: cpu clock / 1024 (4096Hz)
            01: cpu clock / 16 (262144 Hz)
            10: cpu clock / 64 (65536Hz)
            11: cpu clock / 256 (16384 Hz) clock frequency will be double in CGB mode
     */
    int timerCounter;
    int timerModulo;
    int timerControl;
    int inputClock;
    boolean timerEnable;
    int cycleSum;
    final InterruptRegister interruptRegister;

    Timer(InterruptRegister interruptRegister) {
        this.interruptRegister = interruptRegister;
        this.timerCounter = 0;
        this.timerModulo = 0;
        this.timerControl = 0;
        this.inputClock = 0;
        this.timerEnable = false;
    }

    void reset() {
        this.timerCounter = 0;
        this.timerModulo = 0;
        this.timerControl = 0;
        this.inputClock = 0;
        this.timerEnable = false;
    }

    void addCycle(final int cycle) {
        if (this.timerEnable) {
            this.cycleSum += cycle;
            if (inputClock <= this.cycleSum) {
                this.cycleSum %= inputClock;
                this.timerCounter++;
                if (timerCounter > 0xFF) {
                    this.cycleSum = 0;
                    this.timerCounter = this.timerModulo;
                    this.interruptRegister.setTimerInterrupt(true);
                }
            }
        }
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void write(int address, byte data) {
        if (address == 0xFF05) {
            this.timerCounter = Byte.toUnsignedInt(data);
        } else if (address == 0xFF06) {
            this.timerModulo = Byte.toUnsignedInt(data);
        } else if (address == 0xFF07) {
            this.timerEnable = (data & 0b100) == 0b100;
            final int clockSelect = data & 0b11;
            this.inputClock = switch (clockSelect) {
                case 0b00 -> 1024;
                case 0b01 -> 16;
                case 0b10 -> 64;
                case 0b11 -> 256;
                default -> throw new IllegalStateException("Unexpected value: " + clockSelect);
            };
        }
    }
}
