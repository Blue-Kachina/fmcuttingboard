# Roadmap: Publishing FMCuttingBoard on the JetBrains Plugin Marketplace

Repository: [Blue-Kachina/fmcuttingboard](https://github.com/Blue-Kachina/fmcuttingboard)

This roadmap started from a ChatGPT-drafted publishing plan, verified against the repo, then acted
on directly. Everything below reflects the **current** repo state (last updated 2026-07-27). Items
are marked `[x]` (done), `[ ]` (still open), or `[~]` (scaffolded, needs a manual follow-up you must
do yourself — e.g. anything touching credentials or GitHub settings).

Current baseline facts:

- Plugin ID `dev.bluekachina.fmcuttingboard`, name `FMCuttingBoard`.
- `pluginVersion=1.0.5`, `platformType=IC`, `platformVersion=2024.3`, `javaVersion=21`.
- `sinceBuild="242"` (IntelliJ Platform 2024.2), no `untilBuild` cap.
- IntelliJ Platform Gradle Plugin **2.18.1** (bumped from the earlier 2.0.1).
- Root `LICENSE` (MIT) now exists.
- `org.jetbrains.changelog` is applied and wired to generate `<change-notes>` from `CHANGELOG.md`.
- Repo-wide CRLF→LF normalization done, with `.gitattributes` added to keep it that way
  (`gradlew.bat` correctly stays CRLF).
- `v1.0.5` tag committed, pushed, and confirmed on GitHub: the `Release` Action ran successfully
  against it and published a GitHub Release with `FMCuttingBoard-1.0.5.zip` attached as a downloadable
  asset — the tag → build → release pipeline is proven working end-to-end, not just configured.
- JetBrains Marketplace vendor account created: vendor id `blue-kachina` (matches `plugin.xml`'s
  `vendor` name "Blue Kachina").
- Real screenshot added: `docs/screenshots/FmCuttingBoardScreenie.png` (Tools menu open over a
  generated fmxmlsnippet XML file), wired into both `README.md` and `docs/MarketplaceListing.md`.
- The exact release ZIP was manually installed in a clean IDE and smoke-tested successfully.

---

## Phase 1 — Legal & metadata baseline

### 1.1 License
- [x] Added root `LICENSE` (MIT), copyright "Blue Kachina".
- [x] Added a `## License` section to `README.md` pointing at it.
- [ ] When uploading to Marketplace, select **MIT** and supply the GitHub repo as the source-code URL.

### 1.2 Third-party bundle — resolved
Already fixed in a prior session

### 1.3 Marketplace-facing metadata
- [x] `org.jetbrains.changelog` plugin applied in `build.gradle.kts` and wired into `patchPluginXml`.
      Verified by running `./gradlew patchPluginXml` — the patched `plugin.xml` now contains a real
      `<change-notes>` block rendered from the `[1.0.5]` entry in `CHANGELOG.md`.
- [x] `CHANGELOG.md` reconciled with real version history (1.0.0 → 1.0.4, reconstructed from git tags
      `v1.0.0`–`v1.0.4` and their commit ranges), plus a `[1.0.5]` entry covering this session's
      packaging cleanup and verifier-driven fixes — `gradle.properties` bumped to `1.0.5` to match.
- [~] **Vendor `<vendor email="...">` — intentionally left blank.** You chose to skip publishing an
      email in `plugin.xml` for now and set contact info via the Marketplace vendor profile instead.
      Revisit before/at upload time if you change your mind.

---

## Phase 2 — Compatibility & verification

### 2.1 IDE/version support
- [ ] Manually test at least: PhpStorm 2024.2, a current PhpStorm release, IntelliJ IDEA Community
      2024.2, a current IDEA Community release. (Not automatable from here — needs a human in an IDE.)

### 2.2 Build and manually test the exact ZIP
- [x] `./gradlew clean buildPlugin` run successfully this session.
- [x] Output confirmed at `build/distributions/FMCuttingBoard-1.0.5.zip`, contents verified clean.
- [x] Installed that exact ZIP into a clean IDE via **Settings → Plugins → gear icon → Install Plugin
      from Disk** and smoke-tested the menu items successfully (confirmed by you 2026-07-27).

### 2.3 Plugin Verifier
- [x] Added:
  ```kotlin
  intellijPlatform {
      pluginVerification {
          ides { recommended() }
      }
  }
  ```
- [x] Bumped IntelliJ Platform Gradle Plugin to **2.18.1** (confirmed current via Gradle Plugin
      Portal at the time of this session).
- [x] Ran `./gradlew verifyPlugin` this session — it caught two real, previously-hidden bugs:
      1. `untilBuild.set("")` in `patchPluginXml` produced `<idea-version ... until-build="" />`,
         which the current (2.18.1-bundled) Plugin Verifier rejects outright as an invalid value —
         older tooling silently tolerated it. **Fixed** by simply never calling `untilBuild.set(...)`
         (omitting the attribute is the correct way to leave the upper bound uncapped).
      2. With that fixed, the verifier could actually run and reported the plugin **Compatible**
         against IC-2024.3, IC-2025.1, and IC-2025.2 — but failed the build on an
         `OVERRIDE_ONLY_API_USAGES` violation: `GetFileMakerClipboardContentAction` was calling
         `.actionPerformed(e)` directly on two other `AnAction` subclasses
         (`ReadClipboardIntoNewXmlFileAction`, `GetFileMakerCalculationFromClipboardAction`).
         `actionPerformed` is `@ApiStatus.OverrideOnly` — it's meant to be invoked only by the
         platform's action system, never called directly by other code. **Fixed** by extracting each
         action's logic into a plain `public void perform(AnActionEvent e)` method that
         `actionPerformed` now just delegates to, and pointing the caller at `perform(e)` instead.
         Also a minor deprecated-API cleanup opportunity noted but not required to pass: 7 deprecated
         API usages remain (folding/highlighter/formatter/notification APIs), none blocking.
      Re-ran `buildPlugin verifyPlugin` — **now BUILD SUCCESSFUL**, plugin reported Compatible
      against IC-2024.3, IC-2025.1, and IC-2025.2, with only 7 non-blocking deprecation warnings
      remaining (folding/highlighter/formatter/notification APIs — fine to leave for now, worth a
      follow-up pass later since some are "scheduled for removal").
- [x] Added a `verifyPlugin` step to CI (`.github/workflows/ci.yml`), running after `build`/`test`.

---

## Phase 3 — Signing

- [x] Added to `build.gradle.kts`:
  ```kotlin
  intellijPlatform {
      signing {
          certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
          privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
          password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
      }
      publishing {
          token.set(providers.environmentVariable("PUBLISH_TOKEN"))
      }
  }
  ```
- [x] Generated a self-signed key/cert this session (10-year validity, `CN=FMCuttingBoard,
      OU=Blue Kachina, O=Blue Kachina, C=US`) via `openssl`, written **only** to the local scratch
      directory outside the repo — never committed, never printed to chat.
- [x] Moved the generated cert/key/password into GitHub Actions secrets via `gh secret set` (flags
      must come *before* the secret name, e.g. `gh secret set --repo OWNER/REPO NAME < file` — `gh
      secret set NAME --repo ...` fails with "accepts at most 1 arg(s)" because `secret set` disables
      flag interspersion after the positional name). Confirmed via `gh secret list --repo
      Blue-Kachina/fmcuttingboard`:
      - `CERTIFICATE_CHAIN` ✓
      - `PRIVATE_KEY` ✓
      - `PRIVATE_KEY_PASSWORD` ✓

      Local scratch copies deleted afterward. Note: JetBrains will also sign automatically on their
      end if you never configure your own certificate — signing your own is only needed if you want
      update authenticity independent of Marketplace's own signing, or plan to distribute the ZIP
      outside Marketplace too.
- [x] Secrets are set — `./gradlew signPlugin` can now produce a signed artifact locally.
- [x] Added a `signPlugin` step to `.github/workflows/release.yml`, running after `verifyPlugin` and
      before the changelog/release steps, with `CERTIFICATE_CHAIN`/`PRIVATE_KEY`/
      `PRIVATE_KEY_PASSWORD` scoped as step-level env vars (not job-wide, to limit exposure). Per the
      task's own source (`SignPluginTask`), it takes `FMCuttingBoard-<version>.zip` and writes
      `FMCuttingBoard-<version>-signed.zip` next to it, and no-ops safely if signing credentials are
      ever absent. The `Create GitHub Release` step now attaches `build/distributions/*-signed.zip`
      specifically, so future tagged releases (1.0.6+) will publish the **signed** zip as the GitHub
      Release asset.

---

## Phase 4 — Vendor account & first upload

- [x] Created JetBrains Marketplace vendor account: vendor id `blue-kachina`.
- [x] Vendor profile finished (Developer Agreement, trader/non-trader status, contact info).
- [x] Chose **Upload plugin**, selected MIT license (Phase 1.1), provided the GitHub source-code URL,
      picked categories/tags, and uploaded `FMCuttingBoard-1.0.5.zip` manually.
- [~] **Submitted, awaiting JetBrains review** (expected within ~2 business days as of 2026-07-27).
      Listing: https://plugins.jetbrains.com/plugin/33170-fmcuttingboard
- [ ] No banking info needed — this is a free plugin (n/a once accepted).

---

## Phase 5 — Listing content

- [x] `docs/MarketplaceListing.md` has name, summary, description, use cases, features, vendor info.
- [x] Privacy statement now explicitly states no telemetry/analytics, in addition to no network access.
- [x] Real screenshot added: `docs/screenshots/FmCuttingBoardScreenie.png`, covering both the
      `Tools → FMCuttingBoard` menu and a generated FileMaker XML file (with the "FileMaker XML
      Detected" editor notification banner) in one shot. Wired into `README.md` and
      `docs/MarketplaceListing.md`, replacing the old placeholder icon embed. The plugin icon itself
      is still visible small in the menu screenshot and is now only *mentioned* in text (it's used as
      the Tools menu item's icon), not displayed large/standalone anymore.
- [x] Second screenshot added: `docs/screenshots/FmCuttingBoard_fmcalc.png`, showing FileMaker
      calculation-language syntax highlighting in a `.fmcalc` file. Wired into `README.md`;
      `docs/MarketplaceListing.md` already references it. Both planned screenshots are now real.
- [ ] Confirm which tags exist in the live Marketplace upload form before picking any (Languages,
      Tools, Productivity, XML, etc. — don't assume all are offered).

---

## Phase 6 — Clipboard & privacy disclosure

- [x] README now has an explicit **Privacy** section: no network calls, no telemetry, clipboard +
      project-local files only.
- [x] `docs/MarketplaceListing.md` privacy statement expanded to match.
- [ ] Confirm in the actual Marketplace listing form: Windows fully supported, macOS supported,
      Linux not supported (README already documents this — just carry it into the form).

---

## Phase 7 — CI/CD

- [x] `.github/workflows/ci.yml` JDK bumped from 17 → 21 to match the project's actual toolchain
      target (`javaVersion=21` in `gradle.properties`).
- [x] Added a `verifyPlugin` CI step after `build`/`test`.
- [x] Added `.github/workflows/release.yml`: on `v*` tag push, verifies the tag matches
      `pluginVersion` in `gradle.properties`, runs tests + `buildPlugin` + `verifyPlugin`, extracts
      the matching `CHANGELOG.md` section, and creates a GitHub Release with the ZIP attached.
- [x] **Proven working end-to-end**, not just configured: pushing the real `v1.0.5` tag triggered a
      successful `Release` run, and the resulting
      [GitHub Release](https://github.com/Blue-Kachina/fmcuttingboard/releases/tag/v1.0.5) has
      `FMCuttingBoard-1.0.5.zip` attached. (An earlier run against `611f80e` failed on a CRLF-induced
      bash string-comparison bug in the tag-vs-`gradle.properties` check — fixed, then the whole repo
      was normalized to LF via `.gitattributes` to prevent the same class of bug elsewhere.)
- [x] Added `signPlugin` to `release.yml` (see Phase 3) — tagged releases now produce and attach a
      signed zip automatically. **Not yet added: `publishPlugin`.** A GitHub Release existing does
      **not** cause anything to appear on JetBrains Marketplace — Marketplace has no awareness of this
      repo's tags or releases at all. Until `publishPlugin` is wired up (Phase 9), every new version
      still has to be uploaded to Marketplace by hand, the same way 1.0.5 was.

---

## Phase 8 — Submit & respond to review

- [x] Submitted `1.0.5` for review 2026-07-27: https://plugins.jetbrains.com/plugin/33170-fmcuttingboard
- [ ] Awaiting JetBrains review (expected ~2 business days).
- [ ] Be ready to address: metadata, license info, compatibility declarations, internal API usage,
      listing copy, screenshots/icon, privacy disclosures.
- [ ] If no response after several working days, contact Marketplace support.

---

## Phase 9 — Future release automation (post first-acceptance)

**How Marketplace actually finds out about new versions:** it doesn't poll GitHub, watch tags, or
know this repo exists beyond the source-code URL shown on the listing page. A new version only
appears on Marketplace when someone (you, or `publishPlugin`) calls the Marketplace upload API with a
valid `PUBLISH_TOKEN`. Pushing a `v1.0.6` tag today would run `release.yml` (tests → build → verify →
sign → GitHub Release) and stop there — nothing would reach Marketplace until you either upload the
new zip by hand on the plugin's Marketplace page, or `publishPlugin` is added to the workflow.

- [ ] Generate a **permanent Marketplace token** (plugins.jetbrains.com → your account → *My Tokens*)
      once `1.0.5` is accepted, and add it as a GitHub secret the same way as the signing material:
      `gh secret set --repo Blue-Kachina/fmcuttingboard PUBLISH_TOKEN < token.txt`
      (already wired in `build.gradle.kts`'s `publishing { token.set(...) }` — just needs the secret
      to exist).
- [ ] Add a `publishPlugin` step to `release.yml` (after `signPlugin`), so pushing a version tag both
      creates the GitHub Release **and** uploads that version to Marketplace automatically. Until this
      exists, treat "tag pushed" and "Marketplace updated" as two independent, manual-in-the-middle
      steps.
- [ ] Typical pipeline once wired: bump `pluginVersion` in `gradle.properties` → add a matching
      `## [x.y.z] - date` section to `CHANGELOG.md` (the changelog plugin picks it up automatically
      via `patchPluginXml`) → tag and push → CI runs tests → `verifyPlugin` → `buildPlugin` →
      `signPlugin` → `publishPlugin`.
- [ ] Consider publishing to Marketplace's "beta"/test channel first before promoting to stable,
      once this is a routine you trust.

---

## Consolidated remaining checklist

- [x] `verifyPlugin` passes clean (Compatible against IC-2024.3/2025.1/2025.2; 2 real bugs found and fixed along the way — see Phase 2.3).
- [x] `v1.0.5` tagged, pushed, and the `release.yml` pipeline confirmed working: GitHub Release live with `FMCuttingBoard-1.0.5.zip` attached.
- [x] Manually installed the built ZIP in a clean IDE and smoke-tested the golden path — success.
- [x] Created the JetBrains vendor account (`blue-kachina`) and finished the vendor profile.
- [x] Both planned screenshots added and wired into README + Marketplace listing doc.
- [x] Signing cert/key/password moved into GitHub Actions secrets (`CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`), local copies deleted.
- [x] `signPlugin` added to `release.yml` — future tagged releases produce and attach a signed zip automatically.
- [x] Uploaded `1.0.5` to Marketplace and submitted for review: https://plugins.jetbrains.com/plugin/33170-fmcuttingboard
- [ ] Awaiting JetBrains review (~2 business days as of 2026-07-27).
- [ ] **Not yet automated:** Marketplace won't pick up new versions on its own. Each future version (starting with 1.0.6) still needs a manual "Upload plugin" on the Marketplace page — *or* a `publishPlugin` CI step wired to a `PUBLISH_TOKEN` secret (see Phase 9) — before it appears there. Tagging/pushing alone only updates GitHub.
- [ ] (Optional, can happen later) Decide on and add a vendor email (in `plugin.xml` and/or the Marketplace vendor profile).
- [ ] (Optional, can happen later) Test against at least one more recent IDE build beyond 2024.2/2024.3.
