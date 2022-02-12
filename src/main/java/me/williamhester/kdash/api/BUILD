load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "api",
    srcs = glob(["*.kt"]),
    deps = [
        "@maven//:net_java_dev_jna_jna",
        "@maven//:net_java_dev_jna_jna_platform",
        "@maven//:org_jetbrains_kotlin_kotlin_stdlib_jdk8"
    ],
)