#!/usr/bin/env python3
"""Capture Google Play phone screenshots for Today List."""
from __future__ import annotations

import re
import subprocess
import time
from pathlib import Path

SHOTS = Path(__file__).resolve().parent
SHOTS.mkdir(parents=True, exist_ok=True)
PKG = "com.fourctech.todaylist"
ACTIVITY = f"{PKG}/.MainActivity"


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(["adb", *args], check=check, text=True, capture_output=True)


def adb_out(*args: str) -> bytes:
    return subprocess.check_output(["adb", *args])


def dump_ui() -> str:
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml", check=False)
    time.sleep(0.2)
    return adb_out("exec-out", "cat", "/sdcard/ui.xml").decode("utf-8", errors="ignore")


def nodes(xml: str) -> list[dict]:
    out = []
    for m in re.finditer(r"<node\b[^>]*>", xml):
        tag = m.group(0)

        def attr(name: str) -> str:
            mm = re.search(rf'\b{name}="([^"]*)"', tag)
            return mm.group(1) if mm else ""

        b = attr("bounds")
        bm = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)
        if not bm:
            continue
        x1, y1, x2, y2 = map(int, bm.groups())
        out.append(
            {
                "text": attr("text"),
                "desc": attr("content-desc"),
                "cls": attr("class"),
                "cx": (x1 + x2) // 2,
                "cy": (y1 + y2) // 2,
                "y1": y1,
            }
        )
    return out


def hide_keyboard() -> None:
    adb("shell", "cmd", "input_method", "hide", check=False)
    adb("shell", "input", "keyevent", "111", check=False)
    time.sleep(0.35)


def tap_xy(x: int, y: int, wait: float = 1.0) -> None:
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(wait)


def tap_desc_exact(xml: str, desc: str) -> bool:
    for n in nodes(xml):
        if n["desc"].strip().lower() == desc.strip().lower():
            print(f"TAP desc '{n['desc']}' @ {n['cx']},{n['cy']}")
            tap_xy(n["cx"], n["cy"])
            return True
    print(f"MISS desc {desc!r}")
    return False


def tap_text_exact(xml: str, text: str) -> bool:
    for n in nodes(xml):
        if n["text"].strip().lower() == text.strip().lower():
            print(f"TAP text '{n['text']}' @ {n['cx']},{n['cy']}")
            tap_xy(n["cx"], n["cy"])
            return True
    print(f"MISS text {text!r}")
    return False


def tap_bottom_nav(xml: str, label: str) -> bool:
    """Prefer bottom-nav nodes (lower third of screen)."""
    candidates = [
        n for n in nodes(xml)
        if n["text"].strip().lower() == label.lower() or n["desc"].strip().lower() == label.lower()
    ]
    if not candidates:
        print(f"MISS nav {label!r}")
        return False
    # Prefer lowest y (bottom bar)
    n = max(candidates, key=lambda c: c["y1"])
    print(f"TAP nav '{n['text'] or n['desc']}' @ {n['cx']},{n['cy']}")
    tap_xy(n["cx"], n["cy"])
    return True


def shot(name: str) -> None:
    hide_keyboard()
    path = SHOTS / f"{name}.png"
    path.write_bytes(adb_out("exec-out", "screencap", "-p"))
    print(f"SAVED {path.name} ({path.stat().st_size} bytes)")


def type_text(text: str) -> None:
    adb("shell", "input", "text", text.replace(" ", "%s"))
    time.sleep(0.25)


def ensure_launched() -> None:
    adb("shell", "am", "force-stop", PKG, check=False)
    time.sleep(0.4)
    result = adb("shell", "am", "start", "-n", ACTIVITY, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"Failed to start app: {result.stderr}")
    time.sleep(3.0)


def quick_add(title: str, later: bool = False) -> None:
    xml = dump_ui()
    if not tap_desc_exact(xml, "Quick add"):
        raise RuntimeError("FAB Quick add not found")
    time.sleep(1.0)
    xml = dump_ui()
    edits = [n for n in nodes(xml) if "EditText" in n["cls"]]
    if not edits:
        raise RuntimeError("Quick Add field missing")
    tap_xy(edits[0]["cx"], edits[0]["cy"], 0.35)
    type_text(title)
    hide_keyboard()
    if later:
        xml = dump_ui()
        if not tap_desc_exact(xml, "List Later") and not tap_text_exact(xml, "Later"):
            raise RuntimeError("Later chip missing")
        time.sleep(0.3)
    xml = dump_ui()
    if not tap_text_exact(xml, "Add"):
        raise RuntimeError("Add button missing")
    time.sleep(0.9)


def main() -> None:
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "0", check=False)
    # Clear data without uninstalling
    adb("shell", "pm", "clear", PKG, check=False)
    time.sleep(0.5)
    # pm clear can briefly break until process restarts cleanly
    ensure_launched()

    for title in ("Review inbox", "Ship store listing", "Walk outside"):
        quick_add(title)
    quick_add("Read design notes", later=True)
    quick_add("Plan weekend", later=True)

    assert tap_bottom_nav(dump_ui(), "Today")
    time.sleep(0.6)
    shot("01-today")

    assert tap_bottom_nav(dump_ui(), "Later")
    time.sleep(0.6)
    shot("02-later")

    assert tap_bottom_nav(dump_ui(), "Today")
    time.sleep(0.5)
    if not tap_desc_exact(dump_ui(), "Open Review inbox"):
        assert tap_text_exact(dump_ui(), "Review inbox")
    time.sleep(1.0)
    shot("03-task-detail")
    adb("shell", "input", "keyevent", "4")
    time.sleep(0.8)

    if not tap_desc_exact(dump_ui(), "Complete Walk outside"):
        # Fallback: first complete control
        for n in nodes(dump_ui()):
            if n["desc"].lower().startswith("complete "):
                tap_xy(n["cx"], n["cy"])
                break
    time.sleep(6.0)

    assert tap_bottom_nav(dump_ui(), "History")
    time.sleep(0.8)
    shot("05-history")

    assert tap_bottom_nav(dump_ui(), "Settings")
    time.sleep(0.8)
    shot("06-settings")

    assert tap_bottom_nav(dump_ui(), "Today")
    time.sleep(0.5)
    shot("01b-today-after-complete")

    print("DONE")
    for p in sorted(SHOTS.glob("*.png")):
        print(p.name, p.stat().st_size)


if __name__ == "__main__":
    main()
