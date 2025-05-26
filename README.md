# Nof1 - Personal Experiment Tracker

A simple, elegant Android app that helps users run structured projects for achieving goals through systematic experimentation.

## Overview

Nof1 follows a hierarchical structure designed to help users systematically test and validate their hypotheses:

- **Projects** contain multiple **Hypotheses**
- **Hypotheses** contain multiple **Experiments**  
- Users set goals via Projects, refine them through Hypotheses, and test them via Experiments
- **Log Entries** capture responses to experiment notifications

## Features

### Core Functionality
- ✅ Create, edit, archive, and delete Projects, Hypotheses, and Experiments
- ✅ Hierarchical data structure (Project → Hypothesis → Experiment → Log Entry)
- ✅ Quick logging interface for experiment responses
- ✅ Toggle visibility of archived/completed items
- ✅ Clean MVVM architecture with Room database

### Notification System
- ✅ Configurable experiment notifications (daily, weekly, custom frequency)
- ✅ Smart notification skipping (if user logs manually before notification)
- ✅ WorkManager-based scheduling for reliable delivery
- ✅ Boot-completed receiver for notification persistence

### UI/UX
- ✅ Minimalist Material Design 3 interface
- ✅ Jetpack Compose UI with intuitive navigation
- ✅ Fast, single-tap logging workflow
- ✅ Contextual actions and confirmation dialogs

## Technical Architecture

### Technology Stack
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room with SQLite
- **Notifications**: WorkManager + AlarmManager
- **Navigation**: Navigation Compose
- **Language**: Kotlin

### Project Structure
```
app/src/main/java/com/nof1/
├── data/
│   ├── model/          # Data classes (Project, Hypothesis, Experiment, LogEntry)
│   ├── local/          # Room database, DAOs, converters
│   └── repository/     # Repository pattern for data access
├── ui/
│   ├── screens/        # Compose screens
│   ├── components/     # Reusable UI components
│   ├── navigation/     # Navigation setup
│   └── theme/          # Material Design theme
├── viewmodel/          # ViewModels for UI state management
├── utils/              # Notification helpers and workers
├── MainActivity.kt     # Main activity
└── Nof1Application.kt  # Application class
```

### Database Schema
- **Projects**: id, name, description, goal, isArchived, timestamps
- **Hypotheses**: id, projectId, name, description, isArchived, timestamps
- **Experiments**: id, hypothesisId, name, description, question, notification settings, timestamps
- **LogEntries**: id, experimentId, response, createdAt, isFromNotification

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.8+

### Building the App
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device or emulator

```bash
git clone <repository-url>
cd nof1
./gradlew build
```

### Installation
- Install directly from Android Studio
- Or build APK: `./gradlew assembleDebug`

## Usage

### Basic Workflow
1. **Create a Project** - Define your overall goal
2. **Add Hypotheses** - Break down your goal into testable hypotheses  
3. **Create Experiments** - Design specific tests for each hypothesis
4. **Configure Notifications** - Set up reminders for regular logging
5. **Log Responses** - Quickly capture experiment results
6. **Review Progress** - Analyze your data over time

### Example Use Case
**Project**: "Improve Sleep Quality"
- **Hypothesis 1**: "Reducing screen time before bed improves sleep"
  - **Experiment**: "No screens 1 hour before bed"
  - **Question**: "How did you sleep last night? (1-10)"
- **Hypothesis 2**: "Exercise timing affects sleep quality"
  - **Experiment**: "Morning vs evening workouts"
  - **Question**: "Rate your sleep quality and note workout timing"

## Notification Features

### Smart Scheduling
- Notifications respect user preferences (time, frequency)
- Automatic skipping if user logs manually
- Persistent across device reboots
- Configurable per experiment

### Frequency Options
- **Daily**: Every day at specified time
- **Weekly**: Once per week
- **Custom**: Every N days

## Future Enhancements

### Planned Features
- [ ] Data visualization and analytics
- [ ] Export functionality (CSV, PDF)
- [ ] Cloud sync and backup
- [ ] AI-powered insights and recommendations
- [ ] Collaborative experiments
- [ ] Advanced statistical analysis
- [ ] Integration with health apps
- [ ] Dark mode support
- [ ] Widget for quick logging

### Extensibility
The app is designed for future extensibility:
- Modular architecture supports new features
- Repository pattern enables multiple data sources
- Clean separation of concerns
- Comprehensive test coverage (planned)

## Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests (when test framework is added)
5. Submit a pull request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add documentation for public APIs
- Maintain consistent formatting

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions, issues, or feature requests:
- Open an issue on GitHub
- Check existing documentation
- Review the code comments for implementation details

## Acknowledgments

- Material Design 3 for UI guidelines
- Jetpack Compose team for modern Android UI
- Room database for local persistence
- WorkManager for reliable background tasks 