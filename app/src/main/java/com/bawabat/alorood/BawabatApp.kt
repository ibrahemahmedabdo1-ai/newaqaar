package com.bawabat.alorood

import android.app.Application
import com.bawabat.alorood.data.Repo

class BawabatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
    }
}
