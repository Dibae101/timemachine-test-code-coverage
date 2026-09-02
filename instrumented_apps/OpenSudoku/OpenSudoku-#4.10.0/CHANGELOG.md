**4.10**
- Add 'Validate pencil marks' option to highlight clearly invalid marks \[Bart Uliasz\]
- Fix landscape pop-up keypad visibility and button sizing \[Bart Uliasz\]
- Refactor SudokuBoardView measurement and update landscape layout \[Bart Uliasz\]
- Optimize BaseThemeColor and 'Modify Cell Coloring Colors' dialogs for landscape mode \[Bart Uliasz\]
- Improve theme selection UI and primary color preview in custom themes \[Bart Uliasz\]
- Performance optimizations: caching game stats, batch updates for commands, and guarding UI redraws \[Bart Uliasz\]
- Fix ANR during batch undo operations \[Bart Uliasz\]
- Fix UI clipping and centering issues across multiple screens \[Bart Uliasz\]
- Improve edge-to-edge compliance and fix UI overlaps \[Bart Uliasz\]
- Translations updates \[Bart Uliasz, Michalis, hamburger2048\]

**4.9**
- Experimental Puzzle Scanning feature improvements (portrait scanning support, new 'Edit' button, OCR engine migrated to FOSS-compatible TensorFlow Lite) \[Bart Uliasz\]
- New 'with mistakes' and 'with hints' filter options \[Bart Uliasz\]
- Improved primary color selection for custom themes \[Bart Uliasz\]
- Full edge-to-edge support for all major screens \[Bart Uliasz\]
- Updated targetSdk to 37 \[Bart Uliasz\]
- Translations updates \[Bart Uliasz, Andreas Pettersson, hamburger2048\]

**4.8**
- New 'Puzzle info' option in game screen menu \[Bart Uliasz\]
- New 'Experimental Functions' option in Settings \[Bart Uliasz\]
- New puzzle scanning using camera experimental feature \[Bart Uliasz, Nik Clayton\]
- New cell coloring experimental feature \[Andras Porjesz\]
- New cell value overwrite confirmation \[Bart Uliasz\]
- New 'Apply' button for hint function \[Rainer Popella\]
- Added random puzzle folder info toast \[Bart Uliasz\]
- Performance improvements by caching SUID and solutions \[Bart Uliasz\]
- Fixed database stability issues \[Bart Uliasz\]
- Bugfixes and stability improvements \[Bart Uliasz, Rainer Popella\]
- Translations updates \[Bart Uliasz, hamburger2048, Luan M. D. Lima, Rainer Popella\]

**4.7**
- New Puzzle Rating feature \[Rainer Popella\]
- New feature to select fonts for digits \[Andras Porjesz\]
- New translation to Greek! \[Michalis\]
- Bugfixes/improvements \[Bart Uliasz\]
- Translations updates \[大王叫我来巡山, Andreas Pettersson, Bart Uliasz, Joan Montané, Michalis, ssantos, Ștefan Zaharia\]

**4.6**
- New folders sorting feature \[Rainer Popella\]
- Bugfixes/improvements \[Bart Uliasz\]
- Translations updates \[大王叫我来巡山, தமிழ்நேரம், Andreas Pettersson, Bart Uliasz, Manuel Tassi, Óscar García Amor, Rainer Popella\]

**4.5**

- Puzzle difficulty rating function introduced \[Rainer Popella\]
- Themes functionality reimplemented \[Bart Uliasz\]
- Reworked pencil marks look and behaviour \[Bart Uliasz\]
- New "Hint" strategies (Simple Coloring Type 1 and 2, XY-Chain, X-Chain) \[Rainer Popella\]
- Existing "Hint" strategies improvements \[Rainer Popella\]
- Hint usage added to puzzle stats \[Bart Uliasz, Rainer Popella\]
- Added option to choose filters when random puzzle button is clicked \[Bart Uliasz\]
- Visual keyboard improvements and fixes \[Bart Uliasz\]
- Lots of bugfixes and improvements \[Bart Uliasz, Rainer Popella\]
- New translation to Arabic! \[E2EEAES256\]
- New translation to Galician! \[José M.\]
- Translations updates \[AndyRn.t.me, Bart Uliasz, Claudio Strizzolo, Ettore Atalan, Joonas Reinholm, Lula Bye, Manuel Tassi, Nico, poesty, Rainer Popella, Stankendesokies, Yurt Page, zatrit\]

