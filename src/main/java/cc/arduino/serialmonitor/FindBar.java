package cc.arduino.serialmonitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

/**
 * An inline "find" bar for a {@link JTextArea}: case-insensitive full-text
 * search with all matches highlighted, the current match emphasized, and
 * next/previous navigation with wrap-around. Hidden until {@link #open()}.
 */
public final class FindBar extends JPanel {

  private static final Color ALL_MATCHES = new Color(0xFF, 0xF3, 0x8A);
  private static final Color CURRENT_MATCH = new Color(0xFF, 0x9E, 0x2C);
  private static final Color NO_MATCH_BG = new Color(0xFF, 0xC9, 0xC9);

  private final Highlighter.HighlightPainter allPainter =
      new DefaultHighlighter.DefaultHighlightPainter(ALL_MATCHES);
  private final Highlighter.HighlightPainter currentPainter =
      new DefaultHighlighter.DefaultHighlightPainter(CURRENT_MATCH);

  private final JTextArea textArea;
  private final JTextField field = new JTextField(20);
  private final JToggleButton matchCaseToggle = new JToggleButton("Aa");
  private final JToggleButton regexToggle = new JToggleButton(".*");
  private final JLabel countLabel = new JLabel();
  private final List<Object> highlightTags = new ArrayList<>();

  private List<int[]> matches = new ArrayList<>();
  private int current = -1;
  private boolean invalidPattern = false;

  public FindBar(JTextArea textArea) {
    this.textArea = textArea;
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    setBorder(new EmptyBorder(2, 4, 2, 4));
    setVisible(false);

    JButton prev = new JButton("▲");
    JButton next = new JButton("▼");
    JButton close = new JButton("✕");
    prev.setToolTipText("Previous match (Shift+Enter)");
    next.setToolTipText("Next match (Enter)");
    close.setToolTipText("Close (Esc)");
    matchCaseToggle.setToolTipText("Match case");
    regexToggle.setToolTipText("Regular expression");
    matchCaseToggle.setSelected(Settings.getBool("find.match_case", false));
    regexToggle.setSelected(Settings.getBool("find.regex", false));

    add(new JLabel("Find: "));
    add(field);
    add(Box.createRigidArea(new Dimension(4, 0)));
    add(matchCaseToggle);
    add(regexToggle);
    add(Box.createRigidArea(new Dimension(4, 0)));
    add(prev);
    add(next);
    add(Box.createRigidArea(new Dimension(6, 0)));
    add(countLabel);
    add(Box.createHorizontalGlue());
    add(close);
    field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));

    prev.addActionListener(e -> previous());
    next.addActionListener(e -> next());
    close.addActionListener(e -> close());
    matchCaseToggle.addActionListener(e -> {
      Settings.setBool("find.match_case", matchCaseToggle.isSelected());
      recompute();
    });
    regexToggle.addActionListener(e -> {
      Settings.setBool("find.regex", regexToggle.isSelected());
      recompute();
    });

    field.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { recompute(); }
      @Override public void removeUpdate(DocumentEvent e) { recompute(); }
      @Override public void changedUpdate(DocumentEvent e) { recompute(); }
    });

    bindKey("ENTER", "findNext", () -> next());
    bindKey("shift ENTER", "findPrev", () -> previous());
    bindKey("ESCAPE", "findClose", () -> close());
  }

  private void bindKey(String keyStroke, String name, Runnable action) {
    field.getInputMap().put(KeyStroke.getKeyStroke(keyStroke), name);
    field.getActionMap().put(name, new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) { action.run(); }
    });
  }

  /** Show the bar, focus the field, and (re)run the search on current text. */
  public void open() {
    setVisible(true);
    revalidate();
    field.requestFocusInWindow();
    field.selectAll();
    recompute();
  }

  /** Hide the bar, clear highlights, and return focus to the text area. */
  public void close() {
    clearHighlights();
    matches = new ArrayList<>();
    current = -1;
    setVisible(false);
    revalidate();
    textArea.requestFocusInWindow();
  }

  /** Recompute matches for the current query against the current text. */
  private void recompute() {
    String query = field.getText();
    invalidPattern = false;
    try {
      matches = TextSearch.findMatches(textArea.getText(), query,
          matchCaseToggle.isSelected(), regexToggle.isSelected());
    } catch (PatternSyntaxException ex) {
      matches = new ArrayList<>();
      invalidPattern = true;
    }
    current = matches.isEmpty() ? -1 : 0;
    boolean flagField = !query.isEmpty() && (matches.isEmpty() || invalidPattern);
    field.setBackground(flagField ? NO_MATCH_BG : Color.WHITE);
    field.setToolTipText(invalidPattern ? "Invalid regular expression" : null);
    render();
  }

  public void next() {
    if (matches.isEmpty()) {
      return;
    }
    current = (current + 1) % matches.size();
    render();
  }

  public void previous() {
    if (matches.isEmpty()) {
      return;
    }
    current = (current - 1 + matches.size()) % matches.size();
    render();
  }

  private void render() {
    Highlighter highlighter = textArea.getHighlighter();
    clearHighlights();
    for (int i = 0; i < matches.size(); i++) {
      int[] m = matches.get(i);
      try {
        highlightTags.add(highlighter.addHighlight(
            m[0], m[1], i == current ? currentPainter : allPainter));
      } catch (BadLocationException ignored) {
        // Text changed under us; the next recompute() will resync.
      }
    }
    if (current >= 0) {
      scrollTo(matches.get(current)[0]);
      countLabel.setText((current + 1) + " / " + matches.size());
    } else if (invalidPattern) {
      countLabel.setText("bad pattern");
    } else {
      countLabel.setText(field.getText().isEmpty() ? "" : "0 results");
    }
  }

  private void scrollTo(int offset) {
    try {
      Rectangle2D r = textArea.modelToView2D(offset);
      if (r != null) {
        textArea.scrollRectToVisible(r.getBounds());
      }
    } catch (BadLocationException ignored) {
      // Offset no longer valid; ignore.
    }
  }

  private void clearHighlights() {
    Highlighter highlighter = textArea.getHighlighter();
    for (Object tag : highlightTags) {
      highlighter.removeHighlight(tag);
    }
    highlightTags.clear();
  }
}
