package com.dabomstew.pkrandom.pokemon;

public enum EncounterType {
    WALKING, //e.g. grass, cave, seaweed(unfortunately), Horde encounters, shaking grass
    SURFING, FISHING, //obvious
    INTERACT, //e.g. headbutt trees, Rock Smash
    AMBUSH, //e.g. flying pokemon, shaking trees
    SPECIAL, //e.g. Poke Radar, DexNav Foreign encounter, Hoenn/Sinnoh Sound
    UNUSED //obvious
} //SOS encounters are included in the same area as their non-SOS origin, so aren't included as a type