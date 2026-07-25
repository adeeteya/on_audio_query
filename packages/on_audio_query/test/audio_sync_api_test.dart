import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:on_audio_query/on_audio_query.dart';

void main() {
  late OnAudioQueryPlatform previousPlatform;
  late _FakePlatform fakePlatform;

  setUp(() {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    previousPlatform = OnAudioQueryPlatform.instance;
    fakePlatform = _FakePlatform();
    OnAudioQueryPlatform.instance = fakePlatform;
  });

  tearDown(() {
    OnAudioQueryPlatform.instance = previousPlatform;
    debugDefaultTargetPlatformOverride = null;
  });

  test('querySongs preserves the compatibility route without options',
      () async {
    await OnAudioQuery().querySongs();

    expect(fakePlatform.compatibilityQueries, 1);
    expect(fakePlatform.typedQueries, 0);
  });

  test('querySongs uses the typed route when options are supplied', () async {
    await OnAudioQuery().querySongs(
      options: const AudioQueryOptions(isMusic: true),
    );

    expect(fakePlatform.compatibilityQueries, 0);
    expect(fakePlatform.typedQueries, 1);
  });

  test('querySongsPage validates paging arguments before platform calls', () {
    expect(
      () => OnAudioQuery().querySongsPage(limit: 0),
      throwsArgumentError,
    );
    expect(
      () => OnAudioQuery().querySongsPage(offset: -1),
      throwsArgumentError,
    );
  });

  test('synchronization APIs report unsupported platforms explicitly', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;

    expect(
      () => OnAudioQuery().queryAudioVolumes(),
      throwsUnsupportedError,
    );
    expect(
      () => OnAudioQuery().watchAudioLibrary(),
      throwsUnsupportedError,
    );
  });
}

class _FakePlatform extends OnAudioQueryPlatform {
  int compatibilityQueries = 0;
  int typedQueries = 0;

  @override
  Future<List<SongModel>> querySongs({
    SongSortType? sortType,
    OrderType? orderType,
    UriType? uriType,
    bool? ignoreCase,
    String? path,
  }) async {
    compatibilityQueries++;
    return [];
  }

  @override
  Future<List<SongModel>> querySongsWithOptions({
    required AudioQueryOptions options,
    SongSortType? sortType,
    OrderType? orderType,
    UriType? uriType,
    bool? ignoreCase,
    String? path,
  }) async {
    typedQueries++;
    return [];
  }
}
