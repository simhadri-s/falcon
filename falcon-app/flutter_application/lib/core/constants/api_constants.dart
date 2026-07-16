import 'package:flutter_dotenv/flutter_dotenv.dart';

class ApiConstants {
  static String get hostUrl => dotenv.env['HOST_URL'] ?? 'http://192.168.1.36:8080';
  static String get baseUrl => '$hostUrl/api';
}

