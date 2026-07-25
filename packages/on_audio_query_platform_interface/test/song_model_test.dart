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
}
