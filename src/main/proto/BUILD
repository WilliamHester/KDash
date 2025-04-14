load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

java_proto_library(
    name = "tdf_header_java_proto",
    deps = [":tdf_header_proto"],
    visibility = ["//visibility:public"],
)

proto_library(
    name = "tdf_header_proto",
    srcs = ["tdf_header.proto"],
    deps = [
        "@protobuf//:timestamp_proto",
    ]
)