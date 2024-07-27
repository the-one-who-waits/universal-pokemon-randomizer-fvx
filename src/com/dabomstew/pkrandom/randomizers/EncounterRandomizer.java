package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.exceptions.RandomizationException;
import com.dabomstew.pkrandom.game_data.*;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.*;

public class EncounterRandomizer extends Randomizer {

    public EncounterRandomizer(RomHandler romHandler, Settings settings, Random random) {
        super(romHandler, settings, random);
    }

    public void randomizeEncounters() {
        boolean useTimeOfDay = settings.isUseTimeBasedEncounters();
        int levelModifier = settings.isWildLevelsModified() ? settings.getWildLevelModifier() : 0;

        if(!settings.isRandomizeWildPokemon()) {
            modifyLevelsOnly(useTimeOfDay, levelModifier);
            return;
        }

        Settings.WildPokemonRegionMod mode = settings.getWildPokemonRegionMod();
        boolean splitByEncounterType = settings.isSplitWildRegionByEncounterTypes();
        boolean randomTypeThemes = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.RANDOM_THEMES;
        boolean keepTypeThemes = settings.isKeepWildTypeThemes();
        boolean keepPrimaryType = settings.getWildPokemonTypeMod() == Settings.WildPokemonTypeMod.KEEP_PRIMARY;
        boolean keepEvolutions = settings.isKeepWildEvolutionFamilies();
        boolean catchEmAll = settings.isCatchEmAllEncounters();
        boolean similarStrength = settings.isSimilarStrengthEncounters();
        boolean noLegendaries = settings.isBlockWildLegendaries();
        boolean balanceShakingGrass = settings.isBalanceShakingGrass();
        boolean allowAltFormes = settings.isAllowWildAltFormes();
        boolean banIrregularAltFormes = settings.isBanIrregularAltFormes();
        boolean abilitiesAreRandomized = settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE;

        randomizeEncounters(mode, splitByEncounterType, useTimeOfDay,
                randomTypeThemes, keepTypeThemes, keepPrimaryType, keepEvolutions, catchEmAll, similarStrength, noLegendaries,
                balanceShakingGrass, levelModifier, allowAltFormes, banIrregularAltFormes, abilitiesAreRandomized);
        changesMade = true;
    }

    // only exists for some old test cases, please don't use
    public void randomizeEncounters(Settings.WildPokemonRegionMod mode, Settings.WildPokemonTypeMod typeMode,
                                    boolean useTimeOfDay,
                                    boolean catchEmAll, boolean similarStrength,
                                    boolean noLegendaries, boolean balanceShakingGrass, int levelModifier,
                                    boolean allowAltFormes, boolean banIrregularAltFormes,
                                    boolean abilitiesAreRandomized) {
        randomizeEncounters(mode,
                false,
                useTimeOfDay,
                typeMode == Settings.WildPokemonTypeMod.RANDOM_THEMES,
                false,
                typeMode == Settings.WildPokemonTypeMod.KEEP_PRIMARY,
                false,
                catchEmAll, similarStrength,
                noLegendaries, balanceShakingGrass, levelModifier,
                allowAltFormes, banIrregularAltFormes,
                abilitiesAreRandomized);
    }

    // only public for some old test cases, please don't use
    public void randomizeEncounters(Settings.WildPokemonRegionMod mode, boolean splitByEncounterType,
                                    boolean useTimeOfDay,
                                    boolean randomTypeThemes, boolean keepTypeThemes, boolean keepPrimaryType,
                                    boolean keepEvolutions, boolean catchEmAll, boolean similarStrength,
                                    boolean noLegendaries, boolean balanceShakingGrass, int levelModifier,
                                    boolean allowAltFormes, boolean banIrregularAltFormes,
                                    boolean abilitiesAreRandomized) {
        // - prep settings
        // - get encounters
        // - setup banned + allowed
        // - randomize inner
        // - apply level modifier
        // - set encounters

        rSpecService.setRestrictions(settings);

        List<EncounterArea> encounterAreas = romHandler.getEncounters(useTimeOfDay);
        List<EncounterArea> preppedAreas = prepEncounterAreas(encounterAreas);

        SpeciesSet banned = getBannedForWildEncounters(banIrregularAltFormes, abilitiesAreRandomized);
        SpeciesSet allowed = new SpeciesSet(rSpecService.getSpecies(noLegendaries, allowAltFormes, false));
        allowed.removeAll(banned);

        InnerRandomizer ir = new InnerRandomizer(allowed, banned,
                randomTypeThemes, keepTypeThemes, keepPrimaryType, catchEmAll, similarStrength, balanceShakingGrass,
                keepEvolutions);
        switch (mode) {
            case NONE:
                if(romHandler.isORAS()) {
                    //this mode crashes ORAS and needs special handling to approximate
                    ir.randomEncountersORAS(preppedAreas);
                } else {
                    ir.randomEncounters(preppedAreas);
                }
                break;
            case ENCOUNTER_SET:
                ir.area1to1Encounters(preppedAreas);
                break;
            case MAP:
                ir.map1to1Encounters(preppedAreas, splitByEncounterType);
                break;
            case NAMED_LOCATION:
                ir.location1to1Encounters(preppedAreas, splitByEncounterType);
                break;
            case GAME:
                ir.game1to1Encounters(preppedAreas, splitByEncounterType);
                break;
        }

        applyLevelModifier(levelModifier, encounterAreas);
        romHandler.setEncounters(useTimeOfDay, encounterAreas);
    }

    /**
     * Changes the levels of wild Pokemon encounters without randomizing them.
     */
    private void modifyLevelsOnly(boolean useTimeOfDay, int levelModifier) {
        List<EncounterArea> encounterAreas = romHandler.getEncounters(useTimeOfDay);
        applyLevelModifier(levelModifier, encounterAreas);
        romHandler.setEncounters(useTimeOfDay, encounterAreas);
    }

    private SpeciesSet getBannedForWildEncounters(boolean banIrregularAltFormes,
                                                           boolean abilitiesAreRandomized) {
        SpeciesSet banned = new SpeciesSet();
        banned.addAll(romHandler.getBannedForWildEncounters());
        banned.addAll(rSpecService.getBannedFormesForPlayerPokemon());
        if (!abilitiesAreRandomized) {
            SpeciesSet abilityDependentFormes = rSpecService.getAbilityDependentFormes();
            banned.addAll(abilityDependentFormes);
        }
        if (banIrregularAltFormes) {
            banned.addAll(romHandler.getIrregularFormes());
        }
        return banned;
    }

