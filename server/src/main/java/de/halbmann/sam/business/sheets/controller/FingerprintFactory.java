package de.halbmann.sam.business.sheets.controller;

import de.halbmann.sam.business.persons.entity.PersonEntity;
import de.halbmann.sam.business.sheets.entity.FingerprintEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.controller.FingerprintService;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FingerprintFactory {

    public static FingerprintEntity forSheet(final SheetMusicEntity sheet) {
        final FingerprintEntity fingerprint = new FingerprintEntity();
        fingerprint.setVersion(FingerprintService.getFINGERPRINT_VERSION());
        fingerprint.setHashValue(FingerprintService.sheetFingerprint(
                sheet.getTitle(),
                sheet.getPublisher(),
                Optional.ofNullable(sheet.getComposer())
                        .map(PersonEntity::getName)
                        .orElse(null),
                Optional.ofNullable(sheet.getArranger())
                        .map(PersonEntity::getName)
                        .orElse(null)));
        return fingerprint;
    }
}
