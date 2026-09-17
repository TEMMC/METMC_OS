#!/usr/bin/env python3
"""
met-install.py -- installer for .met packages (METMC OS app format)
Created by Tinotenda Enock Mapfumo (Dr TEMMC).
Usage: python3 met-install.py path/to/app.met
Works on Linux, macOS, and Windows.
"""

import json
import os
import platform
import shutil
import subprocess
import sys
import tempfile
import zipfile


def detect_platform():
    system = platform.system().lower()
    if system == "linux":
        return "linux"
    if system == "darwin":
        return "macos"
    if system == "windows":
        return "windows"
    return None


def main():
    if len(sys.argv) != 2:
        print("Usage: python3 met-install.py path/to/app.met")
        sys.exit(1)

    met_path = sys.argv[1]
    if not os.path.isfile(met_path):
        print(f"File not found: {met_path}")
        sys.exit(1)

    current_platform = detect_platform()
    if current_platform is None:
        print("Unsupported platform. .met packages currently support Linux, macOS, and Windows.")
        print("(Android and METMC OS installs happen through the METMC OS app itself.)")
        sys.exit(1)

    with tempfile.TemporaryDirectory() as workdir:
        print(f"Extracting {met_path}...")
        with zipfile.ZipFile(met_path, "r") as z:
            z.extractall(workdir)

        manifest_path = os.path.join(workdir, "manifest.json")
        if not os.path.isfile(manifest_path):
            print("Invalid .met package: no manifest.json found.")
            sys.exit(1)

        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)

        name = manifest.get("name", os.path.basename(met_path))
        version = manifest.get("version", "unknown")
        author = manifest.get("author", "Unknown")
        platforms = manifest.get("platforms", {})

        print(f"Installing {name} (v{version}) by {author}")

        if current_platform not in platforms:
            print(f"This package does not support {current_platform}.")
            print(f"Available platforms: {', '.join(platforms.keys())}")
            sys.exit(1)

        entry_info = platforms[current_platform]
        entry_path = os.path.join(workdir, entry_info["entry"])

        if not os.path.isfile(entry_path):
            print(f"Entry file not found in package: {entry_info['entry']}")
            sys.exit(1)

        install_dir = os.path.join(
            os.path.expanduser("~"), "MetApps",
            "".join(c if c.isalnum() or c in "-_" else "_" for c in name)
        )
        os.makedirs(install_dir, exist_ok=True)

        platform_folder = os.path.join(workdir, current_platform)
        if os.path.isdir(platform_folder):
            for item in os.listdir(platform_folder):
                src = os.path.join(platform_folder, item)
                dst = os.path.join(install_dir, item)
                if os.path.isdir(src):
                    shutil.copytree(src, dst, dirs_exist_ok=True)
                else:
                    shutil.copy2(src, dst)

        script_name = os.path.basename(entry_info["entry"])
        script_target = os.path.join(install_dir, script_name)

        print(f"Running installer script: {script_target}")

        if current_platform == "windows":
            result = subprocess.run(["cmd", "/c", script_target], cwd=install_dir)
        else:
            os.chmod(script_target, 0o755)
            result = subprocess.run(["/bin/sh", script_target], cwd=install_dir)

        if result.returncode == 0:
            print(f"\n{name} installed successfully to {install_dir}")
        else:
            print(f"\nInstall script exited with code {result.returncode}")
            sys.exit(result.returncode)


if __name__ == "__main__":
    main()
