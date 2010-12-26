# Synopsis <a name="Synopsis"></a>

This is a plugin for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) (PMS) that adds configurable support for web video streaming.

# Prerequisites <a name="Prerequisites"></a>

Due to an [issue](http://code.google.com/p/ps3mediaserver/issues/detail?id=766) in the Mac OS X build of PMS, PMSEncoder doesn't currently work on Macs. In addition, PMS doesn't support web streams for the Xbox 360. PMSEncoder should work on all other platforms/renderers supported by PS3 Media Server.

These instructions assume you have the [latest version](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=3217) of PS3 Media Server, the [latest version](http://www.java.com/en/download/index.jsp) of Java, and a recent MEncoder.

# Installation <a name="Install"></a>

* download the [PMSEncoder jar file](http://github.com/downloads/chocolateboy/pmsencoder/pmsencoder-1.2.5.jar) and place it in the PMS `plugins` directory
* shut down PMS and add `pmsencoder` to the front of the list of engines in PMS.conf e.g.
  * `engines = pmsencoder,mencoder,tsmuxer,mplayeraudio` &c.
* restart PMS

## Upgrading <a name="Upgrade"></a>

To upgrade to a new version of the plugin, simply replace the old jar file in the `plugins` directory with the [new version](http://github.com/downloads/chocolateboy/pmsencoder/pmsencoder-1.2.5.jar) and restart PMS.

## Uninstalling <a name="Uninstall"></a>

To uninstall PMSEncoder, remove the jar file from the `plugins` directory and remove `pmsencoder,` from the list of engines in
`PMS.conf`.

## Building <a name="Build"></a>

To build PMSEncoder from source, see the [Wiki](https://github.com/chocolateboy/pmsencoder/wiki/Development).

# Tips <a name="Tips"></a>

* To work around the PMS [bug](http://code.google.com/p/ps3mediaserver/issues/detail?id=759) that causes web video playback to be delayed for ~40s, uncheck "HTTP Engine V2" in the PMS "General Configuration" tab. This can also be done by setting `http_engine_v2 = false` in PMS.conf. Then restart PMS. See [below](#HTTPEngine) for caveats.
* To take PMSEncoder for a spin, try [this WEB.conf](http://github.com/chocolateboy/pmsencoder/raw/master/misc/conf/WEB.conf), which contains a list of feeds that are regularly tested and updated.
* The [Community Beta](https://code.google.com/p/ps3mediaservercontrib/) of PMS includes a [patch](https://code.google.com/p/ps3mediaserver/issues/detail?id=757) that
  restores support for [GameTrailers feeds](http://www.gametrailers.com/rssgenform.php).

# Troubleshooting <a name="Troubleshooting"></a>

* Internet Explorer saves .jar files as .zip files. Either save the file with a [different](http://www.mozilla.com/firefox/) [browser](http://www.google.com/chrome), or rename it, replacing the .zip extension with .jar.
* <a name="HTTPEngine"></a>If you [disabled "HTTP Engine V2"](#Tips), try re-enabling it as some renderers, such as the Sony Bravia KDL-37V5500, require it.
* If PMS.conf doesn't exist (see [here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=32731#p32731) for the default locations), create it by clicking the "Save" icon in the PMS GUI.
* If the list of engines doesn't exist in PMS.conf, follow the instructions [here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=7435&p=34674#p34674).
* Make sure there's only one version of the plugin in the `plugins` directory.
* Make sure the edited PMS.conf is the one PMS is using (search the filesystem for other copies).
* Make sure `pmsencoder` is still first in the engines list.
* Make sure `mencoder_path` is not set (or is not pointing to the old, standalone version of PMSEncoder).
* Make sure the error is reproducible after a PMS restart. Caching in PMS and/or the PS3 often produces one-off errors.
* Check the PMS trace, PMS debug.log, and pmsencoder.log (which should be in the same directory as the debug.log) for
  errors.

## Reporting Issues <a name="Help"></a>

Please do the following when reporting any issues:

1. restart (or start) PMS
2. try to stream a web video
3. wait until it fails
4. post your PMS.conf, pmsencoder.log and debug.log (as a zipped attachment)

## Support <a name="Support"></a>

For more details, discussion and troubleshooting tips, see the [wiki](http://wiki.github.com/chocolateboy/pmsencoder/) and [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776) (start [here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776#p22479)).

# Version <a name="Version"></a>

1.2.5

# License <a name="License"></a>

Copyright 2009-2010 [chocolateboy](mailto:chocolate@cpan.org).

PMSEncoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
