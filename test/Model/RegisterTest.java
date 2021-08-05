package Model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterTest {
    @Test
    public void registerIOTest() {
        var register = new Register();
        register.bc = 0x1234;
        assertEquals(0x12, register.getB());
        assertEquals(0x34, register.getC());
        System.out.println(register);
        register.setB((byte)0x56);
        register.setC((byte)0x78);
        assertEquals(0x56, register.getB());
        assertEquals(0x78, register.getC());
        System.out.println(register);

        register.de = 0x1234;
        assertEquals(0x12, register.getD());
        assertEquals(0x34, register.getE());
        System.out.println(register);
        register.setD((byte)0x56);
        register.setE((byte)0x78);
        assertEquals(0x56, register.getD());
        assertEquals(0x78, register.getE());
        System.out.println(register);

        register.hl = 0x1234;
        assertEquals(0x12, register.getH());
        assertEquals(0x34, register.getL());
        System.out.println(register);
        register.setH((byte)0x56);
        register.setL((byte)0x78);
        assertEquals(0x56, register.getH());
        assertEquals(0x78, register.getL());
        System.out.println(register);
    }
}