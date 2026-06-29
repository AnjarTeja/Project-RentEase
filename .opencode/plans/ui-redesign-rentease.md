# UI Redesign Plan - RentEase Modern Blue

## Overview
Transform from dark galaxy theme to white + modern blue (#1565C0) theme.
User dashboard gets a Shopee-like layout. Petugas/Admin get theme-only changes.

## Implementation Order (30+ files)

### PHASE 1: Theme Foundation (4 files)

#### 1. Color.kt
Replace all colors with:
- PrimaryBlue = #1565C0
- BlueDark = #0D47A1
- BlueLight = #E3F2FD
- BlueAccent = #42A5F5
- BlueSoftBg = #F5F8FF
- White = #FFFFFF
- SurfaceGray = #F5F5F5
- TextPrimary = #212121
- TextSecondary = #757575
- TextHint = #BDBDBD
- SuccessGreen = #2E7D32
- ErrorRed = #C62828
- WarningOrange = #EF6C00

#### 2. Theme.kt
Switch from `darkColorScheme` to `lightColorScheme` using new colors.

#### 3. Type.kt
Update text colors for light theme (TextPrimary/TextSecondary instead of TextDark/TextLight).

#### 4. Shape.kt
Keep as-is (optional fine-tune).

### PHASE 2: Core Components (5 files)

#### 5. GalaxyBackground.kt → ModernBackground
Replace with white/light-blue gradient background, remove StarFieldOverlay reference.

#### 6. NebulaHeader.kt → BlueHeader
Replace dark nebula gradient with solid blue gradient (#1565C0 → #0D47A1).

#### 7. StarFieldOverlay.kt
Can be deleted or kept as no-op (no longer referenced).

#### 8. GlassCard.kt
Change background from TechCardBg to White, add subtle shadow.

#### 9. GlowCard.kt
Change background from TechCardBg to White, border uses PrimaryBlue.

### PHASE 3: Shared Components (1 file)

#### 10. AppComponents.kt
- StatCard: Light blue bg + blue accent
- MenuGridItem: White card with shadow, blue icon circle
- RoleBadge: White bg with colored text
- GlowButton: Blue bg with white text
- ExitConfirmDialog: White bg with blue buttons
- CategoryFilterChips: White bg with blue dropdown
- AppToolbar: Blue header with white text
- InfoRow: White card with blue icon

### PHASE 4: New Components (2 files)

#### 11. BannerItem.kt (new)
```kotlin
data class BannerItem(
    val id: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val itemId: String? = null,
    val isActive: Boolean = true
)
```

#### 12. UserBottomNavBar.kt (new)
Bottom navigation with 3 items:
- Beranda (Home icon) → DashboardUser
- Chat (Chat icon) → UserChatScreen
- Saya (Person icon) → ProfileUserScreen
- Active indicator: BlueDark
- Inactive: TextSecondary
- White background with top shadow

### PHASE 5: User Screens (3 files - biggest changes)

#### 13. DashboardUserScreen.kt (REWRITE)
Layout:
```
┌──────────────────────────────┐
│  🔍  Cari barang sewaan...   │ ← Sticky search bar
├──────────────────────────────┤
│  ┌────────────────────────┐  │
│  │   Banner Auto-Slide    │  │ ← HorizontalPager from Firestore
│  └────────────────────────┘  │    with dot indicators
├──────────────────────────────┤
│ [Semua][Kamera][Alat][...]   │ ← Horizontal scrollable chips
├──────────────────────────────┤
│ ┌──────┐ ┌──────┐           │
│ │ Item │ │ Item │           │ ← 2-column grid in chunked rows
│ └──────┘ └──────┘           │
│ ┌──────┐ ┌──────┐           │
│ │ Item │ │ Item │           │
│ └──────┘ └──────┘           │
├──────────────────────────────┤
│  🏠 Beranda │ 💬 Chat │ 👤 Saya│ ← Bottom Nav
└──────────────────────────────┘
```

Key features:
- Search bar at top (navigates to BrowseItemsScreen or filters items)
- Auto-sliding banner from Firestore `banners` collection
- Horizontal category chips (LazyRow)
- Items in 2-column grid (manual chunked Row layout)
- Bottom navigation bar

#### 14. ChatListScreen.kt
- Add bottom nav
- Remove/replace GalaxyBackground with ModernBackground
- Keep functionality intact

#### 15. ProfileUserScreen.kt
- Add bottom nav
- Update theme colors (replace NebulaGradient with BlueDark)
- Keep all functionality (edit, photo, etc.)

### PHASE 6: Petugas & Admin Screens (6 files - theme only)

#### 16. DashboardPetugasScreen.kt
- Replace GalaxyBackground → ModernBackground
- Replace NebulaHeader → BlueHeader
- Update text/icon colors to new theme
- Keep grid menu layout

#### 17. DashboardAdminScreen.kt
- Same as Petugas

#### 18. ProfilePetugasScreen.kt
- Same pattern as ProfileUserScreen but without bottom nav
- Replace NebulaGradient with BlueDark

#### 19. ProfileAdminScreen.kt
- Same pattern but without bottom nav

### PHASE 7: All Other Screens (auto-updated)

Screens like LoginScreen, SplashScreen, BrowseItemsScreen, ItemDetailScreen, etc.
will automatically update because they use GalaxyBackground/GlassCard/GlowCard components.

### Navigation Changes

No changes to NavGraph.kt needed because:
- DashboardUserScreen already uses navController
- Bottom nav items navigate to existing routes
- Chat tab → navigates to Screen.UserChat.route
- Profile tab → navigates to Screen.ProfileUser.route

### Banner Data (Firestore)

Add `banners` collection to Firestore with documents:
```json
{
  "imageUrl": "base64...",
  "title": "Promo Spesial!",
  "itemId": "optional_item_id",
  "isActive": true
}
```

DashboardUserScreen reads from this collection and shows auto-sliding carousel.
If no banners exist, the section is hidden.
