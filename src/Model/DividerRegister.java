package Model;

class DividerRegister implements IODevice {
    /*
    FF04: DIV Divider Register
    This register is incremented at a rate of 16384Hz(in CGB mode, incremented at 32768Hz)
    Writing any value to this register resets it to $0x00. Also register will be reset by executing stop
     */

    private int counter;
    private int cycleSum;

    DividerRegister() {
        this.reset();
    }

    void reset() {
        this.counter = 0;
        this.cycleSum = 0;
    }

    void addCycle(final int cycle) {
        this.cycleSum += cycle;
        if (this.cycleSum >= 256) {
            this.counter++;
            this.cycleSum %= 256;
            if (this.counter > 0xFF) {
                this.counter = 0;
            }
        }
    }

    @Override
    public byte read(int address) {
        return (byte) this.counter;
    }

    @Override
    public void write(int address, byte data) {
        this.counter = 0;
    }
}
