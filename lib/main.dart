
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Blocker',
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: Colors.red,
        fontFamily: 'sans',
      ),
      home: const BlockerHomePage(),
    );
  }
}

class BlockerHomePage extends StatefulWidget {
  const BlockerHomePage({super.key});

  @override
  State<BlockerHomePage> createState() => _BlockerHomePageState();
}

class _BlockerHomePageState extends State<BlockerHomePage> {
  static const MethodChannel platform =
      MethodChannel('com.homsfree.blocker/settings');

  static const String _keywordsKey = 'blocker_keywords';
  static const String _domainsKey = 'blocker_domains';

  List<String> _keywords = [];
  List<String> _domains = [];

  bool _loading = true;
  bool _blockingEnabled = true;

  @override
  void initState() {
    super.initState();
    _loadLists();
  }

  Future<void> _loadLists() async {
    final prefs = await SharedPreferences.getInstance();

    final keywords = prefs.getStringList(_keywordsKey) ?? [];
    final domains = prefs.getStringList(_domainsKey) ?? [];

    if (!mounted) return;

    setState(() {
      _keywords = keywords;
      _domains = domains;
      _loading = false;
    });
  }

  Future<void> _saveLists() async {
    final prefs = await SharedPreferences.getInstance();

    await prefs.setStringList(_keywordsKey, _keywords);
    await prefs.setStringList(_domainsKey, _domains);
  }

  Future<void> _addKeyword() async {
    final controller = TextEditingController();

    final value = await _showInputDialog(
      title: 'إضافة كلمة أو عبارة',
      hint: 'اكتب كلمة أو عبارة تريد حجبها',
      controller: controller,
    );

    controller.dispose();

    if (value == null || value.trim().isEmpty) return;

    final item = value.trim();

    if (_keywords.contains(item)) {
      _showMessage('هذه الكلمة أو العبارة موجودة بالفعل');
      return;
    }

    setState(() {
      _keywords.add(item);
    });

    await _saveLists();
  }

  Future<void> _addDomain() async {
    final controller = TextEditingController();

    final value = await _showInputDialog(
      title: 'إضافة رابط أو نطاق',
      hint: 'مثال: example.com',
      controller: controller,
    );

    controller.dispose();

    if (value == null || value.trim().isEmpty) return;

    String item = value.trim();

    // إزالة البروتوكول إن أدخله المستخدم.
    item = item
        .replaceFirst(RegExp(r'^https?://'), '')
        .replaceFirst(RegExp(r'^www\.'), '');

    // إزالة / في نهاية النطاق.
    item = item.split('/').first.trim();

    if (item.isEmpty) return;

    if (_domains.contains(item)) {
      _showMessage('هذا الرابط أو النطاق موجود بالفعل');
      return;
    }

    setState(() {
      _domains.add(item);
    });

    await _saveLists();
  }

  Future<String?> _showInputDialog({
    required String title,
    required String hint,
    required TextEditingController controller,
  }) {
    return showDialog<String>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(title),
          content: TextField(
            controller: controller,
            autofocus: true,
            textDirection: TextDirection.rtl,
            decoration: InputDecoration(
              hintText: hint,
              border: const OutlineInputBorder(),
            ),
            onSubmitted: (_) {
              Navigator.pop(context, controller.text);
            },
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('إلغاء'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, controller.text),
              child: const Text('إضافة'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _deleteKeyword(int index) async {
    setState(() {
      _keywords.removeAt(index);
    });

    await _saveLists();
  }

  Future<void> _deleteDomain(int index) async {
    setState(() {
      _domains.removeAt(index);
    });

    await _saveLists();
  }

  Future<void> _openAccessibilitySettings() async {
    try {
      await platform.invokeMethod('openAccessibilitySettings');
    } on PlatformException catch (e) {
      _showMessage('تعذر فتح إعدادات خدمة الحجب: ${e.message}');
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Directionality(
      textDirection: TextDirection.rtl,
      child: Scaffold(
        appBar: AppBar(
          title: const Text(
            'نظام الحجب الصارم',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          centerTitle: true,
        ),
        body: _loading
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildStatusCard(),
                  const SizedBox(height: 16),
                  _buildKeywordsSection(),
                  const SizedBox(height: 16),
                  _buildDomainsSection(),
                  const SizedBox(height: 16),
                  _buildAccessibilityButton(),
                ],
              ),
      ),
    );
  }

  Widget _buildStatusCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(
              _blockingEnabled
                  ? Icons.shield
                  : Icons.shield_outlined,
              size: 42,
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'الحجب الصارم',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _blockingEnabled
                        ? 'الحجب مفعّل'
                        : 'الحجب متوقف',
                  ),
                ],
              ),
            ),
            Switch(
              value: _blockingEnabled,
              onChanged: (value) {
                setState(() {
                  _blockingEnabled = value;
                });
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildKeywordsSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.text_fields),
                const SizedBox(width: 8),
                const Expanded(
                  child: Text(
                    'الكلمات والعبارات المحظورة',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                IconButton(
                  tooltip: 'إضافة',
                  onPressed: _addKeyword,
                  icon: const Icon(Icons.add_circle),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (_keywords.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Text(
                  'لم تتم إضافة أي كلمات بعد.',
                  style: TextStyle(color: Colors.grey),
                ),
              )
            else
              ..._keywords.asMap().entries.map(
                (entry) {
                  final index = entry.key;
                  final value = entry.value;

                  return ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.block),
                    title: Text(value),
                    trailing: IconButton(
                      tooltip: 'حذف',
                      icon: const Icon(Icons.delete_outline),
                      onPressed: () => _deleteKeyword(index),
                    ),
                  );
                },
              ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: _addKeyword,
                icon: const Icon(Icons.add),
                label: const Text('إضافة كلمة أو عبارة'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDomainsSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.language),
                const SizedBox(width: 8),
                const Expanded(
                  child: Text(
                    'الروابط والنطاقات المحظورة',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                IconButton(
                  tooltip: 'إضافة',
                  onPressed: _addDomain,
                  icon: const Icon(Icons.add_circle),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (_domains.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Text(
                  'لم تتم إضافة أي روابط بعد.',
                  style: TextStyle(color: Colors.grey),
                ),
              )
            else
              ..._domains.asMap().entries.map(
                (entry) {
                  final index = entry.key;
                  final value = entry.value;

                  return ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.public),
                    title: Text(
                      value,
                      textDirection: TextDirection.ltr,
                    ),
                    trailing: IconButton(
                      tooltip: 'حذف',
                      icon: const Icon(Icons.delete_outline),
                      onPressed: () => _deleteDomain(index),
                    ),
                  );
                },
              ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: _addDomain,
                icon: const Icon(Icons.add),
                label: const Text('إضافة رابط أو نطاق'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAccessibilityButton() {
    return FilledButton.icon(
      style: FilledButton.styleFrom(
        padding: const EdgeInsets.symmetric(vertical: 16),
      ),
      icon: const Icon(Icons.security),
      label: const Text(
        'فتح إعدادات خدمة الحجب',
        style: TextStyle(fontSize: 17),
      ),
      onPressed: _openAccessibilitySettings,
    );
  }
}
