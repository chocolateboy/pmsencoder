# Synopsis

This is a helper script for [PS3 Media Server](http://code.google.com/p/ps3mediaserver/) that restores support for Web video streaming via MEncoder.

# Prerequisites

PMSEncoder should work on all platforms supported by PS3 Media Server.

These instructions assume you have the latest version of [PS3 Media Server](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=3217) (PMS) and the latest version of [Java](http://www.java.com/en/download/index.jsp).

## Windows

There are no additional dependencies.

## Linux, Mac OS X &c.

You'll need a version of perl >= 5.10.0. This should be easy to find on Mac OS X, Linux and other *nixes if it isn't installed already.

If you wish to install PMSEncoder and its dependencies without interfering with your system's Perl libraries (**recommended**), follow [these instructions](http://perl.jonallen.info/writing/articles/install-perl-modules-without-root) (but **don't install Catalyst**, and you can skip Acme::Time::Baby). Since version 0.50, there is no need to manually install PMSEncoder dependencies. They should be installed automatically as part of the installation process.

The [pip](http://search.cpan.org/perldoc?pip) installer is required. This may be available in packaged form on some platforms, but the latest version from CPAN is recommended, particularly as there are at least two other applications called pip. In addition, installing pip from CPAN verifies that your CPAN setup works. To install pip, open a console and enter: `cpan -i pip`

# Installation

## Windows

* Download and run the PMSEncoder [installer](http://cloud.github.com/downloads/chocolateboy/pmsencoder/PMSEncoder-0.60.exe)
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* [Locate your PMS.conf](http://ps3mediaserver.org/forum/viewtopic.php?f=2&t=5301) and add your pmsencoder path e.g.:
  * `mencoder_path = C:\\Program Files\\PMSEncoder\\pmsencoder.exe`
* Restart PMS

## Linux, Mac OS X &c.

* Install PMSEncoder: `pip -i http://cloud.github.com/downloads/chocolateboy/pmsencoder/PMSEncoder-0.60.tar.gz`
* Move "MEncoder Web" to the top of the list of "Video Web Streaming Engines" on the PMS "Transcoding Settings" tab
* Save your settings and quit PMS
* Add your pmsencoder path to your PMS.conf e.g.:
  * `mencoder_path = \/home\/<username>\/perl5\/bin\/pmsencoder`
* Restart PMS

# Tips
* Use `pmsencoder --version` to see configuration details e.g. to find out the location of the logfile
* The config file is looked for in the path specified in the PMSENCODER_CONFIG environment variable (which should specify the file's full path), followed by the user config directory (shown in the output of `pmsencoder --version`). If no custom config file is specified for either of these, the default config file is used ("default config file" in the `pmsencoder --version` output)
* mencoder is looked for in the mencoder_path specified in the config file, the path specified in the MENCODER_PATH environment variable, the current directory, or the PATH environment variable
* The config file is in [YAML](http://en.wikipedia.org/wiki/YAML) format. It can have a .conf, .yml or .yaml extension
* Don't modify the default config file. Copy (don't link) it to the directory listed as "user config dir" in the output of `pmsencoder --version` and modify the copy. Delete the copy if you wish to revert to the default configuration.

# Troubleshooting
* Check the PMS debug.log
* Check the pmsencoder.log logfile (see the output of `pmsencoder --version` for the path)
* Run `pmsencoder --test` to test pmsencoder from the command line

# Support

* For more details, discussion and troubleshooting tips, see [this thread](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002) ([start here](http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=5002#p22479)).

# Version

0.60

# License

Copyright 2009-2010 [chocolateboy](mailto:chocolate@cpan.org).

PMSEncoder is free software; you can redistribute it and/or modify it under the terms of the [Artistic License 2.0](http://www.opensource.org/licenses/artistic-license-2.0.php).
