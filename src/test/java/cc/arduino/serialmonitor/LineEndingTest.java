package cc.arduino.serialmonitor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LineEndingTest {
  @Test public void none()       { assertEquals("hi",     LineEnding.apply("hi", 0)); }
  @Test public void newline()    { assertEquals("hi\n",   LineEnding.apply("hi", 1)); }
  @Test public void carriage()   { assertEquals("hi\r",   LineEnding.apply("hi", 2)); }
  @Test public void both()       { assertEquals("hi\r\n", LineEnding.apply("hi", 3)); }
}
