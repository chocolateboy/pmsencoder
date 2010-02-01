# Synopsis

This is a helper script for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) that restores support for Web video streaming via mencoder.

# Prerequisites

ps3mencoder should work on all platforms supported by PS3 Media Server.

These instructions assume you have the latest version of [PS3 Media Server](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=3217) (PMS) and the latest version of [Java](http://www.java.com/en/download/index.jsp).

## Windows

There are no additional dependencies. If you have previously installed Strawberry Perl, and have no further use for it, it can safely be uninstalled. Likewise, any old versions of the ps3mencoder config file (e.g. ps3mencoder.conf) should be removed, along with any old versions of ps3mencoder.exe, ps3mencoder.pl or ps3mencoder.bat.

## Linux, Mac OS X &c.

You'll need a version of perl >= 5.10.0. This should be easy to find on Mac OS X, Linux and other *nices if it isn't installed already.

If you wish to install PS3MEncoder and its dependencies without interfering with your system's perl and its libraries, you can either set up a local perl library with [local::lib](http://FIXME), or use the latest version of ActivePerl for your platform. Since version 0.50, there is no need to manually install PS3Mencoder dependencies. They should be installed automatically as part of the installation process.
 
# Installation

## Windows

* Download and run the ps3mencoder [installer](http://cloud.github.com/downloads/chocolateboy/ps3mencoder/ps3mencoder_installer.exe)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* Add the following line to $PMS_HOME\PMS.conf (create the file/line if it doesn't exist and adjust the path accordingly if you installed PS3MEncoder somewhere else):
  * `mencoder_path = C:\\Program Files\\PS3MEncoder\\ps3mencoder.exe`
* Restart PMS

## Linux, Mac OS X &c.

* Save [bin/ps3mencoder](http://github.com/chocolateboy/ps3mencoder/raw/master/bin/ps3mencoder)
  to a directory in your $PATH e.g. /home/\<username\>/bin/ps3mencoder
* Make it executable: `chmod a+x /home/<username>/bin/ps3mencoder`
* Save [conf/ps3mencoder.conf](http://github.com/chocolateboy/ps3mencoder/raw/master/conf/ps3mencoder.conf)
  to the PMS directory ($PMS_HOME)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* Add your ps3mencoder path to $PMS_HOME/PMS.conf (create the file/line if it doesn't exist):
  * `mencoder_path = \/home\/<username>\/bin\/ps3mencoder`
* Restart PMS

# Tips
* The config file is in [YAML](http://en.wikipedia.org/wiki/YAML) format. It can have a .conf, .yml or .yaml extension

# Troubleshooting
* Check the ps3mencoder.log logfile in the current directory e.g. $PMS_HOME/ps3mencoder.log (see above)
* Try running ps3mencoder from the command line: change to the $PMS_HOME directory and run:

    `ps3mencoder "http://videos.theonion.com/onion_video/2010/01/19/LOST_FANS_ITUNES.mp4" -prefer-ipv4 -nocache -quiet -oac lavc -of lavf -lavfopts format=dvd -ovc lavc -lavcopts vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128 -ofps 24000/1001 -o deleteme.mpg`

# Support

* For more details, discussion and troubleshooting tips, see [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002) ([start here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002#p22479)).

# Version

0.50

# License

Copyright 2009-2010 [chocolateboy](mailto:chocolate@cpan.org).

ps3mencoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
