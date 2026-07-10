package cc.arduino.serialmonitor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Utilities for cleaning up the raw serial-port list from jssc. */
public final class SerialPorts {

  private static final String CU = "/dev/cu.";
  private static final String TTY = "/dev/tty.";

  private SerialPorts() {
  }

  /**
   * On macOS each device is exposed as both {@code /dev/tty.NAME} and
   * {@code /dev/cu.NAME}. Drop the {@code tty.*} node whenever its
   * {@code cu.*} sibling is present, so a device is listed once — as the
   * {@code cu.*} call-out node, which is the correct one for talking to a
   * device (it does not block waiting for carrier detect). A {@code tty.*}
   * node with no {@code cu.*} sibling is kept. Non-macOS names (e.g.
   * {@code /dev/ttyUSB0}, {@code COM3}) are returned unchanged, in order.
   */
  public static List<String> dedupe(String[] names) {
    Set<String> cuSuffixes = new LinkedHashSet<>();
    for (String name : names) {
      if (name.startsWith(CU)) {
        cuSuffixes.add(name.substring(CU.length()));
      }
    }
    List<String> result = new ArrayList<>();
    for (String name : names) {
      if (name.startsWith(TTY) && cuSuffixes.contains(name.substring(TTY.length()))) {
        continue;
      }
      result.add(name);
    }
    return result;
  }
}
