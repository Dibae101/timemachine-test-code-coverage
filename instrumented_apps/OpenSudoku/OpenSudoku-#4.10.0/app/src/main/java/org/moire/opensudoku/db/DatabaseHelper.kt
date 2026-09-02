/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2026 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("HardCodedStringLiteral")

package org.moire.opensudoku.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.util.Log
import org.moire.opensudoku.BuildConfig
import org.moire.opensudoku.R
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import java.time.Instant

/**
 * This class helps open, create, and upgrade the database file.
 */
class DatabaseHelper private constructor(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
	companion object {
		@Volatile
		private var instance: DatabaseHelper? = null

		fun getInstance(context: Context): DatabaseHelper {
			return instance ?: synchronized(this) {
				instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
			}
		}
	}

	override fun onConfigure(db: SQLiteDatabase) {
		super.onConfigure(db)
		db.enableWriteAheadLogging()
	}

	override fun onOpen(db: SQLiteDatabase?) {
		super.onOpen(db)
		if (db == null || FoldersColumn.indexesUpdated) return

		db.query(PUZZLES_TABLE_NAME, null, null, null, null, null, null)?.use { cursor ->
			PuzzlesColumn.entries.forEach { column ->
				column.cid = cursor.getColumnIndex(column.nme)
			}
		}

		db.query(FOLDERS_TABLE_NAME, null, null, null, null, null, null).use { cursor ->
			FoldersColumn.entries.forEach { column ->
				column.cid = cursor.getColumnIndex(column.nme)
			}
		}
		FoldersColumn.indexesUpdated = true
	}

	override fun onCreate(db: SQLiteDatabase) {
		createPuzzlesTable(db)
		db.execSQL(
			"CREATE TABLE $FOLDERS_TABLE_NAME (" +
				"${FoldersColumn.ID} INTEGER PRIMARY KEY," +
				"${FoldersColumn.CREATED} INTEGER," +
				"${FoldersColumn.NAME} TEXT UNIQUE NOT NULL);"
		)
		insertFolder(db, 1, context.getString(R.string.difficulty_easy))
		insertInitialPuzzle(db, 1, 1, "052006000160900004049803620400000800083201590001000002097305240200009056000100970")
		insertInitialPuzzle(db, 1, 2, "052400100100002030000813025400007010683000597070500002890365000010700006006004970")
		insertInitialPuzzle(db, 1, 3, "302000089068052734009000000400007000083201590000500002000000200214780350530000908")
		insertInitialPuzzle(db, 1, 4, "402000007000080420050302006090030050503060708070010060900406030015070000200000809")
		insertInitialPuzzle(db, 1, 5, "060091080109680405050040106600000200023904710004000003907020030305079602040150070")
		insertInitialPuzzle(db, 1, 6, "060090380009080405050300106001008200020904010004200900907006030305070600046050070")
		insertInitialPuzzle(db, 1, 7, "402000380109607400008300106090030004023964710800010060907006500005809602046000809")
		insertInitialPuzzle(db, 1, 8, "400091000009007425058340190691000000003964700000000963087026530315800600000150009")
		insertInitialPuzzle(db, 1, 9, "380001004002600070000487003000040239201000406495060000600854000070006800800700092")
		insertInitialPuzzle(db, 1, 10, "007520060002009008006407000768005009031000450400300781000804300100200800050013600")
		insertInitialPuzzle(db, 1, 11, "380000000540009078000407503000145209000908000405362000609804000170200045000000092")
		insertInitialPuzzle(db, 1, 12, "007001000540609078900487000760100230230000056095002081000854007170206045000700600")
		insertInitialPuzzle(db, 1, 13, "007021900502009078006407500000140039031908450490062000009804300170200805004710600")
		insertInitialPuzzle(db, 1, 14, "086500204407008090350009000009080601010000080608090300000200076030800409105004820")
		insertInitialPuzzle(db, 1, 15, "086507000007360100000000068249003050500000007070100342890000000002056400000904820")
		insertInitialPuzzle(db, 1, 16, "000007230420368000050029768000080650000602000078090000894230070000856019065900000")
		insertInitialPuzzle(db, 1, 17, "906000200400368190350400000209080051013040980670090302000001076032856009005000803")
		insertInitialPuzzle(db, 1, 18, "095002000700804001810076500476000302000000000301000857003290075500307006000400130")
		insertInitialPuzzle(db, 1, 19, "005002740002850901810000500070501302008723600301609050003000075509017200087400100")
		insertInitialPuzzle(db, 1, 20, "605102740732004001000000020400501300008020600001609007060000000500300286087405109")
		insertInitialPuzzle(db, 1, 21, "695102040700800000000970023076000090900020004020000850160098000000007006080405139")
		insertInitialPuzzle(db, 1, 22, "090002748000004901800906500470500090008000600020009057003208005509300000287400030")
		insertInitialPuzzle(db, 1, 23, "001009048089070030003106005390000500058602170007000094900708300030040860870300400")
		insertInitialPuzzle(db, 1, 24, "600039708000004600000100025002017506408000103107850200910008000005900000806320009")
		insertInitialPuzzle(db, 1, 25, "620500700500270631040100005302000086000090000160000204900008050235041007006005019")
		insertInitialPuzzle(db, 1, 26, "080130002140902007273080000000070206007203900502040000000060318600308024400021050")
		insertInitialPuzzle(db, 1, 27, "980100402046950000200684001010009086007000900590800070700465008000098720408001059")
		insertInitialPuzzle(db, 1, 28, "085100400000950007073684001010070080067203940090040070700465310600098000008001650")
		insertInitialPuzzle(db, 1, 29, "085100460146000807070004001300009080067000940090800003700400010601000724038001650")
		insertInitialPuzzle(db, 1, 30, "085130462006000007270680090000009200060213040002800000020065018600000700438021650")
		insertFolder(db, 2, context.getString(R.string.difficulty_medium))
		insertInitialPuzzle(db, 2, 31, "916004072800620050500008930060000200000207000005000090097800003080076009450100687")
		insertInitialPuzzle(db, 2, 32, "000900082063001409908000000000670300046050290007023000000000701704300620630007000")
		insertInitialPuzzle(db, 2, 33, "035670000400829500080003060020005807800206005301700020040900070002487006000052490")
		insertInitialPuzzle(db, 2, 34, "030070902470009000009003060024000837007000100351000620040900200000400056708050090")
		insertInitialPuzzle(db, 2, 35, "084200000930840000057000000600401700400070002005602009000000980000028047000003210")
		insertInitialPuzzle(db, 2, 36, "007861000008003000560090010100070085000345000630010007050020098000600500000537100")
		insertInitialPuzzle(db, 2, 37, "040001003000050079560002804100270080082000960030018007306100098470080000800500040")
		insertInitialPuzzle(db, 2, 38, "000500006000870302270300081000034900793050614008790000920003057506087000300005000")
		insertInitialPuzzle(db, 2, 39, "000900067090000208460078000320094070700603002010780043000850016501000090670009000")
		insertInitialPuzzle(db, 2, 40, "024000017000301000300000965201000650000637000093000708539000001000502000840000570")
		insertInitialPuzzle(db, 2, 41, "200006143004000600607008029100800200003090800005003001830500902006000400942600005")
		insertInitialPuzzle(db, 2, 42, "504002030900073008670000020000030780005709200047060000050000014100450009060300502")
		insertInitialPuzzle(db, 2, 43, "580000637000000000603540000090104705010709040807205090000026304000000000468000072")
		insertInitialPuzzle(db, 2, 44, "000010000900003408670500021000130780015000240047065000750006014102400009000090000")
		insertInitialPuzzle(db, 2, 45, "780300050956000000002065001003400570600000003025008100200590800000000417030004025")
		insertInitialPuzzle(db, 2, 46, "200367500500800060300450700090530400080000070003074050001026005030005007002783001")
		insertInitialPuzzle(db, 2, 47, "801056200000002381900003000350470000008000100000068037000600002687100000004530806")
		insertInitialPuzzle(db, 2, 48, "300004005841753060000010000003000087098107540750000100000070000030281796200300008")
		insertInitialPuzzle(db, 2, 49, "000064810040050062009010300003040607008107500704030100006070200430080090017390000")
		insertInitialPuzzle(db, 2, 50, "000040320000357080000600400357006040600705003080900675008009000090581000064070000")
		insertInitialPuzzle(db, 2, 51, "905040026026050900030600050350000009009020800100000075010009030003080760560070108")
		insertInitialPuzzle(db, 2, 52, "010403060030017400200000300070080004092354780500070030003000005008530040050906020")
		insertInitialPuzzle(db, 2, 53, "605900100000100073071300005009010004046293510700040600200001730160002000008009401")
		insertInitialPuzzle(db, 2, 54, "049060002800210490100040000000035084008102300630470000000080001084051006700020950")
		insertInitialPuzzle(db, 2, 55, "067020300003700000920103000402035060300000002010240903000508039000009200008010750")
		insertInitialPuzzle(db, 2, 56, "050842001004000900800050040600400019007506800430009002080090006001000400500681090")
		insertInitialPuzzle(db, 2, 57, "000076189000002030009813000025000010083000590070000460000365200010700000536120000")
		insertInitialPuzzle(db, 2, 58, "080000030400368000350409700000003650003000900078100000004201076000856009060000020")
		insertInitialPuzzle(db, 2, 59, "000500748589000001700086900302010580000000000067050204004760002200000867876005000")
		insertInitialPuzzle(db, 2, 60, "021009008000004031740100025000007086058000170160800000910008052230900000800300410")
		insertFolder(db, 3, context.getString(R.string.difficulty_hard))
		insertInitialPuzzle(db, 3, 61, "600300100071620000805001000500870901009000600407069008000200807000086410008003002")
		insertInitialPuzzle(db, 3, 62, "906013008058000090030000010060800920003409100049006030090000080010000670400960301")
		insertInitialPuzzle(db, 3, 63, "300060250000500103005210486000380500030000040002045000413052700807004000056070004")
		insertInitialPuzzle(db, 3, 64, "060001907100007230080000406018002004070040090900100780607000040051600009809300020")
		insertInitialPuzzle(db, 3, 65, "600300208400185000000000450000070835030508020958010000069000000000631002304009006")
		insertInitialPuzzle(db, 3, 66, "400030090200001600760800001500318000032000510000592008900003045001700006040020003")
		insertInitialPuzzle(db, 3, 67, "004090170900070002007204000043000050798406213060000890000709400600040001085030700")
		insertInitialPuzzle(db, 3, 68, "680001003007004000000820000870009204040302080203400096000036000000500400700200065")
		insertInitialPuzzle(db, 3, 69, "000002000103400005200050401340005090807000304090300017605030009400008702000100000")
		insertInitialPuzzle(db, 3, 70, "050702003073480005000050400040000200027090350006000010005030000400068730700109060")
		insertInitialPuzzle(db, 3, 71, "500080020007502801002900040024000308000324000306000470090006700703208900060090005")
		insertInitialPuzzle(db, 3, 72, "108090000200308096090000400406009030010205060080600201001000040360904007000060305")
		insertInitialPuzzle(db, 3, 73, "010008570607050009052170000001003706070000040803700900000017260100020407024300090")
		insertInitialPuzzle(db, 3, 74, "020439800080000001003001520050092703000000000309740080071300900600000030008924010")
		insertInitialPuzzle(db, 3, 75, "000500201800006005005207080017960804000000000908074610080405300700600009504009000")
		insertInitialPuzzle(db, 3, 76, "920000000500870000038091000052930160090000030073064980000410250000053001000000073")
		insertInitialPuzzle(db, 3, 77, "590006010001254709000001400003715008100000004200648100002500000708463900050100047")
		insertInitialPuzzle(db, 3, 78, "309870004000005008870400000104580003000706000700034105000009081900300000400057206")
		insertInitialPuzzle(db, 3, 79, "800200000910300706000007002084000009095104860100000230500600000609003071000005008")
		insertInitialPuzzle(db, 3, 80, "005037001000050627600002530020070000001968200000010090013700008486090000700840100")
		insertInitialPuzzle(db, 3, 81, "090350700000800029000402008710000000463508297000000051300204000940005000008037040")
		insertInitialPuzzle(db, 3, 82, "000005904080090605006000030030701450008040700074206090060000300801060070309800000")
		insertInitialPuzzle(db, 3, 83, "030004087948700500060800009010586720000000000087312050800003070003007865570200090")
		insertInitialPuzzle(db, 3, 84, "300687015000030082050000300400300000601050709000004003008000020210040000970521004")
		insertInitialPuzzle(db, 3, 85, "702000004030702010400093008000827090007030800080956000300570009020309080600000503")
		insertInitialPuzzle(db, 3, 86, "300040057400853060025700000000000430800406001034000000000005690090624003160080002")
		insertInitialPuzzle(db, 3, 87, "000260050000005900000380046020094018004000500950810070380021000005700000040058000")
		insertInitialPuzzle(db, 3, 88, "062080504008050090700320001000740620000203000027065000200036007040070100803090240")
		insertInitialPuzzle(db, 3, 89, "002001000068000003000086090900002086804000102520800009080140000100000920000700500")
		insertInitialPuzzle(db, 3, 90, "000030065460950200000086004003070006004090100500010300200140000007065028630020000")
		createIndexes(db)
	}

	private fun createPuzzlesTable(db: SQLiteDatabase) {
		// table schema for current database version
		db.execSQL(
			"CREATE TABLE $PUZZLES_TABLE_NAME (" +
				"${PuzzlesColumn.ID} INTEGER PRIMARY KEY," +				// 0
				"${PuzzlesColumn.FOLDER_ID} INTEGER," +						// 1
				"${PuzzlesColumn.CREATED} INTEGER," +						// 2
				"${PuzzlesColumn.STATE} INTEGER," +							// 3
				"${PuzzlesColumn.TIME} INTEGER," +							// 4
				"${PuzzlesColumn.MISTAKE_COUNTER} INTEGER DEFAULT NULL," +	// 5
				"${PuzzlesColumn.HINT_USAGE} INTEGER DEFAULT NULL," +		// 6
				"${PuzzlesColumn.LAST_PLAYED} INTEGER," +					// 7
				"${PuzzlesColumn.ORIGINAL_VALUES} Text NOT NULL," +			// 8
				"${PuzzlesColumn.CELLS_DATA} Text," +						// 9
				"${PuzzlesColumn.COMMAND_STACK} Text," +					// 10
				"${PuzzlesColumn.USER_NOTE} Text," +						// 11
				"${PuzzlesColumn.RATING_LEVEL} INTEGER DEFAULT 0," +		// 12
				"${PuzzlesColumn.RATING_VALUE}  INTEGER DEFAULT 0," +		// 13
				"${PuzzlesColumn.SUID} TEXT," +								// 14
				"${PuzzlesColumn.SOLUTION} TEXT);"							// 15
		)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (BuildConfig.DEBUG) Log.i(javaClass.simpleName, "Upgrading database from version $oldVersion to $newVersion")

		if (oldVersion < 8) {
			db.execSQL("create index " + PuzzlesColumn.FOLDER_ID + "_idx on sudoku (" + PuzzlesColumn.FOLDER_ID + ");")
		}

		if (oldVersion < 9) {
			db.execSQL("ALTER TABLE sudoku ADD COLUMN ${PuzzlesColumn.COMMAND_STACK} TEXT;")
		}

		if (oldVersion < 11) {
			db.execSQL("ALTER TABLE folder RENAME TO $FOLDERS_TABLE_NAME; ")
			convertPuzzlesTableToLatest(db)
		} else {
			if (oldVersion < 12) {
				db.execSQL("ALTER TABLE $PUZZLES_TABLE_NAME ADD COLUMN ${PuzzlesColumn.MISTAKE_COUNTER} INTEGER DEFAULT NULL;")
			}
			if (oldVersion < 13) {
				db.execSQL("ALTER TABLE $PUZZLES_TABLE_NAME ADD COLUMN hint_counter INTEGER DEFAULT NULL;")
			}
			if (oldVersion < 14) {
				doChangesForV14(db)
			}
			if (oldVersion < 15) {
				doChangesForV15(db)
			}
			if (oldVersion < 16) {
				doChangesForV16(db)
			}
			if (oldVersion < 17) {
				doChangesForV17(db)
			}
		}
	}

	private fun convertPuzzlesTableToLatest(db: SQLiteDatabase) {
		createPuzzlesTable(db)

		val oldColumns = arrayOf(
			PuzzlesColumn.ID.nme,           	// 0
			PuzzlesColumn.FOLDER_ID.nme,    	// 1
			PuzzlesColumn.CREATED.nme,      	// 2
			PuzzlesColumn.STATE.nme,        	// 3
			PuzzlesColumn.TIME.nme,         	// 4
			PuzzlesColumn.LAST_PLAYED.nme,  	// 5
			"data",               				// 6
			PuzzlesColumn.COMMAND_STACK.nme, 	// 7
			"puzzle_note"         				// 8
		)

		val qb = SQLiteQueryBuilder()
		qb.tables = "sudoku"
		qb.query(db, oldColumns, null, null, null, null, null).forEach { cursor ->
			db.insert(PUZZLES_TABLE_NAME, null, ContentValues().apply {
				put(PuzzlesColumn.ID.nme, cursor.getLong(0))
				put(PuzzlesColumn.FOLDER_ID.nme, cursor.getLong(1))
				put(PuzzlesColumn.CREATED.nme, cursor.getLong(2))
				put(PuzzlesColumn.STATE.nme, cursor.getInt(3))
				put(PuzzlesColumn.TIME.nme, cursor.getLong(4))
				put(PuzzlesColumn.LAST_PLAYED.nme, cursor.getLong(5))
				val cellsData = cursor.getString(6)
				put(PuzzlesColumn.CELLS_DATA.nme, cellsData)
				put(PuzzlesColumn.COMMAND_STACK.nme, cursor.getString(7))
				put(PuzzlesColumn.USER_NOTE.nme, cursor.getString(8))
				if (cellsData.length == 81) {
					put(PuzzlesColumn.ORIGINAL_VALUES.nme, cellsData)
				} else {
					val (board, _) = SudokuBoard.deserialize(cellsData, false)
					put(PuzzlesColumn.ORIGINAL_VALUES.nme, board.originalValues)
				}
			})
		}
		createOriginalValuesIndex(db)
		db.execSQL("DROP TABLE sudoku;")
	}

	private fun insertFolder(db: SQLiteDatabase, folderId: Long, folderName: String) {
		val now = Instant.now().toEpochMilli()
		db.execSQL("INSERT INTO $FOLDERS_TABLE_NAME VALUES ($folderId, $now, '$folderName');")
	}

	private fun insertInitialPuzzle(db: SQLiteDatabase, folderId: Long, puzzleID: Long, originalValues: String) {
		db.insert(PUZZLES_TABLE_NAME, null, ContentValues().apply {
			put(PuzzlesColumn.ID.nme, puzzleID)
			put(PuzzlesColumn.FOLDER_ID.nme, folderId)
			put(PuzzlesColumn.CREATED.nme, 0)
			put(PuzzlesColumn.STATE.nme, SudokuGame.GAME_STATE_NOT_STARTED)
			put(PuzzlesColumn.MISTAKE_COUNTER.nme, 0)
			put(PuzzlesColumn.HINT_USAGE.nme, 0)
			put(PuzzlesColumn.TIME.nme, 0)
			put(PuzzlesColumn.ORIGINAL_VALUES.nme, originalValues)
			put(PuzzlesColumn.USER_NOTE.nme, "")
			put(PuzzlesColumn.COMMAND_STACK.nme, "")
			put(PuzzlesColumn.RATING_LEVEL.nme, 0)
			put(PuzzlesColumn.RATING_VALUE.nme, 0)
		})
	}

	private fun createIndexes(db: SQLiteDatabase) {
		createFolderIDIndex(db)
		createOriginalValuesIndex(db)
	}

	private fun createFolderIDIndex(db: SQLiteDatabase) {
		db.execSQL("create index " + PuzzlesColumn.FOLDER_ID.nme + "_idx on " + PUZZLES_TABLE_NAME + " (" + PuzzlesColumn.FOLDER_ID.nme + ");")
	}

	private fun createOriginalValuesIndex(db: SQLiteDatabase) {
		db.execSQL("create index " + PuzzlesColumn.ORIGINAL_VALUES.nme + "_idx on " + PUZZLES_TABLE_NAME + " (" + PuzzlesColumn.ORIGINAL_VALUES.nme + ");")
	}

	/** do changes in DB for V14
	 * rename column "hint_counter" to "hint_usage" in table "puzzles"
	 * 	-> the function "rename column" is not available in sqlite for android 9!
	 * 	   Use a workaround for this for all android versions :-(
	 */
	private fun doChangesForV14(db: SQLiteDatabase) {
		val puzzlesTableName = "puzzles"
		val puzzlesTableNameTemp = "puzzlestemp"
		// db.execSQL("ALTER TABLE $PUZZLES_TABLE_NAME RENAME COLUMN hint_counter TO ${PuzzlesColumn.HINT_USAGE};")
		// rename table to TEMP
		db.execSQL("ALTER TABLE $puzzlesTableName RENAME TO $puzzlesTableNameTemp;")
		// create NEW table for DB version 12
		db.execSQL(
			"CREATE TABLE puzzles (" +
				"_id INTEGER PRIMARY KEY," +				// 0
				"folder_id INTEGER," +						// 1
				"created INTEGER," +						// 2
				"state INTEGER," +							// 3
				"time INTEGER," +							// 4
				"mistake_counter INTEGER DEFAULT NULL," +	// 5
				"hint_usage INTEGER DEFAULT NULL," +		// 6
				"last_played INTEGER," +					// 7
				"original_values Text NOT NULL," +			// 8
				"cells_data Text," +						// 9
				"command_stack Text," +						// 10
				"user_note Text);"							// 11
		)
		// copy from TEMP to NEW
		db.execSQL("INSERT INTO $puzzlesTableName SELECT" +
			" _id, folder_id, created, state, time, mistake_counter," +
			" hint_counter AS hint_usage," +
			" last_played, original_values, cells_data, command_stack, user_note" +
			" FROM $puzzlesTableNameTemp;")
		// delete TEMP table
		db.execSQL("DROP TABLE $puzzlesTableNameTemp;")
	}

	/** do changes in DB for V15
	 * add new columns to table "puzzles"
	 *  - rating_level
	 *  - rating_value
	 */
	private fun doChangesForV15(db: SQLiteDatabase) {
		db.execSQL("ALTER TABLE puzzles ADD COLUMN rating_level INTEGER DEFAULT 0;")
		db.execSQL("ALTER TABLE puzzles ADD COLUMN rating_value INTEGER DEFAULT 0;")
	}

	/** do changes in DB for V16: add new column to table "puzzles" - suid
	 */
	private fun doChangesForV16(db: SQLiteDatabase) {
		db.execSQL("ALTER TABLE puzzles ADD COLUMN ${PuzzlesColumn.SUID} TEXT;")
	}

	/** do changes in DB for V17: add new column to table "puzzles"  - solution
	 */
	private fun doChangesForV17(db: SQLiteDatabase) {
		db.execSQL("ALTER TABLE puzzles ADD COLUMN ${PuzzlesColumn.SOLUTION} TEXT;")
	}

}
