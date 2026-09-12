#!/bin/sh
# knowledgectl.sh — read a personal knowledge corpus from any project.
#
# Dependencies: git and a POSIX shell. That is the whole list — no runtime, no
# token, no API. Everything is a git operation against a repository the machine
# already has SSH access to.
#
# The corpus is fetched blobless + sparse + shallow, restricted to notes/, so the
# transfer stays flat as raw/ grows: raw/ is the audit trail and is never needed
# to answer a question.
set -eu

REPO="${KNOWLEDGE_REPO:-git@github.com:Thernal/knowledge.git}"
CACHE="${KNOWLEDGE_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/knowledge-corpus}"
WORK="$CACHE/repo"
NOTES="$WORK/notes"
STAMP="$CACHE/.head"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }
say() { printf '%s\n' "$*" >&2; }

# --- freshness ---------------------------------------------------------------
# Keyed on the default branch's commit rather than a published version ref. A
# skill needs per-skill versions because each is installed independently; a
# corpus only needs "is my copy current", and HEAD answers that in one round
# trip with nothing to publish and therefore nothing to forget to publish.
remote_head() { git ls-remote "$REPO" HEAD 2>/dev/null | head -1 | cut -f1; }

fetch_corpus() {
    mkdir -p "$CACHE"
    if [ -d "$WORK/.git" ]; then
        git -C "$WORK" fetch --quiet --depth 1 --filter=blob:none origin HEAD 2>/dev/null \
            || die "cannot reach $REPO — check SSH access to the corpus"
        git -C "$WORK" checkout --quiet --force FETCH_HEAD 2>/dev/null \
            || die "fetched, but could not check out"
    else
        rm -rf "$WORK"
        git clone --quiet --depth 1 --filter=blob:none --sparse "$REPO" "$WORK" 2>/dev/null \
            || die "cannot clone $REPO — check SSH access to the corpus"
        git -C "$WORK" sparse-checkout set notes >/dev/null 2>&1 \
            || die "sparse checkout failed — git 2.25+ required"
    fi
    remote_head > "$STAMP"
}

ensure() {
    head=$(remote_head)
    if [ -z "$head" ]; then
        # Offline is not an error when the cache can still answer. Say so on
        # stderr so the answer is never silently stale.
        [ -d "$NOTES" ] || die "offline, and no cached corpus to read"
        say "warning: cannot reach the corpus; answering from cache"
        return 0
    fi
    if [ ! -f "$STAMP" ] || [ "$(cat "$STAMP")" != "$head" ]; then
        fetch_corpus
    fi
    [ -d "$NOTES" ] || die "corpus fetched but notes/ is missing"
}

note_path() {
    p="$NOTES/$1.md"
    [ -f "$p" ] || die "no note '$1' — try: $(basename "$0") search \"$1\""
    printf '%s' "$p"
}

