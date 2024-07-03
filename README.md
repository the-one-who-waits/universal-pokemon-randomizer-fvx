# Universal Pokemon Randomizer FVX

The **Universal Pokemon Randomizer FVX** (**F**ox + **V**oliol + z**X**) is a continuation of the [Universal Pokemon Randomizer](https://github.com/Dabomstew/universal-pokemon-randomizer) by Dabomstew. It was born of a merge of branches by [foxoftheasterisk](https://github.com/foxoftheasterisk) and [voliol](https://github.com/voliol/universal-pokemon-randomizer), both based on Ajarmar's [UPR ZX](https://github.com/Ajarmar/universal-pokemon-randomizer-zx). 

Compared to ZX, FVX adds a number of features; from upgrades to Trainer and wild Pokémon randomization, to Pokémon Palette randomization and Custom Player Graphics. 
For a full list of new features, see [the wiki](https://github.com/upr-fvx/universal-pokemon-randomizer-fvx/wiki).

True to its ancestry in ZX, it supports all vanilla core series Pokémon games from Generation 1-7 except Let's Go, Pikachu!/Eevee!; in other words, it supports all core series games for the GameBoy, GameBoy Color, GameBoy Advance, Nintendo DS, and Nintendo 3DS.

For developers, FVX also has a considerable amount of refactoring and new features, including separate Randomizer classes for each category of randomization, a PokemonSet class with many helper functions, and automated tests for most features.

# Feature requests

We gladly take feature requests to know what the user-base wants, but be aware that we are just two people working on this at our own discretion and pace, and will implement them (or not) according to that. 
If you want to guarantee your feature makes it in, the only way is to pick up Java and code it yourself. It is fun :)

# Contributing

If you want to contribute something to the codebase, we recommended to create an issue for it first (using the`Contribution Idea` template). This way, we can discuss how to accomplish this, and possibly whether it is a good fit for the randomizer. 

[This page on ZX's wiki](https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/Building-Universal-Pokemon-Randomizer-ZX) explains how to set up to build/test locally.

If you are adding a new setting, make sure you follow the new setting checklist in the root folder of the repository.

### What is a good fit for the randomizer?

In general, the UPR should have settings as universal as possible. This means that an idea preferably should work in as many games as possible, and also that it is something that many people will find useful/fun. If the setting is very niche, it will mostly just bloat the GUI. FVX is more laissez-faire than other forks, but still follows this general design guideline.

If your idea is a change to an existing setting rather than a new setting, it needs to be well motivated.

# Bug reports

If you encounter something that seems to be a bug, submit an issue using the `Bug Report` issue template.

# Other problems

If you have problems using the randomizer, it could be because of some problem with Java or your operating system. **If you have problems with starting the randomizer specifically, [read this page first before creating an issue.](https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/About-Java)** If that page does not solve your problem, submit an issue using the `Need Help` issue template.
