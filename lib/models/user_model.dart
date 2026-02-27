// lib/models/user_model.dart
// Data models for the application

class User {
  final String id;
  final String fullName;
  final String email;
  final String? profileImage;
  final DateTime createdAt;
  final bool isVerified;

  User({
    required this.id,
    required this.fullName,
    required this.email,
    this.profileImage,
    required this.createdAt,
    required this.isVerified,
  });

  // Convert to JSON
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'fullName': fullName,
      'email': email,
      'profileImage': profileImage,
      'createdAt': createdAt.toIso8601String(),
      'isVerified': isVerified,
    };
  }

  // Create from JSON
  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as String,
      fullName: json['fullName'] as String,
      email: json['email'] as String,
      profileImage: json['profileImage'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      isVerified: json['isVerified'] as bool? ?? false,
    );
  }

  // Copy with updates
  User copyWith({
    String? id,
    String? fullName,
    String? email,
    String? profileImage,
    DateTime? createdAt,
    bool? isVerified,
  }) {
    return User(
      id: id ?? this.id,
      fullName: fullName ?? this.fullName,
      email: email ?? this.email,
      profileImage: profileImage ?? this.profileImage,
      createdAt: createdAt ?? this.createdAt,
      isVerified: isVerified ?? this.isVerified,
    );
  }
}

class AuthResponse {
  final String token;
  final String refreshToken;
  final User user;

  AuthResponse({
    required this.token,
    required this.refreshToken,
    required this.user,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      token: json['token'] as String,
      refreshToken: json['refreshToken'] as String,
      user: User.fromJson(json['user'] as Map<String, dynamic>),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'token': token,
      'refreshToken': refreshToken,
      'user': user.toJson(),
    };
  }
}

class VerificationResponse {
  final bool success;
  final String message;
  final String? code;

  VerificationResponse({
    required this.success,
    required this.message,
    this.code,
  });

  factory VerificationResponse.fromJson(Map<String, dynamic> json) {
    return VerificationResponse(
      success: json['success'] as bool,
      message: json['message'] as String,
      code: json['code'] as String?,
    );
  }
}

class PasswordResetRequest {
  final String email;

  PasswordResetRequest({required this.email});

  Map<String, dynamic> toJson() {
    return {'email': email};
  }
}

class PasswordResetConfirm {
  final String email;
  final String code;
  final String newPassword;

  PasswordResetConfirm({
    required this.email,
    required this.code,
    required this.newPassword,
  });

  Map<String, dynamic> toJson() {
    return {
      'email': email,
      'code': code,
      'newPassword': newPassword,
    };
  }
}

class ApiError {
  final int statusCode;
  final String message;
  final Map<String, dynamic>? errors;

  ApiError({
    required this.statusCode,
    required this.message,
    this.errors,
  });

  factory ApiError.fromJson(Map<String, dynamic> json) {
    return ApiError(
      statusCode: json['statusCode'] as int? ?? 500,
      message: json['message'] as String? ?? 'Unknown error',
      errors: json['errors'] as Map<String, dynamic>?,
    );
  }

  @override
  String toString() => message;
}

// Usage Example:
/*
// Creating a user
final user = User(
  id: '123',
  fullName: 'John Doe',
  email: 'john@example.com',
  createdAt: DateTime.now(),
  isVerified: false,
);

// Converting to JSON for API
final json = user.toJson();

// Creating from API response
final userFromApi = User.fromJson(apiResponse);

// Using copyWith to update
final updatedUser = user.copyWith(isVerified: true);

// Auth response
final authResponse = AuthResponse.fromJson(loginApiResponse);
final token = authResponse.token;
*/