**4.4**

- New SUID (Sudoku Unique ID) to identify any valid puzzle \[Bart Uliasz\]
- New "Hint" strategies (Remote Pair, Chute Remote Pair, BUG: Bi-value Universal Grave, Unique Rectangles) \[Rainer Popella\]
- New "Play random unsolved puzzle" button if there's no resume \[ReisMiner\]
- New title screen menu option to save Logs \[Bart Uliasz\]
- Unsolvable puzzle alert \[Bart Uliasz\]
- Numerous bugfixes and improvements \[Bart Uliasz, ILikeYourHat\]
- UI updates \[Bart Uliasz\]
- New translation to Romanian! \[Dragos Miron\]
- Translations updates \[AndyRn.t.me, தமிழ்நேரம் - anishprabu.t, Bart Uliasz, Dream X, Ettore Atalan, Erik Dorner, Joonas Reinholm, Libor Rejč, Lula Bye, Maksim_220 Кабанов, Manuel Tassi, michte, Óscar García Amor, Rainer Popella, Yurt Page\]

**4.3**

- New "Hint" strategies implemented (WXYZ-Wing, Fish basic, Skyscraper, 2-string Kite, Turbot - Crane, Empty Rectangle, Remote Pair). \[Rainer Popella\]
- New Xmas Challenge (available only in December). \[Bart Uliasz\]
- Keyboard UI updated. \[Bart Uliasz\]
- Multiple bugfixes (landscape mode, puzzle editor, import/export, and many others). \[Bart Uliasz, Rainer Popella\]
- Speedup folder list with big sets of puzzles. \[Bart Uliasz\]
- UI and application responsiveness improvements. \[Bart Uliasz\]
- Updated "About" and "What's new" dialogs. \[Bart Uliasz\]
- New translation to Finnish! \[Joonas Reinholm\]
- New translation to Tamil! \[தமிழ்நேரம் - anishprabu.t\]
- Translations updates. \[Bart Uliasz, Ettore Atalan, Jens Persson, Óscar García Amor, Rainer Popella\]

**4.2**

- New feature: "Hint" a.k.a "Next step". \[Rainer Popella\]
- Bugfixes and small improvements. \[Bart Uliasz\]
- Translations updates.
- New translation to Chinese Traditional Han script. \[anenasa\]

**4.1**

- New feature: Sudoku import strategies. \[Rainer Popella\]
- Option to disable completed number blinking in Settings. \[Bart Uliasz\]
- Added Settings option to disable mistake counter. \[Bart Uliasz\]
- Bugfixes (cells highlighting). \[Bart Uliasz\]
- Translations updates.

**4.0**

