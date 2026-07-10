package cc.arduino.serialmonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Plain-text or regular-expression search, optionally case-sensitive. */
public final class TextSearch {

  private TextSearch() {
  }

  /**
   * Case-insensitive literal search. Equivalent to
   * {@link #findMatches(String, String, boolean, boolean)} with
   * {@code matchCase=false, regex=false}.
   */
  public static List<int[]> findMatches(String text, String query) {
    return findMatches(text, query, false, false);
  }

  /**
   * Returns the {@code [start, end)} offsets of every non-overlapping match of
   * {@code query} in {@code text}, in order. Offsets index into the original
   * {@code text}. Zero-width matches (e.g. from a regex like {@code a*}) are
   * skipped. Returns an empty list if {@code query} or {@code text} is
   * null/empty.
   *
   * @param matchCase when true the search is case-sensitive
   * @param regex     when true {@code query} is a regular expression;
   *                  otherwise it is matched literally
   * @throws java.util.regex.PatternSyntaxException if {@code regex} is true and
   *         {@code query} is not a valid regular expression
   */
  public static List<int[]> findMatches(String text, String query,
                                        boolean matchCase, boolean regex) {
    List<int[]> matches = new ArrayList<>();
    if (text == null || text.isEmpty() || query == null || query.isEmpty()) {
      return matches;
    }
    int flags = matchCase ? 0 : (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    Pattern pattern = Pattern.compile(regex ? query : Pattern.quote(query), flags);
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      if (matcher.end() > matcher.start()) {
        matches.add(new int[] {matcher.start(), matcher.end()});
      }
    }
    return matches;
  }
}
