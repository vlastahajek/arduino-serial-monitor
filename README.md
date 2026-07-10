# Serial Monitor

A standalone desktop **Serial Monitor + Plotter** for talking to Arduino and
other serial devices — extracted from the Arduino IDE 1.x into a small,
self-contained app that runs on its own (no IDE required).

One window, one connection:

- **Port bar** — pick a serial port and baud rate, connect/disconnect, refresh.
  On macOS duplicate `tty.*`/`cu.*` nodes are collapsed to a single entry.
- **Monitor** — streaming text view with autoscroll, timestamps, selectable
  line endings, command history, and full-text search (`Cmd`/`Ctrl`+F) with
  match-case and regex toggles.
- **Plotter** — live graph of comma/space/tab-separated numbers (and
  `label:value` pairs); toggle between Monitor and Plotter on the same open port.

## Requirements

- A JDK (17+) with Maven for building. The app itself runs on any Java 17+
  runtime; native installers (below) bundle their own runtime.

## Build & run

```bash
make run        # build (if needed) and launch
make dev        # run straight from sources, skipping tests
make test       # run the unit tests
make build      # produce the runnable shaded jar in target/
make help       # list all targets
```

Or with Maven directly: `mvn package` then
`java -jar target/serial-monitor-1.0.0.jar`.

## Serial backend

Serial I/O uses [jssc](https://github.com/java-native/jssc), whose native
libraries are bundled inside the shaded jar and extracted at runtime, so no
extra setup is needed.

## License

This project reuses code from the Arduino IDE and the Processing project and is
distributed under the **GNU General Public License**; see [`license.txt`](license.txt).
