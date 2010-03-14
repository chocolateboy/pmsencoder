# Synopsis <a name="Synopsis"></a>

This is a helper script for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) that restores support for Web video streaming via MEncoder.

# Prerequisites <a name="Prerequisites"></a>

PMSEncoder should work on all platforms supported by PS3 Media Server.

These instructions assume you have the latest version of [PS3 Media Server](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=3217) (PMS) and the latest version of [Java](http://www.java.com/en/download/index.jsp).

## Windows <a name="Windows"></a>

There are no additional dependencies.

## Linux, Mac OS X &c. <a name="Linux, Mac OS X &c."></a>

You'll need a version of perl >= 5.10.0. This should be easy to find on Mac OS X, Linux and other *nixes if it isn't installed already (run `perl -v` to check).

# Installation <a name="Installation"></a>

## Windows <a name="Windows"></a>

* Download and run the PMSEncoder [installer](http://cloud.github.com/downloads/chocolateboy/pmsencoder/PMSEncoder-0.70.exe)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* [Locate your PMS.conf](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=5301) and add your pmsencoder path e.g.:
  * `mencoder_path = C:\\Program Files\\PMSEncoder\\pmsencoder.exe`
* Restart PMS

## Linux, Mac OS X &c. <a name="Linux, Mac OS X &c."></a>

For now, PMSEncoder will need to be installed from source e.g.

  * Download the latest copy of [cpanminus](http://github.com/miyagawa/cpanminus) e.g. `wget http://cpanmin.us`
  * `chmod +x cpanm`
  * `cpanm --install http://github.com/chocolateboy/pmsencoder/tarball/master`
* Save your settings and quit PMS
* Add your pmsencoder path to your PMS.conf e.g.:
  * `mencoder_path = \/home\/<username>\/perl5\/bin\/pmsencoder`
* Restart PMS

# Tips <a name="Tips"></a>
* Use `pmsencoder --version` to see configuration details e.g. to find out the location of the logfile
* mencoder is looked for in following places (in order):
  * the `mencoder_path` specified in the config file
  * the path specified in the PMSENCODER_PATH environment variable
  * the current working directory
  * the directories specified in the PATH environment variable
  * (on Windows) the $PMSENCODER/res directory
* The config file is looked for in the following locations:
  * the path in the PMSENCODER_CONFIG environment variable
  * the "user config directory" setting displayed by `pmsencoder --version`
  * the "default config file" displayed by `pmsencoder --version`
* The config file is in [YAML](http://en.wikipedia.org/wiki/YAML) format. It can have a .conf, .yml or .yaml extension
* Don't modify the default config file. Copy (don't link) it to the directory listed as "user config dir" in the output of `pmsencoder --version` and modify the copy. Delete the copy if you wish to revert to the default configuration.

# Troubleshooting <a name="Troubleshooting"></a>
* Check the pmsencoder.log logfile (see the output of `pmsencoder --version` for the path)
* Run `pmsencoder --test` to test pmsencoder from the command line
* Check the PMS debug.log

# Support <a name="Support"></a>

* For more details, discussion and troubleshooting tips, see [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002) ([start here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002#p22479)).

# Version <a name="Version"></a>

0.70

# License <a name="License"></a>

Copyright 2009-2010 [chocolateboy](mailto:chocolate@cpan.org).

PMSEncoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
