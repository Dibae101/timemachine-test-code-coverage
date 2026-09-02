/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2023 by original authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.moire.opensudoku.game.nextstep

import android.content.Context
import org.moire.opensudoku.R
import org.moire.opensudoku.game.SudokuBoard

/** enum class with an id for each strategy level
 */
enum class StrategyLevelIds {
	NONE,
	BASIC,
	INTERMEDIATE,
	ADVANCED,
	MASTER;

	fun getStrategyLevelName(context: Context) : String {
		val strategyLevelName = when( this ) {
			NONE -> "./."
			BASIC -> context.getString(R.string.hint_strategy_level_basic)
			INTERMEDIATE -> context.getString(R.string.hint_strategy_level_intermediate)
			ADVANCED -> context.getString(R.string.hint_strategy_level_advanced)
			MASTER -> context.getString(R.string.hint_strategy_level_master)
		}
		return strategyLevelName
	}

	fun getStrategyLevelId() : Int {
		val strategyLevelId = when( this ) {
			NONE -> 0
			BASIC -> 1
			INTERMEDIATE -> 2
			ADVANCED -> 3
			MASTER -> 4
		}
		return strategyLevelId
	}

	companion object {

		fun getStrategyLevel(id: Int): StrategyLevelIds {
			return when(id) {
				0 -> NONE
				1 -> BASIC
				2 -> INTERMEDIATE
				3 -> ADVANCED
				4 -> MASTER
				else -> NONE
			}
		}

	}
}


/** enum class with an id for each strategy
 */
enum class StrategyIds {
	NONE,
	WRONG_VALUES,
	MISSING_CANDIDATES,
	OBSOLETE_CANDIDATES,
	LAST_DIGIT,
	NAKED_SINGLE,
	HIDDEN_SINGLE,
	NAKED_GROUP,
	NAKED_PAIR,
	LOCKED_GROUP,
	LOCKED_PAIR,
	HIDDEN_GROUP,
	HIDDEN_PAIR,
	POINTING_GROUP,
	POINTING_PAIR,
	POINTING_TRIPLE,
	CLAIMING_GROUP,
	CLAIMING_PAIR,
	CLAIMING_TRIPLE,
	NAKED_TRIPLE,
	LOCKED_TRIPLE,
	HIDDEN_TRIPLE,
	NAKED_QUAD,
	HIDDEN_QUAD,
	BASIC_FISH_GROUP,
	BASIC_FISH_2N_X_WING,
	REMOTE_PAIR,
	CHUTE_REMOTE_PAIR,
	CHUTE_REMOTE_PAIR_SINGLE,
	CHUTE_REMOTE_PAIR_DOUBLE,
	CHUTE_REMOTE_PAIR_BONUS,
	SIMPLE_COLORING_TYPE_1,
	SIMPLE_COLORING_TYPE_2,
	TURBOT_SKYSCRAPER,
	TURBOT_2_STRING_KITE,
	TURBOT_CRANE,
	EMPTY_RECTANGLE,
	BASIC_FISH_3N_SWORDFISH,
	XY_WING,
	XYZ_WING,
	X_CHAIN,
	X_CHAIN_LOOP,
	X_CHAIN_ONE_ENDPOINT,
	XY_CHAIN,
	XY_CHAIN_LOOP,
	BUG1,
	UNIQUE_RECTANGLE_TYPE_1,
	UNIQUE_RECTANGLE_TYPE_1M,
	UNIQUE_RECTANGLE_TYPE_2,
	UNIQUE_RECTANGLE_TYPE_3,
	UNIQUE_RECTANGLE_TYPE_4,
	UNIQUE_RECTANGLE_TYPE_4M,
	UNIQUE_RECTANGLE_TYPE_5,
	UNIQUE_RECTANGLE_TYPE_5P,
	UNIQUE_RECTANGLE_TYPE_6,
	UNIQUE_RECTANGLE_TYPE_7,
	BASIC_FISH_4N_JELLYFISH,
	WXYZ_WING,
	BASIC_FISH_5N_STARFISH,
	BASIC_FISH_6N_WHALE,
	BASIC_FISH_7N_LEVIATHAN,
	NO_NEXT_STEP_FOUND;