    protected void applyLevelModifier(int levelModifier, List<EncounterArea> currentEncounterAreas) {
        if (levelModifier != 0) {
            for (EncounterArea area : currentEncounterAreas) {
                for (Encounter enc : area) {
                    enc.setLevel(Math.min(100, (int) Math.round(enc.getLevel() * (1 + levelModifier / 100.0))));
                    enc.setMaxLevel(Math.min(100, (int) Math.round(enc.getMaxLevel() * (1 + levelModifier / 100.0))));
                }
            }
        }
    }

    private class InnerRandomizer {
        private final boolean randomTypeThemes;
        private final boolean keepTypeThemes;
        private final boolean keepPrimaryType;
        private final boolean needsTypes;
        private final boolean catchEmAll;
        private final boolean similarStrength;
        private final boolean balanceShakingGrass;
        private final boolean keepEvolutions;

        private boolean useMapping;

        private Map<Type, SpeciesSet> allowedByType;
        private final SpeciesSet allowed;
        private final SpeciesSet banned;

        private Map<Type, SpeciesSet> remainingByType;
        private SpeciesSet remaining;

        private Type regionType;
        //private SpeciesSet allowedForArea;
        private Map<Species, Species> regionMap;
        //private SpeciesSet allowedForReplacement;

        private Map<Species, SpeciesAreaInformation> areaInformationMap = null;

        //ORAS's DexNav will crash if the load is higher than this value.
        final int ORAS_CRASH_THRESHOLD = 18;

        public InnerRandomizer(SpeciesSet allowed, SpeciesSet banned,
                               boolean randomTypeThemes, boolean keepTypeThemes, boolean keepPrimaryType,
                               boolean catchEmAll, boolean similarStrength, boolean balanceShakingGrass,
                               boolean keepEvolutions) {
            if (randomTypeThemes && keepPrimaryType) {
                throw new IllegalArgumentException("Can't use keepPrimaryType with randomTypeThemes.");
            }
            this.randomTypeThemes = randomTypeThemes;
            this.keepTypeThemes = keepTypeThemes;
            this.keepPrimaryType = keepPrimaryType;
            this.keepEvolutions = keepEvolutions;
            this.needsTypes = keepPrimaryType || keepTypeThemes || randomTypeThemes;
            this.catchEmAll = catchEmAll;
            this.similarStrength = similarStrength;
            this.balanceShakingGrass = balanceShakingGrass;
            this.allowed = allowed;
            this.banned = banned;
            if (needsTypes) {
                this.allowedByType = allowed.sortByType(false, typeService.getTypes());
            }
            //any algorithm that uses mapping should use remaining, not just catch-em-all
            //easiest to just always use it
            refillRemainingSpecies();
        }

        private void refillRemainingSpecies() {
            remaining = new SpeciesSet(allowed);
            if (needsTypes) {
                remainingByType = new EnumMap<>(Type.class);
                for (Type t : typeService.getTypes()) {
                    remainingByType.put(t, new SpeciesSet(allowedByType.get(t)));
                }
            }
        }

        //This is now the one most different, algorithm-wise
        //but it has enough overlap to make sense here, anyway.
        public void randomEncounters(List<EncounterArea> encounterAreas) {
            useMapping = false;

            //ok. this is dumb, but it makes it integrate well.
            List<List<EncounterArea>> regions = new ArrayList<>();
            for(EncounterArea area : encounterAreas) {
                List<EncounterArea> region = new ArrayList<>();
                region.add(area);
                regions.add(region);
            }

            randomizeRegions(regions);
        }

        /**
         * Special case to approximate random encounters in ORAS, since they crash if the
         * normal algorithm is used.
         * @param encounterAreas The list of EncounterAreas to randomize.
         */
        private void randomEncountersORAS(List<EncounterArea> encounterAreas) {

            List<EncounterArea> collapsedEncounters = EncounterArea.flattenEncounterTypesInMaps(encounterAreas);
            List<List<EncounterArea>> maps = new ArrayList<>(
                    EncounterArea.groupAreasByMapIndex(collapsedEncounters).values());
            Collections.shuffle(maps, random);
            //Awkwardly, the grouping is run twice...

            //sort out Rock Smash areas
            List<EncounterArea> rockSmashAreas = new ArrayList<>();
            for(List<EncounterArea> map : maps) {
                Iterator<EncounterArea> mapIterator = map.iterator();
                while(mapIterator.hasNext()) {
                    EncounterArea area = mapIterator.next();
                    if(area.getEncounterType() == EncounterType.INTERACT) {
                        //rock smash is the only INTERACT type in ORAS
                        rockSmashAreas.add(area);
                        mapIterator.remove();
                    }
                }
            }

            //Rock smash is not affected by the crashing, so we can run the standard RandomEncounters on it.
            this.randomEncounters(rockSmashAreas);

            randomizeRegionsORAS(maps);
        }

        public void area1to1Encounters(List<EncounterArea> encounterAreas) {
            useMapping = true;

            //ok. this is dumb, but it makes it integrate well.
            List<List<EncounterArea>> regions = new ArrayList<>();
            for(EncounterArea area : encounterAreas) {
                List<EncounterArea> region = new ArrayList<>();
                region.add(area);
                regions.add(region);
            }

            randomizeRegions(regions);
        }

        public void map1to1Encounters(List<EncounterArea> encounterAreas, boolean splitByEncounterType) {
            useMapping = true;
            Collection<List<EncounterArea>> regions = EncounterArea.groupAreasByMapIndex(encounterAreas).values();

            if(splitByEncounterType) {
                Collection<List<EncounterArea>> maps = regions;
                regions = new ArrayList<>();
                for(List<EncounterArea> map : maps) {
                    regions.addAll(EncounterArea.groupAreasByEncounterType(map).values());
                }
            }

            randomizeRegions(regions);
        }

        public void location1to1Encounters(List<EncounterArea> encounterAreas, boolean splitByEncounterType) {
            useMapping = true;
            Collection<List<EncounterArea>> regions = EncounterArea.groupAreasByLocation(encounterAreas).values();

            if(splitByEncounterType) {
                Collection<List<EncounterArea>> maps = regions;
                regions = new ArrayList<>();
                for(List<EncounterArea> map : maps) {
                    regions.addAll(EncounterArea.groupAreasByEncounterType(map).values());
                }
            }

            randomizeRegions(regions);
        }

