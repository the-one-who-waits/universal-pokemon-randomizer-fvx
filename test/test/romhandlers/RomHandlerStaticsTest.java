package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.StaticEncounter;
import com.dabomstew.pkrandom.randomizers.StaticPokemonRandomizer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RomHandlerStaticsTest extends RomHandlerTest {

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void staticPokemonIsNotEmpty(String romName) {
        loadROM(romName);
        System.out.println(romHandler.getStaticPokemon());
        assertFalse(romHandler.getStaticPokemon().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void staticPokemonDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        List<StaticEncounter> statics = romHandler.getStaticPokemon();
        System.out.println(statics);
        List<StaticEncounter> before = deepCopy(statics);
        romHandler.setStaticPokemon(statics);
        List<StaticEncounter> after = romHandler.getStaticPokemon();
        for (int i = 0; i < before.size(); i++) {
            if (after.size() != before.size()) {
                throw new RuntimeException("size mismatch, before:" + before.size() + " after:" + after.size());
            }
            System.out.println("Before:");
            System.out.println(toLongString(before.get(i), false));
            System.out.println("After:");
            System.out.println(toLongString(after.get(i), false));
            System.out.println();
            assertEquals(before.get(i), after.get(i));
        }
    }

    private String toLongString(StaticEncounter se, boolean isLinkedEncounter) {
        StringBuilder sb = new StringBuilder();
        sb.append(se.pkmn.fullName());
        sb.append(" forme=").append(se.forme);
        sb.append(" level=").append(se.level);
        if (se.maxLevel > 0) {
            sb.append(" maxLevel=").append(se.maxLevel);
        }
        sb.append(" isEgg=").append(se.isEgg);
        sb.append(" resetMoves=").append(se.resetMoves);
        sb.append(" restrictedPool=").append(se.restrictedPool);

        sb.append(" restrictedList=");
        if (se.restrictedList == null) {
            sb.append("null");
        } else {
            sb.append("(");
            sb.append(se.restrictedList.stream()
                    .map(Pokemon::fullName)
                    .collect(Collectors.joining(",")));
            sb.append(")");
        }

        sb.append(" linkedEncounters=");
        if (se.linkedEncounters == null) {
            sb.append("null");
        } else if (se.linkedEncounters.isEmpty()) {
            sb.append("[]");
        } else {
            if (isLinkedEncounter) {
                throw new IllegalArgumentException("linkedEncounter should not have linkedEncounters of its own!");
            }
            sb.append("[\n");
            for (StaticEncounter linked : se.linkedEncounters) {
                sb.append("\t");
                sb.append(toLongString(linked, true));
                sb.append("\n");
            }
            sb.append("]");
        }

        return sb.toString();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void randomStaticsSwapLegForLegNormalForNormalWorks(String romName) {
        loadROM(romName);
        List<StaticEncounter> before = deepCopy(romHandler.getStaticPokemon());

        Settings s = new Settings();
        s.setStaticPokemonMod(Settings.StaticPokemonMod.RANDOM_MATCHING);
        new StaticPokemonRandomizer(romHandler, s, RND).randomizeStaticPokemon();

        List<StaticEncounter> after = romHandler.getStaticPokemon();
        if (before.size() != after.size()) {
            throw new RuntimeException("static pokemon list mismatch");
        }
        for (int i = 0; i < before.size(); i++) {
            Pokemon befPk = before.get(i).pkmn;
            Pokemon aftPk = after.get(i).pkmn;
            System.out.println("bef=" + befPk.fullName() + (befPk.isLegendary() ? " (legendary)" : "") +
                    ", aft=" + aftPk.fullName() + (aftPk.isLegendary() ? " (legendary)" : ""));
            assertEquals(befPk.isLegendary(), aftPk.isLegendary());
        }

    }

    private List<StaticEncounter> deepCopy(List<StaticEncounter> original) {
        List<StaticEncounter> copy = new ArrayList<>(original.size());
        for (StaticEncounter se : original) {
            copy.add(new StaticEncounter(se));
        }
        return copy;
    }

}