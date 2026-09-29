cmake_minimum_required(VERSION 3.22.1)

# 顶层项目声明，消除 CMake "No project() command" 警告
project(winlator_native C CXX)

add_subdirectory(winlator)
add_subdirectory(vortekrenderer)
add_subdirectory(virglrenderer)
add_subdirectory(midihandler)
add_subdirectory(libadrenotools)
add_subdirectory(gladiorenderer)