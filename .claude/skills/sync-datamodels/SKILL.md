---
name: sync-datamodels
description: Regenerate ui/src/main/webui/src/app/model/datamodels.d.ts from the Java API classes using the typescript-generator-maven-plugin. Run this after adding or changing any DTO/enum in the api module to keep the Angular type model in sync.
---

# Sync Datamodels

Regenerates `datamodels.d.ts` from the Java `api` module source classes. The plugin scans `de.halbmann.sam.api.entity.**`, maps Java types to TypeScript interfaces/types, and writes the result to `ui/src/main/webui/src/app/model/datamodels.d.ts`.

## When to use

- After adding a new DTO, record, or enum under `api/src/main/java/de/halbmann/sam/api/entity/`
- After adding or removing fields on an existing DTO
- To verify the Angular model is in sync after a series of backend changes

## Steps

1. **Run the generator** using the Maven wrapper:

   ```
   .\mvnw.cmd generate-sources -pl ui -am -Pgenerate-ts
   ```

   On Linux/Mac use `./mvnw` instead.

2. **Show the diff**:

   ```
   git diff ui/src/main/webui/src/app/model/datamodels.d.ts
   ```

3. **Interpret the diff**:
   - New types/fields appearing — expected after adding Java classes/fields
   - Types disappearing — investigate: the Java class may have been deleted or moved out of the scanned package
   - No diff — the model was already in sync; nothing to do

4. **Run TypeScript compilation** to verify the generated types don't break anything:

   ```
   cd ui/src/main/webui && npx tsc --noEmit
   ```

5. **Report** what changed (or confirm no changes) and wait for the user to decide whether to stage/commit the updated file.

## Important notes

- The generator uses the **compiled** api classes, not source. If you just added a field, make sure the api module has been compiled (e.g. by running `.\mvnw.cmd compile -pl ui -am -Pgenerate-ts` which compiles first). The `-am` flag handles this automatically.
- The `requiredAnnotations` config means only fields annotated with `@NotBlank`, `@NotEmpty`, or `@NotNull` are typed as required (`field: type`); all others are optional (`field?: type`).
- Custom type mappings in the plugin config: `java.time.Duration` → `long`, `DifficultyLevel` → `short`. Add new mappings in `ui/pom.xml` under `<customTypeMappings>` if needed.
- The output file is determined by `<outputFile>` in `ui/pom.xml`. Do not change the path without also updating Angular imports.
- If the generator produces undesirable output for a specific type (e.g. a type you need to override manually), add it as a `<customTypeMapping>` in the plugin config rather than editing the generated file by hand.

## What the generator does NOT handle

- Custom union types you have defined manually (e.g. narrow string literals beyond enum values)
- Manually added utility types not present as Java classes
- Types from other modules not in `de.halbmann.sam.api.entity.**`

These must be maintained manually in `datamodels.d.ts` alongside the generated output.