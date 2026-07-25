<div align=center>

# on_audio_query
[![Pub.dev](https://img.shields.io/pub/v/on_audio_query?color=9cf&label=Pub.dev&style=flat-square)](https://pub.dev/packages/on_audio_query)
[![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20IOS%20%7C%20Web-9cf?&style=flat-square)]()
[![Languages](https://img.shields.io/badge/Languages-Dart%20%7C%20Kotlin%20%7C%20Swift-9cf?&style=flat-square)]()

[Flutter](https://flutter.dev/) Plugin used to query audios/songs 🎶 infos [title, artist, album, etc..] from device storage. <br>

**Any problem? [Issues](https://github.com/LucJosin/on_audio_query/issues)** <br>
**Any suggestion? [Pull request](https://github.com/LucJosin/on_audio_query/pulls)**

</div>

### Topics:

* [Installation](#installation)
* [Platforms](#platforms)
* [Overview](#overview)
* [Examples](#examples)
* [Gif Examples](#gif-examples)
* [License](#license)

## Platforms:

<!-- ✔️ | ❌ -->
|  Methods  |   Android   |   IOS   |   Web   |
|-------|:----------:|:----------:|:----------:|
| `querySongs` | `✔️` | `✔️` | `✔️` | <br>
| `querySongsPage` | `✔️` | `❌` | `❌` | <br>
| `queryAudioVolumes` | `✔️` | `❌` | `❌` | <br>
| `watchAudioLibrary` | `✔️` | `❌` | `❌` | <br>
| `queryAlbums` | `✔️` | `✔️` | `✔️` | <br>
| `queryArtists` | `✔️` | `✔️` | `✔️` | <br>
| `queryPlaylists` | `✔️` | `✔️` | `❌` | <br>
| `queryGenres` | `✔️` | `✔️` | `✔️` | <br>
| `queryAudiosFrom` | `✔️` | `✔️` | `✔️` | <br>
| `queryWithFilters` | `✔️` | `✔️` | `✔️` | <br>
| `queryArtwork` | `✔️` | `✔️` | `✔️` | <br>
| `queryArtworkByUri` | `✔️` | `❌` | `❌` | <br>
| `createPlaylist` | `✔️` | `✔️` | `❌` | <br>
| `removePlaylist` | `✔️` | `❌` | `❌` | <br>
| `addToPlaylist` | `✔️` | `✔️` | `❌` | <br>
| `removeFromPlaylist` | `✔️` | `❌` | `❌` | <br>
| `renamePlaylist` | `✔️` | `❌` | `❌` | <br>
| `moveItemTo` | `✔️` | `❌` | `❌` | <br>
| `checkAndRequest` | `✔️` | `✔️` | `❌` | <br>
| `permissionsRequest` | `✔️` | `✔️` | `❌` | <br>
| `permissionsStatus` | `✔️` | `✔️` | `❌` | <br>
| `queryDeviceInfo` | `✔️` | `✔️` | `✔️` | <br>
| `scanMedia` | `✔️` | `❌` | `❌` | <br>

✔️ -> Supported <br>
❌ -> Not Supported <br>

**[See all platforms methods support](https://github.com/LucJosin/on_audio_query/blob/main/on_audio_query/PLATFORMS.md)**

## Installation:

Add the following code to your `pubspec.yaml`:
```yaml
dependencies:
  on_audio_query: ^2.9.0
```

### Request Permission:

#### Android:
To use this plugin add the following code to your [AndroidManifest.xml](https://github.com/LucJosin/on_audio_query/blob/main/on_audio_query/example/android/app/src/main/AndroidManifest.xml)
```xml
<manifest>
  
  <!-- Android 12 or below -->
  <uses-permission
      android:name="android.permission.READ_EXTERNAL_STORAGE"
      android:maxSdkVersion="32"/>

  <!-- Android 13 or greater -->
  <uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>

</manifest>
```

#### IOS:
To use this plugin add the following code to your [Info.plist](https://github.com/LucJosin/on_audio_query/blob/main/on_audio_query/example/ios/Runner/Info.plist)
```
<dict>

	<key>NSAppleMusicUsageDescription</key>
	<string>$(PROJECT_NAME) requires access to media library</string>

</dict>
```

## Some Features:

* Optional and Built-in storage `READ` and `WRITE` permission request
* Get all audios/songs.
* Get all albums and album-specific audios.
* Get all artists and artist-specific audios.
* Get all playlists and playlists-specific audios.
* Get all genres and genres-specific audios.
* Get all query methods with specific `keys` [Search].
* Create/Delete/Rename playlists.
* Add/Remove/Move specific audios to playlists.
* Specific sort types for all query methods.

## Overview:

All types of methods on this plugin:

### Android library synchronization

Use typed options to avoid raw SQL and page large MediaStore libraries without
copying the complete catalog through a method channel:

```dart
final options = AudioQueryOptions(
  isMusic: true,
  includeAlarms: false,
  includeNotifications: false,
  includeRingtones: false,
  minimumDuration: 30000,
);

SongPage page = await _audioQuery.querySongsPage(
  options: options,
  limit: 500,
);
```

`queryAudioVolumes()` reports mounted volumes and, on Android 11+, recently
disconnected volumes. Compare a volume's opaque `version` before using generation
numbers; a changed version requires a full rescan because generation values may
have reset.

`watchAudioLibrary()` forwards native MediaStore notifications without debouncing.
The change type is `unknown` before Android 11, so consumers should use every event
as a signal to reconcile rather than as a complete change record. Cancel the
subscription when observation is no longer needed.

### Artwork Widget

```dart
  Widget someOtherName() async {
    return QueryArtworkWidget(
      id: <audioId>,
      type: ArtworkType.AUDIO,
    );
  }
```

**See more: [QueryArtworkWidget](https://pub.dev/documentation/on_audio_query/latest/on_audio_query/QueryArtworkWidget-class.html)**

## Examples:

#### OnAudioQuery

```dart
final OnAudioQuery _audioQuery = OnAudioQuery();
```

#### Query methods:

- queryAudios();
- queryAlbums();
- queryArtists();
- queryPlaylists();
- queryGenres().

```dart
  someName() async {
    // Query Audios
    List<AudioModel> audios = await _audioQuery.queryAudios();

    // Query Albums
    List<AlbumModel> albums = await _audioQuery.queryAlbums();
  }
```

#### scanMedia

Use this opt-in Android method after adding, changing, or deleting one media file so
`MediaStore` can update that file's state. Pass a filesystem path to a single file.
Directories and storage roots are unsupported, and query methods never invoke media
scanning automatically.

```dart
  someName() async {
    OnAudioQuery _audioQuery = OnAudioQuery();
    File file = File('path');
    try {
      if (file.existsSync()) {
        file.deleteSync();
        _audioQuery.scanMedia(file.path); // Scan the media 'path'
      }
    } catch (e) {
      debugPrint('$e');
    }
  }
```

#### queryArtwork

```dart
  someName() async {
    // DEFAULT: ArtworkFormat.JPEG, 200 and false
    Uint8List something = await _audioQuery.queryArtwork(
        <audioId>,
        ArtworkType.AUDIO,
        ...,
      );
  }
```

For songs returned from Android primary storage, SD cards, or USB volumes,
prefer the volume-specific content URI:

```dart
final SongModel song = songs.first;
final Uint8List? artwork = song.uri == null
    ? null
    : await _audioQuery.queryArtworkByUri(song.uri!);
```

Or you can use a basic and custom Widget.
**See example [QueryArtworkWidget](#artwork-widget)**

## Gif Examples:
| <img src="https://user-images.githubusercontent.com/76869974/129740857-33f38b27-06a3-4959-bb31-2ae97d6b66ff.gif"/> | <img src="https://user-images.githubusercontent.com/76869974/129741012-1215b292-d700-466f-9c41-552df0ad5e89.gif"/> | <img src="https://user-images.githubusercontent.com/76869974/129741188-e6803432-24d7-4e39-bfde-cc6765e13663.gif"/> | <img src="https://user-images.githubusercontent.com/76869974/129741151-b820edc9-ddbf-4446-b67a-6e254cb5a46d.gif"/> |
|:---:|:---:|:---:|:---:|
| <img src="https://user-images.githubusercontent.com/76869974/129763885-c0cb3871-39af-45fa-aebf-ebf4113effa2.gif"/> | <img src="https://user-images.githubusercontent.com/76869974/129763519-497cab72-6a95-42fd-8237-3f83e954ea50.gif"/> | <img src="https://user-images.githubusercontent.com/76869974/129763577-9037d16f-f940-4bcb-ba37-879a0eecf2ac.gif"/> | <img src="https://user-images.githubusercontent.com/76869974/129763551-726512a9-bc10-4c75-a167-8928f0c0c212.gif"/> |
| Songs | Albums | Playlists | Artists |

## LICENSE:

* [LICENSE](https://github.com/LucJosin/on_audio_query/blob/main/on_audio_query/LICENSE)

> * [Back to top](#on_audio_query)
