import 'dart:async';

import 'package:flutter/services.dart';

class ForegroundLocation {
  static const EventChannel _eventChannel =
      EventChannel('foreground_location/event_handler');

  static Stream<String> get locationSteram =>
      _eventChannel.receiveBroadcastStream().map((event) => event.toString());
}
