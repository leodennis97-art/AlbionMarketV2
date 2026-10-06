package com.example.albionmarketv2

import android.app.Application

object SharedViewModelProvider {
    var viewModel: AlbionResourceViewModel? = null
    
    fun get(application: Application): AlbionResourceViewModel {
        return viewModel ?: AlbionResourceViewModel(application).also { viewModel = it }
    }
}
