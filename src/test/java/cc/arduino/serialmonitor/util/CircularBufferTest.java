package cc.arduino.serialmonitor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CircularBufferTest {
  @Test
  public void wrapsAndTracksMinMax() {
    CircularBuffer b = new CircularBuffer(3);
    assertTrue(b.isEmpty());
    b.add(1.0); b.add(2.0); b.add(3.0);
    assertEquals(3, b.size());
    assertEquals(1.0, b.min(), 0.0);
    assertEquals(3.0, b.max(), 0.0);
    b.add(4.0); // evicts the oldest (1.0)
    assertEquals(3, b.size());
    assertEquals(2.0, b.min(), 0.0);
    assertEquals(4.0, b.max(), 0.0);
  }
}
