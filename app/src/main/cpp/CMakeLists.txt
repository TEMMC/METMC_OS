cmake_minimum_required(VERSION 3.22.1)

project("metmc_display")

add_library(
    metmc_display
    SHARED
    metmc_display_bridge.cpp
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
