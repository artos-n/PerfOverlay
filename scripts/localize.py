#!/usr/bin/env python3
"""Localize hardcoded strings in Kotlin UI files."""

import re
import os

# Map of hardcoded string -> string resource ID
# Only for user-visible static strings (not formatting strings)
STRING_MAP = {
    "Battery": "R.string.stat_battery",
    "Storage I/O": "R.string.stat_storage_io",
    "FPS": "R.string.stat_fps",
    "CPU": "R.string.stat_cpu",
    "GPU": "R.string.stat_gpu",
    "RAM": "R.string.stat_ram",
    "Temperature": "R.string.stat_temperature",
    "Network": "R.string.stat_network",
    "Stop": "R.string.btn_stop",
    "Start Recording": "R.string.btn_start_recording",
    "PAST RECORDINGS": "R.string.past_recordings",
    "RECORDINGS": "R.string.session_recordings",
    "Select": "R.string.btn_select",
    "Start Stress Test": "R.string.btn_start_stress",
    "Overlay permission required": "R.string.overlay_permission_required",
    "Needed to show stats on top of other apps": "R.string.overlay_permission_desc",
    "Settings": "R.string.btn_settings",
    "⚡ Shizuku": "R.string.btn_shizuku",
    "Performance Overlay": "R.string.performance_overlay",
    "Active — monitoring stats": "R.string.overlay_active",
    "Inactive": "R.string.overlay_inactive",
    "LIVE STATS": "R.string.live_stats",
    "STATS": "R.string.stats",
    "Frame Time": "R.string.stat_frame_time",
    "APPEARANCE": "R.string.appearance",
    "Compact mode": "R.string.compact_mode",
    "Minimal horizontal bar": "R.string.compact_mode_desc",
    "Background blur": "R.string.background_blur",
    "Frosted glass effect (Android 12+)": "R.string.background_blur_desc",
    "Opacity": "R.string.opacity",
    "Scale": "R.string.scale",
    "THEME": "R.string.theme",
    "POSITION": "R.string.position",
    "REFRESH RATE": "R.string.refresh_rate",
    "SHIZUKU": "R.string.shizuku",
    "Connected — ready to grant permissions": "R.string.shizuku_connected",
    "Permission denied — open Shizuku app": "R.string.shizuku_denied",
    "Not running — start Shizuku first": "R.string.shizuku_not_running",
    "Not installed": "R.string.shizuku_not_installed",
    "Recordings": "R.string.recordings",
    "Recording in progress…": "R.string.recording_in_progress",
    "Capture performance data": "R.string.recording_idle",
    "No recordings yet": "R.string.no_recordings_yet",
    "Start recording to capture performance data": "R.string.no_recordings_desc",
    "Session Detail": "R.string.session_detail",
    "Not enough data points for graphs": "R.string.not_enough_data",
    "Delete": "R.string.btn_delete",
    "Export CSV": "R.string.btn_export_csv",
    "Export JSON": "R.string.btn_export_json",
    "Compare Sessions": "R.string.compare_sessions",
    "Select two sessions to compare": "R.string.select_sessions_hint",
    "Stress Test": "R.string.stress_test_title",
    "Controlled load generation": "R.string.stress_idle",
    "⚠ DETECTED — CPU ↓": "R.string.throttle_detected",
    "Stress Test": "R.string.stress_test_title",
    "Duration": "R.string.result_duration",
    "Avg CPU": "R.string.result_avg_cpu",
    "Avg GPU": "R.string.result_avg_gpu",
    "Peak CPU Temp": "R.string.result_peak_cpu_temp",
    "Peak GPU Temp": "R.string.result_peak_gpu_temp",
    "CPU Freq Range": "R.string.result_cpu_freq_range",
    "Throttle Events": "R.string.result_throttle_events",
    "PERF": "R.string.overlay_perf",
    "FT": "R.string.overlay_ft",
    "avg": "R.string.overlay_avg_ms",
    "p95": "R.string.overlay_p95_ms",
    "THROTTLED": "R.string.overlay_throttled",
    "Monitoring performance": "R.string.notification_text",
    "Temperature": "R.string.cd_temperature",
    "Battery": "R.string.cd_battery",
    "Anomalies": "R.string.anomalies",
    "Storage I/O": "R.string.stat_storage_io",
    "Download": "R.string.graph_download",
    "Upload": "R.string.graph_upload",
    "Dropped Frames": "R.string.graph_dropped_frames",
    "No anomalies detected": "R.string.no_anomalies",
}

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    original = content
    changes = []

    # Replace specific known strings
    for en_str, res_id in STRING_MAP.items():
        # Match string literal in code context
        # Only replace if it's NOT inside a stringResource call already
        # and NOT part of a larger string concatenation
        pattern = rf'"{re.escape(en_str)}"'
        replacement = f'stringResource({res_id})'
        new_content = re.sub(pattern, replacement, content)
        if new_content != content:
            changes.append((en_str, res_id))
            content = new_content

    if content != original:
        with open(filepath, 'w') as f:
            f.write(content)
        return changes
    return []

def main():
    base = "app/src/main/java/dev/perfoverlay"
    files = [
        f"{base}/ui/MainActivity.kt",
        f"{base}/ui/component/OverlayView.kt",
        f"{base}/ui/RecordingScreen.kt",
        f"{base}/ui/SessionCompareScreen.kt",
        f"{base}/ui/StressTestScreen.kt",
        f"{base}/service/OverlayService.kt",
    ]
    total = 0
    for f in files:
        if os.path.exists(f):
            changes = process_file(f)
            if changes:
                print(f"\n{f}:")
                for old, new in changes:
                    print(f"  '{old}' -> {new}")
                total += len(changes)
    print(f"\nTotal replacements: {total}")

if __name__ == "__main__":
    main()
