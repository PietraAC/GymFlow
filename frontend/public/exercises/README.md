# Exercise GIFs

Place each exercise GIF in this directory using the exercise UUID as its filename:

```text
<exercise-id>.gif
```

The Angular `ExerciseMediaComponent` automatically requests `/exercises/<exercise-id>.gif` and keeps the designed placeholder visible when the file does not exist. No template or CSS change is required when media is added.

Keep files optimized for the web and use a 4:3 composition with the movement centered. The interface crops the same asset into a compact square thumbnail in lists and shows the complete 4:3 version in expanded exercise details.
