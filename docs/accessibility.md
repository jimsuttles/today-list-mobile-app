# Today List — accessibility notes

## TalkBack coverage (v1)
- Bottom navigation labels
- FAB “Quick add”
- Task complete control: “Complete {title}”
- Task row: “Open {title}”
- Task overflow: “Actions for {title}”
- Today progress announced politely when counts change
- History rows: “Completed: {title}”
- Settings rows as buttons; radio groups for theme / rollover / week start
- Quick Add list chips announce selected state

## Targets
IconButtons and settings rows use Material minimum interactive size (≥ 48dp).

## Contrast
Theme defines navy primary, coral secondary, and explicit `outline` / `error` colors for light and dark schemes. Selected chips and the Today progress fill use coral on high-contrast on-colors.

## Manual smoke (TalkBack)
1. Enable TalkBack → open Today → complete a task → Undo snackbar
2. Open task detail → change reminder → back
3. Settings → change theme / rollover with radio announcements
4. History → open a completion → recreate to Today
