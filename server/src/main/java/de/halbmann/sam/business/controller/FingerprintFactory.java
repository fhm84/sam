package de.halbmann.sam.business.controller;

import de.halbmann.sam.business.entity.FingerprintEntity;
import de.halbmann.sam.business.entity.MusicianEntity;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import java.util.Optional;

public class FingerprintFactory {

    public static FingerprintEntity forSheet(final SheetMusicEntity sheet) {
        final FingerprintEntity fingerprint = new FingerprintEntity();
        fingerprint.setVersion(FingerprintService.getFINGERPRINT_VERSION());
        fingerprint.setHashValue(FingerprintService.sheetFingerprint(
                sheet.getTitle(),
                sheet.getPublisher(),
                Optional.ofNullable(sheet.getComposer())
                        .map(MusicianEntity::getName)
                        .orElse(null),
                Optional.ofNullable(sheet.getArranger())
                        .map(MusicianEntity::getName)
                        .orElse(null)));
        return fingerprint;
    }
}
