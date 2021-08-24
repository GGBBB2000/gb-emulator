package Model;

class InterruptRegister {
    private byte interruptRequestFlag;
    private byte interruptEnable;

    /*
    FF0F - IF - Interrupt Flag (R/W)
    Bit 0: VBlank   Interrupt Request (INT 40h)  (1=Request)
    Bit 1: LCD STAT Interrupt Request (INT 48h)  (1=Request)
    Bit 2: Timer    Interrupt Request (INT 50h)  (1=Request)
    Bit 3: Serial   Interrupt Request (INT 58h)  (1=Request)
    Bit 4: Joypad   Interrupt Request (INT 60h)  (1=Request)


    FFFF - IE - Interrupt Enable (R/W)
    Bit 0: VBlank   Interrupt Enable  (INT 40h)  (1=Enable)
    Bit 1: LCD STAT Interrupt Enable  (INT 48h)  (1=Enable)
    Bit 2: Timer    Interrupt Enable  (INT 50h)  (1=Enable)
    Bit 3: Serial   Interrupt Enable  (INT 58h)  (1=Enable)
    Bit 4: Joypad   Interrupt Enable  (INT 60h)  (1=Enable)
     */

    InterruptRegister() {
        this.interruptRequestFlag = 0;
        this.interruptEnable = 0;
    }

    void setInterruptEnable(byte interruptEnable) {
        this.interruptEnable = interruptEnable;
    }

    byte getInterruptEnable() {
        return interruptEnable;
    }

    void setInterruptRequestFlag(final byte interruptRequestFlag) {
        this.interruptRequestFlag = interruptRequestFlag;
    }

    byte getInterruptRequestFlag() {
        return interruptRequestFlag;
    }

    void setVBlankInterrupt(final boolean flag) {
        if (flag) {
            this.interruptRequestFlag |= 0b0000_0001;
        } else {
            this.interruptRequestFlag &= 0b1111_1110;
        }
    }

    void setLCD_STAT_Interrupt(final boolean flag) {
        if (flag) {
            this.interruptRequestFlag |= 0b0000_0010;
        } else {
            this.interruptRequestFlag &= 0b1111_1101;
        }
    }

    void setTimerInterrupt(final boolean flag) {
        if (flag) {
            this.interruptRequestFlag |= 0b0000_0100;
        } else {
            this.interruptRequestFlag &= 0b1111_1011;
        }
    }

    void setSerialInterrupt(final boolean flag) {
        if (flag) {
            this.interruptRequestFlag |= 0b0000_1000;
        } else {
            this.interruptRequestFlag &= 0b1111_0111;
        }
    }

    void setJoyPadInterrupt(final boolean flag) {
        if (flag) {
            this.interruptRequestFlag |= 0b0001_0000;
        } else {
            this.interruptRequestFlag &= 0b1110_1111;
        }
    }
}
