package cc.arduino.serialmonitor;

import java.util.prefs.Preferences;

/** Thin wrapper over the JDK user-preferences store. */
public final class Settings {
  private static final Preferences PREFS =
      Preferences.userRoot().node("cc/arduino/serialmonitor");

  private Settings() {}

  public static String get(String key, String def) { return PREFS.get(key, def); }
  public static void set(String key, String val)    { PREFS.put(key, val); }
  public static int getInt(String key, int def)     { return PREFS.getInt(key, def); }
  public static void setInt(String key, int val)    { PREFS.putInt(key, val); }
  public static boolean getBool(String key, boolean def) { return PREFS.getBoolean(key, def); }
  public static void setBool(String key, boolean val)    { PREFS.putBoolean(key, val); }
}
