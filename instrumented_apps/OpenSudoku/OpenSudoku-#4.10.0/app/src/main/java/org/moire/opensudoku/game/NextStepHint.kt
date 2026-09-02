/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2025 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.moire.opensudoku.game

import android.content.Context
import org.moire.opensudoku.game.nextstep.HintLevels
import org.moire.opensudoku.game.nextstep.NextStep
import org.moire.opensudoku.game.nextstep.NextStepBUG
import org.moire.opensudoku.game.nextstep.NextStepClaimingPairTriple
import org.moire.opensudoku.game.nextstep.NextStepChuteRemotePair
import org.moire.opensudoku.game.nextstep.NextStepEmptyRectangle
import org.moire.opensudoku.game.nextstep.NextStepFish3nSwordfish
import org.moire.opensudoku.game.nextstep.NextStepFish2nXWing
import org.moire.opensudoku.game.nextstep.NextStepFish4nJellyfish
import org.moire.opensudoku.game.nextstep.NextStepFish5nStarfish
import org.moire.opensudoku.game.nextstep.NextStepFish6nWhale
import org.moire.opensudoku.game.nextstep.NextStepFish7nLeviathan
import org.moire.opensudoku.game.nextstep.NextStepLastDigit
import org.moire.opensudoku.game.nextstep.NextStepHiddenPair
import org.moire.opensudoku.game.nextstep.NextStepHiddenQuad
import org.moire.opensudoku.game.nextstep.NextStepHiddenSingle
import org.moire.opensudoku.game.nextstep.NextStepHiddenTriple
import org.moire.opensudoku.game.nextstep.NextStepMissingCandidate
import org.moire.opensudoku.game.nextstep.NextStepNakedPair
import org.moire.opensudoku.game.nextstep.NextStepNakedQuad
import org.moire.opensudoku.game.nextstep.NextStepNakedSingle
import org.moire.opensudoku.game.nextstep.NextStepNakedTriple
import org.moire.opensudoku.game.nextstep.NextStepNotFound
import org.moire.opensudoku.game.nextstep.NextStepObsoleteCandidate
import org.moire.opensudoku.game.nextstep.NextStepPointingPairTriple
import org.moire.opensudoku.game.nextstep.NextStepRemotePair
import org.moire.opensudoku.game.nextstep.NextStepSimpleColoring
import org.moire.opensudoku.game.nextstep.NextStepTurbot2StringKite
import org.moire.opensudoku.game.nextstep.NextStepTurbotCrane
import org.moire.opensudoku.game.nextstep.NextStepTurbotSkyscraper
import org.moire.opensudoku.game.nextstep.NextStepUniqueRectangle
import org.moire.opensudoku.game.nextstep.NextStepWXYZWing
import org.moire.opensudoku.game.nextstep.NextStepWrongValue
import org.moire.opensudoku.game.nextstep.NextStepXChain
import org.moire.opensudoku.game.nextstep.NextStepXYChain
import org.moire.opensudoku.game.nextstep.NextStepXYWing
import org.moire.opensudoku.game.nextstep.NextStepXYZWing

/** NextStepHint
 *
 * This class is the interface between the gui and the implemented strategies to find a next step.
 * Object of a next step is to reduce the candidates or to find a value for a cell.
 *
 * Data used to find the next step:
 * 	- value of the cells
 * 		- given and entered
 * 	- the primary marks, the secondary marks are not used
 *  - solutions value for the cell
 *
 * The output to user is on one hand a message with the main details to understand the
 * used strategy and on the other highlighted cells to see the region, the cause and the target
 * of the used strategy.
 *
 * A list of the implemented strategies can be found in the class NextStep.
 */
class NextStepHint(private val context: Context,
				   private val board: SudokuBoard,
	               private val hintLevel: HintLevels) {

	fun getNextStepHint(): NextStep {

		var nextStep: Any?

		// check for wrong values
		nextStep = NextStepWrongValue(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for missing candidates
		nextStep = NextStepMissingCandidate(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for obsolete candidates
		nextStep = NextStepObsoleteCandidate(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Full House (Last Digit)
		nextStep = NextStepLastDigit(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Naked Single
		nextStep = NextStepNakedSingle(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Hidden Single
		nextStep = NextStepHiddenSingle(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Naked Pair / Locked Pair -> Naked Group with size 2
		nextStep = NextStepNakedPair(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Hidden Pair -> Hidden Group with size 2
		nextStep = NextStepHiddenPair(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Pointing Pair / Pointing Triple
		nextStep = NextStepPointingPairTriple(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Claiming Pair / Claiming Triple
		nextStep = NextStepClaimingPairTriple(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Naked Triple / Locked Triple -> Naked Group with size 3
		nextStep = NextStepNakedTriple(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Hidden Triple -> Hidden Group with size 3
		nextStep = NextStepHiddenTriple(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Naked Quad -> Naked Group with size 4
		nextStep = NextStepNakedQuad(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Hidden Quad -> Hidden Group with size 4
		nextStep = NextStepHiddenQuad(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for X-Wing -> Fish Basic Group with size 2
		nextStep = NextStepFish2nXWing(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Turbot Skyscraper
		nextStep = NextStepTurbotSkyscraper(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Turbot 2-String-Kite
		nextStep = NextStepTurbot2StringKite(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Turbot Crane
		nextStep = NextStepTurbotCrane(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for XY-Wing
		nextStep = NextStepXYWing(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for XYZ-Wing
		nextStep = NextStepXYZWing(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Remote Pair
		nextStep = NextStepRemotePair(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Chute Remote Pair
		nextStep = NextStepChuteRemotePair(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Simple Coloring Type 1 and 2
		nextStep = NextStepSimpleColoring(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Empty Rectangle
		nextStep = NextStepEmptyRectangle(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Swordfish -> Fish Basic Group with size 3
		nextStep = NextStepFish3nSwordfish(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for X-Chain
		nextStep = NextStepXChain(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for XY-Chain
		nextStep = NextStepXYChain(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for BUG
		nextStep = NextStepBUG(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Unique Rectangle Type 1-7
		nextStep = NextStepUniqueRectangle(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Jellyfish -> Fish Basic Group with size 4
		nextStep = NextStepFish4nJellyfish(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for WXYZ-Wing
		nextStep = NextStepWXYZWing(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Starfish -> Fish Basic Group with size 5
		nextStep = NextStepFish5nStarfish(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Whale -> Fish Basic Group with size 6
		nextStep = NextStepFish6nWhale(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// check for Leviathan -> Fish Basic Group with size 7
		nextStep = NextStepFish7nLeviathan(context,board,hintLevel)
		if ( nextStep.search() ) return nextStep

		// no next step found
		nextStep = NextStepNotFound(context,board,hintLevel)
		return nextStep
	}

}
