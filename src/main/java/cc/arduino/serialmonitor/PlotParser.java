package cc.arduino.serialmonitor;

import java.util.ArrayList;
import java.util.List;

/** Parses one line of plotter input into labeled/anonymous numeric samples. */
public final class PlotParser {

  public static final class Sample {
    public final String label; // nullable
    public final double value;
    public Sample(String label, double value) {
      this.label = label;
      this.value = value;
    }
  }

  private PlotParser() {}

  public static List<Sample> parseLine(String line) {
    List<Sample> out = new ArrayList<>();
    String trimmed = line.trim();
    if (trimmed.isEmpty()) {
      return out;
    }
    for (String token : trimmed.split("[,\t ]+")) {
      if (token.isEmpty()) {
        continue;
      }
      String label = null;
      String numberPart = token;
      int colon = token.indexOf(':');
      if (colon >= 0) {
        label = token.substring(0, colon);
        numberPart = token.substring(colon + 1);
      }
      try {
        out.add(new Sample(label, Double.parseDouble(numberPart)));
      } catch (NumberFormatException e) {
        // not a number -> skip this token
      }
    }
    return out;
  }
}
