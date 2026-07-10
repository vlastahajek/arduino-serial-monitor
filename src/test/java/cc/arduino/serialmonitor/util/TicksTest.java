package cc.arduino.serialmonitor.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TicksTest {
  @Test
  public void producesAscendingTicksCoveringRange() {
    Ticks t = new Ticks(0.0, 10.0, 5);
    assertTrue(t.getTickCount() >= 2);
    for (int i = 1; i < t.getTickCount(); i++) {
      assertTrue(t.getTick(i) > t.getTick(i - 1));
    }
  }
}
