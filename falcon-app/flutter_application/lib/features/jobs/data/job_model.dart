class Job {
  final String id;
  final String title;
  final String department;
  final String location;
  final String type;
  final String description;
  final bool isActive;
  final DateTime createdAt;

  Job({
    required this.id,
    required this.title,
    required this.department,
    required this.location,
    required this.type,
    required this.description,
    required this.isActive,
    required this.createdAt,
  });

  factory Job.fromJson(Map<String, dynamic> json) {
    return Job(
      id: json['_id'] ?? json['id'] ?? '',
      title: json['title'] ?? '',
      department: json['department'] ?? '',
      location: json['location'] ?? '',
      type: json['type'] ?? '',
      description: json['description'] ?? '',
      isActive: json['isActive'] ?? true,
      createdAt: json['createdAt'] != null 
          ? DateTime.parse(json['createdAt']) 
          : DateTime.now(),
    );
  }
}
