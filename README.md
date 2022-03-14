<div align="center">
	<img height="300" src=".github/media/app-logo.png" alt="FlixTraktor">
	<br>
	<br>
</div>

[![Maven build](https://github.com/dylanvdbrink/flixtraktor/actions/workflows/maven.yml/badge.svg)](https://github.com/dylanvdbrink/flixtraktor/actions/workflows/maven.yml)

### Features
- [x] Retrieve Netflix Viewing Activity
- [ ] Sync to trakt

------

### Thanks to
* [HowardStark](https://github.com/HowardStark/shakti "HowardStark") - For describing the Netflix Shakti API
* [UweTrottmann/trakt-java](https://github.com/UweTrottmann/trakt-java) - For a Java trakt.tv API wrapper

------

### Design choices

#### Episode lookup
Because Netflix returns the localized titles of episode names, we need a flow of calls to the Trakt API to make sure
we sync the correct episodes, which goes as follows:
1. Search the Trakt API for the show name + episode title, if it returns an episode, we check the 
[Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) of the name of the show to verify the match is correct. 
If it does not return an episode, we go to step 2. 
2. Capture the season and episode number by removing all text from the seasonDescription and episode title (e.g. "Season 3", "Episode 1"). 
It is necessary to do this because both descriptions are localized as well. If it finds an episode and the title of the show is a match, we 
sync that episode. If not, we do not sync that title.