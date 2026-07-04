---
version: alpha
name: SmartMentor-design-analysis
description: "SmartMentor's dark product interface design system, aligned with the restrained modern language of top developer products like Linear / Vercel / Cursor. The canvas is a near-black #08090C with a faint cool cast — not pure black, not cyberpunk neon. On top sits a four-step charcoal surface ladder (surface-1 to surface-4); depth comes from surface lift + 1px hairline borders + top inner edge-highlights, almost never drop shadows. The single accent is a restrained indigo #6A75E8 — only on the brand mark, primary CTA, focus ring, and key links, never decorative. Display type is sans-serif (Inter / Geist substitute) at weight 500–600 with aggressive negative tracking (-3px @ 64px); editorial hierarchy comes from size and tracking, never serif or 700+. Frosted glass (backdrop-blur) is reserved for the top nav and overlays. The whole thing reads like a precise, quiet, crafted software-engineering document — not a colorful learning app. v1 (white editorial) and v2 (neon cyberpunk) are deprecated."

colors:
  # —— Accent (single, highly restrained) ——
  primary: "#6A75E8"          # Restrained indigo: brand mark, primary CTA, focus ring, key links
  primary-hover: "#828CF0"
  primary-active: "#5560D6"
  on-primary: "#ffffff"
  primary-wash: "rgba(106,117,232,0.12)"   # Faint fill: selected state, focus ring
  # —— Text (light, four steps) ——
  ink: "#F4F5F7"              # Primary text / large headings
  ink-muted: "#C2C6D0"        # Secondary body
  ink-subtle: "#8A8F9C"       # Descriptions / meta / deselected
  ink-tertiary: "#5C616E"     # Placeholder / disabled / footnotes
  # —— Canvas & surfaces (dark ladder, layer via lift) ——
  canvas: "#08090C"           # Site canvas: near-black with a faint cool cast
  surface-1: "#0E0F13"        # Level 1: cards, panels
  surface-2: "#14161B"        # Level 2: hover cards, emphasis cards
  surface-3: "#1A1C22"        # Level 3: dropdowns, sub-nav, inputs
  surface-4: "#202329"        # Level 4: nested region of top overlay
  scrim: "rgba(8,9,12,0.72)"  # Frosted scrim base (top nav / modal backdrop, with backdrop-blur)
  # —— Hairline borders (via highlight, not gray lines) ——
  hairline: "rgba(255,255,255,0.07)"        # Standard 1px border: cards, dividers
  hairline-strong: "rgba(255,255,255,0.12)" # Hover / emphasis border
  edge-highlight: "rgba(255,255,255,0.06)"  # Top inner highlight: surface reflection texture
  focus-ring: "rgba(106,117,232,0.55)"      # Focus outline
  # —— Data viz (restrained trio, charts/status only, never decorative) ——
  viz-1: "#6A75E8"            # Primary: indigo (same as primary)
  viz-2: "#4FB8A4"            # Secondary: restrained teal (2nd data series)
  viz-3: "#7C82A0"            # Neutral gray-blue (3rd series / grid)
  # —— Semantic (dark-adapted, low saturation) ——
  success: "#3FB573"          # Correct / mastered
  caution: "#D4A24E"          # Caution / needs work
  critical: "#E5645E"         # Error / weak (coral, not bright red)
  success-wash: "rgba(63,181,115,0.12)"
  caution-wash: "rgba(212,162,78,0.12)"
  critical-wash: "rgba(229,100,94,0.12)"

typography:
  display-xl:
    fontFamily: "Geist, Inter, -apple-system, system-ui, sans-serif"
    fontSize: 64px
    fontWeight: 600
    lineHeight: 1.05
    letterSpacing: -3.0px
  display-lg:
    fontFamily: "Geist, Inter, sans-serif"
    fontSize: 44px
    fontWeight: 600
    lineHeight: 1.1
    letterSpacing: -1.8px
  display-md:
    fontFamily: "Geist, Inter, sans-serif"
    fontSize: 32px
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: -1.0px
  headline:
    fontFamily: "Geist, Inter, sans-serif"
    fontSize: 24px
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: -0.6px
  card-title:
    fontFamily: "Geist, Inter, sans-serif"
    fontSize: 18px
    fontWeight: 500
    lineHeight: 1.3
    letterSpacing: -0.3px
  subhead:
    fontFamily: "Geist, Inter, sans-serif"
    fontSize: 18px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: -0.1px
  body:
    fontFamily: "Inter, -apple-system, sans-serif"
    fontSize: 15px
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: -0.05px
  body-sm:
    fontFamily: "Inter, sans-serif"
    fontSize: 13px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0
  caption:
    fontFamily: "Inter, sans-serif"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.4
    letterSpacing: 0
  eyebrow:
    fontFamily: "Inter, sans-serif"
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.3
    letterSpacing: 0.6px
    textTransform: uppercase
  button:
    fontFamily: "Inter, sans-serif"
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.0
    letterSpacing: 0
  mono:
    fontFamily: "Geist Mono, JetBrains Mono, ui-monospace, SFMono-Regular, monospace"
    fontSize: 13px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0