        public void game1to1Encounters(List<EncounterArea> encounterAreas, boolean splitByEncounterType) {
            useMapping = true;

            Collection<List<EncounterArea>> regions;
            if (splitByEncounterType) {
                regions = EncounterArea.groupAreasByEncounterType(encounterAreas).values();
            } else {
                regions = new ArrayList<>();
                regions.add(encounterAreas);
            }

            randomizeRegions(regions);
        }

        /**
         * Given a Collection of regions (represented by a List of EncounterAreas),
         * randomizes each region such that type theming, 1-to-1 map, etc., are carried
         * throughout the region.
         * @param regions The regions to randomize.
         */
        private void randomizeRegions(Collection<List<EncounterArea>> regions) {

            //Shuffle regions; otherwise, large regions would tend to be randomized first.
            List<List<EncounterArea>> shuffledRegions = new ArrayList<>(regions);
            Collections.shuffle(shuffledRegions, random);

            for(List<EncounterArea> region : shuffledRegions) {
                regionType = pickRegionType(region);

                if(useMapping) {
                    regionMap = new HashMap<>();
                    setupAreaInfoMap(region);

                    if(keepEvolutions) {
                        spreadThemesThroughFamilies();
                    }
                }

                for(EncounterArea area : region) {
                    randomizeArea(area);
                }

                if(useMapping && !catchEmAll) {
                    //if not using mapping or catch em all, remaining will not empty in the first place.
                    refillRemainingSpecies();
                }
            }
        }

        private void randomizeArea(EncounterArea area) {
            //no area-level type theme, because that could foul up other type restrictions.

            boolean needsIndividualTypeRestrictions = (needsTypes && regionType == null);
            //efficiency-related: if each Species has its own type restrictions, faster to
            //run area filters on remainingByType than to run type filters on AllowedForArea

            SpeciesSet allowedForArea = null;
            if(!needsIndividualTypeRestrictions) {
                allowedForArea = setupAllowedForArea(regionType, area);
                if (useMapping || catchEmAll) {
                    allowedForArea = new SpeciesSet(allowedForArea);
                }
            }

            for (Encounter enc : area) {
                Species current = enc.getSpecies();

                Species replacement;
                if(useMapping && regionMap.containsKey(current)) {
                    //checking the map first lets us avoid creating a pointless allowedForReplacement set
                    replacement = regionMap.get(current);

                } else {
                    if(keepEvolutions && mapHasFamilyMember(current)) {
                        replacement = pickFamilyMemberReplacement(current);
                    } else {

                        //we actually need to pick a new one
                        SpeciesSet allowedForReplacement;
                        if (needsIndividualTypeRestrictions) {
                            allowedForReplacement = setupAllowedForReplacement(enc, area);
                        } else {
                            allowedForReplacement = setupAllowedForReplacement(enc, allowedForArea);
                            if (allowedForReplacement.isEmpty()) {
                                allowedForReplacement = retrySetupAllowedForAreaAndReplacement(enc, area, regionType);
                            }
                        }
                        if (allowedForReplacement.isEmpty()) {
                            throw new RandomizationException("Could not find a wild Species replacement for " + enc);
                        }


                        //ok, we have a valid set. Time to actually choose a Species!
                        replacement = pickReplacement(current, allowedForReplacement);
                    }

                    //add to map if applicable
                    if (useMapping) {
                        regionMap.put(current, replacement);
                    }

                    //remove from possible picks if applicable
                    if (useMapping || catchEmAll) {
                        removeFromRemaining(replacement);
                        if(allowedForArea != null) {
                            allowedForArea.remove(replacement);

                            if (allowedForArea.isEmpty()) {
                                allowedForArea = new SpeciesSet(setupAllowedForArea(regionType, area));
                            }
                        }
                        //removeFromRemaining() already checks if remaining is empty, so we don't need to do that here.
                    }
                }

                enc.setSpecies(replacement);
                setFormeForEncounter(enc, replacement);
            }

            if (area.isForceMultipleSpecies()) {
                enforceMultipleSpecies(area);
            }
        }

        /**
         * Given a {@link Species} which has at least one evolutionary relative contained within the regionMap,
         * chooses a replacement for it that is a corresponding relative of its relative's replacement.
         * @param toReplace The {@link Species} to find a replacement for.
         * @return An appropriate replacement {@link Species}.
         */
        private Species pickFamilyMemberReplacement(Species toReplace) {
            SpeciesAreaInformation info = areaInformationMap.get(toReplace);
            SpeciesSet family = info.getFamily();
            for(Species relative : family) {
                if(regionMap.containsKey(relative)) {
                    return pickFamilyMemberReplacementInner(toReplace, relative);
                }
            }

            throw new IllegalArgumentException("Tried to pick family member replacement for non-mapped Species!");
        }

        /**
         * Given a {@link Species} and a relative of that {@link Species} which is contained in the regionMap,
         * chooses a replacement for it that is a corresponding relative of its relative's replacement.
         * @param toReplace The {@link Species} to replace.
         * @param relative A relative of that {@link Species}, which is contained as a key in the regionMap.
         * @return An appropriate replacement {@link Species}.
         */
        private Species pickFamilyMemberReplacementInner(Species toReplace, Species relative) {
            SpeciesAreaInformation info = areaInformationMap.get(toReplace);
            int relation = relative.getRelation(info.getSpecies(), true);
            Species relativeReplacement = regionMap.get(relative);
            if(relativeReplacement == null) {
                throw new IllegalArgumentException("Relative had a null replacement!");
            }

            SpeciesSet possibleReplacements = relativeReplacement.getRelativesAtPosition(relation, false);
            possibleReplacements.retainAll(remaining);
            possibleReplacements.removeAll(info.getBannedForReplacement());
            if(!possibleReplacements.isEmpty()) {
                pickReplacement(toReplace, possibleReplacements);
            }
            //else - remaining didn't have any valid, but allowed should.
            possibleReplacements = relativeReplacement.getRelativesAtPosition(relation, false);
            possibleReplacements.retainAll(allowed);
            possibleReplacements.removeAll(info.getBannedForReplacement());
            if(!possibleReplacements.isEmpty()) {
                pickReplacement(toReplace, possibleReplacements);
            }
            //else - we messed up earlier, this Species has no replacement
            throw new IllegalStateException("Chose a family that is invalid!");
        }

