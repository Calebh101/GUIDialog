import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_highlight/flutter_highlight.dart';
import 'package:flutter_highlight/themes/github.dart';
import 'package:flutter_highlight/themes/monokai-sublime.dart';
import 'package:localpkg_flutter/localpkg.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Section sign
const String sec = "§";

class DialogAction {
  final Key key = UniqueKey();
  final TextEditingController id;
  final TextEditingController pretty;

  DialogAction({required this.id, required this.pretty});
}

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'GUIDialog',
      theme: .light(),
      darkTheme: .dark(),
      home: Home(),
    );
  }
}

class Home extends StatefulWidget {
  const Home({super.key});

  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  final title = TextEditingController();
  final body = TextEditingController();

  final key = FormKey();
  final actions = <DialogAction>[];

  String? calculated;
  List<String> warnings = [];
  bool changed = true;

  void calculate() {
    setState(() {
      changed = false;
      warnings.clear();

      if (title.text.isEmptyTrimmed) warnings.add("Title is empty.");
      if (title.text.contains("\n")) warnings.add("Title should only have one line.");
      if (body.text.isEmptyTrimmed) warnings.add("Body is empty.");

      for (final (i, action) in actions.indexed) {
        if (action.id.text.isEmptyTrimmed) warnings.add("Action #${i + 1} ID is empty.");
        if (!RegExp(r'^[a-zA-Z0-9_]+$').hasMatch(action.id.text)) warnings.add("Action #${i + 1} ID can only contain letters, numbers, or underscores.");
        if (action.pretty.text.isEmptyTrimmed) warnings.add("Action #${i + 1} name is empty.");
      }

      calculated = jsonEncode({"title": title.text, "body": body.text, "actions": Map.fromEntries(actions.map((x) => MapEntry(x.pretty.text, x.id.text)))});
    });
  }

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();

