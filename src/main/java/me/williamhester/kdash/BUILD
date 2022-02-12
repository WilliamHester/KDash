load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "kdash",
    srcs = glob(["*.kt"]),
    deps = [
        "//main/java/me/williamhester/kdash/api",
    ],
)

java_binary(
    name = "kdash_main",
    main_class = "me.williamhester.kdash.IRacingReaderMainKt",
    runtime_deps = [":kdash"],
)