# Firebase Migration Guide for Nof1 App

This guide walks you through migrating your Nof1 app from local Room database storage to Firebase cloud storage.

## Overview

The migration implements a **hybrid approach** that combines:
- **Local storage (Room)** for offline functionality
- **Cloud storage (Firebase Firestore)** for cross-device sync
- **Firebase Authentication** for user management

## Migration Strategy

### Phase 1: Firebase Project Setup

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create new project or use existing one
   - Add Android app with package name `com.nof1`

2. **Download Configuration**
   - Download `google-services.json`
   - Place it in `app/` directory

3. **Enable Firebase Services**
   - **Authentication**: Enable Email/Password and Anonymous providers
   - **Firestore**: Create database in test mode initially
   - **Storage**: (Optional) For future file uploads

### Phase 2: Dependencies and Configuration

The following dependencies have been added to your project:

```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
```

### Phase 3: Data Model Migration

The migration creates Firebase-compatible versions of your existing data models:

- `Project` → `FirebaseProject`
- `Hypothesis` → `FirebaseHypothesis` 
- `Experiment` → `FirebaseExperiment`
- `LogEntry` → `FirebaseLogEntry`

### Phase 4: Repository Pattern Update

Three repository types are now available:

1. **Local Repositories** (existing): `ProjectRepository`, `HypothesisRepository`, etc.
2. **Firebase Repositories**: `FirebaseProjectRepository`, etc.
3. **Hybrid Repositories**: `HybridProjectRepository` - combines local + cloud

## Implementation Details

### Authentication Flow

1. User launches app
2. If not authenticated → show `AuthScreen`
3. User can:
   - Sign in with email/password
   - Create new account
   - Try anonymously (demo mode)
4. Once authenticated → proceed to main app

### Data Synchronization

The hybrid repository implements **offline-first** strategy:

1. **Write operations**: Save locally first, then sync to cloud
2. **Read operations**: Return local data, update from cloud in background
3. **Conflict resolution**: Local data takes precedence during active session

### Security Rules

Firestore security rules ensure:
- Users can only access their own data
- Authentication is required for all operations
- Data validation on server side

## Migration Steps

### Step 1: Setup Firebase Project
```bash
# 1. Create Firebase project at console.firebase.google.com
# 2. Add Android app
# 3. Download google-services.json to app/ directory
# 4. Enable Authentication (Email/Password, Anonymous)
# 5. Create Firestore database
```

### Step 2: Deploy Security Rules
```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize project
firebase init firestore

# Deploy rules
firebase deploy --only firestore:rules
```

### Step 3: Update ViewModels

Gradually update your ViewModels to use hybrid repositories:

```kotlin
// Before
class ProjectViewModel(
    private val repository: ProjectRepository
) : ViewModel()

// After
class ProjectViewModel(
    private val repository: HybridProjectRepository
) : ViewModel()
```

### Step 4: Add Authentication

1. Update `MainActivity` to check authentication state
2. Show `AuthScreen` if not authenticated
3. Navigate to main app after successful auth

### Step 5: Data Migration

For existing users with local data:

```kotlin
// In your Application class or initial setup
suspend fun migrateExistingData() {
    if (authManager.isAuthenticated && isFirstTimeAfterUpdate()) {
        hybridProjectRepository.syncToCloud()
        markMigrationComplete()
    }
}
```

## Usage Examples

### Creating a Project

```kotlin
// Hybrid repository automatically handles local + cloud storage
val projectId = hybridProjectRepository.insertProject(project)
```

### Reading Projects

```kotlin
// Returns local data immediately, syncs from cloud in background
hybridProjectRepository.getActiveProjects().collect { projects ->
    // Update UI with projects
}
```

### Handling Offline Mode

```kotlin
// The hybrid repository gracefully handles offline scenarios:
// - Writes: Save locally, queue for cloud sync
// - Reads: Return local data
// - Sync: Automatic when connection restored
```

## Testing

### Test Firebase Integration

1. **Authentication**: Test login/logout flows
2. **Data Sync**: Create data on one device, verify on another
3. **Offline Mode**: Disable internet, verify app works
4. **Conflict Resolution**: Modify same data on different devices

### Local Testing

```bash
# Run local Firestore emulator
firebase emulators:start --only firestore

# Update app to use emulator (for development)
```

## Monitoring and Analytics

Firebase provides built-in monitoring:

1. **Authentication**: User engagement, sign-in methods
2. **Firestore**: Read/write operations, performance
3. **Crashlytics**: Error reporting (add later)

## Best Practices

### Performance

1. **Offline Persistence**: Enable automatic caching
2. **Pagination**: Use Firestore query limits for large datasets
3. **Indexing**: Create composite indexes for complex queries

### Security

1. **Rules**: Start restrictive, gradually open as needed
2. **Data Validation**: Validate data on client AND server
3. **User Roles**: Plan for admin/premium features

### Cost Optimization

1. **Read Minimization**: Cache frequently accessed data
2. **Write Batching**: Group related operations
3. **Delete Cleanup**: Remove unused data regularly

## Rollback Plan

If issues arise, you can rollback by:

1. Switch ViewModels back to local repositories
2. Keep Room database intact
3. Disable Firebase dependencies
4. App continues working with local data only

## Future Enhancements

Once Firebase integration is stable:

1. **Real-time Updates**: Use Firestore listeners for live sync
2. **File Storage**: Store images/documents in Firebase Storage
3. **Cloud Functions**: Add server-side logic
4. **Analytics**: Track user behavior and app performance
5. **Remote Config**: Feature flags and A/B testing

## Troubleshooting

### Common Issues

1. **Auth Errors**: Check Firebase project configuration
2. **Permission Denied**: Verify Firestore security rules
3. **Network Issues**: Ensure offline persistence is enabled
4. **Build Errors**: Check google-services.json placement

### Debug Steps

```kotlin
// Enable Firestore debugging
FirebaseFirestore.setLoggingEnabled(true)

// Check authentication state
Log.d("Auth", "User: ${FirebaseAuth.getInstance().currentUser}")

// Verify data structure
FirebaseFirestore.getInstance()
    .collection("projects")
    .get()
    .addOnSuccessListener { documents ->
        for (document in documents) {
            Log.d("Firestore", "${document.id} => ${document.data}")
        }
    }
```

## Support and Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Android Guide](https://firebase.google.com/docs/firestore/quickstart)
- [Firebase Auth Android](https://firebase.google.com/docs/auth/android/start)
- [Security Rules Reference](https://firebase.google.com/docs/rules)

## Conclusion

This migration provides:
- ✅ Cloud storage and sync
- ✅ User authentication
- ✅ Offline functionality
- ✅ Scalable architecture
- ✅ Data security
- ✅ Cross-device compatibility

The hybrid approach ensures your app remains functional even during network issues while providing the benefits of cloud storage and synchronization. 