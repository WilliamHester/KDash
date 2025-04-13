load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "monitors",
    srcs = glob(["*.kt"]),
    deps = [
        "//main/java/me/williamhester/kdash/api",
        "@maven//:com_google_guava_guava",
    ],
)
