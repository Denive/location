import 'package:flutter/material.dart';
import 'package:foreground_location/foreground_location.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: StreamBuilder<Object>(
            stream: ForegroundLocation.locationSteram,
            builder: (context, snapshot) {
              return Center(
                child: Text('Running on: ${snapshot.data}\n'),
              );
            }),
      ),
    );
  }
}
