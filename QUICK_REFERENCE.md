# 🎯 Quick Reference Guide

## 🚀 Run the App in 30 Seconds

```bash
cd flutter_application_1
flutter pub get
flutter run
```

---

## 📱 Screen Navigation Flow

```
Splash (3 sec) → Onboarding → Welcome
                                 ├→ Register → Verify → Verified → Login
                                 └→ Login (with Forgot Password option)
```

---

## 🎨 Customization Cheat Sheet

### Change Primary Color
**File**: `lib/constants/colors.dart`
```dart
static const Color primary = Color(0xFF4169E1);  // Change this
```

### Change App Name
**File**: `lib/main.dart`
```dart
title: 'Wallet App',  // Change this
```

### Add Custom Font
1. Add font files to `assets/fonts/`
2. Update `pubspec.yaml`:
```yaml
fonts:
  - family: YourFont
    fonts:
      - asset: assets/fonts/YourFont.ttf
```
3. Update `main.dart`:
```dart
fontFamily: 'YourFont',
```

### Change Splash Logo
**File**: `lib/screens/splash_screen.dart`
```dart
Icon(Icons.wallet, ...)  // Change Icons.wallet
```

### Change Button Text
Search for text in any screen file and update the string.

---

## 🔌 API Integration Quick Start

**File**: `lib/services/api_service.dart`

1. Update base URL:
```dart
static const String baseUrl = 'https://your-api.com/api';
```

2. Use in screen:
```dart
import 'package:flutter_application_1/services/api_service.dart';

onPressed: () async {
  try {
    final result = await ApiService.loginUser(
      email: 'user@email.com',
      password: 'password',
    );
    // Handle success
  } catch (e) {
    // Handle error
  }
}
```

---

## 📁 File Locations

| What | Where |
|------|-------|
| Screens | `lib/screens/` |
| Colors | `lib/constants/colors.dart` |
| Fonts | `lib/constants/text_styles.dart` |
| API calls | `lib/services/api_service.dart` |
| Reusable widgets | `lib/widgets/custom_widgets.dart` |
| Data models | `lib/models/user_model.dart` |
| Routing | `lib/main.dart` |

---

## 🎯 Common Tasks

### Add a New Screen
1. Create file: `lib/screens/new_screen.dart`
2. Add route in `lib/main.dart`:
```dart
'/new_route': (context) => const NewScreen(),
```
3. Navigate:
```dart
Navigator.pushNamed(context, '/new_route');
```

### Change Input Field Style
**File**: Screen file, in TextField
```dart
TextField(
  decoration: InputDecoration(
    filled: true,
    fillColor: Colors.grey[100],  // Change color
    border: OutlineInputBorder(...),
  ),
)
```

### Add Input Validation
```dart
if (_emailController.text.isEmpty) {
  ScaffoldMessenger.of(context).showSnackBar(
    const SnackBar(content: Text('Email required')),
  );
  return;
}
```

### Show Error Message
```dart
ScaffoldMessenger.of(context).showSnackBar(
  SnackBar(
    content: const Text('Error occurred'),
    backgroundColor: Colors.red,
  ),
);
```

---

## 🔑 Key Widgets Used

| Widget | Purpose | File |
|--------|---------|------|
| Scaffold | Main layout | All screens |
| AppBar | Top bar | Screens with header |
| TextField | Input field | All form screens |
| ElevatedButton | Primary button | All screens |
| OutlinedButton | Secondary button | Welcome screen |
| PageView | Screen swiping | Onboarding, Registration |
| Container | Grouping | All screens |
| Column | Vertical layout | All screens |
| Row | Horizontal layout | Various screens |
| Checkbox | Checkbox input | Login screen |
| Icon | Icon display | All screens |

---

## 🎨 Color Usage

```dart
// Primary actions
backgroundColor: AppColors.primary,  // #4169E1

// Backgrounds
backgroundColor: AppColors.darkBlue,  // #681DB7

// Input fields
fillColor: Colors.grey[100],

// Success
backgroundColor: AppColors.accent,  // #1E90FF

// Text
color: Colors.black,
color: Colors.grey[600],
```

---

## 📤 Build & Deploy

### Android APK
```bash
flutter build apk --release
```
Output: `build/app/outputs/apk/release/app-release.apk`

### iOS App
```bash
flutter build ios --release
```

### Web
```bash
flutter build web --release
```
Output: `build/web/`

---

## 🐛 Troubleshooting

| Issue | Fix |
|-------|-----|
| App won't run | `flutter clean && flutter pub get` |
| Cannot send Null | Same as above |
| Hot reload not working | Use hot restart (Shift+R) |
| Font not showing | Add to pubspec.yaml and rebuild |
| Import errors | Run `flutter pub get` |

---

## 🎓 Documentation Map

```
Start Here:
  ├─ DELIVERY_SUMMARY.md (Overview)
  ├─ PROJECT_STRUCTURE.md (Structure)
  ├─ DEVELOPER_GUIDE.md (Deep dive)
  └─ FILE_INVENTORY.md (All files)

This file: QUICK_REFERENCE.md (Cheat sheet)
```

---

## 💡 Pro Tips

1. **Use const constructors** for performance:
   ```dart
   const Text('Hello')  // Good
   Text('Hello')        // Less efficient
   ```

2. **Use SingleChildScrollView** for scrollable forms:
   ```dart
   SingleChildScrollView(
     child: Column(...),
   )
   ```

3. **Extract widgets** for reusability:
   ```dart
   Widget _buildButton() => ...
   ```

4. **Use MediaQuery** for responsive design:
   ```dart
   MediaQuery.of(context).size.width
   ```

5. **Use ThemeData** for consistent styling:
   ```dart
   Theme.of(context).colorScheme.primary
   ```

---

## 🔐 Security Notes

Before production, ensure:
- [ ] API uses HTTPS
- [ ] Tokens stored securely
- [ ] Input validation on all forms
- [ ] Error messages don't expose sensitive info
- [ ] App signed with certificate

---

## 📊 Stats

| Metric | Value |
|--------|-------|
| Screens | 8 |
| Files | 18+ |
| Lines of Code | 2000+ |
| Colors | 10+ |
| Text Styles | 7 |
| Widgets | 4 |

---

## ❓ FAQ

**Q: How do I add more onboarding pages?**
A: Edit `lib/screens/onboarding_screen.dart`, add to `pages` list

**Q: How do I connect to my API?**
A: Edit `lib/services/api_service.dart`, update base URL

**Q: Can I use different colors?**
A: Yes, edit `lib/constants/colors.dart`

**Q: How do I add a new screen?**
A: Create file in `lib/screens/`, add route in `main.dart`

**Q: Is there state management?**
A: Not included, use Provider or Riverpod package

**Q: Can I deploy to app stores?**
A: Yes, after adding backend integration

---

## 🚀 Next Steps

1. ✅ Run the app
2. ✅ Explore all screens
3. ✅ Read DEVELOPER_GUIDE.md
4. ✅ Customize colors/fonts
5. ✅ Integrate your API
6. ✅ Add state management
7. ✅ Deploy to stores

---

## 📞 Quick Links

- Flutter Docs: https://flutter.dev/docs
- Material Design: https://material.io/design
- Dart Docs: https://dart.dev/guides
- pub.dev: https://pub.dev

---

**Version**: 1.0.0
**Last Updated**: January 21, 2026
**Status**: ✅ Ready to Use

**Happy Coding!** 💙🚀
