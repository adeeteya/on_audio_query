// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "on_audio_query_ios",
    platforms: [.iOS("11.0")],
    products: [
        .library(name: "on-audio-query-ios", targets: ["on_audio_query_ios"])
    ],
    dependencies: [
        .package(name: "FlutterFramework", path: "../FlutterFramework"),
        .package(url: "https://github.com/SwiftyBeaver/SwiftyBeaver.git", exact: "1.9.5")
    ],
    targets: [
        .target(
            name: "on_audio_query_ios",
            dependencies: [
                .product(name: "FlutterFramework", package: "FlutterFramework"),
                .product(name: "SwiftyBeaver", package: "SwiftyBeaver")
            ]
        )
    ]
)
