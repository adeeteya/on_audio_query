import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:on_audio_query_platform_interface/on_audio_query_platform_interface.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('com.lucasjosino.on_audio_query');

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('queryArtworkByUri sends the complete URI and image options', () async {
    MethodCall? receivedCall;
    final expectedBytes = Uint8List.fromList([1, 2, 3]);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      receivedCall = call;
      return expectedBytes;
    });

    final result = await OnAudioQueryPlatform.instance.queryArtworkByUri(
      'content://media/1234-5678/audio/media/42',
      size: 320,
      quality: 75,
      format: ArtworkFormat.PNG,
    );

    expect(receivedCall?.method, 'queryArtworkByUri');
    expect(receivedCall?.arguments, {
      'uri': 'content://media/1234-5678/audio/media/42',
      'format': ArtworkFormat.PNG.index,
      'size': 320,
      'quality': 75,
    });
    expect(result, expectedBytes);
  });

  test('paged query serializes typed filters and parses the page', () async {
    MethodCall? receivedCall;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      receivedCall = call;
      return {
        'songs': [
          {'_id': 1, 'volume_name': 'external_primary'},
        ],
        'nextOffset': null,
        'totalCount': 1,
      };
    });

    final page = await OnAudioQueryPlatform.instance.querySongsPage(
      options: const AudioQueryOptions(
        isMusic: true,
        includeAlarms: false,
        volumeNames: {'external_primary'},
        paths: [
          AudioPathFilter(
            volumeName: 'external_primary',
            relativePathPrefix: 'Music/',
          ),
        ],
      ),
      limit: 250,
      offset: 500,
    );

    expect(receivedCall?.method, 'querySongsPage');
    expect((receivedCall?.arguments as Map)['limit'], 250);
    expect((receivedCall?.arguments as Map)['offset'], 500);
    expect(
      ((receivedCall?.arguments as Map)['options'] as Map)['volumeNames'],
      ['external_primary'],
    );
    expect(page.songs.single.sourceId, 'external_primary:1');
    expect(page.totalCount, 1);
  });

  test('volume query parses mounted and disconnected entries', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      expect(call.method, 'queryAudioVolumes');
      return [
        {
          'name': '1234-5678',
          'version': null,
          'generation': null,
          'currentlyMounted': false,
        },
      ];
    });

    final volumes = await OnAudioQueryPlatform.instance.queryAudioVolumes();

    expect(volumes.single.name, '1234-5678');
    expect(volumes.single.currentlyMounted, isFalse);
  });
}
