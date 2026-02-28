package eu.anifantakis.camerax_demo.di

import eu.anifantakis.camerax_demo.ui.screens.realistic.CameraViewModel
import eu.anifantakis.camerax_demo.ui.screens.realistic.media3.Media3ViewModel
import eu.anifantakis.camerax_demo.ui.screens.realistic.mlkit.MlKitViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::CameraViewModel)
    viewModelOf(::MlKitViewModel)
    viewModelOf(::Media3ViewModel)
}