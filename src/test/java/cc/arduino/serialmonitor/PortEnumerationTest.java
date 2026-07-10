package cc.arduino.serialmonitor;

import static org.junit.Assert.assertNotNull;

import jssc.SerialPortList;
import org.junit.Test;

public class PortEnumerationTest {
  @Test
  public void getPortNamesNeverReturnsNull() {
    // jssc returns an empty array (never null) when no ports are present.
    assertNotNull(SerialPortList.getPortNames());
  }
}
