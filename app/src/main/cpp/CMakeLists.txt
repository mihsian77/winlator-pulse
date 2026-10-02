cmake_minimum_required(VERSION 3.22.1)

# 顶层项目声明，消除 CMake "No project() command" 警告
project(winlator_native C CXX)

# 16KB 页大小修复：骁龙 8 至尊等新设备原生 16KB 页，
# 4KB 对齐的 .so 在这些设备上 mmap 可能失败导致闪退。
# 源自 marcomorosi06/DroidWine (LGPL-2.1)。
# 注意：midihandler/pulseaudio 等预编译库仍为 4KB 对齐，
# 此处仅影响本仓库编译的 .so（winlator/vortek/virgl/gladio 等）。
add_link_options("-Wl,-z,max-page-size=16384" "-Wl,-z,common-page-size=16384")

add_subdirectory(winlator)
add_subdirectory(vortekrenderer)
add_subdirectory(virglrenderer)
add_subdirectory(midihandler)
add_subdirectory(libadrenotools)
add_subdirectory(gladiorenderer)
