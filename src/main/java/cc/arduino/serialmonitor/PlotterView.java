package cc.arduino.serialmonitor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import cc.arduino.serialmonitor.util.CircularBuffer;
import cc.arduino.serialmonitor.util.Ticks;

public class PlotterView extends JPanel {

  private static final int BUFFER_CAPACITY = 500;
  private static final Color DISCONNECTED_BG = new Color(0xEE, 0xEE, 0xEE);
  private static final Color DISCONNECTED_FG = new Color(0x99, 0x99, 0x99);

  private final StringBuilder messageBuffer = new StringBuilder();
  private final List<CircularBuffer> series = new ArrayList<>();
  private final GraphPanel graph = new GraphPanel();
  private boolean connected = false;
  private final JTextField textField = new JTextField(40);
  private final JButton sendButton = new JButton("Send");
  private final JComboBox<String> lineEndings = new JComboBox<>(LineEnding.LABELS);

  public PlotterView(Consumer<String> sendToPort) {
    super(new BorderLayout());
    add(graph, BorderLayout.CENTER);

    JPanel bottom = new JPanel();
    bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
    bottom.setBorder(new EmptyBorder(4, 4, 4, 4));
    bottom.add(textField);
    bottom.add(Box.createRigidArea(new Dimension(4, 0)));
    bottom.add(sendButton);
    bottom.add(Box.createRigidArea(new Dimension(8, 0)));
    lineEndings.setSelectedIndex(Settings.getInt("serial.line_ending", 1));
    lineEndings.setMaximumSize(lineEndings.getMinimumSize());
    bottom.add(lineEndings);
    add(bottom, BorderLayout.SOUTH);

    Runnable doSend = () -> {
      sendToPort.accept(LineEnding.apply(textField.getText(), lineEndings.getSelectedIndex()));
      textField.setText("");
    };
    sendButton.addActionListener(e -> doSend.run());
    textField.addActionListener(e -> doSend.run());
    lineEndings.addActionListener(e ->
        Settings.setInt("serial.line_ending", lineEndings.getSelectedIndex()));
  }

  public void append(String text) {
    SwingUtilities.invokeLater(() -> {
      messageBuffer.append(text);
      int linebreak;
      while ((linebreak = messageBuffer.indexOf("\n")) != -1) {
        String line = messageBuffer.substring(0, linebreak);
        messageBuffer.delete(0, linebreak + 1);
        if (line.endsWith("\r")) {
          line = line.substring(0, line.length() - 1);
        }
        List<PlotParser.Sample> samples = PlotParser.parseLine(line);
        for (int i = 0; i < samples.size(); i++) {
          while (series.size() <= i) {
            series.add(new CircularBuffer(BUFFER_CAPACITY));
          }
          series.get(i).add(samples.get(i).value);
        }
      }
      graph.repaint();
    });
  }

  public void setConnected(boolean connected) {
    this.connected = connected;
    textField.setEnabled(connected);
    sendButton.setEnabled(connected);
    graph.setBackground(connected ? Palette.PLOT_BG : DISCONNECTED_BG);
    graph.repaint();
  }

  private class GraphPanel extends JPanel {
    GraphPanel() {
      setBackground(Palette.PLOT_BG);
      setPreferredSize(new Dimension(600, 360));
    }

    @Override
    protected void paintComponent(Graphics g1) {
      super.paintComponent(g1);
      Graphics2D g = (Graphics2D) g1;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int w = getWidth(), h = getHeight();
      if (!connected) {
        drawDisconnectedLabel(g, w, h);
      }

      double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
      for (CircularBuffer b : series) {
        if (!b.isEmpty()) {
          minY = Math.min(minY, b.min());
          maxY = Math.max(maxY, b.max());
        }
      }
      if (minY == Double.POSITIVE_INFINITY) {
        return; // no data yet
      }
      final double MIN_DELTA = 10.0;
      if (maxY - minY < MIN_DELTA) {
        double mid = (maxY + minY) / 2;
        maxY = mid + MIN_DELTA / 2;
        minY = mid - MIN_DELTA / 2;
      }
      Ticks ticks = new Ticks(minY, maxY, 5);
      minY = Math.min(minY, ticks.getTick(0));
      maxY = Math.max(maxY, ticks.getTick(ticks.getTickCount() - 1));
      double rangeY = maxY - minY;
      if (rangeY == 0) rangeY = 1;

      g.setColor(Palette.PLOT_GRID);
      for (int i = 0; i < ticks.getTickCount(); i++) {
        int y = (int) transformY(ticks.getTick(i), minY, rangeY, h);
        g.drawLine(0, y, w, y);
      }
      g.setColor(Palette.PLOT_BOUNDS);
      g.drawRect(0, 0, w - 1, h - 1);

      for (int s = 0; s < series.size(); s++) {
        CircularBuffer b = series.get(s);
        if (b.size() < 2) continue;
        g.setColor(Palette.graphColor(s));
        g.setStroke(new BasicStroke(1.0f));
        float xstep = (float) w / (BUFFER_CAPACITY - 1);
        for (int i = 0; i < b.size() - 1; i++) {
          g.drawLine(
              (int) (i * xstep),       (int) transformY(b.get(i),     minY, rangeY, h),
              (int) ((i + 1) * xstep), (int) transformY(b.get(i + 1), minY, rangeY, h));
        }
      }
    }

    private void drawDisconnectedLabel(Graphics2D g, int w, int h) {
      String msg = "Disconnected";
      g.setColor(DISCONNECTED_FG);
      g.setFont(getFont().deriveFont(Font.BOLD, 20f));
      FontMetrics fm = g.getFontMetrics();
      g.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 + fm.getAscent() / 2);
    }

    private float transformY(double rawY, double minY, double rangeY, double height) {
      return (float) (5 + (height - 10) * (1.0 - (rawY - minY) / rangeY));
    }
  }
}
