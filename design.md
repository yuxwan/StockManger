---
name: Soft Minimalist System
colors:
  surface: '#fbf9f9'
  surface-dim: '#dbdad9'
  surface-bright: '#fbf9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f3'
  surface-container: '#efeded'
  surface-container-high: '#e9e8e7'
  surface-container-highest: '#e3e2e2'
  on-surface: '#1b1c1c'
  on-surface-variant: '#4c4546'
  inverse-surface: '#303031'
  inverse-on-surface: '#f2f0f0'
  outline: '#7e7576'
  outline-variant: '#cfc4c5'
  surface-tint: '#5e5e5e'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#1b1b1b'
  on-primary-container: '#848484'
  inverse-primary: '#c6c6c6'
  secondary: '#5d5f5f'
  on-secondary: '#ffffff'
  secondary-container: '#dcdddd'
  on-secondary-container: '#5f6161'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#1b1b1b'
  on-tertiary-container: '#848484'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e2e2e2'
  primary-fixed-dim: '#c6c6c6'
  on-primary-fixed: '#1b1b1b'
  on-primary-fixed-variant: '#474747'
  secondary-fixed: '#e2e2e2'
  secondary-fixed-dim: '#c6c6c7'
  on-secondary-fixed: '#1a1c1c'
  on-secondary-fixed-variant: '#454747'
  tertiary-fixed: '#e2e2e2'
  tertiary-fixed-dim: '#c6c6c6'
  on-tertiary-fixed: '#1b1b1b'
  on-tertiary-fixed-variant: '#474747'
  background: '#fbf9f9'
  on-background: '#1b1c1c'
  surface-variant: '#e3e2e2'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 36px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 24px
  lg: 48px
  xl: 80px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 64px
---

## Brand & Style

The design system is rooted in **Soft Minimalism**. It prioritizes clarity, breathability, and an organic tactile feel. By stripping away unnecessary ornamentation and shadows, the focus shifts to form, negative space, and high-contrast intentionality. 

The emotional response should be one of serenity and approachability. It targets a sophisticated audience that values "quiet luxury" and functional simplicity. The UI avoids the coldness often associated with minimalism by employing extreme corner rounding, creating a "pebble-like" or "pill-style" aesthetic that feels safe and friendly to the touch.

## Colors

The palette is strictly monochromatic to ensure maximum focus on content and structure. 

- **Primary (Black):** Used for primary actions, headings, and high-emphasis icons. It provides the "anchor" for the interface.
- **Secondary (Light Gray):** Used for container backgrounds, inactive states, and subtle dividers. This creates a soft layering effect without the need for shadows.
- **Neutral (Medium Gray):** Reserved for secondary body text, captions, and supporting information that requires less visual weight.
- **Background (White):** The primary canvas. Use pure white to maintain a crisp, clean aesthetic.

## Typography

This design system utilizes a pairing of two modern sans-serifs. **Hanken Grotesk** provides a sharp, geometric precision for headlines and labels, while **Plus Jakarta Sans** offers a softer, more legible experience for long-form body text.

Typography should be treated as a structural element. Use tight tracking on large headings to emphasize the "blocky" minimalist feel, and generous leading in body text to maintain the "Soft" brand promise. All text should adhere to the primary black or neutral gray color tokens to maintain the monochromatic theme.

## Layout & Spacing

The layout philosophy follows a **fixed-fluid hybrid**. Content containers generally adhere to a 12-column grid on desktop, but with significantly larger margins than traditional systems to enforce whitespace.

- **Rhythm:** Use a 8px baseline grid. Padding within containers should be generous (typically 24px or 32px) to complement the large corner radii.
- **Breakpoints:**
  - **Mobile (<600px):** 4 columns, 16px margins, 16px gutters.
  - **Tablet (600px - 1024px):** 8 columns, 32px margins, 24px gutters.
  - **Desktop (>1024px):** 12 columns, 64px margins (or max-width 1200px centered), 24px gutters.
- **Reflow:** Elements should stack vertically on mobile, with full-width buttons to provide easy tap targets.

## Elevation & Depth

This design system completely avoids traditional box-shadows. Depth is achieved through **Tonal Layering** and **Subtle Outlines**.

1.  **Level 0 (Base):** Pure White (#FFFFFF).
2.  **Level 1 (Containers):** Very Light Gray (#F2F2F2) fills.
3.  **Level 2 (Active/Hover):** Thin 1px solid borders in Black or a slightly darker gray (#E0E0E0).

When an element needs to feel "elevated," it does not rise via a shadow; instead, it gains a subtle fill or a stroke. This maintains the flat, minimalist integrity of the design while providing clear visual hierarchy.

## Shapes

The "Soft" in Soft Minimalism is defined by the shape language. This design system uses **extreme roundedness** (Pill-shaped) for almost all components.

- **Standard Elements:** Buttons and Input fields should use a 32px or fully rounded radius.
- **Cards and Containers:** Larger containers should use a minimum of 24px (rounded-lg) to 48px (rounded-xl) radius.
- **Consistency:** Never mix sharp corners with rounded ones. Even decorative elements or image masks should adopt the system-wide roundedness to maintain the organic, friendly feel.

## Components

### Buttons
Primary buttons are solid Black with White text. Secondary buttons use a Light Gray (#F2F2F2) fill with Black text. All buttons feature a 32px border radius. Hover states should involve a subtle shift in opacity or a 1px interior stroke.

### Input Fields
Inputs are Light Gray (#F2F2F2) containers with no borders in their default state. On focus, they transition to a 1px Black border. Text is set in Body-MD.

### Cards
Cards are defined by their 24px - 48px rounded corners. They should either have a White background with a 1px light gray border or a solid Light Gray (#F2F2F2) fill with no border.

### Chips & Tags
Small, fully pill-shaped elements. Use these for categorization. They should always have a Light Gray fill to remain secondary to primary buttons.

### Lists
Lists should be separated by whitespace rather than lines where possible. If dividers are necessary, use a 1px Light Gray (#F2F2F2) stroke that does not touch the edges of the container, maintaining the "soft" disconnected feel.

### Progress Bars
Solid Black fill on a Light Gray track. The ends of both the track and the fill must be fully rounded.