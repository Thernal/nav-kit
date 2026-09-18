# Agent skills

Skills for AI coding agents working in projects that **use** nav-kit. Each skill is a directory with a
`SKILL.md` — YAML frontmatter (`name`, `description`) plus instructions — and the reference files it points
to, in the [Agent Skills](https://agentskills.io) layout.

| Skill | Use it when |
|---|---|
| [`nav-kit`](nav-kit/SKILL.md) | installing nav-kit into an app; adding screens, routes and navigation; guards (sign-in redirects, unsaved-work refusals, deferred 401/PIN checks); results and arguments; back interception; deep links; tabs and nested hosts; bottom sheets and transitions; navigation events and tests; debugging nav-kit errors or reviewing navigation code |

`nav-kit` keeps its entry file short and loads detail on demand:

```
nav-kit/
├── SKILL.md                              the model, orientation greps, task router, verification checklist
└── references/
    ├── setup.md                          dependencies, graph (Metro or by hand), root, platform entry points
    ├── screens-and-navigation.md         routes, entries, commands and outcomes, nested hosts, tabs, sheets, transitions
    ├── guards.md                         RouteGuard, transition guards, deferred decisions, invalidations, the runner's rules
    ├── passing-data.md                   results, arguments, and what survives process death
    ├── back-handling.md                  NavigationBackHandler and its limits
    ├── deep-links.md                     handlers, parsing, root resolution, platform wiring, outbound links
    ├── events-and-testing.md             NavigationEventSink recipes, unit-test harnesses
    └── troubleshooting.md                error messages, symptoms, review checklist
```

## Using a skill

**Without installing.** An agent can read `skills/nav-kit/SKILL.md` straight from this repository and follow
its links; nothing in it depends on being installed.

**Installed**, so the agent loads it on its own when a task matches the description — copy the skill
directory into the directory your agent loads skills from:

```sh
git clone --depth 1 https://github.com/Thernal/nav-kit.git /tmp/nav-kit

# Claude Code, for one project
mkdir -p .claude/skills && cp -R /tmp/nav-kit/skills/nav-kit .claude/skills/

# Claude Code, for every project on this machine
mkdir -p ~/.claude/skills && cp -R /tmp/nav-kit/skills/nav-kit ~/.claude/skills/
```

Runtimes that read Agent Skills from another directory (for example `.agents/skills/`) take the same
directory unchanged.

A skill describes the revision of the kit it was copied from. When the project moves to a newer nav-kit,
copy the skill again from the same revision.

## Maintaining

The skills restate the public contracts documented in [`navigation/api/README.md`](../navigation/api/README.md)
for an agent audience. A change to a public contract updates that README and the matching reference file in
the same change; an error message changed in `navigation/impl` updates
[`troubleshooting.md`](nav-kit/references/troubleshooting.md).
