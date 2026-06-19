package com.hambalapps.expressivebox

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hambalapps.expressivebox.ui.main.MainScreen
import com.hambalapps.expressivebox.ui.split.SplitTunnelingScreen

import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val motionScheme = MaterialTheme.motionScheme
  val spatialSpec = remember(motionScheme) { motionScheme.defaultSpatialSpec<IntOffset>() }
  val effectsSpec = remember(motionScheme) { motionScheme.slowEffectsSpec<Float>() }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    transitionSpec = {
      slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spatialSpec
      ) + fadeIn(
        animationSpec = effectsSpec
      ) togetherWith slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = spatialSpec
      ) + fadeOut(
        animationSpec = effectsSpec
      )
    },
    popTransitionSpec = {
      slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = spatialSpec
      ) + fadeIn(
        animationSpec = effectsSpec
      ) togetherWith slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = spatialSpec
      ) + fadeOut(
        animationSpec = effectsSpec
      )
    },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.fillMaxSize())
        }
        entry<SplitTunneling> {
          SplitTunnelingScreen(onBack = { backStack.removeLastOrNull() }, modifier = Modifier.fillMaxSize())
        }
      },
  )
}