- Code rewritten in Kotlin programming language. \[Bart Uliasz\]
- Added double pencil marks support (used Nik Clayton's draft as a base). \[Bart Uliasz\]
- Added mistake counter to count incorrect value inserts. \[Bart Uliasz\]
- Added option to highlight value if entry doesn't match final solution value. \[Bart Uliasz\]
- Added verification if the puzzle is invalid (has more than one solution). \[Bart Uliasz\]
- Added single puzzle export functionality. \[Bart Uliasz\]
- Added verification if the imported puzzle(s) already exists (prevent duplicates). \[Bart Uliasz\]
- New copy to clipboard function from puzzle list. \[Rainer Popella\]
- Added sound and blink when all of a digit are entered. \[Bart Uliasz\]
- Added sound notification when wrong value is entered. \[Bart Uliasz\]
- Added support for Sudoku Exchange "Puzzle Bank" format import. \[Bart Uliasz\]
- Weblate configured for translations. \[Óscar García Amor\]
- Added Polish translation. \[Bart Uliasz\]
- Updated Spanish translation. \[Óscar García Amor\]
- Updated German translation. \[Rainer Popella\]
- Automatically close current puzzle after it is solved. \[Bart Uliasz\]
- Import/export speed-up and fixes. Supports big amounts now. \[Bart Uliasz\]
- Prevent creating duplicate puzzles on import. \[Bart Uliasz\]
- Fixed undo button and functionality. \[Bart Uliasz\]
- Added two more "Get more puzzles online" links. \[Bart Uliasz\]
- Modified keyboard behavior. \[Bart Uliasz\]
- Fixed numerous existing code bugs. \[Bart Uliasz\]
- Changed some visual effects and icons. \[Bart Uliasz\]
- Improved functionality and performance of many features in the game. \[Bart Uliasz\]
- Replaced deprecated libraries/calls usage with an up to date code and libraries. \[Bart Uliasz\]
- A lot of "under the hood" refactors and cleanups for code maintainability and performance. \[Bart Uliasz\]

**3.9.2**

- All user interface changes introduced in version 3.9.0 have been reverted, they need to go back to the oven.

**3.9.1**

- Fixed a problem with the dark theme that was using light colors as a base instead of dark colors.

**3.9.0**

- UI updated to use Material Design. \[Nik Clayton\]
- The buttons now have BIGGER numbers. \[Nik Clayton\]
- Now a frame is drawn around the selected cell making it easier to know which cell it is. \[Nik Clayton\]
- Italian translation updated. \[fabpolli\]

**3.8.1**

- The keyboard layout has been modified and the numbers are now larger.
- Added the issue tracker URL to the about info.
- Italian translation updated. \[fabpolli\]

**3.8.0**

- Added _reset all_ option in sudoku list menu. \[D0ct0rZl0\]
- Added a new option to fill in pencil marks with all values.
- Added _ASC/DESC_ sorting and hex input for custom theme colours. \[demield\]

**3.7.0**

- Add copy and paste ability in sudoku editor. \[demield\]

**3.6.1**

- Fixed a bux introduced in 3.6.0 that prevented the import of sudokus in sdm format.

**3.6.0**

- Fix import and export of sudokus in Android 11. \[autinerd\]

**3.5.1**

- Fixed a crash when import sudokus on Android 10 or higher.
- Basque translation updated. \[Porrumentzio\]
- German translation updated. \[cyclingcat\]

**3.5.0**

- Prevent use undo once the game is finished.
- Show the resume button on the title screen only if you can resume a game.
- Code cleanup, update and convert drawables to vectors. \[TacoTheDank\]
- Added basque language. \[Porrumentzio\]
- French translation updated. \[guy-teube\]
- Chinese translation updated. \[kevinshq\]

**3.4.1**

- Improved SDM importer to support files with dots instead of zeros.
- If a digit is not present in the board, clear selection and highlight pencil marks (if any).
- French translation updated. \[Tubix\]
- German translation updated. \[cweiske\]

**3.4.0**

- New menu option to sort puzzles by last played and play time. \[fuentesj11\]

**3.3.1**

- Fix crash when using the new feature to automatically remove cell pencil marks. \[jlnordin\]

**3.3.0**

- Adds a new config option to skip title screen. \[jlnordin\]
- Fix folder view to show sudoku previews with custom theme colors. \[jlnordin\]
- Fix the crash when no cell was selected and you used the "hint" command. \[jlnordin\]

**3.2.0**

- Adds a new config option to remove pencil marks on number entry. \[jlnordin\]

**3.1.0**

- Adds a menu option to undo to before mistake. \[jlnordin\]
- French translation updated. \[Tubix\]

**3.0.0**

- New title screen with resume feature. \[jlnordin\]
- Spectacular full App UI themes. \[jlnordin\]
- New action buttons for undo and settings in game screen. \[jlnordin\]
- New option for highlight pencil marks that match a particular number when a cell is selected. \[jlnordin\]

**2.7.0**

- Awesome Theme Preview. \[jlnordin\]
- Keep same values highlighted after empty cell selection. \[ChrisLane\]
- Hungarian translation. \[madcrow\]

**2.6.4**

- French translation updated. \[Tubix\]

**2.6.3**

- Adds a menu option to check if a puzzle is solvable when editing or adding a puzzle. \[ssteve3424\]

**2.6.2**

- Solved import crash.
- Puzzle solver always work even if you made a mistake. \[ssteve3424\]
- Added a new option to get hints (single cell solved). \[ssteve3424\]

**2.6.1**

- New adaptive icon.

**2.6.0**

- Added permission checks for reading/writing external storage. \[ssteve3424\]
- Added puzzle solver. \[ssteve3424\]

**2.5.2**

- Added chinese language. \[kevinshq\]

**2.5.1**

- Expose the settings menu everywhere. \[bebehei\]
- Updated german translation. \[Stunkymonkey, bebehei\]

**2.5.0**

- Added new themes and custom theme settings. \[spimanov\]

**2.4.4**

- Fixed export issue. \[spimanov\]

**2.4.3**

- Added portuguese language. \[Joaquim Pedroto Costa\]

**2.4.2**

- Added brazilian portuguese language. \[prschopf\]

**2.4.1**

- Minor bug importing version 1 pencil marks fixed.

**2.4.0**

- Checkpoint issue - Now app saves undo history for games which are in "Playing" status. As well as checkpoints.
- After performing an undo, the previously edited cell will be selected.
- New option has been added - Bidirectional selection for Single Number input panel. Any selection of a cell will automatically select a corresponding number of the input panel and vice versa. If user selects a number on input panel, all corresponding cells will be highlighted (in case if option Highlight Similar is also enabled).
- Fixed a small issue with the congratulation message: "Congratulations, you have solved the puzzle in nn:nn". How to reproduce: solve a puzzle, get a message, restart the puzzle, solve it again, and get the same message (with the same time) as the first.
- Fixed a small issue with SN input panel - selected number stays in "completed" status after a puzzle restart.

**2.3.0**

- Highlight similar cells - Now you can activate a new option to highlight cells with similar values. \[spimanov\]

**2.2.1**

- Added turkish language. \[gokaykayki\]

**2.2**

- New dark default themes.

**2.1**

- Adapted to Material design.
- Better icons.

**2.0 RC0**

- Adapted to Holo Theme.
- New fancy icon.
- Translated to spanish.

**1.1.5**

- Fixes crashing on Android 3 (issue #148).

**1.1.4**

- New logo (issue #134) \[msal.coding\].
- Fixes few minor bugs.

**1.1.3**

- Undo checkpoints - press Menu and select "Set checkpoint" while in game. You can later return to this checkpoint by selecting "Undo to checkpoint" (issue #109) \[thexbin\].
- Some users report problems with install / update, this update hopefully fixes that.
- Fixes bug with undo when under certain circumstances undo information is lost (issue #86).

**1.1.2**

- Manual import of puzzles from SD card - just press Menu and select Import \[dracul\].
- OpenSudoku is now installed on SD card by default.

**1.1.1**

- Android 2.2 - move application to SD card support.
- Fixes few bugs reported via new Android 2.2 "Report" button.
- Few visual tweaks for Nexus One.
- "Fill in pencil marks" menu item can now be enabled in game settings.

**1.1.0**

- Italian translation \[diego.pierotto\]
- Few visual tweaks (thicker lines, some changes in Paper themes)
- New game helper - shows how many times number has been used \[Todd Southen\]
- Fixed bugs:
    - QVGA - Screen is sometimes shifted down (issue #82)
    - Undo should be disabled for a completed puzzle (issue #84)
    - List of games does not update (issue #73)
    - Other small fixes
