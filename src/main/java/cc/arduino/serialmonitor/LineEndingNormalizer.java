package cc.arduino.serialmonitor;

/**
 * Normalizes incoming CR and CRLF line endings to a single LF for display,
 * correctly across streamed chunks (a CRLF may be split so the CR ends one
 * chunk and the LF begins the next). Stateful; call {@link #normalize} in
 * arrival order from a single thread.
 */
public final class LineEndingNormalizer {

  private boolean lastCharWasCR = false;

  /** Returns {@code text} with lone CR and CRLF collapsed to LF. */
  public String normalize(String text) {
    StringBuilder sb = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\r') {
        // Emit the line break immediately so CR-only devices break lines
        // without waiting for the next byte; a following LF is swallowed.
        sb.append('\n');
        lastCharWasCR = true;
      } else if (c == '\n') {
        if (!lastCharWasCR) {
          sb.append('\n');
        }
        lastCharWasCR = false;
      } else {
        sb.append(c);
        lastCharWasCR = false;
      }
    }
    return sb.toString();
  }
}
