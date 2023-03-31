load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "laphero",
    srcs = glob(["*.kt"]),
    deps = [
        "//main/java/me/williamhester/kdash/api",
        "//main/java/me/williamhester/kdash/testing",
        "//main/proto:tdf_header_java_proto",
    ],
)

java_binary(
    name = "swing_main",
    main_class = "me.williamhester.kdash.swing.laphero.GraphKt",
    runtime_deps = [":laphero"],
)
