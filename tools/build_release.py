#!/usr/bin/env python3
"""
Baut die Version zum Weitergeben an Freunde:

    python3 tools/build_release.py

Ergebnis:
    target/DroidRampage.jar   – ein einziges .jar, läuft auf Windows, macOS (Intel + Apple Silicon) und Linux
    target/DroidRampage.zip   – das .jar plus Start-Skripte, die Java selbst herunterladen, falls es fehlt

Warum ein extra Skript statt nur "mvnw package"?
JavaFX liefert für Intel-Macs und Apple-Silicon-Macs native Bibliotheken mit *gleichem* Dateinamen
(z.B. libglass.dylib). In einem einzigen .jar kann nur eine davon liegen. Dieses Skript fügt beide
Varianten zu einer "Universal Binary" zusammen (wie Apples lipo-Tool), damit das .jar auf beiden Macs läuft.
"""
import re
import shutil
import stat
import struct
import subprocess
import sys
import time
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TARGET = ROOT / "target"
JAR = TARGET / "DroidRampage.jar"
M2 = Path.home() / ".m2" / "repository" / "org" / "openjfx"
MAC_MODULES = ["javafx-graphics", "javafx-media"]  # nur diese enthalten native Mac-Bibliotheken

FAT_MAGIC = 0xCAFEBABE
MACHO_64_MAGIC = 0xFEEDFACF
SLICE_ALIGN = 14  # 2^14 = 16 KiB – Seitengröße von Apple Silicon


def javafx_version() -> str:
    pom = (ROOT / "pom.xml").read_text(encoding="utf-8")
    m = re.search(r"<javafx.version>([^<]+)</javafx.version>", pom)
    if not m:
        sys.exit("javafx.version not found in pom.xml")
    return m.group(1)


def macho_cpu(data: bytes):
    magic, cputype, cpusubtype = struct.unpack_from("<Iii", data, 0)
    if magic != MACHO_64_MAGIC:
        raise ValueError("not a thin 64-bit Mach-O file")
    return cputype, cpusubtype


def make_universal(slices: list) -> bytes:
    """Baut eine Mach-O Universal Binary (Fat-Header + ausgerichtete Slices)."""
    header = struct.pack(">II", FAT_MAGIC, len(slices))
    offset = 4096
    arch_table, body_parts = b"", []
    pos = offset
    for data in slices:
        align = 1 << SLICE_ALIGN
        pos = (pos + align - 1) // align * align
        cputype, cpusubtype = macho_cpu(data)
        arch_table += struct.pack(">iiIII", cputype, cpusubtype, pos, len(data), SLICE_ALIGN)
        body_parts.append((pos, data))
        pos += len(data)
    out = bytearray(pos)
    out[0:len(header) + len(arch_table)] = header + arch_table
    for p, data in body_parts:
        out[p:p + len(data)] = data
    return bytes(out)


def mac_libs(version: str, classifier: str) -> dict:
    libs = {}
    for module in MAC_MODULES:
        jar = M2 / module / version / f"{module}-{version}-{classifier}.jar"
        if not jar.exists():
            sys.exit(f"missing {jar} – run ./mvnw package first")
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if name.endswith(".dylib"):
                    libs[name] = z.read(name)
    return libs


def patch_jar(version: str):
    intel = mac_libs(version, "mac")
    arm = mac_libs(version, "mac-aarch64")
    universal = {}
    for name in sorted(set(intel) | set(arm)):
        if name in intel and name in arm:
            universal[name] = make_universal([intel[name], arm[name]])
        else:
            print(f"  warning: {name} only exists for one Mac architecture")
            universal[name] = intel.get(name) or arm.get(name)

    tmp = JAR.with_suffix(".tmp")
    with zipfile.ZipFile(JAR) as src, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            if item.filename in universal:
                continue
            dst.writestr(item, src.read(item.filename))
        for name, data in universal.items():
            dst.writestr(name, data)
    tmp.replace(JAR)
    print(f"  {len(universal)} Mac libraries merged into universal binaries (Intel + Apple Silicon)")


def build_zip():
    out_dir = TARGET / "release" / "DroidRampage"
    if out_dir.parent.exists():
        shutil.rmtree(out_dir.parent)
    out_dir.mkdir(parents=True)

    launcher = ROOT / "launcher"
    files = {
        JAR: "DroidRampage.jar",
        launcher / "Start-Windows.bat": "Start-Windows.bat",
        launcher / "start.sh": "Start-Mac.command",
        launcher / "HOW-TO-PLAY.txt": "HOW-TO-PLAY.txt",
    }
    for src, name in files.items():
        shutil.copy2(src, out_dir / name)
    shutil.copy2(launcher / "start.sh", out_dir / "start-linux.sh")

    zip_path = TARGET / "DroidRampage.zip"
    zip_path.unlink(missing_ok=True)
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(out_dir.iterdir()):
            info = zipfile.ZipInfo(f"DroidRampage/{f.name}", date_time=time.localtime()[:6])
            info.compress_type = zipfile.ZIP_DEFLATED
            executable = f.suffix in (".command", ".sh")
            mode = 0o755 if executable else 0o644
            info.external_attr = (stat.S_IFREG | mode) << 16  # Ausführrechte für Mac/Linux mitspeichern
            info.create_system = 3  # Unix
            z.writestr(info, f.read_bytes())
    print(f"  {zip_path.relative_to(ROOT)}  ({zip_path.stat().st_size / 1e6:.1f} MB)")


def main():
    mvnw = ROOT / ("mvnw.cmd" if sys.platform.startswith("win") else "mvnw")
    print("Building jar ...")
    subprocess.run([str(mvnw), "-q", "-DskipTests", "package"], cwd=ROOT, check=True)
    print("Making Mac libraries universal ...")
    patch_jar(javafx_version())
    print(f"  {JAR.relative_to(ROOT)}  ({JAR.stat().st_size / 1e6:.1f} MB)")
    print("Packing zip with start scripts ...")
    build_zip()
    print("Done.")


if __name__ == "__main__":
    main()
