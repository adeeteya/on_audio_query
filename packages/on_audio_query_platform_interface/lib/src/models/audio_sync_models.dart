import 'song_model.dart';

/// A MediaStore directory filter scoped to one storage volume.
class AudioPathFilter {
  const AudioPathFilter({
    required this.volumeName,
    required this.relativePathPrefix,
  });

  final String volumeName;
  final String relativePathPrefix;

  Map<String, dynamic> toMap() => {
        'volumeName': volumeName,
        'relativePathPrefix': relativePathPrefix,
      };
}

/// Typed filters for Android MediaStore audio queries.
class AudioQueryOptions {
  const AudioQueryOptions({
    this.isMusic,
    this.includeAlarms = true,
    this.includeNotifications = true,
    this.includeRingtones = true,
    this.includePodcasts = true,
    this.includeAudiobooks = true,
    this.minimumDuration,
    this.volumeNames,
    this.paths,
    this.modifiedAfter,
    this.generationModifiedAfter,
  });

  final bool? isMusic;
  final bool includeAlarms;
  final bool includeNotifications;
  final bool includeRingtones;
  final bool includePodcasts;
  final bool includeAudiobooks;
  final int? minimumDuration;
  final Set<String>? volumeNames;
  final List<AudioPathFilter>? paths;
  final int? modifiedAfter;
  final int? generationModifiedAfter;

  Map<String, dynamic> toMap() => {
        'isMusic': isMusic,
        'includeAlarms': includeAlarms,
        'includeNotifications': includeNotifications,
        'includeRingtones': includeRingtones,
        'includePodcasts': includePodcasts,
        'includeAudiobooks': includeAudiobooks,
        'minimumDuration': minimumDuration,
        'volumeNames': volumeNames?.toList(),
        'paths': paths?.map((path) => path.toMap()).toList(),
        'modifiedAfter': modifiedAfter,
        'generationModifiedAfter': generationModifiedAfter,
      };
}

/// One page from a filtered song query.
class SongPage {
  const SongPage({
    required this.songs,
    required this.nextOffset,
    required this.totalCount,
  });

  factory SongPage.fromMap(Map<dynamic, dynamic> map) {
    final rawSongs = map['songs'] as List<dynamic>? ?? const [];
    return SongPage(
      songs: rawSongs.map((song) => SongModel(song as Map)).toList(),
      nextOffset: map['nextOffset'] as int?,
      totalCount: map['totalCount'] as int,
    );
  }

  final List<SongModel> songs;
  final int? nextOffset;
  final int totalCount;
}

/// State associated with one Android MediaStore external volume.
class AudioVolume {
  const AudioVolume({
    required this.name,
    required this.version,
    required this.generation,
    required this.currentlyMounted,
  });

  factory AudioVolume.fromMap(Map<dynamic, dynamic> map) => AudioVolume(
        name: map['name'] as String,
        version: map['version'] as String?,
        generation: map['generation'] as int?,
        currentlyMounted: map['currentlyMounted'] as bool,
      );

  final String name;
  final String? version;
  final int? generation;
  final bool currentlyMounted;
}

enum AudioChangeType {
  unknown,
  insert,
  update,
  delete;

  static AudioChangeType fromName(String? name) {
    for (final type in values) {
      if (type.name == name) return type;
    }
    return AudioChangeType.unknown;
  }
}

/// A native MediaStore change notification.
class AudioLibraryChange {
  const AudioLibraryChange({
    required this.volumeName,
    required this.contentUri,
    required this.type,
  });

  factory AudioLibraryChange.fromMap(Map<dynamic, dynamic> map) {
    return AudioLibraryChange(
      volumeName: map['volumeName'] as String?,
      contentUri: map['contentUri'] as String?,
      type: AudioChangeType.fromName(map['type'] as String?),
    );
  }

  final String? volumeName;
  final String? contentUri;
  final AudioChangeType type;
}
