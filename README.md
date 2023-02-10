pmcaFilesystemServer
====
[![Build Status](https://travis-ci.org/schnatterer/pmcaFilesystemServer.svg?branch=develop)](https://travis-ci.org/schnatterer/pmcaFilesystemServer)

Simple Android app for Sony Cameras ( PlayMemories Camera App Store) that provides the File System 
of the camera via HTTP.

This app uses the [OpenMemories: Framework](https://github.com/ma1co/OpenMemories-Framework) and is 
greatly inspired by the following existing open source PMCA Apps

* [ma1co/PMCADemo](https://github.com/ma1co/PMCADemo)
* [LubikR/SynologyUploader](https://github.com/LubikR/SynologyUploader)
* [Bostwickenator/STGUploader](https://github.com/Bostwickenator/STGUploader)

# Pre-release of new UI

The PR for the new user interface hase been dangling for years. Now there is a pre-release that can be installed via Sony-PMCA-RE or through adb. See [#4](https://github.com/schnatterer/pmcaFilesystemServer/pull/4).

<img src="https://user-images.githubusercontent.com/1522953/88163092-7608fe80-cc12-11ea-8db7-9d0efad416be.png" height="240px">&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://user-images.githubusercontent.com/1522953/88175996-ef125100-cc26-11ea-83ce-7c54c6b1f269.png" height="240px"> 

# Installation 

* Use [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE), 
* via [sony-pmca.appspot.com](https://sony-pmca.appspot.com/apps) or 
* through adb (using [tweak app](https://github.com/ma1co/OpenMemories-Tweak)).

# Usage

On Startup a WiFi Connection will be established. Once this succeeds a webserver is started 
and its URL is displayed. There you can download all data from the camera, like images and videos.

This works around the constraint of certain Sony cameras where videos can not be downloaded via WiFi.

<font color="red">⚠</font>  The Web Server exposes the whole file system without authentication to everyone on the same network 
as the camera. Make sure to run this in a private network, using WiFi direct or by using your 
Mobile's Hotspot.

# Development

```bash
adb tcpip 5555
adb connect 192.168.178.53:5555
```

See https://stackoverflow.com/a/3623727

For creating a release, set git tag and then upload an *unsigned* APK to GitHub's release page.
Signed APKs seem to be denied by Sony-PMCA-RE.

The app writes a log file to the SD card: `/storage/sdcard0/pmcaFilesystemServer/LOG.TXT`.

## Icon

Was generated with 
[AndroidAssetStudio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html#foreground.type=text&foreground.text.text=HTTP%20FS&foreground.text.font=Allerta%20Stencil&foreground.space.trim=1&foreground.space.pad=0.1&foreColor=rgba(96%2C%20125%2C%20139%2C%200)&backColor=rgb(139%2C%20195%2C%2074)&crop=0&backgroundShape=square&effects=none&name=ic_launcher) 

## Feature Ideas

* QR Code: https://stackoverflow.com/a/8800974/
* Basic Auth: https://github.com/NanoHttpd/nanohttpd/issues/496
