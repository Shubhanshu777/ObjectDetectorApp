#!/bin/bash

# Download EfficientDet-Lite0 model (good balance of speed and accuracy)
# This model is trained on COCO dataset

MODEL_URL="https://storage.googleapis.com/tfhub-lite-models/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1.tflite"
OUTPUT_DIR="app/src/main/assets"

echo "Creating assets directory..."
mkdir -p "$OUTPUT_DIR"

echo "Downloading EfficientDet-Lite0 model..."
curl -L -o "$OUTPUT_DIR/model.tflite" "$MODEL_URL"

if [ -f "$OUTPUT_DIR/model.tflite" ]; then
    echo "Model downloaded successfully!"
    echo "Model size: $(ls -lh "$OUTPUT_DIR/model.tflite" | awk '{print $5}')"
else
    echo "Failed to download model. Please download manually from:"
    echo "$MODEL_URL"
    echo "And place it in: $OUTPUT_DIR/model.tflite"
fi

echo ""
echo "The app is configured to detect these COCO classes and map them to your labels:"
echo "  - person -> person"
echo "  - chair -> chair"
echo "  - dining table -> table"
echo "  - laptop -> laptop"
echo "  - mouse -> mouse"
echo "  - keyboard -> keyboard"
echo "  - cell phone/tv -> computer"
echo "  - book -> paper"
echo "  - scissors -> pen"
echo ""
echo "NOTE: For better detection of pen/paper/computer, you would need to train a custom model."
