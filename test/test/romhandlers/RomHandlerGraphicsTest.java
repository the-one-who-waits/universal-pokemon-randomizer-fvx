package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.constants.Gen3Constants;
import com.dabomstew.pkrandom.graphics.packs.*;
import com.dabomstew.pkrandom.romhandlers.Gen3RomHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class RomHandlerGraphicsTest extends RomHandlerTest {

    private static final String TEST_CPG_PATH = "test/players";

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void dumpAllPokemonImages(String romName) {
        loadROM(romName);
        romHandler.dumpAllPokemonImages();
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canSetCustomPlayerGraphicsWithoutThrowing(String romName) {
        loadROM(romName);
        assumeTrue(romHandler.hasCustomPlayerGraphicsSupport());
        GraphicsPack cpg = getCustomPlayerGraphics();
        romHandler.setCustomPlayerGraphics(cpg, Settings.PlayerCharacterMod.PC1);
        if (romHandler.hasMultiplePlayerCharacters()) {
            romHandler.setCustomPlayerGraphics(cpg, Settings.PlayerCharacterMod.PC2);
        }
        assertTrue(true);
    }

    private static final List<GraphicsPackEntry> cpgEntries = initCPGEntries();

    private static List<GraphicsPackEntry> initCPGEntries() {
        try {
            return GraphicsPackEntry.readAllFromFolder(TEST_CPG_PATH);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private GraphicsPack getCustomPlayerGraphics() {
        GraphicsPack cpg;
        switch (romHandler.generationOfPokemon()) {
            case 1 -> cpg = new Gen1PlayerCharacterGraphics(cpgEntries.get(0));
            case 2 -> cpg = new Gen2PlayerCharacterGraphics(cpgEntries.get(1));
            case 3 -> {
                Gen3RomHandler gen3RomHandler = (Gen3RomHandler) romHandler;
                return gen3RomHandler.getRomEntry().getRomType() == Gen3Constants.RomType_FRLG ?
                        new FRLGPlayerCharacterGraphics(cpgEntries.get(3)) :
                        new RSEPlayerCharacterGraphics(cpgEntries.get(2));
            }
            default -> cpg = null;
        }
        return cpg;
    }
}
