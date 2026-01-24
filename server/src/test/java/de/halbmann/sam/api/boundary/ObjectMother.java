package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.*;

/** Test data builder */
public class ObjectMother {

  public static SheetMusic createSimpleSheetMusic() {
    final SheetMusic sheetMusic = new SheetMusic();
    sheetMusic.setTitle("Music from Pirates of the Caribbean");
    sheetMusic.setPublisher("Disney Music Corp");
    sheetMusic.setGenre("Movie");
    sheetMusic.setDifficultyLevel("Medium");
    return sheetMusic;
  }

  public static Musician createComposer() {
    final Musician composer = new Musician();
    composer.setName("Klaus Badelt");
    composer.setBirthYear(1967);
    return composer;
  }

  public static Musician createArranger() {
    final Musician arranger = new Musician();
    arranger.setName("John Wasson");
    return arranger;
  }

  public static SheetMusic createFullSheetMusic() {
    return createSheetMusicWithComposerAndArranger(createComposer(), createArranger());
  }

  public static SheetMusic createSheetMusicWithComposer(final Musician composer) {
    final SheetMusic sheetMusic = createSimpleSheetMusic();
    sheetMusic.setComposer(composer);
    return sheetMusic;
  }

  public static SheetMusic createSheetMusicWithArranger(final Musician arranger) {
    final SheetMusic sheetMusic = createSimpleSheetMusic();
    sheetMusic.setArranger(arranger);
    return sheetMusic;
  }

  public static SheetMusic createSheetMusicWithComposerAndArranger(
      final Musician composer, final Musician arranger) {
    final SheetMusic sheetMusic = createSimpleSheetMusic();
    sheetMusic.setComposer(composer);
    sheetMusic.setArranger(arranger);
    return sheetMusic;
  }

  public static Instrumentation flute1C() {
    final Instrumentation flute = new Instrumentation();
    flute.setInstrumentName("Flute");
    flute.setPartLabel("1");
    flute.setTransposition(InstrumentTransposing.C);
    flute.setNotationType(NotationType.STANDARD);
    flute.setClef(Clef.TREBLE);
    return flute;
  }

  public static Instrumentation trumpet2Bb() {
    final Instrumentation trumpet = new Instrumentation();
    trumpet.setInstrumentName("Trumpet");
    trumpet.setPartLabel("2");
    trumpet.setTransposition(InstrumentTransposing.Bb);
    trumpet.setNotationType(NotationType.STANDARD);
    trumpet.setClef(Clef.TREBLE);
    return trumpet;
  }
}
