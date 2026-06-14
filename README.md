# DocumentScanner

Lightweight Android document scanner app using ML Kit GMS Document Scanner, Room, and a Pinyin annotation helper. Core languages: Kotlin / Java. Build system: Gradle.

## Files
- Project source: `app/src/main/java/com/example/documentscanner`
- Main activity: `app/src/main/java/com/example/documentscanner/MainActivity.kt`
- This README: `README.md`

## Features
- Scan documents using Google ML Kit Document Scanner (JPEG + PDF).
- Save scanned pages as PDF and store first-image preview.
- Extract text from scanned pages and store in local Room database.
- Toggle inline Pinyin annotations for Chinese text.
- Share PDF, delete documents, and view scan history.

## Requirements
- Android Studio (recommended) — tested on Android Studio Otter 2025.2.2 Patch 1 (Windows).
- Android SDK and emulator or device with Google Play services.
- Gradle (wrapper provided).
- Minimum SDK and target SDK are defined in `build.gradle` files.

## Key Dependencies
- Google ML Kit Document Scanner (GmsDocumentScanner)
- AndroidX (Activity, ViewModel, Lifecycle, Core)
- Room (local database)
- com.github.promeg.pinyinhelper (Pinyin helper)
- Material Components

(See `app/build.gradle` for exact dependency versions.)

## Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Use the Gradle wrapper on Windows:
   - `.\gradlew.bat assembleDebug`
4. Run on a device or emulator.

## Permissions
The app may request runtime permissions required for scanning, reading/writing files and sharing URIs. Ensure storage/URI access is granted when prompted.

## Usage
1. Tap Scan to start the ML Kit document scanner.
2. After scanning, the result screen shows:
   - Image preview (first page)
   - Extracted text (if available)
   - Buttons: Share, Delete, History, Show/Hide Pinyin
3. Use Show Pinyin to toggle annotated Pinyin lines above Chinese characters.

## Implementation Notes
- Main logic is in `MainActivity.kt`: scanner initialization, ActivityResultLauncher, text extraction orchestration, and UI state handling.
- Room is used to persist `ScannedDocument` entries (PDF URI, image URIs, extracted text).
- Text extraction for PDF pages is performed asynchronously and results are joined before saving.
- Pinyin annotations are applied using a custom span (`PinyinAnnotationSpan`) and `com.github.promeg.pinyinhelper.Pinyin`.

## Troubleshooting
- If scanner fails to start, verify Google Play services on the device and network connectivity.
- For issues with URI permissions when sharing, ensure `Intent.FLAG_GRANT_READ_URI_PERMISSION` is applied and URIs are FileProvider-backed when needed
## License
Copyright <2026> <COPYRIGHT HOLDER>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
