package app.receptnik.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.receptnik.Shared

private data class Tab(val route: String, val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("recipes", "Рецепты", Icons.Outlined.RestaurantMenu),
    Tab("plan", "План", Icons.Outlined.CalendarMonth),
    Tab("grocery", "Покупки", Icons.Outlined.ShoppingCart),
    Tab("cookbooks", "Книги", Icons.AutoMirrored.Outlined.MenuBook),
)

object Routes {
    fun recipe(id: Long) = "recipe/$id"
    fun cook(id: Long) = "cook/$id"
    fun edit(id: Long) = "edit/$id"
    fun cookbook(id: Long) = "cookbook/$id"
    fun importLink(text: String = "") = "import?text=${Uri.encode(text)}&image="
    fun importImage(uri: Uri) = "import?text=&image=${Uri.encode(uri.toString())}"
    const val IMPORT_PICK_PHOTO = "import?text=&image=pick"
}

@Composable
fun AppNav(shared: Shared?, onSharedConsumed: () -> Unit) {
    val nav = rememberNavController()

    LaunchedEffect(shared) {
        when (shared) {
            is Shared.Text -> nav.navigate(Routes.importLink(shared.text))
            is Shared.Image -> nav.navigate(Routes.importImage(shared.uri))
            null -> return@LaunchedEffect
        }
        onSharedConsumed()
    }

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = { nav.switchTab(t.route) },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.title) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "recipes", modifier = Modifier.padding(bottom = if (showBar) pad.calculateBottomPadding() else 0.dp)) {
            composable("recipes") { RecipesScreen(nav) }
            composable("plan") { MealPlanScreen(nav) }
            composable("grocery") { GroceryScreen() }
            composable("cookbooks") { CookbooksScreen(nav) }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable(
                "recipe/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { RecipeDetailScreen(it.arguments!!.getLong("id"), nav) }
            composable(
                "cook/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { CookModeScreen(it.arguments!!.getLong("id"), onBack = { nav.popBackStack() }) }
            composable(
                "edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { EditRecipeScreen(it.arguments!!.getLong("id"), nav) }
            composable(
                "cookbook/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { CookbookDetailScreen(it.arguments!!.getLong("id"), nav) }
            composable(
                "import?text={text}&image={image}",
                arguments = listOf(
                    navArgument("text") { type = NavType.StringType; defaultValue = "" },
                    navArgument("image") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                ImportScreen(
                    initialText = it.arguments?.getString("text").orEmpty(),
                    initialImage = it.arguments?.getString("image").orEmpty(),
                    nav = nav,
                )
            }
        }
    }
}

fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
