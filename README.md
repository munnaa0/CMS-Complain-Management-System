# CMS - Complaint Management System

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

## 📱 Overview

CMS (Complaint Management System) is an Android application designed to streamline the process of managing complaints and reports within institutions. The app features role-based access control with separate dashboards for managers and regular users, making it easy to submit, track, and manage reports efficiently.

Built with modern Android development practices, the app leverages Firebase for authentication and cloud-based data storage, ensuring real-time synchronization and secure data management.

## ✨ Features

### For Managers

- **Institution Management**: Create and manage institutions
- **Role Management**: Add and assign roles to users
- **Report Management**: View all reports and manage their status
- **User Oversight**: Monitor user activities and submitted reports
- **Comprehensive Dashboard**: Get an overview of all institution activities

### For Users

- **Join Institutions**: Request to join existing institutions
- **Submit Reports**: Create and submit complaints or reports
- **Track Reports**: View status and updates on submitted reports
- **Institution Details**: Access detailed information about joined institutions
- **User Dashboard**: Personalized view of your activities and reports

### Authentication & Security

- Secure user registration and login
- Firebase Authentication integration
- Role-based access control
- Internet permission for cloud synchronization

## 🏗️ Architecture

The application follows a modular architecture with clear separation of concerns:

```
├── Activities
│   ├── LoginActivity (Launcher)
│   ├── RegisterActivity
│   ├── ManagerDashboardActivity
│   ├── UserDashboardActivity
│   ├── CreateInstitutionActivity
│   ├── InstitutionDetailActivity
│   ├── UserInstitutionDetailActivity
│   ├── AddRolesActivity
│   ├── JoinInstitutionActivity
│   ├── SubmitReportActivity
│   ├── ViewMyReportsActivity
│   ├── ViewAllReportsActivity
│   └── ManageReportActivity
└── Firebase Integration
    ├── Firebase Authentication
    └── Cloud Firestore
```

## 🛠️ Tech Stack

- **Language**: Java 11
- **Min SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 36
- **Build System**: Gradle (Kotlin DSL)
- **Backend**: Firebase
  - Firebase Authentication v24.0.1
  - Cloud Firestore v26.0.2

### Dependencies

```gradle
- AndroidX AppCompat 1.7.1
- Material Design Components 1.13.0
- ConstraintLayout 2.2.1
- Firebase Authentication 24.0.1
- Firebase Firestore 26.0.2
```

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- [Android Studio](https://developer.android.com/studio) (Latest version recommended)
- JDK 11 or higher
- Android SDK with API 24 or higher
- Firebase account

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/munnaa0/CMS.git
cd CMS
```

### 2. Firebase Setup

1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
3. Register your app with package name: `com.example.cms`
4. Download the `google-services.json` file
5. Place the `google-services.json` file in the `app/` directory

**Note**: The repository includes placeholder `google-services.json` files. Replace them with your own configuration files.

### 3. Enable Firebase Services

In your Firebase Console, enable the following services:

- **Authentication**: Enable Email/Password sign-in method
- **Cloud Firestore**: Create a database in production mode (or test mode for development)

### 4. Configure Firestore Security Rules

Set up basic security rules in Firestore:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // Institutions collection
    match /institutions/{institutionId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }

    // Reports collection
    match /reports/{reportId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 5. Build and Run

1. Open the project in Android Studio
2. Let Gradle sync and download dependencies
3. Connect an Android device or start an emulator
4. Click **Run** or press `Shift + F10`

## 📱 Application Flow

### First-Time Users

1. Launch the app → **Login Screen**
2. Navigate to **Register** → Create account
3. Login with credentials
4. Choose user type (Manager/User)
5. Access respective dashboard

### Manager Flow

```
Login → Manager Dashboard → Create Institution → Add Roles → View All Reports → Manage Reports
```

### User Flow

```
Login → User Dashboard → Join Institution → Submit Report → View My Reports
```

## 🗂️ Project Structure

```
CMS/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/cms/
│   │   │   │   ├── LoginActivity.java
│   │   │   │   ├── RegisterActivity.java
│   │   │   │   ├── ManagerDashboardActivity.java
│   │   │   │   ├── UserDashboardActivity.java
│   │   │   │   └── ... (other activities)
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   ├── values/
│   │   │   │   └── mipmap/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── google-services.json
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🔧 Configuration

### Application ID

```gradle
applicationId = "com.example.cms"
```

### Version Information

- **Version Code**: 1
- **Version Name**: 1.0

### Permissions

The app requires the following permissions:

- `INTERNET` - For Firebase cloud services and real-time synchronization

## 🧪 Testing

The project includes test directories for both unit tests and instrumented tests:

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## 🐛 Troubleshooting

### Common Issues

**Issue**: Firebase initialization error

- **Solution**: Ensure `google-services.json` is properly placed in the `app/` directory and contains valid credentials

**Issue**: Build fails with dependency resolution error

- **Solution**: Sync project with Gradle files (`File → Sync Project with Gradle Files`)

**Issue**: Login/Registration not working

- **Solution**: Verify Firebase Authentication is enabled in Firebase Console

**Issue**: Data not syncing

- **Solution**: Check internet connection and Firestore security rules

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Contributors

- Md. Sultan Mahmud Munna - _Initial work_

## 🙏 Acknowledgments

- Firebase for backend services
- Material Design for UI components
- AndroidX libraries for compatibility

## 📞 Contact

For questions or support, please contact:

- Email: your.email@example.com
- GitHub: [@munnaa0](https://github.com/munnaa0)

## 🔮 Future Enhancements

- [ ] Push notifications for report updates
- [ ] Image attachment support for reports
- [ ] Advanced filtering and search functionality
- [ ] Report analytics and statistics
- [ ] Multi-language support
- [ ] Dark mode theme
- [ ] Export reports to PDF
- [ ] In-app messaging between users and managers

---

**Note**: This is an academic project developed as part of Software Development Project II coursework.

Made with ❤️ for efficient complaint management
