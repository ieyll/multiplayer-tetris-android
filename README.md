# 🎮 Multiplayer Tetris Game (Android)

A classic Tetris game developed with **Kotlin** for Android.
The project includes both **single-player** and **real-time multiplayer** modes.

## 🚀 Technologies
- Kotlin
- Android Studio
- Firebase Realtime Database
- Firebase Authentication

## 🔥 Multiplayer Architecture
Real-time multiplayer functionality was implemented using **Firebase Realtime Database**.
Firebase services were used for **testing and prototyping purposes** and are currently inactive.

> ⚠️ `google-services.json` is not included in this repository for security reasons.

## ✨ Features
- Classic Tetris gameplay mechanics
- Single-player mode
- Real-time two-player mode
- Clean architecture (`model`, `logic`, `ui`)
- Scalable game logic

## 🧠 Multiplayer Logic
- Player states are synchronized in real time using Firebase Realtime Database
- Game events such as score updates and piece positions are shared between players
- Designed to support scalability for future online matchmaking

## 📸 Screenshots

### Tetris
![Tetris](tetris.png)

### Multiplayer Mode
![Multiplayer](assets/multiplayer.png)

## ▶️ How to Run
1. Clone the repository
2. Open with Android Studio
3. (Optional) Connect your own Firebase project
4. Sync Gradle and run

## 👩‍💻 Developer
**Eylül İlbarış**
