pmcaFilesystemServer
====

Simple Android app for Sony Cameras ( PlayMemories Camera App Store) that provides the File System 
of the camera via HTTP.

This app uses the [OpenMemories: Framework](https://github.com/ma1co/OpenMemories-Framework) and is 
greatly inspired by the following existing open source PMCA Apps

* [ma1co/PMCADemo](https://github.com/ma1co/PMCADemo)
* [LubikR/SynologyUploader](https://github.com/LubikR/SynologyUploader)
* [Bostwickenator/STGUploader](https://github.com/Bostwickenator/STGUploader)

# Installation 

* Use [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) 
* or through adb.
* In the future it might be available via [sony-pmca.appspot.com](https://sony-pmca.appspot.com/apps). 

# Usage

On Startup a WiFi Connection will be established. Once this succeeds a webserver is started 
and its URL is displayed. There you can download all data from the camera, like images and videos.

This works around the constraint of certain Sony cameras where videos can not be downloaded via WiFi.

# Development

```bash
adb tcpip 5555
adb connect 192.168.178.53:5555
```

See https://stackoverflow.com/a/3623727

For creating a signed release, see https://developer.android.com/studio/publish/app-signing#sign_release  
Right now, this step is not yet automated using gradle.

The app writes a log file to the SD card: `/storage/sdcard0/pmcaFilesystemServer/LOG.TXT`.

## Icon

Was generated with 
[AndroidAssetStudio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html#foreground.type=text&foreground.text.text=HTTP%20FS&foreground.text.font=Allerta%20Stencil&foreground.space.trim=1&foreground.space.pad=0.1&foreColor=rgba(96%2C%20125%2C%20139%2C%200)&backColor=rgb(139%2C%20195%2C%2074)&crop=0&backgroundShape=square&effects=none&name=ic_launcher) 

## Feature Ideas

* QR Code: https://stackoverflow.com/a/8800974/
* Basic Auth: https://github.com/NanoHttpd/nanohttpd/issues/496