rounded:
  xs: 4px
  sm: 6px
  md: 8px
  lg: 12px
  xl: 16px
  xxl: 24px
  pill: 9999px
  full: 9999px

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  section: 96px

elevation:
  flat: "none"
  hairline: "inset 0 0 0 1px {colors.hairline}"
  edge: "inset 0 1px 0 {colors.edge-highlight}, inset 0 0 0 1px {colors.hairline}"
  raised: "0 1px 2px rgba(0,0,0,0.3), 0 4px 12px rgba(0,0,0,0.25), inset 0 1px 0 {colors.edge-highlight}"
  floating: "0 8px 24px rgba(0,0,0,0.4), 0 2px 6px rgba(0,0,0,0.3), inset 0 1px 0 {colors.edge-highlight}"
  blur: "backdrop-filter: blur(16px) saturate(140%)"
  focus: "0 0 0 3px {colors.primary-wash}, 0 0 0 1px {colors.focus-ring}"

motion:
  duration-fast: "120ms"
  duration-base: "200ms"
  duration-slow: "360ms"
  easing: "cubic-bezier(0.22, 1, 0.36, 1)"

components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: "10px 18px"
  button-secondary:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink}"
    border: "1px solid {colors.hairline-strong}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: "10px 18px"
  button-ghost:
    backgroundColor: transparent
    textColor: "{colors.ink-muted}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
  card:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "{spacing.lg}"
    shadow: "{elevation.edge}"
  card-hover:
    backgroundColor: "{colors.surface-2}"
    shadow: "{elevation.raised}"
  card-glass:
    backgroundColor: "{colors.scrim}"
    backdrop: "{elevation.blur}"
    border: "1px solid {colors.hairline}"
    rounded: "{rounded.xl}"
  input:
    backgroundColor: "{colors.surface-3}"
    textColor: "{colors.ink}"
    border: "1px solid {colors.hairline}"
    rounded: "{rounded.md}"
    padding: "10px 14px"
    focus: "{elevation.focus}"
  badge:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink-muted}"
    typography: "{typography.caption}"
    border: "1px solid {colors.hairline}"
    rounded: "{rounded.pill}"
    padding: "2px 10px"
  top-nav:
    backgroundColor: "{colors.scrim}"
    backdrop: "{elevation.blur}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    height: 56px
  sidebar-item-active:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink}"
    indicator: "2px {colors.primary} left-edge"
illustration:
  style: "minimal-line-on-dark"
  palette: ["ink-subtle", "primary", "viz-2", "hairline"]
  background: "transparent / canvas"
---

> This file follows Google Stitch's open `DESIGN.md` format: the top YAML holds machine-readable tokens ("what"), the Markdown below holds rationale and per-page specs ("why"). Any AI coding agent should **strictly follow this file**, reference `{tokens}` rather than hardcoding hex, and validate color, contrast, and component choices against it. Chinese version: `DESIGN.md`.
>
> **v1 (white academic editorial) and v2 (neon cyberpunk glow) are both deprecated.** v1 was washed-out, floating cards, low contrast, stock photos — a dated admin-template look; v2 was dual-neon glow + aurora gradients — a Y2K cyberpunk look. This v3 aligns with the restrained modern language of Linear / Vercel / Cursor.

## Overview

SmartMentor is a personalized learning system built on multi-agent LLM collaboration. Its core idea is **"traceback learning"** — walking back up the knowledge-graph dependency chain to locate the true root-cause weakness, then generating a learning path bottom-up.

The interface is aligned with **top developer products like Linear / Vercel / Cursor**: precise, quiet, crafted. Modern premium feel doesn't come from a filter (neither retro serif nor neon glow) but from the exact execution of these things:

