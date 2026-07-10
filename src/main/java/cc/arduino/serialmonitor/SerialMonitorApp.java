package cc.arduino.serialmonitor;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class SerialMonitorApp {
  private SerialMonitorApp() {}

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ignored) {
      // fall back to default look and feel
    }
    SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
  }
}
