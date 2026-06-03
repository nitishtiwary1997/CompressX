package com.hire.smartcompress.presentation.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hire.smartcompress.presentation.screens.batch.BatchScreen
import com.hire.smartcompress.presentation.screens.compression.image.ImageCompressionScreen
import com.hire.smartcompress.presentation.screens.compression.pdf.PdfCompressionScreen
import com.hire.smartcompress.presentation.screens.compression.video.VideoCompressionScreen
import com.hire.smartcompress.presentation.screens.dashboard.DashboardScreen
import com.hire.smartcompress.presentation.screens.fileselection.FileSelectionScreen
import com.hire.smartcompress.presentation.screens.history.HistoryScreen
import com.hire.smartcompress.presentation.screens.result.ResultScreen
import com.hire.smartcompress.domain.model.ConversionType
import com.hire.smartcompress.presentation.screens.converter.FormatConverterScreen
import com.hire.smartcompress.presentation.screens.crop.ImageCropScreen
import com.hire.smartcompress.presentation.screens.settings.SettingsScreen
import com.hire.smartcompress.presentation.screens.smartcompress.SmartCompressScreen
import com.hire.smartcompress.presentation.screens.storage.StorageAnalyzerScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToFileSelection = { type ->
                    navController.navigate(Screen.FileSelection.createRoute(type))
                },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToStorage = { navController.navigate(Screen.StorageAnalyzer.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToBatch = { navController.navigate(Screen.Batch.route) },
                onNavigateToConverter = { navController.navigate(Screen.FormatConverter.createRoute()) },
                onNavigateToImagePdf = { navController.navigate(Screen.FormatConverter.createRoute("IMAGES_TO_PDF")) },
                onNavigateToSmartCompress = { navController.navigate(Screen.SmartCompress.route) },
                onNavigateToCrop = { navController.navigate(Screen.ImageCrop.route) }
            )
        }

        composable(Screen.ImageCrop.route) {
            ImageCropScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.FormatConverter.route,
            arguments = listOf(navArgument("type") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStack ->
            val typeStr = backStack.arguments?.getString("type") ?: ""
            val preselected = ConversionType.entries.firstOrNull { it.name == typeStr }
            FormatConverterScreen(
                initialType = preselected,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SmartCompress.route) {
            SmartCompressScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.FileSelection.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStack ->
            val type = backStack.arguments?.getString("type") ?: "all"
            FileSelectionScreen(
                fileTypeFilter = type,
                onFileSelected = { uri, mimeType ->
                    val encoded = Uri.encode(uri.toString())
                    when {
                        mimeType.startsWith("image/") -> navController.navigate(Screen.ImageCompression.createRoute(encoded))
                        mimeType.startsWith("video/") -> navController.navigate(Screen.VideoCompression.createRoute(encoded))
                        mimeType == "application/pdf" -> navController.navigate(Screen.PdfCompression.createRoute(encoded))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ImageCompression.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStack ->
            val encodedUri = backStack.arguments?.getString("uri") ?: ""
            ImageCompressionScreen(
                fileUri = Uri.parse(Uri.decode(encodedUri)),
                onCompressionComplete = { result ->
                    val encoded = Uri.encode(result.compressedUri.toString())
                    navController.navigate(
                        Screen.Result.createRoute(
                            result.originalSize, result.compressedSize,
                            result.savedBytes, result.savedPercent,
                            result.processingTimeMs, "IMAGE", encoded
                        )
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VideoCompression.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStack ->
            val encodedUri = backStack.arguments?.getString("uri") ?: ""
            VideoCompressionScreen(
                fileUri = Uri.parse(Uri.decode(encodedUri)),
                onCompressionComplete = { result ->
                    val encoded = Uri.encode(result.compressedUri.toString())
                    navController.navigate(
                        Screen.Result.createRoute(
                            result.originalSize, result.compressedSize,
                            result.savedBytes, result.savedPercent,
                            result.processingTimeMs, "VIDEO", encoded
                        )
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PdfCompression.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStack ->
            val encodedUri = backStack.arguments?.getString("uri") ?: ""
            PdfCompressionScreen(
                fileUri = Uri.parse(Uri.decode(encodedUri)),
                onCompressionComplete = { result ->
                    val encoded = Uri.encode(result.compressedUri.toString())
                    navController.navigate(
                        Screen.Result.createRoute(
                            result.originalSize, result.compressedSize,
                            result.savedBytes, result.savedPercent,
                            result.processingTimeMs, "PDF", encoded
                        )
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Batch.route) {
            BatchScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("originalSize") { type = NavType.LongType },
                navArgument("compressedSize") { type = NavType.LongType },
                navArgument("savedBytes") { type = NavType.LongType },
                navArgument("savedPercent") { type = NavType.FloatType },
                navArgument("processingTime") { type = NavType.LongType },
                navArgument("fileType") { type = NavType.StringType },
                navArgument("compressedUri") { type = NavType.StringType }
            )
        ) { backStack ->
            val args = backStack.arguments!!
            ResultScreen(
                originalSize = args.getLong("originalSize"),
                compressedSize = args.getLong("compressedSize"),
                savedBytes = args.getLong("savedBytes"),
                savedPercent = args.getFloat("savedPercent"),
                processingTimeMs = args.getLong("processingTime"),
                fileType = args.getString("fileType") ?: "IMAGE",
                compressedUri = Uri.parse(Uri.decode(args.getString("compressedUri") ?: "")),
                onBack = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) } }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.StorageAnalyzer.route) {
            StorageAnalyzerScreen(onBack = { navController.popBackStack() })
        }
    }
}
