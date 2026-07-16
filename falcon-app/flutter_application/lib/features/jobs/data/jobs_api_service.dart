import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_application/core/network/api_client.dart';
import 'package:http_parser/http_parser.dart';
import 'job_model.dart';
import 'package:flutter_application/core/error/api_exception.dart';
import 'dart:io';
import 'package:flutter_application/core/constants/api_constants.dart';

class PaginatedJobs {
  final List<Job> jobs;
  final int total;
  final int pages;

  PaginatedJobs({
    required this.jobs,
    required this.total,
    required this.pages,
  });
}

class JobsApiService {
  static String get baseUrl => ApiConstants.baseUrl;

  Future<PaginatedJobs> getJobs({
    int page = 0,
    int size = 10,
  }) async {
    try {
      final queryParameters = {
        'page': (page + 1).toString(),
        'size': size.toString(),
      };

      final uri = Uri.parse('$baseUrl/jobs').replace(queryParameters: queryParameters);
      final response = await ApiClient.get(uri);

      if (response.statusCode == 200) {
        final Map<String, dynamic> body = json.decode(response.body);
        final List<dynamic> data = body['data'] ?? [];
        final int total = body['total'] ?? 0;
        final int pages = body['pages'] ?? 1;

        return PaginatedJobs(
          jobs: data.map((json) => Job.fromJson(json)).toList(),
          total: total,
          pages: pages,
        );
      } else {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to load jobs: $e');
    }
  }

  Future<void> submitApplication({
    required String name,
    required String email,
    required String phone,
    required String jobId,
    required File cv,
    File? coverLetter,
  }) async {
    try {
      var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/applications'));
      
      request.fields['name'] = name;
      request.fields['email'] = email;
      request.fields['phone'] = phone;
      request.fields['jobId'] = jobId;

      String getContentType(String path) {
        if (path.endsWith('.pdf')) return 'pdf';
        if (path.endsWith('.doc')) return 'msword';
        if (path.endsWith('.docx')) return 'vnd.openxmlformats-officedocument.wordprocessingml.document';
        return 'octet-stream';
      }

      request.files.add(await http.MultipartFile.fromPath(
        'cv',
        cv.path,
        contentType: MediaType('application', getContentType(cv.path)),
      ));

      if (coverLetter != null) {
        request.files.add(await http.MultipartFile.fromPath(
          'coverLetter',
          coverLetter.path,
          contentType: MediaType('application', getContentType(coverLetter.path)),
        ));
      }

      var streamedResponse = await request.send();
      var response = await http.Response.fromStream(streamedResponse);

      if (response.statusCode != 201 && response.statusCode != 200) {
        throw ApiException.fromResponse(response);
      }
    } catch (e) {
      if (e is ApiException) rethrow;
      throw ApiException('Failed to submit application: $e');
    }
  }
}
