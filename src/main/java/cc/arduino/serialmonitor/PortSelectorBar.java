package cc.arduino.serialmonitor;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import jssc.SerialPortException;
import jssc.SerialPortList;

public class PortSelectorBar extends JPanel {

  private static final String[] BAUD_RATES = {
      "300", "1200", "2400", "4800", "9600", "19200", "38400", "57600",
      "74880", "115200", "230400", "250000", "500000", "921600", "1000000", "2000000"
  };

  private final JComboBox<String> portBox = new JComboBox<>();
  private final JComboBox<String> baudBox = new JComboBox<>(BAUD_RATES);
  private final JButton refreshButton = new JButton("Refresh");
  private final JButton connectButton = new JButton("Connect");
  private final JLabel status = new JLabel("Disconnected");

  private final SerialConnection connection;
  private final Runnable onConnectionChanged;
  private final Timer portWatch;

  public PortSelectorBar(Consumer<String> onText, Runnable onConnectionChanged) {
    super(new FlowLayout(FlowLayout.LEFT, 6, 4));
    this.connection = new SerialConnection(onText);
    this.onConnectionChanged = onConnectionChanged;

    add(new JLabel("Port:"));
    add(portBox);
    add(refreshButton);
    add(new JLabel("Baud:"));
    baudBox.setSelectedItem(Settings.get("serial.baud", "9600"));
    add(baudBox);
    add(connectButton);
    add(status);

    refreshButton.addActionListener(e -> refreshPorts());
    connectButton.addActionListener(e -> toggleConnection());
    baudBox.addActionListener(e ->
        Settings.set("serial.baud", (String) baudBox.getSelectedItem()));

    refreshPorts();

    portWatch = new Timer(1000, e -> checkPortStillPresent());
    portWatch.start();
  }

  private void refreshPorts() {
    String previous = (String) portBox.getSelectedItem();
    portBox.removeAllItems();
    for (String name : SerialPorts.dedupe(SerialPortList.getPortNames())) {
      portBox.addItem(name);
    }
    String remembered = previous != null ? previous : Settings.get("serial.port", null);
    if (remembered != null) {
      portBox.setSelectedItem(remembered);
    }
  }

  private void toggleConnection() {
    if (connection.isOpen()) {
      disconnect();
    } else {
      connect();
    }
  }

  private void connect() {
    String port = (String) portBox.getSelectedItem();
    if (port == null) {
      status.setText("No port selected");
      return;
    }
    int baud = Integer.parseInt((String) baudBox.getSelectedItem());
    try {
      connection.open(port, baud);
      Settings.set("serial.port", port);
      connectButton.setText("Disconnect");
      status.setText("Connected " + port + " @ " + baud);
      setSelectorsEnabled(false);
    } catch (SerialPortException e) {
      status.setText("Open failed: " + e.getMessage());
    }
    onConnectionChanged.run();
  }

  private void disconnect() {
    connection.close();
    connectButton.setText("Connect");
    status.setText("Disconnected");
    setSelectorsEnabled(true);
    onConnectionChanged.run();
  }

  private void checkPortStillPresent() {
    if (!connection.isOpen()) {
      return;
    }
    String port = (String) portBox.getSelectedItem();
    boolean present = SerialPorts.dedupe(SerialPortList.getPortNames()).contains(port);
    if (!present) {
      disconnect();
      status.setText("Port disappeared");
    }
  }

  private void setSelectorsEnabled(boolean enabled) {
    portBox.setEnabled(enabled);
    baudBox.setEnabled(enabled);
    refreshButton.setEnabled(enabled);
  }

  public void shutdown() {
    portWatch.stop();
    connection.close();
  }

  public boolean isConnected() {
    return connection.isOpen();
  }

  public void writeToPort(String s) {
    connection.write(s);
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
  }
}
