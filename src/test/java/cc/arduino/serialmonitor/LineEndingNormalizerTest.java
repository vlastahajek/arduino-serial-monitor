package cc.arduino.serialmonitor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LineEndingNormalizerTest {

  @Test
  public void loneCarriageReturnBecomesNewline() {
    assertEquals("a\nb", new LineEndingNormalizer().normalize("a\rb"));
  }

  @Test
  public void crlfCollapsesToSingleNewline() {
    assertEquals("a\nb", new LineEndingNormalizer().normalize("a\r\nb"));
  }

  @Test
  public void bareNewlineIsUnchanged() {
    assertEquals("a\nb", new LineEndingNormalizer().normalize("a\nb"));
  }

  @Test
  public void consecutiveCarriageReturnsBreakSeparateLines() {
    assertEquals("x\n\ny", new LineEndingNormalizer().normalize("x\r\ry"));
  }

  @Test
  public void crlfSplitAcrossChunksCollapsesToOneNewline() {
    LineEndingNormalizer n = new LineEndingNormalizer();
    // CR ends the first chunk, LF begins the next — must not double-break.
    assertEquals("a\n", n.normalize("a\r"));
    assertEquals("b", n.normalize("\nb"));
  }
}
