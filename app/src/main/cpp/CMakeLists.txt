cmake_minimum_required(VERSION 3.22.1)

# 16KB 页对齐：Android 15+ 新设备要求，否则 native 库加载失败
# 参考 marcomorosi06/DroidWine
add_link_options("-Wl,-z,max-page-size=16384" "-Wl,-z,common-page-size=16384")

add_subdirectory(winlator)
add_subdirectory(vortekrenderer)
add_subdirectory(virglrenderer)
add_subdirectory(midihandler)
add_subdirectory(libadrenotools)
add_subdirectory(gladiorenderer)
