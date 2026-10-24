# 🏦 Agentic Bank Mobile Application

A premium, full-featured mobile banking application built with Flutter. It combines **traditional banking operations** (account balances, fund transfers, card controls, utility bill payments, loan tracking, payee management) with a **conversational AI banking assistant** that understands and responds in English, Sinhala (සිංහල), and Tamil (தமிழ்).

---

## 📱 Dual-Mode Experience

1. **Traditional Banking (Self-Service)**:
   - **Dashboard**: Aggregated balance, quick action grid, recent accounts overview.
   - **Accounts**: Account type badges, masked numbers, currency balances, transactions preview.
   - **Fund Transfers**: Two-step transfers with OTP verification and idempotency keys.
   - **Cards Management**: Virtual card visualizer, freeze/block cards, view CVV, credit limits and spending bars.
   - **Bill Payments**: Pay registered utility bills (CEB, Water Board, Dialog, Mobitel, SLT) with one-tap confirmation.
   - **Loans & Financing**: Active loan portfolio, repayment progress bars, and an interactive real-time EMI loan calculator.
   - **Payees / Beneficiaries**: Instant search, add new payees, direct transfer shortcut.
   - **Profile & Settings**: Customer details, security controls, biometric login, and language selection.

2. **AI Banking Assistant**:
   - Natural language conversations with the AI banking agent gateway.
   - Multilingual support for English, Sinhala, and Tamil.
   - One-tap quick prompts ("Check balance", "Pay electricity bill", "Transfer LKR 5000", "Show loans").
   - Real-time online agent health status and conversation history clearing.

---

## 🏗️ Clean Modular Architecture

Every component is separated into focused, single-responsibility files for maintainability:

```
agentic-ai-banking-assistant-mobile-fb-Agent_v2/
├── android/
│   └── app/src/main/
│       └── AndroidManifest.xml       # Internet & cleartext HTTP permissions
├── lib/
│   ├── main.dart                     # App entry point, MultiProvider & route table
│   ├── config/
│   │   └── api_config.dart           # Central API & Agent Gateway endpoints
│   ├── theme/
│   │   └── app_theme.dart            # Dark banking theme, typography & gradients
│   ├── models/
│   │   ├── account_model.dart        # Bank account schema & JSON parsing
│   │   ├── card_model.dart           # Debit/Credit card schema & blocked status
│   │   ├── loan_model.dart           # Loan facility schema & paid % calculation
│   │   ├── payee_model.dart          # Beneficiary / Payee schema
│   │   ├── biller_model.dart         # Utility biller schema
│   │   ├── user_model.dart           # Customer profile schema
│   │   └── chat_message_model.dart   # Chat message schema (User vs Bot)
│   ├── services/
│   │   ├── api_client.dart           # HTTP client with timeouts & auth headers
│   │   ├── auth_service.dart         # Login & Sign up API calls
│   │   ├── bank_service.dart         # Accounts, cards, loans, bills, transfer APIs
│   │   ├── chat_service.dart         # AI Agent gateway (/chat, /health, /session)
│   │   └── storage_service.dart      # Secure storage for JWT & session IDs
│   ├── providers/
│   │   ├── auth_provider.dart        # Authentication state management
│   │   ├── accounts_provider.dart    # Banking data provider (accounts, cards, loans)
│   │   └── chat_provider.dart        # AI chat messages & typing state provider
│   ├── widgets/
│   │   ├── account_summary_card.dart # Visual card for account details
│   │   ├── chat_bubble.dart          # Styled message bubbles with timestamp
│   │   ├── custom_text_field.dart    # Reusable styled text input
│   │   ├── empty_state.dart          # Reusable placeholder with icon & message
│   │   ├── gradient_card.dart        # Premium container with gradient & shadows
│   │   ├── loading_overlay.dart      # Full-screen spinner for async operations
│   │   ├── quick_action_grid.dart    # Grid of quick banking actions
│   │   └── typing_indicator.dart     # Animated 3-dot typing indicator
│   └── screens/
│       ├── main_shell.dart           # Bottom navigation shell with tab state
│       ├── splash/
│       │   └── splash_screen.dart    # Animated branding & auto session check
│       ├── auth/
│       │   ├── login_screen.dart     # Sign in with username/password & demo accounts
│       │   └── signup_screen.dart    # New user onboarding with language selection
│       ├── home/
│       │   └── home_screen.dart      # Dashboard with balance, actions & accounts
│       ├── accounts/
│       │   └── accounts_screen.dart  # Detailed accounts list & transactions
│       ├── transfer/
│       │   └── transfer_screen.dart  # Fund transfer flow with OTP verification
│       ├── cards/
│       │   └── cards_screen.dart     # Virtual cards carousel, controls & freeze
│       ├── bills/
│       │   └── bills_screen.dart     # Registered billers payment flow
│       ├── loans/
│       │   └── loans_screen.dart     # Active loans list & interactive EMI calculator
│       ├── payees/
│       │   └── payees_screen.dart    # Beneficiaries list, search & add payee
│       ├── profile/
│       │   └── profile_screen.dart   # KYC status, security settings, server config
│       └── chat/
│           └── chat_screen.dart      # Conversational AI banking assistant
├── analysis_options.yaml             # Flutter linter configuration
├── pubspec.yaml                      # Dependencies & asset declarations
└── README.md                         # Project documentation
```

---

## ⚙️ Configuration & Backend Setup

### API Endpoints (`lib/config/api_config.dart`)

```dart
class ApiConfig {
  // Use 10.0.2.2 for Android Emulator
  // Use your computer's local IP (e.g. 192.168.1.100) for physical devices
  static const String _host = '10.0.2.2';

  /// Core Banking API (Spring Boot on port 8081)
  static const String bankApiBase = 'http://$_host:8081/api/v1';

  /// Python AI Agent Gateway (FastAPI/Uvicorn on port 8290)
  static const String agentGatewayBase = 'http://$_host:8290';
}
```

> **Note for Physical Devices**: If running the app on a physical Android or iOS device connected over Wi-Fi, change `_host` from `10.0.2.2` to your computer's local Wi-Fi IP (e.g. `192.168.8.102`), and ensure your firewall allows inbound connections on ports `8081` and `8290`.

---

## 🚀 Getting Started

### 1. Prerequisites
- **Flutter SDK**: `>=3.2.0` installed and in your system `PATH`.
- **Android Studio / VS Code** with Flutter and Dart extensions.
- Running Core Banking Backend (Port `8081`) and Python Agent Gateway (Port `8290`).

### 2. Install Dependencies
Open a terminal in this directory:
```bash
flutter pub get
```

### 3. Run the App
- On Android Emulator:
  ```bash
  flutter run
  ```
- On Chrome / Web (for quick UI testing):
  ```bash
  flutter run -d chrome
  ```
- On a connected physical Android device:
  ```bash
  flutter run -d <device-id>
  ```

---

## 🧪 Default Test Credentials

When testing against the backend demo database:
- **Username / Customer ID**: `CUST001` (or your registered user)
- **Password**: `password123`
- Quick demo shortcuts are also available on the Login screen.