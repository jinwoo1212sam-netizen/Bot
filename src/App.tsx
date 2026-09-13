import React, { useState, useEffect, useRef } from "react";
import {
  Terminal,
  Settings,
  MessageSquare,
  Mic,
  MicOff,
  Send,
  Play,
  Volume2,
  Phone,
  Shield,
  Cpu,
  Folder,
  FileText,
  CheckCircle2,
  XCircle,
  Smartphone,
  Sparkles,
  Clock,
  Music,
  Code2,
  Layers,
  Info
} from "lucide-react";

interface Message {
  id: string;
  sender: "USER" | "ULTRON";
  text: string;
  actionStatus?: {
    type: string;
    success: boolean;
  };
  timestamp: string;
}

interface Contact {
  id: string;
  name: string;
  nickname: string;
  phone: string;
}

interface CustomRoutine {
  id: string;
  trigger: string;
  desc: string;
  actions: string[];
}

export default function App() {
  const [activeTab, setActiveTab] = useState<"phone" | "project" | "guide">("phone");
  const [navSection, setNavSection] = useState<"chat" | "terminal" | "settings">("chat");
  const [terminalSubTab, setTerminalSubTab] = useState<"contacts" | "social" | "routines" | "aliases" | "logs">("contacts");

  // State emulation
  const [isAiEnabled, setIsAiEnabled] = useState(false);
  const [aiModel, setAiModel] = useState("ling-3.0-flash-vl:free");
  const [userTitle, setUserTitle] = useState("Boss");
  const [assistantName, setAssistantName] = useState("ULTRON");
  const [isListening, setIsListening] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [inputText, setInputText] = useState("");
  const [volumeLevel, setVolumeLevel] = useState(50);
  const [audioRms, setAudioRms] = useState(0.2);

  const [messages, setMessages] = useState<Message[]>([
    {
      id: "1",
      sender: "ULTRON",
      text: "⚡ ULTRON Core Online. Local Command Parser active. Ready for your command, Boss.",
      timestamp: "10:00 AM"
    }
  ]);

  const [logs, setLogs] = useState<string[]>([
    "✓ System initialized in Boss Mode",
    "✓ LocalCommandParser v1.0 compiled",
    "✓ Android SpeechRecognizer ready",
    "✓ Room Database instance mounted",
    "✓ Action Allowlist enforced (14 tools)"
  ]);

  const [contacts, setContacts] = useState<Contact[]>([
    { id: "1", name: "Mummy", nickname: "Mummy", phone: "+91 98765 43210" },
    { id: "2", name: "Rahul", nickname: "Rahul", phone: "+91 98111 22233" }
  ]);

  const [routines, setRoutines] = useState<CustomRoutine[]>([
    { id: "1", trigger: "office mode", desc: "Work setup routine", actions: ["OPEN_APP:chrome", "SET_VOLUME:30"] },
    { id: "2", trigger: "gaming mode", desc: "Performance & media mode", actions: ["OPEN_APP:youtube", "SET_VOLUME:80"] }
  ]);

  const [selectedFile, setSelectedFile] = useState<string>("LocalCommandParser.kt");
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Audio RMS simulation when listening or speaking
  useEffect(() => {
    let interval: any;
    if (isListening || isSpeaking) {
      interval = setInterval(() => {
        setAudioRms(0.2 + Math.random() * 0.7);
      }, 100);
    } else {
      setAudioRms(0.1);
    }
    return () => clearInterval(interval);
  }, [isListening, isSpeaking]);

  const handleCommandExecution = (text: string) => {
    const raw = text.trim();
    if (!raw) return;

    const userMsg: Message = {
      id: Date.now().toString(),
      sender: "USER",
      text: raw,
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
    };

    setMessages((prev) => [...prev, userMsg]);
    setLogs((prev) => [`User command: "${raw}"`, ...prev]);

    // Emulate command parsing logic
    setTimeout(() => {
      processCommand(raw);
    }, 450);
  };

  const processCommand = (cmd: string) => {
    const lower = cmd.toLowerCase();
    let responseText = `Yes ${userTitle}.`;
    let actionInfo: { type: string; success: boolean } | undefined;

    if (lower.includes("phone on kar") || lower.includes("power on")) {
      responseText = `${userTitle}, phone completely power off hone par normal Android app se power on nahi kiya ja sakta. Agar screen off hai toh main active hoon.`;
      actionInfo = { type: "Security Verification", success: true };
    } else if (lower.includes("youtube kholo") || lower.includes("open youtube")) {
      responseText = `Yes ${userTitle}, YouTube open kar raha hoon.`;
      actionInfo = { type: "YouTube launched", success: true };
      setLogs((prev) => [`✓ OPEN_APP: com.google.android.youtube`, ...prev]);
    } else if (lower.includes("kesariya") || lower.includes("search karo") || lower.includes("chalao") || lower.includes("play")) {
      const match = lower.replace("youtube par", "").replace("search karo", "").replace("chalao", "").replace("play", "").trim();
      const query = match || "Kesariya";
      responseText = `${userTitle}, YouTube par "${query}" play kar raha hoon.`;
      actionInfo = { type: `YouTube query: "${query}"`, success: true };
      setLogs((prev) => [`✓ SEARCH_YOUTUBE: query='${query}'`, ...prev]);
    } else if (lower.includes("call") || lower.includes("phone lagao")) {
      const isMummy = lower.includes("mummy");
      const person = isMummy ? "Mummy" : "contact";
      responseText = `${userTitle}, ${person} ko call kar raha hoon.`;
      actionInfo = { type: `ACTION_CALL: ${person} (+91 98765 43210)`, success: true };
      setLogs((prev) => [`✓ CALL_ACTION: Resolved nickname '${person}' to +919876543210`, ...prev]);
    } else if (lower.includes("volume")) {
      if (lower.includes("full") || lower.includes("100")) {
        setVolumeLevel(100);
        responseText = `${userTitle}, media volume full kar diya.`;
        actionInfo = { type: "SET_VOLUME: 100%", success: true };
      } else if (lower.includes("50")) {
        setVolumeLevel(50);
        responseText = `${userTitle}, media volume 50% set kar diya.`;
        actionInfo = { type: "SET_VOLUME: 50%", success: true };
      } else if (lower.includes("badhao") || lower.includes("up")) {
        setVolumeLevel((prev) => Math.min(100, prev + 15));
        responseText = `${userTitle}, volume badha diya.`;
        actionInfo = { type: "SET_VOLUME: +15%", success: true };
      } else {
        setVolumeLevel((prev) => Math.max(0, prev - 15));
        responseText = `${userTitle}, volume kam kar diya.`;
        actionInfo = { type: "SET_VOLUME: -15%", success: true };
      }
      setLogs((prev) => [`✓ VOLUME_ACTION: AudioManager.STREAM_MUSIC adjusted`, ...prev]);
    } else if (lower.includes("timer")) {
      responseText = `${userTitle}, 10 minute ka timer set kar diya.`;
      actionInfo = { type: "ACTION_SET_TIMER: 600s", success: true };
      setLogs((prev) => [`✓ TIMER_ACTION: AlarmClock.ACTION_SET_TIMER dispatched`, ...prev]);
    } else if (lower.includes("office mode") || lower.includes("gaming mode")) {
      responseText = `Done, ${userTitle}. Routine poori ho gayi.`;
      actionInfo = { type: "MULTI-ACTION ROUTINE: Finished", success: true };
      setLogs((prev) => [
        `✓ ROUTINE_COMPLETE: Trigger '${lower}' dispatched 2 actions`,
        `✓ Step 2: SET_VOLUME executed`,
        `✓ Step 1: OPEN_APP executed`,
        ...prev
      ]);
    } else if (isAiEnabled) {
      responseText = `Yes ${userTitle}, OpenRouter LLM (${aiModel}) processed your request.`;
      actionInfo = { type: "OPENROUTER: Structured Tool Result", success: true };
      setLogs((prev) => [`✓ AI_RESPONSE: OpenRouter response received with valid tool call`, ...prev]);
    } else {
      responseText = `${userTitle}, main action execute kar raha hoon.`;
      actionInfo = { type: "Local Command Engine", success: true };
    }

    const ultronMsg: Message = {
      id: (Date.now() + 1).toString(),
      sender: "ULTRON",
      text: responseText,
      actionStatus: actionInfo,
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
    };

    setMessages((prev) => [...prev, ultronMsg]);
    setIsSpeaking(true);
    setTimeout(() => setIsSpeaking(false), 2200);
  };

  const handleMicToggle = () => {
    if (!isListening) {
      setIsListening(true);
      setTimeout(() => {
        setIsListening(false);
        handleCommandExecution("YouTube par Kesariya search karo");
      }, 2500);
    } else {
      setIsListening(false);
    }
  };

  // Sample file previews
  const androidFiles: Record<string, string> = {
    "LocalCommandParser.kt": `package com.ultron.assistant.command

import com.ultron.assistant.tools.ToolCall
import java.util.regex.Pattern

class LocalCommandParser {
    fun parse(rawInput: String): ParsedCommand {
        val cleanInput = normalizeInput(rawInput)

        // 1. Phone ON safe explanation
        if (isPhoneOnCommand(cleanInput)) {
            return ParsedCommand.SpecialResponse(
                "Boss, phone completely power off hone par normal Android app se power on nahi kiya ja sakta."
            )
        }

        // 2. Multi-command split ("aur", "then", ",")
        val subPhrases = splitMultiCommands(cleanInput)
        if (subPhrases.size > 1) {
            val parsedList = subPhrases.mapNotNull { parseSingle(it) as? ParsedCommand.Single }
            return ParsedCommand.Multi(parsedList, rawInput)
        }

        return parseSingle(cleanInput)
    }

    private fun parseSingle(text: String): ParsedCommand {
        // Dynamic YouTube variable query extraction (Kesariya, Believer, etc.)
        val ytPattern = Pattern.compile("(?:youtube\\\\s+(?:par|pe|me)\\\\s+)?(.+?)\\\\s+(?:search\\\\s+karo|chalao|play)", Pattern.CASE_INSENSITIVE)
        val m = ytPattern.matcher(text)
        if (m.find()) {
            val query = m.group(1)?.trim() ?: ""
            return ParsedCommand.Single(ToolCall("search_youtube", mapOf("query" to query)), text)
        }

        // App Launching, Calling, Volume, Alarms...
        return ParsedCommand.Unknown(text)
    }
}`,
    "AndroidManifest.xml": `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Official Runtime Permissions -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />

    <!-- Package Visibility Declarations for Android 11+ App Launches -->
    <queries>
        <package android:name="com.google.android.youtube" />
        <package android:name="com.instagram.android" />
        <package android:name="com.whatsapp" />
        <package android:name="org.telegram.messenger" />
        <package android:name="com.android.chrome" />
    </queries>

    <application
        android:name=".UltronApplication"
        android:label="@string/app_name"
        android:theme="@style/Theme.Ultron">

        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>`,
    "ToolRegistry.kt": `package com.ultron.assistant.tools

class ToolRegistry(
    private val context: Context,
    private val appLauncher: AppLauncher,
    private val youTubeAction: YouTubeAction,
    private val callAction: CallAction,
    private val messagingAction: MessagingAction,
    private val volumeAction: VolumeAction,
    private val settingsAction: SettingsAction,
    private val timerAction: TimerAction,
    private val alarmAction: AlarmAction
) {
    // Strictly allowed actions. Unknown calls are rejected.
    val allowedTools = setOf(
        "open_app", "search_youtube", "call_contact", "prepare_message",
        "open_settings", "set_volume", "set_timer", "set_alarm",
        "open_camera", "open_browser", "web_search", "get_time", "get_date"
    )

    suspend fun executeTool(toolCall: ToolCall): ActionResult {
        if (!isToolAllowed(toolCall.type)) {
            return ActionResult.Failure("Security rejection: Tool '\${toolCall.type}' not in allowlist.")
        }
        return when (toolCall.type) {
            "open_app" -> appLauncher.launchApp(toolCall.parameters["app"] ?: "")
            "search_youtube" -> youTubeAction.searchOrPlay(toolCall.parameters["query"] ?: "")
            "call_contact" -> callAction.executeCall(toolCall.parameters["contact"] ?: "")
            // ...
        }
    }
}`,
    "OpenRouterProvider.kt": `package com.ultron.assistant.ai

class OpenRouterProvider : AiProvider {
    override val providerName = "OpenRouter"

    override suspend fun generateResponse(
        endpointUrl: String,
        apiKey: String,
        request: AiRequest
    ): AiResponse {
        // Enforces JSON mode: { "actions": [...], "response": "Opening YouTube, Boss." }
        // Validates tool calls against ToolRegistry allowlist before any Android invocation.
    }
}`
  };

  return (
    <div className="min-h-screen bg-[#050811] text-slate-200 flex flex-col font-sans">
      {/* TOP HEADER */}
      <header className="border-b border-cyan-950/60 bg-[#080d1a]/80 backdrop-blur px-6 py-3 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-cyan-500/20 border border-cyan-400/50 flex items-center justify-center text-cyan-400 font-mono font-bold text-sm shadow-[0_0_12px_rgba(6,182,212,0.3)]">
            ⚡
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-mono font-bold tracking-wider text-cyan-400 text-base">ULTRON</span>
              <span className="text-[11px] px-2 py-0.5 rounded bg-cyan-950/80 border border-cyan-800/60 text-cyan-300 font-mono">
                Native Android
              </span>
            </div>
            <p className="text-xs text-slate-400">Futuristic Voice & Text Assistant • Kotlin & Jetpack Compose</p>
          </div>
        </div>

        {/* View Switcher */}
        <div className="flex bg-[#0d1526] p-1 rounded-lg border border-slate-800 text-xs font-mono">
          <button
            onClick={() => setActiveTab("phone")}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded transition ${
              activeTab === "phone" ? "bg-cyan-500 text-slate-950 font-bold shadow" : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>Live Assistant</span>
          </button>
          <button
            onClick={() => setActiveTab("project")}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded transition ${
              activeTab === "project" ? "bg-cyan-500 text-slate-950 font-bold shadow" : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Code2 className="w-3.5 h-3.5" />
            <span>Android Codebase</span>
          </button>
          <button
            onClick={() => setActiveTab("guide")}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded transition ${
              activeTab === "guide" ? "bg-cyan-500 text-slate-950 font-bold shadow" : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            <span>Android Studio Guide</span>
          </button>
        </div>
      </header>

      {/* MAIN CONTENT AREA */}
      <main className="flex-1 flex overflow-hidden">
        {activeTab === "phone" && (
          <div className="flex-1 flex justify-center items-center p-4 lg:p-6 bg-[#03060c]">
            {/* ANDROID DEVICE FRAME */}
            <div className="w-full max-w-[420px] h-[820px] bg-[#070c17] rounded-[38px] border-4 border-slate-800 shadow-[0_0_50px_rgba(0,0,0,0.8)] flex flex-col overflow-hidden relative">
              {/* Notch */}
              <div className="absolute top-2 left-1/2 -translate-x-1/2 w-28 h-4 bg-slate-900 rounded-full z-30 flex items-center justify-center">
                <div className="w-2.5 h-2.5 rounded-full bg-slate-950 border border-slate-800 mr-2" />
                <div className="w-1.5 h-1.5 rounded-full bg-cyan-900" />
              </div>

              {/* HUD / TOP STATUS BAR */}
              <div className="pt-7 px-4 pb-3 bg-[#0a101f] border-b border-cyan-950/70">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 animate-pulse" />
                    <span className="font-mono font-bold text-sm text-cyan-400 tracking-wider">{assistantName}</span>
                    <span className="text-[10px] font-mono text-slate-400">• Boss Mode</span>
                  </div>
                  <div className="flex items-center space-x-1.5">
                    <span
                      className={`text-[9px] font-mono font-bold px-1.5 py-0.5 rounded border ${
                        isAiEnabled
                          ? "bg-emerald-950/80 border-emerald-500/60 text-emerald-400"
                          : "bg-cyan-950/80 border-cyan-500/60 text-cyan-400"
                      }`}
                    >
                      {isAiEnabled ? "AI ONLINE" : "LOCAL ONLY"}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between mt-1 text-[10px] font-mono text-slate-500">
                  <span>{isAiEnabled ? `[OpenRouter: ${aiModel}]` : "[LocalCommandParser v1.0]"}</span>
                  <span className={isListening ? "text-cyan-400 font-bold" : isSpeaking ? "text-emerald-400 font-bold" : ""}>
                    {isListening ? "🎙 LISTENING..." : isSpeaking ? "🔊 SPEAKING..." : "READY"}
                  </span>
                </div>
              </div>

              {/* ACTIVE SCREEN CONTENT */}
              <div className="flex-1 flex flex-col overflow-hidden">
                {navSection === "chat" && (
                  <div className="flex-1 flex flex-col bg-[#050811]">
                    {/* Chat Stream */}
                    <div className="flex-1 overflow-y-auto p-3 space-y-3">
                      {messages.map((m) => (
                        <div
                          key={m.id}
                          className={`flex flex-col ${m.sender === "USER" ? "items-end" : "items-start"}`}
                        >
                          <span className="text-[10px] font-mono font-bold text-slate-400 px-1 mb-0.5">
                            {m.sender === "USER" ? userTitle.toUpperCase() : assistantName}
                          </span>
                          <div
                            className={`max-w-[85%] rounded-xl px-3.5 py-2.5 text-xs leading-relaxed ${
                              m.sender === "USER"
                                ? "bg-cyan-950/60 border border-cyan-500/40 text-cyan-100"
                                : "bg-[#0d1629] border border-slate-800 text-slate-200"
                            }`}
                          >
                            <p>{m.text}</p>
                            {m.actionStatus && (
                              <div className="mt-1.5 pt-1 border-t border-slate-800 flex items-center space-x-1 text-[10px] font-mono">
                                {m.actionStatus.success ? (
                                  <CheckCircle2 className="w-3 h-3 text-emerald-400 shrink-0" />
                                ) : (
                                  <XCircle className="w-3 h-3 text-red-400 shrink-0" />
                                )}
                                <span className={m.actionStatus.success ? "text-emerald-400" : "text-red-400"}>
                                  {m.actionStatus.type}
                                </span>
                              </div>
                            )}
                          </div>
                        </div>
                      ))}
                      <div ref={messagesEndRef} />
                    </div>

                    {/* Audio Waveform Canvas */}
                    <div className="h-10 bg-[#090f1d] border-t border-slate-800 flex items-center justify-center px-4">
                      {isListening || isSpeaking ? (
                        <div className="flex items-center space-x-1.5 h-6">
                          {[...Array(16)].map((_, i) => (
                            <div
                              key={i}
                              className="w-1 rounded-full bg-cyan-400 transition-all duration-100"
                              style={{
                                height: `${Math.max(4, Math.sin(i * 0.4 + audioRms * 10) * 18 + 8)}px`,
                                opacity: 0.8
                              }}
                            />
                          ))}
                        </div>
                      ) : (
                        <div className="flex items-center space-x-2 text-[10px] font-mono text-slate-500">
                          <span className="w-1.5 h-1.5 rounded-full bg-cyan-500/50" />
                          <span>ULTRON CORE STANDBY</span>
                        </div>
                      )}
                    </div>

                    {/* Input Area */}
                    <div className="p-3 bg-[#0a101f] border-t border-slate-800 flex items-center space-x-2">
                      <button
                        onClick={handleMicToggle}
                        className={`w-10 h-10 rounded-full flex items-center justify-center transition shadow ${
                          isListening
                            ? "bg-red-500 text-white animate-pulse shadow-red-500/50"
                            : "bg-cyan-500/20 border border-cyan-400/60 text-cyan-400 hover:bg-cyan-500/30"
                        }`}
                      >
                        {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
                      </button>

                      <input
                        type="text"
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" && inputText.trim()) {
                            handleCommandExecution(inputText);
                            setInputText("");
                          }
                        }}
                        placeholder={isListening ? "Listening for Boss..." : "Command or message..."}
                        className="flex-1 bg-[#10192e] border border-slate-700/80 rounded-full px-3.5 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500"
                      />

                      <button
                        onClick={() => {
                          if (inputText.trim()) {
                            handleCommandExecution(inputText);
                            setInputText("");
                          }
                        }}
                        disabled={!inputText.trim()}
                        className={`w-10 h-10 rounded-full flex items-center justify-center transition ${
                          inputText.trim()
                            ? "bg-cyan-500 text-slate-950 font-bold"
                            : "bg-slate-800 text-slate-600 cursor-not-allowed"
                        }`}
                      >
                        <Send className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                )}

                {navSection === "terminal" && (
                  <div className="flex-1 flex flex-col bg-[#050811]">
                    {/* Sub-tabs */}
                    <div className="flex bg-[#090f1e] border-b border-slate-800 p-1 text-[10px] font-mono justify-between overflow-x-auto">
                      {(["contacts", "social", "routines", "aliases", "logs"] as const).map((tab) => (
                        <button
                          key={tab}
                          onClick={() => setTerminalSubTab(tab)}
                          className={`px-2 py-1 rounded uppercase tracking-wider ${
                            terminalSubTab === tab ? "bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/40" : "text-slate-400"
                          }`}
                        >
                          {tab}
                        </button>
                      ))}
                    </div>

                    <div className="flex-1 p-3 overflow-y-auto space-y-3">
                      {terminalSubTab === "contacts" && (
                        <div className="space-y-2">
                          <div className="flex justify-between items-center text-xs font-mono text-cyan-400 font-bold">
                            <span>SAVED CONTACTS ({contacts.length})</span>
                          </div>
                          {contacts.map((c) => (
                            <div key={c.id} className="bg-[#0e1628] border border-slate-800 rounded-lg p-2.5 text-xs">
                              <div className="flex justify-between items-center">
                                <span className="font-bold text-slate-200">{c.name}</span>
                                <span className="text-[10px] text-cyan-400 font-mono">Nickname: "{c.nickname}"</span>
                              </div>
                              <p className="text-[11px] text-slate-400 font-mono mt-0.5">{c.phone}</p>
                            </div>
                          ))}
                          <p className="text-[11px] text-slate-500 italic mt-2">
                            Try saying: "Ultron meri mummy ko call kar"
                          </p>
                        </div>
                      )}

                      {terminalSubTab === "social" && (
                        <div className="space-y-2">
                          <div className="text-xs font-mono text-cyan-400 font-bold">MESSAGING ACCOUNTS</div>
                          <div className="bg-[#0e1628] border border-slate-800 rounded-lg p-2.5 text-xs">
                            <div className="flex justify-between">
                              <span className="font-bold text-slate-200">Tital</span>
                              <span className="text-[10px] text-emerald-400 font-mono">WhatsApp</span>
                            </div>
                            <p className="text-[11px] text-slate-400 font-mono mt-0.5">+91 99887 76655</p>
                          </div>
                          <p className="text-[11px] text-slate-500 italic mt-2">
                            Try saying: "Tital ko WhatsApp par message karo: VC aa"
                          </p>
                        </div>
                      )}

                      {terminalSubTab === "routines" && (
                        <div className="space-y-2">
                          <div className="text-xs font-mono text-cyan-400 font-bold">CUSTOM ROUTINES</div>
                          {routines.map((r) => (
                            <div key={r.id} className="bg-[#0e1628] border border-slate-800 rounded-lg p-2.5 text-xs">
                              <span className="font-mono font-bold text-cyan-300">"{r.trigger}"</span>
                              <p className="text-[11px] text-slate-400">{r.desc}</p>
                              <div className="mt-1 flex flex-wrap gap-1">
                                {r.actions.map((a, i) => (
                                  <span key={i} className="text-[9px] font-mono px-1.5 py-0.5 rounded bg-slate-900 border border-slate-700 text-slate-300">
                                    {a}
                                  </span>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}

                      {terminalSubTab === "aliases" && (
                        <div className="space-y-2">
                          <div className="text-xs font-mono text-cyan-400 font-bold">APP ALIASES</div>
                          {[
                            { k: "YT", v: "YouTube" },
                            { k: "IG", v: "Instagram" },
                            { k: "Snap", v: "Snapchat" },
                            { k: "FB", v: "Facebook" }
                          ].map((item, idx) => (
                            <div key={idx} className="bg-[#0e1628] border border-slate-800 rounded-lg p-2 flex justify-between text-xs font-mono">
                              <span className="text-cyan-400 font-bold">"{item.k}"</span>
                              <span className="text-slate-400">➔ {item.v}</span>
                            </div>
                          ))}
                        </div>
                      )}

                      {terminalSubTab === "logs" && (
                        <div className="space-y-1.5 font-mono text-[10px]">
                          <div className="text-cyan-400 font-bold text-xs mb-2">LIVE EXECUTION STREAM</div>
                          {logs.map((l, i) => (
                            <div
                              key={i}
                              className={`p-1.5 rounded ${
                                l.startsWith("✓") ? "text-emerald-400 bg-emerald-950/30" : "text-slate-300 bg-slate-900/60"
                              }`}
                            >
                              › {l}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {navSection === "settings" && (
                  <div className="flex-1 p-3.5 bg-[#050811] overflow-y-auto space-y-4 text-xs">
                    {/* General */}
                    <div className="space-y-2">
                      <div className="font-mono text-cyan-400 font-bold tracking-wider">GENERAL</div>
                      <div className="bg-[#0e1628] border border-slate-800 rounded-lg p-3 space-y-2">
                        <div>
                          <label className="text-[10px] text-slate-400 block mb-1">User Title</label>
                          <input
                            type="text"
                            value={userTitle}
                            onChange={(e) => setUserTitle(e.target.value)}
                            className="w-full bg-[#131f38] border border-slate-700 rounded px-2 py-1 text-slate-200"
                          />
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-400 block mb-1">Assistant Name</label>
                          <input
                            type="text"
                            value={assistantName}
                            onChange={(e) => setAssistantName(e.target.value)}
                            className="w-full bg-[#131f38] border border-slate-700 rounded px-2 py-1 text-slate-200"
                          />
                        </div>
                      </div>
                    </div>

                    {/* AI Provider (OpenRouter) */}
                    <div className="space-y-2">
                      <div className="font-mono text-cyan-400 font-bold tracking-wider">OPENROUTER AI CONFIG</div>
                      <div className="bg-[#0e1628] border border-slate-800 rounded-lg p-3 space-y-2.5">
                        <div className="flex items-center justify-between">
                          <span>Enable AI Integration</span>
                          <input
                            type="checkbox"
                            checked={isAiEnabled}
                            onChange={(e) => setIsAiEnabled(e.target.checked)}
                            className="w-4 h-4 accent-cyan-500"
                          />
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-400 block mb-1">Model Name</label>
                          <input
                            type="text"
                            value={aiModel}
                            onChange={(e) => setAiModel(e.target.value)}
                            className="w-full bg-[#131f38] border border-slate-700 rounded px-2 py-1 font-mono text-[11px]"
                          />
                        </div>
                        <p className="text-[10px] text-slate-500">
                          EncryptedSharedPreferences Android Keystore protects the OpenRouter API Key.
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* THREE-SIDEBAR NAVIGATION */}
              <div className="bg-[#0a101f] border-t border-slate-800 px-3 py-2 flex justify-around">
                <button
                  onClick={() => setNavSection("chat")}
                  className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-mono transition ${
                    navSection === "chat" ? "bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/40" : "text-slate-400"
                  }`}
                >
                  <MessageSquare className="w-3.5 h-3.5" />
                  <span>AI CHAT</span>
                </button>
                <button
                  onClick={() => setNavSection("terminal")}
                  className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-mono transition ${
                    navSection === "terminal" ? "bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/40" : "text-slate-400"
                  }`}
                >
                  <Terminal className="w-3.5 h-3.5" />
                  <span>TERMINAL</span>
                </button>
                <button
                  onClick={() => setNavSection("settings")}
                  className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-mono transition ${
                    navSection === "settings" ? "bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/40" : "text-slate-400"
                  }`}
                >
                  <Settings className="w-3.5 h-3.5" />
                  <span>SETTINGS</span>
                </button>
              </div>

              {/* Home bar indicator */}
              <div className="py-1 bg-[#0a101f] flex justify-center">
                <div className="w-24 h-1 bg-slate-700 rounded-full" />
              </div>
            </div>
          </div>
        )}

        {activeTab === "project" && (
          <div className="flex-1 flex overflow-hidden bg-[#070b14]">
            {/* File List */}
            <div className="w-64 border-r border-slate-800 bg-[#090e1c] p-3 space-y-2 overflow-y-auto">
              <div className="text-xs font-mono font-bold text-cyan-400 uppercase tracking-wider mb-2">
                Kotlin & Android Files
              </div>
              {Object.keys(androidFiles).map((file) => (
                <button
                  key={file}
                  onClick={() => setSelectedFile(file)}
                  className={`w-full text-left px-2.5 py-1.5 rounded text-xs font-mono flex items-center space-x-2 transition ${
                    selectedFile === file ? "bg-cyan-500/20 text-cyan-300 border border-cyan-500/40" : "text-slate-400 hover:text-slate-200"
                  }`}
                >
                  <FileText className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                  <span className="truncate">{file}</span>
                </button>
              ))}
            </div>

            {/* Code Viewer */}
            <div className="flex-1 flex flex-col overflow-hidden">
              <div className="bg-[#0b1122] border-b border-slate-800 px-4 py-2 flex items-center justify-between">
                <span className="text-xs font-mono text-cyan-300">{selectedFile}</span>
                <span className="text-[11px] text-slate-500 font-mono">Clean MVVM Architecture</span>
              </div>
              <div className="flex-1 p-4 overflow-auto bg-[#04070e]">
                <pre className="font-mono text-xs text-slate-300 leading-relaxed">
                  <code>{androidFiles[selectedFile]}</code>
                </pre>
              </div>
            </div>
          </div>
        )}

        {activeTab === "guide" && (
          <div className="flex-1 p-8 overflow-y-auto max-w-4xl mx-auto space-y-6">
            <div>
              <h2 className="text-xl font-mono font-bold text-cyan-400 tracking-wider">
                ANDROID STUDIO BUILD & EXPORT GUIDE
              </h2>
              <p className="text-xs text-slate-400 mt-1">
                How to open, compile, sync Gradle, and install the native ULTRON APK on a physical Android device.
              </p>
            </div>

            <div className="bg-[#0d1527] border border-slate-800 rounded-xl p-5 space-y-4">
              <h3 className="font-mono font-bold text-sm text-slate-200">1. Open Project in Android Studio</h3>
              <ol className="list-decimal list-inside text-xs text-slate-400 space-y-2 leading-relaxed">
                <li>Launch Android Studio (Hedgehog, Iguana, Jellyfish, or newer).</li>
                <li>Choose <strong>File &gt; Open...</strong> and select the project folder.</li>
                <li>Android Studio will automatically detect <code>settings.gradle.kts</code> and initialize the project sync.</li>
              </ol>
            </div>

            <div className="bg-[#0d1527] border border-slate-800 rounded-xl p-5 space-y-4">
              <h3 className="font-mono font-bold text-sm text-slate-200">2. Compile APK from Command Line or Terminal</h3>
              <div className="bg-[#060a14] border border-slate-800 rounded p-3 font-mono text-xs text-cyan-300">
                # Generate Debug APK<br />
                ./gradlew assembleDebug<br /><br />
                # Run Automated Test Suite<br />
                ./gradlew test
              </div>
              <p className="text-xs text-slate-400">
                The compiled APK file will be generated at:<br />
                <code className="text-cyan-400">app/build/outputs/apk/debug/app-debug.apk</code>
              </p>
            </div>

            <div className="bg-[#0d1527] border border-slate-800 rounded-xl p-5 space-y-4">
              <h3 className="font-mono font-bold text-sm text-slate-200">3. Install on Physical Android Device</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Connect your Android phone with USB Debugging enabled, then run:
              </p>
              <div className="bg-[#060a14] border border-slate-800 rounded p-3 font-mono text-xs text-emerald-400">
                adb install -r app/build/outputs/apk/debug/app-debug.apk
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
