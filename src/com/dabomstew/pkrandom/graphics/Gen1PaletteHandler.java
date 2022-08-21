package com.dabomstew.pkrandom.graphics;

/*----------------------------------------------------------------------------*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import java.util.EnumMap;
import java.util.Random;

import com.dabomstew.pkrandom.pokemon.*;

/**
 * A {@link PaletteHandler} for Gen 1 games (R/B/Y).
 */
public class Gen1PaletteHandler extends PaletteHandler<Gen1Pokemon> {

	private static final PaletteID DEFAULT_PALETTE_ID = PaletteID.MEWMON;
	private static final EnumMap<Type, PaletteID[]> TYPE_PALETTE_IDS = initTypePaletteIDs();

	private boolean typeSanity;

	public Gen1PaletteHandler(Random random) {
		super(random);
	}

	private static EnumMap<Type, PaletteID[]> initTypePaletteIDs() {
		EnumMap<Type, PaletteID[]> typePaletteIDs = new EnumMap<>(Type.class);
		typePaletteIDs.put(Type.NORMAL, new PaletteID[] { PaletteID.BROWNMON, PaletteID.PINKMON, PaletteID.GREYMON });
		typePaletteIDs.put(Type.FIGHTING, new PaletteID[] { PaletteID.BROWNMON, PaletteID.GREYMON });
		typePaletteIDs.put(Type.FLYING, new PaletteID[] { PaletteID.BLUEMON });
		typePaletteIDs.put(Type.GRASS, new PaletteID[] { PaletteID.GREENMON });
		typePaletteIDs.put(Type.WATER, new PaletteID[] { PaletteID.BLUEMON, PaletteID.CYANMON });
		typePaletteIDs.put(Type.FIRE, new PaletteID[] { PaletteID.REDMON });
		typePaletteIDs.put(Type.ROCK, new PaletteID[] { PaletteID.GREYMON });
		typePaletteIDs.put(Type.GROUND, new PaletteID[] { PaletteID.BROWNMON });
		typePaletteIDs.put(Type.PSYCHIC, new PaletteID[] { PaletteID.MEWMON, PaletteID.YELLOWMON });
		typePaletteIDs.put(Type.BUG, new PaletteID[] { PaletteID.GREENMON, PaletteID.YELLOWMON });
		typePaletteIDs.put(Type.DRAGON, new PaletteID[] { PaletteID.BLUEMON, PaletteID.BROWNMON, PaletteID.GREYMON });
		typePaletteIDs.put(Type.ELECTRIC, new PaletteID[] { PaletteID.YELLOWMON });
		typePaletteIDs.put(Type.GHOST, new PaletteID[] { PaletteID.PURPLEMON });
		typePaletteIDs.put(Type.POISON, new PaletteID[] { PaletteID.PURPLEMON });
		typePaletteIDs.put(Type.ICE, new PaletteID[] { PaletteID.BLUEMON, PaletteID.CYANMON });
		return typePaletteIDs;
	}

	@Override
	public void randomizePokemonPalettes(PokemonSet<Gen1Pokemon> pokemonSet, boolean typeSanity,
										 boolean evolutionSanity, boolean shinyFromNormal) {
		// obviously shinyFromNormal is not used, it is here for a hopefully prettier
		// class structure
		this.typeSanity = typeSanity;
		CopyUpEvolutionsHelper<Gen1Pokemon> cueh = new CopyUpEvolutionsHelper<>(() -> pokemonSet);
		cueh.apply(evolutionSanity, true, new BasePokemonIDAction(), new EvolvedPokemonIDAction());
	}

	private PaletteID getRandomPaletteID() {
		return PaletteID.getRandomPokemonPaletteID(random);
	}

	private PaletteID getRandomPaletteID(Type type) {
		PaletteID[] typeIDs = TYPE_PALETTE_IDS.get(type);
		return typeIDs == null ? DEFAULT_PALETTE_ID : typeIDs[random.nextInt(typeIDs.length)];
	}

	private class BasePokemonIDAction implements CopyUpEvolutionsHelper.BasicPokemonAction<Gen1Pokemon> {

		@Override
		public void applyTo(Gen1Pokemon pk) {
			pk.paletteID = typeSanity ? getRandomPaletteID(pk.primaryType) : getRandomPaletteID();
		}

	}

	private class EvolvedPokemonIDAction implements CopyUpEvolutionsHelper.EvolvedPokemonAction<Gen1Pokemon> {

		@Override
		public void applyTo(Gen1Pokemon evFrom, Gen1Pokemon evTo, boolean toMonIsFinalEvo) {
			PaletteID newPaletteID;
			if (typeSanity && !evTo.primaryType.equals(evFrom.primaryType)) {
				PaletteID[] typeIDs = TYPE_PALETTE_IDS.get(evTo.primaryType);
				newPaletteID = contains(typeIDs, evFrom.paletteID) ? evFrom.paletteID
						: getRandomPaletteID(evTo.primaryType);

			} else {
				newPaletteID = evFrom.paletteID;
			}
			evTo.paletteID = newPaletteID;
		}

	}

	private boolean contains(PaletteID[] paletteIDs, PaletteID pid) {
		for (PaletteID pid2 : paletteIDs) {
			if (pid == pid2) {
				return true;
			}
		}
		return false;
	}

}
