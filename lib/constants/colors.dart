import 'package:flutter/material.dart';

class AppColors {
  static const Color primary = Color(0xFF4169E1);
  static const Color secondary = Color(0xFF6495ED);
  static const Color accent = Color(0xFF1E90FF);
  static const Color darkBlue = Color.fromARGB(255, 104, 30, 215);
  static const Color white = Colors.white;
  static const Color black = Colors.black;
  static const Color lightGrey = Color(0xFFF5F5F5);
  static const Color mediumGrey = Color(0xFFE0E0E0);
  static const Color darkGrey = Color(0xFF808080);
  
  // Gradient colors
  static const LinearGradient primaryGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [primary, accent],
  );

  static const LinearGradient blueGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [secondary, accent],
  );
}
