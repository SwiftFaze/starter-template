# Player-facing wiki

Player-facing game content — classes, stats, and (as they're added) tiles,
buildings, items, etc. — lives in the [GitHub wiki](https://github.com/SwiftFaze/Veil/wiki),
not in `docs/`. The split:

- `docs/` — for contributors: architecture, testing, release process.
- The wiki — for players: what's actually in the game and what the numbers
  mean, kept in sync with the code.

## Current pages

- **Home** — index
- **Classes** — Warrior/Mage base stats, derived Attack Power/Defense
- **Player Stats** — what each attribute does, the Attack Power/Defense
  formulas, leveling

## This is part of Step 7 (Documentation)

Any change to player-facing game data must update the matching wiki page
in the same change — not as a follow-up. Concretely: if a class's base
stats change in `Warrior.java`/`Mage.java` (e.g. a max HP tweak), the
**Classes** wiki page must be edited to match before the feature is done.
The same rule applies to a new class, a new attribute, a changed formula in
`Stats.java`, or any other player-visible game data. If a change doesn't
touch anything player-visible, there's nothing to do here — same as any
other "no doc update needed" case in Step 7.

This is a manual step, deliberately: wiki content is prose aimed at
players, not a mechanical dump of source values, so it needs the same
judgment as writing any other doc — there's no script that generates it
from the Java source.

## Editing the wiki

The wiki is its own git repository, independent of this one. Clone it into
`wiki/` at the root of this repo for convenience — that path is gitignored,
so it stays local and doesn't entangle the two repos' history:

```
git clone https://github.com/SwiftFaze/Veil.wiki.git wiki
```

Edit the relevant `.md` file, commit, and push directly to `master` — the
wiki isn't covered by this repo's branch protection (GitHub doesn't support
branch protection on wikis), so no PR is required. You can also edit pages
directly in the browser via the repo's **Wiki** tab.
