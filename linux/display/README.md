# METMC Linux Display

v6 graphical Linux display layer.

The display architecture is intentionally independent of Android DRM/KMS.
Linux application output will eventually be bridged into an Android surface
managed by METMC OS.

Target:

Linux application
    -> display bridge
    -> METMC window
    -> Android Surface
