# Releases

Versioning and changelog generation are automatic — you should not hand-edit
the version in `pom.xml` or write `CHANGELOG.md` entries by hand.

## Two channels: stable (`master`) and beta (`develop`)

- **`master` → stable releases** (`vX.Y.Z`). This is what most players
  should install.
- **`develop` → beta prereleases** (`vX.Y.Z-beta.N`). GitHub marks these
  releases as a "Pre-release" automatically (Release Please's
  `prerelease: true` config). Merging `develop` into `master` is what
  "promotes" accumulated beta work into the next real stable release.

Both are handled by the same `.github/workflows/release.yml`, via two
independent Release Please jobs (`release-please-stable`,
`release-please-beta`) gated on which branch was pushed to, each with its
own config/manifest (`release-please-config.json` +
`.release-please-manifest.json` for stable, the `-beta` variants of both
for beta) so the two version lines and changelogs don't collide.

## How a release happens

1. Commits land on `master` or `develop` (via PR, per branch protection)
   using [Conventional Commits](https://www.conventionalcommits.org/)
   (`feat:`, `fix:`, `feat!:`/`BREAKING CHANGE:`, etc.) — the same style
   already used in this repo.
2. [Release Please](https://github.com/googleapis/release-please) watches
   both branches and keeps a standing `chore(<branch>): release x.y.z`
   pull request up to date on each, computing the next version from those
   commits (`fix:` → patch, `feat:` → minor, a `!` or `BREAKING CHANGE:`
   footer → major) and drafting `CHANGELOG.md` from their messages.
3. Merging that release PR is the actual release trigger: Release Please
   tags the merge commit (`vX.Y.Z` on `master`, `vX.Y.Z-beta.N` on
   `develop`), bumps the `<version>` in `pom.xml` to match (see the
   `extra-files` entry in the relevant config file), and creates the
   GitHub Release with the changelog as its body.
4. That same workflow then builds the installers (see below) and uploads
   them as assets on the release that was just created.

The merge in step 3 happens automatically — no human click needed. Right
after Release Please creates/updates the PR, the same job asks GitHub to
auto-merge it (`gh pr merge --auto`), which waits for the branch's
required status checks to pass and then merges it for you. This exists
because leaving the release PR sitting open was actively harmful: every
other PR that merged into `develop`/`master` in the meantime kept
updating that same standing PR, and if it sat unmerged for a while before
finally being merged, it was easy to lose track of which commits were and
weren't actually reflected in `CHANGELOG.md` for that version. Auto-merge
means each release goes out the moment the commit that should trigger it
lands and CI is green, so there's no window where the release PR can go
stale.

**Why this needs a PAT, not just `GITHUB_TOKEN`**: GitHub suppresses
further workflow-triggering events from a push/merge made with the
default `GITHUB_TOKEN`, to prevent recursive workflow runs. If the
auto-merge step used `GITHUB_TOKEN`, the merge would land the release
commit on `master`/`develop` but never fire the follow-up `push` event
that lets `release-please-action` notice it and actually cut the tag +
GitHub Release — the PR merges, but nothing downstream runs (this
happened in practice; see the `RELEASE_PLEASE_TOKEN` secret and the
comment above the `jobs:` key in `release.yml`). Both
`release-please-action` and the auto-merge step instead use
`RELEASE_PLEASE_TOKEN`, a fine-grained PAT scoped to this repo's
`Contents` and `Pull requests` permissions (matching this workflow's own
`permissions:` block, nothing broader) — a PAT-driven merge isn't subject
to that suppression, and also isn't caught by GitHub's fork-PR
workflow-approval gate, which the bot-authored release PR was otherwise
triggering.

Nothing about this requires a human to decide "what's the next version
number" — that's entirely derived from commit messages. If a change
shouldn't trigger a release at all, use a non-triggering type (`chore:`,
`docs:`, `test:`, `style:`) or `fix:`/`feat:` as appropriate — Release
Please only reacts to types that map to a version bump.

## Installer builds

Triggered only when Release Please reports a release was actually created
(i.e. on merge of the release PR, not on every push to `master`). The
`build-installers` job in `.github/workflows/release.yml` runs as a matrix
across three runners, since `jpackage` only builds for the OS it runs on
— there's no cross-compiling a Windows installer from Linux, etc.:

| Runner | Output | Notes |
|---|---|---|
| `windows-latest` | `Veil-<version>.exe` | Built via WiX (preinstalled on the runner). Start Menu shortcut, directory chooser. |
| `ubuntu-latest` | `Veil-<version>.deb` | Needs `fakeroot` (installed as a workflow step). Debian/Ubuntu only — no `.rpm` variant yet. |
| `macos-latest` | `Veil-<version>.pkg` | **Unsigned** — no Apple Developer account, so first launch shows Gatekeeper's "unidentified developer" warning; the user has to right-click → Open once. |

Each job: checks out the release tag, `mvn -B package` (produces
`target/Veil-<version>-app.jar`, a single runnable jar with all
dependencies bundled — see the `maven-shade-plugin` execution in
`pom.xml`), then `jpackage --type <exe|deb|pkg>` wraps that jar with a
bundled JRE — no separate Java install required on the player's machine —
and the "Normalize installer filename" step renames whatever `jpackage`
produced (its own per-OS naming conventions vary, e.g. Debian's
underscore-separated scheme) to a consistent `Veil-<version>.<ext>`.
The result is uploaded as a release asset.

**Bundling `mods/`**: the repo's checked-in `mods/` directory (currently
just `mods/core`) is passed to `jpackage` via `--app-content mods`, not
staged alongside the jar in `target/jpackage-input`. `jpackage --input`
content ends up nested inside an `app/` subfolder of the installed
application, but `ModLoader` resolves `mods/` relative to the running
game's own working directory — which is the top-level install directory,
not `app/`. `--app-content` places its target at the top level instead,
alongside the executable, so `mods/core` ends up exactly where the game
looks for it. Verified locally via a `--type app-image` build (no
installer/WiX required) and a real launch of the resulting
`Veil.exe`, which loaded every `core` tile, building, class, item, and
quest from the bundled `mods/core`.

