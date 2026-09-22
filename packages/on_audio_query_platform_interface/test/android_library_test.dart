import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:on_audio_query_platform_interface/method_channel_on_audio_query.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final api = MethodChannelOnAudioQuery();
  const channel = MethodChannel('com.lucasjosino.on_audio_query');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  test('preserves volume identity, absent paths and 64-bit sizes', () async {
    messenger.setMockMethodCallHandler(channel, (call) async {
      expect(call.method, 'queryAndroidLibrary');
      return {
        'sdkVersion': 33,
        'volumes': ['external_primary', 'sd-card'],
        'songs': [
          {
            'uri': 'content://media/sd-card/audio/media/1',
            'volume': 'sd-card',
            'path': null,
            'size': 5000000000,
            'modified': 2000000000,
            'metadata': {'title': 'Track'},
          }
        ],
      };
    });
    final result = await api.queryAndroidLibrary();
    expect(result.songs.single.size, 5000000000);
    expect(result.songs.single.path, isNull);
    expect(result.volumes, contains('sd-card'));
  });

  test('propagates failures instead of returning an empty snapshot', () async {
    messenger.setMockMethodCallHandler(channel, (_) async {
      throw PlatformException(code: 'LibraryQueryFailed');
    });
    expect(api.queryAndroidLibrary(), throwsA(isA<PlatformException>()));
  });
}
