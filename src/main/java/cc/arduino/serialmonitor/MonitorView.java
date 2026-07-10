package cc.arduino.serialmonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import cc.arduino.serialmonitor.util.CommandHistory;
import cc.arduino.serialmonitor.util.TextAreaFIFO;

public class MonitorView extends JPanel {

  private static final String LINE_SEPARATOR = "\n";
  private static final int COMMAND_HISTORY_SIZE = 100;

  private final TextAreaFIFO textArea = new TextAreaFIFO(8_000_000);
  private final JTextField textField = new JTextField(40);
  private final JButton sendButton = new JButton("Send");
  private final JButton clearButton = new JButton("Clear output");
  private final JCheckBox autoscrollBox = new JCheckBox("Autoscroll", true);
  private final JCheckBox addTimeStampBox = new JCheckBox("Show timestamp", false);
  private final JComboBox<String> lineEndings = new JComboBox<>(LineEnding.LABELS);
  private final CommandHistory history = new CommandHistory(COMMAND_HISTORY_SIZE);

  private boolean isStartingLine = true;
  private final LineEndingNormalizer normalizer = new LineEndingNormalizer();
  private final FindBar findBar = new FindBar(textArea);

  public MonitorView(Consumer<String> sendToPort) {
    super(new BorderLayout());

    textArea.setRows(16);
    textArea.setColumns(40);
    textArea.setEditable(false);
    textArea.setFont(Palette.CONSOLE_FONT);
    ((DefaultCaret) textArea.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    add(new JScrollPane(textArea), BorderLayout.CENTER);

    JPanel top = new JPanel();
    top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
    top.setBorder(new EmptyBorder(4, 4, 4, 4));
    top.add(textField);
    top.add(Box.createRigidArea(new Dimension(4, 0)));
    top.add(sendButton);
    add(top, BorderLayout.NORTH);

    JPanel bottom = new JPanel();
    bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
    bottom.setBorder(new EmptyBorder(4, 4, 4, 4));
    bottom.add(autoscrollBox);
    bottom.add(addTimeStampBox);
    bottom.add(Box.createHorizontalGlue());
    lineEndings.setSelectedIndex(Settings.getInt("serial.line_ending", 1));
    lineEndings.setMaximumSize(lineEndings.getMinimumSize());
    bottom.add(lineEndings);
    bottom.add(Box.createRigidArea(new Dimension(8, 0)));
    bottom.add(clearButton);

    JPanel south = new JPanel(new BorderLayout());
    south.add(findBar, BorderLayout.NORTH);
    south.add(bottom, BorderLayout.SOUTH);
    add(south, BorderLayout.SOUTH);

    int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    // WHEN_IN_FOCUSED_WINDOW so Cmd/Ctrl+F works from any focused element (port
    // selector, toggle, send field). It only fires while this view is the
    // showing card — CardLayout marks the hidden card as not showing.
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask), "openFind");
    getActionMap().put("openFind", new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) {
        findBar.open();
      }
    });

    autoscrollBox.setSelected(Settings.getBool("serial.autoscroll", true));
    addTimeStampBox.setSelected(Settings.getBool("serial.show_timestamp", false));

    Runnable doSend = () -> {
      String command = textField.getText();
      sendToPort.accept(LineEnding.apply(command, lineEndings.getSelectedIndex()));
      history.addCommand(command);
      textField.setText("");
    };
    sendButton.addActionListener(e -> doSend.run());
    textField.addActionListener(e -> doSend.run());
    clearButton.addActionListener(e -> textArea.setText(""));
    lineEndings.addActionListener(e ->
        Settings.setInt("serial.line_ending", lineEndings.getSelectedIndex()));
    autoscrollBox.addActionListener(e ->
        Settings.setBool("serial.autoscroll", autoscrollBox.isSelected()));
    addTimeStampBox.addActionListener(e ->
        Settings.setBool("serial.show_timestamp", addTimeStampBox.isSelected()));

    textField.addKeyListener(new KeyAdapter() {
      @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_UP:
            if (history.hasPreviousCommand()) {
              textField.setText(history.getPreviousCommand(textField.getText()));
            }
            break;
          case KeyEvent.VK_DOWN:
            if (history.hasNextCommand()) {
              textField.setText(history.getNextCommand());
            }
            break;
          case KeyEvent.VK_ESCAPE:
            textField.setText(history.resetHistoryLocation());
            break;
          default:
            break;
        }
      }
    });
  }

  public void append(String text) {
    SwingUtilities.invokeLater(() -> {
      String normalized = normalizer.normalize(text);
      textArea.append(addTimeStampBox.isSelected() ? addTimestamps(normalized) : normalized);
      if (autoscrollBox.isSelected()) {
        textArea.setCaretPosition(textArea.getDocument().getLength());
      }
    });
  }

  public void setConnected(boolean connected) {
    textField.setEnabled(connected);
    sendButton.setEnabled(connected);
    // Gray the output when disconnected as a visual cue. The text area itself
    // stays enabled so existing output remains selectable and searchable.
    if (connected) {
      textArea.setForeground(Color.BLACK);
      textArea.setBackground(Color.WHITE);
    } else {
      textArea.setForeground(new Color(0x40, 0x40, 0x40));
      textArea.setBackground(new Color(0xEE, 0xEE, 0xEE));
    }
  }

  private String addTimestamps(String text) {
    String now = new SimpleDateFormat("HH:mm:ss.SSS -> ").format(new Date());
    StringBuilder sb = new StringBuilder(text.length() + now.length());
    StringTokenizer tok = new StringTokenizer(text, LINE_SEPARATOR, true);
    while (tok.hasMoreTokens()) {
      if (isStartingLine) {
        sb.append(now);
      }
      String token = tok.nextToken();
      sb.append(token);
      isStartingLine = token.equals(LINE_SEPARATOR);
    }
    return sb.toString();
  }
}
