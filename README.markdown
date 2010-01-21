# Synopsis

This is a helper script for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) that restores support for Web video streaming via mencoder.

# Prerequisites

ps3mencoder should work on all platforms supported by PS3 Media Server apart from Windows 98 and earlier, which aren't supported.

These instructions assume you have the latest version of [PS3 Media Server](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=3217) (PMS) and the latest version of [Java](http://www.java.com/en/download/index.jsp).

You'll need a version of perl >= 5.10.0 installed. This should be easy to find on Mac OS X and Linux if it isn't installed already. The latest version of [Strawberry Perl](http://strawberryperl.com/) (currently 5.10.1.0) is recommended for Windows. In addition, the following Perl modules are required:

* IO::All
* List::MoreUtils
* LWP::Simple
* YAML

## Windows

On Strawberry Perl, LWP::Simple and YAML are already installed. To install the other dependencies, open a [DOS prompt](http://www.computerhope.com/issues/chdos.htm) and type:

`cpan -i IO::All List::MoreUtils`

## Linux

On Ubuntu/Debian, these can be installed with apt-get or aptitude e.g:

`sudo aptitude install libio-all-perl liblist-moreutils-perl libwww-perl libyaml-perl`

Similar packages should be available for other distros.

# Installation

## Windows

* Navigate to the PMS directory - usually C:\Program Files\PS3 Media Server\
* Save [bin/ps3mencoder.bat](http://github.com/chocolateboy/ps3mencoder/raw/master/bin/ps3mencoder.bat)
  to the win32 subdirectory (e.g. alongside mencoder.exe)
* Save [conf/ps3mencoder.conf](http://github.com/chocolateboy/ps3mencoder/raw/master/conf/ps3mencoder.conf)
  to C:\Program Files\PS3 Media Server\ (e.g. alongside PMS.conf if it exists)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* Add the following line to PMS.conf (create the file/line if it doesn't exist):
  * `mencoder_path = C:\\Program Files\\PS3 Media Server\\win32\\ps3mencoder.bat`
* Restart PMS

## Linux, Mac OS X &c.

* Save [bin/ps3mencoder](http://github.com/chocolateboy/ps3mencoder/raw/master/bin/ps3mencoder)
  to somewehere sensible, e.g. /home/\<username\>/bin/ps3mencoder
* Make it executable: `chmod a+x /home/<username>/bin/ps3mencoder`
* Save [conf/ps3mencoder.conf](http://github.com/chocolateboy/ps3mencoder/raw/master/conf/ps3mencoder.conf)
  to the PMS directory ($PMS_HOME)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* Add your ps3mencoder path to $PMS_HOME/PMS.conf (create the file/line if it doesn't exist):
  * `mencoder_path = \/home\/<username>\/bin\/ps3mencoder`
* Restart PMS

# Support #

Check the ps3mencoder.log logfile in the PMS directory (i.e. the same directory as the the debug log).

For more details, discussion and troubleshooting tips, see [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002) ([start here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002#p22479)).

# Version

0.20

# License

Copyright 2009-2010 [chocolateboy](mailto:chocolate@cpan.org).

ps3mencoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
