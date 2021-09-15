package Model;

public class JoyPad implements IODevice {
    enum Mode {
        Action, // P14
        Direction, // P15
        None;
        int modeValue = 0;
        int stateValue = 0;
    }

    enum KeyInput {
        A,
        B,
        SELECT,
        START,
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    int directionKeyState; // ↓ ↑ ← → 0: pressed 1: released
    int actionKeyState; // Start Select B A  0: pressed 1:released
    Mode mode;
    final InterruptRegister interruptRegister;

    JoyPad(InterruptRegister interruptRegister) {
        this.mode = Mode.None;
        this.actionKeyState = 0b0000_1111;
        this.directionKeyState = 0b0000_1111;
        this.interruptRegister = interruptRegister;
    }

    @Override
    public byte read(int address) {
        return (byte) (((this.mode.modeValue << 4) & 0xF0) | this.mode.stateValue);
    }

    @Override
    public void write(int address, byte data) {
        final var index = (data & 0xF0) >> 4;
        if (index == 0b10) {
            this.mode = Mode.Direction;
            this.mode.stateValue = this.directionKeyState;
        } else if (index == 0b01) {
            this.mode = Mode.Action;
            this.mode.stateValue = this.actionKeyState;
        } else {
            this.mode = Mode.None;
            this.mode.stateValue = 0b1111;
        }
        this.mode.modeValue = index;
    }

    public void setKeyState(final KeyInput key, final boolean pressed) {
        int keyMask = switch (key) {
            case START, DOWN -> 0b1000;
            case SELECT, UP -> 0b0100;
            case B, LEFT -> 0b0010;
            case A, RIGHT -> 0b0001;
            default -> 0b1111;
        };
        switch (key) {
            case START, SELECT, B, A -> {
                if (!pressed) {
                    this.actionKeyState |= keyMask;
                } else {
                    keyMask = (~keyMask) & 0b1111;
                    this.actionKeyState &= keyMask;
                    this.interruptRegister.setJoyPadInterrupt(true);
                }
            }
            case DOWN, UP, LEFT, RIGHT -> {
                if (!pressed) {
                    this.directionKeyState |= keyMask;
                } else {
                    keyMask = (~keyMask) & 0b1111;
                    this.directionKeyState &= keyMask;
                    this.interruptRegister.setJoyPadInterrupt(true);
                }
            }
        }
    }
}