	fun isStrategy(): Boolean {
		val nonStrategies = listOf(
			NONE,
			WRONG_VALUES,
			MISSING_CANDIDATES,
			OBSOLETE_CANDIDATES,
			NAKED_GROUP,
			LOCKED_GROUP,
			HIDDEN_GROUP,
			POINTING_GROUP,
			CLAIMING_GROUP,
			BASIC_FISH_GROUP,
			CHUTE_REMOTE_PAIR, // Group
			NO_NEXT_STEP_FOUND)
		return (this !in nonStrategies)
	}


	fun getStrategyName(context: Context) : String {
		val strategyName = when( this ) {
			// keep the same order as in definition
			NONE -> this.name
			WRONG_VALUES -> context.getString(R.string.hint_strategy_wrong_value)
			MISSING_CANDIDATES -> context.getString(R.string.hint_strategy_missing_candidate)
			OBSOLETE_CANDIDATES -> context.getString(R.string.hint_strategy_obsolete_candidate)
			LAST_DIGIT -> context.getString(R.string.hint_strategy_last_digit)
			NAKED_SINGLE -> context.getString(R.string.hint_strategy_naked_single)
			HIDDEN_SINGLE -> context.getString(R.string.hint_strategy_hidden_single)
			NAKED_GROUP -> context.getString(R.string.hint_strategy_naked_group," > 4")
			NAKED_PAIR -> context.getString(R.string.hint_strategy_naked_pair)
			LOCKED_GROUP -> context.getString(R.string.hint_strategy_locked_group," > 3")
			LOCKED_PAIR -> context.getString(R.string.hint_strategy_locked_pair)
			HIDDEN_GROUP -> context.getString(R.string.hint_strategy_hidden_group," > 4")
			HIDDEN_PAIR -> context.getString(R.string.hint_strategy_hidden_pair)
			POINTING_GROUP -> context.getString(R.string.hint_strategy_pointing_group," > 4")
			POINTING_PAIR -> context.getString(R.string.hint_strategy_pointing_pair)
			POINTING_TRIPLE -> context.getString(R.string.hint_strategy_pointing_triple)
			CLAIMING_GROUP -> context.getString(R.string.hint_strategy_claiming_group," > 3")
			CLAIMING_PAIR -> context.getString(R.string.hint_strategy_claiming_pair)
			CLAIMING_TRIPLE -> context.getString(R.string.hint_strategy_claiming_triple)
			NAKED_TRIPLE -> context.getString(R.string.hint_strategy_naked_triple)
			LOCKED_TRIPLE -> context.getString(R.string.hint_strategy_locked_triple)
			HIDDEN_TRIPLE-> context.getString(R.string.hint_strategy_hidden_triple)
			NAKED_QUAD -> context.getString(R.string.hint_strategy_naked_quad)
			HIDDEN_QUAD -> context.getString(R.string.hint_strategy_hidden_quad)
			BASIC_FISH_GROUP -> context.getString(R.string.hint_strategy_fish_basic_group," > 7")
			BASIC_FISH_2N_X_WING -> context.getString(R.string.hint_strategy_fish_basic_2n)
			REMOTE_PAIR -> context.getString(R.string.hint_strategy_remote_pair)
			CHUTE_REMOTE_PAIR -> context.getString(R.string.hint_strategy_chute_remote_pair_unknown)
			CHUTE_REMOTE_PAIR_SINGLE -> context.getString(R.string.hint_strategy_chute_remote_pair_single)
			CHUTE_REMOTE_PAIR_DOUBLE -> context.getString(R.string.hint_strategy_chute_remote_pair_double)
			CHUTE_REMOTE_PAIR_BONUS -> context.getString(R.string.hint_strategy_chute_remote_pair_bonus)
			SIMPLE_COLORING_TYPE_1 -> context.getString(R.string.hint_strategy_simple_coloring_type_1)
			SIMPLE_COLORING_TYPE_2 -> context.getString(R.string.hint_strategy_simple_coloring_type_2)
			TURBOT_SKYSCRAPER -> context.getString(R.string.hint_strategy_turbot_skyscraper)
			TURBOT_2_STRING_KITE -> context.getString(R.string.hint_strategy_turbot_2_string_kite)
			TURBOT_CRANE -> context.getString(R.string.hint_strategy_turbot_crane)
			EMPTY_RECTANGLE -> context.getString(R.string.hint_strategy_empty_rectangle)
			BASIC_FISH_3N_SWORDFISH -> context.getString(R.string.hint_strategy_fish_basic_3n)
			XY_WING -> context.getString(R.string.hint_strategy_xy_wing)
			XYZ_WING -> context.getString(R.string.hint_strategy_xyz_wing)
			X_CHAIN -> context.getString(R.string.hint_strategy_x_chain)
			X_CHAIN_LOOP -> context.getString(R.string.hint_strategy_x_chain_loop)
			X_CHAIN_ONE_ENDPOINT -> context.getString(R.string.hint_strategy_x_chain_one_endpoint)
			XY_CHAIN -> context.getString(R.string.hint_strategy_xy_chain)
			XY_CHAIN_LOOP -> context.getString(R.string.hint_strategy_xy_chain_loop)
			BUG1 -> context.getString(R.string.hint_strategy_bug1)
			UNIQUE_RECTANGLE_TYPE_1 -> context.getString(R.string.hint_strategy_unique_rectangle_type_1)
			UNIQUE_RECTANGLE_TYPE_1M -> context.getString(R.string.hint_strategy_unique_rectangle_type_1m)
			UNIQUE_RECTANGLE_TYPE_2 -> context.getString(R.string.hint_strategy_unique_rectangle_type_2)
			UNIQUE_RECTANGLE_TYPE_3 -> context.getString(R.string.hint_strategy_unique_rectangle_type_3)
			UNIQUE_RECTANGLE_TYPE_4 -> context.getString(R.string.hint_strategy_unique_rectangle_type_4)
			UNIQUE_RECTANGLE_TYPE_4M -> context.getString(R.string.hint_strategy_unique_rectangle_type_4m)
			UNIQUE_RECTANGLE_TYPE_5 -> context.getString(R.string.hint_strategy_unique_rectangle_type_5)
			UNIQUE_RECTANGLE_TYPE_5P -> context.getString(R.string.hint_strategy_unique_rectangle_type_5p)
			UNIQUE_RECTANGLE_TYPE_6 -> context.getString(R.string.hint_strategy_unique_rectangle_type_6)
			UNIQUE_RECTANGLE_TYPE_7 -> context.getString(R.string.hint_strategy_unique_rectangle_type_7)
			BASIC_FISH_4N_JELLYFISH -> context.getString(R.string.hint_strategy_fish_basic_4n)
			WXYZ_WING -> context.getString(R.string.hint_strategy_wxyz_wing)
			BASIC_FISH_5N_STARFISH -> context.getString(R.string.hint_strategy_fish_basic_5n)
			BASIC_FISH_6N_WHALE -> context.getString(R.string.hint_strategy_fish_basic_6n)
			BASIC_FISH_7N_LEVIATHAN -> context.getString(R.string.hint_strategy_fish_basic_7n)
			NO_NEXT_STEP_FOUND -> context.getString(R.string.hint_strategy_no_step_found)
		}
		return strategyName
	}

