/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.util

sealed class Screen(val route: String) {
    data object TimelineScreen : Screen("timeline_screen")
    data object AlbumsScreen : Screen("albums_screen")

    data object AlbumViewScreen : Screen("album_view_screen") {

        fun albumAndName() = "$route?albumId={albumId}&albumName={albumName}"

    }
    data object MediaViewScreen : Screen("media_screen") {

        fun idAndTarget() = "$route?mediaId={mediaId}&target={target}"

        fun idAndAlbum() = "$route?mediaId={mediaId}&albumId={albumId}"

        fun idAndAlbum(id: Long, albumId: Long) = "$route?mediaId=$id&albumId=$albumId"

        fun idAndQuery() = "${route}_search?mediaId={mediaId}"

        fun idAndQuery(id: Long) = "${route}_search?mediaId=$id"

        fun idAndCategory() = "$route?mediaId={mediaId}&category={category}"

        fun idAndCategory(id: Long, category: String) = "$route?mediaId=$id&category=$category"
        
        // New ID-based category navigation
        fun idAndCategoryId() = "$route?mediaId={mediaId}&categoryId={categoryId}"

        fun idAndCategoryId(id: Long, categoryId: Long) = "$route?mediaId=$id&categoryId=$categoryId"

        fun idAndCollection() = "$route?mediaId={mediaId}&collectionId={collectionId}"

        fun idAndCollection(id: Long, collectionId: Long) = "$route?mediaId=$id&collectionId=$collectionId"

        fun idAndPerson() = "$route?mediaId={mediaId}&personId={personId}"

        fun idAndPerson(id: Long, personId: String) = "$route?mediaId=$id&personId=$personId"

        fun idAndLocation() = "$route?mediaId={mediaId}&gpsLocationNameCity={gpsLocationNameCity}&gpsLocationNameCountry={gpsLocationNameCountry}"

        fun idAndLocation(id: Long, gpsLocationNameCity: String, gpsLocationNameCountry: String) = "$route?mediaId=$id&gpsLocationNameCity=$gpsLocationNameCity&gpsLocationNameCountry=$gpsLocationNameCountry"
    }

    data object LocationTimelineScreen : Screen("location_timeline_screen") {

        fun location() = "$route?gpsLocationNameCity={gpsLocationNameCity}&gpsLocationNameCountry={gpsLocationNameCountry}"

        fun location(gpsLocationNameCity: String, gpsLocationNameCountry: String) = "$route?gpsLocationNameCity=$gpsLocationNameCity&gpsLocationNameCountry=$gpsLocationNameCountry"

    }

    data object TrashedScreen : Screen("trashed_screen")
    data object FavoriteScreen : Screen("favorite_screen")

    data object SettingsScreen : Screen("settings_screen")
    data object ColorPaletteScreen : Screen("color_palette_screen")
    data object SettingsGeneralScreen : Screen("settings_general_screen")
    data object SettingsSmartFeaturesScreen : Screen("settings_smart_features_screen")
    data object AIModelsManagerScreen : Screen("ai_models_manager_screen")
    data object EditBackupsViewerScreen : Screen("edit_backups_viewer_screen")
    data object SettingsAppearanceScreen : Screen("settings_appearance_screen")
    data object SettingsTimelineAlbumsScreen : Screen("settings_timeline_albums_screen")
    data object SettingsMediaViewerScreen : Screen("settings_media_viewer_screen")
    data object SettingsNavigationScreen : Screen("settings_navigation_screen")
    data object SettingsSecurityScreen : Screen("settings_security_screen")
    data object SettingsBackupScreen : Screen("settings_backup_screen")
    data object SettingsBackupExportScreen : Screen("settings_backup_export_screen")
    data object SettingsBackupImportScreen : Screen("settings_backup_import_screen")
    data object SettingsSelectionActionsScreen : Screen("settings_selection_actions_screen")

    data object IgnoredScreen : Screen("ignored_screen")

    data object SetupScreen: Screen("setup_screen")

    data object VaultScreen : Screen("vault_screen")
    data object PrivateFolderScreen : Screen("private_folder_screen")
    data object PrivateFolderSecurityScreen : Screen("private_folder_security_screen")

    data object LibraryScreen : Screen("library_screen")

    data object CategoriesScreen : Screen("categories_screen")
    
    data object CategoriesSettingsScreen : Screen("categories_settings_screen")
    
    data object LocationsScreen : Screen("locations_screen") {

        fun withMediaId() = "$route?mediaId={mediaId}"

        fun withMediaId(mediaId: Long) = "$route?mediaId=$mediaId"

    }

    data object CategoryViewScreen : Screen("category_view_screen") {

        fun category() = "$route?category={category}"

        fun category(string: String) = "$route?category=$string"
        
        // New ID-based routing for the new category system
        fun categoryId() = "$route?categoryId={categoryId}"
        
        fun categoryId(id: Long) = "$route?categoryId=$id"

    }
    
    data object CategoryEditScreen : Screen("category_edit_screen") {
        fun categoryId() = "$route?categoryId={categoryId}"
        
        fun categoryId(id: Long) = "$route?categoryId=$id"
    }

    data object AddCategoryScreen : Screen("add_category_screen")
    
    data object EditCategoryScreen : Screen("edit_category_screen") {
        fun route() = "$route?categoryId={categoryId}"
        fun categoryId(id: Long) = "$route?categoryId=$id"
    }

