# Synopsis

This is a helper script for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) that restores support for Web video streaming via mencoder.

# Installation

These instructions assume you have the latest version of [PS3 Media Server](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=3217) (PMS) and the latest version of [Java](http://www.java.com/en/download/index.jsp).

## Windows ###

* Navigate to the PMS directory - usually C:\Program Files\PS3 Media Server
* Save [bin/ps3mencoder.exe](http://github.com/chocolateboy/ps3mencoder/raw/master/bin/ps3mencoder.exe)
  to the win32 subdirectory (e.g. alongside mencoder.exe)
* Save [conf/ps3mencoder.conf](http://github.com/chocolateboy/ps3mencoder/raw/master/conf/ps3mencoder.conf)
  to the current directory (e.g. alongside PMS.conf if it exists)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Quit PMS
* Add the following line to PMS.conf (create the file/line if it doesn't exist):
  * `mencoder_path = C:\\Program Files\\PS3 Media Server\\win32\\ps3mencoder.exe`
* Restart PMS

## Linux, Mac OS X &c.

You'll need a version of perl installed, preferably a recent one. The following modules are required:

* File::Which
* IO::All
* List::MoreUtils
* LWP::Simple
* YAML

On Debian/Ubuntu these can be installed with:

`sudo aptitude install libfile-which-perl libio-all-perl liblist-moreutils-perl libwww-perl libyaml-perl`

* Save [bin/ps3mencoder](http://github.com/chocolateboy/ps3mencoder/raw/master/bin/ps3mencoder)
  e.g. to /home/user/bin/ps3mencoder
* Make it executable: `chmod a+x /home/user/bin/ps3mencoder`
* Save [conf/ps3mencoder.conf](http://github.com/chocolateboy/ps3mencoder/raw/master/conf/ps3mencoder.conf)
  to the PMS directory ($PMS_HOME)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Quit PMS
* Add your ps3mencoder path to $PMS_HOME/PMS.conf (create the file/line if it doesn't exist):
  * `mencoder_path = \/home\/user\/bin\/ps3mencoder`
* Restart PMS

# Support #

Check the ps3mencoder.log logfile in the PMS directory (i.e. the same directory as the the debug log).

For more details, discussion and troubleshooting tips, see [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002) ([start here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002#p22479)).

# LICENSE

Copyright 2009 [chocolateboy](http://github.com/chocolateboy).

ps3mencoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