	fun getStrategyLevel() : StrategyLevelIds {
		val strategyLevel = when( this ) {
			// keep the same order as in definition
			// assign each strategy to a level
			NONE -> StrategyLevelIds.BASIC
			WRONG_VALUES -> StrategyLevelIds.BASIC
			MISSING_CANDIDATES -> StrategyLevelIds.BASIC
			OBSOLETE_CANDIDATES -> StrategyLevelIds.BASIC
			LAST_DIGIT -> StrategyLevelIds.BASIC
			NAKED_SINGLE -> StrategyLevelIds.BASIC
			HIDDEN_SINGLE -> StrategyLevelIds.BASIC
			NAKED_GROUP -> StrategyLevelIds.BASIC
			NAKED_PAIR -> StrategyLevelIds.BASIC
			LOCKED_GROUP -> StrategyLevelIds.BASIC
			LOCKED_PAIR -> StrategyLevelIds.BASIC
			HIDDEN_GROUP -> StrategyLevelIds.BASIC
			HIDDEN_PAIR -> StrategyLevelIds.BASIC
			POINTING_GROUP -> StrategyLevelIds.BASIC
			POINTING_PAIR -> StrategyLevelIds.BASIC
			POINTING_TRIPLE -> StrategyLevelIds.BASIC
			CLAIMING_GROUP -> StrategyLevelIds.BASIC
			CLAIMING_PAIR -> StrategyLevelIds.BASIC
			CLAIMING_TRIPLE -> StrategyLevelIds.BASIC
			NAKED_TRIPLE -> StrategyLevelIds.INTERMEDIATE
			LOCKED_TRIPLE -> StrategyLevelIds.INTERMEDIATE
			HIDDEN_TRIPLE-> StrategyLevelIds.INTERMEDIATE
			NAKED_QUAD -> StrategyLevelIds.INTERMEDIATE
			HIDDEN_QUAD -> StrategyLevelIds.INTERMEDIATE
			BASIC_FISH_GROUP -> StrategyLevelIds.INTERMEDIATE
			BASIC_FISH_2N_X_WING -> StrategyLevelIds.INTERMEDIATE
			REMOTE_PAIR -> StrategyLevelIds.ADVANCED
			CHUTE_REMOTE_PAIR -> StrategyLevelIds.ADVANCED
			CHUTE_REMOTE_PAIR_SINGLE -> StrategyLevelIds.ADVANCED
			CHUTE_REMOTE_PAIR_DOUBLE -> StrategyLevelIds.ADVANCED
			CHUTE_REMOTE_PAIR_BONUS -> StrategyLevelIds.ADVANCED
			SIMPLE_COLORING_TYPE_1 -> StrategyLevelIds.ADVANCED
			SIMPLE_COLORING_TYPE_2 -> StrategyLevelIds.ADVANCED
			TURBOT_SKYSCRAPER -> StrategyLevelIds.INTERMEDIATE
			TURBOT_2_STRING_KITE -> StrategyLevelIds.INTERMEDIATE
			TURBOT_CRANE -> StrategyLevelIds.INTERMEDIATE
			EMPTY_RECTANGLE -> StrategyLevelIds.ADVANCED
			BASIC_FISH_3N_SWORDFISH -> StrategyLevelIds.ADVANCED
			XY_WING -> StrategyLevelIds.INTERMEDIATE
			XYZ_WING -> StrategyLevelIds.INTERMEDIATE
			X_CHAIN -> StrategyLevelIds.ADVANCED
			X_CHAIN_LOOP -> StrategyLevelIds.ADVANCED
			X_CHAIN_ONE_ENDPOINT -> StrategyLevelIds.ADVANCED
			XY_CHAIN -> StrategyLevelIds.ADVANCED
			XY_CHAIN_LOOP -> StrategyLevelIds.ADVANCED
			BUG1 -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_1 -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_1M -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_2 -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_3 -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_4 -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_4M -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_5 -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_5P -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_6 -> StrategyLevelIds.MASTER
			UNIQUE_RECTANGLE_TYPE_7 -> StrategyLevelIds.MASTER
			BASIC_FISH_4N_JELLYFISH -> StrategyLevelIds.MASTER
			WXYZ_WING -> StrategyLevelIds.MASTER
			BASIC_FISH_5N_STARFISH -> StrategyLevelIds.MASTER
			BASIC_FISH_6N_WHALE -> StrategyLevelIds.MASTER
			BASIC_FISH_7N_LEVIATHAN -> StrategyLevelIds.MASTER
			NO_NEXT_STEP_FOUND -> StrategyLevelIds.MASTER
		}
		return strategyLevel
	}

