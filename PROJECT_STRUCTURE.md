# Wallet App - Flutter Frontend

A complete Flutter wallet application frontend with multi-screen authentication flow, matching the provided Figma design.

## 📁 Project Structure

```
lib/
├── main.dart                          # App entry point with routing
├── constants/
│   ├── colors.dart                    # Color definitions
│   ├── text_styles.dart               # Text style constants
│   └── theme.dart                     # Theme utilities
├── screens/
│   ├── splash_screen.dart             # Splash screen (3 seconds)
│   ├── onboarding_screen.dart         # Onboarding flow (2 screens)
│   ├── welcome_screen.dart            # Welcome screen
│   ├── register_screen.dart           # Multi-step registration
│   ├── verify_code_screen.dart        # Email verification
│   ├── verified_screen.dart           # Success screen
│   ├── login_screen.dart              # Login screen
│   └── forgot_password_screen.dart    # Password recovery flow
└── widgets/
    └── custom_widgets.dart            # Reusable custom widgets
```

## 🎨 Features Implemented

### 1. **Splash Screen** (`splash_screen.dart`)
- 3-second auto-redirect to onboarding
- Wallet branding with icon
- Smooth gradient background

### 2. **Onboarding Screens** (`onboarding_screen.dart`)
- Multi-page onboarding with PageView
- Page indicators and dot navigation
- Two onboarding screens:
  - "Take hold of your finances"
  - "See where your money is going"

### 3. **Welcome Screen** (`welcome_screen.dart`)
- Entry point for new users
- Two action buttons: "Create Account" & "Log In"
- Wallet branding

### 4. **Registration Flow** (`register_screen.dart`)
- Multi-step registration (3 steps):
  1. Full Name & Email
  2. Password setup with requirements
  3. Review details
- Password validation checklist
- Form validation feedback

### 5. **Email Verification** (`verify_code_screen.dart`)
- 4-digit code input fields
- Auto-focus between fields
- Resend code option

### 6. **Verification Success** (`verified_screen.dart`)
- Success confirmation screen
- Auto-redirect to login

### 7. **Login Screen** (`login_screen.dart`)
- Email and password inputs
- "Remember me" checkbox
- "Forgot Password" link
- Social login placeholders
- Sign up link

### 8. **Forgot Password** (`forgot_password_screen.dart`)
- Multi-step password recovery:
  1. Enter email
  2. Verify code
  3. Create new password
- Confirmation step with new password verification

## 🎯 Color Palette

| Color | Hex | Usage |
|-------|-----|-------|
| Primary Blue | #4169E1 | Buttons, links |
| Secondary Blue | #6495ED | Accents |
| Accent Blue | #1E90FF | Verified screen |
| Dark Purple | #681DB7 | Splash/Welcome bg |
| White | #FFFFFF | Text, backgrounds |
| Light Grey | #F5F5F5 | Input backgrounds |

## 📱 Screen Flow

```
Splash Screen (3s)
    ↓
Onboarding (2 screens)
    ↓
Welcome Screen
    ├─→ Create Account
    │    ├─→ Register Step 1
    │    ├─→ Register Step 2
    │    ├─→ Register Step 3
    │    └─→ Verify Code → Verified → Login
    │
    └─→ Log In
         ├─→ Login Form
         └─→ Forgot Password
              ├─→ Verify Code
              └─→ New Password
```

## 🚀 Getting Started

### Prerequisites
- Flutter SDK (3.0.0 or higher)
- Dart SDK (3.0.0 or higher)

### Installation

1. **Clone/Extract Project**
```bash
cd flutter_application_1
```

2. **Get Dependencies**
```bash
flutter pub get
```

3. **Run the App**
```bash
flutter run
```

For web:
```bash
flutter run -d chrome
```

## 📦 Dependencies

Current `pubspec.yaml` includes:
- `flutter/material.dart` (Built-in)
- `flutter/cupertino.dart` (Built-in)

No external dependencies required for basic functionality!

## 🎨 Customization

### Add Custom Fonts
1. Add font files to `assets/fonts/`
2. Update `pubspec.yaml`:
```yaml
fonts:
  - family: Poppins
    fonts:
      - asset: assets/fonts/Poppins-Regular.ttf
      - asset: assets/fonts/Poppins-Bold.ttf
        weight: 700
```

### Modify Colors
Edit `lib/constants/colors.dart` to change:
- Primary/Secondary colors
- Gradient definitions
- Accent colors

### Add Animations
Use Flutter's built-in animation widgets:
- `AnimationController`
- `Tween`
- `SlideTransition`
- `FadeTransition`

## 🔧 API Integration Points

Ready to connect:
1. **Login Screen** - `/auth/login` endpoint
2. **Register Screen** - `/auth/register` endpoint
3. **Verify Code** - `/auth/verify-code` endpoint
4. **Forgot Password** - `/auth/reset-password` endpoint

Example integration in `login_screen.dart`:
```dart
onPressed: () async {
  final response = await http.post(
    Uri.parse('https://your-api.com/auth/login'),
    body: {
      'email': _emailController.text,
      'password': _passwordController.text,
    },
  );
  // Handle response
}
```

## 📝 Code Examples

### Creating a Custom Button
```dart
import 'package:flutter_application_1/widgets/custom_widgets.dart';

CustomWidgets.buildCustomButton(
  label: 'Next',
  onPressed: () => Navigator.push(context, ...),
  backgroundColor: AppColors.primary,
)
```

### Using Color Constants
```dart
import 'package:flutter_application_1/constants/colors.dart';

Container(
  color: AppColors.primary,
  child: Text(
    'Hello',
    style: TextStyle(color: AppColors.white),
  ),
)
```

## 🐛 Common Issues

### Error: "Cannot send Null"
**Solution**: This is a debug service issue. Run:
```bash
flutter clean
flutter pub get
flutter run -d chrome
```

### Widgets not updating after hot reload
**Solution**: Use `hot restart` instead:
- In IDE: Press `Shift + R`
- Or: Type `R` in terminal

### Font not showing
**Solution**: Ensure fonts are added to `pubspec.yaml` and run `flutter pub get`

## 📱 Testing

Test the complete flow:
1. ✅ Splash screen auto-redirect
2. ✅ Onboarding navigation
3. ✅ Registration multi-step form
4. ✅ Email verification flow
5. ✅ Login with remember me
6. ✅ Forgot password flow

## 🔐 Security Notes

Before production:
1. Add proper input validation
2. Implement HTTPS for API calls
3. Store tokens securely (use `flutter_secure_storage`)
4. Add encryption for sensitive data
5. Implement certificate pinning
6. Add rate limiting on login attempts

## 📚 Resources

- [Flutter Documentation](https://flutter.dev/docs)
- [Material Design Guidelines](https://material.io)
- [Dart Language Guide](https://dart.dev/guides)

## 👨‍💻 Development Tips

- Use `flutter analyze` to check code quality
- Format code: `flutter format lib/`
- Build APK: `flutter build apk`
- Build iOS: `flutter build ios`
- Web build: `flutter build web`

## 📄 License

This project is provided as-is for development purposes.

## 🤝 Next Steps

1. **Add Backend Integration**: Connect to your authentication API
2. **Add State Management**: Implement Provider, Riverpod, or GetX
3. **Add Error Handling**: Implement proper error messages and logging
4. **Add Animations**: Enhance UX with custom animations
5. **Add Local Storage**: Use `shared_preferences` or `hive`
6. **Testing**: Add unit and widget tests

---

**Created with Flutter** 💙

Version: 1.0.0
Last Updated: January 21, 2026
