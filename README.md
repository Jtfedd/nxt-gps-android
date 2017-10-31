# NXT-GPS-Android

A test project for combining an Android program with the LEGO Mindstorms NXT for GPS navigation and image recognition.

The robot code lives in [GPS Robot](https://github.com/Jtfedd/nxt-gps-android/tree/master/GPS%20Robot)

The Android code lives in the src of the Android Studio project ([here](https://github.com/Jtfedd/nxt-gps-android/tree/master/app/src/main/java/com/opencvtest/jake/opencvtest))

The goal of this project was to use the GPS and camera on the phone, interfacing with the NXT over Bluetooth, to allow the robot to follow waypoints outside. GPS data is fed to the robot, which calculates where it needs to drive to reach the waypoint. The android program also uses the camera to detect and track orange cones marking the waypoints. When the robot is close to a waypoint, it uses the image tracking data to home in on the cone and drive up to touch it.
