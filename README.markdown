# Synopsis

This is a helper script for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) that restores support for Web video streaming via mencoder.

# Prerequisites

ps3mencoder should work on all platforms supported by PS3 Media Server.

These instructions assume you have the latest version of [PS3 Media Server](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=3217) (PMS) and the latest version of [Java](http://www.java.com/en/download/index.jsp).

You'll need a version of perl >= 5.10.0. This should be easy to find on Mac OS X, Linux and other *nices if it isn't installed already. The latest version of [ActivePerl](http://www.activestate.com/activeperl/) (currently 5.10.1.1006) is required for Windows. In addition, the following Perl modules are required:

* IO::All
* List::MoreUtils
* LWP::Simple
* YAML::XS

## Windows

On ActivePerl, LWP::Simple and YAML::XS are already installed. To install the other dependencies, [open a DOS prompt](http://www.computerhope.com/issues/chdos.htm) and type:

    ppm install IO::All
    ppm install List::MoreUtils

## Linux

On Ubuntu/Debian, these can be installed with apt-get or aptitude e.g:

    sudo aptitude install libio-all-perl liblist-moreutils-perl libwww-perl libyaml-libyaml-perl

Similar packages should be available for other distros.

# Installation

## Windows

* Navigate to the PMS directory ($PMS_HOME) - usually C:\Program Files\PS3 Media Server
* Save [bin/ps3mencoder](http://github.com/chocolateboy/ps3mencoder/raw/master/bin/ps3mencoder) to the win32 subdirectory (e.g. alongside mencoder.exe) as ps3mencoder.pl (rename it if it's saved with a .txt extension)
* Save [conf/ps3mencoder.conf](http://github.com/chocolateboy/ps3mencoder/raw/master/conf/ps3mencoder.conf)
  to $PMS_HOME (e.g. alongside PMS.conf if it exists)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* Add the following line to $PMS_HOME\PMS.conf (create the file/line if it doesn't exist):
  * `mencoder_path = C:\\Program Files\\PS3 Media Server\\win32\\ps3mencoder.pl`
* Restart PMS

## Linux, Mac OS X &c.

* Save [bin/ps3mencoder](http://github.com/chocolateboy/ps3mencoder/raw/master/bin/ps3mencoder)
  to a directory in $PATH e.g. /home/\<username\>/bin/ps3mencoder
* Make it executable: `chmod a+x /home/<username>/bin/ps3mencoder`
* Save [conf/ps3mencoder.conf](http://github.com/chocolateboy/ps3mencoder/raw/master/conf/ps3mencoder.conf)
  to the PMS directory ($PMS_HOME)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* Add your ps3mencoder path to $PMS_HOME/PMS.conf (create the file/line if it doesn't exist):
  * `mencoder_path = \/home\/<username>\/bin\/ps3mencoder`
* Restart PMS

# Tips

* The path to the ps3mencoder.conf file can be defined in an environment variable, $PS3MENCODER_CONF e.g.
`export PS3MENCODER_CONF=/home/<username>/lib/pms/ps3mencoder.conf`
* Similarly, if the $PMS_HOME environment variable is set, the config file is looked for in that directory e.g.
`export PMS_HOME=/home/<username>/lib/pms`
* The config file is in [YAML](http://en.wikipedia.org/wiki/YAML) format. It can have a .conf, .yml or .yaml extension
* If ps3mencoder can't find your system's mencoder, the mencoder path can be added to the configuration file

# Support #

Check the ps3mencoder.log logfile in the PMS directory (i.e. the same directory as the the debug log).

Try running ps3mencoder from the command line e.g. change to the $PMS_HOME directory and run:

`ps3mencoder http://movies.apple.com/movies/wb/inception/inception-tlr2_h640w.mov -prefer-ipv4 -nocache -quiet -oac lavc -of lavf -lavfopts format=dvd -ovc lavc -lavcopts vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128 -ofps 24000/1001 -o deleteme.mov`

For more details, discussion and troubleshooting tips, see [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002) ([start here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002#p22479)).

# Version

0.30

# License

Copyright 2009-2010 [chocolateboy](mailto:chocolate@cpan.org).

ps3mencoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
