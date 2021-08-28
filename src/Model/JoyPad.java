package Model;

public class JoyPad implements IODevice {
    enum MODE {
        Action, // P14
        Direction, // P15
        None;
        int value = 0;
    }

    byte register;
    MODE mode;

    JoyPad() {
        this.register = 0b0000_1111;
        this.mode = MODE.None;
    }

    @Override
    public byte read(int address) {
        return (byte) (((this.mode.value << 4) & 0xF0) | this.register);
    }

    @Override
    public void write(int address, byte data) {
        final var index = (data & 0xF0) >> 4;
        if (index == 0b10) {
            this.mode = MODE.Direction;
        } else if (index == 0b01) {
            this.mode = MODE.Action;
        } else {
            this.mode = MODE.None;
        }
        this.mode.value = index;
    }
}
