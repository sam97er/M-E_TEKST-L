# M&E Tekstil AI Smart ERP - Architecture & System Constraints

## 1. Development Environment Analysis (Aşama 0)

- **Platform Core**: Cloud Android Build Environment with Gradle (Kotlin DSL), Android 36 SDK, Jetpack Compose, Material 3.
- **Python Runtime**: Python 3.11 with pip, FastAPI, SQLAlchemy, Pydantic, and Pytest installed.
- **Git Version Control**: Initialized at workspace root with structured branch strategy.
- **APK Target**: `com.aistudio.metekstil.erp`

---

## 2. System Architecture & Local Backend Strategy

### A. Dual Hybrid Architecture
1. **Local Python/FastAPI Backend (`/backend`)**:
   - **Framework**: FastAPI + Pydantic v2 + SQLAlchemy 2.0.
   - **Database**: SQLite with WAL (Write-Ahead Logging) mode for concurrent read/write transactions.
   - **Deployment Options**:
     - Android On-Device via Termux (`python main.py` or `uvicorn`).
     - Local server / LAN server / Cloud backup.
   - **Modularity**:
     - Separate domain services: `trendyol`, `ai`, `inventory`, `sales`, `profit`, `reports`, `telegram`.
     - 3-Provider AI abstraction adapter with manual confirmation gates.
     - Strict idempotent logging and audit trails.

2. **Native Android APK (`/app`)**:
   - **Framework**: Jetpack Compose + Kotlin Coroutines + Material 3.
   - **Communication**: Retrofit/OkHttp client connecting to local backend (`http://10.0.2.2:8000` for emulator, `http://127.0.0.1:8000` for Termux on device, or custom IP configured in Ayarlar).
   - **Resilience**: Integrated Room database cache so that inventory, questions, and drafts remain accessible even if the backend service is offline.
   - **UI/UX**: Turkish default UI with clean M3 components, bottom navigation, responsive cards, and full status indicators.

---

## 3. Operational Constraints & Android Background Execution

As required by general project rules, we document real platform limitations transparently:
1. **Android Doze Mode & Background Tasks**:
   - Android OS suspends background network activity and background processes when the screen is turned off or memory is constrained.
   - Termux/Python backend running on a phone requires battery optimization exclusion (`termux-wake-lock`).
   - Telegram polling and background Trendyol sync must be scheduled with reasonable intervals and cannot guarantee 100% real-time uptime when the app is deeply suspended by Android OS.
2. **Offline Safety**:
   - Every transaction in the ERP (stock movements, approved AI replies, settings changes) is written to the transactional SQLite database first with timestamp and status before any network transmission.
3. **No Unauthenticated AI Actions**:
   - AI outputs (replies to Trendyol customer questions, image modifications, pricing discounts) are strictly stored as `DRAFT` or `NEEDS_REVIEW`. No automated submission occurs without manual human approval in the UI.

---

## 4. Phased Roadmap (10 Phases)

- **Aşama 0**: Environment audit, build strategy, constraints documentation, Git setup. (TAMAMLANDI)
- **Aşama 1**: Project structure, Config, SQLite Database foundation, FastAPI Health endpoint, logging, unit tests. (TAMAMLANDI)
- **Aşama 2**: Ayarlar (Settings), secure secret management, 3 AI Provider Adapters & connection tests. (TAMAMLANDI)
- **Aşama 3**: Ürünler & Stok Yönetimi (Products, Variants, Barcodes, Multi-warehouse, Stock Movement Ledger). (TAMAMLANDI)
- **Aşama 4**: Trendyol API Integration Service (Auth, Rate Limiting, Sync Engine, Error Handling). (TAMAMLANDI)
- **Aşama 5**: Müşteri Soruları & AI Yanıt Sistemi (Manual Approval Workflow: New -> Draft -> Approved -> Sent). (TAMAMLANDI)
- **Aşama 6**: Siparişler, Satış & Kâr/Zarar Hesaplama (Cost breakdown, Commission, Net Profit analysis). (TAMAMLANDI)
- **Aşama 7**: Telegram Bot Bildirim Sistemi (Event-driven alerts, Retry queue, Audit logs). (TAMAMLANDI)
- **Aşama 8**: AI Akıllı Raporlar & Ürün/Görsel Analizi (Stagnant inventory, Quality audit, Pricing advice). (TAMAMLANDI)
- **Aşama 9**: Dashboard Konsolidasyonu, UI/UX Cila, Güvenlik İncelemesi & APK Doğrulama. (TAMAMLANDI)
- **Aşama 10**: Uçtan Uca (E2E) Test Doğrulaması, Sistem Teşhisi, Otomatik Yedekleme & CSV/JSON Dışa Aktarma, Termux & Dağıtım Betikleri ve Nihai Teslimat. (TAMAMLANDI)
