cmake_minimum_required(VERSION 3.22.1)

project(metmc_display)

add_library(
    metmc_display
    SHARED
    display/metmc_display.cpp
)

find_library(
    log-lib
    log
)

find_library(
    android-lib
    android
)

target_link_libraries(
    metmc_display
    ${log-lib}
    ${android-lib}
)

target_compile_features(
    metmc_display
    PRIVATE
    cxx_std_17
)