**Beta version numbers**: `jpackage`'s Windows/macOS installers require a
clean numeric `--app-version` (no `-beta.N` suffix accepted), so the
workflow strips the suffix for that flag specifically
(`APP_VERSION=${full_version%%-*}`) while the uploaded filename still uses
the full version including the suffix (`FULL_VERSION`), so e.g. a
`v0.3.0-beta.2` release uploads `Veil-0.3.0-beta.2.exe` even though
the installer's own internal version metadata just says `0.3.0`.

**Pre-1.0 versions on macOS**: `jpackage --type pkg` additionally rejects a
major version of `0` ("The first number in an app-version cannot be zero
or negative"). While the project is still `0.x.y`, the macOS job bumps
just its own internal bundle version metadata (`0.2.0` → `1.2.0`) — the
uploaded filename is unaffected and still shows the real version. This
step becomes a no-op once the project reaches `1.0.0`.

**App icon**: `jpackage --icon` takes a different format per OS, so the
matrix passes `packaging/icons/veil.ico` (Windows), `veil.png` (Linux) and
`veil.icns` (macOS). All three were generated once from the supplied
512x512 PNG with Pillow and are committed; there is no build step that
regenerates them, so replacing the artwork means re-exporting all three.
The runtime window/taskbar icon is a separate copy of the PNG at
`src/main/resources/icons/veil.png`, applied by `AppIcon` (keep the two
PNGs identical).

### Testing the installer build without cutting a release

The workflow also accepts `workflow_dispatch` (Actions tab → Release →
Run workflow, or `gh workflow run release.yml`). This runs the same
`build-installers` matrix against the current commit and uploads each
platform's installer as a workflow artifact instead of a release asset —
useful for validating a jpackage change (e.g. a new platform, new
`jpackage` flags) without waiting for or faking an actual release.

### Building an installer locally

```powershell
mvn -B clean package
mkdir target\jpackage-input
copy target\Veil-0.1.0-app.jar target\jpackage-input\
jpackage --type exe --input target\jpackage-input --dest target\dist `
  --name Veil --app-version 0.1.0 --vendor SwiftFaze `
  --main-jar Veil-0.1.0-app.jar --main-class com.swiftfaze.veil.Main `
  --app-content mods `
  --win-menu --win-shortcut --win-dir-chooser
```

`--app-content mods` must run from the repo root (or with `mods` replaced
by a path to a directory containing a `core` subfolder) — it bundles the
checked-in `mods/core` at the top level of the installed application,
alongside `Veil.exe`.

Requires a JDK with `jpackage` (bundled since JDK 14) and, for `--type exe`
specifically, the [WiX Toolset](https://wixtoolset.org/) v3 on `PATH`. Without
WiX, use `--type app-image` instead — it produces a `target\dist\Veil\Veil.exe`
launcher folder (no installer, just unzip-and-run) and needs no extra tooling.
The same idea applies on Linux/macOS with `--type deb`/`--type pkg` swapped
in, run on that OS — `jpackage` cannot target an OS other than the one
it's running on.
