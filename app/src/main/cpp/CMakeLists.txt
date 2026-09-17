cmake_minimum_required(VERSION 3.22.1)

project(metmc_display)

add_library(
        metmc_display
        SHARED
        metmc_display_bridge.cpp
)

find_library(
        android-lib
        android
)

find_library(
        log-lib
        log
)

target_link_libraries(
        metmc_display
        ${android-lib}
        ${log-lib}
)
