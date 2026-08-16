package de.halbmann.sam.assistant.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.assistant.boundary.ProgrammeTextDrafter;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionItemEntity;
import de.halbmann.sam.business.collections.entity.TextCollectionItemEntity;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.ValidationException;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgrammeTextDraftServiceTest {

    @Mock
    ProgrammeTextDrafter drafter;

    @Mock
    SheetCollectionRepository collectionRepository;

    @InjectMocks
    ProgrammeTextDraftService service;

    final String collectionId = UUID.randomUUID().toString();

    @Test
    void itemNotInCollection_throwsEntityNotFound() {
        SheetCollectionEntity collection = new SheetCollectionEntity();
        collection.setItems(new ArrayList<>());
        when(collectionRepository.findByIdOptional(UUID.fromString(collectionId)))
                .thenReturn(Optional.of(collection));

        assertThrows(
                EntityNotFoundException.class,
                () -> service.draft(collectionId, UUID.randomUUID().toString(), "en"));
    }

    @Test
    void itemIsNotTextItem_throwsValidation() {
        SheetCollectionItemEntity sheetItem = new SheetCollectionItemEntity();
        sheetItem.setId(UUID.randomUUID());
        SheetCollectionEntity collection = new SheetCollectionEntity();
        collection.setItems(new ArrayList<>(List.of(sheetItem)));
        when(collectionRepository.findByIdOptional(UUID.fromString(collectionId)))
                .thenReturn(Optional.of(collection));

        assertThrows(
                ValidationException.class,
                () -> service.draft(collectionId, sheetItem.getId().toString(), "en"));
    }

    @Test
    void noNeighboringSheet_throwsValidation() {
        TextCollectionItemEntity textItem = new TextCollectionItemEntity();
        textItem.setId(UUID.randomUUID());
        SheetCollectionEntity collection = new SheetCollectionEntity();
        collection.setItems(new ArrayList<>(List.of(textItem)));
        when(collectionRepository.findByIdOptional(UUID.fromString(collectionId)))
                .thenReturn(Optional.of(collection));

        assertThrows(
                ValidationException.class,
                () -> service.draft(collectionId, textItem.getId().toString(), "en"));
    }

    @Test
    void prefersNextSheetOverPreviousSheet() {
        SheetMusicEntity previousSheet = sheet("Previous Piece");
        SheetMusicEntity nextSheet = sheet("Next Piece");

        SheetCollectionItemEntity previousItem = new SheetCollectionItemEntity();
        previousItem.setId(UUID.randomUUID());
        previousItem.setSheet(previousSheet);

        TextCollectionItemEntity textItem = new TextCollectionItemEntity();
        textItem.setId(UUID.randomUUID());

        SheetCollectionItemEntity nextItem = new SheetCollectionItemEntity();
        nextItem.setId(UUID.randomUUID());
        nextItem.setSheet(nextSheet);

        SheetCollectionEntity collection = new SheetCollectionEntity();
        collection.setItems(new ArrayList<>(List.of(previousItem, textItem, nextItem)));
        when(collectionRepository.findByIdOptional(UUID.fromString(collectionId)))
                .thenReturn(Optional.of(collection));

        TokenUsage tokenUsage = new TokenUsage(10, 20);
        when(drafter.draft(contains("Next Piece"), eq("de")))
                .thenReturn(Result.<String>builder()
                        .content("Draft text")
                        .tokenUsage(tokenUsage)
                        .build());

        ProgrammeTextDraftService.DraftOutcome outcome =
                service.draft(collectionId, textItem.getId().toString(), "de");

        assertEquals("Draft text", outcome.draftText());
        assertEquals(tokenUsage, outcome.tokenUsage());
        verify(drafter)
                .draft(
                        argThat(metadata -> metadata.contains("Next Piece") && !metadata.contains("Previous Piece")),
                        eq("de"));
    }

    @Test
    void unsupportedLanguage_fallsBackToEnglish() {
        TextCollectionItemEntity textItem = new TextCollectionItemEntity();
        textItem.setId(UUID.randomUUID());
        SheetCollectionItemEntity nextItem = new SheetCollectionItemEntity();
        nextItem.setId(UUID.randomUUID());
        nextItem.setSheet(sheet("Some Piece"));
        SheetCollectionEntity collection = new SheetCollectionEntity();
        collection.setItems(new ArrayList<>(List.of(textItem, nextItem)));
        when(collectionRepository.findByIdOptional(UUID.fromString(collectionId)))
                .thenReturn(Optional.of(collection));
        when(drafter.draft(anyString(), eq("en")))
                .thenReturn(Result.<String>builder().content("Draft text").build());

        service.draft(collectionId, textItem.getId().toString(), "fr");

        verify(drafter).draft(anyString(), eq("en"));
    }

    private SheetMusicEntity sheet(String title) {
        SheetMusicEntity s = new SheetMusicEntity();
        s.setId(UUID.randomUUID());
        s.setTitle(title);
        MusicianEntity composer = new MusicianEntity();
        composer.setName("Test Composer");
        s.setComposer(composer);
        return s;
    }
}
