# Object Detector Android App

Real-time object detection Android application using TensorFlow Lite and CameraX.

**Package:** `com.rishi.objectdetector`

## Detected Objects

- pen
- paper
- computer
- table
- chair
- person
- laptop
- mouse
- keyboard

## Setup Instructions

### 1. Download the Model

The model file is already included in `app/src/main/assets/model.tflite`.

If you need to re-download:
```bash
chmod +x download_model.sh
./download_model.sh
```

### 2. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files
3. Connect an Android device (API 24+) or start an emulator
4. Click Run

## Important: Pen and Paper Detection

The included COCO model maps similar objects:
- **paper**: Detected via "book" objects
- **pen**: Detected via elongated objects like "scissors", "toothbrush"

For **accurate pen and paper detection**, you should train a custom model:

### Option 1: Use Roboflow (Easiest)

1. Go to [Roboflow Universe](https://universe.roboflow.com/)
2. Search for "office supplies" or "stationery" datasets
3. Export as TensorFlow Lite format
4. Replace `app/src/main/assets/model.tflite`

### Option 2: Train with TensorFlow Model Maker

```python
from tflite_model_maker import object_detector

train_data = object_detector.DataLoader.from_pascal_voc(
    images_dir='train/images',
    annotations_dir='train/annotations',
    label_map=['pen', 'paper', 'computer', 'table', 'chair', 
               'person', 'laptop', 'mouse', 'keyboard']
)

model = object_detector.create(train_data, model_spec='efficientdet_lite0')
model.export(export_dir='.', tflite_filename='model.tflite')
```

### Option 3: Use Edge Impulse

1. Create project at [Edge Impulse](https://edgeimpulse.com/)
2. Upload labeled images
3. Train and export as TFLite

## Project Structure

```
ObjectDetectorApp/
├── app/src/main/
│   ├── java/com/rishi/objectdetector/
│   │   ├── MainActivity.java
│   │   ├── ObjectDetectorHelper.java
│   │   ├── Detection.java
│   │   └── DetectionOverlayView.java
│   ├── assets/
│   │   ├── model.tflite
│   │   └── labelmap.txt
│   ├── res/
│   └── AndroidManifest.xml
├── build.gradle
└── README.md
```

## Features

- Real-time camera detection with CameraX
- TensorFlow Lite inference with GPU acceleration
- Adjustable confidence threshold
- Color-coded bounding boxes per object class
- Stable lifecycle management
- Thread-safe detection processing

## Requirements

- Android 7.0 (API 24) or higher
- Camera hardware
- ~15MB storage for model

## Label Mapping (COCO to App Labels)

| COCO Label    | App Label |
|---------------|-----------|
| person        | person    |
| chair         | chair     |
| dining table  | table     |
| laptop        | laptop    |
| mouse         | mouse     |
| keyboard      | keyboard  |
| tv/cell phone | computer  |
| book          | paper     |
| scissors/toothbrush | pen |

## License

MIT License