    title.text = prefs.getString("title")?.nullIfEmptyTrimmed ?? "";
    body.text = prefs.getString("body")?.nullIfEmptyTrimmed ?? "";
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("GUIDialog${changed ? "*" : ""}"),
        centerTitle: true,
        actions: [
          IconButton(onPressed: () async {
            final result = await showDialog<Map>(context: context, builder: (_) => LoaderDialog());
            if (result == null || !mounted) return;

            setState(() {
              title.text = result["title"];
              body.text = result["body"];
              actions.clear();

              for (final entry in (result["actions"] as Map).entries) {
                final key = entry.key as String;
                final value = entry.value as String;

                actions.add(DialogAction(id: TextEditingController(text: value), pretty: TextEditingController(text: key)));
              }
            });
          }, icon: Icon(Icons.download))
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Center(
          child: SingleChildScrollView(
            child: Column(
              spacing: 8,
              children: [
                TextField(
                  controller: title,
                  decoration: InputDecoration(
                    border: OutlineInputBorder(),
                    hintText: 'Title...',
                  ),
                  onChanged: (value) async {
                    setState(() => changed = true);
                    final prefs = await SharedPreferences.getInstance();
                    await prefs.setString("title", value);
                  },
                ),
                TextField(
                  minLines: 5,
                  maxLines: null,
                  controller: body,
                  decoration: InputDecoration(
                    border: OutlineInputBorder(),
                    hintText: 'Body...',
                  ),
                  onChanged: (value) async {
                    setState(() => changed = true);
                    final prefs = await SharedPreferences.getInstance();
                    await prefs.setString("body", value);
                  },
                ),
                ReorderableListView.builder(
                  shrinkWrap: true,
                  buildDefaultDragHandles: false,
                  itemCount: actions.length,
                  onReorderItem: (oldIndex, newIndex) {
                    setState(() {
                      final item = actions.removeAt(oldIndex);
                      actions.insert(newIndex, item);
                    });
                  },
                  itemBuilder: (context, i) {
                    final action = actions[i];

                    return ListTile(
                      key: action.key,
                      trailing: Row(
                        mainAxisSize: .min,
                        children: [
                          IconButton(onPressed: () {
                            setState(() {
                              actions.removeAt(i);
                              changed = true;
                            });
                          }, icon: Icon(Icons.delete), color: Colors.red),
                          ReorderableDelayedDragStartListener(
                            index: i,
                            child: Icon(Icons.drag_handle),
                          ),
                        ],
                      ),
                      title: Row(
                        mainAxisSize: .min,
                        children: [
                          Expanded(
                            child: TextField(
                              controller: action.pretty,
                              decoration: InputDecoration(
                                border: OutlineInputBorder(),
                                hintText: 'Pretty name...',
                              ),
                              onChanged: (value) => setState(() => changed = true),
                            ),
                          ),
                          Expanded(
                            child: TextField(
                              controller: action.id,
                              decoration: InputDecoration(
                                border: OutlineInputBorder(),
                                hintText: 'Action ID...',
                              ),
                              onChanged: (value) => setState(() => changed = true),
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
                Row(
                  mainAxisAlignment: .center,
                  children: [
                    TextButton(onPressed: calculate, child: Text("Generate")),

                    if (calculated != null) TextButton(onPressed: () {
                      Clipboard.setData(.new(text: calculated!));
                      showSnackBar(context, "Copied ${calculated?.length} characters!");
                    }, child: Text("Copy")),

                    TextButton(onPressed: () {
                      setState(() {
                        changed = true;
                        actions.add(DialogAction(id: TextEditingController(), pretty: TextEditingController()));
                      });
                    }, child: Text("Add Action")),

                    if (calculated != null) TextButton(onPressed: () {
                      Clipboard.setData(.new(text: sec));
                      showSnackBar(context, "Copied: $sec");
                    }, child: Text(sec)),
                  ],
                ),
                Column(
                  mainAxisSize: .min,
                  children: [
                    for (final warning in warnings) Text("Warning: $warning"),
                  ],
                ),
                if (calculated != null) HighlightView(
                  calculated!,
                  language: 'json',
                  theme: Theme.of(context).brightness == Brightness.dark ? monokaiSublimeTheme : githubTheme,
                  padding: .all(16),
                  textStyle: const TextStyle(
                    fontFamily: 'monospace',
                    fontSize: 14,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class LoaderDialog extends StatefulWidget {
  const LoaderDialog({super.key});

  @override
  State<LoaderDialog> createState() => _LoaderDialogState();
}

class _LoaderDialogState extends State<LoaderDialog> {
  final key = FormKey();
  final controller = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text("Import Dialog from JSON"),
      content: Form(
        key: key,
        child: TextFormField(
          controller: controller,
          decoration: .new(
            labelText: "JSON",
          ),
          validator: (value) {
            if (value == null || value.isEmptyTrimmed) return "Input must not be empty.";
            late Map data;

            try {
              final parsed = jsonDecode(value);
              if (parsed is! Map) return "JSON data must be an object.";
              data = parsed;
            } on FormatException catch (e) {
              return "Invalid JSON: ${e.message}";
            }

            if (data["title"] is! String) return "Key title is invalid: expected string, got ${data["title"].runtimeType}.";
            if (data["body"] is! String) return "Key body is invalid: expected string, got ${data["body"].runtimeType}.";
            if (data["actions"] is! Map) return "Key actions is invalid: expected object, got ${data["actions"].runtimeType}.";

            for (final entry in (data["actions"] as Map).entries) {
              if (entry.key is! String) return "Action key invalid: expected string, got ${entry.key.runtimeType}.";
              if (entry.value is! String) return "Action value invalid: expected string, got ${entry.value.runtimeType}.";
            }

            return null;
          },
        ),
      ),
      actions: [
        TextButton(onPressed: context.navigator.pop, child: Text("Cancel")),
        TextButton(onPressed: () {
          if (!key.validate()) return;
          context.navigator.pop(jsonDecode(controller.text));
        }, child: Text("OK")),
      ],
    );
  }
}