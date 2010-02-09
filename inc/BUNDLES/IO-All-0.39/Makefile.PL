use inc::Module::Install;

name        'IO-All';
all_from    'lib/IO/All.pm';

requires    'IO::String' => '0';

clean_files 't/output/';

WriteAll;
