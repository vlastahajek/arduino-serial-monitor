package cc.arduino.serialmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.nio.charset.StandardCharsets;
import jssc.SerialPortException;
import org.junit.Test;

public class SerialConnectionTest {
  @Test
  public void decodesAsciiInOneChunk() {
    StringBuilder out = new StringBuilder();
    SerialConnection c = new SerialConnection(out::append);
    c.feed("hello".getBytes(StandardCharsets.UTF_8));
    assertEquals("hello", out.toString());
  }

  @Test
  public void decodesMultibyteCharSplitAcrossChunks() {
    StringBuilder out = new StringBuilder();
    SerialConnection c = new SerialConnection(out::append);
    byte[] e = "é".getBytes(StandardCharsets.UTF_8); // 2 bytes: 0xC3 0xA9
    c.feed(new byte[] { e[0] });   // incomplete char -> nothing emitted yet
    c.feed(new byte[] { e[1] });   // completes the char
    assertEquals("é", out.toString());
  }

  @Test(expected = SerialPortException.class)
  public void openNonexistentPortThrowsAndLeavesNotOpen() throws SerialPortException {
    SerialConnection c = new SerialConnection(s -> {});
    try {
      c.open("__nonexistent_port__", 9600);
    } finally {
      assertFalse("port must not be open after failed open()", c.isOpen());
    }
  }

  @Test
  public void flushOnCloseEmitsReplacementForIncompleteSequence() {
    StringBuilder out = new StringBuilder();
    SerialConnection c = new SerialConnection(out::append);
    c.feed(new byte[]{ (byte)0xE2 }); // first byte of a 3-byte UTF-8 sequence; nothing emitted yet
    assertEquals("", out.toString());
    c.close(); // no open port; must still flush decoder
    // replacement string is "⸮" as configured by .replaceWith("⸮") in SerialConnection
    assertEquals("⸮", out.toString());
  }
}
