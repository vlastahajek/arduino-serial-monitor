package cc.arduino.serialmonitor;

/** Maps the line-ending combo index to the suffix appended on send. */
public final class LineEnding {
  public static final String[] LABELS = {
      "No line ending", "Newline", "Carriage return", "Both NL & CR"
  };

  private LineEnding() {}

  public static String apply(String s, int index) {
    switch (index) {
      case 1: return s + "\n";
      case 2: return s + "\r";
      case 3: return s + "\r\n";
      default: return s;
    }
  }
}