	fun getStrategyValues() : Pair<Int,Int> {
		val ratingValues = when( this ) {
			//
			// keep the same order as in definition
			// define for each strategy:
			// 	- an average time in seconds for an easy step with minimal unsolved cells
			// 	- an average time in seconds for a hard step, with maximal unsolved cells
			// how to do:
			// - use a puzzle
			// - let the app fill in candidate
			// - use the hint function to get the info what strategy to use to for the next step
			// - note the time used to find the step
			//
			// groups are not really in use so they get 9999
			// the special strategy "NO_NEXT_STEP_FOUND" will also get 9999
			//
			// Pair( <nearly 81 cells solved> , <17 cells solved> )
			//
			NONE -> Pair(0,0)
			WRONG_VALUES-> Pair(0,0)
			MISSING_CANDIDATES -> Pair(0,0)
			OBSOLETE_CANDIDATES -> Pair(0,0)
			LAST_DIGIT -> Pair(2,4)      				//   2,   4
			NAKED_SINGLE -> Pair(2,8)         	     	//   2,   8
			HIDDEN_SINGLE -> Pair(10,25)            	//   10,  25
			//NAKED_GROUP
			NAKED_PAIR -> Pair(20,60)               	//   20,  60
			//LOCKED_GROUP
			LOCKED_PAIR -> Pair(22,50)              	//   22,  50
			//HIDDEN_GROUP
			HIDDEN_PAIR -> Pair(25,80)              	//   25,  80
			//POINTING_GROUP
			POINTING_PAIR -> Pair(20,60)            	//   20,  60
			POINTING_TRIPLE -> Pair(25,60)          	//   25,  60
			//CLAIMING_GROUP
			CLAIMING_PAIR -> Pair(20,60)            	//   20,  60
			CLAIMING_TRIPLE -> Pair(25,60)          	//   25,  60
			NAKED_TRIPLE -> Pair(40,100)            	//   40, 100
			LOCKED_TRIPLE -> Pair(35,70)            	//   35,  70
			HIDDEN_TRIPLE -> Pair(40,110)           	//   40, 110
			NAKED_QUAD -> Pair(40,130)					//   40, 130
			HIDDEN_QUAD -> Pair(60,150)					//   60, 150
			//BASIC_FISH_GROUP
			BASIC_FISH_2N_X_WING -> Pair(40,120)		//   40, 120
			REMOTE_PAIR -> Pair(190,480)				//  190, 480
			CHUTE_REMOTE_PAIR -> Pair(210,500)			//  210, 500
			CHUTE_REMOTE_PAIR_SINGLE -> Pair(220,500)	//  220, 500
			CHUTE_REMOTE_PAIR_DOUBLE -> Pair(220,500)	//  220, 500
			CHUTE_REMOTE_PAIR_BONUS -> Pair(220,500)	//  220, 500
			SIMPLE_COLORING_TYPE_1 -> Pair(280,510)		//  280, 510
			SIMPLE_COLORING_TYPE_2 -> Pair(280,510)		//  280, 510
			TURBOT_SKYSCRAPER -> Pair(120,300)			//  120, 300
			TURBOT_2_STRING_KITE -> Pair(170,350)		//  170, 350
			TURBOT_CRANE -> Pair(180,360)				//  180, 360
			EMPTY_RECTANGLE -> Pair(160,240)			//  160, 240
			BASIC_FISH_3N_SWORDFISH -> Pair(60,180)		//   60, 180
			XY_WING -> Pair(200,440)					//  200, 440
			XYZ_WING -> Pair(220,480)					//  220, 480
			X_CHAIN -> Pair(215,400)					//  215, 400
			X_CHAIN_LOOP -> Pair(250,400)				//  250, 400
			X_CHAIN_ONE_ENDPOINT -> Pair(280,400)		//  280, 400
			XY_CHAIN -> Pair(300,530)					//  300, 530
			XY_CHAIN_LOOP -> Pair(330,570)				//  330, 570
			BUG1 -> Pair(310,480)						//  310, 480
			UNIQUE_RECTANGLE_TYPE_1 -> Pair(350,510)	//  350, 510
			UNIQUE_RECTANGLE_TYPE_1M -> Pair(380,540)	//  380, 540
			UNIQUE_RECTANGLE_TYPE_2 -> Pair(350,510)	//  350, 510
			UNIQUE_RECTANGLE_TYPE_3 -> Pair(350,510)	//  350, 510
			UNIQUE_RECTANGLE_TYPE_4 -> Pair(350,510)	//  350, 510
			UNIQUE_RECTANGLE_TYPE_4M -> Pair(380,540)	//  380, 540
			UNIQUE_RECTANGLE_TYPE_5 -> Pair(350,510)	//  350, 510
			UNIQUE_RECTANGLE_TYPE_5P -> Pair(350,510)	//  350, 510
			UNIQUE_RECTANGLE_TYPE_6 -> Pair(350,510)	//  350, 510
			UNIQUE_RECTANGLE_TYPE_7 -> Pair(350,510)	//  350, 510
			BASIC_FISH_4N_JELLYFISH-> Pair(80,240)		//   80, 240
			WXYZ_WING -> Pair(260,520)					//  260, 520
			BASIC_FISH_5N_STARFISH-> Pair(100,300)		//  100, 300
			BASIC_FISH_6N_WHALE-> Pair(120,360)			//  120, 360
			BASIC_FISH_7N_LEVIATHAN-> Pair(140,4000) 	//  140, 400
			NO_NEXT_STEP_FOUND -> Pair(9999,9999)
			else -> Pair(9999,9999)
		}
		return ratingValues
	}

