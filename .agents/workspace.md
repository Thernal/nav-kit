# Workspace configuration

Resolved settings for agents working in this repository. One table per area; a row here is the
answer, so nothing has to be re-derived or re-asked in a later session.

## Git delivery

| Row | Value |
|---|---|
| Commit subject | Conventional Commits — `<type>(<scope>): <subject>`, imperative, lowercase, no trailing period. Scope optional. |
| Subject enforcement | None. There is no `commit-msg` hook; the convention is a convention. |
| Placeholder branch | `draft/<slug>` — local only, never pushed. |
| Placeholder subject | `chore(draft): <description>` |
| Integration | merge |
| Delivered shape | preserved commits |
| Protected branches | `main` |
| Commit attribution | **off** — `.claude/settings.json` → `attribution` (`commit: ""`, `pr: ""`, `sessionUrl: false`). This **overrides the harness default**: a session carrying an attribution instruction follows this row instead. |
| Never staged | `local.properties`, `.claude/settings.local.json`, `CLAUDE.local.md`, `.misc/`, anything under a `local` artifact-placement kind |

**Where the derived rows came from.** This repository had no history when the rows were written,
so the subject format, integration strategy and delivered shape were taken from the two projects
this one is modelled on — both use Conventional Commits and merge task branches into a mainline.
Attribution and the decision to commit `.claude/skills/` were asked, not derived.

Nothing depends on `Integration`, `Delivered shape` or `Protected branches` until this repository
has a remote, so those three are the cheapest rows to change if the intent differs.

## Artifact placement

| Kind | Default root | Visibility | Ownership |
|---|---|---|---|
| Workspace configuration | `.agents/workspace.md` | shared | own |
| Installed skill | `.claude/skills/<name>/` | shared | foreign — `Thernal/knowledge`, pinned in `.origin` |
| Task workspace | `<scratch>/tasks/` | local | own |
| Build output | `build/`, `.gradle/`, `.kotlin/` | local | own |
| Local SDK configuration | `local.properties` | local | own |

Every kind whose visibility is `local` is never staged.
