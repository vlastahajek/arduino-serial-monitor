package cc.arduino.serialmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class PlotParserTest {
  @Test public void parsesCommaSeparatedNumbers() {
    List<PlotParser.Sample> s = PlotParser.parseLine("1, 2, 3");
    assertEquals(3, s.size());
    assertEquals(2.0, s.get(1).value, 0.0);
    assertNull(s.get(1).label);
  }

  @Test public void parsesLabeledPairs() {
    List<PlotParser.Sample> s = PlotParser.parseLine("temp:21.5 hum:60");
    assertEquals(2, s.size());
    assertEquals("temp", s.get(0).label);
    assertEquals(21.5, s.get(0).value, 0.0);
    assertEquals("hum", s.get(1).label);
    assertEquals(60.0, s.get(1).value, 0.0);
  }

  @Test public void skipsNonNumericTokens() {
    List<PlotParser.Sample> s = PlotParser.parseLine("1 abc 2");
    assertEquals(2, s.size());
    assertEquals(1.0, s.get(0).value, 0.0);
    assertEquals(2.0, s.get(1).value, 0.0);
  }

  @Test public void blankLineYieldsEmpty() {
    assertTrue(PlotParser.parseLine("   ").isEmpty());
  }
}
