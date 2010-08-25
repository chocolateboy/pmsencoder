# Synopsis <a name="Synopsis"></a>

This is a plugin for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) (PMS) that adds configurable support for web video streaming via MEncoder.

# Prerequisites <a name="Prerequisites"></a>

Due to a [bug](http://code.google.com/p/ps3mediaserver/issues/detail?id=766) in the Mac OS X build of PMS, PMSEncoder doesn't currently work on Macs. In addition, PMS doesn't support web streams for the Xbox 360. PMSEncoder should work on all other platforms supported by PS3 Media Server.

These instructions assume you have the [latest version](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=3217) of PS3 Media Server, the [latest version](http://www.java.com/en/download/index.jsp) of Java, and a recent MEncoder.

# Installation <a name="Installation"></a>

* download the [PMSEncoder jar file](http://github.com/downloads/chocolateboy/pmsencoder/pmsencoder-1.1.0.jar) and place it in the PMS `plugins` directory
* shut down PMS and add `pmsencoder` to the front of the list of engines in PMS.conf e.g.
  * `engines = pmsencoder,mencoder,tsmuxer,mplayeraudio` &c.
  * if the line doesn't exist, follow the instructions [here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=7435&p=34674#p34674).
* if you previously used the standalone version of PMSEncoder, disable it by removing the `mencoder_path = /path/to/pmsencoder` line.
* restart PMS

## Upgrading <a name="Upgrading"></a>

To upgrade to a new version of the plugin, simply replace the old jar file with the new version in the `plugins` directory, and restart PMS.

# Tips <a name="Tips"></a>

* To work around the PMS [bug](http://code.google.com/p/ps3mediaserver/issues/detail?id=759) that causes web video playback to be delayed for ~40s, uncheck "HTTP Engine V2" in the PMS "General Configuration" tab. This can also be done by setting `http_engine_v2 = false` in PMS.conf. Then restart PMS.

# Troubleshooting <a name="Troubleshooting"></a>

* If PMS.conf doesn't exist, create it by clicking the "Save" icon in the PMS GUI.
* Make sure PMS is not running when editing PMS.conf.
* Make sure there's only one version of the plugin in the `plugins` directory.
* Make sure the edited PMS.conf is the one PMS is using (search the filesystem for other copies).
* Make sure `pmsencoder` is at the start of the list of engines in PMS.conf.
* Make sure `mencoder_path` is not set (or is not pointing to the standalone PMSEncoder).
* Check the PMS debug.log.
* Check the pmsencoder.log, which should be in the same location as the debug.log.

## Reporting Issues <a name="Help"></a>

Please do the following when reporting any issues:

1. restart (or start) PMS
2. try to stream a web video
3. wait until it fails
4. post your PMS.conf, pmsencoder.log and debug.log (as a zipped attachment)

## Support <a name="Support"></a>

For more details, discussion and troubleshooting tips, see the [wiki](http://wiki.github.com/chocolateboy/pmsencoder/) and [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002).

# Version <a name="Version"></a>

1.1.0

# License <a name="License"></a>

Copyright 2009-2010 [chocolateboy](mailto:chocolate@cpan.org).

PMSEncoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
