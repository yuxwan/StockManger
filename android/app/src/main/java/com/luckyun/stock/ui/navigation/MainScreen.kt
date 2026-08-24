package com.luckyun.stock.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luckyun.stock.ui.screens.CheckoutScreen
import com.luckyun.stock.ui.screens.ProductsScreen
import com.luckyun.stock.ui.screens.ProfileScreen
import com.luckyun.stock.ui.theme.OnPrimary
import com.luckyun.stock.ui.theme.OnSurface
import com.luckyun.stock.ui.theme.SurfaceContainer
import com.luckyun.stock.ui.theme.OnSurfaceVariant
import com.luckyun.stock.ui.theme.Surface
import com.luckyun.stock.ui.theme.Primary

sealed class BottomNavItem(
    val route: String,
    val title: String
) {
    object Checkout : BottomNavItem(route = "checkout", title = "收银台")
    object Products : BottomNavItem(route = "products", title = "商品")
    object Profile : BottomNavItem(route = "profile", title = "我的")
}

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val navItems = listOf(
        BottomNavItem.Checkout,
        BottomNavItem.Products,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    // 记录容器宽度
    var containerWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // 计算指示器的目标位置 - 每个导航项占据容器宽度的1/3
    val targetOffset = remember(currentIndex, containerWidth) {
        if (containerWidth > 0) {
            val itemWidth = containerWidth / 3
            itemWidth * currentIndex
        } else {
            0f
        }
    }

    // 动画偏移量
    val animatedOffset by animateDpAsState(
        targetValue = with(density) { targetOffset.toDp() },
        animationSpec = tween(durationMillis = 300),
        label = "indicatorOffset"
    )

    Scaffold(
        bottomBar = {
            Surface(
                color = SurfaceContainer,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 16.dp)
                        .onGloballyPositioned { coordinates ->
                            containerWidth = coordinates.size.width.toFloat()
                        }
                ) {
                    // 滑动指示器 - 椭圆形
                    if (containerWidth > 0) {
                        val itemWidth = containerWidth / 3
                        val indicatorWidth = with(density) { (itemWidth * 0.7f).toDp() }
                        val indicatorHeight = with(density) { (itemWidth * 0.45f).toDp() }
                        Box(
                            modifier = Modifier
                                .offset(x = animatedOffset + (with(density) { (itemWidth * 0.15f).toDp() }))
                                .align(Alignment.Center)
                                .size(indicatorWidth, indicatorHeight)
                                .clip(RoundedCornerShape(50))
                                .background(Primary)
                        )
                    }

                    // 导航项
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val selected = currentRoute == item.route
                            val icon = when (item) {
                                BottomNavItem.Checkout -> Icons.Filled.ShoppingCart
                                BottomNavItem.Products -> Icons.Filled.Store
                                BottomNavItem.Profile -> Icons.Filled.Person
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = item.title,
                                        tint = if (selected) OnPrimary else OnSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) OnPrimary else OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Checkout.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Checkout.route) {
                CheckoutScreen()
            }
            composable(BottomNavItem.Products.route) {
                ProductsScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen()
            }
        }
    }
}