1. **Near-black canvas + four-step charcoal surface ladder.** `{colors.canvas}` (#08090C) is a near-black with a faint cool cast — not pure black `#000`, not cyber-dark-blue. On top, `{colors.surface-1}` → `{colors.surface-4}` lift step by step; **hierarchy comes from surface brightness difference + 1px hairline borders, almost never drop shadows.**
2. **A single restrained accent.** Indigo `{colors.primary}` (#6A75E8) appears only on the brand mark, primary CTA, focus ring, and key links — **never decorative**. Its area on any screen should be ≤ 5% of the visual field. No second neon color, no aurora gradient.
3. **Sans-serif display with aggressive negative tracking.** `{typography.display-xl}` uses **-3px tracking** at 64px, weight 600 (never 700+). Modern editorial feel comes from size jumps + negative tracking, not serif or bold.
4. **Real materials: frosted glass + edge highlight.** The top nav and overlays use `backdrop-blur(16px)` frosted glass; every raised surface gets a top `{colors.edge-highlight}` inner highlight, creating a "pixel-rendered" reflective edge — the key detail of Linear's dark texture.
5. **Stacked micro-shadows, never a single heavy drop.** `{elevation.raised}` is built from several small offsets + an inner highlight — restrained and natural, not Material's harsh big shadow.

**Mood keywords:** precise, restrained, quiet, engineered, trustworthy.
**Anti-patterns (must avoid):** white admin template, neon glow, aurora gradient, dual fighting accents, retro serif, photo backgrounds, traffic-light tri-color, 700+ heavy display, single heavy drop shadow.

The system serves two roles (mainly students, some teachers) across 14 pages, sharing one token set. Any new page or component must be expressible within these tokens.

## Colors

Palette: **near-black canvas + four-step charcoal ladder + four light text steps + a single indigo accent.** Data viz has a restrained trio; semantic colors are low-saturation.

### Accent · single
- **Indigo `{colors.primary}` (#6A75E8)**: brand mark, primary CTA background, focus ring, key link emphasis. This is the brand voice, **highly restrained** — ≤ 5% of any screen. Hover lifts to `{colors.primary-hover}`, press darkens to `{colors.primary-active}`. `{colors.primary-wash}` (12% faint) for selected states and focus-ring fill.
- There is **no second accent** site-wide. When color differentiation is needed (charts, status), use the data-viz trio or semantic colors below — never invent another brand color.

### Canvas & surfaces · dark ladder
Hierarchy comes from **surface lift**, not shadow:
- `{colors.canvas}` (#08090C) — site canvas, near-black with a faint cool cast. **Never pure black `#000`** (pure black looks flat and cheap in dark UI).
- `{colors.surface-1}` (#0E0F13) — default base for cards and panels, one step up from canvas.
- `{colors.surface-2}` (#14161B) — hover cards, emphasis cards, secondary buttons.
- `{colors.surface-3}` (#1A1C22) — dropdowns, sub-nav, input backgrounds.
- `{colors.surface-4}` (#202329) — nested region in the highest overlay.
- `{colors.scrim}` — frosted scrim base, with `backdrop-blur` for the top nav and modal backdrops.

### Text · four light steps
Hierarchy through brightness, **not color**: `{colors.ink}` (#F4F5F7, titles/body) → `{colors.ink-muted}` (#C2C6D0, secondary body) → `{colors.ink-subtle}` (#8A8F9C, descriptions/meta) → `{colors.ink-tertiary}` (#5C616E, placeholder/disabled). On dark, body in `{colors.ink-muted}` already meets AA; `{colors.ink-tertiary}` only for non-critical info. **Never** use grays darker than `{colors.ink-subtle}` for body text.

### Hairline borders · highlight, not gray lines
On dark, borders are **semi-transparent white highlights**, not solid gray lines:
- `{colors.hairline}` (white 7%) — standard 1px border on cards and dividers.
- `{colors.hairline-strong}` (white 12%) — hover/emphasis borders, secondary-button stroke.
- `{colors.edge-highlight}` (white 6%) — an inner highlight at the **top** of a surface (`inset 0 1px 0`), creating reflective texture. This is the core detail of premium dark UI.

### Data viz · restrained trio
When charts/status need multiple series, use `{colors.viz-1}` (indigo) / `{colors.viz-2}` (restrained teal #4FB8A4) / `{colors.viz-3}` (neutral gray-blue #7C82A0). For continuous values (mastery/accuracy) prefer an **indigo single-hue brightness gradient**. **No** rainbow multicolor or 4-step threshold jumps.

### Semantic · dark-adapted
`{colors.success}` (#3FB573 mastered), `{colors.caution}` (#D4A24E needs work), `{colors.critical}` (#E5645E weak, coral not bright red). Each has a `*-wash` faint fill. Semantic colors are for status only, **never decorative**.

### Hard constraints
- **No pure black `#000` canvas**, **no large white backgrounds**, **no stock photo backgrounds**.
- No high-saturation colors outside the tokens (purple `#667eea`, pink `#fb7299`, amber `#f59e0b`, bright blue `#3b82f6`, neon cyan `#34E0C0`, etc.).
- **No aurora / cross-hue gradients.** Gradients only allowed as same-hue brightness (e.g. `surface-1 → surface-2`).
- Text/background must meet WCAG AA (body 4.5:1, large text 3:1).

## Typography

**Single sans-serif family + aggressive negative tracking + size jumps build hierarchy. Never serif, never 700+.**

### Font Family
- **Geist / Inter (sans)** — carries everything narrative from display to caption. Display uses Geist (or Inter substitute) at weight 500–600.
- **Geist Mono / JetBrains Mono** — only for code, formulas, technical labels, data IDs.
- This is the standard developer-product pairing. SmartMentor has no proprietary font; Inter alone reaches the same look as Linear/Vercel.

### Hierarchy

| Token | Size | Weight | Line Height | Tracking | Use |
|---|---|---|---|---|---|
| `{typography.display-xl}` | 64px | 600 | 1.05 | -3.0px | Hero headline |
| `{typography.display-lg}` | 44px | 600 | 1.1 | -1.8px | Section headlines |
| `{typography.display-md}` | 32px | 600 | 1.15 | -1.0px | Page h1 / sub-section titles |
| `{typography.headline}` | 24px | 600 | 1.2 | -0.6px | Card-group titles, module titles |
| `{typography.card-title}` | 18px | 500 | 1.3 | -0.3px | Card titles |
| `{typography.subhead}` | 18px | 400 | 1.5 | -0.1px | Lead paragraphs, hero subhead |
| `{typography.body}` | 15px | 400 | 1.55 | -0.05px | Default body |
| `{typography.body-sm}` | 13px | 400 | 1.5 | 0 | Card body, secondary info |
| `{typography.caption}` | 12px | 400 | 1.4 | 0 | Captions, meta, badges |
| `{typography.eyebrow}` | 12px | 500 | 1.3 | +0.6px | Section eyebrow (uppercase + positive tracking) |
| `{typography.button}` | 14px | 500 | 1.0 | 0 | Button labels |
| `{typography.mono}` | 13px | 400 | 1.5 | 0 | Code / formula / data ID |

### Principles
- **Negative tracking is the soul of the modern feel.** Display tracking scales from -3px (64px) down to -0.05px at body. Remove it and headlines instantly look dated — a hidden reason v1/v2 failed.
- **Weight ceiling 600.** Display never 700/800. Heavy bold headlines are the "trying-too-hard" cheap signal; restrained 500–600 reads premium.
- **Eyebrow uses positive tracking + uppercase.** `+0.6px` + uppercase contrasts the negative-tracked display, marking taxonomy — replaces colored badges.
- **Numbers (accuracy/mastery)** may use display size + `{colors.primary}` as a focal point, once per screen.
- **Mono only in code/technical contexts**, never for body.

## Layout

**Large whitespace + surface-ladder layering is where the modern feel comes from; dense rows are where the cheap feel comes from.**

### Spacing
- Base 4px. Tokens: `{spacing.xxs}` 4 · `{spacing.xs}` 8 · `{spacing.sm}` 12 · `{spacing.md}` 16 · `{spacing.lg}` 24 · `{spacing.xl}` 32 · `{spacing.xxl}` 48 · `{spacing.section}` 96.
- **Section gap `{spacing.section}` 96px** (landing/marketing), minimum 48px inside the app. The 14–22px compact gaps in the v1 screenshot are the chief "info overload" cause — loosen them.
- **Card padding `{spacing.lg}` 24px** (key cards 32px); card gap 16–24px.

### Grid & Container
- Max content width **1200px** (reading-heavy Lesson/Report up to 1280px), centered.
- App shell: a dark narrow left sidebar (`{colors.canvas}` or `{colors.surface-1}`) + main area (`{colors.canvas}`). Sidebar active item = `{colors.surface-2}` + a 2px `{colors.primary}` left-edge indicator + `{colors.ink}` text.
- Top nav 56px, frosted glass (`{colors.scrim}` + blur), 1px `{colors.hairline}` bottom separator, no shadow.
- Dense pages (Dashboard/Profile) use main + side column + collapsible panels, never stuffing icon+text across one row.

### Whitespace Philosophy
**The dark canvas IS the whitespace.** Sections separate by lifting onto surface-1 panels, not white gaps. 24px block gaps inside panels, 96px between sections.

## Elevation & Depth

**On dark, layer via "surface lift + hairline border + top inner highlight"; shadows are very light and stacked.**

| Level | Treatment | Use |
|---|---|---|
| 0 Flat | No border, no shadow | Body, hero text, canvas |
| 1 Hairline | `{elevation.hairline}` (inset 1px border) | Dividers, light containers |
| 2 Surface | `{colors.surface-1}` + `{elevation.edge}` (top inner highlight + border) | Default cards, panels |
| 3 Raised | `{colors.surface-2}` + `{elevation.raised}` (stacked micro-shadow + inner highlight) | Hover cards, key cards |
| 4 Floating | `{colors.surface-3/4}` + `{elevation.floating}` | Dropdowns, modals, popovers |
| Focus | `{elevation.focus}` (3px primary-wash + 1px focus-ring) | Focused input/button |

**Key texture details:**
- **Top inner highlight `{colors.edge-highlight}`** — a faint white `inset 0 1px 0` at the top of every raised surface, simulating light hitting the surface edge from above. This is Linear's signature dark texture — **always add it.**
- **Frosted glass `{elevation.blur}`** — `backdrop-blur(16px) saturate(140%)`, only for the top nav, modal backdrops, floating CTA bars. This is the "blur/reflection" modern feature you asked for.
- **Stacked shadows** — `{elevation.raised}` is 1px + 4px + 12px layered, never a single 8px drop.
- **Forbidden**: colored shadows, neon glow, single heavy drop shadow, pure-black dead edges.

## Shapes

| Token | Value | Use |
|---|---|---|
| `{rounded.xs}` | 4px | Badges, status dots |
| `{rounded.sm}` | 6px | Inline tags, small elements |
| `{rounded.md}` | 8px | Buttons, inputs |
| `{rounded.lg}` | 12px | Default cards, panels |
| `{rounded.xl}` | 16px | Hero cards, product screenshot frames |
| `{rounded.xxl}` | 24px | Oversized CTA banner (rare) |
| `{rounded.pill}` | 9999px | Capsule badges, status pills, avatar circles |

- Default cards `{rounded.lg}` 12px — one step tighter than v2's 16px, closer to Linear's precision.
- Buttons/inputs `{rounded.md}` 8px (**no full-pill buttons** — that's marketing-page; an app console uses 8px square corners, more professional).
- **Avatars**: circle or 8px-rounded square both fine; circles aren't childish on dark — as long as you drop the colored gradient fill and use `{colors.surface-2}` base + 1px border + single-color monogram or photo.
- Prefer 1px `{colors.hairline}` for separation, not solid blocks.

## Components

### General component language

- **`button-primary`** — the single indigo solid CTA. `{colors.primary}` + white text + `{rounded.md}` 8px + `{typography.button}`. Hover `{colors.primary-hover}`, press `{colors.primary-active}`. Only this one high-emphasis button site-wide, **used scarcely.**
- **`button-secondary`** — `{colors.surface-2}` + `{colors.ink}` + 1px `{colors.hairline-strong}` border. Secondary action.
- **`button-ghost`** — transparent + `{colors.ink-muted}`, hover to `{colors.ink}`. Tertiary text action.
- **`card`** — `{colors.surface-1}` + `{elevation.edge}` (top inner highlight + hairline) + `{rounded.lg}`. Hover lifts to `{colors.surface-2}` + `{elevation.raised}` (shift ≤2px, no flicker).
- **`card-glass`** — `{colors.scrim}` + `{elevation.blur}` frosted, for hero data cards and floating bars.
- **`badge`** — `{colors.surface-2}` + 1px `{colors.hairline}` + `{typography.caption}`. Status colors only via semantic `*-wash` fill + matching bright text. **No rainbow badges.**
- **`input`** — `{colors.surface-3}` + 1px `{colors.hairline}` + `{rounded.md}`; focus `{elevation.focus}` (indigo focus ring). **No inline styles.**
- **Progress bars** — `{colors.surface-3}` track, `{colors.primary}` single-color fill (or indigo brightness gradient), no glow, no gold gradient.
- **Charts** — use `{colors.viz-1/2/3}` or an indigo brightness gradient; grid lines `{colors.hairline}`. **No** rainbow or threshold jumps.
- **Empty states** — minimal line illustration (see below) + title + encouraging copy, replacing bare "no data".
- **Loading** — `{colors.surface-1}`→`{colors.surface-2}` faint breathing skeleton, or a `{colors.primary}` thin ring.

### Per-page specs

> Each page: positioning → "eyesores" to remove → redesigned form. All pages share the tokens above. **Note: Landing/Auth below were visually reviewed via real screenshots; the other 12 pages are based on source-code analysis — fine-tune against real renders during implementation.**

**1. Landing (public)** — *visually reviewed*
Remove: white bg, serif, leather-desk stock photo, gold everywhere, high-saturation floating math symbols, 3D card swap.
Redesign: `{colors.canvas}` dark bg; hero uses `{typography.display-xl}` (64px/-3px), keyword may be tinted `{colors.primary}` (not a gradient); subhead `{typography.subhead}` `{colors.ink-muted}`; primary `button-primary` + secondary `button-secondary`; knowledge-graph SVG nodes as `{colors.surface-2}` circles + thin `{colors.primary}`/`{colors.viz-2}` strokes (no glow), edges `{colors.hairline}`; "core capabilities" to a `card` grid with indigo data; floating symbols to faint `{typography.mono}` or removed.

**2. Auth (public)** — *visually reviewed*
Remove: white bg, inline styles, flat borderless inputs, generic `btn-dark`, isolated bare title.
Redesign: dark bg, centered `card-glass` frosted card (use blur/reflection here); title `{typography.display-md}`; inputs follow `input` spec (indigo focus ring); primary `button-primary`; login/register via segmented control; countdown as a `badge`; a faint knowledge-graph hairline texture may sit behind the card (no glow).

**3. Dashboard (student)**
Remove: white bg, multicolor dimension-card fills, circular gradient monogram avatar, bottom-bar card animation, gold-gradient progress bar, colored node cloud, colored-square legend.
Redesign: narrow left sidebar + main area; dimension cards unify to `card` + a 2px `{colors.primary}` left-edge indicator + numbers in `{typography.display-md}`; avatar to `{colors.surface-2}` base + 1px border + single-color monogram (no gradient); identity row split into two lines, `{colors.ink-subtle}` icons; path progress bar single-color indigo; knowledge nodes unify to `badge`, mastery distinguished by semantic stroke color; legend to single-color dots; main + side column, details collapsed; empty states get minimal line illustration.

**4. Diagnostic (student)**
Remove: white bg, over-saturated Tailwind module cards (amber/blue/green), heavy-glow progress dots, contrast-bg feedback cards.
Redesign: three subjects as `card`, selected state = `{colors.surface-2}` + 1px `{colors.primary}` border + left-edge indicator (no longer one color each, keep single accent); progress dots `{colors.primary}` solid + `{colors.hairline}` connectors; feedback cards `success-wash`/`critical-wash` + bright text; options in `input`-style blocks, selected with indigo border; module-card hover lifts surface + brightens border.

**5. DiagnosticHistory (student)**
Remove: white bg, blue/gold/green colored stat badges, icon pile-up per column, tri-color acc/mastery coding, mishmash buttons.
Redesign: stats as `card` + numbers `{typography.display-md}` (indigo/neutral alternating); table on `{colors.surface-1}`, rows lift to surface-2 on hover, icons only on date/module; levels via single-color brightness + weight; badges reduced to `badge` fill/outline; CTA `button-primary`, detail as indigo text link; gaps loosened.

**6. DiagnosticResult (student)**
Remove: dark-gray hero block, flashy dual-donut, amber-heavy AI card, cheap frosted bar.
Redesign: hero to `card` / `card-glass`, left `{typography.display-lg}` title "Diagnostic Score" + right oversized number (accuracy, `{colors.primary}`); donut to an indigo single-color ring or soft bar; question cards `card` + generous whitespace + indigo knowledge `badge`; AI advice card `{colors.surface-1}` + 1px border (drop amber); weak-point card left-edge in indigo brightness; sticky bar `card-glass` frosted + `button-primary`.

**7. TracingResult (student)**
Remove: orange radial glow, pure-red priority circles, chaotic multicolor badges, fully-different stage colors.
Redesign: hero `card-glass`, de-glowed; priority circles to `{colors.primary}`/semantic soft circles + bright text; badges unified to `badge` tri-state; path stages in indigo brightness light/dark; **the knowledge graph is the star** — nodes as `card`-style circles + indigo strokes, edges `{colors.hairline}`, root-cause node emphasized with a thicker `{colors.primary}` stroke (not glow); mastery bar single-color indigo.

**8. LearningPaths (student)**
Remove: white bg, cartoonish PaperBird, over-saturated green chips, vivid empty-state circle, green-gradient featured card, decorative shift animations.
Redesign: title `{typography.display-md}`; chips to `badge`; empty-state circle to `{colors.surface-2}`; featured card `card` + indigo left-edge indicator + large progress ring (single-color indigo); progress indigo; card padding 32px; hover only lifts surface + brightens border; PaperBird replaced by a minimal geometric line bird or removed.

**9. LearningPathDetail (student)**
Remove: white bg, circular-avatar-like large donut, bright blue/amber, tiny scattered stats, playful timeline dots, over-strong shadows.
Redesign: title sans display; donut to indigo single-color ring/gauge or a large number; palette dark + indigo; stats consolidated into an elegant horizontal data strip; timeline to `{colors.hairline}` thin line + `{colors.primary}` node dots (done solid, locked `{colors.ink-tertiary}`); chips `badge`; keep the knowledge-graph viz inside a `card`; CTA `button-primary`.

**10. Lesson (student)**
Remove: white bg, 3D flip + light sweep on resource cards, colored level-less badges, harsh shimmer, oversized option dots, high-contrast chat bubbles, indigo spinner.
Redesign: dark main area + a right AI-companion `card-glass` frosted sidebar; resource cards `card`, hover lifts surface + indigo left-edge indicator (no flip, no sweep); badges unified to `badge`; skeleton faint breathing; options `input`-style blocks, selected with indigo border; AI bubbles: user `{colors.surface-2}` block, AI `{colors.surface-1}` + 1px border, distinguished left/right; spinner `{colors.primary}`; grid gaps enlarged.

**11. Chat (student)**
Remove: white bg, blue-purple gradient avatar, saturated-blue bubbles, suggestion buttons same color as bg, deep-blue code block, pink resource icons, blue-purple cursor, too-pale text.
Redesign: dark left sidebar session list (`{colors.surface-1}`) + main chat on `{colors.canvas}`; AI avatar `{colors.surface-2}` circle + single-color glyph (no gradient); user bubbles `{colors.surface-2}`, AI bubbles `{colors.surface-1}` + 1px border; suggestion buttons `card` + hover lift; code block `{colors.surface-3}` + low-saturation syntax highlight; welcome area `{typography.display-lg}` title; resource icons `{colors.primary}`/`{colors.viz-2}`; streaming cursor `{colors.primary}`; timestamps `{colors.ink-subtle}`; bottom hint `{colors.ink-subtle}` for readability.

**12. Profile (student)**
Remove: white bg, pure-black active tab, cold-white main bg, green→gold progress gradient, standard mishmash save button.
Redesign: dark bg + left tab sidebar, active uses `sidebar-item-active` (surface-2 + indigo left-edge); "Settings" title `{typography.display-md}`; progress bar single-color indigo; forms follow `input` spec with indigo focus ring; save button `button-primary`; subject-profile cards `card` + indigo data; empty states get illustration.

**13. ProfileOnboarding (student)**
Remove: white bg, purple gradient avatar, blue bubbles, the "👋" emoji, sky-blue result card, over-bright tags, gradient backgrounds, blue focus border.
Redesign: dark centered conversation flow; AI avatar `{colors.surface-2}` single-color circle; opening line in modern wording, drop emoji; result card `card` + 1px border + success state `success-wash`; tags `badge`; bubbles `{colors.surface-2}`/`{colors.surface-1}` distinguished, `{rounded.lg}`; focus border indigo.

**14. Report (student)**
Remove: white bg, 4-step threshold donut color, gold hover border, 7rem cheap watermark, tri-color traffic-light bars, pale-gold chips, monotone pure-green bars, heavy 800-weight title, 18px large corners.
Redesign: dark Bento grid of `card`; donut/radar/trend unified to `{colors.viz-1/2/3}` or an indigo brightness gradient; hover border `{colors.hairline-strong}`; watermark shrunk to 2–3rem faint lines; error-elimination bars single-color indigo gradient + `badge` labels; chips `badge`; bars `{colors.surface-3}`→`{colors.primary}` progression; corners `{rounded.lg}` 12px (large cards 16px); h1 `{typography.display-md}` 600 + an `{typography.eyebrow}` above; empty states get illustration + copy ("Building knowledge graph", "Keep learning to unlock capabilities").

## Illustrations (prompts for Stitch)

Stitch can generate illustrations. **All illustrations must share one visual language.** Aligned with Linear/Vercel's minimal single-color line work — not neon glow (that was deprecated v2) — restrained, precise, geometric thin lines.

### Unified illustration style (always prepend)

> **Master style prompt (every illustration starts with this):**
> *"Minimal geometric line illustration on a near-black background (#08090C), thin precise strokes in light gray (#8A8F9C) with a single restrained indigo accent (#6A75E8) on one or two key elements only, NO glow, NO neon, NO gradient fills, flat 2D, generous negative space, calm engineered aesthetic like Linear or Vercel marketing, NOT cartoonish, NOT 3D, NOT cyberpunk, NOT photorealistic."*

Hard constraints: ① near-black/transparent bg only; ② subject is light-gray thin lines, **only one or two** indigo accents, no glow, no gradient; ③ generous whitespace; ④ motifs limited to "knowledge graph / node network / traceback path / dialogue / data", geometric and precise; ⑤ never cartoon, 3D, cyberpunk, or photo. Export transparent-bg SVG, 1.5px stroke.

### Motifs
- **Knowledge-graph node web** (thin-line nodes + edges, states in light-gray/indigo/dim-gray) — the core brand symbol;
- **Traceback path** (a thin line tracing upward from a node to an indigo root node) — echoes "traceback learning";
- **Data/graph + human silhouette**; **speech bubble + nodes** (AI companion).

### Per-scene prompts (each after the Master style prompt)
- **Landing hero**: *"a constellation-like knowledge graph of small nodes connected by thin gray lines, one node accented in indigo, abstract and airy, lots of dark space."*
- **Auth brand graphic**: *"a single open book whose pages resolve into a small network of connected nodes, thin gray lines, one indigo node, centered, minimal."*
- **Dashboard empty state (no weakness found)**: *"a silhouette figure tracing a thin line upward through a node graph toward one indigo root node, root-cause tracing, minimal lines on dark."*
- **Diagnostic/path empty state**: *"a winding thin dotted path connecting milestone nodes bottom to top, one node marked with a small indigo flag, gray lines on dark."*
- **Graph loading skeleton**: *"a faint skeletal node-and-edge network in dim gray lines, low opacity, a graph still forming, on dark."*
- **Report empty state**: *"a minimal radar/web diagram in thin gray lines, partially complete, one indigo axis, calm, on dark."*
- **Checkpoint passed**: *"a node graph with one root node circled by a thin indigo ring and a small checkmark, restrained, no confetti, on dark."*
- **AI chat avatar**: *"a small minimal node-cluster glyph or monogram in light gray with one indigo dot, flat, on transparent dark, no glow."*

Pairing copy: empty/loading copy uses a title + encouraging line ("Analyzing your learning trajectory…", "Finish one diagnostic and I'll plan your knowledge route"), never a bare "no data".

## Do's and Don'ts

### ✅ Do
- Anchor on `{colors.canvas}` (#08090C near-black with a cool cast) — that blue tint is intentional.
- Layer via the four-step surface ladder (canvas → surface-1 → 2 → 3 → 4) + 1px hairline borders, **almost never shadows**.
- Add a top `{colors.edge-highlight}` inner highlight to every raised surface — the key dark-texture reflection.
- Use `{elevation.blur}` frosted glass on the top nav/overlays — this is the "blur/reflection" modern feature.
- Use only `{colors.primary}` indigo as the accent, **≤ 5% of any screen**, only brand/primary-CTA/focus/key-links.
- Display uses aggressive negative tracking (-3px @ 64px), weight **500–600**, hierarchy via size jumps.
- Shadows are stacked micro-offsets (`{elevation.raised}`), never a single heavy drop.
- Section gap ≥ 48px (96px on marketing), card padding 24–32px.
- Continuous values use an indigo brightness gradient; charts use `{colors.viz-1/2/3}`.
- Empty states use unified minimal line illustration (prepend the Master prompt) + title + encouraging copy.
- Components reference `{tokens}`; no inline styles, no mishmash buttons.

### ❌ Don't
- No pure black `#000` canvas, no large white backgrounds, no stock photo backgrounds.
- **No neon glow, no aurora / cross-hue gradients** (v2's core mistake).
- **No second accent** fighting the indigo.
- No serif display (v1's core mistake), no 700/800 heavy headlines.
- No high-saturation colors outside the tokens (purple `#667eea`, pink `#fb7299`, amber `#f59e0b`, bright blue `#3b82f6`, neon cyan `#34E0C0`, etc.).
- No green/yellow/red traffic-light coding, no rainbow badges.
- No circular + colored-gradient monogram avatars (circle fine, drop gradient for surface base + single color).
- No 3D flips, light sweeps, hover flicker, single heavy drop shadow.
- No low-contrast gray body text (unreadable on dark).
- No full-pill buttons (marketing flavor; app uses 8px square corners).
- Illustrations must not be cartoonish, 3D, cyberpunk, or photo, must not glow, and use indigo on only one or two elements.
