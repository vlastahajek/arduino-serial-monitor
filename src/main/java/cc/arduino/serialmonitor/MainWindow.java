package cc.arduino.serialmonitor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class MainWindow extends JFrame {

  private static final String MONITOR = "monitor";
  private static final String PLOTTER = "plotter";

  private final CardLayout cards = new CardLayout();
  private final JPanel center = new JPanel(cards);
  private final MonitorView monitorView;
  private final PlotterView plotterView;
  private final PortSelectorBar selector;

  private volatile String activeCard = MONITOR;

  public MainWindow() {
    super("Arduino Serial Monitor");

    URL iconUrl = MainWindow.class.getResource("/appicon.png");
    if (iconUrl != null) {
      setIconImage(new ImageIcon(iconUrl).getImage());
    }

    // Route incoming serial text only to the visible view.
    selector = new PortSelectorBar(this::onSerialText, this::onConnectionChanged);
    monitorView = new MonitorView(selector::writeToPort);
    plotterView = new PlotterView(selector::writeToPort);

    JPanel north = new JPanel();
    north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
    north.add(selector);
    north.add(buildToggle());

    center.add(monitorView, MONITOR);
    center.add(plotterView, PLOTTER);

    setLayout(new BorderLayout());
    add(north, BorderLayout.NORTH);
    add(center, BorderLayout.CENTER);

    onConnectionChanged(); // set initial enabled state

    int x = Settings.getInt("window.x", 80);
    int y = Settings.getInt("window.y", 80);
    int w = Settings.getInt("window.w", 720);
    int h = Settings.getInt("window.h", 520);
    setBounds(x, y, w, h);

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) {
        Settings.setInt("window.x", getX());
        Settings.setInt("window.y", getY());
        Settings.setInt("window.w", getWidth());
        Settings.setInt("window.h", getHeight());
        selector.shutdown();
      }
    });
  }

  private JPanel buildToggle() {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    JToggleButton monitorBtn = new JToggleButton("Monitor", true);
    JToggleButton plotterBtn = new JToggleButton("Plotter");
    ButtonGroup group = new ButtonGroup();
    group.add(monitorBtn);
    group.add(plotterBtn);
    monitorBtn.addActionListener(e -> show(MONITOR));
    plotterBtn.addActionListener(e -> show(PLOTTER));
    bar.add(monitorBtn);
    bar.add(plotterBtn);
    return bar;
  }

  private void show(String card) {
    activeCard = card;
    cards.show(center, card);
  }

  private void onSerialText(String text) {
    if (MONITOR.equals(activeCard)) {
      monitorView.append(text);
    } else {
      plotterView.append(text);
    }
  }

  private void onConnectionChanged() {
    boolean connected = selector.isConnected();
    monitorView.setConnected(connected);
    plotterView.setConnected(connected);
  }
}
