# Discard prompt on back

Skill: nav-kit
Fixture: an app with nav-kit installed and a `DraftView` holding one text field

## Prompt
In DraftView, ask "Discard changes?" when the user goes back with unsaved text.

## Passes when
- `grep -rn PopBackHandler` finds the call, imported from `…navigation.impl.presentation.back`
- neither `androidx.activity.compose.BackHandler` nor
  `androidx.navigationevent.compose.NavigationBackHandler` is imported
- leaving after "Discard" calls `popBackTo`, not `popBack()`

## Runs
| Date | Model | Without the skill | With it |
|---|---|---|---|
