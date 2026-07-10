package cc.arduino.serialmonitor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.regex.PatternSyntaxException;
import org.junit.Test;

public class TextSearchTest {

  @Test
  public void findsAllCaseInsensitiveOccurrences() {
    List<int[]> m = TextSearch.findMatches("hello Hello HELLO", "hello");
    assertEquals(3, m.size());
    assertArrayEquals(new int[] {0, 5}, m.get(0));
    assertArrayEquals(new int[] {6, 11}, m.get(1));
    assertArrayEquals(new int[] {12, 17}, m.get(2));
  }

  @Test
  public void nonOverlappingMatches() {
    List<int[]> m = TextSearch.findMatches("aaaa", "aa");
    assertEquals(2, m.size());
    assertArrayEquals(new int[] {0, 2}, m.get(0));
    assertArrayEquals(new int[] {2, 4}, m.get(1));
  }

  @Test
  public void treatsQueryLiterallyNotAsRegex() {
    List<int[]> m = TextSearch.findMatches("a.b axb", ".");
    assertEquals(1, m.size());
    assertArrayEquals(new int[] {1, 2}, m.get(0)); // only the literal '.'
  }

  @Test
  public void emptyQueryYieldsNoMatches() {
    assertTrue(TextSearch.findMatches("anything", "").isEmpty());
  }

  @Test
  public void noMatchYieldsEmpty() {
    assertTrue(TextSearch.findMatches("abc", "xyz").isEmpty());
  }

  @Test
  public void matchCaseIsCaseSensitive() {
    List<int[]> m = TextSearch.findMatches("Hello hello", "hello", true, false);
    assertEquals(1, m.size());
    assertArrayEquals(new int[] {6, 11}, m.get(0)); // only the lowercase one
  }

  @Test
  public void regexMatchesPattern() {
    List<int[]> m = TextSearch.findMatches("axb a.b", "a.b", false, true);
    assertEquals(2, m.size()); // '.' is any-char: matches "axb" and "a.b"
    assertArrayEquals(new int[] {0, 3}, m.get(0));
    assertArrayEquals(new int[] {4, 7}, m.get(1));
  }

  @Test
  public void regexRespectsMatchCase() {
    assertEquals(0, TextSearch.findMatches("ABC", "[a-z]+", true, true).size());
    assertEquals(1, TextSearch.findMatches("ABC", "[a-z]+", false, true).size());
  }

  @Test
  public void zeroWidthRegexMatchesAreSkipped() {
    // "a*" can match empty; those must not be recorded (only the real "aa").
    List<int[]> m = TextSearch.findMatches("baab", "a*", false, true);
    assertEquals(1, m.size());
    assertArrayEquals(new int[] {1, 3}, m.get(0));
  }

  @Test(expected = PatternSyntaxException.class)
  public void invalidRegexThrows() {
    TextSearch.findMatches("abc", "a(b", false, true);
  }
}
