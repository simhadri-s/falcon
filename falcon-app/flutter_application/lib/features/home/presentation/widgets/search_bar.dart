import 'package:flutter/material.dart';
import 'dart:ui';
import 'package:speech_to_text/speech_to_text.dart' as stt;

class CustomSearchBar extends StatefulWidget {
  final ValueChanged<String>? onChanged;
  final TextEditingController? controller;
  final String? hintText;

  const CustomSearchBar({
    super.key,
    this.onChanged,
    this.controller,
    this.hintText,
  });

  @override
  State<CustomSearchBar> createState() => _CustomSearchBarState();
}

class _CustomSearchBarState extends State<CustomSearchBar> {
  late stt.SpeechToText _speech;
  bool _isListening = false;

  @override
  void initState() {
    super.initState();
    _speech = stt.SpeechToText();
  }

  Future<void> _listen() async {
    if (!_isListening) {
      try {
        bool available = await _speech.initialize(
          onStatus: (val) {
            if (val == 'notListening' || val == 'done') {
              if (mounted) {
                setState(() => _isListening = false);
              }
            }
          },
          onError: (val) {
            if (mounted) {
              setState(() => _isListening = false);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text("Voice search error: ${val.errorMsg}"),
                  backgroundColor: Colors.red.shade600,
                  behavior: SnackBarBehavior.floating,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
              );
            }
          },
        );

        if (available) {
          if (!mounted) return;
          setState(() => _isListening = true);
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: const Row(
                children: [
                  Icon(Icons.mic, color: Colors.white),
                  SizedBox(width: 8),
                  Text("Listening... Speak now"),
                ],
              ),
              backgroundColor: const Color(0xFFEF4444),
              duration: const Duration(seconds: 4),
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
          );

          _speech.listen(
            onResult: (val) {
              if (mounted) {
                setState(() {
                  if (widget.controller != null) {
                    widget.controller!.text = val.recognizedWords;
                    // Keep cursor at the end of text
                    widget.controller!.selection =
                        TextSelection.fromPosition(
                      TextPosition(offset: widget.controller!.text.length),
                    );
                  }
                  widget.onChanged?.call(val.recognizedWords);
                });
              }
            },
          );
        } else {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: const Text("Speech recognition not available"),
                backgroundColor: Colors.amber.shade700,
                behavior: SnackBarBehavior.floating,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            );
          }
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text("Failed to start voice search: $e"),
              backgroundColor: Colors.red.shade600,
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
          );
        }
      }
    } else {
      if (mounted) {
        setState(() => _isListening = false);
      }
      await _speech.stop();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 52,
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 24,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
          child: TextField(
            controller: widget.controller,
            onChanged: widget.onChanged,
            style: const TextStyle(fontSize: 15, color: Color(0xFF111827), fontWeight: FontWeight.w500),
            decoration: InputDecoration(
              hintText: widget.hintText ?? "Search products, news...",
              hintStyle: TextStyle(color: Colors.grey.shade400, fontSize: 15),
              prefixIcon: const Icon(Icons.search,
                  color: Color(0xFF0284C7), size: 22),
              suffixIcon: GestureDetector(
                onTap: _listen,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 300),
                  margin: const EdgeInsets.all(6),
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: _isListening
                        ? const Color(0xFFEF4444)
                        : const Color(0xFF0284C7),
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: _isListening
                        ? [
                            BoxShadow(
                              color: const Color(0xFFEF4444).withOpacity(0.4),
                              blurRadius: 10,
                              spreadRadius: 2,
                            )
                          ]
                        : [],
                  ),
                  child: Icon(
                    _isListening ? Icons.mic : Icons.mic_none,
                    color: Colors.white,
                    size: 20,
                  ),
                ),
              ),
              border: InputBorder.none,
              enabledBorder: InputBorder.none,
              focusedBorder: InputBorder.none,
              contentPadding:
                  const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
            ),
          ),
        ),
      ),
    );
  }
}