    data object CategoryEditorScreen : Screen("category_editor_screen") {
        fun create() = route
        fun edit() = "$route?categoryId={categoryId}"
        fun edit(categoryId: Long) = "$route?categoryId=$categoryId"
    }

    data object AlbumGroupViewScreen : Screen("album_group_view_screen") {

        fun groupId() = "$route?groupId={groupId}"

        fun groupId(id: Long) = "$route?groupId=$id"

    }

    data object EditGroupScreen : Screen("edit_group_screen") {

        fun groupId() = "$route?groupId={groupId}"

        fun groupId(id: Long) = "$route?groupId=$id"

    }

    data object CollectionViewScreen : Screen("collection_view_screen") {

        fun collectionId() = "$route?collectionId={collectionId}"

        fun collectionId(id: Long) = "$route?collectionId=$id"

    }

    data object CollectionAlbumSelectorScreen : Screen("collection_album_selector_screen") {

        fun collectionName() = "$route?collectionName={collectionName}"

        fun collectionName(name: String) = "$route?collectionName=$name"

        fun collectionId() = "$route?collectionId={collectionId}"

        fun collectionId(id: Long) = "$route?collectionId=$id"

    }

    data object DateFormatScreen : Screen("date_format_screen")

    data object SearchScreen : Screen("search_screen")

    data object MetadataViewScreen : Screen("metadata_view_screen") {

        fun uriAndType() = "$route?mediaUri={mediaUri}&isVideo={isVideo}"

        fun uriAndType(mediaUri: String, isVideo: Boolean) =
            "$route?mediaUri=$mediaUri&isVideo=$isVideo"
    }

    data object HelpScreen : Screen("help_screen")

    data object TutorialCategoryScreen : Screen("tutorial_category_screen") {
        fun category() = "$route?category={category}"
        fun category(category: String) = "$route?category=$category"
    }

    data object TutorialDetailScreen : Screen("tutorial_detail_screen") {
        fun tipId() = "$route?tipId={tipId}"
        fun tipId(id: String) = "$route?tipId=$id"
    }

    data object WhatsNewScreen : Screen("whats_new_screen")

    data object StoryViewerScreen : Screen("story_viewer_screen") {
        fun cardId() = "$route?cardId={cardId}"
        fun cardId(id: Long) = "$route?cardId=$id"
    }

    data object StoryCardsSettingsScreen : Screen("story_cards_settings_screen")

    data object CloudAccountsScreen : Screen("cloud_accounts_screen")
    data object CloudTimelineScreen : Screen("cloud_timeline_screen")
    data object CloudAddServerScreen : Screen("cloud_add_server_screen") {
        fun providerType() = "$route?providerType={providerType}"
        fun providerType(type: String) = "$route?providerType=$type"
    }
    data object CloudEditServerScreen : Screen("cloud_edit_server_screen") {
        fun configId() = "$route?configId={configId}"
        fun configId(id: Long) = "$route?configId=$id"
    }

    data object CloudUploadSettingsScreen : Screen("cloud_upload_settings_screen")

    data object CloudProviderSettingsScreen : Screen("cloud_provider_settings_screen") {
        fun configId() = "$route?configId={configId}"
        fun configId(id: Long) = "$route?configId=$id"
    }

    // Phase 1 – Library Tab
    data object CloudLibraryScreen : Screen("cloud_library_screen")

    // Phase 2 – Profile
    data object CloudProfileScreen : Screen("cloud_profile_screen")

    // Phase 3 – Backup
    data object CloudBackupScreen : Screen("cloud_backup_screen")
    data object BackupAlbumPickerScreen : Screen("backup_album_picker_screen")
    data object BackupOptionsScreen : Screen("backup_options_screen")
    data object UploadDetailsScreen : Screen("upload_details_screen")

    // Phase 3+4 – Merged Backup & Sync
    data object CloudBackupAndSyncScreen : Screen("cloud_backup_and_sync_screen")

    // Phase 4 – Sync Status
    data object SyncStatusScreen : Screen("sync_status_screen")

    // Phase 5 – Notifications
    data object CloudNotificationSettingsScreen : Screen("cloud_notification_settings_screen")

    // Phase 6 – Networking
    data object CloudNetworkingScreen : Screen("cloud_networking_screen")

    // Phase 7 – Archive
    data object CloudArchiveScreen : Screen("cloud_archive_screen")

    // Phase 8 – Free Up Space
    data object FreeUpSpaceScreen : Screen("free_up_space_screen")

    // Phase 9 – Shared Links
    data object SharedLinksScreen : Screen("shared_links_screen")

    // Phase 10 – Places
    data object CloudPlacesScreen : Screen("cloud_places_screen")
    data object PlaceDetailScreen : Screen("place_detail_screen") {
        fun city() = "$route?city={city}&country={country}"
        fun city(city: String, country: String) = "$route?city=$city&country=$country"
    }

    // Phase 11 – Person Detail
    data object PersonDetailScreen : Screen("person_detail_screen") {
        fun personId() = "$route?personId={personId}"
        fun personId(id: String) = "$route?personId=$id"
    }
    data object PeopleListScreen : Screen("people_list_screen")

    // Phase 12 – Viewer Settings
    data object CloudViewerSettingsScreen : Screen("cloud_viewer_settings_screen")

    // Phase 13 – Advanced Settings
    data object CloudAdvancedSettingsScreen : Screen("cloud_advanced_settings_screen")

    // Phase 16 – Memories
    data object MemoriesScreen : Screen("memories_screen")

    operator fun invoke() = route
}