        /**
         * Checks if any family member (as listed in areaInformationMap) of the given {@link Species}
         * is contained in the regionMap.
         * @param current The {@link Species} to check.
         * @return True if any family member is present, false otherwise.
         */
        private boolean mapHasFamilyMember(Species current) {
           SpeciesAreaInformation info = areaInformationMap.get(current);
           SpeciesSet family = info.getFamily();
           for(Species relative : family) {
               if(regionMap.containsKey(relative)) {
                   return true;
               }
           }

           return false;
        }

        /**
         * Given an encounter and area, and optionally a type, runs (the equivalent of) setupAllowedForArea and
         * setupAllowedForReplacement on allowed, rather than remaining.
         * @param enc The encounter to choose replacements for.
         * @param areaType The type theme for the area, or null if none.
         * @return A {@link SpeciesSet} containing the allowed replacements for this encounter and area.
         */
        private SpeciesSet retrySetupAllowedForAreaAndReplacement(Encounter enc, EncounterArea area, Type areaType) {
            SpeciesSet allowedForArea = removeBannedFromArea(
                    (areaType == null) ? allowed : allowedByType.get(areaType),
                    area);

            return setupAllowedForReplacement(enc, allowedForArea);
        }

        /**
         * Given a {@link List} of maps (each represented by a {@link List} of {@link EncounterArea}s) randomizes them
         * with as many distinct Pokemon as possible without crashing ORAS's DexNav.
         * @param maps The list of maps to randomize.
         */
        private void randomizeRegionsORAS(List<List<EncounterArea>> maps) {
            //Shuffle maps; otherwise, large maps would tend to be randomized first.
            List<List<EncounterArea>> shuffledMaps = new ArrayList<>(maps);
            Collections.shuffle(shuffledMaps, random);

            for(List<EncounterArea> map : shuffledMaps) {
                randomizeMapORAS(map);

                if(!catchEmAll) {
                    refillRemainingSpecies();
                }
            }
        }

        /**
         * Given a list of EncounterAreas, all on the same map, randomizes them with as many
         * different {@link Species} as it can without crashing.
         * @param map The map to randomize.
         */
        private void randomizeMapORAS(List<EncounterArea> map) {
            //a messy method, but less so than the previous versions

            class AreaWithData {
                EncounterArea area;
                Type areaType;
                Map<Species, Species> areaMap;
                SpeciesSet allowedForArea = null;
            }

            Map<Encounter, AreaWithData> encountersToAreas = new IdentityHashMap<>();
            //IdentityHashMap makes each key distinct if it has a different reference to the same value
            //This means that identical Encounters will still map to the correct areas

            for(EncounterArea area : map) {
                AreaWithData awd = new AreaWithData();
                awd.area = area;

                List<EncounterArea> dummyRegion = new ArrayList<>();
                dummyRegion.add(area);
                awd.areaType = pickRegionType(dummyRegion);

                awd.areaMap = new HashMap<>();

                if(!keepPrimaryType) {
                    //the only individual type restrictions should be keepPrimary,
                    //since the area and region are the same
                    awd.allowedForArea = new SpeciesSet(setupAllowedForArea(awd.areaType, area));
                }

                for(Encounter enc : area) {
                    encountersToAreas.put(enc, awd);
                }
            }

            List<Encounter> shuffledEncounters = new ArrayList<>(encountersToAreas.keySet());
            Collections.shuffle(shuffledEncounters, random);

            int dexNavLoad = getORASDexNavLoad(map);

            SpeciesSet usedSpecies = new SpeciesSet();

            //now we're prepared to start actually randomizing
            for(Encounter enc : shuffledEncounters) {
                AreaWithData awd = encountersToAreas.get(enc);


                Species replacement;

                if(!awd.areaMap.containsKey(enc.getSpecies()) || dexNavLoad < ORAS_CRASH_THRESHOLD) {
                    //get new species
                    SpeciesSet allowedForReplacement;
                    if(awd.allowedForArea != null) {
                        awd.allowedForArea.removeAll(usedSpecies);
                        if(awd.allowedForArea.isEmpty()){
                            awd.allowedForArea = new SpeciesSet(setupAllowedForArea(awd.areaType, awd.area));
                        }

                        allowedForReplacement = setupAllowedForReplacement(enc, awd.area);
                        if(allowedForReplacement.isEmpty()) {
                            retrySetupAllowedForAreaAndReplacement(enc, awd.area, awd.areaType);
                        }

                    } else {
                        allowedForReplacement = setupAllowedForReplacement(enc, awd.area);
                    }

                    replacement = pickReplacement(enc.getSpecies(), allowedForReplacement);
                    removeFromRemaining(replacement);
                    usedSpecies.add(replacement);

                    //either put it in the map, or increase DexNav load
                    if(!awd.areaMap.containsKey(enc.getSpecies())) {
                        awd.areaMap.put(enc.getSpecies(), replacement);
                    } else {
                        dexNavLoad++;
                    }
                } else {
                    replacement = awd.areaMap.get(enc.getSpecies());
                }

                enc.setSpecies(replacement);
                setFormeForEncounter(enc, replacement);
            }

        }

        /**
         * Chooses an appropriate type theme for the given region based on the current settings:
         * If keepTypeThemes is true, chooses an existing theme if there is one.
         * If no theme was chosen, and randomTypeThemes is true, chooses a theme at random.
         * (Exception: If using catch-em-all and a banned {@link Species} was present in the regions, the
         * chosen "random" type will be one of the banned {@link Species}'s types.)
         * @param region A List of EncounterAreas representing an appropriately-sized region for randomization.
         * @return A Type chosen by one of the above-listed methods, or null if none was chosen.
         */
        private Type pickRegionType(List<EncounterArea> region) {
            Type picked = null;
            if(keepTypeThemes) {
                //see if any types are shared among all areas in the region
                Set<Type> possibleThemes = EnumSet.allOf(Type.class);
                for(EncounterArea area : region) {
                    possibleThemes.retainAll(area.getSpeciesInArea().getSharedTypes(true));
                    if(possibleThemes.isEmpty()) {
                        break;
                    }
                }

                //if so, pick one
                if(!possibleThemes.isEmpty()) {
                    Iterator<Type> itor = possibleThemes.iterator();
                    picked = itor.next();
                    if(itor.hasNext()) {
                        if(picked == Type.NORMAL) {
                            //prefer not normal
                            picked = itor.next();
                        } else {
                            //prefer primary of first species
                            Type preferredTheme = region.get(0).get(0).getSpecies().getPrimaryType(true);
                            if(picked != preferredTheme) {
                                picked = itor.next();
                            }
                        }
                        //both assume maximum two themes, which should be a safe assumption
                    }
                }
            }

            if(picked == null && randomTypeThemes) {
                picked = pickRandomTypeWithSpeciesRemaining();

                // Unown clause - since Unown (and other banned Species) aren't randomized with catchEmAll active,
                // the "random" type theme must be one of the banned Species's types.
                // The implementation below supports multiple banned Species of the same type in the same area,
                // because why not?
                if (catchEmAll) {
                    SpeciesSet bannedInArea = new SpeciesSet(banned);
                    SpeciesSet speciesInRegion = new SpeciesSet();
                    region.forEach(area -> speciesInRegion.addAll(area.getSpeciesInArea()));
                    bannedInArea.retainAll(speciesInRegion);

                    Type themeOfBanned = bannedInArea.getSharedType(false);
                    if (themeOfBanned != null) {
                        picked = themeOfBanned;
                    }
                }
            }

            return picked;
        }

