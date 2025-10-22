package eu.anifantakis.camerax_demo.di

import eu.anifantakis.camerax_demo.ui.CameraViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::CameraViewModel)
}