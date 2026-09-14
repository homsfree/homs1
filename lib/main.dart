import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const BlockerApp());
}

class BlockerApp extends StatelessWidget {
  const BlockerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(primarySwatch: Colors.teal),
      home: const ControlPanelScreen(),
    );
  }
}

class ControlPanelScreen extends StatefulWidget {
  const ControlPanelScreen({super.key});

  @override
  _ControlPanelScreenState createState() => _ControlPanelScreenState();
}

class _ControlPanelScreenState extends State<ControlPanelScreen> {
  static const platform = MethodChannel('com.homsfree.blocker/blur_engine');
  
  bool _blurImages = true;
  bool _blurVideos = true;

  Future<void> _updateSettings(String key, bool value) async {
    try {
      await platform.invokeMethod('updateSettings', {key: value});
    } on PlatformException catch (e) {
      print("خطأ: ${e.message}");
    }
  }

  Future<void> _enableAdmin() async {
    try {
      await platform.invokeMethod('requestAdmin');
    } on PlatformException catch (e) {
      print("خطأ: ${e.message}");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('درع الحماية الذكي', style: TextStyle(fontWeight: FontWeight.bold)),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: ListView(
          children: [
            const Text('إعدادات الذكاء الاصطناعي للتشويش', style: TextStyle(fontSize: 14, color: Colors.grey, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            SwitchListTile(
              title: const Text('تشويش الصور العارية فوراً', style: TextStyle(fontWeight: FontWeight.bold)),
              value: _blurImages,
              activeColor: Colors.teal,
              onChanged: (val) {
                setState(() => _blurImages = val);
                _updateSettings('blurImages', val);
              },
            ),
            const Divider(),
            SwitchListTile(
              title: const Text('تشويش مقاطع الفيديو', style: TextStyle(fontWeight: FontWeight.bold)),
              value: _blurVideos,
              activeColor: Colors.teal,
              onChanged: (val) {
                setState(() => _blurVideos = val);
                _updateSettings('blurVideos', val);
              },
            ),
            const Divider(),
            const SizedBox(height: 20),
            const Text('حماية النظام', style: TextStyle(fontSize: 14, color: Colors.grey, fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            Card(
              elevation: 3,
              child: Padding(
                padding: const EdgeInsets.all(12.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('ميزة منع إلغاء التثبيت', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    const SizedBox(height: 5),
                    const Text('يمنع حذف التطبيق إلا بكلمة المرور.', style: TextStyle(fontSize: 13, color: Colors.black54)),
                    const SizedBox(height: 15),
                    ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.teal, foregroundColor: Colors.white),
                      onPressed: _enableAdmin,
                      icon: const Icon(Icons.admin_panel_settings),
                      label: const Text('تفعيل حماية عدم الحذف'),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