        /**
         * Given an {@link EncounterArea}, returns a shared type of that area (before randomization) iff keepTypeThemes
         * is true.
         * @param area The area to examine.
         * @return A shared type if keepTypeThemes is true and such a type exists, null otherwise.
         */
        private Type findAreaType(EncounterArea area) {
            Type picked = null;
            if (keepTypeThemes) {
                picked = area.getSpeciesInArea().getSharedType(true);
            }
            return picked;
        }

        private Type pickRandomTypeWithSpeciesRemaining() {
            List<Type> types = new ArrayList<>(typeService.getTypes());
            Collections.shuffle(types, random);
            Type areaType;
            do {
                areaType = types.remove(0);
            } while (remainingByType.get(areaType).isEmpty() && !types.isEmpty());
            if(types.isEmpty()) {
                throw new IllegalStateException("RemainingByType contained no Species of any valid type!");
            }
            return areaType;
        }

        /**
         * Given an area and (optionally) a Type, returns a set of {@link Species} valid for placement in that area,
         * of the given type if there was one.
         * @param areaType The Type which all {@link Species} returned should have, or null.
         * @param area The area to find allowed {@link Species} for.
         * @return A {@link SpeciesSet} (which may be a reference to an existing set) which contains all
         * {@link Species} allowed for the area.
         */
        private SpeciesSet setupAllowedForArea(Type areaType, EncounterArea area) {

            SpeciesSet allowedForArea;
            if (areaType != null) {
                allowedForArea = removeBannedFromArea(remainingByType.get(areaType), area);
                if(allowedForArea.isEmpty()) {
                    allowedForArea = removeBannedFromArea(allowedByType.get(areaType), area);
                }
            } else {
                allowedForArea = removeBannedFromArea(remaining, area);
                if(allowedForArea.isEmpty()) {
                    allowedForArea = removeBannedFromArea(allowed, area);
                }
            }

            return allowedForArea;
        }

        /**
         * Removes all {@link Species} banned from the given area from the given pool.
         * Safe to pass referenced {@link SpeciesSet}s to.
         * @param startingPool The pool of {@link Species} to start from.
         * @param area The area to check for banned {@link Species}.
         * @return startingPool if the area had no banned {@link Species}; a new {@link SpeciesSet} with the banned
         * {@link Species} removed otherwise.
         */
        private SpeciesSet removeBannedFromArea(SpeciesSet startingPool, EncounterArea area) {
            SpeciesSet banned = area.getBannedSpecies();
            if(!banned.isEmpty()) {
                startingPool = new SpeciesSet(startingPool); //don't want to remove from the original!
                startingPool.removeAll(banned);
            }

            return startingPool;
        }

        /**
         * Given an encounter, chooses a set of potential replacements for that encounter.
         * @param enc The encounter to replace.
         * @param area The area the encounter is in. Used to determine banned {@link Species} if areaInformationMap is not
         *             populated.
         * @return A {@link SpeciesSet} containing all valid replacements for the encounter. This may be a
         * reference to another set; do not modify!
         */
        private SpeciesSet setupAllowedForReplacement(Encounter enc, EncounterArea area) {
            if(areaInformationMap == null) {
                //Since this method is only called when the Species need individual type restrictions,
                //this should only happen if we're using keepPrimary.
                //However, I'm not going to make that an assumed condition.

                if(keepPrimaryType) {
                    Type primary = enc.getSpecies().getPrimaryType(true);
                    return setupAllowedForArea(primary, area);
                } else {
                    //this shouldn't be reached, but maybe that will change in future
                    return setupAllowedForArea(null, area);
                }
            }
            //else
            SpeciesAreaInformation info = areaInformationMap.get(enc.getSpecies());
            if(info == null) {
                //technically, this is the same situation as the above. However, this should not happen with the current
                //flow, so we throw an exception.
                throw new IllegalStateException("Info was null for encounter's species!");
            }

            Type type = info.getTheme(keepPrimaryType);
            boolean needsInner = !info.bannedForReplacement.isEmpty() || keepEvolutions;
            //if neither of these is true, the only restriction is the type

            SpeciesSet possiblyAllowed = (type == null) ? remaining : remainingByType.get(type);
            if(needsInner) {
                possiblyAllowed = setupAllowedForReplacementInner(info, possiblyAllowed);
            }
            if(!possiblyAllowed.isEmpty()) {
                return possiblyAllowed;
            }
            //else - it didn't work looking at remaining. Let's try allowed.

            possiblyAllowed = (type == null) ? allowed : allowedByType.get(type);
            if(needsInner) {
                possiblyAllowed = setupAllowedForReplacementInner(info, possiblyAllowed);
            }
            if(!possiblyAllowed.isEmpty()) {
                return possiblyAllowed;
            }

            //it didn't work for allowed, either
            throw new RandomizationException("Could not find any replacements for wild encounter " + enc + "!");
        }

