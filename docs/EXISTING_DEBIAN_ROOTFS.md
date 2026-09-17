# Existing Debian rootfs reuse

METMC OS checks for an existing rooted Debian installation at `/data/local/linux/rootfs` before downloading or extracting another rootfs.

A valid existing installation must contain `/etc/os-release`, `/etc/debian_version`, and an executable `/usr/bin/bash`. When found, METMC OS reuses that tree in place and does not delete, replace, or download a second rootfs.
