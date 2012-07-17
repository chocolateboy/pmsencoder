# Synopsis <a name="Synopsis"></a>

This is a plugin for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) (PMS) that adds scriptable support for web video streaming.

# Prerequisites <a name="Prerequisites"></a>

PMS doesn't support web streams for the Xbox 360. PMSEncoder should work on all other platforms/renderers supported by PS3 Media Server.

These instructions assume you have the [latest version](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=36904#p36904) of PS3 Media Server and the [latest version](http://www.java.com/en/download/index.jsp) of Java.

# Installation <a name="Install"></a>

* download the [PMSEncoder jar file](https://github.com/downloads/chocolateboy/pmsencoder/pmsencoder-1.6.1.jar) and place it in the PMS `plugins` directory
* restart PMS

## Upgrading <a name="Upgrade"></a>

To upgrade to a new version of the plugin:

* check the [release notes](https://github.com/chocolateboy/pmsencoder/wiki/Release-Notes) to see if there are any breaking changes or other incompatibilities
* replace the old jar file in the `plugins` directory with the [new version](https://github.com/downloads/chocolateboy/pmsencoder/pmsencoder-1.6.1.jar) and restart PMS

## Uninstalling <a name="Uninstall"></a>

To uninstall PMSEncoder, remove the jar file from the `plugins` directory.

## Building <a name="Build"></a>

To build PMSEncoder from source, see the [Wiki](https://github.com/chocolateboy/pmsencoder/wiki/Development).

# Tips <a name="Tips"></a>

* To work around the PMS [bug](http://code.google.com/p/ps3mediaserver/issues/detail?id=759) that causes web video playback to be delayed for ~40s, uncheck "HTTP Engine V2" in the PMS "General Configuration" tab. This can also be done by setting `http_engine_v2 = false` in PMS.conf. Then restart PMS. See [below](#HTTPEngine) for caveats.
* To take PMSEncoder for a spin, try [this WEB.conf](https://raw.github.com/chocolateboy/pmsencoder/release/misc/conf/WEB.conf), which contains a list of feeds that are regularly tested.
* For help with particular feeds/streams/sites, see [here](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776&p=46696#p46696).


# Troubleshooting <a name="Troubleshooting"></a>

* Make sure you're using the [latest version of PMS](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=38376#p38376) and the latest version of PMSEncoder.
* Make sure the [WEB.conf](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=64418#p64418) is in the profile directory (see [here](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=32731#p32731) for the default locations).
* Make sure there's only one version of the plugin in the `plugins` directory.
* Make sure the error is reproducible after a PMS restart. Caching in PMS and/or the PS3 often produces one-off errors.
* <a name="HTTPEngine"></a>If you [disabled "HTTP Engine V2"](#Tips), try re-enabling it as some renderers, such as the Sony Bravia KDL-37V5500, require it.
* Make sure `mencoder_path` is not set (or is not pointing to the old, standalone version of PMSEncoder).
* Internet Explorer saves .jar files as .zip files. Either save the file with a [different](http://www.mozilla.com/firefox/) [browser](http://www.google.com/chrome), or rename it, replacing the .zip extension with .jar.
* Check the PMS trace, PMS debug.log, and pmsencoder.log (which should be in the same directory as the debug.log) for
  errors.

## Reporting Issues <a name="Help"></a>

Please do the following when reporting any issues:

1. restart (or start) PMS
2. try to stream a web video
3. wait until it fails
4. post your PMS.conf, WEB.conf, pmsencoder.log and debug.log as a zipped attachment

## Support <a name="Support"></a>

For more details, discussion and troubleshooting tips, see the [wiki](http://wiki.github.com/chocolateboy/pmsencoder/) and [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776) (start [here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776#p22479)).

# Version <a name="Version"></a>

1.6.1

# License <a name="License"></a>

Copyright 2009-2012 [chocolateboy](mailto:chocolate@cpan.org).

PMSEncoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
