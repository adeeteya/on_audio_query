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
}
