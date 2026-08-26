# Personal Finance Tracker

A personal finance tracker Android app focused on fast local/offline registration of incomes, expenses, budget-cycle information, fixed entries, pending entries, statistics, and quick actions through Android widgets.

## Features

- Offline-first: Core functionality works without internet
- Transaction tracking: Incomes and expenses
- Budget cycle management
- Fixed and pending entries
- Statistics and reports
- Home screen widgets for quick actions
- Dominican Peso (DOP / RD$) currency support
- Material Design 3 (Jetpack Compose)
- Dependency Injection with Hilt
- Local database with Room
- Background work with WorkManager (for fixed entries)
- Glance app widgets

## Architecture

The app follows a layered architecture similar to Clean Architecture + MVVM:

- **domain**: Business logic and models (pure Kotlin, no Android dependencies)
- **data**: Data layer (Room database, repositories)
- **presentation**: UI layer (Jetpack Compose, ViewModels)
- **navigation**: Navigation graph
- **ui**: Theming and appearance
- **widget**: Glance app widgets
- **di**: Dependency injection (Hilt)
- **core**: Utilities (money formatting, etc.)

## Technology Stack

- Kotlin
- Android Jetpack:
  - Compose (UI)
  - ViewModel
  - Room (Persistence)
  - WorkManager (Background tasks)
  - Navigation
  - Lifecycle
- Hilt (Dependency Injection)
- Glance (App widgets)
- Material Design 3
- Coroutines (for asynchronous operations)

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11
- Android SDK 24 (minimum SDK) and SDK 34 (compile SDK)

### Installation

1. Clone the repository
2. Open the project in Android Studio
3. Wait for Gradle sync to complete
4. Run the app on an emulator or physical device

### Building

To build the debug APK:

```bash
./gradlew assembleDebug
```

On Windows:

```bash
.\gradlew.bat assembleDebug
```

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

## Acknowledgments

- Inspired by the need for a simple, offline-first personal finance tracker.
- Built with modern Android development practices.