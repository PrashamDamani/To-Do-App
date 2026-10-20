# Manual testing checklist - To Do app (C030 Prasham Damani)
Mark each item after running on an emulator or device.

## Auth
- [ ] Register with valid data -> lands on Home; user in Firebase Auth; `/users/<uid>` created; no password in DB
- [ ] Register with bad email / short password / mismatched confirm -> clear error
- [ ] Duplicate email -> "already exists" message
- [ ] Login valid / wrong password (generic invalid message)
- [ ] Forgot password email sent
- [ ] Kill and reopen app -> session restored (splash -> Home)
- [ ] Logout confirmation -> Login screen; a second account sees none of the first account's tasks

## Tasks CRUD
- [ ] Create task with only a title; empty title rejected
- [ ] Create with all fields; verify record in console
- [ ] Edit fields -> Firebase updates and list updates
- [ ] Change a value in console -> app updates without refresh
- [ ] Swipe left delete -> Undo restores; swipe right toggles complete
- [ ] Delete from details with confirmation; pending reminder cancelled
- [ ] Complete -> leaves Active list, appears in Completed; Reopen returns it
- [ ] Dashboard stats update immediately

## Priority / filters
- [ ] Order High > Medium > Low; overdue first inside a priority; Overdue section present
- [ ] Sort options (priority, deadline, created, A-Z)
- [ ] Filters: category, priority, importance, date, overdue, status, search
- [ ] Dashboard cards open correctly filtered list

## Calendar
- [ ] Heading "C030 - Prasham Damani Schedule" (profile with roll C030)
- [ ] Month/week/agenda navigation, today highlighted, date selection
- [ ] Deadlines vs blocks vs all-day vs unscheduled displayed separately
- [ ] End time from start + duration; none shown without duration
- [ ] Overlap warning; reschedule works and moves reminder

## Reminders / timer
- [ ] Enable reminder -> notification at time; tap opens task
- [ ] Disable / complete / delete cancels it; editing reschedules without duplicates
- [ ] Deny notification permission -> banner + message, app still works
- [ ] Deadline without reminder -> no notification
- [ ] Reminder in the past rejected by the form
- [ ] Reboot device -> future reminder still fires
- [ ] Timer start/pause/resume/reset; finish notification (also in background); link task

## Offline / security / UI
- [ ] Airplane mode: offline banner, cached tasks visible; edit offline syncs later
- [ ] Rules: reading another uid's path in Rules Playground is denied
- [ ] Empty states (no tasks, no results, no reminders, insights)
- [ ] Back button behavior on every screen; bottom nav highlight
- [ ] Reduce animations setting disables transitions
- [ ] Screen rotation keeps form input
