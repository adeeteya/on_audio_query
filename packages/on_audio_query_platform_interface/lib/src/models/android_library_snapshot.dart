/// A complete discovery result for the Android volumes queried successfully.
class AndroidLibrarySnapshot {
  final List<AndroidAudioRecord> songs;
  final List<String> volumes;
  final int sdkVersion;

  AndroidLibrarySnapshot.fromMap(Map<dynamic, dynamic> map)
      : songs = (map['songs'] as List)
            .map((item) => AndroidAudioRecord.fromMap(item as Map))
            .toList(growable: false),
        volumes = List<String>.from(map['volumes'] as List),
        sdkVersion = map['sdkVersion'] as int;
}

/// MediaStore identity and inexpensive metadata, before reading embedded tags.
class AndroidAudioRecord {
  final String uri;
  final String volume;
  final String? path;
  final int size;
  final int modified;
  final Map<String, dynamic> metadata;

  AndroidAudioRecord.fromMap(Map<dynamic, dynamic> map)
      : uri = map['uri'] as String,
        volume = map['volume'] as String,
        path = map['path'] as String?,
        size = (map['size'] as num).toInt(),
        modified = (map['modified'] as num).toInt(),
        metadata = Map<String, dynamic>.from(map['metadata'] as Map);
}