# --- commands ----------------------------------------------------------------
cmd_search() {
    [ $# -gt 0 ] || die "usage: search <terms...>"
    pat=$(printf '%s' "$*" | tr ' ' '|')
    found=0

    if [ -f "$NOTES/_topics.tsv" ]; then
        hits=$(grep -iE "$pat" "$NOTES/_topics.tsv" 2>/dev/null | grep -v '^#' | cut -f1,2 || true)
        if [ -n "$hits" ]; then
            printf 'TOPICS (subject)\n%s\n\n' "$hits"; found=1
        fi
    fi
    if [ -f "$NOTES/_threads.md" ]; then
        hits=$(grep -iE "^## .*($pat)" "$NOTES/_threads.md" 2>/dev/null | sed 's/^## /  /' || true)
        if [ -n "$hits" ]; then
            printf 'THREADS (what the corpus concluded)\n%s\n\n' "$hits"; found=1
        fi
    fi
    if [ -f "$NOTES/_index.md" ]; then
        hits=$(grep -iE "$pat" "$NOTES/_index.md" 2>/dev/null | grep '^- \*\*' \
               | sed 's/^- \*\*\[\[/  /; s/\]\]\*\* — / — /' || true)
        if [ -n "$hits" ]; then
            printf 'NOTES\n%s\n\n' "$hits"; found=1
        fi
    fi

    if [ "$found" = 0 ]; then
        say "no match. The corpus may not cover this, or it may use different words —"
        say "the fix for the second case is an alias in notes/_topics.tsv upstream."
        return 1
    fi
    printf 'next: %s get <slug> --section takeaways   |   %s thread <slug>\n' \
        "$(basename "$0")" "$(basename "$0")"
}

cmd_topics() {
    [ -f "$NOTES/_topics.tsv" ] || die "this corpus has no topic vocabulary"
    if [ $# -gt 0 ]; then
        grep -iE "$(printf '%s' "$*" | tr ' ' '|')" "$NOTES/_topics.tsv" | grep -v '^#' | cut -f1,2,3
    else
        grep -v '^#' "$NOTES/_topics.tsv" | awk -F'\t' 'NF>1 && $1!="slug" {printf "%-32s %s\n", $1, $3" note(s)"}'
    fi
}

cmd_threads() {
    [ -f "$NOTES/_threads.md" ] || die "this corpus has no threads"
    grep '^## ' "$NOTES/_threads.md" | sed 's/^## /  /'
}

cmd_thread() {
    [ $# -eq 1 ] || die "usage: thread <slug>"
    [ -f "$NOTES/_threads.md" ] || die "this corpus has no threads"
    awk -v want="$1" '
        /^## / { if (inb) exit; id=$2; inb = (id==want) }
        inb { print }
    ' "$NOTES/_threads.md" | grep . || die "no thread '$1' — try: $(basename "$0") threads"
}

cmd_get() {
    [ $# -ge 1 ] || die "usage: get <slug> [--section <name>]"
    slug=$1; shift
    section=""
    while [ $# -gt 0 ]; do
        case $1 in
            --section) [ $# -ge 2 ] || die "--section needs a value"; section=$2; shift 2 ;;
            *) die "unknown option: $1" ;;
        esac
    done
    path=$(note_path "$slug")
    [ -n "$section" ] || { cat "$path"; return 0; }

    # Sections are addressed by heading slug or by an explicit <!--s:id--> anchor.
    # Anchors win where both exist, because a heading stops being unique once a
    # note has absorbed several sources.
    awk -v want="$section" '
        function slug(s) { gsub(/[^a-zA-Z0-9]+/, "-", s); sub(/-$/, "", s); return tolower(s) }
        /^<!--s:/ {
            id=$0; sub(/^<!--s:/, "", id); sub(/[^a-zA-Z0-9-].*$/, "", id)
            if (inb) exit
            if (id == want) inb=1
            next
        }
        /^## / {
            # An anchor sits on the line *above* its heading, so the first heading
            # after opening belongs to the section; only the second one ends it.
            if (inb) { if (headings++ > 0) exit }
            else {
                t=$0; sub(/^## /, "", t)
                if (slug(t) == want || index(slug(t), want) == 1) { inb=1; headings=1 }
            }
        }
        inb { print }
    ' "$path" | grep . || die "no section '$section' in $slug — sections: $(grep '^## ' "$path" | sed 's/^## //' | tr '\n' '/' )"
}

cmd_cite() {
    [ $# -eq 1 ] || die "usage: cite <slug>"
    path=$(note_path "$1")
    sed -n 's/^source_url: *"\{0,1\}\([^"]*\)"\{0,1\}$/\1/p;
            s/^additional_sources: *\(.*\)$/also: \1/p' "$path" | head -5
}

cmd_status() {
    if [ -f "$STAMP" ]; then
        printf 'cache   %s\nversion %s\n' "$CACHE" "$(cut -c1-7 < "$STAMP")"
        printf 'notes   %s\n' "$(find "$NOTES" -name '*.md' ! -name '_*' 2>/dev/null | wc -l | tr -d ' ')"
    else
        printf 'no cache yet at %s\n' "$CACHE"
    fi
    printf 'repo    %s\n' "$REPO"
}

usage() {
    cat <<USAGE
knowledgectl.sh — read a knowledge corpus from any project (git + sh only)

  search <terms...>              topics, threads and notes matching the words
  topics [<term>]                the controlled subject vocabulary
  threads                        the claims the corpus itself holds
  thread <slug>                  one claim with its evidence and qualifications
  get <slug> [--section <name>]  a note, or just one of its sections
  cite <slug>                    the original source URL(s)
  status                         cache location and version
  sync                           refresh the cache now

Sections are named by heading slug (takeaways, key-idea, example, connections)
or by an explicit anchor id. Prefer --section: a whole note is ~1300 tokens, a
section is ~300.

Environment: KNOWLEDGE_REPO, KNOWLEDGE_CACHE
USAGE
}

[ $# -gt 0 ] || { usage; exit 0; }
cmd=$1; shift
case $cmd in
    -h|--help|help) usage ;;
    status)  cmd_status ;;
    sync)    fetch_corpus; cmd_status ;;
    search)  ensure; cmd_search "$@" ;;
    topics)  ensure; cmd_topics "$@" ;;
    threads) ensure; cmd_threads ;;
    thread)  ensure; cmd_thread "$@" ;;
    get)     ensure; cmd_get "$@" ;;
    cite)    ensure; cmd_cite "$@" ;;
    *) die "unknown command: $cmd (try --help)" ;;
esac
