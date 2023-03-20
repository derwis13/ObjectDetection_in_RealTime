# Road sign detection and location system designed for mobile devices
The application detect road signs in real time, calculates coordinates of road signs thanks to GPS measurement and built-in sensors. Detecting objects is possible thanks to the imported CNN model.  Collected measurements with detection annotiations are saved in Firebase database. The developed system uses Google maps to place detected objects.
The localization procedure includes:

• real-time object detection based on an image provided by the built-in camera of the mobile
device,

• calculation of the distance between the mobile device and the object,

• orientation of the device in space using built-in inertial sensors,

• localization of the device using the GPS system,

• data fusion, calculation of object coordinates.

<img src="https://github.com/derwis13/ObjectDetection_in_RealTime/blob/master/pictures/ss_4.jpg" alt="Alt text" height=1080>

## Geolocation

Geolocation of detected road signs includes:

• calculation of the current orientation of the device using the available inertial sensors,

relative to the reference frame, which is the north pole of the Earth,

• downloading geographical coordinates from the GPS provider,

• locating the detected object in the image relative to the current position of the device.


<img src="https://github.com/derwis13/ObjectDetection_in_RealTime/blob/master/pictures/map_zoom.jpg" alt="Alt text" height=1080><img src="https://github.com/derwis13/ObjectDetection_in_RealTime/blob/master/pictures/map.jpg" alt="Alt text" height=1080>

## Model CNN

A pretrained model of EfficientDet_Lite0 was used.

![alt text](https://github.com/derwis13/ObjectDetection_in_RealTime/blob/master/pictures/ap_category_epochs.png)

![alt text](https://github.com/derwis13/ObjectDetection_in_RealTime/blob/master/pictures/ap_IoU_epochs.png)

![alt text](https://github.com/derwis13/ObjectDetection_in_RealTime/blob/master/pictures/ap_scales_epochs.png)
## Tech Stack

**CNN:** TensorFlow, Python

**Mobile Application:** Java, Kotlin, Android SDK, Google Maps

**Database:** Firebase, NoSQL


## License

[MIT](https://choosealicense.com/licenses/mit/)

