## AsteraCare Android Application – AI-Based Smart Flower Preservation System

### Description
AsteraCare is a mobile application designed to monitor and control a smart flower preservation chamber. It integrates AI-powered flower classification, real-time sensor monitoring, and Bluetooth-based hardware control to extend the lifespan of flowers under optimal conditions.

### Getting Started
To use this repository for any custom YOLOv8 Object detection model, follow these steps:
1. Clone this repository to your local machine using `git clone https://github.com/surendramaran/YOLOv8-TfLite-Object-Detector`.
2. Put your .tflite model and .txt label file inside the assets folder
3. Rename paths of your model and labels file in Constants.kt file
4. **Build and Run:**

### Features:
- AI Classification: Uses a YOLOv8 model to detect and classify Asteraceae flower species.
- Real-Time Monitoring: Displays chamber temperature, humidity, and other sensor data.
- Bluetooth Connectivity: Communicates with ESP32 microcontroller for sensor readings and actuator control.
- Automation: Automatically adjusts preservation conditions based on AI classification and chamber parameters.