        /**
         * Given an encounter and a set of potential replacements for that encounter, narrows it down to valid
         * replacements for the encounter.
         * Assumes that any type restrictions have already been applied.
         * @param enc The encounter to set up replacements for.
         * @param startingPool The pool to start from.
         * @return startingPool if all {@link Species} in it were valid; a new {@link SpeciesSet} containing
         * all valid {@link Species} from startingPool otherwise.
         */
        private SpeciesSet setupAllowedForReplacement(Encounter enc, SpeciesSet startingPool) {
            if(areaInformationMap == null) {
                //we have no information about this encounter, and assume that means it needs no individual treatment.
                return startingPool;
            }
            SpeciesAreaInformation info = areaInformationMap.get(enc.getSpecies());
            if(info == null) {
                //technically, this is the same situation as the above. However, this should not happen with the current
                //flow, so we throw an exception.
                throw new IllegalStateException("Info was null for encounter's species!");
            }

            if(info.getBannedForReplacement().isEmpty() &&
                    !keepEvolutions) {
                //allowedForReplacement is exactly startingPool
                return startingPool;
            }

            //we actually need to run the inner

            return setupAllowedForReplacementInner(info, startingPool);
            //no check if it's empty, because we don't have the information needed to retry if it is.
        }

        /**
         * Given a {@link SpeciesAreaInformation} and a pool of {@link Species}, narrows the pool down to
         * {@link Species} valid as determined by the {@link SpeciesAreaInformation}.
         * Assumes all type restrictions have already been applied.
         * @param info The restrictions for the current encounter.
         * @param startingPool The pool to start from.
         * @return startingPool if no additional restrictions were applied, a new {@link SpeciesSet} with the narrowed
         * set otherwise.
         */
        private SpeciesSet setupAllowedForReplacementInner(SpeciesAreaInformation info, SpeciesSet startingPool) {
            SpeciesSet allowedForReplacement;
            if(!info.getBannedForReplacement().isEmpty()) {
                allowedForReplacement = new SpeciesSet(startingPool);
                allowedForReplacement.removeAll(info.getBannedForReplacement());
            } else {
                allowedForReplacement = startingPool;
            }

            if(keepEvolutions) {
                allowedForReplacement = setupAllowedForFamily(allowedForReplacement, info);
            }
            return allowedForReplacement;
        }

        private Species pickReplacement(Species current, SpeciesSet allowedForReplacement) {
            if (allowedForReplacement == null || allowedForReplacement.isEmpty()) {
                throw new IllegalArgumentException("No allowed Species to pick as replacement.");
            }

            Species replacement;
            // In Catch 'Em All mode, don't randomize encounters for Species that are banned for
            // wild encounters. Otherwise, it may be impossible to obtain this Species unless it
            // randomly appears as a static or unless it becomes a random evolution.
            if (catchEmAll && banned.contains(current)) {
                replacement = current;
            } else if (similarStrength) {
                if(balanceShakingGrass) {
                    SpeciesAreaInformation info = areaInformationMap.get(current);
                    int bstToUse = Math.min(current.getBSTForPowerLevels(), info.getLowestLevel() * 10 + 250);

                    replacement = allowedForReplacement.getRandomSimilarStrengthSpecies(bstToUse, random);
                } else {
                    replacement = allowedForReplacement.getRandomSimilarStrengthSpecies(current, random);
                }
            } else {
                replacement = allowedForReplacement.getRandomSpecies(random);
            }
            return replacement;
        }

        /**
         * Removes the given {@link Species} from "remaining" and all variants that are in use.
         * If remaining is empty after removing, refills it.
         * @param replacement The {@link Species} to remove.
         */
        private void removeFromRemaining(Species replacement) {
            remaining.remove(replacement);
            if (needsTypes) {
                remainingByType.get(replacement.getPrimaryType(false)).remove(replacement);
                if (replacement.hasSecondaryType(false)) {
                    remainingByType.get(replacement.getSecondaryType(false)).remove(replacement);
                }
            }

            if(remaining.isEmpty()) {
                refillRemainingSpecies();
            }
        }

        private void enforceMultipleSpecies(EncounterArea area) {
            // If an area with forceMultipleSpecies yet has a single species,
            // just randomly pick a different species for one of the Encounters.
            // This is very unlikely to happen in practice, even with very
            // restrictive settings, so it should be okay to break logic here.
            while (area.stream().distinct().count() == 1) {
                area.get(0).setSpecies(rSpecService.randomSpecies(random));
            }
        }

        /**
         * For each {@link Species} in the areaInfoMap, for each that has a type theme, adds that theme
         * to each listed member of its family. <br>
         * setupAreaInfoMap() must be called before this method!
         * @throws IllegalStateException if areaInformationMap is null.
         * @throws IllegalArgumentException if families contains a {@link Species} which has no
         * information in areaInformationMap.
         */
        private void spreadThemesThroughFamilies() {
            SpeciesSet completedFamilies = new SpeciesSet();
            if(areaInformationMap == null) {
                throw new IllegalStateException("Cannot spread themes before determining themes!");
            }
            for(SpeciesAreaInformation info : areaInformationMap.values()) {
                if(info == null) {
                    throw new IllegalStateException("AreaInfoMap contained a null value!");
                }
                Species poke = info.getSpecies();
                if(completedFamilies.contains(poke)) {
                    continue;
                }

                SpeciesSet family = info.getFamily();
                completedFamilies.addAll(family);

                //this algorithm weights any area which contains (for example) two Species in the family twice as strongly
                //this is probably acceptable
                Map<Type, Integer> familyThemeInfo = new EnumMap<>(Type.class);
                for(Species relative : family) {

                    //get this Species's possible themes
                    SpeciesAreaInformation relativeInfo = areaInformationMap.get(relative);
                    if(relativeInfo == null) {
                        throw new IllegalArgumentException("Cannot spread themes among Species without theme information!");
                    }
                    Map<Type, Integer> themeInfo = relativeInfo.getAllPossibleThemes();

                    //add them to the total theme info
                    for(Map.Entry<Type, Integer> possibleTheme : themeInfo.entrySet()) {
                        Type theme = possibleTheme.getKey();
                        int count = possibleTheme.getValue();

                        if(familyThemeInfo.containsKey(theme)) {
                            int existingCount = familyThemeInfo.get(theme);
                            count += existingCount;
                        }
                        familyThemeInfo.put(theme, count);
                    }
                }

                //set our determined theme info to the whole family
                for(Species relative : family) {
                    SpeciesAreaInformation relativeInfo = areaInformationMap.get(relative);
                    if(relativeInfo == null) {
                        //shouldn't be possible
                        throw new RuntimeException("Species's info became null between checking and setting themes??");
                    }
                    relativeInfo.setPossibleThemes(familyThemeInfo);
                }

            }
        }

