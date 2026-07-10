package cc.arduino.serialmonitor;

import java.awt.Color;
import java.awt.Font;

/** Hardcoded colors/font replacing the IDE Theme for the plotter and console. */
public final class Palette {
  public static final Color PLOT_BG     = Color.WHITE;
  public static final Color PLOT_GRID   = new Color(0xCC, 0xCC, 0xCC);
  public static final Color PLOT_BOUNDS = new Color(0x80, 0x80, 0x80);
  public static final Font  CONSOLE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

  private static final Color[] CYCLE = {
      new Color(0x2A, 0x7F, 0xFF), new Color(0xE5, 0x3E, 0x3E),
      new Color(0x2E, 0xA0, 0x4F), new Color(0xE5, 0x8E, 0x26),
      new Color(0x8E, 0x44, 0xAD), new Color(0x16, 0xA0, 0x85),
      new Color(0xC0, 0x39, 0x2B), new Color(0x27, 0x60, 0xB2),
  };

  private Palette() {}

  public static Color graphColor(int index) {
    return CYCLE[Math.floorMod(index, CYCLE.length)];
  }
}
