package cc.arduino.serialmonitor;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SerialPortsTest {

  @Test
  public void dropsMacTtyNodeWhenCuSiblingPresent() {
    List<String> out = SerialPorts.dedupe(new String[] {
        "/dev/cu.wchusbserial1", "/dev/tty.wchusbserial1"
    });
    assertEquals(Arrays.asList("/dev/cu.wchusbserial1"), out);
  }

  @Test
  public void keepsTtyNodeWithNoCuSibling() {
    List<String> out = SerialPorts.dedupe(new String[] { "/dev/tty.orphan" });
    assertEquals(Arrays.asList("/dev/tty.orphan"), out);
  }

  @Test
  public void leavesLinuxPortsUnchanged() {
    String[] in = { "/dev/ttyUSB0", "/dev/ttyACM0" };
    assertEquals(Arrays.asList(in), SerialPorts.dedupe(in));
  }

  @Test
  public void leavesWindowsPortsUnchanged() {
    String[] in = { "COM3", "COM4" };
    assertEquals(Arrays.asList(in), SerialPorts.dedupe(in));
  }

  @Test
  public void preservesOrderAndKeepsAllCuNodes() {
    List<String> out = SerialPorts.dedupe(new String[] {
        "/dev/cu.Bluetooth", "/dev/tty.Bluetooth",
        "/dev/cu.usbA", "/dev/tty.usbA"
    });
    assertEquals(Arrays.asList("/dev/cu.Bluetooth", "/dev/cu.usbA"), out);
  }
}
