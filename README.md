# PMSEncoder

[![Build Status](https://travis-ci.org/chocolateboy/pmsencoder.svg?branch=master)](https://travis-ci.org/chocolateboy/pmsencoder)

- [Description](#description)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
	- [Upgrading](#upgrading)
	- [Uninstalling](#uninstalling)
	- [Building](#building)
- [Tips](#tips)
	- [Downloaders](#downloaders)
	- [Scripting](#scripting)
- [Troubleshooting](#troubleshooting)
	- [Reporting Issues](#reporting-issues)
	- [Support](#support)
- [Version](#version)
- [License](#license)

## Description

This is a plugin for [PS3 Media Server](https://github.com/ps3mediaserver/ps3mediaserver#readme) (PMS) that adds scriptable support for web video streaming.

## Prerequisites

PMSEncoder should work on all platforms/renderers supported by PS3 Media Server.

These instructions assume you have the [latest version](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=36904#p36904) of PS3 Media Server and the [latest version](http://www.java.com/en/download/index.jsp) of Java.

## Installation

* download the [PMSEncoder jar file](https://github.com/chocolateboy/pmsencoder/releases/download/v2.0.0/pmsencoder-2.0.0.jar) and place it in the PMS [`plugins`](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=32731#p32731) directory
* restart PMS

### Upgrading

To upgrade to a new version of the plugin:

* check the [release notes](https://github.com/chocolateboy/pmsencoder/wiki/Release-Notes) to see if there are any breaking changes or other incompatibilities
* replace the old jar file in the [`plugins`](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=32731#p32731) directory with the [new version](https://github.com/chocolateboy/pmsencoder/releases/download/v2.0.0/pmsencoder-2.0.0.jar) and restart PMS

### Uninstalling

To uninstall PMSEncoder, remove the jar file from the [`plugins`](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=32731#p32731) directory.

### Building

To build PMSEncoder from source, see the [Wiki](https://github.com/chocolateboy/pmsencoder/wiki/Development).

## Tips

* To work around the PMS [bug](http://code.google.com/p/ps3mediaserver/issues/detail?id=759) that causes web video playback to be delayed for ~40s, uncheck "HTTP Engine V2" in the PMS "General Configuration" tab. This can also be done by setting `http_engine_v2 = false` in `PMS.conf`. Then restart PMS. See [below](#http-engine) for caveats.
* To take PMSEncoder for a spin, try [this `WEB.conf`](https://raw.github.com/chocolateboy/pmsencoder/master/misc/conf/WEB.conf), which contains a list of feeds that are regularly tested.
* For help with particular feeds/streams/sites, see [here](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776&p=46696#p46696).
* For details on configuring PMSEncoder, see [here](https://github.com/chocolateboy/pmsencoder/wiki/PMS.conf-options).

### Downloaders

While PMSEncoder has basic support for a few sites built-in, it can be configured to support a much wider range of sites by means of external downloaders. For example, [youtube-dl](http://rg3.github.io/youtube-dl/) is highly recommended since it supports a much wider range of YouTube videos and, unlike PMSEncoder's built-in YouTube support, is regularly updated to keep track of changes on the YouTube site. It also supports many other sites that are not natively supported by PMSEncoder. Setup is easy: download youtube-dl, close PMS, and add something like the following to your `PMS.conf`:

    Windows:

        # make sure there are no spaces in the path
        youtube-dl.path = C:\\ProgramData\\PMS\\youtube-dl.exe

    Linux, Mac OS X &c.:

        youtube-dl.path = /path/to/youtube-dl

Alternatively, PMSEncoder will automatically detect downloaders if they're a) in a location in the `PATH` environment variable or b) placed in the PMS `plugins` folder.

Several downloaders are supported. See [here](https://github.com/chocolateboy/pmsencoder/wiki/PMS.conf-options#application-settings-) for more details.

### Scripting

PMSEncoder can be configured to support any video site by writing a small script, similar to a Greasemonkey-style userscript. Scripts are written in [Groovy](http://groovy.codehaus.org/) with a page-scraping syntax very similar to jQuery. See the source of the [builtin scripts](https://github.com/chocolateboy/pmsencoder/tree/master/src/main/resources/scripts) for more details and [here](https://github.com/chocolateboy/pmsencoder/wiki/PMS.conf-options#pmsencoderscriptdirectory-) for details on where to add your own scripts.

In addition, PMSEncoder scripts can be used to change or customize transcoding commands for local files and to dynamically script all aspects of PMS. Almost all of PMSEncoder's own settings are implemented in scripts and can be customized and overridden by user-created scripts.

## Troubleshooting

* For help with particular feeds/streams/sites, see [here](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776&p=46696#p46696).
* Make sure you're using the [latest version of PMS](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=38376#p38376) and the latest version of PMSEncoder.
* Make sure the [WEB.conf](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=64418#p64418) is in the profile directory (see [here](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=32731#p32731) for the default locations).
* Make sure there's only one version of the plugin in the `plugins` directory.
* Make sure PMSEncoder is first in the list of web video engines.
* Make sure the error is reproducible after a PMS restart. Caching in PMS and/or the PS3 often produces one-off errors.
* <a name="http-engine"></a>If you [disabled "HTTP Engine V2"](#tips), try re-enabling it as some renderers, such as the Sony Bravia KDL-37V5500, require it.
* Internet Explorer saves .jar files as .zip files. Either save the file with a [different](http://www.mozilla.com/firefox/) [browser](http://www.google.com/chrome), or rename it, replacing the .zip extension with .jar.

### Reporting Issues

First of all, please make sure you've followed all of the steps in the [Troubleshooting](#troubleshooting) section before reporting issues.

If the issue still remains:

1. restart (or start) PMS
2. try to stream a web video
3. wait until it fails
3. stop PMS
5. upload the **full** [`debug.log`](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=38484#p38484), [`PMS.conf`](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=32731#p32731) and  [`pmsencoder.log`](https://github.com/chocolateboy/pmsencoder/wiki/PMS.conf-options#pmsencoderlogdirectory-) to [Pastebin.com](http://pastebin.com) or [MediaFire](http://www.mediafire.com/)
6. create a new post in [this thread](http://www.ps3mediaserver.org/forum/viewtopic.php?f=12&t=6644) describing the problem, along with links to the uploaded logfiles and
any relevant [`WEB.conf`](http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=64418#p64418) entries

### Support

For more details, discussion and troubleshooting tips, see the [wiki](http://wiki.github.com/chocolateboy/pmsencoder/) and [this thread](http://www.ps3mediaserver.org/forum/viewtopic.php?f=12&t=6644).

## Version

2.0.0

## License

Copyright 2009-2014 [chocolateboy](mailto:chocolate@cpan.org).

PMSEncoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
