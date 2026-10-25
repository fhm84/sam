# ADR-0014: Mermaid for diagrams in the docs

**Status:** accepted

## Context

The docs used two kinds of diagrams: ASCII art in Markdown code blocks (system context,
modules, data model, deployment, stakeholders) and PlantUML files (the four runtime flows,
a class-level component diagram). The ASCII diagrams were tedious to edit and had drifted:
the data model showed less than half of the tables, and the stakeholder view drew the CLI
as something the server calls. GitHub doesn't render PlantUML, so the `.puml` files showed
up there as source text unless someone committed generated images, which go stale.
`docs/` is read on GitHub and opened as an Obsidian vault, and both render Mermaid inline.

## Decision

Diagrams in Markdown docs are Mermaid code blocks (```` ```mermaid ````) inside the page
that explains them, not separate files and not committed images. We use `flowchart` for
structure (context, modules, deployment), `erDiagram` for the data model, and
`sequenceDiagram` for runtime flows. Diagrams use the default theme without custom colours,
so they stay readable in light and dark mode. Diagrams stay at the level of modules,
packages and tables; class names that change with every refactoring are left out.

Folder and file trees stay as plain text blocks: a tree shows a folder layout better than
a diagram does.

## Consequences

- Diagrams render on GitHub, in Obsidian, and in IntelliJ's Markdown preview, and are
  reviewed as text diffs in pull requests.
- The PlantUML files are removed: `docs/architecture/architecture.puml`, the four
  `docs/architecture/runtime/*.puml` flows (now sequence diagrams in
  `runtime/README.md`), and the unused `src/site/resources/*.puml` from the initial commit.
- Mermaid offers little layout control. A diagram that gets too crowded is split, or left
  at a coarser level of detail, rather than tuned.
- A diagram is checked against the code when its page is touched; the data model against
  the Flyway migrations.
