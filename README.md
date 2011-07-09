
DM3 Contacs Importer
=======================

A (yet) one-time importer for semi-structured contact information realized as a deepamehta3-plugin for v0.5-SNAPSHOT. Currently a TAB-separated .CSV-File can be partially mapped onto the dm3.contacts.*-module after being located at   ''/src/main/resources/community-tab.csv'' and build with the plugin from source.

Tested with a Mozilla Thunderbird 3.x TAB-separated .csv-file.

DeepaMehta 3 is a platform for collaboration and knowledge management.  
<http://github.com/jri/deepamehta3>


Installing
----------

1. checkout [DeepaMehta 3](http://github.com/jri/deepamehta3)'s ''no-properties''-branch. After that enter your deepamehta3 directory and run a ''mvn clean install''.

2. checkout this plugin into the same directory where your ''deepamehta3'' directory is located. Place your .csv-file into the plugins ''src/main/resources/''-Folder. Now also run a ''mvn clean install''.


Starting
--------

After having the setup as described above, navigate into the ''deepamehta3-delivery'' module and run "mvn pax:run -P development". All entries (\n) in your .csv-file will be created as ''Person''-Topics with a mailbox, a first- and a last-name plus your notes.


Version History
---------------

**v0.1** -- Jul 09, 2011

* Basic functionality:
    * creates ''Person''-Topics with a first name, last name, mailbox and notes from a TAB-separated .csv-file.
* Compatible with DeepaMehta 3 v0.5-SNAPSHOT


------------
Malte Reißig
Jul 09, 2011
