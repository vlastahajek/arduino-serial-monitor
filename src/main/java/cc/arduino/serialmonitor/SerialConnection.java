package cc.arduino.serialmonitor;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/** Minimal serial wrapper over jssc with a streaming UTF-8 decoder. */
public class SerialConnection implements SerialPortEventListener {

  private static final int IN_BUFFER_CAPACITY = 128;
  private static final int OUT_BUFFER_CAPACITY = 128;

  private final Consumer<String> onText;
  private final ByteBuffer inFromSerial = ByteBuffer.allocate(IN_BUFFER_CAPACITY);
  private final CharBuffer outToMessage = CharBuffer.allocate(OUT_BUFFER_CAPACITY);
  private final CharsetDecoder bytesToStrings;

  private SerialPort port;

  public SerialConnection(Consumer<String> onText) {
    this.onText = onText;
    this.bytesToStrings = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
        .replaceWith("⸮");
  }

  public void open(String portName, int baud) throws SerialPortException {
    port = new SerialPort(portName);
    try {
      port.openPort();
      // N-8-1, RTS on, DTR on (fixed; no board-preference lookups).
      port.setParams(baud, 8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, true, true);
      port.addEventListener(this);
    } catch (SerialPortException e) {
      try { if (port.isOpened()) port.closePort(); } catch (SerialPortException ignored) { }
      port = null;
      throw e;
    }
  }

  public boolean isOpen() {
    return port != null && port.isOpened();
  }

  @Override
  public synchronized void serialEvent(SerialPortEvent event) {
    if (event.isRXCHAR()) {
      try {
        byte[] buf = port.readBytes(event.getEventValue());
        if (buf != null) {
          feed(buf);
        }
      } catch (SerialPortException e) {
        System.err.println("Error reading serial: " + e.getMessage());
      }
    }
  }

  /** Decode bytes (handling multibyte chars split across calls) and emit text. */
  synchronized void feed(byte[] buf) {
    int next = 0;
    while (next < buf.length || inFromSerial.position() > 0) {
      do {
        int copyNow = Math.min(buf.length - next, inFromSerial.remaining());
        inFromSerial.put(buf, next, copyNow);
        next += copyNow;
        inFromSerial.flip();
        bytesToStrings.decode(inFromSerial, outToMessage, false);
        inFromSerial.compact();
      } while (next < buf.length && outToMessage.hasRemaining());

      if (outToMessage.position() == 0) {
        break;
      }
      outToMessage.flip();
      char[] chars = new char[outToMessage.remaining()];
      outToMessage.get(chars);
      onText.accept(new String(chars));
      outToMessage.clear();
    }
  }

  public void write(String s) {
    if (port == null) return;
    try {
      port.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    } catch (SerialPortException e) {
      System.err.println("Error writing serial: " + e.getMessage());
    }
  }

  public void setDTR(boolean state) { safe(() -> port.setDTR(state)); }
  public void setRTS(boolean state) { safe(() -> port.setRTS(state)); }

  private interface PortAction { void run() throws SerialPortException; }
  private void safe(PortAction a) {
    if (port == null) return;
    try { a.run(); } catch (SerialPortException e) {
      System.err.println("Serial control error: " + e.getMessage());
    }
  }

  public synchronized void close() {
    try {
      if (port != null) {
        // Best-effort cleanup. On a physical disconnect the OS port is already
        // gone, so these routinely fail ("Port not opened") — that is expected
        // during a disconnect, not an error worth reporting. Remove the
        // listener first (while the port is still nominally open), then close.
        try {
          port.removeEventListener();
        } catch (SerialPortException ignored) {
          // listener already gone / port not open
        }
        try {
          if (port.isOpened()) {
            port.closePort();
          }
        } catch (SerialPortException ignored) {
          // port already gone
        }
      }
      // Flush streaming decoder: emit any bytes buffered mid-multibyte-sequence on disconnect.
      inFromSerial.flip();
      bytesToStrings.decode(inFromSerial, outToMessage, true);
      bytesToStrings.flush(outToMessage);
      if (outToMessage.position() > 0) {
        outToMessage.flip();
        char[] chars = new char[outToMessage.remaining()];
        outToMessage.get(chars);
        onText.accept(new String(chars));
        outToMessage.clear();
      }
      bytesToStrings.reset();
      inFromSerial.clear();
    } finally {
      port = null;
    }
  }
}
