<div align="center">
	<img height="300" src="media/app-logo.png" alt="FlixTraktor">
	<br>
	<br>
</div>

<p align="center">
⚠️ Work in Progress ⚠️
</p>

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
1. Search the Trakt API for the show name, if it does not return a show, we cannot sync it.
2. Capture the season number by removing all text from the seasonDescription (e.g. Season 3). It is necessary to do 
this, again because the season descriptions are localized as well. If the show only has 1 season, we continue with that season
because 1. The episode probably belongs to that season and 2. sometimes shows only have 1 season and therefore are not numbered.
3. Search through the returned episodes to find the one with the correct season and episode number