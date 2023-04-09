cmake_minimum_required(VERSION 3.22.1)
project("diff_calculator")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")

include_directories(
	/usr/lib/jvm/java-11-openjdk-amd64/include/
	/usr/lib/jvm/java-11-openjdk-amd64/include/linux/
)

add_library(diff_calculator_jni SHARED diff_calculator.cpp)
target_link_libraries(diff_calculator_jni pthread ${CMAKE_DL_LIBS})