        /**
         * Narrows the given pool of {@link Species} down to one that is compatible with the family contained in the
         * given area information. Uses the full allowed pool for relatives.
         * Ignores all type restrictions.
         * @param potentiallyAllowed The set of {@link Species} to work from.
         * @param info The information of the {@link Species} to match.
         * @return A new {@link SpeciesSet} narrowed down as specified.
         * @throws RandomizationException if no match for the given family can be found in the allowed pool.
         */
        private SpeciesSet setupAllowedForFamily(SpeciesSet potentiallyAllowed, SpeciesAreaInformation info) {
            SpeciesSet family = info.getFamily();
            Species match = info.getSpecies();

            int before = family.getNumberEvoStagesBefore(match, true);
            int after = family.getNumberEvoStagesAfter(match, true);
            potentiallyAllowed = potentiallyAllowed.filterHasEvoStages(before, after, false);

            for(Species relative : family) {
                int relation = match.getRelation(relative, true);

                //Remove all Species for which "relative" cannot be replaced by any corresponding relative
                //either because it's not in the allowed pool, or it's banned
                potentiallyAllowed = potentiallyAllowed.filter(p -> {
                       SpeciesSet sameRelations = p.getRelativesAtPositionSameBranch(relation, false);
                       sameRelations.retainAll(allowed);
                       sameRelations.removeAll(areaInformationMap.get(relative).getBannedForReplacement());
                       return !sameRelations.isEmpty();
                });
            }

            //Try to remove any Species which have a relative that has already been used
            SpeciesSet withoutUsedFamilies = potentiallyAllowed.filter(p ->
                    !p.getFamily(false).containsAny(regionMap.keySet()));

            return withoutUsedFamilies.isEmpty() ? potentiallyAllowed : withoutUsedFamilies;
        }

        /**
         * Given the EncounterAreas for a single map, calculates the DexNav load for that map.
         * The DexNav crashes if this load is above ORAS_CRASH_THRESHOLD.
         * @param areasInMap A List of EncounterAreas, all of which are from the same map.
         * @return The DexNav load for that map.
         */
        private int getORASDexNavLoad(List<EncounterArea> areasInMap) {
            //If the previous implementation is to be believed,
            //the load is equal to the number of distinct Species in each area summed.
            //(Not the total number of unique Species).
            //I am not going to attempt to verify this (yet).
            int load = 0;
            for(EncounterArea area : areasInMap) {
                if(area.getEncounterType() == EncounterType.INTERACT) {
                    //Rock Smash doesn't contribute to DexNav load.
                    continue;
                }

                SpeciesSet speciesInArea = new SpeciesSet();
                for (Species poke : area.getSpeciesInArea()) {
                    //Different formes of the same Species do not contribute to load
                    if(poke.isBaseForme()) {
                        speciesInArea.add(poke);
                    } else {
                        speciesInArea.add(poke.getBaseForme());
                    }
                }

                load += speciesInArea.size();
            }
            return load;
        }

        /**
         * Given a set of EncounterAreas, creates a map of every {@link Species} in the areas to
         * information about the areas that {@link Species} is contained in.
         * @param areas The list of EncounterAreas to explore.
         */
        private void setupAreaInfoMap(List<EncounterArea> areas) {

            SpeciesSet existingSpecies = new SpeciesSet();

            areaInformationMap = new HashMap<>();
            for(EncounterArea area : areas) {
                Type areaTheme = findAreaType(area);
                int areaSize = area.getSpeciesInArea().size();

                for(Species species : area.getSpeciesInArea()) {
                    SpeciesAreaInformation info = areaInformationMap.get(species);

                    if(info == null) {
                        info = new SpeciesAreaInformation(species);
                        areaInformationMap.put(species, info);

                        if(keepEvolutions) {
                            existingSpecies.add(species);
                            SpeciesSet family = existingSpecies.filterFamily(species, true);
                            if(family.size() > 1) {
                                family.forEach(relative -> areaInformationMap.get(relative).addFamily(family));
                            }
                        }
                    }

                    info.addTypeTheme(areaTheme, areaSize);
                    info.banAll(area.getBannedSpecies());
                }
                if(balanceShakingGrass) {
                    //TODO: either verify that this IS a shaking grass encounter,
                    // or rename the setting.
                    // (Leaning towards the latter.)
                    for (Encounter enc : area) {
                        SpeciesAreaInformation info = areaInformationMap.get(enc.getSpecies());
                        info.setLevelIfLower((enc.getLevel() + enc.getMaxLevel()) / 2);
                        //TODO: *Should* this be average level? Or should it be lowest?
                    }
                }
            }
        }

        /**
         * A class which stores some information about the areas a {@link Species} was found in,
         * in order to allow us to use this information later.
         */
        private class SpeciesAreaInformation {
            private Map<Type, Integer> possibleThemes = new EnumMap<>(Type.class);
            private final SpeciesSet bannedForReplacement = new SpeciesSet();
            private final SpeciesSet family = new SpeciesSet();
            private final Species species;
            private int lowestLevel = 100;

            /**
             * Creates a new RandomizationInformation with the given data.
             * @param sp The {@link Species} this RandomizationInformation is about.
             */
            SpeciesAreaInformation(Species sp) {
                species = sp;
            }

            /**
             * Adds all {@link Species} in the given collection to the set of {@link Species} banned for replacement.
             * @param banned The Collection of {@link Species} to add.
             */
            public void banAll(Collection<Species> banned) {
                bannedForReplacement.addAll(banned);
            }

            /**
             * Get the list of all {@link Species} banned as replacements for this {@link Species}.
             * @return A new unmodifiable {@link SpeciesSet} containing the banned {@link Species}.
             */
            public SpeciesSet getBannedForReplacement() {
                return SpeciesSet.unmodifiable(bannedForReplacement);
            }

            /**
             * Adds the given type and count of {@link Species} to the list of existing themes for
             * this {@link Species}. <br>
             * If theme is null, has no effect.
             * @param theme The type to add.
             * @throws IllegalArgumentException if count is less than 1.
             */
            public void addTypeTheme(Type theme, int count) {
                if (count < 1) {
                    throw new IllegalArgumentException("Number of Species in theme cannot be less than 1!");
                }
                if(theme != null) {
                    if(possibleThemes.containsKey(theme)) {
                        int existingCount = possibleThemes.get(theme);
                        count += existingCount;
                    }
                    possibleThemes.put(theme, count);
                }
            }

