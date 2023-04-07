Eclipse project.

Required:
	1. eclipse-java+cdt both
	2. cmake

Bringup -
	1. cd jni && mkdir build && cd build and cmake ../
	2. in eclipse edit run configuration (easy way create this - run once with fail!):
	    * in environment set LD_LIBRARY_PATH to ${workspace_project_locations}/jni/build
	    * in Project properties in "Paths and Symbols" add to include jni includes, some like: 
			/usr/lib/jvm/java-11-openjdk-amd64/include/
			/usr/lib/jvm/java-11-openjdk-amd64/include/linux/
		* in Project properies in "C/C++ Build" set "build location" to ${workspace_project_locations/jni/build}
