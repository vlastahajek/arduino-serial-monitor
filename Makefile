# Standalone Serial Monitor + Plotter — build/run helpers.
# Usage:   make run

MVN     := mvn
POM     := pom.xml
JAR     := target/serial-monitor-1.0.0.jar
MAIN    := cc.arduino.serialmonitor.SerialMonitorApp
SOURCES := $(shell find src -name '*.java' 2>/dev/null)

# Native packaging (jlink + jpackage). jpackage builds only for the host OS.
APP_NAME    := SerialMonitor
APP_VERSION := 1.0.0
VENDOR      := Vlastimil Hajek
COPYRIGHT   := © 2026 Vlastimil Hajek
DESCRIPTION := Standalone serial monitor and plotter for Arduino and other serial devices
ABOUT_URL   := https://github.com/vlastahajek/arduino-serial-monitor
DIST        := dist
RUNTIME     := target/runtime
STAGE       := target/jpackage-input
MODULES     := java.base,java.desktop,java.prefs
UNAME       := $(shell uname -s)
ifeq ($(UNAME),Darwin)
  INSTALLER_TYPE := dmg
  ICON := icon/SerialMonitor.icns
  PKG_OPTS :=
  ARCHIVE_CMD := ditto -c -k --keepParent SerialMonitor.app "$(CURDIR)/$(DIST)/serial-monitor-mac.zip"
else ifeq ($(UNAME),Linux)
  INSTALLER_TYPE := deb
  ICON := icon/SerialMonitor.png
  PKG_OPTS := --linux-package-name serial-monitor
  ARCHIVE_CMD := tar czf "$(CURDIR)/$(DIST)/serial-monitor-linux.tar.gz" SerialMonitor
else
  INSTALLER_TYPE := msi
  ICON := icon/SerialMonitor.ico
  PKG_OPTS := --win-menu --win-menu-group "Serial Monitor" --win-shortcut --win-dir-chooser
  ARCHIVE_CMD := 7z a -tzip "$(CURDIR)/$(DIST)/serial-monitor-win.zip" SerialMonitor
endif

.PHONY: all build run dev test package dist installer archive clean help

all: build

## build   : compile, test, and produce the runnable shaded jar
build: package

## package : same as build (Maven package phase → shaded jar)
package: $(JAR)

$(JAR): $(POM) $(SOURCES)
	$(MVN) -f $(POM) package

## run     : build if needed, then launch the app
run: $(JAR)
	java -jar $(JAR)

## dev     : run straight from sources via the exec plugin (no jar, skips tests)
dev:
	$(MVN) -f $(POM) -DskipTests compile exec:java

## test    : run the unit tests only
test:
	$(MVN) -f $(POM) test

# Slim bundled runtime (only the modules the app needs).
$(RUNTIME):
	rm -rf $(RUNTIME)
	jlink --add-modules $(MODULES) --strip-debug --no-header-files \
	      --no-man-pages --compress zip-6 --output $(RUNTIME)

$(STAGE)/$(notdir $(JAR)): $(JAR)
	rm -rf $(STAGE) && mkdir -p $(STAGE)
	cp $(JAR) $(STAGE)/

## dist    : build a self-contained native app image into dist/ (this OS only)
dist: $(STAGE)/$(notdir $(JAR)) $(RUNTIME)
	rm -rf $(DIST) && mkdir -p $(DIST)
	jpackage --type app-image --name $(APP_NAME) --app-version $(APP_VERSION) --vendor "$(VENDOR)" \
	         --copyright "$(COPYRIGHT)" --description "$(DESCRIPTION)" \
	         --input $(STAGE) --main-jar $(notdir $(JAR)) --main-class $(MAIN) \
	         --icon $(ICON) --runtime-image $(RUNTIME) --dest $(DIST)

## installer: build a native installer (dmg/msi/deb) into dist/ (this OS only)
installer: $(STAGE)/$(notdir $(JAR)) $(RUNTIME)
	mkdir -p $(DIST)
	jpackage --type $(INSTALLER_TYPE) --name $(APP_NAME) --app-version $(APP_VERSION) --vendor "$(VENDOR)" \
	         --copyright "$(COPYRIGHT)" --description "$(DESCRIPTION)" \
	         --input $(STAGE) --main-jar $(notdir $(JAR)) --main-class $(MAIN) \
	         --icon $(ICON) --runtime-image $(RUNTIME) --dest $(DIST) \
	         --about-url "$(ABOUT_URL)" $(PKG_OPTS)

## archive : build a portable no-install archive into dist/ (this OS only)
archive: $(STAGE)/$(notdir $(JAR)) $(RUNTIME)
	rm -rf target/appimage && mkdir -p $(DIST)
	jpackage --type app-image --name $(APP_NAME) --app-version $(APP_VERSION) --vendor "$(VENDOR)" \
	         --copyright "$(COPYRIGHT)" --description "$(DESCRIPTION)" \
	         --input $(STAGE) --main-jar $(notdir $(JAR)) --main-class $(MAIN) \
	         --icon $(ICON) --runtime-image $(RUNTIME) --dest target/appimage
	cd target/appimage && $(ARCHIVE_CMD)

## clean   : remove Maven build output and packaging artifacts
clean:
	$(MVN) -f $(POM) clean
	rm -rf $(DIST) $(RUNTIME) $(STAGE)

## help    : list targets
help:
	@grep -E '^## ' $(MAKEFILE_LIST) | sed 's/^## //'
