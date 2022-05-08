load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "kdash",
    srcs = glob(["*.kt"]),
    data = [
      "//main/java/me/williamhester/kdash/sampledata:live-data.ibt",
      "//main/java/me/williamhester/kdash/sampledata:logged-data.ibt",
    ],
    deps = [
        "//main/java/me/williamhester/kdash/api",
        "//main/java/me/williamhester/kdash/testing",
    ],
)

java_binary(
    name = "kdash_main",
    main_class = "me.williamhester.kdash.IRacingReaderMainKt",
    runtime_deps = [":kdash"],
)