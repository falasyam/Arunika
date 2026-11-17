cmake_minimum_required(VERSION 3.22.1)

project("lumina_native_engine")

add_library(
    lumina_native_engine
    SHARED
    lumina_native_engine.cpp
)

find_library(
    log-lib
    log
)

find_library(
    gles3-lib
    GLESv3
)

find_library(
    egl-lib
    EGL
)

find_library(
    android-lib
    android
)

target_link_libraries(
    lumina_native_engine
    ${log-lib}
    ${gles3-lib}
    ${egl-lib}
    ${android-lib}
)
