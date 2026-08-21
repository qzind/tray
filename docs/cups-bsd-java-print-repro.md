# CUPS-BSD Java Print Repro

Small repro for Linux Java pixel printing when `/usr/bin/lpr` is missing

Target: Raspberry Pi OS, DietPi, Debian, or Ubuntu Server with CUPS working and
`cups-bsd` absent

## Prepare CUPS

```bash
sudo apt update
sudo apt install cups cups-client printer-driver-gutenprint
sudo systemctl enable --now cups
```

Add a real printer or CUPS test queue

Check the queue:

```bash
lpstat -p -d
```

Check `lp` printing:

```bash
lp -d <printer-name> /etc/hosts
```

## Break lpr

```bash
sudo apt remove cups-bsd
```

Do not install the standalone `lpr` package

Check `lp` exists and `lpr` is missing:

```bash
which lp
which lpr
```

Expected:

- `which lp` prints `/usr/bin/lp` or similar
- `which lpr` prints nothing and exits non-zero

## Run Java

Manual smoke file:

```text
test/manual/JavaPrintSmoke.java
```

Compile and run:

```bash
mkdir -p out/manual
javac -d out/manual test/manual/JavaPrintSmoke.java
java -cp out/manual manual.JavaPrintSmoke
```

Expected missing-`cups-bsd` failures:

- `java.awt.print.PrinterIOException`
- nested `java.io.IOException` mentioning `/usr/bin/lpr`
- error mentioning `lpr: command not found`

## Fix

```bash
sudo apt install cups-bsd
which lpr
java -cp out/manual manual.JavaPrintSmoke
```

The missing-`lpr` failure should be gone