            /**
             * Gets the type of this {@link Species}'s area theming. <br>
             * If there are two or more themes, returns the one with the highest count of {@link Species}. If tied,
             * will choose the {@link Species}'s original primary type. If neither theme is the original primary,
             * chooses one arbitrarily.<br>
             * If there are no themes, it will default to the original primary only if defaultToPrimary is true;
             * otherwise, it will default to null.
             * @param defaultToPrimary Whether the type should default to the {@link Species}'s primary type
             *                         if there are no themes.
             * @return The type that should be used, or null for any type.
             */
            Type getTheme(boolean defaultToPrimary) {
                if(possibleThemes.isEmpty()) {
                    if(defaultToPrimary) {
                        return species.getPrimaryType(true);
                    } else {
                        return null;
                    }
                } else {
                    Type bestTheme = null;
                    int bestThemeCount = 0;
                    for(Map.Entry<Type, Integer> possibleTheme : possibleThemes.entrySet()) {
                        int possibleThemeCount = possibleTheme.getValue();
                        if(possibleThemeCount > bestThemeCount) {
                            bestThemeCount = possibleThemeCount;
                            bestTheme = possibleTheme.getKey();
                        } else if(possibleThemeCount == bestThemeCount) {

                            //tie - default to primary if present
                            Type primary = species.getPrimaryType(true);
                            if(primary == possibleTheme.getKey()) {
                                bestTheme = primary;
                            }
                            //if bestTheme is already primary, then no change is needed;
                            //if neither is primary, then we have no means of choosing & thus leave it as is.
                            //(The latter can possibly happen with family-to-family.)
                        }
                    }
                    return bestTheme;
                }
            }

            /**
             * Returns the set of all desired themes for this {@link Species}.
             * @return A new Set containing all the possible themes.
             */
            Map<Type, Integer> getAllPossibleThemes() {
                return new EnumMap<>(possibleThemes);
            }

            /**
             * Sets the possible themes to match the data given.
             * @param replacementThemes A map of Types to weights of those types. (Normally, the highest weight will
             *                          be the type used.)
             */
            void setPossibleThemes(Map<Type, Integer> replacementThemes) {
                possibleThemes = new EnumMap<>(replacementThemes);
            }

            /**
             * Adds the {@link Species} in the given set to this {@link Species}'s family.
             * @param family The {@link Species} to add.
             */
            void addFamily(SpeciesSet family) {
                this.family.addAll(family);
            }

            /**
             * Gets any members of the {@link Species}'s family that have been added to the information.
             * @return A new unmodifiable {@link SpeciesSet} containing the {@link Species}'s family.
             */
            SpeciesSet getFamily() {
                return SpeciesSet.unmodifiable(family);
            }

            /**
             * Gets the {@link Species} that this SpeciesAreaInformation is about.
             * @return The {@link Species}.
             */
            public Species getSpecies() {
                return species;
            }

            /**
             * Sets the lowest level to the level given, if it is lower than the current lowest level.
             * @param level The level to lower to.
             */
            void setLevelIfLower(int level) {
                lowestLevel = Math.min(level, lowestLevel);
            }

            /**
             * Gets the lowest level encounter with this Species in the region.
             * @return The lowest level.
             */
            public int getLowestLevel() {
                return lowestLevel;
            }
        }
    }

    /**
     * Prepares the EncounterAreas for randomization by copying them, removing unused areas, and shuffling the order.
     * @param unprepped The List of EncounterAreas to prepare.
     * @return A new List of all the same Encounters, with the areas shuffled and possibly merged as appropriate.
     */
    private List<EncounterArea> prepEncounterAreas(List<EncounterArea> unprepped) {
        // Clone the original set, so that we don't mess up saving
        List<EncounterArea> prepped = new ArrayList<>(unprepped);

        prepped.removeIf(area -> area.getEncounterType() == EncounterType.UNUSED
                || "UNUSED".equals(area.getLocationTag()));
        //don't randomize unused areas
        //mostly important for catch 'em all

        // Shuffling the EncounterAreas leads to less predictable results for various modifiers.
        Collections.shuffle(prepped, random);
        return prepped;
    }

    private void setFormeForEncounter(Encounter enc, Species sp) {
        boolean checkCosmetics = true;
        enc.setFormeNumber(0);
        if (enc.getSpecies().getFormeNumber() > 0) {
            enc.setFormeNumber(enc.getSpecies().getFormeNumber());
            enc.setSpecies(enc.getSpecies().getBaseForme());
            checkCosmetics = false;
        }
        if (checkCosmetics && enc.getSpecies().getCosmeticForms() > 0) {
            enc.setFormeNumber(enc.getSpecies().getCosmeticFormNumber(this.random.nextInt(enc.getSpecies().getCosmeticForms())));
        } else if (!checkCosmetics && sp.getCosmeticForms() > 0) {
            enc.setFormeNumber(enc.getFormeNumber() + sp.getCosmeticFormNumber(this.random.nextInt(sp.getCosmeticForms())));
        }
        //TODO: instead of (most of) this function, have encounter store the actual forme used and call baseSpecies when needed
        // Or.. some other solution to the problem of not recognizing formes in ORAS "enhance" logic
    }

    public void changeCatchRates() {
        int minimumCatchRateLevel = settings.getMinimumCatchRateLevel();

        if (minimumCatchRateLevel == 5) {
            romHandler.enableGuaranteedPokemonCatching();
        } else {
            int normalMin, legendaryMin;
            switch (minimumCatchRateLevel) {
                case 1:
                default:
                    normalMin = 75;
                    legendaryMin = 37;
                    break;
                case 2:
                    normalMin = 128;
                    legendaryMin = 64;
                    break;
                case 3:
                    normalMin = 200;
                    legendaryMin = 100;
                    break;
                case 4:
                    normalMin = legendaryMin = 255;
                    break;
            }
            minimumCatchRate(normalMin, legendaryMin);
        }
    }

    private void minimumCatchRate(int rateNonLegendary, int rateLegendary) {
        for (Species sp : romHandler.getSpeciesSetInclFormes()) {
            int minCatchRate = sp.isLegendary() ? rateLegendary : rateNonLegendary;
            sp.setCatchRate(Math.max(sp.getCatchRate(), minCatchRate));
        }

    }

}
