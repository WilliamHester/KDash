load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "swing",
    srcs = glob(["*.kt"]),
    deps = [
        "//main/java/me/williamhester/kdash/api",
        "//main/java/me/williamhester/kdash/testing",
    ],
)

java_binary(
    name = "swing_main",
    main_class = "me.williamhester.kdash.swing.MainKt",
    runtime_deps = [":swing"],
)