// lib/services/api_service.dart
// Template for API integration
// To use this service:
// 1. Uncomment the import below
// 2. Add 'http: ^1.1.0' to pubspec.yaml
// 3. Run 'flutter pub get'

// import 'package:http/http.dart' as http;

/* ===== UNCOMMENT BELOW WHEN ADDING HTTP PACKAGE =====

class ApiService {
  static const String baseUrl = 'https://your-api-domain.com/api';

  // User Registration
  static Future<Map<String, dynamic>> registerUser({
    required String fullName,
    required String email,
    required String password,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/auth/register'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'fullName': fullName,
              'email': email,
              'password': password,
            }),
          )
          .timeout(const Duration(seconds: 30));

      if (response.statusCode == 201) {
        return jsonDecode(response.body);
      } else {
        throw Exception('Registration failed: ${response.body}');
      }
    } catch (e) {
      throw Exception('Error during registration: $e');
    }
  }

  // Email Verification
  static Future<Map<String, dynamic>> verifyEmail({
    required String email,
    required String code,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/auth/verify-email'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'email': email,
              'code': code,
            }),
          )
          .timeout(const Duration(seconds: 30));

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        throw Exception('Verification failed: ${response.body}');
      }
    } catch (e) {
      throw Exception('Error during verification: $e');
    }
  }

  // User Login
  static Future<Map<String, dynamic>> loginUser({
    required String email,
    required String password,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/auth/login'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'email': email,
              'password': password,
            }),
          )
          .timeout(const Duration(seconds: 30));

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        throw Exception('Login failed: ${response.body}');
      }
    } catch (e) {
      throw Exception('Error during login: $e');
    }
  }

  // Forgot Password - Send Reset Code
  static Future<Map<String, dynamic>> requestPasswordReset({
    required String email,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/auth/forgot-password'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'email': email}),
          )
          .timeout(const Duration(seconds: 30));

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        throw Exception('Reset request failed: ${response.body}');
      }
    } catch (e) {
      throw Exception('Error requesting password reset: $e');
    }
  }

  // Verify Reset Code and Update Password
  static Future<Map<String, dynamic>> resetPassword({
    required String email,
    required String code,
    required String newPassword,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/auth/reset-password'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'email': email,
              'code': code,
              'newPassword': newPassword,
            }),
          )
          .timeout(const Duration(seconds: 30));

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        throw Exception('Password reset failed: ${response.body}');
      }
    } catch (e) {
      throw Exception('Error resetting password: $e');
    }
  }

  // Resend Verification Code
  static Future<Map<String, dynamic>> resendVerificationCode({
    required String email,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/auth/resend-code'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'email': email}),
          )
          .timeout(const Duration(seconds: 30));

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        throw Exception('Resend code failed: ${response.body}');
      }
    } catch (e) {
      throw Exception('Error resending verification code: $e');
    }
  }
}

===== UNCOMMENT ABOVE WHEN ADDING HTTP PACKAGE ===== */

// Placeholder class when http package is not available
class ApiService {
  static Future<Map<String, dynamic>> registerUser({
    required String fullName,
    required String email,
    required String password,
  }) async {
    throw Exception('HTTP package not configured. See comments at top of file.');
  }

  static Future<Map<String, dynamic>> verifyEmail({
    required String email,
    required String code,
  }) async {
    throw Exception('HTTP package not configured. See comments at top of file.');
  }

  static Future<Map<String, dynamic>> loginUser({
    required String email,
    required String password,
  }) async {
    throw Exception('HTTP package not configured. See comments at top of file.');
  }

  static Future<Map<String, dynamic>> requestPasswordReset({
    required String email,
  }) async {
    throw Exception('HTTP package not configured. See comments at top of file.');
  }

  static Future<Map<String, dynamic>> resetPassword({
    required String email,
    required String code,
    required String newPassword,
  }) async {
    throw Exception('HTTP package not configured. See comments at top of file.');
  }

  static Future<Map<String, dynamic>> resendVerificationCode({
    required String email,
  }) async {
    throw Exception('HTTP package not configured. See comments at top of file.');
  }
}
