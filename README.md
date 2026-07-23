# Remakepire - A Custom Modification of the Vampires SMP Plugin
###### Credit to POW Creations for the original plugin "VampireSMP".

_Remakepire_ is an expanded and modified copy created by IntricEight, allowing fans of POW Creation's Vampires SMP series
to run their own Vampires games.
_Remakepire_ keeps the same core mechanics of the VampireSMP plugin, whilst opening up a multitude of configurable options
to change key parts of the game at the game runners' behest. The plugin has received extensive bug-fixing support, and
will help each of you make your vampire games the ideal experience.


## Instructions on using Remakepire

### Loading the Plugin
Important note: You cannot run both the _VampireSMP_ and _Remakepire_ plugins at the same time. If _VampireSMP_ is loaded
onto your server, make a local copy of it to save for later use, then delete the folder and _VampireSMP.jar_ file from
your server before running _Remakepire_.

If you are adding _Remakepire_ to an ongoing game, read through all of this document before starting
to follow the steps in order to avoid losing important game data.

The process of getting the plugin running on your server is identical to running the _VampireSMP_ plugin.
This process may vary platform by platform, so check out a platform-specific tutorial if you are getting
confuse. Here is what works at the most basic level:
- After dragging _Remakepire-1.1.0.jar_ into your plugins folder, run the server and it will create a folder
  containing core files like the sire map, config, and beacons.
- Once the server has booted up and its folder has been created, turn off the server.
- Open the _config.yml_ file and begin changing the plugin values. This is the time you would bring in any existing files
  you want to continue using (Check __Using with Existing Games__ for more details)

The plugin should now be ready to run your Vampires game.


### Using with Existing Games
If you would like to replace the _VampireSMP_ plugin with _Remakepire_ during an ongoing series, some files should be
set aside to replace _Remakepire_'s generated files. These are:
- bat_armor_storage.json
- bat_transformations.txt
- beacons.json
- permadeath_modes.json
- placed_logs.json
- sire_mappings.json
- config.yml*

*_Remakepire_'s config.yml has too many new features to replace the file itself, so you'll need to open both
the old and new config files and manually replace values for settings shared by both.

Other files, such as _beacons.json_, can be copied or dragged into the _Remakepire_ folder to replace the generated files.


### Using the custom ControllableDays
Because of the way POW's _ControllableDays_ is programmed, it will not work well with _Remakepire_.
To keep time frozen outside active sessions, you will need to download my custom version of _ControllableDays_.
This plugin is the same as POW's released version, aside from a single new option in the _config.yml_ file
that allows this plugin to work with projects beyond _VampireSMP_.

By default, this custom plugin's config is set
to work with _Remakepire_, but you can also modify and use it with any custom vampires-based plugin.
If you don't use the custom _ControllableDays_, your game will still run with the slower daytime cycle. However, time
won't freeze when the session does, which may lead to inconsistencies in session times around breaks.


### Using Custom Text through text-config.yml
One of the files created by _Remakepire_ is a rather overwhelming heap of text titled _text-config.yml_.
This file gives game runners almost complete control over the contents of major text instances within the Vampires game.
At present, this includes the contents and interaction messages of cure books, the global cure and death annoucements,
and the text that appears when interacting with the game boundaries.

This file is entirely optional to interact with, so take it at your own pace.
Don't forget to enable custom content within _text-config.yml_ using the true/false toggle so it appears in-game.


### Adding your own Modifications
_Remakepire_ is designed to support all extensions made for POW's _VampireSMP_ plugin.

More notably, though, its source code is also thoroughly documented and freely available. This project was originally designed
to turn POW's plugin into an open-source project for the community to mod. Although the scope of this plugin has grown since
its original release. it remains designed around assisting game runners and other players in customizing the plugin.

There are several modifications to Remakepire within this repository, accessible through different branches. Please peruse
and combine them at your leisure if they contain features you would like to use, or use them as a starting point instead
of the main branch as you desire.
