package io.github.x5897415746.mybalance.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class StringUtilTest {

    @Test
    public void bytesToHex_emptyBytes_emptyResult() {
        byte[] input = new byte[0];
        String result = StringUtil.bytesToHex(input);
        assertEquals("", result);
    }

    @Test
    public void bytesToHex_nonEmptyBytes_nonEmptyResult() {
        byte[] input = new byte[]{0x00, 0x01, 0x02, 0x03, 0x55, 0x5A, (byte) 0xAA, (byte) 0xFF};
        String result = StringUtil.bytesToHex(input);
        assertEquals("00010203555AAAFF", result);
    }
}