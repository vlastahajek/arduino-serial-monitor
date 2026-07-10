package cc.arduino.serialmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.After;
import org.junit.Test;

public class SettingsTest {
  private static final Preferences NODE =
      Preferences.userRoot().node("cc/arduino/serialmonitor");

  @After
  public void cleanUp() throws BackingStoreException {
    NODE.remove("test.port");
    NODE.remove("test.baud");
    NODE.remove("test.flag");
    NODE.flush();
  }

  @Test public void roundTripsValues() {
    Settings.set("test.port", "/dev/ttyTEST");
    assertEquals("/dev/ttyTEST", Settings.get("test.port", "x"));
    Settings.setInt("test.baud", 115200);
    assertEquals(115200, Settings.getInt("test.baud", 9600));
    Settings.setBool("test.flag", true);
    assertTrue(Settings.getBool("test.flag", false));
  }
  @Test public void returnsDefaultWhenMissing() {
    assertEquals("fallback", Settings.get("test.absent.key", "fallback"));
  }
}
