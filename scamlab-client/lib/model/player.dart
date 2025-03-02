import 'dart:convert';

class Player {
  final String secondaryId;
  final String systemRole;
  final String jwtToken;

  Player({
    required this.secondaryId,
    required this.systemRole,
    required this.jwtToken,
  });

  factory Player.fromJson(Map<String, dynamic> json) {
    return Player(
      secondaryId: base64Url.encode(utf8.encode(json['secondaryId'])),
      systemRole: json['systemRole'],
      jwtToken: json['jwtToken'],
    );
  }
}
