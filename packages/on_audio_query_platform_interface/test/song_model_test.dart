import 'package:flutter_test/flutter_test.dart';
import 'package:on_audio_query_platform_interface/on_audio_query_platform_interface.dart';

void main() {
  test('exposes nullable legacy and MediaStore location metadata', () {
    final song = SongModel({
      '_id': 1,
      '_data': null,
      '_uri': 'content://media/volume/audio/media/1',
      'relative_path': 'Music/Albums/',
      'volume_name': 'volume',
    });

    expect(song.dataOrNull, isNull);
    expect(song.uri, 'content://media/volume/audio/media/1');
    expect(song.relativePath, 'Music/Albums/');
    expect(song.volumeName, 'volume');
  });

  test('exposes synchronization metadata and volume-qualified identity', () {
    final song = SongModel({
      '_id': 42,
      'volume_name': '1234-5678',
      'mime_type': 'audio/flac',
      'album_artist': 'Various Artists',
      'year': 2024,
      'generation_added': 9007199254740990,
      'generation_modified': 9007199254740991,
    });

    expect(song.mimeType, 'audio/flac');
    expect(song.albumArtist, 'Various Artists');
    expect(song.year, 2024);
    expect(song.generationAdded, 9007199254740990);
    expect(song.generationModified, 9007199254740991);
    expect(song.sourceId, '1234-5678:42');
    expect(SongModel({'_id': 42}).sourceId, isNull);
  });

  test('synchronization models parse native maps', () {
    final page = SongPage.fromMap({
      'songs': [
        {'_id': 7, 'volume_name': 'external_primary'},
      ],
      'nextOffset': 500,
      'totalCount': 501,
    });
    final volume = AudioVolume.fromMap({
      'name': 'external_primary',
      'version': 'v1',
      'generation': 19,
      'currentlyMounted': true,
    });
    final change = AudioLibraryChange.fromMap({
      'volumeName': 'external_primary',
      'contentUri': 'content://media/external_primary/audio/media/7',
      'type': 'update',
    });

    expect(page.songs.single.sourceId, 'external_primary:7');
    expect(page.nextOffset, 500);
    expect(page.totalCount, 501);
    expect(volume.generation, 19);
    expect(volume.currentlyMounted, isTrue);
    expect(change.type, AudioChangeType.update);
  });
}