	fun getRatingValue(solvedValueCount: Int) : Double {
		val ratingValues = getStrategyValues()
		// compute limit
		val maxValueCount = SudokuBoard.SUDOKU_SIZE * SudokuBoard.SUDOKU_SIZE
		val maxUnsolvedCount = maxValueCount - 17 // minimum for a valid puzzle
		// compute rating value
		val unsolvedCount = maxValueCount - solvedValueCount
		return if (unsolvedCount <= 1)
			ratingValues.first.toDouble()
		else
			ratingValues.first.toDouble() +
				((ratingValues.second - ratingValues.first).toDouble() *
					unsolvedCount / maxUnsolvedCount)
	}

	companion object{

		fun getStrategyNameList(context: Context): List<String> {
			// List of strategies sorted by usage in code
			val strategyNameList = mutableListOf<String>()
			for (s in StrategyIds.entries) {
				if ( s.isStrategy() ) {
					strategyNameList.add( "➠ " + s.getStrategyName(context) )
				}
			}
			return strategyNameList
		}

		fun getStrategyNameListGroupByLevel(context: Context): List<String> {
			// List of strategies grouped by level
			val itemList = mutableListOf<String>()
			for (s in StrategyLevelIds.entries) {
				if (s == StrategyLevelIds.NONE) continue
				val k = s.ordinal * 1000
				itemList.add( "%04d ".format(k) + "\n" + s.getStrategyLevelName(context) + "\n")
			}
			for (s in StrategyIds.entries) {
				if ( s.isStrategy() ) {
					val k = s.getStrategyLevel().ordinal * 1000 + s.ordinal
					itemList.add( "%04d ".format(k) + " ➠ " + s.getStrategyName(context))
				}
			}
			val itemListSorted = itemList.sorted()
			val itemListFormated = itemListSorted.map { it.substring(5) }
			return itemListFormated
		}

		fun getStrategyNameListWithRatingValues(context: Context): List<String> {
			// List of strategies sorted by rating and name
			val lstRatingName = mutableListOf<String>()
			for (s in StrategyIds.entries) {
				if ( s.isStrategy() ) {
					val strategyValues = s.getStrategyValues()
					lstRatingName.add( "%04d".format(strategyValues.first)
						+ "-"
						+ "%04d".format(strategyValues.second)
						+ " "
						+ s.getStrategyName(context))
				}
			}
			val lstRatingNameSorted = lstRatingName.sorted()
			val lstName = lstRatingNameSorted.map { it.substring(5) }
			return lstName
		}

	}
}



