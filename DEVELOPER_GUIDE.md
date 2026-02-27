# 🎨 Wallet App Frontend - Complete Developer Guide

## 📋 Table of Contents
1. [Quick Start](#quick-start)
2. [File Structure](#file-structure)
3. [Screen Details](#screen-details)
4. [Customization Guide](#customization-guide)
5. [API Integration](#api-integration)
6. [Best Practices](#best-practices)

---

## 🚀 Quick Start

### Installation
```bash
# Navigate to project
cd flutter_application_1

# Get dependencies
flutter pub get

# Run on device/emulator
flutter run

# Run on web
flutter run -d chrome

# Run on iOS
flutter run -d ios

# Run on Android
flutter run -d android
```

### File Organization
```
flutter_application_1/
├── lib/
│   ├── main.dart                    # Entry point & routing
│   ├── constants/
│   │   ├── colors.dart              # Color palette
│   │   ├── text_styles.dart         # Typography
│   │   └── theme.dart               # Theme utilities
│   ├── screens/
│   │   ├── splash_screen.dart
│   │   ├── onboarding_screen.dart
│   │   ├── welcome_screen.dart
│   │   ├── register_screen.dart
│   │   ├── verify_code_screen.dart
│   │   ├── verified_screen.dart
│   │   ├── login_screen.dart
│   │   └── forgot_password_screen.dart
│   ├── widgets/
│   │   └── custom_widgets.dart      # Reusable widgets
│   ├── services/
│   │   └── api_service.dart         # API integration template
│   ├── models/                      # (To be added) Data models
│   ├── providers/                   # (To be added) State management
│   └── utils/                       # (To be added) Helper functions
└── pubspec.yaml
```

---

## 📱 Screen Details

### 1️⃣ Splash Screen (3 seconds)
**File**: `screens/splash_screen.dart`

Features:
- Auto-redirect after 3 seconds
- Centered logo with gradient background
- Smooth transition to onboarding

**Key Code**:
```dart
Timer(const Duration(seconds: 3), () {
  Navigator.pushReplacementNamed(context, '/onboarding');
});
```

**Customization**:
- Change duration: Modify `Duration(seconds: 3)`
- Change colors: Edit `backgroundColor`
- Change icon: Replace `Icons.wallet` with any icon

---

### 2️⃣ Onboarding (2 screens)
**File**: `screens/onboarding_screen.dart`

Features:
- PageView-based navigation
- Dot indicators
- Next/Get Started buttons
- Two customizable pages

**Adding More Pages**:
```dart
final List<OnboardingPage> pages = [
  OnboardingPage(
    title: 'Take hold of your finances',
    description: 'Start your financial journey',
    icon: Icons.hand_extended,
    backgroundColor: const Color(0xFF6495ED),
  ),
  // Add more pages here
];
```

---

### 3️⃣ Welcome Screen
**File**: `screens/welcome_screen.dart`

Features:
- Two-button layout
- Create Account & Log In options
- Branding display

**Styling**:
```dart
// Change button colors
backgroundColor: Color(0xFF6495ED),  // Change to your color
```

---

### 4️⃣ Registration (Multi-step)
**File**: `screens/register_screen.dart`

**Step 1**: Full Name & Email
**Step 2**: Password with validation
**Step 3**: Review details

**Validation Example**:
```dart
bool isValidEmail(String email) {
  return RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(email);
}

bool isStrongPassword(String password) {
  return password.length >= 8 &&
         RegExp(r'[A-Z]').hasMatch(password) &&
         RegExp(r'[0-9]').hasMatch(password) &&
         RegExp(r'[!@#\$%^&*]').hasMatch(password);
}
```

---

### 5️⃣ Email Verification
**File**: `screens/verify_code_screen.dart`

Features:
- 4-digit code input
- Auto-focus between fields
- Resend code option

**Customization** (Change code length):
```dart
// Change 4 to desired length
List.generate(4, (index) => ...)
```

---

### 6️⃣ Verified Screen
**File**: `screens/verified_screen.dart`

Simple success confirmation screen with auto-redirect to login.

---

### 7️⃣ Login Screen
**File**: `screens/login_screen.dart`

Features:
- Email & password inputs
- Remember me checkbox
- Forgot password link
- Social login placeholders
- Sign up link

---

### 8️⃣ Forgot Password (Multi-step)
**File**: `screens/forgot_password_screen.dart`

**Step 1**: Enter email
**Step 2**: Verify code
**Step 3**: Create new password

---

## 🎨 Customization Guide

### Change Color Scheme

**File**: `constants/colors.dart`

```dart
static const Color primary = Color(0xFF4169E1);  // Change primary
static const Color secondary = Color(0xFF6495ED); // Change secondary
static const Color darkBlue = Color.fromARGB(255, 104, 30, 215);
```

### Add Custom Fonts

1. **Download font** (e.g., from Google Fonts)
2. **Create folder**: `assets/fonts/`
3. **Add fonts**: Place `.ttf` files in folder
4. **Update `pubspec.yaml`**:

```yaml
flutter:
  assets:
    - assets/
  fonts:
    - family: Poppins
      fonts:
        - asset: assets/fonts/Poppins-Regular.ttf
        - asset: assets/fonts/Poppins-Bold.ttf
          weight: 700
        - asset: assets/fonts/Poppins-Light.ttf
          weight: 300
```

5. **Run**: `flutter pub get`

### Apply Custom Font

```dart
ThemeData(
  fontFamily: 'Poppins',  // Apply to entire app
)

// Or for specific widgets:
Text('Hello', style: TextStyle(fontFamily: 'Poppins'))
```

### Add Custom Icons

```dart
// Place SVG files in assets/icons/
// Use flutter_svg package

import 'package:flutter_svg/flutter_svg.dart';

SvgPicture.asset('assets/icons/wallet.svg')
```

### Modify Text Styles

**File**: `constants/text_styles.dart`

```dart
static const TextStyle heading1 = TextStyle(
  fontSize: 32,        // Change font size
  fontWeight: FontWeight.bold,
  color: Colors.black, // Change color
);
```

---

## 🔌 API Integration

### Setup

1. **Update base URL** in `services/api_service.dart`:
```dart
static const String baseUrl = 'https://your-api.com/api';
```

2. **Add http package** to `pubspec.yaml`:
```yaml
dependencies:
  http: ^1.1.0
```

3. **Run**: `flutter pub get`

### Integration Example - Login Screen

**File**: `screens/login_screen.dart`

```dart
import 'package:flutter_application_1/services/api_service.dart';

// In login button onPressed:
onPressed: () async {
  try {
    final result = await ApiService.loginUser(
      email: _emailController.text,
      password: _passwordController.text,
    );
    
    // Store token
    // SharedPreferences prefs = await SharedPreferences.getInstance();
    // prefs.setString('token', result['token']);
    
    // Navigate to home
    Navigator.pushReplacementNamed(context, '/home');
    
  } catch (e) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(e.toString()),
        backgroundColor: Colors.red,
      ),
    );
  }
}
```

### Add State Management

**Install provider**:
```bash
flutter pub add provider
```

**Create provider**:
```dart
// lib/providers/auth_provider.dart
import 'package:flutter/material.dart';

class AuthProvider with ChangeNotifier {
  String? _token;
  String? get token => _token;

  Future<void> login(String email, String password) async {
    try {
      // Call API
      final result = await ApiService.loginUser(
        email: email,
        password: password,
      );
      _token = result['token'];
      notifyListeners();
    } catch (e) {
      rethrow;
    }
  }
}
```

**Use provider**:
```dart
// In widget
Consumer<AuthProvider>(
  builder: (context, authProvider, child) {
    return ElevatedButton(
      onPressed: () {
        authProvider.login(email, password);
      },
      child: const Text('Login'),
    );
  },
)
```

---

## 🏗️ Best Practices

### 1. Error Handling

```dart
try {
  final result = await ApiService.loginUser(...);
} on SocketException {
  // Network error
  showError('No internet connection');
} on TimeoutException {
  // API timeout
  showError('Request timeout');
} catch (e) {
  // Generic error
  showError(e.toString());
}
```

### 2. Input Validation

```dart
bool validateEmail(String email) {
  final emailRegex = RegExp(
    r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$'
  );
  return emailRegex.hasMatch(email);
}

bool validatePassword(String password) {
  return password.length >= 8;
}
```

### 3. Secure Storage

```dart
// Add to pubspec.yaml
dependencies:
  flutter_secure_storage: ^9.0.0

// Store sensitive data
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

const storage = FlutterSecureStorage();
await storage.write(key: 'token', value: token);

// Retrieve
final token = await storage.read(key: 'token');
```

### 4. Code Organization

```
✅ Group related code
✅ Use meaningful variable names
✅ Add comments for complex logic
✅ Keep widgets focused (single responsibility)
✅ Extract reusable widgets
```

### 5. Performance

```dart
// Use const constructors
const Container(...)

// Use SingleChildScrollView for scrollable content
SingleChildScrollView(child: Column(...))

// Lazy load images
Image.network(..., fit: BoxFit.cover)

// Use ListView.builder for long lists
ListView.builder(
  itemCount: items.length,
  itemBuilder: (context, index) => ...
)
```

---

## 🧪 Testing

### Widget Testing

```dart
// test/login_screen_test.dart
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_application_1/screens/login_screen.dart';

void main() {
  testWidgets('Login screen has email field', (WidgetTester tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: LoginScreen(),
    ));

    expect(find.byType(TextField), findsWidgets);
  });
}
```

### Run Tests

```bash
flutter test
```

---

## 📦 Building & Deployment

### Android APK

```bash
flutter build apk --release
# Output: build/app/outputs/apk/release/app-release.apk
```

### iOS App

```bash
flutter build ios --release
```

### Web

```bash
flutter build web --release
# Output: build/web/
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| "Cannot send Null" error | Run `flutter clean && flutter pub get` |
| Font not showing | Ensure font is added to `pubspec.yaml` and rebuild |
| Widgets not updating | Use `hot restart` instead of `hot reload` |
| API calls failing | Check network connectivity and API endpoint |
| Memory issues | Use `.dispose()` for controllers and focus nodes |

---

## 📚 Useful Resources

- [Flutter Docs](https://flutter.dev/docs)
- [Material Design](https://material.io/design)
- [Dart Language](https://dart.dev/guides)
- [pub.dev Packages](https://pub.dev)

---

## 🔐 Pre-Production Checklist

- [ ] Replace API endpoints with production URLs
- [ ] Add proper error handling and logging
- [ ] Implement secure token storage
- [ ] Add certificate pinning
- [ ] Enable HTTPS for all API calls
- [ ] Add input validation on all forms
- [ ] Implement rate limiting
- [ ] Add app versioning
- [ ] Test on multiple devices
- [ ] Implement analytics
- [ ] Add crash reporting (Firebase Crashlytics)
- [ ] Review and fix security vulnerabilities

---

**Created**: January 21, 2026
**Version**: 1.0.0
**Author**: Flutter Development Team

Happy Coding! 💙🚀
