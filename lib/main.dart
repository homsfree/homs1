import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('نظام الحجب الصارم', style: TextStyle(fontWeight: FontWeight.bold)),
          backgroundColor: Colors.redAccent,
          centerTitle: true,
        ),
        body: Center(
          child: ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.redAccent,
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 15),
            ),
            icon: const Icon(Icons.security, color: Colors.white),
            label: const Text('تفعيل خدمة الحجب', style: TextStyle(fontSize: 18, color: Colors.white)),
            onPressed: () {
              const platform = MethodChannel('com.homsfree.blocker/settings');
              platform.invokeMethod('openAccessibilitySettings');
            },
          ),
        ),
      ),
    );
  }
}
